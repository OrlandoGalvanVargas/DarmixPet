package com.darmix.darmixpet.overlay

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.darmix.darmixpet.MainActivity
import kotlin.math.abs
import kotlin.math.roundToInt

class DeviceControls(private val context: Context) {

    companion object {
        private const val TAG = "DeviceControls"
        private const val TIMER_APP_PACKAGE = "com.darmix.tiempomio"
        private const val WARNING_COOLDOWN_MS = 4000L

        private val CLOCK_PACKAGES = listOf(
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock",
            "com.coloros.alarmclock",
            "com.oneplus.deskclock",
            "com.huawei.deskclock",
            "com.hihonor.deskclock",
            "com.android.BBKClock"
        )
    }

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var lastWarningAt = 0L

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
        if (max <= 0) return 0
        val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current * 100f / max).roundToInt().coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int) {
        val manager = audioManager
        if (manager.isVolumeFixed) {
            warn("Este teléfono no deja cambiar el volumen desde otras apps")
            return
        }
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return
        val target = (percent.coerceIn(0, 100) * max / 100f).roundToInt().coerceIn(0, max)
        val before = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (before == target) return

        runCatching { manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
        if (manager.getStreamVolume(AudioManager.STREAM_MUSIC) == target) return

        stepVolumeTowards(manager, target)
        val after = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (after == before) {
            runCatching {
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            warn("Tu teléfono no dejó cambiar el volumen desde aquí. Usa los botones de volumen.")
        }
    }

    private fun stepVolumeTowards(manager: AudioManager, target: Int) {
        var current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        var attempts = abs(target - current) + 2
        while (current != target && attempts > 0) {
            val direction = if (target > current) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            val ok = runCatching {
                manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
            }.isSuccess
            val next = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (!ok || next == current) return
            current = next
            attempts--
        }
    }

    fun openTimerApp() {
        val pm = context.packageManager
        val ownTimer = pm.getLaunchIntentForPackage(TIMER_APP_PACKAGE)
        if (ownTimer != null && tryStart(ownTimer)) return

        if (tryStart(Intent(AlarmClock.ACTION_SHOW_TIMERS))) return
        if (tryStart(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_SKIP_UI, false))) return

        for (pkg in CLOCK_PACKAGES) {
            val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
            if (tryStart(launch)) return
        }

        if (tryStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))) return

        warn("No encontré una app de reloj en tu teléfono")
    }

    fun openDarmixPet() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        tryStart(intent)
    }

    private fun tryStart(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir ${intent.action ?: intent.`package`}", e)
            false
        }
    }

    private fun warn(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastWarningAt < WARNING_COOLDOWN_MS) return
        lastWarningAt = now
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}