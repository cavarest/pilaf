package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.MineflayerBackend;
import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayerActionExecutor.
 */
@DisplayName("PlayerActionExecutor Tests")
class PlayerActionExecutorTest {

    private PlayerActionExecutor executor;
    @Mock
    private PilafBackend mockBackend;
    @Mock
    private MineflayerBackend mockMineflayerBackend;
    @Mock
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new PlayerActionExecutor();
    }

    @Test
    @DisplayName("getName() returns correct executor name")
    void testGetName() {
        assertEquals("PlayerExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes() returns correct action types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.CONNECT_PLAYER));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.DISCONNECT_PLAYER));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.MAKE_OPERATOR));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_PLAYER_POSITION));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_PLAYER_HEALTH));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.MOVE_PLAYER));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.TELEPORT_PLAYER));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SET_PLAYER_HEALTH));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.KILL_PLAYER));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SET_SPAWN_POINT));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GAMEMODE_CHANGE));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.PLAYER_COMMAND));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.EXECUTE_PLAYER_COMMAND));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.EXECUTE_PLAYER_RAW));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SEND_CHAT_MESSAGE));
        assertEquals(15, executor.getSupportedTypes().size());
    }

    @Test
    @DisplayName("execute() with CONNECT_PLAYER returns success")
    void testExecuteConnectPlayer() throws Exception {
        Action action = new Action(Action.ActionType.CONNECT_PLAYER);
        action.setPlayer("test_player");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("test_player"));
        verify(mockMineflayerBackend).connectPlayer("test_player");
    }

    @Test
    @DisplayName("execute() with DISCONNECT_PLAYER returns success")
    void testExecuteDisconnectPlayer() throws Exception {
        Action action = new Action(Action.ActionType.DISCONNECT_PLAYER);
        action.setPlayer("test_player");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).disconnectPlayer("test_player");
    }

    @Test
    @DisplayName("execute() with MAKE_OPERATOR returns success")
    void testExecuteMakeOperator() throws Exception {
        Action action = new Action(Action.ActionType.MAKE_OPERATOR);
        action.setPlayer("test_player");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).makeOperator("test_player");
    }

    @Test
    @DisplayName("execute() with GET_PLAYER_POSITION returns position")
    void testExecuteGetPlayerPosition() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_POSITION);
        action.setPlayer("test_player");

        Map<String, Object> position = Map.of("x", 100.0, "y", 64.0, "z", 200.0);
        when(mockMineflayerBackend.getPlayerPosition("test_player")).thenReturn(position);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).getPlayerPosition("test_player");
    }

    @Test
    @DisplayName("execute() with GET_PLAYER_POSITION stores position when storeAs is set")
    void testExecuteGetPlayerPosition_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_POSITION);
        action.setPlayer("test_player");
        action.setStoreAs("position_value");

        Map<String, Object> position = Map.of("x", 50.0, "y", 70.0, "z", 100.0);
        when(mockMineflayerBackend.getPlayerPosition("test_player")).thenReturn(position);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("position_value"), eq(position));
    }

    @Test
    @DisplayName("execute() with GET_PLAYER_HEALTH returns health")
    void testExecuteGetPlayerHealth() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_HEALTH);
        action.setPlayer("test_player");

        when(mockMineflayerBackend.getPlayerHealthAsDouble("test_player")).thenReturn(20.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("health="));
        verify(mockMineflayerBackend).getPlayerHealthAsDouble("test_player");
    }

    @Test
    @DisplayName("execute() with GET_PLAYER_HEALTH stores health when storeAs is set")
    void testExecuteGetPlayerHealth_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_HEALTH);
        action.setPlayer("test_player");
        action.setStoreAs("health_value");

        when(mockMineflayerBackend.getPlayerHealthAsDouble("test_player")).thenReturn(15.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("health_value"), eq(15.0));
    }

    @Test
    @DisplayName("execute() with MOVE_PLAYER moves player")
    void testExecuteMovePlayer() {
        Action action = new Action(Action.ActionType.MOVE_PLAYER);
        action.setPlayer("test_player");
        action.setDestination("100 64 200");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).movePlayer("test_player", "destination", "100 64 200");
    }

    @Test
    @DisplayName("execute() with TELEPORT_PLAYER teleports to destination")
    void testExecuteTeleportPlayer_withDestination() throws Exception {
        Action action = new Action(Action.ActionType.TELEPORT_PLAYER);
        action.setPlayer("test_player");
        action.setDestination("100 64 200");

        when(mockMineflayerBackend.executeRconWithCapture("tp test_player 100 64 200"))
            .thenReturn("Teleported");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("tp test_player 100 64 200");
    }

    @Test
    @DisplayName("execute() with TELEPORT_PLAYER teleports to location")
    void testExecuteTeleportPlayer_withLocation() throws Exception {
        Action action = new Action(Action.ActionType.TELEPORT_PLAYER);
        action.setPlayer("test_player");
        action.setLocation(List.of(100.0, 64.0, 200.0));

        when(mockMineflayerBackend.executeRconWithCapture("tp test_player 100 64 200"))
            .thenReturn("Teleported");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("tp test_player 100 64 200");
    }

    @Test
    @DisplayName("execute() with SET_PLAYER_HEALTH sets health")
    void testExecuteSetPlayerHealth() throws Exception {
        Action action = new Action(Action.ActionType.SET_PLAYER_HEALTH);
        action.setPlayer("test_player");
        action.setValue(20.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("attribute minecraft:generic.max_health base set 20.0");
        verify(mockMineflayerBackend).executeRconWithCapture("heal test_player");
    }

    @Test
    @DisplayName("execute() with KILL_PLAYER kills player")
    void testExecuteKillPlayer() throws Exception {
        Action action = new Action(Action.ActionType.KILL_PLAYER);
        action.setPlayer("test_player");

        when(mockMineflayerBackend.executeRconWithCapture("kill test_player"))
            .thenReturn("Killed test_player");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("kill test_player");
    }

    @Test
    @DisplayName("execute() with SET_SPAWN_POINT sets spawn point")
    void testExecuteSetSpawnPoint() throws Exception {
        Action action = new Action(Action.ActionType.SET_SPAWN_POINT);
        action.setPlayer("test_player");
        action.setLocation(List.of(100.0, 64.0, 200.0));

        when(mockMineflayerBackend.executeRconWithCapture("spawnpoint test_player 100 64 200"))
            .thenReturn("Set spawn point");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("spawnpoint test_player 100 64 200");
    }

    @Test
    @DisplayName("execute() with GAMEMODE_CHANGE changes gamemode")
    void testExecuteGamemodeChange() throws Exception {
        Action action = new Action(Action.ActionType.GAMEMODE_CHANGE);
        action.setPlayer("test_player");
        action.setEntity("creative");

        when(mockMineflayerBackend.executeRconWithCapture("gamemode creative test_player"))
            .thenReturn("Gamemode changed");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("gamemode creative test_player");
    }

    @Test
    @DisplayName("execute() with PLAYER_COMMAND executes command")
    void testExecutePlayerCommand() {
        Action action = new Action(Action.ActionType.PLAYER_COMMAND);
        action.setPlayer("test_player");
        action.setCommand("test");
        action.setArgs(List.of("arg1", "arg2"));

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).executePlayerCommand("test_player", "test", List.of("arg1", "arg2"));
    }

    @Test
    @DisplayName("execute() with EXECUTE_PLAYER_COMMAND executes command")
    void testExecuteExecutePlayerCommand() {
        Action action = new Action(Action.ActionType.EXECUTE_PLAYER_COMMAND);
        action.setPlayer("test_player");
        action.setCommand("myplugin:test");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).executePlayerCommand("test_player", "myplugin:test", Collections.emptyList());
    }

    @Test
    @DisplayName("execute() with EXECUTE_PLAYER_RAW executes raw command")
    void testExecuteExecutePlayerRaw() throws Exception {
        Action action = new Action(Action.ActionType.EXECUTE_PLAYER_RAW);
        action.setPlayer("test_player");
        action.setCommand("/raw command");

        when(mockMineflayerBackend.executePlayerCommandRaw("test_player", "/raw command"))
            .thenReturn("Raw response");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executePlayerCommandRaw("test_player", "/raw command");
    }

    @Test
    @DisplayName("execute() with SEND_CHAT_MESSAGE sends chat")
    void testExecuteSendChatMessage() {
        Action action = new Action(Action.ActionType.SEND_CHAT_MESSAGE);
        action.setPlayer("test_player");
        action.setMessage("Hello world");

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).sendChat("test_player", "Hello world");
    }

    @Test
    @DisplayName("execute() handles exception gracefully")
    void testExecute_handlesException() throws Exception {
        Action action = new Action(Action.ActionType.CONNECT_PLAYER);
        action.setPlayer("test_player");

        doThrow(new RuntimeException("Connection failed"))
            .when(mockMineflayerBackend).connectPlayer(any());

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Connection failed"));
    }

    @Test
    @DisplayName("execute() returns failure for unsupported action type")
    void testExecute_unsupportedActionType() {
        Action action = new Action(Action.ActionType.WAIT);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unsupported action type"));
    }

    // ADDITIONAL COVERAGE TESTS

    @Test
    @DisplayName("execute() with CONNECT_PLAYER with null player name")
    void testExecuteConnect_nullPlayerName() throws Exception {
        Action action = new Action(Action.ActionType.CONNECT_PLAYER);
        action.setPlayer(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).connectPlayer(any());
    }

    @Test
    @DisplayName("execute() with DISCONNECT_PLAYER with null player name")
    void testExecuteDisconnect_nullPlayerName() throws Exception {
        Action action = new Action(Action.ActionType.DISCONNECT_PLAYER);
        action.setPlayer(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).disconnectPlayer(any());
    }

    @Test
    @DisplayName("execute() with MAKE_OPERATOR with null player name")
    void testExecuteMakeOperator_nullPlayerName() throws Exception {
        Action action = new Action(Action.ActionType.MAKE_OPERATOR);
        action.setPlayer(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        // When player is null, makeOperator(null) is called
        verify(mockMineflayerBackend).makeOperator(null);
    }

    @Test
    @DisplayName("execute() with GET_POSITION without storeAs")
    void testExecuteGetPosition_withoutStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_POSITION);
        action.setPlayer("test_player");
        // No storeAs set

        when(mockMineflayerBackend.getPlayerPosition("test_player"))
            .thenReturn(Map.of("x", 100, "y", 64, "z", 200));

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        // Should NOT call store
        verify(mockStateManager, never()).store(anyString(), any());
    }

    @Test
    @DisplayName("execute() with GET_HEALTH without storeAs")
    void testExecuteGetHealth_withoutStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_HEALTH);
        action.setPlayer("test_player");
        // No storeAs set

        when(mockMineflayerBackend.getPlayerHealthAsDouble("test_player")).thenReturn(20.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("health="));
        // Should NOT call store
        verify(mockStateManager, never()).store(anyString(), any());
    }

    @Test
    @DisplayName("execute() with GAMEMODE_CHANGE to survival")
    void testExecuteGamemodeChange_survival() throws Exception {
        Action action = new Action(Action.ActionType.GAMEMODE_CHANGE);
        action.setPlayer("test_player");
        action.setEntity("survival");

        when(mockMineflayerBackend.executeRconWithCapture("gamemode survival test_player"))
            .thenReturn("Gamemode changed");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("gamemode survival test_player");
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS - RCON BACKEND
    // ========================================================================

    @Test
    @DisplayName("execute() CONNECT_PLAYER with RCON backend")
    void testExecuteConnect_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.CONNECT_PLAYER);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).connectPlayer("test_player");
    }

    @Test
    @DisplayName("execute() DISCONNECT_PLAYER with RCON backend")
    void testExecuteDisconnect_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.DISCONNECT_PLAYER);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).disconnectPlayer("test_player");
    }

    @Test
    @DisplayName("execute() MAKE_OPERATOR with RCON backend")
    void testExecuteMakeOperator_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.MAKE_OPERATOR);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).makeOperator("test_player");
    }

    @Test
    @DisplayName("execute() GET_PLAYER_POSITION with RCON backend")
    void testExecuteGetPosition_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_POSITION);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        Map<String, Object> position = Map.of("x", 50.0, "y", 70.0, "z", 100.0);
        when(mockRcon.getPlayerPosition("test_player")).thenReturn(position);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getPlayerPosition("test_player");
    }

    @Test
    @DisplayName("execute() GET_PLAYER_HEALTH with RCON backend")
    void testExecuteGetHealth_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_HEALTH);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.getPlayerHealth("test_player")).thenReturn(18.0);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getPlayerHealth("test_player");
    }

    @Test
    @DisplayName("execute() GET_PLAYER_HEALTH with null health response")
    void testExecuteGetHealth_nullHealth() throws Exception {
        Action action = new Action(Action.ActionType.GET_PLAYER_HEALTH);
        action.setPlayer("offline_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        // RconBackend.getPlayerHealth returns Double, so use 0.0 for "no health" instead of null
        when(mockRcon.getPlayerHealth("offline_player")).thenReturn(0.0);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("health=0.0"));
    }

    @Test
    @DisplayName("execute() EXECUTE_PLAYER_RAW with RCON backend")
    void testExecuteExecutePlayerRaw_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.EXECUTE_PLAYER_RAW);
        action.setPlayer("test_player");
        action.setCommand("/raw command");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.executePlayerCommandRaw("test_player", "/raw command"))
            .thenReturn("Raw response from RCON");

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).executePlayerCommandRaw("test_player", "/raw command");
    }

    @Test
    @DisplayName("executeRcon() with RCON backend")
    void testExecuteRcon_withRconBackend() throws Exception {
        // Test executeRcon indirectly through GAMEMODE_CHANGE with RCON backend
        Action action = new Action(Action.ActionType.GAMEMODE_CHANGE);
        action.setPlayer("test_player");
        action.setEntity("creative");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.executeRconWithCapture("gamemode creative test_player"))
            .thenReturn("Gamemode changed to creative");

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).executeRconWithCapture("gamemode creative test_player");
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS - EDGE CASES
    // ========================================================================

    @Test
    @DisplayName("execute() TELEPORT_PLAYER with destination having fewer than 3 coords")
    void testExecuteTeleport_fewerCoords() throws Exception {
        Action action = new Action(Action.ActionType.TELEPORT_PLAYER);
        action.setPlayer("test_player");
        action.setDestination("100 64"); // Only 2 coords

        // When coords.length < 3, no coords are added to the command
        when(mockMineflayerBackend.executeRconWithCapture("tp test_player"))
            .thenReturn("Teleported");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("tp test_player");
    }

    @Test
    @DisplayName("execute() TELEPORT_PLAYER with null destination and null location")
    void testExecuteTeleport_noDestinationNoLocation() throws Exception {
        Action action = new Action(Action.ActionType.TELEPORT_PLAYER);
        action.setPlayer("test_player");
        // No destination or location set

        when(mockMineflayerBackend.executeRconWithCapture("tp test_player"))
            .thenReturn("Teleported");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("tp test_player");
    }

    @Test
    @DisplayName("execute() TELEPORT_PLAYER with location having fewer than 3 elements")
    void testExecuteTeleport_locationTooSmall() throws Exception {
        Action action = new Action(Action.ActionType.TELEPORT_PLAYER);
        action.setPlayer("test_player");
        action.setLocation(List.of(100.0, 64.0)); // Only 2 elements

        when(mockMineflayerBackend.executeRconWithCapture("tp test_player"))
            .thenReturn("Teleported");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("tp test_player");
    }

    @Test
    @DisplayName("execute() SET_SPAWN_POINT with null location")
    void testExecuteSetSpawnPoint_noLocation() throws Exception {
        Action action = new Action(Action.ActionType.SET_SPAWN_POINT);
        action.setPlayer("test_player");
        // No location set

        when(mockMineflayerBackend.executeRconWithCapture("spawnpoint test_player"))
            .thenReturn("Set spawn point");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("spawnpoint test_player");
    }

    @Test
    @DisplayName("execute() SET_SPAWN_POINT with location having fewer than 3 elements")
    void testExecuteSetSpawnPoint_locationTooSmall() throws Exception {
        Action action = new Action(Action.ActionType.SET_SPAWN_POINT);
        action.setPlayer("test_player");
        action.setLocation(List.of(100.0, 64.0)); // Only 2 elements

        when(mockMineflayerBackend.executeRconWithCapture("spawnpoint test_player"))
            .thenReturn("Set spawn point");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("spawnpoint test_player");
    }

    @Test
    @DisplayName("execute() GAMEMODE_CHANGE with null entity (defaults to survival)")
    void testExecuteGamemodeChange_nullEntity() throws Exception {
        Action action = new Action(Action.ActionType.GAMEMODE_CHANGE);
        action.setPlayer("test_player");
        // Entity not set, should default to "survival"

        when(mockMineflayerBackend.executeRconWithCapture("gamemode survival test_player"))
            .thenReturn("Gamemode changed");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("gamemode survival test_player");
    }

    @Test
    @DisplayName("execute() GAMEMODE_CHANGE with null RCON response")
    void testExecuteGamemodeChange_nullRconResponse() throws Exception {
        Action action = new Action(Action.ActionType.GAMEMODE_CHANGE);
        action.setPlayer("test_player");
        action.setEntity("adventure");

        when(mockMineflayerBackend.executeRconWithCapture("gamemode adventure test_player"))
            .thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("Gamemode changed"));
    }

    @Test
    @DisplayName("execute() PLAYER_COMMAND with null args (uses empty list)")
    void testExecutePlayerCommand_nullArgs() {
        Action action = new Action(Action.ActionType.PLAYER_COMMAND);
        action.setPlayer("test_player");
        action.setCommand("test");
        // Args not set, should use empty list

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).executePlayerCommand("test_player", "test", Collections.emptyList());
    }

    @Test
    @DisplayName("execute() EXECUTE_PLAYER_RAW with null response")
    void testExecuteExecutePlayerRaw_nullResponse() throws Exception {
        Action action = new Action(Action.ActionType.EXECUTE_PLAYER_RAW);
        action.setPlayer("test_player");
        action.setCommand("/raw command");

        when(mockMineflayerBackend.executePlayerCommandRaw("test_player", "/raw command"))
            .thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        // When response is null, returns success with empty string
        assertEquals("", result.getResponse());
    }
}
