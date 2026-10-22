#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the sound set of the Gnome, the Earth Spirit (docs/magic/gnome.md): NOT the basic Tensura spirit sounds but a deep rumble, stone grinding and crashing, and a low resonant earth tone.
No recording was used: numpy writes every sample and ffmpeg encodes mono Ogg Vorbis. Reproducible: every variant is seeded from its own name.

  python3 -B tools/gen_gnome_sounds.py              write assets/nusmp/sounds/gnome/<cue>_<n>.ogg, the gnome.* entries of sounds.json and the subtitle lines of tools/lang/gnome.json
  python3 -B tools/gen_gnome_sounds.py --preview D  also write a .wav and a spectrogram .png per file into D
  python3 -B tools/gen_gnome_sounds.py --check      verify that the files exist, are mono Ogg, short and not clipped

The cue table (names, variants, subtitles) is the one of gnome/GnomeSoundRules.java; GnomeSoundsTest reads both and fails when they differ. Engines:
  RUMBLE  low-passed brown noise with a slow swell, a sub-bass sine with a little drift and a beating pair a few Hz apart (the felt weight)
  GRIND   stick-slip friction: band-passed noise gated by an irregular pulse train that speeds up and slows down, plus stone-on-stone squeal partials (the grinding)
  CRASH   a noise burst with a fast decay, a shower of stone clacks (inharmonic ringing modes with random pitch) and a sub thud sweeping 95 -> 34 Hz (the slam)
  TONE    the low resonant earth tone: a fundamental near 55 to 82 Hz with a few stone partials (ratios 1, 1.51, 2.76, 4.16) that ring and slowly beat, a slow attack and a long tail
  GRUFF   the non-verbal voice of the golem (grunt, chuckle, snore, huff; appended after the seven cues above): an irregular "vocal fry" pulse train at 38 to 80 Hz (a glottal source) shaped by
          low vowel formants (a few hundred Hz to about two kHz), with breath noise and a little stone grit under it. Synthesised from nothing like the rest: it is NOT a person's recording
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
import wave
import zlib

import numpy as np

sys.dont_write_bytecode = True
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
SOUND_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds", "gnome")
SOUNDS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds.json")
LANG_OUT = os.path.join(ROOT, "tools", "lang", "gnome.json")
SR = 32000
PEAK = 0.80                        # -2 dBFS: the Vorbis encoder overshoots a little
MIN_S, MAX_S, STREAM_OVER = 0.3, 3.0, 2.4

# cue -> (variants, subtitle, loudness dBFS rms of the active part)
CUES = {
    "link": (3, "The Gnome rises beside its master", -21.0),
    "unlink": (2, "The Gnome sinks into the earth", -22.0),
    "idle": (3, "The Gnome grinds and rumbles", -27.0),
    "cast": (3, "The Gnome grinds stone", -19.0),
    "rise": (3, "The Gnome heaves out of the ground", -17.0),
    "slam": (3, "The Gnome slams the earth", -16.0),
    "sink": (2, "The Gnome sinks into the ground", -19.0),
    "grunt": (4, "The Gnome grunts", -20.0),
    "chuckle": (2, "The Gnome chuckles deeply", -22.0),
    "snore": (2, "The Gnome snores", -26.0),
    "huff": (2, "The Gnome huffs", -23.0),
}


# ---------------------------------------------------------------------------------------------- helpers
def rng_for(name):
    return np.random.default_rng(zlib.crc32(name.encode()) & 0xFFFFFFFF)


def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def fft_filter(x, lo=None, hi=None, slope=4.0):
    """A smooth band filter in the frequency domain: pass between lo and hi (Hz), with a soft skirt."""
    n = len(x)
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1.0 / SR)
    g = np.ones_like(f)
    if hi:
        g *= 1.0 / (1.0 + (f / hi) ** (2 * slope))
    if lo:
        g *= 1.0 - 1.0 / (1.0 + (f / lo) ** (2 * slope))
    return np.fft.irfft(X * g, n)


