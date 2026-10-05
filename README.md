# DARK ALISE OBS - Mobile Broadcast Studio

**Dark Alise OBS** is a genuine, native Android broadcast and screen recording studio engineered to bring the closest possible OBS Studio desktop workflow to Android devices using legitimate, public Android APIs.

---

## ⚡ Highlights & Key Capabilities

- **Real Screen Capture**: Android `MediaProjection` integration supporting native screen resolutions (720p, 1080p, 1440p) at up to 60 FPS.
- **Hardware-Accelerated Video Encoding**: High-performance Android `MediaCodec` pipeline utilizing on-device hardware H.264 (AVC) and HEVC encoders with CBR/VBR modes.
- **Live RTMP/RTMPS Streaming**: Direct socket-level RTMP/RTMPS client with live keyframe interval negotiation, AVC packetization, reconnection backoff, and upload bitrate telemetry.
- **Native MP4 Recording Engine**: Android `MediaMuxer` container writer outputting directly to scoped storage (`Movies/Dark Alise OBS`) via the Android `MediaStore` API without requiring unrestricted disk permissions.
- **Multi-Scene Composition Architecture**:
  - Default Scenes: `MAIN`, `GAMING`, `CAMERA`, `JUST CHAT`, `STARTING`, `BRB`, `ENDING`.
  - Create, duplicate, rename, delete, and reorder scenes.
  - Scene transitions with instant **Cut** and smoothed **Fade**.
- **Real Layered Source Pipeline**:
  - `Display Capture` (system display mirror)
  - `Camera` (front/rear CameraX feeds with drag-and-drop overlays in rectangle, rounded, and circle shapes)
  - `Text Overlays` (custom titles, countdowns, and alerts)
  - `Color / Image / Media / Browser WebView Sources`
  - Independent visibility toggle, layer locking, opacity, and positioning.
- **Broadcast Audio Mixer**:
  - Independent channels: `Microphone`, `Device Audio`, `Camera Audio`, `Media Audio`.
  - Faders with volume boost (up to 150%), independent mute buttons, gain staging.
  - Real-time **Peak dBFS & RMS Level Meters** calculated from raw 16-bit PCM byte buffers.
  - Android 10+ `AudioPlaybackCapture` integration respecting app playback policies.
- **Studio Layouts**:
  - Adaptive **Landscape Studio Layout** featuring side-by-side Program Canvas, Scene Deck, Source Stack, Audio Mixer, and Transport Controls.
  - Responsive **Portrait Mobile Layout** for single-hand stream monitoring.
- **Hardware Hotkeys**:
  - `F1` -> Scene 1
  - `F2` -> Scene 2
  - `F3` -> Scene 3
  - `Ctrl + Shift + R` -> Toggle Recording
  - `Ctrl + Shift + S` -> Toggle Live Stream
- **Strict Privacy Charter**:
  - 100% local processing; no cloud dependencies or account logins.
  - Zero telemetry or analytics trackers.
  - Stream keys stored safely in private device preferences.

---

## 🛠 Project Structure

