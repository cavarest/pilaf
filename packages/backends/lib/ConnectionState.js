/**
 * ConnectionState - Represents the state of a bot or connection
 * Uses freeze() to create immutable enum-like values
 */

class ConnectionState {
  static get DISCONNECTED() { return 'DISCONNECTED'; }
  static get CONNECTING() { return 'CONNECTING'; }
  static get CONNECTED() { return 'CONNECTED'; }
  static get SPAWNING() { return 'SPAWNING'; }
  static get SPAWNED() { return 'SPAWNED'; }
  static get ERROR() { return 'ERROR'; }
  static get DISCONNECTING() { return 'DISCONNECTING'; }

  /**
   * Check if state is a terminal state (no further transitions possible)
   * @param {string} state - State to check
   * @returns {boolean}
   */
  static isTerminal(state) {
    return [
      ConnectionState.DISCONNECTED,
      ConnectionState.ERROR
    ].includes(state);
  }

  /**
   * Check if state allows bot operations
   * @param {string} state - State to check
   * @returns {boolean}
   */
  static canPerformBotOperations(state) {
    return state === ConnectionState.SPAWNED;
  }

  /**
   * Check if state is a transitioning state
   * @param {string} state - State to check
   * @returns {boolean}
   */
  static isTransitioning(state) {
    return [
      ConnectionState.CONNECTING,
      ConnectionState.SPAWNING,
      ConnectionState.DISCONNECTING
    ].includes(state);
  }
}

module.exports = { ConnectionState };
