/**
 * Player Interaction Example
 *
 * This example shows how to test player interactions including
 * chat, commands, and movement.
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Player Interaction Examples', () => {
  // Add delay between tests to prevent connection throttling
  beforeEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 5000));
  });

  it('should test player chat and commands', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Player Chat and Commands',
      description: 'Demonstrates player chat and command execution',

      setup: {
        server: { type: 'paper', version: '1.21.8' },
        players: [
          { name: 'Test Player', username: 'tester' }
        ]
      },

      steps: [
        {
          name: '[RCON] Make player operator',
          action: 'execute_command',
          command: 'op tester'
        },
        {
          name: '[player: tester] Send chat message',
          action: 'chat',
          player: 'tester',
          message: 'Hello from Pilaf!'
        },
        {
          name: '[player: tester] Execute player command',
          action: 'execute_player_command',
          player: 'tester',
          command: '/gamemode creative'
        },
        {
          name: 'Wait for command processing',
          action: 'wait',
          duration: 2
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should test player movement and position', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Player Movement Test',
      description: 'Demonstrates player movement and position tracking',

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
          name: '[player: mover] Move forward',
          action: 'move_forward',
          player: 'mover',
          duration: 2
        },
        {
          name: 'Wait for movement',
          action: 'wait',
          duration: 1
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
});
