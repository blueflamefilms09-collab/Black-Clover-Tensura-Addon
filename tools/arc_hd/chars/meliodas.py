"""Meliodas, the Dragon's Sin of Wrath, HD 256x256: the hand-painted pilot of the HD arc pass (docs/arc/hd_arc.md), reworked by group sds1 (docs/arc/hd_arc_sds1.md).
`meliodas_form2.py` (the Demon King) loads this module and swaps its palette and its face (FORM2 = True); all the drawing code below is shared.

The outfit is the one of the 64x64 sheet (tools/arc/char_meliodas.py: a dark vest with a brown yoke and a stand collar over a cream shirt, short cream sleeves with brown cuffs, a wrapped right hand,
a double belt with a gold buckle, dark trousers tucked into calf boots); nothing is changed in WHAT he wears, only how finely it is drawn. The colour blocking and the layout come from the 64x64
generator through the HD bridge (A.blueprint / A.bridge); then the things that must be crisp are drawn again at 4x detail on BOTH layers (the game drops the second layer and the hair beyond 24 blocks):
the anime face with the emerald eyes, the thick blond brows, the fringe, the wide grin with one fang; the vest with its brown piping and stitching, the shirt with its placket, collar and buttons; the
double belt with the gold buckle; the sleeves with their stitched cuffs; the wrapped right hand; the Dragon's Sin mark on the left forearm (the sources disagree on the exact place: here the forearm below
the sleeve, so it is seen); the trousers with their creases, the boots with their straps, welt and polished toe.

Original art from memory of the character's look (docs/arc/meliodas.md lists the sources), nothing traced. Glow: none in this form (the glow of the sword lives on the weapon model). The Demon King form
(`meliodas_form2.py`) runs this same code with FORM2 = True: dark palette, slit red glowing eyes, the black lock, the sigil, fangs, and glowing cracks and veins on the left arm (the glow map exists only there).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import arc_hdlib as A  # noqa: E402
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds1_kit as K  # noqa: E402

H = A.H

FORM2 = False                                                                       # meliodas_form2.py sets this and the palette below before it calls paint()
HAIR_STYLE = "anime_spike_tall"                                                     # thick layered clumps (reference 154); the pilot's anime_spike_swept read as thin fingers
HAIR = "#f6d044"                                                                    # a golden blond: the sheet's #e8c860 reads khaki once the hair tint pipeline is applied
HAIR_RGB = (246, 208, 68)
SKIN = ("#e5b393", "#f4cfb6", "#fee6d3")
VEST, YOKE, PIPE = "#322a29", "#94713c", "#e0c27a"
SHIRT = "#e4dac2"
TROUS = "#2e2a36"
BOOT = "#43332c"
BELT_A, BELT_B = "#573a24", "#2c1f19"
GOLD = "#e0b43e"
WRAP = "#e6dec9"
INK, INK_L = "#5c1519", "#9a2e2a"                                                    # the Dragon's Sin mark
SIDES = ("front", "back", "west", "east")


# ================================================================================================ small tools
def cover_of(cv, part, name):
    """The second-layer coverage of the blueprint (what the 64x64 sheet draws on the second layer), kept so the HD redraw stays inside it."""
    return (cv.face(part, name, True).m_alpha() > 0.5).astype(np.float32)


def hold_cover(cv, covers):
    """Clips every second-layer island to its blueprint coverage after the drawing (the overlay may only be where the sheet puts cloth)."""
    for (part, name), m in covers.items():
        f = cv.face(part, name, True)
        f.fill(1.0 - m, "#000000", mode="erase")
        f.glow[..., 3] *= m


def seam(f, pts, color="#c9a867", width=0.07, a=0.8, dash=(0.34, 0.2), shadow=0.28):
    """A stitched line. At 256 texels one texel is a quarter of a model pixel: a line thinner than that breaks into dots, so the width and the dashes are kept at texel scale."""
    d = None if dash is None else (max(dash[0], 0.5), max(dash[1], 0.36))
    H.stitch(f, pts, color, width=max(width, 0.2), dash=d, shadow=shadow, a=a)


# ================================================================================================ the head
HAIR_D, HAIR_M, HAIR_L, HAIR_H = "#a8741a", "#eebb2a", "#ffdf58", "#fff3ac"


def lock(f, cx, w, top, length, lean, shade=1.0):
    """One tapered lock of hair hanging from y=top: a teardrop that leans, a dark edge on its shadow side, a light streak down its middle."""
    n = 9
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = w / 2.0 * (1.0 - t ** 1.6)
        x = cx + lean * t * t
        y = top + length * t
        left.append((x - half, y))
        right.append((x + half, y))
    poly = left + right[::-1]
    m = f.m_poly(poly)
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(HAIR_M, HAIR_D, 0.25 * (1 - shade))), (0.55, H.mix(HAIR_L, HAIR_M, 0.2 * (1 - shade))), (1, H.mix(HAIR_M, HAIR_D, 0.3 + 0.3 * (1 - shade)))]))
    f.fill(f.m_polyline(left[2:], 0.07) * m, HAIR_D, 0.8)
    f.fill(f.m_polyline(right[2:], 0.07) * m, H.mix(HAIR_D, "#7a4c10", 0.5), 0.85)
    mid = [(cx + lean * (i / n) ** 2 * 0.9, top + length * 0.12 + length * 0.7 * (i / n)) for i in range(n)]
    f.fill(f.m_polyline(mid, 0.12) * m, HAIR_H, 0.55 * shade)
    return m


def head_front_human(cv):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=None, side=0.09, chin=0.07, shadow=0.0)
    # the hair of the fringe: a messy blond mass; locks hang over the brow, a sideburn each side
    mass = f.m_poly([(-0.3, -0.3), (8.3, -0.3), (8.3, 2.1), (7.3, 1.75), (6.2, 2.2), (5.0, 1.8), (3.9, 2.25), (2.7, 1.8), (1.4, 2.2), (-0.3, 1.9)])
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - 1.9) / 2.4, 0, 1) * (f.Y > 1.6), 0.86)                    # the soft shadow of the fringe on the forehead
    f.fill(mass, H.lin((0, 0), (0, 2.2), [(0, H.mix(HAIR_D, HAIR_M, 0.45)), (0.5, HAIR_M), (1, H.tone(HAIR_M, 0.9))]))
    for x in (0.9, 2.1, 3.3, 4.5, 5.7, 6.9):
        f.fill(f.m_line((x, 0.0), (x + 0.12, 1.9), 0.10, 0.05) * mass, HAIR_H, 0.35)
    for sh, (cx, w, ln, ln_, lean) in ((0.8, (6.9, 1.5, 0.9, 2.5, -0.35)), (0.8, (1.1, 1.5, 0.9, 2.4, 0.35)), (0.9, (4.55, 1.5, 1.2, 1.9, 0.3)), (0.9, (2.95, 1.35, 1.2, 1.5, -0.1)),
                                       (1.0, (5.9, 1.3, 1.1, 1.5, -0.3)), (1.0, (1.9, 1.5, 1.0, 2.1, 0.35))):
        lock(f, cx, w, ln, ln_, lean, sh)
    lock(f, 0.42, 1.0, 0.8, 3.9, 0.15, 1.0)                                                                   # the sideburns
    lock(f, 7.58, 1.0, 0.8, 3.9, -0.15, 1.0)
    # the brows: thick, blond, set low and slanted: a confident look
    brow = "#b8800f"
    H.brow(f, (3.55, 3.95), (1.15, 3.5), brow, 0.36, 0.14, arch=0.10)
    H.brow(f, (4.45, 3.95), (6.85, 3.5), brow, 0.36, 0.14, arch=0.10)
    # the eyes: emerald anime eyes, the outer corner raised
    iris = ("#0b5a3c", "#27b072", "#a6f0c4")
    H.eye(f, 2.4, 5.05, 2.4, 2.0, iris, lash="#2a1812", tilt=0.2, lash_w=0.42, flick=0.42)
    H.eye(f, 5.6, 5.05, 2.4, 2.0, iris, lash="#2a1812", tilt=0.2, lash_w=0.42, flick=0.42)
    H.blush(f, 1.2, 6.2, 0.95, 0.5, "#ee8a86", 0.4)
    H.blush(f, 6.8, 6.2, 0.95, 0.5, "#ee8a86", 0.4)
    H.nose(f, 4.0, 6.05, "#cf9680", 0.26)
    # the grin: wide and open, the upper teeth showing, one fang
    H.mouth_smile(f, 4.0, 6.6, 4.0, color="#6a2428", lip="#d98a80", depth=0.34, thick=0.2, teeth="#fffdf6", open_h=0.72)
    f.poly([(5.15, 6.92), (5.6, 6.92), (5.38, 7.5)], "#ffffff", 0.95)


# ================================================================================================ form 2: the Demon King (assault mode): slit red eyes, angry brows, the sigil, cracks and veins that glow
DEMON, DEMON_L, CRIMSON = "#1a0a14", "#4a1030", "#ff3a5a"
BLACK_LOCK = {"d": "#07050a", "m": "#15121a", "l": "#2c2434", "h": "#54486a"}


def crack(f, pts, width=0.17, a=0.95):
    """A jagged crack of light: a dark crimson edge, a bright core, a glow-map twin."""
    m = f.m_polyline(pts, width + 0.12)
    f.fill(m, DEMON, 0.85)
    K.glow_line(f, pts, CRIMSON, width, a, core="#ffd0d8", core_a=0.6)


def sigil(f, cx, cy, s=1.0, glow=True):
    """The Demon mark on the forehead (drawn from memory, stylised): a pointed diamond with a hooked cross-line and a short tail, dark with a crimson edge that glows."""
    d = [(cx, cy - 1.0 * s), (cx + 0.55 * s, cy - 0.1 * s), (cx, cy + 1.05 * s), (cx - 0.55 * s, cy - 0.1 * s)]
    f.poly(d, DEMON, 0.95)
    f.fill(f.m_polyline(d + [d[0]], 0.1 * s), CRIMSON, 0.9, glow=glow)
    f.fill(f.m_line((cx - 1.0 * s, cy + 0.15 * s), (cx + 1.0 * s, cy + 0.15 * s), 0.12 * s, 0.12 * s), DEMON, 0.95)
    f.fill(f.m_line((cx - 1.0 * s, cy + 0.15 * s), (cx + 1.0 * s, cy + 0.15 * s), 0.07 * s, 0.07 * s), CRIMSON, 0.85, glow=glow)
    f.poly([(cx - 0.12 * s, cy + 0.15 * s), (cx + 0.12 * s, cy + 0.15 * s), (cx, cy + 1.0 * s)], CRIMSON, 0.85, glow=glow)


def head_front_demon(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    skin = ("#dcae92", "#efc8b0", "#fbe0cc")
    H.skin_face(f, skin, hair=None, side=0.1, chin=0.09, shadow=0.0)
    mass = f.m_poly([(-0.3, -0.3), (8.3, -0.3), (8.3, 1.9), (7.2, 1.6), (6.0, 2.0), (4.9, 1.5), (3.8, 1.9), (2.6, 1.5), (1.4, 1.9), (-0.3, 1.7)])
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - 1.6) / 2.0, 0, 1) * (f.Y > 1.3), 0.84)
    f.fill(mass, H.lin((0, 0), (0, 2.0), [(0, H.mix(HAIR_D, HAIR_M, 0.45)), (0.5, HAIR_M), (1, H.tone(HAIR_M, 0.9))]))
    for x in (0.9, 2.1, 3.3, 4.5, 5.7, 6.9):
        f.fill(f.m_line((x, 0.0), (x + 0.12, 1.7), 0.10, 0.05) * mass, HAIR_H, 0.35)
    # the fringe is swept up and apart to show the sigil; one black lock hangs on the left of the picture
    for sh, (cx, w, ln, ln_, lean) in ((0.8, (7.0, 1.5, 0.9, 2.6, -0.4)), (0.8, (1.0, 1.5, 0.9, 2.6, 0.4)), (0.9, (5.7, 1.3, 1.0, 1.1, -0.3)), (0.9, (2.4, 1.3, 1.0, 1.2, 0.3))):
        lock(f, cx, w, ln, ln_, lean, sh)
    lock_pal = BLACK_LOCK
    K.lock(f, lock_pal, 2.9, 1.2, 0.5, 2.1, -0.2, 1.0)
    lock(f, 0.42, 1.0, 0.8, 4.0, 0.15, 1.0)
    lock(f, 7.58, 1.0, 0.8, 4.0, -0.15, 1.0)
    sigil(f, 4.7, 2.75, 0.85)
    # angry brows (inner ends low), slit crimson eyes, black rims, a hard open grin with two fangs
    brow = "#8a5a10"
    H.brow(f, (3.3, 4.2), (0.95, 3.15), brow, 0.42, 0.14, arch=0.0)
    H.brow(f, (4.7, 4.2), (7.05, 3.15), brow, 0.42, 0.14, arch=0.0)
    iris = ("#5a0614", "#ff3a5a", "#ffb4be")
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.1, 2.5, 1.9, iris, lash="#10060c", tilt=0.35, lash_w=0.5, flick=0.45, lid_shadow=0.35, glow="#ff3a5a", hi=0.7, pupil="#ff3a5a")
        f.fill(f.m_ellipse(cx, 5.15, 0.11, 0.78), "#1a0408", 0.95)                                     # the slit pupil
        f.fill(f.m_polyline([(cx - 1.35, 5.95 + (0.0 if cx < 4 else 0.0)), (cx, 6.35), (cx + 1.35, 5.95)], 0.12), DEMON, 0.6)   # the dark rim under the eye
    H.nose(f, 4.0, 6.1, "#c08a76", 0.24)
    H.mouth_smile(f, 4.0, 6.55, 4.0, color="#5a1018", lip="#c8706c", depth=0.3, thick=0.2, teeth="#fffdf6", open_h=0.8)
    for x in (2.9, 5.1):
        f.poly([(x - 0.22, 6.82), (x + 0.22, 6.82), (x, 7.55)], "#ffffff", 0.95)
    # cracks from the outer eye corners down the cheeks and the veins of the temples
    for sgn in (1.0, -1.0):
        x0 = 4.0 + sgn * 3.3
        crack(f, [(x0, 5.0), (x0 - sgn * 0.4, 5.6), (x0 - sgn * 0.15, 6.2), (x0 - sgn * 0.7, 6.9), (x0 - sgn * 0.55, 7.7)])
        crack(f, [(x0 - sgn * 0.3, 5.5), (x0 - sgn * 0.95, 5.85)], 0.12, 0.8)
        crack(f, [(4.0 + sgn * 1.6, 2.7), (4.0 + sgn * 2.2, 3.05), (4.0 + sgn * 2.7, 2.85)], 0.11, 0.7)


def head_front(cv):
    (head_front_demon if FORM2 else head_front_human)(cv)


def demon_marks(cv):
    """Form 2: the Demon mark has spread over the arms: crimson veins under the wrap and cracks along the left forearm, glowing."""
    for nm in SIDES:
        for f in A.lay(cv, "arm_l", nm):
            crack(f, [(f.w * 0.3, 8.9), (f.w * 0.55, 9.6), (f.w * 0.4, 10.4), (f.w * 0.65, 11.2)], 0.14, 0.85)
            crack(f, [(f.w * 0.7, 4.6), (f.w * 0.5, 5.2), (f.w * 0.62, 6.0)], 0.12, 0.75)


# ================================================================================================ the body
def rim_light(f, mask, x0, x1, k=1.55, a=0.22):
    """A thin lit edge on the light side of a panel (the vest is a smooth dark cloth: without it the panels read as flat black)."""
    f.fill(mask * f.m_rect(x0, f.Y.min(), x1, f.Y.max() + 1.0), H.tone(VEST, k), a)


def vest_front(f):
    K.cloth(f, f.m_all(), VEST, top=1.5, bottom=0.80, round_=0.08, fold_xs=[0.8, 7.2], fold_depth=0.10, fold_y0=1.8, fold_y1=9.0, weave_k=0.0)
    for x0, x1 in ((0.0, 2.3), (5.7, 8.0)):                                              # the two vest panels: a sheen of the cloth, a lit edge, a princess seam
        pm = f.m_rect(x0, 1.5, x1, 9.0)
        H.sheen(f, pm, [x0 + 0.55 if x0 < 4 else x0 + 0.5], width=0.2, strength=0.22, tilt=0.05, y0=2.0, y1=8.6, color="#a99ea0")
        rim_light(f, pm, x0, x0 + 0.22, 1.7, 0.35)
    seam(f, [(1.05, 2.2), (1.0, 8.7)], "#9c8a6a", 0.065, 0.7)
    seam(f, [(6.95, 2.2), (7.0, 8.7)], "#9c8a6a", 0.065, 0.7)
    # the brown yoke across the chest
    yoke = f.m_poly([(0, 0), (8, 0), (8, 1.55), (5.65, 1.55), (5.4, 0.8), (2.6, 0.8), (2.35, 1.55), (0, 1.55)])
    f.fill(yoke, H.lin((0, 0), (0, 1.55), [(0, H.tone(YOKE, 1.18)), (0.6, YOKE), (1, H.tone(YOKE, 0.68))]))
    H.bevel(f, yoke, depth=0.24, gain=1.05, spec=0.3)
    f.fill(f.m_polyline([(0, 1.58), (2.35, 1.58), (2.6, 0.82)], 0.10), H.tone(YOKE, 0.38), 0.9)
    f.fill(f.m_polyline([(8, 1.58), (5.65, 1.58), (5.4, 0.82)], 0.10), H.tone(YOKE, 0.38), 0.9)
    seam(f, [(0.2, 1.15), (2.2, 1.15)], PIPE, 0.065, 0.85)
    seam(f, [(7.8, 1.15), (5.8, 1.15)], PIPE, 0.065, 0.85)
    for x in (0.55, 7.45):
        H.stud(f, x, 0.62, 0.17, "#d8b660", spec=0.7)
    H.shadow_under(f, 1.6, 0.45, 0.26, x0=0.0, x1=2.4)
    H.shadow_under(f, 1.6, 0.45, 0.26, x0=5.6, x1=8.0)
    # the cream shirt in the opening of the vest
    placket = f.m_poly([(2.55, 0.0), (5.45, 0.0), (6.05, 9.05), (1.95, 9.05)])
    K.cloth(f, placket, SHIRT, top=1.1, bottom=0.9, round_=0.05, fold_xs=[3.05, 5.0], fold_depth=0.08, fold_y0=2.2, fold_y1=8.8, weave_k=0.0)
    f.shade(placket * np.clip(1.0 - np.minimum(f.X - (2.55 - (f.Y / 9.05) * 0.6), (5.45 + (f.Y / 9.05) * 0.6) - f.X) / 0.6, 0, 1), 0.78, 0.9)   # the vest shades the shirt at its edge
    f.fill(f.m_line((4.0, 1.7), (4.0, 9.0), 0.17, 0.17) * placket, H.tone(SHIRT, 0.55), 0.6)
    f.fill(f.m_line((4.17, 1.7), (4.17, 9.0), 0.12, 0.12) * placket, H.tone(SHIRT, 1.25), 0.55)
    for y in (2.7, 4.7, 6.7):
        H.stud(f, 4.0, y, 0.25, "#cdb67e", spec=0.9)
    for sgn in (-1.0, 1.0):                                                              # the two collar flaps of the shirt at the neck
        fl = f.m_poly([(4.0 + sgn * 0.1, 0.0), (4.0 + sgn * 1.45, 0.0), (4.0 + sgn * 1.1, 1.0), (4.0 + sgn * 0.12, 1.75)])
        f.fill(fl, H.tone(SHIRT, 1.12))
        f.fill(f.m_polyline([(4.0 + sgn * 0.12, 1.75), (4.0 + sgn * 1.1, 1.0), (4.0 + sgn * 1.45, 0.0)], 0.08), H.tone(SHIRT, 0.5), 0.8)
    # the brown piping of the vest opening and a stitch beside it
    for x0, x1, s in ((2.5, 1.9, -1.0), (5.5, 6.1, 1.0)):
        H.trim_line(f, [(x0, 1.2), (x1, 9.0)], YOKE, width=0.30, spec=0.4, outline=0.6)
        seam(f, [(x0 + s * 0.42, 1.7), (x1 + s * 0.46, 9.0)], PIPE, 0.065, 0.8)


def belts(f, front=False):
    """Two straps y 9..11: the wide brown one (with the gold buckle on the front) over a darker one."""
    a = f.m_rect(0.0, 9.0, f.w, 10.05)
    f.fill(a, H.lin((0, 9.0), (0, 10.05), [(0, H.tone(BELT_A, 1.4)), (0.4, BELT_A), (1, H.tone(BELT_A, 0.7))]))
    f.fill(f.m_rect(0.0, 9.03, f.w, 9.2), "#b08a58", 0.5)
    seam(f, [(0.0, 9.3), (f.w, 9.3)], "#d4ac68", 0.055, 0.8, (0.3, 0.18), 0.2)
    seam(f, [(0.0, 9.8), (f.w, 9.8)], "#d4ac68", 0.055, 0.65, (0.3, 0.18), 0.2)
    b = f.m_rect(0.0, 10.05, f.w, 11.0)
    f.fill(b, H.lin((0, 10.05), (0, 11.0), [(0, H.tone(BELT_B, 1.5)), (0.3, BELT_B), (1, H.tone(BELT_B, 0.7))]))
    f.fill(f.m_rect(0.0, 10.02, f.w, 10.14), "#0e0a08", 0.8)
    H.shadow_under(f, 11.0, 0.5, 0.3)
    if front:
        for x in (1.0, 7.0):
            H.stud(f, x, 9.55, 0.2, GOLD)
        f.rect(2.65, 8.72, 5.35, 10.38, "#3a2a10", r=0.26)                               # the buckle: a gold frame with a dark opening and a prong
        f.rect(2.72, 8.79, 5.28, 10.31, H.metal_paint(8.79, 10.31, GOLD, 1.15), r=0.22)
        H.bevel(f, f.m_rect(2.72, 8.79, 5.28, 10.31, 0.22), depth=0.24, gain=1.3, spec=0.7)
        f.rect(3.2, 9.2, 4.8, 9.9, "#1d130c", r=0.12)
        f.line((2.95, 9.55), (4.2, 9.55), H.metal_paint(9.3, 9.8, GOLD, 1.1), 0.2, 0.14)
        f.ellipse(3.1, 9.0, 0.26, 0.15, "#fff6c8", a=0.8)


def body_front(f, ov):
    if not ov:
        vest_front(f)
        f.fill(f.m_poly([(2.2, 10.9), (5.8, 10.9), (5.8, 12.0), (2.2, 12.0)]), H.lin((0, 10.9), (0, 12), [(0, H.tone(SHIRT, 0.7)), (1, H.tone(SHIRT, 0.98))]))     # the shirt tail
        f.fill(f.m_rect(0.0, 11.0, 2.2, 12.0), H.tone(VEST, 0.95))
        f.fill(f.m_rect(5.8, 11.0, 8.0, 12.0), H.tone(VEST, 0.95))
        f.fill(f.m_line((2.2, 11.0), (2.2, 12.0), 0.1, 0.1), H.tone(VEST, 0.45), 0.8)
        f.fill(f.m_line((5.8, 11.0), (5.8, 12.0), 0.1, 0.1), H.tone(VEST, 0.45), 0.8)
        H.shadow_under(f, 11.0, 0.5, 0.22)
    belts(f, True)


def body_back(f, ov):
    if not ov:
        K.cloth(f, f.m_all(), VEST, top=1.25, bottom=0.8, round_=0.08, fold_xs=[1.6, 3.0, 5.0, 6.4], fold_depth=0.09, fold_y0=1.8, fold_y1=9.0, weave_k=0.0)
        H.sheen(f, f.m_rect(0, 1.5, 8, 9.0), [1.6, 6.3], width=0.22, strength=0.18, tilt=0.04, y0=2.0, y1=8.6, color="#a99ea0")
        yoke = f.m_rect(0, 0, 8, 1.55)
        f.fill(yoke, H.lin((0, 0), (0, 1.55), [(0, H.tone(YOKE, 1.12)), (0.6, YOKE), (1, H.tone(YOKE, 0.68))]))
        H.bevel(f, yoke, depth=0.24, gain=1.0, spec=0.25)
        f.fill(f.m_rect(0, 1.52, 8, 1.66), H.tone(YOKE, 0.38), 0.9)
        seam(f, [(0.2, 1.15), (7.8, 1.15)], PIPE, 0.065, 0.8)
        H.shadow_under(f, 1.62, 0.45, 0.26)
        seam(f, [(4.0, 1.9), (4.0, 8.9)], "#a89678", 0.075, 0.75)                           # the centre seam
        f.fill(f.m_line((4.0, 1.9), (4.0, 8.9), 0.17, 0.17), H.tone(VEST, 0.4), 0.55)
        f.fill(f.m_rect(0.0, 11.0, 8.0, 12.0), H.tone(VEST, 0.9))
        f.fill(f.m_line((4.0, 11.0), (4.0, 12.0), 0.1, 0.1), H.tone(VEST, 0.4), 0.9)       # the vent of the vest tail
        H.shadow_under(f, 11.0, 0.5, 0.22)
    belts(f)


def body_side(f, ov, name):
    if not ov:
        K.cloth(f, f.m_all(), VEST, top=1.25, bottom=0.8, round_=0.08, fold_xs=[1.4, 2.6], fold_depth=0.08, fold_y0=1.8, fold_y1=9.0, weave_k=0.0)
        yoke = f.m_rect(0, 0, f.w, 1.55)
        f.fill(yoke, H.lin((0, 0), (0, 1.55), [(0, H.tone(YOKE, 1.12)), (0.6, YOKE), (1, H.tone(YOKE, 0.68))]))
        H.bevel(f, yoke, depth=0.24, gain=1.0, spec=0.25)
        f.fill(f.m_rect(0, 1.52, f.w, 1.66), H.tone(YOKE, 0.38), 0.9)
        seam(f, [(0.2, 1.15), (f.w - 0.2, 1.15)], PIPE, 0.065, 0.8)
        H.shadow_under(f, 1.62, 0.45, 0.26)
        edge = f.w - 0.8 if name == "west" else 0.8                                        # the side seam and the edge of the opening at the front end
        seam(f, [(edge, 2.0), (edge, 8.9)], "#9c8a6a", 0.065, 0.65)
        f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.tone(VEST, 0.9))
        H.shadow_under(f, 11.0, 0.5, 0.22)
    belts(f)


def body(cv):
    for nm in SIDES:
        base, ov = A.lay(cv, "body", nm)
        if nm == "front":
            body_front(base, False)
            body_front(ov, True)
        elif nm == "back":
            body_back(base, False)
            body_back(ov, True)
        else:
            body_side(base, False, nm)
            body_side(ov, True, nm)


# ================================================================================================ the arms
def dragon(f, x0, y0, w, h, flip=False, a=0.92):
    """The Dragon's Sin of Wrath: a coiled dragon in profile, drawn inside the box (x0, y0, w, h) of a face: an S-shaped body, a horned head at the top, a wing, a curled tail."""
    def P(u, v):
        return (x0 + (1.0 - u if flip else u) * w, y0 + v * h)
    body = [P(0.30, 0.10), P(0.62, 0.20), P(0.74, 0.36), P(0.56, 0.50), P(0.32, 0.60), P(0.40, 0.76), P(0.66, 0.86), P(0.80, 0.80)]
    f.fill(f.m_polyline(body, 0.34 * w, 1.0), INK, a)
    for i, wid in enumerate((0.20, 0.18)):
        f.fill(f.m_polyline(body[: 4 + i], wid * w), INK_L, a * 0.6)
    head = [P(0.14, 0.02), P(0.46, 0.04), P(0.58, 0.14), P(0.32, 0.16)]
    f.poly(head, INK, a)
    f.poly([P(0.20, 0.03), P(0.08, -0.05), P(0.26, 0.0)], INK, a)                          # a horn
    f.poly([P(0.50, 0.08), P(0.62, -0.03), P(0.56, 0.12)], INK, a)
    f.poly([P(0.62, 0.22), P(0.98, 0.06), P(0.90, 0.30), P(0.80, 0.28), P(0.74, 0.40)], INK, a * 0.95)    # the wing
    f.ellipse(*P(0.30, 0.095), 0.07 * w, 0.045 * h, "#ffd0b0", a=0.8)                    # the eye
    f.poly([P(0.70, 0.84), P(0.92, 0.94), P(0.78, 0.96)], INK, a)                          # the tail tip


