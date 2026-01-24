# @pilaf/backends

Backend implementations for Pilaf testing framework.

This package provides RCON and Mineflayer backends, log monitoring, event correlation, and pattern-based parsing for comprehensive Minecraft PaperMC server testing.

## Installation

```bash
pnpm add @pilaf/backends
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Pilaf Test Suite                          │
│              (Jest Tests with Pilaf Reporter)                      │
└─────────────────────────────────────────────────────────────────┘
                                  │
                ┌─────────────────┼─────────────────┐
                ▼                 ▼                 ▼
    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
    │ Mineflayer   │   │     RCON     │   │    Docker     │
    │   Backend    │   │   Backend    │   │   Collector   │
    │              │   │              │   │              │
    │ - Chat       │   │ - Commands  │   │ - Stream      │
    │ - Movement   │   │ - Queries   │   │ - Reconnect  │
    │ - Actions    │   │ - Health     │   │ - Parsing    │
    └──────────────┘   └──────────────┘   └──────────────┘
            │                  │                  │
            └──────────────────┼──────────────────┘
                               ▼
                    ┌──────────────────────┐
                    │   Pilaf Core Layer    │
                    │                      │
                    │ ┌──────────────────┐ │
                    │ │  QueryHelper      │ │
                    │ │  - listPlayers()  │ │
                    │ │  - getTPS()       │ │
                    │ │  - getWorldTime() │ │
                    │ └──────────────────┘ │
                    │                      │
                    │ ┌──────────────────┐ │
                    │ │ EventObserver   │ │
                    │ │  - onPlayerJoin() │ │
                    │ │  - onPlayerDeath()│ │
                    │ │  - onEvent()      │ │
                    │ └──────────────────┘ │
                    │                      │
                    │ ┌──────────────────┐ │
                    │ │   LogMonitor     │ │
                    │ │  - Correlation    │ │
                    │ │  - Buffers       │ │
                    │ └──────────────────┘ │
                    │                      │
                    │ ┌──────────────────┐ │
                    │ │ PatternRegistry  │ │
                    │ │  - Add/remove    │ │
                    │ │  - Priority      │ │
                    │ └──────────────────┘ │
                    └──────────────────────┘
```

---

## 📦 Backends

### RCONBackend

Connects via RCON protocol to execute server commands and retrieve responses.

```javascript
const { RconBackend } = require('@pilaf/backends');

const backend = new RconBackend().connect({
  host: 'localhost',
  port: 25575,
  password: 'your_password'
});

// Execute command
const response = await backend.send('/op player1');
// { raw: '...', parsed: null }
```

### MineflayerBackend

Creates a real Minecraft player using Mineflayer for realistic player simulation.

```javascript
const { MineflayerBackend } = require('@pilaf/backends');

const backend = new MineflayerBackend();
await backend.connect({
  host: 'localhost',
  port: 25565,
  username: 'TestBot',
  auth: 'offline'  // or 'microsoft'
});

// Spawn bot player
await backend.spawn();

// Get player position
const pos = await backend.getPlayerPosition();

// Chat as player
await backend.chat('Hello world!');

// Disconnect
await backend.disconnect();
```

### PilafBackendFactory

Factory for creating backend instances.

```javascript
const { PilafBackendFactory } = require('@pilaf/backends');

// Create RCON backend
const rcon = PilafBackendFactory.create('rcon', {
  host: 'localhost',
  port: 25575,
  password: 'password'
});

// Create Mineflayer backend
const bot = PilafBackendFactory.create('mineflayer', {
  host:localhost',
  port: 25565,
  username: 'TestBot',
  auth: 'offline'
});
```

---

## 🔍 QueryHelper

Convenience methods for common RCON queries with structured response parsing.

### Overview

QueryHelper eliminates the need for manual string parsing of RCON responses, providing type-safe, structured data.

### API Reference

