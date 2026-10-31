"""Udaeus, the skeleton giant (DanMachi, the Monster Rex of the 37th floor), HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for both forms: `udaeus_form2.py` is the same code with the leveled
palette (K.is_form2).

What he is on the 64x64 sheet (tools/arc/char_udaeus.py): a bald bone giant, a skull face with two deep sockets and a lit pupil in each, a tattered dark cloak over the torso, a belt, bone pauldrons,
bone arms with big knuckles, bone greaves on the shins, bare bone feet. The rib cage on the front of the torso, the spine on the back, the skull mask and the big head bone (head scaled 1.5x) are the
`pmx_udaeus*` extras, and so is the black sword: the torso of the skin is therefore a dark cavity with only faint ribs (the 3D ribs stand in front of it), the back keeps a plain tattered cape for the spine.
Wide arms, the height scale (2.08) and the glow semantics (the eye lights, the violet seams and cracks) are the sheet's.
Drawn at 4x detail on both layers (the second layer is dropped beyond 24 blocks): the skull face with the brow ridge, the sockets with a glowing pupil, the nose hole, the cheekbones, the tooth row, sutures
and hairline cracks (on the base layer under the second-layer mask, the mask is only the lower face in the sheet); the collar bones, a tattered cloak with torn hems and a lit violet piping, the belt with a
bone buckle, the pelvis; the arm bones (humerus, elbow knob, the two forearm bones, the finger bones with their joints), pauldrons of overlapping bone plates, kneecaps, the greaves with a violet rim.
Form 2 (the leveled giant): grey ash-bone, a grey cloak, violet eye lights and violet cracks glowing through the bone.

Source (rule 10): no wiki page could be fetched in this environment (danmachi.fandom.com is blocked by the session's proxy); from memory of the series (a huge white skeleton with a black sword on the 37th
floor) plus docs/arc/udaeus.md (which names the page). The cloak, the pauldrons and the violet light are the mod's own (docs/arc/udaeus.md section 2).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = None                                                                # bald: the giant has no hair (hair_map)

P1 = dict(
    hair="#e0d8c0", hair_rgb=(224, 216, 192),
    b0="#8f8670", b1="#bdb398", b2="#e0d8c0", b3="#f6f0de", pit="#14101a",
    skin=("#9a927a", "#c8bea4", "#e8e0c8"),
    cloak="#17121f", cloak_l="#322a42", cloak_d="#08060c", trim="#8a6aff", trim_l="#c8b4ff", glow="#8a6aff",
    eye="#9ad8ff", eye_g="#6ac0ff", crack=False,
)
P2 = dict(
    hair="#bdb7a2", hair_rgb=(189, 183, 162),
    b0="#6a665a", b1="#98927e", b2="#bab49e", b3="#dcd6c2",  pit="#0e0a14",
    skin=("#7a766a", "#a8a290", "#cac4b0"),
    cloak="#34323c", cloak_l="#56546a", cloak_d="#14121a", trim="#b58cff", trim_l="#e4d4ff", glow="#b58cff",
    eye="#d4b4ff", eye_g="#b58cff", crack=True,
)


# ================================================================================================ bone
def bone_fill(f, mask, P, y0, y1, seed, speck=14, ring=True):
    """A bone cylinder: lit in the middle, darker at both sides, specks and a hairline crack."""
    f.fill(mask, H.lin((0, 0), (f.w, 0), [(0, P["b0"]), (0.22, P["b2"]), (0.45, P["b3"]), (0.75, P["b1"]), (1, P["b0"])]))
    for i in range(speck):
        x, y = f.w * (0.08 + 0.84 * H.hsh(seed, i, 0)), y0 + (y1 - y0) * H.hsh(seed, i, 1)
        f.fill(f.m_ellipse(x, y, 0.09 + 0.05 * H.hsh(seed, i, 2), 0.07) * mask, P["b0"], 0.5)
    if H.hsh(seed, 77) > 0.3:
        x = f.w * (0.25 + 0.5 * H.hsh(seed, 78))
        f.fill(f.m_polyline([(x, y0 + 0.2), (x + 0.2, y0 + (y1 - y0) * 0.35), (x - 0.1, y0 + (y1 - y0) * 0.6)], 0.1) * mask, H.tone(P["b0"], 0.7), 0.7)
    if ring:
        H.shadow_under(f, y0 + 0.02, 0.6, 0.25, mask=mask)


def joint(f, P, cx, cy, rx, ry):
    """A rounded joint knob: a lit bone ball with a shadow under it."""
    f.fill(f.m_ellipse(cx, cy + 0.12, rx + 0.08, ry + 0.08), P["b0"], 0.9)
    f.fill(f.m_ellipse(cx, cy, rx, ry), H.lin((cx - rx, cy - ry), (cx + rx, cy + ry), [(0, P["b3"]), (0.55, P["b2"]), (1, P["b0"])]))
    f.fill(f.m_ellipse(cx - rx * 0.3, cy - ry * 0.35, rx * 0.35, ry * 0.3), "#ffffff", 0.35)


# ================================================================================================ the head
def skull_front(f, P, ov):
    sk = P["skin"]
    K.face_base(f, sk, side=0.09, chin=0.1)
    m = f.m_all()
    f.fill(m, H.lin((0, 0), (0, 8), [(0, P["b2"]), (0.5, P["b3"]), (1, P["b1"])]))
    f.fill(f.m_rect(0.0, 0.0, 0.5, 8.0), P["b0"], 0.55)
    f.fill(f.m_rect(7.5, 0.0, 8.0, 8.0), P["b0"], 0.65)
    for i in range(16):
        f.fill(f.m_ellipse(8 * H.hsh(81, i, 0), 8 * H.hsh(81, i, 1), 0.1, 0.07), P["b0"], 0.45)
    # the brow ridge and the two deep sockets with a lit pupil each
    f.fill(f.m_polyline([(0.5, 3.2), (2.4, 2.95), (4.0, 3.25), (5.6, 2.95), (7.5, 3.2)], 0.3), P["b0"], 0.85)
    for cx, sgn in ((2.35, -1.0), (5.65, 1.0)):
        f.fill(f.m_ellipse(cx, 4.75, 1.38, 1.18), P["b0"], 0.95)
        f.fill(f.m_ellipse(cx, 4.8, 1.22, 1.04), H.lin((0, 3.8), (0, 5.9), [(0, "#000000"), (1, P["pit"])]))
        f.fill(f.m_ring(cx, 4.8, 1.22, 0.12), H.tone(P["b0"], 0.55), 0.8)
        f.fill(f.m_ellipse(cx + sgn * 0.1, 4.85, 0.46, 0.46, soft=2.0), P["eye_g"], 0.45, glow=P["eye_g"])
        f.fill(f.m_ellipse(cx + sgn * 0.1, 4.85, 0.3, 0.3), P["eye"], 1.0, glow=P["eye"])
        f.fill(f.m_ellipse(cx + sgn * 0.1 - 0.08, 4.77, 0.1, 0.09), "#ffffff", 0.95)
        f.glow_fill(f.m_ellipse(cx, 4.8, 1.2, 1.0, soft=3.0), P["eye_g"], 0.22)
    # the nose hole, an upside-down heart, and the cheek bones
    f.fill(f.m_poly([(3.55, 5.6), (4.45, 5.6), (4.7, 6.15), (4.0, 6.75), (3.3, 6.15)]), P["pit"], 0.97)
    f.fill(f.m_polyline([(3.55, 5.6), (4.0, 5.9), (4.45, 5.6)], 0.1), P["b0"], 0.8)
    for cx in (1.0, 7.0):
        f.fill(f.m_ellipse(cx, 6.1, 0.85, 0.7), H.tone(P["b3"], 1.03), 0.5)
        f.fill(f.m_polyline([(cx - 0.8, 6.9), (cx, 6.75), (cx + 0.8, 6.9)], 0.16), P["b0"], 0.55)
    # the tooth row
    f.fill(f.m_rect(1.45, 7.0, 6.55, 8.0), P["pit"], 0.97)
    for y0, y1 in ((7.05, 7.5), (7.55, 7.95)):
        for k in range(7):
            x0 = 1.5 + k * 0.72
            f.fill(f.m_rect(x0 + 0.04, y0, x0 + 0.66, y1), H.lin((0, y0), (0, y1), [(0, P["b3"]), (1, P["b1"])]))
    f.fill(f.m_rect(1.45, 7.5, 6.55, 7.56), P["b0"], 0.7)
    # a crack from the brow through the left socket, lit violet in the leveled form
    cr = [[(2.8, 3.0), (3.2, 3.8), (2.9, 4.4)], [(1.6, 5.8), (1.1, 6.5), (1.4, 7.0)]]
    if P["crack"]:
        cr += [[(6.3, 3.1), (6.0, 3.8), (6.4, 4.4)], [(4.0, 3.1), (3.8, 3.7)]]
        K.cracks_on(f, cr, P["trim"], glow=P["glow"], width=0.12, core=P["trim_l"], core_a=0.55, dark="#08040c")
    else:
        for c in cr:
            f.fill(f.m_polyline(c, 0.1), H.tone(P["b0"], 0.55), 0.8)


def head_faces(cv, P):
    """The skull's other faces: bone with sutures, an ear hole on the sides, the back plate with cracks."""
    for nm in ("west", "east", "back", "top", "bottom"):
        f = cv.face("head", nm, False)
        m = f.m_all()
        f.fill(m, H.lin((0, 0), (0, f.h), [(0, P["b3"] if nm != "bottom" else P["b1"]), (1, P["b1"])]))
        for i in range(14):
            f.fill(f.m_ellipse(f.w * H.hsh(91, i, 0, ord(nm[0])), f.h * H.hsh(91, i, 1, ord(nm[0])), 0.1, 0.07), P["b0"], 0.45)
        if nm in ("west", "east"):
            f.fill(f.m_ellipse(f.w * 0.5, 4.6, 0.55, 0.5), P["pit"], 0.95)
            f.fill(f.m_ring(f.w * 0.5, 4.6, 0.55, 0.1), P["b0"], 0.8)
            f.fill(f.m_polyline([(0.2, 2.0), (f.w * 0.3, 1.4), (f.w * 0.55, 2.2), (f.w * 0.8, 1.5), (f.w - 0.2, 2.1)], 0.12), P["b0"], 0.7)      # the temple suture
            f.fill(f.m_polyline([(f.w * 0.8, 5.6), (f.w * 0.9, 7.0)], 0.12), P["b0"], 0.5)
        if nm == "top":
            f.fill(f.m_polyline([(f.w * 0.5, 0.0), (f.w * 0.45, f.h * 0.3), (f.w * 0.55, f.h * 0.7), (f.w * 0.5, f.h)], 0.14), P["b0"], 0.8)
            f.fill(f.m_polyline([(0.0, f.h * 0.5), (f.w * 0.3, f.h * 0.42), (f.w * 0.7, f.h * 0.55), (f.w, f.h * 0.48)], 0.14), P["b0"], 0.8)
        if nm == "back":
            f.fill(f.m_polyline([(f.w * 0.5, 0.0), (f.w * 0.42, 2.0), (f.w * 0.56, 4.0), (f.w * 0.48, 6.4)], 0.14), P["b0"], 0.75)
            f.fill(f.m_polyline([(0.4, 2.8), (f.w * 0.3, 3.2), (f.w * 0.7, 2.7), (f.w - 0.4, 3.0)], 0.12), P["b0"], 0.6)
            if P["crack"]:
                K.cracks_on(f, [[(f.w * 0.5, 1.0), (f.w * 0.62, 2.4), (f.w * 0.5, 3.6)]], P["trim"], glow=P["glow"], width=0.12, core=P["trim_l"], core_a=0.55, dark="#08040c")


