#!/usr/bin/env node

/**
 * Fix prismarine-physics dependency issue
 *
 * The prismarine-physics package imports './lib/features' but the file
 * is actually './lib/features.json'. This script creates a symlink to fix the issue.
 */

const fs = require('fs');
const path = require('path');

function fixPrismarinePhysics() {
  // Find all prismarine-physics instances in node_modules
  const nodeModulesPath = path.join(__dirname, 'node_modules', '.pnpm');

  if (!fs.existsSync(nodeModulesPath)) {
    console.log('No .pnpm directory found, skipping prismarine-physics fix');
    return;
  }

  let fixedCount = 0;

  // Find all prismarine-physics directories
  const findAndFix = (dir) => {
    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);

      if (entry.isDirectory()) {
        // Check if this is a prismarine-physics package
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
    }
  };

  findAndFix(nodeModulesPath);

  if (fixedCount > 0) {
    console.log(`\nFixed ${fixedCount} prismarine-physics package(s)`);
  } else {
    console.log('No prismarine-physics packages found or already fixed');
  }
}

fixPrismarinePhysics();
