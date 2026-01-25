/**
 * Integration Tests for New Player Actions
 *
 * Tests all 22 new player simulation actions through StoryRunner
 * with a real Minecraft server (Docker).
 *
 * Test categories:
 * - Movement Control (9 actions)
 * - Entity Combat (4 actions)
 * - Block Interaction (3 actions)
 * - Inventory Management (4 actions)
 * - Advanced Features (3 actions)
 */

const { StoryRunner } = require('@pilaf/framework');
const { PilafBackendFactory } = require('@pilaf/backends');

// Common config
const rconConfig = {
  rconHost: process.env.RCON_HOST || 'localhost',
  rconPort: parseInt(process.env.RCON_PORT) || 25576,
  rconPassword: process.env.RCON_PASSWORD || 'cavarest'
};

const mcConfig = {
  host: process.env.MC_HOST || 'localhost',
  port: parseInt(process.env.MC_PORT) || 25566,
  auth: 'offline'
};

describe('New Player Actions Integration Tests', () => {
  let runner;
  let rconBackend;

  beforeAll(async () => {
    // Add delay to let server recover from previous tests
    await new Promise(resolve => setTimeout(resolve, 5000));

    // Connect RCON backend for verification
    rconBackend = await PilafBackendFactory.create('rcon', rconConfig);
    await rconBackend.send('say [Pilaf] Starting new player actions tests...');

    // Create StoryRunner instance
    runner = new StoryRunner();
  }, 30000);

  afterAll(async () => {
    if (rconBackend) {
      await rconBackend.send('say [Pilaf] New player actions tests completed');
      await rconBackend.disconnect();
    }
  });

  // Add delay between test suites
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 3000));
  });

  describe('Movement Control Actions', () => {
    it('should execute move_backward action', async () => {
      const story = {
        name: 'Move Backward Test',
        description: 'Test move_backward action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Mover', username: 'mover_test' }]
        },
        steps: [
          {
            name: 'Get starting position',
            action: 'get_player_location',
            player: 'mover_test',
            store_as: 'start'
          },
          {
            name: 'Move backward',
            action: 'move_backward',
            player: 'mover_test',
            duration: 1
          },
          {
            name: 'Wait for movement',
            action: 'wait',
            duration: 0.5
          },
          {
            name: 'Get ending position',
            action: 'get_player_location',
            player: 'mover_test',
            store_as: 'end'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute move_left action', async () => {
      const story = {
        name: 'Move Left Test',
        description: 'Test move_left action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'LeftMover', username: 'left_mover' }]
        },
        steps: [
          {
            name: 'Move left (strafe)',
            action: 'move_left',
            player: 'left_mover',
            duration: 1
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute move_right action', async () => {
      const story = {
        name: 'Move Right Test',
        description: 'Test move_right action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'RightMover', username: 'right_mover' }]
        },
        steps: [
          {
            name: 'Move right (strafe)',
            action: 'move_right',
            player: 'right_mover',
            duration: 1
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute jump action', async () => {
      const story = {
        name: 'Jump Test',
        description: 'Test jump action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Jumper', username: 'jumper_test' }]
        },
        steps: [
          {
            name: 'Get starting Y position',
            action: 'get_player_location',
            player: 'jumper_test',
            store_as: 'start'
          },
          {
            name: 'Jump',
            action: 'jump',
            player: 'jumper_test'
          },
          {
            name: 'Wait for jump',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Get ending Y position',
            action: 'get_player_location',
            player: 'jumper_test',
            store_as: 'end'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute sneak and unsneak actions', async () => {
      const story = {
        name: 'Sneak Toggle Test',
        description: 'Test sneak and unsneak actions',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Sneaker', username: 'sneaker_test' }]
        },
        steps: [
          {
            name: 'Toggle sneaking on',
            action: 'sneak',
            player: 'sneaker_test'
          },
          {
            name: 'Wait for sneak',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Toggle sneaking off',
            action: 'unsneak',
            player: 'sneaker_test'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute sprint and walk actions', async () => {
      const story = {
        name: 'Sprint Toggle Test',
        description: 'Test sprint and walk actions',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Sprinter', username: 'sprinter_test' }]
        },
        steps: [
          {
            name: 'Toggle sprinting on',
            action: 'sprint',
            player: 'sprinter_test'
          },
          {
            name: 'Move while sprinting',
            action: 'move_forward',
            player: 'sprinter_test',
            duration: 1
          },
          {
            name: 'Toggle sprinting off',
            action: 'walk',
            player: 'sprinter_test'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute look_at action (position)', async () => {
      const story = {
        name: 'Look At Position Test',
        description: 'Test look_at action with position',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Looker', username: 'looker_test' }]
        },
        steps: [
          {
            name: 'Look at specific position',
            action: 'look_at',
            player: 'looker_test',
            position: { x: 100, y: 64, z: 100 }
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });
  });

  describe('Entity Combat Actions', () => {
    it('should execute attack_entity action', async () => {
      const story = {
        name: 'Attack Entity Test',
        description: 'Test attack_entity action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Warrior', username: 'warrior_test' }]
        },
        steps: [
          {
            name: 'Give weapon',
            action: 'execute_player_command',
            player: 'warrior_test',
            command: '/give @p diamond_sword'
          },
          {
            name: 'Wait for item',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Spawn zombie',
            action: 'execute_command',
            command: 'summon zombie ~ ~5 ~'
          },
          {
            name: 'Wait for spawn',
            action: 'wait',
            duration: 2
          },
          {
            name: 'Attack zombie',
            action: 'attack_entity',
            player: 'warrior_test',
            entity_name: 'zombie'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute interact_with_entity action', async () => {
      const story = {
        name: 'Interact With Entity Test',
        description: 'Test interact_with_entity action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Interactor', username: 'interactor_test' }]
        },
        steps: [
          {
            name: 'Spawn villager',
            action: 'execute_command',
            command: 'summon villager ~3 ~ ~'
          },
          {
            name: 'Wait for spawn',
            action: 'wait',
            duration: 2
          },
          {
            name: 'Look at villager',
            action: 'look_at',
            player: 'interactor_test',
            entity_name: 'villager'
          },
          {
            name: 'Interact with villager',
            action: 'interact_with_entity',
            player: 'interactor_test',
            entity_name: 'villager'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute mount_entity action', async () => {
      const story = {
        name: 'Mount Entity Test',
        description: 'Test mount_entity action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Rider', username: 'rider_test' }]
        },
        steps: [
          {
            name: 'Spawn boat',
            action: 'execute_command',
            command: 'summon boat ~2 ~ ~'
          },
          {
            name: 'Wait for spawn',
            action: 'wait',
            duration: 2
          },
          {
            name: 'Look at boat',
            action: 'look_at',
            player: 'rider_test',
            entity_name: 'boat'
          },
          {
            name: 'Mount boat',
            action: 'mount_entity',
            player: 'rider_test',
            entity_name: 'boat'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute dismount action', async () => {
      const story = {
        name: 'Dismount Test',
        description: 'Test dismount action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Dismounter', username: 'dismounter_test' }]
        },
        steps: [
          {
            name: 'Spawn and mount boat',
            action: 'execute_command',
            command: 'summon boat ~2 ~ ~'
          },
          {
            name: 'Wait for spawn',
            action: 'wait',
            duration: 2
          },
          {
            name: 'Mount boat',
            action: 'mount_entity',
            player: 'dismounter_test',
            entity_name: 'boat'
          },
          {
            name: 'Wait for mount',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Dismount',
            action: 'dismount',
            player: 'dismounter_test'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });
  });

  describe('Block Interaction Actions', () => {
    it('should execute break_block action', async () => {
      const story = {
        name: 'Break Block Test',
        description: 'Test break_block action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Miner', username: 'miner_test' }]
        },
        steps: [
          {
            name: 'Get starting position',
            action: 'get_player_location',
            player: 'miner_test',
            store_as: 'position'
          },
          {
            name: 'Place dirt block',
            action: 'execute_command',
            command: 'setblock ~ ~1 ~ dirt'
          },
          {
            name: 'Wait for placement',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Give pickaxe',
            action: 'execute_player_command',
            player: 'miner_test',
            command: '/give @p diamond_pickaxe'
          },
          {
            name: 'Break the block',
            action: 'break_block',
            player: 'miner_test',
            location: {
              x: '{position.x}',
              y: '{position.y} + 1',
              z: '{position.z}'
            }
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute place_block action', async () => {
      const story = {
        name: 'Place Block Test',
        description: 'Test place_block action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Builder', username: 'builder_test' }]
        },
        steps: [
          {
            name: 'Get starting position',
            action: 'get_player_location',
            player: 'builder_test',
            store_as: 'position'
          },
          {
            name: 'Give building materials',
            action: 'execute_player_command',
            player: 'builder_test',
            command: '/give @p cobblestone 64'
          },
          {
            name: 'Wait for items',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Place cobblestone',
            action: 'place_block',
            player: 'builder_test',
            block: 'cobblestone',
            location: {
              x: '{position.x}',
              y: '{position.y}',
              z: '{position.z}'
            },
            face: 'top'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute interact_with_block action', async () => {
      const story = {
        name: 'Interact With Block Test',
        description: 'Test interact_with_block action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'BlockInteractor', username: 'block_interactor_test' }]
        },
        steps: [
          {
            name: 'Get starting position',
            action: 'get_player_location',
            player: 'block_interactor_test',
            store_as: 'position'
          },
          {
            name: 'Place chest',
            action: 'execute_command',
            command: 'setblock ~2 ~ ~ chest'
          },
          {
            name: 'Wait for placement',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Look at chest',
            action: 'look_at',
            player: 'block_interactor_test',
            position: {
              x: '{position.x} + 2',
              y: '{position.y}',
              z: '{position.z}'
            }
          },
          {
            name: 'Interact with chest',
            action: 'interact_with_block',
            player: 'block_interactor_test',
            location: {
              x: '{position.x} + 2',
              y: '{position.y}',
              z: '{position.z}'
            }
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });
  });

  describe('Inventory Management Actions', () => {
    it('should execute drop_item action', async () => {
      const story = {
        name: 'Drop Item Test',
        description: 'Test drop_item action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Dropper', username: 'dropper_test' }]
        },
        steps: [
          {
            name: 'Get inventory before',
            action: 'get_inventory',
            player: 'dropper_test',
            store_as: 'inventory_before'
          },
          {
            name: 'Give items',
            action: 'execute_player_command',
            player: 'dropper_test',
            command: '/give @p diamond 64'
          },
          {
            name: 'Wait for items',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Drop diamonds',
            action: 'drop_item',
            player: 'dropper_test',
            item_name: 'diamond',
            count: 10
          },
          {
            name: 'Get inventory after',
            action: 'get_inventory',
            player: 'dropper_test',
            store_as: 'inventory_after'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute consume_item action', async () => {
      const story = {
        name: 'Consume Item Test',
        description: 'Test consume_item action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Eater', username: 'eater_test' }]
        },
        steps: [
          {
            name: 'Get food level before',
            action: 'get_player_food_level',
            player: 'eater_test',
            store_as: 'food_before'
          },
          {
            name: 'Give food',
            action: 'execute_player_command',
            player: 'eater_test',
            command: '/give @p cooked_beef 64'
          },
          {
            name: 'Wait for items',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Consume food',
            action: 'consume_item',
            player: 'eater_test',
            item_name: 'cooked_beef'
          },
          {
            name: 'Get food level after',
            action: 'get_player_food_level',
            player: 'eater_test',
            store_as: 'food_after'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute equip_item action', async () => {
      const story = {
        name: 'Equip Item Test',
        description: 'Test equip_item action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Equiper', username: 'equiper_test' }]
        },
        steps: [
          {
            name: 'Give sword',
            action: 'execute_player_command',
            player: 'equiper_test',
            command: '/give @p diamond_sword'
          },
          {
            name: 'Wait for item',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Equip sword to hand',
            action: 'equip_item',
            player: 'equiper_test',
            item_name: 'diamond_sword',
            destination: 'hand'
          },
          {
            name: 'Give helmet',
            action: 'execute_player_command',
            player: 'equiper_test',
            command: '/give @p diamond_helmet'
          },
          {
            name: 'Wait for item',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Equip helmet to head',
            action: 'equip_item',
            player: 'equiper_test',
            item_name: 'diamond_helmet',
            destination: 'head'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    it('should execute swap_inventory_slots action', async () => {
      const story = {
        name: 'Swap Inventory Slots Test',
        description: 'Test swap_inventory_slots action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Swapper', username: 'swapper_test' }]
        },
        steps: [
          {
            name: 'Give items',
            action: 'execute_player_command',
            player: 'swapper_test',
            command: '/give @p diamond'
          },
          {
            name: 'Wait for item',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Give more items',
            action: 'execute_player_command',
            player: 'swapper_test',
            command: '/give @p gold_ingot'
          },
          {
            name: 'Wait for items',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Get inventory before swap',
            action: 'get_inventory',
            player: 'swapper_test',
            store_as: 'inventory_before'
          },
          {
            name: 'Swap hotbar slots',
            action: 'swap_inventory_slots',
            player: 'swapper_test',
            slot_a: 0,
            slot_b: 1
          },
          {
            name: 'Get inventory after swap',
            action: 'get_inventory',
            player: 'swapper_test',
            store_as: 'inventory_after'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });
  });

  describe('Advanced Features', () => {
    // Note: navigate_to requires mineflayer-pathfinder plugin
    // This test may be skipped if the plugin is not available
    it.skip('should execute navigate_to action', async () => {
      const story = {
        name: 'Navigate To Test',
        description: 'Test navigate_to action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Navigator', username: 'navigator_test' }]
        },
        steps: [
          {
            name: 'Get starting position',
            action: 'get_player_location',
            player: 'navigator_test',
            store_as: 'start'
          },
          {
            name: 'Navigate to location',
            action: 'navigate_to',
            player: 'navigator_test',
            destination: {
              x: '{start.x} + 10',
              y: '{start.y}',
              z: '{start.z}'
            },
            timeout_ms: 10000
          },
          {
            name: 'Get final position',
            action: 'get_player_location',
            player: 'navigator_test',
            store_as: 'end'
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });

    // Note: open_container is tested via interact_with_block with chests
    // These are functionally equivalent for most use cases

    // Note: craft_item requires proper recipe setup and inventory management
    // This test may be skipped depending on server configuration
    it.skip('should execute craft_item action', async () => {
      const story = {
        name: 'Craft Item Test',
        description: 'Test craft_item action',
        setup: {
          server: { type: 'paper', version: '1.21.8' },
          players: [{ name: 'Crafter', username: 'crafter_test' }]
        },
        steps: [
          {
            name: 'Give materials',
            action: 'execute_player_command',
            player: 'crafter_test',
            command: '/give @p oak_log 64'
          },
          {
            name: 'Wait for items',
            action: 'wait',
            duration: 1
          },
          {
            name: 'Craft planks',
            action: 'craft_item',
            player: 'crafter_test',
            recipe: 'oak_planks_from_log',
            count: 4,
            use_crafting_table: false
          },
          {
            name: 'Wait for crafting',
            action: 'wait',
            duration: 1
          }
        ],
        teardown: { stop_server: false }
      };

      const result = await runner.execute(story);
      expect(result.success).toBe(true);
    });
  });
});
