# Pilaf Enhanced Backend Implementation Plan

## Purpose

This document outlines the comprehensive plan to enhance Pilaf's backend architecture with log monitoring capabilities, enabling event-based testing while maintaining backward compatibility with existing RCON-based structured queries.

## Architectural Overview

### Current State

Pilaf currently provides two independent backend implementations:

1. **MineflayerBackend** - Bot player simulation
   - Sends commands via `bot.chat()`
   - Returns empty responses (Minecraft protocol limitation)
   - Excellent for player actions, poor for server state verification

2. **RconBackend** - Remote console protocol
   - Sends commands via RCON protocol
   - Returns structured responses
   - Excellent for queries, but requires separate connection

### The Enhancement

Add a **three-tier observation architecture** to MineflayerBackend:

| Tier | Purpose | Channel | Example |
|------|---------|---------|---------|
| **Actions** | Send player commands | `bot.chat()` | Execute plugin commands |
| **Queries** | Inspect server state | RCON | `/data get entity Pos` |
| **Events** | Observe game events | Log Monitor | Detect teleportation, deaths |

### Key Design Principles

1. **Object-Oriented**: Abstract bases, concrete implementations, composition over inheritance
2. **MECE**: Mutually Exclusive, Collectively Exhaustive responsibilities
3. **DRY**: Don't Repeat Yourself - shared abstractions
4. **Dependency Inversion**: Depend on abstractions, inject for testability
5. **Backward Compatible**: Existing code works without changes

## Architecture Design

### Class Hierarchy

```
PilafBackend (existing abstract base)
├── MineflayerBackend (existing, to be enhanced)
├── RconBackend (existing, unchanged)
└── [NEW] LogCollector (abstract base)
    ├── DockerLogCollector
    ├── FileLogCollector
    └── SyslogCollector

[NEW] LogParser (abstract base)
└── MinecraftLogParser

[NEW] CommandRouter (abstract base)
└── SmartCommandRouter

[NEW] CorrelationStrategy (abstract base)
├── TagCorrelationStrategy
└── UsernameCorrelationStrategy

[NEW] LogMonitor (composite)
    ├── Uses: LogCollector
    ├── Uses: LogParser
    └── Uses: CorrelationStrategy
```

### File Structure

```
packages/backends/lib/
├── backend.js                    # Existing: PilafBackend base
├── factory.js                    # Existing: Backend factory
├── index.js                      # Existing: Public API
│
├── core/                         # NEW: Core abstractions
│   ├── LogCollector.js          # Abstract base for log collection
│   ├── LogParser.js             # Abstract base for log parsing
│   ├── CommandRouter.js         # Abstract base for command routing
│   ├── CorrelationStrategy.js   # Abstract base for response correlation
│   └── index.js                 # Core exports
│
├── collectors/                   # NEW: Log collector implementations
│   ├── DockerLogCollector.js    # Docker API-based collection
│   ├── FileLogCollector.js      # File tailing collection
│   ├── SyslogCollector.js       # Syslog protocol collection
│   └── index.js                 # Collector factory
│
├── parsers/                      # NEW: Log parser implementations
│   ├── MinecraftLogParser.js    # Minecraft-specific parser
│   ├── PatternRegistry.js       # Pattern registration/matching
│   └── index.js                 # Parser factory
│
├── strategies/                   # NEW: Routing/correlation strategies
│   ├── SmartCommandRouter.js    # Intelligent command routing
│   ├── TagCorrelationStrategy.js # Entity tag correlation
│   ├── UsernameCorrelationStrategy.js # Username-based correlation
│   └── index.js                 # Strategy factory
│
├── monitoring/                   # NEW: Log monitoring composites
│   ├── LogMonitor.js            # Main log monitor
│   └── index.js                 # Monitor factory
│
├── mineflayer-backend.js         # EXISTING: Enhance with routing
├── rcon-backend.js              # EXISTING: Unchanged
└── bot/                         # EXISTING: Bot management
    ├── BotPool.js
    ├── BotLifecycleManager.js
    └── ServerHealthChecker.js
```

