"""The worn HEAD models: crowns, circlets, hoods, hats, horned helms. Authoring frame in gen_worn_lib (head box x -4..4, y 0..8, z -4..4, front -z)."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


def _spire(m, tag, x, z, y0, h, r, lean=(0.0, 0.0), sides=5, tile="gilt", slot=MAIN, tip_gem=None, gem_r=0.55):
    p = Part("spire_" + tag)
    L.cone(p, (x, y0 - 0.2, z), (x + lean[0], y0 + h, z + lean[1]), r, sides, mid=0.45, mid_r=0.62, cap=False)
    m.add(p, slot, tile)
    if tip_gem:
        g = Part("tipgem_" + tag)
        L.dia(g, (x + lean[0], y0 + h + gem_r * 0.6, z + lean[1]), gem_r * 0.8, gem_r * 1.1, gem_r * 0.8)
        m.add(g, tip_gem, "pearl" if tip_gem != GLOW else "gem", EMIT if tip_gem == GLOW else 0, uv="fit")


def _gem(m, tag, c, r, slot=GLOW, tile="gem", flags=EMIT | HOT, long=1.5, axis="y"):
    g = Part("gem_" + tag)
    ml_gem(g, c, r, long, axis)
    m.add(g, slot, tile, flags, uv="fit")


def ml_gem(part, c, r, long=1.4, axis="y", sides=4):
    L.ml.gem(part, c, r, long, sides, axis)


def _band(m, y0, y1, a=4.6, b=4.6, t=0.7, n=None, taper=0.15, flare=0.0, tile="gilt", slot=MAIN, p=3.0, name="band"):
    pt = Part(name)
    L.band(pt, y0, y1, a, b, t, n or L.RN, taper, flare, p)
    m.add(pt, slot, tile)
    return pt


def _front_gem_settings(m, y, r=0.85):
    """The brow stone on the band's front: a raised bezel ring and the gem."""
    bz = Part("bezel")
    L.torus(bz, (0, y, -5.55), r * 1.05, 0.26, 6, 4, plane="xy")
    m.add(bz, TRIM, "gilt")
    _gem(m, "brow", (0, y, -5.55), r * 0.8, long=0.8, axis="z")


@model
def crown_spired():
    """A royal crown: a flared band with a trim of beads, five tall spires with jewels, a gem on the brow, a velvet cap showing between the spires."""
    m = WModel("crown_spired", "head")
    _band(m, 6.2, 8.4, 4.55, 4.55, 0.65, None, 0.15, 0.25)
    rim = Part("rim_top")
    L.band(rim, 8.2, 8.55, 4.6, 4.6, 0.95, L.RN, 0.0, 0.25, 3.0)
    m.add(rim, TRIM, "gilt")
    cap = Part("cap")                                                      # the velvet cap showing inside the spires
    L.sph(cap, (0, 8.0, 0), 4.0, 3, 8, squash=0.6)
    m.add(cap, DARK, "cloth")
    # spires round the band (front one tallest)
    n = 5
    for i in range(n):
        th = math.pi + 2 * math.pi * (i - 0.0) / n * 1.0
        x, z = L._sq(2 * math.pi * i / n - math.pi / 2, 4.75, 4.75, 3.0)
        h = 4.4 if i == 0 else 3.4
        _spire(m, str(i), x, z, 8.0, h, 0.85, (x * 0.12, z * 0.12), 5, tip_gem=GLOW if i == 0 else ACCENT, gem_r=0.5)
    _front_gem_settings(m, 7.2)
    # side gems
    for sx in (-1, 1):
        _gem(m, "side%d" % sx, (sx * 5.2, 7.3, 0.0), 0.5, long=0.8, axis="x", flags=EMIT)
    return m


@model
def crown_duck():
    """The Tiny Duck Crown: a small golden band with three little points, a velvet cushion, and a plump duck sitting on top with a beak, eyes and a wing."""
    m = WModel("crown_duck", "head")
    _band(m, 6.4, 7.8, 4.5, 4.5, 0.6, None, 0.12, 0.15)
    rim = Part("rim_top")
    L.band(rim, 7.7, 8.05, 4.55, 4.55, 0.8, L.RN, 0.0, 0.15)
    m.add(rim, TRIM, "gilt")
    # three low points (front, left, right) and a rear one, each tipped with a bead
    for i, (x, z) in enumerate(((0.0, -4.75), (4.75, 0.0), (-4.75, 0.0), (0.0, 4.75))):
        _spire(m, str(i), x, z, 7.8, 1.5, 0.75, (0, 0), 5, tip_gem=ACCENT if i else GLOW, gem_r=0.38)
    cushion = Part("cushion")
    L.sph(cushion, (0, 8.0, 0), 3.3, 4, 12, squash=0.45)
    m.add(cushion, DARK, "cloth", uv="tile")
    # the duck: body, head, beak, eyes, wing, tail
    body = Part("duck_body")
    L.sph(body, (0.0, 10.3, 0.2), 2.5, 4, 8, squash=0.85)
    m.add(body, NONE, "duck", uv="fit")
    head = Part("duck_head")
    L.sph(head, (0.0, 12.9, -1.6), 1.45, 4, 8, squash=1.0)
    m.add(head, NONE, "duck", uv="fit")
    tail = Part("duck_tail")
    L.cone(tail, (0.0, 10.7, 2.1), (0.0, 12.4, 3.4), 0.9, 4, mid=0.5, mid_r=0.8)
    m.add(tail, NONE, "duck", uv="fit")
    wing = Part("duck_wing")
    for sx in (-1, 1):
        L.cone(wing, (sx * 2.2, 10.6, 0.3), (sx * 2.7, 10.2, 2.6), 0.95, 5, mid=0.4, mid_r=0.9)
    m.add(wing, NONE, "duck", uv="fit")
    beak = Part("duck_beak")
    loft(beak, "-z", (0.0, 12.55, -2.7), [(0.0, 1.0, 1.0), (0.6, 0.85, 0.5), (1.4, 0.35, 0.2)], L.ellipse(0.85, 0.42, 8))
    m.add(beak, NONE, "orange", uv="fit")
    eyes = Part("duck_eyes")
    for sx in (-1, 1):
        L.sph(eyes, (sx * 0.95, 13.3, -2.4), 0.26, 3, 6)
    m.add(eyes, NONE, "black", uv="fit")
    feet = Part("duck_feet")
    for sx in (-1, 1):
        L.dia(feet, (sx * 1.0, 8.55, -1.2), 0.5, 0.25, 0.6)
    m.add(feet, NONE, "orange", uv="fit")
    # a tiny glint on the brow
    _front_gem_settings(m, 7.1, 0.55)
    return m


