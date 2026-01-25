# Player Simulation Enhancement - Task List

## Task Legend

- 🔴 **Blocker** - Must be completed before proceeding
- 🟡 **Medium** - Important but can be parallelized
- 🟢 **Low** - Nice to have, can defer
- ⏱️ **Estimate** - Time estimate (hours/days)
- 🔄 **Dependency** - Depends on another task

---

## Phase 0: Infrastructure Setup (Blocker)

### Core Utilities

- [ ] 🔴 **Create CorrelationUtils helper** ⏱️ 4h
  - Location: `packages/framework/lib/helpers/correlation.js`
  - Methods:
    - `waitForServerConfirmation(storyRunner, options)`
    - `_matchPattern(text, pattern)`
  - Dependencies: None
  - Tests: Unit tests for pattern matching, timeout handling

- [ ] 🔴 **Create EntityUtils helper** ⏱️ 3h
  - Location: `packages/framework/lib/helpers/entities.js`
  - Methods:
    - `findEntity(bot, identifier)`
    - `getNearestEntity(bot, typeName, maxDistance)`
  - Dependencies: None
  - Tests: Unit tests for entity lookup logic

- [ ] 🔴 **Update StoryRunner with correlation integration** ⏱️ 2h
  - Location: `packages/framework/lib/StoryRunner.js`
  - Changes:
    - Import CorrelationUtils and EntityUtils
    - Add `_waitForServerConfirmation()` private method
    - Add `_getTimeoutForAction(action)` private method
  - Dependencies: CorrelationUtils, EntityUtils
  - Tests: Verify helper methods are accessible

- [ ] 🟡 **Update framework exports** ⏱️ 0.5h
  - Location: `packages/framework/lib/index.js`
  - Add: `CorrelationUtils`, `EntityUtils` to exports
  - Dependencies: Core utilities completed

---

## Phase 1: Block Interaction (Foundation)

### 1.1 break_block Action

- [ ] 🔴 **Implement break_block handler** ⏱️ 4h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `location` (required) - {x, y, z}
    - `wait_for_drop` (optional, default: true)
  - Implementation:
    - Use `bot.blockAt()` to get target block
    - Call `bot.dig(target, true)`
    - Use CorrelationUtils for server confirmation
    - Optionally wait for item drop entity
  - Return: `{ broken: true, location }`
  - Dependencies: Phase 0 complete

- [ ] 🟡 **Unit tests for break_block** ⏱️ 2h
  - Location: `packages/framework/lib/break_block.spec.js`
  - Tests:
    - Missing player parameter
    - Missing location parameter
    - Invalid location (no block at position)
    - Server correlation timeout
    - Item drop waiting
  - Dependencies: break_block implementation

- [ ] 🟡 **Integration test for break_block** ⏱️ 2h
  - Location: `tests/integration/block-interaction.pilaf.test.js`
  - Scenario: Place dirt block, break it, verify drop
  - Dependencies: break_block implementation

### 1.2 place_block Action

- [ ] 🔴 **Implement place_block handler** ⏱️ 4h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `block` (required) - block name (e.g., 'dirt', 'stone')
    - `location` (required) - {x, y, z}
    - `face` (optional, default: 'top') - which face to place on
  - Implementation:
    - Use minecraft-data to lookup block type
    - Use `bot.blockAt()` to get reference block
    - Call `bot.placeBlock(referenceBlock, faceVector)`
    - Use CorrelationUtils for server confirmation
  - Return: `{ placed: true, location }`
  - Dependencies: Phase 0 complete

- [ ] 🟡 **Unit tests for place_block** ⏱️ 2h
  - Location: `packages/framework/lib/place_block.spec.js`
  - Tests:
    - Missing required parameters
    - Invalid block name
    - Invalid location
    - Server correlation
  - Dependencies: place_block implementation

- [ ] 🟡 **Integration test for place_block** ⏱️ 2h
  - Location: `tests/integration/block-interaction.pilaf.test.js`
  - Scenario: Place various blocks, verify with RCON
  - Dependencies: place_block implementation

