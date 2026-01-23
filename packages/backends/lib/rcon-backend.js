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
    this._connectTimeout = null;
  }

  /**
   * Connect to RCON server
   * @param {Object} config - Configuration object
   * @param {string} config.host - RCON host
   * @param {number} config.port - RCON port
   * @param {string} config.password - RCON password
   * @param {number} [config.timeout=30000] - Connection timeout in milliseconds
   * @param {number} [config.maxRetries=5] - Max connection retry attempts
   * @returns {Promise<void>}
   */
  async connect(config) {
    const host = config?.host || 'localhost';
    const port = config?.port || 25575;
    const password = config?.password || '';
    const timeout = config?.timeout || 30000; // 30 second default timeout
    const maxRetries = config?.maxRetries || 5; // Retry up to 5 times

    let lastError = null;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        // Add timeout to prevent hanging on slow/unresponsive RCON servers
        // Store timeout ID so we can clear it on success
        const timeoutPromise = new Promise((_, reject) => {
          this._connectTimeout = setTimeout(() => {
            this._connectTimeout = null;
            reject(new Error(`RCON connection timeout after ${timeout}ms`));
          }, timeout);
        });

        this.client = await Promise.race([
          Rcon.connect({ host, port, password }),
          timeoutPromise
        ]);

        // Clear timeout if connection succeeded
        if (this._connectTimeout) {
          clearTimeout(this._connectTimeout);
          this._connectTimeout = null;
        }

        this.connected = true;
        return this;
      } catch (error) {
        // Clean up timeout on error
        if (this._connectTimeout) {
          clearTimeout(this._connectTimeout);
          this._connectTimeout = null;
        }

        lastError = error;

        // If not the last attempt, wait before retrying with exponential backoff
        if (attempt < maxRetries) {
          const waitTime = Math.min(1000 * Math.pow(2, attempt - 1), 5000); // Max 5 seconds
          await new Promise(resolve => setTimeout(resolve, waitTime));
        }
      }
    }

    // All retries exhausted
    throw new Error(`Failed to connect to RCON after ${maxRetries} attempts: ${lastError.message}`);
  }

  /**
   * Disconnect from RCON server
   * @returns {Promise<void>}
   */
  async disconnect() {
    // Clear connection timeout if still pending
    if (this._connectTimeout) {
      clearTimeout(this._connectTimeout);
      this._connectTimeout = null;
    }

    if (this.client) {
      // Store the client reference before clearing
      const client = this.client;
      this.client = null;
      this.connected = false;

      // Destroy the socket immediately without waiting for TCP FIN
      // This prevents Jest from hanging on open handles
      if (client.socket) {
        try {
          client.socket.destroy();
        } catch (e) {
          // Socket might already be destroyed
        }
      }

      // Clear all event listeners from the internal emitter
      if (client.emitter) {
        try {
          client.emitter.removeAllListeners();
        } catch (e) {
          // Emitter might already be cleaned up
        }
      }
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

    // Create a single timeout that we can cancel
    let timeoutId = null;
    const timeoutPromise = new Promise((_, reject) => {
      timeoutId = setTimeout(() => {
        reject(new Error(`RCON command timeout after ${timeout}ms`));
      }, timeout);
    });

    try {
      // Race the actual send against the timeout
      const response = await Promise.race([
        this.client.send(command),
        timeoutPromise
      ]);

      // Clear timeout if command succeeded
      if (timeoutId !== null) {
        clearTimeout(timeoutId);
        timeoutId = null;
      }

      return this._parseResponse(response);
    } catch (error) {
      // Clear timeout on error
      if (timeoutId !== null) {
        clearTimeout(timeoutId);
        timeoutId = null;
      }
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
