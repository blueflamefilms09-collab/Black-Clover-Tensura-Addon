"""Great Spirits (1.99.0): Undine, the Water Spirit, as the canon shows her: a GIANT, elegant ADULT MERMAID.

    python3 -B tools/gen_undine_mermaid.py            # writes spirit_undine_mermaid + spirit_undine_mermaid_water (geo, animation, texture, glow map)
    python3 -B tools/geo_check.py spirit_undine_mermaid spirit_undine_mermaid_water

The look (the owner's reference): extremely long wavy flowing pale-blue hair that fills the frame, fin-like ears (webbed, ribbed, blue), pale blue eyes, pale grey-blue skin, a calm,
serious look, bare shoulders. Canon: the great spirit of water, bound to the queens of the Heart Kingdom by a pact that passes from generation to generation; her hosts are
Lolopechka, and for a time Noelle. Here she floats upright with a tail that curls away behind her, a bodice of two pearl shells and a band of water, and about 3.5 blocks from the
crown of her hair to the fin of her tail. Water-mana glows in the scales, the fins, the shells and the hair's sheen.

Clips: idle (a slow float, the hair and the tail waving with a lag along their length), move (a swimming stroke, the hair streaming back), cast (both arms rise, the hair lifts, the tail
snaps), hit. The old models (spirit_undine, spirit_undine_water) stay on disk, written by tools/gen_spirit_lords.py.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import (FnMat, Piece, Anim, Mat, grid, blank, paste, add_glow, cover, disc, ellipse, capsule, shaded, film, hexcells, hash2,  # noqa: E402
                               col, fbm, smooth, sine_rot, loop_rot, solid, TAU)

KEY = "spirit"

SKIN, SKIN_D, SKIN_L = col("C8DCEA"), col("93B0CA"), col("EAF4FB")
HAIR, HAIR_D, HAIR_L = col("9CCBF6"), col("4F86D8"), col("E0F2FF")
SHELL, SHELL_D, SHELL_L = col("EAF2F6"), col("A8BFD0"), col("FFFFFF")
TAIL_TOP, TAIL_MID, TAIL_END = col("A4C4E0"), col("5BA0E4"), col("78D6F0")
FIN, FIN_D, FIN_L = col("84C4F0"), col("3F86D2"), col("D8F2FF")
CYAN = col("8CE8FF")


def skin():
    return FnMat(lambda f, w, h, t: shaded(f, SKIN, SKIN_D, SKIN_L, w, h, noise=0.03, tag=t, glow=col("CFEFFF"), glow_amount=0.10), "skin")


# ---------------------------------------------------------------------------------------------------------------------- the face
def face_mat():
    """Pale blue eyes, calm level brows, a closed mouth that is neither smile nor frown: serious and composed. Soft shapes at the cube's density; no heavy outlines."""
    def fn(face, w, h, tag):
        img, g = shaded(face, SKIN, SKIN_D, SKIN_L, w, h, noise=0.02, tag=tag, glow=col("CFEFFF"), glow_amount=0.06)
        if face != "north":
            return img, g
        u, v, x, y = grid(w, h)
        s = w / 8.0
        paste(img, col("DCEAF4"), cover(ellipse(x, y, w * 0.5, h * 0.5, 3.4 * s, 3.9 * s), soft=3.5 * s) * 0.35)      # a soft light on the middle of the face
        for side in (-1, 1):
            ex, ey = w * (0.5 + side * 0.215), h * 0.56
            rx, ry = 1.55 * s, 1.05 * s
            paste(img, col("A9C2D8"), cover(ellipse(x, y, ex, ey - 0.1 * s, rx + 0.2 * s, ry + 0.3 * s), soft=1.2 * s) * 0.22)    # eye shadow
            white = cover(ellipse(x, y, ex, ey, rx, ry))
            paste(img, col("F2F8FD"), white)
            iris = cover(ellipse(x, y, ex, ey + 0.02 * s, 0.9 * s, 1.0 * s)) * white
            t = np.clip((y - (ey - 1.0 * s)) / (2.0 * s), 0, 1)
            paste(img, col("4F86C2"), iris)
            paste(img, col("A6DCFB"), iris * (1 - 0.7 * t))
            paste(img, col("E4FAFF"), iris * np.clip((t - 0.55) * 2.2, 0, 1) * 0.9)
            paste(img, col("1E4668"), cover(disc(x, y, ex, ey + 0.03 * s, 0.36 * s)) * white)                      # pupil
            paste(img, col("FFFFFF"), cover(disc(x, y, ex - 0.3 * s, ey - 0.36 * s, 0.22 * s)) * white)             # catch-light
            add_glow(g, col("BFF0FF"), iris * 0.22)
            lid = capsule(x, y, ex - 1.6 * s, ey - 0.25 * s, ex, ey - 1.02 * s, 0.13 * s)                            # the upper lid: one soft arc
            lid = np.minimum(lid, capsule(x, y, ex, ey - 1.02 * s, ex + 1.65 * s, ey - 0.32 * s, 0.13 * s))
            paste(img, col("4A6E96"), cover(lid) * 0.9)
            paste(img, col("4A6E96"), cover(capsule(x, y, ex + side * 1.45 * s, ey - 0.35 * s, ex + side * 1.95 * s, ey - 0.62 * s, 0.1 * s)) * 0.8)    # a lash at the outer corner
            brow = capsule(x, y, w * 0.5 + side * 0.7 * s, h * 0.375, w * 0.5 + side * 3.05 * s, h * 0.355, 0.15 * s)    # level, thin brows
            paste(img, col("7FA4D0"), cover(brow) * 0.9)
        paste(img, col("A9C2D8"), cover(capsule(x, y, w * 0.5, h * 0.62, w * 0.5 - 0.1 * s, h * 0.73, 0.13 * s)) * 0.55)        # the nose
        mx, my = w * 0.5, h * 0.855
        paste(img, col("8BA8CC"), cover(capsule(x, y, mx - 0.95 * s, my, mx + 0.95 * s, my, 0.1 * s)))                  # the mouth line
        paste(img, col("B9CFEA"), cover(ellipse(x, y, mx, my + 0.3 * s, 0.75 * s, 0.22 * s)) * 0.85)                      # the lower lip, pale blue-pink
        paste(img, col("C8D6EE"), cover(ellipse(x, y, mx, my - 0.24 * s, 0.7 * s, 0.17 * s)) * 0.6)
        paste(img, col("E4B8C8"), cover(ellipse(x, y, w * 0.30, h * 0.74, 1.0 * s, 0.5 * s), soft=2.4 * s) * 0.12)          # a very light flush
        paste(img, col("E4B8C8"), cover(ellipse(x, y, w * 0.70, h * 0.74, 1.0 * s, 0.5 * s), soft=2.4 * s) * 0.12)
        return img, g
    return FnMat(fn, "face")


# ---------------------------------------------------------------------------------------------------------------------- the materials
def hair_mat(seed, tip=False, light=0.0):
    def fn(face, w, h, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 211) % (2 ** 32))
        u, v, x, y = grid(w, h)
        n = fbm(rng, w, h, 3, 4)
        streak = 0.5 + 0.5 * np.sin(u * w * 2.3 + n * 4.0 + v * 1.5)
        lum = 0.32 + 0.45 * streak + 0.20 * (1 - v) + light
        lum = np.clip(lum, 0, 1)
        c = np.where((lum < 0.5)[..., None], HAIR_D + (HAIR - HAIR_D) * (lum * 2)[..., None], HAIR + (HAIR_L - HAIR) * ((lum - 0.5) * 2)[..., None])
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = c
        img[..., 3] = 255
        if tip and face in ("north", "south", "east", "west"):
            r = smooth(1.30 - v - (fbm(rng, w, h, 3, 5) - 0.5) * 0.7, 0.05, 0.28)               # the tips thin out and fray
            img[..., 3] = 255 * r
        g = np.zeros((h, w, 4), np.float32)
        add_glow(g, col("BDEBFF"), np.clip(streak - 0.78, 0, 1) * 4.0 * 0.25 * (img[..., 3] > 100))
        return img, g
    return FnMat(fn, "hair")


def shell_mat(seed):
    """A pearl scallop shell: fan ridges from the hinge, a pearly sheen that shifts pink - white - blue, glowing ridges."""
    def fn(face, w, h, tag):
        u, v, x, y = grid(w, h)
        ang = np.arctan2(u - 0.5, 1.02 - v)
        ridge = 0.5 + 0.5 * np.cos(ang * 9.0)
        lum = 0.55 + 0.35 * ridge - 0.12 * v
        sheen = film(0.25 * u + 0.35 * v + seed * 0.13, sat=0.16, lo=0.5, hi=0.95)
        c = np.where((lum < 0.5)[..., None], SHELL_D + (SHELL - SHELL_D) * (lum * 2)[..., None], SHELL + (SHELL_L - SHELL) * ((lum - 0.5) * 2)[..., None])
        c = c * 0.78 + sheen * 0.22
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = c
        img[..., 3] = 255
        g = np.zeros((h, w, 4), np.float32)
        add_glow(g, CYAN, np.clip(ridge - 0.86, 0, 1) * 5.0 * 0.5)
        return img, g
    return FnMat(fn, "shell")