### 1.3 interact_with_block Action

- [ ] 🔴 **Implement interact_with_block handler** ⏱️ 3h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `location` (required) - {x, y, z}
    - `interaction_type` (optional) - 'click', 'use', etc.
  - Implementation:
    - Use `bot.blockAt()` to get target
    - Call `bot.activateBlock(block)`
    - Handle special cases (door, chest, button, lever)
    - Use CorrelationUtils for server confirmation
  - Return: `{ interacted: true, block_type }`
  - Dependencies: Phase 0 complete

- [ ] 🟡 **Unit tests for interact_with_block** ⏱️ 1.5h
  - Location: `packages/framework/lib/interact_with_block.spec.js`
  - Tests: Parameter validation, error handling
  - Dependencies: interact_with_block implementation

- [ ] 🟡 **Integration test for interact_with_block** ⏱️ 2h
  - Location: `tests/integration/block-interaction.pilaf.test.js`
  - Scenario: Open chest, press button, flip lever
  - Dependencies: interact_with_block implementation

---

## Phase 2: Advanced Movement

### 2.1 Directional Movement Actions

- [ ] 🟡 **Implement movement handlers** ⏱️ 3h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Actions:
    - `move_backward` - parameters: player, duration
    - `move_left` - parameters: player, duration
    - `move_right` - parameters: player, duration
  - Implementation:
    - Use `bot.setControlState(direction, true/false)`
    - Wait for duration
    - Server correlation: check for "moved wrongly" anti-cheat
  - Return: `{ moved: true, distance, final_position }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Unit tests for directional movement** ⏱️ 2h
  - Location: `packages/framework/lib/movement.spec.js`
  - Tests: Parameter validation
  - Dependencies: Movement implementation

- [ ] 🟢 **Integration test for directional movement** ⏱️ 2h
  - Location: `tests/integration/movement.pilaf.test.js`
  - Scenario: Move in all directions, verify position changes
  - Dependencies: Movement implementation

### 2.2 jump Action

- [ ] 🟡 **Implement jump handler** ⏱️ 2h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters: player (required)
  - Implementation: `bot.setControlState('jump', true)` then false
  - Return: `{ jumped: true }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Unit tests for jump** ⏱️ 1h
  - Tests: Parameter validation
  - Dependencies: jump implementation

### 2.3 look_at Action

- [ ] 🟡 **Implement look_at handler** ⏱️ 2h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `position` (required) - {x, y, z} OR
    - `entity` (optional) - entity name to look at
  - Implementation:
    - Use `bot.lookAt(position)` or `bot.lookAt(entity.position)`
    - Optional: `bot.look(yaw, pitch)` for precise angles
  - Return: `{ looked: true, yaw, pitch }`
  - Dependencies: Phase 0 complete, EntityUtils

- [ ] 🟢 **Unit tests for look_at** ⏱️ 1.5h
  - Tests: Position and entity parameter variants
  - Dependencies: look_at implementation

### 2.4 navigate_to Action (Pathfinding)

- [ ] 🔴 **Add pathfinding dependencies** ⏱️ 0.5h
  - Location: `packages/backends/package.json`
  - Add: `mineflayer-pathfinder` plugin to MineflayerBackend
  - Dependencies: None

- [ ] 🔴 **Implement navigate_to handler** ⏱️ 6h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `destination` (required) - {x, y, z}
    - `timeout_ms` (optional, default: 10000)
  - Implementation:
    - Use `bot.pathfinder.goto()` with GoalBlock
    - Configure Movements for the bot
    - Server correlation: anti-cheat check
    - Handle pathfinding failures
  - Return: `{ reached: true/false, final_position }`
  - Dependencies: Pathfinding plugin, Phase 0 complete

- [ ] 🟡 **Unit tests for navigate_to** ⏱️ 2h
  - Tests: Parameters, timeout handling
  - Dependencies: navigate_to implementation

