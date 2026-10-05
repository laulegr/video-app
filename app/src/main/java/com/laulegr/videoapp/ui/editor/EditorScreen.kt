package com.laulegr.videoapp.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.laulegr.videoapp.ai.EditTemplate
import com.laulegr.videoapp.editing.shareVideo
import com.laulegr.videoapp.model.CanvasFormat
import com.laulegr.videoapp.model.FilterPreset
import com.laulegr.videoapp.model.SPEED_STEPS
import com.laulegr.videoapp.model.VideoClip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var transitionSheetClipId by remember { mutableStateOf<String?>(null) }

    val pickMusic = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) viewModel.setMusic(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor · ${state.clips.size} Clips") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::export,
                    enabled = state.clips.isNotEmpty() && state.exportState !is ExportState.Running,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                ) {
                    if (state.exportState is ExportState.Running) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Exportiere…")
                    } else {
                        Text("Exportieren (${formatMs(state.totalDurationMs)})")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        ) {
            SectionLabel("Timeline")
            Timeline(
                clips = state.clips,
                onMove = viewModel::moveClip,
                onRemove = viewModel::removeClip,
                onTrim = viewModel::updateTrim,
                onSpeed = viewModel::setClipSpeed,
                onCaption = viewModel::setClipCaption,
                onTransitionClick = { clipId -> transitionSheetClipId = clipId },
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("KI-Vorlagen")
            TemplateRow(templates = state.templates, onApply = viewModel::applyTemplate)

            Spacer(Modifier.height(20.dp))
            SectionLabel("Format")
            CanvasRow(selected = state.canvas, onSelect = viewModel::setCanvas)

            Spacer(Modifier.height(20.dp))
            SectionLabel("Filter")
            FilterRow(selected = state.filter, onSelect = viewModel::setFilter)

            Spacer(Modifier.height(20.dp))
            SectionLabel("Musik")
            MusicSection(
                musicPicked = state.musicUri != null,
                volume = state.musicVolume,
                onPick = { pickMusic.launch("audio/*") },
                onRemove = { viewModel.setMusic(null) },
                onVolumeChange = viewModel::setMusicVolume,
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    val sheetClip = state.clips.firstOrNull { it.id == transitionSheetClipId }
    if (sheetClip != null) {
        TransitionSheet(
            current = sheetClip.transitionOut,
            onChange = { viewModel.setClipTransition(sheetClip.id, it) },
            onApplyToAll = viewModel::applyTransitionToAll,
            onDismiss = { transitionSheetClipId = null },
        )
    }

    when (val exportState = state.exportState) {
        is ExportState.Done -> AlertDialog(
            onDismissRequest = viewModel::dismissExportResult,
            confirmButton = { Button(onClick = viewModel::dismissExportResult) { Text("OK") } },
            dismissButton = {
                TextButton(onClick = { shareVideo(context, exportState.uri) }) { Text("Teilen") }
            },
            title = { Text("Export fertig") },
            text = { Text("Das Video wurde in der Galerie unter Movies/ReelCut gespeichert.") },
        )
        is ExportState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissExportResult,
            confirmButton = { Button(onClick = viewModel::dismissExportResult) { Text("OK") } },
            title = { Text("Export fehlgeschlagen") },
            text = { Text(exportState.message) },
        )
        else -> Unit
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun Timeline(
    clips: List<VideoClip>,
    onMove: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onTrim: (String, Long, Long) -> Unit,
    onSpeed: (String, Float) -> Unit,
    onCaption: (String, String) -> Unit,
    onTransitionClick: (String) -> Unit,
) {
    if (clips.isEmpty()) {
        Text(
            "Noch keine Clips. Geh zurück und wähle Videos aus.",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(clips, key = { _, clip -> clip.id }) { index, clip ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ClipCard(
                    clip = clip,
                    onMoveLeft = { onMove(clip.id, -1) },
                    onMoveRight = { onMove(clip.id, 1) },
                    onRemove = { onRemove(clip.id) },
                    onTrim = { start, end -> onTrim(clip.id, start, end) },
                    onSpeed = { speed -> onSpeed(clip.id, speed) },
                    onCaption = { text -> onCaption(clip.id, text) },
                )
                if (index < clips.lastIndex) {
                    TransitionButton(clip.transitionOut, onClick = { onTransitionClick(clip.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipCard(
    clip: VideoClip,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
    onTrim: (Long, Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onCaption: (String) -> Unit,
) {
    Card(modifier = Modifier.width(220.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = clip.thumbnail
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("Kein Vorschaubild", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMoveLeft) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Nach links")
                }
                Text(formatMs(clip.effectiveDurationMs), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onMoveRight) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nach rechts")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Entfernen")
                }
            }

            if (clip.durationMs > 0) {
                RangeSlider(
                    value = clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat(),
                    onValueChange = { range ->
                        onTrim(range.start.toLong(), range.endInclusive.toLong())
                    },
                    valueRange = 0f..clip.durationMs.toFloat(),
                )
            }

            Text("Geschwindigkeit", style = MaterialTheme.typography.bodyMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SPEED_STEPS) { step ->
                    FilterChip(
                        selected = clip.speed == step,
                        onClick = { onSpeed(step) },
                        label = { Text("${step}x") },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = clip.captionText ?: "",
                onValueChange = onCaption,
                label = { Text("Text (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TemplateRow(templates: List<EditTemplate>, onApply: (EditTemplate) -> Unit) {
    if (templates.isEmpty()) {
        Text(
            "Wähle Clips aus, um Vorschläge zu erhalten.",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(templates, key = { it.id }) { template ->
            Card(modifier = Modifier.width(220.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(template.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onApply(template) }) {
                        Text("Anwenden")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(selected: FilterPreset, onSelect: (FilterPreset) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FilterPreset.entries) { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                label = { Text(preset.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanvasRow(selected: CanvasFormat, onSelect: (CanvasFormat) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CanvasFormat.entries) { canvas ->
            FilterChip(
                selected = canvas == selected,
                onClick = { onSelect(canvas) },
                label = { Text(canvas.label) },
            )
        }
    }
}

@Composable
private fun MusicSection(
    musicPicked: Boolean,
    volume: Float,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (!musicPicked) {
            OutlinedButton(onClick = onPick) {
                Icon(Icons.Filled.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Musik hinzufügen")
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Titel ausgewählt", modifier = Modifier.weight(1f))
                TextButton(onClick = onRemove) { Text("Entfernen") }
            }
            Text("Lautstärke", style = MaterialTheme.typography.bodyMedium)
            Slider(value = volume, onValueChange = onVolumeChange, valueRange = 0f..1f)
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