```javascript
const { QueryHelper } = require('@pilaf/backends');

const helper = new QueryHelper(rconBackend);

// List online players
const players = await helper.listPlayers();
// { online: 2, players: ['Steve', 'Alex'], raw: '...' }

// Get detailed player info
const info = await helper.getPlayerInfo('Steve');
// { player: 'Steve', dimension: 'minecraft:overworld',
//   position: { x: 100.5, y: 64.0, z: -200.3 },
//   health: 20, food: 20, saturation: 5, etc. }

// Get world time
const time = await helper.getWorldTime();
// { time: 1500, daytime: true, raw: '...' }

// Get weather
const weather = await helper.getWeather();
// { weather: 'clear', duration: -1, raw: '...' }

// Get difficulty
const difficulty = await helper.getDifficulty();
//// { difficulty: 'hard', raw: '...' }

// Get game mode
const gameMode = await helper.getGameMode();
// { gameMode: 'survival', mode: 'Survival', raw: '...' }

// Get server TPS
const tps = await helper.getTPS();
// { tps: 19.8, raw: '...' }

// Get world seed
const seed = await helper.getSeed();
// { seed: 1234567890, raw: '...' }
```

### Usage Example

```javascript
const { MineflayerBackend } = require('@pilaf/backends');

describe('Server State Tests', () => {
  let backend;

  beforeEach(async () => {
    backend = new MineflayerBackend();
    await backend.connect({
      host: 'localhost',
      port: 25565,
      auth: 'offline',
      rconPort: 25575,
      rconPassword: 'test'
    });
  });

  it('should retrieve player information', async () => {
    const info = await backend.getPlayerInfo('TestPlayer');

    expect(info.player).toBe('TestPlayer');
    expect(info.health).toBeGreaterThan(0);
    expect(info.position).toBeDefined();
  });

  it('should get current server TPS', async () => {
    const { tps } = await backend.getTPS();

    expect(tps).toBeGreaterThan(15); // Server should be healthy
  });

  afterEach(async () => {
    await backend.disconnect();
  });
});
```

---

## 📡 EventObserver

Clean API for subscribing to Minecraft server events with pattern matching and wildcard support.

### Overview

EventObserver provides a declarative interface for subscribing to log events without manual pattern matching. Events are parsed from logs in real-time and emitted as structured objects.

### API Reference

```javascript
const { EventObserver } = require('@pilaf/backends');

const observer = new EventObserver({
  logMonitor: monitor,  // LogMonitor instance
  parser: parser        // MinecraftLogParser instance
});

// Subscribe to specific events
const unsubscribe = observer.onEvent('entity.join', (event) => {
  console.log('Join:', event.type, event.data);
});

// Convenience methods
observer.onPlayerJoin((event) => { /* ... */ });
observer.onPlayerLeave((event) => { /* ... */ });
observer.onPlayerDeath((event) => { /* ... */ });
observer.onPlayerChat((event) => { /* ... */ });
observer.onCommand((event) => { /* ... */ });

// Wildcard patterns
observer.onEvent('entity.death.*', (event) => {
  // Matches: entity.death.player, entity.death.mob, etc.
});

// Start observing
await observer.start();

// Stop observing
observer.stop();

// Unsubscribe from specific pattern
unsubscribe();
```

### Event Types

| Event Type | Description | Event Data Structure |
|------------|-------------|---------------------|
| `entity.join` | Player joined | `{ player, timestamp, location }` |
| `entity.leave` | Player left | `{ player, timestamp, reason }` |
| `entity.death.player` | Player died | `{ victim, killer, cause }` |
| `entity.death.mob` | Mob died | `{ victim, killer, cause }` |
| `entity.chat` | Player chat | `{ player, message, timestamp }` |
| `command.success` | Command succeeded | `{ command, executor, result }` |
| `command.failed` | Command failed | `{ command, error }` |

### Usage Example