- [ ] 🟡 **Integration test for navigate_to** ⏱️ 3h
  - Scenario: Navigate to various locations, verify arrival
  - Dependencies: navigate_to implementation

---

## Phase 3: Entity Interaction

### 3.1 attack_entity Action

- [ ] 🔴 **Implement attack_entity handler** ⏱️ 4h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `entity_name` (optional) - name/customName to find
    - `entity_selector` (optional) - direct entity ID
  - Implementation:
    - Use EntityUtils.findEntity() to locate target
    - Call `bot.attack(entity)`
    - Wait for server damage/death log
    - Return entity health post-attack
  - Return: `{ attacked: true, entity: { id, name, health } }`
  - Dependencies: Phase 0 complete, EntityUtils

- [ ] 🟡 **Unit tests for attack_entity** ⏱️ 2h
  - Tests: Entity lookup, attack execution
  - Dependencies: attack_entity implementation

- [ ] 🟡 **Integration test for attack_entity** ⏱️ 3h
  - Scenario: Spawn zombie, attack it, verify damage
  - Dependencies: attack_entity implementation

### 3.2 interact_with_entity Action

- [ ] 🟡 **Implement interact_with_entity handler** ⏱️ 4h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `entity_name` (optional)
    - `entity_selector` (optional)
    - `interaction_type` (optional) - 'breed', 'trade', 'shear', etc.
  - Implementation:
    - Use EntityUtils.findEntity() to locate target
    - Call `bot.useOn(entity)`
    - Handle special cases (villager trading, animal breeding)
    - Server correlation for trade/breed events
  - Return: `{ interacted: true, entity_type }`
  - Dependencies: Phase 0 complete, EntityUtils

- [ ] 🟢 **Unit tests for interact_with_entity** ⏱️ 2h
  - Tests: Entity lookup, interaction execution
  - Dependencies: interact_with_entity implementation

- [ ] 🟢 **Integration test for interact_with_entity** ⏱️ 3h
  - Scenario: Breed animals, trade with villager
  - Dependencies: interact_with_entity implementation

### 3.3 mount/dismount Actions

- [ ] 🟡 **Implement mount_entity handler** ⏱️ 2h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters: player, entity_name (optional), entity_selector (optional)
  - Implementation: `bot.mount(entity)`
  - Return: `{ mounted: true, entity_type }`
  - Dependencies: Phase 0 complete, EntityUtils

- [ ] 🟡 **Implement dismount handler** ⏱️ 1h
  - Parameters: player
  - Implementation: `bot.dismount()`
  - Return: `{ dismounted: true }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Unit tests for mount/dismount** ⏱️ 1.5h
  - Tests: Both actions
  - Dependencies: mount/dismount implementation

- [ ] 🟢 **Integration test for mount/dismount** ⏱️ 2h
  - Scenario: Ride horse/boat, dismount
  - Dependencies: mount/dismount implementation

---

## Phase 4: Inventory Management

### 4.1 drop_item Action

- [ ] 🟡 **Implement drop_item handler** ⏱️ 3h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `item_name` (optional) - if not specified, drop held item
    - `count` (optional, default: 1)
  - Implementation:
    - Use `bot.toss(itemType, null, count)` or `bot.tossStack()`
    - Server correlation: wait for item drop entity
  - Return: `{ dropped: true, item, count }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Unit tests for drop_item** ⏱️ 2h
  - Tests: Item lookup, drop execution
  - Dependencies: drop_item implementation

- [ ] 🟢 **Integration test for drop_item** ⏱️ 2h
  - Scenario: Give item, drop it, verify on ground
  - Dependencies: drop_item implementation

### 4.2 consume_item Action

