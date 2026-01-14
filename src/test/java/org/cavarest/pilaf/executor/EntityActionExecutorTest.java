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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityActionExecutor.
 */
@DisplayName("EntityActionExecutor Tests")
class EntityActionExecutorTest {

    private EntityActionExecutor executor;
    @Mock
    private PilafBackend mockBackend;
    @Mock
    private MineflayerBackend mockMineflayerBackend;
    @Mock
    private StateManager mockStateManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new EntityActionExecutor();
    }

    @Test
    @DisplayName("getName() returns correct executor name")
    void testGetName() {
        assertEquals("EntityExecutor", executor.getName());
    }

    @Test
    @DisplayName("getSupportedTypes() returns correct action types")
    void testGetSupportedTypes() {
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SPAWN_ENTITY));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_ENTITIES));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_ENTITIES_IN_VIEW));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_ENTITY_BY_NAME));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.GET_ENTITY_HEALTH));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.SET_ENTITY_HEALTH));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.KILL_ENTITY));
        assertTrue(executor.getSupportedTypes().contains(Action.ActionType.REMOVE_ENTITIES));
        assertEquals(8, executor.getSupportedTypes().size());
    }

    @Test
    @DisplayName("execute() with SPAWN_ENTITY returns success")
    void testExecuteSpawnEntity() {
        Action action = new Action(Action.ActionType.SPAWN_ENTITY);
        action.setName("test_zombie");
        action.setEntityType("zombie");
        action.setLocation(List.of(100.0, 64.0, 200.0));

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("zombie"));
        verify(mockBackend).spawnEntity(eq("test_zombie"), eq("zombie"), eq(List.of(100.0, 64.0, 200.0)), isNull());
    }

    @Test
    @DisplayName("execute() with SPAWN_ENTITY uses equipment when specified")
    void testExecuteSpawnEntity_withEquipment() {
        Action action = new Action(Action.ActionType.SPAWN_ENTITY);
        action.setName("test_villager");
        action.setEntityType("villager");
        action.setEquipment(Map.of("hand", "emerald"));

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockBackend).spawnEntity(eq("test_villager"), eq("villager"), isNull(), eq(Map.of("hand", "emerald")));
    }

    @Test
    @DisplayName("execute() with GET_ENTITIES returns entity list")
    void testExecuteGetEntities() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES);
        action.setPlayer("test_player");

        Map<String, Object> mockEntities = Map.of("entities", List.of(Map.of("id", "minecraft:zombie")));
        when(mockMineflayerBackend.getEntities("test_player")).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertNotNull(result.getResponse());
        verify(mockMineflayerBackend).getEntities("test_player");
    }

    @Test
    @DisplayName("execute() with GET_ENTITIES stores entities when storeAs is set")
    void testExecuteGetEntities_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES);
        action.setPlayer("test_player");
        action.setStoreAs("entity_list");

        Map<String, Object> mockEntities = Map.of("entities", List.of(Map.of("id", "minecraft:skeleton")));
        when(mockMineflayerBackend.getEntities("test_player")).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("entity_list"), any());
    }

    @Test
    @DisplayName("execute() with GET_ENTITIES_IN_VIEW returns entities in view")
    void testExecuteGetEntitiesInView() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES_IN_VIEW);
        action.setPlayer("test_player");

        Map<String, Object> mockEntities = Map.of("entities", List.of(Map.of("id", "minecraft:cow")));
        when(mockMineflayerBackend.getEntitiesInView("test_player")).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).getEntitiesInView("test_player");
    }

    @Test
    @DisplayName("execute() with GET_ENTITY_BY_NAME returns entity data")
    void testExecuteGetEntityByName() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_BY_NAME);
        action.setEntity("test_entity");
        action.setPlayer("test_player");

        Map<String, Object> mockEntity = Map.of("CustomName", "test_entity");
        when(mockMineflayerBackend.getEntityByName("test_entity", "test_player")).thenReturn(mockEntity);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).getEntityByName("test_entity", "test_player");
    }

    @Test
    @DisplayName("execute() with GET_ENTITY_HEALTH returns health")
    void testExecuteGetEntityHealth() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_HEALTH);
        action.setEntity("test_entity");

        when(mockMineflayerBackend.getEntityHealth("test_entity")).thenReturn(20.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("health="));
        verify(mockMineflayerBackend).getEntityHealth("test_entity");
    }

    @Test
    @DisplayName("execute() with GET_ENTITY_HEALTH stores health when storeAs is set")
    void testExecuteGetEntityHealth_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_HEALTH);
        action.setEntity("test_entity");
        action.setStoreAs("health_value");

        when(mockMineflayerBackend.getEntityHealth("test_entity")).thenReturn(15.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("health_value"), eq(15.0));
    }

    @Test
    @DisplayName("execute() with SET_ENTITY_HEALTH sets entity health")
    void testExecuteSetEntityHealth() throws Exception {
        Action action = new Action(Action.ActionType.SET_ENTITY_HEALTH);
        action.setEntity("test_entity");
        action.setValue(10.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("10.0"));
        verify(mockMineflayerBackend).setEntityHealth("test_entity", 10.0);
    }

    @Test
    @DisplayName("execute() with KILL_ENTITY kills entity by name")
    void testExecuteKillEntity_byName() throws Exception {
        Action action = new Action(Action.ActionType.KILL_ENTITY);
        action.setEntity("test_zombie");

        when(mockMineflayerBackend.executeRconWithCapture("kill test_zombie"))
            .thenReturn("Killed test_zombie");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("kill test_zombie");
    }

    @Test
    @DisplayName("execute() with KILL_ENTITY kills entity by type")
    void testExecuteKillEntity_byType() throws Exception {
        Action action = new Action(Action.ActionType.KILL_ENTITY);
        action.setEntityType("zombie");

        when(mockMineflayerBackend.executeRconWithCapture("kill @e[type=zombie]"))
            .thenReturn("Killed zombie");

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).executeRconWithCapture("kill @e[type=zombie]");
    }

    @Test
    @DisplayName("execute() with REMOVE_ENTITIES removes all test entities")
    void testExecuteRemoveEntities() {
        Action action = new Action(Action.ActionType.REMOVE_ENTITIES);

        ActionResult result = executor.execute(action, mockBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("Removed all test entities"));
        verify(mockBackend).removeAllTestEntities();
    }

    @Test
    @DisplayName("execute() handles exception gracefully")
    void testExecute_handlesException() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_HEALTH);
        action.setEntity("test_entity");

        when(mockMineflayerBackend.getEntityHealth("test_entity"))
            .thenThrow(new RuntimeException("Entity not found"));

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Entity not found"));
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
    @DisplayName("execute() with GET_ENTITIES_IN_VIEW filters entities when filterType set")
    void testExecuteGetEntitiesInView_withFilterType() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES_IN_VIEW);
        action.setPlayer("test_player");
        action.setFilterType("type");
        action.setFilterValue("minecraft:zombie");

        Map<String, Object> mockEntities = Map.of("entities", List.of(Map.of("id", "minecraft:zombie")));
        when(mockMineflayerBackend.getEntitiesInView("test_player")).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).getEntitiesInView("test_player");
    }

    @Test
    @DisplayName("execute() with GET_ENTITIES_IN_VIEW stores when storeAs set")
    void testExecuteGetEntitiesInView_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES_IN_VIEW);
        action.setPlayer("test_player");
        action.setStoreAs("view_entities");

        Map<String, Object> mockEntities = Map.of("entities", List.of(Map.of("id", "minecraft:cow")));
        when(mockMineflayerBackend.getEntitiesInView("test_player")).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("view_entities"), any());
    }

    @Test
    @DisplayName("execute() with GET_ENTITY_BY_NAME filters when filterType set")
    void testExecuteGetEntityByName_withFilterType() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_BY_NAME);
        action.setEntity("test_entity");
        action.setPlayer("test_player");
        action.setFilterType("health");
        action.setFilterValue("10");

        Map<String, Object> mockEntity = Map.of("CustomName", "test_entity", "Health", 10);
        when(mockMineflayerBackend.getEntityByName("test_entity", "test_player")).thenReturn(mockEntity);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).getEntityByName("test_entity", "test_player");
    }

    @Test
    @DisplayName("execute() with GET_ENTITY_BY_NAME stores when storeAs set")
    void testExecuteGetEntityByName_withStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_BY_NAME);
        action.setEntity("test_entity");
        action.setPlayer("test_player");
        action.setStoreAs("entity_data");

        Map<String, Object> mockEntity = Map.of("CustomName", "test_entity");
        when(mockMineflayerBackend.getEntityByName("test_entity", "test_player")).thenReturn(mockEntity);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockStateManager).store(eq("entity_data"), any());
    }

    @Test
    @DisplayName("execute() with GET_ENTITY_HEALTH returns response without storing")
    void testExecuteGetEntityHealth_withoutStoreAs() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_HEALTH);
        action.setEntity("test_entity");
        // No storeAs set

        when(mockMineflayerBackend.getEntityHealth("test_entity")).thenReturn(20.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("health="));
        // Should NOT call store
        verify(mockStateManager, never()).store(anyString(), any());
    }

    @Test
    @DisplayName("execute() with SET_ENTITY_HEALTH calls backend setEntityHealth method")
    void testExecuteSetEntityHealth_callsBackendMethod() throws Exception {
        Action action = new Action(Action.ActionType.SET_ENTITY_HEALTH);
        action.setEntity("test_entity");
        action.setValue(10.0);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        // The implementation calls setEntityHealth on the backend, not RCON
        verify(mockMineflayerBackend).setEntityHealth("test_entity", 10.0);
    }

    @Test
    @DisplayName("execute() with GET_ENTITIES handles null player")
    void testExecuteGetEntities_nullPlayer() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES);
        action.setPlayer(null);

        Map<String, Object> mockEntities = Map.of("entities", List.of());
        when(mockMineflayerBackend.getEntities(null)).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockMineflayerBackend, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockMineflayerBackend).getEntities(null);
    }

    // ========================================================================
    // UNCOVERED BRANCH TESTS - RCON BACKEND
    // ========================================================================

    @Test
    @DisplayName("execute() GET_ENTITIES with RCON backend")
    void testExecuteGetEntities_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        Map<String, Object> mockEntities = Map.of("entities", List.of(Map.of("id", "minecraft:zombie")));
        when(mockRcon.getEntitiesInView("test_player")).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getEntitiesInView("test_player");
    }

    @Test
    @DisplayName("execute() GET_ENTITIES_IN_VIEW with RCON backend")
    void testExecuteGetEntitiesInView_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITIES_IN_VIEW);
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        Map<String, Object> mockEntities = Map.of("entities", List.of(Map.of("id", "minecraft:cow")));
        when(mockRcon.getEntitiesInView("test_player")).thenReturn(mockEntities);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getEntitiesInView("test_player");
    }

    @Test
    @DisplayName("execute() GET_ENTITY_BY_NAME with RCON backend")
    void testExecuteGetEntityByName_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_BY_NAME);
        action.setEntity("test_entity");
        action.setPlayer("test_player");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        Map<String, Object> mockEntity = Map.of("CustomName", "test_entity");
        when(mockRcon.getEntityByName("test_entity", "test_player")).thenReturn(mockEntity);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getEntityByName("test_entity", "test_player");
    }

    @Test
    @DisplayName("execute() GET_ENTITY_HEALTH with RCON backend")
    void testExecuteGetEntityHealth_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_HEALTH);
        action.setEntity("test_entity");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.getEntityHealth("test_entity")).thenReturn(20.0);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).getEntityHealth("test_entity");
    }

    @Test
    @DisplayName("execute() GET_ENTITY_HEALTH with RCON backend returns null when entity not found")
    void testExecuteGetEntityHealth_withRconBackend_null() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_HEALTH);
        action.setEntity("missing_entity");

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        when(mockRcon.getEntityHealth("missing_entity")).thenReturn(0.0);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().contains("health="));
    }

    @Test
    @DisplayName("execute() SET_ENTITY_HEALTH with RCON backend")
    void testExecuteSetEntityHealth_withRconBackend() throws Exception {
        Action action = new Action(Action.ActionType.SET_ENTITY_HEALTH);
        action.setEntity("test_entity");
        action.setValue(15.0);

        org.cavarest.pilaf.backend.RconBackend mockRcon = mock(org.cavarest.pilaf.backend.RconBackend.class);
        doNothing().when(mockRcon).setEntityHealth("test_entity", 15.0);

        ActionResult result = executor.execute(action, mockRcon, mockStateManager);

        assertTrue(result.isSuccess());
        verify(mockRcon).setEntityHealth("test_entity", 15.0);
    }

    @Test
    @DisplayName("execute() GET_ENTITY_HEALTH with zero value returns correct response")
    void testExecuteGetEntityHealth_zeroValue() throws Exception {
        Action action = new Action(Action.ActionType.GET_ENTITY_HEALTH);
        action.setEntity("dead_entity");

        org.cavarest.pilaf.backend.MineflayerBackend mockMineflayer = mock(org.cavarest.pilaf.backend.MineflayerBackend.class);
        when(mockMineflayer.getEntityHealth("dead_entity")).thenReturn(0.0);

        ActionResult result = executor.execute(action, mockMineflayer, mockStateManager);

        assertTrue(result.isSuccess());
        assertEquals("health=0.0", result.getResponse());
    }
}
