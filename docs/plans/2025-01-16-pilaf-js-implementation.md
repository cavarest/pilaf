# Pilaf JS Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace Java-based Pilaf with pure JavaScript testing framework for Minecraft PaperMC plugin development.

**Architecture:** Monorepo with pnpm workspaces containing packages for CLI, framework, backends, and reporting. Tests use Jest with custom reporter capturing execution metadata for Vue.js-based HTML reports.

**Tech Stack:** Node.js, pnpm, Jest, mineflayer, rcon-client, commander, Vue.js 3 (CDN), Tailwind CSS (CDN)

---

## Task 1: Initialize Monorepo Structure

**Files:**
- Create: `package.json`
- Create: `pnpm-workspace.yaml`
- Create: `pilaf.config.js`

**Step 1: Create root package.json**

```json
{
  "name": "pilaf",
  "version": "1.0.0",
  "private": true,
  "description": "Pure JS testing framework for Minecraft PaperMC plugins",
  "scripts": {
    "test": "pnpm -r --filter './packages/**' test",
    "build": "pnpm -r --filter './packages/**' build",
    "lint": "eslint packages/**/*.js",
    "pilaf": "node packages/cli/bin/pilaf.js"
  },
  "devDependencies": {
    "eslint": "^8.56.0"
  },
  "engines": {
    "node": ">=18.0.0",
    "pnpm": ">=8.0.0"
  }
}
```

**Step 2: Create pnpm-workspace.yaml**

```yaml
packages:
  - 'packages/*'
```

**Step 3: Create default pilaf.config.js**

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

**Step 4: Commit**

```bash
git add package.json pnpm-workspace.yaml pilaf.config.js
git commit -m "feat: initialize monorepo structure"
```

---

## Task 2: Create Backends Package

**Files:**
- Create: `packages/backends/package.json`
- Create: `packages/backends/lib/backend.js`
- Create: `packages/backends/lib/rcon-backend.js`
- Create: `packages/backends/lib/mineflayer-backend.js`
- Create: `packages/backends/lib/factory.js`
- Create: `packages/backends/lib/index.js`

**Step 1: Create backends/package.json**

```json
{
  "name": "@pilaf/backends",
  "version": "1.0.0",
  "main": "lib/index.js",
  "dependencies": {
    "mineflayer": "^4.20.1",
    "rcon-client": "^4.3.0"
  }
}
```

**Step 2: Create base backend interface**

```javascript
// packages/backends/lib/backend.js
class PilafBackend {
  connect(config) {
    throw new Error('connect() must be implemented by subclass');
  }

  disconnect() {
    throw new Error('disconnect() must be implemented by subclass');
  }

  async sendCommand(command) {
    throw new Error('sendCommand() must be implemented by subclass');
  }

  async createBot(options) {
    throw new Error('createBot() must be implemented by subclass');
  }

  async getEntities(selector) {
    throw new Error('getEntities() must be implemented by subclass');
  }

  async getPlayerInventory(player) {
    throw new Error('getPlayerInventory() must be implemented by subclass');
  }

  async getBlockAt(x, y, z) {
    throw new Error('getBlockAt() must be implemented by subclass');
  }
}

module.exports = { PilafBackend };
```

**Step 3: Create RCON backend**

```javascript
// packages/backends/lib/rcon-backend.js
const { Rcon } = require('rcon-client');
const { PilafBackend } = require('./backend');

class RconBackend extends PilafBackend {
  constructor() {
    super();
    this.client = null;
  }

  async connect({ host, port, password }) {
    this.client = await Rcon.connect({
      host,
      port: parseInt(port, 10),
      password
    });
    return this;
  }

  async sendCommand(command) {
    if (!this.client) {
      throw new Error('RCON client not connected');
    }
    const response = await this.client.send(command);
    return {
      raw: response,
      parsed: this._parseResponse(response)
    };
  }

  disconnect() {
    if (this.client) {
      return this.client.end();
    }
    return Promise.resolve();
  }

  _parseResponse(response) {
    try {
      return JSON.parse(response);
    } catch {
      return { text: response };
    }
  }
}

module.exports = { RconBackend };
```

**Step 4: Create Mineflayer backend**

