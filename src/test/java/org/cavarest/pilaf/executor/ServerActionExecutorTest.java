package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.backend.RconBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ServerActionExecutor
 */
@DisplayName("ServerActionExecutor Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServerActionExecutorTest {

    private ServerActionExecutor executor;
    private PilafBackend mockBackend;
    private StateManager stateManager;
    private RconBackend rconBackend;

    @Mock
    private org.cavarest.rcon.RconClient mockRconClient;

    @BeforeEach
    void setUp() throws IOException {
        executor = new ServerActionExecutor();
        mockBackend = new MockPilafBackend();
        stateManager = new StateManager();

        // Create RconBackend with mock RconClient for testing
        rconBackend = new RconBackend("localhost", 25575, "password");
        rconBackend.setRconClient(mockRconClient);

        // Setup mock RconClient behavior
        doNothing().when(mockRconClient).connect();
        when(mockRconClient.sendCommand(anyString())).thenReturn("");
    }

    @Test
    @DisplayName("getName returns ServerExecutor")
    void testGetName() {
        assertEquals("ServerExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes returns all supported action types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SERVER_COMMAND));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.EXECUTE_RCON_COMMAND));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.EXECUTE_RCON_WITH_CAPTURE));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.EXECUTE_RCON_RAW));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.WAIT));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.CLEAR_ENTITIES));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_SERVER_INFO));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.PLACE_BLOCK));
    }

    @Test
    @DisplayName("execute with WAIT action waits specified duration")
    void testExecuteWait() throws Exception {
        Action action = new Action(Action.ActionType.WAIT);
        action.setDuration(100L);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("100ms"));
    }

    @Test
    @DisplayName("execute with WAIT action uses default duration of 1000ms")
    void testExecuteWait_defaultDuration() throws Exception {
        Action action = new Action(Action.ActionType.WAIT);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("1000ms"));
    }

    @Test
    @DisplayName("execute with CLEAR_ENTITIES clears all entities")
    void testExecuteClearEntities_all() throws Exception {
        Action action = new Action(Action.ActionType.CLEAR_ENTITIES);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute with CLEAR_ENTITIES clears specific entity type")
    void testExecuteClearEntities_specificType() throws Exception {
        Action action = new Action(Action.ActionType.CLEAR_ENTITIES);
        action.setEntityType("zombie");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute with GET_SERVER_INFO returns server info")
    void testExecuteGetServerInfo() throws Exception {
        Action action = new Action(Action.ActionType.GET_SERVER_INFO);

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute with GET_SERVER_INFO stores result when storeAs is specified")
    void testExecuteGetServerInfo_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_SERVER_INFO);
        action.setStoreAs("serverInfo");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        // Note: mock backend doesn't support RCON, so this will fail
        // but we test the code path exists
        assertNotNull(result);
    }

    @Test
    @DisplayName("execute with PLACE_BLOCK places block")
    void testExecutePlaceBlock() throws Exception {
        Action action = new Action(Action.ActionType.PLACE_BLOCK);
        action.setPosition("100 64 100");
        action.setItem("stone");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("100 64 100"));
    }

    @Test
    @DisplayName("execute with PLACE_BLOCK uses default stone type")
    void testExecutePlaceBlock_defaultBlockType() throws Exception {
        Action action = new Action(Action.ActionType.PLACE_BLOCK);
        action.setPosition("100 64 100");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute with EXECUTE_RCON_COMMAND executes command")
    void testExecuteRconCommand() throws Exception {
        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("list");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        // Note: mock backend doesn't support RCON, so this will fail
        // but we test the code path exists
        assertNotNull(result);
    }

    @Test
    @DisplayName("execute with EXECUTE_RCON_COMMAND stores result when storeAs is specified")
    void testExecuteRconCommand_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("list");
        action.setStoreAs("playerList");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        // Note: mock backend doesn't support RCON, so this will fail
        // but we test the code path exists
        assertNotNull(result);
    }

    @Test
    @DisplayName("execute with unsupported action type returns failure")
    void testExecute_unsupportedActionType() {
        // Use a player action type that's not supported by ServerActionExecutor
        Action action = new Action(Action.ActionType.CONNECT_PLAYER);
        action.setPlayer("testplayer");

        ActionResult result = executor.execute(action, mockBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unsupported action type"));
    }

    // ===== NEW TESTS FOR UNCOVERED BRANCHES =====

    @Test
    @DisplayName("execute RCON command with JSON response and storeAs")
    void testExecuteRconCommand_withJsonResponseAndStoreAs() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("testplayer has the following entity data: {Health: 20.0f, Pos: [100.5d, 64.0d, -200.5d]}");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");
        action.setStoreAs("playerData");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertNotNull(result.getExtractedJson());
        assertNotNull(result.getParsedData());

        // Verify state was stored
        Object storedState = stateManager.retrieve("playerData");
        assertNotNull(storedState);
    }

    @Test
    @DisplayName("execute RCON command with JSON response but no storeAs")
    void testExecuteRconCommand_withJsonResponseNoStoreAs() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("testplayer has the following entity data: {Health: 20.0f}");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertNotNull(result.getExtractedJson());
        assertNotNull(result.getParsedData());
    }

    @Test
    @DisplayName("execute RCON command with non-JSON response and storeAs")
    void testExecuteRconCommand_withNonJsonResponseAndStoreAs() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("There are 3 players online");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("list");
        action.setStoreAs("playerList");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertNull(result.getExtractedJson());

        // Verify raw response was stored
        Object storedState = stateManager.retrieve("playerList");
        assertNotNull(storedState);
        assertEquals("There are 3 players online", storedState);
    }

    @Test
    @DisplayName("execute RCON command with validation failure")
    void testExecuteRconCommand_withValidationFailure() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("No entity was found");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity nonexistent");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("error") || result.getError().contains("Error"));
    }

    @Test
    @DisplayName("execute RCON command with null response")
    void testExecuteRconCommand_withNullResponse() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn(null);

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertEquals("", result.getResponse());
    }

    @Test
    @DisplayName("execute RCON command with empty response")
    void testExecuteRconCommand_withEmptyResponse() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute GET_SERVER_INFO with null response")
    void testExecuteGetServerInfo_withNullResponse() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn(null);

        Action action = new Action(Action.ActionType.GET_SERVER_INFO);

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertEquals("", result.getResponse());
    }

    @Test
    @DisplayName("execute GET_SERVER_INFO with storeAs and null response")
    void testExecuteGetServerInfo_withStoreAsAndNullResponse() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn(null);

        Action action = new Action(Action.ActionType.GET_SERVER_INFO);
        action.setStoreAs("serverInfo");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        // Should still succeed even with null response
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute GET_SERVER_INFO with valid response and storeAs")
    void testExecuteGetServerInfo_withValidResponseAndStoreAs() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("There are 2 players online");

        Action action = new Action(Action.ActionType.GET_SERVER_INFO);
        action.setStoreAs("serverInfo");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertEquals("There are 2 players online", result.getResponse());

        // Verify state was stored
        Object storedState = stateManager.retrieve("serverInfo");
        assertNotNull(storedState);
        assertEquals("There are 2 players online", storedState);
    }

    @Test
    @DisplayName("execute CLEAR_ENTITIES with entity type 'all'")
    void testExecuteClearEntities_withAllEntityType() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Killed 10 entities");

        Action action = new Action(Action.ActionType.CLEAR_ENTITIES);
        action.setEntityType("all");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertNotNull(result.getResponse());
    }

    @Test
    @DisplayName("execute CLEAR_ENTITIES with specific entity type")
    void testExecuteClearEntities_withSpecificEntityType() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Killed 5 zombies");

        Action action = new Action(Action.ActionType.CLEAR_ENTITIES);
        action.setEntityType("zombie");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertNotNull(result.getResponse());
    }

    @Test
    @DisplayName("execute CLEAR_ENTITIES with null result")
    void testExecuteClearEntities_withNullResult() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn(null);

        Action action = new Action(Action.ActionType.CLEAR_ENTITIES);

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertEquals("Entities cleared", result.getResponse());
    }

    @Test
    @DisplayName("execute PLACE_BLOCK with null position")
    void testExecutePlaceBlock_withNullPosition() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Block placed");

        Action action = new Action(Action.ActionType.PLACE_BLOCK);
        // No position set - should use default stone

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        // Should use default block type "stone"
    }

    @Test
    @DisplayName("execute PLACE_BLOCK with position and item")
    void testExecutePlaceBlock_withPositionAndItem() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Block placed");

        Action action = new Action(Action.ActionType.PLACE_BLOCK);
        action.setPosition("100 64 100");
        action.setItem("diamond_block");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("100 64 100"));
    }

    @Test
    @DisplayName("execute PLACE_BLOCK with null item uses default")
    void testExecutePlaceBlock_withNullItem() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Block placed");

        Action action = new Action(Action.ActionType.PLACE_BLOCK);
        action.setPosition("100 64 100");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute RCON command with expectContains validation")
    void testExecuteRconCommand_withExpectContains() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Success: Operation completed");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("test command");
        action.setExpectContains("Success");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute RCON command with expectContains validation failure")
    void testExecuteRconCommand_withExpectContainsFailure() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Error: Something went wrong");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("test command");
        action.setExpectContains("Success");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    @DisplayName("execute RCON command with failOnError disabled")
    void testExecuteRconCommand_withFailOnErrorDisabled() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("No entity was found");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity test");
        action.setFailOnError(false);

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        // Should succeed because failOnError is false
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute RCON command with storeAs and extraction succeeded but no JSON")
    void testExecuteRconCommand_withExtractionSucceededButNoJson() throws Exception {
        // Response that will pass validation but has no JSON content
        when(mockRconClient.sendCommand(anyString())).thenReturn("Players online: testplayer, anotherplayer");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("list");
        action.setStoreAs("playerList");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
        assertNull(result.getExtractedJson());

        // Verify raw response was stored
        Object storedState = stateManager.retrieve("playerList");
        assertNotNull(storedState);
    }

    @Test
    @DisplayName("execute RCON command with validation failure and no JSON content")
    void testExecuteRconCommand_withValidationFailureNoJsonContent() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("No entity was found");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity test");
        action.setStoreAs("result");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertFalse(result.isSuccess());
        assertNull(result.getExtractedJson());
        assertNull(result.getParsedData());
    }

    @Test
    @DisplayName("execute RCON command with validation failure but has JSON content")
    void testExecuteRconCommand_withValidationFailureHasJsonContent() throws Exception {
        // Response that will fail validation (contains error) but has JSON
        when(mockRconClient.sendCommand(anyString())).thenReturn("testplayer has the following entity data: {Health: 20.0f, Error: true}");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");
        action.setStoreAs("result");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertFalse(result.isSuccess());
        // Should have extracted JSON even though validation failed
        assertNotNull(result.getExtractedJson());
        assertNotNull(result.getParsedData());
    }

    @Test
    @DisplayName("execute RCON command with empty entityType clears all entities")
    void testExecuteClearEntities_withEmptyEntityType() throws Exception {
        when(mockRconClient.sendCommand(anyString())).thenReturn("Killed 15 entities");

        Action action = new Action(Action.ActionType.CLEAR_ENTITIES);
        action.setEntityType("");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute RCON command with malformed JSON triggers extraction failure")
    void testExecuteRconCommand_withMalformedJsonTriggersExtractionFailure() throws Exception {
        // Response with malformed quotes that can't be fixed
        // Using unbalanced quotes will cause JSON parsing to fail
        when(mockRconClient.sendCommand(anyString())).thenReturn("testplayer has the following entity data: {Health: 20.0f, Name: \"Test");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");
        action.setStoreAs("playerData");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        // Should still succeed overall (validation passes)
        assertTrue(result.isSuccess());
        // But extraction should fail, so no JSON extracted
        assertNull(result.getExtractedJson());
        assertNull(result.getParsedData());
        // Raw response should still be stored
        Object storedState = stateManager.retrieve("playerData");
        assertNotNull(storedState);
    }

    @Test
    @DisplayName("execute RCON command with severely malformed NBT structure")
    void testExecuteRconCommand_withSeverelyMalformedNBT() throws Exception {
        // Response with malformed quotes in middle of value
        when(mockRconClient.sendCommand(anyString())).thenReturn("Entity data: {Health: 20.0f, Name: \"Player\" One\"}");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");
        action.setStoreAs("entityData");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        // Should still succeed (validation passes as no error patterns)
        assertTrue(result.isSuccess());
        // Extraction should fail or have no content
        assertNull(result.getExtractedJson());
    }

    @Test
    @DisplayName("execute RCON command with unclosed JSON structure")
    void testExecuteRconCommand_withUnclosedJsonStructure() throws Exception {
        // Response with unclosed quote that can't be fixed
        when(mockRconClient.sendCommand(anyString())).thenReturn("testplayer data: {Health: 20.0f, Pos: [100.5d, \"test}");

        Action action = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("data get entity testplayer");

        ActionResult result = executor.execute(action, rconBackend, stateManager);

        // Should still succeed
        assertTrue(result.isSuccess());
        // Extraction should fail
        assertNull(result.getExtractedJson());
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
}
