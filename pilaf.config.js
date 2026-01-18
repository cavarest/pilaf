module.exports = {
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