# ================================================================================================ the body
def cloak(f, P, nm):
    m = f.m_band(0.0, 10.0)
    K.cloth(f, m, P["cloak"], top=1.35, bottom=0.7, round_=0.12, fold_xs=[1.0, 2.2, 3.4, 4.8, 6.0, 7.2] if f.w > 7 else [1.0, 2.0], fold_depth=0.14, fold_y0=0.6, fold_y1=9.6)
    f.fill(f.m_rect(0.0, 0.0, 0.26, 10.0), P["cloak_l"], 0.4)
    f.fill(f.m_rect(f.w - 0.26, 0.0, f.w, 10.0), P["cloak_d"], 0.5)


def body_front(f, P, ov):
    cloak(f, P, "front")
    # the dark chest cavity the 3D ribs stand in front of: a darker oval with faint ribs
    cav = f.m_ellipse(4.0, 4.2, 3.2, 3.6)
    f.fill(cav, H.lin((0, 0.6), (0, 7.8), [(0, P["cloak_d"]), (1, P["cloak"])]), 0.95)
    for y in (2.1, 3.1, 4.1, 5.1, 6.1):
        for s in (-1.0, 1.0):
            rp = [(4.0 + s * 0.4, y), (4.0 + s * 1.4, y - 0.2), (4.0 + s * 2.6, y + 0.15), (4.0 + s * 3.0, y + 0.7)]
            f.fill(f.m_polyline([(x, yy + 0.14) for x, yy in rp], 0.36) * cav, P["cloak_d"], 0.85)
            f.fill(f.m_polyline(rp, 0.3) * cav, P["b0"], 0.95)
            f.fill(f.m_polyline([(x, yy - 0.06) for x, yy in rp], 0.12) * cav, P["b2"], 0.8)
    f.fill(f.m_line((4.0, 1.4), (4.0, 7.4), 0.3, 0.3), P["b0"], 0.95)
    f.fill(f.m_line((3.95, 1.4), (3.95, 7.4), 0.1, 0.1), P["b2"], 0.8)
    # the magic stone behind the breastbone, dim until the cage breaks
    f.fill(f.m_ellipse(4.0, 4.6, 0.62, 0.74), H.lin((0, 3.9), (0, 5.4), [(0, P["trim_l"]), (1, P["trim"])]), 1.0, glow=P["glow"])
    f.fill(f.m_ring(4.0, 4.6, 0.7, 0.12), P["b0"], 0.9)
    f.fill(f.m_ellipse(3.85, 4.35, 0.14, 0.2), "#ffffff", 0.8)
    if P["crack"]:
        K.cracks_on(f, [[(2.0, 3.2), (1.6, 4.2), (2.0, 5.0)], [(6.0, 4.6), (6.4, 5.6), (5.9, 6.4)]], P["trim"], glow=P["glow"], width=0.11, core=P["trim_l"], core_a=0.5, dark="#08040c", mask=cav)
    # the collar bones across the top, the tattered neckline with a lit rim
    for s in (-1.0, 1.0):
        f.fill(f.m_polyline([(4.0 + s * 0.5, 0.45), (4.0 + s * 1.8, 0.3), (4.0 + s * 3.2, 0.7)], 0.42), P["b0"], 1.0)
        f.fill(f.m_polyline([(4.0 + s * 0.5, 0.4), (4.0 + s * 1.8, 0.25), (4.0 + s * 3.2, 0.62)], 0.28), P["b2"], 1.0)
        f.fill(f.m_polyline([(4.0 + s * 0.6, 0.3), (4.0 + s * 1.8, 0.16)], 0.08), P["b3"], 0.9)
    f.fill(f.m_poly([(3.2, 0.0), (4.8, 0.0), (4.4, 0.7), (3.6, 0.7)]), P["b2"], 1.0)
    K.seam(f, [(0.4, 9.3), (1.4, 8.6), (2.2, 9.3), (3.0, 8.7)], P["trim"], 0.06, 0.7)
    K.seam(f, [(7.6, 9.2), (6.6, 8.6), (5.8, 9.3), (5.0, 8.7)], P["trim"], 0.06, 0.7)
    for pts in ([(0.3, 3.0), (0.9, 4.4), (0.5, 5.9)], [(7.7, 2.4), (7.1, 4.0), (7.5, 5.8)]):                     # the torn side edges
        f.fill(f.m_polyline(pts, 0.14), P["cloak_d"], 0.8)


def body_back(f, P, ov):
    cloak(f, P, "back")
    # the shoulder blades, a plain tattered cape for the spine extra, a lit seam in the middle
    for cx in (2.1, 5.9):
        f.fill(f.m_poly([(cx - 1.5, 0.5), (cx + 1.5, 0.3), (cx + 1.2, 2.6), (cx, 3.4), (cx - 1.4, 2.4)]), H.lin((0, 0.3), (0, 3.4), [(0, P["b2"]), (1, P["b1"])]), 0.9)
        f.fill(f.m_polyline([(cx - 1.4, 1.2), (cx + 0.9, 1.0)], 0.1), P["b0"], 0.7)
    f.fill(f.m_line((4.0, 0.0), (4.0, 10.0), 0.18, 0.18), P["cloak_d"], 0.8)
    for k in range(6):
        y = 3.8 + k * 0.95
        f.fill(f.m_polyline([(0.4, y), (2.2, y + 0.3), (3.8, y + 0.1)], 0.12), P["cloak_d"], 0.55)
        f.fill(f.m_polyline([(4.2, y + 0.1), (5.8, y + 0.3), (7.6, y)], 0.12), P["cloak_d"], 0.55)
    if P["crack"]:
        K.cracks_on(f, [[(2.0, 4.2), (1.5, 5.6), (2.0, 7.0)], [(6.0, 5.0), (6.5, 6.4)]], P["trim"], glow=P["glow"], width=0.11, core=P["trim_l"], core_a=0.5, dark="#08040c")
    K.seam(f, [(0.4, 9.4), (2.0, 8.8), (3.4, 9.4)], P["trim"], 0.06, 0.7)
    K.seam(f, [(4.6, 9.4), (6.0, 8.8), (7.6, 9.4)], P["trim"], 0.06, 0.7)


