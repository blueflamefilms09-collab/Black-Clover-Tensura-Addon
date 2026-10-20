"""Great Spirits (1.99.0): Sylph, the Wind Spirit, as the canon shows her: a TINY winged fairy.

    python3 -B tools/gen_sylph_fairy.py               # writes spirit_sylph_fairy + spirit_sylph_fairy_wings (geo, animation, texture, glow map)
    python3 -B tools/geo_check.py spirit_sylph_fairy spirit_sylph_fairy_wings

The look (the owner's reference): blonde hair swept up in a messy updo with a swept-up tail and loose wisps, green eyes, an annoyed / pouty face, hands on the hips, a pale
lime-green short strapless dress with a flared LAYERED skirt, bare feet, and translucent iridescent wings drawn as a honeycomb of hexagon cells whose thin-film colour runs
green - blue - pink. Canon: jealous and possessive, tornadoes and condensed wind beams; the host is Yuno.

Scale. The models are authored at FOUR times the size they are drawn (27 px tall here; client/ElementalSpiritRenderer draws them at 0.25, so the spirit is about 0.42 of a
block tall) so the face and the wing cells have enough texels. The old models (spirit_sylph, spirit_sylph_wings) stay on disk, written by tools/gen_spirit_lords.py.

Clips (body and wings share the names): idle (hover, hands on the hips, wisps and skirt drifting), move (leans into the flight), cast (a wind beam: one arm points, the skirt
and hair blow back), hit, annoyed (an angry hop with a stomp and a head shake: the renderer plays it now and then while she waits).
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import (FnMat, Piece, Anim, Mat, grid, blank, paste, add_glow, cover, disc, ellipse, capsule, shaded, film, hexcells, hash2,  # noqa: E402
                               col, fbm, smooth, sine_rot, loop_rot, solid, TAU)

KEY = "spirit"

SKIN, SKIN_D, SKIN_L = col("FCE8D0"), col("E0B998"), col("FFF6E8")
HAIR, HAIR_D, HAIR_L = col("FAD96E"), col("C99432"), col("FFF0A8")
LIME, LIME_D, LIME_L = col("C9EC92"), col("93C25C"), col("E8FAC0")
LIME2, LIME2_D = col("B8E27C"), col("84B450")
EYE, EYE_D, EYE_L = col("3CBE62"), col("17743A"), col("A8FFB8")


def skin(**kw):
    return FnMat(lambda f, w, h, t: shaded(f, SKIN, SKIN_D, SKIN_L, w, h, noise=0.03, tag=t, glow=col("FFE8D0"), glow_amount=0.10), "skin")


def hair_mat(seed=0, light=False, glow=0.18):
    base = HAIR_L if light else HAIR

    def fn(face, w, h, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 131) % (2 ** 32))
        u, v, x, y = grid(w, h)
        n = fbm(rng, w, h, 3, 4)
        streak = 0.5 + 0.5 * np.sin(u * w * 1.9 + n * 5.0 + v * 2.0)
        lum = 0.3 + 0.5 * streak * (1 - 0.25 * v) + 0.2 * (1 - v)
        c = np.where((lum < 0.5)[..., None], HAIR_D + (base - HAIR_D) * (lum * 2)[..., None], base + (HAIR_L - base) * ((lum - 0.5) * 2)[..., None])
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = c
        img[..., 3] = 255
        g = np.zeros((h, w, 4), np.float32)
        add_glow(g, HAIR_L, np.clip(streak - 0.8, 0, 1) * 4 * glow)
        return img, g
    return FnMat(fn, "hair")


def dress_mat(seed, light=False, hem=True, trim=False):
    base, dark, lite = (LIME2, LIME2_D, LIME_L) if light else (LIME, LIME_D, LIME_L)

    def fn(face, w, h, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 977) % (2 ** 32))
        u, v, x, y = grid(w, h)
        n = fbm(rng, w, h, 3, 3)
        fold = 0.5 + 0.5 * np.sin(u * w * 0.55 + n * 3.0)
        lum = 0.45 + 0.28 * fold + 0.18 * (1 - v) - 0.06 * v
        lum = np.clip(lum, 0, 1)
        c = np.where((lum < 0.5)[..., None], dark + (base - dark) * (lum * 2)[..., None], base + (lite - base) * ((lum - 0.5) * 2)[..., None])
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = c
        img[..., 3] = 255
        g = np.zeros((h, w, 4), np.float32)
        if face in ("north", "south", "east", "west") and hem:
            period = max(4.0, w / 3.0)                                   # a scalloped hem: arcs hanging off the bottom edge
            arc = (np.abs(((x % period) / period) - 0.5) * 2) ** 2
            edge_y = h * (0.90 - 0.16 * arc)
            img[..., 3] = np.where(y > edge_y, 0, 255)
            line = (np.abs(y - edge_y) < max(1.0, h * 0.04)) & (y <= edge_y + 0.5)
            img[..., :3] = np.where(line[..., None], LIME_D, img[..., :3])
            add_glow(g, col("E6FFC0"), np.where(line, 0.35, 0.0).astype(np.float32))
        if trim:
            ln = (v < 0.16)
            img[..., :3] = np.where(ln[..., None], LIME_L * 0.95, img[..., :3])
        if face == "up":
            img[..., 3] = 255
        return img, g
    return FnMat(fn, "dress")


# ---------------------------------------------------------------------------------------------------------------------- the face
def face_mat():
    """The front of the head: green eyes, annoyed brows (the inner ends low), a pouting mouth, blush. Painted with soft shapes at the cube's density."""
    def fn(face, w, h, tag):
        img, g = shaded(face, SKIN, SKIN_D, SKIN_L, w, h, noise=0.02, tag=tag)
        if face != "north":
            return img, g
        u, v, x, y = grid(w, h)
        s = w / 7.0                                                       # texels per model pixel of the head
        for side in (-1, 1):                                              # image left = the model's right
            ex = w * (0.5 + side * 0.215)
            ey = h * 0.60
            rx, ry = 1.18 * s, 1.30 * s
            white = cover(ellipse(x, y, ex, ey, rx, ry))
            paste(img, col("FFFFFF"), white)
            iris = cover(ellipse(x, y, ex + side * 0.05 * s, ey + 0.1 * s, 0.86 * s, 1.1 * s)) * white
            t = np.clip((y - (ey - 1.1 * s)) / (2.2 * s), 0, 1)
            paste(img, EYE_D, iris)
            paste(img, EYE, iris * (1 - 0.55 * t))
            paste(img, col("D8FFD0"), iris * np.clip((t - 0.55) * 2.2, 0, 1) * 0.8)
            pupil = cover(ellipse(x, y, ex + side * 0.05 * s, ey + 0.05 * s, 0.38 * s, 0.6 * s)) * white
            paste(img, col("0B2A14"), pupil)
            paste(img, col("FFFFFF"), cover(disc(x, y, ex - 0.28 * s + side * 0.05 * s, ey - 0.42 * s, 0.26 * s)) * white)        # catch-light
            paste(img, col("FFFFFF"), cover(disc(x, y, ex + 0.3 * s + side * 0.05 * s, ey + 0.4 * s, 0.13 * s)) * white * 0.9)
            add_glow(g, EYE_L, iris * 0.42)
            # the upper lid: a dark line over the eye, pressed down on the inner side (annoyed)
            lid = capsule(x, y, ex - side * 1.25 * s, ey - 0.95 * s + side * 0.0, ex + side * 1.25 * s, ey - 1.15 * s, 0.17 * s)
            lid = np.minimum(lid, capsule(x, y, ex - side * 1.25 * s, ey - 0.78 * s, ex + side * 0.0 * s, ey - 1.22 * s, 0.16 * s))
            paste(img, col("5A3A1E"), cover(lid))
            # brows: the inner end low and near the nose, the outer end high
            inner_x, outer_x = w * 0.5 + side * 0.4 * s, w * 0.5 + side * 2.85 * s
            brow = capsule(x, y, inner_x, h * 0.40 + 0.55 * s, outer_x, h * 0.40 - 0.45 * s, 0.2 * s)
            paste(img, col("B8862E"), cover(brow))
            blush = cover(ellipse(x, y, w * (0.5 + side * 0.33), h * 0.78, 1.05 * s, 0.55 * s), soft=2.4 * s) * 0.34
            paste(img, col("F2907E"), blush)
        # the mouth: small, pushed out, the corners down
        mx, my = w * 0.5, h * 0.88
        m = capsule(x, y, mx - 0.65 * s, my + 0.1 * s, mx, my - 0.12 * s, 0.17 * s)
        m = np.minimum(m, capsule(x, y, mx, my - 0.12 * s, mx + 0.65 * s, my + 0.1 * s, 0.17 * s))
        paste(img, col("C4605C"), cover(m))
        paste(img, col("E88880"), cover(ellipse(x, y, mx, my + 0.28 * s, 0.4 * s, 0.17 * s)) * 0.7)
        paste(img, col("C99A82"), cover(disc(x, y, w * 0.5, h * 0.76, 0.12 * s)) * 0.6)                                           # the nose
        return img, g
    return FnMat(fn, "face")


