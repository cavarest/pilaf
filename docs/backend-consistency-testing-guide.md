# PILAF Backend Consistency Testing Guide

## Overview

This guide explains how to use PILAF's backend consistency testing framework to validate that PILAF behaves consistently across different backend combinations.

## What is Backend Consistency Testing?

Backend consistency testing validates that PILAF produces identical test results regardless of which backend combination you choose:

- **Docker Server + Mineflayer Client** (Production)
- **Docker Server + HeadlessMc Client** (CI/CD)
- **HeadlessMc Server + Mineflayer Client** (Self-Contained)
- **HeadlessMc Server + HeadlessMc Client** (Pure Java)

## Test Stories

The testing framework includes 4 comprehensive test stories:

### 1. Basic Item Operations (`test-story-1-basic-items.yaml`)
Tests fundamental item management:
- Item giving and removal
- Inventory checking
- Multiple item types
- Assertion validation

### 2. Entity Operations (`test-story-2-entities.yaml`)
Tests entity management:
- Entity spawning (cow, zombie, creeper)
- Health management
- Entity existence verification
- Multiple entity handling

### 3. Player Movement (`test-story-3-movement.yaml`)
Tests player positioning:
- Coordinate-based movement
- Position verification with tolerance
- Dimension changes
- Relative positioning

### 4. Server Commands (`test-story-4-commands.yaml`)
Tests server command execution:
- Server connectivity commands
- Player management commands
- World setting commands
- Command success validation

## Configuration Files

Each backend combination has a corresponding configuration file:

- `config-docker-mineflayer.yaml` - Docker + Mineflayer
- `config-docker-headlessmc.yaml` - Docker + HeadlessMc
- `config-headlessmc-mineflayer.yaml` - HeadlessMc + Mineflayer
- `config-headlessmc-both.yaml` - HeadlessMc + HeadlessMc

## Running Consistency Tests

### Quick Start
```bash
# Make the script executable
chmod +x run-consistency-tests.sh

# Run all consistency tests
./run-consistency-tests.sh
```

### Manual Execution
```bash
# Compile the project
./gradlew compileJava

# Run consistency tests
./gradlew run --args="--consistency-test"
```

### Individual Backend Testing
```bash
# Test specific backend combination
./gradlew run --args="--config=config-docker-mineflayer.yaml test-story-1-basic-items.yaml"
```

## Understanding Results

### Consistency Status
- **CONSISTENT**: All backends produced identical results
- **INCONSISTENT**: Different backends produced different results

### Performance Metrics
- **Execution Time**: Time taken by each backend
- **Relative Difference**: Performance variation between backends
- **Pass Rate**: Percentage of successful test executions

### Report Files
- **HTML Report**: `consistency-report-YYYY-MM-DD_HH-mm-ss.html`
  - Interactive web-based report with charts and tables
  - Visual consistency indicators
  - Detailed backend performance comparison

- **Text Report**: `consistency-report-YYYY-MM-DD_HH-mm-ss.txt`
  - Console-friendly format
  - Detailed error messages
  - Suitable for CI/CD integration

## Interpreting Reports

### Success Indicators
✅ **All Tests Pass**: 100% consistency rate
✅ **Consistent Assertions**: Identical assertion counts
✅ **Performance Tolerance**: Execution time differences < 10%

### Warning Indicators
⚠️ **Performance Variations**: Execution time differences > 10%
⚠️ **Mixed Results**: Some backends pass, others fail
⚠️ **Timing Issues**: Occasional timeout or slow responses

### Failure Indicators
❌ **Inconsistent Results**: Different outcomes across backends
❌ **Backend-Specific Errors**: Errors only on certain backends
❌ **High Failure Rate**: Multiple test story failures

## Troubleshooting

### Common Issues

#### 1. RCON Connection Refused
**Symptoms**: "Connection refused" errors in logs
**Solutions**:
- Verify server is running
- Check RCON credentials in config
- Ensure correct port (default 25575)

#### 2. Backend Initialization Fails
**Symptoms**: "Failed to initialize backend" errors
**Solutions**:
- Check Docker daemon is running (for Docker backends)
- Verify Java installation (for HeadlessMc backends)
- Ensure proper file permissions

#### 3. Test Timeouts
**Symptoms**: Tests timeout after 3-5 minutes
**Solutions**:
- Increase timeout values in BackendConsistencyTester
- Check system resources
- Use faster backend combinations for testing

#### 4. Plugin Conflicts
**Symptoms**: Unexpected behavior in some backends
**Solutions**:
- Ensure clean server state between tests
- Check for conflicting plugins
- Use vanilla server configurations

### Debug Commands

```bash
# Test backend connectivity
curl -s http://localhost:3000/health  # For Mineflayer
docker ps --filter name=pilaf-paper  # For Docker

# Run verbose tests
./gradlew run --args="--config=config-docker-mineflayer.yaml --verbose test-story-1-basic-items.yaml"

# Check generated reports
ls -la consistency-report-*.html consistency-report-*.txt
```

## Advanced Usage

### Custom Test Stories
Create new YAML test stories following the existing pattern:

```yaml
name: "Custom Test Story"
description: "Description of your test"
setup:
  - action: "execute_server_command"
    command: "say Starting custom test"
steps:
  - action: "your_custom_action"
    # ... test steps
cleanup:
  - action: "execute_server_command"
    command: "say Custom test completed"
```

### Custom Configuration
Create new configuration files for different backend setups:

```yaml
server_backend: docker
client_backend: mineflayer
server_version: "1.21.5"
mineflayer_url: http://localhost:3000
rcon_host: localhost
rcon_port: 25575
rcon_password: your_password
```

### CI/CD Integration
Add to your GitHub Actions workflow:

```yaml
- name: Run Backend Consistency Tests
  run: |
    chmod +x run-consistency-tests.sh
    ./run-consistency-tests.sh

- name: Upload Reports
  uses: actions/upload-artifact@v6
  with:
    name: consistency-reports
    path: consistency-report-*.html
```

## Best Practices

### 1. Regular Testing
- Run consistency tests before releases
- Test after backend modifications
- Include in CI/CD pipeline

### 2. Clean Environment
- Use dedicated test environments
- Clean server state between tests
- Monitor system resources

### 3. Result Analysis
- Review performance differences
- Investigate any inconsistencies
- Document known limitations

### 4. Gradual Rollout
- Start with HeadlessMc backends (faster)
- Progress to Docker backends
- Monitor resource usage

## Expected Results

### Typical Performance
- **HeadlessMc**: 5-30 seconds per test story
- **Docker**: 30-120 seconds per test story
- **Overall Test Suite**: 5-15 minutes (all backends)

### Success Criteria
- ✅ All test stories pass on all backends
- ✅ Consistent assertion results
- ✅ Performance variations < 20%
- ✅ Clean server state after tests

## Support

For issues with the consistency testing framework:

1. Check the troubleshooting section above
2. Review generated error logs
3. Consult the main PILAF documentation
4. Open an issue on the project repository

## Conclusion

The backend consistency testing framework ensures PILAF reliability across different deployment scenarios. Regular testing helps maintain consistent user experience regardless of backend choice.
