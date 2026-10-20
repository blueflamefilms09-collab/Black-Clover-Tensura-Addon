"""Great Spirits (1.99.0): the pieces of the SPIRIT DIVE (Spirit Assimilation), one GeoLite model per piece, in whole-body coordinates (see tools/water_aura_lib.py):

    python3 -B tools/gen_spirit_dive_geo.py           # writes geo/entity/spiritdive_arm_*.geo.json, animations, textures + _glow maps, and the four body overlays aura/spirit_*
    python3 -B tools/geo_check.py spiritdive_arm_wind_wing_l ...

The mage merges the spirit into the body: the body is altered and cloaked in dense elemental mana (the owner's canon). The painter is client/aura/SpiritDiveAura.java (Aura.SPIRIT_DIVE; the
style number says the element and the stage, see book/SpiritDivePieces), the piece lists per element and stage live in that pure class and the names below are the same.

  WIND   (Yuno and Sylph)       first dive: ONLY THE LEFT HALF transforms: one wind wing, half a crown, a halo ring round the wrist (+ a half mantle, sleeve and greave);
                                full dive: two wings, the full crown, symmetrical, rings round both wrists.
  WATER  (Noelle and Undine)    the Valkyrie armour is the old water set (tools/gen_water_aura_geo.py, drawn tinted); the SAINT VALKYRIE DRESS adds feathered wings, a halo, feathered
                                sleeves and a gold sash, white - gold - blue.
  FIRE   (Fuegoleon, Salamander) arms cloaked in fire mana with draconic scales, claws and fiery wings (small wings first, the full pair later), draconic greaves and chest at the full stage.
  EARTH  (the unnamed earth spirit) a generic stone dive: mossy plates, boulder pauldrons, stone fists, a brow crown of crystals, floating rocks at the full stage.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import (FnMat, Piece, Anim, Mat, grid, blank, paste, add_glow, cover, hexcells, hash2, film, col, fbm, smooth, sine_rot, loop_rot,  # noqa: E402
                               spin_y, mbox, stepped_spike, shell_texture, rgb, TAU)

KEY = "spiritdive"


# ====================================================================================================================== materials
# ---- wind: translucent green-white mana, crystalline leaf plates, honeycomb wings
W_GLASS = Mat("crystal", "BDF4D8", dark="5CBE98", light="F2FFF8", glow="98FFD0", glow_amount=0.7, alpha=150, cells=5, seed=41)
W_PLATE = Mat("plate", "CFF5E0", dark="68BC94", light="F4FFF8", glow="98FFD0", glow_amount=0.7, seed=43)
W_LEAF = Mat("feather", "C8F8DC", dark="5CC48C", light="FFFFFF", glow="A8FFD6", glow_amount=0.95, tip=1, seed=42)
W_GOLD = Mat("metal", "F0E8A8", dark="9C8A44", light="FFFFE0", glow="FFF2A0", glow_amount=0.8, seam=1, seed=44)

# ---- fire: draconic scales, bone-coloured horns and claws, flame feathers
F_SCALE = Mat("scales", "B5301C", dark="4A0C06", light="F27A40", glow="FF7A22", glow_amount=0.9, cells=5, seed=51)
F_SCALE2 = Mat("scales", "8E2012", dark="3A0804", light="D8582A", glow="FF6A1E", glow_amount=0.75, cells=6, seed=52)
F_HORN = Mat("plate", "F0DCA0", dark="8A6A38", light="FFF6D0", glow="FFD070", glow_amount=0.55, seed=53)
F_FEATHER = Mat("feather", "E8561C", dark="9A1C0C", light="FFD060", glow="FF9A2E", glow_amount=0.95, seed=54)
F_FLAME = Mat("flame", "FF7C1E", dark="B3200C", light="FFE276", glow="FF9830", glow_amount=0.95, seed=55, grain=0.04)
F_GOLD = Mat("metal", "E0AE35", dark="70490A", light="FFF0B0", glow="FFD070", glow_amount=0.6, seam=1, seed=56)

# ---- saint valkyrie: white feathers, gold, water blue
S_FEATHER = Mat("feather", "F2F6FF", dark="9CC0F0", light="FFFFFF", glow="FFE6A0", glow_amount=0.8, seed=61)
S_FEATHER2 = Mat("feather", "DCEBFF", dark="7CA8E8", light="FFFFFF", glow="BFEFFF", glow_amount=0.8, seed=62)
S_GOLD = Mat("metal", "EBC45E", dark="8A6420", light="FFF2B8", glow="FFE08A", glow_amount=0.85, seam=1, seed=63)
S_GOLD_P = Mat("plate", "EEC860", dark="8A6420", light="FFF4C0", glow="FFE49A", glow_amount=0.7, inlay=1, seed=64)
S_WATER = Mat("water", "6EBEF4", dark="2C78D0", light="F2FCFF", glow="A8F4FF", glow_amount=0.9, alpha=200, seed=65)


def _vor(x, y, cell, salt=0):
    """Vectorised cell noise: returns (edge, id) where edge = distance to the second nearest point minus the nearest (0 on a crack)."""
    gx, gy = np.floor(x / cell), np.floor(y / cell)
    best = np.full(x.shape, 1e9, np.float32)
    second = np.full(x.shape, 1e9, np.float32)
    bid = np.zeros(x.shape, np.float32)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            cx, cy = gx + dx, gy + dy
            px = (cx + hash2(cx, cy, salt)) * cell
            py = (cy + hash2(cx, cy, salt + 5)) * cell
            d = np.hypot(x - px, y - py)
            closer = d < best
            second = np.where(closer, best, np.minimum(second, d))
            bid = np.where(closer, hash2(cx, cy, salt + 9), bid)
            best = np.where(closer, d, best)
    return second - best, bid


def stone(seed, moss=0.35, cell=4.0, crack_glow="FFB040", glow_amount=0.5, tone=1.0):
    """Cracked stone in plates, moss on the upper faces and in the hollows, amber light in some of the cracks."""
    base, dark, light, mossc = rgb("9C8E7A"), rgb("463C32"), rgb("CFC2A8"), rgb("5C8A3C")

    def fn(face, w, h, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 389) % (2 ** 32))
        u, v, x, y = grid(w, h)
        k = max(1.0, w / 8.0)
        edge, cid = _vor(x + (13 if face in ("up", "down") else 0), y, cell * k, seed)
        n = fbm(rng, w, h, 4, 3)
        lum = (0.34 + 0.5 * cid + (n - 0.5) * 0.35) * tone
        if face == "up":
            lum += 0.14
        elif face == "down":
            lum -= 0.2
        c = np.where((lum < 0.5)[..., None], dark + (base - dark) * (np.clip(lum, 0, 1) * 2)[..., None], base + (light - base) * (np.clip(lum - 0.5, 0, 1) * 2)[..., None])
        crack = edge < 0.32 * k
        c = np.where(crack[..., None], dark * 0.75, c)
        c = np.where(((edge >= 0.32 * k) & (edge < 0.9 * k))[..., None], c * 0.9, c)
        m = fbm(rng, w, h, 3, 3)
        grass = (m + (0.18 if face == "up" else 0.0) - (0.1 if face == "down" else 0.0) + (1 - v) * 0.1 > 1.0 - moss * 0.62) & ~crack
        c = np.where(grass[..., None], mossc * (0.8 + 0.4 * n)[..., None], c)
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = c
        img[..., 3] = 255
        g = np.zeros((h, w, 4), np.float32)
        glowc = crack & (hash2(np.floor(x / (3 * k)), np.floor(y / (3 * k)), seed + 3) > 0.78)
        add_glow(g, rgb(crack_glow), glowc.astype(np.float32) * glow_amount)
        return img, g
    return FnMat(fn, "stone")


def crystal_amber():
    return Mat("crystal", "FFC050", dark="B86A10", light="FFF4C0", glow="FFC860", glow_amount=0.95, cells=4, alpha=235, seed=71)


def hex_wing_mat(side, W, H, ax, bx, wmax, lead, cell, salt, hue=(0.26, 0.66), alpha_cell=(46, 40)):
    """A honeycomb wing plate (the wing of the Sylph at body size): hexagon cells, thin borders, a thin-film colour over the wing. `side` +1 = the model's left wing."""
    def fn(face, w, h, tag):
        img, g = blank(w, h)
        if face not in ("north", "south"):
            return img, g
        u, v, x, y = grid(w, h)
        if face == "south":
            u = 1 - u
            x = w - x
        o = u if side > 0 else 1 - u
        k = w / W
        px, py = o * W, v * H
        ax_, ay_, bx_, by_ = ax[0] * W, ax[1] * H, bx[0] * W, bx[1] * H
        dx, dy = bx_ - ax_, by_ - ay_
        L = math.hypot(dx, dy)
        dx, dy = dx / L, dy / L
        t = ((px - ax_) * dx + (py - ay_) * dy) / L
        q = (px - ax_) * (-dy) + (py - ay_) * dx
        tt = np.clip(t, 0, 1)
        prof = np.clip(np.sin(np.pi * tt ** 0.72), 0, 1) ** 0.85
        hw = wmax * prof * np.where(q < 0, lead, 1.0)
        dist = np.where((t > -0.02) & (t < 1.02), np.abs(q) - hw, 9.0) - 0.15 * np.sin(t * 20 + salt) * prof
        alpha = cover(dist * k, soft=1.1)
        edge, cq, cr = hexcells(x + salt * 5.3, y + salt * 2.1, cell * k)
        line = np.clip(1.0 - np.maximum(edge - 0.10 * k, 0) / (0.30 * k), 0, 1)
        hc = hash2(cq, cr, salt)
        ph = 0.55 * o + 0.30 * (1 - v) + 0.30 * hc + salt * 0.11
        lo, hi = hue
        tint = film(ph, sat=0.26, val=1.0, lo=lo, hi=hi)
        tint_e = film(ph + 0.12, sat=0.50, val=1.0, lo=lo, hi=hi)
        wall = line > 0.45
        img[..., :3] = np.where(wall[..., None], tint_e, tint)
        img[..., 3] = np.where(wall, 220, alpha_cell[0] + alpha_cell[1] * hash2(cq, cr, salt + 3)) * alpha
        rim = (dist > -0.34) & (dist < 0.06)
        img[..., :3] = np.where(rim[..., None], film(ph + 0.3, sat=0.5, val=1.0, lo=lo, hi=hi), img[..., :3])
        img[..., 3] = np.where(rim, 244 * alpha, img[..., 3])
        ga = np.where(wall, 0.85, 0.08 + 0.14 * hash2(cq, cr, salt + 9)) * alpha
        ga = np.where(rim, 1.0 * alpha, ga)
        g[..., :3] = film(ph + 0.1, sat=0.5, val=0.95, lo=lo, hi=hi) * ga[..., None]
        g[..., 3] = np.where(ga > 0.04, 255, 0)
        return img, g
    return FnMat(fn, "hexwing")


