// packages/framework/lib/helpers/events.js

async function waitForEvents(bot, eventName, count, timeout = 5000) {
  const events = [];
  const handler = (data) => events.push({ type: eventName, data });

  bot.on(eventName, handler);

  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bot.removeListener(eventName, handler);
      if (events.length >= count) {
        resolve(events);
      } else {
        reject(new Error(`Timeout waiting for ${count} ${eventName} events (got ${events.length})`));
      }
    }, timeout);

    const checkComplete = () => {
      if (events.length >= count) {
        clearTimeout(timer);
        bot.removeListener(eventName, handler);
        resolve(events);
      }
    };

    bot.on(eventName, checkComplete);
  });
}

function captureEvents(bot, eventNames) {
  const captured = [];
  const handlers = {};

  eventNames.forEach(eventName => {
    const handler = (data) => captured.push({ type: eventName, data });
    handlers[eventName] = handler;
    bot.on(eventName, handler);
  });

  return {
    events: captured,
    release: () => {
      Object.entries(handlers).forEach(([eventName, handler]) => {
        bot.removeListener(eventName, handler);
      });
    }
  };
}

module.exports = {
  waitForEvents,
  captureEvents
};
