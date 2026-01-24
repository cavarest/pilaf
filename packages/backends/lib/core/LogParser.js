/**
 * LogParser - Abstract base class for log parsing
 *
 * Defines the interface for parsing raw log lines into structured events.
 * Concrete implementations must extend this class and implement the
 * abstract parse() method.
 *
 * @abstract
 * @class
 *
 * Responsibilities (MECE):
 * - ONLY: Parse raw log lines into structured events
 * - NOT: Collect logs (LogCollector's responsibility)
 * - NOT: Emit events to consumers (LogMonitor's responsibility)
 * - NOT: Correlate responses (CorrelationStrategy's responsibility)
 *
 * Usage:
 *   class MinecraftLogParser extends LogParser {
 *     parse(line) {
 *       // Parse and return { type, data, raw } or null
 *     }
 *   }
 */

class LogParser {
  /**
   * Create a LogParser
   * @throws {Error} Direct instantiation of abstract class
   */
  constructor() {
    // Prevent direct instantiation of abstract class
    if (this.constructor === LogParser) {
      throw new Error('LogParser is abstract and cannot be instantiated directly');
    }
  }

  /**
   * Parse a log line into a structured event
   *
   * Abstract method that must be implemented by concrete classes.
   * Should analyze the log line and extract structured data.
   *
   * @abstract
   * @param {string} line - Raw log line to parse
   * @returns {Object|null} - Parsed event or null if line doesn't match any pattern
   * @property {string} type - Event type (e.g., 'teleport', 'death', 'command')
   * @property {Object} data - Parsed event data (structure varies by type)
   * @property {string} raw - Original raw log line
   * @example
   * // Returns:
   * // {
   * //   type: 'teleport',
   * //   data: { player: 'TestPlayer', position: { x: 100, y: 64, z: 100 } },
   * //   raw: '[12:34:56] Teleported TestPlayer to 100.0, 64.0, 100.0'
   * // }
   */
  parse(line) {
    throw new Error('Method "parse()" must be implemented by subclass');
  }

  /**
   * Add a parsing pattern (optional method for pattern-based parsers)
   *
   * Default implementation throws. Pattern-based parsers should override.
   *
   * @param {string} name - Pattern name/identifier
   * @param {RegExp|string} pattern - Regex pattern or string pattern
   * @param {Function} handler - Handler function: (match) => data
   * @returns {void}
   * @throws {Error} If not supported by parser implementation
   */
  addPattern(name, pattern, handler) {
    throw new Error('Method "addPattern()" is not supported by this parser');
  }

  /**
   * Remove a parsing pattern (optional method for pattern-based parsers)
   *
   * Default implementation throws. Pattern-based parsers should override.
   *
   * @param {string} name - Pattern name to remove
   * @returns {boolean} - True if pattern was removed, false if not found
   * @throws {Error} If not supported by parser implementation
   */
  removePattern(name) {
    throw new Error('Method "removePattern()" is not supported by this parser');
  }

  /**
   * Get all registered patterns (optional method for pattern-based parsers)
   *
   * Default implementation throws. Pattern-based parsers should override.
   *
   * @returns {Array<string>} - Array of pattern names
   * @throws {Error} If not supported by parser implementation
   */
  getPatterns() {
    throw new Error('Method "getPatterns()" is not supported by this parser');
  }

  /**
   * Validate parse result format
   *
   * Protected method to ensure parse() returns correct format.
   * Used by concrete implementations to validate their output.
   *
   * @protected
   * @param {Object|null} result - Result from parse()
   * @returns {boolean} - True if format is valid
   */
  _validateResult(result) {
    if (result === null) {
      return true; // null is valid (no match)
    }

    return (
      typeof result === 'object' &&
      typeof result.type === 'string' &&
      typeof result.data === 'object' &&
      typeof result.raw === 'string'
    );
  }
}

module.exports = { LogParser };
