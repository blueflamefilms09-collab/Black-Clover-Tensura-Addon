"""The 3D pieces of the transformation overlays (client/mode/ModeArmorLayer, docs/mode_armor_spec.md): wings, crowns, horns and skirts of the five modes, built with the
worn-model method and drawn by the worn library next to the older painted plates. A mode is a catalog entry named mode_<enum name in lower case>."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


def _spread_wing(m, sx, group, root, span, height, ranks=3, per=5, slot=MAIN, slot2=ACCENT, tile="feather", flags=SHELL, bone_slot=TRIM, bone_tile="bone"):
    """One open wing: a wing bone sweeping out and up from the root, and `ranks` ranks of `per` feathers fanning from it (outer feathers longest)."""
    rx, ry, rz = root
    bone = Part("bone%d" % sx)
    pts = L.bez3((rx, ry, rz), (rx + sx * span * 0.35, ry + height * 0.55, rz + 0.8), (rx + sx * span * 0.75, ry + height * 0.9, rz + 1.2), (rx + sx * span, ry + height * 0.7, rz + 1.5), 6)
    sweep(bone, pts, circle(0.5, 4), scales=[(1.1, 1.1)] + [(1.0, 1.0)] * 4 + [(0.5, 0.5), (0.0, 0.0)][:1] + [(0.0, 0.0)], caps=(True, True))
    m.add(bone, bone_slot, bone_tile, SHELL, group=group)
    for r in range(ranks):
        for k in range(per):
            t = (k + 0.6) / (per + 0.2)
            bx, by, bz = L.bez3((rx, ry, rz), (rx + sx * span * 0.35, ry + height * 0.55, rz + 0.8), (rx + sx * span * 0.75, ry + height * 0.9, rz + 1.2),
                                (rx + sx * span, ry + height * 0.7, rz + 1.5), 20)[int(t * 20)]
            ln = (height * 1.15 - 3.0 * r) * (0.55 + 0.6 * t)
            tipx = bx + sx * (2.0 + 2.2 * t + r * 0.8)
            tipy = by - ln * (0.82 - 0.1 * t)
            f = Part("feather%d_%d_%d" % (sx, r, k))
            L.feather(f, (bx, by - 0.4 * r, bz + 0.3 * r), (tipx, tipy, bz + 0.9 + 0.4 * r), 0.8 + 0.12 * k, 0.26, 4)
            m.add(f, slot if (r + k) % 2 == 0 else slot2, tile, flags, group=group, uv="fit")


@model
def wings_wind():
    """Wind Spirit Dive wings: a pair of wide open wings of long blade feathers, pale wind-green with lit tips, beating slowly."""
    m = WModel("wings_wind", "body")
    for sx in (-1, 1):
        g = m.group("wing%d" % sx, "sway", (sx * 1.6, -2.0, 2.6), 5.0 * sx, 0.1, 0.0)
        _spread_wing(m, sx, g, (sx * 1.6, -2.0, 2.6), 13.0, 8.0, ranks=3, per=4)
    return m


@model
def crown_starblades():
    """Eight star blades standing in a ring over the head, point up, slowly turning: Yuno's crown of wind."""
    m = WModel("crown_starblades", "head")
    g = m.group("ring", "orbit", (0, 11.0, 0), 1.2, 0.0, 0.0)
    for k in range(8):
        a = 2 * math.pi * k / 8
        h = 7.0 if k % 2 == 0 else 5.0
        x, z = 4.8 * math.cos(a), 4.8 * math.sin(a)
        b = Part("blade%d" % k)
        L.dia(b, (x, 11.0 + h / 2, z), 0.9, h / 2, 0.35)
        b.V = [np.array([(v[0] - x) * math.cos(a + math.pi / 2) - (v[2] - z) * math.sin(a + math.pi / 2) + x, v[1],
                         (v[0] - x) * math.sin(a + math.pi / 2) + (v[2] - z) * math.cos(a + math.pi / 2) + z]) for v in b.V]
        m.add(b, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    r = Part("halo")
    L.torus(r, (0, 11.0, 0), 4.8, 0.18, 16, 4, plane="xz")
    m.add(r, TRIM, "gilt", EMIT, group=g, uv="fit")
    return m


@model
def wings_dragon():
    """Fire Spirit Dive wings: two dragon wings, each a bone arm with three finger bones and a flame-lit membrane stretched between them, torn at the edges."""
    m = WModel("wings_dragon", "body")
    for sx in (-1, 1):
        g = m.group("wing%d" % sx, "sway", (sx * 1.6, -2.0, 2.6), 4.0 * sx, 0.09, 0.0)
        root = (sx * 1.8, -2.0, 2.8)
        arm = Part("arm%d" % sx)
        pts = L.bez3(root, (sx * 5.0, 3.0, 3.4), (sx * 9.0, 6.8, 3.8), (sx * 12.5, 6.2, 4.0), 6)
        sweep(arm, pts, circle(0.6, 5), scales=[(1.15, 1.15)] + [(1.0, 1.0)] * 4 + [(0.55, 0.55), (0.0, 0.0)], caps=(True, True))
        m.add(arm, DARK, "horn", SHELL, group=g)
        tips = [(sx * 16.5, 1.0, 4.3), (sx * 15.0, -6.0, 4.0), (sx * 10.0, -11.0, 3.7), (sx * 4.5, -13.0, 3.3)]
        wrist = (sx * 12.5, 6.2, 4.0)
        for k, tip in enumerate(tips[:3]):
            fg = Part("finger%d_%d" % (sx, k))
            mid = ((wrist[0] + tip[0]) / 2 + sx * 0.6, (wrist[1] + tip[1]) / 2 + 0.5, wrist[2] + 0.3)
            fp = L.bez(wrist, mid, tip, 4)
            sweep(fg, fp, circle(0.34, 4), scales=[(1.0, 1.0), (0.9, 0.9), (0.7, 0.7), (0.4, 0.4), (0.0, 0.0)], caps=(True, True))
            m.add(fg, DARK, "horn", SHELL, group=g)
        edges = [root] + [tips[0], tips[1], tips[2], tips[3]]
        for k in range(len(edges) - 1):
            a, b = edges[k], edges[k + 1]
            mem = Part("membrane%d_%d" % (sx, k))
            L.quad_strip(mem, [wrist, a], [wrist, b], 0.25)
            m.add(mem, ACCENT, "flame", EMIT | SHELL, group=g, uv="fit")
    return m


@model
def flame_hair():
    """Seven tongues of flame licking up from the head, bent back, flickering: Salamander's hair."""
    m = WModel("flame_hair", "head")
    g = m.group("flames", "pulse", (0, 8.0, 0), 0.12, 1.4, 0.0)
    for k in range(7):
        a = math.radians(-75 + 25 * k + 180)
        x, z = 3.6 * math.cos(a), 3.6 * math.sin(a)
        h = 5.5 + 1.6 * ((k * 3) % 3)
        t = Part("tongue%d" % k)
        L.cone(t, (x, 7.8, z), (x * 1.5, 7.8 + h, z * 1.5 + 1.2), 1.1, 5, mid=0.4, mid_r=1.1)
        m.add(t, GLOW if k % 2 else ACCENT, "flame", EMIT | HOT, group=g, uv="fit")
    return m


@model
def horn_demon():
    """Asta's black horn: one jagged horn growing from the left of the forehead, curving out and up in four angular segments, with a broken stub on the other side."""
    m = WModel("horn_demon", "head")
    pts = [(2.4, 7.4, -3.4), (3.4, 9.6, -3.2), (5.2, 11.4, -2.8), (7.6, 12.6, -2.0), (9.6, 15.0, -1.2)]
    h = Part("horn")
    sweep(h, pts, L.octagon(2.4, 2.0, 0.7), scales=[(1.0, 1.0), (0.84, 0.84), (0.68, 0.68), (0.5, 0.5), (0.0, 0.0)], caps=(True, True))
    m.add(h, DARK, "horn", SHELL)
    for k in range(3):
        c = Part("crack%d" % k)
        L.dia(c, (3.0 + 1.4 * k, 9.2 + 1.2 * k, -3.9 + 0.5 * k), 0.2, 0.7, 0.15)
        m.add(c, GLOW, "soft", EMIT | HOT, uv="fit")
    st = Part("stub")
    L.cone(st, (-2.6, 7.6, -3.2), (-3.4, 9.4, -3.0), 1.0, 5, mid=0.5, mid_r=0.9)
    m.add(st, DARK, "horn", SHELL)
    return m


@model
def wing_demon():
    """Asta's one tattered wing on the left shoulder: a bone arm with four ragged fingers and torn, dark membranes veined in red light."""
    m = WModel("wing_demon", "body")
    g = m.group("wing", "sway", (2.2, -1.5, 2.4), 5.0, 0.08, 0.0)
    root = (2.2, -1.5, 2.6)
    arm = Part("arm")
    pts = L.bez3(root, (5.0, 4.0, 3.2), (9.0, 8.0, 3.6), (13.0, 8.0, 3.8), 6)
    sweep(arm, pts, circle(0.62, 5), scales=[(1.2, 1.2)] + [(1.0, 1.0)] * 4 + [(0.5, 0.5), (0.0, 0.0)], caps=(True, True))
    m.add(arm, DARK, "horn", SHELL, group=g)
    wrist = (13.0, 8.0, 3.8)
    tips = [(18.0, 3.0, 4.2), (16.5, -5.0, 4.0), (11.5, -11.0, 3.8), (6.0, -12.5, 3.4)]
    for k, tip in enumerate(tips):
        fg = Part("finger%d" % k)
        mid = ((wrist[0] + tip[0]) / 2 + 0.7, (wrist[1] + tip[1]) / 2 + 0.6, 3.9)
        sweep(fg, L.bez(wrist, mid, tip, 4), circle(0.34, 4), scales=[(1.0, 1.0), (0.9, 0.9), (0.7, 0.7), (0.4, 0.4), (0.0, 0.0)], caps=(True, True))
        m.add(fg, DARK, "horn", SHELL, group=g)
    prev = root
    for k, tip in enumerate(tips):
        mem = Part("membrane%d" % k)
        a = (wrist[0], wrist[1], wrist[2] + 0.2)
        L.quad_strip(mem, [a, prev], [a, tip], 0.25)
        m.add(mem, MAIN, "vein", SHELL, group=g, uv="fit")
        v = Part("vein%d" % k)
        sweep(v, L.bez(a, ((a[0] + tip[0]) / 2, (a[1] + tip[1]) / 2 - 1.0, 3.6), tip, 3), circle(0.12, 3), scales=[(1.0, 1.0), (0.8, 0.8), (0.5, 0.5), (0.0, 0.0)], caps=(True, True))
        m.add(v, GLOW, "soft", EMIT | HOT, group=g, uv="fit")
        prev = tip
    return m


@model
def crown_bolts():
    """A crown of seven crackling lightning bolts standing over the head: each bolt is a zigzag shard, pulsing and turning: Luck's crown."""
    m = WModel("crown_bolts", "head")
    g = m.group("bolts", "pulse", (0, 9.0, 0), 0.15, 3.0, 0.0)
    for k in range(7):
        a = 2 * math.pi * k / 7
        h = 6.0 + 2.5 * ((k * 5) % 3)
        x, z = 4.6 * math.cos(a), 4.6 * math.sin(a)
        pts = [(x, 8.8, z), (x * 1.08 + 0.7, 8.8 + h * 0.35, z * 1.08), (x * 0.95 - 0.8, 8.8 + h * 0.6, z * 0.95 + 0.4), (x * 1.04 + 0.5, 8.8 + h, z * 1.04)]
        b = Part("bolt%d" % k)
        sweep(b, pts, rect_ring(0.9, 0.35), scales=[(1.0, 1.0), (0.9, 0.9), (0.7, 0.7), (0.0, 0.0)], caps=(True, True))
        m.add(b, GLOW, "spark", EMIT | HOT, group=g, uv="fit")
    r = Part("ring")
    L.torus(r, (0, 8.9, 0), 4.7, 0.22, 16, 4, plane="xz")
    m.add(r, TRIM, "gilt", EMIT, uv="fit")
    return m


@model
def wings_valk():
    """Valkyrie Dress wings: two wide avian wings of long crystal-water feathers, translucent blue-white, with lit edges."""
    m = WModel("wings_valk", "body")
    for sx in (-1, 1):
        g = m.group("wing%d" % sx, "sway", (sx * 1.6, -2.0, 2.6), 5.0 * sx, 0.1, 0.0)
        _spread_wing(m, sx, g, (sx * 1.6, -2.0, 2.6), 15.0, 9.0, ranks=3, per=5, slot=MAIN, slot2=GLOW, tile="crystal", flags=SHELL)
    return m


@model
def skirt_valk():
    """Six crystal-water plates flared round the hips like a skirt: tall faceted shards, lit at the core."""
    m = WModel("skirt_valk", "body")
    g = m.group("skirt", "swing", (0, -11.0, 0), 5.0, 0.9, 0.0)
    for k in range(6):
        a = 2 * math.pi * k / 6 + math.pi / 6
        x, z = 4.6 * math.sin(a), -4.0 * math.cos(a)
        p = Part("plate%d" % k)
        L.dia(p, (x * 1.12, -14.5, z * 1.12), 1.9, 3.8, 0.7)
        p.V = [np.array([(v[0] - x * 1.12) * math.cos(a) + (v[2] - z * 1.12) * math.sin(a) + x * 1.12, v[1], -(v[0] - x * 1.12) * math.sin(a) + (v[2] - z * 1.12) * math.cos(a) + z * 1.12]) for v in p.V]
        m.add(p, ACCENT, "crystal", SHELL, group=g, uv="fit")
        c = Part("core%d" % k)
        L.dia(c, (x * 1.12, -14.5, z * 1.12), 0.5, 2.4, 0.3)
        m.add(c, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    return m


@model
def helm_valk():
    """Two swept feathered wings at the temples of the helm, crystal-water, lit at the edge."""
    m = WModel("helm_valk", "head")
    for sx in (-1, 1):
        g = m.group("wing%d" % sx, "sway", (sx * 4.4, 5.0, 0.0), 4.0 * sx, 0.12, 0.0)
        for k in range(4):
            f = Part("feather%d_%d" % (sx, k))
            L.feather(f, (sx * 4.4, 5.6 + 0.8 * k, 0.4 + 0.3 * k), (sx * (9.4 - 0.4 * k), 8.6 + 1.6 * k, 1.8 + 0.5 * k), 0.9, 0.22, 4)
            m.add(f, MAIN if k % 2 == 0 else GLOW, "crystal", SHELL, group=g, uv="fit")
    return m


MODES = [
    ("mode_wind_spirit_dive", ("wings_wind", "crown_starblades"), (0x7FD8B0, 0xF0FFF4, 0xB8FFE0), 5, "leaf"),
    ("mode_fire_spirit_dive", ("wings_dragon", "flame_hair"), (0x8A2A10, 0xFF9A30, 0xFF5A18), 5, "ember"),
    ("mode_demon", ("horn_demon", "wing_demon"), (0x0C0C10, 0x3A0A12, 0xE02030), 6, "wisp"),
    ("mode_lightning_god", ("crown_bolts",), (0xD8D060, 0xFFF8B0, 0xFFF060), 5, "spark"),
    ("mode_valkyrie", ("wings_valk", "skirt_valk", "helm_valk"), (0x90D0F0, 0xF4FCFF, 0x6AE0FF), 5, "snow"),
]


def catalog_modes(cat):
    for key, pieces, pal, tier, theme in MODES:
        cat.item(key, pieces, pal, tier)
        cat.theme(key, theme)
