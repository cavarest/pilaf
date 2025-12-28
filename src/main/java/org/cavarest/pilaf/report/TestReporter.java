package org.cavarest.pilaf.report;

import org.cavarest.pilaf.model.TestResult;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TestReporter {
    private final List<TestResult> results = new ArrayList<>();
    private String outputDir = "target/pilaf-reports";

    public void addResult(TestResult result) { results.add(result); }
    public void setOutputDir(String dir) { this.outputDir = dir; }

    public void generateTextReport() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("PILAF TEST REPORT\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        int passed = 0, failed = 0;
        for (TestResult r : results) {
            String status = r.isSuccess() ? "PASSED" : "FAILED";
            sb.append(r.getStoryName()).append(" - ").append(status).append("\n");
            if (r.isSuccess()) passed++; else failed++;
        }
        sb.append("\nTotal: ").append(results.size()).append(" Passed: ").append(passed).append(" Failed: ").append(failed).append("\n");

        Files.createDirectories(Paths.get(outputDir));
        Files.writeString(Paths.get(outputDir, "report.txt"), sb.toString());
        System.out.println(sb);
    }

    public void generateJsonReport() throws IOException {
        StringBuilder sb = new StringBuilder("{\n  \"results\": [\n");
        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            sb.append("    {\"story\":\"").append(r.getStoryName())
              .append("\",\"success\":").append(r.isSuccess())
              .append(",\"timeMs\":").append(r.getExecutionTimeMs()).append("}");
            if (i < results.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        Files.createDirectories(Paths.get(outputDir));
        Files.writeString(Paths.get(outputDir, "report.json"), sb.toString());
    }

    public void generateJUnitXml() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n<testsuite tests=\"").append(results.size()).append("\">\n");
        for (TestResult r : results) {
            sb.append("  <testcase name=\"").append(r.getStoryName()).append("\">\n");
            if (!r.isSuccess()) sb.append("    <failure/>\n");
            sb.append("  </testcase>\n");
        }
        sb.append("</testsuite>");
        Files.createDirectories(Paths.get(outputDir));
        Files.writeString(Paths.get(outputDir, "TEST-pilaf.xml"), sb.toString());
    }

    public void generateAllReports() throws IOException {
        generateTextReport();
        generateJsonReport();
        generateJUnitXml();
    }

    public boolean hasFailures() { return results.stream().anyMatch(r -> !r.isSuccess()); }
    public int getPassedCount() { return (int) results.stream().filter(TestResult::isSuccess).count(); }
    public int getFailedCount() { return (int) results.stream().filter(r -> !r.isSuccess()).count(); }
}
