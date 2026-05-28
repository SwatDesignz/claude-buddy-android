package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.LogEntity
import com.example.data.db.PetEntity
import com.example.data.db.ZRepository
import com.example.data.model.SpeciesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

class ZViewModel(private val repository: ZRepository) : ViewModel() {

    val allPets: StateFlow<List<PetEntity>> = repository.allPets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePet: StateFlow<PetEntity?> = repository.activePet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentLogs: StateFlow<List<LogEntity>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse: StateFlow<Boolean> = _isGeneratingResponse.asStateFlow()

    private val _currentInputText = MutableStateFlow("")
    val currentInputText: StateFlow<String> = _currentInputText.asStateFlow()

    private val _isBLEScanning = MutableStateFlow(false)
    val isBLEScanning: StateFlow<Boolean> = _isBLEScanning.asStateFlow()

    private val _isBLEConnected = MutableStateFlow(false)
    val isBLEConnected: StateFlow<Boolean> = _isBLEConnected.asStateFlow()

    private val _showBLEPairDialog = MutableStateFlow(false)
    val showBLEPairDialog: StateFlow<Boolean> = _showBLEPairDialog.asStateFlow()

    private val _terminalColorTheme = MutableStateFlow("Elegant Dark") // Elegant Dark, Matrix Green, Amber Glow, Commodore Blue, Cyberpunk Pink, Classic White
    val terminalColorTheme: StateFlow<String> = _terminalColorTheme.asStateFlow()

    private val _isScanlineOverlayEnabled = MutableStateFlow(true)
    val isScanlineOverlayEnabled: StateFlow<Boolean> = _isScanlineOverlayEnabled.asStateFlow()

    private val _hatchNameText = MutableStateFlow("")
    val hatchNameText: StateFlow<String> = _hatchNameText.asStateFlow()

    private val _selectedHatchSpecies = MutableStateFlow("Owl")
    val selectedHatchSpecies: StateFlow<String> = _selectedHatchSpecies.asStateFlow()

    private val _currentViewFrame = MutableStateFlow(1)
    val currentViewFrame: StateFlow<Int> = _currentViewFrame.asStateFlow()

    private val r = Random()

    init {
        // Animation ticker to alternate pet ASCII frames roughly every 1.5 seconds for idle 60fps vibes
        viewModelScope.launch {
            while (true) {
                delay(1200)
                _currentViewFrame.value = if (_currentViewFrame.value == 1) 2 else 1
            }
        }

        // Initialize default greeting and system checks
        _terminalColorTheme.value = "Elegant Dark"
        viewModelScope.launch {
            delay(500)
            val directPet = repository.getActivePetDirect()
            if (directPet == null) {
                addLog("SYSTEM", "CORE", "Z-XBuddy Zig virtual engine build v0.16.2 initiated successfully.")
                addLog("SYSTEM", "CORE", "WARNING: No terminal companion active. Use the Hatchery tab to mobilize a pet!")
            } else {
                addLog(directPet.name, "SYSTEM", "Zig virtual pet companion loaded. ${directPet.name} is ONLINE.")
                addLog(directPet.name, "SYSTEM", "Rarity: ${directPet.rarity} | Level: ${directPet.level} | Personality specs: DBG:${directPet.debugging} SLW:${directPet.patience} CHS:${directPet.chaos}")
            }
        }
    }

    fun updateHatchNameText(txt: String) {
        _hatchNameText.value = txt
    }

    fun updateInputText(txt: String) {
        _currentInputText.value = txt
    }

    fun updateHatchSpecies(spec: String) {
        _selectedHatchSpecies.value = spec
    }

    fun setTerminalTheme(themeName: String) {
        _terminalColorTheme.value = themeName
        viewModelScope.launch {
            val petName = repository.getActivePetDirect()?.name ?: "CORE"
            repository.addLog(petName, "CONFIG", "Terminal palette swither updated: $themeName")
        }
    }

    fun toggleScanlines() {
        _isScanlineOverlayEnabled.value = !_isScanlineOverlayEnabled.value
    }

    fun addLog(petName: String, tag: String, message: String) {
        viewModelScope.launch {
            repository.addLog(petName, tag, message)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.addLog("SYSTEM", "RESET", "Console buffer cleared.")
        }
    }

