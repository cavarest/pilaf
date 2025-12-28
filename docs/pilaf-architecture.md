# PILAF Architecture

**Paper Integration Layer for Automation Functions Framework**

## Overview

PILAF is an integration testing framework for PaperMC/Bukkit plugins that enables
automated testing of real plugin behavior in actual Minecraft server environments.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PILAF TEST FRAMEWORK                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        TEST LAYER (JUnit 5)                          │   │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                 │   │
│  │  │ DragonEggLightning   │  │ RealMinecraftClient  │                 │   │
│  │  │ UseCaseTest          │  │ IntegrationTest      │                 │   │
│  │  └──────────┬───────────┘  └──────────┬───────────┘                 │   │
│  └─────────────┼─────────────────────────┼─────────────────────────────┘   │
│                │                         │                                  │
│                ▼                         ▼                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ORCHESTRATION LAYER                               │   │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                 │   │
│  │  │ TestOrchestrator     │  │ YamlStoryParser      │                 │   │
│  │  │ (Timeline Replay)    │  │ (YAML DSL)           │                 │   │
│  │  └──────────┬───────────┘  └──────────┬───────────┘                 │   │
│  └─────────────┼─────────────────────────┼─────────────────────────────┘   │
│                │                         │                                  │
│                ▼                         ▼                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      BACKEND LAYER                                   │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐   │   │
│  │  │ PilafBackend │◀─┤ Backend      │  │ PilafBackendFactory      │   │   │
│  │  │ (Interface)  │  │ Factory      │  │ (creates backends)       │   │   │
│  │  └──────┬───────┘  └──────────────┘  └──────────────────────────┘   │   │
│  │         │                                                            │   │
│  │         ├─────────────────┬─────────────────┬───────────────────┐   │   │
│  │         ▼                 ▼                 ▼                   │   │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │   │   │
│  │  │ MockBukkit   │  │ RealServer   │  │ RealMinecraftClient  │   │   │   │
│  │  │ Backend      │  │ Backend      │  │ Backend              │   │   │   │
│  │  │ (unit tests) │  │ (RCON only)  │  │ (Player simulation)  │   │   │   │
│  │  └──────────────┘  └──────┬───────┘  └──────────┬───────────┘   │   │   │
│  └───────────────────────────┼─────────────────────┼───────────────┘   │   │
│                              │                     │                    │   │
└──────────────────────────────┼─────────────────────┼────────────────────┘   │
                               │                     │                        │
                               ▼                     ▼                        │
┌──────────────────────────────────────────────────────────────────────────────┐
│                         COMMUNICATION LAYER                                  │
│                                                                              │
│  ┌───────────────────────┐           ┌───────────────────────────────────┐  │
│  │      RconClient       │           │    MineflayerBridge (Node.js)     │  │
│  │    (Java, TCP)        │           │    HTTP/WebSocket Server          │  │
│  │    Port: 25575        │           │    Port: 3000                     │  │
│  └───────────┬───────────┘           └────────────────┬──────────────────┘  │
│              │                                        │                      │
│              │                                        │                      │
│              │                                        ▼                      │
│              │                       ┌───────────────────────────────────┐  │
│              │                       │    PilafMineflayerClient          │  │
│              │                       │    (Node.js, mineflayer lib)      │  │
│              │                       │    Connects to MC Server          │  │
│              │                       └────────────────┬──────────────────┘  │
│              │                                        │                      │
└──────────────┼────────────────────────────────────────┼──────────────────────┘
               │                                        │
               ▼                                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         MINECRAFT SERVER                                     │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    PaperMC Server (Docker)                           │   │
│  │                                                                      │   │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │   │
│  │  │ RCON Interface   │  │ Minecraft        │  │ DragonEggLightning│   │   │
│  │  │ Port: 25575      │  │ Game Port: 25565 │  │ Plugin            │   │   │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘   │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Test Layer

JUnit 5 test classes that define test scenarios.

```
src/test/java/com/dragonegg/lightning/pilaf/
├── DragonEggLightningUseCaseTest.java   # RCON-based tests
├── RealMinecraftClientIntegrationTest.java  # Player simulation tests
└── ...
```

### 2. Orchestration Layer

#### TestOrchestrator

Replays test timelines, coordinating actions between server and player.

```java
TestOrchestrator orchestrator = new TestOrchestrator(backend);
orchestrator.loadStory("lightning-ability-test.yaml");
orchestrator.execute();
TestResult result = orchestrator.getResult();
```

#### YamlStoryParser

Parses YAML DSL test stories into executable actions.

```yaml
# src/test/resources/test-stories/lightning-ability-test.yaml
story: "Lightning ability kills unarmored zombie"
steps:
  - action: spawn_entity
    entity: zombie_unarmored
    type: ZOMBIE
    location: [10, 64, 10]

  - action: player_command
    player: test_player
    command: "ability lightning zombie_unarmored"

  - assertion: entity_health
    entity: zombie_unarmored
    condition: less_than
    value: 20.0
```

