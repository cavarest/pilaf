package org.cavarest.pilaf.testing.comparator;

import org.cavarest.pilaf.model.TestResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TestResultComparator compares test results across different backend combinations
 * to identify inconsistencies and ensure consistent behavior.
 */
public class TestResultComparator {

    private static final double EXECUTION_TIME_TOLERANCE = 0.10; // 10% tolerance for execution time differences

    /**
     * Compares test results from different backends to determine if they are consistent
     */
    public boolean areResultsConsistent(Map<String, TestResult> results) {
        if (results == null || results.size() < 2) {
            return true; // Cannot compare with less than 2 results
        }

        List<TestResult> resultList = new ArrayList<>(results.values());

        // Compare success/failure status
        boolean allPassed = resultList.stream().allMatch(TestResult::isSuccess);
        boolean allFailed = resultList.stream().noneMatch(TestResult::isSuccess);

        if (!allPassed && !allFailed) {
            return false; // Mixed success/failure results are inconsistent
        }

        // Compare assertion counts (with tolerance for timing-related variations)
        return areAssertionCountsConsistent(resultList);
    }

    /**
     * Finds specific inconsistencies between test results
     */
    public List<String> findInconsistencies(Map<String, TestResult> results) {
        List<String> inconsistencies = new ArrayList<>();

        if (results == null || results.size() < 2) {
            return inconsistencies;
        }

        List<TestResult> resultList = new ArrayList<>(results.values());

        // Check success/failure consistency
        boolean allPassed = resultList.stream().allMatch(TestResult::isSuccess);
        boolean allFailed = resultList.stream().noneMatch(TestResult::isSuccess);

        if (!allPassed && !allFailed) {
            long passed = resultList.stream().filter(TestResult::isSuccess).count();
            long failed = resultList.size() - passed;
            inconsistencies.add("Mixed success/failure results: " + passed + " passed, " + failed + " failed");
        }

        // Check assertion counts
        List<String> assertionInconsistencies = checkAssertionCountConsistency(resultList);
        inconsistencies.addAll(assertionInconsistencies);

        // Check execution time differences
        List<String> timingInconsistencies = checkExecutionTimeConsistency(resultList);
        inconsistencies.addAll(timingInconsistencies);

        // Check error patterns
        List<String> errorInconsistencies = checkErrorConsistency(resultList);
        inconsistencies.addAll(errorInconsistencies);

        return inconsistencies;
    }

    /**
     * Compares assertion counts across results with acceptable tolerance
     */
    private boolean areAssertionCountsConsistent(List<TestResult> results) {
        if (results.isEmpty()) return true;

        // Group results by assertion counts
        Map<String, List<TestResult>> groups = results.stream()
            .collect(Collectors.groupingBy(result ->
                result.getAssertionsPassed() + ":" + result.getAssertionsFailed()));

        // If all results have the same assertion counts, they're consistent
        return groups.size() == 1;
    }

    /**
     * Checks for assertion count inconsistencies
     */
    private List<String> checkAssertionCountConsistency(List<TestResult> results) {
        List<String> inconsistencies = new ArrayList<>();

        if (results.isEmpty()) return inconsistencies;

        Map<Integer, Long> passedCounts = results.stream()
            .collect(Collectors.groupingBy(TestResult::getAssertionsPassed, Collectors.counting()));

        Map<Integer, Long> failedCounts = results.stream()
            .collect(Collectors.groupingBy(TestResult::getAssertionsFailed, Collectors.counting()));

        if (passedCounts.size() > 1) {
            inconsistencies.add("Inconsistent assertion pass counts: " + passedCounts);
        }

        if (failedCounts.size() > 1) {
            inconsistencies.add("Inconsistent assertion fail counts: " + failedCounts);
        }

        return inconsistencies;
    }

