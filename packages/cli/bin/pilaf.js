#!/usr/bin/env node
const { program } = require('commander');
const { loadConfig } = require('../lib/config-loader');

program
  .name('pilaf')
  .description('Pure JS testing framework for Minecraft PaperMC plugins')
  .version('1.0.0');

program
  .command('test [files...]')
  .description('Run Pilaf tests')
  .option('-w, --watch', 'Watch mode')
  .option('--config <path>', 'Config file path')
  .option('--output <path>', 'Report output path')
  .action(async (files, options) => {
    const config = loadConfig(options.config);

    // For now, just echo what would be run
    console.log('[Pilaf] Would run tests:', files || config.testMatch);
    console.log('[Pilaf] Report would be saved to:', options.output || config.reportDir);
    console.log('[Pilaf] Full implementation in next tasks');
  });

program
  .command('health-check')
  .description('Check backend connectivity')
  .action(async () => {
    const { PilafBackendFactory } = require('@pilaf/backends');
    const config = loadConfig();

    console.log('[Pilaf] Checking RCON connection...');
    try {
      const rcon = await PilafBackendFactory.create('rcon', config.backend.rcon);
      const result = await rcon.sendCommand('list');
      console.log('[Pilaf] ✓ RCON connected:', result.raw.substring(0, 100));
      await rcon.disconnect();
    } catch (err) {
      console.error('[Pilaf] ✗ RCON connection failed:', err.message);
    }
  });

program.parse();
