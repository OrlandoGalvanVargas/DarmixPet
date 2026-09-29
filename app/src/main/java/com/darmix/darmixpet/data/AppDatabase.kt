package com.darmix.darmixpet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MonitoredAppEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun monitoredAppDao(): MonitoredAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "darmixpet_db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Red de seguridad SOLO para saltos de versión no cubiertos
                    // por una migración explícita (por ejemplo, instalaciones muy
                    // antiguas). Cualquier cambio de esquema nuevo debe agregar
                    // su propia Migration en Migrations.kt, no depender de esto.
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}