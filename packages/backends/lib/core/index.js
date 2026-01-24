/**
 * Core Abstractions Index
 *
 * This module exports all core abstraction classes for the Pilaf backend system.
 * These abstractions define the interfaces that concrete implementations must follow.
 *
 * @module core/index
 */

const { LogCollector } = require('./LogCollector.js');
const { LogParser } = require('./LogParser.js');
const { CommandRouter } = require('./CommandRouter.js');
const { CorrelationStrategy } = require('./CorrelationStrategy.js');

module.exports = {
  // Abstract base classes
  LogCollector,
  LogParser,
  CommandRouter,
  CorrelationStrategy,

  // Factory functions (implemented by concrete modules)
  collectors: null,  // Will be set by collectors/index.js
  parsers: null,     // Will be set by parsers/index.js
  strategies: null   // Will be set by strategies/index.js
};