# ====================================================================================================================== helpers
def lr(side):
    return "l" if side else "r"


def fan(p, a, side, P, defs, mat, prefix, yaw, amp, clip, length, thick=0.5, parent="w", phase=0.0):
    """Feathers fanned from P on the bone `parent`: defs = [(roll degrees, length, width)], each a flat cube along the outward axis; each beats a little with a lag."""
    sg = 1.0 if side else -1.0
    for i, (rz, L, w) in enumerate(defs):
        b = "%s%d" % (prefix, i)
        p.bone(b, P, parent=parent, rotation=(0, sg * (yaw + i * 3), -sg * rz))
        o = (P[0] - L, P[1] - w / 2, P[2]) if side == 0 else (P[0], P[1] - w / 2, P[2])
        p.box(b, o, (L, w, thick), mat)
        sine_rot(a, clip, b, length, 2, amp * 0.5 * sg, 0.22 + i * 0.05 + phase)


def wing_arm(p, side, P, length, mat, tilt=36.0, bone="w"):
    sg = 1.0 if side else -1.0
    p.box(bone, (P[0] - (length if side == 0 else 0.0), P[1] - 0.6, P[2] - 0.2), (length, 1.2, 1.0), mat, rot=(0, 0, sg * tilt), pivot=P)


def ring_of(p, bone, cx, cy, cz, radius, n, size, mat, y_jitter=0.0):
    """A ring of small cubes round a vertical axis (axis aligned: no cube is rotated, so it can never be drawn wrong)."""
    for i in range(n):
        a = i * TAU / n
        p.box(bone, (cx + radius * math.cos(a) - size[0] / 2, cy + y_jitter * math.sin(a * 3) - size[1] / 2, cz + radius * math.sin(a) - size[2] / 2), size, mat, d=3)


