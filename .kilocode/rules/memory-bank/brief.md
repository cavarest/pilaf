# Pilaf: Minecraft Plugin Integration Testing Framework

PILAF (Paper Integration Layer for Automation Functions) is a comprehensive YAML
story-driven testing framework specifically designed for PaperMC Minecraft
plugin developers. It transforms complex Java integration tests into simple,
readable YAML scenarios that enable automated testing of Minecraft plugin
functionality.

## Purpose

Traditional Minecraft plugin testing requires extensive Java code to simulate
player actions, execute server commands, and validate results. PILAF eliminates
this complexity by providing a YAML-based story format that non-developers can
understand and maintain.

## Way it works

Pilaf believes that integration tests can be done using high-level stories that
describe the interactions between players and the server. These stories are
written in YAML format, making them easy to read and write.

Pilaf provides a test runner that interprets these YAML stories and executes
them against a live Minecraft server instance. The test runner handles the
details of connecting to the server, simulating player actions, and verifying
the expected outcomes.

Pilaf supports various backends:

- **Minecraft Server Backend**: Pilaf supports the server backends of PaperMC
  and Folio with the specified plugins and configurations. Pilaf launches the
  server using Docker Compose, and the Pilaf test runner (run on the host)
  interacts with the server using RCON (Remote Console) to execute commands and
  retrieve server output.

- **Minecraft Client Backend**: Pilaf can connect to the server using one or
  more headless clients, including Mineflayer (JavaScript) and HeadlessMC
  (Java), through a "Bridge" component (running on Docker) that translates Pilaf
  story steps into client actions. For testing, it uses Docker Compose to launch
  the Bridge which wraps and interacts with the clients.

Pilaf is designed to be a test orchestration framework, which has the ability
to interact with multiple Minecraft servers and clients in a single story. As
a result, a test report for a single story can include results from multiple
servers and clients in a single thread.

Pilaf test reports are generated in JUnit XML format, making them compatible
with various CI/CD systems and test reporting tools. It also produces rich HTML
reports that are interactive for easy visualization of test results.

Pilaf supports GitHub Actions CI/CD environments by utilizing Docker Compose on
the GitHub-hosted runners.


## Pilaf actions

Pilaf support a library of actions that can be used in the YAML stories to
perform various actions, separated into server actions, client actions and
story actions.

- **Server actions**: These actions interact with the Minecraft server through
  RCON commands. Examples include executing server commands, checking server
  logs, and verifying plugin states.
- **Client actions**: These actions simulate player interactions with the
  Minecraft server through the connected clients. Examples include sending chat
  messages, moving the player, and interacting with the game world. Each client
  action is bound to a particular player that is connected to the server,
  e.g. an operator player or a regular player.
- **Story actions**: These actions control the flow of the story, such as
  defining steps, story-wide actions like "wait", assertions, and conditions.

A library of these actions are maintained.


## Assertions, checks and state comparisons

Pilaf provides a rich set of assertions and checks that can be used to validate
the expected outcomes of the actions performed in the story. These assertions
can be used to verify server states, client states, and other conditions.

Examples include checking for specific chat messages, verifying player
positions, and ensuring that certain server events have occurred.

Actions can also capture the state of the server or client at a particular point
in the story, and compare it with an expected state defined in the story. This
allows for more complex validations and ensures that the plugin behaves as
expected throughout the test.

For example, a story can capture the inventory state of a player before
performing certain actions, assign it to a variable, and later compare the
player's inventory state after the actions to ensure that the expected items
were added or removed, and then assert the comparison result.



## Run configuration

Pilaf uses a configuration file (pilaf.yml) to define the test environment,
including server settings, client settings, and other parameters. This allows
for easy customization and reuse of test environments across different stories.




## Comparing with other test frameworks

HeadlessMC has an automatic test framework called
[mc-runtime-test](https://github.com/headlesshq/mc-runtime-test), which is
designed for testing Minecraft client "mods" against a Minecraft server during
runtime behavior. While mc-runtime-test supports launching a Minecraft server
and connecting a Minecraft client to it, it does not support loading server-side
plugins, and also does not interact with the Minecraft server (through the RCON
server console) or support more than one Minecraft client.

Pilaf differs from mc-runtime-test as Pilaf tests supports testing both
server-side plugins and client-side mods. It has the ability to utilize one
story to interleave multiple Minecraft clients and a Minecraft server, allowing
for more complex integration tests that involve multiple players and server-side
logic.

