package org.cavarest.pilaf.backend;

import org.cavarest.rcon.RconClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for RconBackend class covering all server commands,
 * edge cases, null handling, and interaction verification with mock RconClient.
 */
@ExtendWith(MockitoExtension.class)
class RconBackendTest {

    @Mock
    private RconClient mockRconClient;

    private RconBackend backend;

    @BeforeEach
    void setUp() {
        backend = new RconBackend("localhost", 25575, "testpassword");
        backend.setRconClient(mockRconClient);
    }

    @AfterEach
    void tearDown() {
        backend = null;
    }

    // EXECUTE_RCON_COMMAND TESTS

    @Test
    void testExecuteCommand_Basic() throws IOException {
        when(mockRconClient.sendCommand("list"))
            .thenReturn("There are 0 of a max of 10 players online");
        backend.executeServerCommand("list", Collections.emptyList());
        verify(mockRconClient).sendCommand("list");
    }

    @Test
    void testExecuteCommand_WithArguments() throws IOException {
        when(mockRconClient.sendCommand("give player diamond_sword 1"))
            .thenReturn("Given diamond_sword to player");
        backend.executeServerCommand("give", Arrays.asList("player", "diamond_sword", "1"));
        verify(mockRconClient).sendCommand("give player diamond_sword 1");
    }

    @Test
    void testExecuteCommand_EmptyArguments() throws IOException {
        when(mockRconClient.sendCommand("say")).thenReturn("");
        backend.executeServerCommand("say", Collections.emptyList());
        verify(mockRconClient).sendCommand("say");
    }

    @Test
    void testExecuteCommand_NullArguments() throws IOException {
        when(mockRconClient.sendCommand("version")).thenReturn("Paper 1.20.4");
        backend.executeServerCommand("version", null);
        verify(mockRconClient).sendCommand("version");
    }

    @Test
    void testExecuteCommand_EmptyStringArg() throws IOException {
        when(mockRconClient.sendCommand("tellraw player ")).thenReturn("");
        backend.executeServerCommand("tellraw", Arrays.asList("player", ""));
        verify(mockRconClient).sendCommand("tellraw player ");
    }

    @Test
    void testExecuteCommand_SpecialCharacters() throws IOException {
        when(mockRconClient.sendCommand("give player diamond_sword 1 {ench:[{id:1}]}")).thenReturn("");
        backend.executeServerCommand("give", Arrays.asList("player", "diamond_sword", "1", "{ench:[{id:1}]}"));
        verify(mockRconClient).sendCommand("give player diamond_sword 1 {ench:[{id:1}]}");
    }

    @Test
    void testExecuteCommand_ManyArguments() throws IOException {
        when(mockRconClient.sendCommand("give player diamond_sword 64 0 {ench:[{id:16,lvl:5}]}")).thenReturn("");
        backend.executeServerCommand("give", Arrays.asList("player", "diamond_sword", "64", "0", "{ench:[{id:16,lvl:5}]}"));
        verify(mockRconClient).sendCommand("give player diamond_sword 64 0 {ench:[{id:16,lvl:5}]}");
    }

    // EXECUTE_RCON_WITH_CAPTURE TESTS

    @Test
    void testExecuteWithCapture_ReturnsOutput() throws Exception {
        when(mockRconClient.sendCommand("time query gametime")).thenReturn("The time is 89565");
        String result = backend.executeRconWithCapture("time query gametime");
        assertEquals("The time is 89565", result);
        verify(mockRconClient).sendCommand("time query gametime");
    }

    @Test
    void testExecuteWithCapture_EmptyResponse() throws Exception {
        when(mockRconClient.sendCommand("save-off")).thenReturn("");
        String result = backend.executeRconWithCapture("save-off");
        assertNotNull(result);
        verify(mockRconClient).sendCommand("save-off");
    }

    // RAW RCON COMMAND TESTS

    @Test
    void testExecuteRconRaw_ExactCommand() throws Exception {
        when(mockRconClient.sendCommand("minecraft:give player diamond 1")).thenReturn("");
        backend.executeRconRaw("minecraft:give player diamond 1");
        verify(mockRconClient).sendCommand("minecraft:give player diamond 1");
    }