def sleeve(f, ov, part):
    K.cloth(f, f.m_band(0.0, 3.0), SHIRT, top=1.1, bottom=0.88, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.07, fold_y0=0.9, fold_y1=2.8, weave_k=0.0)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), H.tone(SHIRT, 0.76), 0.7)                          # the shoulder seam
    seam(f, [(0.0, 0.62), (f.w, 0.62)], "#a39574", 0.055, 0.7, (0.3, 0.2), 0.15)
    seam(f, [(0.0, 2.55), (f.w, 2.55)], "#a39574", 0.055, 0.7, (0.3, 0.2), 0.15)           # the hem stitch
    H.shadow_under(f, 3.0, 0.5, 0.3)
    H.trim_band(f, 3.0, 4.0, YOKE, spec=0.3, outline=0.6)
    seam(f, [(0.12, 3.5), (f.w - 0.12, 3.5)], PIPE, 0.065, 0.85, (0.3, 0.18), 0.2)
    for x in (0.55, f.w - 0.55):
        H.stud(f, x, 3.5, 0.13, "#d8b660", spec=0.6)


def forearm(f, ov, part, y0, y1):
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(SKIN[1], 0.86)), (0.2, SKIN[1]), (0.7, H.tone(SKIN[1], 0.98)), (1, H.tone(SKIN[0], 0.92))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.12 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
    H.sheen(f, m, [f.w * 0.42], width=0.4, strength=0.12, tilt=0.0, y0=y0 + 0.6, y1=y1 - 0.4, color="#fff0e0")
    H.shadow_under(f, y0 + 0.05, 0.9, 0.25, mask=m)
    f.fill(f.m_line((f.w * 0.64, y0 + 1.6), (f.w * 0.58, y0 + 3.4), 0.16, 0.08) * m, H.tone(SKIN[0], 0.8), 0.4)   # a tendon line
    f.fill(f.m_band(y1 - 0.7, y1), H.tone(SKIN[0], 0.9), 0.25)                               # the wrist


