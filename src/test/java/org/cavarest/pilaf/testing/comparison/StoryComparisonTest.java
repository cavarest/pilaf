package org.cavarest.pilaf.testing.comparison;

import org.cavarest.pilaf.model.TestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StoryComparison
 */
@DisplayName("StoryComparison Tests")
class StoryComparisonTest {

    private StoryComparison comparison;

    @BeforeEach
    void setUp() {
        comparison = new StoryComparison("test-story.yaml");
    }

    // CONSTRUCTOR TESTS

    @Test
    @DisplayName("constructor creates StoryComparison with story file")
    void testConstructor() {
        assertEquals("test-story.yaml", comparison.getStoryFile());
        assertTrue(comparison.isConsistent());
        assertTrue(comparison.getInconsistencies().isEmpty());
    }

    @Test
    @DisplayName("constructor with different story file")
    void testConstructor_differentStoryFile() {
        StoryComparison comp = new StoryComparison("another-story.yaml");
        assertEquals("another-story.yaml", comp.getStoryFile());
    }

    // SETCONSISTENT TESTS

    @Test
    @DisplayName("setConsistent sets consistent flag")
    void testSetConsistent() {
        comparison.setConsistent(true);
        assertTrue(comparison.isConsistent());

        comparison.setConsistent(false);
        assertFalse(comparison.isConsistent());
    }

    // SETINCONSISTENCIES TESTS

    @Test
    @DisplayName("setInconsistencies sets inconsistencies list")
    void testSetInconsistencies() {
        List<String> inconsistencies = List.of("Issue 1", "Issue 2");
        comparison.setInconsistencies(inconsistencies);

        List<String> result = comparison.getInconsistencies();
        assertEquals(2, result.size());
        assertTrue(result.contains("Issue 1"));
        assertTrue(result.contains("Issue 2"));
    }

    @Test
    @DisplayName("setInconsistencies with null uses empty list")
    void testSetInconsistencies_null() {
        comparison.setInconsistencies(null);

        assertTrue(comparison.getInconsistencies().isEmpty());
    }

    // ADDBACKENDRESULT TESTS

    @Test
    @DisplayName("addBackendResult adds result")
    void testAddBackendResult() {
        TestResult result = new TestResult("test");
        result.setSuccess(true);
        comparison.addBackendResult("rcon", result);

        assertEquals(result, comparison.getBackendResult("rcon"));
    }

    @Test
    @DisplayName("addBackendResult with multiple backends")
    void testAddBackendResult_multiple() {
        TestResult rconResult = new TestResult("test");
        rconResult.setSuccess(true);
        comparison.addBackendResult("rcon", rconResult);

        TestResult mineflayerResult = new TestResult("test");
        mineflayerResult.setSuccess(true);
        comparison.addBackendResult("mineflayer", mineflayerResult);

        assertEquals(rconResult, comparison.getBackendResult("rcon"));
        assertEquals(mineflayerResult, comparison.getBackendResult("mineflayer"));
    }

    // GETBACKENDRESULT TESTS

    @Test
    @DisplayName("getBackendResult returns null for non-existent backend")
    void testGetBackendResult_nonExistent() {
        assertNull(comparison.getBackendResult("non-existent"));
    }

    // GETBACKENDRESULTS TESTS

    @Test
    @DisplayName("getBackendResults returns unmodifiable map")
    void testGetBackendResults_unmodifiable() {
        TestResult result = new TestResult("test");
        comparison.addBackendResult("rcon", result);

        Map<String, TestResult> results = comparison.getBackendResults();

        assertThrows(UnsupportedOperationException.class, () -> results.put("other", result));
    }

    @Test
    @DisplayName("getBackendResults returns empty map when no results")
    void testGetBackendResults_empty() {
        Map<String, TestResult> results = comparison.getBackendResults();
        assertTrue(results.isEmpty());
    }

    // GETINCONSISTENCIES TESTS

    @Test
    @DisplayName("getInconsistencies returns unmodifiable list")
    void testGetInconsistencies_unmodifiable() {
        comparison.setInconsistencies(List.of("issue"));

        List<String> inconsistencies = comparison.getInconsistencies();

        assertThrows(UnsupportedOperationException.class, () -> inconsistencies.add("another"));
    }

    // GETSUMMARY TESTS

