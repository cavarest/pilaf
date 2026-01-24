/**
 * CommandRouter Base Class Tests
 *
 * Tests the abstract CommandRouter base class to ensure:
 * - Cannot be instantiated directly
 * - Defines correct interface with channel constants
 * - Custom rule management works
 * - Pattern matching helper works
 */

const { CommandRouter } = require('./CommandRouter.js');

describe('CommandRouter (Abstract Base Class)', () => {
  describe('Channel Constants', () => {
    it('should have BOT channel constant', () => {
      expect(CommandRouter.CHANNELS.BOT).toBe('bot');
    });

    it('should have RCON channel constant', () => {
      expect(CommandRouter.CHANNELS.RCON).toBe('rcon');
    });

    it('should have LOG channel constant', () => {
      expect(CommandRouter.CHANNELS.LOG).toBe('log');
    });
  });

  describe('Direct Instantiation Prevention', () => {
    it('should throw when instantiated directly', () => {
      expect(() => new CommandRouter()).toThrow('CommandRouter is abstract');
    });
  });

  describe('Concrete Implementation', () => {
    // Create a minimal concrete implementation for testing
    class TestCommandRouter extends CommandRouter {
      route(command, context) {
        // Simple test implementation: default to bot
        return {
          channel: CommandRouter.CHANNELS.BOT,
          options: {}
        };
      }
    }

    let router;

    beforeEach(() => {
      router = new TestCommandRouter();
    });

    describe('Custom Rules Management', () => {
      it('should add a custom rule with string pattern', () => {
        router.addRule('/custom', CommandRouter.CHANNELS.RCON);

        const rules = router.getRules();
        expect(rules).toHaveLength(1);
        expect(rules[0].pattern).toBe('/custom');
        expect(rules[0].channel).toBe(CommandRouter.CHANNELS.RCON);
      });

      it('should add a custom rule with regex pattern', () => {
        const pattern = /^\/data get/;
        router.addRule(pattern, CommandRouter.CHANNELS.RCON);

        const rules = router.getRules();
        expect(rules).toHaveLength(1);
        expect(rules[0].pattern).toBe(pattern);
        expect(rules[0].channel).toBe(CommandRouter.CHANNELS.RCON);
      });

      it('should reject invalid channel', () => {
        expect(() => router.addRule('/test', 'invalid'))
          .toThrow('Invalid channel: invalid');
      });

      it('should remove a custom rule', () => {
        router.addRule('/test', CommandRouter.CHANNELS.RCON);
        expect(router.getRules()).toHaveLength(1);

        const result = router.removeRule('/test');

        expect(result).toBe(true);
        expect(router.getRules()).toHaveLength(0);
      });

      it('should return false when removing non-existent rule', () => {
        const result = router.removeRule('/nonexistent');

        expect(result).toBe(false);
      });

      it('should get copy of rules (not internal reference)', () => {
        router.addRule('/test', CommandRouter.CHANNELS.RCON);

        const rules1 = router.getRules();
        const rules2 = router.getRules();

        expect(rules1).not.toBe(rules2);
        expect(rules1).toEqual(rules2);
      });
    });

    describe('Pattern Matching Helper', () => {
      it('should match string prefix pattern', () => {
        const pattern = '/data get';

        expect(router._matchesPattern('/data get entity TestPlayer', pattern))
          .toBe(true);
        expect(router._matchesPattern('/teleport TestPlayer', pattern))
          .toBe(false);
      });

      it('should match regex pattern', () => {
        const pattern = /^\/data get entity (\w+)/;

        expect(router._matchesPattern('/data get entity TestPlayer Pos', pattern))
          .toBe(true);
        expect(router._matchesPattern('/data get block 100 64 100', pattern))
          .toBe(false);
      });

      it('should handle complex regex patterns', () => {
        const pattern = /teleport.*?\d{1,3}\s+\d{1,3}\s+\d{1,3}/;

        expect(router._matchesPattern('teleport TestPlayer 100 64 100', pattern))
          .toBe(true);
        expect(router._matchesPattern('teleport TestPlayer', pattern))
          .toBe(false);
      });
    });

    // Note: The constructor prevents direct instantiation, so testing that
    // the abstract method throws is not possible. The constructor check
    // at lib/core/CommandRouter.js:52-54 provides sufficient protection.
  });

  describe('Rule-Based Routing Example', () => {
    // Example implementation showing custom rules in action
    class RuleBasedRouter extends CommandRouter {
      route(command, context) {
        const { options } = context;

        // Check forced options first
        if (options?.useRcon) {
          return { channel: CommandRouter.CHANNELS.RCON, options };
        }
        if (options?.expectLogResponse) {
          return { channel: CommandRouter.CHANNELS.LOG, options };
        }

        // Check custom rules
        const rules = this.getRules();
        for (const { pattern, channel } of rules) {
          if (this._matchesPattern(command, pattern)) {
            return { channel, options };
          }
        }

        // Default: bot chat
        return { channel: CommandRouter.CHANNELS.BOT, options };
      }
    }

    let router;

    beforeEach(() => {
      router = new RuleBasedRouter();
    });

    it('should use custom rule for matching command', () => {
      router.addRule(/^\/data get/, CommandRouter.CHANNELS.RCON);

      const result = router.route('/data get entity TestPlayer Pos', {});

      expect(result.channel).toBe(CommandRouter.CHANNELS.RCON);
    });

    it('should prioritize useRcon option', () => {
      router.addRule(/^\/teleport/, CommandRouter.CHANNELS.LOG);

      const result = router.route('/teleport TestPlayer', { options: { useRcon: true } });

      expect(result.channel).toBe(CommandRouter.CHANNELS.RCON);
    });

    it('should use expectLogResponse option', () => {
      const result = router.route('/test', { options: { expectLogResponse: true } });

      expect(result.channel).toBe(CommandRouter.CHANNELS.LOG);
    });

    it('should default to BOT for unrecognized commands', () => {
      const result = router.route('/unknown command', {});

      expect(result.channel).toBe(CommandRouter.CHANNELS.BOT);
    });
  });
});
