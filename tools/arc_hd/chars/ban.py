"""Ban, the Fox's Sin of Greed, HD 256x256 (docs/arc/hd_arc_sds1.md): this module paints both looks, `ban` and `ban_form2` (the last life; `ban_form2.py` only loads it).

What he wears is the 64x64 sheet's outfit (tools/arc/char_ban.py): a dark-red jacket open over a bare, muscled chest (two panels of red cloth at the sides, a dark fur collar), long dark-red sleeves with dark
cuffs, fingerless dark gloves, a double belt with a steel buckle and pouches, black trousers and calf boots with a lighter cuff. The sheet's face: sharp red eyes, thin brows, a smirk, a scar on the
left cheek, stubble. Added in the HD pass (not in the sheet, and flagged in the doc): the Fox's Sin mark low on the left of the stomach (docs/arc/ban.md lists it as canon), the trouser waist under the belt (the
sheet left bare skin there). Form 2: the jacket is darker and torn, the sleeves are torn off at the forearm, white hair, the eyes and the lines on the face glow Fountain green.

Drawn at 4x detail on both layers: the face (red anime eyes with highlights, thin angled brows, a one-sided smirk with a fang, the scar with its stitch marks, stubble, thick silver bangs), the chest
(pecs, collar bones, abs, obliques), the fur collar, the jacket panels with lapels, seams and folds, the double belt, the pouches and the buckle, the sleeves with elbow creases and rolled cuffs, the gloves,
the trousers and the boots with their straps.

Source (rule 10): no wiki page could be fetched here; the look is from memory and docs/arc/ban.md (which cites the wikis): a very tall, lean, muscled man with pale spiky hair and scarlet eyes.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES
SEEDS = {"front": 3, "back": 5, "west": 7, "east": 9}                                       # fixed seeds of the torn sleeve edges (deterministic)

SKIN = ("#c58a60", "#dba578", "#f0c496")
BASE = dict(
    hair="#d6dae6", hair_rgb=(214, 218, 230), jacket="#5a1c24", jacket_d="#2a1014", glove="#241c1a", belt="#3c281c", steel="#9aa0aa", trous="#1e1c22", boot="#241c1a",
    boot_cuff="#4a3a34", iris=("#5a0810", "#c83a3a", "#ff9a8a"), eye_glow=None, brow="#6c727e", form2=False, glow="#7be0a4",
)
FORM2 = dict(
    hair="#eef2f8", hair_rgb=(238, 242, 248), jacket="#42161d", jacket_d="#1e0a0e", glove="#241c1a", belt="#3c281c", steel="#999fa9", trous="#1e1c22", boot="#241c1a",
    boot_cuff="#4a3a34", iris=("#0f6a40", "#7be0a4", "#d8ffe8"), eye_glow="#7be0a4", brow="#aab0bc", form2=True, glow="#7be0a4",
)


# ================================================================================================ the head
def head_front(cv, P):
    f = cv.face("head", "front")
    K.clear_glow(f)
    H.skin_face(f, SKIN, hair=None, side=0.1, chin=0.09, shadow=0.0)
    pal = K.hair_palette(P["hair"])
    # a hard jaw: the face is long and lean, shaded at the cheek bones
    f.shade(f.m_poly([(0.0, 5.2), (0.9, 5.6), (1.6, 7.6), (2.6, 8.0), (0.0, 8.0)]), 0.9, 0.7)
    f.shade(f.m_poly([(8.0, 5.2), (7.1, 5.6), (6.4, 7.6), (5.4, 8.0), (8.0, 8.0)]), 0.9, 0.7)
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.6, 1.9), (6.9, 2.5), (6.0, 1.9), (5.1, 2.3), (4.2, 1.6), (3.0, 2.2), (2.1, 1.8), (1.1, 2.4), (-0.3, 2.0)]
    mass = f.m_poly(outline)
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - 1.8) / 2.0, 0, 1) * (f.Y > 1.4), 0.86)
    K.fringe_mass(f, pal, outline, 0.0, 2.4)
    # thick pointed bangs hang over the forehead, the long ones frame both cheeks
    for sh, (cx, w, top, ln, lean) in ((0.8, (6.7, 1.9, 1.3, 2.4, 0.55)), (0.9, (4.9, 1.7, 1.4, 1.9, -0.4)), (0.9, (3.0, 1.7, 1.3, 2.2, 0.35)), (1.0, (1.3, 1.9, 1.3, 2.6, -0.5)), (1.0, (4.0, 1.2, 1.5, 1.2, 0.0))):
        K.spike(f, pal, cx, w, top, ln, lean, sh)
    K.spike(f, pal, 0.45, 1.0, 1.0, 5.6, 0.2, 1.0)
    K.spike(f, pal, 7.55, 1.0, 1.0, 5.6, -0.2, 1.0)
    # thin angled brows, sharp red eyes (the outer corner up), a long jaw with stubble, the scar on the left cheek (the right of the picture), a one-sided smirk with a fang
    H.brow(f, (3.5, 3.55), (1.1, 3.0), P["brow"], 0.22, 0.09, arch=0.0)
    H.brow(f, (4.5, 3.55), (6.9, 3.0), P["brow"], 0.22, 0.09, arch=0.0)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 4.95, 2.5, 1.75, P["iris"], lash="#1e0e12", tilt=0.34, lash_w=0.44, flick=0.4, lid_shadow=0.4, glow=P["eye_glow"], iris_r=1.0, pupil="#240408")
    H.nose(f, 4.0, 6.2, "#b9805a", 0.22)
    f.shade(f.m_poly([(1.2, 6.6), (2.6, 7.6), (4.0, 7.9), (5.4, 7.6), (6.8, 6.6), (6.8, 8.1), (1.2, 8.1)]), 0.93, 0.8)      # the shadow of the stubble on the jaw
    for i in range(16):                                                                   # stubble: a few short dark strokes along the jaw line and on the chin
        x = 1.5 + 5.0 * H.hsh(i, 1, 9)
        y = 7.0 + 0.9 * H.hsh(i, 2, 9)
        if abs(x - 4.0) < 1.2 and y < 7.4:
            continue
        f.fill(f.m_line((x, y), (x + 0.04, y + 0.2), 0.1, 0.07), H.tone(SKIN[0], 0.5), 0.4)
    smirk = [(2.9, 7.25), (3.6, 7.2), (4.3, 7.15), (5.0, 7.0), (5.6, 6.7)]                # the corner on the right of the picture goes up
    f.fill(f.m_polyline(smirk, 0.17), "#6a2a2c", 0.95)
    f.fill(f.m_polyline([(3.2, 7.5), (4.2, 7.45), (5.0, 7.3)], 0.11), H.tone(SKIN[0], 0.85), 0.5)
    f.poly([(4.45, 7.15), (4.8, 7.07), (4.62, 7.6)], "#ffffff", 0.95)                       # a small fang
    scar = [(5.9, 5.5), (6.4, 6.1), (6.8, 6.6), (7.3, 7.2)]
    f.fill(f.m_polyline(scar, 0.28), H.tone(SKIN[0], 0.62), 0.7)
    f.fill(f.m_polyline(scar, 0.12), "#f3c6a8", 0.9)
    for k in ((6.2, 5.9), (6.65, 6.45), (7.05, 7.0)):                                     # the stitch marks across the scar
        f.fill(f.m_line((k[0] - 0.28, k[1] + 0.2), (k[0] + 0.28, k[1] - 0.2), 0.11, 0.11), H.tone(SKIN[0], 0.6), 0.65)
    if P["form2"]:
        for sgn in (1.0, -1.0):                                                           # the Fountain's light: lines across both cheeks and the brow
            x0 = 4.0 + sgn * 3.6
            K.glow_line(f, [(x0, 4.4), (x0 - sgn * 0.3, 5.6), (x0 - sgn * 0.85, 6.7), (x0 - sgn * 1.3, 7.6)], "#a8ffcc", 0.2, 0.9)
            K.glow_line(f, [(4.0 + sgn * 0.8, 2.6), (4.0 + sgn * 1.9, 3.05)], "#a8ffcc", 0.16, 0.75)


# ================================================================================================ the body
def fur_collar(f, P, front):
    fur, tip = "#2c1e1a", "#6a5044"
    m = f.m_rect(0.0, 0.0, f.w, 1.5)
    f.fill(m, H.lin((0, 0), (0, 1.5), [(0, H.tone(fur, 1.5)), (1, H.tone(fur, 0.7))]))
    H.scallops(f, m, 0.15, rows=2, cell=0.9, depth=1.1, base=None, tip=None, edge=H.tone(fur, 0.4), edge_a=0.5, shadow=0.24)
    for i in range(14):                                                                   # light fur tips
        x = f.w * H.hsh(i, 5, 4)
        y = 0.2 + 1.0 * H.hsh(i, 6, 4)
        f.fill(f.m_line((x, y), (x + 0.12, y + 0.4), 0.09, 0.03) * m, tip, 0.7)
    H.shadow_under(f, 1.5, 0.5, 0.3)


def chest(f, P):
    """The bare chest in the opening: a skin base, pecs with their lower shadow, collar bones, abs, obliques and the Fox's Sin mark on the left of the stomach."""
    s0, s1, s2 = SKIN
    left, right = 2.2, 5.8
    op = f.m_poly([(left + 0.05, 1.0), (right - 0.05, 1.0), (right + 0.25, 9.0), (left - 0.25, 9.0)])
    f.fill(op, H.lin((0, 1.0), (0, 9.0), [(0, H.tone(s1, 1.05)), (0.5, s1), (1, H.tone(s0, 0.95))]))
    # pecs
    for cx in (3.05, 4.95):
        pm = f.m_ellipse(cx, 3.55, 1.05, 0.95) * op
        f.fill(pm, H.rad((cx - 0.3, 3.2), 1.4, [(0, H.tone(s1, 1.12)), (0.7, s1), (1, H.tone(s0, 0.9))]), 0.9)
        f.fill(f.m_polyline([(cx - 0.95, 3.95), (cx - 0.3, 4.45), (cx + 0.5, 4.5), (cx + 0.98, 4.1)], 0.17) * op, H.tone(s0, 0.62), 0.6)
    f.fill(f.m_line((4.0, 2.2), (4.0, 4.6), 0.15, 0.1) * op, H.tone(s0, 0.7), 0.55)             # the sternum
    for sgn in (-1.0, 1.0):                                                               # the collar bones
        f.fill(f.m_polyline([(4.0 + sgn * 0.35, 1.5), (4.0 + sgn * 1.2, 1.75), (4.0 + sgn * 1.8, 1.65)], 0.12) * op, H.tone(s0, 0.7), 0.55)
    # abs: two columns of three blocks, the dark linea alba and the grooves between them
    for y in (5.05, 6.25, 7.45):
        f.fill(f.m_line((3.0, y), (5.0, y), 0.16, 0.16) * op, H.tone(s0, 0.55), 0.8)
        f.fill(f.m_line((3.1, y + 0.16), (4.9, y + 0.16), 0.07, 0.07) * op, H.tone(s2, 1.05), 0.35)
    f.fill(f.m_line((4.0, 4.7), (4.0, 8.9), 0.16, 0.14) * op, H.tone(s0, 0.55), 0.8)
    for sgn in (-1.0, 1.0):                                                               # the obliques: a shadow line down the sides of the abs
        f.fill(f.m_polyline([(4.0 + sgn * 1.45, 4.9), (4.0 + sgn * 1.55, 6.8), (4.0 + sgn * 1.9, 8.8)], 0.17) * op, H.tone(s0, 0.7), 0.5)
    f.shade(op * np.clip(1.0 - np.minimum(f.X - left, right - f.X) / 0.5, 0, 1), 0.82, 0.6)       # the jacket shades the edge of the chest
    fox_mark(f)


