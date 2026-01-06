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
    private boolean verbose = false;

    // Legacy support
    private final List<TestStep> steps = new ArrayList<>();
    private final List<TestResult> results = new ArrayList<>();

    /**
     * Calculate whether overall test passed based on actual results.
     * Returns false if any step failed or there was an error.
     */
    private boolean calculatePassed() {
        // Check steps
        if (!steps.isEmpty()) {
            return steps.stream().allMatch(s -> s.passed);
        }
        // Check results - if any result has an error, the test failed
        if (!results.isEmpty()) {
            return results.stream().allMatch(r -> r.isSuccess());
        }
        // If no steps/results but we have error info, mark as failed
        if (failureReason != null && !failureReason.isEmpty()) {
            return false;
        }
        // No results = passed (empty test)
        return true;
    }

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

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void generateAllReports() throws IOException {
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        Files.createDirectories(Paths.get(outputDir));

        generateDetailedTextReport();
        generateDetailedJsonReport();
        generateDetailedJUnitXml();

        // Calculate actual passed status based on all results
        boolean actualPassed = calculatePassed();

        HtmlReportGenerator.generate(suiteName, actualPassed, startTime, endTime,
            stories.isEmpty() ? Collections.singletonList(new TestStory(suiteName, steps)) : stories,
            serverLogs.toString(), clientLogs.toString(), outputDir);

        printSummary();
    }

    public TestStory story(String name) {
        TestStory story = new TestStory(name);
        stories.add(story);
        currentStory = story;
        return story;
    }

    public TestStory getCurrentStory() {
        if (currentStory == null) {
            currentStory = new TestStory(suiteName);
            stories.add(currentStory);
        }
        return currentStory;
    }

    public TestStep step(String name) {
        TestStep step = new TestStep(name);
        getCurrentStory().addStep(step);
        return step;
    }

    public void logServer(String log) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        serverLogs.append("[").append(timestamp).append("] ").append(log).append("\n");
    }

    public void logClient(String log) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        clientLogs.append("[").append(timestamp).append("] ").append(log).append("\n");
    }

    public void fail(String reason) {
        this.passed = false;
        this.failureReason = reason;
    }

    public void addResult(TestResult result) {
        results.add(result);

        // Automatically create a TestStory from TestResult
        if (result.getStoryName() != null) {
            TestStory story = new TestStory(result.getStoryName());
            story.description = "Executed from " + result.getStoryName();

            // Only add a summary step if no steps were already recorded
            // Steps are already added via reporter.step() calls in TestOrchestrator
            if (story.getSteps().isEmpty()) {
                TestStep step = new TestStep("Test Execution");
                step.action = result.getStoryName();
                step.passed = result.isSuccess();
                // Add error info if test failed
                if (result.getError() != null) {
                    step.evidence.add("✗ Error: " + result.getError().getMessage());
                    step.actual = result.getError().getMessage();
                } else {
                    step.evidence.add(result.isSuccess() ? "✓ Test passed" : "✗ Test failed");
                    step.actual = result.isSuccess() ? "passed" : "failed";
                }
                if (result.getExecutionTimeMs() > 0) {
                    step.evidence.add("Duration: " + result.getExecutionTimeMs() + "ms");
                }
                story.addStep(step);
            }

            stories.add(story);
        }
    }

    public void complete() throws IOException {
        this.endTime = LocalDateTime.now();
        generateAllReports();
    }

    private void generateDetailedTextReport() throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("PILAF TEST REPORT\n");
        report.append("==================\n\n");

        report.append("Test: ").append(suiteName).append("\n");
        report.append("Status: ").append(passed ? "PASSED" : "FAILED").append("\n");
        report.append("Duration: ").append(Duration.between(startTime, endTime).toMillis()).append("ms\n\n");

        // Use steps from stories if available, otherwise use legacy steps list
        List<TestReporter.TestStep> allSteps = new ArrayList<>();
        for (TestStory story : stories) {
            allSteps.addAll(story.getSteps());
        }
        if (allSteps.isEmpty()) {
            allSteps = steps;
        }

        if (!allSteps.isEmpty()) {
            report.append("TEST STEPS\n");
            report.append("-----------\n\n");

            for (int i = 0; i < allSteps.size(); i++) {
                TestStep step = allSteps.get(i);
                report.append(String.format("Step %d: %s\n", i + 1, step.name));
                report.append("  Status: ").append(step.passed ? "PASSED" : "FAILED").append("\n");

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
                        report.append("    - ").append(evidence).append("\n");
                    }
                }
                report.append("\n");
            }
        }

        if (serverLogs.length() > 0) {
            report.append("SERVER LOGS\n");
            report.append("-----------\n");
            report.append(serverLogs.toString());
            report.append("\n");
        }

        if (clientLogs.length() > 0) {
            report.append("CLIENT LOGS\n");
            report.append("-----------\n");
            report.append(clientLogs.toString());
            report.append("\n");
        }

        String filename = suiteName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.txt";
        Files.writeString(Paths.get(outputDir, filename), report.toString());
    }

    private void generateDetailedJsonReport() throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"suiteName\": \"").append(suiteName).append("\",\n");
        json.append("  \"status\": \"").append(passed ? "PASSED" : "FAILED").append("\",\n");
        json.append("  \"durationMs\": ").append(Duration.between(startTime, endTime).toMillis()).append("\n");
        json.append("}\n");

        String filename = suiteName.replaceAll("[^a-zA-Z0-9]", "_") + "_report.json";
        Files.writeString(Paths.get(outputDir, filename), json.toString());
    }

    private void generateDetailedJUnitXml() throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite name=\"").append(suiteName).append("\" ");
        xml.append("tests=\"").append(steps.isEmpty() ? results.size() : steps.size()).append("\" ");
        xml.append("failures=\"").append(getFailedCount()).append("\" ");
        xml.append("time=\"").append(Duration.between(startTime, endTime).toMillis() / 1000.0).append("\">\n");
        xml.append("</testsuite>\n");

        String filename = "TEST-" + suiteName.replaceAll("[^a-zA-Z0-9]", "_") + ".xml";
        Files.writeString(Paths.get(outputDir, filename), xml.toString());
    }

    private void printSummary() {
        System.out.println();
        System.out.println("PILAF TEST SUMMARY");
        System.out.println("==================");
        System.out.println();
        System.out.println("Suite: " + suiteName);
        System.out.println("Status: " + (calculatePassed() ? "PASSED" : "FAILED"));
        System.out.println("Duration: " + Duration.between(startTime, endTime).toMillis() + "ms");
        System.out.println();
        System.out.println("Reports generated in: " + outputDir + "/");
        System.out.println("==================");
    }

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

    private String escapeXml(String s) {
        if (s == null) return "";
        return s;
    }

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

    public static class TestStep {
        public final String name;
        public String action;
        public String arguments;
        public String expected;
        public String actual;
        public boolean passed = true;
        public final List<String> evidence = new ArrayList<>();
        public String stateBefore;
        public String stateAfter;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String player;
        public String assertionType;

        public TestStep(String name) {
            this.name = name;
        }

        public TestStep action(String action) {
            this.action = action;
            return this;
        }

        public TestStep arguments(String arguments) {
            this.arguments = arguments;
            return this;
        }

        public TestStep player(String player) {
            this.player = player;
            return this;
        }

        public TestStep assertionType(String type) {
            this.assertionType = type;
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
    }

    enum LogType {
        RCON("rcon", "RCON"),
        PLAYER_CMD("player", "Player"),
        CLIENT("client", "Client"),
        SERVER("server", "Server");

        public final String cssClass;
        public final String icon;

        LogType(String cssClass, String icon) {
            this.cssClass = cssClass;
            this.icon = icon;
        }
    }

    class LogEntry {
        public final String timestamp;
        public final LogType type;
        public final String message;
        public final String username;

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
}