```
DarkAliseOBS/
├── .github/workflows/
│   └── build.yml               # Automated CI to build and upload Debug APK
├── app/
│   ├── build.gradle.kts        # Android build configuration & dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/darkalise/obs/
│       │   │   ├── core/
│       │   │   │   ├── audio/          # AudioCaptureManager & AudioMixer (PCM VU meter)
│       │   │   │   ├── camera/         # CameraManager (CameraX front/rear & controls)
│       │   │   │   ├── capture/        # ScreenCaptureManager (MediaProjection & VirtualDisplay)
│       │   │   │   ├── encoder/        # VideoEncoder (MediaCodec H.264/HEVC)
│       │   │   │   ├── notification/   # NotificationHelper (Foreground service stats & actions)
│       │   │   │   ├── overlay/        # CameraOverlayConfig (Shapes & drag coords)
│       │   │   │   ├── performance/    # PerformanceMonitor (FPS, RAM, Thermals)
│       │   │   │   ├── permissions/    # PermissionManager (Camera, Mic, Notifications)
│       │   │   │   ├── recording/      # RecordingEngine (MediaMuxer MP4 writer)
│       │   │   │   ├── scene/          # SceneManager & SceneModels
│       │   │   │   ├── settings/       # SettingsRepository & Profiles (JSON Export/Import)
│       │   │   │   ├── source/         # SourceModels & Layering
│       │   │   │   ├── storage/        # RecordingsManager (MediaStore queries & thumbnails)
│       │   │   │   └── stream/         # RtmpStreamer (Live RTMP/RTMPS socket transmitter)
│       │   │   ├── service/
│       │   │   │   └── RecordingService.kt  # Android Foreground Service
│       │   │   └── ui/
│       │   │       ├── MainActivity.kt # Entry point, Hotkey listeners & Navigation
│       │   │       ├── studio/         # StudioScreen, StudioPreview & AudioMixerPanel
│       │   │       ├── recordings/     # RecordingsScreen (library & playback)
│       │   │       ├── settings/       # SettingsScreen (Video, Audio, RTMP, Profiles)
│       │   │       ├── privacy/        # PrivacyScreen
│       │   │       └── theme/          # Dark Alise black/neon purple visual identity
│       │   └── res/
│       │       └── values/             # strings.xml, colors.xml, themes.xml
│       └── test/java/com/darkalise/obs/
│           ├── SceneManagerTest.kt     # Unit tests for scene switching & duplication
│           └── AudioMixerTest.kt       # Unit tests for PCM RMS & dBFS calculation
├── build.gradle.kts            # Root Gradle script
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Building the APK

### Requirements
- **JDK 17** (Temurin or OpenJDK)
- **Android SDK Platform 35**
- **Gradle 8.8+** (or Gradle wrapper)

### Build Command (Terminal)
```bash
# Clone the repository
git clone https://github.com/darkalise/dark-alise-obs.git
cd dark-alise-obs

# Grant wrapper permissions
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Automated GitHub Actions Build
Every commit pushed to `main` or triggered via `workflow_dispatch` will automatically run `.github/workflows/build.yml` on Ubuntu runners with JDK 17, assemble the APK, and publish it under the Actions Artifacts as `DarkAliseOBS-debug.apk`.

---

## 🔒 Permissions & Security Architecture

| Permission | Purpose |
| :--- | :--- |
| `RECORD_AUDIO` | Feeds the broadcast audio mixer with real microphone PCM data. |
| `CAMERA` | Supplies live front/back video to the camera source and overlay preview. |
| `FOREGROUND_SERVICE` | Ensures continuous recording and streaming when the UI is minimized. |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Mandatory on Android 14+ (API 34) for screen capture services. |
| `FOREGROUND_SERVICE_MICROPHONE` | Mandatory on Android 14+ for background audio capture. |
| `FOREGROUND_SERVICE_CAMERA` | Mandatory on Android 14+ for background camera use. |
| `POST_NOTIFICATIONS` | Displays the live persistent notification with Stop and Pause controls. |
| `INTERNET` | Transmits live RTMP/RTMPS streams to your chosen ingest server. |

---

## ⚠️ Known Android Architectural Limitations

1. **Virtual Camera Output**:
   Android does not permit third-party applications without system-level firmware vendor privileges to register as a system-wide camera HAL device (`/dev/video*`). In Dark Alise OBS, when Virtual Camera is selected, the application displays:
   `"Virtual camera output is not supported on this Android configuration."`
   rather than faking the functionality.

2. **Device Internal Audio Capture**:
   Android 10+ `AudioPlaybackCapture` allows capturing internal audio only if the third-party application being played explicitly opts in via `AUDIO_USAGE_MEDIA`, `AUDIO_USAGE_GAME`, or `AUDIO_USAGE_UNKNOWN`. If an app sets its policy to forbid capture (e.g., banking or copyright-protected apps), Dark Alise OBS respects the flag and displays:
   `"Device/application does not allow audio capture."`

---

## 📄 License
Apache License 2.0. Built with native Android Jetpack Compose and Kotlin.
