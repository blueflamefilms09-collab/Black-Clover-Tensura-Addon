#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Synthesises the Sylph's own sound set (docs/magic/sylph.md): wind chimes, the light flutter of dragonfly wings, a giggle that tinkles, gust whooshes, and the swell of her powered form. NOTHING is
recorded or taken from anywhere and none of it is the basic Tensura spirit's sound: every file is made from nothing with numpy (the DSP kit of tools/gen_mob_sounds.py) and encoded to mono Vorbis
with ffmpeg. It is reproducible: every variant is seeded from its own name, so a re-run writes the same audio.

  python3 -B tools/gen_sylph_sounds.py                 write every file, the sylph.* entries of sounds.json (every other entry is kept) and tools/lang/sylph.json (the subtitle lines; every other line of that fragment is kept)
  python3 -B tools/gen_sylph_sounds.py --preview DIR   also write a .wav and a spectrogram .png per file into DIR (to look at them without the game)
  python3 -B tools/gen_sylph_sounds.py --check         verify that every file sounds.json names exists, is a valid Ogg and short

The cues are the ones of src/main/java/.../sylph/SylphSoundSet (a test keeps the table, the files and sounds.json in step): link, unlink, chime, flutter, giggle, gust, surge, gale, and (the moods) hum, gasp, whimper, cheer, pout.
  --only hum,gasp   makes just those cues (the others are left as they are)
