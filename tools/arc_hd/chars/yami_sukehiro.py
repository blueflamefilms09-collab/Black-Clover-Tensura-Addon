"""Yami Sukehiro, captain of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc1.md): the hand-painted look of the tanned, stubbled, sharp-eyed captain who is the first NPC of the roster.

What he wears is the 64x64 sheet's outfit (tools/arc/char_yami_sukehiro.py): a plain white tank top with a black strap at the throat, a dark belt of books with a gold buckle, black trousers under light
leather chaps, black calf boots, bare broad arms with wrapped forearms. The ragged black Black Bull cloak (bull emblem on the back), the katana in his right hand and the cigarette at the corner of
his mouth are 3D extras (`pmx_yami_sukehiro`: `cape`, `cape_low`, `prop_r`, `cigarette`), so the skin carries nothing of them; the left corner of the mouth stays clear for the cigarette. Dark magic is
not drawn (no glow; the cigarette ember is the only glow of the look).

Drawn at 4x detail on both layers: the sharp, narrow dark eyes with heavy lids, thick slanted black brows, a broad tanned face with a dense stubble, a moustache and a small goatee, a lopsided smirk, the
white tank with folds, a deep scooped neck and a black throat strap with a ring, the muscled bare arms with bandage wraps on the forearms, the belt with a big gold buckle and three small books, the
black trousers with creases, the beige leather chaps (stitched edges, rivets, a thigh strap, knee wrinkles) and the black boots with a strap and buckle.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus docs/arc/npcb.md
(which cites the wikis) and the spec of the sheet: tall broad tanned man, wild black hair, stubble and moustache, a smirk, a white tank, a black throat strap. Not verified against a wiki: the wrapped
forearms (the owner's brief asked for bandages; they were already in the spec) and the books on the belt (the spec says `books: True`).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_spike_tall"
HAIR = "#15151b"
HAIR_RGB = (21, 21, 27)
SKIN = ("#b86d45", "#da9565", "#f0b888")
TANK, TANK_D = "#ecebe6", "#b9b4aa"
STRAP = "#25252c"
TROUS = "#26262e"
CHAP, CHAP_D, CHAP_L = "#c4b08a", "#8a7552", "#e6d6b0"
BOOT = "#222027"
BELT, BUCKLE = "#3d2b22", "#e4b454"
BAND, BAND_D = "#d8d2c0", "#8e8872"
IRIS = ("#1a1920", "#3a3945", "#9a9aac")
BROW = "#101015"
STUBBLE = "#2a1c18"


def noise(f, seed):
    return np.random.RandomState(seed).rand(*f.X.shape).astype(np.float32)


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.14, chin=0.1, jaw=0.18)
    pal = K.dark_hair_palette(HAIR)
    # the fringe: a mass at the crown and three short wild bangs over the brow (the 3D spikes are drawn on top of it)
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.0), (7.6, 1.6), (6.9, 2.3), (6.1, 1.7), (5.3, 2.2), (4.4, 1.5), (3.5, 2.1), (2.6, 1.6), (1.6, 2.3), (0.9, 1.7), (-0.3, 2.1)]
    K.hair_mass(f, pal, outline, 0.0, 2.3, shadow_k=0.82, shadow_depth=1.6, streaks=(0.9, 2.2, 3.5, 4.9, 6.3, 7.3))
    for sh, (cx, w, top, ln, lean) in ((0.9, (7.0, 1.5, 1.6, 1.5, -0.3)), (0.9, (5.0, 1.3, 1.8, 1.0, 0.1)), (0.9, (3.0, 1.3, 1.8, 1.1, -0.1)), (0.9, (1.0, 1.5, 1.6, 1.7, 0.3))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    # thick, black, hard-slanted brows; narrow sharp eyes with heavy lids; a dark ring under them (he is tired, not sleepy)
    H.brow(f, (3.5, 4.0), (0.95, 3.2), BROW, 0.56, 0.2, arch=0.04)
    H.brow(f, (4.5, 4.0), (7.05, 3.2), BROW, 0.56, 0.2, arch=0.04)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.0, 2.6, 1.6, IRIS, lash="#0c0b10", tilt=0.28, lash_w=0.5, flick=0.3, lid_shadow=0.38, iris_r=1.04, hi=0.8)
        f.ellipse(cx, 5.85, 1.1, 0.25, H.tone(SKIN[0], 0.78), a=0.35, soft=2.0)
    H.nose(f, 4.0, 6.0, "#9a5c3c", 0.32)
    f.fill(f.m_line((3.3, 4.3), (3.2, 5.9), 0.2, 0.12), H.tone(SKIN[0], 0.82), 0.25)                           # the bridge of the nose, shaded on one side
    f.fill(f.m_line((1.0, 6.2), (1.5, 6.85), 0.1, 0.06), H.tone(SKIN[0], 0.7), 0.35)                           # the laugh lines
    f.fill(f.m_line((7.0, 6.2), (6.5, 6.85), 0.1, 0.06), H.tone(SKIN[0], 0.7), 0.35)
    # the stubble: single dark texels along the jaw and the chin (the cheeks stay clean under the eyes), a little denser round the mouth
    region = f.m_poly([(0.4, 5.9), (1.4, 6.5), (2.3, 6.3), (5.7, 6.3), (6.6, 6.5), (7.6, 5.9), (7.9, 6.9), (6.9, 8.1), (1.1, 8.1), (0.1, 6.9)])
    f.fill(region * (noise(f, 11) < 0.24).astype(np.float32), STUBBLE, 0.5)
    f.fill(f.m_ellipse(4.0, 7.3, 2.9, 1.1) * (noise(f, 12) < 0.22).astype(np.float32), STUBBLE, 0.5)
    # the moustache: a dark band over the lip that droops at both ends, with a clear philtrum notch; the goatee: a small dark patch under the lip
    f.fill(f.m_poly([(2.3, 6.7), (3.1, 6.3), (3.85, 6.4), (4.0, 6.5), (4.15, 6.4), (4.9, 6.3), (5.7, 6.7), (5.3, 6.85), (4.0, 6.62), (2.7, 6.85)]), "#17141a", 0.9)
    f.fill(f.m_poly([(3.3, 7.6), (4.7, 7.6), (4.55, 8.05), (3.45, 8.05)]), "#17141a", 0.8)
    f.fill(f.m_line((3.1, 6.45), (3.7, 6.42), 0.08, 0.05), "#55506a", 0.45)
    # the mouth: a lopsided smirk (the viewer's right corner lifted, the left one flat and clear for the cigarette); the lower lip catches the light
    f.fill(f.m_polyline([(3.1, 7.12), (3.9, 7.22), (4.8, 7.1), (5.45, 6.82), (5.8, 6.58)], 0.2), "#3c1612", 0.95)
    f.fill(f.m_ellipse(4.5, 7.42, 0.7, 0.15), "#d08a70", 0.6)
    f.fill(f.m_line((5.95, 6.35), (5.85, 6.75), 0.1, 0.06), H.tone(SKIN[0], 0.62), 0.55)                       # the dimple at the lifted corner


# ================================================================================================ the body (a white tank)
def tank_front(f):
    m = f.m_all()
    K.cloth(f, m, TANK, top=1.04, bottom=0.78, round_=0.1)
    f.shade(m, 1.0 + 0.1 * K.blob(f, 4.0, 3.0, 3.7, 1.5, 0.9), 0.9)
    for sgn in (-1.0, 1.0):                                                                    # the pecs: a shallow shadow arc under each, a tension fold from the armhole
        cx = 4.0 + sgn * 1.9
        pts = [(cx - 1.8, 4.3), (cx - 0.9, 4.7), (cx + 0.1, 4.75), (cx + 1.0, 4.55), (cx + 1.8, 4.25)] if sgn < 0 else [(cx - 1.8, 4.25), (cx - 1.0, 4.55), (cx - 0.1, 4.75), (cx + 0.9, 4.7), (cx + 1.8, 4.3)]
        f.fill(f.m_polyline(pts, 0.32, 2.0), TANK_D, 0.5)
    f.fill(f.m_line((4.0, 3.0), (4.0, 4.8), 0.16, 0.12), TANK_D, 0.45)
    for y in (6.0, 7.2):                                                                       # the abs under the cloth: soft rows
        f.shade(m, 1.0 - 0.1 * K.blob(f, 4.0, y, 2.2, 0.3, 1.0), 0.9)
    K.crease(f, [(0.8, 5.3), (1.8, 6.4), (2.3, 7.8)], TANK, 0.1, 0.4, light=1.05)
    K.crease(f, [(7.2, 5.3), (6.2, 6.4), (5.7, 7.8)], TANK, 0.1, 0.4, light=1.05)
    # the deep scooped neck: skin inside, the cloth edge a little darker and a lit hem
    scoop = f.m_poly([(1.75, 0.0), (6.25, 0.0), (5.9, 0.9), (4.0, 2.2), (2.1, 0.9)])
    f.fill(scoop, H.lin((0, 0), (0, 2.2), [(0, H.tone(SKIN[1], 0.86)), (1, H.tone(SKIN[0], 0.95))]))
    f.fill(f.m_polyline([(1.75, 0.0), (2.1, 0.9), (4.0, 2.2), (5.9, 0.9), (6.25, 0.0)], 0.2), TANK_D, 0.95)
    f.fill(f.m_polyline([(2.0, 0.0), (2.3, 0.85), (4.0, 2.0), (5.7, 0.85), (6.0, 0.0)], 0.07), "#ffffff", 0.55)
    f.fill(f.m_polyline([(2.6, 1.0), (4.0, 1.9), (5.4, 1.0)], 0.08), H.tone(SKIN[0], 0.7), 0.45)               # the collar bones
    # the black strap of the throat: a band with a stitch, a small steel ring in the middle
    f.fill(f.m_rect(1.8, 0.0, 6.2, 0.62), H.lin((0, 0), (0, 0.62), [(0, K.lit(STRAP, 1.6)), (1, H.tone(STRAP, 0.7))]))
    f.fill(f.m_rect(1.8, 0.56, 6.2, 0.66), "#07070a", 0.8)
    K.seam(f, [(1.9, 0.3), (6.1, 0.3)], "#6a6a78", 0.05, 0.5, (0.28, 0.18), 0.0)
    f.ellipse(4.0, 0.7, 0.34, 0.34, "#8f95a0")
    f.ellipse(4.0, 0.7, 0.18, 0.18, "#15151a")
    f.ellipse(3.9, 0.58, 0.1, 0.07, "#f4f6fa", a=0.8)
    # the hem
    f.fill(f.m_rect(0.0, 8.3, 8.0, 8.85), H.lin((0, 8.3), (0, 8.85), [(0, TANK), (1, TANK_D)]), 0.95)
    K.seam(f, [(0.1, 8.55), (7.9, 8.55)], "#aaa59a", 0.05, 0.55, (0.3, 0.2), 0.2)
    # the armholes: a wide cut, skin inside, the cloth edge folded
    for x0, x1, e in ((0.0, 0.8, 0.8), (7.2, 8.0, 7.2)):
        f.fill(f.m_rect(x0, 0.8, x1, 4.2), H.lin((0, 0.8), (0, 4.2), [(0, H.tone(SKIN[1], 0.9)), (1, SKIN[0])]))
        f.fill(f.m_line((e, 0.8), (e, 4.2), 0.2, 0.2), TANK_D, 0.95)


def belt(f, front, books=True):
    K.belt_band(f, 9.0, 10.05, BELT, stitch="#a07a58")
    K.belt_band(f, 10.05, 10.45, H.tone(BELT, 0.62), stitch_a=0.0)
    H.shadow_under(f, 10.45, 0.45, 0.3)
    if not front:
        return
    K.buckle(f, 4.0, 9.55, 2.5, 1.55, BUCKLE)
    # the books of the belt: three small hanging books, a red one, a brown one and a blue one, with page edges and a gold clasp
    for x0, x1, y1, c in ((0.3, 1.5, 11.7, "#8a2c2c"), (1.55, 2.5, 11.3, "#4a3322"), (6.2, 7.5, 11.8, "#2c3f7a")):
        f.fill(f.m_rect(x0, 10.0, x1, y1), H.lin((0, 10.0), (0, y1), [(0, K.lit(c, 1.3)), (1, H.tone(c, 0.7))]))
        f.fill(f.m_rect(x0, 10.0, x0 + 0.22, y1), H.tone(c, 0.55), 0.8)                       # the spine
        f.fill(f.m_rect(x1 - 0.18, 10.2, x1, y1 - 0.15), "#efe6cf", 0.9)                       # the pages
        f.fill(f.m_rect(x0 + 0.3, y1 - 0.5, x1 - 0.3, y1 - 0.38), BUCKLE, 0.8)                 # the clasp
        f.fill(f.m_rect(x0, y1 - 0.08, x1, y1), "#0a0808", 0.7)
        H.shadow_under(f, y1, 0.3, 0.25, x0=x0, x1=x1)


def waist(f, front):
    f.fill(f.m_rect(0.0, 10.45, f.w, 12.0), H.lin((0, 10.45), (0, 12.0), [(0, H.tone(TROUS, 0.85)), (1, H.tone(TROUS, 0.55))]))
    if front:
        f.fill(f.m_line((4.0, 10.6), (4.0, 12.0), 0.1, 0.1), H.tone(TROUS, 0.4), 0.8)


def body_front(f, ov):
    if not ov:
        tank_front(f)
        waist(f, True)
    belt(f, True)


def body_back(f, ov):
    if not ov:
        m = f.m_all()
        K.cloth(f, m, TANK, top=1.04, bottom=0.78, round_=0.1, fold_xs=[1.7, 6.3], fold_depth=0.07, fold_y0=1.5, fold_y1=8.8)
        f.shade(m, 1.0 + 0.1 * K.blob(f, 2.2, 3.2, 1.7, 1.9) + 0.1 * K.blob(f, 5.8, 3.2, 1.7, 1.9), 0.9)       # the shoulder blades
        f.fill(f.m_line((4.0, 1.0), (4.0, 8.6), 0.15, 0.1), TANK_D, 0.4)
        f.fill(f.m_poly([(1.75, 0.0), (6.25, 0.0), (5.9, 0.7), (4.0, 1.1), (2.1, 0.7)]), H.lin((0, 0), (0, 1.1), [(0, H.tone(SKIN[1], 0.86)), (1, H.tone(SKIN[0], 0.95))]))
        f.fill(f.m_polyline([(1.75, 0.0), (2.1, 0.7), (4.0, 1.1), (5.9, 0.7), (6.25, 0.0)], 0.2), TANK_D, 0.95)
        f.fill(f.m_rect(1.8, 0.0, 6.2, 0.55), H.lin((0, 0), (0, 0.55), [(0, K.lit(STRAP, 1.6)), (1, H.tone(STRAP, 0.7))]))
        f.fill(f.m_rect(0.0, 8.3, 8.0, 8.85), H.lin((0, 8.3), (0, 8.85), [(0, TANK), (1, TANK_D)]), 0.95)
        K.seam(f, [(0.1, 8.55), (7.9, 8.55)], "#aaa59a", 0.05, 0.55, (0.3, 0.2), 0.2)
        for x0, x1, e in ((0.0, 0.8, 0.8), (7.2, 8.0, 7.2)):
            f.fill(f.m_rect(x0, 0.8, x1, 4.2), H.lin((0, 0.8), (0, 4.2), [(0, H.tone(SKIN[1], 0.9)), (1, SKIN[0])]))
            f.fill(f.m_line((e, 0.8), (e, 4.2), 0.2, 0.2), TANK_D, 0.95)
        waist(f, False)
    belt(f, False)


def body_side(f, ov, name):
    if not ov:
        K.cloth(f, f.m_all(), TANK, top=1.04, bottom=0.78, round_=0.12, fold_xs=[1.2, 2.6], fold_depth=0.07, fold_y0=1.5, fold_y1=8.8)
        x0, x1 = (0.0, 1.3) if name == "east" else (f.w - 1.3, f.w)
        f.fill(f.m_rect(x0, 0.0, x1, 4.2), H.lin((0, 0.0), (0, 4.2), [(0, H.tone(SKIN[1], 0.9)), (1, SKIN[0])]))
        f.fill(f.m_line((x1 if name == "east" else x0, 0.0), (x1 if name == "east" else x0, 4.2), 0.2, 0.2), TANK_D, 0.95)
        f.fill(f.m_rect(0.0, 8.3, f.w, 8.85), H.lin((0, 8.3), (0, 8.85), [(0, TANK), (1, TANK_D)]), 0.95)
        waist(f, False)
    belt(f, False)


def body(cv):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, ov)
            elif nm == "back":
                body_back(f, ov)
            else:
                body_side(f, ov, nm)


# ================================================================================================ the arms (bare, bandaged forearms)
def bandage(f, y0, y1):
    """A wrapped bandage from y0 to y1: strips at a slant with a dark gap and a lit edge, a thicker rim at the top, a frayed end at the bottom, a shadow under the rim."""
    m = f.m_rect(-0.1, y0, f.w + 0.1, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(BAND, 1.1)), (0.5, BAND), (1, H.tone(BAND, 0.82))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.16 * (1.0 - H.smoothstep(0.0, 1.0, side)), 0.9)
    y = y0 + 0.5
    while y < y1 - 0.3:
        f.fill(f.m_line((-0.1, y + 0.32), (f.w + 0.1, y - 0.12), 0.1, 0.08) * m, BAND_D, 0.6)
        f.fill(f.m_line((-0.1, y + 0.2), (f.w + 0.1, y - 0.24), 0.07, 0.05) * m, "#fffdf2", 0.5)
        y += 0.78
    f.fill(f.m_rect(-0.1, y0, f.w + 0.1, y0 + 0.2), BAND_D, 0.85)
    H.shadow_under(f, y1, 0.3, 0.28)
    for i in range(7):                                                                         # a few loose threads at the wrist
        x = f.w * (0.1 + 0.13 * i)
        f.fill(f.m_line((x, y1 - 0.15), (x + 0.1, y1 + 0.2), 0.08, 0.03), BAND, 0.8)


def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            K.bare_arm(f, SKIN, nm, 0.0, 12.0, power=1.0)
            f.fill(f.m_band(0.0, 0.9), H.tone(SKIN[0], 0.88), 0.4)
            bandage(f, 6.3, 10.6)


# ================================================================================================ the legs (black trousers, beige chaps, black boots)
def chap_panel(f, x0, x1, y0, y1, edge_left):
    """A leather chap panel: a gradient, a shaded edge towards the trousers, a stitched border, rivets, a thigh strap with a buckle, wrinkles at the knee."""
    m = f.m_rect(x0, y0, x1, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(CHAP, 1.15)), (0.5, CHAP), (1, H.tone(CHAP, 0.82))]))
    f.shade(m, 1.0 + 0.12 * K.blob(f, (x0 + x1) / 2.0, 1.6, (x1 - x0) * 0.7, 1.8), 0.9)
    f.shade(m, 1.0 - 0.1 * K.blob(f, (x0 + x1) / 2.0, 3.8, (x1 - x0) * 0.8, 0.6, 1.0), 0.9)
    for y, k in ((3.3, 0.5), (3.75, 0.4)):                                                   # the knee wrinkles
        K.crease(f, [(x0 + 0.05, y), ((x0 + x1) / 2.0, y + 0.22), (x1 - 0.05, y + 0.04)], CHAP, 0.09, k, light=1.4, mask=m)
    edge_x = x0 if edge_left else x1
    f.fill(f.m_rect(x0, y0, x1, y1) * (np.abs(f.X - edge_x) < 0.28), CHAP_D, 0.55)           # the inner edge falls into shadow
    inner = x0 + 0.34 if edge_left else x1 - 0.34
    K.seam(f, [(inner, y0 + 0.25), (inner, y1 - 0.15)], "#f4e6c0", 0.05, 0.75, (0.28, 0.18), 0.25)
    for y in np.arange(y0 + 1.0, y1 - 0.5, 1.15):
        H.stud(f, inner + (0.0 if edge_left else 0.0), y, 0.13, "#e6c878", spec=0.5)
    f.fill(f.m_rect(x0, y1 - 0.14, x1, y1), CHAP_D, 0.7)
    # the thigh strap with a small buckle
    f.fill(f.m_rect(x0, 1.8, x1, 2.25), H.lin((0, 1.8), (0, 2.25), [(0, K.lit(BELT, 1.6)), (1, H.tone(BELT, 0.7))]))
    f.fill(f.m_rect(x0, 2.2, x1, 2.28), "#0a0806", 0.7)
    H.stud(f, (x0 + x1) / 2.0, 2.02, 0.2, BUCKLE, spec=0.7)
    return m


def leg(f, ov, name, part):
    right = part == "leg_r"
    K.trousers(f, TROUS, 0.0, 6.3, name, knee=True, fold_xs=(1.2, 2.8), seam_color="#6a6a7c", top=1.35, bottom=0.8, knee_y=(3.3, 3.95))
    # which part of this face is covered by the chap (the outer half of the front, a narrow strip of the back, the whole outer side)
    outer_side = (name == "west") if right else (name == "east")
    if name == "front":
        chap_panel(f, 0.0, 2.0, 0.5, 6.3, True) if right else chap_panel(f, 2.0, 4.0, 0.5, 6.3, False)
    elif name == "back":
        chap_panel(f, 3.2, 4.0, 0.5, 6.3, True) if right else chap_panel(f, 0.0, 0.8, 0.5, 6.3, False)
    elif outer_side:
        chap_panel(f, 0.0, f.w, 0.5, 6.3, True)
    H.shadow_under(f, 6.2, 0.7, 0.34)
    # the boots: black calf boots with a folded rim, a strap and a steel buckle
    cuff = f.m_rect(0.0, 6.35, f.w, 7.2)
    f.fill(cuff, H.lin((0, 6.35), (0, 7.2), [(0, K.lit(BOOT, 2.4)), (1, K.lit(BOOT, 1.2))]))
    f.fill(f.m_rect(0.0, 6.35, f.w, 6.47), "#050507", 0.7)
    K.seam(f, [(0.12, 6.85), (f.w - 0.12, 6.85)], "#6a6a78", 0.05, 0.6, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 7.2, 11.25, name, strap="#2e2c34", gold="#aeb4c0", welt_k=2.2)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                leg(f, ov, nm, part)


# ================================================================================================ paint
def paint(spec, cid, masks):
    spec["hair"]["color"] = HAIR
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=HAIR_STYLE)
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, HAIR_RGB, skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    covers = K.all_covers(cv)
    head_front(cv)
    K.head_rear(cv, K.dark_hair_palette(HAIR), back_y=5.6, nape_y=6.8, side_top=2.6, ear_x=4.2, side_low=5.4)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim
