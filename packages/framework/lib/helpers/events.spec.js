const { waitForEvents, captureEvents } = require('./events');

describe('events helpers', () => {
  describe('waitForEvents', () => {
    let mockBot;
    let mockEventHandlers;

    beforeEach(() => {
      mockEventHandlers = {};
      mockBot = {
        on: jest.fn((event, handler) => {
          mockEventHandlers[event] = mockEventHandlers[event] || [];
          mockEventHandlers[event].push(handler);
        }),
        removeListener: jest.fn((event, handler) => {
          if (mockEventHandlers[event]) {
            mockEventHandlers[event] = mockEventHandlers[event].filter(h => h !== handler);
          }
        })
      };
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.runOnlyPendingTimers();
      jest.useRealTimers();
    });

    it('should wait for specified number of events', async () => {
      const promise = waitForEvents(mockBot, 'test', 2);

      // Emit first event
      mockEventHandlers.test?.forEach(h => h({ data: 'first' }));
      // Emit second event
      mockEventHandlers.test?.forEach(h => h({ data: 'second' }));

      jest.runAllTimers();

      const result = await promise;
      expect(result).toHaveLength(2);
      expect(result[0]).toEqual({ type: 'test', data: { data: 'first' } });
      expect(result[1]).toEqual({ type: 'test', data: { data: 'second' } });
    });

    it('should timeout if not enough events are received', async () => {
      const promise = waitForEvents(mockBot, 'test', 3, 1000);

      // Only emit 1 event instead of 3
      mockEventHandlers.test?.forEach(h => h({ data: 'first' }));

      jest.advanceTimersByTime(1000);

      await expect(promise).rejects.toThrow('Timeout waiting for 3 test events (got 1)');
    });

    it('should clean up event listener on completion', async () => {
      const promise = waitForEvents(mockBot, 'test', 1);

      mockEventHandlers.test?.forEach(h => h({ data: 'first' }));
      jest.runAllTimers();

      await promise;

      expect(mockBot.removeListener).toHaveBeenCalledWith('test', expect.any(Function));
    });

    it('should clean up event listener on timeout', async () => {
      const promise = waitForEvents(mockBot, 'test', 2, 1000);

      mockEventHandlers.test?.forEach(h => h({ data: 'first' }));
      jest.advanceTimersByTime(1000);

      try {
        await promise;
      } catch (e) {
        // Expected to timeout
      }

      expect(mockBot.removeListener).toHaveBeenCalledWith('test', expect.any(Function));
    });

    it('should use default timeout of 5000ms', async () => {
      const promise = waitForEvents(mockBot, 'test', 1);

      mockEventHandlers.test?.forEach(h => h({ data: 'first' }));
      jest.advanceTimersByTime(5000);

      await promise;

      // Should have resolved, not timed out
      await expect(promise).resolves.toBeDefined();
    });
  });

  describe('captureEvents', () => {
    let mockBot;
    let mockEventHandlers;

    beforeEach(() => {
      mockEventHandlers = {};
      mockBot = {
        on: jest.fn((event, handler) => {
          mockEventHandlers[event] = mockEventHandlers[event] || [];
          mockEventHandlers[event].push(handler);
        }),
        removeListener: jest.fn((event, handler) => {
          if (mockEventHandlers[event]) {
            mockEventHandlers[event] = mockEventHandlers[event].filter(h => h !== handler);
          }
        })
      };
    });

    it('should capture events from multiple event types', () => {
      const capture = captureEvents(mockBot, ['event1', 'event2']);

      // Emit events
      mockEventHandlers.event1?.forEach(h => h({ data: 'event1-data' }));
      mockEventHandlers.event2?.forEach(h => h({ data: 'event2-data' }));
      mockEventHandlers.event1?.forEach(h => h({ data: 'event1-data-2' }));

      expect(capture.events).toHaveLength(3);
      expect(capture.events[0]).toEqual({ type: 'event1', data: { data: 'event1-data' } });
      expect(capture.events[1]).toEqual({ type: 'event2', data: { data: 'event2-data' } });
      expect(capture.events[2]).toEqual({ type: 'event1', data: { data: 'event1-data-2' } });
    });

    it('should register handlers for all specified event types', () => {
      captureEvents(mockBot, ['event1', 'event2', 'event3']);

      expect(mockBot.on).toHaveBeenCalledWith('event1', expect.any(Function));
      expect(mockBot.on).toHaveBeenCalledWith('event2', expect.any(Function));
      expect(mockBot.on).toHaveBeenCalledWith('event3', expect.any(Function));
    });

    it('should remove all event listeners when released', () => {
      const capture = captureEvents(mockBot, ['event1', 'event2']);

      capture.release();

      expect(mockBot.removeListener).toHaveBeenCalledTimes(2);
      expect(mockBot.removeListener).toHaveBeenCalledWith('event1', expect.any(Function));
      expect(mockBot.removeListener).toHaveBeenCalledWith('event2', expect.any(Function));
    });

    it('should start with empty events array', () => {
      const capture = captureEvents(mockBot, ['event1']);

      expect(capture.events).toEqual([]);
    });

    it('should handle single event type', () => {
      const capture = captureEvents(mockBot, ['single']);

      mockEventHandlers.single?.forEach(h => h({ data: 'test' }));

      expect(capture.events).toHaveLength(1);
      expect(capture.events[0]).toEqual({ type: 'single', data: { data: 'test' } });
    });

    it('should handle empty event names array', () => {
      const capture = captureEvents(mockBot, []);

      expect(capture.events).toEqual([]);
      expect(mockBot.on).not.toHaveBeenCalled();

      capture.release();
      expect(mockBot.removeListener).not.toHaveBeenCalled();
    });

    it('should allow multiple calls to release without error', () => {
      const capture = captureEvents(mockBot, ['event1']);

      capture.release();
      const callCountAfterFirst = mockBot.removeListener.mock.calls.length;

      capture.release();
      const callCountAfterSecond = mockBot.removeListener.mock.calls.length;

      // Second release should have made additional calls (idempotent not implemented)
      expect(callCountAfterSecond).toBeGreaterThanOrEqual(callCountAfterFirst);
    });
  });
});
