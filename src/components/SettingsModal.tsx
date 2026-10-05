import React, { useState } from 'react';
import { ObsProfilePreset } from '../types/obs';
import { Sliders, Video, Mic, Radio, Layers, Download, AlertTriangle } from 'lucide-react';

interface SettingsModalProps {
  currentProfile: ObsProfilePreset;
  onSelectProfile: (profile: ObsProfilePreset) => void;
  streamUrl: string;
  setStreamUrl: (url: string) => void;
  streamKey: string;
  setStreamKey: (key: string) => void;
}

export const defaultProfiles: ObsProfilePreset[] = [
  {
    id: 'p_1080p_rec',
    name: '1080p Recording',
    resolution: '1920x1080',
    width: 1920,
    height: 1080,
    fps: 60,
    bitrateKbps: 12000,
    codec: 'H.264 (Hardware AVC)',
  },
  {
    id: 'p_720p_rec',
    name: '720p Recording',
    resolution: '1280x720',
    width: 1280,
    height: 720,
    fps: 60,
    bitrateKbps: 6000,
    codec: 'H.264 (Hardware AVC)',
  },
  {
    id: 'p_1080p_stream',
    name: '1080p Live Streaming',
    resolution: '1920x1080',
    width: 1920,
    height: 1080,
    fps: 60,
    bitrateKbps: 6000,
    codec: 'H.264 (CBR Stream)',
  },
  {
    id: 'p_mobile_game',
    name: 'Mobile Gaming',
    resolution: '1280x720',
    width: 1280,
    height: 720,
    fps: 60,
    bitrateKbps: 4500,
    codec: 'H.264 (Hardware AVC)',
  },
  {
    id: 'p_low_end',
    name: 'Low-End Device Mode',
    resolution: '854x480',
    width: 854,
    height: 480,
    fps: 30,
    bitrateKbps: 2000,
    codec: 'H.264 Baseline',
  },
];

