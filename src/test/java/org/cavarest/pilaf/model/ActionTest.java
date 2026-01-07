package org.cavarest.pilaf.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive unit tests for Action class covering all action types,
 * field validation, factory methods, and state management.
 */
public class ActionTest {

    // ========================================================================
    // ACTION TYPE EXISTENCE TESTS
    // ========================================================================

    @Test
    public void testAllActionTypesExist() {
        // Verify all major action types exist
        assertNotNull(Action.ActionType.WAIT);
        assertNotNull(Action.ActionType.CONNECT_PLAYER);
        assertNotNull(Action.ActionType.DISCONNECT_PLAYER);
        assertNotNull(Action.ActionType.EXECUTE_RCON_COMMAND);
        assertNotNull(Action.ActionType.GIVE_ITEM);
        assertNotNull(Action.ActionType.GET_PLAYER_POSITION);
        assertNotNull(Action.ActionType.GET_ENTITIES_IN_VIEW);
        assertNotNull(Action.ActionType.SEND_CHAT_MESSAGE);
    }

    @Test
    public void testActionTypeCount() {
        // Verify we have the expected number of action types (should be 70+)
        Action.ActionType[] types = Action.ActionType.values();
        assertTrue(types.length >= 70, "Should have at least 70 action types, found: " + types.length);
    }

    @Test
    public void testAllServerCommandTypesExist() {
        // Server command types
        assertNotNull(Action.ActionType.SERVER_COMMAND);
        assertNotNull(Action.ActionType.EXECUTE_RCON_COMMAND);
        assertNotNull(Action.ActionType.EXECUTE_RCON_WITH_CAPTURE);
        assertNotNull(Action.ActionType.EXECUTE_RCON_RAW);
        assertNotNull(Action.ActionType.EXECUTE_PLUGIN_COMMAND);
        assertNotNull(Action.ActionType.GET_SERVER_INFO);
        assertNotNull(Action.ActionType.GET_PLUGIN_STATUS);
    }

    @Test
    public void testAllPlayerCommandTypesExist() {
        // Player command types
        assertNotNull(Action.ActionType.PLAYER_COMMAND);
        assertNotNull(Action.ActionType.EXECUTE_PLAYER_COMMAND);
        assertNotNull(Action.ActionType.EXECUTE_PLAYER_RAW);
        assertNotNull(Action.ActionType.MOVE_PLAYER);
        assertNotNull(Action.ActionType.TELEPORT_PLAYER);
        assertNotNull(Action.ActionType.LOOK_AT);
        assertNotNull(Action.ActionType.JUMP);
        assertNotNull(Action.ActionType.USE_ITEM);
        assertNotNull(Action.ActionType.ATTACK_ENTITY);
        assertNotNull(Action.ActionType.BREAK_BLOCK);
        assertNotNull(Action.ActionType.PLACE_BLOCK);
    }

    @Test
    public void testAllEntityCommandTypesExist() {
        // Entity command types
        assertNotNull(Action.ActionType.SPAWN_ENTITY);
        assertNotNull(Action.ActionType.REMOVE_ENTITIES);
        assertNotNull(Action.ActionType.CLEAR_ENTITIES);
        assertNotNull(Action.ActionType.KILL_ENTITY);
        assertNotNull(Action.ActionType.GET_ENTITIES);
        assertNotNull(Action.ActionType.GET_ENTITIES_IN_VIEW);
        assertNotNull(Action.ActionType.GET_ENTITY_BY_NAME);
        assertNotNull(Action.ActionType.GET_ENTITY_DISTANCE);
        assertNotNull(Action.ActionType.GET_ENTITY_HEALTH);
        assertNotNull(Action.ActionType.GET_ENTITY_HEALTH_INFO);
        assertNotNull(Action.ActionType.SET_ENTITY_HEALTH);
        assertNotNull(Action.ActionType.DAMAGE_ENTITY);
        assertNotNull(Action.ActionType.WAIT_FOR_ENTITY_SPAWN);
    }

    @Test
    public void testAllInventoryCommandTypesExist() {
        // Inventory command types
        assertNotNull(Action.ActionType.GET_INVENTORY);
        assertNotNull(Action.ActionType.GET_PLAYER_INVENTORY);
        assertNotNull(Action.ActionType.GIVE_ITEM);
        assertNotNull(Action.ActionType.REMOVE_ITEM);
        assertNotNull(Action.ActionType.EQUIP_ITEM);
        assertNotNull(Action.ActionType.GET_PLAYER_EQUIPMENT);
        assertNotNull(Action.ActionType.CLEAR_INVENTORY);
        assertNotNull(Action.ActionType.CLEAR_COOLDOWN);
        assertNotNull(Action.ActionType.SET_COOLDOWN);
    }

