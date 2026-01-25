# Pilaf

> **Pure JavaScript testing framework for Minecraft PaperMC plugin development.**

Pilaf replaces complex Java integration tests with simple, readable JavaScript test scenarios using Jest and Mineflayer/RCON backends.

[![npm version](https://badge.fury.io/js/%40pilaf%2Fcli.svg)](https://www.npmjs.com/package/@pilaf/cli)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[![Documentation](https://img.shields.io/badge/Website-pilaf_documentation-blue.svg)](https://cavarest.github.io/pilaf/)

## Why Pilaf?

Testing PaperMC plugins traditionally requires writing complex Java integration tests. Pilaf makes this simple by:

- **Writing tests in JavaScript** - No Java compilation, no plugin jar dependencies
- **Using familiar Jest syntax** - `describe`, `it`, `expect` - same as your frontend tests
- **Testing against real servers** - Mineflayer bot players or RCON for command execution
- **Getting instant feedback** - Run tests locally while developing
- **Beautiful HTML reports** - Share test results with your team

## Quick Start

```bash
# Install Pilaf CLI
pnpm add -D @pilaf/cli

# Or install individual packages
pnpm add -D @pilaf/framework @pilaf/backends @pilaf/reporting
```

### Your First Test

Create `tests/basic-command.pilaf.test.js`:

```javascript
const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('My Plugin', () => {
  it('should execute my custom command', async () => {
    const runner = new StoryRunner();

    const result = await runner.execute({
      name: 'Basic command test',
      setup: {
        server: { type: 'paper', version: '1.21.8' }
      },
      steps: [
        {
          name: 'Make player an operator',
          action: 'execute_command',
          command: 'op TestPlayer'
        },
        {
          name: 'Execute my plugin command',
          action: 'execute_player_command',
          player: 'TestPlayer',
          command: '/myplugin hello'
        }
      ],
      teardown: { stop_server: false }
    });

    expect(result.success).toBe(true);
  });
});
```

### Configure Jest

Add to your `jest.config.js`:

```javascript
module.exports = {
  testMatch: ['**/*.pilaf.test.js'],
  testTimeout: 300000, // 5 minutes for server operations
  reporters: [
    'default',
    ['@pilaf/framework/lib/reporters/pilaf-reporter.js', {
      outputPath: 'target/pilaf-reports/index.html',
      suiteName: 'My Plugin Tests'
    }]
  ]
};
```

### Run Tests

```bash
# Set up your environment (or use .env file)
export RCON_HOST=localhost
export RCON_PORT=25575
export RCON_PASSWORD=your_password

# Run tests
pnpm test

# Generate HTML report
pnpm test --reporters=default --reporters=@pilaf/framework/lib/reporters/pilaf-reporter.js
```

---

## Writing Stories

Stories are declarative test scenarios that define setup, steps, and teardown.

### Story Structure

```javascript
const story = {
  // Human-readable name
  name: 'My Test Story',

  // Server and player setup
  setup: {
    server: {
      type: 'paper',      // Server type
      version: '1.21.8',  // Minecraft version
      rcon: {             // RCON connection
        host: 'localhost',
        port: 25575,
        password: 'your_password'
      }
    },
    players: [
      {
        name: 'TestPlayer',  // Internal reference name
        username: 'TestPlayer',  // Minecraft username
        auth: 'offline'     // Authentication type
      }
    ]
  },

  // Test steps executed in sequence
  steps: [
    {
      name: 'Step description',
      action: 'action_name',
      // action-specific parameters...
    }
  ],

  // Cleanup after test
  teardown: {
    stop_server: false,  // Keep server running for more tests
    disconnect_players: true
  }
};
```

### Available Actions

#### Server Actions

| Action | Description | Parameters |
|--------|-------------|------------|
| `execute_command` | Execute RCON command | `command` (string) |
| `wait` | Pause execution | `duration` (ms) |
| `assert` | Make assertion | `condition`, `expected` |

#### Player Actions (Mineflayer only)

| Action | Description | Parameters |
|--------|-------------|------------|
| `execute_player_command` | Execute command as player | `player`, `command` |
| `chat` | Send chat message | `player`, `message` |
| `move_forward` | Move player forward | `player`, `duration` (ms) |
| `login` / `logout` | Reconnect/disconnect player | `player` |
| `respawn` | Respawn player | `player` |

#### Entity Actions (Mineflayer only)

| Action | Description | Parameters |
|--------|-------------|------------|
| `get_entities` | Get nearby entities | `player`, `store_as` |
| `get_entity_location` | Get entity position | `entity`, `store_as` |
| `kill_entity` | Kill an entity | `entity` |

#### Inventory Actions (Mineflayer only)

| Action | Description | Parameters |
|--------|-------------|------------|
| `get_player_inventory` | Get inventory contents | `player`, `store_as` |
| `give_item` | Give item to player | `player`, `item`, `count` |

#### State Actions

| Action | Description | Parameters |
|--------|-------------|------------|
| `get_player_location` | Get player position | `player`, `store_as` |
| `calculate_distance` | Calculate distance | `from`, `to`, `store_as` |

### Using Stored Values

Store results from actions and use them later:

```javascript
const story = {
  name: 'Using stored values',
  setup: {
    server: { type: 'paper', version: '1.21.8' },
    players: [{ name: 'P1', username: 'Player1' }]
  },
  steps: [
    {
      name: 'Get player position',
      action: 'get_player_location',
      player: 'P1',
      store_as: 'start_position'  // Store the result
    },
    {
      name: 'Move player forward',
      action: 'move_forward',
      player: 'P1',
      duration: 1000
    },
    {
      name: 'Get new position',
      action: 'get_player_location',
      player: 'P1',
      store_as: 'end_position'
    },
    {
      name: 'Calculate distance traveled',
      action: 'calculate_distance',
      from: '{start_position}',  // Reference with {}
      to: '{end_position}',
      store_as: 'distance'
    },
    {
      name: 'Verify distance',
      action: 'assert',
      condition: '{distance} > 0',  // Use stored value
      expected: true
    }
  ],
  teardown: { stop_server: false }
};
```

### Complete Example

```javascript
const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Teleport Plugin', () => {
  it('should teleport player to spawn', async () => {
    const runner = new StoryRunner();

    const result = await runner.execute({
      name: 'Teleport to spawn test',
      setup: {
        server: {
          type: 'paper',
          version: '1.21.8',
          rcon: {
            host: process.env.RCON_HOST || 'localhost',
            port: parseInt(process.env.RCON_PORT) || 25575,
            password: process.env.RCON_PASSWORD || 'dragon'
          }
        },
        players: [
          {
            name: 'Steve',
            username: 'TestSteve',
            auth: 'offline'
          }
        ]
      },
      steps: [
        {
          name: 'Teleport player away from spawn',
          action: 'execute_command',
          command: 'tp TestSteve 100 64 100'
        },
        {
          name: 'Get current position',
          action: 'get_player_location',
          player: 'Steve',
          store_as: 'away_position'
        },
        {
          name: 'Execute spawn teleport command',
          action: 'execute_player_command',
          player: 'Steve',
          command: '/spawn'
        },
        {
          name: 'Wait for teleport',
          action: 'wait',
          duration: 500
        },
        {
          name: 'Get spawn position',
          action: 'get_player_location',
          player: 'Steve',
          store_as: 'spawn_position'
        },
        {
          name: 'Verify at spawn coordinates',
          action: 'assert',
          condition: '{spawn_position}.x === 0 && {spawn_position}.z === 0',
          expected: true
        }
      ],
      teardown: { stop_server: false }
    });

    expect(result.success).toBe(true);
  });
});
```

---

## Backends

Pilaf supports multiple backends for different testing needs:

### RCON Backend

Best for server-side command testing:

```javascript
{
  setup: {
    server: {
      rcon: {
        host: 'localhost',
        port: 25575,
        password: 'password'
      }
    }
  }
}
```

**Features:**
- Execute server commands
- Query server state
- Fast and lightweight

### Mineflayer Backend

Best for player interaction testing:

```javascript
{
  setup: {
    server: {
      type: 'paper',
      version: '1.21.8'
    },
    players: [{
      name: 'TestPlayer',
      username: 'BotPlayer',
      auth: 'offline'
    }]
  }
}
```

**Features:**
- Realistic player simulation
- Chat, movement, inventory
- Entity interactions

---

## Configuration

### Environment Variables

```bash
# RCON Connection
RCON_HOST=localhost
RCON_PORT=25575
RCON_PASSWORD=your_password

# Minecraft Server
MC_HOST=localhost
MC_PORT=25565

# Authentication
MC_AUTH=offline  # or 'microsoft' for real auth
```

### .env File

Create `.env` in your project root:

```env
RCON_HOST=localhost
RCON_PORT=25575
RCON_PASSWORD=pilaf_test
MC_HOST=localhost
MC_PORT=25566
MC_AUTH=offline
```

### Jest Configuration

```javascript
// jest.config.js
module.exports = {
  testMatch: ['**/*.pilaf.test.js'],
  testTimeout: 300000,
  moduleNameMapper: {
    '^@pilaf/backends$': '<rootDir>/node_modules/@pilaf/backends/lib',
    '^@pilaf/framework$': '<rootDir>/node_modules/@pilaf/framework/lib',
    '^@pilaf/reporting$': '<rootDir>/node_modules/@pilaf/reporting/lib'
  },
  reporters: [
    'default',
    ['@pilaf/framework/lib/reporters/pilaf-reporter.js', {
      outputPath: 'target/pilaf-reports/index.html',
      suiteName: 'Plugin Tests'
    }]
  ]
};
```

---

## HTML Reports

Pilaf generates interactive HTML reports showing:
- All test stories and their steps
- Action/response pairs for each step
- Console logs per story
- Execution time and status

![HTML Report Example](https://cavarest.github.io/pilaf/images/report-screenshot.png)

Generate reports:

```bash
# Run with HTML report generation
pnpm test --reporters=default --reporters=@pilaf/framework/lib/reporters/pilaf-reporter.js

# View the report
open target/pilaf-reports/index.html
```

---

## Docker Setup (Optional)

Use Docker to run a test server:

```bash
# Start PaperMC server
docker-compose -f docker-compose.dev.yml up -d

# Run tests
RCON_HOST=localhost RCON_PORT=25576 RCON_PASSWORD=cavarest MC_HOST=localhost MC_PORT=25566 pnpm test:report

# Stop server
docker-compose -f docker-compose.dev.yml down
```

Example `docker-compose.dev.yml` with **deterministic flat world for testing**:

```yaml
version: '3.8'
services:
  minecraft:
    image: itzg/minecraft-server
    container_name: pilaf-minecraft-dev
    ports:
      - "${MC_PORT:-25566}:25565"
      - "${RCON_PORT:-25576}:25575"
    environment:
      EULA: 'TRUE'
      ONLINE_MODE: 'false'
      TYPE: 'PAPER'
      VERSION: '1.21.8'
      RCON_PASSWORD: '${RCON_PASSWORD:-cavarest}'
      ENABLE_RCON: 'true'
      RCON_PORT: '25575'
      MAX_PLAYERS: '5'
      MEMORY: '1G'
      SPAWN_PROTECTION: '0'
      WHITELIST: ''

      # === DETERMINISTIC FLAT WORLD FOR TESTING ===
      LEVEL: 'pilaf-test'
      LEVEL_TYPE: 'FLAT'
      SEED: '1234567890'
      GENERATE_STRUCTURES: 'false'
      MAX_WORLD_SIZE: '50'
      MODE: 'creative'
      DIFFICULTY: 'peaceful'
      PVP: 'false'
      ALLOW_NETHER: 'false'

      # === PERFORMANCE FOR TESTING ===
      VIEW_DISTANCE: '4'
      SIMULATION_DISTANCE: '4'

      # === ENTITY SPAWNING FOR TESTING ===
      SPAWN_ANIMALS: 'true'     # Enable for entity testing
      SPAWN_MONSTERS: 'false'   # Disable hostile mobs
      SPAWN_NPCS: 'false'

      # === CUSTOM FLAT WORLD LAYERS ===
      GENERATOR_SETTINGS: >-
        {
          "layers": [
            {"block": "minecraft:bedrock", "height": 1},
            {"block": "minecraft:dirt", "height": 2},
            {"block": "minecraft:grass_block", "height": 1}
          ],
          "biome": "minecraft:plains"
        }
    volumes:
      - mc-data:/data
    healthcheck:
      test: ["CMD", "mc-health"]
      interval: 30s
      timeout: 10s
      retries: 15
      start_period: 120s

volumes:
  mc-data:
```

**Configuration benefits:**
- **Deterministic**: Fixed seed ensures same world every run
- **Fast**: Small world size and reduced view distances speed up tests
- **Predictable**: Flat terrain with custom layers simplifies coordinate testing
- **No hostile mobs**: Peaceful mode with monsters disabled
- **Entity testing**: Animals enabled for spawn/persistence tests

---

## Examples

Check the `examples/` directory for complete examples:

- **basic-rcon** - Simple RCON command testing
- **player-interaction** - Player chat and commands
- **entity-interaction** - Entity spawning and management
- **inventory-testing** - Item manipulation

---

## GitHub Actions Integration

Add CI/CD to your plugin repository:

```yaml
# .github/workflows/pilaf-tests.yml
name: Pilaf Tests

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  pilaf-test:
    runs-on: ubuntu-latest
    timeout-minutes: 60

    services:
      minecraft:
        image: itzg/minecraft-server
        ports:
          - 25566:25565
          - 25576:25575
        env:
          EULA: 'TRUE'
          ONLINE_MODE: 'false'
          TYPE: 'PAPER'
          VERSION: '1.21.8'
          RCON_PASSWORD: 'pilaf_test'
          ENABLE_RCON: 'true'
          RCON_PORT: '25575'
          MAX_PLAYERS: '5'
          SPAWN_PROTECTION: '0'

          # Deterministic flat world for testing
          LEVEL: 'pilaf-test'
          LEVEL_TYPE: 'FLAT'
          SEED: '1234567890'
          GENERATE_STRUCTURES: 'false'
          MAX_WORLD_SIZE: '50'
          MODE: 'creative'
          DIFFICULTY: 'peaceful'
          PVP: 'false'
          ALLOW_NETHER: 'false'
          VIEW_DISTANCE: '4'
          SIMULATION_DISTANCE: '4'
          SPAWN_ANIMALS: 'true'
          SPAWN_MONSTERS: 'false'
          SPAWN_NPCS: 'false'
          GENERATOR_SETTINGS: >-
            {
              "layers": [
                {"block": "minecraft:bedrock", "height": 1},
                {"block": "minecraft:dirt", "height": 2},
                {"block": "minecraft:grass_block", "height": 1}
              ],
              "biome": "minecraft:plains"
            }
        options: >-
          --health-cmd="mc-health"
          --health-interval=10s
          --health-timeout=60s
          --health-retries=10
          --health-start-period=120s

    steps:
    - uses: actions/checkout@v4

    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '18'

    - name: Install pnpm
      uses: pnpm/action-setup@v4
      with:
        version: 8

    - name: Install dependencies
      run: pnpm install

    - name: Run Pilaf tests
      env:
        RCON_HOST: localhost
        RCON_PORT: 25576
        RCON_PASSWORD: pilaf_test
        MC_HOST: localhost
        MC_PORT: 25566
      run: pnpm test

    - name: Upload HTML report
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: pilaf-report
        path: target/pilaf-reports/
```

---

## Development Notes

### Third-Party Dependencies

**prismarine-physics Compatibility Patch**

The `prismarine-physics` package (a transitive dependency via Mineflayer) contains a file import bug where it attempts to import `./lib/features` but the actual file is `./lib/features.json`.

Pilaf automatically patches this during installation via a postinstall hook that creates a symlink from `features` to `features.json`.

**Implementation:**
```javascript
// scripts/fix-prismarine-physics.js
// Symlink: node_modules/.pnpm/prismarine-physics@*/node_modules/prismarine-physics/lib/features -> features.json
```

**Platform Support:**
- Unix/macOS: Symbolic links (`fs.symlinkSync`)
- Windows: Junctions (more reliable on Windows)

**Manual invocation (if needed):**
```bash
node scripts/fix-prismarine-physics.js
```

### Roadmap

**Player Simulation Enhancements**

Current implementation supports: chat, player commands, movement, location/entity queries, inventory inspection, login/logout, and death/respawn.

Planned additions:

| Area | Potential Actions |
|------|-------------------|
| Block Interaction | Break/place blocks, interact with buttons, doors, chests |
| Advanced Movement | Strafe, jump, look control, pathfinding navigation |
| Entity Interaction | Attack, right-click (villagers, animals), mount/dismount |
| Inventory Management | Drop/consume items, equip gear, container interactions |
| Advanced Actions | Sneak, sprint, craft, item usage on blocks |

---

## Releasing

### Using GitHub Actions (Recommended)

Pilaf uses npm trusted publishing for secure, token-less releases. The workflow automatically calculates and bumps versions based on semver:

**Via GitHub CLI:**
```bash
# Patch release (1.0.0 → 1.0.1)
gh workflow run release.yml -f bump=patch

# Minor release (1.0.0 → 1.1.0)
gh workflow run release.yml -f bump=minor

# Major release (1.0.0 → 2.0.0)
gh workflow run release.yml -f bump=major

# Custom version
gh workflow run release.yml -f bump=custom -f custom_version=1.2.3
```

**Via GitHub UI:**
1. Go to: https://github.com/cavarest/pilaf/actions/workflows/release.yml
2. Click "Run workflow"
3. Choose bump type: `patch`, `minor`, `major`, or `custom`
4. If `custom`, enter the version number
5. Click "Run workflow"

The workflow will:
- ✅ Auto-calculate new version from current version
- ✅ Update version in all `@pilaf/*` packages
- ✅ Run tests
- ✅ Publish packages to npm using OIDC
- ✅ Create git tag and push
- ✅ Create GitHub release

### Manual Release

For manual releases or first-time setup:

```bash
# 1. Bump version for all packages
pnpm -r exec npm version patch   # 1.0.0 → 1.0.1
# or
pnpm -r exec npm version minor   # 1.0.0 → 1.1.0
# or
pnpm -r exec npm version major   # 1.0.0 → 2.0.0
# or specific version
VERSION="1.0.1" pnpm -r exec npm version $VERSION --no-git-tag-version

# 2. Run the publish script
./scripts/publish.sh

# 3. Create and push tag
git add packages/*/package.json
git commit -m "chore: release v$VERSION"
git tag -a "v$VERSION" -m "Release v$VERSION"
git push origin main --tags
```

---

## Documentation

Full documentation: [https://cavarest.github.io/pilaf/](https://cavarest.github.io/pilaf/)

- [Getting Started](https://cavarest.github.io/pilaf/docs/getting-started/)
- [Story Writing Guide](https://cavarest.github.io/pilaf/docs/guides/writing-stories.html)
- [Actions Reference](https://cavarest.github.io/pilaf/docs/guides/actions-reference.html)
- [GitHub Actions Setup](https://cavarest.github.io/pilaf/docs/guides/github-actions.html)

---

## Packages

```
@pilaf/cli         # Command-line interface
@pilaf/framework   # Jest integration + StoryRunner
@pilaf/backends    # RCON and Mineflayer backends
@pilaf/reporting   # HTML report generation
```

---

## License

MIT © Pilaf Team

---

## Links

- [GitHub](https://github.com/cavarest/pilaf)
- [npm](https://www.npmjs.com/package/@pilaf/cli)
- [Documentation](https://cavarest.github.io/pilaf/)
- [Issues](https://github.com/cavarest/pilaf/issues)
