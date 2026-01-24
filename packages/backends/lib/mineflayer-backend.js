/**
 * MineflayerBackend - Mineflayer implementation for Pilaf backend
 * Refactored with proper OOP architecture and separation of concerns
 */

const mineflayer = require('mineflayer');
const { PilafBackend } = require('./backend.js');
const { BotPool } = require('./BotPool.js');
const { BotLifecycleManager } = require('./BotLifecycleManager.js');
const { ServerHealthChecker } = require('./ServerHealthChecker.js');
const { QueryHelper } = require('./helpers/QueryHelper.js');
const { EventObserver } = require('./helpers/EventObserver.js');

class MineflayerBackend extends PilafBackend {
  /**
   * Create a MineflayerBackend
   * @param {Object} options - Options
   */
  constructor(options = {}) {
    super();
    this._host = null;
    this._port = null;
    this._auth = null;
    this._connectConfig = {};
    this._botPool = new BotPool();
    this._healthChecker = null;
    this._queryHelper = null;
    this._eventObserver = null;
  }

  /**
   * Get server host
   * @returns {string}
   */
  get host() {
    return this._host;
  }

  /**
   * Get server port
   * @returns {number}
   */
  get port() {
    return this._port;
  }

  /**
   * Get authentication mode
   * @returns {string}
   */
  get auth() {
    return this._auth;
  }

  /**
   * Connect to server
   * @param {Object} config - Configuration object
   * @param {string} config.host - Server host
   * @param {number} config.port - Server port
   * @param {string} [config.auth] - Auth mode ('offline' or 'mojang')
   * @param {string} [config.rconHost] - RCON host (for health checks, defaults to host)
   * @param {number} [config.rconPort] - RCON port (for health checks, default: 25575)
   * @param {string} [config.rconPassword] - RCON password (for health checks, default: 'cavarest')
   * @returns {Promise<this>}
   */
  async connect(config) {
    this._host = config?.host || 'localhost';
    this._port = config?.port || 25565;
    this._auth = config?.auth || 'offline';

    // Store complete connection config
    this._connectConfig = {
      host: this._host,
      port: this._port,
      auth: this._auth,
      ...config
    };

    // Initialize health checker with RCON config for fast checks
    this._healthChecker = new ServerHealthChecker({
      host: this._host,
      port: this._port,
      auth: this._auth,
      rconHost: config?.rconHost || this._host,
      rconPort: config?.rconPort || 25575,
      rconPassword: config?.rconPassword || 'cavarest'
    });

    // Initialize query helper with the health checker's RCON backend
    this._queryHelper = new QueryHelper(this._healthChecker.getRconBackend());

    return this;
  }

  /**
   * Disconnect all bots
   * @returns {Promise<void>}
   */
  async disconnect() {
    const results = await this._botPool.quitAll({ disconnectTimeout: 10000 });
    this._botPool.clear();
  }

  /**
   * Send command via default bot
   * @param {string} command - Command to send
   * @returns {Promise<{raw: string, parsed?: any}>}
   */
  async sendCommand(command) {
    const readyBots = this._botPool.getReadyBots();

    if (readyBots.length === 0) {
      throw new Error('No bot available. Create a bot first using createBot()');
    }

    const bot = readyBots[0].bot;

    try {
      bot.chat(command);
      return { raw: '' };
    } catch (error) {
      throw new Error(`Failed to send command: ${error.message}`);
    }
  }

  /**
   * Create a Mineflayer bot with proper lifecycle management
   * @param {Object} options - Bot options
   * @param {string} options.username - Bot username (required)
   * @param {string} [options.host] - Server host (optional, uses connect config)
   * @param {number} [options.port] - Server port (optional, uses connect config)
   * @param {string} [options.auth] - Auth mode (optional, uses connect config)
   * @param {number} [options.spawnTimeout] - Max time to wait for spawn (default: 30000ms)
   * @param {Object} [options] - Additional mineflayer options
   * @returns {Promise<Object>} Bot instance
   */
  async createBot(options) {
    const username = options?.username;
    if (!username) {
      throw new Error('username is required in options');
    }

    if (this._botPool.has(username)) {
      throw new Error(`Bot "${username}" already exists`);
    }

    // Merge connect config with bot options (bot options take precedence)
    const botConfig = {
      ...this._connectConfig,
      ...options
    };

    // Create and spawn bot using lifecycle manager
    const { bot } = await BotLifecycleManager.createAndSpawnBot(
      () => mineflayer.createBot(botConfig),
      botConfig
    );

    // Add to pool
    this._botPool.add(username, bot);

    return bot;
  }

