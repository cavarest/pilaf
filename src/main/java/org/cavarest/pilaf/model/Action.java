package org.cavarest.pilaf.model;

import java.util.List;
import java.util.Map;

/**
 * Represents an action in a PILAF test story.
 * Actions are executed sequentially by the TestOrchestrator.
 */
public class Action {

    public enum ActionType {
        SPAWN_ENTITY,
        GIVE_ITEM,
        EQUIP_ITEM,
        PLAYER_COMMAND,
        SERVER_COMMAND,
        MOVE_PLAYER,
        WAIT,
        REMOVE_ENTITIES,
        REMOVE_PLAYERS
    }

    private ActionType type;
    private String name;
    private String player;
    private String entity;
    private String entityType;
    private String item;
    private String slot;
    private String command;
    private String pattern;
    private String destination;
    private Integer count;
    private Long duration;
    private List<Double> location;
    private List<String> args;
    private Map<String, String> equipment;

    public Action() {}

    public Action(ActionType type) {
        this.type = type;
    }

    // Getters and setters
    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlayer() { return player; }
    public void setPlayer(String player) { this.player = player; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public List<Double> getLocation() { return location; }
    public void setLocation(List<Double> location) { this.location = location; }

    public List<String> getArgs() { return args; }
    public void setArgs(List<String> args) { this.args = args; }

    public Map<String, String> getEquipment() { return equipment; }
    public void setEquipment(Map<String, String> equipment) { this.equipment = equipment; }

    @Override
    public String toString() {
        return "Action{type=" + type + ", name=" + name + ", player=" + player + "}";
    }

    // Static factory methods
    public static Action spawnEntity(String name, String type, List<Double> location) {
        Action action = new Action(ActionType.SPAWN_ENTITY);
        action.setName(name);
        action.setEntityType(type);
        action.setLocation(location);
        return action;
    }

    public static Action giveItem(String player, String item, int count) {
        Action action = new Action(ActionType.GIVE_ITEM);
        action.setPlayer(player);
        action.setItem(item);
        action.setCount(count);
        return action;
    }

    public static Action equipItem(String player, String item, String slot) {
        Action action = new Action(ActionType.EQUIP_ITEM);
        action.setPlayer(player);
        action.setItem(item);
        action.setSlot(slot);
        return action;
    }

    public static Action playerCommand(String player, String command, List<String> args) {
        Action action = new Action(ActionType.PLAYER_COMMAND);
        action.setPlayer(player);
        action.setCommand(command);
        action.setArgs(args);
        return action;
    }

    public static Action waitFor(long durationMs) {
        Action action = new Action(ActionType.WAIT);
        action.setDuration(durationMs);
        return action;
    }
}