def fox_mark(f, cx=5.0, cy=7.55):
    """The Fox's Sin of Greed: a fox in profile (pointed ears, a long muzzle, a bushy tail wrapped round it), drawn in dark red ink on the left of the stomach (the right of the picture)."""
    ink, ink_l = "#6a1820", "#a03038"
    body = [(cx - 0.55, cy + 0.15), (cx - 0.05, cy - 0.2), (cx + 0.4, cy + 0.1), (cx + 0.5, cy + 0.7), (cx + 0.1, cy + 1.05), (cx - 0.35, cy + 0.8)]
    f.fill(f.m_poly(body), ink, 0.88)
    f.poly([(cx - 0.55, cy + 0.15), (cx - 0.78, cy - 0.45), (cx - 0.3, cy - 0.15)], ink, 0.88)                          # the head, the ears and the muzzle
    f.poly([(cx - 0.2, cy - 0.12), (cx - 0.12, cy - 0.7), (cx + 0.22, cy - 0.05)], ink, 0.88)
    f.poly([(cx - 0.55, cy + 0.15), (cx - 1.05, cy + 0.38), (cx - 0.5, cy + 0.38)], ink, 0.88)
    f.fill(f.m_polyline([(cx + 0.4, cy + 0.15), (cx + 1.05, cy + 0.0), (cx + 1.3, cy + 0.55), (cx + 0.95, cy + 1.15), (cx + 0.3, cy + 1.25)], 0.36), ink, 0.88)     # the tail
    f.fill(f.m_polyline([(cx + 0.45, cy + 0.12), (cx + 1.0, cy + 0.0), (cx + 1.2, cy + 0.5)], 0.1), ink_l, 0.7)
    f.ellipse(cx - 0.55, cy + 0.0, 0.07, 0.05, "#ffd8c0", a=0.9)


