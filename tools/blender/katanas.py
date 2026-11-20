"""The katanas of the mod as REAL, THIN katanas, made in Blender (scripted bpy, no window) on the forms kit (forms_kit.py, forms_vassal_lib.py).

    python3 -B tools/blender/katanas.py --list
    python3 -B tools/blender/katanas.py --only hakurou,bokken [--trial DIR] [--fast] [--samples N] [--bake PX] [--threads N]

Why: the katanas were broadswords in the hand. The old script meshes were thin ribbons, and the 1.110.0 size rule (docs/visual_upgrade/weapon_sizes.md) thickened every
katana blade to 1/8 block (about 1/6 of its length); the Blender forms had blades 1/10 of their length. docs/heroweapon/katanas.md has the measurements.

What one katana is here (all numbers are shares of the mesh length L, the blade length Bl and the width at the habaki w0 = Bl / div):
  blade (nagasa)   0.70 .. 0.78 L, width w0 = Bl/18 at the habaki, tapering to 0.73 w0 at the yokote, then a kissaki (point) in the last 5 percent;
                   cross-section a flat diamond: a shinogi ridge at 62 percent of the width from the edge, the hira sloping to the edge, a roof-shaped mune (spine);
                   thickness 0.27 w0 at the habaki, 0.75 of that at the yokote; sori (back-to-chord depth) 2 to 3.5 percent of Bl, the tip bowed toward the edge (-x, the
                   repo's katana rule); a bo-hi groove when the lore wants one; a hamon painted along the edge (bake-only strips: free in game) with a glowing line when lit
  habaki           a gold collar, tsuba a thin plate (about 2.5 w0 across, 0.5 cm), seppa washers, fuchi collar
  tsuka            0.25 L, an oval grip over white ray skin with a DIAMOND lattice of ito (two opposite helices of cord), two same-size menuki, a kashira cap
  no saya          (these items are drawn drawn, not sheathed)
Variation between katanas = tsuba shape, wrap colours, hamon pattern, finish, sori within the real range, and one lore feature each; never a fatter blade.
The mesh keeps the old file's total length, hilt-at-low-y orientation and place line (orient manual), so the display transforms, class and hand still fit.
The old meshes and skins of every katana here are kept in weapon_meshes/classic_vassal and textures/entity/weapon/classic_vassal (docs/heroweapon/katanas.md).
"""
import math
import os
import random
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,F401
import bl_lib as bl  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
import forms_vassal_common as C  # noqa: E402

CLASSIC = "classic_vassal"
MESH_DIR = bl.MESH_DIR
SKIN_DIR = bl.SKIN_DIR
SMOOTH = 9.0            # degrees: normals are split above this angle, so the shinogi (a 10 degree crease) stays a crisp line
SIDES = 32              # lathe sides: 11 degrees a step, smooth under SMOOTH-free shading below 12


# ================================================================ the blade
def interp(pts, x):
    if x <= pts[0][0]:
        return pts[0][1]
    for (a, va), (b, vb) in zip(pts, pts[1:]):
        if x <= b:
            return va + (vb - va) * (x - a) / (b - a)
    return pts[-1][1]


class Ctx:
    """The measurements of one katana (cm) and the curves every part follows."""

    def __init__(self, S, y0, L):
        self.S, self.y0, self.L = S, y0, L
        self.ytip = y0 + L
        tsuka = S.get("tsuka", 0.25) * L
        self.kashira_h = 0.022 * L
        self.yt0 = y0 + 0.012 * L
        self.yt1 = y0 + tsuka                                   # the top of the wrap
        self.fuchi_h = 0.016 * L
        self.tsuba_y = self.yt1 + self.fuchi_h
        self.tsuba_t = max(0.6, 0.0048 * L)
        self.seppa_t = 0.0035 * L
        self.hab_y0 = self.tsuba_y + self.tsuba_t + 2 * self.seppa_t
        self.hab_h = 0.035 * L
        self.yh = self.hab_y0 + 0.45 * self.hab_h                 # the machi: where the blade proper begins
        self.Bl = self.ytip - self.yh
        self.div = S.get("div", 18.0)
        self.w0 = self.Bl / self.div
        self.kas = S.get("kasane", 0.27)
        self.th0 = 0.5 * self.kas * self.w0                      # half thickness at the habaki
        self.Kt = 4.0 * S.get("sori", 0.024) * self.Bl           # the tip's offset: the back's depth under the chord is Kt / 4
        self.kis = S.get("kissaki", 0.05)
        self.uk = 1.0 - self.kis
        self.taper = S.get("taper", 0.73)
        # the cross-section: positions across from the edge (-1) to the spine (+1) and the height share there (0 on the edge and the spine line)
        if S.get("bohi"):
            self.vs = (-1.0, -0.93, -0.45, 0.25, 0.40, 0.48, 0.80, 0.88, 0.94, 1.0)
            self.hs = (0.0, 0.10, 0.62, 1.0, 0.98, 0.62, 0.62, 0.98, 0.86, 0.0)
        elif S.get("round"):                                       # a bokken: an oval, no ridge
            self.vs = (-1.0, -0.86, -0.5, 0.0, 0.5, 0.86, 1.0)
            self.hs = (0.0, 0.62, 0.9, 1.0, 0.9, 0.62, 0.0)
        else:
            self.vs = (-1.0, -0.93, -0.45, 0.25, 0.94, 1.0)
            self.hs = (0.0, 0.10, 0.62, 1.0, 0.86, 0.0)

    # ---- curves of the blade, y in [yh, ytip]
    def u(self, y):
        return min(1.0, max(0.0, (y - self.yh) / self.Bl))

    def xc(self, y):
        return -self.Kt * self.u(y) ** 2

    def width(self, y):
        s = min(1.0, self.u(y) / self.uk)
        return self.w0 * (1.0 - (1.0 - self.taper) * s)

    def spine(self, y):
        u = self.u(y)
        if u <= self.uk:
            return self.xc(y) + 0.5 * self.width(y)
        return self.xc(y) + 0.5 * self.width(self.yh + self.uk * self.Bl)

    def edge(self, y):
        u = self.u(y)
        if u <= self.uk:
            return self.xc(y) - 0.5 * self.width(y)
        wk = self.width(self.yh + self.uk * self.Bl)
        v = (u - self.uk) / (1.0 - self.uk)
        return self.spine(y) - wk * (1.0 - v ** 1.8)

    def half(self, y):
        """Half thickness at the shinogi."""
        u = self.u(y)
        t = self.th0 * (1.0 - 0.25 * min(1.0, u / self.uk))
        if u > self.uk:
            v = (u - self.uk) / (1.0 - self.uk)
            t *= 1.0 - 0.5 * v ** 1.5
        return t

    def hs_at(self, a):
        """Height share at a (0 edge .. 1 spine)."""
        return interp(list(zip([(v + 1.0) * 0.5 for v in self.vs], self.hs)), a)

    def pt(self, y, a, s=1, lift=0.0):
        """A point on face s (+1 / -1 in z) at height y and a share a across the blade (0 edge .. 1 spine), `lift` cm above the surface."""
        e, sp = self.edge(y), self.spine(y)
        return V((e + (sp - e) * a, y, s * (self.half(y) * self.hs_at(a) + lift)))


def blade(c, mat, name="blade"):
    """The blade: rows of (edge, spine) points from the machi to the tip, a flat-diamond section with a shinogi."""
    ys = []
    n = max(18, int(c.Bl / 3.2))
    for i in range(n + 1):
        ys.append(c.yh + (c.uk * c.Bl) * i / n)
    kn = 8
    for j in range(1, kn + 1):
        v = j / kn
        ys.append(c.yh + c.uk * c.Bl + v * (1.0 - c.uk) * c.Bl)
    rows = [(V((c.edge(y), y, 0.0)), V((c.spine(y), y, 0.0))) for y in ys]
    # the very last row is a point: edge == spine; keep a hair of width so the ring is not degenerate
    hv = [c.half(y) for y in ys]
    K.strip(name, rows, mat, c.vs, c.hs, lambda t: hv[min(len(hv) - 1, int(round(t * (len(hv) - 1))))], part="blade", up=(0, 0, 1), angle=14)
    return ys


