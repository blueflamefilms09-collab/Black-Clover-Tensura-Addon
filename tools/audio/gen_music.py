#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the themed dungeon music (release 1.107.0, "themed music engine").

There are no OST files in this project, so every track is composed and rendered here from nothing: numpy only (no scipy, no samples), then ffmpeg encodes mono Vorbis.
Everything is deterministic: each track is seeded from its own name, so a re-run writes the same audio (the Ogg bytes are bit-exact for one ffmpeg build).

The set is 25 loops of 60 to 90 seconds:
  * ten FAMILIES of themes (see FAMILIES; the Java side maps the 27 Region Themes onto them, audio/music/MusicCatalog.java), each with an EXPLORE loop (slow, sparse, ambient)
    and a BATTLE loop (same key and motif, faster, drums and an ostinato), so a fight growing out of a quiet floor stays in the same musical world;
  * two BOSS loops (the Holy road and the Abyssal road), a SAFE-zone loop, the HUB loop and the RIFT loop.
Every family has its own tonic, scale, tempo, instruments and texture, which is what makes them sound different, see the FAMILIES table (python3 tools/audio/gen_music.py --list).

How a loop is made seamless: every note is added into a circular buffer (a note that runs past the end continues at the start), every slow movement uses a whole number of
cycles per loop, and the reverb is a circular convolution, so the last sample runs straight into the first one.

  python3 tools/audio/gen_music.py                  write every track, then sounds.json (only the "music." entries; all others are kept as they are)
  python3 tools/audio/gen_music.py --only stone     one family (stone) / one track (stone_battle, boss_holy, safe, hub, rift)
  python3 tools/audio/gen_music.py --preview DIR    also write a .wav of each track into DIR
  python3 tools/audio/gen_music.py --list           the table of keys, scales, tempos and lengths
  python3 tools/audio/gen_music.py --check          verify files and sounds.json (count, length, mono, total size, entries)
  python3 tools/audio/gen_music.py --manifest-only  rewrite the "music." entries of sounds.json from the files that are there

To use real music instead: see docs/audio/themed_music.md (same file names in a resource pack, no code change).
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

SR = 22050
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
MUSIC_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp', 'sounds', 'music')
SOUNDS_JSON = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp', 'sounds.json')
BUDGET_BYTES = 25 * 1024 * 1024
MIN_SECONDS, MAX_SECONDS = 60.0, 90.0
TARGET_SECONDS = 78.0

SCALES = {
    'dorian': [0, 2, 3, 5, 7, 9, 10], 'phrygian': [0, 1, 3, 5, 7, 8, 10], 'phrygian_dom': [0, 1, 4, 5, 7, 8, 10],
    'lydian': [0, 2, 4, 6, 7, 9, 11], 'mixolydian': [0, 2, 4, 5, 7, 9, 10], 'ionian': [0, 2, 4, 5, 7, 9, 11],
    'aeolian': [0, 2, 3, 5, 7, 8, 10], 'harm_minor': [0, 2, 3, 5, 7, 8, 11], 'wholetone': [0, 2, 4, 6, 8, 10],
    'pent_major': [0, 2, 4, 7, 9], 'locrian_nat2': [0, 2, 3, 5, 6, 8, 10],
}
NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']

# --------------------------------------------------------------------------- the catalogue (the Java side mirrors the ids: audio/music/MusicCatalog.java)
# root: tonic as a MIDI note in the bass octave; prog: one scale degree per chord (8 chords, repeated); chord_bars: bars one chord lasts in the explore loop;
# pad / lead / texture / kit: instrument names below; arp: the battle loop arpeggiates; motif_start: first scale degree of the theme.
FAMILIES = {
    'stone': dict(label='Stone and weight', root=38, scale='dorian', bpm=(70, 112), prog=[0, 0, 3, 0, 6, 6, 4, 3], chord_bars=2, pad='pad_dark', lead='horn',
                  texture='rumble', kit='taiko', arp=False, motif_start=0, rt60=3.6, wet=0.34, bass='sawbass', stab='strings'),
    'storm': dict(label='Copper and lightning', root=40, scale='mixolydian', bpm=(88, 140), prog=[0, 6, 3, 0, 0, 6, 4, 3], chord_bars=1, pad='pad_bright', lead='pluck',
                  texture='storm', kit='driving', arp=True, motif_start=2, rt60=2.4, wet=0.28, bass='sawbass', stab='strings'),
    'blood': dict(label='Blood and bone', root=33, scale='phrygian_dom', bpm=(58, 100), prog=[0, 1, 0, 1, 3, 0, 5, 1], chord_bars=2, pad='strings', lead='cello',
                  texture='heartbeat', kit='tribal', arp=False, motif_start=2, rt60=4.2, wet=0.36, bass='sub', stab='strings'),
    'flame': dict(label='Forge and fire', root=40, scale='phrygian', bpm=(84, 132), prog=[0, 1, 0, 6, 0, 1, 3, 6], chord_bars=1, pad='pad_dark', lead='brass',
                  texture='embers', kit='driving', arp=False, motif_start=0, rt60=2.2, wet=0.26, bass='sawbass', stab='brass'),
    'frost': dict(label='Ice and stars', root=42, scale='lydian', bpm=(56, 96), prog=[0, 1, 4, 1, 5, 1, 0, 4], chord_bars=2, pad='glass', lead='bell',
                  texture='wind_high', kit='soft', arp=True, motif_start=4, rt60=5.0, wet=0.42, bass='sub', stab='glass'),
    'verdant': dict(label='Moss and fey lights', root=43, scale='mixolydian', bpm=(80, 118), prog=[0, 3, 6, 3, 0, 5, 6, 0], chord_bars=1, pad='pad_warm', lead='kalimba',
                    texture='birds', kit='hand', arp=True, motif_start=2, rt60=2.8, wet=0.30, bass='pluckbass', stab='pluck'),
    'spatial': dict(label='Folded space and shadow', root=37, scale='wholetone', bpm=(50, 90), prog=[0, 2, 4, 1, 3, 5, 2, 0], chord_bars=2, pad='glass', lead='glassbell',
                    texture='whoosh', kit='sparse', arp=True, motif_start=1, rt60=5.6, wet=0.46, bass='sub', stab='glass'),
    'holy': dict(label='Light and choirs', root=38, scale='ionian', bpm=(76, 120), prog=[0, 4, 5, 3, 0, 3, 4, 0], chord_bars=2, pad='choir_ah', lead='bell',
                 texture='airy', kit='timp', arp=False, motif_start=4, rt60=4.8, wet=0.40, bass='sub', stab='strings'),
    'deep': dict(label='Drowned and abyssal', root=35, scale='aeolian', bpm=(54, 92), prog=[0, 5, 3, 6, 0, 5, 6, 4], chord_bars=2, pad='pad_dark', lead='sonar',
                 texture='drips', kit='deep', arp=False, motif_start=0, rt60=5.4, wet=0.44, bass='sub', stab='strings'),
    'crypt': dict(label='Crypts and haunted halls', root=40, scale='harm_minor', bpm=(62, 104), prog=[0, 5, 3, 4, 0, 3, 5, 4], chord_bars=2, pad='organ', lead='celesta',
                  texture='wind_low', kit='bone', arp=False, motif_start=4, rt60=4.4, wet=0.38, bass='sawbass', stab='organ'),
}
FAMILY_ORDER = list(FAMILIES)

