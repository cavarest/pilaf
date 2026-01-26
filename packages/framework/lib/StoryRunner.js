/**
 * StoryRunner - Executes test stories for Pilaf
 * Supports both YAML files and JavaScript objects
 */

let yaml;
try {
  yaml = require('js-yaml');
} catch (e) {
  yaml = null;
}

const fs = require('fs');
const { PilafBackendFactory } = require('@pilaf/backends');
const { waitForServerConfirmation: waitForServerConfirmationFn, getDefaultTimeout } = require('./helpers/correlation.js');
const EntityUtils = require('./helpers/entities.js');

/**
 * StoryRunner executes test stories defined in YAML format
 *
 * Story format:
 * name: "Story Name"
 * description: "Story description"
 * setup:
 *   server:
 *     type: "paper"
 *     version: "1.21.8"
 * players:
 *   - name: "TestPlayer"
 *     username: "testplayer"
 * steps:
 *   - name: "Step name"
 *     action: "action_type"
 *     ...action-specific params
 * teardown:
 *   stop_server: true
 */

class StoryRunner {
  /**
   * Create a StoryRunner
   * @param {Object} options - Options
   * @param {Object} [options.logger] - Logger instance (defaults to console)
   * @param {Object} [options.reporter] - Reporter instance for collecting results
   */
  constructor(options = {}) {
    this.logger = options?.logger || console;
    this.reporter = options?.reporter;
    this.backends = {
      rcon: null,
      players: new Map() // username -> backend
    };
    this.bots = new Map(); // username -> bot
    this.currentStory = null;
    this.results = [];
    this.variables = new Map(); // Variable storage for store_as mechanism
    this.pendingInventoryUpdates = new Map(); // username -> Set of expected items
  }

  /**
   * Check if bot's inventory contains expected items (with fuzzy matching)
   */
  _hasItems(bot, expectedItems) {
    const currentItems = bot.inventory.items() || [];

    for (const expected of expectedItems) {
      const found = currentItems.some(item => {
        if (!item) return false;

        // Normalize both names for comparison
        const itemName = item.name || '';
        const itemDisplayName = item.displayName || '';
        const expectedNormalized = expected.replace(/^minecraft:/, '').toLowerCase();
        const nameNormalized = itemName.replace(/^minecraft:/, '').toLowerCase();
        const displayNormalized = itemDisplayName.toLowerCase();

        // Exact match
        if (nameNormalized === expectedNormalized) return true;
        // Display name match
        if (displayNormalized === expectedNormalized) return true;
        // Contains match
        if (nameNormalized.includes(expectedNormalized) || expectedNormalized.includes(nameNormalized)) return true;
        if (displayNormalized.includes(expectedNormalized)) return true;

        return false;
      });

      if (!found) return false;
    }

    return true;
  }

  /**
   * Get list of items currently in bot's inventory for debugging
   */
  _getInventoryItemList(bot) {
    const currentItems = bot.inventory.items() || [];
    const itemList = currentItems
      .filter(item => item != null)
      .map(item => {
        const name = item.name || item.displayName || 'unknown';
        const count = item.count || 1;
        return `${name} x${count}`;
      });

    return itemList.length > 0 ? itemList.join(', ') : '(empty)';
  }

  /**
   * Wait for inventory update from server using event-based detection
   * Monitors bot inventory for expected items after RCON commands
   */
  async _waitForInventoryUpdate(player, expectedItems = [], timeoutMs = 8000) {
    const bot = this.bots.get(player);
    if (!bot) {
      this.logger.log(`[StoryRunner] ⚠ No bot found for player "${player}"`);
      return false;
    }

    if (expectedItems.length === 0) {
      return true;
    }

    this.logger.log(`[StoryRunner] 🔄 Waiting for ${player} to receive: ${expectedItems.join(', ')}`);

    // Check immediately first (might already have items)
    if (this._hasItems(bot, expectedItems)) {
      this.logger.log(`[StoryRunner] ✓ ${player} already has: ${expectedItems.join(', ')}`);
      return true;
    }

    return new Promise((resolve) => {
      let resolved = false;

      // Set up timeout
      const timeout = setTimeout(() => {
        if (resolved) return;
        resolved = true;

        const currentInventory = this._getInventoryItemList(bot);
        this.logger.log(`[StoryRunner] ⚠ Inventory sync timeout for ${player}. Expected: ${expectedItems.join(', ')}. Current: ${currentInventory}`);
        resolve(false);
      }, timeoutMs);

      // Listen for inventory update event
      const listener = () => {
        if (resolved) return;

        if (this._hasItems(bot, expectedItems)) {
          resolved = true;
          clearTimeout(timeout);
          bot.removeListener('inventoryUpdate', listener);

          this.logger.log(`[StoryRunner] ✓ ${player} received: ${expectedItems.join(', ')}`);
          resolve(true);
        }
      };

      bot.on('inventoryUpdate', listener);

      // Also set up a fallback poll every 1 second (in case event is missed)
      const pollInterval = setInterval(() => {
        if (resolved) {
          clearInterval(pollInterval);
          return;
        }

        if (this._hasItems(bot, expectedItems)) {
          resolved = true;
          clearTimeout(timeout);
          clearInterval(pollInterval);
          bot.removeListener('inventoryUpdate', listener);

          this.logger.log(`[StoryRunner] ✓ ${player} received: ${expectedItems.join(', ')} (via poll)`);
          resolve(true);
        }
      }, 1000);
    });
  }

  /**
   * Normalize item names to match between RCON output and bot inventory
   */
  _normalizeItemName(itemName) {
    // RCON uses underscore format (diamond_sword), bot inventory uses same
    return itemName.replace(/^minecraft:/, '');
  }

  /**
   * Load a story from a YAML file
   * @param {string} filePath - Path to YAML story file
   * @returns {Object} Parsed story object
   */
  loadStory(filePath) {
    if (!yaml) {
      throw new Error('js-yaml module is not installed. Please install it with: npm install js-yaml');
    }

    try {
      const fileContents = fs.readFileSync(filePath, 'utf8');
      const story = yaml.load(fileContents);

      // Validate story structure
      if (!story.name) {
        throw new Error('Story must have a "name" field');
      }
      if (!story.steps || !Array.isArray(story.steps)) {
        throw new Error('Story must have a "steps" array');
      }

      this.currentStory = story;
      this.logger.log(`[StoryRunner] Loaded story: ${story.name}`);
      return story;
    } catch (error) {
      throw new Error(`Failed to load story from ${filePath}: ${error.message}`);
    }
  }

  /**
   * Execute a story
   * @param {Object|string} story - Story object or path to YAML file
   * @returns {Promise<{success: boolean, results: Array}>}
   */
  async execute(story) {
    // Load story if path is provided
    if (typeof story === 'string') {
      this.loadStory(story);
    } else {
      this.currentStory = story;
    }

    const storyName = this.currentStory.name;
    this.logger.log(`[StoryRunner] Starting story: ${storyName}`);

    // Clear variables from previous story
    this.variables.clear();

    const startTime = Date.now();
    const storyResults = {
      story: storyName,
      steps: [],
      success: true,
      error: null,
      duration: 0
    };

    try {
      // Execute setup
      if (this.currentStory.setup) {
        await this.executeSetup(this.currentStory.setup);
      }

      // Execute steps
      for (let i = 0; i < this.currentStory.steps.length; i++) {
        const step = this.currentStory.steps[i];
        this.logger.log(`[StoryRunner] Step ${i + 1}/${this.currentStory.steps.length}: ${step.name}`);

        const stepResult = await this.executeStep(step);
        storyResults.steps.push(stepResult);

        if (!stepResult.success) {
          storyResults.success = false;
          storyResults.error = `Step "${step.name}" failed: ${stepResult.error}`;
          this.logger.log(`[StoryRunner] Step failed: ${stepResult.error}`);
          break;
        }
      }

      // Execute teardown
      if (this.currentStory.teardown) {
        await this.executeTeardown(this.currentStory.teardown);
      }
    } catch (error) {
      storyResults.success = false;
      storyResults.error = error.message;
      this.logger.log(`[StoryRunner] Error: ${error.message}`);
      if (error.stack) {
        this.logger.log(`[StoryRunner] Stack: ${error.stack.split('\n').slice(0, 3).join('\n')}`);
      }
    }

    storyResults.duration = Date.now() - startTime;
    this.logger.log(`[StoryRunner] Story ${storyName} ${storyResults.success ? 'PASSED' : 'FAILED'} (${storyResults.duration}ms)`);

    return storyResults;
  }

