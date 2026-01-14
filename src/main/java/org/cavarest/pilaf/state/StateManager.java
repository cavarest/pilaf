package org.cavarest.pilaf.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

import java.util.*;

/**
 * Centralized state management for Pilaf test execution.
 *
 * Single source of truth for:
 * - Storing and retrieving state objects
 * - Deep copying to prevent reference issues
 * - State comparison and diff generation
 * - JSON serialization of states
 *
 * This class eliminates code duplication and provides type-safe
 * state operations throughout the test execution lifecycle.
 */
public class StateManager {

    private final Map<String, Object> states = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean verbose = false;

    /**
     * Store a state value as an immutable JSON string snapshot.
     * This IMMEDIATELY serializes the value to JSON to capture the state
     * at THIS moment - preventing any later mutations from affecting stored state.
     *
     * @param key   The key to store the state under
     * @param value The state value to store (will be serialized to JSON immediately)
     */
    public void store(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("State key cannot be null or empty");
        }

        // CRITICAL: Serialize to JSON STRING immediately to create immutable snapshot
        String jsonSnapshot = deepCopyToJson(value);
        states.put(key, jsonSnapshot);

        if (verbose) {
            log("StateManager: Stored '" + key + "' = " + jsonSnapshot);
        }
    }

    /**
     * Retrieve a stored state value.
     * Parses the stored JSON string back to an object.
     *
     * @param key The key to retrieve
     * @return The stored state value parsed from JSON, or null if not found
     */
    public Object retrieve(String key) {
        Object stored = states.get(key);
        if (stored == null) {
            return null;
        }
        // States are stored as JSON strings - parse back to object
        if (stored instanceof String) {
            return parseJson((String) stored);
        }
        return stored;
    }

    /**
     * Retrieve a stored state value as JSON string.
     *
     * @param key The key to retrieve
     * @return JSON string representation, or "{}" if not found
     */
    public String retrieveAsJson(String key) {
        Object stored = states.get(key);
        if (stored == null) {
            return "{}";
        }
        // States are already stored as JSON strings
        if (stored instanceof String) {
            return (String) stored;
        }
        return toJsonSafe(stored);
    }

    /**
     * Check if a state key exists.
     *
     * @param key The key to check
     * @return true if the key exists
     */
    public boolean exists(String key) {
        return states.containsKey(key);
    }

    /**
     * Get all stored state keys.
     *
     * @return Set of all keys
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(states.keySet());
    }

    /**
     * Compare two stored states and return a comparison result.
     *
     * @param key1 First state key (before)
     * @param key2 Second state key (after)
     * @return ComparisonResult containing before, after, diff, and whether changes exist
     */
    public ComparisonResult compare(String key1, String key2) {
        // Retrieve stored JSON strings directly
        String beforeJson = retrieveAsJson(key1);
        String afterJson = retrieveAsJson(key2);

        if (verbose) {
            log("StateManager: Comparing '" + key1 + "' vs '" + key2 + "'");
            log("StateManager:   BEFORE JSON: " + beforeJson);
            log("StateManager:   AFTER JSON:  " + afterJson);
        }

        // Check if there are actual changes by comparing JSON strings
        boolean hasChanges = !beforeJson.equals(afterJson);

        // Generate JSON Patch diff using zjsonpatch
        Object beforeObj = parseJson(beforeJson);
        Object afterObj = parseJson(afterJson);
        JsonNode diffNode = generateJsonPatchDiff(beforeObj, afterObj);
        String diffJson = toJsonSafe(diffNode);

        if (verbose) {
            log("StateManager:   hasChanges=" + hasChanges);
            log("StateManager:   diff=" + diffJson);
        }

        return new ComparisonResult(beforeJson, afterJson, diffJson, hasChanges, beforeObj, afterObj);
    }

    /**
     * Clear all stored states.
     */
    public void clear() {
        states.clear();
        if (verbose) {
            log("StateManager: Cleared all states");
        }
    }

    /**
     * Remove a specific state.
     *
     * @param key The key to remove
     * @return true if the state was removed
     */
    public boolean remove(String key) {
        boolean existed = states.containsKey(key);
        states.remove(key);
        if (verbose && existed) {
            log("StateManager: Removed '" + key + "'");
        }
        return existed;
    }

    /**
     * Get count of stored states.
     *
     * @return Number of stored states
     */
    public int size() {
        return states.size();
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    // ========================================
    // Private helper methods
    // ========================================

    /**
     * Deep copy an object using JSON serialization.
     * This ensures complete isolation between stored states.
     *
     * CRITICAL: We store the JSON STRING, not the object, to prevent
     * any reference issues where the original object gets mutated.
     */
    private String deepCopyToJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            if (verbose) {
                log("StateManager: Serialization failed for " + obj.getClass().getName() + ": " + e.getMessage());
            }
            return String.valueOf(obj);
        }
    }

    /**
     * Parse a JSON string back to an object.
     */
    private Object parseJson(String json) {
        if (json == null || json.equals("null")) {
            return null;
        }
        try {
            return mapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    /**
     * Convert object to JSON string safely.
     */
    private String toJsonSafe(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    /**
     * Generate a JSON Patch diff using zjsonpatch library.
     * Returns proper RFC 6902 JSON Patch format for rendering with jsondiffpatch.
     */
    private JsonNode generateJsonPatchDiff(Object before, Object after) {
        try {
            String beforeJson = toJsonSafe(before);
            String afterJson = toJsonSafe(after);

            JsonNode beforeNode = mapper.readTree(beforeJson);
            JsonNode afterNode = mapper.readTree(afterJson);

            // Use zjsonpatch to generate RFC 6902 JSON Patch
            return JsonDiff.asJson(beforeNode, afterNode);
        } catch (Exception e) {
            if (verbose) {
                log("StateManager: Failed to generate JSON diff: " + e.getMessage());
            }
            // Return empty array (no changes) on error
            return mapper.createArrayNode();
        }
    }

    /**
     * Check if there are actual changes between two states.
     */
    private boolean hasActualChanges(Object before, Object after) {
        String beforeJson = toJsonSafe(before);
        String afterJson = toJsonSafe(after);
        return !beforeJson.equals(afterJson);
    }

    private void log(String message) {
        System.out.println(message);
    }

    // ========================================
    // Result classes
    // ========================================

    /**
     * Result of a state comparison operation.
     */
    public static class ComparisonResult {
        private final String beforeJson;
        private final String afterJson;
        private final String diffJson;
        private final boolean hasChanges;
        private final Object beforeState;
        private final Object afterState;

        public ComparisonResult(String beforeJson, String afterJson, String diffJson,
                                boolean hasChanges, Object beforeState, Object afterState) {
            this.beforeJson = beforeJson;
            this.afterJson = afterJson;
            this.diffJson = diffJson;
            this.hasChanges = hasChanges;
            this.beforeState = beforeState;
            this.afterState = afterState;
        }

        public String getBeforeJson() { return beforeJson; }
        public String getAfterJson() { return afterJson; }
        public String getDiffJson() { return diffJson; }
        public boolean hasChanges() { return hasChanges; }
        public Object getBeforeState() { return beforeState; }
        public Object getAfterState() { return afterState; }

        /**
         * Get a summary message for logging/display.
         */
        public String getSummaryMessage() {
            if (hasChanges) {
                return "Changes detected between states";
            } else {
                return "No changes detected - states are identical";
            }
        }
    }
}
