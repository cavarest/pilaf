package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.MineflayerBackend;
import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryActionExecutor.
 */
@DisplayName("InventoryActionExecutor Tests")
class InventoryActionExecutorTest {

    private InventoryActionExecutor executor;
    @Mock
    private PilafBackend mockBackend;
    @Mock
    private MineflayerBackend mockMineflayerBackend;
    @Mock
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new InventoryActionExecutor();
    }

    @Test
    @DisplayName("getName() returns correct executor name")
    void testGetName() {
        assertEquals("InventoryExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes() returns correct action types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_INVENTORY));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_PLAYER_INVENTORY));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GIVE_ITEM));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.REMOVE_ITEM));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.EQUIP_ITEM));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_PLAYER_EQUIPMENT));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.CLEAR_INVENTORY));
        assertEquals(7, executor.getSupportedTypes().size());
    }

    @Test
    @DisplayName("execute() with GET_PLAYER_INVENTORY returns inventory JSON")
    void testExecuteGetPlayerInventory() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_INVENTORY);
        action.setPlayer("test_player");

        Map<String, Object> mockInventory = Map.of("slot", "diamond");
        when(mockMineflayerBackend.getPlayerInventory("test_player")).thenReturn(mockInventory);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("slot") || result.getResponse().contains("diamond"));
    }

    @Test
    @DisplayName("execute() with GET_INVENTORY stores inventory when storeAs is set")
    void testExecuteGetInventory_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_INVENTORY);
        action.setPlayer("test_player");
        action.setStoreAs("inventory_data");

        Map<String, Object> mockInventory = Map.of("count", 64);
        when(mockMineflayerBackend.getPlayerInventory("test_player")).thenReturn(mockInventory);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("inventory_data"), eq(mockInventory));
    }

    @Test
    @DisplayName("execute() with GET_INVENTORY handles null inventory")
    void testExecuteGetInventory_nullInventory() throws Exception {
        Action action = new Action(Action.ActionType.GET_INVENTORY);
        action.setPlayer("test_player");

        when(mockMineflayerBackend.getPlayerInventory("test_player")).thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with GIVE_ITEM returns success message")
    void testExecuteGiveItem() {
        Action action = new Action(Action.ActionType.GIVE_ITEM);
        action.setPlayer("test_player");
        action.setItem("diamond");
        action.setCount(64);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("diamond"));
        assertTrue(result.getResponse().contains("64"));
        verify(mockBackend).giveItem("test_player", "diamond", 64);
    }

    @Test
    @DisplayName("execute() with REMOVE_ITEM removes item via backend")
    void testExecuteRemoveItem() throws Exception {
        Action action = new Action(Action.ActionType.REMOVE_ITEM);
        action.setPlayer("test_player");
        action.setItem("dirt");
        action.setCount(32);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("dirt"));
        assertTrue(result.getResponse().contains("32"));
        verify(mockMineflayerBackend).removeItem("test_player", "dirt", 32);
    }

    @Test
    @DisplayName("execute() with EQUIP_ITEM equips item via backend")
    void testExecuteEquipItem() {
        Action action = new Action(Action.ActionType.EQUIP_ITEM);
        action.setPlayer("test_player");
        action.setItem("diamond_sword");
        action.setSlot("mainhand");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("diamond_sword"));
        verify(mockBackend).equipItem("test_player", "diamond_sword", "mainhand");
    }

    @Test
    @DisplayName("execute() with GET_PLAYER_EQUIPMENT returns equipment JSON")
    void testExecuteGetPlayerEquipment() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_EQUIPMENT);
        action.setPlayer("test_player");

        Map<String, Object> mockEquipment = Map.of("head", "helmet");
        when(mockMineflayerBackend.getPlayerEquipment("test_player")).thenReturn(mockEquipment);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertNotNull(result.getResponse());
    }

    @Test
    @DisplayName("execute() with GET_PLAYER_EQUIPMENT stores equipment when storeAs is set")
    void testExecuteGetEquipment_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_EQUIPMENT);
        action.setPlayer("test_player");
        action.setStoreAs("equipment_data");

        Map<String, Object> mockEquipment = Map.of("chest", "chestplate");
        when(mockMineflayerBackend.getPlayerEquipment("test_player")).thenReturn(mockEquipment);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("equipment_data"), eq(mockEquipment));
    }

    @Test
    @DisplayName("execute() with CLEAR_INVENTORY executes clear command")
    void testExecuteClearInventory() throws Exception {
        Action action = new Action(Action.ActionType.CLEAR_INVENTORY);
        action.setPlayer("test_player");

        when(mockMineflayerBackend.executeRconWithCapture("clear test_player"))
            .thenReturn("Cleared test_player's inventory");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("test_player"));
        verify(mockMineflayerBackend).executeRconWithCapture("clear test_player");
    }

    @Test
    @DisplayName("execute() with CLEAR_INVENTORY handles null response")
    void testExecuteClearInventory_nullResponse() throws Exception {
        Action action = new Action(Action.ActionType.CLEAR_INVENTORY);
        action.setPlayer("test_player");

        when(mockMineflayerBackend.executeRconWithCapture(any())).thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("Inventory cleared"));
    }

    @Test
    @DisplayName("execute() handles exception gracefully")
    void testExecute_handlesException() {
        Action action = new Action(Action.ActionType.GIVE_ITEM);
        action.setPlayer("test_player");

        doThrow(new RuntimeException("Player not found"))
            .when(mockBackend).giveItem(any(), any(), any());

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Player not found"));
    }

    @Test
    @DisplayName("execute() returns failure for unsupported action type")
    void testExecute_unsupportedActionType() {
        Action action = new Action(Action.ActionType.WAIT);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unsupported action type"));
    }

    @Test
    @DisplayName("execute() with GIVE_ITEM uses null count when not set")
    void testExecuteGiveItem_nullCount() {
        Action action = new Action(Action.ActionType.GIVE_ITEM);
        action.setPlayer("test_player");
        action.setItem("stone");
        // count not set, so it's null

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).giveItem("test_player", "stone", null);
    }

    @Test
    @DisplayName("execute() with REMOVE_ITEM uses specific count")
    void testExecuteRemoveItem_specificCount() throws Exception {
        Action action = new Action(Action.ActionType.REMOVE_ITEM);
        action.setPlayer("test_player");
        action.setItem("arrow");
        action.setCount(16);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).removeItem("test_player", "arrow", 16);
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS - RCON BACKEND
    // ========================================================================

    @Test
    @DisplayName("execute() GET_INVENTORY with RCON backend")
    void testExecuteGetInventory_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_INVENTORY);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        Map<String, Object> mockInventory = Map.of("slot", "diamond");
        when(mockRcon.getPlayerInventory("test_player")).thenReturn(mockInventory);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getPlayerInventory("test_player");
    }

    @Test
    @DisplayName("execute() REMOVE_ITEM with RCON backend")
    void testExecuteRemoveItem_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.REMOVE_ITEM);
        action.setPlayer("test_player");
        action.setItem("dirt");
        action.setCount(32);

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        doNothing().when(mockRcon).removeItem("test_player", "dirt", 32);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("dirt"));
        verify(mockRcon).removeItem("test_player", "dirt", 32);
    }

    @Test
    @DisplayName("execute() GET_PLAYER_EQUIPMENT with RCON backend")
    void testExecuteGetEquipment_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_EQUIPMENT);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        Map<String, Object> mockEquipment = Map.of("head", "helmet");
        when(mockRcon.getPlayerEquipment("test_player")).thenReturn(mockEquipment);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getPlayerEquipment("test_player");
    }

    @Test
    @DisplayName("execute() CLEAR_INVENTORY with RCON backend")
    void testExecuteClearInventory_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.CLEAR_INVENTORY);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.executeRconWithCapture("clear test_player"))
            .thenReturn("Cleared test_player's inventory");

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).executeRconWithCapture("clear test_player");
    }
}