def made(m, out):
    out.append(m)
    return m


# ====================================================================================================================== WIND
def wind_wing(side, out):
    n = "wind_wing_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=4)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    sg = 1.0 if side else -1.0
    p.bone("wings", (0, 20.0, 2.6), parent="root")
    for kind, (W, H, hy, yaw, ax, bx, wmax, lead, cell) in {
            "u": (24.0, 26.0, 20.4, 16.0, (0.03, 0.90), (0.97, 0.08), 5.4, 0.72, 2.0),
            "d": (16.0, 15.0, 19.4, 22.0, (0.03, 0.14), (0.97, 0.90), 3.8, 0.9, 1.7)}.items():
        name = "w" + kind
        p.bone(name, (sg * 1.2, hy, 2.7), parent="wings", rotation=(0, sg * -yaw, 0))
        y0 = hy - H * (1 - ax[1])
        ox = sg * 1.2 - W * ax[0] if side else -1.2 - W * (1 - ax[0])
        p.box(name, (ox, y0, 2.7), (W, H, 0.5), hex_wing_mat(sg, W, H, ax, bx, wmax, lead, cell, 3 + (kind == "d") * 5 + (side == 0) * 2), d=4)
        base = 0.0
        frames = {}
        for q in range(9):
            t = 2.4 * q / 8
            frames[t] = (0, sg * -13.0 * (1.0 if kind == "u" else 0.8) * math.sin(TAU * t / 2.4 * 2), 0)
        a.rot("idle", name, frames)
    p.save(a)
    return made(p, out)


def wind_crown(part, out):
    """part 'l' / 'r' = a half crown (the arc over that side of the head, prongs and a thin band), 'c' = the centre prongs (front jewel and back leaf) that join the halves."""
    n = "wind_crown_" + part
    p = Piece(KEY, "arm_" + n, density=4)
    a = Anim()
    a.clip("idle", length=6.0, loop=True)
    p.bone("halo", (0, 33.0, 0), parent="root")
    R = 5.7
    if part in ("l", "r"):
        sg = 1.0 if part == "l" else -1.0
        for i, phi in enumerate((18, 50, 84, 118, 152)):
            ang = math.radians(phi)
            cx, cz = sg * R * math.sin(ang), -R * math.cos(ang)
            h = 6.2 if i % 2 == 0 else 4.4
            stepped_spike(p, "halo", (cx, 32.6, cz), "+y", h, 1.7, W_LEAF if i % 2 else W_PLATE, steps=2, thick=1.1)
        for i in range(8):
            ang = math.radians(12 + i * 22.0)
            p.box("halo", (sg * R * math.sin(ang) - 1.0, 32.2, -R * math.cos(ang) - 1.0), (2.0, 0.8, 2.0), W_GOLD, d=3)
    else:
        stepped_spike(p, "halo", (0, 32.6, -R), "+y", 9.0, 2.0, W_LEAF, steps=4, thick=1.2)                  # the tall front leaf
        p.box("halo", (-0.8, 38.6, -R - 0.4), (1.6, 1.6, 0.9), W_LEAF, rot=(0, 0, 45), pivot=(0, 39.4, -R))     # its jewel
        stepped_spike(p, "halo", (0, 32.6, R), "+y", 6.4, 1.8, W_PLATE, steps=3, thick=1.1)                    # the back leaf
        for i in range(2):                                                                                      # the band between the halves at the front and the back
            p.box("halo", (-1.0, 32.2, -R - 1.0 + i * 2 * R), (2.0, 0.8, 2.0), W_GOLD, d=3)
    a.rot("idle", "halo", {0.0: (0, 0, 0), 3.0: (0, 0, 0), 6.0: (0, 0, 0)})
    a.pos("idle", "halo", {0.0: (0, 0, 0), 1.5: (0, 0.5, 0), 3.0: (0, 0, 0), 4.5: (0, 0.5, 0), 6.0: (0, 0, 0)})
    p.save(a)
    return made(p, out)


