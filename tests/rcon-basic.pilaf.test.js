// Tests for basic RCON command execution
const { PilafBackendFactory } = require('@pilaf/backends');

describe('Basic RCON Commands', () => {
  let server;

  beforeAll(async () => {
    const backend = await PilafBackendFactory.create('rcon', {
      host: process.env.RCON_HOST || 'localhost',
      port: parseInt(process.env.RCON_PORT) || 25575,
      password: process.env.RCON_PASSWORD || 'cavarest'
    });
    server = backend;
  }, 10000);

  afterAll(async () => {
    if (server) {
      await server.send('say [Pilaf] RCON basic test completed');
      await server.disconnect();
    }
  });

  it('should execute time set command', async () => {
    const result = await server.send('time set noon');
    expect(result).toBeDefined();
    expect(result.raw).toBeTruthy();
  });

  it('should get server version', async () => {
    const result = await server.send('version');
    // Server may be checking version, so just check it doesn't error
    expect(result).toBeDefined();
    expect(result.raw).toBeTruthy();
  });

  it('should list online players', async () => {
    const result = await server.send('list');
    expect(result.raw).toBeTruthy();
  });

  it('should send server broadcast', async () => {
    const result = await server.send('say Test broadcast message');
    // say command doesn't return output, just check it doesn't throw
    expect(result).toBeDefined();
  });
});
