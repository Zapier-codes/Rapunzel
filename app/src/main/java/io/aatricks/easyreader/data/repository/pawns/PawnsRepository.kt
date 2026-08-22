package io.aatricks.easyreader.data.repository.pawns

import android.content.Context
import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.local.PawnsEarningsDao
import io.aatricks.easyreader.data.local.PawnsEarningsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository wrapping the Pawns SDK bandwidth-sharing functionality.
 *
 * All SDK calls are wrapped in try/catch so the app never crashes if the SDK
 * is unavailable or the API key is invalid. Feature flag gates everything.
 */
@Singleton
class PawnsRepository @Inject constructor(
    private val appConfig: AppConfig,
    private val earningsDao: PawnsEarningsDao,
) {
    private val tag = "PawnsRepository"

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _earningsToday = MutableStateFlow(0.0)
    val earningsToday: StateFlow<Double> = _earningsToday.asStateFlow()

    private val _totalEarnings = MutableStateFlow(0.0)
    val totalEarnings: StateFlow<Double> = _totalEarnings.asStateFlow()

    private val _consentGiven = MutableStateFlow(false)
    val consentGiven: StateFlow<Boolean> = _consentGiven.asStateFlow()

    val recentEarnings: Flow<List<PawnsEarningsEntity>> = earningsDao.getRecentEarnings()

    /** Initialize Pawns SDK in Application.onCreate(). Safe to call even if disabled. */
    fun initialize(context: Context) {
        if (!appConfig.isPawnsEnabled) {
            Log.d(tag, "Pawns SDK disabled by feature flag")
            return
        }
        if (appConfig.pawnsApiKey.isBlank()) {
            Log.w(tag, "Pawns SDK enabled but API key is blank")
            return
        }
        try {
            val pawnsClass = Class.forName("app.pawns.sdk.Pawns")
            val builderClass = Class.forName("app.pawns.sdk.Pawns\$Builder")
            val serviceTypeClass = Class.forName("app.pawns.sdk.ServiceType")
            val foreground = serviceTypeClass.getField("FOREGROUND").get(null)

            val builder = builderClass.getConstructor(Context::class.java)
                .newInstance(context)
            builderClass.getMethod("apiKey", String::class.java)
                .invoke(builder, appConfig.pawnsApiKey)
            builderClass.getMethod("serviceType", serviceTypeClass)
                .invoke(builder, foreground)
            builderClass.getMethod("build").invoke(builder)

            Log.i(tag, "Pawns SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Pawns SDK: ${e.message}")
        }
    }

    /** Check if user has given consent. */
    fun refreshConsentState() {
        if (!appConfig.isPawnsEnabled) return
        try {
            val pawns = Class.forName("app.pawns.sdk.Pawns")
            val instance = pawns.getMethod("getInstance").invoke(null)
            val given = instance.javaClass.getMethod("isConsentGiven")
                .invoke(instance) as? Boolean ?: false
            _consentGiven.value = given
        } catch (e: Exception) {
            Log.e(tag, "Failed to check consent: ${e.message}")
        }
    }

    /** Start bandwidth sharing. Returns true if started successfully. */
    suspend fun startSharing(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!appConfig.isPawnsEnabled) {
            Log.d(tag, "Cannot start: Pawns disabled by feature flag")
            return@withContext false
        }
        try {
            val pawns = Class.forName("app.pawns.sdk.Pawns")
            val instance = pawns.getMethod("getInstance").invoke(null)
            instance.javaClass.getMethod("startSharing", Context::class.java)
                .invoke(instance, context)
            _isSharing.value = true
            Log.i(tag, "Bandwidth sharing started")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start sharing: ${e.message}")
            false
        }
    }

    /** Stop bandwidth sharing. */
    suspend fun stopSharing(context: Context) = withContext(Dispatchers.IO) {
        if (!appConfig.isPawnsEnabled) return@withContext
        try {
            val pawns = Class.forName("app.pawns.sdk.Pawns")
            val instance = pawns.getMethod("getInstance").invoke(null)
            instance.javaClass.getMethod("stopSharing", Context::class.java)
                .invoke(instance, context)
            _isSharing.value = false
            Log.i(tag, "Bandwidth sharing stopped")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop sharing: ${e.message}")
        }
    }

    /** Set user consent (true = agreed, false = withdrawn). */
    fun setConsentGiven(given: Boolean) {
        if (!appConfig.isPawnsEnabled) return
        try {
            val pawns = Class.forName("app.pawns.sdk.Pawns")
            val instance = pawns.getMethod("getInstance").invoke(null)
            instance.javaClass.getMethod("setConsentGiven", Boolean::class.java)
                .invoke(instance, given)
            _consentGiven.value = given
        } catch (e: Exception) {
            Log.e(tag, "Failed to set consent: ${e.message}")
        }
    }

    /** Get the SDK consent intent for ActivityResultLauncher. */
    fun getConsentIntent(context: Context): android.content.Intent? {
        if (!appConfig.isPawnsEnabled) return null
        return try {
            val pawns = Class.forName("app.pawns.sdk.Pawns")
            val instance = pawns.getMethod("getInstance").invoke(null)
            instance.javaClass.getMethod("getConsentIntent")
                .invoke(instance) as? android.content.Intent
        } catch (e: Exception) {
            Log.e(tag, "Failed to get consent intent: ${e.message}")
            null
        }
    }

    /** Refresh earnings from local DB. */
    suspend fun refreshEarnings() = withContext(Dispatchers.IO) {
        val total = earningsDao.getTotalEarningsUsd() ?: 0.0
        _totalEarnings.value = total
        val today = DateTimeFormatter.ISO_LOCAL_DATE.format(
            Instant.now().atZone(ZoneId.systemDefault())
        )
        val todayEntity = earningsDao.getEarningsForDate(today)
        _earningsToday.value = todayEntity?.estimatedEarningsUsd ?: 0.0
    }

    /** Record estimated earnings for today. Called by worker. */
    suspend fun recordEarnings(bytesShared: Long, earningsUsd: Double) {
        val today = DateTimeFormatter.ISO_LOCAL_DATE.format(
            Instant.now().atZone(ZoneId.systemDefault())
        )
        val existing = earningsDao.getEarningsForDate(today)
        val updated = existing?.copy(
            bytesShared = existing.bytesShared + bytesShared,
            estimatedEarningsUsd = existing.estimatedEarningsUsd + earningsUsd,
            sessionMinutes = existing.sessionMinutes + 1,
            updatedAt = Instant.now().epochSecond
        ) ?: PawnsEarningsEntity(
            date = today,
            bytesShared = bytesShared,
            estimatedEarningsUsd = earningsUsd,
            sessionMinutes = 1
        )
        earningsDao.insertOrUpdate(updated)
        refreshEarnings()
    }

    /** Prune earnings older than 90 days. */
    suspend fun pruneOldEarnings() {
        val cutoff = DateTimeFormatter.ISO_LOCAL_DATE.format(
            Instant.now().minusSeconds(90 * 24 * 3600).atZone(ZoneId.systemDefault())
        )
        earningsDao.pruneOldEarnings(cutoff)
    }
}
