#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the voice of the Spirit of Space (docs/magic/spirit_space.md): a deep cosmic hum, glassy pings, the spatial "tear" whoosh, the swell of a portal, and the creature cues of a spirit that has
no words (whispers, chimes, soft murmurs, a curious chirp, a distress warble, a happy trill, a sigh). numpy only (scipy is not needed), then ffmpeg encodes mono Vorbis, like tools/gen_mob_sounds.py.
Every variant is seeded from its own name, so a re-run writes the same audio.

  python3 -B tools/gen_spirit_space_sounds.py                 write every file under assets/nusmp/sounds/spirit_space/ and the "spirit_space.*" entries of sounds.json
                                                               (the other entries of sounds.json are not touched)
  python3 -B tools/gen_spirit_space_sounds.py --preview DIR   also write a .wav and a spectrogram .png of every file into DIR
  python3 -B tools/gen_spirit_space_sounds.py --check         verify that every file the entries name exists, is a valid mono Ogg and short

Events (one sound event each, nusmp:spirit_space.<id>, the same ids as spiritspace.SoundGate.Kind and spiritspace.SpaceMoodRules.Voice):
  hum ping unlink tear swell open gravity wisp deny          the cue sounds of the abilities (2 variants each)
  voice_whisper voice_chime voice_murmur voice_chirp voice_warble voice_trill voice_sigh     the creature cues (2 to 3 variants each)
"""
from __future__ import annotations

import argparse
import json
import math
import os
import re
import subprocess
import sys
import tempfile
import wave
import zlib

import numpy as np

sys.dont_write_bytecode = True
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
SOUND_DIR = os.path.join(ASSETS, "sounds", "spirit_space")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
SR = 44100
PEAK = 0.80                                    # the Vorbis encoder overshoots a little: leave headroom so the decoded file never clips


def rng_of(name):
    return np.random.default_rng(zlib.crc32(("spirit_space/" + name).encode()))


def t_of(sec):
    return np.arange(int(sec * SR), dtype=np.float64) / SR


def env_ar(n, a, r, curve=2.0):
    """An attack and a release (seconds) over n samples."""
    t = np.arange(n) / SR
    e = np.minimum(1.0, t / max(a, 1e-4)) * np.minimum(1.0, (n / SR - t) / max(r, 1e-4))
    return np.clip(e, 0, 1) ** curve


def osc(freq, phase0=0.0):
    """A sine with a (possibly moving) frequency array in Hz."""
    return np.sin(phase0 + 2 * np.pi * np.cumsum(freq) / SR)


def bandnoise(rng, n, fc, bw):
    """White noise through a band-pass (FFT mask with soft edges)."""
    spec = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / SR)
    mask = np.exp(-0.5 * ((f - fc) / max(bw, 1.0)) ** 2)
    return np.fft.irfft(spec * mask, n)


def lowpass(x, fc):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec *= 1.0 / (1.0 + (f / fc) ** 4)
    return np.fft.irfft(spec, len(x))


def highpass(x, fc):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec *= (f / fc) ** 4 / (1.0 + (f / fc) ** 4)
    return np.fft.irfft(spec, len(x))


def echo(x, delay, gain, taps=3):
    out = x.copy()
    d = int(delay * SR)
    for k in range(1, taps + 1):
        if k * d < len(x):
            out[k * d:] += x[:len(x) - k * d] * gain ** k
    return out


def glass(freq, dur, rng, decay=3.0, partials=(1.0, 2.76, 5.40, 8.93), amps=(1.0, 0.55, 0.30, 0.15)):
    """One glassy ping: inharmonic partials that ring and fade, a tiny click at the start."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    for p, a in zip(partials, amps):
        y += a * np.sin(2 * np.pi * freq * p * t + rng.uniform(0, 6.28)) * np.exp(-t * decay * (0.6 + 0.4 * p))
    y *= np.minimum(1.0, t / 0.002)
    return y


