"""Bertholdt Hoover (Attack on Titan, the Colossal Titan's holder), HD 256x256 (3.38.0, group d of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_d.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/bertholdt_hoover.py): the tallest of the cadets, dark-brown hair that falls over a sad, tired face with green-grey eyes, the brown
Survey Corps jacket over a white cravat, brown leather harness straps and a double belt with silver buckles, white trousers with thigh straps, tan-topped brown boots, and the green Scout cloak on
the shoulders with the two feathered wings (one white, one blue) on the back, an original rendering of the wings motif.  HD adds: a really drawn sad face (heavy lids, eye bags, a parted fringe on the
base head), a jacket with panel seams and stitching, the cravat, the harness with buckles, the hip cases of the manoeuvre gear on both sides, the cloak with folds, a gathered neck cord and a ragged
hem (it continues on the back of the legs), feathered wings with a bevelled outline, leather bracers and gloves, thigh straps, boots with tan cuffs and buckles.
Hair: `anime_bangs_long` (long parted bangs: the forehead and the eyes stay free), dark brown.  Original art from memory of the canon look (rule 10: no wiki could be fetched).  No glow.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402
import _dlib as D  # noqa: E402

SKIN = ("#c99a78", "#e9bf9c", "#f8dcc0")
HAIR = (52, 40, 34)
HAIRC = "#4a382e"
JK = "#7c5336"                       # the brown jacket
JK_D = "#442a18"
LEA = "#43291a"                      # dark leather
LEA_L = "#6a4630"
GRN, GRN_D, GRN_L = "#4c6c44", "#26402a", "#74a068"      # the Scout cloak
WHT, WHT_D = "#efe9d8", "#aaa088"
BOOT, BOOT_T = "#6c4428", "#b08450"
SIL, SIL_D = "#cfd6e0", "#6a7484"
WING_W = ("#a8b4c8", "#e6ecf6", "#ffffff")
WING_B = ("#1e4690", "#3f78d0", "#8cb8f4")
SIDES = D.SIDES


# ================================================================================================ the head
def head(cv):
    D.head_lod(cv, SKIN, HAIRC, sb_x=7.0, sb_y=5.4, fringe=1.5, ear_x=4.5, nape=7.0, back_nape=7.4)
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=None, side=0.08, chin=0.06)
    # a long face: soft cheek hollows, a narrow chin
    for sg in (-1, 1):
        f.fill(f.m_ellipse(4.0 + sg * 3.35, 6.5, 0.85, 1.7, soft=3.0), H.tone(SKIN[0], 0.85), a=0.3)
    # the parted fringe on the base head: two long locks fall from the part at the centre to the temples and the cheeks, the forehead is open between them
    left = [(3.95, -0.2), (-0.2, -0.2), (-0.2, 5.7), (0.55, 6.1), (0.95, 4.5), (1.7, 3.1), (2.8, 2.2), (3.55, 1.6), (3.95, 0.9)]
    for sg, pts in ((-1, left), (1, D.mirror(left, 8.0))):
        hm = f.m_poly(pts)
        D.paint_hair(f, hm, HAIRC, strands=3.4, seed=2 + sg, top=1.04, bottom=0.82, flow=0.5)
        f.fill(f.m_polyline(pts[3:9], 0.1), H.tone(HAIRC, 0.5), 0.5)
        f.shade(f.m_poly([(x + sg * 0.0, y + 0.5) for x, y in pts[3:9]] + [(4.0, 1.5)]) * (1.0 - hm), 0.9, a=0.5)
    # sad brows: thick, the inner end raised
    for sg in (-1, 1):
        H.brow(f, (4.0 + sg * 0.9, 3.0), (4.0 + sg * 3.1, 3.6), "#2e1e14", 0.4, 0.22, arch=0.12)
    iris = ("#16302a", "#4c8068", "#a4cdb4")
    for cx in (2.35, 5.65):
        H.eye(f, cx, 4.95, 2.5, 1.75, iris, lash="#1a100c", sclera="#f2f0ea", tilt=-0.12, pupil="#08120e", hi=0.9, lid_shadow=0.48, lash_w=0.4, flick=0.14, outer=(-1 if cx < 4 else 1))
        f.fill(f.m_ellipse(cx, 6.1, 1.3, 0.34, soft=3.0), "#6a5a7a", a=0.3)                      # the tired bags under the eyes
    H.nose(f, 4.0, 6.15, "#c8887a", 0.28)
    f.line((3.7, 4.8), (3.65, 5.9), H.tone(SKIN[0], 0.8), 0.1, 0.16, a=0.4)
    f.line((4.3, 4.8), (4.35, 5.9), H.tone(SKIN[0], 0.8), 0.1, 0.16, a=0.4)
    H.mouth_smile(f, 4.0, 7.1, 1.7, color="#8e4e48", lip="#c4806e", depth=-0.1, thick=0.15)
    f.fill(f.m_ellipse(4.0, 7.55, 0.7, 0.2, soft=3.0), H.tone(SKIN[0], 0.72), a=0.28)
    # the hat layer: nothing (the 3D hair is the second layer); the neck gets a shadow under the chin from the cravat


# ================================================================================================ pieces
def jacket(f, mask, folds=(), y0=0.0, y1=12.0):
    H.cloth(f, mask, JK, top=1.14, bottom=0.74, round_=0.12, fold_xs=list(folds), fold_depth=0.14, fold_y0=y0, fold_y1=y1, weave_k=0.02)
    H.weave(f, mask, strength=0.03, period=0.4, diagonal=False)            # a faint leather grain


def cloak(f, mask, folds=(), y0=0.0, y1=12.0, k=1.0):
    H.cloth(f, mask, H.tone(GRN, k), top=1.16, bottom=0.72, round_=0.12, fold_xs=None, weave_k=0.04)
    H.folds(f, mask, list(folds), depth=0.2, width=0.3, sway=0.18, y0=y0, y1=y1)


def strap(f, pts, w=0.62, color=LEA):
    """A leather strap along a path: dark outline, gradient, stitching along both edges."""
    d, _ = f.polyline_sd(pts)
    f.fill(f.m_polyline(pts, w + 0.2), "#1a0e08", 0.7)
    m = f.m_polyline(pts, w)
    ys = [p[1] for p in pts]
    f.fill(m, H.lin((0, min(ys)), (0, max(ys) + 0.1), [(0, H.tone(color, 1.3)), (0.5, color), (1, H.tone(color, 0.7))]))
    H.bevel(f, m, depth=w * 0.5, gain=0.7, spec=0.25, spec_pow=12)
    for sg in (-1, 1):
        off = [(x + sg * w * 0.32, y) for x, y in pts]
        H.stitch(f, off, "#c8a878", width=0.04, dash=(0.2, 0.15), shadow=0.0)
    return m


def buckle(f, x, y, w=0.9, h=0.8):
    D.buckle(f, x, y, w, h, color=SIL, hole="#26160e")


def feather(f, root, ang, length, width, body, outline="#0e1a3a", tip_shade=None):
    """One feather from `root` pointing at `ang` degrees (0 = right, 90 = up): an elongated leaf with a bevelled outline and a centre line."""
    a = math.radians(ang)
    ux, uy = math.cos(a), -math.sin(a)
    px, py = -uy, ux
    pts_l, pts_r = [], []
    for t, wd in ((0.0, 0.25), (0.25, 0.8), (0.55, 1.0), (0.82, 0.7), (1.0, 0.0)):
        cx, cy = root[0] + ux * length * t, root[1] + uy * length * t
        pts_l.append((cx + px * width * wd / 2.0, cy + py * width * wd / 2.0))
        pts_r.append((cx - px * width * wd / 2.0, cy - py * width * wd / 2.0))
    poly = pts_l + pts_r[::-1][1:]
    f.fill(f.m_poly([(x + 0.05, y + 0.06) for x, y in poly]), "#000000", 0.35)
    f.fill(f.m_poly(poly, soft=1.0), outline)
    shrink = []
    ccx, ccy = root[0] + ux * length * 0.5, root[1] + uy * length * 0.5
    for x, y in poly:
        shrink.append((ccx + (x - ccx) * 0.78, ccy + (y - ccy) * 0.86))
    m2 = f.m_poly(shrink)
    f.fill(m2, H.lin((root[0], root[1]), (root[0] + ux * length, root[1] + uy * length), [(0, body[0]), (0.6, body[1]), (1, body[2])]))
    f.line(root, (root[0] + ux * length * 0.95, root[1] + uy * length * 0.95), H.tone(body[0], 0.7), 0.06, 0.03, a=0.7)
    H.bevel(f, m2, depth=0.18, gain=0.5)


def wings(f, cx=4.0, cy=9.0):
    """The two wings, crossed at the bottom: the blue one sweeps up and to the viewer's left, the white one up and to the right, six feathers each, the white one on top."""
    for ang, ln in ((160, 3.5), (148, 4.3), (136, 5.0), (124, 5.6), (112, 5.9), (100, 5.8)):
        feather(f, (cx - 0.35, cy), ang, ln, 1.3, WING_B, outline="#0c1a3e")
    for ang, ln in ((20, 3.5), (32, 4.3), (44, 5.0), (56, 5.6), (68, 5.9), (80, 5.8)):
        feather(f, (cx + 0.35, cy), ang, ln, 1.3, WING_W, outline="#4a566c")
    f.ellipse(cx, cy + 0.1, 0.55, 0.5, H.rad((cx - 0.1, cy), 0.8, [(0, "#e8d8a8"), (1, "#8a7440")]))            # the knot where the wings cross


