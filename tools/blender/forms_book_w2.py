"""Style wave 2 of the VASSAL BOOK family: the base book and three forms (docs/heroweapon/style/wave2.md). REPLACEMENT of vassal_b.py's book() and of the v2 builders in
forms_vassal_book.py (pipe_book, revised_edition, library_tome), which stay in the repo untouched; animal_needle_book is not touched. The previous meshes and skins are kept
in weapon_meshes/classic_style/. Every book keeps the base frame (cover xy plane x -13.1..13.7, y 0..36, +z the front, the spine on -x, loose pages up to y 45) so the
hand and the size class stay.

    python3 -B tools/blender/forms_book_w2.py --only base,pipe,revised,library [--trial DIR] [--quick] [--samples N] [--bake PX]

The audit's finding was FIVE BOOKS ON ONE OUTLINE (profile IoU 0.83) with one sun medallion, gold corners and a page fan on all four. Now each has its own outline and ONE hero:
  base      (vassal_book)   the one with a fan of loose pages tied with a cord out of a plain worn cloth book, one strap; NOT a sun medallion and gold corners.
  pipe      (pipe_book)     the one with ONE iron pipe cage, a chain and a big padlock hanging off the lower corner and one steam stack; NOT a brass nut at every crossing.
  revised   (revised_edition) the one with a thick oxblood volume and a wide fan of annotated loose pages leaning one way, three ribbons; NOT a gold seal, gem and corner crowns.
  library   (library_tome)  the one with an arched top (the cover is cut like a window) and shelves of small books inside it, one lamp; NOT columns, ladder, sconces and a pediment.
Palette per book: 3 bases + 1 named accent (lit ink / amber / lamp), no gold. Glow only on the accent.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
import forms_kit as K  # noqa: E402
import forms_vassal_book as FB  # noqa: E402
import forms_vassal_common_b as C  # noqa: E402
import wave2_kit as W  # noqa: E402
import vassal_b as VB  # noqa: E402
from forms_kit import TAU, rgb  # noqa: E402
from forms_vassal_common_b import V  # noqa: E402

X0, X1, Y0, Y1 = FB.X0, FB.X1, FB.Y0, FB.Y1


def dsec(r, w, y, lap=False):
    """The rounded back of the spine at height y (a half ring on the -x side); lap=True also laps onto the covers (bands only: a flat in the cover plane would flicker)."""
    pts = [V(-12.3 + r * math.cos(math.radians(a)), y, w * math.sin(math.radians(a))) for a in range(90, 271, 20)]
    return pts + ([V(-10.1, y, -w), V(-10.1, y, w)] if lap else [])


def block(P, th, outline, spine_bands, cover="cover", pages="paper", spine="spine", band="band", page_y=(0.7, 35.3)):
    """The book's block with its own cover outline: two stepped cover boards, a bulging page loft, a rounded spine and unequal raised spine bands [(y, half-height, extra radius)]."""
    for nm, sg in (("cover_front", 1), ("cover_back", -1)):
        K.stepped_plate(nm, outline, (th - 0.9) * sg, P[cover], steps=2, step_h=0.45, inset=0.5, part="cover", up=sg, bevel_w=0.14)
    y0, y1 = page_y
    hs = ((y0, 0.86), (y0 + 1.7, 0.96), (y0 + 8.3, 1.0), (y1 - 8.3, 1.0), (y1 - 1.7, 0.96), (y1, 0.86))
    C.loft("pages", [C.ring_y(y, 12.4, (th - 0.8) * k, 1.3, cx=0.5) for y, k in hs], P[pages], "pages")
    C.loft("spine", [dsec(2.6, th, 0.0), dsec(2.6, th, 36.0)], P[spine], "spine")
    for i, (y, hh, er) in enumerate(spine_bands):
        C.loft("band%d" % i, [dsec(2.85 + er, th + 0.9, y - hh, True), dsec(2.85 + er, th + 0.9, y + hh, True)], P[band], "spine_bands")


