#!/bin/bash
# Pilaf Integration Test Runner
# Alias for run-tests.sh for convenience

set -e

echo "🎮 Pilaf Integration Test Runner"
echo "================================="
echo ""
echo "This script will:"
echo "  1. Start a local Minecraft server (if not running)"
echo "  2. Run integration tests"
echo "  3. Generate HTML report"
echo ""

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "❌ Docker is not running!"
    echo "Please start Docker Desktop and try again."
    exit 1
fi

# Use non-default ports to avoid conflicts
export MC_PORT=25566
export RCON_PORT=25576
export MC_HOST=localhost
export RCON_HOST=localhost
export RCON_PASSWORD=cavarest

# Check if server is already running
if docker ps | grep -q "pilaf-minecraft-dev"; then
    echo "✅ Minecraft server already running (docker-compose)"
    echo ""
    exec ./run-tests.sh
elif docker ps | grep -q "mc-server"; then
    echo "✅ Minecraft server already running (standalone)"
    echo ""
else
    echo "🚀 Starting Minecraft server container..."
    echo "   Using port ${MC_PORT} for Minecraft and ${RCON_PORT} for RCON"

    docker run -d \
        -p ${MC_PORT}:25565 \
        -p ${RCON_PORT}:25575 \
        -e EULA='TRUE' \
        -e ONLINE_MODE='false' \
        -e TYPE='PAPER' \
        -e VERSION='1.21.8' \
        -e RCON_PASSWORD='cavarest' \
        -e ENABLE_RCON='true' \
        -e RCON_PORT='25575' \
        -e MAX_PLAYERS='5' \
        -e SPAWN_PROTECTION='0' \
        --name mc-server \
        itzg/minecraft-server

    echo "⏳ Waiting for server to initialize (this may take 60-90 seconds)..."

    # Wait for RCON port to be accessible
    for i in {1..45}; do
        if timeout 1 bash -c "cat < /dev/null > /dev/tcp/$RCON_HOST/$RCON_PORT" 2>/dev/null; then
            echo ""
            echo "✅ Server is ready!"
            break
        fi
        echo -n "."
        sleep 2
    done

    if ! timeout 1 bash -c "cat < /dev/null > /dev/tcp/$RCON_HOST/$RCON_PORT" 2>/dev/null; then
        echo ""
        echo "❌ Server failed to start. Check logs: docker logs mc-server"
        docker stop mc-server && docker rm mc-server
        exit 1
    fi
    echo ""
fi

echo "🧪 Running integration tests..."
echo "==============================="
echo ""

# Run integration tests
pnpm test tests/

TEST_EXIT_CODE=$?

echo ""
echo "==============================="

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo "✅ All integration tests passed!"
    echo ""
else
    echo "❌ Some integration tests failed."
    echo ""
fi

echo ""
echo "💡 Server is still running. Stop it with:"
echo "   docker stop mc-server && docker rm mc-server"
echo ""
echo "💡 Or connect to it with a Minecraft client:"
echo "   Address: $MC_HOST"
echo "   Port:   $MC_PORT"
echo ""

exit $TEST_EXIT_CODE
