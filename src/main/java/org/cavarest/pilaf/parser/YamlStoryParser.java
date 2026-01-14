package org.cavarest.pilaf.parser;

import org.cavarest.pilaf.model.*;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.*;

public class YamlStoryParser {
    private final Yaml yaml = new Yaml();

    public TestStory parseFromClasspath(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
        return parse(is);
    }

    public TestStory parse(InputStream is) { return parseStory(yaml.load(is)); }
    public TestStory parseString(String content) { return parseStory(yaml.load(content)); }

    @SuppressWarnings("unchecked")
    private TestStory parseStory(Map<String, Object> data) {
        TestStory story = new TestStory();
        story.setName((String) data.get("name"));
        story.setDescription((String) data.get("description"));
        story.setBackend((String) data.getOrDefault("backend", "mineflayer"));

        // Parse configuration section if present
        if (data.containsKey("config")) {
            Map<String, Object> config = (Map<String, Object>) data.get("config");
            // Store configuration in story for potential use by executor
            story.setBackend((String) config.getOrDefault("backend", story.getBackend()));
        }

        // Parse setup actions
        if (data.containsKey("setup")) {
            List<Map<String, Object>> setupActions = (List<Map<String, Object>>) data.get("setup");
            for (Map<String, Object> actionData : setupActions) {
                Action action = parseAction(actionData);
                if (action != null) {
                    story.addSetupAction(action);
                }
            }
        }

        // Parse story steps
        if (data.containsKey("steps")) {
            List<Map<String, Object>> steps = (List<Map<String, Object>>) data.get("steps");
            for (Map<String, Object> stepData : steps) {
                Action action = parseAction(stepData);
                if (action != null) {
                    story.addStep(action);
                }
            }
        }

        // Parse cleanup actions
        if (data.containsKey("cleanup")) {
            List<Map<String, Object>> cleanupActions = (List<Map<String, Object>>) data.get("cleanup");
            for (Map<String, Object> actionData : cleanupActions) {
                Action action = parseAction(actionData);
                if (action != null) {
                    story.addCleanupAction(action);
                }
            }
        }

        // Parse assertions
        if (data.containsKey("assertions")) {
            List<Map<String, Object>> assertionList = (List<Map<String, Object>>) data.get("assertions");
            for (Map<String, Object> assertionData : assertionList) {
                Assertion assertion = parseAssertion(assertionData);
                if (assertion != null) {
                    story.addAssertion(assertion);
                }
            }
        }

        // Parse assertions
        if (data.containsKey("assertions")) {
            List<Map<String, Object>> assertionList = (List<Map<String, Object>>) data.get("assertions");
            for (Map<String, Object> assertionData : assertionList) {
                Assertion assertion = parseAssertion(assertionData);
                if (assertion != null) {
                    story.addAssertion(assertion);
                }
            }
        }

        return story;
    }

