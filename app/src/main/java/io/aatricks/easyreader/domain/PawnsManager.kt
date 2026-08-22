package io.aatricks.easyreader.domain

import android.content.Context
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.repository.pawns.PawnsRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain manager for Pawns bandwidth sharing.
 * Coordinates between UI, repository, and lifecycle events.
 */
@Singleton
class PawnsManager @Inject constructor(
    private val appConfig: AppConfig,
    private val repository: PawnsRepository,
) {
    val isSharing: StateFlow<Boolean> = repository.isSharing
    val earningsToday: StateFlow<Double> = repository.earningsToday
    val totalEarnings: StateFlow<Double> = repository.totalEarnings
    val consentGiven: StateFlow<Boolean> = repository.consentGiven

    val isAvailable: Boolean
        get() = appConfig.isPawnsEnabled && appConfig.pawnsApiKey.isNotBlank()

    fun initialize(context: Context) = repository.initialize(context)

    fun refreshConsent() = repository.refreshConsentState()

    suspend fun start(context: Context): Boolean {
        if (!isAvailable) return false
        if (!consentGiven.value) return false
        return repository.startSharing(context)
    }

    suspend fun stop(context: Context) = repository.stopSharing(context)

    fun setConsent(given: Boolean) = repository.setConsentGiven(given)

    fun getConsentIntent(context: Context) = repository.getConsentIntent(context)

    suspend fun refreshEarnings() = repository.refreshEarnings()

    suspend fun prune() = repository.pruneOldEarnings()
}
