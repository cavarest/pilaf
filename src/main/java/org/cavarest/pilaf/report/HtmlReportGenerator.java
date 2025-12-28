package org.cavarest.pilaf.report;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates beautiful HTML SPA-style test reports for PILAF.
 * Features:
 * - JSON data rendered as visual tables
 * - Setup/Test/Cleanup phase sections
 * - Collapsible entity lists
 */
public class HtmlReportGenerator {

    public static void generate(String testName, boolean passed, LocalDateTime startTime,
            LocalDateTime endTime, List<TestReporter.TestStep> steps,
            String serverLogs, String clientLogs, String outputDir) throws IOException {

        long durationMs = Duration.between(startTime, endTime).toMillis();
        int total = steps.size();
        int passedCount = (int) steps.stream().filter(s -> s.passed).count();
        int failedCount = total - passedCount;

        StringBuilder html = new StringBuilder();

        // HTML Header
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>PILAF Test Report</title>\n");
        html.append(getStyles());
        html.append("</head>\n<body>\n");
        html.append("  <div class=\"container\">\n");

        // Header section
        html.append("    <div class=\"header\">\n");
        html.append("      <h1>🧪 PILAF Test Report</h1>\n");
        html.append("      <div class=\"subtitle\">").append(esc(testName)).append("</div>\n");
        html.append("      <div class=\"time-info\">").append(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</div>\n");
        html.append("    </div>\n\n");

        // Stats cards
        html.append("    <div class=\"stats\">\n");
        html.append("      <div class=\"stat-card ").append(passed ? "passed" : "failed").append("\">\n");
        html.append("        <div class=\"value\">").append(passed ? "✅ PASS" : "❌ FAIL").append("</div>\n");
        html.append("        <div class=\"label\">Status</div>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"stat-card passed\"><div class=\"value\">").append(passedCount).append("</div><div class=\"label\">Passed</div></div>\n");
        html.append("      <div class=\"stat-card failed\"><div class=\"value\">").append(failedCount).append("</div><div class=\"label\">Failed</div></div>\n");
        html.append("      <div class=\"stat-card\"><div class=\"value\">").append(total).append("</div><div class=\"label\">Total Steps</div></div>\n");
        html.append("      <div class=\"stat-card\"><div class=\"value\">").append(formatDuration(durationMs)).append("</div><div class=\"label\">Duration</div></div>\n");
        html.append("    </div>\n\n");

        // Test Steps grouped by phase
        html.append("    <div class=\"section\">\n");
        html.append("      <div class=\"section-header\">📋 Test Steps</div>\n");
        html.append("      <div class=\"section-content\">\n");

        for (int i = 0; i < steps.size(); i++) {
            TestReporter.TestStep step = steps.get(i);
            String phase = detectPhase(step.name);

            html.append("        <div class=\"step ").append(step.passed ? "passed" : "failed").append("\">\n");
            html.append("          <div class=\"step-header\">\n");
            html.append("            <span class=\"step-num\">").append(i + 1).append("</span>\n");
            if (phase != null) {
                html.append("            <span class=\"phase-badge ").append(phase).append("\">").append(phase.toUpperCase()).append("</span>\n");
            }
            html.append("            <span class=\"step-name\">").append(esc(step.name)).append("</span>\n");
            html.append("            <span class=\"badge ").append(step.passed ? "passed" : "failed").append("\">");
            html.append(step.passed ? "✓ PASSED" : "✗ FAILED").append("</span>\n");
            html.append("          </div>\n");
            html.append("          <div class=\"step-details\">\n");

            if (step.action != null) {
                html.append("            <div class=\"detail\"><span class=\"label\">⚡ Action:</span> ").append(esc(step.action)).append("</div>\n");
            }
            if (step.expected != null) {
                html.append("            <div class=\"detail\"><span class=\"label\">📎 Expected:</span> ").append(esc(step.expected)).append("</div>\n");
            }
            if (step.actual != null) {
                html.append("            <div class=\"detail\"><span class=\"label\">📄 Actual:</span>\n");
                html.append(renderActualValue(step.actual));
                html.append("            </div>\n");
            }
            if (!step.evidence.isEmpty()) {
                html.append("            <div class=\"detail\"><span class=\"label\">🔍 Evidence:</span><ul class=\"evidence-list\">\n");
                for (String e : step.evidence) {
                    String icon = e.startsWith("✓") ? "✅" : e.startsWith("✗") ? "❌" : "•";
                    html.append("              <li>").append(icon).append(" ").append(esc(e.replaceFirst("^[✓✗]\\s*", ""))).append("</li>\n");
                }
                html.append("            </ul></div>\n");
            }
            html.append("          </div>\n");
            html.append("        </div>\n");
        }

        html.append("      </div>\n");
        html.append("    </div>\n\n");

        // Logs
        if ((serverLogs != null && !serverLogs.isEmpty()) || (clientLogs != null && !clientLogs.isEmpty())) {
            html.append("    <div class=\"section\">\n");
            html.append("      <div class=\"section-header\">📜 Execution Logs</div>\n");
            html.append("      <div class=\"section-content\">\n");

            if (serverLogs != null && !serverLogs.isEmpty()) {
                html.append("        <details open>\n");
                html.append("          <summary>🖥️ Minecraft Server Logs</summary>\n");
                html.append("          <pre class=\"log-area\">").append(esc(serverLogs)).append("</pre>\n");
                html.append("        </details>\n");
            }
            if (clientLogs != null && !clientLogs.isEmpty()) {
                html.append("        <details open>\n");
                html.append("          <summary>🤖 Mineflayer Client Logs</summary>\n");
                html.append("          <pre class=\"log-area\">").append(esc(clientLogs)).append("</pre>\n");
                html.append("        </details>\n");
            }

            html.append("      </div>\n");
            html.append("    </div>\n\n");
        }

        // Footer
        html.append("    <div class=\"footer\">\n");
        html.append("      Generated by <strong>PILAF</strong> - Plugin Integration and Live Automation Framework\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append(getScripts());
        html.append("</body>\n</html>\n");

        String filename = testName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.html";
        Files.writeString(Paths.get(outputDir, filename), html.toString());
    }

    /**
     * Detect phase from step name (setup, test, cleanup)
     */
    private static String detectPhase(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("setup") || lower.contains("prepare") || lower.contains("give") ||
            lower.contains("spawn") || lower.contains("clear") || lower.contains("connect")) {
            return "setup";
        }
        if (lower.contains("cleanup") || lower.contains("remove") || lower.contains("disconnect")) {
            return "cleanup";
        }
        if (lower.contains("verify") || lower.contains("check") || lower.contains("assert") ||
            lower.contains("use") || lower.contains("attempt") || lower.contains("test")) {
            return "test";
        }
        return null;
    }

    /**
     * Render the actual value - detect JSON and render it nicely
     */
    private static String renderActualValue(String actual) {
        if (actual == null || actual.isEmpty()) {
            return "<span class=\"no-value\">No value</span>\n";
        }

        // Check if it's JSON
        String trimmed = actual.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return renderJsonValue(trimmed);
        }

        // Plain text
        return "<span class=\"actual-text\">" + esc(actual) + "</span>\n";
    }

