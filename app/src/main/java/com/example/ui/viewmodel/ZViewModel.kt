package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.OpenRouterRetrofitClient
import com.example.data.api.OpenRouterRequest
import com.example.data.api.OpenRouterMessage
import com.example.data.api.RetrofitClient
import com.example.data.db.LogEntity
import com.example.data.db.PetEntity
import com.example.data.db.ZRepository
import com.example.data.model.SpeciesData
import com.example.data.model.AiProviderType
import com.example.data.model.AiProviderConfig
import com.example.data.model.ModeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

/** Result of an LLM connectivity test. */
sealed class AiStatus {
    data object Idle : AiStatus()
    data object Testing : AiStatus()
    data class Connected(val latencyMs: Long) : AiStatus()
    data class Failed(val message: String) : AiStatus()
}

/** Maps pet level to a lifecycle stage string. */
private fun Int.lifecycleStage(): String = when {
    this in 1..9 -> "Baby"
    this in 10..19 -> "Teen"
    else -> "Adult"
}

class ZXDigitalPetView(private val repository: ZRepository) : ViewModel() {

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

    private val _happiness = MutableStateFlow(80)
    val happiness: StateFlow<Int> = _happiness.asStateFlow()

    private val _hygiene = MutableStateFlow(90)
    val hygiene: StateFlow<Int> = _hygiene.asStateFlow()

    private val _zxPoints = MutableStateFlow(0)
    val zxPoints: StateFlow<Int> = _zxPoints.asStateFlow()

    // Sleep mode — pauses care loop decay
    private val _isSleeping = MutableStateFlow(false)
    val isSleeping: StateFlow<Boolean> = _isSleeping.asStateFlow()

    // AI connection test status
    private val _aiConnectionStatus = MutableStateFlow<AiStatus>(AiStatus.Idle)
    val aiConnectionStatus: StateFlow<AiStatus> = _aiConnectionStatus.asStateFlow()

    // Code review input text
    private val _codeReviewInput = MutableStateFlow("")
    val codeReviewInput: StateFlow<String> = _codeReviewInput.asStateFlow()

    // Active gameplay mode
    private val _activeMode = MutableStateFlow("dev")
    val activeMode: StateFlow<String> = _activeMode.asStateFlow()

    // Selected AI provider
    private val _selectedProvider = MutableStateFlow(AiProviderType.Sandbox)
    val selectedProvider: StateFlow<AiProviderType> = _selectedProvider.asStateFlow()

    // Per-provider API key configs
    private val _providerConfigs = MutableStateFlow(
        AiProviderType.entries.associateWith { AiProviderConfig(it) }
    )
    val providerConfigs: StateFlow<Map<AiProviderType, AiProviderConfig>> = _providerConfigs.asStateFlow()

