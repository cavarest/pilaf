package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.EnumSet;
import java.util.Set;

/**
 * Executor for assertion actions.
 * Handles: ASSERT_RESPONSE_CONTAINS, ASSERT_ENTITY_EXISTS, ASSERT_ENTITY_MISSING,
 *          ASSERT_PLAYER_HAS_ITEM, ASSERT_JSON_EQUALS, ASSERT_LOG_CONTAINS, ASSERT_CONDITION
 */
public class AssertionActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.ASSERT_RESPONSE_CONTAINS,
        Action.ActionType.ASSERT_ENTITY_EXISTS,
        Action.ActionType.ASSERT_ENTITY_MISSING,
        Action.ActionType.ASSERT_PLAYER_HAS_ITEM,
        Action.ActionType.ASSERT_JSON_EQUALS,
        Action.ActionType.ASSERT_LOG_CONTAINS,
        Action.ActionType.ASSERT_CONDITION,
        Action.ActionType.PRINT_STATE_COMPARISON,
        Action.ActionType.PRINT_STORED_STATE
    );

    @Override
    public String getName() {
        return "AssertionExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case ASSERT_RESPONSE_CONTAINS:
                    return executeAssertResponseContains(action, stateManager);

                case ASSERT_ENTITY_EXISTS:
                    return executeAssertEntityExists(action, backend);

                case ASSERT_ENTITY_MISSING:
                    return executeAssertEntityMissing(action, backend);

                case ASSERT_PLAYER_HAS_ITEM:
                    return executeAssertPlayerHasItem(action, backend);

                case ASSERT_JSON_EQUALS:
                    return executeAssertJsonEquals(action, stateManager);

                case ASSERT_LOG_CONTAINS:
                    return executeAssertLogContains(action);

                case ASSERT_CONDITION:
                    return executeAssertCondition(action);

                case PRINT_STATE_COMPARISON:
                    return executePrintStateComparison(action, stateManager);

                case PRINT_STORED_STATE:
                    return executePrintStoredState(action, stateManager);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    /**
     * Asserts that a stored response contains specific text.
     * The 'source' parameter specifies the stored state variable name.
     */
    private ActionResult executeAssertResponseContains(Action action, StateManager stateManager) {
        String source = action.getSource();
        String contains = action.getContains();

        if (source == null || source.isEmpty()) {
            return ActionResult.failure("Missing 'source' parameter for assert_response_contains");
        }

        if (contains == null || contains.isEmpty()) {
            return ActionResult.failure("Missing 'contains' parameter for assert_response_contains");
        }

        // Get the stored value
        Object storedValue = stateManager.retrieve(source);
        String responseText = storedValue != null ? storedValue.toString() : "null";

        // Check if response contains the text
        boolean passed = responseText.contains(contains);

        // Build detailed response message
        StringBuilder response = new StringBuilder();
        response.append("Assertion: Response contains '").append(contains).append("'\n");
        response.append("Source: ").append(source).append("\n");
        response.append("Result: ").append(passed ? "PASSED" : "FAILED").append("\n");
        response.append("\nActual Response:\n").append(truncateResponse(responseText, 500));

        return new ActionResult.Builder()
            .success(passed)
            .response(response.toString())
            .build();
    }

    /**
     * Asserts that an entity exists in the world.
     */
    private ActionResult executeAssertEntityExists(Action action, PilafBackend backend) {
        String entity = action.getEntity();

        if (entity == null || entity.isEmpty()) {
            return ActionResult.failure("Missing 'entity' parameter for assert_entity_exists");
        }

        boolean exists = backend.entityExists(entity);

        StringBuilder response = new StringBuilder();
        response.append("Assertion: Entity '").append(entity).append("' exists\n");
        response.append("Result: ").append(exists ? "PASSED" : "FAILED").append("\n");
        response.append("Entity exists: ").append(exists);

        return new ActionResult.Builder()
            .success(exists)
            .response(response.toString())
            .build();
    }

    /**
     * Asserts that an entity does NOT exist in the world.
     */
    private ActionResult executeAssertEntityMissing(Action action, PilafBackend backend) {
        String entity = action.getEntity();

        if (entity == null || entity.isEmpty()) {
            return ActionResult.failure("Missing 'entity' parameter for assert_entity_missing");
        }

        boolean exists = backend.entityExists(entity);
        boolean passed = !exists;  // Pass if entity is missing

        StringBuilder response = new StringBuilder();
        response.append("Assertion: Entity '").append(entity).append("' does NOT exist\n");
        response.append("Result: ").append(passed ? "PASSED" : "FAILED").append("\n");
        response.append("Entity exists: ").append(exists);

        return new ActionResult.Builder()
            .success(passed)
            .response(response.toString())
            .build();
    }

    /**
     * Asserts that a player has a specific item in their inventory.
     */
    private ActionResult executeAssertPlayerHasItem(Action action, PilafBackend backend) {
        String player = action.getPlayer();
        String item = action.getItem();

        if (player == null || player.isEmpty()) {
            return ActionResult.failure("Missing 'player' parameter for assert_player_has_item");
        }

        if (item == null || item.isEmpty()) {
            return ActionResult.failure("Missing 'item' parameter for assert_player_has_item");
        }

        boolean hasItem = backend.playerInventoryContains(player, item, action.getSlot());

        StringBuilder response = new StringBuilder();
        response.append("Assertion: Player '").append(player).append("' has '").append(item).append("'\n");
        response.append("Result: ").append(hasItem ? "PASSED" : "FAILED").append("\n");
        response.append("Has item: ").append(hasItem);

        return new ActionResult.Builder()
            .success(hasItem)
            .response(response.toString())
            .build();
    }

    /**
     * Asserts that two JSON values are equal.
     */
    private ActionResult executeAssertJsonEquals(Action action, StateManager stateManager) {
        String state1 = action.getState1();
        String state2 = action.getState2();

        if (state1 == null || state2 == null) {
            return ActionResult.failure("Missing state1 or state2 for assert_json_equals");
        }

        Object value1 = stateManager.retrieve(state1);
        Object value2 = stateManager.retrieve(state2);

        String json1 = value1 != null ? serializeToJson(value1) : "null";
        String json2 = value2 != null ? serializeToJson(value2) : "null";

        boolean equals = json1.equals(json2);

        StringBuilder response = new StringBuilder();
        response.append("Assertion: JSON equality\n");
        response.append("State 1: ").append(state1).append("\n");
        response.append("State 2: ").append(state2).append("\n");
        response.append("Result: ").append(equals ? "PASSED" : "FAILED").append("\n");
        response.append("\nJSON 1:\n").append(json1).append("\n\nJSON 2:\n").append(json2);

        return new ActionResult.Builder()
            .success(equals)
            .response(response.toString())
            .build();
    }

    /**
     * Asserts that a log contains specific text (placeholder).
     */
    private ActionResult executeAssertLogContains(Action action) {
        return ActionResult.failure("assert_log_contains not yet implemented");
    }

    /**
     * Asserts that a condition is true (placeholder).
     */
    private ActionResult executeAssertCondition(Action action) {
        return ActionResult.failure("assert_condition not yet implemented");
    }

    /**
     * Prints a state comparison result.
     */
    private ActionResult executePrintStateComparison(Action action, StateManager stateManager) {
        String variableName = action.getVariableName();

        if (variableName == null || variableName.isEmpty()) {
            return ActionResult.failure("Missing 'variableName' for print_state_comparison");
        }

        Object storedValue = stateManager.retrieve(variableName);

        StringBuilder response = new StringBuilder();
        response.append("State Comparison: ").append(variableName).append("\n");

        if (storedValue != null) {
            String json = serializeToJson(storedValue);
            response.append("\n").append(json);
        } else {
            response.append("\n(null or not found)");
        }

        return ActionResult.success(response.toString());
    }

    /**
     * Prints a stored state value.
     */
    private ActionResult executePrintStoredState(Action action, StateManager stateManager) {
        String variableName = action.getVariableName();

        if (variableName == null || variableName.isEmpty()) {
            return ActionResult.failure("Missing 'variableName' for print_stored_state");
        }

        Object storedValue = stateManager.retrieve(variableName);

        StringBuilder response = new StringBuilder();
        response.append("Stored State: ").append(variableName).append("\n");

        if (storedValue != null) {
            String json = serializeToJson(storedValue);
            response.append("\n").append(json);
        } else {
            response.append("\n(null or not found)");
        }

        return ActionResult.success(response.toString());
    }

    /**
     * Truncates a response string to a maximum length.
     */
    private String truncateResponse(String response, int maxLength) {
        if (response == null) return "null";
        if (response.length() <= maxLength) return response;
        return response.substring(0, maxLength) + "... (truncated, total " + response.length() + " chars)";
    }
}
