"""The pieces only the spirits have (Spirit Dive, Djinn-equip style): crowns and crests, the plume of the Salamander, mantles, wings of plates, the crystals of the Gnome.

Everything is built the way the Djinn-quality armour pieces are (tools/gen_erza_armour_core.py: lofts, curved plates, blades, spikes, gems; tools/erza_armour_kit.py: painted sheets, culling, atlas) and is
ARMOUR: plates and blades hinged on bones, never floating ribbons. The body frame is the one of the geo files (feet y = 0, shoulders y = 24, head y = 24..32, the right arm at x = -5, the front on -z).

    build(c, name)   name: crest | crest_l | crest_r | crest_c | plume | mantle | wing | backcrystals     (c: a DCtx of tools/gen_spirit_dive_djinn.py: c.el, c.tier, c.pal, c.ornate)
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_erza_armour_core import *  # noqa: E402,F401,F403
import gen_erza_armour_extras as EX  # noqa: E402

ORDER = ["crest", "plume", "mantle", "wing", "backcrystals"]


# ====================================================================================================================== small helpers
def flat_plate(p, bone, centre, right, up, w, h, mat, thick=0.5, rim="lrtb", decals=(), outward=None, nu=1, nv=1):
    """A flat armour plate: w x h model pixels centred on `centre`, its width along `right` and its height along `up`; the outer face looks along `outward` (default: right x up)."""
    r, u = unit(V3(right)), unit(V3(up))
    n = unit(np.cross(r, u)) if outward is None else unit(V3(outward))
    c0 = V3(centre)

    def S(a, b):
        return c0 + r * ((a - 0.5) * w) + u * ((b - 0.5) * h)
    return p.shell(bone, S, nu, nv, mat, thick=thick, rim=rim, decals=decals, ref=c0 - n * 5.0)


def curved_blades(p, bone, base_fn, dir_fn, n, mat_fn, length_fn, width, normal_fn, segs=3, bend_fn=None, thick=0.4, tip=0.3):
    """n blades from points of a curve: base_fn(t), dir_fn(t), normal_fn(t), length_fn(t), mat_fn(i), bend_fn(t) (t = 0..1 along the curve)."""
    out = []
    for i in range(n):
        t = i / max(1.0, n - 1.0)
        made, tip_pos = blade(p, bone, base_fn(t), dir_fn(t), normal_fn(t), length_fn(t), width, mat_fn(i), segs=segs, bend=bend_fn(t) if bend_fn else (0, 0, 0), thick=thick, tip=tip)
        out.append(tip_pos)
    return out


def crystal(p, bone, base, direction, length, width, mat, normal=(0, 0, -1)):
    """A cut crystal prism: a body, the same body turned 45 degrees about its axis (an eight-sided section) and a pointed cap; one hard gem, not a stack of boxes."""
    d = unit(V3(direction))
    b = V3(base)
    body = length * 0.62
    c0 = b + d * (body / 2.0)
    M = frame(d, normal, 1, 2)
    p.cbox(bone, c0, (width, body, width * 0.8), mat, eul=euler_of(M))
    p.cbox(bone, c0, (width, body * 0.98, width * 0.8), mat, eul=euler_of(M @ rot_xyz(0, 45, 0)))
    tip0 = b + d * body
    p.cbox(bone, tip0 + d * (length * 0.19), (width * 0.62, length * 0.38, width * 0.5), mat, eul=euler_of(M @ rot_xyz(0, 45, 0)))
    return b + d * length


def ring_band(p, bone, y0, y1, a, depth, mat, cell=1.2, thick=0.6, th0=-180, th1=180, decals=()):
    """A closed band round the dome of the helm (the cap is 4.6 wide at y 29.5 to 32)."""
    L = Loft([(y0, a, depth, depth, 5), (y1, a - 0.08, depth - 0.08, depth - 0.08, 5)])
    return shell_auto(p, bone, L.surf(th0, th1, y0, y1), mat, ref=L.ref(), rim="tb", thick=thick, cell=cell, decals=decals)


# ====================================================================================================================== wind: the laurel and the crown
def _laurel(c, side):
    """The laurel of the Sylph: leaf plates in two rows along the temple, from the brow to the nape, each leaf swept back and a little up (one half of the crest)."""
    P = c.pal
    p = c.piece("crest_" + ("l" if side else "r"), skin=None)
    n = 8 if c.ornate else 6

    def base(t):
        return V(-4.95 - 0.3 * math.sin(math.pi * t), 29.7 + 1.6 * math.sin(math.pi * t * 0.9) + 0.4 * t, -3.7 + 7.4 * t, side)

    def tang(t):
        return V(0.0, 0.25 * math.cos(math.pi * t * 0.9), 1.0, side)
    for i in range(n):
        t = i / (n - 1.0)
        b = base(t)
        up = 0.55 + 0.25 * math.sin(math.pi * t)
        for row in range(2):
            d = V(-(0.55 if row == 0 else 0.95), up if row == 0 else -0.1, 0.55 + 0.25 * t, side)
            L = (5.4 - 1.6 * abs(t - 0.45)) * (1.0 if row == 0 else 0.72)
            blade(p, "root", b + V(-0.1 * row, -0.5 * row, 0.0, side), d, V(-1.0, 0.25, 0.0, side), L, 2.3 - 0.4 * row, P["feather"] if row == 0 else P["trim"], segs=2,
                  bend=V(-0.2, 0.5, 0.5, side), thick=0.4, tip=0.3)
    # the band that holds the laurel: a gold bar along the temple, a gem at the brow end
    pts = [base(t) + V(0.35, -0.55, 0.0, side) for t in (0.0, 0.33, 0.66, 1.0)]
    strap(p, "root", pts, 0.9, 0.6, P["trim"], normal=V(-1, 0, 0, side))
    gem(p, "root", base(0.0) + V(-0.3, -0.1, -0.5, side), 1.2 if c.ornate else 0.9, P["gem"], facing=V(-0.6, 0, -0.8, side))
    if c.ornate:                                              # an ear-wing: three small feather blades folded back behind the ear
        for k in range(3):
            blade(p, "root", V(-5.0, 27.6 - 0.9 * k, 1.8 + 0.5 * k, side), V(-0.35, -0.1 - 0.12 * k, 1.0, side), V(-1, 0, 0.1, side), 4.4 - 0.7 * k, 1.5, P["feather"], segs=2,
                  bend=V(-0.1, -0.5, 0.4, side), thick=0.35)
    return c.done(p)


def _wind_crown(c):
    """The crown-like crest of Yuno's dive, MOUNTED on the helm: a gold ring round the cap with a fan of leaf prongs at the front and three taller ones behind (a halo-like arc), a gem in the brow."""
    P = c.pal
    p = c.piece("crest_c", skin=None)
    ring_band(p, "root", 32.35, 33.25, 4.62, 4.62, P["trim"], cell=0.95, thick=0.6)
    cap = Loft([(32.35, 4.6, 4.6, 4.6, 5), (33.25, 4.5, 4.5, 4.5, 5)])
    prongs = [(-70, 3.8), (-52, 4.6), (-34, 5.4), (-17, 6.2), (0, 7.4), (17, 6.2), (34, 5.4), (52, 4.6), (70, 3.8)]
    for k, (th, L) in enumerate(prongs):
        out = unit(V3((math.sin(math.radians(th)), 0.0, -math.cos(math.radians(th)))))
        base = cap.point(th, 32.9, 0.1)
        lean = 0.30 + 0.07 * abs(k - 4)                                  # the prongs fan out like a tiara
        blade(p, "root", base, V3((0, 1, 0)) + out * lean, out, L, 2.3 if k not in (0, 8) else 1.8, P["alt"] if k % 2 == 0 else P["main"], segs=3, bend=out * 0.1 + V3((0, 0.2, 0.1)), thick=0.45, tip=0.04)
        p.cbox("root", base + V3((0, 0.2, 0)) + out * 0.2, (1.5, 0.7, 0.7), P["trim"], eul=euler_of(frame(V3((0, 1, 0)), out)))
    for th, L in ((180, 6.4), (152, 5.4), (208, 5.4)):          # three taller prongs behind the head: the arc of the halo, standing on the ring
        out = unit(V3((math.sin(math.radians(th)), 0.0, -math.cos(math.radians(th)))))
        blade(p, "root", cap.point(th, 32.9, 0.1), V3((0, 1, 0)) + out * 0.45, out, L, 2.3, P["trim"], segs=2, bend=out * 0.2 + V3((0, 0.2, 0)), thick=0.45, tip=0.12)
    gem(p, "root", V3((0, 33.3, -4.95)), 1.7, P["gem"], facing=V3((0, 0.4, -1)))
    return c.done(p)