    /**
     * Render JSON as a visual component
     */
    private static String renderJsonValue(String json) {
        StringBuilder sb = new StringBuilder();

        // Try to detect entity list
        if (json.contains("\"entities\"") && json.contains("\"type\"")) {
            sb.append("<details class=\"json-viewer\">\n");
            sb.append("  <summary>📦 Entity List (click to expand)</summary>\n");
            sb.append("  <div class=\"entity-grid\">\n");

            // Parse entities - simple regex approach
            Pattern entityPattern = Pattern.compile("\\{\"id\":(\\d+),\"type\":\"([^\"]+)\",\"name\":\"([^\"]+)\",\"position\":\\{\"x\":([\\d.-]+),\"y\":([\\d.-]+),\"z\":([\\d.-]+)\\}\\}");
            Matcher m = entityPattern.matcher(json);

            int count = 0;
            while (m.find() && count < 20) { // Limit to 20 entities
                String type = m.group(2);
                String name = m.group(3);
                String x = String.format("%.1f", Double.parseDouble(m.group(4)));
                String y = String.format("%.1f", Double.parseDouble(m.group(5)));
                String z = String.format("%.1f", Double.parseDouble(m.group(6)));

                String icon = getEntityIcon(type, name);
                String typeClass = type.replace("\"", "");

                sb.append("    <div class=\"entity-card ").append(typeClass).append("\">\n");
                sb.append("      <span class=\"entity-icon\">").append(icon).append("</span>\n");
                sb.append("      <span class=\"entity-name\">").append(esc(name)).append("</span>\n");
                sb.append("      <span class=\"entity-pos\">").append(x).append(", ").append(y).append(", ").append(z).append("</span>\n");
                sb.append("    </div>\n");
                count++;
            }

            // Count remaining
            Pattern countPattern = Pattern.compile("\\{\"id\":");
            Matcher countMatcher = countPattern.matcher(json);
            int total = 0;
            while (countMatcher.find()) total++;
            if (total > 20) {
                sb.append("    <div class=\"entity-more\">... and ").append(total - 20).append(" more entities</div>\n");
            }

            sb.append("  </div>\n");
            sb.append("</details>\n");
            return sb.toString();
        }

        // Check for inventory
        if (json.contains("\"items\"")) {
            sb.append("<details class=\"json-viewer\">\n");
            sb.append("  <summary>🎒 Inventory</summary>\n");
            if (json.contains("[]") || json.equals("{\"items\":[]}")) {
                sb.append("  <div class=\"empty-list\">📭 Empty inventory</div>\n");
            } else {
                sb.append("  <pre class=\"json-pretty\">").append(prettyPrintJson(json)).append("</pre>\n");
            }
            sb.append("</details>\n");
            return sb.toString();
        }

        // Generic JSON
        sb.append("<details class=\"json-viewer\">\n");
        sb.append("  <summary>📋 JSON Data</summary>\n");
        sb.append("  <pre class=\"json-pretty\">").append(prettyPrintJson(json)).append("</pre>\n");
        sb.append("</details>\n");
        return sb.toString();
    }

