"""Skill icon redraw v2: real symbols for the icons that were placeholders.

    python tools/skills/gen_icons_v2.py                 # redraw every icon below (live 32 px icons + the classic copies)
    python tools/skills/gen_icons_v2.py --only key      # only keys containing 'key'
    python tools/skills/gen_icons_v2.py --sheet out.png # also write a labelled contact sheet (not shipped)
    python tools/skills/gen_icons_v2.py --doc           # before/after contact sheets for docs/skill_icons_v2/ (each < 200 KB)

Why: the audit (docs/skill_icons_v2.md) found two groups of grimoire icons that were never given a symbol of their own:
  * SET A, 29 magics of the 0.53 expansion (barrier ... legion). Their tools/icons/g_<key>.py are marked STUB and all draw the same small
    diamond, so 29 x 3 icons (grimoire, _buff, _ultimate) plus the 29 sym_ layers looked alike.
  * SET B, 30 "wiki" magics (air ... wing, tools/gen_grimoire_icons.mjs). Six motifs ("stone", "shield", "strand" ...) are shared by up to six
    magics, so mud, rock, stone, shakudo, skin and red_ochre are the same letter M in a ring, hair/tongue/tree/vine the same Y.
This script gives each of those 59 magics its own themed glyph and renders it in the style of the existing good icons: it uses the very same
pipeline as tools/icon_forge.py (relief emblem lit from the top-left, aura / ancient-parchment / void background, gem frame), so the palette
family is unchanged. Set B keeps its path (only the active grimoire icon existed) and gets the square gold frame of the other grimoire icons.

Nothing is renamed: every file keeps its path and id. The version that was in the resources before is copied once to
tools/skills/icons_classic/<same path> (never overwritten, not shipped). tools/skills/icons_v2.txt lists every redrawn icon (set|path) for
SkillIconsV2Test. Deterministic: same inputs, same PNGs.
"""
import hashlib
import math
import os
import shutil
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.join(HERE, "..")
sys.path.insert(0, TOOLS)
import gen_skill_icons as G          # noqa: E402
import icon_forge as F               # noqa: E402

U = G.U
TEX = G.TEX
CLASSIC = os.path.join(HERE, "icons_classic")
MANIFEST = os.path.join(HERE, "icons_v2.txt")
DOCS = os.path.join(TOOLS, "..", "docs", "skill_icons_v2")


# --------------------------------------------------------------------------------------------- pen helpers (glyph units, origin = centre)
def W(u):
    return max(1, int(u * U))


def circ(d, x, y, r, f=255):
    d.ellipse(G.box(x - r, y - r, x + r, y + r), fill=f)


def ring(d, x, y, r, w, f=255):
    d.ellipse(G.box(x - r, y - r, x + r, y + r), outline=f, width=W(w))


def poly(d, pts, f=255):
    d.polygon(G.P(pts), fill=f)


def line(d, pts, w, f=255):
    d.line(G.P(pts), fill=f, width=W(w), joint="curve")
    for p in (pts[0], pts[-1]):
        circ(d, p[0], p[1], w / 2, f)


def arc(d, x, y, r, a0, a1, w, f=255):
    d.arc(G.box(x - r, y - r, x + r, y + r), a0, a1, fill=f, width=W(w))


def rect(d, x0, y0, x1, y1, f=255):
    d.rectangle(G.box(x0, y0, x1, y1), fill=f)


def rot(pts, deg, cx=0.0, cy=0.0):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]


def ellp(cx, cy, rx, ry, deg=0.0, n=28):
    pts = [(cx + math.cos(i * math.tau / n) * rx, cy + math.sin(i * math.tau / n) * ry) for i in range(n)]
    return rot(pts, deg, cx, cy) if deg else pts


def taper(d, pts, w0, w1, f=255, n=40):
    """A stroke along a polyline whose width goes from w0 to w1 (hair, horns, claws, feathers)."""
    seg = [(pts[i], pts[i + 1]) for i in range(len(pts) - 1)]
    tot = sum(math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in seg) or 1.0
    run = 0.0
    for a, b in seg:
        ln = math.hypot(b[0] - a[0], b[1] - a[1])
        k = max(2, int(ln * 6))
        for i in range(k + 1):
            t = i / k
            w = w0 + (w1 - w0) * ((run + ln * t) / tot)
            circ(d, a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, w / 2, f)
        run += ln


def curve(fn, t0, t1, n=40):
    return [fn(t0 + (t1 - t0) * i / n) for i in range(n + 1)]


def bez(p0, p1, p2, p3, n=24):
    out = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        out.append((u ** 3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0],
                    u ** 3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1]))
    return out


def sparkle(d, x, y, r, f=255):
    poly(d, [(x, y - r), (x + r * 0.28, y - r * 0.28), (x + r, y), (x + r * 0.28, y + r * 0.28), (x, y + r),
             (x - r * 0.28, y + r * 0.28), (x - r, y), (x - r * 0.28, y - r * 0.28)], f)


# --------------------------------------------------------------------------------------------- SET A glyphs (29)
def g_barrier(d):                                   # refractive glass dome on a base plate
    dome = [(9.8 * math.cos(math.radians(a)), 4.5 - 9.8 * math.sin(math.radians(a))) for a in range(0, 181, 6)]
    poly(d, dome)
    poly(d, [(6.6 * math.cos(math.radians(a)), 4.5 - 6.6 * math.sin(math.radians(a))) for a in range(0, 181, 6)], 150)
    for a in (40, 90, 140):
        line(d, [(0, 4.5), (9.8 * math.cos(math.radians(a)), 4.5 - 9.8 * math.sin(math.radians(a)))], 0.9, 0)
    arc(d, 0, 4.5, 3.2, 180, 360, 0.9, 0)
    rect(d, -10.5, 5.6, 10.5, 8.6)
    line(d, [(-6.4, -1.0), (-3.6, -4.8)], 1.0, 255)


def g_black_oil(d):                                  # a glossy crude-oil drop over a rippling slick
    drop = bez((0, -10.6), (1.0, -6.0), (7.6, -2.0), (6.8, 2.6)) + bez((6.8, 2.6), (6.2, 6.4), (-6.2, 6.4), (-6.8, 2.6))[1:] + bez((-6.8, 2.6), (-7.6, -2.0), (-1.0, -6.0), (0, -10.6))[1:]
    poly(d, drop)
    poly(d, bez((0.6, -3.0), (4.0, -0.6), (4.6, 2.4), (3.0, 4.6)) + bez((3.0, 4.6), (4.6, 1.0), (3.4, -1.0), (0.6, -3.0))[1:], 150)
    arc(d, 0, 1.8, 3.6, 150, 235, 1.2, 0)
    circ(d, -2.6, -0.4, 0.9, 255)
    d.ellipse(G.box(-10.0, 7.2, 10.0, 10.6), fill=255)
    d.ellipse(G.box(-7.4, 7.9, 7.4, 9.9), fill=150)
    d.ellipse(G.box(-3.6, 8.3, 3.6, 9.5), fill=0)
    for (x, y, r) in ((-8.6, -5.0, 0.9), (8.4, -7.2, 0.8)):
        circ(d, x, y, r, 150)


def g_body(d):                                       # double-biceps pose
    circ(d, 0, -8.2, 2.4)
    poly(d, [(-5.0, -5.4), (5.0, -5.4), (3.6, 4.0), (-3.6, 4.0)])
    for s in (-1, 1):
        line(d, [(s * 4.6, -4.4), (s * 8.6, -3.2), (s * 8.8, -8.6)], 3.0)
        circ(d, s * 7.4, -4.9, 2.4, 150)
        circ(d, s * 8.8, -9.2, 1.9)
        line(d, [(s * 0.4, -4.2), (s * 3.2, -2.4)], 0.8, 0)
    line(d, [(0, -4.6), (0, 3.8)], 0.8, 0)
    for y in (-0.6, 1.9):
        line(d, [(-2.6, y), (2.6, y)], 0.7, 0)
    rect(d, -4.6, 4.0, 4.6, 6.8, 150)


def g_briar(d):                                      # a rose on a thorny stem
    stem = bez((0.5, 1.0), (4.0, 4.0), (-5.0, 6.0), (-3.0, 10.4))
    taper(d, stem, 1.8, 1.6)
    for (i, s) in ((5, 1), (11, -1), (17, 1)):
        x, y = stem[i]
        poly(d, [(x - 0.8, y - 1.2), (x + 0.8, y + 1.2), (x + s * 3.6, y)])
    poly(d, rot(ellp(5.6, 5.6, 3.6, 1.7, 0), -30, 5.6, 5.6), 255)
    for k in range(5):
        a = math.radians(-90 + k * 72)
        circ(d, math.cos(a) * 4.6, -3.8 + math.sin(a) * 4.6, 3.0)
    circ(d, 0, -3.8, 4.4)
    arc(d, 0, -3.8, 3.2, 30, 320, 0.9, 0)
    arc(d, 0, -3.8, 1.5, 120, 400, 0.8, 0)
    circ(d, -0.2, -3.8, 0.6, 150)
    for (x, y, r) in ((-8.6, 3.0, 0.8), (9.0, -8.6, 0.8)):
        circ(d, x, y, r, 150)