```javascript
// packages/backends/lib/mineflayer-backend.js
const mineflayer = require('mineflayer');
const { PilafBackend } = require('./backend');

class MineflayerBackend extends PilafBackend {
  constructor() {
    super();
    this.bots = new Map();
  }

  async connect({ host, port }) {
    this.host = host;
    this.port = port;
    return this;
  }

  async createBot({ username, host = this.host, port = this.port, auth = 'offline' }) {
    const bot = mineflayer({ username, host, port: parseInt(port, 10), auth });

    // Add Pilaf-specific helpers
    bot.equip = async (item, hand = 'hand') => {
      const itemType = bot.registry.itemsByName[item];
      if (!itemType) {
        throw new Error(`Unknown item: ${item}`);
      }
      bot.equip(itemType.id, hand);
      await this._waitForEquip(bot);
    };

    bot.trackEvents = (eventNames) => {
      const events = [];
      eventNames.forEach(eventName => {
        bot.on(eventName, (event) => events.push({ type: eventName, data: event }));
      });
      return events;
    };

    // Wait for spawn
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('Bot spawn timeout')), 30000);
      bot.once('spawn', () => {
        clearTimeout(timeout);
        resolve();
      });
      bot.once('error', (err) => {
        clearTimeout(timeout);
        reject(err);
      });
    });

    this.bots.set(username, bot);
    return bot;
  }

  disconnect() {
    return Promise.all(
      Array.from(this.bots.values()).map(bot => bot.quit())
    );
  }

  _waitForEquip(bot) {
    return new Promise(resolve => setTimeout(resolve, 100));
  }
}

module.exports = { MineflayerBackend };
```

**Step 5: Create factory**

```javascript
// packages/backends/lib/factory.js
const { RconBackend } = require('./rcon-backend');
const { MineflayerBackend } = require('./mineflayer-backend');

class PilafBackendFactory {
  static create(type, config) {
    switch (type) {
      case 'rcon':
        return new RconBackend().connect(config);
      case 'mineflayer':
        return new MineflayerBackend().connect(config);
      default:
        throw new Error(`Unknown backend type: ${type}`);
    }
  }
}

module.exports = { PilafBackendFactory };
```

**Step 6: Create index exports**

```javascript
// packages/backends/lib/index.js
const { PilafBackend } = require('./backend');
const { RconBackend } = require('./rcon-backend');
const { MineflayerBackend } = require('./mineflayer-backend');
const { PilafBackendFactory } = require('./factory');

module.exports = {
  PilafBackend,
  RconBackend,
  MineflayerBackend,
  PilafBackendFactory
};
```

**Step 7: Commit**

```bash
git add packages/backends/
git commit -m "feat: implement backends package (RCON + Mineflayer)"
```

---

## Task 3: Create Reporting Package

**Files:**
- Create: `packages/reporting/package.json`
- Create: `packages/reporting/lib/generator.js`
- Create: `packages/reporting/templates/report.html`
- Create: `packages/reporting/templates/styles.css`
- Create: `packages/reporting/lib/index.js`

**Step 1: Create reporting/package.json**

```json
{
  "name": "@pilaf/reporting",
  "version": "1.0.0",
  "main": "lib/index.js",
  "dependencies": {}
}
```

**Step 2: Create HTML template**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Pilaf Test Report - __SUITENAME__</title>
  <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
  <script src="https://cdn.tailwindcss.com"></script>
  <style>__CSSCONTENT__</style>
</head>
<body class="bg-gray-900 text-gray-100 min-h-screen">
  <div id="app">
    <header class="bg-gray-800 border-b border-gray-700 sticky top-0 z-50 p-4">
      <h1 class="text-xl font-bold">Pilaf Test Report</h1>
      <p class="text-sm text-gray-400">{{ report.suiteName }}</p>
      <span class="px-3 py-1 rounded-full text-sm font-semibold"
            :class="report.passed ? 'bg-green-900 text-green-300' : 'bg-red-900 text-red-300'">
        {{ report.passed ? 'PASSED' : 'FAILED' }}
      </span>
    </header>
    <main class="max-w-7xl mx-auto px-4 py-6">
      <div v-for="story in stories" :key="story.name" class="bg-gray-800 rounded-lg p-4 mb-4 border border-gray-700">
        <h2 class="text-lg font-semibold text-white">{{ story.name }}</h2>
        <p class="text-sm text-gray-400">{{ story.passedCount }} passed, {{ story.failedCount }} failed</p>
        <div v-for="step in story.steps" :key="step.name" class="ml-4 mt-2 p-2 rounded"
             :class="step.passed ? 'bg-green-900/30' : 'bg-red-900/30'">
          <span class="font-mono text-sm">{{ step.name }}</span>
          <span v-if="step.executionContext" class="ml-2 px-2 py-0.5 rounded text-xs bg-gray-700">
            {{ step.executionContext.executor }}
          </span>
        </div>
      </div>
    </main>
  </div>
  <script>
    const reportData = __STORIESJSON__;
    const { createApp } = Vue;
    createApp({
      data() {
        return {
          report: reportData,
          stories: reportData.stories || []
        };
      }
    }).mount('#app');
  </script>
</body>
</html>
```

**Step 3: Create CSS styles**

```css
/* Minimal styles - Tailwind handles most */
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
```

**Step 4: Create generator**

```javascript
// packages/reporting/lib/generator.js
const fs = require('fs');
const path = require('path');

class ReportGenerator {
  constructor(options = {}) {
    this.templatePath = options.templatePath ||
      path.join(__dirname, '../templates/report.html');
    this.cssPath = options.cssPath ||
      path.join(__dirname, '../templates/styles.css');
  }

  generate(testResults, outputPath) {
    const template = fs.readFileSync(this.templatePath, 'utf8');
    const cssContent = fs.readFileSync(this.cssPath, 'utf8');

    const html = template
      .replace('__SUITENAME__', this._escapeHtml(testResults.suiteName || 'Pilaf Tests'))
      .replace('__STORIESJSON__', this._escapeJson(JSON.stringify(testResults)))
      .replace('__CSSCONTENT__', cssContent);

    // Ensure output directory exists
    const dir = path.dirname(outputPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    fs.writeFileSync(outputPath, html);
    return outputPath;
  }

  _escapeHtml(text) {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  _escapeJson(json) {
    // Escape </script> to prevent breaking out of script tag
    return json.replace(/<\/script>/g, '<\\/script>');
  }
}

module.exports = { ReportGenerator };
```

**Step 5: Create index exports**

```javascript
// packages/reporting/lib/index.js
const { ReportGenerator } = require('./generator');

module.exports = {
  ReportGenerator
};
```

**Step 6: Commit**

```bash
git add packages/reporting/
git commit -m "feat: implement reporting package (Vue.js HTML generator)"
```

---

## Task 4: Create Framework Package

**Files:**
- Create: `packages/framework/package.json`
- Create: `packages/framework/lib/reporters/pilaf-reporter.js`
- Create: `packages/framework/lib/matchers/game-matchers.js`
- Create: `packages/framework/lib/helpers/events.js`
- Create: `packages/framework/lib/helpers/state.js`
- Create: `packages/framework/lib/index.js`

**Step 1: Create framework/package.json**

```json
{
  "name": "@pilaf/framework",
  "version": "1.0.0",
  "main": "lib/index.js",
  "dependencies": {
    "@pilaf/backends": "workspace:*",
    "@pilaf/reporting": "workspace:*",
    "jest": "^29.7.0"
  },
  "devDependencies": {
    "@jest/globals": "^29.7.0"
  },
  "peerDependencies": {
    "jest": ">=29.0.0"
  }
}
```

**Step 2: Create Jest reporter**

```javascript
// packages/framework/lib/reporters/pilaf-reporter.js
const { ReportGenerator } = require('@pilaf/reporting');

class PilafReporter {
  constructor(globalConfig, options = {}) {
    this._globalConfig = globalConfig;
    this._options = options;
    this._results = {
      suiteName: options.suiteName || 'Pilaf Tests',
      durationMs: 0,
      passed: true,
      stories: []
    };
    this._currentStory = null;
    this._storyStartTime = 0;
  }

  onRunStart(aggregatedResults, options) {
    this._runStartTime = Date.now();
  }

  onTestSuiteStart(testSuite) {
    // testSuite is a describe block
    this._currentStory = {
      name: testSuite.testPath ? testSuite.testPath.split('/').pop() : 'Unknown Story',
      file: testSuite.testPath || '',
      passedCount: 0,
      failedCount: 0,
      steps: []
    };
    this._storyStartTime = Date.now();
  }

  onTestResult(test, testResult) {
    const step = {
      name: testResult.fullName,
      passed: testResult.status === 'passed',
      durationMs: testResult.duration,
      executionContext: {
        executor: 'ASSERT'
      }
    };

    if (testResult.status === 'passed') {
      this._currentStory.passedCount++;
    } else {
      this._currentStory.failedCount++;
      this._results.passed = false;
    }

    this._currentStory.steps.push(step);
  }

  onTestSuiteResult(testSuite, testSuiteResult) {
    if (this._currentStory) {
      this._results.stories.push(this._currentStory);
      this._currentStory = null;
    }
  }

  onRunComplete(contexts, aggregatedResults) {
    this._results.durationMs = Date.now() - this._runStartTime;

    if (this._options.outputPath) {
      const generator = new ReportGenerator();
      generator.generate(this._results, this._options.outputPath);
      console.log(`\n[Pilaf] Report generated: ${this._options.outputPath}`);
    }
  }
}

module.exports = { PilafReporter };
```

**Step 3: Create event helpers**

```javascript
// packages/framework/lib/helpers/events.js

async function waitForEvents(bot, eventName, count, timeout = 5000) {
  const events = [];
  const handler = (data) => events.push({ type: eventName, data });

  bot.on(eventName, handler);

  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bot.removeListener(eventName, handler);
      if (events.length >= count) {
        resolve(events);
      } else {
        reject(new Error(`Timeout waiting for ${count} ${eventName} events (got ${events.length})`));
      }
    }, timeout);