export const SettingsModal: React.FC<SettingsModalProps> = ({
  currentProfile,
  onSelectProfile,
  streamUrl,
  setStreamUrl,
  streamKey,
  setStreamKey,
}) => {
  const [activeTab, setActiveTab] = useState<'video' | 'audio' | 'stream' | 'profiles' | 'export' | 'virtualcam'>('video');
  const [exportedJson, setExportedJson] = useState('');
  const [importJsonText, setImportJsonText] = useState('');
  const [importMessage, setImportMessage] = useState('');

  const handleExport = () => {
    const config = {
      appName: 'Dark Alise OBS',
      version: '1.0.0',
      activeProfile: currentProfile,
      streamUrl: streamUrl,
      // Stream key excluded by default for security
    };
    setExportedJson(JSON.stringify(config, null, 2));
  };

  const handleImport = () => {
    try {
      const parsed = JSON.parse(importJsonText);
      if (parsed.activeProfile) {
        onSelectProfile(parsed.activeProfile);
        if (parsed.streamUrl) setStreamUrl(parsed.streamUrl);
        setImportMessage('Profile imported successfully!');
      } else {
        setImportMessage('Invalid profile JSON structure.');
      }
    } catch (e) {
      setImportMessage('Error parsing JSON. Check syntax.');
    }
  };

  return (
    <div className="flex-1 p-6 flex flex-col gap-6 max-w-5xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-bold bg-gradient-to-r from-purple-400 to-fuchsia-400 bg-clip-text text-transparent">
          Studio Settings & Profiles
        </h1>
        <p className="text-xs text-slate-400">
          Configure video rendering parameters, RTMP live endpoints, audio architecture, and device presets.
        </p>
      </div>

      <div className="bg-[#121118] rounded-xl border border-[#2E2A42] overflow-hidden flex flex-col md:flex-row shadow-2xl">
        {/* Settings Sidebar Tabs */}
        <div className="w-full md:w-56 bg-[#0D0C14] border-b md:border-b-0 md:border-r border-[#2E2A42] p-2 flex flex-col gap-1">
          <button
            onClick={() => setActiveTab('video')}
            className={`px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-colors ${
              activeTab === 'video'
                ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Video className="w-4 h-4 text-purple-400" />
            <span>Video & Codec</span>
          </button>

          <button
            onClick={() => setActiveTab('audio')}
            className={`px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-colors ${
              activeTab === 'audio'
                ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Mic className="w-4 h-4 text-emerald-400" />
            <span>Audio Architecture</span>
          </button>

          <button
            onClick={() => setActiveTab('stream')}
            className={`px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-colors ${
              activeTab === 'stream'
                ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Radio className="w-4 h-4 text-fuchsia-400" />
            <span>RTMP / RTMPS Stream</span>
          </button>

          <button
            onClick={() => setActiveTab('profiles')}
            className={`px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-colors ${
              activeTab === 'profiles'
                ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Sliders className="w-4 h-4 text-blue-400" />
            <span>Profiles Presets</span>
          </button>

          <button
            onClick={() => setActiveTab('export')}
            className={`px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-colors ${
              activeTab === 'export'
                ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Download className="w-4 h-4 text-amber-400" />
            <span>Export & Import JSON</span>
          </button>

          <button
            onClick={() => setActiveTab('virtualcam')}
            className={`px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 transition-colors ${
              activeTab === 'virtualcam'
                ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Layers className="w-4 h-4 text-slate-400" />
            <span>Virtual Camera</span>
          </button>
        </div>

        {/* Settings Body */}
        <div className="flex-1 p-6">
          {activeTab === 'video' && (
            <div className="flex flex-col gap-4">
              <h2 className="text-sm font-bold text-slate-200 border-b border-[#2E2A42] pb-2">
                Video Hardware & Canvas Pipeline
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-xs text-slate-400">Canvas Base Resolution</span>
                  <span className="text-sm font-semibold text-slate-200 font-mono">
                    {currentProfile.resolution} ({currentProfile.width}x{currentProfile.height})
                  </span>
                </div>
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-xs text-slate-400">Frame Rate</span>
                  <span className="text-sm font-semibold text-purple-400 font-mono">
                    {currentProfile.fps} FPS
                  </span>
                </div>
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-xs text-slate-400">Target Video Bitrate</span>
                  <span className="text-sm font-semibold text-slate-200 font-mono">
                    {currentProfile.bitrateKbps} kbps
                  </span>
                </div>
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-xs text-slate-400">Hardware Encoder</span>
                  <span className="text-sm font-semibold text-emerald-400 font-mono">
                    {currentProfile.codec}
                  </span>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'audio' && (
            <div className="flex flex-col gap-4">
              <h2 className="text-sm font-bold text-slate-200 border-b border-[#2E2A42] pb-2">
                Audio Engine Configuration
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-slate-400">PCM Sample Rate</span>
                  <span className="text-sm font-semibold text-slate-200 font-mono">48,000 Hz / 44,100 Hz</span>
                </div>
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-slate-400">Channel Mode</span>
                  <span className="text-sm font-semibold text-slate-200 font-mono">Stereo (2 Channels)</span>
                </div>
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-slate-400">Audio Bitrate</span>
                  <span className="text-sm font-semibold text-slate-200 font-mono">160 kbps AAC-LC</span>
                </div>
                <div className="bg-[#181622] p-3 rounded-lg border border-[#2E2A42] flex flex-col gap-1">
                  <span className="text-slate-400">Internal Audio Capture</span>
                  <span className="text-sm font-semibold text-purple-400 font-mono">AudioPlaybackCapture (Android 10+)</span>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'stream' && (
            <div className="flex flex-col gap-4">
              <h2 className="text-sm font-bold text-slate-200 border-b border-[#2E2A42] pb-2">
                Live RTMP Ingest Settings
              </h2>
              <div className="flex flex-col gap-3">
                <div>
                  <label className="text-xs text-slate-400 block mb-1">Server URL (rtmp:// or rtmps://)</label>
                  <input
                    type="text"
                    value={streamUrl}
                    onChange={(e) => setStreamUrl(e.target.value)}
                    className="w-full bg-[#181622] border border-[#2E2A42] rounded px-3 py-2 text-xs text-white outline-none focus:border-purple-500 font-mono"
                    placeholder="rtmp://live.twitch.tv/app/"
                  />
                </div>

                <div>
                  <label className="text-xs text-slate-400 block mb-1">Stream Key (Stored locally)</label>
                  <input
                    type="password"
                    value={streamKey}
                    onChange={(e) => setStreamKey(e.target.value)}
                    className="w-full bg-[#181622] border border-[#2E2A42] rounded px-3 py-2 text-xs text-white outline-none focus:border-purple-500 font-mono"
                    placeholder="live_..."
                  />
                </div>
              </div>
            </div>
          )}

          {activeTab === 'profiles' && (
            <div className="flex flex-col gap-4">
              <h2 className="text-sm font-bold text-slate-200 border-b border-[#2E2A42] pb-2">
                Broadcast Profiles
              </h2>
              <div className="flex flex-col gap-2">
                {defaultProfiles.map((p) => {
                  const isSelected = p.id === currentProfile.id;
                  return (
                    <div
                      key={p.id}
                      className={`p-3 rounded-lg border flex items-center justify-between text-xs transition-colors ${
                        isSelected
                          ? 'bg-purple-600/20 border-purple-500 text-purple-200'
                          : 'bg-[#181622] border-[#2E2A42] text-slate-300 hover:border-slate-600'
                      }`}
                    >
                      <div className="flex flex-col gap-0.5">
                        <span className="font-semibold text-sm text-slate-100">{p.name}</span>
                        <span className="text-[11px] text-slate-400 font-mono">
                          {p.resolution} @ {p.fps}fps · {p.bitrateKbps} kbps · {p.codec}
                        </span>
                      </div>
                      <button
                        onClick={() => onSelectProfile(p)}
                        className={`px-3 py-1.5 rounded text-xs font-semibold ${
                          isSelected
                            ? 'bg-purple-600 text-white cursor-default'
                            : 'bg-[#252236] hover:bg-[#34304B] text-slate-200'
                        }`}
                      >
                        {isSelected ? 'Active' : 'Apply'}
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {activeTab === 'export' && (
            <div className="flex flex-col gap-4">
              <h2 className="text-sm font-bold text-slate-200 border-b border-[#2E2A42] pb-2">
                Export / Import Configuration
              </h2>
              <div className="flex flex-col gap-3">
                <button
                  onClick={handleExport}
                  className="w-fit px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-semibold"
                >
                  Generate Configuration JSON
                </button>
                {exportedJson && (
                  <textarea
                    rows={6}
                    readOnly
                    value={exportedJson}
                    className="w-full bg-[#181622] border border-[#2E2A42] rounded p-2 text-xs font-mono text-purple-300 outline-none"
                  />
                )}

                <div className="border-t border-[#2E2A42] pt-3 flex flex-col gap-2">
                  <span className="text-xs text-slate-400">Import Configuration JSON</span>
                  <textarea
                    rows={4}
                    placeholder="Paste exported JSON here..."
                    value={importJsonText}
                    onChange={(e) => setImportJsonText(e.target.value)}
                    className="w-full bg-[#181622] border border-[#2E2A42] rounded p-2 text-xs font-mono text-slate-200 outline-none focus:border-purple-500"
                  />
                  <button
                    onClick={handleImport}
                    className="w-fit px-4 py-1.5 bg-[#252236] hover:bg-[#34304B] text-slate-200 rounded text-xs font-semibold"
                  >
                    Import Settings
                  </button>
                  {importMessage && (
                    <span className="text-xs font-mono text-purple-400">{importMessage}</span>
                  )}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'virtualcam' && (
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-2 text-amber-400">
                <AlertTriangle className="w-5 h-5" />
                <h3 className="font-bold text-sm">Virtual Camera Notice</h3>
              </div>
              <p className="text-xs text-slate-300 font-semibold bg-[#181622] p-3 rounded-lg border border-[#2E2A42]">
                &quot;Virtual camera output is not supported on this Android configuration.&quot;
              </p>
              <p className="text-xs text-slate-400 leading-relaxed">
                Standard Android security and sandboxing architecture does not permit non-system third-party applications to inject mock frames into the system-wide camera hardware abstraction layer without root or OEM platform signing. Dark Alise OBS adheres strictly to real, public Android APIs without dangerous bypasses.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
