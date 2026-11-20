"""Weapon-form models of the VASSAL MIRROR (base `vassal_mirror`, size class shield), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_mirror.py --only tiger [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids (docs/heroweapon/form_models.md): vassal_mirror__<form>:
  white_tiger_clone_mirror  round tiger-face mirror: fur ruff, ears, whisker blades, hanging chains with star-glass panes, a ghost twin mirror behind
  hetsu_kagami              two old bronze mirrors (Okitsu / Hetsu, the sacred treasures): serrated rims, ring relief, a boss, jade comma jewels on cords
  yata_mirror               the eight-sided sun mirror of the old stories: a gold octagon with a kaleidoscope of eight glass facets, sun rays, a straw rope with paper streamers
  envious_glass             Envy 1: cracked green glass in a shard frame, four clawed hands grabbing the rim, a slit eye, dripping ooze, thorned vines
  green_eyed_mirror         Envy 2: the glass is a bulging green eye with lids and lashes, small envious eyes on the frame, thorn vines
  mirror_of_envy            Envy 3: a black void glass in a writhing tendril frame, a horned crown, six grabbing hands, a ring of eyes, shackle chains
Every form keeps the base's handle (grip y 6..23.6, collar y 23..34, chain loop to y -12.5), the head above y 30, the mirror face toward +z and the base's place line.
The mirror head never grows taller than the base's (y <= 86.8, the base already sits at the top of the shield class).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402
import forms_vassal_common as C  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
from forms_vassal_run import main  # noqa: E402

MANIFEST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "forms_manifest_vassal_mirror.json")
BASE = "vassal_mirror"


def ell(cx, cy, rx, ry, n=36, a0=0.0, jit=0.0, rng_=None):
    pts = []
    for i in range(n):
        a = a0 + TAU * i / n
        k = 1.0 + (rng_.uniform(-jit, jit) if (jit and rng_) else 0.0)
        pts.append((cx + rx * k * math.cos(a), cy + ry * k * math.sin(a)))
    return pts


def rot_pts(pts, ang, ox=0.0, oy=0.0):
    c, s = math.cos(ang), math.sin(ang)
    return [(ox + x * c - y * s, oy + x * s + y * c) for x, y in pts]


def handle(P, rr, *, wrap_turns=7.0, collar_petals=0, collar_mat=None, thorns=False, bands=(7.0, 22.6)):
    """The base's handle in the base's place: pommel y 0..6, grip y 6..23.6 (grip words only here), a collar y 23..34 flaring to the head, a short chain loop to y ~ -12."""
    K.grip_wrapped("grip", 6.0, 23.4, 2.0, P["grip_core"], P["grip_wrap"], turns=wrap_turns, cord=0.42, bands=bands, band_mat=P["trim"], swell=0.1, part="grip", seed=3, sides=10)
    Lb.lathe("collar", [(2.3, 23.0), (3.3, 24.6), (4.4, 27.4), (5.5, 30.4), (5.9, 32.4), (5.2, 34.0), (3.4, 34.4)], collar_mat or P["trim"], "collar", sides=14)
    Lb.lathe("pommel", [(0.01, 0.0), (1.8, 0.2), (2.8, 1.6), (2.9, 3.6), (2.3, 5.2), (2.1, 6.0)], P["trim"], "pommel", sides=12)
    for k in range(collar_petals):
        a = TAU * k / collar_petals + 0.3
        Lb.petal("cpet%d" % k, V((4.2 * math.cos(a), 28.0, 4.2 * math.sin(a))), V((math.cos(a) * 0.55, 0.85, math.sin(a) * 0.55)), 7.0, 3.6, P["trim"], facing=(math.cos(a), 0, math.sin(a)), curl=1.0, cup=0.3,
                 thick=0.16, part="collar_petals", seed=k, simple=True, rows=4, notch=False)
    if thorns:
        Lb.fur("thorn", [V((2.2 * math.cos(a), 8.0 + k * 1.9, 2.2 * math.sin(a))) for k, a in enumerate(TAU * j * 0.37 for j in range(9))],
               [V((math.cos(a), 0.35, math.sin(a))) for a in (TAU * j * 0.37 for j in range(9))], 2.2, 0.32, P["thorn"], rr, part="grip_thorns")
    K.chain("chain", [V((0.0, -0.8, 0.0)), V((0.3, -5.0, 0.4)), V((0.0, -9.6, 0.0))], 2.4, 1.6, 0.3, P["trim"], max_links=4, part="chain_loop", sides=4, segs=6)
    K.blob("drop", (0.0, -11.4, 0.0), (0.9, 1.3, 0.9), P["drop"], seed=4, lump=0.04, rings=5, segs=8, part="chain_loop")


# ================================================================ 1. WHITE TIGER CLONE MIRROR
def tiger_pal():
    P = {}
    P["silver"] = Lb.metal("wt_silver", rgb(214, 220, 232), rgb(255, 255, 255), 501, rough=0.2, mottle=rgb(150, 175, 210), dirt=0.4, dirt_col=(0.03, 0.04, 0.07, 1))
    P["black"] = Lb.metal("wt_black", rgb(22, 24, 30), rgb(130, 140, 160), 502, rough=0.4, dirt=0.3, dirt_col=(0, 0, 0, 1), bump="hammer", bs=0.12)
    P["gold"] = Lb.hammered("wt_gold", rgb(226, 178, 66), rgb(255, 244, 200), 503, mottle=rgb(160, 100, 30))
    P["fur"] = Lb.paint("wt_fur", rgb(238, 234, 222), rgb(255, 255, 250), 504, rough=0.8, dirt_col=(0.05, 0.045, 0.04, 1), mottle=rgb(190, 180, 160), bump="leather")
    P["fur_dark"] = Lb.paint("wt_fur_dark", rgb(30, 28, 32), rgb(120, 118, 124), 505, rough=0.8, bump="leather")
    P["glass"] = Lb.mirrorm("wt_glass", rgb(190, 225, 245), 506)
    P["ghost"] = Lb.glow("wt_ghost", rgb(160, 225, 255), 0.8, 507, flicker=0.7)
    P["eye"] = Lb.gem("wt_eye", rgb(255, 176, 30), 1.0, 508)
    P["pane"] = Lb.glass("wt_pane", rgb(190, 235, 255), 509, strength=0.5)
    P["grip_core"] = Lb.paint("wt_core", rgb(60, 56, 58), rgb(150, 145, 140), 510, bump="leather")
    P["grip_wrap"] = Lb.cloth("wt_wrap", rgb(238, 236, 228), rgb(110, 108, 100), 511, mottle=rgb(170, 165, 150), bump="wrap", scale=1.0)
    P["trim"] = P["silver"]
    P["drop"] = P["pane"]
    return P


def white_tiger_mirror(opts):
    C.begin(opts)
    C.weights(head_glass=1.1, head_rim=1.0, fur=0.9, whiskers=1.0, ghost=0.8)
    P = tiger_pal()
    rr = K.rng(7501)
    cy, R = 53.0, 12.6
    handle(P, rr)
    K.plate("glass", ell(0, cy, R - 1.9, R - 1.9, 40), -0.45, 0.45, P["glass"], part="head_glass", bevel_w=0.12)
    K.plate("back", ell(0, cy, R - 0.6, R - 0.6, 40), -1.7, -0.45, P["black"], part="back_plate", bevel_w=0.2)
    Lb.ring_torus("rim", (0, cy, 0.1), R - 0.9, 1.35, P["silver"], "head_rim", sides=8, segs=44)
    Lb.ring_torus("rim_in", (0, cy, 0.95), R - 2.2, 0.6, P["gold"], "head_bezel", sides=6, segs=40)
    for k in range(20):                                                                     # tiger stripes across the rim face (bake only)
        a = TAU * k / 20 + rr.uniform(-0.05, 0.05)
        w = rr.uniform(0.06, 0.12)
        rows = []
        for r_, ww in ((R - 2.3, w), (R - 0.9, w * 1.2), (R + 0.4, w * 0.4)):
            rows.append((V(((r_) * math.cos(a - ww), cy + r_ * math.sin(a - ww), 1.35)), V((r_ * math.cos(a + ww), cy + r_ * math.sin(a + ww), 1.35))))
        K.strip("stripe%d" % k, rows, P["black"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.3, part="stripes", up=(0, 0, 1), detail=True)
    K.rivets("rim_rv", [(V(((R - 0.9) * math.cos(a), cy + (R - 0.9) * math.sin(a), 1.7)), (0, 0, 1)) for a in [TAU * k / 14 + 0.12 for k in range(14)]], P["gold"], r=0.6, rng_=rr, jitter=0.15, detail=False)
    # fur ruff round the rim: white tufts with dark tips, longer at the sides, short where the collar is
    bases, dirs = [], []
    for k in range(34):
        a = TAU * k / 34 + rr.uniform(-0.05, 0.05)
        bases.append(V(((R + 0.4) * math.cos(a), cy + (R + 0.4) * math.sin(a), -0.2)))
        dirs.append(V((math.cos(a), math.sin(a), -0.25 + 0.2 * math.sin(k * 1.9))))
    Lb.fur("ruff", bases, dirs, 3.8, 0.6, P["fur"], rr, part="fur", tip_mat=P["fur_dark"])
    # ears
    for sx in (-1, 1):
        ang = sx * math.radians(52)
        base = (R * 0.86 * math.sin(ang), cy + R * 0.86 * math.cos(ang))
        outer = [(-3.6, 0.0), (3.6, 0.0), (3.0, 3.4), (1.2, 6.4), (-0.4, 7.0), (-2.4, 4.8), (-3.4, 2.6)]
        inner = [(-2.1, 0.8), (2.1, 0.8), (1.7, 3.0), (0.4, 5.2), (-1.4, 3.6)]
        K.plate("ear%d" % sx, rot_pts(outer, -ang, *base), -0.9, 0.7, P["fur"], part="ears", bevel_w=0.15)
        K.plate("earin%d" % sx, rot_pts(inner, -ang, *base), 0.7, 1.15, P["fur_dark"], part="ears", bevel_w=0.1)
        tip = rot_pts([(0.2, 6.6)], -ang, *base)[0]
        Lb.fur("eartuft%d" % sx, [V((tip[0], tip[1], 0.0))] * 3, [V((math.sin(ang) * 0.6 + 0.3 * k, 1.0, 0.2)) for k in (-1, 0, 1)], 2.2, 0.35, P["fur"], rr, part="ears", tip_mat=P["fur_dark"])
    # whisker blades: three curved silver lancets on each side
    for sx in (-1, 1):
        for k in range(3):
            ang = (8 + 17 * k) * (1 if k != 1 else 1.1)
            a = math.radians(ang)
            Lb.lancet("whisker%d_%d" % (sx, k), V((sx * (R + 0.2), cy - 2.6 + 2.6 * k, 0.2)), V((sx * math.cos(a), math.sin(a) - 0.05, 0)), 8.5 - 1.2 * k + sx * 0.4, 1.7, P["silver"], thick=0.35,
                      part="whiskers", curve=sx * 1.5, n=6)
    K.gem_setting("brow", V((0, cy + R + 0.4, 1.4)), 1.9, P["eye"], P["gold"], normal=(0, 0, 1), claws=5, sides=6, part="gem", rng_=rr)
    for k in range(3):                                                                      # forehead stripes
        K.bar("fstripe%d" % k, (-1.2 + k * 1.2, cy + R + 1.8, 1.0), (-0.8 + k * 1.2, cy + R + 4.3, 0.8), 0.28, 0.2, P["black"], part="ears", taper=0.5)
    # the clone: a ghost twin of the mirror behind it, offset up and to the side, tied to the rim by two light rods
    Lb.ring_torus("ghost_ring", (7.6, cy + 7.4, -3.2), 9.0, 0.6, P["ghost"], "ghost", sides=5, segs=32)
    K.plate("ghost_glass", ell(7.6, cy + 7.4, 8.4, 8.4, 32), -3.5, -3.0, P["ghost"], part="ghost", bevel_w=0.0)
    for k, (x, y) in enumerate(((6.0, 8.0), (10.0, 1.5))):
        K.tapered("ghostrod%d" % k, [V((x * 0.7, cy + y * 0.6, -0.8)), V((x * 0.9, cy + y * 0.8, -2.0)), V((x, cy + y, -3.2))], 0.5, 0.2, P["ghost"], sides=5, sub=1, part="ghost")
    # chains of star-glass: two chains hang from the lower rim, each ending in a hexagonal pane in a gold frame
    for sx in (-1, 1):
        path = [V((sx * 9.0, cy - 9.2, 0.4)), V((sx * 11.2, cy - 15.0, 0.6)), V((sx * 10.0, cy - 21.0, 0.2))]
        K.chain("chain%d" % sx, path, 2.3, 1.5, 0.3, P["silver"], max_links=5, part="chains", sides=4, segs=6)
        hexo = ell(sx * 9.6, cy - 24.4, 2.3, 2.3, 6, a0=0.52)
        K.plate("pane%d" % sx, hexo, -0.3, 0.3, P["pane"], part="chain_panes", bevel_w=0.1)
        Lb.ring_torus("paneframe%d" % sx, (sx * 9.6, cy - 24.4, 0.0), 2.4, 0.38, P["gold"], "chain_panes", sides=4, segs=6)
    return K.quad_estimate()


# ================================================================ 2. OKITSU KAGAMI: HETSU KAGAMI  (two bronze mirrors)
def hetsu_pal():
    P = {}
    P["bronze"] = K.surface("het_bronze", rgb(176, 118, 62), 1.0, 0.3, wear=1.0, wear_color=rgb(246, 210, 140), dirt=0.8, dirt_color=(0.02, 0.03, 0.02, 1), scratches=0.8, bump="hammer", bump_scale=0.5,
                            bump_strength=0.2, mottle=1.0, mottle_color=rgb(42, 120, 100), fine=0.5, seed=601, bevel=0.2, ao_dist=2.0, wear_radius=0.5)
    P["bronze2"] = K.surface("het_bronze2", rgb(130, 96, 60), 1.0, 0.4, wear=1.0, wear_color=rgb(214, 170, 110), dirt=0.9, dirt_color=(0.01, 0.025, 0.02, 1), scratches=0.7, bump="hammer", bump_scale=0.6,
                             bump_strength=0.25, mottle=1.0, mottle_color=rgb(50, 140, 112), fine=0.6, seed=602, bevel=0.2, ao_dist=2.0)
    P["glass"] = Lb.mirrorm("het_glass", rgb(240, 214, 160), 603)
    P["glass2"] = Lb.mirrorm("het_glass2", rgb(190, 232, 210), 604)
    P["jade"] = Lb.gem("het_jade", rgb(70, 220, 160), 0.8, 605)
    P["cord"] = Lb.cloth("het_cord", rgb(176, 30, 36), rgb(60, 6, 10), 606, mottle=rgb(230, 80, 70), bump="wrap", scale=1.0)
    P["gold"] = Lb.hammered("het_gold", rgb(226, 176, 62), rgb(255, 244, 200), 607, mottle=rgb(160, 100, 30))
    P["glow"] = Lb.glow("het_glow", rgb(255, 226, 150), 0.8, 608, flicker=0.6)
    P["grip_core"] = Lb.paint("het_core", rgb(60, 40, 34), rgb(150, 120, 100), 609, bump="grain")
    P["grip_wrap"] = P["cord"]
    P["trim"] = P["bronze"]
    P["drop"] = P["jade"]
    return P


def serrated(cx, cy, R, teeth, depth, a0=0.0):
    """A disc outline with a saw-tooth edge (triangle teeth): the rim of an old bronze mirror."""
    pts = []
    for i in range(teeth):
        for k, rr_ in ((0.0, R - depth), (0.5, R + depth * 0.2), (0.5001, R + depth * 0.2)):
            pass
        a = a0 + TAU * i / teeth
        pts.append((cx + (R - depth) * math.cos(a), cy + (R - depth) * math.sin(a)))
        pts.append((cx + (R + depth) * math.cos(a + TAU / teeth * 0.5), cy + (R + depth) * math.sin(a + TAU / teeth * 0.5)))
    return pts


def bronze_mirror(P, name, cx, cy, R, z, glass_mat, rim_mat, rr, front=1.0, boss=True, seed=0):
    """One old round bronze mirror: serrated disc, raised double rim, concentric ring relief, a domed boss, a polished face. `z` is its centre plane."""
    K.stepped_plate(name + "_disc", serrated(cx, cy, R, 40, 0.9), z - 0.7, rim_mat, steps=2, step_h=0.55, inset=0.9, part=name, up=1, bevel_w=0.0)
    K.plate(name + "_back", ell(cx, cy, R - 0.4, R - 0.4, 40), z - 1.5, z - 0.7, P["bronze2"], part=name + "_back", bevel_w=0.15)
    K.plate(name + "_glass", ell(cx, cy, R - 2.8, R - 2.8, 36), z + 0.35, z + 0.75, glass_mat, part=name + "_glass", bevel_w=0.1)
    for i, (rr_, w) in enumerate(((R - 2.0, 0.5), (R - 3.2, 0.32))):
        Lb.ring_torus("%s_ring%d" % (name, i), (cx, cy, z + 0.7), rr_, w, rim_mat, name + "_rings", sides=5, segs=36)
    if boss:
        Lb.lathe(name + "_boss", [(0.01, 1.2), (1.4, 1.1), (2.2, 0.7), (2.6, 0.2), (2.4, -0.1)], rim_mat, name + "_boss", sides=12, axis="z", origin=(cx, cy, z + 0.6))
    # ring of little bosses on the disc between the rings (the studs of the real thing)
    K.rivets(name + "_studs", [(V((cx + (R - 4.6) * math.cos(a), cy + (R - 4.6) * math.sin(a), z + 0.75)), (0, 0, 1)) for a in [TAU * k / 9 + seed for k in range(9)]], rim_mat, r=0.75, rng_=rr,
             jitter=0.12, detail=False)


def hetsu_kagami(opts):
    C.begin(opts)
    C.weights(okitsu=1.2, okitsu_glass=1.2, hetsu=1.0, tassels=0.8)
    P = hetsu_pal()
    rr = K.rng(7602)
    handle(P, rr, wrap_turns=6.0, bands=(7.0, 22.8))
    cy = 51.0
    bronze_mirror(P, "okitsu", -8.6, cy + 9.4, 8.8, -3.4, P["glass2"], P["bronze2"], rr, seed=0.3)                       # the second mirror, behind and above
    bronze_mirror(P, "hetsu", 3.2, cy, 12.6, 0.0, P["glass"], P["bronze"], rr, seed=0.1)
    for sx, a in ((-1, 200), (1, 340)):                                                       # cloud scrolls on the face of the front mirror
        pts = Lb_spiral(3.2 + sx * 0.0, cy, a)
        K.filigree("scroll%d" % sx, pts, P["gold"], w=0.45, h=0.3, z=1.2, part="scrolls", sub=2, detail=False)
    # a looped red cord through the boss, with a tassel hanging down beside the collar, and three jade comma-jewels (magatama) on cords
    cp = [V(p) for p in ((3.2, cy + 0.0, 1.8), (8.6, cy - 1.0, 2.2), (12.0, cy - 7.0, 1.6), (11.2, cy - 14.0, 0.8), (9.8, cy - 20.0, 0.4))]
    K.tapered("cord", cp, 0.4, 0.3, P["cord"], sides=5, sub=3, part="tassels")
    for k in range(8):
        a = TAU * k / 8
        K.tapered("tassel%d" % k, [cp[-1], cp[-1] + V((0.5 * math.cos(a), -2.4, 0.5 * math.sin(a))), cp[-1] + V((0.9 * math.cos(a), -5.2 - 0.5 * (k % 3), 0.9 * math.sin(a)))], 0.3, 0.1, P["cord"], sides=4, sub=1,
                  part="tassels")
    for k, (x, ln) in enumerate(((-4.0, 14.0), (3.4, 17.5), (10.0, 12.0))):
        x0 = 3.2 + (x - 3.2) * 0.8
        top = V((x0, cy - 10.6, 0.5))
        K.tapered("string%d" % k, [top, top + V((0.2, -ln * 0.5, 0.2)), top + V((0.0, -ln, 0.0))], 0.26, 0.2, P["cord"], sides=4, sub=2, part="tassels")
        end = top + V((0.0, -ln - 0.5, 0.0))
        mp = [end + V((0.0, 0.0, 0.0)), end + V((1.4, -1.4, 0.0)), end + V((1.2, -3.2, 0.0)), end + V((-0.5, -4.2, 0.0)), end + V((-1.4, -3.5, 0.0))]
        K.tapered("magatama%d" % k, mp, 1.25, 0.18, P["jade"], sides=6, sub=2, part="magatama", bulge=lambda t: 1.0 + 0.1 * math.sin(t * 6))
        K.blob("mhole%d" % k, end + V((0.1, -0.1, 1.2)), (0.3, 0.3, 0.2), P["gold"], seed=k, lump=0.0, rings=3, segs=6, part="magatama")
    # a gold sun-dot ring on the upper mirror's disc and little ray ticks round the rim of the front one
    for k in range(12):
        a = TAU * k / 12 + 0.1
        Lb.lancet("tick%d" % k, V((3.2 + 13.2 * math.cos(a), cy + 13.2 * math.sin(a), -0.2)), V((math.cos(a), math.sin(a), 0)), 2.4 if k % 2 else 1.6, 0.9, P["gold"], thick=0.2, part="ticks", n=3)
    return K.quad_estimate()


def Lb_spiral(cx, cy, a_deg):
    a = math.radians(a_deg)
    return K.bl and [(cx + p[0], cy + p[1]) for p in Lb_curl(a)]


def Lb_curl(a):
    out = []
    for i in range(14):
        t = i / 13.0
        r = 7.6 - 3.4 * t
        th = a + 2.6 * t * (1 if math.cos(a) > 0 else -1)
        out.append((r * math.cos(th), r * math.sin(th)))
    return out


# ================================================================ 3. YATA MIRROR (eight-sided sun mirror)
def yata_pal():
    P = {}
    P["gold"] = Lb.hammered("yata_gold", rgb(240, 192, 72), rgb(255, 248, 210), 701, rough=0.28, mottle=rgb(180, 110, 30))
    P["gold2"] = Lb.metal("yata_gold2", rgb(190, 140, 50), rgb(250, 220, 140), 702, rough=0.4, dirt_col=(0.05, 0.025, 0.004, 1), bump="hammer", bs=0.15)
    P["red"] = Lb.paint("yata_red", rgb(176, 26, 24), rgb(250, 120, 90), 703, rough=0.35, dirt_col=(0.04, 0.0, 0.0, 1), mottle=rgb(110, 10, 12), bump="grain")
    P["f1"] = Lb.glass("yata_f1", rgb(176, 214, 255), 704, strength=0.3)
    P["f2"] = Lb.glass("yata_f2", rgb(255, 176, 96), 705, strength=0.3)
    P["jewel"] = Lb.gem("yata_jewel", rgb(255, 70, 60), 0.9, 706)
    P["paper"] = Lb.cloth("yata_paper", rgb(244, 242, 234), rgb(160, 156, 140), 707, mottle=rgb(200, 196, 180), bump="cross", scale=0.8)
    P["straw"] = Lb.cloth("yata_straw", rgb(206, 174, 94), rgb(90, 66, 24), 708, mottle=rgb(150, 120, 60), bump="wrap", scale=1.6)
    P["glow"] = Lb.glow("yata_glow", rgb(255, 214, 120), 0.85, 709, flicker=0.6)
    P["grip_core"] = Lb.paint("yata_core", rgb(120, 20, 20), rgb(220, 110, 90), 710, bump="grain")
    P["grip_wrap"] = P["red"]
    P["trim"] = P["gold"]
    P["drop"] = P["jewel"]
    return P


def yata_mirror(opts):
    C.begin(opts)
    C.weights(head_glass=1.2, facets=1.2, rays=1.0, rope=0.9, paper=0.8)
    P = yata_pal()
    rr = K.rng(7703)
    cy, R = 52.0, 13.2
    handle(P, rr, wrap_turns=6.0, collar_petals=7)
    oct_ = ell(0, cy, R, R, 8, a0=math.pi / 8)
    for s_ in (1, -1):
        K.stepped_plate("frame%d" % s_, oct_, 0.0 if s_ > 0 else 0.0, P["gold"], steps=3, step_h=0.6, inset=1.1, part="head_frame", up=s_, bevel_w=0.0)
    K.plate("back_disc", ell(0, cy, R - 0.8, R - 0.8, 8, a0=math.pi / 8), -2.4, -1.8, P["gold2"], part="back_plate", bevel_w=0.1)
    # the kaleidoscope: eight triangular glass facets of the inner octagon, each tilted differently, alternating silver and gold glass
    ri = R - 3.4
    inner = ell(0, cy, ri, ri, 8, a0=math.pi / 8)
    for k in range(8):
        p0, p1 = inner[k], inner[(k + 1) % 8]
        peak = 3.3 + 0.55 * math.sin(k * 2.3)
        verts = [(0, cy, peak), (p0[0], p0[1], 2.0), (p1[0], p1[1], 2.0), (0, cy, 1.2), (p0[0], p0[1], 1.2), (p1[0], p1[1], 1.2)]
        faces = [(0, 1, 2), (0, 3, 4, 1), (1, 4, 5, 2), (2, 5, 3, 0), (3, 5, 4)]
        K.mk("facet%d" % k, verts, faces, P["f1"] if k % 2 else P["f2"], part="facets", smooth=False)
    Lb.ring_torus("inner_ring", (0, cy, 2.1), ri + 0.2, 0.55, P["gold"], "head_frame", sides=6, segs=16)
    K.gem_setting("sunjewel", V((0, cy, 3.6)), 1.7, P["jewel"], P["gold"], normal=(0, 0, 1), claws=6, sides=8, part="gem", rng_=rr)
    for k in range(8):                                                                       # a red jewel in each corner of the frame
        a = math.pi / 8 + TAU * k / 8
        K.gem_setting("cj%d" % k, V(((R - 1.1) * math.cos(a), cy + (R - 1.1) * math.sin(a), 1.9)), 0.8, P["jewel"], P["gold"], normal=(0, 0, 1), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.3)
    # sun rays: sixteen rays, long straight ones alternating with short wavy ones (left and right are not twins)
    for k in range(16):
        a = TAU * k / 16 + 0.04
        long_ = k % 2 == 0
        ln = (8.4 if long_ else 4.6) * rr.uniform(0.9, 1.1)
        if abs(math.sin(a)) < 0.2 and math.cos(a) < 0 and abs(a - math.pi) < 0.2 and False:
            continue
        r0 = R + 0.2
        if cy + (r0 + ln) * math.sin(a) > 85.5:
            ln = max(2.5, (85.5 - cy) / max(0.2, math.sin(a)) - r0)
        if math.sin(a) < -0.85:
            continue                                                                         # no ray into the collar
        Lb.lancet("ray%d" % k, V((r0 * math.cos(a), cy + r0 * math.sin(a), -0.3)), V((math.cos(a), math.sin(a), 0)), ln, 2.5 if long_ else 1.7, P["gold"] if long_ else P["gold2"], thick=0.45, part="rays",
                  curve=(0.0 if long_ else (1.2 if k % 4 == 1 else -1.2)), n=7)
    # a second row of short rays behind the long ones (the sun's corona), and a ring of studs on the frame
    for k in range(16):
        a = TAU * (k + 0.5) / 16 + 0.04
        if math.sin(a) < -0.8 or cy + (R + 6.0) * math.sin(a) > 84.5:
            continue
        Lb.lancet("corona%d" % k, V(((R + 0.4) * math.cos(a), cy + (R + 0.4) * math.sin(a), -1.1)), V((math.cos(a), math.sin(a), 0)), 5.4, 1.7, P["gold2"], thick=0.4, part="rays", curve=1.4 if k % 2 else -1.4, n=6)
    for sd in (1, -1):
        K.rivets("frame_rv%d" % sd, [(V(((R - 2.2) * math.cos(a), cy + (R - 2.2) * math.sin(a), 0.2 + 0.0)) + V((0, 0, 1.7 * sd)), (0, 0, sd)) for a in (TAU * (k + 0.5) / 16 for k in range(16))],
                 P["gold"], r=0.5, part="head_frame", detail=False, sides=5, height=0.5, rng_=rr, jitter=0.15)
    # shimenawa: a twisted straw rope round the octagon's frame with four paper streamers hanging from it
    rp = []
    for i in range(49):
        t = i / 48.0
        a = TAU * t + 0.2
        rr_ = R - 0.2 + 0.5 * math.sin(t * TAU * 6.0)
        rp.append(V((rr_ * math.cos(a) * 0.98, cy + rr_ * math.sin(a) * 0.98, 1.6 + 0.4 * math.cos(t * TAU * 6.0))))
    K.tube("rope", rp, 0.55, P["straw"], sides=6, sub=1, part="rope")
    for k, a in enumerate((205, 250, 295, 335)):
        a = math.radians(a)
        top = V((R * math.cos(a), cy + R * math.sin(a), 1.8))
        path = [top + V((0.8 * math.sin(j * 1.6) * (1 if k % 2 else -1), -j * 2.4, 0.5 * math.cos(j * 1.6))) for j in range(7)]
        K.ribbon("shide%d" % k, path, 2.4, P["paper"], facing=(0, 0, 1), thick=0.06, part="paper", fray=0.5, rng_=rr, edge_wave=0.15)
    return K.quad_estimate()


# ================================================================ Envy series (shared pieces)
def envy_pal(level):
    P = {}
    g = {1: (60, 200, 110), 2: (70, 220, 90), 3: (50, 255, 120)}[level]
    P["glass"] = K.surface("env_glass", rgb(*g), 0.0, 0.05, emit=rgb(*g), emit_strength=0.45, emit_flicker=0.7, mottle=1.0, mottle_color=rgb(210, 255, 200), seed=801 + level, bevel=0.0,
                           rim=(rgb(220, 255, 220), 0.6), scratches=0.6, veins=0.55, vein_color=rgb(10, 40, 20), vein_strength=0.0)
    P["void"] = K.surface("env_void", rgb(6, 14, 10), 0.4, 0.15, emit=rgb(*g), emit_strength=0.5, emit_flicker=0.9, veins=0.7, vein_color=rgb(*g), vein_strength=1.1, mottle=0.6, mottle_color=rgb(10, 40, 24), seed=810,
                          bevel=0.0)
    P["frame"] = Lb.metal("env_frame", rgb(34, 72, 48), rgb(150, 230, 150), 802, rough=0.4, mottle=rgb(10, 30, 18), dirt=0.85, dirt_col=(0.0, 0.01, 0.0, 1), bump="hammer", bs=0.2)
    P["dark"] = Lb.metal("env_dark", rgb(18, 28, 22), rgb(90, 140, 100), 803, rough=0.45, dirt=0.7, dirt_col=(0, 0, 0, 1), bump="hammer", bs=0.2)
    P["claw"] = Lb.paint("env_claw", rgb(150, 178, 120), rgb(230, 250, 190), 804, rough=0.55, dirt_col=(0.03, 0.05, 0.01, 1), mottle=rgb(70, 100, 50), bump="leather")
    P["vine"] = K.surface("env_vine", rgb(40, 96, 48), 0.0, 0.7, bump="grain", bump_scale=1.4, bump_strength=0.8, groove=rgb(6, 24, 10), groove_amount=0.8, dirt=0.7, mottle=0.8, mottle_color=rgb(90, 150, 60), fine=0.6,
                          seed=805, bevel=0.0)
    P["thorn"] = Lb.paint("env_thorn", rgb(96, 70, 40), rgb(210, 190, 120), 806, rough=0.6, mottle=rgb(40, 28, 14))
    P["ooze"] = Lb.glow("env_ooze", rgb(*g), 0.7, 807, flicker=0.8, ramp=("y", 20.0, 60.0, [(0.0, rgb(40, 180, 80)), (0.5, rgb(100, 255, 120)), (1.0, rgb(190, 255, 180))]))
    P["sclera"] = K.surface("env_sclera", rgb(224, 226, 150), 0.0, 0.25, mottle=0.9, mottle_color=rgb(250, 250, 200), veins=1.1, vein_color=rgb(170, 40, 40), fine=0.4, seed=808, bevel=0.0, dirt=0.4, dirt_color=(0.1, 0.1, 0.0, 1))
    P["iris"] = Lb.glow("env_iris", rgb(*g), 1.0, 809, flicker=0.6, ramp=("y", 38.0, 70.0, [(0.0, rgb(30, 150, 60)), (0.5, rgb(90, 255, 90)), (1.0, rgb(200, 255, 120))]))
    P["pupil"] = Lb.paint("env_pupil", rgb(2, 6, 4), rgb(20, 40, 24), 811, rough=0.2)
    P["lid"] = Lb.paint("env_lid", rgb(70, 120, 70), rgb(190, 240, 170), 812, rough=0.6, dirt_col=(0.0, 0.02, 0.0, 1), mottle=rgb(24, 56, 30), bump="leather")
    P["grip_core"] = Lb.paint("env_core", rgb(22, 40, 28), rgb(110, 150, 120), 813, bump="leather")
    P["grip_wrap"] = P["vine"]
    P["trim"] = P["frame"]
    P["drop"] = P["glass"]
    return P


def hand_claws(prefix, P, rr, base, outward, n=4, length=7.0, curl=4.5, r=0.5, spread=1.7):
    """A clawed hand gripping over the rim: a bony palm and `n` curling fingers (3-joint tapered), the fingertips hooking toward +z over the glass."""
    b = V(base)
    o = V(outward).normalized()
    K.blob(prefix + "_palm", b + o * 0.2, (1.8, 1.5, 1.2), P["claw"], seed=3, lump=0.15, rings=5, segs=8, part="hands")
    side = o.cross(V((0, 0, 1)))
    if side.length < 0.1:
        side = V((1, 0, 0))
    side.normalize()
    for i in range(n):
        off = (i - (n - 1) / 2.0) * spread * 0.85
        f0 = b + side * off
        ln = length * (0.85 + 0.15 * math.sin(i * 1.3 + 1))
        pts = [f0, f0 - o * ln * 0.3 + V((0, 0, 1.0)), f0 - o * ln * 0.62 + V((0, 0, 2.6 + curl * 0.3)), f0 - o * ln * 0.9 + V((0, 0, 3.4 + curl * 0.8)), f0 - o * ln * 0.98 + V((0, 0, 2.0 + curl * 0.4))]
        K.tapered("%s_f%d" % (prefix, i), pts, r * rr.uniform(0.9, 1.15), 0.06, P["claw"], sides=5, sub=2, part="hands")


def head_frame_envy(P, rr, cy, rx, ry, level):
    """The jagged frame of the Envy mirrors: a dark torus, a ring of green shards (broken glass) pointing out at different lengths, and a back plate."""
    K.plate("back", ell(0, cy, rx - 0.6, ry - 0.6, 36, jit=0.05, rng_=rr), -1.8, -0.5, P["dark"], part="back_plate", bevel_w=0.2)
    Lb.ring_torus("rim", (0, cy, 0.1), rx - 0.8, 1.2, P["frame"], "head_rim", sides=7, segs=40, squash=ry / rx)
    n = 15 + 2 * level
    for i in range(n):
        a = TAU * i / n + rr.uniform(-0.08, 0.08)
        if math.sin(a) < -0.8:
            continue
        sz = rr.uniform(1.0, 1.8) * (1.0 + 0.25 * level)
        base = V(((rx + 0.2) * math.cos(a), cy + (ry + 0.2) * math.sin(a), rr.uniform(-0.3, 0.5)))
        d = V((math.cos(a), math.sin(a) * 1.0, rr.uniform(-0.15, 0.2)))
        K.shard("sh%d" % i, base + d * (sz * 1.6), sz, P["glass"], rr, part="shards", sides=5, stretch=rr.uniform(2.2, 3.6), dir_=d)


# ================================================================ 4. ENVIOUS GLASS
def envious_glass(opts):
    C.begin(opts)
    C.weights(head_glass=1.2, hands=1.1, shards=0.9, drips=0.8, eyes=1.0)
    P = envy_pal(1)
    rr = K.rng(7804)
    cy, rx, ry = 52.0, 12.0, 15.0
    handle(P, rr, thorns=True, wrap_turns=5.0)
    head_frame_envy(P, rr, cy, rx, ry, 1)
    K.plate("glass", ell(0, cy, rx - 1.6, ry - 1.6, 36, jit=0.04, rng_=rr), -0.5, 0.5, P["glass"], part="head_glass", bevel_w=0.12)
    # cracks across the glass (bake-only dark seams radiating from an off-centre impact) and a slit eye above it
    for k in range(9):
        a = TAU * k / 9 + rr.uniform(-0.2, 0.2)
        pts = [(3.0 + 0.8 * j * math.cos(a) + rr.uniform(-0.25, 0.25), cy - 2.0 + 0.8 * j * math.sin(a) + rr.uniform(-0.25, 0.25)) for j in range(1, 11)]
        K.seam("crack%d" % k, pts, P["dark"], w=0.12, depth=0.15, z=0.5, part="cracks", detail=True)
    Lb.eye("crown_eye", V((0, cy + ry + 3.2, 1.6)), 2.6, V((0, 0.05, 1.0)), P["sclera"], P["iris"], P["pupil"], P["frame"], part="eyes", rng_=rr)
    # four clawed hands grab the rim from behind and hook over the glass
    for k, (x, y, ox, oy) in enumerate(((-rx + 0.6, cy + 4.0, -1.0, 0.25), (rx - 0.6, cy + 6.0, 1.0, 0.2), (-rx * 0.7, cy - ry + 3.0, -0.5, -0.9), (rx * 0.75, cy - ry + 3.0, 0.5, -0.9))):
        hand_claws("hand%d" % k, P, rr, V((x, y, -0.4)), V((ox, oy, 0.0)), n=4, length=7.5, curl=4.0 + (k % 2), r=0.55)
    for k, x in enumerate((-8.0, -3.0, 4.5, 9.0)):                                            # ooze drips hang from the lower rim
        K.drip("drip%d" % k, V((x, cy - ry + 0.4 - (0.5 if k % 2 else 0), 0.3)), 6.0 + 2.0 * (k % 3), 1.0, P["ooze"], seed=k + 3, part="drips")
    K.vine("vine", [V(p) for p in ((-3.0, 30.0, 1.8), (-6.4, 40.0, 1.2), (-10.4, 49.0, 1.6), (-12.4, 58.0, 1.0), (-8.0, 66.0, 1.4))], 0.55, P["vine"], leaves=5, leaf_mat=P["lid"], rng_=rr, leaf_len=3.4, part="vines")
    Lb.fur("rthorn", [V((-12.0 + 0.1 * k, 38.0 + 4.2 * k, 1.0)) for k in range(6)], [V((-1.0, 0.3, 0.4)) for k in range(6)], 2.6, 0.4, P["thorn"], rr, part="vines")
    return K.quad_estimate()


# ================================================================ 5. GREEN-EYED MIRROR
def green_eyed(opts):
    C.begin(opts)
    C.weights(eyeball=1.2, hands=0.9, shards=0.8, lids=1.0, eyes=0.9)
    P = envy_pal(2)
    rr = K.rng(7905)
    cy, rx, ry = 52.0, 12.6, 15.4
    handle(P, rr, thorns=True, wrap_turns=5.0)
    head_frame_envy(P, rr, cy, rx, ry, 2)
    # the eyeball: a veined globe filling the frame, a big glowing green iris, a slit pupil, a bright rim of gel, and lids with lashes
    K.blob("eyeball", (0.0, cy, 0.0), (rx - 1.4, ry - 1.4, 5.4), P["sclera"], seed=5, lump=0.03, rings=10, segs=16, part="eyeball", subsurf=0)
    K.mk("iris_dome", *_iris(0.0, cy, 6.3, 5.6, P), P["iris"], part="iris", smooth=True)
    K.plate("pupil", ell(0.0, cy, 0.9, 3.8, 14), 5.35, 5.9, P["pupil"], part="iris", bevel_w=0.1)
    for sgn in (1, -1):                                                                       # eyelids: curved plates with a row of lashes
        pts = []
        for i in range(15):
            u = -1.0 + 2.0 * i / 14.0
            x = u * (rx - 0.4)
            y = cy + sgn * (ry * 0.34 + (1.0 - u * u) * 3.2 * 1.0)
            pts.append(V((x, y, 2.0 + (1 - u * u) * 3.4)))
        rows = []
        for p in pts:
            rows.append((p + V((0, sgn * 2.2, -0.5)), p + V((0, -sgn * 0.0, 0.4))))
        K.strip("lid%d" % sgn, rows, P["lid"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.5, part="lids", up=(0, 0, 1))
        for i in range(8):
            u = -0.8 + 1.6 * i / 7.0
            p = pts[int((u + 1) / 2 * 14)]
            Lb.claw("lash%d_%d" % (sgn, i), p + V((0, sgn * 1.4, 0)), V((u * 0.6, sgn * 1.0, 0.4)), 3.0 * (1.1 - abs(u) * 0.5), 0.3, P["dark"], curl=0.8, side=(0, 0, 1), part="lashes", sides=4)
    for k, (x, y, r_) in enumerate(((-rx + 0.4, cy + 5.0, 1.7), (rx - 0.4, cy - 3.0, 1.9), (rx - 1.0, cy + 9.5, 1.3), (-rx + 1.0, cy - 8.5, 1.4))):   # small envious eyes on the frame
        Lb.eye("seye%d" % k, V((x, y, 2.6)), r_, V((0.1 * (1 if x > 0 else -1), 0, 1.0)), P["sclera"], P["iris"], P["pupil"], P["frame"], part="eyes", rng_=rr)
    for k, (x, y, ox, oy) in enumerate(((-rx + 0.2, cy + 2.0, -1.0, 0.1), (rx - 0.2, cy + 1.0, 1.0, 0.1))):
        hand_claws("hand%d" % k, P, rr, V((x, y, -0.4)), V((ox, oy, 0.0)), n=3, length=6.5, curl=3.4, r=0.5)
    for k, x in enumerate((-6.0, 1.0, 7.0)):
        K.drip("drip%d" % k, V((x, cy - ry + 0.4, 0.3)), 5.0 + 2.0 * (k % 2), 0.9, P["ooze"], seed=k + 7, part="drips")
    K.vine("vine", [V(p) for p in ((3.0, 30.0, 1.8), (6.4, 40.0, 1.2), (11.4, 48.0, 1.6), (12.8, 60.0, 1.0), (7.0, 70.0, 1.4))], 0.55, P["vine"], leaves=6, leaf_mat=P["lid"], rng_=rr, leaf_len=3.4, part="vines")
    return K.quad_estimate()


def _iris(cx, cy, zc, r, P):
    """Verts / faces of a shallow dome ring (iris): a funnel from the pupil out to the iris edge."""
    verts, faces = [], []
    prof = [(0.9, zc - 0.6), (2.0, zc - 0.2), (3.4, zc - 0.5), (r, zc - 1.6), (r + 0.6, zc - 2.2)]
    n = 20
    for r_, z in prof:
        for k in range(n):
            a = TAU * k / n
            verts.append((cx + r_ * math.cos(a), cy + r_ * math.sin(a) * 1.0, z))
    for i in range(len(prof) - 1):
        for k in range(n):
            a, b = i * n + k, i * n + (k + 1) % n
            faces.append((a, b, b + n, a + n))
    return verts, faces


# ================================================================ 6. MIRROR OF ENVY
def mirror_of_envy(opts):
    C.begin(opts)
    C.weights(head_glass=1.2, tendrils=1.0, hands=1.0, horns=1.0, eyes=0.9)
    P = envy_pal(3)
    rr = K.rng(8006)
    cy, rx, ry = 52.0, 12.8, 16.0
    handle(P, rr, thorns=True, wrap_turns=5.0)
    K.plate("back", ell(0, cy, rx - 0.6, ry - 0.6, 36), -2.0, -0.5, P["dark"], part="back_plate", bevel_w=0.2)
    K.plate("glass", ell(0, cy, rx - 2.0, ry - 2.0, 36), -0.5, 0.5, P["void"], part="head_glass", bevel_w=0.12)
    # the frame is a nest of three writhing tendrils spiralling round the oval, thick at the bottom and thorned
    for t in range(3):
        pts = []
        ph = TAU * t / 3
        for i in range(37):
            u = i / 36.0
            a = ph + TAU * (u * 1.0)
            wob = 1.0 + 0.09 * math.sin(a * 5 + t * 1.7) + 0.05 * rr.uniform(-1, 1)
            pts.append(V(((rx - 0.8) * wob * math.cos(a), cy + (ry - 0.8) * wob * math.sin(a), 0.5 + 0.9 * math.sin(a * 3 + t) )))
        K.tube("tend%d" % t, pts + [pts[0], pts[1]], 1.0 - 0.12 * t, P["vine"] if t % 2 else P["frame"], sides=6, sub=1, part="tendrils", bumps=0.25, seed=t)
    for i in range(10):                                                                       # thorns along the frame
        a = TAU * i / 10 + rr.uniform(-0.1, 0.1)
        if math.sin(a) < -0.82:
            continue
        b = V(((rx + 0.1) * math.cos(a), cy + (ry + 0.1) * math.sin(a), rr.uniform(-0.2, 1.0)))
        Lb.claw("thorn%d" % i, b, V((math.cos(a), math.sin(a), rr.uniform(-0.2, 0.3))), rr.uniform(2.4, 4.2), 0.5, P["thorn"], curl=rr.uniform(-1, 1), side=(-math.sin(a), math.cos(a), 0), part="tendril_thorns", sides=4)
    # a horned crown: seven curved horns / thorns of different heights above the frame, the middle one tallest (top stays under y 86)
    for i, (x, h, lean) in enumerate(((-9.5, 7.0, -0.6), (-6.4, 11.0, -0.35), (-3.2, 14.5, -0.15), (0.0, 17.0, 0.0), (3.4, 13.6, 0.2), (6.6, 10.4, 0.4), (9.6, 6.5, 0.65))):
        base = V((x, cy + ry * math.sqrt(max(0.05, 1 - (x / rx) ** 2)) - 0.6, 0.5))
        pts = [base, base + V((lean * h * 0.15, h * 0.35, 0.4)), base + V((lean * h * 0.45, h * 0.7, 0.0)), base + V((lean * h * 0.9, h * 0.98, -0.6 + 0.3 * (i % 3)))]
        K.tapered("horn%d" % i, pts, 1.1 - 0.05 * abs(i - 3), 0.07, P["claw"] if i % 2 else P["dark"], sides=6, sub=2, part="horns")
    # a ring of eyes inside the frame, all turned toward whoever looks in, and one big eye above
    for k in range(3):
        a = math.radians(-60 + 200 * k / 2)
        Lb.eye("eye%d" % k, V(((rx - 2.2) * math.cos(a + 0.0) * 0.98, cy + (ry - 2.2) * math.sin(a) * 0.98, 1.8)), 1.35 + 0.15 * (k % 3), V((-math.cos(a) * 0.15, -math.sin(a) * 0.15, 1.0)), P["sclera"], P["iris"],
               P["pupil"], P["frame"], part="eyes", rng_=rr)
    Lb.eye("big_eye", V((0.0, cy + ry + 2.6, 1.8)), 3.0, V((0, 0.05, 1.0)), P["sclera"], P["iris"], P["pupil"], P["frame"], part="eyes", rng_=rr)
    for k, (x, y, ox, oy) in enumerate(((-rx + 0.2, cy + 7.0, -1.0, 0.2), (rx - 0.2, cy + 8.0, 1.0, 0.2), (-rx * 0.65, cy - ry + 2.5, -0.5, -0.9))):
        hand_claws("hand%d" % k, P, rr, V((x, y, -0.4)), V((ox, oy, 0.0)), n=3, length=7.0, curl=4.0, r=0.5)
    # shackle chains hang from the lower frame, ending in an open cuff; bounded
    for sx in (-1, 1):
        path = [V((sx * 7.0, cy - ry + 0.6, 0.6)), V((sx * 10.2, cy - ry - 5.0, 0.8)), V((sx * 8.4, cy - ry - 11.0, 0.2))]
        K.chain("chain%d" % sx, path, 2.4, 1.6, 0.3, P["dark"], max_links=5, part="chains", sides=4, segs=6)
        Lb.ring_torus("cuff%d" % sx, (sx * 8.0, cy - ry - 13.4, 0.0), 2.2, 0.55, P["frame"], "chains", sides=5, segs=10)
    for k, x in enumerate((-4.0, 3.0, 8.0)):
        K.drip("drip%d" % k, V((x, cy - ry + 0.4, 0.3)), 5.5 + 2.0 * (k % 2), 0.95, P["ooze"], seed=k + 11, part="drips")
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["tiger"] = (white_tiger_mirror, "vassal_mirror__white_tiger_clone_mirror", "white_tiger_clone_mirror")
BUILDERS["hetsu"] = (hetsu_kagami, "vassal_mirror__hetsu_kagami", "hetsu_kagami")
BUILDERS["yata"] = (yata_mirror, "vassal_mirror__yata_mirror", "yata_mirror")
BUILDERS["envious"] = (envious_glass, "vassal_mirror__envious_glass", "envious_glass")
BUILDERS["greeneyed"] = (green_eyed, "vassal_mirror__green_eyed_mirror", "green_eyed_mirror")
BUILDERS["envy"] = (mirror_of_envy, "vassal_mirror__mirror_of_envy", "mirror_of_envy")

if __name__ == "__main__":
    main(sys.argv[1:], BUILDERS, BASE, "shield", MANIFEST)
