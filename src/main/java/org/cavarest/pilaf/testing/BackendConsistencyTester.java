package org.cavarest.pilaf.testing;

import org.cavarest.pilaf.config.TestConfiguration;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.orchestrator.TestOrchestrator;
import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.backend.PilafBackendFactory;
import org.cavarest.pilaf.testing.comparator.TestResultComparator;
import org.cavarest.pilaf.testing.report.ConsistencyReportGenerator;
import org.cavarest.pilaf.testing.comparison.ConsistencyComparison;
import org.cavarest.pilaf.testing.comparison.StoryComparison;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * REAL BackendConsistencyTester that tests actual Pilaf behavior across different backend combinations.
 * This uses REAL docker-compose to start actual Minecraft servers and connect to them.
 */
public class BackendConsistencyTester {

    private static final String[] TEST_STORIES = {
        "test-story-1-basic-items.yaml",
        "test-story-2-entities.yaml",
        "test-story-3-movement.yaml",
        "test-story-4-commands.yaml"
    };

    private static final String[] CONFIG_FILES = {
        "config-docker-mineflayer.yaml",
        "config-docker-headlessmc.yaml",
        "config-headlessmc-mineflayer.yaml",
        "config-headlessmc-both.yaml"
    };

    private final TestResultComparator resultComparator;
    private final ConsistencyReportGenerator reportGenerator;
    private final Map<String, BackendTestResult> testResults;

    public BackendConsistencyTester() {
        this.resultComparator = new TestResultComparator();
        this.reportGenerator = new ConsistencyReportGenerator();
        this.testResults = new ConcurrentHashMap<>();
    }

    /**
     * Runs REAL consistency tests across all backend combinations using actual docker-compose
     */
    public ConsistencyTestResult runConsistencyTests() {
        System.out.println("Starting REAL Pilaf Backend Consistency Testing...");
        System.out.println("Testing " + TEST_STORIES.length + " stories across " + CONFIG_FILES.length + " backend combinations");

        ConsistencyTestResult overallResult = new ConsistencyTestResult();

        // Start Docker stack first
        System.out.println("\n=== Starting Docker Stack ===");
        startDockerStack();

        // Test each backend combination with REAL backends
        for (String configFile : CONFIG_FILES) {
            System.out.println("\n=== Testing Backend Combination: " + configFile + " ===");
            BackendTestResult backendResult = testBackendCombination(configFile);
            testResults.put(configFile, backendResult);

            overallResult.addBackendResult(configFile, backendResult);
        }

        // Stop Docker stack after tests
        System.out.println("\n=== Stopping Docker Stack ===");
        stopDockerStack();

        // Compare results across backends
        System.out.println("\n=== Comparing Results Across Backends ===");
        ConsistencyComparison comparison = compareResultsAcrossBackends();
        overallResult.setComparison(comparison);

        // Generate report
        System.out.println("\n=== Generating Consistency Report ===");
        String reportPath = reportGenerator.generateReport(testResults, comparison);
        System.out.println("Consistency report generated: " + reportPath);

        return overallResult;
    }

    /**
     * Starts the Docker stack using docker-compose
     */
    private void startDockerStack() {
        try {
            // Generate docker-compose.yml for testing
            generateDockerComposeFile();

            // Start the stack
            System.out.println("🚀 Starting Docker stack with docker-compose...");
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", "test-docker-compose.yml", "up", "-d");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Wait for stack to start
            Thread.sleep(30000); // 30 seconds to start

            // Check if stack is running
            if (process.isAlive()) {
                System.out.println("✅ Docker stack started successfully");
                process.destroy();
            } else {
                System.err.println("❌ Failed to start Docker stack");
            }

        } catch (Exception e) {
            System.err.println("❌ Error starting Docker stack: " + e.getMessage());
        }
    }

    /**
     * Stops the Docker stack
     */
    private void stopDockerStack() {
        try {
            System.out.println("🛑 Stopping Docker stack...");
            ProcessBuilder pb = new ProcessBuilder("docker-compose", "-f", "test-docker-compose.yml", "down");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
            System.out.println("✅ Docker stack stopped");
        } catch (Exception e) {
            System.err.println("❌ Error stopping Docker stack: " + e.getMessage());
        }
    }

