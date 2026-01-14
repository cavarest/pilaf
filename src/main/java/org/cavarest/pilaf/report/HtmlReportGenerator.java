package org.cavarest.pilaf.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates beautiful HTML SPA-style test reports for Pilaf.
 * Uses Pebble template engine for clean separation of template and logic.
 */
public class HtmlReportGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static PebbleEngine createEngine() {
        // Create a new engine each time to avoid template caching issues
        return new PebbleEngine.Builder().build();
    }

    /**
     * Reads a resource file and returns its content as a string.
     */
    private static String readResourceFile(String resourcePath) throws IOException {
        try (InputStream is = HtmlReportGenerator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void generate(String suiteName, boolean passed, LocalDateTime startTime,
            LocalDateTime endTime, List<TestReporter.TestStory> stories,
            String serverLogs, String clientLogs, String outputDir) throws IOException {

        // Create fresh engine each time to avoid template caching issues
        PebbleEngine engine = createEngine();

        // Load Vue template (SPA-style, client-side rendering)
        PebbleTemplate template = engine.getTemplate("templates/report-vue.peb");

        // Read CSS and JS content from resource files
        String cssContent = readResourceFile("templates/report.css");
        String jsContent = readResourceFile("templates/report-vue.js");

        // Calculate stats
        long durationMs = Duration.between(startTime, endTime).toMillis();
        int totalSteps = stories.stream().mapToInt(s -> s.getSteps().size()).sum();
        int storiesPassed = (int) stories.stream().filter(TestReporter.TestStory::isPassed).count();
        int storiesFailed = stories.size() - storiesPassed;

        // Build context for template
        Map<String, Object> context = new HashMap<>();
        context.put("suiteName", suiteName);
        context.put("passed", passed);
        context.put("startTime", startTime.format(DATE_FORMAT));
        context.put("endTime", endTime.format(DATE_FORMAT));
        context.put("durationSeconds", durationMs / 1000.0);
        context.put("totalSteps", totalSteps);
        context.put("storiesPassed", storiesPassed);
        context.put("storiesFailed", storiesFailed);
        context.put("generatedAt", LocalDateTime.now().format(DATE_FORMAT));
        context.put("stories", transformStories(stories));

        // Add CSS and JS content for inlining
        context.put("cssContent", cssContent);
        context.put("jsContent", jsContent);

        // Serialize stories as JSON for Alpine.js SPA
        String storiesJson = serializeStoriesToJson(stories, suiteName, passed, startTime, endTime);
        context.put("storiesJson", storiesJson);

        // Add flat steps list for two-pane layout
        // Use steps from stories if available, otherwise use legacy steps list
        List<TestReporter.TestStep> allSteps = new ArrayList<>();
        for (TestReporter.TestStory story : stories) {
            allSteps.addAll(story.getSteps());
        }
        if (allSteps.isEmpty()) {
            allSteps = flattenAllSteps(stories);
        }
        context.put("steps", transformSteps(allSteps));

        // Add environment and server metrics
        context.put("javaVersion", System.getProperty("java.version"));
        context.put("osName", System.getProperty("os.name"));
        context.put("backendType", "Mineflayer + RCON");
        context.put("serverVersion", "PaperMC 1.21.8");

        // Count action types for metrics
        Map<String, Integer> actionMetrics = countActionTypes(stories);
        context.put("actionMetrics", actionMetrics);
        context.put("totalActions", actionMetrics.values().stream().mapToInt(Integer::intValue).sum());

        // Render template
        StringWriter writer = new StringWriter();
        template.evaluate(writer, context);

        // Write output
        String filename = suiteName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.html";
        Files.writeString(Paths.get(outputDir, filename), writer.toString());
    }

    /**
     * Flattens all steps from all stories into a single list.
     */
    private static List<TestReporter.TestStep> flattenAllSteps(List<TestReporter.TestStory> stories) {
        List<TestReporter.TestStep> allSteps = new ArrayList<>();
        for (TestReporter.TestStory story : stories) {
            allSteps.addAll(story.getSteps());
        }
        return allSteps;
    }

    /**
     * Serializes all stories data to JSON string for Alpine.js SPA.
     */
    private static String serializeStoriesToJson(List<TestReporter.TestStory> stories, String suiteName, boolean passed, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        Map<String, Object> reportData = new LinkedHashMap<>();
        reportData.put("suiteName", suiteName);
        reportData.put("passed", passed);
        reportData.put("startTime", startTime.format(DATE_FORMAT));
        reportData.put("endTime", endTime.format(DATE_FORMAT));
        reportData.put("durationMs", Duration.between(startTime, endTime).toMillis());
        reportData.put("generatedAt", LocalDateTime.now().format(DATE_FORMAT));
        List<Map<String, Object>> transformedStories = transformStories(stories);
        reportData.put("stories", transformedStories);
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(reportData);
    }

    /**
     * Transforms TestStory objects into template-friendly maps.
     */
    private static List<Map<String, Object>> transformStories(List<TestReporter.TestStory> stories) {
        return stories.stream()
            .map(HtmlReportGenerator::transformStory)
            .collect(Collectors.toList());
    }

    private static Map<String, Object> transformStory(TestReporter.TestStory story) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", story.name);
        map.put("passed", story.isPassed());
        map.put("passedCount", story.getPassedCount());
        map.put("failedCount", story.getFailedCount());
        map.put("steps", transformSteps(story.getSteps()));
        map.put("logs", transformLogs(story.getLogs()));
        return map;
    }

    private static List<Map<String, Object>> transformSteps(List<TestReporter.TestStep> steps) {
        return steps.stream()
            .map(HtmlReportGenerator::transformStep)
            .collect(Collectors.toList());
    }

    private static Map<String, Object> transformStep(TestReporter.TestStep step) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", step.name);
        map.put("passed", step.passed);
        map.put("expected", step.expected);
        map.put("actual", step.actual);
        map.put("stateBefore", step.stateBefore);
        map.put("stateAfter", step.stateAfter);
        map.put("player", step.player);
        map.put("assertionType", step.assertionType);

        // Format start/end times
        if (step.startTime != null) {
            map.put("startTime", step.startTime.format(DATE_FORMAT));
        }
        if (step.endTime != null) {
            map.put("endTime", step.endTime.format(DATE_FORMAT));
        }

        // Arguments
        map.put("arguments", step.arguments);

        // Action - use the action field for raw command
        map.put("action", step.action);

        // Detect action type from step.action (raw command) and step.player
        Map<String, String> actionType = detectActionTypeFromStep(step);
        map.put("actionType", actionType);

        // Actual result type inherits from action type for better labeling
        // Always include actual field - show "EMPTY RESPONSE" if null
        String actualValue = (step.actual != null) ? step.actual : "EMPTY RESPONSE";
        map.put("actual", actualValue);
        map.put("actualType", detectResultType(actualValue, actionType));
        // Store raw JSON - JavaScript will render it with the tree viewer
        map.put("actualHtml", null);  // No pre-rendered HTML, let JS handle it

        // Pre-render HTML for states
        map.put("stateBeforeHtml", renderState(step.stateBefore));
        map.put("stateAfterHtml", renderState(step.stateAfter));

        // Generate diff if both states exist and contain valid JSON
        if (step.stateBefore != null && step.stateAfter != null &&
            !step.stateBefore.trim().isEmpty() && !step.stateAfter.trim().isEmpty()) {
            String diffHtml = generateJsonDiff(step.stateBefore, step.stateAfter);
            if (diffHtml != null && !diffHtml.trim().isEmpty()) {
                map.put("diffHtml", diffHtml);
            }
        }

        // Transform evidence
        map.put("evidence", transformEvidence(step.evidence));

        // Execution context - always include, with defaults
        Map<String, Object> executionContext = new LinkedHashMap<>();
        executionContext.put("executor", step.executor != null ? step.executor : "UNKNOWN");
        if (step.executorPlayer != null) {
            executionContext.put("executorPlayer", step.executorPlayer);
        }
        if (step.isOperator != null) {
            executionContext.put("isOperator", step.isOperator);
        }
        map.put("executionContext", executionContext);

        return map;
    }

    /**
     * Renders a response as a collapsible JSON tree HTML structure.
     */
    private static String renderCollapsibleJsonTree(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        String trimmed = response.trim();
        Object data = null;

        // Try to parse as JSON
        try {
            data = MAPPER.readValue(trimmed, Object.class);
        } catch (Exception e) {
            // Try to convert Java object notation
            String converted = convertJavaToJson(trimmed);
            if (converted != null) {
                try {
                    data = MAPPER.readValue(converted, Object.class);
                } catch (Exception e2) {
                    // Return as plain text if parsing fails
                    return escapeHtml(response);
                }
            } else {
                return escapeHtml(response);
            }
        }

        // Generate collapsible tree HTML
        return renderJsonTree(data, "", 0);
    }

    /**
     * Recursively renders a JSON node as collapsible HTML.
     */
    private static String renderJsonTree(Object value, String path, int depth) {
        String type = getValueType(value);
        String nodeId = path.isEmpty() ? "root" : path;
        boolean hasChildren = type.equals("object") || type.equals("array");

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"json-tree\">");
        html.append(renderJsonNode(value, path, depth, hasChildren, nodeId));
        html.append("</div>");

        return html.toString();
    }

    /**
     * Renders a single JSON node with optional children.
     */
    private static String renderJsonNode(Object value, String path, int depth, boolean hasChildren, String nodeId) {
        String type = getValueType(value);
        StringBuilder html = new StringBuilder();

        html.append("<div class=\"line\">");

        if (hasChildren) {
            html.append("<span class=\"toggle collapsed\" onclick=\"toggleNode(this, '").append(escapeHtml(nodeId)).append("')\"></span>");
        } else {
            html.append("<span class=\"toggle\" style=\"visibility: hidden;\"></span>");
        }

        if (!path.isEmpty()) {
            String key = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            if (key.contains("[")) {
                key = key.substring(key.indexOf('['));
            }
            html.append("<span class=\"key\">").append(escapeHtml(key)).append("</span>: ");
        }

        switch (type) {
            case "object":
                @SuppressWarnings("unchecked")
                Map<String, Object> obj = (Map<String, Object>) value;
                html.append("<span class=\"bracket\">{</span>");
                html.append("<div class=\"children\" id=\"").append(escapeHtml(nodeId)).append("_children\">");
                String[] keys = obj.keySet().toArray(new String[0]);
                for (int i = 0; i < keys.length; i++) {
                    String key = keys[i];
                    String childPath = path.isEmpty() ? key : path + "." + key;
                    html.append(renderJsonNode(obj.get(key), childPath, depth + 1, false, childPath));
                    if (i < keys.length - 1) {
                        html.append(",");
                    }
                    html.append("");
                }
                html.append("</div>");
                html.append("<span class=\"bracket\">}</span>");
                break;

            case "array":
                @SuppressWarnings("unchecked")
                List<Object> arr = (List<Object>) value;
                html.append("<span class=\"bracket\">[</span>");
                html.append("<div class=\"children\" id=\"").append(escapeHtml(nodeId)).append("_children\">");
                for (int i = 0; i < arr.size(); i++) {
                    String childPath = path + "[" + i + "]";
                    html.append(renderJsonNode(arr.get(i), childPath, depth + 1, false, childPath));
                    if (i < arr.size() - 1) {
                        html.append(",");
                    }
                    html.append("");
                }
                html.append("</div>");
                html.append("<span class=\"bracket\">]</span>");
                break;

            case "string":
                html.append("<span class=\"string\">\"").append(escapeHtml((String) value)).append("\"</span>");
                break;

            case "number":
                html.append("<span class=\"number\">").append(value).append("</span>");
                break;

            case "boolean":
                html.append("<span class=\"boolean\">").append(value).append("</span>");
                break;

            case "null":
                html.append("<span class=\"null\">null</span>");
                break;

            default:
                html.append("<span class=\"string\">\"").append(escapeHtml(String.valueOf(value))).append("\"</span>");
        }

        html.append("</div>");
        return html.toString();
    }

    /**
     * Gets the type of a JSON value.
     */
    private static String getValueType(Object value) {
        if (value == null) return "null";
        if (value instanceof Map) return "object";
        if (value instanceof List) return "array";
        if (value instanceof String) return "string";
        if (value instanceof Number) return "number";
        if (value instanceof Boolean) return "boolean";
        return "unknown";
    }

    /**
     * Converts Java object notation to JSON.
     * Handles patterns like {items=[{name=value}]} -> {"items":[{"name":"value"}]}
     */
    private static String convertJavaToJson(String javaStr) {
        if (javaStr == null || javaStr.isEmpty()) return null;
        if (!javaStr.contains("=") && !javaStr.contains("[")) return null;

        try {
            return convertJavaObjectToJson(javaStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Recursively converts Java object notation to JSON.
     */
    private static String convertJavaObjectToJson(String javaStr) {
        javaStr = javaStr.trim();

        // Handle empty or simple values
        if (javaStr.isEmpty()) return "";

        // Check if it's a string (wrapped in quotes)
        if (javaStr.startsWith("\"") && javaStr.endsWith("\"")) {
            return javaStr;
        }

        // Check if it's a number
        if (javaStr.matches("-?\\d+(\\.\\d+)?")) {
            return javaStr;
        }

        // Check if it's a boolean or null
        if (javaStr.equals("true") || javaStr.equals("false") || javaStr.equals("null")) {
            return javaStr;
        }

        // Handle array notation: [item1, item2, ...]
        if (javaStr.startsWith("[") && javaStr.endsWith("]")) {
            String content = javaStr.substring(1, javaStr.length() - 1).trim();
            if (content.isEmpty()) {
                return "[]";
            }
            StringBuilder result = new StringBuilder("[");
            int depth = 0;
            int start = 0;
            boolean inString = false;

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                    inString = !inString;
                }
                if (!inString) {
                    if (c == '[' || c == '{') depth++;
                    if (c == ']' || c == '}') depth--;
                    if (depth == 0 && c == ',') {
                        String item = content.substring(start, i).trim();
                        if (!item.isEmpty()) {
                            result.append(convertJavaObjectToJson(item)).append(", ");
                        }
                        start = i + 1;
                    }
                }
            }

            String lastItem = content.substring(start).trim();
            if (!lastItem.isEmpty()) {
                result.append(convertJavaObjectToJson(lastItem));
            }

            // Remove trailing comma
            if (result.length() > 1 && result.charAt(result.length() - 1) == ',') {
                result.setLength(result.length() - 1);
            }
            result.append("]");
            return result.toString();
        }

        // Handle object notation: {key1=value1, key2=value2, ...}
        if (javaStr.startsWith("{") && javaStr.endsWith("}")) {
            String content = javaStr.substring(1, javaStr.length() - 1).trim();
            if (content.isEmpty()) {
                return "{}";
            }

            StringBuilder result = new StringBuilder("{");
            int depth = 0;
            int keyStart = 0;
            int valueStart = -1;
            boolean inString = false;
            boolean foundEquals = false;

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                    inString = !inString;
                }
                if (!inString) {
                    if (c == '[' || c == '{') depth++;
                    if (c == ']' || c == '}') depth--;
                    if (depth == 0) {
                        if (c == '=' && !foundEquals) {
                            String key = content.substring(keyStart, i).trim();
                            if (!key.isEmpty()) {
                                result.append("\"").append(escapeJsonKey(key)).append("\": ");
                            }
                            valueStart = i + 1;
                            foundEquals = true;
                        }
                        if (c == ',' && foundEquals) {
                            String value = content.substring(valueStart, i).trim();
                            if (!value.isEmpty()) {
                                result.append(convertJavaObjectToJson(value)).append(", ");
                            }
                            keyStart = i + 1;
                            valueStart = -1;
                            foundEquals = false;
                        }
                    }
                }
            }

            // Handle last key-value pair
            if (foundEquals && valueStart < content.length()) {
                String value = content.substring(valueStart).trim();
                if (!value.isEmpty()) {
                    result.append(convertJavaObjectToJson(value));
                }
            }

            result.append("}");
            return result.toString();
        }

        // Fallback: return as-is (escaped)
        return "\"" + escapeHtml(javaStr) + "\"";
    }

    private static String escapeJsonKey(String key) {
        return key.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * Detects action type from TestStep (uses raw command and player field).
     */
    private static Map<String, String> detectActionTypeFromStep(TestReporter.TestStep step) {
        Map<String, String> type = new HashMap<>();
        String action = step.action != null ? step.action.toLowerCase() : "";
        String player = step.player;

        // If player is set, it's a client action
        if (player != null && !player.isEmpty()) {
            type.put("icon", "👤");
            type.put("label", "CLIENT");
            type.put("cssClass", "client");
            return type;
        }

        // Check for server/RCON commands
        if (action.startsWith("rcon:") ||
            action.startsWith("give ") ||
            action.startsWith("spawn ") ||
            action.startsWith("kill ") ||
            action.startsWith("tp ") ||
            action.startsWith("gamemode ") ||
            action.startsWith("weather ") ||
            action.startsWith("time ") ||
            action.startsWith("clear ") ||
            action.startsWith("execute ") ||
            action.contains("entity:")) {
            type.put("icon", "🖥️");
            type.put("label", "SERVER");
            type.put("cssClass", "server");
            return type;
        }

        // Default to workflow
        type.put("icon", "⚙️");
        type.put("label", "WORKFLOW");
        type.put("cssClass", "workflow");
        return type;
    }

    /**
     * Detects result type using action type context for accurate labeling.
     * The result type inherits from the action type when available.
     */
    private static Map<String, String> detectResultType(String actual, Map<String, String> actionType) {
        Map<String, String> type = new HashMap<>();
        String lowerActual = actual.toLowerCase();

        // If we know the action type, use it to determine the response type
        if (actionType != null) {
            String actionLabel = actionType.get("label");
            if ("RCON".equals(actionLabel) || "OP".equals(actionLabel)) {
                type.put("icon", "🖥️");
                type.put("label", "RCON RESPONSE");
                type.put("cssClass", "rcon");
                return type;
            } else if ("PLAYER".equals(actionLabel)) {
                type.put("icon", "👤");
                type.put("label", "PLAYER RESULT");
                type.put("cssClass", "player");
                return type;
            } else if ("CLIENT".equals(actionLabel)) {
                type.put("icon", "🤖");
                type.put("label", "CLIENT RESPONSE");
                type.put("cssClass", "client");
                return type;
            }
        }

        // Fallback: detect from content
        if (lowerActual.contains("chatmessages") || lowerActual.contains("\"status\"")) {
            type.put("icon", "🤖");
            type.put("label", "CLIENT RESPONSE");
            type.put("cssClass", "client");
        } else if (lowerActual.contains("entities") || lowerActual.contains("inventory")) {
            type.put("icon", "🤖");
            type.put("label", "CLIENT STATE");
            type.put("cssClass", "client");
        } else if (lowerActual.contains("§")) {
            // Minecraft color codes indicate server response
            type.put("icon", "🖥️");
            type.put("label", "SERVER RESPONSE");
            type.put("cssClass", "server");
        } else {
            type.put("icon", "📄");
            type.put("label", "RESULT");
            type.put("cssClass", "");
        }
        return type;
    }

    private static List<Map<String, String>> transformEvidence(List<String> evidence) {
        return evidence.stream()
            .map(e -> {
                Map<String, String> map = new HashMap<>();
                String icon = e.startsWith("✓") ? "✅" : e.startsWith("✗") ? "❌" : "•";
                String text = e.replaceFirst("^[✓✗]\\s*", "");
                map.put("icon", icon);
                map.put("text", text);
                return map;
            })
            .collect(Collectors.toList());
    }

    private static List<Map<String, Object>> transformLogs(List<TestReporter.LogEntry> logs) {
        return logs.stream()
            .map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("timestamp", log.timestamp);
                map.put("message", log.message);
                map.put("type", Map.of("cssClass", log.type.cssClass, "icon", log.type.icon));
                String label = log.type.icon;
                if (log.username != null) {
                    label += " " + log.username;
                }
                map.put("label", label);
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * Renders state content as HTML (JSON or plain text).
     * Returns null for empty/null states so template can skip them.
     */
    private static String renderState(String state) {
        if (state == null || state.trim().isEmpty()) {
            return null;  // Return null so template skips empty state boxes
        }

        String trimmed = state.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            String formatted = prettyPrintJson(trimmed);
            if (formatted.trim().isEmpty()) {
                return null;  // Skip empty JSON
            }
            return "<pre class=\"state-json\">" + formatted + "</pre>";
        }

        String escaped = escapeHtml(state);
        if (escaped.trim().isEmpty()) {
            return null;  // Skip empty text
        }
        return "<span class=\"state-text\">" + escaped + "</span>";
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Generates a semantic JSON diff using zjsonpatch.
     * Shows RFC 6902 JSON Patch operations with path, operation type, and values.
     */
    private static String generateJsonDiff(String before, String after) {
        StringBuilder sb = new StringBuilder();

        try {
            JsonNode beforeNode = MAPPER.readTree(before.trim());
            JsonNode afterNode = MAPPER.readTree(after.trim());

            // Generate diff with flags for readable output
            EnumSet<DiffFlags> flags = EnumSet.of(
                DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE,
                DiffFlags.OMIT_MOVE_OPERATION
            );
            JsonNode patch = JsonDiff.asJson(beforeNode, afterNode, flags);

            if (patch.isEmpty()) {
                sb.append("<div class=\"diff-line unchanged\">No changes detected</div>\n");
                return sb.toString();
            }

            // Render each operation
            for (JsonNode op : patch) {
                String operation = op.get("op").asText();
                String path = op.get("path").asText();
                JsonNode value = op.get("value");
                JsonNode fromValue = op.get("fromValue");

                String cssClass;
                String icon;
                String description;

                switch (operation) {
                    case "add":
                        cssClass = "added";
                        icon = "+";
                        description = formatPath(path) + " = " + formatValue(value);
                        break;
                    case "remove":
                        cssClass = "removed";
                        icon = "−";
                        description = formatPath(path) + " (removed)";
                        break;
                    case "replace":
                        cssClass = "changed";
                        icon = "~";
                        description = formatPath(path) + ": " +
                            formatValue(fromValue) + " → " + formatValue(value);
                        break;
                    default:
                        cssClass = "unchanged";
                        icon = "?";
                        description = operation + " " + path;
                }

                sb.append("<div class=\"diff-line ").append(cssClass).append("\">")
                  .append("<span class=\"diff-sign\">").append(icon).append("</span>")
                  .append("<span class=\"diff-path\">").append(escapeHtml(description)).append("</span>")
                  .append("</div>\n");
            }

        } catch (Exception e) {
            // Return null if JSON parsing fails - don't show diff section
            // This happens when states are not valid JSON (e.g., truncated responses)
            return null;
        }

        return sb.toString();
    }

    /**
     * Formats a JSON path for display (e.g., "/entities/0/name" -> "entities[0].name")
     */
    private static String formatPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "(root)";
        }
        return path.substring(1)  // Remove leading /
                   .replaceAll("/([0-9]+)", "[$1]")  // Array indices
                   .replace("/", ".");  // Object keys
    }

    /**
     * Formats a JSON value for display (truncated if too long)
     */
    private static String formatValue(JsonNode value) {
        if (value == null) {
            return "null";
        }
        String str = value.toString();
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }
        return str;
    }

    /**
     * Pretty prints JSON for diff comparison (returns unescaped string).
     */
    private static String prettyPrintJsonForDiff(String json) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;

        for (char c : json.toCharArray()) {
            if (c == '"' && (sb.length() == 0 || sb.charAt(sb.length() - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    sb.append(c).append("\n").append("  ".repeat(++indent));
                } else if (c == '}' || c == ']') {
                    sb.append("\n").append("  ".repeat(--indent)).append(c);
                } else if (c == ',') {
                    sb.append(c).append("\n").append("  ".repeat(indent));
                } else if (c == ':') {
                    sb.append(": ");
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Pretty prints JSON with HTML escaping.
     */
    private static String prettyPrintJson(String json) {
        // First check if it's valid JSON or Java object notation
        String trimmed = json.trim();
        if ((trimmed.startsWith("{") || trimmed.startsWith("[")) && trimmed.length() > 100) {
            try {
                // Try to parse as JSON first
                JsonNode node = MAPPER.readTree(trimmed);
                String formatted = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
                return escapeHtml(formatted);
            } catch (Exception e) {
                // If not valid JSON, try to format Java object notation
                return formatJavaObject(json);
            }
        }
        return escapeHtml(json);
    }

    /**
     * Formats Java object notation (like {key=value, ...}) into pretty HTML.
     */
    private static String formatJavaObject(String input) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"json-formatted\">");

        String content = input.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);

            int indent = 0;
            int i = 0;
            while (i < content.length()) {
                char c = content.charAt(i);

                if (c == '{' || c == '[') {
                    sb.append(c).append("<br>").append("  ".repeat(++indent));
                } else if (c == '}' || c == ']') {
                    sb.append("<br>").append("  ".repeat(--indent)).append(c);
                } else if (c == ',') {
                    sb.append(c).append("<br>").append("  ".repeat(indent));
                } else if (c == '=') {
                    sb.append(": ");
                } else {
                    sb.append(escapeHtml(String.valueOf(c)));
                }
                i++;
            }
        } else {
            sb.append(escapeHtml(content));
        }

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Formats a response string for display (JSON or Java object notation).
     */
    private static String formatResponseForDisplay(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        String trimmed = response.trim();

        // Check if it's an object/array notation
        if ((trimmed.startsWith("{") || trimmed.startsWith("[")) && trimmed.length() > 50) {
            try {
                // Try to parse as JSON first
                if (trimmed.startsWith("{")) {
                    JsonNode node = MAPPER.readTree(trimmed);
                    return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
                }
            } catch (Exception e) {
                // Not valid JSON, format as Java object notation
                return formatJavaObjectNotation(trimmed);
            }
        }

        return response;
    }

    /**
     * Formats Java object notation (like {key=value, ...}) into pretty-printed string.
     */
    private static String formatJavaObjectNotation(String input) {
        StringBuilder sb = new StringBuilder();
        String content = input.trim();

        // Remove outer braces if present
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
        }

        int indent = 0;
        boolean inString = false;
        boolean escapeNext = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escapeNext) {
                current.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                current.append(c);
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    if (current.length() > 0 && current.toString().trim().endsWith(",")) {
                        sb.append(current.toString().trim());
                        current.setLength(0);
                    }
                    sb.append(current);
                    current.setLength(0);
                    sb.append(c).append("\n").append("  ".repeat(++indent));
                } else if (c == '}' || c == ']') {
                    if (current.length() > 0) {
                        String trimmed = current.toString().trim();
                        if (trimmed.endsWith(",")) {
                            trimmed = trimmed.substring(0, trimmed.length() - 1);
                        }
                        sb.append(trimmed);
                    }
                    current.setLength(0);
                    sb.append("\n").append("  ".repeat(--indent)).append(c);
                } else if (c == ',') {
                    String trimmed = current.toString().trim();
                    if (trimmed.endsWith(",")) {
                        trimmed = trimmed.substring(0, trimmed.length() - 1);
                    }
                    sb.append(trimmed).append(",").append("\n").append("  ".repeat(indent));
                    current.setLength(0);
                } else if (c == '=') {
                    current.append(": ");
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            sb.append(current.toString().trim());
        }

        return sb.toString();
    }

    /**
     * Escapes HTML special characters.
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&" + "amp;")
                .replace("<", "&" + "lt;")
                .replace(">", "&" + "gt;")
                .replace("\"", "&" + "quot;");
    }

    /**
     * Counts action types for metrics display (server/client/workflow).
     */
    private static Map<String, Integer> countActionTypes(List<TestReporter.TestStory> stories) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("server", 0);
        counts.put("client", 0);
        counts.put("workflow", 0);

        for (TestReporter.TestStory story : stories) {
            for (TestReporter.TestStep step : story.getSteps()) {
                // Detect type using the same logic as detectActionTypeFromStep
                Map<String, String> type = detectActionTypeFromStep(step);
                String cssClass = type.get("cssClass");

                if ("server".equals(cssClass)) {
                    counts.put("server", counts.get("server") + 1);
                } else if ("client".equals(cssClass)) {
                    counts.put("client", counts.get("client") + 1);
                } else {
                    counts.put("workflow", counts.get("workflow") + 1);
                }
            }
        }
        return counts;
    }
}
