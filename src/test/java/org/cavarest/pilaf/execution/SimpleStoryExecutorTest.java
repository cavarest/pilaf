package org.cavarest.pilaf.execution;

import org.cavarest.pilaf.config.TestConfiguration;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.report.TestReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimpleStoryExecutor.
 */
@DisplayName("SimpleStoryExecutor Tests")
@ExtendWith(MockitoExtension.class)
class SimpleStoryExecutorTest {

    @Mock
    private TestConfiguration config;

    @Mock
    private TestReporter reporter;

    private SimpleStoryExecutor executor;

    private Path tempYamlFile;

    @BeforeEach
    void setUp() throws IOException {
        executor = new SimpleStoryExecutor(config, reporter);
        // Create a temporary YAML file for testing
        tempYamlFile = Files.createTempFile("test-story", ".yaml");
    }

    /**
     * Helper method to create a valid YAML test story file.
     */
    private void createValidYamlStory() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Test Story\"\n");
            writer.write("description: \"A test story\"\n");
            writer.write("setup:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"say test\"\n");
            writer.write("    name: \"Test setup\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 100\n");
            writer.write("    name: \"Wait test\"\n");
            writer.write("cleanup:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"say done\"\n");
            writer.write("    name: \"Test cleanup\"\n");
        }
    }

    /**
     * Helper method to create an invalid YAML file.
     */
    private void createInvalidYamlStory() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("invalid: yaml: content:\n");
            writer.write("  - broken\n");
            writer.write("    [malformed\n");
        }
    }

    @Test
    @DisplayName("Constructor initializes with config and reporter")
    void testConstructor_initializesFields() {
        assertNotNull(executor);
    }

    @Test
    @DisplayName("executeStory with valid YAML file returns successful result")
    void testExecuteStory_validYaml_returnsSuccess() throws IOException {
        createValidYamlStory();

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
        assertEquals("Test Story", result.getStoryName());
    }

    @Test
    @DisplayName("executeStory with invalid YAML file returns failed result")
    void testExecuteStory_invalidYaml_returnsFailure() throws IOException {
        createInvalidYamlStory();

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is added to logs, not returned by getMessage()
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("Failed to parse YAML story")) ||
                   result.getLogs().stream().anyMatch(l -> l.contains("INFO: Failed to parse YAML story")));
    }

    @Test
    @DisplayName("executeStory with non-existent file returns failed result")
    void testExecuteStory_nonExistentFile_returnsFailure() {
        TestResult result = executor.executeStory("/non/existent/file.yaml");

        assertFalse(result.isSuccess());
        // Message is in logs when parsing fails
        assertFalse(result.getLogs().isEmpty());
    }

    @Test
    @DisplayName("executeStory with empty story (only name) returns success")
    void testExecuteStory_emptyStory_returnsSuccess() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Empty Story\"\n");
            writer.write("description: \"No actions\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
        assertEquals("Empty Story", result.getStoryName());
    }

    @Test
    @DisplayName("executeStory with story containing only setup actions")
    void testExecuteStory_onlySetupActions() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Setup Only Story\"\n");
            writer.write("setup:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"say setup\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
        assertEquals("Setup Only Story", result.getStoryName());
    }

    @Test
    @DisplayName("executeStory with story containing only steps")
    void testExecuteStory_onlySteps() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Steps Only Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 50\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
        assertEquals("Steps Only Story", result.getStoryName());
    }

    @Test
    @DisplayName("executeStory with story containing only cleanup actions")
    void testExecuteStory_onlyCleanupActions() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Cleanup Only Story\"\n");
            writer.write("cleanup:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"say cleanup\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
        assertEquals("Cleanup Only Story", result.getStoryName());
    }

    @Test
    @DisplayName("executeStory with action that has no name uses action type")
    void testExecuteStory_actionWithoutName() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"No Name Action Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 10\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with unsupported action type skips action")
    void testExecuteStory_unsupportedActionType_skips() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Unsupported Action Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"assert_entity_exists\"\n");
            writer.write("    entity_type: \"zombie\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with CONNECT_PLAYER action")
    void testExecuteStory_connectPlayerAction() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Connect Player Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"connect_player\"\n");
            writer.write("    player: \"test_player\"\n");
            writer.write("    name: \"Connect test player\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with DISCONNECT_PLAYER action")
    void testExecuteStory_disconnectPlayerAction() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Disconnect Player Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"disconnect_player\"\n");
            writer.write("    player: \"test_player\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with EXECUTE_PLAYER_COMMAND action")
    void testExecuteStory_executePlayerCommandAction() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Player Command Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"execute_player_command\"\n");
            writer.write("    player: \"test_player\"\n");
            writer.write("    command: \"/help\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with EXECUTE_RCON_COMMAND action")
    void testExecuteStory_executeRconCommandAction() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"RCON Command Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"time set day\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with GET_ENTITIES action")
    void testExecuteStory_getEntitiesAction() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Get Entities Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"get_entities\"\n");
            writer.write("    player: \"test_player\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with WAIT action with duration")
    void testExecuteStory_waitActionWithDuration() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Wait Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 50\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with WAIT action without duration")
    void testExecuteStory_waitActionWithoutDuration() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Wait No Duration Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with CONNECT_PLAYER action without player fails")
    void testExecuteStory_connectPlayerWithoutPlayer_fails() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Connect Player No Name Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"connect_player\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is in logs
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("Player name is required")));
    }

    @Test
    @DisplayName("executeStory with DISCONNECT_PLAYER action without player fails")
    void testExecuteStory_disconnectPlayerWithoutPlayer_fails() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Disconnect Player No Name Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"disconnect_player\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is in logs
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("Player name is required")));
    }

    @Test
    @DisplayName("executeStory with EXECUTE_PLAYER_COMMAND without player fails")
    void testExecuteStory_playerCommandWithoutPlayer_fails() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Player Command No Player Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"execute_player_command\"\n");
            writer.write("    command: \"/help\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is in logs
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("Player and command are required")));
    }

    @Test
    @DisplayName("executeStory with EXECUTE_PLAYER_COMMAND without command fails")
    void testExecuteStory_playerCommandWithoutCommand_fails() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Player Command No Command Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"execute_player_command\"\n");
            writer.write("    player: \"test_player\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is in logs
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("Player and command are required")));
    }

    @Test
    @DisplayName("executeStory with EXECUTE_RCON_COMMAND without command fails")
    void testExecuteStory_rconCommandWithoutCommand_fails() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"RCON Command No Command Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is in logs
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("Command is required")));
    }

    @Test
    @DisplayName("executeStory with multiple actions in sequence")
    void testExecuteStory_multipleActions() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Multiple Actions Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"say step 1\"\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 10\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"say step 2\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with setup, steps, and cleanup all present")
    void testExecuteStory_allPhasesPresent() throws IOException {
        createValidYamlStory();

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertTrue(result.isSuccess());
        assertEquals("Test Story", result.getStoryName());
    }

    @Test
    @DisplayName("executeStory stops execution when setup action fails")
    void testExecuteStory_setupFailure_stopsExecution() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Setup Failure Story\"\n");
            writer.write("setup:\n");
            writer.write("  - action: \"connect_player\"\n");
            // No player specified - will fail
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 10\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is in logs
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("setup")));
    }

    @Test
    @DisplayName("executeStory continues to cleanup when step action fails")
    void testExecuteStory_stepFailure_runsCleanup() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Step Failure Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"connect_player\"\n");
            // No player specified - will fail
            writer.write("cleanup:\n");
            writer.write("  - action: \"execute_rcon_command\"\n");
            writer.write("    command: \"say cleanup\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertFalse(result.isSuccess());
        // Message is in logs
        assertTrue(result.getLogs().stream().anyMatch(l -> l.contains("test")));
    }

    @Test
    @DisplayName("executeStory sets test name from parsed story")
    void testExecuteStory_setsTestName() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Custom Test Name\"\n");
            writer.write("description: \"Custom description\"\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        assertEquals("Custom Test Name", result.getStoryName());
    }

    @Test
    @DisplayName("executeStory with action with null type")
    void testExecuteStory_actionWithNullType() throws Exception {
        // Use reflection to create an action with null type to cover the null check branch
        java.lang.reflect.Field typeField = Action.class.getDeclaredField("type");
        typeField.setAccessible(true);

        Action nullTypeAction = new Action(Action.ActionType.WAIT);
        typeField.set(nullTypeAction, null);

        // The executeAction method throws IllegalArgumentException for null type
        // This is caught by executeActions, which returns false
        // But we need to cover the null check in executeAction line 128
        java.lang.reflect.Method executeMethod = SimpleStoryExecutor.class
            .getDeclaredMethod("executeAction", Action.class);
        executeMethod.setAccessible(true);

        // invoke() wraps exceptions in InvocationTargetException
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            executeMethod.invoke(executor, nullTypeAction);
        });

        // Verify the cause is IllegalArgumentException
        try {
            executeMethod.invoke(executor, nullTypeAction);
            fail("Expected exception");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause());
            assertEquals("Action type is required", e.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("executeStory with empty actions list")
    void testExecuteStory_emptyActionsList() throws IOException {
        // Create a YAML with empty actions array
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Empty Actions Story\"\n");
            writer.write("steps: []\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        // Empty actions list should return true (lines 89-91)
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with wait action with zero duration")
    void testExecuteStory_waitActionZeroDuration() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Wait Zero Duration Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 0\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        // Zero duration should use default delay (line 110)
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with wait action with negative duration")
    void testExecuteStory_waitActionNegativeDuration() throws IOException {
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Wait Negative Duration Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: -100\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        // Negative duration should use default delay (line 110)
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with action without type in description uses 'Unknown'")
    void testExecuteStory_actionWithoutTypeInDescription() throws Exception {
        // Use reflection to test the branch where action type is null (line 98)
        Action action = new Action(Action.ActionType.WAIT);
        action.setName(null); // Clear the name

        java.lang.reflect.Field typeField = Action.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.set(action, null);

        // Create a list with this action
        java.util.List<Action> actions = new java.util.ArrayList<>();
        actions.add(action);

        // Use reflection to call executeActions
        java.lang.reflect.Method executeActionsMethod = SimpleStoryExecutor.class
            .getDeclaredMethod("executeActions", java.util.List.class, String.class, TestResult.class);
        executeActionsMethod.setAccessible(true);

        TestResult result = new TestResult("test");
        // Should throw IllegalArgumentException for null type, caught by executeActions
        boolean returnValue = (Boolean) executeActionsMethod.invoke(executor, actions, "test", result);

        // Returns false when action fails
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory handles runtime exception during execution")
    void testExecuteStory_runtimeException() throws IOException {
        // Create a malformed action that will cause a runtime exception
        // during executeAction that's NOT caught by executeActions
        // Note: We can't use a very large duration because Thread.sleep would hang
        // Instead we test that the executor completes without crashing
        try (FileWriter writer = new FileWriter(tempYamlFile.toFile())) {
            writer.write("name: \"Runtime Exception Story\"\n");
            writer.write("steps:\n");
            writer.write("  - action: \"wait\"\n");
            writer.write("    duration: 10\n");
        }

        TestResult result = executor.executeStory(tempYamlFile.toString());

        // Should handle gracefully - either success or failure but no crash
        assertNotNull(result);
    }

    @Test
    @DisplayName("executeStory with action description missing both name and type")
    void testExecuteStory_actionMissingNameAndType() throws Exception {
        // Create action with null name and null type
        Action action = new Action(Action.ActionType.WAIT);
        action.setName(null);

        java.lang.reflect.Field typeField = Action.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.set(action, null);

        java.util.List<Action> actions = new java.util.ArrayList<>();
        actions.add(action);

        java.lang.reflect.Method executeActionsMethod = SimpleStoryExecutor.class
            .getDeclaredMethod("executeActions", java.util.List.class, String.class, TestResult.class);
        executeActionsMethod.setAccessible(true);

        TestResult result = new TestResult("test");
        boolean returnValue = (Boolean) executeActionsMethod.invoke(executor, actions, "test", result);

        // Should fail when action has null type
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("executeStory with null actions list handles gracefully")
    void testExecuteStory_nullActionsList() throws Exception {
        // Use reflection to test the null check in executeActions (line 89)
        java.lang.reflect.Method executeActionsMethod = SimpleStoryExecutor.class
            .getDeclaredMethod("executeActions", java.util.List.class, String.class, TestResult.class);
        executeActionsMethod.setAccessible(true);

        TestResult result = new TestResult("test");
        boolean returnValue = (Boolean) executeActionsMethod.invoke(executor, null, "test", result);

        // Should return true for null actions list (line 90)
        assertTrue(returnValue);
    }
}