# ================================================================ the painted lines (bake-only strips: no quads in game)
def paint(c, name, ys, a0, a1, mat, lift=0.07, thick=0.04, part="blade_paint"):
    """A strip over both faces of the blade between two curves a0(y) < a1(y) (shares across, 0 edge .. 1 spine)."""
    for s in (1, -1):
        rows = [(c.pt(y, a0(y), s, lift), c.pt(y, a1(y), s, lift)) for y in ys]
        K.strip("%s%+d" % (name, s), rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=(0, 0, s), detail=True, angle=60)


def hamon_curve(kind, amp, period, seed, base=0.17):
    """The upper boundary of the hamon as a function of y (share across from the edge): suguha straight, notare a slow wave, gunome round teeth, midare irregular, choji clove bulbs."""
    r = random.Random(seed)
    ph = [r.uniform(0, TAU) for _ in range(4)]

    def f(y):
        if kind == "suguha":
            return base + 0.0 * y
        if kind == "notare":
            return base + amp * math.sin(TAU * y / period + ph[0])
        if kind == "gunome":
            return base + amp * (abs(math.sin(math.pi * y / period + ph[0])) ** 0.7 - 0.5)
        if kind == "choji":
            return base + amp * (abs(math.sin(math.pi * y / period + ph[0])) ** 0.55 * (0.8 + 0.2 * math.sin(y / period * 2.3 + ph[1])) - 0.5)
        return base + amp * (0.55 * math.sin(TAU * y / period + ph[0]) + 0.3 * math.sin(TAU * y / (period * 0.43) + ph[1]) + 0.2 * math.sin(TAU * y / (period * 0.21) + ph[2]))   # midare
    return f


def hamon(c, ys, P, kind="notare", amp=0.09, period=None, seed=1, base=0.17, line=None, start=0.06):
    """Hamon (the bright temper band along the edge, upper boundary wavy, it runs round the kissaki as the boshi) and, when `line` is a material, a thin glowing line on its boundary."""
    period = period or c.Bl / 9.0
    f = hamon_curve(kind, amp, period, seed, base)
    ysb = [y for y in ys if y >= c.yh + start * c.Bl]
    # a tighter sampling so the teeth are drawn well
    n = max(40, int(c.Bl / 1.4))
    ysb = [c.yh + start * c.Bl + (c.ytip - (c.yh + start * c.Bl)) * i / n for i in range(n + 1)]

    def top(y):
        u = c.u(y)
        t = f(y)
        if u > c.uk:                                            # the boshi: the band follows the point and closes
            v = (u - c.uk) / (1.0 - c.uk)
            t = t * (1.0 - v) + 0.02 * v
        fade = min(1.0, (u - start) / 0.03) if start > 0 else 1.0
        return max(0.03, t * fade + 0.03 * (1 - fade))

    def low(y):
        u = c.u(y)
        if u > c.uk:
            return 0.012 + 0.0 * y
        return 0.014 + 0.0 * y
    ysb = [y for y in ysb if y < c.ytip - 0.02 * c.Bl * (1 - 0.0)]
    paint(c, "hamon", ysb, low, top, P["hamon"], lift=0.06, part="hamon")
    if line is not None:
        paint(c, "hamon_line", ysb, lambda y: top(y) - 0.010, lambda y: top(y) + 0.012, line, lift=0.09, thick=0.03, part="hamon")
    return top


