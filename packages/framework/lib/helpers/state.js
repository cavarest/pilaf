// packages/framework/lib/helpers/state.js

function captureState(data) {
  return {
    timestamp: Date.now(),
    data: JSON.parse(JSON.stringify(data)) // Deep clone
  };
}

function compareStates(before, after, description = 'State comparison') {
  const changes = [];

  const beforeKeys = Object.keys(before.data || {});
  const afterKeys = Object.keys(after.data || {});

  const allKeys = new Set([...beforeKeys, ...afterKeys]);

  allKeys.forEach(key => {
    const beforeVal = before.data[key];
    const afterVal = after.data[key];

    if (JSON.stringify(beforeVal) !== JSON.stringify(afterVal)) {
      changes.push({
        key,
        before: beforeVal,
        after: afterVal
      });
    }
  });

  return {
    description,
    before,
    after,
    changes,
    hasChanges: changes.length > 0
  };
}

module.exports = {
  captureState,
  compareStates
};
