// packages/framework/lib/reporters/pilaf-reporter.js
const { ReportGenerator } = require('@pilaf/reporting');
const path = require('path');

class PilafReporter {
  constructor(globalConfig, options = {}) {
    this._globalConfig = globalConfig;
    this._options = options;
    this._results = {
      suiteName: options.suiteName || 'Pilaf Tests',
      durationMs: 0,
      passed: true,
      stories: [],
      consoleLogs: []
    };
    this._runStartTime = 0;
    // Track console logs per test file to avoid mixing
    this._logsPerTest = new Map();
  }

  onRunStart(aggregatedResults, options) {
    this._runStartTime = Date.now();
    this._logsPerTest.clear();
  }

  onTestResult(test, testResult, aggregatedResult) {
    const testPath = test.path;

    // Initialize logs array for this test if not exists
    if (!this._logsPerTest.has(testPath)) {
      this._logsPerTest.set(testPath, []);
    }

    // Capture console logs from this test result
    if (testResult.console && testResult.console.length > 0) {
      for (const logEntry of testResult.console) {
        // Jest console entries have: { message: string, origin: string, type: string }
        const message = logEntry.message || logEntry;
        this._logsPerTest.get(testPath).push({
          timestamp: Date.now(),
          message: typeof message === 'string' ? message : JSON.stringify(message)
        });
      }
    }

    // Stories are parsed from console logs in onRunComplete
  }

  onRunComplete(contexts, aggregatedResults) {
    this._results.durationMs = Date.now() - this._runStartTime;

    // Parse stories from each test file's logs separately
    const allStories = [];
    const allConsoleLogs = [];

    for (const [testPath, logs] of this._logsPerTest.entries()) {
      // Parse stories from this test file's logs, including the logs for display
      const stories = this._parseStoriesFromLogs(logs, testPath);
      allStories.push(...stories);

      // Also collect all console logs for the full log view
      allConsoleLogs.push(...logs);
    }

    this._results.stories = allStories;
    this._results.consoleLogs = allConsoleLogs;

    if (this._options.outputPath) {
      const generator = new ReportGenerator();
      generator.generate(this._results, this._options.outputPath);
      console.log(`\n[Pilaf] Report generated: ${this._options.outputPath}`);
    }
  }

  /**
   * Parse console logs to build story structure with steps and details
   * @param {Array} logs - Console logs for a single test file
   * @param {string} testPath - Path to the test file
   * @returns {Array} Array of story objects
   */
  _parseStoriesFromLogs(logs, testPath) {
    const stories = [];
    const storyPattern = /\[StoryRunner\] Starting story:\s*(.+)/;
    const stepPattern = /\[StoryRunner\] Step (\d+)\/(\d+):\s*(.+)/;
    const storyCompletePattern = /\[StoryRunner\] Story (.+) (PASSED|FAILED)/;

    let currentStory = null;
    let currentStep = null;
    let storyLogs = []; // Track logs for the current story

    for (const log of logs) {
      // All logs go to the current story's console logs
      if (currentStory) {
        storyLogs.push(log);
      }

      // Check for story start
      const storyMatch = log.message.match(storyPattern);
      if (storyMatch) {
        currentStory = {
          name: storyMatch[1].trim(),
          file: testPath,
          passedCount: 0,
          failedCount: 0,
          steps: [],
          consoleLogs: [] // Will be populated with logs for this story
        };
        stories.push(currentStory);
        currentStep = null;
        storyLogs = []; // Start collecting logs for this story
        continue;
      }

      // Check for story completion
      const completeMatch = log.message.match(storyCompletePattern);
      if (completeMatch) {
        if (completeMatch[2] === 'PASSED') {
          currentStory.passedCount = currentStory.steps.length;
          currentStory.failedCount = 0;
        } else {
          currentStory.passedCount = 0;
          currentStory.failedCount = currentStory.steps.length;
        }
        // Store the collected logs with this story
        currentStory.consoleLogs = [...storyLogs];
        this._results.passed = this._results.passed && currentStory.passedCount > 0;
        currentStory = null;
        currentStep = null;
        storyLogs = [];
        continue;
      }

      // Only process steps if we have a current story
      if (!currentStory) continue;

      // Check for step start
      const stepMatch = log.message.match(stepPattern);
      if (stepMatch) {
        const stepNum = parseInt(stepMatch[1], 10);
        const stepName = stepMatch[3].trim();

        // Determine executor from step name
        let executor = 'ASSERT';
        if (stepName.includes('[RCON]')) {
          executor = 'RCON';
        } else if (stepName.includes('[player:')) {
          const playerMatch = stepName.match(/\[player:\s*(\w+)\]/);
          if (playerMatch) {
            executor = `player: ${playerMatch[1]}`;
          }
        }

        // Only add step once (in case of duplicate logs)
        if (!currentStory.steps.find(s => s.name === stepName)) {
          currentStep = {
            name: stepName,
            passed: true,
            durationMs: 0,
            executionContext: { executor },
            details: []
          };
          currentStory.steps.push(currentStep);
        } else {
          // If step already exists, get reference to it
          currentStep = currentStory.steps.find(s => s.name === stepName);
        }
        continue;
      }

      // If we have a current step, add non-step, non-story logs as details
      if (currentStep && !stepMatch && !storyMatch && !completeMatch) {
        // Filter out logs that are part of step/story management
        if (!log.message.includes('[StoryRunner] Step') &&
            !log.message.includes('[StoryRunner] Starting story') &&
            !log.message.includes('[StoryRunner] Story') &&
            log.message.includes('[StoryRunner]')) {
          const detailMessage = log.message.replace('[StoryRunner] ', '').trim();

          // Check if this is an ACTION or RESPONSE line
          if (detailMessage.startsWith('ACTION:')) {
            currentStep.details.push({
              timestamp: log.timestamp,
              type: 'action',
              message: detailMessage.replace('ACTION:', '').trim()
            });
          } else if (detailMessage.startsWith('RESPONSE:')) {
            currentStep.details.push({
              timestamp: log.timestamp,
              type: 'response',
              message: detailMessage.replace('RESPONSE:', '').trim()
            });
          } else {
            // Other detail types (stored results, assertions, etc.)
            currentStep.details.push({
              timestamp: log.timestamp,
              type: 'other',
              message: detailMessage
            });
          }
        }
      }
    }

    return stories;
  }
}

module.exports = PilafReporter;