# ================================================================================================ the body
def body_front(f):
    jacket(f, f.m_all(), folds=[1.2, 6.8])
    # the green cloak falls over both shoulders: a wedge at the outer top corners
    for sg in (0, 1):
        pts = [(0.0, -0.1), (1.7, -0.1), (1.3, 1.6), (0.9, 3.2), (0.0, 3.9)]
        if sg:
            pts = D.mirror(pts, 8.0)
        m = f.m_poly(pts)
        cloak(f, m, folds=[0.6 if not sg else 7.4], y0=0.0, y1=4.0)
        f.fill(f.m_polyline(pts[1:5], 0.12), "#142418", 0.6)
    # the high collar and the white cravat
    H.cloth(f, f.m_band(0.0, 1.0), H.tone(JK, 1.05), top=1.1, bottom=0.85, round_=0.0, fold_xs=[], weave_k=0.0)
    H.trim_line(f, [(1.7, 1.05), (6.3, 1.05)], JK_D, width=0.1, outline=0.0, spec=0.0)
    cr = f.m_poly([(3.0, 0.9), (5.0, 0.9), (4.7, 1.8), (4.0, 2.6), (3.3, 1.8)])
    f.fill(f.m_poly([(3.1, 1.0), (5.1, 1.0), (4.8, 1.95), (4.0, 2.75), (3.2, 1.95)]), "#12080a", 0.5)
    H.cloth(f, cr, "#fbf8ee", top=1.03, bottom=0.8, round_=0.05, fold_xs=[3.6, 4.0, 4.4], fold_depth=0.2, weave_k=0.0)
    f.polyline([(3.2, 1.1), (3.6, 1.9), (4.0, 2.5)], H.tone(WHT, 0.78), 0.06, a=0.7)
    f.polyline([(4.8, 1.1), (4.4, 1.9), (4.0, 2.5)], H.tone(WHT, 0.78), 0.06, a=0.7)
    # the harness: two straps over the chest running down to the belt, joined by a cross strap with buckles
    strap(f, [(1.7, 1.2), (2.35, 4.5), (2.9, 8.0)], 0.62)
    strap(f, [(6.3, 1.2), (5.65, 4.5), (5.1, 8.0)], 0.62)
    strap(f, [(2.15, 4.6), (5.85, 4.6)], 0.5, LEA_L)
    buckle(f, 4.0, 4.6, 0.95, 0.75)
    for x in (2.15, 5.85):
        H.stud(f, x, 4.6, 0.13, SIL)
    # a small chest pocket with a flap on the viewer's right and a stitched jacket seam down the centre
    D.stitch_seam(f, [(4.0, 3.8), (4.0, 7.9)], "#c8a878", 0.04)
    pk = f.m_rect(5.35, 5.2, 7.25, 6.6, 0.1)
    f.fill(f.m_rect(5.4, 5.28, 7.32, 6.7, 0.1), "#1a0e08", 0.45)
    H.cloth(f, pk, H.tone(JK, 1.1), top=1.1, bottom=0.88, round_=0.0, fold_xs=[], weave_k=0.0)
    D.stitch_seam(f, [(5.5, 5.42), (7.1, 5.42)], "#c8a878", 0.04)
    H.stud(f, 6.3, 5.95, 0.15, SIL)
    # the double belt with the silver buckles, the hip hooks
    for (y0, y1, c) in ((7.95, 8.95, LEA), (8.95, 9.95, "#52331f")):
        m = f.m_band(y0, y1)
        D.leather(f, m, c, y0, y1, sheen_x=[1.3, 6.6], sheen_strength=0.2)
        D.stitch_seam(f, [(0.0, y0 + 0.2), (8.0, y0 + 0.2)], "#c8a878", 0.04)
        D.stitch_seam(f, [(0.0, y1 - 0.2), (8.0, y1 - 0.2)], "#c8a878", 0.04)
    H.shadow_under(f, 9.95, 0.5, 0.36)
    H.shadow_under(f, 8.95, 0.16, 0.2)
    buckle(f, 4.0, 8.95, 1.7, 1.3)
    for x in (1.0, 7.0):
        buckle(f, x, 8.4, 0.7, 0.6)
        H.stud(f, x + (0.6 if x < 4 else -0.6), 9.4, 0.12, SIL)
    # the white trousers start under the belt
    H.cloth(f, f.m_band(10.0, 12.0), WHT, top=1.04, bottom=0.8, round_=0.1, fold_xs=[1.6, 4.0, 6.4], fold_depth=0.12, fold_y0=10.0, fold_y1=12.0, weave_k=0.02)
    H.shadow_under(f, 10.0, 0.6, 0.3)


