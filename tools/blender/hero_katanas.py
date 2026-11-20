"""The HERO katanas as TRUE katanas, made in Blender (scripted bpy, headless, no window) on the katana core of katanas.py (docs/heroweapon/hero_katanas.md).

    python3 -B tools/blender/hero_katanas.py --keep                         copy the 4.61.0 (thinned) meshes and skins into the classic_thin folders (rule 2); idempotent, run it FIRST
    python3 -B tools/blender/hero_katanas.py --list
    python3 -B tools/blender/hero_katanas.py --only tiger,soul [--trial DIR] [--fast] [--samples N] [--bake PX] [--threads N]
    python3 -B tools/blender/hero_katanas.py --only hellblaze [...]         (hero_hellblaze.py: the Hellblaze katana, its lod and its saya)

Why: 4.61.0 only THINNED the blades of the hero katana forms (parameter), so they still read as fantasy blades: wings, ribs, petals, a crown and a star guard as wide as the blade is long.
Here each one is rebuilt as a katana first and the lore is put ON that silhouette, not round it:
  blade    single edge, shinogi ridge, mune, yokote and kissaki, sori, a painted hamon band (the temper line, glowing where the lore wants light), width w0 = blade / 17..20
  tsuba    a real thin tsuba (<= 1/14 of the whole mesh across), cut with sukashi windows in the shape of the lore (claw slits, ribs, star, petals, feathers) and a rim
  habaki, fuchi, tsuka with a diamond ito lattice over ray skin, a kashira and a tassel; two menuki icons of the lore (paw, skull, star, blossom, feather) on the flats of the grip
  lore     engraved / inlaid in the blade (bake-only paint: zero quads) and small crests, never outline: tiger stripes, rib arcs, star flecks, petal inlays, wind swirls
The mesh keeps the 4.61.0 file's place line, glow header and total length (the header and extent are read from weapon_meshes/classic_thin), so every display transform, class and grip still fit.
The meshes before this change are in weapon_meshes/classic_thin (4.61.0) and weapon_meshes/classic_vassal (before 4.61.0); nothing is deleted.
"""
import math
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

THIN = "classic_thin"
IDS = ["vassal_katana__white_tiger_katana", "vassal_katana__soul_eater_katana", "vassal_katana__siderite_katana", "vassal_katana__sakura_stone_katana", "legendary_sword__wind_king_katana"]
HELLBLAZE = ["hellblaze_the_demon_sin__katana", "hellblaze_the_demon_sin__katana_lod", "hellblaze_the_demon_sin__saya"]


# ================================================================ keep the 4.61.0 files (pure python, no bpy)
def keep():
    ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
    A = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
    M, S = os.path.join(A, "weapon_meshes"), os.path.join(A, "textures", "entity", "weapon")

    def cp(src, dst):
        if os.path.exists(src) and not os.path.exists(dst):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(src, dst)
            print("copied", os.path.relpath(dst, ROOT))
    for i in IDS:
        cp(os.path.join(M, i + ".txt"), os.path.join(M, THIN, i + ".txt"))
        for suf in ("", "_glow"):
            cp(os.path.join(S, i + suf + ".png"), os.path.join(S, THIN, i + suf + ".png"))
    for i in HELLBLAZE:
        cp(os.path.join(M, "up", i + ".txt"), os.path.join(M, "up", THIN, i + ".txt"))
        for suf in ("", "_glow", "_veins"):
            cp(os.path.join(S, "up", i + suf + ".png"), os.path.join(S, "up", THIN, i + suf + ".png"))


if "--keep" in sys.argv:
    keep()
    sys.exit(0)

import bpy  # noqa: E402,F401
import bl_lib as bl  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
import forms_vassal_common as C  # noqa: E402
import katanas as KT  # noqa: E402
import katanas_specs as KS  # noqa: E402

KT.CLASSIC = THIN                      # katanas.build / finish read the old extent, glow and header of an id from this folder: the 4.61.0 mesh


# ================================================================ painted inlay on the blade (bake-only strips: no quads in game)
def ribbon_on(c, name, path, wfun, mat, s, lift=0.07, thick=0.04, part="blade_paint"):
    """A ribbon lying on face s of the blade along path [(y cm, a share)], half width wfun(t) cm. Both edges are put ON the surface (through c.pt), so it follows the diamond section."""
    n = len(path)
    rows = []
    for k, (y, a) in enumerate(path):
        y0, a0 = path[max(k - 1, 0)]
        y1, a1 = path[min(k + 1, n - 1)]
        wd = c.width(y)
        tx, ty = (a1 - a0) * wd, (y1 - y0)
        ln = math.hypot(tx, ty) or 1.0
        nx, ny = -ty / ln, tx / ln
        w = wfun(k / max(1, n - 1))

        def at(sg):
            yy = min(c.ytip - 0.05, max(c.yh + 0.05, y + sg * w * ny))
            aa = min(0.985, max(0.015, a + sg * w * nx / wd))
            return c.pt(yy, aa, s, lift)
        rows.append((at(-1), at(1)))
    K.strip("%s%+d" % (name, s), rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=(0, 0, s), detail=True, angle=60)


