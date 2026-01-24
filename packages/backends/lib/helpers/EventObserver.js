/**
 * EventObserver - Observe and subscribe to Minecraft server events
 *
 * Wraps LogMonitor and MinecraftLogParser to provide a clean API for
 * subscribing to Minecraft server events without manually parsing logs.
 *
 * Responsibilities (MECE):
 * - ONLY: Provide clean API for event subscriptions
 * - NOT: Collect logs (LogCollector's responsibility)
 * - NOT: Parse logs (LogParser's responsibility)
 * - NOT: Monitor logs (LogMonitor's responsibility)
 *
 * Usage Example:
 *   const observer = new EventObserver(logMonitor, parser);
 *   observer.onPlayerJoin((event) => {
 *     console.log('Player joined:', event.data.player);
 *   });
 *   await observer.start();
 */

const { EventEmitter } = require('events');

/**
 * EventObserver class for event subscriptions
 * @extends EventEmitter
 */
class EventObserver extends EventEmitter {
  /**
   * Create an EventObserver
   *
   * @param {Object} options - Observer options
   * @param {Object} options.logMonitor - LogMonitor instance
   * @param {Object} options.parser - LogParser instance
   * @throws {Error} If required dependencies are not provided
   */
  constructor(options = {}) {
    super();

    if (!options.logMonitor) {
      throw new Error('logMonitor is required');
    }
    if (!options.parser) {
      throw new Error('parser is required');
    }

    /**
     * LogMonitor instance for log collection
     * @private
     * @type {Object}
     */
    this._logMonitor = options.logMonitor;

    /**
     * LogParser instance for parsing logs
     * @private
     * @type {Object}
     */
    this._parser = options.parser;

    /**
     * Event subscriptions: pattern -> Set<callback>
     * @private
     * @type {Map<string, Set<Function>>}
     */
    this._subscriptions = new Map();

    /**
     * Whether observer is currently running
     * @private
     * @type {boolean}
     */
    this._isObserving = false;

    // Set up event listener for log monitor
    this._logMonitor.on('event', (event) => {
      this._handleEvent(event);
    });
  }

  /**
   * Subscribe to events matching a pattern
   *
   * @param {string} pattern - Event pattern (supports wildcards: "entity.*")
   * @param {Function} callback - Callback function(event)
   * @returns {Function} - Unsubscribe function
   *
   * @example
   * const unsubscribe = observer.onEvent('entity.*', (event) => {
   *   console.log('Entity event:', event.type);
   * });
   */
  onEvent(pattern, callback) {
    if (!this._subscriptions.has(pattern)) {
      this._subscriptions.set(pattern, new Set());
    }
    this._subscriptions.get(pattern).add(callback);

    // Return unsubscribe function
    return () => {
      this._subscriptions.get(pattern)?.delete(callback);
      if (this._subscriptions.get(pattern)?.size === 0) {
        this._subscriptions.delete(pattern);
      }
    };
  }

  /**
   * Subscribe to player join events
   *
   * @param {Function} callback - Callback function(event)
   * @returns {Function} - Unsubscribe function
   */
  onPlayerJoin(callback) {
    return this.onEvent('entity.join', callback);
  }

  /**
   * Subscribe to player leave events
   *
   * @param {Function} callback - Callback function(event)
   * @returns {Function} - Unsubscribe function
   */
  onPlayerLeave(callback) {
    return this.onEvent('entity.leave', callback);
  }

  /**
   * Subscribe to player death events
   *
   * @param {Function} callback - Callback function(event)
   * @returns {Function} - Unsubscribe function
   */
  onPlayerDeath(callback) {
    return this.onEvent('entity.death.*', callback);
  }

  /**
   * Subscribe to command events
   *
   * @param {Function} callback - Callback function(event)
   * @returns {Function} - Unsubscribe function
   */
  onCommand(callback) {
    return this.onEvent('command.*', callback);
  }

  /**
   * Subscribe to world events (time, weather, save)
   *
   * @param {Function} callback - Callback function(event)
   * @returns {Function} - Unsubscribe function
   */
  onWorldEvent(callback) {
    return this.onEvent('world.*', callback);
  }

  /**
   * Start observing events
   *
   * @returns {Promise<void>}
   * @throws {Error} If already observing
   */
  async start() {
    if (this._isObserving) {
      throw new Error('EventObserver is already observing');
    }

    this._isObserving = true;
    await this._logMonitor.start();
    this.emit('start');
  }

  /**
   * Stop observing events
   *
   * @returns {void}
   */
  stop() {
    if (!this._isObserving) {
      return;
    }

    this._isObserving = false;
    this._logMonitor.stop();
    this.emit('stop');
  }

  /**
   * Check if observer is currently running
   *
   * @type {boolean}
   */
  get isObserving() {
    return this._isObserving;
  }

  /**
   * Get all active subscriptions
   *
   * @returns {Array<Object>} - Array of {pattern, callbackCount}
   */
  getSubscriptions() {
    return Array.from(this._subscriptions.entries()).map(([pattern, callbacks]) => ({
      pattern,
      callbackCount: callbacks.size
    }));
  }

  /**
   * Clear all event subscriptions
   *
   * @returns {void}
   */
  clearSubscriptions() {
    this._subscriptions.clear();
  }

  /**
   * Handle incoming event from log monitor
   *
   * @private
   * @param {Object} event - Parsed event
   * @returns {void}
   */
  _handleEvent(event) {
    // Find matching subscriptions and notify
    for (const [pattern, callbacks] of this._subscriptions) {
      if (this._matchesPattern(event.type, pattern)) {
        callbacks.forEach(cb => {
          try {
            cb(event);
          } catch (error) {
            // Emit error event but don't crash
            this.emit('error', { error, event });
          }
        });
      }
    }
  }

  /**
   * Check if event type matches pattern
   *
   * @private
   * @param {string} eventType - Event type (e.g., "entity.join")
   * @param {string} pattern - Pattern (e.g., "entity.*", "command.issued")
   * @returns {boolean} - True if pattern matches
   */
  _matchesPattern(eventType, pattern) {
    // Wildcard match
    if (pattern === '*') {
      return true;
    }

    // Glob-style pattern matching
    if (pattern.includes('*')) {
      const regexPattern = '^' + pattern.replace(/\*/g, '.*') + '$';
      const regex = new RegExp(regexPattern);
      return regex.test(eventType);
    }

    // Exact match
    return eventType === pattern;
  }
}

module.exports = { EventObserver };