## Component Specifications

### 1. Core Abstractions

#### LogCollector (Abstract Base)

**Purpose**: Define interface for collecting raw log data

**Interface**:
```javascript
class LogCollector extends EventEmitter {
  async connect(config)    // Connect to log source
  async disconnect()       // Disconnect and cleanup
  pause()                 // Pause log collection
  resume()                // Resume log collection

  // Events
  on('data', callback)    // Raw log line received
  on('error', callback)   // Collection error
  on('end', callback)     // Stream ended
}
```

**Implementations**:
- `DockerLogCollector` - Uses Dockerode API to tail container logs
- `FileLogCollector` - Uses fs.watch to tail log files
- `SyslogCollector` - Connects to syslog daemon via UDP/TCP

#### LogParser (Abstract Base)

**Purpose**: Define interface for parsing log lines into structured events

**Interface**:
```javascript
class LogParser {
  parse(line)  // Returns: { type, data, raw } or null

  // Pattern management
  addPattern(name, regex, handler)
  removePattern(name)
  getPatterns()
}
```

**Output Format**:
```javascript
{
  type: 'teleport',        // Event type
  data: {                  // Parsed data
    player: 'TestPlayer',
    position: { x: 100, y: 64, z: 100 }
  },
  raw: '[12:34:56] Teleported TestPlayer to 100.0, 64.0, 100.0'
}
```

#### CommandRouter (Abstract Base)

**Purpose**: Define interface for routing commands to appropriate channel

**Interface**:
```javascript
class CommandRouter {
  route(command, context)  // Returns: { channel, options }
}
```

**Routing Logic**:
1. Command starts with `/data get` → `rcon`
2. Command starts with `/execute` + `run data` → `rcon`
3. Options `useRcon: true` → `rcon`
4. Options `expectLogResponse: true` → `log`
5. Default → `bot`

#### CorrelationStrategy (Abstract Base)

**Purpose**: Define interface for matching command responses

**Interface**:
```javascript
class CorrelationStrategy {
  async correlate(command, eventStream, timeout)
  // Returns: matched response or throws CorrelationError
}
```

**Strategies**:
- `TagCorrelationStrategy` - Uses entity tags (most reliable)
- `UsernameCorrelationStrategy` - Uses username + timestamp window

### 2. Concrete Implementations

#### PatternRegistry

**Purpose**: Centralized pattern management for log parsing

**Features**:
- Compile regex patterns once (performance)
- Support for named capture groups
- Pattern priority ordering
- Custom pattern registration

**Pattern Categories** (MECE):

| Category | Patterns | Example |
|----------|----------|---------|
| Entity Events | joins, leaves, deaths, advancements | `Teleported TestPlayer to ...` |
| Movement Events | teleports, position changes, dimension | `Teleported.*to.*` |
| Command Events | issued, responses, results | `Set.*to.*` |
| World Events | time, weather, blocks, entities | `Set the time to.*` |
| Plugin Events | custom plugin formats | Dragon egg events |
| Server Status | started, stopping, saving | `Done.*For help.*` |

#### MinecraftLogParser

**Purpose**: Parse Minecraft-specific log formats

**Features**:
- Handles versions 1.19, 1.20, 1.21
- Timestamp extraction
- Log level parsing (INFO, WARN, ERROR)
- Multi-line output handling

#### LogMonitor (Composite)

**Purpose**: Combine collector + parser + correlation for complete log monitoring

**Features**:
- Circular buffer for correlation (max 1000 lines)
- Event emission for parsed logs
- Response correlation with timeout
- Event filtering by type

