# PILAF Technologies

## Core Technologies

### Java & JVM
- **Language**: Java 21 (LTS)
- **Build Tool**: Gradle
- **Testing Framework**: JUnit 5
- **Runtime**: OpenJDK or Oracle JDK 21+

### Dependencies (build.gradle)

#### Core Dependencies
```gradle
// YAML Parsing
implementation 'org.yaml:snakeyaml:2.0'

// Pebble Template Engine for HTML reports
implementation 'io.pebbletemplates:pebble:3.2.2'

// JSON Diff for semantic state comparison
implementation 'com.flipkart.zjsonpatch:zjsonpatch:0.4.16'

// Jackson for JSON parsing (required by zjsonpatch)
implementation 'com.fasterxml.jackson.core:jackson-core:2.17.0'
implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.0'

// JUnit Jupiter for testing
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

// JSONPath for data extraction
implementation 'com.jayway.jsonpath:json-path:2.9.0'

// Picocli for CLI
implementation 'info.picocli:picocli:4.7.5'
```

## Backend Technologies

### 1. Docker Backend
- **Container Platform**: Docker & Docker Compose
- **Server Image**: `itzg/minecraft-server:java21`
- **Server Type**: PaperMC (latest stable)
- **RCON**: Built-in RCON support
- **Network**: Custom bridge network (172.20.0.0/16)

### 2. Mineflayer Backend
- **Runtime**: Node.js 16+
- **Framework**: Express.js
- **Library**: Mineflayer v4.33.0
- **Pathfinding**: mineflayer-pathfinder v2.4.4
- **Communication**: HTTP/REST API on port 3000

### 3. HeadlessMc Backend (Development)
- **Runtime**: Java 21
- **Server Management**: Self-contained PaperMC launcher
- **RCON**: Fallback mechanism for server commands
- **CI/CD**: Optimized for GitHub Actions and similar platforms

## Communication Protocols

### RCON Protocol
- **Port**: 25575
- **Protocol**: TCP-based RCON
- **Authentication**: Password-based
- **Client**: Custom Java RCON implementation

### HTTP/WebSocket (Mineflayer Bridge)
- **Server**: Express.js HTTP server
- **Port**: 3000
- **API Style**: RESTful JSON
- **WebSocket**: Real-time events (chat, player actions)

## Server Technologies

### Minecraft Server
- **Version**: PaperMC 1.20.4+ (Java 21 compatible)
- **Plugins**: Paper/Bukkit plugins
- **Configuration**: Custom server.properties
- **Performance**: Optimized for testing (peaceful difficulty, reduced view distance)

### Environment Setup
```yaml
# Docker Compose Configuration
services:
  papermc:
    image: itzg/minecraft-server:java21
    ports:
      - "25565:25565"  # Minecraft server
      - "25575:25575"  # RCON
    environment:
      - EULA=true
      - TYPE=PAPER
      - ENABLE_RCON=true
      - RCON_PASSWORD=dragon123
      - MAX_PLAYERS=10
      - ONLINE_MODE=false
```

## Development Environment

### Build System
- **Build Tool**: Gradle 8.x
- **Java Version**: 21 (sourceCompatibility & targetCompatibility)
- **Application Main**: `org.cavarest.pilaf.cli.PilafCli`
- **JAR Configuration**: Fat JAR with all dependencies

### Project Structure
```
build.gradle                    # Build configuration
settings.gradle                 # Project settings
gradle.properties              # Gradle properties
src/
├── main/java/                 # Production code
├── test/java/                 # Test code
└── main/resources/            # Resources (templates, configs)
```

### IDE Support
- **Recommended IDEs**: IntelliJ IDEA, Eclipse, VS Code
- **Java Extensions**: Language support for Java, Gradle for Java
- **Code Style**: 2 spaces indentation, 80-character line limit

## Testing Technologies

### Unit Testing
- **Framework**: JUnit 5 (Jupiter)
- **Assertions**: Built-in JUnit assertions
- **Test Execution**: JUnit Platform Launcher
- **Coverage**: Not configured (consider JaCoCo)