def grip_dims(c):
    r0 = 0.56 * c.w0
    sq = c.S.get("grip_sq", 0.80)
    return r0, sq, r0 * 1.05 * sq, c.yt0 + 0.56 * (c.yt1 - c.yt0)       # half width, squash, half thickness at the wrap, y of the menuki


def mats_extra(*items):
    """{name: fn(seed)} for the spec's extra_mats."""
    return dict(items)


# ================================================================ the tsuba with sukashi windows (a real thin plate, cut in the lore's shape)
def petal_prof(t):
    return math.sin(math.pi * t) ** 0.8


def slit_prof(t):
    return min(1.0, math.sin(math.pi * t) ** 0.3) * 0.55


def leaf_prof(t):
    return math.sin(math.pi * min(1.0, t ** 0.85)) ** 0.9


def diamond_prof(t):
    return 1.0 - abs(2.0 * t - 1.0)


TSUBA = {
    # name: n windows, window profile, inner / outer radius of the windows (share of R), outer outline r(a)/R, arc samples per sector, window width (share of the sector), phase
    "claw": dict(n=6, prof=slit_prof, rin=0.54, rout=0.86, outer=lambda a, n: 1.0 - 0.07 * (0.5 + 0.5 * math.cos(5 * a)), m=4, dm=0.55),            # white tiger: claw slits in a paw-round plate
    "ribs": dict(n=7, prof=slit_prof, rin=0.54, rout=0.86, outer=lambda a, n: 0.92 + 0.08 * abs(math.cos(3.5 * a)) ** 0.5, m=4, dm=0.62),           # soul eater: a rib cage
    "star": dict(n=8, prof=diamond_prof, rin=0.52, rout=0.76, outer=lambda a, n: 0.72 + 0.28 * abs(math.cos(n * a / 2.0)) ** 1.2, m=2, dm=0.78),    # siderite: an eight-point star, valley / tip
    "petal": dict(n=5, prof=petal_prof, rin=0.54, rout=0.86, outer=lambda a, n: 0.90 + 0.10 * abs(math.cos(n * a / 2.0)) ** 0.8, m=4, dm=0.74),      # sakura: five petal windows
    "feather": dict(n=4, prof=leaf_prof, rin=0.54, rout=0.86, outer=lambda a, n: 0.88 + 0.12 * abs(math.sin(4 * a)) ** 0.7, m=6, dm=0.70),          # wind king: four feathers
    "thorn": dict(n=8, prof=slit_prof, rin=0.52, rout=0.80, outer=lambda a, n: 0.70 + 0.30 * abs(math.cos(4 * a)) ** 2.6, m=4, dm=0.50),           # hellblaze: the ring of thorns
}


def polar(r, a):
    return (r * math.cos(a), r * math.sin(a))


def hero_tsuba(c, P, shape, R=None, rr=1.0, steps=1, lite=False):
    """A tsuba cut into n sectors round a hub (a plate each, so the sukashi windows are real holes), a boss on the hub, seppa washers either side. Thickness c.tsuba_t."""
    cfg = TSUBA[shape]
    R = R or 1.08 * c.w0
    n, m = cfg["n"], (2 if lite else cfg["m"])
    half = math.pi / n
    rin, rout = cfg["rin"] * R, cfg["rout"] * R
    ts = (0.0, 0.25, 0.5, 0.75, 1.0)
    obs = []
    for i in range(n):
        a0, a1 = TAU * i / n, TAU * (i + 1) / n
        pts = [(0.0, 0.0)]
        for t in ts:
            pts.append(polar(rin + (rout - rin) * t, a0 + cfg["dm"] * half * cfg["prof"](t)))
        pts.append(polar(R * cfg["outer"](a0, n), a0))
        for k in range(1, m):
            a = a0 + (a1 - a0) * k / m
            pts.append(polar(R * cfg["outer"](a, n), a))
        pts.append(polar(R * cfg["outer"](a1, n), a1))
        for t in reversed(ts):
            pts.append(polar(rin + (rout - rin) * t, a1 - cfg["dm"] * half * cfg["prof"](t)))
        pts = [(x, y * rr) for x, y in pts]
        obs += [K.plate("tsuba%d" % i, pts, 0.0, c.tsuba_t, P["tsuba"], part="guard", bevel_w=0.0 if lite else 0.03)]
    for o in obs:
        K.xf(o, K.frame((0, c.tsuba_y, 0), (0, 1, 0), (0, 0, 1)))
    ys = c.tsuba_y
    Lb.lathe("hub", [(0.01, ys - 0.05), (rin * 0.96, ys - 0.05), (rin * 0.86, ys + c.tsuba_t + 0.16), (0.01, ys + c.tsuba_t + 0.2)], P["fit"], "guard_collar", sides=12 if lite else 24, squash=0.9)
    for k, (y0, y1) in enumerate(((ys - c.seppa_t, ys), (ys + c.tsuba_t, ys + c.tsuba_t + c.seppa_t))):
        Lb.lathe("seppa%d" % k, [(0.50 * c.w0, y0), (0.50 * c.w0, y1)], P["fit"], "guard_collar", sides=KT.SIDES, squash=0.78)
    return obs


