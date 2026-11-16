#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the sound set of the dungeon bosses and creatures (docs/dungeon_bosses_rework2.md, part 2): the cues the Floor Warden, the Labyrinth Guardians, the Direwolf, the Basilisk and the Living
Armor play on their telegraphs, impacts, phase changes and ultimates, so the moves no longer reuse vanilla anvils and bells. No recording was used: numpy writes every sample (the engines of
tools/gen_gnome_sounds.py are reused) and ffmpeg encodes mono Ogg Vorbis. Reproducible: every variant is seeded from its own name.

  python3 -B tools/gen_dungeon_boss_sounds.py             write assets/anime_multiverse/sounds/dboss/<cue>_<n>.ogg, the dboss.* entries of sounds.json and the subtitle lines of tools/lang/dungeon_bosses2.json
  python3 -B tools/gen_dungeon_boss_sounds.py --preview D also write a .wav and a spectrogram .png per file into D
  python3 -B tools/gen_dungeon_boss_sounds.py --check     verify that the files exist, are mono Ogg, short and not clipped

The cue table (ids, variants, subtitles) is the one of dungeon/rework2/DungeonBossSoundRules.java; DungeonBossSoundsTest reads both and fails when they differ.
Engines: RISE (a bank of noise bands whose centre climbs, over a rising sine), SLAM (crash + sub thud), CHAIN (irregular metal clinks), ROAR / GROWL (a vocal-fry source through low formants, roughened),
HOWL (a gliding voiced tone with vibrato), HISS (high noise with a flutter), GONG / TOLL (modal bells with beating partials), CHARGE (a climbing shimmer), BLAST (crash + a falling sweep), CLANG, SPIT, SHIMMER
(random bright blips), CRACKLE (pops), STEP (a short whoosh and a pop).
"""
from __future__ import annotations

import json
import os
import sys
import tempfile

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_gnome_sounds as G  # noqa: E402  (the engines: crash, grind, tone, fry, formants, breath, fft_filter, ...)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
SOUND_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "sounds", "dboss")
SOUNDS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "sounds.json")
LANG_OUT = os.path.join(ROOT, "tools", "lang", "dungeon_bosses2.json")
SR = G.SR
MIN_S, MAX_S = 0.3, 2.35         # under the 2.4 s that sounds.json would stream

# cue -> (variants, subtitle, loudness dBFS rms of the active part)
CUES = {
    "rise": (3, "A boss gathers its strength", -22.0),
    "slam": (3, "A boss slams down", -16.0),
    "chain": (3, "Chains rattle", -21.0),
    "roar": (3, "A boss roars", -17.0),
    "howl": (2, "A beast howls", -19.0),
    "hiss": (3, "A serpent hisses", -23.0),
    "gong": (2, "A deep gong tolls", -18.0),
    "charge": (2, "Power gathers", -19.0),
    "blast": (2, "A great blast", -15.0),
    "toll": (2, "A bell tolls", -19.0),
    "clang": (3, "Metal clangs", -19.0),
    "spit": (2, "Something spits venom", -21.0),
    "shimmer": (3, "Magic shimmers", -24.0),
    "crackle": (3, "Energy crackles", -22.0),
    "step": (2, "A boss vanishes", -20.0),
    "growl": (3, "A beast growls", -20.0),
}


def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def norm(x):
    return x / (np.max(np.abs(x)) + 1e-9)


def band(rng, n, c, width=1.4):
    return G.fft_filter(rng.standard_normal(n), lo=c / width, hi=c * width, slope=3)


def modal(t, base, ratios, decay, amps=None, beat=0.0):
    out = np.zeros(len(t))
    for i, r in enumerate(ratios):
        a = amps[i] if amps else 1.0 / (1 + i * 0.6)
        d = decay / (1 + 0.35 * i)
        out += a * np.sin(2 * np.pi * base * r * t) * np.exp(-t * d)
        if beat:
            out += 0.6 * a * np.sin(2 * np.pi * (base * r + beat * (1 + 0.3 * i)) * t) * np.exp(-t * d)
    return out


# ---------------------------------------------------------------------------------------------- engines
def rise(rng, dur, lo=160.0, hi=4200.0):
    n = int(dur * SR)
    t = t_axis(dur)
    out = np.zeros(n)
    centres = np.geomspace(lo, hi, 9)
    for k, c in enumerate(centres):
        peak = dur * (0.12 + 0.72 * k / (len(centres) - 1))
        env = np.exp(-0.5 * ((t - peak) / (dur * 0.17)) ** 2)
        out += band(rng, n, c) * env * (0.7 + 0.05 * k)
    f = lo * 1.5 * (hi / lo / 1.5) ** (t / dur)
    out += np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.10 * (t / dur) ** 1.4
    return norm(out) * G.swell(n, 0.82, 1.4)


def slam(rng, dur, power=1.0):
    return G.mix(G.crash(rng, dur, 18, power), G.tone(rng, dur, attack=0.01, tail=1.1, f0=rng.uniform(48, 64), level=0.9), G.rumble(rng, dur, 0.4, peak_at=0.04))


def chain(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    out = np.zeros(n)
    st = 0.02
    while st < dur - 0.12:
        base = rng.uniform(1500, 4800)
        i0 = int(st * SR)
        tt = t[: n - i0]
        m = np.zeros(len(tt))
        for ratio, a in ((1.0, 1.0), (1.48, 0.6), (2.31, 0.4), (3.17, 0.25)):
            m += a * np.sin(2 * np.pi * base * ratio * tt) * np.exp(-tt * rng.uniform(55, 110) * ratio ** 0.5)
        out[i0:] += m * rng.uniform(0.35, 1.0) * (0.6 + 0.4 * np.sin(np.pi * st / dur))
        st += rng.exponential(0.065) + 0.025
    out += G.fft_filter(rng.standard_normal(n), lo=120, hi=700, slope=3) * 0.1 * G.swell(n, 0.5)
    return out


def voice(rng, dur, f0, f_end, formant, rough=0.5, irregular=0.3, level=1.0):
    n = int(dur * SR)
    src = G.fry(rng, dur, f0, f_end, irregular=irregular)
    src = src + 0.5 * G.fry(rng, dur, f0 * 1.5, f_end * 1.5, irregular=irregular + 0.1) + G.breath(rng, dur, 200, 2600) * rough * 0.5
    out = G.formants(src, formant)
    rum = G.fft_filter(rng.standard_normal(n), hi=240, slope=3) * 0.15
    out = norm(out + rum)
    return np.tanh(out * (1.5 + rough)) * level


ROAR_FORMS = ((300, 90, 1.0), (850, 160, 0.9), (1700, 260, 0.35), (2800, 380, 0.12))
GROWL_FORMS = ((420, 110, 1.0), (900, 180, 0.7), (1500, 240, 0.25))


def roar(rng, dur):
    n = int(dur * SR)
    x = voice(rng, dur, rng.uniform(78, 98), rng.uniform(46, 58), ROAR_FORMS, rough=0.55)
    return x * (G.swell(n, 0.28, 0.9) ** 0.8)


def growl(rng, dur):
    n = int(dur * SR)
    x = voice(rng, dur, rng.uniform(70, 90), rng.uniform(48, 62), GROWL_FORMS, rough=0.5, irregular=0.4)
    am = 0.75 + 0.25 * np.sin(2 * np.pi * rng.uniform(14, 22) * t_axis(dur))
    return x * am * G.swell(n, 0.35, 1.1)


def howl(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    k = t / dur
    f = 190 + 150 * np.sin(np.pi * np.minimum(1, k * 1.15) * 0.9) ** 1.2 - 40 * k ** 3
    f = f * (1 + 0.012 * np.sin(2 * np.pi * (5.0 + 1.5 * k) * t + rng.uniform(0, 6)))
    ph = np.cumsum(f) / SR
    src = np.sin(2 * np.pi * ph) + 0.55 * np.sin(4 * np.pi * ph) + 0.3 * np.sin(6 * np.pi * ph) + 0.15 * np.sin(8 * np.pi * ph)
    src = src + G.breath(rng, dur, 300, 3000) * 0.18
    out = G.formants(src, ((380, 90, 1.0), (760, 140, 0.55), (2400, 300, 0.1)))
    return norm(out) * G.swell(n, 0.45, 1.0)


def hiss(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    x = G.fft_filter(rng.standard_normal(n), lo=2400, hi=9000, slope=3)
    x = x * (0.7 + 0.3 * np.sin(2 * np.pi * rng.uniform(18, 30) * t + rng.uniform(0, 6)))
    low = G.fft_filter(rng.standard_normal(n), lo=500, hi=1800, slope=3) * 0.25
    return norm(x + low) * G.swell(n, 0.3, 1.2)


def gong(rng, dur, base):
    t = t_axis(dur)
    x = modal(t, base, (1.0, 1.59, 2.14, 2.30, 2.65, 3.16, 4.07, 5.43), 2.4, beat=1.1)
    x += G.fft_filter(rng.standard_normal(len(t)), lo=200, hi=3500, slope=2) * np.exp(-t * 28) * 0.7
    x *= np.minimum(1.0, t / 0.012)
    return x


def toll(rng, dur, base):
    t = t_axis(dur)
    x = modal(t, base, (0.5, 1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.2), 1.9, amps=[0.6, 1.0, 0.7, 0.45, 0.5, 0.25, 0.18, 0.12], beat=0.9)
    x += G.fft_filter(rng.standard_normal(len(t)), lo=600, hi=5000, slope=2) * np.exp(-t * 45) * 0.45
    x *= np.minimum(1.0, t / 0.006)
    return x


def charge(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    k = t / dur
    f = 70 * (6.0 ** k)
    out = np.zeros(n)
    for r, a in ((1.0, 1.0), (2.0, 0.5), (3.01, 0.3), (5.02, 0.15)):
        out += a * np.sin(2 * np.pi * np.cumsum(f * r) / SR)
    out *= 0.5 + 0.5 * np.sin(2 * np.pi * (4 + 14 * k) * t) ** 2                          # a flutter that speeds up
    out += rise(rng, dur, 220, 5200) * 0.8 + G.rumble(rng, dur, 0.5, peak_at=0.9) * 0.5
    return norm(out) * (k ** 1.1)


def blast(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    sweep = np.sin(2 * np.pi * np.cumsum(2400 * np.exp(-t * 3.2) + 90) / SR) * np.exp(-t * 2.4) * 0.5
    return G.mix(G.crash(rng, dur, 26, 1.2), sweep, G.rumble(rng, dur, 0.9, peak_at=0.05), G.tone(rng, dur, attack=0.01, tail=1.3, f0=rng.uniform(40, 52), level=1.0))


def clang(rng, dur):
    t = t_axis(dur)
    base = rng.uniform(380, 700)
    x = modal(t, base, (1.0, 1.47, 2.09, 2.77, 3.52, 4.6), 9.0, beat=3.0)
    x += G.fft_filter(rng.standard_normal(len(t)), lo=900, hi=7000, slope=2) * np.exp(-t * 90) * 0.8
    return x * np.minimum(1.0, t / 0.002)


def spit(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    burst = G.fft_filter(rng.standard_normal(n), lo=900, hi=4800, slope=2) * np.exp(-t * 16) * (0.6 + 0.4 * np.sin(2 * np.pi * 70 * t))
    blip = np.sin(2 * np.pi * np.cumsum(520 * np.exp(-t * 9) + 140) / SR) * np.exp(-t * 11) * 0.8
    wet = G.fft_filter(rng.standard_normal(n), lo=300, hi=1200, slope=2) * np.exp(-((t - dur * 0.55) / 0.07) ** 2) * 0.7
    return burst + blip + wet


def shimmer(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    out = np.zeros(n)
    for _ in range(int(dur * 38)):
        st = rng.uniform(0, dur * 0.85)
        i0 = int(st * SR)
        f = rng.uniform(1800, 6500)
        tt = t[: n - i0]
        out[i0:] += np.sin(2 * np.pi * f * tt) * np.exp(-tt * rng.uniform(14, 40)) * rng.uniform(0.2, 1.0) * np.sin(np.pi * st / dur)
    pad = np.zeros(n)
    for f in (rng.uniform(520, 640), rng.uniform(780, 900), rng.uniform(1180, 1320)):
        pad += np.sin(2 * np.pi * f * t + 0.5 * np.sin(2 * np.pi * 5 * t))
    return norm(out) * 0.8 + norm(pad) * 0.18 * G.swell(n, 0.4, 1.3)


def crackle(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    out = np.zeros(n)
    for _ in range(int(dur * 55)):
        st = rng.uniform(0, dur * 0.9)
        i0 = int(st * SR)
        ln = int(rng.uniform(0.002, 0.014) * SR)
        out[i0:i0 + ln] += rng.standard_normal(min(ln, n - i0)) * np.exp(-np.arange(min(ln, n - i0)) / (ln * 0.3)) * rng.uniform(0.2, 1.0)
    out = G.fft_filter(out, lo=500, hi=9000, slope=2)
    hum = np.sin(2 * np.pi * 110 * t) * 0.12 + G.fft_filter(rng.standard_normal(n), lo=100, hi=900, slope=3) * 0.25
    return norm(out) * 0.8 + hum * G.swell(n, 0.5)


def step(rng, dur):
    n = int(dur * SR)
    t = t_axis(dur)
    x = rise(rng, dur * 0.55, 400, 5000)
    pop = np.sin(2 * np.pi * np.cumsum(260 * np.exp(-t * 18) + 70) / SR) * np.exp(-t * 14) * 0.9
    out = np.zeros(n)
    out[: len(x)] += x * 0.6
    cut = int(dur * 0.5 * SR)
    out[cut:] += (pop[: n - cut] if len(pop) >= n - cut else 0) + G.fft_filter(rng.standard_normal(n - cut), lo=150, hi=1500, slope=3) * np.exp(-t[: n - cut] * 12) * 0.5
    return out


def make(cue, k):
    name = "%s_%d" % (cue, k)
    rng = G.rng_for("dboss." + name)
    v = k - 1
    if cue == "rise":
        dur = 1.0 + 0.15 * v
        x = rise(rng, dur, 160 + 40 * v)
    elif cue == "slam":
        dur = 1.5 + 0.1 * v
        x = slam(rng, dur, 1.0 + 0.1 * v)
    elif cue == "chain":
        dur = 0.9 + 0.12 * v
        x = chain(rng, dur)
    elif cue == "roar":
        dur = 1.7 + 0.2 * v
        x = roar(rng, dur)
    elif cue == "howl":
        dur = 2.1 + 0.1 * v
        x = howl(rng, dur)
    elif cue == "hiss":
        dur = 0.9 + 0.15 * v
        x = hiss(rng, dur)
    elif cue == "gong":
        dur = 2.3
        x = gong(rng, dur, 82 + 12 * v)
    elif cue == "charge":
        dur = 2.1 + 0.1 * v
        x = charge(rng, dur)
    elif cue == "blast":
        dur = 1.7 + 0.15 * v
        x = blast(rng, dur)
    elif cue == "toll":
        dur = 2.1
        x = toll(rng, dur, 150 + 20 * v)
    elif cue == "clang":
        dur = 0.7 + 0.1 * v
        x = clang(rng, dur)
    elif cue == "spit":
        dur = 0.5 + 0.05 * v
        x = spit(rng, dur)
    elif cue == "shimmer":
        dur = 1.2 + 0.15 * v
        x = shimmer(rng, dur)
    elif cue == "crackle":
        dur = 1.0 + 0.12 * v
        x = crackle(rng, dur)
    elif cue == "step":
        dur = 0.7 + 0.1 * v
        x = step(rng, dur)
    else:                                                                                  # growl
        dur = 0.95 + 0.12 * v
        x = growl(rng, dur)
    x = G.fades(np.array(x, dtype=float), 0.003, 0.06 if dur > 1.0 else 0.03)
    return G.shape_lufs(x, CUES[cue][2]), dur


# ---------------------------------------------------------------------------------------------- output
def entries_text():
    lines = []
    for cue, (nvar, _sub, _) in CUES.items():
        sounds = ", ".join('{ "name": "anime_multiverse:dboss/%s_%d", "stream": false }' % (cue, k) for k in range(1, nvar + 1))
        lines.append('  "dboss.%s": {\n    "subtitle": "subtitles.anime_multiverse.dboss.%s",\n    "sounds": [ %s ]\n  }' % (cue, cue, sounds))
    return ",\n".join(lines)


def update_sounds_json():
    """Appends (or replaces in place) the block of the dboss.* entries; every other entry stays where it is."""
    import re
    raw = open(SOUNDS_JSON, newline="").read()
    pat = re.compile(r',\n  "dboss\.[a-z_]+": \{\n    "subtitle".*?\n  \}', re.S)
    raw = pat.sub("", raw)
    end = raw.rstrip().rfind("}")
    out = raw[:end].rstrip() + ",\n" + entries_text() + "\n}\n"
    json.loads(out)
    open(SOUNDS_JSON, "w", newline="").write(out)
    lang = json.load(open(LANG_OUT)) if os.path.exists(LANG_OUT) else {}
    lang.update({"subtitles.anime_multiverse.dboss.%s" % c: s for c, (_, s, _) in CUES.items()})
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
            pk, dur = G.decoded_peak(ogg)
            ok = MIN_S <= dur <= MAX_S and 0.05 < pk <= 0.97 and os.path.getsize(ogg) < 200 * 1024
            bad += 0 if ok else 1
            print("%-12s %.2f s  peak %.3f  %6d bytes %s" % ("%s_%d" % (cue, k), dur, pk, os.path.getsize(ogg), "ok" if ok else "BAD"))
    return bad


def main():
    if "--check" in sys.argv:
        sys.exit(1 if check() else 0)
    prev = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    if prev:
        os.makedirs(prev, exist_ok=True)
    os.makedirs(SOUND_DIR, exist_ok=True)
    only = [a for a in sys.argv[1:] if not a.startswith("--") and a != prev]
    with tempfile.TemporaryDirectory() as tmp:
        for cue, (nvar, _, _) in CUES.items():
            if only and cue not in only:
                continue
            for k in range(1, nvar + 1):
                x, _dur = make(cue, k)
                wav = os.path.join(tmp, "x.wav")
                ogg = os.path.join(SOUND_DIR, "%s_%d.ogg" % (cue, k))
                G.write_wav(wav, x)
                G.encode(wav, ogg)
                pk, d2 = G.decoded_peak(ogg)
                if pk > 0.97:
                    G.write_wav(wav, x * 0.9)
                    G.encode(wav, ogg)
                    pk, d2 = G.decoded_peak(ogg)
                print("%-10s %.2f s  peak %.3f  %d bytes" % ("%s_%d" % (cue, k), d2, pk, os.path.getsize(ogg)))
                if prev:
                    G.write_wav(os.path.join(prev, "%s_%d.wav" % (cue, k)), x)
                    G.spectrogram(x, os.path.join(prev, "%s_%d.png" % (cue, k)))
    update_sounds_json()


if __name__ == "__main__":
    main()
