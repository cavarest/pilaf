/**
 * PILAF Mineflayer Bridge Server
 * HTTP server that bridges Java tests to Minecraft via Mineflayer
 */

const express = require('express');
const mineflayer = require('mineflayer');

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 3000;
const MC_HOST = process.env.MC_HOST || 'localhost';
const MC_PORT = parseInt(process.env.MC_PORT || '25565');

// Store active bots by player name
const bots = new Map();
// Store recent chat messages per bot (circular buffer)
const chatHistory = new Map();
const MAX_CHAT_HISTORY = 50;

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', bots: Array.from(bots.keys()) });
});

// Connect a bot to the server
app.post('/connect', async (req, res) => {
    const { username, host = MC_HOST, port = MC_PORT } = req.body;

    if (!username) {
        return res.status(400).json({ error: 'Username is required' });
    }

    if (bots.has(username)) {
        return res.json({ status: 'already_connected', username });
    }

    try {
        const bot = mineflayer.createBot({
            host,
            port,
            username,
            auth: 'offline'
        });

        await new Promise((resolve, reject) => {
            bot.once('spawn', resolve);
            bot.once('error', reject);
            bot.once('kicked', (reason) => reject(new Error(`Kicked: ${reason}`)));
            setTimeout(() => reject(new Error('Connection timeout')), 30000);
        });

        // Initialize chat history for this bot
        chatHistory.set(username, []);

        // Listen for chat messages
        bot.on('message', (jsonMsg, position) => {
            const history = chatHistory.get(username) || [];
            const msg = {
                timestamp: Date.now(),
                text: jsonMsg.toString(),
                json: jsonMsg.json || jsonMsg,
                position
            };
            history.push(msg);
            // Keep only last MAX_CHAT_HISTORY messages
            if (history.length > MAX_CHAT_HISTORY) {
                history.shift();
            }
            console.log(`📨 [${username}] Chat: ${msg.text}`);
        });

        bots.set(username, bot);
        console.log(`✅ Bot ${username} connected to ${host}:${port}`);
        res.json({ status: 'connected', username, host, port });
    } catch (error) {
        console.error(`❌ Failed to connect ${username}:`, error.message);
        res.status(500).json({ error: error.message });
    }
});

// Disconnect a bot
app.post('/disconnect', (req, res) => {
    const { username } = req.body;
    const bot = bots.get(username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    bot.quit();
    bots.delete(username);
    console.log(`👋 Bot ${username} disconnected`);
    res.json({ status: 'disconnected', username });
});

// Execute chat command as player and capture response
app.post('/command', async (req, res) => {
    const { username, command, waitForChat = true, chatTimeout = 2000 } = req.body;
    const bot = bots.get(username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    const fullCommand = command.startsWith('/') ? command : `/${command}`;

    // Record current chat history size
    const history = chatHistory.get(username) || [];
    const beforeCount = history.length;

    bot.chat(fullCommand);
    console.log(`💬 ${username} executed: ${fullCommand}`);

    if (waitForChat) {
        // Wait for chat response
        await new Promise(resolve => setTimeout(resolve, chatTimeout));

        // Get new messages since command was sent
        const newMessages = history.slice(beforeCount).map(m => m.text);
        res.json({
            status: 'executed',
            username,
            command: fullCommand,
            chatMessages: newMessages
        });
    } else {
        res.json({ status: 'executed', username, command: fullCommand });
    }
});

// Get recent chat messages for a bot
app.get('/chat/:username', (req, res) => {
    const username = req.params.username;
    const limit = parseInt(req.query.limit || '10');
    const since = parseInt(req.query.since || '0');

    if (!bots.has(username)) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    const history = chatHistory.get(username) || [];
    const filtered = since > 0
        ? history.filter(m => m.timestamp > since)
        : history.slice(-limit);

    res.json({
        username,
        messages: filtered.map(m => ({
            timestamp: m.timestamp,
            text: m.text
        }))
    });
});

// Clear chat history for a bot
app.delete('/chat/:username', (req, res) => {
    const username = req.params.username;

    if (!bots.has(username)) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    chatHistory.set(username, []);
    res.json({ status: 'cleared', username });
});

// Send chat message
app.post('/chat', (req, res) => {
    const { username, message } = req.body;
    const bot = bots.get(username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    bot.chat(message);
    res.json({ status: 'sent', username, message });
});

// Move player to location
app.post('/move', async (req, res) => {
    const { username, x, y, z } = req.body;
    const bot = bots.get(username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    try {
        // Use chat to teleport (requires op)
        bot.chat(`/tp ${username} ${x} ${y} ${z}`);
        res.json({ status: 'moved', username, location: { x, y, z } });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Get player position
app.get('/position/:username', (req, res) => {
    const bot = bots.get(req.params.username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${req.params.username} not found` });
    }

    const pos = bot.entity.position;
    res.json({ x: pos.x, y: pos.y, z: pos.z });
});

// Get player health
app.get('/health/:username', (req, res) => {
    const bot = bots.get(req.params.username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${req.params.username} not found` });
    }

    res.json({ health: bot.health, food: bot.food });
});

// Get player inventory
app.get('/inventory/:username', (req, res) => {
    const bot = bots.get(req.params.username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${req.params.username} not found` });
    }

    const items = bot.inventory.items().map(item => ({
        name: item.name,
        count: item.count,
        slot: item.slot
    }));
    res.json({ items });
});

// Equip item to slot
app.post('/equip', async (req, res) => {
    const { username, item, slot } = req.body;
    const bot = bots.get(username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    try {
        const itemToEquip = bot.inventory.items().find(i => i.name.includes(item));
        if (itemToEquip) {
            await bot.equip(itemToEquip, slot || 'hand');
            res.json({ status: 'equipped', username, item, slot });
        } else {
            res.status(404).json({ error: `Item ${item} not found in inventory` });
        }
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Use held item on entity
app.post('/use', (req, res) => {
    const { username, target } = req.body;
    const bot = bots.get(username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${username} not found` });
    }

    // Find entity by name
    const entity = Object.values(bot.entities).find(e =>
        e.name?.includes(target) || e.username?.includes(target)
    );

    if (entity) {
        bot.useOn(entity);
        res.json({ status: 'used', username, target });
    } else {
        res.status(404).json({ error: `Entity ${target} not found` });
    }
});

// Get nearby entities
app.get('/entities/:username', (req, res) => {
    const bot = bots.get(req.params.username);

    if (!bot) {
        return res.status(404).json({ error: `Bot ${req.params.username} not found` });
    }

    const entities = Object.values(bot.entities)
        .filter(e => e.type !== 'player' || e.username !== req.params.username)
        .map(e => ({
            id: e.id,
            type: e.type,
            name: e.name || e.username,
            position: e.position,
            health: e.health
        }));
    res.json({ entities });
});

// Disconnect all bots on shutdown
process.on('SIGINT', () => {
    console.log('\n🛑 Shutting down...');
    for (const [username, bot] of bots) {
        bot.quit();
        console.log(`👋 Disconnected ${username}`);
    }
    process.exit(0);
});

app.listen(PORT, () => {
    console.log(`🚀 PILAF Mineflayer Bridge running on port ${PORT}`);
    console.log(`   MC Server: ${MC_HOST}:${MC_PORT}`);
});
