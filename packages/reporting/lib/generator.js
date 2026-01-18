// packages/reporting/lib/generator.js
const fs = require('fs');
const path = require('path');

class ReportGenerator {
  constructor(options = {}) {
    this.templatePath = options.templatePath ||
      path.join(__dirname, '../templates/report.html');
    this.cssPath = options.cssPath ||
      path.join(__dirname, '../templates/styles.css');
  }

  generate(testResults, outputPath) {
    const template = fs.readFileSync(this.templatePath, 'utf8');
    const cssContent = fs.readFileSync(this.cssPath, 'utf8');

    const html = template
      .replace('__SUITENAME__', this._escapeHtml(testResults.suiteName || 'Pilaf Tests'))
      .replace('__STORIESJSON__', this._escapeJson(JSON.stringify(testResults)))
      .replace('__CSSCONTENT__', cssContent);

    // Ensure output directory exists
    const dir = path.dirname(outputPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    fs.writeFileSync(outputPath, html);
    return outputPath;
  }

  _escapeHtml(text) {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  _escapeJson(json) {
    // Escape </script> to prevent breaking out of script tag
    return json.replace(/<\/script>/g, '<\\/script>');
  }
}

module.exports = { ReportGenerator };
