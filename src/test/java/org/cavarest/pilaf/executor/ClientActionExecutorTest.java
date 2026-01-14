package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.MineflayerBackend;
import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.backend.RconBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClientActionExecutor.
 */
@DisplayName("ClientActionExecutor Tests")
class ClientActionExecutorTest {

    private ClientActionExecutor executor;
    @Mock
    private PilafBackend mockBackend;
    @Mock
    private MineflayerBackend mockMineflayerBackend;
    @Mock
    private RconBackend mockRconBackend;
    @Mock
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new ClientActionExecutor();
    }

    @Test
    @DisplayName("getName() returns correct executor name")
    void testGetName() {
        assertEquals("ClientExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes() returns correct action types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.USE_ITEM));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ATTACK_ENTITY));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.LOOK_AT));
        assertEquals(3, executor.getSupportedTypes().size());
    }

    @Test
    @DisplayName("execute() with USE_ITEM returns success")
    void testExecuteUseItem() {
        Action action = new Action(Action.ActionType.USE_ITEM);
        action.setPlayer("test_player");
        action.setItem("diamond_sword");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("diamond_sword"));
        verify(mockBackend).useItem("test_player", "diamond_sword", null);
    }

    @Test
    @DisplayName("execute() with USE_ITEM includes entity when specified")
    void testExecuteUseItem_withEntity() {
        Action action = new Action(Action.ActionType.USE_ITEM);
        action.setPlayer("test_player");
        action.setItem("bow");
        action.setEntity("zombie");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).useItem("test_player", "bow", "zombie");
    }

    @Test
    @DisplayName("execute() with ATTACK_ENTITY attacks nearest entity")
    void testExecuteAttackEntity_nearest() throws Exception {
        Action action = new Action(Action.ActionType.ATTACK_ENTITY);
        // No entity specified, should use default target

        when(mockMineflayerBackend.executeRconWithCapture("damage @e[sort=nearest,limit=1] 1 minecraft:player_attack"))
            .thenReturn("Dealt 1 damage to entity");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("damage"));
        verify(mockMineflayerBackend).executeRconWithCapture("damage @e[sort=nearest,limit=1] 1 minecraft:player_attack");
    }

    @Test
    @DisplayName("execute() with ATTACK_ENTITY attacks specific entity")
    void testExecuteAttackEntity_specific() throws Exception {
        Action action = new Action(Action.ActionType.ATTACK_ENTITY);
        action.setEntity("@e[type=zombie]");

        when(mockMineflayerBackend.executeRconWithCapture("damage @e[type=zombie] 1 minecraft:player_attack"))
            .thenReturn("Dealt 1 damage");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("damage @e[type=zombie] 1 minecraft:player_attack");
    }

    @Test
    @DisplayName("execute() with ATTACK_ENTITY handles null RCON response")
    void testExecuteAttackEntity_nullResponse() throws Exception {
        Action action = new Action(Action.ActionType.ATTACK_ENTITY);
        action.setEntity("test_entity");

        when(mockMineflayerBackend.executeRconWithCapture(any())).thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("test_entity"));
    }

    @Test
    @DisplayName("execute() with LOOK_AT returns success")
    void testExecuteLookAt() {
        Action action = new Action(Action.ActionType.LOOK_AT);
        action.setEntity("@p");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("@p"));
    }

    @Test
    @DisplayName("execute() with LOOK_AT uses entity field")
    void testExecuteLookAt_withEntity() {
        Action action = new Action(Action.ActionType.LOOK_AT);
        action.setEntity("villager");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("villager"));
    }

    @Test
    @DisplayName("execute() returns failure for exception")
    void testExecute_handlesException() {
        Action action = new Action(Action.ActionType.USE_ITEM);

        doThrow(new RuntimeException("Connection lost"))
            .when(mockBackend).useItem(any(), any(), any());

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Connection lost"));
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
    @DisplayName("execute() with ATTACK_ENTITY uses RCON backend when available")
    void testExecuteAttackEntity_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.ATTACK_ENTITY);
        action.setEntity("zombie");

        when(mockRconBackend.executeRconWithCapture("damage zombie 1 minecraft:player_attack"))
            .thenReturn("Dealt 1 damage");

        ActionResult result = executor.execute(action, mockRconBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("damage"));
        verify(mockRconBackend).executeRconWithCapture("damage zombie 1 minecraft:player_attack");
    }
}
