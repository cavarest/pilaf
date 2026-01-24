# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
