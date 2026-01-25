# Player Simulation Enhancement - Implementation Plan

## Overview

Enhance Pilaf's player simulation capabilities by wrapping additional Mineflayer actions with Pilaf's cross-channel correlation (player bot + server logs via EventObserver).

## Objectives

1. **Expand Test Coverage** - Enable testing of more complex plugin behaviors
2. **Maintain Correlation** - Ensure all actions are observable across both player and server channels
3. **Declarative API** - Keep test scenarios readable and maintainable in YAML/JS format
4. **Backward Compatible** - All additions are non-breaking

## Architecture Context

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Pilaf Test Layer                             │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                     StoryRunner                                    │ │
│  │  - Executes declarative test stories                               │ │
│  │  - Manages bot lifecycle                                           │ │
│  │  - Coordinates action handlers                                      │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                   │
│                                    ▼                                   │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                   Action Handlers (NEW ADDITIONS)                 │ │
│  │  ┌───────────┬─────────────┬──────────────┬──────────────────┐    │ │
│  │  │  Block    │  Movement   │   Entity     │   Inventory      │    │ │
│  │  │ Actions   │  Actions    │  Actions     │   Actions        │    │ │
│  │  └───────────┴─────────────┴──────────────┴──────────────────┘    │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                   │
│                    ┌───────────────┴───────────────┐                   │
│                    ▼                               ▼                   │
│  ┌─────────────────────────┐         ┌─────────────────────────────────┐│
│  │   MineflayerBackend     │         │      EventObserver              ││
│  │   - bot.dig()           │         │      (Cross-Channel            ││
│  │   - bot.placeBlock()    │   +     │       Correlation)              ││
│  │   - bot.attack()        │         │      - Player Bot Events        ││
│  │   - bot.equip()         │         │      - Docker Log Monitoring    ││
│  └─────────────────────────┘         └─────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

## Design Principles

### 1. Wrapping Criteria

Wrap an action when:
- ✅ **Common Test Scenario** - Frequently needed in plugin testing
- ✅ **Server Observability Required** - Need to verify server-side confirmation
- ✅ **Declarative Value** - Benefits from readable YAML format
- ✅ **Complex Behavior** - Multi-step operations that should be abstracted

**DO NOT wrap when:**
- ❌ Rarely used in tests
- ❌ No server-side confirmation needed
- ❌ Simple one-liner better expressed directly
- ❌ Highly plugin-specific behavior

### 2. Correlation Strategy

Each wrapped action must:

```javascript
async action_name(params) {
  // 1. Extract parameters
  const { player, ...args } = params;

  // 2. Get bot
  const bot = this.bots.get(player);

  // 3. Execute player action (Mineflayer API)
  await bot.someMethod(args);

  // 4. CRITICAL: Correlate with server logs
  // Wait for EventObserver to detect server confirmation
  await this._waitForServerConfirmation({
    pattern: `*expected_server_event*`,
    timeout: 5000
  });

  // 5. Return observable result
  return { success: true, data: '...' };
}
```

### 3. Error Handling

```javascript
async action_name(params) {
  try {
    // Player action
    const result = await bot.someMethod();

    // Server correlation with timeout
    await this._waitForServerConfirmation({
      pattern: expectedPattern,
      timeout: this._getTimeoutForAction(action)
    });

    return result;
  } catch (error) {
    // Distinguish between:
    // - Player action failure (bot API error)
    // - Server correlation timeout (server didn't confirm)
    throw new ActionError(action, error);
  }
}
```

## Implementation Phases

### Phase 1: Block Interaction (Foundation)

**Actions:**
- `break_block` - Break a block at location
- `place_block` - Place a block at location
- `interact_with_block` - Right-click block (chest, door, button, lever)

**Complexity:** Medium
**Rationale:** Most plugins interact with blocks; foundational for other actions

