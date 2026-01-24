/**
 * LogCollector Base Class Tests
 *
 * Tests the abstract LogCollector base class to ensure:
 * - Cannot be instantiated directly
 * - Defines correct interface
 * - State management works
 * - Event emission works correctly
 */

const { LogCollector } = require('./LogCollector.js');

describe('LogCollector (Abstract Base Class)', () => {
  describe('Direct Instantiation Prevention', () => {
    it('should throw when instantiated directly', () => {
      expect(() => new LogCollector()).toThrow('LogCollector is abstract');
    });
  });

  describe('Concrete Implementation', () => {
    // Create a minimal concrete implementation for testing
    class TestLogCollector extends LogCollector {
      constructor() {
        super();
        this.mockConnected = false;
      }

      async connect(config) {
        this.mockConnected = true;
        this._connected = true;
        this._config = config;
      }

      async disconnect() {
        this.mockConnected = false;
        this._connected = false;
      }
    }

    let collector;

    beforeEach(() => {
      collector = new TestLogCollector();
    });

    describe('State Management', () => {
      it('should have connected property', () => {
        expect(collector.connected).toBe(false);

        collector._connected = true;

        expect(collector.connected).toBe(true);
      });

      it('should have paused property', () => {
        expect(collector.paused).toBe(false);

        collector.pause();

        expect(collector.paused).toBe(true);
      });

      it('should have config property', () => {
        expect(collector.config).toBeNull();

        const config = { host: 'localhost', port: 514 };
        collector._config = config;

        expect(collector.config).toEqual(config);
      });
    });

    describe('Pause/Resume', () => {
      it('should pause and emit paused event', () => {
        const pausedSpy = jest.fn();
        collector.on('paused', pausedSpy);

        collector.pause();

        expect(collector.paused).toBe(true);
        expect(pausedSpy).toHaveBeenCalledTimes(1);
      });

      it('should resume and emit resumed event', () => {
        collector.pause();

        const resumedSpy = jest.fn();
        collector.on('resumed', resumedSpy);

        collector.resume();

        expect(collector.paused).toBe(false);
        expect(resumedSpy).toHaveBeenCalledTimes(1);
      });
    });

    describe('Data Emission', () => {
      it('should emit data events when connected and not paused', () => {
        const dataSpy = jest.fn();
        collector.on('data', dataSpy);

        collector._connected = true;
        collector._paused = false;

        const result = collector._emitData('test log line');

        expect(result).toBe(true);
        expect(dataSpy).toHaveBeenCalledWith('test log line');
      });

      it('should not emit data when disconnected', () => {
        const dataSpy = jest.fn();
        collector.on('data', dataSpy);

        collector._connected = false;
        collector._paused = false;

        const result = collector._emitData('test log line');

        expect(result).toBe(false);
        expect(dataSpy).not.toHaveBeenCalled();
      });

      it('should not emit data when paused', () => {
        const dataSpy = jest.fn();
        collector.on('data', dataSpy);

        collector._connected = true;
        collector._paused = true;

        const result = collector._emitData('test log line');

        expect(result).toBe(false);
        expect(dataSpy).not.toHaveBeenCalled();
      });
    });

    describe('Error Emission', () => {
      it('should emit error events', () => {
        const errorSpy = jest.fn();
        collector.on('error', errorSpy);

        const testError = new Error('Test error');
        collector._emitError(testError);

        expect(errorSpy).toHaveBeenCalledWith(testError);
      });
    });

    describe('End Event', () => {
      it('should emit end event and set disconnected', () => {
        const endSpy = jest.fn();
        collector.on('end', endSpy);

        collector._connected = true;

        collector._emitEnd();

        expect(collector.connected).toBe(false);
        expect(endSpy).toHaveBeenCalledTimes(1);
      });
    });

    describe('Abstract Methods', () => {
      it('should throw on connect() if not implemented', async () => {
        // Create an incomplete subclass that doesn't implement connect()
        class IncompleteCollector extends LogCollector {
          constructor() {
            super();
          }
          // connect() not implemented
        }

        const baseCollector = new IncompleteCollector();

        await expect(baseCollector.connect({}))
          .rejects.toThrow('Method "connect()" must be implemented');
      });

      it('should throw on disconnect() if not implemented', async () => {
        // Create an incomplete subclass that doesn't implement disconnect()
        class IncompleteCollector extends LogCollector {
          constructor() {
            super();
          }
          // disconnect() not implemented
        }

        const baseCollector = new IncompleteCollector();

        await expect(baseCollector.disconnect())
          .rejects.toThrow('Method "disconnect()" must be implemented');
      });
    });
  });
});
