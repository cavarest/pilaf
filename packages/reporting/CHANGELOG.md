# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.1] - 2025-01-27

### Fixed
- **Security**: Added pnpm override for axios >=1.8.2 to address CVE-2025-27152 and CVE-2023-45857
- **Compatibility**: Updated Node.js engine requirement from >=18.0.0 to >=22.0.0 to match mineflayer@4.34.0 and minecraft-protocol@1.63.0 requirements

## [1.3.0] - 2025-01-27

### Changed
- Improved HTML report rendering for new action types (entity combat, pathfinding)
- Enhanced display of navigate_to results with position information
- Better visualization of entity interaction results

## [1.1.0] - 2025-01-24

### Added
- Enhanced report templates with improved styling
- Better support for log monitoring events in reports
- Support for displaying correlated events

### Changed
- Updated report data structure to support new event types
- Improved HTML template rendering

### Fixed
- Fixed template rendering issues with special characters
- Better handling of large test suites

## [1.0.1] - 2025-01-20

### Fixed
- Updated README with report data structure documentation
- Fixed CSS issues in report template

## [1.0.0] - 2025-01-15

### Added
- Initial release of @pilaf/reporting
- ReportGenerator for creating HTML reports
- Vue.js-based interactive reports
- Custom template support
- CSS customization