_orig_tsuba = KT.tsuba


def _tsuba(c, P, shape="round", R=None, rr=0.92, steps=3, rim=True):
    if shape in TSUBA:
        return hero_tsuba(c, P, shape, R, 1.0, steps)
    return _orig_tsuba(c, P, shape, R, rr, steps, rim)


KT.tsuba = _tsuba


# ================================================================ 1. WHITE TIGER KATANA
def tiger_menuki(c, P, rng):
    """A gold tiger paw (a pad and four toes) on each flat of the grip."""
    r0, sq, zf, ym = grip_dims(c)
    for s in (1, -1):
        z = s * (zf + 0.05)
        K.blob("pad%+d" % s, (0.0, ym - 0.35, z), (0.80, 0.66, 0.30), P["menuki"], seed=3, lump=0.04, rings=4, segs=7, part="menuki")
        for j, dx in enumerate((-0.92, -0.32, 0.32, 0.92)):
            K.blob("toe%+d_%d" % (s, j), (dx, ym + 0.62 + 0.16 * (1 - abs(dx)), z), (0.27, 0.34, 0.22), P["menuki"], seed=4 + j, lump=0.04, rings=4, segs=6, part="menuki")


def tiger_extras(c, P, rng):
    # tiger stripes: black bands slanting from the spine across the hira, thick at the spine, thinning to a point toward the edge
    for s in (1, -1):
        for k in range(11):
            yk = c.yh + c.Bl * (0.07 + 0.80 * k / 10) + rng.uniform(-1.0, 1.0)
            a_sp, a_end = rng.uniform(0.90, 0.97), rng.uniform(0.38, 0.50)
            slant = rng.uniform(2.0, 3.8)
            wmax = rng.uniform(0.55, 0.85)
            path = [(yk + slant * (i / 6.0) ** 2, a_sp + (a_end - a_sp) * i / 6.0) for i in range(7)]
            ribbon_on(c, "stripe%d" % k, path, lambda t, w=wmax: w * (1.0 - t ** 1.6) + 0.05, P["accent_dark"], s)
    # the phantom second blade (Double Sword): a second glowing line beside the groove
    KS.grooves(c, P, P["line"])
    ys = [c.yh + c.Bl * (0.12 + 0.70 * i / 24) for i in range(25)]
    KT.paint(c, "ghost_line", ys, lambda y: 0.585, lambda y: 0.615, P["accent"], lift=0.05, thick=0.03, part="blade_paint")
    tiger_menuki(c, P, rng)
    # kashira: an amber tiger eye; the striped tail with a tuft of fur
    K.gem_setting("pommelgem", V((0, c.y0 - 0.15, 0.0)), 1.05, P["eye"], P["fit"], normal=(0, -1, 0), claws=4, sides=6, part="kashira_gem", rng_=rng, up=(0, 0, 1), claw_r=0.3)
    top = V((0.0, c.y0 - 1.2, 0.0))
    tp = [top, top + V((0.9, -5.0, 0.7)), top + V((3.2, -10.5, 1.2)), top + V((4.3, -16.0, 0.2)), top + V((3.4, -21.0, -1.0)), top + V((2.4, -24.0, -0.8))]
    K.tapered("tail", tp, 0.85, 0.5, P["tail"], sides=6, sub=3, part="tassel", bulge=lambda t: 1.0 + 0.18 * math.sin(t * 9.0))
    Lb.fur("tailtuft", [tp[-1]] * 6, [V((math.sin(k * 1.1), -1.0, math.cos(k * 1.1) * 0.6)) for k in range(6)], 3.2, 0.42, P["fur"], rng, part="tassel", tip_mat=P["fur_dark"])


def tiger_mats():
    return dict(
        tail=lambda seed: K.surface("tiger_tail", rgb(236, 232, 222), 0.0, 0.8, bump="rings", bump_scale=1.5, bump_strength=0.9, groove=rgb(14, 14, 18), groove_amount=1.0, dirt=0.5, mottle=0.6,
                                    mottle_color=rgb(190, 180, 165), fine=0.6, seed=seed, bevel=0.0),
        fur=lambda seed: Lb.paint("tiger_fur", rgb(236, 232, 220), rgb(255, 255, 250), seed, rough=0.8, dirt_col=(0.05, 0.045, 0.04, 1), mottle=rgb(190, 180, 160), bump="leather"),
        fur_dark=lambda seed: Lb.paint("tiger_fur_dark", rgb(30, 28, 32), rgb(120, 118, 124), seed, rough=0.8, bump="leather"),
        eye=lambda seed: Lb.gem("tiger_eye", rgb(255, 176, 30), 1.0, seed))