# ====================================================================================================================== crests of fire, water and earth
def _fire_crest(c):
    """The flame crest of the Salamander's helm: tongues of gold-edged plate rising from the brow and sweeping back (three for the standard tier, a fan of seven and two swept horns for the character tier)."""
    P = c.pal
    p = c.piece("crest", skin=None)
    ring_band(p, "root", 32.3, 33.0, 4.6, 4.6, P["trim"], cell=1.0, thick=0.55)
    n = 7 if c.ornate else 3
    for k in range(n):
        t = (k - (n - 1) / 2.0) / max(1.0, (n - 1) / 2.0)
        sx = t * 3.0
        L = (8.5 - 3.4 * abs(t)) * (1.0 if c.ornate else 0.8)
        base = V3((sx, 32.9, -2.4 + 0.7 * abs(t)))
        d = V3((t * 0.35, 1.0, 0.35 + 0.35 * (1 - abs(t))))
        blade(p, "root", base, d, V3((1, 0, 0)) if abs(t) < 0.1 else V3((math.copysign(1, t), 0.1, 0)), L, 2.4 if k == (n - 1) // 2 else 1.9, P["hot"].with_(taper=0.8, tip_top=1) if k % 2 else P["main"],
              segs=4, bend=V3((0, 0.55, 0.9)), thick=0.5, tip=0.2)
    if c.ornate:
        for sx in (0, 1):                                      # two swept horns of gold from the temples
            spike(p, "root", V(-4.4, 30.8, -1.0, sx), V(-0.8, 0.8, 0.1, sx), 6.6, 2.0, P["trim"], segs=6, bend=V(0.9, 0.9, 0.5, sx), normal=V(0, 0, -1, sx))
    gem(p, "root", V3((0, 32.9, -4.7)), 1.5, P["gem"], facing=V3((0, 0.3, -1)))
    return c.done(p)


def _water_crest(c):
    """The fin helm of the Undine: a dorsal fin of pearl plates rising from the brow, over the crown and sweeping back along the nape (three big overlapping plates, five for the character tier), and a swept fin at each temple."""
    P = c.pal
    p = c.piece("crest", skin=None)
    ring_band(p, "root", 32.3, 33.0, 4.6, 4.6, P["trim"], cell=1.0, thick=0.55)
    n = 5 if c.ornate else 3
    for k in range(n):
        t = k / (n - 1.0)
        base = V3((0, 33.1 - 0.4 * t, -3.0 + 6.6 * t))
        ang = math.radians(8 + 70 * t)                          # from standing at the brow to lying back along the nape
        d = V3((0, math.cos(ang), math.sin(ang) * 0.9 + 0.3))
        L = (7.0 + 3.6 * math.sin(math.pi * (0.15 + 0.7 * t))) * (1.0 if c.ornate else 0.9)
        blade(p, "root", base, d, V3((1, 0, 0)), L, 3.6, P["alt"] if k % 2 == 0 else P["main"], segs=3, bend=V3((0, -0.25, 1.0)), thick=0.45, tip=0.1)
    for sx in (0, 1):
        for k in range(2 if c.ornate else 1):
            blade(p, "root", V(-4.75, 29.9 + 1.4 * k, -0.3 + 1.6 * k, sx), V(-0.8, 0.3 + 0.25 * k, 0.5, sx), V(0, 1, -0.2, sx), 6.4 - 1.0 * k, 2.6, P["alt"] if k == 0 else P["glass"], segs=3,
                  bend=V(-0.4, 0.2, 0.8, sx), thick=0.4, tip=0.12)
    gem(p, "root", V3((0, 32.7, -4.85)), 1.6, P["gem"], facing=V3((0, 0.3, -1)))
    return c.done(p)