- [ ] 🟡 **Implement consume_item handler** ⏱️ 3h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `item_name` (optional) - if not specified, consume held item
  - Implementation:
    - Use `bot.consume()` or equip then consume
    - Server correlation: food eaten log
    - Check for food level changes
  - Return: `{ consumed: true, food_level, saturation }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Unit tests for consume_item** ⏱️ 2h
  - Tests: Item validation, consume execution
  - Dependencies: consume_item implementation

- [ ] 🟢 **Integration test for consume_item** ⏱️ 2h
  - Scenario: Eat food, verify hunger restored
  - Dependencies: consume_item implementation

### 4.3 equip_item Action

- [ ] 🟡 **Implement equip_item handler** ⏱️ 3h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `item_name` (required)
    - `destination` (optional, default: 'hand') - 'hand', 'head', 'chest', 'legs', 'feet'
  - Implementation:
    - Find item in inventory
    - Call `bot.equip(item, destination)`
    - Verify equipped by checking inventory slot
  - Return: `{ equipped: true, item, slot }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Unit tests for equip_item** ⏱️ 2h
  - Tests: Item lookup, equip execution
  - Dependencies: equip_item implementation

- [ ] 🟢 **Integration test for equip_item** ⏱️ 2h
  - Scenario: Equip armor, verify with RCON
  - Dependencies: equip_item implementation

### 4.4 swap_inventory_slots Action

