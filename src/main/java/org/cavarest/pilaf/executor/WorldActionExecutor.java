package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.EnumSet;
import java.util.Set;

/**
 * Executor for world/environment-related actions.
 * Handles: GET_WORLD_TIME, GET_WEATHER, SET_TIME, SET_WEATHER
 */
public class WorldActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.GET_WORLD_TIME,
        Action.ActionType.GET_WEATHER,
        Action.ActionType.SET_TIME,
        Action.ActionType.SET_WEATHER
    );

    @Override
    public String getName() {
        return "WorldExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case GET_WORLD_TIME:
                    return executeGetWorldTime(action, backend, stateManager);

                case GET_WEATHER:
                    return executeGetWeather(action, backend, stateManager);

                case SET_TIME:
                    return executeSetTime(action, backend);

                case SET_WEATHER:
                    return executeSetWeather(action, backend);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    private ActionResult executeGetWorldTime(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Long worldTime = null;
        if (isMineflayer(backend)) {
            worldTime = asMineflayer(backend).getWorldTime();
        } else if (isRcon(backend)) {
            worldTime = asRcon(backend).getWorldTime();
        }

        String response = worldTime != null ? "time=" + worldTime : "null";

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), worldTime);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeGetWeather(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        String weather = null;
        if (isMineflayer(backend)) {
            weather = asMineflayer(backend).getWeather();
        } else if (isRcon(backend)) {
            weather = asRcon(backend).getWeather();
        }

        String response = weather != null ? "weather=" + weather : "null";

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), weather);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeSetTime(Action action, PilafBackend backend) throws Exception {
        String timeValue = action.getEntity() != null ? action.getEntity() : "day";
        String result = executeRcon(backend, "time set " + timeValue);
        return ActionResult.success(result != null ? result : "Time set to " + timeValue);
    }

    private ActionResult executeSetWeather(Action action, PilafBackend backend) throws Exception {
        String weatherValue = action.getEntity() != null ? action.getEntity() : "clear";
        String result = executeRcon(backend, "weather " + weatherValue);
        return ActionResult.success(result != null ? result : "Weather set to " + weatherValue);
    }

    private String executeRcon(PilafBackend backend, String command) throws Exception {
        if (isMineflayer(backend)) {
            return asMineflayer(backend).executeRconWithCapture(command);
        } else if (isRcon(backend)) {
            return asRcon(backend).executeRconWithCapture(command);
        }
        return null;
    }
}