```javascript
const { MineflayerBackend } = require('@pilaf/backends');

describe('Event Monitoring Tests', () => {
  let backend;

  beforeEach(async () => {
    backend = new MineflayerBackend();
    await backend.connect({ /* ... */ });
    await backend.observe();
  });

  it('should track player join events', async () => {
    const joins = [];
    backend.onPlayerJoin((event) => {
      joins.push(event.data.player);
    });

    // Simulate player joining
    await backend.chat('/test join Player123');

    await new Promise(resolve => setTimeout(resolve, 1000));

    expect(joins).toContain('Player123');
  });

  afterEach(async () => {
    backend.unobserve();
    await backend.disconnect();
  });
});
```

---

## 📊 LogMonitor

Orchestrates log collection, parsing, correlation, and real-time event emission.

### Overview

LogMonitor is the central orchestrator that:
1. Collects logs from Docker containers or files
2. Parses logs using MinecraftLogParser
3. Correlates related events using strategies
4. Emits structured events for consumption
5. Uses a circular buffer for memory efficiency

### Architecture

```
DockerLogCollector → Log Lines → MinecraftLogParser → Structured Events
                                                              ↓
                                                   CircularBuffer (1000 lines)
                                                              ↓
                                                   Correlation Strategy
                                                              ↓
                                                   EventEmitter → Events
                                                              ↓
                                                   Correlation Sessions
```

### API Reference

```javascript
const { LogMonitor } = require('@pilaf/backends');

const monitor = new LogMonitor({
  collector: new DockerLogCollector({ container: 'mc-server', follow: true }),
  parser: new MinecraftLogParser(),
  correlation: new UsernameCorrelationStrategy(),
  bufferSize: 1000  // Max events in circular buffer
});

// Subscribe to all events
monitor.on('event', (event) => {
  console.log('Event:', event.type, event.data);
});

// Subscribe to correlation sessions
monitor.on('correlation', (session) => {
  console.log('Session:', session.username, session.events.length);
});

// Start monitoring
await monitor.start();

// Stop monitoring
monitor.stop();
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `collector` | LogCollector | required | Log collector instance |
| `parser` | LogParser | required | Log parser instance |
| `correlation` | CorrelationStrategy | optional | Event correlation strategy |
| `bufferSize` | number | 1000 | Circular buffer size |

---

## 🔄 Correlation Strategies

### UsernameCorrelationStrategy

Tracks player sessions by grouping events by username.

```javascript
const { UsernameCorrelationStrategy } = require('@pilaf/backends');

const strategy = new UsernameCorrelationStrategy();

// Returns session object with all events for that player
const session = strategy.correlate({
  type: 'entity.join',
  data: { player: 'Steve' }
});

console.log(session.username);    // 'Steve'
console.log(session.isActive);   // true
console.log(session.events);     // Array of all events

// Session ends when player leaves
strategy.correlate({
  type: 'entity.leave',
  data: { player: 'Steve' }
});
```

### TagCorrelationStrategy

Groups events by custom tag/ID with automatic expiration.

```javascript
const { TagCorrelationStrategy } = require('@pilaf/backends');

const strategy = new TagCorrelationStrategy({
  tagExtractor: (event) => event.data.questId,  // Extract tag from event
  timeout: 300000  // 5 minutes auto-expire
});

// All events with same questId are grouped
strategy.correlate({
  type: 'quest.started',
  data: { questId: 'dragon-quest-123', player: 'Steve' }
});

strategy.correlate({
  type: 'quest.objective',
  data: { questId: 'dragon-quest-123', objective: 'kill_dragon' }
});

strategy.correlate({
  type: 'quest.completed',
  data: { questId: 'dragon-quest-123', player: 'Steve' }
});

