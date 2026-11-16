"""More part generators for the Black Clover NPC and Midnight Sun looks (docs/npcs_bc.md).  They only ADD to bcparts / bcparts2 / bcparts3 (nothing there is changed): hair tails, braids, a bun, a
headband, a cap, witch hats, glasses, half-face masks, a bandage wrap, shawls, scarves with ropes, feather panels, a banner, hoods, earrings, necklaces and a forehead stone.

Same conventions as bcparts.py: a `Build` (b), a layer ("main" always / "fine" within the detail distance / "fx" glowing), geo coordinates in pixels (y up, front at -z, +x the model's left,
the head is x -4..4, y 24..32, z -4..4, the body y 12..24, z -2..2).  Every part is cubes or single-face cards, deterministic, and states its quad cost in its doc line.
"""
import math

import numpy as np

import bclib as L
import bcparts as P
from bclib import hsh, rgb, tone
from bcparts import HEAD


def _uid(b, prefix):
    """A bone name that is unique in this build."""
    n = getattr(b, "_npc_uid", 0) + 1
    b._npc_uid = n
    return "%s%d" % (prefix, n)


# ================================================================================================ hair
def hair_tail(b, color, x=0.0, y_top=30.0, z=5.2, length=12.0, w=2.6, count=2, seed=60, sway=3.0, y_split=24.2, z_low=3.4, flare=0.0, head_only=False, layer="fine", prongs=3, tip=0.3):
    """A hanging lock or tail of hair (a ponytail, a twin tail): `count` strands, each two crossed cards (front and side view), the upper part on the head and, below the neck line, the lower part on the
    body so it hangs against the back instead of swinging with the head.  `flare` leans the whole tail outward (degrees, the side of x).  Quads: 4 per strand (8 with a lower part)."""
    geo = b.g(layer)
    s = 1.0 if x >= 0 else -1.0
    for k in range(count):
        xx = x + (k - (count - 1) / 2.0) * w * 0.5
        ln = length * (0.86 + 0.24 * hsh(k, seed))
        up = b.t_hairup(color, seed=seed + k)
        low = b.t_hairlow(color, seed=seed + k, prongs=prongs, tip=tip)
        rot = (0.0, 0.0, s * flare)
        if head_only or y_top - ln > y_split:
            nm = b.sway(layer, HEAD, _uid(b, "ht"), (xx, y_top, z), ax=sway * 0.6, az=sway * 0.5, phase=1.3 * k + seed)
            geo.cube(nm, (xx - w / 2, y_top - ln, z), (w, ln, 0.0), b.t_hairlow(color, seed=seed + k, prongs=prongs, tip=tip), faces=("north",), rot=rot, pivot=(xx, y_top, z))
            geo.cube(nm, (xx, y_top - ln, z - w / 2), (0.0, ln, w), b.t_hairlow(color, seed=seed + k + 3, prongs=prongs, tip=tip), faces=("east",), rot=rot, pivot=(xx, y_top, z))
            continue
        ln_up = y_top - y_split + 1.0
        bu = b.sway(layer, HEAD, _uid(b, "htu"), (xx, y_top, z), ax=sway * 0.4, az=sway * 0.3, phase=1.1 * k + seed)
        geo.cube(bu, (xx - w / 2, y_top - ln_up, z), (w, ln_up, 0.0), up, faces=("north",), rot=rot, pivot=(xx, y_top, z))
        geo.cube(bu, (xx, y_top - ln_up, z - w / 2), (0.0, ln_up, w), up, faces=("east",), rot=rot, pivot=(xx, y_top, z))
        tail = max(2.0, ln - ln_up + 1.0)
        bl = b.sway(layer, "body", _uid(b, "htl"), (xx, y_split + 0.5, z_low), ax=sway, az=sway * 0.7, phase=1.1 * k + seed + 0.6, walk=1.4, run=2.4)
        geo.cube(bl, (xx - w / 2, y_split + 1.0 - tail, z_low), (w, tail, 0.0), low, faces=("north",), pivot=(xx, y_split + 0.5, z_low))
        geo.cube(bl, (xx, y_split + 1.0 - tail, z_low - w / 2), (0.0, tail, w), low, faces=("east",), pivot=(xx, y_split + 0.5, z_low))


def tie(b, x, y, z, color="#c8283a", size=1.5, layer="main", anchor=HEAD):
    """A hair tie or bobble: one small cube."""
    b.g(layer).cube(anchor, (x - size / 2, y - size / 2, z - size / 2), (size, size, size), b.t_flat(color, 8, 8))


