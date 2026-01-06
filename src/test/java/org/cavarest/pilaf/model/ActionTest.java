package org.cavarest.pilaf.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Action enum
 */
public class ActionTest {

    @Test
    public void testActionTypesExist() {
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
        // Verify we have the expected number of action types
        Action.ActionType[] types = Action.ActionType.values();
        assertTrue(types.length > 60, "Should have more than 60 action types");
    }

    @Test
    public void testActionConstructor() {
        Action action = new Action();
        action.setType(Action.ActionType.WAIT);
        action.setDuration(1000L);

        assertEquals(Action.ActionType.WAIT, action.getType());
        assertEquals(Long.valueOf(1000L), action.getDuration());
    }
}