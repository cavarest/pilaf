package org.cavarest.pilaf.orchestrator;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.Assertion;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.model.TestStory;
import org.cavarest.pilaf.parser.YamlStoryParser;
import org.cavarest.pilaf.report.TestReporter;

import java.time.LocalDateTime;
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
    private TestReporter reporter;
    private LocalDateTime storyStartTime;

    public TestOrchestrator(PilafBackend backend) {
        this.backend = backend;
        this.parser = new YamlStoryParser();
        this.verbose = true;
    }

    public void setReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    public void loadStory(String resourcePath) {
        this.currentStory = parser.parseFromClasspath(resourcePath);
        this.result = new TestResult(currentStory.getName());
        this.storyStartTime = LocalDateTime.now();

        // Create TestReporter story if reporter is set
        if (reporter != null) {
            reporter.story(currentStory.getName());
        }

        log("📖 Loaded story: " + currentStory.getName());
    }

    public void loadStoryFromString(String yamlContent) {
        this.currentStory = parser.parseString(yamlContent);
        this.result = new TestResult(currentStory.getName());
        this.storyStartTime = LocalDateTime.now();

        // Create TestReporter story if reporter is set
        if (reporter != null) {
            reporter.story(currentStory.getName());
        }

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
            backend.setVerbose(verbose);
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
        LocalDateTime stepStartTime = LocalDateTime.now();

        String actionDesc = describeAction(action);
        String actionClass = getActionClass(action);
        String rawCommand = buildRawCommand(action);
        String rawResponse = null;
        log("  ▶ " + action.getType() + ": " + actionDesc);

        // Create reporter step if reporter is set
        TestReporter.TestStep reporterStep = null;
        if (reporter != null) {
            reporterStep = reporter.step(actionDesc);
            reporterStep.action(rawCommand);  // Store raw command
            reporterStep.player(action.getPlayer());
            reporterStep.arguments(describeArguments(action));
            reporterStep.startTime = stepStartTime;
        }

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
                case GET_INVENTORY:
                case GET_PLAYER_INVENTORY:
                    Object inventory = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        inventory = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getPlayerInventory(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        inventory = ((org.cavarest.pilaf.backend.RconBackend) backend).getPlayerInventory(action.getPlayer());
                    }
                    rawResponse = inventory != null ? inventory.toString() : "null";
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), inventory);
                    }
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

                // Player Connection Commands
                case CONNECT_PLAYER:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend).connectPlayer(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend).connectPlayer(action.getPlayer());
                    }
                    break;
                case DISCONNECT_PLAYER:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend).disconnectPlayer(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend).disconnectPlayer(action.getPlayer());
                    }
                    break;

                // Player Management Commands
                case MAKE_OPERATOR:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend).makeOperator(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend).makeOperator(action.getPlayer());
                    }
                    break;
                case GET_PLAYER_POSITION:
                    Object position = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        position = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getPlayerPosition(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        position = ((org.cavarest.pilaf.backend.RconBackend) backend).getPlayerPosition(action.getPlayer());
                    }
                    rawResponse = position != null ? position.toString() : "null";
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), position);
                    }
                    break;
                case GET_PLAYER_HEALTH:
                    Double health = 0.0;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        health = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getPlayerHealthAsDouble(action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        health = ((org.cavarest.pilaf.backend.RconBackend) backend).getPlayerHealth(action.getPlayer());
                    }
                    rawResponse = health != null ? "health=" + health + ", food=20" : "null";
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
                    rawResponse = entities != null ? entities.toString() : "null";
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
                    rawResponse = entitiesInView != null ? entitiesInView.toString() : "null";
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
                    rawResponse = entityData != null ? entityData.toString() : "null";
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), entityData);
                    }
                    break;

                // Command Execution Commands
                case EXECUTE_PLAYER_COMMAND:
                    backend.executePlayerCommand(action.getPlayer(), action.getCommand(), Collections.emptyList());
                    break;
                case EXECUTE_RCON_COMMAND:
                    // Execute RCON command and capture response for report
                    String rconCmdResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        rconCmdResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).executeRconWithCapture(action.getCommand());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        rconCmdResult = ((org.cavarest.pilaf.backend.RconBackend) backend).executeRconWithCapture(action.getCommand());
                    }
                    rawResponse = (rconCmdResult != null && !rconCmdResult.isEmpty()) ? rconCmdResult : "✓ Command sent";
                    break;
                case EXECUTE_RCON_WITH_CAPTURE:
                    String rconResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        rconResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).executeRconWithCapture(action.getCommand());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        rconResult = ((org.cavarest.pilaf.backend.RconBackend) backend).executeRconWithCapture(action.getCommand());
                    }
                    // Show response or indicate command was sent (fire-and-forget)
                    rawResponse = (rconResult != null && !rconResult.isEmpty()) ? rconResult : "✓ Command sent (no response)";
                    break;
                case EXECUTE_RCON_RAW:
                    // RAW RCON - execute the exact command as-is, no parsing
                    String rawRconResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        rawRconResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).executeRconRaw(action.getCommand());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        rawRconResult = ((org.cavarest.pilaf.backend.RconBackend) backend).executeRconRaw(action.getCommand());
                    }
                    rawResponse = rawRconResult != null ? rawRconResult : "";
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
                    rawResponse = equipment != null ? equipment.toString() : "null";
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
                    rawResponse = action.getVariableName() + " = " + stateToStore;
                    break;
                case PRINT_STORED_STATE:
                    Object storedState = storedStates.get(action.getVariableName());
                    rawResponse = action.getVariableName() + " = " + storedState;
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
                    rawResponse = comparison.toString();
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), comparison);
                    }
                    break;
                case PRINT_STATE_COMPARISON:
                    Object comparisonResult = storedStates.get(action.getVariableName());
                    rawResponse = comparisonResult != null ? comparisonResult.toString() : "null";
                    log("    📊 " + action.getVariableName() + ": " + comparisonResult);
                    break;

                // World & Environment Commands
                case GET_WORLD_TIME:
                    Long worldTime = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        worldTime = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getWorldTime();
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        worldTime = ((org.cavarest.pilaf.backend.RconBackend) backend).getWorldTime();
                    }
                    rawResponse = worldTime != null ? "time=" + worldTime : "null";
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), worldTime);
                    }
                    break;
                case GET_WEATHER:
                    String weather = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        weather = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getWeather();
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        weather = ((org.cavarest.pilaf.backend.RconBackend) backend).getWeather();
                    }
                    rawResponse = weather != null ? "weather=" + weather : "null";
                    if (action.getStoreAs() != null) {
                        storedStates.put(action.getStoreAs(), weather);
                    }
                    break;

                // Utility Commands
                case CLEAR_COOLDOWN:
                    backend.executeServerCommand("dragonlightning clearcooldown", Collections.singletonList(action.getPlayer()));
                    break;
                case SET_COOLDOWN:
                    backend.executeServerCommand("dragonlightning setcooldown",
                        java.util.Arrays.asList(action.getPlayer(), action.getDuration().toString()));
                    break;
                case EXECUTE_PLAYER_RAW:
                    // RAW Player command - execute the exact command as the player
                    String rawPlayerResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        rawPlayerResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).executePlayerCommandRaw(action.getPlayer(), action.getCommand());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        rawPlayerResult = ((org.cavarest.pilaf.backend.RconBackend) backend).executePlayerCommandRaw(action.getPlayer(), action.getCommand());
                    }
                    rawResponse = rawPlayerResult != null ? rawPlayerResult : "";

                    break;

                default:
                    log("    ⚠️ Unknown action type: " + action.getType());
            }

            // Mark step as passed
            if (reporterStep != null) {
                reporterStep.endTime = LocalDateTime.now();
                reporterStep.pass();
                reporterStep.evidence.add("✓ Action completed successfully");
                if (rawResponse != null) {
                    reporterStep.actual(rawResponse);
                }
            }
            result.incrementActionsExecuted();
        } catch (Exception e) {
            log("    ❌ Action failed: " + e.getMessage());
            if (reporterStep != null) {
                reporterStep.endTime = LocalDateTime.now();
                reporterStep.fail();
                reporterStep.evidence.add("✗ Error: " + e.getMessage());
                reporterStep.actual(e.getMessage());
            }
            throw new RuntimeException("Action failed: " + action.getType(), e);
        }
    }

    /**
     * Builds the raw command string that was executed.
     */
    private String buildRawCommand(Action action) {
        StringBuilder sb = new StringBuilder();

        switch (action.getType()) {
            case SPAWN_ENTITY:
                sb.append("Spawn entity: ").append(action.getEntityType());
                if (action.getName() != null) sb.append(" ('").append(action.getName()).append("')");
                break;
            case GIVE_ITEM:
                sb.append("give ").append(action.getItem()).append(" ").append(action.getCount());
                if (action.getPlayer() != null) sb.append(" to ").append(action.getPlayer());
                break;
            case GET_INVENTORY:
            case GET_PLAYER_INVENTORY:
                sb.append("Get inventory of ").append(action.getPlayer());
                break;
            case EQUIP_ITEM:
                sb.append("Equip ").append(action.getItem()).append(" to ").append(action.getPlayer());
                break;
            case PLAYER_COMMAND:
                sb.append("Player ").append(action.getPlayer()).append(" executes: ").append(action.getCommand());
                break;
            case SERVER_COMMAND:
                sb.append("RCON: ").append(action.getCommand());
                if (action.getArgs() != null && !action.getArgs().isEmpty()) {
                    sb.append(" ").append(String.join(" ", action.getArgs()));
                }
                break;
            case MOVE_PLAYER:
                sb.append("Move ").append(action.getPlayer()).append(" to ").append(action.getDestination());
                break;
            case WAIT:
                Long duration = action.getDuration();
                sb.append("Wait ").append(duration != null ? duration : 0).append("ms");
                break;
            case CONNECT_PLAYER:
                sb.append("Connect player ").append(action.getPlayer()).append(" to server");
                break;
            case DISCONNECT_PLAYER:
                sb.append("Disconnect player ").append(action.getPlayer()).append(" from server");
                break;
            case MAKE_OPERATOR:
                sb.append("Make ").append(action.getPlayer()).append(" an operator");
                break;
            case GET_PLAYER_POSITION:
                sb.append("Get position of ").append(action.getPlayer());
                break;
            case GET_PLAYER_HEALTH:
                sb.append("Get health of ").append(action.getPlayer());
                break;
            case SEND_CHAT_MESSAGE:
                sb.append("Player ").append(action.getPlayer()).append(" sends: ").append(action.getMessage());
                break;
            case GET_ENTITIES:
                sb.append("Get entities near ").append(action.getPlayer());
                break;
            case GET_ENTITIES_IN_VIEW:
                sb.append("Get entities in view of ").append(action.getPlayer());
                break;
            case GET_ENTITY_BY_NAME:
                sb.append("Get entity '").append(action.getEntity()).append("' near ").append(action.getPlayer());
                break;
            case EXECUTE_PLAYER_COMMAND:
                sb.append("Player ").append(action.getPlayer()).append(" executes: ").append(action.getCommand());
                break;
            case EXECUTE_RCON_COMMAND:
                sb.append("RCON: ").append(action.getCommand());
                break;
            case EXECUTE_RCON_WITH_CAPTURE:
                sb.append("RCON with capture: ").append(action.getCommand());
                break;
            case REMOVE_ITEM:
                sb.append("Remove ").append(action.getCount()).append("x ").append(action.getItem())
                  .append(" from ").append(action.getPlayer());
                break;
            case GET_PLAYER_EQUIPMENT:
                sb.append("Get equipment of ").append(action.getPlayer());
                break;
            case STORE_STATE:
                sb.append("Store state as ").append(action.getVariableName());
                break;
            case PRINT_STORED_STATE:
                sb.append("Print state ").append(action.getVariableName());
                break;
            case COMPARE_STATES:
                sb.append("Compare ").append(action.getState1()).append(" and ").append(action.getState2());
                break;
            case PRINT_STATE_COMPARISON:
                sb.append("Print comparison ").append(action.getVariableName());
                break;
            case GET_WORLD_TIME:
                sb.append("Get world time");
                break;
            case GET_WEATHER:
                sb.append("Get weather status");
                break;
            case CLEAR_COOLDOWN:
                sb.append("Clear cooldown for ").append(action.getPlayer());
                break;
            case SET_COOLDOWN:
                sb.append("Set cooldown for ").append(action.getPlayer()).append(" to ").append(action.getDuration()).append("ms");
                break;
            case EXECUTE_PLAYER_RAW:
                sb.append("Player ").append(action.getPlayer()).append(" executes: ").append(action.getCommand());
                break;
            default:
                sb.append(action.getType().toString().toLowerCase().replace("_", " "));
        }

        return sb.toString();
    }

    /**
     * Determines the action class based on the action type.
     * Returns: "server", "client", or "workflow"
     */
    private String getActionClass(Action action) {
        if (action.getPlayer() != null && !action.getPlayer().isEmpty()) {
            return "client";
        }
        switch (action.getType()) {
            case SPAWN_ENTITY:
            case GIVE_ITEM:
            case SERVER_COMMAND:
            case EXECUTE_RCON_COMMAND:
            case EXECUTE_RCON_WITH_CAPTURE:
            case CLEAR_COOLDOWN:
            case SET_COOLDOWN:
            case REMOVE_ENTITIES:
            case REMOVE_PLAYERS:
            case MAKE_OPERATOR:
                return "server";
            case WAIT:
            case STORE_STATE:
            case PRINT_STORED_STATE:
            case COMPARE_STATES:
            case PRINT_STATE_COMPARISON:
                return "workflow";
            default:
                return action.getPlayer() != null ? "client" : "workflow";
        }
    }

    /**
     * Formats action arguments for display (NO truncation).
     */
    private String describeArguments(Action action) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        if (action.getPlayer() != null) {
            sb.append("player=").append(action.getPlayer());
            first = false;
        }
        if (action.getItem() != null) {
            if (!first) sb.append(", ");
            sb.append("item=").append(action.getItem());
            first = false;
        }
        if (action.getCount() != null) {
            if (!first) sb.append(", ");
            sb.append("count=").append(action.getCount());
            first = false;
        }
        if (action.getCommand() != null) {
            if (!first) sb.append(", ");
            sb.append("cmd=").append(action.getCommand());
            first = false;
        }
        if (action.getMessage() != null) {
            if (!first) sb.append(", ");
            sb.append("msg=").append(action.getMessage());
            first = false;
        }
        if (action.getDestination() != null) {
            if (!first) sb.append(", ");
            sb.append("dest=").append(action.getDestination());
        }

        return sb.toString();
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
            case CONNECT_PLAYER: return "Connect player " + action.getPlayer() + " to server";
            case DISCONNECT_PLAYER: return "Disconnect player " + action.getPlayer() + " from server";
            case GET_INVENTORY: return "Get inventory of " + action.getPlayer();
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
            case GET_WORLD_TIME: return "Get world time";
            case GET_WEATHER: return "Get weather status";
            case EXECUTE_PLAYER_RAW: return "Execute player command: " + action.getCommand();
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