def scales_mat(t0, t1, seed, cell=5.0):
    """Fish scales on a tail segment; the colour runs from the blue-grey of the hip (t = 0) through deep blue to the cyan of the fin (t = 1); `t0` / `t1` is this segment's span."""
    def fn(face, w, h, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 313) % (2 ** 32))
        u, v, x, y = grid(w, h)
        cs = max(3.0, cell * (w / max(1.0, w)))
        rowh = cs * 0.62
        row = np.floor(y / rowh)
        xo = (row % 2) * cs * 0.5
        fx = ((x + xo) % cs) / cs                                           # 0..1 across a scale
        fy = (y % rowh) / rowh                                              # 0..1 down a scale row
        d = np.hypot((fx - 0.5) * 1.9, (1.0 - fy) * 1.15)                   # distance from the scale's rounded bottom edge
        body = np.clip(1.0 - d, 0, 1)
        edge = np.clip((d - 0.80) * 5.0, 0, 1) * (d < 1.12)
        tt = np.clip(t0 + (t1 - t0) * v, 0, 1)
        base = np.where((tt < 0.5)[..., None], TAIL_TOP + (TAIL_MID - TAIL_TOP) * (tt * 2)[..., None], TAIL_MID + (TAIL_END - TAIL_MID) * ((tt - 0.5) * 2)[..., None])
        lum = 0.98 + 0.30 * body - 0.20 * edge + (fbm(rng, w, h, 3, 4) - 0.5) * 0.10
        c = base * lum[..., None]
        c = c + np.clip(body - 0.7, 0, 1)[..., None] * 90 * np.array([0.6, 0.9, 1.0])
        if face == "up":
            c = c * 1.1
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = c
        img[..., 3] = 255
        g = np.zeros((h, w, 4), np.float32)
        add_glow(g, CYAN, np.clip(body - 0.72, 0, 1) * 4.0 * (0.4 + 0.6 * tt) * (hash2(np.floor((x + xo) / cs), row, seed) > 0.32))
        return img, g
    return FnMat(fn, "scales")


def fin_mat(kind, seed=0, W=30.0, H=14.0):
    """A fin: translucent membrane between opaque ribs that fan out from the base; kind 'fluke' (the tail fin, base at the top, a notch and two lobes), 'ear' (the fin-ears: a fan of
    five spines sweeping back from the ear hole, the membrane scalloped between them) or 'hip' (a small fin). W / H are the plate's size in model px."""
    def fn(face, w, h, tag):
        img, g = blank(w, h)
        if kind == "ear":
            if face not in ("east", "west"):
                return img, g
        elif face not in ("north", "south"):
            return img, g
        u, v, x, y = grid(w, h)
        k = w / W                                                          # texels per model px
        if kind == "fluke":
            if face == "south":
                u = 1 - u
            px, py = (u - 0.5) * W, v * H
            ang = np.arctan2(px, py + 1.2)                                   # the angle from the base, 0 straight down
            rad = np.hypot(px, py + 1.2)
            lobe = np.clip(1 - np.abs(np.abs(ang) - 0.62) / 0.70, 0, 1)
            extent = (H + 0.6) * (0.50 + 0.50 * lobe) * (1.0 - 0.20 * np.clip(1 - np.abs(ang) / 0.18, 0, 1))
            extent = np.where(np.abs(ang) > 1.28, 0, extent)
            dist = rad - extent
            rays = 0.5 + 0.5 * np.cos(ang * 17.0)
            frac = np.clip(rad / (H + 0.6), 0, 1)
        elif kind == "ear":
            ub = (1 - u) if face == "east" else u                            # 0 at the ear hole (front), 1 at the back
            px, py = ub * W, v * H
            ang = np.arctan2(-(py - 0.60 * H), px + 0.5)                       # +up, -down, 0 straight back
            rad = np.hypot(px + 0.5, py - 0.60 * H)
            a0, a1 = -0.75, 1.15
            tt = (ang - a0) / (a1 - a0)
            spines = 0.5 + 0.5 * np.cos(tt * TAU * 2.5)                      # five spines: peaks where the cosine is 1
            env = np.clip(1 - np.abs(tt - 0.5) * 1.45, 0, 1)
            extent = W * (0.30 + 0.70 * env) * (0.62 + 0.38 * spines ** 0.7)
            extent = np.where((tt < 0) | (tt > 1), 0, extent)
            dist = rad - extent
            rays = spines
            frac = np.clip(rad / W, 0, 1)
        else:                                                                # hip fin
            if face == "south":
                u = 1 - u
            px, py = u * W, v * H
            dist = np.hypot((px - 0.5 * W) / (0.5 * W), (py - 0.5 * H) / (0.6 * H)) * 0.5 * W - 0.5 * W
            rays = 0.5 + 0.5 * np.cos(np.arctan2(py - 0.1 * H, px - 0.5 * W) * 11.0)
            frac = np.clip(np.hypot(px - 0.5 * W, py) / H, 0, 1)
        a = cover(dist * k, soft=1.2)
        rib = rays > 0.9
        memb = FIN + (FIN_L - FIN) * (1 - frac)[..., None] * 0.5
        c = np.where(rib[..., None], FIN_D, memb)
        img[..., :3] = c
        img[..., 3] = np.where(rib, 235, 150) * a
        rim = (dist > -0.45) & (dist < 0.12)
        img[..., :3] = np.where(rim[..., None], FIN_L, img[..., :3])
        img[..., 3] = np.where(rim, 245 * a, img[..., 3])
        ga = np.where(rib, 0.60, 0.05 + 0.06 * (1 - frac)) * a
        ga = np.where(rim, 0.70 * a, ga)
        g[..., :3] = CYAN[None, None, :] * ga[..., None]
        g[..., 3] = np.where(ga > 0.05, 255, 0)
        return img, g
    return FnMat(fn, "fin_" + kind)


