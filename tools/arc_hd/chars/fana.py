"""Fana, the Hatred of the Third Eye (Eye of the Midnight Sun, Salamander's caller), HD 256x256 (docs/arc/hd_arc_bc5.md).

What she wears is the 64x64 sheet's outfit (tools/arc/char_fana.py, docs/arc/fana.md): a short, slim girl's body in a high-collared furry maroon dress with a cream fur collar and a pale
eye emblem on the chest, long sleeves with cream fur cuffs, a brown belt with a gold buckle, a dark hem, brown knee boots with a rose turned cuff; thick wavy pink hair (the attached 3D hair
`anime_long`), large sea-green eyes, a purple mage stone on the forehead framed by a red tattoo with the third eye under it. The light hooded robe of the Eye, the grimoire, the satchel, the
floating spark of Salamander and (second form) the crown of flames are the `pmx_fana` / `pmx_fana_form2` extras and are not painted on the skin.
Drawn at 4x detail on both layers: a small angry face (sharp shaded sea-green eyes with a lash flick, tilted brows, a blush, a clenched frown), the mage stone (faceted, set in dark metal) and the
third eye (a red almond with a pupil and a highlight) with the tattoo lines that frame them, the dress with a fleece of short fur strokes and lit panels, the fur collar with a tufted edge, the
Eye emblem (an almond with an iris), a studded belt with a gold buckle, a dark hem with a rose zigzag, fur cuffs and boots with a rose turned cuff.
Glow: the stone, the third eye and the emblem (soft, the sheet's `trim` glow). Form 2 (`fana_form2.py`, the Evil Heart of her last stand) runs this same code with FORM2 = True: a brighter red
dress with burning orange trim and cracks, burning orange eyes with slit pupils, a mad grin with fangs, a gold third eye, a hot pink stone, glowing veins; the hair is the wild mane (`anime_mane`).

Source (rule 10): no wiki page could be fetched in this environment (English and Japanese fandom are blocked); the look is from memory of the series plus docs/arc/fana.md and the sheet.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

FORM2 = False
HAIR_STYLE = "anime_long_open"                                # the attached long pink hair WITHOUT a centre fringe (hair_map has anime_long, whose fringe hides the stone and the third eye): it frames the face and falls behind the shoulders
HAIR = "#e0709c"                                              # a deeper rose than the sheet's #e88fb4: the 3D hair tint renders paler than its hex
HAIR_RGB = (224, 112, 156)
FUR = "#f2e6d8"
FUR_L = "#fffaf0"
FUR_D = "#b8a698"
ROSE = "#b08088"
GOLD = "#c8a040"
GOLD_D = "#7a5a1c"
BELT = "#3c281c"
BOOT = "#5a3a2c"
HEM = "#4a1220"
IRIS = ("#10584a", "#3fb894", "#c0fae0")
IRIS2 = ("#8a1a08", "#ff5a2a", "#ffe098")
EMBER = "#ffb347"
EMBER_G = "#ff7a2a"


def dress():
    return "#b02030" if FORM2 else "#7a1a2c"


def trim():
    return EMBER if FORM2 else ROSE


def hem_c():
    return "#6a1020" if FORM2 else HEM


def skin_tones():
    if FORM2:
        return ("#e8a890", "#f8c8b0", "#ffe0cc"), "#c07868"
    return K.skin3("fair")


# ================================================================================================ the head
def lens(f, cx, cy, a, bt, bb, n=21):
    """A pointed almond (a lens) of half-width a, top half-height bt and bottom half-height bb."""
    xs = np.linspace(-1.0, 1.0, n)
    top = [(cx + a * float(u), cy - bt * (1.0 - float(u) ** 2)) for u in xs]
    bot = [(cx + a * float(u), cy + bb * (1.0 - float(u) ** 2)) for u in xs[::-1]]
    return f.m_poly(top + bot)


def third_eye(f, cx, cy):
    """The third eye on the forehead: a dark-rimmed almond, a pale rim of sclera, a red (gold in the second form) iris with a pupil and a highlight; it lights the glow map."""
    col, gcol = ("#ffd35a", EMBER) if FORM2 else ("#f04830", "#ff6a4a")
    f.fill(lens(f, cx, cy, 1.3, 0.72, 0.66), "#3a0a14", 0.95)
    m = lens(f, cx, cy, 1.12, 0.56, 0.5)
    f.fill(m, H.lin((0, cy - 0.56), (0, cy + 0.5), [(0, "#d8b0a0"), (0.4, "#f8e6d6"), (1, "#fff4e8")]))
    ix = f.m_ellipse(cx, cy + 0.02, 0.52, 0.52) * m
    f.fill(ix, H.lin((0, cy - 0.5), (0, cy + 0.54), [(0, H.tone(col, 0.6)), (0.5, col), (1, K.lit(col, 1.3))]), 1.0, glow=gcol)
    f.fill(f.m_ellipse(cx, cy + 0.03, 0.2, 0.3) * m, "#2a040c", 0.95)
    f.fill(f.m_ellipse(cx - 0.17, cy - 0.17, 0.12, 0.1), "#ffffff", 0.9)
    f.fill(f.m_polyline([(cx - 1.12, cy), (cx - 0.5, cy - 0.55), (cx + 0.5, cy - 0.55), (cx + 1.12, cy)], 0.18), "#2a040c", 0.9)
    f.glow_fill(f.m_ellipse(cx, cy, 1.3, 0.85, soft=3.0), gcol, 0.5 if not FORM2 else 0.85)


def tattoo(f):
    """The red tattoo that frames the stone and the third eye: two hooked lines that run from the stone round the eye and out toward the temples, and a short tick under the eye."""
    col = "#ff3a2a" if FORM2 else "#a81020"
    g = EMBER_G if FORM2 else None
    for s in (-1.0, 1.0):
        pts = [(4.0 + s * 0.85, 1.7), (4.0 + s * 1.7, 2.05), (4.0 + s * 2.2, 2.9), (4.0 + s * 1.95, 3.7), (4.0 + s * 1.4, 4.0)]
        f.fill(f.m_polyline(pts, 0.26), col, 0.95, glow=g)
        f.fill(f.m_polyline([(4.0 + s * 2.2, 2.9), (4.0 + s * 2.8, 2.6)], 0.2), col, 0.9, glow=g)
    f.fill(f.m_polyline([(4.0, 3.95), (4.0, 4.3)], 0.16), col, 0.8, glow=g)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    skin, crease = skin_tones()
    K.face_base(f, skin, side=0.08, chin=0.09)
    blush = "#ff8a8a" if FORM2 else "#f08a94"
    f.fill(f.m_ellipse(1.15, 6.55, 1.0, 0.55, soft=2.5), blush, 0.4)                              # the blush
    f.fill(f.m_ellipse(6.85, 6.55, 1.0, 0.55, soft=2.5), blush, 0.4)
    tattoo(f)
    stone = ("#ff4a6a", "#8a1a3a", "#ff7a8a") if FORM2 else ("#9a4ae0", "#5a2a9a", "#c58cff")
    H.gem(f, 4.0, 1.5, 0.52, stone[0], setting="#6a3a30", facets=6, spec=0.9, glow=stone[2], ry=0.6)
    third_eye(f, 4.0, 3.2)
    brow_c = "#d8607a" if FORM2 else "#b04a78"
    H.brow(f, (3.0, 4.3), (0.65, 3.45), brow_c, 0.36, 0.14, arch=0.08)
    H.brow(f, (5.0, 4.3), (7.35, 3.45), brow_c, 0.36, 0.14, arch=0.08)
    for cx in (2.15, 5.85):
        if FORM2:
            H.eye(f, cx, 5.55, 2.7, 2.4, IRIS2, lash="#3a0a10", tilt=0.3, lash_w=0.46, flick=0.46, glow=EMBER_G, hi=0.85, pupil="#2a0608", lid_shadow=0.38)
            f.fill(f.m_ellipse(cx, 5.58, 0.1, 0.75), "#1a0406", 0.95)
        else:
            H.eye(f, cx, 5.55, 2.7, 2.45, IRIS, lash="#4a1a34", tilt=0.22, lash_w=0.46, flick=0.4, hi=0.95, pupil="#0c2a24", lid_shadow=0.36)
    H.nose(f, 4.0, 6.35, crease, 0.2)
    if FORM2:
        H.mouth_smile(f, 4.0, 7.0, 4.0, color="#6a1018", lip="#e0505a", depth=0.34, thick=0.24, teeth="#fffaf2", open_h=0.75)       # the mad grin
        for x in (2.85, 5.15):
            f.poly([(x - 0.16, 7.0), (x + 0.16, 7.0), (x, 7.55)], "#ffffff", 0.97)
        K.jag(f, [(0.7, 6.0), (1.1, 6.7), (0.8, 7.3)], EMBER, 0.14, 0.95, glow=EMBER_G, core="#fff0c8", dark="#4a0a10")
        K.jag(f, [(7.3, 6.0), (6.9, 6.7), (7.2, 7.3)], EMBER, 0.14, 0.95, glow=EMBER_G, core="#fff0c8", dark="#4a0a10")
        K.jag(f, [(2.9, 3.9), (3.3, 4.1)], EMBER, 0.12, 0.8, glow=EMBER_G)
    else:
        H.mouth_smile(f, 4.0, 7.35, 2.0, color="#9a3a50", lip="#e08090", depth=-0.2, thick=0.2)                                   # a clenched frown
        f.fill(f.m_line((3.05, 7.45), (2.8, 7.25), 0.12, 0.1), "#9a3a50", 0.6)
        f.fill(f.m_line((4.95, 7.45), (5.2, 7.25), 0.12, 0.1), "#9a3a50", 0.6)


def head_rest(cv, img64, spec):
    skin, _ = skin_tones()
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, skin)


# ================================================================================================ the dress
def fleece(f, mask, base, seed, a=0.5, step=0.4, y0=0.0, y1=None):
    """The furry nap of the dress: short, slanted light and dark strokes on a loose grid (deterministic)."""
    y1 = f.h if y1 is None else y1
    lt, dk = K.lit(base, 1.4), K.deep(base, 0.7)
    for i in range(int(f.w / step) + 1):
        for j in range(int((y1 - y0) / step) + 1):
            r = H.hsh(seed, i, j)
            if r < 0.45:
                continue
            x = step * (i + 0.5 + 0.5 * (H.hsh(seed, i, j, 1) - 0.5))
            y = y0 + step * (j + 0.5 + 0.5 * (H.hsh(seed, i, j, 2) - 0.5))
            lean = (H.hsh(seed, i, j, 3) - 0.5) * 0.26
            f.fill(f.m_line((x, y), (x + lean, y + 0.32), 0.2, 0.1) * mask, lt if r > 0.8 else dk, a)


def collar(f, name, seed):
    """The high cream fur collar: a fluffy band over the neck with a tufted lower edge and a soft shadow under it."""
    K.fur_band(f, 0.0, 1.7, FUR, FUR_L, FUR_D, tuft=0.55, seed=seed)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.16), "#ffffff", 0.5)
    H.shadow_under(f, 2.1, 0.45, 0.3)


def eye_emblem(f):
    """The emblem of the Eye of the Midnight Sun on the chest: a pale almond with a dark rim, a red iris and a pupil (soft glow on the glow map)."""
    cx, cy = 4.0, 5.45
    rim = "#4a0e1a"
    col = "#ffd890" if FORM2 else "#e8c8c0"
    g = EMBER_G if FORM2 else "#ff9a7a"
    f.fill(lens(f, cx, cy, 2.55, 1.28, 1.2), rim, 0.95)
    m = lens(f, cx, cy, 2.3, 1.08, 1.0)
    f.fill(m, H.lin((0, cy - 1.1), (0, cy + 1.0), [(0, H.tone(col, 0.78)), (0.4, col), (1, K.lit(col, 1.1))]), 1.0)
    iris = "#ff6a2a" if FORM2 else "#b02838"
    f.fill(f.m_ellipse(cx, cy + 0.02, 0.92, 0.98) * m, H.lin((0, cy - 1.0), (0, cy + 1.0), [(0, H.tone(iris, 0.5)), (0.5, iris), (1, K.lit(iris, 1.3))]), 1.0, glow=g)
    f.fill(f.m_ellipse(cx, cy + 0.04, 0.38, 0.5) * m, "#240408", 0.95)
    f.fill(f.m_ellipse(cx - 0.3, cy - 0.32, 0.2, 0.17), "#ffffff", 0.85)
    f.fill(f.m_polyline([(cx - 2.3, cy), (cx - 1.2, cy - 0.96), (cx, cy - 1.1), (cx + 1.2, cy - 0.96), (cx + 2.3, cy)], 0.18), rim, 0.8)
    for s in (-1.0, 1.0):                                                                               # a short flourish at each corner
        f.fill(f.m_polyline([(cx + s * 2.5, cy), (cx + s * 3.1, cy - 0.35)], 0.16), rim, 0.8)
    f.glow_fill(f.m_ellipse(cx, cy, 2.2, 1.2, soft=3.0), g, 0.3 if not FORM2 else 0.55)


def belt(f, front=False):
    K.belt_band(f, 8.85, 10.0, BELT, stitch="#a88860" if not FORM2 else "#ffc080")
    if front:
        K.buckle(f, 4.0, 9.42, 1.7, 1.0, GOLD, r=0.22)
        if FORM2:
            f.glow_fill(f.m_ellipse(4.0, 9.42, 0.7, 0.4, soft=3.0), EMBER_G, 0.5)
        for x in (1.1, 6.9):
            H.stud(f, x, 9.42, 0.18, GOLD_D, spec=0.5)
    else:
        for x in (1.1, 4.0, 6.9):
            H.stud(f, x, 9.42, 0.16, GOLD_D, spec=0.4)


def hem(f, y0, y1, nm=None):
    """The dark hem of the dress: a band with a rose (ember in the second form) zigzag along its edge."""
    c = hem_c()
    K.band(f, y0, y1, c, spec=0.1)
    t = trim()
    zig = []
    n = int(round(f.w / 0.8))
    for i in range(n + 1):
        zig.append((f.w * i / max(n, 1), y0 + 0.4 + (0.42 if i % 2 == 0 else 0.0)))
    if FORM2:
        K.glow_line(f, zig, t, 0.18, 0.95, glow=EMBER_G, core="#fff0c0", core_a=0.45)
    else:
        f.fill(f.m_polyline(zig, 0.2), t, 0.75)
    H.shadow_under(f, y1, 0.45, 0.3)


def body_front(f):
    c = dress()
    K.cloth(f, f.m_all(), c, top=1.4, bottom=0.8, round_=0.16, fold_xs=[1.4, 6.6], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    fleece(f, f.m_rect(0.0, 1.9, f.w, 8.8), c, 11, a=0.5, y0=1.9, y1=8.8)
    for x0, x1 in ((0.0, 2.9), (5.1, 8.0)):
        K.sheen(f, f.m_rect(x0, 2.0, x1, 8.8), [x0 + 0.8 if x0 < 4 else x1 - 0.8], 2.3, 8.5, 0.24, "#ff9aa8", width=0.3)
    f.fill(f.m_rect(0.0, 2.0, 0.3, 8.9), K.lit(c, 1.6), 0.35)
    f.fill(f.m_rect(7.7, 2.0, 8.0, 8.9), "#000000", 0.3)
    for x0, x1 in ((2.0, 1.6), (6.0, 6.4)):                                                             # the princess seams from the collar to the belt
        K.seam(f, [(x0, 2.3), (x1, 8.6)], trim() if FORM2 else ROSE, 0.06, 0.5)
    eye_emblem(f)
    collar(f, "front", 3)
    belt(f, True)
    hem(f, 11.0, 12.0)


def body_back(f):
    c = dress()
    K.cloth(f, f.m_all(), c, top=1.35, bottom=0.8, round_=0.16, fold_xs=[1.6, 6.4], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    fleece(f, f.m_rect(0.0, 1.9, f.w, 8.8), c, 12, a=0.5, y0=1.9, y1=8.8)
    K.sheen(f, f.m_rect(0, 2, 8, 8.8), [1.7, 6.2], 2.3, 8.4, 0.2, "#ff9aa8", width=0.3)
    K.seam(f, [(4.0, 2.4), (4.0, 8.7)], trim() if FORM2 else ROSE, 0.06, 0.5)
    collar(f, "back", 4)
    belt(f)
    hem(f, 11.0, 12.0)


def body_side(f, name):
    c = dress()
    K.cloth(f, f.m_all(), c, top=1.35, bottom=0.8, round_=0.16, fold_xs=[1.4], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    fleece(f, f.m_rect(0.0, 1.9, f.w, 8.8), c, 13 if name == "west" else 14, a=0.5, y0=1.9, y1=8.8)
    collar(f, name, 5 if name == "west" else 6)
    belt(f)
    hem(f, 11.0, 12.0)


def body(cv):
    for nm in SIDES:
        for f in K.pair(cv, "body", nm):
            if nm == "front":
                body_front(f)
            elif nm == "back":
                body_back(f)
            else:
                body_side(f, nm)


# ================================================================================================ the arms and legs
def arm(cv, part):
    c = dress()
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            K.cloth(f, f.m_all(), c, top=1.45, bottom=0.8, round_=0.2, fold_xs=[f.w * 0.5], fold_depth=0.07, fold_y0=1.0, fold_y1=10.0)
            fleece(f, f.m_rect(0.0, 0.9, f.w, 10.2), c, 21, a=0.5, y0=0.9, y1=10.2)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.4), K.lit(c, 1.7), 0.5)
            K.sheen(f, f.m_band(0.5, 10.0), [f.w * 0.3], 1.5, 9.5, 0.26, "#ff9aa8", width=0.3)
            K.seam(f, [(0.1, 1.15), (f.w - 0.1, 1.15)], trim() if FORM2 else ROSE, 0.06, 0.5)             # the shoulder seam
            f.fill(f.m_polyline([(0.0, 5.3), (f.w * 0.5, 5.5), (f.w, 5.35)], 0.16), K.deep(c, 0.5), 0.5)    # elbow crease
            # the fur cuff: a fluffy band over the wrist (the hand is inside the sleeve)
            f.fill(f.m_rect(0.0, 10.2, f.w, 12.0), H.lin((0, 10.2), (0, 12.0), [(0, FUR), (1, FUR_D)]))
            K.fur_band(f, 10.15, 11.45, FUR, FUR_L, FUR_D, tuft=0.55, seed=31 + len(nm))
            f.fill(f.m_rect(0.0, 10.1, f.w, 10.3), K.deep(c, 0.4), 0.7)
            H.shadow_under(f, 10.15, 0.4, 0.3)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", dress(), edge=K.deep(dress(), 0.6), k_top=1.5, overlay=ov)
            K.cap(cv, part, "bottom", FUR, edge=FUR_D, k_top=1.0, overlay=ov)


def legs(cv):
    c = dress()
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.cloth(f, f.m_band(0.0, 6.0), c, top=1.35, bottom=0.82, round_=0.14, fold_xs=[2.0], fold_depth=0.07, fold_y0=0.2, fold_y1=5.6)
                fleece(f, f.m_rect(0.0, 0.0, f.w, 5.6), c, 41 + len(nm), a=0.5, y0=0.0, y1=5.6)
                hem(f, 5.6, 7.0)
                # the knee boots: brown, a rose turned cuff, a polished toe, a light seam up the front
                K.boot(f, BOOT, 7.0, nm, strap=True, strap_color="#33201a", gold=GOLD, cuff_color=ROSE if not FORM2 else "#e0a070", cuff_h=0.95, welt="#3a2218", sole="#100c14", toe=True)
                if nm == "front":
                    K.seam(f, [(f.w * 0.5, 8.2), (f.w * 0.5, 10.6)], "#9a7a62", 0.05, 0.5)
                    for y in (8.7, 9.7):
                        f.fill(f.m_rect(f.w * 0.5 - 0.5, y, f.w * 0.5 + 0.5, y + 0.14), K.deep(BOOT, 0.45), 0.6)


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
