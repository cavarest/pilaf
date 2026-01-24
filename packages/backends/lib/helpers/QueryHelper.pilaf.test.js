/**
 * QueryHelper Tests
 *
 * Tests the QueryHelper class for structured RCON queries.
 */

const { QueryHelper } = require('./QueryHelper.js');

// Mock RconBackend for testing
class MockRconBackend {
  constructor(responses = {}) {
    this._responses = responses;
    this._commands = [];
  }

  async sendCommand(cmd) {
    this._commands.push(cmd);
    if (this._responses[cmd]) {
      return this._responses[cmd];
    }
    if (this._responses['*']) {
      return this._responses['*'];
    }
    return { raw: '', parsed: null };
  }

  getCommands() {
    return this._commands;
  }

  reset() {
    this._commands = [];
  }
}

describe('QueryHelper', () => {
  describe('Construction', () => {
    it('should create with valid RconBackend', () => {
      const mockRcon = new MockRconBackend();
      const helper = new QueryHelper(mockRcon);
      expect(helper._rcon).toBe(mockRcon);
    });

    it('should throw without RconBackend', () => {
      expect(() => new QueryHelper()).toThrow('RconBackend is required');
      expect(() => new QueryHelper(null)).toThrow('RconBackend is required');
      expect(() => new QueryHelper(undefined)).toThrow('RconBackend is required');
    });
  });

  describe('getPlayerInfo', () => {
    it('should parse NBT data when available', async () => {
      const mockRcon = new MockRconBackend({
        '*': {
          raw: 'Steve has the following entity data: {...}',
          parsed: {
            Pos: [100.5, 64.0, -200.3],
            Health: 20,
            FoodLevel: 18,
            XpTotal: 150,
            XpLevel: 5
          }
        }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getPlayerInfo('Steve');

      expect(result.username).toBe('Steve');
      expect(result.position).toEqual({ x: 100.5, y: 64.0, z: -200.3 });
      expect(result.health).toBe(20);
      expect(result.food).toBe(18);
      expect(result.exp).toBe(150);
      expect(result.level).toBe(5);
    });

    it('should return minimal info when no NBT data', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'No data available', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getPlayerInfo('Steve');

      expect(result.username).toBe('Steve');
      expect(result.position).toBeNull();
      expect(result.health).toBeNull();
      expect(result.food).toBeNull();
      expect(result.exp).toBeNull();
      expect(result.level).toBeNull();
      expect(result.raw).toBe('No data available');
    });

    it('should handle partial NBT data', async () => {
      const mockRcon = new MockRconBackend({
        '*': {
          raw: 'Partial data',
          parsed: {
            Health: 15,
            FoodLevel: 10
          }
        }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getPlayerInfo('Alex');

      expect(result.username).toBe('Alex');
      expect(result.position).toBeNull();
      expect(result.health).toBe(15);
      expect(result.food).toBe(10);
    });

    it('should send correct command', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: '', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      await helper.getPlayerInfo('TestPlayer');

      expect(mockRcon.getCommands()).toHaveLength(1);
      expect(mockRcon.getCommands()[0]).toBe('data get entity TestPlayer');
    });
  });

  describe('listPlayers', () => {
    it('should parse full response format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'There are 3 players online: Steve, Alex, Notch', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.online).toBe(3);
      expect(result.players).toEqual(['Steve', 'Alex', 'Notch']);
    });

    it('should parse simple player list', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Steve, Alex', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.online).toBe(2);
      expect(result.players).toEqual(['Steve', 'Alex']);
    });

    it('should handle empty server', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'There are 0 players online: ', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.online).toBe(0);
      expect(result.players).toEqual([]);
    });

    it('should handle single player', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'There are 1 players online: Steve', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.online).toBe(1);
      expect(result.players).toEqual(['Steve']);
    });

    it('should handle unicode usernames', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'There are 2 players online: Player123, 日本語', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.online).toBe(2);
      expect(result.players).toEqual(['Player123', '日本語']);
    });

    it('should fallback for unknown format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Unknown response format', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.online).toBe(0);
      expect(result.players).toEqual([]);
      expect(result.raw).toBe('Unknown response format');
    });
  });

  describe('getWorldTime', () => {
    it('should parse standard time format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The time is 1500', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWorldTime();

      expect(result.time).toBe(1500);
      expect(result.daytime).toBe(true);
    });

    it('should parse alternative format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Time: 8000', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWorldTime();

      expect(result.time).toBe(8000);
      expect(result.daytime).toBe(true);
    });

    it('should correctly identify daytime', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The time is 5000', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWorldTime();

      expect(result.daytime).toBe(true);
    });

    it('should correctly identify nighttime', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The time is 18000', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWorldTime();

      expect(result.time).toBe(18000);
      expect(result.daytime).toBe(false);
    });

    it('should handle boundary at 13000', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The time is 13000', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWorldTime();

      expect(result.daytime).toBe(false);
    });

    it('should fallback for unknown format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Cannot query time', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWorldTime();

      expect(result.time).toBeNull();
      expect(result.daytime).toBeNull();
      expect(result.raw).toBe('Cannot query time');
    });
  });

  describe('getWeather', () => {
    it('should parse clear weather', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The weather is clear', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWeather();

      expect(result.weather).toBe('clear');
    });

    it('should parse rain weather', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The weather is rain', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWeather();

      expect(result.weather).toBe('rain');
    });

    it('should parse thunder weather', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The weather is thunder', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWeather();

      expect(result.weather).toBe('thunder');
    });

    it('should parse alternative format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Weather: clear', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWeather();

      expect(result.weather).toBe('clear');
    });

    it('should fallback for unknown format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Weather unknown', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWeather();

      expect(result.weather).toBeNull();
      expect(result.raw).toBe('Weather unknown');
    });
  });

  describe('getDifficulty', () => {
    it('should parse normal difficulty', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The difficulty is set to: Normal', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getDifficulty();

      expect(result.difficulty).toBe('normal');
    });

    it('should parse easy difficulty', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The difficulty is set to: Easy', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getDifficulty();

      expect(result.difficulty).toBe('easy');
    });

    it('should parse hard difficulty', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The difficulty is set to: Hard', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getDifficulty();

      expect(result.difficulty).toBe('hard');
    });

    it('should parse peaceful difficulty', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The difficulty is set to: Peaceful', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getDifficulty();

      expect(result.difficulty).toBe('peaceful');
    });

    it('should parse alternative format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Difficulty: Normal', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getDifficulty();

      expect(result.difficulty).toBe('normal');
    });

    it('should fallback for unknown format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Unknown difficulty', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getDifficulty();

      expect(result.difficulty).toBeNull();
      expect(result.raw).toBe('Unknown difficulty');
    });
  });

  describe('getGameMode', () => {
    it('should parse creative mode', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Game mode: Creative (Player: Steve)', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getGameMode();

      expect(result.gameMode).toBe('creative');
    });

    it('should parse survival mode', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Game mode: Survival', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getGameMode();

      expect(result.gameMode).toBe('survival');
    });

    it('should parse adventure mode', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Game mode: Adventure', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getGameMode();

      expect(result.gameMode).toBe('adventure');
    });

    it('should parse spectator mode', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Game mode: Spectator', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getGameMode();

      expect(result.gameMode).toBe('spectator');
    });

    it('should handle custom player selector', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Game mode: Creative', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      await helper.getGameMode('Steve');

      expect(mockRcon.getCommands()[0]).toBe('/gamemode query Steve');
    });

    it('should fallback for unknown format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Unknown mode', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getGameMode();

      expect(result.gameMode).toBeNull();
      expect(result.raw).toBe('Unknown mode');
    });
  });

  describe('getTPS', () => {
    it('should parse multiple TPS values', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'TPS from last 1m, 5m, 15m: 20.0, 19.8, 20.0, 20.0', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getTPS();

      expect(result.tps).toBe(20.0);
      expect(result.allTPS).toEqual([20.0, 19.8, 20.0, 20.0]);
    });

    it('should parse single TPS value', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Current TPS: 19.5', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getTPS();

      expect(result.tps).toBe(19.5);
    });

    it('should handle low TPS', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'TPS from last 1m, 5m, 15m: 15.2, 14.8, 16.0, 15.5', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getTPS();

      expect(result.tps).toBe(15.2);
      expect(result.allTPS).toEqual([15.2, 14.8, 16.0, 15.5]);
    });

    it('should handle integer TPS', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Current TPS: 20', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getTPS();

      expect(result.tps).toBe(20);
    });

    it('should fallback for unknown format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'TPS unknown', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getTPS();

      expect(result.tps).toBeNull();
      expect(result.raw).toBe('TPS unknown');
    });
  });

  describe('getSeed', () => {
    it('should parse positive seed', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Seed: [123456789]', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getSeed();

      expect(result.seed).toBe('123456789');
    });

    it('should parse negative seed', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Seed: [-987654321]', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getSeed();

      expect(result.seed).toBe('-987654321');
    });

    it('should parse large seed', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Seed: [123456789012345678]', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getSeed();

      expect(result.seed).toBe('123456789012345678');
    });

    it('should fallback for unknown format', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'Seed unknown', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getSeed();

      expect(result.seed).toBeNull();
      expect(result.raw).toBe('Seed unknown');
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty response', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: '', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.online).toBe(0);
      expect(result.players).toEqual([]);
    });

    it('should handle whitespace-only response', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: '   ', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWeather();

      expect(result.weather).toBeNull();
    });

    it('should handle case variations in weather', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The weather is Rain', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWeather();

      expect(result.weather).toBe('rain');
    });

    it('should handle case variations in difficulty', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The difficulty is set to: HARD', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getDifficulty();

      expect(result.difficulty).toBe('hard');
    });

    it('should handle extra spaces in list response', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'There are 2 players online:  Steve ,  Alex ', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.listPlayers();

      expect(result.players).toEqual(['Steve', 'Alex']);
    });

    it('should handle decimal time values', async () => {
      const mockRcon = new MockRconBackend({
        '*': { raw: 'The time is 1500.75', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const result = await helper.getWorldTime();

      expect(result.time).toBe(1500);
    });

    it('should handle multiple calls', async () => {
      const mockRcon = new MockRconBackend({
        '/list': { raw: 'There are 1 players online: Steve', parsed: null },
        '/time query daytime': { raw: 'The time is 1000', parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      await helper.listPlayers();
      await helper.getWorldTime();

      expect(mockRcon.getCommands()).toHaveLength(2);
    });

    it('should preserve raw response in all methods', async () => {
      const rawResponse = 'Some response';
      const mockRcon = new MockRconBackend({
        '*': { raw: rawResponse, parsed: null }
      });
      const helper = new QueryHelper(mockRcon);

      const timeResult = await helper.getWorldTime();
      expect(timeResult.raw).toBe(rawResponse);

      const weatherResult = await helper.getWeather();
      expect(weatherResult.raw).toBe(rawResponse);
    });
  });
});
