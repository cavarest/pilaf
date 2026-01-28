# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.1] - 2025-01-27

### Fixed
- **Security**: Added pnpm override for axios >=1.8.2 to address CVE-2025-27152 and CVE-2023-45857
- **Compatibility**: Updated Node.js engine requirement from >=18.0.0 to >=22.0.0 to match mineflayer@4.34.0 and minecraft-protocol@1.63.0 requirements

## [1.3.0] - 2025-01-27

### Added

#### Entity Combat Actions
- **attack_entity** - Attack an entity (deals damage)
  - Supports entity_name and entity_selector parameters
  - Returns attacked status and entity information
- **interact_with_entity** - Right-click entity interactions
  - Supports villagers (trade), animals, and other interactable entities
  - Returns interacted status and entity type
- **mount_entity** - Mount rideable entities
  - Supports horses, boats, and minecarts
  - Returns mounted status and entity type
- **dismount** - Dismount from currently mounted entity
  - Returns dismounted status

#### Advanced Navigation
- **navigate_to** - Pathfinding navigation using mineflayer-pathfinder
  - Uses GoalNear for flexible pathfinding (within 1 block of target)
  - Supports relative navigation via `offset` parameter
  - Returns reached status and final position
  - Configurable timeout via `timeout_ms` parameter

#### New Helper Utilities
- **CorrelationUtils** - RCON inventory correlation system
  - Tracks command-response pairs for inventory changes
  - Enables reliable inventory verification after RCON commands
- **EntityUtils** - Entity helper functions
  - Normalizes entity names for consistent lookups
  - Handles entity type aliases (boat → oak_boat)

### Changed

#### Crafting Improvements
- **craft_item** now supports `minecraft:` prefixed item names
  - Automatic normalization for mcData lookup
  - Consistent behavior regardless of namespace prefix

#### Block Interaction Enhancements
- **place_block** uses event-based confirmation with RCON verification
  - Listens for blockUpdate events from Mineflayer
  - Falls back to RCON command verification on timeout
- **interact_with_block** improved for container interactions

#### Movement Actions
- Look_at action supports both position and entity_name parameters
- Consistent position handling using Vec3 from bot instance

### Fixed

#### CI Stability
- Configured flat world for deterministic testing
  - Fixed SPAWN_MONSTERS for entity combat tests
  - Fixed SPAWN_ANIMALS for passive entity spawning
- Entity spawning at exact player position using `execute at @p`
- Fixed Vec3 constructor usage (bot.entity.position.constructor)
- Fixed entity alias handling (boat → oak_boat)

#### Mineflayer Compatibility
- Updated to mineflayer 4.34.0 for Minecraft 1.21.9/1.21.10 support
- Proper blockUpdate event handling with server version detection
- Fixed equipment slot handling with bot.getEquipmentDestSlot

#### Inventory Management
- Fixed swap_inventory_slots using bot.moveSlotItem()
- Added empty slot tolerance to swap_inventory_slots
- Fixed equipment verification using bot.heldItem

#### Test Reliability
- Added entity cleanup between tests using unique names
- Increased wait times for entity spawn loading
- Fixed horse spawning with NBT data for tamed horse with saddle

### Removed

- Removed debug logging from production code
- Removed duplicate test runner script

## [1.2.3] - 2025-01-27

### Added
- **mineflayer-pathfinder** (v2.4.5) dependency for navigate_to action

### Changed
- **navigate_to** action now supports relative navigation via `offset` parameter
  - Use `{x, y, z, offset: {x, y, z}}` for relative movement from stored position
  - Changed from GoalBlock to GoalNear for flexible pathfinding (within 1 block)
- **craft_item** action now supports `minecraft:` prefixed item names
  - Automatically normalizes item names for mcData lookup

### Fixed
- Fixed food consumption test - use RCON for hunger effect (bot lacks operator permissions)
- Fixed crafting test - item name normalization handles namespace prefix
- Fixed horse riding test - spawn tamed horse with saddle via NBT data
- Enabled pathfinding test (was previously skipped)

## [1.1.0] - 2025-01-24

### Added

#### Test Context Helper
- **createTestContext()** - Simplified bot player and RCON setup
  - Creates both MineflayerBackend and RconBackend in one call
  - Returns unified context object with bot, backend, and rcon properties
  - Includes comprehensive documentation on when to use RCON vs bot commands
  - Automatic cleanup with cleanupTestContext()
  - Better error messages for common configuration issues

#### Documentation
- Added justification for test context helper necessity
  - Explains why RCON is needed for commands that return responses
  - Documents the difference between bot.chat() and rcon.send()
  - Includes examples of when to use each approach

#### State Management
- **captureState()** - Deep clone any object for state snapshots
- **compareStates()** - Compare two states and detect changes
  - Returns hasChanges boolean and detailed changes array
  - Useful for verifying plugin state mutations

#### Event Helpers
- **waitForEvents()** - Wait for specific number of events
- **captureEvents()** - Capture events with automatic cleanup
  - Returns release() function to remove listeners
  - Supports multiple event types

### Changed
- Enhanced documentation with real-world testing patterns
- Improved error messages in test context helper
- Updated README with comprehensive examples

### Fixed
- Fixed prismarine-physics compatibility issues
- Resolved entity test failures
- Fixed RCON timing issues in tests

## [1.0.1] - 2025-01-20

### Fixed
- Updated README with comprehensive usage examples
- Fixed import paths in examples

## [1.0.0] - 2025-01-15

### Added
- Initial release of @pilaf/framework
- StoryRunner for declarative test story execution
- Jest integration with custom reporters
- HTML report generation
- Test helpers for state and events
- Custom Jest matchers for game testing
