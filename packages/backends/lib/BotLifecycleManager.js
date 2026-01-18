/**
 * BotLifecycleManager - Manages the complete lifecycle of a Mineflayer bot
 * Handles creation, spawning, and disconnection with proper event-driven patterns
 */

const { ConnectionState } = require('./ConnectionState.js');

class BotLifecycleManager {
  /**
   * Create a bot and wait for it to spawn
   * @param {Function} createBotFn - Function that creates a Mineflayer bot
   * @param {Object} options - Bot creation options
   * @param {number} [options.spawnTimeout] - Max time to wait for spawn (default: 30000ms)
   * @returns {Promise<{bot: Object, state: string}>} Bot instance and final state
   */
  static async createAndSpawnBot(createBotFn, options = {}) {
    const { spawnTimeout = 30000 } = options;
    let bot = null;
    let currentState = ConnectionState.CONNECTING;

    try {
      // Create the bot
      bot = createBotFn(options);
      currentState = ConnectionState.SPAWNING;

      // Wait for spawn event with proper error handling
      await new Promise((resolve, reject) => {
        const timeoutId = setTimeout(() => {
          // Clean up event listeners
          bot.removeAllListeners('spawn');
          bot.removeAllListeners('error');
          bot.removeAllListeners('kicked');
          bot.removeAllListeners('end');
          reject(new Error(`Bot spawn timeout after ${spawnTimeout}ms`));
        }, spawnTimeout);

        bot.once('spawn', () => {
          clearTimeout(timeoutId);
          currentState = ConnectionState.SPAWNED;
          resolve();
        });

        bot.once('error', (err) => {
          clearTimeout(timeoutId);
          currentState = ConnectionState.ERROR;
          bot.removeAllListeners('spawn');
          reject(new Error(`Bot connection error: ${err.message}`));
        });

        bot.once('kicked', (reason) => {
          clearTimeout(timeoutId);
          currentState = ConnectionState.ERROR;
          bot.removeAllListeners('spawn');
          reject(new Error(`Bot kicked by server: ${reason}`));
        });

        bot.once('end', () => {
          if (currentState !== ConnectionState.SPAWNED) {
            clearTimeout(timeoutId);
            currentState = ConnectionState.ERROR;
            bot.removeAllListeners('spawn');
            reject(new Error(`Bot connection ended before spawn`));
          }
        });
      });

      return { bot, state: currentState };
    } catch (error) {
      currentState = ConnectionState.ERROR;
      // Clean up bot if creation failed
      if (bot) {
        try {
          bot.quit();
        } catch {
          // Ignore cleanup errors
        }
      }
      throw error;
    }
  }

  /**
   * Quit a bot and wait for disconnection to complete
   * @param {Object} bot - Mineflayer bot instance
   * @param {Object} options - Options
   * @param {number} [options.disconnectTimeout] - Max time to wait (default: 10000ms)
   * @returns {Promise<{success: boolean, reason: string}>}
   */
  static async quitBot(bot, options = {}) {
    const { disconnectTimeout = 10000 } = options;

    return new Promise((resolve) => {
      let resolved = false;

      // Set up timeout FIRST
      const timeoutId = setTimeout(() => {
        if (!resolved) {
          resolved = true;
          // Force cleanup on timeout
          bot.removeAllListeners('end');
          bot.removeAllListeners('error');
          // Also close the underlying client connection if it exists
          if (bot._client) {
            bot._client.end();
          }
          resolve({ success: false, reason: 'Disconnect timeout' });
        }
      }, disconnectTimeout);

      // Set up event listeners BEFORE calling bot.quit()
      // to avoid race condition where 'end' fires before listeners are attached

      // Handle successful disconnection
      bot.once('end', () => {
        if (!resolved) {
          resolved = true;
          clearTimeout(timeoutId);
          // Explicitly close the underlying client connection
          // This ensures no residual socket state remains
          if (bot._client) {
            bot._client.end();
          }
          resolve({ success: true, reason: 'Clean disconnect' });
        }
      });

      // Handle disconnection error
      bot.once('error', (err) => {
        if (!resolved) {
          resolved = true;
          clearTimeout(timeoutId);
          // Still close the client on error
          if (bot._client) {
            bot._client.end();
          }
          resolve({ success: false, reason: `Disconnect error: ${err.message}` });
        }
      });

      // Initiate quit AFTER event listeners are set up
      // Use bot.quit() for graceful disconnect instead of socket.destroy()
      // This gives the server time to properly clean up player state
      try {
        bot.quit();
      } catch (error) {
        if (!resolved) {
          resolved = true;
          clearTimeout(timeoutId);
          // Close client on quit failure
          if (bot._client) {
            bot._client.end();
          }
          resolve({ success: false, reason: `Quit failed: ${error.message}` });
        }
      }
    });
  }

  /**
   * Wait for a specific event on a bot
   * @param {Object} bot - Mineflayer bot instance
   * @param {string} eventName - Event to wait for
   * @param {Object} options - Options
   * @param {number} [options.timeout] - Max time to wait (default: 5000ms)
   * @returns {Promise<any>} Event data
   */
  static async waitForEvent(bot, eventName, options = {}) {
    const { timeout = 5000 } = options;

    return new Promise((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        bot.removeAllListeners(eventName);
        reject(new Error(`Timeout waiting for event "${eventName}" after ${timeout}ms`));
      }, timeout);

      bot.once(eventName, (...args) => {
        clearTimeout(timeoutId);
        resolve(args.length === 1 ? args[0] : args);
      });
    });
  }

  /**
   * Check if a bot is ready for operations
   * @param {Object} bot - Mineflayer bot instance
   * @returns {boolean} True if bot is spawned and ready
   */
  static isBotReady(bot) {
    return bot && bot.entity && typeof bot.health === 'number';
  }

  /**
   * Get bot state information
   * @param {Object} bot - Mineflayer bot instance
   * @returns {Object} Bot state info
   */
  static getBotInfo(bot) {
    if (!bot) {
      return { connected: false, spawned: false, username: null };
    }

    return {
      connected: true,
      spawned: !!bot.entity,
      username: bot.username,
      health: bot.health,
      position: bot.entity ? bot.entity.position : null
    };
  }
}

module.exports = { BotLifecycleManager };