def place(buf, x, at):
    i = int(at * SR)
    n = min(len(x), len(buf) - i)
    if n > 0:
        buf[i:i + n] += x[:n]


# ------------------------------------------------------------------------------------------------------------------ the ability sounds
def hum(name, var):
    rng = rng_of(name)
    dur = 2.2
    n = int(dur * SR)
    t = np.arange(n) / SR
    base = 55.0 * (1.0 + 0.06 * var)
    y = np.zeros(n)
    for ratio, a, det in ((1.0, 1.0, 0.0), (1.5, 0.55, 0.4), (2.0, 0.4, -0.3), (3.0, 0.15, 0.7)):
        trem = 1.0 + 0.25 * np.sin(2 * np.pi * (0.35 + 0.1 * rng.random()) * t + rng.uniform(0, 6.28))
        y += a * osc(base * ratio + det + 0.8 * np.sin(2 * np.pi * 0.2 * t + ratio)) * trem
    y += 0.12 * lowpass(rng.standard_normal(n), 400)
    return y * env_ar(n, 0.5, 0.9, 1.5)


def ping(name, var):
    rng = rng_of(name)
    f = (880.0, 1318.5)[var % 2]
    y = glass(f, 1.0, rng, decay=3.2)
    y[int(0.04 * SR):] += 0.35 * glass(f * 1.5, 0.96, rng, decay=4.0)[:len(y) - int(0.04 * SR)]
    return y


def unlink(name, var):
    rng = rng_of(name)
    n = int(1.0 * SR)
    t = np.arange(n) / SR
    glide = 1100.0 * np.exp(-t * (1.6 + 0.4 * var)) + 220.0
    y = osc(glide) * np.exp(-t * 2.2) + 0.5 * osc(glide * 2.76) * np.exp(-t * 4.0)
    y += 0.08 * highpass(rng.standard_normal(n), 3000) * np.exp(-t * 6)
    return y * env_ar(n, 0.003, 0.25, 1.0)


def tear(name, var):
    rng = rng_of(name)
    dur = 0.95
    n = int(dur * SR)
    t = np.arange(n) / SR
    # a whoosh: noise whose band sweeps up then falls, a rip of crackle, a low boom
    y = np.zeros(n)
    bands = np.linspace(400, 7000, 9)
    for i, fc in enumerate(bands):
        center = 0.18 + 0.5 * (i / (len(bands) - 1)) * (1.0 if var == 0 else 0.7)
        amp = np.exp(-0.5 * ((t - center) / 0.12) ** 2)
        y += bandnoise(rng, n, fc, fc * 0.35) * amp
    crack = np.zeros(n)
    for k in range(int(40 + 20 * var)):
        at = rng.uniform(0.1, 0.6)
        i = int(at * SR)
        ln = int(rng.uniform(0.001, 0.004) * SR)
        crack[i:i + ln] += rng.standard_normal(min(ln, n - i)) * rng.uniform(0.3, 1.0) * np.exp(-at * 2)
    boom = osc(70.0 * np.exp(-t * 3) + 35) * np.exp(-t * 5) * 0.8
    y = y * 0.35 + highpass(crack, 1500) * 0.9 + boom
    return y * env_ar(n, 0.01, 0.25, 1.0)


