/**
 * Log Monitoring Module
 *
 * Provides continuous log monitoring with event processing and correlation.
 */

const { CircularBuffer } = require('./CircularBuffer.js');
const { LogMonitor } = require('./LogMonitor.js');
const { TagCorrelationStrategy, UsernameCorrelationStrategy } = require('./correlations/index.js');

module.exports = {
  CircularBuffer,
  LogMonitor,
  TagCorrelationStrategy,
  UsernameCorrelationStrategy
};
