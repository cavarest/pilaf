package org.cavarest.pilaf.cli;

import org.cavarest.pilaf.config.TestConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Loads Pilaf configuration from YAML files
 */
public class ConfigLoader {

    private final Yaml yaml = new Yaml();

    public TestConfiguration loadFromFile(File configFile) throws Exception {
        if (!configFile.exists()) {
            throw new FileNotFoundException("Configuration file not found: " + configFile);
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Map<String, Object> config = yaml.load(fis);
            return parseConfiguration(config);
        }
    }

    public TestConfiguration loadFromString(String yamlContent) throws Exception {
        Map<String, Object> config = yaml.load(yamlContent);
        return parseConfiguration(config);
    }

    private TestConfiguration parseConfiguration(Map<String, Object> config) {
        TestConfiguration testConfig = new TestConfiguration();

        // Backend configuration - check both 'backend' and 'server_backend'
        if (config.containsKey("backend")) {
            testConfig.setBackend(getString(config.get("backend")));
        } else if (config.containsKey("server_backend")) {
            testConfig.setBackend(getString(config.get("server_backend")));
        }

        // Server version
        if (config.containsKey("server_version")) {
            testConfig.setServerVersion(getString(config.get("server_version")));
        }

        // Service URLs
        if (config.containsKey("mineflayer_url")) {
            testConfig.setMineflayerUrl(getString(config.get("mineflayer_url")));
        }

        if (config.containsKey("rcon_host")) {
            testConfig.setRconHost(getString(config.get("rcon_host")));
        }

        if (config.containsKey("rcon_port")) {
            testConfig.setRconPort(getInt(config.get("rcon_port")));
        }

        if (config.containsKey("rcon_password")) {
            testConfig.setRconPassword(getString(config.get("rcon_password")));
        }

        // Health checks
        if (config.containsKey("skip_health_checks")) {
            testConfig.setSkipHealthChecks(getBoolean(config.get("skip_health_checks")));
        }

        // Report configuration
        if (config.containsKey("report_directory")) {
            testConfig.setReportDir(getString(config.get("report_directory")));
        }

        // Verbose
        if (config.containsKey("verbose")) {
            testConfig.setVerbose(getBoolean(config.get("verbose")));
        }

        return testConfig;
    }

    private String getString(Object value) {
        return value != null ? value.toString() : null;
    }

    private int getInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return 0;
    }

    private boolean getBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
}
