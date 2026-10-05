package com.darkalise.obs.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.darkalise.obs.core.permissions.PermissionManager
import com.darkalise.obs.core.performance.PerformanceMonitor
import com.darkalise.obs.core.recording.RecordingStats
import com.darkalise.obs.core.scene.SceneManager
import com.darkalise.obs.core.settings.SettingsRepository
import com.darkalise.obs.core.storage.RecordingsManager
import com.darkalise.obs.core.stream.StreamConfig
import com.darkalise.obs.core.stream.StreamLiveStats
import com.darkalise.obs.service.RecordingService
import com.darkalise.obs.ui.privacy.PrivacyScreen
import com.darkalise.obs.ui.recordings.RecordingsScreen
import com.darkalise.obs.ui.settings.SettingsScreen
import com.darkalise.obs.ui.studio.StudioScreen
import com.darkalise.obs.ui.theme.DarkAliseBlack
import com.darkalise.obs.ui.theme.DarkAliseNeon
import com.darkalise.obs.ui.theme.DarkAlisePurpleDark
import com.darkalise.obs.ui.theme.DarkAliseSurface
import com.darkalise.obs.ui.theme.DarkAliseTextMuted
import com.darkalise.obs.ui.theme.DarkAliseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val sceneManager = SceneManager()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var recordingsManager: RecordingsManager
    private lateinit var performanceMonitor: PerformanceMonitor

    private var recordingService: RecordingService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? RecordingService.LocalBinder
            recordingService = localBinder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            isBound = false
        }
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, RecordingService::class.java).apply {
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestMediaProjection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(this)
        recordingsManager = RecordingsManager(this)
        performanceMonitor = PerformanceMonitor(this).apply {
            startMonitoring(lifecycleScope)
        }

        lifecycleScope.launch {
            recordingsManager.loadRecordings()
        }

        requestPermissionsIfNeeded()

        setContent {
            DarkAliseTheme {
                MainAppScaffold()
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = PermissionManager.getRequiredPermissions().filter {
            !PermissionManager.hasCameraPermission(this) || !PermissionManager.hasRecordAudioPermission(this)
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        } else {
            requestMediaProjection()
        }
    }

    private fun requestMediaProjection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Hardware Hotkeys support
        when (keyCode) {
            KeyEvent.KEYCODE_F1 -> {
                sceneManager.scenes.value.getOrNull(0)?.let { sceneManager.switchScene(it.id) }
                return true
            }
            KeyEvent.KEYCODE_F2 -> {
                sceneManager.scenes.value.getOrNull(1)?.let { sceneManager.switchScene(it.id) }
                return true
            }
            KeyEvent.KEYCODE_F3 -> {
                sceneManager.scenes.value.getOrNull(2)?.let { sceneManager.switchScene(it.id) }
                return true
            }
            KeyEvent.KEYCODE_R -> {
                if (event?.isCtrlPressed == true && event.isShiftPressed) {
                    toggleRecording()
                    return true
                }
            }
            KeyEvent.KEYCODE_S -> {
                if (event?.isCtrlPressed == true && event.isShiftPressed) {
                    toggleStreaming()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun toggleRecording() {
        val service = recordingService ?: return
        if (service.isRecordingActive.value) {
            service.stopRecordingEngine()
        } else {
            service.startRecordingEngine()
        }
    }

    private fun toggleStreaming() {
        val service = recordingService ?: return
        if (service.isStreamingActive.value) {
            service.stopLiveStream()
        } else {
            val active = settingsRepository.activeProfile.value
            service.startLiveStream(
                StreamConfig(
                    serverUrl = active.stream.serverUrl,
                    streamKey = active.stream.streamKey,
                    width = active.video.outputResolutionWidth,
                    height = active.video.outputResolutionHeight,
                    fps = active.video.fps,
                    bitrateBps = active.video.videoBitrateKbps * 1000
                )
            )
        }
    }

    @Composable
    private fun MainAppScaffold() {
        var selectedNavIndex by remember { mutableIntStateOf(0) }
        val navItems = listOf("Studio", "Recordings", "Settings", "Privacy")

        val recordingStats = recordingService?.recordingEngine?.stats?.collectAsState()?.value ?: RecordingStats()
        val streamStats = recordingService?.rtmpStreamer?.stats?.collectAsState()?.value ?: StreamLiveStats()
        val telemetry by performanceMonitor.telemetry.collectAsState()

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DarkAliseSurface,
                    contentColor = DarkAliseNeon
                ) {
                    NavigationBarItem(
                        selected = selectedNavIndex == 0,
                        onClick = { selectedNavIndex = 0 },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = "Studio") },
                        label = { Text("Studio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkAliseNeon,
                            selectedTextColor = DarkAliseNeon,
                            indicatorColor = DarkAlisePurpleDark,
                            unselectedIconColor = DarkAliseTextMuted,
                            unselectedTextColor = DarkAliseTextMuted
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavIndex == 1,
                        onClick = {
                            selectedNavIndex = 1
                            lifecycleScope.launch { recordingsManager.loadRecordings() }
                        },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Recordings") },
                        label = { Text("Recordings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkAliseNeon,
                            selectedTextColor = DarkAliseNeon,
                            indicatorColor = DarkAlisePurpleDark,
                            unselectedIconColor = DarkAliseTextMuted,
                            unselectedTextColor = DarkAliseTextMuted
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavIndex == 2,
                        onClick = { selectedNavIndex = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkAliseNeon,
                            selectedTextColor = DarkAliseNeon,
                            indicatorColor = DarkAlisePurpleDark,
                            unselectedIconColor = DarkAliseTextMuted,
                            unselectedTextColor = DarkAliseTextMuted
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavIndex == 3,
                        onClick = { selectedNavIndex = 3 },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Privacy") },
                        label = { Text("Privacy") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkAliseNeon,
                            selectedTextColor = DarkAliseNeon,
                            indicatorColor = DarkAlisePurpleDark,
                            unselectedIconColor = DarkAliseTextMuted,
                            unselectedTextColor = DarkAliseTextMuted
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DarkAliseBlack)
            ) {
                when (selectedNavIndex) {
                    0 -> StudioScreen(
                        sceneManager = sceneManager,
                        audioMixer = recordingService?.audioMixer ?: com.darkalise.obs.core.audio.AudioMixer(),
                        recordingStats = recordingStats,
                        streamStats = streamStats,
                        telemetry = telemetry,
                        onStartRecording = { recordingService?.startRecordingEngine() },
                        onStopRecording = { recordingService?.stopRecordingEngine() },
                        onStartStreaming = { toggleStreaming() },
                        onStopStreaming = { recordingService?.stopLiveStream() }
                    )
                    1 -> RecordingsScreen(recordingsManager = recordingsManager)
                    2 -> SettingsScreen(settingsRepository = settingsRepository)
                    3 -> PrivacyScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
