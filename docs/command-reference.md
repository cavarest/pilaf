# PILAF Command Reference

Complete documentation for all PILAF actions organized by category.

## Table of Contents

1. [Output System](#output-system) - Types and referencing
2. [Server Commands](#server-commands) - RCON-based server operations
3. [Player Commands](#player-commands) - Mineflayer client actions
4. [Workflow Commands](#workflow-commands) - Control flow and state management
5. [Assertion Commands](#assertion-commands) - Validation and verification

---

## Output System

PILAF supports a GitHub Actions-style output system that allows actions to produce outputs that can be referenced by subsequent actions.

### Output Types

| Type | Description | Example |
|------|-------------|---------|
| `string` | Plain text response | RCON command output |
| `number` | Numeric value | Health, position, count |
| `boolean` | True/false | Existence checks, assertions |
| `json` | Structured object | Player data, entity list |
| `array` | List of items | Inventory, entities |

### Output Referencing

Outputs are stored using the `stepId` parameter and referenced using `${{ steps.<stepId>.outputs.<outputName> }}` syntax:

```yaml
steps:
  - action: "GET_PLAYER_POSITION"
    id: "get_start_pos"
    player: "pilaf_tester"
    storeAs: "start_position"

  - action: "MOVE_PLAYER"
    player: "pilaf_tester"
    location: [110, 65, 110]

  - action: "GET_PLAYER_POSITION"
    id: "get_end_pos"
    player: "pilaf_tester"
    storeAs: "end_position"

  - action: "COMPARE_STATES"
    name: "Compare positions"
    state1: "${{ steps.get_start_pos.outputs.result }}"
    state2: "${{ steps.get_end_pos.outputs.result }}"
```

### Output Variables

Each action produces standard outputs:

| Output | Type | Description |
|--------|------|-------------|
| `result` | varies | Primary output (stored value) |
| `status` | string | `"success"` or `"failure"` |
| `message` | string | Human-readable status message |
| `duration` | number | Execution time in milliseconds |

---

## Server Commands

Server commands are executed via RCON (Remote Console) on the Minecraft server. These commands interact directly with the server engine and require operator privileges for most operations.

### EXECUTE_RCON_COMMAND

Execute any RCON command on the server.

**YAML Syntax:**
```yaml
- action: "EXECUTE_RCON_COMMAND"
  id: "unique_step_id"              # Optional: for output referencing
  name: "Human-readable name"
  command: "minecraft_command"
  args: ["arg1", "arg2", "arg3"]
```

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `action` | string | Yes | Must be `"EXECUTE_RCON_COMMAND"` |
| `id` | string | No | Step ID for output referencing |
| `name` | string | No | Descriptive name for logging |
| `command` | string | Yes | The RCON command to execute |
| `args` | list | No | Arguments to pass to the command |

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response from server |
| `status` | string | `"success"` or `"failure"` |
| `message` | string | Server response message |

**Example:**
```yaml
- action: "EXECUTE_RCON_COMMAND"
  id: "op_player"
  name: "Op the test player"
  command: "op"
  args: ["pilaf_tester"]

# Reference output
- action: "ASSERT_RESPONSE_CONTAINS"
  name: "Verify op succeeded"
  source: "${{ steps.op_player.outputs.result }}"
  contains: "is now operator"
```

---

### EXECUTE_RCON_WITH_CAPTURE

Execute an RCON command and capture its output for state management.

**YAML Syntax:**
```yaml
- action: "EXECUTE_RCON_WITH_CAPTURE"
  id: "get_server_version"
  name: "Get server info"
  command: "version"
  storeAs: "server_version"         # Legacy: use id instead
```

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `action` | string | Yes | Must be `"EXECUTE_RCON_WITH_CAPTURE"` |
| `id` | string | No | Step ID for output referencing |
| `name` | string | No | Descriptive name |
| `command` | string | Yes | The RCON command |
| `storeAs` | string | No | **Deprecated**: Use `id` instead |

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Full command output |
| `status` | string | `"success"` or `"failure"` |
| `message` | string | Response message |

**Example:**
```yaml
- action: "EXECUTE_RCON_WITH_CAPTURE"
  id: "get_world_time"
  name: "Get world time"
  command: "time query gametime"

# Reference as number using JSONPath
- action: "EXTRACT_WITH_JSONPATH"
  id: "extract_time"
  name: "Extract time value"
  sourceVariable: "${{ steps.get_world_time.outputs.result }}"
  jsonPath: "$"
  storeAs: "time_value"
```

---

### SERVER_LOG_OUTPUT

Capture and assert on the Minecraft server log. This is unique in that it reads from the server's log file rather than RCON responses or player output.

**YAML Syntax:**
```yaml
- action: "SERVER_LOG_OUTPUT"
  id: "check_plugin_load"
  name: "Check plugin loaded"
  pattern: "Plugin.*loaded successfully"
  timeout: 5000
```

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `action` | string | Yes | Must be `"SERVER_LOG_OUTPUT"` |
| `id` | string | No | Step ID for output referencing |
| `name` | string | No | Descriptive name |
| `pattern` | string | Yes | Regex pattern to match |
| `timeout` | int | No | Max time to wait (ms) |
| `negated` | boolean | No | If true, assert pattern NOT found |

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Matched log lines |
| `count` | number | Number of matches |
| `status` | string | `"success"` or `"failure"` |
| `message` | string | Match status message |

**Example:**
```yaml
# Wait for and capture plugin load message
- action: "SERVER_LOG_OUTPUT"
  id: "wait_for_plugin"
  name: "Wait for MyPlugin"
  pattern: "MyPlugin.*v[0-9.]+ loaded"
  timeout: 30000

# Assert plugin loaded (using negated output)
- action: "ASSERT_LOG_CONTAINS"
  name: "Verify plugin loaded"
  source: "${{ steps.wait_for_plugin.outputs.result }}"
  contains: "MyPlugin"
```

---

### GET_SERVER_INFO

Retrieve server information including version, plugins, and status.

**YAML Syntax:**
```yaml
- action: "GET_SERVER_INFO"
  id: "get_server_info"
  name: "Get server status"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Server info object |
| `version` | string | Server version |
| `plugins` | array | List of loaded plugins |
| `players` | number | Online player count |
| `status` | string | `"success"` or `"failure"` |

**Example Response:**
```json
{
  "version": "Paper 1.21.8",
  "onlinePlayers": 1,
  "plugins": ["EssentialsX", "WorldEdit", "Vault"],
  "uptime": "2m 30s"
}
```

---

### GET_PLUGIN_STATUS

Check the status of a specific plugin.

**YAML Syntax:**
```yaml
- action: "GET_PLUGIN_STATUS"
  id: "check_worldedit"
  name: "Check WorldEdit status"
  plugin: "WorldEdit"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Plugin status object |
| `loaded` | boolean | Whether plugin is loaded |
| `version` | string | Plugin version |
| `status` | string | `"success"` or `"failure"` |

---

### EXECUTE_PLUGIN_COMMAND

Execute a command provided by a specific plugin.

**YAML Syntax:**
```yaml
- action: "EXECUTE_PLUGIN_COMMAND"
  id: "teleport_player"
  name: "Execute plugin command"
  plugin: "Essentials"
  command: "/tpa"
  args: ["pilaf_tester", "target_player"]
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Plugin command response |
| `status` | string | `"success"` or `"failure"` |

---

### MAKE_OPERATOR

Grant operator status to a player.

**YAML Syntax:**
```yaml
- action: "MAKE_OPERATOR"
  id: "make_op"
  name: "Make player operator"
  player: "player_name"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `status` | boolean | True if successful |
| `message` | string | Status message |

---

### GET_WORLD_TIME

Retrieve the current world time.

**YAML Syntax:**
```yaml
- action: "GET_WORLD_TIME"
  id: "get_time"
  name: "Get current time"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | number | Current time in ticks (0-24000) |
| `day` | number | Day number |
| `timeOfDay` | string | Time as string ("12:00 PM") |
| `status` | string | `"success"` or `"failure"` |

---

### GET_WEATHER

Retrieve the current weather status.

**YAML Syntax:**
```yaml
- action: "GET_WEATHER"
  id: "check_weather"
  name: "Check weather"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Weather state (`clear`, `rain`, `thunder`) |
| `duration` | number | Remaining duration in seconds |
| `status` | string | `"success"` or `"failure"` |

---

### SET_WEATHER

Set the weather on the server.

**YAML Syntax:**
```yaml
- action: "SET_WEATHER"
  id: "set_clear"
  name: "Set clear weather"
  weather: "clear"
  duration: 600
```

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `action` | string | Yes | Must be `"SET_WEATHER"` |
| `id` | string | No | Step ID |
| `name` | string | No | Descriptive name |
| `weather` | string | Yes | `clear`, `rain`, or `thunder` |
| `duration` | int | No | Duration in seconds |

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `status` | boolean | True if successful |
| `message` | string | Result message |

---

### SPAWN_ENTITY

Spawn an entity at a specific location.

**YAML Syntax:**
```yaml
- action: "SPAWN_ENTITY"
  id: "spawn_zombie"
  name: "Spawn a zombie"
  entityType: "minecraft:zombie"
  location: [100, 65, 100]
  customName: "test_zombie"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Spawn result |
| `uuid` | string | Entity UUID |
| `entityId` | number | Entity ID |
| `status` | boolean | True if spawned |
| `message` | string | Spawn message |

---

### REMOVE_ENTITIES

Remove entities matching specific criteria.

**YAML Syntax:**
```yaml
- action: "REMOVE_ENTITIES"
  id: "remove_zombies"
  name: "Remove spawned zombies"
  entityType: "zombie"
  position: [100, 65, 100]
  radius: 10
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Removal result |
| `count` | number | Number removed |
| `status` | boolean | True if successful |
| `message` | string | Result message |

---

### SET_ENTITY_HEALTH

Set the health of a specific entity.

**YAML Syntax:**
```yaml
- action: "SET_ENTITY_HEALTH"
  id: "set_health"
  name: "Set zombie health"
  entity: "test_zombie"
  health: 10
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `status` | boolean | True if successful |
| `message` | string | Result message |

---

### GET_ENTITY_HEALTH

Get the current health of an entity.

**YAML Syntax:**
```yaml
- action: "GET_ENTITY_HEALTH"
  id: "get_health"
  name: "Check zombie health"
  entity: "test_zombie"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | number | Current health (half-hearts) |
| `maxHealth` | number | Maximum health |
| `status` | string | `"success"` or `"failure"` |

---

### DAMAGE_ENTITY

Deal damage to an entity.

**YAML Syntax:**
```yaml
- action: "DAMAGE_ENTITY"
  id: "damage_zombie"
  name: "Damage zombie"
  entity: "test_zombie"
  damage: 5
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `newHealth` | number | Health after damage |
| `status` | boolean | True if entity damaged |
| `message` | string | Result message |

---

### CLEAR_INVENTORY

Clear a player's inventory.

**YAML Syntax:**
```yaml
- action: "CLEAR_INVENTORY"
  id: "clear_inv"
  name: "Clear player inventory"
  player: "player_name"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `count` | number | Items cleared |
| `status` | boolean | True if successful |
| `message` | string | Result message |

---

## Player Commands

Player commands interact with the Minecraft client via the Mineflayer bridge. These simulate real player actions and interactions.

### CONNECT_PLAYER

Connect a player to the server.

**YAML Syntax:**
```yaml
- action: "CONNECT_PLAYER"
  id: "connect_player"
  name: "Connect test player"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Connection result |
| `connected` | boolean | True if connected |
| `uuid` | string | Player UUID |
| `status` | string | `"success"` or `"failure"` |

---

### DISCONNECT_PLAYER

Disconnect a player from the server.

**YAML Syntax:**
```yaml
- action: "DISCONNECT_PLAYER"
  id: "disconnect_player"
  name: "Disconnect test player"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Disconnection result |
| `disconnected` | boolean | True if disconnected |
| `status` | string | `"success"` or `"failure"` |

---

### SEND_CHAT_MESSAGE

Send a chat message as a player.

**YAML Syntax:**
```yaml
- action: "SEND_CHAT_MESSAGE"
  id: "send_msg"
  name: "Send test message"
  player: "pilaf_tester"
  message: "Hello from PILAF!"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Send result |
| `sent` | boolean | True if sent |
| `messageId` | string | Message ID |
| `status` | string | `"success"` or `"failure"` |

---

### MOVE_PLAYER

Move a player to specific coordinates.

**YAML Syntax:**
```yaml
- action: "MOVE_PLAYER"
  id: "move_player"
  name: "Teleport to spawn"
  player: "pilaf_tester"
  location: [100, 65, 100]
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Move result |
| `x` | number | New X coordinate |
| `y` | number | New Y coordinate |
| `z` | number | New Z coordinate |
| `status` | boolean | True if moved |

---

### GET_PLAYER_POSITION

Get a player's current position.

**YAML Syntax:**
```yaml
- action: "GET_PLAYER_POSITION"
  id: "get_pos"
  name: "Get player location"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Position object |
| `x` | number | X coordinate |
| `y` | number | Y coordinate |
| `z` | number | Z coordinate |
| `yaw` | number | Yaw rotation |
| `pitch` | number | Pitch rotation |
| `world` | string | World name |

**Example Response:**
```json
{
  "x": 105.5,
  "y": 65.0,
  "z": 100.3,
  "yaw": 180.0,
  "pitch": 0.0,
  "world": "world"
}
```

---

### GET_PLAYER_HEALTH

Get a player's current health.

**YAML Syntax:**
```yaml
- action: "GET_PLAYER_HEALTH"
  id: "get_health"
  name: "Check player health"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Health object |
| `health` | number | Current health (half-hearts) |
| `maxHealth` | number | Maximum health |
| `food` | number | Food level |
| `saturation` | number | Saturation level |

---

### HEAL_PLAYER

Heal a player to full health.

**YAML Syntax:**
```yaml
- action: "HEAL_PLAYER"
  id: "heal_player"
  name: "Heal the player"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `health` | number | Health after heal |
| `status` | boolean | True if successful |

---

### GET_PLAYER_INVENTORY

Get a player's inventory contents.

**YAML Syntax:**
```yaml
- action: "GET_PLAYER_INVENTORY"
  id: "get_inventory"
  name: "Check inventory"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Inventory object |
| `items` | array | List of items |
| `hotbar` | array | Hotbar items |
| `armor` | array | Armor slots |
| `offhand` | object | Offhand item |
| `size` | number | Total slots |

**Example Response:**
```json
{
  "items": [
    {"slot": 0, "id": "diamond_sword", "count": 1, "damage": 0},
    {"slot": 1, "id": "apple", "count": 5, "damage": 0}
  ],
  "hotbar": [...],
  "armor": [...],
  "offhand": null,
  "size": 36
}
```

---

### GIVE_ITEM

Give an item to a player.

**YAML Syntax:**
```yaml
- action: "GIVE_ITEM"
  id: "give_sword"
  name: "Give diamond sword"
  player: "pilaf_tester"
  item: "diamond_sword"
  count: 1
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `slot` | number | Slot where item was placed |
| `status` | boolean | True if successful |

---

### REMOVE_ITEM

Remove an item from a player's inventory.

**YAML Syntax:**
```yaml
- action: "REMOVE_ITEM"
  id: "remove_apple"
  name: "Remove apples"
  player: "pilaf_tester"
  item: "apple"
  count: 5
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `removed` | number | Items removed |
| `status` | boolean | True if removed |

---

### EQUIP_ITEM

Equip an item in a specific slot.

**YAML Syntax:**
```yaml
- action: "EQUIP_ITEM"
  id: "equip_sword"
  name: "Equip diamond sword"
  player: "pilaf_tester"
  item: "diamond_sword"
  slot: "hand"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | Command response |
| `equipped` | boolean | True if equipped |
| `slot` | string | Slot where equipped |
| `status` | string | `"success"` or `"failure"` |

---

### GET_PLAYER_EQUIPMENT

Get a player's equipped items.

**YAML Syntax:**
```yaml
- action: "GET_PLAYER_EQUIPMENT"
  id: "get_equipment"
  name: "Check equipment"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Equipment object |
| `hand` | object | Main hand item |
| `offhand` | object | Offhand item |
| `head` | object | Helmet |
| `chest` | object | Chestplate |
| `legs` | object | Leggings |
| `feet` | object | Boots |

---

### ASSERT_PLAYER_HAS_ITEM

Assert that a player has a specific item.

**YAML Syntax:**
```yaml
- action: "ASSERT_PLAYER_HAS_ITEM"
  id: "assert_sword"
  name: "Verify item received"
  player: "pilaf_tester"
  item: "diamond_sword"
  count: 1
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Assertion result |
| `hasItem` | boolean | True if player has item |
| `foundCount` | number | Actual count |
| `passed` | boolean | True if assertion passed |
| `message` | string | Result message |

---

### EXECUTE_PLAYER_COMMAND

Execute a command as a player.

**YAML Syntax:**
```yaml
- action: "EXECUTE_PLAYER_COMMAND"
  id: "exec_home"
  name: "Execute home command"
  player: "pilaf_tester"
  command: "/home"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Command result |
| `executed` | boolean | True if executed |
| `chatMessage` | string | Message sent to chat |
| `status` | string | `"success"` or `"failure"` |

---

### GET_ENTITIES

Get all entities visible to a player.

**YAML Syntax:**
```yaml
- action: "GET_ENTITIES"
  id: "get_entities"
  name: "Scan for entities"
  player: "pilaf_tester"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Entities object |
| `entities` | array | List of entities |
| `count` | number | Number of entities |
| `types` | object | Count by type |

**Example Response:**
```json
{
  "entities": [
    {"id": 1, "type": "zombie", "name": "test_zombie", "x": 105, "y": 65, "z": 100},
    {"id": 2, "type": "cow", "name": "test_cow", "x": 95, "y": 65, "z": 95}
  ],
  "count": 2,
  "types": {"zombie": 1, "cow": 1}
}
```

---

### WAIT_FOR_ENTITY_SPAWN

Wait for an entity to spawn.

**YAML Syntax:**
```yaml
- action: "WAIT_FOR_ENTITY_SPAWN"
  id: "wait_spawn"
  name: "Wait for spawn"
  entityType: "zombie"
  position: [100, 65, 100]
  radius: 20
  timeout: 10000
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Spawn result |
| `found` | boolean | True if found |
| `entity` | object | Entity data |
| `waitTime` | number | Time waited (ms) |

---

### WAIT_FOR_CHAT_MESSAGE

Wait for a specific chat message.

**YAML Syntax:**
```yaml
- action: "WAIT_FOR_CHAT_MESSAGE"
  id: "wait_chat"
  name: "Wait for message"
  player: "pilaf_tester"
  pattern: ".*test.*"
  timeout: 5000
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Message result |
| `found` | boolean | True if found |
| `message` | string | Matched message |
| `sender` | string | Message sender |

---

## Workflow Commands

Workflow commands control the test flow, state management, and data handling.

### WAIT

Pause execution for a specified duration.

**YAML Syntax:**
```yaml
- action: "WAIT"
  id: "wait_5s"
  name: "Wait for server"
  duration: 5000
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | string | "Wait completed" |
| `waited` | number | Duration waited (ms) |
| `status` | string | `"success"` |

---

### CHECK_SERVICE_HEALTH

Check if a service is healthy and available.

**YAML Syntax:**
```yaml
- action: "CHECK_SERVICE_HEALTH"
  id: "check_health"
  name: "Check server health"
  source: "http://localhost:3000/health"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Health check result |
| `healthy` | boolean | True if service is healthy |
| `latency` | number | Response time (ms) |
| `status` | string | `"success"` or `"failure"` |

---

### STORE_STATE

Store the result of an action for later comparison.

**YAML Syntax:**
```yaml
- action: "STORE_STATE"
  id: "store_inv"
  name: "Store initial inventory"
  fromAction: "GET_PLAYER_INVENTORY"
  storeAs: "initial_inventory"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | varies | Stored value |
| `stored` | boolean | True if stored |
| `key` | string | Variable name |
| `type` | string | Data type stored |

---

### COMPARE_STATES

Compare two stored states.

**YAML Syntax:**
```yaml
- action: "COMPARE_STATES"
  id: "compare_inv"
  name: "Compare inventories"
  state1: "initial_inventory"
  state2: "final_inventory"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Comparison result |
| `equal` | boolean | True if states are equal |
| `diff` | json | Differences found |
| `added` | array | Items added |
| `removed` | array | Items removed |
| `changed` | array | Items changed |

**Example Response:**
```json
{
  "equal": false,
  "added": [{"id": "diamond_sword", "count": 1}],
  "removed": [],
  "changed": []
}
```

---

### EXTRACT_WITH_JSONPATH

Extract data using a JSONPath expression.

**YAML Syntax:**
```yaml
- action: "EXTRACT_WITH_JSONPATH"
  id: "extract_x"
  name: "Extract x coordinate"
  sourceVariable: "${{ steps.get_pos.outputs.result }}"
  jsonPath: "$.x"
  storeAs: "player_x"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | varies | Extracted value |
| `extracted` | boolean | True if extracted |
| `path` | string | JSONPath used |
| `type` | string | Type of extracted value |

---

### FILTER_ENTITIES

Filter entities by criteria.

**YAML Syntax:**
```yaml
- action: "FILTER_ENTITIES"
  id: "filter_zombies"
  name: "Filter zombies"
  sourceVariable: "${{ steps.get_entities.outputs.result }}"
  filterType: "type"
  filterValue: "zombie"
  storeAs: "zombies"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | array | Filtered entities |
| `count` | number | Number of matches |
| `filtered` | boolean | True if filtered |
| `criteria` | object | Filter criteria used |

---

## Assertion Commands

Assertions validate expected outcomes and can fail tests if conditions aren't met.

### ASSERT_ENTITY_EXISTS

Assert that an entity exists.

**YAML Syntax:**
```yaml
- action: "ASSERT_ENTITY_EXISTS"
  id: "assert_zombie"
  name: "Verify zombie spawned"
  entity: "test_zombie"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Assertion result |
| `exists` | boolean | True if entity exists |
| `entity` | object | Entity data if found |
| `passed` | boolean | True if assertion passed |
| `message` | string | Result message |

---

### ASSERT_ENTITY_MISSING

Assert that an entity no longer exists.

**YAML Syntax:**
```yaml
- action: "ASSERT_ENTITY_MISSING"
  id: "assert_removed"
  name: "Verify entity removed"
  entity: "dead_zombie"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Assertion result |
| `missing` | boolean | True if entity is missing |
| `passed` | boolean | True if assertion passed |
| `message` | string | Result message |

---

### ASSERT_RESPONSE_CONTAINS

Assert that a response contains expected text.

**YAML Syntax:**
```yaml
- action: "ASSERT_RESPONSE_CONTAINS"
  id: "assert_success"
  name: "Verify command output"
  source: "${{ steps.execute_command.outputs.result }}"
  contains: "Success"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Assertion result |
| `contains` | boolean | True if text found |
| `matchedText` | string | Text that matched |
| `passed` | boolean | True if assertion passed |
| `message` | string | Result message |

---

### ASSERT_LOG_CONTAINS

Assert that server log contains expected text. This is the primary way to assert on server-side events.

**YAML Syntax:**
```yaml
- action: "ASSERT_LOG_CONTAINS"
  id: "assert_plugin"
  name: "Verify plugin loaded"
  source: "${{ steps.wait_for_plugin.outputs.result }}"
  contains: "MyPlugin loaded"
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Assertion result |
| `found` | boolean | True if pattern found |
| `count` | number | Number of matches |
| `passed` | boolean | True if assertion passed |
| `message` | string | Result message |

---

### ASSERT_JSON_EQUALS

Assert that JSON data matches expected values.

**YAML Syntax:**
```yaml
- action: "ASSERT_JSON_EQUALS"
  id: "assert_health"
  name: "Verify player health"
  sourceVariable: "${{ steps.get_health.outputs.result }}"
  expected: '{"health": 20, "maxHealth": 20}'
```

**Outputs:**
| Output | Type | Description |
|--------|------|-------------|
| `result` | json | Assertion result |
| `equal` | boolean | True if JSON matches |
| `diff` | json | Differences if any |
| `passed` | boolean | True if assertion passed |
| `message` | string | Result message |

---

## Output Reference Examples

### Capturing and Comparing Player Position

```yaml
- action: "GET_PLAYER_POSITION"
  id: "get_start"
  name: "Store start position"
  player: "pilaf_tester"

- action: "MOVE_PLAYER"
  name: "Move player"
  player: "pilaf_tester"
  location: [110, 65, 110]

- action: "GET_PLAYER_POSITION"
  id: "get_end"
  name: "Store end position"
  player: "pilaf_tester"

- action: "COMPARE_STATES"
  name: "Verify movement"
  state1: "${{ steps.get_start.outputs.result }}"
  state2: "${{ steps.get_end.outputs.result }}"
```

### Using JSONPath to Extract Values

```yaml
- action: "GET_PLAYER_INVENTORY"
  id: "get_inv"
  name: "Get inventory"
  player: "pilaf_tester"

- action: "EXTRACT_WITH_JSONPATH"
  id: "count_swords"
  name: "Count swords"
  sourceVariable: "${{ steps.get_inv.outputs.result }}"
  jsonPath: "$.items[?(@.id=='diamond_sword')].count"
  storeAs: "sword_count"

- action: "ASSERT"
  name: "Has sword"
  condition: "${{ steps.count_swords.outputs.result }} > 0"
```

### Asserting on Server Log

```yaml
- action: "EXECUTE_PLUGIN_COMMAND"
  id: "run_command"
  name: "Run custom command"
  plugin: "MyPlugin"
  command: "/mycommand"

- action: "SERVER_LOG_OUTPUT"
  id: "check_log"
  name: "Check for plugin message"
  pattern: "MyPlugin.*processed command"
  timeout: 5000

- action: "ASSERT_LOG_CONTAINS"
  name: "Verify command processed"
  source: "${{ steps.check_log.outputs.result }}"
  contains: "processed"
```

---

## Correlating Action Outputs with Server Log Assertions

A precise pattern for capturing server logs between two points:

1. **CAPTURE_START** - Get current log position (line number or offset)
2. **Execute Action** - The action being tested
3. **CAPTURE_STOP** - Get new log position
4. **EXTRACT_LOGS** - Extract logs between start and stop
5. **ASSERT** - Make assertions on the captured logs

### Pattern: Start/Stop Log Capture

```yaml
# Step 1: Capture start position (current line number)
- action: "CAPTURE_LOG_START"
  id: "log_start"
  name: "Mark log start position"

# Step 2: Execute the action under test
- action: "EXECUTE_RCON_COMMAND"
  id: "run_command"
  name: "Execute plugin command"
  command: "kill"
  args: ["@e[type=zombie,name=test_zombie]"]

# Step 3: Capture stop position
- action: "CAPTURE_LOG_STOP"
  id: "log_stop"
  name: "Mark log stop position"

# Step 4: Extract logs between start and stop
- action: "EXTRACT_LOG_RANGE"
  id: "extract_logs"
  name: "Extract action logs"
  startFrom: "${{ steps.log_start.outputs.position }}"
  stopAt: "${{ steps.log_stop.outputs.position }}"

# Step 5: Assert on the extracted logs
- action: "ASSERT_LOG_CONTAINS"
  id: "verify_log"
  name: "Verify death message"
  source: "${{ steps.extract_logs.outputs.result }}"
  contains: "test_zombie died"
```

### Alternative: Marker-Based Capture

Use unique markers to identify the log range:

```yaml
# Step 1: Generate unique start marker
- action: "EXECUTE_RCON_COMMAND"
  id: "start_marker"
  name: "Set start marker"
  command: "say"
  args: ["[PILAF-LOG-START-{{ timestamp }}]"]

# Step 2: Execute action
- action: "SPAWN_ENTITY"
  id: "spawn"
  name: "Spawn entity"
  entityType: "minecraft:zombie"
  location: [100, 65, 100]

# Step 3: Generate stop marker
- action: "EXECUTE_RCON_COMMAND"
  id: "stop_marker"
  name: "Set stop marker"
  command: "say"
  args: ["[PILAF-LOG-STOP-{{ timestamp }}]"]

# Step 4: Extract logs between markers
- action: "EXTRACT_LOG_BETWEEN_MARKERS"
  id: "extract"
  name: "Extract marked logs"
  startPattern: "\[PILAF-LOG-START-.*\]"
  stopPattern: "\[PILAF-LOG-STOP-.*\]"

# Step 5: Assert on captured logs
- action: "ASSERT_LOG_CONTAINS"
  id: "verify_spawn"
  name: "Verify spawn logged"
  source: "${{ steps.extract.outputs.result }}"
  contains: "zombie"
```

### Key Log Actions

| Action | Description | Outputs |
|--------|-------------|---------|
| `CAPTURE_LOG_START` | Mark start position | `position`, `timestamp` |
| `CAPTURE_LOG_STOP` | Mark stop position | `position`, `timestamp` |
| `EXTRACT_LOG_RANGE` | Extract between positions | `result`, `lines` |
| `EXTRACT_LOG_BETWEEN_MARKERS` | Extract between markers | `result`, `lines` |
| `ASSERT_LOG_CONTAINS` | Assert on log content | `passed`, `count`, `matchedText` |

### Outputs Reference

| Output | Type | Description |
|--------|------|-------------|
| `${{ steps.action.outputs.position }}` | number | Log line number |
| `${{ steps.action.outputs.timestamp }}` | string | Timestamp at capture |
| `${{ steps.extract.outputs.result }}` | string | Extracted log content |
| `${{ steps.extract.outputs.lines }}` | number | Number of lines extracted |
| `${{ steps.assert.outputs.passed }}` | boolean | Assertion passed |
| `${{ steps.assert.outputs.count }}` | number | Match count |

---

## Complete Action Output Summary

| Action | Result Type | Status Output | Key Outputs |
|--------|-------------|---------------|-------------|
| `EXECUTE_RCON_COMMAND` | string | boolean | `result`, `message` |
| `EXECUTE_RCON_WITH_CAPTURE` | string | boolean | `result`, `message` |
| `SERVER_LOG_OUTPUT` | string | boolean | `result`, `count`, `message` |
| `GET_SERVER_INFO` | json | boolean | `version`, `plugins`, `players` |
| `GET_PLUGIN_STATUS` | json | boolean | `loaded`, `version` |
| `SPAWN_ENTITY` | json | boolean | `uuid`, `entityId` |
| `REMOVE_ENTITIES` | json | boolean | `count` |
| `GET_ENTITY_HEALTH` | number | string | `result`, `maxHealth` |
| `GET_PLAYER_POSITION` | json | string | `x`, `y`, `z`, `yaw`, `pitch` |
| `GET_PLAYER_HEALTH` | json | string | `health`, `maxHealth`, `food` |
| `GET_PLAYER_INVENTORY` | json | string | `items`, `hotbar`, `armor`, `size` |
| `GET_ENTITIES` | json | string | `entities`, `count`, `types` |
| `GET_WORLD_TIME` | number | string | `result`, `day` |
| `GET_WEATHER` | string | string | `result`, `duration` |
| `CONNECT_PLAYER` | json | boolean | `connected`, `uuid` |
| `DISCONNECT_PLAYER` | json | boolean | `disconnected` |
| `SEND_CHAT_MESSAGE` | json | boolean | `sent`, `messageId` |
| `COMPARE_STATES` | json | boolean | `equal`, `diff`, `added`, `removed` |
| `ASSERT_*` | json | boolean | `passed`, `message` |
