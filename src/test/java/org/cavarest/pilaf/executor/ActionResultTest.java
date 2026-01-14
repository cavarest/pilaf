package org.cavarest.pilaf.executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActionResult.
 */
@DisplayName("ActionResult Tests")
class ActionResultTest {

    @Test
    @DisplayName("success() creates successful result with response")
    void testSuccessWithResponse() {
        ActionResult result = ActionResult.success("Command executed successfully");

        assertTrue(result.isSuccess());
        assertEquals("Command executed successfully", result.getResponse());
        assertNull(result.getError());
        assertFalse(result.hasStateToStore());
        assertFalse(result.isComparison());
    }

    @Test
    @DisplayName("successWithState() creates result with state to store")
    void testSuccessWithState() {
        Object value = "test value";
        ActionResult result = ActionResult.successWithState("Response", "my_key", value);

        assertTrue(result.isSuccess());
        assertEquals("Response", result.getResponse());
        assertTrue(result.hasStateToStore());
        assertEquals("my_key", result.getStoreKey());
        assertEquals(value, result.getStoreValue());
    }

    @Test
    @DisplayName("successWithExtractedJson() creates result with extracted JSON")
    void testSuccessWithExtractedJson() {
        String raw = "data get entity @s Inventory";
        String extracted = "{\"Inventory\":[{\"id\":\"minecraft:diamond\"}]}";
        Object parsed = parsedData();

        ActionResult result = ActionResult.successWithExtractedJson(raw, extracted, parsed);

        assertTrue(result.isSuccess());
        assertEquals(raw, result.getResponse());
        assertTrue(result.hasExtractedJson());
        assertEquals(extracted, result.getExtractedJson());
        assertEquals(parsed, result.getParsedData());
    }

    @Test
    @DisplayName("successWithExtractedJsonAndState() creates result with JSON and state")
    void testSuccessWithExtractedJsonAndState() {
        String raw = "raw response";
        String extracted = "{\"key\":\"value\"}";
        Object parsed = parsedData();

        ActionResult result = ActionResult.successWithExtractedJsonAndState(raw, extracted, parsed, "state_key");

        assertTrue(result.isSuccess());
        assertTrue(result.hasExtractedJson());
        assertTrue(result.hasStateToStore());
        assertEquals("state_key", result.getStoreKey());
        assertEquals(parsed, result.getStoreValue());
    }

    @Test
    @DisplayName("successWithExtractedJsonAndState() uses raw response when parsed data is null")
    void testSuccessWithExtractedJsonAndState_nullParsedData() {
        String raw = "raw response";
        String extracted = "{\"key\":\"value\"}";

        ActionResult result = ActionResult.successWithExtractedJsonAndState(raw, extracted, null, "state_key");

        assertTrue(result.hasStateToStore());
        assertEquals(raw, result.getStoreValue());
    }

    @Test
    @DisplayName("failure(String) creates failed result with error message")
    void testFailureWithError() {
        ActionResult result = ActionResult.failure("Something went wrong");

        assertFalse(result.isSuccess());
        assertEquals("Something went wrong", result.getError());
        assertNull(result.getResponse());
    }

    @Test
    @DisplayName("failure(Exception) creates failed result from exception")
    void testFailureWithException() {
        Exception exception = new Exception("Database connection failed");
        ActionResult result = ActionResult.failure(exception);

        assertFalse(result.isSuccess());
        assertEquals("Database connection failed", result.getError());
    }

    @Test
    @DisplayName("comparison() creates result with state comparison data")
    void testComparison() {
        String before = "{\"health\":20}";
        String after = "{\"health\":15}";
        String diff = "[{\"op\":\"replace\",\"path\":\"/health\",\"value\":15}]";

        ActionResult result = ActionResult.comparison(before, after, diff, true);

        assertTrue(result.isSuccess());
        assertTrue(result.isComparison());
        assertTrue(result.hasChanges());
        assertEquals("Changes detected", result.getResponse());
        assertEquals(before, result.getStateBefore());
        assertEquals(after, result.getStateAfter());
        assertEquals(diff, result.getStateDiff());
    }