def braid(b, color, x=0.0, y_top=30.0, z=4.9, segs=6, seg=2.1, w=1.9, tie_color="#e8b830", seed=70, sway=2.6, flare=0.0, layer="main"):
    """A braid: `segs` small cubes alternately offset, ending in a tie and a tuft, swinging as one (a bone on the head).  Quads: 6 per segment + a tuft card (2)."""
    geo = b.g(layer)
    s = 1.0 if x >= 0 else -1.0
    bone = b.sway(layer, HEAD, _uid(b, "br"), (x, y_top, z), ax=sway, az=sway * 0.8, phase=seed * 0.37, walk=1.2, run=2.0)
    rot = (0.0, 0.0, s * flare)
    for i in range(segs):
        off = 0.35 * (1 if i % 2 == 0 else -1)
        ww = w * (1.0 - 0.06 * i)
        y0 = y_top - (i + 1) * seg + 0.35
        geo.cube(bone, (x + off - ww / 2, y0, z - ww / 2), (ww, seg + 0.1, ww), b.t_mass(color, 16, 16, seed + i % 3), rot=rot, pivot=(x, y_top, z))
    yb = y_top - segs * seg
    geo.cube(bone, (x - 0.8, yb - 0.5, z - 0.8), (1.6, 0.9, 1.6), b.t_flat(tie_color, 8, 8), rot=rot, pivot=(x, y_top, z))
    tuft = b.t_hairlow(color, seed=seed, prongs=2, tip=0.3)
    geo.cube(bone, (x - 1.0, yb - 3.2, z), (2.0, 3.0, 0.0), tuft, faces=("north",), rot=rot, pivot=(x, y_top, z))


def bun(b, color, x=0.0, y=31.5, z=3.2, size=3.4, ribbon=None, layer="main"):
    """A hair bun on the head: a cube with a lit top and a ribbon band round it.  Quads: 6 (+6)."""
    geo = b.g(layer)
    geo.cube(HEAD, (x - size / 2, y - size / 2, z - size / 2), (size, size, size), b.t_mass(color, 16, 16, 90))
    if ribbon:
        geo.cube(HEAD, (x - size / 2 - 0.15, y - 0.4, z - size / 2 - 0.15), (size + 0.3, 0.8, size + 0.3), b.t_flat(ribbon, 8, 8))


def swept_fringe(b, color, side=1, y_top=31.6, n=5, length=5.0, seed=80, layer="fine"):
    """A long fringe swept to one side over the brow (Nozel, Fuegoleon, Klaus): cards leaning across the forehead.  Quads: n."""
    geo = b.g(layer)
    for i in range(n):
        t = (i + 0.5) / n
        x = side * (-2.6 + 5.2 * t)
        ln = length * (0.8 + 0.4 * hsh(i, seed)) * (0.7 + 0.5 * t)
        tile = b.t_strand(color, seed=seed + i, prongs=2, tip=0.3)
        zz = -4.8 - 0.07 * (i % 3)
        geo.card(HEAD, x, y_top, zz, 2.0, ln, tile, rot=(0.0, 0.0, side * (10.0 + 8.0 * t)), pivot=(x, y_top, zz))


# ================================================================================================ headwear
def headband(b, color="#14101a", trim="#e8b830", y=29.7, h=1.5, emblem="diamond", knot=True, tails=2, layer="main"):
    """A headband round the forehead with an emblem plate in front and a knot with two tails behind (Asta).  Quads: ~26."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 16, 16, seed=31, trim=None)
    w = 9.2
    geo.cube(HEAD, (-w / 2, y, -4.0 - 0.75), (w, h, 0.75), cloth, faces=("north", "south", "up", "down"))
    geo.cube(HEAD, (-w / 2, y, 4.0), (w, h, 0.75), cloth)
    for s in (-1, 1):
        geo.cube(HEAD, (s * 4.0 + (0.0 if s > 0 else -0.75), y, -4.0), (0.75, h, 8.0), cloth)
    if emblem:
        geo.cube(HEAD, (-1.5, y - 0.1, -4.9), (3.0, h + 0.2, 0.3), b.t_cloth(color, 16, 16, seed=32, emblem=emblem, emblem_color=trim), faces=("north",))
    if knot:
        geo.cube(HEAD, (-1.0, y + 0.1, 4.7), (2.0, 1.3, 1.1), cloth)
        for k in range(tails):
            bone = b.sway("fine", HEAD, _uid(b, "hb"), (0.0, y + 0.6, 5.2), ax=5.0, az=3.5, phase=1.7 * k, walk=2.0, run=3.0)
            b.g("fine").cube(bone, (-0.9 + (k - 0.5) * 1.6, y - 5.5, 5.2), (1.8, 6.0, 0.0), b.t_cloth(color, 16, 16, seed=33 + k, hem="zigzag"), faces=("north",), pivot=(0.0, y + 0.6, 5.2))


def cap(b, color="#3a3a44", band="#14101a", badge="#e8b830", y=32.0, layer="main"):
    """A peaked cap (Gordon): a crown block, a band, a stiff bill and a badge.  Quads: ~24."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 16, 16, seed=41)
    geo.cube(HEAD, (-4.7, y, -4.7), (9.4, 2.6, 9.4), cloth)
    geo.cube(HEAD, (-4.85, y - 0.1, -4.85), (9.7, 1.0, 9.7), b.t_flat(band, 8, 8))
    geo.cube(HEAD, (-4.2, y + 2.4, -4.2), (8.4, 0.6, 8.4), b.t_cloth(color, 16, 16, seed=42, light=1.3))
    geo.cube(HEAD, (-4.2, y - 0.6, -8.4), (8.4, 0.5, 3.8), b.t_flat(band, 8, 8), rot=(-9.0, 0.0, 0.0), pivot=(0.0, y - 0.3, -4.8))
    geo.cube(HEAD, (-0.9, y + 0.5, -4.95), (1.8, 1.8, 0.3), b.t_cloth(color, 16, 16, seed=43, emblem="diamond", emblem_color=badge), faces=("north",))


def witch_hat(b, color="#8a2a4a", band="#5a2a8a", gem="#e8b830", brim=13.0, cone_h=9.0, y=32.2, droop=22.0, flowers=None, layer="main"):
    """A witch hat: a wide flat brim, a cone of six stacked blocks that bends back at the tip, a band and a pin; `flowers` is a list of colours for a garland round the base (Dorothy).  Quads: ~58."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 32, 32, seed=51, folds=3)
    geo.cube(HEAD, (-brim / 2, y, -brim / 2), (brim, 0.7, brim), cloth, faces=("north", "south", "east", "west", "up", "down"))
    n = 6
    seg = cone_h / n
    for i in range(n):
        w = 7.4 - i * (6.0 / n)
        rot = (-droop * (i / (n - 1.0)) ** 1.5, 0.0, 0.0)
        geo.cube(HEAD, (-w / 2, y + 0.7 + seg * i - 0.1, -w / 2 + 0.2 * i), (w, seg + 0.2, w), b.t_cloth(color, 16, 16, seed=52 + i, folds=2), rot=rot, pivot=(0.0, y + 0.7, 0.0))
    geo.cube(HEAD, (-4.2, y + 0.7, -4.2), (8.4, 1.2, 8.4), b.t_flat(band, 8, 8))
    b.g("fx").cube(HEAD, (-0.8, y + 0.8, -4.55), (1.6, 1.4, 0.5), b.t_gem(gem), faces=("north", "up"))
    if flowers:
        n = 10
        for i in range(n):
            a = i / n * math.tau
            x, z = math.sin(a) * 4.5, -math.cos(a) * 4.5
            geo.cube(HEAD, (x - 0.6, y + 1.6, z - 0.6), (1.2, 1.2, 1.2), b.t_flat(flowers[i % len(flowers)], 8, 8, 0.25))


def kippah(b, color="#f4f2ec", trim="#2a2a30", emblem="eye", y=32.0, layer="main"):
    """A kippah, the skull cap with the eye in a cross on the front (Patolli).  Quads: ~12."""
    geo = b.g(layer)
    geo.cube(HEAD, (-4.4, y, -4.4), (8.8, 1.3, 8.8), b.t_cloth(color, 16, 16, seed=61, trim=trim, trim_h=2))
    geo.cube(HEAD, (-3.6, y + 1.2, -3.6), (7.2, 0.8, 7.2), b.t_cloth(color, 16, 16, seed=62))
    geo.cube(HEAD, (-1.5, y - 0.2, -4.75), (3.0, 1.7, 0.3), b.t_cloth(color, 16, 16, seed=63, emblem=emblem, emblem_color=trim), faces=("north",))


# ================================================================================================ face parts
def _glasses_tile(b, frame, lens, w=24, h=18):
    def paint():
        a = np.zeros((h, w, 4), np.float32)
        fr = rgb(frame)
        a[..., :3] = fr
        a[..., 3] = 1.0
        a[2:h - 2, 2:w - 2, 3] = 0.0
        a[2:h - 2, 2:w - 2, :3] = rgb(lens)
        a[2:h - 2, 2:w - 2, 3] = 0.62                                         # a dark glass, the eye shows through
        for i in range(3, 9):
            a[3 + (i - 3) // 2, i, :3] = np.clip(rgb(lens) + 0.45, 0, 1)
            a[3 + (i - 3) // 2, i, 3] = 0.85
        a[0, :, :3] = tone(frame, 1.5)
        return a
    return b.atlas.tile(("glasses", L.hexs(rgb(frame)), L.hexs(rgb(lens)), w, h), w, h, paint)


def glasses(b, frame="#2a2430", lens="#14101c", eye_x=1.65, eye_y=27.05, w=3.1, h=2.3, layer="main"):
    """Rectangular glasses (Klaus): a frame-and-lens card in front of each eye, a bridge and two arms to the ears.  Quads: 2 + 2 cubes (~14)."""
    geo = b.g(layer)
    tile = _glasses_tile(b, frame, lens)
    for s in (-1, 1):
        geo.cube(HEAD, (s * eye_x - w / 2, eye_y - h / 2 - 0.1, -4.5), (w, h, 0.0), tile, faces=("north",))
        geo.cube(HEAD, (s * (eye_x + w / 2) - (0.0 if s > 0 else 0.4), eye_y + 0.3, -4.5), (0.4, 0.4, 4.6), b.t_flat(frame, 8, 8), faces=("north", "up", "east", "west"))
    geo.cube(HEAD, (-0.55, eye_y + 0.2, -4.55), (1.1, 0.4, 0.4), b.t_flat(frame, 8, 8))


def _plate_tile(b, color, edge, hole, tw=24, th=16, key="plate"):
    """A mask plate with an eye hole (cutout): `hole` is (cx, cy, rx, ry) in texels."""
    def paint():
        a = np.zeros((th, tw, 4), np.float32)
        ramp = L._grad(th, [(0.0, tone(color, 1.35)), (0.5, rgb(color)), (1.0, tone(color, 0.62))])
        a[..., :3] = ramp[:, None, :]
        a[..., 3] = 1.0
        ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
        cx, cy, rx, ry = hole
        d = ((xs + 0.5 - cx) / rx) ** 2 + ((ys + 0.5 - cy) / ry) ** 2
        a[d < 1.0, 3] = 0.0
        ring = (d >= 1.0) & (d < 1.9)
        a[ring, :3] = rgb(edge)
        return L._edge_outline(a, tone(color, 0.3), 0.5)
    return b.atlas.tile((key, L.hexs(rgb(color)), L.hexs(rgb(edge)), tw, th, hole), tw, th, paint)


def half_mask(b, right="#e8b830", left="#3a68d0", edge="#c8283a", layer="main", eye_x=1.65, eye_y=27.05):
    """The upper-face mask of William Vangeance: a plate over each eye with an eye hole, a rim, and a brow bar.  Quads: ~14."""
    geo = b.g(layer)
    for s, col, ed in ((-1, right, edge), (1, left, "#f2f0f6")):
        tile = _plate_tile(b, col, ed, (12.0, 8.0, 6.2, 4.6), key="mask%d" % s)
        geo.cube(HEAD, (s * 2.35 - 2.35, eye_y - 1.9, -4.7), (4.7, 3.7, 0.0), tile, faces=("north",))
        geo.cube(HEAD, (s * 2.35 - 2.35, eye_y - 1.9, -4.7), (4.7, 3.7, 0.7), b.t_flat(col, 8, 8), faces=("up", "down"))
    geo.cube(HEAD, (-4.4, 29.0, -4.7), (8.8, 1.3, 8.7), b.t_metal("#c89a30", 16, 8), faces=("north", "east", "west", "up"))


def eye_bandage(b, color="#7a1a24", ornament="#8ae8a8", side=1, layer="main"):
    """Rades's wrap: a band across the brow, a plate over the left eye with a green eye ornament, and bandages round the neck with a tattered end.  Quads: ~30."""
    geo = b.g(layer)
    wrap = b.t_cloth(color, 16, 16, seed=71, folds=6)
    geo.cube(HEAD, (-4.45, 29.4, -4.45), (8.9, 1.7, 8.9), wrap, faces=("north", "south", "east", "west", "up", "down"))
    x0 = 0.1 if side > 0 else -4.6
    geo.cube(HEAD, (x0, 25.3, -4.55), (4.5, 4.4, 0.6), wrap, faces=("north", "east", "west", "down"))
    b.g("fx").cube(HEAD, (x0 + 1.5, 28.2, -5.2), (1.5, 1.2, 0.6), b.t_gem(ornament), faces=("north", "up"))
    geo.cube("body", (-2.7, 23.3, -2.7), (5.4, 1.7, 5.4), wrap)
    bone = b.sway("fine", "body", _uid(b, "bd"), (0.0, 23.5, -2.9), ax=5.0, az=3.0, phase=0.8, walk=2.0, run=3.0)
    b.g("fine").cube(bone, (-1.0, 17.5, -2.9), (2.0, 6.0, 0.0), b.t_cloth(color, 16, 16, seed=72, hem="ragged"), faces=("north",), pivot=(0.0, 23.5, -2.9))