# ================================================================ 2. SOUL EATER KATANA
def soul_extras(c, P, rng):
    # the rib cage as inlay: bone arcs wrapping the blade from the spine toward the edge on both faces, with a vertebra bead at each root
    for s in (1, -1):
        for k in range(7):
            yk = c.yh + c.Bl * (0.10 + 0.74 * k / 6)
            ln = 7.5 - 0.5 * k
            path = []
            for i in range(8):
                t = i / 7.0
                path.append((yk + ln * 0.55 * math.sin(t * math.pi * 0.62), 0.94 - 0.50 * t))
            ribbon_on(c, "rib%d" % k, path, lambda t, w=0.46: w * (1.0 - 0.45 * t), P["bone"], s)
            KS.diamond("vert%d%+d" % (k, s), (c.pt(yk, 0.95, s, 0.08).x, yk, c.pt(yk, 0.95, s, 0.08).z), 0.42, P["bone"], s, stretch=1.0)
    KS.grooves(c, P, P["line"])
    # soul flames licking up the edge: thin glowing tongues inlaid near the edge
    for i in range(7):
        s = 1 if i % 2 else -1
        y = c.yh + c.Bl * (0.14 + 0.72 * i / 6)
        p = c.pt(y, 0.26, s, 0.07)
        KS.diamond("flame%d" % i, (p.x, p.y, p.z), c.w0 * 0.10, P["accent"], s, stretch=5.5 + (i % 3))
    # menuki: a skull with glowing sockets on each flat
    r0, sq, zf, ym = grip_dims(c)
    for s in (1, -1):
        z = s * (zf + 0.05)
        K.blob("skull%+d" % s, (0.0, ym + 0.1, z), (0.78, 0.86, 0.34), P["ray"], seed=5, lump=0.05, rings=5, segs=8, part="menuki")
        K.blob("jaw%+d" % s, (0.0, ym - 0.78, z), (0.48, 0.30, 0.26), P["ray"], seed=6, lump=0.05, rings=4, segs=6, part="menuki")
        for ex in (-1, 1):
            K.blob("socket%+d_%d" % (s, ex), (ex * 0.30, ym + 0.2, z + s * 0.2), (0.20, 0.22, 0.12), P["accent"], seed=7, lump=0.02, rings=3, segs=5, part="menuki")
    # kashira: the captured soul (an orb) and a tail of two soul wisps
    K.blob("soulorb", (0.0, c.y0 - 1.0, 0.0), (1.05, 1.05, 1.05), P["accent"], seed=8, lump=0.05, rings=5, segs=8, part="kashira_gem")
    for k, (dx, ln, w, bend, mat) in enumerate(((0.0, 17.0, 1.3, 2.0, "accent"), (-1.0, 11.0, 1.1, -2.2, "accent2"), (1.0, 13.0, 1.2, 1.6, "accent"))):
        Lb.flame("wisp%d" % k, V((dx, c.y0 - 2.0, 0.3 * k - 0.3)), V((0.0, -1.0, 0.0)), ln, w, P[mat], bend=V((bend, 0, 0)), curl=0.8 * (1 if k % 2 else -1), facing=(0, 0, 1), seed=40 + k, part="tassel", rows=7)


def soul_mats():
    return dict(
        bone=lambda seed: Lb.paint("soul_bone", rgb(218, 208, 178), rgb(255, 252, 234), seed, rough=0.6, dirt_col=(0.09, 0.06, 0.02, 1), mottle=rgb(150, 130, 90), bump="leather"),
        accent2=lambda seed: Lb.glow("soul_glow2", rgb(150, 120, 255), 0.8, seed, flicker=0.8))


# ================================================================ 3. SIDERITE KATANA
def star4(c, name, centre, size, mat, s):
    """A four-point star inlay (two crossing needles) lying on face s of the blade."""
    x, y, z = centre
    KS.diamond(name + "a", (x, y, z), size * 0.34, mat, s, stretch=3.2)
    KS.diamond(name + "b", (x, y, z), size * 1.0, mat, s, stretch=0.34)


