/**
 * QueryHelper - Convenience methods for common Minecraft server queries
 *
 * Wraps RconBackend to provide structured data from common RCON commands.
 * Parses raw RCON responses into useful JavaScript objects.
 *
 * Responsibilities (MECE):
 * - ONLY: Provide convenience methods for RCON queries
 * - NOT: Connect to RCON (RconBackend's responsibility)
 * - NOT: Handle errors (RconBackend's responsibility)
 * - NOT: Cache results (caller's responsibility)
 *
 * Usage Example:
 *   const helper = new QueryHelper(rconBackend);
 *   const players = await helper.listPlayers();
 *   // Returns: { online: 2, players: ['Steve', 'Alex'] }
 */

/**
 * QueryHelper class for structured RCON queries
 */
class QueryHelper {
  /**
   * Create a QueryHelper
   *
   * @param {Object} rconBackend - RconBackend instance
   * @throws {Error} If rconBackend is not provided
   */
  constructor(rconBackend) {
    if (!rconBackend) {
      throw new Error('RconBackend is required');
    }

    /**
     * RconBackend instance
     * @private
     * @type {Object}
     */
    this._rcon = rconBackend;
  }

  /**
   * Get information about a specific player
   *
   * @param {string} username - Player username
   * @returns {Promise<Object>} - Player information
   * @property {string} username - Player name
   * @property {Object} position - Player position {x, y, z}
   * @property {number} health - Player health
   * @property {number} food - Player food level
   * @property {number} exp - Player experience
   * @property {number} level - Player level
   */
  async getPlayerInfo(username) {
    const response = await this._rcon.sendCommand(`data get entity ${username}`);

    // If RCON returned parsed NBT data, use it
    if (response.parsed) {
      return this._parsePlayerInfo(response.parsed, username);
    }

    // Otherwise parse from raw text or return minimal info
    return {
      username,
      position: null,
      health: null,
      food: null,
      exp: null,
      level: null,
      raw: response.raw
    };
  }

  /**
   * List all online players
   *
   * @returns {Promise<Object>} - List of online players
   * @property {number} online - Number of online players
   * @property {Array<string>} players - Array of player names
   */
  async listPlayers() {
    const response = await this._rcon.sendCommand('/list');

    // Parse response format: "There are 2 players online: Steve, Alex"
    const onlineMatch = response.raw.match(/There are (\d+) players online:? (.*)/);
    if (onlineMatch) {
      const online = parseInt(onlineMatch[1], 10);
      const playersStr = onlineMatch[2].trim();
      const players = playersStr === '' ? [] : playersStr.split(', ').map(p => p.trim());
      return { online, players };
    }

    // Alternative format: "Steve, Alex"
    const playersMatch = response.raw.match(/^([a-zA-Z0-9_]+(?:, [a-zA-Z0-9_]+)*)$/);
    if (playersMatch) {
      const players = response.raw.split(', ').map(p => p.trim());
      return { online: players.length, players };
    }

    // Fallback
    return { online: 0, players: [], raw: response.raw };
  }

  /**
   * Get current world time
   *
   * @returns {Promise<Object>} - World time information
   * @property {number} time - Game time (0-24000)
   * @property {boolean} daytime - Whether it's daytime
   */
  async getWorldTime() {
    const response = await this._rcon.sendCommand('/time query daytime');

    // Parse: "The time is 1500"
    const timeMatch = response.raw.match(/The time is (\d+)/);
    if (timeMatch) {
      const time = parseInt(timeMatch[1], 10);
      return {
        time,
        daytime: time >= 0 && time < 13000
      };
    }

    // Parse: "Time: 1500" (alternative format)
    const altMatch = response.raw.match(/Time: (\d+)/);
    if (altMatch) {
      const time = parseInt(altMatch[1], 10);
      return {
        time,
        daytime: time >= 0 && time < 13000
      };
    }

    return { time: null, daytime: null, raw: response.raw };
  }

