/**
 * BotPool - Manages a collection of Mineflayer bots with proper lifecycle
 * Ensures cleanup and provides query methods
 */

const { ConnectionState } = require('./ConnectionState.js');
const { BotLifecycleManager } = require('./BotLifecycleManager.js');

class BotPool {
  /**
   * Create a BotPool
   * @param {Object} options - Options
   * @param {Function} [options.createBotFn] - Function to create bots
   */
  constructor(options = {}) {
    this.bots = new Map(); // username -> { bot, state, createdAt }
    this.createBotFn = options.createBotFn;
  }

  /**
   * Add a bot to the pool
   * @param {string} username - Bot username
   * @param {Object} bot - Mineflayer bot instance
   * @returns {void}
   */
  add(username, bot) {
    if (this.bots.has(username)) {
      throw new Error(`Bot "${username}" already exists in pool`);
    }

    this.bots.set(username, {
      bot,
      state: ConnectionState.SPAWNED,
      createdAt: new Date()
    });
  }

  /**
   * Remove a bot from the pool (without disconnecting)
   * @param {string} username - Bot username
   * @returns {boolean} True if bot was removed
   */
  remove(username) {
    return this.bots.delete(username);
  }

  /**
   * Get a bot by username
   * @param {string} username - Bot username
   * @returns {Object|null} Bot instance or null
   */
  get(username) {
    const entry = this.bots.get(username);
    return entry ? entry.bot : null;
  }

  /**
   * Get a bot entry with metadata
   * @param {string} username - Bot username
   * @returns {Object|null} Bot entry or null
   */
  getEntry(username) {
    return this.bots.get(username) || null;
  }

  /**
   * Check if a bot exists in the pool
   * @param {string} username - Bot username
   * @returns {boolean}
   */
  has(username) {
    return this.bots.has(username);
  }

  /**
   * Get all bot usernames
   * @returns {Array<string>}
   */
  getUsernames() {
    return Array.from(this.bots.keys());
  }

  /**
   * Get all bots
   * @returns {Array<Object>} Array of bot instances
   */
  getAll() {
    return Array.from(this.bots.values()).map(entry => entry.bot);
  }

  /**
   * Get pool size
   * @returns {number}
   */
  size() {
    return this.bots.size;
  }

  /**
   * Check if pool is empty
   * @returns {boolean}
   */
  isEmpty() {
    return this.bots.size === 0;
  }

  /**
   * Quit and remove a bot from the pool
   * @param {string} username - Bot username
   * @param {Object} options - Options
   * @returns {Promise<{success: boolean, reason: string}>}
   */
  async quitBot(username, options = {}) {
    const entry = this.bots.get(username);
    if (!entry) {
      return { success: false, reason: `Bot "${username}" not found in pool` };
    }

    const result = await BotLifecycleManager.quitBot(entry.bot, options);
    this.bots.delete(username);
    return result;
  }

  /**
   * Quit all bots in the pool
   * @param {Object} options - Options
   * @returns {Promise<Array<{username: string, result: Object}>>}
   */
  async quitAll(options = {}) {
    const results = [];
    const usernames = this.getUsernames();

    for (const username of usernames) {
      const result = await this.quitBot(username, options);
      results.push({ username, result });
    }

    return results;
  }

  /**
   * Get health status of the pool
   * @returns {Object} Pool health info
   */
  getHealth() {
    const bots = [];
    let healthyCount = 0;

    for (const [username, entry] of this.bots.entries()) {
      const isReady = BotLifecycleManager.isBotReady(entry.bot);
      if (isReady) healthyCount++;

      bots.push({
        username,
        state: entry.state,
        ready: isReady,
        createdAt: entry.createdAt
      });
    }

    return {
      total: this.bots.size,
      healthy: healthyCount,
      unhealthy: this.bots.size - healthyCount,
      bots
    };
  }

  /**
   * Find bots by state
   * @param {string} state - ConnectionState to filter by
   * @returns {Array<Object>} Array of matching bots
   */
  findByState(state) {
    return Array.from(this.bots.entries())
      .filter(([_, entry]) => entry.state === state)
      .map(([username, entry]) => ({ username, ...entry }));
  }

  /**
   * Get bots that are ready for operations
   * @returns {Array<Object>} Array of ready bots
   */
  getReadyBots() {
    return Array.from(this.bots.entries())
      .filter(([_, entry]) => BotLifecycleManager.isBotReady(entry.bot))
      .map(([username, entry]) => ({ username, bot: entry.bot }));
  }

  /**
   * Clear the pool (remove all references without disconnecting)
   * Use quitAll() for proper cleanup
   * @returns {number} Number of bots removed
   */
  clear() {
    const size = this.bots.size;
    this.bots.clear();
    return size;
  }
}

module.exports = { BotPool };
