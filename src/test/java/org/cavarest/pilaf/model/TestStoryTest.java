package org.cavarest.pilaf.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TestStory
 */
@DisplayName("TestStory Tests")
class TestStoryTest {

    @Test
    @DisplayName("default constructor creates TestStory with default values")
    void testDefaultConstructor() {
        TestStory story = new TestStory();

        assertNull(story.getName());
        assertNull(story.getDescription());
        assertNull(story.getBackend());
        assertNotNull(story.getSetup());
        assertTrue(story.getSetup().isEmpty());
        assertNotNull(story.getSteps());
        assertTrue(story.getSteps().isEmpty());
        assertNotNull(story.getAssertions());
        assertTrue(story.getAssertions().isEmpty());
        assertNotNull(story.getCleanup());
        assertTrue(story.getCleanup().isEmpty());
    }

    @Test
    @DisplayName("constructor with name sets name")
    void testConstructorWithName() {
        TestStory story = new TestStory("test-story");

        assertEquals("test-story", story.getName());
    }

    @Test
    @DisplayName("setName and getName work correctly")
    void testSetName_getName() {
        TestStory story = new TestStory();

        story.setName("my-story");
        assertEquals("my-story", story.getName());
    }

    @Test
    @DisplayName("setDescription and getDescription work correctly")
    void testSetDescription_getDescription() {
        TestStory story = new TestStory();

        story.setDescription("A test description");
        assertEquals("A test description", story.getDescription());
    }

    @Test
    @DisplayName("setBackend and getBackend work correctly")
    void testSetBackend_getBackend() {
        TestStory story = new TestStory();

        story.setBackend("mineflayer");
        assertEquals("mineflayer", story.getBackend());
    }

    @Test
    @DisplayName("setSetup and getSetup work correctly")
    void testSetSetup_getSetup() {
        TestStory story = new TestStory();
        Action action1 = new Action(Action.ActionType.CONNECT_PLAYER);
        Action action2 = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);
        List<Action> setupActions = List.of(action1, action2);

        story.setSetup(setupActions);