def fingers(f):
    """The bare hand: four fingers on the front, a thumb crease on the side."""
    if f.name in ("front", "back"):
        for x in (f.w * 0.25, f.w * 0.5, f.w * 0.75):
            f.fill(f.m_line((x, 10.9), (x, 11.9), 0.17, 0.12), H.tone(SKIN[0], 0.7), 0.6)
        f.fill(f.m_band(10.8, 10.95), H.tone(SKIN[0], 0.72), 0.5)


def wrap(f, y0, y1):
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(WRAP, 1.05)), (0.5, WRAP), (1, H.tone(WRAP, 0.84))]))
    per = 1.25
    ph = ((f.Y - 0.45 * (f.X - f.w / 2.0) - y0) % per) / per                                 # slanted turns: a dark gap (one texel and a half) at the lower edge of each turn, a light streak on its body
    f.shade(m * np.clip((0.32 - ph * per) / 0.12, 0, 1), 0.66, 0.95)
    f.fill(m * np.clip(1.0 - np.abs(ph - 0.45) / 0.18, 0, 1), "#ffffff", 0.20)
    roll = f.m_band(y0, y0 + 0.62)
    f.fill(roll, H.lin((0, y0), (0, y0 + 0.62), [(0, H.tone(WRAP, 1.1)), (1, H.tone(WRAP, 0.8))]))
    f.fill(f.m_rect(0.0, y0 - 0.02, f.w, y0 + 0.12), H.tone(WRAP, 0.42), 0.85)
    f.fill(f.m_rect(0.0, y0 + 0.55, f.w, y0 + 0.66), H.tone(WRAP, 0.62), 0.7)
    H.shadow_under(f, y0 + 0.1, 0.6, 0.34)
    f.fill(f.m_band(y1 - 0.5, y1), H.tone(WRAP, 0.8), 0.4)
    if f.name == "front":                                                                    # the loose end of the bandage hangs from the roll
        tail = f.m_poly([(0.9, y0 + 0.5), (2.1, y0 + 0.5), (1.95, y0 + 2.2), (1.45, y0 + 1.9), (1.0, y0 + 2.3)])
        f.fill(tail, H.lin((0, y0 + 0.5), (0, y0 + 2.3), [(0, H.tone(WRAP, 1.0)), (1, H.tone(WRAP, 0.78))]))
        f.fill(f.m_polyline([(0.9, y0 + 0.5), (1.0, y0 + 2.3), (1.45, y0 + 1.9), (1.95, y0 + 2.2), (2.1, y0 + 0.5)], 0.07), H.tone(WRAP, 0.45), 0.8)


