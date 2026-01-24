/**
 * LogCollector - Abstract base class for log collection
 *
 * Defines the interface for collecting raw log data from various sources
 * (Docker containers, log files, syslog, etc.). Concrete implementations
 * must extend this class and implement the abstract methods.
 *
 * @abstract
 * @class
 * @extends EventEmitter
 *
 * Responsibilities (MECE):
 * - ONLY: Connect to log source and receive raw log lines
 * - NOT: Parse log content (LogParser's responsibility)
 * - NOT: Correlate responses (CorrelationStrategy's responsibility)
 *
 * Usage Example:
 *   // Extend LogCollector and implement abstract methods
 *   function DockerLogCollector() {
 *     LogCollector.call(this);
 *   }
 *   DockerLogCollector.prototype = Object.create(LogCollector.prototype);
 *   DockerLogCollector.prototype.connect = async function(config) { ... };
 *   DockerLogCollector.prototype.disconnect = async function() { ... };
 */

const EventEmitter = require('events');

class LogCollector extends EventEmitter {
  /**
   * Create a LogCollector
   * @throws {Error} Direct instantiation of abstract class
   */
  constructor() {
    super();

    // Prevent direct instantiation of abstract class
    if (this.constructor === LogCollector) {
      throw new Error('LogCollector is abstract and cannot be instantiated directly');
    }

    /**
     * Connection state
     * @protected
     * @type {boolean}
     */
    this._connected = false;

    /**
     * Pause state
     * @protected
     * @type {boolean}
     */
    this._paused = false;

    /**
     * Connection configuration
     * @protected
     * @type {Object|null}
     */
    this._config = null;
  }

  /**
   * Get connection state
   * @returns {boolean}
   */
  get connected() {
    return this._connected;
  }

  /**
   * Get pause state
   * @returns {boolean}
   */
  get paused() {
    return this._paused;
  }

  /**
   * Get connection configuration
   * @returns {Object|null}
   */
  get config() {
    return this._config;
  }

  /**
   * Connect to the log source
   *
   * Abstract method that must be implemented by concrete classes.
   * Should establish connection to the log source and start emitting
   * 'data' events with raw log lines.
   *
   * @abstract
   * @param {Object} config - Connection configuration
   * @param {string} [config.host] - Host address (for remote sources)
   * @param {number} [config.port] - Port number (for remote sources)
   * @param {string} [config.path] - File path (for file sources)
   * @param {string} [config.containerName] - Container name (for Docker)
   * @returns {Promise<void>}
   * @throws {Error} If connection fails
   */
  async connect(config) {
    throw new Error('Method "connect()" must be implemented by subclass');
  }

  /**
   * Disconnect from the log source
   *
   * Abstract method that must be implemented by concrete classes.
   * Should clean up resources and stop emitting events.
   *
   * @abstract
   * @returns {Promise<void>}
   */
  async disconnect() {
    throw new Error('Method "disconnect()" must be implemented by subclass');
  }

  /**
   * Pause log collection
   *
   * Stops emitting 'data' events without disconnecting.
   * Collection can be resumed with resume().
   *
   * @returns {void}
   */
  pause() {
    this._paused = true;
    this.emit('paused');
  }

  /**
   * Resume log collection
   *
   * Resumes emitting 'data' events after pause().
   *
   * @returns {void}
   */
  resume() {
    this._paused = false;
    this.emit('resumed');
  }

  /**
   * Emit a log line (protected method for subclasses)
   *
   * Emits 'data' event with raw log line if connected and not paused.
   * Subclasses should call this method when they receive log data.
   *
   * @protected
   * @param {string} line - Raw log line
   * @returns {boolean} - True if event was emitted, false if paused/disconnected
   */
  _emitData(line) {
    if (!this._connected || this._paused) {
      return false;
    }

    this.emit('data', line);
    return true;
  }

  /**
   * Emit an error (protected method for subclasses)
   *
   * Emits 'error' event. Subclasses should call this method when
   * errors occur during collection.
   *
   * @protected
   * @param {Error} error - The error to emit
   * @returns {void}
   */
  _emitError(error) {
    this.emit('error', error);
  }

  /**
   * Emit end event (protected method for subclasses)
   *
   * Emits 'end' event when the log stream terminates.
   * Subclasses should call this when the stream ends naturally.
   *
   * @protected
   * @returns {void}
   */
  _emitEnd() {
    this._connected = false;
    this.emit('end');
  }
}

module.exports = { LogCollector };
