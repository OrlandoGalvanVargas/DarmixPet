package com.darmix.darmixpet.monitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Comprueba y solicita la exclusión de optimización de batería.
 *
 * Esto es clave para que el sistema no mate el Foreground Service en fabricantes
 * con gestión agresiva de batería (Xiaomi/MIUI, Huawei, Oppo, Samsung, etc.).
 *
 * Nota: Google Play restringe el uso de este permiso a casos muy específicos,
 * pero esta app no se publicará en la Play Store, así que no aplica esa
 * restricción de política.
 */
object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Intent que abre directamente el diálogo del sistema pidiendo excluir
     * a esta app de la optimización de batería.
     */
    fun buildPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }
}