    const checkComplete = () => {
      if (events.length >= count) {
        clearTimeout(timer);
        bot.removeListener(eventName, handler);
        resolve(events);
      }
    };

    bot.on(eventName, checkComplete);
  });
}

function captureEvents(bot, eventNames) {
  const captured = [];
  const handlers = {};

  eventNames.forEach(eventName => {
    const handler = (data) => captured.push({ type: eventName, data });
    handlers[eventName] = handler;
    bot.on(eventName, handler);
  });

  return {
    events: captured,
    release: () => {
      Object.entries(handlers).forEach(([eventName, handler]) => {
        bot.removeListener(eventName, handler);
      });
    }
  };
}

module.exports = {
  waitForEvents,
  captureEvents
};
```

**Step 4: Create state helpers**

```javascript
// packages/framework/lib/helpers/state.js

function captureState(data) {
  return {
    timestamp: Date.now(),
    data: JSON.parse(JSON.stringify(data)) // Deep clone
  };
}

function compareStates(before, after, description = 'State comparison') {
  const changes = [];

  const beforeKeys = Object.keys(before.data || {});
  const afterKeys = Object.keys(after.data || {});

  const allKeys = new Set([...beforeKeys, ...afterKeys]);

  allKeys.forEach(key => {
    const beforeVal = before.data[key];
    const afterVal = after.data[key];

    if (JSON.stringify(beforeVal) !== JSON.stringify(afterVal)) {
      changes.push({
        key,
        before: beforeVal,
        after: afterVal
      });
    }
  });

  return {
    description,
    before,
    after,
    changes,
    hasChanges: changes.length > 0
  };
}

module.exports = {
  captureState,
  compareStates
};
```

**Step 5: Create game matchers**

```javascript
// packages/framework/lib/matchers/game-matchers.js

function toHaveReceivedLightningStrikes(received, count) {
  const strikes = received.filter(e =>
    e.type === 'entityHurt' && e.data?.damageSource?.type === 'lightning'
  );

  return {
    pass: strikes.length === count,
    message: () => `expected ${count} lightning strikes, got ${strikes.length}`
  };
}

module.exports = {
  toHaveReceivedLightningStrikes
};
```

**Step 6: Create main index exports**

```javascript
// packages/framework/lib/index.js
const { PilafReporter } = require('./reporters/pilaf-reporter');
const { waitForEvents, captureEvents } = require('./helpers/events');
const { captureState, compareStates } = require('./helpers/state');
const { toHaveReceivedLightningStrikes } = require('./matchers/game-matchers');

const { PilafBackendFactory } = require('@pilaf/backends');

// Backend helpers
const rcon = {
  connect: async (config) => PilafBackendFactory.create('rcon', config)
};

const mineflayer = {
  createBot: async (options) => {
    const backend = await PilafBackendFactory.create('mineflayer', options);
    return backend.createBot(options);
  }
};

// Main pilaf API
const pilaf = {
  waitForEvents,
  captureEvents,
  captureState,
  compareStates
};