def siderite_extras(c, P, rng):
    # the rough meteor crust along the spine (painted band with lumps) and the starlight inlay
    for s in (1,):
        ys = [c.yh + c.Bl * (0.10 + 0.82 * i / 40) for i in range(41)]
        KT.paint(c, "crust", ys, lambda y: 0.90 + 0.04 * math.sin(y * 0.9), lambda y: 0.995, P["crust"], lift=0.05, thick=0.03)
    for s in (-1,):
        ys = [c.yh + c.Bl * (0.10 + 0.82 * i / 40) for i in range(41)]
        KT.paint(c, "crust_b", ys, lambda y: 0.90 + 0.04 * math.sin(y * 0.7 + 1.0), lambda y: 0.995, P["crust"], lift=0.05, thick=0.03)
    KS.scatter(c, rng, 8, P["crust"], ry=(0.8, 1.8), ra=(0.04, 0.07), y=(0.12, 0.9), a=(0.86, 0.94), name="lump", lift=0.08)
    for i in range(7):
        s = 1 if i % 2 else -1
        y = c.yh + c.Bl * (0.12 + 0.76 * i / 6)
        p = c.pt(y, 0.55 + 0.12 * ((i % 3) - 1), s, 0.07)
        star4(c, "bigstar%d" % i, (p.x, p.y, p.z), c.w0 * (0.20 if i % 2 else 0.15), P["accent"], s)
    KS.stars(c, P, rng, n=34, mat=P["accent"], size=0.20)
    KS.grooves(c, P, P["line"]) if c.S.get("bohi") else None
    # menuki: a four-point star on each flat of the grip (a silver plate under a glowing one)
    r0, sq, zf, ym = grip_dims(c)
    for s in (1, -1):
        for k, (ro, ri, mat) in enumerate(((1.05, 0.42, P["menuki"]), (0.78, 0.24, P["accent"]))):
            pts = Lb.star_outline(4, ro, ri, 0.0, ym, phase=0.0)
            z0 = s * (zf - 0.05 + 0.18 * k)
            K.plate("kmenuki%+d_%d" % (s, k), pts, min(z0, z0 + s * 0.16), max(z0, z0 + s * 0.16), mat, part="menuki", bevel_w=0.0)
    # the orbit ring hovering round the tsuba, three bead stars on it
    Rr = c.S["tsuba"][1] * 0.86                                       # inside the star's tips: the ring must not widen the guard
    yo = c.tsuba_y + c.tsuba_t + 0.9
    Lb.ring_torus("orbit", (0.0, yo, 0.0), Rr, 0.13, P["accent"], "orbit", plane="xz", sides=5, segs=28)
    for k, ang in enumerate((25, 145, 265)):
        a = math.radians(ang)
        K.blob("bead%d" % k, (Rr * math.cos(a), yo, Rr * math.sin(a)), (0.32, 0.32, 0.32), P["accent"], seed=90 + k, lump=0.05, rings=4, segs=6, part="orbit")
    # kashira: a six-point star crest and a star cord with glowing beads
    pts = Lb.star_outline(6, 1.5, 0.62, 0.0, 0.0, phase=0.2)
    K.xf(K.plate("kstar", pts, 0.0, 0.22, P["menuki"], part="kashira_gem", bevel_w=0.0), K.frame((0, c.y0 - 0.25, 0), (0, -1, 0), (0, 0, 1)))
    cp = [V(p) for p in ((0.0, c.y0 - 0.6, 0.0), (1.1, c.y0 - 4.0, 0.7), (1.8, c.y0 - 8.0, 0.0), (0.7, c.y0 - 12.5, -0.7), (0.9, c.y0 - 16.0, 0.0))]
    K.tapered("starcord", cp, 0.30, 0.22, P["ito2"], sides=5, sub=3, part="tassel")
    for k, p in enumerate(cp[2:]):
        K.blob("cbead%d" % k, p, (0.62, 0.62, 0.62), P["accent"], seed=95 + k, lump=0.04, rings=4, segs=6, part="tassel")


def siderite_mats():
    return dict(
        meteor=lambda seed: K.surface("sid_iron", rgb(78, 84, 98), 1.0, 0.32, wear=1.0, wear_color=rgb(210, 220, 235), dirt=0.6, dirt_color=(0.01, 0.01, 0.02, 1), scratches=0.8, bump="hammer", bump_scale=0.5,
                                      bump_strength=0.2, mottle=0.8, mottle_color=rgb(40, 44, 56), fine=0.5, seed=seed, bevel=0.2, ao_dist=2.0, veins=0.7, vein_color=rgb(190, 200, 215), wear_radius=0.55,
                                      cramp=("y", 31.0, 108.0, [(0.0, rgb(60, 66, 80)), (0.5, rgb(92, 100, 118)), (1.0, rgb(110, 124, 150))])),
        crust=lambda seed: Lb.paint("sid_crust", rgb(46, 40, 42), rgb(150, 130, 120), seed, rough=0.85, dirt_col=(0.0, 0.0, 0.0, 1), mottle=rgb(96, 62, 46), bump="hammer"))