def water_mat(seed, alpha=128):
    """Flowing water for the layer that follows her: bands and caustic nets, translucent, foam-white crest, glowing."""
    def fn(face, w, h, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 101) % (2 ** 32))
        u, v, x, y = grid(w, h)
        n = fbm(rng, w, h, 3, 3)
        bands = 0.5 + 0.5 * np.sin(v * h * 0.7 + n * 6.0 + u * w * 0.2)
        caustic = np.abs(np.sin(u * w * 0.8 + n * 5) * np.sin(v * h * 0.6 + n * 4)) < 0.10
        lum = 0.45 + 0.30 * bands + 0.2 * (1 - v)
        c = col("5EB8F2") + (col("E8FAFF") - col("5EB8F2")) * np.clip(lum - 0.4, 0, 1)[..., None] * 1.4
        c = np.where(caustic[..., None], col("F4FDFF"), c)
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = c
        img[..., 3] = np.where(caustic, 220, alpha * (0.7 + 0.5 * bands)) * np.clip(1.6 * (1 - v) + 0.4, 0, 1)
        g = np.zeros((h, w, 4), np.float32)
        add_glow(g, CYAN, np.where(caustic, 0.85, np.clip(bands - 0.9, 0, 1) * 3) * 0.7)
        return img, g
    return FnMat(fn, "water")