**Technical Details:**
```javascript
// break_block
async break_block(params) {
  const { player, location, wait_for_drop = true } = params;
  const bot = this.bots.get(player);

  const target = bot.blockAt(new Vec3(location.x, location.y, location.z));

  // Player action
  await bot.dig(target, true);  // true = ignore 'aren't you allowed to dig' error

  // Server correlation: wait for block break log
  await this._waitForServerLog({
    pattern: `*Cannot break block*|*Broken block*`,  // Negative or positive
    invert: true,  // Wait for ABSENCE of error message
    timeout: 3000
  });

  if (wait_for_drop) {
    // Optionally wait for item drop
    await this._waitForEntitySpawn({
      type: 'item',
      near: location,
      timeout: 2000
    });
  }

  return { broken: true, location };
}
```

**Dependencies:**
- Minecraft data (`minecraft-data`) for block lookups
- Vec3 for position handling

### Phase 2: Advanced Movement

**Actions:**
- `move_backward` / `move_left` / `move_right` - Directional movement
- `jump` - Player jump
- `look_at` - Change view direction
- `navigate_to` - Pathfinding to location

**Complexity:** Low (first 4), High (navigate_to)
**Rationale:** Complete movement control for positioning tests

**Technical Details:**
```javascript
// navigate_to (uses pathfinding)
async navigate_to(params) {
  const { player, destination, timeout_ms = 10000 } = params;
  const bot = this.bots.get(player);

  const target = new Vec3(destination.x, destination.y, destination.z);

  // Use Mineflayer's pathfinding
  const movements = new Movements(bot);
  const goal = new goals.GoalBlock(target.x, target.y, target.z);

  await bot.pathfinder.goto(goal, { timeout: timeout_ms });

  // Server correlation: wait for position update in logs
  await this._waitForServerLog({
    pattern: `*${player}*moved*wrongly!*`,  // Anti-cheat check
    invert: true,
    timeout: 1000
  });

  // Return actual final position
  return {
    reached: true,
    position: {
      x: bot.entity.position.x,
      y: bot.entity.position.y,
      z: bot.entity.position.z
    }
  };
}
```

**Dependencies:**
- `mineflayer-pathfinder` (already in Mineflayer)
- `mineflayer-movements` for pathfinding config

### Phase 3: Entity Interaction

**Actions:**
- `attack_entity` - Attack an entity
- `interact_with_entity` - Right-click entity (villager trade, animal breeding)
- `mount_entity` / `dismount` - Ride vehicles/animals

**Complexity:** Medium
**Rationale:** Testing combat, NPC interaction, transportation plugins

**Technical Details:**
```javascript
// attack_entity
async attack_entity(params) {
  const { player, entity_name, entity_selector } = params;
  const bot = this.bots.get(player);

  // Find entity
  const target = entity_selector
    ? bot.entities[entity_selector]
    : Object.values(bot.entities).find(e =>
        e.name === entity_name ||
        e.displayName === entity_name ||
        e.customName === entity_name
      );

  if (!target) {
    throw new Error(`Entity "${entity_name}" not found`);
  }

  // Player action
  await bot.attack(target);

  // Server correlation: wait for damage/death log
  await this._waitForServerLog({
    pattern: `*dealt*damage*|*killed*`,
    timeout: 3000
  });

  return {
    attacked: true,
    entity: {
      id: target.id,
      name: target.name,
      health: target.health  // Post-attack health
    }
  };
}
```

### Phase 4: Inventory Management

**Actions:**
- `drop_item` - Drop item from inventory
- `consume_item` - Eat food, drink potion
- `equip_item` - Equip armor/tool
- `swap_inventory_slots` - Move items between slots

**Complexity:** Medium
**Rationale:** Testing inventory plugins, food mechanics, equipment systems

