"""Secre Swallowtail, the deadpan keeper of seals, HD 256x256 (docs/arc/hd_arc_bc3.md): the hand-painted look of the small, tired witch of the Black Bulls.

What she wears is the 64x64 sheet's outfit (tools/arc/char_secre_swallowtail.py): a black dress with thin straps and a short ruffled, feather-like hem, bare arms and legs, small black slippers, and the grey
cord that holds the Black Bull cloak round her neck. The cloak (`cape`, `cape_low`) and the floating cyan grimoire (`prop_float`) are 3D extras (`pmx_secre_swallowtail`), so the back of the dress is kept plain.
The 3D hair is the classic `bob` (the black bob: no anime cut is a short blunt bob).

Drawn at 4x detail on both layers: flat, half-closed, slightly drooping dark eyes with heavy lashes and the dark eye bags under them, straight flat brows, a tiny neutral mouth, a blunt black fringe with
side locks, the dress with a sweetheart neckline, silver piping, a lacing of grey cord down the bodice, the thin shoulder straps, the three tiers of ruffles with feather-like ragged tips, the grey neck cord with
a knot whose small gem glows cyan (the colour of her seals; glow map), and the slippers with an ankle strap and a silver buckle.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus the header of
tools/arc/char_secre_swallowtail.py (which cites the wikis): short black bob, a pale small body with dark under-eye bags and flat slanted eyes, a black dress with a ruffled hem, black slippers, a grey cord.
She does not wear the horns of her forbidden magic here.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc3_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "bob"
HAIR = "#26263a"
HAIR_RGB = (38, 38, 58)
SKIN = ("#e2bdbd", "#f4dcd6", "#fff0ea")
DRESS, DRESS_L = "#1b1b24", "#3a3a4c"
TRIM = "#8c8ca0"
CORD, CORD_L = "#8a8a9c", "#c8c8d8"
CYAN, CYAN_L = "#5ad8ff", "#c8f4ff"
SHOE = "#17171e"
IRIS = ("#0c0c14", "#32324a", "#7a7a9c")


# ================================================================================================ the head
def fringe(f, pal):
    """A blunt bob fringe: a black mass down to y 2.7 with a slightly ragged lower edge, a few lighter strand lines and two side locks to the cheeks."""
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 3.0), (7.2, 2.75), (6.0, 2.9), (4.8, 2.65), (3.6, 2.85), (2.4, 2.7), (1.2, 2.9), (-0.3, 2.7)]
    K.hair_mass(f, pal, outline, 0.0, 3.0, shadow_k=0.86, shadow_depth=1.5, streaks=(0.8, 1.9, 3.0, 4.2, 5.4, 6.5, 7.3), streak_len=2.4)
    for x, ln, lean in ((0.55, 5.6, 0.1), (7.45, 5.6, -0.1)):
        K.lock(f, pal, x, 1.1, 1.8, ln, lean, 1.0)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.08, chin=0.07)
    pal = K.dark_hair_palette(HAIR)
    fringe(f, pal)
    for cx in (2.45, 5.55):
        K.eye_bags(f, cx, 5.65, 2.3, "#8a6a86", 0.34, 0.4)
    H.brow(f, (3.5, 3.95), (1.0, 3.9), "#1c1c26", 0.24, 0.10, arch=0.0)                       # flat brows
    H.brow(f, (4.5, 3.95), (7.0, 3.9), "#1c1c26", 0.24, 0.10, arch=0.0)
    K.eye_pair(f, IRIS, cx=(2.45, 5.55), cy=5.1, w=2.25, h=1.2, lash="#0a0a12", tilt=-0.1, lash_w=0.46, flick=0.26, lid_shadow=0.5, iris_r=0.95, hi=0.75, pupil="#050508")
    H.nose(f, 4.0, 6.35, "#d0a09a", 0.18)
    H.mouth_smile(f, 4.0, 7.15, 1.2, color="#a85a5e", lip="#e0a0a0", depth=0.0, thick=0.15)
    f.fill(f.m_ellipse(4.0, 7.4, 0.6, 0.12), "#d89494", 0.5)


def head_rest(cv):
    pal = K.dark_hair_palette(HAIR)
    K.head_rear(cv, pal, back_y=6.2, nape_y=7.0, side_top=3.0, ear_x=4.0, side_low=6.2)
    for nm in ("west", "east"):
        f = cv.face("head", nm, False)
        K.ear_face(f, SKIN, nm, 4.6, 1.5)


# ================================================================================================ the dress
def bodice(f, front):
    top = 1.7 if f.w >= 6 else 0.0
    skin_top = f.m_rect(0.0, 0.0, f.w, top)
    if front and f.w >= 6:
        f.fill(f.m_rect(0.0, 0.0, f.w, 3.3), H.lin((0, 0), (0, 3.3), [(0, SKIN[2]), (1, SKIN[1])]))
    m = f.m_all()
    dress = m if f.w < 6 else H.minus(m, f.m_rect(0.0, 0.0, f.w, 1.7))
    if front and f.w >= 6:                                                                   # the sweetheart neckline: two arcs over the bust, thin straps up to the shoulders
        dress = H.minus(m, H.union(f.m_poly([(1.9, -0.1), (6.1, -0.1), (6.1, 1.5), (4.0, 2.7), (1.9, 1.5)]), f.m_rect(0.0, 0.0, 1.05, 1.0), f.m_rect(6.95, 0.0, 8.0, 1.0)))
        straps = H.union(f.m_rect(1.0, -0.1, 1.9, 1.7), f.m_rect(6.1, -0.1, 7.0, 1.7))
        dress = H.union(dress, straps)
    K.cloth(f, dress, DRESS, top=1.6, bottom=0.78, round_=0.08, fold_xs=[1.6, 6.4], fold_depth=0.06, fold_y0=2.0, fold_y1=8.6)
    H.sheen(f, dress, [1.8, 6.2], width=0.3, strength=0.2, tilt=0.03, y0=2.0, y1=8.6, color="#8a8aa8")
    if front and f.w >= 6:
        arc = [(1.9, 1.5), (3.0, 2.35), (4.0, 2.7), (5.0, 2.35), (6.1, 1.5)]
        f.fill(f.m_polyline(arc, 0.34), H.tone(TRIM, 0.5), 0.9)
        f.fill(f.m_polyline(arc, 0.22), TRIM, 0.95)
        f.fill(f.m_polyline([(p[0], p[1] - 0.06) for p in arc], 0.08), "#e0e0f0", 0.7)
        # lacing of grey cord down the bodice
        for i, y in enumerate((3.6, 4.7, 5.8, 6.9)):
            f.fill(f.m_line((3.4, y), (4.6, y + 0.55), 0.15, 0.15), CORD, 0.9)
            f.fill(f.m_line((4.6, y), (3.4, y + 0.55), 0.15, 0.15), CORD, 0.9)
            H.stud(f, 3.35, y + 0.02, 0.1, "#b8b8c8", spec=0.6)
            H.stud(f, 4.65, y + 0.02, 0.1, "#b8b8c8", spec=0.6)
        f.fill(f.m_line((4.0, 3.4), (4.0, 7.6), 0.14, 0.14), H.tone(DRESS, 0.4), 0.55)
    elif not front:
        f.fill(f.m_line((4.0, 2.0), (4.0, 8.4), 0.16, 0.16), H.tone(DRESS, 0.45), 0.55)
    return dress


def cord(f, front):
    """The grey cord round the neck with a knot at the front; the small gem in the knot glows cyan."""
    if f.w < 6:
        return
    pts = [(1.6, 0.0), (2.6, 1.3), (4.0, 1.8), (5.4, 1.3), (6.4, 0.0)] if front else [(1.6, 0.0), (4.0, 0.9), (6.4, 0.0)]
    f.fill(f.m_polyline(pts, 0.4), H.tone(CORD, 0.45), 0.9)
    f.fill(f.m_polyline(pts, 0.28), H.lin((0, 0), (0, 2.0), [(0, CORD_L), (1, CORD)]))
    f.fill(f.m_polyline([(p[0] - 0.02, p[1] - 0.07) for p in pts], 0.08), "#f0f0fa", 0.6)
    if front:
        f.fill(f.m_line((4.0, 1.8), (3.5, 3.3), 0.24, 0.16), CORD, 1.0)                    # the two loose ends below the knot
        f.fill(f.m_line((4.0, 1.8), (4.6, 3.1), 0.24, 0.16), CORD, 1.0)
        f.fill(f.m_ellipse(4.0, 1.85, 0.4, 0.34), CORD_L)
        H.gem(f, 4.0, 1.88, 0.26, CYAN, setting="#6a6a80", facets=4, spec=0.9, glow=True)


def ruffles(f, y0, name):
    """Three tiers of ruffles from y0 down (the feather-like hem): each tier is a band of scallops with a pale lip and a dark gap, the lowest one ragged."""
    tiers = [(y0, y0 + 1.0), (y0 + 0.9, y0 + 1.95), (y0 + 1.8, 12.0)]
    for i, (a, b) in enumerate(tiers):
        if b - a < 0.5:
            continue
        K.ruffle(f, a, b, DRESS if i != 1 else H.tone(DRESS, 1.1), DRESS_L, "#050508", cell=1.35 if f.w >= 6 else 1.0, rows=1, seed=4 + i)
        f.fill(f.m_rect(0.0, a, f.w, a + 0.12), "#6a6a84", 0.6)
        H.shadow_under(f, b, 0.4, 0.3)
    f.fill(f.m_rect(0.0, 11.8, f.w, 12.0), "#050508", 0.0)


def body(cv):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            bodice(f, nm == "front")
            f.fill(f.m_rect(0.0, 8.3, f.w, 8.6), "#2a2a3a", 0.9)
            f.fill(f.m_rect(0.0, 8.3, f.w, 8.4), "#8a8aa4", 0.6)
            ruffles(f, 8.9, nm)
            cord(f, nm == "front")
            if nm in ("west", "east"):
                K.seam(f, [(1.5 if nm == "east" else 2.5, 2.0), (1.5 if nm == "east" else 2.5, 8.2)], "#52526a", 0.05, 0.4)
    # straps and neckline: the second layer is open over the bare chest
    ov = cv.face("body", "front", True)
    ov.clear(ov.m_poly([(1.9, -0.3), (6.1, -0.3), (6.1, 1.5), (4.0, 2.7), (1.9, 1.5)]))


# ================================================================================================ the arms and legs
def arm(cv, part):
    for nm in SIDES:
        base, ov = A.lay(cv, part, nm)
        K.bare_arm(base, SKIN, nm, 0.0, 12.0, 0.55, fist=True)
        ov.clear()


def leg(f, ov, name):
    f.fill(f.m_all(), H.lin((0, 0), (0, 12), [(0, SKIN[1]), (1, SKIN[0])]))
    K.bare_leg(f, SKIN, name, 0.0, 10.4, 6.0)
    # the lowest ruffle tier of the dress reaches over the thigh (the second layer of the sheet covers rows 0..3)
    f.fill(f.m_rect(0.0, -1.0, f.w, 4.0), H.lin((0, 0), (0, 4), [(0, DRESS), (1, H.tone(DRESS, 0.8))]))
    K.ruffle(f, 1.9, 4.0, DRESS, DRESS_L, "#050508", cell=1.35, rows=1, seed=9)
    f.fill(f.m_rect(0.0, 1.9, f.w, 2.02), "#6a6a84", 0.6)
    H.shadow_under(f, 4.0, 0.6, 0.38)
    # the slipper: a low black shoe with an ankle strap, a silver buckle and a sole
    sh = f.m_rect(0.0, 10.0, f.w, 12.0)
    f.fill(sh, H.lin((0, 10.0), (0, 12.0), [(0, K.lit(SHOE, 1.6)), (0.6, SHOE), (1, "#0a0a0e")]))
    f.fill(f.m_rect(0.0, 10.0, f.w, 10.12), "#4a4a5c", 0.7)
    H.shadow_under(f, 10.0, 0.5, 0.35)
    f.fill(f.m_rect(0.0, 10.55, f.w, 10.95), "#2a2a38")
    f.fill(f.m_rect(0.0, 10.55, f.w, 10.65), "#8a8aa4", 0.7)
    if name == "front":
        H.stud(f, f.w * 0.5, 10.75, 0.26, "#c8c8dc", spec=0.9)
        f.ellipse(f.w * 0.5, 11.2, 1.2, 0.3, K.lit(SHOE, 2.2), a=0.4, soft=2.0)
    f.fill(f.m_band(11.5, 12.0), "#06060a")


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                leg(f, ov, nm)


# ================================================================================================ paint
def paint(spec, cid, masks):
    spec["hair"]["color"] = HAIR
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=HAIR_STYLE)
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, HAIR_RGB, skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    K.clear_head_overlay(cv)
    covers = K.all_covers(cv, ("body", "arm_r", "arm_l", "leg_r", "leg_l"))
    head_front(cv)
    head_rest(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim
