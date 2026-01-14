package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

import java.util.EnumSet;
import java.util.Set;

/**
 * Executor for inventory-related actions.
 * Handles: GET_INVENTORY, GET_PLAYER_INVENTORY, GIVE_ITEM, REMOVE_ITEM,
 *          EQUIP_ITEM, GET_PLAYER_EQUIPMENT, CLEAR_INVENTORY
 */
public class InventoryActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.GET_INVENTORY,
        Action.ActionType.GET_PLAYER_INVENTORY,
        Action.ActionType.GIVE_ITEM,
        Action.ActionType.REMOVE_ITEM,
        Action.ActionType.EQUIP_ITEM,
        Action.ActionType.GET_PLAYER_EQUIPMENT,
        Action.ActionType.CLEAR_INVENTORY
    );

    @Override
    public String getName() {
        return "InventoryExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case GET_INVENTORY:
                case GET_PLAYER_INVENTORY:
                    return executeGetInventory(action, backend, stateManager);

                case GIVE_ITEM:
                    return executeGiveItem(action, backend);

                case REMOVE_ITEM:
                    return executeRemoveItem(action, backend);

                case EQUIP_ITEM:
                    return executeEquipItem(action, backend);

                case GET_PLAYER_EQUIPMENT:
                    return executeGetEquipment(action, backend, stateManager);

                case CLEAR_INVENTORY:
                    return executeClearInventory(action, backend);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    private ActionResult executeGetInventory(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Object inventory = null;
        if (isMineflayer(backend)) {
            inventory = asMineflayer(backend).getPlayerInventory(action.getPlayer());
        } else if (isRcon(backend)) {
            inventory = asRcon(backend).getPlayerInventory(action.getPlayer());
        }

        String response = serializeToJson(inventory);

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), inventory);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeGiveItem(Action action, PilafBackend backend) {
        backend.giveItem(action.getPlayer(), action.getItem(), action.getCount());
        return ActionResult.success("Gave " + action.getCount() + "x " + action.getItem() + " to " + action.getPlayer());
    }

    private ActionResult executeRemoveItem(Action action, PilafBackend backend) throws Exception {
        if (isMineflayer(backend)) {
            asMineflayer(backend).removeItem(action.getPlayer(), action.getItem(), action.getCount());
        } else if (isRcon(backend)) {
            asRcon(backend).removeItem(action.getPlayer(), action.getItem(), action.getCount());
        }
        return ActionResult.success("Removed " + action.getCount() + "x " + action.getItem() + " from " + action.getPlayer());
    }

    private ActionResult executeEquipItem(Action action, PilafBackend backend) {
        backend.equipItem(action.getPlayer(), action.getItem(), action.getSlot());
        return ActionResult.success("Equipped " + action.getItem() + " to " + action.getPlayer());
    }

    private ActionResult executeGetEquipment(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        Object equipment = null;
        if (isMineflayer(backend)) {
            equipment = asMineflayer(backend).getPlayerEquipment(action.getPlayer());
        } else if (isRcon(backend)) {
            equipment = asRcon(backend).getPlayerEquipment(action.getPlayer());
        }

        String response = serializeToJson(equipment);

        if (action.getStoreAs() != null) {
            stateManager.store(action.getStoreAs(), equipment);
        }

        return ActionResult.success(response);
    }

    private ActionResult executeClearInventory(Action action, PilafBackend backend) throws Exception {
        String result = null;
        if (isMineflayer(backend)) {
            result = asMineflayer(backend).executeRconWithCapture("clear " + action.getPlayer());
        } else if (isRcon(backend)) {
            result = asRcon(backend).executeRconWithCapture("clear " + action.getPlayer());
        }
        return ActionResult.success(result != null ? result : "Inventory cleared");
    }
}
