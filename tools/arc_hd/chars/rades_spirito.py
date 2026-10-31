"""Rades Spirito, the Soul Corpse mage cast out of the Purple Orca, HD 256x256 (docs/arc/hd_arc_bc6.md). One painter for both forms: `rades_spirito_form2.py` is the same code with the Betrayed palette.

What he wears is the 64x64 sheet's outfit (tools/arc/char_rades_spirito.py): a dark red sleeveless top over the body with bandages wound tight round the neck, a wide dark belt with a steel buckle, dark
red trousers, calf boots with a light cuff and light stripes, a long black glove on the character's left arm, dark red bandages up the right forearm, dark red bandages over the forehead and the
left eye with a light-green ornament, a scar on the right cheek and one blue eye. The tattered cape on the back, the grimoire in the left hand and (form 2) the ring of bone thorns are the
`pmx_rades_spirito*` extras, so the back of the top is kept plain under the cape. Wide arms (the sheet's), height scale kept.
Form 2 (the Betrayed): ashen skin, the visible eye burns green, glowing green veins on the face and the arms, a redder top and trousers; the ornament burns brighter.
Glow: the ornament on the eyepatch (both forms), the green eye and the veins (form 2), a faint thread of light along the glove strap.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): a narrow angry blue eye with a dark ring under it and a hard brow, the bandage turns of the forehead
band and of the eyepatch with a slanted wrap, a sewn gem for the ornament, a raised scar with stitch marks, the long ragged hair framing the face (the cut itself is the 3D hair: `anime_long_open`,
and `anime_mane` in form 2); the top with black piping at the armholes and a dark centre fold, the neck bandage with a hanging end, the wide belt with studs and a hanging strap, bare arms with
muscle lines, the bandage turns on the right forearm, the glove with a dark red strap, trousers with knee creases and boots with a light cuff and two light stripes.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); from memory of the series plus docs/arc/rades_spirito.md (which cites the wiki
summaries): long messy light-purple hair, one blue eye, dark red bandages over the forehead and the left eye with a light-green ornament, bandages round the neck, a scar on the right cheek, a dark
red sleeveless top and trousers, a glove on the left arm, a wide belt and light-striped boots. Which eye is covered: the text says the left, so it is the character's left (the viewer's right).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc6 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE_1 = "anime_long_open"                                  # long messy hair with an open forehead (hair_map says anime_short: too short for "long messy" hair)
HAIR_STYLE_2 = "anime_mane"                                       # the mane of the second form (as in hair_map)

P1 = dict(
    hair="#c8aaff", hair_rgb=(200, 170, 255), tint=(172, 112, 255),
    skin=("#dcc0c4", "#eed6d8", "#fbeced"),
    top="#6a1620", top_l="#8e2a38", top_d="#340810",
    pants="#5a1220", pants_d="#2a0610",
    band="#8a1c2c", band_l="#b0384a", band_d="#4a0c16",
    black="#241a1e", black_l="#4a3a42",
    belt="#2a1c1c", belt_d="#120a0a", steel="#a8a8b8", steel_d="#5a5a6a",
    glove="#2a2024", boot="#2a2024", light="#d4d0c4",
    iris=("#1e48a8", "#4a82e8", "#c0dcff"), sclera="#f8f4f6", lash="#1a1218",
    green="#9be8b0", green_d="#3a8a5a", green_l="#e4ffee",
    scar="#c88a8a", scar_l="#f0c0bc",
)
P2 = dict(
    hair="#e4d2ff", hair_rgb=(228, 210, 255), tint=(205, 160, 255),
    skin=("#8a8694", "#a6a2b0", "#cac6d4"),
    top="#8a1c2c", top_l="#b0384a", top_d="#400a14",
    pants="#6a1620", pants_d="#300812",
    band="#8a1c2c", band_l="#b0384a", band_d="#4a0c16",
    black="#1c1418", black_l="#44343c",
    belt="#2a1c1c", belt_d="#120a0a", steel="#a8a8b8", steel_d="#5a5a6a",
    glove="#241a1e", boot="#241a1e", light="#c8c4b8",
    iris=("#2a8a52", "#9be8b0", "#f0fff4"), sclera="#e8f4ec", lash="#10201a",
    green="#9be8b0", green_d="#3a8a5a", green_l="#e4ffee",
    scar="#b08090", scar_l="#dcb0b8",
)


# ================================================================================================ bandages
def bandage(f, P, mask, y0, y1, slant=0.4, step=0.95, a=1.0):
    """Red bandage turns inside a mask: a base, slanted wraps with dark gaps and light edges."""
    f.fill(mask, H.lin((0, y0), (0, y1), [(0, K.lit(P["band"], 1.2)), (0.5, P["band"]), (1, H.tone(P["band"], 0.7))]), a)
    K.wrap_lines(f, mask, y0 - 0.2, y1, P["band"], P["band_d"], step=step, slant=slant, a=0.9 * a)
    side = np.minimum(f.X, f.w - f.X)
    f.shade(mask, 1.0 - 0.1 * (1.0 - H.smoothstep(0.0, 0.7, side)), 0.8)


def forehead_band(f, P, y0=2.25, y1=3.4):
    """The bandage turns round the forehead (drawn on every side of the head, both layers)."""
    m = f.m_band(y0, y1)
    f.fill(f.m_rect(0.0, y0 - 0.08, f.w, y1 + 0.1), H.tone(P["band_d"], 0.5), 0.6)
    bandage(f, P, m, y0, y1, slant=0.3, step=0.6)
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.1), K.lit(P["band_l"], 1.3), 0.6)
    f.fill(f.m_rect(0.0, y1 - 0.1, f.w, y1), H.tone(P["band_d"], 0.6), 0.7)


def eyepatch(f, P, form2):
    """The bandage over the character's left eye (the viewer's right): a wrap that falls from the forehead band over the eye to the cheek, a light-green ornament sewn onto it."""
    poly = [(4.15, 3.0), (7.95, 3.0), (7.95, 5.6), (7.5, 6.6), (6.5, 7.05), (5.2, 6.8), (4.2, 6.1), (3.9, 4.6)]
    m = f.m_poly(poly)
    f.fill(f.m_polyline(poly + [poly[0]], 0.34), H.tone(P["band_d"], 0.5), 0.7)
    bandage(f, P, m, 3.0, 7.0, slant=1.5, step=0.8)
    f.fill(f.m_polyline([(4.15, 3.0), (3.9, 4.6), (4.2, 6.1)], 0.16), K.lit(P["band_l"], 1.3), 0.5)
    # the loose end of the wrap that crosses the cheek
    f.fill(f.m_poly([(5.2, 6.7), (6.6, 6.9), (6.9, 7.8), (6.1, 7.55)]), P["band"], 0.95)
    f.fill(f.m_polyline([(5.2, 6.7), (6.9, 7.8)], 0.12), P["band_d"], 0.7)
    # the ornament: a green gem in a dark setting, sewn on the bandage, it glows
    f.ellipse(5.85, 4.75, 0.95, 1.25, H.tone(P["band_d"], 0.5), a=0.95)
    H.gem(f, 5.85, 4.75, 0.62, P["green"], setting=P["steel"], facets=4, spec=0.9, glow=P["green"], ry=0.85)
    for dy in (-1.05, 1.05):
        H.stud(f, 5.85, 4.75 + dy, 0.1, P["steel"], spec=0.7)


def scar_cheek(f, P, form2):
    """The scar on the character's right cheek (the viewer's left): a diagonal from under the eye to the jaw with stitch marks."""
    pts = [(1.0, 5.95), (1.7, 6.6), (2.4, 7.3), (2.9, 7.9)]
    K.scar(f, pts, P["scar_l"], P["scar"], 0.24)
    for t in (0.3, 0.62):
        i = int(t * (len(pts) - 1)) + 1
        x, y = pts[min(i, len(pts) - 1)]
        f.fill(f.m_line((x - 0.4, y - 0.1), (x + 0.4, y + 0.4), 0.12, 0.12), P["scar"], 0.75)


# ================================================================================================ the head
def head_front(cv, P, form2):
    f = cv.face("head", "front")
    fo = cv.face("head", "front", True)
    fo.clear()
    K.face_base(f, P["skin"], side=0.08, chin=0.07)
    pal = K.hair_pal(P["hair"], light_k=1.12, dark_k=0.62)
    # a ragged messy fringe: points hang over the forehead, a long lock falls each side of the face
    rag = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.5), (7.6, 1.5), (6.9, 2.3), (6.2, 1.2), (5.4, 2.1), (4.6, 1.0), (3.7, 2.0), (2.9, 1.1), (2.1, 2.2), (1.2, 1.3), (0.5, 2.3), (-0.3, 1.6)]
    mass = K.fringe_mass(f, pal, rag, y_top=0.0, y_bot=2.4)
    K.strands(f, pal, (0.8, 1.9, 3.1, 4.3, 5.5, 6.7, 7.5), 0.0, 2.0, mass, a=0.4)
    for cx, lean, ln in ((0.45, 0.12, 6.4), (7.55, -0.12, 6.0)):
        K.lock(f, pal, cx, 1.0, 1.2, ln, lean, 1.0)
    # the forehead band and the eyepatch
    forehead_band(f, P)
    eyepatch(f, P, form2)
    # the blue eye (the character's right, the viewer's left): narrow and angry under a hard brow
    H.brow(f, (3.55, 4.2), (0.9, 3.3), "#4a3a78" if not form2 else "#8a80b0", 0.5, 0.16, arch=0.0)
    K.eye_bag(f, 2.3, 6.0, 2.2, "#9a7e90" if not form2 else "#4a4658", 0.5)
    h = 1.55 if not form2 else 1.9
    al = H.eye(f, 2.3, 5.2, 2.6, h, P["iris"], lash=P["lash"], sclera=P["sclera"], tilt=0.26, lash_w=0.55, flick=0.4, lid_shadow=0.5, hi=0.85)
    K.eye_glow(f, al, 2.3, 5.2, 2.6, h, P["green"] if form2 else "#6aa0ff", 0.95 if form2 else 0.0)
    H.nose(f, 4.0, 6.35, "#c8a0a8" if not form2 else "#76727e", 0.2)
    if form2:                                                                  # a mad, thin grin with the teeth showing
        H.mouth_smile(f, 4.0, 7.35, 3.0, color="#3a1a28", lip="#8a5a68", depth=0.28, thick=0.22)
        f.fill(f.m_polyline([(2.9, 7.45), (4.0, 7.7), (5.1, 7.45)], 0.2), "#eef0e8", 0.9)
    else:
        H.mouth_smile(f, 4.0, 7.4, 2.0, color="#8a5058", lip="#c48a90", depth=-0.2, thick=0.18)
    scar_cheek(f, P, form2)
    if form2:                                                                  # veins of the soul-light on the face
        for p in ([(0.5, 3.9), (0.9, 4.8), (0.5, 5.6)], [(1.4, 6.9), (1.0, 7.5), (1.5, 7.9)], [(7.6, 6.2), (7.2, 7.1)], [(3.3, 6.7), (3.5, 7.2)]):
            K.glow_line(f, p, P["green"], 0.15, 0.85, core="#ffffff", core_a=0.5)
    # the same bandage drawn on the second layer (the hair and the layer are dropped beyond 24 blocks; the forehead band stays)
    forehead_band(fo, P)
    eyepatch(fo, P, form2)


def head_sides(cv, P, form2):
    pal = K.hair_pal(P["hair"], light_k=1.18, dark_k=0.62)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.8, 1.0, 5.6, lean, 0.95)
    for nm in ("west", "east", "back"):
        for ov in (False, True):
            g = cv.face("head", nm, ov)
            if ov:
                g.clear()
            forehead_band(g, P)
    # the tail of the forehead band hangs at the back of the head
    g = cv.face("head", "back")
    g.fill(g.m_poly([(5.4, 3.3), (6.9, 3.3), (7.1, 5.6), (6.2, 5.1), (5.5, 5.8)]), P["band"], 1.0)
    g.fill(g.m_polyline([(5.4, 3.3), (5.5, 5.8)], 0.12), P["band_d"], 0.7)


# ================================================================================================ the top
def neck(f, P, name):
    """The bandage wound tight round the neck, with a hanging end on the front."""
    m = f.m_band(0.0, 1.9)
    bandage(f, P, m, 0.0, 1.9, slant=0.35, step=0.6)
    f.fill(f.m_rect(0.0, 1.78, f.w, 1.9), H.tone(P["band_d"], 0.55), 0.8)
    H.shadow_under(f, 1.92, 0.55, 0.32)
    if name == "front":
        f.fill(f.m_poly([(5.6, 1.5), (6.7, 1.6), (6.9, 3.6), (6.1, 3.2), (5.7, 3.8)]), P["band"], 0.97)
        f.fill(f.m_polyline([(5.6, 1.5), (5.7, 3.8)], 0.12), P["band_d"], 0.7)
        f.fill(f.m_polyline([(6.7, 1.6), (6.9, 3.6)], 0.1), K.lit(P["band_l"], 1.3), 0.5)


def top_front(f, P, form2):
    K.cloth(f, f.m_all(), P["top"], top=1.25 if not form2 else 1.2, bottom=0.78, round_=0.14, fold_xs=[1.4, 3.0, 5.0, 6.6], fold_depth=0.11, fold_y0=2.0, fold_y1=8.4)
    K.sheen(f, f.m_rect(0.0, 1.9, 4.0, 8.4), [1.4], 2.1, 8.2, 0.3, "#ff9aa8", width=0.3)
    K.sheen(f, f.m_rect(4.0, 1.9, 8.0, 8.4), [6.5], 2.1, 8.2, 0.16, "#ff9aa8", width=0.26)
    # the black piping round the armholes and a dark centre fold
    for x0, x1 in ((0.0, 0.34), (7.66, 8.0)):
        f.fill(f.m_rect(x0, 1.9, x1, 8.5), P["black"], 0.95)
    f.fill(f.m_line((4.0, 2.2), (4.0, 8.4), 0.18, 0.18), P["top_d"], 0.5)
    f.fill(f.m_polyline([(0.4, 3.2), (1.5, 4.3), (2.6, 5.3)], 0.14), P["top_d"], 0.5)
    f.fill(f.m_polyline([(7.6, 3.2), (6.5, 4.3), (5.4, 5.3)], 0.14), P["top_d"], 0.5)
    # panel seams with stitching and a lighter line along the hem above the belt; the neck bandage covers the neckline
    for x in (2.7, 5.3):
        f.fill(f.m_line((x, 2.0), (x, 8.4), 0.14, 0.14), P["top_d"], 0.55)
        K.seam(f, [(x + 0.18, 2.1), (x + 0.18, 8.3)], P["top_l"], 0.05, 0.6, (0.3, 0.2), 0.0)
    f.fill(f.m_rect(0.0, 8.0, f.w, 8.12), P["top_l"], 0.6)
    # a tear at the hem above the belt
    f.fill(f.m_poly([(1.0, 7.9), (1.5, 8.5), (2.2, 8.2), (2.4, 8.6), (1.2, 8.7)]), P["top_d"], 0.55)
    neck(f, P, "front")
    belt(f, P, True)


def top_back(f, P, form2):
    K.cloth(f, f.m_all(), P["top"], top=1.2, bottom=0.78, round_=0.14, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.11, fold_y0=2.0, fold_y1=8.4)
    K.sheen(f, f.m_rect(0.0, 1.9, 8.0, 8.4), [1.6, 6.4], 2.1, 8.2, 0.2, "#ff9aa8", width=0.3)
    for x0, x1 in ((0.0, 0.34), (7.66, 8.0)):
        f.fill(f.m_rect(x0, 1.9, x1, 8.5), P["black"], 0.95)
    f.fill(f.m_line((4.0, 2.0), (4.0, 8.4), 0.2, 0.2), P["top_d"], 0.55)
    neck(f, P, "back")
    belt(f, P, False)


def top_side(f, P, form2, name):
    K.cloth(f, f.m_all(), P["top"], top=1.2, bottom=0.78, round_=0.14, fold_xs=[1.3, 2.7], fold_depth=0.11, fold_y0=2.0, fold_y1=8.4)
    ed = f.w - 0.34 if name == "west" else 0.0
    f.fill(f.m_rect(ed, 1.9, ed + 0.34, 8.5), P["black"], 0.95)
    neck(f, P, name)
    belt(f, P, False)


def belt(f, P, front):
    """The wide belt: a thick dark band with a top and a bottom stitch line, studs and a steel buckle in front, a strap hanging from it."""
    K.belt_band(f, 8.45, 10.05, P["belt"], stitch=P["black_l"])
    f.fill(f.m_rect(0.0, 8.45, f.w, 8.58), P["steel_d"], 0.55)
    for x in (0.8, 7.2) if front else (1.0, 3.0, 5.0, 7.0):
        H.stud(f, x, 9.25, 0.17, P["steel"], spec=0.8)
    if front:
        K.buckle(f, 4.0, 9.25, 2.2, 1.45, P["steel"], hole=P["belt_d"])
        # a strap with a steel tip hanging from the belt on the character's right hip
        f.fill(f.m_poly([(1.4, 10.0), (2.4, 10.0), (2.35, 11.9), (1.45, 11.9)]), H.lin((0, 10.0), (0, 11.9), [(0, K.lit(P["belt"], 1.6)), (1, H.tone(P["belt"], 0.7))]))
        f.fill(f.m_rect(1.45, 11.55, 2.35, 11.9), P["steel"], 0.95)
    else:
        H.stud(f, 4.0, 9.25, 0.2, P["steel"], spec=0.7)


def hips(f, P, form2):
    m = f.m_band(10.05, 12.0)
    f.fill(m, H.lin((0, 10.05), (0, 12.0), [(0, K.lit(P["pants"], 1.1)), (1, H.tone(P["pants"], 0.75))]))
    H.folds(f, m, [f.w * 0.3, f.w * 0.7], depth=0.12, width=0.25, y0=10.1, y1=12.0)
    H.shadow_under(f, 10.08, 0.5, 0.3)


def body(cv, P, form2):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                top_front(f, P, form2)
            elif nm == "back":
                top_back(f, P, form2)
            else:
                top_side(f, P, form2, nm)
            hips(f, P, form2)


# ================================================================================================ arms and legs
def bare_arm(f, P, nm, form2, y0, y1):
    """Bare skin with muscle: a shoulder shadow, a bicep line, a tendon, veins in form 2."""
    sk = P["skin"]
    m = K.limb_skin(f, sk, y0, y1, tendon=True)
    if nm in ("front", "back"):
        K.muscle(f, m, sk, [(f.w * 0.3, 2.0), (f.w * 0.33, 3.4), (f.w * 0.3, 4.8)], [(f.w * 0.4, 2.0), (f.w * 0.43, 3.3)], k=0.8, a=0.4)
    else:
        K.muscle(f, m, sk, [(f.w * 0.5, 1.4), (f.w * 0.55, 3.2), (f.w * 0.5, 4.8)], None, k=0.8, a=0.35)
    f.fill(f.m_band(0.0, 0.7) * m, H.tone(sk[0], 0.78), 0.35)
    return m


def vein_lines(f, P, nm):
    """The soul-light veins on a bare arm (form 2): thin branching glowing lines."""
    if nm in ("front", "back"):
        for pts in ([(f.w * 0.3, 1.2), (f.w * 0.4, 2.6), (f.w * 0.25, 3.8), (f.w * 0.35, 5.0)], [(f.w * 0.7, 2.0), (f.w * 0.6, 3.2), (f.w * 0.72, 4.4)], [(f.w * 0.4, 2.6), (f.w * 0.62, 3.2)]):
            K.glow_line(f, pts, P["green"], 0.15, 0.85, core="#ffffff", core_a=0.5)
    else:
        K.glow_line(f, [(f.w * 0.4, 1.6), (f.w * 0.55, 3.0), (f.w * 0.42, 4.4)], P["green"], 0.15, 0.85, core="#ffffff", core_a=0.5)


def arm_right(cv, P, form2):
    """The character's right arm (the viewer's left): bare, the forearm wrapped in dark red bandages, a bare hand."""
    for nm in SIDES:
        for f in K.pair(cv, "arm_r", nm):
            bare_arm(f, P, nm, form2, 0.0, 12.0)
            if form2:
                vein_lines(f, P, nm)
            wrap = f.m_band(6.3, 10.4)
            f.fill(f.m_rect(0.0, 6.2, f.w, 10.5), H.tone(P["band_d"], 0.5), 0.6)
            bandage(f, P, wrap, 6.3, 10.4, slant=0.5, step=0.8)
            f.fill(f.m_rect(0.0, 6.3, f.w, 6.4), K.lit(P["band_l"], 1.3), 0.6)
            f.fill(f.m_rect(0.0, 10.3, f.w, 10.5), H.tone(P["band_d"], 0.55), 0.8)
            H.shadow_under(f, 10.5, 0.7, 0.3)
            # the loose end of the wrap at the elbow side
            if nm in ("front", "west", "east"):
                f.fill(f.m_poly([(f.w * 0.55, 6.1), (f.w * 0.8, 6.1), (f.w * 0.82, 7.0), (f.w * 0.62, 6.8)]), P["band"], 0.95)
            K.fingers(f, P["skin"], 11.0, 12.0, n=3)


def arm_left(cv, P, form2):
    """The character's left arm (the viewer's right): bare upper arm, a long black glove with a dark red strap and a steel stud; the strap line glows faintly."""
    for nm in SIDES:
        for f in K.pair(cv, "arm_l", nm):
            bare_arm(f, P, nm, form2, 0.0, 12.0)
            if form2:
                vein_lines(f, P, nm)
            K.glove(f, P["glove"], 5.6, 12.0, nm, finger_n=3, cuff_color=None, knuckle_k=1.5)
            f.fill(f.m_rect(0.0, 5.6, f.w, 5.75), K.lit(P["glove"], 2.2), 0.4)
            # the strap round the top of the glove: a dark red band with a steel stud and a glowing thread
            K.cuff(f, 5.75, 6.75, P["band"], pipe=P["band_l"], stud_color=P["steel"], spec=0.3)
            f.fill(f.m_rect(0.0, 6.55, f.w, 6.62), P["green"], 0.7, glow=P["green"])
            # a second narrow strap near the wrist
            f.fill(f.m_rect(0.0, 9.0, f.w, 9.4), H.tone(P["black_l"], 1.0), 0.9)
            f.fill(f.m_rect(0.0, 9.0, f.w, 9.08), K.lit(P["black_l"], 1.8), 0.6)
            H.stud(f, f.w * 0.5, 9.2, 0.13, P["steel"], spec=0.8)
            f.fill(f.m_poly([(f.w * 0.1, 7.8), (f.w * 0.5, 8.0), (f.w * 0.9, 7.8), (f.w * 0.85, 8.2), (f.w * 0.15, 8.2)]), P["black_l"], 0.25)
            H.shadow_under(f, 5.78, 0.6, 0.3)


def arm_tops(cv, P):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            f = cv.face(part, "top", ov)
            f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, K.lit(P["skin"][1], 1.0)), (1, H.tone(P["skin"][0], 0.85))]))


def legs(cv, P, form2):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, P["pants"], 0.0, 8.4, nm, knee=True, top=1.2, bottom=0.8, seam_color=P["black_l"])
                H.shadow_under(f, 0.0, 0.6, 0.25)
                K.boot(f, P["boot"], 8.2, nm, strap=False, cuff_color=P["light"], cuff_h=1.0, welt=P["black_l"], sole="#0a0708")
                # one light stripe down the front and the back of the shaft, a thin one on the sides
                if nm in ("front", "back"):
                    xc = f.w * 0.5
                    f.fill(f.m_rect(xc - 0.3, 9.4, xc + 0.3, 11.25), P["light"], 0.92)
                    f.fill(f.m_rect(xc - 0.3, 9.4, xc - 0.16, 11.25), "#ffffff", 0.5)
                    f.fill(f.m_rect(xc + 0.2, 9.4, xc + 0.3, 11.25), H.tone(P["light"], 0.7), 0.7)


# ================================================================================================ paint
def paint(spec, cid, masks):
    form2 = K.is_form2(cid)
    P = P2 if form2 else P1
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE_2 if form2 else HAIR_STYLE_1, P["hair"], P["hair_rgb"])
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"])
    head_sides(cv, P, form2)
    head_front(cv, P, form2)
    body(cv, P, form2)
    arm_right(cv, P, form2)
    arm_left(cv, P, form2)
    arm_tops(cv, P)
    legs(cv, P, form2)
    return K.finish(cv, covers, hair, P["tint"], slim)
