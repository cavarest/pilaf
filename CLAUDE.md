# CLAUDE.md

## Build Commands

```bash
pnpm install              # Install dependencies
pnpm test                 # Run all tests
pnpm -r build            # Build all packages
pilaf test              # Run Pilaf tests
```

## Project Overview

Pilaf is a **pure JavaScript** testing framework for Minecraft PaperMC plugin development. It orchestrates tests across Mineflayer clients (player simulation) and RCON (server commands), generating interactive Vue.js-based HTML reports.

**Key Goal**: Replace complex Java integration tests with simple, readable JavaScript tests.

## Architecture

```
Jest Tests → Pilaf Reporter → Backend Layer → Minecraft Server
            (Data Capture)    (Mineflayer/RCON)    (PaperMC)
```

### Core Packages

- **@pilaf/cli**: Command-line interface using commander
- **@pilaf/framework**: Jest integration with custom reporters and helpers
- **@pilaf/backends**: RCON and Mineflayer backend implementations
- **@pilaf/reporting**: Vue.js-based HTML report generation

## Data Model

See [docs/plans/2025-01-16-pilaf-js-design.md](docs/plans/2025-01-16-pilaf-js-design.md) for complete architecture.

## Development

### Running Tests

```bash
# Unit tests
pnpm --filter @pilaf/framework test

# Integration tests (requires Minecraft server)
pilaf test tests/**/*.pilaf.test.js
```

### Adding New Features

1. Add backend method in `packages/backends/lib/`
2. Add helper in `packages/framework/lib/helpers/`
3. Update exports in `packages/framework/lib/index.js`
4. Write test in `tests/`
