# PILAF Backend Consistency Testing Plan

## Overview
This plan outlines how to test and validate that PILAF behaves consistently across different backend combinations, ensuring that users get the same test results regardless of which backend they choose.

## Test Objective
Validate that the same YAML story produces identical results across all backend combinations:
1. Docker Server + Mineflayer Client
2. Docker Server + HeadlessMc Client
3. HeadlessMc Server + Mineflayer Client
4. HeadlessMc Server + HeadlessMc Client

## Backend Combinations to Test

### 1. Docker Server + Mineflayer Client (Production)
```yaml
# config-docker-mineflayer.yaml
server_backend: docker
client_backend: mineflayer
server_version: "1.21.5"
mineflayer_url: http://localhost:3000
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
```

### 2. Docker Server + HeadlessMc Client (CI/CD)
```yaml
# config-docker-headlessmc.yaml
server_backend: docker
client_backend: headlessmc
server_version: "1.21.5"
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
```

### 3. HeadlessMc Server + Mineflayer Client (Self-Contained)
```yaml
# config-headlessmc-mineflayer.yaml
server_backend: headlessmc
client_backend: mineflayer
server_version: "1.21.5"
mineflayer_url: http://localhost:3000
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
auto_launch: true
rcon_fallback: true
```

### 4. HeadlessMc Server + HeadlessMc Client (Pure Java)
```yaml
# config-headlessmc-both.yaml
server_backend: headlessmc
client_backend: headlessmc
server_version: "1.21.5"
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
auto_launch: true
rcon_fallback: true
```

## Test Stories

### Test Story 1: Basic Item Operations
```yaml
# test-story-1-basic-items.yaml
name: "Basic Item Operations Test"
description: "Tests basic item giving and inventory checking across backends"

steps:
  # Give player a diamond sword
  - action: "give_item"
    player: "TestPlayer"
    item: "diamond_sword"
    count: 1

  # Verify player has diamond sword
  - action: "check_inventory"
    player: "TestPlayer"
    item: "diamond_sword"
    slot: "mainhand"
    assertions:
      - type: "inventory_contains"
        expected: true

  # Give player cobblestone
  - action: "give_item"
    player: "TestPlayer"
    item: "cobblestone"
    count: 64

  # Verify cobblestone count
  - action: "check_inventory"
    player: "TestPlayer"
    item: "cobblestone"
    slot: "inventory"
    assertions:
      - type: "inventory_contains"
        expected: true

  # Clean up
  - action: "remove_item"
    player: "TestPlayer"
    item: "diamond_sword"
    count: 1

  - action: "remove_item"
    player: "TestPlayer"
    item: "cobblestone"
    count: 64
```

### Test Story 2: Entity Operations
```yaml
# test-story-2-entities.yaml
name: "Entity Operations Test"
description: "Tests entity spawning and health management across backends"

steps:
  # Spawn a test cow
  - action: "spawn_entity"
    name: "TestCow"
    type: "cow"
    location: [0, 64, 0]
    equipment: {}

  # Check entity exists
  - action: "check_entity_exists"
    entity_name: "TestCow"
    assertions:
      - type: "entity_exists"
        expected: true

  # Set entity health
  - action: "set_entity_health"
    entity_name: "TestCow"
    health: 10.0

  # Verify health change
  - action: "check_entity_health"
    entity_name: "TestCow"
    assertions:
      - type: "health_equals"
        expected: 10.0

  # Clean up entities
  - action: "remove_all_test_entities"
```

### Test Story 3: Player Movement
```yaml
# test-story-3-movement.yaml
name: "Player Movement Test"
description: "Tests player movement and positioning across backends"

steps:
  # Connect player
  - action: "connect_player"
    username: "TestPlayer"

  # Move player to coordinates
  - action: "move_player"
    player_name: "TestPlayer"
    destination_type: "coordinates"
    destination: "100 64 100"

  # Verify position
  - action: "check_player_position"
    player_name: "TestPlayer"
    assertions:
      - type: "position_approximately"
        x: 100
        y: 64
        z: 100

  # Move player to another location
  - action: "move_player"
    player_name: "TestPlayer"
    destination_type: "coordinates"
    destination: "200 70 200"

  # Disconnect player
  - action: "disconnect_player"
    username: "TestPlayer"
```

