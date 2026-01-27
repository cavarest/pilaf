# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2025-01-27

### Added
- **CorrelationUtils** - RCON inventory correlation system
  - Tracks command-response pairs for inventory changes
  - Enables reliable inventory verification after RCON commands
- **EntityUtils** - Entity helper functions
  - Normalizes entity names for consistent lookups
  - Handles entity type aliases (boat → oak_boat)

### Fixed
- Entity spawning at exact player position using `execute at @p`
- Fixed Vec3 constructor usage (bot.entity.position.constructor)
- Fixed entity alias handling for proper entity lookups
- Fixed equipment slot handling with bot.getEquipmentDestSlot
- Fixed swap_inventory_slots using bot.moveSlotItem()
- Added empty slot tolerance to swap_inventory_slots

## [1.2.3] - 2025-01-27

### Added
- **mineflayer-pathfinder** (v2.4.5) plugin support for pathfinding navigation
  - Loaded automatically in MineflayerBackend.createBot()
  - Enables navigate_to action with GoalNear for flexible pathfinding

### Changed
- Upgraded mineflayer from 4.20.0 to 4.34.0 (latest stable)
  - Fixed blockUpdate event handling issues
  - Improved player simulation reliability

### Fixed
- Fixed pathfinding navigation with relative coordinates and offset support
- Fixed horse riding test by spawning tamed horses with saddles via NBT data

## [1.1.0] - 2025-01-24

### Added

#### Log Monitoring System
- **LogMonitor** - Central orchestrator for collection, parsing, correlation, and emission
  - Collects logs from any LogCollector implementation
  - Parses using any LogParser implementation
  - Correlates events using pluggable CorrelationStrategy
  - Emits typed events with correlated data
  - Configurable circular buffer for recent events (69 tests)

#### Log Parsing
- **MinecraftLogParser** - Comprehensive Minecraft server log parser
  - 112 built-in log patterns covering all major Minecraft events
  - Pattern management via PatternRegistry with priority-based ordering
  - Support for player events (join, leave, chat, death, advancement)
  - Entity tracking (spawn, despawn, damage)
  - Server events (start, stop, save, crash)
  - Command execution tracking
  - Game state events (time change, weather, difficulty)

- **PatternRegistry** - Centralized pattern management
  - Priority-based pattern matching (0-10, lower = higher priority)
  - Add/remove patterns at runtime
  - Clone pattern sets for isolated testing
  - Thread-safe pattern storage

#### Log Collection
- **DockerLogCollector** - Docker container log streaming with reconnection
  - Automatic reconnection with exponential backoff
  - Configurable reconnection delay and max attempts
  - ANSI color code stripping
  - Pause/resume support
  - Stream lifecycle events (connected, disconnected, error, end)

#### Event Correlation
- **CorrelationStrategy** - Abstract base for event correlation
- **UsernameCorrelationStrategy** - Groups events by player username
- **TagCorrelationStrategy** - Groups events by custom tags
- **CircularBuffer** - O(1) fixed-size event buffer

#### Command Routing
- **CommandRouter** - Abstract base for command routing
  - BOT, RCON, and LOG channel constants
  - Custom rule management with pattern matching
  - String prefix and regex pattern support

#### Query Helpers
- **QueryHelper** - Structured RCON query methods
  - `listPlayers()` - Get online player count and list
  - `getPlayerInfo(username)` - Get player position, health, dimension
  - `getWorldTime()` - Get current game time
  - `getWeather()` - Get current weather state
  - `getDifficulty()` - Get difficulty level
  - `getGameMode(username)` - Get player game mode
  - `getTPS()` - Get server ticks per second
  - `getSeed()` - Get world seed

#### Event Observation
- **EventObserver** - Declarative event subscription API
  - Pattern-based event matching
  - Wildcard event subscriptions
  - Convenience methods for common events
  - Automatic log monitoring setup
  - Start/stop observation control

### Changed
- Enhanced MineflayerBackend with lazy QueryHelper and EventObserver initialization
- Improved test context helper with better error messages
- Fixed DockerLogCollector reconnection bug (reconnectCount reset issue)

### Fixed
- Fixed DockerLogCollector reconnection logic to properly track attempts
- Fixed regex syntax error in QueryHelper player list parsing
- Fixed test issues with abstract class instantiation prevention
- Fixed CommandRouter option passing format in tests
- Fixed PatternRegistry cloning test expectations

## [1.0.1] - 2025-01-20

### Fixed
- Corrected import paths for backends (../../ not ../)
- Added repository.url to all package.json files
- Updated release workflow for trusted publishing

## [1.0.0] - 2025-01-15

### Added
- Initial release of @pilaf/backends
- MineflayerBackend for bot player connections
- RconBackend for server console access
- HealthChecker for connection monitoring
- Basic command execution via bot chat and RCON
