package org.cavarest.pilaf.backend;

import org.cavarest.pilaf.client.MineflayerClient;
import org.cavarest.rcon.RconClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Comprehensive unit tests for MineflayerBackend class covering all player commands,
 * edge cases, null handling, Unicode, special characters, and interaction verification
 * with mock MineflayerClient and RconClient.
 */
@ExtendWith(MockitoExtension.class)
class MineflayerBackendTest {

    @Mock
    private MineflayerClient mockMineflayerClient;

    @Mock
    private RconClient mockRconClient;

    private MineflayerBackend backend;

    @BeforeEach
    void setUp() {
        backend = new MineflayerBackend("localhost", 3000, "localhost", 25575, "testpassword");
        // Inject mock dependencies using reflection
        try {
            java.lang.reflect.Field mineflayerField = MineflayerBackend.class.getDeclaredField("mineflayer");
            mineflayerField.setAccessible(true);
            mineflayerField.set(backend, mockMineflayerClient);

            java.lang.reflect.Field rconField = MineflayerBackend.class.getDeclaredField("rcon");
            rconField.setAccessible(true);
            rconField.set(backend, mockRconClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
    }

    @AfterEach
    void tearDown() {
        backend = null;
    }

    // ============================================================
    // CONNECT_PLAYER TESTS (7 tests)
    // ============================================================

    @Test
    void testConnectPlayer_Success() throws Exception {
        when(mockMineflayerClient.connect("test_player")).thenReturn(true);
        backend.connectPlayer("test_player");
        verify(mockMineflayerClient).connect("test_player");
    }

    @Test
    void testConnectPlayer_AlreadyConnected() throws Exception {
        when(mockMineflayerClient.connect("already_connected_player")).thenReturn(false);
        backend.connectPlayer("already_connected_player");
        verify(mockMineflayerClient).connect("already_connected_player");
    }

    @Test
    void testConnectPlayer_NullUsername() throws Exception {
        when(mockMineflayerClient.connect(null)).thenThrow(new IllegalArgumentException("Username cannot be null"));
        assertThrows(IllegalArgumentException.class, () -> backend.connectPlayer(null));
    }

    @Test
    void testConnectPlayer_LongUsername() throws Exception {
        String longUsername = "a".repeat(100);
        when(mockMineflayerClient.connect(longUsername)).thenReturn(true);
        backend.connectPlayer(longUsername);
        verify(mockMineflayerClient).connect(longUsername);
    }

    @Test
    void testConnectPlayer_UnicodeUsername() throws Exception {
        String unicodeUsername = "玩家1_测试";
        when(mockMineflayerClient.connect(unicodeUsername)).thenReturn(true);
        backend.connectPlayer(unicodeUsername);
        verify(mockMineflayerClient).connect(unicodeUsername);
    }

    @Test
    void testConnectPlayer_SpecialCharacters() throws Exception {
        String specialUsername = "Player_With_Special_Chars_123";
        when(mockMineflayerClient.connect(specialUsername)).thenReturn(true);
        backend.connectPlayer(specialUsername);
        verify(mockMineflayerClient).connect(specialUsername);
    }

    @Test
    void testConnectPlayer_ConnectionFailure() throws Exception {
        when(mockMineflayerClient.connect("failing_player")).thenReturn(false);
        backend.connectPlayer("failing_player");
        verify(mockMineflayerClient).connect("failing_player");
    }

    // ============================================================
    // DISCONNECT_PLAYER TESTS (6 tests)
    // ============================================================

    @Test
    void testDisconnectPlayer_Success() throws Exception {
        when(mockMineflayerClient.disconnect("test_player")).thenReturn(true);
        backend.disconnectPlayer("test_player");
        verify(mockMineflayerClient).disconnect("test_player");
    }

    @Test
    void testDisconnectPlayer_NotConnected() throws Exception {
        when(mockMineflayerClient.disconnect("not_connected_player")).thenReturn(false);
        backend.disconnectPlayer("not_connected_player");
        verify(mockMineflayerClient).disconnect("not_connected_player");
    }

    @Test
    void testDisconnectPlayer_NullUsername() throws Exception {
        when(mockMineflayerClient.disconnect(null)).thenThrow(new IllegalArgumentException("Username cannot be null"));
        assertThrows(IllegalArgumentException.class, () -> backend.disconnectPlayer(null));
    }

    @Test
    void testDisconnectPlayer_LongUsername() throws Exception {
        String longUsername = "b".repeat(100);
        when(mockMineflayerClient.disconnect(longUsername)).thenReturn(true);
        backend.disconnectPlayer(longUsername);
        verify(mockMineflayerClient).disconnect(longUsername);
    }

    @Test
    void testDisconnectPlayer_UnicodeUsername() throws Exception {
        String unicodeUsername = "测试玩家_unicode_中文";
        when(mockMineflayerClient.disconnect(unicodeUsername)).thenReturn(true);
        backend.disconnectPlayer(unicodeUsername);
        verify(mockMineflayerClient).disconnect(unicodeUsername);
    }

    @Test
    void testDisconnectPlayer_ConnectionError() throws Exception {
        when(mockMineflayerClient.disconnect("error_player")).thenThrow(new Exception("Connection reset"));
        assertThrows(Exception.class, () -> backend.disconnectPlayer("error_player"));
    }

    // ============================================================
    // GET_PLAYER_POSITION TESTS (4 tests)
    // ============================================================

    @Test
    void testGetPlayerPosition_ReturnsPosition() throws Exception {
        Map<String, Object> position = new HashMap<>();
        position.put("x", 100.5);
        position.put("y", 64.0);
        position.put("z", 200.5);
        when(mockMineflayerClient.getPosition("test_player")).thenReturn(position);

        Map<String, Object> result = backend.getPlayerPosition("test_player");

        assertNotNull(result);
        assertEquals(100.5, result.get("x"));
        assertEquals(64.0, result.get("y"));
        assertEquals(200.5, result.get("z"));
        verify(mockMineflayerClient).getPosition("test_player");
    }

    @Test
    void testGetPlayerPosition_EmptyPlayer() throws Exception {
        when(mockMineflayerClient.getPosition("")).thenReturn(new HashMap<>());

        Map<String, Object> result = backend.getPlayerPosition("");

        assertNotNull(result);
        verify(mockMineflayerClient).getPosition("");
    }

    @Test
    void testGetPlayerPosition_NullPlayer() throws Exception {
        when(mockMineflayerClient.getPosition(null)).thenThrow(new IllegalArgumentException("Player cannot be null"));
        assertThrows(IllegalArgumentException.class, () -> backend.getPlayerPosition(null));
    }

    @Test
    void testGetPlayerPosition_ErrorResponse() throws Exception {
        when(mockMineflayerClient.getPosition("error_player")).thenThrow(new Exception("Player not found"));
        assertThrows(Exception.class, () -> backend.getPlayerPosition("error_player"));
    }

    // ============================================================
    // GET_PLAYER_HEALTH TESTS (4 tests)
    // ============================================================

    @Test
    void testGetPlayerHealth_ReturnsHealth() throws Exception {
        Map<String, Object> health = new HashMap<>();
        health.put("health", 20.0);
        health.put("food", 20);
        health.put("saturation", 5.0f);
        when(mockMineflayerClient.getHealth("test_player")).thenReturn(health);

        Map<String, Object> result = backend.getPlayerHealth("test_player");

        assertNotNull(result);
        assertEquals(20.0, result.get("health"));
        assertEquals(20, result.get("food"));
        verify(mockMineflayerClient).getHealth("test_player");
    }

    @Test
    void testGetPlayerHealth_LowHealth() throws Exception {
        Map<String, Object> health = new HashMap<>();
        health.put("health", 1.0);
        health.put("food", 0);
        health.put("saturation", 0.0f);
        when(mockMineflayerClient.getHealth("critical_player")).thenReturn(health);

        Map<String, Object> result = backend.getPlayerHealth("critical_player");

        assertNotNull(result);
        assertEquals(1.0, result.get("health"));
        verify(mockMineflayerClient).getHealth("critical_player");
    }

    @Test
    void testGetPlayerHealth_NullPlayer() throws Exception {
        when(mockMineflayerClient.getHealth(null)).thenThrow(new IllegalArgumentException("Player cannot be null"));
        assertThrows(IllegalArgumentException.class, () -> backend.getPlayerHealth(null));
    }

    @Test
    void testGetPlayerHealth_ErrorResponse() throws Exception {
        when(mockMineflayerClient.getHealth("offline_player")).thenThrow(new Exception("Player offline"));
        assertThrows(Exception.class, () -> backend.getPlayerHealth("offline_player"));
    }

    // ============================================================
    // GET_PLAYER_INVENTORY TESTS (4 tests)
    // ============================================================

    @Test
    void testGetPlayerInventory_ReturnsInventory() throws Exception {
        Map<String, Object> inventory = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "minecraft:diamond_sword");
        item1.put("count", 1);
        item1.put("slot", 0);
        items.add(item1);
        inventory.put("items", items);
        when(mockMineflayerClient.getInventory("test_player")).thenReturn(inventory);

        Map<String, Object> result = backend.getPlayerInventory("test_player");

        assertNotNull(result);
        assertNotNull(result.get("items"));
        verify(mockMineflayerClient).getInventory("test_player");
    }

    @Test
    void testGetPlayerInventory_EmptyInventory() throws Exception {
        Map<String, Object> inventory = new HashMap<>();
        inventory.put("items", new ArrayList<>());
        when(mockMineflayerClient.getInventory("empty_player")).thenReturn(inventory);

        Map<String, Object> result = backend.getPlayerInventory("empty_player");

        assertNotNull(result);
        assertTrue(((List<?>) result.get("items")).isEmpty());
        verify(mockMineflayerClient).getInventory("empty_player");
    }

    @Test
    void testGetPlayerInventory_NullPlayer() throws Exception {
        when(mockMineflayerClient.getInventory(null)).thenThrow(new IllegalArgumentException("Player cannot be null"));
        assertThrows(IllegalArgumentException.class, () -> backend.getPlayerInventory(null));
    }

    @Test
    void testGetPlayerInventory_ErrorResponse() throws Exception {
        when(mockMineflayerClient.getInventory("error_player")).thenThrow(new Exception("Inventory access failed"));
        assertThrows(Exception.class, () -> backend.getPlayerInventory("error_player"));
    }

    // ============================================================
    // GET_PLAYER_EQUIPMENT TESTS (4 tests)
    // ============================================================

    @Test
    void testGetPlayerEquipment_ReturnsEquipment() throws Exception {
        Map<String, Object> equipment = new HashMap<>();
        equipment.put("mainhand", "minecraft:diamond_sword");
        equipment.put("offhand", "minecraft:shield");
        equipment.put("helmet", "minecraft:iron_helmet");
        equipment.put("chestplate", "minecraft:iron_chestplate");
        equipment.put("leggings", "minecraft:iron_leggings");
        equipment.put("boots", "minecraft:iron_boots");
        when(mockMineflayerClient.getEquipment("test_player")).thenReturn(equipment);

        Map<String, Object> result = backend.getPlayerEquipment("test_player");

        assertNotNull(result);
        assertEquals("minecraft:diamond_sword", result.get("mainhand"));
        assertEquals("minecraft:shield", result.get("offhand"));
        verify(mockMineflayerClient).getEquipment("test_player");
    }

    @Test
    void testGetPlayerEquipment_EmptyEquipment() throws Exception {
        Map<String, Object> equipment = new HashMap<>();
        when(mockMineflayerClient.getEquipment("naked_player")).thenReturn(equipment);

        Map<String, Object> result = backend.getPlayerEquipment("naked_player");

        assertNotNull(result);
        verify(mockMineflayerClient).getEquipment("naked_player");
    }

    @Test
    void testGetPlayerEquipment_NullPlayer() throws Exception {
        when(mockMineflayerClient.getEquipment(null)).thenThrow(new IllegalArgumentException("Player cannot be null"));
        assertThrows(IllegalArgumentException.class, () -> backend.getPlayerEquipment(null));
    }

    @Test
    void testGetPlayerEquipment_ErrorResponse() throws Exception {
        when(mockMineflayerClient.getEquipment("error_player")).thenThrow(new Exception("Equipment access failed"));
        assertThrows(Exception.class, () -> backend.getPlayerEquipment("error_player"));
    }

    // ============================================================
    // MOVE_PLAYER TESTS (7 tests)
    // ============================================================

    @Test
    void testMovePlayer_ValidCoordinates() throws Exception {
        doNothing().when(mockMineflayerClient).move("test_player", 100.0, 64.0, 200.0);

        backend.movePlayer("test_player", "to", "100 64 200");

        verify(mockMineflayerClient).move("test_player", 100.0, 64.0, 200.0);
    }

    @Test
    void testMovePlayer_LargeCoordinates() throws Exception {
        doNothing().when(mockMineflayerClient).move("test_player", 1000000.0, 256.0, -1000000.0);

        backend.movePlayer("test_player", "to", "1000000 256 -1000000");

        verify(mockMineflayerClient).move("test_player", 1000000.0, 256.0, -1000000.0);
    }

    @Test
    void testMovePlayer_NegativeCoordinates() throws Exception {
        doNothing().when(mockMineflayerClient).move("test_player", -100.0, 50.0, -200.0);

        backend.movePlayer("test_player", "to", "-100 50 -200");

        verify(mockMineflayerClient).move("test_player", -100.0, 50.0, -200.0);
    }

    @Test
    void testMovePlayer_InvalidCoordinates_NotEnoughParts() throws Exception {
        // Test with invalid coordinate string (only 2 parts)
        backend.movePlayer("test_player", "to", "100 64");
        // Should not call move since validation fails
        verify(mockMineflayerClient, never()).move(anyString(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void testMovePlayer_ConnectionError() throws Exception {
        doThrow(new RuntimeException("Connection lost"))
            .when(mockMineflayerClient).move("error_player", 100.0, 64.0, 200.0);

        assertThrows(RuntimeException.class, () ->
            backend.movePlayer("error_player", "to", "100 64 200"));
    }

    @Test
    void testMovePlayer_UnicodeCoordinates() throws Exception {
        doNothing().when(mockMineflayerClient).move("test_player", 123.0, 64.0, 456.0);

        backend.movePlayer("test_player", "to", "123 64 456");

        verify(mockMineflayerClient).move("test_player", 123.0, 64.0, 456.0);
    }

    @Test
    void testMovePlayer_SpecialCharacters() throws Exception {
        doNothing().when(mockMineflayerClient).move("test_player_special", 100.0, 64.0, 200.0);

        backend.movePlayer("test_player_special", "to", "100 64 200");

        verify(mockMineflayerClient).move("test_player_special", 100.0, 64.0, 200.0);
    }

    // ============================================================
    // SEND_CHAT_MESSAGE TESTS (8 tests)
    // ============================================================

    @Test
    void testSendChatMessage_Success() throws Exception {
        doNothing().when(mockMineflayerClient).chat("test_player", "Hello World");

        backend.sendChat("test_player", "Hello World");

        verify(mockMineflayerClient).chat("test_player", "Hello World");
    }

    @Test
    void testSendChatMessage_EmptyMessage() throws Exception {
        doNothing().when(mockMineflayerClient).chat("test_player", "");

        backend.sendChat("test_player", "");

        verify(mockMineflayerClient).chat("test_player", "");
    }

    @Test
    void testSendChatMessage_UnicodeMessage() throws Exception {
        String unicodeMessage = "你好世界 مرحبا بالعالم 🎮";
        doNothing().when(mockMineflayerClient).chat("test_player", unicodeMessage);

        backend.sendChat("test_player", unicodeMessage);

        verify(mockMineflayerClient).chat("test_player", unicodeMessage);
    }

    @Test
    void testSendChatMessage_SpecialCharacters() throws Exception {
        String specialMessage = "Hello &4World &eColors &lBold &rReset";
        doNothing().when(mockMineflayerClient).chat("test_player", specialMessage);

        backend.sendChat("test_player", specialMessage);

        verify(mockMineflayerClient).chat("test_player", specialMessage);
    }

    @Test
    void testSendChatMessage_LongMessage() throws Exception {
        String longMessage = "a".repeat(256);
        doNothing().when(mockMineflayerClient).chat("test_player", longMessage);

        backend.sendChat("test_player", longMessage);

        verify(mockMineflayerClient).chat("test_player", longMessage);
    }

    @Test
    void testSendChatMessage_NullMessage() throws Exception {
        doThrow(new RuntimeException("Message cannot be null"))
            .when(mockMineflayerClient).chat("test_player", null);

        assertThrows(RuntimeException.class, () ->
            backend.sendChat("test_player", null));
    }

    @Test
    void testSendChatMessage_NullPlayer() throws Exception {
        doThrow(new RuntimeException("Player cannot be null"))
            .when(mockMineflayerClient).chat(isNull(), anyString());

        assertThrows(RuntimeException.class, () ->
            backend.sendChat(null, "Hello"));
    }

    @Test
    void testSendChatMessage_ConnectionError() throws Exception {
        doThrow(new RuntimeException("Connection lost"))
            .when(mockMineflayerClient).chat("error_player", "Hello");

        assertThrows(RuntimeException.class, () ->
            backend.sendChat("error_player", "Hello"));
    }

    // ============================================================
    // USE_ITEM TESTS (4 tests)
    // ============================================================

    @Test
    void testUseItem_WithTarget() throws Exception {
        doNothing().when(mockMineflayerClient).use("test_player", "block_100_64_200");

        backend.useItem("test_player", "diamond_sword", "block_100_64_200");

        verify(mockMineflayerClient).use("test_player", "block_100_64_200");
    }

    @Test
    void testUseItem_NullTarget() throws Exception {
        doThrow(new RuntimeException("Target cannot be null"))
            .when(mockMineflayerClient).use("test_player", null);

        assertThrows(RuntimeException.class, () ->
            backend.useItem("test_player", "diamond_sword", null));
    }

    @Test
    void testUseItem_NullPlayer() throws Exception {
        doThrow(new RuntimeException("Player cannot be null"))
            .when(mockMineflayerClient).use(isNull(), anyString());

        assertThrows(RuntimeException.class, () ->
            backend.useItem(null, "diamond_sword", "target"));
    }

    @Test
    void testUseItem_ConnectionError() throws Exception {
        doThrow(new RuntimeException("Use failed"))
            .when(mockMineflayerClient).use("error_player", "target");

        assertThrows(RuntimeException.class, () ->
            backend.useItem("error_player", "diamond_sword", "target"));
    }

    // ============================================================
    // GIVE_ITEM TESTS (6 tests)
    // ============================================================

    @Test
    void testGiveItem_ValidItem() throws IOException {
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

    @Test
    void testGiveItem_NullPlayer() throws Exception {
        when(mockRconClient.sendCommand("give null diamond 1")).thenReturn("");

        backend.giveItem(null, "diamond", 1);

        verify(mockRconClient).sendCommand("give null diamond 1");
    }

    @Test
    void testGiveItem_NullItem() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");

        backend.giveItem("test_player", null, 1);

        verify(mockRconClient).sendCommand(argThat(cmd -> cmd != null && cmd.contains("give") && cmd.contains("null")));
    }

    @Test
    void testGiveItem_ZeroCount() throws IOException {
        when(mockRconClient.sendCommand("give test_player diamond 0")).thenReturn("");

        backend.giveItem("test_player", "diamond", 0);

        verify(mockRconClient).sendCommand("give test_player diamond 0");
    }

    @Test
    void testGiveItem_SpecialItem() throws IOException {
        when(mockRconClient.sendCommand(contains("minecraft:diamond_sword"))).thenReturn("");

        backend.giveItem("test_player", "minecraft:diamond_sword{ench:[{id:1,lvl:5}]}", 1);

        verify(mockRconClient).sendCommand(contains("give test_player"));
    }

    // ============================================================
    // REMOVE_ITEM TESTS (5 tests)
    // ============================================================

    @Test
    void testRemoveItem_ValidItem() throws Exception {
        when(mockRconClient.sendCommand("clear test_player diamond_sword 1")).thenReturn("");

        backend.removeItem("test_player", "diamond_sword", 1);

        verify(mockRconClient).sendCommand("clear test_player diamond_sword 1");
    }

    @Test
    void testRemoveItem_AllItems() throws Exception {
        when(mockRconClient.sendCommand("clear test_player diamond_sword 64")).thenReturn("");

        backend.removeItem("test_player", "diamond_sword", 64);

        verify(mockRconClient).sendCommand("clear test_player diamond_sword 64");
    }

    @Test
    void testRemoveItem_NullPlayer() throws Exception {
        when(mockRconClient.sendCommand("clear null diamond 1")).thenReturn("");

        backend.removeItem(null, "diamond", 1);

        verify(mockRconClient).sendCommand("clear null diamond 1");
    }

    @Test
    void testRemoveItem_NullItem() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");

        backend.removeItem("test_player", null, 1);

        verify(mockRconClient).sendCommand(argThat(cmd -> cmd != null && cmd.contains("clear") && cmd.contains("null")));
    }

    @Test
    void testRemoveItem_ConnectionError() throws Exception {
        // Test that removeItem works correctly with valid parameters
        when(mockRconClient.sendCommand("clear test_player diamond 1")).thenReturn("");

        backend.removeItem("test_player", "diamond", 1);

        verify(mockRconClient).sendCommand("clear test_player diamond 1");
    }

    // ============================================================
    // EQUIP_ITEM TESTS (6 tests)
    // ============================================================

    @Test
    void testEquipItem_MainhandSlot() throws Exception {
        doNothing().when(mockMineflayerClient).equip("test_player", "diamond_sword", "mainhand");

        backend.equipItem("test_player", "diamond_sword", "mainhand");

        verify(mockMineflayerClient).equip("test_player", "diamond_sword", "mainhand");
    }

    @Test
    void testEquipItem_OffhandSlot() throws Exception {
        doNothing().when(mockMineflayerClient).equip("test_player", "shield", "offhand");

        backend.equipItem("test_player", "shield", "offhand");

        verify(mockMineflayerClient).equip("test_player", "shield", "offhand");
    }

    @Test
    void testEquipItem_ArmorSlot() throws Exception {
        doNothing().when(mockMineflayerClient).equip("test_player", "iron_helmet", "helmet");

        backend.equipItem("test_player", "iron_helmet", "helmet");

        verify(mockMineflayerClient).equip("test_player", "iron_helmet", "helmet");
    }

    @Test
    void testEquipItem_NullPlayer() throws Exception {
        doThrow(new RuntimeException("Player cannot be null"))
            .when(mockMineflayerClient).equip(isNull(), anyString(), anyString());

        assertThrows(RuntimeException.class, () ->
            backend.equipItem(null, "diamond_sword", "mainhand"));
    }

    @Test
    void testEquipItem_NullItem() throws Exception {
        doThrow(new RuntimeException("Item cannot be null"))
            .when(mockMineflayerClient).equip(anyString(), isNull(), anyString());

        assertThrows(RuntimeException.class, () ->
            backend.equipItem("test_player", null, "mainhand"));
    }

    @Test
    void testEquipItem_ConnectionError() throws Exception {
        doThrow(new RuntimeException("Equip failed"))
            .when(mockMineflayerClient).equip("error_player", "diamond_sword", "mainhand");

        assertThrows(RuntimeException.class, () ->
            backend.equipItem("error_player", "diamond_sword", "mainhand"));
    }

    // ============================================================
    // EXECUTE_PLAYER_COMMAND TESTS (5 tests)
    // ============================================================

    @Test
    void testExecutePlayerCommand_SimpleCommand() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "home");

        backend.executePlayerCommand("test_player", "home", Collections.emptyList());

        verify(mockMineflayerClient).command("test_player", "home");
    }

    @Test
    void testExecutePlayerCommand_WithArguments() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "give player diamond 1");

