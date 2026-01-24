# Pilaf Documentation and Test Improvements Roadmap

```
Version: 2.0
Last Updated: 2026-01-08
Status: Planning
```

This roadmap outlines the plan to create comprehensive, practical Pilaf examples that demonstrate the framework's capabilities from basic to advanced usage.

---

## Executive Summary

Pilaf needs enhanced documentation with real-world examples that showcase its full feature set. This roadmap focuses on creating 3 carefully designed YAML test stories that progress from simple to complex, demonstrating increasingly advanced Pilaf features.

**Key Goals:**
- Create 3 example stories (simple, intermediate, complex) demonstrating Pilaf's capabilities
- Each example serves as a learning template for plugin developers
- Examples cover: RCON commands, player actions, state management, assertions, and entity operations
- Include world reset mechanisms for clean, repeatable tests

---

## Phase 1: Simple Example - Core Operations

**Priority: High** | **Timeline: 1 week**

### `examples/01-simple-player-inventory.yaml`

**Purpose:** Introduction to Pilaf basics - player connection, RCON commands, and basic assertions.

**Pilaf Features Demonstrated:**
- Connecting/disconnecting players via Mineflayer
- Server commands via RCON (`/give`, `/op`, `/deop`)
- Getting player inventory and position
- Basic assertions (item presence, position verification)
- Variable storage (`storeAs`) and retrieval
- Cleanup operations

**Test Coverage:**
```yaml
name: "Simple Player Inventory Test"
description: |
  Basic Pilaf example demonstrating:
  - Player connection and disconnection
  - RCON server commands
  - Inventory checks with assertions
  - State storage and retrieval

setup:
  # Connect test player
  - action: "connect"
    player: "pilaf_tester"
    name: "Connect test player"

  # Give initial items via RCON
  - action: "execute_rcon_command"
    command: "give pilaf_tester diamond_sword 1"
    name: "Give diamond sword"

  # Store initial inventory state
  - action: "get_player_inventory"
    player: "pilaf_tester"
    store_as: "initial_inventory"
    name: "Store initial inventory"

steps:
  # 1. Verify item was given
  - action: "execute_rcon_command"
    command: "give pilaf_tester golden_apple 5"
    name: "Give golden apples"

  - action: "get_player_inventory"
    player: "pilaf_tester"
    store_as: "inventory_with_apple"
    name: "Get updated inventory"

  # 2. Assertion: Check player has specific item
  - action: "assert_player_has_item"
    player: "pilaf_tester"
    item: "golden_apple"
    count: 5
    name: "Verify golden apples received"

  # 3. Position tracking and teleport
  - action: "get_player_position"
    player: "pilaf_tester"
    store_as: "pos_before"
    name: "Store position before teleport"

  - action: "execute_player_command"
    player: "pilaf_tester"
    command: "/spawn"
    name: "Teleport to spawn"

  - action: "get_player_position"
    player: "pilaf_tester"
    store_as: "pos_after"
    name: "Store position after teleport"

  # 4. State comparison
  - action: "compare_states"
    state1: "pos_before"
    state2: "pos_after"
    store_as: "position_diff"
    name: "Compare positions"

  - action: "print_state_comparison"
    variable_name: "position_diff"
    name: "Show position change"

cleanup:
  - action: "disconnect"
    player: "pilaf_tester"
    name: "Disconnect test player"

  - action: "execute_rcon_command"
    command: "clear pilaf_tester"
    name: "Clear player inventory"
```

**Learning Outcomes:**
- Understanding YAML story structure (setup/steps/cleanup)
- Basic action types and their parameters
- Using `storeAs` for state storage
- Simple assertions

---

## Phase 2: Intermediate Example - State & Comparisons

**Priority: High** | **Timeline: 1-2 weeks**

### `examples/02-intermediate-item-transaction.yaml`

**Purpose:** Advanced state management - inventory changes, state comparisons, and JSONPath extraction.

**Pilaf Features Demonstrated:**
- Pre/post state capture and comparison
- JSONPath for data extraction
- Multiple assertions in single action
- State variable manipulation
- Complex cleanup with entity removal
- Multi-player interaction (buyer/seller)

