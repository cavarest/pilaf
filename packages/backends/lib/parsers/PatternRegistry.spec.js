/**
 * PatternRegistry Tests
 *
 * Tests the PatternRegistry class to ensure:
 * - Pattern registration works
 * - Pattern matching works correctly
 * - Priority ordering is respected
 * - Edge cases are handled
 */

const { PatternRegistry } = require('./PatternRegistry.js');

describe('PatternRegistry', () => {
  let registry;

  beforeEach(() => {
    registry = new PatternRegistry();
  });

  describe('Constructor', () => {
    it('should create empty registry', () => {
      expect(registry.size).toBe(0);
      expect(registry.getPatterns()).toEqual([]);
    });

    it('should accept case-insensitive option', () => {
      const caseInsensitiveRegistry = new PatternRegistry({ caseInsensitive: true });

      expect(caseInsensitiveRegistry._caseInsensitive).toBe(true);
    });

    it('should default to case-sensitive', () => {
      expect(registry._caseInsensitive).toBe(false);
    });
  });

  describe('Pattern Registration', () => {
    it('should add pattern with RegExp', () => {
      const pattern = /Teleported (\w+) to (.+)/;
      const handler = jest.fn(() => ({ player: 'test' }));

      registry.addPattern('teleport', pattern, handler);

      expect(registry.size).toBe(1);
      expect(registry.getPatterns()).toContain('teleport');
    });

    it('should add pattern with string', () => {
      const handler = jest.fn(() => ({ test: true }));

      registry.addPattern('test', 'UUID of player .* is (.+)', handler);

      expect(registry.size).toBe(1);
      expect(registry.getPattern('test')).toBeDefined();
    });

    it('should convert string to RegExp', () => {
      registry.addPattern('test', 'hello', () => ({}));

      const patternDef = registry.getPattern('test');
      expect(patternDef.pattern).toBeInstanceOf(RegExp);
    });

    it('should use case-insensitive flag when configured', () => {
      const ciRegistry = new PatternRegistry({ caseInsensitive: true });

      ciRegistry.addPattern('test', 'HELLO', () => ({}));

      const patternDef = ciRegistry.getPattern('test');
      expect(patternDef.pattern.flags).toContain('i');
    });

    it('should reject duplicate pattern name', () => {
      registry.addPattern('test', /test/, () => ({}));

      expect(() => {
        registry.addPattern('test', /test2/, () => ({}));
      }).toThrow('Pattern "test" already exists');
    });

    it('should reject invalid pattern name', () => {
      expect(() => {
        registry.addPattern('', /test/, () => ({}));
      }).toThrow('Pattern name must be a non-empty string');
    });

    it('should reject non-string pattern name', () => {
      expect(() => {
        registry.addPattern(null, /test/, () => ({}));
      }).toThrow('Pattern name must be a non-empty string');
    });

    it('should reject invalid pattern type', () => {
      expect(() => {
        registry.addPattern('test', 123, () => ({}));
      }).toThrow('Pattern must be a RegExp or string');
    });

    it('should reject non-function handler', () => {
      expect(() => {
        registry.addPattern('test', /test/, 'not a function');
      }).toThrow('Handler must be a function');
    });

    it('should add pattern with priority', () => {
      registry.addPattern('low', /low/, () => ({}), 10);
      registry.addPattern('high', /high/, () => ({}), 1);

      expect(registry.getPatterns()).toEqual(['high', 'low']);
    });

    it('should add pattern without priority (appends to end)', () => {
      registry.addPattern('first', /first/, () => ({}));
      registry.addPattern('second', /second/, () => ({}));

      expect(registry.getPatterns()).toEqual(['first', 'second']);
    });
  });

  describe('Pattern Removal', () => {
    it('should remove existing pattern', () => {
      registry.addPattern('test', /test/, () => ({}));

      const result = registry.removePattern('test');

      expect(result).toBe(true);
      expect(registry.size).toBe(0);
      expect(registry.getPatterns()).toEqual([]);
    });

    it('should return false when removing non-existent pattern', () => {
      const result = registry.removePattern('nonexistent');

      expect(result).toBe(false);
    });

    it('should remove pattern from order list', () => {
      registry.addPattern('first', /first/, () => ({}));
      registry.addPattern('second', /second/, () => ({}));

      registry.removePattern('first');

      expect(registry.getPatterns()).toEqual(['second']);
    });
  });

  describe('Pattern Retrieval', () => {
    it('should get pattern by name', () => {
      const pattern = /test/;
      const handler = () => ({ matched: true });

      registry.addPattern('test', pattern, handler);

      const result = registry.getPattern('test');

      expect(result).toEqual({
        name: 'test',
        pattern,
        handler,
        priority: 0
      });
    });

    it('should return null for non-existent pattern', () => {
      const result = registry.getPattern('nonexistent');

      expect(result).toBeNull();
    });

    it('should get all pattern names', () => {
      registry.addPattern('first', /first/, () => ({}));
      registry.addPattern('second', /second/, () => ({}));
      registry.addPattern('third', /third/, () => ({}));

      const names = registry.getPatterns();

      expect(names).toEqual(['first', 'second', 'third']);
    });
  });

  describe('Pattern Matching', () => {
    beforeEach(() => {
      // Add test patterns
      registry.addPattern(
        'teleport',
        /Teleported (\w+) to (.+)/,
        (match) => ({
          player: match[1],
          destination: match[2]
        })
      );

      registry.addPattern(
        'death',
        /(\w+) was slain by (\w+)/,
        (match) => ({
          victim: match[1],
          killer: match[2]
        })
      );
    });

    it('should match first pattern in order', () => {
      const result = registry.match('[12:34:56] Teleported TestPlayer to 100.0, 64.0, 100.0');

      expect(result).not.toBeNull();
      expect(result.name).toBe('teleport');
      expect(result.data).toEqual({
        player: 'TestPlayer',
        destination: '100.0, 64.0, 100.0'
      });
    });

    it('should match second pattern', () => {
      const result = registry.match('[12:34:56] Steve was slain by Zombie');

      expect(result).not.toBeNull();
      expect(result.name).toBe('death');
      expect(result.data).toEqual({
        victim: 'Steve',
        killer: 'Zombie'
      });
    });

    it('should return null when no patterns match', () => {
      const result = registry.match('[12:34:56] Unknown log message');

      expect(result).toBeNull();
    });

    it('should return null for non-string input', () => {
      expect(registry.match(null)).toBeNull();
      expect(registry.match(undefined)).toBeNull();
      expect(registry.match(123)).toBeNull();
    });

    it('should include raw input in result', () => {
      const line = '[12:34:56] Teleported TestPlayer to 100.0, 64.0, 100.0';
      const result = registry.match(line);

      expect(result.raw).toBe(line);
    });

    it('should include regex match object in result', () => {
      const result = registry.match('[12:34:56] Teleported TestPlayer to somewhere');

      expect(result.match).toBeInstanceOf(Array);
      expect(result.match[0]).toContain('Teleported TestPlayer to somewhere');
      expect(result.match[1]).toBe('TestPlayer');
      expect(result.match[2]).toBe('somewhere');
    });

    it('should handle handler errors gracefully', () => {
      registry.addPattern(
        'error',
        /Error: (.+)/,
        () => {
          throw new Error('Handler failed');
        }
      );

      // Should not throw, should continue to next pattern
      const result = registry.match('[12:34:56] Error: Something went wrong');

      expect(result).toBeNull();
    });
  });

  describe('Pattern-Specific Matching', () => {
    beforeEach(() => {
      registry.addPattern('test', /test (\w+)/, (match) => ({ word: match[1] }));
    });

    it('should match specific pattern', () => {
      const result = registry.matchPattern('test hello', 'test');

      expect(result).not.toBeNull();
      expect(result.name).toBe('test');
      expect(result.data.word).toBe('hello');
    });

    it('should return null when pattern does not match', () => {
      const result = registry.matchPattern('other message', 'test');

      expect(result).toBeNull();
    });

    it('should return null for non-existent pattern', () => {
      const result = registry.matchPattern('test hello', 'nonexistent');

      expect(result).toBeNull();
    });

    it('should return null when handler throws', () => {
      registry.addPattern('throws', /test/, () => {
        throw new Error('Handler error');
      });

      const result = registry.matchPattern('test', 'throws');

      expect(result).toBeNull();
    });
  });

  describe('Pattern Testing', () => {
    beforeEach(() => {
      registry.addPattern('test', /test/i, () => ({}));
    });

    it('should return true when pattern matches', () => {
      const result = registry.test('TEST', 'test');

      expect(result).toBe(true);
    });

    it('should return false when pattern does not match', () => {
      const result = registry.test('other', 'test');

      expect(result).toBe(false);
    });

    it('should return false for non-existent pattern', () => {
      const result = registry.test('test', 'nonexistent');

      expect(result).toBe(false);
    });
  });

  describe('Priority Ordering', () => {
    it('should test patterns in priority order', () => {
      // Add patterns in reverse priority order
      registry.addPattern('specific', /Specific: (.+)/, (m) => ({ type: 'specific', value: m[1] }), 10);
      registry.addPattern('general', /General: (.+)/, (m) => ({ type: 'general', value: m[1] }), 1);
      registry.addPattern('fallback', /.+/, (m) => ({ type: 'fallback' }), 100);

      // This matches both 'general' and 'fallback', but 'general' has higher priority (lower number)
      const result = registry.match('General: test');

      expect(result.name).toBe('general');
      expect(result.data.type).toBe('general');
    });

    it('should respect insertion order when priorities are equal', () => {
      registry.addPattern('first', /First/, () => ({ order: 1 }), 5);
      registry.addPattern('second', /Second/, () => ({ order: 2 }), 5);

      // Both have same priority, should use insertion order
      expect(registry.getPatterns()).toEqual(['first', 'second']);
    });
  });

  describe('Clear Operations', () => {
    it('should clear all patterns', () => {
      registry.addPattern('first', /first/, () => ({}));
      registry.addPattern('second', /second/, () => ({}));

      registry.clear();

      expect(registry.size).toBe(0);
      expect(registry.getPatterns()).toEqual([]);
    });

    it('should reset compiled pattern cache', () => {
      registry.addPattern('test', /test/, () => ({}));

      // Compile pattern by accessing it
      registry._getCompiledPattern('test', /test/);

      expect(registry._compiled.size).toBe(1);

      registry.clear();

      expect(registry._compiled.size).toBe(0);
    });
  });

  describe('Cloning', () => {
    it('should create independent copy', () => {
      registry.addPattern('test', /test (\w+)/, (m) => ({ word: m[1] }));

      const cloned = registry.clone();

      expect(cloned).not.toBe(registry);
      expect(cloned.size).toBe(registry.size);
      expect(cloned.getPatterns()).toEqual(registry.getPatterns());
    });

    it('should have independent pattern sets', () => {
      // Add a pattern before cloning
      registry.addPattern('test', /test/, () => ({}));

      const cloned = registry.clone();

      registry.addPattern('original', /original/, () => ({}));
      cloned.addPattern('cloned', /cloned/, () => ({}));

      // Original should have 'test' + 'original' = 2 patterns
      expect(registry.size).toBe(2);
      // Clone should have 'test' + 'cloned' = 2 patterns
      expect(cloned.size).toBe(2);
      expect(registry.getPatterns()).toContain('test');
      expect(registry.getPatterns()).toContain('original');
      expect(cloned.getPatterns()).toContain('test');
      expect(cloned.getPatterns()).toContain('cloned');
    });
  });

  describe('Serialization', () => {
    it('should export to JSON', () => {
      registry.addPattern('test', /test/, () => ({}), 5);

      const json = registry.toJSON();

      expect(json.caseInsensitive).toBe(false);
      expect(json.patternCount).toBe(1);
      expect(json.patterns).toHaveProperty('test');
    });

    it('should include pattern string in JSON', () => {
      registry.addPattern('test', /test/i, () => ({}));

      const json = registry.toJSON();

      expect(json.patterns.test.pattern).toContain('/test');
      expect(json.patterns.test.pattern).toContain('i');
    });

    it('should not include handlers in JSON', () => {
      registry.addPattern('test', /test/, () => ({ sensitive: 'data' }));

      const json = registry.toJSON();

      expect(json.patterns.test).not.toHaveProperty('handler');
    });
  });

  describe('Named Capture Groups', () => {
    it('should extract named capture groups in handler', () => {
      registry.addPattern(
        'named',
        /Teleported (?<player>\w+) to (?<position>.+)/,
        (match) => ({
          player: match.groups?.player,
          position: match.groups?.position
        })
      );

      const result = registry.match('Teleported TestPlayer to 100.0, 64.0, 100.0');

      expect(result.data.player).toBe('TestPlayer');
      expect(result.data.position).toBe('100.0, 64.0, 100.0');
    });

    it('should handle missing groups object', () => {
      registry.addPattern(
        'unnamed',
        /Teleported (\w+) to (.+)/,
        (match) => ({
          all: match.slice(1)
        })
      );

      const result = registry.match('Teleported TestPlayer to 100.0, 64.0, 100.0');

      expect(result.data.all).toEqual(['TestPlayer', '100.0, 64.0, 100.0']);
    });
  });

  describe('Complex Patterns', () => {
    it('should handle multiline patterns', () => {
      registry.addPattern(
        'multiline',
        /Line 1\nLine 2/,
        () => ({ multiline: true })
      );

      const result = registry.match('Line 1\nLine 2');

      expect(result).not.toBeNull();
      expect(result.data.multiline).toBe(true);
    });

    it('should handle patterns with special characters', () => {
      registry.addPattern(
        'special',
        /\[\d{2}:\d{2}:\d{2}\]/,
        () => ({ timestamp: true })
      );

      const result = registry.match('[12:34:56] message');

      expect(result).not.toBeNull();
      expect(result.data.timestamp).toBe(true);
    });

    it('should handle greedy vs non-greedy matching', () => {
      registry.addPattern(
        'greedy',
        /a.+b/,
        () => ({ greedy: true })
      );

      const result = registry.match('a XXX b XXX b');

      expect(result.data.greedy).toBe(true);
    });
  });
});
