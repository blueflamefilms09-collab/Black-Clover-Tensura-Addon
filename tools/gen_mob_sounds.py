#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the bespoke mob voices of the dungeon (round 1.99.0, "the bespoke audio engine").

The owner's rule: mobs must not use the generic vanilla zombie / skeleton sounds. Every family of dungeon mob (goblins, orcs, kobolds, skeletons, ghouls, liches, wraiths,
living armour, golems, dragons, drakes, slimes, demons, imps, harpies, trolls, ogres, beasts, insects, elementals, drowned folk, cultists, vampires, celestials and the
hub's people) has eight cues (ambient, aggro, attack, hurt, death, step, cast, taunt) and each cue several variants. This script writes every .ogg of them from nothing:
numpy only (scipy is not needed), then ffmpeg encodes mono Vorbis. It is reproducible: every variant is seeded from its own name, so a re-run writes the same bytes
of audio, and the Ogg files are bit-exact for one ffmpeg build (the encoder may differ in its last bits across builds).

  python3 tools/gen_mob_sounds.py                 write every file, sounds.json (the mob entries only; other entries are kept) and tools/lang/mob_audio.json
  python3 tools/gen_mob_sounds.py --only goblin   one family (or goblin.taunt for one cue)
  python3 tools/gen_mob_sounds.py --preview DIR   also write a .wav and a spectrogram .png per file into DIR (to look at them without the game)
  python3 tools/gen_mob_sounds.py --check         verify that every file sounds.json names exists, is a valid Ogg and short
  python3 tools/gen_mob_sounds.py --manifest-only rewrite sounds.json and the lang fragment from the files already there

The voice of each family is built from a few engines: a source-filter VOICE (a glottal pulse train through moving vowel formants, with vibrato, breath, roughness and a
sub-harmonic), the goblin and imp LAUGH (bursts with a pitch glide and a fast vibrato), the lich WHISPER (noise through moving formants, a low drone and a long echo),
the ROAR and WAIL, resonator banks for METAL, BONE and STONE, SLIME squelches, insect CHITTER, elemental CRACKLE, and footsteps of eleven kinds.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import struct
import subprocess
import sys
import tempfile
import wave
import zlib

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
SOUND_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp', 'sounds', 'mob')
SOUNDS_JSON = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp', 'sounds.json')
LANG_OUT = os.path.join(ROOT, 'tools', 'lang', 'mob_audio.json')

CUES = ['ambient', 'aggro', 'attack', 'hurt', 'death', 'step', 'cast', 'taunt']
VARIANTS = {'ambient': 3, 'aggro': 2, 'attack': 3, 'hurt': 3, 'death': 2, 'step': 3, 'cast': 2, 'taunt': 3}
# loudness target (dBFS, RMS of the active part) per cue: voices louder than steps and idle sounds
LOUDNESS = {'ambient': -25.0, 'aggro': -19.0, 'attack': -19.0, 'hurt': -20.0, 'death': -21.0, 'step': -26.0, 'cast': -22.0, 'taunt': -21.0}
PEAK_LIMIT = 0.89
MAX_SECONDS = 3.0
# families whose loud cues carry further than the default 16 blocks (attenuation_distance in sounds.json)
ATTENUATION = {'dragon': 40, 'golem': 24, 'demon': 24, 'celestial': 24, 'lich': 20, 'ogre': 20, 'troll': 20, 'elemental': 20}
LOUD_CUES = ('aggro', 'attack', 'death', 'cast', 'taunt')
STREAM_OVER = 2.4           # seconds: a file at least this long is marked "stream" in sounds.json (the game has few streaming channels, so only the long ones)

# family id -> (label, sample rate, subtitle verbs in CUES order)
FAMILIES = {
    'goblin': ('Goblin', 44100, ['giggles', 'shrieks', 'snarls', 'squeals', 'dies', 'scurries', 'chants', 'cackles']),
    'orc': ('Orc', 32000, ['grunts', 'bellows', 'roars', 'grunts in pain', 'falls', 'stomps', 'chants', 'laughs']),
    'kobold': ('Kobold', 44100, ['chatters', 'yaps', 'snaps', 'yelps', 'dies', 'scampers', 'chants', 'yap-laughs']),
    'skeleton': ('Skeleton', 44100, ['rattles', 'clacks its jaw', 'strikes', 'cracks', 'collapses', 'clacks', 'rattles a spell', 'clatters']),
    'ghoul': ('Ghoul', 32000, ['groans', 'moans', 'claws', 'gurgles in pain', 'dies', 'shuffles', 'rasps a spell', 'moans hungrily']),
    'lich': ('Lich', 44100, ['whispers', 'hisses', 'strikes coldly', 'gasps', 'whispers a last curse', 'drifts', 'whispers a spell', 'whispers a laugh']),
    'wraith': ('Wraith', 44100, ['moans', 'wails', 'slashes', 'shrieks', 'fades', 'drifts', 'sings a spell', 'keens']),
    'armor': ('Living armour', 32000, ['creaks', 'clanks angrily', 'swings', 'clangs', 'collapses', 'clanks', 'rings', 'scrapes']),
    'golem': ('Golem', 32000, ['grinds', 'rumbles', 'slams', 'cracks', 'crumbles', 'thuds', 'hums', 'grinds a laugh']),
    'dragon': ('Dragon', 32000, ['rumbles', 'roars', 'bites', 'bellows in pain', 'dies', 'thuds', 'breathes in', 'chuckles darkly']),
    'drake': ('Drake', 44100, ['hisses', 'screeches', 'snaps', 'squawks', 'dies', 'skitters', 'inhales', 'cackles']),
    'slime': ('Slime', 32000, ['bubbles', 'gurgles', 'slaps', 'squeals', 'splatters', 'squelches', 'burbles', 'giggles']),
    'demon': ('Demon', 32000, ['growls', 'snarls', 'rends', 'roars in pain', 'dies', 'stomps', 'chants darkly', 'laughs darkly']),
    'imp': ('Imp', 44100, ['chitters', 'screeches', 'snaps', 'squeals', 'pops', 'patters', 'gibbers', 'cackles']),
    'harpy': ('Harpy', 44100, ['warbles', 'screeches', 'rakes', 'squawks', 'falls', 'flaps', 'keens', 'shrieks with laughter']),
    'troll': ('Troll', 32000, ['grumbles', 'roars', 'smashes', 'grunts', 'topples', 'stomps', 'mutters', 'guffaws']),
    'ogre': ('Ogre', 32000, ['mumbles', 'bellows', 'pounds', 'bellows in pain', 'falls', 'stomps', 'chants', 'laughs heartily']),
    'beast': ('Beast', 32000, ['growls', 'snarls', 'bites', 'yelps', 'whimpers', 'pads', 'howls', 'howls']),
    'insect': ('Insect', 44100, ['chitters', 'hisses', 'snaps', 'squeals', 'crunches', 'skitters', 'buzzes', 'clacks']),
    'elemental': ('Elemental', 32000, ['crackles', 'flares', 'lashes', 'sputters', 'fizzles out', 'thumps', 'surges', 'crackles with laughter']),
    'aquatic': ('Drowned', 32000, ['gurgles', 'croaks', 'lunges', 'gurgles in pain', 'sinks', 'squelches', 'bubbles a spell', 'gurgles with laughter']),
    'cultist': ('Cultist', 32000, ['murmurs', 'shouts', 'grunts', 'gasps', 'chokes', 'shuffles', 'chants', 'laughs madly']),
    'vampire': ('Vampire', 44100, ['murmurs', 'hisses', 'strikes', 'hisses in pain', 'turns to dust', 'steps', 'whispers a spell', 'chuckles']),
    'celestial': ('Celestial', 44100, ['sings', 'calls', 'strikes', 'rings', 'dissolves', 'glides', 'sings a spell', 'laughs like bells']),
    'npc': ('Villager', 32000, ['hums', 'calls out', 'grunts', 'cries out', 'sighs', 'walks', 'hums a spell', 'chuckles']),
}

# ======================================================================================================================================================================
# DSP kit
# ======================================================================================================================================================================


class Ctx:
    """The sample rate and the random generator of one rendered variant."""

    def __init__(self, sr, rng):
        self.sr = sr
        self.rng = rng

    def n(self, seconds):
        return max(16, int(round(seconds * self.sr)))


def curve(pts, n):
    """A piecewise-linear contour from [(t0, v0), (t1, v1), ...] with t in 0..1, sampled at n points."""
    t = np.array([p[0] for p in pts], dtype=float)
    v = np.array([p[1] for p in pts], dtype=float)
    return np.interp(np.linspace(0.0, 1.0, n), t, v)


def white(c, n):
    return c.rng.standard_normal(n)


def fshape(x, fn, sr):
    """Zero-phase filter by a magnitude function of the frequency (the signal is zero padded, so nothing wraps round)."""
    n = len(x)
    size = 1 << int(math.ceil(math.log2(max(16, n * 2))))
    spec = np.fft.rfft(x, size)
    f = np.fft.rfftfreq(size, 1.0 / sr)
    spec = spec * fn(f)
    return np.fft.irfft(spec, size)[:n]


def lp(x, fc, sr, order=2):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (f / fc) ** (2 * order)), sr)


def hp(x, fc, sr, order=2):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (fc / np.maximum(f, 1e-3)) ** (2 * order)), sr)


def bp(x, f0, q, sr):
    return fshape(x, lambda f: 1.0 / np.sqrt(1.0 + (q * (f / f0 - f0 / np.maximum(f, 1e-3))) ** 2), sr)


def pink(c, n):
    return fshape(white(c, n), lambda f: 1.0 / np.sqrt(np.maximum(f, 30.0)), c.sr)


def brown(c, n):
    return fshape(white(c, n), lambda f: 1.0 / np.maximum(f, 30.0), c.sr)


def wander(c, n, rate_hz, depth=1.0):
    """Smooth random numbers in [-depth, depth]: random points joined by straight lines, rate_hz of them a second."""
    k = max(2, int(n / c.sr * rate_hz) + 2)
    pts = c.rng.uniform(-1.0, 1.0, k)
    return np.interp(np.linspace(0, k - 1, n), np.arange(k), pts) * depth


def env_pts(n, pts):
    return curve(pts, n)


def adsr(c, n, a, d, s, r):
    """Attack, decay to sustain level s, release at the end (seconds)."""
    t = np.arange(n) / c.sr
    total = n / c.sr
    e = np.ones(n)
    a = max(a, 1e-4)
    e = np.where(t < a, t / a, e)
    e = np.where((t >= a) & (t < a + d), 1.0 - (1.0 - s) * ((t - a) / max(d, 1e-4)), e)
    e = np.where(t >= a + d, s, e)
    rel = np.clip((total - t) / max(r, 1e-4), 0.0, 1.0)
    return e * rel


def expdecay(c, n, tau):
    return np.exp(-(np.arange(n) / c.sr) / max(tau, 1e-4))


def sat(x, k):
    """Soft saturation."""
    return np.tanh(x * k) / np.tanh(k) if k > 0 else x


def norm(x, peak=1.0):
    m = float(np.max(np.abs(x))) if len(x) else 0.0
    return x * (peak / m) if m > 1e-9 else x


def place(buf, x, start, gain=1.0):
    """Adds x into buf at sample offset start (the buffer grows if needed); returns the buffer."""
    start = int(max(0, start))
    end = start + len(x)
    if end > len(buf):
        buf = np.concatenate([buf, np.zeros(end - len(buf))])
    buf[start:end] += x * gain
    return buf


def mixdown(parts, length=None):
    n = max(len(p) for p in parts) if length is None else length
    out = np.zeros(n)
    for p in parts:
        out[:min(n, len(p))] += p[:n]
    return out


def resample(x, ratio):
    """Speeds the sound up by ratio (pitch and speed together)."""
    if abs(ratio - 1.0) < 1e-6:
        return x
    idx = np.arange(0.0, len(x) - 1, ratio)
    return np.interp(idx, np.arange(len(x)), x)


def echo(x, sr, delay, fb, taps, mix=1.0):
    y = x.copy()
    d = int(delay * sr)
    if d <= 0:
        return y
    total = len(x) + d * taps
    y = np.concatenate([y, np.zeros(total - len(y))])
    for k in range(1, taps + 1):
        y[k * d:k * d + len(x)] += x * (fb ** k) * mix
    return y


def reverb(x, sr, rt60=1.2, mix=0.3, predelay=0.0, tail=None):
    """A small Schroeder reverb (four combs, two all-passes) done with whole-block operations; the tail is added after the sound."""
    if mix <= 0:
        return x
    tail_s = min(rt60 * 0.8, 1.2) if tail is None else tail
    n = len(x) + int(tail_s * sr) + int(predelay * sr)
    dry = np.concatenate([np.zeros(int(predelay * sr)), x, np.zeros(n - len(x) - int(predelay * sr))])
    wet = np.zeros(n)
    for d_ms, g_scale in ((29.7, 1.0), (37.1, 0.97), (41.1, 0.94), (43.7, 0.91)):
        d = int(d_ms * 0.001 * sr)
        g = 0.001 ** (d_ms * 0.001 / max(rt60, 0.05)) * g_scale ** 0.1
        y = dry.copy()
        for k in range(1, n // d + 1):
            lo, hi = k * d, min(n, (k + 1) * d)
            if lo >= n:
                break
            y[lo:hi] += g * y[lo - d:lo - d + (hi - lo)]
        wet += y
    wet /= 4.0
    for d_ms, g in ((5.0, 0.7), (1.7, 0.7)):
        d = int(d_ms * 0.001 * sr)
        y = np.zeros(n)
        y[:d] = -g * wet[:d]
        for k in range(1, n // d + 1):
            lo, hi = k * d, min(n, (k + 1) * d)
            if lo >= n:
                break
            m = hi - lo
            y[lo:hi] = -g * wet[lo:hi] + wet[lo - d:lo - d + m] + g * y[lo - d:lo - d + m]
        wet = y
    wet = lp(wet, 6500.0, sr, 1)
    return dry * (1.0 - 0.35 * mix) + wet * mix * 1.6


def chorus(x, sr, depth=0.004, rate=0.7, mix=0.5, voices=2):
    n = len(x)
    out = x.copy()
    t = np.arange(n) / sr
    for v in range(voices):
        d = (0.012 + 0.006 * v) * sr + depth * sr * np.sin(2 * np.pi * (rate + 0.13 * v) * t + v * 1.7)
        idx = np.clip(np.arange(n) - d, 0, n - 1)
        out += mix * np.interp(idx, np.arange(n), x)
    return out / (1.0 + mix * voices * 0.7)


def ringmod(x, sr, f, mix=0.5):
    t = np.arange(len(x)) / sr
    return x * (1.0 - mix) + x * np.sin(2 * np.pi * f * t) * mix


def stft_apply(x, sr, resp_fn, frame=1024, hop=256):
    """Filters x with a magnitude response that changes with time: resp_fn(frame_index, time_0_to_1, frequencies) -> magnitudes."""
    n = len(x)
    win = np.hanning(frame)
    pad = frame
    xp = np.concatenate([np.zeros(pad), x, np.zeros(pad + frame)])
    out = np.zeros(len(xp))
    wsum = np.zeros(len(xp))
    freqs = np.fft.rfftfreq(frame, 1.0 / sr)
    nframes = (len(xp) - frame) // hop + 1
    for k in range(nframes):
        s = k * hop
        seg = xp[s:s + frame] * win
        spec = np.fft.rfft(seg)
        spec *= resp_fn(k, (s - pad + frame / 2.0) / max(1, n), freqs)
        out[s:s + frame] += np.fft.irfft(spec, frame) * win
        wsum[s:s + frame] += win * win
    out = out / np.maximum(wsum, 1e-3)
    return out[pad:pad + n]


# ---------------------------------------------------------------------------------------------------------------------------------------------------------------------
# the voice: a glottal pulse train through moving vowel formants
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------------
VOWELS = {
    'a': [(730, 85), (1090, 110), (2440, 160), (3400, 220)],
    'e': [(530, 75), (1840, 110), (2480, 160), (3400, 220)],
    'i': [(270, 65), (2290, 110), (3010, 160), (3600, 220)],
    'o': [(570, 80), (840, 95), (2410, 160), (3400, 220)],
    'u': [(300, 70), (870, 95), (2240, 160), (3400, 220)],
    'ae': [(660, 85), (1720, 110), (2410, 160), (3400, 220)],
    'uh': [(520, 85), (1190, 110), (2390, 160), (3400, 220)],
    'er': [(490, 90), (1350, 120), (1690, 150), (3200, 220)],
    'oo': [(380, 70), (760, 90), (2200, 160), (3400, 220)],
}
FORMANT_GAIN = [1.0, 0.75, 0.42, 0.2]


def glottal(c, f0, tilt=1.5, jitter=0.0, sub=0.0):
    """A band-limited harmonic pulse source following the pitch contour f0 (Hz per sample); sub adds a sub-harmonic (a growl); jitter wobbles the period."""
    n = len(f0)
    if jitter > 0:
        f0 = f0 * (1.0 + wander(c, n, 38.0, jitter))
    ph = 2 * np.pi * np.cumsum(f0) / c.sr
    fmax = float(np.max(f0)) if n else 100.0
    kmax = int(max(3, min(70, (c.sr * 0.47) / max(fmax, 30.0))))
    out = np.zeros(n)
    for k in range(1, kmax + 1):
        mask = (k * f0 < c.sr * 0.46).astype(float)
        out += (k ** -tilt) * np.sin(k * ph + 0.3 * k) * mask
    if sub > 0:
        ph2 = ph * 0.5
        sub_sig = np.zeros(n)
        for k in range(1, max(2, kmax // 2)):
            sub_sig += (k ** -(tilt + 0.3)) * np.sin(k * ph2) * (k * f0 * 0.5 < c.sr * 0.46)
        out += sub * sub_sig
    return out


def _formant_set(vowel, tract, bwmul):
    if isinstance(vowel, str):
        base = VOWELS[vowel]
    else:
        base = vowel
    return np.array([[f * tract, bw * bwmul * (0.6 + 0.4 * tract)] for f, bw in base])


def vox(c, dur, f0pts, vseq, tract=1.0, tilt=1.5, breath=0.1, rough=0.0, rough_hz=70.0, vib=(5.5, 0.02), jitter=0.012, sub=0.0, drive=0.0,
        amp=None, bwmul=1.0, voiced=1.0, band=None):
    """One vocal gesture. f0pts: pitch contour [(t, Hz)]; vseq: vowels over time [(t, name)]; tract: <1 deep big throat, >1 small throat; breath: aspiration noise;
    rough: depth of the amplitude roughness at rough_hz (a growl); sub: sub-harmonic; voiced: 0 gives a whisper (noise only)."""
    n = c.n(dur)
    t = np.arange(n) / c.sr
    f0 = curve(f0pts, n)
    if vib and vib[1] > 0:
        f0 = f0 * (1.0 + vib[1] * np.sin(2 * np.pi * vib[0] * t + c.rng.uniform(0, 6.28)))
    src = glottal(c, f0, tilt=tilt, jitter=jitter, sub=sub) if voiced > 0 else np.zeros(n)
    if voiced > 0:
        src = src / (np.max(np.abs(src)) + 1e-9) * voiced
    if rough > 0:
        src = src * (1.0 - rough * (0.5 + 0.5 * np.sin(2 * np.pi * rough_hz * t + c.rng.uniform(0, 6.28))))
    noise = white(c, n)
    if breath > 0:
        pulse_gate = 0.55 + 0.45 * (src / (np.max(np.abs(src)) + 1e-9)) if voiced > 0 else 1.0
        src = src + breath * noise * pulse_gate * (0.9 if voiced > 0 else 1.0)
    sets = [(tt, _formant_set(v, tract, bwmul)) for tt, v in vseq]
    times = np.array([s[0] for s in sets])

    def resp(k, tn, freqs):
        tn = min(max(tn, 0.0), 1.0)
        j = int(np.searchsorted(times, tn))
        if j <= 0:
            fs = sets[0][1]
        elif j >= len(sets):
            fs = sets[-1][1]
        else:
            w = (tn - times[j - 1]) / max(times[j] - times[j - 1], 1e-6)
            fs = sets[j - 1][1] * (1 - w) + sets[j][1] * w
        total = np.full(len(freqs), 0.03)
        for i in range(len(fs)):
            fi, bwi = fs[i]
            q = fi / max(bwi, 20.0)
            total += FORMANT_GAIN[i] / np.sqrt(1.0 + (q * (freqs / fi - fi / np.maximum(freqs, 1.0))) ** 2)
        return total

    y = stft_apply(src, c.sr, resp)
    if band is not None:
        y = hp(lp(y, band[1], c.sr, 2), band[0], c.sr, 2)
    if amp is not None:
        y = y * curve(amp, n)
    y = norm(y)
    if drive > 0:
        y = sat(y * (1 + drive * 2.0), 1.0 + drive * 2.5)
    return y


def vox_at(buf, c, start_s, *args, gain=1.0, **kw):
    """Renders a vox gesture and drops it into buf at start_s seconds."""
    return place(buf, vox(c, *args, **kw), int(start_s * c.sr), gain)


def whisper_syllable(c, dur, vowels, lo=500.0, hi=5200.0, level=1.0):
    """Breath through moving vowel formants: noise only, no voicing."""
    return vox(c, dur, [(0, 100), (1, 100)], vowels, tract=1.0, voiced=0.0, breath=1.0, amp=[(0, 0), (0.12, 1.0), (0.7, 0.8), (1, 0)], band=(lo, hi), vib=None) * level


def hiss(c, dur, lo=3000.0, hi=9000.0, shape=None):
    n = c.n(dur)
    y = hp(lp(white(c, n), hi, c.sr, 2), lo, c.sr, 2)
    if shape is not None:
        y = y * curve(shape, n)
    return norm(y)


def whoosh(c, dur, f_start, f_end, q=1.6, shape=None):
    """Noise through a band-pass that slides from f_start to f_end."""
    n = c.n(dur)
    x = white(c, n)
    fa, fb = f_start, f_end

    def resp(k, tn, freqs):
        f0 = fa + (fb - fa) * min(max(tn, 0), 1)
        return 1.0 / np.sqrt(1.0 + (q * (freqs / f0 - f0 / np.maximum(freqs, 1.0))) ** 2)

    y = stft_apply(x, c.sr, resp)
    shape = shape or [(0, 0), (0.3, 1), (0.7, 0.8), (1, 0)]
    return norm(y * curve(shape, n))


def thud(c, dur=0.3, f=60.0, tau=0.07, click=0.3):
    n = c.n(dur)
    t = np.arange(n) / c.sr
    body = np.sin(2 * np.pi * (f + f * 0.6 * np.exp(-t / 0.02)) * t) * np.exp(-t / tau)
    ck = lp(white(c, n), 1800.0, c.sr, 2) * np.exp(-t / 0.006) * click
    return norm(body + ck)


def ring(c, dur, partials, decays, amps=None, noise_click=0.0, click_hz=6000.0):
    n = c.n(dur)
    t = np.arange(n) / c.sr
    out = np.zeros(n)
    amps = amps if amps is not None else [1.0] * len(partials)
    for f, d, a in zip(partials, decays, amps):
        out += a * np.exp(-t / d) * np.sin(2 * np.pi * f * t + c.rng.uniform(0, 6.28))
    if noise_click > 0:
        out += noise_click * hp(white(c, n), click_hz, c.sr, 1) * np.exp(-t / 0.002)
    return norm(out)


def metal_hit(c, dur=0.5, base=None, bright=1.0, damp=1.0, clicks=0.5):
    base = base or c.rng.uniform(260.0, 760.0)
    ratios = [1.0, 2.32, 3.97, 5.55, 7.88, 10.4]
    partials = [base * r * (1 + 0.012 * c.rng.standard_normal()) for r in ratios]
    decays = [0.22 / damp, 0.15 / damp, 0.10 / damp, 0.07 / damp, 0.05 / damp, 0.035 / damp]
    amps = [1.0, 0.8 * bright, 0.6 * bright, 0.45 * bright, 0.3 * bright, 0.2 * bright]
    return ring(c, dur, partials, decays, amps, noise_click=clicks)


def bone_click(c, dur=0.06, tone=None, level=1.0):
    tone = tone or c.rng.uniform(900.0, 2400.0)
    n = c.n(dur)
    t = np.arange(n) / c.sr
    y = np.sin(2 * np.pi * tone * t + c.rng.uniform(0, 6.28)) * np.exp(-t / 0.006)
    y += 0.6 * np.sin(2 * np.pi * tone * 2.7 * t + c.rng.uniform(0, 6.28)) * np.exp(-t / 0.003)
    y += 0.5 * bp(white(c, n), tone * 1.4, 2.0, c.sr) * np.exp(-t / 0.004)
    return norm(y) * level


def crackles(c, dur, density, tone=(2500.0, 8000.0), decay=0.004):
    """Sparse random pops: density a second."""
    n = c.n(dur)
    out = np.zeros(n)
    k = int(density * dur)
    for _ in range(max(1, k)):
        at = c.rng.integers(0, max(1, n - 16))
        ln = min(n - at, c.n(decay * 6))
        tt = np.arange(ln) / c.sr
        f = c.rng.uniform(*tone)
        pop = np.sin(2 * np.pi * f * tt + c.rng.uniform(0, 6.28)) * np.exp(-tt / decay)
        out[at:at + ln] += pop * c.rng.uniform(0.25, 1.0)
    return out


def blorp(c, dur=0.12, f_from=180.0, f_to=520.0, level=1.0):
    """One bubble: a sine that glides from f_from to f_to with a soft attack and a quick decay."""
    n = c.n(dur)
    t = np.arange(n) / c.sr
    k = t / dur
    f = f_from + (f_to - f_from) * k ** 0.7
    ph = 2 * np.pi * np.cumsum(f) / c.sr
    y = np.sin(ph) * np.exp(-t / (dur * 0.5)) * np.minimum(1.0, t / 0.004)
    y += 0.3 * np.sin(2 * ph) * np.exp(-t / (dur * 0.3))
    return y * level


def footstep(c, kind, scale=1.0):
    r = c.rng
    if kind == 'bare':
        y = thud(c, 0.16, r.uniform(80, 110) / scale, 0.04, 0.5)
        y = mixdown([y, 0.35 * lp(white(c, len(y)), 1500, c.sr, 2) * expdecay(c, len(y), 0.025)])
    elif kind == 'boot':
        y = thud(c, 0.2, r.uniform(70, 100) / scale, 0.05, 0.7)
        y = mixdown([y, 0.4 * hp(lp(white(c, len(y)), 4500, c.sr, 2), 800, c.sr, 2) * expdecay(c, len(y), 0.03)])
    elif kind == 'claw':
        y = np.zeros(c.n(0.2))
        for off in (0.0, 0.045):
            y = place(y, bone_click(c, 0.05, r.uniform(1800, 3200)), int(off * c.sr), 0.8)
        y = place(y, thud(c, 0.12, 90.0 / scale, 0.03, 0.2), 0, 0.5)
    elif kind == 'paw':
        y = thud(c, 0.14, r.uniform(90, 130) / scale, 0.03, 0.15)
        y = mixdown([y, 0.3 * lp(white(c, len(y)), 900, c.sr, 2) * expdecay(c, len(y), 0.02)])
    elif kind == 'metal':
        y = mixdown([thud(c, 0.3, r.uniform(55, 75) / scale, 0.08, 0.4), 0.8 * metal_hit(c, 0.28, r.uniform(380, 620), 0.8, 1.6, 0.4)])
    elif kind == 'stone':
        n = c.n(0.4)
        y = mixdown([thud(c, 0.4, r.uniform(38, 52) / scale, 0.14, 0.5),
                     0.5 * lp(brown(c, n), 400, c.sr, 2) * expdecay(c, n, 0.12),
                     0.25 * bp(white(c, n), 1200, 1.2, c.sr) * expdecay(c, n, 0.02)])
    elif kind == 'bone':
        y = np.zeros(c.n(0.16))
        y = place(y, bone_click(c, 0.07, r.uniform(1100, 1800)), 0, 1.0)
        y = place(y, thud(c, 0.1, 120.0, 0.02, 0.1), 0, 0.4)
    elif kind == 'wet':
        n = c.n(0.22)
        y = mixdown([blorp(c, 0.1, 160, 90, 0.9), 0.5 * bp(white(c, n), 700, 1.6, c.sr) * expdecay(c, n, 0.03)])
    elif kind == 'chitin':
        y = np.zeros(c.n(0.24))
        for k in range(r.integers(4, 7)):
            y = place(y, bone_click(c, 0.03, r.uniform(2400, 5200)), int(k * r.uniform(0.02, 0.04) * c.sr), r.uniform(0.4, 0.9))
    elif kind == 'hover':
        y = whoosh(c, 0.3, 600, 1400, 1.0, [(0, 0), (0.4, 0.6), (1, 0)]) * 0.5
    elif kind == 'hoof':
        y = mixdown([thud(c, 0.16, r.uniform(110, 150), 0.03, 0.6), 0.5 * bone_click(c, 0.05, 1300)])
    elif kind == 'robe':
        n = c.n(0.3)
        y = mixdown([0.5 * bp(white(c, n), 1400, 0.7, c.sr) * adsr(c, n, 0.03, 0.1, 0.3, 0.12), thud(c, 0.2, 90, 0.03, 0.1) * 0.4])
    elif kind == 'flap':
        n = c.n(0.3)
        y = whoosh(c, 0.3, 300, 900, 0.8, [(0, 0), (0.2, 1), (0.5, 0.3), (1, 0)]) * 0.9
        y = mixdown([y, thud(c, 0.2, 130, 0.04, 0.1) * 0.25])
    else:
        y = thud(c, 0.2, 90.0)
    return y


# ---------------------------------------------------------------------------------------------------------------------------------------------------------------------
# the engines the families are built from
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------------
def j(c, lo, hi):
    return float(c.rng.uniform(lo, hi))


def jitter_pts(c, pts, amount=0.05):
    return [(t, v * (1.0 + amount * c.rng.standard_normal())) for t, v in pts]


def laugh(c, V, count=None, spacing=None, burst=None, f0=None, rise=None, vow=('a', 'e'), tract=None, breath=None, vib=None, rough=None, tail=True, drive=0.0, inhale=True,
          glide=1.1, hold=0.0, tilt=1.4, amp_decay=0.0):
    """A run of "ha" bursts that climb or fall in pitch, each with a small glide, then (optionally) a longer last note."""
    r = c.rng
    count = count or int(r.integers(5, 9))
    spacing = spacing or j(c, 0.11, 0.14)
    burst = burst or j(c, 0.07, 0.095)
    f0 = f0 or V['f0']
    rise = 0.22 if rise is None else rise
    tract = tract or V.get('tract', 1.0)
    breath = V.get('breath', 0.12) if breath is None else breath
    vib = vib or V.get('vib', (6.0, 0.03))
    rough = V.get('rough', 0.0) if rough is None else rough
    buf = np.zeros(c.n(0.05))
    t0 = 0.0
    if inhale:
        hs = hiss(c, 0.1, 900.0, 5200.0, [(0, 0), (1, 1)]) * 0.18
        buf = place(buf, hs, 0, 1.0)
        t0 = 0.08
    for i in range(count):
        fc = f0 * (1.0 + rise * (i / max(1, count - 1))) * (1.0 + 0.03 * r.standard_normal())
        L = burst * (1.0 + 0.12 * r.standard_normal())
        g = (1.0 - amp_decay * i / max(1, count - 1))
        y = vox(c, max(0.05, L), [(0, fc * 0.93), (0.3, fc * glide), (1, fc)], [(0, vow[0]), (1, vow[1])], tract=tract, tilt=tilt, breath=breath, vib=vib, rough=rough,
                drive=drive, amp=[(0, 0), (0.12, 1.0), (0.6, 0.65), (1, 0)])
        buf = place(buf, y, int(t0 * c.sr), g)
        t0 += spacing * (1.0 + 0.1 * r.standard_normal())
    if tail:
        L = burst * 2.6
        fl = f0 * (1.0 + rise)
        y = vox(c, L, [(0, fl), (0.4, fl * 1.12), (1, fl * 0.8)], [(0, vow[1]), (1, 'i')], tract=tract, tilt=tilt, breath=breath, vib=(vib[0], vib[1] * 1.4), rough=rough,
                drive=drive, amp=[(0, 0), (0.1, 1), (0.7, 0.8), (1, 0)])
        buf = place(buf, y, int(t0 * c.sr), 0.9)
    if hold:
        pass
    return buf


def goblin_laugh(c, V):
    """The creepy, evil, high-pitched goblin cackle: an indrawn breath, then rising "heh" bursts with a fast warble, a second detuned voice behind the first, a shrill held end."""
    base = laugh(c, V, count=int(c.rng.integers(6, 10)), spacing=j(c, 0.095, 0.125), burst=j(c, 0.06, 0.08), f0=V['f0'] * j(c, 0.95, 1.1), rise=j(c, 0.28, 0.45), vow=('e', 'i'),
                 vib=(j(c, 13.0, 19.0), 0.05), breath=0.2, rough=0.12, glide=1.16, tail=True, amp_decay=0.0)
    second = resample(base, 1.0 + j(c, 0.012, 0.03))
    out = mixdown([base, 0.55 * np.concatenate([np.zeros(c.n(0.014)), second])])
    out = hp(out, 260.0, c.sr, 2)
    return out


def imp_cackle(c, V):
    base = laugh(c, V, count=int(c.rng.integers(10, 15)), spacing=j(c, 0.07, 0.09), burst=j(c, 0.04, 0.055), f0=V['f0'] * j(c, 0.95, 1.05), rise=j(c, 0.1, 0.3), vow=('i', 'ae'),
                 vib=(j(c, 24.0, 34.0), 0.06), breath=0.3, rough=0.3, glide=1.2, tail=True, drive=0.4)
    return hp(base, 400.0, c.sr, 2)


def whisper_phrase(c, syllables, lo=500.0, hi=5200.0, gap=0.05, spread=(0.13, 0.22), vowel_pool=('u', 'a', 'o', 'i', 'e', 'er')):
    r = c.rng
    buf = np.zeros(c.n(0.05))
    t0 = 0.0
    for _ in range(syllables):
        v1, v2 = r.choice(vowel_pool), r.choice(vowel_pool)
        L = float(r.uniform(*spread))
        y = whisper_syllable(c, L, [(0, v1), (1, v2)], lo, hi, level=float(r.uniform(0.6, 1.0)))
        hs = hiss(c, 0.04, 3500, 8500, [(0, 1), (1, 0)]) * 0.35
        y = place(y, hs, 0, 1.0)
        buf = place(buf, y, int(t0 * c.sr))
        t0 += L + gap * float(r.uniform(0.5, 1.8))
    return buf


def drone(c, dur, f=62.0, level=1.0, wob=0.4):
    n = c.n(dur)
    t = np.arange(n) / c.sr
    fm = f * (1.0 + 0.01 * np.sin(2 * np.pi * 0.35 * t) + 0.006 * wander(c, n, 3.0))
    ph = 2 * np.pi * np.cumsum(fm) / c.sr
    y = np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.25 * np.sin(3 * ph)
    y = lp(y, 380.0, c.sr, 2) * (1.0 + wob * np.sin(2 * np.pi * 0.7 * t)) * adsr(c, n, 0.25, 0.1, 0.8, 0.5)
    return norm(y) * level


def lich_voice(c, dur, syllables, tone=1.0, wet=0.6, taps=2, tail=1.1, hall=True):
    """The lich's speech: layered whispers (one low and slow behind), a drone, and a long echo."""
    r = c.rng
    main = whisper_phrase(c, syllables, 450, 5000)
    low = resample(whisper_phrase(c, max(2, syllables - 1), 350, 3000), 0.8)
    n = c.n(dur)
    buf = np.zeros(n)
    buf = place(buf, main, int(0.02 * c.sr), 1.0)
    buf = place(buf, low, int(0.11 * c.sr), 0.55)
    buf = place(buf, drone(c, dur * 0.9, 58.0 * tone, 1.0, 0.5), 0, 0.14)
    buf = buf[:max(n, len(buf))]
    buf = ringmod(buf, c.sr, 110.0 * tone, 0.15)
    buf = echo(buf, c.sr, 0.3, 0.5, taps, 0.55)
    return reverb(buf, c.sr, 2.4, 0.5, 0.03, tail) if hall else buf


def roar(c, V, dur, f0=None, rise=0.2, tract=None, noise=0.4, sub=0.7, drive=1.0, wob=True, pts=None, vseq=None, tail=0.0):
    f0 = f0 or V['f0']
    tract = tract or V.get('tract', 0.6)
    pts = pts or [(0, f0 * 0.8), (0.2, f0 * (1 + rise)), (0.6, f0 * 1.05), (1, f0 * 0.7)]
    vseq = vseq or [(0, 'o'), (0.3, 'a'), (0.7, 'ae'), (1, 'o')]
    y = vox(c, dur, pts, vseq, tract=tract, tilt=1.05, breath=noise * 0.55, rough=V.get('rough', 0.55), rough_hz=j(c, 38.0, 62.0), vib=(6.0, 0.015), jitter=0.03, sub=sub, drive=drive * 0.5,
            amp=[(0, 0), (0.08, 1.0), (0.6, 0.9), (1, 0)], bwmul=1.6)
    y = y + 0.25 * lp(white(c, len(y)), 700, c.sr, 2) * adsr(c, len(y), 0.05, 0.1, 0.8, 0.2)
    return norm(lp(y, 3400.0, c.sr, 2))


def wail(c, V, dur, f0=None, rise=0.5, vib=(5.5, 0.045), vseq=None, breath=0.35, wet=0.55, fall=0.0):
    f0 = f0 or V['f0']
    vseq = vseq or [(0, 'o'), (0.4, 'a'), (1, 'u')]
    pts = [(0, f0 * 0.8), (0.35, f0 * (1 + rise)), (0.75, f0 * (1 + rise * 0.8)), (1, f0 * (1 - fall))]
    y = vox(c, dur, pts, vseq, tract=V.get('tract', 1.0), tilt=1.7, breath=breath, vib=vib, jitter=0.02, amp=[(0, 0), (0.25, 1.0), (0.8, 0.8), (1, 0)])
    y = chorus(y, c.sr, 0.003, 0.4, 0.4, 2)
    return reverb(y, c.sr, 2.2, wet, 0.02)


def howl(c, V, dur, f0=None):
    f0 = f0 or V['f0']
    pts = [(0, f0 * 0.8), (0.25, f0 * 1.5), (0.6, f0 * 1.6), (0.85, f0 * 1.45), (1, f0 * 1.0)]
    y = vox(c, dur, pts, [(0, 'u'), (0.4, 'o'), (1, 'u')], tract=V.get('tract', 0.9), tilt=1.9, breath=0.18, vib=(5.8, 0.03), jitter=0.012, drive=0.2,
            amp=[(0, 0), (0.2, 1.0), (0.85, 0.85), (1, 0)])
    return reverb(y, c.sr, 1.6, 0.25, 0.01, 0.6)


def growl(c, V, dur, f0=None, depth=0.7, snarl=0.0):
    f0 = f0 or V['f0']
    pts = [(0, f0 * 0.9), (0.3, f0 * 1.1), (0.7, f0 * 0.95), (1, f0 * 0.75)]
    y = vox(c, dur, pts, [(0, 'o'), (0.5, 'uh'), (1, 'o')], tract=V.get('tract', 0.8), tilt=1.2, breath=0.3, rough=depth, rough_hz=j(c, 42.0, 78.0), vib=(0.0, 0.0), jitter=0.04,
            sub=0.5, drive=0.8, amp=[(0, 0), (0.1, 1), (0.8, 0.9), (1, 0)], bwmul=1.4)
    if snarl > 0:
        y = y + snarl * hp(white(c, len(y)), 2500, c.sr, 1) * curve([(0, 0), (0.2, 1), (1, 0)], len(y)) * 0.3
    return y


def chitter_train(c, dur, rate, tone=(3000.0, 6500.0), am=30.0, accel=0.0, level=1.0):
    n = c.n(dur)
    out = np.zeros(n)
    t = 0.0
    while t < dur:
        y = bone_click(c, 0.02, c.rng.uniform(*tone), 0.8)
        out = place(out, y, int(t * c.sr), level * (0.5 + 0.5 * c.rng.random()))
        t += (1.0 / rate) * (1.0 - accel * (t / dur)) * (0.7 + 0.6 * c.rng.random())
    return out[:n]


def buzz(c, dur, f_lo, f_hi, am=38.0, level=1.0):
    n = c.n(dur)
    t = np.arange(n) / c.sr
    f = curve([(0, f_lo), (0.5, f_hi), (1, f_lo * 1.1)], n)
    ph = 2 * np.pi * np.cumsum(f) / c.sr
    y = np.zeros(n)
    for k in range(1, 12):
        y += np.sin(k * ph) / k
    y *= 0.5 + 0.5 * np.sin(2 * np.pi * am * t)
    y = bp(y, f_hi * 3, 0.6, c.sr)
    return norm(y) * adsr(c, n, 0.15, 0.1, 0.8, 0.3) * level


def squelch(c, dur, lo=250.0, hi=1100.0, q=4.0, wet=0.3):
    n = c.n(dur)
    x = white(c, n)
    k0 = c.rng.uniform(0, 6.28)

    def resp(k, tn, freqs):
        f0 = lo + (hi - lo) * (0.5 + 0.5 * math.sin(tn * 5.0 + k0))
        return 1.0 / np.sqrt(1.0 + (q * (freqs / f0 - f0 / np.maximum(freqs, 1.0))) ** 2)

    y = stft_apply(x, c.sr, resp)
    y = y * (0.55 + 0.45 * np.sin(2 * np.pi * c.rng.uniform(9.0, 17.0) * np.arange(n) / c.sr)) ** 2
    return norm(y) * adsr(c, n, 0.02, 0.1, 0.7, 0.1)


def choir(c, dur, f0, vseq, voices=4, detune=0.012, breath=0.08, tract=1.0, wet=0.55, swell=True, tail=None):
    n = c.n(dur)
    out = np.zeros(n)
    for v in range(voices):
        fv = f0 * (1.0 + detune * (v - (voices - 1) / 2.0) + 0.004 * c.rng.standard_normal()) * (2.0 if v == voices - 1 and voices > 3 else 1.0)
        y = vox(c, dur, [(0, fv * 0.99), (0.5, fv * 1.01), (1, fv)], vseq, tract=tract, tilt=1.4, breath=breath, vib=(5.2 + 0.4 * v, 0.012), jitter=0.01,
                amp=[(0, 0), (0.3, 1.0), (0.8, 0.9), (1, 0)] if swell else [(0, 0), (0.05, 1), (0.9, 0.9), (1, 0)])
        out[:len(y)] += y[:n]
    return reverb(norm(out), c.sr, 2.0, wet, 0.02, tail)


def stone_grind(c, dur, lo=70.0, hi=520.0, chaos=5.0):
    n = c.n(dur)
    x = brown(c, n) + 0.3 * pink(c, n)
    y = hp(lp(x, hi, c.sr, 2), lo, c.sr, 2)
    gate = np.clip(0.5 + 0.9 * wander(c, n, chaos), 0.05, 1.5)
    return norm(y * gate)


def stone_crack(c, dur=0.2, level=1.0):
    n = c.n(dur)
    t = np.arange(n) / c.sr
    y = hp(white(c, n), 700, c.sr, 2) * np.exp(-t / 0.012)
    y += 0.7 * bp(white(c, n), 260, 1.2, c.sr) * np.exp(-t / 0.05)
    y += 0.5 * thud(c, dur, 70, 0.05, 0.0)[:n]
    return norm(y) * level


def finalize_env(x, c, fade_in=0.003, fade_out=0.02):
    n = len(x)
    fi, fo = min(n // 4, int(fade_in * c.sr)), min(n // 3, int(fade_out * c.sr))
    if fi > 0:
        x[:fi] *= np.linspace(0, 1, fi)
    if fo > 0:
        x[-fo:] *= np.linspace(1, 0, fo) ** 1.5
    return x


# ======================================================================================================================================================================
# the families
# ======================================================================================================================================================================
# voice parameters of the vocal families (f0 = the usual pitch in Hz, tract = size of the throat: below 1 big and deep, above 1 small and shrill)
VOICES = {
    'goblin': dict(f0=640.0, tract=1.38, tilt=1.35, breath=0.18, rough=0.12, vib=(15.0, 0.045), vow=['e', 'i', 'ae']),
    'orc': dict(f0=118.0, tract=0.88, tilt=1.25, breath=0.2, rough=0.35, rough_hz=62.0, vib=(5.0, 0.012), sub=0.15, vow=['o', 'a', 'uh']),
    'kobold': dict(f0=520.0, tract=1.28, tilt=1.4, breath=0.22, rough=0.25, vib=(12.0, 0.035), vow=['i', 'ae', 'e']),
    'ghoul': dict(f0=92.0, tract=0.9, tilt=1.1, breath=0.45, rough=0.5, rough_hz=48.0, vib=(4.0, 0.02), sub=0.3, vow=['o', 'uh', 'u']),
    'demon': dict(f0=74.0, tract=0.7, tilt=1.0, breath=0.28, rough=0.55, rough_hz=55.0, vib=(5.0, 0.012), sub=0.5, drive=0.5, vow=['o', 'a', 'uh']),
    'imp': dict(f0=880.0, tract=1.5, tilt=1.25, breath=0.3, rough=0.3, vib=(26.0, 0.05), vow=['i', 'ae', 'e']),
    'troll': dict(f0=84.0, tract=0.8, tilt=1.2, breath=0.28, rough=0.4, rough_hz=46.0, vib=(3.5, 0.015), sub=0.3, vow=['uh', 'o', 'u']),
    'ogre': dict(f0=70.0, tract=0.74, tilt=1.2, breath=0.24, rough=0.45, rough_hz=40.0, vib=(4.0, 0.012), sub=0.3, vow=['o', 'a', 'uh']),
    'cultist': dict(f0=125.0, tract=0.95, tilt=1.45, breath=0.22, rough=0.1, vib=(5.0, 0.02), vow=['o', 'u', 'a']),
    'vampire': dict(f0=118.0, tract=0.98, tilt=1.7, breath=0.3, rough=0.05, vib=(4.5, 0.015), vow=['a', 'e', 'o']),
    'npc': dict(f0=170.0, tract=1.0, tilt=1.6, breath=0.12, rough=0.0, vib=(5.3, 0.02), vow=['a', 'o', 'e']),
    'beast': dict(f0=78.0, tract=0.85, tilt=1.1, breath=0.3, rough=0.6, rough_hz=52.0, vib=(0.0, 0.0), vow=['o', 'uh']),
    'aquatic': dict(f0=105.0, tract=0.92, tilt=1.4, breath=0.4, rough=0.4, rough_hz=32.0, vib=(3.0, 0.03), vow=['o', 'u', 'uh']),
    'harpy': dict(f0=1250.0, tract=1.3, tilt=1.1, breath=0.25, rough=0.2, vib=(8.0, 0.05), vow=['a', 'ae', 'i']),
    'wraith': dict(f0=330.0, tract=1.1, tilt=1.8, breath=0.45, rough=0.0, vib=(5.2, 0.05), vow=['o', 'a', 'u']),
    'celestial': dict(f0=392.0, tract=1.05, tilt=1.5, breath=0.06, rough=0.0, vib=(5.6, 0.012), vow=['a', 'o', 'e']),
    'dragon': dict(f0=58.0, tract=0.55, tilt=1.0, breath=0.45, rough=0.6, rough_hz=44.0, vib=(4.0, 0.012), vow=['o', 'a']),
    'drake': dict(f0=210.0, tract=0.9, tilt=1.1, breath=0.5, rough=0.4, rough_hz=60.0, vib=(7.0, 0.03), vow=['a', 'ae', 'i']),
    'lich': dict(f0=110.0, tract=1.0, tilt=1.5, breath=0.5, rough=0.0, vib=(4.0, 0.02), vow=['u', 'a', 'o']),
}

RECIPES = {}


def recipe(family, cue):
    def deco(fn):
        RECIPES[(family, cue)] = fn
        return fn
    return deco


def shaped(y, pts):
    return y * curve(pts, len(y))


def vocal_cues(family):
    """Registers the seven vocal cues every voiced family shares (everything but the taunt), shaped by the family's voice parameters. Returns the gesture helper."""
    V = VOICES[family]
    vows = V['vow']

    def pick(c):
        return str(c.rng.choice(vows))

    def common(c, dur, pts, vseq, **over):
        kw = dict(tract=V['tract'], tilt=V['tilt'], breath=V['breath'], rough=V['rough'], rough_hz=V.get('rough_hz', 70.0), vib=V['vib'], sub=V.get('sub', 0.0), drive=V.get('drive', 0.0))
        kw.update(over)
        return vox(c, dur, pts, vseq, **kw)

    def ambient(c):
        buf = np.zeros(c.n(0.05))
        t0 = 0.0
        base = V['f0'] * j(c, 0.82, 0.98)
        for _ in range(int(c.rng.integers(2, 4))):
            L = j(c, 0.18, 0.34)
            f = base * j(c, 0.9, 1.12)
            y = common(c, L, [(0, f * 0.95), (0.5, f * 1.04), (1, f * 0.9)], [(0, pick(c)), (1, pick(c))], amp=[(0, 0), (0.2, 0.9), (0.7, 0.7), (1, 0)])
            buf = place(buf, y, int(t0 * c.sr), 0.85)
            t0 += L + j(c, 0.06, 0.2)
        return buf

    def aggro(c):
        f = V['f0'] * j(c, 1.0, 1.25)
        return common(c, j(c, 0.5, 0.85), [(0, f * 0.85), (0.25, f * 1.4), (0.7, f * 1.3), (1, f * 0.95)], [(0, 'a'), (0.5, pick(c)), (1, 'o')],
                      rough=min(1.0, V['rough'] * 1.5 + 0.1), drive=V.get('drive', 0.0) + 0.5, amp=[(0, 0), (0.08, 1), (0.75, 0.9), (1, 0)])

    def attack(c):
        f = V['f0'] * j(c, 1.1, 1.45)
        y = common(c, j(c, 0.2, 0.36), [(0, f * 1.05), (0.4, f * 0.9), (1, f * 0.7)], [(0, 'a'), (1, pick(c))], drive=V.get('drive', 0.0) + 0.6, breath=V['breath'] + 0.2,
                   amp=[(0, 0), (0.04, 1), (0.5, 0.8), (1, 0)])
        return place(y, hiss(c, 0.05, 1500, 6000, [(0, 1), (1, 0)]) * 0.35, 0, 1.0)

    def hurt(c):
        f = V['f0'] * j(c, 1.3, 1.7)
        return common(c, j(c, 0.2, 0.36), [(0, f * 0.9), (0.15, f * 1.25), (1, f * 0.85)], [(0, 'ae'), (0.5, 'e'), (1, pick(c))], breath=V['breath'] + 0.1,
                      amp=[(0, 0), (0.05, 1), (0.6, 0.8), (1, 0)])

    def death(c):
        f = V['f0'] * j(c, 1.1, 1.35)
        return common(c, j(c, 0.85, 1.5), [(0, f), (0.3, f * 0.85), (1, f * 0.4)], [(0, 'a'), (0.5, 'o'), (1, 'u')], breath=V['breath'] + 0.25, rough=min(1.0, V['rough'] + 0.2),
                      amp=[(0, 0), (0.08, 1), (0.5, 0.7), (1, 0)])

    def cast(c):
        L = j(c, 1.0, 1.5)
        n_syl = int(c.rng.integers(3, 5))
        buf = np.zeros(c.n(0.05))
        f = V['f0'] * j(c, 0.85, 1.0)
        t0 = 0.0
        for i in range(n_syl):
            y = common(c, L / n_syl * 0.9, [(0, f * (1.0 + 0.04 * i)), (1, f * (1.02 + 0.04 * i))], [(0, pick(c)), (0.6, pick(c)), (1, pick(c))], breath=V['breath'] + 0.1,
                       vib=(6.0, 0.03), amp=[(0, 0), (0.15, 1), (0.8, 0.8), (1, 0)])
            buf = place(buf, y, int(t0 * c.sr))
            t0 += L / n_syl
        buf = ringmod(buf, c.sr, V['f0'] * 3.0, 0.18)
        return reverb(buf, c.sr, 1.5, 0.35, 0.01, 0.55)

    RECIPES[(family, 'ambient')] = ambient
    RECIPES[(family, 'aggro')] = aggro
    RECIPES[(family, 'attack')] = attack
    RECIPES[(family, 'hurt')] = hurt
    RECIPES[(family, 'death')] = death
    RECIPES[(family, 'cast')] = cast
    return common


def both(*parts):
    return mixdown([p for p in parts])


# ---- goblin: the creepy, evil, high-pitched laugh -------------------------------------------------------------------------------------------------------------------
vocal_cues('goblin')
recipe('goblin', 'taunt')(lambda c: goblin_laugh(c, VOICES['goblin']))
recipe('goblin', 'step')(lambda c: footstep(c, 'bare', 0.6))


@recipe('goblin', 'ambient')
def _goblin_ambient(c):
    """Giggling mutters: a few short high "hee" with a creepy warble, now and then a hiss."""
    V = VOICES['goblin']
    buf = np.zeros(c.n(0.05))
    t0 = 0.0
    for _ in range(int(c.rng.integers(3, 6))):
        L = j(c, 0.07, 0.16)
        f = V['f0'] * j(c, 0.95, 1.3)
        y = vox(c, L, [(0, f * 0.95), (1, f * 1.1)], [(0, 'i'), (1, 'e')], tract=V['tract'], tilt=1.3, breath=0.25, vib=(j(c, 12.0, 18.0), 0.05), rough=0.1, amp=[(0, 0), (0.15, 1), (0.7, 0.7), (1, 0)])
        buf = place(buf, y, int(t0 * c.sr), 0.7)
        t0 += L + j(c, 0.06, 0.22)
    if c.rng.random() < 0.5:
        buf = place(buf, hiss(c, 0.2, 2500, 8000, [(0, 0), (0.4, 1), (1, 0)]) * 0.25, int(t0 * c.sr))
    return buf


# ---- orc -------------------------------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('orc')
recipe('orc', 'step')(lambda c: footstep(c, 'boot', 1.0))


@recipe('orc', 'taunt')
def _orc_taunt(c):
    """A barking guffaw: "huh huh huh HAH" low in the chest."""
    V = VOICES['orc']
    return laugh(c, V, count=int(c.rng.integers(4, 7)), spacing=j(c, 0.16, 0.2), burst=j(c, 0.1, 0.13), f0=V['f0'] * 1.1, rise=-0.12, vow=('uh', 'a'), tract=V['tract'], breath=0.3,
                 rough=0.5, drive=0.5, tail=True, glide=1.2, inhale=False)


# ---- kobold ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('kobold')
recipe('kobold', 'step')(lambda c: footstep(c, 'claw', 0.7))


@recipe('kobold', 'taunt')
def _kobold_taunt(c):
    """A yapping laugh that trails into a hiss."""
    V = VOICES['kobold']
    a = laugh(c, V, count=int(c.rng.integers(6, 9)), spacing=j(c, 0.085, 0.11), burst=j(c, 0.05, 0.065), f0=V['f0'] * 1.05, rise=0.18, vow=('i', 'ae'), tract=V['tract'], breath=0.3, rough=0.3,
              tail=False, glide=1.25, inhale=False)
    return place(a, hiss(c, 0.35, 2500, 7500, [(0, 0), (0.3, 1), (1, 0)]) * 0.3, max(0, len(a) - int(0.1 * c.sr)))


# ---- ghoul -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('ghoul')


@recipe('ghoul', 'step')
def _ghoul_step(c):
    n = c.n(0.3)
    return mixdown([footstep(c, 'boot', 1.3) * 0.6, 0.4 * lp(brown(c, n), 500, c.sr, 2) * expdecay(c, n, 0.08)])


@recipe('ghoul', 'taunt')
def _ghoul_taunt(c):
    """A hungry moan that breaks into wet, rasping chuckles."""
    V = VOICES['ghoul']
    moan = vox(c, 0.9, [(0, 95), (0.4, 120), (1, 70)], [(0, 'o'), (0.5, 'a'), (1, 'u')], tract=0.9, breath=0.5, rough=0.55, rough_hz=36.0, jitter=0.04, amp=[(0, 0), (0.25, 1), (0.8, 0.8), (1, 0)])
    chuck = laugh(c, V, count=int(c.rng.integers(4, 6)), spacing=0.17, burst=0.1, f0=88.0, rise=-0.2, vow=('uh', 'o'), tract=0.9, breath=0.55, rough=0.6, drive=0.3, tail=False, inhale=False)
    out = np.zeros(c.n(0.05))
    out = place(out, moan, 0)
    return place(out, chuck, int(0.75 * c.sr), 0.85)


# ---- demon -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('demon')
recipe('demon', 'step')(lambda c: mixdown([footstep(c, 'stone', 1.2) * 0.6, footstep(c, 'hoof', 1.0) * 0.6]))


@recipe('demon', 'taunt')
def _demon_taunt(c):
    """A slow infernal laugh: "HA... HA... HA..." doubled an octave down and distorted, with a long hall behind it."""
    V = VOICES['demon']
    count, spacing, burst = int(c.rng.integers(3, 5)), j(c, 0.24, 0.29), j(c, 0.15, 0.18)
    base = laugh(c, V, count=count, spacing=spacing, burst=burst, f0=V['f0'], rise=-0.18, vow=('a', 'o'), tract=0.7, breath=0.3, rough=0.6, drive=1.0, tail=True, glide=1.18, inhale=False, tilt=1.0)
    low = laugh(c, V, count=count, spacing=spacing, burst=burst, f0=V['f0'] * 0.5, rise=-0.18, vow=('o', 'u'), tract=0.62, breath=0.3, rough=0.6, drive=1.0, tail=True, glide=1.18, inhale=False, tilt=1.0)
    out = mixdown([base, 0.7 * low])
    out = chorus(out, c.sr, 0.004, 0.5, 0.5, 2)
    return reverb(out, c.sr, 1.8, 0.4, 0.02, 0.8)


# ---- imp -------------------------------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('imp')
recipe('imp', 'taunt')(lambda c: imp_cackle(c, VOICES['imp']))
recipe('imp', 'step')(lambda c: footstep(c, 'claw', 0.5) if c.rng.random() < 0.6 else footstep(c, 'flap', 0.8) * 0.7)

# ---- troll and ogre ----------------------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('troll')
recipe('troll', 'step')(lambda c: mixdown([footstep(c, 'bare', 1.7) * 0.8, footstep(c, 'wet', 1.2) * 0.3]))


@recipe('troll', 'taunt')
def _troll_taunt(c):
    V = VOICES['troll']
    return laugh(c, V, count=int(c.rng.integers(3, 5)), spacing=j(c, 0.22, 0.28), burst=j(c, 0.15, 0.19), f0=V['f0'] * 1.2, rise=-0.25, vow=('uh', 'o'), tract=0.8, breath=0.35, rough=0.5, drive=0.7,
                 tail=True, inhale=False, glide=1.1)


vocal_cues('ogre')
recipe('ogre', 'step')(lambda c: mixdown([footstep(c, 'stone', 0.9) * 0.8, footstep(c, 'boot', 1.8) * 0.4]))


@recipe('ogre', 'taunt')
def _ogre_taunt(c):
    V = VOICES['ogre']
    return laugh(c, V, count=int(c.rng.integers(3, 5)), spacing=j(c, 0.2, 0.26), burst=j(c, 0.13, 0.17), f0=V['f0'] * 1.3, rise=-0.3, vow=('a', 'o'), tract=0.74, breath=0.3, rough=0.45, drive=0.6,
                 tail=True, inhale=True, glide=1.15)


# ---- cultist, vampire, npc ----------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('cultist')
recipe('cultist', 'step')(lambda c: footstep(c, 'robe', 1.0))


@recipe('cultist', 'taunt')
def _cultist_taunt(c):
    """Zealot laughter: fast, a little hysterical, cracking upward."""
    V = VOICES['cultist']
    return laugh(c, V, count=int(c.rng.integers(7, 11)), spacing=j(c, 0.1, 0.13), burst=j(c, 0.06, 0.08), f0=V['f0'] * 1.5, rise=0.35, vow=('a', 'e'), tract=0.98, breath=0.25, rough=0.15,
                 tail=True, inhale=True, glide=1.18)


@recipe('cultist', 'ambient')
def _cultist_ambient(c):
    """A hooded chant murmured under the breath: a droned pair of notes."""
    return choir(c, j(c, 1.0, 1.5), VOICES['cultist']['f0'] * j(c, 0.9, 1.0), [(0, 'o'), (0.5, 'u'), (1, 'o')], voices=2, detune=0.01, breath=0.2, tract=0.95, wet=0.3) * 0.8


vocal_cues('vampire')
recipe('vampire', 'step')(lambda c: footstep(c, 'robe', 1.2))


@recipe('vampire', 'taunt')
def _vampire_taunt(c):
    """An elegant, breathy chuckle: three low notes and a sigh."""
    V = VOICES['vampire']
    return laugh(c, V, count=3, spacing=j(c, 0.24, 0.3), burst=j(c, 0.14, 0.18), f0=V['f0'] * 1.15, rise=-0.1, vow=('a', 'e'), tract=0.98, breath=0.55, rough=0.0, tail=True, inhale=True,
                 glide=1.1, tilt=1.9)


@recipe('vampire', 'ambient')
def _vampire_ambient(c):
    y = whisper_phrase(c, int(c.rng.integers(3, 5)), 600, 7000, 0.06, (0.14, 0.26), ('a', 'e', 'i', 'o'))
    v = vox(c, 0.9, [(0, 110), (0.5, 122), (1, 105)], [(0, 'a'), (0.5, 'o'), (1, 'u')], tract=0.98, tilt=1.9, breath=0.45, vib=(4.5, 0.015), amp=[(0, 0), (0.3, 0.6), (0.8, 0.5), (1, 0)])
    return mixdown([y * 0.8, np.concatenate([np.zeros(c.n(0.1)), v]) * 0.6])


vocal_cues('npc')
recipe('npc', 'step')(lambda c: footstep(c, 'boot', 0.9) * 0.8)


@recipe('npc', 'taunt')
def _npc_taunt(c):
    V = VOICES['npc']
    return laugh(c, V, count=int(c.rng.integers(3, 5)), spacing=j(c, 0.16, 0.2), burst=j(c, 0.09, 0.12), f0=V['f0'] * 1.2, rise=0.05, vow=('a', 'o'), tract=1.0, breath=0.18, tail=False,
                 inhale=False, glide=1.08)


@recipe('npc', 'ambient')
def _npc_ambient(c):
    """A hummed phrase: "hm-hmm"."""
    V = VOICES['npc']
    buf = np.zeros(c.n(0.05))
    t0 = 0.0
    f = V['f0'] * j(c, 0.85, 1.0)
    for fr in (1.0, 1.12, 0.94):
        L = j(c, 0.18, 0.3)
        y = vox(c, L, [(0, f * fr), (1, f * fr * 1.01)], [(0, 'u'), (1, 'oo')], tract=1.0, tilt=1.9, breath=0.05, vib=(5.3, 0.02), amp=[(0, 0), (0.2, 1), (0.8, 0.9), (1, 0)])
        buf = place(buf, lp(y, 900.0, c.sr, 2), int(t0 * c.sr), 0.8)
        t0 += L + j(c, 0.04, 0.12)
    return buf


@recipe('npc', 'cast')
def _npc_cast(c):
    f = j(c, 520.0, 700.0)
    return reverb(ring(c, 1.1, [f, f * 1.5, f * 2.0, f * 3.0], [0.55, 0.45, 0.35, 0.25], [1, 0.6, 0.4, 0.2], 0.05), c.sr, 1.4, 0.4)


# ---- beast: growls, snarls and howls ------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('beast')
recipe('beast', 'step')(lambda c: footstep(c, 'paw', 1.0))
recipe('beast', 'ambient')(lambda c: growl(c, VOICES['beast'], j(c, 0.8, 1.4), depth=0.55) * 0.8)
recipe('beast', 'aggro')(lambda c: growl(c, VOICES['beast'], j(c, 0.6, 1.0), f0=95.0, depth=0.8, snarl=1.0))


@recipe('beast', 'attack')
def _beast_attack(c):
    n = c.n(0.3)
    snap = mixdown([bone_click(c, 0.08, 1500) * 0.7, hp(white(c, n), 1800, c.sr, 1) * adsr(c, n, 0.005, 0.05, 0.1, 0.1) * 0.4])
    bark = vox(c, 0.22, [(0, 280), (0.3, 360), (1, 220)], [(0, 'a'), (1, 'o')], tract=0.85, breath=0.4, rough=0.35, drive=0.8, amp=[(0, 0), (0.05, 1), (1, 0)])
    return mixdown([snap, bark])


recipe('beast', 'hurt')(lambda c: vox(c, j(c, 0.25, 0.38), [(0, 380), (0.15, 620), (1, 340)], [(0, 'ae'), (1, 'i')], tract=0.9, breath=0.2, rough=0.12, vib=(9.0, 0.04), amp=[(0, 0), (0.05, 1), (0.6, 0.8), (1, 0)]))
recipe('beast', 'death')(lambda c: vox(c, j(c, 0.9, 1.4), [(0, 460), (0.2, 520), (1, 190)], [(0, 'ae'), (0.6, 'o'), (1, 'u')], tract=0.9, breath=0.35, rough=0.15, vib=(7.0, 0.05), amp=[(0, 0), (0.1, 1), (0.6, 0.6), (1, 0)]))
recipe('beast', 'cast')(lambda c: howl(c, VOICES['beast'], j(c, 1.1, 1.5), f0=240.0))
recipe('beast', 'taunt')(lambda c: howl(c, VOICES['beast'], j(c, 1.5, 1.9), f0=210.0))

# ---- aquatic: wet gurgles and bubbles ---------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('aquatic')
recipe('aquatic', 'step')(lambda c: footstep(c, 'wet', 1.0))


def _bubbled(c, y, count):
    for _ in range(count):
        at = int(c.rng.uniform(0, max(0.05, len(y) / c.sr - 0.1)) * c.sr)
        y = place(y, blorp(c, j(c, 0.05, 0.12), j(c, 150, 260), j(c, 380, 700), 0.5), at, j(c, 0.3, 0.7))
    return y


def _aquatic_wrap(cue, bubbles):
    base = RECIPES[('aquatic', cue)]

    def f(c):
        return _bubbled(c, lp(base(c), 1400.0, c.sr, 2), bubbles)
    RECIPES[('aquatic', cue)] = f


for _cue, _b in (('ambient', 4), ('aggro', 3), ('attack', 1), ('hurt', 2), ('death', 7), ('cast', 6)):
    _aquatic_wrap(_cue, _b)


@recipe('aquatic', 'taunt')
def _aquatic_taunt(c):
    V = VOICES['aquatic']
    y = laugh(c, V, count=int(c.rng.integers(5, 8)), spacing=j(c, 0.12, 0.16), burst=j(c, 0.07, 0.09), f0=V['f0'] * 1.5, rise=0.1, vow=('u', 'uh'), tract=0.92, breath=0.5, rough=0.4, tail=False, inhale=False)
    return _bubbled(c, lp(y, 1600.0, c.sr, 2), 8)


# ---- harpy -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('harpy')
recipe('harpy', 'step')(lambda c: footstep(c, 'flap', 1.0))


def screech(c, f0, dur, rise=0.5, rough=0.3, tract=1.25, drive=0.5, breath=0.3):
    pts = [(0, f0 * 0.85), (0.15, f0 * (1.0 + rise)), (0.7, f0 * (1.0 + rise * 0.8)), (1, f0 * 0.9)]
    return vox(c, dur, pts, [(0, 'a'), (0.5, 'ae'), (1, 'i')], tract=tract, tilt=1.0, breath=breath, rough=rough, rough_hz=j(c, 50.0, 90.0), vib=(9.0, 0.05), jitter=0.03, drive=drive,
               amp=[(0, 0), (0.06, 1), (0.7, 0.9), (1, 0)])


recipe('harpy', 'aggro')(lambda c: screech(c, 1100.0 * j(c, 0.95, 1.1), j(c, 0.55, 0.9)))
recipe('harpy', 'ambient')(lambda c: vox(c, j(c, 0.9, 1.4), [(0, 800), (0.25, 1050), (0.5, 880), (0.75, 1150), (1, 820)], [(0, 'o'), (0.5, 'a'), (1, 'i')], tract=1.3, tilt=1.5, breath=0.15,
                                           vib=(6.5, 0.05), amp=[(0, 0), (0.2, 0.7), (0.8, 0.6), (1, 0)]))
recipe('harpy', 'attack')(lambda c: mixdown([screech(c, 1000.0, j(c, 0.22, 0.34), 0.3, 0.4), whoosh(c, 0.25, 700, 2200, 1.0) * 0.5]))
recipe('harpy', 'hurt')(lambda c: screech(c, 1300.0 * j(c, 0.95, 1.15), j(c, 0.22, 0.36), 0.35, 0.15))


@recipe('harpy', 'death')
def _harpy_death(c):
    s = vox(c, j(c, 0.9, 1.3), [(0, 1500), (0.2, 1700), (1, 380)], [(0, 'a'), (0.6, 'o'), (1, 'u')], tract=1.25, tilt=1.1, breath=0.4, rough=0.25, vib=(8.0, 0.06), amp=[(0, 0), (0.06, 1), (0.6, 0.6), (1, 0)])
    return mixdown([s, 0.18 * lp(white(c, len(s)), 3000, c.sr, 1) * curve([(0, 0.2), (0.5, 1), (1, 0)], len(s))])


@recipe('harpy', 'cast')
def _harpy_cast(c):
    y = vox(c, j(c, 1.1, 1.6), [(0, 700), (0.5, 1500), (1, 1100)], [(0, 'i'), (0.5, 'a'), (1, 'e')], tract=1.3, tilt=1.3, breath=0.2, vib=(7.0, 0.06), amp=[(0, 0), (0.2, 1), (0.8, 0.8), (1, 0)])
    y = y * (0.7 + 0.3 * np.sin(2 * np.pi * 9.0 * np.arange(len(y)) / c.sr))
    return reverb(y, c.sr, 1.5, 0.4)


@recipe('harpy', 'taunt')
def _harpy_taunt(c):
    V = VOICES['harpy']
    y = laugh(c, V, count=int(c.rng.integers(5, 8)), spacing=j(c, 0.1, 0.13), burst=j(c, 0.06, 0.08), f0=V['f0'] * 0.9, rise=0.3, vow=('a', 'i'), tract=1.3, breath=0.3, rough=0.3, drive=0.4,
              tail=True, inhale=False, glide=1.3)
    return hp(y, 500.0, c.sr, 2)


# ---- wraith: airy moans and wails ------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('wraith')
recipe('wraith', 'ambient')(lambda c: wail(c, VOICES['wraith'], j(c, 1.0, 1.4), rise=0.18, breath=0.5, wet=0.5) * 0.7)
recipe('wraith', 'aggro')(lambda c: wail(c, VOICES['wraith'], j(c, 0.9, 1.3), f0=380.0, rise=0.9, breath=0.35, wet=0.5))
recipe('wraith', 'attack')(lambda c: mixdown([whoosh(c, 0.4, 3000, 700, 1.2, [(0, 0), (0.15, 1), (1, 0)]), 0.5 * wail(c, VOICES['wraith'], 0.35, f0=500.0, rise=0.4, wet=0.3)]))
recipe('wraith', 'hurt')(lambda c: wail(c, VOICES['wraith'], j(c, 0.35, 0.5), f0=620.0, rise=0.5, vib=(9.0, 0.06), wet=0.4))
recipe('wraith', 'death')(lambda c: wail(c, VOICES['wraith'], j(c, 1.5, 1.8), f0=420.0, rise=0.5, fall=0.85, breath=0.5, wet=0.7))
recipe('wraith', 'step')(lambda c: footstep(c, 'hover', 1.0))
recipe('wraith', 'cast')(lambda c: choir(c, j(c, 1.1, 1.5), 330.0, [(0, 'o'), (0.5, 'a'), (1, 'u')], voices=3, detune=0.02, breath=0.3, tract=1.1, wet=0.7))


@recipe('wraith', 'taunt')
def _wraith_taunt(c):
    y = wail(c, VOICES['wraith'], j(c, 1.1, 1.5), f0=560.0, rise=0.8, vib=(6.0, 0.07), breath=0.4, wet=0.6)
    lg = laugh(c, VOICES['wraith'], count=5, spacing=0.14, burst=0.09, f0=520.0, rise=0.35, vow=('o', 'a'), tract=1.1, breath=0.6, rough=0.0, tail=False, inhale=False)
    return mixdown([y * 0.8, np.concatenate([np.zeros(c.n(0.5)), reverb(lg, c.sr, 2.0, 0.6)]) * 0.6])


# ---- celestial: choirs and bells ------------------------------------------------------------------------------------------------------------------------------------------------
vocal_cues('celestial')
recipe('celestial', 'ambient')(lambda c: choir(c, j(c, 1.0, 1.5), 392.0 * j(c, 0.9, 1.0), [(0, 'a'), (0.5, 'o'), (1, 'a')], voices=4, detune=0.008, breath=0.04, tract=1.05, wet=0.6) * 0.7)
recipe('celestial', 'aggro')(lambda c: choir(c, j(c, 0.7, 1.0), 440.0 * j(c, 0.95, 1.05), [(0, 'a'), (1, 'e')], voices=4, detune=0.01, breath=0.02, tract=1.0, wet=0.4, swell=False))
recipe('celestial', 'attack')(lambda c: mixdown([ring(c, 0.5, [880, 1318, 1760, 2637], [0.4, 0.3, 0.25, 0.2], [1, 0.7, 0.5, 0.3], 0.2), 0.4 * whoosh(c, 0.4, 900, 4500, 1.0)]))
recipe('celestial', 'hurt')(lambda c: ring(c, 0.5, [1568, 2093, 3136], [0.25, 0.18, 0.1], [1, 0.6, 0.3], 0.4))


@recipe('celestial', 'death')
def _cel_death(c):
    bells = ring(c, 1.5, [523, 659, 784, 1046, 1568], [0.9, 0.9, 0.8, 0.7, 0.5], [1, 0.8, 0.8, 0.6, 0.4], 0.05)
    voice = choir(c, 1.3, 392.0, [(0, 'a'), (1, 'o')], voices=3, wet=0.2, tail=0.2)
    return reverb(mixdown([bells, voice * 0.7]), c.sr, 2.5, 0.5, 0.0, 1.0)


recipe('celestial', 'step')(lambda c: mixdown([0.5 * ring(c, 0.3, [2093, 3136], [0.12, 0.08], [1, 0.5], 0.3), 0.3 * whoosh(c, 0.25, 800, 2500, 1.2)]))
recipe('celestial', 'cast')(lambda c: choir(c, j(c, 1.0, 1.4), 330.0 * j(c, 0.95, 1.1), [(0, 'o'), (0.4, 'a'), (1, 'e')], voices=4, detune=0.015, breath=0.04, tract=1.05, wet=0.6) * 0.9)


@recipe('celestial', 'taunt')
def _cel_taunt(c):
    out = np.zeros(c.n(0.05))
    f = 1568.0 * j(c, 0.95, 1.05)
    for i in range(int(c.rng.integers(5, 8))):
        k = 1.0 - 0.05 * i
        out = place(out, ring(c, 0.5, [f * k, f * 2.0 * k, f * 3.01 * k], [0.25, 0.2, 0.12], [1, 0.5, 0.25], 0.2), int(i * j(c, 0.13, 0.17) * c.sr), 1.0 - 0.07 * i)
    return reverb(out, c.sr, 1.8, 0.45)


# ---- skeleton: bones --------------------------------------------------------------------------------------------------------------------------------------------------------------
def bone_rattle(c, dur, count, tone=(900, 2400), level=1.0, accel=0.0, hollow=0.0):
    n = c.n(dur)
    out = np.zeros(n)
    for i in range(count):
        at = (i / max(1, count)) ** (1.0 + accel) * dur * 0.92 + c.rng.uniform(0, 0.01)
        out = place(out, bone_click(c, 0.05, c.rng.uniform(*tone)), int(at * c.sr), level * c.rng.uniform(0.4, 1.0))
    out = out[:n] if len(out) >= n else np.concatenate([out, np.zeros(n - len(out))])
    if hollow > 0:
        out = out + hollow * bp(white(c, n), 450.0, 1.5, c.sr) * adsr(c, n, 0.1, 0.2, 0.5, 0.3) * 0.25
    return out


recipe('skeleton', 'ambient')(lambda c: bone_rattle(c, j(c, 0.8, 1.3), int(c.rng.integers(5, 10)), (700, 1800), 0.8, 0.0, 1.0))


@recipe('skeleton', 'aggro')
def _skel_aggro(c):
    n = c.n(0.6)
    rattle = bone_rattle(c, j(c, 0.45, 0.7), int(c.rng.integers(12, 18)), (1000, 2600), 1.0, -0.4, 0.0)
    breath = vox(c, 0.5, [(0, 130), (0.4, 190), (1, 120)], [(0, 'a'), (1, 'o')], tract=0.95, breath=0.8, rough=0.3, voiced=0.35, amp=[(0, 0), (0.1, 1), (1, 0)], band=(300, 4500))
    return mixdown([rattle, breath * 0.4])


@recipe('skeleton', 'attack')
def _skel_attack(c):
    return mixdown([whoosh(c, 0.3, 500, 2500, 1.2, [(0, 0), (0.4, 1), (1, 0)]) * 0.5, np.concatenate([np.zeros(c.n(0.12)), bone_click(c, 0.1, 1200)]),
                    np.concatenate([np.zeros(c.n(0.14)), thud(c, 0.15, 150, 0.03, 0.2) * 0.5])])


recipe('skeleton', 'hurt')(lambda c: mixdown([bone_click(c, 0.12, j(c, 800, 1300)), bone_rattle(c, 0.3, 5, (900, 2000), 0.6, 0.5)]))


@recipe('skeleton', 'death')
def _skel_death(c):
    n = c.n(1.3)
    out = np.zeros(n)
    for i in range(26):
        at = (i / 26.0) ** 0.6 * 1.0
        out = place(out, bone_click(c, 0.06, c.rng.uniform(500, 2200) * (1.0 - 0.4 * (i / 26.0))), int(at * c.sr), c.rng.uniform(0.3, 1.0) * (1.0 - 0.3 * i / 26.0))
    return place(out, thud(c, 0.3, 80, 0.1, 0.3), int(0.95 * c.sr), 0.8)


recipe('skeleton', 'step')(lambda c: footstep(c, 'bone', 1.0))


@recipe('skeleton', 'cast')
def _skel_cast(c):
    out = bone_rattle(c, 1.3, 14, (1200, 2800), 0.7, 0.0, 0.0)
    out = place(out, whisper_phrase(c, 4, 400, 3500), int(0.1 * c.sr), 0.5)
    out = place(out, ring(c, 0.9, [660, 1330, 1990], [0.5, 0.35, 0.2], [1, 0.4, 0.2], 0.1), int(0.5 * c.sr), 0.45)
    return reverb(out, c.sr, 1.2, 0.3)


@recipe('skeleton', 'taunt')
def _skel_taunt(c):
    """The jaw chattering a laugh: groups of three clacks, "ha-ha-ha", over a dry hollow breath."""
    out = np.zeros(c.n(1.2))
    t0 = 0.0
    for _ in range(int(c.rng.integers(5, 8))):
        for k in range(3):
            out = place(out, bone_click(c, 0.05, c.rng.uniform(1100, 2200)), int((t0 + k * 0.035) * c.sr), 0.9 - 0.2 * k)
        t0 += j(c, 0.12, 0.16)
    return place(out, hiss(c, 0.9, 300, 3000, [(0, 0), (0.1, 1), (0.8, 0.6), (1, 0)]) * 0.15, 0)


# ---- living armour -----------------------------------------------------------------------------------------------------------------------------------------------------------------
def squeak(c, dur, f_lo, f_hi, level=0.4):
    n = c.n(dur)
    f = curve([(0, f_lo), (0.4, f_hi), (1, f_lo * 1.05)], n)
    ph = 2 * np.pi * np.cumsum(f) / c.sr
    y = (np.sin(ph) + 0.4 * np.sin(2 * ph)) * (0.5 + 0.5 * np.sin(2 * np.pi * 14 * np.arange(n) / c.sr)) * adsr(c, n, 0.05, 0.1, 0.7, 0.2)
    return norm(bp(y + 0.3 * white(c, n), (f_lo + f_hi) / 2, 3.0, c.sr)) * level


@recipe('armor', 'ambient')
def _armor_ambient(c):
    out = squeak(c, j(c, 0.5, 0.8), j(c, 250, 380), j(c, 420, 640), 0.45)
    for _ in range(2):
        out = place(out, metal_hit(c, 0.2, j(c, 800, 1600), 0.6, 2.5, 0.3), c.n(j(c, 0.1, 0.6)), 0.35)
    return out


@recipe('armor', 'aggro')
def _armor_aggro(c):
    clang = metal_hit(c, 0.6, j(c, 300, 420), 1.0, 1.0, 0.8)
    scrape = whoosh(c, 0.4, 600, 3500, 1.4) * 0.5
    helm = bp(vox(c, 0.5, [(0, 110), (0.4, 150), (1, 100)], [(0, 'a'), (1, 'o')], tract=0.9, breath=0.3, drive=0.6, amp=[(0, 0), (0.1, 1), (1, 0)]), 900, 1.2, c.sr) * 0.9
    out = clang
    out = place(out, scrape, c.n(0.2))
    return place(out, helm, c.n(0.1))


recipe('armor', 'attack')(lambda c: place(whoosh(c, 0.3, 400, 2200, 1.2) * 0.6, metal_hit(c, 0.4, j(c, 380, 700), 1.0, 1.2, 0.8), c.n(0.14)))
recipe('armor', 'hurt')(lambda c: mixdown([metal_hit(c, 0.45, j(c, 520, 980), 1.1, 1.0, 1.0), 0.3 * bp(white(c, c.n(0.45)), 2400, 2.0, c.sr) * expdecay(c, c.n(0.45), 0.05)]))


@recipe('armor', 'death')
def _armor_death(c):
    out = np.zeros(c.n(1.5))
    for i in range(14):
        at = (i / 14.0) ** 0.7
        out = place(out, metal_hit(c, 0.4, j(c, 300, 1500) * (1.0 - 0.3 * i / 14.0), 0.8, 1.4, 0.5), int(at * c.sr), c.rng.uniform(0.35, 1.0))
    return place(out, thud(c, 0.4, 55, 0.12, 0.3), c.n(1.0), 0.9)


recipe('armor', 'step')(lambda c: footstep(c, 'metal', 1.0))


@recipe('armor', 'cast')
def _armor_cast(c):
    n = c.n(1.5)
    bells = ring(c, 1.5, [330, 495, 660, 990, 1320], [0.9, 0.8, 0.7, 0.5, 0.4], [1, 0.8, 0.7, 0.4, 0.3], 0.1) * adsr(c, n, 0.5, 0.2, 0.7, 0.6)
    return reverb(mixdown([bells, 0.3 * squeak(c, 1.2, 300, 500)]), c.sr, 1.6, 0.4)


@recipe('armor', 'taunt')
def _armor_taunt(c):
    """A hollow laugh from inside a helm: three tin-can "ha"s and a shield scrape."""
    V = dict(f0=140.0, tract=0.85)
    y = laugh(c, V, count=3, spacing=0.22, burst=0.12, f0=140.0, rise=0.1, vow=('a', 'o'), tract=0.85, breath=0.15, tail=False, inhale=False, drive=0.5)
    y = bp(y, 900.0, 0.9, c.sr) * 1.5 + 0.5 * bp(y, 1900.0, 1.5, c.sr)
    y = reverb(y, c.sr, 0.7, 0.5)
    return place(y, whoosh(c, 0.35, 1200, 5000, 3.0), c.n(0.6), 0.4)


# ---- golem: stone ------------------------------------------------------------------------------------------------------------------------------------------------------------------
@recipe('golem', 'ambient')
def _golem_ambient(c):
    d = j(c, 1.0, 1.6)
    return stone_grind(c, d, 60, 420, 3.0) * adsr(c, c.n(d), 0.2, 0.1, 0.8, 0.3)


@recipe('golem', 'aggro')
def _golem_aggro(c):
    d = j(c, 0.8, 1.2)
    g = shaped(stone_grind(c, d, 50, 600, 8.0), [(0, 0.2), (0.4, 1), (1, 0)])
    return place(g, stone_crack(c, 0.3), c.n(0.05))


recipe('golem', 'attack')(lambda c: mixdown([whoosh(c, 0.3, 200, 900, 0.8) * 0.35, np.concatenate([np.zeros(c.n(0.15)), stone_crack(c, 0.3)]), np.concatenate([np.zeros(c.n(0.16)), thud(c, 0.4, 45, 0.15, 0.2)])]))
recipe('golem', 'hurt')(lambda c: mixdown([stone_crack(c, 0.3), 0.5 * hp(lp(white(c, c.n(0.3)), 5000, c.sr, 1), 1500, c.sr, 1) * expdecay(c, c.n(0.3), 0.06)]))


@recipe('golem', 'death')
def _golem_death(c):
    out = shaped(stone_grind(c, 1.6, 40, 500, 6.0), [(0, 0.5), (0.3, 1), (1, 0)])
    for i in range(18):
        out = place(out, stone_crack(c, 0.15, c.rng.uniform(0.2, 0.7)), int((i / 18.0) ** 0.8 * 1.4 * c.sr))
    return place(out, thud(c, 0.5, 40, 0.2, 0.2), c.n(0.2), 1.0)


recipe('golem', 'step')(lambda c: footstep(c, 'stone', 1.0))
recipe('golem', 'cast')(lambda c: mixdown([drone(c, 1.6, 54.0, 1.0, 0.2), drone(c, 1.6, 81.0, 1.0, 0.3) * 0.6, 0.3 * bp(white(c, c.n(1.6)), 700, 4.0, c.sr) * adsr(c, c.n(1.6), 0.5, 0.2, 0.6, 0.5)]))


@recipe('golem', 'taunt')
def _golem_taunt(c):
    """Stone grating in the rhythm of a laugh: three heavy grinds."""
    out = np.zeros(c.n(1.1))
    for i in range(3):
        out = place(out, stone_grind(c, 0.25, 70, 700, 12.0), int(i * 0.3 * c.sr), 0.9 - 0.15 * i)
        out = place(out, thud(c, 0.2, 55, 0.08, 0.2), int(i * 0.3 * c.sr), 0.5)
    return out


# ---- dragon and drake ---------------------------------------------------------------------------------------------------------------------------------------------------------------
@recipe('dragon', 'ambient')
def _dragon_ambient(c):
    d = j(c, 1.4, 2.0)
    growl_ = roar(c, VOICES['dragon'], d, f0=48.0, rise=0.1, noise=0.3, sub=0.5, drive=0.3) * 0.6
    breath = lp(pink(c, c.n(d)), 500, c.sr, 2) * shaped(np.ones(c.n(d)), [(0, 0), (0.3, 0.6), (0.5, 0.2), (0.7, 0.6), (1, 0)]) * 0.5
    return mixdown([growl_, breath])


recipe('dragon', 'aggro')(lambda c: reverb(roar(c, VOICES['dragon'], j(c, 1.5, 2.0), f0=62.0 * j(c, 0.95, 1.1), rise=0.5, noise=0.5), c.sr, 1.2, 0.25, 0.0, 0.7))
recipe('dragon', 'attack')(lambda c: mixdown([roar(c, VOICES['dragon'], 0.4, f0=90.0, rise=0.3, noise=0.6, pts=[(0, 70), (0.2, 110), (1, 60)]), np.concatenate([np.zeros(c.n(0.05)), bone_click(c, 0.1, 700)]) * 0.7,
                                                whoosh(c, 0.35, 300, 1800, 1.0) * 0.4]))
recipe('dragon', 'hurt')(lambda c: roar(c, VOICES['dragon'], j(c, 0.5, 0.75), f0=85.0, rise=0.6, noise=0.5, pts=[(0, 70), (0.12, 150), (1, 75)]))
recipe('dragon', 'death')(lambda c: reverb(roar(c, VOICES['dragon'], j(c, 1.8, 2.2), f0=70.0, noise=0.6, pts=[(0, 80), (0.15, 110), (0.6, 60), (1, 28)], vseq=[(0, 'a'), (0.5, 'o'), (1, 'u')]), c.sr, 1.6, 0.35, 0.0, 0.8))


@recipe('dragon', 'step')
def _dragon_step(c):
    n = c.n(0.45)
    return mixdown([footstep(c, 'stone', 0.7), 0.4 * lp(brown(c, n), 220, c.sr, 2) * expdecay(c, n, 0.2)])


@recipe('dragon', 'cast')
def _dragon_cast(c):
    """The breath: a long indrawn hiss, a pause, then the roaring gout."""
    out = np.zeros(c.n(1.8))
    out = place(out, whoosh(c, 0.7, 400, 2500, 1.0, [(0, 0), (1, 1)]) * 0.5, 0)
    gout = mixdown([whoosh(c, 1.0, 600, 400, 0.6, [(0, 0), (0.08, 1), (0.6, 0.8), (1, 0)]) * 0.9, roar(c, VOICES['dragon'], 1.0, f0=56.0, noise=0.5) * 0.6])
    return place(out, gout, c.n(0.75))


@recipe('dragon', 'taunt')
def _dragon_taunt(c):
    """A low, rolling chuckle: "hrrh... hrrh... hrrh" from deep in the chest."""
    out = np.zeros(c.n(0.1))
    t0 = 0.0
    for _ in range(int(c.rng.integers(3, 5))):
        y = vox(c, j(c, 0.28, 0.38), [(0, 50), (0.3, 66), (1, 44)], [(0, 'uh'), (1, 'o')], tract=0.55, tilt=1.0, breath=0.5, rough=0.7, rough_hz=j(c, 30.0, 40.0), sub=0.8, drive=0.8,
                amp=[(0, 0), (0.1, 1), (0.7, 0.7), (1, 0)], bwmul=1.6)
        out = place(out, y, int(t0 * c.sr), 0.9)
        t0 += j(c, 0.36, 0.46)
    return reverb(out, c.sr, 1.4, 0.3)


vocal_cues('drake')


@recipe('drake', 'ambient')
def _drake_ambient(c):
    h = hiss(c, j(c, 0.8, 1.2), 1500, 6000, [(0, 0), (0.3, 0.8), (0.6, 0.3), (1, 0)]) * 0.35
    v = vox(c, 0.7, [(0, 190), (0.5, 230), (1, 180)], [(0, 'a'), (1, 'ae')], tract=0.9, breath=0.6, rough=0.5, rough_hz=55.0, amp=[(0, 0), (0.3, 0.6), (1, 0)])
    return place(h, v * 0.5, c.n(0.1))


recipe('drake', 'aggro')(lambda c: screech(c, 520.0 * j(c, 0.95, 1.1), j(c, 0.7, 1.1), 0.7, 0.4, 0.9, 0.9, 0.5))
recipe('drake', 'attack')(lambda c: mixdown([screech(c, 440.0, 0.25, 0.3, 0.4, 0.9, 0.8), bone_click(c, 0.1, 900) * 0.7]))
recipe('drake', 'hurt')(lambda c: screech(c, 640.0 * j(c, 0.95, 1.1), j(c, 0.25, 0.4), 0.45, 0.25, 0.9, 0.5))
recipe('drake', 'death')(lambda c: reverb(vox(c, j(c, 1.4, 2.0), [(0, 600), (0.15, 720), (1, 110)], [(0, 'a'), (0.6, 'o'), (1, 'u')], tract=0.9, tilt=1.1, breath=0.5, rough=0.45, rough_hz=50.0, vib=(7.0, 0.04),
                                           drive=0.6, amp=[(0, 0), (0.08, 1), (0.6, 0.7), (1, 0)]), c.sr, 1.2, 0.3))
recipe('drake', 'step')(lambda c: footstep(c, 'claw', 0.8))


@recipe('drake', 'cast')
def _drake_cast(c):
    out = np.zeros(c.n(1.5))
    out = place(out, hiss(c, 0.5, 1500, 7000, [(0, 0), (1, 1)]) * 0.4, 0)
    return place(out, whoosh(c, 0.9, 900, 500, 0.6) * 0.8, c.n(0.5))


@recipe('drake', 'taunt')
def _drake_taunt(c):
    V = VOICES['drake']
    y = laugh(c, V, count=int(c.rng.integers(5, 8)), spacing=j(c, 0.1, 0.13), burst=j(c, 0.07, 0.09), f0=300.0, rise=0.3, vow=('a', 'ae'), tract=0.9, breath=0.5, rough=0.45, drive=0.6, tail=True,
              inhale=False, glide=1.3)
    return hp(y, 300.0, c.sr, 2)


# ---- slime ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@recipe('slime', 'ambient')
def _slime_ambient(c):
    out = np.zeros(c.n(1.2))
    for _ in range(int(c.rng.integers(3, 6))):
        up = c.rng.random() < 0.6
        a, b = j(c, 130, 260), j(c, 320, 620)
        out = place(out, blorp(c, j(c, 0.08, 0.18), a if up else b, b if up else a, 0.8), int(c.rng.uniform(0, 0.9) * c.sr), j(c, 0.4, 0.9))
    return place(out, squelch(c, 0.5, 200, 700, 3.0) * 0.25, int(c.rng.uniform(0.1, 0.6) * c.sr))


recipe('slime', 'aggro')(lambda c: mixdown([squelch(c, j(c, 0.5, 0.8), 250, 1400, 5.0), blorp(c, 0.25, 160, 720, 0.6)]))
recipe('slime', 'attack')(lambda c: mixdown([squelch(c, 0.22, 300, 900, 3.0), blorp(c, 0.12, 400, 120, 0.9)]))
recipe('slime', 'hurt')(lambda c: mixdown([blorp(c, j(c, 0.2, 0.3), 420, 1100, 1.0), squelch(c, 0.3, 500, 1500, 5.0) * 0.4]))


@recipe('slime', 'death')
def _slime_death(c):
    out = np.zeros(c.n(1.0))
    splat = mixdown([squelch(c, 0.4, 200, 1000, 2.5), blorp(c, 0.3, 380, 90, 0.9), 0.5 * lp(white(c, c.n(0.35)), 1800, c.sr, 1) * expdecay(c, c.n(0.35), 0.1)])
    out = place(out, splat, 0)
    for i in range(int(c.rng.integers(4, 8))):
        out = place(out, blorp(c, 0.07, j(c, 250, 480), j(c, 130, 240), 0.5), int((0.4 + i * j(c, 0.07, 0.11)) * c.sr), 0.6)
    return out


recipe('slime', 'step')(lambda c: footstep(c, 'wet', 1.0))


@recipe('slime', 'cast')
def _slime_cast(c):
    out = np.zeros(c.n(1.1))
    for i in range(6):
        out = place(out, blorp(c, 0.12, 170 + 60 * i, 340 + 90 * i, 0.8), int(i * 0.14 * c.sr), 0.9)
    return place(out, squelch(c, 0.9, 180, 1400, 4.0) * 0.4, 0)


@recipe('slime', 'taunt')
def _slime_taunt(c):
    out = np.zeros(c.n(1.0))
    for i in range(int(c.rng.integers(6, 10))):
        out = place(out, blorp(c, 0.07, 280 + 28 * i, 560 + 40 * i, 0.9), int(i * j(c, 0.08, 0.1) * c.sr), 0.8)
    return place(out, squelch(c, 0.5, 300, 1200, 4.0) * 0.3, c.n(0.5))


# ---- insect -------------------------------------------------------------------------------------------------------------------------------------------------------------------------
recipe('insect', 'ambient')(lambda c: mixdown([chitter_train(c, j(c, 0.8, 1.3), 22.0, (3000, 6000), level=0.7), hiss(c, 0.6, 5000, 9000, [(0, 0), (0.5, 1), (1, 0)]) * 0.1]))


@recipe('insect', 'aggro')
def _insect_aggro(c):
    d = j(c, 0.5, 0.8)
    n = c.n(d)
    h = hiss(c, d, 2500, 9000, [(0, 0), (0.1, 1), (0.7, 0.9), (1, 0)]) * 0.8
    h = h * (0.55 + 0.45 * np.sin(2 * np.pi * 45.0 * np.arange(len(h)) / c.sr))
    return mixdown([h, chitter_train(c, d, 50.0, (2500, 5500), accel=0.3)])


recipe('insect', 'attack')(lambda c: mixdown([bone_click(c, 0.1, j(c, 1400, 2200)), np.concatenate([np.zeros(c.n(0.02)), hiss(c, 0.2, 3000, 8000, [(0, 1), (1, 0)]) * 0.5])]))
recipe('insect', 'hurt')(lambda c: vox(c, j(c, 0.22, 0.34), [(0, 1800), (0.15, 3100), (1, 1700)], [(0, 'i'), (1, 'ae')], tract=1.6, tilt=0.9, breath=0.5, rough=0.5, rough_hz=70.0, drive=0.3, amp=[(0, 0), (0.05, 1), (1, 0)]))


@recipe('insect', 'death')
def _insect_death(c):
    out = np.zeros(c.n(1.0))
    out = place(out, mixdown([hiss(c, 0.15, 800, 5000, [(0, 1), (1, 0)]), bone_click(c, 0.12, 500), thud(c, 0.2, 140, 0.04, 0.4) * 0.6]), 0)
    return place(out, chitter_train(c, 0.7, 18.0, (3000, 5000), accel=-0.7, level=0.5), c.n(0.15))


recipe('insect', 'step')(lambda c: footstep(c, 'chitin', 1.0))


@recipe('insect', 'cast')
def _insect_cast(c):
    d = j(c, 1.0, 1.4)
    return mixdown([buzz(c, d, 170, 260, 38.0), 0.3 * chitter_train(c, d, 30.0, (3500, 6000))])


@recipe('insect', 'taunt')
def _insect_taunt(c):
    out = np.zeros(c.n(1.1))
    t = 0.0
    gap = 0.17
    for _ in range(int(c.rng.integers(6, 9))):
        for k in range(2):
            out = place(out, bone_click(c, 0.04, j(c, 2000, 3800)), int((t + 0.03 * k) * c.sr), 0.9)
        t += gap
        gap *= 0.9
    return out


# ---- elemental -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
def fire_bed(c, dur, level=1.0, hum=90.0):
    n = c.n(dur)
    y = lp(pink(c, n), 1800, c.sr, 1) * 0.4 * (0.6 + 0.4 * np.clip(wander(c, n, 7.0) + 0.5, 0, 1))
    y = y + 0.5 * crackles(c, dur, 70.0, (1800, 7000), 0.003)[:n]
    y = y + 0.35 * np.sin(2 * np.pi * hum * np.arange(n) / c.sr) * (0.6 + 0.4 * wander(c, n, 3.0))
    return y * level


@recipe('elemental', 'ambient')
def _elem_ambient(c):
    d = j(c, 1.2, 1.8)
    return fire_bed(c, d, 1.0, 80.0) * adsr(c, c.n(d), 0.2, 0.1, 0.8, 0.4)


recipe('elemental', 'aggro')(lambda c: mixdown([whoosh(c, 0.85, 300, 2400, 0.9, [(0, 0), (0.5, 1), (1, 0)]), fire_bed(c, 0.9, 0.6, 70.0)]))
recipe('elemental', 'attack')(lambda c: mixdown([whoosh(c, 0.3, 500, 3500, 1.0, [(0, 0), (0.2, 1), (1, 0)]), crackles(c, 0.35, 150.0, (2000, 7000), 0.003) * 0.8]))
recipe('elemental', 'hurt')(lambda c: mixdown([crackles(c, 0.3, 200.0, (1500, 6000), 0.004), hiss(c, 0.3, 2000, 7000, [(0, 1), (1, 0)]) * 0.5]))
recipe('elemental', 'death')(lambda c: mixdown([shaped(fire_bed(c, 1.5, 1.0, 70.0), [(0, 1), (1, 0)]), whoosh(c, 1.4, 1500, 200, 0.8, [(0, 0.5), (0.2, 1), (1, 0)]) * 0.6]))
recipe('elemental', 'step')(lambda c: mixdown([thud(c, 0.2, 75, 0.05, 0.1), crackles(c, 0.2, 90.0, (2000, 6000), 0.003) * 0.6]))
recipe('elemental', 'cast')(lambda c: mixdown([buzz(c, 1.5, 200, 900, 6.0, 0.3), whoosh(c, 1.5, 400, 3500, 0.9, [(0, 0), (0.7, 1), (0.85, 1), (1, 0)]) * 0.8, fire_bed(c, 1.5, 0.5)]))


@recipe('elemental', 'taunt')
def _elem_taunt(c):
    out = np.zeros(c.n(1.2))
    for i in range(int(c.rng.integers(4, 7))):
        n = c.n(0.12)
        out = place(out, mixdown([crackles(c, 0.12, 300.0, (1500, 5000), 0.003), lp(white(c, n), 1200, c.sr, 2) * expdecay(c, n, 0.04) * 0.6]), int(i * j(c, 0.15, 0.19) * c.sr), 1.0)
    return place(out, fire_bed(c, 0.6, 0.5), 0)


# ---- lich ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
recipe('lich', 'ambient')(lambda c: lich_voice(c, j(c, 1.2, 1.5), int(c.rng.integers(3, 5)), 1.0, tail=0.9) * 0.8)
recipe('lich', 'aggro')(lambda c: reverb(mixdown([whoosh(c, 0.8, 1200, 4500, 1.0, [(0, 0), (0.6, 1), (1, 0)]) * 0.5, whisper_phrase(c, 3, 600, 6500, 0.04, (0.12, 0.2), ('i', 'e', 'a'))]), c.sr, 2.0, 0.5, 0.02))
recipe('lich', 'attack')(lambda c: reverb(mixdown([hiss(c, 0.15, 2500, 9000, [(0, 1), (1, 0)]) * 0.8, ring(c, 0.5, [1480, 2217, 2960], [0.3, 0.2, 0.12], [1, 0.4, 0.2], 0.3) * 0.6]), c.sr, 1.8, 0.45))
recipe('lich', 'hurt')(lambda c: reverb(mixdown([whisper_syllable(c, 0.22, [(0, 'a'), (1, 'e')], 500, 6000), np.concatenate([np.zeros(c.n(0.03)), 0.5 * whisper_syllable(c, 0.25, [(0, 'e'), (1, 'i')], 800, 7000)])]), c.sr, 1.6, 0.5))
recipe('lich', 'death')(lambda c: lich_voice(c, j(c, 1.2, 1.5), int(c.rng.integers(3, 5)), 0.8, taps=2, tail=1.1) * 0.9)
recipe('lich', 'step')(lambda c: reverb(mixdown([footstep(c, 'robe', 1.0), 0.4 * bone_click(c, 0.05, 700)]), c.sr, 1.4, 0.4))


@recipe('lich', 'cast')
def _lich_cast(c):
    v = lich_voice(c, 1.2, 4, 1.1, taps=1, tail=0.9)
    ch = choir(c, 1.3, 110.0, [(0, 'o'), (1, 'u')], voices=2, breath=0.4, wet=0.2) * 0.3
    rg = ring(c, 1.4, [220, 330, 440], [0.8, 0.7, 0.6], [1, 0.5, 0.3], 0.0) * 0.2
    return mixdown([v, ch, rg])


@recipe('lich', 'taunt')
def _lich_taunt(c):
    """A whispered laugh: breathy "hh-hh-hh" bursts, doubled an octave down, thrown into a long hall."""
    out = np.zeros(c.n(1.2))
    t0 = 0.05
    for _ in range(int(c.rng.integers(4, 6))):
        L = j(c, 0.09, 0.13)
        y = whisper_syllable(c, L, [(0, 'a'), (1, 'e')], 500, 5500, 1.0)
        out = place(out, y, int(t0 * c.sr), 0.8)
        out = place(out, resample(y, 0.7), int((t0 + 0.02) * c.sr), 0.45)
        t0 += L + j(c, 0.07, 0.11)
    out = place(out, drone(c, 1.5, 55.0, 1.0, 0.3), 0, 0.12)
    out = echo(out, c.sr, 0.3, 0.5, 2, 0.6)
    return reverb(out, c.sr, 2.6, 0.55, 0.03, 1.0)


# ======================================================================================================================================================================
# rendering
# ======================================================================================================================================================================
def seed_for(family, cue, index):
    return zlib.crc32(('nusmp-mob-voice:%s.%s.%d' % (family, cue, index)).encode('utf-8')) & 0xFFFFFFFF


def finalize(x, sr, cue):
    """Trim, fade, filter out the infrasonic rumble, match the loudness of the cue, and keep the peak under -1 dBFS without clipping."""
    x = np.nan_to_num(np.asarray(x, dtype=np.float64))
    x = x - float(np.mean(x))
    x = hp(x, 28.0, sr, 1)
    pk = float(np.max(np.abs(x))) if len(x) else 0.0
    if pk < 1e-6:
        raise ValueError('silent sound')
    idx = np.nonzero(np.abs(x) > pk * 10 ** (-50.0 / 20.0))[0]
    lo = max(0, int(idx[0]) - int(0.004 * sr))
    hi = min(len(x), int(idx[-1]) + 1 + int(0.02 * sr))
    x = x[lo:hi]
    limit = int(MAX_SECONDS * sr)
    if len(x) > limit:
        x = x[:limit]
    fo = min(len(x) // 3, int((0.06 if len(x) > 1.2 * sr else 0.025) * sr))
    fi = min(len(x) // 4, int(0.003 * sr))
    if fi > 0:
        x[:fi] *= np.linspace(0.0, 1.0, fi)
    if fo > 0:
        x[-fo:] *= np.linspace(1.0, 0.0, fo) ** 1.5
    # loudness of the active part (frames of 10 ms louder than -45 dB of the peak)
    frame = max(1, int(0.01 * sr))
    nfr = len(x) // frame
    if nfr >= 1:
        ms = (x[:nfr * frame].reshape(nfr, frame) ** 2).mean(axis=1)
        thr = (np.max(np.abs(x)) ** 2) * 10 ** (-45.0 / 10.0)
        act = ms[ms > thr]
        rms = math.sqrt(float(act.mean())) if len(act) else math.sqrt(float(ms.mean()))
    else:
        rms = math.sqrt(float((x ** 2).mean()))
    x = x * (10 ** (LOUDNESS[cue] / 20.0) / max(rms, 1e-6))
    knee = 0.6
    span = PEAK_LIMIT - knee
    a = np.abs(x)
    over = a > knee
    x = np.where(over, np.sign(x) * (knee + span * np.tanh((a - knee) / span)), x)
    mn = int(0.3 * sr)
    if len(x) < mn:
        x = np.concatenate([x, np.zeros(mn - len(x))])
    return x.astype(np.float64)


def render(family, cue, index):
    label, sr, _ = FAMILIES[family]
    c = Ctx(sr, np.random.default_rng(seed_for(family, cue, index)))
    fn = RECIPES[(family, cue)]
    return finalize(fn(c), sr, cue), sr


def write_wav(path, x, sr):
    pcm = np.clip(np.round(x * 32767.0), -32768, 32767).astype('<i2')
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())


def encode(wav_path, ogg_path, sr, quality):
    os.makedirs(os.path.dirname(ogg_path), exist_ok=True)
    cmd = ['ffmpeg', '-y', '-loglevel', 'error', '-i', wav_path, '-ac', '1', '-ar', str(sr), '-c:a', 'libvorbis', '-q:a', str(quality), '-map_metadata', '-1', '-fflags', '+bitexact', '-flags:a', '+bitexact', ogg_path]
    subprocess.run(cmd, check=True)


def spectrogram_png(x, sr, path):
    try:
        from PIL import Image
    except ImportError:
        return
    n_fft, hop = 1024, 256
    win = np.hanning(n_fft)
    frames = max(1, (len(x) - n_fft) // hop + 1)
    cols = []
    for k in range(frames):
        seg = x[k * hop:k * hop + n_fft]
        if len(seg) < n_fft:
            seg = np.pad(seg, (0, n_fft - len(seg)))
        cols.append(np.abs(np.fft.rfft(seg * win)))
    s = 20 * np.log10(np.array(cols).T + 1e-7)
    s = np.clip((s - (s.max() - 80.0)) / 80.0, 0, 1)
    img = (np.flipud(s) * 255).astype(np.uint8)
    Image.fromarray(img).resize((min(1000, img.shape[1] * 2), 256)).save(path)


def verify_recipes():
    """Every family needs a recipe for every cue; a hole is reported before anything is written."""
    missing = ['%s.%s' % (f, c) for f in FAMILIES for c in CUES if (f, c) not in RECIPES]
    extra = sorted(set(f for f, _ in RECIPES) - set(FAMILIES))
    if missing or extra:
        raise SystemExit('recipes missing: %s; recipes for unknown families: %s' % (missing, extra))


def wanted(only):
    out = []
    for fam in FAMILIES:
        for cue in CUES:
            if only and not (only == fam or only == fam + '.' + cue or only == cue):
                continue
            out.append((fam, cue))
    return out


def file_name(family, cue, index):
    return 'mob/%s/%s_%d' % (family, cue, index)


def generate(only=None, preview=None, quality=3):
    report = {}
    with tempfile.TemporaryDirectory() as tmp:
        for fam, cue in wanted(only):
            for i in range(1, VARIANTS[cue] + 1):
                x, sr = render(fam, cue, i)
                wav = os.path.join(tmp, '%s_%s_%d.wav' % (fam, cue, i))
                write_wav(wav, x, sr)
                ogg = os.path.join(SOUND_DIR, fam, '%s_%d.ogg' % (cue, i))
                encode(wav, ogg, sr, quality)
                report[(fam, cue, i)] = (len(x) / sr, os.path.getsize(ogg))
                if preview:
                    os.makedirs(preview, exist_ok=True)
                    write_wav(os.path.join(preview, '%s_%s_%d.wav' % (fam, cue, i)), x, sr)
                    spectrogram_png(x, sr, os.path.join(preview, '%s_%s_%d.png' % (fam, cue, i)))
            print('%-10s %-8s %s' % (fam, cue, ' '.join('%.2fs/%dB' % report[(fam, cue, i)] for i in range(1, VARIANTS[cue] + 1))), flush=True)
    return report


def ogg_seconds(path):
    """Duration of an Ogg Vorbis file from its last page's granule position and the sample rate in its identification header."""
    with open(path, 'rb') as f:
        data = f.read()
    if data[:4] != b'OggS':
        raise ValueError('not an Ogg file')
    at = data.find(b'\x01vorbis')
    if at < 0:
        raise ValueError('no Vorbis identification header')
    rate = struct.unpack('<I', data[at + 12:at + 16])[0]
    last = data.rfind(b'OggS')
    granule = struct.unpack('<q', data[last + 6:last + 14])[0]
    return granule / float(rate)


def manifest():
    """The mob entries of sounds.json, the subtitles, and the measured length of every file."""
    entries = {}
    lang = {}
    sizes = 0
    for fam in FAMILIES:
        label, sr, verbs = FAMILIES[fam]
        for ci, cue in enumerate(CUES):
            key = 'mob.%s.%s' % (fam, cue)
            sub = 'subtitles.nusmp.mob.%s.%s' % (fam, cue)
            lang[sub] = '%s %s' % (label, verbs[ci])
            sounds = []
            for i in range(1, VARIANTS[cue] + 1):
                path = os.path.join(SOUND_DIR, fam, '%s_%d.ogg' % (cue, i))
                if not os.path.exists(path):
                    raise SystemExit('missing %s (run the generator first)' % path)
                sizes += os.path.getsize(path)
                s = {'name': 'nusmp:' + file_name(fam, cue, i)}
                if ogg_seconds(path) >= STREAM_OVER:
                    s['stream'] = True
                if fam in ATTENUATION and cue in LOUD_CUES:
                    s['attenuation_distance'] = ATTENUATION[fam]
                sounds.append(s)
            entry = {'subtitle': sub, 'sounds': sounds}
            entries[key] = entry
    return entries, lang, sizes


def _sound_text(snd):
    parts = ['"name": %s' % json.dumps(snd['name'])]
    for k, v in snd.items():
        if k != 'name':
            parts.append('%s: %s' % (json.dumps(k), json.dumps(v)))
    return '{ ' + ', '.join(parts) + ' }'


def _entry_text(key, entry):
    """One sounds.json entry in the file's own style (the Zagred entries come out byte for byte as they were written by hand)."""
    lines = ['  %s: {' % json.dumps(key)]
    extra = [k for k in entry if k not in ('subtitle', 'sounds')]
    if 'subtitle' in entry:
        lines.append('    "subtitle": %s,' % json.dumps(entry['subtitle']))
    for k in extra:
        lines.append('    %s: %s,' % (json.dumps(k), json.dumps(entry[k])))
    sounds = entry.get('sounds', [])
    sounds = [x if isinstance(x, dict) else {'name': x} for x in sounds]
    if len(sounds) == 1:
        lines.append('    "sounds": [%s]' % _sound_text(sounds[0]))
    else:
        lines.append('    "sounds": [')
        lines.extend('      %s%s' % (_sound_text(x), ',' if i < len(sounds) - 1 else '') for i, x in enumerate(sounds))
        lines.append('    ]')
    lines.append('  }')
    return '\n'.join(lines)


def write_manifest():
    entries, lang, sizes = manifest()
    try:
        with open(SOUNDS_JSON, encoding='utf-8') as f:
            doc = json.load(f)
    except FileNotFoundError:
        doc = {}
    doc = {k: v for k, v in doc.items() if not k.startswith('mob.')}      # every entry that is not a mob voice stays, in its place and its form
    doc.update(entries)
    text = '{\n' + ',\n'.join(_entry_text(k, v) for k, v in doc.items()) + '\n}\n'
    json.loads(text)                                                        # never write a file that does not parse
    with open(SOUNDS_JSON, 'w', encoding='utf-8') as f:
        f.write(text)
    with open(LANG_OUT, 'w', encoding='utf-8') as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('sounds.json: %d mob events, %d files, %.2f MB; %d subtitles' % (len(entries), sum(len(e['sounds']) for e in entries.values()), sizes / 1048576.0, len(lang)))


def check():
    with open(SOUNDS_JSON, encoding='utf-8') as f:
        doc = json.load(f)
    bad = []
    n = 0
    total = 0
    for fam in FAMILIES:
        for cue in CUES:
            key = 'mob.%s.%s' % (fam, cue)
            if key not in doc:
                bad.append('no entry ' + key)
                continue
            for s in doc[key]['sounds']:
                rel = s['name'].split(':', 1)[1]
                path = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp', 'sounds', rel + '.ogg')
                if not os.path.exists(path):
                    bad.append('missing ' + path)
                    continue
                try:
                    sec = ogg_seconds(path)
                except Exception as e:
                    bad.append('%s: %s' % (path, e))
                    continue
                if not (0.29 <= sec <= 3.3):
                    bad.append('%s is %.2f s' % (path, sec))
                n += 1
                total += os.path.getsize(path)
    print('%d files, %.2f MB, %d problems' % (n, total / 1048576.0, len(bad)))
    for b in bad[:30]:
        print('  ' + b)
    return 1 if bad else 0


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--only', help='one family (goblin), one cue of a family (goblin.taunt) or one cue of all (hurt)')
    ap.add_argument('--preview', help='also write a .wav and a spectrogram .png of every file into this directory')
    ap.add_argument('--quality', type=float, default=3.0, help='Vorbis quality (-1..10), default 3')
    ap.add_argument('--check', action='store_true', help='only verify the files sounds.json names')
    ap.add_argument('--no-manifest', action='store_true', help='do not touch sounds.json and the lang fragment')
    ap.add_argument('--manifest-only', action='store_true', help='rewrite sounds.json and the lang fragment from the files that are there, without synthesising anything')
    args = ap.parse_args()
    verify_recipes()
    if args.check:
        return check()
    if args.manifest_only:
        write_manifest()
        return 0
    if args.only is not None and not wanted(args.only):
        raise SystemExit('nothing matches --only ' + args.only)
    generate(args.only, args.preview, args.quality)
    if not args.no_manifest and args.only is None:
        write_manifest()
    return 0


if __name__ == '__main__':
    sys.exit(main())
