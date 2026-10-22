#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the sound set of the Undine (Noelle Silva's water Spirit Lord, docs/magic/undine.md): a set of her own, so she never sounds like the basic Tensura spirit.

What she sounds like: flowing water with bubbles, soft glass bells that ring out and fade into a wet room, a low watery choir hum that wobbles like light on a surface, and a big wave crash
for the full form. Every sound is built from nothing (numpy only, no samples, no scipy): filtered noise and chirped sines for the water, inharmonic partials with a slow decay for the glass,
formant-weighted detuned voices with vibrato for the hum, a swept noise burst under a low thump for the crash, and a synthetic noise-tail reverb. It is reproducible: every variant is seeded
from its own name, so a re-run writes the same audio. ffmpeg (libvorbis) encodes mono Ogg Vorbis at 44.1 kHz.

  python3 -B tools/gen_undine_sounds.py                 write every file, the entries of sounds.json (the "undine.*" entries only; every other entry is kept) and tools/lang/undine.json
  python3 -B tools/gen_undine_sounds.py --only cast     one cue
  python3 -B tools/gen_undine_sounds.py --preview DIR   also write a .wav and a spectrogram .png per file into DIR (to look at them without the game)
  python3 -B tools/gen_undine_sounds.py --report        print the length, peak and loudness of every file already written

The catalogue (cues, variants, volumes, ranges) is src/main/java/com/newuniverse/nusmp/undine/UndineSoundSet.java; UndineSoundsTest keeps this table, the catalogue, sounds.json and the files in step.
"""
import argparse
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
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp')
SOUND_DIR = os.path.join(ASSETS, 'sounds', 'undine')
SOUNDS_JSON = os.path.join(ASSETS, 'sounds.json')
LANG_OUT = os.path.join(ROOT, 'tools', 'lang', 'undine.json')

SR = 44100
# cue -> (variants, attenuation distance in blocks, subtitle verb, loudness target in dBFS RMS of the active part): the same table as UndineSoundSet.java
CUES = {
    'ambient': (3, 16, 'flows and hums', -24.0),
    'link': (2, 24, 'rises from the water', -18.0),
    'unlink': (2, 20, 'sinks into the water', -20.0),
    'cast': (3, 24, 'casts a water spell', -17.0),
    'full': (2, 48, 'rises as a wave', -16.0),
    'crash': (2, 56, 'crashes as a wave', -16.0),
    'hurt': (2, 20, 'splashes in pain', -17.0),
    'death': (1, 24, 'dissolves into water', -18.0),
    'hum': (3, 16, 'hums a soft tune', -22.0),
}
STREAM_OVER = 2.4
PEAK = 0.80
# the bell notes: D major pentatonic over two octaves (Hz), the key of her hum
NOTES = [293.66, 329.63, 369.99, 440.00, 493.88, 587.33, 659.25, 739.99, 880.00, 987.77, 1174.66]


def rng_for(name):
    return np.random.default_rng(zlib.crc32(name.encode('utf-8')))


def tt(dur):
    return np.arange(int(dur * SR)) / SR


# ====================================================================================================================== building blocks
def shaped_noise(rng, n, shape_fn, block=2048):
    """White noise filtered by a time-varying spectral shape: shape_fn(freqs_hz, t_seconds) -> gain per bin. Overlap-add of Hann blocks (no scipy)."""
    hop = block // 2
    win = np.hanning(block)
    out = np.zeros(n + block)
    freqs = np.fft.rfftfreq(block, 1.0 / SR)
    noise = rng.standard_normal(n + block)
    for s in range(0, n, hop):
        seg = noise[s:s + block] * win
        spec = np.fft.rfft(seg) * shape_fn(freqs, s / SR)
        out[s:s + block] += np.fft.irfft(spec, block) * win
    return out[:n] * (1.0 / 1.5)


def band(f, centre, width):
    """A smooth band-pass bump in log frequency (gain 1 at the centre)."""
    f = np.maximum(f, 1.0)
    return np.exp(-0.5 * (np.log2(f / centre) / width) ** 2)


def envelope(n, attack, release, curve=2.0):
    t = np.arange(n) / SR
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    r = np.clip((n / SR - t) / max(release, 1e-4), 0, 1)
    return (a ** 1.5) * (r ** curve)


def water_flow(dur, rng, level=1.0, sweep=None, bubbles=1.0, bright=1.0):
    """Running water: noise through two moving bands (a body and a hiss), plus bubbles (short chirps that rise in pitch), plus a shimmer of high noise. `sweep(t)` moves the body's centre (Hz)."""
    n = int(dur * SR)
    centre = sweep or (lambda t: 900.0 + 250.0 * math.sin(t * 3.1))
    body = shaped_noise(rng, n, lambda f, t: band(f, centre(t), 0.75) * (0.6 + 0.4 * math.sin(t * 7.3 + 1.0) ** 2))
    hiss = shaped_noise(rng, n, lambda f, t: band(f, 5200.0 * bright, 0.6) * (0.5 + 0.5 * math.sin(t * 5.1) ** 2))
    x = body * 1.0 + hiss * 0.28 * bright
    # bubbles
    t = tt(dur)
    cnt = int(dur * 26 * bubbles)
    for _ in range(cnt):
        t0 = rng.uniform(0, max(0.01, dur - 0.06))
        f0 = rng.uniform(380, 1500)
        ln = int(rng.uniform(0.012, 0.04) * SR)
        i0 = int(t0 * SR)
        if i0 + ln >= n:
            continue
        k = np.arange(ln) / SR
        f = f0 * (1.0 + rng.uniform(1.0, 2.2) * k / (ln / SR))
        ph = 2 * math.pi * np.cumsum(f) / SR
        x[i0:i0 + ln] += np.sin(ph) * np.exp(-k * rng.uniform(70, 140)) * rng.uniform(0.15, 0.55) * 1.4
    return x * level


