"""Leopold Vermillion, the youngest of the Crimson Lion's Vermillions, HD 256x256 (docs/arc/hd_arc_bc4.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_leopold_vermillion.py): a dark-red tunic with a stand collar and gold trim down to the thigh, short sleeves with gold cuffs, a plain
belt with a gold buckle, fingerless dark gloves with wrist bands, dark trousers and calf-high boots. The red mantle with the gold flame and the floating grimoire are the
`pmx_leopold_vermillion` extra (the mantle hangs from the shoulders and covers the back and the back of the legs to the thigh, so the back of the tunic is kept plain; nothing is painted where
the grimoire floats). No glow on the skin (the flame glow is the grimoire's). The sheet's second layer covers the whole tunic, the sleeves with their cuffs and the wrist bands; the forearms and
the hands are bare.
Drawn at 4x detail on both layers: the big amber eyes with strong lashes and a highlight, the short thick slanted brows, the wide grin with a sharp canine and a light blush, the wild locks of
the fringe; the tunic with lit panels, a gold-trimmed placket with toggles, the stand collar, the yoke seam, the belt with the buckle, the gold hem of the skirt; the sleeves with stitched gold cuffs,
the bare forearms with tendon lines, the wrist bands with studs, the fingerless hands; the trousers with creases and the boots with their gold-edged cuffs and straps.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus docs/arc/npcb.md: a young, wiry
man with wild spiky red hair, a sharp canine tooth and an eager grin, in the dark-red Crimson Lion tunic. The hair is the tall layered spike cut (flame-like clumps), the toggles and the stitched
cuffs are added in the HD pass.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc4 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_spike_tall"                               # thick layered clumps standing up like flames (the baseline anime_spiky read as thin fingers)
HAIR = "#de4a1e"
HAIR_RGB = (222, 74, 30)
SKIN = ("#deaa8a", "#f0c6a4", "#fadcbc")
TUNIC = "#a02a1e"
TUNIC_D = "#6a1612"
GOLD = "#e8b64c"
TROUS = "#2a2a34"
BOOT = "#4a3228"
BELT = "#46301f"
BAND = "#3a2a22"
IRIS = ("#7a3408", "#f2a236", "#ffe9a8")
SEAM = "#d9b45a"


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.09, chin=0.06)
    pal = K.hair_pal(HAIR, light_k=1.15, dark_k=0.52)
    H.blush(f, 1.25, 6.35, 1.0, 0.55, "#ee8a7a", 0.4)
    H.blush(f, 6.75, 6.35, 1.0, 0.55, "#ee8a7a", 0.4)
    brow = H.mix(HAIR, "#2a0c06", 0.5)
    H.brow(f, (3.55, 4.5), (1.1, 3.75), brow, 0.46, 0.2, arch=0.02)
    H.brow(f, (4.45, 4.5), (6.9, 3.75), brow, 0.46, 0.2, arch=0.02)
    # big eager amber eyes
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.3, 2.75, 2.1, IRIS, lash="#2a0e08", tilt=0.24, lash_w=0.46, flick=0.4, lid_shadow=0.34, hi=0.95)
    H.nose(f, 4.0, 6.4, "#c58f78", 0.22)
    H.mouth_smile(f, 4.0, 6.85, 4.0, color="#7a2a26", lip="#d8847a", depth=0.3, thick=0.2, teeth="#fffdf6", open_h=0.62)
    f.poly([(5.1, 7.1), (5.55, 7.1), (5.32, 7.75)], "#ffffff", 0.95)                                    # the sharp canine
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.1), (7.4, 1.4), (6.5, 2.4), (5.4, 1.3), (4.4, 2.2), (3.3, 1.3), (2.1, 2.4), (1.0, 1.5), (-0.3, 2.2)], y_bot=2.3)
    K.strands(f, pal, (0.7, 1.9, 3.1, 4.3, 5.5, 6.7), 0.0, 1.8, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.85, (6.5, 1.5, 1.0, 2.0, -0.6)),
        (0.9, (4.4, 1.6, 1.0, 1.7, 0.4)),
        (0.9, (2.1, 1.6, 1.0, 2.0, 0.5)),
        (0.95, (3.3, 1.2, 1.0, 1.2, 0.2)),
        (0.95, (5.4, 1.2, 1.0, 1.2, -0.2)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.45, 1.1, 0.7, 4.2, 0.3, 1.0)
    K.lock(f, pal, 7.55, 1.1, 0.7, 4.2, -0.3, 1.0)


# ================================================================================================ the tunic
def collar(f, name):
    m = f.m_band(0.0, 1.5)
    K.cloth(f, m, TUNIC_D, top=1.4, bottom=0.85, round_=0.1)
    f.fill(f.m_rect(0.0, 1.36, f.w, 1.52), GOLD, 1.0)
    f.fill(f.m_rect(0.0, 1.36, f.w, 1.42), K.lit(GOLD, 1.5), 0.9)
    f.fill(f.m_rect(0.0, 1.5, f.w, 1.6), "#2a0a0a", 0.55)
    H.shadow_under(f, 1.52, 0.5, 0.24)
    K.seam(f, [(0.15, 1.05), (f.w - 0.15, 1.05)], SEAM, 0.05, 0.5)
    if name == "front":
        f.fill(f.m_poly([(3.3, 0.0), (4.7, 0.0), (4.0, 1.2)]), SKIN[1])                           # the open neck: a small V of skin
        f.fill(f.m_polyline([(3.3, 0.0), (4.0, 1.2), (4.7, 0.0)], 0.16), "#2a0a0a", 0.6)
        f.fill(f.m_ellipse(4.0, 0.2, 0.8, 0.5, soft=2.0), H.tone(SKIN[0], 0.8), 0.4)


def skirt(f, y0, y1=12.0, hem=None):
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(TUNIC, 0.9)), (1, H.tone(TUNIC, 0.74))]))
    H.folds(f, m, [f.w * 0.25, f.w * 0.5, f.w * 0.75], depth=0.12, width=0.25, y0=y0, y1=y1)
    if hem is not None:
        f.fill(f.m_band(hem, y1), H.lin((0, hem), (0, y1), [(0, K.lit(GOLD, 1.15)), (1, H.tone(GOLD, 0.7))]))
        f.fill(f.m_band(hem - 0.06, hem + 0.06), "#2a0a0a", 0.7)
        f.fill(f.m_band(hem + 0.04, hem + 0.16), K.lit(GOLD, 1.7), 0.6)


def body_front(f, ov):
    K.cloth(f, f.m_all(), TUNIC, top=1.3, bottom=0.8, round_=0.13, fold_xs=[1.3, 6.7], fold_depth=0.10, fold_y0=2.0, fold_y1=8.8)
    for x0, x1 in ((0.0, 3.0), (5.0, 8.0)):
        K.sheen(f, f.m_rect(x0, 1.6, x1, 8.8), [x0 + 0.7 if x0 < 4 else x1 - 0.7], 2.0, 8.4, 0.2, "#f0a590")
    f.fill(f.m_rect(0.0, 1.6, 0.28, 8.8), K.lit(TUNIC, 1.5), 0.35)
    f.fill(f.m_rect(7.72, 1.6, 8.0, 8.8), K.deep(TUNIC, 0.6), 0.4)
    # the placket: a darker strip with a gold edge each side and three gold toggles
    pl = f.m_rect(3.25, 1.5, 4.75, 8.9)
    K.cloth(f, pl, TUNIC_D, top=1.15, bottom=0.85, round_=0.05)
    for x in (3.25, 4.75):
        H.trim_line(f, [(x, 1.6), (x, 8.9)], GOLD, width=0.32, spec=0.5, outline=0.6)
    for y in (2.9, 4.9, 6.9):
        H.stud(f, 4.0, y, 0.3, GOLD, spec=0.9)
        f.fill(f.m_line((3.55, y), (4.45, y), 0.14, 0.14), "#2a0a0a", 0.5)
    # the yoke seam across the chest and a gold shoulder line
    K.seam(f, [(0.2, 2.5), (3.0, 2.8)], SEAM, 0.05, 0.55)
    K.seam(f, [(7.8, 2.5), (5.0, 2.8)], SEAM, 0.05, 0.55)
    collar(f, "front")
    # the belt
    K.belt_band(f, 8.9, 10.2, BELT, stitch="#a8794a")
    K.buckle(f, 4.0, 9.55, 2.0, 1.55, GOLD, hole="#1c0c0a")
    skirt(f, 10.2, hem=None)


def body_back(f, ov):
    K.cloth(f, f.m_all(), TUNIC, top=1.22, bottom=0.8, round_=0.13, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.09, fold_y0=2.0, fold_y1=9.0)
    collar(f, "back")
    f.fill(f.m_line((4.0, 1.9), (4.0, 8.8), 0.2, 0.2), K.deep(TUNIC, 0.45), 0.6)
    K.belt_band(f, 8.9, 10.2, BELT, stitch="#a8794a")
    H.stud(f, 4.0, 9.55, 0.2, GOLD, spec=0.7)
    skirt(f, 10.2)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), TUNIC, top=1.22, bottom=0.8, round_=0.13, fold_xs=[1.3, 2.7], fold_depth=0.09, fold_y0=2.0, fold_y1=9.0)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.6)], SEAM, 0.05, 0.5)
    K.belt_band(f, 8.9, 10.2, BELT, stitch="#a8794a")
    skirt(f, 10.2)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ the arms: short sleeves, bare forearms, wrist bands, fingerless hands
def arm(cv, part):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, part, nm), (False, True)):
            # bare arm below the sleeve
            m = f.m_band(3.0, 12.0)
            f.fill(m, H.lin((0, 3.0), (0, 12.0), [(0, H.tone(SKIN[1], 0.88)), (0.2, SKIN[1]), (0.7, H.tone(SKIN[1], 0.98)), (1, H.tone(SKIN[0], 0.95))]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(m, 1.0 - 0.13 * (1.0 - H.smoothstep(0.0, 1.0, side)), 0.9)
            K.sheen(f, m, [f.w * 0.42], 4.0, 8.6, 0.12, "#fff0e0", width=0.45)
            if nm in ("front", "back"):
                f.fill(f.m_line((f.w * 0.64, 5.0), (f.w * 0.58, 7.4), 0.18, 0.12) * m, H.tone(SKIN[0], 0.78), 0.42)     # a tendon line
            f.fill(f.m_ellipse(f.w * 0.5, 5.4, f.w * 0.4, 0.4, soft=2.5) * m, H.tone(SKIN[0], 0.82), 0.35)              # the elbow
            # the short sleeve and its gold cuff
            sl = f.m_band(0.0, 3.0)
            K.cloth(f, sl, TUNIC, top=1.28, bottom=0.82, round_=0.16, fold_xs=[0.9, 2.1], fold_depth=0.10, fold_y0=0.8, fold_y1=2.8)
            K.sheen(f, sl, [f.w * 0.33], 0.8, 2.6, 0.18, "#f0a590")
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), K.lit(TUNIC, 1.3), 0.5)
            K.seam(f, [(0.0, 0.6), (f.w, 0.6)], SEAM, 0.05, 0.5)                                                         # the shoulder seam
            H.shadow_under(f, 3.0, 0.5, 0.3)
            K.cuff(f, 3.0, 4.25, GOLD, pipe="#6a4a14", stud_color="#fff0b0", spec=0.4)
            H.shadow_under(f, 4.25, 0.6, 0.28, mask=m)
            # the wrist band and the fingerless hand
            K.belt_band(f, 9.1, 10.5, BAND, stitch="#a8794a")
            if nm in ("front", "back"):
                H.stud(f, f.w * 0.5, 9.8, 0.2, GOLD, spec=0.8)
            hand = f.m_band(10.5, 12.0)
            f.fill(hand, H.lin((0, 10.5), (0, 12.0), [(0, "#4a3a32"), (0.6, BAND), (1, "#8a5a44")]))          # a dark leather palm with the fingers peeking out below
            f.fill(f.m_band(11.45, 12.0), H.lin((0, 11.45), (0, 12.0), [(0, SKIN[1]), (1, H.tone(SKIN[0], 0.95))]))
            if nm in ("front", "back"):
                for i in range(1, 4):
                    x = f.w * i / 4
                    f.fill(f.m_line((x, 11.5), (x, 11.98), 0.2, 0.18), H.tone(SKIN[0], 0.7), 0.7)
            f.fill(f.m_rect(0.0, 11.4, f.w, 11.5), "#1c1410", 0.6)
            H.shadow_under(f, 10.5, 0.4, 0.22)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", TUNIC, edge=GOLD, overlay=ov)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 7.0, nm, knee=False, top=1.35, bottom=0.85, seam_color="#5a5a6a")
                skirt(f, 0.0, 4.2, hem=3.45)
                H.shadow_under(f, 4.2, 0.8, 0.3)
                for y, k in ((5.4, 0.6),):
                    f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.28), (f.w - 0.2, y + 0.05)], 0.14), K.deep(TROUS, 0.5), k)
                H.shadow_under(f, 7.0, 0.8, 0.4)
                K.boot(f, BOOT, 6.9, nm, strap=False, gold=GOLD, cuff_color=GOLD, cuff_h=0.7, welt="#7a5a40", sole="#0d0a0a")
                if nm == "front":
                    f.fill(f.m_rect(0.0, 8.6, f.w, 9.2), H.tone("#6a4a34", 1.0), 0.95)
                    f.fill(f.m_rect(0.0, 8.6, f.w, 8.72), K.lit("#6a4a34", 1.6), 0.8)
                    H.stud(f, f.w * 0.5, 8.9, 0.26, GOLD)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    arm_tops(cv)
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)
