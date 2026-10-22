#!/usr/bin/env python3
"""Synthesises the sound set of the Spirit of Light (docs/magic/spirit_light.md): numpy only, then ffmpeg encodes mono Ogg Vorbis (the project's procedural .ogg synthesis, like tools/gen_mob_sounds.py).

It is NOT the basic Tensura spirit's set and not the mob voice engine's: bright inharmonic bell chimes (link, unlink), a choir-like pad (pulse, the seraph rising), the beam's zap, a soft lion purr, and the
lion cub's CREATURE sounds with no spoken word at all: a mew, a small roar, a yawn, a happy chirp, a whimper. Every variant is seeded from its own name, so a re-run writes the same audio.

    python3 -B tools/gen_spirit_light_sounds.py             writes sounds/spirit_light/<name>_<n>.ogg, the events in sounds.json (the block of tools/audio/sl_sounds_json.py) and tools/lang/spirit_light_sounds.json
    python3 -B tools/gen_spirit_light_sounds.py --preview D also writes a .wav per file into D (to look at them without the game)
"""
import argparse
import hashlib
import json
import math
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "audio"))
import sl_sounds_json as SJ  # noqa: E402

ROOT = SJ.ROOT
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds", "spirit_light")
LANG = os.path.join(ROOT, "tools", "lang", "spirit_light_sounds.json")
SR = 44100
PEAK = 0.80

# event name -> (variants, subtitle)
EVENTS = {
    "link": (2, "The Spirit of Light chimes"), "unlink": (2, "The Spirit of Light fades"), "beam": (3, "A beam of light zaps"), "pulse": (2, "The Spirit of Light pulses"),
    "power": (2, "The Spirit of Light rises"), "purr": (2, "The light cub purrs"), "cub_mew": (2, "The light cub mews"), "cub_roar": (2, "The light cub roars"),
    "cub_yawn": (2, "The light cub yawns"), "cub_chirp": (3, "The light cub chirps"), "cub_whimper": (2, "The light cub whimpers"),
}


def rng_for(name):
    return np.random.default_rng(int.from_bytes(hashlib.sha1(name.encode()).digest()[:8], "little"))


def t_of(seconds):
    return np.arange(int(seconds * SR)) / SR


def env(n, attack, release, curve=2.0):
    e = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    if a > 0:
        e[:a] = np.linspace(0, 1, a) ** curve
    if r > 0:
        e[-r:] *= np.linspace(1, 0, r) ** curve
    return e


def fshape(x, fn):
    """Zero phase filter by a magnitude function of the frequency."""
    n = len(x)
    size = 1 << int(math.ceil(math.log2(max(16, n * 2))))
    spec = np.fft.rfft(x, size) * fn(np.fft.rfftfreq(size, 1.0 / SR))
    return np.fft.irfft(spec, size)[:n]


def lp(x, fc, order=2):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (f / fc) ** (2 * order)))


def hp(x, fc, order=2):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (fc / np.maximum(f, 1e-3)) ** (2 * order)))


def bp(x, f0, q):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (q * (f / f0 - f0 / np.maximum(f, 1e-3))) ** 2))


def bell(freq, dur, rng, bright=1.0, decay=3.0):
    """A struck bell: inharmonic partials with their own decays and a soft click."""
    t = t_of(dur)
    out = np.zeros_like(t)
    for ratio, amp, dec in ((1.0, 1.0, 1.0), (2.0, 0.55, 1.4), (2.76, 0.45 * bright, 1.8), (4.07, 0.30 * bright, 2.4), (5.4, 0.22 * bright, 3.0), (8.93, 0.10 * bright, 4.5)):
        ph = rng.uniform(0, math.tau)
        out += amp * np.sin(math.tau * freq * ratio * t + ph) * np.exp(-t * decay * dec)
    out *= env(len(t), 0.002, 0.02)
    return out


def choir(freqs, dur, rng, vowel="ah", swell=0.5, vib=5.2, drift=0.0):
    """A choir-like pad: a stack of detuned, vibrato'd harmonic voices through vowel formants."""
    t = t_of(dur)
    n = len(t)
    formants = {"ah": ((800, 1.0), (1150, 0.7), (2900, 0.3)), "oo": ((350, 1.0), (800, 0.4), (2800, 0.15)), "ee": ((300, 0.8), (2300, 1.0), (3000, 0.4))}[vowel]
    out = np.zeros(n)
    for f0 in freqs:
        for k in range(3):                                                                    # three slightly detuned voices per note
            det = 1.0 + (k - 1) * 0.004 + rng.uniform(-0.001, 0.001)
            vib_ph = rng.uniform(0, math.tau)
            ff = f0 * det * (1 + drift * t / dur) * (1 + 0.004 * np.sin(math.tau * vib * t + vib_ph))
            phase = np.cumsum(ff) / SR
            src = np.zeros(n)
            for h in range(1, 14):
                src += np.sin(math.tau * h * phase) / (h ** 1.1)
            out += src
    shaped = sum(a * bp(out, fc, 5.0) for fc, a in formants) + 0.15 * lp(out, 900)
    e = np.minimum(1.0, t / max(1e-3, swell * dur)) ** 1.5 * env(n, 0.0, 0.35 * dur)
    return shaped * e


