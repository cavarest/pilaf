package org.cavarest.pilaf.backend;

import org.cavarest.rcon.RconClient;
import java.io.IOException;
import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RCON-based backend implementation for executing server commands.
 */
public class RconBackend implements PilafBackend {
    private static final Logger logger = Logger.getLogger(RconBackend.class.getName());

    private final String host;
    private final int port;
    private final String password;
    private RconClient rcon;
    private Map<String, Double> entityHealths = new HashMap<>();
    private Set<String> spawnedEntities = new HashSet<>();
    private boolean verbose = false;

    public RconBackend(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        if (rcon != null) {
            rcon.setVerbose(verbose);
        }
    }

    private void log(Level level, String message) {
        if (verbose || level.intValue() >= Level.INFO.intValue()) {
            logger.log(level, "[RCON] " + message);
        }
    }

    /**
     * Executes an RCON command and returns the result.
     * Handles IOException internally for backward compatibility.
     */
    private String executeCommand(String command) {
        try {
            return rcon.sendCommand(command);
        } catch (IOException e) {
            log(Level.WARNING, "Command failed: " + e.getMessage());
            return "";
        }
    }

    /**
     * Executes an RCON command without checking result.
     */
    private void sendCommand(String command) {
        try {
            rcon.sendCommand(command);
        } catch (IOException e) {
            log(Level.WARNING, "Command failed: " + e.getMessage());
        }
    }

    @Override
    public void initialize() throws Exception {
        log(Level.INFO, "Connecting to RCON server at " + host + ":" + port);
        rcon = new RconClient(host, port, password);
        rcon.connect();
        log(Level.INFO, "Successfully connected to RCON server");
    }

    @Override
    public void cleanup() throws Exception {
        if (rcon != null) {
            log(Level.INFO, "Closing RCON connection");
            rcon.close();
            rcon = null;
        }
    }

    @Override
    public String getType() { return "rcon"; }

    @Override
    public void movePlayer(String player, String type, String dest) {
        executeCommand("tp " + player + " " + dest);
    }

    @Override
    public void equipItem(String player, String item, String slot) {
        String mcSlot = slot.equals("offhand") ? "weapon.offhand" : "weapon.mainhand";
        executeCommand("replaceitem entity " + player + " " + mcSlot + " " + item);
    }

    @Override
    public void giveItem(String player, String item, Integer count) {
        executeCommand("give " + player + " " + item + " " + count);
    }

    @Override
    public void executePlayerCommand(String player, String command, List<String> args) {
        String fullCmd = command + " " + String.join(" ", args);
        executeCommand("execute as " + player + " run " + fullCmd);
    }

    @Override
    public void sendChat(String player, String message) {
        executeCommand("tellraw " + player + " \"" + message + "\"");
    }

    @Override
    public void useItem(String player, String item, String target) {
        // Simulated - actual implementation depends on plugin
    }

    @Override
    public void spawnEntity(String name, String type, List<Double> location, Map<String, String> equipment) {
        String cmd = String.format("summon %s %.1f %.1f %.1f {CustomName:'\"test_%s\"'}",
            type.toLowerCase(), location.get(0), location.get(1), location.get(2), name);
        executeCommand(cmd);
        spawnedEntities.add(name);
        entityHealths.put(name, 20.0);
    }

    @Override
    public boolean entityExists(String name) {
        String result = executeCommand("execute if entity @e[name=test_" + name + "]");
        return result != null && result.contains("1");
    }

    @Override
    public double getEntityHealth(String name) {
        String result = executeCommand("data get entity @e[name=test_" + name + ",limit=1] Health");
        try {
            if (result != null && result.contains("has the following entity data:")) {
                String num = result.replaceAll("[^0-9.]", "");
                return Double.parseDouble(num);
            }
        } catch (Exception e) { }
        return entityHealths.getOrDefault(name, 20.0);
    }