    @Test
    @DisplayName("getSummary returns summary with correct values")
    void testGetSummary() {
        TestResult result1 = new TestResult("test");
        result1.setSuccess(true);
        comparison.addBackendResult("rcon", result1);

        TestResult result2 = new TestResult("test");
        result2.setSuccess(false);
        comparison.addBackendResult("mineflayer", result2);

        StoryComparison.StoryComparisonSummary summary = comparison.getSummary();

        assertEquals("test-story.yaml", summary.getStoryFile());
        assertEquals(2, summary.getTotalBackends());
        assertEquals(1, summary.getPassedBackends());
        assertEquals(1, summary.getFailedBackends());
    }

    @Test
    @DisplayName("getSummary with no backend results")
    void testGetSummary_noResults() {
        StoryComparison.StoryComparisonSummary summary = comparison.getSummary();

        assertEquals("test-story.yaml", summary.getStoryFile());
        assertEquals(0, summary.getTotalBackends());
        assertEquals(0, summary.getPassedBackends());
        assertEquals(0, summary.getFailedBackends());
    }

    // GENERATEDETAILEDREPORT TESTS

    @Test
    @DisplayName("generateDetailedReport contains story file name")
    void testGenerateDetailedReport_containsStoryFile() {
        String report = comparison.generateDetailedReport();
        assertTrue(report.contains("test-story.yaml"));
    }