def body_back(f):
    cloak(f, f.m_all(), folds=[1.1, 2.5, 4.0, 5.5, 6.9], y0=0.0, y1=12.0)
    # the neck: a gathered cord and the rolled collar of the hood
    roll = f.m_band(0.0, 1.5)
    f.fill(roll, H.lin((0, 0), (0, 1.5), [(0, GRN_L), (0.55, GRN), (1, GRN_D)]))
    H.folds(f, roll, [0.8, 2.0, 3.1, 4.9, 6.0, 7.2], depth=0.2, width=0.2, sway=0.1, y0=0.0, y1=1.5)
    H.trim_line(f, [(0.5, 1.55), (3.0, 1.9), (5.0, 1.9), (7.5, 1.55)], "#d8c690", width=0.14, outline=0.4, spec=0.2)
    for sg in (-1, 1):
        f.polyline([(4.0 + sg * 0.1, 1.9), (4.0 + sg * 0.5, 2.7), (4.0 + sg * 0.3, 3.3)], "#d8c690", 0.1, a=0.9)
    H.shadow_under(f, 1.9, 0.5, 0.3)
    wings(f, 4.0, 9.4)
    # the ragged hem of the cloak at the bottom: a darker band with torn tabs
    hem = f.m_band(10.6, 12.0)
    H.scallops(f, f.m_band(10.8, 12.2), 10.8, rows=1, cell=0.9, depth=1.2, base=H.tone(GRN, 0.8), tip=GRN_D, edge=GRN_D, edge_a=0.5, shadow=0.0)
    H.shadow_under(f, 10.6, 0.6, 0.18)


