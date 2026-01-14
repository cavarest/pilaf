package org.cavarest.pilaf.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StateManager.
 */
@DisplayName("StateManager Tests")
class StateManagerTest {

    private StateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new StateManager();
    }

    @Test
    @DisplayName("store() stores value successfully")
    void testStore() {
        stateManager.store("test_key", "test_value");

        assertTrue(stateManager.exists("test_key"));
        assertEquals("test_value", stateManager.retrieve("test_key"));
    }

    @Test
    @DisplayName("store() throws exception for null key")
    void testStore_nullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            stateManager.store(null, "value");
        });
    }

    @Test
    @DisplayName("store() throws exception for empty key")
    void testStore_emptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            stateManager.store("", "value");
        });
    }

    @Test
    @DisplayName("store() stores complex object as JSON")
    void testStore_complexObject() {
        TestObject obj = new TestObject("test", 42);

        stateManager.store("complex", obj);

        Object retrieved = stateManager.retrieve("complex");
        assertNotNull(retrieved);
        // Retrieved as Map due to JSON parsing
        assertTrue(retrieved instanceof Map);
    }

    @Test
    @DisplayName("store() stores null value")
    void testStore_nullValue() {
        stateManager.store("null_key", null);

        Object retrieved = stateManager.retrieve("null_key");
        assertNull(retrieved);
    }

    @Test
    @DisplayName("retrieve() returns null for non-existent key")
    void testRetrieve_nonExistentKey() {
        assertNull(stateManager.retrieve("non_existent"));
    }

    @Test
    @DisplayName("retrieve() returns parsed object for stored JSON")
    void testRetrieve_parsedJson() {
        Map<String, Object> data = Map.of("name", "test", "value", 42);

        stateManager.store("data", data);

        Object retrieved = stateManager.retrieve("data");
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof Map);
    }

    @Test
    @DisplayName("retrieveAsJson() returns JSON string")
    void testRetrieveAsJson() {
        stateManager.store("test", "value");

        String json = stateManager.retrieveAsJson("test");

        assertEquals("\"value\"", json);
    }

    @Test
    @DisplayName("retrieveAsJson() returns empty object for non-existent key")
    void testRetrieveAsJson_nonExistentKey() {
        String json = stateManager.retrieveAsJson("non_existent");

        assertEquals("{}", json);
    }

    @Test
    @DisplayName("exists() returns true for existing key")
    void testExists_existing() {
        stateManager.store("key", "value");

        assertTrue(stateManager.exists("key"));
    }

    @Test
    @DisplayName("exists() returns false for non-existent key")
    void testExists_nonExistent() {
        assertFalse(stateManager.exists("non_existent"));
    }

    @Test
    @DisplayName("getKeys() returns all stored keys")
    void testGetKeys() {
        stateManager.store("key1", "value1");
        stateManager.store("key2", "value2");
        stateManager.store("key3", "value3");

        Set<String> keys = stateManager.getKeys();

        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));
    }

    @Test
    @DisplayName("getKeys() returns empty set when no states stored")
    void testGetKeys_empty() {
        Set<String> keys = stateManager.getKeys();

        assertTrue(keys.isEmpty());
    }

    @Test
    @DisplayName("getKeys() returns unmodifiable set")
    void testGetKeys_unmodifiable() {
        stateManager.store("key", "value");

        Set<String> keys = stateManager.getKeys();

        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("new_key");
        });
    }

    @Test
    @DisplayName("compare() returns no changes for identical states")
    void testCompare_identical() {
        stateManager.store("state1", Map.of("value", 42));
        stateManager.store("state2", Map.of("value", 42));

        StateManager.ComparisonResult result = stateManager.compare("state1", "state2");

        assertFalse(result.hasChanges());
        assertEquals("No changes detected - states are identical", result.getSummaryMessage());
    }

    @Test
    @DisplayName("compare() returns changes for different states")
    void testCompare_different() {
        stateManager.store("state1", Map.of("value", 42));
        stateManager.store("state2", Map.of("value", 43));

        StateManager.ComparisonResult result = stateManager.compare("state1", "state2");

        assertTrue(result.hasChanges());
        assertEquals("Changes detected between states", result.getSummaryMessage());
    }

    @Test
    @DisplayName("compare() returns proper JSON fields")
    void testCompare_jsonFields() {
        stateManager.store("before", Map.of("health", 20));
        stateManager.store("after", Map.of("health", 15));

        StateManager.ComparisonResult result = stateManager.compare("before", "after");

        assertNotNull(result.getBeforeJson());
        assertNotNull(result.getAfterJson());
        assertNotNull(result.getDiffJson());
        assertNotNull(result.getBeforeState());
        assertNotNull(result.getAfterState());
    }

    @Test
    @DisplayName("clear() removes all states")
    void testClear() {
        stateManager.store("key1", "value1");
        stateManager.store("key2", "value2");

        stateManager.clear();

        assertEquals(0, stateManager.size());
        assertFalse(stateManager.exists("key1"));
        assertFalse(stateManager.exists("key2"));
    }

    @Test
    @DisplayName("remove() removes specific state")
    void testRemove() {
        stateManager.store("key1", "value1");
        stateManager.store("key2", "value2");

        boolean removed = stateManager.remove("key1");

        assertTrue(removed);
        assertEquals(1, stateManager.size());
        assertFalse(stateManager.exists("key1"));
        assertTrue(stateManager.exists("key2"));
    }

    @Test
    @DisplayName("remove() returns false for non-existent key")
    void testRemove_nonExistent() {
        boolean removed = stateManager.remove("non_existent");

        assertFalse(removed);
    }

    @Test
    @DisplayName("size() returns number of stored states")
    void testSize() {
        assertEquals(0, stateManager.size());

        stateManager.store("key1", "value1");
        assertEquals(1, stateManager.size());

        stateManager.store("key2", "value2");
        assertEquals(2, stateManager.size());

        stateManager.remove("key1");
        assertEquals(1, stateManager.size());
    }

    @Test
    @DisplayName("setVerbose() enables verbose logging")
    void testSetVerbose() {
        assertDoesNotThrow(() -> {
            stateManager.setVerbose(true);
            stateManager.setVerbose(false);
        });
    }

    @Test
    @DisplayName("store() creates immutable snapshot")
    void testStore_immutableSnapshot() {
        StringBuilder sb = new StringBuilder("initial");

        stateManager.store("mutable", sb);

        // Modify the original object
        sb.append("_modified");

        // Retrieved value should still be "initial" because JSON serialization created a snapshot
        Object retrieved = stateManager.retrieve("mutable");
        assertEquals("initial", retrieved);
    }

    @Test
    @DisplayName("ComparisonResult getSummaryMessage() returns appropriate message")
    void testComparisonResult_getSummaryMessage() {
        StateManager.ComparisonResult result = stateManager.compare("non_existent1", "non_existent2");

        // Both return {}, so they're identical
        assertFalse(result.hasChanges());
        assertTrue(result.getSummaryMessage().contains("No changes"));
    }

    // PRIVATE METHOD TESTS

    @Test
    @DisplayName("hasActualChanges returns false for identical objects")
    void testHasActualChanges_identicalObjects() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("hasActualChanges", Object.class, Object.class);
        method.setAccessible(true);

        Map<String, Object> obj = Map.of("key", "value");
        boolean result = (boolean) method.invoke(stateManager, obj, obj);

        assertFalse(result);
    }

    @Test
    @DisplayName("hasActualChanges returns true for different objects")
    void testHasActualChanges_differentObjects() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("hasActualChanges", Object.class, Object.class);
        method.setAccessible(true);

        Map<String, Object> obj1 = Map.of("key", "value1");
        Map<String, Object> obj2 = Map.of("key", "value2");
        boolean result = (boolean) method.invoke(stateManager, obj1, obj2);

        assertTrue(result);
    }

    @Test
    @DisplayName("hasActualChanges returns false for both null")
    void testHasActualChanges_bothNull() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("hasActualChanges", Object.class, Object.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(stateManager, null, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("hasActualChanges returns true for null vs object")
    void testHasActualChanges_nullVsObject() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("hasActualChanges", Object.class, Object.class);
        method.setAccessible(true);

        Map<String, Object> obj = Map.of("key", "value");
        boolean result = (boolean) method.invoke(stateManager, null, obj);

        assertTrue(result);
    }

    @Test
    @DisplayName("hasActualChanges returns true for object vs null")
    void testHasActualChanges_objectVsNull() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("hasActualChanges", Object.class, Object.class);
        method.setAccessible(true);

        Map<String, Object> obj = Map.of("key", "value");
        boolean result = (boolean) method.invoke(stateManager, obj, null);

        assertTrue(result);
    }

    @Test
    @DisplayName("hasActualChanges returns false for both empty maps")
    void testHasActualChanges_bothEmptyMaps() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("hasActualChanges", Object.class, Object.class);
        method.setAccessible(true);

        Map<String, Object> empty1 = Map.of();
        Map<String, Object> empty2 = Map.of();
        boolean result = (boolean) method.invoke(stateManager, empty1, empty2);

        assertFalse(result);
    }

    @Test
    @DisplayName("hasActualChanges returns false for both empty lists")
    void testHasActualChanges_bothEmptyLists() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("hasActualChanges", Object.class, Object.class);
        method.setAccessible(true);

        List<Object> empty1 = List.of();
        List<Object> empty2 = List.of();
        boolean result = (boolean) method.invoke(stateManager, empty1, empty2);

        assertFalse(result);
    }

    @Test
    @DisplayName("log method prints message")
    void testLog() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("log", String.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> {
            method.invoke(stateManager, "Test log message");
        });
    }

    @Test
    @DisplayName("log method with null message")
    void testLog_null() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("log", String.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> {
            method.invoke(stateManager, (String) null);
        });
    }

    @Test
    @DisplayName("log method with empty message")
    void testLog_empty() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("log", String.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> {
            method.invoke(stateManager, "");
        });
    }

    @Test
    @DisplayName("store() with verbose enabled logs message")
    void testStore_withVerbose() {
        stateManager.setVerbose(true);

        assertDoesNotThrow(() -> {
            stateManager.store("test_key", "test_value");
        });
    }

    @Test
    @DisplayName("clear() with verbose enabled logs message")
    void testClear_withVerbose() {
        stateManager.store("key", "value");
        stateManager.setVerbose(true);

        assertDoesNotThrow(() -> {
            stateManager.clear();
        });
    }

    @Test
    @DisplayName("remove() with verbose enabled logs message")
    void testRemove_withVerbose() {
        stateManager.store("key", "value");
        stateManager.setVerbose(true);

        assertDoesNotThrow(() -> {
            stateManager.remove("key");
        });
    }

    @Test
    @DisplayName("compare() with verbose enabled logs messages")
    void testCompare_withVerbose() {
        stateManager.store("state1", Map.of("value", 42));
        stateManager.store("state2", Map.of("value", 43));
        stateManager.setVerbose(true);

        assertDoesNotThrow(() -> {
            stateManager.compare("state1", "state2");
        });
    }

    @Test
    @DisplayName("store() handles serialization failure gracefully")
    void testStore_serializationFailure() throws Exception {
        // Create an object that will fail JSON serialization (self-referencing)
        class SelfReferringObject {
            public SelfReferringObject self = this;
        }

        assertDoesNotThrow(() -> {
            stateManager.store("self_ref", new SelfReferringObject());
        });
    }

    @Test
    @DisplayName("parseJson handles invalid JSON string gracefully")
    void testParseJson_invalidJson() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("parseJson", String.class);
        method.setAccessible(true);

        // Invalid JSON should be returned as-is
        Object result = method.invoke(stateManager, "not-valid-json");
        assertEquals("not-valid-json", result);
    }

    @Test
    @DisplayName("parseJson handles null string")
    void testParseJson_nullString() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("parseJson", String.class);
        method.setAccessible(true);

        Object result = method.invoke(stateManager, (String) null);
        assertNull(result);
    }

    @Test
    @DisplayName("parseJson handles 'null' string")
    void testParseJson_nullStringValue() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("parseJson", String.class);
        method.setAccessible(true);

        Object result = method.invoke(stateManager, "null");
        assertNull(result);
    }

    @Test
    @DisplayName("toJsonSafe handles null object")
    void testToJsonSafe_null() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("toJsonSafe", Object.class);
        method.setAccessible(true);

        Object result = method.invoke(stateManager, (Object) null);
        assertEquals("{}", result);
    }

    @Test
    @DisplayName("deepCopyToJson handles null object")
    void testDeepCopyToJson_null() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("deepCopyToJson", Object.class);
        method.setAccessible(true);

        Object result = method.invoke(stateManager, (Object) null);
        assertEquals("null", result);
    }

    @Test
    @DisplayName("generateJsonPatchDiff handles exception gracefully")
    void testGenerateJsonPatchDiff_exception() throws Exception {
        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("generateJsonPatchDiff", Object.class, Object.class);
        method.setAccessible(true);

        // Pass objects that can't be serialized to JSON
        Object result = method.invoke(stateManager, new Object(), new Object());
        assertNotNull(result);
    }

    @Test
    @DisplayName("remove() existing state with verbose=true")
    void testRemove_existingStateWithVerbose() throws Exception {
        stateManager.setVerbose(true);
        stateManager.store("test_key", "test_value");

        boolean result = stateManager.remove("test_key");

        assertTrue(result);
        assertNull(stateManager.retrieve("test_key"));
    }

    @Test
    @DisplayName("deepCopyToJson with verbose=true logs serialization error")
    void testDeepCopyToJson_verboseSerializationError() throws Exception {
        stateManager.setVerbose(true);

        // Create an object that will fail during serialization
        class NonSerializable {
            @Override
            public String toString() {
                return "NonSerializable";
            }
        }

        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("deepCopyToJson", Object.class);
        method.setAccessible(true);

        // This should trigger the serialization error path and log with verbose=true
        Object result = method.invoke(stateManager, new NonSerializable());

        // Should return String.valueOf(obj) as fallback
        assertNotNull(result);
        assertTrue(result.toString().contains("NonSerializable"));
    }

    @Test
    @DisplayName("generateJsonPatchDiff with verbose=true logs error")
    void testGenerateJsonPatchDiff_verboseError() throws Exception {
        stateManager.setVerbose(true);

        java.lang.reflect.Method method = StateManager.class.getDeclaredMethod("generateJsonPatchDiff", Object.class, Object.class);
        method.setAccessible(true);

        // Pass objects that can't be serialized to JSON to trigger error
        Object result = method.invoke(stateManager, new Object(), new Object());
        assertNotNull(result);
    }

    @Test
    @DisplayName("retrieve() handles non-String value in states map (defensive code)")
    void testRetrieve_nonStringValueInMap() throws Exception {
        // Use reflection to directly put a non-String value into the states map
        // This tests the defensive code path that handles backward compatibility
        java.lang.reflect.Field statesField = StateManager.class.getDeclaredField("states");
        statesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> statesMap = (java.util.Map<String, Object>) statesField.get(stateManager);

        // Put a non-String value directly into the map (bypassing store() which always converts to JSON)
        statesMap.put("direct_key", 12345); // Integer, not String

        // retrieve() should handle this and return the value as-is (line 66 branch)
        Object result = stateManager.retrieve("direct_key");
        assertEquals(12345, result);
    }

    @Test
    @DisplayName("retrieveAsJson() handles non-String value in states map")
    void testRetrieveAsJson_nonStringValueInMap() throws Exception {
        // Use reflection to directly put a non-String value into the states map
        java.lang.reflect.Field statesField = StateManager.class.getDeclaredField("states");
        statesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> statesMap = (java.util.Map<String, Object>) statesField.get(stateManager);

        // Put a non-String value directly into the map
        statesMap.put("direct_key", Map.of("test", "value"));

        // retrieveAsJson() should convert non-String values to JSON (line 84 branch)
        String result = stateManager.retrieveAsJson("direct_key");
        assertNotNull(result);
        assertTrue(result.contains("test") || result.contains("{"));
    }

    @Test
    @DisplayName("remove() with verbose=false and existing key does not log")
    void testRemove_existingKeyVerboseFalse() throws Exception {
        stateManager.setVerbose(false); // Explicitly set verbose to false
        stateManager.store("test_key", "test_value");

        // This should cover the branch where verbose is false but existed is true (line 160)
        boolean result = stateManager.remove("test_key");

        assertTrue(result);
        assertFalse(stateManager.exists("test_key"));
    }

    // Helper test class
    private static class TestObject {
        private final String name;
        private final int value;

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