def g_bronze(d):                                     # hoplon shield with a spear thrust across it
    circ(d, -1.6, 1.6, 7.6)
    ring(d, -1.6, 1.6, 5.6, 0.9, 0)
    circ(d, -1.6, 1.6, 3.0, 150)
    circ(d, -1.6, 1.6, 1.3, 255)
    for i in range(8):
        a = math.radians(i * 45 + 22)
        circ(d, -1.6 + math.cos(a) * 6.6, 1.6 + math.sin(a) * 6.6, 0.55, 0)
    line(d, [(-9.4, 9.6), (7.2, -7.0)], 2.6, 0)
    line(d, [(-9.4, 9.6), (7.2, -7.0)], 1.3)
    poly(d, [(6.4, -9.4), (10.8, -10.8), (9.4, -6.2), (7.6, -5.6), (5.2, -7.0)])
    line(d, [(4.4, -4.4), (7.6, -7.6)], 2.4, 0)
    line(d, [(4.8, -4.8), (7.2, -7.2)], 1.5, 255)


def g_bubble(d):                                     # three soap bubbles
    for (x, y, r) in ((-2.4, 2.6, 6.8), (5.8, -5.8, 3.6), (6.8, 6.2, 2.4)):
        circ(d, x, y, r)
        circ(d, x, y, r - 1.2, 150)
        arc(d, x, y, r - 2.0, 200, 270, max(0.9, r * 0.22), 255)
        circ(d, x + r * 0.38, y + r * 0.34, max(0.45, r * 0.11), 255)


def g_butoh(d):                                      # dancer with shock-rings
    circ(d, -0.6, -8.0, 2.0)
    line(d, [(-0.4, -5.6), (0.4, 1.0)], 2.6)
    line(d, [(-0.4, -4.4), (-5.0, -7.8), (-8.4, -5.2)], 1.7)
    line(d, [(0.4, -4.4), (4.6, -1.6), (8.0, -5.6)], 1.7)
    line(d, [(0.4, 1.0), (-3.4, 5.0), (-6.4, 9.4)], 2.0)
    line(d, [(0.4, 1.0), (4.0, 5.2), (3.0, 9.8)], 2.0)
    arc(d, 0, 0, 10.2, 150, 205, 1.0, 150)
    arc(d, 0, 0, 10.2, -25, 30, 1.0, 150)
    arc(d, 0, 0, 8.6, 300, 330, 0.9, 150)


def g_chain(d):                                      # three interlocked links
    def link(cx, cy, ang):
        pts = []
        for i in range(0, 361, 12):
            a = math.radians(i)
            x, y = math.cos(a) * 2.6, math.sin(a) * 2.6
            y += 2.9 if y > 0 else -2.9
            pts.append((x, y))
        pts = rot(pts, ang)
        pts = [(cx + x, cy + y) for x, y in pts]
        line(d, pts + [pts[0]], 2.1)
    link(-5.6, -5.6, 45)
    link(0.0, 0.0, -45)
    link(5.6, 5.6, 45)


def g_cherry_blossom(d):                             # five-petal sakura with stamens
    for k in range(5):
        pts = rot(ellp(0, -5.6, 3.7, 4.7), k * 72)
        poly(d, pts)
    for k in range(5):
        poly(d, rot([(-1.2, -10.8), (1.2, -10.8), (0, -8.4)], k * 72), 0)
        line(d, rot([(0, -1.6), (0, -5.4)], k * 72), 0.6, 150)
    circ(d, 0, 0, 2.4, 150)
    for k in range(5):
        a = math.radians(-90 + k * 72 + 36)
        circ(d, math.cos(a) * 1.6, math.sin(a) * 1.6, 0.5, 255)
    poly(d, rot(ellp(8.8, 8.6, 1.6, 2.1), 40), 255)


def g_copper(d):                                     # a stack of ingots
    def ingot(cx, cy, w):
        poly(d, [(cx - w / 2, cy + 2.3), (cx + w / 2, cy + 2.3), (cx + w / 2 - 1.5, cy - 2.3), (cx - w / 2 + 1.5, cy - 2.3)])
        poly(d, [(cx - w / 2 + 1.9, cy - 0.2), (cx + w / 2 - 1.9, cy - 0.2), (cx + w / 2 - 2.5, cy - 1.5), (cx - w / 2 + 2.5, cy - 1.5)], 150)
    ingot(-5.4, 6.4, 10.4)
    ingot(5.4, 6.4, 10.4)
    ingot(0, 1.2, 10.4)
    ingot(0, -4.4, 8.4)
    sparkle(d, 8.6, -6.6, 2.0)


def g_corundum(d):                                   # cut ruby
    poly(d, [(-9.4, -3.2), (-5.4, -8.2), (5.4, -8.2), (9.4, -3.2), (0, 9.4)])
    poly(d, [(-4.6, -7.0), (4.6, -7.0), (2.6, -3.2), (-2.6, -3.2)], 150)
    line(d, [(-9.4, -3.2), (9.4, -3.2)], 0.8, 0)
    for (a, b) in (((-5.4, -8.2), (-2.6, -3.2)), ((5.4, -8.2), (2.6, -3.2)), ((-2.6, -3.2), (0, 9.4)), ((2.6, -3.2), (0, 9.4)), ((-9.4, -3.2), (-2.6, -3.2))):
        line(d, [a, b], 0.8, 0)
    sparkle(d, 8.4, -8.4, 2.4)


def g_crystal(d):                                    # cluster of three hexagonal prisms
    def prism(cx, base, tip, hw, ang, shade=150):
        pts = [(cx - hw, base), (cx - hw, tip + hw * 1.9), (cx, tip), (cx + hw, tip + hw * 1.9), (cx + hw, base)]
        poly(d, rot(pts, ang, cx, base))
        poly(d, rot([(cx + hw * 0.05, base), (cx + hw * 0.05, tip + 0.4), (cx + hw, tip + hw * 1.9), (cx + hw, base)], ang, cx, base), shade)
    prism(-5.4, 9.0, -3.0, 2.4, -24)
    prism(5.6, 9.0, -2.4, 2.3, 22)
    prism(0, 9.4, -10.0, 3.0, 0)
    sparkle(d, -8.4, -7.8, 1.8)


def g_curse(d):                                      # skull with a jagged brand
    poly(d, ellp(0, -2.4, 7.4, 7.0))
    poly(d, [(-4.6, 2.6), (4.6, 2.6), (3.8, 8.8), (-3.8, 8.8)])
    for s in (-1, 1):
        poly(d, [(s * 1.4, -2.4), (s * 5.6, -3.6), (s * 5.0, 0.8), (s * 2.0, 1.0)], 0)
    poly(d, [(0, 0.2), (1.2, 2.8), (-1.2, 2.8)], 0)
    for x in (-2.4, 0, 2.4):
        line(d, [(x, 5.2), (x, 8.8)], 0.7, 0)
    line(d, [(2.2, -9.4), (0.6, -7.0), (2.4, -5.6), (0.4, -3.4)], 0.9, 0)
    for (x, y) in ((-9.6, -8.4), (9.6, -8.4), (0, -11.2)):
        sparkle(d, x, y, 1.4, 150)


def g_curse_warding(d):                              # heater shield with a warding rune and a returning arrow
    shield = [(-7.6, -8.6), (7.6, -8.6), (7.6, -1.0), (4.4, 5.8), (0, 9.8), (-4.4, 5.8), (-7.6, -1.0)]
    poly(d, shield)
    poly(d, [(x * 0.74, y * 0.74 - 0.4) for x, y in shield], 150)
    line(d, [(0, -6.4), (0, 6.2)], 1.2, 0)
    line(d, [(-3.6, -3.6), (0, -0.4), (3.6, -3.6)], 1.2, 0)
    line(d, [(-3.0, 1.2), (0, 3.6), (3.0, 1.2)], 1.2, 0)
    for s in (-1, 1):
        sparkle(d, s * 9.6, 5.4, 1.5)


