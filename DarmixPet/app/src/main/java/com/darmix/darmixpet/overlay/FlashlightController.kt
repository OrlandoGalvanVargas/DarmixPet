package com.darmix.darmixpet.overlay

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper


class FlashlightController(context: Context) {

    enum class Result {
        ON,
        OFF,
        NO_FLASH,
        IN_USE
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = findTorchCameraId()

    @Volatile private var torchOn = false
    @Volatile private var turnedOnByUs = false
    private var callbackRegistered = false

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            torchOn = enabled
            if (!enabled) turnedOnByUs = false
        }
    }

    init {
        if (cameraId != null) {
            try {
                cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
                callbackRegistered = true
            } catch (e: Exception) {

            }
        }
    }

    fun toggle(): Result {
        val id = cameraId ?: return Result.NO_FLASH
        val target = !torchOn
        return try {
            cameraManager.setTorchMode(id, target)
            torchOn = target
            turnedOnByUs = target
            if (target) Result.ON else Result.OFF
        } catch (e: CameraAccessException) {
            Result.IN_USE
        } catch (e: IllegalArgumentException) {
            Result.NO_FLASH
        }
    }


    fun release() {
        val id = cameraId
        if (id != null && turnedOnByUs && torchOn) {
            try { cameraManager.setTorchMode(id, false) } catch (e: Exception) {  }
        }
        if (callbackRegistered) {
            try { cameraManager.unregisterTorchCallback(callback) } catch (e: Exception) { }
            callbackRegistered = false
        }
    }

    private fun findTorchCameraId(): String? = try {
        val ids = cameraManager.cameraIdList
        ids.firstOrNull { id ->
            val c = cameraManager.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: ids.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        null
    }
}