def body_side(f, P, nm):
    cloak(f, P, nm)
    for y in (2.2, 3.2, 4.2, 5.2):
        f.fill(f.m_polyline([(f.w * 0.1, y), (f.w * 0.5, y + 0.2), (f.w * 0.9, y)], 0.16), P["cloak_l"], 0.5)
    K.seam(f, [(0.3, 9.4), (f.w * 0.5, 8.8), (f.w - 0.3, 9.4)], P["trim"], 0.06, 0.7)


def belt_and_pelvis(f, P, nm):
    K.belt_band(f, 9.1, 10.0, "#241c2c", stitch=P["trim"])
    if nm == "front":
        K.buckle(f, 4.0, 9.55, 1.6, 0.85, P["b1"], hole="#241c2c")
        joint(f, P, 4.0, 9.55, 0.28, 0.25)
    # the pelvis: iliac wings, a pelvic notch and two holes, bone bands under a tattered loincloth
    m = f.m_band(10.0, 12.0)
    f.fill(m, H.lin((0, 10), (0, 12), [(0, P["b2"]), (0.6, P["b3"]), (1, P["b1"])]))
    H.shadow_under(f, 10.0, 0.5, 0.35)
    if nm in ("front", "back"):
        for cx in (2.0, 6.0):
            f.fill(f.m_ellipse(cx, 11.0, 0.55, 0.42), P["pit"], 0.95)
            f.fill(f.m_ring(cx, 11.0, 0.55, 0.1), P["b0"], 0.8)
        f.fill(f.m_poly([(3.2, 10.0), (4.8, 10.0), (4.35, 11.2), (3.65, 11.2)]), P["pit"], 0.95)
        f.fill(f.m_polyline([(0.2, 10.3), (1.8, 10.9), (3.3, 10.5)], 0.1), P["b0"], 0.6)
        f.fill(f.m_polyline([(7.8, 10.3), (6.2, 10.9), (4.7, 10.5)], 0.1), P["b0"], 0.6)
        for x, ln in ((0.8, 1.3), (2.6, 1.0), (5.4, 1.2), (7.0, 1.4)):                                  # the loincloth ribbons
            f.fill(f.m_poly([(x - 0.4, 10.0), (x + 0.4, 10.0), (x + 0.2, 10.0 + ln), (x - 0.1, 10.0 + ln + 0.3)]), H.lin((0, 10), (0, 12), [(0, P["cloak"]), (1, P["cloak_d"])]), 0.97)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, P, ov)
            elif nm == "back":
                body_back(f, P, ov)
            else:
                body_side(f, P, nm)
            belt_and_pelvis(f, P, nm)


