import React from 'react';
import { Volume2, VolumeX, Mic, MicOff } from 'lucide-react';
import { AudioChannel } from '../types/obs';

interface AudioMixerProps {
  channels: AudioChannel[];
  onVolumeChange: (id: string, volume: number) => void;
  onToggleMute: (id: string) => void;
  isMicActive: boolean;
  onToggleMic: () => void;
}

export const AudioMixer: React.FC<AudioMixerProps> = ({
  channels,
  onVolumeChange,
  onToggleMute,
  isMicActive,
  onToggleMic,
}) => {
  return (
    <div className="bg-[#121118] rounded-xl border border-[#2E2A42] p-3 flex flex-col gap-2 shadow-lg">
      <div className="flex items-center justify-between pb-2 border-b border-[#2E2A42]">
        <div className="flex items-center gap-2">
          <span className="text-purple-400 font-bold uppercase tracking-wider text-[11px]">
            Audio Mixer
          </span>
          <span className="text-[10px] text-slate-500 font-mono">dBFS PEAK / RMS</span>
        </div>
        <button
          onClick={onToggleMic}
          className={`px-2 py-0.5 rounded text-[10px] font-medium flex items-center gap-1 transition-colors ${
            isMicActive
              ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
              : 'bg-[#1E1B2E] text-slate-400 hover:text-slate-200'
          }`}
          title="Toggle Hardware Microphone"
        >
          {isMicActive ? <Mic className="w-3 h-3 text-purple-400" /> : <MicOff className="w-3 h-3 text-red-400" />}
          <span>{isMicActive ? 'Mic Active' : 'Enable Mic'}</span>
        </button>
      </div>

      <div className="flex flex-col gap-2.5">
        {channels.map((channel) => {
          const isMuted = channel.isMuted;
          const displayDb = isMuted ? -60 : channel.peakDb;
          const fillWidth = isMuted ? 0 : Math.min(100, Math.max(0, channel.rmsLevel * 100));

          // Color calculation
          let meterColor = 'bg-emerald-500';
          if (displayDb > -3) {
            meterColor = 'bg-red-500';
          } else if (displayDb > -12) {
            meterColor = 'bg-amber-400';
          }

          return (
            <div
              key={channel.id}
              className="bg-[#181622] rounded-lg border border-[#252236] p-2.5 flex flex-col gap-1.5 transition-colors hover:border-[#383352]"
            >
              {/* Channel Header */}
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => onToggleMute(channel.id)}
                    className={`p-1 rounded transition-colors ${
                      isMuted
                        ? 'bg-red-500/20 text-red-400'
                        : 'text-purple-400 hover:bg-purple-500/10'
                    }`}
                    title={isMuted ? 'Unmute' : 'Mute'}
                  >
                    {isMuted ? <VolumeX className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5" />}
                  </button>
                  <span className="font-semibold text-slate-200 text-[11px]">{channel.name}</span>
                </div>

                <div className="flex items-center gap-2 font-mono text-[10px]">
                  <span className="text-slate-500">{channel.statusText}</span>
                  <span className={`font-bold tabular-nums ${isMuted ? 'text-red-400' : 'text-slate-300'}`}>
                    {isMuted ? 'MUTED' : `${displayDb.toFixed(1)} dB`}
                  </span>
                </div>
              </div>

              {/* Peak dBFS Level Bar */}
              <div className="relative w-full h-2 bg-[#09090D] rounded-full overflow-hidden border border-[#222033]">
                <div
                  style={{ width: `${fillWidth}%` }}
                  className={`h-full rounded-full transition-all duration-75 ${meterColor}`}
                />
                {/* Visual decibel markers */}
                <div className="absolute top-0 bottom-0 left-[60%] w-[1px] bg-slate-700/50" title="-12dB" />
                <div className="absolute top-0 bottom-0 left-[85%] w-[1px] bg-red-700/60" title="-3dB" />
              </div>

              {/* Volume Slider & Gain */}
              <div className="flex items-center gap-2 pt-0.5">
                <input
                  type="range"
                  min="0"
                  max="1.5"
                  step="0.01"
                  value={channel.volume}
                  onChange={(e) => onVolumeChange(channel.id, parseFloat(e.target.value))}
                  disabled={isMuted}
                  className="w-full h-1 bg-[#252236] rounded-lg appearance-none cursor-pointer accent-purple-500 disabled:opacity-40"
                />
                <span className="text-[10px] font-mono tabular-nums text-slate-400 w-10 text-right">
                  {Math.round(channel.volume * 100)}%
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