def g_demon_beast(d):                                # horned underworld hound head
    poly(d, [(-6.2, -4.6), (-3.4, -7.2), (3.4, -7.2), (6.2, -4.6), (5.4, 1.4), (2.8, 5.0), (3.2, 9.6), (0, 7.8), (-3.2, 9.6), (-2.8, 5.0), (-5.4, 1.4)])
    for s in (-1, 1):
        taper(d, [(s * 5.2, -5.6), (s * 8.4, -8.0), (s * 9.6, -11.0)], 3.0, 0.5)
        poly(d, [(s * 1.0, -2.6), (s * 5.0, -3.8), (s * 4.0, -0.6), (s * 1.8, -0.8)], 0)
        poly(d, [(s * 2.8, 4.6), (s * 2.0, 7.8), (s * 1.0, 4.4)], 0)
    poly(d, [(-1.4, 1.2), (1.4, 1.2), (0, 3.2)], 0)
    for s in (-1, 1):
        poly(d, [(s * 6.0, -1.0), (s * 9.6, 1.6), (s * 5.6, 2.8)], 150)


def g_demon_fire(d):                                 # inverted flame: a hollow black fire with a spark inside
    poly(d, [(0, -10.6), (2.2, -6.0), (5.6, -8.0), (5.2, -2.6), (8.2, -1.2), (6.2, 4.6), (2.8, 9.2), (-2.8, 9.2), (-6.2, 4.6), (-8.2, -1.2), (-5.2, -2.6), (-5.6, -8.0), (-2.2, -6.0)])
    poly(d, [(0, -5.4), (1.6, -1.8), (4.4, 1.2), (3.2, 5.2), (0, 7.4), (-3.2, 5.2), (-4.4, 1.2), (-1.6, -1.8)], 0)
    poly(d, [(0, -1.6), (1.4, 1.6), (0.8, 4.6), (-0.8, 4.6), (-1.4, 1.6)], 150)


def g_demon_ice(d):                                  # frost-star of long shards around a hollow core
    for k in range(8):
        L = 10.8 if k % 2 == 0 else 7.4
        w = 2.0 if k % 2 == 0 else 1.5
        poly(d, rot([(0, -L), (w, -3.0), (0, -1.0), (-w, -3.0)], k * 45))
        poly(d, rot([(0, -L), (w, -3.0), (0, -1.0)], k * 45), 150)
    circ(d, 0, 0, 3.4)
    circ(d, 0, 0, 1.6, 0)


def g_demon_light(d):                                # black lens-flare: a hollow sun-star
    sparkle(d, 0, 0, 10.8)
    for k in range(4):
        poly(d, rot([(0, -6.8), (1.4, -2.0), (0, 0), (-1.4, -2.0)], 45 + k * 90))
    circ(d, 0, 0, 4.4)
    ring(d, 0, 0, 3.0, 1.0, 0)
    circ(d, 0, 0, 1.4, 150)
    for (x, y, r) in ((-8.4, -8.0, 0.9), (8.4, 8.4, 0.9), (8.6, -8.4, 0.7), (-8.6, 8.2, 0.7)):
        circ(d, x, y, r, 150)


def g_demon_water(d):                                # a black wave breaking over with a hooked crest
    outer = bez((-10.4, 9.6), (-9.6, -6.4), (1.0, -11.6), (7.6, -7.6))
    hook = bez((7.6, -7.6), (10.6, -5.6), (9.6, -0.4), (5.8, -1.4))
    inner = bez((5.8, -1.4), (7.4, -3.8), (4.6, -6.0), (1.0, -4.0))
    back = bez((1.0, -4.0), (-3.0, -1.6), (-2.6, 5.0), (3.0, 9.6))
    poly(d, outer + hook[1:] + inner[1:] + back[1:] + [(10.4, 9.6), (10.4, 10.8), (-10.4, 10.8)])
    poly(d, bez((-6.0, 9.0), (-6.4, -1.0), (-1.0, -6.4), (2.6, -6.0)) + bez((2.6, -6.0), (-2.0, -2.0), (-1.4, 5.0), (3.0, 9.0))[1:], 150)
    for (x, y, r) in ((9.4, 2.6, 1.1), (6.4, 4.4, 0.8), (10.0, -9.6, 0.8)):
        circ(d, x, y, r)
    for x in (-6.6, -3.6):
        poly(d, [(x - 0.8, 8.6), (x + 0.8, 8.6), (x, 10.4)], 0)


def g_eye(d):                                        # almond eye with a tracking iris
    top = curve(lambda t: (-10 + 20 * t, -5.4 * math.sin(math.pi * t) ** 0.9), 0, 1, 30)
    bot = curve(lambda t: (10 - 20 * t, 5.4 * math.sin(math.pi * t) ** 0.9), 0, 1, 30)
    poly(d, top + bot)
    circ(d, 0, 0, 4.2, 150)
    circ(d, 0, 0, 1.9, 0)
    circ(d, -1.3, -1.4, 0.9, 255)
    for x in (-5.0, 0.0, 5.0):
        line(d, [(x, -7.6 - (1.0 if x == 0 else 0)), (x, -10.2)], 0.9, 150)


def g_eyeball(d):                                    # veined eyeball with its nerve
    taper(d, [(4.6, 5.2), (7.8, 7.2), (9.6, 10.0)], 2.4, 1.6)
    circ(d, -1.4, -0.6, 7.8)
    circ(d, -2.0, -0.8, 3.6, 150)
    circ(d, -2.0, -0.8, 1.7, 0)
    for (a, b, c) in (((3.6, -5.0), (1.6, -2.8), (0.2, -1.6)), ((5.2, 0.2), (2.8, 0.2), (1.2, -0.2)), ((2.6, 4.6), (1.4, 2.6), (-0.4, 1.4)), ((-3.6, 5.8), (-3.4, 3.4), (-2.8, 2.2))):
        line(d, [a, b, c], 0.7, 0)
    circ(d, -3.2, -2.2, 0.8, 255)


def g_food(d):                                       # steaming bowl with chopsticks
    poly(d, [(-9.0, 0.0), (9.0, 0.0), (7.2, 6.0), (4.0, 9.4), (-4.0, 9.4), (-7.2, 6.0)])
    poly(d, [(-7.0, 1.8), (7.0, 1.8), (6.0, 4.6), (-6.0, 4.6)], 150)
    rect(d, -4.8, 9.4, 4.8, 10.6)
    line(d, [(-3.0, 0.0), (6.0, -9.6)], 1.0)
    line(d, [(-0.6, 0.0), (8.4, -8.4)], 1.0)
    for x in (-4.4, -0.6, 3.2):
        pts = [(x + math.sin(y * 0.9 + x) * 1.1, y) for y in np.linspace(-1.8, -9.4, 14)]
        line(d, pts, 1.2, 150)


def g_fungus(d):                                     # mushroom pair with spores
    def shroom(cx, base, rw, hh):
        top = base - hh - hh * 0.55
        cap = curve(lambda a: (cx + rw * math.cos(math.radians(a)), top + hh * 1.25 * math.sin(math.radians(a))), 180, 360, 24)
        poly(d, [(cx - rw * 0.3, top + 0.6), (cx + rw * 0.3, top + 0.6), (cx + rw * 0.25, base), (cx - rw * 0.25, base)])
        poly(d, cap + [(cx + rw, top + hh * 0.32), (cx - rw, top + hh * 0.32)])
        poly(d, [(cx + rw * 0.55, top - hh * 1.0), (cx + rw, top), (cx + rw * 0.4, top + 0.3)], 150)
        for (fx, fy, fr) in ((-0.42, -0.62, 0.15), (0.12, -0.9, 0.12), (0.45, -0.46, 0.13)):
            circ(d, cx + rw * fx, top + hh * fy, rw * fr, 0)
    shroom(-2.4, 9.6, 7.0, 4.6)
    shroom(7.2, 9.6, 3.2, 2.2)
    for (x, y, r) in ((-8.0, -9.0, 0.9), (-3.0, -10.2, 0.7), (3.0, -9.8, 0.9), (8.2, -6.6, 0.7), (-9.8, -4.6, 0.7)):
        circ(d, x, y, r, 150)


def g_gel(d):                                        # grinning slime
    body = bez((-9.0, 7.4), (-9.6, 0.0), (-4.0, -4.0), (0, -9.6)) + bez((0, -9.6), (4.0, -4.0), (9.6, 0.0), (9.0, 7.4))[1:]
    poly(d, body + [(10.2, 8.4), (-10.2, 8.4)])
    poly(d, [(x * 0.78, y * 0.78 + 1.2) for x, y in body] + [(7.0, 6.8), (-7.0, 6.8)], 150)
    circ(d, -3.2, -1.2, 1.5, 0)
    circ(d, 3.2, -1.2, 1.5, 0)
    arc(d, 0, 1.6, 3.2, 20, 160, 0.9, 0)
    poly(d, ellp(-5.2, -4.4, 1.1, 2.2, 40), 255)
    circ(d, 0, -9.8, 1.5)


def g_glass(d):                                      # three hyper-sharp shards
    poly(d, [(-9.8, 7.4), (-6.0, -9.6), (-2.2, 7.6)])
    poly(d, [(-6.0, -9.6), (-2.2, 7.6), (-4.6, 2.0)], 150)
    poly(d, [(-3.0, 9.4), (1.6, -10.4), (6.0, 9.4)])
    poly(d, [(1.6, -10.4), (6.0, 9.4), (2.6, 1.0)], 150)
    poly(d, [(4.0, 8.4), (9.0, -3.6), (10.4, 8.4)])
    line(d, [(1.0, -4.0), (-0.4, 1.2), (1.4, 4.2)], 0.7, 0)
    sparkle(d, -8.6, -4.0, 1.5)


def g_ice_wedge(d):                                  # wedge of ice driven into the ground, which splits around it
    poly(d, [(-10.4, 1.4), (-3.0, 1.4), (-1.8, 4.4), (-3.6, 6.0), (-2.4, 10.2), (-10.4, 10.2)])
    poly(d, [(10.4, 1.4), (3.0, 1.4), (1.8, 4.8), (3.8, 6.4), (2.6, 10.2), (10.4, 10.2)])
    poly(d, [(-6.2, -9.8), (6.2, -9.8), (0, 10.0)])
    poly(d, [(0, -9.8), (6.2, -9.8), (0, 10.0)], 150)
    line(d, [(-3.0, -6.6), (-0.8, -1.2)], 0.9, 0)
    for s in (-1, 1):
        line(d, [(s * 6.0, 4.0), (s * 8.0, 6.2)], 0.8, 0)
        line(d, [(s * 9.4, 1.4), (s * 7.6, 3.4), (s * 9.0, 5.0)], 0.7, 0)


def g_iron(d):                                       # anvil
    poly(d, [(-10.2, -5.6), (6.8, -5.6), (10.4, -7.0), (9.0, -2.2), (5.4, -1.0), (3.6, 1.8), (4.6, 3.6), (6.8, 3.6), (6.8, 7.0), (-6.8, 7.0), (-6.8, 3.6), (-4.6, 3.6), (-3.6, 1.8), (-5.2, -0.6), (-8.8, -2.2)])
    rect(d, -8.2, 7.0, 8.2, 9.8)
    rect(d, -9.0, -5.6, 8.4, -3.6, 150)
    line(d, [(-2.0, -1.0), (2.8, -1.0)], 0.9, 0)
    sparkle(d, -6.4, -9.2, 1.8)
    sparkle(d, 3.8, -9.8, 1.2)


def g_key(d):                                        # ornate key with a trefoil bow, tilted
    def R(pts):
        return rot(pts, 40)
    for (x, y) in ((0, -7.4), (-2.9, -3.6), (2.9, -3.6)):
        c = R([(x, y + 4.2)])[0]
        circ(d, c[0], c[1], 3.1)
    c0 = R([(0, -4.4)])[0]
    circ(d, c0[0], c0[1], 1.4, 0)
    shaft = R([(0, -2.0), (0, 9.6)])
    line(d, shaft, 2.2)
    for (y, w) in ((6.0, 3.8), (8.8, 3.0)):
        a = R([(0, y), (w, y)])
        line(d, a, 1.7)
    b = R([(0, 5.0), (0, 2.6)])
    for (y0) in (0.6,):
        c = R([(0, y0)])[0]
        circ(d, c[0], c[1], 1.8, 255)
    sparkle(d, -7.6, 7.6, 1.8, 150)


def g_legion(d):                                     # ranks of faceless constructs
    def fig(cx, cy, s, f=255, halo=False):
        if halo:
            circ(d, cx, cy - 3.4 * s, 2.1 * s, 0)
            poly(d, [(cx - 3.1 * s, cy - 1.4 * s), (cx + 3.1 * s, cy - 1.4 * s), (cx + 2.7 * s, cy + 4.6 * s), (cx - 2.7 * s, cy + 4.6 * s)], 0)
        circ(d, cx, cy - 3.4 * s, 1.55 * s, f)
        poly(d, [(cx - 2.4 * s, cy - 1.0 * s), (cx + 2.4 * s, cy - 1.0 * s), (cx + 2.0 * s, cy + 4.0 * s), (cx - 2.0 * s, cy + 4.0 * s)], f)
    for x in (-5.8, 0, 5.8):
        fig(x, -3.2, 0.85, 150)
    for x in (-3.8, 3.8):
        fig(x, 1.4, 1.0, 200, True)
    fig(0, 5.2, 1.25, 255, True)
    fig(-7.6, 5.6, 1.1, 255, True)
    fig(7.6, 5.6, 1.1, 255, True)


# --------------------------------------------------------------------------------------------- SET B glyphs (30)
def g_air(d):                                        # folding fan and the gust it throws
    for k in range(7):
        a = -90 + (k - 3) * 22
        r = math.radians(a)
        if k < 6:
            poly(d, [(0, 8.0), (math.cos(r) * 11.0 - 0.0, 8.0 + math.sin(r) * 11.0 - 0.6 * 0), (math.cos(r + math.radians(22)) * 11.0, 8.0 + math.sin(r + math.radians(22)) * 11.0)], 255 if k % 2 == 0 else 150)
    for k in range(7):
        r = math.radians(-90 + (k - 3) * 22)
        line(d, [(0, 8.0), (math.cos(r) * 10.4, 8.0 + math.sin(r) * 10.4)], 0.6, 0)
    circ(d, 0, 8.0, 1.6)
    poly(d, [(-1.2, 8.0), (1.2, 8.0), (0.8, 10.8), (-0.8, 10.8)])
    for (x, y, l) in ((-9.8, -2.4, 3.2), (-9.8, 1.0, 2.2)):
        line(d, [(x, y), (x + l, y)], 0.9, 150)


def g_hair(d):                                       # a sweeping lock of hair with a tie
    def cx(y):
        return 3.6 * math.sin(y * 0.40 + 0.6)

    def w(y):
        return 0.8 + 3.8 * max(0.0, math.sin(math.pi * (y + 10.6) / 21.2)) ** 0.6
    ys = list(np.linspace(-10.4, 10.4, 40))
    for (dx, f) in ((5.2, 150), (0.0, 255)):
        left = [(cx(y) + dx - w(y), y) for y in ys]
        right = [(cx(y) + dx + w(y), y) for y in ys]
        poly(d, left + right[::-1], f)
    for off in (-1.5, 0.0, 1.5):
        line(d, [(cx(y) + off * (w(y) / 3.8), y) for y in ys[4:-4]], 0.7, 0)
    rect(d, cx(-7.0) - 4.2, -8.4, cx(-7.0) + 4.2, -6.0, 150)
    line(d, [(cx(-7.0) - 4.2, -7.2), (cx(-7.0) + 4.2, -7.2)], 0.8, 0)


def g_memory(d):                                     # fanned photographs, a sunlit hill on the top one
    poly(d, rot([(-5.4, -6.8), (5.4, -6.8), (5.4, 6.8), (-5.4, 6.8)], -24, 0, 4))
    poly(d, rot([(-5.4, -6.8), (5.4, -6.8), (5.4, 6.8), (-5.4, 6.8)], 18, 1, 3), 150)
    poly(d, rot([(-6.0, -7.4), (6.0, -7.4), (6.0, 7.4), (-6.0, 7.4)], 0, -0.6, 0.6), 0)
    poly(d, [(-5.2, -6.6), (5.2, -6.6), (5.2, 6.6), (-5.2, 6.6)], 255)
    poly(d, [(-4.2, -5.6), (4.2, -5.6), (4.2, 2.8), (-4.2, 2.8)], 150)
    poly(d, [(-4.2, 2.8), (-1.2, -1.8), (1.0, 1.2), (2.6, -0.8), (4.2, 2.8)], 255)
    circ(d, 2.4, -3.2, 1.2, 255)
    d.rectangle(G.box(-4.2, 3.2, 4.2, 5.8), fill=255)
    sparkle(d, 8.6, -8.6, 1.8)


def g_mineral(d):                                    # geode: crystals rising from a rock cup
    taper(d, [(-5.0, 1.0), (-7.2, -6.0)], 3.2, 0.6)
    taper(d, [(0, 1.0), (0, -9.8)], 4.0, 0.6)
    taper(d, [(5.0, 1.0), (7.6, -5.6)], 3.2, 0.6)
    poly(d, [(0, -9.6), (2.0, 1.0), (0, 1.0)], 150)
    poly(d, [(-7.0, -5.6), (-3.8, 1.0), (-5.6, 1.0)], 150)
    poly(d, [(7.4, -5.4), (6.8, 1.0), (4.6, 1.0)], 150)
    poly(d, [(-9.6, 0.4), (9.6, 0.4), (8.0, 6.6), (3.6, 10.0), (-3.6, 10.0), (-8.0, 6.6)])
    poly(d, [(-7.4, 1.6), (7.4, 1.6), (6.2, 5.4), (-6.2, 5.4)], 150)
    line(d, [(-3.0, 6.6), (-1.0, 8.4), (2.0, 6.8)], 0.8, 0)
    sparkle(d, 8.6, -9.0, 1.4)


