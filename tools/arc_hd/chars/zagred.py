"""Zagred, the Word Soul devil, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for both forms: `zagred_form2.py` is the same code with the devil palette (K.is_form2).

What he wears is the 64x64 sheet's outfit (tools/arc/char_zagred.py): a long black coat with a violet trim, rune patterns and a high collar, left open over a bare chest, no sleeves (bare ashen arms),
black gloves, a double belt with a glowing buckle, a coat skirt to the ankles over knee boots with a violet cuff. The glyph tattoos on the chest, the upper arms and the forearms, the floating grimoire
and (form 2) the horns, the bat wings, the tail and the trident are the `pmx_zagred*` extras: the chest of the skin is kept plain (the glyph boxes stand on it), the back keeps one clean panel
for the wings with the big rune emblem on it (the wings sit lower and wider). Wide arms, the height scale (1.23) and the glow semantics (eyes, trim, buckle, rune emblem) are the sheet's.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): narrow angry red eyes with a glow under hard dark brows, a smirk, the rune on the brow (the lower half
shows under the fringe), glowing lines on the cheeks and thin veins; tall black spikes (the 3D hair); the coat with lit lapel panels, folds, violet piping on the opening and a lit stitched rune
pattern, a high collar with a lit rim, a double belt with a glowing buckle, the coat skirt with deep folds and a glowing violet hem, black gloves with a lit cuff, knee boots with a violet cuff; the
back with the big glowing rune emblem. Form 2 (the Word Soul devil): grey skin, slit red eyes with fangs and face cracks, a crimson trim and a ragged hem, red glowing runes and cracks.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia are blocked by the session's proxy); from memory of the series plus docs/arc/zagred.md
(which cites the wiki): a tall lean devil with grey skin, red irises, horns, bat wings and clawed hands. The host coat, the violet Word and the glyphs are the mod's own (docs/arc/zagred.md).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_spike_tall"                                                  # tall dark spikes in clumps (hair_map)

P1 = dict(
    hair="#16121e", hair_rgb=(22, 18, 30),
    skin=("#686a78", "#868898", "#a6a8b6"),
    coat="#1c1626", coat_l="#3a2e52", coat_d="#0c0812", trim="#8a4cff", trim_l="#c8a8ff", trim_d="#4a2a8a", rune=(74, 42, 138), collar="#241c32",
    belt="#241c32", buckle="#8a4cff", boot="#10101a", glove="#14101c", glove_l="#3a3050",
    iris=("#6a0a1c", "#e0203e", "#ffa0b0"), eye_glow="#ff3a5c", sigil="#8a4cff", sigil_g="#8a4cff", fang=False,
)
P2 = dict(
    hair="#0e0a14", hair_rgb=(14, 10, 20),
    skin=("#5e606c", "#7a7c8a", "#989aa8"),
    coat="#241018", coat_l="#4a2030", coat_d="#0e060a", trim="#f2194d", trim_l="#ff8aa6", trim_d="#8a0c2c", rune=(122, 24, 48), collar="#2c141c",
    belt="#2a141c", buckle="#f2194d", boot="#14080c", glove="#180c10", glove_l="#4a2030",
    iris=("#7a0a1c", "#ff2a4a", "#ffc0c8"), eye_glow="#ff5a70", sigil="#f2194d", sigil_g="#f2194d", fang=True,
)


# ================================================================================================ the head
def head_front(cv, P, form2):
    f = cv.face("head", "front")
    sk = P["skin"]
    K.face_base(f, sk, side=0.1, chin=0.09)
    pal = K.hair_pal(P["hair"], light_k=1.55, dark_k=0.5)
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.2), (7.4, 1.9), (6.4, 2.6), (5.4, 1.9), (4.3, 2.5), (3.2, 1.9), (2.0, 2.6), (0.9, 1.9), (-0.3, 2.3)], y_bot=2.4)
    K.strands(f, pal, (0.9, 2.1, 3.3, 4.5, 5.7, 6.9), 0.0, 2.1, mass, a=0.45)
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.9, 1.4, 1.5, 2.2, -0.3)), (0.85, (1.1, 1.4, 1.5, 2.2, 0.3)), (1.0, (5.4, 1.2, 1.7, 1.3, -0.1)), (1.0, (2.6, 1.2, 1.7, 1.3, 0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    for cx, lean in ((0.4, 0.15), (7.6, -0.15)):
        K.lock(f, pal, cx, 0.9, 0.8, 3.4, lean, 1.0)
    # the rune on the brow: a diamond with a hooked cross-line, lit
    s = P["sigil"]
    d = [(4.0, 2.55), (4.5, 3.2), (4.0, 3.9), (3.5, 3.2)]
    f.fill(f.m_poly(d), "#0a0610", 0.95)
    f.fill(f.m_polyline(d + [d[0]], 0.12), s, 0.95, glow=P["sigil_g"])
    f.fill(f.m_line((3.15, 3.25), (4.85, 3.25), 0.1, 0.1), s, 0.9, glow=P["sigil_g"])
    f.fill(f.m_line((4.0, 3.25), (4.0, 3.95), 0.1, 0.05), s, 0.9, glow=P["sigil_g"])
    # hard dark brows, narrow angry eyes
    brow = "#2a2630"
    H.brow(f, (3.5, 4.2), (0.9, 3.45), brow, 0.5, 0.16, arch=0.0)
    H.brow(f, (4.5, 4.2), (7.1, 3.45), brow, 0.5, 0.16, arch=0.0)
    for cx in (2.3, 5.7):
        K.eye_bag(f, cx, 6.05, 2.1, "#6a6a80", 0.4)
        al = H.eye(f, cx, 5.2, 2.6, 1.3, P["iris"], lash="#0a0610", tilt=0.34, lash_w=0.55, flick=0.4, lid_shadow=0.5, hi=0.7, pupil="#1a0408")
        if form2:
            f.fill(f.m_ellipse(cx, 5.22, 0.13, 0.6) * al, "#0a0204", 0.95)
        K.eye_glow(f, al, cx, 5.2, 2.6, 1.3, P["eye_glow"], 0.95)
        f.glow_fill(f.m_ellipse(cx, 5.2, 1.7, 0.9, soft=3.0), P["eye_glow"], 0.22)
    f.fill(f.m_polyline([(4.0, 5.7), (3.88, 6.3), (4.12, 6.45)], 0.15), H.tone(sk[1], 0.62), 0.8)
    if form2:                                                                    # a grin with fangs
        f.fill(f.m_poly([(2.5, 7.0), (4.0, 7.25), (5.5, 7.0), (5.0, 7.75), (4.0, 7.9), (3.0, 7.75)]), "#14040a", 1.0)
        for x, y0 in ((3.0, 7.05), (5.0, 7.05), (3.9, 7.25), (4.2, 7.25)):
            f.fill(f.m_poly([(x - 0.17, y0 + 0.05), (x + 0.17, y0 + 0.05), (x, y0 + (0.75 if x in (3.0, 5.0) else 0.45))]), "#f4eee4", 1.0)
        f.fill(f.m_polyline([(2.5, 7.0), (4.0, 7.25), (5.5, 7.0)], 0.16), "#4a1a24", 0.9)
    else:                                                                        # a thin smirk, higher on one side
        f.fill(f.m_polyline([(3.0, 7.3), (4.0, 7.4), (5.0, 7.25), (5.65, 6.9)], 0.22), "#2a1820", 0.95)
        f.fill(f.m_polyline([(3.4, 7.65), (4.4, 7.7), (5.2, 7.5)], 0.14), H.tone(sk[1], 0.8), 0.45)
    # the lines on the cheeks and the veins at the temples
    for pts in ([(0.55, 4.6), (0.9, 5.6), (0.7, 6.7), (1.2, 7.6)], [(7.45, 4.6), (7.1, 5.6), (7.3, 6.7), (6.8, 7.6)]):
        K.taper_line(f, pts, 0.2, P["sigil"], 0.95, glow=P["sigil_g"], ends=0.3)
    for pts in ([(1.9, 3.2), (1.5, 3.9), (1.7, 4.4)], [(6.1, 3.2), (6.5, 3.9), (6.3, 4.4)]):
        K.taper_line(f, pts, 0.14, H.tone(P["sigil"], 0.8), 0.8, ends=0.4)
    if form2:
        K.cracks_on(f, [[(2.0, 5.9), (1.6, 6.5), (1.9, 7.2)], [(6.0, 5.9), (6.4, 6.5), (6.1, 7.2)], [(4.0, 4.3), (3.8, 4.9)]], P["trim"], glow=P["trim"], width=0.12, core="#ffd0d8", core_a=0.5, dark="#0a0206")


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.5, dark_k=0.5)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.4, 1.0, 2.8, lean, 0.95)


# ================================================================================================ the coat
def runes_on(f, mask, y0, y1, P, seed, a=0.85):
    """Rune strokes on the coat: small hooked glyph marks in the Word's violet (or red) in a loose grid."""
    col = tuple(c / 255.0 for c in P["rune"])
    c = "#%02x%02x%02x" % P["rune"]
    for j in range(int((y1 - y0) / 1.3)):
        for i in range(int(f.w / 1.2)):
            if H.hsh(seed, i, j) < 0.38:
                continue
            x, y = 0.6 + i * 1.2 + 0.2 * H.hsh(seed, i, j, 1), y0 + 0.6 + j * 1.3
            g = int(H.hsh(seed, i, j, 2) * 4)
            pts = ([(x - 0.3, y - 0.4), (x + 0.3, y - 0.4), (x, y + 0.4)], [(x - 0.3, y - 0.4), (x - 0.3, y + 0.4), (x + 0.3, y)], [(x, y - 0.45), (x, y + 0.45), (x + 0.3, y + 0.1)], [(x - 0.3, y), (x + 0.3, y), (x, y - 0.45), (x, y + 0.45)])[g]
            f.fill(f.m_polyline(pts, 0.17) * mask, c, a)


def collar(f, P, nm):
    m = f.m_band(0.0, 1.9)
    K.cloth(f, m, P["collar"], top=1.5, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.26, gain=1.2, spec=0.25)
    f.fill(f.m_rect(0.0, 1.75, f.w, 1.92), P["trim"], 1.0)
    f.fill(f.m_rect(0.0, 1.75, f.w, 1.82), P["trim_l"], 0.7)
    H.shadow_under(f, 1.95, 0.5, 0.3)


def coat_front(f, P, ov, form2):
    cl = P["coat"]
    K.cloth(f, f.m_all(), cl, top=1.7, bottom=0.8, round_=0.14, fold_xs=[0.9, 1.7, 6.3, 7.1], fold_depth=0.12, fold_y0=2.0, fold_y1=9.0)
    # the opening: a bare ashen chest between two lit lapel panels, violet piping and a lit stitched line beside it
    op = f.m_poly([(2.5, 0.0), (5.5, 0.0), (5.75, 9.0), (2.25, 9.0)])
    sk = P["skin"]
    f.fill(op, H.lin((0, 0), (0, 9), [(0, sk[2]), (0.5, sk[1]), (1, H.tone(sk[0], 0.8))]))
    f.fill(f.m_line((4.0, 1.8), (4.0, 8.8), 0.2, 0.2) * op, H.tone(sk[0], 0.8), 0.5)                    # the breastbone
    for sgn in (-1.0, 1.0):
        f.fill(f.m_ellipse(4.0 + sgn * 0.95, 3.2, 0.9, 0.75) * op, H.tone(sk[2], 1.04), 0.45)
        f.fill(f.m_polyline([(4.0 + sgn * 0.1, 3.95), (4.0 + sgn * 0.95, 4.15), (4.0 + sgn * 1.7, 3.9)], 0.2) * op, H.tone(sk[0], 0.7), 0.7)
    for x0, x1, s in ((2.5, 2.25, -1.0), (5.5, 5.75, 1.0)):
        H.trim_line(f, [(x0, 0.0), (x1, 9.0)], P["trim"], width=0.3, spec=0.5, outline=0.6, glow=P["trim"])
        K.seam(f, [(x0 + s * 0.5, 1.2), (x1 + s * 0.55, 9.0)], P["trim_l"], 0.05, 0.5)
    for x0, x1 in ((0.0, 2.1), (5.9, 8.0)):
        pm = f.m_rect(x0, 1.9, x1, 9.0)
        K.sheen(f, pm, [x0 + 0.5 if x0 < 4 else x1 - 0.5], 2.0, 8.7, 0.3, "#8a70c0" if not form2 else "#c06080", width=0.26)
        runes_on(f, pm, 2.0, 8.8, P, 3 if x0 < 4 else 4)
    f.fill(f.m_rect(0.0, 1.9, 0.24, 9.0), P["coat_l"], 0.45)
    f.fill(f.m_rect(7.76, 1.9, 8.0, 9.0), P["coat_d"], 0.5)
    collar(f, P, "front")


