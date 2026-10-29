package com.darmix.darmixpet.ui.theme

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.darmix.darmixpet.mascota.SkinPreferences

@Composable
fun rememberSelectedSkinId(): State<String> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(SkinPreferences.getSelectedSkinId(context)) }
    DisposableEffect(context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SkinPreferences.KEY_SELECTED_SKIN) {
                state.value = SkinPreferences.getSelectedSkinId(context)
            }
        }
        SkinPreferences.registerListener(context, listener)
        onDispose { SkinPreferences.unregisterListener(context, listener) }
    }
    return state
}