def wind_ring(side, out):
    """A halo-like ring round the wrist: a wide bead ring and a thin inner one, a few leaves riding it; the ring turns."""
    n = "wind_ring_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=4)
    a = Anim()
    a.clip("idle", length=4.0, loop=True)
    cx = 6.0 if side else -6.0
    p.bone("ring", (cx, 14.4, 0), parent="root")
    ring_of(p, "ring", cx, 14.4, 0.0, 4.6, 10, (2.8, 0.7, 2.8), W_GOLD)
    ring_of(p, "ring", cx, 14.4, 0.0, 3.5, 6, (1.8, 0.5, 1.8), W_PLATE)
    for i in range(3):
        ang = i * TAU / 3
        p.box("ring", (cx + 5.2 * math.cos(ang) - 0.5, 14.4 - 0.2, 5.2 * math.sin(ang) - 0.5), (1.0, 2.6, 1.0), W_LEAF)
    spin_y(a, "idle", "ring", 4.0, turns=1, steps=8)
    p.save(a)
    return made(p, out)


def wind_mantle(side, out):
    """Chest half-plate, a back half-plate and a shoulder fan of leaves (one half of the body's armour)."""
    n = "wind_mantle_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.4, 13.0, -2.95), (4.4, 10.4, 0.95), W_PLATE)                                      # the half breastplate
    mbox(p, "root", side, (-4.3, 11.6, -2.8), (4.3, 1.6, 0.8), W_GOLD)
    mbox(p, "root", side, (-4.4, 13.0, 2.0), (4.4, 10.4, 0.95), W_GLASS)                                         # the half back plate
    mbox(p, "root", side, (-9.6, 21.8, -3.4), (5.6, 2.8, 6.8), W_GLASS)                                          # the shoulder cap
    bx = -7.0 if side == 0 else 7.0
    for i, (z, hh, rz) in enumerate(((-2.4, 4.6, 12), (0.0, 6.4, 0), (2.4, 4.6, -12))):                          # a fan of leaves rising from the shoulder
        mbox(p, "root", side, (-7.7, 23.6, z - 0.6), (1.4, hh, 1.2), W_LEAF, rot=(0, 0, 10 + i * 3), pivot=(-7.0, 23.6, z))
    mbox(p, "root", side, (-0.6, 22.0, -3.3), (0.9, 2.2, 0.6), W_GOLD)                                           # the clasp at the collarbone
    p.save()
    return made(p, out)


def wind_sleeve(side, out):
    n = "wind_sleeve_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    mbox(p, "root", side, (-8.5, 12.2, -2.5), (5.0, 11.4, 5.0), W_GLASS)                                         # a sleeve of wind-mana, translucent
    mbox(p, "root", side, (-8.7, 17.6, -2.7), (5.4, 1.0, 5.4), W_GOLD)                                           # the elbow band
    for i, (y, ln) in enumerate(((19.5, 3.6), (16.2, 4.4), (13.4, 3.4))):                                         # leaf fins trailing back from the outer arm
        bx = -8.5 if side == 0 else 8.5
        p.bone("f%d" % i, (bx, y, 2.0), parent="root")
        stepped_spike(p, "f%d" % i, (bx, y, 2.2), "+z", ln, 1.6, W_LEAF, steps=3, thick=0.7)
        sine_rot(a, "idle", "f%d" % i, 2.0, 1, 10.0 * (1 if side else -1), 0.12 * i)
    p.save(a)
    return made(p, out)


def wind_greave(side, out):
    n = "wind_greave_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    mbox(p, "root", side, (-4.4, 0.4, -2.45), (4.8, 10.2, 4.9), W_GLASS)                                         # the leg in wind-mana
    mbox(p, "root", side, (-4.55, 5.8, -2.6), (5.1, 1.0, 5.2), W_GOLD)
    for i, sgn in enumerate((-1, 1)):                                                                            # two small leaf-wings at the ankle
        bx = -2.0 + sgn * 3.2 if side == 0 else 2.0 + sgn * 3.2
        p.bone("a%d" % i, (bx, 2.2, 1.5), parent="root")
        stepped_spike(p, "a%d" % i, (bx, 2.2, 2.0), "+z", 3.6, 1.8, W_LEAF, steps=3, thick=0.7)
        sine_rot(a, "idle", "a%d" % i, 2.0, 0, 12.0, 0.2 * i)
    p.save(a)
    return made(p, out)


# ====================================================================================================================== FIRE
def fire_arm(side, out):
    n = "fire_arm_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.65, 17.2, -2.65), (5.3, 6.4, 5.3), F_SCALE)                                        # upper arm, scaled
    mbox(p, "root", side, (-8.55, 12.0, -2.55), (5.1, 5.6, 5.1), F_SCALE2)                                       # forearm
    mbox(p, "root", side, (-8.8, 16.8, -2.8), (5.6, 1.2, 5.6), F_GOLD)                                           # the elbow ring
    bx = -8.7 if side == 0 else 8.7
    d = "-x" if side == 0 else "+x"
    for y, ln in ((20.6, 3.8), (17.4, 4.6), (14.2, 3.6)):                                                        # a ridge of horn spikes down the outer arm
        stepped_spike(p, "root", (bx, y, 0.0), d, ln, 2.2, F_HORN, steps=3, thick=1.5)
    stepped_spike(p, "root", (bx, 18.6, 0.0), "+y", 3.4, 2.0, F_HORN, steps=2, thick=1.6)                        # an elbow horn
    p.save()
    return made(p, out)


def fire_claws(side, out):
    n = "fire_claws_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.55, 10.6, -2.65), (5.1, 2.0, 5.3), F_SCALE2)                                       # the knuckles
    mbox(p, "root", side, (-8.7, 10.4, -3.0), (5.4, 1.0, 1.0), F_GOLD)
    for k in range(4):                                                                                           # four long talons, bone coloured with a hot tip
        cx = (-8.1 + k * 1.2) if side == 0 else (6.9 - k * 1.2)
        stepped_spike(p, "root", (cx + 0.5, 10.6, -2.2 + 0.2 * (k % 2)), "-y", 5.2, 1.1, F_HORN, steps=4, thick=0.9)
        p.box("root", (cx, 5.2, -2.5 + 0.2 * (k % 2)), (1.0, 1.0, 0.9), F_FLAME, d=3)
    p.save()
    return made(p, out)


def fire_pauldron(side, out):
    n = "fire_pauldron_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-10.2, 21.4, -3.5), (6.2, 3.6, 7.0), F_SCALE)
    mbox(p, "root", side, (-9.8, 24.4, -3.1), (5.4, 1.2, 6.2), F_GOLD)
    mbox(p, "root", side, (-10.6, 19.2, -3.0), (1.0, 2.6, 6.0), F_SCALE2)
    bx = -8.2 if side == 0 else 8.2
    for z, ln in ((-2.2, 4.2), (0.2, 5.6), (2.6, 4.0)):                                                          # three horns standing from the shoulder
        stepped_spike(p, "root", (bx, 25.4, z), "+y", ln, 1.9, F_HORN, steps=3, thick=1.5)
    p.save()
    return made(p, out)


def fire_wing(side, big, out):
    n = ("fire_wing_" if big else "fire_wingbud_") + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=1.2, loop=True)
    sg = 1.0 if side else -1.0
    P = (sg * 2.6, 21.6, 2.7)
    p.bone("w", P, parent="root")
    sine_rot(a, "idle", "w", 1.2, 2, -10.0 * sg, 0.0)
    sine_rot(a, "idle", "w", 1.2, 1, 6.0 * sg, 0.15)
    if big:
        fan(p, a, side, P, [(-50, 15.0, 3.6), (-34, 19.0, 4.0), (-18, 23.0, 4.2), (-3, 25.0, 4.4), (12, 22.0, 4.2), (27, 18.0, 3.8), (42, 13.0, 3.2)], F_FEATHER, "f", 22, 9.0, "idle", 1.2)
        fan(p, a, side, (P[0], P[1], P[2] + 0.4), [(-42, 11.0, 3.0), (-22, 14.0, 3.2), (-4, 16.0, 3.4), (16, 13.0, 3.0)], F_FLAME, "g", 22, 11.0, "idle", 1.2, thick=0.4, phase=0.3)
        wing_arm(p, side, P, 14.0, F_HORN, tilt=40.0)
        p.box("w", (P[0] - (3.0 if side == 0 else 0.0), P[1] - 1.0, P[2] - 0.6), (3.0, 2.0, 1.8), F_SCALE)
    else:
        fan(p, a, side, P, [(-42, 11.0, 3.0), (-22, 14.0, 3.4), (-2, 15.0, 3.6), (18, 13.0, 3.2), (36, 10.0, 2.8)], F_FEATHER, "f", 22, 8.0, "idle", 1.2)
        wing_arm(p, side, P, 9.0, F_HORN, tilt=38.0)
    p.save(a)
    return made(p, out)


def fire_greave(side, out):
    n = "fire_greave_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.6, 6.6, -2.7), (5.2, 5.2, 5.4), F_SCALE)                                           # thigh, scaled
    mbox(p, "root", side, (-4.5, 1.6, -2.6), (5.0, 5.0, 5.2), F_SCALE2)                                          # shin
    mbox(p, "root", side, (-4.7, 5.4, -3.2), (5.4, 1.6, 1.0), F_GOLD)                                            # the knee
    sx = -2.0 if side == 0 else 2.0
    stepped_spike(p, "root", (sx, 6.4, -3.1), "-z", 3.2, 2.2, F_HORN, steps=3, thick=1.6)                        # a knee horn
    mbox(p, "root", side, (-4.7, 0.0, -3.4), (5.4, 1.8, 6.8), F_SCALE)                                           # the foot
    for k in range(3):                                                                                           # toe claws
        cx = (-4.2 + k * 1.7) if side == 0 else (2.6 - k * 1.7)
        stepped_spike(p, "root", (cx + 0.7, 0.4, -3.4), "-z", 2.4, 1.3, F_HORN, steps=2, thick=1.0)
    p.save()
    return made(p, out)


