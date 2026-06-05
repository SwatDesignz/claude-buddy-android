# ZXBuddy v0.16.2

**Release date:** June 4, 2026

## ✨ What's New

### Shiny Pets (Pokémon GO style)
- Wild encounters now have a small chance to spawn **Shiny** pets — ultra-rare color variants
- Animated sparkle particles float around shiny pets
- Golden glow border and color-tinted ASCII art
- Sparkle animation uses Compose `infiniteTransition` — no jank

### 🐛 Whack-a-Bug Mini Game
- New mini game in F7:GAME tab
- Bugs pop up in a 3×3 grid — tap to squish 'em
- 30-second rounds with increasing difficulty
- Score ZX Points based on performance
- Pet gives personality-driven commentary after each game
- New achievements: Bug Squisher (squish 50 total), Whack Master (score 20+)

### 🎰 Lucky Spin (Slot Machine)
- Bet 5, 10, or 25 ZX Points on a 3-reel slot
- 3 matching symbols = Jackpot (5× payout)
- 2 matching = small win (2× payout)
- Animated spinning effect with result celebration
- New achievement: Lucky Dev (win first spin)

### 🛠️ AI Provider & Desktop Tab Redesign (F3:DESKTOP)
- BLE connection status with radar animation remains at top
- New **AI Provider Status** section shows:
  - Sandbox (always available ✅), Gemini, OpenRouter
  - Status indicator dot (green/red/yellow)
  - Whether API key is configured
  - "Test Connection" button per provider
  - Active provider switching buttons

### 🏆 Achievement System
- 8 achievements trackable with Room persistence
- Progress bar showing overall completion
- Unlock notifications in terminal activity log
- Pet reacts on achievement unlock

### 🐣 Full Pet Lifecycle
- Baby → Teen → Adult evolution stages
- 5 core stats: Hunger, Happiness, Energy, Patience, Chaos
- Feeding, training, playing, and cleaning mechanics
- Personality-driven pet responses

### 🎨 Retro Terminal Aesthetic
- ASCII art pet display for all species
- 6 color themes: Matrix Green, Amber Glow, Commodore Blue, Cyberpunk Pink, Classic White, Elegant Dark
- Scanline overlay toggle
- In-app terminal command system (/feed, /stats, /theme, /mode, etc.)

### 🤖 AI Mentor Mode (Optional)
- Connect your own Gemini or OpenRouter API key
- Chat with your pet for personality-driven responses
- Works fully offline without an API key

### 📦 Other
- Room database (v2) with pets, logs, achievements tables
- 8 species: Rexy, Whiskers, Bubbles, Sparky, Bloop, Mochi, Ghost, Ember
- In-app command system with 20+ commands
- Settings persistence (theme, mode, scanline toggle)
- ProGuard minification for release builds
- Privacy-first — no analytics, no tracking, no accounts
- Offline-first design — all data stored locally

## 🔧 Technical

- **minSdk:** 31 | **targetSdk:** 36 | **compileSdk:** 36
- **JDK 21** required for build
- **Kotlin** + **Jetpack Compose** + **Room** + **Moshi** + **Retrofit**
- **PKCS12 keystore** signing (store password = key password)

## 📱 Downloads

| File | Size | Type |
|------|------|------|
| `zxbuddy-v0.16.2-debug.apk` | 30 MB | Debug APK (installable) |
| `zxbuddy-v0.16.2-release.apk` | 3.4 MB | Release APK (minified) |
| `zxbuddy-v0.16.2-release.aab` | 4.5 MB | Release AAB (Play Store) |

## 🔜 Coming Next

- GitHub Actions CI for auto-builds
- Play Store deployment
- BLE desktop pairing
- SSH remote debugging
- Additional mini-games