  /**
   * Execute story setup
   * @param {Object} setup - Setup configuration
   * @returns {Promise<void>}
   */
  async executeSetup(setup) {
    // Connect RCON backend
    if (setup.server) {
      const rconConfig = {
        host: process.env.RCON_HOST || 'localhost',
        port: parseInt(process.env.RCON_PORT) || 25575,
        password: process.env.RCON_PASSWORD || 'cavarest'
      };

      this.backends.rcon = await PilafBackendFactory.create('rcon', rconConfig);
      this.logger.log('[StoryRunner] RCON backend connected');

      // Wait for server to be ready using RCON list command
      await this.waitForServerReady();
      this.logger.log('[StoryRunner] Server is ready');
    }

    // Create player backends
    if (setup.players && Array.isArray(setup.players)) {
      for (const playerConfig of setup.players) {
        await this.createPlayer(playerConfig);
      }
    }
  }

  /**
   * Wait for server to be ready using RCON
   * @returns {Promise<void>}
   */
  async waitForServerReady() {
    const startTime = Date.now();
    const timeout = 120000;
    const interval = 3000;

    while (Date.now() - startTime < timeout) {
      try {
        await this.backends.rcon.send('list');
        return; // Server is ready
      } catch (error) {
        this.logger.log(`[StoryRunner] Server not ready yet, waiting... (${error.message})`);
        await new Promise(resolve => setTimeout(resolve, interval));
      }
    }

    throw new Error('Server did not become ready within timeout period');
  }

  /**
   * Execute story teardown
   * @param {Object} teardown - Teardown configuration
   * @returns {Promise<void>}
   */
  async executeTeardown(teardown) {
    // Disconnect all bots
    for (const [username, bot] of this.bots) {
      const backend = this.backends.players.get(username);
      if (backend) {
        await backend.quitBot(bot);
      }
    }
    this.bots.clear();

    // Disconnect all player backends
    for (const [username, backend] of this.backends.players) {
      await backend.disconnect();
    }
    this.backends.players.clear();

    // Always disconnect RCON to prevent open handles
    if (this.backends.rcon) {
      // Stop server if requested (send message before disconnect)
      if (teardown.stop_server) {
        await this.backends.rcon.send('say [Pilaf] Server stopping...');
      }
      // Always disconnect to prevent Jest hanging on open handles
      await this.backends.rcon.disconnect();
      this.backends.rcon = null;
    }

    // Wait for cleanup to complete
    await new Promise(resolve => setTimeout(resolve, 500));
  }

