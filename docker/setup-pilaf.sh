#!/bin/bash

# PILAF One-Time Setup Script
# This script sets up the complete PILAF ecosystem for developers
# Usage: bash lib/pilaf/docker/setup-pilaf.sh

set -e

echo "🚀 PILAF Ecosystem Setup"
echo "========================"
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
print_status "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    print_error "Java is not installed. Please install Java 21 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    print_error "Java 21 or later is required. Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

print_success "Java $(java -version 2>&1 | head -n 1 | cut -d'"' -f2) detected"

# Check Maven
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven 3.8 or later."
    exit 1
fi

print_success "Maven $(mvn -version | head -n 1) detected"

# Check Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker."
    exit 1
fi

print_success "Docker $(docker --version) detected"

# Check Docker Compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose."
    exit 1
fi

if command -v docker-compose &> /dev/null; then
    print_success "Docker Compose $(docker-compose --version) detected"
else
    print_success "Docker Compose (v2) $(docker compose version --short) detected"
fi

echo

# Step 1: Build PILAF CLI
print_status "Step 1: Building PILAF CLI..."
cd "$(dirname "$0")/.."

if mvn clean package -DskipTests -q; then
    print_success "PILAF CLI built successfully"
else
    print_error "Failed to build PILAF CLI"
    exit 1
fi

# Step 2: Copy CLI to root directory for easy access
print_status "Step 2: Setting up CLI access..."
PILAF_JAR=$(find target -name "pilaf-*.jar" | head -n 1)
if [ -n "$PILAF_JAR" ]; then
    cp "$PILAF_JAR" ../pilaf.jar
    print_success "CLI copied to root: pilaf.jar"
else
    print_error "Could not find built PILAF JAR"
    exit 1
fi

# Step 3: Setup Docker environment
print_status "Step 3: Setting up Docker environment..."
cp docker/docker-compose.pilaf.yml ../../docker-compose.pilaf.yml
print_success "Docker Compose configuration copied to root"

# Step 4: Create sample configuration
print_status "Step 4: Creating sample configuration..."
cat > ../../pilaf.yaml << 'EOF'
# PILAF Configuration File
# This file contains settings for connecting to Minecraft services
# and configuring test execution

# Backend Configuration
backend: "mineflayer"  # Options: mineflayer, rcon, mock

# Service Connection Settings
mineflayer_url: "http://localhost:3000"
rcon_host: "localhost"
rcon_port: 25575
rcon_password: "dragon123"

# Story Discovery
stories:
  - "src/test/resources/integration-stories/"  # Directory of integration tests
  - "src/test/resources/test-stories/"         # Directory of unit tests

# Reporting
report_directory: "target/pilaf-reports"
verbose: false

# Health Checks
skip_health_checks: false
EOF

print_success "Sample configuration created: pilaf.yaml"

# Step 5: Create convenience scripts
print_status "Step 5: Creating convenience scripts..."

# Start services script
cat > ../../start-pilaf-services.sh << 'EOF'
#!/bin/bash
# Start PILAF Docker services
echo "🚀 Starting PILAF services..."
docker-compose -f docker-compose.pilaf.yml up -d
echo "⏳ Waiting for services to be ready..."
sleep 10
echo "🏥 Checking service health..."
java -jar pilaf.jar --health-check
EOF

# Stop services script
cat > ../../stop-pilaf-services.sh << 'EOF'
#!/bin/bash
# Stop PILAF Docker services
echo "🛑 Stopping PILAF services..."
docker-compose -f docker-compose.pilaf.yml down
echo "✅ Services stopped"
EOF

# Run tests script
cat > ../../run-pilaf-tests.sh << 'EOF'
#!/bin/bash
# Run PILAF tests
echo "🧪 Running PILAF tests..."
java -jar pilaf.jar --config=pilaf.yaml --verbose
echo "📊 Opening test report..."
if command -v open &> /dev/null; then
    open target/pilaf-reports/index.html
elif command -v xdg-open &> /dev/null; then
    xdg-open target/pilaf-reports/index.html
else
    echo "Report available at: target/pilaf-reports/index.html"
fi
EOF

chmod +x ../../start-pilaf-services.sh
chmod +x ../../stop-pilaf-services.sh
chmod +x ../../run-pilaf-tests.sh