# ================================================================ 4. SAKURA STONE OF DESTINY KATANA
def sakura_extras(c, P, rng):
    # petals inlaid in the stone: small five-petal flowers and loose petals (painted: no quads)
    for i in range(9):
        s = 1 if i % 2 else -1
        y = c.yh + c.Bl * (0.10 + 0.80 * i / 8)
        a = 0.52 + 0.14 * math.sin(i * 1.9)
        p = c.pt(y, a, s, 0.07)
        ph = rng.uniform(0, TAU)
        for k in range(5):
            ang = ph + TAU * k / 5
            KS.diamond("fl%d_%d" % (i, k), (p.x + 0.55 * math.cos(ang) * 0.9, p.y + 0.55 * math.sin(ang), p.z), 0.30, P["accent"] if (i + k) % 2 else P["petal2"], s, stretch=1.5)
        KS.diamond("fl%dc" % i, (p.x, p.y, p.z), 0.16, P["heart"], s)
    for i in range(18):
        s = 1 if i % 2 else -1
        y = c.yh + c.Bl * rng.uniform(0.08, 0.93)
        p = c.pt(y, rng.uniform(0.2, 0.9), s, 0.07)
        KS.diamond("lp%d" % i, (p.x, p.y, p.z), rng.uniform(0.18, 0.3), P["petal"], s, stretch=rng.uniform(1.2, 1.9))
    # menuki: a five-petal blossom on each flat of the grip
    r0, sq, zf, ym = grip_dims(c)
    for s in (1, -1):
        Lb.blossom("menuki%+d" % s, V((0.0, ym, s * (zf + 0.1))), V((0, 0, s)), 2.0, P["petal"], P["heart"], P["stamen"], rng, petals=5, part="menuki", rows=3, thick=0.08)
    # kashira: a blossom crest, and a tassel of cord with three loose petals
    Lb.blossom("crest", V((0.0, c.y0 - 0.25, 0.0)), V((0, -1, 0)), 2.0, P["petal2"], P["heart"], P["stamen"], rng, petals=5, part="kashira_gem", up=(0, 0, 1), rows=3, thick=0.08)
    top = V((0.0, c.y0 - 1.6, 0.0))
    cp = [top, top + V((0.9, -5.0, 0.8)), top + V((2.2, -10.0, 0.2)), top + V((1.5, -15.0, -0.8)), top + V((1.1, -19.0, -0.2))]
    K.tapered("cord", cp, 0.32, 0.22, P["cord"], sides=5, sub=3, part="tassel")
    for k, p in enumerate(cp[1:]):
        Lb.petal("cp%d" % k, p, V((0.5 * (1 if k % 2 else -1), -1.0, 0.1)), 2.9, 1.9, P["petal2"] if k % 2 else P["petal"], facing=(0, 0, 1), curl=0.5, cup=0.25, thick=0.07, part="tassel", seed=k + 9, rows=4, simple=True)


def sakura_mats():
    def petal(seed):
        return K.surface("sak_petal", rgb(255, 196, 214), 0.0, 0.6, tramp=[(0.0, rgb(255, 238, 244)), (0.6, rgb(255, 190, 212)), (1.0, rgb(255, 142, 184))], wear=0.4, wear_color=rgb(255, 250, 252),
                         wear_metal=0.0, mottle=0.7, mottle_color=rgb(255, 120, 170), seed=seed, bevel=0.0, fine=0.3)

    def petal2(seed):
        return K.surface("sak_petal2", rgb(255, 150, 190), 0.0, 0.6, tramp=[(0.0, rgb(255, 220, 232)), (0.6, rgb(255, 150, 190)), (1.0, rgb(226, 96, 150))], wear=0.4, wear_color=rgb(255, 245, 250),
                         wear_metal=0.0, mottle=0.7, mottle_color=rgb(210, 80, 140), seed=seed, bevel=0.0, fine=0.3)
    return dict(
        stone=lambda seed: K.surface("sak_stone", rgb(250, 128, 178), 0.0, 0.1, mottle=0.9, mottle_color=rgb(255, 200, 224), seed=seed, bevel=0.1, rim=(rgb(255, 235, 245), 0.4), scratches=0.6, veins=0.5,
                                     vein_color=rgb(255, 250, 252), vein_strength=0.0, wear=0.4, wear_color=rgb(255, 245, 250), wear_metal=0.0,
                                     cramp=("y", 31.0, 105.0, [(0.0, rgb(214, 80, 136)), (0.55, rgb(250, 124, 174)), (1.0, rgb(255, 170, 204))])),
        petal=petal, petal2=petal2,
        heart=lambda seed: Lb.glow("sak_heart", rgb(255, 214, 120), 0.9, seed, flicker=0.4),
        stamen=lambda seed: Lb.paint("sak_stamen", rgb(255, 232, 190), rgb(255, 255, 240), seed, rough=0.5),
        cord=lambda seed: Lb.cloth("sak_cord", rgb(255, 214, 224), rgb(150, 90, 110), seed, mottle=rgb(255, 160, 190), bump="wrap", scale=1.0))