// All three events are grouped in one session
```

---

## 📝 MinecraftLogParser

Pattern-based log parser that extracts structured events from raw Minecraft log lines.

### Supported Log Patterns

| Pattern | Event Type | Description |
|---------|-----------|-------------|
| `/Teleported (\w+) to (.+)/` | `teleport` | Player teleportation |
| `/(\w+) was slain by (\w+)/` | `entity.death.player` | Player killed by entity |
| `/(\w+) was slain by (.+)/` | `entity.death.mob` | Mob killed |
| `/(\w+) joined the game/` | `entity.join` | Player joined |
| `/(\w+) left the game/` | `entity.leave` | Player left |
| `/\<(\w+)\> (?:ordered|said)\:/` | `entity.chat` | Player chat |
| `/Issued server command: (\S+) .+ by (\w+)/` | `command.success` | Command executed |

### Usage Example

```javascript
const { MinecraftLogParser } = require('@pilaf/backends');

const parser = new MinecraftLogParser();

// Add custom pattern
parser.addPattern('custom', '/Custom event: (.+)/', (match) => ({
  message: match[1]
}));

// Parse a log line
const event = parser.parse('[12:34:56] [Server thread/INFO]: Teleported Steve to 100 70 200');

if (event) {
  console.log(event.type);     // 'teleport'
  console.log(event.data);     // { player: 'Steve', destination: '100 70 200' }
  console.log(event.raw);       // Original log line
}
```

---

## 🗃️ PatternRegistry

Centralized pattern management with priority-based ordering for complex log parsing scenarios.

### Overview

PatternRegistry manages multiple parsing patterns with:
- **Priority ordering**: More specific patterns tested first (lower priority number = higher priority)
- **Pattern types**: RegExp or string patterns
- **Handler functions**: Custom parsing logic
- **Dynamic management**: Add/remove patterns at runtime

### API Reference

```javascript
const { PatternRegistry } = require('@pilaf/backends');

const registry = new PatternRegistry({
  caseInsensitive: false  // Enable case-insensitive matching
});

// Add pattern with priority (0-10, where 0 is highest priority)
registry.addPattern('high-priority', /Specific pattern (.+)/, (match) => ({
  captured: match[1]
}), 1);  // High priority (tested first)

registry.addPattern('low-priority', /.+/, (match) => ({
  everything: match[0]
}), 10);  // Low priority (tested last)

// Match first matching pattern in priority order
const result = registry.match('[Server] Specific pattern matched');

// Remove pattern
registry.removePattern('high-priority');

// Get pattern
const pattern = registry.getPattern('low-priority');

// Get all pattern names
const names = registry.getPatterns();

// Clone registry
const cloned = registry.clone();

// Clear all patterns
registry.clear();
```

### Priority System

Patterns are tested in ascending priority order:

```javascript
// Priority 1: High priority (tested first)
registry.addPattern('exact-match', /^Teleported Steve to (.+)/), handler, 1);

// Priority 10: Low priority (fallback)
registry.addPattern('fallback', /.+/, handler, 10);

// When parsing "Teleported Steve to 100 64 100":
// - Tests 'exact-match' first → matches!
// - Returns immediately, 'fallback' never tested
```

### Usage Example

```javascript
const { PatternRegistry, MinecraftLogParser } = require('@pilaf/backends');

const parser = new MinecraftLogParser();

// Add custom high-priority pattern
parser.addPattern('dragon-spawn', /Dragon spawned at (.+)/, (match) => ({
  location: match[1]
}), 1);

// Add low-priority catch-all
parser.addPattern('default', /.+/, (match) => ({
  raw: match[0]
}), 10);

// Parse log line - dragon pattern tested first
const event = parser.parse('[12:34:56] Dragon spawned at 100 70 200');
// { type: 'dragon-spawn', data: { location: '100 70 200' }, raw: '...' }
```

---

## 🐳 DockerLogCollector

Streams logs from Docker containers with automatic reconnection and error handling.

### Overview

DockerLogCollector connects to Docker containers and follows log output in real-time, with:
- **Automatic reconnection**: Exponential backoff on disconnection
- **ANSI code stripping**: Clean log output without color codes
- **Pause/resume**: Control data flow during tests
- **Error handling**: Graceful error recovery

### API Reference

```javascript
const { DockerLogCollector } = require('@pilaf/backends');

