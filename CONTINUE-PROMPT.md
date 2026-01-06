# PILAF Implementation Continuation Prompt

## Context

You are continuing work on PILAF (Paper Integration Layer for Automation Functions), a YAML-driven testing framework for Minecraft PaperMC plugins. The framework allows developers to write integration tests in simple YAML format instead of complex Java code.

## Current State

### What's Working
- ✅ Docker infrastructure (PaperMC 1.21.8 + Mineflayer bridge)
- ✅ RCON authentication and basic commands
- ✅ Player connection via Mineflayer HTTP bridge
- ✅ Verbose logging showing raw RCON/HTTP requests and responses
- ✅ Test report generation
- ✅ Basic action execution (WAIT, CONNECT_PLAYER, GET_PLAYER_POSITION)

### Critical Bugs to Fix First

1. **RCON Trailing Space Bug** (HIGHEST PRIORITY)
   - Location: `src/main/java/org/cavarest/pilaf/backend/RconBackend.java` line 107-109
   - Problem: `executeServerCommand()` adds trailing space even when arguments list is empty
   - Current code:
     ```java
     public void executeServerCommand(String command, List<String> arguments) {
         rcon.executeCommand(command + " " + String.join(" ", arguments));
     }
     ```
   - Fix needed:
     ```java
     public void executeServerCommand(String command, List<String> arguments) {
         String fullCommand = command;
         if (arguments != null && !arguments.isEmpty()) {
             fullCommand = command + " " + String.join(" ", arguments);
         }
         rcon.executeCommand(fullCommand);
     }
     ```
   - Same fix needed in `MineflayerBackend.java` line 141-143

2. **JSON Parsing Error** (HIGH PRIORITY)
   - Location: `src/main/java/org/cavarest/pilaf/client/MineflayerClient.java` lines 170-192
   - Problem: Simple JSON parser can't handle nested objects/arrays from Mineflayer
   - Current error: `For input string: "{"x":-1.6265173605548293"`
   - Fix needed: Use proper JSON library (Jackson already in dependencies) or improve parser
   - Affects: GET_ENTITIES, GET_INVENTORY actions

3. **GET_INVENTORY Action Mapping** (MEDIUM PRIORITY)
   - Location: `src/main/java/org/cavarest/pilaf/orchestrator/TestOrchestrator.java`
   - Status: Partially fixed - GET_INVENTORY now maps to GET_PLAYER_INVENTORY
   - Needs: Testing to verify it works after JSON parser fix

## Your Task

**Implement the complete PILAF action library following MECE principles and proper OOP architecture.**

### Phase 1: Fix Critical Bugs (DO THIS FIRST)

1. Fix RCON trailing space in both RconBackend and MineflayerBackend
2. Fix JSON parsing in MineflayerClient (use Jackson or improve parser)
3. Test demo-story.yaml runs without errors

### Phase 2: Complete Server Actions

Implement these in `TestOrchestrator.java` executeAction() switch statement:

**Player Management** (via RCON):
- `GAMEMODE_CHANGE` - Change player gamemode
- `CLEAR_INVENTORY` - Clear player inventory  
- `SET_SPAWN_POINT` - Set player spawn point
- `TELEPORT_PLAYER` - Teleport player
- `SET_PLAYER_HEALTH` - Set player health
- `KILL_PLAYER` - Kill player

**Entity Management** (via RCON):
- `SPAWN_ENTITY` - Spawn entity at location
- `KILL_ENTITY` - Kill entity
- `SET_ENTITY_HEALTH` - Set entity health
- `GET_ENTITY_HEALTH` - Get entity health

**World Management** (via RCON):
- `SET_WEATHER` - Set weather
- `GET_WEATHER` - Get weather (already in MineflayerBackend)
- `SET_TIME` - Set world time
- `GET_WORLD_TIME` - Get world time (already in MineflayerBackend)

### Phase 3: Complete Client Actions

Implement these in `TestOrchestrator.java` and add corresponding methods to `MineflayerBackend.java`:

**Movement**:
- `MOVE_PLAYER` - Move to coordinates (already exists, verify)
- `LOOK_AT` - Look at target
- `JUMP` - Make player jump

**Interaction**:
- `USE_ITEM` - Use held item
- `ATTACK_ENTITY` - Attack entity
- `BREAK_BLOCK` - Break block
- `PLACE_BLOCK` - Place block

**Communication**:
- `SEND_CHAT_MESSAGE` - Already implemented
- `GET_CHAT_HISTORY` - Get recent chat

### Phase 4: State Management