  /**
   * Quit a bot and remove from pool
   * @param {Object} bot - Bot instance to quit
   * @returns {Promise<{success: boolean, reason: string}>}
   */
  async quitBot(bot) {
    // Find username by bot reference
    for (const [username, entry] of this._botPool.bots.entries()) {
      if (entry.bot === bot) {
        return await this._botPool.quitBot(username);
      }
    }

    return { success: false, reason: 'Bot not found in pool' };
  }

  /**
   * Check if server is ready for bot connections
   * @param {Object} options - Options
   * @param {number} [options.timeout] - Total timeout (default: 120000ms)
   * @returns {Promise<{success: boolean, checks: number, totalLatency: number}>}
   */
  async waitForServerReady(options = {}) {
    if (!this._healthChecker) {
      throw new Error('Backend not connected. Call connect() first.');
    }

    return await this._healthChecker.waitForReady(options);
  }

  /**
   * Get health checker instance
   * @returns {ServerHealthChecker}
   */
  getHealthChecker() {
    return this._healthChecker;
  }

  /**
   * Get bot pool instance
   * @returns {BotPool}
   */
  getBotPool() {
    return this._botPool;
  }

  /**
   * Get query helper instance
   * @returns {QueryHelper}
   */
  getQueryHelper() {
    return this._queryHelper;
  }

  /**
   * Get event observer instance
   * @returns {EventObserver}
   */
  getEventObserver() {
    return this._eventObserver;
  }

  // ==========================================================================
  // QUERY HELPER METHODS
  // ==========================================================================

  /**
   * Get player information via RCON
   * @param {string} username - Player username
   * @returns {Promise<Object>} Player information
   */
  async getPlayerInfo(username) {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.getPlayerInfo(username);
  }

  /**
   * List all online players
   * @returns {Promise<Object>} List of players
   */
  async listPlayers() {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.listPlayers();
  }

  /**
   * Get current world time
   * @returns {Promise<Object>} World time information
   */
  async getWorldTime() {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.getWorldTime();
  }

  /**
   * Get current weather
   * @returns {Promise<Object>} Weather information
   */
  async getWeather() {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.getWeather();
  }

  /**
   * Get difficulty level
   * @returns {Promise<Object>} Difficulty information
   */
  async getDifficulty() {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.getDifficulty();
  }

  /**
   * Get game mode
   * @param {string} [player='@s'] - Target player selector
   * @returns {Promise<Object>} Game mode information
   */
  async getGameMode(player = '@s') {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.getGameMode(player);
  }

  /**
   * Get server TPS
   * @returns {Promise<Object>} TPS information
   */
  async getTPS() {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.getTPS();
  }

  /**
   * Get world seed
   * @returns {Promise<Object>} Seed information
   */
  async getSeed() {
    if (!this._queryHelper) {
      throw new Error('Backend not connected. Call connect() first.');
    }
    return await this._queryHelper.getSeed();
  }

  // ==========================================================================
  // EVENT OBSERVATION METHODS
  // ==========================================================================

  /**
   * Subscribe to server events
   * @param {string} pattern - Event pattern (supports wildcards)
   * @param {Function} callback - Event callback
   * @returns {Function} Unsubscribe function
   */
  onEvent(pattern, callback) {
    this._ensureEventObserver();
    return this._eventObserver.onEvent(pattern, callback);
  }

  /**
   * Subscribe to player join events
   * @param {Function} callback - Event callback
   * @returns {Function} Unsubscribe function
   */
  onPlayerJoin(callback) {
    this._ensureEventObserver();
    return this._eventObserver.onPlayerJoin(callback);
  }

  /**
   * Subscribe to player leave events
   * @param {Function} callback - Event callback
   * @returns {Function} Unsubscribe function
   */
  onPlayerLeave(callback) {
    this._ensureEventObserver();
    return this._eventObserver.onPlayerLeave(callback);
  }

