// packages/framework/lib/matchers/game-matchers.js

function toHaveReceivedLightningStrikes(received, count) {
  const strikes = received.filter(e =>
    e.type === 'entityHurt' && e.data?.damageSource?.type === 'lightning'
  );

  return {
    pass: strikes.length === count,
    message: () => `expected ${count} lightning strikes, got ${strikes.length}`
  };
}

module.exports = {
  toHaveReceivedLightningStrikes
};
