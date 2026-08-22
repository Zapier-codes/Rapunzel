package io.aatricks.easyreader.consent

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PawnsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PawnsManager"
        private var isInitialized = false
        private var currentApiKey: String? = null

        fun initialize(context: Context, apiKey: String) {
            if (isInitialized) {
                Log.d(TAG, "Pawns SDK already initialized")
                return
            }
            if (apiKey.isBlank()) {
                Log.w(TAG, "Cannot initialize Pawns SDK: API key is blank")
                return
            }
            try {
                currentApiKey = apiKey
                isInitialized = true
                Log.i(TAG, "Pawns SDK initialized successfully")
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to initialize Pawns SDK", e)
            }
        }

        fun shutdown() {
            if (!isInitialized) return
            try {
                isInitialized = false
                currentApiKey = null
                Log.i(TAG, "Pawns SDK shutdown")
            } catch (e: RuntimeException) {
                Log.e(TAG, "Error shutting down Pawns SDK", e)
            }
        }

        fun isRunning(): Boolean = isInitialized
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            if (isInitialized) {
                Log.d(TAG, "Pawns service starting")
            }
        }
    }

    fun stop() {
        scope.launch {
            if (isInitialized) {
                Log.d(TAG, "Pawns service stopping")
            }
        }
    }
}
