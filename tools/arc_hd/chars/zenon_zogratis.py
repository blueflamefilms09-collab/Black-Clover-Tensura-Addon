"""Zenon Zogratis, the youngest of the Dark Triad, HD 256x256 (docs/arc/hd_arc_bc5.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_zenon_zogratis.py): a maroon open vest with a stand collar and dark piping over a dark tank, bare arms with grey-white bandage wraps on the
hands, a double belt with a small steel buckle and two pouches, dark trousers and calf boots. No crown (the sheet is bareheaded). The horns and the insect wings of the second form are the
`pmx_zenon_zogratis_form2` extras and are not painted on the skin; the bone plates of the second form are the sheet's `armor` pieces drawn on the skin.
Drawn at 4x detail on both layers: a lean pale face with cold pale-grey eyes under flat brows, dark rings under them, a thin flat mouth, a vertical scar with cross bars through the right eye and the
brow and a short scar on the cheek; the vest with lit panels, dark piping and stitched patches over the dark tank with its chest shading; the belts with pouches and a small steel buckle; the lean arms
with veins and the bandages; the trousers with knee creases and the boots with a leather cuff.
Form 2 (`zenon_zogratis_form2.py`, the hybrid of the Gates, 100% Beelzebub) runs this same code with FORM2 = True: grey skin, hollow eyes that burn indigo, fangs, a ragged vest over a bone ribcage and
a spine, bone pauldrons and bracers with glowing indigo trim, glowing cracks and veins (the glow map exists only there); the hair is the bone-pale crest of the mohawk.

Source (rule 10): no wiki page could be fetched in this environment (English and Japanese fandom are blocked); the look is from memory of the series (a lean, pale, scarred young man in a dark vest)
plus docs/arc/remodel.md and the sheet. The cut and the colours are the sheet's choice.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

FORM2 = False
HAIR_STYLE = "anime_short"
HAIR = "#1a1618"
HAIR_RGB = (26, 22, 24)
VEST = "#8a2a38"
VEST_D = "#4a121e"
TANK = "#322a38"
TROUS = "#2e2630"
BOOT = "#241c24"
BELT = "#2e1c20"
STEEL = "#b8a888"
WRAP = "#d4ccbc"
BONE = "#dcd4b8"
BONE_D = "#8a8268"
INDIGO = "#6a78ff"
INDIGO_G = "#8a96ff"
IRIS = ("#5a6a7a", "#c0d0de", "#f2f8ff")


def skin_tones():
    if FORM2:
        return ("#a8a8b8", "#c4c4d0", "#dcdce4"), "#8a8a98"
    return K.skin3("pale")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    skin, crease = skin_tones()
    K.face_base(f, skin, side=0.1, chin=0.09)
    f.fill(f.m_ellipse(1.6, 6.4, 0.9, 0.9, soft=3.0), H.tone(skin[0], 0.9), 0.3)                         # hollow cheeks
    f.fill(f.m_ellipse(6.4, 6.4, 0.9, 0.9, soft=3.0), H.tone(skin[0], 0.9), 0.3)
    brow_c = "#14101a" if not FORM2 else "#2a2438"
    H.brow(f, (3.55, 3.95), (0.9, 3.7), brow_c, 0.3, 0.16, arch=0.0)
    H.brow(f, (4.45, 3.95), (7.1, 3.7), brow_c, 0.3, 0.16, arch=0.0)
    if FORM2:
        for cx in (2.4, 5.6):                                                                          # hollow sockets with a burning indigo point
            f.fill(f.m_ellipse(cx, 5.1, 1.25, 0.85, rot=0.0), "#08060e", 1.0)
            f.fill(f.m_ellipse(cx, 5.1, 1.25, 0.85, soft=3.0), "#1a1430", 0.4)
            f.fill(f.m_ellipse(cx, 5.12, 0.5, 0.42, soft=2.0), INDIGO, 1.0, glow=INDIGO_G)
            f.fill(f.m_ellipse(cx, 5.12, 0.22, 0.2), "#ffffff", 0.95, glow="#ffffff")
            f.fill(f.m_polyline([(cx - 1.3, 4.45), (cx, 4.3), (cx + 1.3, 4.5)], 0.18), "#08060e", 0.9)
    else:
        for cx in (2.4, 5.6):
            H.eye(f, cx, 5.2, 2.5, 1.4, IRIS, lash="#12101a", tilt=0.06, lash_w=0.44, flick=0.3, lid_shadow=0.55, hi=0.75, pupil="#10101a")
            f.fill(f.m_ellipse(cx, 6.0, 1.45, 0.42, soft=2.5), "#a888a0", 0.5)                         # the rings under the eyes
    H.nose(f, 4.0, 6.2, crease, 0.22)
    if FORM2:
        H.mouth_smile(f, 4.0, 6.95, 4.0, color="#2a1a30", lip="#8a7090", depth=0.1, thick=0.2, teeth="#f2ecd8", open_h=0.5)
        for x in (2.7, 3.4, 4.6, 5.3):
            f.poly([(x - 0.15, 7.0), (x + 0.15, 7.0), (x, 7.42)], "#ffffff", 0.95)
    else:
        H.mouth_smile(f, 4.0, 7.15, 2.0, color="#8a5a64", lip="#d0a0a8", depth=-0.05, thick=0.17)
        f.fill(f.m_line((3.0, 7.4), (3.4, 7.3), 0.1, 0.1), crease, 0.35)
    # the scar: a vertical cut through the right eye and the brow with three cross bars, a second short one on the cheek (the left of the picture)
    sc, sd = ("#c88a96", "#6a2c3a") if not FORM2 else ("#8a96ff", "#2a2468")
    pts = [(5.75, 2.1), (5.65, 3.2), (5.55, 4.3), (5.6, 5.4), (5.5, 6.5), (5.4, 7.5)]
    if FORM2:
        K.jag(f, pts, INDIGO, 0.2, 0.95, glow=INDIGO_G, core="#e6eaff", dark="#14102a")
    else:
        K.scar(f, pts, sc, sd, 0.22)
    for y in (2.7, 4.0, 6.3):
        a, b = (5.0, y - 0.03), (6.2, y + 0.06)
        if FORM2:
            K.jag(f, [a, b], INDIGO, 0.12, 0.9, glow=INDIGO_G, dark="#14102a", dark_a=0.5)
        else:
            f.fill(f.m_line(a, b, 0.14, 0.14), sd, 0.7)
            f.fill(f.m_line((a[0], a[1] - 0.04), (b[0], b[1] - 0.04), 0.1, 0.1), sc, 0.8)
    if not FORM2:
        K.scar(f, [(1.5, 5.7), (1.3, 6.4), (1.5, 7.1)], sc, sd, 0.2)
        K.scar(f, [(2.6, 3.2), (1.9, 3.5), (1.1, 3.4)], sc, sd, 0.18)
    else:
        for pts2 in ([(1.2, 3.4), (1.55, 4.1), (1.1, 4.8)], [(7.0, 5.2), (6.8, 6.0), (7.1, 6.7)], [(2.4, 6.4), (2.1, 7.0), (2.3, 7.5)], [(3.7, 2.2), (4.0, 2.9), (3.8, 3.5)]):
            K.jag(f, pts2, INDIGO, 0.12, 0.9, glow=INDIGO_G, dark="#14102a", dark_a=0.5)


def head_rest(cv, img64, spec):
    skin, _ = skin_tones()
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, skin)


# ================================================================================================ the vest, the tank and the bones
def tank(f, name):
    skin, crease = skin_tones()
    K.cloth(f, f.m_all(), TANK, top=1.5, bottom=0.8, round_=0.14)
    if name == "front":
        for cx in (2.9, 5.1):                                                                          # the chest: two lit curves
            f.fill(f.m_ellipse(cx, 4.3, 1.3, 1.0, soft=3.0), K.lit(TANK, 1.5), 0.4)
        f.fill(f.m_line((4.0, 2.2), (4.0, 8.8), 0.18, 0.18), H.tone(TANK, 0.5), 0.5)                    # the breast line and the stomach line
        for y in (6.2, 7.3):
            f.fill(f.m_polyline([(3.0, y), (4.0, y + 0.18), (5.0, y)], 0.14), H.tone(TANK, 0.55), 0.4)


def collar(f, name):
    c = VEST if not FORM2 else "#5a1a28"
    m = f.m_band(0.0, 1.8)
    K.cloth(f, m, c, top=1.45, bottom=0.75, round_=0.12)
    H.bevel(f, m, depth=0.22, gain=1.1, spec=0.2)
    f.fill(f.m_rect(0.0, 1.66, f.w, 1.82), "#0e060a", 0.85)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.45), "#1a1018", 1.0)                                                # the dark neckband
    f.fill(f.m_rect(0.0, 0.42, f.w, 0.52), "#000000", 0.5)
    H.shadow_under(f, 1.82, 0.5, 0.3)
    if name == "front":
        f.fill(f.m_poly([(3.4, 0.0), (4.6, 0.0), (4.0, 1.2)]), H.tone(skin_tones()[0][1], 0.9), 1.0)         # the open neck


def patch(f, x0, y0, x1, y1, color):
    m = f.m_rect(x0, y0, x1, y1, 0.08)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(color, 1.15)), (1, H.tone(color, 0.75))]))
    K.seam(f, [(x0 + 0.2, y0 + 0.2), (x1 - 0.2, y0 + 0.2), (x1 - 0.2, y1 - 0.2), (x0 + 0.2, y1 - 0.2), (x0 + 0.2, y0 + 0.2)], "#d89aa0" if not FORM2 else "#a07a88", 0.05, 0.6, (0.3, 0.2), 0.2)


def vest_panel(f, left):
    """One half of the open vest, a lapel that narrows toward the belt: lit panel, dark piping on the opening, a stitch beside it, patches."""
    c = VEST if not FORM2 else "#5a1a28"
    X = (lambda x: x) if left else (lambda x: f.w - x)
    poly = [(X(-0.2), 0.8), (X(2.9), 1.6), (X(2.1), 9.0), (X(-0.2), 9.0)]
    m = f.m_poly(poly)
    f.fill(m, H.lin((0, 1.0), (0, 9.0), [(0, K.lit(c, 1.4)), (0.5, c), (1, H.tone(c, 0.7))]))
    edge = f.m_rect(-0.2, 1.0, 0.35, 9.0) if left else f.m_rect(f.w - 0.35, 1.0, f.w + 0.2, 9.0)
    f.fill(m * edge, K.lit(c, 1.7), 0.35)
    H.trim_line(f, [(X(2.9), 1.55), (X(2.1), 9.0)], "#14080c", width=0.34, spec=0.0, outline=0.0)
    K.seam(f, [(X(2.55 - 0.1), 1.9), (X(1.8 - 0.1), 8.8)], "#c87a86" if not FORM2 else "#8a5a68", 0.05, 0.6)
    patch(f, X(0.5) if left else X(1.7), 4.8, X(1.7) if left else X(0.5), 6.9, "#6a1a28" if not FORM2 else "#3a0e18")


def belts(f, front=False):
    K.belt_band(f, 8.9, 10.0, BELT, stitch="#8a6a60")
    K.belt_band(f, 10.0, 10.95, "#1c1216", stitch="#5a4448")
    if front:
        for x0 in (0.35, 6.1):                                                                          # the two pouches: small leather boxes with a flap and a stud
            pm = f.m_rect(x0, 9.0, x0 + 1.55, 11.0, 0.12)
            f.fill(pm, H.lin((0, 9.0), (0, 11.0), [(0, K.lit("#4a3430", 1.3)), (1, H.tone("#4a3430", 0.7))]))
            f.fill(f.m_rect(x0, 9.0, x0 + 1.55, 9.9, 0.1), K.lit("#5a423c", 1.2), 1.0)
            f.fill(f.m_rect(x0, 9.85, x0 + 1.55, 9.97), "#0e0808", 0.8)
            H.stud(f, x0 + 0.78, 9.5, 0.16, STEEL, spec=0.8)
        K.buckle(f, 4.0, 9.7, 1.9, 1.4, STEEL, hole="#14101a")
    else:
        H.stud(f, 4.0, 9.5, 0.18, STEEL, spec=0.6)


def ribcage(f):
    """Form 2: a bone ribcage over the chest: a sternum plate and four pairs of curved ribs that grow out of the vest, with indigo light in the gaps."""
    plate = [(3.4, 2.2), (4.6, 2.2), (4.75, 7.4), (4.0, 8.2), (3.25, 7.4)]
    K.plate(f, plate, BONE, edge="#2a2418", spec=0.3, depth=0.28)
    for y in (3.3, 4.6, 5.9, 7.0):
        for sg in (-1.0, 1.0):
            a = (4.0 + sg * 0.6, y)
            pts = [a, (4.0 + sg * 1.6, y - 0.35), (4.0 + sg * 2.6, y - 0.15), (4.0 + sg * 3.3, y + 0.5)]
            f.fill(f.m_polyline(pts, 0.5), "#2a2418", 0.9)
            f.fill(f.m_polyline(pts, 0.34), H.lin((0, y - 0.5), (0, y + 0.6), [(0, K.lit(BONE, 1.15)), (1, H.tone(BONE, 0.7))]), 1.0)
            f.fill(f.m_polyline([(p[0], p[1] - 0.06) for p in pts[:3]], 0.1), "#ffffff", 0.35)
            f.fill(f.m_polyline([(p[0], p[1] + 0.34) for p in pts[:3]], 0.1), INDIGO, 0.8, glow=INDIGO_G)
    f.fill(f.m_line((4.0, 2.3), (4.0, 7.9), 0.1, 0.1), "#2a2418", 0.7)


def body_front(f, ov):
    tank(f, "front")
    if FORM2:
        ribcage(f)
    vest_panel(f, True)
    vest_panel(f, False)
    collar(f, "front")
    belts(f, True)
    f.fill(f.m_band(11.0, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, "#2a2230"), (1, "#16121a")]), 1.0)
    H.shadow_under(f, 11.0, 0.5, 0.3)


def body_back(f, ov):
    tank(f, "back")
    c = VEST if not FORM2 else "#5a1a28"
    K.cloth(f, f.m_rect(0, 0.8, 8, 9.0), c, top=1.35, bottom=0.75, round_=0.14, fold_xs=[2.0, 6.0], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    f.fill(f.m_line((4.0, 1.8), (4.0, 8.9), 0.18, 0.18), H.tone(c, 0.45), 0.7)
    K.seam(f, [(3.7, 2.0), (3.7, 8.8)], "#c87a86" if not FORM2 else "#8a5a68", 0.05, 0.5)
    patch(f, 1.0, 3.0, 2.9, 5.2, "#6a1a28" if not FORM2 else "#3a0e18")
    patch(f, 5.3, 5.8, 7.2, 7.8, "#6a1a28" if not FORM2 else "#3a0e18")
    if FORM2:
        for y in np.arange(2.3, 8.6, 1.5):                                                              # the spine: a ridge of bone knobs
            K.plate(f, [(3.4, y), (4.6, y), (4.45, y + 1.0), (3.55, y + 1.0)], BONE, edge="#2a2418", spec=0.25, depth=0.2)
            f.poly([(3.7, y + 0.1), (4.3, y + 0.1), (4.0, y - 0.55)], BONE, 0.95)
    collar(f, "back")
    belts(f)
    f.fill(f.m_band(11.0, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, "#2a2230"), (1, "#16121a")]), 1.0)
    H.shadow_under(f, 11.0, 0.5, 0.3)


def body_side(f, ov, name):
    c = VEST if not FORM2 else "#5a1a28"
    K.cloth(f, f.m_all(), TANK, top=1.4, bottom=0.8, round_=0.14)
    K.cloth(f, f.m_rect(0, 0.8, f.w, 9.0), c, top=1.35, bottom=0.75, round_=0.14, fold_xs=[1.5], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.8)], "#c87a86" if not FORM2 else "#8a5a68", 0.05, 0.5)
    collar(f, name)
    belts(f)
    f.fill(f.m_band(11.0, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, "#2a2230"), (1, "#16121a")]), 1.0)
    H.shadow_under(f, 11.0, 0.5, 0.3)


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
def pauldron(f, name):
    """Form 2: a bone shoulder plate (rows 0..3.2) with ridges and an indigo glowing edge."""
    c = BONE
    m = f.m_band(0.0, 3.2)
    f.fill(m, H.lin((0, 0), (0, 3.2), [(0, K.lit(c, 1.15)), (0.5, c), (1, H.tone(c, 0.66))]))
    for x in np.arange(0.0, f.w + 1.0, 1.1):
        f.fill(f.m_polyline([(float(x), 0.2), (float(x) + 0.25, 3.0)], 0.2) * m, "#2a2418", 0.4)
        f.fill(f.m_polyline([(float(x) + 0.28, 0.2), (float(x) + 0.5, 3.0)], 0.12) * m, "#ffffff", 0.18)
    H.bevel(f, m, depth=0.3, gain=1.0, spec=0.2)
    f.fill(f.m_rect(0.0, 2.85, f.w, 3.2), INDIGO, 1.0, glow=INDIGO_G)
    f.fill(f.m_rect(0.0, 2.75, f.w, 2.88), "#14102a", 0.8)
    f.fill(f.m_rect(0.0, 3.2, f.w, 3.32), "#14102a", 0.8)
    H.shadow_under(f, 3.3, 0.5, 0.3)


def arm(cv, part):
    skin, crease = skin_tones()
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            am = f.m_band(0.0, 7.0)
            f.fill(am, H.lin((0, 0), (0, 7.0), [(0, H.tone(skin[1], 0.9)), (0.25, skin[1]), (1, H.tone(skin[0], 0.95))]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(am, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
            K.sheen(f, am, [f.w * 0.4], 0.8, 6.6, 0.14, "#fff0f0", width=0.4)
            K.muscle(f, am, (skin[0], skin[1], skin[2]), [(f.w * 0.62, 1.8), (f.w * 0.52, 3.4), (f.w * 0.6, 5.2)], [(f.w * 0.7, 1.9), (f.w * 0.6, 3.5)], 0.72, 0.4)
            f.fill(f.m_polyline([(f.w * 0.72, 3.8), (f.w * 0.66, 4.8), (f.w * 0.74, 6.2)], 0.16) * am, "#8a98b8" if not FORM2 else "#8a88b8", 0.4)   # a vein
            if nm in ("front", "back") and part == "arm_l":
                K.scar(f, [(f.w * 0.3, 2.4), (f.w * 0.6, 3.1), (f.w * 0.8, 3.9)], "#c88a96" if not FORM2 else "#8a96ff", "#6a2c3a" if not FORM2 else "#2a2468", 0.18)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), K.lit(skin[1], 1.1), 0.4)
            H.shadow_under(f, 7.0, 0.6, 0.3)
            K.wraps(f, 7.0, 12.0, WRAP if not FORM2 else BONE, slant=0.45 if part == "arm_r" else -0.45, end_tail=(part == "arm_r"))
            if FORM2:
                pauldron(f, nm)
                f.fill(f.m_rect(0.0, 6.0, f.w, 6.25), INDIGO, 1.0, glow=INDIGO_G)                       # the bracer rings
                f.fill(f.m_rect(0.0, 5.9, f.w, 6.0), "#14102a", 0.8)
                f.fill(f.m_rect(0.0, 10.0, f.w, 10.25), INDIGO, 1.0, glow=INDIGO_G)
                f.fill(f.m_rect(0.0, 9.9, f.w, 10.0), "#14102a", 0.8)
                for x in np.arange(0.5, f.w, 1.0):
                    f.poly([(float(x) - 0.28, 7.0), (float(x) + 0.28, 7.0), (float(x), 6.45)], BONE, 0.95)


def arm_tops(cv):
    skin, _ = skin_tones()
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", BONE if FORM2 else skin[1], edge=BONE_D if FORM2 else H.tone(skin[0], 0.8), k_top=1.1, overlay=ov)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 7.0, nm, knee=True, top=1.45, bottom=0.8, seam_color="#5a4a62")
                H.shadow_under(f, 7.0, 0.8, 0.4)
                K.boot(f, BOOT, 6.0, nm, strap=True, strap_color="#4a3a42", gold=STEEL, cuff_color="#4a3a40", cuff_h=1.0, welt="#4a3a40", sole="#08060a")
                if FORM2 and nm in ("front", "west", "east"):
                    f.fill(f.m_rect(0.0, 7.1, f.w, 7.3), INDIGO, 0.9, glow=INDIGO_G)


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