**Interface**:
```javascript
class LogMonitor extends EventEmitter {
  // Connection
  async connect(config)
  async disconnect()

  // Event observation
  onLogEvent(type, callback)
  onceLogEvent(type, callback)
  offLogEvent(type, callback)

  // Correlation
  expectResponse(commandId)
  waitForResponse(commandId, timeout = 5000)

  // State
  getEvents(type, since)
  clearBuffer()
}
```

#### Enhanced MineflayerBackend

**Purpose**: Unify bot control, RCON queries, and log observation

**New Methods**:
```javascript
// Command routing (options-based)
async sendCommand(command, options = {
  useRcon: false,
  expectLogResponse: false,
  timeout: 10000
})

// Query shortcuts (uses RCON)
async queryEntityData(username, nbtPath = 'Pos')
async queryBlockData(x, y, z)
async queryScoreboard(objective)

// Event observation
onLogEvent(eventType, callback)
onceLogEvent(eventType, callback)
offLogEvent(eventType, callback)

// State queries
async waitForEvent(eventType, filter, timeout)
```

**Configuration**:
```javascript
const backend = new MineflayerBackend();
await backend.connect({
  // Existing (Bot connection)
  host: 'localhost',
  port: 25565,
  auth: 'offline',

  // Existing (RCON for health checks)
  rconHost: 'localhost',
  rconPort: 25575,
  rconPassword: 'dragon123',

  // NEW: RCON for commands
  enableRconCommands: true,

  // NEW: Log monitoring
  enableLogMonitoring: true,
  logMonitoring: {
    type: 'docker',           // 'docker' | 'file' | 'syslog'
    containerName: 'minecraft',
    parser: 'minecraft',
    correlation: 'auto',
    eventTypes: ['all']
  }
});
```

## Implementation Phases

### Phase 1: Foundation (Week 1)

**Deliverables**:
- Core abstraction classes (LogCollector, LogParser, CommandRouter, CorrelationStrategy)
- PatternRegistry implementation
- Factory classes for all abstractions
- Complete unit test suite

**Acceptance Criteria**:
- All abstract bases have JSDoc documentation
- Unit tests achieve 90%+ coverage
- Factory pattern works for all abstractions
- No integration dependencies (pure unit tests)

### Phase 2: Docker Log Collector (Week 1-2)

**Deliverables**:
- DockerLogCollector implementation
- Docker API integration via Dockerode
- Connection management with reconnection
- Integration test suite

**Acceptance Criteria**:
- Can connect to running Docker container
- Receives log lines in real-time
- Handles container restart gracefully
- Reconnects after connection loss
- Integration test with real Minecraft container passes

### Phase 3: Minecraft Log Parser (Week 2)

**Deliverables**:
- MinecraftLogParser implementation
- All 6 pattern categories implemented
- Cross-version compatibility (1.19, 1.20, 1.21)
- Comprehensive fixture suite

**Acceptance Criteria**:
- Parses all 6 pattern categories
- Handles Minecraft versions 1.19, 1.20, 1.21
- Pattern registry allows custom patterns
- Returns null for unparseable lines
- Test suite covers 50+ real log lines

### Phase 4: Log Monitor Composite (Week 2-3)

**Deliverables**:
- LogMonitor implementation (collector + parser composition)
- Event emission system
- Correlation strategies (tag-based, username-based)
- Circular buffer implementation
- Integration tests

**Acceptance Criteria**:
- Emits events for all parsed patterns
- Correlation strategies work for single-bot case
- waitForResponse handles timeout correctly
- Circular buffer prevents memory leaks
- Integration test: command → log → correlation works

### Phase 5: Enhanced Backend (Week 3)

**Deliverables**:
- Enhanced MineflayerBackend with command routing
- Query helper methods
- Event observation API
- End-to-end integration tests
- Updated test context helper

**Acceptance Criteria**:
- Existing tests still pass (backward compatibility)
- sendCommand() with useRcon returns RCON response
- sendCommand() with expectLogResponse waits for log
- queryEntityData() returns parsed NBT data
- onLogEvent() receives real-time events
- End-to-end test: full workflow passes

### Phase 6: Documentation (Week 3-4)

**Deliverables**:
- Architecture documentation
- API reference for all new components
- Usage examples
- Migration guide
- Updated README

**Acceptance Criteria**:
- Every public method has JSDoc
- Every architectural decision documented
- Every feature has usage example
- Migration guide complete
- README updated

### Phase 7: Polish (Week 4)

**Deliverables**:
- Performance optimization
- Error handling refinement
- Additional test coverage
- Code review and polish
- Release notes

**Acceptance Criteria**:
- Performance benchmarks meet targets (1000 lines/sec, <50MB, <10ms latency)
- All error types have tests
- Linter passes
- Code review approved
- Release notes written

## Configuration Examples

### Minimal Setup (Backward Compatible)

```javascript
const { MineflayerBackend } = require('@pilaf/backends');

const backend = new MineflayerBackend();
await backend.connect({
  host: 'localhost',
  port: 25565,
  auth: 'offline'
});

const bot = await backend.createBot({ username: 'TestPlayer' });

// Works exactly as before
bot.chat('/mycommand');
```

### RCON Commands (New)

```javascript
await backend.connect({
  host: 'localhost',
  port: 25565,
  rconPassword: 'dragon123',
  enableRconCommands: true
});

// NEW: Query shortcuts
const pos = await backend.queryEntityData('TestPlayer', 'Pos');
console.log(pos); // { x: 100.5, y: 64.0, z: 100.5 }

// NEW: Command routing
const result = await backend.sendCommand('data get entity TestPlayer Pos', {
  useRcon: true
});
```

### Log Monitoring (New)

```javascript
await backend.connect({
  host: 'localhost',
  port: 25565,
  enableLogMonitoring: true,
  logMonitoring: {
    type: 'docker',
    containerName: 'minecraft'
  }
});

// NEW: Event observation
backend.onLogEvent('teleport', (event) => {
  console.log(`${event.player} teleported to`, event.position);
});

// NEW: Wait for event
await backend.waitForEvent('teleport', { player: 'TestPlayer' }, 5000);
```

### Complete Setup (All Features)

```javascript
await backend.connect({
  host: 'localhost',
  port: 25565,
  rconPassword: 'dragon123',
  enableRconCommands: true,
  enableLogMonitoring: true,
  logMonitoring: {
    type: 'docker',
    containerName: 'minecraft',
    correlation: 'username',
    eventTypes: ['teleport', 'death', 'command']
  }
});

// All three tiers available:
bot.chat('/myplugin dash');                          // Action
const data = await backend.queryEntityData('...');    // Query
backend.onLogEvent('teleport', handler);              // Event
```

## Error Handling

### Error Hierarchy

```
PilafError (base)
├── ConnectionError
│   ├── RconConnectionError
│   ├── DockerConnectionError
│   └── FileAccessError
├── CommandExecutionError
│   ├── CommandTimeoutError
│   └── CommandRejectedError
├── ParseError
│   ├── MalformedLogError
│   └── UnknownPatternError
├── CorrelationError
│   ├── ResponseTimeoutError
│   └── AmbiguousMatchError
└── ResourceError
    ├── BufferOverflowError
    └── HandleExhaustedError
```

### Error Format

```javascript
{
  name: 'CommandTimeoutError',
  code: 'COMMAND_TIMEOUT',
  message: 'Command timed out after 5000ms',
  details: {
    command: '/data get entity TestPlayer Pos',
    timeout: 5000,
    bufferedEvents: [...]
  },
  cause: originalError
}
```

## Performance Considerations

### Optimization Strategies

1. **Lazy Regex Compilation**: Compile patterns once, reuse
2. **Circular Buffer**: Prevent unbounded memory growth
3. **Event Filtering**: Filter before emission
4. **Stream Processing**: Process logs as stream, not batch