Implement in `TestOrchestrator.java`:

- `STORE_STATE` - Already partially implemented
- `PRINT_STORED_STATE` - Already partially implemented
- `COMPARE_STATES` - Already partially implemented
- `PRINT_STATE_COMPARISON` - Already partially implemented
- `EXTRACT_WITH_JSONPATH` - Extract using JSONPath
- `FILTER_ENTITIES` - Filter entities by criteria

### Phase 5: Assertions

Add new assertion types to `Assertion.java` and implement evaluation:

- `ASSERT_ENTITY_EXISTS`
- `ASSERT_ENTITY_MISSING`
- `ASSERT_PLAYER_HAS_ITEM`
- `ASSERT_RESPONSE_CONTAINS`
- `ASSERT_JSON_EQUALS`

### Phase 6: Documentation

1. Update `README.adoc`:
   - Add complete action reference
   - Add working examples
   - Add state management examples

2. Create `docs/action-reference.md`:
   - Complete reference of all actions
   - Parameters for each
   - Examples for each

3. Update `docs/yaml-dsl.md`:
   - All action types
   - Assertion types
   - State management syntax

## Architecture Principles

### MECE (Mutually Exclusive, Collectively Exhaustive)
- Actions are categorized: Server (RCON), Client (Mineflayer), General (flow control)
- No overlap between categories
- All necessary actions covered

### Object-Oriented Design
- Backend interface abstracts execution details
- Each backend implements the interface
- Orchestrator coordinates without knowing implementation

### Separation of Concerns
- Parser: YAML → Action objects
- Orchestrator: Executes actions via backend
- Backend: Implements execution (RCON, HTTP, etc.)
- Reporter: Generates output reports

### Open/Closed Principle
- New actions: Add enum + case statement
- New backends: Implement interface
- New assertions: Add type + evaluation logic

## Files You'll Work With

### Primary Implementation Files
- `src/main/java/org/cavarest/pilaf/orchestrator/TestOrchestrator.java` - Main execution
- `src/main/java/org/cavarest/pilaf/backend/MineflayerBackend.java` - Client actions
- `src/main/java/org/cavarest/pilaf/backend/RconBackend.java` - Server actions
- `src/main/java/org/cavarest/pilaf/model/Action.java` - Action enum
- `src/main/java/org/cavarest/pilaf/model/Assertion.java` - Assertion types
- `src/main/java/org/cavarest/pilaf/parser/YamlStoryParser.java` - YAML parsing

### Support Files
- `src/main/java/org/cavarest/pilaf/client/MineflayerClient.java` - HTTP client
- `src/main/java/org/cavarest/pilaf/rcon/RconClient.java` - RCON client

### Test Files
- `demo-story.yaml` - Demonstration story
- `test-story-*.yaml` - Test stories

## Testing Commands

Start Docker:
```bash
cd /Users/mulgogi/src/cavarest/pilaf/docker && docker-compose -f docker-compose.pilaf.yml up -d
```

Wait for server:
```bash
for i in {1..60}; do
  if docker exec pilaf-papermc rcon-cli list >/dev/null 2>&1; then
    echo "✅ PaperMC is ready!"
    break
  fi
  sleep 3
done
```

Run tests with verbose:
```bash
cd /Users/mulgogi/src/cavarest/pilaf && gradle run --args="--config config-demo.yaml demo-story.yaml --verbose"
```

Stop Docker:
```bash
cd /Users/mulgogi/src/cavarest/pilaf/docker && docker-compose -f docker-compose.pilaf.yml down
```

## Success Criteria

1. ✅ All critical bugs fixed
2. ✅ demo-story.yaml executes completely without errors
3. ✅ All ~60 actions implemented
4. ✅ State management fully working
5. ✅ Assertions implemented and tested
6. ✅ Documentation complete and accurate
7. ✅ Verbose mode shows all requests/responses clearly
8. ✅ Test reports generated correctly

## Important Notes

- Use verbose mode (`--verbose`) to see raw RCON/HTTP requests
- RCON commands for Minecraft 1.21.8 have specific syntax (no trailing spaces!)
- Mineflayer returns complex nested JSON - use proper JSON library
- Every action must have a case in TestOrchestrator.executeAction()
- Every action must be described in TestOrchestrator.describeAction()
- Follow existing patterns for consistency

## References

- See `CONTINUATION-PLAN.md` for detailed implementation phases
- See `IMPLEMENTATION-STATUS.md` for current status tracker
- See `.kilocode/rules/memory-bank/` for project architecture and context