    /**
     * Generates a docker-compose.yml file for testing
     */
    private void generateDockerComposeFile() {
        try {
            String composeContent =
"version: '3.8'\n" +
"services:\n" +
"  # PaperMC Minecraft Server\n" +
"  minecraft:\n" +
"    image: itzg/minecraft-server:java21\n" +
"    container_name: pilaf-minecraft\n" +
"    ports:\n" +
"      - \"25565:25565\"  # Minecraft server port\n" +
"      - \"25575:25575\"  # RCON port\n" +
"    volumes:\n" +
"      - minecraft-data:/data\n" +
"    environment:\n" +
"      # Accept EULA\n" +
"      - EULA=true\n" +
"\n" +
"      # Version and Type\n" +
"      - TYPE=PAPER\n" +
"      - PAPER_DOWNLOAD_URL=https://papermc.io/api/v2/projects/paper/versions/1.20.4/builds/419/downloads/paper-1.20.4-419.jar\n" +
"\n" +
"      # RCON Configuration\n" +
"      - ENABLE_RCON=true\n" +
"      - RCON_PASSWORD=dragon123\n" +
"      - RCON_PORT=25575\n" +
"\n" +
"      # Server Configuration\n" +
"      - SERVER_NAME=Pilaf-Test-Server\n" +
"      - MAX_PLAYERS=10\n" +
"      - ONLINE_MODE=false\n" +
"      - GAMEMODE=survival\n" +
"      - DIFFICULTY=peaceful\n" +
"\n" +
"      # Performance Settings for Testing\n" +
"      - VIEW_DISTANCE=10\n" +
"      - SIMULATION_DISTANCE=10\n" +
"\n" +
"  # Mineflayer Bridge Service\n" +
"  mineflayer:\n" +
"    image: node:18-alpine\n" +
"    container_name: pilaf-mineflayer\n" +
"    ports:\n" +
"      - \"3000:3000\"  # Pilaf API port\n" +
"    volumes:\n" +
"      - ./mineflayer-bridge:/app\n" +
"    working_dir: /app\n" +
"    environment:\n" +
"      # Minecraft Connection\n" +
"      - MINECRAFT_HOST=minecraft\n" +
"      - MINECRAFT_PORT=25565\n" +
"\n" +
"      # RCON Connection\n" +
"      - MINECRAFT_RCON_HOST=minecraft\n" +
"      - MINECRAFT_RCON_PORT=25575\n" +
"      - MINECRAFT_RCON_PASSWORD=dragon123\n" +
"\n" +
"      # Bridge Configuration\n" +
"      - BRIDGE_LOG_LEVEL=INFO\n" +
"      - BRIDGE_PLAYER_NAME=pilaf_tester\n" +
"      - BRIDGE_AUTO_RECONNECT=true\n" +
"      - BRIDGE_CONNECTION_TIMEOUT=30000\n" +
"\n" +
"      # API Configuration\n" +
"      - API_HOST=0.0.0.0\n" +
"      - API_PORT=3000\n" +
"      - API_RATE_LIMIT=100\n" +
"    command: sh -c \"npm install && node server.js\"\n" +
"    depends_on:\n" +
"      minecraft:\n" +
"        condition: service_healthy\n" +
"\n" +
"volumes:\n" +
"  minecraft-data:\n" +
"    driver: local\n";

            try (FileWriter writer = new FileWriter("test-docker-compose.yml")) {
                writer.write(composeContent);
            }

            System.out.println("📄 Generated test-docker-compose.yml");

        } catch (IOException e) {
            System.err.println("❌ Error generating docker-compose file: " + e.getMessage());
        }
    }

    /**
     * Tests a specific backend combination using REAL Pilaf backend infrastructure
     */
    private BackendTestResult testBackendCombination(String configFile) {
        BackendTestResult result = new BackendTestResult(configFile);

        try {
            // Load configuration using real ConfigLoader
            TestConfiguration config = loadConfiguration(configFile);
            if (config == null) {
                result.addError("Failed to load configuration: " + configFile);
                return result;
            }

            // Wait for Docker stack to be ready
            waitForServices(config);

            // Test each story with REAL TestOrchestrator
            for (String storyFile : TEST_STORIES) {
                System.out.println("  Testing story: " + storyFile);
                TestResult storyResult = testStoryWithBackend(storyFile, config);
                result.addStoryResult(storyFile, storyResult);
            }

            result.setSuccessful(true);

        } catch (Exception e) {
            System.err.println("Error testing backend combination " + configFile + ": " + e.getMessage());
            result.addError("Backend testing failed: " + e.getMessage());
            result.setSuccessful(false);
        }

        return result;
    }

