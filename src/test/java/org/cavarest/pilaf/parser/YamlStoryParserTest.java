package org.cavarest.pilaf.parser;

import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.Assertion;
import org.cavarest.pilaf.model.TestStory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

/**
 * Integration tests for YamlStoryParser covering complex story structures,
 * state management workflows, and various action types.
 */
public class YamlStoryParserTest {

    // ========================================================================
    // BASIC PARSING TESTS
    // ========================================================================

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

    // ========================================================================
    // SERVER COMMAND PARSING TESTS
    // ========================================================================

    @Test
    public void testParseExecuteRconCommand() throws Exception {
        String yaml = "name: \"RCON Test\"\n" +
                      "steps:\n" +
                      "  - action: \"execute_rcon_command\"\n" +
                      "    command: \"give pilaf_tester diamond_sword 1\"\n" +
                      "    name: \"Give diamond sword\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.EXECUTE_RCON_COMMAND, step.getType());
        assertEquals("give pilaf_tester diamond_sword 1", step.getCommand());
        assertEquals("Give diamond sword", step.getName());
    }

    @Test
    public void testParseExecuteRconWithArgs() throws Exception {
        String yaml = "name: \"RCON Args Test\"\n" +
                      "steps:\n" +
                      "  - action: \"execute_rcon_command\"\n" +
                      "    command: \"op\"\n" +
                      "    args:\n" +
                      "      - \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.EXECUTE_RCON_COMMAND, step.getType());
        assertNotNull(step.getArgs());
        assertEquals(1, step.getArgs().size());
        assertEquals("pilaf_tester", step.getArgs().get(0));
    }

    @Test
    public void testParseSpawnEntity() throws Exception {
        String yaml = "name: \"Spawn Entity Test\"\n" +
                      "steps:\n" +
                      "  - action: \"spawn_entity\"\n" +
                      "    type: \"minecraft:zombie\"\n" +
                      "    location: [100, 65, 100]\n" +
                      "    customName: \"test_zombie\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.SPAWN_ENTITY, step.getType());
        assertEquals("minecraft:zombie", step.getEntityType());
        assertNotNull(step.getLocation());
        assertEquals(3, step.getLocation().size());
        assertEquals(100.0, step.getLocation().get(0));
        assertEquals("test_zombie", step.getCustomName());
    }

    // ========================================================================
    // PLAYER COMMAND PARSING TESTS
    // ========================================================================