def g_modification(d):                                   # spanner crossed with a screwdriver
    line(d, [(-7.6, 8.0), (4.6, -4.2)], 2.3)
    poly(d, rot([(-2.8, -0.8), (-1.2, -0.8), (-1.2, -3.6), (1.2, -3.6), (1.2, -0.8), (2.8, -0.8), (2.8, -5.0), (-2.8, -5.0)], 45, 6.4, -6.4), 255)
    ring(d, 6.2, -6.2, 3.8, 2.2, 255)
    poly(d, rot([(-1.4, -2.4), (1.4, -2.4), (1.4, 2.4), (-1.4, 2.4)], 45, 6.2, -6.2), 0)
    line(d, [(7.6, 8.0), (-3.0, -2.6)], 2.0, 255)
    poly(d, [(-6.4, -6.4), (-3.4, -4.4), (-4.4, -3.4)], 255)
    taper(d, [(-3.8, -3.8), (-8.6, -8.6)], 1.6, 0.6)
    circ(d, 8.2, 8.4, 2.4, 150)
    circ(d, -8.0, 8.4, 2.4, 150)
    sparkle(d, -8.4, -8.8, 1.4, 150)


def g_mucus(d):                                      # a lump of goo with a long sagging string to a drip
    pts = []
    for i in range(36):
        a = math.tau * i / 36
        r = 5.2 + 0.7 * math.sin(a * 3 + 1.0)
        pts.append((-2.6 + math.cos(a) * r, -3.6 + math.sin(a) * r * 0.9))
    poly(d, pts)
    poly(d, ellp(-3.8, -5.0, 2.0, 1.2, -30), 150)
    taper(d, [(1.0, 0.0), (3.2, 4.0), (4.6, 8.0)], 3.4, 1.0)
    circ(d, 4.8, 8.4, 2.0)
    poly(d, ellp(-7.6, 3.4, 0.9, 1.4, 0))
    for (x, y, r) in ((8.8, -3.0, 1.0), (7.4, 0.4, 0.7), (-9.0, -9.0, 0.8)):
        circ(d, x, y, r, 150)
    arc(d, -2.6, -3.6, 3.0, 20, 110, 0.9, 0)


def g_mud(d):                                        # a dripping clump of mud
    for (x, y, r) in ((-3.6, -4.2, 4.2), (3.0, -4.6, 4.6), (0.0, -2.0, 5.2), (-6.0, 0.0, 2.8), (6.6, -0.4, 3.0)):
        circ(d, x, y, r)
    for (x, L, w) in ((-4.0, 6.4, 2.6), (0.6, 8.6, 3.0), (5.0, 5.2, 2.4)):
        taper(d, [(x, 0.4), (x, L)], w, w * 0.7)
        circ(d, x, L + 0.8, w * 0.62)
    poly(d, ellp(-2.6, -5.4, 2.2, 1.3, -25), 150)
    for (x, y, r) in ((-2.6, -1.2, 0.7), (3.4, -3.4, 0.6), (1.2, -6.6, 0.55)):
        circ(d, x, y, r, 150)
    d.ellipse(G.box(-9.4, 9.2, 9.4, 11.0), fill=150)
    for (x, y, r) in ((8.4, -9.0, 0.9), (-9.0, -8.0, 0.8)):
        circ(d, x, y, r)


def g_nail(d):                                       # two iron nails, crossed
    for (ang, L, w) in ((-38, 9.8, 1.5), (38, 8.2, 1.3)):
        pts = [(0, -L), (0, L)]
        line(d, rot(pts, ang, 0, 0), w)
        poly(d, rot([(-w * 2.2, -L - 1.1), (w * 2.2, -L - 1.1), (w * 2.2, -L + 0.4), (-w * 2.2, -L + 0.4)], ang, 0, 0))
        poly(d, rot([(-w * 0.7, L - 1.2), (w * 0.7, L - 1.2), (0, L + 1.6)], ang, 0, 0))
    circ(d, 0, 0, 1.0, 0)
    sparkle(d, 8.6, -8.4, 1.5, 150)


def g_permeation(d):                                 # an arrow passing through a brick wall
    poly(d, [(-3.4, -9.8), (3.4, -9.8), (3.4, 9.8), (-3.4, 9.8)])
    for y in (-5.4, -1.0, 3.4):
        line(d, [(-3.4, y), (3.4, y)], 0.7, 0)
    line(d, [(-10.0, 0.6), (8.0, 0.6)], 4.0, 0)
    line(d, [(-10.0, 0.6), (7.0, 0.6)], 1.7, 255)
    poly(d, [(6.0, -2.6), (10.4, 0.6), (6.0, 3.8)], 255)
    for x in (-8.6, -6.4):
        line(d, [(x, -3.4), (x - 1.0, -2.0)], 0.7, 150)
        line(d, [(x, 4.6), (x - 1.0, 3.2)], 0.7, 150)


def g_poison_plant(d):                                   # belladonna: leaves above, berries hanging below
    taper(d, [(0, -9.4), (0, -1.0), (-4.0, 3.0)], 1.5, 1.0)
    for s in (-1, 1):
        poly(d, rot(ellp(s * 4.6, -6.0, 3.8, 2.0, 0), s * 28, s * 4.6, -6.0))
        line(d, rot([(s * 2.2, -6.0), (s * 6.8, -6.0)], s * 28, s * 4.6, -6.0), 0.6, 0)
    for (x, y, bx, by) in ((-4.6, 4.8, -4.0, 3.0), (3.2, 2.0, 0, -1.0), (5.0, 7.4, 3.2, 2.0)):
        line(d, [(bx, by), (x, y - 2.0)], 0.9, 255)
        circ(d, x, y, 2.9)
        circ(d, x - 0.9, y - 0.9, 1.0, 150)
        poly(d, [(x - 1.4, y - 2.6), (x, y - 3.4), (x + 1.4, y - 2.6), (x, y - 1.6)], 0)
    taper(d, [(-8.6, 8.6), (-8.6, 9.8)], 1.0, 1.0)
    circ(d, -8.6, 7.4, 1.0)


def g_red_ochre(d):                                  # stencilled handprint
    poly(d, [(-4.0, 9.8), (4.0, 9.8), (5.2, 2.6), (-5.2, 2.6)])
    poly(d, ellp(0, 3.4, 5.2, 3.8))
    for (x0, y0, x1, y1, w) in ((-4.6, 1.8, -7.6, -3.4, 1.8), (-2.2, 0.0, -3.0, -8.8, 1.8), (0.4, 0.0, 0.6, -10.2, 1.9), (2.8, 0.6, 4.2, -8.6, 1.8)):
        taper(d, [(x0, y0), (x1, y1)], w, w * 0.8)
    taper(d, [(4.6, 3.4), (8.2, 0.4), (9.6, -2.6)], 2.0, 1.5)
    for (x, y, r) in ((-9.4, 6.4, 0.7), (9.0, 6.0, 0.9), (-8.4, -8.6, 0.7)):
        circ(d, x, y, r, 150)
    circ(d, 0, 4.6, 1.2, 150)


def g_rock(d):                                       # cracked boulder on gravel
    poly(d, [(-9.6, 6.8), (-8.4, -1.6), (-4.2, -7.6), (2.4, -9.0), (8.0, -4.4), (9.8, 3.0), (7.8, 6.8)])
    poly(d, [(2.4, -9.0), (8.0, -4.4), (9.8, 3.0), (7.8, 6.8), (3.0, 6.8), (4.2, -1.0)], 150)
    line(d, [(-2.0, -8.0), (-0.4, -3.0), (-3.0, 0.4), (-1.0, 4.4), (-2.2, 6.8)], 0.9, 0)
    line(d, [(-0.4, -3.0), (2.0, -1.6)], 0.8, 0)
    for (x, y, r) in ((-9.8, 9.2, 1.1), (-6.4, 9.6, 0.8), (6.4, 9.4, 1.0), (9.6, 9.0, 0.8), (0, 9.6, 0.8)):
        circ(d, x, y, r)


def g_sandstone(d):                                  # strata block with grains
    poly(d, [(-9.4, -3.2), (0, -9.2), (9.4, -3.2), (9.4, 6.0), (0, 10.2), (-9.4, 6.0)])
    poly(d, [(-9.4, -3.2), (0, -9.2), (9.4, -3.2), (0, 2.2)], 150)
    for (y0, y1) in ((3.0, 4.6), (6.0, 7.4)):
        pts = [(x, y0 + 0.0 + math.sin(x * 0.8) * 0.5 + (x + 9.4) * 0.0) for x in np.linspace(-9.4, 0, 12)]
        line(d, [(x, y + (9.4 + x) * 0.0 + (0 if x < 0 else 0)) for x, y in pts], 0.8, 0)
    line(d, [(0, 2.2), (0, 10.2)], 0.9, 0)
    line(d, [(-9.4, -3.2), (0, 2.2), (9.4, -3.2)], 0.9, 0)
    for (a, b) in (((1.6, 5.2), (9.4, 1.0)), ((1.6, 8.0), (9.4, 3.6))):
        line(d, [a, b], 0.8, 0)
    for (x, y) in ((-5.4, 1.6), (-2.4, 4.8), (5.2, 6.0), (3.0, 3.0)):
        circ(d, x, y, 0.6, 255)


