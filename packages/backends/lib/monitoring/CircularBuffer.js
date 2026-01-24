/**
 * CircularBuffer - Fixed-size circular buffer implementation
 *
 * A circular buffer is a fixed-size data structure that uses a single,
 * contiguous block of memory. When the buffer is full, new data overwrites
 * the oldest data.
 *
 * This provides O(1) time complexity for push and pop operations,
 * making it ideal for streaming data and log monitoring scenarios.
 *
 * Usage Example:
 *   const buffer = new CircularBuffer(3);
 *   buffer.push(1);  // [1]
 *   buffer.push(2);  // [1, 2]
 *   buffer.push(3);  // [1, 2, 3]
 *   buffer.push(4);  // [2, 3, 4] - 1 was overwritten
 *   buffer.pop();    // Returns 2, buffer is [3, 4]
 */

/**
 * CircularBuffer class for efficient fixed-size buffer operations
 */
class CircularBuffer {
  /**
   * Create a CircularBuffer
   *
   * @param {number} capacity - Maximum number of elements (must be > 0)
   * @throws {Error} If capacity is not a positive integer
   */
  constructor(capacity) {
    if (!Number.isInteger(capacity) || capacity <= 0) {
      throw new Error(`Capacity must be a positive integer, got: ${capacity}`);
    }

    /**
     * Maximum capacity of the buffer
     * @private
     * @type {number}
     */
    this._capacity = capacity;

    /**
     * Internal array to store elements
     * @private
     * @type {Array}
     */
    this._buffer = new Array(capacity);

    /**
     * Index of the oldest element (front of queue)
     * @private
     * @type {number}
     */
    this._head = 0;

    /**
     * Index where the next element will be inserted (back of queue)
     * @private
     * @type {number}
     */
    this._tail = 0;

    /**
     * Current number of elements in the buffer
     * @private
     * @type {number}
     */
    this._size = 0;
  }

  /**
   * Add a value to the buffer
   *
   * If the buffer is full, the oldest value is removed and returned.
   *
   * @param {*} value - Value to add
   * @returns {*} - The displaced value if buffer was full, undefined otherwise
   */
  push(value) {
    const displaced = this.isFull ? this._buffer[this._head] : undefined;

    this._buffer[this._tail] = value;
    this._tail = (this._tail + 1) % this._capacity;

    if (this.isFull) {
      this._head = (this._head + 1) % this._capacity;
    } else {
      this._size++;
    }

    return displaced;
  }

  /**
   * Remove and return the oldest value
   *
   * @returns {*} - The oldest value, or undefined if buffer is empty
   */
  pop() {
    if (this.isEmpty) {
      return undefined;
    }

    const value = this._buffer[this._head];
    this._buffer[this._head] = undefined; // Clear reference
    this._head = (this._head + 1) % this._capacity;
    this._size--;

    return value;
  }

  /**
   * Get value at logical index (0 = oldest, size-1 = newest)
   *
   * @param {number} index - Logical index
   * @returns {*} - Value at index, or undefined if index is out of bounds
   */
  get(index) {
    if (index < 0 || index >= this._size) {
      return undefined;
    }

    const physicalIndex = (this._head + index) % this._capacity;
    return this._buffer[physicalIndex];
  }

  /**
   * Convert buffer to a regular array (ordered from oldest to newest)
   *
   * @returns {Array} - Array containing all elements
   */
  toArray() {
    const result = [];
    for (let i = 0; i < this._size; i++) {
      result.push(this.get(i));
    }
    return result;
  }

  /**
   * Extract a slice of the buffer
   *
   * @param {number} start - Start index (default: 0)
   * @param {number} end - End index (default: size)
   * @returns {Array} - Array containing elements from start to end-1
   */
  slice(start = 0, end = this._size) {
    const result = [];
    for (let i = start; i < Math.min(end, this._size); i++) {
      result.push(this.get(i));
    }
    return result;
  }

  /**
   * Clear all elements from the buffer
   */
  clear() {
    this._buffer = new Array(this._capacity);
    this._head = 0;
    this._tail = 0;
    this._size = 0;
  }

  /**
   * Get the current number of elements
   *
   * @type {number}
   */
  get size() {
    return this._size;
  }

  /**
   * Get the maximum capacity
   *
   * @type {number}
   */
  get capacity() {
    return this._capacity;
  }

  /**
   * Check if buffer is empty
   *
   * @type {boolean}
   */
  get isEmpty() {
    return this._size === 0;
  }

  /**
   * Check if buffer is full
   *
   * @type {boolean}
   */
  get isFull() {
    return this._size === this._capacity;
  }
}

module.exports = { CircularBuffer };
