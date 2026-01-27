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
 *
 * Note: Some tests are skipped as they require additional plugins:
 * - navigate_to: requires mineflayer-pathfinder plugin
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Advanced Features Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  // Pathfinding tests - mineflayer-pathfinder plugin loaded and functional
  // Uses GoalNear for flexible navigation to nearby locations
  describe('Pathfinding', () => {
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
            name: '[player: explorer] Navigate 5 blocks north',
            action: 'navigate_to',
            player: 'explorer',
            destination: {
              x: '{start_position.x}',
              y: '{start_position.y}',
              z: '{start_position.z}',
              offset: { x: 0, y: 0, z: -5 }
            },
            timeout_ms: 30000
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
            name: 'Verify bot moved at least 3 blocks',
            action: 'assert',
            condition: 'greater_than',
            actual: '{distance}',
            expected: 3
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

  describe('Container Interaction', () => {
    it('should test container interaction (chest)', async () => {
      const runner = new StoryRunner();

      const story = {
        name: 'Container Interaction Test - Chest',
        description: 'Demonstrates opening a chest container',

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
            name: '[RCON] Create a chest with items at bot position',
            action: 'execute_command',
            command: 'setblock {position.x} {position.y} {position.z} chest{Items:[{id:"minecraft:diamond",Count:64}]}'
          },
          {
            name: 'Wait for chest creation',
            action: 'wait',
            duration: 1
          },
          {
            name: '[player: looter] Look at chest position',
            action: 'look_at',
            player: 'looter',
            position: {
              x: '{position.x}',
              y: '{position.y}',
              z: '{position.z}'
            }
          },
          {
            name: '[player: looter] Interact with chest block',
            action: 'interact_with_block',
            player: 'looter',
            location: {
              x: '{position.x}',
              y: '{position.y}',
              z: '{position.z}'
            }
          },
          {
            name: 'Wait for chest to open',
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
  });

  // Crafting tests - using plank item directly
  describe('Crafting', () => {
    it('should test crafting sticks from oak planks', async () => {
      const runner = new StoryRunner();

      const story = {
        name: 'Crafting Test - Planks to Sticks',
        description: 'Demonstrates crafting sticks from planks',

        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [
            { name: 'Crafter', username: 'crafter' }
          ]
        },

        steps: [
          {
            name: '[player: crafter] Give oak planks',
            action: 'execute_command',
            player: 'crafter',
            command: '/give @p minecraft:oak_planks 64'
          },
          {
            name: 'Wait for items and inventory update',
            action: 'wait',
            duration: 3
          },
          {
            name: '[player: crafter] Craft sticks from planks',
            action: 'craft_item',
            player: 'crafter',
            item_name: 'minecraft:stick',
            count: 4
          },
          {
            name: 'Wait for crafting',
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
  });
});
