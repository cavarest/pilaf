# PILAF Framework Testing Guide

**Date**: December 27, 2025
**Version**: 1.0.0
**Framework**: PILAF (Paper Integration Lightning Automation Framework)

---

## 🎯 Overview

The PILAF framework provides **two distinct testing modes** for PaperMC plugins:

1. **MockBukkitBackend**: Fast, in-memory testing (no server required)
2. **RealServerBackend**: Full integration testing with actual PaperMC server

---

## 🚀 Quick Start Commands

### MockBukkit Tests (Fast)
```bash
# Run all PILAF mock tests
mvn test -Dtest.groups=mock

# Run specific MockBukkit tests
mvn test -Dtest=DragonEggLightningUseCaseTest#testDragonEggLightningMockBackend

# Run all PILAF integration tests
mvn test -Dtest.groups=pilaf
```

### Real Server Tests (Full Integration)
```bash
# Start server and run full test suite
./start-pilaf-integration-tests.sh

# Or manually:
./start-server.sh          # Start PaperMC server
mvn test -Dtest.groups=integration   # Run integration tests
./stop-server.sh          # Stop server
```

---

## 📋 Detailed Testing Commands

### 1. MockBukkitBackend Tests

#### Why Use MockBukkit Tests?
- ✅ **Fast execution** (milliseconds)
- ✅ **No server required**
- ✅ **Perfect for development**
- ✅ **Repeatable results**

#### Run MockBukkit Tests
```bash
# All MockBukkit tests
mvn test -Dtest.groups=mock

# Specific PILAF use case test
mvn test -Dtest=DragonEggLightningUseCaseTest

# Unit tests with MockBukkit
mvn test -Dtest.groups=unit
```

#### Example Output
```
🧪 Testing Dragon Egg Lightning with Mock Backend
=================================================
📝 Simulating player setup...
🎁 MockBukkit: Giving 3 dragon_egg to test_player
🎮 MockBukkit: Equipping dragon_egg to offhand for test_player
✅ Mock backend simulation completed successfully
```

### 2. RealServerBackend Tests

#### Why Use Real Server Tests?
- ✅ **Full PaperMC integration**
- ✅ **Real plugin behavior**
- ✅ **RCON protocol testing**
- ✅ **Production-like environment**

#### Prerequisites
1. **Docker Desktop** must be running
2. **Server configuration** in `.env` file
3. **RCON access** configured

#### Run Real Server Tests
```bash
# Start PaperMC server with plugin
./start-server.sh

# Wait for server to fully start (30-60 seconds)
sleep 60

# Run real server integration tests
mvn test -Dtest.groups=integration

# Or run specific real server tests
mvn test -Dtest=DragonEggLightningUseCaseTest#testDragonEggLightningEndToEnd

# Stop server
./stop-server.sh
```

#### Complete Integration Test Script
```bash
#!/bin/bash
# save as: run-pilaf-integration-tests.sh

echo "🚀 Starting PILAF Integration Test Suite"

# Start PaperMC server
echo "📡 Starting PaperMC server..."
./start-server.sh

# Wait for server to be ready
echo "⏳ Waiting for server to start..."
sleep 60

# Verify server is running
echo "🔍 Checking server status..."
if docker ps | grep -q papermc-dragonegg; then
    echo "✅ Server is running"
else
    echo "❌ Server failed to start"
    exit 1
fi

# Run integration tests
echo "🧪 Running PILAF integration tests..."
mvn test -Dtest.groups=integration

# Capture test results
TEST_RESULT=$?

# Stop server
echo "🛑 Stopping PaperMC server..."
./stop-server.sh

# Report results
if [ $TEST_RESULT -eq 0 ]; then
    echo "🎉 All PILAF integration tests PASSED!"
else
    echo "❌ Some PILAF integration tests FAILED"
fi

exit $TEST_RESULT
```

Make it executable:
```bash
chmod +x run-pilaf-integration-tests.sh
```

---

## 🔧 Test Categories

### Unit Tests (MockBukkit)
```bash
mvn test -Dtest.groups=unit
```
- **Purpose**: Test individual plugin components
- **Speed**: ~1-2 seconds
- **Backend**: MockBukkitBackend
- **Coverage**: AbilityManager, LightningAbility, HudManager

### Integration Tests (Mixed)
```bash
mvn test -Dtest.groups=integration
```
- **Purpose**: Test plugin integration with PILAF framework
- **Speed**: ~30-60 seconds (with server startup)
- **Backend**: Both MockBukkit and RealServer
- **Coverage**: End-to-end scenarios, use case verification

### PILAF Framework Tests (MockBukkit)
```bash
mvn test -Dtest.groups=pilaf
```
- **Purpose**: Test PILAF framework functionality
- **Speed**: ~5-10 seconds
- **Backend**: MockBukkitBackend
- **Coverage**: Framework APIs, backend switching, assertions

### Use Case Tests (Real World)
```bash
mvn test -Dtest=DragonEggLightningUseCaseTest
```
- **Purpose**: Verify real-world plugin use cases
- **Speed**: Variable (mock vs real)
- **Backend**: Both backends tested
- **Coverage**: Dragon egg lightning plugin scenarios

---

## 📊 Test Results Interpretation

### MockBukkit Test Success
```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
✅ DragonEggLightningUseCaseTest.testDragonEggLightningMockBackend: PASSED
✅ All MockBukkit tests completed successfully
```

### Real Server Test Success
```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
🖥️ Initializing Real Server backend...
✅ RealServerBackend initialized for use case testing
✅ All real server tests completed successfully
```

### Common Test Status
- **PASSED** ✅: Test completed successfully
- **FAILED** ❌: Test assertion failed
- **ERROR** 💥: Test threw unexpected exception
- **SKIPPED** ⏭️: Test was disabled

---

## 🔍 Debugging Tests

### Enable Verbose Output
```bash
# Maven verbose output
mvn test -X -Dtest=DragonEggLightningUseCaseTest

# Surefire detailed reports
mvn test -Dtest=DragonEggLightningUseCaseTest
# Check: target/surefire-reports/*.txt
```

### Check Server Logs
```bash
# Real server logs
docker logs papermc-dragonegg --tail 50

# Server startup verification
docker logs papermc-dragonegg | grep "Done"
```

### Common Issues and Solutions

#### Issue: RCON Connection Failed
```
❌ Failed to connect to RCON server: null
```
**Solution**:
1. Check Docker is running: `docker ps`
2. Wait longer for server startup: `sleep 60`
3. Verify RCON port: `docker port papermc-dragonegg`

#### Issue: Server Won't Start
```
Cannot connect to the Docker daemon
```
**Solution**:
1. Start Docker Desktop
2. Check Docker status: `docker --version`
3. Restart Docker service

#### Issue: Plugin Not Loaded
```
[DragonEggLightning] DragonEggLightning plugin enabled!
```
**Verification**: This should appear in server logs during startup.

---

## 🎯 Recommended Testing Workflow

### Development Phase (Fast Iteration)
```bash
# 1. Run MockBukkit tests frequently
mvn test -Dtest.groups=mock

# 2. Quick unit test verification
mvn test -Dtest.groups=unit
```

### Pre-Release Testing (Comprehensive)
```bash
# 1. Start fresh server environment
./start-server.sh
sleep 60

# 2. Run complete test suite
mvn test

# 3. Check integration tests specifically
mvn test -Dtest.groups=integration

# 4. Clean shutdown
./stop-server.sh
```

### CI/CD Integration
```yaml
# Example GitHub Actions workflow
name: PILAF Integration Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Start PaperMC Server
        run: ./start-server.sh
      - name: Wait for server
        run: sleep 60
      - name: Run PILAF Tests
        run: mvn test -Dtest.groups=integration
      - name: Stop Server
        run: ./stop-server.sh
```

---

## 📈 Performance Benchmarks

### Test Execution Times
| Test Type | Backend | Typical Time | Use Case |
|-----------|---------|--------------|----------|
| Unit Tests | MockBukkit | 1-2 seconds | Development |
| PILAF Framework | MockBukkit | 5-10 seconds | API Testing |
| Integration Tests | Mixed | 30-60 seconds | Pre-release |
| Use Case Tests | Real Server | 60-120 seconds | Production validation |

### Optimization Tips
- Use **MockBukkit** for development (90% faster)
- Run **Real Server** tests only when necessary
- Use **CI caching** to speed up Maven builds
- Run tests in **parallel** when possible: `mvn test -T 4`

---

## 🛠️ Advanced Usage

### Custom Test Groups
Add to `pom.xml`:
```xml
<profiles>
    <profile>
        <id>pilaf-integration</id>
        <properties>
            <test.groups>integration,pilaf</test.groups>
        </properties>
    </profile>
</profiles>
```

Run with profile:
```bash
mvn test -Ppilaf-integration
```

### Custom Backend Selection
```java
// Force specific backend in tests
@BeforeEach
void setUp() {
    // Use only MockBukkit for this test
    MockBukkitBackend = new MockBukkitBackend();
}
```

---

## 📚 Additional Resources

- **PILAF Integration Report**: `PILAF_INTEGRATION_TESTING_REPORT.md`
- **Plugin Documentation**: `README.md`
- **Testing Guide**: `TESTING_GUIDE.md`
- **Server Setup**: `INSTALLATION_GUIDE.md`

---

**Last Updated**: December 27, 2025
**Framework Version**: PILAF 1.0.0
**Test Coverage**: 24/24 unit tests passing
