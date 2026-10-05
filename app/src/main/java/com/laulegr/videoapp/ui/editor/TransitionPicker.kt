package com.laulegr.videoapp.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laulegr.videoapp.model.Transition
import com.laulegr.videoapp.model.TransitionType
import kotlin.math.roundToLong

/** The small round button sitting between two clips in the timeline, CapCut-style. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionButton(transition: Transition, onClick: () -> Unit) {
    val active = transition.type != TransitionType.NONE
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    transition.type.icon(),
                    contentDescription = "Übergang ändern: ${transition.type.label}",
                    tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            transition.type.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionSheet(
    current: Transition,
    onChange: (Transition) -> Unit,
    onApplyToAll: (Transition) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Übergang", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            TransitionType.entries.chunked(4).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    row.forEach { type ->
                        TransitionTile(
                            type = type,
                            selected = type == current.type,
                            onClick = { onChange(current.copy(type = type)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(8.dp))
            val seconds = current.durationMs / 1_000f
            Text(
                "Dauer: ${"%.1f".format(seconds)} s",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = current.durationMs.toFloat(),
                onValueChange = { ms ->
                    val rounded = ((ms / 100f).roundToLong() * 100L)
                        .coerceIn(Transition.MIN_DURATION_MS, Transition.MAX_DURATION_MS)
                    if (rounded != current.durationMs) onChange(current.copy(durationMs = rounded))
                },
                valueRange = Transition.MIN_DURATION_MS.toFloat()..Transition.MAX_DURATION_MS.toFloat(),
                steps = ((Transition.MAX_DURATION_MS - Transition.MIN_DURATION_MS) / 100L - 1).toInt(),
                enabled = current.type != TransitionType.NONE,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onApplyToAll(current) }, modifier = Modifier.weight(1f)) {
                    Text("Auf alle anwenden")
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Fertig")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransitionTile(
    type: TransitionType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(76.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp),
        ) {
            Icon(type.icon(), contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(
                type.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

private fun TransitionType.icon(): ImageVector = when (this) {
    TransitionType.NONE -> Icons.Filled.Block
    TransitionType.FADE_BLACK -> Icons.Filled.DarkMode
    TransitionType.FLASH_WHITE -> Icons.Filled.FlashOn
    TransitionType.ZOOM_IN -> Icons.Filled.ZoomIn
    TransitionType.ZOOM_OUT -> Icons.Filled.ZoomOut
    TransitionType.SPIN -> Icons.AutoMirrored.Filled.RotateRight
    TransitionType.SWIPE_LEFT -> Icons.Filled.KeyboardDoubleArrowLeft
    TransitionType.SWIPE_RIGHT -> Icons.Filled.KeyboardDoubleArrowRight
    TransitionType.SWIPE_UP -> Icons.Filled.KeyboardDoubleArrowUp
    TransitionType.SHAKE -> Icons.Filled.Vibration
    TransitionType.GLITCH -> Icons.Filled.BrokenImage
}
