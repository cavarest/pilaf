# Pilaf

Pure JavaScript testing framework for Minecraft PaperMC plugin development.

## Features

- **Mineflayer Integration**: Realistic player simulation for complex interactions
- **RCON Support**: Direct server command execution
- **Jest-Based**: Familiar describe/it syntax with full Jest ecosystem
- **Interactive Reports**: Vue.js-powered HTML reports with state comparisons
- **Type-Safe**: Full TypeScript support (optional)

## Quick Start

```bash
# Install
npm install -g @pilaf/cli

# Run tests
pilaf test

# Health check
pilaf health-check
```

## Example Test

```javascript
const { pilaf, rcon, mineflayer } = require('@pilaf/framework');

describe('My Plugin Feature', () => {
  let bot, server;

  beforeAll(async () => {
    server = await rcon.connect({ host: 'localhost', port: 25575 });
    bot = await mineflayer.createBot({ username: 'TestBot' });
  });

  it('should do something', async () => {
    await server.send('op TestBot');
    await bot.chat('/myplugin command');
    const events = await pilaf.waitForEvents(bot, 'entityHurt', 1, 5000);
    expect(events.length).toBe(1);
  });
});
```

## Configuration

Create `pilaf.config.js`:

```javascript
module.exports = {
  backend: {
    rcon: {
      host: 'localhost',
      port: 25575,
      password: process.env.RCON_PASSWORD
    },
    mineflayer: {
      host: 'localhost',
      port: 25565,
      auth: 'offline'
    }
  },
  reportDir: 'target/pilaf-reports'
};
```

## Documentation

See [docs/](docs/) for full documentation.