    @Test
    public void testParseConnectPlayer() throws Exception {
        String yaml = "name: \"Connect Test\"\n" +
                      "setup:\n" +
                      "  - action: \"connect_player\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    name: \"Connect player\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSetup().get(0);
        assertEquals(Action.ActionType.CONNECT_PLAYER, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
    }

    @Test
    public void testParseDisconnectPlayer() throws Exception {
        String yaml = "name: \"Disconnect Test\"\n" +
                      "cleanup:\n" +
                      "  - action: \"disconnect_player\"\n" +
                      "    player: \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getCleanup().get(0);
        assertEquals(Action.ActionType.DISCONNECT_PLAYER, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
    }

    @Test
    public void testParseSendChatMessage() throws Exception {
        String yaml = "name: \"Chat Test\"\n" +
                      "steps:\n" +
                      "  - action: \"send_chat_message\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    message: \"Hello from Pilaf!\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.SEND_CHAT_MESSAGE, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
        assertEquals("Hello from Pilaf!", step.getMessage());
    }

    @Test
    public void testParseGetPlayerPosition() throws Exception {
        String yaml = "name: \"Position Test\"\n" +
                      "steps:\n" +
                      "  - action: \"get_player_position\"\n" +
                      "    player: \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.GET_PLAYER_POSITION, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
    }

    @Test
    public void testParseMovePlayer() throws Exception {
        String yaml = "name: \"Move Test\"\n" +
                      "steps:\n" +
                      "  - action: \"move_player\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    location: [110, 65, 110]\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.MOVE_PLAYER, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
        assertNotNull(step.getLocation());
        assertEquals(3, step.getLocation().size());
    }

    // ========================================================================
    // INVENTORY COMMAND PARSING TESTS
    // ========================================================================

    @Test
    public void testParseGiveItem() throws Exception {
        String yaml = "name: \"Give Item Test\"\n" +
                      "steps:\n" +
                      "  - action: \"give_item\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    item: \"diamond_sword\"\n" +
                      "    count: 1\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.GIVE_ITEM, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
        assertEquals("diamond_sword", step.getItem());
        assertEquals(Integer.valueOf(1), step.getCount());
    }

    @Test
    public void testParseGetInventory() throws Exception {
        String yaml = "name: \"Inventory Test\"\n" +
                      "steps:\n" +
                      "  - action: \"get_inventory\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    storeAs: \"inventory_before\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.GET_INVENTORY, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
        assertEquals("inventory_before", step.getStoreAs());
    }

    @Test
    public void testParseClearInventory() throws Exception {
        String yaml = "name: \"Clear Inventory Test\"\n" +
                      "steps:\n" +
                      "  - action: \"clear_inventory\"\n" +
                      "    player: \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.CLEAR_INVENTORY, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
    }

    // ========================================================================
    // ENTITY COMMAND PARSING TESTS
    // ========================================================================

    @Test
    public void testParseGetEntities() throws Exception {
        String yaml = "name: \"Get Entities Test\"\n" +
                      "steps:\n" +
                      "  - action: \"get_entities\"\n" +
                      "    player: \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.GET_ENTITIES, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
    }

    @Test
    public void testParseKillEntity() throws Exception {
        String yaml = "name: \"Kill Entity Test\"\n" +
                      "steps:\n" +
                      "  - action: \"kill_entity\"\n" +
                      "    entity: \"test_zombie\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.KILL_ENTITY, step.getType());
        assertEquals("test_zombie", step.getEntity());
    }

    @Test
    public void testParseRemoveEntities() throws Exception {
        String yaml = "name: \"Remove Entities Test\"\n" +
                      "steps:\n" +
                      "  - action: \"remove_entities\"\n" +
                      "    type: \"zombie\"\n" +
                      "    location: [100, 65, 100]\n" +
                      "    count: 10\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.REMOVE_ENTITIES, step.getType());
        // Parser uses "type" field for entityType
        assertNotNull(step.getLocation());
        assertEquals(3, step.getLocation().size());
    }

    // ========================================================================
    // STATE MANAGEMENT PARSING TESTS
    // ========================================================================

    @Test
    public void testParseStoreState() throws Exception {
        String yaml = "name: \"Store State Test\"\n" +
                      "steps:\n" +
                      "  - action: \"store_state\"\n" +
                      "    variableName: \"item_count_before\"\n" +
                      "    fromCommandResult: \"inventory_before\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.STORE_STATE, step.getType());
        assertEquals("item_count_before", step.getVariableName());
        assertEquals("inventory_before", step.getFromCommandResult());
    }

    @Test
    public void testParseCompareStates() throws Exception {
        String yaml = "name: \"Compare States Test\"\n" +
                      "steps:\n" +
                      "  - action: \"compare_states\"\n" +
                      "    state1: \"inventory_before\"\n" +
                      "    state2: \"inventory_after\"\n" +
                      "    storeAs: \"inventory_comparison\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.COMPARE_STATES, step.getType());
        assertEquals("inventory_before", step.getState1());
        assertEquals("inventory_after", step.getState2());
        assertEquals("inventory_comparison", step.getStoreAs());
    }

    @Test
    public void testParsePrintStoredState() throws Exception {
        String yaml = "name: \"Print State Test\"\n" +
                      "steps:\n" +
                      "  - action: \"print_stored_state\"\n" +
                      "    variableName: \"inventory_before\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.PRINT_STORED_STATE, step.getType());
        assertEquals("inventory_before", step.getVariableName());
    }

    @Test
    public void testParsePrintStateComparison() throws Exception {
        String yaml = "name: \"Print Comparison Test\"\n" +
                      "steps:\n" +
                      "  - action: \"print_state_comparison\"\n" +
                      "    variableName: \"inventory_comparison\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.PRINT_STATE_COMPARISON, step.getType());
        assertEquals("inventory_comparison", step.getVariableName());
    }

    // ========================================================================
    // DATA EXTRACTION PARSING TESTS
    // ========================================================================

    @Test
    public void testParseExtractWithJsonPath() throws Exception {
        String yaml = "name: \"JSONPath Test\"\n" +
                      "steps:\n" +
                      "  - action: \"extract_with_jsonpath\"\n" +
                      "    sourceVariable: \"${{ steps.get_inventory.outputs.result }}\"\n" +
                      "    jsonPath: \"$.items[0].id\"\n" +
                      "    storeAs: \"first_item\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.EXTRACT_WITH_JSONPATH, step.getType());
        assertEquals("${{ steps.get_inventory.outputs.result }}", step.getSourceVariable());
        assertEquals("$.items[0].id", step.getJsonPath());
        assertEquals("first_item", step.getStoreAs());
    }

    @Test
    public void testParseFilterEntities() throws Exception {
        String yaml = "name: \"Filter Entities Test\"\n" +
                      "steps:\n" +
                      "  - action: \"filter_entities\"\n" +
                      "    sourceVariable: \"${{ steps.get_entities.outputs.result }}\"\n" +
                      "    filterType: \"type\"\n" +
                      "    filterValue: \"zombie\"\n" +
                      "    storeAs: \"zombies\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.FILTER_ENTITIES, step.getType());
        assertEquals("${{ steps.get_entities.outputs.result }}", step.getSourceVariable());
        assertEquals("type", step.getFilterType());
        assertEquals("zombie", step.getFilterValue());
        assertEquals("zombies", step.getStoreAs());
    }

    // ========================================================================
    // ASSERTION PARSING TESTS
    // ========================================================================

    @Test
    public void testParseAssertEntityExists() throws Exception {
        String yaml = "name: \"Assert Entity Test\"\n" +
                      "steps:\n" +
                      "  - action: \"assert_entity_exists\"\n" +
                      "    entity: \"test_zombie\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.ASSERT_ENTITY_EXISTS, step.getType());
        assertEquals("test_zombie", step.getEntity());
    }

    @Test
    public void testParseAssertEntityMissing() throws Exception {
        String yaml = "name: \"Assert Missing Test\"\n" +
                      "steps:\n" +
                      "  - action: \"assert_entity_missing\"\n" +
                      "    entity: \"dead_zombie\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.ASSERT_ENTITY_MISSING, step.getType());
        assertEquals("dead_zombie", step.getEntity());
    }

    @Test
    public void testParseAssertResponseContains() throws Exception {
        String yaml = "name: \"Assert Response Test\"\n" +
                      "steps:\n" +
                      "  - action: \"assert_response_contains\"\n" +
                      "    source: \"${{ steps.execute.outputs.result }}\"\n" +
                      "    contains: \"Success\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.ASSERT_RESPONSE_CONTAINS, step.getType());
        assertEquals("${{ steps.execute.outputs.result }}", step.getSource());
        assertEquals("Success", step.getContains());
    }

    @Test
    public void testParseAssertLogContains() throws Exception {
        String yaml = "name: \"Assert Log Test\"\n" +
                      "steps:\n" +
                      "  - action: \"assert_log_contains\"\n" +
                      "    source: \"${{ steps.check_log.outputs.result }}\"\n" +
                      "    contains: \"MyPlugin loaded\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.ASSERT_LOG_CONTAINS, step.getType());
        assertEquals("${{ steps.check_log.outputs.result }}", step.getSource());
        assertEquals("MyPlugin loaded", step.getContains());
    }

    // ========================================================================
    // WORLD COMMAND PARSING TESTS
    // ========================================================================

    @Test
    public void testParseGetWorldTime() throws Exception {
        String yaml = "name: \"World Time Test\"\n" +
                      "steps:\n" +
                      "  - action: \"get_world_time\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.GET_WORLD_TIME, step.getType());
    }

    @Test
    public void testParseGetWeather() throws Exception {
        String yaml = "name: \"Weather Test\"\n" +
                      "steps:\n" +
                      "  - action: \"get_weather\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.GET_WEATHER, step.getType());
    }

    @Test
    public void testParseSetWeather() throws Exception {
        String yaml = "name: \"Set Weather Test\"\n" +
                      "steps:\n" +
                      "  - action: \"set_weather\"\n" +
                      "    weather: \"clear\"\n" +
                      "    duration: 600\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.SET_WEATHER, step.getType());
    }

    // ========================================================================
    // UTILITY PARSING TESTS
    // ========================================================================

    @Test
    public void testParseWait() throws Exception {
        String yaml = "name: \"Wait Test\"\n" +
                      "steps:\n" +
                      "  - action: \"wait\"\n" +
                      "    duration: 5000\n" +
                      "    name: \"Wait for server\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.WAIT, step.getType());
        assertEquals(Long.valueOf(5000L), step.getDuration());
        assertEquals("Wait for server", step.getName());
    }

    @Test
    public void testParseCheckServiceHealth() throws Exception {
        String yaml = "name: \"Health Check Test\"\n" +
                      "steps:\n" +
                      "  - action: \"check_service_health\"\n" +
                      "    source: \"http://localhost:3000/health\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.CHECK_SERVICE_HEALTH, step.getType());
        assertEquals("http://localhost:3000/health", step.getSource());
    }

    @Test
    public void testParseWaitForChatMessage() throws Exception {
        String yaml = "name: \"Wait Chat Test\"\n" +
                      "steps:\n" +
                      "  - action: \"wait_for_chat_message\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    pattern: \".*test.*\"\n" +
                      "    duration: 5000\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        Action step = story.getSteps().get(0);
        assertEquals(Action.ActionType.WAIT_FOR_CHAT_MESSAGE, step.getType());
        assertEquals("pilaf_tester", step.getPlayer());
        assertEquals(".*test.*", step.getPattern());
        // Duration is parsed and converted to milliseconds
        assertNotNull(step.getDuration());
        assertEquals(5000L, step.getDuration());
    }

    // ========================================================================
    // COMPLEX STORY PARSING TESTS
    // ========================================================================

    @Test
    public void testParseComplexStoryWithAllSections() throws Exception {
        String yaml = "name: \"Complex Integration Test\"\n" +
                      "description: \"A comprehensive test story\"\n" +
                      "setup:\n" +
                      "  - action: \"connect_player\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "  - action: \"wait\"\n" +
                      "    duration: 5000\n" +
                      "    name: \"Wait for connection\"\n" +
                      "steps:\n" +
                      "  - action: \"execute_rcon_command\"\n" +
                      "    command: \"say Starting test...\"\n" +
                      "  - action: \"give_item\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    item: \"diamond_sword\"\n" +
                      "    count: 1\n" +
                      "  - action: \"get_inventory\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    storeAs: \"inventory_before\"\n" +
                      "cleanup:\n" +
                      "  - action: \"clear_inventory\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "  - action: \"disconnect_player\"\n" +
                      "    player: \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertEquals("Complex Integration Test", story.getName());
        assertEquals("A comprehensive test story", story.getDescription());
        assertEquals(2, story.getSetup().size());
        assertEquals(3, story.getSteps().size());
        assertEquals(2, story.getCleanup().size());

        // Verify setup actions
        assertEquals(Action.ActionType.CONNECT_PLAYER, story.getSetup().get(0).getType());
        assertEquals(Action.ActionType.WAIT, story.getSetup().get(1).getType());

        // Verify step actions
        assertEquals(Action.ActionType.EXECUTE_RCON_COMMAND, story.getSteps().get(0).getType());
        assertEquals(Action.ActionType.GIVE_ITEM, story.getSteps().get(1).getType());
        assertEquals(Action.ActionType.GET_INVENTORY, story.getSteps().get(2).getType());

        // Verify cleanup actions
        assertEquals(Action.ActionType.CLEAR_INVENTORY, story.getCleanup().get(0).getType());
        assertEquals(Action.ActionType.DISCONNECT_PLAYER, story.getCleanup().get(1).getType());
    }

    @Test
    public void testParseStateManagementWorkflow() throws Exception {
        String yaml = "name: \"State Management Test\"\n" +
                      "steps:\n" +
                      "  - action: \"get_inventory\"\n" +
                      "    id: \"get_inv\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    storeAs: \"inventory_before\"\n" +
                      "  - action: \"store_state\"\n" +
                      "    variableName: \"item_count_before\"\n" +
                      "    fromCommandResult: \"inventory_before\"\n" +
                      "  - action: \"execute_rcon_command\"\n" +
                      "    command: \"give pilaf_tester iron_sword 1\"\n" +
                      "  - action: \"wait\"\n" +
                      "    duration: 2000\n" +
                      "  - action: \"get_inventory\"\n" +
                      "    id: \"get_inv_after\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    storeAs: \"inventory_after\"\n" +
                      "  - action: \"compare_states\"\n" +
                      "    state1: \"inventory_before\"\n" +
                      "    state2: \"inventory_after\"\n" +
                      "    storeAs: \"inventory_comparison\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertEquals(6, story.getSteps().size());

        // Verify state management actions
        assertEquals(Action.ActionType.GET_INVENTORY, story.getSteps().get(0).getType());
        assertEquals(Action.ActionType.STORE_STATE, story.getSteps().get(1).getType());
        assertEquals(Action.ActionType.EXECUTE_RCON_COMMAND, story.getSteps().get(2).getType());
        assertEquals(Action.ActionType.WAIT, story.getSteps().get(3).getType());
        assertEquals(Action.ActionType.GET_INVENTORY, story.getSteps().get(4).getType());
        assertEquals(Action.ActionType.COMPARE_STATES, story.getSteps().get(5).getType());
    }

    @Test
    public void testParsePlayerManagementStory() throws Exception {
        String yaml = "name: \"Player Management Test\"\n" +
                      "steps:\n" +
                      "  - action: \"make_operator\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "  - action: \"gamemode_change\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    value: \"creative\"\n" +
                      "  - action: \"set_spawn_point\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "    location: [0, 64, 0]\n" +
                      "  - action: \"get_player_health\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "  - action: \"heal_player\"\n" +
                      "    player: \"pilaf_tester\"\n" +
                      "  - action: \"kill_player\"\n" +
                      "    player: \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertEquals(6, story.getSteps().size());

        assertEquals(Action.ActionType.MAKE_OPERATOR, story.getSteps().get(0).getType());
        assertEquals(Action.ActionType.GAMEMODE_CHANGE, story.getSteps().get(1).getType());
        assertEquals(Action.ActionType.SET_SPAWN_POINT, story.getSteps().get(2).getType());
        assertEquals(Action.ActionType.GET_PLAYER_HEALTH, story.getSteps().get(3).getType());
        assertEquals(Action.ActionType.HEAL_PLAYER, story.getSteps().get(4).getType());
        assertEquals(Action.ActionType.KILL_PLAYER, story.getSteps().get(5).getType());
    }

    // ========================================================================
    // ERROR HANDLING TESTS
    // ========================================================================

    @Test
    public void testParseEmptySteps() throws Exception {
        String yaml = "name: \"Empty Steps Test\"\n" +
                      "steps: []\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertNotNull(story.getSteps());
        assertTrue(story.getSteps().isEmpty());
    }

    @Test
    public void testParseStoryWithOnlyName() throws Exception {
        String yaml = "name: \"Minimal Story\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        assertEquals("Minimal Story", story.getName());
    }

    @Test
    public void testParseStoryWithBackend() throws Exception {
        String yaml = "name: \"Backend Test\"\n" +
                      "backend: mineflayer\n" +
                      "steps:\n" +
                      "  - action: \"connect_player\"\n" +
                      "    player: \"pilaf_tester\"\n";

        YamlStoryParser parser = new YamlStoryParser();
        TestStory story = parser.parseString(yaml);

        assertNotNull(story);
        // Backend parsing would be tested with a full parser implementation
    }
}
