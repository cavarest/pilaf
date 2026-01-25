/**
 * Advanced Features Example
 *
 * This example shows how to test advanced player actions including
 * pathfinding navigation, container interactions, and crafting.
 *
 * New actions demonstrated:
 * - navigate_to: Pathfinding navigation to a destination
 * - open_container: Open and interact with container blocks
 * - craft_item: Craft items using crafting table or inventory
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Advanced Features Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  it('should test pathfinding navigation', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Pathfinding Navigation Test',
      description: 'Demonstrates automatic pathfinding to a destination',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Explorer', username: 'explorer' }
        ]
      },

      steps: [
        {
          name: '[player: explorer] Get starting position',
          action: 'get_player_location',
          player: 'explorer',
          store_as: 'start_position'
        },
        {
          name: '[player: explorer] Navigate to distant location',
          action: 'navigate_to',
          player: 'explorer',
          destination: {
            x: '{start_position.x} + 20',
            y: '{start_position.y}',
            z: '{start_position.z} + 20'
          },
          timeout_ms: 15000
        },
        {
          name: 'Wait for navigation',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: explorer] Get final position',
          action: 'get_player_location',
          player: 'explorer',
          store_as: 'end_position'
        },
        {
          name: 'Calculate distance traveled',
          action: 'calculate_distance',
          from: '{start_position}',
          to: '{end_position}',
          store_as: 'distance'
        },
        {
          name: 'Verify navigation completed',
          action: 'assert',
          condition: 'greater_than',
          actual: '{distance}',
          expected: 25
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test container interaction (chest)', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Container Interaction Test - Chest',
      description: 'Demonstrates opening and interacting with a chest',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Looter', username: 'looter' }
        ]
      },

      steps: [
        {
          name: '[player: looter] Get starting position',
          action: 'get_player_location',
          player: 'looter',
          store_as: 'position'
        },
        {
          name: '[RCON] Create a chest with items',
          action: 'execute_command',
          command: 'setblock ~3 ~ ~ chest{Items:[{id:"minecraft:diamond",Count:64}]}'
        },
        {
          name: 'Wait for chest creation',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: looter] Look at chest',
          action: 'look_at',
          player: 'looter',
          position: {
            x: '{position.x} + 3',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: '[player: looter] Move closer to chest',
          action: 'move_forward',
          player: 'looter',
          duration: 2
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: looter] Open chest container',
          action: 'open_container',
          player: 'looter',
          location: {
            x: '{position.x} + 3',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: 'Wait for container to open',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: looter] Get inventory before taking',
          action: 'get_inventory',
          player: 'looter',
          store_as: 'inventory_before'
        },
        {
          name: '[player: looter] Take diamonds from chest',
          action: 'take_from_container',
          player: 'looter',
          location: {
            x: '{position.x} + 3',
            y: '{position.y}',
            z: '{position.z}'
          },
          item_name: 'diamond',
          count: 32
        },
        {
          name: 'Wait for item transfer',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: looter] Get inventory after taking',
          action: 'get_inventory',
          player: 'looter',
          store_as: 'inventory_after'
        },
        {
          name: 'Verify diamonds obtained',
          action: 'assert',
          condition: 'has_item',
          actual: '{inventory_after}',
          expected: 'diamond'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test crafting items', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Crafting Test',
      description: 'Demonstrates crafting items using a crafting table',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Crafter', username: 'crafter' }
        ]
      },

      steps: [
        {
          name: '[player: crafter] Get starting position',
          action: 'get_player_location',
          player: 'crafter',
          store_as: 'position'
        },
        {
          name: '[player: crafter] Give crafting materials',
          action: 'execute_player_command',
          player: 'crafter',
          command: '/give @p oak_log 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: crafter] Give crafting table',
          action: 'execute_player_command',
          player: 'crafter',
          command: '/give @p crafting_table'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[RCON] Place crafting table',
          action: 'execute_command',
          command: 'setblock ~2 ~ ~ crafting_table'
        },
        {
          name: 'Wait for placement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: crafter] Look at crafting table',
          action: 'look_at',
          player: 'crafter',
          position: {
            x: '{position.x} + 2',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: '[player: crafter] Move to crafting table',
          action: 'move_forward',
          player: 'crafter',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: crafter] Get inventory before crafting',
          action: 'get_inventory',
          player: 'crafter',
          store_as: 'inventory_before'
        },
        {
          name: '[player: crafter] Craft oak planks from logs',
          action: 'craft_item',
          player: 'crafter',
          recipe: 'oak_planks_from_log',
          count: 4,
          use_crafting_table: true,
          location: {
            x: '{position.x} + 2',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: 'Wait for crafting',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: crafter] Get inventory after crafting',
          action: 'get_inventory',
          player: 'crafter',
          store_as: 'inventory_after'
        },
        {
          name: 'Verify planks were crafted',
          action: 'assert',
          condition: 'has_item',
          actual: '{inventory_after}',
          expected: 'oak_planks'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test complex navigation with obstacles', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Complex Navigation Test',
      description: 'Demonstrates pathfinding around obstacles',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Pathfinder', username: 'pathfinder' }
        ]
      },

      steps: [
        {
          name: '[player: pathfinder] Get starting position',
          action: 'get_player_location',
          player: 'pathfinder',
          store_as: 'start'
        },
        {
          name: '[RCON] Create a wall obstacle',
          action: 'execute_command',
          command: 'setblock ~5 ~ ~ stone_brick'
        },
        {
          name: 'Wait for block placement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[RCON] Create more wall blocks',
          action: 'execute_command',
          command: 'setblock ~5 ~1 ~ stone_brick'
        },
        {
          name: 'Wait for block placement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[RCON] Create more wall blocks',
          action: 'execute_command',
          command: 'setblock ~5 ~2 ~ stone_brick'
        },
        {
          name: 'Wait for wall completion',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: pathfinder] Navigate around wall',
          action: 'navigate_to',
          player: 'pathfinder',
          destination: {
            x: '{start.x} + 10',
            y: '{start.y}',
            z: '{start.z}'
          },
          timeout_ms: 10000
        },
        {
          name: 'Wait for navigation',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: pathfinder] Get final position',
          action: 'get_player_location',
          player: 'pathfinder',
          store_as: 'end'
        },
        {
          name: 'Verify destination reached',
          action: 'assert',
          condition: 'position_near',
          actual: '{end}',
          expected: '{start.x} + 10, {start.y}, {start.z}',
          tolerance: 3
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test multi-container workflow', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Multi-Container Workflow Test',
      description: 'Demonstrates interacting with multiple containers',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'StorageManager', username: 'storage_mgr' }
        ]
      },

      steps: [
        {
          name: '[player: storage_mgr] Get starting position',
          action: 'get_player_location',
          player: 'storage_mgr',
          store_as: 'position'
        },
        {
          name: '[RCON] Create first chest (input)',
          action: 'execute_command',
          command: 'setblock ~3 ~ ~ chest{Items:[{id:"minecraft:oak_log",Count:64}]}'
        },
        {
          name: 'Wait for chest',
          action: 'wait',
          duration: 1
        },
        {
          name: '[RCON] Create second chest (output)',
          action: 'execute_command',
          command: 'setblock ~-3 ~ ~ chest'
        },
        {
          name: 'Wait for chest',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: storage_mgr] Look at first chest',
          action: 'look_at',
          player: 'storage_mgr',
          position: {
            x: '{position.x} + 3',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: '[player: storage_mgr] Move to first chest',
          action: 'move_forward',
          player: 'storage_mgr',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: storage_mgr] Open first container',
          action: 'open_container',
          player: 'storage_mgr',
          location: {
            x: '{position.x} + 3',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: 'Wait for container',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: storage_mgr] Take logs from first chest',
          action: 'take_from_container',
          player: 'storage_mgr',
          location: {
            x: '{position.x} + 3',
            y: '{position.y}',
            z: '{position.z}'
          },
          item_name: 'oak_log',
          count: 64
        },
        {
          name: 'Wait for transfer',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: storage_mgr] Turn to second chest',
          action: 'look_at',
          player: 'storage_mgr',
          position: {
            x: '{position.x} - 3',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: '[player: storage_mgr] Move to second chest',
          action: 'move_backward',
          player: 'storage_mgr',
          duration: 2
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: storage_mgr] Open second container',
          action: 'open_container',
          player: 'storage_mgr',
          location: {
            x: '{position.x} - 3',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: 'Wait for container',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: storage_mgr] Put logs in second chest',
          action: 'put_in_container',
          player: 'storage_mgr',
          location: {
            x: '{position.x} - 3',
            y: '{position.y}',
            z: '{position.z}'
          },
          item_name: 'oak_log',
          count: 64
        },
        {
          name: 'Wait for transfer',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: storage_mgr] Get inventory',
          action: 'get_inventory',
          player: 'storage_mgr',
          store_as: 'final_inventory'
        },
        {
          name: 'Verify logs moved between chests',
          action: 'assert',
          condition: 'item_not_in_inventory',
          actual: '{final_inventory}',
          expected: 'oak_log'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test advanced crafting workflow', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Advanced Crafting Workflow',
      description: 'Demonstrates crafting multiple items in sequence',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'MasterCrafter', username: 'master_crafter' }
        ]
      },

      steps: [
        {
          name: '[player: master_crafter] Give raw materials',
          action: 'execute_player_command',
          player: 'master_crafter',
          command: '/give @p oak_log 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: master_crafter] Give sticks',
          action: 'execute_player_command',
          player: 'master_crafter',
          command: '/give @p stick 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[RCON] Place crafting table',
          action: 'execute_command',
          command: 'setblock ~2 ~ ~ crafting_table'
        },
        {
          name: 'Wait for placement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: master_crafter] Craft oak planks',
          action: 'craft_item',
          player: 'master_crafter',
          recipe: 'oak_planks_from_log',
          count: 4,
          use_crafting_table: false
        },
        {
          name: 'Wait for crafting',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: master_crafter] Craft crafting table',
          action: 'craft_item',
          player: 'master_crafter',
          recipe: 'crafting_table',
          count: 1,
          use_crafting_table: false
        },
        {
          name: 'Wait for crafting',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: master_crafter] Craft sticks',
          action: 'craft_item',
          player: 'master_crafter',
          recipe: 'stick',
          count: 4,
          use_crafting_table: false
        },
        {
          name: 'Wait for crafting',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: master_crafter] Craft wooden pickaxe',
          action: 'craft_item',
          player: 'master_crafter',
          recipe: 'wooden_pickaxe',
          count: 1,
          use_crafting_table: true,
          location: { x: '~2', y: '~', z: '~' }
        },
        {
          name: 'Wait for crafting',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: master_crafter] Get inventory',
          action: 'get_inventory',
          player: 'master_crafter',
          store_as: 'final_inventory'
        },
        {
          name: 'Verify pickaxe was crafted',
          action: 'assert',
          condition: 'has_item',
          actual: '{final_inventory}',
          expected: 'wooden_pickaxe'
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
