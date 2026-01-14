package org.cavarest.pilaf.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DockerServerBackend
 * Focuses on methods that don't require actual Docker infrastructure.
 */
@DisplayName("DockerServerBackend Tests")
class DockerServerBackendTest {

    private DockerServerBackend backend;

    @BeforeEach
    void setUp() {
        backend = new DockerServerBackend("1.20.4", "localhost", 25575, "password");
    }

    // CONSTRUCTOR TESTS

    @Test
    @DisplayName("constructor creates DockerServerBackend with provided parameters")
    void testConstructor() {
        DockerServerBackend b = new DockerServerBackend("1.20.4", "localhost", 25575, "password");
        assertNotNull(b);
        assertFalse(b.isInitialized());
    }

    @Test
    @DisplayName("constructor with different server version")
    void testConstructor_differentVersion() {
        DockerServerBackend b = new DockerServerBackend("1.19.2", "localhost", 25575, "password");
        assertNotNull(b);
    }

    @Test
    @DisplayName("constructor with different host")
    void testConstructor_differentHost() {
        DockerServerBackend b = new DockerServerBackend("1.20.4", "example.com", 25575, "password");
        assertNotNull(b);
    }

    @Test
    @DisplayName("constructor with different port")
    void testConstructor_differentPort() {
        DockerServerBackend b = new DockerServerBackend("1.20.4", "localhost", 12345, "password");
        assertNotNull(b);
    }

    @Test
    @DisplayName("constructor with different password")
    void testConstructor_differentPassword() {
        DockerServerBackend b = new DockerServerBackend("1.20.4", "localhost", 25575, "secret");
        assertNotNull(b);
    }

    // GETTYPE TESTS

    @Test
    @DisplayName("getType returns 'docker-server'")
    void testGetType() {
        assertEquals("docker-server", backend.getType());
    }

    // ISINITIALIZED TESTS

    @Test
    @DisplayName("isInitialized returns false for new backend")
    void testIsInitialized_newBackend() {
        assertFalse(backend.isInitialized());
    }

    // GETDOCKERPROCESS TESTS

    @Test
    @DisplayName("getDockerProcess returns null before initialization")
    void testGetDockerProcess_notInitialized() {
        assertNull(backend.getDockerProcess());
    }

    // SETSERVERVERSION TESTS

    @Test
    @DisplayName("setServerVersion does not throw")
    void testSetServerVersion() {
        assertDoesNotThrow(() -> backend.setServerVersion("1.19.2"));
    }

    @Test
    @DisplayName("setServerVersion with different versions")
    void testSetServerVersion_multipleVersions() {
        assertDoesNotThrow(() -> backend.setServerVersion("1.20.4"));
        assertDoesNotThrow(() -> backend.setServerVersion("1.19.2"));
        assertDoesNotThrow(() -> backend.setServerVersion("1.18.1"));
    }

    // SETVERBOSE TESTS

    @Test
    @DisplayName("setVerbose does not throw")
    void testSetVerbose() {
        assertDoesNotThrow(() -> backend.setVerbose(true));
        assertDoesNotThrow(() -> backend.setVerbose(false));
    }

    // GETSERVERLOG TESTS

    @Test
    @DisplayName("getServerLog returns error message when Docker not available")
    void testGetServerLog_dockerNotAvailable() {
        String log = backend.getServerLog();
        assertNotNull(log);
        // Returns error message when Docker is not available or container doesn't exist
        // The message may be "Error getting logs:" or similar
    }

    // GETSERVERSTATUS TESTS

    @Test
    @DisplayName("getServerStatus returns status map")
    void testGetServerStatus() {
        Map<String, Object> status = backend.getServerStatus();

        assertNotNull(status);
        assertEquals("docker-server", status.get("type"));
        assertEquals("1.20.4", status.get("version"));
        assertEquals("localhost", status.get("rcon_host"));
        assertEquals(25575, status.get("rcon_port"));
    }

    @Test
    @DisplayName("getServerStatus contains 'running' key")
    void testGetServerStatus_hasRunningKey() {
        Map<String, Object> status = backend.getServerStatus();
        assertTrue(status.containsKey("running"));
    }

    @Test
    @DisplayName("getServerStatus running is false before initialization")
    void testGetServerStatus_notRunning() {
        Map<String, Object> status = backend.getServerStatus();
        assertFalse((Boolean) status.get("running"));
    }

    // EXECUTECOMMAND TESTS

    @Test
    @DisplayName("executeCommand returns message when RCON not available")
    void testExecuteCommand_rconNotAvailable() {
        String result = backend.executeCommand("list");
        assertNotNull(result);
        // Should return error message or similar
    }

    // ISSERVERRUNNING TESTS

    @Test
    @DisplayName("isServerRunning returns false when Docker process is null")
    void testIsServerRunning_noProcess() {
        assertFalse(backend.isServerRunning());
    }

    // MOVEPLAYER TESTS

    @Test
    @DisplayName("movePlayer does not throw")
    void testMovePlayer() {
        assertDoesNotThrow(() -> backend.movePlayer("player", "to", "100 64 200"));
    }

    // EQUIPITEM TESTS

    @Test
    @DisplayName("equipItem does not throw")
    void testEquipItem() {
        assertDoesNotThrow(() -> backend.equipItem("player", "diamond_sword", "mainhand"));
    }

    // GIVEITEM TESTS

    @Test
    @DisplayName("giveItem does not throw")
    void testGiveItem() {
        assertDoesNotThrow(() -> backend.giveItem("player", "diamond", 64));
    }

    @Test
    @DisplayName("giveItem with different counts")
    void testGiveItem_differentCounts() {
        assertDoesNotThrow(() -> backend.giveItem("player", "diamond", 1));
        assertDoesNotThrow(() -> backend.giveItem("player", "dirt", 64));
    }

    // EXECUTEPLAYERCOMMAND TESTS

    @Test
    @DisplayName("executePlayerCommand does not throw")
    void testExecutePlayerCommand() {
        assertDoesNotThrow(() -> backend.executePlayerCommand("player", "home", List.of()));
    }

    @Test
    @DisplayName("executePlayerCommand with arguments")
    void testExecutePlayerCommand_withArguments() {
        assertDoesNotThrow(() -> backend.executePlayerCommand("player", "give", List.of("diamond", "64")));
    }

    // SENDCHAT TESTS

    @Test
    @DisplayName("sendChat does not throw")
    void testSendChat() {
        assertDoesNotThrow(() -> backend.sendChat("player", "Hello World"));
    }

    // USEITEM TESTS

    @Test
    @DisplayName("useItem does not throw")
    void testUseItem() {
        assertDoesNotThrow(() -> backend.useItem("player", "diamond_sword", "target"));
    }

    // SPAWNENTITY TESTS

    @Test
    @DisplayName("spawnEntity does not throw")
    void testSpawnEntity() {
        assertDoesNotThrow(() -> backend.spawnEntity("test_zombie", "minecraft:zombie", List.of(100.0, 64.0, 200.0), null));
    }

    // ENTITYEXISTS TESTS

    @Test
    @DisplayName("entityExists returns false when RCON not available")
    void testEntityExists_rconNotAvailable() {
        boolean result = backend.entityExists("test_entity");
        assertFalse(result);
    }

    // GETENTITYHEALTH TESTS

    @Test
    @DisplayName("getEntityHealth returns 0.0 when RCON not available")
    void testGetEntityHealth_rconNotAvailable() {
        double health = backend.getEntityHealth("test_entity");
        assertEquals(0.0, health, 0.01);
    }

    // SETENTITYHEALTH TESTS

    @Test
    @DisplayName("setEntityHealth does not throw")
    void testSetEntityHealth() {
        assertDoesNotThrow(() -> backend.setEntityHealth("test_entity", 20.0));
    }

    // EXECUTESERVERCOMMAND TESTS

    @Test
    @DisplayName("executeServerCommand does not throw")
    void testExecuteServerCommand() {
        assertDoesNotThrow(() -> backend.executeServerCommand("list", List.of()));
    }

    @Test
    @DisplayName("executeServerCommand with arguments")
    void testExecuteServerCommand_withArguments() {
        assertDoesNotThrow(() -> backend.executeServerCommand("give", List.of("player", "diamond", "64")));
    }

    // PLAYERINVENTORYCONTAINS TESTS

    @Test
    @DisplayName("playerInventoryContains returns false when RCON not available")
    void testPlayerInventoryContains_rconNotAvailable() {
        boolean result = backend.playerInventoryContains("player", "diamond", "mainhand");
        assertFalse(result);
    }

    // PLUGINRECEIVEDCOMMAND TESTS

    @Test
    @DisplayName("pluginReceivedCommand returns false")
    void testPluginReceivedCommand() {
        boolean result = backend.pluginReceivedCommand("Essentials", "home", "player");
        assertFalse(result);
    }

    // REMOVEALLTESTENTITIES TESTS

    @Test
    @DisplayName("removeAllTestEntities does not throw")
    void testRemoveAllTestEntities() {
        assertDoesNotThrow(() -> backend.removeAllTestEntities());
    }

    // REMOVEALLTESTPLAYERS TESTS

    @Test
    @DisplayName("removeAllTestPlayers does not throw")
    void testRemoveAllTestPlayers() {
        assertDoesNotThrow(() -> backend.removeAllTestPlayers());
    }

    // INITIALIZATION AND CLEANUP TESTS

    @Test
    @DisplayName("initialize does not throw")
    void testInitialize() throws Exception {
        assertDoesNotThrow(() -> backend.initialize());
    }

    @Test
    @DisplayName("cleanup does not throw when not initialized")
    void testCleanup_notInitialized() throws Exception {
        assertDoesNotThrow(() -> backend.cleanup());
    }

    @Test
    @DisplayName("initialize then cleanup does not throw")
    void testInitialize_thenCleanup() throws Exception {
        assertDoesNotThrow(() -> backend.initialize());
        assertDoesNotThrow(() -> backend.cleanup());
    }

    // SERVERBACKEND INTERFACE TESTS

    @Test
    @DisplayName("stopServer does not throw when not running")
    void testStopServer_notRunning() throws Exception {
        assertDoesNotThrow(() -> backend.stopServer());
    }
}
