package org.cavarest.pilaf.backend;

import java.util.List;
import java.util.Map;

/**
 * Client Backend Interface
 * Provides client control and server instrumentation
 */
public interface ClientBackend {
    /**
     * Initialize the client backend.
     */
    void initialize() throws Exception;

    /**
     * Cleanup and disconnect.
     */
    void cleanup() throws Exception;

    /**
     * Get the backend type identifier.
     */
    String getType();

    /**
     * Connect a player to the server.
     */
    void connectPlayer(String username) throws Exception;

    /**
     * Disconnect a player from the server.
     */
    void disconnectPlayer(String username) throws Exception;

    /**
     * Move player to coordinates.
     */
    void movePlayer(String playerName, String destinationType, String destination);

    /**
     * Equip item for player.
     */
    void equipItem(String playerName, String item, String slot);

    /**
     * Give item to player.
     */
    void giveItem(String playerName, String item, Integer count);

    /**
     * Execute player command.
     */
    void executePlayerCommand(String playerName, String command, List<String> arguments);

    /**
     * Send chat message as player.
     */
    void sendChat(String playerName, String message);

    /**
     * Use item.
     */
    void useItem(String playerName, String item, String target);

    /**
     * Spawn entity.
     */
    void spawnEntity(String name, String type, List<Double> location, Map<String, String> equipment);

    /**
     * Check if entity exists.
     */
    boolean entityExists(String entityName);

    /**
     * Get entity health.
     */
    double getEntityHealth(String entityName);

    /**
     * Set entity health.
     */
    void setEntityHealth(String entityName, Double health);

    /**
     * Check player inventory.
     */
    boolean playerInventoryContains(String playerName, String item, String slot);

    /**
     * Get player information.
     */
    Map<String, Object> getPlayerInventory(String playerName) throws Exception;
    Map<String, Object> getPlayerPosition(String playerName) throws Exception;
    Map<String, Object> getPlayerHealth(String playerName) throws Exception;

    /**
     * Get entities information.
     */
    Map<String, Object> getEntities(String playerName) throws Exception;
    Map<String, Object> getEntitiesInView(String playerName) throws Exception;
    Map<String, Object> getEntityByName(String entityName, String playerName) throws Exception;

    /**
     * Execute RCON command with capture.
     */
    String executeRconWithCapture(String command) throws Exception;

    /**
     * Remove items/entities.
     */
    void removeItem(String playerName, String item, int count) throws Exception;
    void removeAllTestEntities();
    void removeAllTestPlayers();

    /**
     * Check if backend is healthy/connected.
     */
    boolean isHealthy();
}
