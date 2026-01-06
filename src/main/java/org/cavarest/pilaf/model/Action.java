package org.cavarest.pilaf.model;

import java.util.List;
import java.util.Map;

/**
 * Represents an action in a PILAF test story.
 * Actions are executed sequentially by the TestOrchestrator.
 */
public class Action {

    public enum ActionType {
        // Existing actions
        SPAWN_ENTITY,
        GIVE_ITEM,
        EQUIP_ITEM,
        PLAYER_COMMAND,
        SERVER_COMMAND,
        MOVE_PLAYER,
        WAIT,
        REMOVE_ENTITIES,
        REMOVE_PLAYERS,
        CONNECT_PLAYER,
        DISCONNECT_PLAYER,
        EXECUTE_PLAYER_COMMAND,
        EXECUTE_RCON_COMMAND,
        GET_ENTITIES,
        GET_INVENTORY,
        SET_ENTITY_HEALTH,
        GET_ENTITY_HEALTH,
        CHECK_SERVICE_HEALTH,
        CLEAR_COOLDOWN,
        SET_COOLDOWN,
        SPAWN_TARGET,

        // Player Management Commands
        MAKE_OPERATOR,
        GET_PLAYER_INVENTORY,
        GET_PLAYER_POSITION,
        GET_PLAYER_HEALTH,
        SEND_CHAT_MESSAGE,

        // Entity Management Commands
        GET_ENTITIES_IN_VIEW,
        GET_ENTITY_BY_NAME,
        GET_ENTITY_DISTANCE,
        GET_ENTITY_HEALTH_INFO,

        // Command Execution Commands
        EXECUTE_RCON_WITH_CAPTURE,
        EXECUTE_RCON_RAW,
        EXECUTE_PLAYER_RAW,

        // Inventory Management Commands
        REMOVE_ITEM,
        GET_PLAYER_EQUIPMENT,

        // World & Environment Commands
        GET_BLOCK_AT_POSITION,
        GET_WORLD_TIME,
        GET_WEATHER,

        // State Management Commands
        STORE_STATE,
        PRINT_STORED_STATE,
        COMPARE_STATES,
        PRINT_STATE_COMPARISON,

        // Data Extraction Commands
        EXTRACT_WITH_JSONPATH,
        FILTER_ENTITIES,

        // Assertion Commands
        ASSERT_ENTITY_MISSING,
        ASSERT_ENTITY_EXISTS,
        ASSERT_PLAYER_HAS_ITEM,
        ASSERT_RESPONSE_CONTAINS,
        ASSERT_JSON_EQUALS,

        // Plugin & Server Commands
        GET_PLUGIN_STATUS,
        EXECUTE_PLUGIN_COMMAND,
        GET_SERVER_INFO,

        // Utility Commands
        WAIT_FOR_ENTITY_SPAWN,
        WAIT_FOR_CHAT_MESSAGE,
        CLEAR_ENTITIES,
        DAMAGE_ENTITY,
        HEAL_PLAYER
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

    // New fields for extended functionality
    private String customName;
    private String variableName;
    private String sourceVariable;
    private String jsonPath;
    private String filterType;
    private String filterValue;
    private String source;
    private String contains;
    private String negated;
    private String state1;
    private String state2;
    private String fromCommandResult;
    private String fromComparison;
    private String plugin;
    private String damage;
    private String position;
    private String format;
    private String storeAs;
    private String timeout;
    private String message;

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

    // New getters and setters
    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public String getVariableName() { return variableName; }
    public void setVariableName(String variableName) { this.variableName = variableName; }

    public String getSourceVariable() { return sourceVariable; }
    public void setSourceVariable(String sourceVariable) { this.sourceVariable = sourceVariable; }

    public String getJsonPath() { return jsonPath; }
    public void setJsonPath(String jsonPath) { this.jsonPath = jsonPath; }

    public String getFilterType() { return filterType; }
    public void setFilterType(String filterType) { this.filterType = filterType; }

    public String getFilterValue() { return filterValue; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getContains() { return contains; }
    public void setContains(String contains) { this.contains = contains; }

    public String getNegated() { return negated; }
    public void setNegated(String negated) { this.negated = negated; }

    public String getState1() { return state1; }
    public void setState1(String state1) { this.state1 = state1; }

    public String getState2() { return state2; }
    public void setState2(String state2) { this.state2 = state2; }

    public String getFromCommandResult() { return fromCommandResult; }
    public void setFromCommandResult(String fromCommandResult) { this.fromCommandResult = fromCommandResult; }

    public String getFromComparison() { return fromComparison; }
    public void setFromComparison(String fromComparison) { this.fromComparison = fromComparison; }

    public String getPlugin() { return plugin; }
    public void setPlugin(String plugin) { this.plugin = plugin; }

    public String getDamage() { return damage; }
    public void setDamage(String damage) { this.damage = damage; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getStoreAs() { return storeAs; }
    public void setStoreAs(String storeAs) { this.storeAs = storeAs; }

    public String getTimeout() { return timeout; }
    public void setTimeout(String timeout) { this.timeout = timeout; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

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

    // Extended factory methods for new action types
    public static Action connectPlayer(String username) {
        Action action = new Action(ActionType.CONNECT_PLAYER);
        action.setPlayer(username);
        return action;
    }

    public static Action disconnectPlayer(String username) {
        Action action = new Action(ActionType.DISCONNECT_PLAYER);
        action.setPlayer(username);
        return action;
    }

    public static Action executePlayerCommand(String username, String command) {
        Action action = new Action(ActionType.EXECUTE_PLAYER_COMMAND);
        action.setPlayer(username);
        action.setCommand(command);
        return action;
    }

    public static Action executeRconCommand(String command) {
        Action action = new Action(ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand(command);
        return action;
    }

    public static Action executeRconRaw(String command) {
        Action action = new Action(ActionType.EXECUTE_RCON_RAW);
        action.setCommand(command);
        return action;
    }

    public static Action executePlayerRaw(String username, String command) {
        Action action = new Action(ActionType.EXECUTE_PLAYER_RAW);
        action.setPlayer(username);
        action.setCommand(command);
        return action;
    }

    public static Action getEntities(String username) {
        Action action = new Action(ActionType.GET_ENTITIES);
        action.setPlayer(username);
        return action;
    }

    public static Action getInventory(String username) {
        Action action = new Action(ActionType.GET_INVENTORY);
        action.setPlayer(username);
        return action;
    }

    public static Action clearCooldown(String username) {
        Action action = new Action(ActionType.CLEAR_COOLDOWN);
        action.setPlayer(username);
        return action;
    }

    public static Action setCooldown(String username, long duration) {
        Action action = new Action(ActionType.SET_COOLDOWN);
        action.setPlayer(username);
        action.setDuration(duration);
        return action;
    }
}