def coat_back(f, P, ov, form2):
    K.cloth(f, f.m_all(), P["coat"], top=1.7, bottom=0.8, round_=0.14, fold_xs=[1.4, 2.8, 5.2, 6.6], fold_depth=0.12, fold_y0=2.0, fold_y1=9.0)
    f.fill(f.m_rect(0.0, 1.9, 0.24, 9.0), P["coat_l"], 0.35)
    f.fill(f.m_rect(7.76, 1.9, 8.0, 9.0), P["coat_d"], 0.5)
    runes_on(f, f.m_rect(0.0, 1.9, 8.0, 9.0), 2.0, 8.8, P, 9, 0.5)
    # the big rune emblem between the wings: a ringed glyph that glows
    cx, cy = 4.0, 5.2
    f.fill(f.m_ring(cx, cy, 2.5, 0.28), P["trim"], 0.95, glow=P["trim"])
    f.fill(f.m_ring(cx, cy, 2.0, 0.14), P["trim_l"], 0.8)
    for pts in ([(cx - 1.0, cy - 1.2), (cx + 1.0, cy - 1.2), (cx, cy + 1.3)], [(cx - 1.0, cy + 0.2), (cx + 1.0, cy + 0.2)], [(cx, cy - 1.5), (cx, cy + 1.5)]):
        f.fill(f.m_polyline(pts, 0.22), P["trim_l"], 0.95, glow=P["trim"])
    f.glow_fill(f.m_ellipse(cx, cy, 3.0, 3.0, soft=3.0), P["trim"], 0.25)
    collar(f, P, "back")


