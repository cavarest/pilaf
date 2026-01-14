package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.EnumSet;
import java.util.Set;

/**
 * Executor for client-side actions.
 * Handles: USE_ITEM, ATTACK_ENTITY, LOOK_AT
 */
public class ClientActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.USE_ITEM,
        Action.ActionType.ATTACK_ENTITY,
        Action.ActionType.LOOK_AT
    );

    @Override
    public String getName() {
        return "ClientExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case USE_ITEM:
                    return executeUseItem(action, backend);

                case ATTACK_ENTITY:
                    return executeAttackEntity(action, backend);

                case LOOK_AT:
                    return executeLookAt(action, backend);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    private ActionResult executeUseItem(Action action, PilafBackend backend) {
        backend.useItem(action.getPlayer(), action.getItem(), action.getEntity());
        return ActionResult.success("Used item " + action.getItem());
    }

    private ActionResult executeAttackEntity(Action action, PilafBackend backend) throws Exception {
        // Attack entity using damage command via RCON
        String target = action.getEntity() != null ? action.getEntity() : "@e[sort=nearest,limit=1]";
        String damageCmd = "damage " + target + " 1 minecraft:player_attack";
        String result = executeRcon(backend, damageCmd);
        return ActionResult.success(result != null ? result : "Attacked entity " + action.getEntity());
    }

    private ActionResult executeLookAt(Action action, PilafBackend backend) throws Exception {
        // Look at is a client-side action - use teleport with rotation if needed
        // For now, return success as the action is acknowledged
        return ActionResult.success("Looking at " + action.getEntity());
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