# ================================================================ the fittings
def helix(name, y0, y1, rx, rz, turns, hand, mat, cord, phase=0.0, sub=9, lift=0.0, part="grip_ito", seed=0):
    """One hand of the ito lattice: a cord wound `turns` times (hand +1 / -1) round an oval grip of half widths rx, rz."""
    rr = random.Random(seed)
    n = int(turns * sub)
    path = []
    for i in range(n + 1):
        t = i / n
        a = hand * TAU * turns * t + phase
        wob = 1.0 + 0.012 * math.sin(a * 3.0) + 0.008 * rr.uniform(-1, 1)
        path.append(V(((rx + lift) * wob * math.cos(a), y0 + (y1 - y0) * t, (rz + lift) * wob * math.sin(a))))
    sec = [(-0.04, -cord), (0.07, -cord * 0.92), (0.12, 0.0), (0.07, cord * 0.92), (-0.04, cord)]
    ob = bl.sweep(name, path, sec, mat, up=(0, 1, 0), part=part, caps=(True, True))
    return K._finish_obj(ob, part, True, 55.0)


def tsuka(c, P, wrap2=None, turns=7.0, menuki=True):
    """The grip: a white ray-skin core (oval), a diamond lattice of ito (two opposite helices), a fuchi collar at the top, a kashira cap at the bottom."""
    S = c.S
    r0 = 0.56 * c.w0
    sq = S.get("grip_sq", 0.80)
    n = 9
    prof = [(r0 * (1.0 + 0.05 * math.sin(math.pi * i / (n - 1))), c.yt0 + (c.yt1 - c.yt0) * i / (n - 1)) for i in range(n)]
    Lb.lathe("grip_core", prof, P["ray"], "grip", sides=SIDES, squash=sq)
    cord = S.get("cord", 0.15) * c.w0
    rmid = r0 * 1.05
    helix("ito_a", c.yt0 + 0.3, c.yt1 - 0.3, rmid, rmid * sq, turns, 1, P["ito"], cord, seed=3)
    helix("ito_b", c.yt0 + 0.3, c.yt1 - 0.3, rmid, rmid * sq, turns, -1, wrap2 or P["ito"], cord, phase=0.9, lift=0.05, seed=4)
    # menuki: two same-sized ornaments under the wrap, one on each face
    ym = c.yt0 + 0.56 * (c.yt1 - c.yt0)
    for s in ((1, -1) if menuki else ()):
        K._finish_obj(bl.gem("menuki%+d" % s, 0.5, P["menuki"], center=(0, ym, s * (rmid * sq + 0.12)), depth=0.35, crown=0.3, sides=6, axis="z", rx=0.3 * c.w0, ry=0.62 * c.w0, part="menuki"), "menuki", True, 40.0)
    # fuchi (collar over the top of the grip) and kashira (cap on the bottom)
    rf = r0 * 1.16
    Lb.lathe("fuchi", [(rf * 0.9, c.yt1 - 0.4), (rf, c.yt1 + 0.0), (rf * 1.05, c.yt1 + c.fuchi_h * 0.5), (rf, c.yt1 + c.fuchi_h)], P["fit"], "fuchi", sides=SIDES, squash=sq)
    h = c.kashira_h
    rk = r0 * 1.1
    Lb.lathe("kashira", [(0.02, c.y0), (rk * 0.62, c.y0 + 0.04 * h), (rk * 0.95, c.y0 + 0.4 * h), (rk * 1.08, c.y0 + 0.75 * h), (rk, c.yt0 + 0.3), (rk * 0.9, c.yt0 + 0.6)], P["kash"], "kashira", sides=SIDES, squash=sq)


def outline(shape, R, rr, n=40):
    """The outline [(x, z)] of a tsuba: round, mokko (a rounded square with four lobes), aoi (eight round lobes: a cloud / leaf), sun (round with a notched rim), thorn (spiked), cross."""
    pts = []
    for i in range(n):
        a = TAU * i / n
        if shape == "mokko":
            r = R * (0.80 + 0.20 * abs(math.cos(2 * a)) ** 0.8) * 1.04
        elif shape == "aoi":
            r = R * (0.78 + 0.22 * abs(math.sin(4 * a)) ** 0.6)
        elif shape == "cloud":
            r = R * (0.80 + 0.20 * abs(math.sin(3 * a + 0.6)) ** 0.5)
        elif shape == "sun":
            r = R * (1.0 - 0.07 * (0.5 + 0.5 * math.cos(12 * a)) ** 2)
        elif shape == "thorn":
            r = R * (0.80 + 0.30 * max(0.0, math.cos(5 * a)) ** 3)
        elif shape == "cross":
            r = R * (0.62 + 0.38 * abs(math.cos(2 * a)) ** 2.2)
        elif shape == "oval":
            r = R
        else:
            r = R
        pts.append((r * math.cos(a), r * math.sin(a) * rr))
    return pts


