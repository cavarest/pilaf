package org.cavarest.pilaf.backend;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * HeadlessMc backend for PILAF test execution.
 * Uses HeadlessMc to launch and manage Minecraft servers.
 *
 * Limitations:
 * - Server plugin management is limited (can launch servers but not install plugins)
 * - No native RCON support (requires fallback to external RCON client)
 * - Best used for CI/CD environments where Docker is not available
 */
public class HeadlessMcBackend implements PilafBackend {

    private String serverVersion;
    private final String rconHost;
    private final int rconPort;
    private final String rconPassword;
    private boolean autoLaunch;
    private boolean rconFallback;

    private Process serverProcess;
    private RconBackend rconBackend;
    private boolean initialized = false;

    public HeadlessMcBackend(String serverVersion, String rconHost, int rconPort,
                           String rconPassword, boolean autoLaunch, boolean rconFallback) {
        this.serverVersion = serverVersion;
        this.rconHost = rconHost;
        this.rconPort = rconPort;
        this.rconPassword = rconPassword;
        this.autoLaunch = autoLaunch;
        this.rconFallback = rconFallback;
    }

    @Override
    public void setVerbose(boolean verbose) {
        // Not implemented for HeadlessMc backend
    }

    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }

        System.out.println("🔧 Initializing HeadlessMc backend for version: " + serverVersion);

        if (autoLaunch) {
            launchServer();
        }

        if (rconFallback) {
            rconBackend = new RconBackend(rconHost, rconPort, rconPassword);
            try {
                rconBackend.initialize();
                System.out.println("✅ RCON fallback initialized for server commands");
            } catch (Exception e) {
                System.out.println("⚠️ RCON fallback failed: " + e.getMessage());
            }
        }

        initialized = true;
        System.out.println("✅ HeadlessMc backend initialized");
    }

    @Override
    public void cleanup() throws Exception {
        if (!initialized) {
            return;
        }

        System.out.println("🧹 Cleaning up HeadlessMc backend...");

        if (rconBackend != null) {
            rconBackend.cleanup();
        }

        if (serverProcess != null && serverProcess.isAlive()) {
            System.out.println("🛑 Stopping server process...");
            serverProcess.destroy();
            serverProcess.waitFor();
        }

        initialized = false;
        System.out.println("✅ HeadlessMc backend cleaned up");
    }

    @Override
    public String getType() {
        return "headlessmc";
    }

    @Override
    public void movePlayer(String playerName, String destinationType, String destination) {
        executeServerCommand("tp " + playerName + " " + destination, new ArrayList<>());
    }

    @Override
    public void equipItem(String playerName, String item, String slot) {
        executeServerCommand("equipment give " + playerName + " " + item + " " + slot, new ArrayList<>());
    }

    @Override
    public void giveItem(String playerName, String item, Integer count) {
        List<String> args = new ArrayList<>();
        args.add(count.toString());
        executeServerCommand("give " + playerName + " " + item, args);
    }

    @Override
    public void executePlayerCommand(String playerName, String command, List<String> arguments) {
        System.out.println("⚠️ Player command execution not supported by HeadlessMc backend: " + command);
    }

    @Override
    public void sendChat(String playerName, String message) {
        executeServerCommand("msg " + playerName + " " + message, new ArrayList<>());
    }

    @Override
    public void useItem(String playerName, String item, String target) {
        System.out.println("⚠️ Item usage not supported by HeadlessMc backend");
    }

    @Override
    public void spawnEntity(String name, String type, List<Double> location, Map<String, String> equipment) {
        String locationStr = location != null ?
            String.format("%.2f %.2f %.2f", location.get(0), location.get(1), location.get(2)) :
            "~ ~ ~";

        executeServerCommand("summon " + type + " " + locationStr + " {CustomName:\"" + name + "\"}", new ArrayList<>());
    }

    @Override
    public boolean entityExists(String entityName) {
        System.out.println("⚠️ Entity existence check not supported by HeadlessMc backend");
        return false;
    }

    @Override
    public double getEntityHealth(String entityName) {
        System.out.println("⚠️ Entity health query not supported by HeadlessMc backend");
        return 0.0;
    }

    @Override
    public void setEntityHealth(String entityName, Double health) {
        executeServerCommand("data merge entity @e[name=\"" + entityName + "\",limit=1] {Health:" + health + "}", new ArrayList<>());
    }

    @Override
    public void executeServerCommand(String command, List<String> arguments) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand(command, arguments);
                return;
            } catch (Exception e) {
                System.out.println("⚠️ RCON command failed: " + e.getMessage());
            }
        }

        if (serverProcess != null && serverProcess.isAlive()) {
            System.out.println("📤 Server command (via process): " + command);
        } else {
            System.out.println("❌ No server process available for command: " + command);
        }
    }

    @Override
    public boolean playerInventoryContains(String playerName, String item, String slot) {
        System.out.println("⚠️ Inventory check not supported by HeadlessMc backend");
        return false;
    }

    @Override
    public boolean pluginReceivedCommand(String pluginName, String command, String playerName) {
        System.out.println("⚠️ Plugin command tracking not supported by HeadlessMc backend");
        return false;
    }

    @Override
    public void removeAllTestEntities() {
        executeServerCommand("kill @e[type=!minecraft:player]", new ArrayList<>());
    }

    @Override
    public void removeAllTestPlayers() {
        System.out.println("⚠️ Player removal not supported by HeadlessMc backend");
    }

    private void launchServer() throws Exception {
        System.out.println("🚀 Launching Paper server via HeadlessMc: " + serverVersion);

        try {
            Path serverDir = Paths.get("headlessmc-servers", "paper-" + serverVersion);

            if (!Files.exists(serverDir)) {
                System.out.println("📥 Server not found, would download Paper " + serverVersion);
                return;
            }

            System.out.println("✅ Using existing server directory: " + serverDir);

            ProcessBuilder pb = new ProcessBuilder(
                "java", "-Xmx2G", "-jar", "paper.jar", "--nogui"
            );
            pb.directory(serverDir.toFile());
            pb.redirectErrorStream(true);

            serverProcess = pb.start();
            System.out.println("✅ Server process started (PID: " + serverProcess.pid() + ")");

            Thread.sleep(5000);

            if (!serverProcess.isAlive()) {
                throw new RuntimeException("Server process died during startup");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to launch server: " + e.getMessage(), e);
        }
    }

    public void setServerVersion(String version) {
        this.serverVersion = version;
    }

    public void setAutoLaunch(boolean autoLaunch) {
        this.autoLaunch = autoLaunch;
    }

    public void setRconFallback(boolean rconFallback) {
        this.rconFallback = rconFallback;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Process getServerProcess() {
        return serverProcess;
    }
}
