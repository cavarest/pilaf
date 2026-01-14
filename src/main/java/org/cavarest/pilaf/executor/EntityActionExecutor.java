package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.EnumSet;
import java.util.Set;

/**
 * Executor for entity-related actions.
 * Handles: SPAWN_ENTITY, GET_ENTITIES, GET_ENTITIES_IN_VIEW, GET_ENTITY_BY_NAME,
 *          GET_ENTITY_HEALTH, SET_ENTITY_HEALTH, KILL_ENTITY, REMOVE_ENTITIES
 */
public class EntityActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.SPAWN_ENTITY,
        Action.ActionType.GET_ENTITIES,
        Action.ActionType.GET_ENTITIES_IN_VIEW,
        Action.ActionType.GET_ENTITY_BY_NAME,
        Action.ActionType.GET_ENTITY_HEALTH,
        Action.ActionType.SET_ENTITY_HEALTH,
        Action.ActionType.KILL_ENTITY,
        Action.ActionType.REMOVE_ENTITIES
    );

    @Override
    public String getName() {
        return "EntityExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case SPAWN_ENTITY:
                    return executeSpawnEntity(action, backend);

                case GET_ENTITIES:
                    return executeGetEntities(action, backend, stateManager);

                case GET_ENTITIES_IN_VIEW:
                    return executeGetEntitiesInView(action, backend, stateManager);

                case GET_ENTITY_BY_NAME:
                    return executeGetEntityByName(action, backend, stateManager);

                case GET_ENTITY_HEALTH:
                    return executeGetEntityHealth(action, backend, stateManager);

                case SET_ENTITY_HEALTH:
                    return executeSetEntityHealth(action, backend);

                case KILL_ENTITY:
                    return executeKillEntity(action, backend);

                case REMOVE_ENTITIES:
                    return executeRemoveEntities(action, backend);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    private ActionResult executeSpawnEntity(Action action, PilafBackend backend) {
        backend.spawnEntity(action.getName(), action.getEntityType(),
            action.getLocation(), action.getEquipment());
        return ActionResult.success("Spawned " + action.getEntityType() + " named " + action.getName());
    }

    private ActionResult executeGetEntities(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Object entities = null;
        if (isMineflayer(backend)) {
            entities = asMineflayer(backend).getEntities(action.getPlayer());
        } else if (isRcon(backend)) {
            entities = asRcon(backend).getEntitiesInView(action.getPlayer());
        }

        String response = serializeToJson(entities);

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), entities);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeGetEntitiesInView(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Object entitiesInView = null;
        if (isMineflayer(backend)) {
            entitiesInView = asMineflayer(backend).getEntitiesInView(action.getPlayer());
        } else if (isRcon(backend)) {
            entitiesInView = asRcon(backend).getEntitiesInView(action.getPlayer());
        }

        String response = serializeToJson(entitiesInView);

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), entitiesInView);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeGetEntityByName(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Object entityData = null;
        if (isMineflayer(backend)) {
            entityData = asMineflayer(backend).getEntityByName(action.getEntity(), action.getPlayer());
        } else if (isRcon(backend)) {
            entityData = asRcon(backend).getEntityByName(action.getEntity(), action.getPlayer());
        }

        String response = serializeToJson(entityData);

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), entityData);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeGetEntityHealth(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Double entityHealth = null;
        if (isMineflayer(backend)) {
            entityHealth = asMineflayer(backend).getEntityHealth(action.getEntity());
        } else if (isRcon(backend)) {
            entityHealth = asRcon(backend).getEntityHealth(action.getEntity());
        }

        String response = entityHealth != null ? "health=" + entityHealth : "null";

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), entityHealth);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeSetEntityHealth(Action action, PilafBackend backend) throws Exception {
        if (isMineflayer(backend)) {
            asMineflayer(backend).setEntityHealth(action.getEntity(), action.getValue());
        } else if (isRcon(backend)) {
            asRcon(backend).setEntityHealth(action.getEntity(), action.getValue());
        }
        return ActionResult.success("Entity health set to " + action.getValue());
    }

    private ActionResult executeKillEntity(Action action, PilafBackend backend) throws Exception {
        String killCmd = "kill " + (action.getEntity() != null ? action.getEntity() : "@e[type=" + action.getEntityType() + "]");
        String result = executeRcon(backend, killCmd);
        return ActionResult.success(result != null ? result : "Entity killed");
    }

    private ActionResult executeRemoveEntities(Action action, PilafBackend backend) {
        backend.removeAllTestEntities();
        return ActionResult.success("Removed all test entities");
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
