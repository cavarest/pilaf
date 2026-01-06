# PILAF Implementation Continuation Plan

## Current Status

### What Works ✅
- Docker infrastructure (PaperMC 1.21.8 + Mineflayer bridge)
- RCON authentication and connection
- HTTP client connection to Mineflayer bridge
- Player connection via Mineflayer
- Verbose logging with raw request/response output
- Basic GIVE_ITEM actions
- Player position retrieval
- Test report generation

### Critical Bugs 🐛
1. **RCON Command Trailing Space**: `executeServerCommand()` adds space even with no arguments
2. **GET_ENTITIES JSON Parsing**: Complex JSON response from Mineflayer causes parsing error
3. **Action Type Mapping**: Some actions not properly mapped from YAML to enum

### Missing Implementations 🚧

#### Server Actions (RCON-based)
- ✅ Basic server commands (say, give, etc.)
- ❌ Proper command formatting (no trailing spaces)
- ❌ Entity spawning with correct syntax
- ❌ Player management (gamemode, clear, spawnpoint)
- ❌ World management (weather, time)

#### Client Actions (Mineflayer-based)
- ✅ Player connection/disconnection
- ✅ Position retrieval
- ❌ Inventory operations (getInventory returns complex JSON)
- ❌ Entity queries (getEntities returns complex JSON)
- ❌ Chat message sending
- ❌ Movement actions
- ❌ Health queries
- ❌ Equipment queries

#### State Management
- ❌ State capture and storage
- ❌ State comparison
- ❌ JSONPath extraction
- ❌ Conditional execution based on state

#### Assertions
- ❌ Entity health assertions
- ❌ Entity existence assertions
- ❌ Player inventory assertions
- ❌ Response content assertions
- ❌ JSON equality assertions

## Implementation Plan

### Phase 1: Fix Critical Bugs (Priority: HIGH)
**Estimated Time: 2-3 hours**

1. **Fix RCON Command Formatting**
   - Location: `RconBackend.java`, `MineflayerBackend.java`
   - Action: Remove trailing space when no arguments
   - Files: `src/main/java/org/cavarest/pilaf/backend/RconBackend.java`

2. **Fix GET_ENTITIES JSON Parsing**
   - Location: `MineflayerClient.java`
   - Action: Improve JSON parser to handle nested objects and arrays
   - Files: `src/main/java/org/cavarest/pilaf/client/MineflayerClient.java`

3. **Fix GET_INVENTORY JSON Parsing**
   - Location: `MineflayerClient.java`
   - Action: Same as GET_ENTITIES fix
   - Files: `src/main/java/org/cavarest/pilaf/client/MineflayerClient.java`

### Phase 2: Complete Server Actions (Priority: HIGH)
**Estimated Time: 3-4 hours**

1. **Entity Management**
   - Spawn entities with correct Minecraft 1.21.8 syntax
   - Kill entities
   - Query entity data

2. **Player Management**
   - Gamemode changes
   - Inventory clearing
   - Spawn point setting
   - Teleportation

3. **World Management**
   - Weather control
   - Time control
   - World queries

### Phase 3: Complete Client Actions (Priority: MEDIUM)
**Estimated Time: 4-5 hours**

1. **Movement Actions**
   - Walk to position
   - Look at target
   - Jump

2. **Interaction Actions**
   - Use item
   - Attack entity
   - Break block
   - Place block

3. **Communication Actions**
   - Send chat
   - Read chat history

### Phase 4: State Management (Priority: MEDIUM)
**Estimated Time: 3-4 hours**

1. **State Capture**
   - Capture server state
   - Capture player state
   - Capture entity state

2. **State Comparison**
   - Deep equality comparison
   - Diff generation
   - Semantic comparison

3. **State Queries**
   - JSONPath extraction
   - Filter operations
   - Aggregation

### Phase 5: Assertions (Priority: MEDIUM)
**Estimated Time: 2-3 hours**

1. **Entity Assertions**
   - Health checks
   - Existence checks
   - Position checks

2. **Player Assertions**
   - Inventory checks
   - Health checks
   - Position checks

3. **Response Assertions**
   - Content checks
   - JSON equality
   - Pattern matching

### Phase 6: Documentation (Priority: HIGH)
**Estimated Time: 2-3 hours**

1. **Update README.adoc**
   - Complete action library reference
   - Working examples
   - Backend comparison

2. **Update YAML DSL Documentation**
   - All action types
   - All assertion types
   - State management syntax

3. **Create Action Reference**
   - Server actions
   - Client actions
   - General actions

## Architecture Principles

### MECE Structure
- Actions are mutually exclusive by category:
  - Server Actions (RCON execution)
  - Client Actions (Mineflayer execution)
  - General Actions (wait, state management)
  
### Separation of Concerns
- Backend interface abstracts execution
- Parser handles YAML → Action mapping
- Orchestrator coordinates execution
- Reporter generates output

### Extensibility
- New actions via enum + case statement
- New backends via interface implementation
- New assertions via type system

## Testing Strategy

### Unit Tests
- Action parsing from YAML
- Backend command formatting
- JSON response parsing
- State management operations

### Integration Tests
- Full story execution
- Backend consistency
- Report generation

### Acceptance Tests
- Demo story execution
- Real Minecraft server interaction
- All action types verified

## Success Criteria

1. ✅ Demo story runs without errors
2. ✅ All actions execute successfully
3. ✅ Verbose mode shows all requests/responses
4. ✅ Test reports generated correctly
5. ✅ Documentation is comprehensive and accurate
6. ✅ Backend abstraction works for all backends
7. ✅ State management fully functional
8. ✅ Assertions provide clear pass/fail

## Next Steps

1. Start with Phase 1 (Fix Critical Bugs)
2. Verify fixes with demo-story.yaml
3. Proceed to Phase 2 (Complete Server Actions)
4. Continue through phases sequentially
5. Update documentation as we go

## Files to Modify

### Critical Fixes
- `src/main/java/org/cavarest/pilaf/backend/RconBackend.java`
- `src/main/java/org/cavarest/pilaf/backend/MineflayerBackend.java`
- `src/main/java/org/cavarest/pilaf/client/MineflayerClient.java`

### Action Implementation
- `src/main/java/org/cavarest/pilaf/orchestrator/TestOrchestrator.java`
- `src/main/java/org/cavarest/pilaf/model/Action.java`
- `src/main/java/org/cavarest/pilaf/parser/YamlStoryParser.java`

### Documentation
- `README.adoc`
- `docs/yaml-dsl.md`
- `docs/pilaf-architecture.md`
- New: `docs/action-reference.md`