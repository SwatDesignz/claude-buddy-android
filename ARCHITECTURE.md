# ZXBuddy Architecture

## Project Structure
```
zx_buddyv1/
├── app/
│   ├── build.gradle.kts          # App module build config
│   └── src/main/java/com/example/
│       ├── MainActivity.kt          # Single Activity, all UI (Compose)
│       ├── ui/
│       │   ├── theme/               # Material3 theme (Color, Theme, Typography)
│       │   └── viewmodel/
│       │       └── ZViewModel.kt    # Single ViewModel, all app state
│       └── data/
│           ├── api/                 # Retrofit Gemini API client
│           │   └── GeminiApi.kt     # API models + RetrofitClient singleton
│           ├── db/                  # Room database
│           │   └── ZDatabase.kt     # PetEntity, LogEntity, DAOs, ZRepository
│           └── model/
│               ├── SpeciesData.kt   # 18 species definitions with ASCII frames & base stats
│               └── Lifecycle.kt     # Pet lifecycle extension (Baby/Teen/Adult)
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
└── gradle/libs.versions.toml     # Version catalog
```

## Architecture Pattern
Single-Activity + ViewModel. No Hilt DI — dependencies are manually constructed in `MainActivity.onCreate()`.

```
MainActivity
  └─► Room Builder → AppDatabase → ZRepository → ZViewModelFactory → ZViewModel
                                                                   └─► ZBuddyTerminalApp (root Composable)
                                                                        ├─ PetConsoleView      (F1:CONSOLE tab)
                                                                        ├─ HatcheryView        (F2:HATCHERY tab)
                                                                        ├─ BluetoothSyncView   (F3:DESKTOP tab)
                                                                        └─ TerminalThemesView  (F4:THEMES tab)
```

## Data Flow
```
Room DB (SQLite)
  ├─ pets table:     id, name, species, rarity, isShiny, level, xp, hunger, energy,
  │                  debugging, patience, chaos, wisdom, snark, birthday, lastUpdated, isActive
  └─ logs table:     id, timestamp, petName, tag, message
       │
       ▼ (Flow<List<T>>)
  ZRepository ──► ZViewModel (MutableStateFlows)
       │
       ▼ (collectAsStateWithLifecycle)
  Compose UI
```

## ViewModel State
| State | Type | Description |
|-------|------|-------------|
| `allPets` | `StateFlow<List<PetEntity>>` | All pets ordered by birthday desc |
| `activePet` | `StateFlow<PetEntity?>` | Currently active pet (isActive=1) |
| `recentLogs` | `StateFlow<List<LogEntity>>` | Last 100 log entries |
| `isGeneratingResponse` | `StateFlow<Boolean>` | AI request in progress |
| `currentInputText` | `StateFlow<String>` | Command input field text |
| `terminalColorTheme` | `StateFlow<String>` | Active theme name |
| `isScanlineOverlayEnabled` | `StateFlow<Boolean>` | CRT scanline toggle |
| `hatchNameText` | `StateFlow<String>` | Hatch name input |
| `selectedHatchSpecies` | `StateFlow<String>` | Selected species for hatching |
| `currentViewFrame` | `StateFlow<Int>` | 1 or 2, toggles ASCII animation |
| `happiness` | `StateFlow<int>` | Happiness (0-100) |
| `hygiene` | `StateFlow<int>` | Hygiene (0-100) |
| `zxPoints` | `StateFlow<int>` | Points from mini-game |
| `isBLEScanning` | `StateFlow<Boolean>` | BLE scan state |
| `isBLEConnected` | `StateFlow<Boolean>` | BLE connection state |

## Key Systems

### Pet Lifecycle & Care Loop
- **Init:** Care loop runs every 15s, increases hunger by 3, decreases happiness by 2, hygiene by 1
- **Warn:** Logs critical warnings when hunger > 80, happiness < 20, hygiene < 20
- **Feed:** hunger -= 25, energy += 15
- **Poke:** patience -= 8, chaos += 10, random mood shift (giggled/protested)
- **Patch Code:** Consumes 20 energy + 15 hunger, grants 20-39 XP × rarity multiplier, level up at 100 XP
- **Hatch:** Deactivates all pets, rolls rarity (Common 50%, Uncommon 30%, Rare 14%, Epic 5%, Legendary 1%), 1% shiny

### Rarity & Shiny Mechanics
```
roll = random(1..100)
  99-100 → Legendary (1.5x stats)
  94-98  → Epic (1.3x)
  80-93  → Rare (1.15x)
  50-79  → Uncommon (1.05x)
  1-49   → Common (1.0x)
shinyRoll = random(0..99) == 7  → isShiny = true (+15 all stats)
```

### Command System
Input text sent to `sendChatMessage()`, which first checks `handleLocalCommand()` for local commands (prefixed with `/` or plain text). If matched, handled locally. Otherwise sent to Gemini API (or sandbox fallback).

Local commands: `/help`, `/stats`, `/feed`, `/train`, `/play`, `/clean`, `/minigame`, `/mode`, `/theme`, `/scan`, `/disconnect`, `/clear`

### Theme Engine
6 palette presets define foreground color and background. Selected via `/theme <name>` command or Themes tab. Scanline overlay is a separate toggle drawing 120 lines with alpha 0.05 + radial vignette.

### BLE Integration (Emulated)
Scan → delay 1.2s → pairing dialog → approve → delay 0.8s → connected. All simulated with coroutine delays.

### Gemini API Integration
Retrofit calls `POST v1beta/models/gemini-3.5-flash:generateContent`. System instruction injected with pet personality stats. Falls back to sandbox mock responses when API key is empty or placeholder.

## Testing Infrastructure
- **Unit Tests:** JUnit + Robolectric (example: context string assertion)
- **Screenshot Tests:** Roborazzi (compose screenshot capture)
- **Instrumented Tests:** Espresso/JUnit (empty example)
- **CI:** GitHub Actions (build + test on PR and push to main)