def cut_outline(cut_tr=2.0, cut_tl=2.0, cut_br=2.0, cut_bl=2.0, arch=None):
    """A cover outline with a different chamfer at each corner; arch=(height, steps) replaces the top edge by a round arch rising `height` above Y1."""
    pts = [(X0 + cut_bl, Y0), (X1 - cut_br, Y0), (X1, Y0 + cut_br), (X1, Y1 - cut_tr)]
    if arch:
        h, n = arch
        cx, hw = 0.3 * (X0 + X1) + 0.2, 0.5 * (X1 - X0)
        top = [(cx + hw * math.cos(math.pi * k / n), Y1 - cut_tr + h * math.sin(math.pi * k / n)) for k in range(n + 1)]
        pts += [(X1, Y1 - cut_tr)] + top[1:-1] + [(X0, Y1 - cut_tl)]
    else:
        pts += [(X1 - cut_tr, Y1), (X0 + cut_tl, Y1), (X0, Y1 - cut_tl)]
    pts += [(X0, Y0 + cut_bl)]
    out = []
    for p in pts:
        if not out or abs(p[0] - out[-1][0]) + abs(p[1] - out[-1][1]) > 1e-6:
            out.append(p)
    return out


def strap(name, y, width, mat, iron, th, x_out=16.7, buckle=True):
    path = bl.smooth_path([(10.6, y, th + 0.12), (14.2, y, th + 0.7), (x_out - 0.3, y, th * 0.7), (x_out, y, 0.0), (x_out - 0.3, y, -th * 0.7), (14.2, y, -th - 0.7), (10.6, y, -th - 0.12)], 3)
    bl.sweep(name, path, [(-0.28, -width), (0.28, -width), (0.28, width), (-0.28, width)], mat, up=(0, 1, 0), part="strap")
    if buckle:
        K.plate(name + "_buckle", [(11.0, y - width * 0.8), (13.6, y - width * 0.8), (13.6, y + width * 0.8), (11.0, y + width * 0.8)], th + 0.7, th + 1.2, iron, part="strap", bevel_w=0.12)


def mats_cloth(prefix, col, accent_col, accent_glow, seed, spine_col=(70, 46, 32)):
    P = W.Palette("ink", ["cover", "spine", "iron"], ["paper", "cord"])
    P["cover"] = W.hide(prefix + "_cover", rgb(*col), seed=seed, bump_strength=0.45)
    P["spine"] = W.hide(prefix + "_spine", rgb(*spine_col), seed=seed + 1)
    P["iron"] = W.iron(prefix + "_iron", rgb(64, 66, 78), seed=seed + 2, mottle_color=rgb(88, 64, 50))
    P["paper"] = W.paper(prefix + "_paper", rgb(232, 222, 192), seed=seed + 3)
    P["cord"] = W.cord(prefix + "_cord", rgb(120, 96, 64), seed=seed + 4)
    P["lit"] = W.accent_glow(prefix + "_lit", rgb(*accent_glow), 0.9, seed=seed + 5)
    P["band"] = P["spine"]
    return P


