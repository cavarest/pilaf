package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.MineflayerBackend;
import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.backend.RconBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for AbstractActionExecutor.
 */
@DisplayName("AbstractActionExecutor Tests")
class AbstractActionExecutorTest {

    private TestExecutor executor;
    private PilafBackend mockRconBackend;
    private PilafBackend mockMineflayerBackend;
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() {
        executor = new TestExecutor();
        mockRconBackend = mock(RconBackend.class);
        mockMineflayerBackend = mock(MineflayerBackend.class);
        mockStateManager = mock(StateManager.class);
    }

    @Test
    @DisplayName("serializeToJson() converts object to JSON string")
    void testSerializeToJson_withObject() {
        TestObject obj = new TestObject("test", 42);

        String json = executor.testSerializeToJson(obj);

        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("42"));
    }

    @Test
    @DisplayName("serializeToJson() returns 'null' for null input")
    void testSerializeToJson_withNull() {
        String json = executor.testSerializeToJson(null);

        assertEquals("null", json);
    }

    @Test
    @DisplayName("serializeToJson() returns toString() when JSON serialization fails")
    void testSerializeToJson_unserializableObject() {
        Object unserializable = new Object() {
            @Override
            public String toString() {
                return "custom_toString";
            }
        };

        String result = executor.testSerializeToJson(unserializable);

        assertEquals("custom_toString", result);
    }

    @Test
    @DisplayName("isMineflayer() returns true for MineflayerBackend")
    void testIsMineflayer_withMineflayerBackend() {
        assertTrue(executor.testIsMineflayer(mockMineflayerBackend));
    }

    @Test
    @DisplayName("isMineflayer() returns false for RconBackend")
    void testIsMineflayer_withRconBackend() {
        assertFalse(executor.testIsMineflayer(mockRconBackend));
    }

    @Test
    @DisplayName("isRcon() returns true for RconBackend")
    void testIsRcon_withRconBackend() {
        assertTrue(executor.testIsRcon(mockRconBackend));
    }

    @Test
    @DisplayName("isRcon() returns false for MineflayerBackend")
    void testIsRcon_withMineflayerBackend() {
        assertFalse(executor.testIsRcon(mockMineflayerBackend));
    }

    @Test
    @DisplayName("asMineflayer() casts backend to MineflayerBackend")
    void testAsMineflayer() {
        MineflayerBackend result = executor.testAsMineflayer(mockMineflayerBackend);

        assertSame(mockMineflayerBackend, result);
    }

    @Test
    @DisplayName("asRcon() casts backend to RconBackend")
    void testAsRcon() {
        RconBackend result = executor.testAsRcon(mockRconBackend);

        assertSame(mockRconBackend, result);
    }

    @Test
    @DisplayName("successWithOptionalState() stores state when storeAs is set")
    void testSuccessWithOptionalState_withStoreAs() {
        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setStoreAs("my_key");
        Object value = "test_value";

        ActionResult result = executor.testSuccessWithOptionalState("Response", action, value);

        assertTrue(result.isSuccess());
        assertTrue(result.hasStateToStore());
        assertEquals("my_key", result.getStoreKey());
        assertEquals(value, result.getStoreValue());
    }

    @Test
    @DisplayName("successWithOptionalState() does not store state when storeAs is null")
    void testSuccessWithOptionalState_withoutStoreAs() {
        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        Object value = "test_value";

        ActionResult result = executor.testSuccessWithOptionalState("Response", action, value);

        assertTrue(result.isSuccess());
        assertFalse(result.hasStateToStore());
    }

    @Test
    @DisplayName("log() prints message with executor name prefix")
    void testLog() {
        // This test just verifies the method doesn't throw
        assertDoesNotThrow(() -> executor.testLog("Test message"));
    }

    @Test
    @DisplayName("getName() returns executor name")
    void testGetName() {
        assertEquals("TestExecutor", executor.getName());
    }

    // Test helper class
    private static class TestObject {
        private final String name;
        private final int value;

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    // Concrete test executor to test abstract methods
    private static class TestExecutor extends AbstractActionExecutor {

        @Override
        public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
            return ActionResult.success("test execution");
        }

        @Override
        public java.util.Set<Action.ActionType> getSupportedTypes() {
            return java.util.EnumSet.of(Action.ActionType.WAIT);
        }

        @Override
        public String getName() {
            return "TestExecutor";
        }

        // Expose protected methods for testing
        public String testSerializeToJson(Object obj) {
            return serializeToJson(obj);
        }

        public boolean testIsMineflayer(PilafBackend backend) {
            return isMineflayer(backend);
        }

        public boolean testIsRcon(PilafBackend backend) {
            return isRcon(backend);
        }

        public MineflayerBackend testAsMineflayer(PilafBackend backend) {
            return asMineflayer(backend);
        }

        public RconBackend testAsRcon(PilafBackend backend) {
            return asRcon(backend);
        }

        public ActionResult testSuccessWithOptionalState(String response, Action action, Object value) {
            return successWithOptionalState(response, action, value);
        }

        public void testLog(String message) {
            log(message);
        }
    }
}
