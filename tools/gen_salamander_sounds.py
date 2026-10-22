#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the Salamander's creature sounds (docs/magic/fire_casting.md, 4.2). The Salamander does not speak, so there is no word in any of them: a rising trill and a short chirp, a soft growl, a hiss, a
small flame whoosh, a content purr-rumble, a squeaky yawn, a whimper, the flap of small leathery wings, and for the big golden form a roar and the heavy beats of its wings. NOTHING is recorded or taken
from anywhere: every file is made from nothing with numpy (the DSP kit of tools/gen_mob_sounds.py, the same one tools/gen_sylph_sounds.py uses) and encoded to mono Vorbis with ffmpeg. It is
reproducible: every variant is seeded from its own name, so a re-run writes the same audio.

  python3 -B tools/gen_salamander_sounds.py                 write every file, the salamander.* entries of sounds.json (every other entry is kept, byte for byte) and the subtitle lines of
                                                            tools/lang/salamander_moods.json (every other line of that fragment is kept)
  python3 -B tools/gen_salamander_sounds.py --preview DIR   also write a .wav and a spectrogram .png per file into DIR (to look at them without the game)
  python3 -B tools/gen_salamander_sounds.py --check         verify that every file sounds.json names exists, is a valid Ogg and short

The cues are the ones of src/main/java/.../firecast/SalamanderSoundSet (a test keeps the table, the files, sounds.json and the subtitles in step): trill, chirp, growl, hiss, whoosh, purr, yawn, whimper,
flap, roar, dragon_flap (25 files in all, a few hundred KB).
"""
from __future__ import annotations

import json
import math
import os
import re
import sys
import tempfile
import zlib

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_mob_sounds as M  # noqa: E402  (the DSP kit: Ctx, filters, reverb, encoder, Ogg reader)

ROOT = M.ROOT
SOUND_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp', 'sounds', 'salamander')
SOUNDS_JSON = M.SOUNDS_JSON
LANG_OUT = os.path.join(ROOT, 'tools', 'lang', 'salamander_moods.json')
SR = 44100
PREFIX = 'salamander.'

# cue -> (variants, loudness dBFS rms of the active part, subtitle). Keep in step with SalamanderSoundSet.java.
CUES = {
    'trill': (3, -24.0, 'Salamander trills'),
    'chirp': (3, -24.0, 'Salamander chirps'),
    'growl': (2, -21.0, 'Salamander growls'),
    'hiss': (2, -22.0, 'Salamander hisses'),
    'whoosh': (2, -23.0, 'Salamander breathes a small flame'),
    'purr': (2, -27.0, 'Salamander purrs'),
    'yawn': (2, -26.0, 'Salamander yawns'),
    'whimper': (2, -24.0, 'Salamander whimpers'),
    'flap': (2, -26.0, 'Salamander flaps its wings'),
    'roar': (3, -17.0, 'Salamander roars'),
    'dragon_flap': (2, -21.0, 'The great Salamander beats its wings'),
}
for _k, (_n, _l, _s) in CUES.items():
    M.LOUDNESS[_k] = _l


def seed_for(cue, index):
    return zlib.crc32(('nusmp-salamander:%s.%d' % (cue, index)).encode('utf-8')) & 0xFFFFFFFF


# ====================================================================================================================== the voices
def osc(c, f_pts, dur, harm=(1.0, 0.35, 0.15), vib=(0.0, 0.0), jitter=0.0, am=None):
    """A tone whose pitch follows f_pts ([(t 0..1, Hz)]), with a few harmonics, a vibrato (rate, depth) and a little pitch jitter."""
    n = c.n(dur)
    t = np.arange(n) / c.sr
    f = M.curve(f_pts, n)
    if vib[1] > 0:
        f = f * (1.0 + vib[1] * np.sin(2 * np.pi * vib[0] * t + c.rng.uniform(0, 6.28)))
    if jitter > 0:
        f = f * (1.0 + jitter * M.lp(M.white(c, n), 60.0, c.sr, 1) * 6.0)
    ph = 2 * np.pi * np.cumsum(f) / c.sr
    y = np.zeros(n)
    for k, a in enumerate(harm):
        y += a * np.sin((k + 1) * ph)
    if am is not None:
        y = y * am(t)
    return M.norm(y)


def breath(c, dur, lo=1800.0, hi=7000.0, level=1.0, shape=None):
    n = c.n(dur)
    y = M.hp(M.lp(M.white(c, n), hi, c.sr, 2), lo, c.sr, 2)
    if shape is not None:
        y = y * M.curve(shape, n)
    return M.norm(y) * level


def gesture(c, dur, f_pts, shape, harm=(1.0, 0.35, 0.15), vib=(0.0, 0.0), air=0.15, air_lo=2200.0, wet=0.18, jitter=0.0):
    """One creature call: a tone with a contour, an amplitude shape, a breath of air under it and a touch of room."""
    n = c.n(dur)
    y = osc(c, f_pts, dur, harm, vib, jitter) * M.curve(shape, n)
    if air > 0:
        y = y + air * breath(c, dur, air_lo, 8000.0, 1.0, shape)
    return M.reverb(y, c.sr, 0.45, wet) if wet > 0 else y


# ====================================================================================================================== the cues
def r_trill(c, i):
    """A rising reptile trill: a bright tone that rises while a fast vibrato wobbles it (22 to 30 times a second), two or three little steps up."""
    dur = (0.55, 0.46, 0.70)[i - 1]
    base = (880.0, 1020.0, 780.0)[i - 1]
    top = base * (1.55, 1.75, 1.45)[i - 1]
    rate = (24.0, 29.0, 22.0)[i - 1]
    y = gesture(c, dur, [(0, base), (0.35, base * 1.18), (0.7, top), (1, top * 0.97)], [(0, 0), (0.08, 1), (0.7, 0.9), (1, 0)], (1.0, 0.45, 0.2), (rate, 0.055), 0.10, 2600.0, 0.2, 0.002)
    return y


def r_chirp(c, i):
    """One to three short happy chirps, each a little up-down blip."""
    count = (1, 2, 3)[i - 1]
    n = c.n(0.14 * count + 0.2)
    out = np.zeros(n)
    t0 = 0.0
    for k in range(count):
        f = (1250.0, 1500.0, 1350.0)[i - 1] * (1.0 + 0.12 * k)
        b = gesture(c, 0.11, [(0, f * 0.9), (0.4, f * 1.25), (1, f * 1.05)], [(0, 0), (0.12, 1), (0.6, 0.7), (1, 0)], (1.0, 0.3), (0.0, 0.0), 0.08, 3000.0, 0.0)
        out = M.place(out, b, int(t0 * c.sr), 1.0 - 0.15 * k)
        t0 += 0.14
    return M.reverb(out, c.sr, 0.4, 0.15)


def r_growl(c, i):
    """A soft growl in the throat: a low rough tone (a 40 Hz roughness on 95 to 120 Hz), formants of a small throat, a rumble of breath."""
    dur = (0.95, 1.2)[i - 1]
    f0 = (118.0, 96.0)[i - 1]
    n = c.n(dur)
    t = np.arange(n) / c.sr
    y = osc(c, [(0, f0 * 0.9), (0.3, f0 * 1.1), (0.75, f0), (1, f0 * 0.8)], dur, (1.0, 0.7, 0.5, 0.35, 0.2), (5.0, 0.012), 0.004)
    y = y * (0.55 + 0.45 * np.sin(2 * np.pi * (38.0 + 6.0 * i) * t + c.rng.uniform(0, 6.28)) ** 2)
    y = M.bp(y, 320.0, 0.8, c.sr) * 0.7 + M.bp(y, 900.0, 1.2, c.sr) * 0.5 + 0.25 * M.lp(M.white(c, n), 700.0, c.sr, 2)
    y = M.norm(y) * M.curve([(0, 0), (0.12, 1), (0.7, 0.9), (1, 0)], n)
    return M.reverb(M.sat(y, 1.6), c.sr, 0.5, 0.15)


def r_hiss(c, i):
    """A hiss: noise through a high band, a fast attack, a slow fall, with a slight rhythm of breath in it."""
    dur = (0.65, 0.85)[i - 1]
    n = c.n(dur)
    t = np.arange(n) / c.sr
    y = breath(c, dur, 3500.0, 9500.0, 1.0, [(0, 0), (0.05, 1), (0.5, 0.8), (1, 0)]) * (0.8 + 0.2 * np.sin(2 * np.pi * (9.0 + 3 * i) * t))
    y = y + 0.35 * M.bp(M.white(c, n), 1800.0, 1.0, c.sr) * M.curve([(0, 0), (0.08, 1), (0.6, 0.4), (1, 0)], n)
    return M.reverb(M.norm(y), c.sr, 0.35, 0.1)


def r_whoosh(c, i):
    """A small flame whoosh: a band of noise that opens upward and closes, a bed of low rush, and a few sparks."""
    dur = (0.75, 0.9)[i - 1]
    f1 = (2600.0, 3200.0)[i - 1]
    x = M.whoosh(c, dur, 380.0, f1, 1.1, [(0, 0), (0.22, 1), (0.55, 0.75), (1, 0)])
    x = x + 0.45 * M.lp(M.whoosh(c, dur, 120.0, 700.0, 0.9, [(0, 0), (0.3, 1), (0.7, 0.8), (1, 0)]), 900.0, c.sr, 1)
    x = M.place(x, M.crackles(c, dur * 0.7, 55.0, (2800.0, 7000.0), 0.003) * 0.35, int(0.12 * c.sr))
    return M.reverb(M.norm(x), c.sr, 0.6, 0.18)


def r_purr(c, i):
    """A content purr-rumble: a low tone and low noise chopped by a slow 22 to 26 Hz amplitude wobble, swelling and fading."""
    dur = (1.7, 2.1)[i - 1]
    n = c.n(dur)
    t = np.arange(n) / c.sr
    rate = (23.0, 26.0)[i - 1]
    gate = 0.5 + 0.5 * np.sin(2 * np.pi * rate * t + 0.3 * np.sin(2 * np.pi * 0.7 * t)) ** 2
    y = osc(c, [(0, 68.0), (0.5, 75.0), (1, 64.0)], dur, (1.0, 0.8, 0.5, 0.3), (0.6, 0.01), 0.003)
    y = y + 0.6 * M.lp(M.white(c, n), 420.0, c.sr, 2) + 0.12 * M.bp(M.white(c, n), 1100.0, 1.0, c.sr)
    y = y * gate * M.curve([(0, 0), (0.25, 1), (0.7, 0.9), (1, 0)], n)
    return M.reverb(M.norm(y), c.sr, 0.5, 0.15)


def r_yawn(c, i):
    """A small squeaky yawn: a breathy rise to a held, wavering vowel-like note that falls and fades into a sigh."""
    dur = (1.25, 1.5)[i - 1]
    top = (760.0, 640.0)[i - 1]
    y = gesture(c, dur, [(0, top * 0.55), (0.28, top), (0.55, top * 0.92), (1, top * 0.45)], [(0, 0), (0.2, 0.8), (0.5, 1), (0.85, 0.5), (1, 0)], (1.0, 0.5, 0.25, 0.1), (5.5, 0.02), 0.34, 1400.0, 0.2, 0.003)
    return y


def r_whimper(c, i):
    """A low whimper: two or three falling whines, each wavering, a breath in between."""
    count = (2, 3)[i - 1]
    n = c.n(0.55 * count + 0.2)
    out = np.zeros(n)
    t0 = 0.0
    for k in range(count):
        f = (820.0, 740.0)[i - 1] * (1.0 - 0.07 * k)
        b = gesture(c, 0.5, [(0, f), (0.2, f * 1.08), (1, f * 0.62)], [(0, 0), (0.15, 1), (0.65, 0.7), (1, 0)], (1.0, 0.4, 0.15), (6.5, 0.03), 0.12, 1800.0, 0.0, 0.003)
        out = M.place(out, b, int(t0 * c.sr), 1.0 - 0.12 * k)
        t0 += 0.52
    return M.reverb(out, c.sr, 0.6, 0.2)


def flap_beat(c, f_lo, f_hi, dur, level=1.0):
    """One wing beat: a short rush of leathery air and a low soft thump."""
    n = c.n(dur)
    t = np.arange(n) / c.sr
    air = M.bp(M.white(c, n), (f_lo + f_hi) / 2.0, 0.9, c.sr) * np.exp(-t / (dur * 0.35)) * np.minimum(1.0, t / 0.006)
    thump = np.sin(2 * np.pi * (f_lo * 0.28 + f_lo * 0.2 * np.exp(-t / 0.03)) * t) * np.exp(-t / (dur * 0.3)) * 0.6
    return M.norm(air + thump) * level


def r_flap(c, i):
    """A few quick flaps of small leathery wings."""
    beats = (4, 5)[i - 1]
    gap = (0.085, 0.07)[i - 1]
    n = c.n(gap * beats + 0.25)
    out = np.zeros(n)
    for k in range(beats):
        out = M.place(out, flap_beat(c, 700.0, 2600.0, 0.11, 1.0 - 0.1 * k), int((k * gap + c.rng.uniform(0, 0.008)) * c.sr))
    return M.reverb(out, c.sr, 0.4, 0.12)


def r_dragon_flap(c, i):
    """The heavy beats of the big form's wings: a slow, low, wide rush with a deep thump on each beat."""
    beats = (3, 4)[i - 1]
    gap = (0.30, 0.26)[i - 1]
    n = c.n(gap * beats + 0.5)
    out = np.zeros(n)
    for k in range(beats):
        out = M.place(out, flap_beat(c, 160.0, 900.0, 0.3, 1.0 - 0.08 * k), int((k * gap + c.rng.uniform(0, 0.012)) * c.sr))
    return M.reverb(M.sat(out, 1.3), c.sr, 0.9, 0.22)


def r_roar(c, i):
    """The big form's roar: a deep voiced growl that swells and rises, with a rush of fire noise and a hall behind it."""
    dur = (1.7, 2.0, 1.45)[i - 1]
    V = dict(f0=(72.0, 64.0, 82.0)[i - 1], tract=0.58, rough=0.62)
    y = M.roar(c, V, dur, f0=V['f0'], rise=(0.35, 0.28, 0.4)[i - 1], noise=0.5, sub=0.9, drive=1.2)
    n = len(y)
    fire = M.lp(M.white(c, n), 1800.0, c.sr, 2) * M.curve([(0, 0), (0.2, 0.8), (0.55, 1), (1, 0)], n)
    y = M.norm(y) + 0.28 * M.norm(fire)
    return M.reverb(M.norm(y), c.sr, 1.4, 0.3)


RECIPES = {'trill': r_trill, 'chirp': r_chirp, 'growl': r_growl, 'hiss': r_hiss, 'whoosh': r_whoosh, 'purr': r_purr, 'yawn': r_yawn, 'whimper': r_whimper,
           'flap': r_flap, 'roar': r_roar, 'dragon_flap': r_dragon_flap}


# ====================================================================================================================== writing
def render(cue, index):
    c = M.Ctx(SR, np.random.default_rng(seed_for(cue, index)))
    return M.finalize(RECIPES[cue](c, index), SR, cue)


