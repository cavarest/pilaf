package org.cavarest.pilaf.testing.comparison;

import org.cavarest.pilaf.model.TestResult;

import java.util.*;

/**
 * StoryComparison holds the comparison results for a single test story
 * across different backend combinations.
 */
public class StoryComparison {

    private final String storyFile;
    private boolean consistent;
    private List<String> inconsistencies;
    private Map<String, TestResult> backendResults;

    public StoryComparison(String storyFile) {
        this.storyFile = storyFile;
        this.consistent = true;
        this.inconsistencies = new ArrayList<>();
        this.backendResults = new HashMap<>();
    }

    /**
     * Sets whether the story comparison is consistent across backends
     * @param consistent true if consistent, false otherwise
     */
    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    /**
     * Sets the list of inconsistencies found
     * @param inconsistencies the list of inconsistency descriptions
     */
    public void setInconsistencies(List<String> inconsistencies) {
        this.inconsistencies = inconsistencies != null ? inconsistencies : new ArrayList<>();
    }

    /**
     * Adds a test result from a specific backend
     * @param backendName the name of the backend
     * @param result the test result
     */
    public void addBackendResult(String backendName, TestResult result) {
        backendResults.put(backendName, result);
    }

    /**
     * Gets the test result from a specific backend
     * @param backendName the name of the backend
     * @return the test result, or null if not found
     */
    public TestResult getBackendResult(String backendName) {
        return backendResults.get(backendName);
    }

    /**
     * Gets all backend results
     * @return unmodifiable map of backend names to results
     */
    public Map<String, TestResult> getBackendResults() {
        return Collections.unmodifiableMap(backendResults);
    }

    /**
     * Gets the story file name
     * @return the story file path
     */
    public String getStoryFile() {
        return storyFile;
    }

    /**
     * Checks if the story is consistent across all backends
     * @return true if consistent, false otherwise
     */
    public boolean isConsistent() {
        return consistent;
    }

    /**
     * Gets the list of inconsistencies
     * @return unmodifiable list of inconsistency descriptions
     */
    public List<String> getInconsistencies() {
        return Collections.unmodifiableList(inconsistencies);
    }

    /**
     * Gets summary statistics for this story comparison
     */
    public StoryComparisonSummary getSummary() {
        int totalBackends = backendResults.size();
        int passedBackends = 0;
        int failedBackends = 0;

        for (TestResult result : backendResults.values()) {
            if (result.isSuccess()) {
                passedBackends++;
            } else {
                failedBackends++;
            }
        }

        return new StoryComparisonSummary(storyFile, totalBackends, passedBackends,
                                        failedBackends, consistent, inconsistencies.size());
    }

    /**
     * Generates a detailed report for this story comparison
     */
    public String generateDetailedReport() {
        StringBuilder report = new StringBuilder();
        StoryComparisonSummary summary = getSummary();

        report.append(String.format("=== Story Comparison: %s ===\n", storyFile));
        report.append("Overall Status: ").append(consistent ? "CONSISTENT" : "INCONSISTENT").append("\n");
        report.append("Total Backends: ").append(summary.getTotalBackends()).append("\n");
        report.append("Passed: ").append(summary.getPassedBackends()).append("\n");
        report.append("Failed: ").append(summary.getFailedBackends()).append("\n");
        report.append("Inconsistencies: ").append(summary.getInconsistencyCount()).append("\n\n");

        // Detailed backend results
        report.append("Backend Results:\n");
        for (Map.Entry<String, TestResult> entry : backendResults.entrySet()) {
            String backend = entry.getKey();
            TestResult result = entry.getValue();

            report.append(String.format("  %s:\n", backend));
            report.append(String.format("    - Success: %s\n", result.isSuccess()));
            report.append(String.format("    - Assertions Passed: %d\n", result.getAssertionsPassed()));
            report.append(String.format("    - Assertions Failed: %d\n", result.getAssertionsFailed()));
            report.append(String.format("    - Execution Time: %dms\n", result.getExecutionTimeMs()));

            if (result.getError() != null) {
                report.append(String.format("    - Error: %s\n", result.getError().getMessage()));
            }

            report.append("\n");
        }

        // Inconsistencies
        if (!inconsistencies.isEmpty()) {
            report.append("Inconsistencies Found:\n");
            for (String inconsistency : inconsistencies) {
                report.append("  - ").append(inconsistency).append("\n");
            }
        } else {
            report.append("No inconsistencies detected.\n");
        }

        return report.toString();
    }

    /**
     * Gets performance comparison across backends
     */
    public PerformanceComparison getPerformanceComparison() {
        List<Long> executionTimes = backendResults.values().stream()
            .map(TestResult::getExecutionTimeMs)
            .filter(time -> time > 0)
            .sorted()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (executionTimes.isEmpty()) {
            return new PerformanceComparison(0, 0, 0, 0, 0.0);
        }

        long minTime = executionTimes.get(0);
        long maxTime = executionTimes.get(executionTimes.size() - 1);
        long avgTime = executionTimes.stream().mapToLong(Long::longValue).sum() / executionTimes.size();

        double relativeDifference = minTime > 0 ? (double) (maxTime - minTime) / minTime : 0.0;

        return new PerformanceComparison(minTime, maxTime, avgTime, executionTimes.size(), relativeDifference);
    }

    /**
     * Summary statistics for story comparison
     */
    public static class StoryComparisonSummary {
        private final String storyFile;
        private final int totalBackends;
        private final int passedBackends;
        private final int failedBackends;
        private final boolean consistent;
        private final int inconsistencyCount;

        public StoryComparisonSummary(String storyFile, int totalBackends, int passedBackends,
                                    int failedBackends, boolean consistent, int inconsistencyCount) {
            this.storyFile = storyFile;
            this.totalBackends = totalBackends;
            this.passedBackends = passedBackends;
            this.failedBackends = failedBackends;
            this.consistent = consistent;
            this.inconsistencyCount = inconsistencyCount;
        }

        public String getStoryFile() { return storyFile; }
        public int getTotalBackends() { return totalBackends; }
        public int getPassedBackends() { return passedBackends; }
        public int getFailedBackends() { return failedBackends; }
        public boolean isConsistent() { return consistent; }
        public int getInconsistencyCount() { return inconsistencyCount; }

        public double getPassRate() {
            if (totalBackends == 0) return 0.0;
            return (double) passedBackends / totalBackends * 100.0;
        }
    }

    /**
     * Performance comparison across backends
     */
    public static class PerformanceComparison {
        private final long minExecutionTime;
        private final long maxExecutionTime;
        private final long averageExecutionTime;
        private final int backendCount;
        private final double relativeDifference;

        public PerformanceComparison(long minExecutionTime, long maxExecutionTime,
                                   long averageExecutionTime, int backendCount, double relativeDifference) {
            this.minExecutionTime = minExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
            this.averageExecutionTime = averageExecutionTime;
            this.backendCount = backendCount;
            this.relativeDifference = relativeDifference;
        }

        public long getMinExecutionTime() { return minExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public long getAverageExecutionTime() { return averageExecutionTime; }
        public int getBackendCount() { return backendCount; }
        public double getRelativeDifference() { return relativeDifference; }

        public boolean hasSignificantPerformanceDifference() {
            return relativeDifference > 0.10; // 10% threshold
        }

        @Override
        public String toString() {
            return String.format("Performance: min=%dms, max=%dms, avg=%dms, diff=%.1f%%",
                minExecutionTime, maxExecutionTime, averageExecutionTime, relativeDifference * 100);
        }
    }
}
