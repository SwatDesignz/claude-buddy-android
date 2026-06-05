package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ── Request DTOs ──────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String, // "system" or "user" or "assistant"
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String = "openai/gpt-4o-mini",
    val messages: List<OpenRouterMessage>,
    @param:Json(name = "max_tokens")
    val maxTokens: Int = 256,
    val temperature: Double = 0.7
)

// ── Response DTOs ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val message: OpenRouterMessage? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterUsage(
    @param:Json(name = "prompt_tokens")
    val promptTokens: Int = 0,
    @param:Json(name = "completion_tokens")
    val completionTokens: Int = 0,
    @param:Json(name = "total_tokens")
    val totalTokens: Int = 0
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice>? = null,
    val usage: OpenRouterUsage? = null,
    val error: OpenRouterError? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    val message: String? = null,
    val code: Int? = null
)

// ── API Service ───────────────────────────────────────────────────────────────

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

// ── Retrofit Singleton ────────────────────────────────────────────────────────

object OpenRouterRetrofitClient {
    private const val BASE_URL = "https://openrouter.ai/"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    val service: OpenRouterApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(OpenRouterApiService::class.java)
    }
}
