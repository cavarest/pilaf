/**
 * CorrelationStrategy Base Class Tests
 *
 * Tests the abstract CorrelationStrategy base class to ensure:
 * - Cannot be instantiated directly
 * - Defines correct interface
 * - Helper methods work correctly
 * - Timeout handling works
 */

const { CorrelationStrategy } = require('./CorrelationStrategy.js');
const { ResponseTimeoutError } = require('../errors/index.js');

describe('CorrelationStrategy (Abstract Base Class)', () => {
  describe('Direct Instantiation Prevention', () => {
    it('should throw when instantiated directly', () => {
      expect(() => new CorrelationStrategy()).toThrow('CorrelationStrategy is abstract');
    });
  });

  describe('Concrete Implementation', () => {
    // Create a minimal concrete implementation for testing
    class TestCorrelationStrategy extends CorrelationStrategy {
      constructor(options = {}) {
        super(options);
        this.commandId = null;
      }

      async correlate(command, eventStream, timeout) {
        this.commandId = this._generateCommandId();
        // In a real implementation, would wait for matching event
        return { type: 'test', data: { command }, raw: command };
      }
    }

    let strategy;

    beforeEach(() => {
      strategy = new TestCorrelationStrategy();
    });

    describe('Configuration', () => {
      it('should use default timeout', () => {
        expect(strategy._defaultTimeout).toBe(5000);
      });

      it('should use default correlation window', () => {
        expect(strategy._correlationWindow).toBe(2000);
      });

      it('should accept custom timeout', () => {
        const customStrategy = new TestCorrelationStrategy({ timeout: 10000 });
        expect(customStrategy._defaultTimeout).toBe(10000);
      });

      it('should accept custom correlation window', () => {
        const customStrategy = new TestCorrelationStrategy({ window: 3000 });
        expect(customStrategy._correlationWindow).toBe(3000);
      });
    });

    describe('Command ID Generation', () => {
      it('should generate unique command IDs', () => {
        const id1 = strategy._generateCommandId();
        const id2 = strategy._generateCommandId();

        expect(id1).toMatch(/^pilaf-cmd-\d+-[a-z0-9]+$/);
        expect(id2).toMatch(/^pilaf-cmd-\d+-[a-z0-9]+$/);
        expect(id1).not.toBe(id2);
      });

      it('should generate IDs with correct format', () => {
        const id = strategy._generateCommandId();

        expect(id).toMatch(/^pilaf-cmd-/);  // Prefix
        expect(id).toMatch(/\d+-/);          // Timestamp
        expect(id).toMatch(/[a-z0-9]+$/);    // Random suffix
      });
    });

    describe('Marker Injection', () => {
      it('should inject marker into @s selector', () => {
        const command = '/tp @s ~ ~ ~';
        const marker = 'pilaf-cmd-123';

        const result = strategy._injectMarker(command, marker);

        expect(result).toContain(`[tag=${marker}]`);
      });

      it('should inject marker into @p selector', () => {
        const command = '/tp @p ~ ~ ~';
        const marker = 'pilaf-cmd-123';

        const result = strategy._injectMarker(command, marker);

        expect(result).toContain(`[tag=${marker}]`);
      });

      it('should inject marker into @a selector', () => {
        const command = '/tp @a ~ ~ ~';
        const marker = 'pilaf-cmd-123';

        const result = strategy._injectMarker(command, marker);

        expect(result).toContain(`[tag=${marker}]`);
      });

      it('should inject marker into @e selector', () => {
        const command = '/tp @e ~ ~ ~';
        const marker = 'pilaf-cmd-123';

        const result = strategy._injectMarker(command, marker);

        expect(result).toContain(`[tag=${marker}]`);
      });

      it('should return command unchanged if no selector', () => {
        const command = '/say hello';
        const marker = 'pilaf-cmd-123';

        const result = strategy._injectMarker(command, marker);

        expect(result).toBe(command);
      });

      it('should handle commands with multiple selectors', () => {
        const command = '/tp @s @s';
        const marker = 'pilaf-cmd-123';

        const result = strategy._injectMarker(command, marker);

        // Only first @s is replaced (simple implementation)
        expect(result).toContain(`@s[tag=${marker}]`);
      });
    });

    describe('Timeout Handling', () => {
      it('should reject after timeout', async () => {
        const timeout = 100;

        await expect(strategy._waitForTimeout(timeout))
          .rejects.toThrow(ResponseTimeoutError);
      });

      it('should reject with correct timeout value', async () => {
        const timeout = 250;

        try {
          await strategy._waitForTimeout(timeout);
          fail('Should have thrown');
        } catch (error) {
          expect(error).toBeInstanceOf(ResponseTimeoutError);
          expect(error.details.timeout).toBe(timeout);
        }
      });

      it('should reject with timeout error code', async () => {
        try {
          await strategy._waitForTimeout(100);
          fail('Should have thrown');
        } catch (error) {
          expect(error.code).toBe('CORRELATION_TIMEOUT');
        }
      });
    });

    describe('Event Matching', () => {
      it('should return true for matching event', () => {
        const event = {
          type: 'teleport',
          data: { player: 'TestPlayer', tag: 'pilaf-cmd-123' },
          raw: '[12:34:56] Teleported TestPlayer'
        };

        const result = strategy._matchesEvent(event, 'pilaf-cmd-123');

        expect(result).toBe(true);
      });

      it('should return false for non-matching event', () => {
        const event = {
          type: 'teleport',
          data: { player: 'TestPlayer' },
          raw: '[12:34:56] Teleported TestPlayer'
        };

        const result = strategy._matchesEvent(event, 'pilaf-cmd-123');

        expect(result).toBe(false);
      });

      it('should return false for null event', () => {
        const result = strategy._matchesEvent(null, 'pilaf-cmd-123');

        expect(result).toBe(false);
      });

      it('should return false for event without data', () => {
        const event = { type: 'teleport', raw: '[12:34:56]' };

        const result = strategy._matchesEvent(event, 'pilaf-cmd-123');

        expect(result).toBe(false);
      });

      it('should find marker in nested data', () => {
        const event = {
          type: 'teleport',
          data: {
            player: 'TestPlayer',
            metadata: {
              tags: ['pilaf-cmd-123', 'other-tag']
            }
          },
          raw: '[12:34:56]'
        };

        const result = strategy._matchesEvent(event, 'pilaf-cmd-123');

        expect(result).toBe(true);
      });
    });

    describe('Abstract Method', () => {
      it('should throw on correlate() if not implemented', async () => {
        // Create a stub that doesn't implement correlate()
        class IncompleteStrategy extends CorrelationStrategy {
          constructor() {
            super();
          }
          // Note: correlate() is not implemented, so it will throw from base class
        }

        const strategy = new IncompleteStrategy();

        await expect(strategy.correlate('/test', null))
          .rejects.toThrow('Method "correlate()" must be implemented');
      });
    });
  });

  describe('Example Implementation', () => {
    // Example showing how correlate() might be implemented
    class SimpleCorrelationStrategy extends CorrelationStrategy {
      constructor(options = {}) {
        super(options);
        this._pendingCommands = new Map();
      }

      async correlate(command, eventStream, timeout = this._defaultTimeout) {
        const commandId = this._generateCommandId();
        this._pendingCommands.set(commandId, { command, timestamp: Date.now() });

        // In a real implementation, would:
        // 1. Inject marker into command (if using tag-based correlation)
        // 2. Send command
        // 3. Wait for matching event in stream
        // 4. Return matching event or throw timeout

        // For testing, just return a mock response
        return new Promise((resolve, reject) => {
          const timer = setTimeout(() => {
            this._pendingCommands.delete(commandId);
            reject(new ResponseTimeoutError(command, timeout));
          }, timeout);

          // In real implementation, eventStream would trigger resolve
          // For now, resolve immediately
          clearTimeout(timer);
          resolve({
            type: 'response',
            data: { commandId, command },
            raw: `Response for: ${command}`
          });
        });
      }
    }

    it('should complete correlation successfully', async () => {
      const strategy = new SimpleCorrelationStrategy({ timeout: 5000 });

      const result = await strategy.correlate('/test', null);

      expect(result).toHaveProperty('type', 'response');
      expect(result.data).toHaveProperty('commandId');
      expect(result.data.command).toBe('/test');
    });

    it('should timeout after specified duration', async () => {
      const strategy = new SimpleCorrelationStrategy({ timeout: 100 });

      // Override correlate to actually timeout
      strategy.correlate = () => strategy._waitForTimeout(100);

      await expect(strategy.correlate('/test', null))
        .rejects.toThrow(ResponseTimeoutError);
    }, 200);
  });
});
