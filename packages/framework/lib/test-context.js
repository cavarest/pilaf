/**
 * Test Context Helper
 *
 * Provides a simplified way to create bot players for testing
 * This combines RCON and Mineflayer backends for seamless testing
 *
 * IMPORTANT: Returns TWO backends:
 * - rcon: RconBackend for server commands that return responses
 * - backend: MineflayerBackend for bot player control
 *
 * Use context.rcon.send() for commands like /data get, /teleport, etc.
 * Use context.bot.chat() for player commands
 */

const { MineflayerBackend } = require('../backends/lib/mineflayer-backend.js');
const { RconBackend } = require('../backends/lib/rcon-backend.js');

/**
 * Create a test context with both RCON and a bot player
 * This is the recommended way to set up tests that need player commands
 *
 * @param {Object} config - Configuration
 * @param {string} config.username - Bot username (default: 'TestPlayer')
 * @param {string} config.host - Server host (default: 'localhost')
 * @param {number} config.gamePort - Server game port (default: 25565)
 * @param {number} config.rconPort - RCON port (default: 25575)
 * @param {string} config.rconPassword - RCON password (default: 'minecraft')
 * @param {string} config.auth - Auth mode (default: 'offline')
 * @returns {Promise<TestContext>} Test context with rcon, backend, and bot
 */
async function createTestContext(config = {}) {
  const username = config.username || 'TestPlayer';
  const host = config.host || 'localhost';
  const gamePort = config.gamePort || 25565;
  const rconPort = config.rconPort || 25575;
  const rconPassword = config.rconPassword || 'minecraft';
  const auth = config.auth || 'offline';

  // Create RCON backend for server commands (returns responses)
  const rcon = new RconBackend();
  await rcon.connect({
    host,
    port: rconPort,
    password: rconPassword
  });

  // Create Mineflayer backend for bot players
  const backend = new MineflayerBackend();

  // Connect to server
  await backend.connect({
    host,
    port: gamePort,
    auth,
    rconHost: host,
    rconPort,
    rconPassword
  });

  // Wait for server to be ready
  await backend.waitForServerReady({ timeout: 60000 });

  // Create bot player
  const bot = await backend.createBot({
    username,
    auth
  });

  return { backend, rcon, bot, playerName: username };
}

/**
 * Disconnect and cleanup test context
 *
 * @param {TestContext} context - The test context to cleanup
 */
async function cleanupTestContext(context) {
  if (!context) return;

  if (context.bot && context.backend) {
    await context.backend.quitBot(context.bot);
  }

  if (context.backend) {
    await context.backend.disconnect();
  }

  if (context.rcon) {
    await context.rcon.disconnect();
  }
}

/**
 * @typedef {Object} TestContext
 * @property {MineflayerBackend} backend - The Mineflayer backend instance (for bot control)
 * @property {RconBackend} rcon - The RCON backend instance (for server commands with responses)
 * @property {Object} bot - The bot player instance
 * @property {string} playerName - The bot's username
 */

module.exports = {
  createTestContext,
  cleanupTestContext
};
