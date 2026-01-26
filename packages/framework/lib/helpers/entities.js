/**
 * EntityUtils - Utility functions for working with Mineflayer entities
 *
 * Provides helper methods for finding and querying entities in a
 * Mineflayer bot's entity list.
 *
 * Responsibilities (MECE):
 * - ONLY: Entity lookup and query utilities
 * - NOT: Entity manipulation (use bot methods directly)
 * - NOT: Entity spawning/despawning (server-side actions)
 */

/**
 * Find an entity by identifier
 *
 * Searches for an entity using multiple strategies:
 * 1. Direct entity ID (number)
 * 2. Custom name (named entities, mobs with nametags)
 * 3. Display name (player names, entity display names)
 * 4. Entity type name (zombie, skeleton, etc.)
 *
 * @param {Object} bot - Mineflayer bot instance
 * @param {number|string} identifier - Entity ID, name, or custom name
 * @returns {Object|null} Entity object or null if not found
 *
 * @example
 * // Find by entity ID
 * const entity = EntityUtils.findEntity(bot, 123);
 *
 * @example
 * // Find by custom name
 * const entity = EntityUtils.findEntity(bot, 'MyPet');
 *
 * @example
 * // Find by entity type
 * const entity = EntityUtils.findEntity(bot, 'zombie');
 */
function findEntity(bot, identifier) {
  if (!bot || !bot.entities) {
    console.log(`[EntityUtils] No bot or bot.entities`);
    return null;
  }

  // Strategy 1: Direct entity ID (number)
  if (typeof identifier === 'number') {
    return bot.entities[identifier] || null;
  }

  const allEntities = Object.values(bot.entities).filter(e => e);

  // COMPREHENSIVE DEBUG: Log entity state on failure
  const debugLog = () => {
    console.log(`[EntityUtils DEBUG] Searching for: "${identifier}"`);
    console.log(`[EntityUtils DEBUG] Total entities: ${allEntities.length}`);
    if (allEntities.length > 0) {
      allEntities.slice(0, 5).forEach((e, i) => {
        console.log(`[EntityUtils DEBUG] Entity ${i}:`, {
          id: e.id,
          name: e.name,
          type: e.type,
          displayName: e.displayName,
          username: e.username,
          customName: e.customName
        });
      });
    }
  };

  // Strategy 2: Try custom name (named mobs, nametags)
  let entity = Object.values(bot.entities).find(e => {
    if (!e) return false;
    const customName = e.customName;
    // Check both string and text component formats
    if (typeof customName === 'string') {
      return customName === identifier;
    }
    if (customName && typeof customName === 'object' && customName.text) {
      return customName.text === identifier;
    }
    return false;
  });

  if (entity) {
    console.log(`[EntityUtils] Found by customName: "${identifier}"`);
    return entity;
  }

  // Strategy 3: Try display name (players, some entities)
  entity = Object.values(bot.entities).find(e => {
    if (!e) return false;
    return e.displayName === identifier ||
           (e.username && e.username === identifier);
  });

  if (entity) {
    console.log(`[EntityUtils] Found by displayName/username: "${identifier}"`);
    return entity;
  }

  // Strategy 4: Try entity type name (zombie, skeleton, etc.)
  entity = Object.values(bot.entities).find(e => {
    if (!e) return false;
    // Direct match on entity name
    return e.name === identifier ||
           (e.type && e.type === identifier);
  });

  if (entity) {
    console.log(`[EntityUtils] Found by name/type: "${identifier}"`);
    return entity;
  }

  // NOT FOUND - log debug info
  debugLog();
  return null;
}

/**
 * Calculate distance between two positions
 *
 * @private
 * @param {Object} pos1 - First position {x, y, z}
 * @param {Object} pos2 - Second position {x, y, z}
 * @returns {number} Distance in blocks
 */
function _calculateDistance(pos1, pos2) {
  if (!pos1 || !pos2) return Infinity;
  const dx = pos2.x - pos1.x;
  const dy = pos2.y - pos1.y;
  const dz = pos2.z - pos1.z;
  return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

/**
 * Get the nearest entity of a specific type
 *
 * @param {Object} bot - Mineflayer bot instance
 * @param {string} typeName - Entity type name (e.g., 'zombie', 'player')
 * @param {number} [maxDistance=32] - Maximum search distance in blocks
 * @returns {Object|null} Nearest entity or null if not found
 *
 * @example
 * // Find nearest zombie within 32 blocks
 * const zombie = EntityUtils.getNearestEntity(bot, 'zombie', 32);
 *
 * @example
 * // Find nearest player within 16 blocks
 * const player = EntityUtils.getNearestEntity(bot, 'player', 16);
 */
function getNearestEntity(bot, typeName, maxDistance = 32) {
  if (!bot || !bot.entities || !bot.entity) {
    return null;
  }

  const playerPos = bot.entity.position;

  // Find all entities of the specified type within max distance
  const nearbyEntities = Object.values(bot.entities)
    .filter(e => {
      if (!e || !e.position || !e.name) return false;
      // Match entity type
      return e.name === typeName;
    })
    .map(e => ({
      entity: e,
      distance: _calculateDistance(playerPos, e.position)
    }))
    .filter(e => e.distance <= maxDistance);

  // Sort by distance (nearest first)
  nearbyEntities.sort((a, b) => a.distance - b.distance);

  // Return nearest entity or null
  return nearbyEntities.length > 0 ? nearbyEntities[0].entity : null;
}

/**
 * Get all entities of a specific type within distance
 *
 * @param {Object} bot - Mineflayer bot instance
 * @param {string} typeName - Entity type name
 * @param {number} [maxDistance=32] - Maximum search distance in blocks
 * @returns {Array<Object>} Array of matching entities (sorted by distance)
 *
 * @example
 * // Get all zombies within 32 blocks
 * const zombies = EntityUtils.getEntitiesOfType(bot, 'zombie', 32);
 */
function getEntitiesOfType(bot, typeName, maxDistance = 32) {
  if (!bot || !bot.entities || !bot.entity) {
    return [];
  }

  const playerPos = bot.entity.position;

  const nearbyEntities = Object.values(bot.entities)
    .filter(e => {
      if (!e || !e.position || !e.name) return false;
      return e.name === typeName && _calculateDistance(playerPos, e.position) <= maxDistance;
    })
    .map(e => ({
      entity: e,
      distance: _calculateDistance(playerPos, e.position)
    }))
    .sort((a, b) => a.distance - b.distance);

  return nearbyEntities.map(e => e.entity);
}

/**
 * Find multiple entities by matching a predicate
 *
 * @param {Object} bot - Mineflayer bot instance
 * @param {Function} predicate - Function that returns true for matching entities
 * @param {number} [maxDistance=32] - Maximum search distance in blocks
 * @returns {Array<Object>} Array of matching entities (sorted by distance)
 *
 * @example
 * // Find all hostile mobs with low health
 * const weakHostiles = EntityUtils.findEntities(bot, (e) => {
 *   return e.health < 10 && isHostile(e.name);
 * }, 32);
 */
function findEntities(bot, predicate, maxDistance = 32) {
  if (!bot || !bot.entities || !bot.entity || typeof predicate !== 'function') {
    return [];
  }

  const playerPos = bot.entity.position;

  const matchingEntities = Object.values(bot.entities)
    .filter(e => {
      if (!e || !e.position) return false;
      return _calculateDistance(playerPos, e.position) <= maxDistance && predicate(e);
    })
    .map(e => ({
      entity: e,
      distance: _calculateDistance(playerPos, e.position)
    }))
    .sort((a, b) => a.distance - b.distance);

  return matchingEntities.map(e => e.entity);
}

/**
 * Get distance from player to an entity
 *
 * @param {Object} bot - Mineflayer bot instance
 * @param {Object} entity - Entity object
 * @returns {number|null} Distance in blocks, or null if invalid
 */
function getDistanceToEntity(bot, entity) {
  if (!bot || !bot.entity || !entity || !entity.position) {
    return null;
  }

  return _calculateDistance(bot.entity.position, entity.position);
}

/**
 * Check if an entity is alive (has health > 0)
 *
 * @param {Object} entity - Entity object
 * @returns {boolean} True if entity is alive
 */
function isEntityAlive(entity) {
  if (!entity) return false;
  // Some entities may not have health property
  return entity.health === undefined || entity.health > 0;
}

/**
 * Get entity display name (with fallbacks)
 *
 * Returns the most appropriate name for display:
 * 1. Custom name (nametag)
 * 2. Username (for players)
 * 3. Display name
 * 4. Entity type name
 *
 * @param {Object} entity - Entity object
 * @returns {string} Display name
 */
function getEntityDisplayName(entity) {
  if (!entity) return 'Unknown';

  // Try custom name first
  if (entity.customName) {
    if (typeof entity.customName === 'string') {
      return entity.customName;
    }
    if (typeof entity.customName === 'object' && entity.customName.text) {
      return entity.customName.text;
    }
  }

  // Try username (for players)
  if (entity.username) {
    return entity.username;
  }

  // Try display name
  if (entity.displayName) {
    return entity.displayName;
  }

  // Fall back to entity type name
  return entity.name || 'Unknown';
}

module.exports = {
  findEntity,
  getNearestEntity,
  getEntitiesOfType,
  findEntities,
  getDistanceToEntity,
  isEntityAlive,
  getEntityDisplayName
};
