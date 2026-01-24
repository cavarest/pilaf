/**
 * Minecraft Log Fixtures
 *
 * Real log lines from Minecraft servers 1.19, 1.20, 1.21+
 * Used for testing MinecraftLogParser
 */

module.exports = {
  // ============================================================================
  // STATUS EVENTS (Server Lifecycle)
  // ============================================================================

  status: {
    starting: [
      '[12:34:56] [Server thread/INFO]: Starting minecraft server version 1.20.1',
      '[12:34:56] [Server thread/INFO]: Starting minecraft server on *:25565',
      '[12:34:56] [Server thread/INFO]: Loading properties',
      '[12:34:56] [Server thread/INFO]: Default game type: SURVIVAL',
      '[12:34:56] [Server thread/INFO]: Generating keypair',
      '[12:34:56] [Worker-Main-1/INFO]: Starting minecraft server version 1.19.4',
      '[12:34:56] [Worker-Main-2/INFO]: Starting minecraft server version 1.21'
    ],
    preparing: [
      '[12:34:56] [Server thread/INFO]: Preparing level "world"',
      '[12:34:56] [Server thread/INFO]: Preparing start region for dimension minecraft:overworld',
      '[12:34:56] [Server thread/INFO]: Preparing start region for level 0'
    ],
    done: [
      '[12:34:56] [Server thread/INFO]: Done (3.452s)! For help, type "help"',
      '[12:34:56] [Server thread/INFO]: Done (5.123s)! For help, type "help"',
      '[12:34:56] [Worker-Main-3/INFO]: Time elapsed: 3422 ms'
    ]
  },

  // ============================================================================
  // ENTITY EVENTS (Player join/leave/death/spawn)
  // ============================================================================

  entity: {
    join: [
      '[12:34:56] [Server thread/INFO]: TestPlayer joined the game',
      '[12:34:56] [Server thread/INFO]: Steve joined the game',
      '[12:34:56] [Server thread/INFO]: Alex joined the game'
    ],
    leave: [
      '[12:34:56] [Server thread/INFO]: TestPlayer lost connection: Disconnected',
      '[12:34:56] [Server thread/INFO]: Steve lost connection: Timed out',
      '[12:34:56] [Server thread/INFO]: Alex lost connection: Internal Exception: java.io.IOException'
    ],
    spawn: [
      '[12:34:56] [Server thread/INFO]: UUID of player TestPlayer is abc123-def456-7890-abcd-ef1234567890',
      '[12:34:56] [Server thread/INFO]: UUID of player Steve is 12345678-1234-5678-1234-5678123456789'
    ],
    death: {
      slain: [
        '[12:34:56] [Server thread/INFO]: TestPlayer was slain by Zombie',
        '[12:34:56] [Server thread/INFO]: Steve was slain by Skeleton',
        '[12:34:56] [Server thread/INFO]: Alex was slain by Spider',
        '[12:34:56] [Server thread/INFO]: TestPlayer was slain by Creeper',
        '[12:34:56] [Server thread/INFO]: Player was slain by Husk',
        '[12:34:56] [Server thread/INFO]: TestPlayer was slain by Phantom'
      ],
      fall: [
        '[12:34:56] [Server thread/INFO]: TestPlayer fell from a high place',
        '[12:34:56] [Server thread/INFO]: Steve fell from a high place'
      ],
      fire: [
        '[12:34:56] [Server thread/INFO]: TestPlayer burned to death',
        '[12:34:56] [Server thread/INFO]: Alex was burnt to a crisp whilst fighting Skeleton'
      ],
      lava: [
        '[12:34:56] [Server thread/INFO]: TestPlayer tried to swim in lava',
        '[12:34:56] [Server thread/INFO]: Steve was killed by Magma Block',
        '[12:34:56] [Server thread/INFO]: Alex was killed by Lava'
      ],
      drown: [
        '[12:34:56] [Server thread/INFO]: TestPlayer drowned',
        '[12:34:56] [Server thread/INFO]: Steve drowned'
      ],
      sprint: [
        '[12:34:56] [Server thread/INFO]: TestPlayer splatted against a wall'
      ],
      generic: [
        '[12:34:56] [Server thread/INFO]: TestPlayer died',
        '[12:34:56] [Server thread/INFO]: Steve died'
      ]
    }
  },

  // ============================================================================
  // MOVEMENT EVENTS (Teleport)
  // ============================================================================

  movement: {
    teleport: [
      '[12:34:56] [Server thread/INFO]: Teleported TestPlayer from 100.5, 64.0, 200.3 to 150.2, 70.0, -300.5',
      '[12:34:56] [Server thread/INFO]: Teleported Steve from -50.0, 80.0, 100.0 to 50.0, 64.0, -50.0',
      '[12:34:56] [Server thread/INFO]: Teleported Alex from 0.5, 100.0, 0.5 to 100.0, 64.0, 200.0',
      '[12:34:56] [Server thread/INFO]: Teleported TestPlayer1 from 256.5, 64.0, -128.9 to -256.5, 70.0, 128.9',
      '[12:34:56] [Server thread/INFO]: Teleported NPC from 10, 70, 20 to 100, 65, -100'
    ]
  },

  // ============================================================================
  // COMMAND EVENTS (Server commands)
  // ============================================================================

  command: {
    issued: [
      '[12:34:56] [Server thread/INFO]: TestPlayer issued server command: /gamemode creative',
      '[12:34:56] [Server thread/INFO]: Steve issued server command: /time set 1000',
      '[12:34:56] [Server thread/INFO]: Alex issued server command: /weather rain',
      '[12:34:56] [Server thread/INFO]: TestPlayer issued server command: /tp Steve 100 64 100',
      '[12:34:56] [Server thread/INFO]: OP issued server command: /stop',
      '[12:34:56] [Server thread/INFO]: Server issued server command: /save-all'
    ]
  },

  // ============================================================================
  // WORLD EVENTS (Time/weather/save)
  // ============================================================================

  world: {
    time: [
      '[12:34:56] [Server thread/INFO]: Changing the time to 1000',
      '[12:34:56] [Server thread/INFO]: Changing the time to 13000',
      '[12:34:56] [Server thread/INFO]: Changing the time to 18000'
    ],
    weather: [
      '[12:34:56] [Server thread/INFO]: Changing the weather to rain',
      '[12:34:56] [Server thread/INFO]: Changing the weather to thunder',
      '[12:34:56] [Server thread/INFO]: Changing the weather to clear'
    ],
    difficulty: [
      '[12:34:56] [Server thread/INFO]: Changing the difficulty to hard',
      '[12:34:56] [Server thread/INFO]: Changing the difficulty to easy',
      '[12:34:56] [Server thread/INFO]: Changing the difficulty to normal'
    ],
    gamemode: [
      '[12:34:56] [Server thread/INFO]: The game mode has been updated to Creative',
      '[12:34:56] [Server thread/INFO]: Gamemode has been updated to Survival Mode',
      '[12:34:56] [Server thread/INFO]: The game mode has been updated to Spectator'
    ],
    save: [
      '[12:34:56] [Server thread/INFO]: Saving the game',
      '[12:34:56] [Server thread/INFO]: Saving the game in level 0',
      '[12:34:56] [Server thread/INFO]: Saved the game',
      '[12:34:56] [Server thread/INFO]: Saving chunks for level \'world\'/minecraft:overworld'
    ]
  },

  // ============================================================================
  // MIXED EVENTS (for testing priority ordering)
  // ============================================================================

  mixed: [
    // Teleport should match over other entity events (priority test)
    '[12:34:56] [Server thread/INFO]: Teleported TestPlayer from 100.0, 64.0, 200.0 to 150.0, 70.0, -300.0',
    // Death should match even if contains command-like text
    '[12:34:56] [Server thread/INFO]: TestPlayer was slain by Zombie using /kill'
  ],

  // ============================================================================
  // EDGE CASES AND VARIATIONS
  // ============================================================================

  edge: [
    // Different thread names
    '[12:34:56] [Worker-Main-1/INFO]: TestPlayer joined the game',
    '[12:34:56] [Worker-Main-2/INFO]: Steve issued server command: /gamemode creative',
    '[12:34:56] [Server-Add-Worker-1/INFO]: Saving the game',

    // Unicode player names
    '[12:34:56] [Server thread/INFO]: 张三 joined the game',
    '[12:34:56] [Server thread/INFO]: игрок joined the game',

    // Long coordinates (negative values)
    '[12:34:56] [Server thread/INFO]: Teleported TestPlayer from -1024.5, 128.0, 2048.9 to 512.0, -60.0, -512.0',

    // Multi-word death messages
    '[12:34:56] [Server thread/INFO]: TestPlayer was shot by a Skeleton',
    '[12:34:56] [Server thread/INFO]: TestPlayer was blown up by a Creeper',

    // Unknown/unmatched patterns
    '[12:34:56] [Server thread/INFO]: Some unknown log message that does not match any pattern',
    '[12:34:56] [Server thread/WARN]: Can\'t keep up! Did the system time change, or is the server overloaded?'
  ]
};
