"""The signature pieces of the Erza armours (1.99.0): wings, halos, tails, capes, backdrops, ribbons.

A feather, a fin or a ribbon is a STRIP: a shell whose surface is a ribbon of changing width along a curved centre line (so one painted sheet, a rib and barbs, runs the whole length). Pieces that move
(wings, halos, tails, capes, ribbons) get a looped `idle` clip, the one the painter plays (requip/ArmourSets: clip "idle"; wings run faster in the air).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_erza_armour_core import *  # noqa: E402,F401,F403


def lerp3(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def bez(p0, p1, p2, t):
    """A quadratic curve through three points."""
    return tuple((1 - t) ** 2 * p0[i] + 2 * (1 - t) * t * p1[i] + t * t * p2[i] for i in range(3))


def strip(p, bone, centre_fn, width_fn, normal_hint, nv, nu, mat, thick=0.3, side=0, double=True, rim="", cell=None, sheet=None, d=None, facing=(0.0, 0.0, 1.0)):
    """A flat ribbon: the surface S(u, v) = centre(v) + (u - 0.5) * width(v) * across(v), where across is square to the direction of travel and to `normal_hint`.
    v runs along the ribbon (0 = root, 1 = tip), so the texture's top is the tip."""
    nh = unit(normal_hint)

    def S(u, v):
        c = np.asarray(centre_fn(v), np.float64)
        e = 1e-3
        t = unit(np.asarray(centre_fn(min(1.0, v + e)), np.float64) - np.asarray(centre_fn(max(0.0, v - e)), np.float64))
        across = unit(np.cross(t, nh))
        return V3(c + (u - 0.5) * width_fn(v) * across)

    # the glow pass culls back faces, so the outer face (the one with the glow) must face `facing`: the back for wings and capes
    fv = V3(facing)
    return p.shell(bone, S, nu, nv, mat, thick=thick, rim=rim, double=double, ref=lambda P: np.asarray(P) - 5.0 * fv, d=d, sheet=sheet)


# ====================================================================================================================== wings
# the rows of feathers of each style: (count, length of the longest, width, offset toward the back); the rows are layered from the back (the long flight feathers) to the front (the short coverts)
FEATHER_KW = dict(taper=0.0)
WINGS = {
    "feather": dict(rows=[(9, 20.0, 3.0, 0.0), (7, 12.5, 3.1, 0.55), (6, 7.0, 3.2, -0.5)], glass=False, zig=0.0, spar=True),
    "blade": dict(rows=[(8, 20.0, 2.5, 0.0), (7, 13.0, 2.6, 0.55), (5, 7.5, 2.6, -0.5)], glass=False, zig=0.0, spar=True),
    "bolt": dict(rows=[(7, 19.0, 2.8, 0.0), (6, 11.0, 2.8, 0.5)], glass=True, zig=1.3, spar=True),
    "fin": dict(rows=[(7, 15.0, 4.6, 0.0), (6, 9.5, 4.4, 0.5)], glass=True, zig=0.0, spar=True),
}