### Integration Testing
- **Docker**: Test containers for isolated testing
- **Test Stories**: YAML-based integration tests
- **Backend Testing**: Cross-backend consistency testing
- **Mock Framework**: Custom mocking for backend interfaces

## Reporting Technologies

### HTML Reports
- **Template Engine**: Pebble Templates
- **CSS Framework**: Custom CSS (inline)
- **JavaScript**: Vanilla JS for interactivity
- **Output**: Self-contained HTML files

### XML Reports
- **Format**: JUnit XML
- **CI/CD Integration**: GitHub Actions, Jenkins, etc.
- **Schema**: JUnit 5 compatible

### Text Reports
- **Format**: Plain text with timestamps
- **Logging**: Structured log entries
- **Storage**: File-based persistence

## CI/CD Technologies

### GitHub Actions
```yaml
# Example workflow
name: PILAF Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run PILAF tests
        run: gradle run --args="--backend headlessmc story.yaml"
```

### Docker Integration
- **Multi-stage builds**: Efficient container builds
- **Health checks**: Service availability verification
- **Volume management**: Persistent data storage
- **Network isolation**: Custom bridge networks

## Development Tools

### Code Quality
- **Linting**: None currently configured (consider Checkstyle)
- **Formatting**: Project conventions (2 spaces, 80 chars)
- **Documentation**: Javadoc for public APIs
- **Version Control**: Git with semantic commits

### Debugging
- **Logging**: Structured logging with timestamps
- **Debug Mode**: `--verbose` flag for detailed output
- **Health Checks**: Service availability verification
- **Error Handling**: Graceful degradation with detailed error messages

## Performance Considerations

### Memory Usage
- **Java Heap**: Configured based on server size
- **Docker Containers**: 2-4GB per container
- **HeadlessMc**: ~2GB for server process

### Network
- **RCON**: TCP connections with timeout handling
- **HTTP**: REST API with rate limiting
- **WebSocket**: Real-time bidirectional communication

### Storage
- **Server Data**: Docker volumes for persistence
- **Reports**: File-based storage in `target/pilaf-reports`
- **Logs**: Rotating log files for long-running tests

## Platform Support

### Operating Systems
- **Primary**: Linux (Ubuntu, CentOS, Docker)
- **Development**: macOS, Windows (via WSL2)
- **CI/CD**: GitHub Actions (Ubuntu), Jenkins

### Java Compatibility
- **Minimum**: Java 21 (LTS)
- **Recommended**: OpenJDK 21 or Oracle JDK 21
- **Future**: Ready for Java 22+ (when LTS)

## External Dependencies

### Runtime Dependencies
- **Docker**: For Docker backend (optional)
- **Node.js**: For Mineflayer bridge (optional)
- **Minecraft Server**: PaperMC server runtime

### Development Dependencies
- **Git**: Version control
- **Gradle**: Build automation
- **Java 21+**: Development runtime
- **IDE**: Development environment

## Security Considerations

### Network Security
- **RCON Authentication**: Password-protected RCON
- **Localhost Binding**: Services bind to localhost by default
- **No Authentication**: Local testing environment (no user auth)

### Data Protection
- **Test Data**: Temporary, cleaned up after tests
- **Server Logs**: No sensitive information in logs
- **Configuration**: No hardcoded credentials in code

## Future Technology Roadmap

### Short Term
- **Java 22**: Upgrade when stable
- **Docker Desktop**: Better Windows/macOS support
- **Maven Alternative**: Consider Maven for simpler builds

### Medium Term
- **GraalVM**: Native image compilation for faster startup
- **Micronaut/Quarkus**: Consider for CLI optimization
- **Kubernetes**: Container orchestration for scaling

### Long Term
- **WebAssembly**: Potential for cross-platform testing
- **Cloud Native**: Cloud-based testing infrastructure
- **AI Integration**: Automated test generation and maintenance