def forehead_stone(b, color="#a84ae8", x=0.0, y=30.4, w=1.4, h=1.7, layer="fx"):
    """A mage stone on the forehead (Fana): a faceted gem that glows.  Quads: 2."""
    b.g(layer).cube(HEAD, (x - w / 2, y, -4.75), (w, h, 0.5), b.t_gem(color), faces=("north", "up"))


def earrings(b, color="#8ae8a8", hoop=False, y=27.0, layer="fx", length=1.6):
    """A small gem dangling from each ear (loops in gold when `hoop`).  Quads: 4-8."""
    for s in (-1, 1):
        if hoop:
            b.g("main").cube(HEAD, (s * 4.35 - 0.2, y - 1.4, -0.7), (0.4, 1.5, 1.4), b.t_metal("#e8b830", 8, 8), faces=("east", "west", "north"))
        b.g(layer).cube(HEAD, (s * 4.4 - 0.4, y - 1.6 - length, -0.4), (0.8, length, 0.8), b.t_gem(color, 8, 8), faces=("north", "south", "east", "west"))


# ================================================================================================ body
def shawl(b, color="#1c1c24", trim="#e8b830", y=22.0, width=14.6, depth=6.4, drop=4.0, ornament=None, layer="main"):
    """A plain cloth shawl over the shoulders and upper chest (Patolli's black shawl): a ring on the body and two hanging panels in front.  Quads: ~16."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 32, 16, seed=81, trim=trim, trim_h=1)
    geo.cube("body", (-width / 2, y + 0.4, -depth / 2), (width, 1.9, depth), cloth)
    for s in (-1, 1):
        geo.cube("body", (s * (width / 2 - 1.6) - 1.0, y - drop + 0.4, -depth / 2 - 0.0), (2.0, drop, 0.0), b.t_cloth(color, 16, 16, seed=82, hem="zigzag"), faces=("north",))
    if ornament:
        geo.cube("body", (-0.35, y - drop - 1.0, -depth / 2 - 0.2), (0.7, drop + 2.0, 0.5), b.t_metal(ornament, 8, 16))


def scarf(b, color="#c8321c", rope="#e8b830", stone="#a84ae8", y=22.6, layer="main"):
    """The Crimson Lion's scarf: a thick ring at the neck and two gold ropes with a stone each hanging in front.  Quads: ~20."""
    geo = b.g(layer)
    geo.cube("body", (-3.6, y + 0.6, -3.1), (7.2, 1.9, 6.2), b.t_cloth(color, 32, 16, seed=91, folds=5))
    for s in (-1, 1):
        bone = b.sway("fine", "body", _uid(b, "sc"), (s * 1.6, y + 0.4, -2.9), ax=3.0, az=2.0, phase=1.1 * s, walk=1.5, run=2.5)
        b.g("fine").cube(bone, (s * 1.6 - 0.35, y - 5.6, -3.0), (0.7, 6.0, 0.7), b.t_metal(rope, 8, 16), pivot=(s * 1.6, y + 0.4, -2.9))
        gb = b.sway("fx", "body", _uid(b, "scg"), (s * 1.6, y + 0.4, -2.9), ax=3.0, az=2.0, phase=1.1 * s, walk=1.5, run=2.5)      # the stone swings with its rope (a bone of the fx file)
        b.g("fx").cube(gb, (s * 1.6 - 0.7, y - 7.0, -3.35), (1.4, 1.5, 1.4), b.t_gem(stone), faces=("north", "south", "east", "west", "up"), pivot=(s * 1.6, y + 0.4, -2.9))


