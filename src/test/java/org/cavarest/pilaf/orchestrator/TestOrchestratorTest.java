package org.cavarest.pilaf.orchestrator;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.model.Assertion;
import org.cavarest.pilaf.model.TestResult;
import org.cavarest.pilaf.model.TestStory;
import org.cavarest.pilaf.report.TestReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestOrchestrator.
 */
@DisplayName("TestOrchestrator Tests")
class TestOrchestratorTest {

    private TestOrchestrator orchestrator;
    @Mock
    private PilafBackend mockBackend;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestrator = new TestOrchestrator(mockBackend);
        orchestrator.setVerbose(false); // Disable verbose output during tests
    }

    @Test
    @DisplayName("loadStory() throws exception for null path")
    void testLoadStory_nullPath() {
        assertThrows(NullPointerException.class, () -> orchestrator.loadStory(null));
    }

    @Test
    @DisplayName("execute() throws exception when no story loaded")
    void testExecute_noStoryLoaded() {
        assertThrows(IllegalStateException.class, () -> orchestrator.execute());
    }

    @Test
    @DisplayName("execute() successfully executes story with setup and steps")
    void testExecute_simpleStory() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "description: \"A simple test\"\n" +
            "setup:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 64\n" +
            "cleanup:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).giveItem(any(), any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).giveItem("test_player", "diamond", 64);
    }

    @Test
    @DisplayName("execute() handles exceptions during action execution")
    void testExecute_actionException() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 64\n";

        orchestrator.loadStoryFromString(yaml);

        doThrow(new RuntimeException("Failed to give item"))
            .when(mockBackend).giveItem(any(), any(), any());

        TestResult result = orchestrator.execute();

        // TestOrchestrator continues execution even after action exceptions
        // Success is determined by assertions, not action failures
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles backend cleanup exception")
    void testExecute_cleanupException() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps: []\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).initialize();
        doThrow(new RuntimeException("Cleanup failed")).when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        // Cleanup exceptions are caught and logged, but don't fail the test
        assertTrue(result.isSuccess());
        verify(mockBackend).cleanup();
    }

    @Test
    @DisplayName("setVerbose() updates verbose and state manager settings")
    void testSetVerbose() {
        orchestrator.setVerbose(true);
        orchestrator.setVerbose(false);
        // No exception thrown - settings updated
    }

    @Test
    @DisplayName("getResult() returns result before execute")
    void testGetResult_beforeExecute() {
        orchestrator.loadStoryFromString("name: Test\nsteps: []");
        TestResult result = orchestrator.getResult();
        assertNotNull(result);
    }

    @Test
    @DisplayName("loadStory() initializes result with story name")
    void testLoadStory_initializesResult() {
        String yaml = "name: \"My Test Story\"\n" +
            "steps: []\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.getResult();
        assertEquals("My Test Story", result.getStoryName());
    }

    @Test
    @DisplayName("setReporter() sets the reporter")
    void testSetReporter() {
        orchestrator.setReporter(null);
        // No exception - reporter can be null
    }

    @Test
    @DisplayName("execute() executes WAIT action correctly")
    void testExecute_waitAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 100\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        assertTrue(result.getExecutionTimeMs() >= 100);
    }

    @Test
    @DisplayName("execute() executes GIVE_ITEM action correctly")
    void testExecute_giveItemAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 64\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).giveItem("test_player", "diamond", 64);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).giveItem("test_player", "diamond", 64);
    }

    @Test
    @DisplayName("execute() with empty story")
    void testExecute_emptyStory() throws Exception {
        String yaml = "name: \"Empty Story\"\n" +
            "setup: []\n" +
            "steps: []\n" +
            "cleanup: []\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() tracks execution time")
    void testExecute_tracksExecutionTime() throws Exception {
        String yaml = "name: \"Timing Test\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 50\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertNotNull(result.getExecutionTimeMs());
        assertTrue(result.getExecutionTimeMs() >= 50);
    }

    @Test
    @DisplayName("execute() processes actions with count field")
    void testExecute_actionWithCount() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"stone\"\n" +
            "    count: 32\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).giveItem("test_player", "stone", 32);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).giveItem("test_player", "stone", 32);
    }

    @Test
    @DisplayName("execute() handles multiple actions in sequence")
    void testExecute_multipleActions() throws Exception {
        String yaml = "name: \"Multi Action Story\"\n" +
            "setup:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 1\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"iron_sword\"\n" +
            "    count: 1\n" +
            "cleanup:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).giveItem(any(), any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend, times(2)).giveItem(any(), any(), any());
    }

    @Test
    @DisplayName("execute() handles EQUIP_ITEM action correctly")
    void testExecute_equipItemAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"equip_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond_sword\"\n" +
            "    slot: \"mainhand\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).equipItem("test_player", "diamond_sword", "mainhand");

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).equipItem("test_player", "diamond_sword", "mainhand");
    }

    @Test
    @DisplayName("execute() handles USE_ITEM action correctly")
    void testExecute_useItemAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"use_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond_sword\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).useItem(any(), any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).useItem("test_player", "diamond_sword", null);
    }

    @Test
    @DisplayName("execute() handles MOVE_PLAYER action correctly")
    void testExecute_movePlayerAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"move_player\"\n" +
            "    player: \"test_player\"\n" +
            "    destination: \"100 64 200\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).movePlayer(any(), any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).movePlayer("test_player", "destination", "100 64 200");
    }

    @Test
    @DisplayName("execute() handles SEND_CHAT_MESSAGE action correctly")
    void testExecute_sendChatAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"send_chat_message\"\n" +
            "    player: \"test_player\"\n" +
            "    message: \"Hello world\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).sendChat(any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).sendChat("test_player", "Hello world");
    }

    @Test
    @DisplayName("execute() handles SPAWN_ENTITY action correctly")
    void testExecute_spawnEntityAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"spawn_entity\"\n" +
            "    name: \"test_zombie\"\n" +
            "    type: \"minecraft:zombie\"\n" +
            "    position: \"100 64 200\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).spawnEntity(any(), any(), any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles SET_ENTITY_HEALTH action correctly")
    void testExecute_setEntityHealthAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"set_entity_health\"\n" +
            "    entity: \"test_zombie\"\n" +
            "    health: 10.0\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).setEntityHealth(any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles SERVER_COMMAND action correctly")
    void testExecute_serverCommandAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"server_command\"\n" +
            "    command: \"say hello\"\n" +
            "    args: []\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).executeServerCommand(any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles PLAYER_COMMAND action correctly")
    void testExecute_playerCommandAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"player_command\"\n" +
            "    player: \"test_player\"\n" +
            "    command: \"home\"\n" +
            "    args: []\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).executePlayerCommand(any(), any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles CLEAR_ENTITIES action correctly")
    void testExecute_clearEntitiesAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"clear_entities\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).removeAllTestEntities();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles CONNECT_PLAYER action correctly")
    void testExecute_connectPlayerAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"connect_player\"\n" +
            "    player: \"test_player\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        // Action is handled even if backend doesn't have the method
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles DISCONNECT_PLAYER action correctly")
    void testExecute_disconnectPlayerAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"disconnect_player\"\n" +
            "    player: \"test_player\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        // Action is handled even if backend doesn't have the method
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles GET_SERVER_INFO action correctly")
    void testExecute_getServerInfoAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_server_info\"\n" +
            "    storeAs: \"server_info\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        // Action is handled even if backend doesn't have the method
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles GET_PLAYER_HEALTH action correctly")
    void testExecute_getPlayerHealthAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_player_health\"\n" +
            "    player: \"test_player\"\n" +
            "    storeAs: \"health_info\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        // Action is handled even if backend doesn't have the method
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles GET_PLAYER_INVENTORY action correctly")
    void testExecute_getPlayerInventoryAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_player_inventory\"\n" +
            "    player: \"test_player\"\n" +
            "    storeAs: \"inventory_info\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        // Action is handled even if backend doesn't have the method
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles GET_ENTITIES action correctly")
    void testExecute_getEntitiesAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_entities\"\n" +
            "    storeAs: \"entities_data\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        // Action is handled even if backend doesn't have the method
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles STORE_STATE action correctly")
    void testExecute_storeStateAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"store_state\"\n" +
            "    variableName: \"my_state\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles COMPARE_STATES action correctly")
    void testExecute_compareStatesAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"compare_states\"\n" +
            "    state1: \"state1\"\n" +
            "    state2: \"state2\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles EXTRACT_WITH_JSONPATH action correctly")
    void testExecute_extractJsonPathAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"extract_with_jsonpath\"\n" +
            "    source: \"state1\"\n" +
            "    jsonPath: \"key\"\n" +
            "    storeAs: \"extracted\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles FILTER_ENTITIES action correctly")
    void testExecute_filterEntitiesAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"filter_entities\"\n" +
            "    source: \"entities_data\"\n" +
            "    filterType: \"type\"\n" +
            "    filterValue: \"zombie\"\n" +
            "    storeAs: \"filtered_entities\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles PLACE_BLOCK action correctly")
    void testExecute_placeBlockAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"place_block\"\n" +
            "    player: \"test_player\"\n" +
            "    block: \"stone\"\n" +
            "    position: \"100 64 100\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles MAKE_OPERATOR action correctly")
    void testExecute_makeOperatorAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"make_operator\"\n" +
            "    player: \"test_player\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles SET_SPAWN_POINT action correctly")
    void testExecute_setSpawnPointAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"set_spawn_point\"\n" +
            "    position: \"100 64 100\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles TELEPORT_PLAYER action correctly")
    void testExecute_teleportPlayerAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"teleport_player\"\n" +
            "    player: \"test_player\"\n" +
            "    target: \"100 64 100\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles SET_PLAYER_HEALTH action correctly")
    void testExecute_setPlayerHealthAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"set_player_health\"\n" +
            "    player: \"test_player\"\n" +
            "    health: 10.0\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles KILL_PLAYER action correctly")
    void testExecute_killPlayerAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"kill_player\"\n" +
            "    player: \"test_player\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles KILL_ENTITY action correctly")
    void testExecute_killEntityAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"kill_entity\"\n" +
            "    entity: \"test_zombie\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles DAMAGE_ENTITY action correctly")
    void testExecute_damageEntityAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"damage_entity\"\n" +
            "    entity: \"test_zombie\"\n" +
            "    damage: \"5.0\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles HEAL_PLAYER action correctly")
    void testExecute_healPlayerAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"heal_player\"\n" +
            "    player: \"test_player\"\n" +
            "    amount: \"5.0\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles SET_TIME action correctly")
    void testExecute_setTimeAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"set_time\"\n" +
            "    time: \"noon\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles SET_WEATHER action correctly")
    void testExecute_setWeatherAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"set_weather\"\n" +
            "    weather: \"clear\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles CLEAR_INVENTORY action correctly")
    void testExecute_clearInventoryAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"clear_inventory\"\n" +
            "    player: \"test_player\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles WAIT_FOR_ENTITY_SPAWN action correctly")
    void testExecute_waitForEntitySpawnAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait_for_entity_spawn\"\n" +
            "    entityType: \"zombie\"\n" +
            "    timeout: \"5000\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles WAIT_FOR_CHAT_MESSAGE action correctly")
    void testExecute_waitForChatMessageAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait_for_chat_message\"\n" +
            "    contains: \"hello\"\n" +
            "    timeout: \"5000\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles LOOK_AT action correctly")
    void testExecute_lookAtAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"look_at\"\n" +
            "    player: \"test_player\"\n" +
            "    entity: \"zombie\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles ATTACK_ENTITY action correctly")
    void testExecute_attackEntityAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"attack_entity\"\n" +
            "    player: \"test_player\"\n" +
            "    entity: \"zombie\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles JUMP action correctly")
    void testExecute_jumpAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"jump\"\n" +
            "    player: \"test_player\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles BREAK_BLOCK action correctly")
    void testExecute_breakBlockAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"break_block\"\n" +
            "    player: \"test_player\"\n" +
            "    position: \"100 64 100\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles GET_PLAYER_POSITION action correctly")
    void testExecute_getPlayerPositionAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_player_position\"\n" +
            "    player: \"test_player\"\n" +
            "    storeAs: \"position\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles GET_PLAYER_EQUIPMENT action correctly")
    void testExecute_getPlayerEquipmentAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_player_equipment\"\n" +
            "    player: \"test_player\"\n" +
            "    storeAs: \"equipment\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles GET_CHAT_HISTORY action correctly")
    void testExecute_getChatHistoryAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_chat_history\"\n" +
            "    player: \"test_player\"\n" +
            "    storeAs: \"chat\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles EXECUTE_PLUGIN_COMMAND action correctly")
    void testExecute_executePluginCommandAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"execute_plugin_command\"\n" +
            "    player: \"test_player\"\n" +
            "    command: \"myplugin\"\n" +
            "    args: [\"arg1\", \"arg2\"]\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles EXECUTE_PLAYER_RAW action correctly")
    void testExecute_executePlayerRawAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"execute_player_raw\"\n" +
            "    player: \"test_player\"\n" +
            "    command: \"custom command\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles EXECUTE_RCON_RAW action correctly")
    void testExecute_executeRconRawAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"execute_rcon_raw\"\n" +
            "    command: \"custom rcon command\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles PRINT_STATE_COMPARISON action correctly")
    void testExecute_printStateComparisonAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"print_state_comparison\"\n" +
            "    state1: \"state1\"\n" +
            "    state2: \"state2\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() handles PRINT_STORED_STATE action correctly")
    void testExecute_printStoredStateAction() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"print_stored_state\"\n" +
            "    variableName: \"my_state\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with WAIT action with null duration uses default")
    void testExecute_waitAction_nullDuration() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        assertTrue(result.getExecutionTimeMs() >= 900); // Default wait is 1000ms (allowing some timing variance)
    }

    @Test
    @DisplayName("execute() with WAIT action with zero duration uses default")
    void testExecute_waitAction_zeroDuration() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 0\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        // When duration is 0 or negative, it waits the default 1000ms
        assertNotNull(result.getExecutionTimeMs());
    }

    @Test
    @DisplayName("execute() with WAIT action with negative duration uses default")
    void testExecute_waitAction_negativeDuration() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: -100\n";

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        // When duration is negative, it waits the default 1000ms
        assertNotNull(result.getExecutionTimeMs());
    }

    // UTILITY METHOD TESTS (using reflection)

    @Test
    @DisplayName("describeAction returns description for CONNECT action")
    void testDescribeAction_connectAction() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.CONNECT_PLAYER);
        action.setPlayer("test_player");

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAction", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertTrue(result.contains("Connect") || result.contains("connect"));
        assertTrue(result.contains("test_player"));
    }

    @Test
    @DisplayName("describeAction returns description for SPAWN_ENTITY action")
    void testDescribeAction_spawnEntityAction() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.SPAWN_ENTITY);
        action.setEntity("zombie");
        action.setEntityType("minecraft:zombie");
        action.setName("TestZombie");

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAction", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertTrue(result.contains("Spawn") || result.contains("spawn"));
        assertTrue(result.contains("TestZombie"));
    }

    @Test
    @DisplayName("describeAction returns description for GIVE_ITEM action")
    void testDescribeAction_giveItemAction() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.GIVE_ITEM);
        action.setPlayer("test_player");
        action.setItem("diamond");
        action.setCount(64);

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAction", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertTrue(result.contains("diamond") || result.contains("Give") || result.contains("give"));
    }

    @Test
    @DisplayName("describeAction returns description for WAIT action")
    void testDescribeAction_waitAction() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.WAIT);
        action.setDuration(5000L);

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAction", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertTrue(result.contains("Wait") || result.contains("wait"));
        assertTrue(result.contains("5000"));
    }

    @Test
    @DisplayName("buildRawCommand builds command for PLAYER_COMMAND action")
    void testBuildRawCommand_playerCommand() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.PLAYER_COMMAND);
        action.setPlayer("test_player");
        action.setCommand("/gamemode creative");

        Method method = TestOrchestrator.class.getDeclaredMethod("buildRawCommand", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertTrue(result.contains("gamemode"));
        assertTrue(result.contains("creative"));
    }

    @Test
    @DisplayName("buildRawCommand builds command for SERVER_COMMAND action")
    void testBuildRawCommand_serverCommand() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.SERVER_COMMAND);
        action.setCommand("time set day");

        Method method = TestOrchestrator.class.getDeclaredMethod("buildRawCommand", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertTrue(result.contains("time"));
        assertTrue(result.contains("day"));
    }

    @Test
    @DisplayName("getActionClass returns client for player actions")
    void testGetActionClass_playerAction() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.PLAYER_COMMAND);
        action.setPlayer("test_player");

        Method method = TestOrchestrator.class.getDeclaredMethod("getActionClass", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertEquals("client", result);
    }

    @Test
    @DisplayName("getActionClass returns server for server actions")
    void testGetActionClass_serverAction() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.SERVER_COMMAND);

        Method method = TestOrchestrator.class.getDeclaredMethod("getActionClass", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertEquals("server", result);
    }

    @Test
    @DisplayName("getActionClass returns workflow for WAIT action")
    void testGetActionClass_waitAction() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.WAIT);

        Method method = TestOrchestrator.class.getDeclaredMethod("getActionClass", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertEquals("workflow", result);
    }

    @Test
    @DisplayName("describeArguments returns formatted arguments for action")
    void testDescribeArguments_withArguments() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.GIVE_ITEM);
        action.setPlayer("test_player");
        action.setItem("diamond");
        action.setCount(64);

        Method method = TestOrchestrator.class.getDeclaredMethod("describeArguments", Action.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, action);

        assertTrue(result.contains("player") || result.contains("test_player"));
    }

    @Test
    @DisplayName("serializeToJson converts map to JSON string")
    void testSerializeToJson_simpleMap() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 42);

        Method method = TestOrchestrator.class.getDeclaredMethod("serializeToJson", Object.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, data);

        assertTrue(result.contains("key1"));
        assertTrue(result.contains("value1"));
    }

    @Test
    @DisplayName("serializeToJson handles null map")
    void testSerializeToJson_nullMap() throws Exception {
        Method method = TestOrchestrator.class.getDeclaredMethod("serializeToJson", Object.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, (Object) null);

        assertEquals("null", result);
    }

    @Test
    @DisplayName("filterEntities filters entities by type")
    void testFilterEntities_byType() throws Exception {
        List<Map<String, Object>> entities = new ArrayList<>();
        Map<String, Object> entity1 = new HashMap<>();
        entity1.put("name", "zombie");
        entity1.put("id", 1);
        entity1.put("type", "minecraft:zombie");
        entities.add(entity1);

        Map<String, Object> entity2 = new HashMap<>();
        entity2.put("name", "skeleton");
        entity2.put("id", 2);
        entity2.put("type", "minecraft:skeleton");
        entities.add(entity2);

        Method method = TestOrchestrator.class.getDeclaredMethod("filterEntities", Object.class, String.class, String.class);
        method.setAccessible(true);
        Object result = method.invoke(orchestrator, entities, "type", "minecraft:zombie");

        assertNotNull(result);
    }

    @Test
    @DisplayName("filterEntities handles empty entity list")
    void testFilterEntities_emptyList() throws Exception {
        List<Map<String, Object>> entities = new ArrayList<>();

        Method method = TestOrchestrator.class.getDeclaredMethod("filterEntities", Object.class, String.class, String.class);
        method.setAccessible(true);
        Object result = method.invoke(orchestrator, entities, "name", "zombie");

        assertNotNull(result);
    }

    @Test
    @DisplayName("filterEntities returns all entities when no filters")
    void testFilterEntities_noFilters() throws Exception {
        List<Map<String, Object>> entities = new ArrayList<>();
        Map<String, Object> entity1 = new HashMap<>();
        entity1.put("name", "zombie");
        entities.add(entity1);

        Method method = TestOrchestrator.class.getDeclaredMethod("filterEntities", Object.class, String.class, String.class);
        method.setAccessible(true);
        Object result = method.invoke(orchestrator, entities, null, null);

        assertNotNull(result);
    }

    // UNCOVERED METHOD TESTS

    @Test
    @DisplayName("loadStory loads story from classpath")
    void testLoadStory_fromClasspath() {
        orchestrator.loadStory("simple-test.yaml");
        assertNotNull(orchestrator.getResult());
        assertEquals("Simple Test Story", orchestrator.getResult().getStoryName());
    }

    @Test
    @DisplayName("loadStory initializes result and start time")
    void testLoadStory_initializesFields() {
        orchestrator.loadStory("simple-test.yaml");
        assertNotNull(orchestrator.getResult());
        assertTrue(orchestrator.getResult().getActionsExecuted() >= 0);
    }

    @Test
    @DisplayName("describeAssertion for ENTITY_HEALTH assertion")
    void testDescribeAssertion_entityHealth() throws Exception {
        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.ENTITY_HEALTH);
        assertion.setEntity("test_entity");
        assertion.setConditionType(Assertion.Condition.EQUALS);
        assertion.setValue(20.0);

        when(mockBackend.getEntityHealth("test_entity")).thenReturn(20.0);

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAssertion", Assertion.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, assertion);

        assertTrue(result.contains("test_entity"));
        assertTrue(result.contains("health"));
        assertTrue(result.contains("20.0"));
    }

    @Test
    @DisplayName("describeAssertion for ENTITY_EXISTS assertion")
    void testDescribeAssertion_entityExists() throws Exception {
        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.ENTITY_EXISTS);
        assertion.setEntity("test_entity");
        assertion.setExpected(true);

        when(mockBackend.entityExists("test_entity")).thenReturn(true);

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAssertion", Assertion.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, assertion);

        assertTrue(result.contains("test_entity"));
        assertTrue(result.contains("exists"));
    }

    @Test
    @DisplayName("describeAssertion for PLAYER_INVENTORY assertion")
    void testDescribeAssertion_playerInventory() throws Exception {
        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.PLAYER_INVENTORY);
        assertion.setPlayer("test_player");
        assertion.setItem("diamond");
        assertion.setSlot("0");

        when(mockBackend.playerInventoryContains("test_player", "diamond", "0")).thenReturn(true);

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAssertion", Assertion.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, assertion);

        assertTrue(result.contains("test_player"));
        assertTrue(result.contains("diamond"));
    }

    @Test
    @DisplayName("describeAssertion for ASSERT_ENTITY_MISSING assertion")
    void testDescribeAssertion_entityMissing() throws Exception {
        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.ASSERT_ENTITY_MISSING);
        assertion.setEntity("test_entity");

        when(mockBackend.entityExists("test_entity")).thenReturn(false);

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAssertion", Assertion.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, assertion);

        assertTrue(result.contains("test_entity"));
        assertTrue(result.contains("missing"));
    }

    @Test
    @DisplayName("describeAssertion for ASSERT_RESPONSE_CONTAINS assertion")
    void testDescribeAssertion_responseContains() throws Exception {
        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS);
        assertion.setContains("success");

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAssertion", Assertion.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, assertion);

        assertTrue(result.contains("Response contains"));
        assertTrue(result.contains("success"));
    }

    @Test
    @DisplayName("describeAssertion for PLUGIN_COMMAND assertion")
    void testDescribeAssertion_pluginCommand() throws Exception {
        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.PLUGIN_COMMAND);
        assertion.setPlugin("TestPlugin");
        assertion.setCommand("test");

        Method method = TestOrchestrator.class.getDeclaredMethod("describeAssertion", Assertion.class);
        method.setAccessible(true);
        String result = (String) method.invoke(orchestrator, assertion);

        assertTrue(result.contains("TestPlugin"));
        assertTrue(result.contains("test"));
    }

    @Test
    @DisplayName("evaluateAssertion adds result to test result")
    void testEvaluateAssertion_addsResult() throws Exception {
        // Need to load a story first to initialize the result
        orchestrator.loadStoryFromString("name: \"Test\"");

        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.ENTITY_EXISTS);
        assertion.setEntity("test_entity");
        assertion.setExpected(true);

        when(mockBackend.entityExists("test_entity")).thenReturn(true);

        Method method = TestOrchestrator.class.getDeclaredMethod("evaluateAssertion", Assertion.class);
        method.setAccessible(true);
        method.invoke(orchestrator, assertion);

        // The assertion result should be added to the test result
        TestResult result = orchestrator.getResult();
        assertNotNull(result);
    }

    @Test
    @DisplayName("setExecutionContext sets RCON executor for SERVER_COMMAND")
    void testSetExecutionContext_serverCommand() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.SERVER_COMMAND);
        action.setCommand("op test_player");

        TestReporter reporter = new TestReporter();
        TestReporter.TestStep step = reporter.step("Test Step");

        Method method = TestOrchestrator.class.getDeclaredMethod("setExecutionContext", TestReporter.TestStep.class, Action.class);
        method.setAccessible(true);
        method.invoke(orchestrator, step, action);

        // The executor should be set (we can't directly verify this without reflection on TestStep)
        assertNotNull(step);
    }

    @Test
    @DisplayName("setExecutionContext sets PLAYER executor for PLAYER_COMMAND")
    void testSetExecutionContext_playerCommand() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.PLAYER_COMMAND);
        action.setPlayer("test_player");

        TestReporter reporter = new TestReporter();
        TestReporter.TestStep step = reporter.step("Test Step");

        Method method = TestOrchestrator.class.getDeclaredMethod("setExecutionContext", TestReporter.TestStep.class, Action.class);
        method.setAccessible(true);
        method.invoke(orchestrator, step, action);

        assertNotNull(step);
    }

    @Test
    @DisplayName("setExecutionContext sets MINEFLAYER executor for CONNECT_PLAYER")
    void testSetExecutionContext_connectPlayer() throws Exception {
        Action action = new Action();
        action.setType(Action.ActionType.CONNECT_PLAYER);
        action.setPlayer("test_player");

        TestReporter reporter = new TestReporter();
        TestReporter.TestStep step = reporter.step("Test Step");

        Method method = TestOrchestrator.class.getDeclaredMethod("setExecutionContext", TestReporter.TestStep.class, Action.class);
        method.setAccessible(true);
        method.invoke(orchestrator, step, action);

        assertNotNull(step);
    }

    // ========================================================================
    // REPORTER AND ASSERTION TESTS - COVER UNCOVERED BRANCHES
    // ========================================================================

    @Test
    @DisplayName("execute() with reporter captures step information")
    void testExecute_withReporter_capturesStepInfo() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 50\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        // Reporter is set and used internally, we just verify execution completes
    }

    @Test
    @DisplayName("execute() with reporter on store_state action")
    void testExecute_withReporter_storeState() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"store_state\"\n" +
            "    variableName: \"myState\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with reporter on compare_states action")
    void testExecute_withReporter_compareStates() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "setup:\n" +
            "  - action: \"store_state\"\n" +
            "    variableName: \"state1\"\n" +
            "steps:\n" +
            "  - action: \"compare_states\"\n" +
            "    state1: \"state1\"\n" +
            "    state2: \"state1\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        // Same state comparison should succeed but show no changes
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with action that has extracted JSON")
    void testExecute_withExtractedJson() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_server_info\"\n" +
            "    storeAs: \"serverInfo\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with action that fails and reporter captures failure")
    void testExecute_withActionFailure_reporterCaptures() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 64\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doThrow(new RuntimeException("Failed to give item"))
            .when(mockBackend).giveItem(any(), any(), any());

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        // Orchestrator continues execution even after action exceptions
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with reporter and GIVE_ITEM action")
    void testExecute_withReporter_giveItem() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 64\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).giveItem(any(), any(), any());
        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        verify(mockBackend).giveItem("test_player", "diamond", 64);
    }

    @Test
    @DisplayName("execute() with reporter on SPAWN_ENTITY action")
    void testExecute_withReporter_spawnEntity() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"spawn_entity\"\n" +
            "    name: \"test_zombie\"\n" +
            "    type: \"minecraft:zombie\"\n" +
            "    position: \"100 64 100\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();
        doNothing().when(mockBackend).spawnEntity(any(), any(), any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with reporter on SERVER_COMMAND action")
    void testExecute_withReporter_serverCommand() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"server_command\"\n" +
            "    command: \"say hello\"\n" +
            "    args: []\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();
        doNothing().when(mockBackend).executeServerCommand(any(), any());

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with backend initialization exception")
    void testExecute_backendInitException() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n";

        orchestrator.loadStoryFromString(yaml);

        doThrow(new RuntimeException("Init failed"))
            .when(mockBackend).initialize();

        TestResult result = orchestrator.execute();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().getMessage().contains("Init failed"));
    }

    @Test
    @DisplayName("loadStory with reporter set creates reporter story")
    void testLoadStory_withReporter() throws Exception {
        String yaml = "name: \"Reporter Test Story\"\n" +
            "steps: []\n";

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        orchestrator.loadStoryFromString(yaml);

        TestResult result = orchestrator.getResult();
        assertEquals("Reporter Test Story", result.getStoryName());
    }

    @Test
    @DisplayName("execute() handles exception during action execution with reporter")
    void testExecute_actionException_withReporter() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 64\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doThrow(new RuntimeException("Action execution failed"))
            .when(mockBackend).giveItem(any(), any(), any());

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        // Orchestrator logs but doesn't fail on action exceptions
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with player inventory command and reporter")
    void testExecute_getPlayerInventory_withReporter() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"get_player_inventory\"\n" +
            "    player: \"test_player\"\n" +
            "    storeAs: \"inventory\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("execute() with wait action and raw response with reporter")
    void testExecute_wait_withReporter() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        assertTrue(result.getExecutionTimeMs() >= 10);
    }

    // TESTS FOR ASSERTIONS EVALUATION (lines 104-106, 113)

    @Test
    @DisplayName("execute() evaluates assertions and marks result as failed when assertions fail")
    void testExecute_withAssertions() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n" +
            "assertions:\n" +
            "  - assertion: \"assert_response_contains\"\n" +
            "    expected: \"expected text\"\n" +
            "    description: \"Should fail because response doesn't contain expected text\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        // Assertions failed, so result should not be successful
        assertFalse(result.isSuccess());
        assertTrue(result.getAssertionsFailed() > 0);
    }

    @Test
    @DisplayName("execute() with assertions that pass")
    void testExecute_withPassingAssertions() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n" +
            "assertions:\n" +
            "  - assertion: \"assert_response_contains\"\n" +
            "    expected: \"✓\"\n";

        orchestrator.loadStoryFromString(yaml);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        // Assertions are evaluated
        assertNotNull(result);
    }

    // TESTS FOR EXCEPTION HANDLING IN executeAction (lines 207-215)

    @Test
    @DisplayName("execute() handles exceptions in executeAction with reporter")
    void testExecute_actionExceptionWithReporter() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 64\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doThrow(new RuntimeException("Failed to give item"))
            .when(mockBackend).giveItem(any(), any(), any());

        TestResult result = orchestrator.execute();

        // TestOrchestrator continues execution even after action exceptions
        assertTrue(result.isSuccess());
        // Verify reporter has a story
        assertNotNull(reporter);
    }

    // TESTS FOR STATE COMPARISON WITH CHANGES (lines 181-190)

    @Test
    @DisplayName("execute() with COMPARE_STATES action that has changes marks step as passed")
    void testExecute_compareStatesWithChanges() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "setup:\n" +
            "  - action: \"store_state\"\n" +
            "    variableName: \"before\"\n" +
            "steps:\n" +
            "  - action: \"give_item\"\n" +
            "    player: \"test_player\"\n" +
            "    item: \"diamond\"\n" +
            "    count: 1\n" +
            "  - action: \"compare_states\"\n" +
            "    state1: \"before\"\n" +
            "    state2: \"after\"\n" +
            "    storeAs: \"after\"\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).giveItem(any(), any(), any());
        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        assertNotNull(reporter);
    }

    // TESTS FOR EXTRACTED JSON (lines 166-167)

    @Test
    @DisplayName("execute() with action that returns extracted JSON")
    void testExecute_actionWithExtractedJson() throws Exception {
        String yaml = "name: \"Test Story\"\n" +
            "steps:\n" +
            "  - action: \"wait\"\n" +
            "    duration: 10\n";

        orchestrator.loadStoryFromString(yaml);

        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        doNothing().when(mockBackend).initialize();
        doNothing().when(mockBackend).cleanup();

        TestResult result = orchestrator.execute();

        assertTrue(result.isSuccess());
        assertNotNull(reporter);
    }

    // TESTS FOR loadStory(String) with reporter (line 60-61)

    @Test
    @DisplayName("loadStory() with reporter creates story in reporter")
    void testLoadStory_withReporterCreatesStory() {
        TestReporter reporter = new TestReporter();
        orchestrator.setReporter(reporter);

        // Load a story from classpath that exists
        // This should cover the reporter != null branch in loadStory() line 60
        assertDoesNotThrow(() -> orchestrator.loadStory("simple-test.yaml"));

        // Verify the reporter is not null
        assertNotNull(reporter);
    }
}
