package org.cavarest.pilaf.testing.comparison;

import java.util.*;

/**
 * ConsistencyComparison holds the comparison results across all backend combinations
 * for all test stories.
 */
public class ConsistencyComparison {

    private final Map<String, StoryComparison> storyComparisons;
    private boolean overallConsistent;
    private List<String> overallInconsistencies;

    public ConsistencyComparison() {
        this.storyComparisons = new HashMap<>();
        this.overallConsistent = true;
        this.overallInconsistencies = new ArrayList<>();
    }

    /**
     * Adds a story comparison result
     */
    public void addStoryComparison(String storyFile, StoryComparison comparison) {
        storyComparisons.put(storyFile, comparison);

        // Update overall consistency
        if (!comparison.isConsistent()) {
            overallConsistent = false;
            overallInconsistencies.add("Story '" + storyFile + "' failed consistency check");
            overallInconsistencies.addAll(comparison.getInconsistencies());
        }
    }

    /**
     * Gets the comparison for a specific story
     */
    public StoryComparison getStoryComparison(String storyFile) {
        return storyComparisons.get(storyFile);
    }

    /**
     * Gets all story comparisons
     */
    public Map<String, StoryComparison> getStoryComparisons() {
        return Collections.unmodifiableMap(storyComparisons);
    }

    /**
     * Checks if overall consistency is maintained
     */
    public boolean isOverallConsistent() {
        return overallConsistent;
    }

    /**
     * Sets overall consistency status
     */
    public void setOverallConsistent(boolean overallConsistent) {
        this.overallConsistent = overallConsistent;
    }

    /**
     * Gets overall inconsistencies
     */
    public List<String> getOverallInconsistencies() {
        return Collections.unmodifiableList(overallInconsistencies);
    }

    /**
     * Sets overall inconsistencies
     */
    public void setOverallInconsistencies(List<String> overallInconsistencies) {
        this.overallInconsistencies = overallInconsistencies;
    }

    /**
     * Gets summary statistics
     */
    public ConsistencySummary getSummary() {
        int totalStories = storyComparisons.size();
        int consistentStories = 0;
        int inconsistentStories = 0;

        for (StoryComparison comparison : storyComparisons.values()) {
            if (comparison.isConsistent()) {
                consistentStories++;
            } else {
                inconsistentStories++;
            }
        }

        return new ConsistencySummary(totalStories, consistentStories, inconsistentStories,
                                    overallConsistent, overallInconsistencies.size());
    }

    /**
     * Generates a detailed comparison report
     */
    public String generateDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Backend Consistency Comparison Report ===\n\n");

        ConsistencySummary summary = getSummary();
        report.append("Summary:\n");
        report.append("- Total Test Stories: ").append(summary.getTotalStories()).append("\n");
        report.append("- Consistent Stories: ").append(summary.getConsistentStories()).append("\n");
        report.append("- Inconsistent Stories: ").append(summary.getInconsistentStories()).append("\n");
        report.append("- Overall Consistency: ").append(summary.isOverallConsistent() ? "PASSED" : "FAILED").append("\n");
        report.append("- Total Inconsistencies: ").append(summary.getTotalInconsistencies()).append("\n\n");

        // Detailed story comparisons
        report.append("Detailed Story Comparisons:\n");
        for (Map.Entry<String, StoryComparison> entry : storyComparisons.entrySet()) {
            String storyFile = entry.getKey();
            StoryComparison comparison = entry.getValue();

            report.append(String.format("\n--- Story: %s ---\n", storyFile));
            report.append("Status: ").append(comparison.isConsistent() ? "CONSISTENT" : "INCONSISTENT").append("\n");

            if (comparison.isConsistent()) {
                report.append("All backend combinations produced identical results.\n");
            } else {
                report.append("Inconsistencies found:\n");
                for (String inconsistency : comparison.getInconsistencies()) {
                    report.append("  - ").append(inconsistency).append("\n");
                }
            }
        }

        // Overall inconsistencies
        if (!overallInconsistencies.isEmpty()) {
            report.append("\n=== Overall System Inconsistencies ===\n");
            for (String inconsistency : overallInconsistencies) {
                report.append("- ").append(inconsistency).append("\n");
            }
        }

        return report.toString();
    }

    /**
     * Summary statistics for consistency comparison
     */
    public static class ConsistencySummary {
        private final int totalStories;
        private final int consistentStories;
        private final int inconsistentStories;
        private final boolean overallConsistent;
        private final int totalInconsistencies;

        public ConsistencySummary(int totalStories, int consistentStories, int inconsistentStories,
                                boolean overallConsistent, int totalInconsistencies) {
            this.totalStories = totalStories;
            this.consistentStories = consistentStories;
            this.inconsistentStories = inconsistentStories;
            this.overallConsistent = overallConsistent;
            this.totalInconsistencies = totalInconsistencies;
        }

        public int getTotalStories() { return totalStories; }
        public int getConsistentStories() { return consistentStories; }
        public int getInconsistentStories() { return inconsistentStories; }
        public boolean isOverallConsistent() { return overallConsistent; }
        public int getTotalInconsistencies() { return totalInconsistencies; }

        public double getConsistencyPercentage() {
            if (totalStories == 0) return 100.0;
            return (double) consistentStories / totalStories * 100.0;
        }
    }
}
