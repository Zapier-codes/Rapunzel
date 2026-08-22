package io.aatricks.easyreader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Tracks Pawns SDK bandwidth-sharing earnings per session.
 * One row per day; updated throughout the day.
 */
@Entity(tableName = "pawns_earnings")
data class PawnsEarningsEntity(
    @PrimaryKey
    val date: String, // ISO-8601 date (YYYY-MM-DD)
    val bytesShared: Long = 0,
    val estimatedEarningsUsd: Double = 0.0,
    val sessionMinutes: Int = 0,
    val updatedAt: Long = Instant.now().epochSecond
)
