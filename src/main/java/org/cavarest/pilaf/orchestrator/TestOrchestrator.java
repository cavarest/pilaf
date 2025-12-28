package org.cavarest.pilaf.orchestrator;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.Assertion;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.model.TestStory;
import org.cavarest.pilaf.parser.YamlStoryParser;

import java.util.Collections;

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
                    log("    ⏳ Waiting " + action.getDuration() + "ms...");
                    Thread.sleep(action.getDuration());
                    break;
                case REMOVE_ENTITIES:
                    backend.removeAllTestEntities();
                    break;
                case REMOVE_PLAYERS:
                    backend.removeAllTestPlayers();
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
            case WAIT: return "Wait " + action.getDuration() + "ms";
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