def glass_bell(freq, dur, rng, amp=1.0, bright=1.0):
    """A soft glass bell: five inharmonic partials with their own decays, each a doublet detuned a few cents (it shimmers), a very short soft attack."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    ratios = [1.0, 2.76, 5.40, 8.93, 13.34]
    amps = [1.0, 0.42, 0.22 * bright, 0.10 * bright, 0.05 * bright]
    decays = [dur * 0.55, dur * 0.34, dur * 0.2, dur * 0.12, dur * 0.07]
    x = np.zeros(n)
    for r, a, d in zip(ratios, amps, decays):
        f = freq * r
        if f > 12000:
            continue
        for det in (-1.0, 1.0):
            fd = f * 2 ** (det * rng.uniform(2.0, 6.0) / 1200.0)
            ph = rng.uniform(0, 2 * math.pi)
            x += a * 0.5 * np.sin(2 * math.pi * fd * t + ph) * np.exp(-t / (d / 3.0))
    x *= np.clip(t / 0.004, 0, 1)
    return x * amp


def choir(freq, dur, rng, voices=5, vowel='oo', amp=1.0, wobble=1.0):
    """A low watery choir hum: several detuned voices (harmonic stacks weighted by a vowel's formants), a slow vibrato, a swell, and a wobble in the level like light on a surface."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    formants = {'oo': [(330, 90, 1.0), (760, 120, 0.45), (2400, 300, 0.12)], 'ah': [(700, 110, 1.0), (1150, 140, 0.6), (2600, 300, 0.18)], 'mm': [(250, 70, 1.0), (1100, 200, 0.18), (2200, 300, 0.05)]}[vowel]
    x = np.zeros(n)
    for v in range(voices):
        f0 = freq * 2 ** (rng.uniform(-9, 9) / 1200.0)
        vib = 1.0 + 0.0045 * np.sin(2 * math.pi * (5.0 + rng.uniform(-0.5, 0.5)) * t + rng.uniform(0, 6.28)) * np.clip(t / 0.5, 0, 1)
        phase = 2 * math.pi * np.cumsum(f0 * vib) / SR
        voice = np.zeros(n)
        for h in range(1, 26):
            fh = f0 * h
            if fh > 8000:
                break
            g = sum(a * math.exp(-0.5 * ((fh - c) / w) ** 2) for c, w, a in formants) + 0.02 / h
            voice += (g / h ** 0.35) * np.sin(h * phase + rng.uniform(0, 6.28))
        x += voice * (1.0 + 0.3 * wobble * np.sin(2 * math.pi * (7.0 + rng.uniform(-1, 1)) * t + rng.uniform(0, 6.28)))
    sw = np.minimum(np.clip(t / (dur * 0.45), 0, 1) ** 1.2, np.clip((dur - t) / (dur * 0.4), 0, 1) ** 1.5)
    return x * sw * amp / voices


def thump(dur, f_start, f_end, decay):
    t = tt(dur)
    f = f_end + (f_start - f_end) * np.exp(-t * 12)
    return np.sin(2 * math.pi * np.cumsum(f) / SR) * np.exp(-t * decay)


def crash(dur, rng, power=1.0):
    """A wave crashing: a swell of noise that rises, a loud burst that darkens as it decays, a long hiss of foam, a low thump under it."""
    n = int(dur * SR)
    t = tt(dur)
    swell_len = 0.55
    swell = shaped_noise(rng, n, lambda f, ts: band(f, 500 + 3500 * min(1.0, ts / swell_len), 0.9)) * np.clip(t / swell_len, 0, 1) ** 2 * (t < swell_len + 0.1)
    t0 = swell_len
    after = np.clip(t - t0, 0, None)
    burst = shaped_noise(rng, n, lambda f, ts: band(f, 4200.0 * math.exp(-max(0.0, ts - t0) * 1.4) + 250.0, 1.0)) * (t > t0) * np.exp(-after * 2.3) * np.clip(after / 0.02, 0, 1)
    foam = shaped_noise(rng, n, lambda f, ts: band(f, 6500.0, 0.7)) * (t > t0) * np.exp(-after * 1.1) * 0.35
    th = np.zeros(n)
    i0 = int(t0 * SR)
    seg = thump(min(dur - t0, 0.9), 70.0, 34.0, 4.5)
    th[i0:i0 + len(seg)] = seg
    return (swell * 0.5 + burst * 1.0 + foam + th * 0.8 * power)


def reverb(x, rng, rt60=1.4, wet=0.35, pre=0.012, damp=0.5):
    """A synthetic room: an exponentially decaying noise tail that darkens as it goes, convolved by FFT."""
    ln = int(rt60 * 1.2 * SR)
    t = np.arange(ln) / SR
    ir = rng.standard_normal(ln) * np.exp(-t * 6.9 / rt60)
    spec = np.fft.rfft(ir)
    f = np.fft.rfftfreq(ln, 1.0 / SR)
    ir = np.fft.irfft(spec * np.exp(-damp * f / 6000.0), ln)
    ir = np.concatenate([np.zeros(int(pre * SR)), ir])
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    n = len(x) + len(ir)
    size = 1 << (n - 1).bit_length()
    y = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:len(x) + int(rt60 * 0.5 * SR)]
    out = np.zeros(len(y))
    out[:len(x)] += x * (1 - wet)
    out += y * wet * 1.6
    return out


