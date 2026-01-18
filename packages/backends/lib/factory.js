/**
 * PilafBackendFactory - Factory for creating backend instances
 */

const { PilafBackend } = require('./backend.js');
const { RconBackend } = require('./rcon-backend.js');
const { MineflayerBackend } = require('./mineflayer-backend.js');

class PilafBackendFactory {
  /**
   * Create a backend instance based on type
   * @param {string} type - Backend type ('rcon' or 'mineflayer')
   * @param {Object} config - Configuration object
   * @returns {PilafBackend} Connected backend instance
   */
  static create(type, config = {}) {
    switch (type.toLowerCase()) {
      case 'rcon':
        return new RconBackend().connect(config);

      case 'mineflayer':
        return new MineflayerBackend().connect(config);

      default:
        throw new Error(`Unknown backend type: ${type}. Supported types: 'rcon', 'mineflayer'`);
    }
  }
}

module.exports = { PilafBackendFactory };
