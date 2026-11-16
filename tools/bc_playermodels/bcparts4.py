"""Wave 2 part generators (docs/bosses_bc_wave2.md): a cloth hood, glasses, a headband with diamonds, a beard, a cloak collar with a button, lapels, fur boot collars, a long robe skirt, a stole,
tentacles, a chest gem. Same conventions as bcparts.py: every function draws into a `Build` (b) on a layer ("main" / "fine" / "fx"); coordinates are the geo file's (pixels, y up, front on -z,
the head x -4..4 y 24..32 z -4..4, the body y 12..24; the entity's RIGHT is -x)."""
import math

import numpy as np

import bclib as L
from bclib import rgb, tone, hsh
import bcparts as P
from bcparts import HEAD

EYE_X, EYE_Y = 1.65, 27.05           # the centre of an eye in model coordinates (face px 2.35 / 5.65, 4.95 down from the top of the head)


# ================================================================================================ small tiles
def t_lens(b, rim="#14101a", lens="#1a1620", alpha=0.9, tw=16, th=16):
    """A round lens: a rim ring, a tinted (slightly transparent) disc and a glint."""
    def paint():
        ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
        x, y = (xs + 0.5 - tw / 2) / (tw / 2), (ys + 0.5 - th / 2) / (th / 2)
        r = np.sqrt(x * x + y * y)
        a = np.zeros((th, tw, 4), np.float32)
        inner, ring = r < 0.76, (r >= 0.76) & (r < 1.0)
        a[inner, :3] = rgb(lens)
        a[inner, 3] = alpha
        a[ring, :3] = rgb(rim)
        a[ring, 3] = 1.0
        g = ((x + 0.38) ** 2 + (y + 0.42) ** 2 < 0.05) & inner
        a[g, :3] = np.clip(rgb(lens) * 0.4 + 0.6, 0, 1)
        a[g, 3] = 1.0
        return a
    return b.atlas.tile(("lens", L.hexs(rgb(rim)), L.hexs(rgb(lens)), alpha, tw, th), tw, th, paint)


def t_diamond(b, color="#d8d0ec", edge=None, tw=12, th=16):
    """A diamond ornament (a rhombus, lit toward the top, dark edge)."""
    def paint():
        ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
        x, y = (xs + 0.5 - tw / 2) / (tw / 2), (ys + 0.5 - th / 2) / (th / 2)
        m = (np.abs(x) + np.abs(y)) < 0.96
        a = np.zeros((th, tw, 4), np.float32)
        ramp = L._grad(th, [(0.0, tone(color, 1.5)), (0.5, rgb(color)), (1.0, tone(color, 0.55))])
        a[..., :3] = ramp[:, None, :]
        a[..., 3] = m.astype(np.float32)
        facet = m & (np.abs(x) < 0.08)
        a[facet, :3] *= 0.85
        return L._edge_outline(a, tone(edge or color, 0.35), 0.7)
    return b.atlas.tile(("diamond", L.hexs(rgb(color)), tw, th, edge), tw, th, paint)


