package org.cavarest.pilaf.model;

import java.util.ArrayList;
import java.util.List;

public class TestResult {
    private boolean success;
    private String storyName;
    private long executionTimeMs;
    private int actionsExecuted, assertionsPassed, assertionsFailed;
    private List<String> logs = new ArrayList<>();
    private List<AssertionResult> assertionResults = new ArrayList<>();
    private Exception error;

    public TestResult() {}
    public TestResult(String storyName) { this.storyName = storyName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getStoryName() { return storyName; }
    public void setStoryName(String storyName) { this.storyName = storyName; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public int getActionsExecuted() { return actionsExecuted; }
    public int getAssertionsPassed() { return assertionsPassed; }
    public int getAssertionsFailed() { return assertionsFailed; }
    public List<String> getLogs() { return logs; }
    public Exception getError() { return error; }
    public void setError(Exception error) { this.error = error; }

    public void addLog(String message) { logs.add(message); }
    public void incrementActionsExecuted() { actionsExecuted++; }
    public void addAssertionResult(AssertionResult result) {
        assertionResults.add(result);
        if (result.isPassed()) assertionsPassed++; else assertionsFailed++;
    }

    public static class AssertionResult {
        private Assertion assertion;
        private boolean passed;
        private String message;

        public AssertionResult(Assertion assertion, boolean passed) { this.assertion = assertion; this.passed = passed; }
        public boolean isPassed() { return passed; }
        public void setMessage(String message) { this.message = message; }
    }
}