def fire_chest(out):
    p = Piece(KEY, "arm_fire_chest", density=3)
    p.box("root", (-4.6, 16.4, -2.8), (9.2, 7.4, 1.2), F_SCALE)
    p.box("root", (-4.5, 14.6, -2.7), (9.0, 1.8, 1.0), F_GOLD)
    p.box("root", (-4.4, 12.6, -2.6), (8.8, 1.8, 0.9), F_SCALE2)
    p.box("root", (-1.5, 19.0, -3.3), (3.0, 3.0, 0.7), F_FLAME, rot=(0, 0, 45), pivot=(0, 20.5, -3.1))             # the heart of the dragon, burning
    p.box("root", (-4.8, 23.2, -2.9), (9.6, 1.9, 5.8), F_GOLD)
    for sx in (-1, 1):                                                                                           # two horns at the collar
        stepped_spike(p, "root", (sx * 3.4, 25.0, -1.0), "+y", 4.0, 1.9, F_HORN, steps=3, thick=1.4)
    p.box("root", (-4.7, 12.2, 2.05), (9.4, 11.2, 0.95), F_SCALE2)                                               # the back
    for i in range(5):                                                                                           # the spine ridge
        stepped_spike(p, "root", (0, 12.6 + i * 2.2, 3.0), "+z", 2.6, 1.6, F_HORN, steps=2, thick=1.0)
    p.save()
    return p


# ====================================================================================================================== EARTH
def earth_chest(out):
    p = Piece(KEY, "arm_earth_chest", density=3)
    S1, S2 = stone(1), stone(2, moss=0.5)
    p.box("root", (-4.8, 12.4, -3.0), (9.6, 11.2, 1.3), S1)
    p.box("root", (-4.9, 20.4, -3.2), (9.8, 3.4, 1.6), S2)                                                        # the breast ridge with moss
    p.box("root", (-3.6, 13.0, -3.5), (7.2, 5.0, 0.7), stone(3), rot=(0, 0, 0))
    p.box("root", (-4.8, 12.4, 1.9), (9.6, 11.2, 1.3), S2)
    p.box("root", (-1.2, 17.8, -3.9), (2.4, 2.4, 0.9), crystal_amber(), rot=(0, 0, 45), pivot=(0, 19.0, -3.5))      # an amber core in the chest
    for sx in (-1, 1):
        p.box("root", (sx * 4.6 - 0.6, 12.8, -1.8), (1.2, 8.6, 3.6), stone(4 + sx))
    p.save()
    return made(p, out)


def earth_pauldron(side, out):
    n = "earth_pauldron_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-11.2, 20.4, -3.9), (7.2, 5.2, 7.8), stone(10 + side, moss=0.55), rot=(0, 0, 10), pivot=(-8.0, 22.0, 0))
    mbox(p, "root", side, (-10.2, 24.6, -2.0), (4.2, 2.0, 4.0), stone(12 + side, moss=0.7))
    mbox(p, "root", side, (-9.6, 18.8, -2.6), (3.6, 2.4, 3.4), stone(14 + side))
    bx = -8.8 if side == 0 else 8.8
    stepped_spike(p, "root", (bx, 26.4, -0.6), "+y", 3.6, 1.8, crystal_amber(), steps=3, thick=1.5)
    p.save()
    return made(p, out)


def earth_vambrace(side, out):
    n = "earth_vambrace_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.9, 12.6, -2.9), (5.8, 6.2, 5.8), stone(20 + side, moss=0.45))
    mbox(p, "root", side, (-8.7, 18.6, -2.7), (5.4, 3.8, 5.4), stone(22 + side, moss=0.6))
    bx = -9.0 if side == 0 else 9.0
    stepped_spike(p, "root", (bx, 15.6, 0.4), "-x" if side == 0 else "+x", 3.0, 2.4, stone(24 + side), steps=2, thick=1.6)
    p.save()
    return made(p, out)


def earth_gauntlet(side, out):
    n = "earth_gauntlet_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-9.2, 8.0, -3.5), (6.4, 5.8, 7.0), stone(30 + side, moss=0.3))                         # a stone fist, bigger than the hand
    for k in range(4):
        cx = (-8.9 + k * 1.55) if side == 0 else (7.4 - k * 1.55)
        p.box("root", (cx, 7.3, -4.0), (1.4, 1.4, 1.2), stone(32 + k + side * 5), d=3)
    mbox(p, "root", side, (-9.0, 13.4, -3.3), (6.0, 1.6, 6.6), stone(34 + side, moss=0.5))
    p.save()
    return made(p, out)


def earth_greave(side, out):
    n = "earth_greave_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.9, 6.0, -3.0), (5.8, 6.0, 6.0), stone(40 + side, moss=0.5))
    mbox(p, "root", side, (-4.8, 1.6, -2.9), (5.6, 4.6, 5.8), stone(42 + side))
    mbox(p, "root", side, (-5.0, 0.0, -3.6), (5.8, 2.2, 7.6), stone(44 + side, moss=0.6))
    mbox(p, "root", side, (-4.2, 5.0, -3.9), (3.6, 2.8, 1.4), stone(46 + side))                                  # the knee rock
    p.save()
    return made(p, out)


def earth_helm(out):
    p = Piece(KEY, "arm_earth_helm", density=3)
    p.box("root", (-4.8, 28.0, -4.9), (9.6, 2.6, 1.2), stone(50))                                                  # the brow band
    p.box("root", (-4.8, 28.0, 3.7), (9.6, 2.6, 1.2), stone(51))
    p.box("root", (-4.8, 28.0, -3.7), (1.2, 2.6, 7.4), stone(52))
    p.box("root", (3.6, 28.0, -3.7), (1.2, 2.6, 7.4), stone(53))
    for sx in (-1, 1):
        p.box("root", (sx * 4.9 - 0.5, 24.6, -2.4), (1.0, 4.4, 4.8), stone(54 + sx, moss=0.5))                  # cheek plates
    for i, (x, hh) in enumerate(((-2.6, 4.2), (0.0, 6.4), (2.6, 4.2))):                                            # a crown of amber crystals
        stepped_spike(p, "root", (x, 30.6, -4.0), "+y", hh, 1.8, crystal_amber(), steps=3, thick=1.4)
    p.box("root", (-3.4, 30.4, -1.8), (6.8, 1.8, 3.6), stone(56, moss=0.8))                                         # moss on the crown
    p.save()
    return made(p, out)


