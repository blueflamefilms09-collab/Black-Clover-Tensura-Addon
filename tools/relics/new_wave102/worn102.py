"""The worn models of the gear of Wave102 (release 1.111.0), built with the worn relic library (tools/relics/gen_worn_lib.py).

GOD (4): a signature ensemble each, multi-part, with emissive and pulsing glow parts and animated groups (orbit, spin, bob, sway) the library draws with its glow / halo / shimmer
passes: `w102_susanoo_*` (the Perfect Susanoo Plate: cuirass, ribcage, collar, tassets, pauldrons), `w102_strawhat` (the Joyboy's Straw Hat: brim, crown, red band, a sun halo and
rays), `w102_binder` (the Greed Island Binder: a book at the hip with latch and orbiting cards), `w102_ladybug` (the Requiem Ladybug Brooch: a gold shell with spots, a pin, a rune
ring and orbiting motes).
SPECIAL (16): the look of the slot plus the small unique ornament of the generated waves (`uq_<id>`, tools/relics/gen_worn_unique.py: a few motifs picked and sized by the id).

gen_worn_all.py calls `register(BUILDERS)` and `catalog(cat)` here (two append-only hook lines).
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
sys.path.insert(0, HERE)
import gen_worn_lib as L  # noqa: E402
from gen_worn_lib import WModel, Part, loft, lathe, sweep, circle, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT  # noqa: E402
import gen_worn_unique as UQ  # noqa: E402
from roster102 import GEAR, WORN_GOD  # noqa: E402

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


def arc(cx, cy, cz, rx, rz, a0, a1, n, dy=0.0):
    """Points of an arc in the xz plane about (cx, cz) at height cy (a rib across the chest or back), dy drops the middle."""
    out = []
    for i in range(n + 1):
        t = a0 + (a1 - a0) * i / n
        out.append((cx + rx * math.cos(t), cy - dy * math.sin(t) ** 2, cz + rz * math.sin(t)))
    return out


# ================================================================ Perfect Susanoo Plate (CHEST)
@model
def w102_susanoo_body():
    """The Perfect Susanoo Plate, body: a violet plated cuirass, a ribcage of glowing chakra ribs over the chest and back, a spiked gorget with a sigil, hanging tassets and chakra motes."""
    m = WModel("w102_susanoo_body", "body")
    cu = Part("cuirass")
    L.band(cu, -11.0, -0.6, 4.2, 2.3, 0.55, 14, 0.1, 0.3, 3.4)
    m.add(cu, MAIN, "plate", SHELL)
    tr = Part("cuirass_trim")
    L.band(tr, -6.2, -5.85, 4.25, 2.35, 0.7, 14, 0.0, 0.0, 3.4)
    m.add(tr, TRIM, "gilt")
    for side, ribs in (("front", 3), ("back", 2)):
        rib = Part("ribs_" + side)
        for k in range(ribs):
            y = -2.4 - 2.6 * k
            pts = arc(0.0, y, 0.0, 5.0 - 0.25 * k, 3.2 * (1 if side == "back" else -1), math.radians(-70), math.radians(70), 4, dy=0.4)
            sweep(rib, pts, circle(0.3, 4), scales=[(0.5, 0.5)] + [(1.0, 1.0)] * 3 + [(0.5, 0.5)], caps=(True, True))
        m.add(rib, GLOW, "flame", EMIT, uv="fit")
    sp = Part("spine")
    sweep(sp, [(0.0, -0.8, 3.2), (0.0, -6.0, 3.5), (0.0, -11.0, 3.2)], circle(0.4, 4), scales=[(1.0, 1.0)] * 3, caps=(True, True))
    m.add(sp, GLOW, "flame", EMIT, uv="fit")
    gt = Part("gorget")
    L.torus(gt, (0, 0.6, 0), 4.5, 0.55, 10, 4, plane="xz")
    m.add(gt, TRIM, "gilt")
    spk = Part("gorget_spikes")
    for k in range(5):
        th = math.pi * 2 * k / 5
        x, z = 4.6 * math.cos(th), 3.0 * math.sin(th)
        L.cone(spk, (x, 0.8, z), (x * 1.15, 4.0 + (k % 2), z * 1.15), 0.5, 4, mid=0.4, mid_r=0.9)
    m.add(spk, ACCENT, "plate")
    sg = Part("sigil")
    L.ml.gem(sg, (0, -4.6, -2.95), 0.95, 1.0, 6, "z")
    m.add(sg, GLOW, "gem", EMIT | HOT, uv="fit")
    for k, x in enumerate((-2.6, 0.0, 2.6)):
        ts = Part("tasset%d" % k)
        loft(ts, "y", (x, -11.0, -2.5), [(0.0, 1.0, 1.0), (-2.4, 0.95, 1.0), (-4.2, 0.7, 1.0)], L.rect_ring(1.8, 0.5))
        m.add(ts, MAIN if k % 2 == 0 else TRIM, "plate" if k % 2 == 0 else "gilt", SHELL)
    g = m.group("chakra", "orbit", (0, -6.0, 0), 1.4, 0.0, 0.0)
    for k in range(4):
        a = 2 * math.pi * k / 4
        mt = Part("mote%d" % k)
        L.dia(mt, (6.4 * math.cos(a), -4.0 - 1.2 * math.sin(a * 2), 5.0 * math.sin(a)), 0.4, 0.7, 0.4)
        m.add(mt, GLOW, "spark", EMIT | HOT, group=g, uv="fit")
    return m


@model
def w102_susanoo_pauldron():
    """The Perfect Susanoo Plate, shoulder: three arched plates in violet and gilt, a horn over the outer edge and a glowing rib arc."""
    m = WModel("w102_susanoo_pauldron", "rarm")
    for k, (y, rr, hh) in enumerate(((1.4, 3.8, 1.5), (-0.4, 3.6, 1.4), (-2.1, 3.2, 1.3))):
        pl = Part("plate%d" % k)
        loft(pl, "y", (-1.0, y, 0.0), [(0.0, 0.88, 0.88), (-hh * 0.4, 1.0, 1.0), (-hh, 0.95, 0.95)], L.octagon(2 * rr, 2 * rr * 0.92, rr * 0.55), caps=(True, False))
        m.add(pl, MAIN if k != 1 else TRIM, "plate" if k != 1 else "gilt", SHELL)
    hn = Part("horn")
    L.cone(hn, (-4.2, 2.4, 0.0), (-5.8, 7.4, -0.6), 0.8, 4, mid=0.4, mid_r=0.8)
    m.add(hn, ACCENT, "horn")
    rb = Part("rib_arc")
    sweep(rb, arc(-1.0, 3.2, 0.0, 3.6, 3.0, math.radians(-100), math.radians(100), 4), circle(0.28, 4), scales=[(0.4, 0.4)] + [(1.0, 1.0)] * 3 + [(0.4, 0.4)], caps=(True, True))
    m.add(rb, GLOW, "flame", EMIT, uv="fit")
    gm = Part("stone")
    L.dia(gm, (-1.0, 3.6, -3.1), 0.5, 0.55, 0.4)
    m.add(gm, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


# ================================================================ Joyboy's Straw Hat (HEAD)
@model
def w102_strawhat():
    """Joyboy's Straw Hat: a wide woven brim and a round crown, a red band with a knot, a chin cord, and behind the head a sun halo with turning rays and drifting sparks."""
    m = WModel("w102_strawhat", "head")
    br = Part("brim")
    lathe(br, "y", (0, 6.4, 0), [(-0.1, 4.6), (0.0, 7.2), (0.4, 10.0), (0.9, 10.6), (1.1, 9.8), (0.9, 4.6)], sides=18)
    m.add(br, MAIN, "wood", SHELL)
    cr = Part("crown")
    lathe(cr, "y", (0, 7.2, 0), [(0.0, 4.7), (1.0, 4.9), (2.6, 4.5), (4.0, 3.4), (4.9, 1.8), (5.2, 0.0)], sides=16)
    m.add(cr, MAIN, "wood", SHELL)
    bd = Part("band")
    L.band(bd, 7.2, 8.9, 4.88, 4.88, 0.2, 16, 0.0, 0.0, 2.0)
    m.add(bd, ACCENT, "cloth")
    kn = Part("knot")
    L.sph(kn, (0, 8.0, -5.2), 0.9, 3, 6)
    m.add(kn, ACCENT, "cloth")
    for sx in (-1, 1):
        tl = Part("tail%d" % sx)
        sweep(tl, [(sx * 0.6, 8.0, -5.4), (sx * 1.6, 6.4, -5.6), (sx * 2.2, 4.2, -5.4)], circle(0.4, 4), scales=[(1.0, 1.0), (0.9, 0.9), (0.3, 0.3)], caps=(True, True))
        m.add(tl, ACCENT, "cloth")
    for sx in (-1, 1):
        cc = Part("chin_cord%d" % sx)
        sweep(cc, [(sx * 9.6, 6.8, 0.0), (sx * 5.0, 3.0, -1.0), (sx * 2.0, -0.6, -3.6)], circle(0.14, 4), scales=[(1.0, 1.0)] * 3, caps=(True, True))
        m.add(cc, DARK, "leather")
    g = m.group("sun", "spin", (0, 9.5, 6.2), 0.6, 0.0, 0.0)
    ha = Part("sun_ring")
    L.torus(ha, (0, 9.5, 6.2), 6.6, 0.3, 16, 4, plane="xy")
    m.add(ha, GLOW, "flame", EMIT, group=g, uv="fit")
    rays = Part("sun_rays")
    for k in range(12):
        a = 2 * math.pi * k / 12
        L.cone(rays, (6.9 * math.cos(a), 9.5 + 6.9 * math.sin(a), 6.2), (9.2 * math.cos(a), 9.5 + 9.2 * math.sin(a), 6.2), 0.4, 3, mid=0.3, mid_r=1.0)
    m.add(rays, GLOW, "ember", EMIT | HOT, group=g, uv="fit")
    g2 = m.group("sparks", "orbit", (0, 9.0, 0), -1.6, 0.0, 0.0)
    for k in range(4):
        a = 2 * math.pi * k / 4
        sp = Part("spark%d" % k)
        L.dia(sp, (11.6 * math.cos(a), 8.2 + 0.8 * math.sin(a * 2), 11.6 * math.sin(a)), 0.35, 0.6, 0.35)
        m.add(sp, GLOW, "spark", EMIT | HOT, group=g2, uv="fit")
    return m


