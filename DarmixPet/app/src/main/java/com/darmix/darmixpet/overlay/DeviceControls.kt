package com.darmix.darmixpet.overlay

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.darmix.darmixpet.MainActivity

class DeviceControls(private val context: Context) {

    companion object {
        private const val TAG = "DeviceControls"
        private const val TIMER_APP_PACKAGE = "com.darmix.tiempomio"
    }

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun brightnessPercent(): Int {
        return try {
            val value = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            (value * 100 / 255).coerceIn(0, 100)
        } catch (e: Settings.SettingNotFoundException) {
            50
        }
    }

    fun setBrightnessPercent(percent: Int) {
        if (!Settings.System.canWrite(context)) {
            Toast.makeText(context, "Activa el permiso para modificar ajustes del sistema", Toast.LENGTH_SHORT).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
            return
        }
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val value = (percent * 255 / 100).coerceIn(0, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        } catch (e: SecurityException) {
            Log.w(TAG, "No se pudo cambiar el brillo", e)
        }
    }

    fun volumePercent(): Int {
        val manager = audioManager
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max == 0) return 0
        val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current * 100 / max).coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int) {
        val manager = audioManager
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val value = (percent * max / 100).coerceIn(0, max)
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
    }

    fun openTimerApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(TIMER_APP_PACKAGE)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return
        }
        try {
            val systemTimerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(systemTimerIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No se encontró ninguna app de temporizador", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDarmixPet() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }
}