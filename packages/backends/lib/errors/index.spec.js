/**
 * Error Hierarchy Tests
 *
 * Tests all error classes to ensure:
 * - Proper inheritance
 * - Correct error codes
 * - Proper error formatting
 * - toJSON and toString methods work
 */

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
} = require('../index.js');

describe('PilafError (Base)', () => {
  it('should create error with code and message', () => {
    const error = new PilafError('TEST_CODE', 'Test message');

    expect(error.name).toBe('PilafError');
    expect(error.code).toBe('TEST_CODE');
    expect(error.message).toBe('Test message');
  });

  it('should accept details object', () => {
    const details = { key: 'value', number: 123 };
    const error = new PilafError('TEST', 'Message', details);

    expect(error.details).toEqual(details);
  });

  it('should accept cause error', () => {
    const cause = new Error('Original error');
    const error = new PilafError('TEST', 'Message', {}, cause);

    expect(error.cause).toBe(cause);
  });

  it('should capture stack trace', () => {
    const error = new PilafError('TEST', 'Message');

    expect(error.stack).toBeDefined();
    expect(error.stack).toContain('PilafError');
  });

  it('should convert to JSON', () => {
    const details = { key: 'value' };
    const error = new PilafError('TEST_CODE', 'Test message', details);

    const json = error.toJSON();

    expect(json.name).toBe('PilafError');
    expect(json.code).toBe('TEST_CODE');
    expect(json.message).toBe('Test message');
    expect(json.details).toEqual(details);
  });

  it('should include cause in JSON', () => {
    const cause = new Error('Original');
    const error = new PilafError('TEST', 'Message', {}, cause);

    const json = error.toJSON();

    expect(json.cause).toBeDefined();
    expect(json.cause.name).toBe('Error');
    expect(json.cause.message).toBe('Original');
  });

  it('should format as string', () => {
    const error = new PilafError('TEST_CODE', 'Test message');

    const str = error.toString();

    expect(str).toContain('[TEST_CODE]');
    expect(str).toContain('Test message');
  });

  it('should include details in string', () => {
    const error = new PilafError('TEST', 'Message', { key: 'value' });

    const str = error.toString();

    expect(str).toContain('Details:');
    expect(str).toContain('key');
  });

  it('should include cause in string', () => {
    const cause = new Error('Original');
    const error = new PilafError('TEST', 'Message', {}, cause);

    const str = error.toString();

    expect(str).toContain('Caused by:');
    expect(str).toContain('Original');
  });
});

describe('ConnectionError', () => {
  it('should create connection error', () => {
    const error = new ConnectionError('Connection failed');

    expect(error.name).toBe('ConnectionError');
    expect(error.code).toBe('CONNECTION_ERROR');
    expect(error).toBeInstanceOf(PilafError);
  });

  it('should accept details', () => {
    const error = new ConnectionError('Failed', { host: 'localhost', port: 25575 });

    expect(error.details.host).toBe('localhost');
    expect(error.details.port).toBe(25575);
  });
});

describe('RconConnectionError', () => {
  it('should create RCON connection error', () => {
    const error = new RconConnectionError('RCON failed');

    expect(error.name).toBe('RconConnectionError');
    expect(error.code).toBe('RCON_CONNECTION_ERROR');
    expect(error).toBeInstanceOf(ConnectionError);
    expect(error.details.component).toBe('RCON');
  });

  it('should include RCON component in details', () => {
    const error = new RconConnectionError('Failed', { host: 'localhost' });

    expect(error.details.component).toBe('RCON');
    expect(error.details.host).toBe('localhost');
  });
});

describe('DockerConnectionError', () => {
  it('should create Docker connection error', () => {
    const error = new DockerConnectionError('Docker failed');

    expect(error.name).toBe('DockerConnectionError');
    expect(error.code).toBe('DOCKER_CONNECTION_ERROR');
    expect(error).toBeInstanceOf(ConnectionError);
    expect(error.details.component).toBe('Docker');
  });
});

describe('FileAccessError', () => {
  it('should create file access error', () => {
    const error = new FileAccessError('File not found');

    expect(error.name).toBe('FileAccessError');
    expect(error.code).toBe('FILE_ACCESS_ERROR');
    expect(error).toBeInstanceOf(ConnectionError);
    expect(error.details.component).toBe('FileSystem');
  });
});

describe('CommandExecutionError', () => {
  it('should create command execution error', () => {
    const error = new CommandExecutionError('Command failed');

    expect(error.name).toBe('CommandExecutionError');
    expect(error.code).toBe('COMMAND_ERROR');
    expect(error).toBeInstanceOf(PilafError);
  });
});

describe('CommandTimeoutError', () => {
  it('should create command timeout error', () => {
    const error = new CommandTimeoutError('/test command', 5000);

    expect(error.name).toBe('CommandTimeoutError');
    expect(error.code).toBe('COMMAND_TIMEOUT');
    expect(error.message).toContain('5000ms');
    expect(error.details.command).toBe('/test command');
    expect(error.details.timeout).toBe(5000);
  });
});

describe('CommandRejectedError', () => {
  it('should create command rejected error', () => {
    const error = new CommandRejectedError('/test', 'Unknown command');

    expect(error.name).toBe('CommandRejectedError');
    expect(error.code).toBe('COMMAND_REJECTED');
    expect(error.message).toContain('Unknown command');
    expect(error.details.command).toBe('/test');
    expect(error.details.reason).toBe('Unknown command');
  });
});

describe('ParseError', () => {
  it('should create parse error', () => {
    const error = new ParseError('Parse failed');

    expect(error.name).toBe('ParseError');
    expect(error.code).toBe('PARSE_ERROR');
    expect(error).toBeInstanceOf(PilafError);
  });
});

describe('MalformedLogError', () => {
  it('should create malformed log error', () => {
    const error = new MalformedLogError('[broken log', 'Missing closing bracket');

    expect(error.name).toBe('MalformedLogError');
    expect(error.code).toBe('MALFORMED_LOG');
    expect(error.message).toContain('Missing closing bracket');
    expect(error.details.line).toBe('[broken log');
  });
});

describe('UnknownPatternError', () => {
  it('should create unknown pattern error', () => {
    const error = new UnknownPatternError('[12:34:56] Unknown log format');

    expect(error.name).toBe('UnknownPatternError');
    expect(error.code).toBe('UNKNOWN_PATTERN');
    expect(error.message).toContain('does not match any known pattern');
    expect(error.details.line).toBe('[12:34:56] Unknown log format');
  });
});

describe('CorrelationError', () => {
  it('should create correlation error', () => {
    const error = new CorrelationError('Correlation failed');

    expect(error.name).toBe('CorrelationError');
    expect(error.code).toBe('CORRELATION_ERROR');
    expect(error).toBeInstanceOf(PilafError);
  });
});

describe('ResponseTimeoutError', () => {
  it('should create response timeout error', () => {
    const error = new ResponseTimeoutError('/test command', 5000);

    expect(error.name).toBe('ResponseTimeoutError');
    expect(error.code).toBe('CORRELATION_TIMEOUT');
    expect(error.message).toContain('5000ms');
    expect(error.details.command).toBe('/test command');
    expect(error.details.timeout).toBe(5000);
  });
});

describe('AmbiguousMatchError', () => {
  it('should create ambiguous match error', () => {
    const matches = [{ id: 1 }, { id: 2 }];
    const error = new AmbiguousMatchError('/test command', matches);

    expect(error.name).toBe('AmbiguousMatchError');
    expect(error.code).toBe('AMBIGUOUS_MATCH');
    expect(error.message).toContain('2 potential responses');
    expect(error.details.command).toBe('/test command');
    expect(error.details.matchCount).toBe(2);
  });
});

describe('ResourceError', () => {
  it('should create resource error', () => {
    const error = new ResourceError('Resource exhausted');

    expect(error.name).toBe('ResourceError');
    expect(error.code).toBe('RESOURCE_ERROR');
    expect(error).toBeInstanceOf(PilafError);
  });
});

describe('BufferOverflowError', () => {
  it('should create buffer overflow error', () => {
    const error = new BufferOverflowError(2000, 1000);

    expect(error.name).toBe('BufferOverflowError');
    expect(error.code).toBe('BUFFER_OVERFLOW');
    expect(error.message).toContain('2000');
    expect(error.message).toContain('1000');
    expect(error.details.size).toBe(2000);
    expect(error.details.limit).toBe(1000);
  });
});

describe('HandleExhaustedError', () => {
  it('should create handle exhausted error', () => {
    const error = new HandleExhaustedError('file');

    expect(error.name).toBe('HandleExhaustedError');
    expect(error.code).toBe('HANDLE_EXHAUSTED');
    expect(error.message).toContain('file');
    expect(error.details.resourceType).toBe('file');
  });
});

describe('Error Inheritance Chain', () => {
  it('should maintain instanceof for all errors', () => {
    const errors = [
      new RconConnectionError('test'),
      new DockerConnectionError('test'),
      new FileAccessError('test'),
      new CommandTimeoutError('/test', 5000),
      new CommandRejectedError('/test', 'reason'),
      new MalformedLogError('line', 'reason'),
      new UnknownPatternError('line'),
      new ResponseTimeoutError('/test', 5000),
      new AmbiguousMatchError('/test', []),
      new BufferOverflowError(2000, 1000),
      new HandleExhaustedError('file')
    ];

    errors.forEach(error => {
      expect(error).toBeInstanceOf(PilafError);
    });
  });

  it('should maintain instanceof for specific categories', () => {
    expect(new RconConnectionError('test')).toBeInstanceOf(ConnectionError);
    expect(new CommandTimeoutError('/test', 5000)).toBeInstanceOf(CommandExecutionError);
    expect(new MalformedLogError('line', 'reason')).toBeInstanceOf(ParseError);
    expect(new ResponseTimeoutError('/test', 5000)).toBeInstanceOf(CorrelationError);
    expect(new BufferOverflowError(2000, 1000)).toBeInstanceOf(ResourceError);
  });
});