    @Test
    @DisplayName("generateDetailedReport contains backend results")
    void testGenerateDetailedReport_containsBackendResults() {
        TestResult result = new TestResult("test");
        result.setSuccess(true);
        result.setExecutionTimeMs(1000);
        comparison.addBackendResult("rcon", result);

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("rcon"));
        assertTrue(report.contains("Success: true"));
        assertTrue(report.contains("Execution Time: 1000ms"));
    }

    @Test
    @DisplayName("generateDetailedReport shows consistent status")
    void testGenerateDetailedReport_consistentStatus() {
        String report = comparison.generateDetailedReport();
        assertTrue(report.contains("CONSISTENT"));
    }

    @Test
    @DisplayName("generateDetailedReport shows inconsistent status")
    void testGenerateDetailedReport_inconsistentStatus() {
        comparison.setConsistent(false);
        comparison.setInconsistencies(List.of("Test failed"));

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("INCONSISTENT"));
        assertTrue(report.contains("Test failed"));
    }

    @Test
    @DisplayName("generateDetailedReport shows no inconsistencies message")
    void testGenerateDetailedReport_noInconsistencies() {
        String report = comparison.generateDetailedReport();
        assertTrue(report.contains("No inconsistencies detected"));
    }

    // GETPERFORMANCECOMPARISON TESTS

    @Test
    @DisplayName("getPerformanceComparison returns comparison with execution times")
    void testGetPerformanceComparison() {
        TestResult result1 = new TestResult("test");
        result1.setExecutionTimeMs(100);
        comparison.addBackendResult("rcon", result1);

        TestResult result2 = new TestResult("test");
        result2.setExecutionTimeMs(200);
        comparison.addBackendResult("mineflayer", result2);

        StoryComparison.PerformanceComparison perf = comparison.getPerformanceComparison();

        assertEquals(100, perf.getMinExecutionTime());
        assertEquals(200, perf.getMaxExecutionTime());
        assertEquals(150, perf.getAverageExecutionTime());
        assertEquals(2, perf.getBackendCount());
    }

    @Test
    @DisplayName("getPerformanceComparison with no execution times")
    void testGetPerformanceComparison_noExecutionTimes() {
        StoryComparison.PerformanceComparison perf = comparison.getPerformanceComparison();

        assertEquals(0, perf.getMinExecutionTime());
        assertEquals(0, perf.getMaxExecutionTime());
        assertEquals(0, perf.getAverageExecutionTime());
        assertEquals(0, perf.getBackendCount());
    }

    @Test
    @DisplayName("getPerformanceComparison calculates relative difference")
    void testGetPerformanceComparison_relativeDifference() {
        TestResult result1 = new TestResult("test");
        result1.setExecutionTimeMs(100);
        comparison.addBackendResult("rcon", result1);

        TestResult result2 = new TestResult("test");
        result2.setExecutionTimeMs(200);
        comparison.addBackendResult("mineflayer", result2);

        StoryComparison.PerformanceComparison perf = comparison.getPerformanceComparison();

        // (200 - 100) / 100 = 1.0
        assertEquals(1.0, perf.getRelativeDifference(), 0.01);
    }

    // STORYCOMPARISONSUMMARY INNER CLASS TESTS

    @Test
    @DisplayName("StoryComparisonSummary stores all values")
    void testStoryComparisonSummary() {
        StoryComparison.StoryComparisonSummary summary =
            new StoryComparison.StoryComparisonSummary("story.yaml", 3, 2, 1, true, 0);

        assertEquals("story.yaml", summary.getStoryFile());
        assertEquals(3, summary.getTotalBackends());
        assertEquals(2, summary.getPassedBackends());
        assertEquals(1, summary.getFailedBackends());
        assertTrue(summary.isConsistent());
        assertEquals(0, summary.getInconsistencyCount());
    }

    // PERFORMANCECOMPARISON INNER CLASS TESTS

    @Test
    @DisplayName("PerformanceComparison stores all values")
    void testPerformanceComparison() {
        StoryComparison.PerformanceComparison perf =
            new StoryComparison.PerformanceComparison(100, 200, 150, 2, 1.0);

        assertEquals(100, perf.getMinExecutionTime());
        assertEquals(200, perf.getMaxExecutionTime());
        assertEquals(150, perf.getAverageExecutionTime());
        assertEquals(2, perf.getBackendCount());
        assertEquals(1.0, perf.getRelativeDifference(), 0.01);
    }

    // ADDITIONAL COVERAGE TESTS FOR PERFORMANCECOMPARISON

    @Test
    @DisplayName("PerformanceComparison hasSignificantPerformanceDifference returns true when diff > 10%")
    void testPerformanceComparison_hasSignificantPerformanceDifference_true() {
        StoryComparison.PerformanceComparison perf =
            new StoryComparison.PerformanceComparison(100, 120, 110, 2, 0.15);

        assertTrue(perf.hasSignificantPerformanceDifference());
    }

    @Test
    @DisplayName("PerformanceComparison hasSignificantPerformanceDifference returns false when diff <= 10%")
    void testPerformanceComparison_hasSignificantPerformanceDifference_false() {
        StoryComparison.PerformanceComparison perf =
            new StoryComparison.PerformanceComparison(100, 105, 102, 2, 0.05);

        assertFalse(perf.hasSignificantPerformanceDifference());
    }

    @Test
    @DisplayName("PerformanceComparison hasSignificantPerformanceDifference returns false at exactly 10%")
    void testPerformanceComparison_hasSignificantPerformanceDifference_atThreshold() {
        StoryComparison.PerformanceComparison perf =
            new StoryComparison.PerformanceComparison(100, 110, 105, 2, 0.10);

        assertFalse(perf.hasSignificantPerformanceDifference());
    }

    @Test
    @DisplayName("PerformanceComparison toString returns formatted string")
    void testPerformanceComparison_toString() {
        StoryComparison.PerformanceComparison perf =
            new StoryComparison.PerformanceComparison(100, 200, 150, 2, 1.0);

        String result = perf.toString();

        assertTrue(result.contains("min=100ms"));
        assertTrue(result.contains("max=200ms"));
        assertTrue(result.contains("avg=150ms"));
        assertTrue(result.contains("diff=100.0%"));
    }

    @Test
    @DisplayName("PerformanceComparison toString with different values")
    void testPerformanceComparison_toString_differentValues() {
        StoryComparison.PerformanceComparison perf =
            new StoryComparison.PerformanceComparison(50, 75, 62, 3, 0.5);

        String result = perf.toString();

        assertTrue(result.contains("min=50ms"));
        assertTrue(result.contains("max=75ms"));
        assertTrue(result.contains("avg=62ms"));
        assertTrue(result.contains("diff=50.0%"));
    }

    // ADDITIONAL COVERAGE TESTS FOR STORYCOMPARISONSUMMARY

    @Test
    @DisplayName("StoryComparisonSummary getPassRate returns 0% when no backends")
    void testStoryComparisonSummary_getPassRate_noBackends() {
        StoryComparison.StoryComparisonSummary summary =
            new StoryComparison.StoryComparisonSummary("story.yaml", 0, 0, 0, true, 0);

        assertEquals(0.0, summary.getPassRate(), 0.001);
    }

    @Test
    @DisplayName("StoryComparisonSummary getPassRate returns correct percentage")
    void testStoryComparisonSummary_getPassRate() {
        StoryComparison.StoryComparisonSummary summary =
            new StoryComparison.StoryComparisonSummary("story.yaml", 4, 3, 1, true, 0);

        assertEquals(75.0, summary.getPassRate(), 0.001);
    }

    @Test
    @DisplayName("StoryComparisonSummary getPassRate returns 100% when all passed")
    void testStoryComparisonSummary_getPassRate_allPassed() {
        StoryComparison.StoryComparisonSummary summary =
            new StoryComparison.StoryComparisonSummary("story.yaml", 5, 5, 0, true, 0);

        assertEquals(100.0, summary.getPassRate(), 0.001);
    }

    @Test
    @DisplayName("StoryComparisonSummary getPassRate returns 0% when all failed")
    void testStoryComparisonSummary_getPassRate_allFailed() {
        StoryComparison.StoryComparisonSummary summary =
            new StoryComparison.StoryComparisonSummary("story.yaml", 3, 0, 3, false, 3);

        assertEquals(0.0, summary.getPassRate(), 0.001);
    }

    // ADDITIONAL COVERAGE TESTS FOR STORYCOMPARISON

    @Test
    @DisplayName("generateDetailedReport includes error message when result has error")
    void testGenerateDetailedReport_includesError() {
        TestResult result = new TestResult("test");
        result.setSuccess(false);
        result.setError(new Exception("Test error message"));
        comparison.addBackendResult("rcon", result);

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("Error: Test error message"));
    }

    @Test
    @DisplayName("generateDetailedReport includes assertions passed and failed")
    void testGenerateDetailedReport_includesAssertions() {
        TestResult result = new TestResult("test");
        result.setSuccess(true);
        // Add assertion results using addAssertionResult
        org.cavarest.pilaf.model.Assertion assertion = new org.cavarest.pilaf.model.Assertion();
        for (int i = 0; i < 10; i++) {
            result.addAssertionResult(new TestResult.AssertionResult(assertion, true));
        }
        for (int i = 0; i < 2; i++) {
            result.addAssertionResult(new TestResult.AssertionResult(assertion, false));
        }
        comparison.addBackendResult("rcon", result);

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("Assertions Passed: 10"));
        assertTrue(report.contains("Assertions Failed: 2"));
    }

    @Test
    @DisplayName("getPerformanceComparison handles zero min execution time")
    void testGetPerformanceComparison_zeroMinTime() {
        TestResult result1 = new TestResult("test");
        result1.setExecutionTimeMs(0);
        comparison.addBackendResult("rcon", result1);

        TestResult result2 = new TestResult("test");
        result2.setExecutionTimeMs(100);
        comparison.addBackendResult("mineflayer", result2);

        StoryComparison.PerformanceComparison perf = comparison.getPerformanceComparison();

        // When min time is 0, relative difference should be 0.0
        assertEquals(0.0, perf.getRelativeDifference(), 0.001);
    }

    @Test
    @DisplayName("getPerformanceComparison filters out zero execution times")
    void testGetPerformanceComparison_filtersZeroTimes() {
        TestResult result1 = new TestResult("test");
        result1.setExecutionTimeMs(0);
        comparison.addBackendResult("rcon", result1);

        TestResult result2 = new TestResult("test");
        result2.setExecutionTimeMs(100);
        comparison.addBackendResult("mineflayer", result2);

        TestResult result3 = new TestResult("test");
        result3.setExecutionTimeMs(0);
        comparison.addBackendResult("headlessmc", result3);

        StoryComparison.PerformanceComparison perf = comparison.getPerformanceComparison();

        // Only the non-zero time should be counted
        assertEquals(1, perf.getBackendCount());
        assertEquals(100, perf.getMinExecutionTime());
        assertEquals(100, perf.getMaxExecutionTime());
        assertEquals(100, perf.getAverageExecutionTime());
    }

    @Test
    @DisplayName("getSummary includes inconsistency count")
    void testGetSummary_inconsistencyCount() {
        comparison.setInconsistencies(List.of("Issue 1", "Issue 2", "Issue 3"));

        StoryComparison.StoryComparisonSummary summary = comparison.getSummary();

        assertEquals(3, summary.getInconsistencyCount());
    }

    @Test
    @DisplayName("generateDetailedReport lists all inconsistencies")
    void testGenerateDetailedReport_listsInconsistencies() {
        comparison.setInconsistencies(List.of("Issue 1", "Issue 2"));

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("Inconsistencies Found:"));
        assertTrue(report.contains("Issue 1"));
        assertTrue(report.contains("Issue 2"));
    }

    @Test
    @DisplayName("generateDetailedReport shows correct total and passed/failed counts")
    void testGenerateDetailedReport_counts() {
        TestResult result1 = new TestResult("test");
        result1.setSuccess(true);
        comparison.addBackendResult("rcon", result1);

        TestResult result2 = new TestResult("test");
        result2.setSuccess(false);
        comparison.addBackendResult("mineflayer", result2);

        TestResult result3 = new TestResult("test");
        result3.setSuccess(true);
        comparison.addBackendResult("headlessmc", result3);

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("Total Backends: 3"));
        assertTrue(report.contains("Passed: 2"));
        assertTrue(report.contains("Failed: 1"));
    }
}
