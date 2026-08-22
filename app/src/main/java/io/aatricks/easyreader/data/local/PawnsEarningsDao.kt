package io.aatricks.easyreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PawnsEarningsDao {
    @Query("SELECT * FROM pawns_earnings ORDER BY date DESC LIMIT 30")
    fun getRecentEarnings(): Flow<List<PawnsEarningsEntity>>

    @Query("SELECT * FROM pawns_earnings WHERE date = :date")
    suspend fun getEarningsForDate(date: String): PawnsEarningsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(earnings: PawnsEarningsEntity)

    @Query("SELECT SUM(bytesShared) FROM pawns_earnings")
    suspend fun getTotalBytesShared(): Long?

    @Query("SELECT SUM(estimatedEarningsUsd) FROM pawns_earnings")
    suspend fun getTotalEarningsUsd(): Double?

    @Query("DELETE FROM pawns_earnings WHERE date < :cutoffDate")
    suspend fun pruneOldEarnings(cutoffDate: String)
}