def put(dst, src, at):
    i = int(at * SR)
    if i >= len(dst):
        return
    m = min(len(src), len(dst) - i)
    dst[i:i + m] += src[:m]


def fit(x, dur, fade=0.06):
    """Cuts or pads to dur seconds and fades the end."""
    n = int(dur * SR)
    y = np.zeros(n)
    m = min(n, len(x))
    y[:m] = x[:m]
    k = int(fade * SR)
    y[-k:] *= np.linspace(1, 0, k) ** 1.5
    return y


def highpass(x, hz=40.0):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1.0 / SR)
    spec *= 1.0 / (1.0 + (hz / np.maximum(f, 1e-3)) ** 4)
    return np.fft.irfft(spec, len(x))


# ====================================================================================================================== recipes (one per cue; variant 1..n)
def pick(rng, k, lo=0, hi=None):
    hi = hi or len(NOTES)
    return [NOTES[int(i)] for i in rng.integers(lo, hi, k)]


def arms(x):
    """RMS of the active part of a track (where it is above 2 percent of its peak)."""
    pk = float(np.max(np.abs(x))) if len(x) else 0.0
    if pk < 1e-9:
        return 1e-9
    act = x[np.abs(x) > 0.02 * pk]
    return math.sqrt(float(np.mean(act ** 2)))


