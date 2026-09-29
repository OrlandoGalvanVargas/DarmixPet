package com.darmix.darmixpet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps")
    fun getAllMonitoredApps(): Flow<List<MonitoredAppEntity>>

    // Lectura puntual (no reactiva) de todas las apps monitoreadas,
    // usada por el loop de reinicio diario proactivo.
    @Query("SELECT * FROM monitored_apps")
    suspend fun getAllMonitoredAppsOnce(): List<MonitoredAppEntity>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): MonitoredAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: MonitoredAppEntity)

    @Update
    suspend fun update(app: MonitoredAppEntity)

    @Delete
    suspend fun delete(app: MonitoredAppEntity)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}