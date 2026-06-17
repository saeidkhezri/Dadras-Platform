package com.example.network

import android.content.Context
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

        // 1. First read keys from SharedPreferences via context (completely secure and live persistent storage)
        val context = AiOrchestrator.appContext
        val prefs = context?.getSharedPreferences("admin_ai_keys", Context.MODE_PRIVATE)

        val geminiKey = if (prefs != null) {
            prefs.getString("gemini_key_1", "")?.ifBlank {
                prefs.getString("gemini_key_2", "")?.ifBlank {
                    prefs.getString("gemini_key_3", "")
                }
            } ?: ""
        } else {
            AiOrchestrator.adminGeminiKey
        }

        val openRouterKey = if (prefs != null) {
            prefs.getString("openrouter_key_1", "")?.ifBlank {
                prefs.getString("openrouter_key_2", "")?.ifBlank {
                    prefs.getString("openrouter_key_3", "")
                }
            } ?: ""
        } else {
            AiOrchestrator.adminOpenRouterKey
        }

        val openAiKey = if (prefs != null) {
            prefs.getString("openai_key_1", "")?.ifBlank {
                prefs.getString("openai_key_2", "")?.ifBlank {
                    prefs.getString("openai_key_3", "")
                }
            } ?: ""
        } else {
            AiOrchestrator.adminOpenAiKey
        }

        val groqKey = if (prefs != null) {
            prefs.getString("groq_key_1", "")?.ifBlank {
                prefs.getString("groq_key_2", "")?.ifBlank {
                    prefs.getString("groq_key_3", "")
                }
            } ?: ""
        } else {
            AiOrchestrator.groqKeysList.firstOrNull { it.isNotBlank() } ?: ""
        }

        val cohereKey = if (prefs != null) {
            prefs.getString("cohere_key_1", "")?.ifBlank {
                prefs.getString("cohere_key_2", "")?.ifBlank {
                    prefs.getString("cohere_key_3", "")
                }
            } ?: ""
        } else {
            AiOrchestrator.cohereKeysList.firstOrNull { it.isNotBlank() } ?: ""
        }

        val huggingFaceKey = if (prefs != null) {
            prefs.getString("huggingface_key_1", "")?.ifBlank {
                prefs.getString("huggingface_key_2", "")?.ifBlank {
                    prefs.getString("huggingface_key_3", "")
                }
            } ?: ""
        } else {
            AiOrchestrator.huggingfaceKeysList.firstOrNull { it.isNotBlank() } ?: ""
        }

        // 2. Identify host and inject/override credentials dynamically
        val originalAuth = originalRequest.header("Authorization") ?: ""
        val originalToken = if (originalAuth.startsWith("Bearer ", ignoreCase = true)) {
            originalAuth.substring(7).trim()
        } else {
            originalAuth.trim()
        }
        val originalIsValidAndNotPlaceholder = originalToken.isNotBlank() && 
            !originalToken.contains("YOUR_") && 
            !originalToken.contains("MY_")

        if (!originalIsValidAndNotPlaceholder) {
            when {
                // A. OpenRouter API
                host.contains("openrouter") || url.toString().contains("openrouter") -> {
                    val key = openRouterKey.ifBlank { AiOrchestrator.adminOpenRouterKey }
                    if (key.isNotBlank() && key != "YOUR_OPENROUTER_API_KEY") {
                        builder.header("Authorization", "Bearer $key")
                    }
                }
                // B. OpenAI API
                host.contains("openai") || url.toString().contains("openai") -> {
                    val key = openAiKey.ifBlank { AiOrchestrator.adminOpenAiKey }
                    if (key.isNotBlank() && key != "YOUR_OPENAI_API_KEY") {
                        builder.header("Authorization", "Bearer $key")
                    }
                }
                // C. Groq API
                host.contains("groq") || url.toString().contains("groq") -> {
                    val key = groqKey.ifBlank { AiOrchestrator.groqKeysList.firstOrNull { it.isNotBlank() } ?: "" }
                    if (key.isNotBlank()) {
                        builder.header("Authorization", "Bearer $key")
                    }
                }
                // D. Cohere API
                host.contains("cohere") || url.toString().contains("cohere") -> {
                    val key = cohereKey.ifBlank { AiOrchestrator.cohereKeysList.firstOrNull { it.isNotBlank() } ?: "" }
                    if (key.isNotBlank()) {
                        builder.header("Authorization", "Bearer $key")
                    }
                }
                // E. HuggingFace API
                host.contains("huggingface") || url.toString().contains("huggingface") -> {
                    val key = huggingFaceKey.ifBlank { AiOrchestrator.huggingfaceKeysList.firstOrNull { it.isNotBlank() } ?: "" }
                    if (key.isNotBlank()) {
                        builder.header("Authorization", "Bearer $key")
                    }
                }
            }
        }

        // F. Google Gemini API (Query parameter "key" and Header "x-goog-api-key")
        val originalGeminiHeader = originalRequest.header("x-goog-api-key") ?: ""
        val originalGeminiParam = url.queryParameter("key") ?: ""
        val originalGeminiValid = (originalGeminiHeader.isNotBlank() && !originalGeminiHeader.contains("MY_")) ||
            (originalGeminiParam.isNotBlank() && !originalGeminiParam.contains("MY_"))

        if (!originalGeminiValid && (host.contains("generativelanguage") || url.encodedPath.contains("generateContent") || host.contains("google"))) {
            val key = geminiKey.ifBlank { AiOrchestrator.adminGeminiKey }
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") {
                builder.header("x-goog-api-key", key)
                val newUrl = url.newBuilder()
                    .setQueryParameter("key", key)
                    .build()
                builder.url(newUrl)
            }
        }

        return chain.proceed(builder.build())
    }
}