def g_scale(d):                                      # overlapping scales
    mask = Image.new("L", (G.S, G.S), 0)
    poly(ImageDraw.Draw(mask), [(0, -10.6), (9.6, -3.0), (6.4, 8.6), (0, 10.6), (-6.4, 8.6), (-9.6, -3.0)])
    layer = Image.new("L", (G.S, G.S), 0)
    ld = ImageDraw.Draw(layer)
    for r, y in enumerate((-9.0, -4.6, -0.2, 4.2, 8.6)):
        off = 0.0 if r % 2 == 0 else 3.4
        for k in range(-3, 4):
            x = k * 6.8 + off
            circ(ld, x, y, 4.0, 255)
            arc(ld, x, y, 3.5, 190, 350, 1.3, 0)
            circ(ld, x, y + 0.8, 1.5, 150)
    a = np.minimum(np.asarray(layer), np.asarray(mask))
    d._image.paste(255, (0, 0), Image.fromarray(a))


def g_shakudo(d):                                    # tsuba (sword guard) with blade slot and cut-outs
    pts = []
    for i in range(96):
        a = math.tau * i / 96
        r = 9.8 + 0.9 * math.cos(a * 4)
        pts.append((math.cos(a) * r, math.sin(a) * r))
    poly(d, pts)
    ring(d, 0, 0, 6.9, 1.0, 150)
    poly(d, [(-1.4, -3.8), (1.4, -3.8), (1.4, 3.8), (-1.4, 3.8)], 0)
    for s in (-1, 1):
        circ(d, s * 3.4, 0, 1.1, 0)
    for k in range(4):
        a = math.radians(45 + k * 90)
        circ(d, math.cos(a) * 7.8, math.sin(a) * 7.8, 1.5, 0)
    sparkle(d, 4.6, -5.0, 1.4, 150)


def g_skin(d):                                       # fingerprint ridges
    for i, r in enumerate((2.0, 3.8, 5.6, 7.4, 9.0)):
        a0, a1 = (200, 520) if i % 2 == 0 else (160, 440)
        arc(d, 0, 1.0, r, a0, a1, 1.2, 255)
    arc(d, 0, 1.0, 9.0, 160, 200, 1.2, 255)
    line(d, [(0, 1.0), (0, 4.0)], 1.2)
    circ(d, 0, 0.6, 0.8)
    for (x0, x1) in ((-6.8, -6.4), (6.4, 6.9)):
        line(d, [(x0, 3.6), (x1, 7.6)], 1.0, 150)


def g_smoke(d):                                      # a smoke column that curls over at the top
    pts = [(math.sin(y * 0.5) * 2.6, y) for y in np.linspace(10.0, -3.0, 30)]
    taper(d, pts, 1.6, 6.2)
    for (cx, cy, r) in ((-2.0, -4.6, 3.6), (2.6, -6.4, 3.4), (-3.6, -8.6, 2.0), (4.8, -2.6, 2.0)):
        circ(d, cx, cy, r)
    for (cx, cy, r) in ((-1.6, -5.0, 1.3), (2.6, -6.6, 1.1)):
        circ(d, cx, cy, r, 150)
    for (x, y, r) in ((-8.0, 5.0, 1.2), (8.0, 1.0, 1.0), (7.4, 7.6, 0.8)):
        circ(d, x, y, r, 150)


def g_snow(d):                                       # snowman
    circ(d, 0, 6.2, 4.4)
    circ(d, 0, -0.6, 3.4)
    circ(d, 0, -6.2, 2.5)
    rect(d, -3.6, -8.6, 3.6, -8.0)
    rect(d, -2.1, -11.0, 2.1, -8.6)
    rect(d, -2.1, -9.4, 2.1, -8.6, 150)
    poly(d, [(0, -6.2), (4.0, -5.6), (0, -5.0)], 150)
    circ(d, -1.0, -6.8, 0.45, 0)
    circ(d, 1.0, -6.8, 0.45, 0)
    for y in (-1.2, 0.4):
        circ(d, 0, y, 0.55, 0)
    for s in (-1, 1):
        line(d, [(s * 3.0, -1.0), (s * 7.6, -3.8), (s * 9.4, -3.2)], 0.9, 150)
    sparkle(d, 8.6, 7.6, 1.5, 150)
    sparkle(d, -8.6, -4.8, 1.2, 150)


def g_song(d):                                       # beamed notes
    for x in (-4.6, 4.6):
        line(d, [(x + 1.7, 5.4), (x + 1.7, -7.4)], 1.3)
        poly(d, ellp(x, 5.8, 2.7, 2.0, -20))
    poly(d, [(-2.9, -7.4), (6.3, -9.8), (6.3, -6.2), (-2.9, -3.8)])
    arc(d, 0, 0, 10.0, 200, 250, 0.9, 150)
    arc(d, 0, 0, 10.0, 290, 340, 0.9, 150)
    sparkle(d, 8.8, 5.0, 1.3, 150)


def g_soul_corpse(d):                                # coffin with a wisp rising from it
    poly(d, [(-3.0, -1.0), (3.0, -1.0), (6.4, 3.0), (4.4, 10.4), (-4.4, 10.4), (-6.4, 3.0)])
    poly(d, [(-2.0, 0.6), (2.0, 0.6), (4.2, 3.6), (3.0, 8.8), (-3.0, 8.8), (-4.2, 3.6)], 150)
    line(d, [(0, 1.6), (0, 8.0)], 1.1, 0)
    line(d, [(-2.2, 4.2), (2.2, 4.2)], 1.1, 0)
    pts = [(math.sin(y * 0.9) * 1.8, y) for y in np.linspace(-2.0, -8.0, 14)]
    taper(d, pts, 1.2, 3.4)
    circ(d, 0.6, -9.2, 2.0)
    for (x, y) in ((-6.0, -5.6), (6.4, -3.6)):
        circ(d, x, y, 1.0, 150)


def g_soul(d):                                       # hitodama: a wisp with a tail
    circ(d, 0, -3.2, 4.8)
    tail = bez((3.2, -0.6), (6.4, 2.8), (2.0, 6.4), (-2.6, 9.4))
    left = bez((-3.2, -0.6), (-6.0, 3.2), (-2.4, 4.8), (-3.0, 7.0))
    poly(d, left + [(-2.6, 9.4)] + tail[::-1])
    poly(d, [(0, -9.6), (2.6, -6.0), (-2.6, -6.0)])
    circ(d, 0, -2.4, 2.6, 150)
    circ(d, -1.4, -3.8, 0.9, 255)
    circ(d, 0, 0.4, 1.0, 0)
    for (x, y, r) in ((-7.6, -6.8, 0.9), (7.8, -5.6, 1.1)):
        circ(d, x, y, r, 150)


def g_sound(d):                                      # speaker cone and sound waves
    poly(d, [(-9.6, -3.0), (-6.0, -3.0), (-1.4, -7.8), (-1.4, 7.8), (-6.0, 3.0), (-9.6, 3.0)])
    poly(d, [(-6.0, -3.0), (-1.4, -7.8), (-1.4, 7.8), (-6.0, 3.0)], 150)
    for i, r in enumerate((4.2, 7.2, 10.0)):
        arc(d, -2.0, 0, r, -48, 48, 1.4, 255)


def g_spike(d):                                      # spiked iron ball
    circ(d, 0, 0, 5.4)
    circ(d, -1.2, -1.2, 3.0, 150)
    for k in range(10):
        a = math.radians(k * 36 - 90)
        c, s = math.cos(a), math.sin(a)
        L = 10.4
        poly(d, [(c * 4.6 - s * 1.6, s * 4.6 + c * 1.6), (c * L, s * L), (c * 4.6 + s * 1.6, s * 4.6 - c * 1.6)])
    circ(d, 0, 0, 1.2, 0)


def g_switching(d):                                  # two arrows swapping places
    for (y, s) in ((-4.8, 1), (4.8, -1)):
        line(d, [(-8.4 * s, y), (3.0 * s, y)], 3.0)
        poly(d, [(2.4 * s, y - 4.6), (10.6 * s, y), (2.4 * s, y + 4.6)])
    circ(d, 0, 0, 1.3, 150)