**Test Coverage:**
```yaml
name: "Intermediate Item Transaction Test"
description: |
  Demonstrates advanced Pilaf features:
  - State capture before/after actions
  - JSONPath data extraction
  - Multi-player interactions
  - Inventory state comparison
  - Complex assertion patterns

setup:
  # Connect two players
  - action: "connect"
    player: "buyer"
    name: "Connect buyer"

  - action: "connect"
    player: "seller"
    name: "Connect seller"

  # Give seller items to sell
  - action: "execute_rcon_command"
    command: "give seller diamond 64"
    name: "Give seller diamonds"

  - action: "execute_rcon_command"
    command: "give seller emerald 32"
    name: "Give seller emeralds"

  # Capture initial states
  - action: "get_player_inventory"
    player: "buyer"
    store_as: "buyer_before"
    name: "Store buyer inventory before"

  - action: "get_player_inventory"
    player: "seller"
    store_as: "seller_before"
    name: "Store seller inventory before"

  # Set spawn points
  - action: "execute_rcon_command"
    command: "execute as buyer at @s run spawnpoint @s ~ ~ ~"
    name: "Set buyer spawn"

  - action: "execute_rcon_command"
    command: "execute as seller at @s run spawnpoint @s ~ ~ ~"
    name: "Set seller spawn"

steps:
  # Transaction: Buyer gives iron to seller, gets diamond
  - action: "execute_rcon_command"
    command: "give buyer iron_ingot 10"
    name: "Give buyer trading items"

  # Manual trade simulation
  - action: "execute_player_command"
    player: "buyer"
    command: "/trade seller diamond 10"
    name: "Initiate trade"

  - action: "wait"
    duration: 2000
    name: "Wait for trade completion"

  # Capture post-transaction states
  - action: "get_player_inventory"
    player: "buyer"
    store_as: "buyer_after"
    name: "Store buyer inventory after"

  - action: "get_player_inventory"
    player: "seller"
    store_as: "seller_after"
    name: "Store seller inventory after"

  # Compare states
  - action: "compare_states"
    state1: "buyer_before"
    state2: "buyer_after"
    store_as: "buyer_diff"
    name: "Compare buyer inventory"

  - action: "compare_states"
    state1: "seller_before"
    state2: "seller_after"
    seller_as: "seller_diff"
    name: "Compare seller inventory"

  # JSONPath extraction example (assuming RCON returns JSON)
  - action: "execute_rcon_with_capture"
    command: "data get entity buyer"
    store_as: "buyer_data_raw"
    name: "Get buyer data"

  # Extract specific field using JSONPath
  - action: "extract_with_jsonpath"
    source_variable: "buyer_data_raw"
    json_path: "$.Health"
    store_as: "buyer_health"
    name: "Extract health value"

  # Print comparison results
  - action: "print_state_comparison"
    variable_name: "buyer_diff"
    name: "Show buyer inventory changes"

  - action: "print_state_comparison"
    variable_name: "seller_diff"
    name: "Show seller inventory changes"

cleanup:
  # Clean up test entities
  - action: "execute_rcon_command"
    command: "kill @e[name=buyer,name=seller]"
    name: "Remove test players if spawned"

  # Disconnect players
  - action: "disconnect"
    player: "buyer"
    name: "Disconnect buyer"

  - action: "disconnect"
    player: "seller"
    name: "Disconnect seller"

  # Clear any test items
  - action: "execute_rcon_command"
    command: "clear buyer"
    name: "Clear buyer inventory"

  - action: "execute_rcon_command"
    command: "clear seller"
    name: "Clear seller inventory"
```

**Learning Outcomes:**
- State capture before/after patterns
- JSONPath for extracting nested data
- Multi-player test coordination
- Complex comparison logic

---

## Phase 3: Complex Example - Full Feature Showcase

**Priority: High** | **Timeline: 2 weeks**

### `examples/03-comprehensive-plugin-test.yaml`

**Purpose:** Comprehensive demonstration of Pilaf's complete feature set for testing complex plugin behaviors.

**Pilaf Features Demonstrated:**
- All assertion types (entity, response, JSON, log)
- Entity spawning and verification
- Chat message capture and assertions
- World state manipulation
- Permission/cooldown mechanics
- Event-driven testing patterns
- World reset mechanism
- Multiple backend interactions

