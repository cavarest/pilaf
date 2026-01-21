/**
 * Inventory Testing Example
 *
 * This example shows how to test inventory operations
 * including giving items, checking inventory, and verifying counts.
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Inventory Testing Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  it('should test giving items to player', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Give Items Test',
      description: 'Demonstrates giving items and verifying inventory',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Item Receiver', username: 'receiver' }
        ]
      },

      steps: [
        {
          name: '[RCON] Clear inventory',
          action: 'execute_command',
          command: 'clear receiver'
        },
        {
          name: 'Wait for clear',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: receiver] Get initial inventory',
          action: 'get_player_inventory',
          player: 'receiver',
          store_as: 'initial_inventory'
        },
        {
          name: 'Verify no diamonds initially',
          action: 'assert',
          condition: 'does_not_have_item',
          expected: 'diamond',
          actual: '{initial_inventory}'
        },
        {
          name: '[RCON] Give 64 diamonds',
          action: 'execute_command',
          command: 'give receiver diamond 64'
        },
        {
          name: 'Wait for item processing',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: receiver] Get updated inventory',
          action: 'get_player_inventory',
          player: 'receiver',
          store_as: 'updated_inventory'
        },
        {
          name: 'Verify player has diamonds',
          action: 'assert',
          condition: 'has_item',
          expected: 'diamond',
          actual: '{updated_inventory}'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test multiple item operations', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Multiple Item Operations',
      description: 'Demonstrates complex inventory operations',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Inventory Tester', username: 'inv_tester' }
        ]
      },

      steps: [
        {
          name: '[RCON] Clear inventory',
          action: 'execute_command',
          command: 'clear inv_tester'
        },
        {
          name: '[RCON] Give various items',
          action: 'execute_command',
          command: 'give inv_tester diamond_sword'
        },
        {
          name: '[RCON] Give armor',
          action: 'execute_command',
          command: 'give inv_tester diamond_chestplate'
        },
        {
          name: '[RCON] Give food',
          action: 'execute_command',
          command: 'give inv_tester golden_apple 16'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 2
        },
        {
          name: '[player: inv_tester] Check final inventory',
          action: 'get_player_inventory',
          player: 'inv_tester',
          store_as: 'final_inventory'
        },
        {
          name: 'Verify sword received',
          action: 'assert',
          condition: 'has_item',
          expected: 'diamond_sword',
          actual: '{final_inventory}'
        },
        {
          name: 'Verify chestplate received',
          action: 'assert',
          condition: 'has_item',
          expected: 'diamond_chestplate',
          actual: '{final_inventory}'
        },
        {
          name: 'Verify apples received',
          action: 'assert',
          condition: 'has_item',
          expected: 'golden_apple',
          actual: '{final_inventory}'
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
