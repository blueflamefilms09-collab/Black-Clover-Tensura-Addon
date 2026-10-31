"""Dante Zogratis, the eldest of the Dark Triad and the crowned Body Magic brawler of the Spade Kingdom, HD 256x256 (docs/arc/hd_arc_bc5.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_dante_zogratis.py): a brown tunic with a gold stand collar, dark patches and a gold hem down to the thigh, short sleeves with gold cuffs over bare
forearms, cream bandage wraps on the hands, a double belt with a gold buckle, dark trousers and calf boots with gold cuffs, a gold crown with a red stone on the brow. The red scarf (a ring round the
neck and two tails down the back) and, in the second form, the horns and the broken halo are the `pmx_dante_zogratis*` extras: nothing of them is painted on the skin, the front of the neck is only
kept plain under the scarf.
Drawn at 4x detail on both layers: a broad hard face with thick slanted dark brows, sharp green eyes, a wide nose, a wolfish grin with a goatee and stubble, a scar down the left cheek and a
diagonal scar over the right eye; the crown (a gold band with three points and a red cabochon) round the whole head; the tunic with lit panels, stitched patches, a lace placket, a gold collar with a
fold and a geometric gold hem; the double belt with a square gold buckle; the muscled bare forearms with a scar and a vein; the bandages; trousers and boots.
Form 2 (`dante_zogratis_form2.py`, the hybrid of the Gates, 100% Lucifero) runs this same code with FORM2 = True: violet skin, a mauve tunic with lilac trim, slit glowing violet eyes, fangs, glowing
veins and cracks on the face and the arms (the glow map exists only there).

Source (rule 10): no wiki page could be fetched in this environment (English and Japanese fandom are blocked); the look is from memory of the series plus docs/arc/remodel.md and the sheet: a huge, grinning,
dark-haired man with a goatee, scars and a crown. The cut of the tunic and the colours are the sheet's choice.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

FORM2 = False
HAIR_STYLE = "pompadour"                                      # swept back and up from the crown; the crown band stays visible under it (the anime cuts hide the brow)
HAIR = "#181010"
HAIR_RGB = (24, 16, 16)
SKIN_NAME = "tan"
TUNIC = "#93603a"
TUNIC_L = "#b98354"
TUNIC_D = "#5e3a20"
PATCH = "#734624"
GOLD = "#dcb058"
GOLD_D = "#8a6228"
BELT_A = "#6e4426"
BELT_B = "#1e1412"
TROUS = "#4e3c2c"
BOOT = "#3e2c22"
WRAP = "#e0d2b0"
IRIS = ("#1c6a2a", "#4ab84e", "#c0f2b0")
STITCH = "#dcc08a"
RED = "#c8322e"
FANG = False


def skin_tones():
    if FORM2:
        return ("#7a52a0", "#9a6cc0", "#b890d8"), "#5a3a78"
    return K.skin3(SKIN_NAME)


# ================================================================================================ the head
def crown(f, name, overlay):
    """A gold crown band round the head (y 0.7..2.0) with three points and a red cabochon on the front; drawn on both layers of every side."""
    band = f.m_rect(-0.5, 0.95, f.w + 0.5, 2.05)
    f.fill(band, H.lin((0, 0.95), (0, 2.05), [(0, K.lit(GOLD, 1.35)), (0.45, GOLD), (1, H.tone(GOLD, 0.62))]))
    f.fill(f.m_rect(-0.5, 0.95, f.w + 0.5, 1.2), K.lit(GOLD, 1.9), 0.7)
    f.fill(f.m_rect(-0.5, 1.9, f.w + 0.5, 2.05), "#3a2410", 0.85)
    f.fill(f.m_rect(-0.5, 1.5, f.w + 0.5, 1.62), GOLD_D, 0.55)
    pts = [(-0.3, 0.95)]
    n = 3 if name in ("front", "back") else 2
    for i in range(n):
        cx = f.w * (i + 0.5) / n
        pts += [(cx - 0.75, 0.95), (cx, 0.05), (cx + 0.75, 0.95)]
    pts += [(f.w + 0.3, 0.95)]
    pts_m = f.m_poly(pts)
    f.fill(pts_m, H.lin((0, 0.0), (0, 1.0), [(0, K.lit(GOLD, 1.7)), (1, GOLD)]))
    f.fill(f.m_polyline(pts, 0.18) * pts_m, "#3a2410", 0.6)
    if name == "front":
        H.gem(f, 4.0, 1.5, 0.38, RED if not FORM2 else "#b878ff", setting=GOLD_D, facets=5, spec=0.9, glow=FORM2 and "#d8b0ff" or None, ry=0.42)
    else:
        for x in np.arange(0.9, f.w, 1.7):
            H.stud(f, float(x), 1.5, 0.15, GOLD_D, spec=0.4)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    skin, crease = skin_tones()
    K.face_base(f, skin, side=0.1, chin=0.08)
    # a broad hard face: the cheekbones lit, the temples and the jaw a little darker
    f.fill(f.m_ellipse(1.5, 5.9, 0.9, 0.7, soft=3.0), K.lit(skin[1], 1.08), 0.25)
    f.fill(f.m_ellipse(6.5, 5.9, 0.9, 0.7, soft=3.0), K.lit(skin[1], 1.08), 0.25)
    f.shade(f.m_rect(0.0, 6.4, 0.9, 8.0, soft=2.0), 0.9, 0.6)
    f.shade(f.m_rect(7.1, 6.4, 8.0, 8.0, soft=2.0), 0.9, 0.6)
    dark = HAIR
    # thick brows, low and slanted toward the nose: a glare
    brow_c = "#1c1210" if not FORM2 else "#1a1024"
    H.brow(f, (3.55, 4.15), (0.9, 3.45), brow_c, 0.5, 0.2, arch=0.05)
    H.brow(f, (4.45, 4.15), (7.1, 3.45), brow_c, 0.5, 0.2, arch=0.05)
    f.fill(f.m_ellipse(2.25, 4.15, 1.4, 0.35, soft=2.5), crease, 0.35)
    f.fill(f.m_ellipse(5.75, 4.15, 1.4, 0.35, soft=2.5), crease, 0.35)
    if FORM2:
        for cx in (2.4, 5.6):
            H.eye(f, cx, 5.2, 2.5, 1.55, ("#4a2a8a", "#c8a0ff", "#f4eaff"), lash="#12081e", tilt=0.34, lash_w=0.46, flick=0.4, lid_shadow=0.42,
                  glow="#b888ff", hi=0.85, pupil="#12081e")
            f.fill(f.m_ellipse(cx, 5.25, 0.1, 0.62), "#12081e", 0.95)
    else:
        for cx in (2.4, 5.6):
            H.eye(f, cx, 5.2, 2.5, 1.6, IRIS, lash="#16100e", tilt=0.28, lash_w=0.46, flick=0.4, lid_shadow=0.4, hi=0.9)
    f.fill(f.m_polyline([(1.1, 5.95), (2.4, 6.2), (3.5, 5.95)], 0.14), crease, 0.5)               # the lower lids: a hard, tired shadow
    f.fill(f.m_polyline([(4.5, 5.95), (5.6, 6.2), (6.9, 5.95)], 0.14), crease, 0.5)
    # a wide nose
    H.nose(f, 4.0, 6.25, crease, 0.34)
    f.fill(f.m_line((3.3, 6.45), (3.0, 6.3), 0.2, 0.14), crease, 0.7)
    f.fill(f.m_line((4.7, 6.45), (5.0, 6.3), 0.2, 0.14), crease, 0.7)
    # the moustache over the lip, the grin under it (a wolfish open mouth, the upper teeth, a fang), the goatee on the chin; a stubble shadow on the jaw
    beard = dark if not FORM2 else "#1a1024"
    jaw = f.m_poly([(0.2, 6.3), (1.9, 6.9), (2.7, 7.6), (5.3, 7.6), (6.1, 6.9), (7.8, 6.3), (7.8, 8.1), (0.2, 8.1)])
    K.stubble(f, jaw, "#4a3024" if not FORM2 else "#3a2a58", 0.34, step=0.4, seed=3, p=0.5)
    H.mouth_smile(f, 4.0, 6.9, 4.0, color="#4a1e1c", lip=K.lit(skin[1], 0.9), depth=0.28, thick=0.2, teeth="#fff8ee", open_h=0.6)
    if FORM2 or FANG:
        for x in (2.85, 5.15):
            f.poly([(x - 0.2, 7.0), (x + 0.2, 7.0), (x, 7.62)], "#ffffff", 0.97)
    else:
        f.poly([(5.0, 7.05), (5.4, 7.05), (5.2, 7.58)], "#ffffff", 0.95)
    f.fill(f.m_polyline([(2.2, 6.55), (3.2, 6.4), (4.0, 6.5), (4.8, 6.4), (5.8, 6.55)], 0.34), beard, 1.0)                       # the moustache
    f.fill(f.m_poly([(3.1, 7.5), (4.9, 7.5), (4.65, 8.2), (3.35, 8.2)]), beard, 1.0)                                                # the goatee
    f.fill(f.m_line((3.9, 7.55), (3.95, 8.0), 0.14, 0.1), "#5a4034" if not FORM2 else "#4a3a68", 0.5)
    # the scars: one down the left cheek, one slashed over the right eye
    K.scar(f, [(1.6, 5.5), (1.35, 6.3), (1.5, 7.0), (1.2, 7.7)], "#e8a898" if not FORM2 else "#d8b8f0", "#6a2c28" if not FORM2 else "#4a2a6a", 0.3)
    for y in (6.0, 6.6, 7.2):
        f.fill(f.m_line((1.15, y), (1.8, y + 0.05), 0.12, 0.12), "#7a3c34" if not FORM2 else "#4a2a6a", 0.55)
    K.scar(f, [(7.15, 3.0), (6.85, 4.1), (6.4, 5.1), (6.0, 6.0), (5.6, 6.8)], "#e8a898" if not FORM2 else "#d8b8f0", "#6a2c28" if not FORM2 else "#4a2a6a", 0.3)
    for t in (0.2, 0.45, 0.7):
        x = 7.15 - 1.55 * t
        y = 3.0 + 3.8 * t
        f.fill(f.m_line((x - 0.38, y - 0.05), (x + 0.38, y + 0.07), 0.12, 0.12), "#7a3c34" if not FORM2 else "#4a2a6a", 0.55)
    if FORM2:
        for pts in (
            [(0.9, 3.3), (1.2, 4.0), (0.8, 4.7), (1.1, 5.4)],
            [(7.1, 4.3), (6.8, 5.0), (7.3, 5.8), (6.95, 6.5)],
            [(3.2, 2.4), (3.6, 3.0), (3.4, 3.5)],
            [(4.8, 2.4), (4.5, 3.0), (4.7, 3.5)],
        ):
            K.jag(f, pts, "#c8a0ff", 0.14, 0.95, glow="#b888ff", core="#f4eaff")
    crown(f, "front", False)
    f2 = cv.face("head", "front", True)
    f2.clear()
    crown(f2, "front", True)


def head_rest(cv, img64, spec):
    """The sides, back and top of the head: a clean scalp and skin (the 3D hair covers most of it) and the crown band round all four sides."""
    skin, _ = skin_tones()
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, skin)
    for nm in ("west", "east", "back"):
        crown(cv.face("head", nm, False), nm, False)
        ov = cv.face("head", nm, True)
        ov.clear()
        crown(ov, nm, True)


# ================================================================================================ the tunic
def tunic_color():
    return "#6c4a60" if FORM2 else TUNIC


def sleeve_color():
    return "#5a3a52" if FORM2 else "#7a4a2a"


def trim_color():
    return "#c8a0ff" if FORM2 else GOLD


def collar(f, name):
    c = trim_color()
    m = f.m_band(0.0, 1.9)
    f.fill(m, H.lin((0, 0), (0, 1.9), [(0, K.lit(c, 1.25)), (0.5, c), (1, H.tone(c, 0.66))]))
    H.bevel(f, m, depth=0.25, gain=1.15, spec=0.35)
    f.fill(f.m_rect(0.0, 1.78, f.w, 1.92), "#2a1608", 0.8)
    H.shadow_under(f, 1.92, 0.55, 0.34)
    for x in np.arange(0.5, f.w, 1.0):                                                                 # a row of small gold scales: the collar is embossed
        f.fill(f.m_poly([(x - 0.4, 0.55), (x + 0.4, 0.55), (x, 1.35)]), H.tone(c, 0.7), 0.5)
    if name == "front":
        f.fill(f.m_polyline([(3.6, 0.0), (4.0, 1.7), (4.4, 0.0)], 0.2), "#2a1608", 0.8)


def patch(f, x0, y0, x1, y1, color, rot=0.0):
    """A sewn patch: a darker rectangle with a lit top edge and a dashed stitch round it."""
    m = f.m_rect(x0, y0, x1, y1, 0.08)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(color, 1.12)), (1, H.tone(color, 0.78))]))
    f.fill(f.m_rect(x0, y0, x1, y0 + 0.14), K.lit(color, 1.5), 0.45)
    f.fill(f.m_rect(x0, y1 - 0.14, x1, y1), "#000000", 0.2)
    K.seam(f, [(x0 + 0.2, y0 + 0.2), (x1 - 0.2, y0 + 0.2), (x1 - 0.2, y1 - 0.2), (x0 + 0.2, y1 - 0.2), (x0 + 0.2, y0 + 0.2)], STITCH if not FORM2 else "#d8bcf8", 0.06, 0.8, (0.3, 0.2), 0.2)


def hem(f, y0, y1=12.0):
    """The gold geometric hem of the tunic: a band with a row of triangles and a dark line over it."""
    c = trim_color()
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(c, 1.3)), (0.5, c), (1, H.tone(c, 0.66))]))
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.16), "#2a1608", 0.8)
    f.fill(f.m_rect(0.0, y0 + 0.16, f.w, y0 + 0.3), K.lit(c, 1.8), 0.5)
    mid = (y0 + y1) / 2.0 + 0.08
    for x in np.arange(0.0, f.w + 0.5, 1.0):
        f.fill(f.m_poly([(x - 0.5, mid - 0.32), (x + 0.5, mid - 0.32), (x, mid + 0.36)]), H.tone(c, 0.52), 0.75)
    f.fill(f.m_rect(0.0, y1 - 0.14, f.w, y1), "#2a1608", 0.7)


def body_front(f, ov):
    c = tunic_color()
    K.cloth(f, f.m_all(), c, top=1.35, bottom=0.8, round_=0.16, fold_xs=[1.4, 6.6], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    # the chest: two lit panels, the lace placket between them (a dark opening crossed by a gold cord)
    for x0, x1 in ((0.0, 3.1), (4.9, 8.0)):
        K.sheen(f, f.m_rect(x0, 2.0, x1, 8.8), [x0 + 0.7 if x0 < 4 else x1 - 0.7], 2.2, 8.6, 0.24, "#f0c890", width=0.3)
    f.fill(f.m_rect(0.0, 2.0, 0.3, 8.9), K.lit(c, 1.6), 0.35)
    f.fill(f.m_rect(7.7, 2.0, 8.0, 8.9), "#000000", 0.3)
    f.fill(f.m_poly([(3.35, 1.9), (4.65, 1.9), (4.8, 8.9), (3.2, 8.9)]), H.tone(c, 0.46), 0.95)
    for y in np.arange(2.6, 8.7, 1.1):
        K.line(f, [(3.2, y), (4.8, y + 0.7)], STITCH if not FORM2 else "#d8bcf8", 0.16, 0.85)
        K.line(f, [(4.8, y), (3.2, y + 0.7)], STITCH if not FORM2 else "#d8bcf8", 0.16, 0.85)
    patch(f, 5.7, 3.0, 7.5, 5.4, PATCH if not FORM2 else "#5a3a52")
    patch(f, 0.5, 6.0, 2.2, 8.2, PATCH if not FORM2 else "#5a3a52")
    collar(f, "front")
    belts(f, True)
    hem(f, 10.9)


def belts(f, front=False):
    """The double belt: a wide brown strap over a black one, each with stitching; on the front the square gold buckle and two studs."""
    K.belt_band(f, 8.85, 10.0, BELT_A, stitch="#b08a58")
    K.belt_band(f, 10.0, 10.9, BELT_B, stitch="#6a5038")
    if front:
        K.buckle(f, 4.0, 9.8, 2.5, 1.9, trim_color() if not FORM2 else "#c8a0ff", hole="#1a0e08")
        for x in (1.0, 7.0):
            H.stud(f, x, 9.45, 0.2, GOLD_D if not FORM2 else "#8a68c0", spec=0.6)
    else:
        H.stud(f, 4.0, 9.45, 0.2, GOLD_D, spec=0.5)


def body_back(f, ov):
    c = tunic_color()
    K.cloth(f, f.m_all(), c, top=1.3, bottom=0.78, round_=0.16, fold_xs=[1.6, 6.4], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    K.sheen(f, f.m_rect(0, 2, 8, 8.8), [1.6, 6.2], 2.2, 8.4, 0.2, "#f0c890", width=0.3)
    collar(f, "back")
    f.fill(f.m_line((4.0, 2.0), (4.0, 8.8), 0.18, 0.18), H.tone(c, 0.45), 0.7)
    K.seam(f, [(3.7, 2.1), (3.7, 8.7)], STITCH if not FORM2 else "#d8bcf8", 0.06, 0.55)
    patch(f, 1.0, 3.2, 3.0, 5.6, PATCH if not FORM2 else "#5a3a52")
    patch(f, 5.2, 5.6, 7.2, 8.0, PATCH if not FORM2 else "#5a3a52")
    belts(f)
    hem(f, 10.9)


def body_side(f, ov, name):
    c = tunic_color()
    K.cloth(f, f.m_all(), c, top=1.3, bottom=0.78, round_=0.16, fold_xs=[1.4], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.1), (ed, 8.7)], STITCH if not FORM2 else "#d8bcf8", 0.06, 0.6)
    belts(f)
    hem(f, 10.9)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ the arms and legs
def arm(cv, part):
    skin, crease = skin_tones()
    c = tunic_color()
    tc = trim_color()
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 3.0)
            K.cloth(f, sleeve, sleeve_color(), top=1.3, bottom=0.8, round_=0.18, fold_xs=[1.0], fold_depth=0.07, fold_y0=0.6, fold_y1=2.8)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.32), K.lit(c, 1.7), 0.5)
            K.seam(f, [(0.0, 0.75), (f.w, 0.75)], STITCH if not FORM2 else "#d8bcf8", 0.05, 0.55)
            # the gold cuff of the sleeve: a bevelled band with the geometric row
            K.band(f, 3.0, 4.1, tc)
            for x in np.arange(0.4, f.w, 0.9):
                f.fill(f.m_poly([(x - 0.35, 3.25), (x + 0.35, 3.25), (x, 3.8)]), H.tone(tc, 0.55), 0.7)
            H.shadow_under(f, 4.1, 0.5, 0.26)
            # the bare, muscled forearm
            fm = f.m_band(4.1, 7.0)
            f.fill(fm, H.lin((0, 4.1), (0, 7.0), [(0, H.tone(skin[1], 0.88)), (0.3, skin[1]), (1, H.tone(skin[0], 0.92))]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(fm, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
            K.sheen(f, fm, [f.w * 0.4], 4.4, 6.8, 0.14, "#fff0e0", width=0.4)
            K.muscle(f, fm, (skin[0], skin[1], skin[2]), [(f.w * 0.6, 4.3), (f.w * 0.5, 5.4), (f.w * 0.58, 6.6)], [(f.w * 0.68, 4.4), (f.w * 0.6, 5.5)], 0.7, 0.45)
            H.shadow_under(f, 4.15, 0.7, 0.32, mask=fm)
            if nm in ("front", "back") and part == "arm_r":
                K.scar(f, [(f.w * 0.3, 5.0), (f.w * 0.5, 5.6), (f.w * 0.7, 6.2)], "#d89484", "#7a3c34", 0.18)
            if nm in ("front", "back"):
                f.fill(f.m_polyline([(f.w * 0.75, 4.4), (f.w * 0.7, 5.3), (f.w * 0.78, 6.4)], 0.16) * fm, "#b87a68" if not FORM2 else "#8a68c0", 0.45)
            # the bandages from the wrist band down over the hand
            K.wraps(f, 7.0, 12.0, WRAP, slant=0.45 if part == "arm_r" else -0.45, end_tail=(part == "arm_r"))
            f.fill(f.m_band(11.8, 12.0), "#000000", 0.25)
            if FORM2:
                if nm == "front" and part == "arm_l":
                    K.jag(f, [(f.w * 0.3, 4.5), (f.w * 0.55, 5.2), (f.w * 0.4, 6.0), (f.w * 0.62, 6.8)], "#c8a0ff", 0.14, 0.95, glow="#b888ff", core="#f4eaff")
                if nm == "front" and part == "arm_r":
                    K.jag(f, [(f.w * 0.6, 4.4), (f.w * 0.35, 5.2), (f.w * 0.55, 6.0)], "#c8a0ff", 0.14, 0.95, glow="#b888ff", core="#f4eaff")


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", tunic_color(), edge=H.tone(tunic_color(), 0.5), k_top=1.4, overlay=ov)


def legs(cv):
    c = tunic_color()
    tc = trim_color()
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS if not FORM2 else "#3e2c3c", 0.0, 7.2, nm, knee=True, top=1.35, bottom=0.8, seam_color="#8a7258")
                skirt = f.m_band(0.0, 3.0)                                                              # the tunic skirt over the trousers
                K.cloth(f, skirt, c, top=1.3, bottom=0.82, round_=0.14, fold_xs=[2.0], fold_depth=0.07, fold_y0=0.2, fold_y1=2.9)
                hem(f, 3.0, 4.0)
                H.shadow_under(f, 4.0, 0.8, 0.4)
                # the gold boot cuff and the boot
                K.boot(f, BOOT if not FORM2 else "#2e2230", 6.0, nm, strap=True, strap_color="#6a4a30", gold=GOLD_D, cuff_color=tc, cuff_h=1.0, welt="#6a4a30", sole="#08060a")
                if nm == "front":
                    f.fill(f.m_ellipse(f.w * 0.5, 9.7, 1.4, 0.7, soft=2.0), K.lit(BOOT, 1.9), 0.3)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    head_rest(cv, img64, spec)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    arm_tops(cv)
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)
