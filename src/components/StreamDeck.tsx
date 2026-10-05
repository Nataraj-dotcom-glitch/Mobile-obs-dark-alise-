import React from 'react';
import { Play, Square, Mic, MicOff, Monitor, Camera, Radio, Disc, Sparkles } from 'lucide-react';
import { SceneItem } from '../types/obs';

interface StreamDeckProps {
  isStreaming: boolean;
  isRecording: boolean;
  onToggleStream: () => void;
  onToggleRecording: () => void;
  scenes: SceneItem[];
  activeSceneId: string;
  onSelectScene: (id: string) => void;
  isMicMuted: boolean;
  onToggleMuteMic: () => void;
  isScreenCapturing: boolean;
  onToggleScreenCapture: () => void;
  isCameraActive: boolean;
  onToggleCamera: () => void;
}

export const StreamDeck: React.FC<StreamDeckProps> = ({
  isStreaming,
  isRecording,
  onToggleStream,
  onToggleRecording,
  scenes,
  activeSceneId,
  onSelectScene,
  isMicMuted,
  onToggleMuteMic,
  isScreenCapturing,
  onToggleCamera,
  isCameraActive,
  onToggleScreenCapture,
}) => {
  return (
    <div className="bg-[#121118] rounded-xl border border-[#2E2A42] p-3 flex flex-col gap-2 shadow-lg">
      <div className="flex items-center justify-between pb-1 border-b border-[#2E2A42]">
        <span className="text-purple-400 font-bold uppercase tracking-wider text-[11px]">
          Stream Deck Control Panel
        </span>
        <span className="text-[10px] text-slate-500 font-mono">HOTKEYS SYNCED</span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-2">
        {/* Stream Toggle */}
        <button
          onClick={onToggleStream}
          className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all text-xs font-bold active:scale-95 ${
            isStreaming
              ? 'bg-red-600/30 border-red-500 text-red-300 shadow-lg shadow-red-900/30'
              : 'bg-[#181622] border-[#2E2A42] text-slate-200 hover:border-purple-500 hover:text-purple-300'
          }`}
        >
          <Radio className={`w-5 h-5 ${isStreaming ? 'text-red-400 animate-pulse' : 'text-purple-400'}`} />
          <span>{isStreaming ? 'STOP STREAM' : 'START STREAM'}</span>
        </button>

        {/* Record Toggle */}
        <button
          onClick={onToggleRecording}
          className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all text-xs font-bold active:scale-95 ${
            isRecording
              ? 'bg-red-600/30 border-red-500 text-red-300 shadow-lg shadow-red-900/30'
              : 'bg-[#181622] border-[#2E2A42] text-slate-200 hover:border-purple-500 hover:text-purple-300'
          }`}
        >
          <Disc className={`w-5 h-5 ${isRecording ? 'text-red-400 animate-pulse' : 'text-fuchsia-400'}`} />
          <span>{isRecording ? 'STOP REC' : 'RECORD MP4'}</span>
        </button>

        {/* Mic Mute Toggle */}
        <button
          onClick={onToggleMuteMic}
          className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all text-xs font-bold active:scale-95 ${
            isMicMuted
              ? 'bg-red-900/30 border-red-600 text-red-400'
              : 'bg-[#181622] border-[#2E2A42] text-slate-200 hover:border-purple-500'
          }`}
        >
          {isMicMuted ? <MicOff className="w-5 h-5 text-red-400" /> : <Mic className="w-5 h-5 text-emerald-400" />}
          <span>{isMicMuted ? 'UNMUTE MIC' : 'MUTE MIC'}</span>
        </button>

        {/* Screen Capture Toggle */}
        <button
          onClick={onToggleScreenCapture}
          className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all text-xs font-bold active:scale-95 ${
            isScreenCapturing
              ? 'bg-purple-600/30 border-purple-500 text-purple-300'
              : 'bg-[#181622] border-[#2E2A42] text-slate-200 hover:border-purple-500'
          }`}
        >
          <Monitor className="w-5 h-5 text-blue-400" />
          <span>{isScreenCapturing ? 'SCREEN ACTIVE' : 'SHARE SCREEN'}</span>
        </button>

        {/* Camera Toggle */}
        <button
          onClick={onToggleCamera}
          className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all text-xs font-bold active:scale-95 ${
            isCameraActive
              ? 'bg-purple-600/30 border-purple-500 text-purple-300'
              : 'bg-[#181622] border-[#2E2A42] text-slate-200 hover:border-purple-500'
          }`}
        >
          <Camera className="w-5 h-5 text-purple-400" />
          <span>{isCameraActive ? 'CAM ACTIVE' : 'ENABLE CAM'}</span>
        </button>

        {/* Quick Scenes */}
        {scenes.slice(0, 3).map((scene, idx) => {
          const isSelected = scene.id === activeSceneId;
          return (
            <button
              key={scene.id}
              onClick={() => onSelectScene(scene.id)}
              className={`p-3 rounded-xl border flex flex-col items-center justify-center gap-1.5 transition-all text-xs font-bold active:scale-95 ${
                isSelected
                  ? 'bg-purple-600/40 border-purple-400 text-purple-200 shadow-md'
                  : 'bg-[#181622] border-[#2E2A42] text-slate-300 hover:border-purple-500/50'
              }`}
            >
              <span className="text-[10px] font-mono text-purple-400">F{idx + 1}</span>
              <span className="truncate">{scene.name}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
};