        assertEquals(2, story.getSetup().size());
        assertEquals(action1, story.getSetup().get(0));
        assertEquals(action2, story.getSetup().get(1));
    }

    @Test
    @DisplayName("setSteps and getSteps work correctly")
    void testSetSteps_getSteps() {
        TestStory story = new TestStory();
        Action action1 = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);
        Action action2 = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);
        List<Action> steps = List.of(action1, action2);

        story.setSteps(steps);

        assertEquals(2, story.getSteps().size());
        assertEquals(action1, story.getSteps().get(0));
        assertEquals(action2, story.getSteps().get(1));
    }

    @Test
    @DisplayName("setAssertions and getAssertions work correctly")
    void testSetAssertions_getAssertions() {
        TestStory story = new TestStory();
        Assertion assertion1 = new Assertion(Assertion.AssertionType.ASSERT_RESPONSE_CONTAINS);
        assertion1.setContains("text");
        Assertion assertion2 = new Assertion(Assertion.AssertionType.ENTITY_EXISTS);
        assertion2.setEntity("entity");
        List<Assertion> assertions = List.of(assertion1, assertion2);

        story.setAssertions(assertions);

        assertEquals(2, story.getAssertions().size());
        assertEquals(assertion1, story.getAssertions().get(0));
        assertEquals(assertion2, story.getAssertions().get(1));
    }

    @Test
    @DisplayName("setCleanup and getCleanup work correctly")
    void testSetCleanup_getCleanup() {
        TestStory story = new TestStory();
        Action action1 = new Action(Action.ActionType.DISCONNECT_PLAYER);
        Action action2 = new Action(Action.ActionType.CLEAR_ENTITIES);
        List<Action> cleanupActions = List.of(action1, action2);

        story.setCleanup(cleanupActions);

        assertEquals(2, story.getCleanup().size());
        assertEquals(action1, story.getCleanup().get(0));
        assertEquals(action2, story.getCleanup().get(1));
    }

    @Test
    @DisplayName("getSetupActions returns setup list")
    void testGetSetupActions() {
        TestStory story = new TestStory();
        Action action = new Action(Action.ActionType.CONNECT_PLAYER);

        story.addSetupAction(action);

        assertEquals(1, story.getSetupActions().size());
        assertEquals(action, story.getSetupActions().get(0));
    }

    @Test
    @DisplayName("getCleanupActions returns cleanup list")
    void testGetCleanupActions() {
        TestStory story = new TestStory();
        Action action = new Action(Action.ActionType.DISCONNECT_PLAYER);

        story.addCleanupAction(action);

        assertEquals(1, story.getCleanupActions().size());
        assertEquals(action, story.getCleanupActions().get(0));
    }

    @Test
    @DisplayName("addSetupAction adds action and returns this")
    void testAddSetupAction() {
        TestStory story = new TestStory();
        Action action = new Action(Action.ActionType.CONNECT_PLAYER);

        TestStory result = story.addSetupAction(action);

        assertSame(story, result);
        assertEquals(1, story.getSetup().size());
        assertEquals(action, story.getSetup().get(0));
    }

    @Test
    @DisplayName("addStep adds action and returns this")
    void testAddStep() {
        TestStory story = new TestStory();
        Action action = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);

        TestStory result = story.addStep(action);

        assertSame(story, result);
        assertEquals(1, story.getSteps().size());
        assertEquals(action, story.getSteps().get(0));
    }

    @Test
    @DisplayName("addAssertion adds assertion and returns this")
    void testAddAssertion() {
        TestStory story = new TestStory();
        Assertion assertion = new Assertion(Assertion.AssertionType.ENTITY_EXISTS);
        assertion.setEntity("test_entity");

        TestStory result = story.addAssertion(assertion);

        assertSame(story, result);
        assertEquals(1, story.getAssertions().size());
        assertEquals(assertion, story.getAssertions().get(0));
    }

    @Test
    @DisplayName("addCleanupAction adds action and returns this")
    void testAddCleanupAction() {
        TestStory story = new TestStory();
        Action action = new Action(Action.ActionType.DISCONNECT_PLAYER);

        TestStory result = story.addCleanupAction(action);

        assertSame(story, result);
        assertEquals(1, story.getCleanup().size());
        assertEquals(action, story.getCleanup().get(0));
    }

    @Test
    @DisplayName("multiple addSetupAction calls add all actions")
    void testMultipleAddSetupAction() {
        TestStory story = new TestStory();
        Action action1 = new Action(Action.ActionType.CONNECT_PLAYER);
        Action action2 = new Action(Action.ActionType.EXECUTE_RCON_COMMAND);

        story.addSetupAction(action1).addSetupAction(action2);

        assertEquals(2, story.getSetup().size());
    }

    @Test
    @DisplayName("multiple addStep calls add all actions")
    void testMultipleAddStep() {
        TestStory story = new TestStory();
        Action action1 = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);
        Action action2 = new Action(Action.ActionType.ASSERT_RESPONSE_CONTAINS);

        story.addStep(action1).addStep(action2);

        assertEquals(2, story.getSteps().size());
    }

    @Test
    @DisplayName("multiple addAssertion calls add all assertions")
    void testMultipleAddAssertion() {
        TestStory story = new TestStory();
        Assertion assertion1 = new Assertion(Assertion.AssertionType.ENTITY_EXISTS);
        assertion1.setEntity("entity1");
        Assertion assertion2 = new Assertion(Assertion.AssertionType.ENTITY_EXISTS);
        assertion2.setEntity("entity2");

        story.addAssertion(assertion1).addAssertion(assertion2);

        assertEquals(2, story.getAssertions().size());
    }

    @Test
    @DisplayName("multiple addCleanupAction calls add all actions")
    void testMultipleAddCleanupAction() {
        TestStory story = new TestStory();
        Action action1 = new Action(Action.ActionType.DISCONNECT_PLAYER);
        Action action2 = new Action(Action.ActionType.CLEAR_ENTITIES);

        story.addCleanupAction(action1).addCleanupAction(action2);

        assertEquals(2, story.getCleanup().size());
    }

    @Test
    @DisplayName("chained method calls work correctly")
    void testChainedMethodCalls() {
        TestStory story = new TestStory();
        Action setupAction = new Action(Action.ActionType.CONNECT_PLAYER);
        Action stepAction = new Action(Action.ActionType.ASSERT_ENTITY_EXISTS);
        Assertion assertion = new Assertion(Assertion.AssertionType.ENTITY_EXISTS);
        assertion.setEntity("entity");
        Action cleanupAction = new Action(Action.ActionType.DISCONNECT_PLAYER);

        story.setName("test-story");
        story.setDescription("Test description");
        story.setBackend("mineflayer");
        story.addSetupAction(setupAction)
            .addStep(stepAction)
            .addAssertion(assertion)
            .addCleanupAction(cleanupAction);

        assertEquals("test-story", story.getName());
        assertEquals("Test description", story.getDescription());
        assertEquals("mineflayer", story.getBackend());
        assertEquals(1, story.getSetup().size());
        assertEquals(1, story.getSteps().size());
        assertEquals(1, story.getAssertions().size());
        assertEquals(1, story.getCleanup().size());
    }
}