  /**
   * Subscribe to player death events
   * @param {Function} callback - Event callback
   * @returns {Function} Unsubscribe function
   */
  onPlayerDeath(callback) {
    this._ensureEventObserver();
    return this._eventObserver.onPlayerDeath(callback);
  }

  /**
   * Subscribe to command events
   * @param {Function} callback - Event callback
   * @returns {Function} Unsubscribe function
   */
  onCommand(callback) {
    this._ensureEventObserver();
    return this._eventObserver.onCommand(callback);
  }

  /**
   * Subscribe to world events
   * @param {Function} callback - Event callback
   * @returns {Function} Unsubscribe function
   */
  onWorldEvent(callback) {
    this._ensureEventObserver();
    return this._eventObserver.onWorldEvent(callback);
  }

  /**
   * Start observing server events
   * @returns {Promise<void>}
   */
  async observe() {
    this._ensureEventObserver();
    await this._eventObserver.start();
  }

  /**
   * Stop observing server events
   * @returns {void}
   */
  unobserve() {
    if (this._eventObserver) {
      this._eventObserver.stop();
    }
  }

  /**
   * Check if currently observing events
   * @returns {boolean}
   */
  isObserving() {
    return this._eventObserver && this._eventObserver.isObserving;
  }

  /**
   * Ensure event observer is initialized
   * @private
   */
  _ensureEventObserver() {
    if (!this._eventObserver) {
      // Lazy initialization - requires LogMonitor and parser
      const { LogMonitor } = require('./monitoring/index.js');
      const { MinecraftLogParser } = require('./parsers/index.js');
      const { DockerLogCollector } = require('./collectors/index.js');
      const { UsernameCorrelationStrategy } = require('./monitoring/correlations/index.js');

      // Create log collector for this server
      const collector = new DockerLogCollector({
        container: this._host, // Use server host as container name
        follow: true
      });

      // Create parser
      const parser = new MinecraftLogParser();

      // Create log monitor
      const logMonitor = new LogMonitor({
        collector,
        parser,
        correlation: new UsernameCorrelationStrategy(),
        bufferSize: 1000
      });

      // Create event observer
      this._eventObserver = new EventObserver({
        logMonitor,
        parser
      });
    }
  }

  /**
   * Get entities from all bots
   * @param {string} selector - Entity selector (optional, unused in Mineflayer)
   * @returns {Promise<Array>} Array of entities
   */
  async getEntities(selector) {
    const allEntities = [];
    const bots = this._botPool.getAll();

    for (const bot of bots) {
      if (bot.entities) {
        Object.values(bot.entities).forEach(entity => {
          // Use the built-in getCustomName() method from prismarine-entity
          // This properly extracts custom name from entity.metadata[2]
          let customName = null;
          try {
            if (typeof entity.getCustomName === 'function') {
              const customNameObj = entity.getCustomName();
              if (customNameObj) {
                // ChatMessage object has a toString() method or text property
                customName = customNameObj.toString ? customNameObj.toString() : (customNameObj.text || customNameObj);
              }
            }
          } catch (e) {
            // Ignore errors from getCustomName
          }

          allEntities.push({
            id: entity.id,
            name: entity.name || entity.username,
            displayName: entity.displayName,
            customName: customName,
            type: entity.type,
            position: entity.position,
            health: entity.health
          });
        });
      }
    }

    return allEntities;
  }

  /**
   * Get player inventory
   * @param {string} username - Player username
   * @returns {Promise<Object>} Player inventory
   */
  async getPlayerInventory(username) {
    const bot = this._botPool.get(username);

    if (!bot) {
      throw new Error(`Bot "${username}" not found`);
    }

    return {
      slots: bot.inventory.slots,
      items: bot.inventory.items()
    };
  }

  /**
   * Get block at coordinates
   * @param {number} x - X coordinate
   * @param {number} y - Y coordinate
   * @param {number} z - Z coordinate
   * @returns {Promise<Object>} Block data
   */
  async getBlockAt(x, y, z) {
    const readyBots = this._botPool.getReadyBots();

    if (readyBots.length === 0) {
      throw new Error('No bot available');
    }

    const bot = readyBots[0].bot;
    const block = bot.blockAt({ x, y, z });

    return {
      type: block?.type,
      name: block?.name,
      position: block?.position,
      metadata: block?.metadata
    };
  }
}

module.exports = { MineflayerBackend };