# ================================================================ Greed Island Binder (CHARM)
@model
def w102_binder():
    """The Greed Island Binder: a big book on a chain at the left hip, thick covers with gilt corners, a latch and a glowing emblem, and a ring of cards circling it."""
    m = WModel("w102_binder", "body")
    g = m.group("swing", "swing", (4.8, -8.6, 0.0), 4.0, 0.6, 0.0)
    ch = Part("chain")
    sweep(ch, L.bez((3.9, -8.4, 0.8), (6.6, -9.4, -0.3), (5.9, -12.0, -0.8), 5), circle(0.2, 4), scales=[(1.0, 1.0), (0.6, 0.6)] * 3, caps=(True, True))
    m.add(ch, TRIM, "chain", 0, group=g)
    cv = Part("cover")
    loft(cv, "y", (5.9, -11.8, -1.0), [(0.0, 1.0, 1.0), (-5.2, 1.0, 1.0)], L.octagon(4.0, 1.5, 0.3))
    m.add(cv, MAIN, "leather", SHELL, group=g)
    pg = Part("pages")
    loft(pg, "y", (5.95, -12.0, -1.0), [(0.0, 1.0, 1.0), (-4.8, 1.0, 1.0)], L.octagon(3.6, 1.3, 0.1))
    m.add(pg, NONE, "paper", 0, group=g)
    sp = Part("spine")
    loft(sp, "y", (4.0, -11.8, -1.0), [(0.0, 1.0, 1.0), (-5.2, 1.0, 1.0)], L.octagon(0.6, 1.7, 0.2))
    m.add(sp, TRIM, "gilt", 0, group=g)
    for k, (dx, dy) in enumerate(((1.9, 0.0), (-1.9, 0.0), (1.9, -5.2), (-1.9, -5.2))):
        co = Part("corner%d" % k)
        L.dia(co, (5.9 + dx, -11.8 - dy * 1.0 + (0.1 if dy == 0 else -0.1), -1.8), 0.5, 0.5, 0.4)
        m.add(co, TRIM, "gilt", 0, group=g, uv="fit")
    lt = Part("latch")
    loft(lt, "y", (5.9, -16.0, -1.95), [(0.0, 1.0, 1.0), (-1.3, 1.0, 1.0)], L.octagon(1.2, 0.3, 0.1))
    m.add(lt, TRIM, "gilt", 0, group=g)
    em = Part("emblem")
    L.dia(em, (5.9, -13.9, -2.1), 1.0, 1.2, 0.4)
    m.add(em, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    g2 = m.group("cards", "orbit", (5.9, -13.5, -1.0), 1.6, 0.0, 0.0)
    for k in range(6):
        a = 2 * math.pi * k / 6
        cd = Part("card%d" % k)
        c = (5.9 + 4.6 * math.cos(a), -13.5 + 0.6 * math.sin(a * 2), -1.0 + 4.6 * math.sin(a))
        L.ml.box(cd, (c[0] - 0.7, c[1] - 1.0, c[2] - 0.08), (c[0] + 0.7, c[1] + 1.0, c[2] + 0.08), 0.0)
        m.add(cd, GLOW, "paper", EMIT | HOT, group=g2, uv="fit")
    return m


# ================================================================ Requiem Ladybug Brooch (CHARM)
@model
def w102_ladybug():
    """The Requiem Ladybug Brooch: a gold ladybug on the chest, a split shell with six black spots, a small head, a glowing eye, an arrow-shaped pin, a rune ring behind it and motes circling."""
    m = WModel("w102_ladybug", "body")
    sh = Part("shell")
    L.sph(sh, (2.6, -3.4, -2.7), 1.9, 4, 10, squash=0.55)
    sh.V = [np.array(v, float) * np.array([1.0, 1.15, 1.0]) + np.array([0.0, -0.45, 0.0]) for v in sh.V]
    m.add(sh, MAIN, "gilt", SHELL)
    sl = Part("split")
    sweep(sl, [(2.6, -1.4, -3.45), (2.6, -3.4, -3.9), (2.6, -5.6, -3.45)], circle(0.1, 4), scales=[(1.0, 1.0)] * 3, caps=(True, True))
    m.add(sl, NONE, "black")
    for k, (dx, dy) in enumerate(((-0.9, -2.2), (0.9, -2.2), (-1.1, -3.6), (1.1, -3.6), (-0.7, -4.9), (0.7, -4.9))):
        sp = Part("spot%d" % k)
        L.dia(sp, (2.6 + dx, -3.4 + dy + 0.4, -3.55), 0.34, 0.34, 0.15)
        m.add(sp, NONE, "black", uv="fit")
    hd = Part("head")
    L.sph(hd, (2.6, -1.0, -3.0), 0.8, 3, 8, squash=0.7)
    m.add(hd, NONE, "black")
    for sx in (-1, 1):
        an = Part("antenna%d" % sx)
        sweep(an, [(2.6 + sx * 0.3, -0.5, -3.1), (2.6 + sx * 0.9, 0.3, -3.2), (2.6 + sx * 1.3, 0.8, -3.1)], circle(0.08, 4), scales=[(1.0, 1.0)] * 3, caps=(True, True))
        m.add(an, NONE, "black")
    ey = Part("eye")
    L.dia(ey, (2.6, -3.4, -4.1), 0.5, 0.6, 0.2)
    m.add(ey, GLOW, "gem", EMIT | HOT, uv="fit")
    pn = Part("pin")
    L.cone(pn, (2.6, -5.6, -2.9), (2.6, -9.0, -2.7), 0.35, 4, mid=0.3, mid_r=1.0)
    L.cone(pn, (2.6, -8.2, -2.8), (2.6, -9.8, -2.7), 0.8, 3, mid=0.4, mid_r=0.9)
    m.add(pn, MAIN, "gilt")
    g = m.group("rune", "spin", (2.6, -3.4, -3.2), 0.5, 0.0, 0.0)
    rn = Part("rune_ring")
    L.torus(rn, (2.6, -3.4, -3.2), 2.9, 0.12, 14, 4, plane="xy")
    m.add(rn, GLOW, "rune", EMIT, group=g, uv="fit")
    g2 = m.group("motes", "orbit", (2.6, -3.4, -3.0), 1.5, 0.0, 0.0)
    for k in range(5):
        a = 2 * math.pi * k / 5
        mt = Part("mote%d" % k)
        L.dia(mt, (2.6 + 3.7 * math.cos(a), -3.4 + 3.7 * math.sin(a) * 0.9, -3.4), 0.25, 0.4, 0.25)
        m.add(mt, GLOW, "spark", EMIT | HOT, group=g2, uv="fit")
    return m


# ================================================================ registration (called by gen_worn_all.py)
GOD_PIECES = {
    "perfect_susanoo_plate": ["w102_susanoo_body", "w102_susanoo_pauldron@rarm", "w102_susanoo_pauldron@larm~"],
    "joyboys_straw_hat": ["w102_strawhat"],
    "greed_island_binder": ["w102_binder"],
    "requiem_ladybug_brooch": ["w102_ladybug"],
}
GOD_THEMES = {"perfect_susanoo_plate": "wisp", "joyboys_straw_hat": "star", "greed_island_binder": "leaf", "requiem_ladybug_brooch": "star"}


def special_rows():
    """The 16 Special gear relics as the rows gen_worn_unique reads (id, slot, look)."""
    return [r for r in GEAR if r["grade"] == "SPECIAL"]


def register(builders):
    """Adds every model builder of this wave (the God signature models and the Special ornaments) to gen_worn_all's BUILDERS."""
    builders.update(MODELS)
    builders.update(UQ.models_for(special_rows()))


def catalog(cat):
    """Adds the worn entries: the signature ensembles of the God relics, the look + ornament of each Special relic."""
    for rid in WORN_GOD:
        cat.relic(rid, *GOD_PIECES[rid])
        cat.theme(rid, GOD_THEMES[rid])
    UQ.catalog_generated(cat, special_rows())


if __name__ == "__main__":
    # writes only this wave's model files (the full library is gen_worn_all.py): python3 -B tools/relics/new_wave102/worn102.py
    out = {}
    register(out)
    for name, fn in sorted(out.items()):
        L.write_model(fn())
    print("wrote %d worn models" % len(out))
