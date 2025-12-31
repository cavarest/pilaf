package org.cavarest.pilaf.runner;

import org.cavarest.pilaf.config.TestConfiguration;
import org.cavarest.pilaf.discovery.TestDiscovery;
import org.cavarest.pilaf.execution.YamlStoryExecutor;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.report.TestReporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML-Driven Test Runner
 *
 * This is the correct PILAF architecture:
 * - Discovers YAML test stories from plugin repositories
 * - Executes stories as integration tests
 * - Generates comprehensive reports
 *
 * Plugin repositories use this as a Maven dependency.
 */
public class YamlDrivenTestRunner {

    private final TestConfiguration config;
    private final TestReporter reporter;
    private final TestDiscovery discovery;
    private final YamlStoryExecutor executor;
    private final List<TestResult> results = new ArrayList<>();

    public YamlDrivenTestRunner(TestConfiguration config, TestReporter reporter) {
        this.config = config;
        this.reporter = reporter;
        this.discovery = new TestDiscovery();
        this.executor = new YamlStoryExecutor(config, reporter);
    }

    /**
     * Discover and execute all YAML test stories from plugin repository
     */
    public void discoverAndExecuteStories(Path testStoriesDir) {
        if (!Files.exists(testStoriesDir)) {
            System.out.println("Test stories directory not found: " + testStoriesDir);
            return;
        }

        System.out.println("🔍 Discovering YAML test stories in: " + testStoriesDir);

        List<Path> storyFiles = discovery.discoverYamlStories(testStoriesDir);
        System.out.println("Found " + storyFiles.size() + " YAML test stories");

        for (Path storyFile : storyFiles) {
            executeStoryFile(storyFile);
        }
    }

    /**
     * Execute a single YAML story file as a test
     */
    private void executeStoryFile(Path storyFile) {
        String storyName = storyFile.getFileName().toString().replace(".yaml", "");
        String testDisplayName = formatTestName(storyName);

        System.out.println("🧪 Executing test: " + testDisplayName);

        try {
            TestResult result = executor.executeStory(storyFile.toString());
            result.setTestName(testDisplayName);
            result.setStoryFile(storyFile.toString());

            results.add(result);

            String status = result.isPassed() ? "✅ PASSED" : "❌ FAILED";
            System.out.println("  " + status + " - " + testDisplayName);

            if (!result.isPassed() && result.getMessage() != null) {
                System.out.println("    Error: " + result.getMessage());
            }

        } catch (Exception e) {
            System.out.println("  ❌ ERROR - " + testDisplayName);
            System.out.println("    Exception: " + e.getMessage());

            TestResult errorResult = new TestResult(storyName);
            errorResult.setSuccess(false);
            errorResult.setMessage("Execution failed: " + e.getMessage());
            errorResult.setTestName(testDisplayName);
            errorResult.setStoryFile(storyFile.toString());
            results.add(errorResult);
        }
    }

    /**
     * Format YAML filename into readable test name
     */
    private String formatTestName(String storyName) {
        // Convert snake_case to Title Case
        String result = storyName.replace("-", " ")
                               .replace("_", " ")
                               .trim()
                               .toLowerCase();

        // Capitalize first letter of each word
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : result.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
                if (Character.isWhitespace(c)) {
                    capitalizeNext = true;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Get all test results
     */
    public List<TestResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Generate final test report
     */
    public void generateReport() {
        try {
            if (reporter != null) {
                reporter.complete();
            }
        } catch (Exception e) {
            System.out.println("Error generating report: " + e.getMessage());
        }

        // Print summary
        long passed = 0;
        for (TestResult result : results) {
            if (result.isPassed()) passed++;
        }
        long failed = results.size() - passed;

        System.out.println("\n============================================================");
        System.out.println("YAML STORY TEST SUMMARY");
        System.out.println("============================================================");
        System.out.println("Total Tests: " + results.size());
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (results.size() > 0) {
            System.out.println("Success Rate: " + (passed * 100 / results.size()) + "%");
        } else {
            System.out.println("Success Rate: 0%");
        }
        System.out.println("============================================================");
    }
}