SPECIALS = {
    'boss_holy': dict(kind='boss', label='Boss, Holy road', root=38, scale='lydian', bpm=128, prog=[0, 4, 5, 3, 0, 1, 4, 3], pad='choir_ah', lead='brass', texture='airy',
                      kit='timp', arp=False, motif_start=4, rt60=3.8, wet=0.32, bass='sawbass', stab='brass', choir='choir_ah', ostinato=True, seed_family='holy'),
    'boss_abyssal': dict(kind='boss', label='Boss, Abyssal road', root=36, scale='phrygian_dom', bpm=104, prog=[0, 1, 0, 3, 0, 1, 5, 4], pad='pad_dark', lead='brass', texture='rumble',
                         kit='deep', arp=False, motif_start=0, rt60=4.0, wet=0.34, bass='sawbass', stab='brass', choir='choir_oh', ostinato=True, seed_family='deep'),
    'safe': dict(kind='safe', label='Safe zone', root=53, scale='pent_major', bpm=60, prog=[0, 3, 1, 4, 0, 2, 3, 1], chord_bars=2, pad='pad_warm', lead='musicbox',
                 texture='airy', kit='soft', arp=False, motif_start=2, rt60=3.4, wet=0.38, bass='sub', stab='pluck'),
    'hub': dict(kind='hub', label='Hub', root=48, scale='ionian', bpm=92, prog=[0, 4, 5, 3, 0, 3, 4, 0], chord_bars=1, pad='pad_warm', lead='flute',
                texture='birds', kit='hand', arp=True, motif_start=4, rt60=2.0, wet=0.24, bass='pluckbass', stab='pluck'),
    'rift': dict(kind='rift', label='Rift', root=37, scale='wholetone', bpm=48, prog=[0, 3, 1, 4, 2, 5, 0, 3], chord_bars=2, pad='glass', lead='glassbell',
                 texture='whoosh', kit='sparse', arp=False, motif_start=1, rt60=6.0, wet=0.5),
}


def track_names():
    names = []
    for fam in FAMILY_ORDER:
        names += [fam + '_explore', fam + '_battle']
    return names + list(SPECIALS)


def event_id(name):
    """music.stone.explore, music.boss.holy, music.safe (mirrors MusicCatalog.eventId)."""
    if name.startswith('boss_'):
        return 'music.boss.' + name[5:]
    if '_' in name:
        a, b = name.split('_', 1)
        return 'music.%s.%s' % (a, b)
    return 'music.' + name


def seed_of(name):
    return zlib.crc32(('nusmp-music:' + name).encode('utf-8')) & 0xffffffff


