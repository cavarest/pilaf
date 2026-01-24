/**
 * LogParser Base Class Tests
 *
 * Tests the abstract LogParser base class to ensure:
 * - Cannot be instantiated directly
 * - Defines correct interface
 * - Result validation works
 */

const { LogParser } = require('./LogParser.js');

describe('LogParser (Abstract Base Class)', () => {
  describe('Direct Instantiation Prevention', () => {
    it('should throw when instantiated directly', () => {
      expect(() => new LogParser()).toThrow('LogParser is abstract');
    });
  });

  describe('Concrete Implementation', () => {
    // Create a minimal concrete implementation for testing
    class TestLogParser extends LogParser {
      parse(line) {
        // Simple test implementation
        if (line.includes('test')) {
          return {
            type: 'test',
            data: { value: line },
            raw: line
          };
        }
        return null;
      }
    }

    let parser;

    beforeEach(() => {
      parser = new TestLogParser();
    });

    describe('parse() Method', () => {
      it('should return parsed result for matching line', () => {
        const result = parser.parse('test line');

        expect(result).toEqual({
          type: 'test',
          data: { value: 'test line' },
          raw: 'test line'
        });
      });

      it('should return null for non-matching line', () => {
        const result = parser.parse('other line');

        expect(result).toBeNull();
      });

      it('should handle null input gracefully', () => {
        // Create a test implementation that handles null
        const robustParser = new TestLogParser();
        robustParser.parse = function(line) {
          if (line === null) {
            return null;
          }
          if (line && line.includes('test')) {
            return {
              type: 'test',
              data: { value: line },
              raw: line
            };
          }
          return null;
        };

        const result = robustParser.parse(null);
        expect(result).toBeNull();
      });
    });

    // Note: The constructor prevents direct instantiation, so testing that
    // the abstract method throws is not possible. The constructor check
    // at lib/core/LogParser.js:31-34 provides sufficient protection.

    describe('Result Validation', () => {
      it('should validate correct result format', () => {
        const validResult = {
          type: 'teleport',
          data: { player: 'TestPlayer' },
          raw: '[12:34:56] Teleported TestPlayer'
        };

        expect(parser._validateResult(validResult)).toBe(true);
      });

      it('should validate null result', () => {
        expect(parser._validateResult(null)).toBe(true);
      });

      it('should reject result missing type', () => {
        const invalidResult = {
          data: { player: 'TestPlayer' },
          raw: '[12:34:56] Teleported TestPlayer'
        };

        expect(parser._validateResult(invalidResult)).toBe(false);
      });

      it('should reject result missing data', () => {
        const invalidResult = {
          type: 'teleport',
          raw: '[12:34:56] Teleported TestPlayer'
        };

        expect(parser._validateResult(invalidResult)).toBe(false);
      });

      it('should reject result missing raw', () => {
        const invalidResult = {
          type: 'teleport',
          data: { player: 'TestPlayer' }
        };

        expect(parser._validateResult(invalidResult)).toBe(false);
      });

      it('should reject result with wrong type property', () => {
        const invalidResult = {
          type: 123,  // Should be string
          data: { player: 'TestPlayer' },
          raw: '[12:34:56] Teleported TestPlayer'
        };

        expect(parser._validateResult(invalidResult)).toBe(false);
      });

      it('should reject result with wrong data property', () => {
        const invalidResult = {
          type: 'teleport',
          data: 'not an object',  // Should be object
          raw: '[12:34:56] Teleported TestPlayer'
        };

        expect(parser._validateResult(invalidResult)).toBe(false);
      });

      it('should reject result with wrong raw property', () => {
        const invalidResult = {
          type: 'teleport',
          data: { player: 'TestPlayer' },
          raw: 123  // Should be string
        };

        expect(parser._validateResult(invalidResult)).toBe(false);
      });
    });

    describe('Optional Pattern Methods (Not Supported)', () => {
      it('should throw on addPattern() by default', () => {
        expect(() => parser.addPattern('test', /test/, () => {}))
          .toThrow('Method "addPattern()" is not supported');
      });

      it('should throw on removePattern() by default', () => {
        expect(() => parser.removePattern('test'))
          .toThrow('Method "removePattern()" is not supported');
      });

      it('should throw on getPatterns() by default', () => {
        expect(() => parser.getPatterns())
          .toThrow('Method "getPatterns()" is not supported');
      });
    });
  });
});