    @SuppressWarnings("unchecked")
    private Action parseAction(Map<String, Object> data) {
        Action action = new Action();

        // Handle different action type specifications
        String actionType = null;
        if (data.containsKey("action")) {
            actionType = (String) data.get("action");
        } else if (data.containsKey("type")) {
            String type = (String) data.get("type");
            // Special case: if type is "server_action", use the server_action field as the actual action type
            if ("server_action".equals(type) && data.containsKey("server_action")) {
                actionType = (String) data.get("server_action");
            } else {
                actionType = type;
            }
        } else if (data.containsKey("server_action")) {
            actionType = (String) data.get("server_action");
        }

        if (actionType != null) {
            action.setType(parseActionType(actionType));
        }

        // Set basic properties
        action.setName((String) data.get("name"));
        action.setPlayer((String) data.get("player"));
        action.setEntity((String) data.get("entity"));
        action.setEntityType((String) data.get("entityType"));
        action.setEntityType((String) data.get("type")); // Support both "type" and "entityType"
        action.setItem((String) data.get("item"));
        action.setSlot((String) data.get("slot"));
        action.setCommand((String) data.get("command"));
        action.setPattern((String) data.get("pattern"));
        action.setDestination((String) data.get("destination"));

        // Set new action properties for extended functionality
        // Support both camelCase and snake_case naming conventions
        action.setCustomName(getString(data, "customName"));
        action.setVariableName(getString(data, "variableName", "variable_name"));
        action.setSourceVariable(getString(data, "sourceVariable", "source_variable"));
        action.setJsonPath(getString(data, "jsonPath", "json_path"));
        action.setFilterType((String) data.get("filterType"));
        action.setFilterValue((String) data.get("filterValue"));
        action.setSource((String) data.get("source"));
        action.setContains((String) data.get("contains"));
        action.setNegated((String) data.get("negated"));
        action.setState1((String) data.get("state1"));
        action.setState2((String) data.get("state2"));
        action.setFromCommandResult(getString(data, "fromCommandResult", "from_command_result"));
        action.setFromComparison(getString(data, "fromComparison", "from_comparison"));
        action.setPlugin((String) data.get("plugin"));
        action.setDamage((String) data.get("damage"));
        action.setPosition((String) data.get("position"));
        action.setFormat((String) data.get("format"));
        action.setStoreAs((String) data.getOrDefault("storeAs", data.get("store_as")));
        action.setTimeout((String) data.get("timeout"));
        action.setMessage((String) data.get("message"));

        // Handle count
        if (data.containsKey("count")) {
            Number count = (Number) data.get("count");
            action.setCount(count != null ? count.intValue() : 1);
        }

        // Handle duration (in milliseconds) - YAML values are already in milliseconds
        if (data.containsKey("wait") || data.containsKey("duration")) {
            Number duration = (Number) data.getOrDefault("wait", data.get("duration"));
            if (duration != null) {
                // All duration values in YAML are in milliseconds
                action.setDuration(duration.longValue());
            }
        }

        // Handle location coordinates
        if (data.containsKey("location")) {
            List<Double> location = new ArrayList<>();
            Object locObj = data.get("location");
            if (locObj instanceof List) {
                for (Object coord : (List<?>) locObj) {
                    if (coord instanceof Number) {
                        location.add(((Number) coord).doubleValue());
                    }
                }
            }
            action.setLocation(location);
        }

        // Handle command arguments
        if (data.containsKey("arguments") || data.containsKey("args")) {
            List<String> args = new ArrayList<>();
            Object argsObj = data.getOrDefault("arguments", data.get("args"));
            if (argsObj instanceof List) {
                for (Object arg : (List<?>) argsObj) {
                    args.add(arg != null ? arg.toString() : "");
                }
            }
            action.setArgs(args);
        }

        // Handle equipment mapping
        if (data.containsKey("equipment")) {
            action.setEquipment((Map<String, String>) data.get("equipment"));
        }

        return action;
    }

