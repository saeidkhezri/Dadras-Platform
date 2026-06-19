package com.example.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class SecureStorageKeyInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url
        val host = url.host
        val builder = originalRequest.newBuilder()

        // Remove our custom tracking header and store its value
        val providerTag = originalRequest.header("X-AI-Provider")
        if (providerTag != null) {
            builder.removeHeader("X-AI-Provider")
        }

        // Retrieve live active keys synced from Admin/Citizen UI settings to AiOrchestrator
        val context = AiOrchestrator.appContext
        if (context != null && AiOrchestrator.adminGeminiKey.isBlank() && AiOrchestrator.adminOpenRouterKey.isBlank() && AiOrchestrator.adminOpenAiKey.isBlank()) {
            AiOrchestrator.loadKeysFromPrefs(context)
        }

        val geminiKey = AiOrchestrator.adminGeminiKey.ifBlank {
            AiOrchestrator.geminiKeysList.firstOrNull { it.isNotBlank() } ?: ""
        }
        val openRouterKey = AiOrchestrator.adminOpenRouterKey.ifBlank {
            AiOrchestrator.openrouterKeysList.firstOrNull { it.isNotBlank() } ?: ""
        }
        val openAiKey = AiOrchestrator.adminOpenAiKey.ifBlank {
            AiOrchestrator.openaiKeysList.firstOrNull { it.isNotBlank() } ?: ""
        }
        val groqKey = AiOrchestrator.groqKeysList.firstOrNull { it.isNotBlank() } ?: ""
        val cohereKey = AiOrchestrator.cohereKeysList.firstOrNull { it.isNotBlank() } ?: ""
        val huggingFaceKey = AiOrchestrator.huggingfaceKeysList.firstOrNull { it.isNotBlank() } ?: ""

        // Extract original token for validation
        val originalAuth = originalRequest.header("Authorization") ?: ""
        val originalToken = if (originalAuth.startsWith("Bearer ", ignoreCase = true)) {
            originalAuth.substring(7).trim()
        } else {
            originalAuth.trim()
        }
        val originalIsValidAndNotPlaceholder = originalToken.isNotBlank() && 
            !originalToken.contains("YOUR_") && 
            !originalToken.contains("MY_")

        // Determine effective provider using tag metadata first, fallback to hostname matching
        val effectiveProvider = providerTag ?: when {
            host.contains("openrouter") || url.toString().contains("openrouter") -> "openrouter"
            host.contains("openai") || url.toString().contains("openai") -> "openai"
            host.contains("groq") || url.toString().contains("groq") -> "groq"
            host.contains("cohere") || url.toString().contains("cohere") -> "cohere"
            host.contains("huggingface") || url.toString().contains("huggingface") -> "huggingface"
            host.contains("generativelanguage") || url.encodedPath.contains("generateContent") || host.contains("google") -> "gemini"
            else -> ""
        }

        if (effectiveProvider == "gemini") {
            val originalGeminiHeader = originalRequest.header("x-goog-api-key") ?: ""
            val originalGeminiParam = url.queryParameter("key") ?: ""
            val originalGeminiValid = (originalGeminiHeader.isNotBlank() && !originalGeminiHeader.contains("MY_")) ||
                (originalGeminiParam.isNotBlank() && !originalGeminiParam.contains("MY_"))

            if (!originalGeminiValid && geminiKey.isNotBlank() && geminiKey != "MY_GEMINI_API_KEY") {
                builder.header("x-goog-api-key", geminiKey)
                val newUrl = url.newBuilder()
                    .setQueryParameter("key", geminiKey)
                    .build()
                builder.url(newUrl)
            }
        } else {
            // For other API providers, if the original authorization header is blank or placeholder, override it
            if (!originalIsValidAndNotPlaceholder) {
                val keyToInject = when (effectiveProvider) {
                    "openrouter" -> openRouterKey
                    "openai" -> openAiKey
                    "groq" -> groqKey
                    "cohere" -> cohereKey
                    "huggingface" -> huggingFaceKey
                    else -> ""
                }
                if (keyToInject.isNotBlank() && !keyToInject.contains("YOUR_") && !keyToInject.contains("MY_")) {
                    builder.header("Authorization", "Bearer $keyToInject")
                }
            }
        }

        return chain.proceed(builder.build())
    }
}
