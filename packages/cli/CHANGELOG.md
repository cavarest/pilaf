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
- Support for new entity combat and pathfinding actions
- Enhanced test output for navigate_to and entity interaction actions

## [1.1.0] - 2025-01-24

### Added
- Support for new Pilaf framework features
- Improved configuration options for log monitoring
- Better error messages for missing configuration

### Changed
- Updated documentation to reflect @pilaf/backends v2.0.0 changes
- Improved help text and examples

### Fixed
- Fixed configuration loading issues
- Better handling of missing environment variables

## [1.0.1] - 2025-01-20

### Fixed
- Updated README with configuration examples
- Fixed verbose mode output

## [1.0.0] - 2025-01-15

### Added
- Initial release of @pilaf/cli
- `pilaf test` command for running Pilaf tests
- `pilaf report` command for generating HTML reports
- `pilaf init` command for initializing Pilaf configuration
- Configuration file support (pilaf.config.js)
- Environment variable support
- Verbose mode for debugging
