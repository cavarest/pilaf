# PILAF Tasks

## Common Development Tasks

### Adding New Backend Support

**Last performed:** 2026-01-02
**Files to modify:**
- `src/main/java/org/cavarest/pilaf/backend/PilafBackend.java` - Add interface methods
- `src/main/java/org/cavarest/pilaf/backend/PilafBackendFactory.java` - Add factory case
- `src/main/java/org/cavarest/pilaf/backend/{BackendName}Backend.java` - Implementation class
- `src/main/java/org/cavarest/pilaf/config/TestConfiguration.java` - Add configuration
- `src/main/java/org/cavarest/pilaf/cli/PilafCli.java` - Add CLI option
- `README.adoc` - Update backend documentation
- `docs/backend-configuration.md` - Add backend guide

**Steps:**
1. Define backend interface requirements in `PilafBackend`
2. Create new backend implementation class
3. Update factory to handle new backend type
4. Add configuration options
5. Update CLI to support backend selection
6. Write integration tests
7. Update documentation

**Important notes:**
- Ensure all backend implementations maintain the same API
- Test backward compatibility with existing stories
- Add health check support for the new backend
- Document any backend-specific limitations

### Adding New Action Types

**Last performed:** 2026-01-02
**Files to modify:**
- `src/main/java/org/cavarest/pilaf/model/Action.java` - Add enum value
- `src/main/java/org/cavarest/pilaf/parser/YamlStoryParser.java` - Add parser mapping
- `src/main/java/org/cavarest/pilaf/orchestrator/TestOrchestrator.java` - Add execution logic
- `src/main/java/org/cavarest/pilaf/backend/PilafBackend.java` - Add interface method
- Test story files - Add usage examples

**Steps:**
1. Add new action type to `Action.ActionType` enum
2. Update YAML parser to recognize new action type
3. Implement backend method for the new action
4. Add execution logic in TestOrchestrator
5. Create test story demonstrating the new action
6. Update YAML DSL documentation

**Important notes:**
- Follow existing naming conventions for action types
- Ensure action parameters are properly validated
- Add comprehensive error handling
- Test across all backend implementations

### Creating Test Stories

**Last performed:** 2026-01-02
**Files to create:**
- `test-{name}.yaml` - Test story file
- `src/test/resources/` - Resource location

**Steps:**
1. Create YAML story file with proper structure
2. Define setup actions (server preparation)
3. Define test steps (main testing logic)
4. Define cleanup actions (resource cleanup)
5. Test story across different backends
6. Add story to test discovery patterns

**Story Structure Template:**
```yaml
name: "Test Story Name"
description: "Description of what this test validates"

setup:
  - action: "setup_action"
    # Setup parameters

steps:
  - action: "test_action"
    # Test parameters
    assertions:
      - type: "assertion_type"
        # Assertion parameters

cleanup:
  - action: "cleanup_action"
    # Cleanup parameters
```

### Backend Consistency Testing

**Last performed:** 2026-01-02
**Files to modify:**
- `src/main/java/org/cavarest/pilaf/testing/BackendConsistencyTester.java` - Test framework
- `src/main/java/org/cavarest/pilaf/testing/comparison/` - Comparison utilities
- `run-consistency-tests.sh` - Test runner script

**Steps:**
1. Define test scenarios for each backend
2. Implement consistency comparison logic
3. Run tests across all backend combinations
4. Generate consistency reports
5. Analyze and fix inconsistencies

### Docker Configuration Updates

**Last performed:** 2026-01-02
**Files to modify:**
- `docker/docker-compose.pilaf.yml` - Main compose file
- `docker/docker-compose-example.yml` - Example configuration
- `docker/setup-pilaf.sh` - Setup script

**Steps:**
1. Update service configurations
2. Test compose file syntax
3. Verify health checks
4. Update documentation
5. Test on different platforms

### Documentation Updates

**Last performed:** 2026-01-02
**Files to modify:**
- `README.adoc` - Main documentation
- `docs/` - Additional documentation
- `examples/` - Usage examples

**Documentation Structure:**
- Architecture overview
- Quick start guide
- YAML DSL reference
- Backend configuration
- Troubleshooting guide

### CI/CD Pipeline Updates

**Last performed:** 2026-01-02
**Files to modify:**
- `.github/workflows/` - GitHub Actions
- `run-consistency-tests.sh` - Test runner
- `build.gradle` - Build configuration

**Steps:**
1. Update workflow configuration
2. Test pipeline execution
3. Optimize for performance
4. Update documentation
5. Monitor pipeline health

### Release Preparation

**Last performed:** 2026-01-02
**Files to modify:**
- `gradle.properties` - Version numbers
- `CHANGELOG.md` - Change log
- `README.adoc` - Version badges
- Release notes

**Steps:**
1. Update version numbers
2. Create comprehensive changelog
3. Update documentation
4. Run full test suite
5. Create release artifacts
6. Publish to package repositories

### Performance Optimization

**Last performed:** 2026-01-02
**Areas to optimize:**
- Backend startup times
- Memory usage optimization
- Network communication efficiency
- Report generation performance

**Steps:**
1. Profile application performance
2. Identify bottlenecks
3. Implement optimizations
4. Benchmark improvements
5. Update documentation

## Testing Tasks

### Running All Tests
```bash
# Run full test suite
./gradlew test

# Run specific test class
./gradlew test --tests "*TestClass*"

# Run with coverage
./gradlew test jacocoTestReport

# Run backend consistency tests
./run-consistency-tests.sh
```

### Testing Individual Backends
```bash
# Test Docker backend
./gradlew run --args="--backend docker story.yaml"

# Test Mineflayer backend
./gradlew run --args="--backend mineflayer story.yaml"

# Test HeadlessMc backend
./gradlew run --args="--backend headlessmc story.yaml"
```

### Health Checks
```bash
# Check all services
./gradlew run --args="--health-check"

# Check specific backend
curl http://localhost:3000/health
```

## Debugging Tasks

### Common Issues and Solutions

1. **RCON Connection Refused**
   - Check server is running
   - Verify RCON settings
   - Check firewall rules

2. **YAML Parsing Errors**
   - Validate YAML syntax
   - Check action parameters
   - Verify required fields

3. **Backend Initialization Fails**
   - Check dependencies
   - Verify configuration
   - Review logs for errors

### Debug Mode
```bash
# Enable verbose output
./gradlew run --args="--verbose story.yaml"

# Check logs
tail -f target/pilaf-reports/*.log
```

## Maintenance Tasks

### Dependency Updates
- Update Gradle dependencies quarterly
- Test compatibility with new versions
- Update documentation for breaking changes

### Security Updates
- Monitor for security vulnerabilities
- Update dependencies promptly
- Review RCON authentication

### Performance Monitoring
- Monitor startup times
- Track memory usage
- Optimize slow operations

## Integration Tasks

### Plugin Development Integration
1. Add PILAF to plugin's test suite
2. Create plugin-specific test stories
3. Integrate with CI/CD pipeline
4. Generate plugin documentation

### Multi-Project Setup
1. Configure workspace structure
2. Share common test stories
3. Create reusable test utilities
4. Document integration patterns