def brown(rng, n):
    w = rng.standard_normal(n)
    b = np.cumsum(w)
    b -= np.linspace(b[0], b[-1], n)                                  # no drift
    return b / (np.max(np.abs(b)) + 1e-9)


def env_ad(n, a, d_pow=2.0):
    """attack of a seconds then a decay to 0 over the rest (power law)."""
    t = np.arange(n) / SR
    dur = n / SR
    att = np.clip(t / max(a, 1e-3), 0, 1)
    dec = np.clip(1 - (t - a) / max(dur - a, 1e-3), 0, 1) ** d_pow
    return np.where(t < a, att, dec)


def swell(n, peak_at=0.4, power=1.5):
    t = np.linspace(0, 1, n)
    up = (t / peak_at) ** power
    down = ((1 - t) / (1 - peak_at)) ** power
    return np.where(t < peak_at, up, down)


def fades(x, fi=0.004, fo=0.05):
    n = len(x)
    a, b = int(fi * SR), int(fo * SR)
    if a > 0:
        x[:a] *= np.linspace(0, 1, a)
    if b > 0:
        x[-b:] *= np.linspace(1, 0, b)
    return x


def mix(*layers):
    n = max(len(l) for l in layers)
    out = np.zeros(n)
    for l in layers:
        out[:len(l)] += l
    return out


def at(x, start, dur_total):
    out = np.zeros(int(dur_total * SR))
    s = int(start * SR)
    m = min(len(x), len(out) - s)
    if m > 0:
        out[s:s + m] = x[:m]
    return out


# ---------------------------------------------------------------------------------------------- engines
def rumble(rng, dur, depth=1.0, f_lo=95.0, peak_at=0.35):
    n = int(dur * SR)
    t = t_axis(dur)
    bn = fft_filter(brown(rng, n), hi=f_lo, slope=3)
    bn = bn / (np.max(np.abs(bn)) + 1e-9)
    drift = 1 + 0.04 * np.sin(2 * np.pi * rng.uniform(0.3, 0.8) * t + rng.uniform(0, 6))
    f0 = rng.uniform(36, 48)
    sub = np.sin(2 * np.pi * np.cumsum(f0 * drift) / SR) * 0.55 + np.sin(2 * np.pi * np.cumsum((f0 + rng.uniform(2.5, 4.0)) * drift) / SR) * 0.35
    body = fft_filter(rng.standard_normal(n), lo=60, hi=240, slope=3) * 0.18 * (0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(5, 9) * t))
    return (bn * 0.75 + sub * 0.7 + body) * swell(n, peak_at) * depth


def grind(rng, dur, lo=260.0, hi=1700.0, rate=(12.0, 26.0), level=1.0, squeal=True):
    n = int(dur * SR)
    t = t_axis(dur)
    noise = fft_filter(rng.standard_normal(n), lo=lo, hi=hi, slope=3)
    # stick-slip: an irregular pulse train whose speed wanders
    speed = rng.uniform(*rate) * (1 + 0.35 * np.sin(2 * np.pi * rng.uniform(0.4, 1.1) * t + rng.uniform(0, 6)))
    phase = np.cumsum(speed) / SR
    saw = (phase % 1.0)
    gate = (1.0 - saw) ** 2.2
    jitter = fft_filter(rng.standard_normal(n), hi=40, slope=2)
    jitter = 0.65 + 0.35 * jitter / (np.max(np.abs(jitter)) + 1e-9)
    g = noise * gate * jitter
    g = g / (np.max(np.abs(g)) + 1e-9)
    low = fft_filter(rng.standard_normal(n), lo=70, hi=380, slope=3) * 0.6 * (0.4 + 0.6 * gate)
    out = g * 0.8 + low * 0.5
    if squeal:                                                        # stone squeal: two faint partials that slide
        f = rng.uniform(520, 900) * (1 + 0.12 * np.sin(2 * np.pi * rng.uniform(0.6, 1.6) * t))
        sq = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.05 * (0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(2, 4) * t)) ** 2
        out = out + sq
    return out * level