def arm(cv, part):
    for nm in SIDES:
        base, ov = A.lay(cv, part, nm)
        for f, isov in ((base, False), (ov, True)):
            forearm(f, isov, part, 3.6, 12.0)
            if part == "arm_r":
                wrap(f, 7.0, 12.0)
            else:
                fingers(f)
            sleeve(f, isov, part)
        if part == "arm_l":
            if nm == "east":
                dragon(base, 0.35, 4.6, 3.3, 4.9)
                dragon(ov, 0.35, 4.6, 3.3, 4.9)
            elif nm == "front":
                dragon(base, 3.0, 4.9, 1.0, 3.6, flip=True, a=0.85)


def arms(cv):
    arm(cv, "arm_r")
    arm(cv, "arm_l")


# ================================================================================================ the legs
def leg(f, ov, part, name):
    K.cloth(f, f.m_band(0.0, 6.4), TROUS, top=1.5, bottom=0.8, round_=0.12, fold_xs=[1.2, 2.8], fold_depth=0.10, fold_y0=0.4, fold_y1=6.0, weave_k=0.0)
    for y, k in ((2.6, 0.6), (3.7, 0.45)):                                                # knee creases
        f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.28), (f.w - 0.2, y + 0.05)], 0.1) * f.m_band(0, 6.4), H.tone(TROUS, 0.5), k)
        f.fill(f.m_polyline([(0.2, y - 0.15), (f.w * 0.5, y + 0.13), (f.w - 0.2, y - 0.10)], 0.06) * f.m_band(0, 6.4), H.tone(TROUS, 1.8), k * 0.5)
    if name in ("west", "east"):
        seam(f, [(f.w * 0.5, 0.2), (f.w * 0.5, 6.0)], "#8a7e9a", 0.06, 0.55)               # the outer seam of the trousers
    H.shadow_under(f, 6.55, 0.9, 0.38)                                                    # the trousers bulge over the boot top
    H.trim_band(f, 6.3, 7.35, "#7a6252", spec=0.25, outline=0.6)
    seam(f, [(0.12, 6.85), (f.w - 0.12, 6.85)], "#d0b48a", 0.06, 0.75, (0.28, 0.18), 0.2)
    boot = f.m_band(7.35, 11.25)
    f.fill(boot, H.lin((0, 7.35), (0, 11.25), [(0, K.lit(BOOT, 1.4)), (0.5, BOOT), (1, H.tone(BOOT, 0.72))]))
    H.shadow_under(f, 7.4, 0.7, 0.34)
    H.sheen(f, boot, [f.w * 0.3], width=0.2, strength=0.26, tilt=0.04, y0=7.7, y1=10.6, color="#c8a898")
    f.fill(f.m_polyline([(0.0, 9.45), (f.w * 0.5, 9.7), (f.w, 9.5)], 0.11), H.tone(BOOT, 0.45), 0.7)       # ankle wrinkles
    f.fill(f.m_polyline([(0.0, 9.78), (f.w * 0.5, 10.0), (f.w, 9.8)], 0.07), H.tone(BOOT, 1.8), 0.32)
    if name == "front":                                                                   # the strap across the shin with its buckle
        f.rect(0.0, 8.1, f.w, 8.9, H.tone(BELT_B, 1.05), r=0.0)
        f.fill(f.m_rect(0.0, 8.1, f.w, 8.28), H.tone(BELT_A, 1.5), 0.8)
        f.fill(f.m_rect(0.0, 8.82, f.w, 8.92), "#0e0a08", 0.7)
        H.stud(f, f.w * 0.5, 8.5, 0.34, GOLD)
        f.ellipse(f.w * 0.5, 10.25, 1.2, 0.65, H.tone(BOOT, 1.6), a=0.38, soft=2.0)       # the polished toe cap
    welt = f.m_band(11.0, 11.35)
    f.fill(welt, "#6e4e38", 0.95)
    f.fill(f.m_band(11.3, 12.0), H.lin((0, 11.3), (0, 12.0), [(0, "#1c1512"), (1, "#0d0b0a")]))
    f.fill(f.m_band(11.0, 11.12), "#a07c5c", 0.6)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                leg(f, ov, part, nm)


# ================================================================================================ paint
def paint(spec, cid, masks):
    spec["hair"]["color"] = HAIR                                                          # the scalp and the 3D hair share one golden blond
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=HAIR_STYLE)
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, HAIR_RGB, skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    covers = {(p, n): cover_of(cv, p, n) for p in ("body", "arm_r", "arm_l", "leg_r", "leg_l") for n in SIDES}
    head_front(cv)
    body(cv)
    arms(cv)
    legs(cv)
    if FORM2:
        demon_marks(cv)
    hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim
