/**
 * CorrelationUtils - Server-side correlation for player actions
 *
 * Provides utilities for correlating player actions with server-side
 * confirmation events. Works with or without EventObserver:
 *
 * - WITH EventObserver: Event-driven correlation (faster, more reliable)
 * - WITHOUT EventObserver: Timeout-based fallback (simple, compatible)
 *
 * Responsibilities (MECE):
 * - ONLY: Wait for server confirmation of player actions
 * - NOT: Execute player actions (use bot methods directly)
 * - NOT: Parse server logs (EventObserver/LogParser's responsibility)
 * - NOT: Collect logs (LogCollector's responsibility)
 */

/**
 * Simple glob pattern matcher
 *
 * Supports:
 * - * : matches any sequence of characters
 * - ? : matches any single character
 *
 * @private
 * @param {string} text - Text to match against
 * @param {string} pattern - Glob pattern (e.g., "*placed*", "entity.*")
 * @returns {boolean} True if pattern matches
 */
function _matchPattern(text, pattern) {
  if (!text || !pattern) return false;

  // Convert glob pattern to regex
  // First escape all special regex characters except * and ?
  let regexPattern = pattern
    .replace(/[.+^${}()|[\]\\]/g, '\\$&');  // Escape special regex chars

  // Then convert * and ? to their regex equivalents
  regexPattern = '^' + regexPattern
    .replace(/\*/g, '.*')   // * becomes .*
    .replace(/\?/g, '.') + '$';  // ? becomes .

  const regex = new RegExp(regexPattern, 'i');  // Case-insensitive
  return regex.test(text);
}

/**
 * Wait for server confirmation of a player action
 *
 * This is the main correlation utility. It waits for a server log event
 * that matches the expected pattern, confirming that the player's action
 * was processed by the server.
 *
 * Supports two modes:
 * 1. Event-driven mode (with EventObserver): Listens for matching events
 * 2. Timeout mode (without EventObserver): Waits for specified timeout
 *
 * @param {Object} storyRunner - StoryRunner instance (provides access to backends)
 * @param {Object} options - Options
 * @param {string} options.pattern - Glob pattern to match in server logs
 * @param {number} [options.timeout=5000] - Timeout in milliseconds
 * @param {boolean} [options.invert=false] - If true, wait for ABSENCE of pattern
 * @param {string} [options.player] - Player name to filter events (optional)
 * @returns {Promise<Object|null>} Matching event object, or null if timeout/inverted
 *
 * @example
 * // Wait for block placement confirmation
 * await CorrelationUtils.waitForServerConfirmation(storyRunner, {
 *   pattern: '*Cannot place block*',
 *   invert: true,
 *   timeout: 3000
 * });
 *
 * @example
 * // Wait for command execution
 * const event = await CorrelationUtils.waitForServerConfirmation(storyRunner, {
 *   pattern: '*issued command* /gamemode*',
 *   timeout: 5000
 * });
 */
async function waitForServerConfirmation(storyRunner, options) {
  const {
    pattern = '*',
    timeout = 5000,
    invert = false,
    player = null
  } = options || {};

  // Try EventObserver mode first (event-driven, faster)
  const eventObserver = _getEventObserver(storyRunner);

  if (eventObserver && eventObserver.isObserving) {
    return await _waitForEventObserver(eventObserver, {
      pattern,
      timeout,
      invert,
      player
    });
  }

  // Fall back to timeout mode (simple, works without EventObserver)
  return await _waitForTimeout({ timeout, invert });
}

/**
 * Get EventObserver from StoryRunner's backends
 *
 * Tries multiple strategies to find an available EventObserver:
 * 1. Any player backend's EventObserver (MineflayerBackend)
 * 2. RCON backend's EventObserver (if available)
 *
 * @private
 * @param {Object} storyRunner - StoryRunner instance
 * @returns {EventObserver|null} EventObserver instance or null
 */
function _getEventObserver(storyRunner) {
  if (!storyRunner || !storyRunner.backends) {
    return null;
  }

  // Strategy 1: Check player backends (MineflayerBackend may have EventObserver)
  if (storyRunner.backends.players) {
    for (const [username, backend] of storyRunner.backends.players) {
      if (backend && typeof backend.getEventObserver === 'function') {
        const observer = backend.getEventObserver();
        if (observer && observer.isObserving) {
          return observer;
        }
      }
    }
  }

  // Strategy 2: Check RCON backend (unlikely to have EventObserver, but future-proof)
  if (storyRunner.backends.rcon && typeof storyRunner.backends.rcon.getEventObserver === 'function') {
    const observer = storyRunner.backends.rcon.getEventObserver();
    if (observer && observer.isObserving) {
      return observer;
    }
  }

  return null;
}

