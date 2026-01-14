package org.cavarest.pilaf.model;

import java.util.List;
import java.util.Map;

/**
 * Represents an action in a Pilaf test story.
 * Actions are executed sequentially by the TestOrchestrator.
 */
public class Action {

    /**
     * Represents the type of action to be executed in a Pilaf test story.
     *
     * <p>Each action type corresponds to a specific operation that can be performed
     * on the Minecraft server, player, or test environment.
     */
    public enum ActionType {
        /**
         * Spawns an entity at a specified location.
         */
        SPAWN_ENTITY,

        /**
         * Gives an item to a player.
         */
        GIVE_ITEM,

        /**
         * Equips an item in a specific equipment slot.
         */
        EQUIP_ITEM,

        /**
         * Executes a command as a player.
         */
        PLAYER_COMMAND,

        /**
         * Executes a server command via RCON.
         */
        SERVER_COMMAND,

        /**
         * Moves a player to a specified location.
         */
        MOVE_PLAYER,

        /**
         * Waits for a specified duration.
         */
        WAIT,

        /**
         * Removes entities from the world.
         */
        REMOVE_ENTITIES,

        /**
         * Removes players from the server.
         */
        REMOVE_PLAYERS,

        /**
         * Connects a player to the server.
         */
        CONNECT_PLAYER,

        /**
         * Disconnects a player from the server.
         */
        DISCONNECT_PLAYER,

        /**
         * Executes a command as a specific player.
         */
        EXECUTE_PLAYER_COMMAND,

        /**
         * Executes an RCON command on the server.
         */
        EXECUTE_RCON_COMMAND,

        /**
         * Gets all entities visible to a player.
         */
        GET_ENTITIES,

        /**
         * Gets a player's inventory contents.
         */
        GET_INVENTORY,

        /**
         * Sets an entity's health value.
         */
        SET_ENTITY_HEALTH,

        /**
         * Gets an entity's current health.
         */
        GET_ENTITY_HEALTH,

        /**
         * Checks the health status of a service.
         */
        CHECK_SERVICE_HEALTH,

        /**
         * Clears a player's attack cooldown.
         */
        CLEAR_COOLDOWN,

        /**
         * Sets a player's attack cooldown duration.
         */
        SET_COOLDOWN,

        /**
         * Spawns a target entity for testing.
         */
        SPAWN_TARGET,

        /**
         * Makes a player a server operator.
         */
        MAKE_OPERATOR,

        /**
         * Gets a player's inventory contents.
         */
        GET_PLAYER_INVENTORY,

        /**
         * Gets a player's current position coordinates.
         */
        GET_PLAYER_POSITION,

        /**
         * Gets a player's current health value.
         */
        GET_PLAYER_HEALTH,

        /**
         * Sends a chat message from a player.
         */
        SEND_CHAT_MESSAGE,

        /**
         * Changes a player's game mode.
         */
        GAMEMODE_CHANGE,

        /**
         * Clears all items from a player's inventory.
         */
        CLEAR_INVENTORY,

        /**
         * Sets a player's spawn point location.
         */
        SET_SPAWN_POINT,

        /**
         * Teleports a player to specified coordinates.
         */
        TELEPORT_PLAYER,

        /**
         * Sets a player's health value.
         */
        SET_PLAYER_HEALTH,

        /**
         * Kills a player.
         */
        KILL_PLAYER,

        /**
         * Gets entities within a player's view range.
         */
        GET_ENTITIES_IN_VIEW,

        /**
         * Gets an entity by its custom name.
         */
        GET_ENTITY_BY_NAME,

        /**
         * Gets the distance between player and entity.
         */
        GET_ENTITY_DISTANCE,

        /**
         * Gets detailed health information for an entity.
         */
        GET_ENTITY_HEALTH_INFO,

        /**
         * Kills a specific entity.
         */
        KILL_ENTITY,

        /**
         * Executes an RCON command and captures the response.
         */
        EXECUTE_RCON_WITH_CAPTURE,

        /**
         * Executes a raw RCON command without processing.
         */
        EXECUTE_RCON_RAW,

        /**
         * Executes a raw player command without processing.
         */
        EXECUTE_PLAYER_RAW,

        /**
         * Removes a specific item from player inventory.
         */
        REMOVE_ITEM,