# ================================================================ base book (vassal_book): a worn cloth book, a fan of loose pages, one strap
def base_book(out):
    FAST = VB.FAST
    K.begin(8 if FAST else 24)
    P = mats_cloth("bk", (34, 66, 88), None, (110, 190, 255), 301)
    P["ribbon"] = W.cord("bk_ribbon", rgb(150, 34, 40), seed=306, bump_strength=0.4)
    P["lit"] = W.accent_glow("bk_lit", rgb(100, 180, 255), 0.9, seed=305)
    th = 4.2
    block(P, th, cut_outline(2.0, 5.5, 2.0, 2.0), [(8.0, 1.5, 0.35), (13.2, 0.7, 0.2), (28.6, 1.2, 0.3)])                         # one bigger cut corner: the dog-eared top fore corner... on the spine side
    strap("strap", 11.0, 2.3, P["cord"], P["iron"], th)
    # a worn iron title plate (the only metal on the front) with scored runes, lower left; a scuffed spine label
    K.plate("title_plate", [(-9.4, 4.6), (2.8, 4.6), (2.8, 9.6), (-9.4, 9.6)], th - 0.1, th + 0.35, P["iron"], part="title_plate", bevel_w=0.1)
    K.runes("title_runes", K.RUNES[:5], 2.1, W.scratch("bk_scratch"), (-3.3, 7.1, th + 0.38), extrude=0.05, spacing=1.25, detail=True, font=K.FONT_RUNIC, part="title_plate")
    # the hero: a fan of loose pages (graded 12.6 .. 8.6) tied with a cord, leaning to the right, lit script on them
    for i, (px, lean, tilt, top, w, h) in enumerate(((-6.1, 24.0, 8.0, 43.6, 8.2, 11.6), (-1.6, 6.0, 12.0, 45.272, 9.0, 13.0), (3.6, -12.0, 7.0, 44.2, 8.6, 12.4), (8.4, -30.0, 9.0, 40.6, 7.6, 9.8))):
        FB.page("pg%d" % i, px, top, lean, tilt, P, P["lit"], w=w, h=h, lines=5 - (i % 2))
    K.tube("page_cord", [(-9.0, 33.8, 1.3), (-2.0, 34.4, 2.4), (6.0, 34.2, 2.2), (10.0, 33.6, 1.2)], 0.28, P["cord"], sides=4, sub=3, part="strap")
    # the ribbon hangs to the box floor (y -8.9)
    K.ribbon("ribbon", [V(-4.4, 0.9, 0.0), V(-3.7, -3.0, 0.5), V(-3.2, -6.2, 0.1), V(-3.6, -8.02, 0.4)], 2.0, P["ribbon"], facing=(0, 0, 1), thick=0.08, part="ribbon", fray=0.3, rng_=K.rng(311), edge_wave=0.05, notch=1.0)
    return VB.write("vassal_book", out)


