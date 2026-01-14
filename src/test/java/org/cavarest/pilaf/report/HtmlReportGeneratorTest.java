package org.cavarest.pilaf.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HtmlReportGenerator utility methods using reflection.
 */
@DisplayName("HtmlReportGenerator Tests")
class HtmlReportGeneratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ESCAPE HTML TESTS

    @Test
    @DisplayName("escapeHtml escapes special characters")
    void testEscapeHtml() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "<script>alert(\"xss\")</script>");

        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
        assertTrue(result.contains("&quot;"));
    }

    @Test
    @DisplayName("escapeHtml escapes ampersand")
    void testEscapeHtml_ampersand() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "A & B");

        assertTrue(result.contains("&amp;"));
    }

    @Test
    @DisplayName("escapeHtml handles null")
    void testEscapeHtml_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (String) null);

        assertEquals("", result);
    }

    @Test
    @DisplayName("escapeHtml handles empty string")
    void testEscapeHtml_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertEquals("", result);
    }

    // ESCAPE JSON KEY TESTS

    @Test
    @DisplayName("escapeJsonKey escapes special characters")
    void testEscapeJsonKey() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("escapeJsonKey", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "key\"with\\quotes");

        assertTrue(result.contains("\\\""));
        assertTrue(result.contains("\\\\"));
    }

    @Test
    @DisplayName("escapeJsonKey escapes newlines and tabs")
    void testEscapeJsonKey_newlinesAndTabs() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("escapeJsonKey", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "key\n\r\t");

        assertTrue(result.contains("\\n"));
        assertTrue(result.contains("\\r"));
        assertTrue(result.contains("\\t"));
    }

    // FORMAT PATH TESTS

    @Test
    @DisplayName("formatPath converts JSON path to display format")
    void testFormatPath() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatPath", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "/entities/0/name");

        assertEquals("entities[0].name", result);
    }

    @Test
    @DisplayName("formatPath handles root path")
    void testFormatPath_root() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatPath", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "/");

        assertEquals("(root)", result);
    }

    @Test
    @DisplayName("formatPath handles empty path")
    void testFormatPath_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatPath", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertEquals("(root)", result);
    }

    @Test
    @DisplayName("formatPath handles null path")
    void testFormatPath_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatPath", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (String) null);

        assertEquals("(root)", result);
    }

    @Test
    @DisplayName("formatPath handles nested array indices")
    void testFormatPath_nestedArrays() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatPath", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "/items/0/values/1/name");

        assertEquals("items[0].values[1].name", result);
    }

    // FORMAT VALUE TESTS

    @Test
    @DisplayName("formatValue returns null string for null")
    void testFormatValue_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatValue", JsonNode.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (JsonNode) null);

        assertEquals("null", result);
    }

    @Test
    @DisplayName("formatValue returns JSON for string node")
    void testFormatValue_string() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatValue", JsonNode.class);
        method.setAccessible(true);

        JsonNode node = MAPPER.readTree("\"test\"");
        String result = (String) method.invoke(null, node);

        assertEquals("\"test\"", result);
    }

    @Test
    @DisplayName("formatValue returns JSON for number node")
    void testFormatValue_number() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatValue", JsonNode.class);
        method.setAccessible(true);

        JsonNode node = MAPPER.readTree("42");
        String result = (String) method.invoke(null, node);

        assertEquals("42", result);
    }

    @Test
    @DisplayName("formatValue returns JSON for object node")
    void testFormatValue_object() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatValue", JsonNode.class);
        method.setAccessible(true);

        JsonNode node = MAPPER.readTree("{\"key\":\"value\"}");
        String result = (String) method.invoke(null, node);

        assertEquals("{\"key\":\"value\"}", result);
    }

    // GET VALUE TYPE TESTS

    @Test
    @DisplayName("getValueType returns 'null' for null")
    void testGetValueType_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (Object) null);

        assertEquals("null", result);
    }

    @Test
    @DisplayName("getValueType returns 'object' for Map")
    void testGetValueType_map() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        Map<String, Object> map = new HashMap<>();
        String result = (String) method.invoke(null, map);

        assertEquals("object", result);
    }

    @Test
    @DisplayName("getValueType returns 'array' for List")
    void testGetValueType_list() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        List<Object> list = new ArrayList<>();
        String result = (String) method.invoke(null, list);

        assertEquals("array", result);
    }

    @Test
    @DisplayName("getValueType returns 'string' for String")
    void testGetValueType_string() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "test");

        assertEquals("string", result);
    }

    @Test
    @DisplayName("getValueType returns 'number' for Integer")
    void testGetValueType_integer() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, 42);

        assertEquals("number", result);
    }

    @Test
    @DisplayName("getValueType returns 'number' for Double")
    void testGetValueType_double() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, 3.14);

        assertEquals("number", result);
    }

    @Test
    @DisplayName("getValueType returns 'boolean' for Boolean")
    void testGetValueType_boolean() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, true);

        assertEquals("boolean", result);
    }

    @Test
    @DisplayName("getValueType returns 'unknown' for unknown type")
    void testGetValueType_unknown() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("getValueType", Object.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, new Object());

        assertEquals("unknown", result);
    }

    // TRANSFORM EVIDENCE TESTS

    @Test
    @DisplayName("transformEvidence converts checkmarks to success icons")
    void testTransformEvidence_success() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformEvidence", List.class);
        method.setAccessible(true);

        List<String> evidence = List.of("✓ Test passed");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> result = (List<Map<String, String>>) method.invoke(null, evidence);

        assertEquals(1, result.size());
        assertEquals("✅", result.get(0).get("icon"));
        assertEquals("Test passed", result.get(0).get("text"));
    }

    @Test
    @DisplayName("transformEvidence converts cross marks to failure icons")
    void testTransformEvidence_failure() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformEvidence", List.class);
        method.setAccessible(true);

        List<String> evidence = List.of("✗ Test failed");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> result = (List<Map<String, String>>) method.invoke(null, evidence);

        assertEquals(1, result.size());
        assertEquals("❌", result.get(0).get("icon"));
        assertEquals("Test failed", result.get(0).get("text"));
    }

    @Test
    @DisplayName("transformEvidence uses bullet for plain text")
    void testTransformEvidence_plainText() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformEvidence", List.class);
        method.setAccessible(true);

        List<String> evidence = List.of("Some message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> result = (List<Map<String, String>>) method.invoke(null, evidence);

        assertEquals(1, result.size());
        assertEquals("•", result.get(0).get("icon"));
        assertEquals("Some message", result.get(0).get("text"));
    }

    @Test
    @DisplayName("transformEvidence handles empty list")
    void testTransformEvidence_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformEvidence", List.class);
        method.setAccessible(true);

        List<String> evidence = List.of();
        @SuppressWarnings("unchecked")
        List<Map<String, String>> result = (List<Map<String, String>>) method.invoke(null, evidence);

        assertTrue(result.isEmpty());
    }

    // CONVERT JAVA TO JSON TESTS

    @Test
    @DisplayName("convertJavaToJson handles empty string")
    void testConvertJavaToJson_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertNull(result);
    }

    @Test
    @DisplayName("convertJavaToJson handles null")
    void testConvertJavaToJson_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (String) null);

        assertNull(result);
    }

    @Test
    @DisplayName("convertJavaToJson returns null for plain string without special chars")
    void testConvertJavaToJson_plainString() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "just a string");

        assertNull(result);
    }

    @Test
    @DisplayName("convertJavaToJson converts Java object notation")
    void testConvertJavaToJson_javaObject() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key=value}");

        assertNotNull(result);
        assertTrue(result.contains("\"key\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    @DisplayName("convertJavaToJson converts array notation")
    void testConvertJavaToJson_array() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "[item1, item2]");

        assertNotNull(result);
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    // COUNT ACTION TYPES TESTS

    @Test
    @DisplayName("countActionTypes counts server actions")
    void testCountActionTypes_server() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("countActionTypes", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story = new TestReporter.TestStory("Test");
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.action = "give item diamond";
        story.addStep(step1);

        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) method.invoke(null, List.of(story));

        assertEquals(1, result.get("server"));
        assertEquals(0, result.get("client"));
        assertEquals(0, result.get("workflow"));
    }

    @Test
    @DisplayName("countActionTypes counts client actions")
    void testCountActionTypes_client() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("countActionTypes", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story = new TestReporter.TestStory("Test");
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.action = "some action";
        step1.player = "test_player";
        story.addStep(step1);

        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) method.invoke(null, List.of(story));

        assertEquals(0, result.get("server"));
        assertEquals(1, result.get("client"));
        assertEquals(0, result.get("workflow"));
    }

    @Test
    @DisplayName("countActionTypes counts workflow actions")
    void testCountActionTypes_workflow() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("countActionTypes", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story = new TestReporter.TestStory("Test");
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.action = "wait";
        story.addStep(step1);

        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) method.invoke(null, List.of(story));

        assertEquals(0, result.get("server"));
        assertEquals(0, result.get("client"));
        assertEquals(1, result.get("workflow"));
    }

    @Test
    @DisplayName("countActionTypes handles empty stories")
    void testCountActionTypes_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("countActionTypes", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) method.invoke(null, List.of());

        assertEquals(0, result.get("server"));
        assertEquals(0, result.get("client"));
        assertEquals(0, result.get("workflow"));
    }

    @Test
    @DisplayName("countActionTypes handles multiple stories")
    void testCountActionTypes_multipleStories() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("countActionTypes", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story1 = new TestReporter.TestStory("Test 1");
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.action = "give item";
        story1.addStep(step1);

        TestReporter.TestStory story2 = new TestReporter.TestStory("Test 2");
        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.action = "some action";
        step2.player = "player";
        story2.addStep(step2);

        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) method.invoke(null, List.of(story1, story2));

        assertEquals(1, result.get("server"));
        assertEquals(1, result.get("client"));
        assertEquals(0, result.get("workflow"));
    }

    // RENDER STATE TESTS

    @Test
    @DisplayName("renderState returns null for null")
    void testRenderState_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderState", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (String) null);

        assertNull(result);
    }

    @Test
    @DisplayName("renderState returns null for empty string")
    void testRenderState_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderState", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertNull(result);
    }

    @Test
    @DisplayName("renderState returns null for whitespace only")
    void testRenderState_whitespace() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderState", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "   ");

        assertNull(result);
    }

    @Test
    @DisplayName("renderState renders JSON object")
    void testRenderState_jsonObject() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderState", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"value\"}");

        assertNotNull(result);
        assertTrue(result.contains("<pre"));
        assertTrue(result.contains("state-json"));
    }

    @Test
    @DisplayName("renderState renders JSON array")
    void testRenderState_jsonArray() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderState", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "[\"item1\",\"item2\"]");

        assertNotNull(result);
        assertTrue(result.contains("<pre"));
        assertTrue(result.contains("state-json"));
    }

    @Test
    @DisplayName("renderState renders plain text")
    void testRenderState_plainText() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderState", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "plain text state");

        assertNotNull(result);
        assertTrue(result.contains("<span"));
        assertTrue(result.contains("state-text"));
    }

    // PRETTY PRINT JSON TESTS

    @Test
    @DisplayName("prettyPrintJson handles empty string")
    void testPrettyPrintJson_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertEquals("", result);
    }

    @Test
    @DisplayName("prettyPrintJson escapes HTML")
    void testPrettyPrintJson_escapesHtml() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"<value>\"}");

        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
    }

    @Test
    @DisplayName("prettyPrintJson formats valid JSON longer than 100 chars")
    void testPrettyPrintJson_formatsJson() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJson", String.class);
        method.setAccessible(true);

        // Create JSON longer than 100 characters to trigger formatting
        String longJson = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\",\"key4\":\"value4\",\"key5\":\"value5\",\"nested\":{\"item1\":1,\"item2\":2,\"item3\":3}}";
        String result = (String) method.invoke(null, longJson);

        // Should have newlines after formatting
        assertTrue(result.contains("\n"));
    }

    @Test
    @DisplayName("prettyPrintJson returns short JSON as-is")
    void testPrettyPrintJson_shortJson() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"value\"}");

        // Short JSON (< 100 chars) is returned escaped but not reformatted
        assertTrue(result.contains("&quot;"));
    }

    @Test
    @DisplayName("prettyPrintJson handles invalid JSON")
    void testPrettyPrintJson_invalidJson() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "not valid json");

        assertEquals("not valid json", result);
    }

    // RENDER JSON NODE TESTS

    @Test
    @DisplayName("renderJsonNode renders string value")
    void testRenderJsonNode_string() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "test", "key", 0, false, "key");

        assertTrue(result.contains("<span class=\"string\">"));
        assertTrue(result.contains("test"));
    }

    @Test
    @DisplayName("renderJsonNode renders number value")
    void testRenderJsonNode_number() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, 42, "key", 0, false, "key");

        assertTrue(result.contains("<span class=\"number\">"));
        assertTrue(result.contains("42"));
    }

    @Test
    @DisplayName("renderJsonNode renders boolean value")
    void testRenderJsonNode_boolean() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, true, "key", 0, false, "key");

        assertTrue(result.contains("<span class=\"boolean\">"));
        assertTrue(result.contains("true"));
    }

    @Test
    @DisplayName("renderJsonNode renders null value")
    void testRenderJsonNode_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, null, "key", 0, false, "key");

        assertTrue(result.contains("<span class=\"null\">"));
        assertTrue(result.contains("null"));
    }

    @Test
    @DisplayName("renderJsonNode renders object with children")
    void testRenderJsonNode_object() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        Map<String, Object> obj = new HashMap<>();
        obj.put("key1", "value1");
        obj.put("key2", "value2");

        String result = (String) method.invoke(null, obj, "", 0, true, "root");

        assertTrue(result.contains("<span class=\"bracket\">{</span>"));
        assertTrue(result.contains("<div class=\"children\""));
        assertTrue(result.contains("<span class=\"key\">"));
    }

    @Test
    @DisplayName("renderJsonNode renders array with children")
    void testRenderJsonNode_array() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        List<Object> arr = List.of("item1", "item2", "item3");

        String result = (String) method.invoke(null, arr, "", 0, true, "root");

        assertTrue(result.contains("<span class=\"bracket\">[</span>"));
        assertTrue(result.contains("<div class=\"children\""));
    }

    @Test
    @DisplayName("renderJsonNode adds toggle for elements with children")
    void testRenderJsonNode_toggleForChildren() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        Map<String, Object> obj = new HashMap<>();
        obj.put("key", "value");

        String result = (String) method.invoke(null, obj, "", 0, true, "root");

        assertTrue(result.contains("<span class=\"toggle collapsed\""));
        assertTrue(result.contains("onclick=\"toggleNode"));
    }

    @Test
    @DisplayName("renderJsonNode hides toggle for elements without children")
    void testRenderJsonNode_noToggleForLeaf() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "value", "key", 0, false, "key");

        assertTrue(result.contains("<span class=\"toggle\" style=\"visibility: hidden;\"></span>"));
    }

    @Test
    @DisplayName("renderJsonNode handles array index in path")
    void testRenderJsonNode_arrayIndexPath() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonNode", Object.class, String.class, int.class, boolean.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "value", "items[0]", 0, false, "items[0]");

        assertTrue(result.contains("[0]"));
    }

    // RENDER JSON TREE TESTS

    @Test
    @DisplayName("renderJsonTree wraps result in json-tree div")
    void testRenderJsonTree_wrapper() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonTree", Object.class, String.class, int.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "value", "key", 0);

        assertTrue(result.contains("<div class=\"json-tree\">"));
        assertTrue(result.contains("</div>"));
    }

    @Test
    @DisplayName("renderJsonTree generates unique node ID for root")
    void testRenderJsonTree_rootNodeId() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderJsonTree", Object.class, String.class, int.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "value", "", 0);

        // Root path should generate json-tree wrapper
        assertTrue(result.contains("<div class=\"json-tree\">"));
    }

    // FORMAT JAVA OBJECT NOTATION TESTS

    @Test
    @DisplayName("formatJavaObjectNotation handles empty object")
    void testFormatJavaObjectNotation_emptyObject() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObjectNotation", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{}");

        // Empty object after stripping braces returns empty string
        assertTrue(result.isEmpty() || result.trim().isEmpty());
    }

    @Test
    @DisplayName("formatJavaObjectNotation formats simple key=value pairs")
    void testFormatJavaObjectNotation_simplePairs() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObjectNotation", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key=value}");

        assertTrue(result.contains("key"));
        assertTrue(result.contains(": "));
        assertTrue(result.contains("value"));
    }

    @Test
    @DisplayName("formatJavaObjectNotation handles multiple key=value pairs")
    void testFormatJavaObjectNotation_multiplePairs() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObjectNotation", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key1=value1, key2=value2}");

        assertTrue(result.contains("key1"));
        assertTrue(result.contains("key2"));
        assertTrue(result.contains(",\n"));
    }

    @Test
    @DisplayName("formatJavaObjectNotation handles nested objects")
    void testFormatJavaObjectNotation_nestedObjects() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObjectNotation", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{outer={inner=value}}");

        assertTrue(result.contains("{\n"));
        assertTrue(result.contains("  "));
    }

    @Test
    @DisplayName("formatJavaObjectNotation handles arrays")
    void testFormatJavaObjectNotation_arrays() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObjectNotation", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{items=[item1, item2]}");

        assertTrue(result.contains("[\n"));
    }

    @Test
    @DisplayName("formatJavaObjectNotation handles string escaping")
    void testFormatJavaObjectNotation_stringEscaping() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObjectNotation", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key=\"quoted value\"}");

        assertTrue(result.contains("\"quoted value\""));
    }

    // FORMAT JAVA OBJECT TESTS

    @Test
    @DisplayName("formatJavaObject handles empty object")
    void testFormatJavaObject_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObject", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{}");

        assertTrue(result.contains("<div class=\"json-formatted\">"));
    }

    @Test
    @DisplayName("formatJavaObject formats key=value pairs with HTML")
    void testFormatJavaObject_keyValue() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObject", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key=value}");

        // formatJavaObject wraps content in json-formatted div and replaces = with :
        assertTrue(result.contains("<div class=\"json-formatted\">"));
        assertTrue(result.contains(": "));
    }

    @Test
    @DisplayName("formatJavaObject handles nested structures")
    void testFormatJavaObject_nested() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObject", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{outer={inner=value}}");

        assertTrue(result.contains("<br>"));
        assertTrue(result.contains("  "));
    }

    @Test
    @DisplayName("formatJavaObject escapes HTML in values")
    void testFormatJavaObject_escapesHtml() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatJavaObject", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key=<value>}");

        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
    }

    // PRETTY PRINT JSON FOR DIFF TESTS

    @Test
    @DisplayName("prettyPrintJsonForDiff handles empty string")
    void testPrettyPrintJsonForDiff_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJsonForDiff", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertEquals("", result);
    }

    @Test
    @DisplayName("prettyPrintJsonForDiff handles object")
    void testPrettyPrintJsonForDiff_object() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJsonForDiff", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"value\"}");

        assertTrue(result.contains("{\n"));
        assertTrue(result.contains("  "));
    }

    @Test
    @DisplayName("prettyPrintJsonForDiff handles array")
    void testPrettyPrintJsonForDiff_array() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJsonForDiff", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "[\"item1\",\"item2\"]");

        assertTrue(result.contains("[\n"));
        assertTrue(result.contains("  "));
    }

    @Test
    @DisplayName("prettyPrintJsonForDiff handles nested structures")
    void testPrettyPrintJsonForDiff_nested() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJsonForDiff", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"outer\":{\"inner\":\"value\"}}");

        assertTrue(result.contains("\n"));
        assertTrue(result.contains("  "));
    }

    @Test
    @DisplayName("prettyPrintJsonForDiff adds colon with space after keys")
    void testPrettyPrintJsonForDiff_colonSpacing() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJsonForDiff", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"value\"}");

        assertTrue(result.contains(": "));
    }

    @Test
    @DisplayName("prettyPrintJsonForDiff handles strings with quotes")
    void testPrettyPrintJsonForDiff_quotedStrings() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("prettyPrintJsonForDiff", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"value with \\\"quote\\\"\"}");

        // Should properly handle escaped quotes
        assertTrue(result.contains("value"));
    }

    // FLATTEN ALL STEPS TESTS

    @Test
    @DisplayName("flattenAllSteps collects steps from single story")
    void testFlattenAllSteps_singleStory() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("flattenAllSteps", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story = new TestReporter.TestStory("Test Story");
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        story.addStep(step1);
        story.addStep(step2);

        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> result = (List<TestReporter.TestStep>) method.invoke(null, List.of(story));

        assertEquals(2, result.size());
        assertEquals("Step 1", result.get(0).name);
        assertEquals("Step 2", result.get(1).name);
    }

    @Test
    @DisplayName("flattenAllSteps collects steps from multiple stories")
    void testFlattenAllSteps_multipleStories() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("flattenAllSteps", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story1 = new TestReporter.TestStory("Story 1");
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        story1.addStep(step1);

        TestReporter.TestStory story2 = new TestReporter.TestStory("Story 2");
        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        TestReporter.TestStep step3 = new TestReporter.TestStep("Step 3");
        story2.addStep(step2);
        story2.addStep(step3);

        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> result = (List<TestReporter.TestStep>) method.invoke(null, List.of(story1, story2));

        assertEquals(3, result.size());
        assertEquals("Step 1", result.get(0).name);
        assertEquals("Step 2", result.get(1).name);
        assertEquals("Step 3", result.get(2).name);
    }

    @Test
    @DisplayName("flattenAllSteps handles story with no steps")
    void testFlattenAllSteps_emptyStory() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("flattenAllSteps", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story = new TestReporter.TestStory("Empty Story");

        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> result = (List<TestReporter.TestStep>) method.invoke(null, List.of(story));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("flattenAllSteps handles empty story list")
    void testFlattenAllSteps_emptyList() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("flattenAllSteps", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> result = (List<TestReporter.TestStep>) method.invoke(null, List.of());

        assertTrue(result.isEmpty());
    }

    // CONVERT JAVA OBJECT TO JSON TESTS

    @Test
    @DisplayName("convertJavaObjectToJson handles empty string")
    void testConvertJavaObjectToJson_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertEquals("", result);
    }

    @Test
    @DisplayName("convertJavaObjectToJson handles quoted strings")
    void testConvertJavaObjectToJson_quotedString() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "\"hello\"");

        assertEquals("\"hello\"", result);
    }

    @Test
    @DisplayName("convertJavaObjectToJson handles numbers")
    void testConvertJavaObjectToJson_number() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "42");

        assertEquals("42", result);
    }

    @Test
    @DisplayName("convertJavaObjectToJson handles decimals")
    void testConvertJavaObjectToJson_decimal() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "3.14");

        assertEquals("3.14", result);
    }

    @Test
    @DisplayName("convertJavaObjectToJson handles boolean values")
    void testConvertJavaObjectToJson_boolean() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result1 = (String) method.invoke(null, "true");
        String result2 = (String) method.invoke(null, "false");

        assertEquals("true", result1);
        assertEquals("false", result2);
    }

    @Test
    @DisplayName("convertJavaObjectToJson handles null")
    void testConvertJavaObjectToJson_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "null");

        assertEquals("null", result);
    }

    @Test
    @DisplayName("convertJavaObjectToJson converts empty array")
    void testConvertJavaObjectToJson_emptyArray() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "[]");

        assertEquals("[]", result);
    }

    @Test
    @DisplayName("convertJavaObjectToJson converts simple array")
    void testConvertJavaObjectToJson_simpleArray() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "[item1, item2]");

        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
        assertTrue(result.contains("\""));
    }

    @Test
    @DisplayName("convertJavaObjectToJson converts array with numbers")
    void testConvertJavaObjectToJson_numberArray() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "[1, 2, 3]");

        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    @DisplayName("convertJavaObjectToJson converts empty object")
    void testConvertJavaObjectToJson_emptyObject() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{}");

        assertEquals("{}", result);
    }

    @Test
    @DisplayName("convertJavaObjectToJson converts object with single key=value")
    void testConvertJavaObjectToJson_singlePair() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key=value}");

        assertTrue(result.contains("\"key\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    @DisplayName("convertJavaObjectToJson converts object with multiple key=value pairs")
    void testConvertJavaObjectToJson_multiplePairs() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{key1=value1, key2=value2}");

        assertTrue(result.contains("\"key1\""));
        assertTrue(result.contains("\"value1\""));
        assertTrue(result.contains("\"key2\""));
        assertTrue(result.contains("\"value2\""));
    }

    @Test
    @DisplayName("convertJavaObjectToJson handles nested objects")
    void testConvertJavaObjectToJson_nestedObjects() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{outer={inner=value}}");

        assertTrue(result.contains("\"outer\""));
        assertTrue(result.contains("\"inner\""));
        assertTrue(result.contains("\"value\""));
    }

    @Test
    @DisplayName("convertJavaObjectToJson handles nested arrays")
    void testConvertJavaObjectToJson_nestedArrays() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("convertJavaObjectToJson", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "[[1, 2], [3, 4]]");

        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    // DETECT ACTION TYPE FROM STEP TESTS

    @Test
    @DisplayName("detectActionTypeFromStep detects client action when player is set")
    void testDetectActionTypeFromStep_clientAction() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectActionTypeFromStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.action = "some action";
        step.player = "test_player";

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, step);

        assertEquals("👤", result.get("icon"));
        assertEquals("CLIENT", result.get("label"));
        assertEquals("client", result.get("cssClass"));
    }

    @Test
    @DisplayName("detectActionTypeFromStep detects server action for rcon commands")
    void testDetectActionTypeFromStep_rconCommand() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectActionTypeFromStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.action = "rcon: some command";

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, step);

        assertEquals("🖥️", result.get("icon"));
        assertEquals("SERVER", result.get("label"));
        assertEquals("server", result.get("cssClass"));
    }

    @Test
    @DisplayName("detectActionTypeFromStep detects server action for give command")
    void testDetectActionTypeFromStep_giveCommand() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectActionTypeFromStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.action = "give player diamond";

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, step);

        assertEquals("🖥️", result.get("icon"));
        assertEquals("SERVER", result.get("label"));
        assertEquals("server", result.get("cssClass"));
    }

    @Test
    @DisplayName("detectActionTypeFromStep detects server action for spawn command")
    void testDetectActionTypeFromStep_spawnCommand() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectActionTypeFromStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.action = "spawn zombie";

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, step);

        assertEquals("🖥️", result.get("icon"));
        assertEquals("SERVER", result.get("label"));
    }

    @Test
    @DisplayName("detectActionTypeFromStep defaults to workflow for unknown actions")
    void testDetectActionTypeFromStep_workflowDefault() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectActionTypeFromStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.action = "unknown action";

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, step);

        assertEquals("⚙️", result.get("icon"));
        assertEquals("WORKFLOW", result.get("label"));
        assertEquals("workflow", result.get("cssClass"));
    }

    @Test
    @DisplayName("detectActionTypeFromStep handles null action")
    void testDetectActionTypeFromStep_nullAction() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectActionTypeFromStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.action = null;

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, step);

        assertEquals("⚙️", result.get("icon"));
        assertEquals("WORKFLOW", result.get("label"));
    }

    // DETECT RESULT TYPE TESTS

    @Test
    @DisplayName("detectResultType detects RCON response from action type")
    void testDetectResultType_rconResponse() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectResultType", String.class, Map.class);
        method.setAccessible(true);

        Map<String, String> actionType = new HashMap<>();
        actionType.put("label", "RCON");

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, "some response", actionType);

        assertEquals("🖥️", result.get("icon"));
        assertEquals("RCON RESPONSE", result.get("label"));
        assertEquals("rcon", result.get("cssClass"));
    }

    @Test
    @DisplayName("detectResultType detects OP response from action type")
    void testDetectResultType_opResponse() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectResultType", String.class, Map.class);
        method.setAccessible(true);

        Map<String, String> actionType = new HashMap<>();
        actionType.put("label", "OP");

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, "some response", actionType);

        assertEquals("🖥️", result.get("icon"));
        assertEquals("RCON RESPONSE", result.get("label"));
    }

    @Test
    @DisplayName("detectResultType detects PLAYER result from action type")
    void testDetectResultType_playerResult() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectResultType", String.class, Map.class);
        method.setAccessible(true);

        Map<String, String> actionType = new HashMap<>();
        actionType.put("label", "PLAYER");

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, "some response", actionType);

        assertEquals("👤", result.get("icon"));
        assertEquals("PLAYER RESULT", result.get("label"));
    }

    @Test
    @DisplayName("detectResultType detects CLIENT response from action type")
    void testDetectResultType_clientResponse() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectResultType", String.class, Map.class);
        method.setAccessible(true);

        Map<String, String> actionType = new HashMap<>();
        actionType.put("label", "CLIENT");

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, "some response", actionType);

        assertEquals("🤖", result.get("icon"));
        assertEquals("CLIENT RESPONSE", result.get("label"));
    }

    @Test
    @DisplayName("detectResultType detects client response from content")
    void testDetectResultType_detectsClientFromContent() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectResultType", String.class, Map.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, "{\"chatmessages\":[]}", null);

        assertEquals("🤖", result.get("icon"));
        assertEquals("CLIENT RESPONSE", result.get("label"));
    }

    @Test
    @DisplayName("detectResultType detects server response from Minecraft color codes")
    void testDetectResultType_detectsServerFromColorCodes() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectResultType", String.class, Map.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, "§aSome message", null);

        assertEquals("🖥️", result.get("icon"));
        assertEquals("SERVER RESPONSE", result.get("label"));
    }

    @Test
    @DisplayName("detectResultType defaults to RESULT for unknown content")
    void testDetectResultType_defaultResult() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("detectResultType", String.class, Map.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(null, "plain response", null);

        assertEquals("📄", result.get("icon"));
        assertEquals("RESULT", result.get("label"));
        assertEquals("", result.get("cssClass"));
    }

    // TRANSFORM STORY TESTS

    @Test
    @DisplayName("transformStory converts story to map")
    void testTransformStory() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStory", TestReporter.TestStory.class);
        method.setAccessible(true);

        TestReporter.TestStory story = new TestReporter.TestStory("Test Story");
        TestReporter.TestStep step = new TestReporter.TestStep("Test Step");
        step.passed = true;
        story.addStep(step);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(null, story);

        assertEquals("Test Story", result.get("name"));
        assertTrue((Boolean) result.get("passed"));
        assertNotNull(result.get("steps"));
    }

    @Test
    @DisplayName("transformStory includes passed and failed counts")
    void testTransformStory_counts() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStory", TestReporter.TestStory.class);
        method.setAccessible(true);

        TestReporter.TestStory story = new TestReporter.TestStory("Test Story");
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.passed = true;
        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.passed = false;
        story.addStep(step1);
        story.addStep(step2);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(null, story);

        assertEquals(1, result.get("passedCount"));
        assertEquals(1, result.get("failedCount"));
    }

    // TRANSFORM STORIES TESTS

    @Test
    @DisplayName("transformStories converts story list to map list")
    void testTransformStories() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStories", List.class);
        method.setAccessible(true);

        TestReporter.TestStory story1 = new TestReporter.TestStory("Story 1");
        TestReporter.TestStory story2 = new TestReporter.TestStory("Story 2");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(null, List.of(story1, story2));

        assertEquals(2, result.size());
        assertEquals("Story 1", result.get(0).get("name"));
        assertEquals("Story 2", result.get(1).get("name"));
    }

    @Test
    @DisplayName("transformStories handles empty list")
    void testTransformStories_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStories", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(null, List.of());

        assertTrue(result.isEmpty());
    }

    // TRANSFORM STEPS TESTS

    @Test
    @DisplayName("transformSteps converts step list to map list")
    void testTransformSteps() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformSteps", List.class);
        method.setAccessible(true);

        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(null, List.of(step1, step2));

        assertEquals(2, result.size());
        assertEquals("Step 1", result.get(0).get("name"));
        assertEquals("Step 2", result.get(1).get("name"));
    }

    @Test
    @DisplayName("transformSteps handles empty list")
    void testTransformSteps_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformSteps", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(null, List.of());

        assertTrue(result.isEmpty());
    }

    // TRANSFORM STEP TESTS

    @Test
    @DisplayName("transformStep includes action type detection")
    void testTransformStep_actionType() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.action = "give item";
        step.player = null;

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(null, step);

        assertNotNull(result.get("actionType"));
        @SuppressWarnings("unchecked")
        Map<String, String> actionType = (Map<String, String>) result.get("actionType");
        assertEquals("SERVER", actionType.get("label"));
    }

    @Test
    @DisplayName("transformStep includes execution context")
    void testTransformStep_executionContext() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.executor = "RCON";
        step.executorPlayer = "admin";
        step.isOperator = true;

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(null, step);

        assertNotNull(result.get("executionContext"));
        @SuppressWarnings("unchecked")
        Map<String, Object> execContext = (Map<String, Object>) result.get("executionContext");
        assertEquals("RCON", execContext.get("executor"));
        assertEquals("admin", execContext.get("executorPlayer"));
        assertEquals(true, execContext.get("isOperator"));
    }

    @Test
    @DisplayName("transformStep handles null executor")
    void testTransformStep_nullExecutor() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test step");
        step.executor = null;

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) method.invoke(null, step);

        @SuppressWarnings("unchecked")
        Map<String, Object> execContext = (Map<String, Object>) result.get("executionContext");
        assertEquals("UNKNOWN", execContext.get("executor"));
    }

    // TRANSFORM LOGS TESTS

    @Test
    @DisplayName("transformLogs converts log entries to maps")
    void testTransformLogs() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformLogs", List.class);
        method.setAccessible(true);

        TestReporter reporter = new TestReporter();
        TestReporter.LogEntry entry = reporter.new LogEntry(
            "2023-01-01 12:00:00",
            TestReporter.LogType.RCON,
            "Test message",
            "admin"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(null, List.of(entry));

        assertEquals(1, result.size());
        assertEquals("2023-01-01 12:00:00", result.get(0).get("timestamp"));
        assertEquals("Test message", result.get(0).get("message"));
    }

    @Test
    @DisplayName("transformLogs includes type icon and css class")
    void testTransformLogs_typeInfo() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformLogs", List.class);
        method.setAccessible(true);

        TestReporter reporter = new TestReporter();
        TestReporter.LogEntry entry = reporter.new LogEntry(
            "2023-01-01 12:00:00",
            TestReporter.LogType.RCON,
            "Test message",
            null
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(null, List.of(entry));

        @SuppressWarnings("unchecked")
        Map<String, Object> type = (Map<String, Object>) result.get(0).get("type");
        assertNotNull(type.get("cssClass"));
        assertNotNull(type.get("icon"));
    }

    @Test
    @DisplayName("transformLogs appends username to label when present")
    void testTransformLogs_usernameInLabel() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformLogs", List.class);
        method.setAccessible(true);

        TestReporter reporter = new TestReporter();
        TestReporter.LogEntry entry = reporter.new LogEntry(
            "2023-01-01 12:00:00",
            TestReporter.LogType.RCON,
            "Test message",
            "admin"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(null, List.of(entry));

        String label = (String) result.get(0).get("label");
        assertTrue(label.contains("admin"));
    }

    // RENDER COLLAPSIBLE JSON TREE TESTS

    @Test
    @DisplayName("renderCollapsibleJsonTree returns empty string for null")
    void testRenderCollapsibleJsonTree_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderCollapsibleJsonTree", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (String) null);

        assertEquals("", result);
    }

    @Test
    @DisplayName("renderCollapsibleJsonTree returns empty string for empty input")
    void testRenderCollapsibleJsonTree_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderCollapsibleJsonTree", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertEquals("", result);
    }

    @Test
    @DisplayName("renderCollapsibleJsonTree returns empty string for whitespace")
    void testRenderCollapsibleJsonTree_whitespace() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderCollapsibleJsonTree", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "   ");

        assertEquals("", result);
    }

    @Test
    @DisplayName("renderCollapsibleJsonTree renders valid JSON")
    void testRenderCollapsibleJsonTree_validJson() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderCollapsibleJsonTree", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"value\"}");

        assertTrue(result.contains("<div class=\"json-tree\">"));
    }

    @Test
    @DisplayName("renderCollapsibleJsonTree escapes non-JSON text")
    void testRenderCollapsibleJsonTree_nonJson() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("renderCollapsibleJsonTree", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "plain text");

        assertTrue(result.contains("&lt;") || result.contains("plain text"));
    }

    // FORMAT RESPONSE FOR DISPLAY TESTS

    @Test
    @DisplayName("formatResponseForDisplay returns empty string for null")
    void testFormatResponseForDisplay_null() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatResponseForDisplay", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, (String) null);

        assertEquals("", result);
    }

    @Test
    @DisplayName("formatResponseForDisplay returns empty string for empty input")
    void testFormatResponseForDisplay_empty() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatResponseForDisplay", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "");

        assertEquals("", result);
    }

    @Test
    @DisplayName("formatResponseForDisplay formats JSON objects")
    void testFormatResponseForDisplay_jsonObject() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatResponseForDisplay", String.class);
        method.setAccessible(true);

        String longJson = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\",\"key4\":\"value4\",\"key5\":\"value5\",\"key6\":\"value6\"}";
        String result = (String) method.invoke(null, longJson);

        assertTrue(result.contains("\n"));
    }

    @Test
    @DisplayName("formatResponseForDisplay returns short JSON as-is")
    void testFormatResponseForDisplay_shortJson() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("formatResponseForDisplay", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "{\"key\":\"value\"}");

        assertEquals("{\"key\":\"value\"}", result);
    }

    // READ RESOURCE FILE TESTS

    @Test
    @DisplayName("readResourceFile throws IOException for non-existent resource")
    void testReadResourceFile_nonExistentResource() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("readResourceFile", String.class);
        method.setAccessible(true);

        // Test with a non-existent resource path
        // IOException gets wrapped in InvocationTargetException when using reflection
        Exception exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(null, "templates/non-existent-file.txt");
        });

        // Unwrap to check the cause is IOException
        assertTrue(exception.getCause() instanceof java.io.IOException);
        assertTrue(exception.getCause().getMessage().contains("Resource not found"));
    }

    // TRANSFORM STEP TESTS

    @Test
    @DisplayName("transformStep includes startTime when set")
    void testTransformStep_includesStartTime() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test Step");
        step.startTime = java.time.LocalDateTime.now();

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) method.invoke(null, step);

        assertTrue(result.containsKey("startTime"));
        assertNotNull(result.get("startTime"));
    }

    @Test
    @DisplayName("transformStep includes endTime when set")
    void testTransformStep_includesEndTime() throws Exception {
        Method method = HtmlReportGenerator.class.getDeclaredMethod("transformStep", TestReporter.TestStep.class);
        method.setAccessible(true);

        TestReporter.TestStep step = new TestReporter.TestStep("Test Step");
        step.endTime = java.time.LocalDateTime.now();

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) method.invoke(null, step);

        assertTrue(result.containsKey("endTime"));
        assertNotNull(result.get("endTime"));
    }
}
