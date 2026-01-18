// packages/cli/lib/config-loader.js
const fs = require('fs');
const path = require('path');

function loadConfig(configPath) {
  const defaultConfig = {
    backend: {
      rcon: {
        host: 'localhost',
        port: 25575,
        password: process.env.RCON_PASSWORD || 'dragon123'
      },
      mineflayer: {
        host: 'localhost',
        port: 25565,
        auth: 'offline'
      }
    },
    testMatch: ['**/*.pilaf.test.js', '**/*.story.test.js'],
    testIgnore: ['**/node_modules/**', '**/dist/**'],
    reportDir: 'target/pilaf-reports',
    timeout: 30000,
    retries: 0,
    verbose: false
  };

  if (configPath && fs.existsSync(configPath)) {
    const userConfig = require(path.resolve(configPath));
    return { ...defaultConfig, ...userConfig };
  }

  const defaultPath = path.join(process.cwd(), 'pilaf.config.js');
  if (fs.existsSync(defaultPath)) {
    const userConfig = require(defaultPath);
    return { ...defaultConfig, ...userConfig };
  }

  return defaultConfig;
}

module.exports = { loadConfig };