# ================================================================ pipe book
def pipe_book(opts):
    C.begin(opts)
    C.weights(cover=1.1, cage=1.2, chains=1.0, lock=1.3, pages=0.9, spine=0.9, stack=0.9)
    P = mats_cloth("pb", (52, 58, 74), None, (255, 170, 70), 321)
    P["cover"] = W.iron("pb_cover", rgb(52, 58, 74), seed=321, bump="hammer", bump_strength=0.2)
    P["spine"] = W.iron("pb_spine", rgb(84, 92, 108), seed=322)
    P["pipe"] = W.steel("pb_pipe", rgb(150, 158, 172), seed=323, bevel=0.12)
    P["lit"] = W.accent_glow("pb_lit", rgb(255, 170, 70), 0.9, seed=325)
    rr = K.rng(7201)
    th = 4.2
    block(P, th, cut_outline(2.0, 2.0, 2.0, 5.0), [(6.0, 1.2, 0.3), (18.0, 0.8, 0.2), (30.0, 1.8, 0.35)], cover="cover", spine="spine", band="spine")
    ZF = th + 1.4
    # the cage: a pipe frame, TWO upright pipes and TWO cross pipes (not a grid), one hex nut at the one crossing that holds the lock
    frame = [V(X0 + 1.4, Y0 + 1.4, ZF), V(X1 - 1.0, Y0 + 1.4, ZF), V(X1 - 1.0, Y1 - 1.4, ZF), V(X0 + 1.4, Y1 - 1.4, ZF), V(X0 + 1.4, Y0 + 1.4, ZF)]
    for i in range(4):
        K.tube("frame%d" % i, [frame[i], (frame[i] + frame[i + 1]) * 0.5, frame[i + 1]], 1.3, P["pipe"], sides=8, sub=1, part="cage")
        K.bl.lathe("elbow%d" % i, [(0.01, -1.6), (1.7, -1.2), (1.7, 1.2), (0.01, 1.6)], P["pipe"], sides=8, origin=tuple(frame[i + 1]), axis="z", part="cage")
    for x in (-5.0, 8.4):
        K.tube("up%d" % int(x * 10), [V(x, Y0 + 2.0, ZF), V(x + rr.uniform(-0.3, 0.3), 18.0, ZF), V(x, Y1 - 2.0, ZF)], 1.0, P["pipe"], sides=8, sub=2, part="cage")
    for y in (10.5, 25.5):
        K.tube("cross%d" % int(y), [V(X0 + 2.0, y, ZF + 1.4), V(0.5, y + rr.uniform(-0.3, 0.3), ZF + 1.4), V(X1 - 1.5, y, ZF + 1.4)], 1.0, P["pipe"], sides=8, sub=2, part="cage")
    K.rivets("nut", [(V(-5.0, 10.5, ZF + 2.5), (0, 0, 1))], P["spine"], r=1.7, sides=6, rng_=rr, height=0.7, detail=False)
    # ONE chain from the cage to a big padlock hanging off the lower fore corner (the hero, off the centre line), one steam stack on the head
    K.chain("chain", [V(X1 - 1.0, Y0 + 4.0, ZF + 1.6), V(X1 + 0.4, Y0 - 1.5, 2.0), V(X1 - 1.0, Y0 - 5.0, 0.0)], 5.2, 3.4, 0.75, P["pipe"], max_links=5, part="chains", up=(0, 0, 1))
    c = V(X1 - 2.0, Y0 - 6.2, 0.0)
    C.loft("lock_box", [C.ring_z(z, 4.4, 3.7, c.x, c.y - 1.0, 0.9) for z in (c.z - 2.4, c.z + 2.4)], P["spine"], "lock")
    K.tube("shackle", [c + V(-2.8, 2.4, 0), c + V(-2.8, 6.6, 0), c + V(0.0, 8.4, 0), c + V(2.8, 6.6, 0), c + V(2.8, 2.4, 0)], 1.0, P["pipe"], sides=8, sub=3, part="lock")
    K.plate("keyhole", [(-0.8, -0.7), (0.8, -0.7), (1.1, 0.9), (-1.1, 0.9)], c.z + 2.35, c.z + 2.8, P["lit"], part="lock")
    K.bl.lathe("keyhole_o", [(0.01, 0.0), (1.2, 0.0), (1.2, 0.5), (0.01, 0.5)], P["lit"], sides=8, origin=(c.x, c.y + 0.8, c.z + 2.35), axis="z", part="lock")
    K.bl.lathe("stack", [(0.01, 0.0), (2.4, 0.0), (2.2, 2.9), (2.9, 3.4), (2.9, 4.6), (0.01, 5.0)], P["pipe"], sides=8, origin=(-8.5, 36.0, -1.2), axis="y", part="stack", caps=(False, True))
    for i, (px, lean, tilt, top) in enumerate(((-4.0, 12.0, 8.0, 44.4), (4.8, -14.0, 6.0, 42.6))):
        FB.page("pg%d" % i, px, top, lean, tilt, P, P["lit"])
    return K.quad_estimate()


