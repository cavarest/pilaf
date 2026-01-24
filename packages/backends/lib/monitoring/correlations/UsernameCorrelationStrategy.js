/**
 * UsernameCorrelationStrategy - Correlates events by player username
 *
 * This correlation strategy groups events by player username to track
 * player sessions across all their activities. Useful for:
 *
 * - Tracking player session lifecycle (join -> actions -> leave)
 * - Monitoring player behavior patterns
 * - Generating per-player statistics
 * - Debugging player-specific issues
 *
 * Example use case:
 * - Player joins: entity.join event with data.player = "Steve"
 * - Player dies: entity.death.slain event with data.player = "Steve"
 * - Player leaves: entity.leave event with data.player = "Steve"
 * - Strategy groups all events under username "Steve"
 *
 * Features:
 * - Configurable username extraction function
 * - Session tracking (start/end times)
 * - Active/inactive session status
 * - Automatic session cleanup on leave
 *
 * Usage Example:
 *   const strategy = new UsernameCorrelationStrategy({
 *     usernameExtractor: (event) => event.data?.player
 *   });
 *
 *   const joinEvent = { type: 'entity.join', data: { player: 'Steve' } };
 *   const correlation = strategy.correlate(joinEvent);
 *   // correlation: { username: 'Steve', events: [joinEvent], sessionStart: Date.now(), isActive: true }
 */

const { EventEmitter } = require('events');

/**
 * UsernameCorrelationStrategy class for username-based event correlation
 * @extends EventEmitter
 */
class UsernameCorrelationStrategy extends EventEmitter {
  /**
   * Create a UsernameCorrelationStrategy
   *
   * @param {Object} options - Strategy options
   * @param {Function} [options.usernameExtractor] - Function to extract username from event
   *                                                 Default: (event) => event.data?.player
   * @param {boolean} [options.includeMetadata=true] - Include session metadata (start/end times)
   * @param {boolean} [options.autoCleanup=true] - Auto-remove inactive sessions
   */
  constructor(options = {}) {
    super();

    /**
     * Function to extract username from event
     * @private
     * @type {Function}
     */
    this._usernameExtractor = options.usernameExtractor || ((event) => event.data?.player);

    /**
     * Whether to include session metadata
     * @private
     * @type {boolean}
     */
    this._includeMetadata = options.includeMetadata !== false;

    /**
     * Whether to auto-remove inactive sessions
     * @private
     * @type {boolean}
     */
    this._autoCleanup = options.autoCleanup !== false;

    /**
     * Map of active player sessions
     * @private
     * @type {Map<string, Object>}
     */
    this._sessions = new Map();
  }

  /**
   * Correlate an event by username
   *
   * @param {Object} event - Parsed event object
   * @returns {Object|null} - Session object with username, events, session metadata, or null if no username
   */
  correlate(event) {
    if (!event) {
      return null;
    }

    const username = this._usernameExtractor(event);
    if (!username) {
      return null;
    }

    const now = Date.now();

    if (!this._sessions.has(username)) {
      this._sessions.set(username, {
        username,
        events: [],
        sessionStart: this._includeMetadata ? now : undefined,
        sessionEnd: undefined,
        isActive: true
      });
    }

    const session = this._sessions.get(username);
    session.events.push(event);

    // Mark session as inactive on leave event
    if (event.type === 'entity.leave') {
      session.isActive = false;
      if (this._includeMetadata) {
        session.sessionEnd = now;
      }

      // Auto-cleanup inactive sessions if enabled
      if (this._autoCleanup) {
        this._sessions.delete(username);
      }
    }

    // Emit session update event
    this.emit('session', session);

    return session;
  }

  /**
   * Get all active sessions
   *
   * @returns {Array<Object>} - Array of session objects
   */
  getActiveCorrelations() {
    return Array.from(this._sessions.values());
  }

  /**
   * Get session by username
   *
   * @param {string} username - Username to look up
   * @returns {Object|undefined} - Session object or undefined
   */
  getSession(username) {
    return this._sessions.get(username);
  }

  /**
   * Check if a player has an active session
   *
   * @param {string} username - Username to check
   * @returns {boolean} - True if player has an active session
   */
  hasActiveSession(username) {
    const session = this._sessions.get(username);
    return !!(session && session.isActive);
  }

  /**
   * Get all currently online players
   *
   * @returns {Array<string>} - Array of usernames with active sessions
   */
  getOnlinePlayers() {
    return Array.from(this._sessions.values())
      .filter(session => session.isActive)
      .map(session => session.username);
  }

  /**
   * Reset all session state
   *
   * @returns {void}
   */
  reset() {
    this._sessions.clear();
    this.emit('reset');
  }

  /**
   * End a specific session manually
   *
   * @param {string} username - Username to end session for
   * @returns {boolean} - True if session was ended, false if not found
   */
  endSession(username) {
    const session = this._sessions.get(username);
    if (!session) {
      return false;
    }

    session.isActive = false;
    if (this._includeMetadata) {
      session.sessionEnd = Date.now();
    }

    // Auto-cleanup inactive sessions if enabled
    if (this._autoCleanup) {
      this._sessions.delete(username);
    }

    this.emit('sessionEnd', session);
    return true;
  }

  /**
   * Get session statistics
   *
   * @returns {Object} - Statistics object with total, active, inactive counts
   */
  getStatistics() {
    const sessions = Array.from(this._sessions.values());
    return {
      total: sessions.length,
      active: sessions.filter(s => s.isActive).length,
      inactive: sessions.filter(s => !s.isActive).length
    };
  }

  /**
   * Get the number of active sessions
   *
   * @type {number}
   */
  get size() {
    return this._sessions.size;
  }
}

module.exports = { UsernameCorrelationStrategy };
