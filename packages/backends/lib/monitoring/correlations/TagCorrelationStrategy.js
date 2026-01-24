/**
 * TagCorrelationStrategy - Correlates events by unique tag/ID
 *
 * This correlation strategy groups events by a unique tag or identifier
 * extracted from each event. Useful for tracking request/response cycles,
 * command executions, or any scenario where events share a common identifier.
 *
 * Example use case:
 * - RCON command returns request ID: "req-12345"
 * - Subsequent log lines reference: "req-12345 completed"
 * - Strategy groups all events with tag "req-12345"
 *
 * Features:
 * - Configurable tag extraction function
 * - Automatic correlation expiration (timeout)
 * - Efficient Map-based storage
 * - Thread-safe correlation tracking
 *
 * Usage Example:
 *   const strategy = new TagCorrelationStrategy({
 *     tagExtractor: (event) => event.data?.requestId,
 *     timeout: 5000  // 5 seconds
 *   });
 *
 *   const event = { type: 'command.start', data: { requestId: 'req-123' } };
 *   const correlation = strategy.correlate(event);
 *   // correlation: { tag: 'req-123', events: [event], timestamp: Date.now() }
 */

const { EventEmitter } = require('events');

/**
 * TagCorrelationStrategy class for tag-based event correlation
 * @extends EventEmitter
 */
class TagCorrelationStrategy extends EventEmitter {
  /**
   * Create a TagCorrelationStrategy
   *
   * @param {Object} options - Strategy options
   * @param {Function} [options.tagExtractor] - Function to extract tag from event
   *                                            Default: (event) => event.data?.tag
   * @param {number} [options.timeout=5000] - Auto-expire correlations after N ms
   * @param {number} [options.cleanupInterval=60000] - Cleanup interval in ms
   */
  constructor(options = {}) {
    super();

    /**
     * Function to extract tag from event
     * @private
     * @type {Function}
     */
    this._tagExtractor = options.tagExtractor || ((event) => event.data?.tag);

    /**
     * Correlation timeout in milliseconds
     * @private
     * @type {number}
     */
    this._timeout = options.timeout || 5000;

    /**
     * Cleanup interval for expired correlations
     * @private
     * @type {number}
     */
    this._cleanupInterval = options.cleanupInterval || 60000;

    /**
     * Map of active correlations
     * @private
     * @type {Map<string, Object>}
     */
    this._correlations = new Map();

    /**
     * Interval ID for cleanup timer
     * @private
     * @type {NodeJS.Timeout|null}
     */
    this._cleanupTimer = null;

    // Start cleanup interval if timeout is set
    if (this._timeout > 0) {
      this._startCleanup();
    }
  }

  /**
   * Correlate an event by its tag
   *
   * @param {Object} event - Parsed event object
   * @returns {Object|null} - Correlation object with tag, events, timestamp, or null if no tag
   */
  correlate(event) {
    if (!event) {
      return null;
    }

    const tag = this._tagExtractor(event);
    if (!tag) {
      return null;
    }

    const now = Date.now();

    if (!this._correlations.has(tag)) {
      this._correlations.set(tag, {
        tag,
        events: [],
        timestamp: now
      });
    }

    const correlation = this._correlations.get(tag);
    correlation.events.push(event);
    correlation.timestamp = now;

    return correlation;
  }

  /**
   * Get all active correlations
   *
   * @returns {Array<Object>} - Array of correlation objects
   */
  getActiveCorrelations() {
    return Array.from(this._correlations.values());
  }

  /**
   * Get correlation by tag
   *
   * @param {string} tag - Tag to look up
   * @returns {Object|undefined} - Correlation object or undefined
   */
  getCorrelation(tag) {
    return this._correlations.get(tag);
  }

  /**
   * Reset all correlation state
   *
   * @returns {void}
   */
  reset() {
    this._correlations.clear();
    this.emit('reset');
  }

  /**
   * Remove expired correlations
   *
   * @returns {number} - Number of correlations removed
   */
  cleanup() {
    if (this._timeout <= 0) {
      return 0;
    }

    const now = Date.now();
    const expiredThreshold = now - this._timeout;
    let removed = 0;

    for (const [tag, correlation] of this._correlations.entries()) {
      if (correlation.timestamp < expiredThreshold) {
        this._correlations.delete(tag);
        removed++;
      }
    }

    if (removed > 0) {
      this.emit('cleanup', removed);
    }

    return removed;
  }

  /**
   * Stop the cleanup timer
   *
   * @returns {void}
   */
  destroy() {
    if (this._cleanupTimer) {
      clearInterval(this._cleanupTimer);
      this._cleanupTimer = null;
    }
  }

  /**
   * Start the cleanup interval
   *
   * @private
   * @returns {void}
   */
  _startCleanup() {
    this._cleanupTimer = setInterval(() => {
      this.cleanup();
    }, this._cleanupInterval);
  }

  /**
   * Get the number of active correlations
   *
   * @type {number}
   */
  get size() {
    return this._correlations.size;
  }
}

module.exports = { TagCorrelationStrategy };
