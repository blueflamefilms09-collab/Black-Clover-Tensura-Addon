"""Escanor, the Lion's Sin of Pride, HD 256x256: the painter of both forms (chars/escanor.py and chars/escanor_form2.py are thin wrappers).

The man of the night and the dawn (the first form, the 64x64 sheet of tools/arc/char_escanor.py, docs/arc/escanor.md): a short brown coat with a stand collar and gold-brown piping over a cream shirt with
brass buttons, a plain belt with a gold buckle, dark trousers in calf boots, long sleeves with stitched cuffs; big shy brown eyes with eye bags, sad brows, a faint blush, short messy brown hair. Pride in
the daylight (the second form): tan skin, a black tank with a gold collar, a double belt with a glowing buckle, black trousers, and the bulky bronze pauldrons and bracers of the sheet (layered plates
with gold trim that glows); fierce amber eyes that glow, thick brows, stubble and a wide grin under a blazing orange mane. The Divine Axe Rhitta is a separate model (pmx_escanor*, right hand), so the
right fist is plain; the hair is the kit's `anime_short` (night) and `anime_mane` (noon).

Drawn at 4x detail on BOTH layers (the game drops the second layer and the hair beyond 24 blocks): the faces; the coat panels with piping, stitching and creases; the shirt placket with brass buttons;
the belt; sleeves with elbow wrinkles and stitched cuffs and bare hands; the trousers with knee creases; the boots with cuffs, straps and soles; for the day form the muscle shading of the bare arms,
the layered pauldrons with rivets, the bracers, the tank with its hem stitching. The second layer of the day arms is a little bulkier than the sheet's (pauldrons to row 4, bracers to row 11): bulky
armour pieces are the owner's reference style. Original art from memory of the character's look (docs/arc/escanor.md says the clothes are design choices of the game; the English and Japanese wikis could
not be fetched in this session), nothing traced.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds2 as S  # noqa: E402

A, H = S.A, S.H

F1 = {
    "skin": ("#d7a084", "#efc4a2", "#fbdcbd"),
    "hair": "#6a4a30", "hair_rgb": (106, 74, 48), "hair_pal": S.hair_pal("#6a4a30", dark="#3a2518", light="#90693f", high="#c29560"), "style": "anime_short",
    "coat": "#5c3e2a", "trim": "#9a7440", "trim_l": "#d8b268", "shirt": "#cfc4ac", "shirt_d": "#998562", "belt": "#3c281c", "gold": "#dda331", "trousers": "#241c20", "boot": "#34261e",
    "iris": ("#4a3014", "#8a6a3a", "#d8b878"), "glow": "#ffb040", "form2": False,
}
F2 = {
    "skin": ("#b07c58", "#d8a072", "#efc08e"),
    "hair": "#ff8c14", "hair_rgb": (255, 140, 20), "hair_pal": S.hair_pal("#ff8c14", dark="#8a3a08", light="#ffb04a", high="#ffe48a"), "style": "anime_spike_tall",
    "tank": "#1c1618", "gold": "#daa030", "gold_l": "#ffd070", "bronze": "#b8782a", "belt": "#3c281c", "buckle": "#ffd070", "trousers": "#1e1a1e", "boot": "#2a2220", "boot_top": "#4c413b",
    "iris": ("#8a3a08", "#ffb040", "#fff0a0"), "glow": "#ffd070", "form2": True,
}


# ================================================================================================ the head
def head_front(cv, P):
    f = cv.face("head", "front")
    pal = P["hair_pal"]
    H.skin_face(f, P["skin"], hair=None, side=0.09, chin=0.07, shadow=0.0)
    if P["form2"]:
        head_day(f, P, pal)
    else:
        head_night(f, P, pal)


def head_sides(cv, P):
    for nm in ("west", "east"):
        for ov in (False, True):
            S.ear(cv.face("head", nm, ov), nm, P["skin"], 4.7)


def head_night(f, P, pal):
    mass = S.hair_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.0), (7.4, 2.45), (6.4, 2.0), (5.2, 2.7), (4.0, 2.2), (2.9, 2.8), (1.7, 2.3), (0.7, 2.7), (-0.3, 2.2)])
    S.strands(f, pal, (0.8, 2.0, 3.2, 4.8, 6.0, 7.2), 0.0, 2.3, mass, 0.3)
    S.lock(f, 1.0, 1.4, 1.2, 2.2, 0.3, pal, 0.9)
    S.lock(f, 2.9, 1.3, 1.6, 1.7, -0.15, pal, 1.0)
    S.lock(f, 5.1, 1.3, 1.7, 1.6, 0.1, pal, 0.95)
    S.lock(f, 7.0, 1.4, 1.2, 2.1, -0.3, pal, 1.0)
    S.lock(f, 0.42, 0.9, 0.9, 4.2, 0.0, pal, 1.0)
    S.lock(f, 7.58, 0.9, 0.9, 4.2, 0.0, pal, 1.0)
    # sad brows: the inner ends are raised
    H.brow(f, (3.55, 3.5), (1.2, 4.0), "#4a3220", 0.27, 0.12, arch=0.02)
    H.brow(f, (4.45, 3.5), (6.8, 4.0), "#4a3220", 0.27, 0.12, arch=0.02)
    for cx in (2.4, 5.6):                                                                          # eye bags: a tired crescent under each eye
        f.fill(f.m_polyline([(cx - 1.1, 6.3), (cx - 0.3, 6.65), (cx + 0.6, 6.6), (cx + 1.2, 6.25)], 0.2), "#a8705c", 0.55)
        f.fill(f.m_polyline([(cx - 0.9, 6.55), (cx, 6.9), (cx + 0.9, 6.55)], 0.14), "#b88470", 0.3)
    H.eye(f, 2.3, 5.1, 2.5, 2.6, P["iris"], lash="#2a1a10", tilt=-0.08, lash_w=0.3, flick=0.18, iris_r=1.08, lid_shadow=0.2)
    H.eye(f, 5.7, 5.1, 2.5, 2.6, P["iris"], lash="#2a1a10", tilt=-0.08, lash_w=0.3, flick=0.18, iris_r=1.08, lid_shadow=0.2)
    H.blush(f, 1.1, 6.35, 0.95, 0.5, "#e8806e", 0.4)
    H.blush(f, 6.9, 6.35, 0.95, 0.5, "#e8806e", 0.4)
    H.nose(f, 4.0, 6.05, "#c98a72", 0.28)
    H.mouth_smile(f, 4.0, 7.2, 1.8, color="#8a4a3a", lip="#d08c78", depth=0.04, thick=0.2)


def head_day(f, P, pal):
    mass = S.hair_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 1.8), (7.2, 1.5), (6.2, 2.1), (5.0, 1.2), (4.0, 1.9), (3.0, 1.3), (1.8, 2.1), (0.7, 1.5), (-0.3, 1.9)])
    S.strands(f, pal, (0.9, 2.1, 3.3, 4.7, 5.9, 7.1), 0.0, 1.7, mass, 0.35)
    S.lock(f, 0.9, 1.5, 0.9, 2.0, 0.3, pal, 0.9)
    S.lock(f, 7.1, 1.5, 0.9, 2.0, -0.3, pal, 0.9)
    S.lock(f, 0.4, 0.9, 0.8, 3.8, 0.0, pal, 1.0)
    S.lock(f, 7.6, 0.9, 0.8, 3.8, 0.0, pal, 1.0)
    # thick, straight, angry brows
    H.brow(f, (3.5, 3.6), (0.9, 3.2), "#6a3208", 0.55, 0.3, arch=0.0)
    H.brow(f, (4.5, 3.6), (7.1, 3.2), "#6a3208", 0.55, 0.3, arch=0.0)
    for cx in (2.3, 5.7):                                                                          # fierce narrow eyes that glow
        H.eye(f, cx, 4.95, 2.5, 1.45, P["iris"], lash="#1a0c04", tilt=0.28, lash_w=0.5, flick=0.34, iris_r=1.0, lid_shadow=0.4, hi=0.8)
        f.glow_fill(f.m_ellipse(cx, 5.05, 0.55, 0.46, soft=2.0), P["glow"], 0.6)
    H.nose(f, 4.0, 6.0, "#b87a52", 0.3)
    # stubble over the jaw and the lip
    for i in range(46):
        x = 0.6 + 6.8 * H.hsh(i, 1)
        y = 5.9 + 2.0 * H.hsh(i, 2)
        if (x < 2.0 or x > 6.0 or y > 6.9) and not (3.2 < x < 4.8 and y < 6.5):
            f.fill(f.m_ellipse(x, y, 0.1, 0.08), "#6a4020", 0.55)
    # a wide grin: teeth showing, the corners up
    H.mouth_smile(f, 4.0, 6.75, 4.0, color="#7a2a1a", lip="#b0604a", depth=0.42, thick=0.26, teeth="#fff8ec", open_h=0.7)
    f.fill(f.m_line((4.0, 6.9), (4.0, 7.55), 0.08, 0.08), "#d8c8b0", 0.6)
    f.fill(f.m_rect(0.0, 7.4, 8.0, 8.0), H.tone(P["skin"][0], 0.9), 0.35)                            # the heavy jaw is shaded


# ================================================================================================ the night form: a coat and a shirt
def coat_front(f, P):
    c, tr, tl, sh, sd = P["coat"], P["trim"], P["trim_l"], P["shirt"], P["shirt_d"]
    S.panel(f, f.m_all(), c, 1.2, 0.8, 0.08, folds=[0.9, 1.9, 6.1, 7.1], fold_depth=0.10, y0=1.8, y1=9.0)
    for x0, x1 in ((0.0, 2.2), (5.8, 8.0)):
        pm = f.m_rect(x0, 1.5, x1, 9.0)
        H.sheen(f, pm, [x0 + 0.6 if x0 < 4 else x0 + 0.5], width=0.2, strength=0.14, tilt=0.04, y0=2.0, y1=8.6, color="#c8b49c")
        S.edge_light(f, pm, x0, x0 + 0.2, c, 1.7, 0.35)
    # the shirt in the opening of the coat
    pl = f.m_poly([(2.5, 0.0), (5.5, 0.0), (5.9, 9.1), (2.1, 9.1)])
    H.cloth(f, pl, sh, top=1.1, bottom=0.88, round_=0.04, fold_xs=[3.3, 4.8], fold_depth=0.07, fold_y0=3.0, fold_y1=9.0, weave_k=0.0)
    f.shade(pl * np.clip(1.0 - np.minimum(f.X - (2.5 - (f.Y / 9.1) * 0.4), (5.5 + (f.Y / 9.1) * 0.4) - f.X) / 0.55, 0, 1), 0.7, 0.9)
    f.fill(f.m_line((4.0, 1.6), (4.0, 9.0), 0.17, 0.17) * pl, sd, 0.55)
    f.fill(f.m_line((4.17, 1.6), (4.17, 9.0), 0.1, 0.1) * pl, H.tone(sh, 1.12), 0.5)
    for y in (2.6, 4.4, 6.2, 8.0):
        H.stud(f, 4.0, y, 0.26, "#d8b45a", rim_color="#5a4010", spec=0.9)
    # piping along the opening and a stitch beside it
    for s, x0, x1 in ((-1.0, 2.45, 2.05), (1.0, 5.55, 5.95)):
        H.trim_line(f, [(x0, 1.5), (x1, 9.05)], tr, width=0.3, spec=0.45, outline=0.6)
        S.seam(f, [(x0 + s * 0.45, 1.9), (x1 + s * 0.48, 9.0)], tl, 0.06, 0.6)
    # the stand collar: two stiff gold-brown bands, open at the front
    for sgn in (1.0, -1.0):
        x = (lambda v: v) if sgn > 0 else (lambda v: f.w - v)
        col = f.m_poly([(x(0.0), 0.0), (x(2.7), 0.0), (x(2.5), 1.0), (x(2.0), 1.65), (x(0.0), 1.65)])
        f.fill(col, H.lin((0, 0), (0, 1.65), [(0, H.tone(tr, 1.18)), (1, H.tone(tr, 0.7))]))
        H.bevel(f, col, depth=0.22, gain=1.0, spec=0.3)
        f.fill(f.m_polyline([(x(2.5), 1.0), (x(2.0), 1.65), (x(0.0), 1.65)], 0.12), H.tone(tr, 0.38), 0.85)
        S.seam(f, [(x(0.2), 0.8), (x(2.2), 0.8)], tl, 0.06, 0.6)
    H.shadow_under(f, 1.68, 0.5, 0.3)
    belt(f, P, True)
    f.fill(f.m_rect(0.0, 10.4, f.w, 12.0), H.lin((0, 10.4), (0, 12.0), [(0, H.tone(c, 0.9)), (1, H.tone(c, 0.7))]))       # the coat tail
    f.fill(f.m_line((4.0, 10.4), (4.0, 12.0), 0.2, 0.2), H.tone(c, 0.4), 0.9)
    S.seam(f, [(0.1, 11.6), (3.8, 11.6)], tl, 0.055, 0.55, (0.3, 0.18), 0.2)
    S.seam(f, [(4.2, 11.6), (7.9, 11.6)], tl, 0.055, 0.55, (0.3, 0.18), 0.2)
    H.shadow_under(f, 10.4, 0.5, 0.25)


def belt(f, P, front=False):
    b, g = P["belt"], P["gold"]
    strap = f.m_rect(0.0, 9.0, f.w, 10.4)
    f.fill(strap, H.lin((0, 9.0), (0, 10.4), [(0, H.tone(b, 1.5)), (0.4, b), (1, H.tone(b, 0.65))]))
    f.fill(f.m_rect(0.0, 9.03, f.w, 9.2), "#a07848", 0.5)
    f.fill(f.m_rect(0.0, 9.0, f.w, 9.1), "#0e0806", 0.7)
    S.seam(f, [(0.0, 9.45), (f.w, 9.45)], "#c8a060", 0.055, 0.7, (0.3, 0.18), 0.2)
    S.seam(f, [(0.0, 9.95), (f.w, 9.95)], "#c8a060", 0.055, 0.6, (0.3, 0.18), 0.2)
    H.shadow_under(f, 10.4, 0.5, 0.3)
    if front:
        f.rect(2.6, 8.7, 5.4, 10.7, "#3a2a08", r=0.3)                                               # the buckle: a gold frame, a dark opening, a prong
        f.rect(2.68, 8.78, 5.32, 10.62, H.metal_paint(8.78, 10.62, g, 1.15), r=0.26)
        H.bevel(f, f.m_rect(2.68, 8.78, 5.32, 10.62, 0.26), depth=0.26, gain=1.3, spec=0.75)
        f.rect(3.2, 9.25, 4.8, 10.15, "#1d130c", r=0.14)
        f.line((2.95, 9.7), (4.25, 9.7), H.metal_paint(9.4, 10.0, g, 1.1), 0.22, 0.16)
        f.ellipse(3.15, 9.0, 0.24, 0.14, "#fff6c8", a=0.85)


def coat_back(f, P):
    c, tr, tl = P["coat"], P["trim"], P["trim_l"]
    S.panel(f, f.m_all(), c, 1.18, 0.8, 0.08, folds=[1.5, 3.0, 5.0, 6.5], fold_depth=0.10, y0=1.8, y1=9.0)
    H.sheen(f, f.m_rect(0, 1.8, 8, 9.0), [1.6, 6.3], width=0.22, strength=0.14, tilt=0.04, y0=2.0, y1=8.6, color="#c8b49c")
    band = f.m_rect(0, 0, 8, 1.65)
    f.fill(band, H.lin((0, 0), (0, 1.65), [(0, H.tone(tr, 1.18)), (1, H.tone(tr, 0.7))]))
    H.bevel(f, band, depth=0.22, gain=1.0, spec=0.3)
    f.fill(f.m_rect(0, 1.62, 8, 1.76), H.tone(tr, 0.38), 0.85)
    S.seam(f, [(0.2, 0.8), (7.8, 0.8)], tl, 0.06, 0.6)
    H.shadow_under(f, 1.72, 0.5, 0.3)
    S.seam(f, [(4.0, 2.0), (4.0, 8.9)], tl, 0.075, 0.6)
    f.fill(f.m_line((4.0, 2.0), (4.0, 8.9), 0.17, 0.17), H.tone(c, 0.45), 0.55)
    for x in (2.2, 5.8):
        f.fill(f.m_polyline([(x, 2.6), (x + (0.12 if x < 4 else -0.12), 5.2), (x, 8.8)], 0.14), H.tone(c, 0.5), 0.5)
    belt(f, P)
    f.fill(f.m_rect(0.0, 10.4, f.w, 12.0), H.lin((0, 10.4), (0, 12.0), [(0, H.tone(c, 0.9)), (1, H.tone(c, 0.7))]))
    f.fill(f.m_line((4.0, 10.4), (4.0, 12.0), 0.2, 0.2), H.tone(c, 0.4), 0.9)
    H.shadow_under(f, 10.4, 0.5, 0.25)


def coat_side(f, P, name):
    c, tr = P["coat"], P["trim"]
    S.panel(f, f.m_all(), c, 1.18, 0.8, 0.08, folds=[1.3, 2.7], fold_depth=0.09, y0=1.8, y1=9.0)
    band = f.m_rect(0, 0, f.w, 1.65)
    f.fill(band, H.lin((0, 0), (0, 1.65), [(0, H.tone(tr, 1.18)), (1, H.tone(tr, 0.7))]))
    H.bevel(f, band, depth=0.22, gain=1.0, spec=0.3)
    f.fill(f.m_rect(0, 1.62, f.w, 1.76), H.tone(tr, 0.38), 0.85)
    H.shadow_under(f, 1.72, 0.5, 0.3)
    edge = f.w - 0.7 if name == "west" else 0.7
    H.trim_line(f, [(edge, 2.0), (edge, 9.0)], tr, width=0.26, spec=0.4, outline=0.55)
    belt(f, P)
    f.fill(f.m_rect(0.0, 10.4, f.w, 12.0), H.lin((0, 10.4), (0, 12.0), [(0, H.tone(c, 0.9)), (1, H.tone(c, 0.7))]))
    H.shadow_under(f, 10.4, 0.5, 0.25)


# ================================================================================================ the day form: a black tank, bare skin
def skin_body(f, P, a=1.0):
    """Bare skin with a soft muscle shading: lit from the upper left, darker toward the edges."""
    s0, s1, s2 = P["skin"]
    m = f.m_all()
    f.fill(m, H.lin((0, 0), (0, f.h), [(0, s2), (0.4, s1), (1, s0)]), a)
    return m


def tank_front(f, P):
    t, g = P["tank"], P["gold"]
    # the chest: bare skin in a V-shaped neckline, pecs shaded
    S.panel(f, f.m_all(), t, 1.5, 0.7, 0.06, folds=[1.2, 2.2, 5.8, 6.8], fold_depth=0.12, y0=2.0, y1=9.0)
    for x0, x1 in ((0.7, 3.9), (4.1, 7.3)):
        H.sheen(f, f.m_rect(x0, 2.4, x1, 8.6), [(x0 + x1) / 2.0], width=0.3, strength=0.1, tilt=0.0, y0=2.6, y1=8.4, color="#7a6a74")
    v = f.m_poly([(1.6, 0.0), (6.4, 0.0), (4.0, 3.5)])
    f.fill(v, H.lin((0, 0), (0, 3.5), [(0, P["skin"][2]), (0.7, P["skin"][1]), (1, P["skin"][0])]))
    f.fill(f.m_line((4.0, 0.2), (4.0, 3.2), 0.16, 0.12) * v, H.tone(P["skin"][0], 0.78), 0.55)               # the sternum
    for sgn in (1.0, -1.0):
        x = (lambda vv: vv) if sgn > 0 else (lambda vv: f.w - vv)
        f.fill(f.m_polyline([(x(1.8), 0.7), (x(2.6), 1.7), (x(3.5), 1.9)], 0.3) * v, H.tone(P["skin"][0], 0.78), 0.45)  # the pecs
    # the gold collar of the tank: a thick band along the V and over the shoulders
    for pts in ([(1.55, 0.0), (4.0, 3.55), (6.45, 0.0)],):
        H.trim_line(f, pts, g, width=0.5, spec=0.6, outline=0.65)
        f.glow_fill(f.m_polyline(pts, 0.28), P["glow"], 0.35)
    band = f.m_rect(0.0, 0.0, 1.6, 1.3)
    f.fill(band, H.metal_paint(0.0, 1.3, g, 0.9))
    H.bevel(f, band, depth=0.25, gain=1.0, spec=0.4)
    band2 = f.m_rect(6.4, 0.0, 8.0, 1.3)
    f.fill(band2, H.metal_paint(0.0, 1.3, g, 0.9))
    H.bevel(f, band2, depth=0.25, gain=1.0, spec=0.4)
    f.fill(f.m_band(1.25, 1.4, x0=0.0, x1=1.6), H.tone(g, 0.4), 0.8)
    f.fill(f.m_band(1.25, 1.4, x0=6.4, x1=8.0), H.tone(g, 0.4), 0.8)
    # the armholes: skin at the very edge of the shoulders
    S.seam(f, [(1.0, 2.2), (0.85, 8.6)], "#5a4a54", 0.06, 0.55)
    S.seam(f, [(7.0, 2.2), (7.15, 8.6)], "#5a4a54", 0.06, 0.55)
    belts2(f, P, True)
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(P["trousers"], 1.2)), (1, H.tone(P["trousers"], 0.7))]))
    H.shadow_under(f, 11.0, 0.5, 0.3)


def tank_back(f, P):
    t, g = P["tank"], P["gold"]
    S.panel(f, f.m_all(), t, 1.5, 0.7, 0.06, folds=[1.4, 2.8, 5.2, 6.6], fold_depth=0.12, y0=2.0, y1=9.0)
    sk = f.m_poly([(0.0, 0.0), (8.0, 0.0), (8.0, 1.5), (4.0, 3.0), (0.0, 1.5)])
    f.fill(sk, H.lin((0, 0), (0, 3.0), [(0, P["skin"][1]), (1, P["skin"][0])]))
    f.fill(f.m_polyline([(4.0, 0.3), (4.0, 2.8)], 0.2) * sk, H.tone(P["skin"][0], 0.78), 0.5)         # the spine
    H.trim_line(f, [(0.0, 1.5), (4.0, 3.05), (8.0, 1.5)], g, width=0.45, spec=0.6, outline=0.65)
    f.glow_fill(f.m_polyline([(0.0, 1.5), (4.0, 3.05), (8.0, 1.5)], 0.26), P["glow"], 0.3)
    belts2(f, P)
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(P["trousers"], 1.2)), (1, H.tone(P["trousers"], 0.7))]))
    H.shadow_under(f, 11.0, 0.5, 0.3)


def tank_side(f, P, name):
    t, g = P["tank"], P["gold"]
    S.panel(f, f.m_all(), t, 1.4, 0.7, 0.06, folds=[1.3, 2.7], fold_depth=0.1, y0=2.0, y1=9.0)
    sk = f.m_rect(0.0, 0.0, f.w, 2.2)
    f.fill(sk, H.lin((0, 0), (0, 2.2), [(0, P["skin"][1]), (1, P["skin"][0])]))
    H.trim_line(f, [(0.0, 2.25), (f.w, 2.25)], g, width=0.4, spec=0.5, outline=0.6)
    belts2(f, P)
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(P["trousers"], 1.2)), (1, H.tone(P["trousers"], 0.7))]))
    H.shadow_under(f, 11.0, 0.5, 0.3)


def belts2(f, P, front=False):
    """The double belt of the day form: two brown straps, the upper one studded; a big sun buckle on the front that glows."""
    b, g, gl = P["belt"], P["gold"], P["buckle"]
    S.belt_straps(f, b, H.tone(b, 0.62), 9.0, 11.0, split=10.05, stitch_color="#c8a060")
    for x in (0.8, 1.9, 6.1, 7.2):
        H.stud(f, x, 9.52, 0.18, g)
    if front:
        f.ellipse(4.0, 10.0, 1.45, 1.45, "#3a2a08", a=0.95)
        f.ellipse(4.0, 9.98, 1.36, 1.36, H.metal_paint(8.6, 11.4, gl, 1.1))
        H.bevel(f, f.m_ellipse(4.0, 9.98, 1.36, 1.36), depth=0.3, gain=1.2, spec=0.7)
        for i in range(8):                                                                           # the rays of a sun
            a = i * math.pi / 4 + math.pi / 8
            f.fill(f.m_line((4.0 + 0.55 * math.cos(a), 10.0 + 0.55 * math.sin(a)), (4.0 + 1.1 * math.cos(a), 10.0 + 1.1 * math.sin(a)), 0.2, 0.1), H.tone(g, 0.55), 0.75)
        f.ellipse(4.0, 10.0, 0.5, 0.5, "#fff6c8")
        f.glow_fill(f.m_ellipse(4.0, 10.0, 1.2, 1.2, soft=3.0), P["glow"], 0.7)


def body(cv, P):
    for nm in S.SIDES:
        for f in S.pair(cv, "body", nm):
            if P["form2"]:
                tank_front(f, P) if nm == "front" else tank_back(f, P) if nm == "back" else tank_side(f, P, nm)
            else:
                coat_front(f, P) if nm == "front" else coat_back(f, P) if nm == "back" else coat_side(f, P, nm)


# ================================================================================================ the arms
def hand(f, P, y0, name):
    """A bare hand from y0 to the bottom: skin, a shaded edge, finger lines and a knuckle row."""
    s0, s1, s2 = P["skin"]
    m = f.m_band(y0, 12.0)
    f.fill(m, H.lin((0, y0), (0, 12.0), [(0, H.tone(s1, 0.92)), (0.5, s1), (1, H.tone(s0, 0.95))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.12 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
    H.shadow_under(f, y0, 0.6, 0.3, mask=m)
    if name in ("front", "back"):
        for x in (f.w * 0.25, f.w * 0.5, f.w * 0.75):
            f.fill(f.m_line((x, y0 + 0.7), (x, 11.9), 0.17, 0.12) * m, H.tone(s0, 0.7), 0.6)
        f.fill(f.m_band(y0 + 0.55, y0 + 0.7) * m, H.tone(s0, 0.72), 0.45)
    f.fill(f.m_band(11.75, 12.0) * m, H.tone(s0, 0.8), 0.5)


def arm_night(cv, part, P):
    c, tr, tl = P["coat"], P["trim"], P["trim_l"]
    for nm in S.SIDES:
        for f in S.pair(cv, part, nm):
            sl = f.m_band(0.0, 9.5)
            S.panel(f, sl, c, 1.2, 0.8, 0.12, folds=[f.w * 0.28, f.w * 0.62, f.w * 0.84], fold_depth=0.10, y0=0.6, y1=9.0)
            H.sheen(f, sl, [f.w * 0.3], width=0.22, strength=0.14, tilt=0.03, y0=0.8, y1=8.6, color="#c8b49c")
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.35), H.tone(c, 0.65), 0.7)                                # shoulder seam
            S.seam(f, [(0.0, 0.7), (f.w, 0.7)], tl, 0.055, 0.45, (0.3, 0.2), 0.15)
            for y, k in ((4.7, 0.5), (5.3, 0.4)):                                                       # elbow creases
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.25), (f.w, y + 0.05)], 0.12) * sl, H.tone(c, 0.45), k)
                f.fill(f.m_polyline([(0.0, y - 0.15), (f.w * 0.5, y + 0.1), (f.w, y - 0.1)], 0.07) * sl, H.tone(c, 1.8), k * 0.5)
            hand(f, P, 10.9, nm)
            H.trim_band(f, 9.3, 10.95, tr, spec=0.35, outline=0.6)                                         # the stitched cuff
            S.seam(f, [(0.12, 10.1), (f.w - 0.12, 10.1)], tl, 0.06, 0.8, (0.3, 0.18), 0.2)
            for x in (0.55, f.w - 0.55):
                H.stud(f, x, 10.1, 0.13, "#e0c070", spec=0.6)
            H.shadow_under(f, 10.95, 0.5, 0.3)


def metal_plate(f, y0, y1, color, hi=1.0, cyl=0.3, edge=0.34):
    """A bevelled metal lame across a face: the polished-metal gradient, a cylinder shading across the width (the plate wraps the arm), a bright line under its top edge and a dark gap under its bottom edge."""
    pm = f.m_rect(0.0, y0, f.w, y1)
    f.fill(pm, H.metal_paint(y0, y1, color, hi))
    across = np.abs(f.X - f.w * 0.38) / (f.w * 0.62)
    f.shade(pm, 1.0 - cyl * H.smoothstep(0.25, 1.0, across), 1.0)
    H.bevel(f, pm, depth=0.26, gain=0.9, spec=0.5, spec_pow=10)
    f.fill(f.m_band(y0 + 0.06, y0 + 0.2) * pm, H.tone(color, 1.9), 0.55)
    f.fill(f.m_band(y1 - 0.2, y1 + 0.06), H.tone(color, edge), 0.92)
    return pm


def arm_day(cv, part, P):
    s0, s1, s2 = P["skin"]
    br, g, gl = P["bronze"], P["gold"], P["gold_l"]
    for nm in S.SIDES:
        for f, ov in zip(S.pair(cv, part, nm), (False, True)):
            # the bare arm: muscle shading (deltoid under the pauldron, the biceps bulge, the forearm), veins
            m = f.m_all()
            f.fill(m, H.lin((0, 0), (0, 12.0), [(0, s2), (0.35, s1), (0.7, H.tone(s1, 0.96)), (1, s0)]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(m, 1.0 - 0.2 * (1.0 - H.smoothstep(0.0, 1.0, side)), 0.9)
            f.fill(f.m_ellipse(f.w * 0.42, 5.3, f.w * 0.3, 1.4, soft=2.0), s2, 0.4)                    # the biceps catches the light
            f.fill(f.m_polyline([(f.w * 0.6, 4.6), (f.w * 0.5, 6.0), (f.w * 0.55, 7.3)], 0.14), H.tone(s0, 0.75), 0.4)
            f.fill(f.m_polyline([(f.w * 0.35, 9.8), (f.w * 0.55, 10.5)], 0.12), H.tone(s0, 0.75), 0.35)
            hand(f, P, 10.9, nm)
            # the pauldron: three lames from the shoulder down, gold - bronze - gold, each a bevelled metal plate with a dark gap under it
            H.shadow_under(f, 4.3, 0.8, 0.45)
            for (y0, y1), col in zip(((2.7, 4.3), (1.4, 3.1), (0.0, 1.8)), (g, br, g)):                  # lower lames first, the upper ones overlap them
                metal_plate(f, y0, y1, col, 1.0, 0.32)
            for y in (1.78, 3.08, 4.28):
                f.fill(f.m_band(y - 0.5, y - 0.28) * f.m_rect(0.0, 0.0, f.w, 4.3), gl, 0.75, glow=P["glow"])
            f.glow_fill(f.m_band(4.05, 4.3), P["glow"], 0.5)
            for x in (f.w * 0.22, f.w * 0.78):
                H.stud(f, x, 0.85, 0.17, gl, rim_color="#3a2008", spec=0.9)
                H.stud(f, x, 2.3, 0.17, gl, rim_color="#3a2008", spec=0.9)
                H.stud(f, x, 3.7, 0.17, gl, rim_color="#3a2008", spec=0.9)
            # the bracer: a flared cuff, gold rims top and bottom, a raised ridge down the middle
            bm = metal_plate(f, 6.1, 10.95, br, 1.05, 0.34, 0.3)
            f.fill(f.m_polyline([(f.w * 0.5, 7.1), (f.w * 0.5, 10.0)], 0.34) * bm, H.tone(br, 1.45), 0.6)
            f.fill(f.m_polyline([(f.w * 0.5 + 0.3, 7.1), (f.w * 0.5 + 0.3, 10.0)], 0.14) * bm, H.tone(br, 0.4), 0.7)
            for y0, y1 in ((6.1, 6.9), (10.15, 10.95)):
                H.trim_band(f, y0, y1, g, spec=0.7, glow=P["glow"], outline=0.7)
                f.glow_fill(f.m_rect(0.0, y0, f.w, y1), P["glow"], 0.4)
            for y in (7.6, 8.5, 9.4):
                H.stud(f, f.w * 0.5, y, 0.16, gl, rim_color="#3a2008", spec=0.9)
            H.shadow_under(f, 10.95, 0.5, 0.3)
            H.shadow_under(f, 6.9, 0.5, 0.3, mask=f.m_rect(0.0, 6.9, f.w, 8.0))


def arms(cv, P):
    for part in ("arm_r", "arm_l"):
        (arm_day if P["form2"] else arm_night)(cv, part, P)


# ================================================================================================ the legs
def leg_night(f, P, name):
    tl, g = P["trim_l"], P["gold"]
    c, tz, bt = P["coat"], P["trousers"], P["boot"]
    trs = f.m_band(0.0, 6.4)
    S.panel(f, trs, tz, 1.45, 0.75, 0.12, folds=[1.2, 2.8], fold_depth=0.10, y0=0.4, y1=6.0)
    tail = f.m_band(0.0, 3.1)                                                                           # the coat tail over the thigh
    S.panel(f, tail, c, 1.3, 0.75, 0.12, folds=[1.1, 2.9], fold_depth=0.10, y0=0.2, y1=2.9)
    H.sheen(f, tail, [f.w * 0.3], width=0.2, strength=0.12, tilt=0.03, y0=0.3, y1=2.7, color="#c8b49c")
    f.fill(f.m_rect(0.0, 2.95, f.w, 3.2), H.tone(c, 0.4), 0.9)
    S.seam(f, [(0.12, 2.6), (f.w - 0.12, 2.6)], tl, 0.055, 0.55, (0.3, 0.18), 0.2)
    H.shadow_under(f, 3.15, 0.8, 0.4)
    for y, k in ((4.2, 0.5), (4.9, 0.4)):                                                               # knee creases
        f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.28), (f.w - 0.2, y + 0.05)], 0.1) * trs, H.tone(tz, 0.5), k)
        f.fill(f.m_polyline([(0.2, y - 0.15), (f.w * 0.5, y + 0.13), (f.w - 0.2, y - 0.10)], 0.06) * trs, H.tone(tz, 2.0), k * 0.5)
    H.shadow_under(f, 6.35, 0.8, 0.38)
    cuff = f.m_band(6.0, 7.2)                                                                           # the folded top of the boot
    f.fill(cuff, H.lin((0, 6.0), (0, 7.2), [(0, "#6a5a52"), (1, "#3a302c")]))
    H.bevel(f, cuff, depth=0.28, gain=1.1, spec=0.3)
    f.fill(f.m_band(7.1, 7.25), "#0e0a08", 0.8)
    boot = f.m_band(7.25, 11.2)
    f.fill(boot, H.lin((0, 7.25), (0, 11.2), [(0, H.tone(bt, 1.35)), (0.5, bt), (1, H.tone(bt, 0.7))]))
    H.sheen(f, boot, [f.w * 0.3], width=0.2, strength=0.28, tilt=0.04, y0=7.6, y1=10.6, color="#c8a898")
    f.fill(f.m_polyline([(0.0, 9.4), (f.w * 0.5, 9.65), (f.w, 9.45)], 0.11), H.tone(bt, 0.45), 0.7)
    if name == "front":
        f.rect(0.0, 8.0, f.w, 8.75, H.tone(P["belt"], 1.1), r=0.0)
        f.fill(f.m_rect(0.0, 8.0, f.w, 8.15), "#a07848", 0.5)
        H.stud(f, f.w * 0.5, 8.4, 0.3, g)
        f.ellipse(f.w * 0.5, 10.3, 1.2, 0.62, H.tone(bt, 1.7), a=0.35, soft=2.0)
    S.sole(f, 11.0, 12.0, welt="#6e4e38")
    if name in ("west", "east"):
        S.seam(f, [(f.w * 0.5, 3.4), (f.w * 0.5, 5.9)], "#6a5a62", 0.06, 0.45)


def leg_day(f, P, name):
    tz, bt, g = P["trousers"], P["boot"], P["gold"]
    trs = f.m_band(0.0, 6.5)
    S.panel(f, trs, tz, 1.6, 0.7, 0.12, folds=[1.2, 2.8], fold_depth=0.12, y0=0.4, y1=6.0)
    H.sheen(f, trs, [f.w * 0.3], width=0.22, strength=0.12, tilt=0.03, y0=0.6, y1=5.6, color="#7a6a74")
    for y, k in ((3.4, 0.5), (4.1, 0.4)):
        f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.28), (f.w - 0.2, y + 0.05)], 0.1) * trs, H.tone(tz, 0.5), k)
    H.shadow_under(f, 6.4, 0.8, 0.38)
    cuff = f.m_band(6.1, 7.2)
    f.fill(cuff, H.lin((0, 6.1), (0, 7.2), [(0, P["boot_top"]), (1, H.tone(P["boot_top"], 0.62))]))
    H.bevel(f, cuff, depth=0.26, gain=1.1, spec=0.3)
    f.fill(f.m_band(7.1, 7.25), "#0e0a08", 0.8)
    boot = f.m_band(7.25, 11.2)
    f.fill(boot, H.lin((0, 7.25), (0, 11.2), [(0, H.tone(bt, 1.35)), (0.5, bt), (1, H.tone(bt, 0.7))]))
    H.sheen(f, boot, [f.w * 0.3], width=0.2, strength=0.28, tilt=0.04, y0=7.6, y1=10.6, color="#c8a898")
    f.fill(f.m_polyline([(0.0, 9.4), (f.w * 0.5, 9.65), (f.w, 9.45)], 0.11), H.tone(bt, 0.45), 0.7)
    if name == "front":
        f.rect(0.0, 8.0, f.w, 8.75, H.tone(P["belt"], 1.1), r=0.0)
        f.fill(f.m_rect(0.0, 8.0, f.w, 8.15), "#a07848", 0.5)
        H.stud(f, f.w * 0.5, 8.4, 0.3, g)
        f.ellipse(f.w * 0.5, 10.3, 1.2, 0.62, H.tone(bt, 1.7), a=0.35, soft=2.0)
    S.sole(f, 11.0, 12.0, welt="#6e4e38")


def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in S.SIDES:
            for f in S.pair(cv, part, nm):
                (leg_day if P["form2"] else leg_night)(f, P, nm)


# ================================================================================================ paint
def paint_form(spec, cid, masks, P):
    spec["hair"]["color"] = P["hair"]
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=P["style"])
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, P["hair_rgb"], skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    parts = ("body", "leg_r", "leg_l") if P["form2"] else S.LIMB_PARTS
    cov = S.covers(cv, parts)
    head_front(cv, P)
    head_sides(cv, P)
    body(cv, P)
    arms(cv, P)
    legs(cv, P)
    S.hold_cover(cv, cov)
    if P["form2"]:                                                                                   # the day arms: bare skin under the plates on the base layer; the plates are the second layer
        for part in ("arm_r", "arm_l"):
            for nm in S.SIDES:
                f = cv.face(part, nm, True)
                f.fill(f.m_rect(0.0, 4.35, f.w, 6.05), "#000000", mode="erase")                              # between the pauldron and the bracer the arm is bare
                f.glow[..., 3] *= 1.0 - f.m_rect(0.0, 4.35, f.w, 6.05)
                f.fill(f.m_band(10.95, 12.0), "#000000", mode="erase")                                      # the hand is bare
                f.glow[..., 3] *= 1.0 - f.m_band(10.95, 12.0)
    skin, glow = cv.finish()
    return skin, glow, hair, P["hair_rgb"], slim
