/**
 * Unit tests for EntityUtils
 */

const { findEntity, getNearestEntity, getEntitiesOfType, findEntities, getDistanceToEntity, isEntityAlive, getEntityDisplayName } = require('@pilaf/framework/lib/helpers/entities.js');

describe('EntityUtils', () => {
  describe('findEntity', () => {
    let mockBot;
    let mockEntities;

    beforeEach(() => {
      // Setup mock entities
      mockEntities = {
        100: {
          id: 100,
          name: 'zombie',
          displayName: 'Zombie',
          position: { x: 10, y: 64, z: 10 }
        },
        101: {
          id: 101,
          name: 'player',
          username: 'TestPlayer',
          displayName: 'TestPlayer',
          position: { x: 5, y: 64, z: 5 }
        },
        102: {
          id: 102,
          name: 'villager',
          displayName: 'Villager',
          customName: 'Trader Bob',
          position: { x: 15, y: 64, z: 15 }
        },
        103: {
          id: 103,
          name: 'cow',
          displayName: 'Cow',
          customName: { text: 'Bessie' },
          position: { x: 20, y: 64, z: 20 }
        }
      };

      mockBot = {
        entities: mockEntities
      };
    });

    it('should find entity by numeric ID', () => {
      const result = findEntity(mockBot, 100);
      expect(result).toBeDefined();
      expect(result.id).toBe(100);
      expect(result.name).toBe('zombie');
    });

    it('should find entity by custom name (string)', () => {
      const result = findEntity(mockBot, 'Trader Bob');
      expect(result).toBeDefined();
      expect(result.id).toBe(102);
      expect(result.name).toBe('villager');
    });

    it('should find entity by custom name (text component)', () => {
      const result = findEntity(mockBot, 'Bessie');
      expect(result).toBeDefined();
      expect(result.id).toBe(103);
      expect(result.name).toBe('cow');
    });

    it('should find entity by username (player)', () => {
      const result = findEntity(mockBot, 'TestPlayer');
      expect(result).toBeDefined();
      expect(result.id).toBe(101);
      expect(result.name).toBe('player');
    });

    it('should find entity by entity type name', () => {
      const result = findEntity(mockBot, 'zombie');
      expect(result).toBeDefined();
      expect(result.id).toBe(100);
    });

    it('should return null for non-existent entity', () => {
      const result = findEntity(mockBot, 'NonExistent');
      expect(result).toBeNull();
    });

    it('should return null when bot is null', () => {
      const result = findEntity(null, 'zombie');
      expect(result).toBeNull();
    });

    it('should return null when bot.entities is null', () => {
      const result = findEntity({}, 'zombie');
      expect(result).toBeNull();
    });
  });

  describe('getNearestEntity', () => {
    let mockBot;

    beforeEach(() => {
      mockBot = {
        entity: {
          position: { x: 0, y: 64, z: 0 }
        },
        entities: {
          100: { name: 'zombie', position: { x: 5, y: 64, z: 0 } },
          101: { name: 'zombie', position: { x: 10, y: 64, z: 0 } },
          102: { name: 'zombie', position: { x: 2, y: 64, z: 0 } },
          103: { name: 'skeleton', position: { x: 3, y: 64, z: 0 } }
        }
      };
    });

    it('should return nearest entity of type', () => {
      const result = getNearestEntity(mockBot, 'zombie', 32);
      expect(result).toBeDefined();
      expect(result.name).toBe('zombie');
      // Distance should be 2 blocks
      expect(result.position.x).toBe(2);
    });

    it('should respect max distance limit', () => {
      const result = getNearestEntity(mockBot, 'zombie', 4);
      expect(result).toBeDefined();
      expect(result.position.x).toBeLessThanOrEqual(4);
    });

    it('should return null when no entity found', () => {
      const result = getNearestEntity(mockBot, 'creeper', 32);
      expect(result).toBeNull();
    });

    it('should return null when bot is invalid', () => {
      expect(getNearestEntity(null, 'zombie')).toBeNull();
      expect(getNearestEntity({})).toBeNull();
      expect(getNearestEntity({ entities: {} })).toBeNull();
    });
  });

  describe('getEntitiesOfType', () => {
    let mockBot;

    beforeEach(() => {
      mockBot = {
        entity: {
          position: { x: 0, y: 64, z: 0 }
        },
        entities: {
          100: { name: 'zombie', position: { x: 5, y: 64, z: 0 } },
          101: { name: 'zombie', position: { x: 10, y: 64, z: 0 } },
          102: { name: 'skeleton', position: { x: 3, y: 64, z: 0 } }
        }
      };
    });

    it('should return all entities of type within distance', () => {
      const result = getEntitiesOfType(mockBot, 'zombie', 32);
      expect(result).toHaveLength(2);
      expect(result.every(e => e.name === 'zombie')).toBe(true);
    });

    it('should return empty array when no entities found', () => {
      const result = getEntitiesOfType(mockBot, 'creeper', 32);
      expect(result).toEqual([]);
    });

    it('should return empty array for invalid bot', () => {
      expect(getEntitiesOfType(null, 'zombie')).toEqual([]);
      expect(getEntitiesOfType({})).toEqual([]);
    });
  });

  describe('findEntities', () => {
    let mockBot;

    beforeEach(() => {
      mockBot = {
        entity: {
          position: { x: 0, y: 64, z: 0 }
        },
        entities: {
          100: { name: 'zombie', health: 20, position: { x: 5, y: 64, z: 0 } },
          101: { name: 'zombie', health: 5, position: { x: 10, y: 64, z: 0 } },
          102: { name: 'skeleton', health: 8, position: { x: 3, y: 64, z: 0 } }
        }
      };
    });

    it('should find entities matching predicate', () => {
      const result = findEntities(mockBot, (e) => e.health < 10, 32);
      expect(result).toHaveLength(2);
      expect(result.some(e => e.name === 'zombie' && e.health === 5)).toBe(true);
      expect(result.some(e => e.name === 'skeleton' && e.health === 8)).toBe(true);
    });

    it('should respect max distance', () => {
      const result = findEntities(mockBot, (e) => e.name === 'zombie', 6);
      expect(result).toHaveLength(1);
      expect(result[0].position.x).toBe(5);
    });

    it('should return empty array when predicate is not a function', () => {
      const result = findEntities(mockBot, 'not a function', 32);
      expect(result).toEqual([]);
    });

    it('should return empty array for invalid bot', () => {
      expect(findEntities(null, () => true)).toEqual([]);
    });
  });

  describe('getDistanceToEntity', () => {
    it('should calculate distance correctly', () => {
      const mockBot = {
        entity: { position: { x: 0, y: 64, z: 0 } }
      };
      const entity = { position: { x: 3, y: 64, z: 4 } }; // 3-4-5 triangle

      const result = getDistanceToEntity(mockBot, entity);
      expect(result).toBe(5);
    });

    it('should return null for invalid parameters', () => {
      expect(getDistanceToEntity(null, {})).toBeNull();
      expect(getDistanceToEntity({})).toBeNull();
      expect(getDistanceToEntity({ entity: {} }, {})).toBeNull();
    });
  });

  describe('isEntityAlive', () => {
    it('should return true for entity with health > 0', () => {
      expect(isEntityAlive({ health: 10 })).toBe(true);
    });

    it('should return false for entity with health <= 0', () => {
      expect(isEntityAlive({ health: 0 })).toBe(false);
      expect(isEntityAlive({ health: -5 })).toBe(false);
    });

    it('should return true for entity without health property', () => {
      expect(isEntityAlive({ name: 'item' })).toBe(true);
    });

    it('should return false for null entity', () => {
      expect(isEntityAlive(null)).toBe(false);
    });
  });

  describe('getEntityDisplayName', () => {
    it('should prefer custom name (string)', () => {
      const entity = {
        name: 'villager',
        displayName: 'Villager',
        customName: 'Trader Bob'
      };
      expect(getEntityDisplayName(entity)).toBe('Trader Bob');
    });

    it('should prefer custom name (text component)', () => {
      const entity = {
        name: 'cow',
        displayName: 'Cow',
        customName: { text: 'Bessie' }
      };
      expect(getEntityDisplayName(entity)).toBe('Bessie');
    });

    it('should use username for players', () => {
      const entity = {
        name: 'player',
        displayName: 'Player',
        username: 'TestPlayer'
      };
      expect(getEntityDisplayName(entity)).toBe('TestPlayer');
    });

    it('should use display name as fallback', () => {
      const entity = {
        name: 'zombie',
        displayName: 'Zombie'
      };
      expect(getEntityDisplayName(entity)).toBe('Zombie');
    });

    it('should use entity type name as final fallback', () => {
      const entity = {
        name: 'skeleton'
      };
      expect(getEntityDisplayName(entity)).toBe('skeleton');
    });

    it('should return "Unknown" for null entity', () => {
      expect(getEntityDisplayName(null)).toBe('Unknown');
    });
  });
});
