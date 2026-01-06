# PILAF Backend Configuration Guide

## Overview
PILAF now supports multiple backend implementations, allowing you to choose the best infrastructure for your testing needs.

## Available Backends

### 1. Docker Backend (Default)
**Configuration:**
```yaml
backend: docker
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
```

**Features:**
- ✅ Full server plugin testing
- ✅ Complete RCON support
- ✅ All PILAF features available
- ✅ Production-like server environment

**Requirements:**
- Docker daemon running
- PaperMC server accessible via RCON

### 2. Mineflayer Backend
**Configuration:**
```yaml
backend: mineflayer
mineflayer_url: http://localhost:3000
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
```

**Features:**
- ✅ Remote server control
- ✅ Full plugin testing
- ✅ Integration with existing Mineflayer setups

**Requirements:**
- Mineflayer bridge running
- RCON server accessible

### 3. HeadlessMc Backend (New!)
**Configuration:**
```yaml
backend: headlessmc
server_version: "1.21.5"
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
auto_launch: true
rcon_fallback: true
```

**Features:**
- ✅ Self-contained (no Docker required)
- ✅ CI/CD friendly
- ✅ Automatic server management
- ✅ Multi-version support
- ⚠️ Limited plugin management (server launch only)
- ⚠️ RCON fallback required for server commands

**Best For:**
- CI/CD pipelines without Docker
- Quick testing environments
- Multi-version compatibility testing

## Backend Selection

### Command Line
```bash
# Use specific backend
gradle run --args="--backend headlessmc story.yaml"

# With configuration
gradle run --args="--config=pilaf-headlessmc.yaml story.yaml"
```

### Configuration File
```yaml
# pilaf.yaml
backend: headlessmc  # docker, mineflayer, headlessmc
server_version: "1.21.5"
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
```

## Backend Comparison

| Feature | Docker | Mineflayer | HeadlessMc |
|---------|--------|------------|------------|
| Plugin Installation | ✅ Full | ✅ Full | ⚠️ Limited |
| RCON Commands | ✅ Full | ✅ Full | ✅ Via Fallback |
| Multi-Version | ⚠️ Manual | ⚠️ Manual | ✅ Automatic |
| CI/CD Ready | ❌ Docker Required | ⚠️ Setup Required | ✅ Self-Contained |
| Server Control | ✅ Full | ✅ Full | ⚠️ Launch Only |
| Setup Complexity | 🟡 Medium | 🟡 Medium | 🟢 Low |

## Migration Guide

### From Docker to HeadlessMc
1. Update configuration:
   ```yaml
   # Before
   backend: docker

   # After
   backend: headlessmc
   server_version: "1.21.5"
   auto_launch: true
   rcon_fallback: true
   ```

2. Remove Docker dependency from your CI/CD
3. Test with existing PILAF stories (should work unchanged)

### From Mineflayer to HeadlessMc
1. Update configuration:
   ```yaml
   # Before
   backend: mineflayer
   mineflayer_url: http://localhost:3000

   # After
   backend: headlessmc
   server_version: "1.21.5"
   auto_launch: true
   ```

## Limitations and Workarounds

### HeadlessMc Backend Limitations

1. **Plugin Installation**: Cannot install plugins automatically
   - **Workaround**: Pre-install plugins in server directory

2. **RCON Fallback**: Requires separate RCON connection
   - **Workaround**: Ensure RCON is enabled on launched servers

3. **Entity Queries**: Limited entity information access
   - **Workaround**: Use server commands for entity operations

## Performance Considerations

### HeadlessMc Backend
- **Startup Time**: ~5-10 seconds (server launch)
- **Memory Usage**: ~2GB for server process
- **Network**: RCON fallback for all commands
- **Best For**: CI/CD environments, quick testing

### Docker Backend
- **Startup Time**: ~30-60 seconds (container + server)
- **Memory Usage**: ~2-4GB for container
- **Network**: Direct RCON connection
- **Best For**: Full testing, plugin development

## Future Enhancements

### Planned Features
- **Matrix Testing**: Test across multiple Minecraft versions
- **Client-Side Testing**: Support for client mods via MC-Runtime-Test
- **Plugin Management**: Automatic plugin installation for HeadlessMc
- **Performance Optimization**: Caching and parallel execution

### API Integration
- **HeadlessMc Server API**: Direct server control without RCON
- **GameTest Framework**: Native Minecraft testing framework
- **Multi-Version Management**: Automatic version switching

## Troubleshooting

### Common Issues

1. **RCON Connection Refused**
   - Check if server is running
   - Verify RCON settings in server.properties
   - Ensure firewall allows RCON port

2. **Server Launch Fails**
   - Check Java installation
   - Verify server files exist
   - Review server logs for errors

3. **Backend Not Found**
   - Verify backend type in configuration
   - Check for typos in backend name
   - Ensure all required parameters provided

### Debug Mode
```bash
gradle run --args="--config=pilaf.yaml --verbose story.yaml"
```

## Examples

### CI/CD Pipeline
```yaml
# GitHub Actions example
- name: Run PILAF Tests
  run: |
    gradle run --args="--backend headlessmc story.yaml"
```

### Multi-Version Testing
```yaml
# Test multiple versions
backend: headlessmc
server_version: "1.21.5"  # Can be changed per test
auto_launch: true
rcon_fallback: true
```

### Plugin Development
```yaml
# Full plugin testing
backend: docker
rcon_host: localhost
rcon_port: 25575
rcon_password: dragon123
```

For more information, see the [PILAF README](../README.adoc) and [Architecture Documentation](../docs/pilaf-architecture.md).
