package org.cavarest.pilaf.testing.comparison;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConsistencyComparison.
 */
@DisplayName("ConsistencyComparison Tests")
class ConsistencyComparisonTest {

    private ConsistencyComparison comparison;

    @BeforeEach
    void setUp() {
        comparison = new ConsistencyComparison();
    }

    @Test
    @DisplayName("Constructor initializes with default values")
    void testConstructor_defaultValues() {
        assertTrue(comparison.isOverallConsistent());
        assertNotNull(comparison.getStoryComparisons());
        assertTrue(comparison.getStoryComparisons().isEmpty());
        assertNotNull(comparison.getOverallInconsistencies());
        assertTrue(comparison.getOverallInconsistencies().isEmpty());
    }

    @Test
    @DisplayName("addStoryComparison() adds consistent story")
    void testAddStoryComparison_consistentStory() {
        StoryComparison storyComparison = new StoryComparison("test-story.yaml");
        storyComparison.setConsistent(true);

        comparison.addStoryComparison("test-story.yaml", storyComparison);

        assertEquals(1, comparison.getStoryComparisons().size());
        assertTrue(comparison.isOverallConsistent());
    }

    @Test
    @DisplayName("addStoryComparison() adds inconsistent story and updates overall")
    void testAddStoryComparison_inconsistentStory() {
        StoryComparison storyComparison = new StoryComparison("test-story.yaml");
        storyComparison.setConsistent(false);
        storyComparison.setInconsistencies(new java.util.ArrayList<>(List.of("Test failed")));

        comparison.addStoryComparison("test-story.yaml", storyComparison);

        assertEquals(1, comparison.getStoryComparisons().size());
        assertFalse(comparison.isOverallConsistent());
        assertFalse(comparison.getOverallInconsistencies().isEmpty());
    }

    @Test
    @DisplayName("getStoryComparison() returns correct comparison")
    void testGetStoryComparison() {
        StoryComparison storyComparison = new StoryComparison("test-story.yaml");
        comparison.addStoryComparison("test-story.yaml", storyComparison);

        StoryComparison retrieved = comparison.getStoryComparison("test-story.yaml");

        assertNotNull(retrieved);
        assertEquals(storyComparison, retrieved);
    }

    @Test
    @DisplayName("getStoryComparison() returns null for non-existent story")
    void testGetStoryComparison_nonExistent() {
        StoryComparison retrieved = comparison.getStoryComparison("non-existent.yaml");
        assertNull(retrieved);
    }

    @Test
    @DisplayName("getStoryComparisons() returns unmodifiable map")
    void testGetStoryComparisons_unmodifiable() {
        StoryComparison storyComparison = new StoryComparison("test-story.yaml");
        comparison.addStoryComparison("test-story.yaml", storyComparison);

        Map<String, StoryComparison> comparisons = comparison.getStoryComparisons();

        assertThrows(UnsupportedOperationException.class, () -> {
            comparisons.put("another.yaml", new StoryComparison("another.yaml"));
        });
    }

    @Test
    @DisplayName("setOverallConsistent() updates consistency status")
    void testSetOverallConsistent() {
        comparison.setOverallConsistent(false);
        assertFalse(comparison.isOverallConsistent());

        comparison.setOverallConsistent(true);
        assertTrue(comparison.isOverallConsistent());
    }

    @Test
    @DisplayName("setOverallInconsistencies() updates inconsistencies")
    void testSetOverallInconsistencies() {
        List<String> inconsistencies = List.of("Error 1", "Error 2");
        comparison.setOverallInconsistencies(new java.util.ArrayList<>(inconsistencies));

        List<String> retrieved = comparison.getOverallInconsistencies();
        assertEquals(2, retrieved.size());

        assertThrows(UnsupportedOperationException.class, () -> {
            retrieved.add("Error 3");
        });
    }

    @Test
    @DisplayName("getSummary() returns correct summary for empty comparison")
    void testGetSummary_empty() {
        ConsistencyComparison.ConsistencySummary summary = comparison.getSummary();

        assertEquals(0, summary.getTotalStories());
        assertEquals(0, summary.getConsistentStories());
        assertEquals(0, summary.getInconsistentStories());
        assertTrue(summary.isOverallConsistent());
        assertEquals(0, summary.getTotalInconsistencies());
        assertEquals(100.0, summary.getConsistencyPercentage());
    }

    @Test
    @DisplayName("getSummary() returns correct summary with stories")
    void testGetSummary_withStories() {
        StoryComparison story1 = new StoryComparison("story1.yaml");
        story1.setConsistent(true);

        StoryComparison story2 = new StoryComparison("story2.yaml");
        story2.setConsistent(false);
        story2.setInconsistencies(new java.util.ArrayList<>(List.of("Failed")));

        comparison.addStoryComparison("story1.yaml", story1);
        comparison.addStoryComparison("story2.yaml", story2);

        ConsistencyComparison.ConsistencySummary summary = comparison.getSummary();

        assertEquals(2, summary.getTotalStories());
        assertEquals(1, summary.getConsistentStories());
        assertEquals(1, summary.getInconsistentStories());
        assertFalse(summary.isOverallConsistent());
        assertEquals(2, summary.getTotalInconsistencies());
        assertEquals(50.0, summary.getConsistencyPercentage());
    }

    @Test
    @DisplayName("generateDetailedReport() generates proper report")
    void testGenerateDetailedReport() {
        StoryComparison story1 = new StoryComparison("story1.yaml");
        story1.setConsistent(true);

        comparison.addStoryComparison("story1.yaml", story1);

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("=== Backend Consistency Comparison Report ==="));
        assertTrue(report.contains("Total Test Stories: 1"));
        assertTrue(report.contains("Consistent Stories: 1"));
        assertTrue(report.contains("Inconsistent Stories: 0"));
        assertTrue(report.contains("Overall Consistency: PASSED"));
        assertTrue(report.contains("story1.yaml"));
        assertTrue(report.contains("CONSISTENT"));
    }

    @Test
    @DisplayName("generateDetailedReport() includes inconsistencies when present")
    void testGenerateDetailedReport_withInconsistencies() {
        StoryComparison story1 = new StoryComparison("story1.yaml");
        story1.setConsistent(false);
        story1.setInconsistencies(new java.util.ArrayList<>(List.of("Test inconsistency")));

        comparison.addStoryComparison("story1.yaml", story1);

        String report = comparison.generateDetailedReport();

        assertTrue(report.contains("Overall Consistency: FAILED"));
        assertTrue(report.contains("INCONSISTENT"));
        assertTrue(report.contains("Test inconsistency"));
        assertTrue(report.contains("Overall System Inconsistencies"));
    }

    @Test
    @DisplayName("ConsistencySummary calculates percentage correctly")
    void testConsistencySummary_percentage() {
        ConsistencyComparison.ConsistencySummary summary =
            new ConsistencyComparison.ConsistencySummary(10, 8, 2, true, 5);

        assertEquals(10, summary.getTotalStories());
        assertEquals(8, summary.getConsistentStories());
        assertEquals(2, summary.getInconsistentStories());
        assertTrue(summary.isOverallConsistent());
        assertEquals(5, summary.getTotalInconsistencies());
        assertEquals(80.0, summary.getConsistencyPercentage());
    }

    @Test
    @DisplayName("ConsistencySummary handles zero total stories")
    void testConsistencySummary_zeroTotal() {
        ConsistencyComparison.ConsistencySummary summary =
            new ConsistencyComparison.ConsistencySummary(0, 0, 0, true, 0);

        assertEquals(0, summary.getTotalStories());
        assertEquals(100.0, summary.getConsistencyPercentage());
    }
}