**Technical Details:**
```javascript
// equip_item
async equip_item(params) {
  const { player, item_name, destination = 'hand' } = params;
  const bot = this.bots.get(player);

  // Find item in inventory
  const item = bot.inventory.items().find(i =>
    i && i.name === item_name
  );

  if (!item) {
    throw new Error(`Item "${item_name}" not found in inventory`);
  }

  // Player action
  await bot.equip(item, destination);

  // Server correlation: equipment change is client-side only
  // But we can verify by checking inventory again
  const equipped = bot.inventory.slots[bot.inventory.selectedSlot];
  if (!equipped || equipped.name !== item_name) {
    throw new Error(`Failed to equip "${item_name}"`);
  }

  return {
    equipped: true,
    item: equipped.name,
    slot: bot.inventory.selectedSlot
  };
}
```

### Phase 5: Advanced Actions

**Actions:**
- `sneak` / `unsneak` - Toggle sneaking
- `sprint` / `walk` - Toggle sprinting
- `open_container` - Open chest/furnace menu
- `craft_item` - Simple crafting recipes

**Complexity:** High (crafting), Medium (others)
**Rationale:** Complete player simulation coverage

## Technical Infrastructure

### New Helper: CorrelationUtils

```javascript
// packages/framework/lib/helpers/correlation.js

class CorrelationUtils {
  /**
   * Wait for server log confirmation
   */
  static async waitForServerConfirmation(storyRunner, options) {
    const { pattern, timeout = 5000, invert = false } = options;

    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        cleanup();
        reject(new Error(`Server confirmation timeout: ${pattern}`));
      }, timeout);

      const handler = (event) => {
        const matches = CorrelationUtils._matchPattern(event.message, pattern);

        if (invert && !matches) {
          cleanup();
          resolve();
        } else if (!invert && matches) {
          cleanup();
          resolve(event);
        }
      };

      const cleanup = () => {
        clearTimeout(timer);
        // Unsubscribe from EventObserver
      };

      // Subscribe to EventObserver
      storyRunner.backends.rcon.getEventObserver().onEvent('*', handler);
    });
  }

  /**
   * Simple glob pattern matching
   */
  static _matchPattern(text, pattern) {
    // Convert * wildcards to regex
    const regex = new RegExp(
      '^' + pattern.replace(/\*/g, '.*').replace(/\?/g, '.') + '$'
    );
    return regex.test(text);
  }
}

module.exports = { CorrelationUtils };
```

### New Helper: EntityUtils

```javascript
// packages/framework/lib/helpers/entities.js

class EntityUtils {
  /**
   * Find entity by name/customName/selector
   */
  static findEntity(bot, identifier) {
    // Try direct entity ID first
    if (typeof identifier === 'number') {
      return bot.entities[identifier];
    }

    // Try custom name
    let entity = Object.values(bot.entities).find(e =>
      e.customName === identifier ||
      e.customName?.text === identifier
    );

    if (!entity) {
      // Try display name
      entity = Object.values(bot.entities).find(e =>
        e.displayName === identifier
      );
    }

    if (!entity) {
      // Try entity type name
      entity = Object.values(bot.entities).find(e =>
        e.name === identifier
      );
    }

    return entity;
  }

  /**
   * Get nearest entity of type
   */
  static getNearestEntity(bot, typeName, maxDistance = 32) {
    const playerPos = bot.entity.position;

    return Object.values(bot.entities)
      .filter(e => e.name === typeName)
      .map(e => ({
        entity: e,
        distance: playerPos.distanceTo(e.position)
      }))
      .filter(e => e.distance <= maxDistance)
      .sort((a, b) => a.distance - b.distance)[0]?.entity;
  }
}

module.exports = { EntityUtils };
```

## Testing Strategy

### Unit Tests

Each action handler needs:
1. **Parameter validation** - Required fields, type checking
2. **Bot lookup** - Correct bot retrieval
3. **Error handling** - Missing entities, invalid locations
4. **Return value structure** - Consistent format

```javascript
describe('break_block action', () => {
  it('should require player parameter', async () => {
    const runner = new StoryRunner();
    await expect(runner.executeAction('break_block', {}))
      .rejects.toThrow('player');
  });

  it('should return broken status and location', async () => {
    // Mock bot and backend
    const result = await runner.executeAction('break_block', {
      player: 'TestPlayer',
      location: { x: 0, y: 64, z: 0 }
    });
    expect(result.broken).toBe(true);
    expect(result.location).toEqual({ x: 0, y: 64, z: 0 });
  });
});
```

### Integration Tests

Each action needs:
1. **Real server test** - Full Minecraft server
2. **Server correlation** - Verify EventObserver detects event
3. **Idempotency** - Can be run multiple times
4. **Timeout handling** - Doesn't hang on server non-response

```javascript
describe('break_block - Integration', () => {
  it('should break dirt block and verify server log', async () => {
    const result = await runner.execute({
      name: 'Break dirt block',
      setup: { server: {...}, players: [...] },
      steps: [
        {
          name: 'Place dirt block',
          action: 'place_block',
          block: 'dirt',
          location: { x: 0, y: 64, z: 0 }
        },
        {
          name: 'Break the block',
          action: 'break_block',
          location: { x: 0, y: 64, z: 0 },
          wait_for_drop: true
        },
        {
          name: 'Verify block gone',
          action: 'assert_block',
          location: { x: 0, y: 64, z: 0 },
          expected: 'air'
        }
      ],
      teardown: { stop_server: false }
    });

    expect(result.success).toBe(true);
  });
});
```

### Documentation

Each action needs:
1. **README update** - Add to actions reference table
2. **JSDoc comments** - Parameter types and descriptions
3. **Example test** - Real usage example
4. **Changelog entry** - Document in CHANGELOG.md

## Backward Compatibility

All changes are **additive only**:
- ✅ New actions don't modify existing ones
- ✅ Existing tests continue to work unchanged
- ✅ No breaking changes to API
- ✅ Optional parameters only

## Dependencies to Add

```json
{
  "dependencies": {
    "mineflayer-pathfinder": "^2.4.4",
    "minecraft-data": "^3.60.0"
  }
}
```

Both are likely already pulled in by Mineflayer as dependencies.

## Performance Considerations

1. **Server Correlation Overhead**: Each wrapped action waits for server log
   - Mitigation: Configurable timeouts, optional correlation

2. **Pathfinding Cost**: `navigate_to` can be slow on complex terrain
   - Mitigation: Timeout defaults, distance limits

3. **Entity Query Overhead**: Scanning all entities for each query
   - Mitigation: EntityUtils caches results within tick

## Success Criteria

Each phase is complete when:
- [ ] All actions implemented with error handling
- [ ] Server correlation working (EventObserver integration)
- [ ] Unit tests passing (>80% coverage)
- [ ] Integration tests passing (real server)
- [ ] Documentation updated
- [ ] Changelog entry added
- [ ] No regression in existing tests

## Timeline Estimate

| Phase | Actions | Est. Complexity | Est. Time |
|-------|---------|-----------------|-----------|
| Phase 1 | Block Interaction (3) | Medium | 3-4 days |
| Phase 2 | Advanced Movement (4) | Mixed | 2-5 days |
| Phase 3 | Entity Interaction (3) | Medium | 3-4 days |
| Phase 4 | Inventory Management (4) | Medium | 3-4 days |
| Phase 5 | Advanced Actions (4) | Mixed | 4-6 days |

**Total**: ~15-23 days for all phases (can be done incrementally)

## Open Questions

1. **Action granularity**: Should `move_to` be split into `move_to_north`, etc., or one action with direction parameter?
   - **Recommendation**: One action with `direction` parameter

2. **Server correlation strictness**: Should server correlation be required or optional?
   - **Recommendation**: Required by default, optional via `skip_correlation: true` parameter

3. **Pathfinding timeout**: What's reasonable default for complex navigation?
   - **Recommendation**: 10 seconds, configurable via `timeout_ms`

4. **Container handling**: Should we support container interaction (chest menu navigation)?
   - **Recommendation**: Phase 6 (future), requires window management API