def midi_hz(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


def deg_midi(root, scale, deg):
    s = SCALES[scale]
    return root + 12 * (deg // len(s)) + s[deg % len(s)]


def chord_degrees(scale, deg, size=3):
    return [deg + 2 * k for k in range(size)]


# --------------------------------------------------------------------------- DSP helpers
def tvec(n):
    return np.arange(n, dtype=np.float64) / SR


def smooth(x):
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3 - 2 * x)


def env_ar(n, att, rel):
    """Attack, sustain, release over n samples (release ends at n)."""
    e = np.ones(n)
    a = min(n, max(1, int(att * SR)))
    r = min(n, max(1, int(rel * SR)))
    e[:a] = smooth(np.arange(a) / a)
    e[n - r:] *= smooth(np.arange(r, 0, -1) / r)
    return e


def fft_shape(x, fn):
    """Multiplies the spectrum of x by fn(freqs); circular, so loops stay seamless."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1.0 / SR)
    return np.fft.irfft(X * fn(f), len(x))


def band(x, lo, hi, order=2):
    return fft_shape(x, lambda f: 1.0 / (1.0 + (np.maximum(lo, 1.0) / np.maximum(f, 1.0)) ** (2 * order)) / (1.0 + (f / hi) ** (2 * order)))


def harmonics(f, kind, fc, nmax=48):
    hs = np.arange(1, nmax + 1, dtype=np.float64)
    hs = hs[hs * f < SR * 0.46]
    if len(hs) == 0:
        hs = np.array([1.0])
    if kind == 'saw':
        a = 1.0 / hs
    elif kind == 'square':
        a = (hs % 2 == 1) / hs
    elif kind == 'tri':
        a = (hs % 2 == 1) / hs ** 2
    elif kind == 'organ':
        a = np.zeros_like(hs)
        for h, v in ((1, 1.0), (2, 0.8), (3, 0.6), (4, 0.5), (6, 0.35), (8, 0.25), (10, 0.12)):
            a[hs == h] = v
    else:
        a = hs ** -1.6
    a = a / np.sqrt(1.0 + (hs * f / fc) ** 4)
    keep = a > 0.02 * a.max()
    return hs[keep], a[keep]


def osc(f, n, kind='saw', fc=2000.0, detunes=(0.0,), vib=(5.0, 0.0), rng=None):
    """A bank of band-limited oscillators (additive), optionally detuned in cents and with vibrato (rate Hz, depth fraction)."""
    t = tvec(n)
    hs, a = harmonics(f, kind, fc)
    out = np.zeros(n)
    for d in detunes:
        ff = f * 2.0 ** (d / 1200.0)
        ph = 2 * np.pi * ff * t
        if vib[1] > 0:
            ph = ph - (vib[1] * ff / vib[0]) * np.cos(2 * np.pi * vib[0] * t)
        phi = rng.uniform(0, 2 * np.pi, len(hs)) if rng is not None else np.zeros(len(hs))
        out += a @ np.sin(np.outer(hs, ph) + phi[:, None])
    return out / max(1, len(detunes))


# --------------------------------------------------------------------------- instruments: (freq, seconds, velocity, rng) -> samples (includes the release tail)
def pad(f, dur, vel, rng, kind='saw', fc=1400.0, att=1.4, rel=1.8, detunes=(-9.0, 0.0, 8.0), vib=(0.0, 0.0)):
    n = int((dur + rel) * SR)
    return (osc(f, n, kind, fc, detunes, vib, rng) * env_ar(n, att, rel) * vel * 0.5).astype(np.float32)


def choir(f, dur, vel, rng, vowel='ah', att=1.0, rel=1.6):
    forms = {'ah': ((800, 120), (1150, 140), (2900, 200)), 'oo': ((350, 90), (600, 90), (2700, 200)), 'oh': ((450, 100), (800, 110), (2830, 200)),
             'ee': ((270, 70), (2140, 160), (3010, 200))}[vowel]
    n = int((dur + rel) * SR)
    t = tvec(n)
    out = np.zeros(n)
    for d in (-14.0, 0.0, 12.0):
        ff = f * 2.0 ** (d / 1200.0)
        hs = np.arange(1, 60, dtype=np.float64)
        hs = hs[hs * ff < SR * 0.45]
        amp = np.zeros(len(hs))
        for k, (fr, bw) in enumerate(forms):
            amp += (1.0, 0.6, 0.3)[k] * np.exp(-0.5 * ((hs * ff - fr) / bw) ** 2)
        amp = amp / hs ** 0.5 + 0.02 / hs
        vr = 5.0 + rng.uniform(-0.4, 0.4)
        ph = 2 * np.pi * ff * t - (0.004 * ff / vr) * np.cos(2 * np.pi * vr * t)
        out += amp @ np.sin(np.outer(hs, ph) + rng.uniform(0, 2 * np.pi, len(hs))[:, None])
    breath = band(rng.standard_normal(n), 900, 3500) * 0.05
    return ((out / 3.0 * 0.5 + breath) * env_ar(n, att, rel) * vel).astype(np.float32)


def brass(f, dur, vel, rng, att=0.12, rel=0.5, fc0=500.0, fc1=3200.0):
    n = int((dur + rel) * SR)
    t = tvec(n)
    hs = np.arange(1, 36, dtype=np.float64)
    hs = hs[hs * f < SR * 0.45]
    sw = smooth(t / max(att * 2.5, 1e-3))
    out = np.zeros(n)
    ph = 2 * np.pi * f * t + 0.012 * np.sin(2 * np.pi * 5.2 * t) * smooth((t - 0.3) / 0.5)
    fc = fc0 + (fc1 - fc0) * sw
    for h in hs:
        w = 1.0 / h / np.sqrt(1.0 + (h * f / fc) ** 4)
        if h > 1 and float(w[-1]) < 0.01 and float(w.max()) < 0.01:
            break
        out += np.sin(h * ph + rng.uniform(0, 6.28)) * w
    return (out * env_ar(n, att, rel) * vel * 0.55).astype(np.float32)


def bell(f, dur, vel, rng, ratios=(1.0, 2.01, 2.76, 5.4, 8.93), amps=(1.0, 0.55, 0.4, 0.2, 0.1), decay=1.6):
    n = int(dur * SR)
    t = tvec(n)
    out = np.zeros(n)
    for i, (r, a) in enumerate(zip(ratios, amps)):
        if f * r < SR * 0.45:
            out += a * np.sin(2 * np.pi * f * r * t + rng.uniform(0, 6.28)) * np.exp(-t * decay * (1.0 + 0.55 * i))
    out *= np.minimum(1.0, t / 0.004)
    return (out * vel * 0.5).astype(np.float32)


def pluck(f, dur, vel, rng, decay=3.0, spread=0.9, kind='saw'):
    n = int(dur * SR)
    t = tvec(n)
    hs = np.arange(1, 24, dtype=np.float64)
    hs = hs[hs * f < SR * 0.45]
    a = hs ** (-1.2 if kind == 'saw' else -2.0)
    dec = np.exp(-np.outer(decay * (1.0 + spread * (hs - 1) / 3.0), t))
    ph = 2 * np.pi * f * t * (1.0 + 0.002 * np.exp(-t * 30))
    out = (a[:, None] * dec * np.sin(np.outer(hs, ph))).sum(axis=0)
    out *= np.minimum(1.0, t / 0.003)
    return (out * vel * 0.6).astype(np.float32)


def kalimba(f, dur, vel, rng):
    n = int(dur * SR)
    t = tvec(n)
    out = np.sin(2 * np.pi * f * t) * np.exp(-t * 4.0) + 0.35 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 18.0) + 0.2 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 7.0)
    out *= np.minimum(1.0, t / 0.002)
    return (out * vel * 0.6).astype(np.float32)


def musicbox(f, dur, vel, rng):
    n = int(dur * SR)
    t = tvec(n)
    out = np.sin(2 * np.pi * f * t) * np.exp(-t * 2.4) + 0.5 * np.sin(2 * np.pi * f * 3.0 * t) * np.exp(-t * 7.0) + 0.25 * np.sin(2 * np.pi * f * 6.1 * t) * np.exp(-t * 14.0)
    out *= np.minimum(1.0, t / 0.002)
    return (out * vel * 0.55).astype(np.float32)


def flute(f, dur, vel, rng, att=0.07, rel=0.25):
    n = int((dur + rel) * SR)
    t = tvec(n)
    vr = 5.3
    ph = 2 * np.pi * f * t - (0.005 * f / vr) * np.cos(2 * np.pi * vr * t) * smooth(t / 0.4)
    out = np.sin(ph) + 0.25 * np.sin(2 * ph) + 0.06 * np.sin(3 * ph)
    out += band(rng.standard_normal(n), f * 1.5, f * 5.0) * 0.07
    return (out * env_ar(n, att, rel) * vel * 0.5).astype(np.float32)


def sub(f, dur, vel, rng, att=0.04, rel=0.5):
    n = int((dur + rel) * SR)
    t = tvec(n)
    out = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t) + 0.08 * np.sin(6 * np.pi * f * t)
    return (out * env_ar(n, att, rel) * vel * 0.7).astype(np.float32)


def sawbass(f, dur, vel, rng):
    n = int((dur + 0.15) * SR)
    t = tvec(n)
    x = osc(f, n, 'saw', 380.0, (-6.0, 6.0), rng=rng)
    return (x * env_ar(n, 0.01, 0.15) * np.exp(-t * 0.8) * vel * 0.9).astype(np.float32)


def pluckbass(f, dur, vel, rng):
    return pluck(f, max(dur, 0.4), vel * 1.2, rng, decay=4.5, spread=1.5, kind='tri')


def sonar(f, dur, vel, rng):
    n = int(max(dur, 3.0) * SR)
    t = tvec(n)
    ph = 2 * np.pi * f * t + 0.6 * np.sin(2 * np.pi * f * 1.5 * t) * np.exp(-t * 3.0)
    out = np.sin(ph) * np.exp(-t * 1.1) * np.minimum(1.0, t / 0.01)
    return (out * vel * 0.55).astype(np.float32)


def celesta(f, dur, vel, rng):
    return bell(f, max(dur, 2.0), vel, rng, ratios=(1.0, 4.0, 9.0), amps=(1.0, 0.25, 0.08), decay=1.8)


def glassbell(f, dur, vel, rng):
    return bell(f, max(dur, 3.5), vel, rng, ratios=(1.0, 3.01, 5.43, 8.17), amps=(1.0, 0.5, 0.3, 0.14), decay=0.9)


# the pads and sustained leads by name: name -> callable(f, dur, vel, rng)
INSTR = {
    'pad_dark': lambda f, d, v, r: pad(f, d, v, r, 'saw', 700.0, 1.8, 2.2),
    'pad_warm': lambda f, d, v, r: pad(f, d, v, r, 'tri', 1800.0, 1.2, 1.6),
    'pad_bright': lambda f, d, v, r: pad(f, d, v, r, 'saw', 2600.0, 0.5, 1.0),
    'strings': lambda f, d, v, r: pad(f, d, v, r, 'saw', 1900.0, 0.9, 1.4, (-8.0, 0.0, 9.0), (5.2, 0.003)),
    'cello': lambda f, d, v, r: pad(f, d, v, r, 'saw', 1100.0, 0.35, 0.8, (-5.0, 5.0), (5.0, 0.004)),
    'glass': lambda f, d, v, r: pad(f, d, v, r, 'soft', 3200.0, 1.6, 2.2, (-14.0, 0.0, 13.0)),
    'organ': lambda f, d, v, r: pad(f, d, v, r, 'organ', 3000.0, 0.15, 0.4, (-3.0, 3.0)),
    'horn': lambda f, d, v, r: brass(f, d, v, r, 0.25, 0.7, 350.0, 1800.0),
    'brass': lambda f, d, v, r: brass(f, d, v, r),
    'choir_ah': lambda f, d, v, r: choir(f, d, v, r, 'ah'),
    'choir_oh': lambda f, d, v, r: choir(f, d, v, r, 'oh'),
    'choir_oo': lambda f, d, v, r: choir(f, d, v, r, 'oo'),
    'bell': lambda f, d, v, r: bell(f, max(d, 3.0), v, r),
    'glassbell': glassbell,
    'pluck': lambda f, d, v, r: pluck(f, max(d, 0.9), v, r),
    'kalimba': lambda f, d, v, r: kalimba(f, max(d, 1.2), v, r),
    'musicbox': lambda f, d, v, r: musicbox(f, max(d, 2.0), v, r),
    'flute': lambda f, d, v, r: flute(f, d, v, r),
    'sonar': sonar,
    'celesta': celesta,
    'sub': sub,
    'sawbass': sawbass,
    'pluckbass': pluckbass,
}


# short versions for the rhythmic stabs and the ostinatos
STAB = {
    'strings': lambda f, d, v, r: pad(f, d, v, r, 'saw', 2400.0, 0.02, 0.16, (-8.0, 8.0)),
    'brass': lambda f, d, v, r: brass(f, d, v, r, 0.03, 0.18, 600.0, 3400.0),
    'glass': lambda f, d, v, r: bell(f, 1.6, v, r),
    'pluck': lambda f, d, v, r: pluck(f, 0.7, v, r),
    'organ': lambda f, d, v, r: pad(f, d, v, r, 'organ', 3000.0, 0.01, 0.12, (-3.0, 3.0)),
}


# --------------------------------------------------------------------------- percussion (rendered once per track, a few variants each)
def _noise(n, rng):
    return rng.standard_normal(n)


def drum_kit(rng):
    k = {}

    def kick(f0, f1, dec, n=0.5):
        m = int(n * SR)
        t = tvec(m)
        ph = 2 * np.pi * (f1 * t + (f0 - f1) / 30.0 * (1 - np.exp(-30 * t)))
        return (np.sin(ph) * np.exp(-t / dec) + 0.3 * band(_noise(m, rng), 2000, 6000) * np.exp(-t * 150)) * np.minimum(1.0, t / 0.001)

    def tom(f, dec, n=0.7):
        m = int(n * SR)
        t = tvec(m)
        ph = 2 * np.pi * (f * t + 0.35 * f / 25.0 * (1 - np.exp(-25 * t)))
        return (np.sin(ph) * np.exp(-t / dec) + 0.12 * band(_noise(m, rng), 300, 2500) * np.exp(-t * 60)) * np.minimum(1.0, t / 0.001)

    def snare(n=0.4, tone=190.0):
        m = int(n * SR)
        t = tvec(m)
        return (0.6 * band(_noise(m, rng), 900, 7000) * np.exp(-t * 22) + 0.5 * np.sin(2 * np.pi * tone * t) * np.exp(-t * 28)) * np.minimum(1.0, t / 0.0008)

    def hat(n=0.12, decay=45):
        m = int(n * SR)
        t = tvec(m)
        return band(_noise(m, rng), 5000, 10500) * np.exp(-t * decay) * np.minimum(1.0, t / 0.0005)

    def shaker(n=0.16):
        m = int(n * SR)
        t = tvec(m)
        return band(_noise(m, rng), 3500, 9500) * np.minimum(1.0, t / 0.03) * np.exp(-t * 22)

    def anvil(f, n=1.3):
        m = int(n * SR)
        t = tvec(m)
        out = np.zeros(m)
        for r, a, d in ((1.0, 1.0, 5.0), (1.52, 0.7, 6.0), (2.37, 0.5, 8.0), (3.31, 0.35, 10.0), (4.87, 0.2, 14.0)):
            out += a * np.sin(2 * np.pi * f * r * t + rng.uniform(0, 6.28)) * np.exp(-t * d)
        return (out + 0.4 * band(_noise(m, rng), 2500, 8000) * np.exp(-t * 90)) * np.minimum(1.0, t / 0.0006) * 0.7

    def wood(f, n=0.2):
        m = int(n * SR)
        t = tvec(m)
        return (np.sin(2 * np.pi * f * t) * np.exp(-t * 40) + 0.4 * np.sin(2 * np.pi * f * 2.4 * t) * np.exp(-t * 60)) * np.minimum(1.0, t / 0.0005)

    def cymbal(n=3.2, swell=0.0):
        m = int(n * SR)
        t = tvec(m)
        x = band(_noise(m, rng), 3500, 10000) * (np.exp(-t * 1.6) if swell == 0 else smooth(t / swell) * np.exp(-np.maximum(0, t - swell) * 6.0))
        return x * np.minimum(1.0, t / 0.002)

    k['kick'] = [kick(115, 48, 0.16), kick(105, 45, 0.18)]
    k['kick_deep'] = [kick(95, 38, 0.28, 0.8)]
    k['snare'] = [snare(), snare(tone=210.0)]
    k['tom_hi'] = [tom(150, 0.22), tom(160, 0.2)]
    k['tom_mid'] = [tom(110, 0.3), tom(118, 0.28)]
    k['tom_lo'] = [tom(72, 0.5, 1.0), tom(78, 0.45, 1.0)]
    k['taiko'] = [tom(62, 0.7, 1.4), tom(66, 0.65, 1.4)]
    k['hat'] = [hat(), hat(decay=55), hat(decay=38)]
    k['hat_open'] = [hat(0.4, 9)]
    k['shaker'] = [shaker(), shaker(0.14)]
    k['anvil'] = [anvil(560.0), anvil(740.0)]
    k['wood'] = [wood(900.0), wood(1250.0), wood(700.0)]
    k['crash'] = [cymbal()]
    k['swell'] = [cymbal(4.2, 3.4)]
    return k


# kit name -> (kick-ish voice, snare-ish voice, hat-ish voice, 16-step patterns)
KITS = {
    'taiko':   dict(low='taiko', mid='tom_mid', top='anvil', k=[0, 6, 8, 14], s=[4, 12], h=[], fill='tom_lo'),
    'driving': dict(low='kick', mid='snare', top='hat', k=[0, 3, 8, 10], s=[4, 12], h=list(range(0, 16, 2)), fill='tom_mid'),
    'tribal':  dict(low='tom_lo', mid='tom_mid', top='wood', k=[0, 3, 8, 11], s=[6, 14], h=[2, 5, 10, 13], fill='tom_hi'),
    'soft':    dict(low='tom_lo', mid='wood', top='shaker', k=[0, 8], s=[], h=[4, 12], fill='tom_hi'),
    'hand':    dict(low='tom_mid', mid='tom_hi', top='shaker', k=[0, 5, 8, 13], s=[3, 11], h=list(range(0, 16, 2)), fill='tom_hi'),
    'sparse':  dict(low='tom_lo', mid='wood', top='shaker', k=[0, 10], s=[6], h=[3, 11, 14], fill='tom_hi'),
    'timp':    dict(low='taiko', mid='tom_mid', top='crash', k=[0, 4, 8, 12], s=[], h=[], fill='tom_mid'),
    'deep':    dict(low='kick_deep', mid='tom_lo', top='anvil', k=[0, 6, 10], s=[4, 12], h=[], fill='tom_lo'),
    'bone':    dict(low='tom_lo', mid='wood', top='wood', k=[0, 8], s=[4, 12], h=[2, 6, 10, 14], fill='tom_hi'),
}


# --------------------------------------------------------------------------- textures (continuous or sparse ambience, periodic in the loop)
def lfo(n, cycles, phase=0.0):
    return 0.5 + 0.5 * np.sin(2 * np.pi * (cycles * np.arange(n) / n + phase))


def texture(kind, n, rng, bar_s, level):
    """Returns (main, send): noise layers whose slow movements repeat a whole number of times per loop."""
    secs = n / SR
    if kind in ('wind_low', 'wind_high', 'storm', 'airy'):
        lo, hi = {'wind_low': (120, 900), 'wind_high': (900, 4200), 'storm': (200, 1800), 'airy': (2500, 7500)}[kind]
        x = band(rng.standard_normal(n), lo, hi)
        m = (0.25 + 0.75 * lfo(n, max(2, round(secs / 14)), rng.random())) * (0.5 + 0.5 * lfo(n, max(3, round(secs / 5.5)), rng.random()))
        out = x * m * level * (0.9 if kind != 'airy' else 0.45)
        if kind == 'storm':
            low = band(rng.standard_normal(n), 25, 130) * lfo(n, max(2, round(secs / 19)), 0.3) ** 3 * level * 1.6
            out = out + low
        return out, out * 0.2
    if kind == 'rumble':
        x = band(rng.standard_normal(n), 25, 140) * (0.4 + 0.6 * lfo(n, max(2, round(secs / 11)), rng.random()))
        return x * level * 2.2, np.zeros(n)
    if kind == 'whoosh':
        x = band(rng.standard_normal(n), 400, 5200)
        cyc = max(2, round(secs / (bar_s * 4)))
        m = lfo(n, cyc, rng.random()) ** 4
        return x * m * level * 1.2, x * m * level * 0.6
    if kind == 'heartbeat':
        out = np.zeros(n)
        beat = bar_s / 4.0
        k = np.arange(int(0.4 * SR)) / SR
        thump = np.sin(2 * np.pi * (46 + 22 * np.exp(-k * 30)) * k) * np.exp(-k * 14)
        for i in range(int(secs / (beat * 2))):
            for off, g in ((0.0, 1.0), (0.32 * beat, 0.65)):
                p = int((i * beat * 2 + off) * SR)
                end = min(n, p + len(thump))
                out[p:end] += thump[:end - p] * g
        return out * level * 2.0, np.zeros(n)
    if kind == 'embers':
        out = np.zeros(n)
        base = band(rng.standard_normal(n), 60, 420) * (0.3 + 0.7 * lfo(n, max(2, round(secs / 9)), rng.random())) * 0.5
        for _ in range(int(secs * 5)):
            p = int(rng.random() * n)
            ln = int(rng.uniform(0.004, 0.03) * SR)
            tt = np.arange(ln) / SR
            burst = rng.standard_normal(ln) * np.exp(-tt * rng.uniform(150, 500)) * rng.uniform(0.2, 1.0)
            out[(p + np.arange(ln)) % n] += burst
        return (band(out, 1500, 9000) * 0.7 + base) * level, base * level * 0.3
    if kind == 'drips':
        out = np.zeros(n)
        for _ in range(int(secs / 1.7)):
            p = int(rng.random() * n)
            f = rng.uniform(700, 2400)
            ln = int(0.5 * SR)
            tt = np.arange(ln) / SR
            ping = np.sin(2 * np.pi * (f + 600 * np.exp(-tt * 40)) * tt) * np.exp(-tt * 14) * rng.uniform(0.2, 0.8)
            idx = (p + np.arange(ln)) % n
            out[idx] += ping
        low = band(rng.standard_normal(n), 30, 200) * lfo(n, max(2, round(secs / 16)), rng.random()) * 0.6
        return (out * 0.6 + low) * level, out * level * 0.8
    if kind == 'birds':
        out = np.zeros(n)
        for _ in range(int(secs / 4.5)):
            p = int(rng.random() * n)
            f0 = rng.uniform(2400, 4200)
            for _c in range(int(rng.integers(2, 5))):
                ln = int(rng.uniform(0.05, 0.12) * SR)
                tt = np.arange(ln) / SR
                glide = f0 * (1 + 0.25 * rng.uniform(-1, 1) * tt / tt[-1])
                chirp = np.sin(2 * np.pi * np.cumsum(glide) / SR) * np.sin(np.pi * tt / tt[-1]) * 0.35
                out[(p + np.arange(ln)) % n] += chirp
                p += int(ln + rng.uniform(0.04, 0.12) * SR)
        return out * level, out * level * 0.6
    return np.zeros(n), np.zeros(n)


# --------------------------------------------------------------------------- the mixer
class Mix:
    def __init__(self, n):
        self.n = n
        self.main = np.zeros(n, np.float32)
        self.send = np.zeros(n, np.float32)

    def add(self, t_sec, sig, gain=1.0, send=0.0):
        i = int(round(t_sec * SR)) % self.n
        sig = np.asarray(sig, np.float32)
        if len(sig) > self.n:
            sig = sig[:self.n]
        end = i + len(sig)
        for buf, g in ((self.main, gain), (self.send, gain * send)):
            if g == 0:
                continue
            if end <= self.n:
                buf[i:end] += sig * g
            else:
                k = self.n - i
                buf[i:] += sig[:k] * g
                buf[:end - self.n] += sig[k:] * g


def reverb(x, rt60, rng, damp=3800.0, pre=0.022):
    n = len(x)
    m = min(n, int(rt60 * 1.1 * SR))
    t = np.arange(m) / SR
    ir = rng.standard_normal(m) * np.exp(-6.9 * t / rt60)
    ir = np.concatenate([np.zeros(int(pre * SR)), ir])[:m]
    ir = fft_shape(ir, lambda f: 1.0 / (1.0 + (f / damp) ** 2))
    ir /= math.sqrt(float(np.sum(ir ** 2))) + 1e-9
    full = np.zeros(n)
    full[:m] = ir
    return np.fft.irfft(np.fft.rfft(x) * np.fft.rfft(full), n)


# --------------------------------------------------------------------------- composition
def bars_for(bpm):
    bars = max(4, int(round(TARGET_SECONDS * bpm / 240.0 / 4.0)) * 4)
    while bars * 240.0 / bpm > MAX_SECONDS - 0.5:
        bars -= 4
    while bars * 240.0 / bpm < MIN_SECONDS + 0.5:
        bars += 4
    return bars


def make_motif(rng, start, scale_len):
    """A theme of one bar-pair: list of (degree, beats). Deterministic per family, so the explore and the battle loop share it."""
    rhythms = [[1, 1, 2], [2, 1, 1], [1, 0.5, 0.5, 2], [1.5, 0.5, 1, 1], [3, 1], [1, 1, 1, 1], [0.5, 0.5, 1, 2]]
    rh = rhythms[int(rng.integers(len(rhythms)))]
    deg = start
    out = []
    for i, b in enumerate(rh):
        out.append((deg, b))
        deg += int(rng.choice([-2, -1, -1, 1, 1, 2, 3]))
        deg = max(start - 3, min(start + scale_len, deg))
    return out


def invert(motif, around):
    return [(2 * around - d, b) for d, b in motif]


def snap(deg, chord, L):
    """Nearest degree to deg whose pitch class is a chord tone."""
    ok = {c % L for c in chord}
    for d in (0, 1, -1, 2, -2):
        if (deg + d) % L in ok:
            return deg + d
    return deg


def strong(t, beat):
    """Is t on the first or third beat of a bar?"""
    r = (t / beat) % 4
    return abs(r - round(r)) < 1e-6 and int(round(r)) in (0, 2, 4)


def compose(name, spec, layer, rng):
    """Renders one loop. layer is 'explore', 'battle', 'boss', 'safe', 'hub' or 'rift'. Returns float32 samples."""
    fam = spec
    scale = spec['scale']
    L = len(SCALES[scale])
    bpm = spec['bpm'][0 if layer == 'explore' else 1] if isinstance(spec['bpm'], tuple) else spec['bpm']
    bars = bars_for(bpm)
    beat = 60.0 / bpm
    bar_s = beat * 4
    n = int(round(bars * bar_s * SR))
    mix = Mix(n)
    root = spec['root']
    prog = spec['prog']
    chord_bars = spec.get('chord_bars', 1) if layer in ('explore', 'safe', 'rift') else 1
    pad_fn = INSTR[spec['pad']]
    lead_fn = INSTR[spec['lead']]
    sustained = spec['lead'] in ('horn', 'brass', 'cello', 'organ', 'flute', 'choir_ah', 'strings')
    kit = KITS[spec['kit']]
    drums = drum_kit(rng)
    drum_rng = np.random.default_rng(seed_of(name + ':drums'))
    # the family's own theme, the same for the explore and the battle loop
    theme_rng = np.random.default_rng(seed_of('theme:' + spec.get('seed_family', name.split('_')[0])))
    motif_a = make_motif(theme_rng, spec['motif_start'], L)
    motif_b = invert(make_motif(theme_rng, spec['motif_start'], L), spec['motif_start'] + 2)

    chords = [prog[(b // chord_bars) % len(prog)] for b in range(bars)]

    def chord_at(b):
        return chords[b % bars]

    # --- harmonic bed: bass root + pad chord, one chord per chord_bars
    bed_gain = {'explore': 1.0, 'battle': 0.55, 'boss': 0.5, 'safe': 0.9, 'hub': 0.6, 'rift': 1.0}[layer]
    b = 0
    while b < bars:
        d = chord_at(b)
        span = min(chord_bars, bars - b)
        t0 = b * bar_s
        dur = span * bar_s
        cd = chord_degrees(scale, d)
        for k, cdeg in enumerate(cd):
            m = deg_midi(root + 12, scale, cdeg)
            if layer == 'rift':
                m = deg_midi(root + 12, scale, cdeg) + (12 if k == 2 else 0)
            mix.add(t0 + (0.03 * k), pad_fn(midi_hz(m), dur - 0.4, 0.5 * bed_gain, rng), 0.75, 0.7)
        if layer in ('explore', 'safe', 'rift'):
            mix.add(t0, INSTR['sub'](midi_hz(deg_midi(root, scale, d)), dur - 0.3, 0.8 * bed_gain, rng), 0.8, 0.1)
        b += span

    # --- texture
    tex_main, tex_send = texture(spec['texture'], n, np.random.default_rng(seed_of(name + ':tex')), bar_s,
                                 {'explore': 0.10, 'battle': 0.05, 'boss': 0.05, 'safe': 0.05, 'hub': 0.06, 'rift': 0.12}[layer])
    mix.main += tex_main.astype(np.float32)
    mix.send += tex_send.astype(np.float32)

    # --- melody
    phrase_forms = ['A', 'A2', 'B', 'A']
    for ph in range(bars // 4):
        form = phrase_forms[ph % 4]
        mot = motif_b if form == 'B' else motif_a
        t_ph = ph * 4 * bar_s
        if layer in ('explore', 'safe', 'rift'):
            stretch = 2.0 if layer != 'safe' else 2.0
            if (ph % 4 == 3 and layer != 'safe') and rng.random() < 0.5:
                continue                                        # a breath: the theme rests now and then
            for rep, (shift, when) in enumerate(((0, 0.0), (-1, 8.0))):
                if rep == 1 and form == 'B':
                    continue
                t = t_ph + when * beat
                for deg, bt in mot:
                    dd = deg + shift + (1 if (form == 'A2' and rep == 1) else 0)
                    bar_idx = int(t / bar_s) % bars
                    if strong(t, beat) and layer != 'rift':
                        dd = snap(dd, chord_degrees(scale, chord_at(bar_idx)), L)
                    midi = deg_midi(root + 24, scale, dd)
                    dur = bt * beat * stretch
                    vel = 0.55 if layer == 'explore' else 0.5
                    mix.add(t, lead_fn(midi_hz(midi), dur, vel, rng), 0.7, 0.8)
                    t += bt * beat * stretch
        else:
            pass

    if layer == 'battle' or layer == 'boss' or layer == 'hub':
        mel_gain = {'battle': 0.75, 'boss': 0.8, 'hub': 0.7}[layer]
        for bar_pair in range(bars // 2):
            b0 = bar_pair * 2
            form = phrase_forms[(b0 // 4) % 4]
            mot = motif_b if form == 'B' else motif_a
            if layer == 'hub' and (b0 // 4) % 2 == 1 and form != 'B':
                continue
            t = b0 * bar_s
            for deg, bt in mot:
                bar_idx = int(t / bar_s) % bars
                dd = deg + (1 if form == 'A2' and (b0 % 4 == 2) else 0)
                if strong(t, beat):
                    dd = snap(dd, chord_degrees(scale, chord_at(bar_idx)), L)
                midi = deg_midi(root + (24 if layer != 'boss' else 24), scale, dd)
                dur = bt * beat * (0.9 if sustained else 1.0)
                mix.add(t, lead_fn(midi_hz(midi), dur, 0.55, rng), mel_gain, 0.5)
                t += bt * beat

    # --- battle / boss rhythm section
    if layer in ('battle', 'boss'):
        step = beat / 4.0
        for bi in range(bars):
            d = chord_at(bi)
            t0 = bi * bar_s
            fill = (bi % 4 == 3)
            for s in range(16):
                t = t0 + s * step
                if s in kit['k']:
                    mix.add(t, drums[kit['low']][int(drum_rng.integers(len(drums[kit['low']])))], 0.85 if s % 8 == 0 else 0.6, 0.12)
                if s in kit['s']:
                    mix.add(t, drums[kit['mid']][int(drum_rng.integers(len(drums[kit['mid']])))], 0.7, 0.2)
                if s in kit['h'] and not (fill and s >= 12):
                    mix.add(t, drums[kit['top']][int(drum_rng.integers(len(drums[kit['top']])))], 0.34 if s % 4 else 0.46, 0.15)
                if fill and s >= 12:
                    mix.add(t, drums[kit['fill']][int(drum_rng.integers(len(drums[kit['fill']])))], 0.55 + 0.1 * (s - 12), 0.2)
            if bi % 8 == 0 and layer == 'boss':
                mix.add(t0, drums['crash'][0], 0.5, 0.3)
            if bi % 8 == 7 and layer == 'boss':
                mix.add(t0 + bar_s - 3.3, drums['swell'][0], 0.35, 0.3)
            # bass ostinato in eighths
            root_m = deg_midi(root, scale, d)
            cd = chord_degrees(scale, d)
            for i in range(8):
                pm = root_m if i % 4 != 3 else deg_midi(root, scale, cd[2] if i == 3 else d + 6)
                if spec['bass'] == 'sub':
                    pm = root_m
                acc = 1.0 if i in (0, 4) else 0.65
                mix.add(t0 + i * beat / 2, INSTR[spec['bass']](midi_hz(pm), beat * 0.45, 0.8 * acc, rng), 0.85, 0.05)
            # stabs on the off beats
            for s in (0, 6, 10):
                for cdeg in cd:
                    m = deg_midi(root + 12, scale, cdeg)
                    mix.add(t0 + s * step, STAB[spec['stab']](midi_hz(m), 0.32, 0.4, rng), 0.5, 0.3)
            if spec.get('arp') and layer == 'battle':
                arp = [cd[0], cd[1], cd[2], cd[1] + L]
                for i in range(16):
                    m = deg_midi(root + 24, scale, arp[i % 4] + (L if i % 8 >= 4 else 0))
                    mix.add(t0 + i * step, INSTR['pluck'](midi_hz(m), 0.3, 0.32 + 0.1 * (i % 4 == 0), rng), 0.55, 0.45)
            if spec.get('ostinato'):
                for i in range(16):
                    m = deg_midi(root + 24, scale, cd[i % 3] + (L if i % 8 >= 6 else 0))
                    mix.add(t0 + i * step, STAB['strings'](midi_hz(m), 0.16, 0.34, rng), 0.5, 0.25)
        if spec.get('choir'):
            ch = INSTR[spec['choir']]
            for bi in range(0, bars, 4):
                cd = chord_degrees(scale, chord_at(bi))
                for k, cdeg in enumerate(cd):
                    mix.add(bi * bar_s, ch(midi_hz(deg_midi(root + 12, scale, cdeg)), bar_s * 4 - 0.5, 0.6, rng), 0.55, 0.8)
    elif layer == 'hub':
        step = beat / 4.0
        for bi in range(bars):
            d = chord_at(bi)
            t0 = bi * bar_s
            cd = chord_degrees(scale, d)
            for s in range(0, 16, 2):
                t = t0 + s * step
                m = deg_midi(root + 12, scale, cd[(s // 2) % 3] + (L if (s // 2) % 4 == 3 else 0))
                mix.add(t, INSTR['pluck'](midi_hz(m), 0.8, 0.34, rng), 0.55, 0.45)
            mix.add(t0, INSTR['pluckbass'](midi_hz(deg_midi(root - 12, scale, d)), beat * 1.6, 0.8, rng), 0.8, 0.05)
            mix.add(t0 + beat * 2, INSTR['pluckbass'](midi_hz(deg_midi(root - 12, scale, d + 4)), beat * 1.6, 0.65, rng), 0.8, 0.05)
            for s in range(16):
                if s % 2 == 0:
                    mix.add(t0 + s * step, drums['shaker'][int(drum_rng.integers(2))], 0.28 if s % 4 else 0.38, 0.1)
            for s in (0, 6, 10):
                mix.add(t0 + s * step, drums['tom_mid'][int(drum_rng.integers(2))], 0.35, 0.15)
    elif layer == 'explore' and spec['kit'] in ('taiko', 'timp', 'deep', 'bone', 'tribal', 'sparse', 'soft'):
        # a far-off drum once per four bars: the floor is not empty
        for bi in range(0, bars, 4):
            hit = 'tom_lo' if spec['kit'] != 'bone' else 'wood'
            mix.add(bi * bar_s + (2 * beat if bi % 8 else 0), drums[hit][0], 0.38, 0.8)
    elif layer == 'rift':
        for bi in range(2, bars, 4):
            mix.add(bi * bar_s, drums['tom_lo'][0], 0.45, 0.9)

    # --- the final mix: dry + circular reverb of the send, a gentle high-pass, a soft limiter, a level per layer
    wet = reverb(mix.send.astype(np.float64), spec['rt60'], np.random.default_rng(seed_of(name + ':verb')))
    x = mix.main.astype(np.float64) + wet * spec['wet'] * 2.2
    x = fft_shape(x, lambda f: 1.0 / (1.0 + (28.0 / np.maximum(f, 1.0)) ** 4) / (1.0 + (f / 9800.0) ** 4))
    target_db = {'explore': -23.0, 'battle': -19.5, 'boss': -18.0, 'safe': -24.0, 'hub': -21.0, 'rift': -23.5}[layer]
    rms = math.sqrt(float(np.mean(x ** 2))) + 1e-9
    x = x * (10 ** (target_db / 20.0) / rms)
    x = np.tanh(x * 1.3) / 1.3                                  # soft limiter: no clipped peaks
    peak = float(np.max(np.abs(x)))
    if peak > 0.82:
        x *= 0.82 / peak
    return x.astype(np.float32), bpm, bars


def render_one(name):
    if name in SPECIALS:
        spec = dict(SPECIALS[name])
        layer = spec.pop('kind')
        spec.setdefault('arp', False)
        spec.setdefault('bass', 'sub')
        spec.setdefault('stab', 'strings')
        spec.setdefault('lead', 'bell')
        spec['label'] = spec.get('label', name)
        # the layer function reads bpm as a plain number for the specials
    else:
        fam, layer = name.rsplit('_', 1)
        spec = dict(FAMILIES[fam])
    rng = np.random.default_rng(seed_of(name))
    return compose(name, spec, layer, rng)


# --------------------------------------------------------------------------- files
def write_wav(path, x):
    pcm = np.clip(x, -1.0, 1.0)
    pcm = (pcm * 32767.0).astype('<i2')
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def encode(x, out_path, quality):
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'x.wav')
        write_wav(wav, x)
        cmd = ['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-map_metadata', '-1', '-fflags', '+bitexact', '-flags:a', '+bitexact',
               '-c:a', 'libvorbis', '-q:a', str(quality), '-ac', '1', '-ar', str(SR), out_path]
        subprocess.run(cmd, check=True)


def ogg_info(path):
    d = open(path, 'rb').read()
    if d[:4] != b'OggS':
        raise ValueError('not an Ogg file')
    at = d.find(b'\x01vorbis')
    ch = d[at + 11]
    rate = struct.unpack('<I', d[at + 12:at + 16])[0]
    last = d.rfind(b'OggS')
    granule = struct.unpack('<q', d[last + 6:last + 14])[0]
    return ch, rate, granule / float(rate)


def generate(only, preview, quality):
    os.makedirs(MUSIC_DIR, exist_ok=True)
    if preview:
        os.makedirs(preview, exist_ok=True)
    names = [x for x in track_names() if only is None or x == only or x.startswith(only + '_') or (only in FAMILIES and x.split('_')[0] == only)]
    if not names:
        raise SystemExit('nothing matches --only ' + str(only))
    total = 0
    for name in names:
        x, bpm, bars = render_one(name)
        # the seam of the loop must be as smooth as any other place of the signal
        seam = abs(float(x[0]) - float(x[-1]))
        typical = float(np.percentile(np.abs(np.diff(x)), 99))
        out = os.path.join(MUSIC_DIR, name + '.ogg')
        encode(x, out, quality)
        if preview:
            write_wav(os.path.join(preview, name + '.wav'), x)
        size = os.path.getsize(out)
        total += size
        print('%-14s %3d bpm %3d bars %5.1f s %6.0f KB  peak %.2f  rms %.1f dB  seam %.4f (p99 step %.4f)' % (
            name, bpm, bars, len(x) / SR, size / 1024.0, float(np.max(np.abs(x))), 20 * math.log10(math.sqrt(float(np.mean(x.astype(np.float64) ** 2))) + 1e-9), seam, typical))
    print('%d tracks, %.2f MB' % (len(names), total / 1048576.0))


def manifest_entries():
    entries = {}
    for name in track_names():
        path = os.path.join(MUSIC_DIR, name + '.ogg')
        if not os.path.exists(path):
            raise SystemExit('missing %s (run the generator first)' % path)
        entries[event_id(name)] = {'sounds': [{'name': 'nusmp:music/' + name, 'stream': True}]}
    return entries


def _sound_text(snd):
    parts = ['"name": %s' % json.dumps(snd['name'])]
    for k, v in snd.items():
        if k != 'name':
            parts.append('%s: %s' % (json.dumps(k), json.dumps(v)))
    return '{ ' + ', '.join(parts) + ' }'


def _entry_text(key, entry):
    """One sounds.json entry in the file's own style (the same shape tools/gen_mob_sounds.py writes)."""
    lines = ['  %s: {' % json.dumps(key)]
    if 'subtitle' in entry:
        lines.append('    "subtitle": %s,' % json.dumps(entry['subtitle']))
    sounds = [x if isinstance(x, dict) else {'name': x} for x in entry.get('sounds', [])]
    if len(sounds) == 1:
        lines.append('    "sounds": [%s]' % _sound_text(sounds[0]))
    else:
        lines.append('    "sounds": [')
        lines.extend('      %s%s' % (_sound_text(x), ',' if i < len(sounds) - 1 else '') for i, x in enumerate(sounds))
        lines.append('    ]')
    lines.append('  }')
    return '\n'.join(lines)


def write_manifest():
    entries = manifest_entries()
    with open(SOUNDS_JSON, encoding='utf-8') as f:
        doc = json.load(f)
    doc = {k: v for k, v in doc.items() if not k.startswith('music.')}      # every other entry stays, in its place and its form
    doc.update(entries)
    sys.path.insert(0, os.path.join(ROOT, 'tools'))
    try:
        import gen_mob_sounds as gm                                            # the same text style as the mob voices, so neither script rewrites the other's lines
        entry_text = gm._entry_text
    except Exception:
        entry_text = _entry_text
    text = '{\n' + ',\n'.join(entry_text(k, v) for k, v in doc.items()) + '\n}\n'
    json.loads(text)
    with open(SOUNDS_JSON, 'w', encoding='utf-8') as f:
        f.write(text)
    total = sum(os.path.getsize(os.path.join(MUSIC_DIR, n + '.ogg')) for n in track_names())
    print('sounds.json: %d music events, %.2f MB of music' % (len(entries), total / 1048576.0))


def check():
    with open(SOUNDS_JSON, encoding='utf-8') as f:
        doc = json.load(f)
    bad = []
    total = 0
    for name in track_names():
        key = event_id(name)
        path = os.path.join(MUSIC_DIR, name + '.ogg')
        if key not in doc:
            bad.append('no sounds.json entry ' + key)
        if not os.path.exists(path):
            bad.append('missing ' + path)
            continue
        ch, rate, sec = ogg_info(path)
        total += os.path.getsize(path)
        if ch != 1:
            bad.append('%s has %d channels' % (name, ch))
        if not (MIN_SECONDS <= sec <= MAX_SECONDS):
            bad.append('%s is %.1f s' % (name, sec))
    extra = [k for k in doc if k.startswith('music.') and k not in {event_id(n) for n in track_names()}]
    bad += ['orphan entry ' + k for k in extra]
    if total > BUDGET_BYTES:
        bad.append('%.1f MB is over the budget' % (total / 1048576.0))
    print('%d tracks, %.2f MB, %d problems' % (len(track_names()), total / 1048576.0, len(bad)))
    for b in bad:
        print('  ' + b)
    return 1 if bad else 0


def list_tracks():
    print('%-14s %-28s %-4s %-13s %7s %5s %7s' % ('track', 'label', 'key', 'scale', 'bpm', 'bars', 'seconds'))
    for name in track_names():
        if name in SPECIALS:
            s = SPECIALS[name]
            bpm = s['bpm']
        else:
            s = FAMILIES[name.rsplit('_', 1)[0]]
            bpm = s['bpm'][0 if name.endswith('explore') else 1]
        bars = bars_for(bpm)
        key = NOTE_NAMES[s['root'] % 12]
        print('%-14s %-28s %-4s %-13s %7d %5d %7.1f' % (name, s['label'], key, s['scale'], bpm, bars, bars * 240.0 / bpm))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--only', help='one family (stone) or one track (stone_battle, boss_holy, safe, hub, rift)')
    ap.add_argument('--preview', help='also write a .wav of every track into this directory')
    ap.add_argument('--quality', type=float, default=2.0, help='Vorbis quality (-1..10), default 2')
    ap.add_argument('--list', action='store_true', help='print the table of tracks')
    ap.add_argument('--check', action='store_true', help='verify the files and sounds.json')
    ap.add_argument('--manifest-only', action='store_true', help='rewrite the music entries of sounds.json from the files that are there')
    args = ap.parse_args()
    if args.list:
        list_tracks()
        return 0
    if args.check:
        return check()
    if args.manifest_only:
        write_manifest()
        return 0
    generate(args.only, args.preview, args.quality)
    if args.only is None:
        write_manifest()
    return 0


if __name__ == '__main__':
    sys.exit(main())
