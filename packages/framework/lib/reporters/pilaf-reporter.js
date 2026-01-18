// packages/framework/lib/reporters/pilaf-reporter.js
const { ReportGenerator } = require('@pilaf/reporting');

class PilafReporter {
  constructor(globalConfig, options = {}) {
    this._globalConfig = globalConfig;
    this._options = options;
    this._results = {
      suiteName: options.suiteName || 'Pilaf Tests',
      durationMs: 0,
      passed: true,
      stories: []
    };
    this._currentStory = null;
    this._storyStartTime = 0;
  }

  onRunStart(aggregatedResults, options) {
    this._runStartTime = Date.now();
  }

  onTestSuiteStart(testSuite) {
    // testSuite is a describe block
    this._currentStory = {
      name: testSuite.testPath ? testSuite.testPath.split('/').pop() : 'Unknown Story',
      file: testSuite.testPath || '',
      passedCount: 0,
      failedCount: 0,
      steps: []
    };
    this._storyStartTime = Date.now();
  }

  onTestResult(test, testResult) {
    const step = {
      name: testResult.fullName,
      passed: testResult.status === 'passed',
      durationMs: testResult.duration,
      executionContext: {
        executor: 'ASSERT'
      }
    };

    if (testResult.status === 'passed') {
      this._currentStory.passedCount++;
    } else {
      this._currentStory.failedCount++;
      this._results.passed = false;
    }

    this._currentStory.steps.push(step);
  }

  onTestSuiteResult(testSuite, testSuiteResult) {
    if (this._currentStory) {
      this._results.stories.push(this._currentStory);
      this._currentStory = null;
    }
  }

  onRunComplete(contexts, aggregatedResults) {
    this._results.durationMs = Date.now() - this._runStartTime;

    if (this._options.outputPath) {
      const generator = new ReportGenerator();
      generator.generate(this._results, this._options.outputPath);
      console.log(`\n[Pilaf] Report generated: ${this._options.outputPath}`);
    }
  }
}

module.exports = { PilafReporter };
