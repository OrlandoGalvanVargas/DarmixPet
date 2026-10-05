package com.darmix.darmixpet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons


@Composable
fun OverlayNoticeHelpCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .sticker(shape = MaterialTheme.shapes.large, fill = scheme.surface, depth = 3.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = if (expanded) "Ocultar explicación" else "Ver explicación"
            ) { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(DarmixIcons.Bell, null, tint = scheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Aviso «se muestra sobre otras apps»",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                DarmixIcons.ChevronRight, null, tint = scheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Android muestra ese aviso siempre que una app flota sobre otras, como Archi. " +
                        "Lo pone el sistema y la app no puede quitarlo. No afecta el funcionamiento.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Para ocultarlo: mantén presionado el aviso, entra a sus ajustes (⚙) y desactiva " +
                        "la notificación de DarmixPet «mostrándose sobre otras apps» (aparece bajo «Android System»). " +
                        "Los nombres cambian según el teléfono y en algunos no se puede ocultar.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ojo: no desactives la notificación «DarmixPet activo»; es la que mantiene viva a Archi.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}
