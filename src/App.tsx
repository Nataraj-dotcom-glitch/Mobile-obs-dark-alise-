/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef } from 'react';
import { StudioHeader } from './components/StudioHeader';
import { StudioPreview } from './components/StudioPreview';
import { AudioMixer } from './components/AudioMixer';
import { SceneList } from './components/SceneList';
import { SourceList } from './components/SourceList';
import { StreamDeck } from './components/StreamDeck';
import { RecordingsManager } from './components/RecordingsManager';
import { SettingsModal, defaultProfiles } from './components/SettingsModal';
import { AndroidCodeHub } from './components/AndroidCodeHub';
import { PrivacyView } from './components/PrivacyView';
import {
  AudioChannel,
  CameraOverlayState,
  ObsProfilePreset,
  RecordingEntry,
  SceneItem,
  SourceItem,
  SourceType,
  TransitionType,
} from './types/obs';

export default function App() {
  const [activeTab, setActiveTab] = useState<'studio' | 'recordings' | 'settings' | 'android' | 'privacy'>('studio');

  // Broadcast & Recording States
  const [isStreaming, setIsStreaming] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [streamDurationSec, setStreamDurationSec] = useState(0);
  const [recordDurationSec, setRecordDurationSec] = useState(0);
  const [fps, setFps] = useState(60);
  const [cpuPercent, setCpuPercent] = useState(14);
  const [ramMb, setRamMb] = useState(142);
  const [bitrateKbps, setBitrateKbps] = useState(4500);
  const [droppedFrames, setDroppedFrames] = useState(0);

  // Settings
  const [currentProfile, setCurrentProfile] = useState<ObsProfilePreset>(defaultProfiles[0]);
  const [streamUrl, setStreamUrl] = useState('rtmp://live.twitch.tv/app/');
  const [streamKey, setStreamKey] = useState('');

  // Hardware Streams
  const [screenStream, setScreenStream] = useState<MediaStream | null>(null);
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);
  const [micStream, setMicStream] = useState<MediaStream | null>(null);

  // Canvas & MediaRecorder
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);
  const recordStartTimeRef = useRef<number>(0);

  // Audio Context & Analyser for real VU meter
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);

  // Recordings Library
  const [recordings, setRecordings] = useState<RecordingEntry[]>([]);

  // Default Scenes System
  const [scenes, setScenes] = useState<SceneItem[]>([
    { id: 'sc_main', name: 'MAIN', sourceIds: ['src_display', 'src_cam', 'src_text_title'], isDefault: true },
    { id: 'sc_gaming', name: 'GAMING', sourceIds: ['src_display', 'src_cam'] },
    { id: 'sc_camera', name: 'CAMERA', sourceIds: ['src_cam', 'src_text_title'] },
    { id: 'sc_chat', name: 'JUST CHAT', sourceIds: ['src_cam', 'src_text_title'] },
    { id: 'sc_starting', name: 'STARTING', sourceIds: ['src_text_title'] },
    { id: 'sc_brb', name: 'BRB', sourceIds: [] },
    { id: 'sc_ending', name: 'ENDING', sourceIds: [] },
  ]);
  const [activeSceneId, setActiveSceneId] = useState<string>('sc_main');
  const [transitionType, setTransitionType] = useState<TransitionType>('CUT');

  // Default Sources
  const [sources, setSources] = useState<SourceItem[]>([
    {
      id: 'src_display',
      name: 'Display Capture',
      type: 'DISPLAY_CAPTURE',
      isVisible: true,
      isLocked: false,
      isMuted: false,
      volume: 1.0,
      transform: { x: 0, y: 0, width: 100, height: 100, scale: 1, rotation: 0, opacity: 1 },
    },
    {
      id: 'src_cam',
      name: 'Camera Overlay',
      type: 'CAMERA',
      isVisible: true,
      isLocked: false,
      isMuted: false,
      volume: 1.0,
      transform: { x: 70, y: 65, width: 26, height: 26, scale: 1, rotation: 0, opacity: 1 },
    },
    {
      id: 'src_text_title',
      name: 'Stream Overlay Banner',
      type: 'TEXT',
      isVisible: true,
      isLocked: true,
      isMuted: false,
      volume: 1.0,
      transform: { x: 3, y: 6, width: 30, height: 5, scale: 1, rotation: 0, opacity: 1 },
      extra: { text: 'DARK ALISE OBS BROADCAST', textColor: '#A855F7' },
    },
  ]);

  // Camera Overlay State
  const [cameraOverlay, setCameraOverlay] = useState<CameraOverlayState>({
    shape: 'ROUNDED',
    borderWidth: 3,
    borderColor: '#A855F7',
    x: 72,
    y: 68,
    width: 25,
    height: 25,
  });

  // Audio Mixer Channels
  const [channels, setChannels] = useState<AudioChannel[]>([
    { id: 'ch_mic', name: 'Microphone', volume: 0.9, isMuted: false, gainDb: 0, peakDb: -60, rmsLevel: 0, statusText: 'Ready' },
    { id: 'ch_desktop', name: 'Device Audio', volume: 1.0, isMuted: false, gainDb: 0, peakDb: -60, rmsLevel: 0, statusText: 'Playback' },
    { id: 'ch_cam', name: 'Camera Audio', volume: 0.8, isMuted: false, gainDb: 0, peakDb: -60, rmsLevel: 0, statusText: 'Sync' },
    { id: 'ch_media', name: 'Media Audio', volume: 0.75, isMuted: false, gainDb: 0, peakDb: -60, rmsLevel: 0, statusText: 'Idle' },
  ]);

  const activeScene = scenes.find((s) => s.id === activeSceneId) || scenes[0];

  // Live Timer & Telemetry Tick
  useEffect(() => {
    const timer = setInterval(() => {
      if (isStreaming) {
        setStreamDurationSec((s) => s + 1);
        setBitrateKbps(Math.round(4400 + Math.random() * 200));
      }
      if (isRecording) {
        setRecordDurationSec((s) => s + 1);
      }
      // Modulate CPU & RAM subtly based on activity
      const baseCpu = isRecording && isStreaming ? 28 : isRecording || isStreaming ? 20 : 12;
      setCpuPercent(baseCpu + Math.floor(Math.random() * 5));
    }, 1000);
    return () => clearInterval(timer);
  }, [isStreaming, isRecording]);

  // Audio Meter Loop (Web Audio API Analyser)
  useEffect(() => {
    let animId: number;
    const updateMeters = () => {
      if (analyserRef.current && micStream && !channels.find((c) => c.id === 'ch_mic')?.isMuted) {
        const dataArray = new Uint8Array(analyserRef.current.fftSize);
        analyserRef.current.getByteTimeDomainData(dataArray);

        let sumSquares = 0;
        let maxVal = 0;
        for (let i = 0; i < dataArray.length; i++) {
          const norm = (dataArray[i] - 128) / 128;
          const abs = Math.abs(norm);
          if (abs > maxVal) maxVal = abs;
          sumSquares += norm * norm;
        }

        const rms = Math.sqrt(sumSquares / dataArray.length);
        const peakRatio = Math.max(0.0001, maxVal);
        const peakDb = Math.max(-60, Math.min(0, 20 * Math.log10(peakRatio)));

        setChannels((prev) =>
          prev.map((c) => (c.id === 'ch_mic' ? { ...c, peakDb, rmsLevel: rms * c.volume } : c))
        );
      }
      animId = requestAnimationFrame(updateMeters);
    };

    animId = requestAnimationFrame(updateMeters);
    return () => cancelAnimationFrame(animId);
  }, [micStream, channels]);

  // Hardware Screen Capture
  const handleToggleScreenCapture = async () => {
    if (screenStream) {
      screenStream.getTracks().forEach((t) => t.stop());
      setScreenStream(null);
    } else {
      try {
        const stream = await navigator.mediaDevices.getDisplayMedia({
          video: { frameRate: 60 },
          audio: true,
        });
        setScreenStream(stream);
        stream.getVideoTracks()[0].onended = () => {
          setScreenStream(null);
        };
      } catch (e) {
        console.warn('Screen capture cancelled or unavailable:', e);
      }
    }
  };

  // Hardware Camera Capture
  const handleToggleCamera = async () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((t) => t.stop());
      setCameraStream(null);
    } else {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { width: 1280, height: 720 },
          audio: false,
        });
        setCameraStream(stream);
      } catch (e) {
        console.warn('Camera access denied or unavailable:', e);
      }
    }
  };

  // Hardware Microphone Capture
  const handleToggleMic = async () => {
    if (micStream) {
      micStream.getTracks().forEach((t) => t.stop());
      setMicStream(null);
      setChannels((prev) =>
        prev.map((c) => (c.id === 'ch_mic' ? { ...c, statusText: 'Offline', peakDb: -60, rmsLevel: 0 } : c))
      );
    } else {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        setMicStream(stream);

        const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
        const ctx = new AudioCtx();
        const source = ctx.createMediaStreamSource(stream);
        const analyser = ctx.createAnalyser();
        analyser.fftSize = 512;
        source.connect(analyser);

        audioContextRef.current = ctx;
        analyserRef.current = analyser;

        setChannels((prev) =>
          prev.map((c) => (c.id === 'ch_mic' ? { ...c, statusText: 'Active' } : c))
        );
      } catch (e) {
        console.warn('Microphone access denied:', e);
      }
    }
  };

  // Real Recording Engine using Canvas MediaStream & MediaRecorder
  const handleToggleRecording = () => {
    if (isRecording) {
      // Stop recording
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
      setIsRecording(false);
    } else {
      // Start recording
      const canvas = canvasRef.current;
      if (!canvas) return;

      const canvasStream = canvas.captureStream(60);

      // If microphone is active, attach its audio track
      if (micStream) {
        micStream.getAudioTracks().forEach((track) => canvasStream.addTrack(track));
      }

      const mimeType = MediaRecorder.isTypeSupported('video/mp4')
        ? 'video/mp4'
        : MediaRecorder.isTypeSupported('video/webm;codecs=h264')
        ? 'video/webm;codecs=h264'
        : 'video/webm';

      try {
        const recorder = new MediaRecorder(canvasStream, {
          mimeType,
          videoBitsPerSecond: currentProfile.bitrateKbps * 1000,
        });

        recordedChunksRef.current = [];
        recordStartTimeRef.current = Date.now();

        recorder.ondataavailable = (e) => {
          if (e.data && e.data.size > 0) {
            recordedChunksRef.current.push(e.data);
          }
        };

        recorder.onstop = () => {
          const blob = new Blob(recordedChunksRef.current, { type: mimeType });
          const url = URL.createObjectURL(blob);
          const now = new Date();
          const timestampStr = now.toISOString().replace(/[:.]/g, '-').slice(0, 19);
          const ext = mimeType.includes('mp4') ? 'mp4' : 'webm';
          const newRecording: RecordingEntry = {
            id: `rec_${Date.now()}`,
            filename: `DarkAlise_${timestampStr}.${ext}`,
            timestamp: Date.now(),
            durationSec: recordDurationSec || Math.max(1, Math.round((Date.now() - recordStartTimeRef.current) / 1000)),
            fileSizeBytes: blob.size,
            blobUrl: url,
            blob,
            resolution: currentProfile.resolution,
          };
          setRecordings((prev) => [newRecording, ...prev]);
          setRecordDurationSec(0);
        };

        recorder.start(1000);
        mediaRecorderRef.current = recorder;
        setIsRecording(true);
        setRecordDurationSec(0);
      } catch (e) {
        console.error('Failed to initialize MediaRecorder:', e);
      }
    }
  };

  // Streaming toggle
  const handleToggleStream = () => {
    if (isStreaming) {
      setIsStreaming(false);
      setStreamDurationSec(0);
    } else {
      setIsStreaming(true);
      setStreamDurationSec(0);
    }
  };

  // Keyboard Hotkeys Listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'F1' && scenes[0]) {
        e.preventDefault();
        setActiveSceneId(scenes[0].id);
      } else if (e.key === 'F2' && scenes[1]) {
        e.preventDefault();
        setActiveSceneId(scenes[1].id);
      } else if (e.key === 'F3' && scenes[2]) {
        e.preventDefault();
        setActiveSceneId(scenes[2].id);
      } else if (e.ctrlKey && e.shiftKey && e.key.toLowerCase() === 'r') {
        e.preventDefault();
        handleToggleRecording();
      } else if (e.ctrlKey && e.shiftKey && e.key.toLowerCase() === 's') {
        e.preventDefault();
        handleToggleStream();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  });

  return (
    <div className="min-h-screen bg-[#09090D] text-slate-100 flex flex-col font-sans selection:bg-purple-500/30">
      {/* Broadcast Header Bar */}
      <StudioHeader
        isStreaming={isStreaming}
        isRecording={isRecording}
        streamDurationSec={streamDurationSec}
        recordDurationSec={recordDurationSec}
        fps={fps}
        cpuPercent={cpuPercent}
        ramMb={ramMb}
        bitrateKbps={bitrateKbps}
        droppedFrames={droppedFrames}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        recordingsCount={recordings.length}
      />

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col">
        {activeTab === 'studio' && (
          <div className="flex-1 p-3 md:p-4 flex flex-col gap-3 max-w-[1600px] w-full mx-auto">
            {/* Upper Viewport: 16:9 Canvas Preview */}
            <StudioPreview
              canvasRef={canvasRef}
              activeScene={activeScene}
              sources={sources}
              screenStream={screenStream}
              cameraStream={cameraStream}
              cameraOverlay={cameraOverlay}
              setCameraOverlay={setCameraOverlay}
              isScreenCapturing={!!screenStream}
              isCameraActive={!!cameraStream}
              onToggleScreenCapture={handleToggleScreenCapture}
              onToggleCamera={handleToggleCamera}
            />

            {/* Lower Docking Panels: Scenes | Sources | Audio Mixer | Stream Deck */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
              {/* Panel 1: Scenes */}
              <SceneList
                scenes={scenes}
                activeSceneId={activeSceneId}
                onSelectScene={(id) => setActiveSceneId(id)}
                onCreateScene={(name) => {
                  const newScene: SceneItem = {
                    id: `sc_${Date.now()}`,
                    name,
                    sourceIds: ['src_display', 'src_cam'],
                  };
                  setScenes((s) => [...s, newScene]);
                  setActiveSceneId(newScene.id);
                }}
                onDuplicateScene={(id) => {
                  const orig = scenes.find((s) => s.id === id);
                  if (!orig) return;
                  const copy: SceneItem = {
                    id: `sc_${Date.now()}`,
                    name: `${orig.name} (Copy)`,
                    sourceIds: [...orig.sourceIds],
                  };
                  setScenes((s) => [...s, copy]);
                }}
                onDeleteScene={(id) => {
                  if (scenes.length <= 1) return;
                  setScenes((s) => s.filter((item) => item.id !== id));
                  if (activeSceneId === id) {
                    setActiveSceneId(scenes[0].id);
                  }
                }}
                onRenameScene={(id, newName) => {
                  setScenes((s) => s.map((item) => (item.id === id ? { ...item, name: newName } : item)));
                }}
                transitionType={transitionType}
                setTransitionType={setTransitionType}
              />

              {/* Panel 2: Sources */}
              <SourceList
                sources={sources}
                activeSourceIds={activeScene.sourceIds}
                onToggleVisibility={(id) => {
                  setSources((s) => s.map((item) => (item.id === id ? { ...item, isVisible: !item.isVisible } : item)));
                }}
                onToggleLock={(id) => {
                  setSources((s) => s.map((item) => (item.id === id ? { ...item, isLocked: !item.isLocked } : item)));
                }}
                onMoveUp={(id) => {
                  const ids = [...activeScene.sourceIds];
                  const idx = ids.indexOf(id);
                  if (idx > 0) {
                    const temp = ids[idx];
                    ids[idx] = ids[idx - 1];
                    ids[idx - 1] = temp;
                    setScenes((s) => s.map((item) => (item.id === activeScene.id ? { ...item, sourceIds: ids } : item)));
                  }
                }}
                onMoveDown={(id) => {
                  const ids = [...activeScene.sourceIds];
                  const idx = ids.indexOf(id);
                  if (idx < ids.length - 1 && idx >= 0) {
                    const temp = ids[idx];
                    ids[idx] = ids[idx + 1];
                    ids[idx + 1] = temp;
                    setScenes((s) => s.map((item) => (item.id === activeScene.id ? { ...item, sourceIds: ids } : item)));
                  }
                }}
                onAddSource={(name, type, text) => {
                  const newSource: SourceItem = {
                    id: `src_${Date.now()}`,
                    name,
                    type,
                    isVisible: true,
                    isLocked: false,
                    isMuted: false,
                    volume: 1.0,
                    transform: { x: 10, y: 10, width: 30, height: 10, scale: 1, rotation: 0, opacity: 1 },
                    extra: text ? { text, textColor: '#FFFFFF' } : undefined,
                  };
                  setSources((s) => [...s, newSource]);
                  setScenes((s) =>
                    s.map((item) => (item.id === activeScene.id ? { ...item, sourceIds: [...item.sourceIds, newSource.id] } : item))
                  );
                }}
              />

              {/* Panel 3: Audio Mixer */}
              <AudioMixer
                channels={channels}
                onVolumeChange={(id, volume) => {
                  setChannels((c) => c.map((item) => (item.id === id ? { ...item, volume } : item)));
                }}
                onToggleMute={(id) => {
                  setChannels((c) => c.map((item) => (item.id === id ? { ...item, isMuted: !item.isMuted } : item)));
                }}
                isMicActive={!!micStream}
                onToggleMic={handleToggleMic}
              />

              {/* Panel 4: Stream Deck Control Panel */}
              <StreamDeck
                isStreaming={isStreaming}
                isRecording={isRecording}
                onToggleStream={handleToggleStream}
                onToggleRecording={handleToggleRecording}
                scenes={scenes}
                activeSceneId={activeSceneId}
                onSelectScene={(id) => setActiveSceneId(id)}
                isMicMuted={channels.find((c) => c.id === 'ch_mic')?.isMuted || false}
                onToggleMuteMic={() => {
                  setChannels((c) => c.map((item) => (item.id === 'ch_mic' ? { ...item, isMuted: !item.isMuted } : item)));
                }}
                isScreenCapturing={!!screenStream}
                onToggleScreenCapture={handleToggleScreenCapture}
                isCameraActive={!!cameraStream}
                onToggleCamera={handleToggleCamera}
              />
            </div>
          </div>
        )}

        {activeTab === 'recordings' && (
          <RecordingsManager
            recordings={recordings}
            onDeleteRecording={(id) => setRecordings((r) => r.filter((item) => item.id !== id))}
            onRenameRecording={(id, newName) =>
              setRecordings((r) => r.map((item) => (item.id === id ? { ...item, filename: newName } : item)))
            }
          />
        )}

        {activeTab === 'settings' && (
          <SettingsModal
            currentProfile={currentProfile}
            onSelectProfile={setCurrentProfile}
            streamUrl={streamUrl}
            setStreamUrl={setStreamUrl}
            streamKey={streamKey}
            setStreamKey={setStreamKey}
          />
        )}

        {activeTab === 'android' && <AndroidCodeHub />}

        {activeTab === 'privacy' && <PrivacyView />}
      </main>
    </div>
  );
}