```yaml
name: "Comprehensive Plugin Integration Test"
description: |
  Full Pilaf feature demonstration:
  - Entity operations (spawn, verify, kill)
  - Chat/social features
  - All assertion types
  - World state management
  - Permission testing
  - Event simulation
  - World reset pattern

# World Reset Configuration
variables:
  test_center: [100, 64, 100]
  test_radius: 20

setup:
  # Reset world state for clean test
  - action: "execute_rcon_command"
    command: "save-off"
    name: "Disable auto-save"

  - action: "execute_rcon_command"
    command: "save-all"
    name: "Force world save"

  # Mark test area
  - action: "execute_rcon_command"
    command: "setblock 100 63 100 gold_block"
    name: "Mark test center"

  # Store original spawn
  - action: "execute_rcon_command"
    command: "execute as @p at @s run data get SpawnX"
    store_as: "original_spawn_x"
    name: "Store original spawn X"

  # Connect test players
  - action: "connect"
    player: "admin_tester"
    name: "Connect admin player"

  - action: "connect"
    player: "regular_tester"
    name: "Connect regular player"

  # Give players equipment
  - action: "execute_rcon_command"
    command: "give admin_tester diamond_sword 1"
    name: "Give admin sword"

  - action: "execute_rcon_command"
    command: "give regular_tester wooden_pickaxe 1"
    name: "Give regular pickaxe"

  # Spawn test entities
  - action: "spawn_entity"
    name: "test_zombie"
    type: "zombie"
    location: [102, 64, 102]
    store_as: "zombie_id"
    name: "Spawn test zombie"

  - action: "spawn_entity"
    name: "test_skeleton"
    type: "skeleton"
    location: [105, 64, 105]
    store_as: "skeleton_id"
    name: "Spawn test skeleton"

  # Capture entity states
  - action: "get_entity_health"
    entity: "test_zombie"
    store_as: "zombie_health_before"
    name: "Store zombie health before"

steps:
  # === SECTION 1: Entity Operations ===
  - action: "get_entities_in_view"
    player: "admin_tester"
    store_as: "entities_view_before"
    name: "Get visible entities before"

  - action: "damage_entity"
    entity: "test_zombie"
    amount: 10
    name: "Damage zombie"

  - action: "get_entity_health"
    entity: "test_zombie"
    store_as: "zombie_health_after"
    name: "Get zombie health after"

  - action: "compare_states"
    state1: "zombie_health_before"
    state2: "zombie_health_after"
    store_as: "damage_result"
    name: "Compare health change"

  - action: "assert_entity_exists"
    entity: "test_zombie"
    name: "Verify zombie still exists"

  - action: "get_entities_in_view"
    player: "admin_tester"
    store_as: "entities_view_after"
    name: "Get visible entities after"

  # === SECTION 2: Chat & Social Features ===
  - action: "send_chat_message"
    player: "admin_tester"
    message: "[TEST] Admin message from Pilaf"
    name: "Send test chat message"

  - action: "send_chat_message"
    player: "regular_tester"
    message: "[TEST] Regular player response"
    name: "Send response message"

  - action: "wait"
    duration: 1000
    name: "Wait for chat propagation"

  # Capture and verify chat
  - action: "get_chat_history"
    store_as: "chat_log"
    name: "Get chat history"

  - action: "assert_response_contains"
    source: "chat_log"
    contains: "Pilaf"
    name: "Verify test message in chat"

  # === SECTION 3: Permission & Command Testing ===
  # Test: Admin can execute protected command
  - action: "execute_player_command"
    player: "admin_tester"
    command: "/admin heal"
    name: "Admin attempts heal command"

  - action: "wait"
    duration: 500
    name: "Wait for command"

  - action: "assert_response_contains"
    source: "chat_log"
    contains: "healed"
    name: "Verify admin heal worked"

  # Test: Regular player cannot use admin command
  - action: "execute_player_command"
    player: "regular_tester"
    command: "/admin heal"
    name: "Regular player attempts admin command"

  - action: "assert_log_contains"
    pattern: "No permission"
    negated: true
    name: "Verify permission denial handled"

  # === SECTION 4: World State & Block Operations ===
  # Place block to test block events
  - action: "execute_rcon_command"
    command: "setblock 110 64 110 stone"
    name: "Place test stone"

  - action: "get_block_at_position"
    position: [110, 64, 110]
    store_as: "block_type"
    name: "Verify block placed"

  - action: "assert_json_equals"
    source: "block_type"
    expected: "minecraft:stone"
    name: "Verify block type"

  # === SECTION 5: Inventory & Equipment ===
  - action: "get_player_equipment"
    player: "admin_tester"
    store_as: "admin_equipment"
    name: "Get admin equipment"

  - action: "assert_player_has_item"
    player: "admin_tester"
    item: "diamond_sword"
    count: 1
    name: "Verify diamond sword"

  # === SECTION 6: Cooldown Mechanics ===
  - action: "execute_player_command"
    player: "admin_tester"
    command: "/ability use"
    name: "First ability use"

  - action: "set_cooldown"
    player: "admin_tester"
    duration: 5000
    name: "Set 5 second cooldown"

  - action: "execute_player_command"
    player: "admin_tester"
    command: "/ability use"
    name: "Second ability use during cooldown"

  # === SECTION 7: Complex Assertions ===
  - action: "assert_condition"
    condition: "${zombie_health_after.value} < ${zombie_health_before.value}"
    name: "Verify zombie took damage"

  - action: "assert_entity_missing"
    entity: "test_skeleton"
    name: "Verify skeleton was removed"

cleanup:
  # === World Reset ===
  # Remove test markers
  - action: "execute_rcon_command"
    command: "fill 90 63 90 110 73 110 air replace gold_block"
    name: "Remove test markers"

  # Clear test entities
  - action: "clear_entities"
    name: "Clear all test entities"

  # Disconnect players
  - action: "disconnect"
    player: "admin_tester"
    name: "Disconnect admin"

  - action: "disconnect"
    player: "regular_tester"
    name: "Disconnect regular"

  # Restore world save
  - action: "execute_rcon_command"
    command: "save-on"
    name: "Re-enable auto-save"

  # Print final summary
  - action: "print_stored_state"
    variable_name: "damage_result"
    name: "Show final damage comparison"
```