# ---------------------------------------------------------------------------------------------------------------------- the model
def mermaid():
    p = Piece(KEY, "undine_mermaid", density=3)
    A = Anim()
    sk = skin()
    p.bone("body", (0, 24.0, 0), parent="root")
    p.bone("torso", (0, 27.0, 0), parent="body", rotation=(5, 0, 0))
    p.bone("head", (0, 41.6, 0.4), parent="torso", rotation=(-5, 0, 0))

    # ---- the torso: hips, waist, chest, bare shoulders, the bodice of two pearl shells and a band of water
    p.box("body", (-4.9, 23.2, -3.1), (9.8, 4.6, 6.2), scales_mat(0.0, 0.08, 1, cell=4.0), d=3)                   # the hips, where the scales begin
    p.box("torso", (-4.1, 27.0, -2.7), (8.2, 5.2, 5.4), sk, d=3)                                                  # waist
    p.box("torso", (-4.7, 31.8, -3.0), (9.4, 6.2, 6.0), sk, d=3)                                                  # ribs and chest
    p.box("torso", (-6.0, 37.2, -2.7), (12.0, 2.5, 5.4), sk, d=3)                                                 # the shoulder line, bare
    p.box("torso", (-1.4, 39.6, -1.4), (2.8, 2.4, 2.8), sk, d=3)                                                  # neck
    for sx in (-1, 1):                                                                                           # the shell cups
        p.box("torso", (sx * 2.4 - 2.3, 33.2, -4.5), (4.6, 4.0, 1.9), shell_mat(1 + (sx > 0)), d=4, rot=(-8, sx * 8, 0), pivot=(sx * 2.4, 36.0, -3.4))
        p.box("torso", (sx * 2.4 - 1.5, 36.6, -4.2), (3.0, 1.1, 1.0), shell_mat(3), d=4, rot=(-8, sx * 8, 0), pivot=(sx * 2.4, 36.0, -3.4))
    p.box("torso", (-1.2, 34.6, -4.6), (2.4, 2.4, 1.4), shell_mat(4), d=4)                                       # the pearl between the shells
    p.box("torso", (-4.9, 29.0, -3.05), (9.8, 2.6, 6.1), water_mat(5, 200), d=3)                                  # a band of water round the waist, foam on top
    p.box("torso", (-4.95, 30.9, -3.1), (9.9, 0.7, 6.2), solid(col("F2FCFF"), 235, CYAN, 0.7), d=3)

    # ---- the head: pale blue eyes, a serious face
    p.box("head", (-4.0, 41.8, -4.0), (8.0, 8.8, 8.0), face_mat(), d=4)
    p.box("head", (-4.35, 48.9, -4.35), (8.7, 2.5, 8.7), hair_mat(1), d=3)                                        # the crown of the hair
    p.box("head", (-4.35, 41.2, 2.6), (8.7, 9.2, 1.8), hair_mat(2), d=3)                                          # the back of the head
    for sx in (-1, 1):
        p.box("head", (sx * 4.0 - 0.65, 43.0, -4.4), (1.3, 7.6, 2.4), hair_mat(3 + (sx > 0), light=0.1), d=3)     # the locks parted at the front
        p.box("head", (sx * 4.0 - 0.65, 43.0, -2.2), (1.3, 7.6, 4.8), hair_mat(5 + (sx > 0)), d=3)
    # fin ears: a ribbed web that sweeps back and out from the side of the head
    for sx, tag in ((1, "l"), (-1, "r")):
        p.bone("ear_" + tag, (sx * 4.1, 46.4, 0.0), parent="head", rotation=(0, sx * 26, sx * 10))
        p.box("ear_" + tag, (sx * 4.1 - 0.25, 43.4, -0.6), (0.5, 7.0, 9.0), fin_mat("ear", seed=1, W=9.0, H=7.0), d=4)
        p.box("ear_" + tag, (sx * 4.1 - 0.4, 45.2, -1.2), (0.8, 2.6, 1.8), shell_mat(6), d=4)                 # the base of the fin, a pearl-like cuff

    # ---- arms: slender, bare, held a little away from the body; a small fin runs along each forearm
    for sx, tag in ((1, "l"), (-1, "r")):
        p.bone("arm_" + tag, (sx * 6.9, 37.8, 0), parent="torso", rotation=(0, 0, sx * 20))
        p.bone("arm_%s2" % tag, (sx * 6.9, 29.8, 0), parent="arm_" + tag, rotation=(16, 0, sx * -8))
        p.box("arm_" + tag, (sx * 6.9 - 1.25, 29.8, -1.25), (2.5, 8.2, 2.5), sk, d=3)
        p.box("arm_%s2" % tag, (sx * 6.9 - 1.1, 22.2, -1.1), (2.2, 7.8, 2.2), sk, d=3)
        p.box("arm_%s2" % tag, (sx * 6.9 - 1.25, 19.6, -1.35), (2.5, 2.8, 2.7), sk, d=3)                            # the hand
        for f in range(3):                                                                                       # webbed fingers
            p.box("arm_%s2" % tag, (sx * 6.9 - 1.2 + f * 0.85, 17.6, -0.9), (0.7, 2.4, 1.0), sk, d=4)
        p.box("arm_%s2" % tag, (sx * 6.9 + sx * 1.0 - 0.25, 21.4, -1.0), (0.5, 7.0, 3.0), fin_mat("hip", seed=2, W=3.0, H=7.0), d=4)    # the fin on the forearm

    # ---- the tail: seven scaled segments in an S, curling back and then down, ending in the fluke, which hangs facing front and back so it can be seen
    deltas = [0.0, 25.0, 25.0, 20.0, 10.0, -10.0, -25.0]                                                           # how far each segment turns back more than the one above
    prev, y, z = "body", 24.0, 0.0
    widths = [9.6, 8.6, 7.6, 6.6, 5.6, 4.7, 3.9]
    deps = [6.6, 6.0, 5.4, 4.8, 4.2, 3.6, 3.1]
    SEG = 4.6
    for i in range(7):
        name = "tail%d" % i
        p.bone(name, (0, y, z), parent=prev, rotation=(-deltas[i], 0, 0))
        w, dd = widths[i], deps[i]
        p.box(name, (-w / 2, y - SEG - 0.5, z - dd / 2), (w, SEG + 0.6, dd), scales_mat(i / 7.0, (i + 1) / 7.0, 10 + i, cell=6.0), d=3)
        if i in (1, 3, 5):                                                                                       # dorsal fin plates along the outside of the curl
            p.box(name, (-0.3, y - SEG, z + dd / 2 - 0.1), (0.6, 3.6, 2.4 + (6 - i) * 0.1), fin_mat("hip", seed=3 + i, W=2.6, H=3.6), d=4)
        prev, y = name, y - SEG
    p.bone("fluke", (0, y, z), parent=prev, rotation=(25.0, 0, 0))
    p.box("fluke", (-15.0, y - 13.2, z - 0.35), (30.0, 14.0, 0.7), fin_mat("fluke", seed=1, W=30.0, H=14.0), d=3)
    for sx in (-1, 1):                                                                                           # two small hip fins where the tail begins
        p.bone("hipfin_%d" % (sx > 0), (sx * 5.0, 26.0, 0), parent="body", rotation=(0, 0, sx * 16))
        p.box("hipfin_%d" % (sx > 0), (sx * 5.0 + (0 if sx > 0 else -3.6), 20.0, -0.3), (3.6, 7.0, 0.6), fin_mat("hip", seed=4, W=3.6, H=7.0), d=4)

    # ---- the hair: a curtain of long wavy locks. The middle locks end at the shoulder blades so the tail curls out through the middle; the outer locks fall long and sweep outward,
    # so the hair frames her whole figure and fills the frame. Every lock waves with a lag along its length.
    p.bone("hair", (0, 50.0, 3.0), parent="head")
    locks, rest = [], {}
    xs = [-10.0, -7.5, -5.0, -2.5, 0.0, 2.5, 5.0, 7.5, 10.0]
    for k, x in enumerate(xs):
        edge = abs(x) / 10.0
        nseg = 5 + int(round(4 * edge ** 1.2))                                                                   # 5 in the middle, 9 at the edges
        prev, py = "hair", 50.0
        depth = 3.4 + 1.1 * (k % 3) + 1.4 * (1 - edge)
        for i in range(nseg):
            name = "L%d_%d" % (k, i)
            sgn = 1.0 if x >= 0 else -1.0
            rz = sgn * 7.0 * edge if i == 0 else 15.0 * math.cos(1.0 * i + k * 1.9) + sgn * 1.8 * edge * min(i, 6) / 3.0
            rx = 0.0 if i else 0.0
            p.bone(name, (x, py, depth), parent=prev, rotation=(rx, 0, rz))
            rest[name] = (rx, rz)
            w = 6.6 - 0.34 * i
            tip = i >= nseg - 2
            p.box(name, (x - w / 2, py - 5.4, depth), (w, 5.6, 1.5), hair_mat(10 + k * 9 + i, tip=tip, light=0.05 * ((k + i) % 3 - 1)), d=2)
            locks.append(name)
            prev, py = name, py - 5.0
    for sx, tag in ((1, "l"), (-1, "r")):                                                                        # the front locks, one over each shoulder, framing the shells
        prev, py = "head", 49.6
        for i in range(6):
            name = "F%s%d" % (tag, i)
            rx = 0.0
            rz = sx * (3.0 if i == 0 else 6.0 * math.cos(1.1 * i + (sx > 0)))
            p.bone(name, (sx * 5.6, py, -3.2), parent=prev, rotation=(rx, 0, rz))
            rest[name] = (rx, rz)
            w = 3.8 - 0.3 * i
            p.box(name, (sx * 5.6 - w / 2, py - 5.4, -4.0), (w, 5.6, 1.5), hair_mat(300 + i + (sx > 0) * 7, tip=(i >= 4)), d=2)
            locks.append(name)
            prev, py = name, py - 5.0

    tails = ["tail%d" % i for i in range(7)] + ["fluke"]
    rest_tail = {("tail%d" % i): -deltas[i] for i in range(7)}
    rest_tail["fluke"] = 25.0
    # ---- animation
    L = 4.0
    A.clip("idle", L, True)
    A.pos("idle", "root", {0.0: (0, 0, 0), 1.0: (0, 1.6, 0), 2.0: (0, 0, 0), 3.0: (0, 1.6, 0), L: (0, 0, 0)})
    sine_rot(A, "idle", "torso", L, 2, 2.0, 0.1, offset=(5, 0, 0))
    sine_rot(A, "idle", "head", L, 1, 3.5, 0.2, offset=(-5, 0, 0))
    sine_rot(A, "idle", "head", L, 0, 2.0, 0.4, offset=(-5, 0, 0))
    for i, n in enumerate(tails):
        sine_rot(A, "idle", n, L, 0, 3.5 + 0.35 * i, 0.04 * i, offset=(rest_tail[n], 0, 0))
        sine_rot(A, "idle", n, L, 1, 2.5 + 0.2 * i, 0.05 * i + 0.25)
    for sx, tag in ((1, "l"), (-1, "r")):
        sine_rot(A, "idle", "arm_" + tag, L, 2, 3.0 * sx, 0.1, offset=(0, 0, sx * 20))
        sine_rot(A, "idle", "arm_%s2" % tag, L, 0, 4.0, 0.25, offset=(16, 0, sx * -8))
        sine_rot(A, "idle", "ear_" + tag, L, 1, 2.5 * sx, 0.3, offset=(0, sx * 26, sx * 10))
        sine_rot(A, "idle", "hipfin_%d" % (sx > 0), L, 2, 5.0, 0.2, offset=(0, 0, sx * 16))
    for n in locks:
        i = hair_index(n)
        rx0, rz0 = rest[n]
        sine_rot(A, "idle", n, L, 2, 2.6 + 0.3 * i, 0.07 * i + 0.05 * (ord(n[1]) % 7), offset=(rx0, 0, rz0))
        sine_rot(A, "idle", n, L, 0, 1.8 + 0.18 * i, 0.07 * i + 0.3, offset=(rx0, 0, rz0))

    L = 2.0
    A.clip("move", L, True)
    A.pos("move", "root", {0.0: (0, 0, 0), 0.5: (0, 2.4, 0), 1.0: (0, 0, 0), 1.5: (0, 2.4, 0), L: (0, 0, 0)})
    sine_rot(A, "move", "torso", L, 0, 3.0, 0.0, offset=(-10, 0, 0))
    sine_rot(A, "move", "head", L, 0, 2.0, 0.2, offset=(8, 0, 0))
    for i, n in enumerate(tails):
        sine_rot(A, "move", n, L, 0, 5.0 + 0.3 * i, 0.07 * i, offset=(rest_tail[n], 0, 0))
    for sx, tag in ((1, "l"), (-1, "r")):
        sine_rot(A, "move", "arm_" + tag, L, 0, 12.0, 0.0 if sx > 0 else 0.5, offset=(8, 0, sx * 30))
        A.rot("move", "arm_%s2" % tag, {0.0: (30, 0, sx * -8), L: (30, 0, sx * -8)})
    for n in locks:
        i = hair_index(n)
        rx0, rz0 = rest[n]
        sine_rot(A, "move", n, L, 0, 4.0 + 0.3 * i, 0.09 * i, offset=(rx0 - 3.0 * min(i, 5), 0, rz0))             # the hair streams back
    L = 1.2
    A.clip("cast", L, False)
    A.rot("cast", "arm_l", {0.0: (0, 0, 20), 0.3: (62, 0, 6), 0.8: (62, 0, 6), L: (0, 0, 20)})
    A.rot("cast", "arm_r", {0.0: (0, 0, -20), 0.3: (62, 0, -6), 0.8: (62, 0, -6), L: (0, 0, -20)})
    A.rot("cast", "arm_l2", {0.0: (16, 0, -8), 0.3: (44, 0, -4), 0.8: (44, 0, -4), L: (16, 0, -8)})
    A.rot("cast", "arm_r2", {0.0: (16, 0, 8), 0.3: (44, 0, 4), 0.8: (44, 0, 4), L: (16, 0, 8)})
    A.rot("cast", "head", {0.0: (-5, 0, 0), 0.3: (2, 0, 0), L: (-5, 0, 0)})
    A.rot("cast", "torso", {0.0: (5, 0, 0), 0.3: (-6, 0, 0), 0.8: (-2, 0, 0), L: (5, 0, 0)})
    for n in tails:
        b = rest_tail[n]
        A.rot("cast", n, {0.0: (b, 0, 0), 0.3: (b - 2.5, 0, 0), 0.55: (b + 2.0, 0, 0), L: (b, 0, 0)})
    for n in locks:
        i = hair_index(n)
        rx0, rz0 = rest[n]
        A.rot("cast", n, {0.0: (rx0, 0, rz0), 0.3: (rx0 - 5 - 2 * i, 0, rz0 + (3 if rz0 >= 0 else -3)), 0.8: (rx0 - 3 - 1.5 * i, 0, rz0), L: (rx0, 0, rz0)})
    L = 0.4
    A.clip("hit", L, False)
    A.rot("hit", "torso", {0.0: (5, 0, 0), 0.12: (14, 0, 0), L: (5, 0, 0)})
    A.rot("hit", "head", {0.0: (-5, 0, 0), 0.12: (8, 0, 0), L: (-5, 0, 0)})
    for n in locks:
        i = hair_index(n)
        rx0, rz0 = rest[n]
        A.rot("hit", n, {0.0: (rx0, 0, rz0), 0.12: (rx0 - 4 - i, 0, rz0), L: (rx0, 0, rz0)})
    p.save(A)
    return p