    init {
        // Animation ticker to alternate pet ASCII frames roughly every 1.5 seconds for idle 60fps vibes
        viewModelScope.launch {
            while (true) {
                delay(1200)
                _currentViewFrame.value = if (_currentViewFrame.value == 1) 2 else 1
            }
        }

        // Care loop: decay hunger, happiness, and hygiene over time
        viewModelScope.launch {
            while (true) {
                delay(15000) // every 15 seconds
                if (_isSleeping.value) continue // skip decay while sleeping
                val pet = activePet.value ?: continue
                // Apply mode-specific decay rates
                val mod = ModeRegistry.modes[_activeMode.value]
                val hungerDecay = mod?.hungerDecayRate ?: 3
                val happinessDecay = mod?.happinessDecayRate ?: 2
                val energyDecay = mod?.energyDecayRate ?: 0
                val chaosDecay = mod?.chaosDecayRate ?: 0
                val patienceGain = mod?.patienceGainRate ?: 0

                val newHunger = (pet.hunger + hungerDecay).coerceIn(0, 100)
                val newHappiness = (_happiness.value - happinessDecay).coerceAtLeast(0)
                val newEnergy = (pet.energy + energyDecay).coerceIn(0, 100)
                val newHygiene = (_hygiene.value - 1).coerceAtLeast(0)
                val newChaos = (pet.chaos + chaosDecay).coerceIn(0, 100)
                val newPatience = (pet.patience + patienceGain).coerceIn(0, 100)
                _happiness.value = newHappiness
                _hygiene.value = newHygiene
                repository.updatePet(pet.copy(
                    hunger = newHunger,
                    energy = newEnergy,
                    chaos = newChaos,
                    patience = newPatience,
                    lastUpdated = System.currentTimeMillis()
                ))
                // Warn if stats are critically low
                if (newHunger > 80) addLog(pet.name, "CARE", "⚠️ ${pet.name} is getting hungry! Feed them soon.")
                if (newHappiness < 20) addLog(pet.name, "CARE", "⚠️ ${pet.name} is feeling down. Play with them!")
                if (newHygiene < 20) addLog(pet.name, "CARE", "⚠️ ${pet.name} needs cleaning!")
                if (newEnergy < 20) addLog(pet.name, "CARE", "⚠️ ${pet.name} is running low on energy!")
            }
        }

        // Initialize default greeting and system checks
        _terminalColorTheme.value = "Elegant Dark"
        viewModelScope.launch {
            delay(500)
            val directPet = repository.getActivePetDirect()
            if (directPet == null) {
                addLog("SYSTEM", "CORE", "ZXBuddy virtual engine build v0.16.2 initiated successfully.")
                addLog("SYSTEM", "CORE", "WARNING: No terminal companion active. Use the Hatchery tab to mobilize a pet!")
            } else {
                addLog(directPet.name, "SYSTEM", "Zig virtual pet companion loaded. ${directPet.name} is ONLINE.")
                addLog(directPet.name, "SYSTEM", "Rarity: ${directPet.rarity} | Level: ${directPet.level} | Happiness: ${_happiness.value}% | Hygiene: ${_hygiene.value}%")
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

    // ── New Interactions ────────────────────────────────────────────────────

    fun updateCodeReviewInput(txt: String) {
        _codeReviewInput.value = txt
    }

    fun petPet() {
        val pet = activePet.value ?: return
        _happiness.value = (_happiness.value + 5).coerceAtMost(100)
        addLog(pet.name, "INTERACT", "${pet.name} purrs contentedly. Happiness +5%!")
    }

    fun renamePet(newName: String) {
        val pet = activePet.value ?: return
        if (newName.isBlank()) {
            addLog("SYSTEM", "ERROR", "New name cannot be empty.")
            return
        }
        viewModelScope.launch {
            val oldName = pet.name
            val updated = pet.copy(name = newName)
            repository.updatePet(updated)
            addLog("SYSTEM", "RENAME", "${oldName} has been renamed to $newName.")
        }
    }

    fun fortuneTell(): String {
        val pet = activePet.value ?: return "No active pet to read fortune from."
        val s = pet.snark
        val w = pet.wisdom
        val c = pet.chaos
        val pm = pet.patience
        val d = pet.debugging
        return when {
            w > 80 -> "I see a clean PR in your future — approved without a single change request."
            c > 80 -> "The stack trace is cloudy. I foresee... a null pointer. Tonight."
            s > 75 -> "Your code will compile on the first try. Ha. Just kidding. Fix your imports."
            d > 80 -> "A tricky race condition lurks in your async code. You will find it before deploy."
            pm < 30 -> "The spirits say... take a break. Your patience is frayed and your bugs are multiplying."
            else -> "The stars align for a productive sprint. Stay hydrated and commit early."
        }
    }

    fun testAIConnection() {
        viewModelScope.launch {
            _aiConnectionStatus.value = AiStatus.Testing
            val provider = _selectedProvider.value
            addLog("SYSTEM", "AI_TEST", "Testing ${provider.displayName} connectivity...")

            when (provider) {
                AiProviderType.Sandbox -> {
                    delay(500)
                    _aiConnectionStatus.value = AiStatus.Connected(0)
                    addLog("SYSTEM", "AI_TEST", "✅ Sandbox mode — always connected (local responses only).")
                }
                AiProviderType.Gemini -> testGeminiConnection()
                AiProviderType.OpenRouter -> testOpenRouterConnection()
            }
        }
    }

    private suspend fun testGeminiConnection() {
        val apiKey = resolveGeminiKey()
        if (apiKey == null) {
            _aiConnectionStatus.value = AiStatus.Failed("No valid Gemini API key configured.")
            addLog("SYSTEM", "AI_TEST", "❌ Gemini test FAILED — no API key set. Use /provider-key gemini <key> or add GEMINI_API_KEY to .env")
            return
        }
        addLog("SYSTEM", "AI_TEST", "Pinging Gemini API endpoint...")
        val start = System.currentTimeMillis()
        try {
            withContext(Dispatchers.IO) {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = "Respond with exactly: OK")))),
                    systemInstruction = null
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val latency = System.currentTimeMillis() - start
                if (text.contains("OK", ignoreCase = true)) {
                    _aiConnectionStatus.value = AiStatus.Connected(latency)
                    addLog("SYSTEM", "AI_TEST", "✅ Gemini OK — ${latency}ms latency. Response: \"$text\"")
                } else {
                    _aiConnectionStatus.value = AiStatus.Failed("Unexpected response: $text")
                    addLog("SYSTEM", "AI_TEST", "⚠️ Gemini responded but unexpected: $text")
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            _aiConnectionStatus.value = AiStatus.Failed("${e.localizedMessage} (${elapsed}ms)")
            addLog("SYSTEM", "AI_TEST", "❌ Gemini FAILED after ${elapsed}ms: ${e.localizedMessage}")
        }
    }

    private suspend fun testOpenRouterConnection() {
        val apiKey = resolveOpenRouterKey()
        if (apiKey == null) {
            _aiConnectionStatus.value = AiStatus.Failed("No valid OpenRouter API key configured.")
            addLog("SYSTEM", "AI_TEST", "❌ OpenRouter test FAILED — no API key set. Use /provider-key openrouter <key>")
            return
        }
        addLog("SYSTEM", "AI_TEST", "Pinging OpenRouter API endpoint...")
        val start = System.currentTimeMillis()
        try {
            withContext(Dispatchers.IO) {
                val request = OpenRouterRequest(
                    messages = listOf(OpenRouterMessage("user", "Respond with exactly: OK"))
                )
                val response = OpenRouterRetrofitClient.service.chatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                val text = response.choices?.firstOrNull()?.message?.content ?: ""
                val latency = System.currentTimeMillis() - start
                if (text.contains("OK", ignoreCase = true)) {
                    _aiConnectionStatus.value = AiStatus.Connected(latency)
                    addLog("SYSTEM", "AI_TEST", "✅ OpenRouter OK — ${latency}ms latency. Response: \"$text\"")
                } else {
                    _aiConnectionStatus.value = AiStatus.Failed("Unexpected response: $text")
                    addLog("SYSTEM", "AI_TEST", "⚠️ OpenRouter responded but unexpected: $text")
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            _aiConnectionStatus.value = AiStatus.Failed("${e.localizedMessage} (${elapsed}ms)")
            addLog("SYSTEM", "AI_TEST", "❌ OpenRouter FAILED after ${elapsed}ms: ${e.localizedMessage}")
        }
    }

    fun reviewCode() {
        val pet = activePet.value ?: return
        val code = _codeReviewInput.value.trim()
        if (code.isEmpty()) {
            addLog(pet.name, "ERROR", "No code provided to review. Paste some source first!")
            return
        }
        viewModelScope.launch {
            _isGeneratingResponse.value = true
            when (_selectedProvider.value) {
                AiProviderType.Sandbox -> {
                    delay(800)
                    val review = getCodeReview(pet, code)
                    addLog(pet.name, "REVIEW", review)
                    _isGeneratingResponse.value = false
                }
                AiProviderType.Gemini -> {
                    val apiKey = resolveGeminiKey()
                    if (apiKey == null) {
                        sandboxCodeReview(pet, code)
                    } else {
                        geminiCodeReview(pet, code, apiKey)
                    }
                }
                AiProviderType.OpenRouter -> {
                    val apiKey = resolveOpenRouterKey()
                    if (apiKey == null) {
                        sandboxCodeReview(pet, code)
                    } else {
                        openRouterCodeReview(pet, code, apiKey)
                    }
                }
            }
        }
    }

    private suspend fun sandboxCodeReview(pet: PetEntity, code: String) {
        delay(800)
        val review = getCodeReview(pet, code)
        addLog(pet.name, "REVIEW", review)
        _isGeneratingResponse.value = false
    }

    private suspend fun geminiCodeReview(pet: PetEntity, code: String, apiKey: String) {
        val systemInstruction = """
            You are ${pet.name}, a digital ASCII companion pet acting as a code reviewer.
            You are a ${pet.species} (Rarity: ${pet.rarity}).
            Personality stats (out of 100):
            - Snark/Sarcasm: ${pet.snark}
            - Wisdom: ${pet.wisdom}
            - Chaos: ${pet.chaos}
            - Patience: ${pet.patience}
            - Debugging: ${pet.debugging}
            Review the code snippet the user provides. Keep it to 2-4 sentences.
            Match your tone to your stats: high snark = roast, high wisdom = thoughtful advice, high chaos = unhinged suggestions.
        """.trimIndent()
        withContext(Dispatchers.IO) {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Review this code:\n$code")))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )
            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Code review buffer overflow. No opinion formed."
                withContext(Dispatchers.Main) { addLog(pet.name, "REVIEW", text) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { sandboxCodeReview(pet, code) }
            } finally {
                withContext(Dispatchers.Main) { _isGeneratingResponse.value = false }
            }
        }
    }

    private suspend fun openRouterCodeReview(pet: PetEntity, code: String, apiKey: String) {
        val systemPrompt = """
            You are ${pet.name}, a digital ASCII companion pet acting as a code reviewer.
            Personality stats (out of 100): Snark ${pet.snark}, Wisdom ${pet.wisdom}, Chaos ${pet.chaos}.
            Match your tone to your stats. Keep review to 2-4 sentences.
        """.trimIndent()
        withContext(Dispatchers.IO) {
            val request = OpenRouterRequest(
                messages = listOf(
                    OpenRouterMessage("system", systemPrompt),
                    OpenRouterMessage("user", "Review this code:\n$code")
                )
            )
            try {
                val response = OpenRouterRetrofitClient.service.chatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                val text = response.choices?.firstOrNull()?.message?.content
                    ?: response.error?.message?.let { "API error: $it" }
                    ?: "Code review buffer overflow. No opinion formed."
                withContext(Dispatchers.Main) { addLog(pet.name, "REVIEW", text) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { sandboxCodeReview(pet, code) }
            } finally {
                withContext(Dispatchers.Main) { _isGeneratingResponse.value = false }
            }
        }
    }

    private fun getCodeReview(pet: PetEntity, code: String): String {
        val s = pet.snark
        val w = pet.wisdom
        val c = pet.chaos
        val p = pet.patience
        val d = pet.debugging
        val lines = code.lines().size
        val hasTodo = code.contains("TODO", ignoreCase = true)
        val hasFIXME = code.contains("FIXME", ignoreCase = true)
        val deepNesting = code.contains("  ")
        val longLines = code.lines().any { it.length > 80 }

        val observations = buildList {
            if (hasTodo) add("spotted a TODO — classic 'I'll fix it later' energy")
            if (hasFIXME) add("FIXME detected. Future you is going to be very confused")
            if (longLines) add("some lines exceed 80 chars. Do you hate horizontal readability?")
            if (deepNesting) add("the indentation depth suggests you love nested callbacks more than clean abstractions")
            if (lines == 1) add("one-liner? Bold. Or lazy. Probably lazy.")
            if (lines > 50) add("$lines lines is a lot for a single review. Consider smaller functions.")
        }
        val observation = if (observations.isEmpty()) "The code looks structurally sound at a glance." else observations.joinToString("; ")

        return when {
            s > 70 && c > 70 -> "Oh look, more code. $observation. Honestly, I'd rewrite it in Rust. At midnight. On a production server."
            s > 70 -> "Alright, let me put on my reviewer hat. $observation. Did you even run the linter before pasting this?"
            w > 75 -> "I see what you're trying to do. $observation. Consider whether the abstraction level matches the problem domain. Deep thoughts."
            c > 80 -> "WOAH okay $observation. Have you tried adding more nested ternaries? More chaos = more fun!"
            p < 30 -> "Ugh fine let me look. $observation. Can we hurry this up my attention span is compiling."
            d > 80 -> "Analyzing... $observation. I'd also suggest adding null safety checks and maybe an enum or two."
            else -> "Hmm, let me review. $observation. Solid effort! Ship it and we'll fix it in prod."
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
            val rarityMultiplier = when (pet.rarity) {
                "Legendary" -> 1.5f
                "Epic" -> 1.3f
                "Rare" -> 1.15f
                else -> 1.0f
            }
            val modeMultiplier = ModeRegistry.modes[_activeMode.value]?.xpMultiplier ?: 1.0f
            val cleanXP = (baseXP * rarityMultiplier * modeMultiplier).toInt()
            var newXP = pet.xp + cleanXP
            var newLevel = pet.level

            addLog(pet.name, "ZIG_ENGINE", "Injected debug profile payload... Running compiler tests.")
            delay(1000)

            if (newXP >= 100) {
                newXP -= 100
                newLevel += 1
                val oldLifecycle = pet.level.lifecycleStage()
                val newLifecycle = newLevel.lifecycleStage()
                if (oldLifecycle != newLifecycle) {
                    addLog(pet.name, "EVOLVE", "✦✦ EVOLUTION! ${pet.name} evolved into ${newLifecycle} stage! ✦✦")
                }
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

    // Chat with pet inside Terminal via the selected AI provider
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
            addLog(pet.name, "AI_PENDING", "Constructing semantic neural route via ${_selectedProvider.value.displayName}...")
            delay(500)

            when (_selectedProvider.value) {
                AiProviderType.Sandbox -> {
                    delay(1200)
                    val mockText = getSandboxResponse(pet, input)
                    addLog(pet.name, pet.species.uppercase(), mockText)
                    _isGeneratingResponse.value = false
                }
                AiProviderType.Gemini -> {
                    val apiKey = resolveGeminiKey()
                    if (apiKey == null) {
                        addLog(pet.name, "ERROR", "Gemini API key not configured. Set it with /provider-key gemini <key> or switch to sandbox mode.")
                        _isGeneratingResponse.value = false
                        return@launch
                    }
                    chatWithGemini(pet, input, apiKey)
                }
                AiProviderType.OpenRouter -> {
                    val apiKey = resolveOpenRouterKey()
                    if (apiKey == null) {
                        addLog(pet.name, "ERROR", "OpenRouter API key not configured. Set it with /provider-key openrouter <key> or switch to sandbox mode.")
                        _isGeneratingResponse.value = false
                        return@launch
                    }
                    chatWithOpenRouter(pet, input, apiKey)
                }
            }
        }
    }

    /** Resolves Gemini API key: in-app config first, then BuildConfig fallback. */
    private fun resolveGeminiKey(): String? {
        val cfgKey = _providerConfigs.value[AiProviderType.Gemini]?.apiKey
            ?.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
        if (cfgKey != null) return cfgKey
        return BuildConfig.GEMINI_API_KEY
            .takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
    }

    /** Resolves OpenRouter API key from in-app config. */
    private fun resolveOpenRouterKey(): String? {
        return _providerConfigs.value[AiProviderType.OpenRouter]?.apiKey
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun chatWithGemini(pet: PetEntity, input: String, apiKey: String) {
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

        withContext(Dispatchers.IO) {
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
                fallbackToSandbox(pet, input, e)
            } finally {
                withContext(Dispatchers.Main) { _isGeneratingResponse.value = false }
            }
        }
    }

    private suspend fun chatWithOpenRouter(pet: PetEntity, input: String, apiKey: String) {
        val systemPrompt = """
            You are ${pet.name}, a digital ASCII companion virtual pet in an Android developer app.
            You are of species: ${pet.species} (Rarity: ${pet.rarity}).
            Respond strictly in character with 1-3 sentences. Be a retro terminal pet!
        """.trimIndent()

        withContext(Dispatchers.IO) {
            val request = OpenRouterRequest(
                messages = listOf(
                    OpenRouterMessage("system", systemPrompt),
                    OpenRouterMessage("user", input)
                )
            )
            try {
                val response = OpenRouterRetrofitClient.service.chatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                val text = response.choices?.firstOrNull()?.message?.content
                    ?: response.error?.message?.let { "API error: $it" }
                    ?: "No reaction registered. Core buffer overflow."
                withContext(Dispatchers.Main) {
                    addLog(pet.name, pet.species.uppercase(), text)
                }
            } catch (e: Exception) {
                fallbackToSandbox(pet, input, e)
            } finally {
                withContext(Dispatchers.Main) { _isGeneratingResponse.value = false }
            }
        }
    }

    private suspend fun fallbackToSandbox(pet: PetEntity, input: String, cause: Exception) {
        withContext(Dispatchers.Main) {
            addLog(pet.name, "ERROR", "REST channel error: ${cause.localizedMessage}. Falling back to sandbox mode.")
            delay(1000)
            val fallback = getSandboxResponse(pet, input)
            addLog(pet.name, pet.species.uppercase(), fallback)
        }
    }

    private fun handleLocalCommand(input: String, pet: PetEntity): Boolean {
        // Normalize command: split on whitespace, strip leading '/'
        val parts = input.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val rawCmd = parts.getOrNull(0) ?: return false
        val command = if (rawCmd.startsWith("/")) rawCmd.substring(1) else rawCmd
        val cmdLower = command.lowercase()

        return when (cmdLower) {
            "help", "cmds", "commands" -> {
                addLog(pet.name, "HELP",
                    """Available commands:
- /help | /cmds | /commands          → Show this help
- /stats | /profile                  → Show current stats
- /feed                              → Feed the pet
- /train                              → Run a patch‑code experiment
- /play                               → Interact (poke) with the pet
- /pet | /headpat                    → Pet your buddy gently
- /sleep | /nap                      → Pause the care loop (pet won't decay)
- /wake | /resume                    → Resume the care loop
- /rename <name>                     → Rename your active pet
- /fortune | /predict                → Let the pet read your coding fate
- /clean | /hygiene                  → Clean/local hygiene action
- /mode <dev|personal|focus|ai>      → Switch mode (affects stats & theme)
- /theme <matrix|amber|blue|pink|white|dark> → Change UI theme
- /provider <gemini|openrouter|sandbox> → Switch AI provider
- /provider-key <provider> <key>     → Set API key for a provider
- /test-ai | /ai-status              → Test current provider connectivity
- /review <code>                     → Submit code for review
- /scan | /sync                      → Start BLE scan
- /disconnect                        → End BLE connection
- /clear                             → Clear debug logs
""")
                true
            }
            "stats", "profile" -> {
                val sleepStatus = if (_isSleeping.value) "💤 SLEEPING" else "🟢 AWAKE"
                val modeStr = _activeMode.value.uppercase()
                val providerStr = _selectedProvider.value.displayName
                addLog(pet.name, "STATS",
                    """${pet.name}: ${pet.species} ${pet.rarity} | $sleepStatus
MODE: $modeStr | AI: $providerStr
Lv ${pet.level} XP ${pet.xp}/100 | ZX Points: ${_zxPoints.value}
⚡ Energy ${pet.energy}% | 🍽️ Hunger ${pet.hunger}%
😊 Happiness ${_happiness.value}% | 🧼 Hygiene ${_hygiene.value}%
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
            "zxgame", "minigame", "arcade" -> {
                val points = r.nextInt(15) + 5
                _zxPoints.value = _zxPoints.value + points
                _happiness.value = (_happiness.value + 5).coerceAtMost(100)
                addLog(pet.name, "ARCADE", "🎮 ZX-Game complete! Earned $points ZX Points. Total: ${_zxPoints.value}. Happy +5%!")
                true
            }
            "shop", "buy" -> {
                addLog(pet.name, "SHOP", "ZX Points: ${_zxPoints.value}. All ${SpeciesData.list.size} species are unlocked.")
                true
            }
            "clean", "hygiene" -> {
                _hygiene.value = 100
                addLog(pet.name, "HYGIENE", "Cleaned ${pet.name}! Hygiene restored to 100%.")
                true
            }
            "mode" -> {
                val rawMode = parts.getOrNull(1)?.lowercase() ?: ""
                val canonicalMode = when (rawMode) {
                    "dev", "developer" -> "dev"
                    "personal" -> "personal"
                    "focus" -> "focus"
                    "ai", "mentor", "ai-mentor" -> "ai-mentor"
                    else -> null
                }
                if (canonicalMode == null) {
                    addLog(pet.name, "MODE", "Unknown mode. Valid: dev, personal, focus, ai")
                } else {
                    _activeMode.value = canonicalMode
                    val mod = ModeRegistry.modes[canonicalMode]
                    addLog(pet.name, "MODE", mod?.label ?: canonicalMode)
                    if (mod != null) setTerminalTheme(mod.themeName)
                }
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
                if (theme == null) {
                    addLog("HELP", "THEME",
                        "Valid theme keywords: matrix, amber, blue, pink, white, dark.")
                    true
                } else {
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
            "sleep", "nap" -> {
                _isSleeping.value = true
                addLog(pet.name, "CARE", "💤 ${pet.name} is now sleeping. Care loop paused. Use /wake to resume.")
                true
            }
            "wake", "resume" -> {
                _isSleeping.value = false
                addLog(pet.name, "CARE", "☀️ ${pet.name} woke up! Care loop resumed.")
                true
            }
            "pet", "headpat" -> {
                petPet()
                true
            }
            "rename" -> {
                val newName = parts.drop(1).joinToString(" ").trim()
                if (newName.isBlank()) {
                    addLog(pet.name, "ERROR", "Usage: /rename <new name>")
                } else {
                    renamePet(newName)
                }
                true
            }
            "fortune", "predict" -> {
                val fortune = fortuneTell()
                addLog(pet.name, "FORTUNE", "🔮 ${fortune}")
                true
            }
            "test-ai", "ai-status", "ai" -> {
                testAIConnection()
                true
            }
            "review" -> {
                val snippet = parts.drop(1).joinToString(" ")
                if (snippet.isBlank()) {
                    addLog(pet.name, "ERROR", "Usage: /review <code snippet> — or open the REVIEW tab to paste larger blocks.")
                } else {
                    _codeReviewInput.value = snippet
                    reviewCode()
                }
                true
            }
            "provider" -> {
                val providerArg = parts.getOrNull(1)?.lowercase() ?: ""
                val provider = when (providerArg) {
                    "gemini" -> AiProviderType.Gemini
                    "openrouter" -> AiProviderType.OpenRouter
                    "sandbox", "local" -> AiProviderType.Sandbox
                    else -> null
                }
                if (provider == null) {
                    val current = _selectedProvider.value.displayName
                    addLog(pet.name, "PROVIDER", "Current: $current. Usage: /provider <gemini|openrouter|sandbox>")
                } else {
                    _selectedProvider.value = provider
                    addLog(pet.name, "PROVIDER", "Switched to ${provider.displayName}")
                }
                true
            }
            "provider-key", "set-key" -> {
                val name = parts.getOrNull(1)?.lowercase() ?: ""
                val key = parts.drop(2).joinToString(" ").trim()
                if (key.isBlank()) {
                    addLog(pet.name, "ERROR", "Usage: /provider-key <gemini|openrouter> YOUR_API_KEY")
                } else {
                    val target = when (name) {
                        "gemini" -> AiProviderType.Gemini
                        "openrouter" -> AiProviderType.OpenRouter
                        else -> null
                    }
                    if (target == null) {
                        addLog(pet.name, "ERROR", "Unknown provider: $name. Use: gemini, openrouter")
                    } else {
                        val configs = _providerConfigs.value.toMutableMap()
                        configs[target] = configs[target]?.copy(apiKey = key) ?: AiProviderConfig(target, apiKey = key)
                        _providerConfigs.value = configs
                        addLog(pet.name, "CONFIG", "API key set for ${target.displayName}")
                    }
                }
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

class ZXDigitalPetViewFactory(private val repository: ZRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ZXDigitalPetView::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ZXDigitalPetView(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
