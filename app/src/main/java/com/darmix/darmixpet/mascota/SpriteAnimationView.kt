package com.darmix.darmixpet.mascota

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
fun SpriteAnimationView(
    skin: MascotaSkin,
    state: MascotaAnimState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spriteSheet = remember(skin, state) { skin.getAnimation(context, state) }
    var currentFrame by remember { mutableIntStateOf(0) }

    LaunchedEffect(spriteSheet) {
        while (true) {
            delay(spriteSheet.frameDurationMs)
            currentFrame = (currentFrame + 1) % spriteSheet.frames.size
        }
    }

    Image(
        bitmap = spriteSheet.frames[currentFrame].asImageBitmap(),
        contentDescription = skin.displayName,
        modifier = modifier
    )
}