def mix(n, tracks):
    """Sums tracks, each brought to a loudness (dB, relative) of its own first: the tonal parts (bells, hum) stay in front of the water, whatever the noise happens to measure."""
    out = np.zeros(n)
    for x, db in tracks:
        if x is None:
            continue
        y = np.zeros(n)
        m = min(n, len(x))
        y[:m] = x[:m]
        out += y * (10 ** (db / 20.0) / arms(y))
    return out


def track(n):
    return np.zeros(n)


def r_ambient(v, rng):
    dur = 2.7 + 0.15 * (v % 2)
    n = int(dur * SR)
    water = water_flow(dur, rng, 1.0, sweep=lambda t: 750 + 300 * math.sin(t * (2.0 + v)), bubbles=0.9) * envelope(n, 0.3, 0.6, 1.2)
    hum = choir([146.83, 110.0, 130.81][v - 1], dur, rng, voices=5, vowel='mm' if v == 2 else 'oo')                # D3, A2, C3: a low hum under it
    bells = track(n)
    for k, f in enumerate(pick(rng, 2, 3, 9)):
        put(bells, glass_bell(f, 1.5, rng, 1.0), 0.35 + 0.9 * k + 0.2 * v)
    return reverb(mix(n, [(water, -10.0), (hum, -4.0), (bells, -2.0)]), rng, 1.3, 0.4)


def r_link(v, rng):
    dur = 2.3
    n = int(dur * SR)
    water = water_flow(dur, rng, 1.0, sweep=lambda t: 400 + 1600 * min(1.0, t / 1.4), bubbles=2.0 + v) * envelope(n, 0.05, 0.5, 1.5)
    bells = track(n)
    for k, f in enumerate([NOTES[i] for i in ([3, 5, 7, 9] if v == 1 else [2, 4, 6, 8, 10])]):
        put(bells, glass_bell(f, 1.2, rng, 1.0), 0.25 + 0.14 * k)
    hum = track(n)
    put(hum, choir(146.83 if v == 1 else 164.81, 1.8, rng, 5, 'ah'), 0.45)
    return reverb(mix(n, [(water, -9.0), (hum, -3.0), (bells, -1.0)]), rng, 1.6, 0.42)


def r_unlink(v, rng):
    dur = 1.7
    n = int(dur * SR)
    water = water_flow(dur, rng, 1.0, sweep=lambda t: 2000 - 1500 * min(1.0, t / 1.2), bubbles=1.6) * envelope(n, 0.02, 0.8, 1.6)
    bells = track(n)
    for k, f in enumerate([NOTES[i] for i in ([8, 6, 4, 2] if v == 1 else [9, 7, 5, 3])]):
        put(bells, glass_bell(f, 0.9, rng, 1.0), 0.05 + 0.15 * k)
    hum = track(n)
    put(hum, choir(130.81, 1.3, rng, 4, 'oo'), 0.1)
    return reverb(mix(n, [(water, -9.0), (hum, -4.0), (bells, -1.0)]), rng, 1.2, 0.4)


