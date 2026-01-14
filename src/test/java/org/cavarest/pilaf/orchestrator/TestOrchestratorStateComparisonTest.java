package org.cavarest.pilaf.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.TestStory;
import org.cavarest.pilaf.parser.YamlStoryParser;
import org.cavarest.pilaf.report.TestReporter;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestOrchestrator state comparison functionality.
 * Verifies that COMPARE_STATES properly populates stateBefore/stateAfter
 * and that the JSON serialization produces valid output.
 */
public class TestOrchestratorStateComparisonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Test that COMPARE_STATES properly serializes state data to JSON.
     * This test simulates what happens in TestOrchestrator.executeAction()
     * for the COMPARE_STATES case.
     */
    @Test
    public void testCompareStatesJsonSerialization() throws Exception {
        // Simulate the state storage from get_player_position actions
        Map<String, Object> storedStates = new HashMap<>();

        // Simulate storing initial position
        Map<String, Object> initialPosition = new HashMap<>();
        initialPosition.put("x", 0.0);
        initialPosition.put("y", 64.0);
        initialPosition.put("z", 0.0);
        storedStates.put("initial_position", initialPosition);

        // Simulate storing final position (after teleport)
        Map<String, Object> finalPosition = new HashMap<>();
        finalPosition.put("x", 150.0);
        finalPosition.put("y", 64.0);
        finalPosition.put("z", 150.0);
        storedStates.put("final_position", finalPosition);

        // Simulate COMPARE_STATES logic from TestOrchestrator.java lines 703-736
        String state1 = "initial_position";
        String state2 = "final_position";

        Object state1Obj = storedStates.get(state1);
        Object state2Obj = storedStates.get(state2);

        assertNotNull(state1Obj, "state1 should not be null");
        assertNotNull(state2Obj, "state2 should not be null");

        // Serialize to JSON (same as TestOrchestrator does)
        String state1Json = state1Obj != null ? objectMapper.writeValueAsString(state1Obj) : "{}";
        String state2Json = state2Obj != null ? objectMapper.writeValueAsString(state2Obj) : "{}";

        assertNotNull(state1Json, "state1Json should not be null");
        assertNotNull(state2Json, "state2Json should not be null");
        assertFalse(state1Json.isEmpty(), "state1Json should not be empty");
        assertFalse(state2Json.isEmpty(), "state2Json should not be empty");

        // Verify JSON is valid and contains expected data
        Map<String, Object> parsedBefore = objectMapper.readValue(state1Json, Map.class);
        Map<String, Object> parsedAfter = objectMapper.readValue(state2Json, Map.class);

        assertEquals(0.0, parsedBefore.get("x"), "Initial x position should be 0");
        assertEquals(150.0, parsedAfter.get("x"), "Final x position should be 150");

        System.out.println("state1Json = " + state1Json);
        System.out.println("state2Json = " + state2Json);
    }

    /**
     * Test that YamlStoryParser correctly parses variable_name (snake_case).
     */
    @Test
    public void testPrintStateComparisonWithSnakeCase() {
        String yaml = "name: \"Test\"\n" +
                      "steps:\n" +
                      "  - action: \"print_state_comparison\"\n" +
                      "    variable_name: \"position_diff\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.PRINT_STATE_COMPARISON, step.getType());
        assertEquals("position_diff", step.getVariableName(),
            "variableName should be parsed from variable_name (snake_case)");
    }

    /**
     * Test that the full story parsing works with state comparison actions.
     */
    @Test
    public void testParseStoryWithCompareStates() {
        String yaml = "name: \"State Compare Test\"\n" +
                      "steps:\n" +
                      "  - action: \"get_player_position\"\n" +
                      "    storeAs: \"initial_position\"\n" +
                      "  - action: \"get_player_position\"\n" +
                      "    storeAs: \"final_position\"\n" +
                      "  - action: \"compare_states\"\n" +
                      "    state1: \"initial_position\"\n" +
                      "    state2: \"final_position\"\n" +
                      "    storeAs: \"position_diff\"\n" +
                      "  - action: \"print_state_comparison\"\n" +
                      "    variable_name: \"position_diff\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertEquals(4, story.getSteps().size());

        // Verify compare_states action
        Action compareAction = story.getSteps().get(2);
        assertEquals(Action.ActionType.COMPARE_STATES, compareAction.getType());
        assertEquals("initial_position", compareAction.getState1());
        assertEquals("final_position", compareAction.getState2());
        assertEquals("position_diff", compareAction.getStoreAs());

        // Verify print_state_comparison action
        Action printAction = story.getSteps().get(3);
        assertEquals(Action.ActionType.PRINT_STATE_COMPARISON, printAction.getType());
        assertEquals("position_diff", printAction.getVariableName());
    }

    /**
     * Test that TestReporter.TestStep properly stores stateBefore and stateAfter.
     */
    @Test
    public void testTestReporterStepStateStorage() {
        TestReporter reporter = new TestReporter("Test");
        TestReporter.TestStep step = reporter.step("Compare positions");

        // Simulate what TestOrchestrator does in COMPARE_STATES case
        String stateBeforeJson = "{\"x\":0.0,\"y\":64.0,\"z\":0.0}";
        String stateAfterJson = "{\"x\":150.0,\"y\":64.0,\"z\":150.0}";

        step.stateBefore(stateBeforeJson);
        step.stateAfter(stateAfterJson);

        assertEquals(stateBeforeJson, step.stateBefore, "stateBefore should be stored");
        assertEquals(stateAfterJson, step.stateAfter, "stateAfter should be stored");

        // Verify they are not null or empty
        assertNotNull(step.stateBefore);
        assertNotNull(step.stateAfter);
        assertFalse(step.stateBefore.isEmpty());
        assertFalse(step.stateAfter.isEmpty());
    }

    /**
     * Test that Jackson JSON serialization works correctly for complex objects.
     */
    @Test
    public void testJsonSerializationOfComplexState() throws Exception {
        // Simulate a complex inventory state
        Map<String, Object> inventory = new HashMap<>();
        inventory.put("items", java.util.Arrays.asList(
            java.util.Map.of("id", "diamond_sword", "count", 1),
            java.util.Map.of("id", "golden_apple", "count", 5)
        ));
        inventory.put("selectedSlot", 0);

        String json = objectMapper.writeValueAsString(inventory);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Parse it back
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertNotNull(parsed.get("items"));
        assertTrue(parsed.get("items") instanceof java.util.List);
    }
}
