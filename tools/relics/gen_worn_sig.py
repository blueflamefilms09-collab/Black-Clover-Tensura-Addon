"""Signature worn models: the relics whose card asks for a distinctive shape, and small reusable decorations (swarms, flakes, leaves, wisps, stars) that a catalog entry
can add to any look. See gen_worn_all.py for which relic wears what."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)
import gen_worn_back as B
import gen_worn_body as BO
import gen_worn_neck as N
import gen_worn_head as H

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


# ================================================================ decorations (they attach to the body anchor unless the catalog says otherwise)
def _orbiters(m, tag, count, radius, y, speed, builder, slot=GLOW, tile="spark", flags=EMIT, bob=0.5, tilt=0.0, groups=1):
    """`count` small shapes circling the pivot (0, y, 0) at `radius`, built by builder(part, centre, k); split into `groups` groups that turn at different speeds."""
    gs = [m.group("%s%d" % (tag, g), "orbit", (0, y, 0), speed * (1.0 + 0.45 * g) * (-1 if g % 2 else 1), 0.0, 0.0) for g in range(groups)]
    for k in range(count):
        a = 2 * math.pi * k / count
        r = radius * (0.9 + 0.2 * ((k * 7) % 3) / 2.0)
        c = (r * math.cos(a), y + bob * math.sin(k * 2.1), r * math.sin(a))
        p = Part("%s%d" % (tag, k))
        builder(p, c, k)
        m.add(p, slot, tile, flags, group=gs[k % groups], uv="fit")


@model
def deco_bees():
    """A swarm of six bees circling the wearer at chest height in two loose groups: a yellow body with a black band, a head and two blurred wings."""
    m = WModel("deco_bees", "body")
    gs = [m.group("swarm%d" % g, "orbit", (0, -5.0, 0), (3.1 if g == 0 else -2.3), 0.0, 0.0) for g in range(2)]
    for k in range(6):
        a = 2 * math.pi * k / 6
        r = 8.2 + 1.2 * (k % 2)
        cx, cy, cz = r * math.cos(a), -2.0 - 3.2 * (k % 3) + 0.6 * math.sin(k * 1.7), r * math.sin(a)
        g = gs[k % 2]
        body = Part("bee%d" % k)
        L.dia(body, (cx, cy, cz), 1.2, 0.9, 0.9)
        m.add(body, NONE, "duck", 0, group=g, uv="fit")
        band = Part("band%d" % k)
        L.dia(band, (cx + 0.1, cy, cz), 0.5, 1.0, 1.0)
        m.add(band, NONE, "black", 0, group=g, uv="fit")
        head = Part("head%d" % k)
        L.dia(head, (cx - 1.5 * math.cos(a + 1.57), cy + 0.05, cz - 1.5 * math.sin(a + 1.57)), 0.55, 0.55, 0.55)
        m.add(head, NONE, "black", 0, group=g, uv="fit")
        wing = Part("wing%d" % k)
        L.dia(wing, (cx, cy - 1.1, cz), 0.2, 0.3, 1.3)
        m.add(wing, NONE, "white", EMIT, group=g, uv="fit")
    return m


@model
def deco_flakes():
    """Snowflakes drifting round the shoulders: six thin six-point flakes in two groups, rocking slightly, glowing pale."""
    m = WModel("deco_flakes", "body")
    def mk(p, c, k):
        for ang in (0, 60, 120):
            a = math.radians(ang)
            L.dia(p, c, 1.9 * math.cos(a) + 0.1, 0.22, 1.9 * math.sin(a) + 0.1)
    _orbiters(m, "flakes", 6, 8.0, -3.0, 1.6, mk, GLOW, "snow", EMIT, bob=2.2, groups=2)
    return m


@model
def deco_leaves():
    """Leaves circling the body: seven small curled leaves in two groups, in the relic's accent colours, turning slowly."""
    m = WModel("deco_leaves", "body")
    gs = [m.group("leaves%d" % g, "orbit", (0, -6.0, 0), 1.5 if g == 0 else -1.1, 0.0, 0.0) for g in range(2)]
    for k in range(7):
        a = 2 * math.pi * k / 7
        r = 8.0 + (k % 3) * 0.8
        cx, cy, cz = r * math.cos(a), -2.5 - 1.6 * (k % 4), r * math.sin(a)
        f = Part("leaf%d" % k)
        L.feather(f, (cx, cy, cz), (cx + 1.6 * math.cos(a + 1.2), cy + 0.6, cz + 1.6 * math.sin(a + 1.2)), 0.55, 0.12, 3, curl=0.1)
        m.add(f, ACCENT if k % 2 else MAIN, "feather", 0, group=gs[k % 2], uv="fit")
    return m


@model
def deco_wisps():
    """Pale wisps of shadow circling the legs and hips: five tapering wisps that rise and fall, glowing dimly."""
    m = WModel("deco_wisps", "body")
    def mk(p, c, k):
        L.cone(p, (c[0], c[1] + 2.4, c[2]), (c[0], c[1] - 2.4, c[2]), 0.8, 4, mid=0.35, mid_r=1.0)
    _orbiters(m, "wisp", 5, 7.6, -8.0, 2.0, mk, ACCENT, "soft", EMIT, bob=2.5, groups=2)
    return m


@model
def deco_stars():
    """Little four-point stars circling the body at the waist in two groups, each a bright diamond."""
    m = WModel("deco_stars", "body")
    def mk(p, c, k):
        L.dia(p, c, 0.5, 0.9, 0.12)
        L.dia(p, c, 0.9, 0.5, 0.12)
    _orbiters(m, "stars", 6, 8.4, -6.0, 2.4, mk, GLOW, "spark", EMIT | HOT, bob=2.0, groups=2)
    return m


