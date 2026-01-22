#!/bin/bash
set -e

VERSION="1.0.0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PILAF_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🚀 Publishing Pilaf v$VERSION to npm..."
echo "Pilaf root: $PILAF_ROOT"
echo ""

# Check if logged in to npm
echo "📋 Checking npm login status..."
if ! npm whoami &>/dev/null; then
    echo "❌ Not logged in to npm. Please run: npm login"
    exit 1
fi
echo "✅ Logged in as: $(npm whoami)"
echo ""

# Function to publish a package
publish_package() {
    local pkg_path="$1"
    local pkg_name="$2"

    echo "📦 Publishing $pkg_name..."
    echo "   Path: $pkg_path"

    pushd "$pkg_path" > /dev/null || exit 1

    # Pack first to verify contents
    echo "   🔍 Verifying package contents..."
    pnpm pack --dry-run &>/dev/null || true

    # Check for test files in the tarball
    local tarball_name=$(pnpm pack 2>&1 | grep "Tarball Details" | awk '{print $1}')
    if tar -tzf "$tarball_name" 2>/dev/null | grep -qE "\.spec\.js|\.test\.js"; then
        echo "   ❌ ERROR: Test files found in package! Aborting."
        rm -f "$tarball_name"
        popd > /dev/null
        exit 1
    fi
    rm -f "$tarball_name"

    # Publish
    echo "   📤 Publishing to npm..."
    if pnpm publish --access public; then
        echo "   ✅ $pkg_name published successfully!"
    else
        echo "   ❌ Failed to publish $pkg_name"
        popd > /dev/null
        exit 1
    fi

    popd > /dev/null
    echo ""
}

# Publish in dependency order
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 1: Publishing @pilaf/backends"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
publish_package "$PILAF_ROOT/packages/backends" "@pilaf/backends"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 2: Publishing @pilaf/reporting"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
publish_package "$PILAF_ROOT/packages/reporting" "@pilaf/reporting"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 3: Publishing @pilaf/framework"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
publish_package "$PILAF_ROOT/packages/framework" "@pilaf/framework"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 4: Publishing @pilaf/cli"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
publish_package "$PILAF_ROOT/packages/cli" "@pilaf/cli"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ All packages published successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verify packages on npm
echo "⏳ Waiting 30 seconds for npm registry to propagate..."
sleep 30

echo ""
echo "🔍 Verifying packages on npm..."
npm view @pilaf/backends@$VERSION &>/dev/null && echo "✅ @pilaf/backends@$VERSION" || echo "❌ @pilaf/backends@$VERSION not found"
npm view @pilaf/reporting@$VERSION &>/dev/null && echo "✅ @pilaf/reporting@$VERSION" || echo "❌ @pilaf/reporting@$VERSION not found"
npm view @pilaf/framework@$VERSION &>/dev/null && echo "✅ @pilaf/framework@$VERSION" || echo "❌ @pilaf/framework@$VERSION not found"
npm view @pilaf/cli@$VERSION &>/dev/null && echo "✅ @pilaf/cli@$VERSION" || echo "❌ @pilaf/cli@$VERSION not found"

echo ""
echo "🎉 Pilaf v$VERSION has been published to npm!"
echo ""
echo "📖 To install:"
echo "   pnpm add -D @pilaf/cli"
echo ""
echo "🔗 View on npm: https://www.npmjs.com/package/@pilaf/cli"