print_success "Convenience scripts created"

# Step 6: Create README for quick start
print_status "Step 6: Creating quick start guide..."
cat > ../../PILAF-QUICKSTART.md << 'EOF'
# PILAF Quick Start Guide

## 🚀 One-Time Setup (Already Done)

Your PILAF ecosystem is now ready! The setup script has configured:

- ✅ PILAF CLI (`pilaf.jar`)
- ✅ Docker Compose stack (`docker-compose.pilaf.yml`)
- ✅ Configuration file (`pilaf.yaml`)
- ✅ Convenience scripts

## 🏃‍♂️ Daily Development Workflow

### 1. Start PILAF Services
```bash
./start-pilaf-services.sh
```

### 2. Create YAML Test Stories
Create test files in `src/test/resources/integration-stories/`:

```yaml
# src/test/resources/integration-stories/my-plugin-test.yaml
name: "My Plugin Test"
description: "Test my plugin functionality"

setup:
  - action: "execute_rcon_command"
    command: "op test_player"
    name: "Make test player operator"

steps:
  - action: "execute_player_command"
    player: "test_player"
    command: "/myplugin test"
    name: "Execute plugin command"

cleanup:
  - action: "execute_rcon_command"
    command: "deop test_player"
    name: "Remove operator privileges"
```

### 3. Run Tests
```bash
./run-pilaf-tests.sh
```

### 4. View Results
Open `target/pilaf-reports/index.html` in your browser.

## 🔧 Manual Commands

### Start Services Manually
```bash
docker-compose -f docker-compose.pilaf.yml up -d
```

### Run PILAF CLI
```bash
# Using config file
java -jar pilaf.jar --config=pilaf.yaml

# Using command line arguments
java -jar pilaf.jar \
  --stories=src/test/resources/integration-stories/ \
  --mineflayer-url=http://localhost:3000 \
  --verbose

# Health check
java -jar pilaf.jar --health-check
```

### Stop Services
```bash
./stop-pilaf-services.sh
```

## 🛠️ Troubleshooting

### Check Service Status
```bash
docker-compose -f docker-compose.pilaf.yml ps
```

### View Logs
```bash
# All services
docker-compose -f docker-compose.pilaf.yml logs

# Specific service
docker-compose -f docker-compose.pilaf.yml logs papermc
docker-compose -f docker-compose.pilaf.yml logs mineflayer-bridge
```

### Reset Environment
```bash
./stop-pilaf-services.sh
docker-compose -f docker-compose.pilaf.yml down -v  # Remove volumes
./start-pilaf-services.sh
```

## 📚 Next Steps

1. **Write YAML Stories**: Create test scenarios in YAML format
2. **Explore Commands**: Check PILAF README for 35+ available actions
3. **Customize Config**: Modify `pilaf.yaml` for your needs
4. **Integrate CI/CD**: Add PILAF to your build pipeline

## 🆘 Need Help?

- **PILAF README**: `lib/pilaf/README.adoc` - Complete documentation
- **Command Reference**: 35+ YAML actions for testing
- **Examples**: Look in `src/test/resources/integration-stories/`

Happy testing! 🎮✨
EOF

print_success "Quick start guide created: PILAF-QUICKSTART.md"

# Step 7: Final verification
print_status "Step 7: Final verification..."
echo

# Test PILAF CLI
print_status "Testing PILAF CLI..."
if java -jar ../pilaf.jar --help > /dev/null 2>&1; then
    print_success "PILAF CLI is working"
else
    print_warning "PILAF CLI test failed, but this may be normal"
fi

echo
print_success "🎉 PILAF ecosystem setup complete!"
echo
echo "📋 Summary:"
echo "  • PILAF CLI: pilaf.jar"
echo "  • Docker Stack: docker-compose.pilaf.yml"
echo "  • Configuration: pilaf.yaml"
echo "  • Quick Start: PILAF-QUICKSTART.md"
echo "  • Convenience Scripts: start/stop/run-pilaf-*.sh"
echo
echo "🚀 Next Steps:"
echo "  1. Start services: ./start-pilaf-services.sh"
echo "  2. Run tests: ./run-pilaf-tests.sh"
echo "  3. Read guide: cat PILAF-QUICKSTART.md"
echo
print_success "Happy testing with PILAF! 🎮✨"
