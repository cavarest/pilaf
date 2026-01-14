package org.cavarest.pilaf.orchestrator;

import org.cavarest.pilaf.config.ConnectionManager;
import org.cavarest.pilaf.config.TestConfiguration;
import org.cavarest.pilaf.model.*;
import org.cavarest.pilaf.parser.YamlStoryParser;
import org.cavarest.pilaf.report.TestReporter;

import java.util.*;
import java.util.logging.Logger;

/**
 * Executes YAML test stories using the Pilaf framework.
 * Orchestrates the execution of actions and assertions from YAML stories.
 */
public class StoryExecutor {

    private static final Logger logger = Logger.getLogger(StoryExecutor.class.getName());

    private final TestConfiguration config;
    private final ConnectionManager connectionManager;
    private final TestReporter reporter;
    private final YamlStoryParser parser;

    private TestStory currentStory;
    private final Map<String, Object> storyContext = new HashMap<>();

    public StoryExecutor(TestConfiguration config, TestReporter reporter) {
        this.config = config;
        this.reporter = reporter;
        this.connectionManager = new ConnectionManager(config);
        this.parser = new YamlStoryParser();
    }

    /**
     * Execute a story from YAML file.
     */
    public TestResult executeStory(String storyPath) throws Exception {
        logger.info("Loading story from: " + storyPath);

        // Load and parse story
        currentStory = parser.parseFromClasspath(storyPath);
        logger.info("Executing story: " + currentStory.getName());

        TestResult result = new TestResult(currentStory.getName());

        try {
            // Initialize connections
            connectionManager.initialize();

            // Check if services are healthy
            if (!config.isSkipHealthChecks() && !connectionManager.areServicesHealthy()) {
                result.setSuccess(false);
                return result;
            }

            // Create story context for reporting
            TestReporter.TestStory reporterStory = reporter.story(currentStory.getName());
            reporterStory.description(currentStory.getDescription());

            // Execute setup actions
            executeSetupActions(reporterStory);

            // Execute story steps
            executeStorySteps(reporterStory);

            // Execute cleanup actions
            executeCleanupActions(reporterStory);

            result.setSuccess(true);

        } catch (Exception e) {
            logger.severe("Story execution failed: " + e.getMessage());
            result.setSuccess(false);
            result.setError(e);
        } finally {
            // Cleanup connections
            connectionManager.cleanup();
            reporter.complete();
        }

        return result;
    }

    private void executeSetupActions(TestReporter.TestStory reporterStory) throws Exception {
        if (currentStory.getSetupActions() == null) {
            return;
        }

        logger.info("Executing setup actions");

        for (Action action : currentStory.getSetupActions()) {
            executeAction(reporterStory, action, "Setup");
        }
    }

    private void executeStorySteps(TestReporter.TestStory reporterStory) throws Exception {
        if (currentStory.getSteps() == null) {
            return;
        }

        logger.info("Executing story steps");

        for (Action action : currentStory.getSteps()) {
            executeAction(reporterStory, action, "Step");
        }
    }

    private void executeCleanupActions(TestReporter.TestStory reporterStory) {
        if (currentStory.getCleanupActions() == null) {
            return;
        }

        logger.info("Executing cleanup actions");

        try {
            for (Action action : currentStory.getCleanupActions()) {
                executeAction(reporterStory, action, "Cleanup");
            }
        } catch (Exception e) {
            logger.warning("Cleanup action failed: " + e.getMessage());
        }
    }

