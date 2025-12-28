package org.cavarest.pilaf;

import org.cavarest.pilaf.backend.*;
import org.cavarest.pilaf.model.*;
import org.cavarest.pilaf.orchestrator.TestOrchestrator;
import org.cavarest.pilaf.report.TestReporter;

import java.io.File;
import java.util.*;

/**
 * CLI runner for PILAF integration tests.
 * Usage: java -jar pilaf.jar [options] <story-files...>
 */
public class PilafRunner {

    public static void main(String[] args) {
        Config config = parseArgs(args);

        if (config.showHelp || config.storyFiles.isEmpty()) {
            printHelp();
            System.exit(config.showHelp ? 0 : 1);
        }

        try {
            int exitCode = run(config);
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            if (config.verbose) e.printStackTrace();
            System.exit(1);
        }
    }

    public static int run(Config config) throws Exception {
        PilafBackend backend = PilafBackendFactory.create(
            config.backendType, config.rconHost, config.rconPort, config.rconPassword);

        TestReporter reporter = new TestReporter();
        reporter.setOutputDir(config.reportDir);

        System.out.println("🚀 PILAF Integration Test Runner");
        System.out.println("   Backend: " + config.backendType);
        System.out.println("   Stories: " + config.storyFiles.size());
        System.out.println();

        for (String storyPath : config.storyFiles) {
            TestOrchestrator orchestrator = new TestOrchestrator(backend);
            orchestrator.setVerbose(config.verbose);

            try {
                if (storyPath.startsWith("classpath:")) {
                    orchestrator.loadStory(storyPath.substring(10));
                } else {
                    orchestrator.loadStoryFromString(
                        java.nio.file.Files.readString(java.nio.file.Path.of(storyPath)));
                }

                TestResult result = orchestrator.execute();
                reporter.addResult(result);
            } catch (Exception e) {
                TestResult failedResult = new TestResult(storyPath);
                failedResult.setSuccess(false);
                failedResult.setError(e);
                reporter.addResult(failedResult);
            }
        }

        reporter.generateAllReports();

        return reporter.hasFailures() ? 1 : 0;
    }

    private static Config parseArgs(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h": case "--help":
                    config.showHelp = true;
                    break;
                case "-v": case "--verbose":
                    config.verbose = true;
                    break;
                case "-b": case "--backend":
                    if (++i < args.length) config.backendType = args[i];
                    break;
                case "--rcon-host":
                    if (++i < args.length) config.rconHost = args[i];
                    break;
                case "--rcon-port":
                    if (++i < args.length) config.rconPort = Integer.parseInt(args[i]);
                    break;
                case "--rcon-password":
                    if (++i < args.length) config.rconPassword = args[i];
                    break;
                case "-o": case "--output":
                    if (++i < args.length) config.reportDir = args[i];
                    break;
                default:
                    if (!arg.startsWith("-")) {
                        config.storyFiles.add(arg);
                    }
            }
        }

        return config;
    }

    private static void printHelp() {
        System.out.println("PILAF - Paper Integration Layer for Automation Functions");
        System.out.println();
        System.out.println("Usage: pilaf [options] <story-files...>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help           Show this help message");
        System.out.println("  -v, --verbose        Enable verbose output");
        System.out.println("  -b, --backend TYPE   Backend type: rcon, mineflayer (default: rcon)");
        System.out.println("  --rcon-host HOST     RCON host (default: localhost)");
        System.out.println("  --rcon-port PORT     RCON port (default: 25575)");
        System.out.println("  --rcon-password PWD  RCON password (default: dragon123)");
        System.out.println("  -o, --output DIR     Output directory for reports");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  pilaf test-stories/*.yaml");
        System.out.println("  pilaf -b mineflayer --rcon-host mc.local tests/");
    }

    public static class Config {
        public boolean showHelp = false;
        public boolean verbose = false;
        public String backendType = "rcon";
        public String rconHost = "localhost";
        public int rconPort = 25575;
        public String rconPassword = "dragon123";
        public String reportDir = "target/pilaf-reports";
        public List<String> storyFiles = new ArrayList<>();
    }
}
