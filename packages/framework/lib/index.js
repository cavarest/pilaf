// packages/framework/lib/index.js
const { PilafReporter } = require('./reporters/pilaf-reporter');
const { StoryRunner } = require('./StoryRunner');
const { waitForEvents, captureEvents } = require('./helpers/events');
const { captureState, compareStates } = require('./helpers/state');
const { toHaveReceivedLightningStrikes } = require('./matchers/game-matchers');

const { PilafBackendFactory } = require('@pilaf/backends');

// Backend helpers
const rcon = {
  connect: async (config) => PilafBackendFactory.create('rcon', config)
};

const mineflayer = {
  createBot: async (options) => {
    const backend = await PilafBackendFactory.create('mineflayer', options);
    return backend.createBot(options);
  }
};

// Main pilaf API
const pilaf = {
  waitForEvents,
  captureEvents,
  captureState,
  compareStates
};

module.exports = {
  // Reporter
  PilafReporter,

  // Story Runner
  StoryRunner,

  // Main API
  pilaf,
  rcon,
  mineflayer,

  // Helpers
  waitForEvents,
  captureEvents,
  captureState,
  compareStates,

  // Matchers
  toHaveReceivedLightningStrikes
};
