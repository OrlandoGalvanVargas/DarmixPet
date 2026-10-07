package com.darmix.darmixpet.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE monitored_apps_new (
                packageName TEXT NOT NULL PRIMARY KEY,
                appName TEXT NOT NULL,
                sessionLimitMinutes INTEGER NOT NULL,
                cooldownMinutes INTEGER NOT NULL,
                maxSessionsPerDay INTEGER NOT NULL,
                sessionsUsedToday INTEGER NOT NULL,
                usedInSessionMillis INTEGER NOT NULL,
                halfwayAlertGiven INTEGER NOT NULL,
                lastUsageResetTimestamp INTEGER NOT NULL,
                lastBlockedTimestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO monitored_apps_new (
                packageName, appName, sessionLimitMinutes, cooldownMinutes,
                maxSessionsPerDay, sessionsUsedToday, usedInSessionMillis,
                halfwayAlertGiven, lastUsageResetTimestamp, lastBlockedTimestamp
            )
            SELECT
                packageName, appName, sessionLimitMinutes, cooldownMinutes,
                maxSessionsPerDay, sessionsUsedToday, usedInSessionMillis,
                halfwayAlertGiven, lastUsageResetTimestamp, lastBlockedTimestamp
            FROM monitored_apps
            """.trimIndent()
        )
        db.execSQL("DROP TABLE monitored_apps")
        db.execSQL("ALTER TABLE monitored_apps_new RENAME TO monitored_apps")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_5_6)