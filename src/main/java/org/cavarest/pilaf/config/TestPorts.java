package org.cavarest.pilaf.config;

/**
 * Single source of truth for all test port configurations.
 *
 * Port assignments:
 * - Internal container ports (25565, 25575, 3000) - these are the ports inside containers
 * - External host ports (35115, 35125, 8888) - these are the ports exposed on the host machine
 *
 * This class defines the external ports that should be used for testing.
 * Internal ports are defined by the services themselves and should not change.
 */
public final class TestPorts {

    private TestPorts() {
        // Constants class - prevent instantiation
    }

    // ==================== EXTERNAL HOST PORTS ====================
    // These are the ports exposed on the host machine for testing

    /**
     * External Minecraft server port (host:container mapping)
     * Default: 35115:25565
     */
    public static final int MINECRAFT_PORT = 35115;

    /**
     * External RCON port (host:container mapping)
     * Default: 35125:25575
     */
    public static final int RCON_PORT = 35125;

    /**
     * External Mineflayer bridge API port (host:container mapping)
     * Default: 8888:3000
     */
    public static final int MINEFLAYER_API_PORT = 8888;

    // ==================== INTERNAL CONTAINER PORTS ====================
    // These are the ports inside the containers - should NOT change

    /**
     * Internal Minecraft server port inside container
     * This is the standard Minecraft port and should not change.
     */
    public static final int MINECRAFT_INTERNAL_PORT = 25565;

    /**
     * Internal RCON port inside container
     * This is the standard Minecraft RCON port and should not change.
     */
    public static final int RCON_INTERNAL_PORT = 25575;

    /**
     * Internal Mineflayer bridge API port inside container
     * This is the Node.js default and should not change.
     */
    public static final int MINEFLAYER_API_INTERNAL_PORT = 3000;

    // ==================== CONNECTION STRINGS ====================

    /**
     * Default RCON host for testing
     */
    public static final String DEFAULT_RCON_HOST = "localhost";

    /**
     * Default Mineflayer bridge URL for testing
     */
    public static final String DEFAULT_MINEFLAYER_URL = "http://localhost:" + MINEFLAYER_API_PORT;

    /**
     * Default RCON password for testing
     */
    public static final String DEFAULT_RCON_PASSWORD = "dragon123";

    /**
     * Gets the external RCON connection string (host:port)
     */
    public static String getRconAddress() {
        return DEFAULT_RCON_HOST + ":" + RCON_PORT;
    }

    /**
     * Gets the internal RCON connection string (host:port)
     * For use within Docker networks
     */
    public static String getInternalRconAddress(String hostname) {
        return hostname + ":" + RCON_INTERNAL_PORT;
    }
}
