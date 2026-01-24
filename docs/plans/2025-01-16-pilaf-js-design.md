# Pilaf JS Architecture Design

**Date**: 2025-01-16
**Status**: Draft
**Author**: Pilaf Team

## Overview

Pure JavaScript testing framework for Minecraft PaperMC plugin development. Orchestrates tests across Mineflayer clients (player simulation) and RCON (server commands), generating interactive Vue.js-based HTML reports.

Target audience: Plugin developers testing their own plugins with complex player interactions (combat, inventory, movement, abilities).

## Core Concepts

### Test Hierarchy

| Concept | Definition | Implementation |
|---------|------------|----------------|
| **Test Suite** | Entire test run | All files matching test pattern |
| **Story** | Test scenario | `describe` block |
| **Step** | Action within story | `beforeAll`, `it()`, `afterAll`, or implicit actions |
| **Assertion** | Test verification | Jest `expect()` calls or custom matchers |
| **Compare** | State comparison | Before/after snapshots with visual diff |
| **Action** | Backend operation | Client (Mineflayer) or server (RCON) commands |

### Imported Scripts (MECE)

1. **Helpers** - Pure utility functions
   - Example: `waitForEvents(bot, 'entityHurt', 3)`
   - No side effects, composable

2. **Behaviors** - Composable action sequences
   - Example: `attackUntilDead(bot, target)`
   - Encapsulates multi-step operations

3. **Fixtures** - Shared setup/teardown
   - Example: `setupDragonEggTest()`
   - Test environment initialization

## Data Model

```javascript
{
  suiteName: string,
  durationMs: number,
  passed: boolean,
  stories: [{
    name: string,           // describe block name
    file: string,           // test file path
    passedCount: number,
    failedCount: number,
    steps: [{
      name: string,         // it() name or action name
      passed: boolean,
      durationMs: number,
      action?: string,      // command executed
      executionContext: {
        executor: 'RCON' | 'PLAYER' | 'MINEFLAYER' | 'ASSERT' | 'SYSTEM'
        executorPlayer?: string
      },
      stateBefore?: string,  // for comparisons
      stateAfter?: string,
      actual?: string       // response data
    }]
  }]
}
```

## Test Framework Integration

### Jest Reporter Pattern

```javascript
class PilafReporter {
  onTestStart(test) { /* Capture step start */ }
  onTestResult(test, result) { /* Extract results */ }
  onRunComplete(contexts, results) {
    const testResults = this._formatResults(results);
    new ReportGenerator().generate(testResults, 'target/pilaf-reports/report.html');
  }
}
```

### Test Author API

```javascript
const { pilaf, rcon, mineflayer } = require('@pilaf/jest');

describe('Dragon Lightning Ability', () => {
  let attacker, target, server;

  beforeAll(async () => {
    server = await rcon.connect({ host: 'localhost', port: 25575 });
    attacker = await mineflayer.createBot({ username: 'Attacker' });
    target = await mineflayer.createBot({ username: 'Target' });
  });

  afterAll(async () => {
    await attacker.disconnect();
    await target.disconnect();
    await server.disconnect();
  });

  it('strikes 3 times with 0.5s intervals', async () => {
    // Server actions
    await server.send(`op ${attacker.username}`);
    await server.send(`give ${attacker.username} minecraft:dragon_egg`);

    // Client actions
    await attacker.equip('minecraft:dragon_egg', 'off-hand');

    // State capture
    const before = pilaf.captureState({ health: target.health });

    // Execute with event capture
    await attacker.chat('/dragon-lightning');
    const hurtEvents = await pilaf.waitForEvents(target, 'entityHurt', 3, 5000);

    // Assertions
    expect(hurtEvents.length).toBe(3);
    hurtEvents.forEach(e => expect(e.damageSource.type).toBe('lightning'));

    // State comparison
    const after = pilaf.captureState({ health: target.health });
    pilaf.compareStates(before, after, 'should reduce target health');
  });
});
```

### Custom Matchers

```javascript
expect.extend({
  toHaveReceivedLightningStrikes(bot, count) {
    const strikes = bot.receivedEvents.filter(e => e.damageType === 'lightning');
    return {
      pass: strikes.length === count,
      message: () => `expected ${count} lightning strikes, got ${strikes.length}`
    };
  }
});
```

## Backend Layer

### Unified Backend Interface

```javascript
class PilafBackend {
  connect(config) { /* abstract */ }
  disconnect() { /* abstract */ }

  // Server operations (RCON)
  async sendCommand(command) { /* abstract */ }

  // Player operations (Mineflayer)
  async createBot(options) { /* abstract */ }

  // State queries
  async getEntities(selector) { /* abstract */ }
  async getPlayerInventory(player) { /* abstract */ }
  async getBlockAt(x, y, z) { /* abstract */ }
}
```

### RCON Backend