def tsuba(c, P, shape="round", R=None, rr=0.92, steps=3, rim=True):
    """A thin tsuba plate (2.5 w0 across), layers stepped in 0.5 cm total; seppa washers either side."""
    R = R or 1.25 * c.w0
    pts = outline(shape, R, rr)
    st = c.tsuba_t / steps
    obs = K.stepped_plate("tsuba", pts, 0.0, P["tsuba"], steps=steps, step_h=st, inset=0.06 * R, part="guard", bevel_w=0.04)
    for o in obs:
        K.xf(o, K.frame((0, c.tsuba_y, 0), (0, 1, 0), (0, 0, 1)))
    ys = c.tsuba_y
    for k, (y0, y1) in enumerate(((ys - c.seppa_t, ys), (ys + c.tsuba_t, ys + c.tsuba_t + c.seppa_t))):
        Lb.lathe("seppa%d" % k, [(0.84 * c.w0, y0), (0.84 * c.w0, y1)], P["fit"], "guard_collar", sides=SIDES, squash=0.78)
        # (a lathe with two equal rings is a plain washer)
    return obs


def habaki(c, P):
    """The gold collar over the blade's root: a rounded section a little wider and thicker than the blade, narrowing up the blade."""
    rings = []
    for y, k in ((c.hab_y0, 1.0), (c.hab_y0 + 0.18 * c.hab_h, 1.12), (c.hab_y0 + 0.8 * c.hab_h, 1.1), (c.hab_y0 + c.hab_h, 0.98)):
        w, t = 0.5 * c.w0 * 1.1 * k, c.th0 * 1.25 * k
        rings.append(bl.ring(y, bl.rrect(w, t, min(w, t) * 0.55, 2), ox=c.xc(y)))
    K._loft("habaki", rings, P["fit"], (True, True), "habaki")


