package io.aatricks.easyreader.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.aatricks.easyreader.data.repository.pawns.PawnsRepository

/**
 * Periodic worker that records Pawns earnings and prunes old data.
 * Runs every 15 minutes when bandwidth sharing is active.
 */
@HiltWorker
class PawnsBandwidthWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PawnsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Record a session minute (actual earnings come from SDK dashboard)
        // In production, this would query the SDK for real traffic stats
        repository.recordEarnings(
            bytesShared = 0, // Placeholder: SDK does not expose per-session bytes
            earningsUsd = 0.0 // Placeholder: real earnings fetched from partner dashboard API
        )
        repository.pruneOldEarnings()
        return Result.success()
    }
}
