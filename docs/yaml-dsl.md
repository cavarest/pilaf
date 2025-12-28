# YAML DSL Guide for DragonEggLightning Testing

## Overview

The YAML Domain-Specific Language (DSL) allows non-programmers to write test scenarios for the DragonEggLightning plugin using simple, player-friendly Minecraft commands. This guide explains how to create and run YAML test stories locally.

## Quick Start

### Running YAML Tests Locally

```bash
# Build the plugin and start tests
mvn clean test -Dtest=*YamlDsl*

# Run specific YAML story
mvn test -Dtest=YamlDslTest#testRunPluginVersionStory

# Run all YAML stories
mvn test -Dtest=YamlDslTest#testRunAllYamlStories
```

### YAML Test Stories Location

YAML test stories are stored in: `src/test/resources/test-stories/`

## YAML Story Structure

Each YAML story follows this structure:

```yaml
story:
  name: "Story Name"
  description: "What this test does"

setup:
  server:
    type: "PAPER"
    version: "1.21.8"
    online_mode: false

  players:
    - name: "PlayerName"
      op: true
      position: [x, y, z]
      items:
        - "item_name count"

steps:
  - name: "Step Description"
    action: "action_type"
    # action-specific parameters

cleanup:
  action: "cleanup_action"

expected_results:
  result_key: expected_value
```

## Available Actions

### Server Management

#### `start_server`
Start the Minecraft server with the plugin.
```yaml
- name: "Start Server"
  action: "start_server"
  timeout: 300  # seconds
```

#### `stop_server`
Stop the server after testing.
```yaml
- name: "Stop Server"
  action: "stop_server"
```

### Player Management

#### `connect_player`
Connect a player to the server (simulated).
```yaml
- name: "Connect Test Player"
  action: "connect_player"
  player: "TestPlayer"
```

#### `disconnect_player`
Disconnect a player from the server.
```yaml
- name: "Disconnect Player"
  action: "disconnect_player"
  player: "TestPlayer"
```

#### `get_position`
Get player's current position.
```yaml
- name: "Check Player Position"
  action: "get_position"
  player: "TestPlayer"
  expected_contains: "0.0, 64.0, 0.0"
```

#### `move_player`
Move player to specific coordinates.
```yaml
- name: "Move Player"
  action: "move_player"
  player: "TestPlayer"
  position: [100, 64, -50]
```

### Inventory Management

#### `check_inventory`
Check what's in player's inventory.
```yaml
- name: "Check Inventory"
  action: "check_inventory"
  player: "TestPlayer"
  expected_contains: "dragon_egg"
```

#### `give_item`
Give item to player.
```yaml
- name: "Give Dragon Egg"
  action: "give_item"
  player: "TestPlayer"
  item: "dragon_egg"
  count: 5
```

#### `equip_item`
Equip item to player's hand or armor slot.
```yaml
- name: "Equip Sword"
  action: "equip_item"
  player: "TestPlayer"
  item: "diamond_sword"
  slot: "hand"
```

### Entity Management

#### `spawn_entity`
Spawn an entity in the world.
```yaml
- name: "Spawn Zombie"
  action: "spawn_entity"
  entity_type: "zombie"
  name: "TestZombie"
  position: [5, 64, 5]
```

#### `get_entity_info`
Get information about a named entity.
```yaml
- name: "Check Zombie Health"
  action: "get_entity_info"
  entity_name: "TestZombie"
```

#### `attack_entity`
Attack a specific entity.
```yaml
- name: "Attack Zombie"
  action: "attack_entity"
  player: "TestPlayer"
  entity_name: "TestZombie"
```

### Plugin and Command Testing

#### `check_plugin`
Verify plugin is loaded.
```yaml
- name: "Check Plugin Loaded"
  action: "check_plugin"
  plugin: "DragonEggLightning"
```

#### `player_command`
Execute command as a player (simulated).
```yaml
- name: "Check Plugin Version"
  action: "player_command"
  player: "TestPlayer"
  command: "ability version"
  expected_response_contains: "1.0.2"
```

#### `console_command`
Execute command from console.
```yaml
- name: "Check Plugins List"
  action: "console_command"
  command: "plugins"
  expected_response_contains: "DragonEggLightning"
```

#### `get_help`
Get help/command list for player.
```yaml
- name: "Check Available Commands"
  action: "get_help"
  player: "TestPlayer"
  expected_contains: "ability"
```

### World Interaction

#### `dig_block`
Mine a block at coordinates.
```yaml
- name: "Mine Block"
  action: "dig_block"
  player: "TestPlayer"
  position: [10, 64, 10]
```

#### `place_block`
Place a block at coordinates.
```yaml
- name: "Place Block"
  action: "place_block"
  player: "TestPlayer"
  block_type: "stone"
  position: [11, 64, 10]
```

