package org.cavarest.pilaf.config;

/**
 * Configuration for Pilaf test execution.
 * Manages connection settings and test parameters.
 */
public class TestConfiguration {

    // Mineflayer bridge settings
    private String mineflayerUrl = System.getProperty("mineflayer.url", TestPorts.DEFAULT_MINEFLAYER_URL);
    private int mineflayerTimeout = 10_000; // 10 seconds

    // RCON settings - using TestPorts as single source of truth
    private String rconHost = System.getProperty("rcon.host", TestPorts.DEFAULT_RCON_HOST);
    private int rconPort = Integer.parseInt(System.getProperty("rcon.port", String.valueOf(TestPorts.RCON_PORT)));
    private String rconPassword = System.getProperty("rcon.password", TestPorts.DEFAULT_RCON_PASSWORD);
    private int rconTimeout = 5_000; // 5 seconds

    // Test player settings
    private String testPlayer = System.getProperty("test.player", "pilaf_tester");

    // Backend settings
    private String backend = System.getProperty("pilaf.backend", "mineflayer");
    private String serverVersion = System.getProperty("pilaf.server.version", "1.21.5");

    // Report settings
    private String reportDir = System.getProperty("pilaf.report.dir", "target/pilaf-reports");
    private String reportFormat = System.getProperty("pilaf.report.format", "html");

    // Verbose output
    private boolean verbose = Boolean.parseBoolean(System.getProperty("pilaf.verbose", "false"));

    // Service health check settings
    private boolean skipHealthChecks = Boolean.parseBoolean(System.getProperty("pilaf.skip.health", "false"));
    private int healthCheckTimeout = 5_000; // 5 seconds

    public TestConfiguration() {
        // Load from environment variables if available
        mineflayerUrl = System.getenv().getOrDefault("MINEFLAYER_URL", mineflayerUrl);
        rconHost = System.getenv().getOrDefault("RCON_HOST", rconHost);
        rconPassword = System.getenv().getOrDefault("RCON_PASSWORD", rconPassword);
        testPlayer = System.getenv().getOrDefault("TEST_PLAYER", testPlayer);
    }

    // Getters
    public String getMineflayerUrl() { return mineflayerUrl; }
    public int getMineflayerTimeout() { return mineflayerTimeout; }
    public String getRconHost() { return rconHost; }
    public int getRconPort() { return rconPort; }
    public String getRconPassword() { return rconPassword; }
    public int getRconTimeout() { return rconTimeout; }
    public String getTestPlayer() { return testPlayer; }
    public String getBackend() { return backend; }
    public String getServerVersion() { return serverVersion; }
    public String getReportDir() { return reportDir; }
    public String getReportFormat() { return reportFormat; }
    public boolean isVerbose() { return verbose; }
    public boolean isSkipHealthChecks() { return skipHealthChecks; }
    public int getHealthCheckTimeout() { return healthCheckTimeout; }

    // Setters for programmatic configuration
    public TestConfiguration setMineflayerUrl(String url) {
        this.mineflayerUrl = url;
        return this;
    }

    public TestConfiguration setRconHost(String host) {
        this.rconHost = host;
        return this;
    }

    public TestConfiguration setRconPort(int port) {
        this.rconPort = port;
        return this;
    }

    public TestConfiguration setRconPassword(String password) {
        this.rconPassword = password;
        return this;
    }

    public TestConfiguration setTestPlayer(String player) {
        this.testPlayer = player;
        return this;
    }

    public TestConfiguration setBackend(String backend) {
        this.backend = backend;
        return this;
    }

    public TestConfiguration setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
        return this;
    }

    public TestConfiguration setReportDir(String dir) {
        this.reportDir = dir;
        return this;
    }

    public TestConfiguration setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public TestConfiguration setSkipHealthChecks(boolean skip) {
        this.skipHealthChecks = skip;
        return this;
    }

    @Override
    public String toString() {
        return String.format("TestConfiguration{mineflayerUrl='%s', rconHost='%s:%d', testPlayer='%s', backend='%s', serverVersion='%s'}",
            mineflayerUrl, rconHost, rconPort, testPlayer, backend, serverVersion);
    }
}
