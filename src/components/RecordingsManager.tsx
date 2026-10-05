import React, { useState } from 'react';
import { Play, Download, Trash2, Edit2, Check, Video, Clock, HardDrive, Calendar } from 'lucide-react';
import { RecordingEntry } from '../types/obs';

interface RecordingsManagerProps {
  recordings: RecordingEntry[];
  onDeleteRecording: (id: string) => void;
  onRenameRecording: (id: string, newName: string) => void;
}

export const RecordingsManager: React.FC<RecordingsManagerProps> = ({
  recordings,
  onDeleteRecording,
  onRenameRecording,
}) => {
  const [playingItem, setPlayingItem] = useState<RecordingEntry | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(1)} MB`;
  };

  const formatDuration = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const handleSaveRename = (id: string) => {
    if (editName.trim()) {
      onRenameRecording(id, editName.trim());
    }
    setEditingId(null);
  };

  return (
    <div className="flex-1 p-6 flex flex-col gap-6 max-w-6xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-bold bg-gradient-to-r from-purple-400 to-fuchsia-400 bg-clip-text text-transparent">
          Recordings Library
        </h1>
        <p className="text-xs text-slate-400">
          Hardware-encoded MP4/WebM broadcast captures created with Dark Alise OBS.
        </p>
      </div>

      {/* Video Player Modal */}
      {playingItem && (
        <div className="bg-[#121118] border border-purple-500/40 rounded-xl p-4 flex flex-col gap-3 shadow-2xl">
          <div className="flex items-center justify-between pb-2 border-b border-[#2E2A42]">
            <div className="flex items-center gap-2">
              <Video className="w-4 h-4 text-purple-400" />
              <span className="font-semibold text-sm text-slate-200">{playingItem.filename}</span>
            </div>
            <button
              onClick={() => setPlayingItem(null)}
              className="px-3 py-1 bg-[#1A1826] hover:bg-[#28253A] text-slate-300 rounded text-xs"
            >
              Close Player
            </button>
          </div>
          <div className="aspect-video bg-black rounded-lg overflow-hidden border border-[#2E2A42]">
            <video
              src={playingItem.blobUrl}
              controls
              autoPlay
              className="w-full h-full object-contain"
            />
          </div>
        </div>
      )}

      {/* Recordings Grid */}
      {recordings.length === 0 ? (
        <div className="flex flex-col items-center justify-center p-12 bg-[#121118] rounded-2xl border border-[#2E2A42] text-center gap-3">
          <Video className="w-12 h-12 text-slate-600" />
          <h3 className="text-sm font-semibold text-slate-300">No recordings captured yet</h3>
          <p className="text-xs text-slate-500 max-w-sm">
            Press &quot;RECORD&quot; in the Studio or use the Stream Deck panel to record your program output.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {recordings.map((item) => (
            <div
              key={item.id}
              className="bg-[#121118] rounded-xl border border-[#2E2A42] overflow-hidden flex flex-col gap-3 p-4 hover:border-purple-500/50 transition-colors shadow-lg"
            >
              {/* Card Header & Filename */}
              <div className="flex items-start justify-between gap-2">
                {editingId === item.id ? (
                  <div className="flex items-center gap-1 flex-1">
                    <input
                      type="text"
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                      onKeyDown={(e) => e.key === 'Enter' && handleSaveRename(item.id)}
                      className="w-full bg-[#09090D] border border-purple-500 px-2 py-0.5 rounded text-white text-xs outline-none"
                      autoFocus
                    />
                    <button
                      onClick={() => handleSaveRename(item.id)}
                      className="p-1 text-emerald-400 hover:text-emerald-300"
                    >
                      <Check className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 flex-1 truncate">
                    <span className="font-semibold text-sm text-slate-200 truncate" title={item.filename}>
                      {item.filename}
                    </span>
                    <button
                      onClick={() => {
                        setEditingId(item.id);
                        setEditName(item.filename);
                      }}
                      className="text-slate-500 hover:text-slate-300 p-1"
                      title="Rename"
                    >
                      <Edit2 className="w-3 h-3" />
                    </button>
                  </div>
                )}
              </div>

              {/* Metadata Badges */}
              <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400 font-mono">
                <div className="flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5 text-purple-400" />
                  <span>{formatDuration(item.durationSec)}</span>
                </div>
                <div className="flex items-center gap-1">
                  <HardDrive className="w-3.5 h-3.5 text-blue-400" />
                  <span>{formatBytes(item.fileSizeBytes)}</span>
                </div>
                <div className="flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-fuchsia-400" />
                  <span>{new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-2 pt-2 border-t border-[#2E2A42]">
                <button
                  onClick={() => setPlayingItem(item)}
                  className="flex-1 py-1.5 px-3 bg-purple-600/30 hover:bg-purple-600/40 text-purple-300 border border-purple-500/40 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Play className="w-3.5 h-3.5" />
                  <span>Play</span>
                </button>

                <a
                  href={item.blobUrl}
                  download={item.filename.endsWith('.mp4') ? item.filename : `${item.filename}.mp4`}
                  className="p-2 bg-[#1A1826] hover:bg-[#252236] text-slate-300 rounded-lg transition-colors"
                  title="Download MP4 Video"
                >
                  <Download className="w-3.5 h-3.5" />
                </a>

                <button
                  onClick={() => onDeleteRecording(item.id)}
                  className="p-2 bg-[#1A1826] hover:bg-red-950/40 text-red-400 rounded-lg transition-colors"
                  title="Delete Recording"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
