package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object KeylessAiHelper {
    suspend fun callKeylessPollinations(prompt: String, systemInstruction: String? = null, model: String? = null): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .addInterceptor(SecureStorageKeyInterceptor())
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()

        val cleanPrompt = prompt.trim()
        val mappedModel = when {
            model == null -> "openai"
            model.contains("GPT-4o", ignoreCase = true) -> "openai"
            model.contains("Claude", ignoreCase = true) -> "openai"
            model.contains("DeepSeek", ignoreCase = true) -> "llama"
            model.contains("Qwen", ignoreCase = true) -> "qwen"
            model.contains("Llama", ignoreCase = true) -> "llama"
            model.contains("Mistral", ignoreCase = true) -> "mistral"
            else -> "openai"
        }

        // --- LAYER 1: Standard, secure, unlimited size JSON POST (OpenAI compatible) ---
        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val messagesArray = org.json.JSONArray()
            
            if (!systemInstruction.isNullOrBlank()) {
                messagesArray.put(org.json.JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
            }
            messagesArray.put(org.json.JSONObject().apply {
                put("role", "user")
                put("content", cleanPrompt)
            })

            val jsonBody = org.json.JSONObject().apply {
                put("model", mappedModel)
                put("messages", messagesArray)
                put("jsonMode", false)
                put("temperature", 0.5)
            }

            val request = Request.Builder()
                .url("https://text.pollinations.ai/openai/chat/completions")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isNotBlank()) {
                        val jsonResponse = org.json.JSONObject(bodyString)
                        val choices = jsonResponse.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val message = choice.optJSONObject("message")
                            if (message != null && message.has("content")) {
                                val content = message.getString("content")
                                if (content.isNotBlank()) {
                                    return@withContext content
                                }
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            // Gracefully fall back to Layer 2
        }

        // --- LAYER 2: Query-based/Path segment fallback GET ---
        try {
            // If prompt is too long, we slice it to prevent HTTP 414 headers crash, or pass it directly
            val safePromptForPath = if (cleanPrompt.length > 300) cleanPrompt.take(300) + "..." else cleanPrompt
            
            // Build keyless GET url
            val httpUrlBuilder = "https://text.pollinations.ai/".toHttpUrl()
                .newBuilder()
                .addPathSegment(safePromptForPath)

            if (!systemInstruction.isNullOrBlank()) {
                httpUrlBuilder.addQueryParameter("system", systemInstruction)
            }
            httpUrlBuilder.addQueryParameter("model", mappedModel)
            httpUrlBuilder.addQueryParameter("jsonMode", "false")

            val request = Request.Builder()
                .url(httpUrlBuilder.build())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val resText = response.body?.string() ?: ""
                    if (resText.isNotBlank()) {
                        return@withContext resText
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        throw Exception("خطا در برقراری ارتباط با موتور هوش مصنوعی دادرس (موتور آزاد موقتاً در دسترس نیست)")
    }
}