def crash(rng, dur, clacks=34, power=1.0):
    n = int(dur * SR)
    t = t_axis(dur)
    out = np.zeros(n)
    # the burst: noise with a fast decay, low-passed a little
    burst = fft_filter(rng.standard_normal(n), lo=80, hi=5200, slope=2) * np.exp(-t * rng.uniform(7, 10))
    out += burst * 0.9
    # the thud: a sine sweeping down
    f = 34 + 61 * np.exp(-t * 11.0)
    thud = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 3.0)
    out += thud * 1.25 * power
    # the clacks: inharmonic ringing modes, a shower that thins out
    for k in range(clacks):
        st = rng.exponential(0.16) * (0.4 + k / clacks)
        if st >= dur * 0.9:
            continue
        base = rng.uniform(180, 1500)
        dec = rng.uniform(25, 90)
        amp = rng.uniform(0.2, 0.75) * np.exp(-st * 3.0)
        m = np.zeros(n)
        i0 = int(st * SR)
        tt = t[: n - i0]
        for ratio, a in ((1.0, 1.0), (1.51, 0.6), (2.76, 0.35), (4.16, 0.2)):
            m[i0:] += a * np.sin(2 * np.pi * base * ratio * tt) * np.exp(-tt * dec * ratio ** 0.6)
        out += m * amp * 0.5
    return out


def tone(rng, dur, f0=None, attack=0.5, tail=1.6, level=1.0, glide=0.0):
    n = int(dur * SR)
    t = t_axis(dur)
    f0 = f0 or rng.uniform(55, 82)
    f = f0 * (1 + glide * (t / dur))
    out = np.zeros(n)
    for ratio, a, beat in ((1.0, 1.0, 0.7), (1.51, 0.42, 1.3), (2.0, 0.30, 0.9), (2.76, 0.18, 1.9), (4.16, 0.09, 2.6)):
        ph = np.cumsum(f * ratio) / SR
        out += a * (np.sin(2 * np.pi * ph) + 0.6 * np.sin(2 * np.pi * (ph + beat * t)))
    env = np.minimum(1.0, t / max(attack, 1e-3)) ** 1.5 * np.exp(-np.maximum(0.0, t - attack) / max(tail * 0.45, 1e-3))
    return out * env * 0.4 * level


def fry(rng, dur, f0, f_end=None, irregular=0.28, amp_var=0.35):
    """A glottal source with 'vocal fry': impulses whose spacing and strength are irregular, each one softened to a low pulse."""
    n = int(dur * SR)
    f_end = f_end or f0
    x = np.zeros(n)
    tt = 0.0
    while tt < dur:
        i = int(tt * SR)
        if i < n:
            x[i] += 1.0 - amp_var * rng.random()
        f = f0 + (f_end - f0) * (tt / dur)
        tt += (1.0 / f) * max(0.4, 1.0 + irregular * rng.standard_normal())
    return fft_filter(x, hi=700, slope=2) * 5.0


