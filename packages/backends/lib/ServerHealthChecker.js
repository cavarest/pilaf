/**
 * ServerHealthChecker - Verifies Minecraft server is ready for bot connections
 * Uses RCON for fast health checks instead of creating test bots
 */

const { Rcon } = require('rcon-client');

class ServerHealthChecker {
  /**
   * Create a ServerHealthChecker instance
   * @param {Object} config - Server configuration
   * @param {string} [config.host] - Server host
   * @param {number} [config.port] - Server port (for Minecraft, not RCON)
   * @param {string} [config.auth] - Auth mode
   * @param {string} [config.rconHost] - RCON host (defaults to host)
   * @param {number} [config.rconPort] - RCON port (default: 25575)
   * @param {string} [config.rconPassword] - RCON password (default: 'cavarest')
   */
  constructor(config = {}) {
    this.host = config?.host || 'localhost';
    this.port = config?.port || 25565;
    this.auth = config?.auth || 'offline';
    this.rconHost = config?.rconHost || this.host;
    this.rconPort = config?.rconPort || 25575;
    this.rconPassword = config?.rconPassword || 'cavarest';
    this.ready = false;
    this.lastCheckTime = null;
    this.lastCheckResult = null;
  }

  /**
   * Perform a single health check using RCON
   * @returns {Promise<{success: boolean, reason: string, latency: number}>}
   */
  async check() {
    const startTime = Date.now();

    try {
      const client = await Rcon.connect({
        host: this.rconHost,
        port: this.rconPort,
        password: this.rconPassword
      });

      // Try to send a simple command to verify server is responsive
      await client.send('list');
      await client.end();

      const latency = Date.now() - startTime;

      this.lastCheckTime = new Date();
      this.lastCheckResult = { success: true };
      this.ready = true;

      return {
        success: true,
        reason: 'Server is ready',
        latency
      };
    } catch (error) {
      const latency = Date.now() - startTime;

      this.lastCheckTime = new Date();
      this.lastCheckResult = { success: false, error: error.message };
      this.ready = false;

      return {
        success: false,
        reason: error.message,
        latency
      };
    }
  }

  /**
   * Wait for server to become ready, with polling
   * @param {Object} options - Options
   * @param {number} [options.timeout] - Total timeout (default: 120000ms)
   * @param {number} [options.interval] - Polling interval (default: 3000ms)
   * @param {boolean} [options.fastFail] - Fail immediately on first error (default: false)
   * @returns {Promise<{success: boolean, checks: number, totalLatency: number}>}
   */
  async waitForReady(options = {}) {
    const {
      timeout = 120000,
      interval = 3000,
      fastFail = false
    } = options;

    const startTime = Date.now();
    let checks = 0;
    let totalLatency = 0;

    while (Date.now() - startTime < timeout) {
      checks++;

      const result = await this.check();
      totalLatency += result.latency;

      if (result.success) {
        this.ready = true;
        return {
          success: true,
          checks,
          totalLatency
        };
      }

      // If fast fail is enabled, don't retry
      if (fastFail) {
        break;
      }

      // Wait before retrying
      await new Promise(resolve => setTimeout(resolve, interval));
    }

    this.ready = false;
    return {
      success: false,
      checks,
      totalLatency
    };
  }

  /**
   * Check if server was marked ready on last check
   * @returns {boolean}
   */
  isReady() {
    return this.ready;
  }

  /**
   * Get last check result
   * @returns {{success: boolean|null, error: string|null, time: Date|null}}
   */
  getLastCheck() {
    return {
      success: this.lastCheckResult?.success ?? null,
      error: this.lastCheckResult?.error ?? null,
      time: this.lastCheckTime
    };
  }

  /**
   * Reset the ready state
   */
  reset() {
    this.ready = false;
    this.lastCheckTime = null;
    this.lastCheckResult = null;
  }

  /**
   * Create a health check report
   * @returns {Object} Health check report
   */
  getReport() {
    return {
      ready: this.ready,
      lastCheck: this.getLastCheck(),
      config: {
        host: this.host,
        port: this.port,
        auth: this.auth,
        rconHost: this.rconHost,
        rconPort: this.rconPort
      }
    };
  }
}

module.exports = { ServerHealthChecker };