    @Test
    void testExecutePlayerCommandRaw_Wrapping() throws Exception {
        when(mockRconClient.sendCommand("execute as test_player run /home")).thenReturn("");
        backend.executePlayerCommandRaw("test_player", "/home");
        verify(mockRconClient).sendCommand("execute as test_player run /home");
    }

    // SPAWN_ENTITY TESTS

    @Test
    void testSpawnEntity_WithLocation() throws IOException {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
        List<Double> location = Arrays.asList(100.0, 65.0, 200.0);
        backend.spawnEntity("test_zombie", "minecraft:zombie", location, null);
        verify(mockRconClient).sendCommand(contains("100.0 65.0 200.0"));
    }

    @Test
    void testSpawnEntity_WithCustomName() throws IOException {
        when(mockRconClient.sendCommand(argThat(cmd -> cmd != null && cmd.contains("100.0 65.0 200.0")))).thenReturn("");
        List<Double> location = Arrays.asList(100.0, 65.0, 200.0);
        backend.spawnEntity("my_zombie", "minecraft:zombie", location, null);
        verify(mockRconClient).sendCommand(argThat(cmd -> cmd != null && cmd.contains("test_my_zombie")));
    }

    @Test
    void testSpawnEntity_TypeLowercased() throws IOException {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
        List<Double> location = Arrays.asList(100.0, 65.0, 200.0);
        backend.spawnEntity("test_mob", "ZOMBIE", location, null);
        verify(mockRconClient).sendCommand(contains("zombie"));
    }

    @Test
    void testSpawnEntity_Tracking() throws IOException {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
        List<Double> location = Arrays.asList(100.0, 65.0, 200.0);
        backend.spawnEntity("tracked_entity", "minecraft:creeper", location, null);
        // Verify entity was tracked - entityExists calls executeCommand
        when(mockRconClient.sendCommand(contains("execute if entity"))).thenReturn("1");
        assertTrue(backend.entityExists("tracked_entity"));
    }

    // ENTITY_HEALTH TESTS

    @Test
    void testGetEntityHealth_ReturnsHealth() throws IOException {
        when(mockRconClient.sendCommand(contains("data get entity")))
            .thenReturn("minecraft:zombie has the following entity data: 20.0");
        double health = backend.getEntityHealth("test_zombie");
        assertEquals(20.0, health, 0.1);
    }

    @Test
    void testGetEntityHealth_FromTracking() throws IOException {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
        List<Double> location = Arrays.asList(100.0, 65.0, 200.0);
        backend.spawnEntity("test_entity", "minecraft:zombie", location, null);
        double health = backend.getEntityHealth("test_entity");
        assertEquals(20.0, health, 0.1);
    }

    @Test
    void testSetEntityHealth_SetsValue() throws IOException {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
        backend.setEntityHealth("test_zombie", 10.0);
        verify(mockRconClient).sendCommand(contains("Health set value 10.0"));
    }

    @Test
    void testSetEntityHealth_ZeroValue() throws IOException {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
        backend.setEntityHealth("test_zombie", 0.0);
        verify(mockRconClient).sendCommand(contains("Health set value 0.0"));
    }

    // ENTITY_EXISTS TESTS

    @Test
    void testEntityExists_ReturnsTrue() throws IOException {
        when(mockRconClient.sendCommand("execute if entity @e[name=test_test_zombie]")).thenReturn("1");
        boolean exists = backend.entityExists("test_zombie");
        assertTrue(exists);
    }

    @Test
    void testEntityExists_ReturnsFalse() throws IOException {
        when(mockRconClient.sendCommand("execute if entity @e[name=test_nonexistent]")).thenReturn("0");
        boolean exists = backend.entityExists("nonexistent");
        assertFalse(exists);
    }

    // PLAYER_MANAGEMENT TESTS

    @Test
    void testMakeOperator_GrantsOp() throws Exception {
        when(mockRconClient.sendCommand("op test_player")).thenReturn("");
        backend.makeOperator("test_player");
        verify(mockRconClient).sendCommand("op test_player");
    }

