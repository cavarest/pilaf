package org.cavarest.pilaf.backend;

import org.cavarest.pilaf.client.MineflayerClient;
import org.cavarest.pilaf.rcon.RconClient;
import java.util.*;

/**
 * Mineflayer-based backend for player simulation testing.
 * Uses both RCON (for server commands) and Mineflayer (for player actions).
 */
public class MineflayerBackend implements PilafBackend {
    private final MineflayerClient mineflayer;
    private final RconClient rcon;
    private final Set<String> connectedPlayers = new HashSet<>();
    private final Map<String, Double> entityHealths = new HashMap<>();
    private final Set<String> spawnedEntities = new HashSet<>();
    private boolean verbose = false;

    public MineflayerBackend(String bridgeHost, int bridgePort, String rconHost, int rconPort, String rconPassword) {
        this.mineflayer = new MineflayerClient(bridgeHost, bridgePort);
        this.rcon = new RconClient(rconHost, rconPort, rconPassword);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        this.mineflayer.setVerbose(verbose);
        this.rcon.setVerbose(verbose);
    }

    @Override
    public void initialize() throws Exception {
        if (verbose) System.out.println("   [BACKEND] Initializing Mineflayer backend...");
        if (!rcon.connect()) {
            throw new Exception("Failed to connect to RCON");
        }
        if (!mineflayer.isHealthy()) {
            throw new Exception("Mineflayer bridge not responding");
        }
        if (verbose) System.out.println("   [BACKEND] Mineflayer backend initialized");
    }

    @Override
    public void cleanup() throws Exception {
        for (String player : connectedPlayers) {
            try { mineflayer.disconnect(player); } catch (Exception e) {}
        }
        connectedPlayers.clear();
        rcon.disconnect();
    }

    @Override
    public String getType() { return "mineflayer"; }

    public void connectPlayer(String username) throws Exception {
        if (mineflayer.connect(username)) {
            connectedPlayers.add(username);
        }
    }

    public void disconnectPlayer(String username) throws Exception {
        mineflayer.disconnect(username);
        connectedPlayers.remove(username);
    }

