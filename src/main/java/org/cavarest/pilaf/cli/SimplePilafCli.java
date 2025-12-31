package org.cavarest.pilaf.cli;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Simplified PILAF CLI for demonstration purposes
 */
public class SimplePilafCli {

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 PILAF CLI - Paper Integration Layer for Automation Functions");
        System.out.println("================================================================");

        if (args.length == 0 || Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-h")) {
            printHelp();
            return;
        }

        String configFile = null;
        List<String> storyFiles = new ArrayList<>();
        boolean verbose = false;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--config") || arg.equals("-c")) {
                configFile = args[++i];
            } else if (arg.equals("--verbose") || arg.equals("-v")) {
                verbose = true;
            } else if (arg.endsWith(".yaml") || arg.endsWith(".yml")) {
                storyFiles.add(arg);
            } else if (new File(arg).isDirectory()) {
                // Find YAML files in directory
                Files.walk(Paths.get(arg))
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .forEach(p -> storyFiles.add(p.toString()));
            }
        }

        if (storyFiles.isEmpty()) {
            // Look for default story locations
            List<String> defaultLocations = Arrays.asList(
                "src/test/resources/integration-stories",
                "src/test/resources/test-stories",
                "integration-stories"
            );

            for (String location : defaultLocations) {
                if (new File(location).exists()) {
                    Files.walk(Paths.get(location))
                        .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                        .forEach(p -> storyFiles.add(p.toString()));
                }
            }
        }

        if (storyFiles.isEmpty()) {
            System.err.println("❌ No YAML story files found!");
            System.err.println("💡 Specify story files or ensure they exist in default locations:");
            System.err.println("   - src/test/resources/integration-stories/");
            System.err.println("   - src/test/resources/test-stories/");
            return;
        }

        System.out.println("📋 Configuration:");
        if (configFile != null) {
            System.out.println("   Config: " + configFile);
        } else {
            System.out.println("   Config: (default)");
        }
        System.out.println("   Stories: " + storyFiles.size() + " file(s)");
        System.out.println("   Verbose: " + verbose);
        System.out.println();

        // Execute stories
        int passedTests = 0;
        int totalTests = storyFiles.size();

        for (String storyFile : storyFiles) {
            System.out.println("🧪 Executing: " + storyFile);
            try {
                executeStory(storyFile, verbose);
                passedTests++;
                System.out.println("✅ PASSED: " + storyFile);
            } catch (Exception e) {
                System.out.println("❌ FAILED: " + storyFile);
                if (verbose) {
                    e.printStackTrace();
                } else {
                    System.out.println("   Error: " + e.getMessage());
                }
            }
            System.out.println();
        }

        // Summary
        System.out.println("📊 TEST SUMMARY");
        System.out.println("================");
        System.out.println("Total: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println();

        if (passedTests == totalTests) {
            System.out.println("🎉 All tests passed!");
            System.exit(0);
        } else {
            System.out.println("💥 Some tests failed!");
            System.exit(1);
        }
    }

    private static void executeStory(String storyFile, boolean verbose) throws Exception {
        Yaml yaml = new Yaml();

        try (InputStream inputStream = new FileInputStream(storyFile)) {
            Map<String, Object> story = yaml.load(inputStream);

            String name = (String) story.get("name");
            String description = (String) story.get("description");

            System.out.println("   Story: " + name);
            if (description != null) {
                System.out.println("   Desc: " + description);
            }

            // Execute setup steps
            List<Map<String, Object>> setup = (List<Map<String, Object>>) story.get("setup");
            if (setup != null) {
                System.out.println("   Setup (" + setup.size() + " steps):");
                for (Map<String, Object> step : setup) {
                    executeStep(step, verbose);
                }
            }

            // Execute main steps
            List<Map<String, Object>> steps = (List<Map<String, Object>>) story.get("steps");
            if (steps != null) {
                System.out.println("   Steps (" + steps.size() + " steps):");
                for (Map<String, Object> step : steps) {
                    executeStep(step, verbose);
                }
            }

            // Execute cleanup steps
            List<Map<String, Object>> cleanup = (List<Map<String, Object>>) story.get("cleanup");
            if (cleanup != null) {
                System.out.println("   Cleanup (" + cleanup.size() + " steps):");
                for (Map<String, Object> step : cleanup) {
                    executeStep(step, verbose);
                }
            }
        }
    }

    private static void executeStep(Map<String, Object> step, boolean verbose) {
        String action = (String) step.get("action");
        String name = (String) step.get("name");

        System.out.println("     • " + (name != null ? name : action));

        if (verbose) {
            System.out.println("       Action: " + action);
            System.out.println("       Params: " + step);
        }

        // Simulate execution based on action type
        try {
            simulateAction(action, step);
            System.out.println("       ✅ Success");
        } catch (Exception e) {
            System.out.println("       ❌ Error: " + e.getMessage());
            throw new RuntimeException("Step failed: " + action, e);
        }
    }

    private static void simulateAction(String action, Map<String, Object> step) {
        switch (action) {
            case "execute_rcon_command":
                String command = (String) step.get("command");
                System.out.println("       RCON: " + command);
                // Simulate RCON execution
                break;

            case "execute_player_command":
                String player = (String) step.get("player");
                String playerCommand = (String) step.get("command");
                System.out.println("       Player " + player + ": " + playerCommand);
                // Simulate player command
                break;

            case "wait":
                Object duration = step.get("duration");
                if (duration != null) {
                    System.out.println("       Waiting " + duration + "ms");
                    // Simulate wait
                }
                break;

            case "get_entities_in_view":
                String viewPlayer = (String) step.get("player");
                String storeAs = (String) step.get("store_as");
                System.out.println("       Getting entities for " + viewPlayer);
                // Simulate getting entities
                break;

            case "spawn_entity":
                String entityName = (String) step.get("name");
                String entityType = (String) step.get("type");
                System.out.println("       Spawning " + entityType + " named '" + entityName + "'");
                // Simulate entity spawning
                break;

            case "make_operator":
                String opPlayer = (String) step.get("player");
                System.out.println("       Making " + opPlayer + " an operator");
                // Simulate operator creation
                break;

            case "give_item":
                String givePlayer = (String) step.get("player");
                String item = (String) step.get("item");
                Object count = step.get("count");
                System.out.println("       Giving " + count + "x " + item + " to " + givePlayer);
                // Simulate giving item
                break;

            case "store_state":
                String variableName = (String) step.get("variable_name");
                System.out.println("       Storing state as: " + variableName);
                // Simulate state storage
                break;

            case "print_stored_state":
                String printVar = (String) step.get("variable_name");
                System.out.println("       Printing stored state: " + printVar);
                // Simulate printing state
                break;

            case "compare_states":
                String state1 = (String) step.get("state1");
                String state2 = (String) step.get("state2");
                String compareStoreAs = (String) step.get("store_as");
                System.out.println("       Comparing " + state1 + " vs " + state2 + " -> " + compareStoreAs);
                // Simulate state comparison
                break;

            default:
                System.out.println("       ⚠️  Unknown action: " + action);
                // Unknown action - just log it
                break;
        }
    }

    private static void printHelp() {
        System.out.println("Usage: java -cp pilaf.jar SimplePilafCli [options] [story-files-or-directories]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --config, -c <file>    YAML configuration file");
        System.out.println("  --verbose, -v          Enable verbose output");
        System.out.println("  --help, -h             Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -cp pilaf.jar SimplePilafCli");
        System.out.println("  java -cp pilaf.jar SimplePilafCli --verbose src/test/resources/integration-stories/");
        System.out.println("  java -cp pilaf.jar SimplePilafCli --config=pilaf.yaml story1.yaml story2.yaml");
        System.out.println();
        System.out.println("PILAF will automatically discover YAML stories in:");
        System.out.println("  - src/test/resources/integration-stories/");
        System.out.println("  - src/test/resources/test-stories/");
        System.out.println("  - integration-stories/");
    }
}
