package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Executor for state management actions.
 * Handles: COMPARE_STATES, STORE_STATE, GET_CHAT_HISTORY
 */
public class StateActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.COMPARE_STATES,
        Action.ActionType.STORE_STATE,
        Action.ActionType.GET_CHAT_HISTORY
    );

    @Override
    public String getName() {
        return "StateExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case COMPARE_STATES:
                    return executeCompareStates(action, stateManager);

                case STORE_STATE:
                    return executeStoreState(action, stateManager);

                case GET_CHAT_HISTORY:
                    return executeGetChatHistory(action, backend, stateManager);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    private ActionResult executeCompareStates(Action action, StateManager stateManager) {
        StateManager.ComparisonResult compResult = stateManager.compare(
            action.getState1(),
            action.getState2()
        );

        // Store the comparison result if requested
        if (action.getStoreAs() != null) {
            Map<String, Object> comparisonData = new HashMap<>();
            comparisonData.put("before", compResult.getBeforeState());
            comparisonData.put("after", compResult.getAfterState());
            comparisonData.put("hasChanges", compResult.hasChanges());
            stateManager.store(action.getStoreAs(), comparisonData);
        }

        // Return comparison result for reporter
        return ActionResult.comparison(
            compResult.getBeforeJson(),
            compResult.getAfterJson(),
            compResult.getDiffJson(),
            compResult.hasChanges()
        );
    }

    private ActionResult executeStoreState(Action action, StateManager stateManager) {
        Object value = action.getValue();
        if (action.getStoreAs() != null && value != null) {
            stateManager.store(action.getStoreAs(), value);
            return ActionResult.success("Stored value in " + action.getStoreAs());
        }
        return ActionResult.failure("Missing storeAs or value for STORE_STATE action");
    }

    private ActionResult executeGetChatHistory(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        if (isMineflayer(backend)) {
            Object chatHistory = asMineflayer(backend).getChatHistory(action.getPlayer());
            String response = chatHistory != null ? serializeToJson(chatHistory) : "[]";

            if (action.getStoreAs() != null) {
                stateManager.store(action.getStoreAs(), chatHistory);
            }

            return ActionResult.success(response);
        } else if (isRcon(backend)) {
            return ActionResult.success("Chat history not available via RCON");
        }

        return ActionResult.failure("Unknown backend type");
    }
}
