/**
 * Block Interaction Example
 *
 * This example shows how to test block interactions including
 * breaking blocks, placing blocks, and interacting with
 * block-based mechanisms (buttons, doors, chests, etc.).
 *
 * New actions demonstrated:
 * - break_block: Break a block at a specific location
 * - place_block: Place a block at a specific location
 * - interact_with_block: Right-click interaction with blocks
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Block Interaction Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  it('should test breaking blocks', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Block Breaking Test',
      description: 'Demonstrates breaking a dirt block',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Miner', username: 'miner' }
        ]
      },

      steps: [
        {
          name: '[player: miner] Get starting position',
          action: 'get_player_location',
          player: 'miner',
          store_as: 'player_position'
        },
        {
          name: '[RCON] Place a dirt block nearby',
          action: 'execute_command',
          command: 'setblock ~ ~1 ~ dirt'
        },
        {
          name: 'Wait for block placement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: miner] Give diamond pickaxe',
          action: 'execute_player_command',
          player: 'miner',
          command: '/give @p diamond_pickaxe'
        },
        {
          name: 'Wait for item',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: miner] Equip diamond pickaxe',
          action: 'equip_item',
          player: 'miner',
          item_name: 'diamond_pickaxe',
          destination: 'hand'
        },
        {
          name: '[player: miner] Look at block',
          action: 'look_at',
          player: 'miner',
          position: {
            x: '{player_position.x}',
            y: '{player_position.y} + 1',
            z: '{player_position.z}'
          }
        },
        {
          name: '[player: miner] Break the dirt block',
          action: 'break_block',
          player: 'miner',
          location: {
            x: '{player_position.x}',
            y: '{player_position.y} + 1',
            z: '{player_position.z}'
          },
          wait_for_drop: true
        },
        {
          name: 'Wait for block break',
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

  it('should test placing blocks', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Block Placing Test',
      description: 'Demonstrates placing blocks in different orientations',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Builder', username: 'builder' }
        ]
      },

      steps: [
        {
          name: '[player: builder] Get starting position',
          action: 'get_player_location',
          player: 'builder',
          store_as: 'position'
        },
        {
          name: '[player: builder] Give building materials',
          action: 'execute_player_command',
          player: 'builder',
          command: '/give @p cobblestone 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: builder] Place cobblestone on top face',
          action: 'place_block',
          player: 'builder',
          block: 'cobblestone',
          location: {
            x: '{position.x}',
            y: '{position.y}',
            z: '{position.z}'
          },
          face: 'top'
        },
        {
          name: 'Wait for placement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: builder] Move to side',
          action: 'move_left',
          player: 'builder',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: builder] Get new position',
          action: 'get_player_location',
          player: 'builder',
          store_as: 'new_position'
        },
        {
          name: '[player: builder] Place block on north face',
          action: 'place_block',
          player: 'builder',
          block: 'cobblestone',
          location: {
            x: '{position.x}',
            y: '{position.y} + 1',
            z: '{position.z}'
          },
          face: 'north'
        },
        {
          name: 'Wait for placement',
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

  it('should test interacting with blocks (chest)', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Block Interaction Test - Chest',
      description: 'Demonstrates opening a chest',

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
          name: '[RCON] Place a chest nearby',
          action: 'execute_command',
          command: 'setblock ~2 ~ ~ chest'
        },
        {
          name: 'Wait for chest placement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: looter] Look at chest',
          action: 'look_at',
          player: 'looter',
          position: {
            x: '{position.x} + 2',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: '[player: looter] Move closer to chest',
          action: 'move_forward',
          player: 'looter',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: looter] Interact with chest',
          action: 'interact_with_block',
          player: 'looter',
          location: {
            x: '{position.x} + 2',
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

  it('should test interacting with blocks (door)', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Block Interaction Test - Door',
      description: 'Demonstrates opening and closing a door',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Opener', username: 'opener' }
        ]
      },

      steps: [
        {
          name: '[player: opener] Get starting position',
          action: 'get_player_location',
          player: 'opener',
          store_as: 'position'
        },
        {
          name: '[RCON] Place an oak door',
          action: 'execute_command',
          command: 'setblock ~2 ~ ~ oak_door[half=lower,facing=east]'
        },
        {
          name: 'Wait for door placement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: opener] Look at door',
          action: 'look_at',
          player: 'opener',
          position: {
            x: '{position.x} + 2',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: '[player: opener] Move closer',
          action: 'move_forward',
          player: 'opener',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: opener] Open the door',
          action: 'interact_with_block',
          player: 'opener',
          location: {
            x: '{position.x} + 2',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: 'Wait for door to open',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: opener] Move through door',
          action: 'move_forward',
          player: 'opener',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: opener] Turn around',
          action: 'look_at',
          player: 'opener',
          position: {
            x: '{position.x}',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: '[player: opener] Close the door',
          action: 'interact_with_block',
          player: 'opener',
          location: {
            x: '{position.x} + 2',
            y: '{position.y}',
            z: '{position.z}'
          }
        },
        {
          name: 'Wait for door to close',
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

  it('should test building a simple structure', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Simple Building Test',
      description: 'Demonstrates building a 3x3 platform',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Architect', username: 'architect' }
        ]
      },

      steps: [
        {
          name: '[player: architect] Get starting position',
          action: 'get_player_location',
          player: 'architect',
          store_as: 'origin'
        },
        {
          name: '[player: architect] Give building materials',
          action: 'execute_player_command',
          player: 'architect',
          command: '/give @p stone 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: architect] Place first block (center)',
          action: 'place_block',
          player: 'architect',
          block: 'stone',
          location: {
            x: '{origin.x}',
            y: '{origin.y} - 1',
            z: '{origin.z}'
          },
          face: 'top'
        },
        {
          name: 'Wait for placement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: architect] Place second block (north)',
          action: 'place_block',
          player: 'architect',
          block: 'stone',
          location: {
            x: '{origin.x}',
            y: '{origin.y} - 1',
            z: '{origin.z} - 1'
          },
          face: 'top'
        },
        {
          name: 'Wait for placement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: architect] Place third block (east)',
          action: 'place_block',
          player: 'architect',
          block: 'stone',
          location: {
            x: '{origin.x} + 1',
            y: '{origin.y} - 1',
            z: '{origin.z}'
          },
          face: 'top'
        },
        {
          name: 'Wait for placement',
          action: 'wait',
          duration: 0.5
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test breaking and replacing blocks', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Block Replacement Test',
      description: 'Demonstrates breaking a block and replacing it with a different type',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Replacer', username: 'replacer' }
        ]
      },

      steps: [
        {
          name: '[player: replacer] Get starting position',
          action: 'get_player_location',
          player: 'replacer',
          store_as: 'position'
        },
        {
          name: '[RCON] Place a dirt block',
          action: 'execute_command',
          command: 'setblock ~ ~1 ~ dirt'
        },
        {
          name: 'Wait for placement',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: replacer] Give tools and materials',
          action: 'execute_player_command',
          player: 'replacer',
          command: '/give @p diamond_pickaxe 1'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: replacer] Give stone',
          action: 'execute_player_command',
          player: 'replacer',
          command: '/give @p stone 64'
        },
        {
          name: 'Wait for items',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: replacer] Equip pickaxe',
          action: 'equip_item',
          player: 'replacer',
          item_name: 'diamond_pickaxe',
          destination: 'hand'
        },
        {
          name: '[player: replacer] Break the dirt block',
          action: 'break_block',
          player: 'replacer',
          location: {
            x: '{position.x}',
            y: '{position.y} + 1',
            z: '{position.z}'
          }
        },
        {
          name: 'Wait for break',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: replacer] Equip stone',
          action: 'equip_item',
          player: 'replacer',
          item_name: 'stone',
          destination: 'hand'
        },
        {
          name: '[player: replacer] Place stone in same location',
          action: 'place_block',
          player: 'replacer',
          block: 'stone',
          location: {
            x: '{position.x}',
            y: '{position.y}',
            z: '{position.z}'
          },
          face: 'top'
        },
        {
          name: 'Wait for placement',
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