@model
def deco_halo_back():
    """A glowing golden ring standing behind the shoulders, turning slowly, with small rays: a saint's halo."""
    m = WModel("deco_halo_back", "body")
    g = m.group("halo", "sway", (0, 3.0, 4.5), 0.0, 0.0, 0.0)
    r = Part("ring")
    L.torus(r, (0, 4.0, 6.5), 6.2, 0.32, 24, 4, plane="xy")
    m.add(r, GLOW, "white", EMIT | HOT, group=g, uv="fit")
    for k in range(12):
        a = 2 * math.pi * k / 12
        s = Part("ray%d" % k)
        L.cone(s, (6.6 * math.cos(a), 4.0 + 6.6 * math.sin(a), 6.5), (8.6 * math.cos(a), 4.0 + 8.6 * math.sin(a), 6.5), 0.25, 3, mid=0.5, mid_r=1.0)
        m.add(s, GLOW, "white", EMIT, group=g, uv="fit")
    return m


@model
def deco_flames():
    """Embers rising round the legs and feet: six small flame tongues that bob at different speeds."""
    m = WModel("deco_flames", "body")
    for k in range(6):
        g = m.group("flame%d" % k, "bob", (0, -20.0, 0), 1.2 + 0.2 * (k % 3), 0.11 + 0.02 * k, k * 1.1)
        a = 2 * math.pi * k / 6
        c = (5.2 * math.cos(a), -21.0 - (k % 2) * 3.0, 4.6 * math.sin(a))
        p = Part("flame%d" % k)
        L.cone(p, c, (c[0], c[1] + 3.2, c[2]), 0.55, 4, mid=0.3, mid_r=1.0)
        m.add(p, GLOW, "ember", EMIT | HOT, group=g, uv="fit")
    return m