def generate(preview=None, quality=3):
    os.makedirs(SOUND_DIR, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for cue, (variants, _l, _s) in CUES.items():
            for i in range(1, variants + 1):
                x = render(cue, i)
                wav = os.path.join(tmp, '%s_%d.wav' % (cue, i))
                ogg = os.path.join(SOUND_DIR, '%s_%d.ogg' % (cue, i))
                for _attempt in range(6):
                    M.write_wav(wav, x, SR)
                    M.encode(wav, ogg, SR, quality)
                    if M.decoded_peak(ogg) <= M.DECODED_PEAK_LIMIT:
                        break
                    x = x * 0.9
                if preview:
                    os.makedirs(preview, exist_ok=True)
                    M.write_wav(os.path.join(preview, '%s_%d.wav' % (cue, i)), x, SR)
                    M.spectrogram_png(x, SR, os.path.join(preview, '%s_%d.png' % (cue, i)))
                print('%-12s %d  %.2f s  %5d bytes' % (cue, i, len(x) / SR, os.path.getsize(ogg)))


def manifest():
    entries, lang = {}, {}
    for cue, (variants, _l, text) in CUES.items():
        sub = 'subtitles.nusmp.salamander.' + cue
        lang[sub] = text
        sounds = []
        for i in range(1, variants + 1):
            path = os.path.join(SOUND_DIR, '%s_%d.ogg' % (cue, i))
            if not os.path.exists(path):
                raise SystemExit('missing %s (run the generator first)' % path)
            s = {'name': 'nusmp:salamander/%s_%d' % (cue, i)}
            if M.ogg_seconds(path) >= M.STREAM_OVER:
                s['stream'] = True
            sounds.append(s)
        entries[PREFIX + cue] = {'subtitle': sub, 'sounds': sounds}
    return entries, lang


def write_manifest():
    """Appends the salamander.* entries to the END of sounds.json: every entry that is not ours stays byte for byte, in its place; our own old block is replaced in place."""
    entries, lang = manifest()
    with open(SOUNDS_JSON, encoding='utf-8') as f:
        text = f.read()
    ours = ',\n'.join(M._entry_text(k, v) for k, v in entries.items())
    text = re.sub(r',\n  "salamander\.[a-z_]+": \{.*?\n  \}', '', text, flags=re.S)             # our own old block goes (and only ours); everything else stays byte for byte
    body = text.rstrip()
    assert body.endswith('}')
    text = body[:-1].rstrip() + ',\n' + ours + '\n}\n'
    json.loads(text)                                                                         # never write a file that does not parse
    with open(SOUNDS_JSON, 'w', encoding='utf-8') as f:
        f.write(text)
    try:
        with open(LANG_OUT, encoding='utf-8') as f:
            old = json.load(f)
    except FileNotFoundError:
        old = {}
    old = {k: v for k, v in old.items() if not k.startswith('subtitles.nusmp.salamander.')}
    old.update(lang)
    with open(LANG_OUT, 'w', encoding='utf-8') as f:
        json.dump(old, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('sounds.json: %d salamander events; %d subtitles' % (len(entries), len(lang)))


def check():
    with open(SOUNDS_JSON, encoding='utf-8') as f:
        doc = json.load(f)
    bad = []
    for cue, (variants, _l, _s) in CUES.items():
        e = doc.get(PREFIX + cue)
        if not e:
            bad.append('no entry ' + PREFIX + cue)
            continue
        for s in e['sounds']:
            name = s['name'] if isinstance(s, dict) else s
            path = os.path.join(SOUND_DIR, os.path.basename(name) + '.ogg')
            if not os.path.exists(path):
                bad.append('missing ' + path)
            elif M.ogg_seconds(path) > M.MAX_SECONDS + 0.05:
                bad.append('too long ' + path)
    print('check: %s' % ('ok' if not bad else bad))
    return not bad


def main():
    args = sys.argv[1:]
    if '--check' in args:
        sys.exit(0 if check() else 1)
    preview = args[args.index('--preview') + 1] if '--preview' in args else None
    generate(preview)
    write_manifest()


if __name__ == '__main__':
    main()