        /**
         * Gets a player's currently equipped items.
         */
        GET_PLAYER_EQUIPMENT,

        /**
         * Gets the block type at specified coordinates.
         */
        GET_BLOCK_AT_POSITION,

        /**
         * Gets the current world time.
         */
        GET_WORLD_TIME,

        /**
         * Gets the current weather conditions.
         */
        GET_WEATHER,

        /**
         * Sets the world time.
         */
        SET_TIME,

        /**
         * Sets the weather conditions.
         */
        SET_WEATHER,

        /**
         * Stores a state value in a variable.
         */
        STORE_STATE,

        /**
         * Prints a stored state value to console.
         */
        PRINT_STORED_STATE,

        /**
         * Compares two stored state values.
         */
        COMPARE_STATES,

        /**
         * Prints the result of a state comparison.
         */
        PRINT_STATE_COMPARISON,

        /**
         * Extracts data using JSONPath expression.
         */
        EXTRACT_WITH_JSONPATH,

        /**
         * Filters entities based on criteria.
         */
        FILTER_ENTITIES,

        /**
         * Asserts that an entity does not exist.
         */
        ASSERT_ENTITY_MISSING,

        /**
         * Asserts that an entity exists.
         */
        ASSERT_ENTITY_EXISTS,

        /**
         * Asserts that a player has a specific item.
         */
        ASSERT_PLAYER_HAS_ITEM,

        /**
         * Asserts that a response contains specific text.
         */
        ASSERT_RESPONSE_CONTAINS,

        /**
         * Asserts that JSON values are equal.
         */
        ASSERT_JSON_EQUALS,

        /**
         * Asserts that a log contains specific text.
         */
        ASSERT_LOG_CONTAINS,

        /**
         * Asserts that a condition is true.
         */
        ASSERT_CONDITION,

        /**
         * Gets the status of a plugin.
         */
        GET_PLUGIN_STATUS,

        /**
         * Executes a plugin-specific command.
         */
        EXECUTE_PLUGIN_COMMAND,

        /**
         * Gets server information and status.
         */
        GET_SERVER_INFO,

        /**
         * Waits for an entity to spawn.
         */
        WAIT_FOR_ENTITY_SPAWN,

        /**
         * Waits for a chat message to appear.
         */
        WAIT_FOR_CHAT_MESSAGE,

        /**
         * Clears all entities from the world.
         */
        CLEAR_ENTITIES,

        /**
         * Damages an entity by a specified amount.
         */
        DAMAGE_ENTITY,

        /**
         * Heals a player by a specified amount.
         */
        HEAL_PLAYER,

        /**
         * Makes a player look at specific coordinates.
         */
        LOOK_AT,

        /**
         * Makes a player jump.
         */
        JUMP,

        /**
         * Makes a player use an item.
         */
        USE_ITEM,

        /**
         * Makes a player attack an entity.
         */
        ATTACK_ENTITY,

        /**
         * Makes a player break a block.
         */
        BREAK_BLOCK,

        /**
         * Makes a player place a block.
         */
        PLACE_BLOCK,

        /**
         * Gets the chat message history.
         */
        GET_CHAT_HISTORY
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
    private Double value;  // For numeric values like health, time, etc.

    // Validation fields for response assertions
    private Boolean failOnError;
    private String expect;
    private String expectContains;
    private String expectMatches;
    private String expectNotContains;

    /**
     * Creates a new Action with no type specified.
     */
    public Action() {}

    /**
     * Creates a new Action with the specified type.
     *
     * @param type the action type
     */
    public Action(ActionType type) {
        this.type = type;
    }

    // Getters and setters
    /** @return the action type */
    public ActionType getType() { return type; }
    /** @param type the action type */
    public void setType(ActionType type) { this.type = type; }

    /** @return the action name */
    public String getName() { return name; }
    /** @param name the action name */
    public void setName(String name) { this.name = name; }

    /** @return the target player name */
    public String getPlayer() { return player; }
    /** @param player the target player name */
    public void setPlayer(String player) { this.player = player; }

    /** @return the target entity name */
    public String getEntity() { return entity; }
    /** @param entity the target entity name */
    public void setEntity(String entity) { this.entity = entity; }

    /** @return the entity type (e.g., "zombie", "skeleton") */
    public String getEntityType() { return entityType; }
    /** @param entityType the entity type */
    public void setEntityType(String entityType) { this.entityType = entityType; }

