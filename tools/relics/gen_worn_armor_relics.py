"""Signature worn models of the armour relics that only had the model of their look (chest pieces of waves 6 and 7, three pairs of boots of wave 14): the same method and
file format as gen_worn_armor.py. A relic chest piece keeps its painted body armour (client/relics/ArmorPainter); these models are the raised parts over it (collars,
crests, plates, coils, tails, wisps) and are drawn on the body at the library's body scale, so they are authored as accessories, not as a whole garment."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)
import gen_worn_armor as A

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


def _gorget(m, tile="plate", slot=MAIN, trim_tile="gilt", h=3.1, r=5.15, d=3.1):
    col = Part("gorget")
    L.band(col, -2.6, 0.5, r, d, 0.55, 12, 0.15, 0.5, 3.4)
    m.add(col, slot, tile, SHELL)
    tr = Part("trim")
    L.band(tr, -2.8, -2.35, r + 0.05, d + 0.05, 0.6, 12, 0.0, 0.0, 3.4)
    m.add(tr, TRIM, trim_tile)


def _stone(m, pos, r=0.8, slot=GLOW, tile="gem", name="stone"):
    g = Part(name)
    L.ml.gem(g, pos, r, 0.9, 6, "z")
    m.add(g, slot, tile, EMIT | HOT, uv="fit")


# ================================================================ chest pieces
@model
def chest_conduction():
    """A conduction harness: three rune-lit copper coils wound round the torso, a ring of nodes on the sternum and a conduction pack with two antennae on the back."""
    m = WModel("chest_conduction", "body")
    for k, y in enumerate((-2.6, -6.0, -9.4)):
        c = Part("coil%d" % k)
        L.band(c, y - 0.55, y + 0.55, 4.7, 2.8, 0.32, 10, 0.0, 0.0, 3.4)
        m.add(c, TRIM, "gilt", SHELL)
        r = Part("rune%d" % k)
        L.band(r, y - 0.2, y + 0.2, 4.75, 2.85, 0.36, 10, 0.0, 0.0, 3.4)
        m.add(r, GLOW, "rune", EMIT | HOT, density=3.0)
    for k in range(3):
        _stone(m, (-2.4 + 2.4 * k, -6.0, -3.5), 0.5, name="node%d" % k)
    pk = Part("pack")
    loft(pk, "y", (0, -2.0, 3.6), [(0.0, 1.0, 1.0), (-7.0, 1.0, 1.0)], octagon(6.0, 2.6, 0.9))
    m.add(pk, MAIN, "leather", SHELL)
    pt = Part("pack_plate")
    loft(pt, "z", (0, -5.5, 4.9), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], octagon(4.4, 5.2, 1.0))
    m.add(pt, TRIM, "gilt", 0, uv="fit")
    g = m.group("antenna", "sway", (0, -2.0, 4.0), 4.0, 0.1, 0.0)
    for sx in (-1, 1):
        an = Part("antenna%d" % sx)
        L.cone(an, (sx * 2.0, -2.0, 4.0), (sx * 2.8, 4.8, 4.4), 0.35, 4, mid=0.5, mid_r=0.9)
        m.add(an, TRIM, "gilt", 0, group=g)
        tp = Part("tip%d" % sx)
        L.dia(tp, (sx * 2.8, 5.3, 4.4), 0.4, 0.5, 0.4)
        m.add(tp, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    return m


@model
def coil_arm():
    """Two copper coil turns with a lit rune wrapped round the forearm (right arm; the left is the mirror)."""
    m = WModel("coil_arm", "rarm")
    for y in (-4.4, -7.4):
        c = Part("coil%.0f" % y)
        L.band(c, y - 0.5, y + 0.5, 2.75, 2.75, 0.3, 8, 0.0, 0.0, 3.4)
        c.V = [v + np.array([-1.0, 0, 0]) for v in c.V]
        m.add(c, TRIM, "gilt", SHELL)
        r = Part("rune%.0f" % y)
        L.band(r, y - 0.18, y + 0.18, 2.8, 2.8, 0.34, 8, 0.0, 0.0, 3.4)
        r.V = [v + np.array([-1.0, 0, 0]) for v in r.V]
        m.add(r, GLOW, "rune", EMIT | HOT, density=3.0)
    return m


@model
def chest_plot():
    """The Armor of Plot: a heavy gorget, a big lock ring on the sternum with a bolted plate behind it and four rivet bolts, and a plated back guard."""
    m = WModel("chest_plot", "body")
    _gorget(m)
    pl = Part("plate")
    loft(pl, "z", (0, -5.8, -3.0), [(0.0, 1.0, 1.0), (-0.6, 1.0, 1.0)], octagon(7.6, 8.4, 2.2), caps=(True, True))
    m.add(pl, MAIN, "plate", SHELL, uv="fit")
    rg = Part("ring")
    L.torus(rg, (0, -5.8, -3.8), 2.4, 0.5, 14, 4, plane="xy")
    m.add(rg, TRIM, "gilt", SHELL)
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        b = Part("bolt%d" % k)
        L.dia(b, (3.2 * math.cos(a), -5.8 + 3.4 * math.sin(a), -3.7), 0.4, 0.4, 0.35)
        m.add(b, TRIM, "gilt", uv="fit")
    _stone(m, (0, -5.8, -4.2), 1.1)
    bk = Part("back")
    loft(bk, "y", (0, -1.5, 3.1), [(0.0, 1.0, 1.0), (-9.0, 0.95, 1.0)], octagon(8.4, 1.6, 0.6))
    m.add(bk, MAIN, "plate", SHELL)
    return m


@model
def pauldron_rays():
    """A pauldron crowned with a fan of sun rays: two layered plates and seven gilt rays that spread over the shoulder, a lit stone at the middle."""
    m = WModel("pauldron_rays", "rarm")
    for k, (y, rr, hh) in enumerate(((1.2, 3.5, 1.4), (-0.7, 3.3, 1.3))):
        pl = Part("plate%d" % k)
        loft(pl, "y", (-1.0, y, 0.0), [(0.0, 0.88, 0.88), (-hh * 0.4, 1.0, 1.0), (-hh, 0.95, 0.95)], octagon(2 * rr, 2 * rr * 0.92, rr * 0.55), caps=(True, False))
        m.add(pl, MAIN if k == 0 else TRIM, "plate" if k == 0 else "gilt", SHELL)
    for k in range(7):
        a = math.radians(-70 + k * 23.3)
        ry = Part("ray%d" % k)
        L.cone(ry, (-1.0 + 1.4 * math.sin(a), 2.4, 1.4 * math.cos(a) * 0.6), (-1.0 + 4.6 * math.sin(a), 3.4 + 2.3 * math.cos(a), 4.0 * math.cos(a) * 0.5), 0.55, 4, mid=0.35, mid_r=0.9)
        m.add(ry, TRIM, "gilt", SHELL)
    _stone(m, (-1.0, 2.8, -1.4), 0.5)
    return m


@model
def chest_dawn():
    """The Armor of the Defiant Dawn: a plated gorget, a breast crest of a rising sun (a half disc with twelve rays above a lit horizon line) and a tall sunburst behind the shoulders."""
    m = WModel("chest_dawn", "body")
    _gorget(m)
    d = Part("disc")
    loft(d, "-z", (0, -6.4, -3.1), [(0.0, 1.0, 1.0), (0.6, 1.0, 1.0)], L.ml.circle(2.1, 12), caps=(False, True))
    m.add(d, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(12):
        a = math.pi * (k + 0.5) / 12
        ry = Part("ray%d" % k)
        L.cone(ry, (2.3 * math.cos(a), -6.4 + 2.3 * math.sin(a), -3.2), (3.9 * math.cos(a), -6.4 + 3.9 * math.sin(a), -3.5), 0.4, 4, mid=0.35, mid_r=0.9)
        m.add(ry, TRIM, "gilt", SHELL, uv="fit")
    hz = Part("horizon")
    loft(hz, "-z", (0, -6.45, -3.1), [(0.0, 1.0, 1.0), (0.8, 1.0, 1.0)], octagon(8.8, 0.7, 0.2))
    m.add(hz, TRIM, "gilt", SHELL, uv="fit")
    g = m.group("burst", "pulse", (0, -4.0, 4.0), 0.05, 1.0, 0.0)
    for k in range(9):
        a = math.pi * (k + 0.5) / 9
        ry = Part("back_ray%d" % k)
        L.cone(ry, (3.5 * math.cos(a), -2.0 + 3.5 * math.sin(a), 3.6), (7.5 * math.cos(a), -2.0 + 7.5 * math.sin(a), 4.1), 0.55, 4, mid=0.35, mid_r=0.9)
        m.add(ry, GLOW, "flame", EMIT, group=g, uv="fit")
    return m


@model
def chest_stone():
    """The Stone Sentinel's plates: rough slabs of rock stacked over the chest and belly with cracks and moss, a boulder collar and a stone slab on the back."""
    m = WModel("chest_stone", "body")
    rng = np.random.default_rng(7)
    for k, (y, w, h) in enumerate(((-1.6, 8.6, 2.6), (-4.4, 8.0, 2.8), (-7.4, 7.2, 2.8), (-10.2, 6.4, 2.4))):
        s = Part("slab%d" % k)
        loft(s, "y", (rng.uniform(-0.3, 0.3), y, -2.9), [(0.0, 1.0, 1.0), (-h, 0.95, 1.0)], octagon(w, 1.8 + 0.3 * k % 2, 0.9), caps=(True, True))
        m.add(s, MAIN, "plate" if k % 2 == 0 else "bone", SHELL)
    for k in range(7):
        a = 2 * math.pi * k / 7
        b = Part("boulder%d" % k)
        L.sph(b, (4.7 * math.cos(a), 0.2 + 0.5 * (k % 2), 2.9 * math.sin(a)), 1.5 + 0.3 * (k % 3), 3, 6, squash=0.8)
        m.add(b, MAIN, "plate", SHELL)
    for k in range(3):
        cr = Part("crack%d" % k)
        loft(cr, "y", (-2.4 + 2.4 * k, -2.4 - 1.2 * k, -3.45), [(0.0, 0.14, 0.1), (-2.4, 0.1, 0.1), (-3.2, 0.0, 0.0)], octagon(1.0, 1.0, 0.25))
        m.add(cr, GLOW, "soft", EMIT, uv="fit")
    for k in range(5):
        mo = Part("moss%d" % k)
        L.sph(mo, (-3.5 + 1.8 * k, -1.2 - 2.2 * (k % 3), -3.3), 0.8, 2, 5, squash=0.5)
        m.add(mo, ACCENT, "snow", 0, uv="fit")
    bk = Part("back")
    loft(bk, "y", (0, -1.0, 3.0), [(0.0, 1.0, 1.0), (-9.0, 0.9, 1.0)], octagon(8.2, 1.7, 0.8))
    m.add(bk, MAIN, "plate", SHELL)
    return m


