/**
 * RconBackend - RCON implementation for Pilaf backend
 */

const { Rcon } = require('rcon-client');
const { PilafBackend } = require('./backend.js');

class RconBackend extends PilafBackend {
  constructor() {
    super();
    this.client = null;
    this.connected = false;
  }

  /**
   * Connect to RCON server
   * @param {Object} config - Configuration object
   * @param {string} config.host - RCON host
   * @param {number} config.port - RCON port
   * @param {string} config.password - RCON password
   * @param {number} [config.timeout=30000] - Connection timeout in milliseconds
   * @returns {Promise<void>}
   */
  async connect(config) {
    const host = config?.host || 'localhost';
    const port = config?.port || 25575;
    const password = config?.password || '';
    const timeout = config?.timeout || 30000; // 30 second default timeout

    try {
      // Add timeout to prevent hanging on slow/unresponsive RCON servers
      this.client = await Promise.race([
        Rcon.connect({ host, port, password }),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error(`RCON connection timeout after ${timeout}ms`)), timeout)
        )
      ]);
      this.connected = true;
      return this;
    } catch (error) {
      throw new Error(`Failed to connect to RCON: ${error.message}`);
    }
  }

  /**
   * Disconnect from RCON server
   * @returns {Promise<void>}
   */
  async disconnect() {
    if (this.client) {
      await this.client.end();
      this.client = null;
      this.connected = false;
    }
  }

  /**
   * Send command via RCON
   * @param {string} command - Command to send
   * @param {number} [timeout=10000] - Timeout in milliseconds
   * @returns {Promise<{raw: string, parsed?: any}>}
   */
  async sendCommand(command, timeout = 10000) {
    if (!this.connected || !this.client) {
      throw new Error('Not connected to RCON server');
    }

    try {
      // Add timeout to prevent hanging on slow/unresponsive commands
      const response = await Promise.race([
        this.client.send(command),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error(`RCON command timeout after ${timeout}ms`)), timeout)
        )
      ]);
      return this._parseResponse(response);
    } catch (error) {
      throw new Error(`Failed to send command: ${error.message}`);
    }
  }

  /**
   * Send command via RCON (alias for sendCommand)
   * @param {string} command - Command to send
   * @returns {Promise<{raw: string, parsed?: any}>}
   */
  async send(command) {
    return this.sendCommand(command);
  }

  /**
   * Parse RCON response
   * @private
   * @param {string} response - Raw response
   * @returns {{raw: string, parsed?: any}}
   */
  _parseResponse(response) {
    const result = { raw: response };

    // Try to parse as JSON
    try {
      result.parsed = JSON.parse(response);
    } catch {
      // Not JSON, keep raw only
    }

    return result;
  }

  /**
   * RCON doesn't support bot creation
   */
  async createBot() {
    throw new Error('RCON backend does not support bot creation');
  }

  /**
   * RCON doesn't support entity listing
   */
  async getEntities() {
    throw new Error('RCON backend does not support entity listing');
  }

  /**
   * Get player inventory via RCON command
   * @param {string} username - Player username
   * @returns {Promise<Object>}
   */
  async getPlayerInventory(username) {
    const response = await this.sendCommand(`data get entity ${username} Inventory`);
    return response.parsed || response;
  }

  /**
   * Get block at coordinates via RCON command
   * @param {number} x - X coordinate
   * @param {number} y - Y coordinate
   * @param {number} z - Z coordinate
   * @returns {Promise<Object>}
   */
  async getBlockAt(x, y, z) {
    const response = await this.sendCommand(`data get block ${x} ${y} ${z}`);
    return response.parsed || response;
  }
}

module.exports = { RconBackend };