    /**
     * Checks for execution time inconsistencies with tolerance
     */
    private List<String> checkExecutionTimeConsistency(List<TestResult> results) {
        List<String> inconsistencies = new ArrayList<>();

        if (results.size() < 2) return inconsistencies;

        List<Long> executionTimes = results.stream()
            .map(TestResult::getExecutionTimeMs)
            .filter(time -> time > 0)
            .sorted()
            .collect(Collectors.toList());

        if (executionTimes.isEmpty()) return inconsistencies;

        long minTime = executionTimes.get(0);
        long maxTime = executionTimes.get(executionTimes.size() - 1);

        // Check if the difference exceeds tolerance
        double relativeDifference = (double) (maxTime - minTime) / minTime;

        if (relativeDifference > EXECUTION_TIME_TOLERANCE) {
            inconsistencies.add(String.format(
                "Execution time differences exceed tolerance: min=%dms, max=%dms, diff=%.1f%%",
                minTime, maxTime, relativeDifference * 100));
        }

        return inconsistencies;
    }

    /**
     * Checks for error consistency patterns
     */
    private List<String> checkErrorConsistency(List<TestResult> results) {
        List<String> inconsistencies = new ArrayList<>();

        long resultsWithErrors = results.stream()
            .filter(result -> result.getError() != null)
            .count();

        long resultsWithoutErrors = results.size() - resultsWithErrors;

        // If some results have errors and others don't, it's inconsistent
        if (resultsWithErrors > 0 && resultsWithoutErrors > 0) {
            inconsistencies.add("Mixed error patterns: " + resultsWithErrors + " with errors, " +
                resultsWithoutErrors + " without errors");
        }

        // Check error message patterns for results with errors
        if (resultsWithErrors > 1) {
            List<String> errorMessages = results.stream()
                .filter(result -> result.getError() != null)
                .map(result -> result.getError().getMessage())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            Set<String> uniqueErrorMessages = new HashSet<>(errorMessages);

            if (uniqueErrorMessages.size() > 1) {
                inconsistencies.add("Different error messages across backends: " + uniqueErrorMessages);
            }
        }

        return inconsistencies;
    }

    /**
     * Gets detailed comparison report for a set of results
     */
    public String generateDetailedComparisonReport(Map<String, TestResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== Detailed Test Result Comparison ===\n\n");

        if (results == null || results.isEmpty()) {
            report.append("No results to compare.\n");
            return report.toString();
        }

        // Summary
        report.append("Summary:\n");
        report.append("- Total backends tested: ").append(results.size()).append("\n");

        long passed = results.values().stream().filter(TestResult::isSuccess).count();
        long failed = results.size() - passed;
        report.append("- Passed: ").append(passed).append("\n");
        report.append("- Failed: ").append(failed).append("\n");

        boolean consistent = areResultsConsistent(results);
        report.append("- Consistency: ").append(consistent ? "PASSED" : "FAILED").append("\n\n");

        // Detailed results
        report.append("Detailed Results by Backend:\n");
        for (Map.Entry<String, TestResult> entry : results.entrySet()) {
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
        List<String> inconsistencies = findInconsistencies(results);
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
     * Compares two specific test results
     */
    public boolean compareResults(TestResult result1, TestResult result2) {
        if (result1 == null || result2 == null) {
            return result1 == result2; // Both null is consistent, one null is not
        }

        // Compare success status
        if (result1.isSuccess() != result2.isSuccess()) {
            return false;
        }

        // Compare assertion counts
        if (result1.getAssertionsPassed() != result2.getAssertionsPassed()) {
            return false;
        }

        if (result1.getAssertionsFailed() != result2.getAssertionsFailed()) {
            return false;
        }

        // Compare execution times with tolerance
        long time1 = result1.getExecutionTimeMs();
        long time2 = result2.getExecutionTimeMs();

        if (time1 > 0 && time2 > 0) {
            long maxTime = Math.max(time1, time2);
            long minTime = Math.min(time1, time2);
            double relativeDiff = (double) (maxTime - minTime) / minTime;

            if (relativeDiff > EXECUTION_TIME_TOLERANCE) {
                return false;
            }
        }

        return true;
    }
}