    /** @return the item name */
    public String getItem() { return item; }
    /** @param item the item name */
    public void setItem(String item) { this.item = item; }

    /** @return the inventory slot */
    public String getSlot() { return slot; }
    /** @param slot the inventory slot */
    public void setSlot(String slot) { this.slot = slot; }

    /** @return the command string */
    public String getCommand() { return command; }
    /** @param command the command string */
    public void setCommand(String command) { this.command = command; }

    /**
     * Gets the pattern for matching.
     *
     * @return the pattern string
     */
    public String getPattern() { return pattern; }

    /**
     * Sets the pattern for matching.
     *
     * @param pattern the new pattern string
     */
    public void setPattern(String pattern) { this.pattern = pattern; }

    /**
     * Gets the destination location.
     *
     * @return the destination location
     */
    public String getDestination() { return destination; }

    /**
     * Sets the destination location.
     *
     * @param destination the new destination location
     */
    public void setDestination(String destination) { this.destination = destination; }

    /**
     * Gets the count value.
     *
     * @return the count value
     */
    public Integer getCount() { return count; }

    /**
     * Sets the count value.
     *
     * @param count the new count value
     */
    public void setCount(Integer count) { this.count = count; }

    /**
     * Gets the duration in milliseconds.
     *
     * @return the duration in milliseconds
     */
    public Long getDuration() { return duration; }

    /**
     * Sets the duration in milliseconds.
     *
     * @param duration the new duration in milliseconds
     */
    public void setDuration(Long duration) { this.duration = duration; }

    /**
     * Gets the location coordinates.
     *
     * @return the location coordinates as a list of doubles
     */
    public List<Double> getLocation() { return location; }

    /**
     * Sets the location coordinates.
     *
     * @param location the new location coordinates
     */
    public void setLocation(List<Double> location) { this.location = location; }

    /**
     * Gets the command arguments.
     *
     * @return the command arguments as a list of strings
     */
    public List<String> getArgs() { return args; }

    /**
     * Sets the command arguments.
     *
     * @param args the new command arguments
     */
    public void setArgs(List<String> args) { this.args = args; }

    /**
     * Gets the equipment mapping.
     *
     * @return the equipment mapping of slot to item
     */
    public Map<String, String> getEquipment() { return equipment; }

    /**
     * Sets the equipment mapping.
     *
     * @param equipment the new equipment mapping
     */
    public void setEquipment(Map<String, String> equipment) { this.equipment = equipment; }

    // New getters and setters
    /** @return the custom entity name */
    public String getCustomName() { return customName; }
    /** @param customName the custom entity name */
    public void setCustomName(String customName) { this.customName = customName; }

    /** @return the variable name for state storage */
    public String getVariableName() { return variableName; }
    /** @param variableName the variable name for state storage */
    public void setVariableName(String variableName) { this.variableName = variableName; }

    /** @return the source variable name */
    public String getSourceVariable() { return sourceVariable; }
    /** @param sourceVariable the source variable name */
    public void setSourceVariable(String sourceVariable) { this.sourceVariable = sourceVariable; }

    /** @return the JSONPath expression */
    public String getJsonPath() { return jsonPath; }
    /** @param jsonPath the JSONPath expression */
    public void setJsonPath(String jsonPath) { this.jsonPath = jsonPath; }

    /** @return the filter type (e.g., "name", "type") */
    public String getFilterType() { return filterType; }
    /** @param filterType the filter type */
    public void setFilterType(String filterType) { this.filterType = filterType; }

    /** @return the filter value */
    public String getFilterValue() { return filterValue; }
    /** @param filterValue the filter value */
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }

    /** @return the source for assertion */
    public String getSource() { return source; }
    /** @param source the source for assertion */
    public void setSource(String source) { this.source = source; }

    /** @return the text to search for */
    public String getContains() { return contains; }
    /** @param contains the text to search for */
    public void setContains(String contains) { this.contains = contains; }

    /** @return whether assertion is negated */
    public String getNegated() { return negated; }
    /** @param negated whether assertion is negated */
    public void setNegated(String negated) { this.negated = negated; }

    /** @return the first state name for comparison */
    public String getState1() { return state1; }
    /** @param state1 the first state name for comparison */
    public void setState1(String state1) { this.state1 = state1; }

