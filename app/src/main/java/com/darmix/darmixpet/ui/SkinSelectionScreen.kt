package com.darmix.darmixpet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.mascota.SkinInfo
import com.darmix.darmixpet.mascota.SkinPreferences
import com.darmix.darmixpet.mascota.SkinRegistry
import com.darmix.darmixpet.mascota.SpriteSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SkinSelectionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(SkinPreferences.getSelectedSkinId(context)) }

    var previews by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(Unit) {
        previews = withContext(Dispatchers.IO) {
            SkinRegistry.availableSkins.associate { info ->
                val frame = SpriteSheet.fromDrawable(context, info.idleRes, frameCount = 6).frames[0]
                info.id to frame.asImageBitmap()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Elige tu compañero", style = MaterialTheme.typography.titleLarge)
            Text(
                "Cada personaje te acompaña con su propia energía",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SkinRegistry.availableSkins, key = { it.id }) { skin ->
                SkinCard(
                    skin = skin,
                    preview = previews[skin.id],
                    isSelected = skin.id == selectedId,
                    onClick = {
                        selectedId = skin.id
                        SkinPreferences.setSelectedSkinId(context, skin.id)
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SkinCard(
    skin: SkinInfo,
    preview: ImageBitmap?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(skin.accentColor)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, accent, RoundedCornerShape(22.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = skin.displayName,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(skin.displayName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(skin.subtitle, style = MaterialTheme.typography.labelSmall, color = accent)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    skin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}