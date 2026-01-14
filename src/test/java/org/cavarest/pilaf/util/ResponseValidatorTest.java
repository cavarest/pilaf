package org.cavarest.pilaf.util;

import org.cavarest.pilaf.model.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResponseValidator.
 */
@DisplayName("ResponseValidator Tests")
class ResponseValidatorTest {

    @Test
    @DisplayName("validate() returns success when response matches all criteria")
    void testValidate_allCriteriaMatch() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpect("exact match");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("exact match", action);

        assertTrue(result.isValid());
        assertNull(result.getReason());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("validate() returns success for empty response with no criteria")
    void testValidate_emptyResponseNoCriteria() {
        Action action = new Action(Action.ActionType.WAIT);

        ResponseValidator.ValidationResult result = ResponseValidator.validate("", action);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("validate() fails when exact match doesn't match")
    void testValidate_exactMismatch() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpect("expected");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("actual", action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Expected exact"));
    }

    @Test
    @DisplayName("validate() fails when expectContains is not found")
    void testValidate_expectContainsNotFound() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectContains("needle");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("haystack", action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("needle"));
    }

    @Test
    @DisplayName("validate() passes when expectContains is found")
    void testValidate_expectContainsFound() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectContains("needle");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("this needle is here", action);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("validate() fails when expectMatches regex doesn't match")
    void testValidate_regexMismatch() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectMatches("\\d+");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("no digits", action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("pattern"));
    }

    @Test
    @DisplayName("validate() passes when expectMatches regex matches")
    void testValidate_regexMatches() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectMatches("\\d+");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("12345", action);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("validate() fails when expectMatches has invalid regex")
    void testValidate_invalidRegex() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectMatches("[invalid(");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("any", action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Invalid regex"));
    }

    @Test
    @DisplayName("validate() fails when expectNotContains is found")
    void testValidate_expectNotContainsFound() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectNotContains("forbidden");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("this is forbidden text", action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("NOT to contain"));
    }

    @Test
    @DisplayName("validate() passes when expectNotContains is not found")
    void testValidate_expectNotContainsNotFound() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectNotContains("forbidden");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("this is safe text", action);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("validate() fails on error pattern when failOnError is null (default true)")
    void testValidate_errorPatternDetected() {
        Action action = new Action(Action.ActionType.WAIT);

        ResponseValidator.ValidationResult result = ResponseValidator.validate("Error: something went wrong", action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("error"));
    }

    @Test
    @DisplayName("validate() passes on error pattern when failOnError is false")
    void testValidate_errorPatternIgnored() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setFailOnError(false);

        ResponseValidator.ValidationResult result = ResponseValidator.validate("Error: something went wrong", action);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("validate() fails on error pattern when failOnError is explicitly true")
    void testValidate_errorPatternWithFailOnErrorTrue() {
        // Tests line 91 branch: failOnError == true should trigger error detection
        Action action = new Action(Action.ActionType.WAIT);
        action.setFailOnError(true);  // Explicitly set to true

        ResponseValidator.ValidationResult result = ResponseValidator.validate("Error: something went wrong", action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("error"));
    }

    @Test
    @DisplayName("validate() handles null response")
    void testValidate_nullResponse() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectContains("test");

        ResponseValidator.ValidationResult result = ResponseValidator.validate(null, action);

        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("validate() accumulates multiple errors")
    void testValidate_multipleErrors() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpect("expected");
        action.setExpectContains("contains");
        action.setExpectNotContains("not_contains");

        String response = "actual contains not_contains";

        ResponseValidator.ValidationResult result = ResponseValidator.validate(response, action);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 2);
    }

    @Test
    @DisplayName("ValidationResult.success() returns valid result")
    void testValidationResult_success() {
        ResponseValidator.ValidationResult result = ResponseValidator.ValidationResult.success();

        assertTrue(result.isValid());
        assertNull(result.getReason());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("ValidationResult.success(String) returns valid result with reason")
    void testValidationResult_successWithReason() {
        ResponseValidator.ValidationResult result = ResponseValidator.ValidationResult.success("All good");

        assertTrue(result.isValid());
        assertEquals("All good", result.getReason());
    }

    @Test
    @DisplayName("ValidationResult.failure(String) returns invalid result")
    void testValidationResult_failure() {
        ResponseValidator.ValidationResult result = ResponseValidator.ValidationResult.failure("Failed");

        assertFalse(result.isValid());
        assertEquals("Failed", result.getReason());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    @DisplayName("ValidationResult.failure(String, List) returns invalid result with multiple errors")
    void testValidationResult_failureWithErrors() {
        List<String> errors = List.of("Error 1", "Error 2", "Error 3");

        ResponseValidator.ValidationResult result = ResponseValidator.ValidationResult.failure("Multiple failures", errors);

        assertFalse(result.isValid());
        assertEquals("Multiple failures", result.getReason());
        assertEquals(3, result.getErrors().size());
    }

    @Test
    @DisplayName("containsErrorPattern() returns matched pattern")
    void testContainsErrorPattern_found() {
        String result = ResponseValidator.containsErrorPattern("Error: Unknown command");

        // Returns first matching pattern in ERROR_PATTERNS array
        assertEquals("Unknown command", result);
    }

    @Test
    @DisplayName("containsErrorPattern() returns null for no match")
    void testContainsErrorPattern_notFound() {
        String result = ResponseValidator.containsErrorPattern("Command executed successfully");

        assertNull(result);
    }

    @Test
    @DisplayName("containsErrorPattern() returns null for empty response")
    void testContainsErrorPattern_empty() {
        String result = ResponseValidator.containsErrorPattern("");

        assertNull(result);
    }

    @Test
    @DisplayName("containsErrorPattern() returns null for null response")
    void testContainsErrorPattern_null() {
        String result = ResponseValidator.containsErrorPattern(null);

        assertNull(result);
    }

    @Test
    @DisplayName("isSuccessResponse() returns true for successful response")
    void testIsSuccessResponse_true() {
        assertTrue(ResponseValidator.isSuccessResponse("Command executed successfully"));
    }

    @Test
    @DisplayName("isSuccessResponse() returns false for error response")
    void testIsSuccessResponse_false() {
        assertFalse(ResponseValidator.isSuccessResponse("Error: Unknown command"));
    }

    @Test
    @DisplayName("isSuccessResponse() returns true for null (no error pattern)")
    void testIsSuccessResponse_null() {
        // Null response has no error pattern, so is considered success
        assertTrue(ResponseValidator.isSuccessResponse(null));
    }

    @Test
    @DisplayName("isEntityNotFound() returns true for entity not found")
    void testIsEntityNotFound_true() {
        assertTrue(ResponseValidator.isEntityNotFound("No entity was found"));
        assertTrue(ResponseValidator.isEntityNotFound("No player was found"));
    }

    @Test
    @DisplayName("isEntityNotFound() returns false for other responses")
    void testIsEntityNotFound_false() {
        assertFalse(ResponseValidator.isEntityNotFound("Entity found"));
        assertFalse(ResponseValidator.isEntityNotFound(null));
    }

    @Test
    @DisplayName("isTestFailed() returns true for test failed")
    void testIsTestFailed_true() {
        assertTrue(ResponseValidator.isTestFailed("Test failed"));
    }

    @Test
    @DisplayName("isTestFailed() returns false for other responses")
    void testIsTestFailed_false() {
        assertFalse(ResponseValidator.isTestFailed("Test passed"));
        assertFalse(ResponseValidator.isTestFailed(null));
    }

    @Test
    @DisplayName("isTestPassed() returns true for test passed")
    void testIsTestPassed_true() {
        assertTrue(ResponseValidator.isTestPassed("Test passed"));
    }

    @Test
    @DisplayName("isTestPassed() returns false for other responses")
    void testIsTestPassed_false() {
        assertFalse(ResponseValidator.isTestPassed("Test failed"));
        assertFalse(ResponseValidator.isTestPassed(null));
    }

    @Test
    @DisplayName("validate() passes complex regex pattern")
    void testValidate_complexRegex() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectMatches("^Health: \\d+/20$");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("Health: 15/20", action);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("validate() passes with multiple passing criteria")
    void testValidate_multiplePassingCriteria() {
        Action action = new Action(Action.ActionType.WAIT);
        action.setExpectContains("success");
        action.setExpectNotContains("error");
        action.setExpectMatches(".+success.+");

        ResponseValidator.ValidationResult result = ResponseValidator.validate("operation success completed", action);

        assertTrue(result.isValid());
    }
}
