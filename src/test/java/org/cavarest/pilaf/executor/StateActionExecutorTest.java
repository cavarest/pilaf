package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
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
 * Unit tests for StateActionExecutor.
 */
@DisplayName("StateActionExecutor Tests")
class StateActionExecutorTest {

    private StateActionExecutor executor;
    @Mock
    private PilafBackend mockBackend;
    @Mock
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new StateActionExecutor();
    }

    @Test
    @DisplayName("getName() returns correct executor name")
    void testGetName() {
        assertEquals("StateExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes() returns correct action types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.COMPARE_STATES));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.STORE_STATE));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_CHAT_HISTORY));
        assertFalse(executor.getSupportedTypes().contains(Action.ActionType.WAIT));
    }

    @Test
    @DisplayName("execute() with COMPARE_STATES returns comparison result")
    void testExecuteCompareStates() {
        Action action = new Action(Action.ActionType.COMPARE_STATES);
        action.setState1("state1");
        action.setState2("state2");

        StateManager.ComparisonResult mockResult = mock(StateManager.ComparisonResult.class);
        when(mockResult.getBeforeJson()).thenReturn("{\"before\":true}");
        when(mockResult.getAfterJson()).thenReturn("{\"after\":true}");
        when(mockResult.getDiffJson()).thenReturn("[]");
        when(mockResult.hasChanges()).thenReturn(true);

        when(mockStateManager.compare("state1", "state2")).thenReturn(mockResult);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.hasChanges());
        assertEquals("{\"before\":true}", result.getStateBefore());
        assertEquals("{\"after\":true}", result.getStateAfter());
        assertEquals("[]", result.getStateDiff());
    }

    @Test
    @DisplayName("execute() with COMPARE_STATES stores comparison when storeAs is set")
    void testExecuteCompareStates_withStoreAs() {
        Action action = new Action(Action.ActionType.COMPARE_STATES);
        action.setState1("state1");
        action.setState2("state2");
        action.setStoreAs("comparison_result");

        StateManager.ComparisonResult mockResult = mock(StateManager.ComparisonResult.class);
        when(mockResult.getBeforeJson()).thenReturn("{\"before\":true}");
        when(mockResult.getAfterJson()).thenReturn("{\"after\":true}");
        when(mockResult.getDiffJson()).thenReturn("[]");
        when(mockResult.hasChanges()).thenReturn(true);
        when(mockResult.getBeforeState()).thenReturn("{}");
        when(mockResult.getAfterState()).thenReturn("{}");

        when(mockStateManager.compare("state1", "state2")).thenReturn(mockResult);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.hasChanges());
        verify(mockStateManager).store(eq("comparison_result"), any());
    }

    @Test
    @DisplayName("execute() with STORE_STATE stores value successfully")
    void testExecuteStoreState_success() {
        Action action = new Action(Action.ActionType.STORE_STATE);
        action.setStoreAs("my_key");
        action.setValue(42.0);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("my_key"));
        verify(mockStateManager).store("my_key", 42.0);
    }

    @Test
    @DisplayName("execute() with STORE_STATE fails when storeAs is null")
    void testExecuteStoreState_missingStoreAs() {
        Action action = new Action(Action.ActionType.STORE_STATE);
        action.setValue(42.0);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("storeAs"));
    }

    @Test
    @DisplayName("execute() with STORE_STATE fails when value is null")
    void testExecuteStoreState_missingValue() {
        Action action = new Action(Action.ActionType.STORE_STATE);
        action.setStoreAs("my_key");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("storeAs"));
    }

    @Test
    @DisplayName("execute() handles exception gracefully")
    void testExecute_handlesException() {
        Action action = new Action(Action.ActionType.COMPARE_STATES);
        action.setState1("state1");
        action.setState2("state2");

        when(mockStateManager.compare("state1", "state2"))
            .thenThrow(new RuntimeException("Database error"));

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Database error"));
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS
    // ========================================================================

    @Test
    @DisplayName("execute() with unsupported action type returns failure")
    void testExecute_unsupportedActionType_returnsFailure() {
        // Create an action with a type not in SUPPORTED_TYPES
        Action action = new Action(Action.ActionType.WAIT);
        action.setDuration(100L);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unsupported action type"));
    }

    @Test
    @DisplayName("execute() GET_CHAT_HISTORY with Mineflayer backend")
    void testExecuteGetChatHistory_withMineflayer() throws Exception {
        Action action = new Action(Action.ActionType.GET_CHAT_HISTORY);
        action.setPlayer("test_player");
        action.setStoreAs("chat_history");

        // Mock MineflayerBackend behavior
        org.cavarest.pilaf.backend.MineflayerBackend mockMineflayer = mock(org.cavarest.pilaf.backend.MineflayerBackend.class);
        when(mockMineflayer.getChatHistory("test_player")).thenReturn("[\"msg1\", \"msg2\"]");

        ActionResult result = executor.execute(action, mockMineflayer, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("chat_history"), any());
    }

    @Test
    @DisplayName("execute() GET_CHAT_HISTORY with RCON backend returns not available")
    void testExecuteGetChatHistory_withRcon() {
        Action action = new Action(Action.ActionType.GET_CHAT_HISTORY);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("not available via RCON"));
    }

    @Test
    @DisplayName("execute() GET_CHAT_HISTORY with unknown backend returns failure")
    void testExecuteGetChatHistory_withUnknownBackend() {
        Action action = new Action(Action.ActionType.GET_CHAT_HISTORY);
        action.setPlayer("test_player");

        PilafBackend mockUnknownBackend = mock(PilafBackend.class);

        ActionResult result = executor.execute(action, mockUnknownBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unknown backend type"));
    }
}