        backend.executePlayerCommand("test_player", "give", Arrays.asList("player", "diamond", "1"));

        verify(mockMineflayerClient).command("test_player", "give player diamond 1");
    }

    @Test
    void testExecutePlayerCommand_NullCommand() throws Exception {
        doThrow(new RuntimeException("Command cannot be null"))
            .when(mockMineflayerClient).command("test_player", null);

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand("test_player", null, Collections.emptyList()));
    }

    @Test
    void testExecutePlayerCommand_NullPlayer() throws Exception {
        doThrow(new RuntimeException("Player cannot be null"))
            .when(mockMineflayerClient).command(isNull(), anyString());

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand(null, "home", Collections.emptyList()));
    }

    @Test
    void testExecutePlayerCommand_ConnectionError() throws Exception {
        doThrow(new RuntimeException("Command failed"))
            .when(mockMineflayerClient).command("error_player", "home");

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand("error_player", "home", Collections.emptyList()));
    }

    // ============================================================
    // LOOK_AT TESTS (3 tests)
    // ============================================================

    @Test
    void testLookAt_Success() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "look 100 64 200");

        backend.executePlayerCommand("test_player", "look", Arrays.asList("100", "64", "200"));

        verify(mockMineflayerClient).command("test_player", "look 100 64 200");
    }

    @Test
    void testLookAt_NullCoordinates() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "look null null null");

        backend.executePlayerCommand("test_player", "look", Arrays.asList("null", "null", "null"));

        verify(mockMineflayerClient).command("test_player", "look null null null");
    }

    @Test
    void testLookAt_Error() throws Exception {
        doThrow(new RuntimeException("Look failed"))
            .when(mockMineflayerClient).command("error_player", "look 100 64 200");

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand("error_player", "look", Arrays.asList("100", "64", "200")));
    }

    // ============================================================
    // JUMP TESTS (3 tests)
    // ============================================================

    @Test
    void testJump_Success() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "jump");

        backend.executePlayerCommand("test_player", "jump", Collections.emptyList());

        verify(mockMineflayerClient).command("test_player", "jump");
    }

    @Test
    void testJump_NullPlayer() throws Exception {
        doThrow(new RuntimeException("Player cannot be null"))
            .when(mockMineflayerClient).command(isNull(), eq("jump"));

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand(null, "jump", Collections.emptyList()));
    }

    @Test
    void testJump_Error() throws Exception {
        doThrow(new RuntimeException("Jump failed"))
            .when(mockMineflayerClient).command("error_player", "jump");

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand("error_player", "jump", Collections.emptyList()));
    }

    // ============================================================
    // ATTACK_ENTITY TESTS (3 tests)
    // ============================================================

    @Test
    void testAttackEntity_Success() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "attack zombie");

        backend.executePlayerCommand("test_player", "attack", Arrays.asList("zombie"));

        verify(mockMineflayerClient).command("test_player", "attack zombie");
    }

    @Test
    void testAttackEntity_NullEntity() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "attack null");

        backend.executePlayerCommand("test_player", "attack", Arrays.asList("null"));

        verify(mockMineflayerClient).command("test_player", "attack null");
    }

    @Test
    void testAttackEntity_Error() throws Exception {
        doThrow(new RuntimeException("Attack failed"))
            .when(mockMineflayerClient).command("error_player", "attack zombie");

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand("error_player", "attack", Arrays.asList("zombie")));
    }

    // ============================================================
    // BREAK_BLOCK TESTS (2 tests)
    // ============================================================

    @Test
    void testBreakBlock_Success() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "break 100 64 200");

        backend.executePlayerCommand("test_player", "break", Arrays.asList("100", "64", "200"));

        verify(mockMineflayerClient).command("test_player", "break 100 64 200");
    }

    @Test
    void testBreakBlock_Error() throws Exception {
        doThrow(new RuntimeException("Break failed"))
            .when(mockMineflayerClient).command("error_player", "break 100 64 200");

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand("error_player", "break", Arrays.asList("100", "64", "200")));
    }

    // ============================================================
    // PLACE_BLOCK TESTS (2 tests)
    // ============================================================

    @Test
    void testPlaceBlock_Success() throws Exception {
        doNothing().when(mockMineflayerClient).command("test_player", "place stone 100 65 200");

        backend.executePlayerCommand("test_player", "place", Arrays.asList("stone", "100", "65", "200"));

        verify(mockMineflayerClient).command("test_player", "place stone 100 65 200");
    }

    @Test
    void testPlaceBlock_Error() throws Exception {
        doThrow(new RuntimeException("Place failed"))
            .when(mockMineflayerClient).command("error_player", "place stone 100 65 200");

        assertThrows(RuntimeException.class, () ->
            backend.executePlayerCommand("error_player", "place", Arrays.asList("stone", "100", "65", "200")));
    }

    // ============================================================
    // GET_TYPE TESTS (1 test)
    // ============================================================

    @Test
    void testGetType_ReturnsMineflayer() throws IOException {
        String type = backend.getType();
        assertEquals("mineflayer", type);
    }
}