@model
def pauldron_stone():
    """A boulder over the shoulder: a rough sphere of rock, two smaller stones and a lit crack."""
    m = WModel("pauldron_stone", "rarm")
    b = Part("boulder")
    L.sph(b, (-1.0, 1.4, 0.0), 3.6, 3, 8, squash=0.7)
    m.add(b, MAIN, "plate", SHELL)
    for k, (x, y, z, r) in enumerate(((-4.2, 0.4, 0.8, 1.4), (1.8, 0.2, -1.0, 1.2), (-1.0, 3.4, 1.4, 1.2))):
        s = Part("stone%d" % k)
        L.sph(s, (x, y, z), r, 2, 6, squash=0.8)
        m.add(s, MAIN, "bone", SHELL)
    c = Part("crack")
    loft(c, "y", (-1.0, 3.6, -1.8), [(0.0, 0.2, 0.1), (-2.4, 0.14, 0.1), (-3.0, 0.0, 0.0)], octagon(1.0, 1.0, 0.25))
    m.add(c, GLOW, "soft", EMIT, uv="fit")
    return m


@model
def chest_mist():
    """The Mistwalker Robe: a long tattered skirt of drifting panels in three layers, a loose shawl over the shoulders and a pale lit clasp, all in motion."""
    m = WModel("chest_mist", "body")
    g = m.group("mist", "wave", (0, -8.0, 0.0), 0.9, 0.5, 0.6)
    for layer, (rx, rz, y1, tile) in enumerate(((4.9, 3.0, -21.5, "silk"), (5.6, 3.6, -19.0, "cloth"), (6.3, 4.2, -16.0, "silk"))):
        for sx in (-1, 1):
            def fn(u, v, sx=sx, rx=rx, rz=rz, y1=y1):
                a = math.pi * u
                return (sx * rx * math.sin(a) * (1 + 0.16 * v), -10.8 + (y1 + 10.8) * v, -rz * math.cos(a) * (1 + 0.12 * v))
            p = Part("mist%d_%d" % (layer, sx))
            L.sheet(p, fn, 5, 3, 0.3 * sx * -1)
            m.add(p, MAIN if layer != 1 else ACCENT, tile, SHELL, group=g)
    sh = Part("shawl")
    def sfn(u, v):
        th = 2 * math.pi * u
        r = 3.8 + v * 2.2
        return (r * 1.3 * math.cos(th), 0.8 - v * 3.6 - 0.8 * v * v * (0.5 + 0.5 * math.sin(th - 1.6)), r * 0.86 * math.sin(th))
    L.sheet(sh, sfn, 12, 2, 0.35)
    m.add(sh, ACCENT, "silk", SHELL)
    _stone(m, (0, -0.8, -3.5), 0.7)
    return m


@model
def chest_neket():
    """Neket's practical mage robe: a bandolier of glass vials across the chest, a scroll tube on the back, rolled cuffs and two big front pockets with flaps."""
    m = WModel("chest_neket", "body")
    bd = Part("bandolier")
    pts = L.bez((-4.0, 0.0, -3.0), (0.0, -3.0, -3.3), (4.0, -10.0, -3.0), 6)
    sweep(bd, pts, octagon(1.6, 0.4, 0.15), scales=[(1.0, 1.0)] * len(pts), caps=(True, True), up=(0, 0, 1))
    m.add(bd, DARK, "leather")
    for k in range(5):
        t = (k + 0.5) / 5
        x = -4.0 + 8.0 * t - 0.5 * math.sin(math.pi * t)
        y = -0.4 - 9.6 * t * (1 + 0.0)
        v = Part("vial%d" % k)
        lathe(v, "y", (x, y - 1.0, -3.7), [(0.0, 0.5), (0.3, 0.6), (1.5, 0.6), (1.9, 0.35), (2.2, 0.28)], sides=6, caps=(True, True))
        m.add(v, GLOW if k % 2 == 0 else ACCENT, "crystal", EMIT, uv="fit")
        cp = Part("cork%d" % k)
        L.dia(cp, (x, y + 1.35, -3.7), 0.26, 0.2, 0.26)
        m.add(cp, TRIM, "wood", uv="fit")
    for sx in (-1, 1):
        pk = Part("pocket%d" % sx)
        loft(pk, "z", (sx * 2.7, -9.4, -3.0), [(0.0, 1.0, 1.0), (-0.5, 1.0, 1.0)], octagon(3.0, 3.0, 0.7))
        m.add(pk, MAIN, "cloth", SHELL, uv="fit")
        fl = Part("flap%d" % sx)
        loft(fl, "z", (sx * 2.7, -8.2, -3.5), [(0.0, 1.0, 1.0), (-0.4, 1.0, 1.0)], octagon(3.2, 1.6, 0.5))
        m.add(fl, TRIM, "leather", 0, uv="fit")
    tb = Part("tube")
    L.cone(tb, (-2.0, -9.0, 3.6), (2.6, 2.0, 3.6), 0.9, 8, mid=0.5, mid_r=1.0)
    m.add(tb, TRIM, "wood")
    for k in range(2):
        cap = Part("cap%d" % k)
        L.dia(cap, ((-2.0, -9.0, 3.6) if k == 0 else (2.6, 2.0, 3.6))[0:3], 0.9, 0.5, 0.9)
        m.add(cap, TRIM, "gilt", uv="fit")
    return m


