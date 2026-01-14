package org.cavarest.pilaf.testing.report;

import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.testing.BackendConsistencyTester;
import org.cavarest.pilaf.testing.comparison.ConsistencyComparison;
import org.cavarest.pilaf.testing.comparison.StoryComparison;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConsistencyReportGenerator.
 */
@DisplayName("ConsistencyReportGenerator Tests")
class ConsistencyReportGeneratorTest {

    private ConsistencyReportGenerator generator;
    private Map<String, BackendConsistencyTester.BackendTestResult> testResults;
    private ConsistencyComparison comparison;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new ConsistencyReportGenerator();
        testResults = new HashMap<>();
        comparison = new ConsistencyComparison();
    }

    @Test
    @DisplayName("generateReport() creates both HTML and text reports")
    void testGenerateReport_createsBothReports() {
        // Setup test data
        setupBasicTestData();

        // Change to temp directory for report generation
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        // Verify HTML report was created
        assertNotNull(htmlReportPath);
        assertTrue(htmlReportPath.endsWith(".html"));

        File htmlFile = new File(htmlReportPath);
        assertTrue(htmlFile.exists(), "HTML report file should exist");

        // Verify corresponding text report exists
        String textReportPath = htmlReportPath.replace(".html", ".txt");
        File textFile = new File(textReportPath);
        assertTrue(textFile.exists(), "Text report file should exist");

        // Clean up
        if (htmlFile.exists()) htmlFile.delete();
        if (textFile.exists()) textFile.delete();
    }

    @Test
    @DisplayName("generateReport() returns HTML report path")
    void testGenerateReport_returnsHtmlPath() {
        setupBasicTestData();
        System.setProperty("user.dir", tempDir.toString());

        String reportPath = generator.generateReport(testResults, comparison);

        assertNotNull(reportPath);
        assertTrue(reportPath.contains("consistency-report-"));
        assertTrue(reportPath.endsWith(".html"));

        // Clean up
        new File(reportPath).delete();
        new File(reportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("HTML report contains required content")
    void testHtmlReport_containsRequiredContent() throws Exception {
        setupBasicTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        // Read HTML report content
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();

        // Verify HTML structure
        assertTrue(htmlContent.contains("<!DOCTYPE html>"));
        assertTrue(htmlContent.contains("<html"));
        assertTrue(htmlContent.contains("Pilaf Backend Consistency Report"));
        assertTrue(htmlContent.contains("Executive Summary"));
        assertTrue(htmlContent.contains("Backend Test Results"));
        assertTrue(htmlContent.contains("Consistency Analysis"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("HTML report shows PASSED status for consistent results")
    void testHtmlReport_showsPassedStatusForConsistent() throws Exception {
        setupConsistentTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();
        assertTrue(htmlContent.contains("PASSED"));
        assertTrue(htmlContent.contains("success"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("HTML report shows FAILED status for inconsistent results")
    void testHtmlReport_showsFailedStatusForInconsistent() throws Exception {
        setupInconsistentTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();
        assertTrue(htmlContent.contains("FAILED"));
        assertTrue(htmlContent.contains("failure"));
        assertTrue(htmlContent.contains("INCONSISTENT"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("Text report contains required sections")
    void testTextReport_containsRequiredSections() throws Exception {
        setupBasicTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);
        String textReportPath = htmlReportPath.replace(".html", ".txt");

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(textReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String textContent = content.toString();

        // Verify text report sections
        assertTrue(textContent.contains("Pilaf BACKEND CONSISTENCY REPORT"));
        assertTrue(textContent.contains("EXECUTIVE SUMMARY"));
        assertTrue(textContent.contains("BACKEND TEST RESULTS"));
        assertTrue(textContent.contains("CONSISTENCY ANALYSIS"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(textReportPath).delete();
    }

    @Test
    @DisplayName("HTML report includes backend metrics")
    void testHtmlReport_includesBackendMetrics() throws Exception {
        setupMultipleBackendTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();

        // Verify metrics are shown
        assertTrue(htmlContent.contains("Backend Combinations"));
        assertTrue(htmlContent.contains("Test Stories"));
        assertTrue(htmlContent.contains("Consistent Stories"));
        assertTrue(htmlContent.contains("Consistency Rate"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("HTML report includes backend results table")
    void testHtmlReport_includesBackendResultsTable() throws Exception {
        setupBasicTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();

        // Verify table headers
        assertTrue(htmlContent.contains("Story File"));
        assertTrue(htmlContent.contains("Success"));
        assertTrue(htmlContent.contains("Assertions Passed"));
        assertTrue(htmlContent.contains("Assertions Failed"));
        assertTrue(htmlContent.contains("Execution Time"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("generateReport() handles empty test results")
    void testGenerateReport_handlesEmptyResults() {
        comparison = new ConsistencyComparison();
        testResults = new HashMap<>();
        System.setProperty("user.dir", tempDir.toString());

        String reportPath = generator.generateReport(testResults, comparison);

        assertNotNull(reportPath);
        assertTrue(reportPath.endsWith(".html"));

        // Verify files were created even with empty data
        File htmlFile = new File(reportPath);
        assertTrue(htmlFile.exists());

        // Clean up
        htmlFile.delete();
        new File(reportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("HTML report includes inconsistency details when present")
    void testHtmlReport_includesInconsistencyDetails() throws Exception {
        setupInconsistentTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();
        assertTrue(htmlContent.contains("Inconsistencies"));
        assertTrue(htmlContent.contains("Test inconsistency"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("Text report shows consistency percentage")
    void testTextReport_showsConsistencyPercentage() throws Exception {
        setupConsistentTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);
        String textReportPath = htmlReportPath.replace(".html", ".txt");

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(textReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String textContent = content.toString();
        assertTrue(textContent.contains("Consistent Stories"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(textReportPath).delete();
    }

    @Test
    @DisplayName("HTML report includes error section when backend has errors")
    void testHtmlReport_includesErrorSection() throws Exception {
        setupBackendWithErrors();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();
        assertTrue(htmlContent.contains("Errors"));
        assertTrue(htmlContent.contains("Test error message"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("generateReport() handles multiple backend configurations")
    void testGenerateReport_handlesMultipleBackends() {
        setupMultipleBackendTestData();
        System.setProperty("user.dir", tempDir.toString());

        String reportPath = generator.generateReport(testResults, comparison);

        assertNotNull(reportPath);
        assertTrue(new File(reportPath).exists());

        // Clean up
        new File(reportPath).delete();
        new File(reportPath.replace(".html", ".txt")).delete();
    }

    @Test
    @DisplayName("Text report includes backend status information")
    void testTextReport_includesBackendStatus() throws Exception {
        setupBasicTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);
        String textReportPath = htmlReportPath.replace(".html", ".txt");

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(textReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String textContent = content.toString();
        assertTrue(textContent.contains("Status:"));
        assertTrue(textContent.contains("Stories tested:"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(textReportPath).delete();
    }

    @Test
    @DisplayName("HTML report contains timestamp")
    void testHtmlReport_containsTimestamp() throws Exception {
        setupBasicTestData();
        System.setProperty("user.dir", tempDir.toString());

        String htmlReportPath = generator.generateReport(testResults, comparison);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlReportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String htmlContent = content.toString();
        assertTrue(htmlContent.contains("Report generated:"));

        // Clean up
        new File(htmlReportPath).delete();
        new File(htmlReportPath.replace(".html", ".txt")).delete();
    }

    /**
     * Helper method to setup basic test data
     */
    private void setupBasicTestData() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("config-test.yaml");

        TestResult testResult = new TestResult("test-story.yaml");
        testResult.setSuccess(true);
        setAssertionsPassed(testResult, 5);
        setAssertionsFailed(testResult, 0);
        testResult.setExecutionTimeMs(100L);

        result.addStoryResult("test-story.yaml", testResult);
        result.setSuccessful(true);

        testResults.put("config-test.yaml", result);

        StoryComparison storyComparison = new StoryComparison("test-story.yaml");
        storyComparison.setConsistent(true);

        comparison.addStoryComparison("test-story.yaml", storyComparison);
    }

    /**
     * Helper method to setup consistent test data
     */
    private void setupConsistentTestData() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("config-mineflayer.yaml");

        TestResult testResult = new TestResult("test-story-1.yaml");
        testResult.setSuccess(true);
        setAssertionsPassed(testResult, 10);
        setAssertionsFailed(testResult, 0);
        testResult.setExecutionTimeMs(200L);

        result.addStoryResult("test-story-1.yaml", testResult);
        result.setSuccessful(true);

        testResults.put("config-mineflayer.yaml", result);

        StoryComparison storyComparison = new StoryComparison("test-story-1.yaml");
        storyComparison.setConsistent(true);

        comparison.addStoryComparison("test-story-1.yaml", storyComparison);
    }

    /**
     * Helper method to setup inconsistent test data
     */
    private void setupInconsistentTestData() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("config-test.yaml");

        TestResult testResult = new TestResult("test-story.yaml");
        testResult.setSuccess(false);
        setAssertionsPassed(testResult, 3);
        setAssertionsFailed(testResult, 2);
        testResult.setExecutionTimeMs(150L);

        result.addStoryResult("test-story.yaml", testResult);
        result.setSuccessful(false);

        testResults.put("config-test.yaml", result);

        StoryComparison storyComparison = new StoryComparison("test-story.yaml");
        storyComparison.setConsistent(false);
        storyComparison.setInconsistencies(new java.util.ArrayList<>(List.of("Test inconsistency")));

        comparison.addStoryComparison("test-story.yaml", storyComparison);
    }

    /**
     * Helper method to setup multiple backend test data
     */
    private void setupMultipleBackendTestData() {
        // First backend
        BackendConsistencyTester.BackendTestResult result1 =
            new BackendConsistencyTester.BackendTestResult("config-mineflayer.yaml");

        TestResult testResult1 = new TestResult("story-1.yaml");
        testResult1.setSuccess(true);
        setAssertionsPassed(testResult1, 5);
        setAssertionsFailed(testResult1, 0);
        testResult1.setExecutionTimeMs(100L);

        result1.addStoryResult("story-1.yaml", testResult1);
        result1.setSuccessful(true);

        testResults.put("config-mineflayer.yaml", result1);

        // Second backend
        BackendConsistencyTester.BackendTestResult result2 =
            new BackendConsistencyTester.BackendTestResult("config-rcon.yaml");

        TestResult testResult2 = new TestResult("story-1.yaml");
        testResult2.setSuccess(true);
        setAssertionsPassed(testResult2, 5);
        setAssertionsFailed(testResult2, 0);
        testResult2.setExecutionTimeMs(95L);

        result2.addStoryResult("story-1.yaml", testResult2);
        result2.setSuccessful(true);

        testResults.put("config-rcon.yaml", result2);

        // Consistent comparison
        StoryComparison storyComparison = new StoryComparison("story-1.yaml");
        storyComparison.setConsistent(true);

        comparison.addStoryComparison("story-1.yaml", storyComparison);
    }

    /**
     * Helper method to setup backend with errors
     */
    private void setupBackendWithErrors() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("config-error.yaml");

        TestResult testResult = new TestResult("test-story.yaml");
        testResult.setSuccess(false);
        setAssertionsPassed(testResult, 0);
        setAssertionsFailed(testResult, 1);
        testResult.setExecutionTimeMs(50L);

        result.addStoryResult("test-story.yaml", testResult);
        result.addError("Test error message");
        result.setSuccessful(false);

        testResults.put("config-error.yaml", result);

        StoryComparison storyComparison = new StoryComparison("test-story.yaml");
        storyComparison.setConsistent(false);

        comparison.addStoryComparison("test-story.yaml", storyComparison);
    }

    /**
     * Helper method to set assertions passed using reflection
     */
    private void setAssertionsPassed(TestResult result, int value) {
        try {
            var field = result.getClass().getDeclaredField("assertionsPassed");
            field.setAccessible(true);
            field.set(result, value);
        } catch (Exception e) {
            // Ignore if reflection fails
        }
    }

    /**
     * Helper method to set assertions failed using reflection
     */
    private void setAssertionsFailed(TestResult result, int value) {
        try {
            var field = result.getClass().getDeclaredField("assertionsFailed");
            field.setAccessible(true);
            field.set(result, value);
        } catch (Exception e) {
            // Ignore if reflection fails
        }
    }
}
