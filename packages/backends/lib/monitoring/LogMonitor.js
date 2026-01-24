/**
 * LogMonitor - Continuous log monitoring and event processing
 *
 * Aggregates LogCollector, LogParser, and CorrelationStrategy to provide
 * continuous log monitoring with real-time event processing and correlation.
 *
 * The monitor polls the log collector at regular intervals, parses each line,
 * maintains a circular buffer of recent events, and applies correlation strategies.
 *
 * Responsibilities (MECE):
 * - ONLY: Orchestrate log collection, parsing, and correlation
 * - NOT: Collect logs (LogCollector's responsibility)
 * - NOT: Parse logs (LogParser's responsibility)
 * - NOT: Correlate events (CorrelationStrategy's responsibility)
 *
 * Usage Example:
 *   const monitor = new LogMonitor({
 *     collector: new DockerLogCollector({ container: 'mc-server' }),
 *     parser: new MinecraftLogParser(),
 *     correlation: new UsernameCorrelationStrategy(),
 *     bufferSize: 1000,
 *     pollInterval: 100
 *   });
 *
 *   monitor.on('event', (event) => console.log('Event:', event.type));
 *   monitor.on('correlation', (corr) => console.log('Correlation:', corr));
 *   await monitor.start();
 */

const { EventEmitter } = require('events');
const { CircularBuffer } = require('./CircularBuffer.js');

/**
 * LogMonitor class for continuous log monitoring
 * @extends EventEmitter
 */
class LogMonitor extends EventEmitter {
  /**
   * Create a LogMonitor
   *
   * @param {Object} options - Monitor options
   * @param {Object} options.collector - LogCollector instance
   * @param {Object} options.parser - LogParser instance
   * @param {Object} options.correlation - CorrelationStrategy instance
   * @param {number} [options.bufferSize=1000] - Size of circular event buffer
   * @param {number} [options.pollInterval=100] - Poll interval in milliseconds
   * @throws {Error} If required dependencies are not provided
   */
  constructor(options = {}) {
    super();

    if (!options.collector) {
      throw new Error('LogCollector is required');
    }
    if (!options.parser) {
      throw new Error('LogParser is required');
    }
    if (!options.correlation) {
      throw new Error('CorrelationStrategy is required');
    }

    /**
     * Log collector for fetching log lines
     * @private
     * @type {Object}
     */
    this._collector = options.collector;

    /**
     * Log parser for converting lines to events
     * @private
     * @type {Object}
     */
    this._parser = options.parser;

    /**
     * Correlation strategy for linking related events
     * @private
     * @type {Object}
     */
    this._correlation = options.correlation;

    /**
     * Circular buffer for storing recent events
     * @private
     * @type {CircularBuffer}
     */
    this._buffer = new CircularBuffer(options.bufferSize || 1000);

    /**
     * Poll interval in milliseconds
     * @private
     * @type {number}
     */
    this._pollInterval = options.pollInterval || 100;

    /**
     * Whether the monitor is currently running
     * @private
     * @type {boolean}
     */
    this._isRunning = false;

    /**
     * Whether the monitor is currently paused
     * @private
     * @type {boolean}
     */
    this._isPaused = false;

    /**
     * Interval ID for the polling timer
     * @private
     * @type {NodeJS.Timeout|null}
     */
    this._intervalId = null;
  }

  /**
   * Start monitoring logs
   *
   * @returns {Promise<void>}
   * @throws {Error} If monitor is already running
   */
  async start() {
    if (this._isRunning) {
      throw new Error('LogMonitor is already running');
    }

    this._isRunning = true;
    this.emit('start');

    this._intervalId = setInterval(async () => {
      // Skip polling if paused
      if (this._isPaused) {
        return;
      }

      try {
        await this._processLogs();
      } catch (error) {
        this.emit('error', error);
      }
    }, this._pollInterval);
  }

  /**
   * Stop monitoring logs
   *
   * @returns {void}
   */
  stop() {
    if (!this._isRunning) {
      return;
    }

    this._isRunning = false;
    this._isPaused = false;

    if (this._intervalId) {
      clearInterval(this._intervalId);
      this._intervalId = null;
    }

    this.emit('stop');
  }

  /**
   * Pause monitoring (without stopping)
   *
   * @returns {void}
   */
  pause() {
    if (!this._isRunning) {
      return;
    }

    this._isPaused = true;
    this.emit('pause');
  }

  /**
   * Resume monitoring after pause
   *
   * @returns {void}
   */
  resume() {
    if (!this._isRunning || !this._isPaused) {
      return;
    }

    this._isPaused = false;
    this.emit('resume');
  }

  /**
   * Get all events from the buffer
   *
   * @returns {Array} - Array of all events (oldest to newest)
   */
  getEvents() {
    return this._buffer.toArray();
  }

  /**
   * Get recent events from the buffer
   *
   * @param {number} count - Number of recent events to return
   * @returns {Array} - Array of recent events
   */
  getRecentEvents(count = 10) {
    const start = Math.max(0, this._buffer.size - count);
    return this._buffer.slice(start);
  }

  /**
   * Get all active correlations
   *
   * @returns {Array} - Array of active correlations
   */
  getCorrelations() {
    return this._correlation.getActiveCorrelations();
  }

  /**
   * Clear all events from the buffer
   *
   * @returns {void}
   */
  clear() {
    this._buffer.clear();
    this.emit('clear');
  }

  /**
   * Check if the monitor is running
   *
   * @type {boolean}
   */
  get isRunning() {
    return this._isRunning;
  }

  /**
   * Check if the monitor is paused
   *
   * @type {boolean}
   */
  get isPaused() {
    return this._isPaused;
  }

  /**
   * Get the current buffer size
   *
   * @type {number}
   */
  get bufferSize() {
    return this._buffer.size;
  }

  /**
   * Get the buffer capacity
   *
   * @type {number}
   */
  get bufferCapacity() {
    return this._buffer.capacity;
  }

  /**
   * Process logs from collector
   *
   * @private
   * @returns {Promise<void>}
   */
  async _processLogs() {
    const logs = await this._collector.collect();

    for (const log of logs) {
      const event = this._parser.parse(log);

      // Skip null events or unknown patterns (when type is null)
      if (!event || event.type === null) {
        continue;
      }

      // Add event to buffer
      this._buffer.push(event);

      // Emit parsed event
      this.emit('event', event);

      // Apply correlation strategy
      const correlation = this._correlation.correlate(event);
      if (correlation) {
        this.emit('correlation', correlation);
      }
    }
  }
}

module.exports = { LogMonitor };