```javascript
const { Rcon } = require('rcon-client');

class RconBackend extends PilafBackend {
  async connect({ host, port, password }) {
    this.client = await Rcon.connect({ host, port, password });
    return this;
  }

  async sendCommand(command) {
    const response = await this.client.send(command);
    return { raw: response, parsed: this._parseResponse(response) };
  }

  disconnect() {
    return this.client.end();
  }
}
```

### Mineflayer Backend

```javascript
const mineflayer = require('mineflayer');

class MineflayerBackend extends PilafBackend {
  async createBot({ username, host, port = 25565, auth = 'offline' }) {
    const bot = mineflayer({ username, host, port, auth });

    // Add Pilaf helpers
    bot.equip = async (item, hand) => { /* ... */ };
    bot.trackEvents = () => { /* ... */ };

    await new Promise((resolve, reject) => {
      bot.once('spawn', resolve);
      bot.once('error', reject);
    });

    return bot;
  }
}
```

### Factory Pattern

```javascript
class PilafBackendFactory {
  static create(type, config) {
    switch(type) {
      case 'rcon': return new RconBackend().connect(config);
      case 'mineflayer': return new MineflayerBackend().connect(config);
      default: throw new Error(`Unknown backend: ${type}`);
    }
  }
}
```

## Report Generation

### Architecture

Single-file HTML with embedded JSON data. Vue.js handles all rendering client-side. No server-side templating engine needed.

### Generator

```javascript
const fs = require('fs');

class ReportGenerator {
  generate(testResults, outputPath) {
    const template = fs.readFileSync('./templates/report.html', 'utf8');
    const cssContent = fs.readFileSync('./templates/styles.css', 'utf8');

    const html = template
      .replace('__SUITENAME__', testResults.suiteName)
      .replace('__STORIESJSON__', JSON.stringify(testResults))
      .replace('__CSSCONTENT__', cssContent);

    fs.writeFileSync(outputPath, html);
  }
}
```

### HTML Template Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Pilaf Test Report - __SUITENAME__</title>
  <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
  <script src="https://cdn.tailwindcss.com"></script>
  <style>__CSSCONTENT__</style>
</head>
<body>
  <div id="app">
    <!-- Vue.js template with {{ }} syntax -->
  </div>
  <script>
    const reportData = __STORIESJSON__;
    Vue.createApp({ data() { return reportData; } }).mount('#app');
  </script>
</body>
</html>
```

## Configuration

### Config File (`pilaf.config.js`)

```javascript
module.exports = {
  backend: {
    rcon: {
      host: 'localhost',
      port: 25575,
      password: process.env.RCON_PASSWORD || 'dragon123'
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

## CLI

### Commands

```bash
pilaf test [files...]       # Run tests
pilaf test --watch          # Watch mode
pilaf health-check          # Check backend connectivity
pilaf report                # Generate report from last run
```

### Implementation (commander.js)

```javascript
const { program } = require('commander');

program
  .command('test [files...]')
  .option('-w, --watch', 'Watch mode')
  .option('--config <path>', 'Config file path')
  .action((files, options) => runTests(files, options));

program
  .command('health-check')
  .action(healthCheck);

program.parse();
```

## File Structure

```
pilaf-js/
├── packages/
│   ├── cli/                    # Command-line tool
│   │   ├── bin/pilaf.js
│   │   ├── lib/runner.js
│   │   └── package.json
│   │
│   ├── framework/              # Core testing framework
│   │   ├── lib/
│   │   │   ├── reporters/pilgraf-reporter.js
│   │   │   ├── matchers/game-matchers.js
│   │   │   └── index.js
│   │   └── package.json
│   │
│   ├── backends/               # Backend implementations
│   │   ├── lib/
│   │   │   ├── backend.js
│   │   │   ├── rcon-backend.js
│   │   │   ├── mineflayer-backend.js
│   │   │   └── factory.js
│   │   └── package.json
│   │
│   └── reporting/              # Report generation
│       ├── lib/generator.js
│       ├── templates/report.html
│       ├── templates/styles.css
│       └── package.json
│
├── scripts/                    # Shared behaviors
│   ├── behaviors/combat.js
│   ├── behaviors/movement.js
│   ├── helpers/events.js
│   ├── helpers/state.js
│   └── fixtures/dragon-test.js
│
├── tests/dragon-lightning.pilaf.test.js
├── pilaf.config.js
├── package.json
└── pnpm-workspace.yaml
```

## Dependencies

- **Jest**: Test framework
- **mineflayer**: Minecraft bot framework
- **rcon-client**: RCON protocol client
- **commander**: CLI parsing
- **Vue.js 3**: Report rendering (CDN)
- **Tailwind CSS**: Report styling (CDN)

## Next Steps

1. Create monorepo structure with pnpm workspaces
2. Implement backend layer (RCON, Mineflayer)
3. Implement Jest reporter with data capture
4. Implement report generator with Vue template
5. Implement CLI with commander
6. Write example tests and documentation
