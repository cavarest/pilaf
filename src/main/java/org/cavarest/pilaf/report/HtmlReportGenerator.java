package org.cavarest.pilaf.report;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates beautiful HTML SPA-style test reports for PILAF.
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
        html.append("      <h1>PILAF Test Report</h1>\n");
        html.append("      <div class=\"subtitle\">").append(esc(testName)).append("</div>\n");
        html.append("    </div>\n\n");

        // Stats cards
        html.append("    <div class=\"stats\">\n");
        html.append("      <div class=\"stat-card ").append(passed ? "passed" : "failed").append("\">\n");
        html.append("        <div class=\"value\">").append(passed ? "PASS" : "FAIL").append("</div>\n");
        html.append("        <div class=\"label\">Status</div>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"stat-card passed\"><div class=\"value\">").append(passedCount).append("</div><div class=\"label\">Passed</div></div>\n");
        html.append("      <div class=\"stat-card failed\"><div class=\"value\">").append(failedCount).append("</div><div class=\"label\">Failed</div></div>\n");
        html.append("      <div class=\"stat-card\"><div class=\"value\">").append(total).append("</div><div class=\"label\">Total</div></div>\n");
        html.append("      <div class=\"stat-card\"><div class=\"value\">").append(durationMs).append("ms</div><div class=\"label\">Duration</div></div>\n");
        html.append("    </div>\n\n");

        // Test Steps
        html.append("    <div class=\"section\">\n");
        html.append("      <div class=\"section-header\">Test Steps</div>\n");
        html.append("      <div class=\"section-content\">\n");

        for (int i = 0; i < steps.size(); i++) {
            TestReporter.TestStep step = steps.get(i);
            html.append("        <div class=\"step ").append(step.passed ? "passed" : "failed").append("\">\n");
            html.append("          <div class=\"step-header\">\n");
            html.append("            <span class=\"step-num\">").append(i + 1).append("</span>\n");
            html.append("            <span class=\"step-name\">").append(esc(step.name)).append("</span>\n");
            html.append("            <span class=\"badge ").append(step.passed ? "passed" : "failed").append("\">");
            html.append(step.passed ? "PASSED" : "FAILED").append("</span>\n");
            html.append("          </div>\n");
            html.append("          <div class=\"step-details\">\n");

            if (step.action != null) {
                html.append("            <div class=\"detail\"><span class=\"label\">Action:</span> ").append(esc(step.action)).append("</div>\n");
            }
            if (step.expected != null) {
                html.append("            <div class=\"detail\"><span class=\"label\">Expected:</span> ").append(esc(step.expected)).append("</div>\n");
            }
            if (step.actual != null) {
                html.append("            <div class=\"detail\"><span class=\"label\">Actual:</span> ").append(esc(step.actual)).append("</div>\n");
            }
            if (!step.evidence.isEmpty()) {
                html.append("            <div class=\"detail\"><span class=\"label\">Evidence:</span><ul>\n");
                for (String e : step.evidence) {
                    html.append("              <li>").append(esc(e)).append("</li>\n");
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
            html.append("      <div class=\"section-header\">Logs</div>\n");
            html.append("      <div class=\"section-content\">\n");

            if (serverLogs != null && !serverLogs.isEmpty()) {
                html.append("        <h4>Server Logs</h4>\n");
                html.append("        <pre class=\"log-area\">").append(esc(serverLogs)).append("</pre>\n");
            }
            if (clientLogs != null && !clientLogs.isEmpty()) {
                html.append("        <h4>Client Logs</h4>\n");
                html.append("        <pre class=\"log-area\">").append(esc(clientLogs)).append("</pre>\n");
            }

            html.append("      </div>\n");
            html.append("    </div>\n\n");
        }

        // Footer
        html.append("    <div class=\"footer\">\n");
        html.append("      Generated by PILAF at ").append(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</body>\n</html>\n");

        String filename = testName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.html";
        Files.writeString(Paths.get(outputDir, filename), html.toString());
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String getStyles() {
        return """
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0d1117; color: #c9d1d9; line-height: 1.6; }
    .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
    .header { background: linear-gradient(135deg, #238636 0%, #1f6feb 100%); padding: 40px; border-radius: 12px; margin-bottom: 30px; text-align: center; }
    .header h1 { font-size: 2em; margin-bottom: 10px; }
    .header .subtitle { opacity: 0.9; }
    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin-bottom: 30px; }
    .stat-card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 20px; text-align: center; }
    .stat-card .value { font-size: 2em; font-weight: bold; }
    .stat-card .label { color: #8b949e; font-size: 0.9em; }
    .stat-card.passed .value { color: #3fb950; }
    .stat-card.failed .value { color: #f85149; }
    .section { background: #161b22; border: 1px solid #30363d; border-radius: 8px; margin-bottom: 20px; }
    .section-header { background: #21262d; padding: 15px 20px; font-weight: 600; border-bottom: 1px solid #30363d; }
    .section-content { padding: 20px; }
    .step { background: #0d1117; border: 1px solid #30363d; border-radius: 6px; margin-bottom: 15px; overflow: hidden; }
    .step-header { padding: 15px; display: flex; align-items: center; gap: 12px; }
    .step.passed .step-header { border-left: 4px solid #3fb950; }
    .step.failed .step-header { border-left: 4px solid #f85149; }
    .step-num { background: #21262d; padding: 4px 10px; border-radius: 4px; font-weight: bold; }
    .step-name { font-weight: 600; flex: 1; }
    .step-details { padding: 15px; border-top: 1px solid #30363d; background: #161b22; }
    .detail { margin-bottom: 8px; }
    .detail .label { color: #8b949e; font-weight: 500; }
    .detail ul { margin-left: 20px; margin-top: 5px; }
    .badge { padding: 4px 12px; border-radius: 12px; font-size: 0.85em; font-weight: 500; }
    .badge.passed { background: #238636; color: white; }
    .badge.failed { background: #da3633; color: white; }
    .log-area { background: #0d1117; border: 1px solid #30363d; border-radius: 6px; padding: 15px; font-family: monospace; font-size: 0.85em; max-height: 300px; overflow: auto; white-space: pre-wrap; }
    h4 { margin: 15px 0 10px; color: #8b949e; }
    .footer { text-align: center; padding: 30px; color: #8b949e; font-size: 0.9em; }
  </style>
""";
    }
}
