/**
 * CorrelationStrategy - Abstract base class for command response correlation
 *
 * Defines the interface for matching command responses in log streams.
 * Concrete implementations must extend this class and implement the
 * abstract correlate() method.
 *
 * @abstract
 * @class
 *
 * Responsibilities (MECE):
 * - ONLY: Match log events to commands
 * - NOT: Parse logs (LogParser's responsibility)
 * - NOT: Execute commands (backend's responsibility)
 * - NOT: Store events (LogMonitor's responsibility)
 *
 * Strategy Hierarchy (in order of reliability):
 * 1. Tag-based - Most reliable (uses entity tags)
 * 2. Username-based - Moderately reliable (uses username + timestamp)
 * 3. Sequential - Fragile (assumes order preservation)
 *
 * Usage:
 *   class TagCorrelationStrategy extends CorrelationStrategy {
 *     async correlate(command, eventStream, timeout) {
 *       // Inject tag and wait for matching response
 *     }
 *   }
 */

const { ResponseTimeoutError } = require('../errors/index.js');

class CorrelationStrategy {
  /**
   * Create a CorrelationStrategy
   * @param {Object} options - Strategy options
   * @param {number} [options.timeout=5000] - Default timeout in milliseconds
   * @param {number} [options.window=2000] - Time window for correlation (ms)
   * @throws {Error} Direct instantiation of abstract class
   */
  constructor(options = {}) {
    // Prevent direct instantiation of abstract class
    if (this.constructor === CorrelationStrategy) {
      throw new Error('CorrelationStrategy is abstract and cannot be instantiated directly');
    }

    /**
     * Default timeout for correlation
     * @protected
     * @type {number}
     */
    this._defaultTimeout = options?.timeout || 5000;

    /**
     * Time window for correlation (ms)
     * Used by time-based strategies
     * @protected
     * @type {number}
     */
    this._correlationWindow = options?.window || 2000;
  }

  /**
   * Correlate a command with its response from the event stream
   *
   * Abstract method that must be implemented by concrete classes.
   * Should wait for matching event and return the response, or throw
   * CorrelationError if timeout occurs.
   *
   * @abstract
   * @param {string} command - The command that was sent
   * @param {AsyncIterable<Object>} eventStream - Stream of parsed events
   * @param {number} [timeout] - Timeout in milliseconds (uses default if not provided)
   * @returns {Promise<Object>} - The correlated response event
   * @throws {ResponseTimeoutError} - If correlation fails or times out
   * @example
   * // Returns:
   * // {
   * //   type: 'teleport',
   * //   data: { player: 'TestPlayer', position: { x: 100, y: 64, z: 100 } },
   * //   raw: '[12:34:56] Teleported TestPlayer to 100.0, 64.0, 100.0'
   * // }
   */
  async correlate(command, eventStream, timeout) {
    throw new Error('Method "correlate()" must be implemented by subclass');
  }

  /**
   * Generate a unique command ID for correlation
   *
   * Protected helper for concrete implementations.
   * Generates a UUID-like string for tagging commands.
   *
   * @protected
   * @returns {string} - Unique command ID
   */
  _generateCommandId() {
    return `pilaf-cmd-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Inject correlation marker into a command
   *
   * Protected helper for tag-based strategies.
   * Modifies the command to include a tag or marker.
   *
   * @protected
   * @param {string} command - Original command
   * @param {string} marker - Correlation marker
   * @returns {string} - Modified command with marker
   * @example
   * _injectMarker('/tp @s ~ ~ ~', 'pilaf-cmd-123')
   * // Returns: '/tp @s[tag=pilaf-cmd-123] ~ ~ ~'
   */
  _injectMarker(command, marker) {
    // This is a default implementation - concrete classes may override
    // For Minecraft commands, we can use entity tags
    if (command.includes('@s')) {
      return command.replace('@s', `@s[tag=${marker}]`);
    }
    if (command.includes('@p')) {
      return command.replace('@p', `@p[tag=${marker}]`);
    }
    if (command.includes('@a')) {
      return command.replace('@a', `@a[tag=${marker}]`);
    }
    if (command.includes('@e')) {
      return command.replace('@e', `@e[tag=${marker}]`);
    }
    return command;
  }

  /**
   * Wait for timeout to expire
   *
   * Protected helper for timeout handling.
   *
   * @protected
   * @param {number} timeout - Timeout in milliseconds
   * @returns {Promise<never>} - Rejects after timeout
   */
  async _waitForTimeout(timeout) {
    return new Promise((_, reject) => {
      setTimeout(() => {
        reject(new ResponseTimeoutError('unknown', timeout));
      }, timeout);
    });
  }

  /**
   * Check if an event matches correlation criteria
   *
   * Protected helper for concrete implementations.
   * Provides a default implementation that checks for the
   * correlation marker in the event data or raw text.
   *
   * @protected
   * @param {Object} event - Parsed event to check
   * @param {string} marker - Correlation marker to match
   * @returns {boolean} - True if event matches
   */
  _matchesEvent(event, marker) {
    if (!event || !event.data) {
      return false;
    }

    // Check if marker exists anywhere in the event
    const eventString = JSON.stringify(event);
    return eventString.includes(marker);
  }
}

module.exports = { CorrelationStrategy };
