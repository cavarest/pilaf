/**
 * UsernameCorrelationStrategy Tests
 */

const { UsernameCorrelationStrategy } = require('./UsernameCorrelationStrategy.js');

describe('UsernameCorrelationStrategy', () => {
  let strategy;

  beforeEach(() => {
    strategy = new UsernameCorrelationStrategy({
      usernameExtractor: (event) => event.data?.player
    });
  });

  describe('Construction', () => {
    it('should create with default options', () => {
      const defaultStrategy = new UsernameCorrelationStrategy();
      expect(defaultStrategy.size).toBe(0);
    });
  });

  describe('Correlation', () => {
    it('should create new session for new username', () => {
      const event = { type: 'entity.join', data: { player: 'Steve' } };
      const session = strategy.correlate(event);

      expect(session).toBeTruthy();
      expect(session.username).toBe('Steve');
      expect(session.events).toEqual([event]);
      expect(session.isActive).toBe(true);
      expect(session.sessionStart).toBeTruthy();
    });

    it('should append to existing session', () => {
      const event1 = { type: 'entity.join', data: { player: 'Steve' } };
      const event2 = { type: 'command.issued', data: { player: 'Steve' } };

      strategy.correlate(event1);
      const session = strategy.correlate(event2);

      expect(session.events).toHaveLength(2);
      expect(session.events[0]).toBe(event1);
      expect(session.events[1]).toBe(event2);
    });

    it('should mark session as inactive on leave', () => {
      const joinEvent = { type: 'entity.join', data: { player: 'Steve' } };
      const leaveEvent = { type: 'entity.leave', data: { player: 'Steve' } };

      strategy.correlate(joinEvent);
      const session = strategy.correlate(leaveEvent);

      expect(session.isActive).toBe(false);
      expect(session.sessionEnd).toBeTruthy();
    });

    it('should auto-cleanup inactive sessions by default', () => {
      const joinEvent = { type: 'entity.join', data: { player: 'Steve' } };
      const leaveEvent = { type: 'entity.leave', data: { player: 'Steve' } };

      strategy.correlate(joinEvent);
      strategy.correlate(leaveEvent);

      // Session should be removed after leave
      expect(strategy.getSession('Steve')).toBeUndefined();
    });

    it('should not auto-cleanup when disabled', () => {
      const noCleanupStrategy = new UsernameCorrelationStrategy({
        autoCleanup: false
      });

      const joinEvent = { type: 'entity.join', data: { player: 'Steve' } };
      const leaveEvent = { type: 'entity.leave', data: { player: 'Steve' } };

      noCleanupStrategy.correlate(joinEvent);
      noCleanupStrategy.correlate(leaveEvent);

      // Session should still exist
      expect(noCleanupStrategy.getSession('Steve')).toBeDefined();
      expect(noCleanupStrategy.getSession('Steve').isActive).toBe(false);
    });

    it('should return null for event without username', () => {
      const event = { type: 'world.time', data: { time: 1000 } };
      const session = strategy.correlate(event);

      expect(session).toBeNull();
    });

    it('should return null for null event', () => {
      const session = strategy.correlate(null);
      expect(session).toBeNull();
    });
  });

  describe('Get Session', () => {
    it('should return session for existing username', () => {
      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });
      const session = strategy.getSession('Steve');

      expect(session).toBeTruthy();
      expect(session.username).toBe('Steve');
    });

    it('should return undefined for non-existent username', () => {
      const session = strategy.getSession('NonExistent');
      expect(session).toBeUndefined();
    });
  });

  describe('Get Active Correlations', () => {
    it('should return empty array initially', () => {
      const sessions = strategy.getActiveCorrelations();
      expect(sessions).toEqual([]);
    });

    it('should return all active sessions', () => {
      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });
      strategy.correlate({ type: 'entity.join', data: { player: 'Alex' } });

      const sessions = strategy.getActiveCorrelations();
      expect(sessions).toHaveLength(2);
    });
  });

  describe('Has Active Session', () => {
    it('should return true for active player', () => {
      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });

      expect(strategy.hasActiveSession('Steve')).toBe(true);
    });

    it('should return false for inactive player', () => {
      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });
      strategy.correlate({ type: 'entity.leave', data: { player: 'Steve' } });

      expect(strategy.hasActiveSession('Steve')).toBe(false);
    });

    it('should return false for non-existent player', () => {
      expect(strategy.hasActiveSession('NonExistent')).toBe(false);
    });
  });

  describe('Get Online Players', () => {
    it('should return empty array when no players online', () => {
      const online = strategy.getOnlinePlayers();
      expect(online).toEqual([]);
    });

    it('should return only active players', () => {
      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });
      strategy.correlate({ type: 'entity.join', data: { player: 'Alex' } });
      strategy.correlate({ type: 'entity.leave', data: { player: 'Alex' } });

      const online = strategy.getOnlinePlayers();

      expect(online).toEqual(['Steve']);
    });
  });

  describe('End Session', () => {
    it('should end active session', () => {
      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });

      const ended = strategy.endSession('Steve');

      expect(ended).toBe(true);
      expect(strategy.getSession('Steve')).toBeUndefined();
    });

    it('should return false for non-existent session', () => {
      const ended = strategy.endSession('NonExistent');
      expect(ended).toBe(false);
    });

    it('should emit sessionEnd event', () => {
      let endedSession = null;
      strategy.on('sessionEnd', (session) => {
        endedSession = session;
      });

      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });
      strategy.endSession('Steve');

      expect(endedSession).toBeTruthy();
      expect(endedSession.username).toBe('Steve');
      expect(endedSession.isActive).toBe(false);
    });
  });

  describe('Get Statistics', () => {
    it('should return correct statistics', () => {
      const noCleanupStrategy = new UsernameCorrelationStrategy({
        autoCleanup: false
      });

      noCleanupStrategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });
      noCleanupStrategy.correlate({ type: 'entity.join', data: { player: 'Alex' } });
      noCleanupStrategy.correlate({ type: 'entity.leave', data: { player: 'Alex' } });

      const stats = noCleanupStrategy.getStatistics();

      expect(stats.total).toBe(2);
      expect(stats.active).toBe(1);
      expect(stats.inactive).toBe(1);
    });
  });

  describe('Reset', () => {
    it('should clear all sessions', () => {
      strategy.correlate({ type: 'entity.join', data: { player: 'Steve' } });
      strategy.correlate({ type: 'entity.join', data: { player: 'Alex' } });

      strategy.reset();

      expect(strategy.size).toBe(0);
    });
  });

  describe('Events', () => {
    it('should emit session event on correlation', () => {
      let emittedSession = null;
      strategy.on('session', (session) => {
        emittedSession = session;
      });

      const event = { type: 'entity.join', data: { player: 'Steve' } };
      strategy.correlate(event);

      expect(emittedSession).toBeTruthy();
      expect(emittedSession.username).toBe('Steve');
    });
  });

  describe('Custom Username Extractor', () => {
    it('should use custom username extractor function', () => {
      const customStrategy = new UsernameCorrelationStrategy({
        usernameExtractor: (event) => event.data?.username
      });

      const event = { type: 'test', data: { username: 'CustomUser' } };
      const session = customStrategy.correlate(event);

      expect(session.username).toBe('CustomUser');
    });
  });

  describe('Include Metadata Option', () => {
    it('should not include metadata when disabled', () => {
      const noMetadataStrategy = new UsernameCorrelationStrategy({
        includeMetadata: false
      });

      const event = { type: 'entity.join', data: { player: 'Steve' } };
      const session = noMetadataStrategy.correlate(event);

      expect(session.sessionStart).toBeUndefined();
    });
  });
});
