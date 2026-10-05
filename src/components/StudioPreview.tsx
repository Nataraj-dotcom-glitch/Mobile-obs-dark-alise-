import React, { useRef, useEffect, useState } from 'react';
import { Camera, Monitor, Move, Square, Circle } from 'lucide-react';
import { CameraOverlayState, OverlayShape, SceneItem, SourceItem } from '../types/obs';

interface StudioPreviewProps {
  canvasRef: React.RefObject<HTMLCanvasElement | null>;
  activeScene: SceneItem;
  sources: SourceItem[];
  screenStream: MediaStream | null;
  cameraStream: MediaStream | null;
  cameraOverlay: CameraOverlayState;
  setCameraOverlay: React.Dispatch<React.SetStateAction<CameraOverlayState>>;
  isScreenCapturing: boolean;
  isCameraActive: boolean;
  onToggleScreenCapture: () => void;
  onToggleCamera: () => void;
}

export const StudioPreview: React.FC<StudioPreviewProps> = ({
  canvasRef,
  activeScene,
  sources,
  screenStream,
  cameraStream,
  cameraOverlay,
  setCameraOverlay,
  isScreenCapturing,
  isCameraActive,
  onToggleScreenCapture,
  onToggleCamera,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const screenVideoRef = useRef<HTMLVideoElement>(null);
  const cameraVideoRef = useRef<HTMLVideoElement>(null);
  const [isDraggingOverlay, setIsDraggingOverlay] = useState(false);

  // Bind video element streams
  useEffect(() => {
    if (screenVideoRef.current) {
      screenVideoRef.current.srcObject = screenStream;
    }
  }, [screenStream]);

  useEffect(() => {
    if (cameraVideoRef.current) {
      cameraVideoRef.current.srcObject = cameraStream;
    }
  }, [cameraStream]);

  // Main 60 FPS Canvas Compositor loop
  useEffect(() => {
    let animId: number;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const render = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // 1. Background layer based on scene
      if (activeScene.name === 'STARTING') {
        const grad = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
        grad.addColorStop(0, '#1E1035');
        grad.addColorStop(1, '#09090D');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        ctx.fillStyle = '#C084FC';
        ctx.font = 'bold 64px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('STREAM STARTING SOON', canvas.width / 2, canvas.height / 2 - 20);
        ctx.fillStyle = '#94A3B8';
        ctx.font = '28px sans-serif';
        ctx.fillText('Dark Alise OBS Broadcast', canvas.width / 2, canvas.height / 2 + 40);
      } else if (activeScene.name === 'BRB') {
        ctx.fillStyle = '#09090D';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#EAB308';
        ctx.font = 'bold 72px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('BE RIGHT BACK', canvas.width / 2, canvas.height / 2);
      } else if (activeScene.name === 'ENDING') {
        ctx.fillStyle = '#09090D';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#A855F7';
        ctx.font = 'bold 64px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('THANKS FOR WATCHING!', canvas.width / 2, canvas.height / 2);
      } else {
        // Standard studio backdrop
        ctx.fillStyle = '#0c0a14';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        // Grid lines for studio alignment
        ctx.strokeStyle = '#1E1B2E';
        ctx.lineWidth = 1;
        for (let x = 0; x < canvas.width; x += 120) {
          ctx.beginPath();
          ctx.moveTo(x, 0);
          ctx.lineTo(x, canvas.height);
          ctx.stroke();
        }
        for (let y = 0; y < canvas.height; y += 120) {
          ctx.beginPath();
          ctx.moveTo(0, y);
          ctx.lineTo(canvas.width, y);
          ctx.stroke();
        }
      }

      // 2. Render Screen Capture if active and in current scene
      const displaySource = sources.find((s) => s.type === 'DISPLAY_CAPTURE');
      if (
        displaySource?.isVisible &&
        screenVideoRef.current &&
        screenVideoRef.current.readyState >= 2 &&
        activeScene.sourceIds.includes(displaySource.id)
      ) {
        ctx.drawImage(screenVideoRef.current, 0, 0, canvas.width, canvas.height);
      } else if (
        displaySource &&
        activeScene.sourceIds.includes(displaySource.id) &&
        !isScreenCapturing
      ) {
        // Display capture placeholder banner
        ctx.fillStyle = '#12111A';
        ctx.fillRect(80, 60, canvas.width - 160, canvas.height - 120);
        ctx.strokeStyle = '#2E2A42';
        ctx.strokeRect(80, 60, canvas.width - 160, canvas.height - 120);
        ctx.fillStyle = '#64748B';
        ctx.font = '24px monospace';
        ctx.textAlign = 'center';
        ctx.fillText('DISPLAY CAPTURE [OFFLINE - CLICK SCREEN CAPTURE TO ACTIVATE]', canvas.width / 2, canvas.height / 2);
      }

      // 3. Render Custom Text Sources
      sources
        .filter((s) => s.type === 'TEXT' && s.isVisible && activeScene.sourceIds.includes(s.id))
        .forEach((s) => {
          ctx.save();
          ctx.fillStyle = s.extra?.textColor || '#F8FAFC';
          ctx.font = 'bold 36px sans-serif';
          ctx.textAlign = 'left';
          ctx.fillText(s.extra?.text || s.name, s.transform.x * 19.2, s.transform.y * 10.8);
          ctx.restore();
        });

      // 4. Render Camera Overlay if enabled
      const cameraSource = sources.find((s) => s.type === 'CAMERA');
      if (cameraSource?.isVisible && activeScene.sourceIds.includes(cameraSource.id)) {
        const camX = (cameraOverlay.x / 100) * canvas.width;
        const camY = (cameraOverlay.y / 100) * canvas.height;
        const camW = (cameraOverlay.width / 100) * canvas.width;
        const camH = (cameraOverlay.height / 100) * canvas.height;

        ctx.save();
        ctx.beginPath();

        if (cameraOverlay.shape === 'CIRCLE') {
          const radius = Math.min(camW, camH) / 2;
          ctx.arc(camX + camW / 2, camY + camH / 2, radius, 0, Math.PI * 2);
        } else if (cameraOverlay.shape === 'ROUNDED') {
          const radius = 24;
          ctx.roundRect(camX, camY, camW, camH, radius);
        } else {
          ctx.rect(camX, camY, camW, camH);
        }
        ctx.closePath();
        ctx.clip();

        if (
          cameraVideoRef.current &&
          cameraVideoRef.current.readyState >= 2 &&
          isCameraActive
        ) {
          ctx.drawImage(cameraVideoRef.current, camX, camY, camW, camH);
        } else {
          ctx.fillStyle = '#1E1035';
          ctx.fillRect(camX, camY, camW, camH);
          ctx.fillStyle = '#A855F7';
          ctx.font = 'bold 22px sans-serif';
          ctx.textAlign = 'center';
          ctx.fillText('CAMERA OVERLAY', camX + camW / 2, camY + camH / 2);
        }

        ctx.restore();

        // Overlay Border & Glow
        ctx.save();
        ctx.strokeStyle = cameraOverlay.borderColor || '#A855F7';
        ctx.lineWidth = cameraOverlay.borderWidth || 4;
        ctx.beginPath();
        if (cameraOverlay.shape === 'CIRCLE') {
          const radius = Math.min(camW, camH) / 2;
          ctx.arc(camX + camW / 2, camY + camH / 2, radius, 0, Math.PI * 2);
        } else if (cameraOverlay.shape === 'ROUNDED') {
          ctx.roundRect(camX, camY, camW, camH, 24);
        } else {
          ctx.strokeRect(camX, camY, camW, camH);
        }
        ctx.stroke();
        ctx.restore();
      }

      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [activeScene, sources, isScreenCapturing, isCameraActive, cameraOverlay]);

  // Handle overlay drag repositioning on the DOM preview
  const handlePointerDown = (e: React.PointerEvent) => {
    setIsDraggingOverlay(true);
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDraggingOverlay || !containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const newX = ((e.clientX - rect.left) / rect.width) * 100 - cameraOverlay.width / 2;
    const newY = ((e.clientY - rect.top) / rect.height) * 100 - cameraOverlay.height / 2;

    setCameraOverlay((prev) => ({
      ...prev,
      x: Math.max(0, Math.min(100 - prev.width, newX)),
      y: Math.max(0, Math.min(100 - prev.height, newY)),
    }));
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    setIsDraggingOverlay(false);
    try {
      (e.target as HTMLElement).releasePointerCapture(e.pointerId);
    } catch (ignored) {}
  };

  return (
    <div className="flex flex-col bg-[#121118] rounded-xl border border-[#2E2A42] overflow-hidden shadow-2xl">
      {/* Hidden MediaStream Video elements for Canvas drawing */}
      <video ref={screenVideoRef} autoPlay playsInline muted className="hidden" />
      <video ref={cameraVideoRef} autoPlay playsInline muted className="hidden" />

      {/* Top Preview Status Header */}
      <div className="px-4 py-2 bg-[#0d0c14] border-b border-[#2E2A42] flex items-center justify-between text-xs">
        <div className="flex items-center gap-2">
          <span className="text-purple-400 font-bold uppercase tracking-wider text-[11px]">
            Program Output
          </span>
          <span className="text-slate-500 font-mono">1920x1080 @ 60FPS</span>
          <span className="text-slate-600">·</span>
          <span className="text-slate-300 font-semibold">{activeScene.name}</span>
        </div>

        {/* Quick Hardware Toggles */}
        <div className="flex items-center gap-2">
          <button
            onClick={onToggleScreenCapture}
            className={`px-2.5 py-1 rounded text-[11px] font-medium transition-colors flex items-center gap-1.5 ${
              isScreenCapturing
                ? 'bg-purple-600 text-white shadow-sm'
                : 'bg-[#1E1B2E] text-slate-300 hover:bg-[#2A263F]'
            }`}
            title="Toggle Live Screen Capture"
          >
            <Monitor className="w-3.5 h-3.5" />
            <span>{isScreenCapturing ? 'Screen Active' : 'Share Screen'}</span>
          </button>

          <button
            onClick={onToggleCamera}
            className={`px-2.5 py-1 rounded text-[11px] font-medium transition-colors flex items-center gap-1.5 ${
              isCameraActive
                ? 'bg-purple-600 text-white shadow-sm'
                : 'bg-[#1E1B2E] text-slate-300 hover:bg-[#2A263F]'
            }`}
            title="Toggle Live Camera Overlay"
          >
            <Camera className="w-3.5 h-3.5" />
            <span>{isCameraActive ? 'Cam Active' : 'Enable Cam'}</span>
          </button>
        </div>
      </div>

      {/* Interactive 16:9 Canvas Viewport */}
      <div
        ref={containerRef}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        className="relative w-full aspect-video bg-black flex items-center justify-center overflow-hidden cursor-crosshair select-none"
      >
        <canvas
          ref={canvasRef}
          width={1920}
          height={1080}
          className="w-full h-full object-contain pointer-events-none"
        />

        {/* Draggable HUD Overlay Indicator */}
        <div
          onPointerDown={handlePointerDown}
          style={{
            left: `${cameraOverlay.x}%`,
            top: `${cameraOverlay.y}%`,
            width: `${cameraOverlay.width}%`,
            height: `${cameraOverlay.height}%`,
          }}
          className={`absolute border-2 border-dashed border-purple-400/60 bg-purple-500/10 cursor-move flex items-center justify-center ${
            cameraOverlay.shape === 'CIRCLE'
              ? 'rounded-full'
              : cameraOverlay.shape === 'ROUNDED'
              ? 'rounded-2xl'
              : 'rounded-none'
          }`}
          title="Drag to reposition Camera Overlay"
        >
          <div className="bg-black/80 px-2 py-0.5 rounded text-[10px] font-mono text-purple-300 flex items-center gap-1 pointer-events-none">
            <Move className="w-2.5 h-2.5" />
            <span>Cam Overlay</span>
          </div>
        </div>
      </div>

      {/* Bottom Overlay Controls bar */}
      <div className="px-4 py-2 bg-[#09090D] border-t border-[#2E2A42] flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2 text-slate-400">
          <span>Camera Shape:</span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setCameraOverlay((p) => ({ ...p, shape: 'ROUNDED' }))}
              className={`px-2 py-1 rounded text-[11px] ${
                cameraOverlay.shape === 'ROUNDED'
                  ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Rounded
            </button>
            <button
              onClick={() => setCameraOverlay((p) => ({ ...p, shape: 'CIRCLE' }))}
              className={`px-2 py-1 rounded text-[11px] ${
                cameraOverlay.shape === 'CIRCLE'
                  ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Circle
            </button>
            <button
              onClick={() => setCameraOverlay((p) => ({ ...p, shape: 'RECTANGLE' }))}
              className={`px-2 py-1 rounded text-[11px] ${
                cameraOverlay.shape === 'RECTANGLE'
                  ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Rect
            </button>
          </div>
        </div>

        <div className="flex items-center gap-3 text-slate-400 font-mono text-[11px]">
          <span>POS: {Math.round(cameraOverlay.x)}%, {Math.round(cameraOverlay.y)}%</span>
          <span>SCALE: {Math.round(cameraOverlay.width)}%</span>
        </div>
      </div>
    </div>
  );
};