"""
from __future__ import annotations

import json
import math
import os
import sys
import tempfile
import zlib

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_mob_sounds as M  # noqa: E402  (the DSP kit: Ctx, filters, reverb, encoder, Ogg reader)

ROOT = M.ROOT
SOUND_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'nusmp', 'sounds', 'sylph')
SOUNDS_JSON = M.SOUNDS_JSON
LANG_OUT = os.path.join(ROOT, 'tools', 'lang', 'sylph.json')
SR = 44100

# cue -> (variants, loudness dBFS rms of the active part, subtitle). Keep in step with SylphSoundSet.java.
CUES = {
    'link': (2, -22.0, 'Sylph\'s chimes glitter'),
    'unlink': (2, -23.0, 'Sylph\'s chimes fade away'),
    'chime': (3, -26.0, 'Sylph\'s wind chime rings'),
    'flutter': (3, -27.0, 'Sylph\'s wings flutter'),
    'giggle': (3, -22.0, 'Sylph giggles'),
    'gust': (3, -21.0, 'Sylph\'s gust whooshes'),
    'surge': (2, -20.0, 'Sylph surges with wind'),
    'gale': (2, -19.0, 'Sylph\'s gale howls'),
    # 1.99.0 (moods): vocal stand-ins for the moods whose spoken lines are missing (sylph/SylphVoiceLines.fallbackCue): wordless, bell-voiced, synthesised like the rest
    'hum': (2, -25.0, 'Sylph hums'),
    'gasp': (2, -23.0, 'Sylph gasps'),
    'whimper': (2, -24.0, 'Sylph whimpers'),
    'cheer': (2, -21.0, 'Sylph cheers'),
    'pout': (2, -25.0, 'Sylph huffs'),
}
for _k, (_n, _l, _s) in CUES.items():
    M.LOUDNESS[_k] = _l

PENTA = [659.25, 739.99, 830.61, 987.77, 1108.73, 1318.51, 1479.98, 1661.22, 1975.53, 2217.46]       # a bright pentatonic run (E major pentatonic, E5 .. C#7)
TUBE = [1.0, 2.76, 5.40, 8.93]                                                                      # the partials of a struck metal tube (inharmonic: the sound of a chime)


def seed_for(cue, index):
    return zlib.crc32(('nusmp-sylph:%s.%d' % (cue, index)).encode('utf-8')) & 0xFFFFFFFF


# ====================================================================================================================== the voices
def bell(c, f, dur, bright=1.0, tau=None):
    """One struck chime tube: four inharmonic partials, each fading faster than the one below, a tiny click at the strike, a slow shimmer on the lowest."""
    n = c.n(dur)
    t = np.arange(n) / c.sr
    tau = tau or dur * 0.32
    out = np.zeros(n)
    for k, (r, a) in enumerate(zip(TUBE, [1.0, 0.55 * bright, 0.28 * bright, 0.12 * bright])):
        fr = f * r
        if fr > 0.45 * c.sr:
            continue
        out += a * np.exp(-t / (tau / (1.0 + 0.7 * k))) * np.sin(2 * np.pi * fr * t + c.rng.uniform(0, 6.28))
    out *= 1.0 + 0.04 * np.sin(2 * np.pi * 5.5 * t)
    click = M.hp(M.white(c, n), 5000.0, c.sr, 1) * np.exp(-t / 0.0015) * 0.25 * bright
    return M.norm(out + click)


def sparkle(c, dur, density=60.0, lo=4200.0, hi=9500.0, level=1.0):
    """A glitter of tiny high ticks (bell dust)."""
    n = c.n(dur)
    out = np.zeros(n)
    k = max(1, int(density * dur))
    for _ in range(k):
        at = int(c.rng.uniform(0, 0.95) * n)
        f = c.rng.uniform(lo, hi)
        m = int(c.rng.uniform(0.004, 0.014) * c.sr)
        tt = np.arange(m) / c.sr
        out = M.place(out, np.sin(2 * np.pi * f * tt) * np.exp(-tt / 0.0025) * c.rng.uniform(0.3, 1.0), at)
    return M.norm(out) * level


def gust_noise(c, dur, f0, f1, q=1.3, shape=None):
    return M.whoosh(c, dur, f0, f1, q=q, shape=shape)


def run_of_chimes(c, notes, gap, dur, rising=True, bright=1.0):
    """A run of chime notes spaced by `gap`, each ringing for `dur`."""
    total = gap * len(notes) + dur
    buf = np.zeros(c.n(total))
    seq = list(notes) if rising else list(notes)[::-1]
    for i, f in enumerate(seq):
        buf = M.place(buf, bell(c, f, dur, bright) * (0.55 + 0.45 * math.sin(math.pi * (i + 1) / (len(seq) + 1))), int(i * gap * c.sr + c.rng.uniform(0, 0.012) * c.sr), 1.0)
    return buf


# ====================================================================================================================== the cues
def r_link(c, i):
    notes = PENTA[1:8] if i == 1 else PENTA[0:7:1][::1]
    x = run_of_chimes(c, notes, 0.075 + 0.01 * i, 0.9, True, 1.0)
    sp = sparkle(c, 1.2, 70.0, level=0.45)
    x = M.place(x, sp, 0)
    x = M.place(x, M.lp(gust_noise(c, 0.5, 600.0, 3200.0, 1.6, [(0, 0), (0.5, 1), (1, 0)]), 6000.0, c.sr, 1) * 0.35, 0)
    return M.reverb(x, c.sr, 0.9, 0.3)


def r_unlink(c, i):
    notes = PENTA[2:8] if i == 1 else PENTA[1:7]
    x = run_of_chimes(c, notes, 0.11, 1.1, False, 0.8)
    x = M.place(x, M.lp(gust_noise(c, 0.7, 2600.0, 500.0, 1.4, [(0, 0), (0.3, 1), (1, 0)]), 5000.0, c.sr, 1) * 0.35, 0)
    return M.reverb(x, c.sr, 1.0, 0.35)


def r_chime(c, i):
    n_notes = (1, 2, 3)[i - 1]
    x = np.zeros(c.n(1.8))
    t0 = 0.0
    for k in range(n_notes):
        f = PENTA[int(c.rng.integers(0, 6))] * (1.0 if k == 0 else c.rng.choice([1.0, 1.125, 1.5]))
        x = M.place(x, bell(c, f, 1.3, 0.8, tau=0.5), int(t0 * c.sr), 0.85 ** k)
        t0 += c.rng.uniform(0.18, 0.42)
    return M.reverb(x, c.sr, 1.1, 0.35)


def r_flutter(c, i):
    dur = (0.42, 0.55, 0.36)[i - 1]
    n = c.n(dur)
    t = np.arange(n) / c.sr
    base = M.bp(M.white(c, n), 2600.0 + 400.0 * i, 1.2, c.sr)                                       # the thin air of a small wing
    wob = 0.5 + 0.5 * np.sin(2 * np.pi * (66.0 + 8.0 * i) * t + 0.4 * np.sin(2 * np.pi * 9.0 * t))   # the beat of the wing, 66 to 82 times a second
    env = M.curve([(0, 0), (0.12, 1), (0.7, 0.8), (1, 0)], n)
    x = M.norm(base * wob ** 1.5) * env
    x = x + 0.25 * M.norm(M.bp(M.white(c, n), 5200.0, 2.0, c.sr) * wob) * env
    x = M.place(x, bell(c, PENTA[5 + i - 1], 0.3, 0.6, tau=0.1), int(0.02 * c.sr), 0.3)
    return M.reverb(x, c.sr, 0.5, 0.15)


def r_giggle(c, i):
    """A fairy giggle: a handful of quick high bell-voiced 'hee' notes that skip up and tumble down, each with a breath of noise and a pinch of vibrato, and glitter after."""
    count = (4, 5, 6)[i - 1]
    base = (1180.0, 1320.0, 1050.0)[i - 1]
    n = c.n(0.15 * count + 0.5)
    out = np.zeros(n)
    t0 = 0.02
    for k in range(count):
        up = math.sin(math.pi * k / max(1, count - 1))                                              # a hill: the pitch climbs then tumbles
        f = base * (1.0 + 0.34 * up) * (1.0 + 0.03 * c.rng.uniform(-1, 1))
        dur = 0.085 + 0.02 * c.rng.random()
        m = c.n(dur)
        tt = np.arange(m) / c.sr
        glide = f * (1.0 + 0.10 * np.exp(-tt / 0.02))
        ph = 2 * np.pi * np.cumsum(glide * (1.0 + 0.012 * np.sin(2 * np.pi * 34.0 * tt))) / c.sr
        tone = np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.16 * np.sin(3.76 * ph)
        breath = M.bp(M.white(c, m), 3800.0, 1.0, c.sr) * 0.28
        env = np.exp(-tt / (dur * 0.5)) * np.minimum(1.0, tt / 0.004)
        out = M.place(out, (tone + breath) * env, int(t0 * c.sr), 0.8 + 0.2 * up)
        t0 += 0.105 + 0.03 * c.rng.random()
    out = M.place(out, sparkle(c, 0.5, 90.0, 5200.0, 10500.0, 0.5), int((t0 - 0.05) * c.sr))
    return M.reverb(out, c.sr, 0.6, 0.22)


def r_gust(c, i):
    dur = (0.55, 0.7, 0.45)[i - 1]
    f1 = (3600.0, 2800.0, 4200.0)[i - 1]
    x = gust_noise(c, dur, 380.0, f1, 1.2, [(0, 0), (0.25, 1), (0.6, 0.7), (1, 0)])
    x = x + 0.35 * M.lp(gust_noise(c, dur, 120.0, 700.0, 1.0, [(0, 0), (0.3, 1), (1, 0)]), 900.0, c.sr, 1)
    n_notes = 2 + (i % 2)
    run = run_of_chimes(c, PENTA[3:3 + n_notes][::-1], 0.07, 0.55, False, 0.7)
    x = M.place(x, run * 0.55, int(dur * 0.45 * c.sr))
    return M.reverb(x, c.sr, 0.7, 0.2)


def r_surge(c, i):
    dur = 1.25
    up = gust_noise(c, dur, 300.0, 5200.0, 1.1, [(0, 0), (0.55, 0.9), (0.8, 1), (1, 0)])
    low = M.lp(M.white(c, c.n(dur)), 380.0, c.sr, 2) * M.curve([(0, 0), (0.5, 1), (1, 0)], c.n(dur))
    x = up + 0.45 * M.norm(low)
    notes = PENTA[0:8] if i == 1 else PENTA[1:9]
    run = run_of_chimes(c, notes, 0.07, 0.9, True, 1.0)
    x = M.place(x, run * 0.6, int(0.25 * c.sr))
    x = M.place(x, sparkle(c, 1.0, 80.0, level=0.4), int(0.3 * c.sr))
    return M.reverb(x, c.sr, 1.2, 0.35)


def r_gale(c, i):
    dur = 1.5
    n = c.n(dur)
    body = M.lp(gust_noise(c, dur, 160.0, 1400.0, 0.9, [(0, 0), (0.3, 1), (0.7, 0.9), (1, 0)]), 2500.0, c.sr, 1)
    howl = M.bp(M.white(c, n), 1300.0, 6.0, c.sr) * (0.6 + 0.4 * np.sin(2 * np.pi * 5.0 * np.arange(n) / c.sr)) * M.curve([(0, 0), (0.3, 1), (0.8, 0.6), (1, 0)], n)
    x = M.norm(body) + 0.5 * M.norm(howl)
    ring = run_of_chimes(c, [PENTA[2], PENTA[4], PENTA[6], PENTA[8]] if i == 1 else [PENTA[1], PENTA[3], PENTA[5], PENTA[7]], 0.05, 1.1, True, 1.0)
    x = M.place(x, ring * 0.55, int(0.22 * c.sr))
    return M.reverb(x, c.sr, 1.3, 0.3)


def voice_note(c, f0, dur, glide=0.0, vib=5.5, depth=0.012, breath=0.16, attack=0.02, release=0.07, overtones=(0.42, 0.18)):
    """One soft sung note (no word): a sine with two overtones, a slow vibrato, a glide over the note, a breath of noise and a smooth start and end."""
    n = c.n(dur)
    t = np.arange(n) / c.sr
    f = f0 * (1.0 + glide * (t / dur)) * (1.0 + depth * np.sin(2 * np.pi * vib * t + c.rng.uniform(0, 6.28)))
    ph = 2 * np.pi * np.cumsum(f) / c.sr
    tone = np.sin(ph) + overtones[0] * np.sin(2 * ph) + overtones[1] * np.sin(3 * ph)
    env = np.minimum(1.0, t / attack) * np.clip((dur - t) / release, 0.0, 1.0)
    air = M.bp(M.white(c, n), 3600.0, 1.0, c.sr) * breath
    return (tone * 0.55 + air) * env


def r_hum(c, i):
    """A little tune hummed to herself: five or six soft notes of the pentatonic scale, a lilt, a touch of room."""
    tune = ([523.25, 587.33, 659.25, 587.33, 523.25, 659.25], [659.25, 587.33, 523.25, 587.33, 659.25, 783.99])[i - 1]
    out = np.zeros(c.n(0.26 * len(tune) + 0.5))
    t0 = 0.02
    for k, f in enumerate(tune):
        d = 0.21 + 0.04 * (k % 2)
        out = M.place(out, voice_note(c, f, d, 0.02, 5.0, 0.01, 0.12), int(t0 * c.sr), 0.8)
        t0 += d + 0.04
    return M.reverb(out, c.sr, 0.5, 0.18)


def r_gasp(c, i):
    """A quick intake of breath and a small startled 'oh': air that swells for a moment, then a short rising note."""
    n = c.n(0.5)
    t = np.arange(n) / c.sr
    air = M.bp(M.white(c, n), 3000.0 + 500.0 * i, 0.8, c.sr) * M.curve([(0, 0), (0.16, 1), (0.3, 0.1), (1, 0)], n) * 0.9
    note = voice_note(c, 640.0 + 60.0 * i, 0.2, 0.45, 8.0, 0.02, 0.1, 0.01, 0.06)
    x = M.place(air, note, int(0.2 * c.sr), 0.9)
    return M.reverb(x, c.sr, 0.4, 0.12)


def r_whimper(c, i):
    """A small sob: two falling wavering notes with breath between them."""
    out = np.zeros(c.n(1.25))
    out = M.place(out, voice_note(c, 880.0 - 40.0 * i, 0.42, -0.30, 7.5, 0.03, 0.2, 0.04, 0.12), int(0.03 * c.sr), 0.9)
    out = M.place(out, M.bp(M.white(c, c.n(0.18)), 2600.0, 0.9, c.sr) * M.curve([(0, 0), (0.4, 1), (1, 0)], c.n(0.18)) * 0.5, int(0.48 * c.sr))
    out = M.place(out, voice_note(c, 740.0 - 40.0 * i, 0.5, -0.34, 7.0, 0.035, 0.22, 0.04, 0.2), int(0.62 * c.sr), 0.8)
    return M.reverb(out, c.sr, 0.7, 0.2)


def r_cheer(c, i):
    """A bright 'yay': a short rising voiced note, a run of chimes over it and a glitter."""
    out = np.zeros(c.n(1.1))
    out = M.place(out, voice_note(c, 720.0 + 40.0 * i, 0.3, 0.55, 6.0, 0.015, 0.1, 0.012, 0.1, (0.6, 0.3)), int(0.02 * c.sr), 1.0)
    out = M.place(out, run_of_chimes(c, PENTA[3:8] if i == 1 else PENTA[4:9], 0.06, 0.6, True, 0.9), int(0.14 * c.sr), 0.55)
    out = M.place(out, sparkle(c, 0.7, 90.0, 5200.0, 10500.0, 0.45), int(0.2 * c.sr))
    return M.reverb(out, c.sr, 0.7, 0.22)


def r_pout(c, i):
    """A huffy 'hmph': a puff of air through the nose and a short low note that sags."""
    n = c.n(0.14)
    puff = M.lp(M.white(c, n), 1800.0, c.sr, 1) * M.curve([(0, 0), (0.2, 1), (1, 0)], n)
    out = np.zeros(c.n(0.55))
    out = M.place(out, puff, int(0.02 * c.sr), 0.8)
    out = M.place(out, voice_note(c, 520.0 - 30.0 * i, 0.26, -0.30, 6.0, 0.02, 0.05, 0.015, 0.1, (0.3, 0.1)), int(0.12 * c.sr), 0.9)
    return M.reverb(out, c.sr, 0.35, 0.1)


RECIPES = {'link': r_link, 'unlink': r_unlink, 'chime': r_chime, 'flutter': r_flutter, 'giggle': r_giggle, 'gust': r_gust, 'surge': r_surge, 'gale': r_gale, 'hum': r_hum, 'gasp': r_gasp, 'whimper': r_whimper, 'cheer': r_cheer, 'pout': r_pout}


# ====================================================================================================================== writing
def render(cue, index):
    c = M.Ctx(SR, np.random.default_rng(seed_for(cue, index)))
    return M.finalize(RECIPES[cue](c, index), SR, cue)


def generate(preview=None, quality=3, only=None):
    os.makedirs(SOUND_DIR, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for cue, (variants, _l, _s) in CUES.items():
            if only and cue not in only:
                continue
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
                print('%-8s %d  %.2f s  %5d bytes' % (cue, i, len(x) / SR, os.path.getsize(ogg)))


def manifest():
    entries, lang = {}, {}
    for cue, (variants, _l, text) in CUES.items():
        sub = 'subtitles.nusmp.sylph.' + cue
        lang[sub] = text
        sounds = []
        for i in range(1, variants + 1):
            path = os.path.join(SOUND_DIR, '%s_%d.ogg' % (cue, i))
            if not os.path.exists(path):
                raise SystemExit('missing %s (run the generator first)' % path)
            s = {'name': 'nusmp:sylph/%s_%d' % (cue, i)}
            if M.ogg_seconds(path) >= M.STREAM_OVER:
                s['stream'] = True
            sounds.append(s)
        entries['sylph.' + cue] = {'subtitle': sub, 'sounds': sounds}
    return entries, lang


def write_manifest():
    """Appends the entries of the cues that have none yet (every entry that is in the file, and every other builder's entries and the spoken voice lines after it, stay byte for byte)."""
    entries, lang = manifest()
    with open(SOUNDS_JSON, encoding='utf-8') as f:
        text = f.read()
    added = 0
    tail = ''
    for k, v in entries.items():
        if '"%s"' % k in text:
            continue
        tail += ',\n' + M._entry_text(k, v)
        added += 1
    if tail:
        body = text.rstrip()
        assert body.endswith('}')
        text = body[:-1].rstrip() + tail + '\n}\n'
        json.loads(text)                                                                 # never write a file that does not parse
        with open(SOUNDS_JSON, 'w', encoding='utf-8') as f:
            f.write(text)
    try:
        with open(LANG_OUT, encoding='utf-8') as f:
            old = json.load(f)
    except FileNotFoundError:
        old = {}
    old = {k: v for k, v in old.items() if not k.startswith('subtitles.nusmp.sylph.')}
    old.update(lang)
    with open(LANG_OUT, 'w', encoding='utf-8') as f:
        json.dump(old, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('sounds.json: %d sylph events (%d new); %d subtitles' % (len(entries), added, len(lang)))


def check():
    with open(SOUNDS_JSON, encoding='utf-8') as f:
        doc = json.load(f)
    bad = []
    for cue, (variants, _l, _s) in CUES.items():
        e = doc.get('sylph.' + cue)
        if not e:
            bad.append('no entry sylph.' + cue)
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
    only = args[args.index('--only') + 1].split(',') if '--only' in args else None
    generate(preview, only=only)
    write_manifest()


if __name__ == '__main__':
    main()