# ================================================================ palette
def mats(S):
    """The materials every katana uses, from the spec's colours (rgb tuples 0..255). Anything the spec leaves out is a plain default."""
    p = S["pal"]
    seed = S.get("seed", 1) * 10
    M = {}
    M["steel"] = Lb.metal("k_steel", rgb(*p["steel"]), rgb(*p.get("steel_wear", (235, 240, 250))), seed + 1, rough=p.get("rough", 0.24), mottle=rgb(*p["mottle"]) if "mottle" in p else None,
                          dirt=p.get("dirt", 0.3), dirt_col=(0.02, 0.02, 0.03, 1), bump=p.get("bump", "brushed"), bs=p.get("bump_s", 0.06),
                          **({"cramp": ("y", p["cramp"][0], p["cramp"][1], [(a, rgb(*b)) for a, b in p["cramp"][2]])} if "cramp" in p else {}))
    M["hamon"] = K.surface("k_hamon", rgb(*p.get("hamon", (240, 244, 252))), 1.0, p.get("hamon_rough", 0.1), seed=seed + 2, bevel=0.0, mottle=0.4,
                           mottle_color=rgb(*p.get("hamon2", (205, 214, 232))), **({"emit": rgb(*p["hamon_emit"]), "emit_strength": p.get("hamon_glow", 0.0)} if "hamon_emit" in p else {}))
    M["line"] = Lb.glow("k_line", rgb(*p["line"]), p.get("line_s", 1.4), seed + 3, flicker=0.35) if "line" in p else None
    M["fit"] = Lb.hammered("k_fit", rgb(*p.get("fit", (226, 178, 66))), rgb(*p.get("fit_wear", (255, 244, 200))), seed + 4, mottle=rgb(*p.get("fit2", (160, 100, 30))))
    M["tsuba"] = Lb.metal("k_tsuba", rgb(*p.get("tsuba", (38, 38, 46))), rgb(*p.get("tsuba_wear", (120, 124, 140))), seed + 5, rough=0.4, dirt=0.5, bump="hammer", bs=0.1)
    M["kash"] = Lb.metal("k_kash", rgb(*p.get("kash", p.get("fit", (226, 178, 66)))), rgb(*p.get("fit_wear", (255, 244, 200))), seed + 6, rough=0.34, dirt=0.4)
    M["ray"] = Lb.paint("k_ray", rgb(*p.get("ray", (232, 228, 216))), rgb(255, 255, 250), seed + 7, rough=0.8, mottle=rgb(190, 184, 168), bump="leather")
    M["ito"] = Lb.cloth("k_ito", rgb(*p["ito"]), rgb(*p.get("ito_groove", tuple(int(v * 0.4) for v in p["ito"]))), seed + 8, mottle=rgb(*p["ito"]), bump="wrap", scale=1.0)
    M["ito2"] = Lb.cloth("k_ito2", rgb(*p.get("ito2", p["ito"])), rgb(*p.get("ito2_groove", tuple(int(v * 0.4) for v in p.get("ito2", p["ito"])))), seed + 9, mottle=rgb(*p.get("ito2", p["ito"])), bump="wrap", scale=1.0)
    M["menuki"] = Lb.hammered("k_menuki", rgb(*p.get("menuki", p.get("fit", (226, 178, 66)))), rgb(255, 244, 200), seed + 10)
    M["accent"] = Lb.glow("k_accent", rgb(*p.get("accent", (255, 255, 255))), p.get("accent_s", 1.0), seed + 11, flicker=0.4)
    M["accent_dark"] = Lb.paint("k_accent_dark", rgb(*p.get("accent_dark", (20, 20, 24))), rgb(90, 90, 100), seed + 12, rough=0.35)
    M["extra"] = Lb.metal("k_extra", rgb(*p.get("extra", p.get("tsuba", (38, 38, 46)))), rgb(190, 190, 200), seed + 13, rough=0.35, dirt=0.4)
    for k, v in S.get("extra_mats", {}).items():
        M[k] = v(seed + 20)
    return M


# ================================================================ one katana
def build(S, opts):
    C.begin(opts)
    C.weights(blade=1.7, hamon=1.4, hamon_line=1.0, blade_paint=1.0, grip=1.0, grip_ito=1.0, guard=0.8, habaki=0.8, fuchi=0.7, kashira=0.8, menuki=0.6, grip_core=1.0, tassel=0.6, spine_teeth=0.6, hook=0.6, void_ring=0.5)
    src = os.path.join(MESH_DIR, CLASSIC, S["id"] + ".txt")
    ys_, ymin, ymax = old_extent(src)
    y0, L = ymin + S.get("tail", 0.0), ymax - ymin - S.get("tail", 0.0)      # a hanging charm / hook (`tail` cm) keeps the old total length
    c = Ctx(S, y0, L)
    P = mats(S)
    ys = blade(c, P[S.get("blade_mat", "steel")])
    h = S.get("hamon", ("notare", 0.08, None))
    if h:                                                       # None: no hamon (a wooden bokken)
        period = h[2] if not h[2] or h[2] > 0 else c.Bl / -h[2]    # a negative period = that many waves along the blade
        hamon(c, ys, P, h[0], h[1], period, seed=S.get("seed", 1), line=P["line"], base=S.get("hamon_base", 0.17))
    if not S.get("nohabaki"):
        habaki(c, P)
    tsuba(c, P, *S.get("tsuba", ("round",)))
    tsuka(c, P, wrap2=P["ito2"] if S["pal"].get("ito2") else None, turns=S.get("turns", 7.0), menuki=not S.get("nomenuki"))
    if S.get("extras"):
        S["extras"](c, P, K.rng(S.get("seed", 1) + 7))
    print("[%s] L=%.1f cm  blade %.1f (%.2f L)  w0=%.2f (1/%.1f)  thick %.2f  sori %.1f cm  quads ~%d" % (S["id"], L, c.Bl, c.Bl / L, c.w0, c.Bl / c.w0, 2 * c.th0, c.Kt / 4, K.quad_estimate()))
    return c