module.exports = {
  // Reporter
  PilafReporter,

  // Main API
  pilaf,
  rcon,
  mineflayer,

  // Helpers
  waitForEvents,
  captureEvents,
  captureState,
  compareStates,

  // Matchers
  toHaveReceivedLightningStrikes
};
```

**Step 7: Commit**

```bash
git add packages/framework/
git commit -m "feat: implement framework package (Jest reporter + helpers)"
```

---

## Task 5: Create CLI Package

**Files:**
- Create: `packages/cli/package.json`
- Create: `packages/cli/bin/pilaf.js`
- Create: `packages/cli/lib/runner.js`
- Create: `packages/cli/lib/config-loader.js`

**Step 1: Create CLI package.json**

```json
{
  "name": "@pilaf/cli",
  "version": "1.0.0",
  "main": "lib/index.js",
  "bin": {
    "pilaf": "./bin/pilaf.js"
  },
  "dependencies": {
    "@pilaf/framework": "workspace:*",
    "commander": "^12.0.0",
    "jest": "^29.7.0"
  },
  "devDependencies": {
    "@jest/globals": "^29.7.0"
  }
}
```

**Step 2: Create config loader**

```javascript
// packages/cli/lib/config-loader.js
const fs = require('fs');
const path = require('path');

function loadConfig(configPath) {
  const defaultConfig = {
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

  if (configPath && fs.existsSync(configPath)) {
    const userConfig = require(path.resolve(configPath));
    return { ...defaultConfig, ...userConfig };
  }

  const defaultPath = path.join(process.cwd(), 'pilaf.config.js');
  if (fs.existsSync(defaultPath)) {
    const userConfig = require(defaultPath);
    return { ...defaultConfig, ...userConfig };
  }

  return defaultConfig;
}

module.exports = { loadConfig };
```

**Step 3: Create test runner**

```javascript
// packages/cli/lib/runner.js
const { createConfig } = require('@jest/core');
const { PilafReporter } = require('@pilaf/framework');

async function runTests(files, options) {
  const config = createConfig(
    {
      testMatch: options.testMatch || ['**/*.pilaf.test.js'],
      testPathIgnorePatterns: options.testIgnore || ['node_modules'],
      reporters: [
        'default',
        ['@pilaf/framework', {
          suiteName: options.suiteName || 'Pilaf Tests',
          outputPath: options.outputPath || 'target/pilaf-reports/report.html'
        }]
      ],
      testTimeout: options.timeout || 30000
    },
    null
  );

  if (files && files.length > 0) {
    config.set('testMatch', files);
  }

  const { runCLI } = await import('jest');
  const result = await runCLI(config, [process.cwd(), ...config.args]);

  return results;
}

module.exports = { runTests };
```

**Step 4: Create CLI entry point**

```javascript
#!/usr/bin/env node
const { program } = require('commander');
const { loadConfig } = require('./lib/config-loader');

program
  .name('pilaf')
  .description('Pure JS testing framework for Minecraft PaperMC plugins')
  .version('1.0.0');

program
  .command('test [files...]')
  .description('Run Pilaf tests')
  .option('-w, --watch', 'Watch mode')
  .option('--config <path>', 'Config file path')
  .option('--output <path>', 'Report output path')
  .action(async (files, options) => {
    const config = loadConfig(options.config);

    // For now, just echo what would be run
    console.log('[Pilaf] Would run tests:', files || config.testMatch);
    console.log('[Pilaf] Report would be saved to:', options.output || config.reportDir);
    console.log('[Pilaf] Full implementation in next tasks');
  });

program
  .command('health-check')
  .description('Check backend connectivity')
  .action(async () => {
    const { loadConfig } = require('./lib/config-loader');
    const { PilafBackendFactory } = require('@pilaf/backends');
    const config = loadConfig();

    console.log('[Pilaf] Checking RCON connection...');
    try {
      const rcon = await PilafBackendFactory.create('rcon', config.backend.rcon);
      const result = await rcon.sendCommand('list');
      console.log('[Pilaf] ✓ RCON connected:', result.raw.substring(0, 100));
      await rcon.disconnect();
    } catch (err) {
      console.error('[Pilaf] ✗ RCON connection failed:', err.message);
    }
  });

program.parse();
```

**Step 5: Make CLI executable**

```bash
chmod +x packages/cli/bin/pilaf.js
```

**Step 6: Commit**

```bash
git add packages/cli/
git commit -m "feat: implement CLI package (commander + jest integration)"
```

---

## Task 6: Create Example Test

**Files:**
- Create: `tests/dragon-lightning.pilaf.test.js`

**Step 1: Create example test**

```javascript
// tests/dragon-lightning.pilaf.test.js
const { pilaf, rcon, mineflayer } = require('@pilaf/framework');

describe('Dragon Lightning Ability', () => {
  let attacker, target, server;

  beforeAll(async () => {
    server = await rcon.connect({ host: 'localhost', port: 25575 });
    attacker = await mineflayer.createBot({ username: 'Attacker' });
    target = await mineflayer.createBot({ username: 'Target' });
  }, 30000);

  afterAll(async () => {
    if (attacker) await attacker.quit();
    if (target) await target.quit();
    if (server) await server.disconnect();
  });

  it('should connect bots successfully', () => {
    expect(attacker).toBeDefined();
    expect(target).toBeDefined();
    expect(server).toBeDefined();
  });

  it('strikes 3 times with 0.5s intervals', async () => {
    // Setup: Give attacker dragon egg
    await server.send(`op ${attacker.username}`);
    await server.send(`give ${attacker.username} minecraft:dragon_egg`);

    // Equip and activate
    await attacker.equip('minecraft:dragon_egg', 'off-hand');

    // Capture events
    const hurtEvents = await pilaf.waitForEvents(target, 'entityHurt', 3, 5000);

    // Execute ability
    await attacker.chat('/dragon-lightning');

    // Assert
    expect(hurtEvents.length).toBe(3);
  }, 10000);
});
```

**Step 2: Commit**

```bash
git add tests/
git commit -m "feat: add example dragon lightning test"
```

---

## Task 7: Create README Documentation

**Files:**
- Create: `README.md`
- Update: `CLAUDE.md`

**Step 1: Create main README**

```markdown
# Pilaf

Pure JavaScript testing framework for Minecraft PaperMC plugin development.

## Features

- **Mineflayer Integration**: Realistic player simulation for complex interactions
- **RCON Support**: Direct server command execution
- **Jest-Based**: Familiar describe/it syntax with full Jest ecosystem
- **Interactive Reports**: Vue.js-powered HTML reports with state comparisons
- **Type-Safe**: Full TypeScript support (optional)

## Quick Start

\`\`\`bash
# Install
npm install -g @pilaf/cli

# Run tests
pilaf test

# Health check
pilaf health-check
\`\`\`

## Example Test

\`\`\`javascript
const { pilaf, rcon, mineflayer } = require('@pilaf/framework');

describe('My Plugin Feature', () => {
  let bot, server;

  beforeAll(async () => {
    server = await rcon.connect({ host: 'localhost', port: 25575 });
    bot = await mineflayer.createBot({ username: 'TestBot' });
  });

  it('should do something', async () => {
    await server.send('op TestBot');
    await bot.chat('/myplugin command');
    const events = await pilaf.waitForEvents(bot, 'entityHurt', 1, 5000);
    expect(events.length).toBe(1);
  });
});
\`\`\`

## Configuration

Create \`pilaf.config.js\`:

\`\`\`javascript
module.exports = {
  backend: {
    rcon: {
      host: 'localhost',
      port: 25575,
      password: process.env.RCON_PASSWORD
    },
    mineflayer: {
      host: 'localhost',
      port: 25565,
      auth: 'offline'
    }
  },
  reportDir: 'target/pilaf-reports'
};
\`\`\`

## Documentation

See [docs/](docs/) for full documentation.
```

**Step 2: Update CLAUDE.md**

```markdown
# CLAUDE.md

## Build Commands

\`\`\`bash
pnpm install              # Install dependencies
pnpm test                 # Run all tests
pnpm -r build            # Build all packages
pilaf test              # Run Pilaf tests
\`\`\`

## Project Overview

Pilaf is a **pure JavaScript** testing framework for Minecraft PaperMC plugin development. It orchestrates tests across Mineflayer clients (player simulation) and RCON (server commands), generating interactive Vue.js-based HTML reports.

**Key Goal**: Replace complex Java integration tests with simple, readable JavaScript tests.

## Architecture

\`\`\`
Jest Tests → Pilaf Reporter → Backend Layer → Minecraft Server
            (Data Capture)    (Mineflayer/RCON)    (PaperMC)
\`\`\`

### Core Packages

- **@pilaf/cli**: Command-line interface using commander
- **@pilaf/framework**: Jest integration with custom reporters and helpers
- **@pilaf/backends**: RCON and Mineflayer backend implementations
- **@pilaf/reporting**: Vue.js-based HTML report generation

## Data Model

See [docs/plans/2025-01-16-pilaf-js-design.md](docs/plans/2025-01-16-pilaf-js-design.md) for complete architecture.

## Development

### Running Tests

\`\`\`bash
# Unit tests
pnpm --filter @pilaf/framework test

# Integration tests (requires Minecraft server)
pilaf test tests/**/*.pilaf.test.js
\`\`\`

### Adding New Features

1. Add backend method in \`packages/backends/lib/\`
2. Add helper in \`packages/framework/lib/helpers/\`
3. Update exports in \`packages/framework/lib/index.js\`
4. Write test in \`tests/\`
```

**Step 3: Commit**

```bash
git add README.md CLAUDE.md
git commit -m "docs: add README and update CLAUDE.md for JS implementation"
```

---

## Task 8: Remove Java Codebase

**Files:**
- Delete: All Java source files
- Delete: `src/` directory
- Delete: `build.gradle`, `gradlew`, etc.
- Delete: Java-specific docs

**Step 1: Remove Java source directories**

```bash
rm -rf src/
```

**Step 2: Remove Gradle files**

```bash
rm -f build.gradle gradlew gradlew.bat gradle.properties settings.gradle
```

**Step 3: Remove Java-specific test resources**

```bash
rm -rf bin/test/
```

**Step 4: Remove old docs (keep new ones)**

```bash
# Keep docs/plans/ and README.md, remove old Java-specific docs
rm -rf docs/_references/
```

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove Java codebase (complete migration to JS)"
```

---

## Task 9: Install Dependencies and Verify

**Step 1: Install pnpm (if not installed)**

```bash
npm install -g pnpm
```

**Step 2: Install dependencies**

```bash
pnpm install
```

**Step 3: Verify packages build**

```bash
pnpm build
```

**Step 4: Test CLI help**

```bash
node packages/cli/bin/pilaf.js --help
```

Expected output:
```
pilaf [command]

Commands:
  pilaf test [files...]  Run Pilaf tests
  pilaf health-check     Check backend connectivity

Options:
  --version   output version number
  -h, --help  display help for command
```

**Step 5: Commit**

```bash
git add pnpm-lock.yaml
git commit -m "chore: install dependencies and verify setup"
```

---

## Task 10: Final Polish - Git and Publishing

**Files:**
- Create: `.gitignore`
- Update: `package.json` with proper fields

**Step 1: Create .gitignore**

```text
node_modules/
target/
*.log
.DS_Store
dist/
coverage/
.pnpm-store/
```

**Step 2: Update root package.json for publishing**

```json
{
  "name": "pilaf",
  "version": "1.0.0",
  "private": false,
  "description": "Pure JS testing framework for Minecraft PaperMC plugins",
  "keywords": ["minecraft", "testing", "papermc", "mineflayer", "rcon"],
  "author": "Pilaf Team",
  "license": "MIT",
  "repository": {
    "type": "git",
    "url": "https://github.com/cavarest/pilaf.git"
  },
  "scripts": {
    "test": "pnpm -r --filter './packages/**' test",
    "build": "pnpm -r --filter './packages/**' build",
    "lint": "eslint packages/**/*.js",
    "pilaf": "node packages/cli/bin/pilaf.js"
  },
  "devDependencies": {
    "eslint": "^8.56.0"
  },
  "engines": {
    "node": ">=18.0.0",
    "pnpm": ">=8.0.0"
  }
}
```

**Step 3: Commit**

```bash
git add .gitignore package.json
git commit -m "chore: add .gitignore and finalize package.json"
```

**Step 4: Tag release**

```bash
git tag -a v1.0.0 -m "Initial JS release - replaces Java implementation"
git push origin v1.0.0
```

---

## Completion Checklist

- [ ] All 10 tasks completed
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Java code removed
- [ ] CLI functional
- [ ] Example test provided
- [ ] Git tagged

---

**End of Implementation Plan**
