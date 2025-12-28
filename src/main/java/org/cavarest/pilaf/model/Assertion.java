package org.cavarest.pilaf.model;

import org.cavarest.pilaf.backend.PilafBackend;

public class Assertion {
    public enum AssertionType { ENTITY_HEALTH, ENTITY_EXISTS, PLAYER_INVENTORY, PLUGIN_COMMAND }
    public enum Condition { EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUALS, GREATER_THAN_OR_EQUALS }

    private AssertionType type;
    private String entity, player, item, slot, plugin, command;
    private Condition condition;
    private Double value;
    private Boolean expected;

    public Assertion() {}
    public Assertion(AssertionType type) { this.type = type; }

    public AssertionType getType() { return type; }
    public void setType(AssertionType type) { this.type = type; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getPlayer() { return player; }
    public void setPlayer(String player) { this.player = player; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
    public String getPlugin() { return plugin; }
    public void setPlugin(String plugin) { this.plugin = plugin; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public Condition getCondition() { return condition; }
    public void setCondition(Condition condition) { this.condition = condition; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public Boolean getExpected() { return expected; }
    public void setExpected(Boolean expected) { this.expected = expected; }

    public boolean evaluate(PilafBackend backend) {
        switch (type) {
            case ENTITY_HEALTH: return compareValues(backend.getEntityHealth(entity), value, condition);
            case ENTITY_EXISTS: return backend.entityExists(entity) == expected;
            case PLAYER_INVENTORY: return backend.playerInventoryContains(player, item, slot) == expected;
            case PLUGIN_COMMAND: return backend.pluginReceivedCommand(plugin, command, player);
            default: return false;
        }
    }

    private boolean compareValues(double actual, double expected, Condition cond) {
        switch (cond) {
            case EQUALS: return actual == expected;
            case NOT_EQUALS: return actual != expected;
            case LESS_THAN: return actual < expected;
            case GREATER_THAN: return actual > expected;
            case LESS_THAN_OR_EQUALS: return actual <= expected;
            case GREATER_THAN_OR_EQUALS: return actual >= expected;
            default: return false;
        }
    }
}
