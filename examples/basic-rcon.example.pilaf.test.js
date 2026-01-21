/**
 * Basic RCON Command Example
 *
 * This example shows how to execute basic RCON commands
 * and verify server responses.
 */

const { describe, it, expect } = require('@jest/globals');
const { StoryRunner } = require('@pilaf/framework');

describe('Basic RCON Examples', () => {
  it('should execute simple RCON commands', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Basic RCON Commands',
      description: 'Demonstrates basic RCON command execution',

      setup: {
        server: { type: 'paper', version: '1.21.8' }
      },

      steps: [
        {
          name: '[RCON] Get server version',
          action: 'execute_command',
          command: 'version'
        },
        {
          name: '[RCON] List online players',
          action: 'execute_command',
          command: 'list'
        },
        {
          name: '[RCON] Get current time',
          action: 'execute_command',
          command: 'time query daytime'
        },
        {
          name: '[RCON] Get difficulty',
          action: 'execute_command',
          command: 'difficulty'
        }
      ],

      teardown: {
        stop_server: false
      }
    };

    const result = await runner.execute(story);
    expect(result.success).toBe(true);
  });

  it('should change server settings via RCON', async () => {
    const runner = new StoryRunner();

    const story = {
      name: 'Change Server Settings',
      description: 'Demonstrates changing server settings via RCON',

      setup: {
        server: { type: 'paper', version: '1.21.8' }
      },

      steps: [
        {
          name: '[RCON] Set time to day',
          action: 'execute_command',
          command: 'time set day'
        },
        {
          name: '[RCON] Set weather to clear',
          action: 'execute_command',
          command: 'weather clear'
        },
        {
          name: '[RCON] Set difficulty to easy',
          action: 'execute_command',
          command: 'difficulty easy'
        },
        {
          name: '[RCON] Restore normal difficulty',
          action: 'execute_command',
          command: 'difficulty normal'
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
