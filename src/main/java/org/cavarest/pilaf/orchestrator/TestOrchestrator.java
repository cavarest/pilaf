package org.cavarest.pilaf.orchestrator;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.Assertion;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.model.TestStory;
import org.cavarest.pilaf.parser.YamlStoryParser;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Orchestrates the execution of PILAF test stories.
 * Coordinates actions between the backend and validates assertions.
 */
public class TestOrchestrator {

    private final PilafBackend backend;
    private final YamlStoryParser parser;
    private TestStory currentStory;
    private TestResult result;
    private boolean verbose;
    private Map<String, Object> storedStates = new HashMap<>();

    public TestOrchestrator(PilafBackend backend) {
        this.backend = backend;
        this.parser = new YamlStoryParser();
        this.verbose = true;
    }

    public void loadStory(String resourcePath) {
        this.currentStory = parser.parseFromClasspath(resourcePath);
        this.result = new TestResult(currentStory.getName());
        log("📖 Loaded story: " + currentStory.getName());
    }

    public void loadStoryFromString(String yamlContent) {
        this.currentStory = parser.parseString(yamlContent);
        this.result = new TestResult(currentStory.getName());
        log("📖 Loaded story: " + currentStory.getName());
    }

    public TestResult execute() {
        if (currentStory == null) {
            throw new IllegalStateException("No story loaded. Call loadStory() first.");
        }

        long startTime = System.currentTimeMillis();
        result.setStoryName(currentStory.getName());

        try {
            log("🔧 Initializing backend: " + backend.getType());
            backend.initialize();

            log("\n📋 Executing setup actions...");
            for (Action action : currentStory.getSetup()) {
                executeAction(action);
            }

            log("\n🎬 Executing test steps...");
            for (Action action : currentStory.getSteps()) {
                executeAction(action);
            }

            log("\n✓ Evaluating assertions...");
            for (Assertion assertion : currentStory.getAssertions()) {
                evaluateAssertion(assertion);
            }

            log("\n🧹 Executing cleanup actions...");
            for (Action action : currentStory.getCleanup()) {
                executeAction(action);
            }

            result.setSuccess(result.getAssertionsFailed() == 0);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e);
            log("❌ Error during execution: " + e.getMessage());
        } finally {
            try {
                backend.cleanup();
            } catch (Exception e) {
                log("⚠️ Error during cleanup: " + e.getMessage());
            }
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        logSummary();
        return result;
    }

