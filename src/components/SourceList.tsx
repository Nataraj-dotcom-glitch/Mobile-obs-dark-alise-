import React, { useState } from 'react';
import { Eye, EyeOff, Lock, Unlock, Plus, ArrowUp, ArrowDown, Trash2 } from 'lucide-react';
import { SourceItem, SourceType } from '../types/obs';

interface SourceListProps {
  sources: SourceItem[];
  activeSourceIds: string[];
  onToggleVisibility: (id: string) => void;
  onToggleLock: (id: string) => void;
  onMoveUp: (id: string) => void;
  onMoveDown: (id: string) => void;
  onAddSource: (name: string, type: SourceType, text?: string) => void;
}

export const SourceList: React.FC<SourceListProps> = ({
  sources,
  activeSourceIds,
  onToggleVisibility,
  onToggleLock,
  onMoveUp,
  onMoveDown,
  onAddSource,
}) => {
  const [isAdding, setIsAdding] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState<SourceType>('TEXT');
  const [extraText, setExtraText] = useState('DARK ALISE BROADCAST');

  const sceneSources = activeSourceIds
    .map((id) => sources.find((s) => s.id === id))
    .filter((s): s is SourceItem => !!s);

  const handleCreate = () => {
    if (name.trim()) {
      onAddSource(name.trim(), type, type === 'TEXT' ? extraText : undefined);
      setName('');
      setIsAdding(false);
    }
  };

  return (
    <div className="bg-[#121118] rounded-xl border border-[#2E2A42] p-3 flex flex-col gap-2.5 shadow-lg">
      <div className="flex items-center justify-between pb-2 border-b border-[#2E2A42]">
        <div className="flex items-center gap-2">
          <span className="text-purple-400 font-bold uppercase tracking-wider text-[11px]">
            Sources
          </span>
          <span className="text-[10px] text-slate-500 font-mono">({sceneSources.length} Layers)</span>
        </div>
      </div>

      {/* Layer stack */}
      <div className="flex flex-col gap-1 max-h-56 overflow-y-auto pr-1">
        {sceneSources.length === 0 ? (
          <div className="p-3 text-center text-slate-500 text-xs">
            No sources in this scene.
          </div>
        ) : (
          sceneSources.map((source, index) => (
            <div
              key={source.id}
              className="flex items-center justify-between px-2.5 py-1.5 rounded-lg text-xs bg-[#181622] border border-[#252236] hover:border-[#383352] transition-colors"
            >
              <div className="flex items-center gap-2 truncate">
                <span className="text-[10px] font-mono text-purple-400 bg-purple-500/10 px-1.5 py-0.5 rounded">
                  {source.type.replace('_', ' ')}
                </span>
                <span className={`truncate font-medium ${source.isVisible ? 'text-slate-200' : 'text-slate-500 line-through'}`}>
                  {source.name}
                </span>
              </div>

              <div className="flex items-center gap-1">
                <button
                  onClick={() => onToggleVisibility(source.id)}
                  className={`p-1 rounded ${source.isVisible ? 'text-purple-400 hover:text-purple-300' : 'text-slate-500'}`}
                  title={source.isVisible ? 'Hide Source' : 'Show Source'}
                >
                  {source.isVisible ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                </button>

                <button
                  onClick={() => onToggleLock(source.id)}
                  className={`p-1 rounded ${source.isLocked ? 'text-amber-400' : 'text-slate-500 hover:text-slate-300'}`}
                  title={source.isLocked ? 'Unlock Position' : 'Lock Position'}
                >
                  {source.isLocked ? <Lock className="w-3.5 h-3.5" /> : <Unlock className="w-3.5 h-3.5" />}
                </button>

                <button
                  onClick={() => onMoveUp(source.id)}
                  disabled={index === 0}
                  className="p-1 text-slate-400 hover:text-slate-200 disabled:opacity-30"
                  title="Move Layer Up"
                >
                  <ArrowUp className="w-3 h-3" />
                </button>

                <button
                  onClick={() => onMoveDown(source.id)}
                  disabled={index === sceneSources.length - 1}
                  className="p-1 text-slate-400 hover:text-slate-200 disabled:opacity-30"
                  title="Move Layer Down"
                >
                  <ArrowDown className="w-3 h-3" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Add Source form */}
      {isAdding ? (
        <div className="flex flex-col gap-2 pt-2 border-t border-[#2E2A42]">
          <input
            type="text"
            placeholder="Source name..."
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full bg-[#181622] border border-[#2E2A42] rounded px-2 py-1 text-xs text-white outline-none focus:border-purple-500"
            autoFocus
          />

          <select
            value={type}
            onChange={(e) => setType(e.target.value as SourceType)}
            className="w-full bg-[#181622] border border-[#2E2A42] rounded px-2 py-1 text-xs text-slate-200 outline-none"
          >
            <option value="TEXT">Text Overlay</option>
            <option value="DISPLAY_CAPTURE">Display / Screen</option>
            <option value="CAMERA">Camera Feed</option>
            <option value="COLOR">Solid Color Fill</option>
            <option value="IMAGE">Image File</option>
            <option value="BROWSER_FRAME">Browser / Web Frame</option>
          </select>

          {type === 'TEXT' && (
            <input
              type="text"
              placeholder="Display text..."
              value={extraText}
              onChange={(e) => setExtraText(e.target.value)}
              className="w-full bg-[#181622] border border-[#2E2A42] rounded px-2 py-1 text-xs text-white outline-none"
            />
          )}

          <div className="flex items-center gap-1 justify-end">
            <button
              onClick={() => setIsAdding(false)}
              className="px-2.5 py-1 text-slate-400 hover:text-slate-200 text-xs"
            >
              Cancel
            </button>
            <button
              onClick={handleCreate}
              className="px-3 py-1 bg-purple-600 hover:bg-purple-500 text-white rounded text-xs font-semibold"
            >
              Create
            </button>
          </div>
        </div>
      ) : (
        <button
          onClick={() => {
            setIsAdding(true);
            setName('New Text Overlay');
          }}
          className="flex items-center justify-center gap-1.5 w-full py-1.5 bg-[#181622] hover:bg-[#201D2E] text-slate-300 hover:text-white rounded-lg text-xs font-medium border border-dashed border-[#2E2A42] transition-colors"
        >
          <Plus className="w-3.5 h-3.5 text-purple-400" />
          <span>Add Source</span>
        </button>
      )}
    </div>
  );
};