    /** @return the second state name for comparison */
    public String getState2() { return state2; }
    /** @param state2 the second state name for comparison */
    public void setState2(String state2) { this.state2 = state2; }

    /** @return the command result variable */
    public String getFromCommandResult() { return fromCommandResult; }
    /** @param fromCommandResult the command result variable */
    public void setFromCommandResult(String fromCommandResult) { this.fromCommandResult = fromCommandResult; }

    /** @return the comparison result variable */
    public String getFromComparison() { return fromComparison; }
    /** @param fromComparison the comparison result variable */
    public void setFromComparison(String fromComparison) { this.fromComparison = fromComparison; }

    /** @return the plugin name */
    public String getPlugin() { return plugin; }
    /** @param plugin the plugin name */
    public void setPlugin(String plugin) { this.plugin = plugin; }

    /** @return the damage value */
    public String getDamage() { return damage; }
    /** @param damage the damage value */
    public void setDamage(String damage) { this.damage = damage; }

    /** @return the position string */
    public String getPosition() { return position; }
    /** @param position the position string */
    public void setPosition(String position) { this.position = position; }

    /** @return the format string */
    public String getFormat() { return format; }
    /** @param format the format string */
    public void setFormat(String format) { this.format = format; }

    /** @return the variable name to store result as */
    public String getStoreAs() { return storeAs; }
    /** @param storeAs the variable name to store result as */
    public void setStoreAs(String storeAs) { this.storeAs = storeAs; }

    /** @return the timeout value */
    public String getTimeout() { return timeout; }
    /** @param timeout the timeout value */
    public void setTimeout(String timeout) { this.timeout = timeout; }

    /** @return the message text */
    public String getMessage() { return message; }
    /** @param message the message text */
    public void setMessage(String message) { this.message = message; }

    /** @return the numeric value */
    public Double getValue() { return value; }
    /** @param value the numeric value */
    public void setValue(Double value) { this.value = value; }

    // Validation field getters and setters
    /** @return whether to fail on error detection */
    public Boolean getFailOnError() { return failOnError; }
    /** @param failOnError whether to fail on error detection */
    public void setFailOnError(Boolean failOnError) { this.failOnError = failOnError; }

    /** @return the expected exact response */
    public String getExpect() { return expect; }
    /** @param expect the expected exact response */
    public void setExpect(String expect) { this.expect = expect; }

    /** @return the expected substring in response */
    public String getExpectContains() { return expectContains; }
    /** @param expectContains the expected substring in response */
    public void setExpectContains(String expectContains) { this.expectContains = expectContains; }

    /** @return the expected regex pattern for response */
    public String getExpectMatches() { return expectMatches; }
    /** @param expectMatches the expected regex pattern for response */
    public void setExpectMatches(String expectMatches) { this.expectMatches = expectMatches; }

    /** @return the expected substring that should NOT be in response */
    public String getExpectNotContains() { return expectNotContains; }
    /** @param expectNotContains the expected substring that should NOT be in response */
    public void setExpectNotContains(String expectNotContains) { this.expectNotContains = expectNotContains; }

    @Override
    public String toString() {
        return "Action{type=" + type + ", name=" + name + ", player=" + player + "}";
    }

    // Static factory methods
    /**
     * Creates an action to spawn an entity at a specific location.
     *
     * @param name the entity's custom name
     * @param type the entity type (e.g., "zombie", "skeleton")
     * @param location the spawn location coordinates [x, y, z]
     * @return the configured spawn entity action
     */
    public static Action spawnEntity(String name, String type, List<Double> location) {
        Action action = new Action(ActionType.SPAWN_ENTITY);
        action.setName(name);
        action.setEntityType(type);
        action.setLocation(location);
        return action;
    }

    /**
     * Creates an action to give an item to a player.
     *
     * @param player the player name
     * @param item the item type (e.g., "diamond_sword")
     * @param count the number of items to give
     * @return the configured give item action
     */
    public static Action giveItem(String player, String item, int count) {
        Action action = new Action(ActionType.GIVE_ITEM);
        action.setPlayer(player);
        action.setItem(item);
        action.setCount(count);
        return action;
    }