    private void executeAction(TestReporter.TestStory reporterStory, Action action, String phase) throws Exception {
        String actionName = action.getName() != null ? action.getName() : action.getType().name();

        TestReporter.TestStep step = reporter.step(phase + ": " + actionName)
            .action(action.toString())
            .expected("Action executed successfully");

        try {
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

                case GET_INVENTORY:
                    executeGetInventory(action);
                    break;

                case SPAWN_ENTITY:
                    executeSpawnEntity(action);
                    break;

                case GIVE_ITEM:
                    executeGiveItem(action);
                    break;

                case EQUIP_ITEM:
                    executeEquipItem(action);
                    break;

                case WAIT:
                    executeWait(action);
                    break;

                case CLEAR_COOLDOWN:
                    executeClearCooldown(action);
                    break;

                case SET_COOLDOWN:
                    executeSetCooldown(action);
                    break;

                default:
                    throw new UnsupportedOperationException("Action type not implemented: " + action.getType());
            }

            step.actual("Action completed").pass();

        } catch (Exception e) {
            logger.severe("Action failed: " + e.getMessage());
            step.actual("Action failed: " + e.getMessage()).fail();
            throw e;
        }
    }

    private void executeConnectPlayer(Action action) throws Exception {
        String player = action.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player name required for CONNECT_PLAYER");
        }

        connectionManager.connectPlayer(player);
        storyContext.put("test_player", player);
    }

    private void executeDisconnectPlayer(Action action) throws Exception {
        String player = action.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player name required for DISCONNECT_PLAYER");
        }

        connectionManager.disconnectPlayer(player);
    }

    private void executePlayerCommand(Action action) throws Exception {
        String player = action.getPlayer();
        String command = action.getCommand();

        if (player == null) {
            player = (String) storyContext.get("test_player");
        }

        if (player == null || command == null) {
            throw new IllegalArgumentException("Player and command required for EXECUTE_PLAYER_COMMAND");
        }

        String result = connectionManager.executePlayerCommand(player, command);
        storyContext.put("last_command_result", result);

        logger.info("Player command executed: " + command + " -> " + result);
    }

    private void executeRconCommand(Action action) throws Exception {
        String command = action.getCommand();
        if (command == null) {
            throw new IllegalArgumentException("Command required for EXECUTE_RCON_COMMAND");
        }

        String result = connectionManager.executeRconCommand(command);
        storyContext.put("last_rcon_result", result);

        logger.info("RCON command executed: " + command + " -> " + result);
    }

    private void executeGetEntities(Action action) throws Exception {
        String player = action.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player name required for GET_ENTITIES");
        }

        String entities = connectionManager.getEntities(player);
        storyContext.put("entities", entities);

        logger.info("Entities for " + player + ": " + entities);
    }

    private void executeGetInventory(Action action) throws Exception {
        String player = action.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player name required for GET_INVENTORY");
        }

        String inventory = connectionManager.getInventory(player);
        storyContext.put("inventory", inventory);

        logger.info("Inventory for " + player + ": " + inventory);
    }

    private void executeSpawnEntity(Action action) throws Exception {
        String entityName = action.getName();
        String entityType = action.getEntityType();
        List<Double> location = action.getLocation();

        if (entityName == null || entityType == null) {
            throw new IllegalArgumentException("Entity name and type required for SPAWN_ENTITY");
        }

        String command;
        if (location != null && location.size() >= 3) {
            command = String.format("summon %s %.1f %.1f %.1f {CustomName:'\"%s\"'}",
                entityType.toLowerCase(), location.get(0), location.get(1), location.get(2), entityName);
        } else {
            command = String.format("summon %s ~ ~ ~ {CustomName:'\"%s\"'}", entityType.toLowerCase(), entityName);
        }

        String result = connectionManager.executeRconCommand(command);
        storyContext.put("entity_" + entityName, result);

        logger.info("Entity spawned: " + entityName + " -> " + result);
    }

    private void executeGiveItem(Action action) throws Exception {
        String player = action.getPlayer();
        String item = action.getItem();
        Integer count = action.getCount();

        if (player == null || item == null) {
            throw new IllegalArgumentException("Player and item required for GIVE_ITEM");
        }

        String command = "give " + player + " " + item + " " + (count != null ? count : 1);
        String result = connectionManager.executeRconCommand(command);

        logger.info("Item given: " + command + " -> " + result);
    }

    private void executeEquipItem(Action action) throws Exception {
        String player = action.getPlayer();
        String item = action.getItem();
        String slot = action.getSlot();

        if (player == null || item == null) {
            throw new IllegalArgumentException("Player and item required for EQUIP_ITEM");
        }

        String command;
        if ("offhand".equals(slot)) {
            command = "item replace entity " + player + " weapon.offhand with " + item;
        } else {
            command = "item replace entity " + player + " hotbar.0 with " + item;
        }

        String result = connectionManager.executeRconCommand(command);

        logger.info("Item equipped: " + command + " -> " + result);
    }

    private void executeWait(Action action) throws Exception {
        Long duration = action.getDuration();
        if (duration == null) {
            throw new IllegalArgumentException("Duration required for WAIT");
        }

        Thread.sleep(duration);

        logger.info("Waited for " + duration + "ms");
    }

    private void executeClearCooldown(Action action) throws Exception {
        String player = action.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player name required for CLEAR_COOLDOWN");
        }

        String command = "dragonlightning clearcooldown " + player;
        String result = connectionManager.executeRconCommand(command);

        logger.info("Cooldown cleared: " + command + " -> " + result);
    }

    private void executeSetCooldown(Action action) throws Exception {
        String player = action.getPlayer();
        Long duration = action.getDuration();

        if (player == null || duration == null) {
            throw new IllegalArgumentException("Player and duration required for SET_COOLDOWN");
        }

        String command = "dragonlightning setcooldown " + player + " " + (duration / 1000);
        String result = connectionManager.executeRconCommand(command);

        logger.info("Cooldown set: " + command + " -> " + result);
    }

    /**
     * Get the current story context for advanced usage.
     */
    public Map<String, Object> getStoryContext() {
        return new HashMap<>(storyContext);
    }

    /**
     * Get the connection manager for advanced operations.
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
}
