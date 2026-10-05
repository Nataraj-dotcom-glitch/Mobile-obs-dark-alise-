import React from 'react';
import { Radio, Disc, Cpu, Activity, Download } from 'lucide-react';

interface StudioHeaderProps {
  isStreaming: boolean;
  isRecording: boolean;
  streamDurationSec: number;
  recordDurationSec: number;
  fps: number;
  cpuPercent: number;
  ramMb: number;
  bitrateKbps: number;
  droppedFrames: number;
  activeTab: 'studio' | 'recordings' | 'settings' | 'android' | 'privacy';
  setActiveTab: (tab: 'studio' | 'recordings' | 'settings' | 'android' | 'privacy') => void;
  recordingsCount: number;
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

export const StudioHeader: React.FC<StudioHeaderProps> = ({
  isStreaming,
  isRecording,
  streamDurationSec,
  recordDurationSec,
  fps,
  cpuPercent,
  ramMb,
  bitrateKbps,
  droppedFrames,
  activeTab,
  setActiveTab,
  recordingsCount,
}) => {
  return (
    <header className="bg-[#09090D] border-b border-[#2E2A42] px-4 py-2 flex flex-wrap items-center justify-between gap-3 text-xs select-none">
      {/* Brand Zone */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <span className="font-extrabold tracking-wider text-sm bg-gradient-to-r from-purple-400 to-fuchsia-400 bg-clip-text text-transparent">
            DARK ALISE OBS
          </span>
          <span className="text-[10px] text-slate-500 font-mono hidden sm:inline">
            MOBILE BROADCAST STUDIO
          </span>
        </div>

        {/* Status Indicators */}
        <div className="flex items-center gap-2 font-mono">
          {isStreaming ? (
            <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-bold animate-pulse">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span>LIVE {formatDuration(streamDurationSec)}</span>
            </div>
          ) : isRecording ? (
            <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-red-500/10 border border-red-500/30 text-red-400 font-bold animate-pulse">
              <span className="w-2 h-2 rounded-full bg-red-500" />
              <span>REC {formatDuration(recordDurationSec)}</span>
            </div>
          ) : (
            <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-slate-800/60 border border-slate-700/50 text-slate-400">
              <span className="w-2 h-2 rounded-full bg-slate-500" />
              <span>IDLE</span>
            </div>
          )}
        </div>
      </div>

      {/* Center Nav Tabs */}
      <nav className="flex items-center gap-1 bg-[#121118] p-1 rounded-lg border border-[#2E2A42]">
        <button
          onClick={() => setActiveTab('studio')}
          className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
            activeTab === 'studio'
              ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40 shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Studio
        </button>
        <button
          onClick={() => setActiveTab('recordings')}
          className={`px-3 py-1 rounded text-xs font-medium transition-colors flex items-center gap-1.5 ${
            activeTab === 'recordings'
              ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40 shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Recordings</span>
          {recordingsCount > 0 && (
            <span className="px-1.5 py-0.2 rounded-full bg-purple-500 text-black text-[10px] font-bold">
              {recordingsCount}
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveTab('settings')}
          className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
            activeTab === 'settings'
              ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40 shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Settings
        </button>
        <button
          onClick={() => setActiveTab('android')}
          className={`px-3 py-1 rounded text-xs font-medium transition-colors flex items-center gap-1 ${
            activeTab === 'android'
              ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40 shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Download className="w-3 h-3 text-purple-400" />
          <span>Android APK & Source</span>
        </button>
        <button
          onClick={() => setActiveTab('privacy')}
          className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
            activeTab === 'privacy'
              ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40 shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Privacy
        </button>
      </nav>

      {/* Right Stats Telemetry */}
      <div className="flex items-center gap-3 text-[11px] font-mono tabular-nums text-slate-400">
        <div className="flex items-center gap-1">
          <Activity className="w-3.5 h-3.5 text-purple-400" />
          <span>FPS: <strong className="text-slate-200">{fps}</strong></span>
        </div>
        <div className="hidden md:flex items-center gap-1">
          <Cpu className="w-3.5 h-3.5 text-blue-400" />
          <span>CPU: <strong className="text-slate-200">{cpuPercent}%</strong></span>
        </div>
        <div className="hidden lg:flex items-center gap-1">
          <span>RAM: <strong className="text-slate-200">{ramMb}MB</strong></span>
        </div>
        <div className="hidden sm:flex items-center gap-1">
          <span>RATE: <strong className="text-slate-200">{bitrateKbps}k</strong></span>
        </div>
        <div className="flex items-center gap-1">
          <span>DROP: <strong className={droppedFrames > 0 ? 'text-red-400 font-bold' : 'text-slate-200'}>{droppedFrames}</strong></span>
        </div>
      </div>
    </header>
  );
};
