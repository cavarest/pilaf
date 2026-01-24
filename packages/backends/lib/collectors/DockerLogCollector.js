/**
 * DockerLogCollector - Collect logs from Docker containers
 *
 * Streams log output from Docker containers using the Dockerode API.
 * Handles reconnection with exponential backoff and supports both
 * following and historical log retrieval.
 *
 * Responsibilities (MECE):
 * - ONLY: Connect to Docker daemon and stream container logs
 * - NOT: Parse log content (LogParser's responsibility)
 * - NOT: Handle container lifecycle (user's responsibility)
 *
 * @class
 * @extends LogCollector
 *
 * Usage:
 *   const collector = new DockerLogCollector();
 *   await collector.connect({
 *     containerName: 'minecraft-server',
 *     follow: true,
 *     tail: 100
 *   });
 *   collector.on('data', (line) => console.log(line));
 */

const { LogCollector } = require('../core/LogCollector.js');
const { DockerConnectionError } = require('../errors/index.js');

class DockerLogCollector extends LogCollector {
  /**
   * Create a DockerLogCollector
   * @param {Object} options - Collector options
   * @param {Object} [options.dockerodeOptions] - Options to pass to Dockerode
   * @param {string} [options.dockerodeOptions.socketPath] - Docker socket path (default: /var/run/docker.sock)
   * @param {number} [options.reconnectDelay=1000] - Initial reconnection delay in ms
   * @param {number} [options.maxReconnectDelay=30000] - Maximum reconnection delay in ms
   * @param {number} [options.reconnectAttempts=5] - Maximum reconnection attempts
   */
  constructor(options = {}) {
    super();

    /**
     * Dockerode options
     * @private
     * @type {Object}
     */
    this._dockerodeOptions = options?.dockerodeOptions || {};

    /**
     * Reconnection configuration
     * @private
     * @type {Object}
     */
    this._reconnectConfig = {
      delay: options?.reconnectDelay || 1000,
      maxDelay: options?.maxReconnectDelay || 30000,
      attempts: options?.reconnectAttempts || 5
    };

    /**
     * Dockerode instance (lazy loaded)
     * @private
     * @type {Object|null}
     */
    this._docker = null;

    /**
     * Docker container instance
     * @private
     * @type {Object|null}
     */
    this._container = null;

    /**
     * Log stream instance
     * @private
     * @type {Object|null}
     */
    this._logStream = null;

    /**
     * Reconnection timeout ID
     * @private
     * @type {NodeJS.Timeout|null}
     */
    this._reconnectTimeout = null;

    /**
     * Current reconnection attempt count
     * @private
     * @type {number}
     */
    this._reconnectCount = 0;

    /**
     * Stream options for Docker logs
     * @private
     * @type {Object}
     */
    this._streamOptions = {
      follow: true,
      stdout: true,
      stderr: true,
      tail: 0
    };
  }

  /**
   * Connect to Docker container and stream logs
   *
   * @param {Object} config - Connection configuration
   * @param {string} config.containerName - Name or ID of the container
   * @param {boolean} [config.follow=true] - Follow log stream
   * @param {number} [config.tail=0] - Number of lines from history (0 = all)
   * @param {boolean} [config.stdout=true] - Include stdout
   * @param {boolean} [config.stderr=true] - Include stderr
   * @param {boolean} [config.disableAutoReconnect=false] - Disable automatic reconnection
   * @returns {Promise<void>}
   * @throws {DockerConnectionError} If Docker connection fails
   */
  async connect(config) {
    if (this._connected) {
      await this.disconnect();
    }

    this._config = config;
    this._streamOptions = {
      follow: config?.follow !== false,
      stdout: config?.stdout !== false,
      stderr: config?.stderr !== false,
      tail: config?.tail || 0
    };

    try {
      // Lazy load Dockerode
      if (!this._docker) {
        const Docker = require('dockerode');
        this._docker = new Docker(this._dockerodeOptions);
      }

      // Get container
      const containerName = config?.containerName;
      if (!containerName) {
        throw new DockerConnectionError(
          'Container name is required',
          { config: this._streamOptions }
        );
      }

      this._container = this._docker.getContainer(containerName);

      // Verify container exists
      try {
        await this._container.inspect();
      } catch (inspectError) {
        throw new DockerConnectionError(
          `Container not found: ${containerName}`,
          { containerName },
          inspectError
        );
      }

      // Create log stream
      this._logStream = await this._container.logs(this._streamOptions);

      // Set up stream handlers
      this._setupStreamHandlers(this._logStream);

      this._connected = true;
      // Only reset reconnect count on initial connection (not on reconnection)
      if (!this._reconnectCount) {
        this._reconnectCount = 0;
      }

      this.emit('connected');
    } catch (error) {
      // Re-throw to let caller handle the error
      throw error;
    }
  }

  /**
   * Set up event handlers for the log stream
   *
   * @private
   * @param {Object} stream - Docker log stream
   * @returns {void}
   */
  _setupStreamHandlers(stream) {
    // Docker logs are Buffers, need to decode and strip ANSI codes
    stream.on('data', (chunk) => {
      if (!this._connected) return;

      // Decode buffer to string
      const line = chunk.toString('utf8').trim();

      // Skip empty lines
      if (!line) return;

      // Strip ANSI color codes if present
      const cleanLine = this._stripAnsiCodes(line);

      this._emitData(cleanLine);
    });

    stream.on('error', (error) => {
      this._emitError(new DockerConnectionError(
        'Log stream error',
        { containerName: this._config?.containerName },
        error
      ));
    });

    stream.on('end', () => {
      this._handleStreamEnd();
    });

    stream.on('close', () => {
      this._handleStreamEnd();
    });
  }

  /**
   * Handle log stream termination
   *
   * @private
   * @returns {void}
   */
  _handleStreamEnd() {
    const shouldReconnect = this._config?.disableAutoReconnect !== true &&
                           this._reconnectCount < this._reconnectConfig.attempts;

    if (shouldReconnect && this._connected) {
      this._scheduleReconnect();
    } else {
      this._emitEnd();
    }
  }

  /**
   * Schedule reconnection with exponential backoff
   *
   * @private
   * @returns {void}
   */
  _scheduleReconnect() {
    if (this._reconnectTimeout) {
      clearTimeout(this._reconnectTimeout);
    }

    // Exponential backoff: delay * 2^attempt
    const delay = Math.min(
      this._reconnectConfig.delay * Math.pow(2, this._reconnectCount),
      this._reconnectConfig.maxDelay
    );

    this._reconnectCount++;

    this.emit('reconnecting', {
      attempt: this._reconnectCount,
      maxAttempts: this._reconnectConfig.attempts,
      delay
    });

    this._reconnectTimeout = setTimeout(async () => {
      try {
        await this.connect(this._config);
      } catch (error) {
        this._emitError(error);
      }
    }, delay);
  }

  /**
   * Strip ANSI escape codes from log line
   *
   * @private
   * @param {string} line - Line that may contain ANSI codes
   * @returns {string} - Clean line without ANSI codes
   */
  _stripAnsiCodes(line) {
    // Remove ANSI escape sequences
    // Format: ESC[...m where ESC is \x1b or \u001b
    const ansiRegex = /[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]/g;
    return line.replace(ansiRegex, '');
  }

  /**
   * Disconnect from Docker container
   *
   * @returns {Promise<void>}
   */
  async disconnect() {
    // Cancel pending reconnection
    if (this._reconnectTimeout) {
      clearTimeout(this._reconnectTimeout);
      this._reconnectTimeout = null;
    }

    // Destroy log stream
    if (this._logStream) {
      this._logStream.removeAllListeners();
      if (typeof this._logStream.destroy === 'function') {
        this._logStream.destroy();
      }
      this._logStream = null;
    }

    // Clear references
    this._container = null;
    this._docker = null;
    this._connected = false;

    this.emit('disconnected');
  }

  /**
   * Get current reconnection status
   *
   * @returns {Object} - Reconnection status
   * @property {number} attempt - Current attempt number
   * @property {number} maxAttempts - Maximum attempts
   * @property {boolean} reconnecting - Whether reconnection is pending
   */
  getReconnectStatus() {
    return {
      attempt: this._reconnectCount,
      maxAttempts: this._reconnectConfig.attempts,
      reconnecting: this._reconnectTimeout !== null
    };
  }
}

module.exports = { DockerLogCollector };