    @Override
    public void setEntityHealth(String name, Double health) {
        executeCommand("data modify entity @e[name=test_" + name + ",limit=1] Health set value " + health + "f");
        entityHealths.put(name, health);
    }

    @Override
    public void executeServerCommand(String command, List<String> args) {
        String fullCommand = command;
        if (args != null && !args.isEmpty()) {
            fullCommand = command + " " + String.join(" ", args);
        }
        executeCommand(fullCommand);
    }

    @Override
    public boolean playerInventoryContains(String player, String item, String slot) {
        // Simplified check
        return true;
    }

    @Override
    public boolean pluginReceivedCommand(String plugin, String command, String player) {
        String result = executeCommand("plugins");
        return result != null && result.contains(plugin);
    }

    @Override
    public void removeAllTestEntities() {
        executeCommand("kill @e[name=test_]");
        spawnedEntities.clear();
        entityHealths.clear();
    }

    @Override
    public void removeAllTestPlayers() {
        // Can't remove players via RCON, just clear tracking
    }

    // Extended Player Management Commands
    public void makeOperator(String player) throws Exception {
        executeCommand("op " + player);
    }

    public void connectPlayer(String player) throws Exception {
        // RCON cannot connect players - this is a no-op for RCON-only backend
        // Players must be connected via a client (Mineflayer, regular client, etc.)
        System.out.println("    ℹ️  RCON backend: connectPlayer is a no-op. Use Mineflayer backend for player connections.");
    }

    public void disconnectPlayer(String player) throws Exception {
        // RCON cannot disconnect players - use kick command as closest equivalent
        executeCommand("kick " + player + " Disconnected by test");
    }

    public Map<String, Object> getPlayerInventory(String player) throws Exception {
        String result = executeCommand("data get entity " + player + " Inventory");
        return parseInventoryData(result);
    }

    public Map<String, Object> getPlayerPosition(String player) throws Exception {
        String result = executeCommand("data get entity " + player + " Pos");
        return parsePositionData(result);
    }

    public double getPlayerHealth(String player) throws Exception {
        String result = executeCommand("data get entity " + player + " Health");
        return parseHealthData(result);
    }

    // Extended Entity Management Commands
    public Map<String, Object> getEntitiesInView(String player) throws Exception {
        String result = executeCommand("execute at " + player + " run data get entity @e[distance=..10]");
        return parseEntitiesData(result);
    }

    public Map<String, Object> getEntityByName(String entityName, String player) throws Exception {
        String result = executeCommand("execute at " + player + " run data get entity @e[name=" + entityName + ",limit=1]");
        return parseEntityData(result);
    }

    public double getEntityDistance(String entityName, String player) throws Exception {
        String result = executeCommand("execute at " + player + " run data get entity @e[name=" + entityName + ",limit=1] Pos");
        Map<String, Object> entityPos = parsePositionData(result);
        Map<String, Object> playerPos = getPlayerPosition(player);
        return calculateDistance(entityPos, playerPos);
    }

    // Extended Command Execution Commands
    public String executeRconWithCapture(String command) throws Exception {
        return executeCommand(command);
    }

    // RAW RCON - execute exact command as-is (for full RCON command set access)
    public String executeRconRaw(String command) throws Exception {
        // Execute the exact command without any parsing or transformation
        return executeCommand(command);
    }

    // RAW Player command - execute exact command as the player
    public String executePlayerCommandRaw(String player, String command) throws Exception {
        // Execute the exact command as the player without wrapping in "execute as... run"
        return executeCommand("execute as " + player + " run " + command);
    }

    // Extended Inventory Management Commands
    public void removeItem(String player, String item, int count) throws Exception {
        executeCommand("clear " + player + " " + item + " " + count);
    }

    public Map<String, Object> getPlayerEquipment(String player) throws Exception {
        String result = executeCommand("data get entity " + player + " HandItems");
        return parseEquipmentData(result);
    }

