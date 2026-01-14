package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.model.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActionExecutorFactory.
 */
@DisplayName("ActionExecutorFactory Tests")
class ActionExecutorFactoryTest {

    @Test
    @DisplayName("Constructor registers all default executors")
    void testConstructorRegistersDefaultExecutors() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        assertEquals(8, factory.getExecutorCount());
    }

    @Test
    @DisplayName("getExecutor() returns matching executor for supported action type")
    void testGetExecutorForSupportedType() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.CONNECT_PLAYER);

        assertTrue(executor.isPresent());
        assertEquals("PlayerExecutor", executor.get().getName());
    }

    @Test
    @DisplayName("getExecutor() returns executor for wait action")
    void testGetExecutorForWaitAction() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.WAIT);

        assertTrue(executor.isPresent());
    }

    @Test
    @DisplayName("getExecutor() returns executor for inventory actions")
    void testGetExecutorForInventoryActions() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.GET_PLAYER_INVENTORY);

        assertTrue(executor.isPresent());
        assertEquals("InventoryExecutor", executor.get().getName());
    }

    @Test
    @DisplayName("getExecutor() returns executor for entity actions")
    void testGetExecutorForEntityActions() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.SPAWN_ENTITY);

        assertTrue(executor.isPresent());
        assertEquals("EntityExecutor", executor.get().getName());
    }

    @Test
    @DisplayName("getExecutor() returns executor for state actions")
    void testGetExecutorForStateActions() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.STORE_STATE);

        assertTrue(executor.isPresent());
        assertEquals("StateExecutor", executor.get().getName());
    }

    @Test
    @DisplayName("getExecutor() returns executor for assertion actions")
    void testGetExecutorForAssertionActions() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.ASSERT_RESPONSE_CONTAINS);

        assertTrue(executor.isPresent());
        assertTrue(executor.get().getName().contains("Assertion"));
    }

    @Test
    @DisplayName("getExecutor() returns executor for server actions")
    void testGetExecutorForServerActions() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.EXECUTE_RCON_COMMAND);

        assertTrue(executor.isPresent());
        assertEquals("ServerExecutor", executor.get().getName());
    }

    @Test
    @DisplayName("getExecutor() returns executor for world actions")
    void testGetExecutorForWorldActions() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.SET_TIME);

        assertTrue(executor.isPresent());
        assertEquals("WorldExecutor", executor.get().getName());
    }

    @Test
    @DisplayName("getExecutor() returns executor for client actions")
    void testGetExecutorForClientActions() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        Optional<ActionExecutor> executor = factory.getExecutor(Action.ActionType.USE_ITEM);

        assertTrue(executor.isPresent());
        assertEquals("ClientExecutor", executor.get().getName());
    }

    @Test
    @DisplayName("hasExecutor() returns true for supported action types")
    void testHasExecutorForSupportedType() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        assertTrue(factory.hasExecutor(Action.ActionType.CONNECT_PLAYER));
        assertTrue(factory.hasExecutor(Action.ActionType.GET_PLAYER_INVENTORY));
        assertTrue(factory.hasExecutor(Action.ActionType.EXECUTE_RCON_COMMAND));
    }

    @Test
    @DisplayName("registerExecutor() adds new executor to factory")
    void testRegisterExecutor() {
        ActionExecutorFactory factory = new ActionExecutorFactory();
        int initialCount = factory.getExecutorCount();

        ActionExecutor mockExecutor = new ActionExecutor() {
            @Override
            public ActionResult execute(Action action, org.cavarest.pilaf.backend.PilafBackend backend,
                                       org.cavarest.pilaf.state.StateManager stateManager) {
                return ActionResult.success("mock");
            }

            @Override
            public java.util.Set<Action.ActionType> getSupportedTypes() {
                return java.util.EnumSet.of(Action.ActionType.WAIT);
            }

            @Override
            public String getName() {
                return "MockExecutor";
            }
        };

        factory.registerExecutor(mockExecutor);

        assertEquals(initialCount + 1, factory.getExecutorCount());
        assertTrue(factory.hasExecutor(Action.ActionType.WAIT));
    }

    @Test
    @DisplayName("getAllExecutors() returns copy of executor list")
    void testGetAllExecutors() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        List<ActionExecutor> executors = factory.getAllExecutors();

        assertEquals(factory.getExecutorCount(), executors.size());

        // Verify it's a copy, not the original list
        executors.clear();
        assertEquals(8, factory.getExecutorCount()); // Factory should be unchanged
    }

    @Test
    @DisplayName("getCoveredActionTypeCount() returns number of unique action types covered")
    void testGetCoveredActionTypeCount() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        int coveredCount = factory.getCoveredActionTypeCount();

        // Should cover at least 10 different action types
        assertTrue(coveredCount >= 10, "Expected at least 10 covered action types, got " + coveredCount);
    }

    @Test
    @DisplayName("getCoveredActionTypeCount() counts each action type only once")
    void testGetCoveredActionTypeCount_noDuplicates() {
        ActionExecutorFactory factory = new ActionExecutorFactory();

        // Even though multiple executors might support similar types,
        // each type should only be counted once
        int coveredCount = factory.getCoveredActionTypeCount();

        assertTrue(coveredCount <= Action.ActionType.values().length);
    }
}