    /**
     * Waits for Docker services to be ready
     */
    private void waitForServices(TestConfiguration config) {
        System.out.println("    ⏳ Waiting for services to be ready...");

        // Wait for Minecraft server
        if (config.getBackend().contains("docker") || config.getBackend().contains("headlessmc")) {
            try {
                Thread.sleep(60000); // 60 seconds for server to start
                System.out.println("    ✅ Minecraft server should be ready");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for Mineflayer bridge
        if (config.getBackend().contains("mineflayer")) {
            try {
                Thread.sleep(30000); // 30 seconds for bridge to start
                System.out.println("    ✅ Mineflayer bridge should be ready");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Tests a single story with a REAL backend configuration using actual TestOrchestrator
     */
    private TestResult testStoryWithBackend(String storyFile, TestConfiguration config) {
        long startTime = System.currentTimeMillis();

        try {
            // Setup timeout based on backend type
            long timeoutMs = getTimeoutForBackend(config);

            // Execute test with REAL TestOrchestrator and PilafBackend
            CompletableFuture<TestResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Create REAL PilafBackend using the factory
                    PilafBackend backend = PilafBackendFactory.create(
                        config.getBackend(),
                        config.getRconHost(),
                        config.getRconPort(),
                        config.getRconPassword()
                    );

                    // Initialize REAL backend (which sets up RCON)
                    backend.initialize();

                    // For Docker backends, we need to actually launch the server
                    if (backend instanceof org.cavarest.pilaf.backend.DockerServerBackend) {
                        System.out.println("    🚀 Connecting to Docker PaperMC server...");
                        ((org.cavarest.pilaf.backend.DockerServerBackend) backend)
                            .launchServer(config.getServerVersion() != null ? config.getServerVersion() : "1.20.4");

                        // Wait for server to start and show logs
                        System.out.println("    📋 Docker server logs:");
                        System.out.println("    " + "=".repeat(50));
                        System.out.println(((org.cavarest.pilaf.backend.DockerServerBackend) backend).getServerLogs());
                        System.out.println("    " + "=".repeat(50));

                        // Check if server is actually running
                        if (((org.cavarest.pilaf.backend.DockerServerBackend) backend).isServerRunning()) {
                            System.out.println("    ✅ Docker server is running and ready");
                        } else {
                            System.out.println("    ❌ Docker server failed to start");
                            throw new RuntimeException("Docker server failed to start");
                        }
                    }

                    // Create REAL TestOrchestrator with actual backend
                    TestOrchestrator orchestrator = new TestOrchestrator(backend);
                    orchestrator.setVerbose(true);

                    // Load and execute REAL test story using correct API
                    orchestrator.loadStory(storyFile);
                    return orchestrator.execute();

                } catch (Exception e) {
                    TestResult errorResult = new TestResult(storyFile);
                    errorResult.fail("Real backend test execution failed: " + e.getMessage());
                    errorResult.setError(e);
                    return errorResult;
                }
            });

            TestResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);

            long endTime = System.currentTimeMillis();
            result.setExecutionTimeMs(endTime - startTime);

            return result;

        } catch (Exception e) {
            TestResult errorResult = new TestResult(storyFile);
            errorResult.fail("Test execution failed: " + e.getMessage());
            errorResult.setError(e);
            return errorResult;
        }
    }

    /**
     * Compares results across different backend combinations
     */
    private ConsistencyComparison compareResultsAcrossBackends() {
        ConsistencyComparison comparison = new ConsistencyComparison();

        // Compare each story across all backends
        for (String storyFile : TEST_STORIES) {
            StoryComparison storyComparison = compareStoryAcrossBackends(storyFile);
            comparison.addStoryComparison(storyFile, storyComparison);
        }

        return comparison;
    }

    /**
     * Compares results of a single story across all backends
     */
    private StoryComparison compareStoryAcrossBackends(String storyFile) {
        StoryComparison comparison = new StoryComparison(storyFile);

        // Collect results for this story from all backends
        Map<String, TestResult> storyResults = new HashMap<>();
        for (Map.Entry<String, BackendTestResult> entry : testResults.entrySet()) {
            String backendConfig = entry.getKey();
            TestResult storyResult = entry.getValue().getStoryResult(storyFile);
            if (storyResult != null) {
                storyResults.put(backendConfig, storyResult);
            }
        }

        // Compare results using REAL comparison logic
        boolean isConsistent = resultComparator.areResultsConsistent(storyResults);
        comparison.setConsistent(isConsistent);

        if (!isConsistent) {
            List<String> inconsistencies = resultComparator.findInconsistencies(storyResults);
            comparison.setInconsistencies(inconsistencies);
        }

        return comparison;
    }

    /**
     * Loads configuration from file using REAL ConfigLoader
     */
    private TestConfiguration loadConfiguration(String configFile) {
        try {
            // Use REAL ConfigLoader to load configuration
            org.cavarest.pilaf.cli.ConfigLoader loader = new org.cavarest.pilaf.cli.ConfigLoader();
            return loader.loadFromFile(new java.io.File(configFile));
        } catch (Exception e) {
            System.err.println("Failed to load configuration " + configFile + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets timeout duration based on backend type
     */
    private long getTimeoutForBackend(TestConfiguration config) {
        String backend = config.getBackend();

        // Docker backends typically take longer to start
        if ("docker".equals(backend)) {
            return 300000; // 5 minutes for Docker
        } else {
            return 180000; // 3 minutes for HeadlessMc
        }
    }

    /**
     * Result class for backend combination testing
     */
    public static class BackendTestResult {
        private final String configFile;
        private final Map<String, TestResult> storyResults;
        private final List<String> errors;
        private boolean successful;

        public BackendTestResult(String configFile) {
            this.configFile = configFile;
            this.storyResults = new HashMap<>();
            this.errors = new ArrayList<>();
            this.successful = false;
        }

        public void addStoryResult(String storyFile, TestResult result) {
            storyResults.put(storyFile, result);
        }

        public TestResult getStoryResult(String storyFile) {
            return storyResults.get(storyFile);
        }

        public void addError(String error) {
            errors.add(error);
        }

        public String getConfigFile() { return configFile; }
        public Map<String, TestResult> getStoryResults() { return storyResults; }
        public List<String> getErrors() { return errors; }
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }

        public int getTotalStories() { return storyResults.size(); }
        public int getPassedStories() {
            return (int) storyResults.values().stream()
                .filter(TestResult::isSuccess)
                .count();
        }
    }

    /**
     * Overall result of consistency testing
     */
    public static class ConsistencyTestResult {
        private final Map<String, BackendTestResult> backendResults;
        private ConsistencyComparison comparison;
        private long totalExecutionTime;

        public ConsistencyTestResult() {
            this.backendResults = new HashMap<>();
        }

        public void addBackendResult(String configFile, BackendTestResult result) {
            backendResults.put(configFile, result);
        }

        public void setComparison(ConsistencyComparison comparison) {
            this.comparison = comparison;
        }

        public Map<String, BackendTestResult> getBackendResults() { return backendResults; }
        public ConsistencyComparison getComparison() { return comparison; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public void setTotalExecutionTime(long totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }

        public boolean isOverallConsistent() {
            return comparison != null && comparison.isOverallConsistent();
        }

        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Backend Consistency Test Summary:\n");
            summary.append("Total Backend Combinations: ").append(backendResults.size()).append("\n");

            int totalStories = 0;
            int totalPassed = 0;

            for (BackendTestResult result : backendResults.values()) {
                totalStories += result.getTotalStories();
                totalPassed += result.getPassedStories();
            }

            summary.append("Total Test Stories: ").append(totalStories).append("\n");
            summary.append("Total Passed: ").append(totalPassed).append("\n");
            summary.append("Overall Consistency: ").append(isOverallConsistent() ? "PASSED" : "FAILED").append("\n");

            return summary.toString();
        }
    }
}
