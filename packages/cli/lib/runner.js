// packages/cli/lib/runner.js
const { createConfig } = require('@jest/core');
const { PilafReporter } = require('@pilaf/framework');

async function runTests(files, options) {
  const config = createConfig(
    {
      testMatch: options.testMatch || ['**/*.pilaf.test.js'],
      testPathIgnorePatterns: options.testIgnore || ['node_modules'],
      reporters: [
        'default',
        ['@pilaf/framework', {
          suiteName: options.suiteName || 'Pilaf Tests',
          outputPath: options.outputPath || 'target/pilaf-reports/report.html'
        }]
      ],
      testTimeout: options.timeout || 30000
    },
    null
  );

  if (files && files.length > 0) {
    config.set('testMatch', files);
  }

  const { runCLI } = await import('jest');
  const result = await runCLI(config, [process.cwd(), ...config.args]);

  return result;
}

module.exports = { runTests };
