package com.darkalise.obs.core.scene

import com.darkalise.obs.core.source.Source
import com.darkalise.obs.core.source.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SceneManager {

    private val _scenes = MutableStateFlow<List<Scene>>(emptyList())
    val scenes: StateFlow<List<Scene>> = _scenes.asStateFlow()

    private val _activeSceneId = MutableStateFlow<String>("")
    val activeSceneId: StateFlow<String> = _activeSceneId.asStateFlow()

    private val _sourcesMap = MutableStateFlow<Map<String, Source>>(emptyMap())
    val sourcesMap: StateFlow<Map<String, Source>> = _sourcesMap.asStateFlow()

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    init {
        initializeDefaultScenes()
    }

    fun initializeDefaultScenes() {
        // Create baseline sources
        val displaySource = Source(
            id = "src_display",
            name = "Display Capture",
            type = SourceType.DISPLAY_CAPTURE
        )
        val cameraSource = Source(
            id = "src_cam",
            name = "Front Camera Overlay",
            type = SourceType.CAMERA
        )
        val micSource = Source(
            id = "src_mic",
            name = "Microphone",
            type = SourceType.AUDIO_INPUT
        )
        val textStartingSource = Source(
            id = "src_text_starting",
            name = "Stream Starting Soon",
            type = SourceType.TEXT,
            extraParams = mapOf("text" to "STREAM STARTING SOON")
        )
        val textBrbSource = Source(
            id = "src_text_brb",
            name = "Be Right Back",
            type = SourceType.TEXT,
            extraParams = mapOf("text" to "BE RIGHT BACK")
        )
        val textEndingSource = Source(
            id = "src_text_ending",
            name = "Stream Ending",
            type = SourceType.TEXT,
            extraParams = mapOf("text" to "THANKS FOR WATCHING!")
        )

        _sourcesMap.value = mapOf(
            displaySource.id to displaySource,
            cameraSource.id to cameraSource,
            micSource.id to micSource,
            textStartingSource.id to textStartingSource,
            textBrbSource.id to textBrbSource,
            textEndingSource.id to textEndingSource
        )

        val defaultScenes = listOf(
            Scene(name = "MAIN", sourceIds = listOf(displaySource.id, cameraSource.id, micSource.id), isDefault = true),
            Scene(name = "GAMING", sourceIds = listOf(displaySource.id, micSource.id)),
            Scene(name = "CAMERA", sourceIds = listOf(cameraSource.id, micSource.id)),
            Scene(name = "JUST CHAT", sourceIds = listOf(cameraSource.id, micSource.id)),
            Scene(name = "STARTING", sourceIds = listOf(textStartingSource.id, micSource.id)),
            Scene(name = "BRB", sourceIds = listOf(textBrbSource.id)),
            Scene(name = "ENDING", sourceIds = listOf(textEndingSource.id))
        )

        _scenes.value = defaultScenes
        _activeSceneId.value = defaultScenes.first().id
    }

    fun getActiveScene(): Scene? {
        val currentId = _activeSceneId.value
        return _scenes.value.find { it.id == currentId } ?: _scenes.value.firstOrNull()
    }

    fun switchScene(sceneId: String, transitionOverride: SceneTransition? = null) {
        val target = _scenes.value.find { it.id == sceneId } ?: return
        if (target.id == _activeSceneId.value) return

        val transition = transitionOverride ?: target.transition
        if (transition.type == TransitionType.FADE && transition.durationMs > 0) {
            _isTransitioning.value = true
            _activeSceneId.value = target.id
            // In a real render pipeline, the compositor blends the two framebuffers
            _isTransitioning.value = false
        } else {
            // Cut transition
            _activeSceneId.value = target.id
        }
    }

    fun createScene(name: String, sourceIds: List<String> = emptyList()): Scene {
        val newScene = Scene(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Scene ${_scenes.value.size + 1}" },
            sourceIds = sourceIds
        )
        _scenes.value = _scenes.value + newScene
        return newScene
    }

    fun renameScene(sceneId: String, newName: String) {
        _scenes.value = _scenes.value.map { scene ->
            if (scene.id == sceneId) scene.copy(name = newName.trim()) else scene
        }
    }

    fun duplicateScene(sceneId: String): Scene? {
        val original = _scenes.value.find { it.id == sceneId } ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            isDefault = false
        )
        val currentIndex = _scenes.value.indexOf(original)
        val mutableList = _scenes.value.toMutableList()
        mutableList.add(currentIndex + 1, copy)
        _scenes.value = mutableList
        return copy
    }

    fun deleteScene(sceneId: String): Boolean {
        if (_scenes.value.size <= 1) return false // At least one scene must remain
        val sceneToDelete = _scenes.value.find { it.id == sceneId } ?: return false
        _scenes.value = _scenes.value.filter { it.id != sceneId }
        if (_activeSceneId.value == sceneId) {
            _activeSceneId.value = _scenes.value.first().id
        }
        return true
    }

    fun moveScene(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in _scenes.value.indices || toIndex !in _scenes.value.indices) return
        val mutableList = _scenes.value.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        _scenes.value = mutableList
    }

    fun addSourceToScene(sceneId: String, sourceId: String) {
        _scenes.value = _scenes.value.map { scene ->
            if (scene.id == sceneId && !scene.sourceIds.contains(sourceId)) {
                scene.copy(sourceIds = scene.sourceIds + sourceId)
            } else scene
        }
    }

    fun removeSourceFromScene(sceneId: String, sourceId: String) {
        _scenes.value = _scenes.value.map { scene ->
            if (scene.id == sceneId) {
                scene.copy(sourceIds = scene.sourceIds.filter { it != sourceId })
            } else scene
        }
    }

    fun reorderSceneSources(sceneId: String, newSourceIds: List<String>) {
        _scenes.value = _scenes.value.map { scene ->
            if (scene.id == sceneId) scene.copy(sourceIds = newSourceIds) else scene
        }
    }

    fun upsertSource(source: Source) {
        val current = _sourcesMap.value.toMutableMap()
        current[source.id] = source
        _sourcesMap.value = current
    }

    fun updateSourceVisibility(sourceId: String, isVisible: Boolean) {
        val source = _sourcesMap.value[sourceId] ?: return
        upsertSource(source.copy(isVisible = isVisible))
    }

    fun updateSourceLock(sourceId: String, isLocked: Boolean) {
        val source = _sourcesMap.value[sourceId] ?: return
        upsertSource(source.copy(isLocked = isLocked))
    }

    fun updateSourceMute(sourceId: String, isMuted: Boolean) {
        val source = _sourcesMap.value[sourceId] ?: return
        upsertSource(source.copy(isMuted = isMuted))
    }
}