### Test Story 4: Server Commands
```yaml
# test-story-4-commands.yaml
name: "Server Commands Test"
description: "Tests server command execution across backends"

steps:
  # Execute simple server command
  - action: "execute_server_command"
    command: "say Testing server connectivity"
    assertions:
      - type: "command_success"
        expected: true

  # Check player count (if any players online)
  - action: "execute_server_command"
    command: "list"
    assertions:
      - type: "command_success"
        expected: true

  # Get server time
  - action: "execute_server_command"
    command: "time query gametime"
    assertions:
      - type: "command_success"
        expected: true

  # Clean up - kill test entities
  - action: "execute_server_command"
    command: "kill @e[type=!minecraft:player]"
```

## Testing Procedure

### Step 1: Environment Setup
1. **Start each backend combination separately**
2. **Verify backend is healthy before testing**
3. **Ensure RCON connectivity**

### Step 2: Run Test Stories
For each backend combination:
```bash
# Test each story
for story in test-story-1-basic-items.yaml test-story-2-entities.yaml test-story-3-movement.yaml test-story-4-commands.yaml; do
    echo "Testing $story with backend combination..."
    gradle run --args="--config=$CONFIG_FILE $story"
done
```

### Step 3: Capture Results
1. **Save test output for each combination**
2. **Compare success/failure rates**
3. **Identify any backend-specific behaviors**

### Step 4: Analysis
1. **Verify identical outcomes across backends**
2. **Document any expected differences**
3. **Create consistency report**

## Expected Results

### Consistent Behaviors (Should be identical)
- ✅ Item giving/removal operations
- ✅ Inventory checking
- ✅ Entity spawning and health management
- ✅ Server command execution
- ✅ Player connection/disconnection

### Potentially Different Behaviors (Documented)
- ⚠️ Startup time (Docker: ~30-60s, HeadlessMc: ~5-10s)
- ⚠️ Plugin installation (Docker: Full, HeadlessMc: Limited)
- ⚠️ Error handling (Different failure modes)
- ⚠️ Performance characteristics

## Validation Checklist

### Backend 1: Docker + Mineflayer
- [ ] Test story execution completes successfully
- [ ] All assertions pass
- [ ] Clean shutdown
- [ ] No runtime errors

### Backend 2: Docker + HeadlessMc
- [ ] Test story execution completes successfully
- [ ] All assertions pass
- [ ] Clean shutdown
- [ ] No runtime errors

### Backend 3: HeadlessMc + Mineflayer
- [ ] Test story execution completes successfully
- [ ] All assertions pass
- [ ] Clean shutdown
- [ ] No runtime errors

### Backend 4: HeadlessMc + HeadlessMc
- [ ] Test story execution completes successfully
- [ ] All assertions pass
- [ ] Clean shutdown
- [ ] No runtime errors

## Success Criteria

### All Backends Should Pass
1. **100% test story completion rate**
2. **Identical assertion results**
3. **Consistent final state** (no leftover entities/items)
4. **Proper error handling**

### Documentation Requirements
1. **Backend comparison matrix**
2. **Performance benchmarks**
3. **Known limitations by backend**
4. **Migration guide for users**

## Troubleshooting Guide

### Common Issues
1. **RCON Connection Refused**: Check server is running, verify credentials
2. **Backend Initialization Fails**: Verify dependencies and configuration
3. **Test Timeouts**: Increase timeout values for slower backends
4. **Plugin Conflicts**: Ensure clean server state between tests

### Debug Commands
```bash
# Test backend connectivity
gradle run --args="--config=$CONFIG --verbose test-story-1-basic-items.yaml"

# Check backend health
curl -s http://localhost:3000/health  # For Mineflayer
docker ps --filter name=pilaf-paper  # For Docker
```

## Next Steps
1. **Execute this plan in a dedicated testing session**
2. **Document any discrepancies found**
3. **Update backend documentation based on results**
4. **Create automated testing pipeline**
5. **Add backend consistency to CI/CD checks**

---

**Note**: This plan should be executed in a fresh session with proper infrastructure setup (Docker daemon, Mineflayer bridge, etc.).
