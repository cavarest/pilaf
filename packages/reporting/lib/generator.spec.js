const fs = require('fs');
const path = require('path');
const { ReportGenerator } = require('./generator');

describe('ReportGenerator', () => {
  let generator;
  let mockOutputPath;
  let mockTemplatePath;

  beforeEach(() => {
    // Use actual template path for real testing
    generator = new ReportGenerator();
    mockOutputPath = path.join(__dirname, 'test-report.html');
    mockTemplatePath = path.join(__dirname, '../templates/report.html');
  });

  afterEach(() => {
    // Clean up test files
    if (fs.existsSync(mockOutputPath)) {
      fs.unlinkSync(mockOutputPath);
    }
  });

  describe('constructor', () => {
    it('should create instance with default template paths', () => {
      expect(generator.templatePath).toContain('templates/report.html');
      expect(generator.cssPath).toContain('templates/styles.css');
    });

    it('should create instance with custom template paths', () => {
      const customGenerator = new ReportGenerator({
        templatePath: '/custom/template.html',
        cssPath: '/custom/styles.css'
      });
      expect(customGenerator.templatePath).toBe('/custom/template.html');
      expect(customGenerator.cssPath).toBe('/custom/styles.css');
    });
  });

  describe('_escapeHtml', () => {
    it('should escape ampersands', () => {
      expect(generator._escapeHtml('A & B')).toBe('A &amp; B');
    });

    it('should escape less than signs', () => {
      expect(generator._escapeHtml('A < B')).toBe('A &lt; B');
    });

    it('should escape greater than signs', () => {
      expect(generator._escapeHtml('A > B')).toBe('A &gt; B');
    });

    it('should escape double quotes', () => {
      expect(generator._escapeHtml('"hello"')).toBe('&quot;hello&quot;');
    });

    it('should escape single quotes', () => {
      expect(generator._escapeHtml("'hello'")).toBe('&#039;hello&#039;');
    });

    it('should escape multiple special characters in one string', () => {
      expect(generator._escapeHtml('<script>alert("X&Y")</script>'))
        .toBe('&lt;script&gt;alert(&quot;X&amp;Y&quot;)&lt;/script&gt;');
    });

    it('should handle empty string', () => {
      expect(generator._escapeHtml('')).toBe('');
    });

    it('should handle string with no special characters', () => {
      expect(generator._escapeHtml('hello world')).toBe('hello world');
    });
  });

  describe('_escapeJson', () => {
    it('should escape closing script tags', () => {
      const json = '{"data":"test</script>alert(1)"}';
      expect(generator._escapeJson(json)).toBe('{"data":"test<\\/script>alert(1)"}');
    });

    it('should escape multiple closing script tags', () => {
      const json = '{"a":"</script>","b":"</script>"}';
      expect(generator._escapeJson(json)).toBe('{"a":"<\\/script>","b":"<\\/script>"}');
    });

    it('should handle JSON without script tags', () => {
      const json = '{"name":"test","value":123}';
      expect(generator._escapeJson(json)).toBe('{"name":"test","value":123}');
    });

    it('should handle empty JSON object', () => {
      expect(generator._escapeJson('{}')).toBe('{}');
    });

    it('should handle empty string', () => {
      expect(generator._escapeJson('')).toBe('');
    });
  });

  describe('generate', () => {
    it('should generate HTML report with test results', () => {
      const testResults = {
        suiteName: 'Test Suite',
        stories: [
          {
            name: 'Test Story',
            status: 'passed',
            file: 'test.js'
          }
        ],
        startTime: Date.now(),
        endTime: Date.now()
      };

      // Skip this test if template doesn't exist (e.g., in CI before build)
      if (!fs.existsSync(generator.templatePath)) {
        console.warn('Template file not found, skipping test');
        return;
      }

      const resultPath = generator.generate(testResults, mockOutputPath);

      expect(resultPath).toBe(mockOutputPath);
      expect(fs.existsSync(mockOutputPath)).toBe(true);

      const content = fs.readFileSync(mockOutputPath, 'utf8');
      expect(content).toContain('Test Suite');
      expect(content).toContain('Test Story');
    });

    it('should create output directory if it does not exist', () => {
      const testResults = {
        suiteName: 'Test Suite',
        stories: []
      };

      // Skip if template doesn't exist
      if (!fs.existsSync(generator.templatePath)) {
        return;
      }

      const nestedPath = path.join(__dirname, 'nested/dir/report.html');
      const resultPath = generator.generate(testResults, nestedPath);

      expect(fs.existsSync(resultPath)).toBe(true);
      fs.unlinkSync(nestedPath);
      fs.rmdirSync(path.dirname(nestedPath));
      fs.rmdirSync(path.dirname(path.dirname(nestedPath)));
    });

    it('should use default suite name if not provided', () => {
      const testResults = {
        stories: []
      };

      // Skip if template doesn't exist
      if (!fs.existsSync(generator.templatePath)) {
        return;
      }

      const resultPath = generator.generate(testResults, mockOutputPath);
      const content = fs.readFileSync(resultPath, 'utf8');
      expect(content).toContain('Pilaf Tests');
    });
  });
});