    private static String getEntityIcon(String type, String name) {
        switch (name.toLowerCase()) {
            case "zombie": return "🧟";
            case "skeleton": return "💀";
            case "creeper": return "💥";
            case "spider": return "🕷️";
            case "enderman": return "👁️";
            case "pig": return "🐷";
            case "cow": return "🐄";
            case "sheep": return "🐑";
            case "chicken": return "🐔";
            case "bat": return "🦇";
            case "bee": return "🐝";
            case "witch": return "🧙";
            case "item": return "📦";
            case "chest_minecart": return "🛒";
            case "glow_squid": return "🦑";
            default:
                if ("hostile".equals(type)) return "👹";
                if ("animal".equals(type)) return "🐾";
                if ("passive".equals(type)) return "🌊";
                return "❓";
        }
    }

    private static String prettyPrintJson(String json) {
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

        return esc(sb.toString());
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        return String.format("%dm %ds", ms / 60000, (ms % 60000) / 1000);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
    private static String getStyles() {
        StringBuilder css = new StringBuilder();
        css.append("  <style>\n");
        css.append("    * { box-sizing: border-box; margin: 0; padding: 0; }\n");
        css.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0d1117; color: #c9d1d9; line-height: 1.6; }\n");
        css.append("    .container { max-width: 1200px; margin: 0 auto; padding: 20px; }\n");
        css.append("    .header { background: linear-gradient(135deg, #238636 0%, #1f6feb 100%); padding: 40px; border-radius: 12px; margin-bottom: 30px; text-align: center; }\n");
        css.append("    .header h1 { font-size: 2em; margin-bottom: 10px; }\n");
        css.append("    .header .subtitle { opacity: 0.9; font-size: 1.1em; }\n");
        css.append("    .header .time-info { opacity: 0.7; font-size: 0.9em; margin-top: 10px; }\n");
        css.append("    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin-bottom: 30px; }\n");
        css.append("    .stat-card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 20px; text-align: center; transition: transform 0.2s; }\n");
        css.append("    .stat-card:hover { transform: translateY(-2px); }\n");
        css.append("    .stat-card .value { font-size: 1.8em; font-weight: bold; }\n");
        css.append("    .stat-card .label { color: #8b949e; font-size: 0.9em; }\n");
        css.append("    .stat-card.passed .value { color: #3fb950; }\n");
        css.append("    .stat-card.failed .value { color: #f85149; }\n");
        css.append("    .section { background: #161b22; border: 1px solid #30363d; border-radius: 8px; margin-bottom: 20px; }\n");
        css.append("    .section-header { background: #21262d; padding: 15px 20px; font-weight: 600; border-bottom: 1px solid #30363d; font-size: 1.1em; }\n");
        css.append("    .section-content { padding: 20px; }\n");
        css.append("    .step { background: #0d1117; border: 1px solid #30363d; border-radius: 6px; margin-bottom: 15px; overflow: hidden; }\n");
        css.append("    .step-header { padding: 15px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }\n");
        css.append("    .step.passed .step-header { border-left: 4px solid #3fb950; }\n");
        css.append("    .step.failed .step-header { border-left: 4px solid #f85149; }\n");
        css.append("    .step-num { background: #21262d; padding: 4px 10px; border-radius: 4px; font-weight: bold; font-size: 0.9em; }\n");
        css.append("    .step-name { font-weight: 600; flex: 1; min-width: 200px; }\n");
        css.append("    .step-details { padding: 15px; border-top: 1px solid #30363d; background: #161b22; }\n");
        css.append("    .detail { margin-bottom: 12px; }\n");
        css.append("    .detail .label { color: #8b949e; font-weight: 500; margin-right: 8px; }\n");
        css.append("    .evidence-list { margin-left: 25px; margin-top: 8px; list-style: none; }\n");
        css.append("    .evidence-list li { margin-bottom: 4px; }\n");
        css.append("    .phase-badge { padding: 3px 8px; border-radius: 4px; font-size: 0.7em; font-weight: 600; text-transform: uppercase; }\n");
        css.append("    .phase-badge.setup { background: #1f6feb; color: white; }\n");
        css.append("    .phase-badge.test { background: #a371f7; color: white; }\n");
        css.append("    .phase-badge.cleanup { background: #f0883e; color: white; }\n");
        css.append("    .badge { padding: 4px 12px; border-radius: 12px; font-size: 0.85em; font-weight: 500; }\n");
        css.append("    .badge.passed { background: #238636; color: white; }\n");
        css.append("    .badge.failed { background: #da3633; color: white; }\n");
        css.append("    .log-area { background: #0d1117; border: 1px solid #30363d; border-radius: 6px; padding: 15px; font-family: 'SF Mono', Monaco, monospace; font-size: 0.85em; max-height: 300px; overflow: auto; white-space: pre-wrap; margin-top: 10px; }\n");
        css.append("    details { margin-top: 8px; }\n");
        css.append("    summary { cursor: pointer; padding: 8px 12px; background: #21262d; border-radius: 6px; font-weight: 500; }\n");
        css.append("    summary:hover { background: #30363d; }\n");
        css.append("    .json-viewer { margin-top: 8px; }\n");
        css.append("    .json-pretty { background: #0d1117; border: 1px solid #30363d; border-radius: 6px; padding: 12px; font-family: 'SF Mono', Monaco, monospace; font-size: 0.8em; max-height: 400px; overflow: auto; white-space: pre; margin-top: 10px; }\n");
        css.append("    .entity-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; margin-top: 12px; padding: 12px; background: #0d1117; border-radius: 6px; }\n");
        css.append("    .entity-card { background: #21262d; padding: 10px; border-radius: 6px; display: flex; align-items: center; gap: 8px; }\n");
        css.append("    .entity-card.hostile { border-left: 3px solid #f85149; }\n");
        css.append("    .entity-card.animal { border-left: 3px solid #3fb950; }\n");
        css.append("    .entity-card.passive { border-left: 3px solid #1f6feb; }\n");
        css.append("    .entity-card.ambient { border-left: 3px solid #a371f7; }\n");
        css.append("    .entity-card.other { border-left: 3px solid #8b949e; }\n");
        css.append("    .entity-icon { font-size: 1.4em; }\n");
        css.append("    .entity-name { font-weight: 500; flex: 1; }\n");
        css.append("    .entity-pos { font-size: 0.75em; color: #8b949e; font-family: monospace; }\n");
        css.append("    .entity-more { grid-column: 1 / -1; text-align: center; color: #8b949e; padding: 10px; }\n");
        css.append("    .empty-list { padding: 20px; text-align: center; color: #8b949e; }\n");
        css.append("    .actual-text { display: block; background: #21262d; padding: 8px 12px; border-radius: 4px; margin-top: 4px; }\n");
        css.append("    .footer { text-align: center; padding: 30px; color: #8b949e; font-size: 0.9em; }\n");
        css.append("  </style>\n");
        return css.toString();
    }

    private static String getScripts() {
        return "  <script>\n" +
               "    document.querySelectorAll('.step.failed details').forEach(d => d.open = true);\n" +
               "  </script>\n";
    }
}
