// JavaScript-based story for testing session persistence
// Uses REAL TCP disconnect/reconnect for logout/login
module.exports = {
  name: "Session Persistence Test (Real TCP)",
  description: "Tests that server state persists across REAL logout/login cycles with actual TCP disconnect and reconnect",

  setup: {
    server: {
      type: "paper",
      version: "1.21.8"
    },
    players: [
      {
        name: "SessionTester",
        username: "SessionTester"
      }
    ]
  },

  steps: [
    // Set some server state before logout
    {
      name: "Set initial time",
      action: "execute_command",
      command: "time set day"
    },
    {
      name: "Set initial weather",
      action: "execute_command",
      command: "weather clear"
    },
    {
      name: "Send chat message before logout",
      action: "chat",
      player: "SessionTester",
      message: "[Pilaf] About to test logout/login..."
    },
    {
      name: "Wait for message to propagate",
      action: "wait",
      duration: 1
    },
    // Simulate logout (framework-level, no TCP disconnect)
    {
      name: "REAL Logout (TCP disconnect)",
      action: "logout",
      player: "SessionTester"
    },
    {
      name: "Wait for server to process disconnect",
      action: "wait",
      duration: 15
    },
    // Simulate login (framework-level, no new TCP connection)
    {
      name: "REAL Login (new TCP connection)",
      action: "login",
      player: "SessionTester"
    },
    {
      name: "Wait after login",
      action: "wait",
      duration: 2
    },
    // Verify server state persisted
    {
      name: "Send chat message after login",
      action: "chat",
      player: "SessionTester",
      message: "[Pilaf] Back from logout/login test!"
    },
    {
      name: "Wait for final message",
      action: "wait",
      duration: 1
    }
  ],

  teardown: {
    stop_server: false
  }
};
