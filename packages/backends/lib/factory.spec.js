const { PilafBackendFactory } = require('./factory');
const { RconBackend } = require('./rcon-backend');
const { MineflayerBackend } = require('./mineflayer-backend');

// Mock the backend classes
jest.mock('./rcon-backend.js');
jest.mock('./mineflayer-backend.js');

describe('PilafBackendFactory', () => {
  let mockConfig;

  beforeEach(() => {
    mockConfig = {
      host: 'localhost',
      port: 25575,
      password: 'test'
    };

    // Clear all mocks before each test
    jest.clearAllMocks();
  });

  describe('create', () => {
    it('should create RconBackend for "rcon" type', () => {
      const mockBackend = { connect: jest.fn().mockReturnValue({ connected: true }) };
      RconBackend.mockImplementation(() => mockBackend);

      PilafBackendFactory.create('rcon', mockConfig);

      expect(RconBackend).toHaveBeenCalledTimes(1);
      expect(mockBackend.connect).toHaveBeenCalledWith(mockConfig);
    });

    it('should create MineflayerBackend for "mineflayer" type', () => {
      const mockBackend = { connect: jest.fn().mockReturnValue({ connected: true }) };
      MineflayerBackend.mockImplementation(() => mockBackend);

      PilafBackendFactory.create('mineflayer', mockConfig);

      expect(MineflayerBackend).toHaveBeenCalledTimes(1);
      expect(mockBackend.connect).toHaveBeenCalledWith(mockConfig);
    });

    it('should handle uppercase "RCON" type', () => {
      const mockBackend = { connect: jest.fn().mockReturnValue({ connected: true }) };
      RconBackend.mockImplementation(() => mockBackend);

      PilafBackendFactory.create('RCON', mockConfig);

      expect(RconBackend).toHaveBeenCalledTimes(1);
    });

    it('should handle mixed case "Mineflayer" type', () => {
      const mockBackend = { connect: jest.fn().mockReturnValue({ connected: true }) };
      MineflayerBackend.mockImplementation(() => mockBackend);

      PilafBackendFactory.create('Mineflayer', mockConfig);

      expect(MineflayerBackend).toHaveBeenCalledTimes(1);
    });

    it('should throw error for unknown backend type', () => {
      expect(() => {
        PilafBackendFactory.create('unknown', mockConfig);
      }).toThrow("Unknown backend type: unknown. Supported types: 'rcon', 'mineflayer'");
    });

    it('should use empty config when not provided', () => {
      const mockBackend = { connect: jest.fn().mockReturnValue({ connected: true }) };
      RconBackend.mockImplementation(() => mockBackend);

      PilafBackendFactory.create('rcon');

      expect(mockBackend.connect).toHaveBeenCalledWith({});
    });

    it('should return the result of backend connect method', () => {
      const expectedConnection = { connected: true, backend: 'rcon' };
      const mockBackend = { connect: jest.fn().mockReturnValue(expectedConnection) };
      RconBackend.mockImplementation(() => mockBackend);

      const result = PilafBackendFactory.create('rcon', mockConfig);

      expect(result).toEqual(expectedConnection);
    });
  });
});