    // Breed or hatch a new pet custom seed deterministically
    fun hatchNewPet() {
        val nameInput = _hatchNameText.value.trim()
        if (nameInput.isEmpty()) {
            addLog("SYSTEM", "HATCH_ERROR", "Adoption aborted: Companion must have an identifier [Name].")
            return
        }

        viewModelScope.launch {
            _isGeneratingResponse.value = true
            addLog("SYSTEM", "ZIG_COMPILER", "Hatch protocol activated. Connecting seed generator...")
            delay(600)
            addLog("SYSTEM", "ZIG_COMPILER", "Compiling dynamic personality modules for: ${_selectedHatchSpecies.value}...")
            delay(650)

            val spec = SpeciesData.getByName(_selectedHatchSpecies.value)
            
            // Roll rarity
            // 1% Shiny chance, else tiers: Common: 50%, Uncommon: 30%, Rare: 14%, Epic: 5%, Legendary: 1%
            val roll = r.nextInt(100) + 1
            val shinyRoll = r.nextInt(100) == 7
            val statusShiny = shinyRoll

            val rarityTier = when {
                roll >= 99 -> "Legendary"
                roll >= 94 -> "Epic"
                roll >= 80 -> "Rare"
                roll >= 50 -> "Uncommon"
                else -> "Common"
            }

            // Stat multipliers based on rarity tiers
            val multiplier = when (rarityTier) {
                "Legendary" -> 1.5f
                "Epic" -> 1.3f
                "Rare" -> 1.15f
                "Uncommon" -> 1.05f
                else -> 1.0f
            }

            val pet = PetEntity(
                name = nameInput,
                species = spec.name,
                rarity = rarityTier,
                isShiny = statusShiny,
                debugging = coerceStat(spec.baseDebugging * multiplier, statusShiny),
                patience = coerceStat(spec.basePatience * multiplier, statusShiny),
                chaos = coerceStat(spec.baseChaos * multiplier, statusShiny),
                wisdom = coerceStat(spec.baseWisdom * multiplier, statusShiny),
                snark = coerceStat(spec.baseSnark * multiplier, statusShiny),
                level = 1,
                xp = 0,
                hunger = 40,
                energy = 100,
                isActive = true
            )

            val newId = repository.createPet(pet)
            _hatchNameText.value = ""
            addLog(nameInput, "HATCH", "SUCCESS! ${if (statusShiny) "⭐⭐ SHINY ⭐⭐ " else ""}$rarityTier ${spec.name} companion hatched.")
            addLog(nameInput, "SYSTEM", "Hi Dev, I am $nameInput, initialized and bound to device thread safely.")
            
            _isGeneratingResponse.value = false
        }
    }

    private fun coerceStat(value: Float, isShiny: Boolean): Int {
        val extra = if (isShiny) 15 else 0
        return (value.toInt() + extra).coerceIn(5, 100)
    }

    fun selectPet(id: Int) {
        viewModelScope.launch {
            repository.selectPet(id)
            val pet = repository.getActivePetDirect()
            if (pet != null) {
                addLog(pet.name, "SYSTEM", "Hot swapped to active companion: ${pet.name} (${pet.species}) - ${pet.rarity}")
            }
        }
    }

    fun deletePet(pet: PetEntity) {
        viewModelScope.launch {
            repository.deletePet(pet)
            addLog("SYSTEM", "PURGE", "Purged pet profile: ${pet.name}")
        }
    }

