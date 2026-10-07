package com.darmix.darmixpet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val sessionLimitMinutes: Int,
    val cooldownMinutes: Int,
    val maxSessionsPerDay: Int,
    val sessionsUsedToday: Int = 0,
    val usedInSessionMillis: Long = 0L,
    val halfwayAlertGiven: Boolean = false,
    val lastUsageResetTimestamp: Long = 0L,
    val lastBlockedTimestamp: Long = 0L
)