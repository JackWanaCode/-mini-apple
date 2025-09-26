import React, { useEffect, useMemo, useRef, useState } from "react";
import Hls from "hls.js";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Play, Pause, Volume2, VolumeX, LogIn, UserPlus, Upload, Film } from "lucide-react";

/**
 * Mini Netflix Frontend
 * - Handles register/login
 * - Plays HLS master playlist with token-protected segments (signed via /api/v1/sign)
 * - Enqueue encoding jobs
 *
 * Assumptions:
 * - Backend hosted at http://localhost:9000 (override with VITE_API_BASE or window.API_BASE)
 * - Auth endpoints return { token: string } on success for /login; /register may also return token or be silent
 * - /api/v1/sign returns { url: string } with a full signed URL for the requested segment path
 * - Master playlist is available at /api/v1/videos/:videoId/master.m3u8
 */

const API_BASE = (typeof window !== "undefined" && (window as any).API_BASE) ||
  (import.meta as any)?.env?.VITE_API_BASE ||
  "http://localhost:9000";

function apiHeaders(token?: string) {
  const headers: Record<string, string> = { "content-type": "application/json" };
  if (token) headers["authorization"] = `Bearer ${token}`;
  return headers;
}

async function register(email: string, password: string) {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: apiHeaders(),
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw new Error(await res.text());
  try { return await res.json(); } catch { return {}; }
}

