export type TransitionType = 'CUT' | 'FADE';

export interface SceneTransition {
  type: TransitionType;
  durationMs: number;
}

export type SourceType =
  | 'DISPLAY_CAPTURE'
  | 'CAMERA'
  | 'IMAGE'
  | 'TEXT'
  | 'COLOR'
  | 'AUDIO_INPUT'
  | 'AUDIO_OUTPUT'
  | 'MEDIA'
  | 'BROWSER_FRAME';

export interface SourceTransform {
  x: number;
  y: number;
  width: number;
  height: number;
  scale: number;
  rotation: number;
  opacity: number;
}

export interface SourceItem {
  id: string;
  name: string;
  type: SourceType;
  isVisible: boolean;
  isLocked: boolean;
  isMuted: boolean;
  volume: number;
  transform: SourceTransform;
  extra?: {
    text?: string;
    textColor?: string;
    bgColor?: string;
    color?: string;
    mediaUrl?: string;
  };
}

export interface SceneItem {
  id: string;
  name: string;
  sourceIds: string[];
  isDefault?: boolean;
}

export type OverlayShape = 'RECTANGLE' | 'ROUNDED' | 'CIRCLE';

export interface CameraOverlayState {
  shape: OverlayShape;
  borderWidth: number;
  borderColor: string;
  x: number; // percentage (0 - 100)
  y: number; // percentage (0 - 100)
  width: number; // percentage (0 - 100)
  height: number; // percentage (0 - 100)
}

export interface AudioChannel {
  id: string;
  name: string;
  volume: number; // 0 to 1.5
  isMuted: boolean;
  gainDb: number;
  peakDb: number; // -60 to 0
  rmsLevel: number; // 0 to 1
  statusText: string;
}

export interface RecordingEntry {
  id: string;
  filename: string;
  timestamp: number;
  durationSec: number;
  fileSizeBytes: number;
  blobUrl: string;
  blob: Blob;
  resolution: string;
}

export interface ObsProfilePreset {
  id: string;
  name: string;
  resolution: string;
  width: number;
  height: number;
  fps: number;
  bitrateKbps: number;
  codec: string;
}