    @Test
    public void testAllWorldCommandTypesExist() {
        // World/Environment command types
        assertNotNull(Action.ActionType.GET_BLOCK_AT_POSITION);
        assertNotNull(Action.ActionType.GET_WORLD_TIME);
        assertNotNull(Action.ActionType.GET_WEATHER);
        assertNotNull(Action.ActionType.SET_TIME);
        assertNotNull(Action.ActionType.SET_WEATHER);
    }

    @Test
    public void testAllPlayerManagementTypesExist() {
        // Player management types
        assertNotNull(Action.ActionType.MAKE_OPERATOR);
        assertNotNull(Action.ActionType.GET_PLAYER_HEALTH);
        assertNotNull(Action.ActionType.SET_PLAYER_HEALTH);
        assertNotNull(Action.ActionType.HEAL_PLAYER);
        assertNotNull(Action.ActionType.KILL_PLAYER);
        assertNotNull(Action.ActionType.GAMEMODE_CHANGE);
        assertNotNull(Action.ActionType.SET_SPAWN_POINT);
    }

    @Test
    public void testAllStateManagementTypesExist() {
        // State management types
        assertNotNull(Action.ActionType.STORE_STATE);
        assertNotNull(Action.ActionType.PRINT_STORED_STATE);
        assertNotNull(Action.ActionType.COMPARE_STATES);
        assertNotNull(Action.ActionType.PRINT_STATE_COMPARISON);
    }

    @Test
    public void testAllDataExtractionTypesExist() {
        // Data extraction types
        assertNotNull(Action.ActionType.EXTRACT_WITH_JSONPATH);
        assertNotNull(Action.ActionType.FILTER_ENTITIES);
    }