# ================================================================ head
@model
def circlet_dragon():
    """A dragon circlet: a band, two long swept horns, and over the brow a dragon's head looking straight out: a broad snout with nostrils, a brow ridge, two glowing eyes,
    a lower jaw with teeth, swept-back crest spines, and a gem held between the jaws."""
    m = WModel("circlet_dragon", "head")
    H._band(m, 6.2, 7.6, 4.5, 4.5, 0.5, None, 0.1, 0.0, tile="gilt")
    for sx in (-1, 1):
        pts = L.bez3((sx * 4.4, 7.2, -1.0), (sx * 6.6, 8.6, 0.0), (sx * 7.0, 11.8, 2.4), (sx * 4.4, 14.6, 5.6), 8)
        h = Part("horn%d" % sx)
        sweep(h, pts, circle(1.0, 6), scales=[(1.0, 1.0), (0.96, 0.96), (0.9, 0.9), (0.8, 0.8), (0.66, 0.66), (0.5, 0.5), (0.34, 0.34), (0.18, 0.18), (0.0, 0.0)])
        m.add(h, TRIM, "horn", SHELL)
        for k in (2, 4):
            r = Part("hornring%d_%d" % (k, sx))
            L.torus(r, pts[k], 1.0 - 0.1 * k, 0.17, 7, 4, plane="xz")
            m.add(r, MAIN, "gilt")
    # the dragon head, scaled big: skull block, snout wedge, jaw, brow, eyes, teeth
    sk = Part("skull")
    loft(sk, "y", (0.0, 7.4, -4.0), [(0.0, 2.2, 1.9), (1.8, 2.3, 2.0), (3.4, 1.7, 1.6), (4.4, 0.0, 0.0)], L.circle(1.0, 8))
    m.add(sk, MAIN, "scale", SHELL)
    sn = Part("snout")
    loft(sn, "-z", (0.0, 8.0, -5.2), [(0.0, 1.8, 1.25), (1.4, 1.45, 1.1), (2.8, 1.1, 0.95), (3.4, 0.0, 0.0)], L.circle(1.0, 8))
    m.add(sn, MAIN, "scale", SHELL)
    jw = Part("jaw")
    loft(jw, "-z", (0.0, 7.0, -4.8), [(0.0, 1.6, 0.6), (1.6, 1.25, 0.5), (3.2, 0.7, 0.35), (3.7, 0.0, 0.0)], L.circle(1.0, 8))
    m.add(jw, DARK, "scale")
    for sx in (-1, 1):
        br = Part("brow%d" % sx)
        pts = L.bez((sx * 0.5, 9.7, -5.2), (sx * 2.0, 10.5, -5.0), (sx * 3.0, 9.2, -3.9), 4)
        sweep(br, pts, circle(0.5, 5), scales=[(1.0, 1.0), (1.15, 1.15), (0.8, 0.8), (0.45, 0.45), (0.0, 0.0)])
        m.add(br, TRIM, "gilt")
        e = Part("eye%d" % sx)
        L.dia(e, (sx * 1.4, 9.0, -5.5), 0.55, 0.4, 0.3)
        m.add(e, GLOW, "gem", EMIT | HOT, uv="fit")
        nos = Part("nostril%d" % sx)
        L.dia(nos, (sx * 0.5, 8.6, -8.55), 0.28, 0.28, 0.2)
        m.add(nos, DARK, "metal", uv="fit")
        for k in range(3):
            tt = Part("tooth%d_%d" % (sx, k))
            L.cone(tt, (sx * (0.45 + 0.35 * k), 7.5, -7.4 - 0.5 * k), (sx * (0.45 + 0.35 * k), 6.6, -7.5 - 0.5 * k), 0.17, 3, mid=0.4, mid_r=1.0)
            m.add(tt, NONE, "bone", uv="fit")
    for k in range(4):
        sp = Part("spine%d" % k)
        L.cone(sp, (0.0, 10.4 + 0.2 * k, -2.4 + 1.1 * k), (0.0, 12.6 - 0.3 * k + 0.2 * k, -1.2 + 2.0 * k), 0.5 - 0.06 * k, 4, mid=0.4, mid_r=0.9)
        m.add(sp, TRIM, "horn")
    gm = Part("gem")
    L.dia(gm, (0.0, 6.9, -8.2), 0.45, 0.5, 0.35)
    m.add(gm, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


# ================================================================ cloaks
def _cloak(name, tile="cloth", folds=3.0, amp=0.45, length=22.0, hood=False, collar=None, trim_tile="gilt", split=False, extra=None, width=13.6):
    m = WModel(name, "body")
    B._yoke(m, tile=tile)
    B._clasp(m)
    g = m.group("drape", "swing", (0, 0.2, 2.6), 14.0, 1.6, 0.0)
    fn = B._cloak_sheet(9.2, width, -0.2, -length, 2.75, 1.5, folds, amp)
    if split:
        for half, slot in ((0, MAIN), (1, TRIM)):
            p = Part("half%d" % half)
            L.sheet(p, lambda u, v, half=half: fn(0.5 * half + 0.5 * u, v), 5, 7, -0.5)
            m.add(p, slot, tile, SHELL, group=g)
    else:
        sh = Part("drape")
        L.sheet(sh, fn, 10, 7, -0.5)
        m.add(sh, MAIN, tile, SHELL, group=g)
    B._hem_trim(m, fn, 10, group=g, thick=-0.62, tile=trim_tile)
    if collar == "fur":
        col = Part("fur_collar")
        L.band(col, -1.0, 1.6, 5.25, 3.25, 0.9, L.RN, 0.3, 0.45, 3.0)
        m.add(col, TRIM, "fur", SHELL)
    if hood:
        hd = Part("hood")
        L.sph(hd, (0, 1.2, 3.2), 3.2, 3, 8, squash=0.9)
        m.add(hd, MAIN, tile, SHELL)
        tip = Part("hood_tip")
        L.cone(tip, (0, 1.2, 4.2), (0, -4.0, 6.4), 2.3, 5, mid=0.4, mid_r=0.8)
        m.add(tip, MAIN, tile, SHELL, group=g)
    if extra:
        extra(m, g, fn)
    return m


@model
def cloak_levitation():
    """The Cloak of Levitation: a collar yoke and a drape that ends in a ragged hem of seven separate floating tongues that bob on their own, each tipped with a gold rune glow."""
    m = WModel("cloak_levitation", "body")
    B._yoke(m)
    B._clasp(m)
    g = m.group("drape", "swing", (0, 0.2, 2.6), 10.0, 1.4, 0.0)
    fn = B._cloak_sheet(9.2, 12.4, -0.2, -12.5, 2.75, 1.2, 3.0, 0.35)
    sh = Part("drape")
    L.sheet(sh, fn, 10, 4, -0.5)
    m.add(sh, MAIN, "cloth", SHELL, group=g)
    B._hem_trim(m, fn, 10, v0=0.9, group=g, thick=-0.62)
    for k in range(7):
        u0, u1 = k / 7.0, (k + 1) / 7.0
        gk = m.group("tongue%d" % k, "bob", (0, -12.0, 3.0), 0.9 + 0.25 * (k % 3), 0.09 + 0.012 * k, k * 0.9)
        t = Part("tongue%d" % k)
        def tf(u, v, k=k, u0=u0, u1=u1):
            x, y, z = fn(u0 + (u1 - u0) * u, 1.0)
            return (x, -12.8 - v * (5.5 + 1.6 * (k % 3)), z + 0.6 + 0.5 * math.sin(v * 2.4 + k))
        L.sheet(t, tf, 1, 3, -0.4)
        m.add(t, MAIN, "silk", SHELL, group=gk)
        rn = Part("rune%d" % k)
        x, y, z = tf(0.5, 1.0)
        L.dia(rn, (x, y - 0.4, z + 0.4), 0.45, 0.6, 0.14)
        m.add(rn, GLOW, "spark", EMIT | HOT, group=gk, uv="fit")
    return m


@model
def cape_nine_lives():
    """The Cape of Nine Lives: a short cape with a trimmed hem along which nine small glowing paw-stones sit, one for each life, and cat-ear points on the collar."""
    m = WModel("cape_nine_lives", "body")
    B._yoke(m, tile="silk")
    B._clasp(m)
    g = m.group("drape", "swing", (0, 0.2, 2.6), 18.0, 1.4, 0.0)
    fn = B._cloak_sheet(8.8, 11.0, -0.2, -15.0, 2.7, 0.9, 2.0, 0.3)
    sh = Part("drape")
    L.sheet(sh, fn, 8, 5, -0.5)
    m.add(sh, MAIN, "silk", SHELL, group=g)
    B._hem_trim(m, fn, 8, v0=0.9, group=g, thick=-0.62)
    for k in range(9):
        u = (k + 0.5) / 9.0
        x, y, z = fn(u, 0.955)
        p = Part("paw%d" % k)
        L.dia(p, (x, y, z + 0.9), 0.42, 0.5, 0.18)
        m.add(p, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    for sx in (-1, 1):
        ear = Part("ear%d" % sx)
        L.cone(ear, (sx * 3.2, 0.4, 1.9), (sx * 3.6, 3.4, 1.6), 1.1, 4, mid=0.4, mid_r=0.8, squash=0.5)
        m.add(ear, MAIN, "silk", SHELL)
    return m


@model
def cloak_scaled():
    """A scaled cloak of the deep: a hooded drape of overlapping scales with a ragged hem of fin-points and a glowing pearl clasp."""
    def extra(m, g, fn):
        for k in range(9):
            u = (k + 0.5) / 9.0
            x, y, z = fn(u, 1.0)
            f = Part("fin%d" % k)
            L.cone(f, (x, y + 0.6, z + 0.5), (x, y - 2.2 - (k % 2) * 0.8, z + 0.9), 0.7, 4, mid=0.4, mid_r=0.9, squash=0.5)
            m.add(f, TRIM, "scale", SHELL, group=g)
    return _cloak("cloak_scaled", tile="scale", folds=2.0, amp=0.35, hood=True, trim_tile="scale", extra=extra)


@model
def cloak_furred():
    """A winter cloak: a heavy fur collar, a deep drape with wide folds and a trim of white fur at the hem."""
    return _cloak("cloak_furred", tile="cloth", folds=2.5, amp=0.55, collar="fur", trim_tile="fur")


@model
def cloak_split():
    """A two-faced cloak: the left half of the drape in the main colour, the right half in the second, a hard line down the spine."""
    return _cloak("cloak_split", tile="silk", folds=3.5, amp=0.4, split=True)


@model
def cloak_elven():
    """A hooded cloak of shifting leaves: a deep hood pulled up behind the head, a long drape in soft folds and a hem cut into leaf points."""
    def extra(m, g, fn):
        for k in range(8):
            u = (k + 0.5) / 8.0
            x, y, z = fn(u, 1.0)
            f = Part("leafhem%d" % k)
            L.feather(f, (x, y + 0.8, z + 0.6), (x, y - 2.8, z + 0.9), 0.9, 0.2, 4)
            m.add(f, ACCENT, "feather", SHELL, group=g, uv="fit")
    return _cloak("cloak_elven", tile="cloth", folds=3.0, amp=0.5, hood=True, extra=extra)


@model
def cloak_mimic():
    """A tamed cloak mimic: a cloak whose hem is a row of fangs, with two big eyes glowing on the back and a pink tongue lolling out of the lower hem."""
    def extra(m, g, fn):
        for k in range(10):
            u = (k + 0.5) / 10.0
            x, y, z = fn(u, 1.0)
            f = Part("fang%d" % k)
            L.cone(f, (x, y + 0.6, z + 0.5), (x, y - 2.0 - (k % 2), z + 0.7), 0.55, 4, mid=0.3, mid_r=1.0)
            m.add(f, NONE, "bone", 0, group=g, uv="fit")
        for sx in (-1, 1):
            e = Part("eye%d" % sx)
            x, y, z = fn(0.5 + sx * 0.18, 0.18)
            L.dia(e, (x, y, z + 1.0), 0.8, 0.5, 0.2)
            m.add(e, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
        tg = Part("tongue")
        x, y, z = fn(0.5, 1.0)
        L.cone(tg, (x, y + 0.4, z + 0.8), (x, y - 4.2, z + 1.4), 0.9, 4, mid=0.5, mid_r=0.9, squash=0.4)
        m.add(tg, ACCENT, "silk", 0, group=g, uv="fit")
    return _cloak("cloak_mimic", tile="leather", folds=2.0, amp=0.35, extra=extra)


@model
def cape_striped():
    """A towel worn as a cape: a short cape in broad stripes, knotted at the neck with a tail hanging at the side."""
    m = WModel("cape_striped", "body")
    B._yoke(m, tile="stripe")
    B._clasp(m, gem=False)
    g = m.group("drape", "swing", (0, 0.2, 2.6), 18.0, 1.4, 0.0)
    fn = B._cloak_sheet(9.2, 10.6, -0.2, -14.0, 2.7, 0.8, 1.5, 0.25)
    sh = Part("drape")
    L.sheet(sh, fn, 8, 5, -0.5)
    m.add(sh, MAIN, "stripe", SHELL, group=g)
    B._hem_trim(m, fn, 8, v0=0.93, group=g, thick=-0.62, tile="cloth", slot=TRIM)
    kn = Part("knot")
    L.sph(kn, (0, -1.6, -3.0), 1.1, 3, 6)
    m.add(kn, TRIM, "stripe")
    return m


# ================================================================ neck
@model
def amulet_coven():
    """The Amulet of the Coven: a ring with a five-pointed star inside it, a red stone at the centre, three small hanging charms (a bone, a feather and a bead) beneath."""
    m = WModel("amulet_coven", "body")
    N._chain(m, 3.2)
    N._hang(m, "c", -3.6)
    c = (0.0, -5.4, N.PZ)
    ring = Part("ring")
    L.torus(ring, c, 2.0, 0.3, 14, 4, plane="xy")
    m.add(ring, MAIN, "gilt")
    for k in range(5):
        a0 = math.pi / 2 + k * 2 * math.pi / 5
        a1 = math.pi / 2 + ((k + 2) % 5) * 2 * math.pi / 5
        p0 = (c[0] + 1.95 * math.cos(a0), c[1] + 1.95 * math.sin(a0), c[2])
        p1 = (c[0] + 1.95 * math.cos(a1), c[1] + 1.95 * math.sin(a1), c[2])
        b = Part("bar%d" % k)
        sweep(b, [p0, p1], circle(0.2, 4), scales=[(1.0, 1.0), (1.0, 1.0)], caps=(True, True))
        m.add(b, TRIM, "gilt")
    g = Part("stone")
    L.ml.gem(g, (c[0], c[1], c[2] - 0.3), 0.75, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    gr = m.group("charms", "swing", (0, -7.4, N.PZ), 8.0, 0.8, 0.0)
    for k, (dx, kind) in enumerate(((-1.2, "bone"), (0.0, "bead"), (1.2, "feather"))):
        x = dx
        if kind == "bone":
            p = Part("bone")
            L.cone(p, (x, -7.6, N.PZ - 0.1), (x, -9.8, N.PZ - 0.1), 0.32, 4, mid=0.5, mid_r=0.8)
            m.add(p, NONE, "bone", 0, group=gr, uv="fit")
        elif kind == "bead":
            p = Part("bead")
            L.dia(p, (x, -8.6, N.PZ - 0.1), 0.4, 0.5, 0.4)
            m.add(p, GLOW, "gem", EMIT, group=gr, uv="fit")
        else:
            p = Part("feather")
            L.feather(p, (x, -7.6, N.PZ - 0.1), (x + 0.2, -10.0, N.PZ - 0.1), 0.45, 0.14, 4)
            m.add(p, ACCENT, "feather", 0, group=gr, uv="fit")
    return m


@model
def amulet_forest():
    """The Amulet of the Forest: a green stone framed by a wreath of eight leaves, with two curling vines and a small acorn cap beneath."""
    m = WModel("amulet_forest", "body")
    N._chain(m, 3.2, tile="leather", slot=MAIN, name="vine")
    N._hang(m, "f", -3.6)
    c = (0.0, -5.4, N.PZ)
    for k in range(8):
        a = 2 * math.pi * k / 8
        f = Part("leaf%d" % k)
        L.feather(f, (c[0] + 1.2 * math.cos(a), c[1] + 1.2 * math.sin(a), c[2]), (c[0] + 2.8 * math.cos(a + 0.3), c[1] + 2.8 * math.sin(a + 0.3), c[2] - 0.2), 0.8, 0.22, 4, curl=0.12)
        m.add(f, ACCENT if k % 2 else TRIM, "feather", SHELL, uv="fit")
    g = Part("stone")
    L.ml.gem(g, (c[0], c[1], c[2] - 0.25), 0.95, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    ac = Part("acorn")
    L.sph(ac, (0.0, -8.5, N.PZ - 0.1), 0.6, 3, 6, squash=1.2)
    m.add(ac, MAIN, "wood", uv="fit")
    return m


@model
def pendant_fang():
    """Hemlock's Amulet: a curved viper fang in a silver cap, hanging beside a five-petal purple flower with a glowing heart."""
    m = WModel("pendant_fang", "body")
    N._chain(m, 3.2)
    N._hang(m, "f", -3.6)
    cap = Part("cap")
    loft(cap, "y", (0.0, -4.2, N.PZ), [(0.0, 0.7, 0.6), (-1.0, 0.95, 0.8), (-1.4, 0.0, 0.0)], L.circle(1.0, 8))
    m.add(cap, TRIM, "gilt")
    pts = L.bez((0.0, -5.2, N.PZ), (0.8, -8.0, N.PZ - 0.3), (-0.5, -10.4, N.PZ - 0.2), 6)
    f = Part("fang")
    sweep(f, pts, circle(0.7, 6), scales=[(1.0, 1.0), (0.85, 0.85), (0.7, 0.7), (0.5, 0.5), (0.3, 0.3), (0.15, 0.15), (0.0, 0.0)])
    m.add(f, NONE, "bone", SHELL, uv="fit")
    for k in range(5):
        a = math.pi / 2 + k * 2 * math.pi / 5
        pe = Part("petal%d" % k)
        L.cone(pe, (1.9 + 0.2 * math.cos(a), -7.0 + 0.2 * math.sin(a), N.PZ - 0.2), (1.9 + 1.5 * math.cos(a), -7.0 + 1.5 * math.sin(a), N.PZ - 0.5), 0.55, 4, mid=0.4, mid_r=0.9, squash=0.5)
        m.add(pe, MAIN, "silk")
    hr = Part("heart")
    L.dia(hr, (1.9, -7.0, N.PZ - 0.55), 0.35, 0.35, 0.3)
    m.add(hr, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def pendant_skull():
    """The Amulet of Lost Souls: a small skull on a chain, its eye sockets glowing, with pale soul-flames circling it."""
    m = WModel("pendant_skull", "body")
    N._chain(m, 3.2)
    N._hang(m, "s", -3.6)
    c = (0.0, -5.6, N.PZ)
    cr = Part("cranium")
    L.sph(cr, (c[0], c[1] + 0.3, c[2]), 1.5, 4, 8)
    m.add(cr, NONE, "bone", SHELL, uv="fit")
    jw = Part("jaw")
    loft(jw, "y", (c[0], c[1] - 0.8, c[2] - 0.2), [(0.0, 1.0, 0.8), (-1.0, 0.8, 0.7), (-1.3, 0.0, 0.0)], L.circle(1.0, 6))
    m.add(jw, NONE, "bone", uv="fit")
    for sx in (-1, 1):
        e = Part("eye%d" % sx)
        L.dia(e, (c[0] + sx * 0.6, c[1] + 0.4, c[2] - 1.35), 0.38, 0.45, 0.25)
        m.add(e, GLOW, "gem", EMIT | HOT, uv="fit")
    nz = Part("nose")
    L.dia(nz, (c[0], c[1] - 0.2, c[2] - 1.4), 0.15, 0.3, 0.15)
    m.add(nz, DARK, "metal", uv="fit")
    def mk(p, cc, k):
        L.cone(p, (cc[0], cc[1] + 0.8, cc[2]), (cc[0], cc[1] - 1.2, cc[2]), 0.4, 4, mid=0.4, mid_r=1.0)
    gs = [m.group("souls%d" % g, "orbit", (c[0], c[1], c[2]), 2.2 * (1 if g == 0 else -1.0), 0.0, 0.0) for g in range(1)]
    for k in range(4):
        a = 2 * math.pi * k / 4
        p = Part("soul%d" % k)
        mk(p, (c[0] + 2.7 * math.cos(a), c[1] + 0.6 * math.sin(k * 2.0), c[2] + 2.7 * math.sin(a)), k)
        m.add(p, ACCENT, "soft", EMIT, group=gs[0], uv="fit")
    return m


@model
def amulet_dragon_claw():
    """The Amulet of Dragon Rage: three curved black claws gripping a blue stone, scales along the chain and a small flame under the stone."""
    m = WModel("amulet_dragon_claw", "body")
    N._chain(m, 3.2, tile="chain")
    N._hang(m, "d", -3.6)
    c = (0.0, -5.6, N.PZ)
    g = Part("stone")
    L.sph(g, (c[0], c[1], c[2] - 0.3), 1.05, 3, 8)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(3):
        a = math.pi / 2 + k * 2 * math.pi / 3
        pts = L.bez3((c[0] + 0.6 * math.cos(a), c[1] + 0.6 * math.sin(a) + (1.1 if k == 0 else 0.0), c[2] + 0.2),
                     (c[0] + 2.0 * math.cos(a), c[1] + 2.0 * math.sin(a), c[2] - 0.2),
                     (c[0] + 1.8 * math.cos(a), c[1] + 1.8 * math.sin(a), c[2] - 1.5),
                     (c[0] + 0.5 * math.cos(a), c[1] + 0.5 * math.sin(a), c[2] - 1.9), 6)
        cl = Part("claw%d" % k)
        sweep(cl, pts, circle(0.42, 5), scales=[(1.0, 1.0), (0.95, 0.95), (0.85, 0.85), (0.7, 0.7), (0.5, 0.5), (0.3, 0.3), (0.0, 0.0)])
        m.add(cl, MAIN, "scale", SHELL)
    fl = Part("flame")
    L.cone(fl, (c[0], c[1] - 1.6, c[2] - 0.3), (c[0], c[1] - 3.4, c[2] - 0.3), 0.6, 4, mid=0.3, mid_r=1.0)
    m.add(fl, ACCENT, "ember", EMIT | HOT, uv="fit")
    return m


@model
def pendant_echo():
    """The Pendant of Echoed Souls: a pale droplet pendant circled by two thin glowing rings turning against each other, like ripples of a voice."""
    m = WModel("pendant_echo", "body")
    N._chain(m, 3.2)
    N._hang(m, "e", -3.6)
    c = (0.0, -5.8, N.PZ)
    drop = Part("drop")
    loft(drop, "y", (c[0], c[1] + 1.4, c[2]), [(0.0, 0.5, 0.45), (-0.9, 1.0, 0.8), (-2.2, 0.8, 0.6), (-3.2, 0.0, 0.0)], L.circle(1.0, 6))
    m.add(drop, MAIN, "pearl", SHELL, uv="fit")
    for gi, (rr, sp) in enumerate(((2.2, 1.8), (3.2, -1.2))):
        g = m.group("echo%d" % gi, "orbit", (c[0], c[1], c[2]), sp, 0.0, 0.0)
        r = Part("ring%d" % gi)
        L.torus(r, c, rr, 0.13, 16, 4, plane="xz")
        m.add(r, GLOW, "white", EMIT, group=g, uv="fit")
        d = Part("bead%d" % gi)
        L.dia(d, (c[0] + rr, c[1], c[2]), 0.3, 0.3, 0.3)
        m.add(d, GLOW, "spark", EMIT | HOT, group=g, uv="fit")
    return m


@model
def amulet_sea_stars():
    """The Amulet of the Sea of Stars: a dark medallion set with a swirl of tiny stars, three bright stars orbiting it."""
    m = WModel("amulet_sea_stars", "body")
    N._chain(m, 3.2)
    N._hang(m, "s", -3.6)
    c = (0.0, -5.6, N.PZ)
    ring = Part("ring")
    L.torus(ring, c, 1.8, 0.3, 14, 4, plane="xy")
    m.add(ring, TRIM, "gilt")
    disc = Part("disc")
    loft(disc, "z", (c[0], c[1], c[2] - 0.1), [(0.0, 1.8, 1.8), (0.3, 1.8, 1.8)], L.circle(1.0, 14))
    m.add(disc, DARK, "plate", SHELL, uv="fit")
    for k in range(7):
        a = 2.4 * k
        r = 0.25 + 0.17 * k
        s = Part("pin%d" % k)
        L.dia(s, (c[0] + r * math.cos(a), c[1] + r * math.sin(a), c[2] - 0.5), 0.18, 0.18, 0.15)
        m.add(s, GLOW, "spark", EMIT | HOT, uv="fit")
    g = m.group("stars", "orbit", (c[0], c[1], c[2] - 0.8), 2.6, 0.0, 0.0)
    for k in range(3):
        a = 2 * math.pi * k / 3
        s = Part("star%d" % k)
        L.dia(s, (c[0] + 2.8 * math.cos(a), c[1] + 0.0, c[2] - 0.8 + 2.8 * math.sin(a)), 0.5, 0.9, 0.12)
        m.add(s, GLOW, "spark", EMIT | HOT, group=g, uv="fit")
    return m


@model
def amulet_ward():
    """The Amulet of Safeguarding: a broad shield-shaped pendant with a pink ward stone and a thin ward ring pulsing round it."""
    m = WModel("amulet_ward", "body")
    N._chain(m, 3.0)
    N._hang(m, "w", -3.4)
    sh = Part("shield")
    pts = [(-1.7, 1.7), (1.7, 1.7), (1.9, -0.4), (1.0, -2.0), (0.0, -2.9), (-1.0, -2.0), (-1.9, -0.4)]
    loft(sh, "z", (0, -6.4, N.PZ - 0.05), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], pts)
    m.add(sh, MAIN, "plate", SHELL, uv="fit")
    gm = Part("stone")
    L.ml.gem(gm, (0, -6.1, N.PZ - 0.7), 0.9, 0.9, 6, "z")
    m.add(gm, GLOW, "gem", EMIT | HOT, uv="fit")
    g = m.group("ward", "pulse", (0, -6.2, N.PZ - 0.5), 0.12, 0.12, 0.0)
    rg = Part("ring")
    L.torus(rg, (0, -6.2, N.PZ - 0.6), 3.0, 0.14, 18, 4, plane="xy")
    m.add(rg, GLOW, "white", EMIT, group=g, uv="fit")
    return m


# ================================================================ charms, belts, boots, hands, chests
@model
def charm_heart():
    """Life's Emergency: a red heart-shaped gem floating at the hip in a thin gold cage, beating softly."""
    m = WModel("charm_heart", "body")
    g = m.group("beat", "pulse", (9.6, -9.0, -0.5), 0.12, 0.22, 0.0)
    c = (9.6, -9.0, -0.5)
    for sx in (-1, 1):
        lobe = Part("lobe%d" % sx)
        L.sph(lobe, (c[0] + sx * 0.7, c[1] + 0.55, c[2]), 0.85, 3, 7)
        m.add(lobe, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    tip = Part("tip")
    L.cone(tip, (c[0], c[1] + 0.9, c[2]), (c[0], c[1] - 1.7, c[2]), 1.0, 5, mid=0.4, mid_r=0.9)
    m.add(tip, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    cg = Part("cage")
    L.torus(cg, c, 1.9, 0.12, 12, 4, plane="xy")
    m.add(cg, TRIM, "gilt", 0, group=g)
    cg2 = Part("cage2")
    L.torus(cg2, c, 1.9, 0.12, 12, 4, plane="yz")
    m.add(cg2, TRIM, "gilt", 0, group=g)
    return m


@model
def charm_urn():
    """The Urn of Black Pudding: a squat urn with a bound lid hanging at the hip on a chain, a glowing slit of orange light round its middle and a dripping tongue of ooze."""
    m = WModel("charm_urn", "body")
    cd = Part("chain")
    pts = L.bez3((3.8, -8.4, 0.9), (5.4, -9.0, -0.3), (5.5, -10.2, -0.8), (5.4, -11.0, -0.8), 6)
    sweep(cd, pts, circle(0.2, 4), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
    m.add(cd, TRIM, "chain")
    g = m.group("swing", "swing", (4.6, -9.0, 0.0), 6.0, 0.7, 0.0)
    u = Part("urn")
    lathe(u, "y", (5.4, -16.8, -0.8), [(0.0, 1.0), (0.8, 1.8), (2.2, 2.3), (3.8, 2.0), (5.0, 1.3), (5.6, 1.5), (5.9, 1.1)], sides=8, caps=(True, True))
    m.add(u, MAIN, "plate", SHELL, group=g, uv="tile", density=3.0)
    lid = Part("lid")
    L.sph(lid, (5.4, -10.9, -0.8), 1.2, 3, 8, squash=0.6)
    m.add(lid, TRIM, "gilt", 0, group=g)
    band = Part("glow_band")
    L.band(band, -14.2, -13.5, 2.3, 2.3, 0.14, 8, 0.0, 0.0, 2.0)
    band.V = [v + np.array([5.4, 0.0, -0.8]) for v in band.V]
    m.add(band, GLOW, "flame", EMIT | HOT, group=g, uv="fit")
    dr = Part("drip")
    L.cone(dr, (5.4 + 1.6, -14.0, -0.8), (5.4 + 1.8, -17.6, -0.8), 0.45, 4, mid=0.3, mid_r=1.0)
    m.add(dr, GLOW, "ember", EMIT, group=g, uv="fit")
    return m


@model
def belt_pouches():
    """A heavy belt with two bulging pouches at the front hips, a strap of coin-studs and a tassel: the bags of transference's companion look."""
    m = BO.belt_buckled()
    m.name = "belt_pouches"
    for sx in (-1, 1):
        pch = Part("pouch_f%d" % sx)
        L.sph(pch, (sx * 3.6, -12.0, -3.4), 1.6, 3, 8, squash=0.9)
        m.add(pch, MAIN, "leather", SHELL)
    return m


@model
def boots_crystal():
    """Ruby boots: cuffed boots with a crown of faceted ruby crystals round each ankle and a glowing ruby on the toe."""
    m = BO.boots_cuffed()
    m.name = "boots_crystal"
    for k in range(7):
        a = 2 * math.pi * k / 7
        x, z = 2.7 * math.cos(a), 2.7 * math.sin(a)
        cr = Part("crystal%d" % k)
        L.cone(cr, (x, -6.0, z), (x * 1.5, -3.5 - (k % 3) * 0.7, z * 1.5), 0.55, 4, mid=0.5, mid_r=1.0)
        m.add(cr, GLOW, "crystal", EMIT | HOT, uv="fit")
    rb = Part("toe_ruby")
    L.dia(rb, (0.0, -10.6, -3.6), 0.6, 0.6, 0.45)
    m.add(rb, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def boots_shadow():
    """Soft boots of a burglar: wrapped leather with a quiet sole, trailing wisps of shadow that sway at the ankle and a small pale star-stone."""
    m = BO.boots_cuffed()
    m.name = "boots_shadow"
    g = m.group("shadow", "sway", (0.0, -8.0, 0.0), 8.0, 0.12, 0.0)
    for k in range(4):
        a = 2 * math.pi * k / 4 + 0.4
        w = Part("wisp%d" % k)
        L.cone(w, (2.5 * math.cos(a), -8.5, 2.5 * math.sin(a)), (3.4 * math.cos(a), -5.0 - (k % 2), 3.4 * math.sin(a)), 0.7, 4, mid=0.4, mid_r=1.0)
        m.add(w, ACCENT, "soft", EMIT, group=g, uv="fit")
    return m


@model
def greaves_windwalker():
    """Greaves of the Wind Walker: plated greaves with a pair of long feathered wings at each ankle, glowing wind-ribbons trailing from the heels, and a stone on the knee."""
    m = BO.greaves_plated()
    m.name = "greaves_windwalker"
    g = m.group("wings", "sway", (0.0, -8.0, 0.0), 7.0, 0.14, 0.0)
    for sx in (-1, 1):
        for k in range(5):
            f = Part("feather%d_%d" % (sx, k))
            L.feather(f, (sx * 2.6, -8.0 - 0.5 * k, 0.5), (sx * (7.0 - 0.4 * k), -4.6 + 1.4 * k - 1.2, 2.5 + 0.5 * k), 0.85 - 0.05 * k, 0.22, 4, curl=0.0)
            m.add(f, MAIN if k % 2 else ACCENT, "feather", SHELL, group=g, uv="fit")
        rb = Part("ribbon%d" % sx)
        pts = L.bez3((sx * 2.0, -11.6, 2.0), (sx * 3.0, -10.8, 5.0), (sx * 1.4, -9.6, 7.0), (sx * 2.6, -8.4, 9.0), 6)
        sweep(rb, pts, circle(0.28, 4), scales=[(1.0, 1.0), (0.9, 0.9), (0.8, 0.8), (0.6, 0.6), (0.4, 0.4), (0.2, 0.2), (0.0, 0.0)])
        m.add(rb, GLOW, "white", EMIT | HOT, group=g, uv="fit")
    return m


@model
def bracer_phasing():
    """Bracers of Phasing: a bracer with a ghost-ring of three thin hoops that turn about the forearm like afterimages, and a pale stone."""
    m = BO.bracer_plated()
    m.name = "bracer_phasing"
    g = m.group("ghost", "spin", (-1.0, -6.0, 0.0), 3.0, 0.0, 0.0)
    for k in range(3):
        r = Part("hoop%d" % k)
        L.torus(r, (-1.0, -4.2 - 1.9 * k, 0.0), 3.6 - 0.2 * k, 0.12, 16, 4, plane="xz")
        m.add(r, GLOW, "white", EMIT, group=g, uv="fit")
    return m


@model
def pauldron_dragon():
    """A dragon-scale pauldron: scaled plates over the shoulder with two horn-spikes sweeping back and a short crest of fins."""
    m = WModel("pauldron_dragon", "rarm")
    BO._pauldron(m, spikes=False, tile="scale")
    for k in range(2):
        pts = L.bez((-3.6 + 0.6 * k, 2.6, -0.8 + 1.6 * k), (-6.0, 5.2, 0.4 + 1.2 * k), (-5.4, 7.6, 2.6 + 1.0 * k), 5)
        h = Part("horn%d" % k)
        sweep(h, pts, circle(0.8, 5), scales=[(1.0, 1.0), (0.85, 0.85), (0.65, 0.65), (0.4, 0.4), (0.2, 0.2), (0.0, 0.0)])
        m.add(h, TRIM, "horn", SHELL)
    return m


@model
def pauldron_horned():
    """A cursed pauldron: a dark plate with a long curved horn and hanging chain links, a dim stone set in the plate."""
    m = WModel("pauldron_horned", "rarm")
    BO._pauldron(m, spikes=False)
    pts = L.bez3((-3.8, 2.0, 0.0), (-6.5, 4.0, -0.4), (-7.0, 7.4, -0.6), (-5.0, 9.4, -0.2), 7)
    h = Part("horn")
    sweep(h, pts, circle(1.0, 6), scales=[(1.0, 1.0), (0.92, 0.92), (0.8, 0.8), (0.66, 0.66), (0.5, 0.5), (0.3, 0.3), (0.14, 0.14), (0.0, 0.0)])
    m.add(h, ACCENT, "horn", SHELL)
    g = m.group("chain", "swing", (-1.0, -1.0, -2.4), 6.0, 1.0, 0.0)
    pts = [(-1.0 + 0.3 * math.sin(k * 1.3), -1.4 - 0.9 * k, -3.2) for k in range(6)]
    ch = Part("chain")
    sweep(ch, pts, circle(0.22, 4), scales=[(1.0, 1.0), (0.6, 0.6)] * 3, caps=(True, True))
    m.add(ch, TRIM, "chain", 0, group=g)
    gm = Part("stone")
    L.dia(gm, (-1.0, 2.6, -2.7), 0.5, 0.5, 0.35)
    m.add(gm, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def pauldron_skull():
    """The Armor of the Beheaded: a plated shoulder carrying a small skull trophy with glowing sockets, a spike beside it."""
    m = WModel("pauldron_skull", "rarm")
    BO._pauldron(m, spikes=True)
    cr = Part("skull")
    L.sph(cr, (-3.9, 2.4, -1.0), 1.4, 3, 8)
    m.add(cr, NONE, "bone", SHELL, uv="fit")
    jw = Part("jaw")
    L.cone(jw, (-3.9, 1.6, -1.2), (-3.9, 0.2, -1.5), 0.9, 4, mid=0.4, mid_r=0.9)
    m.add(jw, NONE, "bone", uv="fit")
    for sz in (-1, 1):
        e = Part("eye%d" % sz)
        L.dia(e, (-5.0, 2.7, -1.0 + sz * 0.5), 0.2, 0.3, 0.3)
        m.add(e, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def chest_stars():
    """Starstudded leathers: crossed straps set with small glowing stars, and a stud of light at each shoulder."""
    m = BO.chest_straps()
    m.name = "chest_stars"
    for k in range(6):
        t = k / 5.0
        x, y = -3.0 + 6.0 * t, -1.0 - 10.0 * (1 - t) * 0.0 - 9.5 * t
        s = Part("star%d" % k)
        L.dia(s, (x * (-1 if k % 2 else 1) * 0.8, -1.0 - 1.8 * k, -3.75), 0.45, 0.45, 0.2)
        m.add(s, GLOW, "spark", EMIT | HOT, uv="fit")
    return m


@model
def suit_tails():
    """The Suit of the Golden Shadow: a stand collar, a short tailcoat with golden piping and a cloak of shadow behind the shoulders."""
    m = BO.coat_tails()
    m.name = "suit_tails"
    g = m.group("shadow", "swing", (0, 0.2, 2.6), 10.0, 1.2, 0.0)
    fn = B._cloak_sheet(8.4, 9.6, -0.6, -10.0, 2.8, 0.8, 2.0, 0.3)
    sh = Part("shadow")
    L.sheet(sh, fn, 6, 4, -0.5)
    m.add(sh, DARK, "silk", SHELL, group=g)
    B._hem_trim(m, fn, 6, v0=0.92, group=g, thick=-0.62)
    return m