@model
def circlet_gem():
    """A fine band across the brow with a raised front plate and a faceted stone, small swept side wings and a thin tiara rim."""
    m = WModel("circlet_gem", "head")
    _band(m, 6.5, 7.7, 4.45, 4.45, 0.35, None, 0.05, 0.0, tile="gilt")
    plate = Part("front_plate")
    loft(plate, "y", (0, 6.6, -4.9), [(0.0, 1.5, 0.28), (1.0, 1.35, 0.28), (2.1, 0.9, 0.24), (2.9, 0.38, 0.2), (3.3, 0.0, 0.0)], L.ellipse(1.0, 1.0, 10))
    m.add(plate, TRIM, "gilt")
    _gem(m, "brow", (0, 7.7, -5.25), 0.78, long=0.9, axis="z")
    for sx in (-1, 1):
        w = Part("wing%d" % sx)
        pts = L.bez((sx * 4.6, 7.1, -3.2), (sx * 5.3, 8.4, -1.0), (sx * 4.9, 8.9, 1.6), 6)
        sweep(w, pts, circle(0.34, 5), scales=[(1.0, 1.0), (1.1, 1.1), (1.0, 1.0), (0.8, 0.8), (0.6, 0.6), (0.4, 0.4), (0.0, 0.0)], caps=(True, True))
        m.add(w, TRIM, "gilt")
        t = Part("tip%d" % sx)
        L.sph(t, (sx * 4.9, 8.9, 1.6), 0.35, 3, 6)
        m.add(t, GLOW, "pearl", EMIT, uv="fit")
    return m


