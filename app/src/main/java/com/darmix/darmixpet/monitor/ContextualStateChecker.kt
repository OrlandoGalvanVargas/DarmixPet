package com.darmix.darmixpet.monitor

import android.content.Context
import android.media.AudioManager
import android.os.BatteryManager
import java.util.Calendar

object ContextualStateChecker {

    private const val NIGHT_HOUR_START = 23
    private const val NIGHT_HOUR_END = 6
    private const val LOW_BATTERY_THRESHOLD = 20

    fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= NIGHT_HOUR_START || hour < NIGHT_HOUR_END
    }

    fun batteryPercent(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isBatteryLow(context: Context): Boolean {
        val level = batteryPercent(context)
        return level in 0..LOW_BATTERY_THRESHOLD
    }

    fun isAudioPlaying(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMusicActive
    }
}