def g_tongue(d):                                     # open mouth with fangs and a lolling tongue
    d.ellipse(G.box(-10.0, -9.6, 10.0, 3.2), fill=255)
    d.ellipse(G.box(-8.2, -8.0, 8.2, 1.8), fill=0)
    for x in (-5.6, -2.0, 2.0, 5.6):
        poly(d, [(x - 1.5, -7.4), (x + 1.5, -7.4), (x, -3.6)])
    poly(d, [(-4.8, -2.0), (4.8, -2.0), (5.0, 3.6), (3.6, 8.6), (0, 10.4), (-3.6, 8.6), (-5.0, 3.6)])
    poly(d, [(-0.2, -1.0), (3.8, -1.0), (3.6, 3.6), (2.8, 7.6), (0, 9.2), (-0.2, 5.0)], 150)
    line(d, [(0, -0.4), (0, 6.6)], 0.9, 0)
    circ(d, -2.4, 3.0, 0.5, 0)
    circ(d, -2.0, 5.6, 0.5, 0)


def g_tree(d):                                       # broadleaf tree
    poly(d, [(-1.6, 10.0), (1.6, 10.0), (1.2, 2.0), (4.8, -1.6), (4.0, -2.8), (0.8, -0.2), (0.4, -3.4), (-0.4, -3.4), (-0.8, -0.2), (-4.0, -2.8), (-4.8, -1.6), (-1.2, 2.0)])
    for (x, y, r) in ((0, -7.2, 4.2), (-5.4, -3.8, 3.6), (5.4, -3.8, 3.6), (-2.6, -4.6, 3.8), (2.8, -4.6, 3.8)):
        circ(d, x, y, r)
    for (x, y, r) in ((-2.4, -7.2, 1.5), (3.4, -3.0, 1.3), (-5.6, -2.2, 1.1)):
        circ(d, x, y, r, 150)
    poly(d, [(-4.6, 10.0), (4.6, 10.0), (3.8, 8.4), (-3.8, 8.4)], 150)


def g_stone(d):                                      # masonry wall of carved blocks
    rows = ((-9.4, -4.4, ((-9.8, -1.0), (-0.6, 9.8))), (-3.8, 1.2, ((-9.8, -5.6), (-5.0, 4.0), (4.6, 9.8))), (1.8, 6.8, ((-9.8, -1.0), (-0.6, 9.8))), (7.4, 10.2, ((-9.8, -5.6), (-5.0, 4.0), (4.6, 9.8))))
    for (y0, y1, blocks) in rows:
        for (x0, x1) in blocks:
            poly(d, [(x0, y0), (x1, y0), (x1, y1), (x0, y1)])
            poly(d, [(x0 + 0.5, y0 + 0.5), (x1 - 0.5, y0 + 0.5), (x1 - 0.5, y0 + 1.5), (x0 + 0.5, y0 + 1.5)], 150)


def g_vine(d):                                       # climbing vine with pointed leaves and curled tendrils
    pts = [(math.sin(y * 0.5) * 3.0, y) for y in np.linspace(10.6, -10.0, 40)]
    line(d, pts, 1.5)
    for (y, s) in ((7.4, 1), (2.6, -1), (-2.4, 1), (-7.0, -1)):
        x = math.sin(y * 0.5) * 3.0
        poly(d, rot(ellp(x + s * 3.8, y - 1.0, 3.6, 1.9, 0), -s * 22, x + s * 3.8, y - 1.0))
        line(d, rot([(x + s * 1.2, y - 1.0), (x + s * 6.0, y - 1.0)], -s * 22, x + s * 3.8, y - 1.0), 0.6, 0)
    arc(d, 4.8, 7.4, 2.0, 180, 450, 0.9, 150)
    arc(d, -5.4, -9.0, 1.8, 0, 270, 0.9, 150)


def g_vortex(d):                                     # two-armed whirlpool
    for k in range(2):
        pts = curve(lambda a, k=k: ((1.0 + a * 0.028) * math.cos(math.radians(a + k * 180)), (1.0 + a * 0.028) * math.sin(math.radians(a + k * 180))), 0, 520, 60)
        pts = [(x * 1.0, y * 1.0) for x, y in pts]
        taper(d, pts, 1.2, 3.4)
    circ(d, 0, 0, 1.5, 150)
    for (x, y, r) in ((8.8, -6.4, 0.8), (-8.6, 6.8, 0.8)):
        circ(d, x, y, r, 150)


def g_wing(d):                                       # swept feathered wing
    angs = (-100, -80, -60, -42, -26, -12)
    lens = (16.0, 17.0, 16.0, 14.4, 12.4, 10.0)
    for a, L in zip(angs, lens):
        r = math.radians(a)
        mid = (-8.4 + math.cos(r) * L * 0.5 + 0.0, 8.2 + math.sin(r) * L * 0.5 + 0.0)
        tip = (-8.4 + math.cos(r) * L, 8.2 + math.sin(r) * L)
        taper(d, [(-8.4, 8.2), mid, tip], 3.6, 0.8)
    taper(d, [(-8.4, 8.2), (-9.0, -1.0), (-4.0, -8.4), (3.0, -10.6)], 3.2, 1.6)
    for a, L in zip(angs[1:5], lens[1:5]):
        r = math.radians(a)
        line(d, [(-8.4 + math.cos(r) * L * 0.25, 8.2 + math.sin(r) * L * 0.25), (-8.4 + math.cos(r) * L * 0.82, 8.2 + math.sin(r) * L * 0.82)], 0.6, 150)
    sparkle(d, 8.4, 7.2, 1.4, 150)


# --------------------------------------------------------------------------------------------- tables
# key -> (display, glow, body, glyph, dark relief, glyph scale)
def _lt(c, t):
    return G.mix(G.rgb(c), (255, 255, 255), t)


def _hex(c):
    return (c[0] << 16) | (c[1] << 8) | c[2]


SET_A = {   # the 29 stubs: the colours stay exactly those of tools/icons/g_<key>.py
    "barrier": (0x6AD0FF, 0xCDEFFF, g_barrier, False), "black_oil": (0x40E0A0, 0xBFF4DF, g_black_oil, False),
    "body": (0xFF8A4A, 0xFFD8C2, g_body, False), "briar": (0x8AFF4A, 0xD8FFC2, g_briar, False),
    "bronze": (0xD09A4A, 0xEFDDC2, g_bronze, False), "bubble": (0xA0D8FF, 0xDFF2FF, g_bubble, False),
    "butoh": (0xFF5A8A, 0xFFC8D8, g_butoh, False), "chain": (0xC0C8E0, 0xEAECF4, g_chain, False),
    "cherry_blossom": (0xFFB0D0, 0xFFE4EF, g_cherry_blossom, False), "copper": (0xFF9A4A, 0xFFDDC2, g_copper, False),
    "corundum": (0xFF6A7A, 0xFFCDD2, g_corundum, False), "crystal": (0xA0E0FF, 0xDFF4FF, g_crystal, False),
    "curse": (0x9A2AFF, 0xDDB8FF, g_curse, True), "curse_warding": (0xC08AFF, 0xEAD8FF, g_curse_warding, False),
    "demon_beast": (0xB02A6A, 0xE4B8CD, g_demon_beast, True), "demon_fire": (0x8A2AD0, 0xD8B8EF, g_demon_fire, True),
    "demon_ice": (0x3A6AFF, 0xBDCDFF, g_demon_ice, True), "demon_light": (0xFF3AD0, 0xFFBDEF, g_demon_light, True),
    "demon_water": (0x20D0A0, 0xB4EFDF, g_demon_water, True), "eye": (0xFF4A9A, 0xFFC2DD, g_eye, False),
    "eyeball": (0xFF3A3A, 0xFFBDBD, g_eyeball, False), "food": (0xFFC04A, 0xFFEAC2, g_food, False),
    "fungus": (0xC0A04A, 0xEADFC2, g_fungus, False), "gel": (0x7AFFD8, 0xD2FFF2, g_gel, False),
    "glass": (0xE0FFFF, 0xF4FFFF, g_glass, False), "ice_wedge": (0xC0F0FF, 0xEAFAFF, g_ice_wedge, False),
    "iron": (0xB0B8C8, 0xE4E7EC, g_iron, False), "key": (0xFFD04A, 0xFFEFC2, g_key, False),
    "legion": (0xD0C080, 0xEFEAD4, g_legion, False),
}