def earth_back(out):
    p = Piece(KEY, "arm_earth_back", density=3)
    p.box("root", (-3.4, 12.6, 2.4), (6.8, 11.0, 3.0), stone(60, moss=0.5))                                         # a slab on the back
    p.box("root", (-4.4, 21.0, 2.0), (8.8, 3.6, 3.2), stone(61, moss=0.7))                                          # the hump
    for i, (x, y, hh) in enumerate(((-2.4, 24.6, 3.6), (0.2, 25.0, 5.0), (2.6, 24.4, 3.4), (-1.0, 19.0, 3.0), (1.6, 18.0, 3.4))):
        stepped_spike(p, "root", (x, y, 4.2), "+y" if y > 20 else "+z", hh, 1.8, stone(62 + i), steps=2, thick=1.6)
    p.save()
    return made(p, out)


def earth_orbit(out):
    p = Piece(KEY, "arm_earth_orbit", density=3)
    a = Anim()
    a.clip("idle", length=9.0, loop=True)
    for i in range(6):
        b = "r%d" % i
        p.bone(b, (0, 14.0, 0), parent="root")
        ang = i * TAU / 6
        r = 10.0 + 1.6 * (i % 2)
        y = 10.0 + i * 3.2
        s = 2.4 + (i % 3) * 0.8
        p.box(b, (r * math.cos(ang) - s / 2, y - s / 2, r * math.sin(ang) - s / 2), (s, s, s), stone(70 + i, moss=0.5), rot=(20 * i, 30 * i, 10), pivot=(r * math.cos(ang), y, r * math.sin(ang)))
        a.rot("idle", b, {9.0 * j / 8: (0, 360.0 * j / 8 * (1 if i % 2 == 0 else -1), 0) for j in range(9)})
        a.pos("idle", b, {0.0: (0, 0, 0), 2.25: (0, 1.4 * (1 if i % 2 else -1), 0), 4.5: (0, 0, 0), 6.75: (0, -1.4 * (1 if i % 2 else -1), 0), 9.0: (0, 0, 0)})
    p.save(a)
    return made(p, out)


# ====================================================================================================================== SAINT VALKYRIE (water)
def saint_wing(side, out):
    n = "saint_wing_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    sg = 1.0 if side else -1.0
    P = (sg * 2.4, 22.0, 2.8)
    p.bone("w", P, parent="root")
    sine_rot(a, "idle", "w", 3.0, 2, -8.0 * sg, 0.0)
    sine_rot(a, "idle", "w", 3.0, 1, 5.0 * sg, 0.18)
    fan(p, a, side, P, [(-62, 18.0, 4.2), (-46, 23.0, 4.6), (-30, 28.0, 5.0), (-14, 31.0, 5.2), (2, 30.0, 5.2), (18, 26.0, 4.8), (34, 20.0, 4.2)], S_FEATHER, "p", 24, 7.0, "idle", 3.0)         # the long primaries
    fan(p, a, side, (P[0], P[1], P[2] + 0.5), [(-54, 12.0, 3.4), (-36, 16.0, 3.8), (-18, 19.0, 4.0), (0, 18.0, 4.0), (20, 14.0, 3.6)], S_FEATHER2, "s", 24, 8.0, "idle", 3.0, thick=0.45, phase=0.25)  # the secondaries
    fan(p, a, side, (P[0], P[1], P[2] + 0.9), [(-48, 7.0, 2.6), (-26, 9.0, 2.8), (-4, 9.0, 2.8), (18, 7.0, 2.6)], S_FEATHER, "c", 24, 9.0, "idle", 3.0, thick=0.4, phase=0.5)                     # the coverts
    wing_arm(p, side, P, 13.0, S_GOLD, tilt=42.0)                                                                   # the wing's arm, gold
    p.box("w", (P[0] - (3.4 if side == 0 else 0.0), P[1] - 1.1, P[2] - 0.7), (3.4, 2.2, 2.0), S_GOLD_P)
    p.save(a)
    return made(p, out)


def saint_halo(out):
    p = Piece(KEY, "arm_saint_halo", density=3)
    a = Anim()
    a.clip("idle", length=10.0, loop=True)
    p.bone("spin", (0, 36.2, 0), parent="root")
    ring_of(p, "spin", 0.0, 36.2, 0.0, 7.0, 20, (2.8, 0.9, 2.8), S_GOLD)                                           # the gold ring
    ring_of(p, "spin", 0.0, 36.2, 0.0, 5.6, 14, (1.7, 0.6, 1.7), S_WATER)                                          # the water-blue inner ring
    for i in range(8):                                                                                              # small feathers standing on the ring
        ang = i * TAU / 8
        p.box("spin", (7.0 * math.cos(ang) - 0.6, 36.6, 7.0 * math.sin(ang) - 0.6), (1.2, 3.4, 1.2), S_FEATHER)
    spin_y(a, "idle", "spin", 10.0, turns=1, steps=8)
    a.pos("idle", "spin", {0.0: (0, 0, 0), 2.5: (0, 0.6, 0), 5.0: (0, 0, 0), 7.5: (0, 0.6, 0), 10.0: (0, 0, 0)})
    p.save(a)
    return made(p, out)


