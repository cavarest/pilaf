# Pilaf Examples

This directory contains example Pilaf tests demonstrating common testing patterns.

## Running the Examples

Make sure you have:
- A running PaperMC server with RCON enabled
- Environment variables set (RCON_HOST, RCON_PORT, RCON_PASSWORD, MC_HOST, MC_PORT)

### Run Examples

```bash
# Run all examples (console output only)
pnpm test:examples

# Generate HTML report from examples
pnpm test:report

# Run specific example file
pnpm test examples/basic-rcon.example.pilaf.test.js

# Run with verbose output
jest examples/ --verbose
```

### Generate HTML Report

After running `pnpm test:report`, open the generated report:

```bash
# macOS
open target/pilaf-reports/index.html

# Linux
xdg-open target/pilaf-reports/index.html

# Windows
start target/pilaf-reports/index.html
```

The HTML report shows:
- Test suite overview with pass/fail counts
- Individual test results with step breakdown
- Execution time for each test
- Visual indicators for failed tests

## Example Files

### basic-rcon.example.pilaf.test.js
Demonstrates basic RCON command execution:
- Getting server version
- Listing players
- Querying time and difficulty
- Changing server settings

### player-interaction.example.pilaf.test.js
Demonstrates player testing:
- Making players operators
- Sending chat messages
- Executing player commands
- Testing player movement
- Tracking position changes

### entity-interaction.example.pilaf.test.js
Demonstrates entity testing:
- Spawning entities
- Querying entity lists
- Verifying entity existence
- Complete entity lifecycle (spawn → kill)

### inventory-testing.example.pilaf.test.js
Demonstrates inventory testing:
- Giving items to players
- Checking player inventory
- Verifying item presence
- Multiple item operations

### movement-control.example.pilaf.test.js
Demonstrates advanced movement controls:
- Directional movement (backward, left, right)
- Jumping mechanics
- Sneaking and sprinting toggles
- View orientation (look_at)
- Complex movement sequences

### entity-combat.example.pilaf.test.js
Demonstrates entity combat and interaction:
- Attacking entities with weapons
- Right-click entity interactions (villagers, animals)
- Mounting rideable entities (boats, horses, minecarts)
- Dismounting from entities
- Multi-entity interaction workflows

### block-interaction.example.pilaf.test.js
Demonstrates block manipulation:
- Breaking blocks at specific locations
- Placing blocks with face orientation
- Interacting with blocks (chests, doors, buttons)
- Building simple structures
- Block replacement workflows

### inventory-management.example.pilaf.test.js
Demonstrates inventory operations:
- Dropping items from inventory
- Consuming food items
- Equipping items to different slots (hand, head, torso)
- Swapping inventory slots
- Complete inventory management workflows

### advanced-features.example.pilaf.test.js
Demonstrates complex player actions:
- Pathfinding navigation (navigate_to)
- Container interactions (open_container, take_from_container, put_in_container)
- Crafting items (craft_item)
- Navigation around obstacles
- Multi-container workflows
- Advanced crafting sequences

## Key Concepts Demonstrated

### Story Structure
Each test uses a story object with:
- `name` - Test name
- `description` - What the test does
- `setup` - Server and player configuration
- `steps` - Array of test actions
- `teardown` - Cleanup configuration

### Variable Storage
Store step results for later use:
```javascript
{
  action: 'get_player_location',
  player: 'tester',
  store_as: 'position'  // Result stored here
}
```

### Variable References
Reference stored values with `{variableName}`:
```javascript
{
  action: 'calculate_distance',
  from: '{start_position}',  // References stored value
  to: '{end_position}'
}
```

### Assertions
Validate test outcomes:
```javascript
{
  action: 'assert',
  condition: 'entity_exists',
  expected: 'zombie',
  actual: '{entities}'
}
```

## Common Patterns

### Setup with Players
```javascript
setup: {
  server: { type: 'paper', version: '1.21.8' },
  players: [
    { name: 'Test Player', username: 'tester' }
  ]
}
```

### Execute Server Commands
```javascript
{
  action: 'execute_command',
  command: 'gamemode creative tester'
}
```

### Execute Player Commands
```javascript
{
  action: 'execute_player_command',
  player: 'tester',
  command: '/plugin command'
}
```

### Wait for Processing
```javascript
{
  action: 'wait',
  duration: 2  // seconds
}
```

## Best Practices

1. **Use descriptive names** - Story and step names should clearly describe what's being tested
2. **Clean up resources** - Use `stop_server: false` for shared server tests
3. **Add appropriate waits** - Server operations need time to process
4. **Store intermediate results** - Use `store_as` to capture data for assertions
5. **Group related tests** - Use `describe` blocks to organize tests logically

## Next Steps

- See [Getting Started Guide](../docs/getting-started/quick-start.html)
- See [Actions Reference](../docs/guides/actions-reference.html)
- See [Writing Tests Guide](../docs/guides/writing-tests.html)