def formants(x, spec, floor=0.04):
    """The mouth: sum of smooth peaks (centre Hz, width Hz, gain) in the spectrum, with a small floor so nothing is silent."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1.0 / SR)
    g = np.full_like(f, floor)
    for c, w, a in spec:
        g += a * np.exp(-0.5 * ((f - c) / w) ** 2)
    return np.fft.irfft(X * g, len(x))


def breath(rng, dur, lo=150.0, hi=1800.0):
    return fft_filter(rng.standard_normal(int(dur * SR)), lo=lo, hi=hi, slope=2)


def gruff(rng, kind, v):
    if kind == "grunt":
        dur = 0.5 + 0.09 * v
        n = int(dur * SR)
        top = rng.uniform(62, 78)
        src = fry(rng, dur, top, top * rng.uniform(0.62, 0.74))
        o = [(rng.uniform(330, 440), 85, 1.0), (rng.uniform(720, 900), 140, 0.55), (rng.uniform(2200, 2500), 300, 0.08)]
        body = formants(src, o) * env_ad(n, 0.05, 1.2)
        air = formants(breath(rng, dur), [(450, 220, 1.0), (1300, 400, 0.4)]) * 0.16 * env_ad(n, 0.02, 1.6)
        grit = grind(rng, dur, lo=200, hi=900, rate=(18, 30), level=0.35, squeal=False) * env_ad(n, 0.04, 1.8)
        sub = np.sin(2 * np.pi * np.cumsum(np.linspace(top * 0.55, top * 0.4, n)) / SR) * 0.35 * env_ad(n, 0.05, 1.5)
        return body * 1.0 + air + grit * 0.5 + sub, dur
    if kind == "chuckle":
        beats = 3 + v
        dur = 0.22 + 0.27 * beats
        out = np.zeros(int(dur * SR))
        for b in range(beats):
            d = 0.2
            n = int(d * SR)
            f0 = rng.uniform(70, 82) * (1.0 - 0.05 * b)
            src = fry(rng, d, f0, f0 * 0.82, irregular=0.2)
            body = formants(src, [(rng.uniform(480, 560), 100, 1.0), (rng.uniform(1000, 1150), 160, 0.55), (2400, 300, 0.07)]) * env_ad(n, 0.025, 1.5)
            h = formants(breath(rng, d), [(900, 400, 1.0)]) * 0.2 * env_ad(n, 0.01, 2.2)
            amp = 1.0 - 0.12 * b
            out += at((body + h) * amp, 0.02 + 0.27 * b, dur)
        rum = rumble(rng, dur, 0.3, peak_at=0.4)
        return out + rum * 0.4, dur
    if kind == "snore":
        dur = 2.3 + 0.1 * v
        n = int(dur * SR)
        t = t_axis(dur)
        a_n = int(0.95 * SR)
        rattle = 0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(24, 32) * t[:a_n])
        inh = formants(breath(rng, 0.95, 80, 900), [(260, 120, 1.0), (700, 200, 0.5)]) * rattle ** 1.5 * swell(a_n, 0.7, 1.4) * 1.4
        e_n = int(1.0 * SR)
        exh_src = fry(rng, 1.0, 40, 33, irregular=0.35)
        exh = formants(exh_src, [(300, 110, 1.0), (640, 180, 0.5)]) * swell(e_n, 0.35, 1.3) * 1.0
        exh += formants(breath(rng, 1.0, 100, 700), [(350, 150, 1.0)]) * 0.15 * swell(e_n, 0.35, 1.3)
        out = mix(at(inh, 0.05, dur), at(exh, 1.2, dur), rumble(rng, dur, 0.25, peak_at=0.5))
        return out, dur
    # huff: a breath through the stone
    dur = 0.55 + 0.1 * v
    n = int(dur * SR)
    air = formants(breath(rng, dur, 100, 2400), [(420, 200, 1.0), (1150, 320, 0.5)]) * env_ad(n, 0.04, 1.7)
    thump = np.sin(2 * np.pi * np.cumsum(np.linspace(72, 48, n)) / SR) * 0.28 * env_ad(n, 0.02, 2.2)
    return air * 1.3 + thump + grind(rng, dur, lo=150, hi=700, rate=(14, 22), level=0.2, squeal=False) * env_ad(n, 0.05, 2.0) * 0.4, dur


def shape_lufs(x, target_db):
    x = x - np.mean(x)
    act = np.abs(x) > 0.02 * (np.max(np.abs(x)) + 1e-9)
    rms = np.sqrt(np.mean(x[act] ** 2)) + 1e-9
    x = x * (10 ** (target_db / 20.0) / rms)
    pk = np.max(np.abs(x))
    if pk > PEAK:                                                     # a soft knee instead of a hard clip
        x = np.tanh(x / PEAK * 1.1) * PEAK / np.tanh(1.1)
    return x


# ---------------------------------------------------------------------------------------------- the cues
def make(cue, k):
    name = "%s_%d" % (cue, k)
    rng = rng_for("gnome." + name)
    v = k - 1
    if cue == "link":
        dur = 1.5 + 0.1 * v
        x = mix(tone(rng, dur, attack=0.55, tail=1.2), at(grind(rng, dur * 0.7, level=0.55), 0.1, dur), at(rumble(rng, dur * 0.8, 0.55), 0.0, dur), at(crash(rng, 0.7, 7, 0.35), dur * 0.62, dur))
    elif cue == "unlink":
        dur = 1.4 + 0.1 * v
        x = mix(tone(rng, dur, attack=0.12, tail=1.1, glide=-0.28), grind(rng, dur, level=0.5, rate=(9, 16)) * swell(int(dur * SR), 0.25), rumble(rng, dur, 0.5, peak_at=0.25))
    elif cue == "idle":
        dur = 1.8 + 0.2 * v
        x = mix(grind(rng, dur, level=0.5, rate=(7, 12), lo=180, hi=900) * swell(int(dur * SR), 0.5, 2.0), tone(rng, dur, attack=0.8, tail=1.0, level=0.5), rumble(rng, dur, 0.35, peak_at=0.5))
    elif cue == "cast":
        dur = 0.95 + 0.1 * v
        x = mix(grind(rng, dur * 0.8, level=0.9, rate=(16, 28)) * env_ad(int(dur * 0.8 * SR), 0.05, 1.3), at(crash(rng, 0.7, 12, 0.5), dur * 0.35, dur), tone(rng, dur, attack=0.1, tail=0.6, level=0.5))
    elif cue == "rise":
        dur = 2.8 + 0.05 * v
        n = int(dur * SR)
        x = mix(rumble(rng, dur, 1.5, peak_at=0.40), grind(rng, dur, level=1.0, rate=(8, 18), lo=200, hi=1400) * swell(n, 0.45, 1.4),
                at(crash(rng, 1.4, 22, 0.55), 0.55 + 0.05 * v, dur), tone(rng, dur, attack=0.9, tail=1.5, level=0.6, glide=0.18))
    elif cue == "slam":
        dur = 1.8 + 0.1 * v
        x = mix(crash(rng, dur, 46, 1.0), tone(rng, dur, attack=0.02, tail=1.6, f0=rng.uniform(52, 70), level=0.95), rumble(rng, dur, 0.55, peak_at=0.05))
    elif cue in ("grunt", "chuckle", "snore", "huff"):
        x, dur = gruff(rng, cue, v)
    else:                                                                  # sink
        dur = 2.1 + 0.1 * v
        n = int(dur * SR)
        x = mix(rumble(rng, dur, 1.2, peak_at=0.30), grind(rng, dur, level=0.8, rate=(9, 16)) * swell(n, 0.3, 1.4), tone(rng, dur, attack=0.05, tail=1.8, glide=-0.35, level=0.7),
                at(crash(rng, 0.8, 8, 0.4), 0.05, dur))
    x = fades(x, 0.004, 0.07 if dur > 1.0 else 0.04)
    return shape_lufs(x, CUES[cue][2]), dur


# ---------------------------------------------------------------------------------------------- output
def write_wav(path, x):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def encode(wav, ogg):
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", ogg], check=True)


def decoded_peak(ogg):
    r = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", ogg, "-f", "s16le", "-ac", "1", "-"], capture_output=True, check=True)
    a = np.frombuffer(r.stdout, dtype="<i2").astype(np.float32) / 32768.0
    return float(np.max(np.abs(a))) if len(a) else 0.0, len(a) / SR


def spectrogram(x, path):
    from PIL import Image
    n = 1024
    hop = 256
    win = np.hanning(n)
    cols = []
    for i in range(0, len(x) - n, hop):
        s = np.abs(np.fft.rfft(x[i:i + n] * win))
        cols.append(20 * np.log10(s[:200] + 1e-6))
    a = np.array(cols).T[::-1]
    a = np.clip((a - a.max() + 70) / 70, 0, 1)
    Image.fromarray((a * 255).astype(np.uint8)).resize((a.shape[1], a.shape[0] * 2)).save(path)


def entries_text():
    lines = []
    for cue, (nvar, sub, _) in CUES.items():
        sounds = []
        for k in range(1, nvar + 1):
            ogg = os.path.join(SOUND_DIR, "%s_%d.ogg" % (cue, k))
            dur = decoded_peak(ogg)[1] if os.path.exists(ogg) else 0.0
            sounds.append('{ "name": "nusmp:gnome/%s_%d", "stream": %s }' % (cue, k, "true" if dur >= STREAM_OVER else "false"))
        lines.append('  "gnome.%s": {\n    "subtitle": "subtitles.nusmp.gnome.%s",\n    "sounds": [ %s ]\n  }' % (cue, cue, ", ".join(sounds)))
    return ",\n".join(lines)


def update_sounds_json():
    raw = open(SOUNDS_JSON, newline="").read()
    cut = raw.find(',\n  "gnome.link"')
    tail = "\n}\n"
    if cut >= 0:                                                                  # replace the gnome.* block, keep whatever follows it (the voice lines of tools/audio/gnome_voice.py)
        import re
        last = None
        for m in re.finditer(r'"gnome\.[a-z_]+": \{.*?\n  \}', raw[cut:], re.S):
            last = m
        tail = raw[cut + last.end():] if last else tail
        raw = raw[:cut] + "\n}\n"
    end = raw.rstrip().rfind("}")
    head = raw[:end].rstrip()
    out = head + ",\n" + entries_text() + tail
    json.loads(out)
    open(SOUNDS_JSON, "w", newline="").write(out)
    lang = json.load(open(LANG_OUT)) if os.path.exists(LANG_OUT) else {}
    lang.update({"subtitles.nusmp.gnome.%s" % c: s for c, (_, s, _) in CUES.items()})        # the subtitles join the other text of the Gnome in the one fragment
    with open(LANG_OUT, "w", newline="\n") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


def check():
    bad = 0
    for cue, (nvar, _, _) in CUES.items():
        for k in range(1, nvar + 1):
            ogg = os.path.join(SOUND_DIR, "%s_%d.ogg" % (cue, k))
            if not os.path.exists(ogg):
                print("MISSING", ogg)
                bad += 1
                continue
            pk, dur = decoded_peak(ogg)
            ok = MIN_S <= dur <= MAX_S and pk <= 0.97
            if not ok:
                bad += 1
            print("%-12s %.2f s  peak %.3f %s" % ("%s_%d" % (cue, k), dur, pk, "ok" if ok else "BAD"))
    return bad


def main():
    if "--check" in sys.argv:
        sys.exit(1 if check() else 0)
    prev = None
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(prev, exist_ok=True)
    os.makedirs(SOUND_DIR, exist_ok=True)
    only = [a for a in sys.argv[1:] if not a.startswith("--") and a != prev]
    with tempfile.TemporaryDirectory() as tmp:
        for cue, (nvar, _, _) in CUES.items():
            if only and cue not in only:
                continue
            for k in range(1, nvar + 1):
                x, dur = make(cue, k)
                wav = os.path.join(tmp, "x.wav")
                ogg = os.path.join(SOUND_DIR, "%s_%d.ogg" % (cue, k))
                write_wav(wav, x)
                encode(wav, ogg)
                pk, d2 = decoded_peak(ogg)
                if pk > 0.97:                                           # the decoded file overshoots: re-encode a little quieter
                    write_wav(wav, x * 0.9)
                    encode(wav, ogg)
                    pk, d2 = decoded_peak(ogg)
                print("%-10s %.2f s  peak %.3f  %d bytes" % ("%s_%d" % (cue, k), d2, pk, os.path.getsize(ogg)))
                if prev:
                    write_wav(os.path.join(prev, "%s_%d.wav" % (cue, k)), x)
                    spectrogram(x, os.path.join(prev, "%s_%d.png" % (cue, k)))
    update_sounds_json()


if __name__ == "__main__":
    main()
