#!/bin/bash

# PILAF Demo Runner Script
# =========================
# This script provides a convenient way to run the PILAF demonstrator.
# It manages Docker services and runs the test against a real Minecraft server.

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}PILAF Demo Runner${NC}"
echo "======================"
echo ""

# Function to check if Docker is running
check_docker() {
    echo -e "${YELLOW}Checking Docker status...${NC}"
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running. Please start Docker first.${NC}"
        exit 1
    fi
    echo -e "${GREEN}Docker is running.${NC}"
}

# Function to check/start PaperMC server
check_papermc() {
    echo -e "${YELLOW}Checking PaperMC server...${NC}"
    if docker ps | grep -q "pilaf-papermc"; then
        echo -e "${GREEN}PaperMC server is running!${NC}"
        PAPERM_RUNNING=true
        return 0
    fi

    echo -e "${YELLOW}PaperMC server not running. Starting...${NC}"
    cd docker
    docker-compose -f docker-compose.pilaf.yml up -d papermc
    cd ..

    echo -e "${YELLOW}Waiting for server to be ready...${NC}"
    for i in {1..90}; do
        if docker exec pilaf-papermc rcon-cli list >/dev/null 2>&1; then
            echo -e "${GREEN}Server is ready!${NC}"
            PAPERM_RUNNING=true
            return 0
        fi
        echo -n "."
        sleep 3
    done
    echo ""
    echo -e "${RED}Server did not become ready in time.${NC}"
    PAPERM_RUNNING=false
    return 1
}

# Function to check/start Mineflayer bridge
check_mineflayer() {
    echo -e "${YELLOW}Checking Mineflayer bridge...${NC}"
    if docker ps | grep -q "pilaf-mineflayer-bridge"; then
        echo -e "${GREEN}Mineflayer bridge is running!${NC}"
        return 0
    fi

    echo -e "${YELLOW}Starting Mineflayer bridge...${NC}"
    cd docker
    docker-compose -f docker-compose.pilaf.yml up -d mineflayer-bridge
    cd ..

    echo -e "${YELLOW}Waiting for Mineflayer bridge to be ready...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:3000/health | grep -q "ok"; then
            echo -e "${GREEN}Mineflayer bridge is ready!${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
    done
    echo ""
    echo -e "${RED}Mineflayer bridge failed to start. Check logs with: docker logs pilaf-mineflayer-bridge${NC}"
    return 1
}

# Function to run the demo
run_demo() {
    echo ""
    echo -e "${YELLOW}Running PILAF demonstrator...${NC}"

    # Start services separately to avoid health check issues
    cd docker
    docker-compose -f docker-compose.pilaf.yml up -d papermc
    cd ..

    # Wait for PaperMC to be ready
    echo -e "${YELLOW}Waiting for PaperMC server...${NC}"
    for i in {1..60}; do
        if docker exec pilaf-papermc rcon-cli list >/dev/null 2>&1; then
            echo -e "${GREEN}PaperMC is ready!${NC}"
            break
        fi
        echo -n "."
        sleep 3
    done

    # Start Mineflayer bridge
    cd docker
    docker-compose -f docker-compose.pilaf.yml up -d mineflayer-bridge
    cd ..

    # Wait for Mineflayer bridge
    echo -e "${YELLOW}Waiting for Mineflayer bridge...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:3000/health | grep -q "ok"; then
            echo -e "${GREEN}Mineflayer bridge is ready!${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done

    echo ""
    echo -e "${YELLOW}Running PILAF test...${NC}"
    gradle run --args="--config config-demo.yaml demo-story.yaml"
    echo -e "${GREEN}Demo completed!${NC}"
}

# Function to show results
show_results() {
    echo ""
    echo -e "${BLUE}Test Results:${NC}"
    echo "============="

    # Find the latest report
    local report_dir="target/pilaf-reports"
    if [ -d "$report_dir" ]; then
        local latest_report=$(ls -t "$report_dir"/*.html 2>/dev/null | head -1)
        if [ -n "$latest_report" ]; then
            echo -e "${GREEN}HTML Report: $latest_report${NC}"
        fi

        local log_file=$(ls -t "$report_dir"/*.log 2>/dev/null | head -1)
        if [ -n "$log_file" ]; then
            echo -e "${GREEN}Log File: $log_file${NC}"
        fi
    else
        echo -e "${RED}Report directory not found.${NC}"
    fi

    echo ""
    echo -e "${BLUE}Server Logs (last 30 lines):${NC}"
    echo "================================"
    docker logs pilaf-papermc 2>&1 | tail -30
}

# Function to display usage
usage() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  start     Start Docker services (PaperMC + Mineflayer bridge)"
    echo "  run       Run the demo test (includes starting services)"
    echo "  status    Check service status"
    echo "  logs      Show server logs"
    echo "  stop      Stop Mineflayer bridge"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start  # Start services only"
    echo "  $0 run    # Start services and run demo"
}

# Main script logic
case "${1:-run}" in
    start)
        check_docker
        check_papermc
        check_mineflayer
        ;;
    run)
        check_docker
        check_papermc
        check_mineflayer
        run_demo
        show_results
        ;;
    status)
        check_docker
        check_papermc
        echo ""
        echo -e "${YELLOW}Mineflayer Bridge:${NC}"
        if docker ps | grep -q "pilaf-mineflayer-bridge"; then
            echo -e "${GREEN}Running${NC}"
            curl -s http://localhost:3000/health
        else
            echo -e "${RED}Not running${NC}"
        fi
        ;;
    logs)
        echo -e "${BLUE}PaperMC Server Logs:${NC}"
        docker logs pilaf-papermc 2>&1 | tail -50
        echo ""
        echo -e "${BLUE}Mineflayer Bridge Logs:${NC}"
        docker logs pilaf-mineflayer-bridge 2>&1 | tail -20
        ;;
    stop)
        echo -e "${YELLOW}Stopping PILAF services...${NC}"
        cd docker
        docker-compose -f docker-compose.pilaf.yml down
        cd ..
        echo -e "${GREEN}Done.${NC}"
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        usage
        exit 1
        ;;
esac
