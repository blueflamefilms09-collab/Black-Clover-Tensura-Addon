"""Song Magic (1.93.0 rebuild) particle textures: staff lines, sound-wave rings, note glyphs, the treble clef, a note burst and the ground staff ring.

    python3 -B tools/gen_song_rebuild_textures.py       # writes textures/particle/song_*.png (white + alpha, tinted by the layers)

song_staff    256x32  five staff lines with note heads, fading at both ends (horizontal: SealDraw.ribbon u runs along it)
song_staff_v  32x256  the same turned a quarter (VfxVertexBuffer.beam: v runs along it)
song_wave     128     three broken sound arcs round a soft core
song_note     64      an eighth-note glyph in a halo
song_clef     128     a treble clef in a halo
song_burst    128     a ring of long and short rays with four-point stars
song_staffring 256    the ground sigil: five concentric staff rings, tick marks and notes, with a bright outer line
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_texkit import Canvas, save_particle   # noqa: E402


def catmull(pts, n=10):
    out = []
    P = [pts[0]] + list(pts) + [pts[-1]]
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for k in range(n):
            t = k / float(n)
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2 + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in range(2)))
    out.append(pts[-1])
    return out


def note_glyph(c, cx, cy, s, a=255):
    """An eighth note: tilted head, stem on its right, a flag."""
    c.ellipse(cx, cy, 0.12 * s, 0.085 * s, a, rot=-0.45)
    c.line([(cx + 0.105 * s, cy - 0.02 * s), (cx + 0.105 * s, cy - 0.62 * s)], 0.018 * s, a)
    c.line(catmull([(cx + 0.105 * s, cy - 0.62 * s), (cx + 0.2 * s, cy - 0.52 * s), (cx + 0.26 * s, cy - 0.38 * s), (cx + 0.2 * s, cy - 0.26 * s)], 8), 0.022 * s, a)


def staff():
    c = Canvas(256, 32, seed=1)
    c.w, c.h = 256, 32
    for k in range(5):
        y = 0.18 + k * 0.16
        c.line([(0.02, y), (0.98, y)], 0.0028, 255)
    for k, (x, y) in enumerate(((0.2, 0.34), (0.34, 0.5), (0.5, 0.18), (0.66, 0.66), (0.8, 0.34))):
        c.ellipse(x, y, 0.016, 0.1, 255, rot=0.0)
    img = c.finish(blur=0.5, halo=0.55, halo_r=1.6)
    arr = np.array(img)
    xs = np.arange(arr.shape[1], dtype=np.float32)
    f = np.minimum(1.0, np.minimum(xs / 40.0, (arr.shape[1] - 1 - xs) / 40.0))
    arr[..., 3] = (arr[..., 3] * f[None, :]).astype(np.uint8)
    img = Image.fromarray(arr, "RGBA")
    save_particle("song_staff", img)
    save_particle("song_staff_v", img.rotate(90, expand=True))


def wave():
    c = Canvas(128, seed=2)
    c.glow(0.5, 0.5, 0.2, 220)
    for k, r in enumerate((0.16, 0.29, 0.42)):
        sp = 1.0 - k * 0.12
        for side in range(4):
            a0 = side * math.pi / 2 + 0.25 + k * 0.2
            c.arc(0.5, 0.5, r, 0.014 + 0.004 * (2 - k), a0, a0 + sp * 1.1, int(255 - k * 30))
    save_particle("song_wave", c.finish(blur=0.5, halo=0.4, halo_r=2.0))


def note():
    c = Canvas(64, seed=3)
    c.glow(0.46, 0.7, 0.3, 140)
    note_glyph(c, 0.40, 0.74, 1.0, 255)
    save_particle("song_note", c.finish(blur=0.5, halo=0.5, halo_r=2.2))


def clef():
    c = Canvas(128, seed=4)
    c.glow(0.5, 0.5, 0.45, 70)
    c.line([(0.53, 0.07), (0.53, 0.86)], 0.011, 255)
    body = catmull([(0.53, 0.64), (0.67, 0.56), (0.66, 0.43), (0.5, 0.38), (0.37, 0.47), (0.4, 0.6), (0.55, 0.66), (0.66, 0.6)], 10)
    c.line(body, 0.014, 255)
    top = catmull([(0.53, 0.07), (0.62, 0.15), (0.6, 0.28), (0.48, 0.4), (0.4, 0.5)], 10)
    c.line(top, 0.014, 255)
    c.line(catmull([(0.53, 0.86), (0.5, 0.93), (0.42, 0.93), (0.4, 0.87), (0.45, 0.83)], 8), 0.014, 255)
    c.disc(0.45, 0.82, 0.03, 255)
    save_particle("song_clef", c.finish(blur=0.5, halo=0.6, halo_r=2.4))


def burst():
    c = Canvas(128, seed=5)
    c.glow(0.5, 0.5, 0.16, 200)
    for k in range(16):
        t = k * math.pi / 8
        r0, r1 = (0.12, 0.46) if k % 2 == 0 else (0.14, 0.32)
        c.line([(0.5 + r0 * math.cos(t), 0.5 + r0 * math.sin(t)), (0.5 + r1 * math.cos(t), 0.5 + r1 * math.sin(t))], 0.007 if k % 2 else 0.011, 255)
    for k in range(4):
        t = k * math.pi / 2 + math.pi / 4
        c.star(0.5 + 0.33 * math.cos(t), 0.5 + 0.33 * math.sin(t), 0.07, 4, 0.18, 255, rot=t)
    c.ring(0.5, 0.5, 0.1, 0.012, 255)
    save_particle("song_burst", c.finish(blur=0.5, halo=0.4, halo_r=2.0))


def staffring():
    c = Canvas(256, seed=6)
    c.ring(0.5, 0.5, 0.485, 0.006, 255)
    c.ring(0.5, 0.5, 0.46, 0.003, 180)
    for k in range(5):
        c.ring(0.5, 0.5, 0.30 + k * 0.032, 0.0025, 235)
    for k in range(24):
        t = k * math.pi / 12
        c.line([(0.5 + 0.28 * math.cos(t), 0.5 + 0.28 * math.sin(t)), (0.5 + 0.255 * math.cos(t), 0.5 + 0.255 * math.sin(t))], 0.003, 200)
    for k in range(6):
        t = k * math.pi / 3 + 0.3
        note_glyph(c, 0.5 + 0.38 * math.cos(t) - 0.02, 0.5 + 0.38 * math.sin(t) + 0.03, 0.22, 255)
    c.glow(0.5, 0.5, 0.25, 70)
    save_particle("song_staffring", c.finish(blur=0.5, halo=0.4, halo_r=2.0))


if __name__ == "__main__":
    staff()
    wave()
    note()
    clef()
    burst()
    staffring()
