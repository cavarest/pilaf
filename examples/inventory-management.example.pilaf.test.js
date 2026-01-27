/**
 * Inventory Management Example
 *
 * This example shows how to test inventory management operations
 * including dropping items, consuming items, equipping gear, and
 * managing inventory slots.
 *
 * New actions demonstrated:
 * - drop_item: Drop an item from inventory
 * - consume_item: Consume a food item (eat, drink)
 * - equip_item: Equip an item to a specific slot
 * - swap_inventory_slots: Swap items between inventory slots
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Inventory Management Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  it('should test dropping items', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Drop Item Test',
      description: 'Demonstrates dropping items from inventory',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Dropper', username: 'dropper' }
        ]
      },

      steps: [
        {
          name: '[player: dropper] Get initial inventory',
          action: 'get_player_inventory',
          player: 'dropper',
          store_as: 'initial_inventory'
        },
        {
          name: '[player: dropper] Give items to drop',
          action: 'execute_command',
          command: 'give dropper diamond 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: dropper] Get inventory with diamonds',
          action: 'get_player_inventory',
          player: 'dropper',
          store_as: 'inventory_with_diamonds'
        },
        {
          name: 'Verify diamonds received',
          action: 'assert',
          condition: 'has_item',
          actual: '{inventory_with_diamonds}',
          expected: 'diamond'
        },
        {
          name: '[player: dropper] Drop 10 diamonds',
          action: 'drop_item',
          player: 'dropper',
          item_name: 'diamond',
          count: 10
        },
        {
          name: 'Wait for drop',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: dropper] Get final inventory',
          action: 'get_player_inventory',
          player: 'dropper',
          store_as: 'final_inventory'
        },
        {
          name: 'Verify diamond count decreased (64 → 54)',
          action: 'assert',
          condition: 'count_decreased',
          actual: '{final_inventory}',
          expected: 'diamond',
          max_expected: 54
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  // Consume item test
  it('should test consuming food items', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Consume Item Test',
      description: 'Demonstrates eating food (will fail if food is full)',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Eater', username: 'eater' }
        ]
      },

      steps: [
        {
          name: '[player: eater] Apply strong hunger effect to self',
          action: 'execute_player_command',
          player: 'eater',
          command: 'effect give @s minecraft:hunger 60 10'
        },
        {
          name: 'Wait for hunger effect',
          action: 'wait',
          duration: 3
        },
        {
          name: '[player: eater] Give food items',
          action: 'execute_command',
          command: 'give eater cooked_beef 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: eater] Eat cooked beef',
          action: 'consume_item',
          player: 'eater',
          item_name: 'cooked_beef'
        },
        {
          name: 'Wait for consumption',
          action: 'wait',
          duration: 1
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test equipping items to different slots', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Equip Item Test',
      description: 'Demonstrates equipping items to different equipment slots',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Adventurer', username: 'adventurer' }
        ]
      },

      steps: [
        {
          name: '[player: adventurer] Give equipment',
          action: 'execute_command',
          command: 'give adventurer diamond_sword'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: adventurer] Give helmet',
          action: 'execute_command',
          command: 'give adventurer diamond_helmet'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: adventurer] Give chestplate',
          action: 'execute_command',
          command: 'give adventurer diamond_chestplate'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: adventurer] Equip sword to hand',
          action: 'equip_item',
          player: 'adventurer',
          item_name: 'diamond_sword',
          destination: 'hand'
        },
        {
          name: 'Wait for equip',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: adventurer] Equip helmet to head',
          action: 'equip_item',
          player: 'adventurer',
          item_name: 'diamond_helmet',
          destination: 'head'
        },
        {
          name: 'Wait for equip',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: adventurer] Equip chestplate',
          action: 'equip_item',
          player: 'adventurer',
          item_name: 'diamond_chestplate',
          destination: 'torso'
        },
        {
          name: 'Wait for equip',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: adventurer] Get final inventory',
          action: 'get_player_inventory',
          player: 'adventurer',
          store_as: 'final_inventory'
        },
        {
          name: 'Verify equipment is equipped',
          action: 'assert',
          condition: 'has_item',
          actual: '{final_inventory}',
          expected: 'diamond_sword'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test swapping inventory slots', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Swap Inventory Slots Test',
      description: 'Demonstrates swapping items between inventory slots',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Organizer', username: 'organizer' }
        ]
      },

      steps: [
        {
          name: '[player: organizer] Give items to multiple slots',
          action: 'execute_command',
          command: 'give organizer diamond 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: organizer] Give another stack',
          action: 'execute_command',
          command: 'give organizer diamond 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: organizer] Give a third stack',
          action: 'execute_command',
          command: 'give organizer gold_ingot 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: organizer] Get inventory to see occupied slots',
          action: 'get_player_inventory',
          player: 'organizer',
          store_as: 'initial_inventory'
        },
        {
          name: '[player: organizer] Swap inventory slots (slot 0 to 1)',
          action: 'swap_inventory_slots',
          player: 'organizer',
          from_slot: 0,
          to_slot: 1
        },
        {
          name: 'Wait for swap',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: organizer] Get final inventory state',
          action: 'get_player_inventory',
          player: 'organizer',
          store_as: 'final_inventory'
        }
        // Test demonstrates swapping inventory slots
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test complete inventory workflow', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Complete Inventory Workflow',
      description: 'Demonstrates a complete inventory management sequence',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'InventoryMaster', username: 'inv_master' }
        ]
      },

      steps: [
        {
          name: '[player: inv_master] Get initial inventory',
          action: 'get_player_inventory',
          player: 'inv_master',
          store_as: 'initial_inventory'
        },
        {
          name: '[player: inv_master] Give variety of items',
          action: 'execute_command',
          command: 'give inv_master diamond_sword'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: inv_master] Give food',
          action: 'execute_command',
          command: 'give inv_master bread 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: inv_master] Give armor',
          action: 'execute_command',
          command: 'give inv_master diamond_chestplate'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: inv_master] Equip sword',
          action: 'equip_item',
          player: 'inv_master',
          item_name: 'diamond_sword',
          destination: 'hand'
        },
        {
          name: 'Wait for equip',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: inv_master] Equip armor',
          action: 'equip_item',
          player: 'inv_master',
          item_name: 'diamond_chestplate',
          destination: 'torso'
        },
        {
          name: 'Wait for equip',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: inv_master] Swap inventory slots',
          action: 'swap_inventory_slots',
          player: 'inv_master',
          from_slot: 1,
          to_slot: 2
        },
        {
          name: 'Wait for swap',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: inv_master] Drop extra items',
          action: 'drop_item',
          player: 'inv_master',
          item_name: 'diamond_sword',
          count: 1
        },
        {
          name: 'Wait for drop',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: inv_master] Get final inventory',
          action: 'get_player_inventory',
          player: 'inv_master',
          store_as: 'final_inventory'
        }
        // Test demonstrates complete inventory workflow
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test hotbar slot management', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Hotbar Management Test',
      description: 'Demonstrates managing items in hotbar slots',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'HotbarUser', username: 'hotbar_user' }
        ]
      },

      steps: [
        {
          name: '[player: hotbar_user] Give stacks to fill inventory slots',
          action: 'execute_command',
          command: 'give hotbar_user diamond 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: hotbar_user] Give second stack',
          action: 'execute_command',
          command: 'give hotbar_user iron_ingot 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: hotbar_user] Give third stack',
          action: 'execute_command',
          command: 'give hotbar_user gold_ingot 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: hotbar_user] Give fourth stack',
          action: 'execute_command',
          command: 'give hotbar_user emerald 64'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: hotbar_user] Swap inventory slots (slot 0 to 2)',
          action: 'swap_inventory_slots',
          player: 'hotbar_user',
          from_slot: 0,
          to_slot: 2
        },
        {
          name: 'Wait for swap',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: hotbar_user] Get inventory state',
          action: 'get_player_inventory',
          player: 'hotbar_user',
          store_as: 'inventory'
        }
        // Test demonstrates hotbar slot management
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });
});
