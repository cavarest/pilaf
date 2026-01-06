#!/bin/bash

# PILAF Backend Consistency Testing Script
# This script runs consistency tests across all backend combinations

set -e

echo "========================================"
echo "  PILAF Backend Consistency Testing"
echo "========================================"
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle" ]; then
    echo "Error: Please run this script from the PILAF project root directory"
    exit 1
fi

# Compile the project first
echo "Step 1: Compiling PILAF project..."
gradle compileJava --quiet

# Run the consistency tests
echo "Step 2: Running backend consistency tests..."
gradle run --args="--consistency-test"

echo ""
echo "Step 3: Test execution completed!"
echo "Check the generated consistency report files:"
echo "  - HTML Report: consistency-report-*.html"
echo "  - Text Report: consistency-report-*.txt"
echo ""

# Show available reports
if ls consistency-report-*.html 1> /dev/null 2>&1; then
    echo "Generated HTML reports:"
    ls -la consistency-report-*.html
    echo ""
fi

if ls consistency-report-*.txt 1> /dev/null 2>&1; then
    echo "Generated text reports:"
    ls -la consistency-report-*.txt
    echo ""
fi

echo "Consistency testing completed!"