def swell(name, var):
    rng = rng_of(name)
    dur = 1.6
    n = int(dur * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    for i in range(5):
        f0 = 180.0 * (1.5 ** i) * (1.0 + 0.01 * i * var)
        glide = f0 * (1.0 + 1.2 * (t / dur) ** 2)
        y += (0.7 ** i) * osc(glide + 0.5 * i, rng.uniform(0, 6.28))
    y += 0.5 * bandnoise(rng, n, 3000, 2500) * (t / dur) ** 2
    shimmer = 1.0 + 0.3 * np.sin(2 * np.pi * (6 + 4 * (t / dur)) * t)
    return y * shimmer * np.minimum(1.0, t / 1.2) ** 2 * np.minimum(1.0, (dur - t) / 0.12)


def open_(name, var):
    rng = rng_of(name)
    n = int(1.2 * SR)
    y = np.zeros(n)
    place(y, swell(name + "_s", var)[:int(0.55 * SR)] * np.linspace(0.3, 1.0, int(0.55 * SR)), 0.0)
    f = (660.0, 784.0)[var % 2]
    for k, ratio in enumerate((1.0, 1.5, 2.0)):
        place(y, glass(f * ratio, 0.9, rng, decay=3.0) * 0.8, 0.5 + 0.05 * k)
    return y


def gravity(name, var):
    rng = rng_of(name)
    n = int(1.0 * SR)
    t = np.arange(n) / SR
    thump = osc(90.0 * np.exp(-t * 6) + 38) * np.exp(-t * 5) * 1.2
    shimmer = osc(300 + 900 * t * (1 if var == 0 else -0.6) + 400) * np.exp(-t * 2.4) * 0.35
    air = lowpass(rng.standard_normal(n), 900) * np.exp(-t * 3) * 0.4
    return (thump + shimmer + air) * env_ar(n, 0.004, 0.2, 1.0)


def wisp(name, var):
    rng = rng_of(name)
    dur = 1.3
    n = int(dur * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    for ratio, a in ((1.0, 1.0), (1.5, 0.7), (2.01, 0.5), (3.0, 0.25)):
        y += a * osc(165.0 * (1 + 0.05 * var) * ratio * (1 + 0.5 * np.exp(-t * 4)) + 0.7 * ratio)
    y += 0.4 * bandnoise(rng, n, 1800, 1200) * np.exp(-((t - 0.25) / 0.2) ** 2)
    return y * env_ar(n, 0.05, 0.6, 1.5)


def deny(name, var):
    rng = rng_of(name)
    n = int(0.4 * SR)
    t = np.arange(n) / SR
    y = osc(180.0 - 40 * var) * np.exp(-t * 18) + 0.6 * osc(135.0 - 30 * var) * np.exp(-(t - 0.12).clip(0) * 18) * (t > 0.12)
    y += 0.2 * lowpass(rng.standard_normal(n), 1200) * np.exp(-t * 30)
    return y


# ------------------------------------------------------------------------------------------------------------------ the creature cues
def voice_whisper(name, var):
    rng = rng_of(name)
    dur = (1.2, 1.5, 1.0)[var]
    n = int(dur * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    centers = (500, 900, 1500, 2300, 3300, 4800)
    for i, fc in enumerate(centers):                                            # a moving band: the "vowel" glides up the bands and back, breathy
        phase = (t / dur) * (1.2 + 0.4 * var) + 0.1 * i
        amp = np.exp(-0.5 * ((np.sin(np.pi * phase) * 0.5 + 0.5 - i / (len(centers) - 1)) / 0.28) ** 2)
        y += bandnoise(rng, n, fc, fc * 0.18) * amp
    y *= (0.6 + 0.4 * np.sin(2 * np.pi * (3 + var) * t + 1.0) ** 2)
    y += 0.25 * osc(210.0 + 30 * var + 8 * np.sin(2 * np.pi * 4 * t)) * np.exp(-0.5 * ((t - dur * 0.5) / (dur * 0.25)) ** 2) * 0.3
    return echo(y * env_ar(n, 0.18, 0.5, 1.2), 0.09, 0.3, 3)


def voice_chime(name, var):
    rng = rng_of(name)
    sets = ((784.0, 988.0, 1175.0), (880.0, 1109.0, 1319.0), (698.5, 880.0, 1047.0))[var]
    n = int(1.4 * SR)
    y = np.zeros(n)
    for k, f in enumerate(sets):
        place(y, glass(f, 1.1, rng, decay=3.4), 0.0 + 0.16 * k)
    return echo(y, 0.18, 0.25, 2)


def voice_murmur(name, var):
    rng = rng_of(name)
    dur = (1.6, 1.9)[var]
    n = int(dur * SR)
    t = np.arange(n) / SR
    f0 = (118.0, 98.0)[var]
    vib = 1.0 + 0.02 * np.sin(2 * np.pi * 5.5 * t)
    y = np.zeros(n)
    for h, a in ((1, 1.0), (2, 0.6), (3, 0.35), (4, 0.2), (5, 0.1)):
        y += a * osc(f0 * h * vib * (1 + 0.1 * np.sin(np.pi * t / dur)))
    y += 0.12 * bandnoise(rng, n, 600, 300)
    return lowpass(y, 1400) * env_ar(n, 0.35, 0.6, 1.5)


def voice_chirp(name, var):
    rng = rng_of(name)
    n = int(0.45 * SR)
    y = np.zeros(n)
    base = (1300.0, 1500.0, 1150.0)[var]
    for k, (at, ratio) in enumerate(((0.0, 1.0), (0.13, 1.35))):               # two quick rising notes: a curious "bip-bweep"
        ln = int(0.16 * SR)
        t = np.arange(ln) / SR
        f = base * ratio * (1.0 + 0.55 * (t / 0.16) ** 1.5) * (1 + 0.02 * np.sin(2 * np.pi * 30 * t))
        note = osc(f) + 0.3 * osc(f * 2.01)
        place(y, note * env_ar(ln, 0.01, 0.06, 1.0), at)
    return y


def voice_warble(name, var):
    rng = rng_of(name)
    dur = (0.9, 1.1)[var]
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = (640.0 - 220.0 * (t / dur)) * (1.0 + 0.09 * np.sin(2 * np.pi * (9 + 2 * var) * t))            # a falling pitch with a fast wide vibrato: distress
    y = osc(f) + 0.5 * osc(f * 1.5 + 6.0) + 0.3 * osc(f * 2.02)
    y *= 1.0 + 0.3 * np.sin(2 * np.pi * 14 * t)
    y += 0.08 * bandnoise(rng, n, 1800, 900)
    return y * env_ar(n, 0.05, 0.3, 1.2)


def voice_trill(name, var):
    rng = rng_of(name)
    dur = (0.65, 0.8)[var]
    n = int(dur * SR)
    t = np.arange(n) / SR
    step = (np.floor(t * (14 + 2 * var)) % 2)
    f = (1100.0 + 500.0 * step) * (1.0 + 0.4 * (t / dur))
    y = osc(f) + 0.35 * osc(f * 2.0)
    y *= 0.6 + 0.4 * np.abs(np.sin(np.pi * t * (14 + 2 * var)))
    return y * env_ar(n, 0.02, 0.2, 1.0)


def voice_sigh(name, var):
    rng = rng_of(name)
    dur = (1.3, 1.6)[var]
    n = int(dur * SR)
    t = np.arange(n) / SR
    breath = lowpass(rng.standard_normal(n), 1800)
    f = 340.0 * np.exp(-t * 0.9) + 90.0
    tone = osc(f) * 0.5 + 0.3 * osc(f * 2.0)
    y = (breath * 0.7 + tone * 0.6) * np.exp(-0.5 * ((t - dur * 0.3) / (dur * 0.35)) ** 2)
    return y * env_ar(n, 0.2, 0.5, 1.2)


# id -> (function, variants, peak, attenuation distance, subtitle text)
CUES = {
    "hum": (hum, 2, 0.45, 20, "The Spirit of Space hums"),
    "ping": (ping, 2, 0.6, 24, "The Spirit of Space chimes"),
    "unlink": (unlink, 2, 0.6, 20, "The Spirit of Space fades"),
    "tear": (tear, 2, 0.85, 32, "Space tears"),
    "swell": (swell, 2, 0.6, 24, "A portal swells"),
    "open": (open_, 2, 0.7, 32, "A portal opens"),
    "gravity": (gravity, 2, 0.8, 28, "Gravity warps"),
    "wisp": (wisp, 2, 0.65, 28, "The spirit swells into a dragon"),
    "deny": (deny, 2, 0.5, 16, "The Spirit of Space refuses"),
    "voice_whisper": (voice_whisper, 3, 0.55, 32, "The Spirit of Space whispers"),
    "voice_chime": (voice_chime, 3, 0.6, 32, "The Spirit of Space rings"),
    "voice_murmur": (voice_murmur, 2, 0.5, 32, "The Spirit of Space murmurs"),
    "voice_chirp": (voice_chirp, 3, 0.55, 32, "The Spirit of Space chirps"),
    "voice_warble": (voice_warble, 2, 0.55, 32, "The Spirit of Space warbles anxiously"),
    "voice_trill": (voice_trill, 2, 0.55, 32, "The Spirit of Space trills"),
    "voice_sigh": (voice_sigh, 2, 0.5, 32, "The Spirit of Space sighs"),
}


def finish(y, peak):
    y = np.nan_to_num(y)
    y = y - np.mean(y)
    m = float(np.max(np.abs(y))) or 1.0
    y = y / m * peak * PEAK / 0.85
    fade = int(0.01 * SR)
    y[:fade] *= np.linspace(0, 1, fade)
    y[-fade:] *= np.linspace(1, 0, fade)
    return np.clip(y, -PEAK, PEAK)


def write_wav(path, y):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((y * 32767).astype("<i2").tobytes())


def encode(wav, ogg):
    os.makedirs(os.path.dirname(ogg), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", ogg], check=True)


def entry_text(cue, variants, att):
    names = ",\n".join('      { "name": "nusmp:spirit_space/%s_%d", "attenuation_distance": %d }' % (cue, v + 1, att) for v in range(variants))
    return '  "spirit_space.%s": {\n    "subtitle": "subtitles.nusmp.spirit_space.%s",\n    "sounds": [\n%s\n    ]\n  }' % (cue, cue, names)


def write_sounds_json():
    """Replaces the 'spirit_space.*' entries of sounds.json (text edit, the other entries and their layout are not touched) and appends them at the end."""
    text = open(SOUNDS_JSON, encoding="utf-8").read()
    text = re.sub(r',\n  "spirit_space\.[a-z_]+": \{.*?\n  \}(?=,\n  "|\n\})', "", text, flags=re.S)
    block = ",\n".join(entry_text(c, v, att) for c, (_, v, _, att, _) in CUES.items())
    end = text.rstrip().rfind("}")
    head = text[:end].rstrip()
    text = head + ",\n" + block + "\n}\n"
    json.loads(text)                                                            # it must still parse
    open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n").write(text)


def check():
    doc = json.load(open(SOUNDS_JSON, encoding="utf-8"))
    bad = 0
    for key, e in doc.items():
        if not key.startswith("spirit_space."):
            continue
        for s in e["sounds"]:
            f = os.path.join(ASSETS, "sounds", s["name"].split(":")[1] + ".ogg")
            if not os.path.isfile(f):
                print("missing", f)
                bad += 1
    print("check:", "ok" if not bad else "%d problems" % bad)
    return bad


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    if a.check:
        sys.exit(1 if check() else 0)
    total = 0
    with tempfile.TemporaryDirectory() as tmp:
        for cue, (fn, variants, peak, att, _) in CUES.items():
            for v in range(variants):
                name = "%s_%d" % (cue, v + 1)
                y = finish(fn(name, v), peak)
                wav = os.path.join(tmp, name + ".wav")
                write_wav(wav, y)
                ogg = os.path.join(SOUND_DIR, name + ".ogg")
                encode(wav, ogg)
                total += os.path.getsize(ogg)
                if a.preview:
                    os.makedirs(a.preview, exist_ok=True)
                    write_wav(os.path.join(a.preview, name + ".wav"), y)
                print("%-18s %.2f s  %d bytes" % (name, len(y) / SR, os.path.getsize(ogg)))
    write_sounds_json()
    print("total %d files, %d bytes" % (sum(v for _, v, _, _, _ in CUES.values()), total))


if __name__ == "__main__":
    main()