    @Test
    void testDisconnectPlayer_KickCommand() throws Exception {
        when(mockRconClient.sendCommand("kick test_player Disconnected by test")).thenReturn("");
        backend.disconnectPlayer("test_player");
        verify(mockRconClient).sendCommand("kick test_player Disconnected by test");
    }

    @Test
    void testRemoveItem_ClearCommand() throws Exception {
        when(mockRconClient.sendCommand("clear test_player diamond_sword 1")).thenReturn("");
        backend.removeItem("test_player", "diamond_sword", 1);
        verify(mockRconClient).sendCommand("clear test_player diamond_sword 1");
    }

    @Test
    void testRemoveAllTestEntities_KillCommand() throws IOException {
        when(mockRconClient.sendCommand("kill @e[name=test_]")).thenReturn("");
        backend.removeAllTestEntities();
        verify(mockRconClient).sendCommand("kill @e[name=test_]");
    }

    @Test
    void testRemoveAllTestEntities_ClearsTracking() throws IOException {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
        List<Double> location = Arrays.asList(100.0, 65.0, 200.0);
        backend.spawnEntity("entity1", "minecraft:zombie", location, null);
        backend.spawnEntity("entity2", "minecraft:skeleton", location, null);
        when(mockRconClient.sendCommand(contains("execute if entity"))).thenReturn("0");
        backend.removeAllTestEntities();
        assertFalse(backend.entityExists("entity1"));
        assertFalse(backend.entityExists("entity2"));
    }

    // TELEPORT_PLAYER TESTS

    @Test
    void testTeleportPlayer_Teleports() throws IOException {
        when(mockRconClient.sendCommand("tp test_player 100 64 200")).thenReturn("");
        backend.movePlayer("test_player", "to", "100 64 200");
        verify(mockRconClient).sendCommand("tp test_player 100 64 200");
    }

    // GIVE_ITEM TESTS

    @Test
    void testGiveItem_ExecutesCommand() throws IOException {
        when(mockRconClient.sendCommand("give test_player diamond_sword 1")).thenReturn("");
        backend.giveItem("test_player", "diamond_sword", 1);
        verify(mockRconClient).sendCommand("give test_player diamond_sword 1");
    }

    @Test
    void testGiveItem_WithCount() throws IOException {
        when(mockRconClient.sendCommand("give test_player diamond 64")).thenReturn("");
        backend.giveItem("test_player", "diamond", 64);
        verify(mockRconClient).sendCommand("give test_player diamond 64");
    }

    // EQUIP_ITEM TESTS

    @Test
    void testEquipItem_OffhandSlot() throws IOException {
        when(mockRconClient.sendCommand("replaceitem entity test_player weapon.offhand diamond_sword")).thenReturn("");
        backend.equipItem("test_player", "diamond_sword", "offhand");
        verify(mockRconClient).sendCommand("replaceitem entity test_player weapon.offhand diamond_sword");
    }

    @Test
    void testEquipItem_MainhandSlot() throws IOException {
        when(mockRconClient.sendCommand("replaceitem entity test_player weapon.mainhand diamond_sword")).thenReturn("");
        backend.equipItem("test_player", "diamond_sword", "mainhand");
        verify(mockRconClient).sendCommand("replaceitem entity test_player weapon.mainhand diamond_sword");
    }

    // SEND_CHAT TESTS

    @Test
    void testSendChat_MessageSent() throws IOException {
        when(mockRconClient.sendCommand("tellraw test_player \"Hello World\"")).thenReturn("");
        backend.sendChat("test_player", "Hello World");
        verify(mockRconClient).sendCommand("tellraw test_player \"Hello World\"");
    }

    @Test
    void testSendChat_SpecialCharacters() throws IOException {
        when(mockRconClient.sendCommand("tellraw test_player \"Hello &4World\"")).thenReturn("");
        backend.sendChat("test_player", "Hello &4World");
        verify(mockRconClient).sendCommand("tellraw test_player \"Hello &4World\"");
    }

    // EXECUTE_PLAYER_COMMAND TESTS