@model
def circlet_dragon():
    """A horned circlet: a band with two long swept horns, a dragon-head crest over the brow with jaw and brow ridges, a gem held in its mouth, and
    small fins running back along the band."""
    m = WModel("circlet_dragon", "head")
    _band(m, 6.3, 7.5, 4.5, 4.5, 0.5, None, 0.1, 0.0, tile="gilt")
    # the horns: swept back and up from over the temples
    for sx in (-1, 1):
        pts = L.bez3((sx * 4.3, 7.0, -1.8), (sx * 5.6, 8.4, -0.2), (sx * 5.5, 11.0, 2.4), (sx * 3.7, 12.6, 5.2), 8)
        h = Part("horn%d" % sx)
        sweep(h, pts, circle(0.85, 7), scales=[(1.0, 1.0), (0.95, 0.95), (0.85, 0.85), (0.74, 0.74), (0.62, 0.62), (0.5, 0.5), (0.34, 0.34), (0.18, 0.18), (0.0, 0.0)])
        m.add(h, TRIM, "horn", SHELL)
        for k in (2, 4):
            r = Part("hornring%d_%d" % (k, sx))
            c = pts[k]
            L.torus(r, c, 0.78 - 0.1 * k, 0.16, 8, 4, plane="xz")
            m.add(r, DARK, "metal")
    # the dragon head crest (a snout pointing forward-up, jaw, brow ridges, an eye, nostrils)
    snout = Part("snout")
    loft(snout, "-z", (0.0, 7.5, -4.6), [(0.0, 1.25, 1.0), (1.0, 1.05, 0.95), (2.0, 0.78, 0.8), (2.9, 0.5, 0.55), (3.3, 0.0, 0.0)], L.ellipse(1.0, 0.95, 10))
    m.add(snout, MAIN, "scale", SHELL, uv="tile")
    crown = Part("skull")
    L.sph(crown, (0.0, 8.3, -3.7), 1.5, 4, 9, squash=0.9)
    m.add(crown, MAIN, "scale", SHELL)
    jaw = Part("jaw")
    loft(jaw, "-z", (0.0, 6.55, -4.5), [(0.0, 1.05, 0.5), (1.2, 0.8, 0.45), (2.4, 0.4, 0.3), (2.8, 0.0, 0.0)], L.ellipse(1.0, 0.8, 8))
    m.add(jaw, DARK, "scale")
    for sx in (-1, 1):
        brow = Part("brow%d" % sx)
        pts = L.bez((sx * 0.6, 9.0, -4.2), (sx * 1.6, 9.3, -4.6), (sx * 2.3, 8.7, -3.9), 4)
        sweep(brow, pts, circle(0.33, 5), scales=[(1, 1), (1.1, 1.1), (0.8, 0.8), (0.0, 0.0), (0.0, 0.0)][:len(pts)])
        m.add(brow, TRIM, "gilt")
        eye = Part("eye%d" % sx)
        L.sph(eye, (sx * 1.15, 8.55, -4.75), 0.3, 3, 6)
        m.add(eye, GLOW, "spark", EMIT | HOT, uv="fit")
        nos = Part("nostril%d" % sx)
        L.sph(nos, (sx * 0.38, 7.9, -7.45), 0.2, 3, 5)
        m.add(nos, DARK, "metal", uv="fit")
        horn2 = Part("crest_horn%d" % sx)
        pts = L.bez((sx * 0.9, 9.0, -3.5), (sx * 1.2, 10.0, -2.8), (sx * 1.0, 11.0, -1.5), 5)
        sweep(horn2, pts, circle(0.35, 5), scales=[(1, 1), (0.9, 0.9), (0.75, 0.75), (0.5, 0.5), (0.3, 0.3), (0.0, 0.0)])
        m.add(horn2, TRIM, "horn")
    _gem(m, "mouth", (0, 6.9, -6.4), 0.5, long=0.9, axis="z")
    # a row of dorsal fins along the band's rear
    for i in range(5):
        a = math.radians(-60 + i * 30)
        x, z = 4.55 * math.sin(a), 4.55 * math.cos(a)
        f = Part("fin%d" % i)
        L.cone(f, (x, 7.6, z), (x * 1.0, 7.6 + 2.4 - abs(i - 2) * 0.35, z + 0.6), 0.42, 4, mid=0.3, mid_r=0.5)
        m.add(f, TRIM, "gilt")
    return m


@model
def circlet_phoenix():
    """A band with a fan of flame-feathers: five long feathers rising and curling at the front, three longer plumes sweeping back, a fire gem on the brow."""
    m = WModel("circlet_phoenix", "head")
    _band(m, 6.4, 7.5, 4.45, 4.45, 0.45, None, 0.1, 0.0)
    _front_gem_settings(m, 7.0, 0.7)
    g_flame = m.group("flame", "sway", (0, 7.5, 0), 3.5, 0.11, 0.0)
    # crest feathers
    for i, (ang, ln) in enumerate(((-52, 4.6), (-26, 6.6), (0, 8.6), (26, 6.6), (52, 4.6))):
        a = math.radians(ang)
        base = (math.sin(a) * 3.3, 7.6, -4.4 + (1 - math.cos(a)) * 1.0)
        tip = (math.sin(a) * (3.3 + ln * 0.55), 7.6 + ln * 0.9, -3.4 - ln * 0.12)
        f = Part("feather%d" % i)
        L.feather(f, base, tip, 0.95 + ln * 0.07, 0.3, 6, curl=0.08 if ang >= 0 else -0.08)
        m.add(f, ACCENT if i % 2 else TRIM, "feather", SHELL | EMIT if i == 2 else SHELL, group=g_flame, uv="fit")
        core = Part("fcore%d" % i)
        L.feather(core, base, tip, 0.5 + ln * 0.03, 0.34, 6, curl=0.08 if ang >= 0 else -0.08)
        core.F = core.F
        m.add(core, GLOW, "ember", EMIT | HOT, group=g_flame, uv="fit")
    # back plumes
    g_tail = m.group("plumes", "sway", (0, 7.5, 3.0), 5.0, 0.09, 0.0)
    for i, (xo, ln) in enumerate(((-2.4, 7.5), (0.0, 9.5), (2.4, 7.5))):
        f = Part("plume%d" % i)
        L.feather(f, (xo, 7.4, 3.9), (xo * 1.8, 7.4 + ln * 0.5, 3.9 + ln * 0.9), 1.0, 0.3, 7, curl=0.1)
        m.add(f, TRIM, "feather", SHELL, group=g_tail, uv="fit")
        c = Part("plume_core%d" % i)
        L.feather(c, (xo, 7.4, 3.95), (xo * 1.8, 7.4 + ln * 0.5, 3.95 + ln * 0.9), 0.5, 0.34, 7, curl=0.1)
        m.add(c, GLOW, "ember", EMIT | HOT, group=g_tail, uv="fit")
    return m


