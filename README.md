
##  ZXBuddy

A retro Terminal Pet for your Android device. Think Tamagotchi meets retro computing, built for developers who miss the command line.

ZXBuddy is a lightweight, offline-first Android companion app. It drops a classic virtual pet right into a retro ASCII-styled terminal interface. It is built for engineers, hackers, and terminal lovers who want a digital desk companion that speaks their language.

    
## The Core Loop
ZXBuddy blends classic care mechanics with developer-focused stats.

* Pet Lifecycle: Watch your companion grow from Baby to Teen to Adult. Each stage alters their personality traits and stat curves.
* Care Meters: Keep an eye on Hunger, Happiness, Energy, Patience, and Chaos.
* Hacker Actions: /feed restores energy. /train or /patch runs a mini code-patch simulation for XP. /play pokes the pet to alter its mood.
* Dev Stats: Your pet tracks your debugging power, wisdom, chaos coefficient, and snark level.
* Terminal Themes: Switch palettes instantly between Matrix Green, Amber Glow, Commodore Blue, Cyberpunk Pink, or classic dark/light modes.
* Hybrid Modes: Toggle between Dev, Personal, Focus, or AI Mentor modes to change how your pet reacts to you.

------------------------------
##  Completely Offline-First
Your pet lives locally. ZXBuddy uses an Android Room database to manage all stats, loops, and history directly on your device.
No data leaves your phone, and no internet connection is required for core gameplay.
Note: If you want to use the AI Mentor mode, just drop your Gemini API key into the app configuration. Otherwise, it functions 100% offline.
------------------------------
##  Quick Start Command Cheat-Sheet
You can control everything via the built-in terminal input bar. Tap the command line and type:

| Command | What it does |
|---|---|
| /help | Prints the full command list. |
| /stats | Shows your pet's current levels and XP. |
| /feed | Lowers hunger and boosts energy. |
| /patch | Runs a code-patch simulation for XP. |
| /play | Pokes the pet (modifies mood and chaos). |
| /theme <name> | Switches palette (matrix, amber, blue, pink, dark). |
| /mode <name> | Changes personality behavior (dev, personal, focus, ai). |
| /clear | Flushes the local debug logs. |

------------------------------
## Local Environment Setup
If you want to build the APK yourself or tweak the pet's behavior, here is how to get your environment ready.
## 1. Prerequisites
You will need Java 21, the Android SDK, and Gradle. If you use Homebrew, set it up like this:

# Install core dependencies
brew install openjdk@21 gradle
brew install --cask android-sdk

Configure your environment variables in your .bashrc or .zshrc:

export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

## 2. SDK Licenses & Packages
Accept the Android licenses and install the specific target platform:

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

## 3. Generate the Debug Keystore
Gradle expects a local debug.keystore file in the project root to sign your development builds:

keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug, O=Android, C=US"

------------------------------
##  Building & Running

# 1. Grab the repository
git clone <repo‑url>
cd zx_buddyv1
# 2. Build the binaries
./gradlew --no-configuration-cache assembleDebug


* Debug APK destination: app/build/outputs/apk/debug/app-debug.apk
* Android Studio: Select Open Project, point to the zx_buddyv1 folder, and hit Sync Project with Gradle Files.

## Production CI Environment Variables
If you are setting up a secure pipeline, use these environment variables for signing production releases:

export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=<your-store-password>
export KEY_PASSWORD=<your-key-password>

------------------------------
##  What's Next?

* Core Pet Loop: Lifecycle tracking, feeding, and stat updates are live.
* Theme Engine: Terminal skins and text commands work perfectly.
* Arcade Mini-Game: Core engine is ready; UI polish is underway to earn "ZX Points".
* BLE Desktop Pairing: Early UI scaffolding is in place to link your pet with a "Claude Desktop" client.
* SSH Tunnelling: Future update to let you SSH into your pet for remote debugging and session links.

------------------------------
## Contributing & License
Pull requests are highly encouraged. Please use conventional commits for your messages and run ./gradlew check to verify linting before opening a PR.
Distributed under the MIT License. See LICENSE for details.
------------------------------


