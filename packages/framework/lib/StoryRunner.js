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
        // Check for {variableName} pattern
        const match = value.match(/^\{(.+)\}$/);
        if (match) {
          const varName = match[1];
          if (!this.variables.has(varName)) {
            throw new Error(`Variable "${varName}" not found. Available variables: ${[...this.variables.keys()].join(', ')}`);
          }
          return this.variables.get(varName);
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

      const result = await this.backends.rcon.send(command);
      this.logger.log(`[StoryRunner] RESPONSE: ${result.raw}`);
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
      const itemSummary = itemCount > 0
        ? inventory.items.slice(0, 5).map(i => i.type || i.name).join(', ') + (itemCount > 5 ? '...' : '')
        : 'empty';
      this.logger.log(`[StoryRunner] RESPONSE: ${itemCount} items (${itemSummary})`);

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
      bot.chat(command);
      this.logger.log(`[StoryRunner] RESPONSE: Command sent`);
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
}

module.exports = { StoryRunner };