const collector = new DockerLogCollector({
  dockerodeOptions: {
    socketPath: '/var/run/docker.sock'
  },
  reconnectDelay: 1000,        // Initial reconnection delay (ms)
  maxReconnectDelay: 30000,   // Maximum reconnection delay (ms)
  reconnectAttempts: 5        // Maximum reconnection attempts
});

// Connect to container
await collector.connect({
  containerName: 'minecraft-server',
  follow: true,             // Follow log stream
  tail: 100,               // Last N lines from history
  stdout: true,
  stderr: true,
  disableAutoReconnect: false  // Disable automatic reconnection
});

// Subscribe to events
collector.on('data', (line) => {
  console.log('Log:', line);
});

collector.on('connected', () => {
  console.log('Connected to container');
});

collector.on('reconnecting', (info) => {
  console.log(`Reconnecting attempt ${info.attempt}/${info.maxAttempts}`);
});

collector.on('end', () => {
  console.log('Stream ended');
});

collector.on('error', (error) => {
  console.error('Collector error:', error);
});

// Pause/resume
collector.pause();  // Stop emitting data events
collector.resume(); // Resume emitting data events

// Get reconnection status
const status = collector.getReconnectStatus();
// { attempt: 0, maxAttempts: 5, reconnecting: false }

// Disconnect
await collector.disconnect();
```

### Usage Example with LogMonitor

```javascript
const { LogMonitor, DockerLogCollector, MinecraftLogParser, UsernameCorrelationStrategy } = require('@pilaf/backends');

const monitor = new LogMonitor({
  collector: new DockerLogCollector({
    container: 'minecraft-server',
    follow: true
  }),
  parser: new MinecraftLogParser(),
  correlation: new UsernameCorrelationStrategy(),
  bufferSize: 1000
});

// Subscribe to events
monitor.on('event', (event) => {
  console.log('Event:', event.type, event.data);
});

// Start monitoring
await monitor.start();

// Stop monitoring
monitor.stop();
```

---

## ⭕ CommandRouter

Abstract base class for routing commands to appropriate execution channels (bot chat, RCON, or log monitoring).

### Overview

CommandRouter implements intelligent command routing:
- `/data get` commands → RCON (structured NBT responses)
- `/execute` with run data → RCON (structured queries)
- `useRcon` option → RCON (forced routing)
- `expectLogResponse` option → Log monitoring (event correlation)
- Default → Bot chat (player commands)

### Channels

```javascript
const { CommandRouter } = require('@pilaf/backends');

// Available channels
CommandRouter.CHANNELS.BOT     // Send via bot.chat()
CommandRouter.CHANNELS.RCON    // Send via RCON
CommandRouter.CHANNELS.LOG     // Send via bot and wait for log response
```

### Example Implementation

```javascript
const { CommandRouter } = require('@pilaf/backends');

class SmartCommandRouter extends CommandRouter {
  route(command, context) {
    const { options } = context;

    // Check forced options first
    if (options?.useRcon) {
      return { channel: CommandRouter.CHANNELS.RCON, options };
    }
    if (options?.expectLogResponse) {
      return { channel: CommandRouter.CHANNELS.LOG, options };
    }

    // Check custom rules
    const rules = this.getRules();
    for (const { pattern, channel } of rules) {
      if (this._matchesPattern(command, pattern)) {
        return { channel, options };
      }
    }

    // Default: bot chat
    return { channel: CommandRouter.CHANNELS.BOT, options };
  }
}
```

### Usage Example

```javascript
const router = new SmartCommandRouter();

// Add custom routing rule
router.addRule(/^\/data get/, CommandRouter.CHANNELS.RCON);

// Route command
const result = router.route('/data get entity TestPlayer', { options: {} });
console.log(result.channel);  // 'rcon'
```

---

## 🔁 CircularBuffer

Fixed-size circular buffer for memory-efficient event storage with automatic overflow handling.

### Overview

CircularBuffer provides O(1) operations and prevents memory leaks in long-running tests by limiting stored events.

### API Reference

```javascript
const { CircularBuffer } = require('@pilaf/backends');