# ================================================================ 5. WIND KING KATANA
def wind_extras(c, P, rng):
    # wind swirls: three flowing lines of light along the blade, and feather inlays near the spine
    for s in (1, -1):
        for j, (a0, ph, amp) in enumerate(((0.62, 0.0, 0.07), (0.46, 1.7, 0.06), (0.74, 3.1, 0.05))):
            path = [(c.yh + c.Bl * (0.10 + 0.78 * i / 30), a0 + amp * math.sin(i * 0.55 + ph + s)) for i in range(31)]
            ribbon_on(c, "swirl%d" % j, path, lambda t: 0.16 * math.sin(math.pi * t) ** 0.6 + 0.03, P["accent"], s, thick=0.03)
        for i in range(8):
            y = c.yh + c.Bl * (0.14 + 0.72 * i / 7)
            p = c.pt(y, 0.86, s, 0.07)
            KS.diamond("feather%d" % i, (p.x, p.y, p.z), c.w0 * 0.10, P["accent_dark"], s, stretch=7.0)
            KS.diamond("featherb%d" % i, (p.x, p.y + 0.6, p.z), c.w0 * 0.05, P["accent"], s, stretch=9.0)
    KS.grooves(c, P, P["line"])
    # menuki: two small crossed feathers on each flat of the grip
    r0, sq, zf, ym = grip_dims(c)
    for s in (1, -1):
        for k, dx in enumerate((-0.5, 0.5)):
            K.feather("fm%+d_%d" % (s, k), V((dx * 0.4, ym - 1.5, s * (zf + 0.05))), V((dx * 0.55, 1.0, 0.0)), 3.3, 1.0, P["feather"] if k else P["feather2"], rows=6, serrate=0.15, facing=(0, 0, s),
                      thick=0.07, part="menuki", seed=k + 3, curl=0.4)
    # kashira: a jade gem and a plume of three feathers
    K.gem_setting("pommelgem", V((0, c.y0 - 0.15, 0.0)), 1.0, P["gem"], P["fit"], normal=(0, -1, 0), claws=4, sides=6, part="kashira_gem", rng_=rng, up=(0, 0, 1), claw_r=0.3)
    for k, (dx, ln, wd, bend) in enumerate(((0.0, 16.0, 2.4, 1.2), (-1.0, 12.0, 2.0, -1.4), (1.1, 13.5, 2.1, 1.0))):
        K.feather("plume%d" % k, V((dx * 0.4, c.y0 - 1.4, 0.2 * k)), V((dx * 0.12, -1.0, 0.0)), ln, wd, P["feather"] if k % 2 == 0 else P["feather2"], bend=V((bend, 0, 0)), curl=1.0, rows=7, serrate=0.18,
                  facing=(0, 0, 1), thick=0.08, part="tassel", seed=60 + k)
    cp = [V(p) for p in ((0.0, c.y0 - 0.9, 0.0), (0.7, c.y0 - 3.0, 0.4), (1.1, c.y0 - 6.0, 0.0))]
    K.tapered("cord", cp, 0.28, 0.2, P["cord"], sides=5, sub=2, part="tassel")


def wind_mats():
    return dict(
        feather=lambda seed: K.surface("wind_feather", rgb(220, 240, 236), 0.0, 0.55, wear=0.5, wear_color=rgb(255, 255, 250), wear_metal=0.0, dirt=0.35, dirt_color=(0.03, 0.05, 0.05, 1),
                                       feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(100, 160, 160)), (0.45, rgb(232, 246, 242)), (1.0, rgb(120, 214, 204))], mottle=0.6, mottle_color=rgb(150, 200, 196),
                                       seed=seed, bevel=0.06, fine=0.4),
        feather2=lambda seed: K.surface("wind_feather2", rgb(80, 170, 160), 0.0, 0.5, wear=0.5, wear_color=rgb(220, 255, 245), wear_metal=0.0, dirt=0.4, feather=(14, 5, 0.7, 0.8),
                                        tramp=[(0.0, rgb(30, 90, 90)), (0.5, rgb(70, 170, 156)), (1.0, rgb(200, 250, 236))], mottle=0.6, mottle_color=rgb(30, 110, 110), seed=seed, bevel=0.06, fine=0.4),
        gem=lambda seed: K.surface("wind_jade", rgb(70, 220, 190), 0.0, 0.08, emit=rgb(90, 240, 205), emit_strength=0.9, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(200, 255, 235), seed=seed, bevel=0.0),
        cord=lambda seed: Lb.cloth("wind_cord", rgb(40, 130, 120), rgb(10, 46, 44), seed, mottle=rgb(20, 80, 74), bump="wrap", scale=1.0))


# ================================================================ the specs
SPECS = {}


def spec(key):
    def deco(fn):
        SPECS[key] = fn()
        return fn
    return deco


@spec("tiger")
def _tiger():
    return dict(id="vassal_katana__white_tiger_katana", seed=71, div=17.0, sori=0.026, kasane=0.28, bohi=True, hamon=("midare", 0.10, None), tsuba=("claw", 4.5), tail=28.5, nomenuki=True,
                extras=tiger_extras, extra_mats=tiger_mats(),
                pal=dict(steel=(214, 222, 234), steel_wear=(255, 255, 255), mottle=(160, 185, 215), dirt=0.25, cramp=(31.0, 106.0, [(0.0, (184, 194, 210)), (0.6, (226, 234, 244)), (1.0, (196, 228, 247))]),
                         hamon=(250, 252, 255), hamon2=(200, 225, 245), hamon_emit=(160, 225, 255), hamon_glow=0.35, line=(160, 225, 255), line_s=1.3,
                         fit=(226, 178, 66), fit_wear=(255, 244, 200), fit2=(160, 100, 30), tsuba=(34, 34, 40), tsuba_wear=(150, 150, 165), kash=(226, 178, 66),
                         ito=(238, 236, 228), ito2=(28, 28, 34), ray=(60, 56, 58), menuki=(226, 178, 66), accent=(170, 230, 255), accent_s=0.9, accent_dark=(22, 22, 28)))


