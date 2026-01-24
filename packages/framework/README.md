# @pilaf/framework

Jest integration and test framework for Pilaf.

Provides the `StoryRunner` for executing declarative test stories against Minecraft servers.

## Installation

```bash
pnpm add @pilaf/framework
```

## Peer Dependencies

```bash
pnpm add -D jest@^29.0.0
```

## Quick Start - Bot Player Testing

The simplest way to test with bot players:

```javascript
const { createTestContext, cleanupTestContext } = require('@pilaf/framework');

describe('My Plugin Tests', () => {
  let context;

  beforeAll(async () => {
    // Creates RCON connection + bot player in one call
    context = await createTestContext({
      username: 'TestPlayer',
      host: 'localhost',
      gamePort: 25565,
      rconPort: 25575,
      rconPassword: 'minecraft'
    });
  });

  afterAll(async () => {
    await cleanupTestContext(context);
  });

  it('should execute command as bot', async () => {
    // Use bot.chat() for player commands
    context.bot.chat('/gamemode creative');

    // Use backend.sendCommand() for RCON commands
    const result = await context.backend.sendCommand('seed');
    expect(result.raw).toContain('Seed');
  });
});
```

## StoryRunner

The `StoryRunner` executes declarative test stories against Minecraft servers.

```javascript
const { StoryRunner } = require('@pilaf/framework');

const runner = new StoryRunner();

const story = {
  name: 'My Test Story',
  setup: {
    server: {
      type: 'paper',
      version: '1.21.8',
      rcon: {
        host: 'localhost',
        port: 25575,
        password: 'password'
      }
    },
    players: [
      {
        name: 'Player1',
        username: 'TestPlayer',
        auth: 'offline'
      }
    ]
  },
  steps: [
    {
      name: 'Make player an operator',
      action: 'execute_command',
      command: 'op Player1'
    },
    {
      name: 'Execute player command',
      action: 'execute_player_command',
      player: 'Player1',
      command: '/gamemode creative'
    }
  ],
  teardown: {
    stop_server: false,
    disconnect_players: true
  }
};

const result = await runner.execute(story);
console.log(result.success); // true or false
```

## Story Structure

### Setup

```javascript
setup: {
  server: {
    type: 'paper',           // Server type (optional)
    version: '1.21.8',        // Minecraft version (optional)
    rcon: {
      host: 'localhost',
      port: 25575,
      password: 'password'
    }
  },
  players: [
    {
      name: 'InternalName',    // Reference name in steps
      username: 'MinecraftName',
      auth: 'offline'          // or 'microsoft'
    }
  ]
}
```

### Steps

Each step has:

```javascript
{
  name: 'Step description',
  action: 'action_name',
  // action-specific parameters
}
```

**Available Actions:**

- `execute_command` - Execute RCON command
- `execute_player_command` - Execute command as player
- `chat` - Send chat message
- `move_forward` - Move player forward
- `wait` - Pause execution
- `get_entities` - Get nearby entities
- `get_player_inventory` - Get player inventory
- `get_player_location` - Get player position
- `calculate_distance` - Calculate distance between positions
- `login` / `logout` - Reconnect/disconnect player
- `respawn` - Respawn player
- `assert` - Make assertions

### Using Stored Values

Store results from steps and use them later:

```javascript
steps: [
  {
    name: 'Get position',
    action: 'get_player_location',
    player: 'P1',
    store_as: 'start_pos'
  },
  {
    name: 'Calculate distance',
    action: 'calculate_distance',
    from: '{start_pos}',
    to: '{end_pos}',
    store_as: 'distance'
  }
]
```

### Teardown

```javascript
teardown: {
  stop_server: false,         // Keep server running
  disconnect_players: true    // Disconnect players
}
```

## Jest Reporter

Pilaf includes a custom Jest reporter that generates HTML reports:

```javascript
// jest.config.js
module.exports = {
  testMatch: ['**/*.pilaf.test.js'],
  reporters: [
    'default',
    ['@pilaf/framework/lib/reporters/pilaf-reporter.js', {
      outputPath: 'target/pilaf-reports/index.html',
      suiteName: 'My Plugin Tests'
    }]
  ]
};
```

## Helpers

### Test Context Helper

The `createTestContext()` helper provides a simplified way to create bot players for testing. It sets up both RCON and Mineflayer backends simultaneously.

**Why use this?**

When testing Minecraft plugins with bot players, you need TWO separate connections:
1. **Bot player** - For executing player commands (via `bot.chat()`)
2. **RCON** - For server commands that return responses (via `rcon.send()`)

The `MineflayerBackend.sendCommand()` method only sends commands via bot chat and **always returns empty** responses. You need RCON to get actual server responses for commands like `/data get`, `/teleport`, etc.

```javascript
const { createTestContext, cleanupTestContext } = require('@pilaf/framework');

describe('My Plugin', () => {
  let context;

  beforeAll(async () => {
    context = await createTestContext({
      username: 'TestPlayer',
      rconPassword: 'dragon123'
    });
  });

  afterAll(async () => {
    await cleanupTestContext(context);
  });

  it('should verify entity position after teleport', async () => {
    // Get position BEFORE (using RCON - returns actual data!)
    const beforeResult = await context.rcon.send('data get entity TestPlayer Pos');
    const beforePos = parsePosition(beforeResult);

    // Execute ability (using bot chat)
    context.bot.chat('/myplugin teleport 100 64 100');
    await wait(1000);

    // Get position AFTER (using RCON - returns actual data!)
    const afterResult = await context.rcon.send('data get entity TestPlayer Pos');
    const afterPos = parsePosition(afterResult);

    // Verify teleportation
    expect(afterPos.x).toBeCloseTo(100, 0);
  });
});
```

**API Reference:**

```javascript
const context = await createTestContext({
  username: 'TestPlayer',      // Bot username (default: 'TestPlayer')
  host: 'localhost',           // Server host (default: 'localhost')
  gamePort: 25565,             // Game port (default: 25565)
  rconPort: 25575,             // RCON port (default: 25575)
  rconPassword: 'minecraft',   // RCON password (default: 'minecraft')
  auth: 'offline'              // Auth mode (default: 'offline')
});

// Returns:
{
  backend: MineflayerBackend,  // For bot control
  rcon: RconBackend,           // For server commands WITH RESPONSES
  bot: Object,                 // The bot player instance
  playerName: string          // Bot's username
}

// Use RCON for commands that need responses
const result = await context.rcon.send('data get entity TestPlayer Pos');

// Use bot chat for player commands
context.bot.chat('/mycommand');

// Cleanup when done
await cleanupTestContext(context);
```

### State Management

```javascript
const { captureState, compareStates } = require('@pilaf/framework/lib/helpers/state');

// Capture state with deep clone
const before = captureState(myObject);

// Compare states
const diff = compareStates(before, after);
console.log(diff.hasChanges); // true or false
console.log(diff.changes);    // Array of changes
```

### Event Helpers

```javascript
const { waitForEvents, captureEvents } = require('@pilaf/framework/lib/helpers/events');

// Wait for events
const events = await waitForEvents(bot, 'chat', 3, 5000);

// Capture events
const capture = captureEvents(bot, ['chat', 'playerJoined']);
// ... do stuff ...
capture.release(); // Clean up listeners
```

## Matchers

Custom Jest matchers for game testing:

```javascript
const { matchers } = require('@pilaf/framework');

expect.extend(matchers);

// Use in tests
await expect(player).toHavePosition({ x: 100, y: 64, z: 100 });
await expect(player).toHaveInventoryItem('diamond');
```

## License

MIT