async function login(email: string, password: string): Promise<{ token: string }>{
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: apiHeaders(),
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

async function signPath(pathname: string, token?: string): Promise<string> {
  const res = await fetch(`${API_BASE}/api/v1/sign`, {
    method: "POST",
    headers: apiHeaders(token),
    body: JSON.stringify({ path: pathname }),
  });
  if (!res.ok) throw new Error(await res.text());
  const data = await res.json();
  if (!data?.url) throw new Error("Sign API did not return a 'url'.");
  return data.url as string;
}

async function enqueueJob(videoId: string, sourcePath: string, renditions: string[], token?: string) {
  const res = await fetch(`${API_BASE}/api/v1/jobs/encode`, {
    method: "POST",
    headers: apiHeaders(token),
    body: JSON.stringify({ videoId, sourcePath, renditions }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/**
 * Custom Hls.js loader that signs each fragment (and optionally key) URL by calling /api/v1/sign.
 * This keeps the master/level/playlist public (or less protected) while protecting the actual media.
 */
class SigningLoader extends (Hls as any).DefaultConfig.loader {
  private token?: string;
  constructor(config: any) {
    super(config);
    this.token = config?.token;
  }
  load(context: any, config: any, callbacks: any) {
    const type = context?.type;
    // Only sign media fragments and keys; let playlists load normally
    if (type === "fragment" || type === "key") {
      try {
        const original = new URL(context.url, API_BASE);
        const pathname = original.pathname; // e.g. /hls/VID123/720p/seg00001.ts
        signPath(pathname, this.token)
          .then((signed) => {
            context.url = signed; // replace with signed URL
            super.load(context, config, callbacks);
          })
          .catch((err) => {
            callbacks.onError({ code: 0, text: `Signing error: ${String(err)}` }, context, null);
          });
        return;
      } catch (e) {
        callbacks.onError({ code: 0, text: `Signing URL parse error: ${String(e)}` }, context, null);
        return;
      }
    }
    // Default behavior for manifests and levels
    super.load(context, config, callbacks);
  }
}

export default function App() {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const hlsRef = useRef<Hls | null>(null);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("auth_token"));

  const [videoId, setVideoId] = useState("VID123");
  const [status, setStatus] = useState<string>("");
  const [autoplay, setAutoplay] = useState(true);

  const [sourcePath, setSourcePath] = useState("/ingest/VID123.mp4");
  const [renditions, setRenditions] = useState<string[]>(["1080p", "720p", "480p"]);

  useEffect(() => {
    if (token) localStorage.setItem("auth_token", token);
    else localStorage.removeItem("auth_token");
  }, [token]);

  const hlsConfig = useMemo(() => ({
    debug: false,
    lowLatencyMode: true,
    // Inject our signing loader with token in config
    loader: class extends SigningLoader {
      constructor(config: any) {
        super({ ...config, token });
      }
    },
  }), [token]);

  async function handleRegister() {
    try {
      setStatus("Registering…");
      const res: any = await register(email, password);
      setStatus("Registered. " + (res?.token ? "You are logged in." : "Now log in."));
      if (res?.token) setToken(res.token);
    } catch (e: any) {
      setStatus("Register failed: " + e.message);
    }
  }

  async function handleLogin() {
    try {
      setStatus("Logging in…");
      const res = await login(email, password);
      setToken(res.token);
      setStatus("Logged in");
    } catch (e: any) {
      setStatus("Login failed: " + e.message);
    }
  }

  function destroyPlayer() {
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }
  }

  async function playVideo() {
    setStatus("");
    const videoEl = videoRef.current;
    if (!videoEl) return;

    const masterUrl = `${API_BASE}/api/v1/videos/${encodeURIComponent(videoId)}/master.m3u8`;

    // Native HLS (Safari, iOS)
    if (videoEl.canPlayType("application/vnd.apple.mpegurl")) {
      videoEl.src = masterUrl;
      try {
        await videoEl.play();
      } catch (e) {
        setStatus("Autoplay blocked; press play.");
      }
      return;
    }

    // MSE with hls.js
    if (Hls.isSupported()) {
      destroyPlayer();
      const hls = new Hls(hlsConfig);
      hlsRef.current = hls;

      hls.on(Hls.Events.ERROR, (_evt, data) => {
        if (data?.fatal) setStatus(`HLS fatal error: ${data.type} / ${data.details}`);
      });

      hls.loadSource(masterUrl);
      hls.attachMedia(videoEl);
      hls.on(Hls.Events.MANIFEST_PARSED, async () => {
        try {
          if (autoplay) await videoEl.play();
        } catch (e) {
          setStatus("Autoplay blocked; press play.");
        }
      });
      return;
    }

    setStatus("HLS not supported in this browser.");
  }

  function togglePlay() {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) v.play();
    else v.pause();
  }

  function toggleMute() {
    const v = videoRef.current;
    if (!v) return;
    v.muted = !v.muted;
  }

  async function handleEnqueue() {
    try {
      setStatus("Enqueuing encode job…");
      const res = await enqueueJob(videoId, sourcePath, renditions, token || undefined);
      setStatus("Encode job queued: " + JSON.stringify(res));
    } catch (e: any) {
      setStatus("Enqueue failed: " + e.message);
    }
  }

  useEffect(() => () => destroyPlayer(), []);

  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-slate-50 to-slate-100 p-6">
      <div className="mx-auto max-w-6xl grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Auth */}
        <Card className="lg:col-span-1 shadow-md">
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><LogIn className="h-5 w-5"/> Auth</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex gap-2">
              <Input placeholder="email" value={email} onChange={(e)=>setEmail(e.target.value)} />
              <Input placeholder="password" type="password" value={password} onChange={(e)=>setPassword(e.target.value)} />
            </div>
            <div className="flex gap-2">
              <Button onClick={handleRegister} variant="outline"><UserPlus className="mr-2 h-4 w-4"/>Register</Button>
              <Button onClick={handleLogin}><LogIn className="mr-2 h-4 w-4"/>Login</Button>
              {token ? <Badge>JWT set</Badge> : <Badge variant="secondary">No token</Badge>}
            </div>
          </CardContent>
        </Card>

        {/* Player Controls */}
        <Card className="lg:col-span-2 shadow-md">
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><Film className="h-5 w-5"/> Player</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <Input className="w-44" value={videoId} onChange={(e)=>setVideoId(e.target.value)} placeholder="Video ID" />
              <Button onClick={playVideo}><Play className="mr-2 h-4 w-4"/>Load & Play</Button>
              <Button variant="outline" onClick={togglePlay}><Pause className="mr-2 h-4 w-4"/>Play/Pause</Button>
              <Button variant="outline" onClick={toggleMute}><Volume2 className="mr-2 h-4 w-4"/>Mute/Unmute</Button>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={autoplay} onChange={(e)=>setAutoplay(e.target.checked)} /> Autoplay
              </label>
            </div>
            <div className="relative w-full aspect-video bg-black rounded-2xl overflow-hidden shadow">
              <video ref={videoRef} controls playsInline className="w-full h-full" />
            </div>
            {status && <div className="text-sm text-slate-700">{status}</div>}
          </CardContent>
        </Card>

        {/* Encode Job */}
        <Card className="lg:col-span-3 shadow-md">
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><Upload className="h-5 w-5"/> Encode Job</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex flex-wrap gap-2 items-center">
              <Input className="w-44" value={videoId} onChange={(e)=>setVideoId(e.target.value)} placeholder="Video ID" />
              <Input className="flex-1" value={sourcePath} onChange={(e)=>setSourcePath(e.target.value)} placeholder="/ingest/VID123.mp4" />

              <RenditionPicker renditions={renditions} onChange={setRenditions} />

              <Button onClick={handleEnqueue}>Enqueue</Button>
            </div>
            <div className="text-xs text-slate-600">This calls <code>POST /api/v1/jobs/encode</code> with the chosen renditions.</div>
          </CardContent>
        </Card>

        {/* Tips */}
        <Card className="lg:col-span-3 shadow-sm">
          <CardHeader>
            <CardTitle>Notes</CardTitle>
          </CardHeader>
          <CardContent className="prose prose-slate max-w-none text-sm">
            <ul className="list-disc pl-5">
              <li>Override API base by setting <code>window.API_BASE</code> or <code>VITE_API_BASE</code>.</li>
              <li>Segments are signed on-the-fly via a custom Hls.js loader that POSTs <code>/api/v1/sign</code> with the segment path before each request.</li>
              <li>If your master playlist uses relative segment paths like <code>/hls/VID123/720p/seg00001.ts</code>, signing will work out of the box.</li>
              <li>Make sure CORS is enabled on the backend for the frontend origin.</li>
              <li>Safari/iOS can play HLS natively; other browsers use MSE via hls.js.</li>
            </ul>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function RenditionPicker({ renditions, onChange }: { renditions: string[]; onChange: (r: string[]) => void }) {
  const [newRendition, setNewRendition] = useState("720p");
  const add = () => {
    if (!renditions.includes(newRendition)) onChange([...renditions, newRendition]);
  };
  const remove = (r: string) => onChange(renditions.filter(x => x !== r));
  return (
    <div className="flex items-center gap-2">
      <Select value={newRendition} onValueChange={setNewRendition}>
        <SelectTrigger className="w-32"><SelectValue placeholder="Rendition"/></SelectTrigger>
        <SelectContent>
          {['1080p','720p','480p','360p','240p','144p'].map(r => (
            <SelectItem key={r} value={r}>{r}</SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Button type="button" variant="outline" onClick={add}>Add</Button>
      <div className="flex flex-wrap gap-2">
        {renditions.map(r => (
          <Badge key={r} className="cursor-pointer" onClick={()=>remove(r)} title="Remove">{r} ✕</Badge>
        ))}
      </div>
    </div>
  );
}