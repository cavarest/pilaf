# PILAF Architecture

## System Architecture Overview

PILAF follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PILAF TEST FRAMEWORK                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        TEST LAYER (YAML Stories)                     │   │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                 │   │
│  │  │ Story Files          │  │ Configuration Files  │                 │   │
│  │  │ • test-*.yaml        │  │ • pilaf.yaml         │                 │   │
│  │  │ • *.yaml             │  │ • config-*.yaml      │                 │   │
│  │  └──────────┬───────────┘  └──────────┬───────────┘                 │   │
│  └─────────────┼─────────────────────────┼─────────────────────────────┘   │
│                │                         │                                  │
│                ▼                         ▼                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ORCHESTRATION LAYER                               │   │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                 │   │
│  │  │ TestOrchestrator     │  │ YamlStoryParser      │                 │   │
│  │  │ (Timeline Replay)    │  │ (YAML DSL Parser)    │                 │   │
│  │  │ • Story execution    │  │ • Parse YAML stories │                 │   │
│  │  │ • Action sequencing  │  │ • Create Action objs │                 │   │
│  │  │ • Result aggregation │  │ • Validation         │                 │   │
│  │  └──────────┬───────────┘  └──────────┬───────────┘                 │   │
│  └─────────────┼─────────────────────────┼─────────────────────────────┘   │
│                │                         │                                  │
│                ▼                         ▼                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      BACKEND LAYER                                   │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐   │   │
│  │  │ PilafBackend │◀─┤ Backend      │  │ PilafBackendFactory      │   │   │
│  │  │ (Interface)  │  │ Factory      │  │ (creates backends)       │   │   │
│  │  │ • Player ops │  │ • Config mgmt│  │ • Backend selection      │   │   │
│  │  │ • Entity ops │  │ • Creation   │  │ • Initialization         │   │   │
│  │  │ • Server cmds│  │ • Validation │  │ • Dependency injection   │   │   │
│  │  └──────┬───────┘  └──────────────┘  └──────────────────────────┘   │   │
│  │         │                                                            │   │
│  │         ├─────────────────┬─────────────────┬───────────────────┐   │   │
│  │         ▼                 ▼                 ▼                   │   │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │   │   │
│  │  │ Mineflayer   │  │ HeadlessMc   │  │ Docker/RCON          │   │   │   │
│  │  │ Backend      │  │ Backend      │  │ Backend              │   │   │   │
│  │  │ • Player sim │  │ • Self-cont. │  │ • Server commands    │   │   │   │
│  │  │ • HTTP API   │  │ • CI/CD opt  │  │ • RCON protocol      │   │   │   │
│  │  │ • Bridge     │  │ • Auto-launch│  │ • Full server access │   │   │   │
│  │  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘   │   │   │
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
│              │                       │    (Java HTTP Client)             │  │
│              │                       │    • REST API calls               │  │
│              │                       │    • JSON responses               │  │
│              │                       └────────────────┬──────────────────┘  │
│              │                                        │                      │
└──────────────┼────────────────────────────────────────┼──────────────────────┘
               │                                        │
               ▼                                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         MINECRAFT SERVER LAYER                                │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │   │
│  │  │ PaperMC Server   │  │ RCON Interface   │  │ Minecraft        │   │   │
│  │  │ (Docker/Local)   │  │ Port: 25575      │  │ Game Port: 25565 │   │   │
│  │  │ • Version 1.20.4+│  │ • Command exec   │  │ • Multiplayer    │   │   │
│  │  │ • Plugins        │  │ • Query support  │  │ • Player actions │   │   │
│  │  │ • World data     │  │ • Auth required  │  │ • Entity mgmt    │   │   │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘   │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Source Code Structure

### Main Application Layer
```
src/main/java/org/cavarest/pilaf/
├── cli/                          # Command Line Interface
│   ├── PilafCli.java             # Main CLI entry point
│   ├── ConfigLoader.java         # Configuration file loader
│   └── StoryDiscoverer.java      # Story file discovery
├── orchestrator/                 # Test Orchestration
│   ├── TestOrchestrator.java     # Main orchestration logic
│   └── StoryExecutor.java        # Story execution engine
├── parser/                       # YAML Parsing
│   └── YamlStoryParser.java      # YAML story parser
├── backend/                      # Backend Abstraction
│   ├── PilafBackend.java         # Backend interface
│   ├── PilafBackendFactory.java  # Backend factory
│   ├── MineflayerBackend.java    # Mineflayer implementation
│   ├── HeadlessMcBackend.java    # HeadlessMc implementation
│   ├── DockerServerBackend.java  # Docker backend
│   └── RconBackend.java          # RCON backend
├── config/                       # Configuration Management
│   ├── TestConfiguration.java    # Configuration model
│   └── ConnectionManager.java    # Connection handling
├── model/                        # Data Models
│   ├── Action.java               # Action model
│   ├── Assertion.java            # Assertion model
│   ├── TestStory.java            # Story model
│   └── TestResult.java           # Result model
├── report/                       # Reporting
│   ├── TestReporter.java         # Main reporter
│   └── HtmlReportGenerator.java  # HTML report generation
├── rcon/                         # RCON Protocol
│   └── RconClient.java           # RCON protocol implementation
├── client/                       # Client Communication
│   └── MineflayerClient.java     # Mineflayer HTTP client
└── testing/                      # Testing Framework
    ├── BackendConsistencyTester.java # Backend testing
    └── report/
        └── ConsistencyReportGenerator.java
```

### Supporting Components
```
mineflayer-bridge/                # Node.js Bridge Server
├── server.js                     # Express HTTP server
├── mineflayer-client.js          # Mineflayer wrapper
├── Dockerfile                    # Container definition
└── package.json                  # Node.js dependencies

docker/                          # Docker Configuration
├── docker-compose.pilaf.yml      # Complete PILAF stack
├── docker-compose-example.yml   # Example configuration
└── setup-pilaf.sh              # Setup script

docs/                           # Documentation
├── pilaf-architecture.md       # Architecture guide
├── backend-configuration.md   # Backend config guide
├── yaml-dsl.md                 # YAML DSL reference
└── backend-consistency-testing-plan.md
```

## Key Technical Decisions

### 1. Backend Abstraction Pattern

**Decision**: Use an interface-based backend system with factory pattern
**Rationale**: 
- Enables testing against multiple backend implementations
- Allows for future backend additions without breaking existing code
- Provides clear separation between test logic and execution environment

**Implementation**:
```java
public interface PilafBackend {
    void initialize() throws Exception;
    void cleanup() throws Exception;
    String getType();
    // Core action methods...
}

public class PilafBackendFactory {
    public static PilafBackend create(String type, String... args) {
        switch (type.toLowerCase()) {
            case "mineflayer": return new MineflayerBackend(...);
            case "headlessmc": return new HeadlessMcBackend(...);
            case "docker": return new DockerServerBackend(...);
            default: throw new IllegalArgumentException("Unknown backend: " + type);
        }
    }
}
```

### 2. YAML Story DSL Design

**Decision**: Declarative YAML format with structured action definitions
**Rationale**:
- Human-readable and writable by non-developers
- Extensible without code changes
- Validatable at parse time

**Example Structure**:
```yaml
name: "Test Story"
setup:
  - action: "execute_rcon_command"
    command: "op test_player"
steps:
  - action: "give_item"
    player: "test_player"
    item: "diamond_sword"
    count: 1
cleanup:
  - action: "execute_rcon_command"
    command: "deop test_player"
```

### 3. Multi-Layer Communication

**Decision**: Separate communication layers for different use cases
**Rationale**:
- RCON for direct server commands (reliable, fast)
- HTTP/WebSocket for player simulation (realistic, event-driven)
- Local API for development and testing

**Layer Hierarchy**:
1. **Test Layer**: YAML stories and configuration
2. **Orchestration Layer**: Action parsing and execution coordination
3. **Backend Layer**: Backend-specific implementations
4. **Communication Layer**: Protocol-specific clients
5. **Server Layer**: Minecraft server interactions

### 4. State Management

**Decision**: Centralized state management with variable storage
**Rationale**:
- Enables complex test scenarios with state comparison
- Supports test data capture and validation
- Allows for dynamic test flow based on runtime state

**Implementation**:
```java
public class TestOrchestrator {
    private Map<String, Object> storedStates = new HashMap<>();
    
    public void storeState(String variableName, Object value) {
        storedStates.put(variableName, value);
    }
    
    public Object getState(String variableName) {
        return storedStates.get(variableName);
    }
}
```

## Component Relationships

### Data Flow
1. **Story Loading**: YAML → Parser → Action objects
2. **Action Execution**: Orchestrator → Backend → Communication → Server
3. **State Management**: Actions → Variables → Storage → Retrieval
4. **Result Aggregation**: Actions → Results → Reporter → Output

### Dependency Injection
- **Backend Factory**: Creates backend instances based on configuration
- **Test Configuration**: Manages all runtime settings
- **Connection Manager**: Handles service connections and health checks

### Error Handling Strategy
- **Graceful Degradation**: Fallback mechanisms for backend limitations
- **Comprehensive Logging**: Detailed logs for debugging and analysis
- **Health Monitoring**: Service availability checking and reporting

## Critical Implementation Paths

### Story Execution Flow
1. **Parse YAML**: `YamlStoryParser.parseString()` → `TestStory`
2. **Initialize Backend**: `PilafBackendFactory.create()` → `PilafBackend`
3. **Execute Setup**: For each setup action → `TestOrchestrator.executeAction()`
4. **Execute Steps**: For each step → `TestOrchestrator.executeAction()`
5. **Execute Cleanup**: For each cleanup action → `TestOrchestrator.executeAction()`
6. **Generate Reports**: `TestReporter.complete()` → HTML/JSON/XML reports

### Backend Integration Points
- **Initialization**: Backend startup and connection establishment
- **Action Execution**: Backend-specific action implementation
- **State Management**: Backend state capture and retrieval
- **Cleanup**: Resource cleanup and connection termination

## Design Patterns in Use

1. **Factory Pattern**: Backend creation and management
2. **Strategy Pattern**: Backend implementation selection
3. **Command Pattern**: Action encapsulation and execution
4. **Observer Pattern**: Event handling and logging
5. **Template Method**: Story execution workflow
6. **Builder Pattern**: Complex object construction (TestResult, reports)

This architecture ensures PILAF remains extensible, maintainable, and testable while providing the flexibility needed for various Minecraft testing scenarios.