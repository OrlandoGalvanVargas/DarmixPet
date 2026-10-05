package com.darmix.darmixpet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp


@Composable
fun DotGridBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val dot = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    Box(
        modifier = modifier
            .background(bg)
            .drawBehind {
                val step = 26.dp.toPx()
                val radius = 1.5.dp.toPx()
                val cols = (size.width / step).toInt() + 1
                val rows = (size.height / step).toInt() + 1
                for (row in 0..rows) {
                    for (col in 0..cols) {
                        drawCircle(dot, radius, Offset(col * step + step / 2, row * step + step / 2))
                    }
                }
            },
        content = content
    )
}
