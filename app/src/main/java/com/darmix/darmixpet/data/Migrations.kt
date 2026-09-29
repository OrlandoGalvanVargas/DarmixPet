package com.darmix.darmixpet.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Historial de migraciones reales de Room. A partir de aquí, cualquier cambio
 * de esquema debe agregarse como una migración nueva (MIGRATION_X_Y) en vez
 * de depender de fallbackToDestructiveMigration, para no perder los datos
 * de configuración del usuario (apps monitoreadas, progreso del día, etc.).
 */

// v5 -> v6: se elimina la columna contextualWarningGiven (el aviso contextual
// de noche/batería baja dejó de ser por-app y pasó a ser un estado global de
// la mascota, manejado directamente en PetOverlayService sin tocar Room).
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