def r_cast(v, rng):
    dur = 1.0
    n = int(dur * SR)
    t = np.arange(n) / SR
    swish = shaped_noise(rng, n, lambda f, ts: band(f, 500 + 3600 * min(1.0, ts / 0.25), 0.8)) * np.exp(-np.clip(t - 0.05, 0, None) * 7.0) * np.clip(t / 0.01, 0, 1)
    bells = track(n)
    f = pick(rng, 1, 5, 10)[0] * (1.0 if v != 2 else 0.75)
    put(bells, glass_bell(f, 0.8, rng, 1.0), 0.07)
    put(bells, glass_bell(f * 1.5, 0.6, rng, 0.45), 0.11)
    fl = track(n)
    put(fl, water_flow(0.6, rng, 1.0, sweep=lambda t: 1200, bubbles=3.0) * envelope(int(0.6 * SR), 0.01, 0.4, 1.5), 0.06)
    return reverb(mix(n, [(swish, -5.0), (fl, -9.0), (bells, -1.0)]), rng, 0.9, 0.3)


def r_full(v, rng):
    dur = 3.0
    n = int(dur * SR)
    t = np.arange(n) / SR
    swell = shaped_noise(rng, n, lambda f, ts: band(f, 300 + 2800 * min(1.0, ts / 1.6), 0.85)) * (np.clip(t / 1.6, 0, 1) ** 2) * np.exp(-np.clip(t - 1.9, 0, None) * 1.4)
    water = water_flow(dur, rng, 1.0, sweep=lambda t: 600 + 1200 * min(1.0, t / 1.5), bubbles=2.0) * envelope(n, 0.2, 1.0, 1.4)
    root = 146.83 if v == 1 else 130.81
    hum = track(n)
    for k, mult in enumerate((1.0, 1.4983, 2.0)):                                      # an open fifth and the octave, in the choir
        put(hum, choir(root * mult, 2.4, rng, 4, 'ah' if k == 1 else 'oo'), 0.5 + 0.08 * k)
    bells = track(n)
    for k, f in enumerate([NOTES[i] for i in ([5, 7, 9] if v == 1 else [4, 6, 8, 10])]):
        put(bells, glass_bell(f, 1.6, rng, 1.0), 1.3 + 0.13 * k)
    return reverb(mix(n, [(swell, -6.0), (water, -10.0), (hum, -2.0), (bells, -2.0)]), rng, 1.9, 0.42)


def r_crash(v, rng):
    dur = 2.6
    n = int(dur * SR)
    wave_ = crash(dur, rng, 1.0 + 0.2 * (v - 1))
    bells = track(n)
    put(bells, glass_bell(NOTES[7 if v == 1 else 6], 1.5, rng, 1.0), 0.62)
    return reverb(mix(n, [(wave_, 0.0), (bells, -12.0)]), rng, 1.5, 0.3)


def r_hurt(v, rng):
    dur = 0.7
    n = int(dur * SR)
    t = tt(dur)
    splash = shaped_noise(rng, n, lambda f, ts: band(f, 1800 - 900 * ts, 0.9)) * np.exp(-t * 9.0)
    f0 = 1046.5 if v == 1 else 880.0
    f = f0 * (1.0 - 0.28 * np.clip(t / 0.4, 0, 1))
    ting = np.sin(2 * math.pi * np.cumsum(f) / SR) * np.exp(-t * 8.0) + 0.8 * np.sin(2 * math.pi * np.cumsum(f * 1.06) / SR) * np.exp(-t * 8.0)       # a beating pair: a cracked glass
    bub = track(n)
    put(bub, water_flow(0.3, rng, 1.0, bubbles=4.0), 0.0)
    return reverb(mix(n, [(splash, -6.0), (ting, -1.0), (bub, -12.0)]), rng, 0.7, 0.25)


def r_death(v, rng):
    dur = 2.6
    n = int(dur * SR)
    water = water_flow(dur, rng, 1.0, sweep=lambda t: 2200 * math.exp(-t * 1.2) + 150, bubbles=2.0) * envelope(n, 0.02, 1.2, 1.3)
    bells = track(n)
    for k, f in enumerate([NOTES[i] for i in (9, 7, 5, 3, 1, 0)]):
        put(bells, glass_bell(f, 1.2, rng, 1.0 - 0.1 * k), 0.1 + 0.2 * k)
    hum = track(n)
    put(hum, choir(110.0, 2.2, rng, 5, 'oo'), 0.15)
    return reverb(mix(n, [(water, -9.0), (hum, -3.0), (bells, -1.0)]), rng, 1.8, 0.4)


