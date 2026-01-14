package org.cavarest.pilaf.util;

import org.cavarest.pilaf.model.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Validates RCON and command responses against expected patterns.
 *
 * Supports:
 * - Exact match (expect)
 * - Substring match (expectContains)
 * - Regex match (expectMatches)
 * - Negative match (expectNotContains)
 * - Auto-detection of common Minecraft error patterns
 */
public class ResponseValidator {

    // Common Minecraft error patterns that indicate failure
    private static final String[] ERROR_PATTERNS = {
        "No entity was found",
        "No player was found",
        "No block was found",
        "Test failed",
        "Unknown command",
        "Invalid",
        "Error",
        "Failed to",
        "Could not find",
        "does not exist",
        "is not valid",
        "Incorrect argument"
    };

    /**
     * Result of response validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final List<String> errors;

        private ValidationResult(boolean valid, String reason, List<String> errors) {
            this.valid = valid;
            this.reason = reason;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult success(String reason) {
            return new ValidationResult(true, reason, null);
        }

        public static ValidationResult failure(String reason) {
            List<String> errors = new ArrayList<>();
            errors.add(reason);
            return new ValidationResult(false, reason, errors);
        }

        public static ValidationResult failure(String reason, List<String> errors) {
            return new ValidationResult(false, reason, errors);
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * Validate a response against an action's expected values.
     *
     * @param response The actual response from the command
     * @param action The action containing validation criteria
     * @return ValidationResult indicating pass/fail with reason
     */
    public static ValidationResult validate(String response, Action action) {
        if (response == null) {
            response = "";
        }

        List<String> errors = new ArrayList<>();

        // Check for common error patterns (unless disabled)
        Boolean failOnError = action.getFailOnError();
        if (failOnError == null || failOnError) {
            String errorMatch = containsErrorPattern(response);
            if (errorMatch != null) {
                return ValidationResult.failure(
                    "Response contains error: '" + errorMatch + "'",
                    List.of("Detected error pattern: " + errorMatch)
                );
            }
        }

        // Check exact match
        if (action.getExpect() != null) {
            if (!response.equals(action.getExpect())) {
                errors.add("Expected exact: '" + action.getExpect() + "' but got: '" + response + "'");
            }
        }

        // Check contains
        if (action.getExpectContains() != null) {
            if (!response.contains(action.getExpectContains())) {
                errors.add("Expected response to contain: '" + action.getExpectContains() + "'");
            }
        }

        // Check regex match
        if (action.getExpectMatches() != null) {
            try {
                Pattern pattern = Pattern.compile(action.getExpectMatches());
                if (!pattern.matcher(response).find()) {
                    errors.add("Expected response to match pattern: '" + action.getExpectMatches() + "'");
                }
            } catch (PatternSyntaxException e) {
                errors.add("Invalid regex pattern: '" + action.getExpectMatches() + "' - " + e.getMessage());
            }
        }

        // Check not contains
        if (action.getExpectNotContains() != null) {
            if (response.contains(action.getExpectNotContains())) {
                errors.add("Expected response NOT to contain: '" + action.getExpectNotContains() + "'");
            }
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure(
                errors.get(0),  // Primary reason
                errors          // All errors
            );
        }

        return ValidationResult.success();
    }

    /**
     * Check if response contains common Minecraft error patterns.
     *
     * @param response The response to check
     * @return The matched error pattern, or null if none found
     */
    public static String containsErrorPattern(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        for (String pattern : ERROR_PATTERNS) {
            if (response.contains(pattern)) {
                return pattern;
            }
        }

        return null;
    }

    /**
     * Check if response indicates success (no error patterns).
     *
     * @param response The response to check
     * @return true if response appears successful
     */
    public static boolean isSuccessResponse(String response) {
        return containsErrorPattern(response) == null;
    }

    /**
     * Check if a response indicates an entity was not found.
     *
     * @param response The response to check
     * @return true if entity was not found
     */
    public static boolean isEntityNotFound(String response) {
        if (response == null) return false;
        return response.contains("No entity was found") ||
               response.contains("No player was found");
    }

    /**
     * Check if a response indicates a test/execute condition failed.
     *
     * @param response The response to check
     * @return true if test failed
     */
    public static boolean isTestFailed(String response) {
        if (response == null) return false;
        return response.contains("Test failed");
    }

    /**
     * Check if a response indicates a test/execute condition passed.
     *
     * @param response The response to check
     * @return true if test passed
     */
    public static boolean isTestPassed(String response) {
        if (response == null) return false;
        return response.contains("Test passed");
    }
}
