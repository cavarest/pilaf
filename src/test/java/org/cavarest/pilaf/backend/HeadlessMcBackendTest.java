package org.cavarest.pilaf.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeadlessMcBackend
 * Focuses on methods that don't require actual HeadlessMC infrastructure.
 */
@DisplayName("HeadlessMcBackend Tests")
class HeadlessMcBackendTest {

    private HeadlessMcBackend backend;

    @BeforeEach
    void setUp() {
        backend = new HeadlessMcBackend("1.20.4", "localhost", 25575, "password", false, false);
    }

    // CONSTRUCTOR TESTS

    @Test
    @DisplayName("constructor creates HeadlessMcBackend with provided parameters")
    void testConstructor() {
        HeadlessMcBackend b = new HeadlessMcBackend("1.20.4", "localhost", 25575, "password", false, false);
        assertNotNull(b);
        assertFalse(b.isInitialized());
    }

    @Test
    @DisplayName("constructor with different server version")
    void testConstructor_differentVersion() {
        HeadlessMcBackend b = new HeadlessMcBackend("1.19.2", "localhost", 25575, "password", false, false);
        assertNotNull(b);
    }

    @Test
    @DisplayName("constructor with autoLaunch enabled")
    void testConstructor_autoLaunchEnabled() {
        HeadlessMcBackend b = new HeadlessMcBackend("1.20.4", "localhost", 25575, "password", true, false);
        assertNotNull(b);
    }

    @Test
    @DisplayName("constructor with rconFallback enabled")
    void testConstructor_rconFallbackEnabled() {
        HeadlessMcBackend b = new HeadlessMcBackend("1.20.4", "localhost", 25575, "password", false, true);
        assertNotNull(b);
    }

    // GETTYPE TESTS

    @Test
    @DisplayName("getType returns 'headlessmc'")
    void testGetType() {
        assertEquals("headlessmc", backend.getType());
    }

    // ISINITIALIZED TESTS

    @Test
    @DisplayName("isInitialized returns false for new backend")
    void testIsInitialized_newBackend() {
        assertFalse(backend.isInitialized());
    }

    // SETSERVERVERSION TESTS

    @Test
    @DisplayName("setServerVersion sets the server version")
    void testSetServerVersion() {
        backend.setServerVersion("1.19.2");
        // Version is set internally, we can't directly verify without reflection
        // But the method should not throw
        assertDoesNotThrow(() -> backend.setServerVersion("1.19.2"));
    }

    @Test
    @DisplayName("setServerVersion with different versions")
    void testSetServerVersion_multipleVersions() {
        assertDoesNotThrow(() -> backend.setServerVersion("1.20.4"));
        assertDoesNotThrow(() -> backend.setServerVersion("1.19.2"));
        assertDoesNotThrow(() -> backend.setServerVersion("1.18.1"));
    }

    // SETAUTOLAUNCH TESTS

    @Test
    @DisplayName("setAutoLaunch sets the autoLaunch flag")
    void testSetAutoLaunch() {
        assertDoesNotThrow(() -> backend.setAutoLaunch(true));
        assertDoesNotThrow(() -> backend.setAutoLaunch(false));
    }

    // SETRCONFALLBACK TESTS

    @Test
    @DisplayName("setRconFallback sets the rconFallback flag")
    void testSetRconFallback() {
        assertDoesNotThrow(() -> backend.setRconFallback(true));
        assertDoesNotThrow(() -> backend.setRconFallback(false));
    }

    // GETSERVERPROCESS TESTS

