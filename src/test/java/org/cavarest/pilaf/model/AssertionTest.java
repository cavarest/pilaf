package org.cavarest.pilaf.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Assertion class
 */
public class AssertionTest {

    @Test
    public void testAssertionTypesExist() {
        // Verify all major assertion types exist
        assertNotNull(Assertion.AssertionType.ENTITY_HEALTH);
        assertNotNull(Assertion.AssertionType.ENTITY_EXISTS);
        assertNotNull(Assertion.AssertionType.PLAYER_INVENTORY);
        assertNotNull(Assertion.AssertionType.PLUGIN_COMMAND);
        assertNotNull(Assertion.AssertionType.ASSERT_ENTITY_MISSING);
        assertNotNull(Assertion.AssertionType.ASSERT_PLAYER_HAS_ITEM);
        assertNotNull(Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS);
        assertNotNull(Assertion.AssertionType.ASSERT_JSON_EQUALS);
    }

    @Test
    public void testAssertionTypeCount() {
        // Verify we have the expected number of assertion types
        Assertion.AssertionType[] types = Assertion.AssertionType.values();
        assertTrue(types.length >= 10, "Should have at least 10 assertion types");
    }

    @Test
    public void testAssertionConstructor() {
        Assertion assertion = new Assertion();
        assertion.setType(Assertion.AssertionType.ENTITY_HEALTH);
        assertion.setEntity("test_entity");
        assertion.setValue(20.0);

        assertEquals(Assertion.AssertionType.ENTITY_HEALTH, assertion.getType());
        assertEquals("test_entity", assertion.getEntity());
        assertEquals(20.0, assertion.getValue());
    }
}