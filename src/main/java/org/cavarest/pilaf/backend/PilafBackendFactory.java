package org.cavarest.pilaf.backend;

import org.cavarest.pilaf.config.TestPorts;

/**
 * Factory for creating PilafBackend instances.
 */
public class PilafBackendFactory {

    public static PilafBackend create(String type) {
        return create(type, TestPorts.DEFAULT_RCON_HOST, TestPorts.RCON_PORT, TestPorts.DEFAULT_RCON_PASSWORD);
    }

    public static PilafBackend create(String type, String host, int port, String password) {
        switch (type.toLowerCase()) {
            case "rcon":
            case "real-server":
                return new RconBackend(host, port, password);
            case "docker":
            case "docker-server":
                return new DockerServerBackend("1.21.5", host, port, password);
            case "headlessmc":
                return new HeadlessMcBackend("1.21.5", host, port, password, true, true);
            case "mineflayer":
            case "real-client":
                return new MineflayerBackend(TestPorts.DEFAULT_RCON_HOST, TestPorts.MINEFLAYER_API_PORT, host, port, password);
            default:
                throw new IllegalArgumentException("Unknown backend type: " + type);
        }
    }

    public static RconBackend createRcon(String host, int port, String password) {
        return new RconBackend(host, port, password);
    }

    public static MineflayerBackend createMineflayer(String bridgeHost, int bridgePort,
            String rconHost, int rconPort, String rconPassword) {
        return new MineflayerBackend(bridgeHost, bridgePort, rconHost, rconPort, rconPassword);
    }
}