# ---------------------------------------------------------------------------------------------------------------------- the wings
WING = {   # kind: plate width (outward), plate height, wing axis from the hinge to the tip in plate fractions (outer, down), half width, trailing-edge factor, cell radius
    "upper": dict(W=17.0, H=18.0, ax=(0.03, 0.90), bx=(0.97, 0.10), wmax=3.7, lead=0.7, cell=1.35),
    "lower": dict(W=13.0, H=12.0, ax=(0.03, 0.14), bx=(0.97, 0.90), wmax=2.9, lead=0.9, cell=1.2),
}


def wing_mat(side, kind, salt):
    """One wing plate as a honeycomb. `side` = +1 for the model's left wing (outer = +x), kind = 'upper' or 'lower'. Cells are hexagons with thin bright borders; inside, a
    thin-film colour that runs green -> blue -> pink over the wing and differs a little from cell to cell. The glow map carries the borders and a faint cell glow."""
    S = WING[kind]

    def fn(face, w, h, tag):
        img, g = blank(w, h)
        if face not in ("north", "south"):
            return img, g
        u, v, x, y = grid(w, h)
        if face == "south":
            u = 1 - u
            x = w - x
        o = u if side > 0 else 1 - u                                       # 0 at the hinge, 1 at the tip
        W, H = S["W"], S["H"]
        k = w / W                                                          # texels per model px
        px, py = o * W, v * H                                              # physical position in px
        ax, ay, bx, by = S["ax"][0] * W, S["ax"][1] * H, S["bx"][0] * W, S["bx"][1] * H
        dx, dy = bx - ax, by - ay
        L = math.hypot(dx, dy)
        dx, dy = dx / L, dy / L
        t = ((px - ax) * dx + (py - ay) * dy) / L
        q = (px - ax) * (-dy) + (py - ay) * dx                              # signed distance across the wing's axis
        tt = np.clip(t, 0, 1)
        prof = np.clip(np.sin(np.pi * tt ** 0.7), 0, 1) ** 0.85
        hw = S["wmax"] * prof * np.where(q < 0, S["lead"], 1.0)
        dist = np.where((t > -0.02) & (t < 1.02), np.abs(q) - hw, 9.0)       # < 0 inside the wing (px)
        dist = dist - 0.12 * np.sin(t * 20 + salt) * prof                    # a very slightly scalloped border
        alpha = cover(dist * k, soft=1.1)
        edge, cq, cr = hexcells(x + salt * 5.3, y + salt * 2.1, S["cell"] * k)
        line = np.clip(1.0 - np.maximum(edge - 0.10 * k, 0) / (0.28 * k), 0, 1)
        hc = hash2(cq, cr, salt)
        ph = 0.55 * o + 0.30 * (1 - v) * (1 if kind == "upper" else -1) + 0.30 * hc + salt * 0.13
        tint = film(ph, sat=0.24, val=1.0)
        tint_edge = film(ph + 0.12, sat=0.46, val=1.0)
        cell_a = 58 + 52 * hash2(cq, cr, salt + 3)
        wall = line > 0.45
        img[..., :3] = np.where(wall[..., None], tint_edge, tint)
        img[..., 3] = np.where(wall, 226, cell_a) * alpha
        rim = (dist > -0.30) & (dist < 0.06)                                 # a brighter, denser border round the whole wing
        img[..., :3] = np.where(rim[..., None], film(ph + 0.30, sat=0.50, val=1.0), img[..., :3])
        img[..., 3] = np.where(rim, 246 * alpha, img[..., 3])
        glow_a = np.where(wall, 0.80, 0.06 + 0.14 * hash2(cq, cr, salt + 9)) * alpha
        glow_a = np.where(rim, 0.95 * alpha, glow_a)
        g[..., :3] = film(ph + 0.1, sat=0.50, val=0.95) * glow_a[..., None]
        g[..., 3] = np.where(glow_a > 0.04, 255, 0)
        return img, g
    return FnMat(fn, "wing")


# ---------------------------------------------------------------------------------------------------------------------- the model
def flare_tier(p, parent, name, y_top, r_top, height, tilt, mat, sides=8, cover_w=None):
    """A flounce of the skirt: `sides` thin panels round a vertical axis, each hung from a hinge on its top edge and swung out by `tilt` degrees (a lamp shade of cloth)."""
    r_bot = r_top + height * math.sin(math.radians(tilt))
    chord = 2 * r_bot * math.tan(math.pi / sides) * (cover_w or 1.04)
    for k in range(sides):
        a = f"{name}{k}"
        p.bone(a, (0, y_top, 0), parent=parent, rotation=(0, k * 360.0 / sides, 0))
        h = f"{name}{k}h"
        p.bone(h, (0, y_top, -r_top), parent=a, rotation=(tilt, 0, 0))
        p.box(h, (-chord / 2, y_top - height, -r_top - 0.25), (chord, height, 0.5), mat, d=4)


def body():
    p = Piece(KEY, "sylph_fairy", density=4)
    A = Anim()
    sk, hair, hair_l = skin(), hair_mat(1), hair_mat(2, light=True)
    # ---- skeleton
    p.bone("hips", (0, 11.5, 0), parent="root")
    p.bone("torso", (0, 12.0, 0), parent="hips")
    p.bone("head", (0, 17.4, 0.2), parent="torso")
    p.bone("bun", (0, 24.0, 2.5), parent="head")
    for sx, tag in ((1, "l"), (-1, "r")):
        p.bone("arm_" + tag, (sx * 3.0, 15.4, 0), parent="torso", rotation=(0, 0, sx * 50))
        p.bone("arm_%s2" % tag, (sx * 3.0, 12.4, 0), parent="arm_" + tag, rotation=(0, 0, -sx * 98))
        p.bone("leg_" + tag, (sx * 1.35, 7.6, 0), parent="hips")
    p.bone("skirtA", (0, 12.4, 0), parent="hips")
    p.bone("skirtB", (0, 10.2, 0), parent="hips")
    p.bone("skirtC", (0, 7.9, 0), parent="hips")
    # ---- legs and bare feet
    for sx, tag in ((1, "l"), (-1, "r")):
        x0 = sx * 1.35 - 0.85
        p.box("leg_" + tag, (x0, 1.1, -0.85), (1.7, 6.5, 1.7), sk, d=4)
        p.box("leg_" + tag, (x0 - 0.05, 0.0, -1.9), (1.8, 1.2, 2.9), sk, d=4)                       # the foot, no shoe
        for k in range(3):                                                                         # toes
            p.box("leg_" + tag, (x0 + 0.1 + k * 0.52, 0.0, -2.35), (0.5, 0.55, 0.5), sk, d=5)
    # ---- the layered flared skirt (three flounces) and the bodice
    flare_tier(p, "skirtC", "sc", 7.9, 4.2, 3.1, 24, dress_mat(3, light=True), cover_w=1.05)
    flare_tier(p, "skirtB", "sb", 10.2, 3.4, 3.2, 20, dress_mat(2), cover_w=1.05)
    flare_tier(p, "skirtA", "sa", 12.4, 2.5, 3.0, 15, dress_mat(1, light=True, trim=False), cover_w=1.05)
    p.box("hips", (-3.0, 10.9, -2.1), (6.0, 1.7, 4.2), dress_mat(4, hem=False), d=3)                # the waist band under the top flounce
    p.box("torso", (-2.95, 12.2, -2.0), (5.9, 4.0, 4.0), dress_mat(5, hem=False), d=4)               # the strapless bodice
    p.box("torso", (-3.15, 15.7, -2.2), (6.3, 0.9, 4.4), dress_mat(6, light=True, hem=False, trim=True), d=4)    # the ruffled top edge
    p.box("torso", (-2.6, 15.2, -1.75), (5.2, 2.4, 3.5), sk, d=4)                                   # bare chest and shoulders above the dress
    p.box("torso", (-1.7, 12.9, 1.9), (3.4, 2.0, 1.0), dress_mat(7, light=True, hem=False), d=3)       # the bow at the back
    p.box("torso", (-0.8, 17.2, -0.8), (1.6, 1.0, 1.6), sk, d=4)                                    # neck
    # ---- the head
    p.box("head", (-3.5, 17.4, -3.2), (7.0, 6.4, 6.4), face_mat(), d=4)
    # ears, a little pointed
    for sx, tag in ((1, "l"), (-1, "r")):
        p.box("head", (sx * 3.55 - 0.25 + (0.0 if sx > 0 else 0.0), 20.0, -0.5), (0.6, 1.9, 1.7), sk, d=4, rot=(0, 0, sx * 22), pivot=(sx * 3.6, 20.2, 0))
        p.box("head", (sx * 4.05 - 0.25, 21.3, -0.3), (0.55, 1.2, 1.0), sk, d=4, rot=(0, 0, sx * 38), pivot=(sx * 3.9, 21.2, 0))
    # ---- hair: a cap, the back mass, the side locks, the messy fringe, then the updo (bun + swept-up tail) and the loose wisps
    p.box("head", (-3.8, 22.7, -3.55), (7.6, 1.9, 7.1), hair, d=3)
    p.box("head", (-3.8, 18.2, 2.6), (7.6, 5.9, 1.7), hair, d=3)
    for sx in (1, -1):
        p.box("head", (sx * 3.7 - 0.55, 18.9, -3.4), (1.1, 4.8, 2.5), hair_l, d=3)                   # the side locks that frame the face
        p.box("head", (sx * 3.7 - 0.55, 18.9, 0.1), (1.1, 4.2, 2.6), hair, d=3)
    for k, (x, rz) in enumerate(((-2.4, 18), (-0.6, -6), (1.3, 14), (2.8, -20))):                     # the messy fringe
        p.box("head", (x - 0.7, 22.0, -3.95), (1.4, 1.9, 0.7), hair_l, d=4, rot=(0, 0, rz), pivot=(x, 23.6, -3.9))
    p.box("bun", (-2.4, 23.7, 0.4), (4.8, 2.6, 4.4), hair_l, d=4, rot=(0, 12, 0), pivot=(0, 24.0, 2.5))                     # the messy updo: a bun on the crown ...
    p.box("bun", (-1.7, 25.6, 1.0), (3.4, 1.5, 3.2), hair, d=4, rot=(0, -22, 5), pivot=(0, 25.6, 2.5))
    p.box("bun", (-1.1, 24.9, 3.0), (2.2, 0.9, 1.0), dress_mat(8, light=True, hem=False), d=4)                                  # ... tied with a lime ribbon
    locks = (("lock0", -46, 24, 3.4), ("lock1", -24, 30, 3.9), ("lock2", -3, 16, 4.2), ("lock3", 20, 28, 3.8), ("lock4", 43, 22, 3.3))        # ... and the locks swept up and back
    for i, (name, rz, rx, L) in enumerate(locks):
        p.bone(name, (0.0, 26.0, 2.6), parent="bun", rotation=(-rx, 0, -rz))
        w = 1.7 - 0.1 * abs(i - 2)
        p.box(name, (-w / 2, 26.0, 2.6 - w / 2), (w, L, w), hair_l if i % 2 else hair, d=4)
    p.bone("tail1", (1.4, 25.4, 3.4), parent="bun", rotation=(0, 0, -8))                                                           # the long tail of the updo curling out to one side
    p.box("tail1", (1.4 - 0.9, 25.4, 3.4 - 0.8), (1.8, 2.6, 1.6), hair, d=4)
    p.bone("tail2", (1.4, 28.0, 3.4), parent="tail1", rotation=(-18, 0, -32))
    p.box("tail2", (1.4 - 0.8, 28.0, 3.4 - 0.7), (1.6, 2.4, 1.4), hair_l, d=4)
    p.bone("tail3", (1.4, 30.4, 3.4), parent="tail2", rotation=(-14, 0, -34))
    p.box("tail3", (1.4 - 0.6, 30.4, 3.4 - 0.6), (1.2, 2.0, 1.2), hair, d=4)
    wisp = [("w0", (-1.8, 26.0, 1.2), (-0.9, 0, 26), 3.4), ("w1", (0.0, 27.0, 3.4), (0.7, 0, -14), 3.8), ("w2", (-3.2, 22.6, 3.0), (0, 0, 32), 4.2),
            ("w3", (3.3, 22.4, 3.3), (0, 0, -30), 4.0), ("w4", (-3.9, 21.5, -2.8), (0, 0, 20), 3.6), ("w5", (3.9, 21.2, -2.8), (0, 0, -22), 3.4)]
    for name, pos, rot, L in wisp:                                                                  # the wisps that escape: loose thin strands that drift
        px, py, pz = pos
        p.bone(name, pos, parent="head")
        p.box(name, (px - 0.28, py - L + 0.6, pz - 0.28), (0.56, L, 0.56), hair_l if name in ("w1", "w4") else hair, d=4, rot=rot, pivot=pos)
    # ---- arms: hands on the hips (the pose is the bones' own rotation; the clips only move around it)
    for sx, tag in ((1, "l"), (-1, "r")):
        x0 = sx * 3.0
        p.box("arm_" + tag, (x0 - 0.65, 12.2, -0.65), (1.3, 3.5, 1.3), sk, d=4)                        # upper arm
        p.box("arm_%s2" % tag, (x0 - 0.6, 9.7, -0.6), (1.2, 2.9, 1.2), sk, d=4)                        # forearm
        p.box("arm_%s2" % tag, (x0 - 0.7, 8.8, -0.75), (1.4, 1.2, 1.5), sk, d=4)                       # hand
    # ---- animation
    L = 2.4
    A.clip("idle", L, True)
    A.pos("idle", "root", {0.0: (0, 0, 0), 0.6: (0, 0.9, 0), 1.2: (0, 0, 0), 1.8: (0, 0.9, 0), L: (0, 0, 0)})
    sine_rot(A, "idle", "hips", L, 2, 2.0)
    sine_rot(A, "idle", "head", L, 1, 5.0, 0.15)
    sine_rot(A, "idle", "head", L, 2, 2.2, 0.4)
    for sx, tag in ((1, "l"), (-1, "r")):
        sine_rot(A, "idle", "arm_" + tag, L, 2, 3.0 * sx, 0.1)
        sine_rot(A, "idle", "leg_" + tag, L, 0, 7.0, 0.3 if sx > 0 else 0.8)
    for n, (ph, amp) in {"skirtA": (0.0, 2.5), "skirtB": (0.12, 3.5), "skirtC": (0.24, 4.5)}.items():
        sine_rot(A, "idle", n, L, 0, amp, ph)
        sine_rot(A, "idle", n, L, 2, amp * 0.6, ph + 0.3)
    for i, n in enumerate(("w0", "w1", "w2", "w3", "w4", "w5")):
        sine_rot(A, "idle", n, L, 2, 9.0 + 2 * i, 0.07 * i)
        sine_rot(A, "idle", n, L, 0, 6.0, 0.2 + 0.09 * i)
    for i, n in enumerate(("tail1", "tail2", "tail3")):
        sine_rot(A, "idle", n, L, 2, 7.0 + 2 * i, 0.1 + 0.12 * i)
    for i in range(5):
        sine_rot(A, "idle", "lock%d" % i, L, 2, 4.0, 0.08 * i, offset=(0, 0, 0))

    L = 0.8
    A.clip("move", L, True)
    A.pos("move", "root", {0.0: (0, 0, 0), 0.2: (0, 1.4, 0), 0.4: (0, 0, 0), 0.6: (0, 1.4, 0), L: (0, 0, 0)})
    A.rot("move", "hips", {0.0: (-14, 0, 0), L: (-14, 0, 0)})
    sine_rot(A, "move", "hips", L, 2, 3.0, 0.1, offset=(-14, 0, 0))
    sine_rot(A, "move", "head", L, 0, 4.0, 0.2, offset=(8, 0, 0))
    for sx, tag in ((1, "l"), (-1, "r")):
        sine_rot(A, "move", "leg_" + tag, L, 0, 16.0, 0.0 if sx > 0 else 0.5, offset=(-16, 0, 0))
        sine_rot(A, "move", "arm_" + tag, L, 2, 5.0 * sx, 0.1)
    for n, (ph, amp) in {"skirtA": (0.0, 8.0), "skirtB": (0.1, 11.0), "skirtC": (0.2, 14.0)}.items():
        sine_rot(A, "move", n, L, 0, amp, ph, offset=(-18, 0, 0))
    for i, n in enumerate(("w0", "w1", "w2", "w3", "w4", "w5")):
        sine_rot(A, "move", n, L, 0, 12.0, 0.08 * i, offset=(-26, 0, 0))
    for i, n in enumerate(("tail1", "tail2", "tail3")):
        sine_rot(A, "move", n, L, 2, 9.0 + 2 * i, 0.1 + 0.1 * i, offset=(-18, 0, 0))

    L = 0.8
    A.clip("cast", L, False)                                                                      # a condensed wind beam: the right arm points ahead, the skirt and hair blow back
    A.rot("cast", "arm_r", {0.0: (0, 0, 0), 0.18: (84, 0, 44), 0.6: (84, 0, 44), L: (0, 0, 0)})          # file frame: +rx swings a hanging limb forward (-z), -rx back
    A.rot("cast", "arm_r2", {0.0: (0, 0, 0), 0.18: (0, 0, 98), 0.6: (0, 0, 98), L: (0, 0, 0)})
    A.rot("cast", "hips", {0.0: (0, 0, 0), 0.2: (-12, 0, 0), 0.6: (-8, 0, 0), L: (0, 0, 0)})
    A.rot("cast", "head", {0.0: (0, 0, 0), 0.2: (8, 0, 0), L: (0, 0, 0)})
    for n, k in (("skirtA", 16), ("skirtB", 22), ("skirtC", 28)):
        A.rot("cast", n, {0.0: (0, 0, 0), 0.2: (-k, 0, 0), 0.65: (-k * 0.8, 0, 0), L: (0, 0, 0)})
    for n in ("w0", "w1", "w2", "w3", "w4", "w5", "tail1", "tail2", "tail3"):
        A.rot("cast", n, {0.0: (0, 0, 0), 0.2: (-34, 0, 0), 0.65: (-28, 0, 0), L: (0, 0, 0)})

    L = 0.35
    A.clip("hit", L, False)
    A.rot("hit", "hips", {0.0: (0, 0, 0), 0.1: (-18, 0, 6), L: (0, 0, 0)})
    A.rot("hit", "head", {0.0: (0, 0, 0), 0.1: (14, 0, -8), L: (0, 0, 0)})
    A.pos("hit", "root", {0.0: (0, 0, 0), 0.1: (0, 0, 2.0), L: (0, 0, 0)})

    L = 1.0
    A.clip("annoyed", L, False)                                                                   # an angry hop: up, down with a stomp, up and down again, the head shaking
    A.pos("annoyed", "root", {0.0: (0, 0, 0), 0.16: (0, 3.4, 0), 0.32: (0, 0, 0), 0.5: (0, 2.4, 0), 0.66: (0, 0, 0), L: (0, 0, 0)})
    A.rot("annoyed", "head", {0.0: (0, 0, 0), 0.12: (0, 14, 0), 0.26: (0, -14, 0), 0.4: (0, 12, 0), 0.56: (0, -10, 0), 0.7: (0, 6, 0), L: (0, 0, 0)})
    for sx, tag in ((1, "l"), (-1, "r")):
        A.rot("annoyed", "arm_" + tag, {0.0: (0, 0, 0), 0.16: (0, 0, 14 * sx), 0.32: (0, 0, 0), 0.5: (0, 0, 10 * sx), L: (0, 0, 0)})
        A.rot("annoyed", "leg_" + tag, {0.0: (0, 0, 0), 0.16: (-20 if sx > 0 else 12, 0, 0), 0.32: (4, 0, 0), 0.5: (12 if sx > 0 else -20, 0, 0), 0.66: (0, 0, 0), L: (0, 0, 0)})
    for n, k in (("skirtA", 8), ("skirtB", 11), ("skirtC", 14)):
        A.rot("annoyed", n, {0.0: (0, 0, 0), 0.16: (-k, 0, 0), 0.32: (k, 0, 0), 0.5: (-k * 0.7, 0, 0), 0.66: (k * 0.5, 0, 0), L: (0, 0, 0)})
    p.save(A)
    return p


def wings():
    p = Piece(KEY, "sylph_fairy_wings", density=4)
    A = Anim()
    p.bone("wings", (0, 15.0, 2.3), parent="root")
    plates = {}
    for sx, tag in ((1, "l"), (-1, "r")):
        for kind, yaw in (("upper", 14.0), ("lower", 20.0)):
            S = WING[kind]
            name = "wing_%s_%s" % (kind[0], tag)
            hy = 15.2 if kind == "upper" else 14.0
            y0 = hy - S["H"] * (1 - S["ax"][1]) if kind == "upper" else hy - S["H"] * (1 - S["ax"][1])
            hinge = (sx * 0.7, hy, 2.5)
            p.bone(name, hinge, parent="wings", rotation=(0, sx * -yaw, 0))
            ox = sx * 0.7 - (S["W"] * S["ax"][0]) if sx > 0 else -0.7 - S["W"] * (1 - S["ax"][0])
            p.box(name, (ox, y0, 2.5), (S["W"], S["H"], 0.45), wing_mat(sx, kind, 1 + (sx < 0) * 2 + (kind == "lower") * 5), d=4)
            plates[name] = sx
    L = 2.4
    for clip, per, amp, length in (("idle", 0.30, 22.0, 2.4), ("move", 0.16, 30.0, 0.8), ("cast", 0.20, 34.0, 0.8), ("annoyed", 0.12, 34.0, 1.0), ("hit", 0.2, 12.0, 0.35)):
        A.clip(clip, length, clip not in ("cast", "hit", "annoyed"))
        n = max(1, int(round(length / per)))
        for name, sx in plates.items():
            k = 1.0 if "_u_" in name else 0.78
            frames = {}
            for q in range(n * 4 + 1):
                t = min(q * per / 4, length)
                frames[t] = (0, sx * -amp * k * math.sin(TAU * t / per) * (1 if clip != "hit" else 0.4), 0)
            A.rot(clip, name, frames)
        A.pos(clip, "root", {0.0: (0, 0, 0), length: (0, 0, 0)})
    # the wings ride the body's hover
    A.pos("idle", "root", {0.0: (0, 0, 0), 0.6: (0, 0.9, 0), 1.2: (0, 0, 0), 1.8: (0, 0.9, 0), 2.4: (0, 0, 0)})
    A.pos("move", "root", {0.0: (0, 0, 0), 0.2: (0, 1.4, 0), 0.4: (0, 0, 0), 0.6: (0, 1.4, 0), 0.8: (0, 0, 0)})
    A.pos("annoyed", "root", {0.0: (0, 0, 0), 0.16: (0, 3.4, 0), 0.32: (0, 0, 0), 0.5: (0, 2.4, 0), 0.66: (0, 0, 0), 1.0: (0, 0, 0)})
    A.rot("move", "wings", {0.0: (-14, 0, 0), 0.8: (-14, 0, 0)})
    A.rot("cast", "wings", {0.0: (0, 0, 0), 0.2: (-12, 0, 0), 0.6: (-8, 0, 0), 0.8: (0, 0, 0)})
    p.save(A)
    return p


def main():
    for m in (body(), wings()):
        print("%-30s cubes %3d" % (m.id, m.cube_count()))


if __name__ == "__main__":
    main()
