package com.laulegr.videoapp.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laulegr.videoapp.ai.ContentAwareTemplateEngine
import com.laulegr.videoapp.ai.ContentLabeler
import com.laulegr.videoapp.ai.EditTemplate
import com.laulegr.videoapp.ai.HeuristicTemplateEngine
import com.laulegr.videoapp.ai.TemplateEngine
import com.laulegr.videoapp.editing.ExportOutcome
import com.laulegr.videoapp.editing.MusicSettings
import com.laulegr.videoapp.editing.VideoExporter
import com.laulegr.videoapp.editing.VideoMetadata
import com.laulegr.videoapp.model.FilterPreset
import com.laulegr.videoapp.model.TransitionType
import com.laulegr.videoapp.model.VideoClip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExportState {
    data object Idle : ExportState
    data object Running : ExportState
    data class Done(val uri: Uri) : ExportState
    data class Error(val message: String) : ExportState
}

data class EditorUiState(
    val clips: List<VideoClip> = emptyList(),
    val filter: FilterPreset = FilterPreset.NONE,
    val transition: TransitionType = TransitionType.CUT,
    val templates: List<EditTemplate> = emptyList(),
    val musicUri: Uri? = null,
    val musicVolume: Float = 0.5f,
    val exportState: ExportState = ExportState.Idle,
) {
    val totalDurationMs: Long get() = clips.sumOf { it.effectiveDurationMs }
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val templateEngine: TemplateEngine = HeuristicTemplateEngine()
    private val exporter = VideoExporter(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun addClips(uris: List<Uri>) {
        viewModelScope.launch {
            val newClips = uris.map { uri ->
                val info = VideoMetadata.read(getApplication(), uri)
                VideoClip(uri = uri, durationMs = info.durationMs, thumbnail = info.thumbnail)
            }
            _uiState.update { state ->
                val clips = state.clips + newClips
                state.copy(clips = clips, templates = templateEngine.suggest(clips))
            }
            refreshContentAwareSuggestion()
        }
    }

    /** Best-effort: adds one extra ML Kit-based suggestion once labels are ready; never blocks the UI. */
    private fun refreshContentAwareSuggestion() {
        viewModelScope.launch {
            val clips = _uiState.value.clips
            if (clips.isEmpty()) return@launch
            val labels = runCatching { ContentLabeler.dominantLabels(clips) }.getOrNull() ?: return@launch
            val smart = ContentAwareTemplateEngine.suggestFrom(labels, clips.size) ?: return@launch
            _uiState.update { state ->
                if (state.clips != clips) return@update state // timeline changed meanwhile, discard
                state.copy(templates = listOf(smart) + state.templates.filterNot { it.id == smart.id })
            }
        }
    }

    fun removeClip(clipId: String) {
        _uiState.update { state ->
            val clips = state.clips.filterNot { it.id == clipId }
            state.copy(clips = clips, templates = templateEngine.suggest(clips))
        }
    }

    fun moveClip(clipId: String, delta: Int) {
        _uiState.update { state ->
            val index = state.clips.indexOfFirst { it.id == clipId }
            val target = index + delta
            if (index < 0 || target < 0 || target >= state.clips.size) return@update state
            val mutable = state.clips.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            state.copy(clips = mutable)
        }
    }

    fun updateTrim(clipId: String, startMs: Long, endMs: Long) {
        _uiState.update { state ->
            val clips = state.clips.map { clip ->
                if (clip.id == clipId) clip.copy(trimStartMs = startMs, trimEndMs = endMs) else clip
            }
            state.copy(clips = clips)
        }
    }

    fun setClipSpeed(clipId: String, speed: Float) {
        _uiState.update { state ->
            val clips = state.clips.map { clip -> if (clip.id == clipId) clip.copy(speed = speed) else clip }
            state.copy(clips = clips)
        }
    }

    fun setClipCaption(clipId: String, caption: String) {
        _uiState.update { state ->
            val clips = state.clips.map { clip ->
                if (clip.id == clipId) clip.copy(captionText = caption.ifBlank { null }) else clip
            }
            state.copy(clips = clips)
        }
    }

    fun setFilter(filter: FilterPreset) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setTransition(transition: TransitionType) {
        _uiState.update { it.copy(transition = transition) }
    }

    fun setMusic(uri: Uri?) {
        _uiState.update { it.copy(musicUri = uri) }
    }

    fun setMusicVolume(volume: Float) {
        _uiState.update { it.copy(musicVolume = volume.coerceIn(0f, 1f)) }
    }

    fun applyTemplate(template: EditTemplate) {
        _uiState.update { state ->
            val clips = state.clips.map { clip ->
                val end = template.perClipDurationMs
                    ?.let { it.coerceAtMost(clip.durationMs) }
                    ?: clip.durationMs
                clip.copy(trimStartMs = 0L, trimEndMs = end, speed = template.speed)
            }
            state.copy(clips = clips, filter = template.filter)
        }
    }

    fun export() {
        val state = _uiState.value
        if (state.clips.isEmpty() || state.exportState == ExportState.Running) return

        _uiState.update { it.copy(exportState = ExportState.Running) }
        viewModelScope.launch {
            val music = state.musicUri?.let { MusicSettings(it, state.musicVolume) }
            val outcome = exporter.export(
                clips = state.clips,
                filter = state.filter,
                transition = state.transition,
                music = music,
            )
            when (outcome) {
                is ExportOutcome.Success ->
                    _uiState.update { it.copy(exportState = ExportState.Done(outcome.uri)) }
                is ExportOutcome.Failure ->
                    _uiState.update { it.copy(exportState = ExportState.Error(outcome.message)) }
            }
        }
    }

    fun dismissExportResult() {
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }
}