def _earth_crest(c):
    """The crystal crown of the Gnome's helm: a stone ring with moss-gold trim and a ring of amber crystal prisms (a tall one at the front)."""
    P = c.pal
    p = c.piece("crest", skin=None)
    ring_band(p, "root", 32.2, 33.4, 4.7, 4.7, P["main"].with_(grooves=1), cell=1.5, thick=0.9)
    ring_band(p, "root", 32.0, 32.55, 4.78, 4.78, P["trim"], cell=1.0, thick=0.55)
    cap = Loft([(32.2, 4.7, 4.7, 4.7, 5), (33.4, 4.6, 4.6, 4.6, 5)])
    n = 9
    for k in range(n):
        th = -180 + k * 360.0 / n
        out = unit(V3((math.sin(math.radians(th)), 0.0, -math.cos(math.radians(th)))))
        front = max(0.0, math.cos(math.radians(th)))
        L = 3.2 + 3.6 * front ** 3 + 0.5 * (k % 2)
        base = cap.point(th, 33.2, 0.1)
        crystal(p, "root", base, V3((0, 1, 0)) + out * (0.28 + 0.1 * (k % 3)), L, 2.0 if front > 0.9 else 1.5, P["gem"], normal=out)
    for sx in (0, 1):                                          # a small crystal cluster at each temple
        crystal(p, "root", V(-4.9, 30.6, -0.6, sx), V(-0.7, 0.9, 0.0, sx), 3.8, 1.5, P["gem"], normal=V(0, 0, -1, sx))
        crystal(p, "root", V(-4.9, 30.2, 0.8, sx), V(-0.8, 0.5, 0.4, sx), 2.8, 1.2, P["gem"], normal=V(0, 0, -1, sx))
    return c.done(p)


# ====================================================================================================================== the plume and the mantle of Fuegoleon
def plume(c):
    """The mane of the Salamander as a PLUME OF PLATES: three chains of flame-shaped plates hinged on bones, streaming back from the crown over the nape, swaying (looped idle clip)."""
    P = c.pal
    p = c.piece("plume", skin=None)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    chains = [(0.0, 7, 0.0), (-2.6, 6, 0.35), (2.6, 6, 0.7)]
    for ci, (cx, n, ph) in enumerate(chains):
        parent = "root"
        y, z = 34.2 - 0.4 * abs(cx) / 2.6, -0.6 + 0.4 * abs(cx)
        for i in range(n):
            bn = "m%d_%d" % (ci, i)
            p.bone(bn, (cx, y, z), parent=parent)
            seg = 2.9 - 0.12 * i
            ang = math.radians(35 + i * 7.0)                    # arcs over the crown and falls down the back of the neck
            dy, dz = -math.sin(ang) * seg * 0.55, math.cos(ang) * seg
            a0, a1 = V3((cx, y, z)), V3((cx * 1.06, y + dy, z + dz))
            mat = P["main"] if (i + ci) % 2 == 0 else P["trim"]
            p.between(bn, a0, a1, 3.0 - 0.2 * i, 0.55, mat, normal=V3((0, 1, 0.2)))
            blade(p, bn, a1 - unit(a1 - a0) * 0.1, a1 - a0 + V3((cx * 0.05, -0.2, 0.2)), V3((0, 1, 0.3)), 2.0 + 0.1 * (i % 2), 2.2 - 0.12 * i, P["hot"].with_(taper=0.8, tip_top=1), segs=1, thick=0.4, tip=0.4)
            sine_rot(a, "idle", bn, 2.4, 0, 5.5 + 0.7 * i, phase=ph - i * 0.08)
            sine_rot(a, "idle", bn, 2.4, 1, 3.0 * (1 if cx >= 0 else -1), phase=ph * 0.5 - i * 0.05)
            parent = bn
            y, z = y + dy, z + dz
    return c.done(p, a)


