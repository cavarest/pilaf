/**
 * Pilaf Error Hierarchy
 *
 * Defines all error types used throughout the Pilaf backend system.
 * Follows a hierarchical structure with machine-readable codes and
 * human-readable messages.
 *
 * Error Hierarchy:
 * PilafError (base)
 * ├── ConnectionError
 * │   ├── RconConnectionError
 * │   ├── DockerConnectionError
 * │   └── FileAccessError
 * ├── CommandExecutionError
 * │   ├── CommandTimeoutError
 * │   └── CommandRejectedError
 * ├── ParseError
 * │   ├── MalformedLogError
 * │   └── UnknownPatternError
 * ├── CorrelationError
 * │   ├── ResponseTimeoutError
 * │   └── AmbiguousMatchError
 * └── ResourceError
 *     ├── BufferOverflowError
 *     └── HandleExhaustedError
 */

/**
 * Base Pilaf Error
 *
 * All Pilaf errors extend from this class.
 *
 * @class
 * @extends Error
 * @property {string} code - Machine-readable error code
 * @property {string} message - Human-readable error message
 * @property {Object} details - Additional debugging context
 * @property {Error} [cause] - Original error that caused this error
 */
class PilafError extends Error {
  /**
   * Create a PilafError
   * @param {string} code - Machine-readable error code (e.g., 'CONNECTION_FAILED')
   * @param {string} message - Human-readable error message
   * @param {Object} [details={}] - Additional debugging context
   * @param {Error} [cause] - Original error that caused this error
   */
  constructor(code, message, details = {}, cause = null) {
    super(message);

    /**
     * Error name (class name)
     * @type {string}
     */
    this.name = this.constructor.name;

    /**
     * Machine-readable error code
     * @type {string}
     */
    this.code = code;

    /**
     * Human-readable message
     * @type {string}
     */
    this.message = message;

    /**
     * Additional debugging context
     * @type {Object}
     */
    this.details = details;

    /**
     * Original cause (if any)
     * @type {Error|null}
     */
    this.cause = cause;

    // Maintain proper stack trace
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, this.constructor);
    }
  }

  /**
   * Convert error to JSON for logging/serialization
   * @returns {Object} - JSON representation of error
   */
  toJSON() {
    return {
      name: this.name,
      code: this.code,
      message: this.message,
      details: this.details,
      cause: this.cause ? {
        name: this.cause.name,
        message: this.cause.message,
        stack: this.cause.stack
      } : null,
      stack: this.stack
    };
  }

  /**
   * Pretty-print error for debugging
   * @returns {string} - Formatted error string
   */
  toString() {
    let output = `[${this.code}] ${this.message}`;

    if (Object.keys(this.details).length > 0) {
      output += `\nDetails: ${JSON.stringify(this.details, null, 2)}`;
    }

    if (this.cause) {
      output += `\nCaused by: ${this.cause.message}`;
    }

    return output;
  }
}

// ============================================================================
// CONNECTION ERRORS
// ============================================================================

/**
 * Base class for connection-related errors
 */
class ConnectionError extends PilafError {
  constructor(message, details = {}, cause = null) {
    super('CONNECTION_ERROR', message, details, cause);
  }
}

/**
 * RCON connection failure
 */
class RconConnectionError extends ConnectionError {
  constructor(message, details = {}, cause = null) {
    super(message, { ...details, component: 'RCON' }, cause);
    this.code = 'RCON_CONNECTION_ERROR';
  }
}

/**
 * Docker API connection failure
 */
class DockerConnectionError extends ConnectionError {
  constructor(message, details = {}, cause = null) {
    super(message, { ...details, component: 'Docker' }, cause);
    this.code = 'DOCKER_CONNECTION_ERROR';
  }
}

/**
 * File access failure
 */
class FileAccessError extends ConnectionError {
  constructor(message, details = {}, cause = null) {
    super(message, { ...details, component: 'FileSystem' }, cause);
    this.code = 'FILE_ACCESS_ERROR';
  }
}

// ============================================================================
// COMMAND EXECUTION ERRORS
// ============================================================================

/**
 * Base class for command execution errors
 */
class CommandExecutionError extends PilafError {
  constructor(message, details = {}, cause = null) {
    super('COMMAND_ERROR', message, details, cause);
  }
}

/**
 * Command execution timeout
 */
class CommandTimeoutError extends CommandExecutionError {
  constructor(command, timeout, details = {}) {
    super(
      `Command timed out after ${timeout}ms`,
      { ...details, command, timeout },
      null
    );
    this.code = 'COMMAND_TIMEOUT';
  }
}

/**
 * Command rejected by server
 */
class CommandRejectedError extends CommandExecutionError {
  constructor(command, reason, details = {}) {
    super(
      `Command rejected by server: ${reason}`,
      { ...details, command, reason },
      null
    );
    this.code = 'COMMAND_REJECTED';
  }
}

// ============================================================================
// PARSING ERRORS
// ============================================================================

/**
 * Base class for parsing errors
 */
class ParseError extends PilafError {
  constructor(message, details = {}, cause = null) {
    super('PARSE_ERROR', message, details, cause);
  }
}

/**
 * Malformed log line
 */
class MalformedLogError extends ParseError {
  constructor(line, reason, details = {}) {
    super(
      `Malformed log line: ${reason}`,
      { ...details, line },
      null
    );
    this.code = 'MALFORMED_LOG';
  }
}

/**
 * Unknown log pattern
 */
class UnknownPatternError extends ParseError {
  constructor(line, details = {}) {
    super(
      'Log line does not match any known pattern',
      { ...details, line },
      null
    );
    this.code = 'UNKNOWN_PATTERN';
  }
}

// ============================================================================
// CORRELATION ERRORS
// ============================================================================

/**
 * Base class for correlation errors
 */
class CorrelationError extends PilafError {
  constructor(message, details = {}, cause = null) {
    super('CORRELATION_ERROR', message, details, cause);
  }
}

/**
 * Response correlation timeout
 */
class ResponseTimeoutError extends CorrelationError {
  constructor(command, timeout, details = {}) {
    // Call parent with generic message, then override code
    super(`No response received within ${timeout}ms`, { ...details, command, timeout }, null);
    // Override the parent's code
    this.code = 'CORRELATION_TIMEOUT';
  }
}

/**
 * Ambiguous match (multiple potential responses)
 */
class AmbiguousMatchError extends CorrelationError {
  constructor(command, matches, details = {}) {
    super(
      `${matches.length} potential responses found for command`,
      { ...details, command, matchCount: matches.length },
      null
    );
    this.code = 'AMBIGUOUS_MATCH';
  }
}

// ============================================================================
// RESOURCE ERRORS
// ============================================================================

/**
 * Base class for resource-related errors
 */
class ResourceError extends PilafError {
  constructor(message, details = {}, cause = null) {
    super('RESOURCE_ERROR', message, details, cause);
  }
}

/**
 * Buffer overflow
 */
class BufferOverflowError extends ResourceError {
  constructor(size, limit, details = {}) {
    super(
      `Buffer size (${size}) exceeds limit (${limit})`,
      { ...details, size, limit },
      null
    );
    this.code = 'BUFFER_OVERFLOW';
  }
}

/**
 * Handle/file descriptor exhaustion
 */
class HandleExhaustedError extends ResourceError {
  constructor(resourceType, details = {}) {
    super(
      `Unable to allocate ${resourceType} handle`,
      { ...details, resourceType },
      null
    );
    this.code = 'HANDLE_EXHAUSTED';
  }
}

// ============================================================================
// EXPORTS
// ============================================================================

module.exports = {
  // Base error
  PilafError,

  // Connection errors
  ConnectionError,
  RconConnectionError,
  DockerConnectionError,
  FileAccessError,

  // Command errors
  CommandExecutionError,
  CommandTimeoutError,
  CommandRejectedError,

  // Parse errors
  ParseError,
  MalformedLogError,
  UnknownPatternError,

  // Correlation errors
  CorrelationError,
  ResponseTimeoutError,
  AmbiguousMatchError,

  // Resource errors
  ResourceError,
  BufferOverflowError,
  HandleExhaustedError
};
