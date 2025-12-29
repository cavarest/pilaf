package org.cavarest.pilaf.report;

import org.cavarest.pilaf.model.TestResult;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * PILAF Test Reporter - Generates comprehensive test reports with evidence logs.
 * Supports multiple test stories with navigation and collapsible sections.
 */
public class TestReporter {
    private final String suiteName;
    private final List<TestStory> stories = new ArrayList<>();
    private TestStory currentStory;
    private final StringBuilder serverLogs = new StringBuilder();
    private final StringBuilder clientLogs = new StringBuilder();
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private String outputDir = "target/pilaf-reports";
    private boolean passed = true;
    private String failureReason;

    // Legacy support
    private final List<TestStep> steps = new ArrayList<>();
    private final List<TestResult> results = new ArrayList<>();

    public TestReporter() {
        this("PILAF Test Suite");
    }

    public TestReporter(String suiteName) {
        this.suiteName = suiteName;
        this.startTime = LocalDateTime.now();
    }

    public void setOutputDir(String dir) {
        this.outputDir = dir;
    }

    // === Story-based reporting ===

    /**
     * Start a new test story (grouping of related test steps)
     */
    public TestStory story(String name) {
        TestStory story = new TestStory(name);
        stories.add(story);
        currentStory = story;
        return story;
    }

    /**
     * Get the current story, or create a default one
     */
    public TestStory getCurrentStory() {
        if (currentStory == null) {
            currentStory = new TestStory(suiteName);
            stories.add(currentStory);
        }
        return currentStory;
    }

    // === Step-based reporting (legacy compatibility) ===

    /**
     * Record a test step with assertion and evidence
     * Steps are added to the current story
     */
    public TestStep step(String name) {
        TestStep step = new TestStep(name);
        getCurrentStory().addStep(step);
        steps.add(step); // Also add to flat list for legacy compatibility
        return step;
    }