    /**
     * Creates an action to equip an item in a specific equipment slot.
     *
     * @param player the player name
     * @param item the item type to equip
     * @param slot the equipment slot (e.g., "hand", "head", "chest")
     * @return the configured equip item action
     */
    public static Action equipItem(String player, String item, String slot) {
        Action action = new Action(ActionType.EQUIP_ITEM);
        action.setPlayer(player);
        action.setItem(item);
        action.setSlot(slot);
        return action;
    }

    /**
     * Creates an action to execute a command as a player.
     *
     * @param player the player name
     * @param command the command to execute
     * @param args the command arguments
     * @return the configured player command action
     */
    public static Action playerCommand(String player, String command, List<String> args) {
        Action action = new Action(ActionType.PLAYER_COMMAND);
        action.setPlayer(player);
        action.setCommand(command);
        action.setArgs(args);
        return action;
    }

    /**
     * Creates an action to wait for a specified duration.
     *
     * @param durationMs the duration to wait in milliseconds
     * @return the configured wait action
     */
    public static Action waitFor(long durationMs) {
        Action action = new Action(ActionType.WAIT);
        action.setDuration(durationMs);
        return action;
    }

    /**
     * Creates an action to connect a player to the server.
     *
     * @param username the player username
     * @return the configured connect player action
     */
    public static Action connectPlayer(String username) {
        Action action = new Action(ActionType.CONNECT_PLAYER);
        action.setPlayer(username);
        return action;
    }

    /**
     * Creates an action to disconnect a player from the server.
     *
     * @param username the player username
     * @return the configured disconnect player action
     */
    public static Action disconnectPlayer(String username) {
        Action action = new Action(ActionType.DISCONNECT_PLAYER);
        action.setPlayer(username);
        return action;
    }

    /**
     * Creates an action to execute a command as a specific player.
     *
     * @param username the player username
     * @param command the command to execute
     * @return the configured execute player command action
     */
    public static Action executePlayerCommand(String username, String command) {
        Action action = new Action(ActionType.EXECUTE_PLAYER_COMMAND);
        action.setPlayer(username);
        action.setCommand(command);
        return action;
    }

    /**
     * Creates an action to execute an RCON command on the server.
     *
     * @param command the RCON command to execute
     * @return the configured execute RCON command action
     */
    public static Action executeRconCommand(String command) {
        Action action = new Action(ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand(command);
        return action;
    }

    /**
     * Creates an action to execute a raw RCON command without processing.
     *
     * @param command the raw RCON command to execute
     * @return the configured execute RCON raw action
     */
    public static Action executeRconRaw(String command) {
        Action action = new Action(ActionType.EXECUTE_RCON_RAW);
        action.setCommand(command);
        return action;
    }

    /**
     * Creates an action to execute a raw command as a player without processing.
     *
     * @param username the player username
     * @param command the raw command to execute
     * @return the configured execute player raw action
     */
    public static Action executePlayerRaw(String username, String command) {
        Action action = new Action(ActionType.EXECUTE_PLAYER_RAW);
        action.setPlayer(username);
        action.setCommand(command);
        return action;
    }

    /**
     * Creates an action to get all entities visible to a player.
     *
     * @param username the player username
     * @return the configured get entities action
     */
    public static Action getEntities(String username) {
        Action action = new Action(ActionType.GET_ENTITIES);
        action.setPlayer(username);
        return action;
    }

    /**
     * Creates an action to get a player's inventory contents.
     *
     * @param username the player username
     * @return the configured get inventory action
     */
    public static Action getInventory(String username) {
        Action action = new Action(ActionType.GET_INVENTORY);
        action.setPlayer(username);
        return action;
    }

    /**
     * Creates an action to clear a player's attack cooldown.
     *
     * @param username the player username
     * @return the configured clear cooldown action
     */
    public static Action clearCooldown(String username) {
        Action action = new Action(ActionType.CLEAR_COOLDOWN);
        action.setPlayer(username);
        return action;
    }

    /**
     * Creates an action to set a player's attack cooldown duration.
     *
     * @param username the player username
     * @param duration the cooldown duration in milliseconds
     * @return the configured set cooldown action
     */
    public static Action setCooldown(String username, long duration) {
        Action action = new Action(ActionType.SET_COOLDOWN);
        action.setPlayer(username);
        action.setDuration(duration);
        return action;
    }
}
