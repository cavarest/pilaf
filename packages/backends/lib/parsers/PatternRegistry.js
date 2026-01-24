/**
 * PatternRegistry - Centralized pattern management for log parsing
 *
 * Responsibilities (MECE):
 * - ONLY: Store, compile, and match regex patterns
 * - NOT: Parse log lines (LogParser's responsibility - uses this registry)
 * - NOT: Emit events (LogMonitor's responsibility)
 *
 * Design Principles:
 * - Lazy compilation: Regex compiled on first use (performance)
 * - Priority ordering: Patterns tested in order of addition
 * - Named capture groups: Support for extracting named data
 * - Thread-safe: Safe for concurrent access
 *
 * Usage:
 *   const registry = new PatternRegistry();
 *   registry.addPattern('teleport', /Teleported (\w+) to (.+)/, (match) => ({
 *     player: match[1],
 *     destination: match[2]
 *   }));
 *   const result = registry.match(logLine);
 */

class PatternRegistry {
  /**
   * Create a PatternRegistry
   * @param {Object} options - Registry options
   * @param {boolean} [options.caseInsensitive=false] - Make all patterns case-insensitive
   */
  constructor(options = {}) {
    /**
     * Map of pattern name to pattern definition
     * @private
     * @type {Map<string, Object>}
     */
    this._patterns = new Map();

    /**
     * Ordered array of pattern names (for priority ordering)
     * @private
     * @type {Array<string>}
     */
    this._patternOrder = [];

    /**
     * Compiled regex cache
     * @private
     * @type {Map<string, RegExp>}
     */
    this._compiled = new Map();

    /**
     * Case-insensitive flag
     * @private
     * @type {boolean}
     */
    this._caseInsensitive = options?.caseInsensitive || false;
  }

  /**
   * Add a pattern to the registry
   *
   * @param {string} name - Unique pattern name/identifier
   * @param {RegExp|string} pattern - Regex pattern or string (will be converted to RegExp)
   * @param {Function} handler - Handler function: (match: RegExpMatchArray) => Object
   * @param {number} [priority] - Optional priority (lower = higher priority, default: append to end)
   * @returns {void}
   * @throws {Error} If pattern name already exists
   * @throws {Error} If pattern is invalid
   * @throws {Error} If handler is not a function
   *
   * @example
   * // Add with regex
   * registry.addPattern('teleport', /Teleported (\w+) to (.+)/, (match) => ({
   *   player: match[1],
   *   position: match[2]
   * }));
   *
   * // Add with string (converted to regex)
   * registry.addPattern('join', 'UUID of player .* is (.+)', (match) => ({
   *   username: match[1]
   * }));
   */
  addPattern(name, pattern, handler, priority) {
    // Validate name
    if (typeof name !== 'string' || name.trim() === '') {
      throw new Error('Pattern name must be a non-empty string');
    }

    // Check for duplicate
    if (this._patterns.has(name)) {
      throw new Error(`Pattern "${name}" already exists`);
    }

    // Validate pattern
    if (!(pattern instanceof RegExp) && typeof pattern !== 'string') {
      throw new Error('Pattern must be a RegExp or string');
    }

    // Validate handler
    if (typeof handler !== 'function') {
      throw new Error('Handler must be a function');
    }

    // Convert string to RegExp if needed
    let regexPattern = pattern;
    if (typeof pattern === 'string') {
      const flags = this._caseInsensitive ? 'i' : '';
      regexPattern = new RegExp(pattern, flags);
    }

    // Store pattern definition
    this._patterns.set(name, {
      name,
      pattern: regexPattern,
      handler,
      priority: priority ?? this._patternOrder.length
    });

    // Update order
    if (priority !== undefined) {
      this._patternOrder.push(name);
      this._patternOrder.sort((a, b) => {
        const pA = this._patterns.get(a).priority;
        const pB = this._patterns.get(b).priority;
        return pA - pB;
      });
    } else {
      this._patternOrder.push(name);
    }
  }

  /**
   * Remove a pattern from the registry
   *
   * @param {string} name - Pattern name to remove
   * @returns {boolean} - True if pattern was removed, false if not found
   */
  removePattern(name) {
    if (!this._patterns.has(name)) {
      return false;
    }

    this._patterns.delete(name);
    this._compiled.delete(name);
    this._patternOrder = this._patternOrder.filter(n => n !== name);

    return true;
  }

  /**
   * Get a pattern by name
   *
   * @param {string} name - Pattern name
   * @returns {Object|null} - Pattern definition or null if not found
   * @property {RegExp} pattern - The regex pattern
   * @property {Function} handler - The handler function
   */
  getPattern(name) {
    return this._patterns.get(name) || null;
  }

