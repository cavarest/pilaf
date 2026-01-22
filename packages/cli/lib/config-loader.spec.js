const fs = require('fs');
const path = require('path');
const { loadConfig } = require('./config-loader');

// Mock fs and path
jest.mock('fs');
jest.mock('path');

describe('config-loader', () => {
  let originalEnv;
  const originalCwd = process.cwd;

  beforeEach(() => {
    originalEnv = { ...process.env };
    jest.clearAllMocks();

    // Setup default mocks
    process.cwd = jest.fn(() => '/test/project');
    path.resolve = jest.fn((p) => p);
    path.join = jest.fn((...args) => args.join('/'));
  });

  afterEach(() => {
    process.env = originalEnv;
    process.cwd = originalCwd;
  });

  describe('loadConfig', () => {
    const defaultConfig = {
      backend: {
        rcon: {
          host: 'localhost',
          port: 25575,
          password: 'dragon123'
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

    it('should return default config when no config file exists', () => {
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result).toEqual(defaultConfig);
      expect(fs.existsSync).toHaveBeenCalled();
    });

    it('should load config from provided path', () => {
      const customPath = 'custom.config.js';
      fs.existsSync = jest.fn((p) => p === customPath);

      // Since we can't mock require properly, we just test the path is checked
      try {
        loadConfig(customPath);
      } catch (e) {
        // Expected to fail on require since file doesn't exist
      }

      expect(fs.existsSync).toHaveBeenCalledWith(customPath);
    });

    it('should check for default config path', () => {
      fs.existsSync = jest.fn(() => false);

      loadConfig();

      expect(fs.existsSync).toHaveBeenCalledWith('/test/project/pilaf.config.js');
    });

    it('should use RCON_PASSWORD from env when not set in config', () => {
      process.env.RCON_PASSWORD = 'env_password';
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.backend.rcon.password).toBe('env_password');
    });

    it('should use default RCON password when env var not set', () => {
      delete process.env.RCON_PASSWORD;
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.backend.rcon.password).toBe('dragon123');
    });

    it('should check custom config path when provided', () => {
      const customPath = 'my/custom/path.js';
      fs.existsSync = jest.fn(() => false);

      try {
        loadConfig(customPath);
      } catch (e) {
        // Expected
      }

      expect(fs.existsSync).toHaveBeenCalledWith(customPath);
    });

    it('should have correct default backend config for RCON', () => {
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.backend.rcon).toEqual({
        host: 'localhost',
        port: 25575,
        password: 'dragon123'
      });
    });

    it('should have correct default backend config for Mineflayer', () => {
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.backend.mineflayer).toEqual({
        host: 'localhost',
        port: 25565,
        auth: 'offline'
      });
    });

    it('should have correct default test patterns', () => {
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.testMatch).toEqual(['**/*.pilaf.test.js', '**/*.story.test.js']);
      expect(result.testIgnore).toEqual(['**/node_modules/**', '**/dist/**']);
    });

    it('should have correct default report directory', () => {
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.reportDir).toBe('target/pilaf-reports');
    });

    it('should have correct default timeout and retries', () => {
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.timeout).toBe(30000);
      expect(result.retries).toBe(0);
    });

    it('should have verbose disabled by default', () => {
      fs.existsSync = jest.fn(() => false);

      const result = loadConfig();

      expect(result.verbose).toBe(false);
    });
  });
});