def noise(n, rng):
    return rng.standard_normal(n)


def finish(x, peak=PEAK):
    x = x - np.mean(x)
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


def mix(*parts):
    n = max(len(p[0]) + p[1] for p in parts)
    out = np.zeros(n)
    for sig, at in parts:
        out[at:at + len(sig)] += sig
    return out


# ============================================================================================================ the sounds
PENTA = [0, 2, 4, 7, 9, 12, 14, 16]


def note(root, semis):
    return root * 2 ** (semis / 12.0)


def snd_link(i, rng):
    root = (523.25, 587.33)[i]
    notes = [(0, 0.00), (4, 0.10), (7, 0.20), (12, 0.30)]
    parts = [(bell(note(root, s), 1.2, rng, 1.0, 3.2) * 0.8, int(at * SR)) for s, at in notes]
    pad = choir([note(root / 2, 0), note(root / 2, 7)], 1.3, rng, "oo", 0.4) * 0.25
    return mix(*parts, (pad, 0))


def snd_unlink(i, rng):
    root = (587.33, 523.25)[i]
    notes = [(12, 0.0), (7, 0.12), (4, 0.24), (0, 0.38)]
    parts = [(bell(note(root, s), 1.3, rng, 0.8, 3.0) * 0.7, int(at * SR)) for s, at in notes]
    pad = choir([note(root / 2, 0), note(root / 2, 7)], 1.4, rng, "oo", 0.2) * 0.2
    return mix(*parts, (pad, 0))


def snd_beam(i, rng):
    dur = 0.5
    t = t_of(dur)
    f0 = (2400, 2900, 3300)[i]
    sweep = f0 * np.exp(-t * 6.0) + 380
    zap = np.sin(np.cumsum(sweep) / SR * math.tau) * np.exp(-t * 7.5)
    shimmer = hp(noise(len(t), rng), 4200) * np.exp(-t * 9.0) * 0.16
    low = np.sin(np.cumsum(180 + 520 * np.exp(-t * 14)) / SR * math.tau) * np.exp(-t * 10) * 0.7
    ring = bell(f0 * 0.5, dur, rng, 1.2, 6.0) * 0.35
    return (zap + shimmer + low + ring) * env(len(t), 0.001, 0.05)


def snd_pulse(i, rng):
    dur = 1.9
    root = (392.0, 440.0)[i]
    pad = choir([note(root / 2, 0), note(root / 2, 7), note(root, 4), note(root, 12)], dur, rng, "ah", 0.45)
    t = t_of(dur)
    rise = hp(noise(len(t), rng), 3000) * np.minimum(1, t / 0.9) ** 3 * env(len(t), 0, 0.5) * 0.04
    hit = bell(note(root, 12), 1.2, rng, 1.0, 2.6) * 0.7
    return mix((pad * 0.9, 0), (rise, 0), (hit, int(0.7 * SR)))


def snd_power(i, rng):
    dur = 2.3
    root = (261.63, 293.66)[i]
    pad = choir([note(root, 0), note(root, 7), note(root * 2, 4), note(root * 2, 7), note(root * 4, 0)], dur, rng, "ah", 0.55, 5.0)
    # a soft lion roar tone beneath: a low harmonic voice with a slow breath of noise
    t = t_of(1.4)
    f = 95 + 30 * np.sin(math.pi * t / 1.4)
    ph = np.cumsum(f) / SR
    roar = sum(np.sin(math.tau * h * ph) / h for h in range(1, 9))
    roar = (bp(roar, 420, 2.0) + 0.25 * lp(noise(len(t), rng), 600)) * env(len(t), 0.5, 0.6) * 0.35
    bells = [(bell(note(root * 2, s), 1.2, rng, 1.0, 2.6) * 0.55, int((0.9 + k * 0.12) * SR)) for k, s in enumerate((0, 4, 7, 12))]
    return mix((pad, 0), (roar, int(0.4 * SR)), *bells)


def snd_purr(i, rng):
    dur = 1.9
    t = t_of(dur)
    rate = (24.0, 27.0)[i]
    gate = 0.5 + 0.5 * np.sign(np.sin(math.tau * rate * t)) * 0.6 + 0.2 * np.sin(math.tau * rate * t)
    base = np.zeros_like(t)
    ph = np.cumsum(58 + 5 * np.sin(math.tau * 0.7 * t)) / SR
    for h in range(1, 12):
        base += np.sin(math.tau * h * ph) / (h ** 0.9)
    body = bp(base, 190, 1.4) * 0.8 + lp(noise(len(t), rng), 420) * 0.5
    return body * np.clip(gate, 0, 1) * env(len(t), 0.3, 0.5)


