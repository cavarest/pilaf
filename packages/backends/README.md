# @pilaf/backends

Backend implementations for Pilaf testing framework.

This package provides RCON and Mineflayer backends for connecting to Minecraft PaperMC servers during testing.

## Installation

```bash
pnpm add @pilaf/backends
```

## Backends

### RCONBackend

Connects via RCON protocol to execute server commands.

```javascript
const { RconBackend } = require('@pilaf/backends');

const backend = new RconBackend().connect({
  host: 'localhost',
  port: 25575,
  password: 'your_password'
});

// Execute command
const response = await backend.send('op player1');
```

### MineflayerBackend

Creates a real Minecraft player using Mineflayer for realistic player simulation.

```javascript
const { MineflayerBackend } = require('@pilaf/backends');

const backend = new MineflayerBackend().connect({
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
await bot.chat('Hello world!');
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
  host: 'localhost',
  port: 25565,
  username: 'TestBot',
  auth: 'offline'
});
```

## API

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

Manages a pool of Mineflayer bot instances.

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

## QueryHelper

Convenience methods for common RCON queries with structured response parsing.

```javascript
const { QueryHelper } = require('@pilaf/backends');

const helper = new QueryHelper(rconBackend);

// List players
const players = await helper.listPlayers();
// { online: 2, players: ['Steve', 'Alex'] }

// Get world time
const time = await helper.getWorldTime();
// { time: 1500, daytime: true }

// Get weather
const weather = await helper.getWeather();
// { weather: 'clear' }

// Additional methods: getDifficulty(), getGameMode(), getTPS(), getSeed()
```

## EventObserver

Clean API for subscribing to Minecraft server events with pattern matching.

```javascript
const { EventObserver } = require('@pilaf/backends');

const observer = new EventObserver({ logMonitor, parser });

// Subscribe to player joins
const unsubscribe = observer.onPlayerJoin((event) => {
  console.log('Player joined:', event.data.player);
});

// Subscribe with wildcard pattern
observer.onEvent('entity.death.*', (event) => {
  console.log('Player died:', event.type);
});

// Start observing
await observer.start();

// Stop when done
unsubscribe();
observer.stop();
```

## LogMonitor

Orchestrates log collection, parsing, and correlation with real-time event emission.

```javascript
const { LogMonitor, DockerLogCollector, MinecraftLogParser, UsernameCorrelationStrategy } = require('@pilaf/backends');

const monitor = new LogMonitor({
  collector: new DockerLogCollector({ container: 'mc-server', follow: true }),
  parser: new MinecraftLogParser(),
  correlation: new UsernameCorrelationStrategy(),
  bufferSize: 1000
});

// Subscribe to events
monitor.on('event', (event) => {
  console.log('Event:', event.type, event.data);
});

monitor.on('correlation', (session) => {
  console.log('Player session:', session.username, session.events.length);
});

// Start monitoring
await monitor.start();
```

## Correlation Strategies

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

console.log(session.username); // 'Steve'
console.log(session.isActive); // true

// Session ends on leave event
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
  tagExtractor: (event) => event.data.commandId,
  timeout: 5000  // 5 seconds
});

// Correlates request/response events
strategy.correlate({ type: 'command.issued', data: { commandId: 'cmd-1' } });
strategy.correlate({ type: 'command.success', data: { commandId: 'cmd-1' } });

// Events are grouped by commandId and auto-expire after timeout
```

## Enhanced MineflayerBackend

The MineflayerBackend now includes integrated query and event observation features.

```javascript
const { MineflayerBackend } = require('@pilaf/backends');

const backend = new MineflayerBackend();
await backend.connect({
  host: 'localhost',
  port: 25565,
  auth: 'offline',
  rconPort: 25575,
  rconPassword: 'password'
});

// Query methods (delegates to QueryHelper)
const players = await backend.listPlayers();
const time = await backend.getWorldTime();
const tps = await backend.getTPS();

// Event observation (lazy initialization)
backend.onPlayerJoin((event) => {
  console.log('Player joined:', event.data.player);
});

backend.onPlayerDeath((event) => {
  console.log('Player died:', event.data.cause);
});

// Start observing
await backend.observe();

// Check if observing
console.log(backend.isObserving()); // true

// Stop observing
backend.unobserve();
```

## License

MIT
