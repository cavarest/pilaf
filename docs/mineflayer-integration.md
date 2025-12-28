# 🎮 MINEFLAYER CLIENT + YAML TEST INTEGRATION GUIDE

**Date**: December 27, 2025
**Status**: ✅ **MINEFLAYER + YAML INTEGRATION READY**

---

## 🎯 WHAT THIS GUIDE SHOWS

**You asked**: "SHOW ME HOW TO RUN THE MINEFLAYER CLIENT WITH THE SCRIPT THAT WE CREATED IN YAML!!!"

**This guide demonstrates**:
1. ✅ How to run Mineflayer client with YAML test stories
2. ✅ How to integrate real Minecraft clients with PaperMC server
3. ✅ How to test DragonEggLightning plugin with real players
4. ✅ Complete end-to-end integration testing workflow

---

## 🚀 QUICK START - RUN MINEFLAYER WITH YAML TESTS

### Method 1: Run the Complete Integration Demo
```bash
# Run the full integration demo script
./run-mineflayer-yaml-test.sh
```

### Method 2: Manual Step-by-Step
```bash
# Step 1: Start PaperMC server
./start-server.sh

# Step 2: Run Mineflayer client (separate terminal)
node mineflayer-client.js localhost 25565 mineflayer_test

# Step 3: Run YAML tests with Maven
mvn test -Dtest=YamlDslTest
```

---

## 📋 AVAILABLE YAML TEST STORIES

The system includes these YAML test stories:

### 1. **lightning-ability-test.yaml**
```yaml
story:
  name: "Lightning Ability Test"
  description: "Test lightning ability works on entities"

steps:
  - name: "Start Server"
    action: "start_server"

  - name: "Connect Player"
    action: "connect_player"
    player: "LightningTester"

  - name: "Use Lightning Ability"
    action: "player_command"
    player: "LightningTester"
    command: "ability 1"
```

### 2. **plugin-version-test.yaml**
```yaml
story:
  name: "Plugin Version Test"
  description: "Verify plugin loads with correct version"

steps:
  - name: "Check Plugin Version"
    action: "player_command"
    player: "TestPlayer"
    command: "ability version"
    expected_response_contains: "1.0.2"
```

### 3. **mineflayer-integration-test.yaml** (NEW!)
```yaml
story:
  name: "Mineflayer Client Integration Test"
  description: "Test with real Mineflayer client"

steps:
  - name: "Start Mineflayer Client"
    action: "start_mineflayer_client"
    host: "localhost"
    port: 25565
    username: "mineflayer_test"

  - name: "Execute Lightning Ability"
    action: "mineflayer_command"
    player: "mineflayer_test"
    command: "ability 1"
```

---

## 🎮 MINEFLAYER CLIENT USAGE

### Basic Usage
```bash
node mineflayer-client.js <host> <port> <username>

# Examples:
node mineflayer-client.js localhost 25565 mineflayer_test
node mineflayer-client.js 192.168.1.100 25565 test_player
node mineflayer-client.js mc.example.com 25565 myplayer
```

### What the Mineflayer Client Does
```
✅ Connects to Minecraft server as real player
✅ Executes commands like `/ability 1`
✅ Manages inventory (gives items, equips gear)
✅ Moves around the world
✅ Sends chat messages
✅ Disconnects cleanly
```

### Expected Output
```
[Mineflayer] mineflayer_test logged in to localhost:25565
[Mineflayer] Connected successfully
[Mineflayer] Giving 3 dragon_egg to mineflayer_test
[Mineflayer] Executing command: give @s dragon_egg 3
[Mineflayer] Equipping dragon_egg to offhand
[Mineflayer] Executing command: replaceitem entity @s offhand dragon_egg
[Mineflayer] Executing command: ability 1
[Mineflayer] Disconnected mineflayer_test
```

---

## 🔧 YAML DSL INTEGRATION

### New Mineflayer Actions Available
The YAML framework now supports these Mineflayer-specific actions:

#### `start_mineflayer_client`
```yaml
- name: "Start Mineflayer Client"
  action: "start_mineflayer_client"
  host: "localhost"
  port: 25565
  username: "mineflayer_test"
```

#### `mineflayer_command`
```yaml
- name: "Execute Lightning Ability"
  action: "mineflayer_command"
  player: "mineflayer_test"
  command: "ability 1"
```

#### `mineflayer_give_item`
```yaml
- name: "Give Dragon Eggs"
  action: "mineflayer_give_item"
  player: "mineflayer_test"
  item: "dragon_egg"
  count: 3
```

#### `mineflayer_equip_item`
```yaml
- name: "Equip Dragon Egg"
  action: "mineflayer_equip_item"
  player: "mineflayer_test"
  item: "dragon_egg"
  slot: "offhand"
```

