/**
 * Pilaf Jest Reporter
 * Custom Jest reporter that generates HTML reports using ReportGenerator
 */

const fs = require('fs');
const path = require('path');
const { ReportGenerator } = require('@pilaf/reporting');

class PilafJestReporter {
  constructor(globalConfig, options = {}) {
    this._globalConfig = globalConfig;
    this._options = options;
    this.testResults = [];
    this.suiteName = options.suiteName || 'Pilaf Integration Tests';
  }

  onTestResult(test, testResult) {
    // Collect test results
    const ancestorTitles = testResult.ancestors.map(a => a.name);
    const fullName = [...ancestorTitles, testResult.name].join(' › ');

    const result = {
      name: fullName,
      status: testResult.status,
      duration: testResult.duration || 0,
      errors: []
    };

    // Collect failure messages
    if (testResult.failureMessages && testResult.failureMessages.length > 0) {
      result.errors = testResult.failureMessages;
    }

    this.testResults.push(result);
  }

  onRunComplete(contexts, results) {
    // Generate summary
    const summary = {
      suiteName: this.suiteName,
      startTime: new Date(results.startTime).toISOString(),
      duration: results.testResults.reduce((sum, suite) => sum + suite.perfStats.duration, 0),
      total: results.numTotalTests,
      passed: results.numPassedTests,
      failed: results.numFailedTests,
      skipped: results.numPendingTests,
      tests: this.testResults
    };

    // Generate HTML report
    const outputDir = this._options.outputDir || 'target/pilaf-reports';
    const outputFile = path.join(outputDir, 'index.html');

    const generator = new ReportGenerator();
    generator.generate(summary, outputFile);

    console.log(`\n[Pilaf Reporter] HTML report generated: ${outputFile}`);

    // Also generate a JSON summary for debugging
    const jsonFile = path.join(outputDir, 'summary.json');
    fs.mkdirSync(outputDir, { recursive: true });
    fs.writeFileSync(jsonFile, JSON.stringify(summary, null, 2));
  }
}

module.exports = PilafJestReporter;