    @Test
    void testExecutePlayerCommand_Wrapping() throws IOException {
        when(mockRconClient.sendCommand("execute as test_player run home ")).thenReturn("");
        backend.executePlayerCommand("test_player", "home", Collections.emptyList());
        verify(mockRconClient).sendCommand("execute as test_player run home ");
    }

    @Test
    void testExecutePlayerCommand_WithArguments() throws IOException {
        when(mockRconClient.sendCommand("execute as test_player run give player diamond 1")).thenReturn("");
        backend.executePlayerCommand("test_player", "give", Arrays.asList("player", "diamond", "1"));
        verify(mockRconClient).sendCommand("execute as test_player run give player diamond 1");
    }

    // PLUGIN_DETECTION TESTS

    @Test
    void testPluginReceivedCommand_PluginExists() throws IOException {
        when(mockRconClient.sendCommand("plugins"))
            .thenReturn("Plugins (4):\n- Essentials (2.1)\n- WorldEdit (7.2)\n- WorldGuard (7.0)\n- LuckPerms (5.4)");
        boolean result = backend.pluginReceivedCommand("Essentials", "test", "player");
        assertTrue(result);
    }

    @Test
    void testPluginReceivedCommand_PluginNotFound() throws IOException {
        when(mockRconClient.sendCommand("plugins"))
            .thenReturn("Plugins (2):\n- Essentials\n- WorldEdit");
        boolean result = backend.pluginReceivedCommand("NonExistent", "test", "player");
        assertFalse(result);
    }

    // GET_PLAYER_POSITION TESTS

    @Test
    void testGetPlayerPosition_ExecutesCommand() throws Exception {
        when(mockRconClient.sendCommand("data get entity test_player Pos")).thenReturn("");
        backend.getPlayerPosition("test_player");
        verify(mockRconClient).sendCommand("data get entity test_player Pos");
    }

    // GET_PLAYER_INVENTORY TESTS

    @Test
    void testGetPlayerInventory_ExecutesCommand() throws Exception {
        when(mockRconClient.sendCommand("data get entity test_player Inventory")).thenReturn("");
        backend.getPlayerInventory("test_player");
        verify(mockRconClient).sendCommand("data get entity test_player Inventory");
    }

    // GET_PLAYER_HEALTH TESTS

    @Test
    void testGetPlayerHealth_ExecutesCommand() throws Exception {
        when(mockRconClient.sendCommand("data get entity test_player Health")).thenReturn("20.0");
        double health = backend.getPlayerHealth("test_player");
        verify(mockRconClient).sendCommand("data get entity test_player Health");
    }

    // GET_ENTITIES_IN_VIEW TESTS

    @Test
    void testGetEntitiesInView_ExecutesCommand() throws Exception {
        when(mockRconClient.sendCommand("execute at test_player run data get entity @e[distance=..10]")).thenReturn("");
        backend.getEntitiesInView("test_player");
        verify(mockRconClient).sendCommand("execute at test_player run data get entity @e[distance=..10]");
    }

    // GET_ENTITY_BY_NAME TESTS

    @Test
    void testGetEntityByName_ExecutesCommand() throws Exception {
        // getEntityByName calls executeCommand, which is mocked
        when(mockRconClient.sendCommand(contains("execute at test_player")))
            .thenReturn("minecraft:zombie");
        Map<String, Object> result = backend.getEntityByName("zombie", "test_player");
        verify(mockRconClient).sendCommand(contains("execute at test_player"));
        assertNotNull(result);
    }

    // GET_BLOCK_AT_POSITION TESTS

    @Test
    void testGetBlockAtPosition_ExecutesCommand() throws Exception {
        when(mockRconClient.sendCommand("data get block [100, 64, 100]")).thenReturn("");
        backend.getBlockAtPosition("[100, 64, 100]");
        verify(mockRconClient).sendCommand("data get block [100, 64, 100]");
    }

    // GET_WORLD_TIME TESTS

