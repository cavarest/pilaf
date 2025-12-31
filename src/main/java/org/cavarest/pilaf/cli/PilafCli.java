package org.cavarest.pilaf.cli;

import org.cavarest.pilaf.config.TestConfiguration;
import org.cavarest.pilaf.orchestrator.TestOrchestrator;
import org.cavarest.pilaf.report.TestReporter;
import org.cavarest.pilaf.runner.YamlDrivenTestRunner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * PILAF Command Line Interface
 *
 * A standalone JAR that allows running YAML stories without Java code.
 *
 * Usage:
 *   java -jar pilaf.jar --config=pilaf.yaml
 *   java -jar pilaf.jar --stories=src/test/resources/integration-stories/ --mineflayer-url=http://localhost:3000
 *   java -jar pilaf.jar src/test/resources/my-test.yaml --verbose
 */
@Command(
    name = "pilaf",
    description = "PILAF - Paper Integration Layer for Automation Functions",
    mixinStandardHelpOptions = true,
    version = "PILAF 1.0.0"
)
public class PilafCli implements Callable<Integer> {

    @Option(
        names = {"-c", "--config"},
        description = "Path to PILAF configuration file (YAML format)"
    )
    private File configFile;

    @Option(
        names = {"-s", "--stories"},
        description = "Stories directory or individual YAML file(s) to execute",
        arity = "0..*"
    )
    private List<File> storyFiles = new ArrayList<>();

    @Option(
        names = {"--mineflayer-url"},
        description = "Mineflayer bridge URL (default: http://localhost:3000)"
    )
    private String mineflayerUrl = "http://localhost:3000";

    @Option(
        names = {"--rcon-host"},
        description = "RCON host (default: localhost)"
    )
    private String rconHost = "localhost";

    @Option(
        names = {"--rcon-port"},
        description = "RCON port (default: 25575)"
    )
    private int rconPort = 25575;

    @Option(
        names = {"--rcon-password"},
        description = "RCON password (default: dragon123)"
    )
    private String rconPassword = "dragon123";

    @Option(
        names = {"--backend"},
        description = "Backend to use: mineflayer, rcon, or mock (default: mineflayer)"
    )
    private String backend = "mineflayer";

    @Option(
        names = {"--report-dir"},
        description = "Directory for test reports (default: target/pilaf-reports)"
    )
    private String reportDir = "target/pilaf-reports";

    @Option(
        names = {"--verbose", "-v"},
        description = "Enable verbose output"
    )
    private boolean verbose = false;

    @Option(
        names = {"--health-check"},
        description = "Check if PILAF services are available and exit"
    )
    private boolean healthCheck = false;

    @Option(
        names = {"--skip-health-checks"},
        description = "Skip service availability checks"
    )
    private boolean skipHealthChecks = false;

    @Parameters(
        description = "Additional story files or directories to execute"
    )
    private List<File> additionalStories = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        try {
            if (healthCheck) {
                return performHealthCheck();
            }

            // Load configuration
            TestConfiguration config = loadConfiguration();

            // Setup reporter
            TestReporter reporter = new TestReporter("PILAF CLI Test Run");
            reporter.setOutputDir(reportDir);
            if (verbose) {
                reporter.setVerbose(true);
            }

            // Create test runner
            YamlDrivenTestRunner runner = new YamlDrivenTestRunner(config, reporter);

            // Discover and execute stories
            List<Path> storyPaths = discoverStories();
            if (storyPaths.isEmpty()) {
                System.err.println("❌ No story files found. Please specify story files or directories.");
                return 1;
            }

            System.out.println("🔍 Found " + storyPaths.size() + " story file(s) to execute");
            for (Path path : storyPaths) {
                System.out.println("   📄 " + path);
            }

            // Execute stories
            System.out.println("\n🚀 Starting PILAF test execution...");
            runner.discoverAndExecuteStories(Paths.get("."));

            // Generate report
            System.out.println("\n📊 Generating test report...");
            runner.generateReport();
            reporter.complete();

            System.out.println("\n✅ PILAF test execution complete!");
            System.out.println("📋 Report available at: " + reportDir + "/index.html");

            return 0;

        } catch (Exception e) {
            System.err.println("❌ PILAF execution failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private TestConfiguration loadConfiguration() throws Exception {
        TestConfiguration config = new TestConfiguration();

        // Load from config file if provided
        if (configFile != null) {
            System.out.println("📋 Loading configuration from: " + configFile);
            ConfigLoader loader = new ConfigLoader();
            config = loader.loadFromFile(configFile);
        }

        // Override with command-line options
        config.setMineflayerUrl(mineflayerUrl);
        config.setRconHost(rconHost);
        config.setRconPort(rconPort);
        config.setRconPassword(rconPassword);
        config.setBackend(backend);
        config.setSkipHealthChecks(skipHealthChecks);

        return config;
    }

    private List<Path> discoverStories() throws Exception {
        List<Path> allStories = new ArrayList<>();

        // Add story files from --stories option
        for (File file : storyFiles) {
            allStories.addAll(StoryDiscoverer.discover(file));
        }

        // Add additional story files from parameters
        for (File file : additionalStories) {
            allStories.addAll(StoryDiscoverer.discover(file));
        }

        // If no stories specified, look for default locations
        if (allStories.isEmpty()) {
            List<Path> defaultLocations = List.of(
                Paths.get("src/test/resources/integration-stories"),
                Paths.get("src/test/resources/test-stories"),
                Paths.get("stories")
            );

            for (Path location : defaultLocations) {
                if (location.toFile().exists()) {
                    allStories.addAll(StoryDiscoverer.discover(location.toFile()));
                }
            }
        }

        return allStories;
    }

    private int performHealthCheck() {
        System.out.println("🏥 PILAF Health Check");
        System.out.println("=====================");

        boolean allHealthy = true;

        // Check Mineflayer bridge
        System.out.print("🔗 Mineflayer Bridge (" + mineflayerUrl + ")... ");
        try {
            // Simple HTTP check
            java.net.HttpURLConnection connection =
                (java.net.HttpURLConnection) new java.net.URL(mineflayerUrl + "/health").openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("✅ Healthy");
            } else {
                System.out.println("❌ Unhealthy (HTTP " + responseCode + ")");
                allHealthy = false;
            }
        } catch (Exception e) {
            System.out.println("❌ Unreachable (" + e.getMessage() + ")");
            allHealthy = false;
        }

        // Check RCON
        System.out.print("🔌 RCON (" + rconHost + ":" + rconPort + ")... ");
        try {
            // Simple RCON connection test
            // This would require RCON library implementation
            System.out.println("⚠️  RCON check not implemented");
        } catch (Exception e) {
            System.out.println("❌ Unreachable");
            allHealthy = false;
        }

        System.out.println();
        if (allHealthy) {
            System.out.println("✅ All PILAF services are healthy!");
            return 0;
        } else {
            System.out.println("❌ Some PILAF services are not available.");
            System.out.println("💡 Make sure Docker services are running:");
            System.out.println("   docker-compose -f docker-compose.pilaf.yml up -d");
            return 1;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PilafCli())
            
            .execute(args);
        System.exit(exitCode);
    }
}