@spec("soul")
def _soul():
    return dict(id="vassal_katana__soul_eater_katana", seed=72, div=17.5, sori=0.024, kasane=0.30, bohi=True, hamon=("gunome", 0.17, None), tsuba=("ribs", 4.4), tail=29.8, nomenuki=True,
                extras=soul_extras, extra_mats=soul_mats(),
                pal=dict(steel=(36, 42, 52), steel_wear=(150, 190, 190), mottle=(18, 60, 60), dirt=0.55, cramp=(31.0, 104.0, [(0.0, (28, 30, 40)), (0.6, (42, 52, 62)), (1.0, (40, 84, 84))]),
                         hamon=(150, 235, 215), hamon2=(80, 190, 170), hamon_emit=(100, 255, 205), hamon_glow=0.7, line=(100, 255, 205), line_s=1.6,
                         fit=(70, 80, 90), fit_wear=(170, 210, 205), fit2=(20, 50, 50), tsuba=(218, 208, 178), tsuba_wear=(255, 252, 234), kash=(218, 208, 178),
                         ito=(70, 96, 92), ito2=(218, 208, 178), ray=(218, 208, 178), menuki=(218, 208, 178), accent=(100, 255, 205), accent_s=1.1, accent_dark=(20, 26, 30)))


@spec("siderite")
def _siderite():
    return dict(id="vassal_katana__siderite_katana", seed=73, div=17.0, sori=0.022, kasane=0.30, hamon=("suguha", 0.0, None), hamon_base=0.15, tsuba=("star", 4.3), tail=18.9, nomenuki=True,
                blade_mat="meteor", extras=siderite_extras, extra_mats=siderite_mats(),
                pal=dict(steel=(78, 84, 98), steel_wear=(210, 220, 235), hamon=(214, 224, 244), hamon2=(150, 170, 220), hamon_emit=(150, 200, 255), hamon_glow=0.3, line=(150, 200, 255), line_s=1.0,
                         fit=(214, 220, 234), fit_wear=(255, 255, 255), fit2=(130, 150, 200), tsuba=(28, 36, 74), tsuba_wear=(120, 140, 220), kash=(214, 220, 234),
                         ito=(36, 46, 92), ito2=(196, 204, 232), ray=(30, 30, 40), menuki=(214, 220, 234), accent=(150, 200, 255), accent_s=1.1, accent_dark=(14, 18, 40)))


@spec("sakura")
def _sakura():
    return dict(id="vassal_katana__sakura_stone_katana", seed=74, div=17.5, sori=0.024, kasane=0.28, hamon=("notare", 0.08, None), tsuba=("petal", 4.4), tail=23.6, nomenuki=True,
                blade_mat="stone", extras=sakura_extras, extra_mats=sakura_mats(),
                pal=dict(steel=(255, 160, 196), steel_wear=(255, 245, 250), hamon=(255, 244, 248), hamon2=(255, 200, 222), hamon_emit=(255, 170, 205), hamon_glow=0.28, line=(255, 170, 205), line_s=0.8,
                         fit=(232, 158, 138), fit_wear=(255, 236, 220), fit2=(170, 100, 90), tsuba=(232, 158, 138), tsuba_wear=(255, 236, 220), kash=(232, 158, 138),
                         ito=(236, 130, 164), ito2=(255, 214, 224), ray=(255, 236, 240), menuki=(232, 158, 138), accent=(255, 196, 214), accent_s=0.7, accent_dark=(90, 40, 56)))


@spec("wind")
def _wind():
    return dict(id="legendary_sword__wind_king_katana", seed=75, div=19.5, sori=0.026, kasane=0.28, bohi=True, tsuka=0.175, hamon=("notare", 0.09, -7.0), tsuba=("feather", 5.4), tail=28.1,
                nomenuki=True, extras=wind_extras, extra_mats=wind_mats(),
                pal=dict(steel=(150, 168, 176), steel_wear=(235, 250, 250), mottle=(70, 110, 120), dirt=0.45, cramp=(32.0, 150.0, [(0.0, (112, 128, 134)), (0.55, (168, 188, 196)), (1.0, (150, 215, 215))]),
                         hamon=(228, 246, 243), hamon2=(170, 220, 215), hamon_emit=(120, 240, 215), hamon_glow=0.4, line=(120, 240, 215), line_s=1.3,
                         fit=(220, 170, 64), fit_wear=(255, 240, 190), fit2=(150, 96, 30), tsuba=(54, 104, 98), tsuba_wear=(210, 235, 200), kash=(220, 170, 64),
                         ito=(236, 236, 226), ito2=(60, 140, 130), ray=(210, 200, 176), menuki=(220, 170, 64), accent=(150, 235, 220), accent_s=0.9, accent_dark=(30, 90, 90)))


# ================================================================ run
def main(argv):
    import argparse
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
        c = KT.build(S, opts)
        KT.finish(S, c, trial=a.trial, bake=256 if a.fast else a.bake, skin=256 if a.fast else 1024)


if __name__ == "__main__":
    main(sys.argv[1:])