def body_side(f, nm):
    jacket(f, f.m_all(), folds=[1.4, 2.8])
    # the cloak over the shoulder on the side
    pts = [(-0.1, -0.1), (4.1, -0.1), (4.1, 2.3), (2.6, 3.0), (1.2, 2.4), (-0.1, 3.1)]
    cloak(f, f.m_poly(pts), folds=[1.0, 2.6], y0=0.0, y1=3.2)
    f.fill(f.m_polyline(pts[2:6], 0.1), "#142418", 0.6)
    # the hip case of the manoeuvre gear: a steel box with rivets and a handle grip
    case = f.m_rect(0.5, 6.7, 3.5, 11.1, 0.25)
    f.fill(f.m_rect(0.58, 6.8, 3.6, 11.2, 0.25), "#0c0806", 0.5)
    f.fill(case, H.lin((0, 6.7), (0, 11.1), [(0, "#a4aebc"), (0.45, "#6e7a8c"), (1, "#3c4658")]))
    H.bevel(f, case, depth=0.3, gain=0.8, spec=0.4, spec_pow=14)
    f.fill(f.m_rect(0.95, 7.2, 3.05, 8.4, 0.12), H.tone(SIL_D, 0.75), 0.6)
    for (x, y) in ((0.9, 7.1), (3.1, 7.1), (0.9, 10.6), (3.1, 10.6)):
        H.stud(f, x, y, 0.13, "#dfe6f0")
    f.line((1.0, 9.2), (3.0, 9.2), H.tone(SIL_D, 0.6), 0.08, 0.08, a=0.6)
    H.trim_line(f, [(0.5, 8.9), (3.5, 8.9)], "#52331f", width=0.35, outline=0.5, spec=0.1)
    # belts and trousers
    for (y0, y1, c) in ((7.95, 8.95, LEA), (8.95, 9.95, "#52331f")):
        D.leather(f, f.m_band(y0, y1), c, y0, y1, sheen_x=[1.0, 3.0], sheen_strength=0.2)
    H.cloth(f, f.m_band(10.0, 12.0), WHT, top=1.04, bottom=0.8, round_=0.1, fold_xs=[1.4, 2.8], fold_depth=0.12, weave_k=0.02)


def body(cv):
    for f in D.lay(cv, "body", "front"):
        body_front(f)
    for f in D.lay(cv, "body", "back"):
        body_back(f)
    for nm in ("west", "east"):
        for f in D.lay(cv, "body", nm):
            body_side(f, nm)
    t = cv.face("body", "top")
    t.fill(t.m_all(), GRN)
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), WHT_D)
    cv.face("body", "top", True).fill(cv.face("body", "top", True).m_all(), GRN)


# ================================================================================================ the arms
def arm(cv, part):
    right = part == "arm_r"
    for nm in SIDES:
        for f in D.lay(cv, part, nm):
            jacket(f, f.m_all(), folds=[1.1, 2.9], y0=2.0, y1=11.0)
            # the Scout cloak over the shoulder: a green drape with a ragged lower edge
            cl = f.m_band(0.0, 5.0)
            cloak(f, cl, folds=[1.0, 2.2, 3.1], y0=0.0, y1=5.0)
            H.scallops(f, f.m_band(4.6, 5.9), 4.55, rows=1, cell=0.95, depth=1.1, base=H.tone(GRN, 0.85), tip=GRN_D, edge=GRN_D, edge_a=0.6, shadow=0.34, offset=0.2 if right else 0.0)
            H.shadow_under(f, 5.9, 0.0001, 0.0)
            # the elbow crease and the sleeve seam
            f.polyline([(0.2, 7.0), (1.5, 6.7), (2.6, 7.0), (3.8, 6.8)], H.tone(JK, 0.6), 0.09, a=0.5)
            # leather bracer with two straps and buckles
            br = f.m_band(7.4, 10.3)
            D.leather(f, br, LEA, 7.4, 10.3, sheen_x=[0.9, 2.9], sheen_strength=0.2)
            H.shadow_under(f, 10.3, 0.5, 0.35)
            for y in (8.15, 9.35):
                D.leather(f, f.m_band(y - 0.22, y + 0.22), LEA_L, y - 0.22, y + 0.22)
            D.stitch_seam(f, [(0.0, 7.6), (f.w, 7.6)], "#c8a878", 0.04)
            D.stitch_seam(f, [(0.0, 10.1), (f.w, 10.1)], "#c8a878", 0.04)
            # a dark leather glove
            gl = f.m_band(10.3, 12.0)
            D.leather(f, gl, "#2e1c12", 10.3, 12.0, sheen_x=[1.0, 3.0], sheen_strength=0.18, grain=0.04)
            if nm == "front":
                for k in range(3):
                    f.line((1.0 + k, 11.0), (1.0 + k, 11.95), "#150c08", 0.05, 0.04, a=0.7)
                buckle(f, 2.0, 8.75, 0.8, 0.55)
    for ov in (False, True):
        t = cv.face(part, "top", ov)
        t.fill(t.m_all(), H.rad((1.6, 1.6), 3.0, [(0, GRN_L), (0.6, GRN), (1, GRN_D)]))
        H.folds(t, t.m_all(), [1.0, 2.0, 3.0], depth=0.16, width=0.2, sway=0.1)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), "#2e1c12")


# ================================================================================================ the legs
def leg(cv, part):
    right = part == "leg_r"
    outer = "west" if right else "east"
    for nm in SIDES:
        for f in D.lay(cv, part, nm):
            H.cloth(f, f.m_band(0.0, 8.0), WHT, top=1.08, bottom=0.78, round_=0.14, fold_xs=None, weave_k=0.02)
            H.folds(f, f.m_band(0.0, 8.0), [1.0, 2.9], depth=0.13, width=0.22, sway=0.08, y0=0.3, y1=8.0)
            # the thigh strap with a buckle, the knee crease
            D.leather(f, f.m_band(2.3, 3.3), LEA, 2.3, 3.3, sheen_x=[1.0], sheen_strength=0.2)
            D.stitch_seam(f, [(0.0, 2.5), (f.w, 2.5)], "#c8a878", 0.04)
            D.stitch_seam(f, [(0.0, 3.1), (f.w, 3.1)], "#c8a878", 0.04)
            H.shadow_under(f, 3.3, 0.5, 0.25)
            if nm in ("front", outer):
                buckle(f, 1.0 if nm == "front" else 2.0, 2.8, 0.7, 0.75)
            f.polyline([(0.2, 5.2), (1.4, 4.95), (2.4, 5.25), (3.8, 5.0)], H.tone(WHT, 0.7), 0.08, a=0.4)
            # the boot: a folded tan cuff, the brown boot, a strap with a buckle, a toe cap
            H.shadow_under(f, 7.2, 0.4, 0.26)
            cuff = f.m_band(7.3, 8.8)
            D.leather(f, cuff, BOOT_T, 7.3, 8.8, sheen_x=[1.0, 2.9], sheen_strength=0.2)
            D.stitch_seam(f, [(0.0, 8.5), (f.w, 8.5)], "#e0c898", 0.04)
            boot = f.m_band(8.8, 12.0)
            D.leather(f, boot, BOOT, 8.8, 12.0, sheen_x=[0.9, 2.9], sheen_strength=0.3)
            H.shadow_under(f, 8.8, 0.5, 0.38)
            D.leather(f, f.m_band(9.8, 10.35), LEA_L, 9.8, 10.35)
            if nm == "front":
                buckle(f, 2.0, 10.07, 0.75, 0.7)
                f.polyline([(0.3, 11.1), (1.3, 10.75), (2.7, 10.75), (3.7, 11.1)], H.tone(BOOT, 0.55), 0.1, a=0.7)
            f.fill(f.m_band(11.65, 12.0), "#000000", a=0.55)
    # the cloak hangs over the back of both legs: a green tail with a ragged hem on the second layer
    f = cv.face(part, "back", True)
    tail = f.m_band(0.0, 3.6)
    cloak(f, tail, folds=[1.0, 2.2, 3.1], y0=0.0, y1=3.6)
    H.scallops(f, f.m_band(3.0, 4.6), 2.95, rows=1, cell=0.95, depth=1.2, base=H.tone(GRN, 0.8), tip=GRN_D, edge=GRN_D, edge_a=0.6, shadow=0.3, offset=0.3 if right else 0.0)
    f.fill(f.m_band(3.6, 12.0), "#000000", mode="erase")
    for nm in ("front", "west", "east"):
        f = cv.face(part, nm, True)
        f.fill(f.m_band(0.0, 12.0), "#000000", mode="erase")                                  # the second layer of the trousers is only the cloak tail and the straps below
    for nm in ("front", "west", "east"):
        f = cv.face(part, nm, True)
        D.leather(f, f.m_band(2.3, 3.3), LEA, 2.3, 3.3, sheen_x=[1.0], sheen_strength=0.2)
        if nm in ("front", outer):
            buckle(f, 1.0 if nm == "front" else 2.0, 2.8, 0.7, 0.75)
        cuff = f.m_band(7.3, 8.8)
        D.leather(f, cuff, BOOT_T, 7.3, 8.8, sheen_x=[1.0, 2.9], sheen_strength=0.2)
        boot = f.m_band(8.8, 12.0)
        D.leather(f, boot, BOOT, 8.8, 12.0, sheen_x=[0.9, 2.9], sheen_strength=0.3)
        H.shadow_under(f, 8.8, 0.5, 0.38)
        D.leather(f, f.m_band(9.8, 10.35), LEA_L, 9.8, 10.35)
        if nm == "front":
            buckle(f, 2.0, 10.07, 0.75, 0.7)
        f.fill(f.m_band(11.65, 12.0), "#000000", a=0.55)
    t = cv.face(part, "top")
    t.fill(t.m_all(), WHT)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), "#1c0e08")
    D.clear_top_bottom_overlay(cv, part)


def paint(spec, cid, masks):
    cv = H.Canvas(256, False)
    D.base_fill(cv, JK)
    head(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    cv.face("head", "top", True).clear()
    skin, glow = cv.finish()
    return skin, glow, "anime_bangs_long", HAIR, False