def hair_index(name):
    """The segment number of a hair bone (L3_5 -> 5, Fl2 -> 2)."""
    return int(name.split("_")[-1]) if name[0] == "L" else int(name[2:])


# ---------------------------------------------------------------------------------------------------------------------- the water that follows her
def water():
    p = Piece(KEY, "undine_mermaid_water", density=2)
    A = Anim()
    # two veils of water that stream from the shoulders and wave behind her
    veil = []
    for sx, tag in ((1, "l"), (-1, "r")):
        prev, py = "root", 38.0
        for i in range(7):
            name = "veil_%s%d" % (tag, i)
            p.bone(name, (sx * 8.0, py, 3.0 + 0.6 * i), parent=prev, rotation=(-8.0 if i else 0.0, 0, 0 if i == 0 else sx * 6.0))
            wdt = 5.2 - 0.3 * i
            p.box(name, (sx * 8.0 - wdt / 2, py - 6.2, 3.0 + 0.6 * i), (wdt, 6.4, 0.7), water_mat(20 + i + (sx > 0) * 7, 150), d=3)
            veil.append(name)
            prev, py = name, py - 5.0
    # a slow ring of foam round the waist and drops that circle her
    p.bone("ring", (0, 31.0, 0), parent="root")
    for k in range(18):                                                                                          # a ring of small foam beads
        a = k * 20.0
        p.box("ring", (-0.7, 30.6 + 0.9 * math.sin(k * 1.3), -12.6), (1.4, 1.4, 1.4), solid(col("EAFBFF"), 210, CYAN, 0.8), d=2, rot=(0, a, 0), pivot=(0, 31.0, 0))
    orbs = []
    for n in range(8):
        name = "orb%d" % n
        p.bone(name, (0, 0, 0), parent="root")
        r = 11.0 + (n % 3) * 2.5
        h = 8.0 + n * 5.2
        drop = solid(col("BDEBFF"), 190, CYAN, 0.9, noise=0.04)
        p.box(name, (r - 1.0, h, -1.0), (2.0, 2.4, 2.0), drop, d=2, rot=(0, 45, 0), pivot=(r, h + 1.2, 0))
        p.box(name, (r - 1.0, h, -1.0), (2.0, 2.4, 2.0), drop, d=2, rot=(45, 0, 45), pivot=(r, h + 1.2, 0))
        orbs.append(name)
    L = 4.0
    A.clip("idle", L, True)
    A.pos("idle", "root", {0.0: (0, 0, 0), 1.0: (0, 1.6, 0), 2.0: (0, 0, 0), 3.0: (0, 1.6, 0), L: (0, 0, 0)})
    A.rot("idle", "ring", {L * i / 8: (0, 360.0 * i / 8, 0) for i in range(9)})
    for i, n in enumerate(orbs):
        A.rot("idle", n, {L * j / 8: (0, n_off(i) + 360.0 * j / 8 * (1 if i % 2 else -1), 0) for j in range(9)})
    for n in veil:
        i = int(n[-1])
        sine_rot(A, "idle", n, L, 0, 5.0 + 0.4 * i, 0.08 * i, offset=(-8.0 if i else 0.0, 0, 0))
    L = 2.0
    A.clip("move", L, True)
    A.pos("move", "root", {0.0: (0, 0, 0), 0.5: (0, 2.4, 0), 1.0: (0, 0, 0), 1.5: (0, 2.4, 0), L: (0, 0, 0)})
    A.rot("move", "ring", {L * i / 8: (0, 360.0 * i / 8 * 2, 0) for i in range(9)})
    for i, n in enumerate(orbs):
        A.rot("move", n, {L * j / 8: (0, n_off(i) + 360.0 * j / 8 * (2 if i % 2 else -2), 0) for j in range(9)})
    for n in veil:
        i = int(n[-1])
        sine_rot(A, "move", n, L, 0, 8.0 + 0.5 * i, 0.1 * i, offset=(-8.0 - 5.0 * min(i, 4), 0, 0))
    L = 1.2
    A.clip("cast", L, False)
    A.rot("cast", "ring", {0.0: (0, 0, 0), L: (0, 720, 0)})
    for i, n in enumerate(orbs):
        A.rot("cast", n, {0.0: (0, n_off(i), 0), L: (0, n_off(i) + (720 if i % 2 else -720), 0)})
    for n in veil:
        i = int(n[-1])
        A.rot("cast", n, {0.0: (-8.0 if i else 0.0, 0, 0), 0.3: (-30 - 3 * i, 0, 0), L: (-8.0 if i else 0.0, 0, 0)})
    L = 0.4
    A.clip("hit", L, False)
    A.rot("hit", "ring", {0.0: (0, 0, 0), L: (0, 50, 0)})
    p.save(A)
    return p


def n_off(i):
    return i * 45.0


def main():
    for q in (mermaid(), water()):
        print("%-34s cubes %3d  bones %3d" % (q.id, q.cube_count(), len(q.bones)))


if __name__ == "__main__":
    main()