# ================================================================================================ the arms
def finger_bones(f, P, y0):
    """The bony hand: a wide palm bone row, four long fingers with a joint every third, dark gaps."""
    f.fill(f.m_band(y0, 12.0), H.lin((0, 0), (f.w, 0), [(0, P["b0"]), (0.3, P["b2"]), (0.5, P["b3"]), (0.8, P["b1"]), (1, P["b0"])]))
    H.shadow_under(f, y0, 0.5, 0.3)
    if f.name in ("front", "back"):
        n = 4
        for i in range(1, n):
            x = f.w * i / n
            f.fill(f.m_line((x, y0 + 0.5), (x, 11.98), 0.22, 0.16), P["pit"], 0.9)
        for i in range(n):
            x = f.w * (i + 0.5) / n
            for y in (y0 + 0.9, y0 + 1.25 + (11.9 - y0) * 0.45):
                joint(f, P, x, y, 0.2, 0.16)
        f.fill(f.m_polyline([(0.2, y0 + 0.5), (f.w * 0.5, y0 + 0.62), (f.w - 0.2, y0 + 0.5)], 0.16), P["b0"], 0.8)
    else:
        f.fill(f.m_line((f.w * 0.3, y0 + 0.9), (f.w * 0.7, y0 + 1.0), 0.14, 0.14), P["b0"], 0.6)


