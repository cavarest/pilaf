/**
 * MinecraftLogParser - Parser for Minecraft server logs
 *
 * Parses raw Minecraft server log lines into structured events.
 * Supports Minecraft versions 1.19, 1.20, 1.21+.
 *
 * Responsibilities (MECE):
 * - ONLY: Parse raw log lines into structured events
 * - NOT: Collect logs (LogCollector's responsibility)
 * - NOT: Correlate events (CorrelationStrategy's responsibility)
 *
 * Event Categories:
 * - Entity: Player join/leave/death/spawn
 * - Movement: Teleport events
 * - Command: Server command execution
 * - World: Time/weather/save changes
 * - Status: Server lifecycle events
 * - Plugin: Custom plugin events (extensible)
 *
 * Usage Example:
 *   const parser = new MinecraftLogParser();
 *   const event = parser.parse('[12:34:56] [Server thread/INFO]: TestPlayer joined');
 *   // Returns event object with type, data, raw, timestamp, thread, level
 */

const { LogParser } = require('../core/LogParser.js');
const { PatternRegistry } = require('./PatternRegistry.js');
const { UnknownPatternError } = require('../errors/index.js');

class MinecraftLogParser extends LogParser {
  /**
   * Create a MinecraftLogParser
   * @param {Object} options - Parser options
   * @param {boolean} [options.includeMetadata=true] - Include timestamp/thread/level in output
   * @param {boolean} [options.strictMode=false] - Throw on unknown patterns (vs return null)
   */
  constructor(options = {}) {
    super();

    /**
     * Parser options
     * @private
     * @type {Object}
     */
    this._options = {
      includeMetadata: options?.includeMetadata !== false,
      strictMode: options?.strictMode || false
    };

    /**
     * Pattern registry for all log patterns
     * @private
     * @type {PatternRegistry}
     */
    this._registry = new PatternRegistry();

    /**
     * Initialize all pattern categories
     * @private
     */
    this._initializePatterns();
  }

  /**
   * Parse a log line into a structured event
   *
   * @param {string} line - Raw log line
   * @returns {Object|null} - Parsed event or null if no pattern matches
   * @property {string} type - Event type (e.g., 'entity.join', 'movement.teleport')
   * @property {Object} data - Event data (extracted from pattern)
   * @property {string} raw - Original log line
   * @property {string} [timestamp] - Time from log line (if includeMetadata)
   * @property {string} [thread] - Thread name (if includeMetadata)
   * @property {string} [level] - Log level (if includeMetadata)
   * @throws {UnknownPatternError} If strictMode is true and pattern doesn't match
   */
  parse(line) {
    if (!line || typeof line !== 'string') {
      return null;
    }

    // Trim the line
    const trimmed = line.trim();
    if (!trimmed) {
      return null;
    }

    // Try to match against all patterns in priority order
    const match = this._registry.match(trimmed);

    if (!match) {
      if (this._options.strictMode) {
        throw new UnknownPatternError(trimmed);
      }
      // Even when no pattern matches, return metadata if requested
      if (this._options.includeMetadata) {
        const metadata = this._extractMetadata(trimmed);
        return {
          type: null,
          data: null,
          raw: trimmed,
          ...metadata
        };
      }
      return null;
    }

    // Build result object
    const result = {
      type: match.name,
      data: match.data,
      raw: trimmed
    };

    // Add metadata if requested
    if (this._options.includeMetadata) {
      const metadata = this._extractMetadata(trimmed);
      Object.assign(result, metadata);
    }

    return result;
  }

  /**
   * Extract metadata (timestamp, thread, level) from log line
   *
   * @private
   * @param {string} line - Log line
   * @returns {Object} - Extracted metadata
   */
  _extractMetadata(line) {
    // Minecraft log format: [HH:MM:SS] [Thread/LEVEL]: Message
    // Or: [HH:MM:SS] [Thread/INFO]: [uuid] Message (for some versions)

    const metadataRegex = /^\[(\d{2}:\d{2}:\d{2})\]\s+\[([^\]]+)\/(INFO|WARN|ERROR|DEBUG)\]:/;
    const match = line.match(metadataRegex);

    if (match) {
      return {
        timestamp: match[1],
        thread: match[2],
        level: match[3]
      };
    }

    // Fallback: try to extract just timestamp
    const timestampRegex = /^\[(\d{2}:\d{2}:\d{2})\]/;
    const timestampMatch = line.match(timestampRegex);

