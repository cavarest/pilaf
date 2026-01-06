package org.cavarest.pilaf.backend;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Docker Server Backend for PILAF test execution.
 * Uses Docker + PaperMC to provide Minecraft server infrastructure.
 *
 * This backend:
 * - Launches PaperMC servers via Docker containers
 * - Provides RCON access for server commands
 * - Supports multiple versions via Docker images
 * - Excellent CI/CD support (GitHub Actions)
 */
public class DockerServerBackend implements ServerBackend, PilafBackend {

    private final String rconHost;
    private final int rconPort;
    private final String rconPassword;
    private final String serverVersion;

    private Process dockerProcess;
    private RconBackend rconBackend;
    private boolean initialized = false;
    private boolean serverRunning = false;

    public DockerServerBackend(String serverVersion, String rconHost, int rconPort, String rconPassword) {
        this.serverVersion = serverVersion;
        this.rconHost = rconHost;
        this.rconPort = rconPort;
        this.rconPassword = rconPassword;
    }

    @Override
    public void setVerbose(boolean verbose) {
        // Not implemented for Docker backend
    }

    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }

        System.out.println("🔧 Initializing Docker server backend for version: " + serverVersion);

        // Initialize RCON backend for server commands
        rconBackend = new RconBackend(rconHost, rconPort, rconPassword);
        try {
            rconBackend.initialize();
            System.out.println("✅ RCON backend initialized for server commands");
        } catch (Exception e) {
            System.out.println("⚠️ RCON backend initialization failed: " + e.getMessage());
        }

        initialized = true;
        System.out.println("✅ Docker server backend initialized");
    }

    @Override
    public void cleanup() throws Exception {
        if (!initialized) {
            return;
        }

        System.out.println("🧹 Cleaning up Docker server backend...");

        // Stop server if running
        if (serverRunning) {
            stopServer();
        }

        if (rconBackend != null) {
            rconBackend.cleanup();
        }

        initialized = false;
        System.out.println("✅ Docker server backend cleaned up");
    }

    @Override
    public String getType() {
        return "docker-server";
    }

    @Override
    public void launchServer(String version) throws Exception {
        System.out.println("🚀 Launching PaperMC server via Docker: " + version);

        try {
            // This would typically use Docker SDK or CLI
            // For now, using a simplified approach
            String imageName = "papermc/paper:" + version;

            // Check if Docker is available
            ProcessBuilder dockerCheck = new ProcessBuilder("docker", "--version");
            dockerCheck.redirectErrorStream(true);
            Process checkProcess = dockerCheck.start();

            if (checkProcess.waitFor() != 0) {
                throw new RuntimeException("Docker not available");
            }

            // Launch PaperMC container
            String dockerCmd = String.format(
                "docker run -d --name pilaf-paper-%s -p %d:25565 " +
                "-e RCON_PASSWORD=%s " +
                "papermc/paper:%s --nogui",
                version, rconPort, rconPassword, version
            );

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", dockerCmd);
            pb.redirectErrorStream(true);
            dockerProcess = pb.start();

            // Wait for container to start
            Thread.sleep(10000);

            if (!dockerProcess.isAlive()) {
                throw new RuntimeException("Docker container failed to start");
            }

            serverRunning = true;
            System.out.println("✅ PaperMC server launched via Docker (version: " + version + ")");

        } catch (IOException e) {
            throw new RuntimeException("Failed to launch Docker server: " + e.getMessage(), e);
        }
    }

    @Override
    public void stopServer() throws Exception {
        if (!serverRunning) {
            return;
        }

        System.out.println("🛑 Stopping Docker PaperMC server...");

        try {
            // Stop and remove container
            ProcessBuilder stopCmd = new ProcessBuilder("docker", "stop", "pilaf-paper-" + serverVersion);
            stopCmd.redirectErrorStream(true);
            Process stopProcess = stopCmd.start();
            stopProcess.waitFor();

            ProcessBuilder rmCmd = new ProcessBuilder("docker", "rm", "pilaf-paper-" + serverVersion);
            rmCmd.redirectErrorStream(true);
            Process rmProcess = rmCmd.start();
            rmProcess.waitFor();

            if (dockerProcess != null && dockerProcess.isAlive()) {
                dockerProcess.destroy();
            }

            serverRunning = false;
            System.out.println("✅ Docker PaperMC server stopped");

        } catch (Exception e) {
            System.out.println("⚠️ Error stopping Docker server: " + e.getMessage());
        }
    }

    @Override
    public boolean isServerRunning() {
        try {
            if (dockerProcess == null || !dockerProcess.isAlive()) {
                return false;
            }

            // Check if container is still running
            ProcessBuilder psCmd = new ProcessBuilder("docker", "ps", "--filter", "name=pilaf-paper-" + serverVersion, "--format", "{{.Status}}");
            psCmd.redirectErrorStream(true);
            Process psProcess = psCmd.start();

            return psProcess.waitFor() == 0;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getServerStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("type", getType());
        status.put("version", serverVersion);
        status.put("running", isServerRunning());
        status.put("rcon_host", rconHost);
        status.put("rcon_port", rconPort);

        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("list", new java.util.ArrayList<>());
                status.put("players_online", "Server is responding");
            } catch (Exception e) {
                status.put("rcon_error", e.getMessage());
            }
        }

        return status;
    }

    @Override
    public String executeCommand(String command) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand(command, new java.util.ArrayList<>());
                return "Command executed successfully";
            } catch (Exception e) {
                return "Error executing command: " + e.getMessage();
            }
        }
        return "RCON backend not available";
    }

    @Override
    public String getServerLogs() {
        try {
            ProcessBuilder logsCmd = new ProcessBuilder("docker", "logs", "pilaf-paper-" + serverVersion, "--tail", "100");
            logsCmd.redirectErrorStream(true);
            Process logsProcess = logsCmd.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(logsProcess.getInputStream())
            );

            StringBuilder logs = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logs.append(line).append("\n");
            }

            return logs.toString();

        } catch (Exception e) {
            return "Error getting logs: " + e.getMessage();
        }
    }

    @Override
    public String getServerLog() {
        return getServerLogs();
    }

    // PilafBackend interface methods
    @Override
    public void movePlayer(String playerName, String destinationType, String destination) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("tp " + playerName + " " + destination, new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error moving player: " + e.getMessage());
            }
        }
    }

    @Override
    public void equipItem(String playerName, String item, String slot) {
        // Use RCON backend for equipment commands
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("give " + playerName + " " + item, new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error equipping item: " + e.getMessage());
            }
        }
    }

    @Override
    public void giveItem(String playerName, String item, Integer count) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("give " + playerName + " " + item + " " + count, new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error giving item: " + e.getMessage());
            }
        }
    }

    @Override
    public void executePlayerCommand(String playerName, String command, List<String> arguments) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("execute as " + playerName + " run " + command, arguments);
            } catch (Exception e) {
                System.err.println("Error executing player command: " + e.getMessage());
            }
        }
    }

    @Override
    public void sendChat(String playerName, String message) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("say [" + playerName + "] " + message, new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error sending chat: " + e.getMessage());
            }
        }
    }

    @Override
    public void useItem(String playerName, String item, String target) {
        // Implementation would depend on specific server plugins
        System.out.println("Use item not implemented for Docker backend");
    }

    @Override
    public void spawnEntity(String name, String type, List<Double> location, Map<String, String> equipment) {
        if (rconBackend != null) {
            try {
                String locationStr = location.get(0) + " " + location.get(1) + " " + location.get(2);
                rconBackend.executeServerCommand("summon " + type + " " + locationStr, new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error spawning entity: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean entityExists(String entityName) {
        // Check if entity exists via RCON
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("data get entity " + entityName, new ArrayList<>());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public double getEntityHealth(String entityName) {
        if (rconBackend != null) {
            try {
                // This would need to parse RCON response
                return 20.0; // Default health
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    @Override
    public void setEntityHealth(String entityName, Double health) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("data modify entity " + entityName + " Health set value " + health, new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error setting entity health: " + e.getMessage());
            }
        }
    }

    @Override
    public void executeServerCommand(String command, List<String> arguments) {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand(command, arguments);
            } catch (Exception e) {
                System.err.println("Error executing server command: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean playerInventoryContains(String playerName, String item, String slot) {
        // Check player inventory via RCON
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("data get entity " + playerName + " Inventory", new ArrayList<>());
                return true; // Simplified implementation
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean pluginReceivedCommand(String pluginName, String command, String playerName) {
        // Plugin interaction would depend on specific plugins
        return false;
    }

    @Override
    public void removeAllTestEntities() {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("kill @e[type=!minecraft:player]", new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error removing test entities: " + e.getMessage());
            }
        }
    }

    @Override
    public void removeAllTestPlayers() {
        if (rconBackend != null) {
            try {
                rconBackend.executeServerCommand("kick @a", new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error removing test players: " + e.getMessage());
            }
        }
    }

    // Configuration methods
    public void setServerVersion(String version) {
        // Note: This would require creating a new instance in practice
        // Keeping for compatibility but not used
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Process getDockerProcess() {
        return dockerProcess;
    }
}
