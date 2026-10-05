package com.darkalise.obs

import com.darkalise.obs.core.scene.SceneManager
import com.darkalise.obs.core.scene.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SceneManagerTest {

    private lateinit var sceneManager: SceneManager

    @Before
    fun setUp() {
        sceneManager = SceneManager()
    }

    @Test
    fun defaultScenesInitialized() {
        val scenes = sceneManager.scenes.value
        assertTrue("Should have default scenes", scenes.isNotEmpty())
        assertEquals("MAIN", scenes.first().name)
        assertTrue(scenes.any { it.name == "GAMING" })
        assertTrue(scenes.any { it.name == "CAMERA" })
    }

    @Test
    fun switchSceneUpdatesActiveId() {
        val scenes = sceneManager.scenes.value
        val target = scenes[1]
        sceneManager.switchScene(target.id)
        assertEquals(target.id, sceneManager.activeSceneId.value)
    }

    @Test
    fun createAndDuplicateScene() {
        val initialCount = sceneManager.scenes.value.size
        val created = sceneManager.createScene("INTERVIEW")
        assertEquals(initialCount + 1, sceneManager.scenes.value.size)

        val duplicate = sceneManager.duplicateScene(created.id)
        assertNotNull(duplicate)
        assertEquals("INTERVIEW (Copy)", duplicate?.name)
    }

    @Test
    fun cannotDeleteLastScene() {
        // Delete scenes down to 1
        while (sceneManager.scenes.value.size > 1) {
            val scene = sceneManager.scenes.value.first()
            sceneManager.deleteScene(scene.id)
        }
        val lastScene = sceneManager.scenes.value.first()
        val deleted = sceneManager.deleteScene(lastScene.id)
        assertFalse("Cannot delete the only remaining scene", deleted)
        assertEquals(1, sceneManager.scenes.value.size)
    }
}