# ================================================================ revised edition: thick oxblood volume, a wide lean fan of annotated pages, three ribbons
def revised_edition(opts):
    C.begin(opts)
    C.weights(cover=1.1, pages=1.3, ribbons=0.9, spine=0.9, label=0.8)
    P = mats_cloth("rv", (96, 26, 36), None, (255, 200, 110), 331, spine_col=(58, 34, 28))
    P["lit"] = W.accent_glow("rv_lit", rgb(255, 196, 100), 0.9, seed=335)
    rr = K.rng(7203)
    th = 5.4
    block(P, th, cut_outline(2.6, 2.6, 4.2, 2.6), [(5.5, 1.0, 0.3), (9.6, 0.6, 0.2), (30.5, 1.6, 0.3)])
    # a plain label plate (iron) with scored lines, a thin corner guard on the lower fore corner only
    for s in (1, -1):
        K.plate("label%d" % s, [(-8.0, 25.6), (8.4, 25.6), (8.4, 31.4), (-8.0, 31.4)], *sorted(((th - 0.1) * s, (th + 0.3) * s)), P["iron"], part="label", bevel_w=0.1)
        K.engrave_lines("label_l%d_" % s, [[(-6.6, 28.5), (6.8, 28.5)], [(-6.6, 27.0), (3.0, 27.0)]], W.scratch("rv_scr"), side=s, z=th + 0.32, w=0.12, part="label", detail=True)
    K.plate("guard", [(X1, Y0 + 4.2), (X1 - 6.0, Y0), (X1, Y0)], (th - 0.1), th + 0.4, P["iron"], part="label", bevel_w=0.1)
    # the hero: seven loose annotated pages in a wide lean fan to the left, graded heights
    spec = ((-10.8, 52.0, 9.0, 41.0, 8.6, 11.0), (-7.4, 38.0, 6.0, 43.0, 9.4, 12.6), (-3.6, 24.0, 10.0, 44.6, 9.8, 13.4), (0.6, 10.0, 7.0, 45.0, 9.6, 13.2), (4.6, -4.0, 9.0, 44.0, 9.0, 12.4),
            (8.4, -18.0, 6.0, 42.0, 8.4, 11.2), (11.6, -30.0, 8.0, 38.6, 7.2, 9.2))
    for i, (px, lean, tilt, top, w, h) in enumerate(spec):
        FB.page("pg%d" % i, px, top, lean, tilt, P, P["lit"], w=w, h=h, lines=5 if i % 3 else 3)
    # three ribbons of unequal length, width and colour (not six), one notched
    for i, (x, L_, col, wd) in enumerate(((-9.4, 10.5, (150, 34, 40), 2.0), (-3.0, 7.2, (56, 80, 120), 1.5), (5.4, 13.8, (200, 170, 80), 2.4))):
        P["rib%d" % i] = W.cord("rv_rib%d" % i, rgb(*col), seed=340 + i, bump_strength=0.4)
        path = [V(x + 0.15 * k, 1.0 - L_ * k / 6.0, 0.9 * math.sin(k * 0.8 + i)) for k in range(7)]
        K.ribbon("ribbon%d" % i, path, wd, P["rib%d" % i], facing=(0, 0, 1), thick=0.07, part="ribbons", fray=0.5, rng_=rr, edge_wave=0.06, notch=1.4 if i == 1 else 0.0)
    return K.quad_estimate()


