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

## License

MIT
