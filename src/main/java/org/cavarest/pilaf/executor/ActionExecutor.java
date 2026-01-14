package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.Set;

/**
 * Interface for action executors.
 * Each executor handles a specific category of actions (inventory, player, entity, etc.)
 *
 * This follows the Command Pattern, allowing:
 * - Single Responsibility: Each executor handles one category
 * - Open/Closed: Add new executors without modifying existing code
 * - Easy Testing: Executors can be unit tested in isolation
 */
public interface ActionExecutor {

    /**
     * Execute an action and return the result.
     *
     * @param action       The action to execute
     * @param backend      The backend to execute against
     * @param stateManager The state manager for storing/retrieving state
     * @return ActionResult containing success/failure, response, and any state changes
     */
    ActionResult execute(Action action, PilafBackend backend, StateManager stateManager);

    /**
     * Get the set of action types this executor can handle.
     *
     * @return Set of ActionType values this executor supports
     */
    Set<Action.ActionType> getSupportedTypes();

    /**
     * Check if this executor can handle the given action type.
     *
     * @param type The action type to check
     * @return true if this executor can handle the action type
     */
    default boolean canExecute(Action.ActionType type) {
        return getSupportedTypes().contains(type);
    }

    /**
     * Get a human-readable name for this executor.
     *
     * @return The executor name (e.g., "InventoryExecutor", "PlayerExecutor")
     */
    String getName();
}