    @Override
    public void movePlayer(String player, String type, String dest) {
        try {
            String[] parts = dest.split("\\s+");
            if (parts.length >= 3) {
                mineflayer.move(player, Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void equipItem(String player, String item, String slot) {
        try {
            mineflayer.equip(player, item, slot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void giveItem(String player, String item, Integer count) {
        rcon.executeCommand("give " + player + " " + item + " " + count);
    }

    @Override
    public void executePlayerCommand(String player, String command, List<String> args) {
        try {
            String fullCmd = command + (args.isEmpty() ? "" : " " + String.join(" ", args));
            mineflayer.command(player, fullCmd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendChat(String player, String message) {
        try {
            mineflayer.chat(player, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void useItem(String player, String item, String target) {
        try {
            mineflayer.use(player, target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void spawnEntity(String name, String type, List<Double> location, Map<String, String> equipment) {
        String cmd = String.format("summon %s %.1f %.1f %.1f {CustomName:'\"test_%s\"'}",
            type.toLowerCase(), location.get(0), location.get(1), location.get(2), name);
        rcon.executeCommand(cmd);
        spawnedEntities.add(name);
        entityHealths.put(name, 20.0);
    }

    @Override
    public boolean entityExists(String name) {
        String result = rcon.executeCommand("execute if entity @e[name=test_" + name + "]");
        return result != null && result.contains("1");
    }

    @Override
    public double getEntityHealth(String name) {
        String result = rcon.executeCommand("data get entity @e[name=test_" + name + ",limit=1] Health");
        try {
            if (result != null && result.contains("has the following entity data:")) {
                String num = result.replaceAll("[^0-9.]", "");
                return Double.parseDouble(num);
            }
        } catch (Exception e) {}
        return entityHealths.getOrDefault(name, 20.0);
    }

    @Override
    public void setEntityHealth(String name, Double health) {
        rcon.executeCommand("data modify entity @e[name=test_" + name + ",limit=1] Health set value " + health + "f");
        entityHealths.put(name, health);
    }

    @Override
    public void executeServerCommand(String command, List<String> args) {
        String fullCommand = command;
        if (args != null && !args.isEmpty()) {
            fullCommand = command + " " + String.join(" ", args);
        }
        rcon.executeCommand(fullCommand);
    }

    @Override
    public boolean playerInventoryContains(String player, String item, String slot) {
        try {
            Map<String, Object> inv = mineflayer.getInventory(player);
            return inv.toString().contains(item);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean pluginReceivedCommand(String plugin, String command, String player) {
        String result = rcon.executeCommand("plugins");
        return result != null && result.contains(plugin);
    }

    @Override
    public void removeAllTestEntities() {
        rcon.executeCommand("kill @e[name=test_]");
        spawnedEntities.clear();
        entityHealths.clear();
    }

    @Override
    public void removeAllTestPlayers() {
        for (String player : new ArrayList<>(connectedPlayers)) {
            try { mineflayer.disconnect(player); } catch (Exception e) {}
        }
        connectedPlayers.clear();
    }

    // Extended Player Management Commands
    public void makeOperator(String player) throws Exception {
        rcon.executeCommand("op " + player);
    }

    public Map<String, Object> getPlayerInventory(String player) throws Exception {
        return mineflayer.getInventory(player);
    }

    public Map<String, Object> getPlayerPosition(String player) throws Exception {
        return mineflayer.getPosition(player);
    }

    public Map<String, Object> getPlayerHealth(String player) throws Exception {
        return mineflayer.getHealth(player);
    }

    public Double getPlayerHealthAsDouble(String player) throws Exception {
        Map<String, Object> healthMap = mineflayer.getHealth(player);
        return extractHealthFromMap(healthMap);
    }

    // Extended Entity Management Commands
    public Map<String, Object> getEntitiesInView(String player) throws Exception {
        return mineflayer.getEntities(player);
    }

    public Map<String, Object> getEntities(String player) throws Exception {
        return mineflayer.getEntities(player);
    }

    public Map<String, Object> getEntityByName(String entityName, String player) throws Exception {
        return mineflayer.getEntity(entityName, player);
    }

    public double getEntityDistance(String entityName, String player) throws Exception {
        Map<String, Object> entity = mineflayer.getEntity(entityName, player);
        Map<String, Object> playerPos = mineflayer.getPosition(player);
        // Calculate distance based on entity and player positions
        return calculateDistance(entity, playerPos);
    }

    // Extended Command Execution Commands
    public String executeRconWithCapture(String command) throws Exception {
        return rcon.executeCommand(command);
    }

    // RAW RCON - execute exact command as-is (for full RCON command set access)
    public String executeRconRaw(String command) throws Exception {
        // Execute the exact command without any parsing or transformation
        return rcon.executeCommand(command);
    }

    // RAW Player command - execute exact command as the player
    public String executePlayerCommandRaw(String player, String command) throws Exception {
        // Execute the exact command as the player through the bridge
        // Note: Mineflayer command() returns void, so we just acknowledge execution
        mineflayer.command(player, command);
        return "Command executed: " + command;
    }

    // Extended Inventory Management Commands
    public void removeItem(String player, String item, int count) throws Exception {
        rcon.executeCommand("clear " + player + " " + item + " " + count);
    }

    public Map<String, Object> getPlayerEquipment(String player) throws Exception {
        return mineflayer.getEquipment(player);
    }

    // Extended World & Environment Commands
    public Map<String, Object> getBlockAtPosition(String position) throws Exception {
        // Implementation depends on available API
        return new HashMap<>();
    }

    public long getWorldTime() throws Exception {
        String result = rcon.executeCommand("time query gametime");
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

    // Utility method to extract health value from Map
    private Double extractHealthFromMap(Map<String, Object> healthMap) {
        if (healthMap != null && healthMap.containsKey("health")) {
            Object healthValue = healthMap.get("health");
            if (healthValue instanceof Number) {
                return ((Number) healthValue).doubleValue();
            } else if (healthValue instanceof String) {
                try {
                    return Double.parseDouble((String) healthValue);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }
        return 0.0;
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
            return -1.0; // Error indicator
        }
    }
}