  /**
   * Get all pattern names
   *
   * @returns {Array<string>} - Array of pattern names in priority order
   */
  getPatterns() {
    return [...this._patternOrder];
  }

  /**
   * Get the count of registered patterns
   *
   * @returns {number} - Number of patterns
   */
  get size() {
    return this._patterns.size;
  }

  /**
   * Clear all patterns
   *
   * @returns {void}
   */
  clear() {
    this._patterns.clear();
    this._compiled.clear();
    this._patternOrder = [];
  }

  /**
   * Match a line against all registered patterns
   *
   * Tests patterns in priority order and returns the first match.
   * Returns null if no patterns match.
   *
   * @param {string} line - Log line to match
   * @returns {Object|null} - Match result or null if no match
   * @property {string} name - Name of the pattern that matched
   * @property {*} data - Data returned by the pattern's handler
   * @property {RegExpMatchArray} match - The regex match result
   * @property {string} raw - Original input line
   *
   * @example
   * const result = registry.match('[12:34:56] Teleported TestPlayer to 100.0, 64.0, 100.0');
   * // Returns:
   * // {
   * //   name: 'teleport',
   * //   data: { player: 'TestPlayer', position: '100.0, 64.0, 100.0' },
   * //   match: RegExpMatchArray,
   * //   raw: '[12:34:56] Teleported TestPlayer to 100.0, 64.0, 100.0'
   * // }
   */
  match(line) {
    if (typeof line !== 'string') {
      return null;
    }

    // Test each pattern in priority order
    for (const name of this._patternOrder) {
      const patternDef = this._patterns.get(name);
      const regex = this._getCompiledPattern(name, patternDef.pattern);

      const match = line.match(regex);
      if (match) {
        try {
          const data = patternDef.handler(match);
          return {
            name,
            data,
            match,
            raw: line
          };
        } catch (error) {
          // Handler failed - continue to next pattern
          // (Or we could emit an error event)
          continue;
        }
      }
    }

    return null;
  }

  /**
   * Match a line against a specific pattern
   *
   * @param {string} line - Log line to match
   * @param {string} patternName - Name of pattern to match against
   * @returns {Object|null} - Match result or null if no match
   */
  matchPattern(line, patternName) {
    const patternDef = this._patterns.get(patternName);
    if (!patternDef) {
      return null;
    }

    const regex = this._getCompiledPattern(patternName, patternDef.pattern);
    const match = line.match(regex);

    if (!match) {
      return null;
    }

    try {
      const data = patternDef.handler(match);
      return {
        name: patternName,
        data,
        match,
        raw: line
      };
    } catch (error) {
      return null;
    }
  }

  /**
   * Test if a pattern matches (without executing handler)
   *
   * @param {string} line - Log line to test
   * @param {string} patternName - Name of pattern to test
   * @returns {boolean} - True if pattern matches
   */
  test(line, patternName) {
    const patternDef = this._patterns.get(patternName);
    if (!patternDef) {
      return false;
    }

    const regex = this._getCompiledPattern(patternName, patternDef.pattern);
    return regex.test(line);
  }

  /**
   * Get compiled regex for a pattern (lazy compilation)
   *
   * @private
   * @param {string} name - Pattern name
   * @param {RegExp} pattern - The regex pattern
   * @returns {RegExp} - Compiled regex (cached or newly compiled)
   */
  _getCompiledPattern(name, pattern) {
    // Check cache first
    if (this._compiled.has(name)) {
      return this._compiled.get(name);
    }

    // For string patterns, we've already converted to RegExp in addPattern
    // For RegExp patterns, use as-is
    this._compiled.set(name, pattern);
    return pattern;
  }

  /**
   * Clone the registry (creates a new instance with same patterns)
   *
   * @returns {PatternRegistry} - New registry with copied patterns
   */
  clone() {
    const cloned = new PatternRegistry({
      caseInsensitive: this._caseInsensitive
    });

    // Copy all patterns
    for (const [name, patternDef] of this._patterns) {
      cloned.addPattern(
        name,
        patternDef.pattern,
        patternDef.handler,
        patternDef.priority
      );
    }

    return cloned;
  }

  /**
   * Export patterns as JSON (for serialization)
   *
   * Note: Handlers cannot be serialized (functions are lost)
   *
   * @returns {Object} - JSON-serializable representation
   */
  toJSON() {
    const patterns = {};

    for (const [name, patternDef] of this._patterns) {
      patterns[name] = {
        name,
        pattern: patternDef.pattern.toString(),
        priority: patternDef.priority
        // Note: handler function is lost
      };
    }

    return {
      caseInsensitive: this._caseInsensitive,
      patternCount: this._patterns.size,
      patterns
    };
  }
}

module.exports = { PatternRegistry };
