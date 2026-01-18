/**
 * Base PilafBackend class - abstract class defining the interface for all backend implementations
 */

class PilafBackend {
  /**
   * Connect to the backend (RCON server or Minecraft server)
   * @abstract
   * @param {Object} config - Connection configuration
   * @returns {Promise<void>}
   */
  async connect(config) {
    throw new Error('Method "connect()" must be implemented.');
  }

  /**
   * Disconnect from the backend
   * @abstract
   * @returns {Promise<void>}
   */
  async disconnect() {
    throw new Error('Method "disconnect()" must be implemented.');
  }

  /**
   * Send a command to the server
   * @abstract
   * @param {string} command - The command to send
   * @returns {Promise<{raw: string, parsed?: any}>}
   */
  async sendCommand(command) {
    throw new Error('Method "sendCommand()" must be implemented.');
  }

  /**
   * Create a bot/player instance
   * @abstract
   * @param {Object} options - Bot options (including username)
   * @returns {Promise<Object>} Bot instance
   */
  async createBot(options) {
    throw new Error('Method "createBot()" must be implemented.');
  }

  /**
   * Get all entities on the server
   * @abstract
   * @param {string} selector - Entity selector (optional)
   * @returns {Promise<Array>} Array of entities
   */
  async getEntities(selector) {
    throw new Error('Method "getEntities()" must be implemented.');
  }

  /**
   * Get player inventory
   * @abstract
   * @param {string} username - Player username
   * @returns {Promise<Object>} Player inventory
   */
  async getPlayerInventory(username) {
    throw new Error('Method "getPlayerInventory()" must be implemented.');
  }

  /**
   * Get block at specific coordinates
   * @abstract
   * @param {number} x - X coordinate
   * @param {number} y - Y coordinate
   * @param {number} z - Z coordinate
   * @returns {Promise<Object>} Block data
   */
  async getBlockAt(x, y, z) {
    throw new Error('Method "getBlockAt()" must be implemented.');
  }
}

module.exports = { PilafBackend };
