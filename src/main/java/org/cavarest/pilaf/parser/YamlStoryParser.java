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
        story.setBackend((String) data.getOrDefault("backend", "real-server"));

        if (data.containsKey("setup"))
            for (Map<String, Object> a : (List<Map<String, Object>>) data.get("setup"))
                story.addSetupAction(parseAction(a));
        if (data.containsKey("steps"))
            for (Map<String, Object> a : (List<Map<String, Object>>) data.get("steps"))
                story.addStep(parseAction(a));
        if (data.containsKey("assertions"))
            for (Map<String, Object> a : (List<Map<String, Object>>) data.get("assertions"))
                story.addAssertion(parseAssertion(a));
        if (data.containsKey("cleanup"))
            for (Map<String, Object> a : (List<Map<String, Object>>) data.get("cleanup"))
                story.addCleanupAction(parseAction(a));
        return story;
    }

    @SuppressWarnings("unchecked")
    private Action parseAction(Map<String, Object> data) {
        Action action = new Action();
        action.setType(parseActionType((String) data.get("action")));
        action.setName((String) data.get("name"));
        action.setPlayer((String) data.get("player"));
        action.setEntityType((String) data.get("type"));
        action.setItem((String) data.get("item"));
        action.setSlot((String) data.get("slot"));
        action.setCommand((String) data.get("command"));
        action.setPattern((String) data.get("pattern"));
        action.setDestination((String) data.get("destination"));
        if (data.containsKey("count")) action.setCount(((Number) data.get("count")).intValue());
        if (data.containsKey("duration")) action.setDuration(((Number) data.get("duration")).longValue());
        if (data.containsKey("location")) {
            List<Double> loc = new ArrayList<>();
            for (Number n : (List<Number>) data.get("location")) loc.add(n.doubleValue());
            action.setLocation(loc);
        }
        if (data.containsKey("args")) action.setArgs((List<String>) data.get("args"));
        if (data.containsKey("equipment")) action.setEquipment((Map<String, String>) data.get("equipment"));
        return action;
    }

    private Action.ActionType parseActionType(String type) {
        if (type == null) return null;
        switch (type.toLowerCase()) {
            case "spawn_entity": return Action.ActionType.SPAWN_ENTITY;
            case "give_item": return Action.ActionType.GIVE_ITEM;
            case "equip_item": return Action.ActionType.EQUIP_ITEM;
            case "player_command": return Action.ActionType.PLAYER_COMMAND;
            case "server_command": return Action.ActionType.SERVER_COMMAND;
            case "move_player": return Action.ActionType.MOVE_PLAYER;
            case "wait": return Action.ActionType.WAIT;
            case "remove_entities": return Action.ActionType.REMOVE_ENTITIES;
            case "remove_players": return Action.ActionType.REMOVE_PLAYERS;
            default: throw new IllegalArgumentException("Unknown action: " + type);
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
        if (data.containsKey("condition")) a.setCondition(parseCondition((String) data.get("condition")));
        if (data.containsKey("value")) a.setValue(((Number) data.get("value")).doubleValue());
        if (data.containsKey("expected")) a.setExpected((Boolean) data.get("expected"));
        return a;
    }

    private Assertion.AssertionType parseAssertionType(String t) {
        switch (t.toLowerCase()) {
            case "entity_health": return Assertion.AssertionType.ENTITY_HEALTH;
            case "entity_exists": return Assertion.AssertionType.ENTITY_EXISTS;
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
}