const buffer = new CircularBuffer({
  size: 1000,           // Maximum number of events
  onOverflow: 'discard'  // 'discard' or 'error'
});

// Add event
buffer.push(event);

// Get all events
const events = buffer.getAll();

// Get buffer size
buffer.size;        // Current event count
buffer.maxSize;     // Maximum capacity

// Check if full
buffer.isFull();

// Clear buffer
buffer.clear();

// Iterate over events
buffer.forEach((event, index) => {
  console.log(`Event ${index}:`, event.type);
});
```

### Usage Example with LogMonitor

```javascript
const { LogMonitor } = require('@pilaf/backends');

const monitor = new LogMonitor({
  bufferSize: 500  // Store last 500 events
});

monitor.on('event', (event) => {
  // Events are automatically buffered
});

// Retrieve recent events
const recentEvents = monitor.getEvents();
```

---

## 🚀 Enhanced MineflayerBackend

The MineflayerBackend now includes integrated QueryHelper and EventObserver with **lazy initialization**.

### New Features

1. **Query Methods** (delegates to QueryHelper)
   - `listPlayers()`
   - `getPlayerInfo(username)`
   - `getWorldTime()`
   - `getWeather()`
   - `getDifficulty()`
   - `getGameMode()`
   - `getTPS()`
   - `getSeed()`

2. **Event Methods** (delegates to EventObserver, lazy-loaded)
   - `observe()` - Starts log monitoring
   - `unobserve()` - Stops log monitoring
   - `isObserving()` - Check if observing
   - `onPlayerJoin(callback)`
   - `onPlayerLeave(callback)`
   - `onPlayerDeath(callback)`
   - `onPlayerChat(callback)`
   - `onCommand(callback)`
   - `onEvent(pattern, callback)`

3. **Lazy Initialization**
   - EventObserver is only created when `observe()` is called
   - LogMonitor and parser are created on-demand
   - Zero overhead if events are not used

### Usage Example

```javascript
const { MineflayerBackend } = require('@pilaf/backends');

const backend = new MineflayerBackend();

// Connect with RCON integration
await backend.connect({
  host: 'localhost',
  port: 25565,
  auth: 'offline',
  rconPort: 25575,
  rconPassword: 'password'
});

// Query server state
const players = await backend.listPlayers();
console.log('Online players:', players.players);

const tps = await backend.getTPS();
console.log('Server TPS:', tps.tps);

// Start event observation (lazy initialization)
await backend.observe();

// Subscribe to events
backend.onPlayerJoin((event) => {
  console.log('Player joined:', event.data.player);
});

backend.onPlayerDeath((event) => {
  console.log('Player died:', event.data.cause);
});

// Trigger command
await backend.chat('/test command');

// Clean up
backend.unobserve();
await backend.disconnect();
```

### Complete Test Example

```javascript
describe('Elemental Dragon Tests', () => {
  let backend;

  beforeEach(async () => {
    backend = new MineflayerBackend();
    await backend.connect({
      host: 'localhost',
      port: 25565,
      username: `TestBot_${Date.now()}`,
      auth: 'offline',
      rconPort: 25575,
      rconPassword: 'test'
    });
    await backend.observe();
  });

  afterEach(async () => {
    backend.unobserve();
    await backend.disconnect();
  });

  it('should spawn dragon and verify TPS', async () => {
    // Spawn dragon
    await backend.chat('/summon elemental_dragon 100 70 200');

    // Wait for spawn
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Verify server health
    const { tps } = await backend.getTPS();
    expect(tps.tps).toBeGreaterThan(15);
  });

  it('should track player join events', async () => {
    const joins = [];
    backend.onPlayerJoin((event) => {
      joins.push(event.data.player);
    });

    // Trigger join
    await backend.chat('/test join Player123');

    await new Promise(resolve => setTimeout(resolve, 500));

    expect(joins).toContain('Player123');
  });
});
```

---

## 🔧 Core Components

### ConnectionState

Connection states for backend lifecycle management:

```javascript
const { ConnectionState } = require('@pilaf/backends');

