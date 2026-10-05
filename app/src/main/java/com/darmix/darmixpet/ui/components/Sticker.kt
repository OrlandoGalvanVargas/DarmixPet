package com.darmix.darmixpet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.theme.DarmixTheme


@Composable
fun Modifier.sticker(
    shape: Shape = MaterialTheme.shapes.large,
    fill: Color = MaterialTheme.colorScheme.surface,
    depth: Dp = 3.dp,
    borderWidth: Dp = 2.dp,
    border: Color = DarmixTheme.colors.ink,
    shadow: Color = DarmixTheme.colors.shadow
): Modifier = this
    .drawBehind {
        val d = depth.toPx()
        if (d > 0.01f) {
            val outline = shape.createOutline(size, layoutDirection, this)
            translate(d, d) { drawOutline(outline, shadow) }
        }
    }
    .background(fill, shape)
    .border(borderWidth, border, shape)