**Learning Outcomes:**
- All Pilaf assertion types and when to use them
- Entity lifecycle testing
- Chat/social feature testing patterns
- Permission and cooldown testing
- World state manipulation
- Complex cleanup and reset patterns

---

## Phase 4: Integration Tests

**Priority: Medium** | **Timeline: 1 week**

### `src/test/java/org/cavarest/pilaf/integration/ExampleStoryIntegrationTest.java`

Verify all example stories execute correctly:

```java
@Tag("integration")
public class ExampleStoryIntegrationTest {

    @Test
    void testSimpleExampleExecutes() {
        // Run 01-simple-player-inventory.yaml
        // Verify completion without errors
    }

    @Test
    void testIntermediateExampleExecutes() {
        // Run 02-intermediate-item-transaction.yaml
        // Verify state comparisons work
    }

    @Test
    void testComplexExampleExecutes() {
        // Run 03-comprehensive-plugin-test.yaml
        // Verify all assertions pass
    }
}
```

---

## Success Criteria

| Criterion | Target | Measurement |
|-----------|--------|-------------|
| Example runnability | 100% | All 3 examples execute without errors |
| Feature coverage | 100% | Each Pilaf action type demonstrated |
| Documentation | 100% | All examples documented in README |
| CI/CD integration | 100% | Examples run in CI pipeline |

---

## Example Learning Path

```
01-simple-player-inventory.yaml          -> Learn: Basic actions + assertions
    |
    v
02-intermediate-item-transaction.yaml    -> Learn: State management + comparisons
    |
    v
03-comprehensive-plugin-test.yaml        -> Learn: Full feature set
```

---

## Running the Examples

```bash
# Build Pilaf
./gradlew build

# Run simple example
./gradlew run --args="--config config-demo.yaml examples/01-simple-player-inventory.yaml"

# Run intermediate example
./gradlew run --args="--config config-demo.yaml examples/02-intermediate-item-transaction.yaml"

# Run complex example
./gradlew run --args="--config config-demo.yaml examples/03-comprehensive-plugin-test.yaml"

# Run all examples
./gradlew run --args="--config config-demo.yaml examples/"
```

---

## Dependencies

- Running PaperMC server with RCON enabled
- Mineflayer bridge (for player actions)
- Java 21+, Gradle 7.6+

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 2.0 | 2026-01-08 | Pilaf Team | Reduced to 3 comprehensive examples |
| 1.0 | 2026-01-08 | Pilaf Team | Initial roadmap with 6 examples |

---

*This roadmap is a living document. Update as project needs evolve.*