  /**
   * Get current weather state
   *
   * @returns {Promise<Object>} - Weather information
   * @property {string} weather - Weather type (clear, rain, thunder)
   * @property {number} duration - Remaining duration (if available)
   */
  async getWeather() {
    const response = await this._rcon.sendCommand('/weather query');

    // Parse: "The weather is clear"
    const weatherMatch = response.raw.match(/The weather is (\w+)/);
    if (weatherMatch) {
      return { weather: weatherMatch[1].toLowerCase() };
    }

    // Parse: "Weather: clear" (alternative format)
    const altMatch = response.raw.match(/Weather: (\w+)/);
    if (altMatch) {
      return { weather: altMatch[1].toLowerCase() };
    }

    return { weather: null, raw: response.raw };
  }

  /**
   * Get difficulty level
   *
   * @returns {Promise<Object>} - Difficulty information
   * @property {string} difficulty - Difficulty (peaceful, easy, normal, hard)
   */
  async getDifficulty() {
    const response = await this._rcon.sendCommand('/difficulty');

    // Parse: "The difficulty is set to: Normal"
    const match = response.raw.match(/The difficulty is set to: (\w+)/);
    if (match) {
      return { difficulty: match[1].toLowerCase() };
    }

    // Parse: "Difficulty: Normal" (alternative format)
    const altMatch = response.raw.match(/Difficulty: (\w+)/);
    if (altMatch) {
      return { difficulty: altMatch[1].toLowerCase() };
    }

    return { difficulty: null, raw: response.raw };
  }

  /**
   * Get game mode
   *
   * @param {string} [player='@s'] - Target player selector
   * @returns {Promise<Object>} - Game mode information
   * @property {string} gameMode - Game mode (survival, creative, adventure, spectator)
   */
  async getGameMode(player = '@s') {
    const response = await this._rcon.sendCommand(`/gamemode query ${player}`);

    // Parse: "Game mode: Creative (Player: Steve)"
    const match = response.raw.match(/Game mode: (\w+)/);
    if (match) {
      return { gameMode: match[1].toLowerCase() };
    }

    return { gameMode: null, raw: response.raw };
  }

  /**
   * Get server TPS (ticks per second)
   *
   * @returns {Promise<Object>} - TPS information
   * @property {number} tps - Current TPS
   */
  async getTPS() {
    const response = await this._rcon.sendCommand('/tps');

    // Parse: "TPS from last 1m, 1m, 5m, 15m: 20.0, 20.0, 20.0, 20.0"
    const match = response.raw.match(/TPS from last [\w, ]+: ([\d., ]+)/);
    if (match) {
      const tpsValues = match[1].split(', ').map(s => parseFloat(s.trim()));
      return {
        tps: tpsValues[0] || null,
        allTPS: tpsValues
      };
    }

    // Parse: "Current TPS: 20.0"
    const simpleMatch = response.raw.match(/Current TPS: ([\d.]+)/);
    if (simpleMatch) {
      return { tps: parseFloat(simpleMatch[1]) };
    }

    return { tps: null, raw: response.raw };
  }

  /**
   * Get world seed
   *
   * @returns {Promise<Object>} - Seed information
   * @property {string} seed - World seed
   */
  async getSeed() {
    const response = await this._rcon.sendCommand('/seed');

    // Parse: "Seed: [123456789]"
    const match = response.raw.match(/Seed: \[([-\d]+)\]/);
    if (match) {
      return { seed: match[1] };
    }

    return { seed: null, raw: response.raw };
  }

  /**
   * Parse player info from NBT data
   *
   * @private
   * @param {Object} nbtData - NBT data from RCON
   * @param {string} username - Player username
   * @returns {Object} - Parsed player info
   */
  _parsePlayerInfo(nbtData, username) {
    // NBT data structure varies by Minecraft version
    // Common structure: { Pos: [I; x, y, z], Health, FoodLevel, XpLevel, ... }

    return {
      username,
      position: nbtData.Pos ? {
        x: nbtData.Pos[0],
        y: nbtData.Pos[1],
        z: nbtData.Pos[2]
      } : null,
      health: nbtData.Health || null,
      food: nbtData.FoodLevel || null,
      exp: nbtData.XpTotal || nbtData.XpLevel ? (nbtData.XpTotal || 0) : null,
      level: nbtData.XpLevel || null,
      raw: nbtData
    };
  }
}

module.exports = { QueryHelper };