def _voice(f_curve, dur, rng, formant_curve, breath=0.1, rough=0.0):
    n = int(dur * SR)
    ph = np.cumsum(f_curve) / SR
    src = np.zeros(n)
    for h in range(1, 16):
        src += np.sin(math.tau * h * ph) / (h ** 1.05)
    if rough:
        src *= 1 + rough * np.sign(np.sin(math.tau * 31 * np.arange(n) / SR))
    f1, f2 = formant_curve
    # moving formants: two band-passes crossfaded over short blocks
    out = np.zeros(n)
    block = 2048
    for a in range(0, n, block):
        seg = src[a:a + block + 512]
        k = min(len(f1) - 1, a)
        y = bp(seg, f1[k], 4.0) + 0.6 * bp(seg, f2[k], 5.0)
        out[a:a + block] += y[:min(block, n - a)]
    return out + breath * lp(noise(n, rng), 3500)


def snd_mew(i, rng):
    dur = (0.42, 0.55)[i]
    n = int(dur * SR)
    x = np.linspace(0, 1, n)
    f = (620 + 420 * np.sin(math.pi * x) ** 1.2) * (1 + 0.02 * np.sin(math.tau * 6 * x))
    f1 = 650 + 350 * x
    f2 = 2300 - 800 * x
    v = _voice(f, dur, rng, (f1, f2), 0.08)
    return v * env(n, 0.03, 0.12)


def snd_cub_roar(i, rng):
    dur = (0.95, 1.1)[i]
    n = int(dur * SR)
    x = np.linspace(0, 1, n)
    f = (170 + 70 * np.sin(math.pi * x)) * (1 + 0.04 * np.sin(math.tau * 14 * x))
    f1 = 600 + 250 * np.sin(math.pi * x)
    f2 = 1250 + 300 * x
    v = _voice(f, dur, rng, (f1, f2), 0.25, rough=0.5)
    return v * env(n, 0.06, 0.3)


def snd_cub_yawn(i, rng):
    dur = (1.8, 2.1)[i]
    n = int(dur * SR)
    x = np.linspace(0, 1, n)
    f = 210 + 120 * np.sin(math.pi * x) - 60 * x
    f1 = 500 + 400 * np.sin(math.pi * x)
    f2 = 1500 + 300 * x
    v = _voice(f, dur, rng, (f1, f2), 0.32)
    sw = np.sin(math.pi * x) ** 1.4
    return v * sw


def snd_cub_chirp(i, rng):
    dur = 0.34
    parts = []
    for k, (a, b_) in enumerate(((700, 1100), (780, 1250), (900, 1400))[i:i + 1] * 2):
        n = int(0.14 * SR)
        x = np.linspace(0, 1, n)
        f = a + (b_ - a) * x ** 0.8
        v = _voice(f, 0.14, rng, (700 + 400 * x, 2400 - 500 * x), 0.05) * env(n, 0.01, 0.05)
        parts.append((v, int(k * 0.17 * SR)))
    return mix(*parts)


def snd_cub_whimper(i, rng):
    dur = (0.65, 0.8)[i]
    n = int(dur * SR)
    x = np.linspace(0, 1, n)
    f = (760 - 280 * x) * (1 + 0.05 * np.sin(math.tau * 9 * x))
    v = _voice(f, dur, rng, (600 + 100 * x, 2100 - 400 * x), 0.12)
    return v * env(n, 0.05, 0.25) * (0.7 + 0.3 * np.sin(math.tau * 8 * x) ** 2)


MAKERS = {"link": snd_link, "unlink": snd_unlink, "beam": snd_beam, "pulse": snd_pulse, "power": snd_power, "purr": snd_purr, "cub_mew": snd_mew, "cub_roar": snd_cub_roar,
          "cub_yawn": snd_cub_yawn, "cub_chirp": snd_cub_chirp, "cub_whimper": snd_cub_whimper}


def encode(x, path, preview=None):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wavp = tmp.name
    with wave.open(wavp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wavp, "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", path], check=True)
    if preview:
        os.makedirs(preview, exist_ok=True)
        os.replace(wavp, os.path.join(preview, os.path.basename(path).replace(".ogg", ".wav")))
    else:
        os.remove(wavp)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    args = ap.parse_args()
    block = SJ.read_block()
    sfx, voice = SJ.split(block)
    sfx = {}
    lang = {}
    total = 0
    for name, (variants, subtitle) in EVENTS.items():
        names = []
        for v in range(variants):
            rng = rng_for("spirit_light.%s.%d" % (name, v))
            x = finish(MAKERS[name](v, rng))
            path = os.path.join(OUT, "%s_%d.ogg" % (name, v + 1))
            encode(x, path, args.preview)
            total += os.path.getsize(path)
            names.append({"name": "nusmp:spirit_light/%s_%d" % (name, v + 1)})
        key = "spirit_light." + name
        sfx[key] = {"subtitle": "subtitles.nusmp." + key, "sounds": names}
        lang["subtitles.nusmp." + key] = subtitle
    SJ.write_block({**sfx, **voice})
    with open(LANG, "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print("%d events, %d bytes" % (len(sfx), total))


if __name__ == "__main__":
    main()
