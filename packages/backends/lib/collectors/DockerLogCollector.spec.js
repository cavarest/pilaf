/**
 * DockerLogCollector Tests
 *
 * Tests the Docker log collector to ensure:
 * - Connection to Docker containers works
 * - Log streaming emits data events
 * - Reconnection with exponential backoff works
 * - ANSI code stripping works
 * - Error handling works correctly
 */

const { DockerLogCollector } = require('./DockerLogCollector.js');
const { DockerConnectionError } = require('../errors/index.js');

describe('DockerLogCollector', () => {
  let collector;
  let mockContainer;
  let mockStream;
  let mockDockerode;

  beforeEach(() => {
    jest.clearAllMocks();
    jest.resetModules();

    // Create fresh mock container
    mockContainer = {
      inspect: jest.fn().mockResolvedValue({ Id: 'abc123' }),
      logs: jest.fn()
    };

    // Create fresh mock stream
    mockStream = new (require('events').EventEmitter)();
    mockStream.destroy = jest.fn();

    // Mock Dockerode
    mockDockerode = jest.fn().mockImplementation(() => ({
      getContainer: jest.fn().mockReturnValue(mockContainer)
    }));

    jest.mock('dockerode', () => mockDockerode, { virtual: true });

    collector = new DockerLogCollector();
  });

  afterEach(async () => {
    if (collector && collector.connected) {
      await collector.disconnect();
    }
  });

  describe('Construction', () => {
    it('should create instance with default options', () => {
      expect(collector).toBeInstanceOf(DockerLogCollector);
      expect(collector.connected).toBe(false);
      expect(collector.paused).toBe(false);
    });

    it('should accept custom dockerode options', () => {
      const customCollector = new DockerLogCollector({
        dockerodeOptions: {
          socketPath: '/custom/docker.sock'
        }
      });

      expect(customCollector._dockerodeOptions.socketPath).toBe('/custom/docker.sock');
    });

    it('should accept custom reconnection config', () => {
      const customCollector = new DockerLogCollector({
        reconnectDelay: 2000,
        maxReconnectDelay: 60000,
        reconnectAttempts: 10
      });

      expect(customCollector._reconnectConfig.delay).toBe(2000);
      expect(customCollector._reconnectConfig.maxDelay).toBe(60000);
      expect(customCollector._reconnectConfig.attempts).toBe(10);
    });

    it('should have default stream options', () => {
      expect(collector._streamOptions.follow).toBe(true);
      expect(collector._streamOptions.stdout).toBe(true);
      expect(collector._streamOptions.stderr).toBe(true);
      expect(collector._streamOptions.tail).toBe(0);
    });
  });

  describe('Connection', () => {
    beforeEach(() => {
      // Reset mock stream for connection tests
      mockStream = new (require('events').EventEmitter)();
      mockStream.destroy = jest.fn();
      mockContainer.logs.mockResolvedValue(mockStream);
    });

    it('should connect to container by name', async () => {
      await collector.connect({ containerName: 'minecraft-server' });

      expect(mockDockerode).toHaveBeenCalledTimes(1);
      expect(collector._docker.getContainer).toHaveBeenCalledWith('minecraft-server');
      expect(mockContainer.inspect).toHaveBeenCalled();
      expect(collector.connected).toBe(true);
    });

    it('should emit connected event', async () => {
      const connectedSpy = jest.fn();
      collector.on('connected', connectedSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      expect(connectedSpy).toHaveBeenCalledTimes(1);
    });

    it('should pass stream options to Docker logs', async () => {
      await collector.connect({
        containerName: 'minecraft-server',
        follow: false,
        tail: 100,
        stdout: false,
        stderr: true
      });

      expect(mockContainer.logs).toHaveBeenCalledWith({
        follow: false,
        stdout: false,
        stderr: true,
        tail: 100
      });
    });

    it('should throw when container name is missing', async () => {
      await expect(collector.connect({}))
        .rejects.toThrow(DockerConnectionError);
    });

    it('should throw when container not found', async () => {
      mockContainer.inspect.mockRejectedValue(new Error('No such container'));

      await expect(collector.connect({ containerName: 'nonexistent' }))
        .rejects.toThrow(DockerConnectionError);
    });

    it('should disconnect before reconnecting', async () => {
      await collector.connect({ containerName: 'minecraft-server' });
      expect(collector.connected).toBe(true);

      await collector.connect({ containerName: 'minecraft-server' });

      // Should have disconnected first, then reconnected
      expect(mockContainer.logs).toHaveBeenCalledTimes(2);
    });
  });

  describe('Log Streaming', () => {
    beforeEach(() => {
      // Reset mock stream for streaming tests
      mockStream = new (require('events').EventEmitter)();
      mockStream.destroy = jest.fn();
      mockContainer.logs.mockResolvedValue(mockStream);
    });

    it('should emit data events with log lines', async () => {
      const dataSpy = jest.fn();
      collector.on('data', dataSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      // Simulate Docker log chunk
      const logBuffer = Buffer.from('[12:34:56] [Server thread/INFO]: Test log line');
      mockStream.emit('data', logBuffer);

      expect(dataSpy).toHaveBeenCalledWith('[12:34:56] [Server thread/INFO]: Test log line');
    });

    it('should trim whitespace from log lines', async () => {
      const dataSpy = jest.fn();
      collector.on('data', dataSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      const logBuffer = Buffer.from('  [12:34:56] Test  \n');
      mockStream.emit('data', logBuffer);

      expect(dataSpy).toHaveBeenCalledWith('[12:34:56] Test');
    });

    it('should skip empty lines', async () => {
      const dataSpy = jest.fn();
      collector.on('data', dataSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      mockStream.emit('data', Buffer.from(''));

      expect(dataSpy).not.toHaveBeenCalled();
    });

    it('should strip ANSI color codes', async () => {
      const dataSpy = jest.fn();
      collector.on('data', dataSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      // Log line with ANSI codes (common in Minecraft logs)
      const logWithAnsi = '\x1b[0m\x1b[32m[12:34:56]\x1b[0m [Server thread/INFO]: \x1b[36mTest message\x1b[0m';
      mockStream.emit('data', Buffer.from(logWithAnsi));

      expect(dataSpy).toHaveBeenCalledWith('[12:34:56] [Server thread/INFO]: Test message');
    });

    it('should not emit data when paused', async () => {
      const dataSpy = jest.fn();
      collector.on('data', dataSpy);

      await collector.connect({ containerName: 'minecraft-server' });
      collector.pause();

      mockStream.emit('data', Buffer.from('[12:34:56] Test'));

      expect(dataSpy).not.toHaveBeenCalled();
    });

    it('should resume emitting data after resume', async () => {
      const dataSpy = jest.fn();
      collector.on('data', dataSpy);

      await collector.connect({ containerName: 'minecraft-server' });
      collector.pause();
      collector.resume();

      mockStream.emit('data', Buffer.from('[12:34:56] Test'));

      expect(dataSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('Error Handling', () => {
    beforeEach(() => {
      // Reset mock stream for error tests
      mockStream = new (require('events').EventEmitter)();
      mockStream.destroy = jest.fn();
      mockContainer.logs.mockResolvedValue(mockStream);
    });

    it('should emit error on stream error', async () => {
      const errorSpy = jest.fn();
      collector.on('error', errorSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      const streamError = new Error('Stream broken');
      mockStream.emit('error', streamError);

      expect(errorSpy).toHaveBeenCalled();
      expect(errorSpy.mock.calls[0][0]).toBeInstanceOf(DockerConnectionError);
    });

    it('should include original error in chain', async () => {
      const errorSpy = jest.fn();
      collector.on('error', errorSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      const originalError = new Error('Original error');
      mockStream.emit('error', originalError);

      expect(errorSpy.mock.calls[0][0].cause).toBe(originalError);
    });
  });

  describe('Reconnection', () => {
    let streams = [];

    beforeEach(() => {
      jest.useRealTimers();
      streams = [];
      // Reset mock stream for reconnection tests
      // Use mockImplementation to create a new stream each time logs() is called
      mockContainer.logs.mockImplementation(() => {
        const stream = new (require('events').EventEmitter)();
        stream.destroy = jest.fn();
        streams.push(stream);
        return Promise.resolve(stream);
      });
    });

    afterEach(() => {
      jest.useFakeTimers();
    });

    it('should schedule reconnection on stream end', async () => {
      const reconnectingSpy = jest.fn();
      collector.on('reconnecting', reconnectingSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      // Get the stream from first call
      const firstStream = streams[0];
      firstStream.emit('end');

      expect(reconnectingSpy).toHaveBeenCalledWith({
        attempt: 1,
        maxAttempts: 5,
        delay: 1000
      });
    });

    it('should reconnect after exponential backoff delay', async () => {
      const reconnectingSpy = jest.fn();
      collector.on('reconnecting', reconnectingSpy);

      await collector.connect({ containerName: 'minecraft-server' });

      // Get the first stream
      const firstStream = streams[0];

      // Trigger reconnection
      firstStream.emit('end');

      // Wait for the reconnection event to be emitted
      await new Promise(resolve => setTimeout(resolve, 100));

      // Should have emitted reconnecting event
      expect(reconnectingSpy).toHaveBeenCalledTimes(1);
      expect(reconnectingSpy).toHaveBeenCalledWith({
        attempt: 1,
        maxAttempts: 5,
        delay: 1000
      });
    });

    it('should increase delay exponentially', async () => {
      // Create a collector with shorter delay for faster test
      const fastCollector = new DockerLogCollector({
        reconnectDelay: 100,
        maxReconnectDelay: 400
      });

      const reconnectingSpy = jest.fn();
      fastCollector.on('reconnecting', reconnectingSpy);

      await fastCollector.connect({ containerName: 'minecraft-server' });

      // Trigger first reconnection
      streams[0].emit('end');

      // Wait for reconnection to complete
      await new Promise(resolve => setTimeout(resolve, 150));

      // Get the delay from first reconnection
      const firstDelay = reconnectingSpy.mock.calls[0][0].delay;

      // Trigger second reconnection
      streams[1].emit('end');

      // Wait for reconnection to complete
      await new Promise(resolve => setTimeout(resolve, 250));

      // Get the delay from second reconnection
      const secondDelay = reconnectingSpy.mock.calls[1][0].delay;

      // Second delay should be double the first (exponential backoff)
      expect(secondDelay).toBe(firstDelay * 2);
    });

    it('should cap delay at maxReconnectDelay', async () => {
      // Create a collector with shorter delay for faster test
      const customCollector = new DockerLogCollector({
        reconnectDelay: 100,
        maxReconnectDelay: 300,
        reconnectAttempts: 10
      });

      const reconnectingSpy = jest.fn();
      customCollector.on('reconnecting', reconnectingSpy);

      await customCollector.connect({ containerName: 'minecraft-server' });

      // Trigger multiple reconnections
      for (let i = 0; i < 4; i++) {
        streams[i].emit('end');
        // Wait for reconnection to complete (delays: 100, 200, 300, 300)
        const delay = Math.min(100 * Math.pow(2, i), 300);
        await new Promise(resolve => setTimeout(resolve, delay + 50));
      }

      // All delays should be capped at maxReconnectDelay (300ms)
      const delays = reconnectingSpy.mock.calls.map(call => call[0].delay);
      delays.forEach(delay => {
        expect(delay).toBeLessThanOrEqual(300);
      });
    });

    it('should stop reconnecting after max attempts', async () => {
      // Create a collector with shorter delay for faster test
      const fastCollector = new DockerLogCollector({
        reconnectDelay: 100,
        maxReconnectDelay: 1600,
        reconnectAttempts: 5
      });

      const endSpy = jest.fn();
      const reconnectingSpy = jest.fn();
      fastCollector.on('end', endSpy);
      fastCollector.on('reconnecting', reconnectingSpy);

      await fastCollector.connect({ containerName: 'minecraft-server' });

      // Trigger 5 reconnections (max attempts)
      for (let i = 0; i < 5; i++) {
        streams[i].emit('end');
        // Wait for each reconnection to complete
        const delay = 100 * Math.pow(2, i);
        await new Promise(resolve => setTimeout(resolve, delay + 50));
      }

      // Should have attempted 5 reconnections
      expect(reconnectingSpy).toHaveBeenCalledTimes(5);

      // After max attempts, emit end on the last stream to trigger final end event
      streams[5].emit('end');

      expect(endSpy).toHaveBeenCalled();
      expect(fastCollector.connected).toBe(false);
    });

    it('should not reconnect when disabled', async () => {
      const endSpy = jest.fn();
      collector.on('end', endSpy);

      await collector.connect({
        containerName: 'minecraft-server',
        disableAutoReconnect: true
      });

      // Get the first stream and emit end
      const firstStream = streams[0];
      firstStream.emit('end');

      // Should emit end immediately
      expect(endSpy).toHaveBeenCalled();
      expect(collector.connected).toBe(false);
    });

    it('should return reconnect status', () => {
      const status = collector.getReconnectStatus();

      expect(status).toHaveProperty('attempt', 0);
      expect(status).toHaveProperty('maxAttempts', 5);
      expect(status).toHaveProperty('reconnecting', false);
    });
  });

  describe('Disconnection', () => {
    beforeEach(() => {
      // Reset mock stream for disconnection tests
      mockStream = new (require('events').EventEmitter)();
      mockStream.destroy = jest.fn();
      mockContainer.logs.mockResolvedValue(mockStream);
    });

    it('should disconnect from container', async () => {
      await collector.connect({ containerName: 'minecraft-server' });
      expect(collector.connected).toBe(true);

      await collector.disconnect();

      expect(collector.connected).toBe(false);
      expect(mockStream.destroy).toHaveBeenCalled();
    });

    it('should emit disconnected event', async () => {
      const disconnectedSpy = jest.fn();
      collector.on('disconnected', disconnectedSpy);

      await collector.connect({ containerName: 'minecraft-server' });
      await collector.disconnect();

      expect(disconnectedSpy).toHaveBeenCalledTimes(1);
    });

    it('should cancel pending reconnection', async () => {
      jest.useFakeTimers();

      await collector.connect({ containerName: 'minecraft-server' });
      mockStream.emit('end');

      // Disconnect before reconnection happens
      await collector.disconnect();

      jest.advanceTimersByTime(1000);

      // Should not have reconnected
      expect(mockContainer.logs).toHaveBeenCalledTimes(1);

      jest.useRealTimers();
    });

    it('should clear stream listeners', async () => {
      await collector.connect({ containerName: 'minecraft-server' });

      const listenerCount = mockStream.listenerCount('data');
      expect(listenerCount).toBeGreaterThan(0);

      await collector.disconnect();

      expect(mockStream.listenerCount('data')).toBe(0);
    });
  });

  describe('ANSI Code Stripping', () => {
    it('should strip basic color codes', () => {
      collector._streamOptions = {};

      const input = '\x1b[31mRed text\x1b[0m';
      const output = collector._stripAnsiCodes(input);

      expect(output).toBe('Red text');
    });

    it('should strip cursor movement codes', () => {
      collector._streamOptions = {};

      const input = 'Text\x1b[2C\x1b[1Dmore';
      const output = collector._stripAnsiCodes(input);

      expect(output).toBe('Textmore');
    });

    it('should handle multiple ANSI sequences', () => {
      collector._streamOptions = {};

      const input = '\x1b[32m[12:34:56]\x1b[0m \x1b[1m\x1b[36mTest\x1b[0m';
      const output = collector._stripAnsiCodes(input);

      expect(output).toBe('[12:34:56] Test');
    });

    it('should handle text without ANSI codes', () => {
      collector._streamOptions = {};

      const input = '[12:34:56] Plain text';
      const output = collector._stripAnsiCodes(input);

      expect(output).toBe('[12:34:56] Plain text');
    });

    it('should strip both escape code formats', () => {
      collector._streamOptions = {};

      const input = '\u001b[31mRed\u009b[0m';
      const output = collector._stripAnsiCodes(input);

      expect(output).toBe('Red');
    });
  });
});
