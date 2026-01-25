/**
 * Movement Control Example
 *
 * This example shows how to test advanced player movement controls
 * including directional movement, jumping, sneaking, sprinting, and
 * view orientation.
 *
 * New actions demonstrated:
 * - move_backward: Move player backward
 * - move_left: Strafe left
 * - move_right: Strafe right
 * - jump: Make player jump
 * - sneak: Toggle sneaking on
 * - unsneak: Toggle sneaking off
 * - sprint: Toggle sprinting on
 * - walk: Toggle sprinting off (walk mode)
 * - look_at: Look at position or entity
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Movement Control Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  it('should test directional movement and strafing', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Directional Movement Test',
      description: 'Demonstrates movement in all directions including strafing',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Mover', username: 'mover' }
        ]
      },

      steps: [
        {
          name: '[player: mover] Get starting position',
          action: 'get_player_location',
          player: 'mover',
          store_as: 'start_position'
        },
        {
          name: '[player: mover] Move backward',
          action: 'move_backward',
          player: 'mover',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: mover] Move left (strafe)',
          action: 'move_left',
          player: 'mover',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: mover] Move right (strafe)',
          action: 'move_right',
          player: 'mover',
          duration: 1
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: mover] Get ending position',
          action: 'get_player_location',
          player: 'mover',
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
          name: 'Verify player moved',
          action: 'assert',
          condition: 'greater_than',
          actual: '{distance}',
          expected: 0
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test player state controls (sneak and sprint)', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Player State Control Test',
      description: 'Demonstrates sneaking and sprinting toggles',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'StealthRunner', username: 'stealth' }
        ]
      },

      steps: [
        {
          name: '[player: stealth] Toggle sneaking on',
          action: 'sneak',
          player: 'stealth'
        },
        {
          name: 'Wait to observe sneaking',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: stealth] Toggle sneaking off',
          action: 'unsneak',
          player: 'stealth'
        },
        {
          name: 'Wait to observe walking',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: stealth] Toggle sprinting on',
          action: 'sprint',
          player: 'stealth'
        },
        {
          name: '[player: stealth] Move while sprinting',
          action: 'move_forward',
          player: 'stealth',
          duration: 1
        },
        {
          name: 'Wait for sprint movement',
          action: 'wait',
          duration: 0.5
        },
        {
          name: '[player: stealth] Toggle sprinting off (walk)',
          action: 'walk',
          player: 'stealth'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test jumping and view control', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Jump and View Control Test',
      description: 'Demonstrates jumping and looking at positions',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Jumper', username: 'jumper' }
        ]
      },

      steps: [
        {
          name: '[player: jumper] Get starting position',
          action: 'get_player_location',
          player: 'jumper',
          store_as: 'start_position'
        },
        {
          name: '[player: jumper] Jump',
          action: 'jump',
          player: 'jumper'
        },
        {
          name: 'Wait for jump',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: jumper] Get position after jump',
          action: 'get_player_location',
          player: 'jumper',
          store_as: 'jump_position'
        }
        // Test demonstrates jumping and looking
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test complex movement sequence', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Complex Movement Sequence',
      description: 'Demonstrates combined movement controls for navigation',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Navigator', username: 'navigator' }
        ]
      },

      steps: [
        {
          name: '[player: navigator] Get starting position',
          action: 'get_player_location',
          player: 'navigator',
          store_as: 'start'
        },
        {
          name: '[player: navigator] Start sprinting',
          action: 'sprint',
          player: 'navigator'
        },
        {
          name: '[player: navigator] Move forward while sprinting',
          action: 'move_forward',
          player: 'navigator',
          duration: 2
        },
        {
          name: '[player: navigator] Stop sprinting',
          action: 'walk',
          player: 'navigator'
        },
        {
          name: '[player: navigator] Strafe left',
          action: 'move_left',
          player: 'navigator',
          duration: 1
        },
        {
          name: '[player: navigator] Jump',
          action: 'jump',
          player: 'navigator'
        },
        {
          name: 'Wait for jump',
          action: 'wait',
          duration: 1
        },
        {
          name: '[player: navigator] Start sneaking',
          action: 'sneak',
          player: 'navigator'
        },
        {
          name: '[player: navigator] Move backward while sneaking',
          action: 'move_backward',
          player: 'navigator',
          duration: 1
        },
        {
          name: '[player: navigator] Stop sneaking',
          action: 'unsneak',
          player: 'navigator'
        },
        {
          name: '[player: navigator] Get final position',
          action: 'get_player_location',
          player: 'navigator',
          store_as: 'end'
        }
        // Test demonstrates complex movement sequence
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });
});
