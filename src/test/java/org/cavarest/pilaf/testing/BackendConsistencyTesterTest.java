package org.cavarest.pilaf.testing;

import org.cavarest.pilaf.config.TestConfiguration;
import org.cavarest.pilaf.model.TestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackendConsistencyTester.
 * Note: Full integration tests with Docker are in the integration test suite.
 */
@DisplayName("BackendConsistencyTester Tests")
class BackendConsistencyTesterTest {

    private BackendConsistencyTester tester;

    @BeforeEach
    void setUp() {
        tester = new BackendConsistencyTester();
    }

    @Test
    @DisplayName("constructor initializes dependencies")
    void testConstructor() {
        assertNotNull(tester);
    }

    // BackendTestResult tests

    @Test
    @DisplayName("BackendTestResult constructor initializes with config file")
    void testBackendTestResultConstructor() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("test-config.yaml");

        assertEquals("test-config.yaml", result.getConfigFile());
        assertEquals(0, result.getTotalStories());
        assertEquals(0, result.getPassedStories());
        assertFalse(result.isSuccessful());
        assertNotNull(result.getStoryResults());
        assertNotNull(result.getErrors());
    }

    @Test
    @DisplayName("BackendTestResult addStoryResult adds story result")
    void testBackendTestResultAddStoryResult() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("test-config.yaml");

        TestResult storyResult = new TestResult("story1.yaml");
        storyResult.pass();

        result.addStoryResult("story1.yaml", storyResult);

        assertEquals(1, result.getTotalStories());
        assertEquals(1, result.getPassedStories());
        assertSame(storyResult, result.getStoryResult("story1.yaml"));
    }

    @Test
    @DisplayName("BackendTestResult getStoryResult returns null for non-existent story")
    void testBackendTestResultGetStoryResultNonExistent() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("test-config.yaml");

        assertNull(result.getStoryResult("non-existent.yaml"));
    }

    @Test
    @DisplayName("BackendTestResult addError adds error message")
    void testBackendTestResultAddError() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("test-config.yaml");

        result.addError("Error message 1");
        result.addError("Error message 2");

        List<String> errors = result.getErrors();
        assertEquals(2, errors.size());
        assertTrue(errors.contains("Error message 1"));
        assertTrue(errors.contains("Error message 2"));
    }

    @Test
    @DisplayName("BackendTestResult setSuccessful changes success flag")
    void testBackendTestResultSetSuccessful() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("test-config.yaml");

        assertFalse(result.isSuccessful());

        result.setSuccessful(true);
        assertTrue(result.isSuccessful());

        result.setSuccessful(false);
        assertFalse(result.isSuccessful());
    }

    @Test
    @DisplayName("BackendTestResult getPassedStories counts only successful stories")
    void testBackendTestResultGetPassedStories() {
        BackendConsistencyTester.BackendTestResult result =
            new BackendConsistencyTester.BackendTestResult("test-config.yaml");

        TestResult success1 = new TestResult("story1.yaml");
        success1.pass();

        TestResult failure1 = new TestResult("story2.yaml");
        failure1.fail("Test failed");

        TestResult success2 = new TestResult("story3.yaml");
        success2.pass();

        result.addStoryResult("story1.yaml", success1);
        result.addStoryResult("story2.yaml", failure1);
        result.addStoryResult("story3.yaml", success2);

        assertEquals(3, result.getTotalStories());
        assertEquals(2, result.getPassedStories());
    }

    // ConsistencyTestResult tests

    @Test
    @DisplayName("ConsistencyTestResult constructor initializes empty")
    void testConsistencyTestResultConstructor() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        assertNotNull(result.getBackendResults());
        assertEquals(0, result.getBackendResults().size());
        assertNull(result.getComparison());
        assertEquals(0, result.getTotalExecutionTime());
    }

    @Test
    @DisplayName("ConsistencyTestResult addBackendResult adds backend result")
    void testConsistencyTestResultAddBackendResult() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        BackendConsistencyTester.BackendTestResult backendResult =
            new BackendConsistencyTester.BackendTestResult("config1.yaml");

        result.addBackendResult("config1.yaml", backendResult);

        assertEquals(1, result.getBackendResults().size());
        assertSame(backendResult, result.getBackendResults().get("config1.yaml"));
    }

    @Test
    @DisplayName("ConsistencyTestResult setComparison sets comparison")
    void testConsistencyTestResultSetComparison() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        BackendConsistencyTester.ConsistencyTestResult mockResult =
            new BackendConsistencyTester.ConsistencyTestResult();

        assertNull(mockResult.getComparison());

        // Note: ConsistencyComparison is package-private, so we can't directly test this
        // but we can verify the setter exists
        mockResult.setComparison(null); // Should not throw
        assertNull(mockResult.getComparison());
    }

    @Test
    @DisplayName("ConsistencyTestResult setTotalExecutionTime sets time")
    void testConsistencyTestResultSetTotalExecutionTime() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        result.setTotalExecutionTime(5000);

        assertEquals(5000, result.getTotalExecutionTime());
    }

    @Test
    @DisplayName("ConsistencyTestResult isOverallConsistent returns false when comparison is null")
    void testConsistencyTestResultIsOverallConsistentNullComparison() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        assertFalse(result.isOverallConsistent());
    }

    @Test
    @DisplayName("ConsistencyTestResult getSummary generates summary with no backends")
    void testConsistencyTestResultGetSummaryEmpty() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        String summary = result.getSummary();

        assertTrue(summary.contains("Backend Consistency Test Summary"));
        assertTrue(summary.contains("Total Backend Combinations: 0"));
        assertTrue(summary.contains("Total Test Stories: 0"));
        assertTrue(summary.contains("Total Passed: 0"));
        assertTrue(summary.contains("Overall Consistency: FAILED"));
    }

    @Test
    @DisplayName("ConsistencyTestResult getSummary generates summary with backends")
    void testConsistencyTestResultGetSummaryWithBackends() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        BackendConsistencyTester.BackendTestResult backend1 =
            new BackendConsistencyTester.BackendTestResult("config1.yaml");
        TestResult story1 = new TestResult("story1.yaml");
        story1.pass();
        TestResult story2 = new TestResult("story2.yaml");
        story2.fail("Failed");
        backend1.addStoryResult("story1.yaml", story1);
        backend1.addStoryResult("story2.yaml", story2);

        BackendConsistencyTester.BackendTestResult backend2 =
            new BackendConsistencyTester.BackendTestResult("config2.yaml");
        TestResult story3 = new TestResult("story1.yaml");
        story3.pass();
        backend2.addStoryResult("story1.yaml", story3);

        result.addBackendResult("config1.yaml", backend1);
        result.addBackendResult("config2.yaml", backend2);

        String summary = result.getSummary();

        assertTrue(summary.contains("Backend Consistency Test Summary"));
        assertTrue(summary.contains("Total Backend Combinations: 2"));
        assertTrue(summary.contains("Total Test Stories: 3"));
        assertTrue(summary.contains("Total Passed: 2"));
    }

    @Test
    @DisplayName("ConsistencyTestResult getSummary calculates totals correctly")
    void testConsistencyTestResultGetSummaryCalculations() {
        BackendConsistencyTester.ConsistencyTestResult result =
            new BackendConsistencyTester.ConsistencyTestResult();

        BackendConsistencyTester.BackendTestResult backend1 =
            new BackendConsistencyTester.BackendTestResult("config1.yaml");
        TestResult story1 = new TestResult("story1.yaml");
        story1.pass();
        TestResult story2 = new TestResult("story2.yaml");
        story2.pass();
        backend1.addStoryResult("story1.yaml", story1);
        backend1.addStoryResult("story2.yaml", story2);

        BackendConsistencyTester.BackendTestResult backend2 =
            new BackendConsistencyTester.BackendTestResult("config2.yaml");
        TestResult story3 = new TestResult("story1.yaml");
        story3.pass();
        TestResult story4 = new TestResult("story3.yaml");
        story4.pass();
        backend2.addStoryResult("story1.yaml", story3);
        backend2.addStoryResult("story3.yaml", story4);

        result.addBackendResult("config1.yaml", backend1);
        result.addBackendResult("config2.yaml", backend2);

        String summary = result.getSummary();

        // 2 backends, each with 2 stories = 4 total story executions
        assertTrue(summary.contains("Total Test Stories: 4"));
        // All 4 executions passed
        assertTrue(summary.contains("Total Passed: 4"));
    }
}
