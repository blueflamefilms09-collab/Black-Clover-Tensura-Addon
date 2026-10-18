"""Temporary-armour pieces of the earth family: Sandstone, Rock, Red Ochre, Stone, Earth and Mineral magic (1.93.0 rebuild).

    python3 -B tools/gen_earth_family_armor.py [key ...]        # all six sets, or the keys named (sandstone rock red_ochre stone earth mineral)
    python3 -B tools/geo_check.py rock_helm rock_chest ...      # the checker prints one line per model

Every set is the SAME list of pieces (so the Java painter client/aura/EarthArmorKit can drive them with one table and client/aura/EquipAnimation,
the forge-in / shatter-out of the Djinn full equip), each in the set's own silhouette, material and glow:

    helm, chest, belt, mantle, pauldron_r/l, vambrace_r/l, gauntlet_r/l, greave_r/l, back (animated, behind the body), extra (animated, round the head)

Files, by GeoSpec.of(key, piece):  geo/entity/<key>_<piece>.geo.json, animations/entity/<key>_<piece>.animation.json (only the animated ones),
textures/entity/<key>_<piece>.png + _glow.png.  The pieces are authored in the HUMANOID FILE FRAME (feet y 0, neck y 24, head y 24..32, body x +-4,
arms x +-4..8 around the shoulder pivots (+-5, 22), legs x 0..4 around the hip pivots (+-1.9, 12)) and the painter puts each on the model part it
belongs to, so a piece turns with its limb. Solid cubes that sit on the skin are inflated (>= 0.3) so nothing is coplanar with the player.
Silhouettes:  Sandstone = stacked strata slabs and a dune collar, a monolith behind the back;  Rock = hulking boulder clusters and oversized fists,
orbiting rocks;  Red Ochre = smooth fired clay, painted bands, a horned mask crest and a sun disc;  Stone = dressed masonry blocks, a crenellated
crown, tablets;  Earth = soil strata with grass tufts and roots, a ring of clods;  Mineral = crystal clusters grown from ore matrix, a crystal fan.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from earth_family_lib import *  # noqa: E402,F401,F403

PIECES = ["helm", "chest", "belt", "mantle", "pauldron", "vambrace", "gauntlet", "greave", "back", "extra"]
PAIRED = ("pauldron", "vambrace", "gauntlet", "greave")


def pair(out, name, specs, bones=None, anim=None):
    out[name + "_r"] = (specs, bones, anim)
    out[name + "_l"] = (mirrored(specs), bones, anim)


def aim(dx, dy, dz):
    """Cube rotation (rx, 0, rz) that turns a cube standing along +y toward the direction (dx, dy, dz): rotation order Z then X, +rz tilts +y toward -x,
    +rx tilts +y toward +z (the renderer's convention, see GeoDraw.rotate)."""
    l = math.sqrt(dx * dx + dy * dy + dz * dz) or 1.0
    dx, dy, dz = dx / l, dy / l, dz / l
    return (math.degrees(math.atan2(dz, dy)), 0.0, math.degrees(math.asin(max(-1.0, min(1.0, -dx)))))


def prism(base, direction, length, thick, mat, bone="root", **kw):
    """A square prism of the given length standing on 'base' and pointing along 'direction' (a crystal, a horn, a spike, a root)."""
    bx, by, bz = base
    t = thick
    return spec((bx - t / 2, by, bz - t / 2), (t, length, t), mat=mat, rot=aim(*direction), pv=(bx, by, bz), bone=bone, **kw)


def fan(base, n, a0, a1, plane, length, thick, mat, lerp_len=1.0, bone="root", tilt=(0.0, 0.0), **kw):
    """n prisms radiating from 'base' in the 'xy' plane (angle measured from +y toward -x) or the 'yz' plane (from +y toward +z), a0..a1 degrees."""
    out = []
    for i in range(n):
        f = i / max(1, n - 1)
        a = math.radians(a0 + (a1 - a0) * f)
        d = (-math.sin(a), math.cos(a), 0.0) if plane == "xy" else (0.0, math.cos(a), math.sin(a))
        d = (d[0] + tilt[0], d[1], d[2] + tilt[1])
        L = length * (1.0 - (1.0 - lerp_len) * abs(f - 0.5) * 2)
        out.append(prism(base, d, L, thick, mat, bone, **kw))
    return out


def ring(cx, cy, cz, radius, n, size, mat, bone="root", phase=0.0, **kw):
    """n small cubes on a horizontal circle (a halo of slabs, orbiting clods, floating shards), each turned to follow the circle."""
    out = []
    for i in range(n):
        a = phase + i * math.tau / n
        px, pz = cx + math.sin(a) * radius, cz + math.cos(a) * radius
        s = size if not isinstance(size, (list, tuple)) else size
        sx, sy, sz = (s, s, s) if not isinstance(s, (list, tuple)) else s
        out.append(spec((px - sx / 2, cy - sy / 2, pz - sz / 2), (sx, sy, sz), mat=mat, rot=(0, math.degrees(a), 0), pv=(px, cy, pz), bone=bone, **kw))
    return out


def spin(anim_clip, bone, seconds, axis=1):
    """A looped full turn of a bone about one axis (0 x, 1 y, 2 z)."""
    an = Anim()
    def at(deg):
        v = [0.0, 0.0, 0.0]
        v[axis] = deg
        return tuple(v)
    keys(an, anim_clip, bone, "rot", {0.0: at(0), seconds / 2: at(180), seconds: at(360)}, length=seconds, loop=True)
    return an


def bob(an, clip, bone, seconds, amp, ang=0.0):
    """A looped hover (position y in pixels) and a slight sway (rotation z in degrees) of a bone."""
    wave(an, clip, bone, seconds, steps=8, y=S(amp), kind="pos")
    if ang:
        wave(an, clip, bone, seconds, steps=8, z=S(ang, 0.25), kind="rot")
    return an


# =================================================================================================================================
# ROCK: hulking boulder armour, dark slate with amber cracks; oversized fists, a jagged brow, rocks orbiting the body
# =================================================================================================================================
def rock_set():
    rng = np.random.default_rng(1103)
    G = (255, 150, 60)
    out = {}
    R = lambda *a, **k: spec(*a, mat="rock", **k)
    # helm: a heavy brow block over the eyes, cheek chunks, a ridge of crag on top
    h = [R((-4.7, 28.6, -4.7), (9.4, 3.8, 9.4), infl=0.3, glow=G, cracks=6),
         R((-4.9, 26.6, -5.4), (9.8, 2.0, 2.2), infl=0.2, tone=0.05),
         R((-5.2, 24.4, -4.4), (2.2, 3.4, 5.2), infl=0.2), R((3.0, 24.4, -4.4), (2.2, 3.4, 5.2), infl=0.2),
         R((-4.6, 24.0, 4.2), (9.2, 8.0, 1.6), infl=0.2, glow=G, cracks=5)]
    h += jagspecs(rng, 0, 33.2, -0.5, 3.6, 1.6, 3.8, 6, 2, 4, glow=G, cracks=2)
    h += [R((-1.2, 26.8, -5.9), (2.4, 1.2, 1.2), rot=(14, 0, 0), pv=(0, 27.4, -5.3), tone=0.08)]            # a nose ridge
    out["helm"] = (h, None, None)
    # chest: a cuirass of stacked chunks with cracks glowing amber
    c = [R((-4.6, 18.2, -3.3), (9.2, 5.4, 2.0), infl=0.2, glow=G, cracks=8), R((-5.0, 21.4, -3.5), (10.0, 3.0, 2.6), infl=0.2, rot=(-6, 0, 0), pv=(0, 22.4, -2.4)),
         R((-4.2, 12.4, -3.0), (8.4, 5.4, 1.6), infl=0.2, glow=G, cracks=5), R((-4.7, 12.2, 1.8), (9.4, 11.0, 1.8), infl=0.2, glow=G, cracks=8),
         R((-1.4, 16.0, -4.0), (2.8, 5.0, 1.2), rot=(0, 0, 8), pv=(0, 18.5, -3.4), glow=G, cracks=3, tone=0.06)]
    c += jagspecs(rng, -2.4, 20.8, -3.2, 2.2, 2.4, 0.8, 3, 1.6, 2.6, glow=G) + jagspecs(rng, 2.4, 19.4, -3.2, 2.2, 2.4, 0.8, 3, 1.6, 2.6, glow=G)
    out["chest"] = (c, None, None)
    # belt: hanging tassets
    b = [R((-4.8, 10.4, -2.8), (9.6, 2.4, 5.6), infl=0.2, cracks=3)]
    for i, x in enumerate((-4.6, -1.8, 1.0)):
        b.append(R((x, 6.4 - i % 2 * 0.8, -3.6), (3.4, 5.0, 1.4), rot=(-6, 0, 0), pv=(x + 1.7, 10.4, -3.4), infl=0.1, glow=G, cracks=3))
    b += [R((-6.2, 6.6, -2.0), (1.6, 5.0, 4.0), rot=(0, 0, 6), pv=(-5.4, 10.4, 0), infl=0.1), R((4.6, 6.6, -2.0), (1.6, 5.0, 4.0), rot=(0, 0, -6), pv=(5.4, 10.4, 0), infl=0.1)]
    out["belt"] = (b, None, None)
    # mantle: a spine of crags down the back and a rough collar
    m = []
    for k in range(6):
        y = 23.4 - k * 1.9
        s = 3.6 - 0.4 * k
        m.append(R((-s / 2, y, 2.2), (s, 2.2, 1.8 + 0.5 * (k % 2) + 1.2 * (k == 0)), rot=(0, 0, (k % 3 - 1) * 9), pv=(0, y + 1, 3), infl=0.1, glow=G, cracks=2))
    for i in range(5):
        a = -2.3 + i * 1.15
        m.append(R((math.sin(a) * 4.6 - 1.3, 23.8, math.cos(a) * 3.4 - 1.3), (2.6, 2.4, 2.6), rot=(0, math.degrees(a) * -1, 14), pv=(math.sin(a) * 4.6, 24.4, math.cos(a) * 3.4), infl=0.1))
    out["mantle"] = (m, None, None)
    # pauldron: a boulder on the shoulder over a flat base plate
    p = [R((-9.2, 22.4, -3.0), (5.4, 2.2, 6.0), infl=0.1, glow=G, cracks=3)]
    p += jagspecs(rng, -7.2, 25.0, 0, 2.6, 2.0, 2.6, 5, 3, 5, glow=G, cracks=2)
    p += [R((-9.4, 19.4, -2.6), (1.8, 3.2, 5.2), rot=(0, 0, -8), pv=(-9, 22, 0), infl=0.1)]
    pair(out, "pauldron", p)
    # vambrace: a slab sleeve with knuckles of rock along the outside
    v = [R((-8.5, 12.8, -2.6), (4.9, 6.2, 5.2), infl=0.1, glow=G, cracks=5), R((-9.3, 13.4, -1.8), (1.8, 4.0, 3.6), rot=(0, 0, -10), pv=(-8.5, 15, 0), infl=0.1),
         R((-8.1, 18.8, -3.0), (4.1, 1.6, 6.0), infl=0.1)]
    pair(out, "vambrace", v)
    # gauntlet: an oversized boulder fist hanging below the arm
    g = [R((-9.6, 8.4, -3.8), (6.8, 5.6, 7.4), infl=0.1, glow=G, cracks=6)]
    g += jagspecs(rng, -6.2, 8.4, -3.6, 3.4, 2.2, 3.2, 4, 2, 4, glow=G, cracks=2)
    g += [R((-9.0 + i * 1.9, 6.0, -4.6), (1.8, 1.4, 1.4), rot=(0, 0, 0), infl=0.0, tone=0.1) for i in range(3)]
    pair(out, "gauntlet", g)
    # greave: boulders over the shin, a knee crag, a heavy foot
    gr = [R((-4.7, 1.6, -3.0), (5.0, 7.0, 5.8), infl=0.1, glow=G, cracks=5), R((-4.9, 8.6, -3.6), (4.6, 3.4, 2.2), rot=(-8, 0, 0), pv=(-2, 10, -3), infl=0.1, glow=G, cracks=3),
          R((-5.0, 0.0, -4.2), (5.2, 2.2, 7.6), infl=0.1)]
    gr += jagspecs(rng, -2.0, 4.6, -3.2, 2.0, 2.6, 0.6, 3, 1.6, 2.6)
    pair(out, "greave", gr)
    # back: four boulders orbiting the body (animated)
    bk = []
    for i in range(4):
        a = i * math.pi / 2 + 0.4
        cx, cz, cy = math.sin(a) * 13.0, math.cos(a) * 13.0, 16.0 + (i % 2) * 6.0 - 2.0
        bk += jagspecs(rng, cx, cy, cz, 0.6, 0.6, 0.6, 2, 3, 5, bone="ring", glow=G, cracks=2)
        bk.append(R((cx - 1.8, cy - 1.8, cz - 1.8), (3.6, 3.6, 3.6), rot=(18, 30 * i, 12), pv=(cx, cy, cz), bone="ring", glow=G, cracks=3))
    an = Anim()
    keys(an, "idle", "ring", "rot", {0.0: (0, 0, 0), 4.5: (0, 180, 0), 9.0: (0, 360, 0)}, length=9.0, loop=True)
    out["back"] = (bk, [("ring", "root", (0, 18, 0))], an)
    out["extra"] = ([R((-0.5, 33.0, -0.5), (1, 1, 1))], None, None)
    return out


# =================================================================================================================================
# SANDSTONE: stacked strata slabs, a dune collar, a monolith behind the back (the block of the Variable Hardness Prison), a halo of slabs
# =================================================================================================================================
def sandstone_set():
    rng = np.random.default_rng(1207)
    G = (255, 224, 140)
    out = {}
    S_ = lambda *a, **k: spec(*a, mat="sandstone", **k)
    D_ = lambda *a, **k: spec(*a, mat="sand_dark", **k)
    # helm: three slabs, the middle one a brim; a stepped crest and a neck flap of strata
    h = [S_((-4.7, 30.2, -4.7), (9.4, 2, 9.4), infl=0.3), S_((-5.4, 28.4, -5.4), (10.8, 1.8, 10.8), infl=0.1, tone=0.06),
         S_((-4.9, 26.6, -4.9), (9.8, 1.8, 2.0), infl=0.2), S_((-5.0, 24.6, -4.8), (1.8, 2.8, 9.6), infl=0.2), S_((3.2, 24.6, -4.8), (1.8, 2.8, 9.6), infl=0.2),
         S_((-4.8, 24.2, 4.1), (9.6, 5.6, 1.6), infl=0.1), D_((-1.0, 32.2, -3.4), (2, 1.6, 6.8)), D_((-0.8, 33.6, -2.2), (1.6, 1.4, 4.4)), S_((-0.6, 34.8, -1.2), (1.2, 1.2, 2.4))]
    out["helm"] = (h, None, None)
    # chest: five strata plates from collarbone to waist, each a little different in width; the hourglass in the middle
    c = []
    for i, (y, w) in enumerate(((21.8, 10.2), (19.4, 9.4), (17.0, 9.0), (14.6, 9.4), (12.4, 8.6))):
        c.append((S_ if i % 2 == 0 else D_)((-w / 2, y, -3.2), (w, 2.2, 1.4), infl=0.2, bands=2))
        c.append((S_ if i % 2 else D_)((-w / 2 + 0.4, y, 1.9), (w - 0.8, 2.2, 1.4), infl=0.2, bands=2))
    c += [spec((-1.2, 16.6, -4.2), (2.4, 4.4, 0.9), mat="core", pal=PAL["sandstone"], glow=G), spec((-0.8, 17.0, -4.7), (1.6, 3.6, 0.5), mat="sandstone", shine=1, glow=G, tone=0.1)]
    out["chest"] = (c, None, None)
    b = [S_((-4.8, 10.6, -2.8), (9.6, 2, 5.6), infl=0.2)]
    for i, x in enumerate((-4.6, -2.3, 0.0, 2.3)):
        b.append((S_ if i % 2 else D_)((x, 5.6 - (i % 2) * 1.4, -3.8), (2.3, 5.0 + (i % 2) * 1.4, 1.2), rot=(-5, 0, 0), pv=(x + 1.1, 10.6, -3.2), infl=0.1, bands=3))
    b += [S_((-6.2, 6.2, -2.2), (1.6, 4.6, 4.4), infl=0.1), S_((4.6, 6.2, -2.2), (1.6, 4.6, 4.4), infl=0.1)]
    out["belt"] = (b, None, None)
    # mantle: the dune collar (two flaring rings) and strata drape down the back
    m = [S_((-6.2, 23.4, -4.0), (12.4, 1.6, 8.0), infl=0.1), S_((-7.4, 22.0, -4.6), (14.8, 1.4, 9.2), infl=0.1, tone=0.06)]
    for i, (y, w) in enumerate(((20.4, 9.4), (17.8, 8.6), (15.2, 7.8), (12.6, 6.8))):
        m.append((S_ if i % 2 == 0 else D_)((-w / 2, y, 2.4), (w, 2.4, 1.6), infl=0.1, bands=2))
    out["mantle"] = (m, None, None)
    # pauldron: three overlapping slabs and a spill of sand grains
    p = [S_((-9.6, 23.4, -3.4), (5.6, 1.8, 6.8), infl=0.1), D_((-9.2, 21.6, -3.2), (5.2, 1.8, 6.4), infl=0.1), S_((-8.8, 25.1, -3.0), (4.4, 1.6, 6.0), infl=0.1, tone=0.06),
         D_((-9.6, 19.8, -2.8), (1.6, 2.4, 5.6), infl=0.1)] + [S_((-9.8 + (i % 3) * 0.8, 17.2 - i * 0.2, -1.8 + i * 1.1), (1, 1, 1), infl=0.0) for i in range(3)]
    pair(out, "pauldron", p)
    v = []
    for i, y in enumerate((13.0, 14.9, 16.8)):
        v.append((S_ if i != 1 else D_)((-8.7, y, -2.7), (5.2, 1.7, 5.4), infl=0.1, bands=2, shine=1, glow=G))
    v.append(S_((-9.4, 13.4, -1.6), (1.4, 5.0, 3.2), infl=0.1))
    pair(out, "vambrace", v)
    g = [S_((-9.0, 9.4, -3.4), (5.8, 3.0, 6.8), infl=0.1), D_((-9.4, 6.8, -3.0), (5.8, 2.6, 6.0), infl=0.1), S_((-8.6, 12.0, -3.2), (4.8, 1.4, 6.4), infl=0.1),
         S_((-9.2, 5.2, -4.2), (1.6, 1.6, 1.6)), S_((-6.8, 5.2, -4.2), (1.6, 1.6, 1.6))]
    pair(out, "gauntlet", g)
    gr = [S_((-4.8, 8.4, -3.0), (5.0, 2.0, 5.8), infl=0.1), D_((-4.8, 6.2, -3.0), (5.0, 2.0, 5.8), infl=0.1), S_((-4.8, 4.0, -3.0), (5.0, 2.0, 5.8), infl=0.1),
          D_((-4.8, 1.8, -3.0), (5.0, 2.0, 5.8), infl=0.1), S_((-5.0, 0.0, -4.4), (5.2, 2.0, 7.6), infl=0.1), S_((-4.6, 10.2, -3.8), (4.2, 2.0, 2.0), rot=(-12, 0, 0), pv=(-2.5, 10.4, -3.0), infl=0.1)]
    pair(out, "greave", gr)
    # back: the monolith with the hourglass, two slabs orbiting it (animated)
    bk = [S_((-4.5, 8.0, 7.0), (9, 22, 3), bone="mono", bands=7), D_((-5.2, 29.6, 6.6), (10.4, 1.6, 3.8), bone="mono"), D_((-5.2, 6.6, 6.6), (10.4, 1.6, 3.8), bone="mono"),
          spec((-1.4, 17.0, 6.2), (2.8, 6, 0.8), mat="core", pal=PAL["sandstone"], glow=G, bone="mono"), spec((-2.6, 21.4, 6.2), (5.2, 1.0, 0.8), mat="core", pal=PAL["sandstone"], glow=G, bone="mono"),
          spec((-2.6, 14.8, 6.2), (5.2, 1.0, 0.8), mat="core", pal=PAL["sandstone"], glow=G, bone="mono")]
    bk += ring(0, 20.0, 9.0, 8.5, 2, 3.0, "sandstone", bone="orbit", phase=0.6, bands=2)
    an = Anim()
    wave(an, "idle", "mono", 7.0, steps=14, z=S(1.5, 0.25, 1), kind="rot")                    # the monolith sways and hovers; the slabs circle it (one 7 s loop)
    wave(an, "idle", "mono", 7.0, steps=14, y=S(1.2, 0, 1), kind="pos")
    keys(an, "idle", "orbit", "rot", {0.0: (0, 0, 0), 3.5: (0, 180, 0), 7.0: (0, 360, 0)}, length=7.0, loop=True)
    out["back"] = (bk, [("mono", "root", (0, 18, 8.5)), ("orbit", "root", (0, 20, 9.0))], an)
    # extra: a halo of eight slabs turning over the head
    ex = ring(0, 37.0, 0, 6.6, 8, (2.6, 1.2, 1.6), "sandstone", bone="halo", bands=2)
    out["extra"] = (ex, [("halo", "root", (0, 37.0, 0))], spin("idle", "halo", 10.0))
    return out


# =================================================================================================================================
# RED OCHRE: smooth fired clay with painted bands and glyphs, a horned mask, a plume down the back, a sun disc behind
# =================================================================================================================================
def red_ochre_set():
    rng = np.random.default_rng(1309)
    G = (255, 120, 60)
    out = {}
    O = lambda *a, **k: spec(*a, mat="ochre", **k)
    P = lambda *a, **k: spec(*a, mat="ochre_pale", **k)
    h = [O((-4.6, 28.6, -4.6), (9.2, 3.2, 9.2), infl=0.3), O((-4.9, 26.8, -4.9), (9.8, 1.8, 9.8), infl=0.2, glyph=1, glow=G),
         O((-4.9, 24.4, -4.6), (1.8, 2.6, 5.2), infl=0.2), O((3.1, 24.4, -4.6), (1.8, 2.6, 5.2), infl=0.2), O((-4.8, 24.2, 4.2), (9.6, 5.0, 1.4), infl=0.2),
         P((-1.4, 26.2, -5.4), (2.8, 2.2, 1.0), infl=0.0, glyph=1, glow=G)]
    h += [prism((-3.6, 31.4, 0), (-1.0, 0.9, -0.1), 6.0, 1.8, "ochre", glyph=0)] + [prism((3.6, 31.4, 0), (1.0, 0.9, -0.1), 6.0, 1.8, "ochre")]      # the two horns
    h += [prism((-6.2, 34.8, -0.6), (-0.4, 1.0, -0.4), 3.0, 1.2, "ochre_pale"), prism((6.2, 34.8, -0.6), (0.4, 1.0, -0.4), 3.0, 1.2, "ochre_pale")]
    out["helm"] = (h, None, None)
    c = [O((-4.6, 12.2, -3.3), (9.2, 11.6, 1.6), infl=0.2), P((-4.9, 21.4, -3.5), (9.8, 1.2, 1.8), infl=0.1), P((-4.7, 15.2, -3.5), (9.4, 1.0, 1.8), infl=0.1),
         P((-4.5, 12.2, -3.5), (9.0, 1.0, 1.8), infl=0.1), spec((-2.4, 16.6, -4.1), (4.8, 4.6, 0.8), mat="ochre", glyph=1, glow=G, paint=False),
         O((-4.6, 12.2, 1.8), (9.2, 11.6, 1.6), infl=0.2), P((-4.8, 20.4, 3.2), (9.6, 1.0, 0.8), infl=0.0)]
    c += [spec((-0.5 + (i - 2) * 1.5, 11.4 - (i % 2) * 1.2, -3.4), (0.9, 1.4 + (i % 2), 0.9), mat="ochre", paint=False, tone=0.05) for i in range(5)]        # pigment drips
    out["chest"] = (c, None, None)
    b = [O((-4.8, 10.6, -2.8), (9.6, 1.8, 5.6), infl=0.2), P((-4.8, 11.2, -3.0), (9.6, 0.8, 6.0), infl=0.1)]
    for i in range(7):
        x = -4.4 + i * 1.35
        b.append(O((x, 6.0 - (i % 2) * 1.4, -3.6), (1.1, 4.6 + (i % 2) * 1.4, 1.1), infl=0.0, paint=False, tone=0.04 * (i % 3)))
    out["belt"] = (b, None, None)
    # mantle: a plume of clay feathers fanning down the back from the neck
    m = [O((-4.6, 21.6, 2.2), (9.2, 3.0, 1.8), infl=0.1)]
    for i in range(9):
        a = math.radians(-75 + i * 150 / 8)
        d = (math.sin(a), -math.cos(a) * 0.9, 0.5)                    # down and outward, away from the back
        m.append(prism((math.sin(a) * 2.0, 22.4 - math.cos(a) * 0.5, 3.6), d, 9.0 - 3.0 * abs(math.sin(a)), 1.6, "ochre" if i % 2 else "ochre_pale"))
    out["mantle"] = (m, None, None)
    p = [O((-9.4, 22.6, -3.2), (5.4, 2.2, 6.4), infl=0.1), O((-9.0, 24.6, -2.6), (4.4, 1.6, 5.2), infl=0.1), P((-9.6, 21.2, -3.0), (5.6, 1.0, 6.0), infl=0.0)]
    p += [prism((-8.0 + (i - 1) * 0.0, 25.8, -2.0 + i * 2.0), (-0.5, 1.0, 0.0), 3.0, 1.2, "ochre") for i in range(3)]
    p += [O((-9.8, 18.8, -2.6), (1.8, 3.0, 5.2), infl=0.1)]
    pair(out, "pauldron", p)
    v = [O((-8.7, 13.0, -2.7), (5.2, 5.8, 5.4), infl=0.1), P((-8.9, 14.2, -2.9), (5.6, 0.9, 5.8), infl=0.0), P((-8.9, 16.6, -2.9), (5.6, 0.9, 5.8), infl=0.0),
         O((-9.5, 13.6, -1.8), (1.4, 4.6, 3.6), infl=0.1)]
    pair(out, "vambrace", v)
    g = [O((-9.2, 8.4, -3.6), (6.0, 4.8, 7.2), infl=0.1, glyph=1, glow=G), P((-9.4, 11.6, -3.6), (6.4, 1.0, 7.2), infl=0.0)]
    g += [O((-9.0 + i * 1.7, 6.4, -4.4), (1.5, 2.4, 1.5)) for i in range(3)]
    pair(out, "gauntlet", g)
    gr = [O((-4.8, 1.6, -3.0), (5.0, 8.4, 5.8), infl=0.1), P((-4.9, 7.2, -3.1), (5.2, 1.0, 6.0), infl=0.0), P((-4.9, 4.6, -3.1), (5.2, 1.0, 6.0), infl=0.0),
          O((-5.0, 0.0, -4.4), (5.2, 2.0, 7.6), infl=0.1), O((-4.6, 9.4, -3.8), (4.2, 2.2, 2.2), infl=0.1)]
    pair(out, "greave", gr)
    # back: the sun disc: an eight-pointed star made of two turned squares, a ring of rays, a glyph core; turns slowly
    bk = [O((-6.5, 11.5, 7.4), (13, 13, 2.2), bone="sun", glyph=1, glow=G), O((-6.5, 11.5, 7.4), (13, 13, 2.2), rot=(0, 0, 45), pv=(0, 18, 8.5), bone="sun"),
          P((-3.2, 14.8, 6.8), (6.4, 6.4, 1.0), bone="sun", glyph=1, glow=G), spec((-1.2, 16.8, 6.4), (2.4, 2.4, 0.8), mat="core", glow=G, bone="sun", pal=PAL["ochre"])]
    for i in range(8):
        a = i * 45.0
        dx, dy = -math.sin(math.radians(a)), math.cos(math.radians(a))
        bk.append(prism((-dx * 9.0, 18.0 + dy * 9.0, 8.4), (-dx, dy, 0.0), 4.0, 1.6, "ochre_pale", bone="sun"))
    an = Anim()
    keys(an, "idle", "sun", "rot", {0.0: (0, 0, 0), 6.0: (0, 0, 180), 12.0: (0, 0, 360)}, length=12.0, loop=True)
    out["back"] = (bk, [("sun", "root", (0, 18, 8.5))], an)
    ex = ring(0, 36.0, 0, 6.4, 5, (2.4, 2.4, 2.4), "ochre", bone="beads", paint=False, glyph=1, glow=G)
    out["extra"] = (ex, [("beads", "root", (0, 36, 0))], spin("idle", "beads", 8.0))
    return out


# =================================================================================================================================
# STONE: dressed masonry, a crenellated crown, runes; two floating tablets behind the back, a rune ring over the head
# =================================================================================================================================
def stone_set():
    G = (150, 230, 255)
    out = {}
    T = lambda *a, **k: spec(*a, mat="stone", **k)
    h = [T((-4.8, 28.4, -4.8), (9.6, 3.6, 9.6), infl=0.3, course=4), T((-5.1, 26.4, -5.1), (10.2, 2.0, 2.2), infl=0.1, smooth=1), T((-5.2, 24.4, -4.8), (2.0, 2.6, 9.6), infl=0.2, course=4),
         T((3.2, 24.4, -4.8), (2.0, 2.6, 9.6), infl=0.2, course=4), T((-4.9, 24.2, 4.2), (9.8, 6.0, 1.6), infl=0.2, rune=1, glow=G),
         T((-1.0, 26.4, -5.7), (2.0, 1.6, 0.8), smooth=1, rune=1, glow=G)]
    for x in (-4.6, -1.6, 1.4):
        for z in (-4.6, -1.6, 1.4):
            if (x, z) in ((-1.6, -1.6),): continue
            h.append(T((x, 32.0, z), (3.2, 2.4, 3.2), infl=-0.2, course=3))
    out["helm"] = (h, None, None)
    c = [T((-4.6, 12.2, -3.4), (9.2, 11.8, 1.6), infl=0.2, course=5), T((-5.1, 21.6, -3.7), (10.2, 2.4, 2.0), infl=0.1, smooth=1),
         spec((-2.6, 15.6, -4.6), (5.2, 5.2, 1.0), mat="stone", smooth=1, rune=1, glow=G), T((-4.6, 12.2, 1.8), (9.2, 11.8, 1.6), infl=0.2, rune=1, glow=G),
         T((-4.0, 10.8, -3.6), (8.0, 1.6, 1.2), smooth=1)]
    out["chest"] = (c, None, None)
    b = [T((-4.8, 10.4, -2.9), (9.6, 2.0, 5.8), infl=0.2, smooth=1)]
    for i, x in enumerate((-4.6, -1.5, 1.6)):
        b.append(T((x, 5.4 - (i == 1) * 1.2, -3.8), (3.0, 5.0 + (i == 1) * 1.2, 1.4), infl=0.0, rot=(-4, 0, 0), pv=(x + 1.5, 10.4, -3.2), course=4))
    b += [T((-6.2, 5.8, -2.2), (1.6, 4.8, 4.4), infl=0.1), T((4.6, 5.8, -2.2), (1.6, 4.8, 4.4), infl=0.1)]
    out["belt"] = (b, None, None)
    m = [T((-6.4, 22.6, -4.2), (12.8, 2.2, 8.4), infl=0.1, smooth=1), T((-4.4, 11.8, 2.8), (8.8, 10.8, 1.6), infl=0.1, rune=1, glow=G, course=5)]
    m += [T((-6.4 + i * 3.6, 24.8, -4.2), (2.4, 1.6, 8.4), infl=0.0) for i in range(4)]
    out["mantle"] = (m, None, None)
    p = [T((-9.6, 22.4, -3.6), (5.8, 2.4, 7.2), infl=0.1, smooth=1), T((-9.2, 24.8, -3.0), (4.6, 2.0, 6.0), infl=0.1, course=3, rune=1, glow=G), T((-9.8, 19.6, -3.2), (1.8, 3.0, 6.4), infl=0.1, course=4)]
    p += [T((-9.2 + i * 1.9, 26.8, -3.0), (1.4, 1.4, 6.0)) for i in range(3)]
    pair(out, "pauldron", p)
    v = [T((-8.8, 13.0, -2.8), (5.4, 5.8, 5.6), infl=0.1, course=3), T((-9.1, 15.8, -3.1), (6.0, 1.2, 6.2), infl=0.0, smooth=1), T((-9.1, 12.8, -3.1), (6.0, 1.2, 6.2), infl=0.0, smooth=1),
         spec((-9.8, 14.0, -1.6), (1.0, 3.6, 3.2), mat="stone", smooth=1, rune=1, glow=G)]
    pair(out, "vambrace", v)
    g = [T((-9.4, 8.0, -3.8), (6.2, 5.0, 7.6), infl=0.1, course=3, rune=1, glow=G), T((-9.6, 11.8, -3.9), (6.6, 1.4, 7.8), infl=0.0, smooth=1)]
    g += [T((-9.0 + i * 1.8, 6.2, -4.6), (1.6, 1.8, 1.6)) for i in range(3)]
    pair(out, "gauntlet", g)
    gr = [T((-4.8, 1.8, -3.1), (5.0, 7.6, 6.0), infl=0.1, course=4), T((-4.9, 9.2, -3.9), (5.2, 2.6, 2.6), infl=0.1, smooth=1), T((-5.0, 0.0, -4.4), (5.2, 2.2, 7.6), infl=0.1, smooth=1)]
    pair(out, "greave", gr)
    # back: two tablets standing behind the shoulders, tilted outward, runes lit; a small block circling each
    bk = [T((-9.2, 12.0, 6.4), (5, 15, 2), rot=(0, -14, 8), pv=(-6.7, 12, 7.4), bone="tabl", infl=0.0, rune=1, glow=G, smooth=1),
          T((4.2, 12.0, 6.4), (5, 15, 2), rot=(0, 14, -8), pv=(6.7, 12, 7.4), bone="tabr", infl=0.0, rune=1, glow=G, smooth=1),
          T((-8.8, 26.4, 6.2), (4, 1.6, 2.4), rot=(0, -14, 8), pv=(-6.7, 27, 7.4), bone="tabl", infl=0.0), T((4.8, 26.4, 6.2), (4, 1.6, 2.4), rot=(0, 14, -8), pv=(6.7, 27, 7.4), bone="tabr", infl=0.0)]
    an = Anim()
    wave(an, "idle", "tabl", 6.0, steps=12, y=S(1.4, 0.0), kind="pos")
    wave(an, "idle", "tabr", 6.0, steps=12, y=S(1.4, 0.5), kind="pos")
    out["back"] = (bk, [("tabl", "root", (-6.7, 18, 7.4)), ("tabr", "root", (6.7, 18, 7.4))], an)
    ex = ring(0, 36.4, 0, 6.8, 8, (3.0, 1.0, 2.0), "stone", bone="runes", smooth=1, rune=1, glow=G)
    out["extra"] = (ex, [("runes", "root", (0, 36.4, 0))], spin("idle", "runes", 12.0))
    return out


# =================================================================================================================================
# EARTH: packed soil with strata, grass tufts and roots; a ring of clods and a seed-core behind the back, antlers of root over the head
# =================================================================================================================================
def earth_set():
    rng = np.random.default_rng(1411)
    G = (210, 230, 90)
    out = {}
    E = lambda *a, **k: spec(*a, mat="earth", **k)
    h = [E((-4.7, 28.6, -4.7), (9.4, 3.2, 9.4), infl=0.3, grass=1.0), E((-5.0, 26.6, -5.0), (10.0, 2.0, 2.4), infl=0.2, grass_side=0.5), E((-5.1, 24.4, -4.6), (2.0, 2.6, 5.4), infl=0.2),
         E((3.1, 24.4, -4.6), (2.0, 2.6, 5.4), infl=0.2), E((-4.8, 24.2, 4.2), (9.6, 7.0, 1.6), infl=0.2, grass_side=0.4)]
    for i in range(5):                                                           # grass tufts on the crown
        h.append(prism((-3.2 + i * 1.6, 31.8, -1.6 + (i % 2) * 2.4), ((i - 2) * 0.25, 1.0, 0.0), 2.6 + (i % 3) * 0.6, 0.8, "grass"))
    h += [prism((-4.4, 31.6, 0.0), (-0.9, 1.0, 0.0), 4.0, 1.2, "earth"), prism((4.4, 31.6, 0.0), (0.9, 1.0, 0.0), 4.0, 1.2, "earth")]     # root horns
    out["helm"] = (h, None, None)
    c = [E((-4.6, 18.2, -3.3), (9.2, 5.4, 1.8), infl=0.2, grass_side=0.2), E((-5.0, 21.6, -3.5), (10.0, 2.6, 2.4), infl=0.2, grass=1.0), E((-4.3, 12.4, -3.0), (8.6, 5.6, 1.6), infl=0.2),
         E((-4.7, 12.2, 1.8), (9.4, 11.0, 1.8), infl=0.2, grass_side=0.4), spec((-1.2, 17.4, -4.1), (2.4, 2.8, 0.8), mat="earth", shine=1, glow=G)]
    c += jagspecs(rng, -2.6, 20.0, -3.5, 2.0, 2.0, 0.5, 3, 1.6, 2.4, mat="earth") + [prism((x, 11.8, -3.2), (0.0, -1.0, -0.2), 4.0, 0.8, "earth") for x in (-3.0, -1.0, 1.0, 3.0)]
    out["chest"] = (c, None, None)
    b = [E((-4.8, 10.6, -2.8), (9.6, 1.8, 5.6), infl=0.2, grass_side=0.5)]
    for i in range(6):
        b.append(prism((-4.0 + i * 1.6, 10.4, -3.2 + (i % 2) * 0.4), (0.0, -1.0, -0.15), 5.0 + (i % 2) * 2.0, 1.0, "earth" if i % 2 else "grass"))
    b += [E((-6.2, 6.4, -2.2), (1.6, 4.4, 4.4), infl=0.1), E((4.6, 6.4, -2.2), (1.6, 4.4, 4.4), infl=0.1)]
    out["belt"] = (b, None, None)
    m = [E((-6.0, 22.8, -4.0), (12.0, 2.0, 8.0), infl=0.1, grass=1.0), E((-4.6, 11.8, 2.6), (9.2, 11.0, 1.6), infl=0.1, grass_side=0.7)]
    m += [prism((-4.0 + i * 1.3, 22.6, 4.0), ((i - 3) * 0.12, -1.0, 0.25), 8.0 - abs(i - 3) * 1.1, 1.0, "grass" if i % 2 else "earth") for i in range(7)]
    out["mantle"] = (m, None, None)
    p = [E((-9.6, 22.4, -3.4), (5.8, 2.4, 6.8), infl=0.1), E((-9.0, 24.6, -2.8), (4.6, 2.2, 5.6), infl=0.1, grass=1.0)]
    p += [prism((-8.4 + (i % 2) * 2.0, 26.6, -1.6 + i * 1.4), ((i - 1) * 0.2, 1.0, 0.0), 2.8, 0.9, "grass") for i in range(3)] + jagspecs(rng, -9.6, 20.2, 0.0, 0.5, 1.0, 2.0, 2, 1.6, 2.6, mat="earth")
    pair(out, "pauldron", p)
    v = [E((-8.7, 12.8, -2.7), (5.2, 6.2, 5.4), infl=0.1, grass_side=0.3), E((-9.0, 18.4, -2.9), (5.6, 1.6, 5.8), infl=0.0, grass=1.0)]
    v += [prism((-8.6, 17.6 - i * 2.0, -3.0 + i * 0.5), (-0.3, 0.2, -1.0), 3.0, 0.8, "earth") for i in range(3)]
    pair(out, "vambrace", v)
    g = [E((-9.4, 8.2, -3.8), (6.2, 5.2, 7.6), infl=0.1)] + jagspecs(rng, -6.4, 8.4, -3.6, 3.2, 2.2, 3.2, 4, 2, 3.4, mat="earth") + [E((-9.2, 11.8, -3.6), (5.8, 1.4, 7.0), infl=0.0, grass=1.0)]
    pair(out, "gauntlet", g)
    gr = [E((-4.8, 1.8, -3.0), (5.0, 7.0, 5.8), infl=0.1, grass_side=0.2), E((-5.0, 8.4, -3.2), (5.4, 2.4, 6.2), infl=0.1, grass=1.0), E((-5.0, 0.0, -4.4), (5.2, 2.2, 7.6), infl=0.1)]
    gr += [prism((-4.0 + i * 1.6, 10.6, -3.0 + (i % 2) * 2.0), (0.0, 1.0, 0.0), 2.4, 0.8, "grass") for i in range(3)]
    pair(out, "greave", gr)
    # back: the seed-core (a tilted lump with a gold glow) in two crossed rings of clods (animated)
    core = [E((-2.5, 15.5, 6.0), (5, 5, 5), bone="core", shine=1, glow=G, rot=(20, 30, 10), pv=(0, 18, 8.5)), spec((-1.2, 16.8, 6.2), (2.4, 2.4, 2.4), mat="core", glow=G, bone="core", pal=PAL["earth"])]
    r1 = ring(0, 18.0, 8.5, 9.0, 5, 3.0, "earth", bone="r1", grass_side=0.5)
    r2 = ring(0, 18.0, 8.5, 6.4, 4, 2.4, "earth", bone="r2", phase=0.5)
    an = Anim()
    keys(an, "idle", "r1", "rot", {0.0: (0, 0, 0), 4.0: (0, 180, 0), 8.0: (0, 360, 0)}, length=8.0, loop=True)
    keys(an, "idle", "r2", "rot", {0.0: (0, 360, 0), 4.0: (0, 180, 0), 8.0: (0, 0, 0)}, length=8.0, loop=True)
    wave(an, "idle", "core", 8.0, steps=8, y=S(1.0), kind="pos")
    out["back"] = (core + r1 + r2, [("core", "root", (0, 18, 8.5)), ("r1", "root", (0, 18, 8.5)), ("r2", "root", (0, 18, 8.5))], an)
    ex = []
    for sd in (-1, 1):                                                           # antlers: curling roots with gold buds
        pts = [(sd * 3.2, 33.0, 0.0), (sd * 5.0, 36.0, -0.4), (sd * 6.2, 39.0, -0.8)]
        ex.append(prism(pts[0], (sd * 0.5, 1.0, -0.1), 3.6, 1.2, "earth", bone="ant"))
        ex.append(prism(pts[1], (sd * 0.3, 1.0, -0.1), 3.2, 1.0, "earth", bone="ant"))
        ex.append(spec((pts[2][0] - 0.8, pts[2][1] + 0.4, pts[2][2] - 0.8), (1.6, 1.6, 1.6), mat="earth", shine=1, glow=G, bone="ant"))
        ex.append(prism((sd * 4.6, 35.0, -0.4), (sd * 1.0, 0.7, 0.0), 3.0, 0.9, "grass", bone="ant"))
    an2 = Anim()
    wave(an2, "idle", "ant", 4.0, steps=8, z=S(2.0), kind="rot")
    out["extra"] = (ex, [("ant", "root", (0, 33, 0))], an2)
    return out


# =================================================================================================================================
# MINERAL: crystal clusters grown out of ore matrix (quartz blue, amethyst, malachite, pyrite); a geode chest and a fan of crystal behind
# =================================================================================================================================
def mineral_set():
    rng = np.random.default_rng(1511)
    G = (120, 220, 255)
    out = {}
    M = lambda *a, **k: spec(*a, mat="matrix", **k)
    mats = ["crystal", "amethyst", "crystal", "pyrite", "malachite"]
    def cr(i, *a, **k): return prism(*a, mat=mats[i % len(mats)], **k)
    h = [M((-4.7, 28.4, -4.7), (9.4, 3.4, 9.4), infl=0.3), M((-5.0, 26.6, -5.0), (10.0, 1.8, 2.2), infl=0.2), M((-5.1, 24.4, -4.6), (2.0, 2.6, 5.2), infl=0.2), M((3.1, 24.4, -4.6), (2.0, 2.6, 5.2), infl=0.2),
         M((-4.8, 24.2, 4.2), (9.6, 7.6, 1.6), infl=0.2)]
    for i, a in enumerate((-50, -25, 0, 25, 50)):                                # the crystal tiara, fanned across the brow
        ra = math.radians(a)
        h.append(cr(i, (math.sin(ra) * 3.6, 31.6, -2.6 + abs(a) * 0.02), (math.sin(ra) * 0.5, 1.0, -0.15), 5.6 - abs(a) * 0.05, 1.6, glow=G))
    for i, z in enumerate((0.0, 2.4, 4.4)):
        h.append(cr(i + 1, (0.0, 31.4, z), (0.0, 0.9, 0.35), 4.4, 1.4, glow=(200, 150, 255)))
    out["helm"] = (h, None, None)
    c = [M((-4.6, 12.2, -3.3), (9.2, 11.8, 1.6), infl=0.2), M((-4.7, 12.2, 1.8), (9.2, 11.8, 1.6), infl=0.2), M((-5.0, 21.4, -3.5), (10.0, 2.4, 2.2), infl=0.1),
         spec((-3.0, 15.4, -4.4), (6, 5.6, 1.2), mat="matrix", tone=-0.2)]
    c += [spec((-2.4 + (i % 3) * 2.2, 16.0 + (i // 3) * 2.4, -5.0), (1.8, 1.8, 2.0), mat="amethyst" if i % 2 else "crystal", glow=(220, 170, 255) if i % 2 else G, rot=(-18 + i * 4, i * 10 - 25, 0),
               pv=(-1.5 + (i % 3) * 2.2, 17.0 + (i // 3) * 2.4, -4.4)) for i in range(6)]                                                 # the geode: a nest of crystals in the opening
    for i in range(4):
        c.append(cr(i, (-3.6 + i * 2.4, 11.8, -3.2), (0.0, -1.0, -0.2), 3.0 + (i % 2), 1.4))
    out["chest"] = (c, None, None)
    b = [M((-4.8, 10.6, -2.8), (9.6, 1.8, 5.6), infl=0.2)]
    for i in range(8):
        b.append(cr(i, (-4.2 + i * 1.2, 10.6, -3.4 + (i % 2) * 0.5), (0.0, -1.0, -0.12), 3.0 + (i % 3), 1.2))
    out["belt"] = (b, None, None)
    m = [M((-5.6, 22.6, -3.8), (11.2, 2.2, 7.6), infl=0.1), M((-4.6, 14.6, 2.6), (9.2, 8.2, 1.6), infl=0.1)]
    m += fan((0, 21.0, 3.6), 7, -70, 70, "xy", 9.0, 1.7, "crystal", lerp_len=0.65, tilt=(0.0, 0.5), glow=G)
    m += fan((0, 17.0, 3.8), 4, -50, 50, "xy", 5.8, 1.4, "amethyst", lerp_len=0.7, tilt=(0.0, 0.5), glow=(220, 170, 255))
    out["mantle"] = (m, None, None)
    p = [M((-9.6, 22.4, -3.4), (5.8, 2.4, 6.8), infl=0.1)]
    base = (-7.2, 24.6, 0.0)
    for i, (dx, dy, dz, L, t) in enumerate(((-0.6, 1.0, 0.0, 7.0, 2.2), (-1.0, 0.8, -0.5, 5.0, 1.8), (-0.2, 1.0, 0.8, 5.4, 1.8), (-1.2, 0.5, 0.4, 4.2, 1.6), (0.3, 1.0, -0.4, 4.0, 1.4))):
        p.append(cr(i, (base[0] + (i % 2) * 0.8, base[1], base[2] - 1.5 + i * 0.8), (dx, dy, dz), L, t, glow=G if i % 2 == 0 else (200, 150, 255)))
    p += [cr(1, (-9.6, 22.0, -1.0), (-1.0, -0.2, 0.0), 3.4, 1.6), cr(3, (-9.6, 21.6, 1.2), (-1.0, -0.3, 0.2), 3.0, 1.4)]
    pair(out, "pauldron", p)
    v = [M((-8.7, 12.8, -2.7), (5.2, 6.2, 5.4), infl=0.1), M((-9.0, 18.4, -2.9), (5.6, 1.6, 5.8), infl=0.0)]
    v += [cr(i, (-8.8 + (i % 2) * 0.6, 13.6 + i * 1.5, 2.6), (0.0, 0.2, 1.0), 4.0 - 0.5 * i, 1.3, glow=G) for i in range(4)]       # fins sweeping back
    v += [cr(i + 1, (-9.6, 14.6 + i * 2.0, -1.0), (-1.0, 0.3, 0.0), 2.4, 1.2) for i in range(2)]
    pair(out, "vambrace", v)
    g = [M((-9.2, 8.4, -3.6), (5.8, 4.8, 7.0), infl=0.1)] + [cr(i, (-8.4 + i * 1.6, 8.6, -4.0), (0.0, -0.1, -1.0), 3.6 - (i % 2) * 0.8, 1.5, glow=G) for i in range(3)]
    g += [M((-9.0, 11.8, -3.4), (5.4, 1.4, 6.6), infl=0.0)]
    pair(out, "gauntlet", g)
    gr = [M((-4.8, 1.8, -3.0), (5.0, 7.0, 5.8), infl=0.1), M((-5.0, 8.8, -3.4), (5.2, 2.6, 2.4), infl=0.1), M((-5.0, 0.0, -4.4), (5.2, 2.2, 7.6), infl=0.1)]
    gr += [cr(i, (-4.2 + i * 1.8, 9.6, -3.6), (0.0, 0.5, -1.0), 3.0, 1.4, glow=G) for i in range(3)] + [cr(2, (-4.6, 3.6, 2.6), (-0.4, 0.2, 1.0), 3.0, 1.4)]
    pair(out, "greave", gr)
    # back: two crystal wings, each a fan of long prisms (animated: they open and close a hair)
    bk = [M((-3.0, 14.0, 5.6), (6, 7, 2.4), bone="wing_m")]
    for sd, bone in ((-1, "wing_r"), (1, "wing_l")):
        for i in range(6):
            ang = 25 + i * 13
            d = (sd * math.sin(math.radians(ang)), math.cos(math.radians(ang)) * 0.95, 0.35 + 0.05 * i)
            bk.append(cr(i, (sd * 2.4, 16.0 + (i % 2) * 0.8, 7.2 + (i % 3) * 0.3), d, 14.0 - i * 1.3, 2.0 - 0.1 * i, bone=bone, glow=G if i % 2 == 0 else (200, 150, 255)))
    an = Anim()
    wave(an, "idle", "wing_r", 5.0, steps=10, z=S(3.0, 0.0), kind="rot")
    wave(an, "idle", "wing_l", 5.0, steps=10, z=S(-3.0, 0.0), kind="rot")
    out["back"] = (bk, [("wing_m", "root", (0, 16, 7)), ("wing_r", "root", (-2.4, 16, 7.4)), ("wing_l", "root", (2.4, 16, 7.4))], an)
    ex = []
    for i in range(6):
        a = i * math.tau / 6
        ex.append(prism((math.sin(a) * 6.4, 35.4 + (i % 2) * 1.0, math.cos(a) * 6.4), (math.sin(a) * 0.1, 1.0, math.cos(a) * 0.1), 4.4, 1.5, mats[i % len(mats)], bone="shards", glow=G))
    out["extra"] = (ex, [("shards", "root", (0, 36, 0))], spin("idle", "shards", 9.0))
    return out


SETS = {"rock": rock_set, "sandstone": sandstone_set, "red_ochre": red_ochre_set, "stone": stone_set, "earth": earth_set, "mineral": mineral_set}


def main():
    wanted = sys.argv[1:] or list(SETS)
    for key in wanted:
        pieces = SETS[key]()
        seed = 100 + sum(ord(ch) for ch in key)
        for i, name in enumerate(PIECES):
            names = [name + "_r", name + "_l"] if name in PAIRED else [name]
            for nm in names:
                specs, bones, anim = pieces[nm]
                mid, sh = build(key, nm, seed + i * 31 + (7 if nm.endswith("_l") else 0), specs, bones, anim)
                print(mid, sh.m.cube_count(), "cubes", sh.tw, "x", sh.th)


if __name__ == "__main__":
    main()
