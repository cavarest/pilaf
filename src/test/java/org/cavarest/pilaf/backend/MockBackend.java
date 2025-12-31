package org.cavarest.pilaf.backend;

import java.util.*;

/**
 * Mock backend for testing PILAF commands
 */
public class MockBackend implements PilafBackend {
    private boolean initialized = false;
    private final Map<String, Object> mockData = new HashMap<>();

    @Override
    public void initialize() throws Exception {
        initialized = true;
        // Mock some initial data
        mockData.put("entities_in_view", Map.of("entities", List.of("zombie", "skeleton")));
        mockData.put("player_inventory", Map.of("items", List.of("dragon_egg", "sword")));
        System.out.println("    🔧 MockBackend initialized");
    }

    @Override
    public void cleanup() throws Exception {
        initialized = false;
        mockData.clear();
        System.out.println("    🧹 MockBackend cleaned up");
    }

    @Override
    public String getType() { return "mock"; }

    @Override
    public void movePlayer(String player, String type, String dest) {
        System.out.println("    📍 Mock: Move player " + player + " to " + dest);
    }

    @Override
    public void equipItem(String player, String item, String slot) {
        System.out.println("    ⚔️ Mock: Equip " + item + " to " + player + "'s " + slot);
    }

    @Override
    public void giveItem(String player, String item, Integer count) {
        System.out.println("    🎁 Mock: Give " + count + "x " + item + " to " + player);
    }

    @Override
    public void executePlayerCommand(String player, String command, List<String> args) {
        System.out.println("    🎮 Mock: Player " + player + " executes: " + command);
    }

    @Override
    public void sendChat(String player, String message) {
        System.out.println("    💬 Mock: Chat from " + player + ": " + message);
    }

    @Override
    public void useItem(String player, String item, String target) {
        System.out.println("    🖱️ Mock: " + player + " uses " + item + " on " + target);
    }

    @Override
    public void spawnEntity(String name, String type, List<Double> location, Map<String, String> equipment) {
        System.out.println("    🧟 Mock: Spawn " + type + " named '" + name + "'");
        mockData.put("entities_in_view", Map.of("entities", List.of("zombie", "skeleton", name)));
    }

    @Override
    public boolean entityExists(String name) {
        System.out.println("    ❓ Mock: Check if entity '" + name + "' exists");
        return mockData.get("entities_in_view").toString().contains(name);
    }

    @Override
    public double getEntityHealth(String name) {
        System.out.println("    ❤️ Mock: Get health of entity '" + name + "'");
        return 20.0; // Mock full health
    }

    @Override
    public void setEntityHealth(String name, Double health) {
        System.out.println("    💉 Mock: Set health of '" + name + "' to " + health);
    }

    @Override
    public void executeServerCommand(String command, List<String> args) {
        System.out.println("    ⚙️ Mock: Server command: " + command + " " + String.join(" ", args));
    }

    @Override
    public boolean playerInventoryContains(String player, String item, String slot) {
        System.out.println("    🔍 Mock: Check if " + player + " has " + item + " in " + slot);
        return true; // Mock always true
    }

    @Override
    public boolean pluginReceivedCommand(String plugin, String command, String player) {
        System.out.println("    🔌 Mock: Plugin " + plugin + " received command from " + player);
        return true; // Mock always true
    }

    @Override
    public void removeAllTestEntities() {
        System.out.println("    🗑️ Mock: Remove all test entities");
        mockData.put("entities_in_view", Map.of("entities", List.of()));
    }

    @Override
    public void removeAllTestPlayers() {
        System.out.println("    👋 Mock: Remove all test players");
    }

    // Mock implementations of new methods
    public void makeOperator(String player) {
        System.out.println("    👑 Mock: Make " + player + " an operator");
    }

    public Map<String, Object> getPlayerInventory(String player) {
        System.out.println("    🎒 Mock: Get inventory of " + player);
        return (Map<String, Object>) mockData.getOrDefault("player_inventory", Map.of());
    }

    public Map<String, Object> getPlayerPosition(String player) {
        System.out.println("    📍 Mock: Get position of " + player);
        return Map.of("x", 0.0, "y", 64.0, "z", 0.0);
    }

    public double getPlayerHealth(String player) {
        System.out.println("    ❤️ Mock: Get health of " + player);
        return 20.0;
    }

    public Map<String, Object> getEntitiesInView(String player) {
        System.out.println("    👁️ Mock: Get entities in view of " + player);
        return (Map<String, Object>) mockData.getOrDefault("entities_in_view", Map.of());
    }

    public String executeRconWithCapture(String command) {
        System.out.println("    📋 Mock: Execute RCON with capture: " + command);
        return "mock_response_" + System.currentTimeMillis();
    }

    public void removeItem(String player, String item, int count) {
        System.out.println("    🗑️ Mock: Remove " + count + "x " + item + " from " + player);
    }

    public long getWorldTime() {
        System.out.println("    🕐 Mock: Get world time");
        return 12000L; // Noon
    }

    public String getWeather() {
        System.out.println("    ☀️ Mock: Get weather");
        return "clear";
    }
}
