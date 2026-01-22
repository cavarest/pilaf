# @pilaf/cli

Command-line interface for Pilaf testing framework.

## Installation

```bash
# Install globally
pnpm add -g @pilaf/cli

# Or install locally
pnpm add -D @pilaf/cli
```

## Usage

```bash
# Run all Pilaf tests
pilaf test

# Run specific test file
pilaf test path/to/test.pilaf.test.js

# Run with verbose output
pilaf test --verbose

# Generate HTML report
pilaf report

# Show help
pilaf --help
```

## Configuration

Pilaf looks for a `pilaf.config.js` file in your project root:

```javascript
// pilaf.config.js
module.exports = {
  backend: {
    rcon: {
      host: 'localhost',
      port: 25575,
      password: process.env.RCON_PASSWORD || 'dragon'
    },
    mineflayer: {
      host: 'localhost',
      port: 25565,
      auth: 'offline'
    }
  },
  testMatch: ['**/*.pilaf.test.js', '**/*.story.test.js'],
  testIgnore: ['**/node_modules/**', '**/dist/**'],
  reportDir: 'target/pilaf-reports',
  timeout: 30000,
  retries: 0,
  verbose: false
};
```

## Environment Variables

```bash
# RCON Connection
RCON_HOST=localhost
RCON_PORT=25575
RCON_PASSWORD=your_password

# Minecraft Server
MC_HOST=localhost
MC_PORT=25565

# Authentication
MC_AUTH=offline  # or 'microsoft'
```

## Commands

### `pilaf test [files...]`

Run Pilaf tests.

```bash
# Run all tests
pilaf test

# Run specific file
pilaf test tests/basic-rcon.pilaf.test.js

# Run with pattern
pilaf test tests/**/*.pilaf.test.js

# Verbose mode
pilaf test --verbose
```

### `pilaf report`

Generate HTML report from test results.

```bash
pilaf report
```

Report is saved to `target/pilaf-reports/index.html` by default.

### `pilaf init`

Initialize Pilaf configuration in your project.

```bash
pilaf init
```

Creates `pilaf.config.js` with default settings.

## Examples

Create a test file `my-plugin.test.js`:

```javascript
const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('My Plugin', () => {
  it('should do something', async () => {
    const runner = new StoryRunner();

    const result = await runner.execute({
      name: 'My Test',
      setup: {
        server: {
          rcon: {
            host: process.env.RCON_HOST,
            port: parseInt(process.env.RCON_PORT),
            password: process.env.RCON_PASSWORD
          }
        }
      },
      steps: [
        {
          name: 'Execute command',
          action: 'execute_command',
          command: 'say Hello!'
        }
      ],
      teardown: { stop_server: false }
    });

    expect(result.success).toBe(true);
  });
});
```

Run the test:

```bash
pilaf test my-plugin.test.js
```

## License

MIT