    @Test
    public void testAllAssertionTypesExist() {
        // Assertion types
        assertNotNull(Action.ActionType.ASSERT_ENTITY_EXISTS);
        assertNotNull(Action.ActionType.ASSERT_ENTITY_MISSING);
        assertNotNull(Action.ActionType.ASSERT_PLAYER_HAS_ITEM);
        assertNotNull(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        assertNotNull(Action.ActionType.ASSERT_JSON_EQUALS);
        assertNotNull(Action.ActionType.ASSERT_LOG_CONTAINS);
        assertNotNull(Action.ActionType.ASSERT_CONDITION);
    }

    @Test
    public void testAllUtilityTypesExist() {
        // Utility types
        assertNotNull(Action.ActionType.WAIT);
        assertNotNull(Action.ActionType.WAIT_FOR_CHAT_MESSAGE);
        assertNotNull(Action.ActionType.CHECK_SERVICE_HEALTH);
        assertNotNull(Action.ActionType.SPAWN_TARGET);
        assertNotNull(Action.ActionType.REMOVE_PLAYERS);
        assertNotNull(Action.ActionType.GET_CHAT_HISTORY);
    }

    @Test
    public void testAllClientActionTypesExist() {
        // Client action types (Mineflayer)
        assertNotNull(Action.ActionType.LOOK_AT);
        assertNotNull(Action.ActionType.JUMP);
        assertNotNull(Action.ActionType.USE_ITEM);
        assertNotNull(Action.ActionType.ATTACK_ENTITY);
        assertNotNull(Action.ActionType.BREAK_BLOCK);
        assertNotNull(Action.ActionType.PLACE_BLOCK);
        assertNotNull(Action.ActionType.GET_CHAT_HISTORY);
    }

    // ========================================================================
    // ACTION FIELD VALIDATION TESTS
    // ========================================================================

    @Test
    public void testActionConstructor() {
        Action action = new Action();
        action.setType(Action.ActionType.WAIT);
        action.setDuration(1000L);

        assertEquals(Action.ActionType.WAIT, action.getType());
        assertEquals(Long.valueOf(1000L), action.getDuration());
    }

    @Test
    public void testActionConstructorWithType() {
        Action action = new Action(Action.ActionType.GIVE_ITEM);

        // Constructor sets the type, but player and item are not set
        assertEquals(Action.ActionType.GIVE_ITEM, action.getType());
        assertNull(action.getPlayer());
        assertNull(action.getItem());
    }

    @Test
    public void testActionAllFields() {
        Action action = new Action();
        action.setType(Action.ActionType.SPAWN_ENTITY);
        action.setName("Spawn Zombie");
        action.setEntityType("minecraft:zombie");
        action.setPlayer("test_player");
        action.setItem("diamond_sword");
        action.setCount(5);
        action.setCustomName("test_zombie");
        action.setLocation(Arrays.asList(100.0, 65.0, 100.0));

        assertEquals(Action.ActionType.SPAWN_ENTITY, action.getType());
        assertEquals("Spawn Zombie", action.getName());
        assertEquals("minecraft:zombie", action.getEntityType());
        assertEquals("test_player", action.getPlayer());
        assertEquals("diamond_sword", action.getItem());
        assertEquals(Integer.valueOf(5), action.getCount());
        assertEquals("test_zombie", action.getCustomName());
        assertNotNull(action.getLocation());
        assertEquals(3, action.getLocation().size());
    }

    @Test
    public void testActionStateFields() {
        Action action = new Action();
        action.setType(Action.ActionType.STORE_STATE);
        action.setVariableName("inventory_before");
        action.setSourceVariable("${{ steps.get_inventory.outputs.result }}");
        action.setStoreAs("initial_inventory");

        assertEquals("inventory_before", action.getVariableName());
        assertEquals("${{ steps.get_inventory.outputs.result }}", action.getSourceVariable());
        assertEquals("initial_inventory", action.getStoreAs());
    }

    @Test
    public void testActionComparisonFields() {
        Action action = new Action();
        action.setType(Action.ActionType.COMPARE_STATES);
        action.setState1("inventory_before");
        action.setState2("inventory_after");
        action.setFromComparison("inventory_comparison");

        assertEquals("inventory_before", action.getState1());
        assertEquals("inventory_after", action.getState2());
        assertEquals("inventory_comparison", action.getFromComparison());
    }

    @Test
    public void testActionAssertionFields() {
        Action action = new Action();
        action.setType(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        action.setSource("${{ steps.execute.outputs.result }}");
        action.setContains("Success");
        action.setNegated("false");

        assertEquals("${{ steps.execute.outputs.result }}", action.getSource());
        assertEquals("Success", action.getContains());
        assertEquals("false", action.getNegated());
    }

    @Test
    public void testActionExtractionFields() {
        Action action = new Action();
        action.setType(Action.ActionType.EXTRACT_WITH_JSONPATH);
        action.setSourceVariable("${{ steps.get_inventory.outputs.result }}");
        action.setJsonPath("$.items[0].id");
        action.setStoreAs("first_item");

        assertEquals("${{ steps.get_inventory.outputs.result }}", action.getSourceVariable());
        assertEquals("$.items[0].id", action.getJsonPath());
        assertEquals("first_item", action.getStoreAs());
    }

    @Test
    public void testActionFilterFields() {
        Action action = new Action();
        action.setType(Action.ActionType.FILTER_ENTITIES);
        action.setSourceVariable("${{ steps.get_entities.outputs.result }}");
        action.setFilterType("type");
        action.setFilterValue("zombie");
        action.setStoreAs("zombies");

        assertEquals("${{ steps.get_entities.outputs.result }}", action.getSourceVariable());
        assertEquals("type", action.getFilterType());
        assertEquals("zombie", action.getFilterValue());
        assertEquals("zombies", action.getStoreAs());
    }

    @Test
    public void testActionCommandFields() {
        Action action = new Action();
        action.setType(Action.ActionType.EXECUTE_RCON_COMMAND);
        action.setCommand("give pilaf_tester diamond_sword 1");
        action.setArgs(Arrays.asList("pilaf_tester", "diamond_sword", "1"));

        assertEquals("give pilaf_tester diamond_sword 1", action.getCommand());
        assertNotNull(action.getArgs());
        assertEquals(3, action.getArgs().size());
        assertEquals("pilaf_tester", action.getArgs().get(0));
    }

    @Test
    public void testActionEquipmentField() {
        Action action = new Action();
        action.setType(Action.ActionType.EQUIP_ITEM);
        action.setPlayer("pilaf_tester");
        action.setItem("diamond_sword");
        action.setSlot("hand");

        assertEquals("pilaf_tester", action.getPlayer());
        assertEquals("diamond_sword", action.getItem());
        assertEquals("hand", action.getSlot());
    }

    @Test
    public void testActionEquipmentMap() {
        Action action = new Action();
        action.setEquipment(Map.of(
            "head", "diamond_helmet",
            "chest", "diamond_chestplate",
            "legs", "diamond_leggings",
            "feet", "diamond_boots"
        ));

        assertNotNull(action.getEquipment());
        assertEquals(4, action.getEquipment().size());
        assertEquals("diamond_helmet", action.getEquipment().get("head"));
    }

    @Test
    public void testActionPluginField() {
        Action action = new Action();
        action.setType(Action.ActionType.EXECUTE_PLUGIN_COMMAND);
        action.setPlugin("Essentials");
        action.setCommand("/home");

        assertEquals("Essentials", action.getPlugin());
        assertEquals("/home", action.getCommand());
    }

    @Test
    public void testActionDamageField() {
        Action action = new Action();
        action.setType(Action.ActionType.DAMAGE_ENTITY);
        action.setEntity("test_zombie");
        action.setDamage("5");

        assertEquals("test_zombie", action.getEntity());
        assertEquals("5", action.getDamage());
    }

    @Test
    public void testActionValueField() {
        Action action = new Action();
        action.setType(Action.ActionType.SET_PLAYER_HEALTH);
        action.setValue(20.0);

        assertEquals(Double.valueOf(20.0), action.getValue());
    }

    @Test
    public void testActionPatternAndTimeoutFields() {
        Action action = new Action();
        action.setType(Action.ActionType.WAIT_FOR_CHAT_MESSAGE);
        action.setPattern(".*test.*");
        action.setTimeout("5000");
        action.setPlayer("pilaf_tester");

        assertEquals(".*test.*", action.getPattern());
        assertEquals("5000", action.getTimeout());
        assertEquals("pilaf_tester", action.getPlayer());
    }

    @Test
    public void testActionPositionField() {
        Action action = new Action();
        action.setType(Action.ActionType.GET_BLOCK_AT_POSITION);
        action.setPosition("[100, 64, 100]");

        assertEquals("[100, 64, 100]", action.getPosition());
    }

    @Test
    public void testActionFormatAndMessageFields() {
        Action action = new Action();
        action.setFormat("json");
        action.setMessage("Test message");

        assertEquals("json", action.getFormat());
        assertEquals("Test message", action.getMessage());
    }

    // ========================================================================
    // FACTORY METHOD TESTS
    // ========================================================================

    @Test
    public void testSpawnEntityFactory() {
        List<Double> location = Arrays.asList(100.0, 65.0, 100.0);
        Action action = Action.spawnEntity("Spawn Zombie", "minecraft:zombie", location);

        assertEquals(Action.ActionType.SPAWN_ENTITY, action.getType());
        assertEquals("Spawn Zombie", action.getName());
        assertEquals("minecraft:zombie", action.getEntityType());
        assertEquals(location, action.getLocation());
    }

    @Test
    public void testGiveItemFactory() {
        Action action = Action.giveItem("pilaf_tester", "diamond_sword", 1);

        assertEquals(Action.ActionType.GIVE_ITEM, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
        assertEquals("diamond_sword", action.getItem());
        assertEquals(Integer.valueOf(1), action.getCount());
    }

    @Test
    public void testEquipItemFactory() {
        Action action = Action.equipItem("pilaf_tester", "diamond_sword", "hand");

        assertEquals(Action.ActionType.EQUIP_ITEM, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
        assertEquals("diamond_sword", action.getItem());
        assertEquals("hand", action.getSlot());
    }

    @Test
    public void testPlayerCommandFactory() {
        List<String> args = Arrays.asList("arg1", "arg2");
        Action action = Action.playerCommand("pilaf_tester", "test_command", args);

        assertEquals(Action.ActionType.PLAYER_COMMAND, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
        assertEquals("test_command", action.getCommand());
        assertEquals(args, action.getArgs());
    }

    @Test
    public void testWaitForFactory() {
        Action action = Action.waitFor(5000L);

        assertEquals(Action.ActionType.WAIT, action.getType());
        assertEquals(Long.valueOf(5000L), action.getDuration());
    }

    @Test
    public void testConnectPlayerFactory() {
        Action action = Action.connectPlayer("test_player");

        assertEquals(Action.ActionType.CONNECT_PLAYER, action.getType());
        assertEquals("test_player", action.getPlayer());
    }

    @Test
    public void testDisconnectPlayerFactory() {
        Action action = Action.disconnectPlayer("test_player");

        assertEquals(Action.ActionType.DISCONNECT_PLAYER, action.getType());
        assertEquals("test_player", action.getPlayer());
    }

    @Test
    public void testExecutePlayerCommandFactory() {
        Action action = Action.executePlayerCommand("pilaf_tester", "/home");

        assertEquals(Action.ActionType.EXECUTE_PLAYER_COMMAND, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
        assertEquals("/home", action.getCommand());
    }

    @Test
    public void testExecuteRconCommandFactory() {
        Action action = Action.executeRconCommand("say Hello");

        assertEquals(Action.ActionType.EXECUTE_RCON_COMMAND, action.getType());
        assertEquals("say Hello", action.getCommand());
    }

    @Test
    public void testExecuteRconRawFactory() {
        Action action = Action.executeRconRaw("give pilaf_tester diamond 1");

        assertEquals(Action.ActionType.EXECUTE_RCON_RAW, action.getType());
        assertEquals("give pilaf_tester diamond 1", action.getCommand());
    }

    @Test
    public void testExecutePlayerRawFactory() {
        Action action = Action.executePlayerRaw("pilaf_tester", "/spawn");

        assertEquals(Action.ActionType.EXECUTE_PLAYER_RAW, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
        assertEquals("/spawn", action.getCommand());
    }

    @Test
    public void testGetEntitiesFactory() {
        Action action = Action.getEntities("pilaf_tester");

        assertEquals(Action.ActionType.GET_ENTITIES, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
    }

    @Test
    public void testGetInventoryFactory() {
        Action action = Action.getInventory("pilaf_tester");

        assertEquals(Action.ActionType.GET_INVENTORY, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
    }

    @Test
    public void testClearCooldownFactory() {
        Action action = Action.clearCooldown("pilaf_tester");

        assertEquals(Action.ActionType.CLEAR_COOLDOWN, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
    }

    @Test
    public void testSetCooldownFactory() {
        Action action = Action.setCooldown("pilaf_tester", 60000L);

        assertEquals(Action.ActionType.SET_COOLDOWN, action.getType());
        assertEquals("pilaf_tester", action.getPlayer());
        assertEquals(Long.valueOf(60000L), action.getDuration());
    }

    // ========================================================================
    // ACTION TOSTRING TEST
    // ========================================================================

    @Test
    public void testActionToString() {
        Action action = new Action();
        action.setType(Action.ActionType.GIVE_ITEM);
        action.setName("Give Item");
        action.setPlayer("test_player");

        String toString = action.toString();

        assertTrue(toString.contains("type=GIVE_ITEM"));
        assertTrue(toString.contains("name=Give Item"));
        assertTrue(toString.contains("player=test_player"));
    }

    @Test
    public void testActionToStringWithNullFields() {
        Action action = new Action();
        action.setType(Action.ActionType.WAIT);

        String toString = action.toString();

        assertTrue(toString.contains("type=WAIT"));
        assertTrue(toString.contains("name=null"));
        assertTrue(toString.contains("player=null"));
    }

    // ========================================================================
    // ACTION TYPE VALUE OF TESTS
    // ========================================================================

    @Test
    public void testActionTypeValueOf() {
        assertEquals(Action.ActionType.WAIT, Action.ActionType.valueOf("WAIT"));
        assertEquals(Action.ActionType.GIVE_ITEM, Action.ActionType.valueOf("GIVE_ITEM"));
        assertEquals(Action.ActionType.SPAWN_ENTITY, Action.ActionType.valueOf("SPAWN_ENTITY"));
    }

    @Test
    public void testActionTypeValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            Action.ActionType.valueOf("INVALID_ACTION");
        });
    }

    // ========================================================================
    // ACTION EQUALITY TESTS
    // ========================================================================

    @Test
    public void testActionEquality() {
        Action action1 = new Action();
        action1.setType(Action.ActionType.WAIT);
        action1.setDuration(1000L);

        Action action2 = new Action();
        action2.setType(Action.ActionType.WAIT);
        action2.setDuration(1000L);

        // Actions with same values should be equal
        assertEquals(action1.getType(), action2.getType());
        assertEquals(action1.getDuration(), action2.getDuration());
    }

    @Test
    public void testActionInequality() {
        Action action1 = new Action();
        action1.setType(Action.ActionType.WAIT);
        action1.setDuration(1000L);

        Action action2 = new Action();
        action2.setType(Action.ActionType.WAIT);
        action2.setDuration(2000L);

        assertNotEquals(action1.getDuration(), action2.getDuration());
    }
}