@model
def crown_unicorn():
    """A pale crown with a single spiral horn rising from the brow, leaning forward, with a golden twist ridge and a glow at its tip; small ears beside it."""
    m = WModel("crown_unicorn", "head")
    _band(m, 6.3, 7.7, 4.5, 4.5, 0.55, None, 0.1, 0.15)
    rim = Part("rim_top")
    L.band(rim, 7.55, 7.9, 4.55, 4.55, 0.75, L.RN, 0.0, 0.2)
    m.add(rim, ACCENT, "gilt")
    # the horn: a twisted section along a leaning axis
    base, tip = np.array([0.0, 7.4, -4.2]), np.array([0.0, 17.2, -7.2])
    axis = tip - base
    Lh = float(np.linalg.norm(axis))
    def sec(s):
        pts = []
        n = 12
        tw = s / Lh * 5.5 * math.pi
        for i in range(n):
            th = 2 * math.pi * i / n
            r = 1.0 + (0.26 if i % 2 == 0 else -0.05) * math.cos(0)
            r = 1.0 if i % 2 else 0.72
            pts.append((r * math.cos(th + tw), r * math.sin(th + tw)))
        return pts
    h = Part("horn")
    st = [(0.0, 1.0, 1.0)]
    for k in range(1, 12):
        s = Lh * k / 12.0
        t = k / 12.0
        st.append((s, 1.0 - 0.82 * t ** 1.1, 1.0 - 0.82 * t ** 1.1))
    st.append((Lh, 0.0, 0.0))
    loft(h, tuple(axis), tuple(base), [(s, su * 1.55, sv * 1.55) for (s, su, sv) in st], sec, caps=(True, True), up=(0, 0, 1))
    m.add(h, MAIN, "horn", SHELL)
    tipg = Part("tip_glow")
    L.sph(tipg, tuple(base + axis * 0.985), 0.34, 3, 6)
    m.add(tipg, GLOW, "soft", EMIT | HOT, uv="fit")
    sock = Part("socket")
    L.torus(sock, (0, 7.5, -4.5), 1.25, 0.4, 12, 5, plane="xz")
    m.add(sock, TRIM, "gilt")
    for sx in (-1, 1):
        ear = Part("ear%d" % sx)
        L.cone(ear, (sx * 2.2, 7.7, -2.8), (sx * 2.8, 11.2, -3.4), 0.95, 5, mid=0.35, mid_r=0.9, squash=0.6)
        m.add(ear, MAIN, "fur")
        inner = Part("ear_in%d" % sx)
        L.cone(inner, (sx * 2.2, 7.9, -3.15), (sx * 2.75, 10.6, -3.65), 0.5, 4, mid=0.35, mid_r=0.9, squash=0.5)
        m.add(inner, ACCENT, "cloth")
    _front_gem_settings(m, 7.0, 0.55)
    return m


@model
def crown_unicorn_dark():
    """The Dark Unicorn Crown: jagged black iron band, a cracked horn with red veins, hooked ears and rear spikes, glowing eyes-gems."""
    m = WModel("crown_unicorn_dark", "head")
    _band(m, 6.3, 7.9, 4.55, 4.55, 0.6, None, 0.1, 0.2, tile="metal", slot=MAIN)
    for i in range(7):
        a = math.radians(-120 + i * 40)
        x, z = 4.8 * math.sin(a), -4.8 * math.cos(a)
        if abs(a) < 0.3:
            continue
        _spire(m, "r%d" % i, x, z, 7.7, 2.0 + 0.5 * (i % 2), 0.55, (x * 0.15, z * 0.15), 4, tile="metal", slot=MAIN)
    base, tip = np.array([0.0, 7.4, -4.2]), np.array([0.0, 17.0, -7.4])
    axis = tip - base
    Lh = float(np.linalg.norm(axis))
    def sec(s):
        n = 10
        tw = s / Lh * 4.0 * math.pi
        return [((1.0 if i % 2 else 0.6) * math.cos(2 * math.pi * i / n + tw), (1.0 if i % 2 else 0.6) * math.sin(2 * math.pi * i / n + tw)) for i in range(n)]
    h = Part("horn")
    st = [(Lh * k / 11.0, (1.0 - 0.85 * (k / 11.0) ** 1.1) * 1.1, (1.0 - 0.85 * (k / 11.0) ** 1.1) * 1.1) for k in range(11)] + [(Lh, 0.0, 0.0)]
    loft(h, tuple(axis), tuple(base), st, sec, caps=(True, True), up=(0, 0, 1))
    m.add(h, MAIN, "horn", SHELL)
    v = Part("veins")
    loft(v, tuple(axis), tuple(base + axis * 0.05), [(Lh * k / 11.0, (0.92 - 0.8 * (k / 11.0) ** 1.1) * 1.1, (0.92 - 0.8 * (k / 11.0) ** 1.1) * 1.1) for k in range(10)] + [(Lh * 0.93, 0.0, 0.0)],
         lambda s: [(0.35 * math.cos(2 * math.pi * i / 6 + s / Lh * 9.0), 0.35 * math.sin(2 * math.pi * i / 6 + s / Lh * 9.0)) for i in range(6)], caps=(True, True), up=(0, 0, 1))
    m.add(v, GLOW, "vein", EMIT | HOT)
    for sx in (-1, 1):
        ear = Part("ear%d" % sx)
        pts = L.bez3((sx * 2.4, 7.6, -2.6), (sx * 3.0, 10.4, -3.0), (sx * 3.6, 11.4, -2.0), (sx * 3.4, 11.9, -0.8), 6)
        sweep(ear, pts, circle(0.9, 5), scales=[(1, 0.5), (0.9, 0.5), (0.8, 0.45), (0.6, 0.4), (0.4, 0.3), (0.2, 0.2), (0.0, 0.0)][:len(pts)])
        m.add(ear, MAIN, "leather")
        _gem(m, "eye%d" % sx, (sx * 2.2, 7.0, -5.4), 0.4, long=0.9, axis="z")
    return m


@model
def crown_conjurer():
    """A slim gold crown with four arched pillars meeting over the head in a small orb, a floating rune ring turning above, and three orbiting sigils."""
    m = WModel("crown_conjurer", "head")
    _band(m, 6.4, 7.9, 4.5, 4.5, 0.55, None, 0.1, 0.0)
    for i in range(4):
        th = math.pi / 4 + i * math.pi / 2
        x0, z0 = 4.6 * math.sin(th), 4.6 * math.cos(th)
        pts = L.bez3((x0, 7.8, z0), (x0 * 1.0, 11.4, z0 * 1.0), (x0 * 0.35, 12.4, z0 * 0.35), (0.0, 12.3, 0.0), 4)
        p = Part("pillar%d" % i)
        sweep(p, pts, circle(0.42, 4), scales=[(1.1, 1.1)] + [(1.0, 1.0)] * 3 + [(0.7, 0.7)], caps=(False, False))
        m.add(p, TRIM, "gilt")
    orb = Part("orb")
    L.sph(orb, (0, 12.6, 0), 1.0, 4, 8)
    m.add(orb, GLOW, "gem", EMIT | HOT, uv="fit")
    g_ring = m.group("runes", "spin", (0, 15.8, 0), 1.6, 0.0, 0.0)
    ring = Part("rune_ring")
    L.torus(ring, (0, 15.8, 0), 5.2, 0.28, 16, 4, plane="xz")
    m.add(ring, GLOW, "rune", EMIT, group=g_ring, uv="tile", density=3.0)
    for k in range(6):
        a = 2 * math.pi * k / 6
        s = Part("sigil%d" % k)
        L.ml.gem(s, (5.2 * math.cos(a), 15.8, 5.2 * math.sin(a)), 0.45, 1.2, 4, "y")
        m.add(s, GLOW, "spark", EMIT, group=g_ring, uv="fit")
    g_orbit = m.group("orbit", "orbit", (0, 11.5, 0), -2.6, 0.0, 0.0)
    for k in range(3):
        a = 2 * math.pi * k / 3
        s = Part("star%d" % k)
        L.ml.gem(s, (3.4 * math.cos(a), 11.5 + 0.4 * (k % 2), 3.4 * math.sin(a)), 0.42, 1.4, 4, "y")
        m.add(s, ACCENT, "spark", EMIT, group=g_orbit, uv="fit")
    _front_gem_settings(m, 7.2, 0.6)
    return m


@model
def crown_godking():
    """The Godking Crown: a tall ancient crown of seven blade-spires with rune bands, a great gem on the brow, a vertical halo ring behind the head and floating shards."""
    m = WModel("crown_godking", "head")
    _band(m, 6.2, 8.8, 4.65, 4.65, 0.8, None, 0.2, 0.3, tile="plate", slot=MAIN)
    rune = Part("rune_band")
    L.band(rune, 7.2, 7.9, 4.6, 4.6, 0.95, L.RN, 0.0, 0.0)
    m.add(rune, GLOW, "rune", EMIT, density=3.0)
    for yy in (6.15, 8.7):
        r = Part("rim%.1f" % yy)
        L.band(r, yy - 0.15, yy + 0.2, 4.6, 4.6, 1.1, L.RN, 0.0, 0.3 if yy > 8 else 0.0)
        m.add(r, TRIM, "gilt")
    n = 7
    for i in range(n):
        th = 2 * math.pi * i / n - math.pi / 2
        x, z = L._sq(th, 4.9, 4.9, 3.0)
        h = 7.0 if i == 0 else (5.6 if i in (1, n - 1) else 4.6)
        p = Part("blade%d" % i)
        loft(p, (x * 0.1, 1.0, z * 0.1), (x, 8.4, z), [(0, 1.1, 0.5), (h * 0.5, 0.8, 0.4), (h * 0.85, 0.35, 0.25), (h, 0.0, 0.0)], L.rect_ring(1.6, 0.9))
        m.add(p, TRIM, "gilt")
        c = Part("blade_core%d" % i)
        loft(c, (x * 0.1, 1.0, z * 0.1), (x, 8.5, z), [(0, 0.5, 0.5), (h * 0.5, 0.35, 0.5), (h * 0.8, 0.12, 0.3)], L.rect_ring(1.0, 0.9))
        m.add(c, GLOW, "flame", EMIT)
    _front_gem_settings(m, 7.6, 1.0)
    # the halo: a vertical ring behind the head, slowly turning, and its rune ticks
    g_halo = m.group("halo", "sway", (0, 9.0, 4.5), 0.0, 0.0, 0.0)
    halo = Part("halo")
    L.torus(halo, (0, 11.0, 6.0), 6.2, 0.32, 16, 4, plane="xy")
    m.add(halo, GLOW, "rune", EMIT, group=g_halo, density=3.0)
    halo2 = Part("halo_inner")
    L.torus(halo2, (0, 11.0, 6.0), 5.2, 0.16, 16, 3, plane="xy")
    m.add(halo2, TRIM, "gilt", group=g_halo)
    g_sh = m.group("shards", "orbit", (0, 12.0, 0), -1.8, 0.0, 0.0)
    for k in range(5):
        a = 2 * math.pi * k / 5
        s = Part("shard%d" % k)
        L.ml.gem(s, (7.2 * math.cos(a), 12.5 + 1.2 * math.sin(a * 2), 7.2 * math.sin(a)), 0.5, 2.0, 4, "y")
        m.add(s, GLOW, "crystal", EMIT | HOT, group=g_sh, uv="fit")
    return m


@model
def hood_pointed():
    """A cloth cowl: a soft hood that closes over the whole head with a deep opening for the face, a trimmed edge, a pointed tail that droops down the back, a drape over the
    shoulders and a clasp gem at the brow."""
    m = WModel("hood_pointed", "head")
    OPEN = 0.62                                              # half the face opening, radians
    def prof(v):
        """(radius, y) of the hood at v (0 at the shoulders, 1 at the crown): a drape that flares at the bottom, straight walls up to the top of the head, a rounded dome."""
        y = -2.6 + v * 12.0
        if y < 0.4:
            r = 5.2 + (0.4 - y) * 0.9
        elif y < 7.3:
            r = 5.25
        else:
            t = min(1.0, (y - 7.3) / 3.1)
            r = 5.25 * math.sqrt(max(0.0, 1.0 - t * t)) + 0.05
        return r, y
    def shellfn(u, v):
        th = 1.5 * math.pi + OPEN + u * (2 * math.pi - 2 * OPEN)
        r, y = prof(v)
        x, z = L._sq(th, r, r * 0.98, 3.2)
        back = max(0.0, 1.0 - (y + 2.6) / 3.0) * 0.0
        return (x, y, z + (1.2 * max(0.0, math.sin(th)) * max(0.0, (0.4 - y) / 3.0)))
    shell = Part("shell")
    L.sheet(shell, shellfn, 16, 8, 0.5)
    m.add(shell, MAIN, "cloth", SHELL)
    # the trimmed edge round the face opening, down both sides
    for sx, th in ((1, 1.5 * math.pi + OPEN), (-1, 1.5 * math.pi - OPEN + 2 * math.pi * 0)):
        pts = []
        for i in range(7):
            v = i / 6.0 * 0.86
            r, y = prof(v)
            x, z = L._sq(th, r + 0.25, r * 0.98 + 0.25, 3.2)
            pts.append((x, y, z))
        t = Part("edge%d" % sx)
        sweep(t, pts, circle(0.42, 4), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
        m.add(t, TRIM, "gilt")
    # the top edge of the opening (a brow trim)
    th0, th1 = 1.5 * math.pi - OPEN, 1.5 * math.pi + OPEN
    pts = []
    r, y = prof(0.86)
    for i in range(5):
        th = th0 + (th1 - th0) * i / 4.0
        x, z = L._sq(th, r + 0.25, r * 0.98 + 0.25, 3.2)
        pts.append((x, y, z))
    br = Part("brow")
    sweep(br, pts, circle(0.42, 4), scales=[(1.0, 1.0)] * 5, caps=(True, True))
    m.add(br, TRIM, "gilt")
    # the pointed tail that droops down the back
    g_tail = m.group("tail", "swing", (0, 9.0, 4.5), 7.0, 0.9, 0.0)
    tl = Part("tail")
    pts = L.bez3((0.0, 9.6, 1.0), (0.0, 11.8, 3.2), (0.0, 9.0, 6.4), (0.0, 3.0, 7.4), 6)
    sweep(tl, pts, circle(2.0, 6), scales=[(1.0, 1.0), (0.95, 0.8), (0.85, 0.6), (0.65, 0.45), (0.4, 0.3), (0.2, 0.15), (0.0, 0.0)], caps=(True, True))
    m.add(tl, MAIN, "cloth", SHELL, group=g_tail)
    # a clasp gem at the brow
    _gem(m, "clasp", (0, 8.3, -5.5), 0.6, long=0.8, axis="z")
    cl = Part("clasp_set")
    L.torus(cl, (0, 8.3, -5.4), 0.8, 0.2, 6, 4, plane="xy")
    m.add(cl, TRIM, "gilt")
    return m


@model
def hat_tricorn():
    """A three-cornered hat: a rounded crown, a brim folded up into three corners with gold braid, a swept plume and a badge."""
    m = WModel("hat_tricorn", "head")
    cr = Part("crown")
    L.band(cr, 7.2, 11.4, 4.6, 4.6, 0.55, L.RN, 0.45, -0.5, p=2.6)
    m.add(cr, MAIN, "leather")
    top = Part("top")
    L.sph(top, (0, 11.3, 0), 4.0, 3, 12, squash=0.3)
    m.add(top, MAIN, "leather")
    # the brim as a lofted sheet with three upturned corners (front, left-back, right-back)
    brim = Part("brim")
    def brimfn(u, v):
        th = 2 * math.pi * u
        r = 5.0 + v * 3.7
        # three corners at th = pi (front), pi/3, -pi/3 ... lift the rim near them
        corner = 0.0
        for c in (0.0, 2.0944, 4.18879):
            d = math.cos(th - c)
            corner = max(corner, max(0.0, d) ** 6)
        y = 7.2 + v * (0.6 + 5.0 * corner) + (0.8 * v * v if True else 0)
        x, z = r * math.cos(th) * (1.0 + 0.0 * corner), r * math.sin(th)
        z -= 0.0
        return (x * 0.98, y, z * (1.0 - 0.18 * corner * v))
    L.sheet(brim, brimfn, 24, 2, 0.35)
    m.add(brim, MAIN, "leather", SHELL)
    braid = Part("braid")
    for r_off in (9.6,):
        pass
    L.band(braid, 7.55, 8.3, 4.62, 4.62, 0.12, L.RN, 0.0, 0.0, p=2.6)
    m.add(braid, TRIM, "gilt")
    # the plume
    g_plume = m.group("plume", "sway", (3.8, 11.4, 2.0), 6.0, 0.13, 0.0)
    for i, (xo, zo) in enumerate(((0.0, 0.0), (0.8, 0.8), (-0.8, 1.2))):
        f = Part("plume%d" % i)
        L.feather(f, (4.0 + xo * 0.2, 10.0, -1.0 + zo), (7.6 + xo, 12.6 + i * 0.4, 2.8 + zo), 1.0, 0.3, 6, curl=0.1)
        m.add(f, ACCENT, "feather", SHELL, group=g_plume, uv="fit")
    # skull-and-glow badge on the front fold
    _gem(m, "badge", (0, 8.9, -8.4), 0.55, long=0.8, axis="z")
    return m


@model
def diadem_leaf():
    """A slender silver band with a trailing sprig of leaves at each temple and a bright gem on the brow, light as a laurel."""
    m = WModel("diadem_leaf", "head")
    _band(m, 6.4, 7.3, 4.45, 4.45, 0.3, None, 0.04, 0.0, tile="metal")
    for sx in (-1, 1):
        for k in range(5):
            a = math.radians(-40 + k * 40)
            x = sx * 4.6
            z = -3.0 + k * 1.7
            f = Part("leaf%d_%d" % (sx, k))
            L.feather(f, (x, 6.9, z), (x + sx * 1.2, 8.0 + 0.15 * k, z + 0.7), 0.5, 0.18, 4, curl=0.1)
            m.add(f, ACCENT, "feather", uv="fit")
    _front_gem_settings(m, 7.0, 0.6)
    top = Part("top_leaf")
    L.feather(top, (0, 7.1, -4.9), (0, 9.2, -5.2), 0.7, 0.2, 4)
    m.add(top, ACCENT, "feather", uv="fit")
    return m


@model
def crown_pride():
    """A heavy, broad golden crown with eight pointed fleurs, a cap of velvet, cross-arches meeting over the head and a great stone."""
    m = WModel("crown_pride", "head")
    _band(m, 6.0, 8.6, 4.75, 4.75, 0.85, None, 0.2, 0.35, tile="gilt")
    bd = Part("bead_rim")
    L.band(bd, 6.0, 6.45, 4.8, 4.8, 1.0, L.RN, 0.0, 0.0)
    m.add(bd, ACCENT, "gilt")
    for i in range(8):
        th = 2 * math.pi * i / 8 - math.pi / 2
        x, z = L._sq(th, 4.9, 4.9, 3.0)
        _spire(m, str(i), x, z, 8.4, 3.0 if i % 2 == 0 else 2.0, 0.85, (x * 0.1, z * 0.1), 5, tip_gem=GLOW if i % 2 == 0 else ACCENT, gem_r=0.5)
    for a in (0.0, math.pi / 2):
        pts = []
        for i in range(11):
            t = i / 10.0
            ang = math.pi * t
            pts.append((math.cos(ang + a) * 0 + (4.6 * math.cos(ang)) * (1 if a == 0 else 0), 8.6 + 4.4 * math.sin(ang), (4.6 * math.cos(ang)) * (0 if a == 0 else 1)))
        arch = Part("arch%d" % int(a * 10))
        sweep(arch, pts, circle(0.5, 6), scales=[(1.0, 1.0)] * 11, caps=(True, True))
        m.add(arch, TRIM, "gilt")
    cap = Part("cap")
    L.sph(cap, (0, 8.8, 0), 4.2, 4, 12, squash=0.9)
    m.add(cap, DARK, "cloth")
    orb = Part("orb")
    L.sph(orb, (0, 13.4, 0), 0.85, 4, 8)
    m.add(orb, GLOW, "gem", EMIT | HOT, uv="fit")
    _front_gem_settings(m, 7.3, 0.95)
    for sx in (-1, 1):
        _gem(m, "side%d" % sx, (sx * 5.55, 7.3, 0), 0.6, long=0.8, axis="x", flags=EMIT)
    return m


@model
def circlet_arcanist():
    """A twin-wire silver circlet with a violet stone and a small rune ring orbiting slowly above the brow."""
    m = WModel("circlet_arcanist", "head")
    _band(m, 6.5, 7.4, 4.45, 4.45, 0.3, None, 0.04, 0.0, tile="metal")
    _band(m, 7.5, 7.9, 4.45, 4.45, 0.2, None, 0.0, 0.0, tile="gilt", name="wire")
    plate = Part("front_plate")
    loft(plate, "y", (0, 6.6, -4.85), [(0.0, 1.3, 0.25), (1.2, 1.0, 0.22), (2.2, 0.4, 0.2), (2.6, 0.0, 0.0)], L.ellipse(1.0, 1.0, 8))
    m.add(plate, TRIM, "gilt")
    _gem(m, "brow", (0, 7.4, -5.25), 0.65, long=0.9, axis="z")
    g = m.group("runes", "spin", (0, 11.0, 0), 2.2, 0.0, 0.0)
    ring = Part("ring")
    L.torus(ring, (0, 11.0, 0), 4.0, 0.16, 28, 4, plane="xz")
    m.add(ring, GLOW, "rune", EMIT, group=g, density=3.0)
    for k in range(4):
        a = 2 * math.pi * k / 4
        s = Part("glyph%d" % k)
        L.ml.gem(s, (4.0 * math.cos(a), 11.0, 4.0 * math.sin(a)), 0.34, 1.3, 4, "y")
        m.add(s, GLOW, "spark", EMIT | HOT, group=g, uv="fit")
    return m


@model
def helm_horned():
    """A closed helm with a brow ridge and eye slit, two big curved horns and a cheek guard, trimmed in the second colour."""
    m = WModel("helm_horned", "head")
    sh = Part("skull")
    L.band(sh, 0.2, 8.4, 4.9, 4.9, 0.3, L.RN, 0.0, 0.0, p=3.6)
    cap = Part("dome")
    L.sph(cap, (0, 8.3, 0), 5.0, 4, 14, squash=0.55)
    m.add(sh, MAIN, "plate")
    m.add(cap, MAIN, "metal")
    br = Part("brow")
    L.band(br, 4.4, 5.7, 5.0, 5.0, 0.55, L.RN, 0.0, 0.0, p=3.6)
    m.add(br, TRIM, "gilt")
    for sx in (-1, 1):
        h = Part("horn%d" % sx)
        pts = L.bez3((sx * 4.7, 7.0, 0.0), (sx * 8.0, 7.6, -0.5), (sx * 9.6, 10.4, -1.0), (sx * 8.2, 13.6, -1.6), 8)
        sweep(h, pts, circle(1.1, 7), scales=[(1.0, 1.0), (0.96, 0.96), (0.9, 0.9), (0.8, 0.8), (0.68, 0.68), (0.54, 0.54), (0.38, 0.38), (0.2, 0.2), (0.0, 0.0)])
        m.add(h, ACCENT, "horn", SHELL)
        cg = Part("cheek%d" % sx)
        loft(cg, "y", (sx * 4.9, 0.0, -1.2), [(0, 0.5, 2.0), (2.4, 0.4, 2.0), (3.6, 0.2, 1.2)], rect_ring(1.0, 1.0))
        m.add(cg, TRIM, "plate")
    ey = Part("eye_slit")
    loft(ey, "x", (-2.8, 4.5, -5.0), [(0, 0.5, 0.3), (5.6, 0.5, 0.3)], rect_ring(1.0, 1.0))
    m.add(ey, GLOW, "soft", EMIT | HOT, uv="fit")
    nb = Part("nasal")
    loft(nb, "y", (0, 0.4, -5.0), [(0, 0.7, 0.3), (5.2, 0.7, 0.3)], rect_ring(1.0, 1.0))
    m.add(nb, TRIM, "plate")
    return m


@model
def hat_wizard():
    """A tall pointed hat: a wide brim with a drooping tip, a band with a buckle, a crease and a star charm."""
    m = WModel("hat_wizard", "head")
    brim = Part("brim")
    def brimfn(u, v):
        th = 2 * math.pi * u
        r = 4.8 + v * 5.0
        return (r * math.cos(th), 7.4 + 0.2 * math.sin(th * 2) * v - v * 0.4 * (1 - math.cos(th)) * 0.6, r * math.sin(th))
    L.sheet(brim, brimfn, 20, 2, 0.35)
    m.add(brim, MAIN, "cloth", SHELL)
    cone = Part("cone")
    def conefn(u, v):
        th = 2 * math.pi * u
        r = 4.5 * (1 - v) ** 0.9 + 0.1
        bend = (v ** 2.4) * 5.0
        return (r * math.cos(th), 7.6 + v * 11.0 - (v ** 5) * 3.0, r * math.sin(th) + bend)
    L.sheet(cone, conefn, 16, 6, 0.35)
    m.add(cone, MAIN, "cloth", SHELL)
    bd = Part("band")
    L.band(bd, 8.0, 9.3, 4.42, 4.42, 0.15, L.RN, 0.0, 0.0, p=2.0)
    m.add(bd, TRIM, "stripe")
    bk = Part("buckle")
    L.torus(bk, (0, 8.65, -4.6), 0.95, 0.2, 12, 4, plane="xy")
    m.add(bk, TRIM, "gilt")
    st = Part("star")
    L.ml.gem(st, (0, 8.65, -4.7), 0.5, 0.9, 5, "z")
    m.add(st, GLOW, "gem", EMIT | HOT, uv="fit")
    return m
