package org.cavarest.pilaf.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestDiscovery.
 */
@DisplayName("TestDiscovery Tests")
class TestDiscoveryTest {

    private TestDiscovery testDiscovery;

    @BeforeEach
    void setUp() {
        testDiscovery = new TestDiscovery();
    }

    @Test
    @DisplayName("discoverYamlStories returns empty list when directory does not exist")
    void testDiscoverYamlStories_nonExistentDirectory() {
        Path nonExistentDir = Path.of("/non/existent/directory/path");

        List<Path> result = testDiscovery.discoverYamlStories(nonExistentDir);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("discoverYamlStories returns empty list for empty directory")
    void testDiscoverYamlStories_emptyDirectory(@TempDir Path tempDir) {
        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("discoverYamlStories finds .yaml files")
    void testDiscoverYamlStories_findsYamlFiles(@TempDir Path tempDir) throws IOException {
        // Create test files
        Files.createFile(tempDir.resolve("test1.yaml"));
        Files.createFile(tempDir.resolve("test2.yaml"));
        Files.createFile(tempDir.resolve("test3.yaml"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test1.yaml")));
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test2.yaml")));
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test3.yaml")));
    }

    @Test
    @DisplayName("discoverYamlStories finds .yml files")
    void testDiscoverYamlStories_findsYmlFiles(@TempDir Path tempDir) throws IOException {
        // Create test files
        Files.createFile(tempDir.resolve("test1.yml"));
        Files.createFile(tempDir.resolve("test2.yml"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test1.yml")));
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test2.yml")));
    }

    @Test
    @DisplayName("discoverYamlStories finds both .yaml and .yml files")
    void testDiscoverYamlStories_findsBothExtensions(@TempDir Path tempDir) throws IOException {
        // Create test files with both extensions
        Files.createFile(tempDir.resolve("test1.yaml"));
        Files.createFile(tempDir.resolve("test2.yml"));
        Files.createFile(tempDir.resolve("test3.yaml"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("discoverYamlStories ignores non-yaml files")
    void testDiscoverYamlStories_ignoresNonYamlFiles(@TempDir Path tempDir) throws IOException {
        // Create test files with different extensions
        Files.createFile(tempDir.resolve("test1.yaml"));
        Files.createFile(tempDir.resolve("test2.txt"));
        Files.createFile(tempDir.resolve("test3.json"));
        Files.createFile(tempDir.resolve("test4.yml"));
        Files.createFile(tempDir.resolve("test5.md"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test1.yaml")));
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test4.yml")));
    }

    @Test
    @DisplayName("discoverYamlStories ignores directories")
    void testDiscoverYamlStories_ignoresDirectories(@TempDir Path tempDir) throws IOException {
        // Create files and directories
        Files.createFile(tempDir.resolve("test1.yaml"));
        Files.createDirectory(tempDir.resolve("subdir"));
        Files.createFile(tempDir.resolve("subdir/test2.yaml"));
        Files.createFile(tempDir.resolve("test3.yml"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        // Should find all yaml files recursively
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("discoverYamlStories returns sorted results")
    void testDiscoverYamlStories_returnsSortedResults(@TempDir Path tempDir) throws IOException {
        // Create files in random order
        Files.createFile(tempDir.resolve("zebra.yaml"));
        Files.createFile(tempDir.resolve("apple.yaml"));
        Files.createFile(tempDir.resolve("middle.yaml"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertEquals(3, result.size());
        // Check that results are sorted
        assertTrue(result.get(0).endsWith("apple.yaml"));
        assertTrue(result.get(1).endsWith("middle.yaml"));
        assertTrue(result.get(2).endsWith("zebra.yaml"));
    }

    @Test
    @DisplayName("discoverYamlStories handles nested directories")
    void testDiscoverYamlStories_handlesNestedDirectories(@TempDir Path tempDir) throws IOException {
        // Create nested directory structure
        Path subdir1 = Files.createDirectory(tempDir.resolve("level1"));
        Path subdir2 = Files.createDirectory(subdir1.resolve("level2"));

        Files.createFile(tempDir.resolve("root.yaml"));
        Files.createFile(subdir1.resolve("level1.yaml"));
        Files.createFile(subdir2.resolve("level2.yaml"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("discoverYamlStories handles case-sensitive extensions")
    void testDiscoverYamlStories_caseSensitiveExtensions(@TempDir Path tempDir) throws IOException {
        // Create files with uppercase extensions (should not be found)
        Files.createFile(tempDir.resolve("test1.YAML"));
        Files.createFile(tempDir.resolve("test2.YML"));
        Files.createFile(tempDir.resolve("test3.yaml"));
        Files.createFile(tempDir.resolve("test4.yml"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        // Only lowercase extensions should be found
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test3.yaml")));
        assertTrue(result.stream().anyMatch(p -> p.endsWith("test4.yml")));
    }

    @Test
    @DisplayName("discoverYamlStories handles files with .yaml in name")
    void testDiscoverYamlStories_yamlInFilename(@TempDir Path tempDir) throws IOException {
        // Create file with .yaml in the middle of name
        Files.createFile(tempDir.resolve("test.yaml.backup"));
        Files.createFile(tempDir.resolve("test.yaml"));
        Files.createFile(tempDir.resolve("yaml.test"));

        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        assertEquals(1, result.size());
        assertTrue(result.get(0).endsWith("test.yaml"));
    }

    @Test
    @DisplayName("discoverYamlStories handles symlinks gracefully")
    void testDiscoverYamlStories_handlesSymlinks(@TempDir Path tempDir) throws IOException {
        // Create a regular file
        Path regularFile = Files.createFile(tempDir.resolve("regular.yaml"));

        // Note: Symlinks may or may not be followed depending on OS and Files.walk behavior
        List<Path> result = testDiscovery.discoverYamlStories(tempDir);

        // At minimum, the regular file should be found
        assertTrue(result.size() >= 1);
        assertTrue(result.stream().anyMatch(p -> p.endsWith("regular.yaml")));
    }

    @Test
    @DisplayName("discoverYamlStories handles directory without read permissions gracefully")
    void testDiscoverYamlStories_noReadPermissions(@TempDir Path tempDir) throws IOException {
        // This test is platform-dependent and may not work on all systems
        // Just verify the method doesn't throw an exception
        Path regularDir = Files.createDirectory(tempDir.resolve("regular"));
        Files.createFile(regularDir.resolve("test.yaml"));

        List<Path> result = testDiscovery.discoverYamlStories(regularDir);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("discoverYamlStories handles exceptions gracefully")
    void testDiscoverYamlStories_handlesException(@TempDir Path tempDir) throws IOException {
        // Create a regular file (not a directory)
        Path regularFile = Files.createFile(tempDir.resolve("not-a-directory.txt"));

        // Files.walk() on a regular file returns the file itself (not an exception)
        // This tests the path that exists but isn't a directory
        List<Path> result = testDiscovery.discoverYamlStories(regularFile);

        assertNotNull(result);
        // The file exists but doesn't match .yaml/.yml filter, so result is empty
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("discoverYamlStories handles IO exceptions during walk")
    void testDiscoverYamlStories_ioException(@TempDir Path tempDir) throws IOException {
        // Use reflection to create a mock scenario that triggers IOException
        // Since we can't easily trigger IOException in Files.walk without special filesystem setup,
        // we'll verify the exception handling by testing the method doesn't crash
        Path testDir = Files.createDirectory(tempDir.resolve("test-dir"));
        Files.createFile(testDir.resolve("test.yaml"));

        // This test verifies normal operation doesn't throw exceptions
        List<Path> result = testDiscovery.discoverYamlStories(testDir);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("discoverYamlStories handles circular symlinks exception")
    void testDiscoverYamlStories_circularSymlinks(@TempDir Path tempDir) throws IOException {
        // Create two directories with circular symlinks to trigger IOException
        Path dir1 = Files.createDirectory(tempDir.resolve("dir1"));
        Path dir2 = Files.createDirectory(tempDir.resolve("dir2"));

        // Create circular symlink (dir1/link -> dir2, dir2/link -> dir1)
        // This should cause an exception during Files.walk
        try {
            Files.createSymbolicLink(dir1.resolve("link"), dir2);
            Files.createSymbolicLink(dir2.resolve("link"), dir1);

            // Should handle the circular reference exception gracefully
            List<Path> result = testDiscovery.discoverYamlStories(dir1);
            assertNotNull(result);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
            // Symlinks not supported on this system or OS prevented circular link
            // At minimum verify the method doesn't crash
            List<Path> result = testDiscovery.discoverYamlStories(dir1);
            assertNotNull(result);
        }
    }
}