### Performance Targets

- Parse throughput: 1000+ log lines/second
- Memory overhead: <50MB with log monitoring
- Event latency: <10ms from log to emission
- Correlation timeout: Configurable (default 5s)

## Backward Compatibility

### Compatibility Matrix

| Feature | Existing Code | New Code | Breaking? |
|---------|---------------|----------|-----------|
| bot.chat() | Works | Works | No |
| sendCommand() | Returns empty | Returns empty | No |
| sendCommand(opts) | N/A | Returns response | No |
| queryEntityData() | N/A | Works | No |
| onLogEvent() | N/A | Works | No |
| createTestContext() | Works | Works | No |

### Migration Path

No migration required! Existing code continues to work. New features are opt-in via configuration.

## Testing Strategy

### Test Structure

```
packages/backends/lib/
├── core/
│   ├── LogCollector.spec.js
│   ├── LogParser.spec.js
│   ├── CommandRouter.spec.js
│   └── CorrelationStrategy.spec.js
├── collectors/
│   ├── DockerLogCollector.spec.js
│   ├── FileLogCollector.spec.js
│   ├── SyslogCollector.spec.js
│   └── fixtures/
│       ├── docker-logs.txt
│       └── minecraft-latest.log
├── parsers/
│   ├── MinecraftLogParser.spec.js
│   ├── PatternRegistry.spec.js
│   └── fixtures/
│       ├── teleport-events.log
│       └── entity-data.log
├── strategies/
│   ├── SmartCommandRouter.spec.js
│   ├── TagCorrelationStrategy.spec.js
│   └── UsernameCorrelationStrategy.spec.js
└── monitoring/
    └── LogMonitor.spec.js
```

### Coverage Goals

- Unit tests: 90%+ coverage for all classes
- Integration tests: All composites tested
- End-to-end tests: Full workflow with real server
- Cross-version tests: Minecraft 1.19, 1.20, 1.21

## Dependencies

### New Dependencies

```json
{
  "dockerode": "^4.0.0",      // Docker API
  "chokidar": "^4.0.0"        // File watching
}
```

### Existing Dependencies (Reused)

- `rcon-client` - RCON protocol
- `mineflayer` - Bot implementation
- `eventemitter3` - Event emission

## Documentation Structure

```
docs/
├── architecture/
│   ├── backend-architecture.adoc
│   ├── log-monitoring-system.adoc
│   ├── command-routing-strategy.adoc
│   └── correlation-strategies.adoc
├── api/
│   ├── enhanced-backend-api.adoc
│   ├── log-collector-api.adoc
│   ├── log-parser-api.adoc
│   └── event-reference.adoc
├── guides/
│   ├── log-monitoring-setup.adoc
│   ├── writing-custom-parsers.adoc
│   ├── adding-event-listeners.adoc
│   ├── advanced-command-routing.adoc
│   └── troubleshooting.adoc
└── examples/
    ├── basic-log-monitoring.js
    ├── event-based-testing.js
    ├── custom-pattern-matching.js
    └── multi-backend-orchestration.js
```

## Success Criteria

### Functional Requirements

- ✅ Can monitor Minecraft server logs in real-time
- ✅ Can parse all standard Minecraft log patterns
- ✅ Can correlate commands with log responses
- ✅ Can query server state via RCON shortcuts
- ✅ Can observe game events via event listeners
- ✅ Existing tests continue to pass

### Non-Functional Requirements

- ✅ No breaking changes to existing API
- ✅ Performance targets met (throughput, memory, latency)
- ✅ 90%+ test coverage
- ✅ Complete documentation
- ✅ Clean architecture (OOP, MECE, DRY)

## Timeline

- **Week 1**: Foundation + Docker Collector
- **Week 2**: Parser + Log Monitor
- **Week 3**: Enhanced Backend + Documentation
- **Week 4**: Polish + Review

**Total: 4 weeks**
