#!/bin/bash
# Pilaf Test Runner
# Starts a local Minecraft server and runs Pilaf tests with HTML report generation

set -e

echo "🎮 Pilaf Test Runner"
echo "=================="

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    echo "📋 Loading custom configuration from .env"
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
fi

# Set defaults if not already set
export RCON_HOST="${RCON_HOST:-localhost}"
export RCON_PORT="${RCON_PORT:-25576}"
export RCON_PASSWORD="${RCON_PASSWORD:-cavarest}"
export MC_HOST="${MC_HOST:-localhost}"
export MC_PORT="${MC_PORT:-25566}"

echo "📋 Configuration:"
echo "   Minecraft: $MC_HOST:$MC_PORT"
echo "   RCON:      $RCON_HOST:$RCON_PORT"
echo ""

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "❌ Docker is not running!"
    echo "Please start Docker Desktop and try again."
    exit 1
fi

# Check if server is already running
if docker ps | grep -q "pilaf-minecraft-dev"; then
    echo "✅ Minecraft server already running"
else
    echo "🚀 Starting Minecraft server..."
    docker-compose -f docker-compose.dev.yml up -d

    echo "⏳ Waiting for server to initialize (this takes 60-90 seconds)..."
    echo "You can watch logs with: docker-compose -f docker-compose.dev.yml logs -f"
    echo ""

    # Wait for RCON port to be accessible (using timeout with TCP connection)
    # We'll wait up to 90 seconds for the server to be ready
    echo "Waiting for RCON port $RCON_PORT to be accessible..."
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
        echo "❌ Server failed to start. Check logs: docker-compose -f docker-compose.dev.yml logs"
        docker-compose -f docker-compose.dev.yml down
        exit 1
    fi
fi

echo ""
echo "🧪 Running tests and generating HTML report..."
echo "=================="

# Always generate report
pnpm test:report

# Save exit code
TEST_EXIT_CODE=$?

echo ""
echo "=================="

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo "✅ All tests passed!"
    echo ""
    echo "📊 HTML Report: target/pilaf-reports/index.html"
    echo ""
    # Try to open the report automatically
    if command -v open &> /dev/null; then
        open target/pilaf-reports/index.html 2>/dev/null || true
    elif command -v xdg-open &> /dev/null; then
        xdg-open target/pilaf-reports/index.html 2>/dev/null || true
    fi
else
    echo "❌ Some tests failed."
    echo ""
    echo "📊 Check the report for details: target/pilaf-reports/index.html"
fi

echo ""
echo "💡 Server is still running. Stop it with:"
echo "   docker-compose -f docker-compose.dev.yml down"
echo ""
echo "💡 Or connect to it with a Minecraft client:"
echo "   Address: $MC_HOST"
echo "   Port:   $MC_PORT"

exit $TEST_EXIT_CODE