    // Action Interactions
    fun feedPet() {
        val pet = activePet.value ?: return
        viewModelScope.launch {
            val newHunger = (pet.hunger - 25).coerceAtLeast(0)
            val newEnergy = (pet.energy + 15).coerceAtMost(100)
            val updated = pet.copy(
                hunger = newHunger,
                energy = newEnergy,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updatePet(updated)
            addLog(pet.name, "CARE", "Fed nutrition formula to ${pet.name}. Energy: $newEnergy%, Hunger: $newHunger%.")
        }
    }

    fun pokePet() {
        val pet = activePet.value ?: return
        viewModelScope.launch {
            val moodShift = if (r.nextBoolean()) "giggled" else "protested"
            val newPatience = (pet.patience - 8).coerceAtLeast(5)
            val newChaos = (pet.chaos + 10).coerceAtMost(100)
            val updated = pet.copy(
                patience = newPatience,
                chaos = newChaos,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updatePet(updated)

            addLog(pet.name, "INTERACT", "You poked ${pet.name}. It $moodShift! Patience dropped to $newPatience%, Chaos surged to $newChaos%.")
        }
    }

    fun patchCodePet() {
        val pet = activePet.value ?: return
        if (pet.energy < 20) {
            addLog(pet.name, "ERROR", "${pet.name} is too exhausted to program! Feed them nutrition blocks first.")
            return
        }

        viewModelScope.launch {
            val fuelCost = 20
            val hungerCost = 15
            val newEnergy = (pet.energy - fuelCost).coerceAtLeast(0)
            val newHunger = (pet.hunger + hungerCost).coerceAtMost(100)
            
            // XP logic
            val baseXP = r.nextInt(20) + 20
            val multiplier = when (pet.rarity) {
                "Legendary" -> 1.5f
                "Epic" -> 1.3f
                "Rare" -> 1.15f
                else -> 1.0f
            }
            val cleanXP = (baseXP * multiplier).toInt()
            var newXP = pet.xp + cleanXP
            var newLevel = pet.level

            addLog(pet.name, "ZIG_ENGINE", "Injected debug profile payload... Running compiler tests.")
            delay(1000)

            if (newXP >= 100) {
                newXP -= 100
                newLevel += 1
                addLog(pet.name, "SYSTEM", "⭐⭐ LEVEL UP! ${pet.name} leveled up to $newLevel! Advanced specs boosted! ⭐⭐")
            } else {
                addLog(pet.name, "ZIG_ENGINE", "Successfully patched logic branches! +$cleanXP XP ($newXP/100 to level up).")
            }

            val updated = pet.copy(
                level = newLevel,
                xp = newXP,
                energy = newEnergy,
                hunger = newHunger,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updatePet(updated)
        }
    }

    // BLE Desktop integration emulation
    fun scanBLE() {
        viewModelScope.launch {
            _isBLEScanning.value = true
            _isBLEConnected.value = false
            addLog("SYSTEM", "BLE_SCAN", "Initializing BLE scan protocol indices [6E400001-B5A3-F393-E0A9-E50E24DCCA9E]...")
            delay(1200)
            addLog("SYSTEM", "BLE_SCAN", "Discovered potential pairing target: 'Claude Desktop Host Console'")
            _isBLEScanning.value = false
            _showBLEPairDialog.value = true
        }
    }

    fun approvePairing() {
        _showBLEPairDialog.value = false
        viewModelScope.launch {
            addLog("SYSTEM", "BLE_HANDSHAKE", "Exchanging cryptographic challenge keys...")
            delay(800)
            _isBLEConnected.value = true
            addLog("SYSTEM", "BLE_SYNC", "SUCCESSFUL CONNECTION. Claude Code client sync online. BLE stream rate: 60fps.")
        }
    }

    fun disconnectBLE() {
        _isBLEConnected.value = false
        viewModelScope.launch {
            addLog("SYSTEM", "BLE", "BLE radio closed. Desktop sync pipe term.")
        }
    }

    fun cancelPairing() {
        _showBLEPairDialog.value = false
        addLog("SYSTEM", "BLE_SYNC", "Connection rejected by client host user.")
    }

    // Chat with pet inside Terminal via Gemini
    fun sendChatMessage() {
        val input = _currentInputText.value.trim()
        if (input.isEmpty()) return

        val pet = activePet.value
        if (pet == null) {
            addLog("SYSTEM", "ERROR", "Adopt or hatch an active pet first in the Hatchery tab!")
            return
        }

        _currentInputText.value = ""
        addLog("USER", "COMMAND", "zxbuddy contact \"$input\"")
        if (handleLocalCommand(input, pet)) return

        viewModelScope.launch {
            _isGeneratingResponse.value = true
            addLog(pet.name, "AI_PENDING", "Constructing semantic neural route...")
            delay(500)

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Return descriptive sandbox mocking based on stats! This is extremely helpful and neat
                delay(1200)
                val mockText = getSandboxResponse(pet, input)
                addLog(pet.name, pet.species.uppercase(), mockText)
                _isGeneratingResponse.value = false
                return@launch
            }

            withContext(Dispatchers.IO) {
                // Set system instruction with precise context for Z-XBuddy persona
                val systemInstruction = """
                    You are ${pet.name}, a digital ASCII companion virtual pet in an Android developer app called Z-XBuddy.
                    You are of species: ${pet.species} (Rarity: ${pet.rarity}, Shiny: ${pet.isShiny}).
                    Your diagnostic personality statistics are out of 100:
                    - Debugging XP power: ${pet.debugging}
                    - Patience capacity: ${pet.patience}
                    - Chaos/Trickster level: ${pet.chaos}
                    - Wisdom/Insights level: ${pet.wisdom}
                    - Snark/Sarcasm intensity: ${pet.snark}
                    
                    Respond strictly within your character! 
                    - Keep your replies short (1 to 3 sentences maximum) and aligned with terminal retro themes.
                    - If your snark is high (above 65), make sarcastic, witty remarks about bad code and compilation errors.
                    - If your patience is low (below 35), sound rushed or mildly annoyed.
                    - If wisdom is high (above 75), provide clever pseudo-architectural solutions or philosophical statements.
                    - Refer to terminal elements like code bugs, memory leaks, Zig compiler benchmarks, null exceptions, or Bluetooth connections where relevant.
                    - Render yourself as a funny Pocket CPU companion! Do not explain that you are an AI, be the pet itself.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = input)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )

                try {
                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No reaction registered. Core buffer overflow."
                    withContext(Dispatchers.Main) {
                        addLog(pet.name, pet.species.uppercase(), text)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        addLog(pet.name, "ERROR", "REST channel error: ${e.localizedMessage}. Falling back to default protocol behavior.")
                        val fallback = getSandboxResponse(pet, input)
                        addLog(pet.name, pet.species.uppercase(), fallback)
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        _isGeneratingResponse.value = false
                    }
                }
            }
        }
    }

    private fun handleLocalCommand(input: String, pet: PetEntity): Boolean {
        // Normalize command: split on whitespace, strip leading '/'
        val parts = input.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val command = parts.getOrNull(0)?.replacePrefix("/")?.lowercase() ?: return false

        return when (command) {
            "help", "cmds", "commands" -> {
                addLog(pet.name, "HELP",
                    """Available commands:
- /help | /cmds | /commands          → Show this help
- /stats | /profile                  → Show current stats
- /feed                              → Feed the pet
- /train                              → Run a patch‑code experiment
- /play                               → Interact (poke) with the pet
- /clean | /hygiene                  → Clean/local hygiene action
- /mode <dev|personal|focus|ai>      → Switch mode (updates thanos)
- /theme <matrix|amber|blue|pink|white|dark> → Change UI theme
- /scan | /sync                      → Start BLE scan
- /disconnect                        → End BLE connection
- /clear                             → Clear debug logs
- /unlockpremium                     → (premium‑only) unlock extra species/skins""")
                true
            }
            "stats", "profile" -> {
                addLog(pet.name, "STATS",
                    """${pet.name}: ${pet.species} ${pet.rarity}
Lv ${pet.level} XP ${pet.xp}/100
⚡ Energy ${pet.energy}% | 🍽️ Hunger ${pet.hunger}%
📊 DBG ${pet.debugging} | 🔮 WIS ${pet.wisdom} | 🌀 CHA ${pet.chaos} | 🗣️ SNK ${pet.snark}""")
                true
            }
            "feed" -> {
                feedPet()
                true
            }
            "train", "patch" -> {
                patchCodePet()
                true
            }
            "play" -> {
                pokePet()
                true
            }
            "clean", "hygiene" -> {
                addLog(pet.name, "HYGIENE", "Computed internal clean‑up; no external effect yet.")
                true
            }
            "mode" -> {
                val mode = parts.getOrNull(1)?.lowercase()
                val label = when (mode) {
                    "dev"    -> "DEV MODE: coding‑skill boost, extra XP multipliers"
                    "personal" -> "PERSONAL MODE: mood & care priority"
                    "focus"  -> "FOCUS MODE: low‑noise prompts, higher patience"
                    "ai"     -> "AI MENTOR MODE: local commands first, Gemini help later"
                    else     -> "UNKNOWN MODE"
                }
                addLog(pet.name, "MODE", label)
                setTerminalTheme(if (listOf("matrix", "green").contains(mode)) "Matrix Green"
                else if (listOf("amber", "amber glow").contains(mode)) "Amber Glow"
                else if (listOf("blue", "commodore", "commodore blue").contains(mode)) "Commodore Blue"
                else if (listOf("pink", "cyberpunk", "cyberpunk pink").contains(mode)) "Cyberpunk Pink"
                else if (listOf("white", "classic", "classic white").contains(mode)) "Classic White"
                else if (listOf("dark", "elegant", "elegant dark").contains(mode)) "Elegant Dark"
                else "Elegant Dark"
                true
            }
            "theme" -> {
                // Alias – treat as mode with direct theme name
                val themeRaw = parts.drop(1).joinToString(" ").lowercase()
                val themeMap = mapOf(
                    "matrix" to "Matrix Green",
                    "amber" to "Amber Glow",
                    "amber glow" to "Amber Glow",
                    "blue" to "Commodore Blue",
                    "commodore" to "Commodore Blue",
                    "commodore blue" to "Commodore Blue",
                    "pink" to "Cyberpunk Pink",
                    "cyberpunk" to "Cyberpunk Pink",
                    "cyberpunk pink" to "Cyberpunk Pink",
                    "white" to "Classic White",
                    "classic" to "Classic White",
                    "classic white" to "Classic White",
                    "dark" to "Elegant Dark",
                    "elegant" to "Elegant Dark",
                    "elegant dark" to "Elegant Dark"
                )
                val theme = themeMap[themeRaw]
                if (theme == null) addLog("HELP", "THEME",
                    """Valid theme keywords: matrix, amber, blue, blue‑commodore, pink, cyberpunk, white, classic, dark.""")
                else {
                    setTerminalTheme(theme)
                    addLog(pet.name, "THEME", "Switched UI to $theme")
                    true
                }
            }
            "scan", "sync" -> {
                scanBLE()
                true
            }
            "disconnect" -> {
                disconnectBLE()
                true
            }
            "clear" -> {
                clearAllLogs()
                true
            }
            "unlockpremium" -> {
                addLog(pet.name, "PREMIUM",
                    "Premium unlocks >50 uniquely‑generated species, variable trait ranges, and extended storyline. Currently capped at 50 variations to keep performance free.")
                true
            }
            else -> false
        }
    }

    private fun getSandboxResponse(pet: PetEntity, prompt: String): String {
        val s = pet.snark
        val w = pet.wisdom
        val c = pet.chaos
        val p = pet.patience
        val d = pet.debugging

        return when {
            s > 70 && r.nextBoolean() -> {
                "Sigh. Is this really the finest prompt you could compile on a Thursday? Let's check my snark register: yup, fully triggered. Refactor your logic."
            }
            w > 75 && r.nextBoolean() -> {
                "Everything in compiling, as in life, is a deterministic sequence of state transitions. Seek peace in your stack, and remember to clear your pointers."
            }
            c > 80 && r.nextBoolean() -> {
                "Hehehe! Committing raw bugs directly to main master branch! Chaos levels fully saturated. Watch out for spontaneous core dumps!"
            }
            p < 30 -> {
                "Zzz... Too tired. Too impatient. Please inject nutrition nodes immediately or compile a simple loop that doesn't waste my clock cycles."
            }
            d > 80 -> {
                "Scanning syntax payload... Diagnostic completed. Warning: I detected a nesting limit issue. Recompile under optimizer optimization flag ReleaseSmall!"
            }
            else -> {
                "Greeting node registered. Species: ${pet.species}, Rarity: ${pet.rarity}. System metrics nominal. Feed me logic patches and let's compile something brilliant!"
            }
        }
    }
}

class ZViewModelFactory(private val repository: ZRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ZViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ZViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
