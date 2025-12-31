package org.cavarest.pilaf.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Test Discovery Service
 *
 * Scans plugin repositories for YAML test story files.
 */
public class TestDiscovery {

    /**
     * Discover all YAML test story files in the specified directory
     */
    public List<Path> discoverYamlStories(Path testStoriesDir) {
        List<Path> storyFiles = new ArrayList<>();

        if (!Files.exists(testStoriesDir)) {
            return storyFiles;
        }

        try (Stream<Path> files = Files.walk(testStoriesDir)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                .sorted()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (Exception e) {
            System.out.println("Error discovering stories: " + e.getMessage());
            return storyFiles;
        }
    }
}
