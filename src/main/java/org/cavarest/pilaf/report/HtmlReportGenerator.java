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
 * Generates beautiful HTML SPA-style test reports for PILAF.
 * Uses Pebble template engine for clean separation of template and logic.
 */
public class HtmlReportGenerator {

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder().build();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void generate(String suiteName, boolean passed, LocalDateTime startTime,
            LocalDateTime endTime, List<TestReporter.TestStory> stories,
            String serverLogs, String clientLogs, String outputDir) throws IOException {

        // Load template and CSS
        PebbleTemplate template = ENGINE.getTemplate("templates/report.peb");
        String css = loadResource("templates/report.css");

        // Calculate stats
        long durationMs = Duration.between(startTime, endTime).toMillis();
        int totalSteps = stories.stream().mapToInt(s -> s.getSteps().size()).sum();
        int storiesPassed = (int) stories.stream().filter(TestReporter.TestStory::isPassed).count();
        int storiesFailed = stories.size() - storiesPassed;

        // Build context for template
        Map<String, Object> context = new HashMap<>();
        context.put("suiteName", suiteName);
        context.put("passed", passed);
        context.put("css", css);
        context.put("startTime", startTime.format(DATE_FORMAT));
        context.put("endTime", endTime.format(DATE_FORMAT));
        context.put("durationSeconds", durationMs / 1000.0);
        context.put("totalSteps", totalSteps);
        context.put("storiesPassed", storiesPassed);
        context.put("storiesFailed", storiesFailed);
        context.put("generatedAt", LocalDateTime.now().format(DATE_FORMAT));
        context.put("stories", transformStories(stories));

        // Render template
        StringWriter writer = new StringWriter();
        template.evaluate(writer, context);

        // Write output
        String filename = suiteName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.html";
        Files.writeString(Paths.get(outputDir, filename), writer.toString());
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
        Map<String, Object> map = new HashMap<>();
        map.put("name", step.name);
        map.put("passed", step.passed);
        map.put("expected", step.expected);
        map.put("stateBefore", step.stateBefore);
        map.put("stateAfter", step.stateAfter);

        // Detect action type and add emoji
        Map<String, String> actionType = null;
        if (step.action != null) {
            map.put("action", step.action);
            actionType = detectCommandType(step.action);
            map.put("actionType", actionType);
        }

        // Actual result type inherits from action type for better labeling
        if (step.actual != null) {
            map.put("actual", step.actual);
            map.put("actualType", detectResultType(step.actual, actionType));
            map.put("actualHtml", renderState(step.actual));
        }

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
        return map;
    }

    /**
     * Detects command type from action string and returns appropriate icon/label.
     */
    private static Map<String, String> detectCommandType(String action) {
        Map<String, String> type = new HashMap<>();
        String lowerAction = action.toLowerCase();

        if (lowerAction.startsWith("rcon:") || lowerAction.contains("rcon ")) {
            type.put("icon", "🖥️");
            type.put("label", "RCON");
            type.put("cssClass", "rcon");
        } else if (lowerAction.startsWith("player ") || lowerAction.contains("executes:")) {
            type.put("icon", "👤");
            type.put("label", "PLAYER");
            type.put("cssClass", "player");
        } else if (lowerAction.startsWith("op:") || lowerAction.contains("operator")) {
            type.put("icon", "⚡");
            type.put("label", "OP");
            type.put("cssClass", "rcon");
        } else if (lowerAction.startsWith("client") || lowerAction.contains("mineflayer")) {
            type.put("icon", "🤖");
            type.put("label", "CLIENT");
            type.put("cssClass", "client");
        } else {
            type.put("icon", "⚡");
            type.put("label", "ACTION");
            type.put("cssClass", "");
        }
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

    private static List<Map<String, Object>> transformLogs(List<?> logs) {
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
     * Loads a resource file as a string.
     */
    private static String loadResource(String path) throws IOException {
        try (InputStream is = HtmlReportGenerator.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
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
        return escapeHtml(prettyPrintJsonForDiff(json));
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
}
