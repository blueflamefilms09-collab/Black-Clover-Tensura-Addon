"""Weapon-form models of the VASSAL BOOK (base vassal_book, size class handitem), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_book.py --only pipe [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids: vassal_book__pipe_book, __animal_needle_book, __revised_edition, __library_tome.
Every form keeps the base book's frame (mesh space of vassal_book: the cover is the xy plane x -13.1..13.7, y 0..36, +z the front, the spine on -x, loose pages up to y 45):
same hand, same size class.  Designs: Pipe Book = a book bound in iron pipes, chains and a padlock (the cage), Animal Needle Book = a leather bestiary bristling with needles,
feathers and talons, Revised Edition = a thicker crimson-and-gold copy with six fanned pages and six ribbons, Library Tome = a whole library in one book (arched window
of shelves, columns, ladder, lantern, spines of a hundred books on its side).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_vassal_common_b as C  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import TAU, rgb  # noqa: E402
from forms_vassal_common_b import V  # noqa: E402
from mathutils import Matrix  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
C.setup("vassal_book", "handitem", os.path.join(HERE, "forms_manifest_vassal_book.json"), "vassal_book", glow=("FFFFFF", 0.14, 0.45, 1.0))
mat, glowmat = C.mat, C.glowmat
X0, X1, Y0, Y1 = -13.1, 13.7, 0.0, 36.0


def body(P, th=4.2, cover="cover", pages="paper", spine="spine", bands="gold", n_bands=6, band_r=3.2):
    """The book's block: two covers, the page block, a rounded spine (parts keep the base's names)."""
    for nm, s in (("cover_front", 1), ("cover_back", -1)):
        K.plate(nm, [(X0, Y0), (X1, Y0), (X1, Y1), (X0, Y1)], min(s * (th - 0.9), s * th), max(s * (th - 0.9), s * th), P[cover], part="cover", bevel_w=0.22)
    K.plate("pages", [(-12.1, 0.7), (12.9, 0.7), (12.9, 35.3), (-12.1, 35.3)], -(th - 0.8), th - 0.8, P[pages], part="pages")
    def dsec(r, w, y):
        w = w - 0.5                                                                                  # stay just under the cover plane (no coplanar black faces)
        return [V(-12.3 + r * math.cos(math.radians(a)), y, w * math.sin(math.radians(a))) for a in range(90, 271, 20)] + [V(-10.1, y, -w), V(-10.1, y, w)]
    C.loft("spine", [dsec(2.6, th, 0.0), dsec(2.6, th, 36.0)], P[spine], "spine")
    for i in range(n_bands):
        y = 2.2 + 6.3 * i
        C.loft("band%d" % i, [dsec(band_r, th + 0.9, y - 0.75), dsec(band_r, th + 0.9, y + 0.75)], P[bands], "spine_bands")


def page(name, x, top, lean, tilt, P, ink, w=8.8, h=12.4, lines=5, part="loose_pages"):
    """A loose sheet standing out of the book (leaning `lean` deg about z, `tilt` deg about x), its top at y = top: paper + glowing script (bake-only)."""
    obs = [K.plate(name, [(-w / 2, 0.0), (w / 2, 0.0), (w / 2 - 0.4, h), (-w / 2 + 0.4, h - 0.5)], -0.12, 0.12, P["paper"], part=part, bevel_w=0.04)]
    for k in range(lines):
        for zz in (0.13, -0.13):
            y = 2.0 + (h - 3.5) * k / max(lines - 1, 1)
            z0, z1 = sorted((zz, zz * 1.4))
            obs.append(K.plate("%s_l%d%d" % (name, k, zz > 0), [(-w * 0.36, y), (w * 0.36 - (k % 2) * 1.6, y), (w * 0.36 - (k % 2) * 1.6, y + 0.45), (-w * 0.36, y + 0.45)], z0, z1, ink, part=part + "_ink", detail=True))
    M = Matrix.Rotation(math.radians(tilt), 4, "X") @ Matrix.Rotation(math.radians(lean), 4, "Z")
    for o in obs:
        K.xf(o, M)
    top_now = max(v.co.y for v in obs[0].data.vertices)
    for o in obs:
        K.xf(o, Matrix.Translation(V(x, top - top_now, 1.4)))
    return obs


def corner_cap(name, ex, ey, P, mat_, size=7.4, th=4.2, z0=0.0, part="corner_caps", rr=None):
    sx, sy = (1 if ex < 0 else -1), (1 if ey < 1 else -1)
    outl = [(ex, ey), (ex + sx * size, ey), (ex + sx * size * 0.84, ey + sy * 1.6), (ex + sx * 2.9, ey + sy * 2.9), (ex + sx * 1.6, ey + sy * size * 0.84), (ex, ey + sy * size)]
    for s in (1, -1):
        K.stepped_plate("%s_%d" % (name, s), outl, (th - 0.1) * s, mat_, steps=2, step_h=0.5, inset=0.7, part=part, up=s, bevel_w=0.1)
    return outl


# ================================================================ 1. PIPE BOOK  (iron pipes, chains and a padlock: the cage)
def pipe_palette():
    P = {}
    P["cover"] = mat("pb_cover", (58, 66, 84), 1.0, 0.42, bump="hammer", bump_scale=0.7, bump_strength=0.35, groove=rgb(18, 22, 30), groove_amount=0.6, dirt=0.85, mottle_color=rgb(40, 32, 28))
    P["spine"] = mat("pb_spine", (90, 100, 116), 1.0, 0.38, bump="brushed", bump_strength=0.15, dirt=0.8)
    P["pipe"] = mat("pb_pipe", (126, 136, 150), 1.0, 0.32, bump="brushed", bump_strength=0.15, dirt=0.8, scratches=0.5, mottle_color=rgb(60, 50, 42))
    P["brass"] = mat("pb_brass", (200, 150, 64), 1.0, 0.3, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.7)
    P["paper"] = mat("pb_paper", (232, 222, 192), 0.0, 0.8, bump="brushed", bump_strength=0.5, groove=rgb(160, 130, 86), groove_amount=0.6, dirt=0.6, wear=0.2, wear_metal=0.0, bevel=0.0)
    P["chain"] = mat("pb_chain", (150, 158, 170), 1.0, 0.34, bump="hammer", bump_strength=0.15, dirt=0.8, scratches=0.6)
    P["face"] = mat("pb_face", (236, 232, 218), 0.0, 0.5, dirt=0.4, wear=0.1, bevel=0.0)
    P["lit"] = glowmat("pb_lit", (90, 205, 255), 0.85, 0.6)
    P["needle"] = glowmat("pb_needle", (255, 120, 70), 1.0, 0.3)
    P["cloth"] = mat("pb_cloth", (150, 36, 40), 0.0, 0.7, bump="cross", bump_strength=0.5, dirt=0.6, wear_metal=0.0, bevel=0.0)
    return P


def pipe_book(opts):
    C.begin(opts)
    C.weights(cover=1.1, cage=1.2, chains=1.0, lock=1.2, pages=0.9, spine=0.9, spine_bands=0.8, flanges=0.8)
    P = pipe_palette()
    rr = K.rng(7101)
    body(P, th=4.2, n_bands=6, bands="brass")
    ZF = 5.6
    # the cage: a pipe frame round the front, three upright pipes and four cross pipes, each crossing clamped with a hex nut
    frame = [V(X0 + 1.4, Y0 + 1.4, ZF), V(X1 - 1.0, Y0 + 1.4, ZF), V(X1 - 1.0, Y1 - 1.4, ZF), V(X0 + 1.4, Y1 - 1.4, ZF), V(X0 + 1.4, Y0 + 1.4, ZF)]
    for i in range(4):
        K.tube("frame%d" % i, [frame[i], (frame[i] + frame[i + 1]) * 0.5, frame[i + 1]], 1.15, P["pipe"], sides=8, sub=1, part="cage")
        K.bl.lathe("elbow%d" % i, [(0.01, -1.4), (1.5, -1.0), (1.5, 1.0), (0.01, 1.4)], P["brass"], sides=8, origin=tuple(frame[i + 1]), axis="z", part="cage_joints")
    xs = (-7.0, 0.3, 7.6)
    ys = (7.0, 14.5, 22.0, 29.0)
    for x in xs:
        K.tube("up%d" % int(x * 10), [V(x, Y0 + 2.0, ZF), V(x + rr.uniform(-0.3, 0.3), 18.0, ZF), V(x, Y1 - 2.0, ZF)], 0.85, P["pipe"], sides=8, sub=2, part="cage")
    for y in ys:
        K.tube("cross%d" % int(y), [V(X0 + 2.0, y, ZF + 1.5), V(0.5, y + rr.uniform(-0.3, 0.3), ZF + 1.5), V(X1 - 1.5, y, ZF + 1.5)], 0.85, P["pipe"], sides=8, sub=2, part="cage")
        for x in xs:
            K.rivets("nut%d_%d" % (int(y), int(x * 10)), [(V(x, y, ZF + 2.4), (0, 0, 1))], P["brass"], r=1.45, sides=6, rng_=rr, height=0.7, detail=False)
    # two chains crossing the cover corner to corner, and a heavy padlock where they meet
    K.chain("chainA", [V(X0 + 2.0, Y0 + 3.0, 6.6), V(0.3, 18.0, 7.2), V(X1 - 1.5, Y1 - 3.0, 6.6)], 5.4, 3.4, 0.7, P["chain"], max_links=9, part="chains", up=(0, 0, 1))
    K.chain("chainB", [V(X1 - 1.5, Y0 + 3.0, 6.6), V(0.3, 18.0, 7.6), V(X0 + 2.0, Y1 - 3.0, 6.6)], 5.4, 3.4, 0.7, P["chain"], max_links=9, part="chains", up=(0, 0, 1))
    c = V(0.3, 18.0, 9.2)
    K.bar("lock_body", c + V(-3.8, -3.4, 0), c + V(3.8, -3.4, 0), 3.0, 2.2, P["brass"], part="lock", bevel_w=0.3)
    C.loft("lock_box", [C.ring_z(z, 3.8, 3.4, c.x, c.y - 1.0, 0.8) for z in (c.z - 2.0, c.z + 2.0)], P["brass"], "lock")
    K.tube("shackle", [c + V(-2.4, 2.0, 0), c + V(-2.4, 6.0, 0), c + V(0.0, 7.6, 0), c + V(2.4, 6.0, 0), c + V(2.4, 2.0, 0)], 0.85, P["pipe"], sides=8, sub=3, part="lock")
    K.plate("keyhole", [(-0.7, -0.6), (0.7, -0.6), (1.0, 0.8), (-1.0, 0.8)], c.z + 1.95, c.z + 2.5, P["lit"], part="lock_glow")
    K.bl.lathe("keyhole_o", [(0.01, 0.0), (1.1, 0.0), (1.1, 0.55), (0.01, 0.55)], P["lit"], sides=8, origin=(c.x, c.y + 1.0, c.z + 1.95), axis="z", part="lock_glow")
    # a pressure gauge at the head of the cage
    g = V(7.4, 31.2, 6.6)
    K.bl.lathe("gauge", [(0.01, 0.0), (3.1, 0.0), (3.3, 0.9), (2.7, 1.6), (0.01, 1.6)], P["brass"], sides=14, origin=tuple(g), axis="z", part="gauge", caps=(False, True))
    K.bl.lathe("gauge_face", [(0.01, 1.55), (2.5, 1.55)], P["face"], sides=14, origin=tuple(g), axis="z", part="gauge", caps=(False, True))
    K.plate("gauge_needle", [(-0.22, -0.5), (0.22, -0.5), (0.1, 2.1), (-0.1, 2.1)], g.z + 1.6, g.z + 1.95, P["needle"], part="gauge_needle")
    # flanges where the cage meets the spine, and a pipe flange cap at the foot
    for y in (3.0, 33.0):
        C.loft("spine_fl%d" % y, [K.bl.ring(yy, K.bl.circle(r, 12)) for yy, r in ((y - 0.9, 0.01), (y - 0.9, 5.4), (y + 0.9, 5.4), (y + 0.9, 0.01))], P["pipe"], "flanges", caps=(False, False))
    # loose pages pinned under the top pipe, a ribbon of red cloth at the foot
    for i, (px, lean, tilt, top) in enumerate(((-6.4, 14.0, 8.0, 44.4), (0.8, -5.0, 12.0, 45.3), (7.0, -18.0, 6.0, 43.4))):
        page("pg%d" % i, px, top, lean, tilt, P, P["lit"])
    K.strip("ribbon", [(V(-4.4, 0.9, 0.0), V(-2.2, 0.9, 0.0)), (V(-4.4, -4.0, 0.0), V(-2.2, -4.0, 0.0)), (V(-4.4, -8.9, 0.0), V(-2.2, -7.4, 0.0))], P["cloth"], (-1.0, 0.0, 1.0), (0.4, 1.0, 0.4), 0.14, part="ribbon", up=(0, 0, 1))
    return K.quad_estimate()


# ================================================================ 2. ANIMAL NEEDLE BOOK  (a bestiary: feathers, needles, talons)
def needle_palette():
    P = {}
    P["cover"] = mat("nb_cover", (74, 78, 40), 0.0, 0.6, bump="leather", bump_scale=1.2, bump_strength=0.7, groove=rgb(24, 26, 10), groove_amount=0.7, dirt=0.8, wear=0.6, wear_color=rgb(170, 160, 100), wear_metal=0.0, mottle=0.9, mottle_color=rgb(120, 76, 36))
    P["spine"] = mat("nb_spine", (120, 80, 40), 0.0, 0.6, bump="leather", bump_strength=0.6, groove=rgb(40, 24, 10), groove_amount=0.8, dirt=0.8, wear_metal=0.0)
    P["bone"] = mat("nb_bone", (222, 208, 172), 0.0, 0.5, bump="grain", bump_scale=1.5, bump_strength=0.4, groove=rgb(110, 90, 60), groove_amount=0.7, dirt=0.8, wear=0.5, wear_metal=0.0)
    P["paper"] = mat("nb_paper", (226, 214, 176), 0.0, 0.8, bump="brushed", bump_strength=0.5, groove=rgb(150, 120, 76), groove_amount=0.6, dirt=0.7, wear=0.2, wear_metal=0.0, bevel=0.0)
    P["needle"] = mat("nb_needle", (210, 218, 224), 1.0, 0.22, bump="brushed", bump_strength=0.05, dirt=0.3, scratches=0.3, bevel=0.0)
    P["gold"] = mat("nb_gold", (206, 160, 62), 1.0, 0.3, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.8)
    P["feather"] = mat("nb_feather", (224, 214, 190), 0.0, 0.55, wear=0.3, wear_metal=0.0, dirt=0.4, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(110, 90, 60)), (0.5, rgb(222, 208, 176)), (1.0, rgb(120, 190, 90))],
                       mottle=0.6, mottle_color=rgb(130, 150, 80), bevel=0.06, fine=0.4)
    P["feather2"] = mat("nb_feather2", (96, 150, 70), 0.0, 0.55, wear=0.3, wear_metal=0.0, dirt=0.4, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(50, 70, 30)), (0.5, rgb(100, 160, 74)), (1.0, rgb(230, 230, 150))],
                        mottle=0.6, mottle_color=rgb(50, 100, 40), bevel=0.06, fine=0.4)
    P["lit"] = glowmat("nb_lit", (190, 255, 110), 0.85, 0.6)
    P["thread"] = glowmat("nb_thread", (255, 200, 90), 0.8, 0.5)
    return P


def needle_book(opts):
    C.begin(opts)
    C.weights(cover=1.1, needles=1.2, feathers=1.2, talons=1.0, pages=0.9, spine=0.9, spine_bands=0.7, eye=1.0)
    P = needle_palette()
    rr = K.rng(7102)
    body(P, th=4.2, spine="spine", bands="bone", n_bands=5, band_r=3.0)
    # talons gripping the four corners (bone claws curling over the cover edge, front and back)
    for s, (ex, ey) in enumerate(((X0, Y0), (X1, Y0), (X1, Y1), (X0, Y1))):
        sx, sy = (1 if ex < 0 else -1), (1 if ey < 1 else -1)
        for z in (1, -1):
            for k, (dx, dy) in enumerate(((5.0, 1.0), (3.0, 3.6), (1.0, 5.0))):
                K.tapered("talon%d_%d_%d" % (s, z, k), [V(ex - sx * 0.4, ey - sy * 0.4, 3.0 * z), V(ex + sx * dx * 0.5, ey + sy * dy * 0.5, 5.4 * z), V(ex + sx * dx, ey + sy * dy, 4.6 * z), V(ex + sx * dx * 1.2, ey + sy * dy * 1.2, 3.4 * z)],
                          0.8, 0.08, P["bone"], sides=5, sub=2, part="talons")
    # the bird skull clasp on the fore-edge: a beak and two eye sockets with lit eyes
    sk = V(14.8, 18.0, 0.0)
    K.blob("skull", sk + V(0.4, 0.0, 0.0), (2.6, 4.6, 4.6), P["bone"], seed=2, lump=0.1, rings=7, segs=10, part="skull")
    K.tapered("beak", [sk + V(1.0, 0.0, 0.0), sk + V(5.0, 0.2, 0.0), sk + V(8.4, -1.4, 0.0)], 1.9, 0.12, P["bone"], sides=6, sub=3, part="skull")
    for z in (1, -1):
        K.blob("eye%d" % z, sk + V(2.2, 2.4, 2.9 * z), (0.8, 0.8, 0.8), P["lit"], seed=z + 3, lump=0.0, rings=4, segs=6, part="eye")
    # a feather emblem on the cover, and the plumage fan at the head of the spine
    for s_ in (1, -1):
        K.feather("emblem%d" % s_, V(-6.0, 7.0, 4.5 * s_), V(0.6, 0.8, 0.0), 22.0, 6.4, P["gold"], bend=V(3.0, 0.0, 0.0), curl=1.0 * s_, rows=8, facing=(0, 0, s_), part="emblem", thick=0.18, seed=4, serrate=0.12)
    plume = [(-12.0, 28.0, 108.0, 24.0, 6.4), (-9.0, 30.0, 96.0, 28.0, 7.0), (-4.0, 32.0, 84.0, 31.0, 7.4), (2.0, 33.0, 72.0, 28.0, 7.0), (8.0, 32.0, 56.0, 24.0, 6.4), (12.0, 30.0, 40.0, 19.0, 5.6), (-14.0, 24.0, 124.0, 17.0, 5.6)]
    for i, (x, y, ang, ln, wd) in enumerate(plume):
        a = math.radians(ang)
        K.feather("plume%d" % i, V(x, y, 0.5 * (i % 2) - 0.25), V(math.cos(a), math.sin(a), 0.0), ln, wd, P["feather"] if i % 2 == 0 else P["feather2"], bend=V(-math.sin(a) * 1.6, math.cos(a) * 0.4, 0.0), curl=2.0 * (1 - 2 * (i % 2)),
                  rows=9, serrate=0.2, part="feathers", seed=60 + i, thick=0.18)
    # the needles: steel quills bristling from the spine edge and the top, each with an eye and a thread
    needles = []
    for i in range(16):
        ang = rr.uniform(20.0, 160.0) if i < 10 else rr.uniform(-30.0, 15.0)
        base = V(rr.uniform(-12.5, 10.0), rr.uniform(31.0, 36.0), rr.uniform(-1.0, 1.0)) if i < 10 else V(X1 + 0.2, rr.uniform(4.0, 30.0), rr.uniform(-2.5, 2.5))
        a = math.radians(ang)
        d = V(math.cos(a), math.sin(a), rr.uniform(-0.25, 0.25))
        ln = rr.uniform(12.0, 21.0)
        K.tapered("needle%d" % i, [base, base + d * ln * 0.5, base + d * ln], 0.45, 0.04, P["needle"], sides=4, sub=1, part="needles")
        e = base + d * (ln * 0.2)
        K.bl.lathe("eye%d" % i, [(0.7, -0.15), (0.7, 0.15)], P["gold"], sides=6, origin=tuple(e), axis="z", part="needle_eyes", caps=(False, False))
        if i % 3 == 0:
            K.tube("thread%d" % i, [e, e + V(1.5, -2.5, 0.8), e + V(0.5, -5.5, 1.5)], 0.14, P["thread"], sides=4, sub=2, part="thread")
    for i, (px, lean, tilt, top) in enumerate(((-7.0, 20.0, 6.0, 40.0),)):
        page("pg%d" % i, px, top, lean, tilt, P, P["lit"], w=7.8, h=10.5, lines=4)
    return K.quad_estimate()


# ================================================================ 3. REVISED EDITION  (a thicker crimson-and-gold copy: six pages, six ribbons, a wax-seal star)
def revised_palette():
    P = {}
    P["cover"] = mat("rv_cover", (112, 22, 36), 0.0, 0.5, bump="leather", bump_scale=1.1, bump_strength=0.6, groove=rgb(40, 6, 12), groove_amount=0.7, dirt=0.7, wear=0.55, wear_color=rgb(220, 120, 110), wear_metal=0.0, mottle=0.7, mottle_color=rgb(60, 8, 20))
    P["spine"] = mat("rv_spine", (140, 34, 46), 0.0, 0.5, bump="leather", bump_strength=0.55, groove=rgb(50, 10, 14), groove_amount=0.7, dirt=0.7, wear_metal=0.0)
    P["gold"] = mat("rv_gold", (252, 202, 78), 1.0, 0.24, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.55, mottle_color=rgb(170, 110, 30), wear_color=rgb(255, 245, 200))
    P["gold_dark"] = mat("rv_gold2", (186, 134, 48), 1.0, 0.34, bump="hammer", bump_strength=0.18, dirt=0.8)
    P["paper"] = mat("rv_paper", (240, 232, 204), 0.0, 0.75, bump="brushed", bump_strength=0.5, groove=rgb(176, 146, 96), groove_amount=0.6, dirt=0.4, wear=0.15, wear_metal=0.0, bevel=0.0)
    P["silver"] = mat("rv_silver", (214, 220, 232), 1.0, 0.26, bump="brushed", bump_strength=0.1, dirt=0.4, scratches=0.4)
    P["gem"] = glowmat("rv_gem", (255, 196, 70), 1.0, 0.5, rim=(rgb(255, 240, 190), 0.6))
    P["lit"] = glowmat("rv_lit", (255, 214, 110), 0.85, 0.6)
    for i, col in enumerate(((190, 30, 40), (40, 90, 170), (230, 190, 60), (50, 140, 90), (150, 60, 160), (230, 230, 220))):
        P["rib%d" % i] = mat("rv_rib%d" % i, col, 0.0, 0.6, bump="cross", bump_strength=0.4, dirt=0.5, wear_metal=0.0, bevel=0.0)
    return P


def revised_edition(opts):
    C.begin(opts)
    C.weights(cover=1.1, corners=1.1, seal=1.1, pages=1.0, ribbons=0.8, spine=0.9, spine_bands=0.8, clasp=0.8)
    P = revised_palette()
    rr = K.rng(7103)
    th = 5.4
    body(P, th=th, n_bands=6, band_r=3.4)
    # tall filigreed corner crowns, front and back, each with a real boss
    for s, (ex, ey) in enumerate(((X0, Y0), (X1, Y0), (X1, Y1), (X0, Y1))):
        corner_cap("cap%d" % s, ex, ey, P, P["gold"], size=9.0, th=th, rr=rr)
        sx, sy = (1 if ex < 0 else -1), (1 if ey < 1 else -1)
        for z in (1, -1):
            K.bl.lathe("boss%d_%d" % (s, z), [(1.3, 0.0), (1.3, 0.5), (0.8, 1.1), (0.01, 1.4)], P["gold"], sides=8, origin=(ex + sx * 2.5, ey + sy * 2.5, (th + 0.8) * z), axis="z", part="corner_caps", caps=(False, True))
            K.filigree("fil%d_%d" % (s, z), [(ex + sx * 3.0, ey + sy * 7.0), (ex + sx * 5.0, ey + sy * 5.4), (ex + sx * 5.6, ey + sy * 3.4), (ex + sx * 4.0, ey + sy * 2.8)], P["gold_dark"], w=0.45, h=0.25, z=th + 0.3, side=z, sub=2, part="filigree")
    # the front: a gold frame, a big wax-seal star with a gem and a ring of rays, and a title band of runes
    for s in (1, -1):
        K.stepped_plate("frame_o%d" % s, [(-10.6, 2.2), (11.6, 2.2), (11.6, 33.8), (-10.6, 33.8)], (th - 0.05) * s, P["gold_dark"], steps=1, step_h=0.35, inset=0.0, part="cover_frame", up=s, bevel_w=0.1)
        K.plate("frame_in%d" % s, [(-9.4, 3.4), (10.4, 3.4), (10.4, 32.6), (-9.4, 32.6)], *sorted(((th - 0.1) * s, (th + 0.15) * s)), P["cover"], part="cover_panel")
        star = [(0.6 + (10.0 if i % 2 == 0 else 6.2) * math.cos(TAU * i / 24 + 0.05), 19.0 + (10.0 if i % 2 == 0 else 6.2) * math.sin(TAU * i / 24 + 0.05)) for i in range(24)]
        K.stepped_plate("seal%d" % s, star, (th + 0.1) * s, P["gold"], steps=3, step_h=0.6, inset=0.7, part="seal", up=s, bevel_w=0.0)
        K.gem_setting("gem%d" % s, (0.6, 19.0, (th + 1.9) * s), 4.8, P["gem"], P["gold"], normal=(0, 0, s), claws=8, sides=10, part="seal_gem", rng_=rr, claw_r=0.5)
        for k in range(8):
            a = TAU * (k + 0.5) / 8
            K.rivets("seal_dot%d_%d" % (s, k), [(V(0.6 + 8.3 * math.cos(a), 19.0 + 8.3 * math.sin(a), (th + 1.9) * s), (0, 0, s))], P["lit"], r=0.7, sides=6, rng_=rr, detail=False)
        K.engrave_lines("title%d_" % s, [[(-7.0, 31.0), (8.2, 31.0)], [(-7.0, 6.0), (8.2, 6.0)]], P["lit"], side=s, z=th + 0.15, w=0.14, part="title", detail=True)
        C_ = V(0.6, 31.0, 0.0)
        C.loft("clasp_%d" % s, [C.ring_z(z, 2.4, 3.4, X1 + 1.0, 18.0, 0.8) for z in ((th + 0.6) * s, (th - 1.1) * s)], P["silver"], "clasp")
    path = K.bl.smooth_path([V(11.0, 18.0, th + 0.2), V(14.8, 18.0, th + 0.9), V(16.6, 18.0, 3.0), V(16.9, 18.0, 0.0), V(16.6, 18.0, -3.0), V(14.8, 18.0, -th - 0.9), V(11.0, 18.0, -th - 0.2)], 3)
    K.bl.sweep("strap", path, [(-0.3, -5.0), (0.3, -5.0), (0.3, 5.0), (-0.3, 5.0)], P["silver"], up=(0, 1, 0), part="clasp")
    # six pages fanned above the book, each with glowing script, and six silk ribbons hanging below
    for i in range(6):
        px = -9.0 + 3.6 * i
        page("pg%d" % i, px, 45.3 - 0.6 * abs(2.5 - i), 34.0 - 13.6 * i, 6.0 + 4.0 * (i % 2), P, P["lit"], w=9.4, h=13.0, lines=5)
    for i in range(6):
        x = -10.0 + 4.2 * i
        L = 7.0 + 1.9 * ((i * 3) % 5)
        path = [V(x + 0.15 * k, 1.0 - L * k / 6.0, 0.9 * math.sin(k * 0.8 + i)) for k in range(7)]
        K.ribbon("ribbon%d" % i, path, 2.0, P["rib%d" % i], facing=(0, 0, 1), thick=0.07, part="ribbons", fray=0.5, rng_=rr, edge_wave=0.06, notch=1.4 if i % 2 else 0.0)
    return K.quad_estimate()


# ================================================================ 4. LIBRARY TOME  (a whole library bound in one volume)
def library_palette():
    P = {}
    P["cover"] = mat("lt_cover", (46, 30, 62), 0.0, 0.55, bump="leather", bump_scale=1.1, bump_strength=0.6, groove=rgb(14, 8, 22), groove_amount=0.7, dirt=0.8, wear=0.5, wear_color=rgb(150, 120, 170), wear_metal=0.0, mottle=0.8, mottle_color=rgb(24, 40, 70))
    P["spine"] = mat("lt_spine", (70, 44, 36), 0.0, 0.55, bump="leather", bump_strength=0.6, groove=rgb(24, 12, 8), groove_amount=0.8, dirt=0.8, wear_metal=0.0)
    P["wood"] = mat("lt_wood", (108, 66, 32), 0.0, 0.6, bump="grain", bump_strength=0.7, groove=rgb(44, 24, 10), groove_amount=0.8, dirt=0.8, wear=0.4)
    P["gold"] = mat("lt_gold", (216, 164, 62), 1.0, 0.3, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.7)
    P["stone"] = mat("lt_stone", (150, 146, 156), 0.0, 0.7, bump="hammer", bump_scale=0.5, bump_strength=0.35, groove=rgb(60, 56, 66), groove_amount=0.6, dirt=0.8, wear=0.4, wear_color=rgb(214, 210, 220), wear_metal=0.0, mottle=0.9, mottle_color=rgb(96, 100, 110))
    P["paper"] = mat("lt_paper", (230, 218, 186), 0.0, 0.8, bump="brushed", bump_strength=0.5, groove=rgb(160, 130, 86), groove_amount=0.6, dirt=0.7, wear=0.2, wear_metal=0.0, bevel=0.0)
    P["dark"] = mat("lt_dark", (22, 16, 30), 0.0, 0.8, dirt=0.3, wear=0.0, bevel=0.0)
    P["lit"] = glowmat("lt_lit", (255, 190, 90), 0.95, 0.6)
    P["violet"] = glowmat("lt_violet", (170, 120, 255), 0.85, 0.6)
    for i, col in enumerate(((150, 40, 44), (44, 90, 150), (60, 120, 70), (190, 150, 60), (110, 60, 130), (200, 200, 190), (40, 40, 60))):
        P["bk%d" % i] = mat("lt_bk%d" % i, col, 0.0, 0.6, bump="leather", bump_strength=0.4, dirt=0.7, wear_metal=0.0, bevel=0.06)
    return P


def library_tome(opts):
    C.begin(opts)
    C.weights(cover=1.0, shelves=1.2, columns=1.0, spines=1.1, ladder=0.9, lantern=0.9, pages=0.8, books=1.1)
    P = library_palette()
    rr = K.rng(7104)
    th = 8.0                                                                                      # a fat volume: twice as thick as the base
    body(P, th=th, n_bands=3, band_r=3.4)
    ZF = th + 0.1
    # ---- the front: a stone arch frame round a window full of shelves
    for s in (1, -1):
        arch = [(-10.0, 3.0), (11.0, 3.0), (11.0, 25.0)] + [(0.5 + 10.5 * math.cos(math.radians(a)), 25.0 + 7.4 * math.sin(math.radians(a))) for a in range(0, 181, 20)][1:-1] + [(-10.0, 25.0)]
        K.stepped_plate("arch%d" % s, arch, ZF * s, P["stone"], steps=2, step_h=0.7, inset=0.9, part="arch", up=s, bevel_w=0.1)
        win = [(-8.2, 4.6), (9.2, 4.6), (9.2, 25.0)] + [(0.5 + 8.7 * math.cos(math.radians(a)), 25.0 + 6.0 * math.sin(math.radians(a))) for a in range(0, 181, 20)][1:-1] + [(-8.2, 25.0)]
        K.plate("window%d" % s, win, *sorted(((ZF + 1.3) * s, (ZF + 1.45) * s)), P["dark"], part="window")
        for sh_, y in enumerate((4.8, 11.0, 17.2, 23.0)):                                         # shelves with rows of tiny books (real boxes)
            K.plate("shelf%d_%d" % (s, sh_), [(-8.2, y), (9.2, y), (9.2, y + 0.6), (-8.2, y + 0.6)], *sorted(((ZF + 1.2) * s, (ZF + 2.9) * s)), P["wood"], part="shelves")
            x = -7.8 + rr.uniform(0, 0.5)
            k = 0
            while x < 8.6 and k < 9:
                w = rr.uniform(0.9, 1.6)
                h = rr.uniform(3.2, 5.2) if sh_ < 3 else rr.uniform(2.0, 3.0)
                if sh_ == 3 and not (-6.0 < x < 6.0 + 0.0):
                    h = 2.6
                c = P["bk%d" % rr.randint(0, 6)]
                z0, z1 = sorted(((ZF + 1.3) * s, (ZF + 2.8 - 0.3 * (k % 2)) * s))
                if y + 0.6 + h < 29.0 - 0.0 and x + w < 9.0:
                    K.plate("bk%d_%d_%d" % (s, sh_, k), [(x, y + 0.6), (x + w, y + 0.6), (x + w, y + 0.6 + h), (x, y + 0.6 + h)], z0, z1, c, part="books", bevel_w=0.0)
                x += w + rr.uniform(0.05, 0.3)
                k += 1
        for sx_, x in ((-1, -10.6), (1, 11.6)):                                                  # two fluted columns flanking the arch
            C.loft("col%d_%d" % (s, sx_), [K.bl.ring(y, K.bl.circle(r, 10), ox=x, oz=(ZF + 0.6) * s) for y, r in ((3.0, 1.9), (4.0, 1.4), (24.0, 1.4), (25.0, 1.9), (26.4, 2.0))], P["stone"], "columns")
        # a lamp glowing above the arch
        K.gem_setting("lamp%d" % s, (0.5, 33.2, (ZF + 0.6) * s), 2.2, P["lit"], P["gold"], normal=(0, 0, s), claws=6, sides=8, part="lantern", rng_=rr, claw_r=0.5)
        for k, (dx, dy) in enumerate(((-6.0, 27.5), (6.0, 27.5))):
            K.gem_setting("sconce%d_%d" % (s, k), (dx + 0.5, dy, (ZF + 0.5) * s), 1.2, P["violet"], P["gold"], normal=(0, 0, s), claws=4, sides=6, part="lantern", rng_=rr, claw_r=0.5)
    # ---- the spine side: the spines of a hundred books stacked in rows (it faces -x), a gold plaque
    x = -14.6
    for row in range(6):
        y = 1.4 + 5.7 * row
        z = -th + 0.8
        k = 0
        while z < th - 1.4 and k < 8:
            w = rr.uniform(1.4, 2.4)
            h = 4.6 + rr.uniform(-0.5, 0.5)
            if z + w > th - 0.6:
                break
            c = P["bk%d" % rr.randint(0, 6)]
            K.plate("sp%d_%d" % (row, k), [(x - rr.uniform(0.0, 0.5), y), (x + 1.6, y), (x + 1.6, y + h), (x - rr.uniform(0.0, 0.5), y + h)], z, z + w - 0.1, c, part="spines", bevel_w=0.0)
            z += w + 0.1
            k += 1
    # ---- a library ladder leaning on the cover, its rungs and a hook; a hanging lantern chain at the foot
    xa = lambda y: 4.0 - 2.0 * (y - 3.0) / 35.0                                                   # noqa: E731  (the ladder leans: the rails converge toward the top)
    for k, off in enumerate((0.0, 4.2)):
        K.bar("rail%d" % k, V(xa(3.0) + off, 3.0, ZF + 3.2), V(xa(38.0) + off, 38.0, ZF + 3.2), 0.5, 0.5, P["wood"], part="ladder", bevel_w=0.05)
    for k in range(8):
        y = 6.0 + 28.0 * k / 7.0
        K.bar("rung%d" % k, V(xa(y), y, ZF + 3.2), V(xa(y) + 4.2, y, ZF + 3.2), 0.25, 0.25, P["wood"], part="ladder", bevel_w=0.0)
    chain_pts = [V(0.5, 0.0, th * 0.4), V(0.0, -3.0, th * 0.4), V(0.5, -6.0, th * 0.4)]
    K.chain("lchain", chain_pts, 3.0, 2.0, 0.4, P["gold"], max_links=3, part="lantern", up=(1, 0, 0))
    K.gem_setting("lantern", (0.5, -8.4, th * 0.4), 2.4, P["lit"], P["gold"], normal=(0, 0, 1), claws=6, sides=8, part="lantern", rng_=rr, claw_r=0.5)
    # pages standing out of the top, and a pediment of stacked tiny books
    for i, (px, lean, tilt, top) in enumerate(((-6.0, 16.0, 6.0, 44.4), (3.4, -10.0, 10.0, 45.3), (8.0, -22.0, 6.0, 43.0))):
        page("pg%d" % i, px, top, lean, tilt, P, P["violet"], w=8.0, h=11.0, lines=4)
    for k, (x0, w, h) in enumerate(((-12.0, 7.0, 2.2), (-5.4, 6.0, 1.8), (1.0, 8.0, 2.4), (8.0, 5.2, 1.6))):
        K.plate("stack%d" % k, [(x0, 36.0), (x0 + w, 36.0), (x0 + w, 36.0 + h), (x0, 36.0 + h)], -th + 1.0 + 0.6 * (k % 2), th - 1.0 - 0.8 * (k % 3), P["bk%d" % ((k * 2 + 1) % 7)], part="books", bevel_w=0.1)
    return K.quad_estimate()


C.register("pipe", pipe_book, "pipe_book")
C.register("needle", needle_book, "animal_needle_book")
C.register("revised", revised_edition, "revised_edition")
C.register("library", library_tome, "library_tome")

if __name__ == "__main__":
    C.main(sys.argv[1:])
