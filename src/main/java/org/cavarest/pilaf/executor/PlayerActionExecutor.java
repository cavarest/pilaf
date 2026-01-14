package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Executor for player-related actions.
 * Handles: CONNECT_PLAYER, DISCONNECT_PLAYER, MAKE_OPERATOR, GET_PLAYER_POSITION,
 *          GET_PLAYER_HEALTH, MOVE_PLAYER, TELEPORT_PLAYER, KILL_PLAYER, etc.
 */
public class PlayerActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.CONNECT_PLAYER,
        Action.ActionType.DISCONNECT_PLAYER,
        Action.ActionType.MAKE_OPERATOR,
        Action.ActionType.GET_PLAYER_POSITION,
        Action.ActionType.GET_PLAYER_HEALTH,
        Action.ActionType.MOVE_PLAYER,
        Action.ActionType.TELEPORT_PLAYER,
        Action.ActionType.SET_PLAYER_HEALTH,
        Action.ActionType.KILL_PLAYER,
        Action.ActionType.SET_SPAWN_POINT,
        Action.ActionType.GAMEMODE_CHANGE,
        Action.ActionType.PLAYER_COMMAND,
        Action.ActionType.EXECUTE_PLAYER_COMMAND,
        Action.ActionType.EXECUTE_PLAYER_RAW,
        Action.ActionType.SEND_CHAT_MESSAGE
    );

    @Override
    public String getName() {
        return "PlayerExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case CONNECT_PLAYER:
                    return executeConnect(action, backend);

                case DISCONNECT_PLAYER:
                    return executeDisconnect(action, backend);

                case MAKE_OPERATOR:
                    return executeMakeOperator(action, backend);

                case GET_PLAYER_POSITION:
                    return executeGetPosition(action, backend, stateManager);

                case GET_PLAYER_HEALTH:
                    return executeGetHealth(action, backend, stateManager);

                case MOVE_PLAYER:
                    return executeMove(action, backend);

                case TELEPORT_PLAYER:
                    return executeTeleport(action, backend);

                case SET_PLAYER_HEALTH:
                    return executeSetHealth(action, backend);

                case KILL_PLAYER:
                    return executeKill(action, backend);

                case SET_SPAWN_POINT:
                    return executeSetSpawnPoint(action, backend);

                case GAMEMODE_CHANGE:
                    return executeGamemodeChange(action, backend);

                case PLAYER_COMMAND:
                case EXECUTE_PLAYER_COMMAND:
                    return executePlayerCommand(action, backend);

                case EXECUTE_PLAYER_RAW:
                    return executePlayerRaw(action, backend);

                case SEND_CHAT_MESSAGE:
                    return executeSendChat(action, backend);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    private ActionResult executeConnect(Action action, PilafBackend backend) throws Exception {
        if (isMineflayer(backend)) {
            asMineflayer(backend).connectPlayer(action.getPlayer());
        } else if (isRcon(backend)) {
            asRcon(backend).connectPlayer(action.getPlayer());
        }
        return ActionResult.success("Connected player " + action.getPlayer());
    }

    private ActionResult executeDisconnect(Action action, PilafBackend backend) throws Exception {
        if (isMineflayer(backend)) {
            asMineflayer(backend).disconnectPlayer(action.getPlayer());
        } else if (isRcon(backend)) {
            asRcon(backend).disconnectPlayer(action.getPlayer());
        }
        return ActionResult.success("Disconnected player " + action.getPlayer());
    }

    private ActionResult executeMakeOperator(Action action, PilafBackend backend) throws Exception {
        if (isMineflayer(backend)) {
            asMineflayer(backend).makeOperator(action.getPlayer());
        } else if (isRcon(backend)) {
            asRcon(backend).makeOperator(action.getPlayer());
        }
        return ActionResult.success("Made " + action.getPlayer() + " an operator");
    }

    private ActionResult executeGetPosition(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Object position = null;
        if (isMineflayer(backend)) {
            position = asMineflayer(backend).getPlayerPosition(action.getPlayer());
        } else if (isRcon(backend)) {
            position = asRcon(backend).getPlayerPosition(action.getPlayer());
        }

        String response = serializeToJson(position);

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), position);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeGetHealth(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Double health = 0.0;
        if (isMineflayer(backend)) {
            health = asMineflayer(backend).getPlayerHealthAsDouble(action.getPlayer());
        } else if (isRcon(backend)) {
            health = asRcon(backend).getPlayerHealth(action.getPlayer());
        }

        String response = health != null ? "health=" + health + ", food=20" : "null";

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), health);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeMove(Action action, PilafBackend backend) {
        backend.movePlayer(action.getPlayer(), "destination", action.getDestination());
        return ActionResult.success("Moved " + action.getPlayer() + " to " + action.getDestination());
    }

    private ActionResult executeTeleport(Action action, PilafBackend backend) throws Exception {
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

        String result = executeRcon(backend, tpCmd);
        return ActionResult.success(result != null ? result : "Player teleported");
    }

    private ActionResult executeSetHealth(Action action, PilafBackend backend) throws Exception {
        String healthCmd = "attribute minecraft:generic.max_health base set " + action.getValue();
        executeRcon(backend, healthCmd);
        executeRcon(backend, "heal " + action.getPlayer());
        return ActionResult.success("Player health set to " + action.getValue());
    }

    private ActionResult executeKill(Action action, PilafBackend backend) throws Exception {
        String result = executeRcon(backend, "kill " + action.getPlayer());
        return ActionResult.success(result != null ? result : "Player killed");
    }

    private ActionResult executeSetSpawnPoint(Action action, PilafBackend backend) throws Exception {
        String spawnCmd = "spawnpoint " + action.getPlayer();
        if (action.getLocation() != null && action.getLocation().size() >= 3) {
            spawnCmd += " " + action.getLocation().get(0).intValue()
                       + " " + action.getLocation().get(1).intValue()
                       + " " + action.getLocation().get(2).intValue();
        }
        String result = executeRcon(backend, spawnCmd);
        return ActionResult.success(result != null ? result : "Spawn point set");
    }

    private ActionResult executeGamemodeChange(Action action, PilafBackend backend) throws Exception {
        String gamemode = action.getEntity() != null ? action.getEntity() : "survival";
        String result = executeRcon(backend, "gamemode " + gamemode + " " + action.getPlayer());
        return ActionResult.success(result != null ? result : "Gamemode changed");
    }

    private ActionResult executePlayerCommand(Action action, PilafBackend backend) {
        backend.executePlayerCommand(action.getPlayer(), action.getCommand(),
            action.getArgs() != null ? action.getArgs() : Collections.emptyList());
        return ActionResult.success("Executed command: " + action.getCommand());
    }

    private ActionResult executePlayerRaw(Action action, PilafBackend backend) throws Exception {
        String result = null;
        if (isMineflayer(backend)) {
            result = asMineflayer(backend).executePlayerCommandRaw(action.getPlayer(), action.getCommand());
        } else if (isRcon(backend)) {
            result = asRcon(backend).executePlayerCommandRaw(action.getPlayer(), action.getCommand());
        }
        return ActionResult.success(result != null ? result : "");
    }

    private ActionResult executeSendChat(Action action, PilafBackend backend) {
        backend.sendChat(action.getPlayer(), action.getMessage());
        return ActionResult.success("Sent chat: " + action.getMessage());
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
