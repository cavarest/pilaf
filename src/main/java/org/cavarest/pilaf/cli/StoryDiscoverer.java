package org.cavarest.pilaf.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Discovers YAML story files from directories and individual files
 */
public class StoryDiscoverer {

    /**
     * Discover YAML story files from a file or directory
     */
    public static List<Path> discover(File fileOrDirectory) throws Exception {
        List<Path> stories = new ArrayList<>();

        if (fileOrDirectory.isFile()) {
            // Single file - check if it's a YAML file
            if (isYamlFile(fileOrDirectory.toPath())) {
                stories.add(fileOrDirectory.toPath());
            }
        } else if (fileOrDirectory.isDirectory()) {
            // Directory - recursively find YAML files
            try (Stream<Path> paths = Files.walk(fileOrDirectory.toPath())) {
                stories = paths
                    .filter(Files::isRegularFile)
                    .filter(StoryDiscoverer::isYamlFile)
                    .collect(Collectors.toList());
            }
        }

        return stories;
    }

    /**
     * Check if a file is a YAML story file
     */
    private static boolean isYamlFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    /**
     * Get default story directories to search in
     */
    public static List<Path> getDefaultStoryDirectories() {
        return List.of(
            Paths.get("src/test/resources/integration-stories"),
            Paths.get("src/test/resources/test-stories"),
            Paths.get("stories"),
            Paths.get("src/test/resources")
        );
    }

    /**
     * Check if any default story directories exist
     */
    public static boolean hasDefaultStoryDirectories() {
        return getDefaultStoryDirectories().stream()
            .anyMatch(path -> path.toFile().exists());
    }
}
