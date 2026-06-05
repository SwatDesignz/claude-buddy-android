package com.example.data.model

/** Supported AI providers for chat responses. */
enum class AiProviderType(val displayName: String) {
    Sandbox("Sandbox (local)"),
    Gemini("Google Gemini"),
    OpenRouter("OpenRouter");
}

/** Per-provider configuration: API key and optional base URL. */
data class AiProviderConfig(
    val type: AiProviderType,
    val apiKey: String = "",
    val baseUrl: String = defaultBaseUrl(type)
) {
    companion object {
        fun defaultBaseUrl(type: AiProviderType): String = when (type) {
            AiProviderType.Gemini -> "https://generativelanguage.googleapis.com/"
            AiProviderType.OpenRouter -> "https://openrouter.ai/api/"
            AiProviderType.Sandbox -> ""
        }
    }
}

/** Gameplay modifiers applied per active mode. */
data class ModeModifiers(
    val label: String,
    val xpMultiplier: Float = 1.0f,
    val hungerDecayRate: Int = 3,
    val happinessDecayRate: Int = 2,
    val energyDecayRate: Int = 0,
    val chaosDecayRate: Int = 0,
    val patienceGainRate: Int = 0,
    val themeName: String = "Elegant Dark"
)

/** All available modes with their modifiers. */
object ModeRegistry {
    val modes = mapOf(
        "dev" to ModeModifiers(
            label = "DEV MODE: coding-skill boost, +50% XP",
            xpMultiplier = 1.5f,
            hungerDecayRate = 4,
            themeName = "Matrix Green"
        ),
        "personal" to ModeModifiers(
            label = "PERSONAL MODE: mood & care priority",
            hungerDecayRate = 1,
            happinessDecayRate = 1,
            themeName = "Amber Glow"
        ),
        "focus" to ModeModifiers(
            label = "FOCUS MODE: low-noise, patience recovers",
            hungerDecayRate = 2,
            chaosDecayRate = -2,
            patienceGainRate = 3,
            themeName = "Commodore Blue"
        ),
        "ai-mentor" to ModeModifiers(
            label = "AI MENTOR MODE: wisdom focus, energy-efficient",
            xpMultiplier = 0.8f,
            energyDecayRate = -1,
            themeName = "Elegant Dark"
        )
    )
}
