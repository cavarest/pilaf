/**
 * Unit tests for CorrelationUtils
 */

const {
  waitForServerConfirmation,
  _matchPattern,
  getDefaultTimeout
} = require('@pilaf/framework/lib/helpers/correlation.js');

describe('CorrelationUtils', () => {
  describe('_matchPattern', () => {
    it('should match exact strings', () => {
      expect(_matchPattern('hello world', 'hello world')).toBe(true);
      expect(_matchPattern('hello world', 'hello')).toBe(false);
    });

    it('should match with * wildcard', () => {
      expect(_matchPattern('hello world', 'hello*')).toBe(true);
      expect(_matchPattern('hello world', '*world')).toBe(true);
      expect(_matchPattern('hello world', '*o w*')).toBe(true);
      expect(_matchPattern('hello world', '*')).toBe(true);
    });

    it('should match with ? wildcard', () => {
      expect(_matchPattern('hello', 'h?llo')).toBe(true);
      expect(_matchPattern('hello', 'h????')).toBe(true);
      expect(_matchPattern('hello', 'h?')).toBe(false);
    });

    it('should be case-insensitive', () => {
      expect(_matchPattern('Hello World', 'hello world')).toBe(true);
      expect(_matchPattern('HELLO WORLD', '*world*')).toBe(true);
    });

    it('should handle null/undefined inputs', () => {
      expect(_matchPattern(null, 'test')).toBe(false);
      expect(_matchPattern('test', null)).toBe(false);
      expect(_matchPattern('', '')).toBe(false);
    });

    it('should match complex patterns', () => {
      expect(_matchPattern('Cannot place block xyz', '*place*block*')).toBe(true);
      expect(_matchPattern('issued command /gamemode', '*issued*command*')).toBe(true);
      expect(_matchPattern('entity.join.TestPlayer', 'entity.*')).toBe(true);
      expect(_matchPattern('command.issued.gamemode', 'command.*')).toBe(true);
    });
  });

  describe('getDefaultTimeout', () => {
    it('should return correct timeout for block actions', () => {
      expect(getDefaultTimeout('break_block')).toBe(5000);
      expect(getDefaultTimeout('place_block')).toBe(5000);
      expect(getDefaultTimeout('interact_with_block')).toBe(3000);
    });

    it('should return correct timeout for movement actions', () => {
      expect(getDefaultTimeout('move_forward')).toBe(2000);
      expect(getDefaultTimeout('navigate_to')).toBe(15000);
      expect(getDefaultTimeout('jump')).toBe(1000);
    });

    it('should return correct timeout for entity actions', () => {
      expect(getDefaultTimeout('attack_entity')).toBe(3000);
      expect(getDefaultTimeout('mount_entity')).toBe(3000);
      expect(getDefaultTimeout('dismount')).toBe(1000);
    });

    it('should return correct timeout for inventory actions', () => {
      expect(getDefaultTimeout('drop_item')).toBe(2000);
      expect(getDefaultTimeout('equip_item')).toBe(2000);
    });

    it('should return default timeout for unknown actions', () => {
      expect(getDefaultTimeout('unknown_action')).toBe(5000);
      expect(getDefaultTimeout('')).toBe(5000);
    });
  });

  describe('waitForServerConfirmation', () => {
    let mockStoryRunner;

    beforeEach(() => {
      mockStoryRunner = {
        backends: {
          players: new Map(),
          rcon: null
        }
      };
    });

    it('should timeout when no EventObserver available', async () => {
      const startTime = Date.now();
      const result = await waitForServerConfirmation(mockStoryRunner, {
        pattern: '*test*',
        timeout: 100
      });
      const elapsed = Date.now() - startTime;

      expect(result).toBeNull();
      expect(elapsed).toBeGreaterThanOrEqual(100);
      expect(elapsed).toBeLessThan(200);  // Should be close to 100ms
    });

    it('should use default timeout when not specified', async () => {
      const startTime = Date.now();
      await waitForServerConfirmation(mockStoryRunner, {
        pattern: '*test*'
      });
      const elapsed = Date.now() - startTime;

      expect(elapsed).toBeGreaterThanOrEqual(5000);
      expect(elapsed).toBeLessThan(5200);
    });

    it('should handle null storyRunner', async () => {
      const result = await waitForServerConfirmation(null, {
        pattern: '*test*',
        timeout: 100
      });
      expect(result).toBeNull();
    });

    it('should handle empty options', async () => {
      const result = await waitForServerConfirmation(mockStoryRunner);
      expect(result).toBeNull();
    });

    it('should use EventObserver when available', async () => {
      // Create a mock EventObserver
      let eventCallback = null;
      let unsubscribe = () => {};

      const mockObserver = {
        isObserving: true,
        onEvent: (pattern, callback) => {
          eventCallback = callback;
          unsubscribe = () => { eventCallback = null; };
          return unsubscribe;
        }
      };

      // Add a mock backend with EventObserver
      const mockBackend = {
        getEventObserver: () => mockObserver
      };
      mockStoryRunner.backends.players.set('TestPlayer', mockBackend);

      // Start the wait (should not resolve immediately)
      const waitPromise = waitForServerConfirmation(mockStoryRunner, {
        pattern: '*test event*',
        timeout: 5000
      });

      // Simulate an event
      setTimeout(() => {
        if (eventCallback) {
          eventCallback({
            type: 'test.event',
            message: 'This is a test event',
            data: {}
          });
        }
      }, 100);

      // Should resolve with the event
      const result = await waitPromise;
      expect(result).toBeDefined();
      expect(result.type).toBe('test.event');
    });

    it('should filter by player name when specified', async () => {
      let capturedPattern = null;
      const mockObserver = {
        isObserving: true,
        onEvent: (pattern, callback) => {
          capturedPattern = pattern;
          return () => {};
        }
      };

      const mockBackend = {
        getEventObserver: () => mockObserver
      };
      mockStoryRunner.backends.players.set('TestPlayer', mockBackend);

      // Start wait with player filter
      const waitPromise = waitForServerConfirmation(mockStoryRunner, {
        pattern: '*test*',
        player: 'TestPlayer',
        timeout: 100
      });

      await waitPromise;
      expect(capturedPattern).toBe('*');
    });

    it('should timeout waiting for matching event', async () => {
      const mockObserver = {
        isObserving: true,
        onEvent: (pattern, callback) => () => {}
      };

      const mockBackend = {
        getEventObserver: () => mockObserver
      };
      mockStoryRunner.backends.players.set('TestPlayer', mockBackend);

      const startTime = Date.now();
      const result = await waitForServerConfirmation(mockStoryRunner, {
        pattern: '*never matches*',
        timeout: 200
      });
      const elapsed = Date.now() - startTime;

      expect(result).toBeNull();
      expect(elapsed).toBeGreaterThanOrEqual(200);
    });
  });

  describe('_checkEventMatch (via waitForServerConfirmation)', () => {
    it('should match event type', async () => {
      const mockObserver = {
        isObserving: true,
        onEvent: (pattern, callback) => {
          // Simulate immediate event
          setTimeout(() => {
            callback({
              type: 'block.placed',
              message: 'Placed block',
              data: { block: 'dirt' }
            });
          }, 10);
          return () => {};
        }
      };

      const mockStoryRunner = {
        backends: {
          players: new Map([
            ['TestPlayer', {
              getEventObserver: () => mockObserver
            }]
          ]),
          rcon: null
        }
      };

      const result = await waitForServerConfirmation(mockStoryRunner, {
        pattern: 'block.placed',
        timeout: 1000
      });

      expect(result).toBeDefined();
      expect(result.type).toBe('block.placed');
    });

    it('should match event message', async () => {
      const mockObserver = {
        isObserving: true,
        onEvent: (pattern, callback) => {
          setTimeout(() => {
            callback({
              type: 'unknown.event',
              message: 'Cannot place block xyz',
              data: {}
            });
          }, 10);
          return () => {};
        }
      };

      const mockStoryRunner = {
        backends: {
          players: new Map([
            ['TestPlayer', {
              getEventObserver: () => mockObserver
            }]
          ]),
          rcon: null
        }
      };

      const result = await waitForServerConfirmation(mockStoryRunner, {
        pattern: '*place*block*',
        timeout: 1000
      });

      expect(result).toBeDefined();
      expect(result.message).toContain('place');
      expect(result.message).toContain('block');
    });

    it('should match event data', async () => {
      const mockObserver = {
        isObserving: true,
        onEvent: (pattern, callback) => {
          setTimeout(() => {
            callback({
              type: 'custom.event',
              message: 'Event occurred',
              data: { player: 'TestPlayer', action: 'jumped' }
            });
          }, 10);
          return () => {};
        }
      };

      const mockStoryRunner = {
        backends: {
          players: new Map([
            ['TestPlayer', {
              getEventObserver: () => mockObserver
            }]
          ]),
          rcon: null
        }
      };

      const result = await waitForServerConfirmation(mockStoryRunner, {
        pattern: '*TestPlayer*',
        timeout: 1000
      });

      expect(result).toBeDefined();
      expect(result.data.player).toBe('TestPlayer');
    });
  });
});