- [ ] 🟡 **Implement swap_inventory_slots handler** ⏱️ 3h
  - Location: `packages/framework/lib/StoryRunner.js` → actionHandlers
  - Parameters:
    - `player` (required)
    - `from_slot` (required)
    - `to_slot` (required)
  - Implementation:
    - Use `bot.clickWindow()` logic
    - Move item between slots
  - Return: `{ swapped: true, from_slot, to_slot }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Unit tests for swap_inventory_slots** ⏱️ 2h
  - Tests: Slot validation, swap execution
  - Dependencies: swap_inventory_slots implementation

---

## Phase 5: Advanced Actions

### 5.1 sneak/unsneak Actions

- [ ] 🟡 **Implement sneak handler** ⏱️ 1.5h
  - Parameters: player
  - Implementation: `bot.setControlState('sneak', true)`
  - Return: `{ sneaking: true }`
  - Dependencies: Phase 0 complete

- [ ] 🟡 **Implement unsneak handler** ⏱️ 1h
  - Parameters: player
  - Implementation: `bot.setControlState('sneak', false)`
  - Return: `{ sneaking: false }`
  - Dependencies: Phase 0 complete

### 5.2 sprint/walk Actions

- [ ] 🟡 **Implement sprint handler** ⏱️ 1.5h
  - Parameters: player
  - Implementation: `bot.setControlState('sprint', true)`
  - Return: `{ sprinting: true }`
  - Dependencies: Phase 0 complete

- [ ] 🟡 **Implement walk handler** ⏱️ 1h
  - Parameters: player
  - Implementation: `bot.setControlState('sprint', false)`
  - Return: `{ sprinting: false }`
  - Dependencies: Phase 0 complete

### 5.3 open_container Action

- [ ] 🟡 **Implement open_container handler** ⏱️ 4h
  - Parameters: player, location (optional - uses target block if bot is looking at it)
  - Implementation:
    - Use `bot.openBlock(block)` or `bot.openChest()`
    - Wait for window open event
    - Optionally return container contents
  - Return: `{ opened: true, container_type, items[] }`
  - Dependencies: Phase 0 complete

- [ ] 🟢 **Integration test for open_container** ⏱️ 2h
  - Scenario: Open chest, verify contents
  - Dependencies: open_container implementation

### 5.4 craft_item Action

- [ ] 🔴 **Implement craft_item handler** ⏱️ 8h
  - Parameters:
    - `player` (required)
    - `item_name` (required)
    - `count` (optional, default: 1)
  - Implementation:
    - Use `bot.recipesFor()` to find recipes
    - Use `bot.craft()` to execute crafting
    - Handle crafting table requirement
    - Server correlation for crafted item
  - Return: `{ crafted: true, item, count }`
  - Dependencies: Phase 0 complete

- [ ] 🟡 **Unit tests for craft_item** ⏱️ 3h
  - Tests: Recipe lookup, crafting execution
  - Dependencies: craft_item implementation

- [ ] 🟡 **Integration test for craft_item** ⏱️ 3h
  - Scenario: Craft simple items (sticks, planks)
  - Dependencies: craft_item implementation

---

## Documentation Tasks

- [ ] 🟡 **Update README with new actions** ⏱️ 3h
  - Add all new actions to actions reference table
  - Add example for each category
  - Update "Available Actions" section

- [ ] 🟡 **Update CHANGELOG.md** ⏱️ 1h
  - Document all new actions
  - Note breaking changes (should be none)
  - Add migration guide if needed

- [ ] 🟢 **Create example tests** ⏱️ 4h
  - Location: `examples/`
  - Create examples for each action category:
    - block-interaction.example.pilaf.test.js
    - movement.example.pilaf.test.js
    - entity-interaction.example.pilaf.test.js
    - inventory.example.pilaf.test.js
    - advanced-actions.example.pilaf.test.js

- [ ] 🟢 **Update package.json versions** ⏱️ 0.5h
  - Bump version for all packages (minor version bump)
  - Ensure dependencies are correct

---

## Testing & QA Tasks

- [ ] 🔴 **Run all unit tests** ⏱️ 1h
  - Ensure all new tests pass
  - Check for regressions
  - Dependencies: All unit tests written

- [ ] 🔴 **Run all integration tests** ⏱️ 2h
  - Ensure all integration tests pass
  - Check server correlation working
  - Dependencies: All integration tests written

- [ ] 🟡 **Performance testing** ⏱️ 2h
  - Test with many actions in sequence
  - Check for memory leaks
  - Verify timeout handling
  - Dependencies: All actions implemented

- [ ] 🟡 **Edge case testing** ⏱️ 2h
  - Test with invalid parameters
  - Test with offline players
  - Test with non-existent entities/blocks
  - Test timeout scenarios

---

## Release Tasks

- [ ] 🔴 **Verify backward compatibility** ⏱️ 1h
  - Run existing test suite
  - Ensure no breaking changes
  - Dependencies: All code complete

- [ ] 🟡 **Create release branch** ⏱️ 0.5h
  - Branch from main
  - Name: feature/player-simulation-enhancement

- [ ] 🟡 **Create PR** ⏱️ 1h
  - Summarize all changes
  - Link to PLAN.md and TASK.md
  - Request review

- [ ] 🟡 **Merge to main** ⏱️ 0.5h
  - After approval
  - Ensure CI passes

- [ ] 🟡 **Tag release** ⏱️ 0.5h
  - Create git tag
  - Push to remote

---

## Summary

### Total Time Estimate

| Phase | Estimated Hours |
|-------|----------------|
| Phase 0 - Infrastructure | 9.5h |
| Phase 1 - Block Interaction | 22.5h |
| Phase 2 - Advanced Movement | 20.5h |
| Phase 3 - Entity Interaction | 19.5h |
| Phase 4 - Inventory Management | 16h |
| Phase 5 - Advanced Actions | 22h |
| Documentation | 8.5h |
| Testing & QA | 7h |
| Release | 3.5h |
| **TOTAL** | **~129 hours (~16 days)** |

### Priority Order

**MVP (Minimum Viable Product):**
1. Phase 0 (Infrastructure) - **REQUIRED**
2. Phase 1 (Block Interaction) - **HIGH PRIORITY**
3. Phase 2 (Directional Movement + Jump) - **HIGH PRIORITY**

**Second Iteration:**
4. Phase 3 (Entity Interaction) - **MEDIUM PRIORITY**
5. Phase 4 (Inventory Management) - **MEDIUM PRIORITY**

**Future:**
6. Phase 2 (Pathfinding) - **LOW PRIORITY**
7. Phase 5 (Advanced Actions) - **LOW PRIORITY**
