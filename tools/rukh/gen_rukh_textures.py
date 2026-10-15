#!/usr/bin/env python3
"""
Dark / Light Rukh particle sprites (docs/rukh/r1_core.md): textures/particle/{dark,light}_rukh_<0..5>.png, 32x32 RGBA.

Reference (owner's images): the dark Rukh is a flock of small hard-edged black ink-brush birds in many wing poses (wings up in a V,
spread, diving, gliding); the light Rukh is the same flock as cream-white crescent-winged birds on a soft yellowish glow.
So each texture is ONE bird; the six frames are six wing poses and the particle plays them as a flap cycle, so a stream of particles
(each with its own start phase) reads as the flock of the pictures.

    python3 tools/rukh/gen_rukh_textures.py            # write the 12 PNGs
    python3 tools/rukh/gen_rukh_textures.py --sheet out.png   # also write a contact sheet (6 frames x 2 kinds, 8x, on grey/night)
    python3 tools/rukh/gen_rukh_textures.py --check    # verify the files on disk equal a fresh render

Deterministic (seeded per frame): the same script writes the same bytes. Pure Pillow, no numpy.
"""
import argparse
import math
import os
import random
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SIZE, SS = 32, 8          # texture size, supersampling
N = SIZE * SS

# pose: (wing angle above horizontal in degrees, wing span in px, elbow bend in degrees (tip droops/sweeps), body tilt, tail)
POSES = [
    (62, 12.5, -18, 0, 3.0),    # 0 wings up in a V
    (34, 13.5, -10, 0, 3.0),    # 1 rising
    (6, 14.5, 8, 0, 3.0),       # 2 wings spread flat
    (-34, 13.0, 22, 0, 3.0),    # 3 downstroke
    (14, 15.0, 30, 0, 2.5),     # 4 glide, swept back tips
    (50, 9.0, -40, 14, 4.5),    # 5 dive: wings folded in a narrow V, body tilted
]


def wing(draw, cx, cy, side, pose, rnd):
    ang, span, bend, tilt, _ = pose
    # centre line of the wing: shoulder -> elbow -> tip, tapering crescent (thick at the shoulder, a point at the tip)
    a1 = math.radians(ang)
    a2 = math.radians(ang + bend)
    elbow = span * 0.45
    pts = [(0.0, 0.0), (math.cos(a1) * elbow, -math.sin(a1) * elbow)]
    ex, ey = pts[1]
    pts.append((ex + math.cos(a2) * (span - elbow), ey - math.sin(a2) * (span - elbow)))
    # smooth: sample a quadratic through the 3 points
    path = []
    for i in range(0, 21):
        t = i / 20
        x = (1 - t) ** 2 * pts[0][0] + 2 * (1 - t) * t * pts[1][0] + t * t * pts[2][0]
        y = (1 - t) ** 2 * pts[0][1] + 2 * (1 - t) * t * pts[1][1] + t * t * pts[2][1]
        path.append((x, y, t))
    top, bot = [], []
    for x, y, t in path:
        w = (1.55 * (1 - t) ** 0.8 + 0.12) * (1.0 + 0.5 * math.sin(t * math.pi))   # crescent: widest a third out
        w *= 1 + rnd.uniform(-0.12, 0.12)
        top.append((x, y - w))
        bot.append((x, y + w * 0.7))
    poly = top + bot[::-1]
    poly = [(cx + side * x, cy + y) for x, y in poly]
    draw.polygon([(px * SS, py * SS) for px, py in poly], fill=255)


def bird_mask(frame):
    rnd = random.Random(7000 + frame)
    pose = POSES[frame]
    m = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(m)
    cx, cy = SIZE / 2, SIZE / 2 + (1.5 if frame in (0, 5) else 0)
    for side in (-1, 1):
        wing(d, cx, cy, side, pose, rnd)
    # body: small teardrop + tail
    tail = pose[4]
    d.ellipse([(cx - 1.1) * SS, (cy - 1.0) * SS, (cx + 1.1) * SS, (cy + 1.6) * SS], fill=255)
    d.polygon([((cx - 0.9) * SS, (cy + 1.2) * SS), ((cx + 0.9) * SS, (cy + 1.2) * SS), (cx * SS, (cy + 1.2 + tail) * SS)], fill=255)
    d.ellipse([(cx - 0.7) * SS, (cy - 1.9) * SS, (cx + 0.7) * SS, (cy - 0.3) * SS], fill=255)   # head
    m = m.rotate(pose[3] + rnd.uniform(-4, 4), resample=Image.BICUBIC, center=(N / 2, N / 2))
    return m, rnd


def roughen(m, rnd, amount):
    """ink-brush edge: blur + per-pixel noise + threshold, so the silhouette edge is slightly ragged but still hard."""
    b = m.filter(ImageFilter.GaussianBlur(SS * 0.35))
    noise = Image.new("L", (N, N))
    noise.putdata([rnd.randint(0, 255) for _ in range(N * N)])
    noise = noise.filter(ImageFilter.GaussianBlur(SS * 0.25))
    out = ImageChops.add(b, ImageChops.subtract(noise, Image.new("L", (N, N), 128)).point(lambda v: int(v * amount)))
    return out.point(lambda v: 255 if v > 118 else 0)


def down(img):
    return img.resize((SIZE, SIZE), Image.LANCZOS)


def dark(frame):
    m, rnd = bird_mask(frame)
    m = roughen(m, rnd, 0.9)
    a = down(m)
    rgb = Image.new("RGBA", (SIZE, SIZE), (10, 6, 16, 0))   # black with the faintest violet cast; no outline glow (as in the ink drawing)
    # slight tone variation so the swarm is not one flat colour: dry-brush streak toward the wing tips
    px = rgb.load()
    ap = a.load()
    for y in range(SIZE):
        for x in range(SIZE):
            al = ap[x, y]
            if al:
                k = abs(x - SIZE / 2) / (SIZE / 2)
                px[x, y] = (int(10 + 14 * k), int(6 + 5 * k), int(16 + 26 * k), min(255, int(al * 1.0)))
    return rgb


def light(frame):
    m, rnd = bird_mask(frame)
    core = down(m.filter(ImageFilter.GaussianBlur(SS * 0.18)))
    halo = down(m.filter(ImageFilter.GaussianBlur(SS * 2.2))).point(lambda v: int(min(255, v * 1.5) * 0.55))
    halo2 = down(m.filter(ImageFilter.GaussianBlur(SS * 1.0))).point(lambda v: int(v * 0.6))
    img = Image.new("RGBA", (SIZE, SIZE), (255, 226, 130, 0))
    px = img.load()
    cp, hp, h2 = core.load(), halo.load(), halo2.load()
    for y in range(SIZE):
        for x in range(SIZE):
            c = cp[x, y]
            h = max(hp[x, y], h2[x, y])
            a = max(c, h)
            if a == 0:
                continue
            t = c / 255.0       # core: cream white, halo: soft yellow, fading to translucent
            col = (int(255), int(226 + 26 * t), int(130 + 95 * t))
            px[x, y] = (col[0], col[1], col[2], a)
    return img


def render():
    out = {}
    for f in range(6):
        out["dark_rukh_%d.png" % f] = dark(f)
        out["light_rukh_%d.png" % f] = light(f)
    return out


def sheet(path, imgs):
    S = 8
    W = 6 * SIZE * S
    sh = Image.new("RGBA", (W, 2 * SIZE * S), (0, 0, 0, 255))
    d = ImageDraw.Draw(sh)
    d.rectangle([0, 0, W, SIZE * S], fill=(205, 205, 205, 255))       # dark birds on paper grey (like the drawing)
    d.rectangle([0, SIZE * S, W, 2 * SIZE * S], fill=(20, 28, 64, 255))  # light birds on night blue (like the render)
    for f in range(6):
        for row, kind in enumerate(("dark", "light")):
            im = imgs["%s_rukh_%d.png" % (kind, f)].resize((SIZE * S, SIZE * S), Image.NEAREST)
            sh.alpha_composite(im, (f * SIZE * S, row * SIZE * S))
    sh.convert("RGB").save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--sheet")
    a = ap.parse_args()
    imgs = render()
    if a.sheet:
        sheet(a.sheet, imgs)
    if a.check:
        bad = 0
        for n, im in imgs.items():
            p = os.path.join(OUT, n)
            if not os.path.exists(p) or Image.open(p).convert("RGBA").tobytes() != im.tobytes():
                print("DIFFERS: " + n)
                bad += 1
        print("rukh textures: %d problem(s)" % bad)
        sys.exit(1 if bad else 0)
    os.makedirs(OUT, exist_ok=True)
    for n, im in imgs.items():
        im.save(os.path.join(OUT, n), optimize=True)
    print("wrote %d textures to %s" % (len(imgs), OUT))


if __name__ == "__main__":
    main()