def r_hum(v, rng):
    """The stand-in for a spoken line: a short sung phrase of three soft notes on 'ah' / 'mm' with a bell under the last one (her voice without words, used when a group has no recorded line)."""
    dur = 2.0
    n = int(dur * SR)
    phrase = [[293.66, 329.63, 440.00], [329.63, 293.66, 369.99], [440.00, 369.99, 293.66]][v - 1]
    voice = track(n)
    for k, f in enumerate(phrase):
        put(voice, choir(f / 2.0, 0.85, rng, 4, 'ah' if k < 2 else 'oo', 1.0, 0.6), 0.1 + 0.42 * k)
    bells = track(n)
    put(bells, glass_bell(phrase[-1] * 2.0, 1.2, rng, 0.8), 0.1 + 0.42 * 2 + 0.2)
    water = water_flow(dur, rng, 0.6, sweep=lambda t: 900, bubbles=0.6) * envelope(n, 0.3, 0.7, 1.4)
    return reverb(mix(n, [(voice, -2.0), (bells, -6.0), (water, -17.0)]), rng, 1.5, 0.4)


RECIPES = {'ambient': r_ambient, 'link': r_link, 'unlink': r_unlink, 'cast': r_cast, 'full': r_full, 'crash': r_crash, 'hurt': r_hurt, 'death': r_death, 'hum': r_hum}


# ====================================================================================================================== mastering and files
def master(x, target_db, dur_max=3.0):
    x = highpass(x, 45.0)
    n = min(len(x), int(dur_max * SR))
    x = fit(x, n / SR, 0.12)
    act = x[np.abs(x) > 0.02 * np.max(np.abs(x))]
    rms = math.sqrt(float(np.mean(act ** 2))) if len(act) else 1e-6
    x = x * (10 ** (target_db / 20.0) / rms)
    pk = float(np.max(np.abs(x)))
    if pk > PEAK:                                                                      # a soft knee, not a hard clip
        x = np.tanh(x / PEAK * 1.2) / math.tanh(1.2) * PEAK
    return np.clip(x, -PEAK, PEAK)


def write_wav(path, x):
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype('<i2').tobytes())


def encode(wav_path, ogg_path):
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav_path, '-ac', '1', '-ar', str(SR), '-c:a', 'libvorbis', '-q:a', '4', '-map_metadata', '-1', '-fflags', '+bitexact', '-flags:a', '+bitexact', ogg_path], check=True)


