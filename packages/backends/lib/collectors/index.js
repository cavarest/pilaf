/**
 * Log Collectors
 *
 * Concrete implementations of LogCollector for various log sources.
 */

module.exports = {
  DockerLogCollector: require('./DockerLogCollector.js').DockerLogCollector
};