ConnectionState.DISCONNECTED     // Initial state
ConnectionState.CONNECTING      // Connecting to server
ConnectionState.CONNECTED       // Connection established
ConnectionState.SPAWNING        // Player spawning
ConnectionState.SPAWNED         // Player ready
ConnectionState.ERROR           // Error occurred
ConnectionState.DISCONNECTING   // Disconnecting
```

### BotPool

Manages a pool of Mineflayer bot instances for parallel testing.

```javascript
const { BotPool } = require('@pilaf/backends');

const pool = new BotPool({
  maxBots: 5,
  defaultConfig: {
    host: 'localhost',
    port: 25565,
    auth: 'offline'
  }
});

// Acquire a bot
const bot = await pool.acquire('Bot1');

// Use bot
await bot.chat('Hello!');

// Release when done
await pool.release('Bot1');

// Shutdown all
await pool.shutdownAll();
```

### ServerHealthChecker

Monitors server health and availability.

```javascript
const { ServerHealthChecker } = require('@pilaf/backends');

const checker = new ServerHealthChecker({
  rcon: { host: 'localhost', port: 25575, password: 'pass' }
});

// Check health
const isHealthy = await checker.check();
console.log(isHealthy.status); // 'healthy' | 'unhealthy'
```

---

## 🧪 Testing Patterns

### Pattern 1: Server State Verification

```javascript
it('should verify server state after action', async () => {
  await backend.chat('/time set 12000');

  const { time } = await backend.getWorldTime();
  expect(time.time).toBe(12000);
});
```

### Pattern 2: Event Correlation

```javascript
it('should track multi-step scenario', async () => {
  const questEvents = [];

  monitor.on('correlation', (session) => {
    if (session.questId === 'test-quest') {
      questEvents.push(...session.events);
    }
  });

  await backend.chat('/quest start test-quest');
  await backend.chat('/quest complete test-quest');

  await new Promise(resolve => setTimeout(resolve, 1000));

  expect(questEvents).toHaveLength(2);
});
```

### Pattern 3: Docker Integration

```javascript
describe('Docker Tests', () => {
  let monitor;

  beforeEach(async () => {
    monitor = new LogMonitor({
      collector: new DockerLogCollector({
        container: 'mc-server',
        follow: true
      }),
      parser: new MinecraftLogParser()
    });
    await monitor.start();
  });

  afterEach(async () => {
    await monitor.stop();
  });
});
```

### Pattern 4: Async Event Handling

```javascript
it('should handle async events', async () => {
  let eventReceived = false;

  backend.onPlayerJoin(() => {
    eventReceived = true;
  });

  await backend.chat('/test trigger');

  await waitFor(() => eventReceived, 5000);
  expect(eventReceived).toBe(true);
});

function waitFor(condition, timeout) {
  return new Promise((resolve) => {
    const startTime = Date.now();
    const check = () => {
      if (condition() || Date.now() - startTime > timeout) {
        resolve(condition());
      } else {
        setTimeout(check, 100);
      }
    };
    check();
  });
}
```

---

## 📚 API Reference

### Exports

```javascript
const {
  // Backends
  RconBackend,
  MineflayerBackend,
  PilafBackendFactory,

  // Core
  ConnectionState,
  CommandRouter,

  // Helpers
  QueryHelper,
  EventObserver,

  // Monitoring
  LogMonitor,
  DockerLogCollector,

  // Parsing
  LogParser,
  MinecraftLogParser,
  PatternRegistry,

  // Correlation
  CorrelationStrategy,
  UsernameCorrelationStrategy,
  TagCorrelationStrategy,

  // Utilities
  BotPool,
  ServerHealthChecker,
  CircularBuffer
} = require('@pilaf/backends');
```

---

## 🐛 Troubleshooting

### Issue: Events not being captured

**Cause**: Not observing before triggering events

**Solution**:
```javascript
// WRONG
await backend.chat('/test');
await backend.observe();