def arm(cv, part, P, form2):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            # the upper arm bone, the elbow knob, the two bones of the forearm
            bone_fill(f, f.m_band(2.5, 6.0), P, 2.5, 6.0, 31 + SIDES.index(nm))
            joint(f, P, f.w * 0.5, 6.1, f.w * 0.42, 0.45)
            bone_fill(f, f.m_band(6.5, 10.0), P, 6.5, 10.0, 41 + SIDES.index(nm), ring=False)
            f.fill(f.m_line((f.w * 0.5, 6.6), (f.w * 0.5, 9.9), 0.2, 0.2), P["pit"], 0.85)                 # the gap between radius and ulna
            joint(f, P, f.w * 0.5, 10.05, f.w * 0.36, 0.3)
            finger_bones(f, P, 10.35)
            if P["crack"]:
                K.cracks_on(f, [[(f.w * 0.3, 3.2), (f.w * 0.5, 4.2), (f.w * 0.4, 5.3)], [(f.w * 0.6, 7.2), (f.w * 0.5, 8.3)]], P["trim"], glow=P["glow"], width=0.1, core=P["trim_l"], core_a=0.5, dark="#08040c")
            # the pauldron: overlapping bone plates with a violet rim (the second layer, rows 0 to 3)
            K.plate_band(f, 0.0, 3.4, P["b1"], trim=P["trim"], glow=P["glow"], segs=2, light_k=1.55, rivets=[0.7, f.w - 0.7], jag=0.3, seed=7)
            H.shadow_under(f, 3.4, 0.6, 0.3)
    for ov in (False, True):
        K.cap(cv, part, "top", P["b2"], edge=P["b0"], k_top=1.1, overlay=ov)


# ================================================================================================ the legs
def legs(cv, P, form2):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                bone_fill(f, f.m_band(0.0, 5.0), P, 0.0, 5.0, 51 + SIDES.index(nm) + (7 if part == "leg_l" else 0), ring=False)
                joint(f, P, f.w * 0.5, 5.6, f.w * 0.44, 0.65)                                              # the kneecap
                bone_fill(f, f.m_band(6.2, 10.2), P, 6.2, 10.2, 61 + SIDES.index(nm), ring=False)
                f.fill(f.m_line((f.w * 0.5, 6.3), (f.w * 0.5, 10.0), 0.18, 0.18), H.tone(P["b0"], 0.75), 0.75)
                # the shin greave: a bone plate with a violet rim and two studs, over the lower shin (the second layer, rows 6 to 11)
                K.plate_band(f, 6.4, 10.4, P["b1"], trim=P["trim"], glow=P["glow"], segs=2, light_k=1.5, rivets=[0.7, f.w - 0.7] if f.w >= 4 else None, seed=9)
                # the foot bones
                foot = f.m_band(10.5, 12.0)
                f.fill(foot, H.lin((0, 0), (f.w, 0), [(0, P["b0"]), (0.3, P["b2"]), (0.55, P["b3"]), (1, P["b0"])]))
                H.shadow_under(f, 10.52, 0.5, 0.3)
                if nm == "front":
                    for i in range(1, 4):
                        f.fill(f.m_line((f.w * i / 4.0, 11.2), (f.w * i / 4.0, 11.95), 0.2, 0.14), P["pit"], 0.85)
                    for i in range(4):
                        joint(f, P, f.w * (i + 0.5) / 4.0, 11.3, 0.2, 0.15)
                if P["crack"]:
                    K.cracks_on(f, [[(f.w * 0.3, 0.8), (f.w * 0.5, 2.0), (f.w * 0.35, 3.2)]], P["trim"], glow=P["glow"], width=0.1, core=P["trim_l"], core_a=0.5, dark="#08040c")
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", P["b0"], edge=None, k_top=1.0, overlay=ov)


# ================================================================================================ paint
def paint(spec, cid, masks):
    form2 = K.is_form2(cid)
    P = P2 if form2 else P1
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, P["hair"], P["hair_rgb"])
    head_cov = {("head", "front"): K.cover_of(cv, "head", "front")}
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"], ears=False)
    head_faces(cv, P)
    for f in K.pair(cv, "head", "front"):
        skull_front(f, P, False)
    body(cv, P)
    arm(cv, "arm_r", P, form2)
    arm(cv, "arm_l", P, form2)
    legs(cv, P, form2)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim, extra_covers=head_cov)
