package org.cavarest.pilaf.backend;

import java.util.List;
import java.util.Map;

/**
 * Server Backend Interface
 * Provides Minecraft server infrastructure (PaperMC servers)
 */
public interface ServerBackend {
    /**
     * Initialize the server backend.
     */
    void initialize() throws Exception;

    /**
     * Cleanup and shutdown the server.
     */
    void cleanup() throws Exception;

    /**
     * Get the backend type identifier.
     */
    String getType();

    /**
     * Launch a Minecraft server with specified version.
     */
    void launchServer(String version) throws Exception;

    /**
     * Stop the running server.
     */
    void stopServer() throws Exception;

    /**
     * Check if server is running.
     */
    boolean isServerRunning();

    /**
     * Get server status information.
     */
    Map<String, Object> getServerStatus();

    /**
     * Execute server command (RCON or direct).
     */
    String executeCommand(String command);

    /**
     * Get server logs.
     */
    String getServerLogs();
}
