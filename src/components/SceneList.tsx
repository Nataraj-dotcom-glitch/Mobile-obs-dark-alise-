import React, { useState } from 'react';
import { Plus, Copy, Trash2, Edit2, Check } from 'lucide-react';
import { SceneItem, TransitionType } from '../types/obs';

interface SceneListProps {
  scenes: SceneItem[];
  activeSceneId: string;
  onSelectScene: (id: string) => void;
  onCreateScene: (name: string) => void;
  onDuplicateScene: (id: string) => void;
  onDeleteScene: (id: string) => void;
  onRenameScene: (id: string, newName: string) => void;
  transitionType: TransitionType;
  setTransitionType: (t: TransitionType) => void;
}

export const SceneList: React.FC<SceneListProps> = ({
  scenes,
  activeSceneId,
  onSelectScene,
  onCreateScene,
  onDuplicateScene,
  onDeleteScene,
  onRenameScene,
  transitionType,
  setTransitionType,
}) => {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [newSceneName, setNewSceneName] = useState('');

  const handleStartRename = (scene: SceneItem) => {
    setEditingId(scene.id);
    setEditName(scene.name);
  };

  const handleSaveRename = (id: string) => {
    if (editName.trim()) {
      onRenameScene(id, editName.trim());
    }
    setEditingId(null);
  };

  const handleCreate = () => {
    if (newSceneName.trim()) {
      onCreateScene(newSceneName.trim());
      setNewSceneName('');
      setIsAdding(false);
    }
  };

  return (
    <div className="bg-[#121118] rounded-xl border border-[#2E2A42] p-3 flex flex-col gap-2.5 shadow-lg">
      <div className="flex items-center justify-between pb-2 border-b border-[#2E2A42]">
        <div className="flex items-center gap-2">
          <span className="text-purple-400 font-bold uppercase tracking-wider text-[11px]">
            Scenes
          </span>
          <span className="text-[10px] text-slate-500 font-mono">({scenes.length})</span>
        </div>

        {/* Transition Switcher */}
        <div className="flex items-center gap-1 bg-[#1A1826] p-0.5 rounded border border-[#2E2A42]">
          <button
            onClick={() => setTransitionType('CUT')}
            className={`px-2 py-0.5 rounded text-[10px] font-medium transition-colors ${
              transitionType === 'CUT'
                ? 'bg-purple-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Cut
          </button>
          <button
            onClick={() => setTransitionType('FADE')}
            className={`px-2 py-0.5 rounded text-[10px] font-medium transition-colors ${
              transitionType === 'FADE'
                ? 'bg-purple-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Fade
          </button>
        </div>
      </div>

      {/* Scenes List */}
      <div className="flex flex-col gap-1 max-h-56 overflow-y-auto pr-1">
        {scenes.map((scene) => {
          const isSelected = scene.id === activeSceneId;
          const isEditing = editingId === scene.id;

          return (
            <div
              key={scene.id}
              onClick={() => onSelectScene(scene.id)}
              className={`group flex items-center justify-between px-2.5 py-1.5 rounded-lg text-xs cursor-pointer transition-colors ${
                isSelected
                  ? 'bg-purple-600/30 text-purple-200 border border-purple-500/50 font-semibold shadow-xs'
                  : 'bg-[#181622] text-slate-300 hover:bg-[#201D2E] border border-transparent'
              }`}
            >
              {isEditing ? (
                <div className="flex items-center gap-1 w-full" onClick={(e) => e.stopPropagation()}>
                  <input
                    type="text"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSaveRename(scene.id)}
                    className="w-full bg-[#09090D] border border-purple-500 px-1.5 py-0.5 rounded text-white text-xs outline-none"
                    autoFocus
                  />
                  <button
                    onClick={() => handleSaveRename(scene.id)}
                    className="p-1 text-emerald-400 hover:text-emerald-300"
                  >
                    <Check className="w-3.5 h-3.5" />
                  </button>
                </div>
              ) : (
                <>
                  <div className="flex items-center gap-2 truncate">
                    <span className={`w-1.5 h-1.5 rounded-full ${isSelected ? 'bg-purple-400' : 'bg-transparent'}`} />
                    <span className="truncate">{scene.name}</span>
                  </div>

                  <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleStartRename(scene);
                      }}
                      className="p-1 text-slate-400 hover:text-slate-200"
                      title="Rename"
                    >
                      <Edit2 className="w-3 h-3" />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDuplicateScene(scene.id);
                      }}
                      className="p-1 text-slate-400 hover:text-slate-200"
                      title="Duplicate"
                    >
                      <Copy className="w-3 h-3" />
                    </button>
                    {scenes.length > 1 && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onDeleteScene(scene.id);
                        }}
                        className="p-1 text-red-400 hover:text-red-300"
                        title="Delete"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    )}
                  </div>
                </>
              )}
            </div>
          );
        })}
      </div>

      {/* Add Scene Inline Form */}
      {isAdding ? (
        <div className="flex items-center gap-1 pt-1 border-t border-[#2E2A42]">
          <input
            type="text"
            placeholder="New scene name..."
            value={newSceneName}
            onChange={(e) => setNewSceneName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
            className="flex-1 bg-[#181622] border border-[#2E2A42] rounded px-2 py-1 text-xs text-white outline-none focus:border-purple-500"
            autoFocus
          />
          <button
            onClick={handleCreate}
            className="px-2.5 py-1 bg-purple-600 hover:bg-purple-500 text-white rounded text-xs font-semibold"
          >
            Add
          </button>
          <button
            onClick={() => setIsAdding(false)}
            className="px-2 py-1 text-slate-400 hover:text-slate-200 text-xs"
          >
            Cancel
          </button>
        </div>
      ) : (
        <button
          onClick={() => setIsAdding(true)}
          className="flex items-center justify-center gap-1.5 w-full py-1.5 bg-[#181622] hover:bg-[#201D2E] text-slate-300 hover:text-white rounded-lg text-xs font-medium border border-dashed border-[#2E2A42] transition-colors"
        >
          <Plus className="w-3.5 h-3.5 text-purple-400" />
          <span>Add Scene</span>
        </button>
      )}
    </div>
  );
};