    @Test
    void testGetWorldTime_ReturnsTime() throws Exception {
        when(mockRconClient.sendCommand("time query gametime")).thenReturn("The time is 89565");
        long time = backend.getWorldTime();
        assertEquals(89565L, time);
    }

    @Test
    void testGetWorldTime_DifferentFormat() throws Exception {
        when(mockRconClient.sendCommand("time query gametime")).thenReturn("89565");
        long time = backend.getWorldTime();
        assertEquals(89565L, time);
    }

    @Test
    void testGetWorldTime_NullResponse() throws Exception {
        when(mockRconClient.sendCommand("time query gametime")).thenReturn(null);
        long time = backend.getWorldTime();
        assertEquals(0L, time);
    }

    // GET_WEATHER TESTS

    @Test
    void testGetWeather_ReturnsClear() throws Exception {
        String weather = backend.getWeather();
        assertEquals("clear", weather);
    }

    // STATE_MANAGEMENT TESTS

    @Test
    void testStoreState_StoresValue() throws IOException {
        backend.storeState("test_key", "test_value");
        assertEquals("test_value", backend.getStoredState("test_key"));
    }

    @Test
    void testGetStoredState_ReturnsValue() throws IOException {
        backend.storeState("inventory", "{\"items\":[]}");
        Object result = backend.getStoredState("inventory");
        assertEquals("{\"items\":[]}", result);
    }

    @Test
    void testGetStoredState_NonExistent() throws IOException {
        Object result = backend.getStoredState("nonexistent");
        assertNull(result);
    }

    @Test
    void testCompareStates_Equal() throws IOException {
        backend.storeState("state1", "{\"value\":1}");
        backend.storeState("state2", "{\"value\":1}");
        Map<String, Object> comparison = backend.compareStates("state1", "state2");
        assertEquals("state1", comparison.get("state1_name"));
        assertEquals("state2", comparison.get("state2_name"));
        assertEquals(true, comparison.get("equal"));
    }

    @Test
    void testCompareStates_NotEqual() throws IOException {
        backend.storeState("state1", "{\"value\":1}");
        backend.storeState("state2", "{\"value\":2}");
        Map<String, Object> comparison = backend.compareStates("state1", "state2");
        assertEquals(false, comparison.get("equal"));
    }

    // DATA_EXTRACTION TESTS

    @Test
    void testExtractWithJsonPath_ReturnsOriginal() throws IOException {
        String jsonData = "{\"items\":[{\"id\":\"diamond\"}]}";
        Object result = backend.extractWithJsonPath(jsonData, "$.items[0].id");
        assertEquals(jsonData, result);
    }

    @Test
    void testFilterEntities_ReturnsEmptyList() throws IOException {
        List<Map<String, Object>> result = backend.filterEntities("data", "type", "zombie");
        assertTrue(result.isEmpty());
    }

    // GET_SERVER_LOG TESTS

    @Test
    void testGetServerLog_ReturnsUnavailableMessage() throws IOException {
        String log = backend.getServerLog();
        assertEquals("[Log access not available via RCON]", log);
    }

    // GET_TYPE TESTS

    @Test
    void testGetType_ReturnsRcon() throws IOException {
        String type = backend.getType();
        assertEquals("rcon", type);
    }

    // CONNECT_PLAYER TESTS

    @Test
    void testConnectPlayer_NoOp() throws Exception {
        backend.connectPlayer("test_player");
        verifyNoInteractions(mockRconClient);
    }

    // REMOVE_ALL_TEST_PLAYERS TESTS

    @Test
    void testRemoveAllTestPlayers_NoOp() throws IOException {
        backend.removeAllTestPlayers();
        verifyNoInteractions(mockRconClient);
    }

    // USE_ITEM TESTS

    @Test
    void testUseItem_NoOp() throws IOException {
        backend.useItem("test_player", "diamond_sword", "target");
        verifyNoInteractions(mockRconClient);
    }

    // PLAYER_INVENTORY_CONTAINS TESTS

    @Test
    void testPlayerInventoryContains_ReturnsTrue() throws IOException {
        boolean result = backend.playerInventoryContains("test_player", "diamond_sword", "mainhand");
        assertTrue(result);
    }
}
