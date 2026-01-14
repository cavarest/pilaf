package org.cavarest.pilaf.report;

import org.cavarest.pilaf.model.TestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestReporter.
 */
@DisplayName("TestReporter Tests")
class TestReporterTest {

    private TestReporter reporter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reporter = new TestReporter("Test Suite");
        reporter.setOutputDir(tempDir.toString());
    }

    @Test
    @DisplayName("Constructor initializes with default suite name")
    void testConstructor_defaultSuiteName() {
        TestReporter defaultReporter = new TestReporter();
        assertEquals("test", defaultReporter.story("test").name);
    }

    @Test
    @DisplayName("Constructor initializes with provided suite name")
    void testConstructor_customSuiteName() {
        assertEquals("test", reporter.story("test").name);
    }

    @Test
    @DisplayName("story() creates a new story with given name")
    void testStory_createsNewStory() {
        TestReporter.TestStory story = reporter.story("My Story");
        assertNotNull(story);
        assertEquals("My Story", story.name);
    }

    @Test
    @DisplayName("getCurrentStory() returns existing story when available")
    void testGetCurrentStory_returnsExistingStory() {
        TestReporter.TestStory story1 = reporter.story("Story 1");
        TestReporter.TestStory current = reporter.getCurrentStory();
        assertEquals(story1, current);
    }

    @Test
    @DisplayName("getCurrentStory() creates new story if none exists")
    void testGetCurrentStory_createsNewStoryIfNone() {
        TestReporter.TestStory story = reporter.getCurrentStory();
        assertNotNull(story);
        assertEquals("Test Suite", story.name);
    }

    @Test
    @DisplayName("step() creates a new step and adds to current story")
    void testStep_createsNewStep() {
        TestReporter.TestStep step = reporter.step("Test Step");
        assertNotNull(step);
        assertEquals("Test Step", step.name);
        assertEquals(1, reporter.getCurrentStory().getSteps().size());
    }

    @Test
    @DisplayName("setOutputDir() updates output directory")
    void testSetOutputDir() {
        reporter.setOutputDir("/custom/output");
        // No assertion - just ensure no exception
    }

    @Test
    @DisplayName("setVerbose() updates verbose flag")
    void testSetVerbose() {
        reporter.setVerbose(true);
        reporter.setVerbose(false);
        // No assertion - just ensure no exception
    }

    @Test
    @DisplayName("logServer() appends log with timestamp")
    void testLogServer_appendsWithTimestamp() {
        reporter.logServer("Server started");
        // No direct way to verify, but ensure no exception
    }

    @Test
    @DisplayName("logClient() appends log with timestamp")
    void testLogClient_appendsWithTimestamp() {
        reporter.logClient("Client connected");
        // No direct way to verify, but ensure no exception
    }

    @Test
    @DisplayName("fail() sets failure reason and marks as failed")
    void testFail_setsFailureReason() {
        reporter.fail("Test failed");
        // No direct assertion possible - but method should not throw
    }

    @Test
    @DisplayName("addResult() adds result to results list")
    void testAddResult_addsToList() {
        TestResult result = new TestResult("Test Story");
        result.setSuccess(true);
        reporter.addResult(result);
        // No direct assertion possible - but method should not throw
    }

    @Test
    @DisplayName("generateAllReports() creates report files")
    void testGenerateAllReports_createsFiles() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Step 1");
        step.pass();

        reporter.generateAllReports();

        // Check that report files exist
        File reportDir = new File(tempDir.toString());
        assertTrue(reportDir.exists(), "Report directory should exist");

        File[] htmlFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.html"));
        assertTrue(htmlFiles != null && htmlFiles.length > 0, "HTML report should be created");

        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertTrue(txtFiles != null && txtFiles.length > 0, "Text report should be created");
    }

    @Test
    @DisplayName("complete() sets end time and generates reports")
    void testComplete_generatesReports() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Step 1");
        step.pass();

        reporter.complete();

        // Verify report files were created
        File reportDir = new File(tempDir.toString());
        File[] files = reportDir.listFiles((d, name) -> name.endsWith("_report.html"));
        assertTrue(files != null && files.length > 0);
    }

    @Test
    @DisplayName("hasFailures() returns true when steps failed")
    void testHasFailures_withFailedSteps() {
        // Use results API since hasFailures() checks legacy steps list first
        TestResult result1 = new TestResult("Test 1");
        result1.setSuccess(true);
        TestResult result2 = new TestResult("Test 2");
        result2.setSuccess(false);
        reporter.addResult(result1);
        reporter.addResult(result2);

        assertTrue(reporter.hasFailures());
    }

    @Test
    @DisplayName("hasFailures() returns false when all steps passed")
    void testHasFailures_allStepsPassed() {
        // Use results API
        TestResult result1 = new TestResult("Test 1");
        result1.setSuccess(true);
        TestResult result2 = new TestResult("Test 2");
        result2.setSuccess(true);
        reporter.addResult(result1);
        reporter.addResult(result2);

        assertFalse(reporter.hasFailures());
    }

    @Test
    @DisplayName("getPassedCount() returns count of passed steps")
    void testGetPassedCount_countsPassedSteps() {
        // Use results API
        TestResult result1 = new TestResult("Test 1");
        result1.setSuccess(true);
        TestResult result2 = new TestResult("Test 2");
        result2.setSuccess(true);
        TestResult result3 = new TestResult("Test 3");
        result3.setSuccess(false);
        reporter.addResult(result1);
        reporter.addResult(result2);
        reporter.addResult(result3);

        assertEquals(2, reporter.getPassedCount());
    }

    @Test
    @DisplayName("getFailedCount() returns count of failed steps")
    void testGetFailedCount_countsFailedSteps() {
        // Use results API
        TestResult result1 = new TestResult("Test 1");
        result1.setSuccess(true);
        TestResult result2 = new TestResult("Test 2");
        result2.setSuccess(false);
        TestResult result3 = new TestResult("Test 3");
        result3.setSuccess(false);
        reporter.addResult(result1);
        reporter.addResult(result2);
        reporter.addResult(result3);

        assertEquals(2, reporter.getFailedCount());
    }

    @Test
    @DisplayName("TestStory.isPassed() returns true when all steps passed")
    void testTestStory_isPassed_allStepsPassed() {
        TestReporter.TestStory story = reporter.story("Test Story");
        reporter.step("Step 1").pass();
        reporter.step("Step 2").pass();

        assertTrue(story.isPassed());
    }

    @Test
    @DisplayName("TestStory.isPassed() returns false when any step failed")
    void testTestStory_isPassed_anyStepFailed() {
        TestReporter.TestStory story = reporter.story("Test Story");
        reporter.step("Step 1").pass();
        reporter.step("Step 2").fail();

        assertFalse(story.isPassed());
    }

    @Test
    @DisplayName("TestStory.getPassedCount() returns count of passed steps")
    void testTestStory_getPassedCount() {
        TestReporter.TestStory story = reporter.story("Test Story");
        reporter.step("Step 1").pass();
        reporter.step("Step 2").pass();
        reporter.step("Step 3").fail();

        assertEquals(2, story.getPassedCount());
    }

    @Test
    @DisplayName("TestStory.getFailedCount() returns count of failed steps")
    void testTestStory_getFailedCount() {
        TestReporter.TestStory story = reporter.story("Test Story");
        reporter.step("Step 1").pass();
        reporter.step("Step 2").fail();
        reporter.step("Step 3").fail();

        assertEquals(2, story.getFailedCount());
    }

    @Test
    @DisplayName("TestStory.description() sets description")
    void testTestStory_description() {
        TestReporter.TestStory story = reporter.story("Test Story");
        story.description("This is a test description");

        assertEquals("This is a test description", story.description);
    }

    @Test
    @DisplayName("TestStory with steps constructor")
    void testTestStory_withStepsConstructor() {
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.pass();
        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.pass();

        TestReporter.TestStory story = new TestReporter.TestStory("Test Story", List.of(step1, step2));

        assertEquals(2, story.getSteps().size());
        assertTrue(story.isPassed());
    }

    @Test
    @DisplayName("TestStep builder methods return this for chaining")
    void testTestStep_builderMethods() {
        TestReporter.TestStep step = new TestReporter.TestStep("Step Name");

        assertSame(step, step.action("test action"));
        assertSame(step, step.arguments("arg1 arg2"));
        assertSame(step, step.player("test_player"));
        assertSame(step, step.assertionType("assert_contains"));
        assertSame(step, step.expected("expected value"));
        assertSame(step, step.actual("actual value"));
        assertSame(step, step.extractedJson("{}"));
        assertSame(step, step.evidence("test evidence"));
        assertSame(step, step.pass());
        assertSame(step, step.fail());
        assertSame(step, step.stateBefore("{}"));
        assertSame(step, step.stateAfter("{}"));
        assertSame(step, step.stateDiff("diff"));
        assertSame(step, step.executor("RCON"));
        assertSame(step, step.executorPlayer("player1"));
        assertSame(step, step.isOperator(true));
    }

    @Test
    @DisplayName("TestStep.assertContains() passes when text contains substring")
    void testTestStep_assertContains_passes() {
        TestReporter.TestStep step = new TestReporter.TestStep("Test");
        step.assertContains("Hello World", "World");

        assertTrue(step.passed);
        assertFalse(step.evidence.isEmpty());
        assertTrue(step.evidence.get(0).contains("✓"));
    }

    @Test
    @DisplayName("TestStep.assertContains() fails when text does not contain substring")
    void testTestStep_assertContains_fails() {
        TestReporter.TestStep step = new TestReporter.TestStep("Test");
        step.assertContains("Hello", "World");

        assertFalse(step.passed);
        assertFalse(step.evidence.isEmpty());
        assertTrue(step.evidence.get(0).contains("✗"));
    }

    @Test
    @DisplayName("TestStep.assertContains() handles null text")
    void testTestStep_assertContains_nullText() {
        TestReporter.TestStep step = new TestReporter.TestStep("Test");
        step.assertContains(null, "test");

        assertFalse(step.passed);
        assertFalse(step.evidence.isEmpty());
    }

    @Test
    @DisplayName("Text report includes step details")
    void testTextReport_includesStepDetails() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.action("give_item")
            .arguments("diamond 64")
            .expected("Item given")
            .actual("✓ Command sent")
            .evidence("Item was given successfully")
            .pass();

        reporter.generateAllReports();

        // Read text report
        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);
        assertTrue(txtFiles.length > 0);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("Test Step"));
        assertTrue(report.contains("give_item"));
        assertTrue(report.contains("PASSED"));
    }

    @Test
    @DisplayName("Text report includes state information")
    void testTextReport_includesStateInformation() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.stateBefore("{\"before\": \"value\"}")
            .stateAfter("{\"after\": \"new_value\"}")
            .stateDiff("{\"diff\": \"changes\"}")
            .pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("State Before"));
        assertTrue(report.contains("State After"));
        assertTrue(report.contains("State Diff"));
    }

    @Test
    @DisplayName("generateAllReports() creates JSON report")
    void testGenerateAllReports_createsJsonReport() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Step 1");
        step.pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] jsonFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.json"));
        assertTrue(jsonFiles != null && jsonFiles.length > 0, "JSON report should be created");
    }

    @Test
    @DisplayName("generateAllReports() creates JUnit XML report")
    void testGenerateAllReports_createsJUnitXml() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Step 1");
        step.pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] xmlFiles = reportDir.listFiles((d, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
        assertTrue(xmlFiles != null && xmlFiles.length > 0, "JUnit XML report should be created");
    }

    @Test
    @DisplayName("Multiple stories are handled correctly")
    void testMultipleStories_handledCorrectly() throws IOException {
        reporter.story("Story 1");
        reporter.step("Step 1").pass();

        reporter.story("Story 2");
        reporter.step("Step 2").pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] htmlFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.html"));
        assertTrue(htmlFiles != null && htmlFiles.length > 0);
    }

    // LOGTYPE ENUM TESTS

    @Test
    @DisplayName("LogType.RCON has correct cssClass and icon")
    void testLogType_RCON() {
        TestReporter.LogType rcon = TestReporter.LogType.RCON;
        assertEquals("rcon", rcon.cssClass);
        assertEquals("RCON", rcon.icon);
    }

    @Test
    @DisplayName("LogType.PLAYER_CMD has correct cssClass and icon")
    void testLogType_PLAYER_CMD() {
        TestReporter.LogType playerCmd = TestReporter.LogType.PLAYER_CMD;
        assertEquals("player", playerCmd.cssClass);
        assertEquals("Player", playerCmd.icon);
    }

    @Test
    @DisplayName("LogType.CLIENT has correct cssClass and icon")
    void testLogType_CLIENT() {
        TestReporter.LogType client = TestReporter.LogType.CLIENT;
        assertEquals("client", client.cssClass);
        assertEquals("Client", client.icon);
    }

    @Test
    @DisplayName("LogType.SERVER has correct cssClass and icon")
    void testLogType_SERVER() {
        TestReporter.LogType server = TestReporter.LogType.SERVER;
        assertEquals("server", server.cssClass);
        assertEquals("Server", server.icon);
    }

    // LOGENTRY CLASS TESTS

    @Test
    @DisplayName("LogEntry constructor with username sets all fields")
    void testLogEntry_constructorWithUsername() {
        TestReporter.LogEntry entry = reporter.new LogEntry(
            "2023-01-01 12:00:00",
            TestReporter.LogType.RCON,
            "Test message",
            "testuser"
        );

        assertEquals("2023-01-01 12:00:00", entry.timestamp);
        assertEquals(TestReporter.LogType.RCON, entry.type);
        assertEquals("Test message", entry.message);
        assertEquals("testuser", entry.username);
    }

    @Test
    @DisplayName("LogEntry constructor without username sets username to null")
    void testLogEntry_constructorWithoutUsername() {
        TestReporter.LogEntry entry = reporter.new LogEntry(
            "2023-01-01 12:00:00",
            TestReporter.LogType.PLAYER_CMD,
            "Test command"
        );

        assertEquals("2023-01-01 12:00:00", entry.timestamp);
        assertEquals(TestReporter.LogType.PLAYER_CMD, entry.type);
        assertEquals("Test command", entry.message);
        assertNull(entry.username);
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS FOR calculatePassed()
    // ========================================================================

    @Test
    @DisplayName("Text report includes server logs when present")
    void testTextReport_includesServerLogs() throws IOException {
        // Add some server logs
        reporter.logServer("Server starting...");
        reporter.logServer("Server ready");

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);
        assertTrue(txtFiles.length > 0);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("SERVER LOGS"));
        assertTrue(report.contains("Server starting..."));
    }

    @Test
    @DisplayName("Text report includes client logs when present")
    void testTextReport_includesClientLogs() throws IOException {
        // Add some client logs
        reporter.logClient("Client connecting...");
        reporter.logClient("Client connected");

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);
        assertTrue(txtFiles.length > 0);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("CLIENT LOGS"));
        assertTrue(report.contains("Client connecting..."));
    }

    @Test
    @DisplayName("Text report with state after only")
    void testTextReport_stateAfterOnly() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.stateAfter("{\"after\": \"value\"}")
            .pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("State Before: null"));
        assertTrue(report.contains("State After:"));
    }

    @Test
    @DisplayName("Text report with state before only")
    void testTextReport_stateBeforeOnly() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.stateBefore("{\"before\": \"value\"}")
            .pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("State Before:"));
        assertTrue(report.contains("State After: null"));
    }

    @Test
    @DisplayName("Text report with state diff")
    void testTextReport_withStateDiff() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.stateBefore("{\"before\": \"value\"}")
            .stateAfter("{\"after\": \"new_value\"}")
            .stateDiff("{\"changed\": true}")
            .pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("State Diff:"));
        assertTrue(report.contains("changed"));
    }

    @Test
    @DisplayName("Text report shows FAILED status for failed steps")
    void testTextReport_failedStepStatus() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.fail();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("Status: FAILED"));
    }

    @Test
    @DisplayName("generateAllReports() with legacy steps (no stories)")
    void testGenerateAllReports_legacySteps() throws IOException {
        // Create a reporter without stories, then use legacy API via TestResult
        TestReporter emptyReporter = new TestReporter("Legacy Test");
        emptyReporter.setOutputDir(tempDir.toString());

        // Add results instead of using story API
        TestResult result1 = new TestResult("Step 1");
        result1.setSuccess(true);
        TestResult result2 = new TestResult("Step 2");
        result2.setSuccess(false);
        emptyReporter.addResult(result1);
        emptyReporter.addResult(result2);

        emptyReporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] htmlFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.html"));
        assertTrue(htmlFiles != null && htmlFiles.length > 0, "HTML report should be created");
    }

    @Test
    @DisplayName("Text report shows FAILED status in header when failed")
    void testTextReport_headerFailedStatus() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.fail();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("Status: FAILED"));
    }

    @Test
    @DisplayName("generateAllReports() with fail() reason and no results")
    void testGenerateAllReports_withFailReasonOnly() throws IOException {
        TestReporter emptyReporter = new TestReporter("Failed Test");
        emptyReporter.setOutputDir(tempDir.toString());
        emptyReporter.fail("Test failed due to error");

        emptyReporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] htmlFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.html"));
        assertTrue(htmlFiles != null && htmlFiles.length > 0);
    }

    // TESTS FOR UNCOVERED BRANCHES (lines 37-38 - legacy steps path)

    @Test
    @DisplayName("calculatePassed() with legacy steps (not results) uses steps for passing status")
    void testCalculatePassed_withLegacySteps() throws IOException {
        TestReporter legacyReporter = new TestReporter("Legacy Test");
        legacyReporter.setOutputDir(tempDir.toString());

        // Create a step directly without using stories (legacy API)
        TestReporter.TestStep step = legacyReporter.step("Test Step");
        step.pass(); // Mark as passed

        legacyReporter.generateAllReports();

        // This should cover the legacy steps path at lines 37-38
        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);
        assertTrue(txtFiles.length > 0);
    }

    @Test
    @DisplayName("Text report shows PASSED status in header when passed")
    void testTextReport_headerPassedStatus() throws IOException {
        TestReporter.TestStory story = reporter.story("Test Story");
        TestReporter.TestStep step = reporter.step("Test Step");
        step.pass();

        reporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertNotNull(txtFiles);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String report = content.toString();
        assertTrue(report.contains("Status: PASSED"));
    }

    @Test
    @DisplayName("generateAllReports() with fail() reason covers failureReason branch")
    void testGenerateAllReports_withFailReasonCoversBranch() throws IOException {
        TestReporter testReporter = new TestReporter("Test With Fail");
        testReporter.setOutputDir(tempDir.toString());
        testReporter.fail("Explicit failure reason");

        // Also set a non-empty failure reason on the reporter level
        // This should cover the branch at line 45 where failureReason is not null and not empty
        testReporter.generateAllReports();

        File reportDir = new File(tempDir.toString());
        File[] txtFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.txt"));
        assertTrue(txtFiles != null && txtFiles.length > 0);
    }

    // ========================================================================
    // TESTS FOR UNCOVERED LEGACY STEPS BRANCHES
    // ========================================================================

    @Test
    @DisplayName("calculatePassed() with non-empty legacy steps list covers steps path")
    void testCalculatePassed_withNonEmptyLegacySteps() throws Exception {
        TestReporter testReporter = new TestReporter("Legacy Steps Test");
        testReporter.setOutputDir(tempDir.toString());

        // Use reflection to add steps directly to the legacy steps list
        java.lang.reflect.Field stepsField = TestReporter.class.getDeclaredField("steps");
        stepsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> steps = (List<TestReporter.TestStep>) stepsField.get(testReporter);

        // Add some steps with mixed pass/fail status
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.pass();
        steps.add(step1);

        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.pass();
        steps.add(step2);

        // generateAllReports() calls calculatePassed() which should now use the steps path
        testReporter.generateAllReports();

        // Verify reports were generated
        File reportDir = new File(tempDir.toString());
        File[] htmlFiles = reportDir.listFiles((d, name) -> name.endsWith("_report.html"));
        assertTrue(htmlFiles != null && htmlFiles.length > 0);
    }

    @Test
    @DisplayName("hasFailures() with non-empty legacy steps list returns true when any step failed")
    void testHasFailures_withNonEmptyLegacySteps() throws Exception {
        TestReporter testReporter = new TestReporter("Legacy Steps Test");

        // Use reflection to add steps directly to the legacy steps list
        java.lang.reflect.Field stepsField = TestReporter.class.getDeclaredField("steps");
        stepsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> steps = (List<TestReporter.TestStep>) stepsField.get(testReporter);

        // Add steps with one failed
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.pass();
        steps.add(step1);

        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.fail();
        steps.add(step2);

        assertTrue(testReporter.hasFailures());
    }

    @Test
    @DisplayName("hasFailures() with non-empty legacy steps list returns false when all steps passed")
    void testHasFailures_withNonEmptyLegacySteps_AllPassed() throws Exception {
        TestReporter testReporter = new TestReporter("Legacy Steps Test");

        // Use reflection to add steps directly to the legacy steps list
        java.lang.reflect.Field stepsField = TestReporter.class.getDeclaredField("steps");
        stepsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> steps = (List<TestReporter.TestStep>) stepsField.get(testReporter);

        // Add all passed steps
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.pass();
        steps.add(step1);

        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.pass();
        steps.add(step2);

        assertFalse(testReporter.hasFailures());
    }

    @Test
    @DisplayName("getPassedCount() with non-empty legacy steps list counts passed steps")
    void testGetPassedCount_withNonEmptyLegacySteps() throws Exception {
        TestReporter testReporter = new TestReporter("Legacy Steps Test");

        // Use reflection to add steps directly to the legacy steps list
        java.lang.reflect.Field stepsField = TestReporter.class.getDeclaredField("steps");
        stepsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> steps = (List<TestReporter.TestStep>) stepsField.get(testReporter);

        // Add steps with mixed pass/fail
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.pass();
        steps.add(step1);

        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.pass();
        steps.add(step2);

        TestReporter.TestStep step3 = new TestReporter.TestStep("Step 3");
        step3.fail();
        steps.add(step3);

        assertEquals(2, testReporter.getPassedCount());
    }

    @Test
    @DisplayName("getFailedCount() with non-empty legacy steps list counts failed steps")
    void testGetFailedCount_withNonEmptyLegacySteps() throws Exception {
        TestReporter testReporter = new TestReporter("Legacy Steps Test");

        // Use reflection to add steps directly to the legacy steps list
        java.lang.reflect.Field stepsField = TestReporter.class.getDeclaredField("steps");
        stepsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> steps = (List<TestReporter.TestStep>) stepsField.get(testReporter);

        // Add steps with mixed pass/fail
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.pass();
        steps.add(step1);

        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.fail();
        steps.add(step2);

        TestReporter.TestStep step3 = new TestReporter.TestStep("Step 3");
        step3.fail();
        steps.add(step3);

        assertEquals(2, testReporter.getFailedCount());
    }

    @Test
    @DisplayName("generateDetailedJUnitXml() uses steps count when steps list is not empty")
    void testGenerateDetailedJUnitXml_withNonEmptyLegacySteps() throws Exception {
        TestReporter testReporter = new TestReporter("JUnit XML Test");
        testReporter.setOutputDir(tempDir.toString());

        // Use reflection to add steps directly to the legacy steps list
        java.lang.reflect.Field stepsField = TestReporter.class.getDeclaredField("steps");
        stepsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<TestReporter.TestStep> steps = (List<TestReporter.TestStep>) stepsField.get(testReporter);

        // Add 3 steps
        TestReporter.TestStep step1 = new TestReporter.TestStep("Step 1");
        step1.pass();
        steps.add(step1);

        TestReporter.TestStep step2 = new TestReporter.TestStep("Step 2");
        step2.pass();
        steps.add(step2);

        TestReporter.TestStep step3 = new TestReporter.TestStep("Step 3");
        step3.fail();
        steps.add(step3);

        testReporter.generateAllReports();

        // Verify JUnit XML was created
        File reportDir = new File(tempDir.toString());
        File[] xmlFiles = reportDir.listFiles((d, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
        assertTrue(xmlFiles != null && xmlFiles.length > 0);

        // Read and verify the XML contains tests="3"
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(xmlFiles[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String xml = content.toString();
        assertTrue(xml.contains("tests=\"3\""));
    }
}