def wing(c, style):
    P = c.pal
    spec = WINGS.get(style)
    for side in (0, 1):
        p = c.piece("wing_" + ("l" if side else "r"), skin=None)
        a = Anim()
        a.clip("idle", length=1.4, loop=True)
        sg = 1.0 if side else -1.0
        R = V(-2.2, 21.2, 3.0, side)
        p.bone("w", tuple(R))
        sine_rot(a, "idle", "w", 1.4, 2, -16.0 * sg, 0.0)
        sine_rot(a, "idle", "w", 1.4, 1, 7.0 * sg, 0.12)
        groups = {}
        for r in range(3):
            bn = "r%d" % r
            p.bone(bn, tuple(R), parent="w")
            sine_rot(a, "idle", bn, 1.4, 2, -(5.0 + 3.0 * r) * sg, 0.18 + 0.08 * r)
            groups[r] = bn
        if style == "fairy":
            fairy_wing(c, p, a, side)
        elif style == "jet":
            jet_wing(c, p, a, side)
        else:
            fm = (P["glass"] if spec["glass"] else P["feather"]).with_(**FEATHER_KW)
            arm = lambda t: bez(V(-2.2, 21.2, 3.0, side), V(-7.5, 26.2, 4.4, side), V(-13.5, 28.0, 5.6, side), t)
            for i in range(8):                                                      # the leading bone of the wing: a bent spar of tapered segments
                t0, t1 = i / 8.0, (i + 1) / 8.0
                p.between("w", arm(t0), arm(t1), 1.5 - 0.8 * t0, 1.1 - 0.5 * t0, P["trim"], normal=V(0, 0, -1, side))
            gem(p, "w", arm(0.05), 1.6, P["gem"])
            gem(p, "w", arm(1.0), 1.1, P["gem"])
            outv = V3((sg, 0.0, 0.0))
            for r, (n, L0, wd, zoff) in enumerate(spec["rows"]):
                for i in range(n):
                    s = (i + 0.5) / n
                    root = np.asarray(arm(0.1 + 0.9 * s), np.float64)
                    ang = math.radians(lerp(86.0, 14.0, s ** 0.85))                  # from hanging down to pointing out
                    dirv = unit(outv * math.cos(ang) + V3((0, -1, 0)) * math.sin(ang) + V3((0, 0, 0.18 + 0.22 * s)))
                    L = L0 * (0.55 + 0.45 * math.sin(math.pi * min(1.0, 0.2 + 0.8 * s) * 0.92))
                    zig = spec["zig"]
                    across = unit(np.cross(dirv, V3((0, 0, -1))))

                    def ctr(v, root=root, dirv=dirv, L=L, r=r, zoff=zoff, zig=zig, across=across, i=i):
                        bend = V3((0, -1, 0.35)) * (L * 0.13 * v * v)
                        zz = across * (zig * math.sin(v * math.pi * 3.0 + i) * v if zig else 0.0)
                        return tuple(root + dirv * L * v + bend + V3((0, 0, zoff + 0.05 * i * (1 if r == 0 else 0))) + zz)

                    def wfn(v, wd=wd):
                        return wd * (1.0 - v ** 2.6) ** 0.55 + 0.25
                    strip(p, groups[r], ctr, wfn, V3((0, 0.2, -1)), max(4, int(L / 1.7)), 1, fm, thick=0.32, side=side, double=spec["glass"])
        c.done(p, a)


def fairy_wing(c, p, a, side):
    P = c.pal
    sg = 1.0 if side else -1.0
    gl = P["glass"]
    R = V(-2.4, 21.0, 3.0, side)
    lobes = [(V(-14.5, 31.0, 7.0, side), 13.0, 9.0), (V(-13.0, 20.5, 7.4, side), 11.5, 7.0), (V(-8.5, 14.0, 6.0, side), 7.5, 4.8), (V(-9.0, 26.0, 5.2, side), 9.5, 5.5)]
    for i, (tip, L, wd) in enumerate(lobes):
        r = np.asarray(tip, np.float64)
        Rv = np.asarray(R, np.float64)
        d = unit(r - Rv)
        ctr = lambda v, Rv=Rv, r=r: tuple(Rv + (r - Rv) * v + V3((0, 0, 1.2 * math.sin(math.pi * v))))
        # a lobe is a leaf: wide in the middle, pointed at both ends
        strip(p, "r%d" % min(2, i), ctr, lambda v, wd=wd: wd * math.sin(math.pi * min(1.0, 0.06 + 0.94 * v)) ** 0.8, V3((0, 0.3, -1)), max(5, int(L / 1.4)), max(3, int(wd / 1.5)), gl.with_(taper=0.0, cells=7),
              thick=0.3, side=side)
        p.between("r%d" % min(2, i), tuple(R), tuple(tip), 0.4, 0.4, P["trim"], normal=V(0, 0, -1, side))


def jet_wing(c, p, a, side):
    P = c.pal
    # two rigid blades with a thruster: Flight Armor
    for k, (L, ang, wd) in enumerate(((15.0, 36.0, 4.4), (11.0, 62.0, 3.6))):
        base = V(-2.4, 20.5 - 2.3 * k, 3.1, side)
        d = V(-math.cos(math.radians(ang)), math.sin(math.radians(ang)), 0.35, side)
        ctr = lambda v, base=base, d=d, L=L: tuple(np.asarray(base, np.float64) + unit(d) * L * v)
        strip(p, "r%d" % k, ctr, lambda v, wd=wd: wd * (1.0 - 0.55 * v ** 1.5), V3((0, 0, -1)), max(4, int(L / 1.3)), 3, P["main"].with_(lame=0, trim=1), thick=0.55, side=side)
        tip = np.asarray(base, np.float64) + unit(d) * L
        p.between("r%d" % k, tip - unit(d) * 2.0, tip + unit(d) * 0.6, 1.8, 1.8, P["trim"], normal=V3((0, 0, -1)))
        gem(p, "r%d" % k, tuple(tip + unit(d) * 0.9), 1.5, P["hot"])


