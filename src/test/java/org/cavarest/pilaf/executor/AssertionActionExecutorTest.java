package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AssertionActionExecutor
 */
@DisplayName("AssertionActionExecutor Tests")
class AssertionActionExecutorTest {

    private AssertionActionExecutor executor;
    private PilafBackend mockBackend;
    private StateManager stateManager;

    @BeforeEach
    void setUp() {
        executor = new AssertionActionExecutor();
        mockBackend = new MockPilafBackend();
        stateManager = new StateManager();
    }

    @Test
    @DisplayName("getName returns AssertionExecutor")
    void testGetName() {
        assertEquals("AssertionExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes returns all supported assertion types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ASSERT_RESPONSE_CONTAINS));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ASSERT_ENTITY_EXISTS));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ASSERT_ENTITY_MISSING));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ASSERT_PLAYER_HAS_ITEM));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ASSERT_JSON_EQUALS));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ASSERT_LOG_CONTAINS));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.ASSERT_CONDITION));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.PRINT_STATE_COMPARISON));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.PRINT_STORED_STATE));
    }

    @Test
    @DisplayName("execute with ASSERT_RESPONSE_CONTAINS with no source returns failure")
    void testAssertResponseContains_noSource_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setContains("expected text");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'source'"));
    }

    @Test
    @DisplayName("execute with ASSERT_RESPONSE_CONTAINS with no contains returns failure")
    void testAssertResponseContains_noContains_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("myState");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'contains'"));
    }

    @Test
    @DisplayName("execute with ASSERT_RESPONSE_CONTAINS with stored value passes when contains text")
    void testAssertResponseContains_storedValueContainsText_passes() {
        stateManager.store("myState", "This is the expected response text");

        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("myState");
        action.setContains("expected response");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("PASSED"));
    }

    @Test
    @DisplayName("execute with ASSERT_RESPONSE_CONTAINS with stored value fails when not contains text")
    void testAssertResponseContains_storedValueNotContainsText_fails() {
        stateManager.store("myState", "This is different text");

        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("myState");
        action.setContains("expected");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("FAILED"));
    }

    @Test
    @DisplayName("execute with ASSERT_ENTITY_EXISTS with no entity returns failure")
    void testAssertEntityExists_noEntity_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'entity'"));
    }

    @Test
    @DisplayName("execute with ASSERT_ENTITY_EXISTS returns failure when entity doesn't exist")
    void testAssertEntityExists_entityDoesNotExist_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);
        action.setEntity("test_entity");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("FAILED"));
    }

    @Test
    @DisplayName("execute with ASSERT_ENTITY_MISSING with no entity returns failure")
    void testAssertEntityMissing_noEntity_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_ENTITY_MISSING);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'entity'"));
    }

    @Test
    @DisplayName("execute with ASSERT_ENTITY_MISSING passes when entity is missing")
    void testAssertEntityMissing_entityMissing_passes() {
        Action action = new Action(Action.ActionType.ASSERT_ENTITY_MISSING);
        action.setEntity("test_entity");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("PASSED"));
    }

    @Test
    @DisplayName("execute with ASSERT_PLAYER_HAS_ITEM with no player returns failure")
    void testAssertPlayerHasItem_noPlayer_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_PLAYER_HAS_ITEM);
        action.setItem("diamond");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'player'"));
    }

    @Test
    @DisplayName("execute with ASSERT_PLAYER_HAS_ITEM with no item returns failure")
    void testAssertPlayerHasItem_noItem_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_PLAYER_HAS_ITEM);
        action.setPlayer("testplayer");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'item'"));
    }

    @Test
    @DisplayName("execute with ASSERT_PLAYER_HAS_ITEM returns failure when player doesn't have item")
    void testAssertPlayerHasItem_playerDoesNotHaveItem_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_PLAYER_HAS_ITEM);
        action.setPlayer("testplayer");
        action.setItem("diamond");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("FAILED"));
    }

    @Test
    @DisplayName("execute with ASSERT_JSON_EQUALS with no state values returns failure")
    void testAssertJsonEquals_noStateValues_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_JSON_EQUALS);
        action.setState1("state1");
        action.setState2("state2");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        // Both states are null, so they should be equal
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute with ASSERT_JSON_EQUALS compares stored values")
    void testAssertJsonEquals_comparesStoredValues() {
        stateManager.store("state1", "value1");
        stateManager.store("state2", "value2");

        Action action = new Action(Action.ActionType.ASSERT_JSON_EQUALS);
        action.setState1("state1");
        action.setState2("state2");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("FAILED"));
    }

    @Test
    @DisplayName("execute with ASSERT_JSON_EQUALS with equal values passes")
    void testAssertJsonEquals_equalValues_passes() {
        stateManager.store("state1", "same");
        stateManager.store("state2", "same");

        Action action = new Action(Action.ActionType.ASSERT_JSON_EQUALS);
        action.setState1("state1");
        action.setState2("state2");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("PASSED"));
    }

    @Test
    @DisplayName("execute with ASSERT_LOG_CONTAINS returns not implemented failure")
    void testAssertLogContains_notImplemented() {
        Action action = new Action(Action.ActionType.ASSERT_LOG_CONTAINS);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not yet implemented"));
    }

    @Test
    @DisplayName("execute with ASSERT_CONDITION returns not implemented failure")
    void testAssertCondition_notImplemented() {
        Action action = new Action(Action.ActionType.ASSERT_CONDITION);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not yet implemented"));
    }

    @Test
    @DisplayName("execute with PRINT_STATE_COMPARISON with no variableName returns failure")
    void testPrintStateComparison_noVariableName_returnsFailure() {
        Action action = new Action(Action.ActionType.PRINT_STATE_COMPARISON);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'variableName'"));
    }

    @Test
    @DisplayName("execute with PRINT_STATE_COMPARISON prints stored value")
    void testPrintStateComparison_printsStoredValue() {
        stateManager.store("myComparison", "test data");

        Action action = new Action(Action.ActionType.PRINT_STATE_COMPARISON);
        action.setVariableName("myComparison");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("myComparison"));
        assertTrue(result.getResponse().contains("test data"));
    }

    @Test
    @DisplayName("execute with PRINT_STORED_STATE with no variableName returns failure")
    void testPrintStoredState_noVariableName_returnsFailure() {
        Action action = new Action(Action.ActionType.PRINT_STORED_STATE);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'variableName'"));
    }

    @Test
    @DisplayName("execute with PRINT_STORED_STATE prints stored value")
    void testPrintStoredState_printsStoredValue() {
        stateManager.store("myState", "state data");

        Action action = new Action(Action.ActionType.PRINT_STORED_STATE);
        action.setVariableName("myState");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("myState"));
        assertTrue(result.getResponse().contains("state data"));
    }

    @Test
    @DisplayName("execute with PRINT_STORED_STATE with null value shows not found message")
    void testPrintStoredState_nullValue_showsNotFoundMessage() {
        Action action = new Action(Action.ActionType.PRINT_STORED_STATE);
        action.setVariableName("nonExistent");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("null or not found"));
    }

    @Test
    @DisplayName("execute with unsupported action type returns failure")
    void testExecute_unsupportedActionType() {
        // Use an action type not supported by AssertionActionExecutor
        Action action = new Action(Action.ActionType.WAIT);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unsupported action type"));
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS - EDGE CASES AND EXCEPTION HANDLING
    // ========================================================================

    @Test
    @DisplayName("execute() handles exception gracefully")
    void testExecute_handlesException() {
        // Create a custom mock backend that throws exception
        PilafBackend throwingBackend = new PilafBackend() {
            @Override public void setVerbose(boolean verbose) {}
            @Override public String getType() { return "throwing"; }
            @Override public void initialize() throws Exception {}
            @Override public void cleanup() throws Exception {}
            @Override public void movePlayer(String playerName, String destinationType, String destination) {}
            @Override public void equipItem(String playerName, String item, String slot) {}
            @Override public void giveItem(String playerName, String item, Integer count) {}
            @Override public void executePlayerCommand(String playerName, String command, java.util.List<String> arguments) {}
            @Override public void sendChat(String playerName, String message) {}
            @Override public void useItem(String playerName, String item, String target) {}
            @Override public void spawnEntity(String name, String type, java.util.List<Double> location, java.util.Map<String, String> equipment) {}
            @Override public boolean entityExists(String entityName) { throw new RuntimeException("Test exception"); }
            @Override public double getEntityHealth(String entityName) { return 20.0; }
            @Override public void setEntityHealth(String entityName, Double health) {}
            @Override public void executeServerCommand(String command, java.util.List<String> arguments) {}
            @Override public boolean playerInventoryContains(String playerName, String item, String slot) { return false; }
            @Override public boolean pluginReceivedCommand(String pluginName, String command, String playerName) { return false; }
            @Override public void removeAllTestEntities() {}
            @Override public void removeAllTestPlayers() {}
            @Override public String getServerLog() { return ""; }
        };

        Action action = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);
        action.setEntity("test");

        ActionResult result = executor.execute(action, throwingBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Test exception"));
    }

    @Test
    @DisplayName("execute() ASSERT_RESPONSE_CONTAINS with empty source returns failure")
    void testAssertResponseContains_emptySource_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("");  // Empty string
        action.setContains("text");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'source'"));
    }

    @Test
    @DisplayName("execute() ASSERT_RESPONSE_CONTAINS with empty contains returns failure")
    void testAssertResponseContains_emptyContains_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("myState");
        action.setContains("");  // Empty string

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'contains'"));
    }

    @Test
    @DisplayName("execute() ASSERT_RESPONSE_CONTAINS with null stored value")
    void testAssertResponseContains_nullStoredValue() {
        // Don't store anything, so retrieve returns null
        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("nonExistent");
        action.setContains("anything");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("FAILED"));
        assertTrue(result.getResponse().contains("null"));
    }

    @Test
    @DisplayName("execute() ASSERT_ENTITY_EXISTS with empty entity name returns failure")
    void testAssertEntityExists_emptyEntity_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);
        action.setEntity("");  // Empty string

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'entity'"));
    }

    @Test
    @DisplayName("execute() ASSERT_ENTITY_EXISTS when entity exists passes")
    void testAssertEntityExists_entityExists_passes() {
        // Use a backend that returns true for entityExists
        PilafBackend existingEntityBackend = new MockPilafBackend() {
            @Override public boolean entityExists(String entityName) { return true; }
        };

        Action action = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);
        action.setEntity("existing_entity");

        ActionResult result = executor.execute(action, existingEntityBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("PASSED"));
    }

    @Test
    @DisplayName("execute() ASSERT_ENTITY_MISSING with empty entity name returns failure")
    void testAssertEntityMissing_emptyEntity_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_ENTITY_MISSING);
        action.setEntity("");  // Empty string

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'entity'"));
    }

    @Test
    @DisplayName("execute() ASSERT_ENTITY_MISSING when entity exists fails")
    void testAssertEntityMissing_entityExists_fails() {
        PilafBackend existingEntityBackend = new MockPilafBackend() {
            @Override public boolean entityExists(String entityName) { return true; }
        };

        Action action = new Action(Action.ActionType.ASSERT_ENTITY_MISSING);
        action.setEntity("existing_entity");

        ActionResult result = executor.execute(action, existingEntityBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("FAILED"));
    }

    @Test
    @DisplayName("execute() ASSERT_PLAYER_HAS_ITEM with empty player name returns failure")
    void testAssertPlayerHasItem_emptyPlayer_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_PLAYER_HAS_ITEM);
        action.setPlayer("");  // Empty string
        action.setItem("diamond");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'player'"));
    }

    @Test
    @DisplayName("execute() ASSERT_PLAYER_HAS_ITEM with empty item name returns failure")
    void testAssertPlayerHasItem_emptyItem_returnsFailure() {
        Action action = new Action(Action.ActionType.ASSERT_PLAYER_HAS_ITEM);
        action.setPlayer("testplayer");
        action.setItem("");  // Empty string

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'item'"));
    }

    @Test
    @DisplayName("execute() ASSERT_PLAYER_HAS_ITEM when player has item passes")
    void testAssertPlayerHasItem_playerHasItem_passes() {
        PilafBackend hasItemBackend = new MockPilafBackend() {
            @Override public boolean playerInventoryContains(String playerName, String item, String slot) { return true; }
        };

        Action action = new Action(Action.ActionType.ASSERT_PLAYER_HAS_ITEM);
        action.setPlayer("testplayer");
        action.setItem("diamond");

        ActionResult result = executor.execute(action, hasItemBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("PASSED"));
    }

    @Test
    @DisplayName("execute() ASSERT_JSON_EQUALS with null state1 returns failure")
    void testAssertJsonEquals_nullState1_returnsFailure() {
        stateManager.store("state2", "value2");

        Action action = new Action(Action.ActionType.ASSERT_JSON_EQUALS);
        action.setState1(null);  // Null
        action.setState2("state2");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing state1 or state2"));
    }

    @Test
    @DisplayName("execute() ASSERT_JSON_EQUALS with null state2 returns failure")
    void testAssertJsonEquals_nullState2_returnsFailure() {
        stateManager.store("state1", "value1");

        Action action = new Action(Action.ActionType.ASSERT_JSON_EQUALS);
        action.setState1("state1");
        action.setState2(null);  // Null

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing state1 or state2"));
    }

    @Test
    @DisplayName("execute() PRINT_STATE_COMPARISON with empty variableName returns failure")
    void testPrintStateComparison_emptyVariableName_returnsFailure() {
        Action action = new Action(Action.ActionType.PRINT_STATE_COMPARISON);
        action.setVariableName("");  // Empty string

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'variableName'"));
    }

    @Test
    @DisplayName("execute() PRINT_STATE_COMPARISON with null stored value")
    void testPrintStateComparison_nullStoredValue() {
        Action action = new Action(Action.ActionType.PRINT_STATE_COMPARISON);
        action.setVariableName("nonExistent");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("null or not found"));
    }

    @Test
    @DisplayName("execute() PRINT_STORED_STATE with empty variableName returns failure")
    void testPrintStoredState_emptyVariableName_returnsFailure() {
        Action action = new Action(Action.ActionType.PRINT_STORED_STATE);
        action.setVariableName("");  // Empty string

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Missing 'variableName'"));
    }

    // Mock PilafBackend for testing
    private static class MockPilafBackend implements PilafBackend {
        @Override
        public void setVerbose(boolean verbose) {}

        @Override
        public String getType() { return "mock"; }

        @Override
        public void initialize() throws Exception {}

        @Override
        public void cleanup() throws Exception {}

        @Override
        public void movePlayer(String playerName, String destinationType, String destination) {}

        @Override
        public void equipItem(String playerName, String item, String slot) {}

        @Override
        public void giveItem(String playerName, String item, Integer count) {}

        @Override
        public void executePlayerCommand(String playerName, String command, java.util.List<String> arguments) {}

        @Override
        public void sendChat(String playerName, String message) {}

        @Override
        public void useItem(String playerName, String item, String target) {}

        @Override
        public void spawnEntity(String name, String type, java.util.List<Double> location,
                               java.util.Map<String, String> equipment) {}

        @Override
        public boolean entityExists(String entityName) { return false; }

        @Override
        public double getEntityHealth(String entityName) { return 20.0; }

        @Override
        public void setEntityHealth(String entityName, Double health) {}

        @Override
        public void executeServerCommand(String command, java.util.List<String> arguments) {}

        @Override
        public boolean playerInventoryContains(String playerName, String item, String slot) { return false; }

        @Override
        public boolean pluginReceivedCommand(String pluginName, String command, String playerName) { return false; }

        @Override
        public void removeAllTestEntities() {}

        @Override
        public void removeAllTestPlayers() {}

        @Override
        public String getServerLog() { return ""; }
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS - TRUNCATION
    // ========================================================================

    @Test
    @DisplayName("execute with ASSERT_RESPONSE_CONTAINS truncates long response")
    void testExecuteAssertResponseContains_truncatesLongResponse() {
        // Create a response longer than 500 characters to trigger truncation
        StringBuilder longResponse = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            longResponse.append("x");
        }
        String longResponseStr = longResponse.toString();

        stateManager.store("previous_step", longResponseStr);

        Action action = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("previous_step");
        action.setContains("xxx");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        // The response should be truncated
        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("truncated"));
        assertTrue(result.getResponse().contains("total"));
        assertTrue(result.getResponse().contains("600"));
    }
}
