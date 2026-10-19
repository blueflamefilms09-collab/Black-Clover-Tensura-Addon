"""Soul Magic (1.93.0 rebuild) particle textures: a soul wisp with a tail, a soul flame, a spiral vortex, a rune ring, a flame ribbon and a skull halo.

    python3 -B tools/gen_soul_rebuild_textures.py       # writes textures/particle/soul_*.png (white + alpha, tinted by the layers)

soul_wisp     128     a teardrop soul with a curling tail and two hollow eyes cut out, in a halo
soul_flame    64      a soul flame: a tall teardrop with a bright heart
soul_vortex   256     a spiral of five arms with small souls riding them (the field's sigil)
soul_rune     256     a ring of soul runes between two thin circles, with four small flames at the compass points
soul_ribbon_v 32x256  a flame ribbon running along it (VfxVertexBuffer.beam: v runs along), bright at one end, curling
soul_skull    128     a skull in a ring of fire (the release)
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_texkit import Canvas, save_particle   # noqa: E402


def teardrop(c, cx, cy, w, h, a=255, curl=0.0):
    pts = []
    for k in range(25):
        t = k / 24.0
        y = cy - h * t
        wd = w * math.sin(math.pi * min(1.0, t * 0.85 + 0.0)) ** 0.8 * (1 - t) ** 0.35
        pts.append((cx - wd + curl * t * t, y))
    for k in range(24, -1, -1):
        t = k / 24.0
        y = cy - h * t
        wd = w * math.sin(math.pi * min(1.0, t * 0.85)) ** 0.8 * (1 - t) ** 0.35
        pts.append((cx + wd + curl * t * t, y))
    c.poly(pts, a)


def wisp():
    c = Canvas(128, seed=21)
    c.glow(0.5, 0.45, 0.4, 120)
    teardrop(c, 0.5, 0.85, 0.26, 0.7, 255, curl=0.12)
    c.glow(0.5, 0.68, 0.2, 180)
    img = c.finish(blur=0.6, halo=0.5, halo_r=2.4)
    arr = np.array(img)
    ys, xs = np.mgrid[0:128, 0:128]
    for ex in (0.42, 0.58):                                               # hollow eyes cut out of the body
        d = np.hypot((xs / 128.0 - ex) / 0.045, (ys / 128.0 - 0.52) / 0.07)
        arr[..., 3] = np.where(d < 1.0, arr[..., 3] * 0.15, arr[..., 3]).astype(np.uint8)
    save_particle("soul_wisp", Image.fromarray(arr, "RGBA"))


def flame():
    c = Canvas(64, seed=22)
    c.glow(0.5, 0.65, 0.35, 120)
    teardrop(c, 0.5, 0.92, 0.22, 0.8, 255, curl=0.05)
    c.glow(0.5, 0.74, 0.16, 210)
    save_particle("soul_flame", c.finish(blur=0.5, halo=0.5, halo_r=2.0))


def vortex():
    c = Canvas(256, seed=23)
    c.glow(0.5, 0.5, 0.2, 150)
    for arm in range(5):
        pts = []
        for k in range(40):
            t = k / 39.0
            ang = arm * 2 * math.pi / 5 + t * 3.4
            r = 0.06 + 0.4 * t
            pts.append((0.5 + r * math.cos(ang), 0.5 + r * math.sin(ang)))
        c.line(pts, 0.012, 255)
        ang = arm * 2 * math.pi / 5 + 0.62 * 3.4
        r = 0.06 + 0.4 * 0.62
        c.disc(0.5 + r * math.cos(ang), 0.5 + r * math.sin(ang), 0.022, 255)
    c.ring(0.5, 0.5, 0.47, 0.005, 220)
    save_particle("soul_vortex", c.finish(blur=0.5, halo=0.45, halo_r=2.0))


def rune():
    c = Canvas(256, seed=24)
    c.ring(0.5, 0.5, 0.485, 0.005, 255)
    c.ring(0.5, 0.5, 0.36, 0.004, 255)
    c.ring(0.5, 0.5, 0.42, 0.002, 140)
    for k in range(20):                                                    # blocky rune glyphs between the rings
        t = k * 2 * math.pi / 20
        r = 0.425
        cx, cy = 0.5 + r * math.cos(t), 0.5 + r * math.sin(t)
        for j in range(3):
            if (k * 7 + j * 3) % 5 < 3:
                dx, dy = math.cos(t + math.pi / 2) * 0.018 * (j - 1), math.sin(t + math.pi / 2) * 0.018 * (j - 1)
                c.line([(cx + dx - math.cos(t) * 0.02, cy + dy - math.sin(t) * 0.02), (cx + dx + math.cos(t) * 0.02, cy + dy + math.sin(t) * 0.02)], 0.004, 255)
    for k in range(4):
        t = k * math.pi / 2
        c.disc(0.5 + 0.485 * math.cos(t), 0.5 + 0.485 * math.sin(t), 0.02, 255)
    c.glow(0.5, 0.5, 0.3, 60)
    save_particle("soul_rune", c.finish(blur=0.5, halo=0.4, halo_r=2.0))


def ribbon_v():
    c = Canvas(32, 256, seed=25)
    pts = []
    for k in range(30):
        y = k / 29.0
        pts.append((0.5 + 0.18 * math.sin(y * 7) * y, y))
    c.line(pts, 0.1, 255)
    c.line([(0.5, 0.0), (0.5 + 0.02, 1.0)], 0.04, 255)
    img = c.finish(blur=0.7, halo=0.6, halo_r=2.4)
    arr = np.array(img)
    ys = np.arange(arr.shape[0], dtype=np.float32) / (arr.shape[0] - 1)
    arr[..., 3] = (arr[..., 3] * (0.25 + 0.75 * ys)[:, None]).astype(np.uint8)       # bright at the bottom (the head)
    save_particle("soul_ribbon_v", Image.fromarray(arr, "RGBA"))


def skull():
    c = Canvas(128, seed=26)
    for k in range(24):
        t = k * 2 * math.pi / 24
        r0 = 0.36
        r1 = 0.46 if k % 2 == 0 else 0.41
        c.line([(0.5 + r0 * math.cos(t), 0.5 + r0 * math.sin(t)), (0.5 + r1 * math.cos(t), 0.5 + r1 * math.sin(t))], 0.012, 255)
    c.ring(0.5, 0.5, 0.34, 0.01, 255)
    c.ellipse(0.5, 0.46, 0.2, 0.2, 255)
    c.poly([(0.4, 0.58), (0.6, 0.58), (0.58, 0.72), (0.42, 0.72)], 255)
    img = c.finish(blur=0.5, halo=0.4, halo_r=2.0)
    arr = np.array(img)
    ys, xs = np.mgrid[0:128, 0:128]
    for ex in (0.43, 0.57):
        d = np.hypot((xs / 128.0 - ex) / 0.05, (ys / 128.0 - 0.46) / 0.06)
        arr[..., 3] = np.where(d < 1.0, 25, arr[..., 3]).astype(np.uint8)
    for tx in (0.44, 0.5, 0.56):
        arr[(ys / 128.0 > 0.62) & (ys / 128.0 < 0.72) & (np.abs(xs / 128.0 - tx) < 0.006), 3] = 30
    save_particle("soul_skull", Image.fromarray(arr, "RGBA"))


if __name__ == "__main__":
    wisp()
    flame()
    vortex()
    rune()
    ribbon_v()
    skull()
