/**
 * CommandRouter - Abstract base class for command routing
 *
 * Defines the interface for routing commands to the appropriate channel
 * (bot chat, RCON, or log monitoring). Concrete implementations must
 * extend this class and implement the abstract route() method.
 *
 * @abstract
 * @class
 *
 * Responsibilities (MECE):
 * - ONLY: Decide which channel should handle a command
 * - NOT: Execute commands (backend's responsibility)
 * - NOT: Parse responses (parser's responsibility)
 *
 * Routing Logic:
 * - /data get commands → RCON (structured NBT responses)
 * - /execute with 'run data' → RCON (structured queries)
 * - useRcon option → RCON (forced routing)
 * - expectLogResponse option → Log monitoring (event correlation)
 * - Default → Bot chat (player commands)
 *
 * Usage:
 *   class SmartCommandRouter extends CommandRouter {
 *     route(command, context) {
 *       // Analyze command and return { channel, options }
 *     }
 *   }
 */

class CommandRouter {
  /**
   * Routing channels
   * @static
   * @enum {string}
   */
  static get CHANNELS() {
    return {
      BOT: 'bot',           // Send via bot.chat()
      RCON: 'rcon',         // Send via RCON
      LOG: 'log'            // Send via bot and wait for log response
    };
  }

  /**
   * Create a CommandRouter
   * @param {Object} options - Router options
   * @param {Object} [options.rules] - Custom routing rules
   * @throws {Error} Direct instantiation of abstract class
   */
  constructor(options = {}) {
    // Prevent direct instantiation of abstract class
    if (this.constructor === CommandRouter) {
      throw new Error('CommandRouter is abstract and cannot be instantiated directly');
    }

    /**
     * Custom routing rules
     * @protected
     * @type {Array<{pattern: string|RegExp, channel: string}>}
     */
    this._customRules = options?.rules ? Object.entries(options.rules).map(([pattern, channel]) => ({ pattern, channel })) : [];
  }

  /**
   * Route a command to the appropriate channel
   *
   * Abstract method that must be implemented by concrete classes.
   * Should analyze the command and context to determine the best channel.
   *
   * @abstract
   * @param {string} command - The command to route
   * @param {Object} context - Routing context
   * @param {Object} context.options - Command options (useRcon, expectLogResponse, etc.)
   * @param {string} [context.username] - Bot username (for correlation)
   * @param {Object} [context.backend] - Backend instance (for advanced routing)
   * @returns {Object} Routing decision
   * @property {string} channel - One of: 'bot', 'rcon', 'log'
   * @property {Object} options - Options to pass to the channel
   * @example
   * // Returns:
   * // { channel: 'rcon', options: { timeout: 10000 } }
   */
  route(command, context) {
    throw new Error('Method "route()" must be implemented by subclass');
  }

  /**
   * Add a custom routing rule
   *
   * @param {string|RegExp} pattern - Command pattern to match
   * @param {string} channel - Channel to route to ('bot', 'rcon', 'log')
   * @returns {void}
   * @throws {Error} If channel is invalid
   */
  addRule(pattern, channel) {
    const validChannels = Object.values(CommandRouter.CHANNELS);
    if (!validChannels.includes(channel)) {
      throw new Error(`Invalid channel: ${channel}. Must be one of: ${validChannels.join(', ')}`);
    }

    // Remove existing rule with same pattern (if any)
    this.removeRule(pattern);

    // Add new rule
    this._customRules.push({ pattern, channel });
  }

  /**
   * Remove a custom routing rule
   *
   * @param {string|RegExp} pattern - Pattern to remove
   * @returns {boolean} - True if rule was removed, false if not found
   */
  removeRule(pattern) {
    const initialLength = this._customRules.length;
    this._customRules = this._customRules.filter(rule => {
      // For RegExp, we need to compare by stringification
      if (pattern instanceof RegExp && rule.pattern instanceof RegExp) {
        return rule.pattern.toString() !== pattern.toString();
      }
      return rule.pattern !== pattern;
    });
    return this._customRules.length < initialLength;
  }

  /**
   * Check if a command matches a pattern
   *
   * Protected helper method for concrete implementations.
   *
   * @protected
   * @param {string} command - Command to check
   * @param {string|RegExp} pattern - Pattern to match against
   * @returns {boolean} - True if command matches pattern
   */
  _matchesPattern(command, pattern) {
    if (pattern instanceof RegExp) {
      return pattern.test(command);
    }
    return command.startsWith(pattern);
  }

  /**
   * Get custom rules
   *
   * @returns {Array<{pattern: string|RegExp, channel: string}>} - Copy of custom rules array
   */
  getRules() {
    return [...this._customRules];
  }
}

module.exports = { CommandRouter };
