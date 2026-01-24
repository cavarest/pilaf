#!/usr/bin/env node

/**
 * Fix prismarine-physics dependency issue
 *
 * The prismarine-physics package imports './lib/features' but the file
 * is actually './lib/features.json'. This script creates a symlink to fix the issue.
 *
 * This script supports both npm and pnpm package managers.
 */

const fs = require('fs');
const path = require('path');

function fixPrismarinePhysics() {
  let fixedCount = 0;

  // Handle pnpm-style node_modules (.pnpm directory)
  const pnpmPath = path.join(__dirname, '..', '..', '.pnpm');

  if (fs.existsSync(pnpmPath)) {
    const findAndFix = (dir) => {
      try {
        const entries = fs.readdirSync(dir, { withFileTypes: true });

        for (const entry of entries) {
          if (!entry.isDirectory()) continue;

          const fullPath = path.join(dir, entry.name);

          if (entry.name.startsWith('prismarine-physics@')) {
            const libPath = path.join(fullPath, 'node_modules', 'prismarine-physics', 'lib');

            if (fs.existsSync(libPath)) {
              const featuresJson = path.join(libPath, 'features.json');
              const featuresLink = path.join(libPath, 'features');

              if (fs.existsSync(featuresJson) && !fs.existsSync(featuresLink)) {
                try {
                  fs.symlinkSync('features.json', featuresLink);
                  console.log(`✓ Fixed prismarine-physics: ${featuresLink}`);
                  fixedCount++;
                } catch (err) {
                  // On Windows, try junction instead of symlink
                  if (process.platform === 'win32') {
                    try {
                      fs.symlinkSync(path.join(libPath, 'features.json'), featuresLink, 'junction');
                      console.log(`✓ Fixed prismarine-physics (junction): ${featuresLink}`);
                      fixedCount++;
                    } catch (err2) {
                      console.log(`✗ Failed to fix prismarine-physics: ${featuresLink}`);
                    }
                  } else {
                    console.log(`✗ Failed to fix prismarine-physics: ${featuresLink}`);
                  }
                }
              }
            }
          }
        }
      } catch (err) {
        // Ignore errors from readdirSync
      }
    };

    findAndFix(pnpmPath);
  }

  // Handle npm-style node_modules (direct path)
  const npmLibPath = path.join(__dirname, '..', '..', 'node_modules', 'prismarine-physics', 'lib');
  if (fs.existsSync(npmLibPath)) {
    const featuresJson = path.join(npmLibPath, 'features.json');
    const featuresLink = path.join(npmLibPath, 'features');

    if (fs.existsSync(featuresJson) && !fs.existsSync(featuresLink)) {
      try {
        fs.symlinkSync('features.json', featuresLink);
        console.log(`✓ Fixed prismarine-physics: ${featuresLink}`);
        fixedCount++;
      } catch (err) {
        if (process.platform === 'win32') {
          try {
            fs.symlinkSync(path.join(npmLibPath, 'features.json'), featuresLink, 'junction');
            console.log(`✓ Fixed prismarine-physics (junction): ${featuresLink}`);
            fixedCount++;
          } catch (err2) {
            console.log(`✗ Failed to fix prismarine-physics: ${featuresLink}`);
          }
        } else {
          console.log(`✗ Failed to fix prismarine-physics: ${featuresLink}`);
        }
      }
    }
  }

  if (fixedCount > 0) {
    console.log(`\nFixed ${fixedCount} prismarine-physics package(s)`);
  } else {
    // Silent if already fixed or not found (common during development)
  }
}

fixPrismarinePhysics();