// CORRECT
await backend.observe();
backend.onPlayerJoin(handler);
await backend.chat('/test');
```

### Issue: Tests timing out

**Cause**: Not waiting for async operations

**Solution**:
```javascript
it('test with timeout', async () => {
  await backend.chat('/command');
  await new Promise(resolve => setTimeout(resolve, 1000));
  // Then verify
}, 10000);
```

### Issue: Docker connection fails

**Cause**: Docker socket not accessible

**Solution**:
```javascript
const collector = new DockerLogCollector({
  dockerodeOptions: {
    socketPath: '/var/run/docker.sock'  // Default path
  }
});
```

### Issue: Pattern not matching

**Cause**: Case sensitivity or priority ordering

**Solution**:
```javascript
const registry = new PatternRegistry({ caseInsensitive: true });
registry.addPattern('test', /test/i, handler);  // Case insensitive
registry.addPattern('test', /^test/, handler, 1);  // High priority
```

---

## 📋 Migration Checklist

### From Manual Testing to Pilaf

- [ ] Install `@pilaf/backends` package
- [ ] Configure test environment (Jest, Pilaf reporter)
- [ ] Set up test server (local or Docker)
- [ ] Write first test (start with simple query)
- [ ] Add event observation
- [ ] Implement correlation for complex scenarios
- [ ] Set up CI/CD integration
- [ ] Train team on Pilaf usage

### From Raw RCON to QueryHelper

- [ ] Replace manual RCON string parsing with QueryHelper
- [ ] Use structured responses in assertions
- [ ] Remove regex parsing code
- [ ] Test with actual server responses

### From Manual Log Parsing to EventObserver

- [ ] Replace log monitoring code with EventObserver
- [ ] Use convenience methods (onPlayerJoin, etc.)
- [ ] Add wildcard patterns for custom events
- [ ] Implement correlation strategies
- [ ] Remove manual parsing code

---

## 🎯 Best Practices

### 1. Test Isolation

```javascript
beforeEach(async () => {
  backend = new MineflayerBackend();
  await backend.connect({
    username: `TestBot_${Date.now()}`,  // Unique username
    auth: 'offline'
  });
});

afterEach(async () => {
  await backend.disconnect();
});
```

### 2. Event Observation Lifecycle

```javascript
it('test events', async () => {
  await backend.observe();     // Start FIRST
  backend.onPlayerJoin(() => { });
  await backend.chat('/test');
  await new Promise(resolve => setTimeout(resolve, 500));
  backend.unobserve();   // Stop LAST
});
```

### 3. Use Correlation for Multi-Step Tests

```javascript
monitor.on('correlation', (session) => {
  // All events for this player/session
  expect(session.events).toHaveLength(expectedCount);
});
```

### 4. Docker for CI/CD

```yaml
# docker-compose.test.yml
version: '3'
services:
  minecraft-server:
    image: pilaf/minecraft-test-server
    ports:
      - "25565:25565"
      - "25575:25575"
    environment:
      - RCON_PASSWORD=test
      - OPS=TestBot
```

### 5. Handle Async Timing

```javascript
// Always wait for server response
await new Promise(resolve => setTimeout(resolve, 500));

// For longer operations
await waitFor(() => condition(), 5000);
```

---

## 📖 Additional Documentation

- **Architecture**: See `docs/plans/2025-01-16-pilaf-js-design.md` for detailed architecture
- **Examples**: Check `lib/helpers/*.pilaf.test.js` for reference implementations
- **Integration Guide**: See `docs/proposals/elementaldragon-pilaf-integration-guide.md` for ElementalDragon-specific usage

---

## 📄 License

MIT