def feather_panels(b, color="#e8ecf6", dark="#3a3a54", y=19.0, n=6, length=6.0, width=13.0, layer="fine"):
    """The Silver Eagle's robe skirt: an upper fur mantle (see bcparts.mantle) and a row of long feather cards below its dark band.  Quads: 2n."""
    geo = b.g(layer)
    for i in range(n):
        u = (i + 0.5) / n * 2 - 1
        x = u * (width / 2 - 1.0)
        for zz in (-3.3, 3.3):
            bone = b.sway(layer, "body", _uid(b, "fp"), (x, y, zz), ax=3.0, az=1.6, phase=0.9 * i + (0.5 if zz > 0 else 0.0), walk=1.6, run=2.6)
            geo.card(bone, x, y, zz, width / n * 1.15, length * (0.9 + 0.3 * hsh(i, zz)), b.t_cloth(color, 16, 32, seed=95 + i, hem="zigzag", trim=dark, trim_h=2), pivot=(x, y, zz))


def banner(b, color="#14101a", trim="#e8b830", emblem="diamond", layer="main"):
    """Yami's torn black banner: worn over the right shoulder, hanging down the back with a ragged hem, held by two cords to the left shoulder.  Quads: ~10."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 32, 32, seed=101, hem="ragged", emblem=emblem, emblem_color=trim, trim=trim, trim_h=1)
    geo.cube("body", (-5.0, 22.0, -2.0), (4.2, 2.2, 4.0), b.t_cloth(color, 16, 16, seed=102))
    bone = b.sway(layer, "body", _uid(b, "bn"), (-3.0, 23.0, 2.55), ax=2.0, az=1.4, phase=0.4, walk=2.0, run=3.0)
    geo.cube(bone, (-4.5, 8.0, 2.6), (9.0, 15.0, 0.0), cloth, faces=("north",), pivot=(-3.0, 23.0, 2.6))
    geo.cube("body", (-0.6, 13.5, -2.55), (1.0, 11.5, 0.0), b.t_cloth("#3a2a1a", 8, 16, seed=103), faces=("north",), rot=(0.0, 0.0, -34.0), pivot=(0.0, 23.0, -2.55))


def hood_down(b, color="#14101a", trim="#e8b830", fur=None, layer="main"):
    """A squad robe's hood lying on the back of the neck: a thick wedge of cloth (with a fur rim when `fur`).  Quads: 6-12."""
    geo = b.g(layer)
    geo.cube("body", (-3.6, 21.6, 1.6), (7.2, 3.4, 2.0), b.t_cloth(color, 16, 16, seed=111, trim=trim, trim_h=1))
    geo.cube("body", (-3.9, 24.6, -2.9), (7.8, 1.0, 1.0), b.t_cloth(color, 16, 16, seed=112))
    if fur:
        geo.cube("body", (-4.0, 24.5, 1.4), (8.0, 1.2, 2.6), b.t_fur(fur, seed=113))


def necklace(b, chain="#e8b830", pendant="#3a68d0", y=22.9, layer="main"):
    """A chain round the neck with a pendant on the chest: two thin cubes and a gem.  Quads: ~14."""
    geo = b.g(layer)
    metal = b.t_metal(chain, 16, 8)
    geo.cube("body", (-2.6, y, -2.5), (5.2, 0.35, 0.35), metal)
    for s in (-1, 1):
        geo.cube("body", (s * 2.2 - 0.18, y - 1.6, -2.55), (0.36, 1.7, 0.36), metal, rot=(0.0, 0.0, -s * 22.0), pivot=(s * 2.4, y, -2.5))
    b.g("fx").cube("body", (-0.6, y - 3.0, -2.65), (1.2, 1.4, 0.45), b.t_gem(pendant), faces=("north", "up"))


