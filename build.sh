#!/usr/bin/env bash
set -euo pipefail

# ── MindDump 构建脚本 ──────────────────────────────────
# 用法:
#   ./build.sh          构建 debug APK
#   ./build.sh release  构建 release APK
#   ./build.sh test     运行单元测试
#   ./build.sh install  构建 debug 并安装到设备

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

export MINDUMP_KEYSTORE_PASSWORD="minddump123"
export MINDUMP_KEY_PASSWORD="minddump123"

TASK="${1:-debug}"

case "$TASK" in
    debug)
        echo "🔨 构建 debug APK..."
        ./gradlew assembleDebug
        echo "✅ 完成: app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        echo "🔨 构建 release APK..."
        ./gradlew assembleRelease
        echo "✅ 完成: app/build/outputs/apk/release/app-release.apk"
        ;;
    test)
        echo "🧪 运行单元测试..."
        ./gradlew test
        echo "✅ 测试全部通过"
        ;;
    install)
        echo "📲 构建 debug 并安装到设备..."
        ./gradlew installDebug
        echo "✅ 已安装到设备"
        ;;
    *)
        echo "用法: $0 [debug|release|test|install]"
        exit 1
        ;;
esac
