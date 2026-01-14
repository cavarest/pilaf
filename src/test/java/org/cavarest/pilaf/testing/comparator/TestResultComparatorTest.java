package org.cavarest.pilaf.testing.comparator;

import org.cavarest.pilaf.model.TestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestResultComparator.
 */
@DisplayName("TestResultComparator Tests")
class TestResultComparatorTest {

    private TestResultComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new TestResultComparator();
    }

    @Test
    @DisplayName("areResultsConsistent() returns true for null results")
    void testAreResultsConsistent_nullResults() {
        assertTrue(comparator.areResultsConsistent(null));
    }

    @Test
    @DisplayName("areResultsConsistent() returns true for single result")
    void testAreResultsConsistent_singleResult() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));

        assertTrue(comparator.areResultsConsistent(results));
    }

    @Test
    @DisplayName("areResultsConsistent() returns true for all passed results")
    void testAreResultsConsistent_allPassed() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));
        results.put("backend2", createTestResult(true, 5, 0));

        assertTrue(comparator.areResultsConsistent(results));
    }

    @Test
    @DisplayName("areResultsConsistent() returns true for all failed results")
    void testAreResultsConsistent_allFailed() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(false, 0, 5));
        results.put("backend2", createTestResult(false, 0, 5));

        assertTrue(comparator.areResultsConsistent(results));
    }

    @Test
    @DisplayName("areResultsConsistent() returns false for mixed results")
    void testAreResultsConsistent_mixedResults() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));
        results.put("backend2", createTestResult(false, 0, 5));

        assertFalse(comparator.areResultsConsistent(results));
    }

    @Test
    @DisplayName("areResultsConsistent() returns false for different assertion counts")
    void testAreResultsConsistent_differentAssertionCounts() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));
        results.put("backend2", createTestResult(true, 3, 0));

        assertFalse(comparator.areResultsConsistent(results));
    }

    @Test
    @DisplayName("findInconsistencies() returns empty list for null results")
    void testFindInconsistencies_nullResults() {
        List<String> inconsistencies = comparator.findInconsistencies(null);
        assertTrue(inconsistencies.isEmpty());
    }

    @Test
    @DisplayName("findInconsistencies() returns empty list for single result")
    void testFindInconsistencies_singleResult() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));

        List<String> inconsistencies = comparator.findInconsistencies(results);
        assertTrue(inconsistencies.isEmpty());
    }

    @Test
    @DisplayName("findInconsistencies() detects mixed success/failure")
    void testFindInconsistencies_mixedSuccess() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));
        results.put("backend2", createTestResult(false, 0, 5));

        List<String> inconsistencies = comparator.findInconsistencies(results);
        assertFalse(inconsistencies.isEmpty());
        assertTrue(inconsistencies.get(0).contains("Mixed success/failure"));
    }

    @Test
    @DisplayName("findInconsistencies() detects inconsistent assertion counts")
    void testFindInconsistencies_inconsistentAssertions() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));
        results.put("backend2", createTestResult(true, 3, 0));

        List<String> inconsistencies = comparator.findInconsistencies(results);
        assertTrue(inconsistencies.stream().anyMatch(s -> s.contains("Inconsistent assertion")));
    }

    @Test
    @DisplayName("findInconsistencies() detects execution time differences")
    void testFindInconsistencies_executionTimeDifferences() {
        Map<String, TestResult> results = new HashMap<>();
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(100L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(200L); // 100% difference exceeds 10% tolerance

        results.put("backend1", result1);
        results.put("backend2", result2);

        List<String> inconsistencies = comparator.findInconsistencies(results);
        assertTrue(inconsistencies.stream().anyMatch(s -> s.contains("Execution time")));
    }

    @Test
    @DisplayName("findInconsistencies() detects mixed error patterns")
    void testFindInconsistencies_mixedErrors() {
        Map<String, TestResult> results = new HashMap<>();
        TestResult result1 = createTestResult(true, 5, 0);
        TestResult result2 = createTestResult(true, 5, 0);
        result2.setError(new RuntimeException("Test error"));

        results.put("backend1", result1);
        results.put("backend2", result2);

        List<String> inconsistencies = comparator.findInconsistencies(results);
        assertTrue(inconsistencies.stream().anyMatch(s -> s.contains("Mixed error patterns")));
    }

    @Test
    @DisplayName("findInconsistencies() detects different error messages")
    void testFindInconsistencies_differentErrors() {
        Map<String, TestResult> results = new HashMap<>();
        TestResult result1 = createTestResult(false, 0, 5);
        result1.setError(new RuntimeException("Error 1"));
        TestResult result2 = createTestResult(false, 0, 5);
        result2.setError(new RuntimeException("Error 2"));

        results.put("backend1", result1);
        results.put("backend2", result2);

        List<String> inconsistencies = comparator.findInconsistencies(results);
        assertTrue(inconsistencies.stream().anyMatch(s -> s.contains("Different error messages")));
    }

    @Test
    @DisplayName("generateDetailedComparisonReport() returns message for null results")
    void testGenerateDetailedComparisonReport_nullResults() {
        String report = comparator.generateDetailedComparisonReport(null);
        assertTrue(report.contains("No results to compare"));
    }

    @Test
    @DisplayName("generateDetailedComparisonReport() returns message for empty results")
    void testGenerateDetailedComparisonReport_emptyResults() {
        String report = comparator.generateDetailedComparisonReport(new HashMap<>());
        assertTrue(report.contains("No results to compare"));
    }

    @Test
    @DisplayName("generateDetailedComparisonReport() generates proper report")
    void testGenerateDetailedComparisonReport_properReport() {
        Map<String, TestResult> results = new HashMap<>();
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(100L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(105L);

        results.put("backend1", result1);
        results.put("backend2", result2);

        String report = comparator.generateDetailedComparisonReport(results);

        assertTrue(report.contains("=== Detailed Test Result Comparison ==="));
        assertTrue(report.contains("Total backends tested: 2"));
        assertTrue(report.contains("Passed: 2"));
        assertTrue(report.contains("Failed: 0"));
        assertTrue(report.contains("Consistency: PASSED"));
        assertTrue(report.contains("backend1"));
        assertTrue(report.contains("backend2"));
        assertTrue(report.contains("No inconsistencies detected"));
    }

    @Test
    @DisplayName("generateDetailedComparisonReport() includes inconsistencies when present")
    void testGenerateDetailedComparisonReport_withInconsistencies() {
        Map<String, TestResult> results = new HashMap<>();
        results.put("backend1", createTestResult(true, 5, 0));
        results.put("backend2", createTestResult(false, 0, 5));

        String report = comparator.generateDetailedComparisonReport(results);

        assertTrue(report.contains("Consistency: FAILED"));
        assertTrue(report.contains("Inconsistencies Found"));
    }

    @Test
    @DisplayName("compareResults() returns true for null results")
    void testCompareResults_bothNull() {
        assertTrue(comparator.compareResults(null, null));
    }

    @Test
    @DisplayName("compareResults() returns false when one is null")
    void testCompareResults_oneNull() {
        TestResult result = createTestResult(true, 5, 0);
        assertFalse(comparator.compareResults(result, null));
        assertFalse(comparator.compareResults(null, result));
    }

    @Test
    @DisplayName("compareResults() returns true for identical results")
    void testCompareResults_identical() {
        TestResult result1 = createTestResult(true, 5, 0);
        TestResult result2 = createTestResult(true, 5, 0);

        assertTrue(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() returns false for different success status")
    void testCompareResults_differentSuccess() {
        TestResult result1 = createTestResult(true, 5, 0);
        TestResult result2 = createTestResult(false, 5, 0);

        assertFalse(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() returns false for different assertion counts")
    void testCompareResults_differentAssertions() {
        TestResult result1 = createTestResult(true, 5, 0);
        TestResult result2 = createTestResult(true, 3, 0);

        assertFalse(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() returns false for execution time differences exceeding tolerance")
    void testCompareResults_executionTimeExceedsTolerance() {
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(100L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(200L); // 100% difference exceeds 10% tolerance

        assertFalse(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() returns true for execution time within tolerance")
    void testCompareResults_executionTimeWithinTolerance() {
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(100L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(105L); // 5% difference within 10% tolerance

        assertTrue(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() ignores zero execution times")
    void testCompareResults_ignoresZeroTimes() {
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(0L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(100L);

        assertTrue(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("generateDetailedComparisonReport() includes error details when present")
    void testGenerateDetailedComparisonReport_withErrorDetails() {
        Map<String, TestResult> results = new HashMap<>();
        TestResult result = createTestResult(false, 0, 5);
        result.setError(new RuntimeException("Test error message"));

        results.put("backend1", result);

        String report = comparator.generateDetailedComparisonReport(results);

        assertTrue(report.contains("Error: Test error message"));
    }

    @Test
    @DisplayName("compareResults() returns false for different assertion failed counts")
    void testCompareResults_differentAssertionFailed() {
        TestResult result1 = createTestResult(true, 5, 0);
        TestResult result2 = createTestResult(true, 5, 1);

        assertFalse(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() returns true when both execution times are zero")
    void testCompareResults_bothTimesZero() {
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(0L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(0L);

        assertTrue(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() returns true when only first time is zero")
    void testCompareResults_firstTimeZero() {
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(0L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(100L);

        assertTrue(comparator.compareResults(result1, result2));
    }

    @Test
    @DisplayName("compareResults() returns true when only second time is zero")
    void testCompareResults_secondTimeZero() {
        TestResult result1 = createTestResult(true, 5, 0);
        result1.setExecutionTimeMs(100L);

        TestResult result2 = createTestResult(true, 5, 0);
        result2.setExecutionTimeMs(0L);

        assertTrue(comparator.compareResults(result1, result2));
    }

    /**
     * Helper method to create TestResult objects
     */
    private TestResult createTestResult(boolean success, int passed, int failed) {
        TestResult result = new TestResult("Test Story");
        result.setSuccess(success);
        // Use reflection or direct field access since TestResult might not have setters
        try {
            var field = result.getClass().getDeclaredField("assertionsPassed");
            field.setAccessible(true);
            field.set(result, passed);

            field = result.getClass().getDeclaredField("assertionsFailed");
            field.setAccessible(true);
            field.set(result, failed);
        } catch (Exception e) {
            // If reflection fails, create a basic result
        }
        return result;
    }
}
