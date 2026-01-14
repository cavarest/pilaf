package org.cavarest.pilaf.config;

import org.cavarest.pilaf.client.MineflayerClient;
import org.cavarest.rcon.RconClient;
import java.io.IOException;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages all service connections for Pilaf tests.
 * Handles HTTP client, RCON client, and Mineflayer bridge connections with health checking.
 */
public class ConnectionManager {

    private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());

    private final TestConfiguration config;
    private final HttpClient httpClient;
    private final RconClient rconClient;
    private final MineflayerClient mineflayerClient;
    private final ConcurrentMap<String, Boolean> serviceHealth = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> connectedPlayers = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;

    public ConnectionManager(TestConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.getMineflayerTimeout() / 1000))
            .build();

        this.rconClient = new RconClient(
            config.getRconHost(),
            config.getRconPort(),
            config.getRconPassword()
        );

        this.mineflayerClient = new MineflayerClient(
            extractHost(config.getMineflayerUrl()),
            extractPort(config.getMineflayerUrl())
        );
    }

    /**
     * Initialize all connections and perform health checks.
     */
    public synchronized void initialize() throws Exception {
        if (initialized || shutdown) {
            return;
        }

        logger.info("Initializing Pilaf connections: " + config);

        // Test HTTP connectivity first
        if (!config.isSkipHealthChecks()) {
            testMineflayerHealth();
        }

        // Connect RCON
        try {
            rconClient.connect();
            logger.info("RCON connected to " + config.getRconHost() + ":" + config.getRconPort());
            serviceHealth.put("rcon", true);
        } catch (IOException e) {
            logger.warning("RCON not available: " + e.getMessage());
            serviceHealth.put("rcon", false);
        }

        // Test Mineflayer client
        try {
            if (mineflayerClient.isHealthy()) {
                logger.info("Mineflayer bridge healthy at " + config.getMineflayerUrl());
                serviceHealth.put("mineflayer", true);
            } else {
                logger.warning("Mineflayer bridge not responding");
                serviceHealth.put("mineflayer", false);
            }
        } catch (Exception e) {
            logger.warning("Mineflayer bridge error: " + e.getMessage());
            serviceHealth.put("mineflayer", false);
        }

        initialized = true;
    }

    /**
     * Connect a bot player to the server.
     * @param username the player username to connect
     * @throws Exception if connection fails
     */
    public void connectPlayer(String username) throws Exception {
        ensureInitialized();

        if (mineflayerClient.connect(username)) {
            connectedPlayers.put(username, username);
            logger.info("Player " + username + " connected");
        } else {
            throw new RuntimeException("Failed to connect player: " + username);
        }
    }

    /**
     * Disconnect a bot player from the server.
     * @param username the player username to disconnect
     */
    public void disconnectPlayer(String username) {
        if (!connectedPlayers.containsKey(username)) {
            return;
        }

        try {
            mineflayerClient.disconnect(username);
            connectedPlayers.remove(username);
            logger.info("Player " + username + " disconnected");
        } catch (Exception e) {
            logger.warning("Failed to disconnect player " + username + ": " + e.getMessage());
        }
    }

    /**
     * Execute a player command through Mineflayer.
     */
    public String executePlayerCommand(String username, String command) throws Exception {
        ensureInitialized();

        String body = "{\"username\":\"" + username + "\",\"command\":\"" + command + "\",\"waitForChat\":true,\"chatTimeout\":2000}";
        return httpPost(config.getMineflayerUrl() + "/command", body);
    }

    /**
     * Execute an RCON command on the server.
     */
    public String executeRconCommand(String command) throws Exception {
        ensureInitialized();

        if (Boolean.TRUE.equals(serviceHealth.get("rcon"))) {
            try {
                return rconClient.sendCommand(command);
            } catch (IOException e) {
                throw new RuntimeException("RCON command failed", e);
            }
        } else {
            throw new IllegalStateException("RCON not available");
        }
    }

    /**
     * Get entities visible to a player.
     */
    public String getEntities(String username) throws Exception {
        ensureInitialized();
        return httpGet(config.getMineflayerUrl() + "/entities/" + username);
    }

    /**
     * Get player inventory.
     */
    public String getInventory(String username) throws Exception {
        ensureInitialized();
        return httpGet(config.getMineflayerUrl() + "/inventory/" + username);
    }

    /**
     * Check if all required services are healthy.
     */
    public boolean areServicesHealthy() {
        if (config.isSkipHealthChecks()) {
            return true;
        }

        // For integration tests, we need both RCON and Mineflayer
        return Boolean.TRUE.equals(serviceHealth.get("rcon")) &&
               Boolean.TRUE.equals(serviceHealth.get("mineflayer"));
    }

    /**
     * Check if a specific service is healthy.
     */
    public boolean isServiceHealthy(String service) {
        return Boolean.TRUE.equals(serviceHealth.get(service));
    }

    /**
     * Get the connected player names.
     */
    public String[] getConnectedPlayers() {
        return connectedPlayers.keySet().toArray(new String[0]);
    }

    /**
     * Cleanup all connections.
     */
    public synchronized void cleanup() {
        if (shutdown) {
            return;
        }

        logger.info("Cleaning up Pilaf connections");

        // Disconnect all players
        for (String player : new java.util.ArrayList<>(connectedPlayers.keySet())) {
            disconnectPlayer(player);
        }

        // Close RCON connection
        try {
            if (rconClient != null) {
                rconClient.close();
            }
        } catch (IOException e) {
            logger.warning("Error closing RCON: " + e.getMessage());
        }

        // Clear state
        serviceHealth.clear();
        connectedPlayers.clear();
        shutdown = true;
        initialized = false;
    }

    // HTTP helper methods
    private String httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String httpPost(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private void testMineflayerHealth() throws Exception {
        String healthUrl = config.getMineflayerUrl() + "/health";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(healthUrl))
            .timeout(Duration.ofMillis(config.getHealthCheckTimeout()))
            .GET()
            .build();

        CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response = future.get(config.getHealthCheckTimeout(), TimeUnit.MILLISECONDS);

        if (response.statusCode() != 200) {
            throw new Exception("Mineflayer health check failed: " + response.statusCode());
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ConnectionManager not initialized. Call initialize() first.");
        }
    }

    private String extractHost(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return "localhost";
        }
    }

    private int extractPort(String url) {
        try {
            URI uri = new URI(url);
            int port = uri.getPort();
            return port == -1 ? 80 : port;
        } catch (Exception e) {
            return 3000;
        }
    }
}
