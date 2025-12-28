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

    public MineflayerBackend(String bridgeHost, int bridgePort, String rconHost, int rconPort, String rconPassword) {
        this.mineflayer = new MineflayerClient(bridgeHost, bridgePort);
        this.rcon = new RconClient(rconHost, rconPort, rconPassword);
    }

    @Override
    public void initialize() throws Exception {
        if (!rcon.connect()) {
            throw new Exception("Failed to connect to RCON");
        }
        if (!mineflayer.isHealthy()) {
            throw new Exception("Mineflayer bridge not responding");
        }
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
        rcon.executeCommand(command + " " + String.join(" ", args));
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
}
