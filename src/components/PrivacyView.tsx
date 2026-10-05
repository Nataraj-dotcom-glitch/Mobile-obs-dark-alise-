import React from 'react';
import { ShieldCheck, Lock, HardDrive, Wifi, EyeOff } from 'lucide-react';

export const PrivacyView: React.FC = () => {
  return (
    <div className="flex-1 p-6 flex flex-col gap-6 max-w-4xl mx-auto w-full">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-bold bg-gradient-to-r from-purple-400 to-fuchsia-400 bg-clip-text text-transparent">
          Privacy &amp; Security Charter
        </h1>
        <p className="text-xs text-slate-400">
          Local-First Architecture · Zero Cloud Tracking · Legitimate Public APIs
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
        <div className="bg-[#121118] border border-[#2E2A42] p-4 rounded-xl flex flex-col gap-2">
          <div className="flex items-center gap-2 text-purple-400 font-bold">
            <Lock className="w-4 h-4" />
            <span>1. Screen Capture via MediaProjection</span>
          </div>
          <p className="text-slate-400 leading-relaxed">
            Screen capture frames are encoded directly into your local hardware encoder (MediaCodec). No captured frames are ever transmitted to any third-party analytics or external telemetry services.
          </p>
        </div>

        <div className="bg-[#121118] border border-[#2E2A42] p-4 rounded-xl flex flex-col gap-2">
          <div className="flex items-center gap-2 text-emerald-400 font-bold">
            <ShieldCheck className="w-4 h-4" />
            <span>2. Camera &amp; Microphone Isolation</span>
          </div>
          <p className="text-slate-400 leading-relaxed">
            Camera and microphone hardware are initialized strictly during active preview, recording, or live streaming sessions. Hardware resources are immediately unmapped and released upon stopping.
          </p>
        </div>

        <div className="bg-[#121118] border border-[#2E2A42] p-4 rounded-xl flex flex-col gap-2">
          <div className="flex items-center gap-2 text-blue-400 font-bold">
            <HardDrive className="w-4 h-4" />
            <span>3. Android Scoped Storage</span>
          </div>
          <p className="text-slate-400 leading-relaxed">
            Recordings are saved strictly to the standard Android Movies collection via the official MediaStore ContentResolver API. No deprecated broad storage access permissions are required.
          </p>
        </div>

        <div className="bg-[#121118] border border-[#2E2A42] p-4 rounded-xl flex flex-col gap-2">
          <div className="flex items-center gap-2 text-fuchsia-400 font-bold">
            <Wifi className="w-4 h-4" />
            <span>4. Secure RTMP Socket Ingest</span>
          </div>
          <p className="text-slate-400 leading-relaxed">
            Live streams connect point-to-point directly to your user-configured ingest endpoint (e.g., Twitch, YouTube Live, or private RTMP server). Stream keys are stored strictly in private local device storage.
          </p>
        </div>
      </div>
    </div>
  );
};
