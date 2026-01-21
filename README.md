# Pilaf

**Pure JavaScript testing framework for Minecraft PaperMC plugin development.**

Pilaf replaces complex Java integration tests with simple, readable JavaScript test scenarios using Jest and Mineflayer.

## Features

- **Pure JavaScript** - No Java code required for testing
- **Jest Integration** - Familiar describe/it syntax with full Jest ecosystem
- **RCON Backend** - Direct server command execution
- **Mineflayer Backend** - Realistic player simulation for complex interactions
- **StoryRunner** - Declarative test stories with variable storage
- **HTML Reports** - Vue.js-powered interactive test reports

## Requirements

- Node.js 18+
- pnpm 10+
- PaperMC Server 1.21.8+ with RCON enabled
- Docker (optional, for containerized testing)

## Installation

```bash
# Clone the repository
git clone https://github.com/cavarest/pilaf.git
cd pilaf

# Install dependencies
pnpm install

# Build all packages
pnpm build
```

## Quick Start

### 1. Configure Your Server

Ensure your PaperMC server has RCON enabled in `server.properties`:

```properties
enable-rcon=true
rcon.password=your_password
rcon.port=25575
broadcast-rcon-to-ops=false
```

### 2. Set Environment Variables

```bash
export RCON_HOST=localhost
export RCON_PORT=25575
export RCON_PASSWORD=your_password
export MC_HOST=localhost
export MC_PORT=25565
```

### 3. Write a Test

Create `my-first-test.pilaf.test.js`:

```javascript
const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('My Plugin Tests', () => {
  it('should execute RCON commands', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Basic RCON Test',
      setup: {
        server: { type: 'paper', version: '1.21.8' }
      },
      steps: [
        {
          name: 'Get server version',
          action: 'execute_command',
          command: 'version'
        }
      ],
      teardown: { stop_server: false }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });
});
```

### 4. Run Tests

```bash
# Run all Pilaf tests
pnpm test

# Run specific test file
pnpm test my-first-test.pilaf.test.js

# Run with verbose output
pnpm test --verbose

# Run for CI (generates JUnit XML)
pnpm test:ci
```

## Generating HTML Reports

Generate interactive HTML reports from your tests:

```bash
# Generate report from examples/
pnpm test:report

# The report will be saved to:
# target/pilaf-reports/index.html
```

Open the report in your browser:
```bash
# macOS
open target/pilaf-reports/index.html

# Linux
xdg-open target/pilaf-reports/index.html

# Windows
start target/pilaf-reports/index.html
```

## Running Tests

### Quick Start (Run with Docker)

```bash
# Start server and run all tests
./run-tests.sh

# Start server and run specific test
./run-tests.sh tests/player-integration.pilaf.test.js

# Start server and generate report
./run-tests.sh pnpm test:report
```

**Configuration** (single source of truth):
- All ports and settings defined in `.env.example` (defaults)
- `docker-compose.dev.yml` reads from environment variables
- `run-tests.sh` reads from environment variables
- **Works out of the box** - no setup needed!

**Default Dev Ports** (to avoid conflicts with production servers):
- Minecraft: `localhost:25566` (instead of default 25565)
- RCON: `localhost:25576` (instead of default 25575)

**Optional:** Customize by creating `.env`:
```bash
# Only create .env if you need to change defaults
cp .env.example .env

# Then edit .env to change ports
MC_PORT=25570
RCON_PORT=25575
```

The script will:
1. Start a PaperMC server in Docker
2. Wait for it to be ready (60-90 seconds)
3. Run your tests
4. Leave the server running for more tests

Stop the server when done:
```bash
docker-compose -f docker-compose.dev.yml down
```

### Manual Server Setup

If you have your own PaperMC server:

1. Enable RCON in `server.properties`:
```properties
enable-rcon=true
rcon.password=cavarest
rcon.port=25575
```

2. Set environment variables:
```bash
export RCON_HOST=localhost
export RCON_PORT=25575
export RCON_PASSWORD=cavarest
export MC_HOST=localhost
export MC_PORT=25565
```

3. Run tests:
```bash
pnpm test
```

## Example Tests

See the `examples/` directory for complete examples:

- `basic-rcon.example.pilaf.test.js` - RCON commands
- `player-interaction.example.pilaf.test.js` - Player chat and movement
- `entity-interaction.example.pilaf.test.js` - Entity spawning and lifecycle
- `inventory-testing.example.pilaf.test.js` - Item giving and inventory checks

## Test Reports

Pilaf generates multiple types of test reports:

- **Console Output** - Real-time test results in terminal
- **JUnit XML** - `test-results/junit.xml` for CI/CD integration
- **HTML Reports** - `target/pilaf-reports/index.html` interactive reports

### Generating HTML Reports

```bash
# Generate HTML report from examples
pnpm test:report

# View the report
open target/pilaf-reports/index.html
```

### Custom Report Configuration

Create `jest.config.report.json` for custom report settings:

```json
{
  "reporters": [
    [
      "<rootDir>/packages/framework/lib/reporters/pilaf-reporter.js",
      {
        "outputPath": "target/pilaf-reports/my-report.html",
        "suiteName": "My Custom Suite"
      }
    ]
  ]
}
```

## Project Structure

```
pilaf/
├── packages/
│   ├── backends/      # RCON and Mineflayer implementations
│   ├── cli/           # Command-line interface
│   ├── framework/     # Jest integration + StoryRunner
│   └── reporting/     # Vue.js HTML report generation
├── tests/             # Integration tests
│   ├── stories/       # Reusable story objects
│   └── *.pilaf.test.js # Test files
├── examples/          # Example tests
└── docs/              # Full documentation
```

## Story Structure

Pilaf tests use declarative "stories":

```javascript
{
  name: 'Story Name',
  setup: {
    server: { type: 'paper', version: '1.21.8' },
    players: [{ name: 'Player', username: 'player1' }]
  },
  steps: [
    {
      name: 'Step name',
      action: 'execute_command',
      command: 'op player1'
    },
    {
      name: 'Store result',
      action: 'get_player_location',
      player: 'player1',
      store_as: 'position'  // Store for later use
    },
    {
      name: 'Use stored value',
      action: 'calculate_distance',
      from: '{position}',  // Reference with {}
      to: '{new_position}'
    }
  ],
  teardown: { stop_server: false }
}
```

## Available Actions

- `execute_command` - Execute RCON command
- `chat` - Send chat message as player
- `execute_player_command` - Execute player command
- `move_forward` - Move player forward
- `wait` - Pause execution
- `get_entities` - Get entity list
- `get_player_inventory` - Get player inventory
- `get_player_location` - Get player position
- `get_entity_location` - Get entity position
- `calculate_distance` - Calculate distance between positions
- `login` / `logout` - Reconnect/disconnect player
- `kill` / `respawn` - Player lifecycle
- `assert` - Make assertions

See [docs/guides/actions-reference.html](docs/guides/actions-reference.html) for complete reference.

## Documentation

Full documentation is available in the `docs/` directory:

- [Getting Started](docs/getting-started/)
- [Guides](docs/guides/)
- [Core Topics](docs/core/)
- [API Reference](docs/reference/)

Or view online: https://cavarest.github.io/pilaf/

## Development

```bash
# Install dependencies
pnpm install

# Run tests
pnpm test

# Lint code
pnpm lint

# Build packages
pnpm build
```

## License

MIT License - see LICENSE file for details

## Contributing

Contributions welcome! Please read our contributing guidelines and submit pull requests to https://github.com/cavarest/pilaf/pull/new/main
