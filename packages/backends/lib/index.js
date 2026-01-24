/**
 * Pilaf Backends Package - Main export
 */

const { PilafBackend } = require('./backend.js');
const { RconBackend } = require('./rcon-backend.js');
const { MineflayerBackend } = require('./mineflayer-backend.js');
const { PilafBackendFactory } = require('./factory.js');
const { ConnectionState } = require('./ConnectionState.js');
const { BotLifecycleManager } = require('./BotLifecycleManager.js');
const { ServerHealthChecker } = require('./ServerHealthChecker.js');
const { BotPool } = require('./BotPool.js');

// Core abstractions
const { LogCollector, LogParser, CommandRouter, CorrelationStrategy } = require('./core/index.js');

// Collectors
const { DockerLogCollector } = require('./collectors/index.js');

// Parsers
const { MinecraftLogParser } = require('./parsers/index.js');

// Monitoring
const { CircularBuffer, LogMonitor, TagCorrelationStrategy, UsernameCorrelationStrategy } = require('./monitoring/index.js');

// Helpers
const { QueryHelper, EventObserver } = require('./helpers/index.js');

// Errors
const {
  PilafError,
  ConnectionError,
  RconConnectionError,
  DockerConnectionError,
  FileAccessError,
  CommandExecutionError,
  CommandTimeoutError,
  CommandRejectedError,
  ParseError,
  MalformedLogError,
  UnknownPatternError,
  CorrelationError,
  ResponseTimeoutError,
  AmbiguousMatchError,
  ResourceError,
  BufferOverflowError,
  HandleExhaustedError
} = require('./errors/index.js');

module.exports = {
  PilafBackend,
  RconBackend,
  MineflayerBackend,
  PilafBackendFactory,
  ConnectionState,
  BotLifecycleManager,
  ServerHealthChecker,
  BotPool,
  // Core abstractions
  LogCollector,
  LogParser,
  CommandRouter,
  CorrelationStrategy,
  // Collectors
  DockerLogCollector,
  // Parsers
  MinecraftLogParser,
  // Monitoring
  CircularBuffer,
  LogMonitor,
  TagCorrelationStrategy,
  UsernameCorrelationStrategy,
  // Helpers
  QueryHelper,
  EventObserver,
  // Errors
  PilafError,
  ConnectionError,
  RconConnectionError,
  DockerConnectionError,
  FileAccessError,
  CommandExecutionError,
  CommandTimeoutError,
  CommandRejectedError,
  ParseError,
  MalformedLogError,
  UnknownPatternError,
  CorrelationError,
  ResponseTimeoutError,
  AmbiguousMatchError,
  ResourceError,
  BufferOverflowError,
  HandleExhaustedError
};