def sash(b, color="#6a2a8a", trim="#e8b830", y=14.4, layer="main"):
    """A wide sash round the waist with a knot and two hanging ends (the Vermillion siblings' purple sash).  Quads: ~16."""
    geo = b.g(layer)
    geo.cube("body", (-4.35, y, -2.35), (8.7, 1.6, 4.7), b.t_cloth(color, 32, 16, seed=121, trim=trim, trim_h=1))
    bone = b.sway("fine", "body", _uid(b, "ss"), (2.0, y + 0.8, -2.6), ax=3.5, az=2.5, phase=0.5, walk=2.0, run=3.0)
    b.g("fine").cube(bone, (1.2, y - 5.0, -2.6), (1.7, 5.6, 0.0), b.t_cloth(color, 16, 16, seed=122, hem="zigzag"), faces=("north",), pivot=(2.0, y + 0.8, -2.6))
    b.g("fine").cube(bone, (2.8, y - 4.0, -2.62), (1.5, 4.6, 0.0), b.t_cloth(color, 16, 16, seed=123, hem="zigzag"), faces=("north",), pivot=(2.0, y + 0.8, -2.6))


def antenna_locks(b, color, y=33.0, n=2, length=4.5, seed=130, layer="main"):
    """Two locks of hair standing up from the crown (Secre): crossed cards.  Quads: 4n."""
    geo = b.g(layer)
    sp = b.t_spike(color, tw=12, th=24, seed=seed)
    for i in range(n):
        x = (-1.3 if i == 0 else 1.3) * 1.0
        rot = (-8.0, 0.0, (-14.0 if i == 0 else 14.0))
        piv = (x, y, -0.5)
        geo.cube(HEAD, (x - 0.9, y, -0.5), (1.8, length, 0.0), sp, faces=("north",), rot=rot, pivot=piv)
        geo.cube(HEAD, (x, y, -1.4), (0.0, length, 1.8), sp, faces=("east",), rot=rot, pivot=piv)


def fish_mask(b, color="#9ac0e8", dark="#3a5a8a", bill=False, y=31.0, layer="main"):
    """A priest's fish mask pushed up on the forehead (Kahono: dolphin, Kiato: swordfish with a long bill): a plate on the crown and a fin or bill.  Quads: ~14."""
    geo = b.g(layer)
    geo.cube(HEAD, (-3.2, y, -5.0), (6.4, 2.2, 1.0), b.t_cloth(color, 16, 16, seed=141, trim=dark, trim_h=1), rot=(-12.0, 0.0, 0.0), pivot=(0.0, y, -4.5))
    if bill:
        geo.cube(HEAD, (-0.5, y + 1.0, -13.0), (1.0, 1.0, 8.2), b.t_horn(color, tip="#f2f6ff", tw=8, th=16, seed=142), rot=(-12.0, 0.0, 0.0), pivot=(0.0, y, -4.5))
    else:
        geo.cube(HEAD, (-0.5, y + 2.0, -5.2), (1.0, 2.6, 2.6), b.t_horn(dark, tip=color, tw=8, th=16, seed=143), rot=(-12.0, 0.0, 0.0), pivot=(0.0, y, -4.5))


def shells(b, color="#f6e6d8", n=3, x=0.0, y=29.5, z=5.4, layer="main"):
    """Seashells strung in a ponytail (Kahono): small tinted cubes.  Quads: 6n."""
    geo = b.g(layer)
    for i in range(n):
        geo.cube(HEAD, (x - 0.7, y - i * 2.6 - 0.6, z - 0.7), (1.4, 1.2, 1.4), b.t_flat(color if i % 2 == 0 else "#e8a0a8", 8, 8, 0.3))


def pointed_ears(b, skin, length=3.0, tilt=18.0, y=27.4, layer="main"):
    """The elf ears of the Midnight Sun (and of a possessed host): the kit's elf_ears with the character's own skin tone.  Quads: ~20."""
    P.elf_ears(b, skin, layer=layer, y=y, length=length, tilt=tilt)


def gauntlets(b, slim=False, color="#c8ccd8", trim="#3a68d0", y_top=21.2, rings=2, layer="main"):
    """Plate bracers on both forearms (Charlotte): `rings` bands each with a coloured edge, no tassel.  Quads: 12 per ring pair."""
    P.bracers(b, slim, y_top=y_top, rings=rings, gold=color, gem=trim, tassels=False, layer=layer)


def collar_plate(b, color="#c8ccd8", trim="#3a68d0", layer="main"):
    """A steel gorget round the neck (the kit's collar in steel with a coloured edge)."""
    P.collar(b, color="#2a2c38", gold=color, layer=layer, high=2.6, width=9.0)