    private void executeAction(Action action) {
        log("  ▶ " + action.getType() + ": " + describeAction(action));

        try {
            switch (action.getType()) {
                // Existing actions
                case SPAWN_ENTITY:
                    backend.spawnEntity(action.getName(), action.getEntityType(),
                        action.getLocation(), action.getEquipment());
                    break;
                case GIVE_ITEM:
                    backend.giveItem(action.getPlayer(), action.getItem(), action.getCount());
                    break;
                case EQUIP_ITEM:
                    backend.equipItem(action.getPlayer(), action.getItem(), action.getSlot());
                    break;
                case PLAYER_COMMAND:
                    backend.executePlayerCommand(action.getPlayer(), action.getCommand(),
                        action.getArgs() != null ? action.getArgs() : Collections.emptyList());
                    break;
                case SERVER_COMMAND:
                    backend.executeServerCommand(action.getCommand(),
                        action.getArgs() != null ? action.getArgs() : Collections.emptyList());
                    break;
                case MOVE_PLAYER:
                    backend.movePlayer(action.getPlayer(), "destination", action.getDestination());
                    break;
                case WAIT:
                    // Safe null handling for duration
                    Long waitDuration = action.getDuration();
                    if (waitDuration != null && waitDuration > 0) {
                        log("    ⏳ Waiting " + waitDuration + "ms...");
                        Thread.sleep(waitDuration);
                    } else {
                        log("    ⏳ Waiting 1000ms...");
                        Thread.sleep(1000);
                    }
                    break;
                case REMOVE_ENTITIES:
                    backend.removeAllTestEntities();
                    break;
                case REMOVE_PLAYERS:
                    backend.removeAllTestPlayers();
                    break;

                // Player Management Commands
                case MAKE_OPERATOR:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend).makeOperator(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend).makeOperator(action.getPlayer());
                    }
                    break;
                case GET_PLAYER_INVENTORY:
                    Object inventory = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        inventory = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getPlayerInventory(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        inventory = ((org.cavarest.pilaf.backend.RconBackend) backend).getPlayerInventory(action.getPlayer());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), inventory);
                    }
                    break;
                case GET_PLAYER_POSITION:
                    Object position = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        position = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getPlayerPosition(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        position = ((org.cavarest.pilaf.backend.RconBackend) backend).getPlayerPosition(action.getPlayer());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), position);
                    }
                    break;
                case GET_PLAYER_HEALTH:
                    Double health = 0.0;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        health = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getPlayerHealth(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        health = ((org.cavarest.pilaf.backend.RconBackend) backend).getPlayerHealth(action.getPlayer());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), health);
                    }
                    break;
                case SEND_CHAT_MESSAGE:
                    backend.sendChat(action.getPlayer(), action.getMessage());
                    break;

                // Entity Management Commands
                case GET_ENTITIES:
                    Object entities = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        entities = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getEntities(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        entities = ((org.cavarest.pilaf.backend.RconBackend) backend).getEntitiesInView(action.getPlayer());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), entities);
                    }
                    break;
                case GET_ENTITIES_IN_VIEW:
                    Object entitiesInView = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        entitiesInView = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getEntitiesInView(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        entitiesInView = ((org.cavarest.pilaf.backend.RconBackend) backend).getEntitiesInView(action.getPlayer());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), entitiesInView);
                    }
                    break;
                case GET_ENTITY_BY_NAME:
                    Object entityData = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        entityData = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getEntityByName(action.getEntity(), action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        entityData = ((org.cavarest.pilaf.backend.RconBackend) backend).getEntityByName(action.getEntity(), action.getPlayer());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), entityData);
                    }
                    break;

                // Command Execution Commands
                case EXECUTE_PLAYER_COMMAND:
                    backend.executePlayerCommand(action.getPlayer(), action.getCommand(), Collections.emptyList());
                    break;
                case EXECUTE_RCON_COMMAND:
                    backend.executeServerCommand(action.getCommand(), Collections.emptyList());
                    break;
                case EXECUTE_RCON_WITH_CAPTURE:
                    String result = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        result = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).executeRconWithCapture(action.getCommand());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        result = ((org.cavarest.pilaf.backend.RconBackend) backend).executeRconWithCapture(action.getCommand());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), result);
                    }
                    break;

                // Inventory Management Commands
                case REMOVE_ITEM:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend).removeItem(action.getPlayer(), action.getItem(), action.getCount());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend).removeItem(action.getPlayer(), action.getItem(), action.getCount());
                    }
                    break;
                case GET_PLAYER_EQUIPMENT:
                    Object equipment = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        equipment = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getPlayerEquipment(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        equipment = ((org.cavarest.pilaf.backend.RconBackend) backend).getPlayerEquipment(action.getPlayer());
                    }
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), equipment);
                    }
                    break;

                // State Management Commands
                case STORE_STATE:
                    Object stateToStore = null;
                    if (action.getFromCommandResult() != null) {
                        stateToStore = storedStates.get(action.getFromCommandResult());
                    } else {
                        stateToStore = action.getVariableName();
                    }
                    storedStates.put(action.getVariableName(), stateToStore);
                    break;
                case PRINT_STORED_STATE:
                    Object storedState = storedStates.get(action.getVariableName());
                    log("    📄 " + action.getVariableName() + ": " + storedState);
                    break;
                case COMPARE_STATES:
                    Object state1 = storedStates.get(action.getState1());
                    Object state2 = storedStates.get(action.getState2());
                    Map<String, Object> comparison = new HashMap<>();
                    comparison.put("state1_name", action.getState1());
                    comparison.put("state2_name", action.getState2());
                    comparison.put("state1", state1);
                    comparison.put("state2", state2);
                    comparison.put("equal", state1 != null && state1.equals(state2));
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), comparison);
                    }
                    break;
                case PRINT_STATE_COMPARISON:
                    Object comparisonResult = storedStates.get(action.getVariableName());
                    log("    📊 " + action.getVariableName() + ": " + comparisonResult);
                    break;

                // Utility Commands
                case CLEAR_COOLDOWN:
                    backend.executeServerCommand("dragonlightning clearcooldown", Collections.singletonList(action.getPlayer()));
                    break;
                case SET_COOLDOWN:
                    backend.executeServerCommand("dragonlightning setcooldown",
                        java.util.Arrays.asList(action.getPlayer(), action.getDuration().toString()));
                    break;

                default:
                    log("    ⚠️ Unknown action type: " + action.getType());
            }
            result.incrementActionsExecuted();
        } catch (Exception e) {
            log("    ❌ Action failed: " + e.getMessage());
            throw new RuntimeException("Action failed: " + action.getType(), e);
        }
    }

    private void evaluateAssertion(Assertion assertion) {
        boolean passed = false;
        String message = "";

        try {
            passed = assertion.evaluate(backend);
            message = describeAssertion(assertion);
        } catch (Exception e) {
            message = "Error: " + e.getMessage();
        }

        TestResult.AssertionResult assertionResult = new TestResult.AssertionResult(assertion, passed);
        assertionResult.setMessage(message);
        result.addAssertionResult(assertionResult);

        String status = passed ? "✓" : "✗";
        log("  " + status + " " + assertion.getType() + ": " + message);
    }

    public TestResult getResult() { return result; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    private String describeAction(Action action) {
        switch (action.getType()) {
            case SPAWN_ENTITY: return "Spawn " + action.getEntityType() + " '" + action.getName() + "'";
            case GIVE_ITEM: return "Give " + action.getCount() + "x " + action.getItem() + " to " + action.getPlayer();
            case EQUIP_ITEM: return "Equip " + action.getItem() + " to " + action.getPlayer() + "'s " + action.getSlot();
            case PLAYER_COMMAND: return action.getPlayer() + " executes: " + action.getCommand();
            case WAIT:
                Long duration = action.getDuration();
                return "Wait " + (duration != null ? duration : 0) + "ms";
            case MAKE_OPERATOR: return "Make " + action.getPlayer() + " an operator";
            case GET_PLAYER_INVENTORY: return "Get inventory of " + action.getPlayer();
            case GET_PLAYER_POSITION: return "Get position of " + action.getPlayer();
            case GET_PLAYER_HEALTH: return "Get health of " + action.getPlayer();
            case SEND_CHAT_MESSAGE: return action.getPlayer() + " sends: " + action.getMessage();
            case GET_ENTITIES: return "Get entities near " + action.getPlayer();
            case GET_ENTITIES_IN_VIEW: return "Get entities in view of " + action.getPlayer();
            case GET_ENTITY_BY_NAME: return "Get entity '" + action.getEntity() + "' near " + action.getPlayer();
            case EXECUTE_RCON_WITH_CAPTURE: return "Execute RCON: " + action.getCommand();
            case REMOVE_ITEM: return "Remove " + action.getCount() + "x " + action.getItem() + " from " + action.getPlayer();
            case GET_PLAYER_EQUIPMENT: return "Get equipment of " + action.getPlayer();
            case STORE_STATE: return "Store state as " + action.getVariableName();
            case PRINT_STORED_STATE: return "Print state " + action.getVariableName();
            case COMPARE_STATES: return "Compare " + action.getState1() + " and " + action.getState2();
            case PRINT_STATE_COMPARISON: return "Print comparison " + action.getVariableName();
            case CLEAR_COOLDOWN: return "Clear cooldown for " + action.getPlayer();
            case SET_COOLDOWN: return "Set cooldown for " + action.getPlayer() + " to " + action.getDuration() + "ms";
            default: return action.toString();
        }
    }

    private String describeAssertion(Assertion assertion) {
        switch (assertion.getType()) {
            case ENTITY_HEALTH:
                double health = backend.getEntityHealth(assertion.getEntity());
                return assertion.getEntity() + " health " + assertion.getCondition() + " " + assertion.getValue() + " (actual: " + health + ")";
            case ENTITY_EXISTS:
                boolean exists = backend.entityExists(assertion.getEntity());
                return assertion.getEntity() + " exists=" + assertion.getExpected() + " (actual: " + exists + ")";
            default: return assertion.toString();
        }
    }

    private void logSummary() {
        log("\n" + "=".repeat(50));
        log("📊 Test Summary: " + currentStory.getName());
        log("=".repeat(50));
        log("  Status: " + (result.isSuccess() ? "✅ PASSED" : "❌ FAILED"));
        log("  Actions: " + result.getActionsExecuted() + ", Passed: " + result.getAssertionsPassed() + ", Failed: " + result.getAssertionsFailed());
        log("  Time: " + result.getExecutionTimeMs() + "ms");
        log("=".repeat(50));
    }

    private void log(String message) {
        if (verbose) System.out.println(message);
        result.addLog(message);
    }
}