# ====================================================================================================================== halo
def halo(c, style):
    P = c.pal
    p = c.piece("halo", skin=None)
    a = Anim()
    a.clip("idle", length=10.0, loop=True)
    cy = 38.4
    p.bone("spin", (0, cy, 0))
    n = 20
    R = 6.6
    pts = [V3((R * math.sin(2 * math.pi * i / n), cy, -R * math.cos(2 * math.pi * i / n))) for i in range(n + 1)]
    for i in range(n):
        p.between("spin", pts[i], pts[i + 1], 0.9, 0.7, P["trim"], normal=V3((0, 1, 0)))
    ring2 = [V3(((R - 0.9) * math.sin(2 * math.pi * i / n), cy + 0.55, -(R - 0.9) * math.cos(2 * math.pi * i / n))) for i in range(n + 1)]
    for i in range(n):
        p.between("spin", ring2[i], ring2[i + 1], 0.45, 0.4, P["hot"], normal=V3((0, 1, 0)))
    m = {"blades": 9, "runes": 6, "flame": 14, "coral": 10, "petals": 12, "ring": 8}[style]
    for i in range(m):
        ang = 2 * math.pi * i / m
        out = V3((math.sin(ang), 0.0, -math.cos(ang)))
        base = V3((R * math.sin(ang), cy + 0.4, -R * math.cos(ang)))
        if style == "blades":
            blade(p, "spin", base + V3((0, 0.3, 0)), V3((0, 1, 0)), out, 4.8, 1.0, P["hot"].with_(taper=0.8, tip_top=1), segs=3, tip=0.35, thick=0.35)
            p.cbox("spin", base + V3((0, 0.5, 0)), (2.0, 0.5, 0.6), P["trim"], eul=euler_of(frame(V3((0, 1, 0)), out)))
        elif style == "runes":
            plate = p.cbox("spin", base + V3((0, 1.9, 0)), (2.2, 3.0, 0.5), P["main"], eul=euler_of(frame(V3((0, 1, 0)), out)), decals={"north": [Decal("rune", 0.8, (0.5, 0.5), mode="glow", seed=i)]})
            p.cbox("spin", base + V3((0, 0.4, 0)), (1.2, 0.8, 0.8), P["trim"])
        elif style == "flame":
            spike(p, "spin", base, V3((0, 1, 0)) + out * 0.15, 3.6 + 1.0 * (i % 2), 1.6, P["glass"].with_(taper=0), segs=4, bend=out * 0.4 + V3((0, 0.4, 0)), thick=0.35, normal=out)
        elif style == "coral":
            spike(p, "spin", base, V3((0, 1, 0)) + out * 0.25, 3.4 + (i % 3), 1.2, P["main"], segs=4, bend=out * 0.7, normal=out)
            gem(p, "spin", base + V3((0, 4.0, 0)) + out * 0.6, 0.8, P["hot"])
        elif style == "petals":
            blade(p, "spin", base, V3((0, 1, 0)) + out * 0.2, out, 3.2, 2.0, P["glass"].with_(taper=0.7, tip_top=1), segs=3, tip=0.4, thick=0.3)
        elif style == "ring":
            gem(p, "spin", base + V3((0, 0.6, 0)), 1.1, P["gem"])
    spin_y(a, "idle", "spin", 10.0, turns=1, steps=8)
    c.done(p, a)


# ====================================================================================================================== tail
def tail(c, style):
    P = c.pal
    p = c.piece("tail", skin=None)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    if style == "ribbon":
        for k, sx in enumerate((-1.2, 1.2)):
            parent = "root"
            y, z = 12.5, 2.7
            for i in range(5):
                b = "r%d_%d" % (k, i)
                p.bone(b, (sx, y, z), parent=parent)
                seg = lambda v, sx=sx, y=y, z=z: (sx + 0.2 * v, y - 0.9 * v, z + 3.4 * v)
                strip(p, b, seg, lambda v: 1.9, V3((0, 1, 0)), 3, 1, (P["cloth"] if i % 2 == 0 else P["alt"]).with_(ragged=1 if i == 4 else 0), thick=0.3)
                sine_rot(a, "idle", b, 2.0, 1, 17.0, phase=-i * 0.11 + k * 0.3)
                parent = b
                z += 3.3
                y -= 0.9
        c.done(p, a)
        return
    mats = [P["alt"], P["main"]]
    n = 6
    y, z, parent = 11.4, 2.6, "root"
    curl = [14, 10, 4, -8, -16, -22]
    for i in range(n):                                                                 # armoured tail segments tapering, curling up at the end, ending in a flame tuft
        b = "t%d" % i
        p.bone(b, (0, y, z), parent=parent, rotation=(curl[i], 0, 0))
        r = max(0.9, 1.9 - i * 0.18)
        c0, c1 = V3((0, y, z)), V3((0, y, z + 3.4))
        p.between(b, c0, c1, 2 * r, 2 * r * 0.95, mats[i % 2], normal=V3((0, 1, 0)))
        p.between(b, c0 + V3((0, 0, 0.3)), c1 - V3((0, 0, 0.3)), 2 * r, 2 * r * 0.95, mats[(i + 1) % 2], normal=V3((1, 1, 0)))       # the same box turned 45 degrees: an octagonal section
        p.between(b, c1 - V3((0, 0, 0.5)), c1 + V3((0, 0, 0.2)), 2 * r + 0.5, 2 * r + 0.3, P["trim"], normal=V3((0, 1, 0)))
        sine_rot(a, "idle", b, 2.0, 1, 11.0, phase=-i * 0.1)
        parent = b
        z += 3.4
    p.bone("tuft", (0, y, z), parent=parent)
    for k, ang in enumerate((-26, 0, 26)):
        spike(p, "tuft", V3((0, y, z)), V3((math.sin(math.radians(ang)) * 0.7, 0.1, 1.0)), 5.0 - abs(k - 1) * 1.2, 2.0, P["glass"], segs=4, bend=V3((0, 0.7, 0.2)), thick=0.4, normal=V3((0, 1, 0)))
    sine_rot(a, "idle", "tuft", 2.0, 1, 18.0, phase=-0.6)
    c.done(p, a)


# ====================================================================================================================== cape
def cape(c, style):
    P = c.pal
    p = c.piece("cape", skin=None)
    a = Anim()
    a.clip("idle", length=2.6, loop=True)
    n = 5
    top = 23.3
    for i in range(n):
        cx = -4.2 + i * 2.1
        b = "p%d" % i
        p.bone(b, (cx, top, 2.9))
        for j, (y0, y1) in enumerate(((top, top - 7.0), (top - 6.8, top - 14.0))):
            mat = (P["cloth"] if i % 2 == 0 else P["alt"]).with_(pleats=1)
            def seg(v, cx=cx, y0=y0, y1=y1, j=j):
                return (cx, y0 + (y1 - y0) * v, 2.95 + 0.9 * j + 0.5 * v)
            strip(p, b, seg, lambda v: 2.15, V3((0, 0, 1)), 6, 2, mat.with_(taper=0.0), thick=0.35)
        p.cbox(b, (cx, top - 14.2, 3.7), (2.2, 0.6, 0.8), P["trim"])
        sine_rot(a, "idle", b, 2.6, 0, 5.0 + 1.0 * (i % 2), phase=i * 0.16)
    p.cbox("root", (0, top + 0.2, 2.8), (10.6, 1.0, 1.3), P["trim"])
    for sx in (-1, 1):
        gem(p, "root", V3((sx * 4.6, top + 0.2, 2.2)), 1.2, P["gem"])
    c.done(p, a)


# ====================================================================================================================== backdrop
def backdrop(c, style):
    P = c.pal
    p = c.piece("backdrop", skin=None)
    a = Anim()
    cy, cz = 19.0, 9.5
    if style in ("wheel", "chariot"):
        a.clip("idle", length=24.0, loop=True)
        n = 10 if style == "wheel" else 14
        p.bone("disc", (0, cy, cz))
        for i in range(n):
            ang = 2 * math.pi * i / n
            out = V3((math.sin(ang), math.cos(ang), 0.0))
            base = V3((0, cy, cz)) + out * 4.6
            blade(p, "disc", base, out, V3((0, 0, -1)), 9.0 if style == "wheel" else 10.5, 2.0, P["hot"].with_(taper=0.5, tip_top=1), segs=4, tip=0.3, thick=0.45)
            p.between("disc", base - out * 0.2, base + out * 0.8, 3.2, 0.9, P["trim"], normal=V3((0, 0, -1)))
            p.between("disc", base + out * 0.8, base + out * 2.4, 0.9, 1.0, P["main"], normal=V3((0, 0, -1)))
        for rr, mat in ((4.6, P["trim"]), (3.9, P["main"])):
            pts = [V3((rr * math.sin(2 * math.pi * i / 16), cy + rr * math.cos(2 * math.pi * i / 16), cz)) for i in range(17)]
            for i in range(16):
                p.between("disc", pts[i], pts[i + 1], 1.0, 0.8, mat, normal=V3((0, 0, -1)))
        p.cbox("disc", V3((0, cy, cz - 0.05)), (4.6, 4.6, 1.0), P["main"], eul=(0, 0, 45), decals={"north": [Decal("wheel", 0.8, (0.5, 0.5), mode="glow")]})
        gem(p, "disc", V3((0, cy, cz - 0.7)), 1.8, P["gem"])
        sg = [1] if style == "wheel" else [1, -1]
        a.rot("idle", "disc", {24.0 * i / 8: (0, 0, 360.0 * i / 8) for i in range(9)})
        if style == "chariot":
            p.bone("disc2", (0, cy, cz + 2.2))
            for i in range(8):
                ang = 2 * math.pi * i / 8
                out = V3((math.sin(ang), math.cos(ang), 0.0))
                blade(p, "disc2", V3((0, cy, cz + 2.2)) + out * 2.6, out, V3((0, 0, -1)), 6.0, 1.5, P["trim"].with_(taper=0.6, tip_top=1), segs=3, tip=0.35, thick=0.4)
            a.rot("idle", "disc2", {24.0 * i / 8: (0, 0, -360.0 * i / 8) for i in range(9)})
            p.between("root", V3((-9.5, cy, cz)), V3((9.5, cy, cz)), 1.2, 1.1, P["main"], normal=V3((0, 0, -1)))
    else:                                                                               # "spikes": the spines of Purgatory
        a.clip("idle", length=4.0, loop=True)
        p.bone("spines", (0, cy, 3.2))
        spec = [(0, 4, 11.0), (-3.6, 3, 9.0), (3.6, 3, 9.0), (-6.2, 0, 7.2), (6.2, 0, 7.2), (-5.0, -4, 6.0), (5.0, -4, 6.0), (0, -6, 5.5)]
        for k, (dx, dy, L) in enumerate(spec):
            base = V3((dx, cy + dy, 3.0))
            d = V3((dx * 0.12, 0.35 + (0.7 if dy >= 0 else -0.2), 0.75))
            spike(p, "spines", base, d, L, 3.0, P["main"] if k % 2 == 0 else P["trim"], segs=6, bend=V3((0, 0.5, 0.35)), normal=V3((0, 0, -1)))
            p.cbox("spines", base + V3((0, 0, 0)), (3.4, 1.0, 2.2), P["hot"])
        sine_rot(a, "idle", "spines", 4.0, 2, 2.5)
    c.done(p, a)


# ====================================================================================================================== ribbon (the hair ribbon / scarf of the head)
def ribbon(c, style):
    P = c.pal
    p = c.piece("ribbon", skin=None)
    a = Anim()
    a.clip("idle", length=2.2, loop=True)
    for k, sx in enumerate((-1.0, 1.0)):
        parent = "root"
        y, z = 30.0, 4.6
        for i in range(4):
            b = "r%d_%d" % (k, i)
            p.bone(b, (sx * 1.2, y, z), parent=parent)
            seg = lambda v, sx=sx, y=y, z=z: (sx * (1.2 + 0.8 * v), y - 3.4 * v, z + 1.2 * v)
            strip(p, b, seg, lambda v: 2.0, V3((0, 0, 1)), 3, 1, (P["cloth"] if (i + k) % 2 == 0 else P["alt"]).with_(taper=0.0), thick=0.3)
            sine_rot(a, "idle", b, 2.2, 0, 13.0, phase=-i * 0.12 + k * 0.25)
            parent = b
            y -= 3.4
            z += 1.2
    gem(p, "root", V3((0, 30.4, 4.5)), 1.5, P["gem"], facing=V3((0, 0, 1)))
    c.done(p, a)