def coat_side(f, P, nm):
    K.cloth(f, f.m_all(), P["coat"], top=1.7, bottom=0.8, round_=0.14, fold_xs=[1.2, 2.6], fold_depth=0.12, fold_y0=2.0, fold_y1=9.0)
    ed = f.w - 0.7 if nm == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.8)], P["trim_l"], 0.05, 0.5)
    collar(f, P, nm)


def belts(f, P, front):
    K.belt_band(f, 8.9, 9.6, P["belt"], stitch=P["trim_d"])
    K.belt_band(f, 9.75, 10.45, P["belt"], stitch=P["trim_d"])
    H.trim_line(f, [(0.0, 8.95), (f.w, 8.95)], P["trim"], width=0.14, spec=0.3, outline=0.4)
    if front:
        K.buckle(f, 4.0, 9.65, 2.0, 1.7, P["trim_d"], hole=P["coat_d"])
        f.ellipse(4.0, 9.65, 0.42, 0.42, P["trim_l"], a=1.0, glow=P["buckle"])
        f.ellipse(3.9, 9.5, 0.14, 0.1, "#ffffff", a=0.9)
    else:
        H.stud(f, 4.0, 9.65, 0.22, P["trim_l"], spec=0.8)


def skirt(f, P, nm):
    m = f.m_band(10.45, 12.0)
    K.cloth(f, m, P["coat"], top=1.3, bottom=0.75, round_=0.12, fold_xs=[f.w * 0.2, f.w * 0.5, f.w * 0.8], fold_depth=0.12, fold_y0=10.5, fold_y1=12.0)
    H.shadow_under(f, 10.47, 0.5, 0.3)


