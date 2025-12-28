package org.cavarest.pilaf.backend;

/**
 * Factory for creating PilafBackend instances.
 */
public class PilafBackendFactory {

    public static PilafBackend create(String type) {
        return create(type, "localhost", 25575, "dragon123");
    }

    public static PilafBackend create(String type, String host, int port, String password) {
        switch (type.toLowerCase()) {
            case "rcon":
            case "real-server":
                return new RconBackend(host, port, password);
            default:
                throw new IllegalArgumentException("Unknown backend type: " + type);
        }
    }

    public static RconBackend createRcon(String host, int port, String password) {
        return new RconBackend(host, port, password);
    }
}
