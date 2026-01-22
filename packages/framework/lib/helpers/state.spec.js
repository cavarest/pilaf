const { captureState, compareStates } = require('./state');

describe('state helpers', () => {
  describe('captureState', () => {
    it('should capture state with timestamp', () => {
      const beforeTime = Date.now();
      const result = captureState({ foo: 'bar' });
      const afterTime = Date.now();

      expect(result).toHaveProperty('timestamp');
      expect(result.timestamp).toBeGreaterThanOrEqual(beforeTime);
      expect(result.timestamp).toBeLessThanOrEqual(afterTime);
      expect(result.data).toEqual({ foo: 'bar' });
    });

    it('should deep clone the data', () => {
      const originalData = {
        nested: { value: 42 },
        array: [1, 2, 3]
      };
      const result = captureState(originalData);

      // Modify original
      originalData.nested.value = 999;
      originalData.array.push(4);

      // Result should be unchanged
      expect(result.data.nested.value).toBe(42);
      expect(result.data.array).toEqual([1, 2, 3]);
    });

    it('should handle null values', () => {
      expect(captureState(null).data).toBeNull();
    });

    it('should handle undefined values', () => {
      // JSON.stringify(undefined) returns undefined, which JSON.parse cannot parse
      // The current implementation will throw for undefined
      expect(() => captureState(undefined)).toThrow();
    });

    it('should handle complex objects', () => {
      const complexObj = {
        string: 'test',
        number: 123,
        boolean: true,
        null: null,
        nested: { deep: { value: 'nested' } },
        array: [1, 2, { inArray: true }]
      };

      const result = captureState(complexObj);
      expect(result.data).toEqual(complexObj);
    });
  });

  describe('compareStates', () => {
    it('should detect no changes when states are identical', () => {
      const before = captureState({ foo: 'bar' });
      const after = captureState({ foo: 'bar' });

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(false);
      expect(result.changes).toEqual([]);
      expect(result.before).toBe(before);
      expect(result.after).toBe(after);
    });

    it('should detect single value change', () => {
      const before = captureState({ foo: 'bar' });
      const after = captureState({ foo: 'baz' });

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(true);
      expect(result.changes).toHaveLength(1);
      expect(result.changes[0]).toEqual({
        key: 'foo',
        before: 'bar',
        after: 'baz'
      });
    });

    it('should detect multiple value changes', () => {
      const before = captureState({ foo: 'bar', num: 1, bool: true });
      const after = captureState({ foo: 'baz', num: 2, bool: false });

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(true);
      expect(result.changes).toHaveLength(3);
    });

    it('should detect added keys', () => {
      const before = captureState({ foo: 'bar' });
      const after = captureState({ foo: 'bar', newKey: 'value' });

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(true);
      expect(result.changes).toHaveLength(1);
      expect(result.changes[0]).toEqual({
        key: 'newKey',
        before: undefined,
        after: 'value'
      });
    });

    it('should detect removed keys', () => {
      const before = captureState({ foo: 'bar', oldKey: 'value' });
      const after = captureState({ foo: 'bar' });

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(true);
      expect(result.changes).toHaveLength(1);
      expect(result.changes[0]).toEqual({
        key: 'oldKey',
        before: 'value',
        after: undefined
      });
    });

    it('should handle nested object changes', () => {
      const before = captureState({ nested: { value: 1 } });
      const after = captureState({ nested: { value: 2 } });

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(true);
      expect(result.changes).toHaveLength(1);
    });

    it('should use default description', () => {
      const before = captureState({ foo: 'bar' });
      const after = captureState({ foo: 'bar' });

      const result = compareStates(before, after);

      expect(result.description).toBe('State comparison');
    });

    it('should use custom description', () => {
      const before = captureState({ foo: 'bar' });
      const after = captureState({ foo: 'bar' });

      const result = compareStates(before, after, 'Custom comparison');

      expect(result.description).toBe('Custom comparison');
    });

    it('should handle empty objects', () => {
      const before = captureState({});
      const after = captureState({});

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(false);
      expect(result.changes).toEqual([]);
    });

    it('should handle null data in states', () => {
      const before = captureState(null);
      const after = captureState(null);

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(false);
    });

    it('should handle array changes', () => {
      const before = captureState({ items: [1, 2, 3] });
      const after = captureState({ items: [1, 2, 4] });

      const result = compareStates(before, after);

      expect(result.hasChanges).toBe(true);
      expect(result.changes).toHaveLength(1);
    });
  });
});
