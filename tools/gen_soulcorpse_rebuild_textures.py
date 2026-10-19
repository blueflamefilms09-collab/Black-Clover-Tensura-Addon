"""Soul Corpse Magic (1.93.0 rebuild) particle textures: a skeletal hand, a stitched thread, a coffin sigil, a ring of cross stitches and a bone shard.

    python3 -B tools/gen_soulcorpse_rebuild_textures.py       # writes textures/particle/scorpse_*.png (white + alpha, tinted by the layers)

scorpse_hand      128     a skeletal hand reaching up, five bony fingers with joints, in a halo
scorpse_thread    16x256  a taut thread with cross stitches along it (VfxVertexBuffer.beam: v runs along); also used by SealDraw.ribbon (u along)
scorpse_sigil     256     the ground sigil: a coffin outline inside a ring of cross stitches, a cross and four nails
scorpse_stitch    128     a ring of cross stitches with a thin outer circle
scorpse_shard     64      a splinter of bone, long and jagged
scorpse_grave     128     a headstone with a cross cut in it, in a halo (the field's marker)
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_texkit import Canvas, save_particle   # noqa: E402


def cross(c, x, y, r, w=0.006, a=255, rot=0.0):
    for s in (1, -1):
        dx, dy = r * math.cos(rot + s * math.pi / 4), r * math.sin(rot + s * math.pi / 4)
        c.line([(x - dx, y - dy), (x + dx, y + dy)], w, a, round_ends=False)


def hand():
    c = Canvas(128, seed=31)
    c.glow(0.5, 0.55, 0.4, 70)
    c.poly([(0.38, 0.95), (0.62, 0.95), (0.64, 0.62), (0.36, 0.62)], 255)           # the wrist
    c.ellipse(0.5, 0.58, 0.17, 0.12, 255)                                               # the palm
    for k, (x, ln, tilt) in enumerate(((0.34, 0.34, -0.16), (0.43, 0.42, -0.05), (0.52, 0.44, 0.04), (0.61, 0.38, 0.13))):
        pts = []
        for j in range(4):                                                              # three finger bones with joints
            t = j / 3.0
            pts.append((x + tilt * ln * t + 0.03 * math.sin(t * 2.0), 0.55 - ln * t))
        c.line(pts, 0.016, 255)
        for p in pts[1:]:
            c.disc(p[0], p[1], 0.022, 255)
    c.line([(0.67, 0.6), (0.78, 0.46), (0.8, 0.34)], 0.017, 255)                           # thumb
    c.disc(0.8, 0.34, 0.021, 255)
    save_particle("scorpse_hand", c.finish(blur=0.5, halo=0.5, halo_r=2.2))


def thread_tex():
    c = Canvas(16, 256, seed=32)
    c.line([(0.5, 0.0), (0.5, 1.0)], 0.06, 255, round_ends=False)
    for k in range(16):
        y = (k + 0.5) / 16.0
        c.line([(0.1, y - 0.012), (0.9, y + 0.012)], 0.04, 200, round_ends=False)
        c.line([(0.1, y + 0.012), (0.9, y - 0.012)], 0.04, 200, round_ends=False)
    save_particle("scorpse_thread", c.finish(blur=0.5, halo=0.5, halo_r=1.6))


def sigil():
    c = Canvas(256, seed=33)
    c.ring(0.5, 0.5, 0.485, 0.005, 255)
    c.ring(0.5, 0.5, 0.37, 0.004, 255)
    for k in range(32):
        t = k * 2 * math.pi / 32
        cross(c, 0.5 + 0.43 * math.cos(t), 0.5 + 0.43 * math.sin(t), 0.018, 0.0035, 235, rot=t)
    pts = [(0.5, 0.2), (0.6, 0.34), (0.57, 0.8), (0.43, 0.8), (0.4, 0.34), (0.5, 0.2)]     # the coffin
    c.line(pts, 0.007, 255)
    c.line([(0.5, 0.3), (0.5, 0.64)], 0.007, 255)
    c.line([(0.44, 0.4), (0.56, 0.4)], 0.007, 255)
    for k in range(4):
        t = k * math.pi / 2 + math.pi / 4
        c.line([(0.5 + 0.34 * math.cos(t), 0.5 + 0.34 * math.sin(t)), (0.5 + 0.26 * math.cos(t), 0.5 + 0.26 * math.sin(t))], 0.006, 255)
        c.disc(0.5 + 0.34 * math.cos(t), 0.5 + 0.34 * math.sin(t), 0.012, 255)
    c.glow(0.5, 0.5, 0.3, 50)
    save_particle("scorpse_sigil", c.finish(blur=0.5, halo=0.4, halo_r=2.0))


def stitch():
    c = Canvas(128, seed=34)
    c.ring(0.5, 0.5, 0.47, 0.01, 255)
    c.ring(0.5, 0.5, 0.33, 0.006, 200)
    for k in range(20):
        t = k * 2 * math.pi / 20
        cross(c, 0.5 + 0.4 * math.cos(t), 0.5 + 0.4 * math.sin(t), 0.035, 0.008, 255, rot=t)
    save_particle("scorpse_stitch", c.finish(blur=0.5, halo=0.4, halo_r=1.8))


def shard():
    c = Canvas(64, seed=35)
    c.poly([(0.5, 0.02), (0.64, 0.4), (0.58, 0.98), (0.44, 0.98), (0.36, 0.45)], 255)
    c.line([(0.5, 0.1), (0.5, 0.9)], 0.01, 140, round_ends=False)
    c.glow(0.5, 0.5, 0.3, 50)
    save_particle("scorpse_shard", c.finish(blur=0.4, halo=0.4, halo_r=1.6))


def grave():
    c = Canvas(128, seed=36)
    c.glow(0.5, 0.6, 0.45, 70)
    pts = [(0.3, 0.95), (0.3, 0.3)]
    for k in range(13):
        t = math.pi - k * math.pi / 12
        pts.append((0.5 + 0.2 * math.cos(t), 0.3 - 0.2 * math.sin(t) * 1.0))
    pts += [(0.7, 0.95), (0.3, 0.95)]
    c.poly(pts, 255)
    img = c.finish(blur=0.5, halo=0.5, halo_r=2.2)
    arr = np.array(img)
    ys, xs = np.mgrid[0:128, 0:128]
    arr[((np.abs(xs / 128.0 - 0.5) < 0.018) & (ys / 128.0 > 0.28) & (ys / 128.0 < 0.62)) | ((np.abs(ys / 128.0 - 0.38) < 0.018) & (np.abs(xs / 128.0 - 0.5) < 0.09)), 3] = 40
    save_particle("scorpse_grave", Image.fromarray(arr, "RGBA"))


if __name__ == "__main__":
    hand()
    thread_tex()
    sigil()
    stitch()
    shard()
    grave()