# ================================================================================================ head
def hood(b, color="#3a3850", trim="#c8a0ff", g=0.3, thick=0.9, peak=4.2, front_y=30.0, side_low=25.2, back_low=24.4, seed=40, layer="main", trim_glow=False):
    """A cloth hood round the skull as cube plates (top, back, two sides, a brow band that leaves the face free) with a lit trim along the front edge and a pointed peak at the back."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 32, 32, seed=seed, folds=5)
    w = 8.0 + 2 * g
    top_y = 32.0 + g
    geo.cube(HEAD, (-w / 2, top_y, -w / 2 + 0.4), (w, thick, w - 0.4), cloth)
    geo.cube(HEAD, (-w / 2, back_low, 4.0 + g), (w, top_y + thick - back_low, thick), cloth)
    for s in (-1, 1):
        x0 = 4.0 + g if s > 0 else -(4.0 + g) - thick
        geo.cube(HEAD, (x0, side_low, -3.4), (thick, top_y + thick - side_low, 4.0 + g + 3.4 + thick), cloth)
    geo.cube(HEAD, (-w / 2, front_y, -4.0 - g - thick), (w, top_y + thick - front_y, thick), cloth)
    if trim:
        tr = b.t_metal(trim, 16, 4, band=False, rivets=False)
        geo.cube(HEAD, (-w / 2 - 0.1, front_y - 0.35, -4.0 - g - thick - 0.1), (w + 0.2, 0.5, thick + 0.2), tr)
        for s in (-1, 1):
            x0 = 4.0 + g if s > 0 else -(4.0 + g) - thick
            geo.cube(HEAD, (x0 - 0.1, side_low - 0.35, -3.5), (thick + 0.2, 0.5, 4.0 + g + 3.5 + thick + 0.1), tr)
    sp = b.t_spike(color, tw=16, th=24, seed=seed + 5, tip=trim)
    pv = (0.0, top_y + thick - 0.3, 3.0)
    geo.cube(HEAD, (-2.6, pv[1], pv[2]), (5.2, peak, 0.0), sp, faces=("north",), rot=(36.0, 0.0, 0.0), pivot=pv)
    geo.cube(HEAD, (0.0, pv[1], pv[2] - 2.6), (0.0, peak, 5.2), sp, faces=("east",), rot=(36.0, 0.0, 0.0), pivot=pv)


def glasses(b, rim="#14101a", lens="#1a1620", r=1.85, alpha=0.92, layer="main", temples=True):
    """Round glasses: two lens cards in front of the eyes, a bridge and, if asked, the temple arms back to the ears."""
    geo = b.g(layer)
    tile = t_lens(b, rim, lens, alpha)
    metal = b.t_metal(rim, 16, 8, band=False, rivets=False)
    for s in (-1, 1):
        geo.cube(HEAD, (s * EYE_X - r, EYE_Y - r, -4.4), (2 * r, 2 * r, 0.0), tile, faces=("north",))
        if temples:
            geo.cube(HEAD, (s * 4.15 - 0.2, EYE_Y + 0.4, -4.4), (0.4, 0.4, 4.7), metal, faces=("north", "east", "up"))
    geo.cube(HEAD, (-0.45, EYE_Y + 0.35, -4.45), (0.9, 0.45, 0.4), metal, faces=("north", "up"))


def headband(b, band="#3a3050", dia="#d8d0ec", y=30.3, diamonds=3, side=-1, layer="main", seed=44):
    """A brow band round the head with `diamonds` diamond ornaments in a row on one side (side -1 = the entity's right)."""
    geo = b.g(layer)
    cloth = b.t_cloth(band, 32, 8, seed=seed, folds=3)
    geo.cube(HEAD, (-4.6, y, -4.7), (9.2, 1.1, 0.7), cloth)
    geo.cube(HEAD, (-4.6, y, 4.0), (9.2, 1.1, 0.7), cloth)
    for s in (-1, 1):
        geo.cube(HEAD, (s * 4.6 - (0.0 if s > 0 else 0.7), y, -4.7), (0.7, 1.1, 9.4), cloth)
    tile = t_diamond(b, dia)
    for i in range(diamonds):
        x = side * (1.3 + 1.45 * i)
        geo.cube(HEAD, (x - 0.65, y - 0.35, -5.2), (1.3, 1.7, 0.0), tile, faces=("north",))


def beard(b, color="#d8b050", y_top=24.8, n=5, length=3.2, seed=60, layer="fine", sideburns=True):
    """A beard hanging off the chin (cards) and, if asked, two sideburn strips in front of the ears."""
    geo = b.g(layer)
    for i in range(n):
        t = (i + 0.5) / n * 2 - 1
        x = t * 2.3
        ln = length * (1.0 - 0.28 * abs(t)) * (0.85 + 0.3 * hsh(i, seed))
        tile = b.t_strand(color, seed=seed + i, prongs=2, tip=0.3)
        z = -4.3 - 0.08 * (i % 2)
        geo.card(HEAD, x, y_top, z, 1.8, ln, tile, rot=(0.0, 0.0, -t * 6.0), pivot=(x, y_top, z))
    if sideburns:
        tile = b.t_strand(color, seed=seed + 9, prongs=2, tip=0.3)
        for s in (-1, 1):
            geo.cube(HEAD, (s * 4.15, 24.6, -3.8), (0.0, 3.6, 2.6), tile, faces=("east",))


# ================================================================================================ body
def cloak_collar(b, color="#6a1a24", trim=None, y=21.7, width=12.4, depth=6.8, thick=2.2, drop=3.0, back_drop=5.0, seed=70, layer="main", button=None, button_side=-1, roll=False):
    """A cloth cape-let round the shoulders: a block, a front card and a longer back card hanging from it, a gold button on one shoulder, and (roll) the hood bunched up at the back of the neck."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 32, 16, seed=seed, trim=trim, trim_h=2, folds=5) if trim else b.t_cloth(color, 32, 16, seed=seed, folds=5)
    drape = b.t_cloth(color, 32, 32, seed=seed + 1, trim=trim, trim_h=3, folds=4) if trim else b.t_cloth(color, 32, 32, seed=seed + 1, folds=4)
    geo.cube("body", (-width / 2, y, -depth / 2), (width, thick, depth), cloth)
    geo.card("body", 0.0, y + 0.3, -depth / 2 - 0.05, width * 0.86, drop, drape)
    geo.card("body", 0.0, y + 0.3, depth / 2 + 0.05, width * 0.9, back_drop, drape)
    if roll:
        geo.cube("body", (-3.6, y + thick - 0.4, depth / 2 - 0.5), (7.2, 2.8, 2.6), cloth)
    if button:
        geo.cube("body", (button_side * (width / 2 - 1.3) - 0.6, y + thick - 0.15, -0.9), (1.2, 0.6, 1.2), b.t_metal(button, 16, 16))


def lapels(b, dark="#14101a", gem="#d8d0ec", rows=3, y_top=23.2, layer="main", seed=72):
    """Two dark lapels down the chest, each with a column of small diamonds."""
    geo = b.g(layer)
    cloth = b.t_cloth(dark, 16, 32, seed=seed, folds=2)
    dia = t_diamond(b, gem, tw=8, th=12)
    for s in (-1, 1):
        x = s * 2.0
        geo.card("body", x, y_top, -2.62, 2.6, 7.4, cloth, rot=(0.0, 0.0, s * -8.0), pivot=(x, y_top, -2.62))
        for i in range(rows):
            geo.cube("body", (s * (2.3 - 0.12 * i) - 0.5, y_top - 1.6 - i * 2.0, -2.7), (1.0, 1.4, 0.0), dia, faces=("north",))


def fur_boots(b, color="#4a4048", y=2.0, h=2.2, layer="main", seed=75):
    """A fur collar round each boot top."""
    geo = b.g(layer)
    fur = b.t_fur(color, seed=seed)
    for s in (-1, 1):
        geo.cube("leg_" + ("l" if s > 0 else "r"), (s * 1.9 - 2.65, y, -2.65), (5.3, h, 5.3), fur)


def robe(b, color="#f2eee4", trim="#e8b830", y_top=13.4, length=12.0, rings=2, panels=10, radius=3.7, seed=76, sway=2.4, layer="main"):
    """A long robe skirt: `rings` rings of cloth cards round the hips (each longer and wider), a gold hem on the outer ring, swaying a little."""
    geo = b.g(layer)
    for ring in range(rings):
        r = radius + 0.55 * ring
        ln = length * (1.0 - 0.1 * ring)
        tile = b.t_cloth(color, 16, 32, seed=seed + ring, trim=trim if ring == 0 else None, trim_h=3, folds=3)
        for i in range(panels):
            a = i / panels * math.tau + ring * 0.25
            x, z = math.sin(a) * r, -math.cos(a) * r
            w = 2 * math.pi * r / panels * 1.2
            bone = b.sway(layer, "body", "rb_%d_%d" % (ring, i % 4), (x, y_top, z), ax=sway, az=sway * 0.6, walk=1.4, run=2.2, phase=0.9 * i + ring)
            geo.cube(bone, (x - w / 2, y_top - ln, z), (w, ln, 0.0), tile, faces=("north",), rot=(0.0, -math.degrees(a), 0.0), pivot=(x, y_top, z))


def stole(b, color="#e8b830", trim="#fff2b0", length=19.0, width=3.4, y_top=23.9, layer="main", seed=77):
    """A long stole hanging down the front of the robe from the neck."""
    geo = b.g(layer)
    tile = b.t_cloth(color, 16, 48, seed=seed, trim=trim, trim_h=3, folds=2)
    geo.card("body", 0.0, y_top, -2.78, width, length, tile)
    back = b.t_cloth(color, 16, 48, seed=seed + 1, trim=trim, trim_h=3, folds=2)
    geo.card("body", 0.0, y_top, 2.78, width, length * 0.9, back)


def tentacles(b, color="#d8ccd8", tip="#2a1a28", n=2, y=20.5, length=11.0, w0=1.4, w1=0.5, segs=4, spread=5.6, z=3.6, seed=80, layer="main", ang0=16.0, curl=7.0):
    """Light tentacles hanging from the shoulder blades: `n` per side, each a chain of boxes (thinner toward the end, a dark tip) in one bone that sways."""
    geo = b.g(layer)
    tile = b.t_horn(color, tip=tip, seed=seed)
    for s in (-1, 1):
        for k in range(n):
            x0, z0 = s * (spread + 0.9 * k), z + 0.7 * k
            bone = b.sway(layer, "body", "tent_%s%d" % ("l" if s > 0 else "r", k), (x0, y, z0), ax=6.0, az=5.0, walk=2.5, run=4.0, cast=3.0, phase=1.1 * k + (0.9 if s > 0 else 0.0))
            ang = ang0 + 9.0 * k
            p = np.array([x0, y, z0], float)
            for i in range(segs):
                w = w0 + (w1 - w0) * i / max(1.0, segs - 1.0)
                seg = length / segs * (1.0 - 0.05 * k)
                a = math.radians(ang)
                d = np.array([math.sin(a) * s, -math.cos(a), 0.0])
                c = p + d * seg / 2
                geo.cube(bone, (c[0] - w / 2, c[1] - seg / 2, c[2] - w / 2), (w, seg + 0.3, w), tile, rot=(0.0, 0.0, s * ang), pivot=tuple(c))
                p = p + d * seg
                ang += curl


def chest_gem(b, color="#58e0e8", y=17.4, size=1.8, x=0.0, layer="fx"):
    """A glowing stone on the chest (the fx layer shines)."""
    b.g(layer).cube("body", (x - size / 2, y, -2.78), (size, size * 1.15, 0.7), b.t_gem(color), faces=("north", "up"))


def studs(b, points, color="#58e0e8", size=1.1, anchor="body", layer="fx"):
    """Small glowing stones at the given (x, y, z) points of a bone (a coat's crystals)."""
    g = b.g(layer)
    gem = b.t_gem(color)
    for (x, y, z) in points:
        g.cube(anchor, (x - size / 2, y, z - size / 2), (size, size * 1.2, size), gem, faces=("north", "south", "east", "west", "up"))


def forehead_stones(b, color="#40e0d0", layer="fx"):
    """Three stones in a vertical line on the forehead (the larger one in the middle), standing out of the skin a little."""
    g = b.g(layer)
    gem = b.t_gem(color)
    g.cube(HEAD, (-0.55, 29.6, -4.45), (1.1, 1.5, 0.5), gem, faces=("north", "up"))
    g.cube(HEAD, (-0.4, 31.2, -4.4), (0.8, 0.9, 0.4), gem, faces=("north", "up"))
    g.cube(HEAD, (-0.4, 28.4, -4.4), (0.8, 0.9, 0.4), gem, faces=("north", "up"))
