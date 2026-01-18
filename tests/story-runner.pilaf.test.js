// Story-based test runner for Pilaf
// Uses StoryRunner to execute YAML/JS-based test stories
const { StoryRunner } = require('@pilaf/framework');
const path = require('path');
const fs = require('fs');

describe('Story-Based Tests', () => {
  const storiesDir = path.join(__dirname, 'stories');
  const allFiles = fs.readdirSync(storiesDir)
    .filter(f => f.endsWith('.story.yaml') || f.endsWith('.story.js'));

  allFiles.forEach(storyFile => {
    const storyPath = path.join(storiesDir, storyFile);
    const storyName = path.basename(storyFile).replace(/\.story\.(yaml|js)$/, '');

    describe(storyName, () => {
      let runner;

      beforeEach(() => {
        runner = new StoryRunner({
          logger: console
        });
      });

      it(`should execute story: ${storyName}`, async () => {
        // Load story - for .js files, require() the module; for .yaml files, use loadStory()
        let story;
        if (storyFile.endsWith('.js')) {
          story = require(storyPath);
        } else {
          story = runner.loadStory(storyPath);
        }

        const result = await runner.execute(story);

        if (!result.success) {
          console.error(`Story failed: ${result.error}`);
          console.error('Failed steps:', result.steps.filter(s => !s.success));
        }

        expect(result.success).toBe(true);
      }, 120000); // 2 minute timeout for story execution
    });
  });
});