    private Action.ActionType parseActionType(String type) {
        if (type == null) return null;

        // Handle exact YAML values first before normalization
        if (type.equals("server_execute_command")) {
            return Action.ActionType.EXECUTE_RCON_COMMAND;
        }
        if (type.equals("spawn_entity")) {
            return Action.ActionType.SPAWN_ENTITY;
        }
        if (type.equals("spawn_player")) {
            return Action.ActionType.SPAWN_ENTITY;
        }
        if (type.equals("give_item")) {
            return Action.ActionType.GIVE_ITEM;
        }
        if (type.equals("get_entity_health")) {
            return Action.ActionType.GET_ENTITY_HEALTH;
        }

        // Handle new Pilaf command types
        if (type.equals("make_operator")) return Action.ActionType.MAKE_OPERATOR;
        if (type.equals("get_player_inventory")) return Action.ActionType.GET_PLAYER_INVENTORY;
        if (type.equals("get_player_position")) return Action.ActionType.GET_PLAYER_POSITION;
        if (type.equals("get_player_health")) return Action.ActionType.GET_PLAYER_HEALTH;
        if (type.equals("send_chat_message")) return Action.ActionType.SEND_CHAT_MESSAGE;
        if (type.equals("get_entities_in_view")) return Action.ActionType.GET_ENTITIES_IN_VIEW;
        if (type.equals("get_entity_by_name")) return Action.ActionType.GET_ENTITY_BY_NAME;
        if (type.equals("get_entity_distance")) return Action.ActionType.GET_ENTITY_DISTANCE;
        if (type.equals("get_entity_health_info")) return Action.ActionType.GET_ENTITY_HEALTH_INFO;
        if (type.equals("execute_rcon_with_capture")) return Action.ActionType.EXECUTE_RCON_WITH_CAPTURE;
        if (type.equals("remove_item")) return Action.ActionType.REMOVE_ITEM;
        if (type.equals("get_player_equipment")) return Action.ActionType.GET_PLAYER_EQUIPMENT;
        if (type.equals("get_block_at_position")) return Action.ActionType.GET_BLOCK_AT_POSITION;
        if (type.equals("get_world_time")) return Action.ActionType.GET_WORLD_TIME;
        if (type.equals("get_weather")) return Action.ActionType.GET_WEATHER;
        if (type.equals("store_state")) return Action.ActionType.STORE_STATE;
        if (type.equals("print_stored_state")) return Action.ActionType.PRINT_STORED_STATE;
        if (type.equals("compare_states")) return Action.ActionType.COMPARE_STATES;
        if (type.equals("print_state_comparison")) return Action.ActionType.PRINT_STATE_COMPARISON;
        if (type.equals("extract_with_jsonpath")) return Action.ActionType.EXTRACT_WITH_JSONPATH;
        if (type.equals("filter_entities")) return Action.ActionType.FILTER_ENTITIES;
        if (type.equals("assert_entity_missing")) return Action.ActionType.ASSERT_ENTITY_MISSING;
        if (type.equals("assert_entity_exists")) return Action.ActionType.ASSERT_ENTITY_EXISTS;
        if (type.equals("assert_player_has_item")) return Action.ActionType.ASSERT_PLAYER_HAS_ITEM;
        if (type.equals("assert_response_contains")) return Action.ActionType.ASSERT_RESPONSE_CONTAINS;
        if (type.equals("assert_json_equals")) return Action.ActionType.ASSERT_JSON_EQUALS;
        if (type.equals("get_plugin_status")) return Action.ActionType.GET_PLUGIN_STATUS;
        if (type.equals("execute_plugin_command")) return Action.ActionType.EXECUTE_PLUGIN_COMMAND;
        if (type.equals("get_server_info")) return Action.ActionType.GET_SERVER_INFO;
        if (type.equals("wait_for_entity_spawn")) return Action.ActionType.WAIT_FOR_ENTITY_SPAWN;
        if (type.equals("wait_for_chat_message")) return Action.ActionType.WAIT_FOR_CHAT_MESSAGE;
        if (type.equals("clear_entities")) return Action.ActionType.CLEAR_ENTITIES;
        if (type.equals("damage_entity")) return Action.ActionType.DAMAGE_ENTITY;
        if (type.equals("heal_player")) return Action.ActionType.HEAL_PLAYER;

        // Map various action type aliases to our enum
        String normalizedType = type.toLowerCase().replace("_", "");

        switch (normalizedType) {
            case "connectplayer":
            case "connect":
                return Action.ActionType.CONNECT_PLAYER;

            case "disconnectplayer":
            case "disconnect":
                return Action.ActionType.DISCONNECT_PLAYER;

            case "executeplayercommand":
            case "playercommand":
            case "playercmd":
                return Action.ActionType.EXECUTE_PLAYER_COMMAND;

            case "executerconcommand":
            case "servercommand":
            case "servercmd":
            case "rcon":
            case "serverexecutecommand":
            case "serverexecute":
                return Action.ActionType.EXECUTE_RCON_COMMAND;

            case "getentities":
            case "entities":
                return Action.ActionType.GET_ENTITIES;

            case "getinventory":
            case "inventory":
                return Action.ActionType.GET_INVENTORY;

            case "spawnentity":
            case "spawn":
            case "spawnplayer":
            case "spawn_entity":
                return Action.ActionType.SPAWN_ENTITY;

            case "giveitem":
            case "give":
            case "give_item":
                return Action.ActionType.GIVE_ITEM;

            case "equipitem":
            case "equip":
                return Action.ActionType.EQUIP_ITEM;

            case "moveplayer":
            case "move":
                return Action.ActionType.MOVE_PLAYER;

            case "wait":
            case "sleep":
                return Action.ActionType.WAIT;

            case "removeentities":
                return Action.ActionType.REMOVE_ENTITIES;

            case "removeplayers":
                return Action.ActionType.REMOVE_PLAYERS;

            case "clearcooldown":
            case "cooldownclear":
                return Action.ActionType.CLEAR_COOLDOWN;

            case "setcooldown":
            case "cooldownset":
                return Action.ActionType.SET_COOLDOWN;

            default:
                // Handle the original enum names
                try {
                    return Action.ActionType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unknown action type: " + type);
                }
        }
    }