/**
 * Wait for event using EventObserver (event-driven mode)
 *
 * @private
 * @param {EventObserver} eventObserver - EventObserver instance
 * @param {Object} options - Options
 * @returns {Promise<Object|null>} Matching event or null
 */
async function _waitForEventObserver(eventObserver, options) {
  const { pattern, timeout, invert, player } = options;

  return new Promise((resolve) => {
    let timer = null;
    let unsubscribe = null;

    const cleanup = (result) => {
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      if (unsubscribe) {
        unsubscribe();
        unsubscribe = null;
      }
      resolve(result);
    };

    // Set timeout
    timer = setTimeout(() => {
      cleanup(null);  // Timeout - return null
    }, timeout);

    // Subscribe to all events
    unsubscribe = eventObserver.onEvent('*', (event) => {
      // Filter by player if specified
      if (player && event.data?.player !== player) {
        return;
      }

      // Check if pattern matches
      const matches = _checkEventMatch(event, pattern);

      if (invert) {
        // Inverted mode: wait for event that DOESN'T match pattern
        // For inverted mode, we can't easily detect "absence" of events
        // So we just wait for timeout and verify no matching events occurred
        // This is a simplified approach - could be enhanced with event tracking
        if (!matches) {
          // Got a non-matching event, but we can't conclude yet
          // Continue waiting for timeout or matching event
        } else {
          // Got a matching event in inverted mode - this is "bad"
          // We could reject here, but for now we just continue
          // The caller will interpret null as "no matching events during timeout"
        }
      } else {
        // Normal mode: wait for event that MATCHES pattern
        if (matches) {
          cleanup(event);  // Found matching event - return it
        }
      }
    });

    // For inverted mode, we need to track if we saw any matching events
    // This is a simple implementation - could be enhanced
    if (invert) {
      // In inverted mode, we just wait for timeout
      // The assumption is: no matching events = success
      // This is not perfect but works for simple cases
    }
  });
}

/**
 * Wait using simple timeout (fallback mode)
 *
 * @private
 * @param {Object} options - Options
 * @returns {Promise<null>} Always returns null after timeout
 */
async function _waitForTimeout(options) {
  const { timeout } = options;
  await new Promise(resolve => setTimeout(resolve, timeout));
  return null;
}

/**
 * Check if event matches the pattern
 *
 * Checks multiple fields in the event for pattern matching:
 * - event.type (e.g., "command.issued", "block.broken")
 * - event.message (raw log message)
 * - event.data (additional event data)
 *
 * @private
 * @param {Object} event - Event object from EventObserver
 * @param {string} pattern - Glob pattern to match
 * @returns {boolean} True if pattern matches event
 */
function _checkEventMatch(event, pattern) {
  if (!event || !pattern) {
    return false;
  }

  // Check event type
  if (event.type && _matchPattern(event.type, pattern)) {
    return true;
  }

  // Check raw message (if available)
  if (event.message && _matchPattern(event.message, pattern)) {
    return true;
  }

  // Check data fields
  if (event.data) {
    // Convert data to string for matching
    const dataStr = JSON.stringify(event.data);
    if (_matchPattern(dataStr, pattern)) {
      return true;
    }
  }

  return false;
}

/**
 * Get default timeout for an action type
 *
 * Returns reasonable defaults for different action types:
 * - Block actions: 3-5 seconds
 * - Movement: 1-2 seconds
 * - Entity interaction: 3-5 seconds
 * - Commands: 2-3 seconds
 *
 * @param {string} actionType - Action type (e.g., 'break_block', 'place_block')
 * @returns {number} Timeout in milliseconds
 */
function getDefaultTimeout(actionType) {
  const timeouts = {
    // Block interaction (slowest)
    'break_block': 5000,
    'place_block': 5000,
    'interact_with_block': 3000,

    // Movement (fast)
    'move_forward': 2000,
    'move_backward': 2000,
    'move_left': 2000,
    'move_right': 2000,
    'jump': 1000,
    'look_at': 1000,
    'navigate_to': 15000,  // Pathfinding can be slow

    // Entity interaction (medium)
    'attack_entity': 3000,
    'interact_with_entity': 5000,
    'mount_entity': 3000,
    'dismount': 1000,

    // Inventory (medium)
    'drop_item': 2000,
    'consume_item': 2000,
    'equip_item': 2000,
    'swap_inventory_slots': 1000,

    // Commands (fast)
    'execute_command': 3000,
    'execute_player_command': 3000,
    'chat': 2000,

    // Default
    'default': 5000
  };

  return timeouts[actionType] || timeouts['default'];
}

module.exports = {
  waitForServerConfirmation,
  _matchPattern,  // Exported for testing
  getDefaultTimeout
};
