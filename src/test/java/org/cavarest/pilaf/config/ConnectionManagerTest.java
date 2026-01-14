package org.cavarest.pilaf.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConnectionManager utility methods
 */
@DisplayName("ConnectionManager Tests")
class ConnectionManagerTest {

    @Test
    @DisplayName("constructor creates ConnectionManager with config")
    void testConstructor_createsConnectionManager() {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        assertNotNull(manager);
    }

    @Test
    @DisplayName("getConnectedPlayers returns empty array initially")
    void testGetConnectedPlayers_returnsEmptyArrayInitially() {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        String[] players = manager.getConnectedPlayers();
        assertNotNull(players);
        assertEquals(0, players.length);
    }

    @Test
    @DisplayName("isServiceHealthy returns false for unknown service")
    void testIsServiceHealthy_unknownService_returnsFalse() {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        boolean healthy = manager.isServiceHealthy("unknown");
        assertFalse(healthy);
    }

    @Test
    @DisplayName("isServiceHealthy returns false for rcon initially")
    void testIsServiceHealthy_rconInitial_returnsFalse() {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        boolean healthy = manager.isServiceHealthy("rcon");
        assertFalse(healthy);
    }

    @Test
    @DisplayName("isServiceHealthy returns false for mineflayer initially")
    void testIsServiceHealthy_mineflayerInitial_returnsFalse() {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        boolean healthy = manager.isServiceHealthy("mineflayer");
        assertFalse(healthy);
    }

    @Test
    @DisplayName("areServicesHealthy returns true when skipHealthChecks is true")
    void testAreServicesHealthy_skipHealthChecks_returnsTrue() {
        TestConfiguration config = new TestConfiguration();
        config.setSkipHealthChecks(true);
        ConnectionManager manager = new ConnectionManager(config);
        boolean healthy = manager.areServicesHealthy();
        assertTrue(healthy);
    }

    @Test
    @DisplayName("areServicesHealthy returns false when services not healthy")
    void testAreServicesHealthy_servicesNotHealthy_returnsFalse() {
        TestConfiguration config = new TestConfiguration();
        config.setSkipHealthChecks(false);
        ConnectionManager manager = new ConnectionManager(config);
        boolean healthy = manager.areServicesHealthy();
        assertFalse(healthy);
    }

    @Test
    @DisplayName("extractHost with valid URL returns host")
    void testExtractHost_validUrl_returnsHost() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractHostMethod = ConnectionManager.class.getDeclaredMethod("extractHost", String.class);
        extractHostMethod.setAccessible(true);

        String host = (String) extractHostMethod.invoke(manager, "http://example.com:8080");
        assertEquals("example.com", host);
    }

    @Test
    @DisplayName("extractHost with https URL returns host")
    void testExtractHost_httpsUrl_returnsHost() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractHostMethod = ConnectionManager.class.getDeclaredMethod("extractHost", String.class);
        extractHostMethod.setAccessible(true);

        String host = (String) extractHostMethod.invoke(manager, "https://localhost:3000");
        assertEquals("localhost", host);
    }

    @Test
    @DisplayName("extractHost with malformed URI returns localhost")
    void testExtractHost_malformedUri_returnsLocalhost() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractHostMethod = ConnectionManager.class.getDeclaredMethod("extractHost", String.class);
        extractHostMethod.setAccessible(true);

        // Use an invalid URI that will throw exception during parsing
        String host = (String) extractHostMethod.invoke(manager, "ht!tp://invalid");
        assertEquals("localhost", host);
    }

    @Test
    @DisplayName("extractHost with null URL returns localhost")
    void testExtractHost_nullUrl_returnsLocalhost() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractHostMethod = ConnectionManager.class.getDeclaredMethod("extractHost", String.class);
        extractHostMethod.setAccessible(true);

        String host = (String) extractHostMethod.invoke(manager, (String) null);
        assertEquals("localhost", host);
    }

    @Test
    @DisplayName("extractPort with valid URL returns port")
    void testExtractPort_validUrl_returnsPort() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractPortMethod = ConnectionManager.class.getDeclaredMethod("extractPort", String.class);
        extractPortMethod.setAccessible(true);

        int port = (Integer) extractPortMethod.invoke(manager, "http://example.com:8080");
        assertEquals(8080, port);
    }

    @Test
    @DisplayName("extractPort with URL without explicit port returns 80")
    void testExtractPort_noExplicitPort_returns80() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractPortMethod = ConnectionManager.class.getDeclaredMethod("extractPort", String.class);
        extractPortMethod.setAccessible(true);

        int port = (Integer) extractPortMethod.invoke(manager, "http://example.com");
        assertEquals(80, port);
    }

    @Test
    @DisplayName("extractPort with https URL without explicit port returns 80")
    void testExtractPort_httpsNoExplicitPort_returns80() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractPortMethod = ConnectionManager.class.getDeclaredMethod("extractPort", String.class);
        extractPortMethod.setAccessible(true);

        int port = (Integer) extractPortMethod.invoke(manager, "https://example.com/path");
        assertEquals(80, port);
    }

    @Test
    @DisplayName("extractPort with malformed URI returns 3000")
    void testExtractPort_malformedUri_returns3000() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractPortMethod = ConnectionManager.class.getDeclaredMethod("extractPort", String.class);
        extractPortMethod.setAccessible(true);

        // Use an invalid URI that will throw exception during parsing
        int port = (Integer) extractPortMethod.invoke(manager, "ht!tp://invalid");
        assertEquals(3000, port);
    }

    @Test
    @DisplayName("extractPort with null URL returns 3000")
    void testExtractPort_nullUrl_returns3000() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method extractPortMethod = ConnectionManager.class.getDeclaredMethod("extractPort", String.class);
        extractPortMethod.setAccessible(true);

        int port = (Integer) extractPortMethod.invoke(manager, (String) null);
        assertEquals(3000, port);
    }

    @Test
    @DisplayName("cleanup can be called multiple times safely")
    void testCleanup_multipleCalls_safe() {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        manager.cleanup();
        manager.cleanup();
        // No exception thrown
        assertNotNull(manager);
    }

    @Test
    @DisplayName("disconnectPlayer with non-existent player does not throw")
    void testDisconnectPlayer_nonExistentPlayer_noException() {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        manager.disconnectPlayer("nonexistent");
        // No exception thrown
        assertNotNull(manager);
    }

    @Test
    @DisplayName("ensureInitialized throws IllegalStateException when not initialized")
    void testEnsureInitialized_notInitialized_throwsException() throws Exception {
        TestConfiguration config = new TestConfiguration();
        ConnectionManager manager = new ConnectionManager(config);
        Method ensureInitializedMethod = ConnectionManager.class.getDeclaredMethod("ensureInitialized");
        ensureInitializedMethod.setAccessible(true);

        // Reflection wraps exceptions in InvocationTargetException
        Exception exception = assertThrows(Exception.class, () -> ensureInitializedMethod.invoke(manager));
        assertTrue(exception.getCause() instanceof IllegalStateException);
    }
}