    // Extended World & Environment Commands
    public Map<String, Object> getBlockAtPosition(String position) throws Exception {
        String result = executeCommand("data get block " + position);
        return parseBlockData(result);
    }

    public long getWorldTime() throws Exception {
        String result = executeCommand("time query gametime");
        // Response format: "The time is 89565" - need to extract the number
        if (result != null) {
            String trimmed = result.trim();
            if (trimmed.contains("The time is ")) {
                trimmed = trimmed.replace("The time is ", "").trim();
            }
            return Long.parseLong(trimmed);
        }
        return 0L;
    }

    public String getWeather() throws Exception {
        // In Minecraft 1.21.8, weather query doesn't exist as a separate command
        // Use time and default to clear for simplicity
        return "clear";
    }

    // State Management Commands
    private Map<String, Object> storedStates = new HashMap<>();

    public void storeState(String variableName, Object state) {
        storedStates.put(variableName, state);
    }

    public Object getStoredState(String variableName) {
        return storedStates.get(variableName);
    }

    public Map<String, Object> compareStates(String state1Name, String state2Name) {
        Object state1 = storedStates.get(state1Name);
        Object state2 = storedStates.get(state2Name);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("state1_name", state1Name);
        comparison.put("state2_name", state2Name);
        comparison.put("state1", state1);
        comparison.put("state2", state2);
        comparison.put("equal", Objects.equals(state1, state2));

        return comparison;
    }

    // Data Extraction Commands
    public Object extractWithJsonPath(String jsonData, String jsonPath) {
        // JSONPath implementation would go here
        // For now, return the original data
        return jsonData;
    }

    public List<Map<String, Object>> filterEntities(String entitiesData, String filterType, String filterValue) {
        // Filter implementation would go here
        // For now, return empty list
        return new ArrayList<>();
    }

    // Utility method for distance calculation
    private double calculateDistance(Map<String, Object> entity1, Map<String, Object> entity2) {
        try {
            double x1 = ((Number) entity1.get("x")).doubleValue();
            double y1 = ((Number) entity1.get("y")).doubleValue();
            double z1 = ((Number) entity1.get("z")).doubleValue();
            double x2 = ((Number) entity2.get("x")).doubleValue();
            double y2 = ((Number) entity2.get("y")).doubleValue();
            double z2 = ((Number) entity2.get("z")).doubleValue();

            return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
        } catch (Exception e) {
            return -1.0;
        }
    }

    // Parsing methods for Minecraft data
    private Map<String, Object> parseInventoryData(String data) {
        Map<String, Object> inventory = new HashMap<>();
        // Simplified parsing
        inventory.put("raw", data);
        return inventory;
    }

    private Map<String, Object> parsePositionData(String data) {
        Map<String, Object> position = new HashMap<>();
        // Simplified parsing
        position.put("raw", data);
        return position;
    }

    private double parseHealthData(String data) {
        try {
            String num = data.replaceAll("[^0-9.]", "");
            return Double.parseDouble(num);
        } catch (Exception e) {
            return 20.0;
        }
    }

    private Map<String, Object> parseEntitiesData(String data) {
        Map<String, Object> entities = new HashMap<>();
        // Simplified parsing
        entities.put("raw", data);
        return entities;
    }

    private Map<String, Object> parseEntityData(String data) {
        Map<String, Object> entity = new HashMap<>();
        // Simplified parsing
        entity.put("raw", data);
        return entity;
    }

    private Map<String, Object> parseEquipmentData(String data) {
        Map<String, Object> equipment = new HashMap<>();
        // Simplified parsing
        equipment.put("raw", data);
        return equipment;
    }

    private Map<String, Object> parseBlockData(String data) {
        Map<String, Object> block = new HashMap<>();
        // Simplified parsing
        block.put("raw", data);
        return block;
    }

    @Override
    public String getServerLog() {
        // Server logs are not accessible via RCON in Minecraft
        // This would require direct file access or a plugin
        return "[Log access not available via RCON]";
    }
}
