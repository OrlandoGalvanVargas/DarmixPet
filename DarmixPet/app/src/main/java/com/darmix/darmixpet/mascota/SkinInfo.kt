package com.darmix.darmixpet.mascota

import androidx.annotation.DrawableRes

data class SkinInfo(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val description: String,
    val accentColor: Long,
    val theme: MascotThemeSpec,
    @DrawableRes val idleRes: Int,
    @DrawableRes val tapSimpleRes: Int,
    @DrawableRes val tapDobleRes: Int,
    @DrawableRes val tripleTapRes: Int,
    @DrawableRes val nightRes: Int,
    @DrawableRes val lowBatteryRes: Int,
    @DrawableRes val listeningRes: Int,
    @DrawableRes val thinkingRes: Int
)