const { ConnectionState } = require('./ConnectionState');

describe('ConnectionState', () => {
  describe('state constants', () => {
    it('should have DISCONNECTED constant', () => {
      expect(ConnectionState.DISCONNECTED).toBe('DISCONNECTED');
    });

    it('should have CONNECTING constant', () => {
      expect(ConnectionState.CONNECTING).toBe('CONNECTING');
    });

    it('should have CONNECTED constant', () => {
      expect(ConnectionState.CONNECTED).toBe('CONNECTED');
    });

    it('should have SPAWNING constant', () => {
      expect(ConnectionState.SPAWNING).toBe('SPAWNING');
    });

    it('should have SPAWNED constant', () => {
      expect(ConnectionState.SPAWNED).toBe('SPAWNED');
    });

    it('should have ERROR constant', () => {
      expect(ConnectionState.ERROR).toBe('ERROR');
    });

    it('should have DISCONNECTING constant', () => {
      expect(ConnectionState.DISCONNECTING).toBe('DISCONNECTING');
    });
  });

  describe('isTerminal', () => {
    it('should return true for DISCONNECTED state', () => {
      expect(ConnectionState.isTerminal(ConnectionState.DISCONNECTED)).toBe(true);
    });

    it('should return true for ERROR state', () => {
      expect(ConnectionState.isTerminal(ConnectionState.ERROR)).toBe(true);
    });

    it('should return false for CONNECTING state', () => {
      expect(ConnectionState.isTerminal(ConnectionState.CONNECTING)).toBe(false);
    });

    it('should return false for CONNECTED state', () => {
      expect(ConnectionState.isTerminal(ConnectionState.CONNECTED)).toBe(false);
    });

    it('should return false for SPAWNING state', () => {
      expect(ConnectionState.isTerminal(ConnectionState.SPAWNING)).toBe(false);
    });

    it('should return false for SPAWNED state', () => {
      expect(ConnectionState.isTerminal(ConnectionState.SPAWNED)).toBe(false);
    });

    it('should return false for DISCONNECTING state', () => {
      expect(ConnectionState.isTerminal(ConnectionState.DISCONNECTING)).toBe(false);
    });
  });

  describe('canPerformBotOperations', () => {
    it('should return true for SPAWNED state', () => {
      expect(ConnectionState.canPerformBotOperations(ConnectionState.SPAWNED)).toBe(true);
    });

    it('should return false for DISCONNECTED state', () => {
      expect(ConnectionState.canPerformBotOperations(ConnectionState.DISCONNECTED)).toBe(false);
    });

    it('should return false for CONNECTING state', () => {
      expect(ConnectionState.canPerformBotOperations(ConnectionState.CONNECTING)).toBe(false);
    });

    it('should return false for CONNECTED state', () => {
      expect(ConnectionState.canPerformBotOperations(ConnectionState.CONNECTED)).toBe(false);
    });

    it('should return false for SPAWNING state', () => {
      expect(ConnectionState.canPerformBotOperations(ConnectionState.SPAWNING)).toBe(false);
    });

    it('should return false for ERROR state', () => {
      expect(ConnectionState.canPerformBotOperations(ConnectionState.ERROR)).toBe(false);
    });

    it('should return false for DISCONNECTING state', () => {
      expect(ConnectionState.canPerformBotOperations(ConnectionState.DISCONNECTING)).toBe(false);
    });
  });

  describe('isTransitioning', () => {
    it('should return true for CONNECTING state', () => {
      expect(ConnectionState.isTransitioning(ConnectionState.CONNECTING)).toBe(true);
    });

    it('should return true for SPAWNING state', () => {
      expect(ConnectionState.isTransitioning(ConnectionState.SPAWNING)).toBe(true);
    });

    it('should return true for DISCONNECTING state', () => {
      expect(ConnectionState.isTransitioning(ConnectionState.DISCONNECTING)).toBe(true);
    });

    it('should return false for DISCONNECTED state', () => {
      expect(ConnectionState.isTransitioning(ConnectionState.DISCONNECTED)).toBe(false);
    });

    it('should return false for CONNECTED state', () => {
      expect(ConnectionState.isTransitioning(ConnectionState.CONNECTED)).toBe(false);
    });

    it('should return false for SPAWNED state', () => {
      expect(ConnectionState.isTransitioning(ConnectionState.SPAWNED)).toBe(false);
    });

    it('should return false for ERROR state', () => {
      expect(ConnectionState.isTransitioning(ConnectionState.ERROR)).toBe(false);
    });
  });
});
