package org.cavarest.pilaf.backend;

import java.util.List;
import java.util.Map;

/**
 * Backend interface for PILAF test execution.
 * Implementations provide different ways to interact with Minecraft servers.
 */
public interface PilafBackend {

    /**
     * Initialize the backend connection.
     */
    void initialize() throws Exception;

    /**
     * Cleanup and close connections.
     */
    void cleanup() throws Exception;

    /**
     * Get the backend type identifier.
     */
    String getType();

    // Player actions
    void movePlayer(String playerName, String destinationType, String destination);
    void equipItem(String playerName, String item, String slot);
    void giveItem(String playerName, String item, Integer count);
    void executePlayerCommand(String playerName, String command, List<String> arguments);
    void sendChat(String playerName, String message);
    void useItem(String playerName, String item, String target);

    // Entity management
    void spawnEntity(String name, String type, List<Double> location, Map<String, String> equipment);
    boolean entityExists(String entityName);
    double getEntityHealth(String entityName);
    void setEntityHealth(String entityName, Double health);

    // Server commands
    void executeServerCommand(String command, List<String> arguments);

    // Inventory checks
    boolean playerInventoryContains(String playerName, String item, String slot);

    // Plugin interaction
    boolean pluginReceivedCommand(String pluginName, String command, String playerName);

    // Cleanup helpers
    void removeAllTestEntities();
    void removeAllTestPlayers();
}