def body(cv, P, form2):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                coat_front(f, P, ov, form2)
            elif nm == "back":
                coat_back(f, P, ov, form2)
            else:
                coat_side(f, P, nm)
            belts(f, P, nm == "front")
            skirt(f, P, nm)


# ================================================================================================ arms and legs
def arm(cv, part, P, form2):
    sk = P["skin"]
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            K.limb_skin(f, sk, 0.0, 8.4, tendon=True)
            f.fill(f.m_ellipse(f.w * 0.45, 1.9, f.w * 0.34, 1.3), H.tone(sk[2], 1.04), 0.45)
            f.fill(f.m_polyline([(0.2, 5.0), (f.w * 0.5, 5.25), (f.w - 0.2, 5.0)], 0.16), H.tone(sk[0], 0.72), 0.55)
            for pts in ([(f.w * 0.35, 2.4), (f.w * 0.3, 3.6), (f.w * 0.4, 4.5)], [(f.w * 0.6, 5.8), (f.w * 0.55, 6.8)]):          # veins
                K.taper_line(f, pts, 0.12, H.tone(P["trim_d"], 0.9), 0.55, ends=0.4)
            # the violet armband above the elbow and the muscle of the upper arm
            if f.w >= 3:
                f.fill(f.m_band(3.5, 4.5), H.lin((0, 3.5), (0, 4.5), [(0, P["coat_l"]), (1, P["coat_d"])]), 1.0)
                f.fill(f.m_rect(0.0, 3.5, f.w, 3.64), P["trim"], 1.0, glow=P["trim"])
                f.fill(f.m_rect(0.0, 4.4, f.w, 4.5), P["trim_d"], 0.9)
                H.shadow_under(f, 4.5, 0.5, 0.3)
            f.fill(f.m_ellipse(f.w * 0.5, 7.0, f.w * 0.3, 1.2), H.tone(sk[2], 1.02), 0.3)
            # the glove: black leather with a lit cuff and knuckle line, finger gaps
            K.glove(f, P["glove"], 8.4, 12.0, nm, finger_n=3, cuff_color=P["coat_l"], cuff_h=0.8, knuckle_k=2.0)
            H.trim_line(f, [(0.0, 8.45), (f.w, 8.45)], P["trim"], width=0.14, spec=0.3, outline=0.4)
    for ov in (False, True):
        K.cap(cv, part, "top", sk[1], edge=None, k_top=1.1, overlay=ov)


def legs(cv, P, form2):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 11.0)
                K.cloth(f, m, P["coat"], top=1.5, bottom=0.78, round_=0.14, fold_xs=[0.9, 1.9, 3.0], fold_depth=0.14, fold_y0=0.2, fold_y1=10.4)
                runes_on(f, m, 0.5, 10.0, P, 21 + SIDES.index(nm), 0.55)
                f.fill(f.m_rect(0.0, 0.0, 0.26, 10.8), P["coat_l"], 0.3)
                H.shadow_under(f, 0.0, 0.6, 0.25)
                f.fill(f.m_band(10.2, 11.0), H.lin((0, 10.2), (0, 11.0), [(0, P["trim_d"]), (1, H.tone(P["trim_d"], 0.6))]), 1.0)
                f.fill(f.m_rect(0.0, 10.2, f.w, 10.38), P["trim"], 1.0, glow=P["trim"])
                H.shadow_under(f, 11.0, 0.6, 0.34)
                f.fill(f.m_band(11.0, 12.0), H.lin((0, 11.0), (0, 12), [(0, K.lit(P["boot"], 2.0)), (0.3, P["boot"]), (1, "#050508")]))
                f.fill(f.m_band(11.0, 11.14), P["trim_l"], 0.5)
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", "#050508", edge=None, k_top=1.0, overlay=ov)


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
    legs(cv, P, form2)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim)
