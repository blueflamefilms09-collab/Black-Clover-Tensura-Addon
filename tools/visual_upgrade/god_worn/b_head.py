"""HEAD relics: the Eye of Vecna, the Basilica halo, the Godking crown, the Limitless blindfold, Odin's all-seeing eye, Ra's dawn crown.

Head frame (y up): origin at the neck, the head box is x -4..4, y 0..8, z -4..4, the face is -z, the hat layer reaches 4.5 / 8.5. Head pieces are drawn at full size (no 0.85).
"""
import _paths  # noqa: F401
import math

import numpy as np

import gen_worn_lib as L
import mesh_lib as ml
from mesh_lib import Part, loft, lathe, sweep, circle
from gw_lib import MAIN, TRIM, GLOW, DARK, ACCENT, EMIT, SHELL, HOT, LIT, ADD
import kit as K
from kit import A
from b_back import crest_disc, hanging_tablets, gat
from b_back2 import veins_overlay, shoulder_blades


def head_band(gs, D, y0, y1, a=4.75, b=4.75, t=0.55, tile="hammered", slot=MAIN, name="band", flare=0.0, n=28, taper=0.1):
    p = Part(name)
    L.band(p, y0, y1, a, b, t, D.dim(n, 10), taper, flare, 3.0)
    return gs.add(p, slot, tile, SHELL)


def eyeball(gs, D, c, r, slot=MAIN, iris_slot=GLOW, name="eye", facing=(0, 0, -1), group=0, iris_tile="iris", white_tile="pearl"):
    """A white sphere with an iris disc on the side that faces `facing`."""
    c = A(c, float)
    d, u, v = K.basis(facing, (0, 1, 0))
    sp = Part(name + "_ball")
    L.sph(sp, tuple(c), r, 4, D.sides(10, 6))
    gs.add(sp, slot, white_tile, SHELL, group, "fit")
    ir = Part(name + "_iris")
    K.disc(ir, c + d * r * 0.82, u, v, r * 0.62, r * 0.62, D.dim(14, 8), 0.12)
    gs.add(ir, iris_slot, iris_tile, EMIT | LIT | HOT, group, "fit")


def spire(gs, D, base, d, ln, r, tile="gilt", slot=TRIM, name="spire", gem_slot=GLOW, gem=True):
    p = Part(name)
    st = [(0.0, 1.0, 1.0), (ln * 0.12, 1.25, 1.25), (ln * 0.3, 0.85, 0.85), (ln * 0.82, 0.55, 0.55), (ln, 0.0, 0.0)]
    base = A(base, float)
    loft(p, A(d, float), base, st, circle(r, D.sides(6, 4)), caps=(True, True))
    gs.add(p, slot, tile, SHELL)
    if gem:
        K.gem(gs, D, tuple(base + K.unit(d) * ln * 0.2), r * 0.7, 0.9, 6, "y", slot=gem_slot, name=name + "_gem")


# ================================================================ 1. Eye of Vecna
def eye_of_vecna(b, D):
    gs = b.set("head", "head")
    # the red cord round the brow, knotted at the back with two long tails
    cord = Part("cord")
    L.torus(cord, (0, 6.2, 0), 4.85, 0.38, D.dim(28, 10), 6, 1.0, 1.0)
    gs.add(cord, ACCENT, "rope", 0, 0, "tile", 10.0)
    with gs.lvl(2):
        kn = Part("knot")
        L.sph(kn, (0, 6.2, 5.0), 0.9, 4, D.sides(8, 5))
        gs.add(kn, ACCENT, "rope")
        for k in range(2):
            g = gs.group("tail%d" % k, "wave", (0, 6.0, 5.2), 1.6, 0.1, 0.5 + 0.4 * k)
            pts = [(-0.6 + 1.2 * k + 0.3 * math.sin(i), 6.0 - 1.6 * i, 5.4 + 0.2 * i) for i in range(D.dim(7, 3))]
            K.ribbon(gs, D, pts, 1.2, ACCENT, "silk", SHELL, g, "tail%d" % k, side=(1, 0, 0))
        # the brass plate with its spiked frame and the eye
        plate = Part("plate")
        K.disc(plate, A((0, 6.3, -4.95)), A((1, 0, 0)), A((0, 1, 0)), 2.7, 2.2, D.dim(16, 8), 0.7)
        gs.add(plate, TRIM, "hammered", SHELL, 0, "fit")
        fr = Part("frame")
        K.link(fr, A((0, 6.3, -5.1)), A((1, 0, 0)), A((0, 1, 0)), 2.7, 2.2, 0.26, D.dim(20, 8), 4)
        gs.add(fr, TRIM, "gilt")
        sp = Part("frame_spikes")
        for k in range(D.cnt(10, 5)):
            a = 2 * math.pi * k / D.cnt(10, 5)
            base = A((2.75 * math.cos(a), 6.3 + 2.25 * math.sin(a), -5.1))
            K.spike(sp, base, base + A((1.3 * math.cos(a), 1.2 * math.sin(a), -0.5)), 0.32, 4)
        gs.add(sp, TRIM, "gilt")
        eyeball(gs, D, (0, 6.3, -5.35), 1.5, MAIN, GLOW, "vecna")
        for sgn in (-1, 1):                                                              # the lids
            lid = Part("lid%d" % sgn)
            K.beam(lid, A((-2.2, 6.3 + sgn * 1.15, -5.9)), A((2.2, 6.3 + sgn * 1.15, -5.9)), 0.9, 0.4)
            gs.add(lid, TRIM, "gilt")
        crest_disc(gs, D, (0, 9.4, -4.6), 1.3, "crest", normal=(0, 0.4, -1), name="browruby")
    with gs.lvl(3):
        for k in range(5):                                                               # Eyebite: five small eyes circle the head
            g = gs.group("eyelet%d" % k, "orbit", (0, 6.0, 0), 1.6 + 0.2 * k, 0.0, 0.0)
            a = 2 * math.pi * k / 5
            pos = (7.0 * math.cos(a), 6.0 + 1.5 * math.sin(k * 2.0), 7.0 * math.sin(a))
            eyeball(gs, D, pos, 0.8, MAIN, GLOW, "eyelet%d" % k, facing=(-math.cos(a), 0, -math.sin(a)), group=g)
        gt = gs.group("optic", "wave", (0, 6.5, -4.4), 2.0, 0.13, 0.5)
        for k in range(D.cnt(4, 2)):                                                      # the optic nerves: red cords streaming back from the plate
            a = (k - 1.5) * 0.5
            pts = [(1.5 * math.sin(a) * (1 + 0.3 * i), 6.4 + 0.5 * math.sin(i + k), -4.6 + 1.5 * i) for i in range(D.dim(8, 4))]
            tp = Part("nerve%d" % k)
            K.tube(tp, pts, 0.22, D.sides(5, 3), taper=lambda t: 1.0 - 0.6 * t)
            gs.add(tp, ACCENT, "vein", SHELL | LIT, gt, "tile")
        K.ring_halo(gs, D, (0, 11.5, 0), 6.0, 0.2, plane="xz", n=28)
        K.shards(gs, D, (0.0, 8.0, 0.0), 6.5, 6, 0.4, y_amp=1.5, kind="orbit", speed=-2.0, tile="spark", groups=1, name="gleam")
        K.filigree(gs, D, (0, 6.3, -5.2), (0, 0, -1), 2.2, turns=1.4, r=0.1, segs=22, arms=3, name="filg", plane_up=(0, 1, 0))
        pulse = gs.group("eyepulse", "pulse", (0, 6.3, -5.4), 0.05, 0.1, 0.0)
        pp = Part("pulse")
        K.link(pp, A((0, 6.3, -5.7)), A((1, 0, 0)), A((0, 1, 0)), 2.1, 1.7, 0.12, D.dim(20, 8), 4)
        gs.add(pp, GLOW, "ring_glow", EMIT | HOT | ADD, pulse, "fit")


# ================================================================ 2. Basilica Halo
def basilica_halo(b, D):
    gs = b.set("head", "head")
    head_band(gs, D, 7.0, 8.1, 4.7, 4.7, 0.4, "gilt", TRIM, "circlet")
    spin = gs.group("halo_spin", "spin", (0, 13.0, 0), 1.2, 0.0, 0.0)
    hp = Part("halo")
    L.torus(hp, (0, 13.0, 0), 6.0, 0.62, D.dim(32, 12), 6)
    gs.add(hp, TRIM, "hammered", SHELL, 0, "fit")
    hr = Part("halo_outer")
    L.torus(hr, (0, 13.0, 0), 7.6, 0.24, D.dim(32, 12), 4)
    gs.add(hr, TRIM, "gilt", 0, spin, "fit")
    rb = Part("runes")
    L.band(rb, 12.5, 13.5, 6.65, 6.65, 0.3, D.dim(32, 12), 0.0, 0.0, 2.0)
    gs.add(rb, GLOW, "rune_band", EMIT | LIT, spin, "fit")
    with gs.lvl(2):
        sp = Part("rays")
        for k in range(D.cnt(16, 6)):
            a = 2 * math.pi * k / D.cnt(16, 6)
            base = A((7.9 * math.cos(a), 13.0, 7.9 * math.sin(a)))
            K.spike(sp, base, base + A((3.0 * math.cos(a), 0.0, 3.0 * math.sin(a))) * (1.0 + 0.5 * (k % 2)), 0.5, 4)
        gs.add(sp, TRIM, "gilt", 0, spin)
        for k in range(D.cnt(8, 4)):                                                      # eight pillars from the halo to the circlet, a glass pane in each
            a = 2 * math.pi * k / D.cnt(8, 4) + 0.2
            top = A((5.6 * math.cos(a), 12.6, 5.6 * math.sin(a)))
            bot = A((4.6 * math.cos(a), 8.0, 4.6 * math.sin(a)))
            pil = Part("pillar%d" % k)
            K.tube(pil, [tuple(top + (bot - top) * (i / 4.0)) for i in range(5)], 0.28, D.sides(5, 3))
            gs.add(pil, TRIM, "gilt")
            pane = Part("pane%d" % k)
            K.disc(pane, (top + bot) / 2 + A((math.cos(a), 0, math.sin(a))) * 0.3, A((-math.sin(a), 0, math.cos(a))), A((0, 1, 0)), 0.9, 1.7, 6, 0.2)
            gs.add(pane, GLOW, "glass", EMIT | LIT | HOT, 0, "fit")
        crest_disc(gs, D, (0, 10.3, -5.2), 2.2, "crest", normal=(0, 0.25, -1), name="cross")
        K.gem(gs, D, (0, 7.6, -5.0), 0.8, 1.0, 6, "z", name="browgem")
    with gs.lvl(3):
        for sgn in (-1, 1):                                                               # small wings at the temples
            wg = gs.group("wing%d" % sgn, "sway", (sgn * 4.4, 6.5, 1.0), 5.0 * sgn, 0.08, sgn)
            K.feather_fan(gs, D, (sgn * 4.7, 6.5, 0.8), (sgn * 1.0, 0.45, 0.5), 7, 7.0, 1.0, 1.0, slot=ACCENT, tile="feather", group=wg, name="wing%d" % sgn, ranks=2, curl=0.1)
        K.shards(gs, D, (0.0, 13.0, 0.0), 9.0, 8, 0.45, y_amp=1.2, kind="orbit", speed=1.7, tile="spark", groups=2, name="lights")
        for k in range(D.cnt(6, 3)):                                                      # shafts of light standing up from the halo
            a = 2 * math.pi * k / D.cnt(6, 3)
            bm = Part("shaft%d" % k)
            K.beam(bm, A((6.0 * math.cos(a), 13.2, 6.0 * math.sin(a))), A((6.0 * math.cos(a), 24.0 + 3.0 * (k % 2), 6.0 * math.sin(a))), 0.8, 0.8)
            gs.add(bm, GLOW, "ember", EMIT | ADD | HOT, gs.group("shaft%d" % k, "pulse", (6.0 * math.cos(a), 18.0, 6.0 * math.sin(a)), 0.15, 0.06 + 0.01 * k, k), "fit")
        K.filigree(gs, D, (0, 7.6, 4.8), (0, 0, 1), 2.4, turns=1.5, r=0.1, segs=22, arms=3, name="backfil", plane_up=(0, 1, 0))


# ================================================================ 3. Godking Crown
def godking_crown(b, D):
    gs = b.set("head", "head")
    head_band(gs, D, 6.6, 9.0, 4.8, 4.8, 0.6, "hammered", MAIN, "band", flare=0.4, taper=0.2)
    rim = Part("rims")
    for yy in (6.62, 8.95):
        L.band(rim, yy - 0.14, yy + 0.14, 4.95, 4.95, 0.3, D.dim(28, 10), 0.0, 0.0, 3.0)
    gs.add(rim, TRIM, "gilt")
    # nine spires round the band, tall at the front and back
    n = D.cnt(9, 5)
    for k in range(n):
        a = 2 * math.pi * k / n
        ln = (9.5 if k % 3 == 0 else 6.5) if k else 11.5
        spire(gs, D, (5.0 * math.cos(a - math.pi / 2), 8.6, 5.0 * math.sin(a - math.pi / 2)), (0.2 * math.cos(a - math.pi / 2), 1.0, 0.2 * math.sin(a - math.pi / 2)), ln, 0.8, "gilt", TRIM, "spire%d" % k)
    with gs.lvl(2):
        crest_disc(gs, D, (0, 7.8, -5.3), 1.9, "crest", normal=(0, 0, -1), name="front")
        for sgn in (-1, 1):
            K.gem(gs, D, (sgn * 5.0, 7.8, 0.0), 1.0, 1.2, 6, "x", name="sidegem%d" % sgn)
        for k in range(4):                                                                # arches that bridge the spires over the crown
            a = k * math.pi / 4
            pts = K.bezier(A((5.0 * math.cos(a), 12.0, 5.0 * math.sin(a))), A((2.0 * math.cos(a), 18.0, 2.0 * math.sin(a))), A((-2.0 * math.cos(a), 18.0, -2.0 * math.sin(a))),
                           A((-5.0 * math.cos(a), 12.0, -5.0 * math.sin(a))), D.dim(10, 5))
            ar = Part("arch%d" % k)
            K.tube(ar, pts, 0.36, D.sides(5, 3))
            gs.add(ar, TRIM, "gilt")
        orb = Part("orb")
        L.sph(orb, (0, 18.8, 0), 1.3, 4, D.sides(10, 6))
        gs.add(orb, GLOW, "gem", EMIT | HOT, 0, "fit")
        cross = Part("orbcap")
        K.spike(cross, A((0, 19.8, 0)), A((0, 22.5, 0)), 0.5, 4)
        gs.add(cross, TRIM, "gilt")
    with gs.lvl(3):
        for sgn in (-1, 1):                                                               # wings of flight
            wg = gs.group("wing%d" % sgn, "sway", (sgn * 4.6, 8.0, 1.0), 6.0 * sgn, 0.07, sgn * 0.5)
            K.feather_fan(gs, D, (sgn * 4.8, 8.0, 1.0), (sgn * 1.0, 0.35, 0.45), 8, 10.0, 1.3, 1.15, slot=MAIN, tile="feather", group=wg, name="wing%d" % sgn, ranks=3, curl=0.1)
            K.filigree(gs, D, (sgn * 5.0, 7.8, -2.0), (sgn, 0, 0), 2.4, turns=1.6, r=0.12, segs=22, arms=3, name="sfil%d" % sgn, plane_up=(0, 1, 0))
        veins = Part("veins")
        L.band(veins, 6.8, 8.8, 4.95, 4.95, 0.2, D.dim(28, 10), 0.0, 0.0, 3.0)
        gs.add(veins, MAIN, "vein", LIT, 0, "tile", 5.0)
        K.shards(gs, D, (0.0, 11.0, 0.0), 8.0, 9, 0.5, y_amp=2.5, kind="orbit", speed=1.5, tile="gem2", groups=3, name="skills", flags=EMIT | HOT)
        sr = gs.group("runering", "spin", (0, 14.0, 0), -2.0, 0.0, 0.0)
        rr = Part("runering")
        L.band(rr, 13.4, 14.6, 6.6, 6.6, 0.3, D.dim(30, 10), 0.0, 0.0, 2.0)
        gs.add(rr, GLOW, "rune_band", EMIT | LIT, sr, "fit")
        K.ring_halo(gs, D, (0, 14.0, 0), 7.4, 0.18, plane="xz", n=30)


# ================================================================ 4. Limitless Blindfold
def limitless_blindfold(b, D):
    gs = b.set("head", "head")
    bf = Part("blindfold")
    L.band(bf, 3.0, 6.4, 4.65, 4.65, 0.45, D.dim(32, 12), 0.0, 0.0, 3.0)
    gs.add(bf, MAIN, "damask", SHELL, 0, "tile", 8.0)
    edge = Part("edge")
    for yy in (3.02, 6.38):
        L.band(edge, yy - 0.12, yy + 0.12, 4.78, 4.78, 0.2, D.dim(32, 12), 0.0, 0.0, 3.0)
    gs.add(edge, TRIM, "gilt")
    with gs.lvl(2):
        for k in range(6):                                                                # six eyes on the cloth: three each side of the centre line
            a = math.radians(-90 + (k - 2.5) * 22.0)
            c = A((4.9 * math.cos(a), 4.7 + (0.55 if k % 2 else -0.55), 4.9 * math.sin(a)))
            ep = Part("eye%d" % k)
            dd, u, v = K.basis((math.cos(a), 0, math.sin(a)), (0, 1, 0))
            K.disc(ep, c, u, v, 0.95, 0.62, D.dim(10, 6), 0.2)
            gs.add(ep, GLOW, "iris", EMIT | LIT | HOT, 0, "fit")
        kn = Part("knot")
        L.sph(kn, (0, 4.7, 5.2), 1.0, 4, D.sides(8, 5))
        gs.add(kn, MAIN, "damask", SHELL)
        for k in range(3):
            g = gs.group("tail%d" % k, "wave", (0, 4.6, 5.3), 2.2, 0.09, 0.5 * k)
            pts = [(-1.2 + 1.2 * k + 0.5 * math.sin(i * 0.8 + k), 4.4 - 2.4 * i, 5.4 + 0.15 * i + 0.3 * k) for i in range(D.dim(9, 4))]
            K.ribbon(gs, D, pts, 1.9, MAIN, "damask", SHELL, g, "tail%d" % k)
        crest_disc(gs, D, (0, 6.8, -5.0), 1.0, "crest", normal=(0, 0, -1), name="browsix", n=10)
    with gs.lvl(3):
        for k, (tilt, sp, R) in enumerate(((0.5, 3.0, 7.4), (-0.7, -2.2, 8.3), (1.1, 1.6, 9.2))):          # Infinity: tilted rings about the head
            g = gs.group("infinity%d" % k, "spin", (0, 5.0, 0), sp, 0.0, 0.0)
            rp = Part("ring%d" % k)
            L.torus(rp, (0, 5.0, 0), R, 0.13, D.dim(36, 12), 4)
            rp2 = rp.rotated("x", math.degrees(tilt), about=(0, 5.0, 0))
            gs.add(rp2, GLOW, "ring_glow", EMIT | HOT | ADD, g, "fit")
        for k in range(D.cnt(6, 3)):                                                      # six eyes circling at a distance
            g = gs.group("watcher%d" % k, "orbit", (0, 5.0, 0), 1.4, 0.0, 0.0)
            a = 2 * math.pi * k / D.cnt(6, 3)
            pos = (6.2 * math.cos(a), 5.0 + 1.8 * math.sin(k * 2.2), 6.2 * math.sin(a))
            eyeball(gs, D, pos, 0.75, MAIN, GLOW, "watcher%d" % k, facing=(-math.cos(a), 0, -math.sin(a)), group=g)
        for k, (R, sl) in enumerate(((9.0, TRIM), (9.0, ACCENT))):                          # blue and red: the two orbs of Hollow Purple, circling opposite each other
            g = gs.group("orb%d" % k, "orbit", (0, 7.0, 0), 3.0 * (1 if k == 0 else -1), 0.0, 0.0)
            ob = Part("orb%d" % k)
            L.sph(ob, (R * (1 if k == 0 else -1), 7.0, 0.0), 0.9, 4, D.sides(8, 5))
            gs.add(ob, sl, "soft", EMIT | HOT | ADD, g, "fit")
        K.shards(gs, D, (0.0, 5.0, 0.0), 5.8, 8, 0.3, y_amp=2.0, kind="orbit", speed=2.3, tile="spark", groups=2, name="six")
        veins_overlay(gs, D, lambda u, v: (4.8 * math.cos(2 * math.pi * u), 3.2 + 3.0 * v, 4.8 * math.sin(2 * math.pi * u)), 0, nu=24, nv=4, dz=0.0, tile="vein")


# ================================================================ 5. Odin's All-Seeing Eye
def raven(gs, D, c, facing, size, name, group=0):
    """A raven perched on c, looking along `facing` (a lathe body, a head with a beak, folded wings of feathers, a glowing eye)."""
    c, fd = A(c, float), K.unit(facing)
    d, u, v = K.basis(fd, (0, 1, 0))
    body = Part(name + "_body")
    loft(body, d, c - d * size * 0.9, [(0.0, 0.5, 0.5), (size * 0.8, 1.0, 0.95), (size * 1.5, 0.8, 0.7), (size * 1.9, 0.0, 0.0)], ml.ellipse(size * 0.6, size * 0.5, D.sides(8, 5)), caps=(True, True), up=(0, 1, 0))
    gs.add(body, DARK, "feathery", SHELL, group, "tile", 6.0)
    hd = Part(name + "_head")
    L.sph(hd, tuple(c + d * size * 1.0 + v * size * 0.55), size * 0.42, 3, D.sides(8, 5))
    gs.add(hd, DARK, "feathery", SHELL, group, "tile", 6.0)
    bk = Part(name + "_beak")
    K.spike(bk, c + d * size * 1.3 + v * size * 0.5, c + d * size * 2.0 + v * size * 0.35, size * 0.2, 4)
    gs.add(bk, TRIM, "horn", 0, group)
    e = Part(name + "_eye")
    K.dia(e, c + d * size * 1.2 + v * size * 0.68 + u * size * 0.28, size * 0.07, size * 0.07, size * 0.07)
    e2 = Part(name + "_eye2")
    K.dia(e2, c + d * size * 1.2 + v * size * 0.68 - u * size * 0.28, size * 0.07, size * 0.07, size * 0.07)
    gs.add(e, GLOW, "spark", EMIT | HOT, group, "fit")
    gs.add(e2, GLOW, "spark", EMIT | HOT, group, "fit")
    wp = Part(name + "_wings")
    for sgn in (-1, 1):
        for k in range(D.cnt(5, 2)):
            t = k / 4.0
            base = c - d * size * 0.2 + u * sgn * size * 0.45 + v * size * 0.2
            L.feather(wp, tuple(base), tuple(base - d * size * (1.6 - 0.2 * t) + u * sgn * size * (0.5 + 0.25 * t) - v * size * 0.2), size * 0.32, 0.2, 3)
    gs.add(wp, DARK, "feather", SHELL, group, "fit")


def odins_all_seeing_eye(b, D):
    gs = b.set("head", "head")
    head_band(gs, D, 6.0, 8.2, 4.8, 4.8, 0.55, "hammered", TRIM, "band", flare=0.3)
    rb = Part("runes")
    L.band(rb, 6.6, 7.3, 5.4, 5.4, 0.2, D.dim(28, 10), 0.0, 0.0, 3.0)
    gs.add(rb, GLOW, "rune_band", EMIT | LIT, 0, "fit")
    with gs.lvl(2):
        # the eye in its socket at the brow: lids, a ring of gold, the pale iris
        sock = Part("socket")
        K.link(sock, A((0, 7.0, -5.2)), A((1, 0, 0)), A((0, 1, 0)), 2.2, 1.9, 0.34, D.dim(22, 8), 4)
        gs.add(sock, TRIM, "gilt")
        eyeball(gs, D, (0, 7.0, -5.3), 1.8, MAIN, GLOW, "allseeing", iris_tile="iris")
        for sgn in (-1, 1):
            lid = Part("lid%d" % sgn)
            pts = K.bezier(A((-2.4, 7.0, -5.4)), A((-1.2, 7.0 + sgn * 2.2, -5.9)), A((1.2, 7.0 + sgn * 2.2, -5.9)), A((2.4, 7.0, -5.4)), D.dim(8, 4))
            K.tube(lid, pts, 0.34, D.sides(5, 3))
            gs.add(lid, TRIM, "gilt")
        for sgn in (-1, 1):
            raven(gs, D, (sgn * 4.9, 9.0, -0.5), (sgn * 0.2, 0.0, -1.0), 1.5, "raven%d" % sgn)
        crest_disc(gs, D, (0, 10.6, -4.2), 1.3, "crest", normal=(0, 0.5, -1), name="ravenseal")
    with gs.lvl(3):
        for sgn in (-1, 1):
            wg = gs.group("wing%d" % sgn, "sway", (sgn * 4.6, 7.5, 1.5), 4.0 * sgn, 0.07, sgn)
            K.feather_fan(gs, D, (sgn * 4.7, 7.5, 1.2), (sgn * 1.0, 0.2, 0.8), 8, 9.0, 1.1, 1.1, slot=DARK, tile="feather", group=wg, name="rwing%d" % sgn, ranks=2)
            K.filigree(gs, D, (sgn * 5.0, 7.0, -1.5), (sgn, 0, 0), 2.2, turns=1.6, r=0.11, segs=22, arms=3, name="filg%d" % sgn, plane_up=(0, 1, 0))
        sp = gs.group("gungnir", "bob", (0, 13.0, 0), 0.8, 0.06, 0.0)
        sh = Part("spear")
        K.beam(sh, A((0, 12.0, 3.6)), A((0, 12.0, -6.4)), 0.38, 0.38)
        K.spike(sh, A((0, 12.0, -6.4)), A((0, 12.0, -9.6)), 0.7, 4)
        gs.add(sh, TRIM, "gilt", SHELL, sp)
        hanging_tablets(gs, D, (0, 7.0, 0), 8.0, 8, 0.8, "rune_a", speed=1.6, y_span=4.0)
        K.ring_halo(gs, D, (0, 10.4, 0), 5.8, 0.16, plane="xz", n=28)
        K.shards(gs, D, (0.0, 7.0, 0.0), 6.6, 6, 0.4, y_amp=1.5, kind="orbit", speed=-2.0, tile="spark", groups=1, name="sight")


# ================================================================ 6. Ra's Dawn Crown
def ras_dawn_crown(b, D):
    gs = b.set("head", "head")
    head_band(gs, D, 6.4, 8.6, 4.8, 4.8, 0.55, "hammered", MAIN, "band", flare=0.3)
    rim = Part("rims")
    for yy in (6.42, 8.58):
        L.band(rim, yy - 0.14, yy + 0.14, 4.95, 4.95, 0.3, D.dim(28, 10), 0.0, 0.0, 3.0)
    gs.add(rim, TRIM, "gilt")
    # the sun-rays crown: tall flat rays round the band
    rays = Part("rays")
    n = D.cnt(15, 7)
    for k in range(n):
        a = 2 * math.pi * k / n
        base = A((5.0 * math.cos(a), 8.4, 5.0 * math.sin(a)))
        K.spike(rays, base, base + A((0.9 * math.cos(a), 5.0 + 2.6 * (k % 2), 0.9 * math.sin(a))), 0.6, 4)
    gs.add(rays, TRIM, "gilt", SHELL)
    with gs.lvl(2):
        crest_disc(gs, D, (0, 7.6, -5.3), 2.0, "crest", normal=(0, 0, -1), name="sunankh")
        # the uraeus: a cobra rising at the brow with a spread hood
        cobra = Part("cobra")
        pts = K.bezier(A((0, 8.2, -4.9)), A((0, 10.0, -5.8)), A((0, 11.5, -5.0)), A((0, 12.4, -5.6)), D.dim(9, 4))
        K.tube(cobra, pts, 0.7, D.sides(7, 4), taper=lambda t: 1.0 - 0.45 * t)
        gs.add(cobra, ACCENT, "scale", SHELL)
        hood = Part("hood")
        lathe(hood, "z", (0, 11.2, -4.9), [(0.0, 0.2), (-0.6, 1.5), (-1.0, 1.5), (-1.4, 0.3)], sides=D.sides(12, 6), squash=1.5)
        gs.add(hood, ACCENT, "scale", SHELL)
        for sgn in (-1, 1):
            e = Part("cobra_eye%d" % sgn)
            K.dia(e, A((sgn * 0.3, 12.3, -6.0)), 0.12, 0.12, 0.12)
            gs.add(e, GLOW, "spark", EMIT | HOT, 0, "fit")
        for sgn in (-1, 1):
            K.gem(gs, D, (sgn * 4.9, 7.5, -1.8), 0.9, 1.1, 6, "x", name="sgem%d" % sgn)
    # the sun behind the head: a disc of dawn ringed with long rays that rock gently
    sun = gs.group("sun", "pulse", (0, 11.0, 5.8), 0.05, 0.06, 0.0)
    sd = Part("sun")
    K.disc(sd, A((0, 11.0, 5.8)), A((1, 0, 0)), A((0, 1, 0)), 5.6, 5.6, D.dim(24, 10), 0.6)
    gs.add(sd, GLOW, "sun", EMIT | ADD | HOT | LIT, sun, "fit")
    with gs.lvl(2):
        sd2 = Part("sunplate")
        K.disc(sd2, A((0, 11.0, 6.3)), A((1, 0, 0)), A((0, 1, 0)), 3.6, 3.6, D.dim(24, 10), 0.5)
        gs.add(sd2, TRIM, "gilt", SHELL, 0, "fit")
        sw = gs.group("raysway", "sway", (0, 11.0, 5.8), 2.0, 0.05, 0.0)
        rr = Part("sunrays")
        for k in range(D.cnt(24, 8)):
            a = 2 * math.pi * k / D.cnt(24, 8)
            base = A((6.0 * math.cos(a), 11.0 + 6.0 * math.sin(a), 5.8))
            K.spike(rr, base, base + A((3.4 * math.cos(a), 3.4 * math.sin(a), 0.0)) * (1.0 + 0.6 * (k % 2)), 0.5, 4)
        gs.add(rr, TRIM, "gilt", 0, sw)
    with gs.lvl(3):
        for sgn in (-1, 1):
            wg = gs.group("wing%d" % sgn, "sway", (sgn * 4.6, 8.0, 1.0), 5.0 * sgn, 0.07, sgn)
            K.feather_fan(gs, D, (sgn * 4.8, 8.0, 0.6), (sgn * 1.0, 0.3, 0.6), 8, 9.0, 1.1, 1.0, slot=TRIM, tile="feather", group=wg, name="wing%d" % sgn, ranks=2)
            K.filigree(gs, D, (sgn * 5.0, 7.5, -2.0), (sgn, 0, 0), 2.3, turns=1.5, r=0.11, segs=22, arms=3, name="sfil%d" % sgn, plane_up=(0, 1, 0))
        for k in range(D.cnt(9, 4)):                                                      # dawn shafts fanning up behind the sun
            a = math.radians(-70 + 140 * k / max(1, D.cnt(9, 4) - 1))
            bm = Part("shaft%d" % k)
            K.beam(bm, A((9.0 * math.sin(a), 11.0 + 9.0 * math.cos(a), 5.8)), A((16.0 * math.sin(a), 11.0 + 16.0 * math.cos(a), 5.8)), 0.9, 0.5)
            gs.add(bm, GLOW, "ember", EMIT | ADD | HOT, gs.group("shaft%d" % k, "pulse", (12.0 * math.sin(a), 11.0 + 12.0 * math.cos(a), 5.8), 0.2, 0.05 + 0.008 * k, k), "fit")
        K.shards(gs, D, (0.0, 9.0, 0.0), 7.5, 8, 0.5, y_amp=2.0, kind="orbit", speed=1.8, tile="sun", groups=2, name="suns", flags=EMIT | HOT | ADD)
        K.ring_halo(gs, D, (0, 12.0, 0), 6.6, 0.2, plane="xz", n=28)


BUILDERS = {"eye_of_vecna": eye_of_vecna, "basilica_halo": basilica_halo, "godking_crown": godking_crown, "limitless_blindfold": limitless_blindfold, "odins_all_seeing_eye": odins_all_seeing_eye,
            "ras_dawn_crown": ras_dawn_crown}
