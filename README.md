# Claude Buddy for Android – ZXBuddy™ Retro Terminal Pet Companion

## Overview
**Claude Buddy for Android** is a lightweight companion app (codenamed **ZXBuddy**) that brings a classic Tamagotchi‑style virtual pet to the modern Android terminal experience. The app blends retro ASCII art, ASCII‑styled personality traits, and developer‑focused interactions with a modern Android UI. It runs **offline‑first**, requires no external servers for core features, and is designed to be friendly to developers who love a terminal aesthetic.

## ALL FEATURES ARE EXPERIMENTAL AT THIS TIME
## Core Features
- **Pet Lifecycle** – Hatch, grow, and mature your pet through three life stages (Baby → Teen → Adult). Each stage carries distinct personality traits and stat curves.
- **Care Mechanics** – Manage **Hunger**, **Happiness**, **Energy**, **Patience**, and **Chaos** meters. Feeding, playing, and cleaning keep the pet healthy.
- **Mood & Training System** –  
  - **Feed** restores energy and reduces hunger.  
  - **Train/Patch** runs a small code‑patch simulation that grants XP and occasional level‑ups.  
  - **Play** (poke) influences mood and chaos.
- **Developer Stats** – Display debugging power, wisdom, chaos coefficient, and snark level. Shows an at‑a‑glance health of your companion.
- **Mode Switching** – Choose from *Dev*, *Personal*, *Focus*, or *AI Mentor* modes, each biasing the pet’s behavior and UI theme.
- **Theme Engine** – Switch UI palettes (Matrix Green, Amber Glow, Commodore Blue, Cyberpunk Pink, Classic White, Elegant Dark) on the fly.
- **BLE Integration (Experimental)** – Scan for and pair with a “Claude Desktop” client for future cross‑device telemetry. A placeholder UI is included.
- **Mini‑Game Hook** – A simple arcade‑style mini‑game awards “ZX Points” that can unlock extra species or cosmetics.
- **In‑App Command System** – Type short commands directly in the terminal‑style input bar (`/feed`, `/stats`, `/theme matrix`, `/mode dev`, etc.) to trigger instant actions without invoking AI APIs.

## Offline‑First Design
All core pet logic (feeding, training, stats updates) runs locally using Android Room databases. Network calls are only made for AI‑generated responses when an **AI Mentor** mode is active and a valid Gemini API key is supplied. This keeps the app usable anywhere—no connectivity required for basic pet care.

## Setup & Build

### Prerequisites
1. **Java Development Kit 21** – installed via Homebrew (`brew install openjdk@21`) or your preferred JDK.
2. **Android SDK** – installed via Homebrew (`brew install --cask android-sdk` or download manually).  
   - Ensure the following environment variables point to the SDK location:  
     ```bash
     export ANDROID_HOME=$HOME/android-sdk
     export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
     ```
3. **Gradle** – installed (`brew install gradle`).

### Android SDK Packages
Run once to accept licenses and install build‑tools:
```bash
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

### Project‑Specific Fixes
- **Debug Keystore** – generated with:  
  ```bash
  keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug, O=Android, C=US"
  ```
  Place it in the project root; Gradle will automatically use `debug.keystore` for signing local builds.
- **Gradle Wrapper** – The project ships with a `gradle` wrapper configuration in `settings.gradle.kts`. No wrapper JAR is required; the installed Gradle will be used.

### Build Steps
```bash
# Ensure the SDK path is correct in local.properties
git clone <repo‑url>
cd zx_buddyv1
# Verify environment variables are set (ANDROID_HOME, JAVA_HOME)
./gradlew --no-configuration-cache assembleDebug assembleRelease
```
- Debug APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.  
- Release APK will be at `app/build/outputs/apk/release/app-release.apk` (signature step later handled via CI or manual keystore).

### Environment Variables for CI / Local Builds
```bash
export ANDROID_HOME=$HOME/android-sdk
export JAVA_HOME=$HOME/linuxbrew/Cellar/openjdk@21/21.0.11
export PATH=$PATH:$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Release signing (keystore file + env vars — never commit these)
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=<your-store-password>
export KEY_PASSWORD=<your-key-password>
```

## Running the App
1. Open Android Studio, select **Open Project**, and point to the `zx_buddyv1` folder.  
2. Sync Gradle (`File → Sync Project with Gradle Files`).  
3. Run the app on an emulator or physical device.  
4. Optionally create an `.env` file with a valid `GEMINI_API_KEY` to enable AI responses; otherwise the app operates fully offline.

## Command Cheat‑Sheet
| Command | Effect |
|--------|--------|
| `/help` or `/cmds` | Prints the full command list. |
| `/stats` or `/profile` | Shows current pet stats and XP. |
| `/feed` | Feeds the pet – restores energy, reduces hunger. |
| `/train` or `/patch` | Runs a small code‑patch simulation for XP. |
| `/play` | Pokes the pet – affects mood and chaos. |
| `/clean` or `/hygiene` | Clean‑up action (placeholder for future hygiene logic). |
| `/mode <dev|personal|focus|ai>` | Switches operational mode. |
| `/theme <matrix|amber|blue|pink|white|dark>` | Changes UI colour theme. |
| `/scan` or `/sync` | Starts BLE scan (experimental). |
| `/disconnect` | Ends BLE connection. |
| `/clear` | Clears debug logs. |

## Roadmap (✓ Completed / ◻ Planned)
- **✓ Basic pet lifecycle & care loop** – implemented.  
- **✓ Theme & mode switching** – implemented.  
- **✓ In‑app command system** – implemented.  
- **⚽ Mini‑game prototype** – core loop ready; UI polishing pending.    
- **🔗 BLE Desktop pairing** – UI scaffolding added; functional pairing pending integration test.  
- **🔐 SSH pairing & remote debugging** – remote shell tunnel to pet; wire debugging terminal sessions through ADB or local SSH server. Planned.
- **📡 Wireless & weird debugging connections** – ADB-over-WiFi, USB serial fallback, NFC tap-to-pair, Morse-code BLE beacon, and other unconventional device-link experiments. Planned.
- **◻ Dependency injection with Hilt** — current manual ViewModelProvider.Factory is adequate for the single-screen scope; revisit once Firebase, remote sync, or multi-Activity navigation is added.

## Contributing
Pull requests are welcome. Please follow the conventional commit style for changelog entries. Run `./gradlew check` before submitting to ensure linting passes.

## License
MIT – see the `LICENSE` file for details.