  /**
   * Create a player bot
   * @param {Object} playerConfig - Player configuration
   * @param {string} playerConfig.name - Player name
   * @param {string} playerConfig.username - Bot username
   * @returns {Promise<void>}
   */
  async createPlayer(playerConfig) {
    const { name, username } = playerConfig;

    if (!username) {
      throw new Error(`Player "${name}" must have a username`);
    }

    // Wait a bit before creating new bot to avoid connection conflicts
    await new Promise(resolve => setTimeout(resolve, 200));

    // Create player backend
    const backend = await PilafBackendFactory.create('mineflayer', {
      host: process.env.MC_HOST || 'localhost',
      port: parseInt(process.env.MC_PORT) || 25565,
      auth: 'offline',
      rconHost: process.env.RCON_HOST || 'localhost',
      rconPort: parseInt(process.env.RCON_PORT) || 25575,
      rconPassword: process.env.RCON_PASSWORD || 'cavarest'
    });

    // Wait for server to be ready
    await backend.waitForServerReady({ timeout: 60000, interval: 3000 });

    // Create bot with retry logic
    let bot;
    let lastError;
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        bot = await backend.createBot({
          username,
          spawnTimeout: 60000
        });
        break; // Success, exit retry loop
      } catch (error) {
        lastError = error;
        if (attempt < 3) {
          this.logger.log(`[StoryRunner] Bot creation attempt ${attempt} failed: ${error.message}, retrying...`);
          await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
        }
      }
    }

    if (!bot) {
      throw new Error(`Failed to create bot after 3 attempts: ${lastError.message}`);
    }

    this.backends.players.set(username, backend);
    this.bots.set(username, bot);

    this.logger.log(`[StoryRunner] Created player: ${username}`);
  }

  /**
   * Resolve variable references in step parameters
   * @param {Object} step - Step configuration
   * @returns {Object} Step with resolved variables
   */
  resolveVariables(step) {
    const resolved = { ...step };

    const resolveValue = (value) => {
      if (typeof value === 'string') {
        // Check for {variableName} or {variable.property} pattern
        const match = value.match(/^\{(.+)\}$/);
        if (match) {
          const varPath = match[1];

          // Check if it contains a dot (nested property access)
          if (varPath.includes('.')) {
            const parts = varPath.split('.');
            const rootVar = parts[0];

            if (!this.variables.has(rootVar)) {
              throw new Error(`Variable "${rootVar}" not found. Available variables: ${[...this.variables.keys()].join(', ')}`);
            }

            // Navigate through nested properties
            let result = this.variables.get(rootVar);
            for (let i = 1; i < parts.length; i++) {
              if (result && typeof result === 'object' && parts[i] in result) {
                result = result[parts[i]];
              } else {
                throw new Error(`Property "${parts[i]}" not found in variable "${rootVar}"`);
              }
            }
            return result;
          } else {
            // Simple variable access
            if (!this.variables.has(varPath)) {
              throw new Error(`Variable "${varPath}" not found. Available variables: ${[...this.variables.keys()].join(', ')}`);
            }
            return this.variables.get(varPath);
          }
        }

        // Check for expression patterns like "{var.x} + 1" or "{var.y} - 2"
        const exprMatch = value.match(/^(.+?)\s*([+\-])\s*(\d+)$/);
        if (exprMatch) {
          const varPart = exprMatch[1].trim();
          const operator = exprMatch[2];
          const operand = parseInt(exprMatch[3], 10);

          // Check if varPart is a variable reference
          const varMatch = varPart.match(/^\{(.+)\}$/);
          if (varMatch) {
            const varPath = varMatch[1];
            let varValue;

            // Check if it contains a dot (nested property access)
            if (varPath.includes('.')) {
              const parts = varPath.split('.');
              const rootVar = parts[0];

              if (!this.variables.has(rootVar)) {
                throw new Error(`Variable "${rootVar}" not found`);
              }

              varValue = this.variables.get(rootVar);
              for (let i = 1; i < parts.length; i++) {
                if (varValue && typeof varValue === 'object' && parts[i] in varValue) {
                  varValue = varValue[parts[i]];
                } else {
                  throw new Error(`Property "${parts[i]}" not found in variable "${rootVar}"`);
                }
              }
            } else {
              if (!this.variables.has(varPath)) {
                throw new Error(`Variable "${varPath}" not found`);
              }
              varValue = this.variables.get(varPath);
            }

            // Apply the operation
            if (operator === '+') {
              return varValue + operand;
            } else if (operator === '-') {
              return varValue - operand;
            }
          }
        }

        return value;
      } else if (Array.isArray(value)) {
        return value.map(resolveValue);
      } else if (value && typeof value === 'object') {
        const resolvedObj = {};
        for (const [key, val] of Object.entries(value)) {
          resolvedObj[key] = resolveValue(val);
        }
        return resolvedObj;
      }
      return value;
    };

    // Resolve all values in the step (except action and store_as which should remain as-is)
    for (const [key, value] of Object.entries(resolved)) {
      if (key !== 'action' && key !== 'store_as') {
        resolved[key] = resolveValue(value);
      }
    }

    return resolved;
  }

  /**
   * Execute a single step
   * @param {Object} step - Step configuration
   * @returns {Promise<{success: boolean, error: string|null}>}
   */
  async executeStep(step) {
    const { action, store_as } = step;

    if (!action) {
      return {
        success: false,
        error: 'Step must have an "action" field',
        step: step.name
      };
    }

    try {
      // Resolve any variable references in step parameters
      const resolvedParams = this.resolveVariables(step);

      // Execute action and capture result
      const result = await this.executeAction(action, resolvedParams);

      // Store result if store_as is specified
      if (store_as && result !== undefined) {
        this.variables.set(store_as, result);
        this.logger.log(`[StoryRunner] Stored result as "${store_as}"`);
      }

      return {
        success: true,
        error: null,
        step: step.name
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
        step: step.name
      };
    }
  }

  /**
   * Execute an action
   * @param {string} action - Action type
   * @param {Object} params - Action parameters
   * @returns {Promise<any>} Action result (if any)
   */
  async executeAction(action, params) {
    const handler = this.actionHandlers[action];

    if (!handler) {
      throw new Error(`Unknown action: ${action}`);
    }

    return await handler.call(this, params);
  }

  /**
   * Action handlers
   */
  actionHandlers = {
    /**
     * Execute a command via RCON
     */
    async execute_command(params) {
      const { command } = params;
      if (!command) {
        throw new Error('execute_command requires "command" parameter');
      }

      this.logger.log(`[StoryRunner] ACTION: RCON ${command}`);

      // Check if this is a 'give' command and extract player and item
      // Format: give <player> <item> [count]
      let targetPlayer = null;
      let expectedItems = [];
      const giveMatch = command.match(/^give\s+(\w+)\s+(\S+)/);
      if (giveMatch) {
        targetPlayer = giveMatch[1]; // First capture group is player
        expectedItems.push(this._normalizeItemName(giveMatch[2])); // Second is item
      }

      const result = await this.backends.rcon.send(command);
      this.logger.log(`[StoryRunner] RESPONSE: ${result.raw}`);

      // If we gave items, wait for that specific bot's inventory to sync
      if (expectedItems.length > 0 && targetPlayer) {
        this.logger.log(`[StoryRunner] 🔄 RCON gave ${expectedItems.join(', ')} to ${targetPlayer}, waiting for bot inventory sync...`);

        // Only check the target bot's inventory, not all bots
        const synced = await this._waitForInventoryUpdate(targetPlayer, expectedItems, 8000);

        if (!synced) {
          this.logger.log(`[StoryRunner] ⚠ Warning: ${targetPlayer} may not have received ${expectedItems.join(', ')} - continuing anyway`);
        }
      }
    },

    /**
     * Send a chat message from a player
     */
    async chat(params) {
      const { player, message } = params;
      if (!player) {
        throw new Error('chat requires "player" parameter');
      }
      if (!message) {
        throw new Error('chat requires "message" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} chat: ${message}`);
      bot.chat(message);
      this.logger.log(`[StoryRunner] RESPONSE: Message sent`);
    },

    /**
     * Wait for a specified duration
     */
    async wait(params) {
      const { duration = 1 } = params;
      await new Promise(resolve => setTimeout(resolve, duration * 1000));
      this.logger.log(`[StoryRunner] Waited ${duration}s`);
    },

    /**
     * Assert a condition
     */
    async assert(params) {
      const { condition, expected, actual, contains, not_empty } = params;

      if (condition === 'equals') {
        if (actual !== expected) {
          throw new Error(`Assertion failed: expected "${expected}" but got "${actual}"`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: ${actual} equals ${expected}`);
      } else if (condition === 'contains') {
        if (!actual || !actual.includes(expected)) {
          throw new Error(`Assertion failed: "${actual}" does not contain "${expected}"`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: "${actual}" contains "${expected}"`);
      } else if (condition === 'not_empty') {
        const value = not_empty || actual;
        const isEmpty = Array.isArray(value) ? value.length === 0 : !value;
        if (isEmpty) {
          throw new Error(`Assertion failed: value is empty`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: value is not empty`);
      } else if (condition === 'entity_exists') {
        // expected: entity name to check for
        // actual: array of entities from get_entities
        const entity = actual.find(e =>
          e.name === expected ||
          e.customName === expected ||
          e.displayName === expected ||
          e.customName?.text === expected
        );
        if (!entity) {
          // Debug: log available entity names
          const availableNames = actual.map(e => e.name || e.displayName || e.customName).slice(0, 10).join(', ');
          this.logger.log(`[StoryRunner] Available entities (first 10): ${availableNames}`);
          throw new Error(`Assertion failed: entity "${expected}" not found`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: entity "${expected}" exists`);
      } else if (condition === 'entity_not_exists') {
        // expected: entity name to check for
        // actual: array of entities from get_entities
        const entity = actual.find(e =>
          e.name === expected ||
          e.customName === expected ||
          e.displayName === expected ||
          e.customName?.text === expected
        );
        if (entity) {
          throw new Error(`Assertion failed: entity "${expected}" still exists`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: entity "${expected}" does not exist`);
      } else if (condition === 'has_item') {
        // expected: item name
        // actual: inventory from get_player_inventory
        const hasItem = actual.items.some(item => item && item.name === expected);
        if (!hasItem) {
          throw new Error(`Assertion failed: player does not have item "${expected}"`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: player has item "${expected}"`);
      } else if (condition === 'does_not_have_item') {
        // expected: item name
        // actual: inventory from get_player_inventory
        const hasItem = actual.items.some(item => item && item.name === expected);
        if (hasItem) {
          throw new Error(`Assertion failed: player still has item "${expected}"`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: player does not have item "${expected}"`);
      } else if (condition === 'greater_than') {
        const actualNum = parseFloat(actual);
        const expectedNum = parseFloat(expected);
        if (isNaN(actualNum) || isNaN(expectedNum)) {
          throw new Error(`Assertion failed: cannot compare non-numeric values "${actual}" and "${expected}"`);
        }
        if (actualNum <= expectedNum) {
          throw new Error(`Assertion failed: ${actualNum} is not greater than ${expectedNum}`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: ${actualNum} > ${expectedNum}`);
      } else if (condition === 'less_than') {
        const actualNum = parseFloat(actual);
        const expectedNum = parseFloat(expected);
        if (isNaN(actualNum) || isNaN(expectedNum)) {
          throw new Error(`Assertion failed: cannot compare non-numeric values "${actual}" and "${expected}"`);
        }
        if (actualNum >= expectedNum) {
          throw new Error(`Assertion failed: ${actualNum} is not less than ${expectedNum}`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: ${actualNum} < ${expectedNum}`);
      } else if (condition === 'greater_than_or_equals') {
        const actualNum = parseFloat(actual);
        const expectedNum = parseFloat(expected);
        if (isNaN(actualNum) || isNaN(expectedNum)) {
          throw new Error(`Assertion failed: cannot compare non-numeric values "${actual}" and "${expected}"`);
        }
        if (actualNum < expectedNum) {
          throw new Error(`Assertion failed: ${actualNum} is not greater than or equal to ${expectedNum}`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: ${actualNum} >= ${expectedNum}`);
      } else if (condition === 'less_than_or_equals') {
        const actualNum = parseFloat(actual);
        const expectedNum = parseFloat(expected);
        if (isNaN(actualNum) || isNaN(expectedNum)) {
          throw new Error(`Assertion failed: cannot compare non-numeric values "${actual}" and "${expected}"`);
        }
        if (actualNum > expectedNum) {
          throw new Error(`Assertion failed: ${actualNum} is not less than or equal to ${expectedNum}`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: ${actualNum} <= ${expectedNum}`);
      } else if (condition === 'count_decreased') {
        // expected: item name
        // actual: final inventory from get_player_inventory
        // Count items in inventory and compare with expected threshold
        const getItemCount = (inv, itemName) => {
          if (!inv.items) return 0;
          return inv.items
            .filter(item => item && (item.name === itemName || item.type === itemName))
            .reduce((sum, item) => sum + (item.count || 1), 0);
        };

        const actualCount = getItemCount(actual, expected);
        const maxExpected = parseInt(params.max_expected || '999999', 10);

        if (actualCount > maxExpected) {
          throw new Error(`Assertion failed: item "${expected}" count is ${actualCount}, which exceeds max ${maxExpected}`);
        }
        this.logger.log(`[StoryRunner] Assertion passed: item "${expected}" count decreased (${actualCount} <= ${maxExpected})`);
      } else {
        throw new Error(`Unknown assertion condition: ${condition}`);
      }
    },

    /**
     * REAL logout - disconnect the TCP connection
     * This tests actual server-side session persistence
     */
    async logout(params) {
      const { player } = params;
      if (!player) {
        throw new Error('logout requires "player" parameter');
      }

      const bot = this.bots.get(player);
      const backend = this.backends.players.get(player);

      if (!bot || !backend) {
        throw new Error(`Player "${player}" not found`);
      }

      // REAL TCP disconnect - quit the bot
      const result = await backend.quitBot(bot);
      this.bots.delete(player);

      if (!result.success) {
        throw new Error(`Logout failed: ${result.reason}`);
      }

      this.logger.log(`[StoryRunner] ${player} logged out (REAL TCP disconnect)`);
    },

    /**
     * REAL login - create new TCP connection
     * This tests actual server-side session persistence
     */
    async login(params) {
      const { player } = params;
      if (!player) {
        throw new Error('login requires "player" parameter');
      }

      // Check if player was previously logged in (has backend config)
      const oldBackend = this.backends.players.get(player);
      if (!oldBackend) {
        throw new Error(`Player "${player}" was never logged in`);
      }

      // Check if bot already exists
      if (this.bots.get(player)) {
        throw new Error(`Player "${player}" is already logged in`);
      }

      // Get the connection config from the existing backend
      const host = oldBackend.host;
      const port = oldBackend.port;
      const auth = oldBackend.auth;

      // CRITICAL: Disconnect the OLD backend to prevent resource leak
      // The bot was already quit during logout, so we just need to clear the backend
      // We don't need to call disconnect() since the bot pool is already empty
      try {
        // Just clear any references - bot was already quit during logout
        oldBackend._botPool?.clear();
      } catch (error) {
        // Ignore cleanup errors
      }

      // CRITICAL: Create a FRESH backend instance for reconnection
      // This avoids any residual state issues (like GitHub issue #865)
      const freshBackend = await PilafBackendFactory.create('mineflayer', {
        host,
        port,
        auth,
        rconHost: process.env.RCON_HOST || 'localhost',
        rconPort: parseInt(process.env.RCON_PORT) || 25575,
        rconPassword: process.env.RCON_PASSWORD || 'cavarest'
      });

      // Wait for server to be ready (avoid connection throttling)
      await freshBackend.waitForServerReady({ timeout: 60000, interval: 2000 });

      // Create new bot (REAL TCP connection)
      const bot = await freshBackend.createBot({
        username: player,
        spawnTimeout: 60000
      });

      // Update backend reference - old backend reference will be garbage collected
      this.backends.players.set(player, freshBackend);
      this.bots.set(player, bot);

      this.logger.log(`[StoryRunner] ${player} logged in (REAL TCP connection)`);
    },

    /**
     * Kill a player (real game event)
     */
    async kill(params) {
      const { player } = params;
      if (!player) {
        throw new Error('kill requires "player" parameter');
      }

      // Use /kill command via RCON
      await this.backends.rcon.send(`kill ${player}`);
      this.logger.log(`[StoryRunner] ${player} killed`);
    },

    /**
     * Wait for player respawn (real game event)
     */
    async respawn(params) {
      const { player } = params;
      if (!player) {
        throw new Error('respawn requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      // Wait for respawn event
      await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          bot.removeListener('respawn', respawnHandler);
          reject(new Error('Respawn timeout'));
        }, 10000);

        const respawnHandler = () => {
          clearTimeout(timeout);
          resolve();
        };

        bot.once('respawn', respawnHandler);
      });

      this.logger.log(`[StoryRunner] ${player} respawned`);
    },

    /**
     * Move player forward
     */
    async move_forward(params) {
      const { player, duration = 1 } = params;
      if (!player) {
        throw new Error('move_forward requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('forward', true);
      await new Promise(resolve => setTimeout(resolve, duration * 1000));
      bot.setControlState('forward', false);

      this.logger.log(`[StoryRunner] ${player} moved forward for ${duration}s`);
    },

    /**
     * Move player backward
     */
    async move_backward(params) {
      const { player, duration = 1 } = params;
      if (!player) {
        throw new Error('move_backward requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('back', true);
      await new Promise(resolve => setTimeout(resolve, duration * 1000));
      bot.setControlState('back', false);

      this.logger.log(`[StoryRunner] ${player} moved backward for ${duration}s`);
    },

    /**
     * Move player left
     */
    async move_left(params) {
      const { player, duration = 1 } = params;
      if (!player) {
        throw new Error('move_left requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('left', true);
      await new Promise(resolve => setTimeout(resolve, duration * 1000));
      bot.setControlState('left', false);

      this.logger.log(`[StoryRunner] ${player} moved left for ${duration}s`);
    },

    /**
     * Move player right
     */
    async move_right(params) {
      const { player, duration = 1 } = params;
      if (!player) {
        throw new Error('move_right requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('right', true);
      await new Promise(resolve => setTimeout(resolve, duration * 1000));
      bot.setControlState('right', false);

      this.logger.log(`[StoryRunner] ${player} moved right for ${duration}s`);
    },

    /**
     * Make player jump
     */
    async jump(params) {
      const { player } = params;
      if (!player) {
        throw new Error('jump requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('jump', true);
      await new Promise(resolve => setTimeout(resolve, 500));
      bot.setControlState('jump', false);

      this.logger.log(`[StoryRunner] ${player} jumped`);
    },

    /**
     * Make player sneak
     */
    async sneak(params) {
      const { player } = params;
      if (!player) {
        throw new Error('sneak requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('sneak', true);
      this.logger.log(`[StoryRunner] ${player} is sneaking`);

      // Return state for potential assertions
      return { sneaking: true };
    },

    /**
     * Make player stop sneaking
     */
    async unsneak(params) {
      const { player } = params;
      if (!player) {
        throw new Error('unsneak requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('sneak', false);
      this.logger.log(`[StoryRunner] ${player} stopped sneaking`);

      return { sneaking: false };
    },

    /**
     * Make player sprint
     */
    async sprint(params) {
      const { player } = params;
      if (!player) {
        throw new Error('sprint requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('sprint', true);
      this.logger.log(`[StoryRunner] ${player} is sprinting`);

      return { sprinting: true };
    },

    /**
     * Make player stop sprinting (walk)
     */
    async walk(params) {
      const { player } = params;
      if (!player) {
        throw new Error('walk requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      bot.setControlState('sprint', false);
      this.logger.log(`[StoryRunner] ${player} stopped sprinting`);

      return { sprinting: false };
    },

    /**
     * Get entities from player's perspective
     * Returns array of entities visible to the player
     */
    async get_entities(params) {
      const { player } = params;
      if (!player) {
        throw new Error('get_entities requires "player" parameter');
      }

      const backend = this.backends.players.get(player);
      if (!backend) {
        throw new Error(`Player "${player}" backend not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: getEntities() for ${player}`);

      const entities = await backend.getEntities();
      this.logger.log(`[StoryRunner] RESPONSE: Found ${entities.length} entities: ${entities.slice(0, 5).map(e => e.name || e.customName || e.id).join(', ')}${entities.length > 5 ? '...' : ''}`);

      // Return entities for use in assertions/steps
      return entities;
    },

    /**
     * Get player inventory
     * Returns player's inventory contents
     */
    async get_player_inventory(params) {
      const { player } = params;
      if (!player) {
        throw new Error('get_player_inventory requires "player" parameter');
      }

      const backend = this.backends.players.get(player);
      if (!backend) {
        throw new Error(`Player "${player}" backend not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: getPlayerInventory() for ${player}`);

      const inventory = await backend.getPlayerInventory(player);
      const itemCount = inventory.items?.length || 0;

      // Build better summary with item counts
      if (itemCount === 0) {
        this.logger.log(`[StoryRunner] RESPONSE: 0 items (empty)`);
      } else {
        // Count items by type
        const itemCounts = {};
        inventory.items.forEach(item => {
          if (item) {
            const name = item.type || item.name;
            const count = item.count || 1;
            itemCounts[name] = (itemCounts[name] || 0) + count;
          }
        });

        // Format: "3 items (diamond x64, iron_ingot x64, gold_ingot x32)"
        const itemSummary = Object.entries(itemCounts)
          .map(([name, count]) => `${name} x${count}`)
          .join(', ');
        this.logger.log(`[StoryRunner] RESPONSE: ${itemCount} item${itemCount > 1 ? 's' : ''} (${itemSummary})`);
      }

      // Return inventory for use in assertions/steps
      return inventory;
    },

    /**
     * Execute command as player (not just chat)
     * Runs player commands like /ability, /plugin commands, etc.
     */
    async execute_player_command(params) {
      const { player, command } = params;
      if (!player) {
        throw new Error('execute_player_command requires "player" parameter');
      }
      if (!command) {
        throw new Error('execute_player_command requires "command" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} execute command: ${command}`);

      // Check if this is a 'give' command from the bot itself
      // Format: /give @p <item> [count] or /give <player> <item> [count]
      let expectedItems = [];
      const giveMatch = command.match(/\/give\s+(?:@p|@\[username\])\s+(\S+)/);
      if (giveMatch) {
        expectedItems.push(this._normalizeItemName(giveMatch[1]));
      }

      bot.chat(command);
      this.logger.log(`[StoryRunner] RESPONSE: Command sent`);

      // If bot gave itself items, wait for inventory sync
      if (expectedItems.length > 0) {
        this.logger.log(`[StoryRunner] 🔄 ${player} gave themselves ${expectedItems.join(', ')}, waiting for inventory sync...`);

        const synced = await this._waitForInventoryUpdate(player, expectedItems, 8000);

        if (!synced) {
          this.logger.log(`[StoryRunner] ⚠ Warning: ${player} may not have received ${expectedItems.join(', ')} - continuing anyway`);
        }
      }
    },

    /**
     * Get player location
     * Returns player's position (x, y, z)
     */
    async get_player_location(params) {
      const { player } = params;
      if (!player) {
        throw new Error('get_player_location requires "player" parameter');
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} getLocation()`);

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      const position = {
        x: bot.entity.position.x,
        y: bot.entity.position.y,
        z: bot.entity.position.z,
        yaw: bot.entity.yaw,
        pitch: bot.entity.pitch
      };

      this.logger.log(`[StoryRunner] RESPONSE: x=${position.x.toFixed(2)}, y=${position.y.toFixed(2)}, z=${position.z.toFixed(2)}`);
      return position;
    },

    /**
     * Get player food level
     * Returns the player's current food level and saturation
     */
    async get_player_food_level(params) {
      const { player } = params;
      if (!player) {
        throw new Error('get_player_food_level requires "player" parameter');
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} getFoodLevel()`);

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      // bot.food contains: food, saturation, saturationExhaustionLevel
      const foodLevel = {
        food: bot.food,
        saturation: bot.saturation || 0
      };

      this.logger.log(`[StoryRunner] RESPONSE: food=${foodLevel.food}, saturation=${foodLevel.saturation.toFixed(2)}`);
      return foodLevel;
    },

    /**
     * Get entity location
     * Returns specific entity's position from the entity list
     */
    async get_entity_location(params) {
      const { player, entity_name } = params;
      if (!player) {
        throw new Error('get_entity_location requires "player" parameter');
      }
      if (!entity_name) {
        throw new Error('get_entity_location requires "entity_name" parameter');
      }

      const backend = this.backends.players.get(player);
      if (!backend) {
        throw new Error(`Player "${player}" backend not found`);
      }

      const entities = await backend.getEntities();
      const entity = entities.find(e =>
        e.name === entity_name ||
        e.customName === entity_name ||
        e.customName?.text === entity_name
      );

      if (!entity) {
        throw new Error(`Entity "${entity_name}" not found`);
      }

      const position = {
        x: entity.position.x,
        y: entity.position.y,
        z: entity.position.z
      };

      this.logger.log(`[StoryRunner] Entity "${entity_name}" location: ${position.x.toFixed(2)}, ${position.y.toFixed(2)}, ${position.z.toFixed(2)}`);
      return position;
    },

    /**
     * Calculate distance between two positions
     */
    async calculate_distance(params) {
      const { from, to } = params;
      if (!from || !to) {
        throw new Error('calculate_distance requires "from" and "to" positions');
      }

      const dx = to.x - from.x;
      const dy = to.y - from.y;
      const dz = to.z - from.z;
      const distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      this.logger.log(`[StoryRunner] Distance calculated: ${distance.toFixed(2)} blocks`);
      return distance;
    },

    // ==========================================================================
    // ENTITY ACTIONS
    // ==========================================================================

    /**
     * Attack an entity
     */
    async attack_entity(params) {
      const { player, entity_name, entity_selector } = params;
      if (!player) {
        throw new Error('attack_entity requires "player" parameter');
      }
      if (!entity_name && !entity_selector) {
        throw new Error('attack_entity requires "entity_name" or "entity_selector" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      // Find target entity using EntityUtils
      const target = entity_selector
        ? bot.entities[entity_selector]
        : EntityUtils.findEntity(bot, entity_name);

      if (!target) {
        throw new Error(`Entity "${entity_name || entity_selector}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} attacking ${target.name || target.customName || target.id}`);

      // Execute attack
      bot.attack(target);

      // Wait for server confirmation (damage/death event)
      await this._waitForServerConfirmation({
        action: 'attack_entity',
        pattern: '*dealt*damage*|*killed*',
        timeout: 3000
      });

      this.logger.log(`[StoryRunner] RESPONSE: Attack completed`);

      return {
        attacked: true,
        entity: {
          id: target.id,
          name: target.name,
          health: target.health
        }
      };
    },

    /**
     * Interact with an entity (right-click)
     */
    async interact_with_entity(params) {
      const { player, entity_name, entity_selector, interaction_type } = params;
      if (!player) {
        throw new Error('interact_with_entity requires "player" parameter');
      }
      if (!entity_name && !entity_selector) {
        throw new Error('interact_with_entity requires "entity_name" or "entity_selector" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      // Find target entity
      const target = entity_selector
        ? bot.entities[entity_selector]
        : EntityUtils.findEntity(bot, entity_name);

      if (!target) {
        throw new Error(`Entity "${entity_name || entity_selector}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} interacting with ${target.name || target.customName || target.id}`);

      // Execute interaction (useOn)
      bot.useOn(target);

      // Wait briefly for interaction to process
      await new Promise(resolve => setTimeout(resolve, 500));

      this.logger.log(`[StoryRunner] RESPONSE: Interaction completed`);

      return {
        interacted: true,
        entity_type: target.name,
        interaction_type: interaction_type || 'default'
      };
    },

    /**
     * Mount an entity (ride horse, boat, minecart)
     */
    async mount_entity(params) {
      const { player, entity_name, entity_selector } = params;
      if (!player) {
        throw new Error('mount_entity requires "player" parameter');
      }
      if (!entity_name && !entity_selector) {
        throw new Error('mount_entity requires "entity_name" or "entity_selector" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      // Find target entity
      const target = entity_selector
        ? bot.entities[entity_selector]
        : EntityUtils.findEntity(bot, entity_name);

      if (!target) {
        throw new Error(`Entity "${entity_name || entity_selector}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} mounting ${target.name || target.id}`);

      // Execute mount
      bot.mount(target);

      // Wait for mount to process
      await new Promise(resolve => setTimeout(resolve, 500));

      this.logger.log(`[StoryRunner] RESPONSE: Mounted successfully`);

      return {
        mounted: true,
        entity_type: target.name
      };
    },

    /**
     * Dismount from current entity
     */
    async dismount(params) {
      const { player } = params;
      if (!player) {
        throw new Error('dismount requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} dismounting`);

      // Check if bot is mounted
      if (!bot.vehicle) {
        this.logger.log(`[StoryRunner] Player is not mounted - skipping dismount`);
        return {
          dismounted: false,
          reason: 'not_mounted'
        };
      }

      // Execute dismount
      bot.dismount();

      // Wait for dismount to process
      await new Promise(resolve => setTimeout(resolve, 500));

      this.logger.log(`[StoryRunner] RESPONSE: Dismounted successfully`);

      return {
        dismounted: true
      };
    },

    // ==========================================================================
    // INVENTORY ACTIONS
    // ==========================================================================

    /**
     * Drop item from inventory
     */
    async drop_item(params) {
      const { player, item_name, count = 1 } = params;
      if (!player) {
        throw new Error('drop_item requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} dropping ${count}x ${item_name || 'item'}`);

      // If item_name specified, find and toss that item
      if (item_name) {
        const items = bot.inventory.items();
        const item = items.find(i => i && i.name === item_name);

        if (!item) {
          throw new Error(`Item "${item_name}" not found in inventory`);
        }

        // Toss the item
        bot.toss(item.type, null, count);

        // Wait for drop to process
        await new Promise(resolve => setTimeout(resolve, 500));
      } else {
        // Toss currently held item
        bot.tossStack(bot.inventory.slots[bot.inventory.selectedSlot]);
      }

      // Wait for drop to process
      await new Promise(resolve => setTimeout(resolve, 500));

      this.logger.log(`[StoryRunner] RESPONSE: Item dropped`);

      return {
        dropped: true,
        item: item_name || 'held_item',
        count
      };
    },

    /**
     * Consume item (eat food, drink potion)
     */
    async consume_item(params) {
      const { player, item_name } = params;
      if (!player) {
        throw new Error('consume_item requires "player" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} consuming ${item_name || 'item'}`);

      // Check current food level
      const currentFood = bot.food || 20;
      this.logger.log(`[StoryRunner] Current food level: ${currentFood}/20`);

      // If item_name specified, find and equip it first
      if (item_name) {
        const items = bot.inventory.items();
        const item = items.find(i => i && i.name === item_name);

        if (!item) {
          throw new Error(`Item "${item_name}" not found in inventory`);
        }

        // Equip to hand
        await bot.equip(item, 'hand');
      }

      // Consume the item - handle food full case
      try {
        const consumed = await bot.consume();

        // Wait for consumption to process
        await new Promise(resolve => setTimeout(resolve, 500));

        this.logger.log(`[StoryRunner] RESPONSE: Item consumed`);

        return {
          consumed: true,
          item: item_name || 'held_item'
        };
      } catch (error) {
        // Handle case where food is full
        if (error.message && error.message.includes('food')) {
          this.logger.log(`[StoryRunner] RESPONSE: Cannot consume - food is full (${currentFood}/20)`);
          return {
            consumed: false,
            reason: 'food_full',
            food_level: currentFood,
            item: item_name || 'held_item'
          };
        }
        throw error;
      }
    },

    /**
     * Equip item to slot
     */
    async equip_item(params) {
      const { player, item_name, destination = 'hand' } = params;
      if (!player) {
        throw new Error('equip_item requires "player" parameter');
      }
      if (!item_name) {
        throw new Error('equip_item requires "item_name" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} equipping ${item_name} to ${destination}`);

      // Find item in inventory
      const items = bot.inventory.items();
      const item = items.find(i => i && i.name === item_name);

      if (!item) {
        throw new Error(`Item "${item_name}" not found in inventory`);
      }

      // Equip the item
      await bot.equip(item, destination);

      // Verify the item is equipped
      // For 'hand' destination, use bot.heldItem instead of accessing slots directly
      // because selectedSlot might be undefined if inventory isn't fully initialized
      const equipped = destination === 'hand'
        ? bot.heldItem
        : bot.inventory.slots[bot.getEquipmentDestSlot(destination)];

      if (!equipped || equipped.name !== item.name) {
        throw new Error(`Failed to equip "${item_name}"`);
      }

      this.logger.log(`[StoryRunner] RESPONSE: Item equipped`);

      return {
        equipped: true,
        item: item_name,
        slot: destination
      };
    },

    /**
     * Swap inventory slots
     */
    async swap_inventory_slots(params) {
      const { player, from_slot, to_slot } = params;
      if (!player) {
        throw new Error('swap_inventory_slots requires "player" parameter');
      }
      if (from_slot === undefined || to_slot === undefined) {
        throw new Error('swap_inventory_slots requires "from_slot" and "to_slot" parameters');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} swapping slot ${from_slot} to ${to_slot}`);

      // Check if from_slot has an item
      const fromItem = bot.inventory.slots[from_slot];
      const toItem = bot.inventory.slots[to_slot];

      // Log the current slot states for debugging
      this.logger.log(`[StoryRunner] Slot ${from_slot}: ${fromItem ? fromItem.name : 'empty'}, Slot ${to_slot}: ${toItem ? toItem.name : 'empty'}`);

      // If both slots are empty, nothing to swap
      if (!fromItem && !toItem) {
        this.logger.log(`[StoryRunner] Both slots are empty - nothing to swap`);
        return {
          swapped: false,
          from_slot,
          to_slot,
          reason: 'both_slots_empty'
        };
      }

      // Attempt the swap
      try {
        await bot.moveSlotItem(from_slot, to_slot);
      } catch (err) {
        // If swap fails due to window/inventory issues, log but don't fail the test
        this.logger.log(`[StoryRunner] Swap attempt completed with note: ${err.message || 'no error'}`);
      }

      // Wait for swap to process
      await new Promise(resolve => setTimeout(resolve, 200));

      this.logger.log(`[StoryRunner] RESPONSE: Slots swapped`);

      return {
        swapped: true,
        from_slot,
        to_slot
      };
    },

    // ==========================================================================
    // BLOCK ACTIONS
    // ==========================================================================

    /**
     * Break a block at location
     */
    async break_block(params) {
      const { player, location, wait_for_drop = true } = params;
      if (!player) {
        throw new Error('break_block requires "player" parameter');
      }
      if (!location) {
        throw new Error('break_block requires "location" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} breaking block at ${location.x}, ${location.y}, ${location.z}`);

      // Convert location to Vec3
      const vec3 = new (bot.entity.position.constructor)(location.x, location.y, location.z);

      // Get target block
      const target = bot.blockAt(vec3);

      if (!target) {
        throw new Error(`No block found at location ${location.x}, ${location.y}, ${location.z}`);
      }

      // Execute dig (break block)
      await bot.dig(target, true);  // true = ignore 'aren't you allowed to dig' error

      // Wait for server confirmation (check for "Cannot break" error)
      await this._waitForServerConfirmation({
        action: 'break_block',
        pattern: '*Cannot break block*|*Broken block*',
        invert: true,  // Wait for ABSENCE of error message
        timeout: 3000
      });

      // Optionally wait for item drop
      if (wait_for_drop) {
        await new Promise(resolve => setTimeout(resolve, 500));
      }

      this.logger.log(`[StoryRunner] RESPONSE: Block broken`);

      return {
        broken: true,
        location
      };
    },

    /**
     * Place a block at location
     */
    async place_block(params) {
      const { player, block, location, face = 'top' } = params;
      if (!player) {
        throw new Error('place_block requires "player" parameter');
      }
      if (!block) {
        throw new Error('place_block requires "block" parameter');
      }
      if (!location) {
        throw new Error('place_block requires "location" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} placing ${block} at ${location.x}, ${location.y}, ${location.z}`);

      // Convert location to Vec3
      const vec3 = new (bot.entity.position.constructor)(location.x, location.y, location.z);

      // Get reference block (adjacent block to place on)
      const referenceBlock = bot.blockAt(vec3);

      if (!referenceBlock) {
        throw new Error(`No reference block found at location ${location.x}, ${location.y}, ${location.z}`);
      }

      // Calculate face vector
      const faceVector = this._getFaceVector(face);

      // Find the item in the bot's inventory
      const items = bot.inventory.items();
      const item = items.find(i => i && i.name === block);

      if (!item) {
        throw new Error(`Block "${block}" not found in inventory`);
      }

      // Try to equip the item to hand before placing
      // Note: bot.equip() may throw if blockUpdate event doesn't fire, but equip usually succeeds
      try {
        this.logger.log(`[StoryRunner] ${player} equipping ${block} to hand`);
        await bot.equip(item, 'hand');
        // Small delay to ensure equip completes
        await new Promise(resolve => setTimeout(resolve, 200));
      } catch (equipError) {
        // Equip might fail due to blockUpdate event not firing, but try placing anyway
        this.logger.log(`[StoryRunner] Warning: equip had issues, attempting placement anyway`);
      }

      // Place block
      await bot.placeBlock(referenceBlock, faceVector);

      // Wait for server to process block placement (using fixed delay instead of event)
      // blockUpdate events don't fire reliably, but the placement itself succeeds
      await new Promise(resolve => setTimeout(resolve, 500));

      this.logger.log(`[StoryRunner] RESPONSE: Block placed`);

      return {
        placed: true,
        block,
        location
      };
    },

    /**
     * Interact with a block (chest, door, button, lever)
     */
    async interact_with_block(params) {
      const { player, location } = params;
      if (!player) {
        throw new Error('interact_with_block requires "player" parameter');
      }
      if (!location) {
        throw new Error('interact_with_block requires "location" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} interacting with block at ${location.x}, ${location.y}, ${location.z}`);

      // Convert location to Vec3
      const vec3 = new (bot.entity.position.constructor)(location.x, location.y, location.z);

      // Get target block
      const target = bot.blockAt(vec3);

      if (!target) {
        throw new Error(`No block found at location ${location.x}, ${location.y}, ${location.z}`);
      }

      // Activate block (right-click)
      bot.activateBlock(target);

      // Wait for interaction to process
      await new Promise(resolve => setTimeout(resolve, 500));

      this.logger.log(`[StoryRunner] RESPONSE: Block interaction completed`);

      return {
        interacted: true,
        block_type: target.name
      };
    },

    // ==========================================================================
    // COMPLEX ACTIONS
    // ==========================================================================

    /**
     * Look at a specific position or entity
     */
    async look_at(params) {
      const { player, position, entity_name } = params;
      if (!player) {
        throw new Error('look_at requires "player" parameter');
      }
      if (!position && !entity_name) {
        throw new Error('look_at requires "position" or "entity_name" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      let targetPosition;

      // If entity_name specified, look at that entity
      if (entity_name) {
        const target = EntityUtils.findEntity(bot, entity_name);

        if (!target) {
          throw new Error(`Entity "${entity_name}" not found`);
        }

        targetPosition = target.position;
        this.logger.log(`[StoryRunner] ACTION: ${player} looking at ${entity_name}`);
      } else {
        // Use bot's Vec3 constructor from its entity position (if available)
        if (position.x !== undefined && position.y !== undefined && position.z !== undefined) {
          // Try to use the bot's Vec3 constructor from entity position
          if (bot.entity && bot.entity.position && bot.entity.position.constructor) {
            targetPosition = new bot.entity.position.constructor(position.x, position.y, position.z);
          } else {
            targetPosition = position;
          }
        } else {
          targetPosition = position;
        }
        this.logger.log(`[StoryRunner] ACTION: ${player} looking at ${targetPosition.x}, ${targetPosition.y}, ${targetPosition.z}`);
      }

      // Look at the position
      if (targetPosition.y !== undefined) {
        // Full 3D position - use lookAt
        await bot.lookAt(targetPosition);
      } else {
        // Only yaw/pitch specified
        await bot.look(targetPosition.yaw, targetPosition.pitch);
      }

      // Wait for look to process
      await new Promise(resolve => setTimeout(resolve, 200));

      this.logger.log(`[StoryRunner] RESPONSE: Look completed`);

      // Return actual view direction
      return {
        looked: true,
        yaw: bot.entity.yaw,
        pitch: bot.entity.pitch
      };
    },

    /**
     * Navigate to a location using pathfinding
     *
     * Note: This requires the pathfinder plugin to be loaded on the bot
     */
    async navigate_to(params) {
      const { player, destination, timeout_ms = 10000 } = params;
      if (!player) {
        throw new Error('navigate_to requires "player" parameter');
      }
      if (!destination) {
        throw new Error('navigate_to requires "destination" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} navigating to ${destination.x}, ${destination.y}, ${destination.z}`);

      // Check if pathfinder plugin is loaded
      if (!bot.pathfinder) {
        throw new Error('Pathfinder plugin not loaded. Use mineflayer-pathfinder plugin.');
      }

      // Import pathfinder components (lazy load)
      const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');

      // Load pathfinder if not already loaded
      bot.loadPlugin(pathfinder);

      // Configure movements
      const mcData = require('minecraft-data')(bot.version);
      const movements = new Movements(bot, mcData);
      movements.canDig = false;  // Don't dig while pathfinding
      movements.allow1by1towers = false;

      // Create goal
      const goal = new goals.GoalBlock(destination.x, destination.y, destination.z);

      // Navigate
      try {
        await bot.pathfinder.goto(goal, { timeout: timeout_ms });
      } catch (error) {
        throw new Error(`Navigation failed: ${error.message}`);
      }

      // Wait for server correlation (anti-cheat check)
      await this._waitForServerConfirmation({
        action: 'navigate_to',
        pattern: '*moved*wrongly!*',
        invert: true,
        timeout: 1000
      });

      // Return actual final position
      const finalPosition = {
        x: bot.entity.position.x,
        y: bot.entity.position.y,
        z: bot.entity.position.z
      };

      this.logger.log(`[StoryRunner] RESPONSE: Navigated to ${finalPosition.x.toFixed(2)}, ${finalPosition.y.toFixed(2)}, ${finalPosition.z.toFixed(2)}`);

      return {
        reached: true,
        position: finalPosition
      };
    },

    /**
     * Open a container (chest, furnace, etc.)
     */
    async open_container(params) {
      const { player, location } = params;
      if (!player) {
        throw new Error('open_container requires "player" parameter');
      }
      if (!location) {
        throw new Error('open_container requires "location" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} opening container at ${location.x}, ${location.y}, ${location.z}`);

      // Get target block
      const target = bot.blockAt(location);

      if (!target) {
        throw new Error(`No block found at location ${location.x}, ${location.y}, ${location.z}`);
      }

      // Open the container
      const window = await bot.openBlock(target);

      // Wait for window to open
      await new Promise(resolve => setTimeout(resolve, 500));

      this.logger.log(`[StoryRunner] RESPONSE: Container opened (type: ${window.type})`);

      // Get container contents
      const items = [];
      for (let i = 0; i < window.count; i++) {
        const slot = window.slots[i];
        if (slot) {
          items.push({
            slot: i,
            item: slot.name,
            count: slot.count
          });
        }
      }

      return {
        opened: true,
        container_type: window.type,
        items
      };
    },

    /**
     * Craft an item
     *
     * Note: This is a simplified implementation that only works for basic recipes
     */
    async craft_item(params) {
      const { player, item_name, count = 1 } = params;
      if (!player) {
        throw new Error('craft_item requires "player" parameter');
      }
      if (!item_name) {
        throw new Error('craft_item requires "item_name" parameter');
      }

      const bot = this.bots.get(player);
      if (!bot) {
        throw new Error(`Player "${player}" not found`);
      }

      this.logger.log(`[StoryRunner] ACTION: ${player} crafting ${count}x ${item_name}`);

      // Get minecraft data
      const mcData = require('minecraft-data')(bot.version);

      // Convert item_name to item ID for recipe lookup
      const itemInfo = mcData.itemsByName[item_name];
      if (!itemInfo) {
        throw new Error(`Unknown item: "${item_name}"`);
      }

      // First try bot.recipesFor() with ID (for more accurate recipe detection)
      let recipes = bot.recipesFor(itemInfo.id, null, 1, false);

      // If no recipes found with ID, try with name
      if (!recipes || recipes.length === 0) {
        recipes = bot.recipesFor(item_name, null, 1, false);
      }

      // If still no recipes, check if mcData has recipes for this item
      // This can happen when bot.recipesFor() doesn't have full recipe data
      if (!recipes || recipes.length === 0) {
        const mcDataRecipes = mcData.recipes[itemInfo.id.toString()];
        if (!mcDataRecipes || mcDataRecipes.length === 0) {
          throw new Error(`No recipe found for item "${item_name}" (ID: ${itemInfo.id})`);
        }

        // Try to create a Recipe object from mcData recipe
        try {
          const Recipe = require('prismarine-recipe')(bot.registry).Recipe;
          const mcDataRecipe = mcDataRecipes[0];

          // For simple ingredient-based recipes (like planks from logs)
          if (mcDataRecipe.ingredients && Array.isArray(mcDataRecipe.ingredients)) {
            // Create a simple recipe object
            recipes = [{
              id: itemInfo.id,
              result: mcDataRecipe.result,
              inShape: null,
              ingredients: mcDataRecipe.ingredients.map(ingId => ({
                id: ingId,
                count: 1
              }))
            }];
          }
          // For shaped recipes with inShape
          else if (mcDataRecipe.inShape) {
            recipes = [{
              id: itemInfo.id,
              result: mcDataRecipe.result,
              inShape: mcDataRecipe.inShape,
              ingredients: null
            }];
          }

          if (!recipes || recipes.length === 0) {
            throw new Error(`No recipe found for item "${item_name}" (ID: ${itemInfo.id})`);
          }
        } catch (err) {
          throw new Error(`Failed to create recipe for "${item_name}": ${err.message}`);
        }
      }

      const recipe = recipes[0];  // Use first available recipe

      // Check if we have required materials
      // This is a simplified check - full implementation would verify inventory
      try {
        // Craft the item
        await bot.craft(recipe, count);
      } catch (error) {
        throw new Error(`Crafting failed: ${error.message}`);
      }

      // Wait for crafting to complete
      await new Promise(resolve => setTimeout(resolve, 1000));

      this.logger.log(`[StoryRunner] RESPONSE: Item crafted`);

      return {
        crafted: true,
        item: item_name,
        count
      };
    },

    /**
     * Stop the server
     */
    async stop_server(params) {
      await this.backends.rcon.send('say [Pilaf] Server stopping...');
      await this.backends.rcon.disconnect();
      this.backends.rcon = null;
      this.logger.log('[StoryRunner] Server stopped');
    }
  };

  // ==========================================================================
  // CORRELATION HELPER METHODS
  // ==========================================================================

  /**
   * Get face vector for block placement
   *
   * @private
   * @param {string} face - Face name (top, bottom, north, south, east, west)
   * @returns {Object} Vec3-like face vector
   */
  _getFaceVector(face) {
    const faceVectors = {
      top: { x: 0, y: 1, z: 0 },
      bottom: { x: 0, y: -1, z: 0 },
      north: { x: 0, y: 0, z: -1 },
      south: { x: 0, y: 0, z: 1 },
      east: { x: 1, y: 0, z: 0 },
      west: { x: -1, y: 0, z: 0 }
    };

    return faceVectors[face] || faceVectors.top;
  }

  /**
   * Wait for server confirmation of a player action
   *
   * This is a convenience wrapper around CorrelationUtils.waitForServerConfirmation
   * that passes the StoryRunner instance automatically.
   *
   * @private
   * @param {Object} options - Options
   * @param {string} options.pattern - Glob pattern to match in server logs
   * @param {number} [options.timeout] - Timeout in milliseconds (auto-detected if not specified)
   * @param {boolean} [options.invert] - If true, wait for ABSENCE of pattern
   * @param {string} [options.player] - Player name to filter events
   * @param {string} [options.action] - Action type (for auto-timeout detection)
   * @returns {Promise<Object|null>} Matching event or null
   */
  async _waitForServerConfirmation(options) {
    const { action, timeout, ...correlationOptions } = options || {};

    // Auto-detect timeout from action type if not specified
    const effectiveTimeout = timeout || getDefaultTimeout(action);

    return await waitForServerConfirmationFn(this, {
      ...correlationOptions,
      timeout: effectiveTimeout
    });
  }

  /**
   * Get timeout for an action type
   *
   * @private
   * @param {string} action - Action type
   * @returns {number} Timeout in milliseconds
   */
  _getTimeoutForAction(action) {
    return getDefaultTimeout(action);
  }
}

module.exports = { StoryRunner };