def saint_sleeve(side, out):
    n = "saint_sleeve_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    sg = 1.0 if side else -1.0
    P = (sg * 6.0, 13.6, 2.2)
    p.bone("w", P, parent="root")
    sine_rot(a, "idle", "w", 2.4, 0, 6.0, 0.0)
    # wing-like feathers streaming back from the forearm (the arm is vertical: the feathers hang along it, flaring)
    for i, (rx, L) in enumerate(((10, 7.0), (22, 8.6), (34, 8.0), (46, 6.2))):
        b = "q%d" % i
        p.bone(b, P, parent="w", rotation=(rx, sg * (-6 + 4 * i), 0))
        p.box(b, (P[0] - 1.1, P[1] - L, P[2]), (2.2, L, 0.5), S_FEATHER if i % 2 == 0 else S_FEATHER2)
        sine_rot(a, "idle", b, 2.4, 0, 5.0, 0.12 * i)
    mbox(p, "root", side, (-8.7, 17.0, -2.7), (5.4, 1.0, 5.4), S_GOLD)                                              # the gold cuff
    p.save(a)
    return made(p, out)


def saint_sash(out):
    p = Piece(KEY, "arm_saint_sash", density=3)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    p.box("root", (-4.7, 11.8, -2.65), (9.4, 1.5, 5.3), S_GOLD_P)                                                   # the gold belt
    p.box("root", (-1.2, 11.6, -3.3), (2.4, 2.2, 0.8), S_WATER, rot=(0, 0, 45), pivot=(0, 12.6, -3.0))              # a water jewel
    for i, (x, z, rx) in enumerate(((-3.2, -2.9, -8), (0.0, -3.2, -6), (3.2, -2.9, -8), (-4.8, 0.0, 0), (4.8, 0.0, 0), (-2.4, 3.0, 8), (2.4, 3.0, 8))):
        b = "t%d" % i
        p.bone(b, (x, 11.6, z), parent="root")
        if abs(x) >= 4.8:
            p.box(b, (x - 0.25, 5.4, z - 1.1), (0.5, 6.2, 2.2), S_FEATHER2)
        else:
            p.box(b, (x - 1.1, 5.4, z - 0.25), (2.2, 6.2, 0.5), S_FEATHER if i % 2 == 0 else S_FEATHER2)
        sine_rot(a, "idle", b, 2.4, 0, 5.0, 0.13 * i, offset=(rx, 0, 0))
    p.save(a)
    return made(p, out)


# ====================================================================================================================== the body overlays
def shells():
    """Each element's translucent skin (drawn first, scaled 3 percent up) and haze (additive, last): 64 x 64 player-skin layout textures under textures/aura/."""
    shell_texture("spirit_wind_shell", Mat("wisp", "BDF4D8", dark="5CBE98", light="F2FFF8", glow="98FFD0", glow_amount=0.9, ragged=1, alpha=150, seed=81))
    shell_texture("spirit_wind_haze", Mat("wisp", "D8FFEC", dark="7CDCB0", light="FFFFFF", glow="B8FFE0", glow_amount=1.0, ragged=1, alpha=255, seed=82))
    shell_texture("spirit_fire_shell", Mat("crystal", "3A1210", dark="170605", light="FF9A38", glow="FF6A1C", glow_amount=1.0, cells=14, seamonly=True, alpha=230, seed=83))
    shell_texture("spirit_fire_haze", Mat("flame", "FF8A26", dark="D0300C", light="FFE680", glow="FFB040", glow_amount=1.0, alpha=255, seed=84))
    shell_texture("spirit_water_shell", Mat("water", "6EBEF4", dark="2C78D0", light="F2FCFF", glow="A8F4FF", glow_amount=0.8, alpha=140, seed=85))
    shell_texture("spirit_water_haze", Mat("water", "BFEFFF", dark="5EB8F2", light="FFFFFF", glow="E8FCFF", glow_amount=1.0, alpha=255, seed=86))
    shell_texture("spirit_earth_shell", Mat("crystal", "3E352C", dark="1A1612", light="B8A484", glow="FFB040", glow_amount=0.9, cells=12, seamonly=True, alpha=225, seed=87))
    shell_texture("spirit_earth_haze", Mat("wisp", "FFD890", dark="C88A30", light="FFF4D0", glow="FFC860", glow_amount=0.9, ragged=1, alpha=255, seed=88))


def main():
    out = []
    shells()
    for s in (0, 1):
        wind_wing(s, out)
        wind_ring(s, out)
        wind_mantle(s, out)
        wind_sleeve(s, out)
        wind_greave(s, out)
        fire_arm(s, out)
        fire_claws(s, out)
        fire_pauldron(s, out)
        fire_wing(s, False, out)
        fire_wing(s, True, out)
        fire_greave(s, out)
        earth_pauldron(s, out)
        earth_vambrace(s, out)
        earth_gauntlet(s, out)
        earth_greave(s, out)
        saint_wing(s, out)
        saint_sleeve(s, out)
    for part in ("l", "r", "c"):
        wind_crown(part, out)
    fire_chest(out)
    earth_chest(out)
    earth_helm(out)
    earth_back(out)
    earth_orbit(out)
    saint_halo(out)
    saint_sash(out)
    total = 0
    for m in out:
        print("%-36s cubes %3d" % (m.id, m.cube_count()))
        total += m.cube_count()
    print("models", len(out), "total cubes", total)


if __name__ == "__main__":
    main()