    /**
     * Add server log entry
     */
    public void logServer(String log) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        serverLogs.append("[").append(timestamp).append("] ").append(log).append("\n");
    }

    /**
     * Add client log entry
     */
    public void logClient(String log) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        clientLogs.append("[").append(timestamp).append("] ").append(log).append("\n");
    }

    /**
     * Mark test as failed
     */
    public void fail(String reason) {
        this.passed = false;
        this.failureReason = reason;
    }

    // === Legacy result-based reporting ===

    public void addResult(TestResult result) {
        results.add(result);
    }

    // === Report generation ===

    /**
     * Complete the test and generate all reports
     */
    public void complete() throws IOException {
        this.endTime = LocalDateTime.now();
        Files.createDirectories(Paths.get(outputDir));

        generateDetailedTextReport();
        generateDetailedJsonReport();
        generateDetailedJUnitXml();

        // Generate HTML with multi-story support
        HtmlReportGenerator.generate(suiteName, passed, startTime, endTime,
            stories.isEmpty() ? Collections.singletonList(new TestStory(suiteName, steps)) : stories,
            serverLogs.toString(), clientLogs.toString(), outputDir);

        printSummary();
    }

    public void generateAllReports() throws IOException {
        complete();
    }

    private void generateDetailedTextReport() throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════════════════════════════════════════\n");
        report.append("                      PILAF TEST REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════════════════════\n\n");

        report.append("Test: ").append(suiteName).append("\n");
        report.append("Status: ").append(passed ? "✅ PASSED" : "❌ FAILED").append("\n");
        report.append("Start Time: ").append(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("End Time: ").append(endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("Duration: ").append(Duration.between(startTime, endTime).toMillis()).append("ms\n");

        if (!passed && failureReason != null) {
            report.append("Failure Reason: ").append(failureReason).append("\n");
        }
        report.append("\n");

        // Test Steps
        if (!steps.isEmpty()) {
            report.append("───────────────────────────────────────────────────────────────────────────────\n");
            report.append("                           TEST STEPS\n");
            report.append("───────────────────────────────────────────────────────────────────────────────\n\n");

            for (int i = 0; i < steps.size(); i++) {
                TestStep step = steps.get(i);
                report.append(String.format("Step %d: %s\n", i + 1, step.name));
                report.append("  Status: ").append(step.passed ? "✅ PASSED" : "❌ FAILED").append("\n");

                if (step.action != null) {
                    report.append("  Action: ").append(step.action).append("\n");
                }
                if (step.expected != null) {
                    report.append("  Expected: ").append(step.expected).append("\n");
                }
                if (step.actual != null) {
                    report.append("  Actual: ").append(step.actual).append("\n");
                }
                if (!step.evidence.isEmpty()) {
                    report.append("  Evidence:\n");
                    for (String evidence : step.evidence) {
                        report.append("    • ").append(evidence).append("\n");
                    }
                }
                report.append("\n");
            }
        }

        // Server Logs
        if (serverLogs.length() > 0) {
            report.append("───────────────────────────────────────────────────────────────────────────────\n");
            report.append("                         MINECRAFT SERVER LOGS\n");
            report.append("───────────────────────────────────────────────────────────────────────────────\n\n");
            report.append(serverLogs.toString());
            report.append("\n");
        }

        // Client Logs
        if (clientLogs.length() > 0) {
            report.append("───────────────────────────────────────────────────────────────────────────────\n");
            report.append("                         MINEFLAYER CLIENT LOGS\n");
            report.append("───────────────────────────────────────────────────────────────────────────────\n\n");
            report.append(clientLogs.toString());
            report.append("\n");
        }

        report.append("═══════════════════════════════════════════════════════════════════════════════\n");

        String filename = suiteName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.txt";
        Files.writeString(Paths.get(outputDir, filename), report.toString());
    }

    private void generateDetailedJsonReport() throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"suiteName\": \"").append(escapeJson(suiteName)).append("\",\n");
        json.append("  \"status\": \"").append(passed ? "PASSED" : "FAILED").append("\",\n");
        json.append("  \"startTime\": \"").append(startTime).append("\",\n");
        json.append("  \"endTime\": \"").append(endTime).append("\",\n");
        json.append("  \"durationMs\": ").append(Duration.between(startTime, endTime).toMillis()).append(",\n");

        if (!passed && failureReason != null) {
            json.append("  \"failureReason\": \"").append(escapeJson(failureReason)).append("\",\n");
        }

        json.append("  \"stories\": [\n");
        List<TestStory> storiesToWrite = stories.isEmpty() ?
            Collections.singletonList(new TestStory(suiteName, steps)) : stories;

        for (int s = 0; s < storiesToWrite.size(); s++) {
            TestStory story = storiesToWrite.get(s);
            json.append("    {\n");
            json.append("      \"name\": \"").append(escapeJson(story.name)).append("\",\n");
            json.append("      \"status\": \"").append(story.isPassed() ? "PASSED" : "FAILED").append("\",\n");
            json.append("      \"steps\": [\n");

            List<TestStep> storySteps = story.getSteps();
            for (int i = 0; i < storySteps.size(); i++) {
                TestStep step = storySteps.get(i);
                json.append("        {\n");
                json.append("          \"name\": \"").append(escapeJson(step.name)).append("\",\n");
                json.append("          \"status\": \"").append(step.passed ? "PASSED" : "FAILED").append("\"");
                if (step.action != null) {
                    json.append(",\n          \"action\": \"").append(escapeJson(step.action)).append("\"");
                }
                if (step.expected != null) {
                    json.append(",\n          \"expected\": \"").append(escapeJson(step.expected)).append("\"");
                }
                if (step.actual != null) {
                    json.append(",\n          \"actual\": \"").append(escapeJson(step.actual)).append("\"");
                }
                if (!step.evidence.isEmpty()) {
                    json.append(",\n          \"evidence\": [");
                    for (int j = 0; j < step.evidence.size(); j++) {
                        json.append("\"").append(escapeJson(step.evidence.get(j))).append("\"");
                        if (j < step.evidence.size() - 1) json.append(", ");
                    }
                    json.append("]");
                }
                json.append("\n        }").append(i < storySteps.size() - 1 ? "," : "").append("\n");
            }
            json.append("      ]\n");
            json.append("    }").append(s < storiesToWrite.size() - 1 ? "," : "").append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");

        String filename = suiteName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.json";
        Files.writeString(Paths.get(outputDir, filename), json.toString());
    }

    private void generateDetailedJUnitXml() throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite name=\"").append(escapeXml(suiteName)).append("\" ");
        xml.append("tests=\"").append(steps.isEmpty() ? results.size() : steps.size()).append("\" ");
        xml.append("failures=\"").append(getFailedCount()).append("\" ");
        xml.append("time=\"").append(Duration.between(startTime, endTime).toMillis() / 1000.0).append("\">\n");

        if (!steps.isEmpty()) {
            for (TestStep step : steps) {
                xml.append("  <testcase name=\"").append(escapeXml(step.name)).append("\" ");
                xml.append("classname=\"").append(escapeXml(suiteName)).append("\">\n");

                if (!step.passed) {
                    xml.append("    <failure message=\"Assertion failed\">\n");
                    if (step.expected != null) xml.append("      Expected: ").append(escapeXml(step.expected)).append("\n");
                    if (step.actual != null) xml.append("      Actual: ").append(escapeXml(step.actual)).append("\n");
                    xml.append("    </failure>\n");
                }

                if (!step.evidence.isEmpty()) {
                    xml.append("    <system-out><![CDATA[\n");
                    for (String e : step.evidence) {
                        xml.append(e).append("\n");
                    }
                    xml.append("    ]]></system-out>\n");
                }

                xml.append("  </testcase>\n");
            }
        } else {
            // Fallback to legacy results
            for (TestResult r : results) {
                xml.append("  <testcase name=\"").append(escapeXml(r.getStoryName())).append("\">\n");
                if (!r.isSuccess()) xml.append("    <failure/>\n");
                xml.append("  </testcase>\n");
            }
        }

        xml.append("</testsuite>\n");

        String filename = "TEST-" + suiteName.replaceAll("[^a-zA-Z0-9]", "_") + ".xml";
        Files.writeString(Paths.get(outputDir, filename), xml.toString());
    }

    private void printSummary() {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════");
        System.out.println("                      PILAF TEST SUMMARY");
        System.out.println("═══════════════════════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Suite: " + suiteName);
        System.out.println("Status: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        System.out.println("Duration: " + Duration.between(startTime, endTime).toMillis() + "ms");
        System.out.println();

        if (!stories.isEmpty()) {
            System.out.println("Stories:");
            for (TestStory story : stories) {
                System.out.printf("  %s %s (%d steps)%n",
                    story.isPassed() ? "✅" : "❌", story.name, story.getSteps().size());
            }
        } else if (!steps.isEmpty()) {
            System.out.println("Steps:");
            for (int i = 0; i < steps.size(); i++) {
                TestStep step = steps.get(i);
                System.out.printf("  %d. %s %s%n", i + 1, step.passed ? "✅" : "❌", step.name);
            }
        }

        System.out.println();
        System.out.println("Reports generated in: " + outputDir + "/");
        System.out.println("═══════════════════════════════════════════════════════════════════════════════");
    }

    // === Legacy methods ===

    public void generateTextReport() throws IOException {
        generateDetailedTextReport();
    }

    public void generateJsonReport() throws IOException {
        generateDetailedJsonReport();
    }

    public void generateJUnitXml() throws IOException {
        generateDetailedJUnitXml();
    }

    // === Utility methods ===

    public boolean hasFailures() {
        if (!steps.isEmpty()) {
            return steps.stream().anyMatch(s -> !s.passed);
        }
        return results.stream().anyMatch(r -> !r.isSuccess());
    }

    public int getPassedCount() {
        if (!steps.isEmpty()) {
            return (int) steps.stream().filter(s -> s.passed).count();
        }
        return (int) results.stream().filter(TestResult::isSuccess).count();
    }

    public int getFailedCount() {
        if (!steps.isEmpty()) {
            return (int) steps.stream().filter(s -> !s.passed).count();
        }
        return (int) results.stream().filter(r -> !r.isSuccess()).count();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static final String XML_AMP = "&" + "amp;";
    private static final String XML_LT = "&" + "lt;";
    private static final String XML_GT = "&" + "gt;";
    private static final String XML_QUOT = "&" + "quot;";

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", XML_AMP).replace("<", XML_LT).replace(">", XML_GT).replace("\"", XML_QUOT);
    }

    /**
     * Log entry types for per-story interleaved logs
     */
    public enum LogType {
        RCON("rcon", "🖥️"),           // Server RCON command
        PLAYER_CMD("player", "👤"),    // Player command execution
        CLIENT("client", "🤖"),        // Mineflayer client action
        SERVER("server", "📡");        // Server response/event

        public final String cssClass;
        public final String icon;

        LogType(String cssClass, String icon) {
            this.cssClass = cssClass;
            this.icon = icon;
        }
    }

    /**
     * A single log entry with timestamp, type, and optional username
     */
    public static class LogEntry {
        public final String timestamp;
        public final LogType type;
        public final String message;
        public final String username; // For player commands

        public LogEntry(String timestamp, LogType type, String message, String username) {
            this.timestamp = timestamp;
            this.type = type;
            this.message = message;
            this.username = username;
        }

        public LogEntry(String timestamp, LogType type, String message) {
            this(timestamp, type, message, null);
        }
    }

    /**
     * A test story groups related test steps together
     */
    public static class TestStory {
        public final String name;
        private final List<TestStep> steps = new ArrayList<>();
        private final List<LogEntry> logs = new ArrayList<>();
        public String description;

        public TestStory(String name) {
            this.name = name;
        }

        public TestStory(String name, List<TestStep> steps) {
            this.name = name;
            this.steps.addAll(steps);
        }

        public TestStory description(String desc) {
            this.description = desc;
            return this;
        }

        public void addStep(TestStep step) {
            steps.add(step);
        }

        public List<TestStep> getSteps() {
            return steps;
        }

        /**
         * Add a log entry to this story
         */
        public void log(LogEntry entry) {
            logs.add(entry);
        }

        /**
         * Add RCON command log
         */
        public void logRcon(String message) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            logs.add(new LogEntry(timestamp, LogType.RCON, message));
        }

        /**
         * Add player command log
         */
        public void logPlayerCommand(String username, String command) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            logs.add(new LogEntry(timestamp, LogType.PLAYER_CMD, command, username));
        }

        /**
         * Add client action log
         */
        public void logClient(String message) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            logs.add(new LogEntry(timestamp, LogType.CLIENT, message));
        }

        /**
         * Add server response log
         */
        public void logServer(String message) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            logs.add(new LogEntry(timestamp, LogType.SERVER, message));
        }

        public List<LogEntry> getLogs() {
            return logs;
        }

        public boolean isPassed() {
            return steps.stream().allMatch(s -> s.passed);
        }

        public int getPassedCount() {
            return (int) steps.stream().filter(s -> s.passed).count();
        }

        public int getFailedCount() {
            return (int) steps.stream().filter(s -> !s.passed).count();
        }
    }

    /**
     * Test step with evidence collection
     */
    public static class TestStep {
        public final String name;
        public String action;
        public String expected;
        public String actual;
        public String stateBefore;
        public String stateAfter;
        public boolean passed = true;
        public final List<String> evidence = new ArrayList<>();

        public TestStep(String name) {
            this.name = name;
        }

        public TestStep action(String action) {
            this.action = action;
            return this;
        }

        public TestStep expected(String expected) {
            this.expected = expected;
            return this;
        }

        public TestStep actual(String actual) {
            this.actual = actual;
            return this;
        }

        /**
         * Set the state before the action was performed
         */
        public TestStep stateBefore(String state) {
            this.stateBefore = state;
            return this;
        }

        /**
         * Set the state after the action was performed
         */
        public TestStep stateAfter(String state) {
            this.stateAfter = state;
            return this;
        }

        public TestStep evidence(String evidence) {
            this.evidence.add(evidence);
            return this;
        }

        public TestStep pass() {
            this.passed = true;
            return this;
        }

        public TestStep fail() {
            this.passed = false;
            return this;
        }

        public TestStep assertContains(String text, String contains) {
            if (text != null && text.contains(contains)) {
                this.passed = true;
                this.evidence.add("✓ Found '" + contains + "' in response");
            } else {
                this.passed = false;
                this.evidence.add("✗ Did not find '" + contains + "' in response");
            }
            return this;
        }

        public TestStep assertTrue(boolean condition, String description) {
            if (condition) {
                this.passed = true;
                this.evidence.add("✓ " + description);
            } else {
                this.passed = false;
                this.evidence.add("✗ " + description);
            }
            return this;
        }
    }
}