    @Test
    @DisplayName("comparison() with no changes")
    void testComparisonNoChanges() {
        String state = "{\"health\":20}";

        ActionResult result = ActionResult.comparison(state, state, "[]", false);

        assertTrue(result.isComparison());
        assertFalse(result.hasChanges());
        assertEquals("No changes detected", result.getResponse());
    }

    @Test
    @DisplayName("Builder creates ActionResult with all fields")
    void testBuilder() {
        Object parsed = new Object();

        ActionResult result = new ActionResult.Builder()
            .success(true)
            .response("Test response")
            .error("Test error")
            .storeKey("test_key")
            .storeValue(parsed)
            .extractedJson("{\"data\":123}")
            .parsedData(parsed)
            .stateBefore("before")
            .stateAfter("after")
            .stateDiff("diff")
            .hasChanges(true)
            .build();

        assertTrue(result.isSuccess());
        assertEquals("Test response", result.getResponse());
        assertEquals("Test error", result.getError());
        assertEquals("test_key", result.getStoreKey());
        assertEquals(parsed, result.getStoreValue());
        assertTrue(result.hasExtractedJson());
        assertEquals("{\"data\":123}", result.getExtractedJson());
        assertEquals(parsed, result.getParsedData());
        assertEquals("before", result.getStateBefore());
        assertEquals("after", result.getStateAfter());
        assertEquals("diff", result.getStateDiff());
        assertTrue(result.hasChanges());
    }

    @Test
    @DisplayName("Builder with minimal fields")
    void testBuilderMinimal() {
        ActionResult result = new ActionResult.Builder()
            .success(false)
            .build();

        assertFalse(result.isSuccess());
        assertNull(result.getResponse());
        assertNull(result.getError());
        assertFalse(result.hasStateToStore());
    }

    @Test
    @DisplayName("isComparison() returns true when state fields are set")
    void testIsComparison() {
        ActionResult withBefore = new ActionResult.Builder()
            .stateBefore("before")
            .build();
        assertTrue(withBefore.isComparison());

        ActionResult withAfter = new ActionResult.Builder()
            .stateAfter("after")
            .build();
        assertTrue(withAfter.isComparison());

        ActionResult withBoth = new ActionResult.Builder()
            .stateBefore("before")
            .stateAfter("after")
            .build();
        assertTrue(withBoth.isComparison());

        ActionResult without = new ActionResult.Builder().build();
        assertFalse(without.isComparison());
    }

    @Test
    @DisplayName("hasStateToStore() returns true only when both key and value are set")
    void testHasStateToStore() {
        ActionResult withBoth = new ActionResult.Builder()
            .storeKey("key")
            .storeValue("value")
            .build();
        assertTrue(withBoth.hasStateToStore());

        ActionResult withKeyOnly = new ActionResult.Builder()
            .storeKey("key")
            .build();
        assertFalse(withKeyOnly.hasStateToStore());

        ActionResult withValueOnly = new ActionResult.Builder()
            .storeValue("value")
            .build();
        assertFalse(withValueOnly.hasStateToStore());

        ActionResult withNullValue = new ActionResult.Builder()
            .storeKey("key")
            .storeValue(null)
            .build();
        assertFalse(withNullValue.hasStateToStore());
    }

    @Test
    @DisplayName("hasExtractedJson() returns true when extractedJson is set")
    void testHasExtractedJson() {
        ActionResult withJson = new ActionResult.Builder()
            .extractedJson("{\"test\":true}")
            .build();
        assertTrue(withJson.hasExtractedJson());

        ActionResult withoutJson = new ActionResult.Builder().build();
        assertFalse(withoutJson.hasExtractedJson());
    }

    // Helper method to create a mock parsed data object
    private Object parsedData() {
        return new Object() {
            @Override
            public String toString() {
                return "parsed_data";
            }
        };
    }
}