    private Assertion parseAssertion(Map<String, Object> data) {
        Assertion a = new Assertion();
        a.setType(parseAssertionType((String) data.get("type")));
        a.setEntity((String) data.get("entity"));
        a.setPlayer((String) data.get("player"));
        a.setItem((String) data.get("item"));
        a.setSlot((String) data.get("slot"));
        a.setPlugin((String) data.get("plugin"));
        a.setCommand((String) data.get("command"));
        a.setSource((String) data.get("source"));
        a.setContains((String) data.get("contains"));
        a.setExpectedJson((String) data.get("expected"));
        a.setCondition((String) data.get("condition"));
        a.setVariableName(getString(data, "variableName", "variable_name"));
        if (data.containsKey("condition")) a.setConditionType(parseCondition((String) data.get("condition")));
        if (data.containsKey("value")) a.setValue(((Number) data.get("value")).doubleValue());
        if (data.containsKey("expected")) {
            Object expectedVal = data.get("expected");
            if (expectedVal instanceof Boolean) {
                a.setExpected((Boolean) expectedVal);
            }
        }
        return a;
    }

    private Assertion.AssertionType parseAssertionType(String t) {
        if (t == null) return null;
        String normalizedType = t.toLowerCase().replace("_", "");

        // Handle new assertion types
        if (normalizedType.equals("assertentitymissing"))
            return Assertion.AssertionType.ASSERT_ENTITY_MISSING;
        if (normalizedType.equals("assertentityexists"))
            return Assertion.AssertionType.ENTITY_EXISTS;
        if (normalizedType.equals("assertplayerhasitem"))
            return Assertion.AssertionType.ASSERT_PLAYER_HAS_ITEM;
        if (normalizedType.equals("assertresponsecontains"))
            return Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS;
        if (normalizedType.equals("assertjsonequals"))
            return Assertion.AssertionType.ASSERT_JSON_EQUALS;
        if (normalizedType.equals("assertlogcontains"))
            return Assertion.AssertionType.ASSERT_LOG_CONTAINS;
        if (normalizedType.equals("assertcondition"))
            return Assertion.AssertionType.ASSERT_CONDITION;

        switch (normalizedType) {
            case "entity_health": return Assertion.AssertionType.ENTITY_HEALTH;
            case "player_inventory": return Assertion.AssertionType.PLAYER_INVENTORY;
            case "plugin_command": return Assertion.AssertionType.PLUGIN_COMMAND;
            default: throw new IllegalArgumentException("Unknown assertion: " + t);
        }
    }

    private Assertion.Condition parseCondition(String c) {
        switch (c.toLowerCase()) {
            case "equals": return Assertion.Condition.EQUALS;
            case "less_than": return Assertion.Condition.LESS_THAN;
            case "greater_than": return Assertion.Condition.GREATER_THAN;
            default: return Assertion.Condition.EQUALS;
        }
    }

    /**
     * Helper method to get string value from map, supporting both camelCase and snake_case keys.
     * Tries each key in order and returns the first non-null value.
     *
     * @param data the map to search
     * @param keys the keys to try, in order of preference
     * @return the first non-null value found, or null if all keys are absent
     */
    private String getString(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
}
