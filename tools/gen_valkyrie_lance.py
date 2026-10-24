#!/usr/bin/env python3
"""
VALKYRIE DRILL LANCE: three candidate designs = PREVIEW ONLY (nothing is registered in the game, nothing goes to src/ or assets/).

The core weapon of the Valkyrie armour is, from the owner's frames, the giant SPIRAL DRILL LANCE: a thick conical corkscrew of water, stacked frosted white ring collars between blue ribbed bands, a spiral
groove, a sharp needle tip, blue water ribbons trailing; worn attached at the lower body / legs like a tail (Valkyrie Dress), pointing down-forward, and also held or floating.  The owner asked for three different
versions before anything is built:

  A  "Frosted Drill"            the closest to the frames: a fat cone of six fluffy white foam collars with deep-blue ribbed bands between them, one shallow spiral groove, needle tip
  B  "Crystal Corkscrew"        a sharp faceted translucent ice-crystal screw (a three-lobed section twisted three turns), a thin glowing core inside it, frosted ring collars, water ribbons
  C  "Living Water Dragon Lance" a coiled sea-dragon tail: scalloped scale body spiralling round the axis, fan fins on every segment, ending in a drill point (the Mermaid Form idea)

Mesh text format = the `q` lines of tools/gen_sdw3_models.py (`Mesh`).  Layers: 0 plate (toon), 1 emissive flat (additive), 3 CARD (textured, cut-out), 4 VEIL (textured, translucent), 5 GLOW (textured,
additive) as in tools/gen_valkyrie_armor_v2.py, plus 6 FROST (white foam plate), 7 DEEP (blue band plate), 8 SCALE (blue scale plate), 9 GLASS (flat translucent crystal).
Units: model pixels (1/16 block); mesh space = the weapon frame of tools/gen_weapon_models.py: the long axis is +y, the BUTT at y = 0, the hand grip about a quarter of the way up (GRIP = 0.25 x length), x across, z through.

    python tools/gen_valkyrie_lance.py   ->  docs/previews/vfx_remake/valkyrie/meshes/lance_<A|B|C>[_mid|_lo].txt, lances.json, counts_lance.txt, tex/valk2_fin.png
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_sdw3_models import Mesh, add, sub, mul, cross, dot, norm, mean                 # noqa: E402
from gen_valkyrie_armor_v2 import smooth, W, P, A, D, G, card, TEXD, tex_fin, tex_foam   # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
BASE = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie")
OUT = os.path.join(BASE, "meshes")

FROST, DEEP, SCALE, GLASS = 6, 7, 8, 9
RIB = (150, 215, 245)
LV = ["near", "mid", "far"]
NM = ["", "_mid", "_lo"]
LEN = {"A": 52.0, "B": 54.0, "C": 56.0}
GRIP = 0.25


def ring_pts(y, r, n, twist=0.0, cx=0.0, cz=0.0, lobes=None, groove=0.0, gw=0.5):
    """n points round the axis at height y.  lobes = (k, inner) makes a star section of k lobes (2k points, alternate radius 1 and `inner`); groove dents one point (the spiral groove) following the twist."""
    out = []
    for i in range(n):
        a = twist + 2 * math.pi * i / n
        rr = r
        if lobes:
            rr = r * (1.0 if i % 2 == 0 else lobes[1])
        if groove:
            d = math.atan2(math.sin(a - twist), math.cos(a - twist))
            rr *= 1.0 - groove * math.exp(-(d / gw) ** 2)
        out.append((cx + rr * math.cos(a), y, cz + rr * math.sin(a)))
    return out


def gap(m, layer, a, b, rgb):
    n = len(a)
    ref = mean([mean(a), mean(b)])
    for j in range(n):
        k = (j + 1) % n
        m.quad(layer, [a[j], a[k], b[k], b[j]], [(j / n, 0), ((j + 1) / n, 0), ((j + 1) / n, 1), (j / n, 1)], rgb, ref=ref)


def handle(m, n, lod):
    """the grip and the rounded hand guard (the vamplate of the wiki) at the base of the drill."""
    secs = [ring_pts(0.0, 1.5, n), ring_pts(3.0, 1.35, n), ring_pts(5.6, 2.3, n), ring_pts(7.6, 4.4, n)]
    if lod == 2:
        secs = [secs[0], secs[2], secs[3]]
    for i in range(len(secs) - 1):
        gap(m, DEEP if i < len(secs) - 2 else FROST, secs[i], secs[i + 1], W)


def helix_ribbon(m, radius, y0, y1, turns, width, nseg, phase, u=0.2, off=0.9):
    """a water ribbon winding round the shaft: radius(y) is the shaft's radius, the ribbon rides `off` px outside it; textured VEIL (u, v along)."""
    pts = []
    for i in range(nseg + 1):
        s = i / nseg
        y = y0 + (y1 - y0) * s
        a = phase + 2 * math.pi * turns * s
        r = radius(y) + off
        pts.append(((r * math.cos(a), y, r * math.sin(a)), s))
    for i in range(nseg):
        (p0, s0), (p1, s1) = pts[i], pts[i + 1]
        w0, w1 = width * (1.0 - 0.4 * s0), width * (1.0 - 0.4 * s1)
        m.quad(4, [add(p0, (0, -w0 / 2, 0)), add(p1, (0, -w1 / 2, 0)), add(p1, (0, w1 / 2, 0)), add(p0, (0, w0 / 2, 0))], [(u, s0), (u, s1), (u + 0.3, s1), (u + 0.3, s0)], W)


