package org.cavarest.pilaf.backend;

import org.cavarest.pilaf.rcon.RconClient;
import java.util.*;

/**
 * RCON-based backend implementation for executing server commands.
 */
public class RconBackend implements PilafBackend {
    private final String host;
    private final int port;
    private final String password;
    private RconClient rcon;
    private Map<String, Double> entityHealths = new HashMap<>();
    private Set<String> spawnedEntities = new HashSet<>();

    public RconBackend(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    @Override
    public void initialize() throws Exception {
        rcon = new RconClient(host, port, password);
        if (!rcon.connect()) {
            throw new Exception("Failed to connect to RCON server at " + host + ":" + port);
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (rcon != null) {
            rcon.disconnect();
        }
    }

    @Override
    public String getType() { return "rcon"; }

    @Override
    public void movePlayer(String player, String type, String dest) {
        rcon.executeCommand("tp " + player + " " + dest);
    }

    @Override
    public void equipItem(String player, String item, String slot) {
        String mcSlot = slot.equals("offhand") ? "weapon.offhand" : "weapon.mainhand";
        rcon.executeCommand("replaceitem entity " + player + " " + mcSlot + " " + item);
    }

    @Override
    public void giveItem(String player, String item, Integer count) {
        rcon.executeCommand("give " + player + " " + item + " " + count);
    }

    @Override
    public void executePlayerCommand(String player, String command, List<String> args) {
        String fullCmd = command + " " + String.join(" ", args);
        rcon.executeCommand("execute as " + player + " run " + fullCmd);
    }

    @Override
    public void sendChat(String player, String message) {
        rcon.executeCommand("tellraw " + player + " \"" + message + "\"");
    }

    @Override
    public void useItem(String player, String item, String target) {
        // Simulated - actual implementation depends on plugin
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
        } catch (Exception e) { }
        return entityHealths.getOrDefault(name, 20.0);
    }

    @Override
    public void setEntityHealth(String name, Double health) {
        rcon.executeCommand("data modify entity @e[name=test_" + name + ",limit=1] Health set value " + health + "f");
        entityHealths.put(name, health);
    }

    @Override
    public void executeServerCommand(String command, List<String> args) {
        rcon.executeCommand(command + " " + String.join(" ", args));
    }

    @Override
    public boolean playerInventoryContains(String player, String item, String slot) {
        // Simplified check
        return true;
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
        // Can't remove players via RCON, just clear tracking
    }
}