def jacket_panel(f, P, side, hem_torn=False):
    """One side of the jacket (side = -1 left of the picture, +1 right): the red panel with a lapel edge, a seam and folds."""
    j, jd = P["jacket"], P["jacket_d"]
    if side < 0:
        poly = [(-0.3, 0.9), (2.25, 1.0), (1.95, 9.0), (-0.3, 9.0)]
        xs = [0.5, 1.5]
    else:
        poly = [(8.3, 0.9), (5.75, 1.0), (6.05, 9.0), (8.3, 9.0)]
        xs = [6.5, 7.5]
    m = f.m_poly(poly)
    K.cloth(f, m, j, top=1.22, bottom=0.8, round_=0.1, fold_xs=xs, fold_depth=0.11, fold_y0=1.5, fold_y1=9.0, weave_k=0.0)
    H.sheen(f, m, [xs[0] + 0.25 * side * -1], width=0.2, strength=0.2, tilt=0.03, y0=2.0, y1=8.6, color="#c8808a")
    edge = [(2.25, 1.0), (1.95, 9.0)] if side < 0 else [(5.75, 1.0), (6.05, 9.0)]
    f.fill(f.m_polyline(edge, 0.34), jd, 0.95)                                           # the dark lapel facing
    f.fill(f.m_polyline([(edge[0][0] - side * 0.22, 1.1), (edge[1][0] - side * 0.22, 9.0)], 0.07), H.tone(j, 1.7), 0.5)
    K.seam(f, [(edge[0][0] - side * 0.5, 1.7), (edge[1][0] - side * 0.5, 8.7)], "#8a4a50", 0.06, 0.6)
    if P["form2"]:                                                                       # the torn inner edge of the form 2 jacket: ragged teeth in the skin, frayed threads
        for i in range(7):
            y = 1.6 + i * 1.05
            x = edge[0][0] + (edge[1][0] - edge[0][0]) * (y - 1.0) / 8.0
            d = 0.25 + 0.4 * H.hsh(i, 3, 7)
            f.poly([(x - side * 0.1, y), (x - side * (0.1 + d), y + 0.5), (x - side * 0.1, y + 0.95)], H.tone(SKIN[1], 0.95))
            f.poly([(x + side * 0.15, y), (x - side * (0.1 + d) * 0.4, y + 0.5), (x + side * 0.15, y + 0.95)], jd, 0.6)
    return m


def belts(f, P, front, with_pouches=True):
    b, st = P["belt"], P["steel"]
    top = f.m_rect(0.0, 9.0, f.w, 10.0)
    f.fill(top, H.lin((0, 9.0), (0, 10.0), [(0, H.tone(b, 1.45)), (0.4, b), (1, H.tone(b, 0.7))]))
    f.fill(f.m_rect(0.0, 9.02, f.w, 9.18), "#b08a58", 0.45)
    K.seam(f, [(0.0, 9.4), (f.w, 9.4)], "#c49a68", 0.055, 0.7, (0.3, 0.18), 0.2)
    low = f.m_rect(0.0, 10.0, f.w, 11.0)
    f.fill(low, H.lin((0, 10.0), (0, 11.0), [(0, H.tone("#201615", 1.7)), (0.3, "#201615"), (1, H.tone("#201615", 0.7))]))
    f.fill(f.m_rect(0.0, 9.98, f.w, 10.1), "#0e0a08", 0.8)
    K.seam(f, [(0.0, 10.6), (f.w, 10.6)], "#7a5a40", 0.055, 0.55, (0.3, 0.18), 0.15)
    H.shadow_under(f, 11.0, 0.5, 0.28)
    if front:
        for x0, x1 in ((0.15, 1.85), (6.15, 7.85)):                                      # the two pouches on the hips
            f.rect(x0, 9.05, x1, 10.95, H.lin((0, 9.05), (0, 10.95), [(0, "#6a4a30"), (1, "#3e2a1a")]), r=0.15)
            f.fill(f.m_rect(x0, 9.05, x1, 9.9, 0.12), H.tone("#6a4a30", 1.15), 0.9)
            f.fill(f.m_line((x0 + 0.05, 9.9), (x1 - 0.05, 9.9), 0.12, 0.12), "#1a100a", 0.8)
            H.stud(f, (x0 + x1) / 2, 9.55, 0.17, st)
            K.seam(f, [(x0 + 0.12, 9.2), (x1 - 0.12, 9.2)], "#c49a68", 0.05, 0.6, (0.28, 0.16), 0.0)
        f.rect(3.0, 8.8, 5.0, 10.9, "#2a3036", r=0.24)                                    # the steel buckle: a plate with a dark opening and a prong
        f.rect(3.08, 8.88, 4.92, 10.82, H.metal_paint(8.88, 10.82, st, 1.25), r=0.2)
        H.bevel(f, f.m_rect(3.08, 8.88, 4.92, 10.82, 0.2), depth=0.22, gain=1.3, spec=0.8)
        f.rect(3.5, 9.35, 4.5, 10.3, "#14181c", r=0.1)
        f.line((3.3, 9.85), (4.3, 9.85), H.metal_paint(9.55, 10.1, st, 1.1), 0.2, 0.14)
        f.ellipse(3.4, 9.1, 0.24, 0.12, "#f4f8ff", a=0.85)


def body_front(f, ov, P):
    j = P["jacket"]
    if not ov:
        f.fill(f.m_all(), SKIN[1])
        chest(f, P)
        jacket_panel(f, P, -1)
        jacket_panel(f, P, 1)
        f.fill(f.m_poly([(2.0, 11.0), (6.0, 11.0), (6.0, 12.0), (2.0, 12.0)]), H.lin((0, 11), (0, 12), [(0, H.tone(P["trous"], 0.9)), (1, H.tone(P["trous"], 0.7))]))   # the waist of the trousers
        f.fill(f.m_line((4.0, 11.0), (4.0, 12.0), 0.1, 0.1), H.tone(P["trous"], 1.9), 0.6)
        for x0, x1, s in ((-0.3, 2.0, -1), (6.0, 8.3, 1)):                                # the jacket tails below the belt
            f.fill(f.m_rect(x0, 11.0, x1, 12.0), H.tone(j, 0.7))
            f.fill(f.m_line((x0 if s > 0 else x1, 11.0), (x0 if s > 0 else x1, 12.0), 0.1, 0.1), H.tone(j, 0.4), 0.8)
        fur_collar(f, P, True)
    belts(f, P, True)


def body_back(f, ov, P):
    j, jd = P["jacket"], P["jacket_d"]
    if not ov:
        K.cloth(f, f.m_all(), j, top=1.2, bottom=0.8, round_=0.08, fold_xs=[1.4, 2.8, 5.2, 6.6], fold_depth=0.1, fold_y0=2.0, fold_y1=9.0, weave_k=0.0)
        H.sheen(f, f.m_rect(0, 1.5, 8, 9.0), [1.5, 6.4], width=0.22, strength=0.2, tilt=0.04, y0=2.0, y1=8.6, color="#c8808a")
        yoke = f.m_poly([(0, 1.4), (8, 1.4), (8, 2.0), (4.0, 2.7), (0, 2.0)])
        f.fill(yoke, jd, 0.5)
        K.seam(f, [(0.0, 2.0), (4.0, 2.7), (8.0, 2.0)], "#8a4a50", 0.06, 0.55)
        f.fill(f.m_line((4.0, 2.7), (4.0, 9.0), 0.17, 0.17), H.tone(j, 0.45), 0.6)
        K.seam(f, [(4.0, 2.9), (4.0, 8.9)], "#8a4a50", 0.07, 0.6)
        f.fill(f.m_rect(0.0, 11.0, 8.0, 12.0), H.tone(j, 0.7))
        f.fill(f.m_line((4.0, 11.0), (4.0, 12.0), 0.1, 0.1), H.tone(j, 0.4), 0.9)
        fur_collar(f, P, False)
    belts(f, P, False)


def body_side(f, ov, P, name):
    j = P["jacket"]
    if not ov:
        K.cloth(f, f.m_all(), j, top=1.2, bottom=0.8, round_=0.1, fold_xs=[1.2, 2.6], fold_depth=0.09, fold_y0=2.0, fold_y1=9.0, weave_k=0.0)
        edge = 0.55 if name == "east" else f.w - 0.55
        f.fill(f.m_line((edge, 1.5), (edge, 9.0), 0.3, 0.3), P["jacket_d"], 0.7)
        K.seam(f, [(f.w * 0.5, 1.8), (f.w * 0.5, 8.8)], "#8a4a50", 0.06, 0.55)
        f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.tone(j, 0.7))
        fur_collar(f, P, False)
    belts(f, P, False)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            if ov:
                K.clear_glow(f)
            if nm == "front":
                body_front(f, ov, P)
            elif nm == "back":
                body_back(f, ov, P)
            else:
                body_side(f, ov, P, nm)


# ================================================================================================ the arms
def sleeve(f, P, ov, name):
    j, jd, gl = P["jacket"], P["jacket_d"], P["glove"]
    end = 6.0 if P["form2"] else 8.5
    sm = f.m_band(0.0, end)
    K.cloth(f, sm, j, top=1.22, bottom=0.82, round_=0.12, fold_xs=[0.9, 2.0, 3.1], fold_depth=0.12, fold_y0=1.2, fold_y1=end, weave_k=0.0)
    f.fill(f.m_band(0.0, 1.0), H.tone(j, 1.35), 0.5)                                   # the shoulder catches the light
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), H.tone(j, 0.6), 0.7)
    K.seam(f, [(0.0, 0.8), (f.w, 0.8)], "#8a4a50", 0.055, 0.6, (0.3, 0.2), 0.15)
    for y in (4.3, 4.9):                                                               # the creases at the elbow
        f.fill(f.m_polyline([(0.0, y), (f.w * 0.4, y + 0.3), (f.w, y + 0.05)], 0.11) * sm, H.tone(j, 0.45), 0.6)
        f.fill(f.m_polyline([(0.0, y - 0.13), (f.w * 0.4, y + 0.17), (f.w, y - 0.08)], 0.06) * sm, H.tone(j, 1.7), 0.4)
    if P["form2"]:
        return
    # the rolled cuff, a dark band with a stitch
    H.trim_band(f, 8.0, 9.1, jd, spec=0.15, outline=0.6)
    K.seam(f, [(0.1, 8.55), (f.w - 0.1, 8.55)], "#8a4a50", 0.055, 0.7, (0.3, 0.18), 0.2)
    H.shadow_under(f, 9.1, 0.4, 0.3)


