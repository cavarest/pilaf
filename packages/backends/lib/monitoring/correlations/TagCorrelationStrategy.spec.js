/**
 * TagCorrelationStrategy Tests
 */

const { TagCorrelationStrategy } = require('./TagCorrelationStrategy.js');

describe('TagCorrelationStrategy', () => {
  let strategy;

  beforeEach(() => {
    strategy = new TagCorrelationStrategy({
      tagExtractor: (event) => event.data?.tag,
      timeout: 100,
      cleanupInterval: 50
    });
  });

  afterEach(() => {
    strategy.destroy();
  });

  describe('Construction', () => {
    it('should create with default options', () => {
      const defaultStrategy = new TagCorrelationStrategy();
      expect(defaultStrategy.size).toBe(0);
      defaultStrategy.destroy();
    });
  });

  describe('Correlation', () => {
    it('should create new correlation for new tag', () => {
      const event = { type: 'test', data: { tag: 'tag-1' } };
      const correlation = strategy.correlate(event);

      expect(correlation).toBeTruthy();
      expect(correlation.tag).toBe('tag-1');
      expect(correlation.events).toEqual([event]);
      expect(correlation.timestamp).toBeTruthy();
    });

    it('should append to existing correlation', () => {
      const event1 = { type: 'start', data: { tag: 'tag-1' } };
      const event2 = { type: 'end', data: { tag: 'tag-1' } };

      strategy.correlate(event1);
      const correlation = strategy.correlate(event2);

      expect(correlation.events).toHaveLength(2);
      expect(correlation.events[0]).toBe(event1);
      expect(correlation.events[1]).toBe(event2);
    });

    it('should return null for event without tag', () => {
      const event = { type: 'test', data: {} };
      const correlation = strategy.correlate(event);

      expect(correlation).toBeNull();
    });

    it('should return null for null event', () => {
      const correlation = strategy.correlate(null);
      expect(correlation).toBeNull();
    });

    it('should maintain separate correlations for different tags', () => {
      const event1 = { type: 'test', data: { tag: 'tag-1' } };
      const event2 = { type: 'test', data: { tag: 'tag-2' } };

      strategy.correlate(event1);
      strategy.correlate(event2);

      expect(strategy.size).toBe(2);
    });
  });

  describe('Get Active Correlations', () => {
    it('should return empty array initially', () => {
      const correlations = strategy.getActiveCorrelations();
      expect(correlations).toEqual([]);
    });

    it('should return all active correlations', () => {
      strategy.correlate({ type: 'test', data: { tag: 'tag-1' } });
      strategy.correlate({ type: 'test', data: { tag: 'tag-2' } });

      const correlations = strategy.getActiveCorrelations();
      expect(correlations).toHaveLength(2);
    });
  });

  describe('Get Correlation by Tag', () => {
    it('should return correlation for existing tag', () => {
      strategy.correlate({ type: 'test', data: { tag: 'tag-1' } });
      const correlation = strategy.getCorrelation('tag-1');

      expect(correlation).toBeTruthy();
      expect(correlation.tag).toBe('tag-1');
    });

    it('should return undefined for non-existent tag', () => {
      const correlation = strategy.getCorrelation('non-existent');
      expect(correlation).toBeUndefined();
    });
  });

  describe('Cleanup', () => {
    it('should remove expired correlations', async () => {
      // Create strategy with longer cleanup interval to avoid interference
      const testStrategy = new TagCorrelationStrategy({
        tagExtractor: (event) => event.data?.tag,
        timeout: 50,
        cleanupInterval: 10000 // Very long interval to avoid auto-cleanup
      });

      testStrategy.correlate({ type: 'test', data: { tag: 'old-tag' } });

      // Wait for expiration
      await new Promise(resolve => setTimeout(resolve, 75));

      const removed = testStrategy.cleanup();

      expect(removed).toBe(1);
      expect(testStrategy.size).toBe(0);

      testStrategy.destroy();
    });

    it('should not remove recent correlations', () => {
      strategy.correlate({ type: 'test', data: { tag: 'new-tag' } });

      const removed = strategy.cleanup();

      expect(removed).toBe(0);
      expect(strategy.size).toBe(1);
    });
  });

  describe('Reset', () => {
    it('should clear all correlations', () => {
      strategy.correlate({ type: 'test', data: { tag: 'tag-1' } });
      strategy.correlate({ type: 'test', data: { tag: 'tag-2' } });

      strategy.reset();

      expect(strategy.size).toBe(0);
    });
  });

  describe('Events', () => {
    it('should emit cleanup event when correlations expire', async () => {
      let cleanupCount = 0;
      strategy.on('cleanup', (count) => {
        cleanupCount += count;
      });

      strategy.correlate({ type: 'test', data: { tag: 'old-tag' } });

      await new Promise(resolve => setTimeout(resolve, 150));
      strategy.cleanup();

      expect(cleanupCount).toBe(1);
    });
  });

  describe('Custom Tag Extractor', () => {
    it('should use custom tag extractor function', () => {
      const customStrategy = new TagCorrelationStrategy({
        tagExtractor: (event) => event.data?.requestId
      });

      const event = { type: 'command', data: { requestId: 'req-123' } };
      const correlation = customStrategy.correlate(event);

      expect(correlation.tag).toBe('req-123');

      customStrategy.destroy();
    });
  });
});