def mantle(c):
    """The back mantle of the Salamander: five columns of overlapping flame-edged lames hanging from the shoulders, each column hinged and swaying a little."""
    P = c.pal
    p = c.piece("mantle", skin=None)
    a = Anim()
    a.clip("idle", length=2.8, loop=True)
    top = 23.4
    for i in range(5):
        cx = -4.0 + i * 2.0
        bn = "c%d" % i
        p.bone(bn, (cx, top, 2.9))
        for j in range(3):
            y1 = top - 0.2 - j * 3.9
            h = 4.6 - 0.3 * j
            mat = (P["main"] if (i + j) % 2 == 0 else P["alt"]).with_(lame=1, trim=0, wear=0.2, seed=P["main"].seed + 5 * i + j)
            flat_plate(p, bn, V3((cx, y1 - h / 2.0, 3.15 + 0.5 * j)), V3((1, 0, 0)), V3((0, 1, 0.1 + 0.08 * j)), 2.15, h, mat, thick=0.45, outward=V3((0, 0.05, 1)), rim="lrtb")
            if j == 2:
                blade(p, bn, V3((cx, y1 - h + 0.3, 3.5 + 0.5 * j)), V3((0, -1, 0.25)), V3((0, 0, 1)), 3.0, 2.0, P["hot"].with_(taper=0.8, tip_top=1), segs=2, thick=0.4, tip=0.15)
        sine_rot(a, "idle", bn, 2.8, 0, 3.0 + 0.8 * (i % 2), phase=i * 0.13)
    flat_plate(p, "root", V3((0, top + 0.4, 2.95)), V3((1, 0, 0)), V3((0, 1, 0)), 10.4, 1.2, P["trim"], thick=0.7, outward=V3((0, 0, 1)))
    return c.done(p, a)


# ====================================================================================================================== wings of plates
# the rows of one wing of each element: (count, length of the longest, width, offset toward the back) as in gen_erza_armour_extras.WINGS; fewer feathers than a Requip wing (the draw budget)
EX.WINGS["sylph"] = dict(rows=[(7, 18.0, 2.6, 0.0), (5, 11.0, 2.7, 0.55)], glass=False, zig=0.0, spar=True)
EX.WINGS["salamander"] = dict(rows=[(5, 18.0, 3.4, 0.0), (4, 11.0, 3.4, 0.55)], glass=False, zig=0.0, spar=True)
EX.WINGS["undine"] = dict(rows=[(5, 14.5, 4.6, 0.0), (4, 9.5, 4.4, 0.5)], glass=True, zig=0.0, spar=True)


def wing(c):
    style = {"wind": "sylph", "fire": "salamander", "water": "undine"}[c.el]
    EX.wing(c, style)


# ====================================================================================================================== the crystals of the Gnome
def backcrystals(c):
    """A stone slab with moss-gold edges on the back, and a cluster of amber crystal prisms rising from it past the shoulders (the heavy silhouette of the earth dive)."""
    P = c.pal
    p = c.piece("backcrystals", skin=None)
    flat_plate(p, "root", V3((0, 19.0, 2.95)), V3((1, 0, 0)), V3((0, 1, 0)), 7.6, 8.0, P["main"].with_(grooves=1), thick=1.0, outward=V3((0, 0, 1)), nu=3, nv=3)
    flat_plate(p, "root", V3((0, 23.2, 3.0)), V3((1, 0, 0)), V3((0, 1, 0)), 8.4, 0.9, P["trim"], thick=1.1, outward=V3((0, 0, 1)))
    flat_plate(p, "root", V3((0, 14.9, 3.0)), V3((1, 0, 0)), V3((0, 1, 0)), 6.6, 0.9, P["trim"], thick=1.1, outward=V3((0, 0, 1)))
    prisms = [(0.0, 21.5, 0.0, 12.0, 2.6), (-2.6, 21.0, -0.3, 9.0, 2.2), (2.6, 21.0, 0.3, 9.4, 2.2), (-4.4, 20.2, -0.7, 6.4, 1.8), (4.4, 20.2, 0.7, 6.8, 1.8), (-1.3, 22.0, 0.15, 8.0, 1.8),
              (1.3, 22.0, -0.15, 7.4, 1.7)]
    for k, (x, y, lean, L, w) in enumerate(prisms):
        crystal(p, "root", V3((x, y, 3.8)), V3((lean * 0.5 + x * 0.09, 1.0, 0.18)), L, w, P["gem"], normal=V3((0, 0, 1)))
    return c.done(p)


# ====================================================================================================================== dispatch
def build(c, name):
    el = c.el
    if name in ("crest_l", "crest_r"):
        return _laurel(c, 1 if name == "crest_l" else 0)
    if name == "crest_c":
        return _wind_crown(c)
    if name == "crest":
        return {"fire": _fire_crest, "water": _water_crest, "earth": _earth_crest}[el](c)
    if name == "plume":
        return plume(c)
    if name == "mantle":
        return mantle(c)
    if name == "wing":
        return wing(c)
    if name == "backcrystals":
        return backcrystals(c)
    raise KeyError(name)
