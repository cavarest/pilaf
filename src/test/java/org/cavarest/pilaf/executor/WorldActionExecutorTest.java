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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorldActionExecutor.
 */
@DisplayName("WorldActionExecutor Tests")
class WorldActionExecutorTest {

    private WorldActionExecutor executor;
    @Mock
    private PilafBackend mockBackend;
    @Mock
    private MineflayerBackend mockMineflayerBackend;
    @Mock
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new WorldActionExecutor();
    }

    @Test
    @DisplayName("getName() returns correct executor name")
    void testGetName() {
        assertEquals("WorldExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes() returns correct action types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_WORLD_TIME));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_WEATHER));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SET_TIME));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SET_WEATHER));
        assertEquals(4, executor.getSupportedTypes().size());
    }

    @Test
    @DisplayName("execute() with GET_WORLD_TIME returns time response")
    void testExecuteGetWorldTime() throws Exception {
        Action action = new Action(Action.ActionType.GET_WORLD_TIME);
        when(mockMineflayerBackend.getWorldTime()).thenReturn(12345L);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("time="));
        assertTrue(result.getResponse().contains("12345"));
    }

    @Test
    @DisplayName("execute() with GET_WORLD_TIME stores time when storeAs is set")
    void testExecuteGetWorldTime_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_WORLD_TIME);
        action.setStoreAs("world_time");
        when(mockMineflayerBackend.getWorldTime()).thenReturn(12345L);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("world_time"), eq(12345L));
    }

    @Test
    @DisplayName("execute() with GET_WEATHER returns weather response")
    void testExecuteGetWeather() throws Exception {
        Action action = new Action(Action.ActionType.GET_WEATHER);
        when(mockMineflayerBackend.getWeather()).thenReturn("clear");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("weather="));
        assertTrue(result.getResponse().contains("clear"));
    }

    @Test
    @DisplayName("execute() with GET_WEATHER stores weather when storeAs is set")
    void testExecuteGetWeather_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_WEATHER);
        action.setStoreAs("current_weather");
        when(mockMineflayerBackend.getWeather()).thenReturn("rain");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("current_weather"), eq("rain"));
    }

    @Test
    @DisplayName("execute() with SET_TIME uses default 'day' when no entity specified")
    void testExecuteSetTime_default() throws Exception {
        Action action = new Action(Action.ActionType.SET_TIME);
        when(mockMineflayerBackend.executeRconWithCapture("time set day")).thenReturn("Set the time to day");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("day"));
        verify(mockMineflayerBackend).executeRconWithCapture("time set day");
    }

    @Test
    @DisplayName("execute() with SET_TIME uses entity value when specified")
    void testExecuteSetTime_withEntity() throws Exception {
        Action action = new Action(Action.ActionType.SET_TIME);
        action.setEntity("night");
        when(mockMineflayerBackend.executeRconWithCapture("time set night")).thenReturn("Set the time to night");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("night"));
        verify(mockMineflayerBackend).executeRconWithCapture("time set night");
    }

    @Test
    @DisplayName("execute() with SET_WEATHER uses default 'clear' when no entity specified")
    void testExecuteSetWeather_default() throws Exception {
        Action action = new Action(Action.ActionType.SET_WEATHER);
        when(mockMineflayerBackend.executeRconWithCapture("weather clear")).thenReturn("Weather changed to clear");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("clear"));
        verify(mockMineflayerBackend).executeRconWithCapture("weather clear");
    }

    @Test
    @DisplayName("execute() with SET_WEATHER uses entity value when specified")
    void testExecuteSetWeather_withEntity() throws Exception {
        Action action = new Action(Action.ActionType.SET_WEATHER);
        action.setEntity("thunder");
        when(mockMineflayerBackend.executeRconWithCapture("weather thunder")).thenReturn("Weather changed to thunder");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("thunder"));
        verify(mockMineflayerBackend).executeRconWithCapture("weather thunder");
    }

    @Test
    @DisplayName("execute() handles null weather response")
    void testExecuteGetWeather_nullResponse() throws Exception {
        Action action = new Action(Action.ActionType.GET_WEATHER);
        when(mockMineflayerBackend.getWeather()).thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("null"));
    }

    @Test
    @DisplayName("execute() returns failure for exception")
    void testExecute_handlesException() throws Exception {
        Action action = new Action(Action.ActionType.GET_WORLD_TIME);

        when(mockMineflayerBackend.getWorldTime())
            .thenThrow(new RuntimeException("Connection lost"));

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Connection lost"));
    }

    @Test
    @DisplayName("execute() returns failure for unsupported action type")
    void testExecute_unsupportedActionType() {
        Action action = new Action(Action.ActionType.WAIT);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unsupported action type"));
    }

    @Test
    @DisplayName("execute() with SET_TIME handles null RCON response")
    void testExecuteSetTime_nullRconResponse() throws Exception {
        Action action = new Action(Action.ActionType.SET_TIME);
        when(mockMineflayerBackend.executeRconWithCapture(any())).thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("day"));
    }

    @Test
    @DisplayName("execute() with SET_WEATHER handles null RCON response")
    void testExecuteSetWeather_nullRconResponse() throws Exception {
        Action action = new Action(Action.ActionType.SET_WEATHER);
        action.setEntity("rain");
        when(mockMineflayerBackend.executeRconWithCapture(any())).thenReturn(null);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("rain"));
    }

    @Test
    @DisplayName("execute() handles numeric time values")
    void testExecuteSetTime_numericValue() throws Exception {
        Action action = new Action(Action.ActionType.SET_TIME);
        action.setEntity("12000");
        when(mockMineflayerBackend.executeRconWithCapture("time set 12000")).thenReturn("Set time to 12000");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("time set 12000");
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS - RCON BACKEND
    // ========================================================================

    @Test
    @DisplayName("execute() GET_WORLD_TIME with RCON backend")
    void testExecuteGetWorldTime_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_WORLD_TIME);

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.getWorldTime()).thenReturn(18000L);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("time="));
        assertTrue(result.getResponse().contains("18000"));
        verify(mockRcon).getWorldTime();
    }

    @Test
    @DisplayName("execute() GET_WORLD_TIME with RCON backend and storeAs")
    void testExecuteGetWorldTime_withRconBackend_stores() throws Exception {
        Action action = new Action(Action.ActionType.GET_WORLD_TIME);
        action.setStoreAs("server_time");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.getWorldTime()).thenReturn(6000L);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("server_time"), eq(6000L));
    }

    @Test
    @DisplayName("execute() GET_WEATHER with RCON backend")
    void testExecuteGetWeather_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_WEATHER);

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.getWeather()).thenReturn("thunder");

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("weather=thunder"));
        verify(mockRcon).getWeather();
    }

    @Test
    @DisplayName("execute() GET_WEATHER with RCON backend and storeAs")
    void testExecuteGetWeather_withRconBackend_stores() throws Exception {
        Action action = new Action(Action.ActionType.GET_WEATHER);
        action.setStoreAs("weather_state");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.getWeather()).thenReturn("rain");

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("weather_state"), eq("rain"));
    }

    @Test
    @DisplayName("execute() SET_TIME with RCON backend")
    void testExecuteSetTime_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.SET_TIME);
        action.setEntity("noon");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.executeRconWithCapture("time set noon")).thenReturn("Time set to noon");

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("noon"));
        verify(mockRcon).executeRconWithCapture("time set noon");
    }

    @Test
    @DisplayName("execute() SET_WEATHER with RCON backend")
    void testExecuteSetWeather_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.SET_WEATHER);
        action.setEntity("snow");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.executeRconWithCapture("weather snow")).thenReturn("Weather set to snow");

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("snow"));
        verify(mockRcon).executeRconWithCapture("weather snow");
    }

    @Test
    @DisplayName("execute() with GET_WORLD_TIME returns null for unsupported backend")
    void testExecuteGetWorldTime_unsupportedBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_WORLD_TIME);

        // Using generic PilafBackend mock (not MineflayerBackend or RconBackend)
        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertEquals("null", result.getResponse());
    }

    @Test
    @DisplayName("execute() with GET_WEATHER returns null for unsupported backend")
    void testExecuteGetWeather_unsupportedBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_WEATHER);

        // Using generic PilafBackend mock (not MineflayerBackend or RconBackend)
        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertEquals("null", result.getResponse());
    }
}
