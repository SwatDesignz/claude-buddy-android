#!/bin/bash
# Capture screenshots for Play Store listing
# Run from project root: bash store_assets/capture_screenshots.sh

set -e

OUT="store_assets"
PKG="com.zxbuddy.app"

echo "📱 Launching ZXBuddy..."
adb shell monkey -p "$PKG" 1 2>/dev/null || adb shell am start -n "$PKG/.MainActivity"
sleep 2

echo "📸 1. Console (F1) — main pet view"
adb shell input keyevent KEYCODE_F1
sleep 1
adb shell input tap 300 100  # tap to ensure focus
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_01_console.png"
echo "   saved $OUT/screenshot_01_console.png"

echo "📸 2. Hatchery (F2) — pet list"
adb shell input keyevent KEYCODE_F2
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_02_hatchery.png"
echo "   saved $OUT/screenshot_02_hatchery.png"

echo "📸 3. Desktop/Provider (F3) — BLE + AI provider"
adb shell input keyevent KEYCODE_F3
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_03_desktop.png"
echo "   saved $OUT/screenshot_03_desktop.png"

echo "📸 4. Shop (F4)"
adb shell input keyevent KEYCODE_F4
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_04_shop.png"
echo "   saved $OUT/screenshot_04_shop.png"

echo "📸 5. Achievements (F6)"
adb shell input keyevent KEYCODE_F6
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_05_achievements.png"
echo "   saved $OUT/screenshot_05_achievements.png"

echo "📸 6. Game (F7) — Simon Says"
adb shell input keyevent KEYCODE_F7
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_06_game_simon.png"
echo "   saved $OUT/screenshot_06_game_simon.png"

echo "📸 7. Game (F7) — tap Bug tab"
adb shell input keyevent KEYCODE_F7
sleep 1
# Tap on "BUG" sub-tab (adjust coordinates based on your layout)
adb shell input tap 600 100
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_07_game_bug.png"
echo "   saved $OUT/screenshot_07_game_bug.png"

echo "📸 8. Game (F7) — tap SPIN tab"
adb shell input tap 900 100
sleep 1
adb exec-out screencap -p > "$OUT/screenshot_08_game_spin.png"
echo "   saved $OUT/screenshot_08_game_spin.png"

echo ""
echo "✅ All screenshots captured in $OUT/"
echo ""
echo "To crop status bars (optional):"
echo "  mogrify -crop 1080x2280+0+120 $OUT/*.png"
ls -lh "$OUT"/*.png
