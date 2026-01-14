package org.cavarest.pilaf.executor;

/**
 * Result of executing an action.
 * Encapsulates success/failure, response data, and any state changes.
 */
public class ActionResult {

    private final boolean success;
    private final String response;
    private final String error;
    private final String storeKey;
    private final Object storeValue;

    // Extracted JSON from RCON responses (e.g., from "data get entity" commands)
    private final String extractedJson;
    private final Object parsedData;

    // State comparison fields (for COMPARE_STATES action)
    private String stateBefore;
    private String stateAfter;
    private String stateDiff;
    private boolean hasChanges;

    private ActionResult(Builder builder) {
        this.success = builder.success;
        this.response = builder.response;
        this.error = builder.error;
        this.storeKey = builder.storeKey;
        this.storeValue = builder.storeValue;
        this.extractedJson = builder.extractedJson;
        this.parsedData = builder.parsedData;
        this.stateBefore = builder.stateBefore;
        this.stateAfter = builder.stateAfter;
        this.stateDiff = builder.stateDiff;
        this.hasChanges = builder.hasChanges;
    }

    // ========================================
    // Factory methods for common cases
    // ========================================

    /**
     * Create a successful result with a response.
     */
    public static ActionResult success(String response) {
        return new Builder().success(true).response(response).build();
    }

    /**
     * Create a successful result with response and state to store.
     */
    public static ActionResult successWithState(String response, String storeKey, Object storeValue) {
        return new Builder()
            .success(true)
            .response(response)
            .storeKey(storeKey)
            .storeValue(storeValue)
            .build();
    }

    /**
     * Create a successful result with extracted JSON data from an RCON response.
     *
     * @param rawResponse The raw RCON response string
     * @param extractedJson The extracted and cleaned JSON string
     * @param parsedData The parsed JSON object
     * @return ActionResult with both raw and parsed data
     */
    public static ActionResult successWithExtractedJson(String rawResponse, String extractedJson, Object parsedData) {
        return new Builder()
            .success(true)
            .response(rawResponse)
            .extractedJson(extractedJson)
            .parsedData(parsedData)
            .build();
    }

    /**
     * Create a successful result with extracted JSON and state to store.
     *
     * @param rawResponse The raw RCON response string
     * @param extractedJson The extracted and cleaned JSON string
     * @param parsedData The parsed JSON object
     * @param storeKey The key to store the parsed data under
     * @return ActionResult with raw, parsed data, and state to store
     */
    public static ActionResult successWithExtractedJsonAndState(String rawResponse, String extractedJson,
                                                                  Object parsedData, String storeKey) {
        return new Builder()
            .success(true)
            .response(rawResponse)
            .extractedJson(extractedJson)
            .parsedData(parsedData)
            .storeKey(storeKey)
            .storeValue(parsedData != null ? parsedData : rawResponse)
            .build();
    }

    /**
     * Create a failed result with an error message.
     */
    public static ActionResult failure(String error) {
        return new Builder().success(false).error(error).build();
    }

    /**
     * Create a failed result from an exception.
     */
    public static ActionResult failure(Exception e) {
        return new Builder()
            .success(false)
            .error(e.getMessage())
            .build();
    }

    /**
     * Create a result for state comparison.
     */
    public static ActionResult comparison(String before, String after, String diff, boolean hasChanges) {
        return new Builder()
            .success(hasChanges) // Comparison is "successful" if changes were detected
            .response(hasChanges ? "Changes detected" : "No changes detected")
            .stateBefore(before)
            .stateAfter(after)
            .stateDiff(diff)
            .hasChanges(hasChanges)
            .build();
    }

    // ========================================
    // Getters
    // ========================================

    public boolean isSuccess() {
        return success;
    }

    public String getResponse() {
        return response;
    }

    public String getError() {
        return error;
    }

    public boolean hasStateToStore() {
        return storeKey != null && storeValue != null;
    }

    public String getStoreKey() {
        return storeKey;
    }

    public Object getStoreValue() {
        return storeValue;
    }

    public boolean isComparison() {
        return stateBefore != null || stateAfter != null;
    }

    public boolean hasExtractedJson() {
        return extractedJson != null;
    }

    public String getExtractedJson() {
        return extractedJson;
    }

    public Object getParsedData() {
        return parsedData;
    }

    public String getStateBefore() {
        return stateBefore;
    }

    public String getStateAfter() {
        return stateAfter;
    }

    public String getStateDiff() {
        return stateDiff;
    }

    public boolean hasChanges() {
        return hasChanges;
    }

    // ========================================
    // Builder
    // ========================================

    public static class Builder {
        private boolean success = true;
        private String response;
        private String error;
        private String storeKey;
        private Object storeValue;
        private String extractedJson;
        private Object parsedData;
        private String stateBefore;
        private String stateAfter;
        private String stateDiff;
        private boolean hasChanges;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder response(String response) {
            this.response = response;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder storeKey(String storeKey) {
            this.storeKey = storeKey;
            return this;
        }

        public Builder storeValue(Object storeValue) {
            this.storeValue = storeValue;
            return this;
        }

        public Builder extractedJson(String extractedJson) {
            this.extractedJson = extractedJson;
            return this;
        }

        public Builder parsedData(Object parsedData) {
            this.parsedData = parsedData;
            return this;
        }

        public Builder stateBefore(String stateBefore) {
            this.stateBefore = stateBefore;
            return this;
        }

        public Builder stateAfter(String stateAfter) {
            this.stateAfter = stateAfter;
            return this;
        }

        public Builder stateDiff(String stateDiff) {
            this.stateDiff = stateDiff;
            return this;
        }

        public Builder hasChanges(boolean hasChanges) {
            this.hasChanges = hasChanges;
            return this;
        }

        public ActionResult build() {
            return new ActionResult(this);
        }
    }
}
