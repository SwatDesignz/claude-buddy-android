# Play Store Listing — ZXBuddy™

> Copy-paste these into the Google Play Console.
> All dates and version info as of June 2026.

---

## App Details

- **App name:** ZXBuddy
- **Subtitle (optional):** Retro Terminal Pet Companion
- **Category:** Entertainment → Pets & Virtual Pets
- **Tags (optional):** virtual pet, retro, terminal, tamagotchi, pixel art
- **Content rating:** Everyone

---

## Short Description (80 chars)

> Retro terminal pet companion. Hatch, train & evolve your digital buddy offline.

---

## Full Description (max 4000 chars)

ZXBuddy brings the classic virtual pet experience to your Android device — with a retro terminal aesthetic, mini-games, shiny hunting, and offline-first design.

**🐣 HATCH & EVOLVE**
Hatch eggs into unique pets with distinct personalities and stat curves. Watch your buddy grow through Baby → Teen → Adult stages. Each stage unlocks new traits, behaviours, and visual flair.

**📊 CARE MECHANICS**
Manage 5 core stats — Hunger, Happiness, Energy, Patience, and Chaos. Feed, play, train, and clean to keep your pet healthy. Neglect matters: the stats respond in real time.

**🕹️ MINI-GAMES — EARN ZX POINTS**
- **Simon Says** — Test your memory. Match the pattern to earn ZX Points and unlock new species.
- **Whack-a-Bug** — Bugs are loose in the terminal! Squish 'em in a 3×3 grid before time runs out. Each squish awards points.
- **Lucky Spin** — Feeling lucky? Bet your ZX Points on a 3-reel slot. Hit the jackpot (3 matching symbols) for a 5× payout.

**✨ SHINY HUNTING (Pokémon GO style)**
Wild encounters can spawn Shiny pets — ultra-rare colour variants with animated sparkle effects and a golden glow. Every encounter has a small chance to be shiny. Collect them all!

**🎨 RETRO TERMINAL AESTHETIC**
- ASCII art pet display
- 6 colour themes: Matrix Green, Amber Glow, Commodore Blue, Cyberpunk Pink, Classic White, Elegant Dark
- Scanline overlay toggle for that CRT feel
- In-app terminal command system (/feed, /stats, /theme, /mode, etc.)

**🤖 AI MENTOR MODE (Optional)**
Connect your own Gemini or OpenRouter API key to unlock AI-powered conversations with your pet. Ask for help debugging, get personality-driven responses, or just chat. Works fully offline without an API key.

**📡 BLE DESKTOP SYNC (Experimental)**
Pair with a desktop client over Bluetooth Low Energy for cross-device telemetry and extended capabilities.

**🏆 ACHIEVEMENT SYSTEM**
Earn achievements as you play — hatch your first pet, reach level 10, squish 50 bugs, win Lucky Spin, and more. Each achievement is tracked locally and never leaves your device.

**🔒 PRIVACY FIRST — 100% Offline**
- No accounts, no sign-ups, no tracking
- No analytics SDKs
- All data stored locally via Room (SQLite)
- Internet only used if you supply an API key for AI chat

---

## What's New (v0.16.2)

- ✨ **Shiny Pets!** Wild encounters now have a chance to spawn ultra-rare shiny variants with animated sparkle effects
- 🐛 **Whack-a-Bug** mini-game added to the GAME tab
- 🎰 **Lucky Spin** slot machine — bet ZX Points for a chance at jackpot
- 🛠️ **Provider tab** now shows AI provider status, connection latency, and key config in-app
- 🏆 New achievements: Bug Squisher, Whack Master, Lucky Dev
- 🐛 Bug fixes and performance improvements

---

## Feature Graphic Spec (1024×500px)

ZXBuddy's feature graphic should be a bold, retro-computing image:

**Design concept:**
- Dark background (#0a0a0a or #1a1a2e) with subtle scanline overlay
- Large ASCII-style text "ZXBUDDY" in bright green (#00ff41) or amber (#ffb000)
- Pixel-art pet character (cute monster/dino face) next to the text
- Tagline: "YOUR TERMINAL PET" in a smaller retro font
- Optional: sparkle stars in gold (#ffd700) around the pet

**Tools:** Canva, Figma, or Photoshop. Use "Press Start 2P" or similar pixel font.

---

## Screenshot Guide

Capture these screens from the connected device (moto g stylus 5G) or an emulator:

### Required (8 screenshots)

| # | Screen | What to Show | Caption |
|---|--------|-------------|---------|
| 1 | **F1: CONSOLE** | Active pet with ASCII art, stats bar, terminal input | "Your pet's home — feed, train & play from the terminal" |
| 2 | **F2: HATCHERY** | Pet list showing species and life stage | "Hatch eggs and raise unique pets" |
| 3 | **F3: DESKTOP** | BLE status + AI Provider status cards | "BLE sync & AI provider management" |
| 4 | **F7: GAME — Simon Says** | Simon Says game with pattern | "Test your memory with Simon Says" |
| 5 | **F7: GAME — Whack-a-Bug** | Bug grid showing active bugs | "Whack bugs for ZX Points!" |
| 6 | **F7: GAME — Lucky Spin** | Slot machine with result | "Spin to win — bet ZX on the slots" |
| 7 | **F6: ACHIEVEMENTS** | Achievement list with progress | "Earn achievements as you play" |
| 8 | **F4: SHOP** | Shop screen with items | "Spend ZX Points on items & upgrades" |

### Phone Screenshots
- Portrait mode, 1080×1920px or native resolution (moto g stylus 5G = 1080×2400)
- Clean background (set theme to Matrix Green or Amber Glow for best contrast)
- No status bar clutter (use adb to hide it or crop in editing)

### Tablet Screenshots (optional but recommended)
- 1920×1080px landscape on a tablet emulator
- Show F1: CONSOLE with full terminal width visible

### Pro Tips
1. Before screenshots: `/clear` to clean the terminal log
2. Set theme: `/theme matrix` or `/theme amber` for retro look
3. Have stats in healthy range (feed/train beforehand)
4. For game screenshots, start a game round so it's in "Playing" state
5. Use `adb exec-out screencap -p > screenshot.png` from this terminal for clean captures

### adb Screenshot Commands
```bash
# From project root:
adb exec-out screencap -p > store_assets/screenshot_01_console.png

# Crop status bar (optional, adjust height):
# 1080×2400 device → crop top 120px off
```

---

## Icon Notes

The app already ships with adaptive icons (`ic_launcher`). For the Play Store listing:

- **High-res icon:** 512×512px PNG or 32-bit
- **Feature graphic:** 1024×500px
- **TV banner (optional):** 1280×720px

If you want to create a pixel-art app icon, generate a 512×512 PNG that matches the retro terminal theme (pixel pet face + green/amber glow).

---

## Content Rating Questionnaire

When filling out the Play Console content rating:

1. **Category:** Entertainment / Pets & Virtual Pets
2. **Violence:** None — no realistic violence, just cartoon bugs being squished (whack-a-bug)
3. **Sexual Content:** None
4. **Controlled Substances:** None
5. **Hate Speech:** None
6. **User Interactions:** The app has optional AI chat that sends messages to third-party APIs if configured — mark "Not filtered" for user interactions
7. **Sharing Location:** No
8. **Digital Purchases:** No — no in-app purchases
9. **Advertising:** None — no ads
10. **Account Creation:** Not required

Rating should result in **"Everyone" (ESRB) / "3+" (PEGI)**.

---

## Privacy Policy URL

Host the `PRIVACY.md` at one of:
- GitHub Pages: `https://swatdesignz.github.io/claude-buddy-android/privacy/`
- Raw GitHub: `https://github.com/SwatDesignz/claude-buddy-android/blob/master/PRIVACY.md`
- For Play Store, the raw URL works — Google renders markdown