def old_extent(path):
    """y range of the body and glow layers of a mesh file."""
    lo, hi = 1e9, -1e9
    for line in open(path):
        v = line.split()
        if v and v[0] in ("q", "qn") and int(v[1]) in (0, 2):
            st = 5 if v[0] == "q" else 8
            for k in range(4):
                y = float(v[2 + k * st + 1])
                lo, hi = min(lo, y), max(hi, y)
    return None, lo, hi


def finish(S, c, trial=None, bake=2048, skin=1024):
    """Bake and write; then give the mesh the old place line (same hand and size) with the real lowest y, the grip line of the tsuka and the glow header of the old file."""
    names = []
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail") and o["part"] not in names:
            names.append(o["part"])
    C.PART_NAMES[0] = names
    mesh_dir = os.path.join(trial, "weapon_meshes") if trial else MESH_DIR
    skin_dir = os.path.join(trial, "textures", "entity", "weapon") if trial else SKIN_DIR
    os.makedirs(mesh_dir, exist_ok=True)
    os.makedirs(skin_dir, exist_ok=True)
    hdr = os.path.join(MESH_DIR, CLASSIC, S["id"] + ".txt")
    gl = old_glow(hdr)
    real = K.bl.bpy
    K.bl.bpy = C._BpyProxy()
    try:
        mesh, atlas, stats = bl.finish(S["id"], glow=(gl[0], gl[1], gl[2], gl[3]), header_from=hdr, bake=bake, skin=skin, out_mesh=mesh_dir, out_skin=skin_dir, preview=None, write=True,
                                       budget=10 ** 6, cage=0.5, smooth=SMOOTH)
    finally:
        K.bl.bpy = real
    K.shrink_pngs(skin_dir, S["id"])
    patch_header(os.path.join(mesh_dir, S["id"] + ".txt"), c, hdr)
    return K.report(S["id"], S["id"], mesh_dir, skin_dir, mesh, atlas, time.time() - K._T0[0])


def old_glow(path):
    """The glow header of the old file (tint colour, pulse speed, min, max): kept as it was."""
    for line in open(path):
        v = line.split()
        if v and v[0] == "glow":
            return v[1], float(v[2]), float(v[3]), float(v[4])
    return "FFFFFF", 0.14, 0.45, 1.0


def patch_header(path, c, old):
    """place: the old origin, angle and scale with the lowest y of the new mesh; grip: the middle of the tsuka as a share of the length; orient manual."""
    lines = open(path).read().split("\n")
    lo, hi = 1e9, -1e9
    for l in lines:
        v = l.split()
        if v and v[0] in ("q", "qn"):
            st = 5 if v[0] == "q" else 8
            for k in range(4):
                y = float(v[2 + k * st + 1])
                lo, hi = min(lo, y), max(hi, y)
    op = next(l.split() for l in open(old) if l.startswith("place "))
    out = []
    for l in lines:
        v = l.split()
        if v and v[0] == "place":
            l = "place %s %s %s %s %.3f" % (op[1], op[2], op[3], op[4], lo)
        elif v and v[0] == "grip":
            l = "grip %.3f" % ((0.5 * (c.yt0 + c.yt1) - lo) / (hi - lo))
        out.append(l)
    open(path, "w").write("\n".join(out))


# ================================================================ run
def main(argv):
    import argparse
    from katanas_specs import SPECS
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(SPECS))
    ap.add_argument("--trial")
    ap.add_argument("--fast", action="store_true")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--bake", type=int, default=2048)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--list", action="store_true")
    a = ap.parse_args(argv)
    if a.list:
        print("\n".join(SPECS))
        return
    C.THREADS[0] = max(1, min(4, a.threads))
    for key in a.only.split(","):
        S = SPECS[key]
        opts = {"samples": 3 if a.fast else a.samples}
        c = build(S, opts)
        finish(S, c, trial=a.trial, bake=256 if a.fast else a.bake, skin=256 if a.fast else 1024)


if __name__ == "__main__":
    main(sys.argv[1:])