_B_COL = {   # glow colours are the ones tools/gen_grimoire_icons.mjs used, so each magic keeps its colour identity
    "air": 0x9FE8FF, "hair": 0xFF6E9A, "memory": 0xBCA8FF, "mineral": 0x78A8C8, "modification": 0xFF9B58, "mucus": 0x72D6B4,
    "mud": 0x8A593D, "nail": 0xC7B9A6, "permeation": 0x65D7E8, "poison_plant": 0x9FCB3B, "red_ochre": 0xD64C37, "rock": 0x777C86,
    "sandstone": 0xD5A76C, "scale": 0x63BDB5, "shakudo": 0x6E4946, "skin": 0xE1A995, "smoke": 0x82788F, "snow": 0xE7F7FF,
    "song": 0xFFB4E8, "soul_corpse": 0x6C7899, "soul": 0x72D9CC, "sound": 0x70C8F0, "spike": 0x9BA8B9, "switching": 0xFFD260,
    "tongue": 0xE66C70, "tree": 0x477D39, "stone": 0x8F8C86, "vine": 0x6DB743, "vortex": 0x59B8C9, "wing": 0xE6E2F4,
}
_B_DARK = {"soul_corpse"}
# dark source colours (browns, slate) would vanish on the dark aura: these magics get a brighter glow / body of the same hue
_B_OVERRIDE = {"mud": (0xC9894A, 0xEDC68F), "shakudo": (0xC47C6C, 0xF2CDBF), "tree": (0x62B24C, 0xC8EDB4),
               "soul_corpse": (0x8FA3D6, 0xDCE4FA), "rock": (0x9AA0AC, 0xE2E5EA), "stone": (0xB4B1A9, 0xECEAE4)}


def _b_entry(key):
    glow, body = _B_OVERRIDE.get(key) or (_B_COL[key], _hex(_lt(_B_COL[key], 0.55)))
    return (glow, body, globals()["g_" + key], key in _B_DARK)


SET_B = {k: _b_entry(k) for k in _B_COL}


def register(key, glow, body, glyph):
    G.MAGIC[key] = (key.replace("_", " ").title() + " Magic", glow, body, "", "")
    G.GLYPHS[key] = glyph


# --------------------------------------------------------------------------------------------- render
def variants(key, glow, body, dark, seed):
    """(active, buff, ultimate) 256 px canvases: the same recipe as icon_forge.magic_variants, with a per-magic dark flag."""
    a = G.to_arr(G.glyph_mask(key))
    img = G.bg_aura(glow)
    G.streaks(img, glow, seed + 1); G.particles(img, glow, 22, seed + 2, size=(0.4, 1.0))
    G.over(img, F.relief(a, glow, body, dark, 0.92))
    act = F.frame2(img, G.GOLD, glow, "clover", 0xE23B4B)
    img = G.bg_ancient(glow)
    G.chains(img, (112, 96, 80))
    G.magic_circle(img, glow, 11.8, ticks=24, width=0.9, rot=seed * 0.1)
    G.magic_circle(img, glow, 8.4, ticks=12, width=0.6, strength=0.6, rot=-seed * 0.2)
    G.over(img, F.relief(a, glow, body, dark, 0.64, 0.8))
    G.particles(img, glow, 10, seed + 3, size=(0.4, 0.9), area=0.7)
    buf = F.frame2(img, G.SILVER, glow, "rune", 0x5AC8FF)
    img = G.bg_void(glow)
    G.magic_circle(img, glow, 12.2, star=5, ticks=30, width=0.7, strength=0.9, rot=seed * 0.13)
    G.magic_circle(img, glow, 7.6, star=6, ticks=18, width=0.6, strength=0.7, rot=-seed * 0.21)
    G.particles(img, glow, 30, seed + 4, size=(0.3, 1.0), area=0.95)
    G.over(img, F.relief(a, glow, body, dark, 0.86, 1.4))
    ult = F.frame2(img, G.IRON, glow, "spike", glow)
    return act, buf, ult


def out_files():
    """[(set, relative path, builder)] for every redrawn icon, in a stable order."""
    items = []
    for i, key in enumerate(SET_A):
        glow, body, glyph, dark = SET_A[key]
        items.append(("A", key, glow, body, glyph, dark, 700 + i * 7))
    for i, key in enumerate(SET_B):
        glow, body, glyph, dark = SET_B[key]
        items.append(("B", key, glow, body, glyph, dark, 900 + i * 7))
    return items


def keep_classic(rel):
    src = os.path.join(TEX, rel)
    dst = os.path.join(CLASSIC, rel)
    if not os.path.exists(dst) and os.path.exists(src):
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)


def write_icon(img, rel):
    keep_classic(rel)
    out = G.reduce(img)
    os.makedirs(os.path.dirname(os.path.join(TEX, rel)), exist_ok=True)
    out.save(os.path.join(TEX, rel), optimize=True)
    return out


def main():
    only = sys.argv[sys.argv.index("--only") + 1] if "--only" in sys.argv else None
    if "--preview" in sys.argv:                                  # draw the active icons only, write nothing but the sheet
        ims, names = [], []
        for (cat, key, glow, body, glyph, dark, seed) in out_files():
            if only and only not in key:
                continue
            register(key, glow, body, glyph)
            ims.append(G.reduce(variants(key, glow, body, dark, seed)[0])); names.append(key)
        sheet(ims, sys.argv[sys.argv.index("--preview") + 1], names)
        return
    made, manifest, seen = [], [], {}
    for (cat, key, glow, body, glyph, dark, seed) in out_files():
        if only and only not in key:
            continue
        register(key, glow, body, glyph)
        act, buf, ult = variants(key, glow, body, dark, seed)
        targets = [("grimoire/%s.png" % key, act)]
        if cat == "A":
            targets += [("icons/%s_buff.png" % key, buf), ("icons/%s_ultimate.png" % key, ult)]
        for rel, im in targets:
            icon = write_icon(im, rel)
            h = hashlib.md5(icon.tobytes()).hexdigest()
            assert h not in seen, rel + " identical to " + seen[h]
            seen[h] = rel
            manifest.append("%s|%s" % (cat + ("-grimoire" if rel.startswith("grimoire") else "-buff" if rel.endswith("_buff.png") else "-ultimate"), rel))
            made.append((rel, icon))
        if cat == "A":                                           # the symbol layer (sym_<key>.png) of the same glyph
            rel = "icons/sym_%s.png" % key
            icon = write_icon(G.symbol_layer(key), rel)
            manifest.append("A-sym|" + rel)
            made.append((rel, icon))
    if not only:
        with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
            f.write("# redrawn by tools/skills/gen_icons_v2.py: set-kind|path under textures/skill (read by SkillIconsV2Test)\n")
            f.write("\n".join(manifest) + "\n")
    print("wrote", len(made), "icons")
    if "--sheet" in sys.argv:
        sheet([i for _, i in made], sys.argv[sys.argv.index("--sheet") + 1], [os.path.basename(r)[:-4] for r, _ in made])
    if "--doc" in sys.argv:
        doc_sheets()


def sheet(icons, path, labels=None, z=4, cols=14):
    from PIL import ImageFont
    cw, ch = 32 * z, 32 * z + (11 if labels else 0)
    rows = (len(icons) + cols - 1) // cols
    sh = Image.new("RGBA", (cols * cw, rows * ch), (34, 34, 40, 255))
    dr = ImageDraw.Draw(sh)
    for i, im in enumerate(icons):
        x, y = (i % cols) * cw, (i // cols) * ch
        sh.alpha_composite(im.convert("RGBA").resize((32 * z, 32 * z), Image.NEAREST), (x, y))
        if labels:
            dr.text((x + 1, y + 32 * z), labels[i][:20], fill=(210, 210, 210, 255))
    sh.convert("RGB").save(path)


def doc_sheets():
    """Before / after contact sheets for docs/skill_icons_v2.md: palette-quantised PNGs, each well under 200 KB."""
    os.makedirs(DOCS, exist_ok=True)
    rows = [l.split("|") for l in open(MANIFEST, encoding="utf-8").read().splitlines() if l and not l.startswith("#")]
    groups = {"a_grimoire": "A-grimoire", "a_buff": "A-buff", "a_ultimate": "A-ultimate", "b_grimoire": "B-grimoire"}
    for name, kind in groups.items():
        rels = [r for k, r in rows if k == kind]
        for tag, base in (("before", CLASSIC), ("after", TEX)):
            ims = [Image.open(os.path.join(base, r)).convert("RGBA") for r in rels]
            cols = 10
            z = 3
            rws = (len(ims) + cols - 1) // cols
            sh = Image.new("RGBA", (cols * (32 * z + 2), rws * (32 * z + 2)), (34, 34, 40, 255))
            for i, im in enumerate(ims):
                sh.alpha_composite(im.resize((32 * z, 32 * z), Image.NEAREST), ((i % cols) * (32 * z + 2), (i // cols) * (32 * z + 2)))
            p = os.path.join(DOCS, "%s_%s.png" % (name, tag))
            sh.convert("RGB").quantize(colors=96, method=Image.MEDIANCUT, dither=Image.NONE).save(p, optimize=True)
            print(p, os.path.getsize(p) // 1024, "KB")


if __name__ == "__main__":
    main()
