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

/** Represents the state of the Simon Says memory game. */
sealed class SimonGame {
    data object Idle : SimonGame()
    data class ShowingSequence(
        val sequence: List<Int>,       // color indices 0-3
        val highlightIndex: Int,       // currently lit up index (-1 = all done)
        val round: Int
    ) : SimonGame()
    data class WaitingInput(
        val sequence: List<Int>,
        val playerInput: List<Int>,
        val round: Int
    ) : SimonGame()
    data class GameOver(
        val score: Int,                // how many correct inputs
        val roundsCompleted: Int,
        val pointsAwarded: Int
    ) : SimonGame()
}

/** Represents a wild pet encounter. */
sealed class WildEncounter {
    data object None : WildEncounter()
    data object Hunting : WildEncounter()
    data class Found(
        val speciesName: String,
        val rarity: String,
        val isShiny: Boolean,
        val asciiArt: String,
        val captureDifficulty: Float // 0.0 (impossible) to 1.0 (guaranteed) base
    ) : WildEncounter()
    data object Captured : WildEncounter()
    data object Escaped : WildEncounter()
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

    // Wild encounter state
    private val _wildEncounter = MutableStateFlow<WildEncounter>(WildEncounter.None)
    val wildEncounter: StateFlow<WildEncounter> = _wildEncounter.asStateFlow()

    private val _wildEncounterLog = MutableStateFlow("")
    val wildEncounterLog: StateFlow<String> = _wildEncounterLog.asStateFlow()

    // How many wild pets captured this session
    private val _wildCaptures = MutableStateFlow(0)
    val wildCaptures: StateFlow<Int> = _wildCaptures.asStateFlow()

    // Simon Says game state
    private val _simonGame = MutableStateFlow<SimonGame>(SimonGame.Idle)
    val simonGame: StateFlow<SimonGame> = _simonGame.asStateFlow()

    // Highest Simon score this session
    private val _simonHighScore = MutableStateFlow(0)
    val simonHighScore: StateFlow<Int> = _simonHighScore.asStateFlow()

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

    // ── Wild Encounters ──────────────────────────────────────────────────
    fun startWildEncounter() {
        val pet = activePet.value
        if (pet == null) {
            addLog("SYSTEM", "ERROR", "No active pet to go hunting with! Hatch one first.")
            return
        }
        if (_wildEncounter.value is WildEncounter.Hunting || _wildEncounter.value is WildEncounter.Found) {
            addLog(pet.name, "WILD", "Already tracking a wild encounter! Deal with it first.")
            return
        }
        viewModelScope.launch {
            _wildEncounter.value = WildEncounter.Hunting
            _wildEncounterLog.value = ""
            addLog(pet.name, "WILD", "🌿 ${pet.name} is sniffing around for wild pets...")
            delay(1500)

            // Pick a random species
            val speciesList = SpeciesData.list
            val spec = speciesList[r.nextInt(speciesList.size)]

            // Roll rarity with wild-encounter odds (more weighted towards common)
            val roll = r.nextInt(100) + 1
            val shinyRoll = r.nextInt(256) == 42 // ~0.4% base shiny in wild
            val rarity = when {
                roll >= 99 -> "Legendary"
                roll >= 93 -> "Epic"
                roll >= 75 -> "Rare"
                roll >= 45 -> "Uncommon"
                else -> "Common"
            }

            // Capture difficulty based on rarity
            val difficulty = when (rarity) {
                "Legendary" -> 0.15f
                "Epic" -> 0.30f
                "Rare" -> 0.50f
                "Uncommon" -> 0.70f
                else -> 0.90f
            }

            // Boost difficulty if shiny
            val finalDifficulty = if (shinyRoll) difficulty * 0.6f else difficulty

            // Get species art (use Baby frame1 for wild display)
            val art = spec.frame1.ifEmpty { spec.name.uppercase() }

            _wildEncounter.value = WildEncounter.Found(
                speciesName = spec.name,
                rarity = rarity,
                isShiny = shinyRoll,
                asciiArt = art,
                captureDifficulty = finalDifficulty.coerceIn(0.05f, 0.98f)
            )
            addLog(pet.name, "WILD",
                "🔥 Wild ${spec.name} appeared! Rarity: ${rarity}${if (shinyRoll) " ⭐⭐ SHINY ⭐⭐" else ""}"
            )
            val shinyTag = if (shinyRoll) " [SHINY!]" else ""
            _wildEncounterLog.value = "A wild ${spec.name} appeared!\nRarity: $rarity$shinyTag\nUse /catch to attempt capture!"
        }
    }

    fun attemptCapture() {
        val pet = activePet.value ?: return
        val encounter = _wildEncounter.value
        if (encounter !is WildEncounter.Found) {
            addLog(pet.name, "WILD", "Nothing to catch. Use /hunt first!")
            return
        }
        viewModelScope.launch {
            addLog(pet.name, "WILD", "🎯 ${pet.name} attempts to capture the wild ${encounter.speciesName}...")
            _wildEncounterLog.value = "Attempting capture..."
            delay(1200)

            // Capture formula: base difficulty adjusted by pet stats
            val statBonus = (pet.wisdom * 0.002f + pet.debugging * 0.0015f + pet.patience * 0.001f)
            val captureChance = (encounter.captureDifficulty + statBonus).coerceIn(0.05f, 0.99f)
            val roll = r.nextFloat()

            if (roll <= captureChance) {
                _wildCaptures.value += 1
                _wildEncounter.value = WildEncounter.Captured
                val xpBonus = when (encounter.rarity) {
                    "Legendary" -> 50
                    "Epic" -> 35
                    "Rare" -> 25
                    "Uncommon" -> 15
                    else -> 10
                }
                val zxBonus = when (encounter.rarity) {
                    "Legendary" -> 100
                    "Epic" -> 60
                    "Rare" -> 40
                    "Uncommon" -> 20
                    else -> 10
                }
                val shinyMultiplier = if (encounter.isShiny) 3 else 1
                val finalXP = xpBonus * shinyMultiplier
                val finalZX = zxBonus * shinyMultiplier
                _zxPoints.value += finalZX
                // Award XP directly
                val newXP = pet.xp + finalXP
                var newLevel = pet.level
                var xpCarry = newXP
                var levelUps = 0
                while (xpCarry >= 100) {
                    xpCarry -= 100
                    newLevel += 1
                    levelUps++
                }
                val updated = pet.copy(
                    level = newLevel,
                    xp = xpCarry,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.updatePet(updated)

                val shinyTag = if (encounter.isShiny) "⭐⭐ SHINY ⭐⭐ " else ""
                addLog(pet.name, "WILD",
                    "✅ ${shinyTag}Captured wild ${encounter.speciesName}! +${finalXP}XP +${finalZX}ZX!"
                )
                if (levelUps > 0) {
                    addLog(pet.name, "WILD", "⭐⭐ Level up ×$levelUps! ${pet.name} is now level $newLevel! ⭐⭐")
                }
                _wildEncounterLog.value = "✅ Captured! +${finalXP}XP +${finalZX}ZX Points"
            } else {
                _wildEncounter.value = WildEncounter.Escaped
                addLog(pet.name, "WILD",
                    "💨 The wild ${encounter.speciesName} escaped! (Rolled ${(roll*100).toInt()}% vs ${(captureChance*100).toInt()}% chance)"
                )
                _wildEncounterLog.value = "💨 The wild ${encounter.speciesName} escaped!\nTry a better approach next time."
            }
        }
    }

    fun fleeWildEncounter() {
        val encounter = _wildEncounter.value
        if (encounter is WildEncounter.Found || encounter is WildEncounter.Hunting) {
            _wildEncounter.value = WildEncounter.None
            _wildEncounterLog.value = ""
            val petName = activePet.value?.name ?: "SYSTEM"
            addLog(petName, "WILD", "👋 Retreating from the wild encounter. Better luck next time.")
        }
    }

    // ── Simon Says Memory Game ───────────────────────────────────────────
    fun startSimonGame() {
        val pet = activePet.value
        if (pet == null) {
            addLog("SYSTEM", "ERROR", "No active pet to play with! Hatch one first.")
            return
        }
        if (_simonGame.value !is SimonGame.Idle && _simonGame.value !is SimonGame.GameOver) {
            addLog(pet.name, "GAME", "A game is already in progress! Finish it first.")
            return
        }
        viewModelScope.launch {
            val round = 1
            val sequence = generateSimonSequence(round)
            addLog(pet.name, "GAME", "🎮 Simon Says! Watch the sequence carefully...")
            _simonGame.value = SimonGame.ShowingSequence(sequence = sequence, highlightIndex = -1, round = round)
            playSimonSequence(sequence)
        }
    }

    fun handleSimonInput(colorIndex: Int) {
        val state = _simonGame.value
        if (state !is SimonGame.WaitingInput) return
        val newInput = state.playerInput + colorIndex
        val expectedIndex = newInput.lastIndex

        // Check if the input matches the sequence so far
        if (newInput[expectedIndex] != state.sequence[expectedIndex]) {
            // Wrong! Game over
            val score = newInput.size - 1 // how many they got right before the mistake
            val pet = activePet.value
            val pointsAwarded = score * 5 + 10
            _zxPoints.value += pointsAwarded
            if (score > _simonHighScore.value) {
                _simonHighScore.value = score
            }
            _simonGame.value = SimonGame.GameOver(
                score = score,
                roundsCompleted = state.round - 1,
                pointsAwarded = pointsAwarded
            )
            val petName = pet?.name ?: "PLAYER"
            addLog(petName, "GAME",
                "💥 Wrong! Score: $score | Rounds: ${state.round - 1} | +${pointsAwarded}ZX"
            )
            if (pet != null) {
                val moodMsg = when {
                    pet.snark > 70 -> "Seriously? That was the easy part!"
                    pet.wisdom > 70 -> "Pattern recognition is a pathway to many abilities..."
                    pet.chaos > 70 -> "HAHA you failed! That sequence was CHAOTIC!"
                    else -> "Good try! Let's play again soon."
                }
                addLog(petName, "GAME", "${pet.name}: $moodMsg")
            }
            return
        }

        // Check if they completed the full sequence for this round
        if (newInput.size == state.sequence.size) {
            // Round complete! Next round
            val pet = activePet.value
            val nextRound = state.round + 1
            val nextSequence = generateSimonSequence(nextRound, state.sequence)
            addLog(pet?.name ?: "PLAYER", "GAME", "✅ Round $nextRound! Watch the new sequence...")
            _simonGame.value = SimonGame.ShowingSequence(
                sequence = nextSequence,
                highlightIndex = -1,
                round = nextRound
            )
            viewModelScope.launch { playSimonSequence(nextSequence) }
        } else {
            // Correct so far, wait for next input
            _simonGame.value = state.copy(playerInput = newInput)
        }
    }

    fun resetSimonGame() {
        _simonGame.value = SimonGame.Idle
    }

    private fun generateSimonSequence(round: Int, existing: List<Int> = emptyList()): List<Int> {
        val seq = existing.toMutableList()
        // Add one random step for each round (ensuring round 1 has 2 steps, round 2 has 3, etc.)
        while (seq.size < round + 1) {
            seq.add(r.nextInt(4)) // 0-3 for four colors
        }
        return seq
    }

    private suspend fun playSimonSequence(sequence: List<Int>) {
        for (i in sequence.indices) {
            _simonGame.value = SimonGame.ShowingSequence(
                sequence = sequence,
                highlightIndex = i,
                round = (_simonGame.value as? SimonGame.ShowingSequence)?.round ?: 1
            )
            // Pet visual reaction depends on color index
            val pet = activePet.value
            val colorNames = arrayOf("RED", "BLUE", "GREEN", "YELLOW")
            if (pet != null) {
                addLog(pet.name, "SIMON", "${colorNames[sequence[i]]}!")
            }
            delay(600)
            // Clear highlight briefly
            _simonGame.value = SimonGame.ShowingSequence(
                sequence = sequence,
                highlightIndex = -1,
                round = (_simonGame.value as? SimonGame.ShowingSequence)?.round ?: 1
            )
            delay(200)
        }
        // Switch to waiting for player input
        _simonGame.value = SimonGame.WaitingInput(
            sequence = sequence,
            playerInput = emptyList(),
            round = (_simonGame.value as? SimonGame.ShowingSequence)?.round ?: 1
        )
        val pet = activePet.value
        addLog(pet?.name ?: "PLAYER", "SIMON", "🎯 Your turn! Tap the colors in order.")
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
	- /hunt | /wild                      → Search for a wild pet encounter
	- /catch | /capture                  → Attempt to capture the wild pet
	- /flee | /run                       → Retreat from wild encounter
	- /simon | /memory                     → Play Simon Says memory game
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
            "hunt", "wild" -> {
                startWildEncounter()
                true
            }
            "catch", "capture" -> {
                attemptCapture()
                true
            }
            "flee", "run" -> {
                fleeWildEncounter()
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
            "simon", "memory", "game" -> {
                startSimonGame()
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