    if (timestampMatch) {
      return {
        timestamp: timestampMatch[1],
        thread: null,
        level: null
      };
    }

    return {
      timestamp: null,
      thread: null,
      level: null
    };
  }

  /**
   * Initialize all Minecraft log patterns
   *
   * Patterns are registered in priority order (higher priority = tested first).
   * Priority ranges:
   * - 10+: Most specific patterns (teleport with coordinates)
   * - 8-9: Specific sub-categories (death types)
   * - 5-7: Player actions and commands
   * - 3-4: World state changes
   * - 1-2: Status messages
   * - 0: Plugin/custom patterns (fallback)
   *
   * @private
   */
  _initializePatterns() {
    // =========================================================================
    // PRIORITY 10: Movement Events (Most Specific)
    // =========================================================================

    this._registry.addPattern(
      'movement.teleport',
      /Teleported\s+(\w+)\s+from\s+([\d.-]+),\s*([\d.-]+),\s*([\d.-]+)\s+to\s+([\d.-]+),\s*([\d.-]+),\s*([\d.-]+)/,
      (match) => ({
        player: match[1],
        from: { x: parseFloat(match[2]), y: parseFloat(match[3]), z: parseFloat(match[4]) },
        to: { x: parseFloat(match[5]), y: parseFloat(match[6]), z: parseFloat(match[7]) }
      }),
      10
    );

    // =========================================================================
    // PRIORITY 8: Entity Death Events
    // =========================================================================

    // Death by entity
    this._registry.addPattern(
      'entity.death.slain',
      /(\w+)\s+was\s+slain\s+by\s+(.+)/,
      (match) => ({
        player: match[1],
        killer: match[2],
        cause: 'entity_attack'
      }),
      8
    );

    // Death by fall
    this._registry.addPattern(
      'entity.death.fall',
      /(\w+)\s+fell\s+from\s+a\s+high\s+place/,
      (match) => ({
        player: match[1],
        cause: 'fall'
      }),
      8
    );

    // Death by fire
    this._registry.addPattern(
      'entity.death.fire',
      /(\w+)\s+(?:burned\s+to\s+death|was\s+burnt\s+to\s+a\s+crisp)/,
      (match) => ({
        player: match[1],
        cause: 'fire'
      }),
      8
    );

    // Death by lava
    this._registry.addPattern(
      'entity.death.lava',
      /(\w+)\s+(?:tried\s+to\s+swim\s+in\s+lava|was\s+killed\s+by\s+(?:Magma|Lava)(?:\s+Block)?)/,
      (match) => ({
        player: match[1],
        cause: 'lava'
      }),
      8
    );

    // Death by drowning
    this._registry.addPattern(
      'entity.death.drown',
      /(\w+)\s+drowned/,
      (match) => ({
        player: match[1],
        cause: 'drown'
      }),
      8
    );

    // Death by sprinting into wall
    this._registry.addPattern(
      'entity.death.sprint',
      /(\w+)\s+splatted\s+against\s+a\s+wall/,
      (match) => ({
        player: match[1],
        cause: 'sprint_into_wall'
      }),
      8
    );

    // Generic death
    this._registry.addPattern(
      'entity.death.generic',
      /(\w+)\s+died/,
      (match) => ({
        player: match[1],
        cause: 'unknown'
      }),
      8
    );

    // =========================================================================
    // PRIORITY 6: Player Actions (join, leave, command)
    // =========================================================================

    // Player joined
    this._registry.addPattern(
      'entity.join',
      /([^\s]+)\s+joined\s+the\s+game/,
      (match) => ({
        player: match[1]
      }),
      6
    );

    // Player left
    this._registry.addPattern(
      'entity.leave',
      /(\w+)\s+(?:lost\s+connection:\s*(.+)|left\s+the\s+game)/,
      (match) => ({
        player: match[1],
        reason: match[2]?.trim() || 'Left the game'
      }),
      6
    );

    // Player issued command
    this._registry.addPattern(
      'command.issued',
      /(\w+)\s+issued\s+server\s+command:\s*(.+)/,
      (match) => ({
        player: match[1],
        command: match[2]?.trim()
      }),
      6
    );

    // Player UUID/spawn (modern versions)
    this._registry.addPattern(
      'entity.spawn',
      /UUID\s+of\s+player\s+(\w+)\s+is\s+([a-f0-9-]{36})/,
      (match) => ({
        player: match[1],
        uuid: match[2]
      }),
      6
    );

    // =========================================================================
    // PRIORITY 4: World Events (time, weather, save)
    // =========================================================================

    // Time change
    this._registry.addPattern(
      'world.time',
      /Changing\s+the\s+time\s+to\s+(\d+)/,
      (match) => ({
        time: parseInt(match[1], 10)
      }),
      4
    );

    // Weather change
    this._registry.addPattern(
      'world.weather',
      /Changing\s+the\s+weather\s+to\s+(\w+)/,
      (match) => ({
        weather: match[1]
      }),
      4
    );

    // Difficulty change
    this._registry.addPattern(
      'world.difficulty',
      /Changing\s+the\s+difficulty\s+to\s+(\w+)/,
      (match) => ({
        difficulty: match[1]
      }),
      4
    );

    // Game mode change
    this._registry.addPattern(
      'world.gamemode',
      /(?:The\s+game\s+mode|Gamemode)\s+has\s+been\s+updated\s+to\s+(\w+)/,
      (match) => ({
        gamemode: match[1]
      }),
      4
    );

    // Save start
    this._registry.addPattern(
      'world.save.start',
      /Saving\s+(?:the\s+game|chunks\s+for\s+level)/,
      (match) => ({}),
      4
    );

    // Save complete
    this._registry.addPattern(
      'world.save.complete',
      /Saved\s+the\s+game/,
      (match) => ({}),
      4
    );

    // =========================================================================
    // PRIORITY 2: Status Events (server lifecycle)
    // =========================================================================

    // Server starting (version)
    this._registry.addPattern(
      'status.start',
      /Starting\s+minecraft\s+server\s+version\s+(.+)/,
      (match) => ({
        version: match[1]?.trim()
      }),
      2
    );

    // Server starting (generic)
    this._registry.addPattern(
      'status.starting',
      /Starting\s+(?:minecraft\s+server|server)/,
      (match) => ({}),
      2
    );

    // Loading properties
    this._registry.addPattern(
      'status.loading',
      /Loading\s+(?:properties|chunks|world)/,
      (match) => ({}),
      2
    );

    // Default game type
    this._registry.addPattern(
      'status.gametype',
      /Default\s+game\s+type:\s+(\w+)/,
      (match) => ({
        gameType: match[1]
      }),
      2
    );

    // Generating keypair
    this._registry.addPattern(
      'status.keypair',
      /Generating\s+keypair/,
      (match) => ({}),
      2
    );

    // Preparing level
    this._registry.addPattern(
      'status.preparing',
      /Preparing\s+(?:level\s+"([^"]+)"|start\s+region)/,
      (match) => match[1] ? { level: match[1] } : {},
      2
    );

    // Done loading
    this._registry.addPattern(
      'status.done',
      /Done\s+\([^)]+\)!\s+For\s+help,\s+type\s+"help"/,
      (match) => ({}),
      2
    );

    // Time elapsed
    this._registry.addPattern(
      'status.elapsed',
      /Time\s+elapsed:\s+(\d+)\s+ms/,
      (match) => ({
        elapsed: parseInt(match[1], 10)
      }),
      2
    );

    // =========================================================================
    // PRIORITY 1: Plugin Events (extensible fallback)
    // =========================================================================

    // Placeholder for plugin-defined patterns
    // Users can add custom patterns via addPattern() method
  }

  /**
   * Add a custom pattern to the parser
   *
   * Allows users to extend the parser with plugin-specific patterns.
   *
   * @param {string} name - Pattern name (e.g., 'plugin.myevent')
   * @param {string|RegExp} pattern - Regex pattern to match
   * @param {Function} handler - Function to extract data from regex match
   * @param {number} [priority] - Priority (0-10, default 1 for plugin patterns)
   * @returns {void}
   */
  addPattern(name, pattern, handler, priority) {
    this._registry.addPattern(name, pattern, handler, priority ?? 1);
  }

  /**
   * Remove a pattern from the parser
   *
   * @param {string} name - Pattern name to remove
   * @returns {boolean} - True if pattern was removed
   */
  removePattern(name) {
    return this._registry.removePattern(name);
  }

  /**
   * Get all registered patterns
   *
   * @returns {Array} - Array of pattern definitions
   */
  getPatterns() {
    return this._registry.getPatterns();
  }

  /**
   * Clone the parser with all its patterns
   *
   * @returns {MinecraftLogParser} - A new parser instance with cloned patterns
   */
  clone() {
    const cloned = new MinecraftLogParser(this._options);
    cloned._registry = this._registry.clone();
    return cloned;
  }
}

module.exports = { MinecraftLogParser };
