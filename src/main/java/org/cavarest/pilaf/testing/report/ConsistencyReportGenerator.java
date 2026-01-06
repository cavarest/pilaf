package org.cavarest.pilaf.testing.report;

import org.cavarest.pilaf.testing.BackendConsistencyTester;
import org.cavarest.pilaf.testing.comparison.ConsistencyComparison;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ConsistencyReportGenerator generates comprehensive HTML and text reports
 * for backend consistency testing results.
 */
public class ConsistencyReportGenerator {

    private static final String REPORT_TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    private static final String HTML_REPORT_TEMPLATE = "consistency-report.html";
    private static final String TEXT_REPORT_TEMPLATE = "consistency-report.txt";

    /**
     * Generates a comprehensive consistency report
     */
    public String generateReport(Map<String, BackendConsistencyTester.BackendTestResult> testResults,
                               ConsistencyComparison comparison) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(REPORT_TIMESTAMP_FORMAT));

        // Generate HTML report
        String htmlReportPath = generateHtmlReport(testResults, comparison, timestamp);

        // Generate text report
        String textReportPath = generateTextReport(testResults, comparison, timestamp);

        System.out.println("Consistency reports generated:");
        System.out.println("  HTML: " + htmlReportPath);
        System.out.println("  Text: " + textReportPath);

        return htmlReportPath; // Return the HTML report path as primary
    }

    /**
     * Generates HTML report
     */
    private String generateHtmlReport(Map<String, BackendConsistencyTester.BackendTestResult> testResults,
                                    ConsistencyComparison comparison, String timestamp) {
        String reportPath = "consistency-report-" + timestamp + ".html";

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"en\">");
            writer.println("<head>");
            writer.println("    <meta charset=\"UTF-8\">");
            writer.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("    <title>PILAF Backend Consistency Report</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }");
            writer.println("        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
            writer.println("        .header { text-align: center; border-bottom: 2px solid #007acc; padding-bottom: 20px; margin-bottom: 30px; }");
            writer.println("        .summary { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }");
            writer.println("        .success { color: #28a745; font-weight: bold; }");
            writer.println("        .failure { color: #dc3545; font-weight: bold; }");
            writer.println("        .backend-section { margin-bottom: 30px; border: 1px solid #dee2e6; border-radius: 5px; }");
            writer.println("        .backend-header { background-color: #007acc; color: white; padding: 10px 15px; border-radius: 5px 5px 0 0; }");
            writer.println("        .backend-content { padding: 15px; }");
            writer.println("        .story-result { margin-bottom: 15px; padding: 10px; border-left: 4px solid #007acc; background-color: #f8f9fa; }");
            writer.println("        .inconsistency { background-color: #fff3cd; border-left-color: #ffc107; }");
            writer.println("        .error { background-color: #f8d7da; border-left-color: #dc3545; }");
            writer.println("        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0; }");
            writer.println("        .metric-card { background-color: #f8f9fa; padding: 15px; border-radius: 5px; text-align: center; }");
            writer.println("        .metric-value { font-size: 2em; font-weight: bold; color: #007acc; }");
            writer.println("        .metric-label { color: #6c757d; margin-top: 5px; }");
            writer.println("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
            writer.println("        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #dee2e6; }");
            writer.println("        th { background-color: #f8f9fa; font-weight: bold; }");
            writer.println("        .timestamp { color: #6c757d; font-size: 0.9em; text-align: center; margin-top: 30px; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("    <div class=\"container\">");

            // Header
            writer.println("        <div class=\"header\">");
            writer.println("            <h1>PILAF Backend Consistency Report</h1>");
            writer.println("            <p>Comprehensive analysis of PILAF behavior across different backend combinations</p>");
            writer.println("        </div>");

            // Summary
            generateHtmlSummary(writer, testResults, comparison);

            // Backend results
            generateHtmlBackendResults(writer, testResults);

            // Consistency analysis
            generateHtmlConsistencyAnalysis(writer, comparison);

            // Timestamp
            writer.println("        <div class=\"timestamp\">");
            writer.println("            Report generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("        </div>");

            writer.println("    </div>");
            writer.println("</body>");
            writer.println("</html>");

        } catch (IOException e) {
            System.err.println("Error generating HTML report: " + e.getMessage());
            return "error-generating-report.html";
        }

        return reportPath;
    }

    /**
     * Generates text report
     */
    private String generateTextReport(Map<String, BackendConsistencyTester.BackendTestResult> testResults,
                                    ConsistencyComparison comparison, String timestamp) {
        String reportPath = "consistency-report-" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath))) {
            writer.println("=================================================================");
            writer.println("           PILAF BACKEND CONSISTENCY REPORT");
            writer.println("=================================================================");
            writer.println();
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();

            // Summary
            generateTextSummary(writer, testResults, comparison);

            // Backend results
            generateTextBackendResults(writer, testResults);

            // Consistency analysis
            generateTextConsistencyAnalysis(writer, comparison);

            writer.println();
            writer.println("=================================================================");
            writer.println("                        END OF REPORT");
            writer.println("=================================================================");

        } catch (IOException e) {
            System.err.println("Error generating text report: " + e.getMessage());
            return "error-generating-report.txt";
        }

        return reportPath;
    }

    /**
     * Generates HTML summary section
     */
    private void generateHtmlSummary(PrintWriter writer,
                                   Map<String, BackendConsistencyTester.BackendTestResult> testResults,
                                   ConsistencyComparison comparison) {
        writer.println("        <div class=\"summary\">");
        writer.println("            <h2>Executive Summary</h2>");

        boolean overallConsistent = comparison.isOverallConsistent();
        writer.println("            <div class=\"" + (overallConsistent ? "success" : "failure") + "\">");
        writer.println("                Overall Consistency: " + (overallConsistent ? "PASSED ✓" : "FAILED ✗"));
        writer.println("            </div>");

        // Metrics
        writer.println("            <div class=\"metrics\">");

        int totalBackends = testResults.size();
        int totalStories = comparison.getStoryComparisons().size();
        int passedStories = (int) comparison.getStoryComparisons().values().stream()
            .filter(sc -> sc.isConsistent()).count();

        writer.println("                <div class=\"metric-card\">");
        writer.println("                    <div class=\"metric-value\">" + totalBackends + "</div>");
        writer.println("                    <div class=\"metric-label\">Backend Combinations</div>");
        writer.println("                </div>");

        writer.println("                <div class=\"metric-card\">");
        writer.println("                    <div class=\"metric-value\">" + totalStories + "</div>");
        writer.println("                    <div class=\"metric-label\">Test Stories</div>");
        writer.println("                </div>");

        writer.println("                <div class=\"metric-card\">");
        writer.println("                    <div class=\"metric-value\">" + passedStories + "</div>");
        writer.println("                    <div class=\"metric-label\">Consistent Stories</div>");
        writer.println("                </div>");

        writer.println("                <div class=\"metric-card\">");
        double consistencyPercentage = totalStories > 0 ? (double) passedStories / totalStories * 100 : 0;
        writer.println("                    <div class=\"metric-value\">" + String.format("%.1f%%", consistencyPercentage) + "</div>");
        writer.println("                    <div class=\"metric-label\">Consistency Rate</div>");
        writer.println("                </div>");

        writer.println("            </div>");
        writer.println("        </div>");
    }

    /**
     * Generates HTML backend results section
     */
    private void generateHtmlBackendResults(PrintWriter writer,
                                          Map<String, BackendConsistencyTester.BackendTestResult> testResults) {
        writer.println("        <h2>Backend Test Results</h2>");

        for (Map.Entry<String, BackendConsistencyTester.BackendTestResult> entry : testResults.entrySet()) {
            String backendConfig = entry.getKey();
            BackendConsistencyTester.BackendTestResult result = entry.getValue();

            writer.println("        <div class=\"backend-section\">");
            writer.println("            <div class=\"backend-header\">");
            writer.println("                <h3>" + backendConfig + "</h3>");
            writer.println("                <span class=\"" + (result.isSuccessful() ? "success" : "failure") + "\">");
            writer.println("                    Status: " + (result.isSuccessful() ? "SUCCESS" : "FAILED"));
            writer.println("                </span>");
            writer.println("            </div>");
            writer.println("            <div class=\"backend-content\">");

            // Story results table
            writer.println("                <table>");
            writer.println("                    <thead>");
            writer.println("                        <tr>");
            writer.println("                            <th>Story File</th>");
            writer.println("                            <th>Success</th>");
            writer.println("                            <th>Assertions Passed</th>");
            writer.println("                            <th>Assertions Failed</th>");
            writer.println("                            <th>Execution Time (ms)</th>");
            writer.println("                        </tr>");
            writer.println("                    </thead>");
            writer.println("                    <tbody>");

            for (Map.Entry<String, org.cavarest.pilaf.model.TestResult> storyEntry : result.getStoryResults().entrySet()) {
                String storyFile = storyEntry.getKey();
                org.cavarest.pilaf.model.TestResult storyResult = storyEntry.getValue();

                writer.println("                        <tr>");
                writer.println("                            <td>" + storyFile + "</td>");
                writer.println("                            <td class=\"" + (storyResult.isSuccess() ? "success" : "failure") + "\">");
                writer.println("                                " + (storyResult.isSuccess() ? "✓ PASS" : "✗ FAIL"));
                writer.println("                            </td>");
                writer.println("                            <td>" + storyResult.getAssertionsPassed() + "</td>");
                writer.println("                            <td>" + storyResult.getAssertionsFailed() + "</td>");
                writer.println("                            <td>" + storyResult.getExecutionTimeMs() + "</td>");
                writer.println("                        </tr>");
            }

            writer.println("                    </tbody>");
            writer.println("                </table>");

            // Errors
            if (!result.getErrors().isEmpty()) {
                writer.println("                <div class=\"error\">");
                writer.println("                    <h4>Errors:</h4>");
                writer.println("                    <ul>");
                for (String error : result.getErrors()) {
                    writer.println("                        <li>" + error + "</li>");
                }
                writer.println("                    </ul>");
                writer.println("                </div>");
            }

            writer.println("            </div>");
            writer.println("        </div>");
        }
    }

    /**
     * Generates HTML consistency analysis section
     */
    private void generateHtmlConsistencyAnalysis(PrintWriter writer, ConsistencyComparison comparison) {
        writer.println("        <h2>Consistency Analysis</h2>");

        for (Map.Entry<String, org.cavarest.pilaf.testing.comparison.StoryComparison> entry :
             comparison.getStoryComparisons().entrySet()) {
            String storyFile = entry.getKey();
            org.cavarest.pilaf.testing.comparison.StoryComparison storyComparison = entry.getValue();

            writer.println("        <div class=\"story-result " + (storyComparison.isConsistent() ? "" : "inconsistency") + "\">");
            writer.println("            <h3>" + storyFile + "</h3>");
            writer.println("            <p><strong>Status:</strong> " +
                (storyComparison.isConsistent() ? "<span class=\"success\">CONSISTENT</span>" :
                                                   "<span class=\"failure\">INCONSISTENT</span>") + "</p>");

            if (!storyComparison.isConsistent()) {
                writer.println("            <h4>Inconsistencies:</h4>");
                writer.println("            <ul>");
                for (String inconsistency : storyComparison.getInconsistencies()) {
                    writer.println("                <li>" + inconsistency + "</li>");
                }
                writer.println("            </ul>");
            }

            writer.println("        </div>");
        }
    }

    /**
     * Generates text summary section
     */
    private void generateTextSummary(PrintWriter writer,
                                   Map<String, BackendConsistencyTester.BackendTestResult> testResults,
                                   ConsistencyComparison comparison) {
        writer.println("EXECUTIVE SUMMARY");
        writer.println("==================");
        writer.println();

        boolean overallConsistent = comparison.isOverallConsistent();
        writer.println("Overall Consistency: " + (overallConsistent ? "PASSED" : "FAILED"));
        writer.println();

        int totalBackends = testResults.size();
        int totalStories = comparison.getStoryComparisons().size();
        int passedStories = (int) comparison.getStoryComparisons().values().stream()
            .filter(sc -> sc.isConsistent()).count();

        writer.println("Statistics:");
        writer.println("  Backend Combinations: " + totalBackends);
        writer.println("  Test Stories: " + totalStories);
        writer.println("  Consistent Stories: " + passedStories);
        writer.println("  Inconsistent Stories: " + (totalStories - passedStories));
        writer.println();
    }

    /**
     * Generates text backend results section
     */
    private void generateTextBackendResults(PrintWriter writer,
                                          Map<String, BackendConsistencyTester.BackendTestResult> testResults) {
        writer.println("BACKEND TEST RESULTS");
        writer.println("====================");
        writer.println();

        for (Map.Entry<String, BackendConsistencyTester.BackendTestResult> entry : testResults.entrySet()) {
            String backendConfig = entry.getKey();
            BackendConsistencyTester.BackendTestResult result = entry.getValue();

            writer.println("Backend: " + backendConfig);
            writer.println("Status: " + (result.isSuccessful() ? "SUCCESS" : "FAILED"));
            writer.println("Stories tested: " + result.getTotalStories());
            writer.println("Stories passed: " + result.getPassedStories());
            writer.println();

            for (Map.Entry<String, org.cavarest.pilaf.model.TestResult> storyEntry : result.getStoryResults().entrySet()) {
                String storyFile = storyEntry.getKey();
                org.cavarest.pilaf.model.TestResult storyResult = storyEntry.getValue();

                writer.println("  " + storyFile + ":");
                writer.println("    Success: " + storyResult.isSuccess());
                writer.println("    Assertions Passed: " + storyResult.getAssertionsPassed());
                writer.println("    Assertions Failed: " + storyResult.getAssertionsFailed());
                writer.println("    Execution Time: " + storyResult.getExecutionTimeMs() + "ms");
                writer.println();
            }

            if (!result.getErrors().isEmpty()) {
                writer.println("  Errors:");
                for (String error : result.getErrors()) {
                    writer.println("    - " + error);
                }
                writer.println();
            }

            writer.println();
        }
    }

    /**
     * Generates text consistency analysis section
     */
    private void generateTextConsistencyAnalysis(PrintWriter writer, ConsistencyComparison comparison) {
        writer.println("CONSISTENCY ANALYSIS");
        writer.println("====================");
        writer.println();

        for (Map.Entry<String, org.cavarest.pilaf.testing.comparison.StoryComparison> entry :
             comparison.getStoryComparisons().entrySet()) {
            String storyFile = entry.getKey();
            org.cavarest.pilaf.testing.comparison.StoryComparison storyComparison = entry.getValue();

            writer.println("Story: " + storyFile);
            writer.println("Status: " + (storyComparison.isConsistent() ? "CONSISTENT" : "INCONSISTENT"));
            writer.println();

            if (!storyComparison.isConsistent()) {
                writer.println("Inconsistencies:");
                for (String inconsistency : storyComparison.getInconsistencies()) {
                    writer.println("  - " + inconsistency);
                }
            } else {
                writer.println("All backend combinations produced identical results.");
            }

            writer.println();
        }
    }
}