@model
def hood_shadow():
    """The Shadow Robes' hood: a deep dark cowl with a ragged lower edge that falls over the shoulders and two pale lit eyes in the dark of the face opening."""
    m = WModel("hood_shadow", "head")
    A._cowl_shell(m, open_=0.6, top=9.8, drape=-3.4, r=5.3, tile="silk", slot=DARK, edges=False)
    for sx in (-1, 1):
        ey = Part("eye%d" % sx)
        L.dia(ey, (sx * 1.7, 4.2, -4.4), 0.8, 0.28, 0.2)
        m.add(ey, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(5):
        a = math.pi * (0.1 + 0.8 * k / 4)
        r = 5.6
        t = Part("rag%d" % k)
        L.cone(t, (r * math.cos(a + math.pi), -3.2, r * math.sin(a + math.pi) * 0.9), (r * 1.1 * math.cos(a + math.pi), -6.4 - (k % 3), r * 1.0 * math.sin(a + math.pi) * 0.95), 0.9, 4, mid=0.4, mid_r=0.9)
        m.add(t, DARK, "silk", SHELL)
    return m


@model
def chest_shadow():
    """The Shadow Robes: a ragged shoulder cape of dark silk with pointed tatters, a cloak of shadow that streams behind the wearer in the stride, a pale clasp and drifting wisps."""
    m = WModel("chest_shadow", "body")
    sh = Part("cape")
    def sfn(u, v):
        th = 2 * math.pi * u
        r = 3.8 + v * 2.8
        return (r * 1.3 * math.cos(th), 0.8 - v * 4.4 - 1.0 * v * v * (0.5 + 0.5 * math.sin(th - 1.6)), r * 0.86 * math.sin(th))
    L.sheet(sh, sfn, 12, 2, 0.35)
    m.add(sh, DARK, "silk", SHELL)
    for k in range(8):
        th = 2 * math.pi * (k + 0.5) / 8
        r = 6.4
        x, z = r * 1.3 * math.cos(th), r * 0.86 * math.sin(th)
        t = Part("tatter%d" % k)
        L.cone(t, (x * 0.95, -3.4, z * 0.95), (x * 1.0, -6.6 - (k % 3) * 1.2, z * 1.0), 0.8, 4, mid=0.4, mid_r=0.9)
        m.add(t, DARK, "silk", SHELL)
    g = m.group("shadow", "swing", (0, -1.0, 3.0), 16.0, 1.3, 0.0)
    cp = Part("cloak")
    L.sheet(cp, lambda u, v: ((u - 0.5) * (8.8 + 2.4 * v), -1.4 - v * 19.0, 3.1 + 0.8 * v + 1.0 * math.sin(u * math.pi) * v), 4, 4, -0.4)
    m.add(cp, DARK, "silk", SHELL, group=g)
    for k in range(5):
        t = Part("ctatter%d" % k)
        x = -4.6 + k * 2.3
        L.cone(t, (x, -19.5, 3.9 + 0.4), (x + 0.2, -23.2 - (k % 2) * 1.5, 4.6), 1.0, 4, mid=0.4, mid_r=0.9)
        m.add(t, DARK, "silk", SHELL, group=g)
    _stone(m, (0, -0.9, -3.5), 0.65)
    return m


@model
def coat_smuggler():
    """Skitch's Smuggling Coat: a long coat with a wide collar and tails, two rows of lined inner pouches showing at the open front, a scarf, a rolled map tube and brass buttons."""
    m = WModel("coat_smuggler", "body")
    col = Part("collar")
    L.band(col, -1.8, 1.0, 4.9, 2.95, 0.5, 10, 0.1, 0.5, 3.4)
    m.add(col, MAIN, "leather", SHELL)
    g = m.group("skirt", "swing", (0, -11.0, 0.0), 6.0, 0.9, 0.0)
    for sx in (-1, 1):
        def fn(u, v, sx=sx):
            a = math.pi * u
            return (sx * (4.9 + 0.9 * v) * math.sin(a), -11.0 - 10.0 * v, -(2.75 + 0.7 * v) * math.cos(a))
        p = Part("tail%d" % sx)
        L.sheet(p, fn, 5, 3, 0.35 * -sx)
        m.add(p, MAIN, "leather", SHELL, group=g)
    for row in range(2):
        for k in range(3):
            pk = Part("pouch%d_%d" % (row, k))
            L.sph(pk, (-2.6 + 2.6 * k, -4.4 - 3.0 * row, -3.1), 0.95, 3, 7, squash=0.8)
            m.add(pk, TRIM, "leather", 0)
            fl = Part("flap%d_%d" % (row, k))
            loft(fl, "z", (-2.6 + 2.6 * k, -3.7 - 3.0 * row, -3.45), [(0.0, 1.0, 1.0), (-0.3, 1.0, 1.0)], octagon(2.0, 0.9, 0.3))
            m.add(fl, MAIN, "leather", 0, uv="fit")
    sc = Part("scarf")
    L.band(sc, -0.6, 1.2, 3.6, 2.6, 0.5, 10, 0.0, 0.0, 3.0)
    m.add(sc, ACCENT, "stripe", SHELL)
    tl = Part("scarf_tail")
    loft(tl, "-y", (-2.2, 0.2, -2.9), [(0.0, 0.7, 0.2), (4.0, 0.7, 0.2), (5.2, 0.1, 0.1)], octagon(1.0, 1.0, 0.25))
    m.add(tl, ACCENT, "stripe")
    tb = Part("tube")
    L.cone(tb, (3.0, -9.0, 3.4), (-3.0, 1.0, 3.4), 0.85, 8, mid=0.5, mid_r=1.0)
    m.add(tb, TRIM, "wood")
    for k in range(3):
        b = Part("button%d" % k)
        L.dia(b, (0.0, -3.0 - k * 2.4, -3.0), 0.3, 0.3, 0.22)
        m.add(b, TRIM, "gilt", uv="fit")
    return m


@model
def chest_crystal():
    """The Skycrystal Armor: a cluster of tall cut crystals growing from the chest, a ring of small shards round the neck, a lit core, and two crystal fins behind the shoulders."""
    m = WModel("chest_crystal", "body")
    _gorget(m, tile="crystal", slot=ACCENT, trim_tile="gilt")
    for k, (x, y, h, r) in enumerate(((0, -5.0, 5.2, 1.1), (-1.8, -5.6, 3.8, 0.8), (1.8, -5.6, 3.8, 0.8), (-3.0, -7.4, 2.6, 0.6), (3.0, -7.4, 2.6, 0.6))):
        c = Part("crystal%d" % k)
        L.dia(c, (x, y + h / 2 * 0.0, -3.4), r, h / 2, r * 0.9)
        m.add(c, ACCENT if k else GLOW, "crystal", SHELL if k else EMIT | HOT, uv="fit")
    for k in range(8):
        a = 2 * math.pi * k / 8
        s = Part("shard%d" % k)
        L.dia(s, (4.2 * math.cos(a), 1.6, 2.7 * math.sin(a)), 0.55, 1.5 + 0.4 * (k % 2), 0.55)
        m.add(s, ACCENT, "crystal", SHELL, uv="fit")
    g = m.group("fins", "bob", (0, -3.0, 4.0), 0.4, 1.0, 0.0)
    for sx in (-1, 1):
        for k in range(3):
            f = Part("fin%d_%d" % (sx, k))
            L.dia(f, (sx * (2.0 + 1.6 * k), -2.0 + 2.6 * k, 4.4 + 0.6 * k), 0.6, 2.2 + 0.8 * k, 0.5)
            m.add(f, ACCENT, "crystal", SHELL, group=g, uv="fit")
    return m


@model
def pauldron_crystal():
    """A cluster of crystal shards rising over the shoulder."""
    m = WModel("pauldron_crystal", "rarm")
    b = Part("base")
    L.sph(b, (-1.0, 1.0, 0.0), 3.0, 3, 8, squash=0.5)
    m.add(b, MAIN, "plate", SHELL)
    for k, (dx, dz, h) in enumerate(((0, 0, 4.2), (-2.2, 0.8, 3.0), (2.0, -0.6, 3.2), (-0.6, -2.0, 2.4), (-1.4, 2.0, 2.6))):
        c = Part("shard%d" % k)
        L.dia(c, (-1.0 + dx, 2.4 + h / 2, dz), 0.8, h / 2, 0.8)
        m.add(c, ACCENT if k else GLOW, "crystal", SHELL if k else EMIT | HOT, uv="fit")
    return m


@model
def chest_slime():
    """The Slime Armour: wet gelatinous blobs piled over the chest and belly, a thick dripping collar, sagging drips down the front and a bulge on the back."""
    m = WModel("chest_slime", "body")
    for k, (x, y, z, r) in enumerate(((-2.4, -3.0, -3.0, 2.0), (2.4, -3.2, -3.0, 2.0), (0, -6.4, -3.1, 2.4), (-2.6, -9.2, -3.0, 1.7), (2.6, -9.2, -3.0, 1.7), (0, -10.6, -3.0, 1.5))):
        b = Part("blob%d" % k)
        L.sph(b, (x, y, z), r, 3, 8, squash=0.75)
        m.add(b, MAIN, "pearl", SHELL)
    col = Part("collar")
    L.band(col, -1.6, 1.2, 4.9, 3.0, 0.9, 10, 0.2, 0.5, 2.6)
    m.add(col, MAIN, "pearl", SHELL)
    for k in range(9):
        a = 2 * math.pi * k / 9
        x, z = 4.7 * math.cos(a), 3.0 * math.sin(a)
        d = Part("drip%d" % k)
        L.cone(d, (x, -1.0, z), (x * 1.05, -4.0 - (k % 3) * 1.4, z * 1.05), 0.7, 5, mid=0.55, mid_r=0.8)
        m.add(d, ACCENT, "pearl", SHELL)
    cr = Part("core")
    L.sph(cr, (0, -6.4, -4.4), 0.9, 3, 6)
    m.add(cr, GLOW, "gem", EMIT | HOT, uv="fit")
    bk = Part("back")
    L.sph(bk, (0, -5.0, 3.4), 3.8, 3, 8, squash=1.0)
    m.add(bk, MAIN, "pearl", SHELL)
    return m


@model
def pauldron_slime():
    """A big wet blob on the shoulder with two drips."""
    m = WModel("pauldron_slime", "rarm")
    b = Part("blob")
    L.sph(b, (-1.0, 1.4, 0.0), 3.5, 3, 8, squash=0.75)
    m.add(b, MAIN, "pearl", SHELL)
    for k in range(2):
        d = Part("drip%d" % k)
        L.cone(d, (-2.0 + 2.0 * k, 0.0, 2.0 - 4.0 * k), (-2.0 + 2.0 * k, -3.2, 2.2 - 4.4 * k), 0.8, 5, mid=0.55, mid_r=0.8)
        m.add(d, ACCENT, "pearl", SHELL)
    return m


@model
def chest_soul():
    """The Soul Armor: a ribcage of bone plates over the chest with a spine ridge, a collar of small skulls' teeth, and a flame of a soul burning in the sternum."""
    m = WModel("chest_soul", "body")
    for k in range(4):
        y = -2.0 - 2.4 * k
        for sx in (-1, 1):
            rb = Part("rib%d_%d" % (k, sx))
            pts = L.bez((sx * 0.5, y, -3.0), (sx * 3.4, y + 0.2, -3.3), (sx * 4.6, y - 0.8, -0.5), 4)
            sweep(rb, pts, circle(0.55, 4), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
            m.add(rb, MAIN, "bone", SHELL)
    st = Part("sternum")
    loft(st, "y", (0, -1.4, -3.3), [(0.0, 0.5, 0.35), (-9.0, 0.5, 0.35), (-10.2, 0.0, 0.0)], octagon(1.2, 1.0, 0.3))
    m.add(st, MAIN, "bone", SHELL)
    col = Part("collar")
    L.band(col, -1.0, 1.4, 4.9, 3.0, 0.6, 10, 0.2, 0.4, 3.0)
    m.add(col, MAIN, "bone", SHELL)
    for k in range(8):
        a = 2 * math.pi * k / 8
        t = Part("tooth%d" % k)
        L.cone(t, (4.9 * math.cos(a), 1.4, 3.0 * math.sin(a)), (5.3 * math.cos(a), 3.3, 3.2 * math.sin(a)), 0.45, 4, mid=0.4, mid_r=0.9)
        m.add(t, TRIM, "bone", SHELL)
    g = m.group("soul", "bob", (0, -5.6, -3.6), 0.5, 1.2, 0.0)
    fl = Part("flame")
    L.dia(fl, (0, -5.6, -3.9), 0.9, 1.7, 0.7)
    m.add(fl, GLOW, "flame", EMIT | HOT, group=g, uv="fit")
    return m


@model
def suit_astral():
    """The Suit of the Astral Overseer: a stand collar with a star chart disc at the chest, a tailcoat with silver piping and a ring of seven small stars turning round the wearer."""
    m = WModel("suit_astral", "body")
    base = A.MODELS["chest_coat"]()
    for (part, slot, tile, flags, group, uvm, dens) in base.items:
        m.items.append((part, slot, tile, flags, 0 if group == 0 else m.group(base.groups[group][0], base.groups[group][1], base.groups[group][2], *base.groups[group][3:]), uvm, dens))
    ch = Part("chart")
    loft(ch, "-z", (0, -6.2, -3.05), [(0.0, 1.0, 1.0), (0.45, 1.0, 1.0)], L.ml.circle(2.6, 14), caps=(False, True))
    m.add(ch, MAIN, "rune", SHELL, uv="fit")
    for k in range(5):
        a = math.pi / 2 + 2 * math.pi * k / 5
        s = Part("star%d" % k)
        L.dia(s, (1.6 * math.cos(a), -6.2 + 1.6 * math.sin(a), -3.7), 0.38, 0.38, 0.3)
        m.add(s, GLOW, "gem", EMIT | HOT, uv="fit")
    g = m.group("orbit", "orbit", (0, -6.0, 0.0), 2.4, 0.0, 0.0)
    for k in range(7):
        a = 2 * math.pi * k / 7
        s = Part("orb%d" % k)
        L.dia(s, (7.4 * math.cos(a), -6.0 + 1.6 * math.sin(k * 2.3), 7.4 * math.sin(a) * 0.75), 0.5, 0.7, 0.5)
        m.add(s, GLOW, "spark", EMIT | HOT, group=g, uv="fit")
    return m


@model
def chest_prophet():
    """Z'ar Sancuis, Plate of the Red Prophet: a massive plated gorget, a red tabard from the chest to the knee with a gilt eye emblem, a studded belt of plates and a red mantle behind."""
    m = WModel("chest_prophet", "body")
    _gorget(m, h=3.6, r=5.3)
    g = m.group("tabard", "swing", (0, -3.0, -3.0), 8.0, 0.9, 0.0)
    tb = Part("tabard")
    L.sheet(tb, lambda u, v: ((u - 0.5) * 6.4 * (1 - 0.18 * v), -3.4 - v * 17.0, -3.1 - 0.5 * v), 3, 5, 0.4)
    m.add(tb, ACCENT, "silk", SHELL, group=g)
    ed = Part("eye_edge")
    L.sheet(ed, lambda u, v: ((u - 0.5) * 6.4 * 0.82, -20.4 - 0.3 * v, -3.6), 3, 1, 0.4)
    m.add(ed, TRIM, "gilt", 0, group=g)
    ey = Part("eye")
    loft(ey, "-z", (0, -8.0, -3.75), [(0.0, 1.0, 1.0), (0.4, 1.0, 1.0)], [(-2.0, 0.0), (0.0, 1.1), (2.0, 0.0), (0.0, -1.1)][::-1], caps=(False, True))
    m.add(ey, TRIM, "gilt", SHELL, group=g, uv="fit")
    ir = Part("iris")
    L.dia(ir, (0, -8.0, -4.4), 0.8, 0.8, 0.4)
    m.add(ir, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    for k in range(4):
        bt = Part("belt%d" % k)
        loft(bt, "y", (-3.6 + 2.4 * k, -9.6, -3.0), [(0.0, 1.0, 1.0), (-1.8, 1.0, 1.0)], octagon(2.2, 1.0, 0.3))
        m.add(bt, MAIN, "plate", SHELL)
    gm = m.group("mantle", "swing", (0, -1.0, 3.0), 12.0, 1.1, 0.0)
    mt = Part("mantle")
    L.sheet(mt, lambda u, v: ((u - 0.5) * (8.8 + 2.0 * v), -1.4 - v * 17.0, 3.2 + 0.7 * v + 0.7 * math.sin(u * math.pi) * v), 4, 4, -0.4)
    m.add(mt, ACCENT, "silk", SHELL, group=gm)
    return m


@model
def pauldron_prophet():
    """A spiked pauldron of red-gilt plates: three layered plates with a long spike on the outer edge and a hanging red tassel."""
    m = WModel("pauldron_prophet", "rarm")
    for k, (y, rr, hh) in enumerate(((1.2, 3.7, 1.4), (-0.6, 3.5, 1.3), (-2.3, 3.1, 1.2))):
        pl = Part("plate%d" % k)
        loft(pl, "y", (-1.0, y, 0.0), [(0.0, 0.88, 0.88), (-hh * 0.4, 1.0, 1.0), (-hh, 0.95, 0.95)], octagon(2 * rr, 2 * rr * 0.92, rr * 0.55), caps=(True, False))
        m.add(pl, MAIN if k != 1 else TRIM, "plate" if k != 1 else "gilt", SHELL)
    sp = Part("spike")
    L.cone(sp, (-4.0, 2.0, 0.0), (-6.4, 6.4, -0.6), 1.0, 5, mid=0.4, mid_r=0.8)
    m.add(sp, TRIM, "gilt")
    g = m.group("tassel", "swing", (-1.0, -3.5, 0.0), 6.0, 0.9, 0.0)
    ts = Part("tassel")
    loft(ts, "-y", (-1.0, -3.4, -2.8), [(0.0, 0.5, 0.2), (3.0, 0.6, 0.2), (3.8, 0.0, 0.0)], octagon(1.0, 1.0, 0.25))
    m.add(ts, ACCENT, "silk", 0, group=g)
    _stone(m, (-1.0, 2.2, -2.5), 0.45)
    return m


# ================================================================ feet
@model
def boots_mud():
    """Mudwalker boots: broad flat soles with deep lugs, a wide cuff, clumps of caked mud up the shin and a lit toggle."""
    m = WModel("boots_mud", "rleg")
    sh = Part("shaft")
    loft(sh, "y", (0, -5.0, 0), [(0.0, 1.0, 1.0), (-3.0, 1.0, 1.0), (-6.6, 1.06, 1.06)], A._rr(4.8, 4.8, 1.3, 2), caps=(False, True))
    m.add(sh, MAIN, "leather", SHELL)
    cf = Part("cuff")
    loft(cf, "y", (0, -4.6, 0), [(0.0, 1.0, 1.0), (-0.4, 1.12, 1.12), (-1.8, 1.12, 1.12), (-2.2, 1.0, 1.0)], A._rr(5.2, 5.2, 1.4, 2), caps=(False, False))
    m.add(cf, TRIM, "leather", SHELL)
    tp = Part("toe")
    loft(tp, "-z", (0, -10.8, -2.0), [(0.0, 1.0, 1.0), (1.6, 1.0, 0.95), (3.0, 0.9, 0.7), (3.6, 0.0, 0.0)], octagon(5.4, 2.8, 1.0), caps=(False, True))
    m.add(tp, MAIN, "leather", SHELL)
    sl = Part("sole")
    loft(sl, "y", (0, -11.9, -0.8), [(0.0, 1.0, 1.0), (-1.1, 1.0, 1.0)], octagon(5.8, 8.4, 1.2))
    m.add(sl, DARK, "leather")
    for k in range(4):
        lg = Part("lug%d" % k)
        loft(lg, "y", (0, -12.0 + 0.0, -3.0 + 2.0 * k), [(0.0, 1.0, 1.0), (0.6, 1.0, 1.0)], octagon(6.0, 0.7, 0.2), caps=(True, True))
        m.add(lg, DARK, "wood")
    for k, (x, y, z, r) in enumerate(((-2.3, -6.0, -1.2, 0.9), (2.3, -8.0, 0.8, 0.8), (-2.2, -9.0, 1.4, 1.0), (2.4, -5.2, -1.4, 0.7))):
        mu = Part("mud%d" % k)
        L.sph(mu, (x, y, z), r, 2, 6, squash=0.8)
        m.add(mu, DARK, "snow", 0)
    _stone(m, (0, -7.0, -2.8), 0.4)
    return m


@model
def boots_water():
    """Waterwalker boots: sleek blue-lit boots with a fin at each ankle and a glowing ripple ring under each sole that turns slowly."""
    m = WModel("boots_water", "rleg")
    sh = Part("shaft")
    loft(sh, "y", (0, -3.4, 0), [(0.0, 1.0, 1.0), (-3.0, 0.97, 0.97), (-6.6, 1.02, 1.02), (-8.0, 1.0, 1.0)], A._rr(4.75, 4.75, 1.3, 2), caps=(False, True))
    m.add(sh, MAIN, "scale", SHELL)
    cf = Part("cuff")
    L.band(cf, -3.8, -3.0, 2.55, 2.55, 0.3, 10, 0.0, 0.0, 3.2)
    m.add(cf, TRIM, "gilt")
    tp = Part("toe")
    loft(tp, "-z", (0, -11.0, -2.0), [(0.0, 1.0, 1.0), (1.2, 0.98, 0.9), (2.4, 0.78, 0.6), (2.9, 0.0, 0.0)], octagon(4.7, 2.7, 0.9), caps=(False, True))
    m.add(tp, MAIN, "scale", SHELL)
    sl = Part("sole")
    loft(sl, "y", (0, -11.9, -0.4), [(0.0, 1.0, 1.0), (-0.7, 1.0, 1.0)], octagon(4.9, 7.4, 0.9))
    m.add(sl, DARK, "leather")
    for sx in (-1, 1):
        f = Part("fin%d" % sx)
        L.feather(f, (sx * 2.4, -7.6, 0.6), (sx * 5.2, -5.2, 2.6), 0.9, 0.22, 4)
        m.add(f, ACCENT, "vein", SHELL, uv="fit")
    g = m.group("ripple", "spin", (0, -12.4, 0.0), 1.2, 0.0, 0.0)
    for k, rr in enumerate((3.3, 4.6)):
        rg = Part("ripple%d" % k)
        L.torus(rg, (0, -12.4 - 0.2 * k, 0.0), rr, 0.22, 14, 4, plane="xz", a_scale=1.0, b_scale=1.5)
        m.add(rg, GLOW, "soft", EMIT | HOT, group=g, uv="fit")
    return m


def catalog_relics(cat):
    """The signature entries of the armour relics (the relic ids of the Java defs)."""
    cat.relic("arcane_conduction_armor", "chest_conduction", "coil_arm@rarm", "coil_arm@larm~")
    cat.relic("armor_of_plot", "chest_plot", "pauldron_plate@rarm", "pauldron_plate@larm~", "deco_halo_back")
    cat.relic("armor_of_the_defiant_dawn", "chest_dawn", "pauldron_rays@rarm", "pauldron_rays@larm~")
    cat.relic("armour_of_the_stone_sentinel", "chest_stone", "pauldron_stone@rarm", "pauldron_stone@larm~")
    cat.relic("mistwalker_robe", "chest_mist", "sleeves_flared@rarm", "sleeves_flared@larm~", "deco_wisps")
    cat.relic("nekets_practical_mage_robe", "chest_neket", "sleeve_plain@rarm", "sleeve_plain@larm~")
    cat.relic("shadow_robes", "chest_shadow", "hood_shadow", "sleeves_flared@rarm", "sleeves_flared@larm~")
    cat.relic("skitchs_smuggling_coat", "coat_smuggler")
    cat.relic("skycrystal_armor", "chest_crystal", "pauldron_crystal@rarm", "pauldron_crystal@larm~")
    cat.relic("slime_armour", "chest_slime", "pauldron_slime@rarm", "pauldron_slime@larm~")
    cat.relic("soul_armor", "chest_soul", "pauldron_plate@rarm", "pauldron_plate@larm~", "deco_wisps")
    cat.relic("suit_of_the_astral_overseer", "suit_astral")
    cat.relic("zar_sancuis_plate_of_the_red_prophet", "chest_prophet", "pauldron_prophet@rarm", "pauldron_prophet@larm~")
    cat.relic("boots_of_the_indomitable", "boots_war@rleg", "boots_war@lleg")
    cat.relic("mudwalkers", "boots_mud@rleg", "boots_mud@lleg")
    cat.relic("waterwalkers", "boots_water@rleg", "boots_water@lleg")
    for rid, th in (("skycrystal_armor", "snow"), ("slime_armour", "wisp"), ("soul_armor", "wisp"), ("shadow_robes", "wisp"), ("mistwalker_robe", "wisp"),
                    ("armor_of_the_defiant_dawn", "star"), ("suit_of_the_astral_overseer", "star"), ("arcane_conduction_armor", "spark"),
                    ("zar_sancuis_plate_of_the_red_prophet", "ember"), ("waterwalkers", "snow"), ("armour_of_the_stone_sentinel", "spark")):
        cat.theme(rid, th)
