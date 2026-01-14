package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.model.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Factory for creating and managing action executors.
 * Maintains a registry of all available executors and routes actions to the appropriate one.
 *
 * This replaces the giant switch statement in TestOrchestrator with a clean,
 * extensible executor pattern.
 */
public class ActionExecutorFactory {

    private final List<ActionExecutor> executors = new ArrayList<>();

    public ActionExecutorFactory() {
        // Register all executors
        registerExecutor(new InventoryActionExecutor());
        registerExecutor(new PlayerActionExecutor());
        registerExecutor(new EntityActionExecutor());
        registerExecutor(new StateActionExecutor());
        registerExecutor(new AssertionActionExecutor());
        registerExecutor(new ServerActionExecutor());
        registerExecutor(new WorldActionExecutor());
        registerExecutor(new ClientActionExecutor());
    }

    /**
     * Register an executor with the factory.
     */
    public void registerExecutor(ActionExecutor executor) {
        executors.add(executor);
    }

    /**
     * Get the executor for a given action type.
     *
     * @param type The action type
     * @return Optional containing the executor, or empty if no executor found
     */
    public Optional<ActionExecutor> getExecutor(Action.ActionType type) {
        return executors.stream()
            .filter(e -> e.canExecute(type))
            .findFirst();
    }

    /**
     * Check if an executor exists for the given action type.
     */
    public boolean hasExecutor(Action.ActionType type) {
        return executors.stream().anyMatch(e -> e.canExecute(type));
    }

    /**
     * Get all registered executors.
     */
    public List<ActionExecutor> getAllExecutors() {
        return new ArrayList<>(executors);
    }

    /**
     * Get count of registered executors.
     */
    public int getExecutorCount() {
        return executors.size();
    }

    /**
     * Get count of action types covered by registered executors.
     */
    public int getCoveredActionTypeCount() {
        return (int) executors.stream()
            .flatMap(e -> e.getSupportedTypes().stream())
            .distinct()
            .count();
    }
}