def spectrogram_png(x, path):
    try:
        from PIL import Image
    except ImportError:
        return
    n_fft, hop = 1024, 256
    win = np.hanning(n_fft)
    frames = max(1, (len(x) - n_fft) // hop + 1)
    cols = [np.abs(np.fft.rfft(np.pad(x[k * hop:k * hop + n_fft], (0, max(0, n_fft - len(x[k * hop:k * hop + n_fft])))) * win)) for k in range(frames)]
    s = 20 * np.log10(np.array(cols).T + 1e-7)
    s = np.clip((s - (s.max() - 80.0)) / 80.0, 0, 1)
    img = (np.flipud(s[:400]) * 255).astype(np.uint8)
    Image.fromarray(img).resize((min(1000, img.shape[1] * 2), 300)).save(path)


def seconds_of(path):
    r = subprocess.run(['ffmpeg', '-loglevel', 'error', '-i', path, '-f', 's16le', '-ac', '1', '-'], capture_output=True, check=True)
    y = np.frombuffer(r.stdout, dtype='<i2').astype(np.float64) / 32768.0
    return y


def build(only, preview):
    os.makedirs(SOUND_DIR, exist_ok=True)
    out = {}
    for cue, (variants, att, verb, db) in CUES.items():
        if only and only != cue:
            continue
        for v in range(1, variants + 1):
            name = '%s_%d' % (cue, v)
            rng = rng_for('undine.' + name)
            x = master(RECIPES[cue](v, rng), db)
            with tempfile.TemporaryDirectory() as td:
                wav = os.path.join(td, name + '.wav')
                write_wav(wav, x)
                ogg = os.path.join(SOUND_DIR, name + '.ogg')
                encode(wav, ogg)
                if preview:
                    os.makedirs(preview, exist_ok=True)
                    write_wav(os.path.join(preview, name + '.wav'), x)
                    spectrogram_png(x, os.path.join(preview, name + '.png'))
            y = seconds_of(os.path.join(SOUND_DIR, name + '.ogg'))
            out[name] = (len(y) / SR, float(np.max(np.abs(y))), 20 * math.log10(math.sqrt(float(np.mean(y ** 2))) + 1e-9))
            print('%-12s %5.2f s  decoded peak %.2f  rms %6.1f dBFS' % (name, *out[name]))
    return out


def entry(cue, lengths):
    variants, att, verb, db = CUES[cue]
    lines = ['  "undine.%s": {' % cue, '    "subtitle": "subtitles.nusmp.undine.%s",' % cue, '    "sounds": [']
    items = []
    for v in range(1, variants + 1):
        path = os.path.join(SOUND_DIR, '%s_%d.ogg' % (cue, v))
        sec = (lengths.get('%s_%d' % (cue, v)) or (None,))[0]
        if sec is None:
            sec = len(seconds_of(path)) / SR
        stream = ', "stream": true' if sec >= STREAM_OVER else ''
        items.append('      { "name": "nusmp:undine/%s_%d", "attenuation_distance": %d%s }' % (cue, v, att, stream))
    lines.append(',\n'.join(items))
    lines += ['    ]', '  }']
    return '\n'.join(lines)


def write_manifest(lengths):
    text = open(SOUNDS_JSON, encoding='utf-8').read()
    cut = text.find('\n  "undine.')
    body = text[:cut] if cut >= 0 else text.rstrip()[:-1].rstrip()
    entries = ',\n'.join(entry(c, lengths) for c in CUES)
    new = body.rstrip().rstrip(',') + ',\n' + entries + '\n}\n'
    json.loads(new)                                                                    # it must still be valid JSON
    with open(SOUNDS_JSON, 'w', encoding='utf-8', newline='\n') as f:
        f.write(new)
    keep = {}
    if os.path.exists(LANG_OUT):                                                          # the subtitles of the spoken lines belong to tools/audio/undine_voice_convert.py: kept as they are
        keep = {k: v for k, v in json.load(open(LANG_OUT, encoding='utf-8')).items() if k.startswith('subtitles.nusmp.voice.undine_')}
    lang = {'subtitles.nusmp.undine.%s' % c: 'Undine %s' % CUES[c][2] for c in CUES}
    lang.update(keep)
    lang.update({
        'key.nusmp.undine_link': 'Undine (link or unlink Noelle\'s water spirit)',
        'nusmp.undine.spirit.full': 'Undine cannot answer: too many spirits are out.',
        'nusmp.undine.spirit.linked': 'Undine rises from the water beside you.',
        'nusmp.undine.spirit.unlinked': 'Undine sinks back into the water.',
    })
    with open(LANG_OUT, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write('\n')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--only')
    ap.add_argument('--preview')
    ap.add_argument('--report', action='store_true')
    a = ap.parse_args()
    if a.report:
        for cue, (variants, *_rest) in CUES.items():
            for v in range(1, variants + 1):
                y = seconds_of(os.path.join(SOUND_DIR, '%s_%d.ogg' % (cue, v)))
                print('%s_%d  %5.2f s  peak %.2f' % (cue, v, len(y) / SR, np.max(np.abs(y))))
        return
    lengths = build(a.only, a.preview)
    if not a.only:
        write_manifest(lengths)


if __name__ == '__main__':
    main()
