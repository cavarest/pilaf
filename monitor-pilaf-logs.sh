#!/bin/bash
#
# PILAF Server Log Monitor
# Shows PaperMC and Mineflayer bridge logs in real-time.
#
# Usage: ./monitor-pilaf-logs.sh [options]
#   -p, --paper     Show only PaperMC logs
#   -m, --mineflayer Show only Mineflayer bridge logs
#   -t, --tail      Number of lines to show (default: 50)
#   -h, --help      Show this help message
#

# Default values
TAIL_LINES=50
SHOW_PAPER=true
SHOW_MINEFLAYER=true

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--paper)
            SHOW_MINEFLAYER=false
            shift
            ;;
        -m|--mineflayer)
            SHOW_PAPER=false
            shift
            ;;
        -t|--tail)
            TAIL_LINES="$2"
            shift 2
            ;;
        -h|--help)
            grep -A 30 '# Usage:' "$0" | head -n 32
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo_step() {
    echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

# Check if containers are running
check_container() {
    local name=$1
    if ! docker ps --format '{{.Names}}' | grep -q "^${name}$"; then
        echo -e "${RED}❌ Container '$name' is not running${NC}"
        echo -e "${YELLOW}💡 Start it with: cd docker && docker-compose up -d${NC}"
        return 1
    fi
    return 0
}

# Show PaperMC logs
show_paper_logs() {
    if ! check_container "pilaf-papermc"; then
        return 1
    fi
    echo -e "${BLUE}📋 PaperMC Server Logs (last $TAIL_LINES lines, following...)${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop${NC}\n"
    docker logs --tail "$TAIL_LINES" -f pilaf-papermc 2>&1 | grep -v "^$"
}

# Show Mineflayer logs
show_mineflayer_logs() {
    if ! check_container "pilaf-mineflayer-bridge"; then
        return 1
    fi
    echo -e "${GREEN}🌐 Mineflayer Bridge Logs (last $TAIL_LINES lines, following...)${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop${NC}\n"
    docker logs --tail "$TAIL_LINES" -f pilaf-mineflayer-bridge 2>&1 | grep -v "^$"
}

# Main
if [ "$SHOW_PAPER" = true ] && [ "$SHOW_MINEFLAYER" = true ]; then
    echo_step "PILAF Log Monitor"
    echo -e "${BLUE}📋 PaperMC${NC} | ${GREEN}🌐 Mineflayer${NC}"
    echo ""
    
    # Use docker-compose logs for combined view
    cd docker
    docker-compose -f docker-compose.pilaf.yml logs --tail="$TAIL_LINES" -f
elif [ "$SHOW_PAPER" = true ]; then
    show_paper_logs
else
    show_mineflayer_logs
fi
