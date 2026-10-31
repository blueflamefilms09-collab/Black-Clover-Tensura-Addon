"""Vetto, the Beast of the Third Eye, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for both forms: `vetto_form2.py` is the same code with the Mythical Beast palette (K.is_form2).

What he wears is the 64x64 sheet's outfit (tools/arc/char_vetto.py): a huge brute with a mane of hair, a beard and a red painted band across the face, a leather vest left open over a bare chest, bare
arms with hand wraps, a red sash with a gold buckle, baggy dark trousers, calf boots, a necklace of fangs and a beast pelt on the left shoulder. The claws (form 2: the bull horns and the dragon tail) are
the `pmx_vetto*` extras. Wide arms, the height scale (1.31) and the glow semantics (eyes, claws) are the sheet's.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): a broad face with thick angry brows, wide mad red eyes with a glow, the red war paint across the
eyes and down the cheeks, a scar over the character's right cheek, a blond beard and moustache over a wide toothy grin, a shaggy fringe; the open vest of studded leather with stitched edges and
straps, the bare chest with pectorals, a breastbone, abs and scars, a fang necklace on a cord; bare arms with a lit deltoid, a biceps bulge and forearm tendons, cream hand wraps with dark turns;
the left shoulder pelt (a fur mass in three tones with a tufted edge); the red sash with its gold buckle and a hanging end; baggy trousers with big folds and a rope tie; boots with straps.
Form 2 (the Mythical Beast Magic): white hair and beard, glowing red eyes and war paint, glowing red beast marks on the forearms and the chest.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia are blocked by the session's proxy); from memory of the series plus docs/arc/vetto.md
(which says its search digests disagree on the looks): tall muscular elf-in-a-human-body with long blond hair, a beard and a red tattoo across the face, a sadistic grin; his Evil Eye turns his hair
white. The vest, the wraps, the fangs and the pelt are the mod's own additions (docs/arc/vetto.md).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_mane"                                                         # the wild mane (hair_map): chunky and full

P1 = dict(
    hair="#dcb44c", hair_rgb=(220, 180, 76), beard="#b8903e",
    skin=("#b87a54", "#d8a072", "#ecc090"),
    vest="#3a2418", vest_l="#6a4a2c", vest_d="#1c100a", leather="#8a6a48", stitch="#d8c090",
    paint="#b01e26", paint_l="#e04a50", eye=("#7a1010", "#d83030", "#ffb0a0"), eye_glow="#ff6040",
    sash="#7a1e24", sash_l="#b03a40", sash_d="#3e0c10", gold="#d8b040",
    pants="#2e2824", pants_l="#4a4038", boot="#2a1c14", wrap="#dcd4b8", wrap_d="#9a9070",
    fur="#8a6a48", fur_l="#d0b080", fur_d="#4a3420", fang="#f0e8d0", glowmark=None,
)
P2 = dict(
    hair="#f2f0ea", hair_rgb=(242, 240, 234), beard="#d8d4c8",
    skin=("#b87a54", "#d8a072", "#ecc090"),
    vest="#3a2418", vest_l="#6a4a2c", vest_d="#1c100a", leather="#8a6a48", stitch="#d8c090",
    paint="#c01a2c", paint_l="#ff5a6a", eye=("#6a0810", "#ff2a30", "#ffc0b8"), eye_glow="#ff5040",
    sash="#7a1e24", sash_l="#b03a40", sash_d="#3e0c10", gold="#d8b040",
    pants="#2e2824", pants_l="#4a4038", boot="#2a1c14", wrap="#dcd4b8", wrap_d="#9a9070",
    fur="#8a6a48", fur_l="#d0b080", fur_d="#4a3420", fang="#f0e8d0", glowmark="#e0283c",
)


# ================================================================================================ the head
def head_front(cv, P, form2):
    f = cv.face("head", "front")
    sk = P["skin"]
    K.face_base(f, sk, side=0.1, chin=0.08)
    pal = K.hair_pal(P["hair"], light_k=1.2, dark_k=0.58)
    bp = K.hair_pal(P["beard"], light_k=1.18, dark_k=0.58)
    # the shaggy fringe
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.5), (7.6, 2.0), (6.7, 2.8), (5.6, 2.0), (4.4, 2.6), (3.2, 2.0), (2.0, 2.7), (1.0, 2.0), (-0.3, 2.6)], y_bot=2.6)
    K.strands(f, pal, (0.8, 2.0, 3.2, 4.4, 5.6, 6.8), 0.0, 2.3, mass, a=0.4)
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.8, 1.5, 1.6, 2.4, -0.4)), (0.85, (1.2, 1.5, 1.6, 2.4, 0.4)), (1.0, (5.2, 1.4, 1.8, 1.6, -0.2)), (1.0, (2.8, 1.4, 1.8, 1.6, 0.2)), (1.0, (4.0, 1.2, 1.9, 1.3, 0.0))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    for cx, lean in ((0.4, 0.15), (7.6, -0.15)):
        K.lock(f, pal, cx, 1.0, 0.8, 4.4, lean, 1.0)
    # the red war paint: a band across both eyes, drips down the cheeks
    band = f.m_poly([(-0.2, 4.15), (8.2, 4.15), (8.2, 6.0), (6.2, 5.75), (4.0, 6.1), (1.8, 5.75), (-0.2, 6.0)])
    f.fill(band, H.lin((0, 4.1), (0, 6.1), [(0, H.tone(P["paint"], 1.1)), (1, H.tone(P["paint"], 0.7))]), 0.92)
    f.fill(f.m_polyline([(0.0, 4.2), (4.0, 4.3), (8.0, 4.2)], 0.14) * band, P["paint_l"], 0.5)
    for x, ln in ((0.7, 1.7), (7.3, 1.7), (2.0, 0.9), (6.0, 0.9)):
        f.fill(f.m_poly([(x - 0.28, 5.8), (x + 0.28, 5.8), (x + 0.1, 5.8 + ln), (x, 5.8 + ln + 0.25), (x - 0.1, 5.8 + ln)]), P["paint"], 0.9)
    # the scar over the character's right cheek (the viewer's left)
    K.scar(f, [(0.8, 5.4), (1.6, 6.3), (2.3, 7.3)], "#e0a090", "#8a4a40", 0.2)
    for t in (0.3, 0.6):
        x, y = 0.8 + 1.5 * t, 5.4 + 1.9 * t
        f.fill(f.m_line((x - 0.3, y + 0.25), (x + 0.3, y - 0.25), 0.12, 0.12), "#8a4a40", 0.7)
    # thick angry brows, wide mad eyes
    brow = P["beard"]
    H.brow(f, (3.45, 4.0), (0.8, 3.1), H.tone(brow, 0.72), 0.5, 0.18, arch=-0.05)
    H.brow(f, (4.55, 4.0), (7.2, 3.1), H.tone(brow, 0.72), 0.5, 0.18, arch=-0.05)
    for cx in (2.3, 5.7):
        al = H.eye(f, cx, 5.05, 2.6, 1.7, P["eye"], lash="#2a0808", tilt=0.25, lash_w=0.5, flick=0.3, lid_shadow=0.4, hi=0.9, pupil="#2a0606")
        K.eye_glow(f, al, cx, 5.05, 2.6, 1.7, P["eye_glow"], 0.9)
    H.nose(f, 4.0, 6.3, "#a8664a", 0.3)
    # the beard and the moustache over a wide toothy grin
    beard = f.m_poly([(-0.2, 6.2), (1.0, 6.5), (2.2, 6.8), (3.0, 6.55), (5.0, 6.55), (5.8, 6.8), (7.0, 6.5), (8.2, 6.2), (8.2, 8.2), (-0.2, 8.2)])
    f.fill(beard, H.lin((0, 6.2), (0, 8.2), [(0, bp["l"]), (0.4, bp["m"]), (1, H.tone(bp["m"], 0.7))]))
    for x in (0.4, 1.4, 2.4, 5.6, 6.6, 7.6):
        f.fill(f.m_line((x, 6.5), (x + 0.1, 8.0), 0.2, 0.14) * beard, bp["h"], 0.4)
        f.fill(f.m_line((x + 0.4, 6.7), (x + 0.5, 8.1), 0.18, 0.1) * beard, bp["d"], 0.5)
    mw = f.m_poly([(1.9, 7.1), (4.0, 6.85), (6.1, 7.1), (5.6, 7.9), (4.0, 8.15), (2.4, 7.9)])
    f.fill(mw, "#3a0a10", 1.0)
    teeth = f.m_poly([(2.1, 7.15), (4.0, 6.95), (5.9, 7.15), (5.7, 7.55), (4.0, 7.65), (2.3, 7.55)])
    f.fill(teeth, "#f6f0e0", 1.0)
    for x in (2.9, 3.5, 4.0, 4.5, 5.1):
        f.fill(f.m_line((x, 7.1), (x, 7.6), 0.1, 0.1) * teeth, "#a89a88", 0.7)
    f.fill(f.m_polyline([(2.4, 7.55), (4.0, 7.7), (5.6, 7.55)], 0.16), "#5a1018", 0.8)
    f.fill(f.m_polyline([(2.0, 6.95), (3.0, 6.7), (4.0, 6.8), (5.0, 6.7), (6.0, 6.95)], 0.34), H.tone(bp["m"], 0.78), 0.95)        # the moustache
    f.fill(f.m_polyline([(2.2, 6.85), (3.2, 6.65), (4.0, 6.75)], 0.12), bp["h"], 0.5)
    if form2:                                                                       # the paint and the eyes burn
        f.glow_fill(band * 0.5, P["glowmark"], 0.35)


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.2, dark_k=0.58)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.8, 1.0, 3.4, lean, 0.95)
        K.lock(f, pal, cx + (-2.5 if nm == "west" else 2.5), 1.4, 0.6, 2.6, lean, 1.0)


# ================================================================================================ the body
def fang_necklace(f, P):
    """A cord round the neck with a row of fangs hanging on the chest."""
    cord = [(1.7, 0.0), (2.4, 1.0), (3.2, 1.6), (4.0, 1.85), (4.8, 1.6), (5.6, 1.0), (6.3, 0.0)]
    f.fill(f.m_polyline(cord, 0.2), "#3a2a1a", 0.95)
    f.fill(f.m_polyline([(x, y - 0.05) for x, y in cord], 0.08), "#8a6a48", 0.7)
    for (x, y), ln in zip(cord[1:-1], (0.7, 0.9, 1.15, 0.9, 0.7)):
        f.fill(f.m_poly([(x - 0.2, y), (x + 0.2, y), (x + 0.02, y + ln)]), "#f4ecd8", 1.0)
        f.fill(f.m_poly([(x + 0.02, y), (x + 0.2, y), (x + 0.02, y + ln)]), "#b8ac90", 0.8)


def chest(f, P, form2):
    sk = P["skin"]
    m = f.m_poly([(2.0, 0.0), (6.0, 0.0), (5.9, 9.0), (2.1, 9.0)])
    f.fill(m, H.lin((0, 0), (0, 9), [(0, sk[2]), (0.4, sk[1]), (1, H.tone(sk[0], 0.9))]))
    # the pectorals: two lit swells with a shaded lower edge and a breastbone line
    for cx, sgn in ((3.1, 1), (4.9, -1)):
        f.fill(f.m_ellipse(cx, 2.9, 1.15, 0.95) * m, H.tone(sk[2], 1.05), 0.55)
        f.fill(f.m_polyline([(cx - 1.1, 3.75), (cx, 4.0), (cx + 1.1, 3.75)], 0.22) * m, H.tone(sk[0], 0.7), 0.75)
    f.fill(f.m_line((4.0, 1.4), (4.0, 3.9), 0.2, 0.2) * m, H.tone(sk[0], 0.72), 0.75)
    # abs: three rows of two blocks
    for y in (5.0, 6.3, 7.6):
        f.fill(f.m_polyline([(2.4, y), (4.0, y + 0.12), (5.6, y)], 0.17) * m, H.tone(sk[0], 0.72), 0.7)
    f.fill(f.m_line((4.0, 4.1), (4.0, 8.4), 0.18, 0.18) * m, H.tone(sk[0], 0.75), 0.8)
    for y in (5.6, 6.9):
        for cx in (3.2, 4.8):
            f.fill(f.m_ellipse(cx, y, 0.55, 0.38) * m, H.tone(sk[2], 1.04), 0.4)
    f.fill(f.m_ellipse(4.0, 8.5, 0.16, 0.2), H.tone(sk[0], 0.55), 0.9)                          # the navel
    K.scar(f, [(2.7, 2.2), (3.4, 3.1), (3.9, 4.2)], "#e0a090", "#8a4a40", 0.18)
    K.scar(f, [(5.6, 5.4), (5.1, 6.2)], "#e0a090", "#8a4a40", 0.16)
    if form2:
        for pts in ([(2.4, 2.0), (3.0, 3.0), (2.8, 4.0)], [(5.6, 2.0), (5.0, 3.0), (5.2, 4.0)]):
            K.glow_line(f, pts, P["glowmark"], 0.16, 0.9, core="#ffc0c0", core_a=0.55)


def vest_panel(f, P, x0, x1, left):
    m = f.m_rect(x0, 0.0, x1, 9.0)
    K.cloth(f, m, P["vest"], top=1.5, bottom=0.8, round_=0.1, fold_xs=[(x0 + x1) / 2.0], fold_depth=0.1, fold_y0=1.2, fold_y1=8.6)
    f.fill(f.m_rect(x0, 0.0, x1, 1.3), H.lin((0, 0), (0, 1.3), [(0, P["vest_l"]), (1, P["vest"])]), 0.9)         # the shoulder strap, lighter leather
    edge = x1 if left else x0
    H.trim_line(f, [(edge, 0.2), (edge, 9.0)], P["leather"], width=0.34, spec=0.35, outline=0.6)
    K.seam(f, [(edge + (-0.42 if left else 0.42), 1.0), (edge + (-0.42 if left else 0.42), 9.0)], P["stitch"], 0.06, 0.75)
    for y in (2.2, 4.6, 7.0):                                                                           # rivets
        H.stud(f, (x0 + x1) / 2.0, y, 0.15, P["gold"], spec=0.8)
    f.fill(f.m_line((x0 + 0.4, 0.8), (x1 - 0.4, 0.8), 0.14, 0.14), P["vest_d"], 0.6)
    H.shadow_under(f, 9.0, 0.5, 0.3, x0=x0, x1=x1)


def sash_pieces(f, P, front):
    K.belt_band(f, 9.0, 10.5, P["sash"], stitch=None)
    f.fill(f.m_rect(0.0, 9.0, f.w, 9.2), P["sash_l"], 0.7)
    for x in np.arange(0.3, f.w, 0.9):                                                                   # the weave of the wrapped sash
        f.fill(f.m_line((x, 9.1), (x + 0.3, 10.4), 0.12, 0.12), P["sash_d"], 0.4)
    if front:
        K.buckle(f, 4.0, 9.75, 1.8, 1.3, P["gold"], hole=P["sash_d"])
        f.fill(f.m_poly([(4.8, 10.5), (5.9, 10.5), (5.7, 12.0), (5.35, 11.7), (5.0, 12.0)]), H.lin((0, 10.5), (0, 12), [(0, P["sash"]), (1, P["sash_d"])]), 0.95)
    else:
        H.stud(f, 4.0, 9.75, 0.2, P["gold"], spec=0.8)
    f.fill(f.m_rect(0.0, 10.5, f.w, 12.0), H.lin((0, 10.5), (0, 12), [(0, K.lit(P["pants"], 1.1)), (1, H.tone(P["pants"], 0.7))]))
    H.shadow_under(f, 10.52, 0.5, 0.3)


def body(cv, P, form2):
    sk = P["skin"]
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                chest(f, P, form2)
                vest_panel(f, P, 0.0, 2.1, True)
                vest_panel(f, P, 5.9, 8.0, False)
                fang_necklace(f, P)
            elif nm == "back":
                K.cloth(f, f.m_all(), P["vest"], top=1.5, bottom=0.8, round_=0.12, fold_xs=[1.2, 2.6, 5.4, 6.8], fold_depth=0.1, fold_y0=1.6, fold_y1=8.8)
                f.fill(f.m_rect(0.0, 0.0, 8.0, 1.3), H.lin((0, 0), (0, 1.3), [(0, P["vest_l"]), (1, P["vest"])]), 0.9)
                f.fill(f.m_rect(0.0, 1.25, 8.0, 1.42), P["vest_d"], 0.8)
                K.seam(f, [(0.2, 1.7), (7.8, 1.7)], P["stitch"], 0.06, 0.6)
                f.fill(f.m_line((4.0, 1.7), (4.0, 9.0), 0.2, 0.2), P["vest_d"], 0.7)
                K.seam(f, [(3.55, 1.9), (3.55, 8.8)], P["stitch"], 0.06, 0.55)
                K.seam(f, [(4.45, 1.9), (4.45, 8.8)], P["stitch"], 0.06, 0.55)
                for y in (2.4, 4.4, 6.4, 8.2):
                    H.stud(f, 4.0, y, 0.16, P["gold"], spec=0.8)
            else:
                K.cloth(f, f.m_all(), P["vest"], top=1.5, bottom=0.8, round_=0.12, fold_xs=[1.2, 2.6], fold_depth=0.1, fold_y0=1.6, fold_y1=8.8)
                f.fill(f.m_rect(0.0, 0.0, f.w, 1.3), H.lin((0, 0), (0, 1.3), [(0, P["vest_l"]), (1, P["vest"])]), 0.9)
                f.fill(f.m_rect(0.0, 1.25, f.w, 1.42), P["vest_d"], 0.8)
                ed = f.w - 0.7 if nm == "west" else 0.7
                K.seam(f, [(ed, 1.8), (ed, 8.8)], P["stitch"], 0.06, 0.5)
            sash_pieces(f, P, nm == "front")


# ================================================================================================ the arms
def arm(cv, part, P, form2):
    sk = P["skin"]
    left = part == "arm_l"
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            m = f.m_band(0.0, 12.0)
            K.limb_skin(f, sk, 0.0, 12.0, tendon=True)
            f.fill(m, H.lin((0, 0), (0, 12), [(0, K.lit(sk[2], 1.0)), (0.5, sk[1]), (1, H.tone(sk[0], 0.9))]), 0.5)
            # the deltoid highlight, the biceps bulge (lit centre, shaded rim), the forearm tendons
            f.fill(f.m_ellipse(f.w * 0.42, 1.6, f.w * 0.36, 1.2), H.tone(sk[2], 1.06), 0.6)
            f.fill(f.m_ellipse(f.w * 0.45, 3.6, f.w * 0.3, 1.5), H.tone(sk[2], 1.04), 0.45)
            f.fill(f.m_polyline([(0.2, 5.2), (f.w * 0.5, 5.45), (f.w - 0.2, 5.2)], 0.18), H.tone(sk[0], 0.7), 0.6)             # the elbow
            for x in (f.w * 0.32, f.w * 0.62):
                f.fill(f.m_line((x, 5.9), (x + 0.1, 7.2), 0.14, 0.1), H.tone(sk[0], 0.75), 0.55)
            H.shadow_under(f, 5.3, 0.5, 0.18)
            # the hand wraps: cream turns with dark gaps, from the forearm to the knuckles
            K.wrap_lines(f, f.m_band(7.4, 11.2), 7.4, 11.2, P["wrap"], P["wrap_d"], step=0.9, slant=0.3, a=0.97)
            f.fill(f.m_band(7.38, 7.6), H.tone(P["wrap"], 0.55), 0.85)
            H.shadow_under(f, 7.4, 0.6, 0.3)
            # the bare fist: knuckles and finger gaps
            f.fill(f.m_band(11.2, 12.0), H.lin((0, 11.2), (0, 12), [(0, sk[1]), (1, H.tone(sk[0], 0.85))]))
            if nm in ("front", "back"):
                for i in range(1, 4):
                    x = f.w * i / 4.0
                    f.fill(f.m_line((x, 11.35), (x, 11.95), 0.2, 0.14), H.tone(sk[0], 0.62), 0.85)
            if left:                                                                                         # the pelt on the left shoulder
                K.fur_band(f, 0.0, 2.7, P["fur"], P["fur_l"], P["fur_d"], tuft=0.55, seed=5 + SIDES.index(nm))
                f.fill(f.m_rect(0.0, 2.6, f.w, 3.2), H.tone(P["fur_d"], 0.8), 0.0)
                H.shadow_under(f, 3.3, 0.7, 0.3)
            if form2 and nm in ("front", "west", "east"):
                K.glow_line(f, [(f.w * 0.4, 5.9), (f.w * 0.5, 6.6), (f.w * 0.45, 7.2)], P["glowmark"], 0.14, 0.9, core="#ffc0c0", core_a=0.5)
    for ov in (False, True):
        K.cap(cv, part, "top", P["fur"] if left else sk[1], edge=P["fur_d"] if left else None, k_top=1.15, overlay=ov)


# ================================================================================================ the legs
def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 7.6)
                K.cloth(f, m, P["pants"], top=1.4, bottom=0.8, round_=0.12, fold_xs=[0.9, 1.9, 3.1], fold_depth=0.14, fold_y0=0.3, fold_y1=7.2)
                for y, k in ((2.4, 0.55), (3.2, 0.4), (4.6, 0.4)):                                          # big baggy creases
                    f.fill(f.m_polyline([(0.1, y), (f.w * 0.5, y + 0.34), (f.w - 0.1, y + 0.05)], 0.16) * m, H.tone(P["pants"], 0.5), k)
                    f.fill(f.m_polyline([(0.1, y - 0.2), (f.w * 0.5, y + 0.12), (f.w - 0.1, y - 0.12)], 0.14) * m, P["pants_l"], k * 0.8)
                H.shadow_under(f, 0.0, 0.6, 0.25)
                # a rope tie round the ankle over the boot top
                f.fill(f.m_band(6.9, 7.5), H.lin((0, 6.9), (0, 7.5), [(0, "#a08a62"), (1, "#5a4a30")]))
                for x in np.arange(0.2, f.w, 0.6):
                    f.fill(f.m_line((x, 6.9), (x + 0.3, 7.5), 0.12, 0.12), "#3a2c18", 0.5)
                H.shadow_under(f, 7.55, 0.7, 0.34)
                K.boot(f, P["boot"], 7.5, nm, strap=True, strap_color=P["vest_l"], gold=P["gold"], welt="#6a4a32", sole="#0c0806")
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", "#0c0806", edge=None, k_top=1.0, overlay=ov)


# ================================================================================================ paint
def paint(spec, cid, masks):
    form2 = K.is_form2(cid)
    P = P2 if form2 else P1
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, P["hair"], P["hair_rgb"])
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"])
    head_sides(cv, P)
    head_front(cv, P, form2)
    body(cv, P, form2)
    arm(cv, "arm_r", P, form2)
    arm(cv, "arm_l", P, form2)
    legs(cv, P)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim)