---

## 🧪 RUNNING THE TESTS

### Option 1: Full Integration Demo
```bash
./run-mineflayer-yaml-test.sh
```
This script will:
- ✅ Check all dependencies
- ✅ Create the integration YAML story
- ✅ Start PaperMC server
- ✅ Run Mineflayer client
- ✅ Show live demonstration

### Option 2: Maven YAML Tests
```bash
# Run all YAML tests
mvn test -Dtest=YamlDslTest

# Run specific test story
mvn test -Dtest=YamlDslTest#testRunYamlStory -Dstory=lightning-ability-test.yaml

# Run with verbose output
mvn test -Dtest=YamlDslTest -X
```

### Option 3: Manual Testing
```bash
# Terminal 1: Start server
./start-server.sh

# Terminal 2: Run Mineflayer
node mineflayer-client.js localhost 25565 mineflayer_test

# Terminal 3: Run Java tests
mvn test -Dtest=RealMinecraftClientIntegrationTest
```

---

## 📁 FILES INVOLVED

### Java Components
- `src/test/java/com/dragonegg/lightning/pilaf/RconClient.java` - OOP RCON client
- `src/test/java/com/dragonegg/lightning/pilaf/RealMinecraftIntegrationBackend.java` - Integration coordinator
- `src/test/java/com/dragonegg/lightning/pilaf/RealServerBackend.java` - Server backend

### Node.js Components
- `mineflayer-client.js` - Mineflayer client for PILAF
- `package.json` - Node dependencies (mineflayer, mineflayer-pathfinder)

### YAML Test Stories
- `src/test/resources/test-stories/lightning-ability-test.yaml`
- `src/test/resources/test-stories/plugin-version-test.yaml`
- `src/test/resources/test-stories/mineflayer-integration-test.yaml`

### Scripts
- `run-mineflayer-yaml-test.sh` - Complete integration demo script
- `start-server.sh` - Start PaperMC server
- `test-plugin.sh` - Test the plugin

---

## 🎬 LIVE DEMONSTRATION COMMANDS

Here are the exact commands to run right now:

### 1. Quick Mineflayer Test
```bash
# Make sure server is running first
./start-server.sh &

# Then run Mineflayer client
node mineflayer-client.js localhost 25565 mineflayer_test
```

### 2. YAML Test Execution
```bash
# Run YAML tests
mvn test -Dtest=YamlDslTest#testRunAllYamlStories
```

### 3. Full Integration Test
```bash
# Run the complete demo
./run-mineflayer-yaml-test.sh
```

---

## ✅ SUCCESS INDICATORS

When everything works correctly, you should see:

### Mineflayer Client Success
```
✅ [Mineflayer] mineflayer_test logged in to localhost:25565
✅ [Mineflayer] Connected successfully
✅ [Mineflayer] Executing command: ability 1
✅ [Mineflayer] Disconnected mineflayer_test
```

### YAML Test Success
```
✅ Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
✅ BUILD SUCCESS
```

### Server Integration Success
```
✅ Server receives `/ability 1` command from real player
✅ Plugin processes command correctly
✅ Lightning effects visible to Mineflayer client
```

---

## 🎯 WHAT THIS ACHIEVES

### For Developers
- **Real Client Testing**: Test with actual Minecraft clients, not mocks
- **End-to-End Validation**: Complete server-to-client interaction testing
- **Plugin Integration**: Test DragonEggLightning plugin with real players

### For QA Testing
- **Automated Testing**: YAML stories can be run automatically
- **Real Scenarios**: Test actual gameplay scenarios
- **Cross-Platform**: Works with any PaperMC server

### For CI/CD
- **Integration Testing**: Part of continuous integration pipeline
- **Real Environment**: Tests against actual Minecraft server
- **Repeatable**: Consistent test results every time

---

## 🚀 NEXT STEPS

1. **Run the demo**: `./run-mineflayer-yaml-test.sh`
2. **Create custom YAML stories**: Edit files in `src/test/resources/test-stories/`
3. **Extend Mineflayer client**: Add more actions to `mineflayer-client.js`
4. **Integrate with CI**: Add to your build pipeline

---

## 🎉 SUMMARY

**You asked to see how to run Mineflayer client with YAML scripts - and here's the complete solution:**

✅ **Mineflayer Client**: Real Minecraft client that connects to PaperMC servers
✅ **YAML Test Stories**: Human-readable test scenarios in YAML format
✅ **Integration Script**: One-command demo of everything working together
✅ **Full End-to-End**: Complete server + client + plugin testing workflow

**Ready to test!** Run `./run-mineflayer-yaml-test.sh` to see it all working! 🎮
