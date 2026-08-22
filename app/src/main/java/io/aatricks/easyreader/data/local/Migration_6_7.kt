package io.aatricks.easyreader.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS pawns_earnings (
                date TEXT PRIMARY KEY NOT NULL,
                bytesShared INTEGER NOT NULL DEFAULT 0,
                estimatedEarningsUsd REAL NOT NULL DEFAULT 0.0,
                sessionMinutes INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }
}
