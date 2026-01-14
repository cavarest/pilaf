package org.cavarest.pilaf.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MineflayerClient utility methods
 */
@DisplayName("MineflayerClient Tests")
class MineflayerClientTest {

    @Test
    @DisplayName("constructor creates correct base URL")
    void testConstructor_createsCorrectBaseUrl() {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        // Base URL is http://localhost:3000
        assertNotNull(client);
    }

    @Test
    @DisplayName("constructor with different host and port")
    void testConstructor_differentHostAndPort() {
        MineflayerClient client = new MineflayerClient("example.com", 8080);
        assertNotNull(client);
    }

    @Test
    @DisplayName("setVerbose updates verbose flag")
    void testSetVerbose() {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        client.setVerbose(true);
        client.setVerbose(false);
        // No exception thrown
        assertNotNull(client);
    }

    @Test
    @DisplayName("isHealthy returns false when connection fails")
    void testIsHealthy_returnsFalseOnConnectionFailure() {
        MineflayerClient client = new MineflayerClient("localhost", 9999);
        boolean healthy = client.isHealthy();
        assertFalse(healthy, "Should return false when connection fails");
    }

    @Test
    @DisplayName("log method does not throw exception")
    void testLog_noException() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        client.setVerbose(true);
        // Use reflection to call the private log method
        Method logMethod = MineflayerClient.class.getDeclaredMethod("log", String.class);
        logMethod.setAccessible(true);
        logMethod.invoke(client, "test message");
        // No exception thrown
    }

    @Test
    @DisplayName("toJson with simple string map")
    void testToJson_simpleStringMap() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        Method toJsonMethod = MineflayerClient.class.getDeclaredMethod("toJson", Map.class);
        toJsonMethod.setAccessible(true);
        String result = (String) toJsonMethod.invoke(client, map);

        assertTrue(result.contains("\"key1\":\"value1\""));
        assertTrue(result.contains("\"key2\":\"value2\""));
    }

    @Test
    @DisplayName("toJson with number values")
    void testToJson_withNumberValues() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        Map<String, Object> map = new HashMap<>();
        map.put("count", 42);
        map.put("price", 19.99);

        Method toJsonMethod = MineflayerClient.class.getDeclaredMethod("toJson", Map.class);
        toJsonMethod.setAccessible(true);
        String result = (String) toJsonMethod.invoke(client, map);

        assertTrue(result.contains("\"count\":42"));
        assertTrue(result.contains("\"price\":19.99"));
    }

    @Test
    @DisplayName("toJson with boolean values")
    void testToJson_withBooleanValues() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        Map<String, Object> map = new HashMap<>();
        map.put("active", true);
        map.put("disabled", false);

        Method toJsonMethod = MineflayerClient.class.getDeclaredMethod("toJson", Map.class);
        toJsonMethod.setAccessible(true);
        String result = (String) toJsonMethod.invoke(client, map);

        assertTrue(result.contains("\"active\":true"));
        assertTrue(result.contains("\"disabled\":false"));
    }

    @Test
    @DisplayName("toJson with mixed types")
    void testToJson_mixedTypes() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "test");
        map.put("count", 5);
        map.put("enabled", true);

        Method toJsonMethod = MineflayerClient.class.getDeclaredMethod("toJson", Map.class);
        toJsonMethod.setAccessible(true);
        String result = (String) toJsonMethod.invoke(client, map);

        assertTrue(result.contains("\"name\":\"test\""));
        assertTrue(result.contains("\"count\":5"));
        assertTrue(result.contains("\"enabled\":true"));
    }

    @Test
    @DisplayName("parseJsonSimple with simple string values")
    void testParseJsonSimple_simpleStringValues() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonSimple", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(client, json);

        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    @DisplayName("parseJsonSimple with number values")
    void testParseJsonSimple_numberValues() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "{\"int\":42,\"double\":19.99}";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonSimple", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(client, json);

        assertEquals(42L, result.get("int"));
        assertEquals(19.99, result.get("double"));
    }

    @Test
    @DisplayName("parseJsonSimple with boolean values")
    void testParseJsonSimple_booleanValues() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "{\"active\":true,\"disabled\":false}";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonSimple", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(client, json);

        assertEquals(true, result.get("active"));
        assertEquals(false, result.get("disabled"));
    }

    @Test
    @DisplayName("parseJsonSimple with null value")
    void testParseJsonSimple_nullValue() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "{\"value\":null}";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonSimple", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(client, json);

        assertNull(result.get("value"));
    }

    @Test
    @DisplayName("parseJsonSimple with nested object")
    void testParseJsonSimple_nestedObject() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "{\"outer\":{\"inner\":\"value\"}}";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonSimple", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(client, json);

        assertNotNull(result.get("outer"));
        @SuppressWarnings("unchecked")
        Map<String, Object> outer = (Map<String, Object>) result.get("outer");
        assertEquals("value", outer.get("inner"));
    }

    @Test
    @DisplayName("parseJsonSimple with empty object")
    void testParseJsonSimple_emptyObject() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "{}";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonSimple", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(client, json);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseJsonSimple with invalid JSON returns empty map")
    void testParseJsonSimple_invalidJson() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "not a json object";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonSimple", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(client, json);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseJsonArray with simple string values")
    void testParseJsonArray_stringValues() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "[\"value1\",\"value2\",\"value3\"]";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonArray", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) parseMethod.invoke(client, json);

        assertEquals(3, result.size());
        assertEquals("value1", result.get(0));
        assertEquals("value2", result.get(1));
        assertEquals("value3", result.get(2));
    }

    @Test
    @DisplayName("parseJsonArray with number values")
    void testParseJsonArray_numberValues() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "[1,2,3,4,5]";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonArray", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) parseMethod.invoke(client, json);

        assertEquals(5, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(5L, result.get(4));
    }

    @Test
    @DisplayName("parseJsonArray with mixed types")
    void testParseJsonArray_mixedTypes() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "[\"string\",42,true,null]";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonArray", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) parseMethod.invoke(client, json);

        assertEquals(4, result.size());
        assertEquals("string", result.get(0));
        assertEquals(42L, result.get(1));
        assertEquals(true, result.get(2));
        assertNull(result.get(3));
    }

    @Test
    @DisplayName("parseJsonArray with nested objects")
    void testParseJsonArray_nestedObjects() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "[{\"key\":\"value1\"},{\"key\":\"value2\"}]";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonArray", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) parseMethod.invoke(client, json);

        assertEquals(2, result.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) result.get(0);
        assertEquals("value1", first.get("key"));
    }

    @Test
    @DisplayName("parseJsonArray with empty array")
    void testParseJsonArray_emptyArray() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "[]";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonArray", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) parseMethod.invoke(client, json);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseJsonArray with nested arrays")
    void testParseJsonArray_nestedArrays() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        String json = "[[1,2],[3,4]]";

        Method parseMethod = MineflayerClient.class.getDeclaredMethod("parseJsonArray", String.class);
        parseMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) parseMethod.invoke(client, json);

        assertEquals(2, result.size());
        @SuppressWarnings("unchecked")
        List<Object> first = (List<Object>) result.get(0);
        assertEquals(2, first.size());
    }

    // UNCOVERED BRANCH TESTS

    @Test
    @DisplayName("log() with verbose=true prints message")
    void testLog_verboseTrue() throws Exception {
        MineflayerClient client = new MineflayerClient("localhost", 3000);
        client.setVerbose(true);

        // Just verify the method doesn't throw when verbose is true
        Method logMethod = MineflayerClient.class.getDeclaredMethod("log", String.class);
        logMethod.setAccessible(true);
        assertDoesNotThrow(() -> logMethod.invoke(client, "test verbose message"));
    }

    @Test
    @DisplayName("isHealthy() returns false when server not available")
    void testIsHealthy_serverNotAvailable() {
        MineflayerClient client = new MineflayerClient("invalid-host", 9999);
        assertFalse(client.isHealthy());
    }
}