### 3. Backend Layer

#### PilafBackend Interface

```java
public interface PilafBackend {
    void initialize() throws Exception;
    void cleanup() throws Exception;
    String getType();

    // Player actions
    void movePlayer(String player, String type, String dest);
    void equipItem(String player, String item, String slot);
    void giveItem(String player, String item, Integer count);
    void executePlayerCommand(String player, String cmd, List<String> args);

    // Entity management
    void spawnEntity(String name, String type, List<Double> loc, Map<String,String> equip);
    boolean entityExists(String name);
    double getEntityHealth(String name);
    void setEntityHealth(String name, Double health);

    // Server commands
    void executeServerCommand(String cmd, List<String> args);
    void removeAllTestEntities();
    void removeAllTestPlayers();
}
```

#### Backend Implementations

| Backend | Purpose | Communication |
|---------|---------|---------------|
| MockBukkitBackend | Unit tests, no server needed | In-memory |
| RealServerBackend | Server-side commands via RCON | RCON (TCP:25575) |
| RealMinecraftClientBackend | Player simulation | MineflayerBridge (HTTP:3000) |

### 4. Communication Layer

#### RconClient (Java)

Direct RCON protocol implementation for server commands.

```java
RconClient rcon = new RconClient("localhost", 25575, "dragon123");
rcon.connect();
String result = rcon.executeCommand("summon zombie ~ ~ ~");
rcon.disconnect();
```

#### MineflayerBridge (Node.js)

HTTP/WebSocket server that bridges Java tests to Mineflayer client.

```
┌──────────────────┐     HTTP      ┌────────────────────┐   MC Protocol  ┌───────────┐
│  Java Test       │──────────────▶│ MineflayerBridge   │───────────────▶│ MC Server │
│  (HTTP Client)   │◀──────────────│ (Express + WS)     │◀───────────────│           │
└──────────────────┘    JSON       └────────────────────┘   Game Events  └───────────┘
```

**Endpoints:**
- `POST /connect` - Connect player to server
- `POST /command` - Execute command as player
- `POST /move` - Move player to location
- `POST /equip` - Equip item to slot
- `GET /inventory` - Get player inventory
- `GET /health` - Get player health
- `POST /disconnect` - Disconnect player

## Data Flow

### Test Execution Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 1. Test starts                                                          │
│    └─▶ TestOrchestrator.loadStory("test.yaml")                         │
│                                                                         │
│ 2. Parse YAML into Action list                                          │
│    └─▶ YamlStoryParser.parse() → List<Action>                          │
│                                                                         │
│ 3. Execute each action                                                  │
│    ┌─────────────────────────────────────────────────────────────────┐ │
│    │ For each Action in timeline:                                     │ │
│    │                                                                  │ │
│    │   ┌─ Server Action (spawn, give, etc.) ─────────────────────┐   │ │
│    │   │   Backend.executeServerCommand(...)                      │   │ │
│    │   │     └─▶ RconClient.executeCommand(...)                   │   │ │
│    │   │           └─▶ RCON Protocol → MC Server                  │   │ │
│    │   └──────────────────────────────────────────────────────────┘   │ │
│    │                                                                  │ │
│    │   ┌─ Player Action (command, move, equip) ──────────────────┐   │ │
│    │   │   Backend.executePlayerCommand(...)                      │   │ │
│    │   │     └─▶ HTTP POST → MineflayerBridge                     │   │ │
│    │   │           └─▶ mineflayer.bot.chat("/command")            │   │ │
│    │   │                 └─▶ MC Protocol → MC Server              │   │ │
│    │   └──────────────────────────────────────────────────────────┘   │ │
│    │                                                                  │ │
│    │   ┌─ Wait Action ───────────────────────────────────────────┐   │ │
│    │   │   Thread.sleep(duration)                                 │   │ │
│    │   └──────────────────────────────────────────────────────────┘   │ │
│    │                                                                  │ │
│    │   ┌─ Assertion ─────────────────────────────────────────────┐   │ │
│    │   │   Backend.getEntityHealth(...) → assert condition        │   │ │
│    │   └──────────────────────────────────────────────────────────┘   │ │
│    └──────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ 4. Cleanup                                                              │
│    └─▶ Backend.removeAllTestEntities()                                 │
│    └─▶ Backend.removeAllTestPlayers()                                  │
│    └─▶ Backend.cleanup()                                               │
└─────────────────────────────────────────────────────────────────────────┘
```

## YAML DSL Specification

### Story Structure

```yaml
name: "Test Story Name"
description: "Description of what this test validates"
backend: "real-server"  # or "mockbukkit", "real-client"

setup:
  - action: spawn_entity
    name: target_zombie
    type: ZOMBIE
    location: [100, 64, 100]

  - action: give_item
    player: test_player
    item: dragon_egg
    count: 3

  - action: equip_item
    player: test_player
    item: dragon_egg
    slot: offhand

steps:
  - action: player_command
    player: test_player
    command: ability
    args: [lightning, target_zombie]

  - action: wait
    duration: 1000  # milliseconds

assertions:
  - type: entity_health
    entity: target_zombie
    condition: less_than
    value: 20.0

  - type: entity_exists
    entity: target_zombie
    expected: false  # zombie should be dead

cleanup:
  - action: remove_entities
    pattern: "target_*"

  - action: remove_players
    pattern: "test_*"
```

### Action Types

| Action | Parameters | Description |
|--------|------------|-------------|
| `spawn_entity` | name, type, location, equipment | Spawn entity at location |
| `give_item` | player, item, count | Give items to player |
| `equip_item` | player, item, slot | Equip item to slot |
| `player_command` | player, command, args | Execute command as player |
| `server_command` | command, args | Execute server command |
| `move_player` | player, destination | Teleport player |
| `wait` | duration | Wait milliseconds |
| `remove_entities` | pattern | Remove matching entities |
| `remove_players` | pattern | Remove matching players |

### Assertion Types

| Type | Parameters | Description |
|------|------------|-------------|
| `entity_health` | entity, condition, value | Check entity health |
| `entity_exists` | entity, expected | Check if entity exists |
| `player_inventory` | player, item, slot, expected | Check inventory |
| `plugin_command` | plugin, player, command | Check plugin received command |

## File Structure

```
src/test/
├── java/com/dragonegg/lightning/
│   ├── pilaf/
│   │   ├── PilafBackend.java              # Backend interface
│   │   ├── PilafBackendFactory.java       # Factory for backends
│   │   ├── MockBukkitBackend.java         # Mock backend
│   │   ├── RealServerBackend.java         # RCON backend
│   │   ├── RealMinecraftClientBackend.java # Mineflayer backend
│   │   ├── RconClient.java                # RCON protocol client
│   │   ├── MineflayerClient.java          # HTTP client for bridge
│   │   ├── TestOrchestrator.java          # Timeline orchestrator
│   │   ├── YamlStoryParser.java           # YAML DSL parser
│   │   ├── Action.java                    # Action model
│   │   ├── Assertion.java                 # Assertion model
│   │   ├── TestResult.java                # Test result model
│   │   ├── DragonEggLightningUseCaseTest.java
│   │   └── entities/
│   │       ├── Entity.java
│   │       ├── EntityType.java
│   │       ├── Position.java
│   │       └── Item.java
│   └── unit/
│       ├── AbilityManagerTest.java
│       ├── LightningAbilityTest.java
│       └── HudManagerTest.java
└── resources/
    └── test-stories/
        ├── lightning-ability-test.yaml
        ├── cooldown-behavior-test.yaml
        └── session-persistence-test.yaml

mineflayer-bridge/                         # NEW: Node.js bridge server
├── package.json
├── server.js                              # Express HTTP server
├── mineflayer-client.js                   # Mineflayer wrapper
└── README.md
```

## Implementation Priority

1. **Phase 1: Core Infrastructure** ✅
   - [x] RconClient
   - [x] RealServerBackend
   - [x] MockBukkitBackend
   - [x] PilafBackend interface
   - [x] RconBackend (in PILAF library)
   - [x] PilafBackendFactory

2. **Phase 2: Orchestration** ✅
   - [x] Action model class
   - [x] Assertion model class
   - [x] YamlStoryParser
   - [x] TestOrchestrator

3. **Phase 3: Mineflayer Integration** (Future)
   - [ ] MineflayerBridge (Node.js server)
   - [ ] MineflayerClient (Java HTTP client)
   - [ ] RealMinecraftClientBackend update

4. **Phase 4: Advanced Features** (Future)
   - [ ] Parallel test execution
   - [ ] Test report generation
   - [ ] CI/CD integration

## Usage Examples

### Running Tests

```bash
# Run all RCON-based tests
./run-pilaf-integration-tests.sh

# Run specific test class
TEST_CLASS=DragonEggLightningUseCaseTest ./run-pilaf-integration-tests.sh

# Run specific test method
TEST_METHOD=testRealServerBackend ./run-pilaf-integration-tests.sh

# Run with Mineflayer (requires bridge running)
npm --prefix mineflayer-bridge start &
TEST_CLASS=RealMinecraftClientIntegrationTest ./run-pilaf-integration-tests.sh
```

### Programmatic Usage

```java
// Create orchestrator with backend
PilafBackend backend = PilafBackendFactory.create("real-server");
TestOrchestrator orchestrator = new TestOrchestrator(backend);

// Load and execute story
orchestrator.loadStory("lightning-ability-test.yaml");
orchestrator.execute();

// Check results
TestResult result = orchestrator.getResult();
assertTrue(result.isSuccess());
```