# ================================================================ library tome: an arched cover, shelves of small books, one lamp
def library_tome(opts):
    C.begin(opts)
    C.weights(cover=1.0, shelves=1.2, books=1.2, spines=1.0, lamp=1.0, pages=0.8)
    P = mats_cloth("lt", (52, 34, 70), None, (255, 190, 90), 351, spine_col=(66, 44, 34))
    P["wood"] = W.wood("lt_wood", rgb(100, 66, 36), seed=355)
    P["lit"] = W.accent_glow("lt_lit", rgb(255, 190, 90), 1.0, seed=356)
    P["dark"] = bl.mat("lt_dark", rgb(22, 16, 30), rough=0.8, bevel=0.0)
    for i, col in enumerate(((140, 44, 46), (44, 86, 140), (60, 112, 70), (176, 140, 60), (96, 58, 118), (190, 190, 180), (44, 44, 64))):
        P["bk%d" % i] = W.hide("lt_bk%d" % i, rgb(*col), seed=360 + i, bump_strength=0.35, bevel=0.06)
    rr = K.rng(7204)
    th = 7.4
    out = cut_outline(2.0, 2.0, 2.0, 2.0, arch=(7.5, 10))
    block(P, th, out, [(6.4, 1.2, 0.4), (18.0, 0.6, 0.25), (30.0, 1.4, 0.35)], cover="cover")
    ZF = th + 0.1
    for s in (1, -1):
        win = [(-8.4, 4.6), (9.4, 4.6), (9.4, 25.0)] + [(0.5 + 8.9 * math.cos(math.radians(a)), 25.0 + 8.2 * math.sin(math.radians(a))) for a in range(0, 181, 20)][1:-1] + [(-8.4, 25.0)]
        K.plate("window%d" % s, win, *sorted(((ZF + 0.3) * s, (ZF + 1.3) * s)), P["dark"], part="window")
        for sh_, y in enumerate((4.8, 12.6, 20.4)):                                       # three shelves of different spacing, books of different heights, one leaning
            K.plate("shelf%d_%d" % (s, sh_), [(-8.4, y), (9.4, y), (9.4, y + 0.7), (-8.4, y + 0.7)], *sorted(((ZF + 0.8) * s, (ZF + 2.9) * s)), P["wood"], part="shelves")
            x, k = -8.0 + rr.uniform(0, 0.5), 0
            while x < 8.8 and k < 9:
                w = rr.uniform(0.9, 1.7)
                h = rr.uniform(4.2, 6.8) if sh_ < 2 else rr.uniform(3.0, 6.6)
                z0, z1 = sorted(((ZF + 1.0) * s, (ZF + 2.7 - 0.4 * (k % 2)) * s))
                if y + 0.7 + h < 31.5 and x + w < 9.2:
                    K.plate("bk%d_%d_%d" % (s, sh_, k), [(x, y + 0.7), (x + w, y + 0.7), (x + w + (0.9 if (sh_ == 1 and k == 4) else 0.0), y + 0.7 + h), (x + (0.9 if (sh_ == 1 and k == 4) else 0.0), y + 0.7 + h)], z0, z1,
                            P["bk%d" % rr.randint(0, 6)], part="books", bevel_w=0.0)
                x += w + rr.uniform(0.05, 0.3)
                k += 1
        K.bl.lathe("lamp%d" % s, [(0.01, 0.0), (1.6, 0.0), (2.2, 1.4), (1.8, 3.0), (0.01, 3.6)], P["lit"], sides=8, origin=(0.5, 36.6, (ZF + 0.5) * s), axis="z", part="lamp")
    x = -14.6                                                                              # three rows of book spines on the -x side, not six
    for row, y in enumerate((4.2, 13.4, 24.0)):
        z, k = -th + 0.8, 0
        while z < th - 1.4 and k < 8:
            w = rr.uniform(1.4, 2.4)
            h = 6.4 + rr.uniform(-1.0, 1.0)
            if z + w > th - 0.6:
                break
            K.plate("sp%d_%d" % (row, k), [(x - rr.uniform(0.0, 0.5), y), (x + 1.6, y), (x + 1.6, y + h), (x - rr.uniform(0.0, 0.5), y + h)], z, z + w - 0.1, P["bk%d" % rr.randint(0, 6)], part="spines", bevel_w=0.0)
            z += w + 0.1
            k += 1
    for i, (px, lean, tilt, top) in enumerate(((-3.0, 14.0, 6.0, 45.0), (6.0, -16.0, 8.0, 43.0))):
        FB.page("pg%d" % i, px, top, lean, tilt, P, P["lit"], w=8.0, h=11.0, lines=4)
    return K.quad_estimate()


BOOKS = {"pipe": pipe_book, "revised": revised_edition, "library": library_tome}
C.register("pipe", pipe_book, "pipe_book")
C.register("revised", revised_edition, "revised_edition")
C.register("library", library_tome, "library_tome")


def main(argv):
    if "--base" in argv:                                    # the base book: bl_lib.finish path of vassal_b.write
        argv = [a for a in argv if a != "--base"]
        out = {}
        trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
        if "--fast" in argv:
            VB.FAST = True
        if trial:
            os.makedirs(trial, exist_ok=True)
            out = dict(out_mesh=trial, out_skin=trial, preview=os.path.join(trial, "vassal_book_preview.png"))
        base_book(out)
        K.shrink_pngs(out.get("out_skin") or bl.SKIN_DIR, "vassal_book")
        return
    C.main(argv)


if __name__ == "__main__":
    main(sys.argv[1:])