def trail(m, k, nseg, length=22.0, wid=3.2, sign=1.0):
    """a ribbon streaming back from the butt (the blue water ribbons trailing in the frames): an S-curve in x, a little in z, tapering."""
    pts = []
    for i in range(nseg + 1):
        s = i / nseg
        y = -length * s
        x = sign * (1.5 + 5.0 * s) * math.sin(2 * math.pi * (0.8 * s + 0.1 * k)) + sign * 2.0 * s
        z = 2.2 * s * math.cos(2 * math.pi * (0.7 * s + 0.2 * k))
        pts.append(((x, y, z), s))
    for i in range(nseg):
        (p0, s0), (p1, s1) = pts[i], pts[i + 1]
        w0, w1 = wid * (1 - 0.85 * s0), wid * (1 - 0.85 * s1)
        t = norm((0.0, 0.0, 1.0))
        m.quad(4, [add(p0, mul(t, -w0 / 2)), add(p1, mul(t, -w1 / 2)), add(p1, mul(t, w1 / 2)), add(p0, mul(t, w0 / 2))], [(0.12, s0), (0.12, s1), (0.45, s1), (0.45, s0)], W)


def tip_glow(m, y, size):
    for ang in (0.0, math.pi / 2):
        c, s_ = math.cos(ang), math.sin(ang)
        a, b = (-size * c, y, -size * s_), (size * c, y, size * s_)
        m.quad(5, [add(a, (0, -size, 0)), add(b, (0, -size, 0)), add(b, (0, size, 0)), add(a, (0, size, 0))], [(0, 0), (1, 0), (1, 1), (0, 1)], W)


# ============================================================================================ A: Frosted Drill
def lance_a(lod):
    """131.png: a long cone whose body winds in blue and white spiral stripes with fine ribs and one spiral groove, a mane of jagged white foam tufts round its lower third sweeping back toward the butt,
    a needle point, a blue water ribbon looping round the base."""
    m = Mesh("lance_A" + NM[lod])
    n = {0: 8, 1: 6, 2: 5}[lod]
    nr = {0: 15, 1: 9, 2: 5}[lod]
    y0, L = 8.0, LEN["A"]
    H = L - y0
    R0 = 6.4

    def R(y):
        t = min(1.0, max(0.0, (y - y0) / H))
        return R0 * (1 - t) ** 0.8 + 0.3

    handle(m, n, lod)
    prev = None
    turns = 1.8
    for i in range(nr + 1):
        t = i / nr
        y = y0 + H * t
        rib = 1.0 if i % 2 == 0 else 0.93
        ring = ring_pts(y, max(0.12, R(y) * rib), n, twist=2 * math.pi * turns * t, groove=0.28)
        if prev is not None:
            ph = (turns * (i - 0.5) / nr) * n
            for j in range(n):
                k = (j + 1) % n
                fr = ((j - ph) / n) % 1.0
                lay = DEEP if fr < 0.42 else FROST
                m.quad(lay, [prev[j], prev[k], ring[k], ring[j]], [(j / n, 0), ((j + 1) / n, 0), ((j + 1) / n, 1), (j / n, 1)], W, ref=mean([mean(prev), mean(ring)]))
        prev = ring
    tiers = {0: [(0.04, 12, 12.0, 4.4), (0.15, 11, 11.0, 4.0), (0.27, 10, 9.0, 3.6), (0.4, 7, 7.0, 3.0)], 1: [(0.06, 8, 11.0, 4.4), (0.2, 7, 8.5, 3.8)], 2: [(0.1, 5, 9.0, 4.4)]}[lod]
    for ti, (t, cnt, ln, wd) in enumerate(tiers):
        y = y0 + H * t
        r = R(y)
        for k in range(cnt):
            a = 2 * math.pi * (k + 0.5 * (ti % 2)) / cnt
            o = (math.cos(a), 0.0, math.sin(a))
            tg = (-o[2], 0.0, o[0])
            root = (o[0] * r * 0.92, y, o[2] * r * 0.92)
            d = norm(add(mul(o, 0.72), (0, -0.62, 0)))
            tip = add(root, mul(d, ln * (0.85 + 0.3 * ((k * 7 + ti * 3) % 5) / 4)))
            card(m, 3, add(root, mul(tg, -wd * 0.5)), add(root, mul(tg, wd * 0.5)), add(tip, mul(tg, wd * 0.12)), add(tip, mul(tg, -wd * 0.12)), (0, 0, 1, 1), W)
    if lod < 2:
        helix_ribbon(m, lambda y: R0 + 2.4, y0 - 1, y0 + 9, 1.1, 2.4, 8 if lod == 0 else 5, 0.4, off=0.0)
        helix_ribbon(m, R, y0 + H * 0.5, y0 + H * 0.98, 1.3, 1.6, 7 if lod == 0 else 4, math.pi, off=0.8)
    trail(m, 0, 6 if lod == 0 else 4 if lod == 1 else 2)
    if lod < 2:
        trail(m, 1, 6 if lod == 0 else 4, sign=-1.0)
    tip_glow(m, L + 0.6, 1.5)
    return m


# ============================================================================================ B: Crystal Corkscrew
def lance_b(lod):
    m = Mesh("lance_B" + NM[lod])
    n = 6
    nr = {0: 18, 1: 10, 2: 6}[lod]
    turns = {0: 1.5, 1: 1.5, 2: 0.8}[lod]
    y0, L = 8.0, LEN["B"]
    H = L - y0
    R0 = 5.4
    handle(m, n, lod)

    def R(y):
        t = min(1.0, max(0.0, (y - y0) / H))
        return R0 * (1 - t) ** 0.8 + 0.25

    prev = None
    for i in range(nr + 1):
        t = i / nr
        y = y0 + H * t
        ring = ring_pts(y, R(y), n, twist=2 * math.pi * turns * t, lobes=(3, 0.45))
        if prev is not None:
            gap(m, GLASS, prev, ring, A)
        prev = ring
    # the glowing core: a thin cone inside the crystal (additive)
    cs = [ring_pts(y0 + H * t, max(0.1, 0.9 * (1 - t) ** 0.9), 4) for t in ((0.0, 0.5, 0.97) if lod == 0 else (0.0, 0.97))]
    for i in range(len(cs) - 1):
        gap(m, 1, cs[i], cs[i + 1], G)
    # frosted ring collars: one at the base, three along the shaft
    for t, wd in ((0.02, 0.05), (0.3, 0.03), (0.55, 0.03), (0.78, 0.025))[: 4 if lod == 0 else 2 if lod == 1 else 1]:
        y = y0 + H * t
        r = R(y) * 1.32
        a_, b_ = ring_pts(y - H * wd, r * 0.8, n), ring_pts(y, r, n)
        c_ = ring_pts(y + H * wd, r * 0.8, n)
        gap(m, FROST, a_, b_, W)
        gap(m, FROST, b_, c_, W)
    if lod < 2:
        helix_ribbon(m, R, y0 + 2, y0 + H * 0.95, 2.0, 2.0, 12 if lod == 0 else 7, 0.5, off=1.1)
        if lod == 0:
            helix_ribbon(m, R, y0 + H * 0.3, y0 + H * 0.98, 1.4, 1.6, 8, math.pi + 0.5, off=1.1)
    trail(m, 0, 6 if lod == 0 else 4 if lod == 1 else 2)
    if lod < 2:
        trail(m, 1, 6 if lod == 0 else 4, sign=-1.0)
    tip_glow(m, L + 0.6, 1.7)
    return m


# ============================================================================================ C: Living Water Dragon Lance
def lance_c(lod):
    m = Mesh("lance_C" + NM[lod])
    n = {0: 6, 1: 5, 2: 4}[lod]
    nseg = {0: 11, 1: 7, 2: 4}[lod]
    y0, L = 8.0, LEN["C"]
    H = L - y0
    handle(m, n, lod)

    def centre(t):
        rho = 5.2 * (1 - t) ** 1.1
        a = 2 * math.pi * 1.5 * t
        return (rho * math.cos(a), rho * math.sin(a), a)

    def rad(t):
        return 3.0 * (1 - t) ** 0.9 + 0.35

    drill_from = 0.72
    prev = None
    pcen = None
    for i in range(nseg + 1):
        t = drill_from * i / nseg
        cx, cz, a = centre(t)
        y = y0 + H * t
        ring = ring_pts(y, rad(t), n, twist=a, cx=cx, cz=cz)
        if prev is not None:
            gap(m, SCALE, prev, ring, W)
        # a fan fin on the back of the coil and two on the sides every other segment
        if i > 0 and lod < 2:
            o = norm((cx, 0.0, cz)) if (cx * cx + cz * cz) > 1e-6 else (1.0, 0.0, 0.0)
            tg = norm((-o[2], 0.0, o[0]))
            sz = 5.4 * (1 - t) ** 0.6 + 1.2
            base = (cx + o[0] * rad(t) * 0.8, y, cz + o[2] * rad(t) * 0.8)
            tip = add(base, add(mul(o, sz * 0.9), (0, -sz * 0.35, 0)))
            card(m, 3, add(base, (0, sz * 0.6, 0)), add(tip, (0, sz * 0.6, 0)), add(tip, (0, -sz * 0.6, 0)), add(base, (0, -sz * 0.6, 0)), (0, 0, 1, 1), W if i % 2 else P)
            if i % 2 == 0 and lod == 0:
                for sgn in (-1, 1):
                    b2 = (cx + tg[0] * rad(t) * 0.8 * sgn, y, cz + tg[2] * rad(t) * 0.8 * sgn)
                    t2 = add(b2, add(mul(tg, sgn * sz * 0.8), (0, -sz * 0.45, 0)))
                    card(m, 3, add(b2, (0, sz * 0.45, 0)), add(t2, (0, sz * 0.45, 0)), add(t2, (0, -sz * 0.45, 0)), add(b2, (0, -sz * 0.45, 0)), (0, 0, 1, 1), P)
        prev = ring
    # the drill point: the last part of the tail winds tight into a grooved cone
    nd = {0: 5, 1: 3, 2: 2}[lod]
    for i in range(1, nd + 1):
        t = drill_from + (1 - drill_from) * i / nd
        cx, cz, a = centre(t)
        y = y0 + H * t
        r = 0.2 if i == nd else rad(t)
        ring = ring_pts(y, r, n, twist=a * 2.2, cx=cx, cz=cz, groove=0.3)
        gap(m, FROST if i >= nd - 1 else DEEP, prev, ring, W)
        prev = ring
    # a foam collar where the tail turns into the drill
    cx, cz, a = centre(drill_from)
    yc = y0 + H * drill_from
    rc = rad(drill_from) * 1.3
    r1, r2 = ring_pts(yc - 1.0, rc * 0.8, n, cx=cx, cz=cz), ring_pts(yc, rc, n, cx=cx, cz=cz)
    r3 = ring_pts(yc + 1.0, rc * 0.8, n, cx=cx, cz=cz)
    gap(m, FROST, r1, r2, W)
    gap(m, FROST, r2, r3, W)

    def R(y):
        t = min(1.0, max(0.0, (y - y0) / H))
        return max(0.5, rad(t) + math.hypot(*centre(t)[:2]))

    if lod < 2:
        helix_ribbon(m, R, y0 + 4, y0 + H * 0.96, 1.6, 2.0, 10 if lod == 0 else 6, 0.3, off=1.0)
    trail(m, 0, 6 if lod == 0 else 4 if lod == 1 else 2)
    if lod < 2:
        trail(m, 1, 6 if lod == 0 else 4, sign=-1.0)
    tip_glow(m, L + 0.6, 1.5)
    return m


LANCES = {"A": ("Frosted Drill", lance_a), "B": ("Crystal Corkscrew", lance_b), "C": ("Living Water Dragon Lance", lance_c)}
LAYER_TEX = {"A": {3: "foam", 4: "veil", 5: "glow"}, "B": {4: "veil", 5: "glow"}, "C": {3: "fin", 4: "veil", 5: "glow"}}


def build_all():
    return {(k, lod): fn(lod) for k, (nm, fn) in LANCES.items() for lod in range(3)}


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(TEXD, exist_ok=True)
    Image.fromarray((np.clip(tex_fin(), 0, 1) * 255).astype(np.uint8), "RGBA").save(os.path.join(TEXD, "valk2_fin.png"))
    Image.fromarray((np.clip(tex_foam(), 0, 1) * 255).astype(np.uint8), "RGBA").save(os.path.join(TEXD, "valk2_foam.png"))
    ms = build_all()
    lines = ["Valkyrie drill lance, three candidate designs (preview only): quads per version and level; layers 0 plate, 1 emissive, 3 card, 4 veil, 5 glow, 6 frost, 7 deep, 8 scale, 9 glass", "",
             "%-26s %-5s %6s  %s" % ("version", "level", "quads", "by layer")]
    man = {"units": "px (1/16 block); weapon frame: +y axis, butt y=0, grip at 0.25 x length", "lances": {}}
    for k, (nm, fn) in LANCES.items():
        man["lances"][k] = {"name": nm, "length_px": LEN[k], "grip_px": LEN[k] * GRIP, "tex": {str(a): b for a, b in LAYER_TEX[k].items()}, "levels": {}}
        for lod in range(3):
            m = ms[(k, lod)]
            c = {}
            for qd in m.q:
                c[qd[0]] = c.get(qd[0], 0) + 1
            fn_ = "lance_%s%s.txt" % (k, NM[lod])
            with open(os.path.join(OUT, fn_), "w", encoding="utf-8", newline="\n") as f:
                f.write(m.text())
            lines.append("%-26s %-5s %6d  %s" % ("%s %s" % (k, nm), LV[lod], len(m.q), " ".join("L%d:%d" % (a, c[a]) for a in sorted(c))))
            man["lances"][k]["levels"][LV[lod]] = {"file": fn_, "quads": len(m.q), "vertices": len(m.q) * 4}
    with open(os.path.join(OUT, "counts_lance.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(BASE, "lances.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(man, f, indent=1)
    print("\n".join(lines))


if __name__ == "__main__":
    main()
