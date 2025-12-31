package org.cavarest.pilaf.execution;

import org.cavarest.pilaf.config.TestConfiguration;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.parser.YamlStoryParser;
import org.cavarest.pilaf.model.TestStory;
import org.cavarest.pilaf.report.TestReporter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Simple Story Executor
 *
 * Executes YAML test stories by parsing them and running their actions.
 */
public class SimpleStoryExecutor {

    private final TestConfiguration config;
    private final TestReporter reporter;
    private final YamlStoryParser parser;

    public SimpleStoryExecutor(TestConfiguration config, TestReporter reporter) {
        this.config = config;
        this.reporter = reporter;
        this.parser = new YamlStoryParser();
    }

    /**
     * Execute a YAML story from file path
     */
    public TestResult executeStory(String storyFilePath) {
        TestResult result = new TestResult(storyFilePath);

        try {
            // Parse the YAML story
            TestStory story = parseStory(storyFilePath);
            if (story == null) {
                result.setSuccess(false);
                result.setMessage("Failed to parse YAML story: " + storyFilePath);
                return result;
            }

            result.setTestName(story.getName());

            // Execute setup actions
            if (!executeActions(story.getSetupActions(), "setup", result)) {
                return result;
            }

            // Execute test steps
            if (!executeActions(story.getSteps(), "test", result)) {
                return result;
            }

            // Execute cleanup actions
            executeActions(story.getCleanupActions(), "cleanup", result);

            // Mark as passed if we got here without errors
            result.setSuccess(true);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Execution failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse YAML story from file
     */
    private TestStory parseStory(String storyFilePath) {
        try (InputStream is = new FileInputStream(storyFilePath)) {
            return parser.parse(is);
        } catch (Exception e) {
            System.err.println("Failed to parse YAML story: " + storyFilePath);
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Execute a list of actions
     */
    private boolean executeActions(List<Action> actions, String phase, TestResult result) {
        if (actions == null || actions.isEmpty()) {
            return true;
        }

        System.out.println("  Executing " + phase + " actions (" + actions.size() + ")");

        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            String actionDesc = action.getName() != null ? action.getName() :
                (action.getType() != null ? action.getType().toString() : "Unknown");

            System.out.println("    [" + (i + 1) + "/" + actions.size() + "] " + actionDesc);

            try {
                executeAction(action);

                // Add small delay between actions for stability - FIXED: Safe null handling
                Long duration = action.getDuration();
                if (duration != null && duration > 0) {
                    Thread.sleep(duration);
                } else {
                    Thread.sleep(100); // Default small delay
                }

            } catch (Exception e) {
                System.err.println("      Failed: " + e.getMessage());
                result.setSuccess(false);
                result.setMessage(phase + " action failed: " + actionDesc + " - " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    /**
     * Execute a single action
     */
    private void executeAction(Action action) throws Exception {
        if (action.getType() == null) {
            throw new IllegalArgumentException("Action type is required");
        }

        switch (action.getType()) {
            case CONNECT_PLAYER:
                executeConnectPlayer(action);
                break;
            case DISCONNECT_PLAYER:
                executeDisconnectPlayer(action);
                break;
            case EXECUTE_PLAYER_COMMAND:
                executePlayerCommand(action);
                break;
            case EXECUTE_RCON_COMMAND:
                executeRconCommand(action);
                break;
            case GET_ENTITIES:
                executeGetEntities(action);
                break;
            case WAIT:
                executeWait(action);
                break;
            default:
                System.out.println("      (Action type not implemented, skipping)");
                break;
        }
    }

    private void executeConnectPlayer(Action action) throws Exception {
        String player = action.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player name is required for connect_player action");
        }
        System.out.println("      Connecting player: " + player);
        // TODO: Implement actual player connection
    }

    private void executeDisconnectPlayer(Action action) throws Exception {
        String player = action.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player name is required for disconnect_player action");
        }
        System.out.println("      Disconnecting player: " + player);
        // TODO: Implement actual player disconnection
    }

    private void executePlayerCommand(Action action) throws Exception {
        String player = action.getPlayer();
        String command = action.getCommand();
        if (player == null || command == null) {
            throw new IllegalArgumentException("Player and command are required for execute_player_command action");
        }
        System.out.println("      Player " + player + " executes: " + command);
        // TODO: Implement actual player command execution
    }

    private void executeRconCommand(Action action) throws Exception {
        String command = action.getCommand();
        if (command == null) {
            throw new IllegalArgumentException("Command is required for execute_rcon_command action");
        }
        System.out.println("      RCON executes: " + command);
        // TODO: Implement actual RCON command execution
    }

    private void executeGetEntities(Action action) throws Exception {
        String player = action.getPlayer();
        System.out.println("      Getting entities for player: " + player);
        // TODO: Implement actual entity retrieval
    }

    private void executeWait(Action action) throws Exception {
        // FIXED: Safe null handling for duration
        Long duration = action.getDuration();
        if (duration != null && duration > 0) {
            System.out.println("      Waiting: " + duration + "ms");
            Thread.sleep(duration);
        }
    }
}
