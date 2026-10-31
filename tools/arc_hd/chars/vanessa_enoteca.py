"""Vanessa Enoteca, the lazy and wise witch of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc3.md): the hand-painted look of the tipsy, sultry sister of the Sanctuary Hub.

What she wears is the 64x64 sheet's outfit (tools/arc/char_vanessa_enoteca.py): a dark purple dress with a wine-red sash and hem, a short flared skirt, bare arms and shoulders, dark knee boots, gold
earrings and a red thread tied round the right forearm (her Red Thread of Fate, drawn as a red wrap that glows). The very short Black Bull cloak (`cape`, `cape_low`) is a 3D extra (`pmx_vanessa_enoteca`; its
red thread cord hangs from the right hand), so the back of the dress is kept plain. The 3D hair is `anime_long_open` (long pink hair that leaves the painted fringe and forehead visible; a saturated pink so the grey hair sheet does not turn it mauve).

Drawn at 4x detail on both layers: the half-lidded violet eyes with eyeshadow, a lazy lash line and a faint blush, arched pink brows, full red lips, a pink wavy fringe with a side parting and long cheek locks, gold
earrings with a drop, the V neckline with wine piping over bare collarbones, the bodice with princess seams, the wine satin sash with a gold ring buckle, the pleated flared skirt with its wine hem and gold dot,
the bare arms, the red thread wound round the right forearm with a knot and a loose end (it glows), and the knee boots with plum turned cuffs, straps and soles.

Deliberate exceptions to the 64x64 silhouette (documented in docs/arc/hd_arc_bc3.md): the second layer of both legs is grown by the rows of the turned boot cuff (the sheet has the boots on the base layer only), and the
right forearm gets a second-layer cord wrap (the thread stands off the arm).

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus the sheet's own notes
(docs/arc/art_kit.md and the header of tools/arc/char_vanessa_enoteca.py, which cite the wikis): a tall woman with long wavy pink hair, purple eyes with heavy lids, painted lips, a dark purple top and short
skirt with knee boots, gold earrings. The cut of her clothes stays modest on purpose, as in the sheet.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc3_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_long_open"
HAIR = "#f5559f"
HAIR_RGB = (245, 85, 159)
SKIN = ("#e6b69e", "#f6d2be", "#ffe8da")
DRESS, DRESS_L = "#34214f", "#5d3f8c"
WINE, WINE_L = "#a22f58", "#e0689a"
GOLD = "#e6b858"
BOOT = "#3a2a46"
PLUM, PLUM_L = "#6a3070", "#a860b0"
STRAP = "#6c4b7e"
RED, RED_L, RED_D = "#e0344e", "#ff8aa0", "#7a1228"
IRIS = ("#3a1c6a", "#8658d0", "#dcc0ff")
LIP = "#c8304c"


# ================================================================================================ the head
def fringe(f, pal):
    """A pink fringe with a side parting at x 3.4: the long side sweeps over the right temple, the short side curls to the left; both are thick tapered clumps."""
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.6), (7.4, 2.0), (6.2, 1.5), (4.8, 1.3), (3.4, 0.9), (2.2, 1.5), (1.0, 2.0), (-0.3, 2.6)]
    K.hair_mass(f, pal, outline, 0.0, 2.5, shadow_k=0.88, shadow_depth=1.6, streaks=(0.9, 2.1, 3.3, 4.7, 5.9, 7.1))
    for pts in (
        [(3.4, 0.8), (4.8, 1.0), (6.2, 1.6), (7.4, 2.6), (7.9, 4.0), (7.9, 5.6), (7.4, 4.4), (6.6, 3.2), (5.4, 2.4), (4.2, 1.9)],
        [(3.4, 0.8), (2.2, 1.1), (1.0, 1.9), (0.3, 3.2), (0.1, 5.0), (0.7, 4.2), (1.5, 3.2), (2.4, 2.4), (3.2, 1.9)],
    ):
        m = f.m_poly(pts)
        pal_l = H.lin((pts[0][0], 0.8), (pts[5][0], pts[5][1]), [(0, pal["l"]), (0.6, pal["m"]), (1, H.mix(pal["m"], pal["d"], 0.4))])
        f.fill(m, pal_l)
        f.fill(f.m_polyline(pts[1:6], 0.14) * m, pal["h"], 0.55)
        f.fill(f.m_polyline(pts[6:] + [pts[0]], 0.14) * m, pal["d"], 0.8)
    for x, ln, lean in ((0.4, 5.6, 0.2), (7.6, 5.8, -0.2)):
        K.lock(f, pal, x, 1.0, 1.2, ln, lean, 1.0)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.09, chin=0.07)
    pal = K.hair_palette(HAIR)
    fringe(f, pal)
    for cx in (2.4, 5.6):                                                                    # violet eyeshadow over the heavy lids
        K.eyeshadow(f, cx, 5.0, 2.7, "#b05ac0", 0.55, 0.7, 0.6)
    H.brow(f, (3.3, 3.8), (1.0, 3.35), "#a8487c", 0.26, 0.10, arch=0.14)
    H.brow(f, (4.7, 3.8), (7.0, 3.35), "#a8487c", 0.26, 0.10, arch=0.14)
    K.eye_pair(f, IRIS, cx=(2.4, 5.6), cy=5.1, w=2.6, h=1.65, lash="#241020", tilt=0.18, lash_w=0.5, flick=0.5, lid_shadow=0.5, iris_r=1.0, hi=0.9, pupil="#1c0a30")
    for cx in (2.4, 5.6):
        f.fill(f.m_polyline([(cx - 1.25, 4.35), (cx, 4.18), (cx + 1.25, 4.35)], 0.1), "#7a3aa0", 0.35)   # the crease of the heavy lid
    H.blush(f, 1.2, 6.3, 0.95, 0.5, "#f08aa0", 0.4)
    H.blush(f, 6.8, 6.3, 0.95, 0.5, "#f08aa0", 0.4)
    H.nose(f, 4.0, 6.15, "#cf9484", 0.22)
    H.mouth_smile(f, 4.0, 7.0, 2.3, color="#8a1a34", lip=LIP, depth=0.12, thick=0.34)
    f.fill(f.m_ellipse(4.0, 7.55, 0.8, 0.25), LIP, 0.9)                                      # the full lower lip, lit
    f.fill(f.m_ellipse(3.75, 7.48, 0.4, 0.1), "#ff9aa8", 0.7)


def earring(f, cx, y):
    """A gold earring: a stud on the lobe and a drop with a small violet gem."""
    H.stud(f, cx, y, 0.26, GOLD, spec=0.9)
    f.fill(f.m_line((cx, y + 0.2), (cx, y + 1.1), 0.16, 0.12), GOLD)
    H.gem(f, cx, y + 1.35, 0.28, "#9a5ad8", setting=GOLD, facets=4, spec=0.8, glow=False)


def head_rest(cv):
    pal = K.hair_palette(HAIR)
    K.head_rear(cv, pal, back_y=6.6, nape_y=7.6, side_top=3.0, ear_x=4.0, side_low=6.8, parting=True)
    for nm in ("west", "east"):
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            if not ov:
                K.ear_face(f, SKIN, nm, 4.5, 1.8)
            earring(f, 3.9 if nm == "west" else 4.1, 6.2)


# ================================================================================================ the body: bodice, sash, skirt
def neckline(f, front):
    """The V neckline (front) over bare skin: skin gradient, collarbone shadows, wine piping on both slanted edges."""
    tri = f.m_poly([(2.2, -0.2), (5.8, -0.2), (4.0, 3.9)])
    f.fill(tri, H.lin((0, 0), (0, 4.0), [(0, SKIN[2]), (0.5, SKIN[1]), (1, H.tone(SKIN[0], 0.95))]))
    f.shade(tri, 1.0 - 0.1 * (1.0 - H.smoothstep(0.0, 1.4, np.minimum(f.X - 2.2 + (f.Y * 0.5), 5.8 - (f.Y * 0.5) - f.X) + 0.6)), 0.8)
    f.fill(f.m_polyline([(2.6, 0.7), (3.4, 1.0), (4.0, 0.95)], 0.12), H.tone(SKIN[0], 0.75), 0.35)  # the collarbones
    f.fill(f.m_polyline([(5.4, 0.7), (4.6, 1.0), (4.0, 0.95)], 0.12), H.tone(SKIN[0], 0.75), 0.35)
    for sgn, x0 in ((-1.0, 2.2), (1.0, 5.8)):
        pts = [(x0, -0.1), (4.0, 3.9)]
        edge = f.m_polyline(pts, 0.42)
        f.fill(f.m_polyline(pts, 0.56), H.tone(WINE, 0.4), 0.9)
        f.fill(edge, H.lin((0, 0), (0, 3.9), [(0, H.tone(WINE, 1.35)), (1, WINE)]))
        f.fill(f.m_polyline([(x0 + sgn * -0.05, 0.1), (4.0 + sgn * 0.0, 3.7)], 0.1) * edge, WINE_L, 0.55)
    return tri


def bodice(f, front):
    m = f.m_all()
    K.cloth(f, m, DRESS, top=1.45, bottom=0.8, round_=0.08, fold_xs=[1.4, 6.6], fold_depth=0.07, fold_y0=1.5, fold_y1=8.8)
    H.sheen(f, m, [1.5, 6.4], width=0.3, strength=0.22, tilt=0.03, y0=1.0, y1=8.6, color="#a78ad8")
    # princess seams from the shoulders curving in to the waist, a stitch beside each
    for sgn, x in ((-1.0, 1.8), (1.0, 6.2)):
        pts = [(x, 0.2), (x + sgn * 0.0, 2.6), (x - sgn * 0.5, 4.8), (x - sgn * 0.8, 8.6)]
        f.fill(f.m_polyline(pts, 0.2), H.tone(DRESS, 0.4), 0.7)
        K.seam(f, [(p[0] + sgn * 0.0 + (0.28 if sgn < 0 else -0.28), p[1]) for p in pts], "#8e70c0", 0.05, 0.5)
    f.fill(f.m_line((4.0, 3.9), (4.0, 8.8), 0.16, 0.16), H.tone(DRESS, 0.45), 0.5)           # the centre seam under the V
    if front:
        neckline(f, True)


def sash(f, front):
    y0, y1 = 8.7, 10.5
    m = f.m_rect(0.0, y0, f.w, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(WINE, 1.45)), (0.4, WINE), (1, H.tone(WINE, 0.6))]))
    H.sheen(f, m, [f.w * 0.3, f.w * 0.7], width=0.3, strength=0.4, tilt=0.0, y0=y0 + 0.15, y1=y1 - 0.15, color="#ffc0d8")
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.14), K.lit(WINE, 2.0), 0.6)
    f.fill(f.m_rect(0.0, y1 - 0.12, f.w, y1), "#2a0a18", 0.7)
    H.shadow_under(f, y1, 0.5, 0.3)
    if front and f.w >= 6:                                                                   # a gold ring buckle with a violet gem
        f.fill(f.m_ring(4.0, 9.6, 0.95, 0.4), H.metal_paint(8.6, 10.6, GOLD, 1.1))
        f.fill(f.m_ring(4.0, 9.6, 0.95, 0.4) * f.m_rect(2.0, 8.6, 4.0, 9.6), "#fff0b8", 0.5)
        H.gem(f, 4.0, 9.6, 0.34, "#9a5ad8", setting=GOLD, facets=5, spec=0.8, glow=False)


def skirt(f, y0, y1, hem_y, name, hem=True):
    """A pleated flared skirt: pleat folds, light and shade bands, a wine hem band with a gold dot on the front."""
    m = f.m_rect(0.0, y0, f.w, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(DRESS, 0.82)), (0.5, DRESS), (1, K.lit(DRESS, 1.2))]))
    n = max(2, int(round(f.w / 1.35)))
    for i in range(1, n):
        x = f.w * i / n
        f.fill(f.m_line((x, y0), (x + (x - f.w / 2) * 0.05, y1), 0.2, 0.28) * m, H.tone(DRESS, 0.45), 0.55)
        f.fill(f.m_line((x + 0.24, y0), (x + 0.24 + (x - f.w / 2) * 0.05, y1), 0.12, 0.2) * m, DRESS_L, 0.45)
    if not hem:
        return
    hem = f.m_rect(0.0, hem_y, f.w, y1)
    f.fill(hem, H.lin((0, hem_y), (0, y1), [(0, K.lit(WINE, 1.3)), (1, H.tone(WINE, 0.7))]))
    f.fill(f.m_rect(0.0, hem_y, f.w, hem_y + 0.14), WINE_L, 0.6)
    K.seam(f, [(0.0, hem_y + 0.5), (f.w, hem_y + 0.5)], "#f0a0c0", 0.05, 0.55, (0.3, 0.2), 0.15)
    if name == "front" and f.w >= 4:
        H.stud(f, f.w * 0.5, (hem_y + y1) / 2.0, 0.22, GOLD, spec=0.9)
    H.shadow_under(f, hem_y, 0.4, 0.22)


def body(cv):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            bodice(f, nm == "front")
            sash(f, nm == "front")
            skirt(f, 10.5, 12.0, 11.35, nm, hem=False)                                       # the flare under the sash; the wine hem is on the legs
            if nm in ("west", "east"):
                K.seam(f, [(1.5 if nm == "east" else 2.5, 2.0), (1.5 if nm == "east" else 2.5, 8.6)], "#8e70c0", 0.05, 0.4)
    # the neckline is open on the second layer (the bare skin of the base layer shows through)
    ov = cv.face("body", "front", True)
    ov.clear(ov.m_poly([(2.2, -0.3), (5.8, -0.3), (4.0, 3.9)]))


# ================================================================================================ the arms: bare, the red thread on the right forearm
def thread(f, y0, y1, glow=True):
    """The Red Thread of Fate: a cord wound in three clear slanted turns round the forearm, each with a lit top edge and a dark underside, a knot and a loose end; it glows on the glow map."""
    m = f.m_band(y0, y1)
    f.fill(m, H.tone(RED_D, 0.55), 0.45)                                                         # the shadow of the arm between the turns
    n = 3
    step = (y1 - y0 - 0.5) / n
    for k in range(n):
        yy = y0 + 0.35 + k * step
        pts = [(-0.1, yy - 0.05), (f.w + 0.1, yy + 0.3)]
        body = f.m_polyline(pts, 0.6) * m
        f.fill(f.m_polyline([(-0.1, yy + 0.05), (f.w + 0.1, yy + 0.4)], 0.62) * m, RED_D, 0.85)   # the underside
        f.fill(body, RED, 0.98, glow=RED if glow else None)
        f.fill(f.m_polyline([(-0.1, yy - 0.2), (f.w + 0.1, yy + 0.15)], 0.16) * body, RED_L, 0.85)  # the lit top edge
    if f.name == "front":
        f.fill(f.m_ellipse(f.w * 0.55, y0 + 0.55, 0.5, 0.45), RED, 1.0, glow=RED if glow else None)      # the knot
        f.fill(f.m_ellipse(f.w * 0.5, y0 + 0.45, 0.22, 0.17), RED_L, 0.85)
        f.fill(f.m_polyline([(f.w * 0.55, y0 + 0.9), (f.w * 0.62, y0 + 1.8), (f.w * 0.5, y0 + 2.7)], 0.26), RED, 0.95, glow=RED if glow else None)


def arm(cv, part):
    for nm in SIDES:
        base, ov = A.lay(cv, part, nm)
        K.bare_arm(base, SKIN, nm, 0.0, 12.0, 0.9, fist=True)
        ov.clear()                                                                           # no second layer on the bare arms ...
        if part == "arm_r":
            thread(base, 7.2, 10.2)
            thread(ov, 7.2, 10.2)                                                            # ... except the cord of the thread, which stands off the right forearm
            ov.fill(1.0 - ov.m_band(7.2, 10.2), "#000000", mode="erase")


# ================================================================================================ the legs: skirt, bare thighs, knee boots with wine cuffs
def leg(f, ov, name):
    f.fill(f.m_all(), H.lin((0, 0), (0, 12), [(0, SKIN[1]), (1, SKIN[0])]))                    # the bare thigh under the skirt
    skirt(f, 0.0, 4.0, 3.1, name)
    H.shadow_under(f, 4.0, 0.5, 0.34)
    cuff = f.m_rect(0.0, 4.5, f.w, 5.75)                                                     # the turned cuff of the boot, wine red, with a gold stitch and a strap
    f.fill(cuff, H.lin((0, 4.5), (0, 5.75), [(0, K.lit(PLUM, 1.35)), (0.5, PLUM), (1, H.tone(PLUM, 0.55))]))
    f.fill(f.m_rect(0.0, 4.5, f.w, 4.63), "#1a0a20", 0.6)
    f.fill(f.m_rect(0.0, 4.63, f.w, 4.77), PLUM_L, 0.6)
    K.seam(f, [(0.12, 5.2), (f.w - 0.12, 5.2)], "#e8b8e8", 0.06, 0.65, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 5.75, 11.25, name, strap=STRAP, gold=GOLD)


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
    K.grow_cover(cv, covers, "leg_r", 4.0, 5.9)
    K.grow_cover(cv, covers, "leg_l", 4.0, 5.9)
    K.grow_cover(cv, covers, "arm_r", 7.2, 10.2)
    head_front(cv)
    head_rest(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim
