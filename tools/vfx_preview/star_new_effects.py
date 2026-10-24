"""
STAR MAGIC remake (phase 1), PREVIEW ONLY: the three looks (A anime cel, B volumetric glow, C real 3D toon meshes) as sets of PRIMITIVES (a placed star, a link line, a ground sigil,
a barrier pane, a beam / spear, a whip, a flash, a ring) and the seven effect timelines that are built from them:

    node         a placed star                          (canon: the stars of Star Magic; mod: the node entity)
    lines        constellation lines linking nodes      (canon: connecting stars forms a constellation, Star Magic overview)
    conjunction  teleport flash between stars           (canon: Star Magic: Conjunction, ch. 309 / 310)
    scutum       4-star barrier                         (canon: Quartile Scutum, ch. 309 / 310)
    hasta        spear / laser of light                 (canon: Quartile Hasta; the 4 stars concentrate light into one beam)
    flagellum    whip of light                          (canon: Quartile Flagellum, ch. 310)
    halation     beams from several stars at one point  (no canon found for the name; mod-original, see star_magic_design.md)

The timelines are the ones the design document proposes (ticks); the BEFORE tile of every row is the real current layer called the way the nearest existing page calls it.
A primitive takes (o: Out, ...) and puts quads (and, for C, mesh quads) into the collector; nothing is drawn here.
"""
import math

import numpy as np

from fire_new_out import tint, WHITE
from fire_new_raster import UP, V, norm, hsh, ease_out, ease_in, ease_back, clamp, lerp
import star_new_geo as G

PI = math.pi
BLUEC = (0.72, 0.88, 1.0, 1.0)
GOLDC = (1.0, 0.86, 0.46, 1.0)
HAND = V(0.3, 1.45, 0.5)


def smooth(t):
    t = clamp(t)
    return t * t * (3 - 2 * t)


def catmull(pts, per=4):
    """smooth path through pts (list of np arrays), `per` samples per span"""
    P = [pts[0]] + list(pts) + [pts[-1]]
    out = []
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for k in range(per):
            t = k / per
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(pts[-1])
    return out


# ======================================================================================================================= direction A  anime cel
class A:
    name = "A"

    @staticmethod
    def node(o, p, s, t, seed=0):
        if s <= 0.02:
            return
        st = int(t // 2)
        wob = 1.0 if st % 2 == 0 else 0.9
        o.bb(p, 3.0 * s, 3.0 * s, "B_glow_blue", col=tint(WHITE, 0.40 * min(1.0, s)), blend="ADD")
        o.bb(p, 1.55 * s * wob, 1.55 * s * wob, "A_star4", roll=0.0 if st % 2 == 0 else 0.26)
        for k in range(2):
            a = hsh(o.seed, seed + k, 3) * 6.28 + st * 0.9
            q = p + o.r * math.cos(a) * 0.95 * s + o.u * math.sin(a) * 0.95 * s
            o.bb(q, 0.34 * s, 0.34 * s, "A_spark", roll=a)

    @staticmethod
    def link(o, a, b, prog, w, fade, t):
        prog = clamp(prog)
        if prog <= 0:
            return
        e = a + (b - a) * prog
        o.strip(a, e, w, "A_line", col=tint(WHITE, fade))
        if prog < 1.0:
            o.bb(e, 0.55, 0.55, "A_spark", roll=t * 0.3, col=tint(WHITE, fade))

    @staticmethod
    def sigil(o, c, r, fade, t):
        if fade > 0.01:
            o.ground(c, r, "A_sigil", spin=int(t // 2) * 0.14, col=tint(WHITE, fade))

    @staticmethod
    def pane(o, c, ax, ay, hw, hh, alpha, t):
        if alpha > 0.01:
            o.plane(c, ax, ay, hw, hh, "A_pane", col=tint(WHITE, alpha))

    @staticmethod
    def beam(o, a, b, w, fade, t, kind="lance"):
        if np.linalg.norm(b - a) < 0.05:
            return
        o.strip(a, b, w, "A_beam", col=tint(WHITE, fade))
        o.bb(b, w * 1.9, w * 1.9, "A_spark", roll=int(t // 2) * 0.4, col=tint(WHITE, fade))

    @staticmethod
    def whip(o, pts, w, fade, t):
        n = len(pts) - 1
        for i in range(n):
            o.strip(pts[i], pts[i + 1], w, "A_whip", u=(i / n, (i + 1) / n), col=tint(WHITE, fade))

    @staticmethod
    def flash(o, p, s, fade, t):
        if fade <= 0.01:
            return
        st = int(t // 2)
        o.bb(p, 3.2 * s, 3.2 * s, "A_rays", roll=st * 0.35, col=tint(WHITE, fade))
        o.bb(p, 2.0 * s, 2.0 * s, "A_burst", roll=0.0 if st % 2 == 0 else 0.785, col=tint(WHITE, fade))
        o.bb(p, 0.8 * s, 0.8 * s, "A_disc", col=tint(WHITE, fade))

    @staticmethod
    def ring(o, c, ax, ay, r, fade, t):
        if fade > 0.01:
            o.plane(c, ax, ay, r, r, "A_ring", col=tint(WHITE, fade))

    @staticmethod
    def spark(o, p, s, fade, t):
        o.bb(p, 0.9 * s, 0.9 * s, "A_spark", roll=int(t // 2) * 0.5, col=tint(WHITE, fade))

    @staticmethod
    def shards(o, c, r, n, fade, t, seed=0):
        for i in range(n):
            a = hsh(o.seed, i + seed, 5) * 6.28
            rr = r * (0.4 + 0.6 * hsh(o.seed, i + seed, 6))
            q = c + o.r * math.cos(a) * rr + o.u * math.sin(a) * rr
            o.bb(q, 0.34, 0.34, "A_shard", roll=a, col=tint(WHITE, fade))


# ======================================================================================================================= direction B  volumetric glow
class B:
    name = "B"

    @staticmethod
    def node(o, p, s, t, seed=0):
        if s <= 0.02:
            return
        pulse = 0.85 + 0.15 * math.sin(t * 0.45 + seed)
        o.bb(p, 4.2 * s, 4.2 * s, "B_glow_blue", col=tint(WHITE, 0.55 * pulse), blend="ADD")
        o.bb(p, 2.8 * s, 2.8 * s, "B_star", roll=t * 0.012 + seed, col=tint(WHITE, min(1.0, s)), blend="ADD")
        o.bb(p, 4.4 * s, 4.4 * s, "B_flare", col=tint(WHITE, 0.42 * pulse), blend="ADD")
        for k in range(2):
            a = t * 0.05 + k * 3.1 + seed
            q = p + o.r * math.cos(a) * 0.5 * s + o.u * math.sin(a) * 0.35 * s
            o.bb(q, 2.0 * s, 2.0 * s, "B_dust_" + "abc"[(k + seed) % 3], roll=a, col=tint(BLUEC, 0.26 * pulse), blend="ADD")

    @staticmethod
    def link(o, a, b, prog, w, fade, t):
        prog = clamp(prog)
        if prog <= 0:
            return
        e = a + (b - a) * prog
        o.strip(a, e, w * 4.2, "B_line", col=tint(WHITE, fade), blend="ADD")
        ln = np.linalg.norm(e - a)
        for k in range(max(1, int(ln / 2.2))):
            q = a + (e - a) * ((k + 0.5) / max(1, int(ln / 2.2)))
            o.bb(q, 0.9, 0.9, "B_glow_blue", col=tint(WHITE, 0.30 * fade), blend="ADD")
        if prog < 1.0:
            o.bb(e, 1.6, 1.6, "B_star", roll=t * 0.1, col=tint(WHITE, fade), blend="ADD")

    @staticmethod
    def sigil(o, c, r, fade, t):
        if fade > 0.01:
            o.ground(c, r, "B_sigil", spin=t * 0.012, col=tint(WHITE, fade), blend="ADD")
            o.ground(c, r * 0.9, "B_dust_a", spin=t * 0.01, col=tint(BLUEC, 0.35 * fade), blend="ADD")

    @staticmethod
    def pane(o, c, ax, ay, hw, hh, alpha, t):
        if alpha > 0.01:
            o.plane(c, ax, ay, hw, hh, "B_pane", col=tint(WHITE, alpha), blend="ADD")
            o.plane(c + norm(np.cross(ax, ay)) * 0.02, ax, ay, hw * 0.9, hh * 0.9, "B_dust_b", col=tint(BLUEC, 0.28 * alpha), blend="ADD")

    @staticmethod
    def beam(o, a, b, w, fade, t, kind="lance"):
        if np.linalg.norm(b - a) < 0.05:
            return
        o.strip(a, b, w * 2.4, "B_beam", col=tint(WHITE, fade), blend="ADD")
        o.bb(b, w * 4.0, w * 4.0, "B_glow", col=tint(WHITE, 0.8 * fade), blend="ADD")
        o.bb(b, w * 6.5, w * 6.5, "B_flare", col=tint(WHITE, 0.55 * fade), blend="ADD")
        o.bb(a, w * 2.4, w * 2.4, "B_glow_blue", col=tint(WHITE, 0.6 * fade), blend="ADD")

    @staticmethod
    def whip(o, pts, w, fade, t):
        n = len(pts) - 1
        for i in range(n):
            f = 1 - i / n * 0.8
            o.strip(pts[i], pts[i + 1], w * 4.6 * f, "B_line", u=(0.2, 0.8), col=tint(WHITE, fade), blend="ADD")
        for i in range(0, n, 3):
            o.bb(pts[i], 0.9, 0.9, "B_glow_blue", col=tint(WHITE, 0.3 * fade), blend="ADD")
        o.bb(pts[-1], 2.0, 2.0, "B_star", roll=t * 0.2, col=tint(WHITE, fade), blend="ADD")

    @staticmethod
    def flash(o, p, s, fade, t):
        if fade <= 0.01:
            return
        o.bb(p, 3.4 * s, 3.4 * s, "B_glow", col=tint(WHITE, 0.9 * fade), blend="ADD")
        o.bb(p, 6.0 * s, 6.0 * s, "B_flare", col=tint(WHITE, 0.8 * fade), blend="ADD")
        o.bb(p, 4.4 * s, 4.4 * s, "B_rays", roll=t * 0.03, col=tint(WHITE, 0.7 * fade), blend="ADD")

    @staticmethod
    def ring(o, c, ax, ay, r, fade, t):
        if fade > 0.01:
            o.plane(c, ax, ay, r, r, "B_ring", col=tint(WHITE, fade), blend="ADD")

    @staticmethod
    def spark(o, p, s, fade, t):
        o.bb(p, 1.4 * s, 1.4 * s, "B_star", roll=t * 0.1, col=tint(WHITE, fade), blend="ADD")

    @staticmethod
    def shards(o, c, r, n, fade, t, seed=0):
        for i in range(n):
            a = hsh(o.seed, i + seed, 5) * 6.28
            rr = r * (0.4 + 0.6 * hsh(o.seed, i + seed, 6))
            q = c + o.r * math.cos(a) * rr + o.u * math.sin(a) * rr
            o.bb(q, 0.6, 0.6, "B_star", roll=a, col=tint(WHITE, fade), blend="ADD")


# ======================================================================================================================= direction C  real 3D toon meshes
class C:
    name = "C"

    @staticmethod
    def _add(o, quads, alpha=1.0, emissive=False):
        for pts, mat in quads:
            G.shade(o, pts, mat, alpha=alpha, emissive=emissive)

    @staticmethod
    def node(o, p, s, t, seed=0):
        if s <= 0.02:
            return
        sp = t * 0.11 + seed
        ex = V(math.cos(sp), 0, math.sin(sp))
        ey = norm(UP * 0.96 + V(0.12, 0, 0.12))
        ez = norm(np.cross(ex, ey))
        C._add(o, G.quads_star(p, ex, ey, ez, 0.92 * s, 0.20 * s, 0.30 * s))
        o.bb(p, 3.0 * s, 3.0 * s, "B_glow_blue", col=tint(WHITE, 0.36 * min(1.0, s)), blend="ADD")
        o.bb(p, 1.5 * s, 1.5 * s, "B_star", roll=t * 0.02, col=tint(WHITE, 0.55 * min(1.0, s)), blend="ADD")

    @staticmethod
    def link(o, a, b, prog, w, fade, t):
        prog = clamp(prog)
        if prog <= 0:
            return
        e = a + (b - a) * prog
        C._add(o, G.quads_tube(a, e, 0.028, 0.028, sides=4, mat="blue", rot=0.785), alpha=min(1.0, fade * 1.2), emissive=True)
        o.strip(a, e, w * 3.0, "B_line", col=tint(WHITE, 0.5 * fade), blend="ADD")
        ln = np.linalg.norm(e - a)
        if ln > 1.5:
            m = (a + e) / 2
            C._add(o, G.quads_bipyramid(m, e - a, 0.07, 0.22, sides=4, mat="gold", rot=0.4), alpha=min(1.0, fade * 1.2))
        if prog < 1.0:
            C._add(o, G.quads_bipyramid(e, e - a, 0.09, 0.25, sides=4, mat="white"))

    @staticmethod
    def sigil(o, c, r, fade, t):
        if fade <= 0.01:
            return
        C._add(o, G.quads_annulus(c, UP, r * 0.93, r, segs=10, mat="gold", phase=t * 0.01), alpha=min(1.0, fade * 1.3), emissive=True)
        C._add(o, G.quads_annulus(c, UP, r * 0.52, r * 0.55, segs=6, mat="blue", phase=-t * 0.01), alpha=min(1.0, fade * 1.3), emissive=True)
        for k in range(4):
            a = k * PI / 2 + PI / 4 + t * 0.01
            rd = V(math.cos(a), 0, math.sin(a))
            bc = c + rd * r * 0.96
            tg = V(-rd[2], 0, rd[0]) * 0.07 * r
            C._add(o, [([bc + UP * 0.34 * r * 0.4, bc + tg, bc - UP * 0.0 + rd * 0.0 + UP * 0.02, bc - tg], "white")], alpha=min(1.0, fade * 1.3), emissive=True)
        o.ground(c, r * 1.05, "B_sigil", spin=t * 0.012, col=tint(WHITE, 0.25 * fade), blend="ADD")

    @staticmethod
    def pane(o, c, ax, ay, hw, hh, alpha, t):
        if alpha > 0.01:
            C._add(o, G.quads_panel(c, ax, ay, hw, hh, nx=3, ny=2, mat="blue"), alpha=0.30 * alpha, emissive=True)
            o.plane(c, ax, ay, hw, hh, "B_pane", col=tint(WHITE, 0.45 * alpha), blend="ADD")

    @staticmethod
    def beam(o, a, b, w, fade, t, kind="lance"):
        L = np.linalg.norm(b - a)
        if L < 0.05:
            return
        if kind == "lance":
            hl = min(0.8, L * 0.3)
            C._add(o, G.quads_lance(a, b, 0.16 * w / 0.6, hl, fin=0.24 * w / 0.6), alpha=min(1.0, fade * 1.3))
            ax = norm(b - a)
            C._add(o, G.quads_annulus(a + ax * min(L * 0.5, 1.2), ax, 0.2, 0.30, segs=10, mat="gold"), alpha=min(1.0, fade * 1.3), emissive=True)
            o.strip(a - norm(b - a) * 1.5, b - norm(b - a) * 0.3, w * 2.0, "B_beam", col=tint(WHITE, 0.55 * fade), blend="ADD")
        else:
            C._add(o, G.quads_tube(a, b, 0.05 * w / 0.3, 0.06 * w / 0.3, sides=4, mat="white", rot=0.785), alpha=min(1.0, fade * 1.2), emissive=True)
            o.strip(a, b, w * 1.6, "B_beam", col=tint(WHITE, 0.6 * fade), blend="ADD")
        o.bb(b, w * 3.0, w * 3.0, "B_glow", col=tint(WHITE, 0.7 * fade), blend="ADD")

    @staticmethod
    def whip(o, pts, w, fade, t):
        pts = list(pts[::2]) + ([pts[-1]] if (len(pts) - 1) % 2 else [])
        n = len(pts) - 1
        for i in range(n):
            f0, f1 = 1 - i / n * 0.85, 1 - (i + 1) / n * 0.85
            C._add(o, G.quads_tube(pts[i], pts[i + 1], 0.07 * f0, 0.07 * f1, sides=4, mat="blue" if i % 2 == 0 else "white", rot=0.785), alpha=min(1.0, fade * 1.2), emissive=True)
        for i in range(0, n, 4):
            o.strip(pts[i], pts[min(n, i + 4)], w * 2.4, "B_line", u=(0.2, 0.8), col=tint(WHITE, 0.4 * fade), blend="ADD")
        C._add(o, G.quads_bipyramid(pts[-1], pts[-1] - pts[-2], 0.12, 0.34, sides=4, mat="gold"))

    @staticmethod
    def flash(o, p, s, fade, t):
        if fade <= 0.01:
            return
        C._add(o, G.quads_star(p, o.r, o.u, -o.f, 1.0 * s, 0.24 * s, 0.25 * s, spin=t * 0.18), alpha=min(1.0, fade * 1.4))
        o.bb(p, 3.4 * s, 3.4 * s, "B_glow", col=tint(WHITE, 0.6 * fade), blend="ADD")
        C._add(o, G.quads_annulus(p, o.f, 1.25 * s, 1.42 * s, segs=8, mat="gold"), alpha=min(1.0, fade * 1.3), emissive=True)

    @staticmethod
    def ring(o, c, ax, ay, r, fade, t):
        if fade > 0.01:
            nrm = norm(np.cross(ax, ay))
            C._add(o, G.quads_annulus(c, nrm, r * 0.92, r, segs=10, mat="gold"), alpha=min(1.0, fade * 1.3), emissive=True)

    @staticmethod
    def spark(o, p, s, fade, t):
        C._add(o, G.quads_bipyramid(p, o.u + o.r * 0.3, 0.10 * s, 0.34 * s, sides=3, mat="white", rot=t * 0.2), alpha=min(1.0, fade * 1.3))

    @staticmethod
    def shards(o, c, r, n, fade, t, seed=0):
        for i in range(n):
            a = hsh(o.seed, i + seed, 5) * 6.28
            rr = r * (0.4 + 0.6 * hsh(o.seed, i + seed, 6))
            q = c + o.r * math.cos(a) * rr + o.u * math.sin(a) * rr
            C._add(o, G.quads_bipyramid(q, V(math.cos(a), math.sin(a), 0.4), 0.06, 0.18, sides=3, mat="blue"), alpha=min(1.0, fade * 1.3))


DIRS = {"A": A, "B": B, "C": C}


# ======================================================================================================================= the seven timelines
def eff_node(d, o, sc):
    t, dur = o.age, o.dur
    S = sc["S"]
    gp = V(S[0], 0.04, S[2])
    fade = clamp((dur - t) / 8.0)
    if t < 5:                                                    # a comet flies from the hand to the point
        k = ease_out(t / 5.0)
        head = HAND + (S - HAND) * k
        tail = HAND + (S - HAND) * max(0.0, k - 0.35)
        d.beam(o, tail, head, 0.22, 1.0, t, kind="ray")
    else:
        s = ease_back((t - 5) / 6.0) * (1 + 0.05 * math.sin(t * 0.5))
        d.node(o, S, s * fade, t)
        if t < 12:
            d.flash(o, S, 0.9 * (1 - (t - 5) / 7.0), 1.0, t)
            d.ring(o, S, o.r, o.u, 0.8 + 1.6 * ease_out((t - 5) / 7.0), 1 - (t - 5) / 7.0, t)
        d.sigil(o, gp, 1.7, clamp((t - 5) / 8.0) * fade, t)
        d.link(o, S - UP * 0.5, gp + UP * 0.1, clamp((t - 6) / 6.0), 0.07, 0.55 * fade, t)


NODES4 = [V(-3.2, 3.2, 6.5), V(0.0, 4.5, 8.5), V(3.2, 3.2, 6.5), V(0.0, 2.2, 5.0)]


def eff_lines(d, o, sc):
    t, dur = o.age, o.dur
    fade = clamp((dur - t) / 6.0)
    N = NODES4
    for i, p in enumerate(N):
        d.node(o, p, ease_back((t - i * 1.4) / 4.0) * 0.8 * fade, t, seed=i)
    pairs = [(0, 1), (1, 2), (2, 3), (3, 0), (0, 2)]
    for i, (a, b) in enumerate(pairs):
        pr = clamp((t - (3 + i * 3.6)) / 4.0)
        d.link(o, N[a], N[b], pr, 0.09, fade, t)
    if t > 20:                                                   # the closed figure shimmers: a bright pulse runs along the lines
        k = ((t - 20) * 0.09) % 1.0
        for (a, b) in pairs[:4]:
            q = N[a] + (N[b] - N[a]) * k
            d.spark(o, q, 0.9, fade * 0.9, t)


def eff_conj(d, o, sc):
    t, dur = o.age, o.dur
    A0 = V(0, 1.0, 0)
    S, D = sc["S"], sc["D"]
    fade = clamp((dur - t) / 6.0)
    d.node(o, S, 0.9 * fade, t)
    d.link(o, S - UP * 0.5, D + UP * 0.1, 0.9, 0.07, 0.5 * fade, t)
    d.sigil(o, V(0, 0.04, 0), 1.2 * clamp(t / 4.0), clamp(1 - (t - 4) / 8.0), t)
    d.sigil(o, D + UP * 0.04, 1.6 * ease_out((t - 8) / 6.0), clamp((t - 8) / 3.0) * fade, t)
    if t < 5:
        d.flash(o, A0, 0.35 + 0.8 * ease_in(t / 5.0), 1.0, t)
    if 4 <= t <= 10:
        k = ease_out((t - 4) / 5.0)
        head = A0 + (D + UP - A0) * k
        tail = A0 + (D + UP - A0) * max(0.0, k - 0.3)
        d.beam(o, tail, head, 0.7, 1.0, t, kind="ray")
    if t >= 9:
        k = (t - 9) / 6.0
        d.flash(o, D + UP, 0.9 * (1 - k * 0.4), clamp(1 - k), t)
        d.ring(o, D + V(0, 0.08, 0), V(1, 0, 0), V(0, 0, 1), 0.5 + 1.8 * ease_out(k), clamp(1 - k), t)


def eff_scutum(d, o, sc):
    t, dur = o.age, o.dur
    fade = clamp((dur - t) / 6.0)
    Cn = sc["corners"]
    c = (Cn[0] + Cn[2]) / 2
    ax, ay = V(1, 0, 0), V(0, 1, 0)
    hw, hh = abs(Cn[1][0] - Cn[0][0]) / 2, abs(Cn[2][1] - Cn[1][1]) / 2
    for i, p in enumerate(Cn):
        k = ease_back((t - i * 0.9) / 4.0)
        if t < 4.5 + i * 0.9:
            q = HAND + (p - HAND) * ease_out((t - i * 0.9) / 4.5)
            if t > i * 0.9:
                d.beam(o, HAND + (q - HAND) * 0.7, q, 0.14, 1.0, t, kind="ray")
        d.node(o, p, k * 0.85 * fade, t, seed=i)
    for i in range(4):
        pr = clamp((t - (5 + i * 2.0)) / 3.0)
        d.link(o, Cn[i], Cn[(i + 1) % 4], pr, 0.10, fade, t)
    pane_a = smooth((t - 11) / 5.0)
    if t >= 22:                                                  # a blast (devil-powered magic in canon) hits the pane
        k = (t - 22) / 10.0
        pane_a *= 1.0 + 0.5 * math.sin(clamp(k) * PI) * 0.3
    d.pane(o, c, ax, ay, hw, hh, pane_a * fade, t)
    if 22 <= t <= 34:
        k = (t - 22) / 10.0
        imp = V(-0.6, 2.3, c[2])
        d.flash(o, imp, 0.9 * (1 - k), clamp(1 - k * 1.2), t)
        d.ring(o, imp, ax, ay, 0.3 + 1.1 * ease_out(k), clamp(1 - k), t)
        d.shards(o, imp, 1.6 * k + 0.2, 5, clamp(1 - k), t)


def eff_hasta(d, o, sc):
    t, dur = o.age, o.dur
    E = V(0, 1.6, 0)
    P = V(0, 1.6, 16.0)
    z0 = 3.0
    fade_st = clamp((17 - t) / 5.0)
    stars = [V(math.cos(k * PI / 2) * 1.5, 1.6 + math.sin(k * PI / 2) * 1.5, z0) for k in range(4)]
    for i, p in enumerate(stars):
        s = ease_back((t - i * 0.7) / 4.0) * 0.75 * fade_st
        d.node(o, p, s, t, seed=i)
    for i in range(4):
        d.link(o, stars[i], stars[(i + 1) % 4], clamp((t - (3.5 + i * 1.3)) / 3.0), 0.09, fade_st, t)
    cen = V(0, 1.6, z0)
    if 5 <= t < 10:
        d.flash(o, cen, 0.35 + 0.5 * smooth((t - 5) / 4.0), 1.0, t)               # light gathers in the middle
    if t >= 9.5:
        k = (t - 9.5) / 4.5
        head = cen + (P - cen) * clamp(ease_in(k))
        tail = cen + (P - cen) * clamp(ease_in(k) - 0.35)
        fade = clamp((dur - t) / 8.0)
        d.beam(o, tail, head, 0.40, fade, t, kind="lance")
        if k >= 0.95:
            kk = clamp((t - 14.0) / 8.0)
            d.flash(o, P, 0.9 * (1 - kk * 0.6), clamp(1 - kk), t)
            d.ring(o, P, V(1, 0, 0), V(0, 0, 1), 0.5 + 1.8 * ease_out(kk), clamp(1 - kk), t)


def eff_flagellum(d, o, sc):
    t, dur = o.age, o.dur
    fade = clamp((dur - t) / 6.0)
    dist = [2.5, 4.4, 6.1, 7.7]
    def theta(tt):
        return math.radians(-55 + 110 * smooth((tt - 4) / 10.0))
    stars = []
    for i, dd in enumerate(dist):
        a = theta(t - i * 1.4)
        stars.append(V(math.sin(a) * dd, 1.5 + 0.35 * math.sin(i * 1.3 + t * 0.2), math.cos(a) * dd))
        d.node(o, stars[-1], ease_back((t - i * 0.8) / 4.0) * 0.7 * fade, t, seed=i)
    pts = catmull([HAND] + stars, per=4)
    pr = clamp((t - 1.5) / 3.0)
    n = max(2, int(len(pts) * pr))
    d.whip(o, pts[:n], 0.2, fade, t)
    if 8 <= t <= 17:                                             # the tip passes the foes: cut flashes on them
        k = (t - 8) / 9.0
        for (fx, fz, when) in ((1.5, 6.0, 0.62), (-1.5, 7.0, 0.22)):
            kk = k - when
            if 0 <= kk <= 0.3:
                d.flash(o, V(fx, 1.1, fz), 0.8 * (1 - kk / 0.3), 1.0, t)


def eff_halation(d, o, sc):
    t, dur = o.age, o.dur
    E = sc["E"]
    fade = clamp((dur - t) / 8.0)
    n = 5
    stars = []
    for i in range(n):
        a = math.radians(-70 + 140 * i / (n - 1))
        stars.append(V(math.sin(a) * 3.3, 3.5 + 0.45 * math.cos(a * 2.0), 1.8 + math.cos(a) * 1.2))
    for i, p in enumerate(stars):
        d.node(o, p, ease_back((t - i * 0.6) / 4.0) * 0.7 * fade, t, seed=i)
    for i, p in enumerate(stars):
        t0 = 8 + i * 0.8
        if t >= t0:
            k = clamp((t - t0) / 2.0)
            head = p + (E - p) * k
            pulse = 0.85 + 0.15 * math.sin(t * 0.9 + i)
            d.beam(o, p, head, 0.30, clamp((dur - t) / 8.0) * pulse, t, kind="ray")
        elif t > 4:
            d.flash(o, p, 0.25 * smooth((t - 4) / 4.0), 0.9, t)
    if t >= 9.5:
        k = clamp((t - 9.5) / 4.0)
        fl = fade * (0.8 + 0.2 * math.sin(t * 0.8))
        d.flash(o, E, 0.55 + 0.6 * k, fl, t)
        d.ring(o, E, o.r, o.u, 0.6 + 1.3 * ease_out(clamp((t - 9.5) / 8.0)), clamp(1 - (t - 9.5) / 12.0) * fade, t)


FIG = lambda *a: list(a)

SCENES = [
    dict(key="node", title="Star node (a placed star)", canon="Star Magic overview: 'create and manipulate stars at will' (EN wiki, Star Magic)", before="star_node",
         build=eff_node, S=V(0, 3.2, 6.0), dur=100, ages=[4, 12, 24], mid=1, before_mid=1, F=V(0, 1, 0), T=V(0, 3.2, 6), p=1.0,
         cam=(-5, 2.6, 1.5), target=(0, 2.2, 5.5), figures=[[0, 0, 0, 1.8]]),
    dict(key="lines", title="Constellation lines (nodes linked)", canon="Star Magic overview: 'connecting stars forms a constellation, stronger attacks and defences' (EN wiki)", before="star_lines",
         build=eff_lines, dur=40, ages=[6, 14, 24], mid=1, before_mid=1, F=V(0, 3, 6), T=V(0, 3, 6), p=1.0,
         cam=(-6.5, 3.2, 0.5), target=(0.4, 2.9, 7), figures=[[0, 0, 0, 1.8]]),
    dict(key="conjunction", title="Conjunction (teleport between stars)", canon="Star Magic: Conjunction, ch. 309/310: teleport to the point beneath one of your stars (EN wiki; JA 合天)", before="star_conjunction",
         build=eff_conj, S=V(0, 4.5, 11), D=V(0, 0, 11), dur=24, ages=[4, 9, 14], mid=1, before_mid=1, F=V(0, 1, 0), T=V(0, 1, 11), p=1.0,
         cam=(-9.5, 3.6, 0.5), target=(0, 2, 6), figures=[[0, 0, 0, 1.8], [0, 0, 11, 1.8]]),
    dict(key="scutum", title="Quartile Scutum (4-star barrier)", canon="Star Magic: Quartile Scutum, ch. 309/310: four stars, beams between them form a rectangular barrier (EN wiki; JA 四天・盾)", before="star_scutum",
         build=eff_scutum, corners=[V(-2.2, 0.5, 4.2), V(2.2, 0.5, 4.2), V(2.2, 3.9, 4.2), V(-2.2, 3.9, 4.2)], dur=44, ages=[5, 13, 26], mid=2, before_mid=1, F=V(0, 1, 0), T=V(0, 1, 0), p=1.0,
         cam=(-5.5, 2.6, -1.5), target=(0, 2, 3.5), figures=[[0, 0, 0, 1.8]]),
    dict(key="hasta", title="Quartile Hasta (spear of light)", canon="Star Magic: Quartile Hasta (EN wiki nav, no page text; JA 四天・槍: light gathered in the middle of four stars, fired as a laser)", before="star_hasta",
         build=eff_hasta, dur=26, ages=[5, 10, 13], mid=2, before_mid=1, F=V(0, 1.6, 0), T=V(0, 1.6, 16), p=1.0,
         cam=(-5.5, 3.0, -3.5), target=(0, 1.8, 8), figures=[[0, 0, 0, 1.8], [0, 0, 16.5, 1.8]]),
    dict(key="flagellum", title="Quartile Flagellum (whip of light)", canon="Star Magic: Quartile Flagellum, ch. 310: starlight joins four stars into a thin sharp whip (EN wiki; JA 四天・鞭)", before="star_flagellum",
         build=eff_flagellum, dur=24, ages=[4, 9, 14], mid=1, before_mid=1, F=V(0, 1.6, 0), T=V(0, 1.6, 8), p=1.0,
         cam=(-4.5, 5.5, -2.8), target=(0.4, 1.0, 5), figures=[[0, 0, 0, 1.8], [1.5, 0, 6, 1.8], [-1.5, 0, 7, 1.8]]),
    dict(key="halation", title="Halation (beams from the stars)", canon="no canon spell of this name found (EN and JA search); mod-original beam finisher", before="star_halation",
         build=eff_halation, E=V(0, 1.2, 10), dur=28, ages=[5, 10, 15], mid=2, before_mid=1, F=V(0, 1.6, 0), T=V(0, 1.2, 10), p=1.0,
         cam=(-6, 4.2, -3.5), target=(0, 2.2, 6), figures=[[0, 0, 0, 1.8], [0, 0, 10, 1.8]]),
]
for sc in SCENES:
    sc["sky"] = [0.04, 0.06, 0.16]
    sc["horizon"] = [0.16, 0.17, 0.32]
    sc["floor"] = [0.10, 0.11, 0.17]
