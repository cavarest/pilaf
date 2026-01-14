package org.cavarest.pilaf.cli;

import org.cavarest.pilaf.config.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigLoader.
 */
@DisplayName("ConfigLoader Tests")
class ConfigLoaderTest {

    private ConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new ConfigLoader();
    }

    @Test
    @DisplayName("loadFromFile with non-existent file throws FileNotFoundException")
    void testLoadFromFile_nonExistentFile_throwsException() {
        File nonExistentFile = new File("/non/existent/config.yaml");

        assertThrows(Exception.class, () -> configLoader.loadFromFile(nonExistentFile));
    }

    @Test
    @DisplayName("loadFromFile with valid YAML returns TestConfiguration")
    void testLoadFromFile_validYaml_returnsConfig(@TempDir Path tempDir) throws Exception {
        String yamlContent =
            "backend: rcon\n" +
            "rcon_host: localhost\n" +
            "rcon_port: 25575\n" +
            "rcon_password: test123\n";

        File configFile = tempDir.resolve("config.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(yamlContent);
        }

        TestConfiguration result = configLoader.loadFromFile(configFile);

        assertNotNull(result);
        assertEquals("rcon", result.getBackend());
        assertEquals("localhost", result.getRconHost());
        assertEquals(25575, result.getRconPort());
        assertEquals("test123", result.getRconPassword());
    }

    @Test
    @DisplayName("loadFromString with valid YAML returns TestConfiguration")
    void testLoadFromString_validYaml_returnsConfig() throws Exception {
        String yamlContent =
            "backend: mineflayer\n" +
            "mineflayer_url: http://localhost:3000\n" +
            "skip_health_checks: true\n";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertNotNull(result);
        assertEquals("mineflayer", result.getBackend());
        assertEquals("http://localhost:3000", result.getMineflayerUrl());
        assertTrue(result.isSkipHealthChecks());
    }

    @Test
    @DisplayName("loadFromString with server_backend key sets backend")
    void testLoadFromString_serverBackendKey_setsBackend() throws Exception {
        String yamlContent = "server_backend: docker";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals("docker", result.getBackend());
    }

    @Test
    @DisplayName("loadFromString with backend key takes precedence over server_backend")
    void testLoadFromString_backendKeyTakesPrecedence() throws Exception {
        String yamlContent =
            "backend: rcon\n" +
            "server_backend: docker\n";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals("rcon", result.getBackend());
    }

    @Test
    @DisplayName("loadFromString with all fields returns populated configuration")
    void testLoadFromString_allFields_returnsPopulatedConfig() throws Exception {
        String yamlContent =
            "backend: rcon\n" +
            "server_version: 1.20.4\n" +
            "mineflayer_url: http://localhost:3000\n" +
            "rcon_host: localhost\n" +
            "rcon_port: 25575\n" +
            "rcon_password: dragon123\n" +
            "skip_health_checks: true\n" +
            "report_directory: /tmp/reports\n" +
            "verbose: true\n";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals("rcon", result.getBackend());
        assertEquals("1.20.4", result.getServerVersion());
        assertEquals("http://localhost:3000", result.getMineflayerUrl());
        assertEquals("localhost", result.getRconHost());
        assertEquals(25575, result.getRconPort());
        assertEquals("dragon123", result.getRconPassword());
        assertTrue(result.isSkipHealthChecks());
        assertEquals("/tmp/reports", result.getReportDir());
        assertTrue(result.isVerbose());
    }

    @Test
    @DisplayName("loadFromString with empty YAML throws NullPointerException")
    void testLoadFromString_emptyYaml_throwsException() {
        String yamlContent = "";

        // Empty YAML returns null from SnakeYAML, which causes NPE in parseConfiguration
        assertThrows(NullPointerException.class, () -> configLoader.loadFromString(yamlContent));
    }

    @Test
    @DisplayName("loadFromString parses port as number")
    void testLoadFromString_portAsNumber() throws Exception {
        String yamlContent = "rcon_port: 25575";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals(25575, result.getRconPort());
    }

    @Test
    @DisplayName("loadFromString parses port as string")
    void testLoadFromString_portAsString() throws Exception {
        String yamlContent = "rcon_port: \"25575\"";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals(25575, result.getRconPort());
    }

    @Test
    @DisplayName("loadFromString parses boolean true values")
    void testLoadFromString_booleanTrue() throws Exception {
        String yamlContent =
            "skip_health_checks: true\n" +
            "verbose: true\n";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertTrue(result.isSkipHealthChecks());
        assertTrue(result.isVerbose());
    }

    @Test
    @DisplayName("loadFromString parses boolean false values")
    void testLoadFromString_booleanFalse() throws Exception {
        String yamlContent =
            "skip_health_checks: false\n" +
            "verbose: false\n";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertFalse(result.isSkipHealthChecks());
        assertFalse(result.isVerbose());
    }

    @Test
    @DisplayName("loadFromString handles null values gracefully")
    void testLoadFromString_nullValues() throws Exception {
        String yamlContent =
            "backend: null\n" +
            "rcon_host: null\n" +
            "rcon_password: null\n";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertNull(result.getBackend());
        assertNull(result.getRconHost());
        assertNull(result.getRconPassword());
    }

    @Test
    @DisplayName("loadFromString with invalid port throws NumberFormatException")
    void testLoadFromString_invalidPort_throwsException() {
        String yamlContent = "rcon_port: invalid";

        // Invalid port string throws NumberFormatException
        assertThrows(NumberFormatException.class, () -> configLoader.loadFromString(yamlContent));
    }

    @Test
    @DisplayName("loadFromString with report_directory key sets reportDir")
    void testLoadFromString_reportDirectory_setsReportDir() throws Exception {
        String yamlContent = "report_directory: /custom/path";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals("/custom/path", result.getReportDir());
    }

    @Test
    @DisplayName("loadFromString with server_version key sets serverVersion")
    void testLoadFromString_serverVersion_setsServerVersion() throws Exception {
        String yamlContent = "server_version: 1.21.0";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals("1.21.0", result.getServerVersion());
    }

    @Test
    @DisplayName("loadFromFile with complex YAML configuration")
    void testLoadFromFile_complexConfiguration(@TempDir Path tempDir) throws Exception {
        String yamlContent =
            "# Pilaf Configuration\n" +
            "backend: headlessmc\n" +
            "server_version: 1.20.4\n" +
            "rcon_host: 192.168.1.100\n" +
            "rcon_port: 25575\n" +
            "rcon_password: securePassword\n" +
            "skip_health_checks: false\n" +
            "report_directory: target/test-reports\n" +
            "verbose: true\n";

        File configFile = tempDir.resolve("pilaf-config.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(yamlContent);
        }

        TestConfiguration result = configLoader.loadFromFile(configFile);

        assertEquals("headlessmc", result.getBackend());
        assertEquals("1.20.4", result.getServerVersion());
        assertEquals("192.168.1.100", result.getRconHost());
        assertEquals(25575, result.getRconPort());
        assertEquals("securePassword", result.getRconPassword());
        assertFalse(result.isSkipHealthChecks());
        assertEquals("target/test-reports", result.getReportDir());
        assertTrue(result.isVerbose());
    }

    @Test
    @DisplayName("loadFromString with only backend field")
    void testLoadFromString_onlyBackend() throws Exception {
        String yamlContent = "backend: rcon";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals("rcon", result.getBackend());
        // Other fields should have default values from TestConfiguration
        assertEquals("localhost", result.getRconHost());
        assertEquals(35125, result.getRconPort());  // TestPorts.RCON_PORT (Docker external port)
        assertEquals("dragon123", result.getRconPassword());
    }

    @Test
    @DisplayName("loadFromString preserves mineflayer_url")
    void testLoadFromString_mineflayerUrl() throws Exception {
        String yamlContent = "mineflayer_url: http://example.com:8080";

        TestConfiguration result = configLoader.loadFromString(yamlContent);

        assertEquals("http://example.com:8080", result.getMineflayerUrl());
    }
}
