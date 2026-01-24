/**
 * Correlation Strategies
 *
 * Concrete implementations of CorrelationStrategy for various correlation patterns.
 */

const { TagCorrelationStrategy } = require('./TagCorrelationStrategy.js');
const { UsernameCorrelationStrategy } = require('./UsernameCorrelationStrategy.js');

module.exports = {
  TagCorrelationStrategy,
  UsernameCorrelationStrategy
};
