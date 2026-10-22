#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""The sound set of the Spirit of Darkness (docs/magic/spirit_dark.md), synthesised from nothing (numpy, then ffmpeg encodes mono Vorbis); deterministic: every file is seeded from its own name.

Eight cues, each with two or three variants, all of them short (0.3 to 2.4 s), mono, 32 kHz, peak under -2 dBFS:

    whisper   low whispers: breathy syllables through moving vowel formants, a sub drone under them, a long dark echo      (ambient, now and then, from the companion)
    chime     a dark chime: inharmonic bell partials a minor third apart, low-passed, long tail                           (the link, the appear of a Shadow Step)
    flutter   bat-wing flutter: three or four leathery flaps, band-passed noise with a fast tremolo                         (the powered form's wings)
    step      the Shadow Step whoosh: a swept noise band falling away and a sub sweep                                      (leaving)
    arrive    the arrival: a rising swept band that ends in a soft low thud and a faint chime                              (arriving)
    strike    the life steal strike: a low thump and a descending ring-modulated siphon                                    (a strike that heals)
    unlink    the spirit leaving: a descending chime and a last breath                                                     (unlink, the powered form ending)
    powered   the demon form called: a deep drone swell with a growl, a bell and whispers                                  (the powered form)

    python3 -B tools/gen_spirit_dark_sounds.py            writes assets/nusmp/sounds/spirit/dark/*.ogg and the "spirit.dark.*" entries of sounds.json
    python3 -B tools/gen_spirit_dark_sounds.py --check    verifies the files and the entries

Played by the SERVER (spiritdark/SpiritDarkSounds) to nearby players only, through a per-player gap and a server-wide per-tick budget; sounds.json gives each cue its own attenuation distance.
"""
import json
import math
import os
import subprocess
import sys
import tempfile
import wave
import zlib

import numpy as np

sys.dont_write_bytecode = True
SR = 32000
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds", "spirit", "dark")
SOUNDS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds.json")
CUES = {
    # cue: (variants, attenuation distance in blocks, volume class in dB RMS)
    "whisper": (3, 18, -24.0), "chime": (2, 24, -22.0), "flutter": (3, 20, -23.0), "step": (3, 24, -21.0),
    "arrive": (2, 24, -21.0), "strike": (2, 16, -21.0), "unlink": (2, 20, -23.0), "powered": (2, 32, -20.0),
}
VOWELS = {"u": (320, 800), "o": (450, 900), "a": (700, 1200), "e": (500, 1900), "i": (320, 2300), "er": (480, 1350)}


class Ctx:
    def __init__(self, name):
        self.rng = np.random.default_rng(zlib.crc32(name.encode()) & 0xFFFFFFFF)


def n_of(sec):
    return int(sec * SR)


def white(c, n):
    return c.rng.standard_normal(n)


def fshape(x, fn):
    n = len(x)
    size = 1 << int(math.ceil(math.log2(max(16, n * 2))))
    spec = np.fft.rfft(x, size)
    f = np.fft.rfftfreq(size, 1.0 / SR)
    return np.fft.irfft(spec * fn(f), size)[:n]


def lp(x, fc, order=2):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (f / fc) ** (2 * order)))


def hp(x, fc, order=2):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (fc / np.maximum(f, 1e-3)) ** (2 * order)))


def bp(x, f0, q):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (q * (f / f0 - f0 / np.maximum(f, 1e-3))) ** 2))


def swept_band(c, dur, f0, f1, q=2.5, shape=1.0):
    """Noise through a band whose centre glides from f0 to f1: done in short overlapping blocks (cheap and smooth enough for a whoosh)."""
    n = n_of(dur)
    out = np.zeros(n)
    blocks = max(8, int(dur * 40))
    size = n // blocks + 1
    win = np.hanning(size * 2)
    for b in range(blocks):
        t = b / max(1, blocks - 1)
        f = f0 * (f1 / f0) ** (t ** shape)
        seg = bp(white(c, size * 2), f, q) * win
        s = b * size - size // 2
        a0, a1 = max(0, s), min(n, s + size * 2)
        if a1 > a0:
            out[a0:a1] += seg[a0 - s:a1 - s]
    return out


def env(n, pts):
    t = np.array([p[0] for p in pts], float)
    v = np.array([p[1] for p in pts], float)
    return np.interp(np.linspace(0, 1, n), t, v)


def tone(freq, dur, phase=0.0):
    t = np.arange(n_of(dur)) / SR
    return np.sin(2 * math.pi * freq * t + phase)


def echo(x, delay, fb, taps, mix=1.0):
    out = x.copy()
    d = n_of(delay)
    buf = x
    for k in range(taps):
        buf = np.concatenate([np.zeros(d), buf * fb])
        if len(buf) > len(out):
            out = np.concatenate([out, np.zeros(len(buf) - len(out))])
        out[:len(buf)] += buf * mix
    return out


def reverb(x, rt=1.0, mix=0.35, dark=2800.0):
    """A cheap dark hall: four combs of different lengths, low-passed, mixed in."""
    n = len(x) + n_of(rt * 0.8)
    xp = np.concatenate([x, np.zeros(n - len(x))])
    wet = np.zeros(n)
    for d, g in ((0.029, 0.62), (0.037, 0.58), (0.047, 0.52), (0.061, 0.47)):
        wet += echo(xp, d, g, int(rt / d / 2.2))[:n]
    wet = lp(wet, dark)
    return xp + wet * mix * 0.35


def gain_db(x, db):
    return x * (10 ** (db / 20.0))


def place(buf, x, at):
    a = n_of(at)
    if a + len(x) > len(buf):
        buf = np.concatenate([buf, np.zeros(a + len(x) - len(buf))])
    buf[a:a + len(x)] += x
    return buf


def rms_db(x):
    r = math.sqrt(float(np.mean(x ** 2)) + 1e-12)
    return 20 * math.log10(r + 1e-12)


# ---------------------------------------------------------------------------------------------------------- the cues
def syllable(c, dur, vowel, level=1.0, rough=0.0):
    f1, f2 = VOWELS[vowel]
    n = n_of(dur)
    nz = white(c, n)
    a = bp(nz, f1 * c.rng.uniform(0.92, 1.08), 5.0) + 0.7 * bp(nz, f2 * c.rng.uniform(0.92, 1.08), 6.0)
    e = np.sin(np.linspace(0, math.pi, n)) ** 1.5
    breath = hp(white(c, n), 3500) * 0.12 * np.sin(np.linspace(0, math.pi, n)) ** 3
    return (a * e + breath) * level


def whisper(c):
    dur = float(c.rng.uniform(1.3, 2.0))
    buf = np.zeros(n_of(dur) + n_of(0.3))
    t = 0.05
    pool = ["u", "a", "o", "er", "e", "i"]
    while t < dur - 0.3:
        d = float(c.rng.uniform(0.16, 0.34))
        v = pool[int(c.rng.integers(0, len(pool)))]
        buf = place(buf, syllable(c, d, v, float(c.rng.uniform(0.6, 1.0))), t)
        t += d * float(c.rng.uniform(0.7, 1.1)) + float(c.rng.uniform(0.02, 0.08))
    drone = tone(float(c.rng.uniform(70, 96)), len(buf) / SR) * env(len(buf), [(0, 0), (0.25, 0.35), (0.8, 0.3), (1, 0)]) * 0.10
    buf = buf / (np.max(np.abs(buf)) + 1e-9) * 0.8 + drone
    buf = lp(buf, 3200)
    return reverb(buf, 1.1, 0.6, 2200)


def bell(c, f0, dur, bright=1.0):
    ratios = [1.0, 2.0 * 1.38, 2.0 * 2.7, 2.0 * 4.45, 2.0 * 5.7]
    out = np.zeros(n_of(dur))
    for k, r in enumerate(ratios):
        f = f0 * r * c.rng.uniform(0.997, 1.003)
        tau = dur * 0.34 / (1 + 0.8 * k)
        t = np.arange(len(out)) / SR
        out += np.sin(2 * math.pi * f * t + c.rng.uniform(0, 6.28)) * np.exp(-t / tau) * (0.7 ** k) * (bright if k else 1.0)
    out[:n_of(0.004)] *= np.linspace(0, 1, n_of(0.004))
    return out


def chime(c):
    f0 = float(c.rng.uniform(196, 262))
    buf = bell(c, f0, 1.9)
    buf = place(buf, bell(c, f0 * 1.1892, 1.7, 0.7) * 0.6, 0.11)                   # a minor third above: the dark interval
    buf = place(buf, white(c, n_of(0.05)) * 0.0, 0.0)
    buf = lp(buf, 3600)
    return reverb(buf, 1.4, 0.7, 2600)


def flutter(c):
    flaps = int(c.rng.integers(3, 5))
    dur = 0.16 * flaps + 0.3
    buf = np.zeros(n_of(dur))
    for k in range(flaps):
        d = float(c.rng.uniform(0.12, 0.17))
        n = n_of(d)
        nz = bp(white(c, n), 900 * c.rng.uniform(0.8, 1.2), 1.4) + 0.6 * bp(white(c, n), 260, 2.0)
        e = np.sin(np.linspace(0, math.pi, n)) ** 2
        trem = 0.6 + 0.4 * np.sin(2 * math.pi * 52 * np.arange(n) / SR)
        buf = place(buf, nz * e * trem * (0.9 - 0.1 * k), 0.03 + k * 0.155)
    return reverb(lp(buf, 4200), 0.6, 0.35, 3000)


def step(c):
    dur = float(c.rng.uniform(0.55, 0.75))
    n = n_of(dur)
    w = swept_band(c, dur, 3200, 260, 2.2, 0.8) * env(n, [(0, 0), (0.08, 1.0), (1, 0)])
    sub = np.sin(2 * math.pi * np.cumsum(np.linspace(220, 48, n)) / SR) * env(n, [(0, 0), (0.1, 0.8), (1, 0)]) * 0.6
    tick = np.zeros(n)
    tick[:n_of(0.02)] = hp(white(c, n_of(0.02)), 2500) * np.linspace(1, 0, n_of(0.02))
    return reverb(w / (np.max(np.abs(w)) + 1e-9) * 0.8 + sub + tick * 0.5, 0.7, 0.4, 2500)


def arrive(c):
    dur = float(c.rng.uniform(0.5, 0.65))
    n = n_of(dur)
    w = swept_band(c, dur, 300, 2600, 2.4, 1.6) * env(n, [(0, 0), (0.7, 0.9), (0.9, 0.3), (1, 0)])
    buf = w / (np.max(np.abs(w)) + 1e-9) * 0.6
    thump = tone(62, 0.45) * np.exp(-np.arange(n_of(0.45)) / SR / 0.11) * 0.9
    buf = place(buf, thump, dur * 0.72)
    buf = place(buf, bell(c, 233.0, 0.9, 0.6) * 0.18, dur * 0.7)
    return reverb(lp(buf, 3400), 0.9, 0.45, 2600)


def strike(c):
    n = n_of(0.42)
    thump = tone(float(c.rng.uniform(70, 90)), 0.42) * np.exp(-np.arange(n) / SR / 0.07) * 1.0
    nz = white(c, n)
    sweep = np.sin(2 * math.pi * np.cumsum(np.linspace(900, 140, n)) / SR)
    siphon = lp(nz * sweep * env(n, [(0, 0), (0.05, 1), (1, 0)]), 2200) * 0.5
    return reverb(thump + siphon, 0.5, 0.3, 2000)


def unlink(c):
    f0 = float(c.rng.uniform(180, 230))
    n = n_of(1.5)
    buf = np.zeros(n)
    for k, ratio in enumerate((1.0, 0.8409, 0.7071)):                               # a falling line
        buf = place(buf, bell(c, f0 * ratio, 1.0, 0.6) * (0.9 - 0.2 * k), 0.0 + 0.22 * k)
    breath = lp(white(c, n), 1400) * env(n, [(0, 0), (0.4, 0.0), (0.6, 0.35), (1, 0)]) * 0.12
    return reverb(lp(buf + breath, 3000), 1.2, 0.6, 2400)


def powered(c):
    dur = 2.0
    n = n_of(dur)
    t = np.arange(n) / SR
    f = np.linspace(44, 66, n)
    drone = np.sin(2 * math.pi * np.cumsum(f) / SR) + 0.5 * np.sin(2 * math.pi * np.cumsum(f * 2.01) / SR) + 0.25 * np.sin(2 * math.pi * np.cumsum(f * 3.02) / SR)
    drone *= env(n, [(0, 0), (0.45, 1.0), (0.8, 0.7), (1, 0)])
    growl = lp(white(c, n) * (0.5 + 0.5 * np.sin(2 * math.pi * 38 * t)), 380) * env(n, [(0, 0), (0.5, 0.9), (1, 0)]) * 1.4
    buf = drone * 0.5 + growl * 0.4
    buf = place(buf, bell(c, 207.0, 1.3, 0.7) * 0.5, 0.55)
    buf = place(buf, whisper(c)[:n_of(1.1)] * 0.45, 0.45)
    return reverb(lp(buf, 2800), 1.0, 0.5, 2200)


RECIPES = {"whisper": whisper, "chime": chime, "flutter": flutter, "step": step, "arrive": arrive, "strike": strike, "unlink": unlink, "powered": powered}


def finish(x, target_db):
    x = x - np.mean(x)
    x = x[:n_of(2.4)]
    n = len(x)
    fi, fo = n_of(0.004), min(n // 3, n_of(0.12))
    x[:fi] *= np.linspace(0, 1, fi)
    x[-fo:] *= np.linspace(1, 0, fo)
    peak = float(np.max(np.abs(x))) + 1e-9
    x = x / peak
    g = 10 ** (target_db / 20.0) / (math.sqrt(float(np.mean(x ** 2))) + 1e-9)
    y = x * g
    pk = float(np.max(np.abs(y)))
    if pk > 0.78:                                                                   # keep the peak under -2 dBFS with a soft knee
        y = np.tanh(y / 0.78) * 0.78
    return y


def encode(x, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.TemporaryDirectory() as td:
        wav = os.path.join(td, "x.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-q:a", "3", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", path], check=True)


def file_name(cue, i):
    return "%s_%d" % (cue, i + 1)


def entries():
    out = {}
    for cue, (variants, att, _) in CUES.items():
        out["spirit.dark." + cue] = {"subtitle": "subtitles.nusmp.spirit.dark." + cue,
                                     "sounds": [{"name": "nusmp:spirit/dark/" + file_name(cue, i), "attenuation_distance": att} for i in range(variants)]}
    return out


def write_json():
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        text = f.read()
    doc = json.loads(text)
    new = entries()
    if all(doc.get(k) == v for k, v in new.items()):
        return False
    # keep the file's own layout: insert (or replace) our block right after the last "zagred_phase" entry, so the other builders' appends at the end never collide with it
    for k in list(doc):
        if k.startswith("spirit.dark."):
            pass
    block = ",\n".join('  "%s": %s' % (k, json.dumps(v, indent=2).replace("\n", "\n  ")) for k, v in new.items())
    marker = '  "zagred_phase": {'
    start = text.index(marker)
    end = text.index("},\n", start) + 3
    # drop an older block of ours between the marker entry and the next non-ours entry
    rest = text[end:]
    if rest.lstrip().startswith('"spirit.dark.'):
        stop = rest.index('  "mob.') if '  "mob.' in rest else 0
        rest = rest[stop:]
    out = text[:end] + block + ",\n" + rest
    json.loads(out)
    with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
        f.write(out)
    return True


def main():
    if "--check" in sys.argv:
        doc = json.load(open(SOUNDS_JSON, encoding="utf-8"))
        bad = 0
        for k, v in entries().items():
            if doc.get(k) != v:
                print("entry differs:", k)
                bad += 1
            for s in v["sounds"]:
                p = os.path.join(OUT, s["name"].split("/")[-1] + ".ogg")
                if not os.path.exists(p):
                    print("missing", p)
                    bad += 1
        print("problems:", bad)
        sys.exit(1 if bad else 0)
    for cue, (variants, att, db) in CUES.items():
        for i in range(variants):
            name = file_name(cue, i)
            c = Ctx("spirit_dark_" + name)
            x = finish(RECIPES[cue](c), db)
            encode(x, os.path.join(OUT, name + ".ogg"))
            print("%-12s %5.2f s  rms %.1f dB  peak %.2f" % (name, len(x) / SR, rms_db(x), float(np.max(np.abs(x)))))
    print("sounds.json updated" if write_json() else "sounds.json already current")


if __name__ == "__main__":
    main()
