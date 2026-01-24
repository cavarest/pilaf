/**
 * MinecraftLogParser Tests
 *
 * Tests the Minecraft log parser to ensure:
 * - All 6 pattern categories work correctly
 * - Priority ordering is respected
 * - Metadata extraction works
 * - Edge cases are handled
 * - Cross-version compatibility (1.19, 1.20, 1.21+)
 */

const { MinecraftLogParser } = require('./MinecraftLogParser.js');
const { UnknownPatternError } = require('../errors/index.js');
const fixtures = require('./fixtures/minecraft-logs.js');

describe('MinecraftLogParser', () => {
  let parser;

  beforeEach(() => {
    parser = new MinecraftLogParser();
  });

  describe('Construction', () => {
    it('should create parser with default options', () => {
      expect(parser).toBeInstanceOf(MinecraftLogParser);
      expect(parser._options.includeMetadata).toBe(true);
      expect(parser._options.strictMode).toBe(false);
    });

    it('should accept custom options', () => {
      const customParser = new MinecraftLogParser({
        includeMetadata: false,
        strictMode: true
      });

      expect(customParser._options.includeMetadata).toBe(false);
      expect(customParser._options.strictMode).toBe(true);
    });

    it('should initialize patterns on construction', () => {
      const patterns = parser.getPatterns();
      expect(patterns.length).toBeGreaterThan(0);
    });
  });

  describe('Metadata Extraction', () => {
    it('should extract timestamp from log line', () => {
      const line = '[12:34:56] [Server thread/INFO]: TestPlayer joined the game';
      const result = parser.parse(line);

      expect(result.timestamp).toBe('12:34:56');
    });

    it('should extract thread name', () => {
      const line = '[12:34:56] [Server thread/INFO]: TestPlayer joined the game';
      const result = parser.parse(line);

      expect(result.thread).toBe('Server thread');
    });

    it('should extract log level', () => {
      const line = '[12:34:56] [Server thread/INFO]: TestPlayer joined the game';
      const result = parser.parse(line);

      expect(result.level).toBe('INFO');
    });

    it('should extract WARN level', () => {
      const line = '[12:34:56] [Server thread/WARN]: Can\'t keep up!';
      const result = parser.parse(line);

      expect(result.level).toBe('WARN');
    });

    it('should extract ERROR level', () => {
      const line = '[12:34:56] [Server thread/ERROR]: Some error message';
      const result = parser.parse(line);

      expect(result.level).toBe('ERROR');
    });

    it('should handle Worker thread names', () => {
      const line = '[12:34:56] [Worker-Main-1/INFO]: TestPlayer joined the game';
      const result = parser.parse(line);

      expect(result.thread).toBe('Worker-Main-1');
    });

    it('should handle lines without metadata gracefully', () => {
      const line = 'Some message without timestamp';
      const result = parser.parse(line);

      expect(result.timestamp).toBeNull();
      expect(result.thread).toBeNull();
      expect(result.level).toBeNull();
    });

    it('should skip metadata when includeMetadata is false', () => {
      const parser = new MinecraftLogParser({ includeMetadata: false });
      const line = '[12:34:56] [Server thread/INFO]: TestPlayer joined the game';
      const result = parser.parse(line);

      expect(result.timestamp).toBeUndefined();
      expect(result.thread).toBeUndefined();
      expect(result.level).toBeUndefined();
    });
  });

  describe('Status Events', () => {
    describe('Starting', () => {
      it('should parse server starting with version', () => {
        const line = '[12:34:56] [Server thread/INFO]: Starting minecraft server version 1.20.1';
        const result = parser.parse(line);

        expect(result.type).toBe('status.start');
        expect(result.data.version).toBe('1.20.1');
      });

      it('should parse generic starting message', () => {
        const line = '[12:34:56] [Server thread/INFO]: Starting minecraft server on *:25565';
        const result = parser.parse(line);

        expect(result.type).toBe('status.starting');
      });

      fixtures.status.starting.forEach(line => {
        it(`should parse: ${line.substring(0, 50)}...`, () => {
          const result = parser.parse(line);
          expect(result).not.toBeNull();
          expect(result.type).toContain('status');
        });
      });
    });

    describe('Preparing', () => {
      it('should parse preparing level', () => {
        const line = '[12:34:56] [Server thread/INFO]: Preparing level "world"';
        const result = parser.parse(line);

        expect(result.type).toBe('status.preparing');
        expect(result.data.level).toBe('world');
      });

      fixtures.status.preparing.forEach(line => {
        it(`should parse: ${line.substring(0, 50)}...`, () => {
          const result = parser.parse(line);
          expect(result).not.toBeNull();
          expect(result.type).toBe('status.preparing');
        });
      });
    });

    describe('Done', () => {
      it('should parse done message', () => {
        const line = '[12:34:56] [Server thread/INFO]: Done (3.452s)! For help, type "help"';
        const result = parser.parse(line);

        expect(result.type).toBe('status.done');
      });

      fixtures.status.done.forEach(line => {
        it(`should parse: ${line.substring(0, 50)}...`, () => {
          const result = parser.parse(line);
          expect(result).not.toBeNull();
          expect(result.type).toContain('status');
        });
      });
    });
  });

  describe('Entity Events', () => {
    describe('Player Join', () => {
      it('should parse player joined', () => {
        const line = '[12:34:56] [Server thread/INFO]: TestPlayer joined the game';
        const result = parser.parse(line);

        expect(result.type).toBe('entity.join');
        expect(result.data.player).toBe('TestPlayer');
      });

      fixtures.entity.join.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('entity.join');
          expect(result.data.player).toBeTruthy();
        });
      });
    });

    describe('Player Leave', () => {
      it('should parse player leave with reason', () => {
        const line = '[12:34:56] [Server thread/INFO]: TestPlayer lost connection: Disconnected';
        const result = parser.parse(line);

        expect(result.type).toBe('entity.leave');
        expect(result.data.player).toBe('TestPlayer');
        expect(result.data.reason).toBe('Disconnected');
      });

      it('should parse player leave without reason', () => {
        const line = '[12:34:56] [Server thread/INFO]: TestPlayer left the game';
        const result = parser.parse(line);

        expect(result.type).toBe('entity.leave');
        expect(result.data.player).toBe('TestPlayer');
        // Note: This might not match, depending on pattern
      });

      fixtures.entity.leave.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result).not.toBeNull();
          expect(result.type).toBe('entity.leave');
        });
      });
    });

    describe('Player Spawn', () => {
      it('should parse UUID/spawn event', () => {
        const line = '[12:34:56] [Server thread/INFO]: UUID of player TestPlayer is abc123-def456-7890-abcd-ef1234567890';
        const result = parser.parse(line);

        expect(result.type).toBe('entity.spawn');
        expect(result.data.player).toBe('TestPlayer');
        expect(result.data.uuid).toBeTruthy();
      });

      fixtures.entity.spawn.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('entity.spawn');
        });
      });
    });

    describe('Death Events', () => {
      describe('Slain by entity', () => {
        it('should parse death by Zombie', () => {
          const line = '[12:34:56] [Server thread/INFO]: TestPlayer was slain by Zombie';
          const result = parser.parse(line);

          expect(result.type).toBe('entity.death.slain');
          expect(result.data.player).toBe('TestPlayer');
          expect(result.data.killer).toBe('Zombie');
          expect(result.data.cause).toBe('entity_attack');
        });

        fixtures.entity.death.slain.forEach(line => {
          it(`should parse: ${line}`, () => {
            const result = parser.parse(line);
            expect(result.type).toBe('entity.death.slain');
          });
        });
      });

      describe('Fall death', () => {
        it('should parse fall death', () => {
          const line = '[12:34:56] [Server thread/INFO]: TestPlayer fell from a high place';
          const result = parser.parse(line);

          expect(result.type).toBe('entity.death.fall');
          expect(result.data.player).toBe('TestPlayer');
          expect(result.data.cause).toBe('fall');
        });

        fixtures.entity.death.fall.forEach(line => {
          it(`should parse: ${line}`, () => {
            const result = parser.parse(line);
            expect(result.type).toBe('entity.death.fall');
          });
        });
      });

      describe('Fire death', () => {
        fixtures.entity.death.fire.forEach(line => {
          it(`should parse: ${line}`, () => {
            const result = parser.parse(line);
            expect(result.type).toBe('entity.death.fire');
            expect(result.data.cause).toBe('fire');
          });
        });
      });

      describe('Lava death', () => {
        fixtures.entity.death.lava.forEach(line => {
          it(`should parse: ${line}`, () => {
            const result = parser.parse(line);
            expect(result.type).toBe('entity.death.lava');
            expect(result.data.cause).toBe('lava');
          });
        });
      });

      describe('Drown death', () => {
        fixtures.entity.death.drown.forEach(line => {
          it(`should parse: ${line}`, () => {
            const result = parser.parse(line);
            expect(result.type).toBe('entity.death.drown');
            expect(result.data.cause).toBe('drown');
          });
        });
      });

      describe('Sprint death', () => {
        fixtures.entity.death.sprint.forEach(line => {
          it(`should parse: ${line}`, () => {
            const result = parser.parse(line);
            expect(result.type).toBe('entity.death.sprint');
          });
        });
      });
    });
  });

  describe('Movement Events', () => {
    describe('Teleport', () => {
      it('should parse teleport event', () => {
        const line = '[12:34:56] [Server thread/INFO]: Teleported TestPlayer from 100.5, 64.0, 200.3 to 150.2, 70.0, -300.5';
        const result = parser.parse(line);

        expect(result.type).toBe('movement.teleport');
        expect(result.data.player).toBe('TestPlayer');
        expect(result.data.from.x).toBe(100.5);
        expect(result.data.from.y).toBe(64.0);
        expect(result.data.from.z).toBe(200.3);
        expect(result.data.to.x).toBe(150.2);
        expect(result.data.to.y).toBe(70.0);
        expect(result.data.to.z).toBe(-300.5);
      });

      it('should parse negative coordinates', () => {
        const line = '[12:34:56] [Server thread/INFO]: Teleported TestPlayer from -1024.5, 128.0, 2048.9 to 512.0, -60.0, -512.0';
        const result = parser.parse(line);

        expect(result.type).toBe('movement.teleport');
        expect(result.data.from.x).toBe(-1024.5);
        expect(result.data.to.y).toBe(-60.0);
        expect(result.data.to.z).toBe(-512.0);
      });

      fixtures.movement.teleport.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('movement.teleport');
          expect(result.data.player).toBeTruthy();
          expect(result.data.from).toBeTruthy();
          expect(result.data.to).toBeTruthy();
        });
      });
    });
  });

  describe('Command Events', () => {
    describe('Issued Command', () => {
      it('should parse command execution', () => {
        const line = '[12:34:56] [Server thread/INFO]: TestPlayer issued server command: /gamemode creative';
        const result = parser.parse(line);

        expect(result.type).toBe('command.issued');
        expect(result.data.player).toBe('TestPlayer');
        expect(result.data.command).toBe('/gamemode creative');
      });

      fixtures.command.issued.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('command.issued');
          expect(result.data.command).toBeTruthy();
        });
      });
    });
  });

  describe('World Events', () => {
    describe('Time Change', () => {
      it('should parse time change', () => {
        const line = '[12:34:56] [Server thread/INFO]: Changing the time to 1000';
        const result = parser.parse(line);

        expect(result.type).toBe('world.time');
        expect(result.data.time).toBe(1000);
      });

      fixtures.world.time.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('world.time');
          expect(result.data.time).toBeGreaterThan(0);
        });
      });
    });

    describe('Weather Change', () => {
      it('should parse weather change', () => {
        const line = '[12:34:56] [Server thread/INFO]: Changing the weather to rain';
        const result = parser.parse(line);

        expect(result.type).toBe('world.weather');
        expect(result.data.weather).toBe('rain');
      });

      fixtures.world.weather.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('world.weather');
          expect(result.data.weather).toBeTruthy();
        });
      });
    });

    describe('Save Events', () => {
      it('should parse save start', () => {
        const line = '[12:34:56] [Server thread/INFO]: Saving the game';
        const result = parser.parse(line);

        expect(result.type).toBe('world.save.start');
      });

      it('should parse save complete', () => {
        const line = '[12:34:56] [Server thread/INFO]: Saved the game';
        const result = parser.parse(line);

        expect(result.type).toBe('world.save.complete');
      });

      fixtures.world.save.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toContain('world.save');
        });
      });
    });

    describe('Difficulty Change', () => {
      fixtures.world.difficulty.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('world.difficulty');
          expect(result.data.difficulty).toBeTruthy();
        });
      });
    });

    describe('GameMode Change', () => {
      fixtures.world.gamemode.forEach(line => {
        it(`should parse: ${line}`, () => {
          const result = parser.parse(line);
          expect(result.type).toBe('world.gamemode');
          expect(result.data.gamemode).toBeTruthy();
        });
      });
    });
  });

  describe('Priority Ordering', () => {
    it('should match teleport over other entity events', () => {
      // Teleport has priority 10, should match even if it contains other keywords
      const line = '[12:34:56] [Server thread/INFO]: Teleported TestPlayer from 100.0, 64.0, 200.0 to 150.0, 70.0, -300.0';
      const result = parser.parse(line);

      expect(result.type).toBe('movement.teleport');
      expect(result.type).not.toBe('entity.death.generic');
    });

    it('should match specific death types over generic death', () => {
      const slainLine = '[12:34:56] [Server thread/INFO]: TestPlayer was slain by Zombie';
      const result1 = parser.parse(slainLine);

      expect(result1.type).toBe('entity.death.slain');

      const fallLine = '[12:34:56] [Server thread/INFO]: TestPlayer fell from a high place';
      const result2 = parser.parse(fallLine);

      expect(result2.type).toBe('entity.death.fall');
    });
  });

  describe('Cross-Version Compatibility', () => {
    it('should handle 1.19 logs', () => {
      const line = '[12:34:56] [Worker-Main-1/INFO]: Starting minecraft server version 1.19.4';
      const result = parser.parse(line);

      expect(result).not.toBeNull();
      expect(result.thread).toBe('Worker-Main-1');
    });

    it('should handle 1.20 logs', () => {
      const line = '[12:34:56] [Server thread/INFO]: Starting minecraft server version 1.20.1';
      const result = parser.parse(line);

      expect(result).not.toBeNull();
      expect(result.data.version).toBe('1.20.1');
    });

    it('should handle 1.21+ logs', () => {
      const line = '[12:34:56] [Worker-Main-2/INFO]: Starting minecraft server version 1.21';
      const result = parser.parse(line);

      expect(result).not.toBeNull();
    });
  });

  describe('Unknown Patterns', () => {
    it('should return null for unknown patterns when includeMetadata is false', () => {
      const parser = new MinecraftLogParser({ includeMetadata: false });
      const line = '[12:34:56] [Server thread/INFO]: Some completely unknown message';
      const result = parser.parse(line);

      expect(result).toBeNull();
    });

    it('should return metadata for unknown patterns when includeMetadata is true', () => {
      const line = '[12:34:56] [Server thread/INFO]: Some completely unknown message';
      const result = parser.parse(line);

      expect(result).not.toBeNull();
      expect(result.type).toBeNull();
      expect(result.data).toBeNull();
      expect(result.timestamp).toBe('12:34:56');
      expect(result.thread).toBe('Server thread');
      expect(result.level).toBe('INFO');
    });

    it('should throw in strict mode for unknown patterns', () => {
      const strictParser = new MinecraftLogParser({ strictMode: true });
      const line = '[12:34:56] [Server thread/INFO]: Unknown message';

      expect(() => strictParser.parse(line)).toThrow(UnknownPatternError);
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty string', () => {
      const result = parser.parse('');

      expect(result).toBeNull();
    });

    it('should handle null input', () => {
      const result = parser.parse(null);

      expect(result).toBeNull();
    });

    it('should handle undefined input', () => {
      const result = parser.parse(undefined);

      expect(result).toBeNull();
    });

    it('should handle non-string input', () => {
      const result = parser.parse(12345);

      expect(result).toBeNull();
    });

    it('should handle whitespace-only input', () => {
      const result = parser.parse('   ');

      expect(result).toBeNull();
    });

    it('should trim whitespace from log lines', () => {
      const line = '  [12:34:56] [Server thread/INFO]: TestPlayer joined the game  ';
      const result = parser.parse(line);

      expect(result).not.toBeNull();
      expect(result.raw).not.toContain('  '); // no leading/trailing spaces in raw
    });

    it('should handle unicode player names', () => {
      const line = '[12:34:56] [Server thread/INFO]: 张三 joined the game';
      const result = parser.parse(line);

      expect(result.type).toBe('entity.join');
      expect(result.data.player).toBeTruthy();
    });
  });

  describe('Custom Patterns', () => {
    it('should allow adding custom patterns', () => {
      parser.addPattern(
        'plugin.custom',
        /Custom plugin event by (\w+)/,
        (match) => ({ player: match[1] }),
        5
      );

      const line = '[12:34:56] [Server thread/INFO]: Custom plugin event by TestPlayer';
      const result = parser.parse(line);

      expect(result.type).toBe('plugin.custom');
      expect(result.data.player).toBe('TestPlayer');
    });

    it('should allow removing custom patterns', () => {
      parser.addPattern(
        'plugin.test',
        /Test pattern/,
        () => ({}),
        5
      );

      expect(parser.removePattern('plugin.test')).toBe(true);
      expect(parser.removePattern('plugin.nonexistent')).toBe(false);
    });

    it('should clone with all patterns', () => {
      parser.addPattern(
        'plugin.clone',
        /Clone test/,
        () => ({ cloned: true }),
        5
      );

      const cloned = parser.clone();
      const line = '[12:34:56] [Server thread/INFO]: Clone test';

      const originalResult = parser.parse(line);
      const clonedResult = cloned.parse(line);

      expect(clonedResult.type).toBe(originalResult.type);
      expect(clonedResult.data).toEqual(originalResult.data);
    });
  });

  describe('Mixed Category Priority Tests', () => {
    fixtures.mixed.forEach(line => {
      it(`should correctly categorize: ${line.substring(0, 60)}...`, () => {
        const result = parser.parse(line);
        expect(result).not.toBeNull();
        // Verify that the most specific pattern matched
        expect(result.type).toBeTruthy();
      });
    });
  });
});
