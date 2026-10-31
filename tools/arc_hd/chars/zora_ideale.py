"""Zora Ideale, the rude honest prankster of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc3.md): the hand-painted look of the red-haired trap setter.

What he wears is the 64x64 sheet's outfit (tools/arc/char_zora_ideale.py): a dark sleeveless tank over bare arms, belts round the neck, wrapped forearms, a double belt with a steel buckle, plain dark trousers
in calf boots, steel ear cuffs and a stud above the right brow. The sheet builds the leather straps wound round his lower face as a 3D extra in `extra()`, but the packed model (`pmx_zora_ideale`) holds only the
cloak (`cape`, `cape_low`), so here the straps are painted: two leather bands round the jaw (one under the nose, one under the mouth) joined by a strap on each cheek, on the base layer AND on the second
layer of the head, so they stand off the face. The black cloak with the ash-white bull is the extra, so the back of the tank is plain. The 3D hair is `anime_spiky` (thick red clumps standing up in a ragged crest).

Drawn at 4x detail on both layers: sharp blue anime eyes with a smirk, thin angled brows, a stud above the right brow, a ragged red fringe of tapered clumps, leather face straps with rivets and a buckle, steel ear
cuffs, a ribbed knit tank with frayed hem, two buckled belts round the neck (second layer), bare arms with tendons, the black forearm wraps with buckled straps, a double belt with a steel buckle, trousers with
thigh straps, knee patches and a pocket, and calf boots with buckled straps, steel toe caps, welts and soles.

Deliberate exception to the 64x64 silhouette (docs/arc/hd_arc_bc3.md): the second layer of the body is grown by the rows of the neck belts (y 0 to 1.8; the sheet has only the double belt there).

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus the header of
tools/arc/char_zora_ideale.py (which cites the wikis): a thin man with messy red hair, blue eyes and pale skin, leather straps round the lower face and the neck, metal studs and ear cuffs, a dark sleeveless
shirt and plain dark trousers under the Black Bull cloak.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc3_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_spiky"
HAIR = "#d02e1a"
HAIR_RGB = (208, 46, 26)
SKIN = ("#d9a99c", "#f2d2c4", "#fff0e8")
TANK, TANK_L = "#34343f", "#5a5a6e"
TROUS, TROUS_L = "#3b3b47", "#5e5e72"
LEATHER = "#221d22"
STEEL, STEEL_D = "#b6b8c8", "#6a6c80"
BELT = "#2e241f"
BOOT = "#2c2c36"
WRAP = "#2a2a32"
IRIS = ("#0e2a66", "#3f78d0", "#a8d4ff")


# ================================================================================================ the head
def fringe(f, pal):
    """A ragged red fringe: a mass at the crown and five thick pointed clumps over the brow that lean in different directions (tapered, never single strands)."""
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.4, 1.8), (6.4, 2.2), (5.2, 1.6), (4.0, 2.1), (2.8, 1.6), (1.6, 2.1), (0.6, 1.7), (-0.3, 2.2)]
    K.hair_mass(f, pal, outline, 0.0, 2.3, shadow_k=0.88, shadow_depth=1.5, streaks=(0.9, 2.1, 3.3, 4.5, 5.7, 6.9))
    for sh, (cx, w, top, ln, lean) in ((0.9, (6.9, 1.6, 1.1, 2.0, -0.5)), (1.0, (5.2, 1.4, 1.2, 2.2, 0.4)), (0.85, (3.7, 1.5, 1.2, 1.7, -0.3)),
                                        (1.0, (2.2, 1.4, 1.1, 2.3, 0.5)), (0.9, (0.9, 1.5, 1.0, 2.6, 0.3))):
        K.spike(f, pal, cx, w, top, ln, lean, sh)
    for x, ln, lean in ((0.4, 3.8, 0.1), (7.6, 3.6, -0.1)):                                      # the sideburns
        K.lock(f, pal, x, 0.9, 1.0, ln, lean, 1.0)


def face_straps(f, ov):
    """The leather straps of the lower face: a ring under the nose (y 5.8..6.3) and one round the chin (y 7.3..7.95), joined by a strap down each cheek (x 0.35..1.25 and 6.75..7.65)."""
    ring_a, ring_b = f.m_rect(-0.1, 5.85, f.w + 0.1, 6.3), f.m_rect(-0.1, 7.35, f.w + 0.1, 7.9)
    cheeks = H.union(f.m_rect(0.45, 5.85, 1.15, 7.9), f.m_rect(6.85, 5.85, 7.55, 7.9))
    allm = H.union(ring_a, ring_b, cheeks)
    K.leather_band(f, allm, LEATHER)
    for y0, y1 in ((5.85, 6.3), (7.35, 7.9)):
        f.fill(f.m_rect(-0.1, y0, f.w + 0.1, y0 + 0.1), K.lit(LEATHER, 3.0), 0.55)
    for x in (0.8, 7.2):
        f.fill(f.m_line((x, 6.3), (x, 7.3), 0.12, 0.12), K.lit(LEATHER, 2.4), 0.5)          # a light edge on each cheek strap
        H.stud(f, x, 6.05, 0.2, STEEL, spec=0.8)                                              # rivets where the straps cross the rings
        H.stud(f, x, 7.62, 0.2, STEEL, spec=0.8)
    # a small buckle on the ring under the nose, at the right of the picture
    f.fill(f.m_rect(4.6, 5.78, 5.5, 6.32, 0.08), STEEL_D)
    f.fill(f.m_rect(4.72, 5.9, 5.38, 6.2, 0.05), K.lit(STEEL, 1.1))
    f.fill(f.m_line((4.9, 6.05), (5.3, 6.05), 0.1, 0.1), "#14141c")
    return allm


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.10, chin=0.08, jaw=0.08)
    pal = K.hair_palette(HAIR)
    fringe(f, pal)
    H.brow(f, (3.5, 3.5), (1.0, 3.0), "#7a2a20", 0.28, 0.10, arch=0.02)                      # thin, angled: a cocky look
    H.brow(f, (4.5, 3.5), (7.0, 3.0), "#7a2a20", 0.28, 0.10, arch=0.02)
    K.eye_pair(f, IRIS, cx=(2.4, 5.6), cy=4.75, w=2.5, h=1.85, lash="#150e12", tilt=0.34, lash_w=0.46, flick=0.42, lid_shadow=0.4, iris_r=1.04, hi=0.95, pupil="#06122e")
    H.nose(f, 4.0, 5.35, "#d09a8a", 0.18)
    H.mouth_smile(f, 4.0, 6.75, 3.0, color="#8a3a3a", lip="#d88a84", depth=0.2, thick=0.2, teeth="#fffdf6", open_h=0.28)   # a smirk with a flash of teeth
    f.poly([(3.0, 6.9), (3.35, 6.9), (3.17, 7.2)], "#ffffff", 0.95)
    for ov in (False, True):
        g = cv.face("head", "front", ov)
        face_straps(g, ov)
        H.stud(g, 1.7, 3.0, 0.2, STEEL, spec=0.9)                                             # the stud above the right brow (the viewer's left)
    ov = cv.face("head", "front", True)
    ov.fill(1.0 - H.union(ov.m_rect(-0.1, 5.85, ov.w + 0.1, 6.3), ov.m_rect(-0.1, 7.35, ov.w + 0.1, 7.9), ov.m_rect(0.45, 5.85, 1.15, 7.9), ov.m_rect(6.85, 5.85, 7.55, 7.9), ov.m_ellipse(1.7, 3.0, 0.3, 0.3)), "#000000", mode="erase")


def ear_cuffs(f, nm):
    """Steel cuffs along the rim of the ear: two small rings with a bevel."""
    cx = 3.9 if nm == "west" else 4.1
    for dy in (-0.5, 0.2):
        f.fill(f.m_ring(cx + (0.25 if nm == "west" else -0.25), 4.6 + dy + 0.4, 0.42, 0.18), H.metal_paint(4.0, 5.4, STEEL, 1.0))
    H.stud(f, cx, 6.0, 0.2, STEEL, spec=0.8)


def head_rest(cv):
    pal = K.hair_palette(HAIR)
    K.head_rear(cv, pal, back_y=5.6, nape_y=6.6, side_top=3.0, ear_x=4.1, side_low=5.4)
    for nm in ("west", "east"):
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            if not ov:
                K.ear_face(f, SKIN, nm, 4.5, 1.7)
            ear_cuffs(f, nm)
    for nm in ("west", "east", "back"):                                                      # the strap rings go all the way round the head
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            ring = H.union(f.m_rect(-0.1, 5.85, f.w + 0.1, 6.3), f.m_rect(-0.1, 7.35, f.w + 0.1, 7.9))
            if ov:
                f.clear()
            K.leather_band(f, ring, LEATHER)
            f.fill(f.m_rect(-0.1, 5.85, f.w + 0.1, 5.95), K.lit(LEATHER, 3.0), 0.5)
            f.fill(f.m_rect(-0.1, 7.35, f.w + 0.1, 7.45), K.lit(LEATHER, 3.0), 0.5)
    for nm in ("west", "east"):                                                              # the cheek strap shows on the front edge of each side
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            fx = f.w - 0.9 if nm == "west" else 0.0
            K.leather_band(f, f.m_rect(fx, 5.85, fx + 0.9, 7.9), LEATHER)


# ================================================================================================ the body
def neck_belts(f, front):
    """Two leather belts round the neck (y 0..0.85 and 0.95..1.75) with a steel buckle on the front and a loose end."""
    for i, (y0, y1) in enumerate(((0.0, 0.85), (0.95, 1.75))):
        m = f.m_rect(-0.1, y0, f.w + 0.1, y1)
        K.leather_band(f, m, LEATHER)
        f.fill(f.m_rect(-0.1, y0, f.w + 0.1, y0 + 0.1), K.lit(LEATHER, 3.0), 0.5)
        if f.w >= 6:
            K.seam(f, [(0.2, (y0 + y1) / 2.0 + 0.05), (f.w - 0.2, (y0 + y1) / 2.0 + 0.05)], "#6a5a52", 0.05, 0.5, (0.3, 0.2), 0.2)
    H.shadow_under(f, 1.75, 0.45, 0.4)
    if front and f.w >= 6:
        f.fill(f.m_rect(2.45, 0.9, 3.75, 1.8, 0.1), STEEL_D)
        f.fill(f.m_rect(2.57, 1.0, 3.63, 1.7, 0.08), H.metal_paint(1.0, 1.7, STEEL, 1.1))
        f.fill(f.m_rect(2.85, 1.18, 3.35, 1.52, 0.05), "#14141c")
        f.fill(f.m_line((2.9, 1.35), (3.9, 1.35), 0.12, 0.1), STEEL)


def tank(f, front):
    m = f.m_rect(0.0, 1.0, f.w, 9.2)
    K.cloth(f, m, TANK, top=1.5, bottom=0.78, round_=0.08, fold_xs=[1.6, 6.4] if f.w >= 6 else [1.2, 2.8], fold_depth=0.07, fold_y0=1.8, fold_y1=8.6)
    H.sheen(f, m, [1.8, 6.0] if f.w >= 6 else [1.4], width=0.3, strength=0.2, tilt=0.03, y0=1.8, y1=8.6, color="#9a9ab4")
    # a fine rib: vertical light lines every 0.5 px
    for i in range(int(f.w * 2)):
        x = 0.25 + i * 0.5
        f.fill(f.m_line((x, 1.9), (x, 8.8), 0.12, 0.12) * m, TANK_L, 0.3)
    for i in range(6):                                                                              # worn patches: a lighter, washed-out cloth here and there
        cx, cy = f.w * H.hsh(i, 3, 5), 2.6 + 5.4 * H.hsh(i, 4, 5)
        f.fill(f.m_ellipse(cx, cy, 0.9 + 0.5 * H.hsh(i, 5, 5), 0.6 + 0.5 * H.hsh(i, 6, 5)) * m, TANK_L, 0.2)
    if front and f.w >= 6:
        f.fill(f.m_polyline([(5.3, 3.4), (5.7, 4.2), (5.4, 4.9), (5.9, 5.6)], 0.13) * m, "#0a0a0e", 0.85)   # a rip across the chest
        f.fill(f.m_polyline([(5.4, 3.4), (5.8, 4.2), (5.5, 4.9), (6.0, 5.6)], 0.07) * m, SKIN[1], 0.5)
        f.fill(f.m_poly([(2.2, 1.7), (5.8, 1.7), (4.0, 2.7)]), H.tone(TANK, 0.6), 0.9)             # the neckline of the tank seen under the belts
        f.fill(f.m_line((4.0, 3.0), (4.0, 8.8), 0.14, 0.14), H.tone(TANK, 0.5), 0.45)
        for i, x in enumerate((1.2, 2.4, 5.6, 6.8)):                                               # tears at the hem
            f.fill(f.m_line((x, 8.2), (x + 0.12, 8.95), 0.14, 0.1), "#0c0c10", 0.8)
    # the frayed hem above the belt
    for i in range(int(f.w * 1.5)):
        x = 0.2 + i * 0.7
        f.fill(f.m_line((x, 8.7), (x + 0.15 * (i % 2 * 2 - 1), 9.25), 0.12, 0.05) * f.m_rect(0, 8.6, f.w, 9.4), K.lit(TANK, 1.6), 0.5)


def belts(f, front):
    K.belt_band(f, 9.1, 10.05, BELT, stitch="#8a6a50")
    K.belt_band(f, 10.05, 10.75, H.tone(BELT, 0.75), stitch="#6a5040", stitch_a=0.5)
    H.shadow_under(f, 10.75, 0.4, 0.3)
    if front and f.w >= 6:
        K.buckle(f, 4.0, 9.85, 2.2, 1.55, STEEL, opening="#101018")
        for x in (1.2, 6.8):
            H.stud(f, x, 9.55, 0.17, STEEL, spec=0.8)
    f.fill(f.m_rect(0.0, 10.75, f.w, 12.0), H.lin((0, 10.75), (0, 12.0), [(0, H.tone(TROUS, 0.8)), (1, H.tone(TROUS, 0.55))]))
    if f.w >= 6:
        f.fill(f.m_line((4.0, 10.8), (4.0, 12.0), 0.12, 0.12), H.tone(TROUS, 0.45), 0.7)


def body(cv):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            tank(f, nm == "front")
            neck_belts(f, nm == "front")
            belts(f, nm == "front")
            if nm in ("west", "east"):
                K.seam(f, [(1.5 if nm == "east" else 2.5, 2.2), (1.5 if nm == "east" else 2.5, 8.8)], "#4a4a5a", 0.05, 0.5)


# ================================================================================================ the arms: bare shoulders, black wraps, bare hands
def wrap(f, y0, y1):
    """A black leather wrap from y0 to y1: slanted turns with a lit edge and a dark gap, a buckled strap at the top and the wrist."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(WRAP, 1.5)), (0.5, WRAP), (1, H.tone(WRAP, 0.6))]))
    per = 1.1
    ph = ((f.Y - 0.5 * (f.X - f.w / 2.0) - y0) % per) / per
    f.shade(m * np.clip((0.3 - ph * per) / 0.12, 0, 1), 0.55, 0.95)
    f.fill(m * np.clip(1.0 - np.abs(ph - 0.5) / 0.16, 0, 1), TANK_L, 0.4)
    for yy in (y0 + 0.4, y1 - 0.9):
        f.fill(f.m_rect(0.0, yy, f.w, yy + 0.5), K.lit(LEATHER, 1.4))
        f.fill(f.m_rect(0.0, yy, f.w, yy + 0.1), K.lit(LEATHER, 3.0), 0.5)
        if f.name in ("front", "west", "east"):
            H.stud(f, f.w * 0.5, yy + 0.25, 0.17, STEEL, spec=0.8)
    H.shadow_under(f, y0, 0.5, 0.3)
    f.fill(f.m_band(y1 - 0.2, y1), "#0c0c10", 0.5)


def arm(cv, part):
    for nm in SIDES:
        base, ov = A.lay(cv, part, nm)
        K.bare_arm(base, SKIN, nm, 0.0, 12.0, 1.0, fist=True)
        ov.clear()
        for f in (base, ov):
            wrap(f, 7.0, 11.0)
            if f is base:
                K.fingers(f, SKIN, 11.1, 12.0, n=2)
            else:
                f.fill(1.0 - f.m_band(7.0, 11.0), "#000000", mode="erase")


# ================================================================================================ the legs: dark trousers, buckled boots
def leg(f, ov, name):
    K.trousers(f, TROUS, 0.0, 7.0, name, knee=False, fold_xs=(f.w * 0.5,), top=1.4, bottom=0.82, seam_color="#6a6a82")
    f.shade(f.m_band(0.0, 7.0), 1.0 + 0.12 * K.blob(f, f.w * 0.45, 2.0, f.w * 0.7, 2.2), 0.9)
    # a thigh strap with a buckle, a pocket flap on the outer side and a knee patch
    sl = f.m_rect(-0.1, 2.5, f.w + 0.1, 3.2)
    K.leather_band(f, sl, "#2a221e", 1.6)
    f.fill(f.m_rect(-0.1, 2.5, f.w + 0.1, 2.6), K.lit("#2a221e", 2.6), 0.5)
    H.stud(f, f.w * 0.5, 2.85, 0.2, STEEL, spec=0.8)
    if name in ("west", "east", "front"):
        patch = f.m_rect(0.5, 4.2, f.w - 0.5, 5.6, 0.2)
        f.fill(patch, H.tone(TROUS, 0.74), 0.95)
        K.edge_line(f, patch, H.tone(TROUS, 0.4), 0.8, 1)
        H.bevel(f, patch, depth=0.2, gain=1.0, spec=0.2)
    H.shadow_under(f, 6.9, 0.7, 0.38)
    cuff = f.m_rect(0.0, 6.8, f.w, 7.9)                                                      # the folded boot top
    f.fill(cuff, H.lin((0, 6.8), (0, 7.9), [(0, K.lit("#2c2c36", 1.8)), (1, H.tone("#2c2c36", 0.7))]))
    f.fill(f.m_rect(0.0, 6.8, f.w, 6.92), "#08080c", 0.6)
    K.seam(f, [(0.12, 7.35), (f.w - 0.12, 7.35)], "#8a8aa2", 0.055, 0.65, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 7.9, 11.25, name, strap="#2c2c36", gold=STEEL)
    if name == "front":
        for y in (9.0,):                                                                       # a second strap and the steel toe cap
            f.rect(0.0, y, f.w, y + 0.6, "#2c2c36")
            f.fill(f.m_rect(0.0, y, f.w, y + 0.1), K.lit("#2c2c36", 2.4), 0.6)
            H.stud(f, f.w * 0.5, y + 0.3, 0.2, STEEL, spec=0.8)
        toe = f.m_ellipse(f.w * 0.5, 10.9, f.w * 0.42, 0.62)
        f.fill(toe * f.m_rect(0, 10.1, f.w, 11.3), H.metal_paint(10.2, 11.3, STEEL, 0.9), 0.95)
        H.bevel(f, toe * f.m_rect(0, 10.1, f.w, 11.3), depth=0.2, gain=1.0, spec=0.5)


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
    K.grow_cover(cv, covers, "body", 0.0, 1.8)
    head_front(cv)
    head_rest(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim
