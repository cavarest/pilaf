package org.cavarest.pilaf.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestConfiguration.
 */
@DisplayName("TestConfiguration Tests")
class TestConfigurationTest {

    private TestConfiguration config;

    @BeforeEach
    void setUp() {
        config = new TestConfiguration();
    }

    // CONSTRUCTOR TESTS

    @Test
    @DisplayName("constructor initializes with default values")
    void testConstructor_defaultValues() {
        assertNotNull(config.getMineflayerUrl());
        assertNotNull(config.getRconHost());
        assertTrue(config.getRconPort() > 0);
        assertNotNull(config.getRconPassword());
        assertNotNull(config.getTestPlayer());
        assertNotNull(config.getBackend());
        assertNotNull(config.getServerVersion());
        assertNotNull(config.getReportDir());
        assertNotNull(config.getReportFormat());
    }

    // MINEFLAYER SETTINGS TESTS

    @Test
    @DisplayName("setMineflayerUrl updates and retrieves URL")
    void testSetMineflayerUrl() {
        config.setMineflayerUrl("http://example.com:8080");
        assertEquals("http://example.com:8080", config.getMineflayerUrl());
    }

    @Test
    @DisplayName("getMineflayerTimeout returns default timeout")
    void testGetMineflayerTimeout_default() {
        assertEquals(10_000, config.getMineflayerTimeout());
    }

    // RCON SETTINGS TESTS

    @Test
    @DisplayName("setRconHost updates and retrieves host")
    void testSetRconHost() {
        config.setRconHost("192.168.1.100");
        assertEquals("192.168.1.100", config.getRconHost());
    }

    @Test
    @DisplayName("setRconPort updates and retrieves port")
    void testSetRconPort() {
        config.setRconPort(12345);
        assertEquals(12345, config.getRconPort());
    }

    @Test
    @DisplayName("setRconPassword updates and retrieves password")
    void testSetRconPassword() {
        config.setRconPassword("secret123");
        assertEquals("secret123", config.getRconPassword());
    }

    @Test
    @DisplayName("getRconTimeout returns default timeout")
    void testGetRconTimeout_default() {
        assertEquals(5_000, config.getRconTimeout());
    }

    // TEST PLAYER SETTINGS TESTS

    @Test
    @DisplayName("setTestPlayer updates and retrieves player name")
    void testSetTestPlayer() {
        config.setTestPlayer("custom_player");
        assertEquals("custom_player", config.getTestPlayer());
    }

    // BACKEND SETTINGS TESTS

    @Test
    @DisplayName("setBackend updates and retrieves backend type")
    void testSetBackend() {
        config.setBackend("rcon");
        assertEquals("rcon", config.getBackend());
    }

    @Test
    @DisplayName("setServerVersion updates and retrieves version")
    void testSetServerVersion() {
        config.setServerVersion("1.20.4");
        assertEquals("1.20.4", config.getServerVersion());
    }

    // REPORT SETTINGS TESTS

    @Test
    @DisplayName("setReportDir updates and retrieves report directory")
    void testSetReportDir() {
        config.setReportDir("/tmp/reports");
        assertEquals("/tmp/reports", config.getReportDir());
    }

    // VERBOSE SETTINGS TESTS

    @Test
    @DisplayName("setVerbose to true updates verbose flag")
    void testSetVerbose_true() {
        config.setVerbose(true);
        assertTrue(config.isVerbose());
    }

    @Test
    @DisplayName("setVerbose to false updates verbose flag")
    void testSetVerbose_false() {
        config.setVerbose(false);
        assertFalse(config.isVerbose());
    }

    // HEALTH CHECK SETTINGS TESTS

    @Test
    @DisplayName("setSkipHealthChecks to true updates flag")
    void testSetSkipHealthChecks_true() {
        config.setSkipHealthChecks(true);
        assertTrue(config.isSkipHealthChecks());
    }

    @Test
    @DisplayName("setSkipHealthChecks to false updates flag")
    void testSetSkipHealthChecks_false() {
        config.setSkipHealthChecks(false);
        assertFalse(config.isSkipHealthChecks());
    }

    @Test
    @DisplayName("getHealthCheckTimeout returns default timeout")
    void testGetHealthCheckTimeout_default() {
        assertEquals(5_000, config.getHealthCheckTimeout());
    }

    // METHOD CHAINING TESTS

    @Test
    @DisplayName("setters return this for method chaining")
    void testSetters_returnThis() {
        TestConfiguration result = config
            .setMineflayerUrl("http://localhost:3000")
            .setRconHost("localhost")
            .setRconPort(25575)
            .setRconPassword("password")
            .setTestPlayer("tester")
            .setBackend("mineflayer")
            .setServerVersion("1.21.0")
            .setReportDir("target/reports")
            .setVerbose(true)
            .setSkipHealthChecks(false);

        assertSame(config, result);
        assertEquals("http://localhost:3000", config.getMineflayerUrl());
        assertEquals("localhost", config.getRconHost());
        assertEquals(25575, config.getRconPort());
        assertEquals("password", config.getRconPassword());
        assertEquals("tester", config.getTestPlayer());
        assertEquals("mineflayer", config.getBackend());
        assertEquals("1.21.0", config.getServerVersion());
        assertEquals("target/reports", config.getReportDir());
        assertTrue(config.isVerbose());
        assertFalse(config.isSkipHealthChecks());
    }

    // TOSTRING TEST

    @Test
    @DisplayName("toString returns formatted string with configuration")
    void testToString() {
        config.setMineflayerUrl("http://localhost:3000");
        config.setRconHost("localhost");
        config.setRconPort(25575);
        config.setTestPlayer("tester");
        config.setBackend("mineflayer");
        config.setServerVersion("1.21.0");

        String result = config.toString();

        assertTrue(result.contains("mineflayerUrl="));
        assertTrue(result.contains("http://localhost:3000"));
        assertTrue(result.contains("rconHost="));
        assertTrue(result.contains("localhost"));
        assertTrue(result.contains("25575"));
        assertTrue(result.contains("tester"));
        assertTrue(result.contains("mineflayer"));
        assertTrue(result.contains("1.21.0"));
    }

    // DEFAULT VALUES TESTS

    @Test
    @DisplayName("default mineflayerUrl is localhost:8888")
    void testDefault_mineflayerUrl() {
        assertTrue(config.getMineflayerUrl().contains("localhost"));
        assertTrue(config.getMineflayerUrl().contains("8888"));  // TestPorts.MINEFLAYER_API_PORT
    }

    @Test
    @DisplayName("default rconHost is localhost")
    void testDefault_rconHost() {
        assertEquals("localhost", config.getRconHost());
    }

    @Test
    @DisplayName("default rconPort is 35125")
    void testDefault_rconPort() {
        assertEquals(35125, config.getRconPort());  // TestPorts.RCON_PORT (Docker external port)
    }

    @Test
    @DisplayName("default rconPassword is dragon123")
    void testDefault_rconPassword() {
        assertEquals("dragon123", config.getRconPassword());
    }

    @Test
    @DisplayName("default testPlayer is pilaf_tester")
    void testDefault_testPlayer() {
        assertEquals("pilaf_tester", config.getTestPlayer());
    }

    @Test
    @DisplayName("default backend is mineflayer")
    void testDefault_backend() {
        assertEquals("mineflayer", config.getBackend());
    }

    @Test
    @DisplayName("default serverVersion is 1.21.5")
    void testDefault_serverVersion() {
        assertEquals("1.21.5", config.getServerVersion());
    }

    @Test
    @DisplayName("default reportDir is target/pilaf-reports")
    void testDefault_reportDir() {
        assertEquals("target/pilaf-reports", config.getReportDir());
    }

    @Test
    @DisplayName("default reportFormat is html")
    void testDefault_reportFormat() {
        assertEquals("html", config.getReportFormat());
    }

    @Test
    @DisplayName("default verbose is false")
    void testDefault_verbose() {
        assertFalse(config.isVerbose());
    }

    @Test
    @DisplayName("default skipHealthChecks is false")
    void testDefault_skipHealthChecks() {
        assertFalse(config.isSkipHealthChecks());
    }
}
