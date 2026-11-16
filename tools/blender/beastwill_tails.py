"""Beast Will tails and tentacles, MADE IN BLENDER (docs/beastwill_tails.md).

The cube tails and tentacles of the Beast Will forms (tools/beastwill/remake) are replaced, behind the client setting beastwill.meshTails (default on), by real
modelled meshes: tapering, scaled, finned, suckered, curling. Nine forms:

    kraken (12 tentacles)   scylla_charybdis (6 waist tentacles)   kitsune (9 fox tails)   apophis / typhon (two serpent tails each, Typhon's end in snake heads)
    dragon   nidhoggr   jormungandr   serpent (flame serpent): one scaled tail each

Every tail or tentacle is a CHAIN of parts named sw<chain>_<segment> (a tube cut at its joints; every part runs one ring into the next part, shrunk, so a bend leaves no gap).
The game turns each part about its joint by a gentle procedural sway (client/aura/BeastWillTails.java, pure rules in beastwill/BeastTailRules.java, mirrored here in sway()).
The mesh frame is the one of devil_forms: 1 unit = 1/16 block, y up, face +z, feet at y = 0, the form's left = +x; a tail runs toward -z (the back). It is anchored on the form's
body at the same place as the cube tail it replaces: (0, 13, -2.6).

Run (Blender 5.x as a Python module, docs/heroweapon/blender_pipeline.md):
    python3 -B tools/blender/beastwill_tails.py [--only dragon,kraken] [--trial DIR] [--quick]
Writes entity_meshes/bwtail_<stem>.txt (+ _lod), textures/entity/boss_mesh/bwtail_<stem>(.png, _glow.png) and docs/beastwill_tails/previews/<stem>*.png.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Matrix, Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "beastwill_tails", "previews")
V = Vector
ROOT_PT = V((0.0, 13.0, -2.6))          # where the cube tail of every form is rooted (its geo pivot (0, 13, 2.6), z turned into the mesh frame)
BUDGET = 1500                            # quad lines per form, body + glow (client cost cap)


def _no_self_shadow(orig=bl._join):
    """The baked skin must not be shaded by the part next to it: the shrunk end of a segment sits inside the next one and would bake black (and show black when the joint bends)."""
    def join(obs, name):
        j = orig(obs, name)
        j.visible_shadow = False
        return j
    return join


bl._join = _no_self_shadow()


# ================================================================ the sway the game plays (the same numbers as beastwill/BeastTailRules.angles)
KINDS = {"tail": (0.055, 0.9, 0.55, 0.4), "snake": (0.09, 1.4, 0.75, 0.35), "tentacle": (0.10, 0.9, 0.6, 0.9), "fluff": (0.05, 0.8, 0.5, 0.6)}


def sway(kind, chain, seg, seconds):
    amp, w, lag, cross = KINDS[kind]
    grow = 0.45 + 0.1 * seg
    ph = w * seconds * 6.2831855 - seg * lag + chain * 1.3
    return (amp * cross * grow * math.sin(ph * 0.8 + 1.7), amp * grow * math.sin(ph), 0.0)


# ================================================================ materials
def C(c):
    if isinstance(c, str):
        c = (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))
    return bl.rgb(*c)


def M(name, col, rough=0.55, metal=0.0, bump=None, bscale=1.0, bstr=0.3, bevel=0.1, groove=None, gamount=0.6):
    return bl.mat(name, C(col), metallic=metal, rough=rough, bump=bump, bscale=bscale, bstr=bstr, bevel=bevel, groove=C(groove) if groove else None, gamount=gamount)


def E(name, col, strength=7.0):
    return bl.mat(name, C(col), rough=0.3, emit=C(col), strength=strength * 0.17, bevel=0.0)


# ================================================================ geometry
def resample(pts, n, sub=10):
    """n points evenly spaced along the smoothed curve through pts."""
    S = bl.smooth_path(pts, sub)
    L = [0.0]
    for a, b in zip(S, S[1:]):
        L.append(L[-1] + (b - a).length)
    out, j = [], 0
    for i in range(n):
        d = L[-1] * i / (n - 1)
        while j < len(L) - 2 and L[j + 1] < d:
            j += 1
        f = (d - L[j]) / max(L[j + 1] - L[j], 1e-9)
        out.append(S[j].lerp(S[j + 1], f))
    return out


def curl_path(start, direction, length, n, k0, k1, lat=0.0, droop=0.0):
    """A curling line: n steps; k0 + k1 t^2 radians per unit length of upward bend (the tip curls most), lat turns it sideways, droop pulls it down."""
    d = V(direction).normalized()
    pts = [V(start)]
    ds = length / n
    for i in range(n):
        t = (i + 0.5) / n
        ax = d.cross(V((0, 1, 0)))
        if ax.length < 1e-4:
            ax = V((1, 0, 0))
        ax.normalize()
        d = Matrix.Rotation((k0 + k1 * t * t) * ds, 3, ax) @ d
        if lat:
            d = Matrix.Rotation(lat * ds * (0.4 + t), 3, V((0, 1, 0))) @ d
        if droop:
            d = (d + V((0, -droop * ds, 0))).normalized()
        pts.append(pts[-1] + d * ds)
    return pts


def frames(P, ref=(0, 1, 0)):
    n = len(P)
    T = [(P[min(i + 1, n - 1)] - P[max(i - 1, 0)]).normalized() for i in range(n)]
    N = []
    r = V(ref)
    for i in range(n):
        base = N[-1] if N else r
        v = base - T[i] * base.dot(T[i])
        if v.length < 1e-4:
            v = V((1, 0, 0)) - T[i] * T[i].x
        N.append(v.normalized())
    B = [T[i].cross(N[i]).normalized() for i in range(n)]
    return T, N, B


def oriented(ob, origin, zdir, xhint=(0, 1, 0)):
    """Stands an object built round the z axis at `origin`, its z along zdir."""
    z = V(zdir).normalized()
    x = V(xhint) - z * V(xhint).dot(z)
    if x.length < 1e-4:
        x = V((1, 0, 0)) - z * z.x
    x.normalize()
    y = z.cross(x)
    o = V(origin)
    ob.data.transform(Matrix(((x.x, y.x, z.x, o.x), (x.y, y.y, z.y, o.y), (x.z, y.z, z.z, o.z), (0, 0, 0, 1))))
    ob.data.update()
    bl.outward(ob)
    return ob


def flat(ob, origin, along, wide):
    """Stands a plate built in the xy plane: its x along `along`, its y toward `wide`, its thickness across both."""
    x = V(along).normalized()
    y = V(wide) - x * V(wide).dot(x)
    y.normalize()
    z = x.cross(y)
    o = V(origin)
    ob.data.transform(Matrix(((x.x, y.x, z.x, o.x), (x.y, y.y, z.y, o.y), (x.z, y.z, z.z, o.z), (0, 0, 0, 1))))
    ob.data.update()
    bl.outward(ob)
    return ob


class Chain:
    """A tail or tentacle: nseg parts named sw<ci>_<k>, rings P[0..nseg*step]; radius(t) 0..1 along it; wide / high stretch the section."""

    def __init__(self, ci, pts, nseg, radius, sides, mats, step=1, wide=1.0, high=1.0, tip=0.3, ref=(0, 1, 0), cap=True):
        self.ci, self.nseg, self.step, self.sides, self.radius, self.wide, self.high = ci, nseg, step, sides, radius, wide, high
        self.mats = mats if callable(mats) else (lambda k, m=mats: m)
        self.P = resample(pts, nseg * step + 1)
        self.T, self.N, self.B = frames(self.P, ref)
        self.parts, self.pivots, self.tipc = [], {}, tip
        n = len(self.P)
        for k in range(nseg):
            a, b = k * step, (k + 1) * step
            rings = [self.ring(i) for i in range(a, b + 1)]
            if k > 0:                                                    # a collar: this part reaches back over the end of the one before it, a little wider, so a bend opens no gap
                d = (self.P[a] - self.P[a - 1]).length * 0.4
                rings[0] = self.ring(a, 1.07)
                back = self.P[a] - self.T[a] * d
                rings.insert(0, [back + (q - self.P[a]) for q in self.ring(a, 1.07)])
            if k == nseg - 1 and tip > 0:
                c = self.P[-1]
                rings.append([c + (p - c) * tip + self.T[-1] * self.rad(1.0) * 0.5 for p in self.ring(n - 1)])
            name = self.name(k)
            bl.loft(name, rings, self.mats(k), (k == 0, k == nseg - 1 and cap), part=name)
            self.parts.append(name)
            self.pivots[name] = self.P[a]

    def name(self, k):
        return f"sw{self.ci}_{k}"

    def rad(self, t):
        return max(self.radius(t), 0.05)

    def ring(self, i, scale=1.0):
        t = i / (len(self.P) - 1)
        r = self.rad(t) * scale
        return [self.P[i] + self.N[i] * (math.cos(2 * math.pi * s / self.sides) * r * self.high) + self.B[i] * (math.sin(2 * math.pi * s / self.sides) * r * self.wide) for s in range(self.sides)]

    def seg_of(self, i):
        return min(i // self.step, self.nseg - 1)

    def dirn(self, i, ang):
        """Outward unit direction of the surface at ring i, angle ang (0 = the side the section's N points to, pi/2 = toward B)."""
        return (self.N[i] * math.cos(ang) + self.B[i] * math.sin(ang)).normalized()

    def surf(self, i, ang, grow=1.0):
        t = i / (len(self.P) - 1)
        r = self.rad(t)
        return self.P[i] + self.N[i] * (math.cos(ang) * r * self.high * grow) + self.B[i] * (math.sin(ang) * r * self.wide * grow)

    def best_angle(self, i, target):
        tg = V(target).normalized()
        return max((a * math.pi / 6 for a in range(12)), key=lambda a: self.dirn(i, a).dot(tg))

    def spike(self, name, i, ang, h, w, mat, sides=4, lean=0.0, sink=0.35):
        """A cone standing on the surface at ring i, leaning along the tail by `lean` (fraction of its length)."""
        d = self.dirn(i, ang)
        ob = bl.lathe(name, [(w, 0.0), (w * 0.45, h * 0.55), (0.02, h)], mat, sides=sides, axis="z", caps=(False, False), part=self.name(self.seg_of(i)))
        oriented(ob, self.surf(i, ang) - d * sink, (d + self.T[i] * (-lean)).normalized(), tuple(self.T[i]))
        return ob

    def cup(self, name, i, ang, r, mat, mat_rim=None, sides=6):
        """A sucker: a low ring wall with a dimple, facing outward."""
        d = self.dirn(i, ang)
        ob = bl.lathe(name, [(r, -0.25), (r * 1.02, 0.35), (r * 0.62, 0.55), (r * 0.5, 0.15), (0.03, 0.0)], mat, sides=sides, axis="z", caps=(False, False), part=self.name(self.seg_of(i)))
        oriented(ob, self.surf(i, ang) - d * 0.05, d, tuple(self.T[i]))
        return ob

    def plate(self, name, i, ang, outline, thick, mat, flat_ang=None):
        """A flat fin standing on the surface at ring i: outline [(along the tail, away from it)] in the plane of the tail and the surface direction."""
        d = self.dirn(i, ang)
        ob = bl.plate(name, outline, -thick / 2, thick / 2, mat, part=self.name(self.seg_of(i)))
        return flat(ob, self.surf(i, ang) - d * 0.3, self.T[i], d)

    def rig(self, rig):
        for k, n in enumerate(self.parts):
            rig[n] = (self.parts[k - 1] if k else None, tuple(self.pivots[n]))


def extra_tube(name, part, pts, radfn, sides, mat, wide=1.0, high=1.0, ref=(0, 1, 0), tip=0.3):
    """A rigid piece (a snake head, a flame) that rides on one part."""
    P = resample(pts, 8)
    T, N, B = frames(P, ref)
    rings = []
    for i, p in enumerate(P):
        r = max(radfn(i / (len(P) - 1)), 0.05)
        rings.append([p + N[i] * (math.cos(2 * math.pi * s / sides) * r * high) + B[i] * (math.sin(2 * math.pi * s / sides) * r * wide) for s in range(sides)])
    return bl.loft(name, rings, mat, (True, True), part=part), P, T, N, B


def orb(name, part, c, r, mat, sides=6):
    return bl.lathe(name, [(0.02, -r), (r * 0.8, -r * 0.55), (r, 0.0), (r * 0.8, r * 0.55), (0.02, r)], mat, sides=sides, axis="y", part=part, origin=tuple(c))


# ================================================================ the nine forms
def f_dragon():
    sc = M("dr_scale", (146, 134, 212), 0.45, 0.25, "scales", 1.2, 0.5, groove=(80, 70, 150))
    belly = M("dr_belly", (200, 196, 240), 0.5, 0.1, "scales", 1.2, 0.3)
    spike = M("dr_spike", (224, 230, 255), 0.3, 0.6)
    crystal = E("dr_crystal", "B9A4FF", 9)
    pts = [ROOT_PT, V((0, 12.4, -10)), V((0, 10.2, -20)), V((0, 6.6, -30)), V((0, 4.0, -40)), V((0, 5.2, -48)), V((0, 8.6, -53))]
    ch = Chain(0, pts, 8, lambda t: 3.5 * (1 - t) ** 0.85 + 0.5, 8, lambda k: sc if k < 7 else belly, high=0.95)
    for i in range(1, 8):
        ch.spike(f"ds{i}", i, 0.0, 4.0 * (1 - i / 10.0), 1.1 * (1 - i / 14.0), spike, sides=4, lean=0.25)
    tip = ch.P[-1]
    blade = bl.plate("dr_blade", [(0, -1.4), (4.5, -3.2), (9, 0), (4.5, 3.2), (0, 1.4)], -0.5, 0.5, crystal, part=ch.name(7))
    flat(blade, tip - ch.T[-1] * 1.0, ch.T[-1], ch.B[-1])
    return [ch], "tail", "B9A4FF"


def f_nidhoggr():
    hide = M("ni_hide", (58, 52, 42), 0.75, 0.0, "leather", 1.4, 0.55, groove=(24, 22, 16))
    bone = M("ni_bone", (214, 204, 170), 0.55, 0.0, "grain", 1.0, 0.3)
    spore = E("ni_spore", "6FD01A", 8)
    pts = [ROOT_PT, V((0, 12.5, -10)), V((0, 9.4, -20)), V((0, 5.4, -31)), V((0, 3.4, -42)), V((0, 5.0, -52)), V((0, 8.5, -58))]
    ch = Chain(0, pts, 9, lambda t: 3.7 * (1 - t) ** 0.9 + 0.55, 8, hide, high=0.95)
    for i in range(1, 9, 1):
        if i % 2:
            for sgn in (1, -1):
                ch.spike(f"nt{i}_{sgn}", i, sgn * 1.25, 4.6 - i * 0.25, 1.0, bone, sides=4, lean=0.55)
        elif i < 8:
            orb(f"nu{i}", ch.name(ch.seg_of(i)), tuple(ch.surf(i, 0.0) + ch.N[i] * 0.4), 0.9, spore, sides=5)
    tip = ch.P[-1]
    for k, (yaw, up) in enumerate(((0, 0.0), (35, 0.25), (-35, 0.25))):
        d = (ch.T[-1] + V((math.sin(math.radians(yaw)) * 0.8, up, 0))).normalized()
        bl.tube(f"nroot{k}", [tuple(tip), tuple(tip + d * 4.5 + V((0, -1.0, 0))), tuple(tip + d * 9 + V((0, -0.4 + up * 6, 0)))], 1.0, bone, sides=4, sub=2, part=ch.name(8), taper=(0.9, 0.1))
    return [ch], "tail", "579F0B"


def f_jormungandr():
    sc = M("jo_scale", (30, 72, 142), 0.4, 0.2, "scales", 1.3, 0.5, groove=(14, 34, 78))
    belly = M("jo_belly", (176, 206, 224), 0.5, 0.0, "scales", 1.3, 0.3)
    fin = M("jo_fin", (70, 130, 210), 0.35, 0.1)
    glow = E("jo_glow", "5AB4FF", 8)
    pts = [ROOT_PT, V((0, 12.8, -11)), V((0, 10.2, -22)), V((0, 6.4, -33)), V((0, 3.6, -44)), V((0, 3.4, -54)), V((0, 6.0, -62))]
    ch = Chain(0, pts, 9, lambda t: 4.4 * (1 - t) ** 0.8 + 0.9, 8, lambda k: sc if k < 8 else belly, high=0.92)
    for i in range(1, 9):
        h = 6.5 * (1 - i / 11.0)
        ch.plate(f"jf{i}", i, 0.0, [(-2.4, 0), (-1.2, h), (1.0, h * 0.55), (2.4, 0)], 0.5, fin if i % 2 else glow)
    tip = ch.P[-1]
    fl = bl.plate("jo_fluke", [(0, 0), (3, 3.4), (8, 8.0), (11, 5), (8.5, 0), (11, -5), (8, -8.0), (3, -3.4)], -0.5, 0.5, fin, part=ch.name(8))
    flat(fl, tip - ch.T[-1] * 1.4, ch.T[-1], ch.B[-1])
    return [ch], "tail", "1F4FB0"


def f_serpent():
    sc = M("se_scale", (176, 52, 24), 0.4, 0.15, "scales", 1.3, 0.5, groove=(80, 20, 10))
    belly = M("se_belly", (236, 176, 90), 0.5, 0.0, "scales", 1.3, 0.3)
    fl = E("se_flame", "FF6A10", 5)
    pts = [ROOT_PT, V((0, 12.6, -9)), V((0, 10.0, -18)), V((0, 6.8, -27)), V((0, 5.0, -35)), V((0, 6.6, -42))]
    ch = Chain(0, pts, 7, lambda t: 2.7 * (1 - t) ** 0.85 + 0.5, 6, lambda k: sc if k < 6 else belly, high=0.95)
    for i in range(1, 7):
        ch.spike(f"ss{i}", i, 0.0, 2.6 * (1 - i / 9.0), 0.8, belly, sides=4, lean=0.3)
    tip = ch.P[-1]
    for k, (yaw, ln) in enumerate(((0, 7.0), (30, 5.0), (-30, 5.0))):
        pl = bl.plate(f"sflame{k}", [(0, -1.1), (ln * 0.55, -1.4), (ln, 0), (ln * 0.55, 1.4), (0, 1.1)], -0.4, 0.4, fl, part=ch.name(6))
        flat(pl, tip - ch.T[-1] * 0.6, (ch.T[-1] + V((math.sin(math.radians(yaw)) * 0.9, 0.35, 0))).normalized(), ch.N[-1])
    return [ch], "tail", "FF3A00"


def f_apophis():
    obs = M("ap_obs", (46, 20, 64), 0.3, 0.45, "scales", 1.3, 0.5, groove=(18, 6, 28))
    belly = M("ap_belly", (122, 70, 160), 0.45, 0.2, "scales", 1.3, 0.3)
    void = E("ap_void", "B440FF", 9)
    chains = []
    for ci, s in enumerate((1, -1)):
        pts = [ROOT_PT + V((1.4 * s, 0, 0)), V((4.0 * s, 12.4, -10)), V((9.0 * s, 9.6, -19)), V((11.0 * s, 6.4, -28)), V((7.0 * s, 4.4, -37)), V((2.5 * s, 5.4, -44)), V((1.0 * s, 8.4, -49))]
        ch = Chain(ci, pts, 8, lambda t: 2.7 * (1 - t) ** 0.8 + 0.45, 6, lambda k: obs if k < 7 else belly, high=0.95)
        for i in range(1, 8, 2):
            orb(f"av{ci}_{i}", ch.name(ch.seg_of(i)), tuple(ch.surf(i, 0.0) + ch.N[i] * 0.2), 0.8, void, sides=5)
        for i in range(2, 8):
            ch.spike(f"asp{ci}_{i}", i, 0.0, 2.6 * (1 - i / 10.0), 0.7, belly, sides=4, lean=0.3)
        tip = ch.P[-1]
        pl = bl.plate(f"ap_tip{ci}", [(0, -1.2), (3.0, -2.6), (7.5, 0), (3.0, 2.6), (0, 1.2)], -0.4, 0.4, void, part=ch.name(7))
        flat(pl, tip - ch.T[-1] * 0.8, ch.T[-1], ch.B[-1])
        chains.append(ch)
    return chains, "snake", "6A0F8C"


def f_typhon():
    sc = M("ty_scale", (150, 24, 34), 0.4, 0.2, "scales", 1.2, 0.5, groove=(60, 8, 14))
    belly = M("ty_belly", (226, 150, 80), 0.5, 0.0, "scales", 1.2, 0.3)
    fang = M("ty_fang", (240, 232, 214), 0.35, 0.0)
    eye = E("ty_eye", "FFD23A", 10)
    chains = []
    for ci, s in enumerate((1, -1)):
        pts = [ROOT_PT + V((1.5 * s, 0, 0)), V((4.4 * s, 12.2, -9)), V((9.4 * s, 9.2, -17)), V((11.6 * s, 6.0, -25)), V((8.4 * s, 4.2, -33)), V((4.0 * s, 5.4, -39))]
        ch = Chain(ci, pts, 7, lambda t: 3.3 * (1 - t) ** 0.9 + 1.2, 7, lambda k: sc if k < 6 else belly, high=0.95)
        last = ch.name(6)
        tip, T, N = ch.P[-1], ch.T[-1], ch.N[-1]
        hp = [tip - T * 1.0, tip + T * 2.0 + N * 0.3, tip + T * 6.0 + N * 0.5, tip + T * 9.0 + N * 0.2, tip + T * 11.0 - N * 0.4]
        head, P, Th, Nh, Bh = extra_tube(f"ty_head{ci}", last, hp, lambda t: (1.4 + 2.4 * math.sin(math.pi * min(1.0, t * 1.25)) ** 0.8) * (1 - 0.55 * t * t) + 0.2, 7, sc, wide=1.25, high=0.8, ref=tuple(N))
        for sg in (1, -1):
            eyec = P[2] + Bh[2] * 2.6 * sg + Nh[2] * 1.4
            orb(f"ty_eye{ci}_{sg}", last, tuple(eyec), 0.75, eye, sides=5)
            fg = bl.lathe(f"ty_fang{ci}_{sg}", [(0.55, 0.0), (0.25, 1.6), (0.02, 3.0)], fang, sides=4, axis="z", caps=(True, False), part=last)
            oriented(fg, P[5] + Bh[5] * 1.1 * sg - Nh[5] * 0.7, -Nh[5] + Th[5] * 0.25, tuple(Th[5]))
        for i in range(1, 6, 2):
            ch.spike(f"tsp{ci}_{i}", i, 0.0, 2.8 * (1 - i / 8.0), 0.8, belly, sides=4, lean=0.3)
        chains.append(ch)
    return chains, "snake", "D80023"


def f_kitsune():
    fur = M("ki_fur", (236, 150, 78), 0.9, 0.0, "brushed", 1.5, 0.5, bevel=0.0, groove=(170, 90, 40))
    white = M("ki_white", (252, 240, 232), 0.95, 0.0, "brushed", 1.5, 0.4, bevel=0.0)
    fire = E("ki_fire", "FF8CC0", 8)
    chains = []
    for ci in range(9):
        off = ci - 4
        yaw = math.radians(off * 14.0)
        lift = 0.10 + 0.14 * (4 - abs(off)) / 4.0
        d = V((math.sin(yaw) * 0.95, lift, -math.cos(yaw)))
        ln = 42.0 - abs(off) * 1.5
        pts = curl_path(ROOT_PT + V((0.3 * off, 0.0, 0.0)), d, ln, 10, 0.0, 0.022, lat=-0.003 * off, droop=0.004)
        ch = Chain(ci, pts, 6, lambda t: 0.8 + 0.9 * (1 - t) + 3.3 * math.sin(math.pi * t ** 0.75) ** 0.9, 8, lambda k: fur if k < 5 else white, high=1.0)
        orb(f"kf{ci}", ch.name(5), tuple(ch.P[-1] + ch.T[-1] * 2.4), 0.9, fire, sides=5)
        chains.append(ch)
    return chains, "fluff", "FF7FB0"


def tentacle(ci, start, direction, length, r0, skin, rim, cup_r, n_cups, target, glow_rim=None, k0=0.01, k1=0.04, lat=0.0, droop=0.02):
    """A tapering tentacle: 4 parts of two rings each, curling at the tip, with suckers on the side that faces `target`."""
    pts = curl_path(start, direction, length, 12, k0, k1, lat=lat, droop=droop)
    ch = Chain(ci, pts, 4, lambda t: r0 * (1 - 0.82 * t) ** 1.15 + 0.35, 6, skin, step=2, tip=0.45)
    spots = [4, 6, 7][:n_cups] if n_cups > 1 else [5]
    for j, i in enumerate(spots):
        ch.cup(f"cup{ci}_{j}", i, ch.best_angle(i, target), cup_r * (1.0 - 0.45 * i / len(ch.P)), rim, glow_rim, sides=5)
    return ch


def f_kraken():
    skin = M("kr_skin", (104, 34, 78), 0.5, 0.05, "leather", 1.6, 0.6, groove=(50, 14, 44))
    rim = M("kr_rim", (238, 186, 170), 0.55, 0.0, "leather", 2.0, 0.3)
    chains = []
    for j in range(12):
        side = 1 if j % 2 == 0 else -1
        row = j // 2
        start = V((side * (1.2 + (row % 3) * 1.1), 21.0 - row * 1.4, -2.9 - (row % 2) * 0.5))
        yaw = side * math.radians(10 + row * 12)
        d = V((math.sin(yaw), -0.34 - 0.03 * (row % 3), -math.cos(yaw)))
        ln = (46 if row % 2 == 0 else 36) + (row % 3) * 3
        chains.append(tentacle(j, start, d, ln, 3.6 if row % 2 == 0 else 3.0, skin, rim, 1.6, 2, V((-side * 0.4, 0.5, 0.8)), k0=0.004 + 0.001 * row, k1=0.105, lat=side * (0.006 + 0.003 * row), droop=0.014))
    return chains, "tentacle", "FF6153"


def f_scylla():
    skin = M("sc_skin", (22, 118, 112), 0.5, 0.05, "leather", 1.6, 0.6, groove=(10, 56, 56))
    rim = M("sc_rim", (214, 238, 226), 0.55, 0.0, "leather", 2.0, 0.3)
    glow = E("sc_glow", "3AE8D4", 6)
    chains = []
    roots = ((-3.9, -0.4), (-3.5, 1.1), (-1.4, 2.1), (1.4, 2.1), (3.5, 1.1), (3.9, -0.4))
    dirs = ((-1.0, -0.30, -0.35), (-0.85, -0.30, 0.45), (-0.40, -0.30, 0.95), (0.40, -0.30, 0.95), (0.85, -0.30, 0.45), (1.0, -0.30, -0.35))
    for j, ((x, z), d) in enumerate(zip(roots, dirs)):
        side = -1 if x < 0 else 1
        chains.append(tentacle(j, V((x, 12.5, z)), V(d), 30 + (j % 3) * 3, 3.0, skin, rim, 1.4, 3, V((-side * 0.2, 0.6, -d[2] * 0.5)), glow, k0=0.0, k1=0.10, lat=-side * 0.012, droop=0.016))
    return chains, "tentacle", "047A73"


FORMS = {"dragon": (f_dragon, 0.5), "nidhoggr": (f_nidhoggr, 0.5), "jormungandr": (f_jormungandr, 0.5), "serpent": (f_serpent, 0.5), "apophis": (f_apophis, 0.45),
         "typhon": (f_typhon, 0.45), "kitsune": (f_kitsune, 0.45), "kraken": (f_kraken, 0.35), "scylla_charybdis": (f_scylla, 0.35)}
SKIN = {}


def poses_for(chains, kind):
    out = {}
    for name, secs in (("sway_a", 0.0), ("sway_b", 1.1)):
        p = {}
        for ch in chains:
            for k, n in enumerate(ch.parts):
                p[n] = sway(kind, ch.ci, k, secs + 0.3)
        out[name] = p
    return out


def shrink_skin(path, colors=256):
    """The baked skin as a palette PNG (Pillow's fast octree keeps the alpha)."""
    from PIL import Image
    im = Image.open(path).convert("RGBA")
    im.quantize(colors=colors, method=Image.Quantize.FASTOCTREE).save(path, optimize=True)
    return os.path.getsize(path)


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    only = argv[argv.index("--only") + 1].split(",") if "--only" in argv else list(FORMS)
    pv = trial or PREVIEW_DIR
    for stem in only:
        fn, lod = FORMS[stem]
        bl.reset(samples=8 if quick else 20)
        be.seed(11)
        chains, kind, tint = fn()
        rig = {}
        for ch in chains:
            ch.rig(rig)
        total = sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")
        print(stem, "faces before export:", total, "chains", len(chains))
        eid = "bwtail_" + stem
        previews = {os.path.join(pv, stem + "_side.png"): (90, None), os.path.join(pv, stem + "_back.png"): (205, "sway_a")}
        px = SKIN.get(stem, 512)
        be.finish_entity(eid, rig, glow=(tint, 0.1, 0.55, 1.0), skin=256 if quick else px, bake=512 if quick else px * 2, lod_ratio=lod, out_mesh=trial, out_skin=trial,
                         previews=previews, poses=poses_for(chains, kind), budget=BUDGET, cage=0.2, smooth=80.0)
        for suffix in (".png", "_glow.png"):
            print("skin", eid + suffix, shrink_skin(os.path.join(trial or be.ENTITY_SKIN_DIR, eid + suffix)) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1:])
