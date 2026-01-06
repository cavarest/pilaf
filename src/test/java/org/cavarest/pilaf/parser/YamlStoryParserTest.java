package org.cavarest.pilaf.parser;

import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.TestStory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YamlStoryParser
 */
public class YamlStoryParserTest {

    @Test
    public void testParseSimpleStory() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
                      "description: \"A simple test story\"\n" +
                      "steps:\n" +
                      "  - action: \"wait\"\n" +
                      "    duration: 1000\n" +
                      "    name: \"Wait step\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertEquals("Test Story", story.getName());
        assertEquals("A simple test story", story.getDescription());
        assertNotNull(story.getSteps());
        assertEquals(1, story.getSteps().size());

        Action firstStep = story.getSteps().get(0);
        assertEquals(Action.ActionType.WAIT, firstStep.getType());
        assertEquals(1000L, firstStep.getDuration());
        assertEquals("Wait step", firstStep.getName());
    }

    @Test
    public void testParseStoryWithSetupAndCleanup() throws Exception {
        String yaml = "name: \"Complex Story\"\n" +
                      "setup:\n" +
                      "  - action: \"execute_rcon_command\"\n" +
                      "    command: \"say Setup\"\n" +
                      "steps:\n" +
                      "  - action: \"wait\"\n" +
                      "    duration: 1000\n" +
                      "cleanup:\n" +
                      "  - action: \"execute_rcon_command\"\n" +
                      "    command: \"say Cleanup\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertEquals("Complex Story", story.getName());
        assertNotNull(story.getSetup());
        assertEquals(1, story.getSetup().size());
        assertNotNull(story.getSteps());
        assertEquals(1, story.getSteps().size());
        assertNotNull(story.getCleanup());
        assertEquals(1, story.getCleanup().size());
    }

    @Test
    public void testParseInvalidYaml() {
        String invalidYaml = "invalid: yaml: structure::: [[[";
        YamlStoryParser parser = new YamlStoryParser();
        assertThrows(Exception.class, () -> {
            parser.parseString(invalidYaml);
        });
    }
}