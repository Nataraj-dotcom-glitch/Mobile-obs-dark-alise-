package com.darkalise.obs.core.scene

import java.util.UUID

enum class TransitionType {
    CUT,
    FADE
}

data class SceneTransition(
    val type: TransitionType = TransitionType.CUT,
    val durationMs: Long = 300L
)

data class Scene(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceIds: List<String> = emptyList(),
    val isDefault: Boolean = false,
    val transition: SceneTransition = SceneTransition()
)
