package com.laulegr.videoapp.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laulegr.videoapp.ai.EditTemplate
import com.laulegr.videoapp.ai.HeuristicTemplateEngine
import com.laulegr.videoapp.ai.TemplateEngine
import com.laulegr.videoapp.editing.ExportOutcome
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
    val exportState: ExportState = ExportState.Idle,
) {
    val totalDurationMs: Long get() = clips.sumOf { it.trimmedDurationMs }
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

    fun setFilter(filter: FilterPreset) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setTransition(transition: TransitionType) {
        if (!transition.available) return
        _uiState.update { it.copy(transition = transition) }
    }

    fun applyTemplate(template: EditTemplate) {
        _uiState.update { state ->
            val clips = state.clips.map { clip ->
                val end = template.perClipDurationMs
                    ?.let { (it).coerceAtMost(clip.durationMs) }
                    ?: clip.durationMs
                clip.copy(trimStartMs = 0L, trimEndMs = end)
            }
            state.copy(clips = clips, filter = template.filter)
        }
    }

    fun export() {
        val state = _uiState.value
        if (state.clips.isEmpty() || state.exportState == ExportState.Running) return

        _uiState.update { it.copy(exportState = ExportState.Running) }
        viewModelScope.launch {
            when (val outcome = exporter.export(state.clips, state.filter)) {
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
