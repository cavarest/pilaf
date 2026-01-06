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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

                // =============================================
                // PLAYER MANAGEMENT ACTIONS (via RCON)
                // =============================================

                // Change player gamemode
                case GAMEMODE_CHANGE:
                    String gamemode = action.getEntity() != null ? action.getEntity() : "survival";
                    String gamemodeResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        gamemodeResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture("gamemode " + gamemode + " " + action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        gamemodeResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture("gamemode " + gamemode + " " + action.getPlayer());
                    }
                    rawResponse = gamemodeResult != null ? gamemodeResult : "✓ Gamemode changed";
                    break;

                // Clear player inventory
                case CLEAR_INVENTORY:
                    String clearResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        clearResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture("clear " + action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        clearResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture("clear " + action.getPlayer());
                    }
                    rawResponse = clearResult != null ? clearResult : "✓ Inventory cleared";
                    break;

                // Set player spawn point
                case SET_SPAWN_POINT:
                    String spawnCmd = "spawnpoint " + action.getPlayer();
                    if (action.getLocation() != null && action.getLocation().size() >= 3) {
                        spawnCmd += " " + action.getLocation().get(0).intValue()
                                   + " " + action.getLocation().get(1).intValue()
                                   + " " + action.getLocation().get(2).intValue();
                    }
                    String spawnResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        spawnResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture(spawnCmd);
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        spawnResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture(spawnCmd);
                    }
                    rawResponse = spawnResult != null ? spawnResult : "✓ Spawn point set";
                    break;

                // Teleport player
                case TELEPORT_PLAYER:
                    String tpCmd = "tp " + action.getPlayer();
                    if (action.getDestination() != null) {
                        String[] coords = action.getDestination().split("\\s+");
                        if (coords.length >= 3) {
                            tpCmd += " " + coords[0] + " " + coords[1] + " " + coords[2];
                        }
                    } else if (action.getLocation() != null && action.getLocation().size() >= 3) {
                        tpCmd += " " + action.getLocation().get(0).intValue()
                               + " " + action.getLocation().get(1).intValue()
                               + " " + action.getLocation().get(2).intValue();
                    }
                    String tpResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        tpResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture(tpCmd);
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        tpResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture(tpCmd);
                    }
                    rawResponse = tpResult != null ? tpResult : "✓ Player teleported";
                    break;

                // Set player health
                case SET_PLAYER_HEALTH:
                    String healthCmd = "attribute minecraft:generic.max_health base set " + action.getValue();
                    String healthResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        healthResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture(healthCmd);
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        healthResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture(healthCmd);
                    }
                    // Also heal the player to apply the new max health
                    String healResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        healResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture("heal " + action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        healResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture("heal " + action.getPlayer());
                    }
                    rawResponse = "✓ Player health set to " + action.getValue();
                    break;

                // Kill player
                case KILL_PLAYER:
                    String killResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        killResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture("kill " + action.getPlayer());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        killResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture("kill " + action.getPlayer());
                    }
                    rawResponse = killResult != null ? killResult : "✓ Player killed";
                    break;

                // =============================================
                // ENTITY MANAGEMENT ACTIONS (via RCON)
                // =============================================

                // Kill entity
                case KILL_ENTITY:
                    String killEntityCmd = "kill " + (action.getEntity() != null ? action.getEntity() : "@e[type=" + action.getEntityType() + "]");
                    String killEntityResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        killEntityResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture(killEntityCmd);
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        killEntityResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture(killEntityCmd);
                    }
                    rawResponse = killEntityResult != null ? killEntityResult : "✓ Entity killed";
                    break;

                // Set entity health
                case SET_ENTITY_HEALTH:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .setEntityHealth(action.getEntity(), action.getValue());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .setEntityHealth(action.getEntity(), action.getValue());
                    }
                    rawResponse = "✓ Entity health set to " + action.getValue();
                    break;

                // =============================================
                // WORLD MANAGEMENT ACTIONS (via RCON)
                // =============================================

                // Set world time
                case SET_TIME:
                    String timeCmd = "time set " + (action.getValue() != null ? action.getValue().longValue() : 1000);
                    String timeResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        timeResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture(timeCmd);
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        timeResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture(timeCmd);
                    }
                    rawResponse = timeResult != null ? timeResult : "✓ Time set";
                    break;

                // Set weather
                case SET_WEATHER:
                    String weatherCmd = "weather " + (action.getEntity() != null ? action.getEntity() : "clear")
                                       + " " + (action.getDuration() != null ? action.getDuration() / 1000 : 600);
                    String weatherResult = null;
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        weatherResult = ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executeRconWithCapture(weatherCmd);
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        weatherResult = ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executeRconWithCapture(weatherCmd);
                    }
                    rawResponse = weatherResult != null ? weatherResult : "✓ Weather set";
                    break;

                // =============================================
                // CLIENT ACTIONS (Mineflayer)
                // =============================================

                // Look at target
                case LOOK_AT:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        // Use player command to look at target
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executePlayerCommand(action.getPlayer(), "look " + action.getEntity(), Collections.emptyList());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executePlayerCommand(action.getPlayer(), "look " + action.getEntity(), Collections.emptyList());
                    }
                    rawResponse = "✓ Player looking at " + action.getEntity();
                    break;

                // Jump
                case JUMP:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executePlayerCommand(action.getPlayer(), "jump", Collections.emptyList());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executePlayerCommand(action.getPlayer(), "jump", Collections.emptyList());
                    }
                    rawResponse = "✓ Player jumped";
                    break;

                // Use item
                case USE_ITEM:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .useItem(action.getPlayer(), action.getItem(), action.getEntity());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .useItem(action.getPlayer(), action.getItem(), action.getEntity());
                    }
                    rawResponse = "✓ Item used";
                    break;

                // Attack entity
                case ATTACK_ENTITY:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executePlayerCommand(action.getPlayer(), "attack " + action.getEntity(), Collections.emptyList());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executePlayerCommand(action.getPlayer(), "attack " + action.getEntity(), Collections.emptyList());
                    }
                    rawResponse = "✓ Attacked " + action.getEntity();
                    break;

                // Break block
                case BREAK_BLOCK:
                    String breakCmd = "break " + action.getPosition();
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executePlayerCommand(action.getPlayer(), breakCmd, Collections.emptyList());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executePlayerCommand(action.getPlayer(), breakCmd, Collections.emptyList());
                    }
                    rawResponse = "✓ Block broken at " + action.getPosition();
                    break;

                // Place block
                case PLACE_BLOCK:
                    String placeCmd = "place " + action.getItem() + " " + action.getPosition();
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        ((org.cavarest.pilaf.backend.MineflayerBackend) backend)
                            .executePlayerCommand(action.getPlayer(), placeCmd, Collections.emptyList());
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        ((org.cavarest.pilaf.backend.RconBackend) backend)
                            .executePlayerCommand(action.getPlayer(), placeCmd, Collections.emptyList());
                    }
                    rawResponse = "✓ Block placed at " + action.getPosition();
                    break;

                // Get chat history
                case GET_CHAT_HISTORY:
                    if (backend instanceof org.cavarest.pilaf.backend.MineflayerBackend) {
                        Object chatHistory = ((org.cavarest.pilaf.backend.MineflayerBackend) backend).getChatHistory(action.getPlayer());
                        rawResponse = chatHistory != null ? chatHistory.toString() : "[]";
                        if (action.getStoreAs() != null) {
                            storedStates.put(action.getStoreAs(), chatHistory);
                        }
                    } else if (backend instanceof org.cavarest.pilaf.backend.RconBackend) {
                        rawResponse = "Chat history not available via RCON";
                    }
                    break;

                // =============================================
                // STATE MANAGEMENT ACTIONS
                // =============================================

                // Extract with JSONPath
                case EXTRACT_WITH_JSONPATH:
                    Object sourceState = storedStates.get(action.getSourceVariable());
                    String extracted = extractWithJsonPath(sourceState, action.getJsonPath());
                    storedStates.put(action.getVariableName(), extracted);
                    rawResponse = action.getVariableName() + " = " + extracted;
                    break;

                // Filter entities
                case FILTER_ENTITIES:
                    Object entitiesToFilter = storedStates.get(action.getSourceVariable());
                    Object filtered = filterEntities(entitiesToFilter, action.getFilterType(), action.getFilterValue());
                    storedStates.put(action.getVariableName(), filtered);
                    rawResponse = action.getVariableName() + " = " + filtered;
                    break;

                // =============================================
                // ASSERTION ACTIONS (for assertion stories)
                // =============================================

                case ASSERT_ENTITY_MISSING:
                    // Just log - actual assertion is in evaluateAssertion
                    rawResponse = "Check " + action.getEntity() + " doesn't exist";
                    break;

                case ASSERT_ENTITY_EXISTS:
                    rawResponse = "Check " + action.getEntity() + " exists";
                    break;

                case ASSERT_PLAYER_HAS_ITEM:
                    rawResponse = "Check " + action.getPlayer() + " has " + action.getItem();
                    break;

                case ASSERT_RESPONSE_CONTAINS:
                    rawResponse = "Check response contains '" + action.getContains() + "'";
                    break;

                case ASSERT_JSON_EQUALS:
                    rawResponse = "Check JSON equals";
                    break;

                case ASSERT_LOG_CONTAINS:
                    rawResponse = "Check log contains";
                    break;

                case ASSERT_CONDITION:
                    rawResponse = "Check condition met";
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

            // Player Management Commands
            case GAMEMODE_CHANGE:
                sb.append("Set gamemode ").append(action.getEntity()).append(" for ").append(action.getPlayer());
                break;
            case CLEAR_INVENTORY:
                sb.append("Clear inventory of ").append(action.getPlayer());
                break;
            case SET_SPAWN_POINT:
                sb.append("Set spawn point for ").append(action.getPlayer());
                break;
            case TELEPORT_PLAYER:
                sb.append("Teleport ").append(action.getPlayer()).append(" to ").append(action.getDestination());
                break;
            case SET_PLAYER_HEALTH:
                sb.append("Set health of ").append(action.getPlayer()).append(" to ").append(action.getValue());
                break;
            case KILL_PLAYER:
                sb.append("Kill ").append(action.getPlayer());
                break;

            // Entity Management Commands
            case KILL_ENTITY:
                sb.append("Kill entity ").append(action.getEntity());
                break;
            case SET_ENTITY_HEALTH:
                sb.append("Set health of ").append(action.getEntity()).append(" to ").append(action.getValue());
                break;

            // World Management Commands
            case SET_TIME:
                sb.append("Set world time to ").append(action.getValue());
                break;
            case SET_WEATHER:
                sb.append("Set weather to ").append(action.getEntity()).append(" for ").append(action.getDuration()).append("ms");
                break;

            // Client Actions
            case LOOK_AT:
                sb.append("Player ").append(action.getPlayer()).append(" looks at ").append(action.getEntity());
                break;
            case JUMP:
                sb.append("Player ").append(action.getPlayer()).append(" jumps");
                break;
            case USE_ITEM:
                sb.append("Player ").append(action.getPlayer()).append(" uses ").append(action.getItem());
                break;
            case ATTACK_ENTITY:
                sb.append("Player ").append(action.getPlayer()).append(" attacks ").append(action.getEntity());
                break;
            case BREAK_BLOCK:
                sb.append("Player ").append(action.getPlayer()).append(" breaks block at ").append(action.getPosition());
                break;
            case PLACE_BLOCK:
                sb.append("Player ").append(action.getPlayer()).append(" places ").append(action.getItem()).append(" at ").append(action.getPosition());
                break;
            case GET_CHAT_HISTORY:
                sb.append("Get chat history for ").append(action.getPlayer());
                break;

            // State Management Commands
            case EXTRACT_WITH_JSONPATH:
                sb.append("Extract ").append(action.getJsonPath()).append(" from ").append(action.getSourceVariable()).append(" as ").append(action.getVariableName());
                break;
            case FILTER_ENTITIES:
                sb.append("Filter entities by ").append(action.getFilterType()).append("=").append(action.getFilterValue()).append(" as ").append(action.getVariableName());
                break;

            // Assertion Actions
            case ASSERT_ENTITY_MISSING:
                sb.append("Assert ").append(action.getEntity()).append(" doesn't exist");
                break;
            case ASSERT_ENTITY_EXISTS:
                sb.append("Assert ").append(action.getEntity()).append(" exists");
                break;
            case ASSERT_PLAYER_HAS_ITEM:
                sb.append("Assert ").append(action.getPlayer()).append(" has ").append(action.getItem());
                break;
            case ASSERT_RESPONSE_CONTAINS:
                sb.append("Assert response contains ").append(action.getContains());
                break;
            case ASSERT_JSON_EQUALS:
                sb.append("Assert JSON equals");
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

    @SuppressWarnings("unchecked")
    private void evaluateAssertion(Assertion assertion) {
        boolean passed = false;
        String message = "";
        String details = "";

        try {
            Object evalResult = assertion.evaluate(backend);
            if (evalResult instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) evalResult;
                passed = Boolean.TRUE.equals(resultMap.get("passed"));
                message = (String) resultMap.get("message");
                Object detailsObj = resultMap.get("details");
                details = detailsObj != null ? detailsObj.toString() : "";
            } else {
                passed = (Boolean) evalResult;
                message = describeAssertion(assertion);
            }
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

            // Player Management
            case GAMEMODE_CHANGE: return "Set gamemode " + action.getEntity() + " for " + action.getPlayer();
            case CLEAR_INVENTORY: return "Clear inventory of " + action.getPlayer();
            case SET_SPAWN_POINT: return "Set spawn point for " + action.getPlayer();
            case TELEPORT_PLAYER: return "Teleport " + action.getPlayer() + " to " + action.getDestination();
            case SET_PLAYER_HEALTH: return "Set health of " + action.getPlayer() + " to " + action.getValue();
            case KILL_PLAYER: return "Kill " + action.getPlayer();

            // Entity Management
            case KILL_ENTITY: return "Kill entity " + action.getEntity();
            case SET_ENTITY_HEALTH: return "Set health of " + action.getEntity() + " to " + action.getValue();

            // World Management
            case SET_TIME: return "Set world time to " + action.getValue();
            case SET_WEATHER: return "Set weather to " + action.getEntity();

            // Client Actions
            case LOOK_AT: return action.getPlayer() + " looks at " + action.getEntity();
            case JUMP: return action.getPlayer() + " jumps";
            case USE_ITEM: return action.getPlayer() + " uses " + action.getItem();
            case ATTACK_ENTITY: return action.getPlayer() + " attacks " + action.getEntity();
            case BREAK_BLOCK: return action.getPlayer() + " breaks block at " + action.getPosition();
            case PLACE_BLOCK: return action.getPlayer() + " places " + action.getItem() + " at " + action.getPosition();
            case GET_CHAT_HISTORY: return "Get chat history for " + action.getPlayer();

            // State Management
            case EXTRACT_WITH_JSONPATH: return "Extract " + action.getJsonPath() + " from " + action.getSourceVariable();
            case FILTER_ENTITIES: return "Filter entities by " + action.getFilterType() + "=" + action.getFilterValue();

            // Assertion Actions
            case ASSERT_ENTITY_MISSING: return "Assert " + action.getEntity() + " doesn't exist";
            case ASSERT_ENTITY_EXISTS: return "Assert " + action.getEntity() + " exists";
            case ASSERT_PLAYER_HAS_ITEM: return "Assert " + action.getPlayer() + " has " + action.getItem();
            case ASSERT_RESPONSE_CONTAINS: return "Assert response contains '" + action.getContains() + "'";
            case ASSERT_JSON_EQUALS: return "Assert JSON equals";

            default: return action.toString();
        }
    }

    private String describeAssertion(Assertion assertion) {
        switch (assertion.getType()) {
            case ENTITY_HEALTH:
                double health = backend.getEntityHealth(assertion.getEntity());
                return assertion.getEntity() + " health " + assertion.getConditionType() + " " + assertion.getValue() + " (actual: " + health + ")";
            case ENTITY_EXISTS:
                boolean exists = backend.entityExists(assertion.getEntity());
                return assertion.getEntity() + " exists=" + assertion.getExpected() + " (actual: " + exists + ")";
            case PLAYER_INVENTORY:
            case ASSERT_PLAYER_HAS_ITEM:
                boolean hasItem = backend.playerInventoryContains(assertion.getPlayer(), assertion.getItem(), assertion.getSlot());
                return "Player " + assertion.getPlayer() + " has item " + assertion.getItem() + ": " + hasItem;
            case PLUGIN_COMMAND:
                return "Plugin " + assertion.getPlugin() + " command " + assertion.getCommand();
            case ASSERT_ENTITY_MISSING:
                boolean isMissing = !backend.entityExists(assertion.getEntity());
                return "Entity " + assertion.getEntity() + " missing: " + isMissing;
            case ASSERT_RESPONSE_CONTAINS:
                return "Response contains '" + assertion.getContains() + "': " + (assertion.getSource() != null ? "pending" : "no source");
            case ASSERT_JSON_EQUALS:
                return "JSON equals check (not fully implemented)";
            case ASSERT_LOG_CONTAINS:
                return "Log contains check (not fully implemented)";
            case ASSERT_CONDITION:
                return "Condition check (not fully implemented)";
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

    /**
     * Extract value from data using JSONPath-like syntax.
     * Supports simple key access and nested key access (e.g., "items[0].name").
     */
    @SuppressWarnings("unchecked")
    private String extractWithJsonPath(Object data, String jsonPath) {
        if (data == null || jsonPath == null) {
            return String.valueOf(data);
        }

        try {
            if (data instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) data;

                // Handle simple key access (e.g., "health")
                if (!jsonPath.contains(".") && !jsonPath.contains("[")) {
                    Object value = map.get(jsonPath);
                    return value != null ? String.valueOf(value) : "";
                }

                // Handle nested access (e.g., "items[0]" or "position.x")
                String[] parts = jsonPath.split("\\.");
                Object current = map;

                for (String part : parts) {
                    if (current instanceof Map) {
                        current = ((Map<String, Object>) current).get(part);
                    } else if (current instanceof List && part.contains("[")) {
                        // Handle array access like "items[0]"
                        String arrayKey = part.substring(0, part.indexOf("["));
                        int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                        List<?> list = (List<?>) ((Map<String, Object>) current).get(arrayKey);
                        if (list != null && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return "";
                        }
                    } else {
                        return "";
                    }

                    if (current == null) {
                        return "";
                    }
                }

                return current != null ? String.valueOf(current) : "";
            }
        } catch (Exception e) {
            log("    ⚠️ JSONPath extraction error: " + e.getMessage());
        }

        return String.valueOf(data);
    }

    /**
     * Filter entities based on criteria.
     * Supports filtering by type (e.g., "type=zombie") or name (e.g., "name=TestEntity").
     */
    @SuppressWarnings("unchecked")
    private Object filterEntities(Object entitiesData, String filterType, String filterValue) {
        if (entitiesData == null) {
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> filtered = new ArrayList<>();

            if (entitiesData instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) entitiesData;
                Object entitiesObj = map.get("entities");
                if (entitiesObj instanceof List) {
                    List<Map<String, Object>> entities = (List<Map<String, Object>>) entitiesObj;

                    for (Map<String, Object> entity : entities) {
                        boolean matches = false;

                        if ("type".equals(filterType)) {
                            String entityType = (String) entity.get("type");
                            matches = entityType != null && entityType.equalsIgnoreCase(filterValue);
                        } else if ("name".equals(filterType)) {
                            String entityName = (String) entity.get("name");
                            matches = entityName != null && entityName.equalsIgnoreCase(filterValue);
                        }

                        if (matches) {
                            filtered.add(entity);
                        }
                    }
                }
            }

            return filtered;
        } catch (Exception e) {
            log("    ⚠️ Entity filtering error: " + e.getMessage());
        }

        return new ArrayList<>();
    }
}
