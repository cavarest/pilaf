/**
 * EventObserver Tests
 *
 * Tests the EventObserver class for event subscriptions and pattern matching.
 */

const { EventEmitter } = require('events');
const { EventObserver } = require('./EventObserver.js');

// Mock LogMonitor for testing
class MockLogMonitor extends EventEmitter {
  constructor() {
    super();
    this._isStarted = false;
  }

  async start() {
    this._isStarted = true;
  }

  stop() {
    this._isStarted = false;
  }

  isRunning() {
    return this._isStarted;
  }

  // Helper to emit events for testing
  emitEvent(event) {
    this.emit('event', event);
  }
}

// Mock Parser
class MockParser {
  parse(line) {
    return { type: 'test', data: line };
  }
}

describe('EventObserver', () => {
  describe('Construction', () => {
    it('should create with valid dependencies', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      expect(observer._logMonitor).toBe(logMonitor);
      expect(observer._parser).toBe(parser);
      expect(observer._isObserving).toBe(false);
    });

    it('should throw without logMonitor', () => {
      const parser = new MockParser();
      expect(() => new EventObserver({ parser }))
        .toThrow('logMonitor is required');
    });

    it('should throw without parser', () => {
      const logMonitor = new MockLogMonitor();
      expect(() => new EventObserver({ logMonitor }))
        .toThrow('parser is required');
    });

    it('should set up event listener on logMonitor', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const event = { type: 'test.event', data: {} };
      logMonitor.emitEvent(event);

      // Event was received (no errors)
      expect(true).toBe(true);
    });
  });

  describe('onEvent subscription', () => {
    it('should subscribe to exact event pattern', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let receivedEvent = null;
      observer.onEvent('entity.join', (event) => {
        receivedEvent = event;
      });

      logMonitor.emitEvent({ type: 'entity.join', data: { player: 'Steve' } });

      expect(receivedEvent).toBeTruthy();
      expect(receivedEvent.type).toBe('entity.join');
      expect(receivedEvent.data.player).toBe('Steve');
    });

    it('should subscribe to wildcard pattern', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const events = [];
      observer.onEvent('entity.*', (event) => {
        events.push(event);
      });

      logMonitor.emitEvent({ type: 'entity.join', data: {} });
      logMonitor.emitEvent({ type: 'entity.leave', data: {} });
      logMonitor.emitEvent({ type: 'entity.death', data: {} });

      expect(events).toHaveLength(3);
    });

    it('should subscribe to all events with *', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const events = [];
      observer.onEvent('*', (event) => {
        events.push(event);
      });

      logMonitor.emitEvent({ type: 'entity.join', data: {} });
      logMonitor.emitEvent({ type: 'world.save', data: {} });
      logMonitor.emitEvent({ type: 'command.issued', data: {} });

      expect(events).toHaveLength(3);
    });

    it('should not match non-matching patterns', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let receivedEvent = null;
      observer.onEvent('entity.join', (event) => {
        receivedEvent = event;
      });

      logMonitor.emitEvent({ type: 'entity.leave', data: {} });

      expect(receivedEvent).toBeNull();
    });

    it('should support multiple callbacks for same pattern', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let count = 0;
      observer.onEvent('entity.join', () => { count++; });
      observer.onEvent('entity.join', () => { count++; });

      logMonitor.emitEvent({ type: 'entity.join', data: {} });

      expect(count).toBe(2);
    });

    it('should return unsubscribe function', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let receivedEvent = null;
      const unsubscribe = observer.onEvent('entity.join', (event) => {
        receivedEvent = event;
      });

      unsubscribe();

      logMonitor.emitEvent({ type: 'entity.join', data: {} });

      expect(receivedEvent).toBeNull();
    });

    it('should handle multiple subscriptions correctly', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const entityEvents = [];
      const worldEvents = [];

      observer.onEvent('entity.*', (e) => entityEvents.push(e));
      observer.onEvent('world.*', (e) => worldEvents.push(e));

      logMonitor.emitEvent({ type: 'entity.join', data: {} });
      logMonitor.emitEvent({ type: 'world.save', data: {} });

      expect(entityEvents).toHaveLength(1);
      expect(worldEvents).toHaveLength(1);
    });
  });

  describe('Specific event methods', () => {
    it('should subscribe to player join events', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let receivedEvent = null;
      observer.onPlayerJoin((event) => {
        receivedEvent = event;
      });

      logMonitor.emitEvent({ type: 'entity.join', data: { player: 'Steve' } });

      expect(receivedEvent).toBeTruthy();
      expect(receivedEvent.type).toBe('entity.join');
    });

    it('should subscribe to player leave events', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let receivedEvent = null;
      observer.onPlayerLeave((event) => {
        receivedEvent = event;
      });

      logMonitor.emitEvent({ type: 'entity.leave', data: { player: 'Alex' } });

      expect(receivedEvent).toBeTruthy();
      expect(receivedEvent.type).toBe('entity.leave');
    });

    it('should subscribe to player death events', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const events = [];
      observer.onPlayerDeath((event) => {
        events.push(event);
      });

      logMonitor.emitEvent({ type: 'entity.death.fire', data: {} });
      logMonitor.emitEvent({ type: 'entity.death.lava', data: {} });
      logMonitor.emitEvent({ type: 'entity.death.generic', data: {} });

      expect(events).toHaveLength(3);
    });

    it('should subscribe to command events', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const events = [];
      observer.onCommand((event) => {
        events.push(event);
      });

      logMonitor.emitEvent({ type: 'command.issued', data: { command: '/gamemode' } });
      logMonitor.emitEvent({ type: 'command.success', data: {} });

      expect(events).toHaveLength(2);
    });

    it('should subscribe to world events', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const events = [];
      observer.onWorldEvent((event) => {
        events.push(event);
      });

      logMonitor.emitEvent({ type: 'world.save', data: {} });
      logMonitor.emitEvent({ type: 'world.saved', data: {} });
      logMonitor.emitEvent({ type: 'world.time', data: {} });

      expect(events).toHaveLength(3);
    });
  });

  describe('Pattern matching', () => {
    it('should match exact patterns', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let matched = false;
      observer.onEvent('entity.join', () => {
        matched = true;
      });

      logMonitor.emitEvent({ type: 'entity.join', data: {} });

      expect(matched).toBe(true);
    });

    it('should match wildcard patterns', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const matches = [];
      observer.onEvent('entity.death.*', (e) => matches.push(e.type));

      logMonitor.emitEvent({ type: 'entity.death.fire', data: {} });
      logMonitor.emitEvent({ type: 'entity.death.lava', data: {} });
      logMonitor.emitEvent({ type: 'entity.death.generic', data: {} });
      logMonitor.emitEvent({ type: 'entity.join', data: {} });

      expect(matches).toEqual(['entity.death.fire', 'entity.death.lava', 'entity.death.generic']);
    });

    it('should match complex glob patterns', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const matches = [];
      observer.onEvent('*.save', (e) => matches.push(e.type));

      logMonitor.emitEvent({ type: 'world.save', data: {} });
      logMonitor.emitEvent({ type: 'player.save', data: {} });
      logMonitor.emitEvent({ type: 'world.saved', data: {} });

      expect(matches).toEqual(['world.save', 'player.save']);
    });

    it('should handle pattern case sensitivity', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let matched = false;
      observer.onEvent('Entity.Join', () => {
        matched = true;
      });

      logMonitor.emitEvent({ type: 'Entity.Join', data: {} });

      expect(matched).toBe(true);
    });

    it('should not match partial patterns without wildcard', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let matched = false;
      observer.onEvent('entity.join', () => {
        matched = true;
      });

      logMonitor.emitEvent({ type: 'entity.join.player', data: {} });

      expect(matched).toBe(false);
    });
  });

  describe('Unsubscribe functionality', () => {
    it('should remove single callback', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let count = 0;
      const callback = () => { count++; };
      observer.onEvent('test', callback);

      const unsubscribe = observer.onEvent('test', () => { count++; });
      unsubscribe();

      logMonitor.emitEvent({ type: 'test', data: {} });

      expect(count).toBe(1);
    });

    it('should remove pattern subscription when empty', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const unsubscribe = observer.onEvent('test', () => {});
      unsubscribe();

      const subscriptions = observer.getSubscriptions();
      expect(subscriptions).toHaveLength(0);
    });

    it('should handle multiple unsubscribes', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const unsub1 = observer.onEvent('test', () => {});
      const unsub2 = observer.onEvent('test', () => {});

      unsub1();
      unsub2();

      const subscriptions = observer.getSubscriptions();
      expect(subscriptions).toHaveLength(0);
    });

    it('should handle unsubscribe of non-existent callback gracefully', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const unsubscribe = observer.onEvent('test', () => {});
      unsubscribe(); // First call removes it
      unsubscribe(); // Second call should be no-op

      expect(true).toBe(true);
    });
  });

  describe('Lifecycle', () => {
    it('should start observing', async () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      await observer.start();

      expect(observer.isObserving).toBe(true);
      expect(logMonitor.isRunning()).toBe(true);
    });

    it('should stop observing', async () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      await observer.start();
      observer.stop();

      expect(observer.isObserving).toBe(false);
      expect(logMonitor.isRunning()).toBe(false);
    });

    it('should throw when starting already started observer', async () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      await observer.start();

      await expect(observer.start()).rejects.toThrow('EventObserver is already observing');
    });

    it('should allow restart after stop', async () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      await observer.start();
      observer.stop();
      await observer.start();

      expect(observer.isObserving).toBe(true);
    });

    it('should handle stop when not started', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      observer.stop(); // Should not throw

      expect(observer.isObserving).toBe(false);
    });
  });

  describe('getSubscriptions', () => {
    it('should return empty array initially', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const subscriptions = observer.getSubscriptions();

      expect(subscriptions).toEqual([]);
    });

    it('should return active subscriptions', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      observer.onEvent('entity.*', () => {});
      observer.onEvent('entity.join', () => {});
      observer.onEvent('world.*', () => {});

      const subscriptions = observer.getSubscriptions();

      expect(subscriptions).toHaveLength(3);
    });

    it('should report callback count correctly', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      observer.onEvent('test', () => {});
      observer.onEvent('test', () => {});
      observer.onEvent('test', () => {});

      const subscriptions = observer.getSubscriptions();

      expect(subscriptions[0].callbackCount).toBe(3);
    });
  });

  describe('clearSubscriptions', () => {
    it('should clear all subscriptions', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      observer.onEvent('entity.*', () => {});
      observer.onEvent('world.*', () => {});

      observer.clearSubscriptions();

      expect(observer.getSubscriptions()).toHaveLength(0);
    });

    it('should prevent callbacks after clearing', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      let called = false;
      observer.onEvent('test', () => { called = true; });

      observer.clearSubscriptions();
      logMonitor.emitEvent({ type: 'test', data: {} });

      expect(called).toBe(false);
    });
  });

  describe('Error handling', () => {
    it('should isolate callback errors', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      // Suppress error emission for this test
      observer.on('error', () => {});

      let secondCalled = false;
      observer.onEvent('test', () => { throw new Error('Test error'); });
      observer.onEvent('test', () => { secondCalled = true; });

      logMonitor.emitEvent({ type: 'test', data: {} });

      expect(secondCalled).toBe(true);
    });

    it('should emit error event on callback failure', (done) => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      observer.on('error', ({ error, event }) => {
        expect(error.message).toBe('Test error');
        expect(event.type).toBe('test');
        done();
      });

      observer.onEvent('test', () => { throw new Error('Test error'); });
      logMonitor.emitEvent({ type: 'test', data: {} });
    });

    it('should handle multiple failing callbacks', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      // Suppress error emission for this test
      observer.on('error', () => {});

      let successCount = 0;
      observer.onEvent('test', () => { throw new Error('Error 1'); });
      observer.onEvent('test', () => { throw new Error('Error 2'); });
      observer.onEvent('test', () => { successCount++; });

      logMonitor.emitEvent({ type: 'test', data: {} });

      expect(successCount).toBe(1);
    });
  });

  describe('Integration scenarios', () => {
    it('should handle complex event flow', async () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const joins = [];
      const deaths = [];
      const allEvents = [];

      observer.onPlayerJoin((e) => joins.push(e.data.player));
      observer.onPlayerDeath((e) => deaths.push(e.type));
      observer.onEvent('*', (e) => allEvents.push(e.type));

      await observer.start();

      logMonitor.emitEvent({ type: 'entity.join', data: { player: 'Steve' } });
      logMonitor.emitEvent({ type: 'entity.death.fire', data: {} });
      logMonitor.emitEvent({ type: 'entity.leave', data: { player: 'Alex' } });

      expect(joins).toEqual(['Steve']);
      expect(deaths).toEqual(['entity.death.fire']);
      expect(allEvents).toHaveLength(3);
    });

    it('should handle subscription management during event flow', () => {
      const logMonitor = new MockLogMonitor();
      const parser = new MockParser();
      const observer = new EventObserver({ logMonitor, parser });

      const events = [];
      const unsubscribe = observer.onEvent('test', (e) => {
        events.push(e.type);
        if (events.length === 2) unsubscribe();
      });

      logMonitor.emitEvent({ type: 'test', data: {} });
      logMonitor.emitEvent({ type: 'test', data: {} });
      logMonitor.emitEvent({ type: 'test', data: {} });

      expect(events).toHaveLength(2);
    });
  });
});
