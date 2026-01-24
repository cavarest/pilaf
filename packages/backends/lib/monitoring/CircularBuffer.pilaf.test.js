/**
 * CircularBuffer Tests
 *
 * Tests the circular buffer implementation for correct behavior.
 */

const { CircularBuffer } = require('./CircularBuffer.js');

describe('CircularBuffer', () => {
  describe('Construction', () => {
    it('should create buffer with valid capacity', () => {
      const buffer = new CircularBuffer(10);
      expect(buffer.capacity).toBe(10);
      expect(buffer.size).toBe(0);
      expect(buffer.isEmpty).toBe(true);
      expect(buffer.isFull).toBe(false);
    });

    it('should throw on invalid capacity (zero)', () => {
      expect(() => new CircularBuffer(0)).toThrow('Capacity must be a positive integer');
    });

    it('should throw on invalid capacity (negative)', () => {
      expect(() => new CircularBuffer(-1)).toThrow('Capacity must be a positive integer');
    });

    it('should throw on invalid capacity (non-integer)', () => {
      expect(() => new CircularBuffer(1.5)).toThrow('Capacity must be a positive integer');
    });

    it('should throw on invalid capacity (non-number)', () => {
      expect(() => new CircularBuffer('ten')).toThrow('Capacity must be a positive integer');
    });
  });

  describe('Push and Pop Operations', () => {
    it('should push single value', () => {
      const buffer = new CircularBuffer(3);
      const displaced = buffer.push(1);

      expect(displaced).toBeUndefined();
      expect(buffer.size).toBe(1);
      expect(buffer.get(0)).toBe(1);
    });

    it('should push multiple values', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);

      expect(buffer.size).toBe(3);
      expect(buffer.get(0)).toBe(1);
      expect(buffer.get(1)).toBe(2);
      expect(buffer.get(2)).toBe(3);
    });

    it('should displace oldest value when full', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);
      const displaced = buffer.push(4);

      expect(displaced).toBe(1);
      expect(buffer.size).toBe(3);
      expect(buffer.get(0)).toBe(2);
      expect(buffer.get(1)).toBe(3);
      expect(buffer.get(2)).toBe(4);
    });

    it('should pop oldest value', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);

      const value = buffer.pop();

      expect(value).toBe(1);
      expect(buffer.size).toBe(2);
      expect(buffer.get(0)).toBe(2);
      expect(buffer.get(1)).toBe(3);
    });

    it('should return undefined when popping empty buffer', () => {
      const buffer = new CircularBuffer(3);
      const value = buffer.pop();

      expect(value).toBeUndefined();
    });

    it('should handle push and pop in sequence', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      expect(buffer.pop()).toBe(1);
      buffer.push(3);
      buffer.push(4);

      expect(buffer.size).toBe(3);
      expect(buffer.get(0)).toBe(2);
      expect(buffer.get(1)).toBe(3);
      expect(buffer.get(2)).toBe(4);
    });
  });

  describe('Array-like Access', () => {
    it('should get value at valid index', () => {
      const buffer = new CircularBuffer(5);
      buffer.push(10);
      buffer.push(20);
      buffer.push(30);

      expect(buffer.get(0)).toBe(10);
      expect(buffer.get(1)).toBe(20);
      expect(buffer.get(2)).toBe(30);
    });

    it('should return undefined for out of bounds index', () => {
      const buffer = new CircularBuffer(5);
      buffer.push(10);
      buffer.push(20);

      expect(buffer.get(-1)).toBeUndefined();
      expect(buffer.get(2)).toBeUndefined();
      expect(buffer.get(100)).toBeUndefined();
    });

    it('should return undefined from empty buffer', () => {
      const buffer = new CircularBuffer(5);
      expect(buffer.get(0)).toBeUndefined();
    });

    it('should convert to array in correct order', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);

      const arr = buffer.toArray();

      expect(arr).toEqual([1, 2, 3]);
    });

    it('should handle wrap-around in toArray', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);
      buffer.push(4); // Displaces 1

      const arr = buffer.toArray();

      expect(arr).toEqual([2, 3, 4]);
    });

    it('should slice correctly', () => {
      const buffer = new CircularBuffer(5);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);
      buffer.push(4);
      buffer.push(5);

      expect(buffer.slice(1, 3)).toEqual([2, 3]);
      expect(buffer.slice(0, 5)).toEqual([1, 2, 3, 4, 5]);
      expect(buffer.slice(2)).toEqual([3, 4, 5]);
    });

    it('should handle slice with bounds', () => {
      const buffer = new CircularBuffer(5);
      for (let i = 1; i <= 5; i++) {
        buffer.push(i);
      }

      expect(buffer.slice(0, 100)).toEqual([1, 2, 3, 4, 5]);
      expect(buffer.slice(3, 10)).toEqual([4, 5]);
    });
  });

  describe('Clear Operation', () => {
    it('should clear all elements', () => {
      const buffer = new CircularBuffer(5);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);

      buffer.clear();

      expect(buffer.size).toBe(0);
      expect(buffer.isEmpty).toBe(true);
      expect(buffer.toArray()).toEqual([]);
    });

    it('should allow reuse after clear', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.clear();

      buffer.push(10);
      buffer.push(20);

      expect(buffer.size).toBe(2);
      expect(buffer.toArray()).toEqual([10, 20]);
    });
  });

  describe('Edge Cases', () => {
    it('should handle single element buffer', () => {
      const buffer = new CircularBuffer(1);
      buffer.push(1);

      expect(buffer.size).toBe(1);
      expect(buffer.get(0)).toBe(1);
      expect(buffer.isFull).toBe(true);
    });

    it('should handle large buffer', () => {
      const buffer = new CircularBuffer(10000);
      for (let i = 0; i < 10000; i++) {
        buffer.push(i);
      }

      expect(buffer.size).toBe(10000);
      expect(buffer.get(0)).toBe(0);
      expect(buffer.get(9999)).toBe(9999);
    });

    it('should handle storing different types', () => {
      const buffer = new CircularBuffer(5);
      buffer.push(1);
      buffer.push('string');
      buffer.push({ key: 'value' });
      buffer.push([1, 2, 3]);
      buffer.push(null);

      expect(buffer.size).toBe(5);
      expect(buffer.get(0)).toBe(1);
      expect(buffer.get(1)).toBe('string');
      expect(buffer.get(2)).toEqual({ key: 'value' });
      expect(buffer.get(3)).toEqual([1, 2, 3]);
      expect(buffer.get(4)).toBeNull();
    });

    it('should handle consecutive overflows', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);
      buffer.push(4); // Displaces 1
      buffer.push(5); // Displaces 2
      buffer.push(6); // Displaces 3

      expect(buffer.toArray()).toEqual([4, 5, 6]);
    });

    it('should handle interleaved push and pop with wrap-around', () => {
      const buffer = new CircularBuffer(3);
      buffer.push(1);
      buffer.push(2);
      buffer.push(3);
      buffer.push(4); // Displaces 1, buffer is [2, 3, 4]
      buffer.pop();    // Removes 2, buffer is [3, 4]
      buffer.push(5); // buffer is [3, 4, 5]
      buffer.push(6); // Displaces 3, buffer is [4, 5, 6]

      expect(buffer.toArray()).toEqual([4, 5, 6]);
    });
  });

  describe('Property Getters', () => {
    it('should report correct size', () => {
      const buffer = new CircularBuffer(5);
      expect(buffer.size).toBe(0);

      buffer.push(1);
      expect(buffer.size).toBe(1);

      buffer.push(2);
      expect(buffer.size).toBe(2);
    });

    it('should report correct capacity', () => {
      const buffer = new CircularBuffer(42);
      expect(buffer.capacity).toBe(42);
    });

    it('should report isEmpty correctly', () => {
      const buffer = new CircularBuffer(3);
      expect(buffer.isEmpty).toBe(true);

      buffer.push(1);
      expect(buffer.isEmpty).toBe(false);

      buffer.pop();
      expect(buffer.isEmpty).toBe(true);
    });

    it('should report isFull correctly', () => {
      const buffer = new CircularBuffer(3);
      expect(buffer.isFull).toBe(false);

      buffer.push(1);
      expect(buffer.isFull).toBe(false);

      buffer.push(2);
      buffer.push(3);
      expect(buffer.isFull).toBe(true);

      buffer.pop();
      expect(buffer.isFull).toBe(false);
    });
  });
});
