# @pilaf/reporting

HTML report generation for Pilaf test results.

Generates interactive, browser-based reports from Pilaf test execution data.

## Installation

```bash
pnpm add @pilaf/reporting
```

## Usage

```javascript
const { ReportGenerator } = require('@pilaf/reporting');

const generator = new ReportGenerator({
  templatePath: './path/to/template.html',
  cssPath: './path/to/styles.css'
});

const reportData = {
  suiteName: 'My Plugin Tests',
  stories: [
    {
      name: 'Test Story',
      status: 'passed',
      startTime: Date.now(),
      endTime: Date.now(),
      steps: [
        { name: 'Step 1', action: 'execute_command', response: '...' }
      ],
      consoleLogs: []
    }
  ],
  startTime: Date.now(),
  endTime: Date.now()
};

// Generate HTML report
const outputPath = generator.generate(reportData, './target/report.html');
console.log(`Report saved to: ${outputPath}`);
```

## Report Data Structure

```javascript
{
  suiteName: string,           // Test suite name
  stories: [
    {
      name: string,             // Story name
      status: 'passed' | 'failed',
      file: string,             // Source file path
      startTime: number,        // Timestamp
      endTime: number,
      steps: [
        {
          name: string,         // Step name
          action: string,       // Action type
          response: any,        // Action response
          timestamp: number
        }
      ],
      consoleLogs: [
        {
          timestamp: number,
          message: string
        }
      ]
    }
  ],
  startTime: number,
  endTime: number
}
```

## Custom Templates

You can provide custom HTML templates:

```javascript
const generator = new ReportGenerator({
  templatePath: './my-template.html',
  cssPath: './my-styles.css'
});
```

The template should be a valid HTML file with a Vue.js app that receives the report data as a JavaScript object.

## License

MIT
