/**
 * Entity Interaction Example
 *
 * This example shows how to test entity spawning, querying,
 * and interactions.
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Entity Interaction Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  it('should test entity spawning and querying', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Entity Spawn Test',
      description: 'Demonstrates chicken spawning and query operations',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Entity Master', username: 'entity_master' }
        ]
      },

      steps: [
        {
          name: '[player: entity_master] Get initial entities',
          action: 'get_entities',
          player: 'entity_master',
          store_as: 'initial_entities'
        },
        {
          name: '[RCON] Spawn a chicken',
          action: 'execute_command',
          command: 'summon chicken ~ ~1 ~'
        },
        {
          name: 'Wait for spawn',
          action: 'wait',
          duration: 2
        },
        {
          name: '[player: entity_master] Get updated entities',
          action: 'get_entities',
          player: 'entity_master',
          store_as: 'updated_entities'
        },
        {
          name: 'Verify chicken exists',
          action: 'assert',
          condition: 'entity_exists',
          expected: 'chicken',
          actual: '{updated_entities}'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test entity lifecycle', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Entity Lifecycle Test',
      description: 'Demonstrates complete entity lifecycle',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Game Master', username: 'gamemaster' }
        ]
      },

      steps: [
        {
          name: '[player: gamemaster] Get entity count before',
          action: 'get_entities',
          player: 'gamemaster',
          store_as: 'entities_before'
        },
        {
          name: '[RCON] Spawn named chicken',
          action: 'execute_command',
          command: 'summon chicken ~ ~ ~ {CustomName:"TestChicken"}'
        },
        {
          name: 'Wait for spawn',
          action: 'wait',
          duration: 2
        },
        {
          name: '[player: gamemaster] Get entities after spawn',
          action: 'get_entities',
          player: 'gamemaster',
          store_as: 'entities_after'
        },
        {
          name: 'Verify chicken spawned',
          action: 'assert',
          condition: 'entity_exists',
          expected: 'TestChicken',
          actual: '{entities_after}'
        },
        {
          name: '[RCON] Kill the chicken',
          action: 'execute_command',
          command: 'kill @e[type=chicken]'
        },
        {
          name: 'Wait for death and entity cleanup',
          action: 'wait',
          duration: 3
        },
        {
          name: '[player: gamemaster] Get entities after kill',
          action: 'get_entities',
          player: 'gamemaster',
          store_as: 'entities_final'
        },
        {
          name: 'Verify chicken removed',
          action: 'assert',
          condition: 'entity_not_exists',
          expected: 'TestChicken',
          actual: '{entities_final}'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });
});
