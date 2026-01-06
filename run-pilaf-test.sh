#!/bin/bash
#
# PILAF Test Automation Script
# This script compiles PILAF, starts Docker services, runs tests, and cleans up.
#
# Usage: ./run-pilaf-test.sh [options]
#   -b, --build       Build PILAF before running
#   -s, --skip-build  Skip building (use existing build)
#   -c, --config      Specify config file (default: config-demo.yaml)
#   -t, --story       Specify story file (default: demo-story.yaml)
#   -v, --verbose     Enable verbose output
#   -h, --help        Show this help message
#

set -e

# Default values
BUILD=false
SKIP_BUILD=false
CONFIG_FILE="config-demo.yaml"
STORY_FILE="demo-story.yaml"
VERBOSE=""
DOCKER_DIR="docker"
WAIT_TIME=90

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--build)
            BUILD=true
            shift
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -c|--config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        -t|--story)
            STORY_FILE="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE="--verbose"
            shift
            ;;
        -h|--help)
            grep -A 50 '# Usage:' "$0" | head -n 40
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo_step() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

echo_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

echo_error() {
    echo -e "${RED}❌ $1${NC}"
}

echo_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Function to find a valid JDK 21
find_jdk_21() {
    # Save and clear JAVA_HOME to avoid jenv issues
    local saved_java_home="$JAVA_HOME"
    unset JAVA_HOME

    # Try using jenv if available
    if command -v jenv &> /dev/null; then
        local jenv_version=$(jenv version 2>/dev/null | grep -oE "[0-9]+\.[0-9]+" | head -1)
        if [ "$jenv_version" = "21" ] || [ "$jenv_version" = "21.0" ]; then
            local jenv_prefix=$(jenv prefix 2>/dev/null)
            if [ -n "$jenv_prefix" ] && [ -x "$jenv_prefix/bin/javac" ]; then
                export JAVA_HOME="$jenv_prefix"
                echo "$jenv_prefix"
                return 0
            fi
        fi
    fi

    # Restore JAVA_HOME
    export JAVA_HOME="$saved_java_home"

    # Check /usr/lib/jvm for JDK 21
    if [ -d "/usr/lib/jvm" ]; then
        for jvm_dir in /usr/lib/jvm/*/; do
            if [ -x "${jvm_dir}bin/javac" ]; then
                local version=$("${jvm_dir}bin/java" -version 2>&1 | head -1 | grep -oE "[0-9]+\.[0-9]+")
                if [ "$version" = "21" ]; then
                    echo "${jvm_dir%/}"
                    return 0
                fi
            fi
        done
    fi

    # Check common locations
    local jdk_paths=(
        "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
        "/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home"
        "/opt/java/openjdk"
        "/opt/homebrew/opt/openjdk@21"
    )

    for path in "${jdk_paths[@]}"; do
        if [ -x "$path/bin/javac" ]; then
            echo "$path"
            return 0
        fi
    done

    # Try to find using java_home command on macOS
    if command -v java_home &> /dev/null; then
        local java_home_path=$(/usr/libexec/java_home -v 21 2>/dev/null)
        if [ -n "$java_home_path" ] && [ -x "$java_home_path/bin/javac" ]; then
            echo "$java_home_path"
            return 0
        fi
    fi

    # Fall back to system javac
    if command -v javac &> /dev/null; then
        local javac_path=$(which javac)
        local jdk_home=$(dirname "$(dirname "$(readlink -f "$javac_path")")")
        echo "$jdk_home"
        return 0
    fi

    return 1
}

# Function to wait for server to be ready
wait_for_server() {
    echo_info "Waiting for server to be ready (max ${WAIT_TIME}s)..."
    local count=0
    while [ $count -lt $WAIT_TIME ]; do
        if docker exec pilaf-papermc rcon-cli list >/dev/null 2>&1; then
            echo_success "Server is ready!"
            return 0
        fi
        sleep 2
        count=$((count + 2))
        echo -n "."
    done
    echo_error "Server did not become ready in ${WAIT_TIME}s"
    return 1
}

# Function to stop Docker services
stop_docker() {
    echo_step "Stopping Docker services..."
    if [ -f "$DOCKER_DIR/docker-compose.pilaf.yml" ]; then
        docker-compose -f "$DOCKER_DIR/docker-compose.pilaf.yml" down 2>/dev/null || true
        echo_success "Docker services stopped"
    else
        echo_info "No docker-compose file found, skipping Docker cleanup"
    fi
}

# Cleanup function
cleanup() {
    echo_step "Cleaning up..."
    stop_docker
}

# Set trap to cleanup on exit
trap cleanup EXIT

# Main execution
echo_step "PILAF Test Automation"

# Step 0: Ensure we have a valid JDK 21
echo_info "Checking Java environment..."
JDK_PATH=$(find_jdk_21)
if [ -n "$JDK_PATH" ]; then
    export JAVA_HOME="$JDK_PATH"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo_info "Using JDK at: $JAVA_HOME"
else
    echo_error "No JDK 21 found! Please install JDK 21."
    exit 1
fi

# Verify Java version
java -version 2>&1 | head -3

# Step 1: Build PILAF if requested or if not skipping
if [ "$SKIP_BUILD" = false ]; then
    echo_info "Building PILAF..."
    if [ "$BUILD" = true ] || [ ! -f "build/libs/pilaf-*.jar" ]; then
        ./gradlew clean build -x test
        echo_success "PILAF built successfully"
    else
        ./gradlew build -x test
        echo_success "PILAF built (incremental)"
    fi
else
    echo_info "Skipping build (--skip-build)"
fi

# Step 2: Start Docker services
echo_step "Starting Docker services..."
cd "$DOCKER_DIR"
docker-compose -f docker-compose.pilaf.yml down 2>/dev/null || true
docker-compose -f docker-compose.pilaf.yml up -d
cd "$SCRIPT_DIR"
echo_success "Docker services started"

# Step 3: Wait for server to be ready
wait_for_server || {
    echo_error "Failed to start server"
    exit 1
}

# Step 4: Run tests
echo_step "Running PILAF tests..."
echo_info "Config: $CONFIG_FILE, Story: $STORY_FILE"

./gradlew run --args="--config $CONFIG_FILE $STORY_FILE $VERBOSE"

if [ $? -eq 0 ]; then
    echo_success "Tests completed successfully!"
else
    echo_error "Tests failed"
    exit 1
fi

# Step 5: Show report location
echo_step "Test Complete"
echo ""
echo -e "${GREEN}📊 Test Report (HTML):${NC}"
echo -e "${GREEN}   target/pilaf-reports/PILAF_CLI_Test_Run_report.html${NC}"
echo ""
echo -e "${GREEN}📄 Other Reports:${NC}"
echo -e "${GREEN}   target/pilaf-reports/PILAF_CLI_Test_Run_report.json${NC}"
echo -e "${GREEN}   target/pilaf-reports/PILAF_CLI_Test_Run_report.txt${NC}"
echo -e "${GREEN}   target/pilaf-reports/TEST-PILAF_CLI_Test_Run.xml${NC}"