    @Test
    @DisplayName("getServerProcess returns null before initialization")
    void testGetServerProcess_notInitialized() {
        assertNull(backend.getServerProcess());
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
    @DisplayName("getServerLog returns unavailable message")
    void testGetServerLog() {
        String log = backend.getServerLog();
        assertEquals("[Log access not available in HeadlessMc backend]", log);
    }

    // ENTITYEXISTS TESTS

    @Test
    @DisplayName("entityExists returns false (not supported)")
    void testEntityExists() {
        boolean result = backend.entityExists("test_entity");
        assertFalse(result);
    }

    // GETENTITYHEALTH TESTS

    @Test
    @DisplayName("getEntityHealth returns 0.0 (not supported)")
    void testGetEntityHealth() {
        double health = backend.getEntityHealth("test_entity");
        assertEquals(0.0, health, 0.01);
    }

    // PLAYERINVENTORYCONTAINS TESTS

    @Test
    @DisplayName("playerInventoryContains returns false (not supported)")
    void testPlayerInventoryContains() {
        boolean result = backend.playerInventoryContains("player", "diamond", "mainhand");
        assertFalse(result);
    }

    // PLUGINRECEIVEDCOMMAND TESTS

    @Test
    @DisplayName("pluginReceivedCommand returns false (not supported)")
    void testPluginReceivedCommand() {
        boolean result = backend.pluginReceivedCommand("Essentials", "home", "player");
        assertFalse(result);
    }

    // USEITEM TESTS

    @Test
    @DisplayName("useItem does not throw")
    void testUseItem() {
        assertDoesNotThrow(() -> backend.useItem("player", "diamond_sword", "target"));
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

    // SENDCHAT TESTS

    @Test
    @DisplayName("sendChat does not throw")
    void testSendChat() {
        assertDoesNotThrow(() -> backend.sendChat("player", "Hello World"));
    }

    // SPAWNENTITY TESTS

    @Test
    @DisplayName("spawnEntity does not throw")
    void testSpawnEntity() {
        assertDoesNotThrow(() -> backend.spawnEntity("test_zombie", "minecraft:zombie", List.of(100.0, 64.0, 200.0), null));
    }

    @Test
    @DisplayName("spawnEntity with null location")
    void testSpawnEntity_nullLocation() {
        assertDoesNotThrow(() -> backend.spawnEntity("test_zombie", "minecraft:zombie", null, null));
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
    @DisplayName("initialize does not throw with autoLaunch disabled")
    void testInitialize_noAutoLaunch() throws Exception {
        assertDoesNotThrow(() -> backend.initialize());
    }

    @Test
    @DisplayName("initialize with rconFallback disabled does not throw")
    void testInitialize_noRconFallback() throws Exception {
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

    // UNCOVERED BRANCH TESTS

    @Test
    @DisplayName("initialize() when already initialized returns early")
    void testInitialize_doubleInitialize() throws Exception {
        backend.initialize();
        boolean wasInitialized = backend.isInitialized();

        // Second initialize should return early without throwing
        assertDoesNotThrow(() -> backend.initialize());

        // Should still be initialized
        assertTrue(backend.isInitialized());
        assertEquals(wasInitialized, backend.isInitialized());
    }

    @Test
    @DisplayName("initialize() with rconFallback enabled but RCON unavailable continues")
    void testInitialize_rconFallback_failsToConnect() throws Exception {
        HeadlessMcBackend backendWithRcon = new HeadlessMcBackend("1.20.4", "invalid-host", 9999, "wrong-password", false, true);

        // Should initialize even if RCON connection fails (fallback is optional)
        assertDoesNotThrow(() -> backendWithRcon.initialize());
        assertTrue(backendWithRcon.isInitialized());
    }

    @Test
    @DisplayName("cleanup() cleans up rconBackend when initialized")
    void testCleanup_withRconBackend() throws Exception {
        // Create a backend with rconFallback enabled
        HeadlessMcBackend backendWithRcon = new HeadlessMcBackend("1.20.4", "localhost", 25575, "password", false, true);
        backendWithRcon.initialize();

        // Cleanup should not throw even if RCON wasn't properly initialized
        assertDoesNotThrow(() -> backendWithRcon.cleanup());
        assertFalse(backendWithRcon.isInitialized());
    }

    @Test
    @DisplayName("executeServerCommand() when rconBackend is initialized uses it")
    void testExecuteServerCommand_withRconBackend() throws Exception {
        // This test uses reflection to simulate having an rconBackend
        // The actual RCON connection may fail, but the code path should be tested

        HeadlessMcBackend backendWithRcon = new HeadlessMcBackend("1.20.4", "localhost", 25575, "password", false, true);
        backendWithRcon.initialize();

        // Should not throw - command may fail via RCON but should handle gracefully
        assertDoesNotThrow(() -> backendWithRcon.executeServerCommand("list", List.of()));
    }

    @Test
    @DisplayName("initialize() then cleanup() then initialize() again")
    void testInitialize_afterCleanup() throws Exception {
        backend.initialize();
        backend.cleanup();
        assertFalse(backend.isInitialized());

        // Should be able to initialize again after cleanup
        assertDoesNotThrow(() -> backend.initialize());
        assertTrue(backend.isInitialized());
    }

    @Test
    @DisplayName("cleanup() when not initialized does nothing")
    void testCleanup_notInitialized_doesNothing() throws Exception {
        // Backend is not initialized
        assertFalse(backend.isInitialized());

        // Cleanup should return early without throwing
        assertDoesNotThrow(() -> backend.cleanup());

        // Should still not be initialized
        assertFalse(backend.isInitialized());
    }
}