def forearm_and_glove(f, P, ov, name):
    gl = P["glove"]
    y0 = 6.0 if P["form2"] else 8.5
    K.limb_skin(f, SKIN, y0, 12.0, tendon=True)
    if P["form2"]:
        f.fill(f.m_band(y0, y0 + 1.0), H.tone(SKIN[0], 0.82), 0.35)
    # the fingerless glove: dark leather over the palm, a knuckle seam, the fingers bare below
    g = f.m_band(9.1, 11.05)
    f.fill(g, H.lin((0, 9.1), (0, 11.05), [(0, H.tone(gl, 1.5)), (0.5, gl), (1, H.tone(gl, 0.7))]))
    H.bevel(f, g, depth=0.2, gain=0.7, spec=0.25)
    K.seam(f, [(0.1, 9.55), (f.w - 0.1, 9.55)], "#8a6a54", 0.055, 0.6, (0.3, 0.18), 0.15)
    f.fill(f.m_line((0.0, 10.9), (f.w, 10.9), 0.14, 0.14), "#0c0808", 0.8)
    H.stud(f, f.w * 0.5, 10.1, 0.17, P["steel"])
    H.shadow_under(f, 9.1, 0.4, 0.25)
    K.fingers(f, SKIN, 11.05, 11.95, n=3)
    f.fill(f.m_band(11.3, 11.9), H.tone(SKIN[0], 0.88), 0.2)


def arms(cv, P):
    for part in ("arm_r", "arm_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                if ov:
                    K.clear_glow(f)
                forearm_and_glove(f, P, ov, nm)
                sleeve(f, P, ov, nm)
                if P["form2"]:
                    if ov:
                        K.ragged_hem(f, 5.6, depth=1.1, period=0.8, seed=SEEDS[nm] + (4 if part == "arm_l" else 0))
                    if not ov:                                                          # the base layer under the torn sleeve: the same jacket cloth ends in a frayed edge
                        H.shadow_under(f, 5.8, 0.6, 0.25)


# ================================================================================================ the legs
def legs(cv, P):
    t, b, c, st = P["trous"], P["boot"], P["boot_cuff"], P["steel"]
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                K.trousers(f, t, 0.0, 6.4, nm, knee=True, top=1.12, bottom=0.78)
                H.shadow_under(f, 6.1, 0.9, 0.35)
                # the boot cuff, a lighter folded leather band with a stitch, then the shaft with two buckled straps
                H.trim_band(f, 6.2, 7.3, c, spec=0.3, outline=0.6)
                K.seam(f, [(0.12, 6.75), (f.w - 0.12, 6.75)], "#b09878", 0.06, 0.7, (0.28, 0.18), 0.2)
                K.boot(f, b, 7.3, 11.25, nm, strap=None, gold=st)
                if nm == "front":
                    for y in (8.0, 9.3):
                        f.rect(0.0, y, f.w, y + 0.62, H.tone("#3c281c", 1.1))
                        f.fill(f.m_rect(0.0, y, f.w, y + 0.14), H.tone("#3c281c", 1.7), 0.8)
                        f.fill(f.m_rect(0.0, y + 0.55, f.w, y + 0.66), "#0e0a08", 0.7)
                        H.stud(f, f.w * 0.5, y + 0.31, 0.26, st)


# ================================================================================================ paint
def paint(spec, cid, masks):
    P = FORM2 if cid.endswith("_form2") else BASE
    spec["hair"]["color"] = P["hair"]
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair="anime_spiky")
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, P["hair_rgb"], skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    covers = K.all_covers(cv)
    head_front(cv, P)
    body(cv, P)
    arms(cv, P)
    legs(cv, P)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, P["hair_rgb"], slim