### Waiting and Timing

#### `wait`
Wait for specified duration.
```yaml
- name: "Wait for Lightning"
  action: "wait"
  duration: 5  # seconds
```

## Example Test Stories

### Plugin Version Test
```yaml
story:
  name: "Plugin Version Test"
  description: "Verify plugin loads with correct version"

setup:
  server:
    type: "PAPER"
    version: "1.21.8"
    online_mode: false

  players:
    - name: "TestPlayer"
      op: true
      position: [0, 64, 0]
      items:
        - "dragon_egg 1"

steps:
  - name: "Start Server"
    action: "start_server"
    timeout: 300

  - name: "Connect Player"
    action: "connect_player"
    player: "TestPlayer"

  - name: "Check Plugin Loaded"
    action: "check_plugin"
    plugin: "DragonEggLightning"

  - name: "Check Version"
    action: "player_command"
    player: "TestPlayer"
    command: "ability version"
    expected_response_contains: "1.0.2"

  - name: "Check Commands Available"
    action: "get_help"
    player: "TestPlayer"
    expected_contains: "ability"

cleanup:
  action: "disconnect_player"
  player: "TestPlayer"
  action: "stop_server"

expected_results:
  plugin_loads: true
  version_correct: true
  commands_registered: true
```

### Lightning Ability Test
```yaml
story:
  name: "Lightning Ability Test"
  description: "Test lightning ability works on entities"

setup:
  server:
    type: "PAPER"
    version: "1.21.8"
    online_mode: false

  players:
    - name: "LightningTester"
      op: true
      position: [0, 64, 0]
      items:
        - "dragon_egg 5"

steps:
  - name: "Start Server"
    action: "start_server"
    timeout: 300

  - name: "Connect Player"
    action: "connect_player"
    player: "LightningTester"

  - name: "Spawn Test Zombie"
    action: "spawn_entity"
    entity_type: "zombie"
    name: "TestZombie"
    position: [5, 64, 5]

  - name: "Check Initial Health"
    action: "get_entity_info"
    entity_name: "TestZombie"
    expected_contains: "Health: 20"

  - name: "Use Lightning Ability"
    action: "player_command"
    player: "LightningTester"
    command: "ability lightning TestZombie"
    expected_response_contains: "lightning"

  - name: "Wait for Strike"
    action: "wait"
    duration: 3

  - name: "Check Health After Lightning"
    action: "get_entity_info"
    entity_name: "TestZombie"
    expected_contains: "Health: 16"  # 2 hearts damage

cleanup:
  action: "disconnect_player"
  player: "LightningTester"
  action: "stop_server"

expected_results:
  lightning_works: true
  damage_applied: true
```

## Local Testing Workflow

### 1. Create Your Test Story
Create a new YAML file in `src/test/resources/test-stories/`:

```bash
# Example: my-custom-test.yaml
```

### 2. Write Your Test Scenario
Follow the structure above with:
- Setup (server and player configuration)
- Steps (test actions)
- Cleanup (stop server, disconnect players)
- Expected results

### 3. Run Your Test
```bash
# Run all YAML tests
mvn test -Dtest=*YamlDsl*

# Run specific test
mvn test -Dtest=YamlDslTest#testRunAllYamlStories
```

### 4. Check Results
Test results will be in:
- Console output during test execution
- `target/surefire-reports/` for detailed test reports

## Best Practices

1. **Use Descriptive Names**: Give your stories and steps clear, descriptive names
2. **Set Timeouts**: Always set reasonable timeouts for server operations
3. **Cleanup**: Always include cleanup steps to stop servers and disconnect players
4. **Expected Results**: Define clear expected results for validation
5. **Player-Friendly Commands**: Use commands that a normal Minecraft player would understand

## Troubleshooting

### Common Issues

1. **Server Won't Start**: Check that Paper JAR is available in `integration-test-server/`
2. **Plugin Not Found**: Verify plugin JAR is built with `mvn clean package`
3. **YAML Syntax Errors**: Use a YAML validator to check syntax
4. **Tests Timeout**: Increase timeout values for slow operations

### Debug Steps

1. Check server logs in `integration-test-server/logs/`
2. Verify YAML syntax with online YAML validators
3. Run tests with verbose output: `mvn test -X -Dtest=*YamlDsl*`
4. Check test reports in `target/surefire-reports/`

## Advanced Usage

### Custom Actions
You can extend the YAML DSL by adding new action types to `YamlDslTest.java`.

### Parameter Validation
Add validation for expected parameters in action methods.

### Result Verification
Implement more sophisticated result checking in the `validateResults()` method.

---

For more information, see the source code in `src/test/java/com/dragonegg/lightning/integration/yamldsl/YamlDslTest.java`
