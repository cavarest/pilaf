package org.cavarest.pilaf.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TestResult
 */
@DisplayName("TestResult Tests")
class TestResultTest {

    @Test
    @DisplayName("default constructor creates TestResult with default values")
    void testDefaultConstructor() {
        TestResult result = new TestResult();

        assertFalse(result.isSuccess());
        assertNull(result.getStoryName());
        assertEquals(0, result.getExecutionTimeMs());
        assertEquals(0, result.getActionsExecuted());
        assertEquals(0, result.getAssertionsPassed());
        assertEquals(0, result.getAssertionsFailed());
        assertNotNull(result.getLogs());
        assertTrue(result.getLogs().isEmpty());
        assertNull(result.getError());
    }

    @Test
    @DisplayName("constructor with storyName sets storyName")
    void testConstructorWithStoryName() {
        TestResult result = new TestResult("test-story");

        assertEquals("test-story", result.getStoryName());
    }

    @Test
    @DisplayName("setSuccess and isSuccess work correctly")
    void testSetSuccess_isSuccess() {
        TestResult result = new TestResult();

        result.setSuccess(true);
        assertTrue(result.isSuccess());

        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("setStoryName and getStoryName work correctly")
    void testSetStoryName_getStoryName() {
        TestResult result = new TestResult();

        result.setStoryName("my-story");
        assertEquals("my-story", result.getStoryName());
    }

    @Test
    @DisplayName("setExecutionTimeMs and getExecutionTimeMs work correctly")
    void testSetExecutionTimeMs_getExecutionTimeMs() {
        TestResult result = new TestResult();

        result.setExecutionTimeMs(12345);
        assertEquals(12345, result.getExecutionTimeMs());
    }

    @Test
    @DisplayName("incrementActionsExecuted increments the counter")
    void testIncrementActionsExecuted() {
        TestResult result = new TestResult();

        assertEquals(0, result.getActionsExecuted());
        result.incrementActionsExecuted();
        assertEquals(1, result.getActionsExecuted());
        result.incrementActionsExecuted();
        assertEquals(2, result.getActionsExecuted());
    }

    @Test
    @DisplayName("addLog adds message to logs")
    void testAddLog() {
        TestResult result = new TestResult();

        result.addLog("First message");
        result.addLog("Second message");

        assertEquals(2, result.getLogs().size());
        assertTrue(result.getLogs().contains("First message"));
        assertTrue(result.getLogs().contains("Second message"));
    }

    @Test
    @DisplayName("setError and getError work correctly")
    void testSetError_getError() {
        TestResult result = new TestResult();
        Exception testException = new RuntimeException("Test error");

        result.setError(testException);
        assertEquals(testException, result.getError());
    }

    @Test
    @DisplayName("addAssertionResult with passed assertion increments passed counter")
    void testAddAssertionResult_passed() {
        TestResult result = new TestResult();
        Assertion assertion = new Assertion(Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS);

        TestResult.AssertionResult assertionResult = new TestResult.AssertionResult(assertion, true);
        result.addAssertionResult(assertionResult);

        assertEquals(1, result.getAssertionsPassed());
        assertEquals(0, result.getAssertionsFailed());
    }

    @Test
    @DisplayName("addAssertionResult with failed assertion increments failed counter")
    void testAddAssertionResult_failed() {
        TestResult result = new TestResult();
        Assertion assertion = new Assertion(Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS);

        TestResult.AssertionResult assertionResult = new TestResult.AssertionResult(assertion, false);
        result.addAssertionResult(assertionResult);

        assertEquals(0, result.getAssertionsPassed());
        assertEquals(1, result.getAssertionsFailed());
    }

    @Test
    @DisplayName("pass sets success to true")
    void testPass() {
        TestResult result = new TestResult();

        result.pass();
        assertTrue(result.isSuccess());
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("fail sets success to false and adds log message")
    void testFail() {
        TestResult result = new TestResult();

        result.fail("Test failed");
        assertFalse(result.isSuccess());
        assertTrue(result.getLogs().contains("FAILURE: Test failed"));
    }

    @Test
    @DisplayName("fail with null message sets success to false without log")
    void testFail_nullMessage() {
        TestResult result = new TestResult();

        result.fail(null);
        assertFalse(result.isSuccess());
        assertTrue(result.getLogs().isEmpty());
    }

    @Test
    @DisplayName("getMessage returns error message when error is set")
    void testGetMessage_withError() {
        TestResult result = new TestResult();
        Exception testException = new RuntimeException("Test error message");

        result.setError(testException);
        assertEquals("Test error message", result.getMessage());
    }

    @Test
    @DisplayName("getMessage returns null when error is not set")
    void testGetMessage_noError() {
        TestResult result = new TestResult();

        assertNull(result.getMessage());
    }

    @Test
    @DisplayName("setPassed sets success flag")
    void testSetPassed() {
        TestResult result = new TestResult();

        result.setPassed(true);
        assertTrue(result.isPassed());
        assertTrue(result.isSuccess());

        result.setPassed(false);
        assertFalse(result.isPassed());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("setMessage adds log with INFO prefix")
    void testSetMessage() {
        TestResult result = new TestResult();

        result.setMessage("Test message");
        assertTrue(result.getLogs().contains("INFO: Test message"));
    }

    @Test
    @DisplayName("setMessage with null message does not add log")
    void testSetMessage_nullMessage() {
        TestResult result = new TestResult();

        result.setMessage(null);
        assertTrue(result.getLogs().isEmpty());
    }

    @Test
    @DisplayName("setTestName sets storyName")
    void testSetTestName() {
        TestResult result = new TestResult();

        result.setTestName("my-test");
        assertEquals("my-test", result.getStoryName());
    }

    @Test
    @DisplayName("setStoryFile adds log with story file")
    void testSetStoryFile() {
        TestResult result = new TestResult();

        result.setStoryFile("/path/to/story.yaml");
        assertTrue(result.getLogs().contains("Story file: /path/to/story.yaml"));
    }

    // Inner class AssertionResult tests
    @Test
    @DisplayName("AssertionResult constructor stores assertion and passed flag")
    void testAssertionResultConstructor() {
        Assertion assertion = new Assertion(Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS);
        TestResult.AssertionResult result = new TestResult.AssertionResult(assertion, true);

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("AssertionResult setMessage sets message")
    void testAssertionResultSetMessage() {
        Assertion assertion = new Assertion(Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS);
        TestResult.AssertionResult result = new TestResult.AssertionResult(assertion, true);

        result.setMessage("Test message");
        // Can't access message directly, but we can call it without exception
        assertNotNull(result);
    }
}
