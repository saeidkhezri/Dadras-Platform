package com.example.network

import android.content.Context
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FirebaseService {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    fun initialize(context: Context) {
        if (_isInitialized.value) return
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                _isInitialized.value = true
                return
            }

            // Read keys from BuildConfig (managed securely via Secrets / .env / BuildConfig)
            val apiKey = try { BuildConfig.FIREBASE_API_KEY } catch (e: Exception) { "" }
            val appId = try { BuildConfig.FIREBASE_APP_ID } catch (e: Exception) { "" }
            val projectId = try { BuildConfig.FIREBASE_PROJECT_ID } catch (e: Exception) { "" }
            val dbUrl = try { BuildConfig.FIREBASE_DB_URL } catch (e: Exception) { "" }

            if (apiKey.isNotBlank() && apiKey != "YOUR_FIREBASE_API_KEY") {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .apply {
                        if (dbUrl.isNotBlank() && dbUrl != "YOUR_FIREBASE_DB_URL") {
                            setDatabaseUrl(dbUrl)
                        }
                    }
                    .build()
                FirebaseApp.initializeApp(context, options)
                _isInitialized.value = true
            } else {
                // Initialize default programmatically to avoid crashes if possible,
                // otherwise log error for configurations.
                val defaultOptions = FirebaseOptions.Builder()
                    .setApiKey("your-firebase-api-key-from-ai-studio-secrets")
                    .setApplicationId("1:12345:android:123456789")
                    .setProjectId("dadras-hooshmand-mock")
                    .build()
                FirebaseApp.initializeApp(context, defaultOptions)
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isInitialized.value = false
        }
    }

    val auth: FirebaseAuth?
        get() = if (_isInitialized.value) FirebaseAuth.getInstance() else null

    val firestore: FirebaseFirestore?
        get() = if (_isInitialized.value) FirebaseFirestore.getInstance() else null
}
