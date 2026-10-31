"""Rhya, the Disloyal of the Third Eye, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for both forms: `rhya_form2.py` is the same code with the elf palette (K.is_form2).

What he wears is the 64x64 sheet's outfit (tools/arc/char_rhya.py): the light hooded robe of the Eye of the Midnight Sun (a cream coat with dark trim and a stand collar, knee long) over a dark shirt,
long sleeves with dark cuffs, a double belt with a pale buckle, dark trousers and calf boots. The hood, the cloak and the borrowed grimoire are the `pmx_rhya*` extras: the hood sits behind the neck,
so the back of the body keeps its clean coat panel and the front of the belt is free of any book drawing. Slim arms, height scale and glow semantics are the sheet's.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): sharp half-lidded black eyes with violet sheen under thin dark brows, a one-sided smirk, the red
tattoo on the character's left cheek (curled strokes with a dot) and a fringe of black locks with the white patches of his hair; the coat with lit panels, folds, dark piping, a stand collar with
its lining, lapel flaps, a double belt and buckle, long sleeves with stitched dark cuffs, bare hands with knuckles, trousers and boots with a dark cuff and a strap. Form 2 (the elf awake): white
hair with black patches, gold glowing eyes, the tattoo reaching over the eye and down the jaw, golden cheek lines (glow) and the third eye open on the forehead (gold, glowing; the lower half shows
below the fringe), a cleaner coat with gold piping.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia are blocked by the session's proxy); from memory of the series plus docs/arc/rhya.md and
the sheet: messy black hair with white patches, black eyes, the red tattoo on the left cheek, the hidden third eye. The coat is the mod's addition (the wiki gives no outfit).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE_1 = "anime_short"                                   # a messy black cut that leaves the eyes free
HAIR_STYLE_2 = "anime_spiky"                                   # white spikes in clumps (hair_map)

P1 = dict(
    hair="#1c1c28", hair_rgb=(28, 28, 40), patch="#e8e8f2",
    skin=("#e6c4b6", "#f6dccc", "#fff0e6"),
    coat="#cfc9b4", coat_l="#efe9d6", coat_d="#8c8672", trim="#2a2a38", trim_l="#4c4c64", trim_d="#14141c", pipe="#8e8aa6",
    shirt="#2a2a38", belt="#3c3044", buckle="#c4c4dc", boot="#1c1c24", sole="#0a0a0e", strap="#4a3c52",
    iris=("#0a0a12", "#26263e", "#6a6a9a"), eye_glow=None, tat="#c42434", tat_l="#ee5a62",
)
P2 = dict(
    hair="#eceef8", hair_rgb=(236, 238, 248), patch="#1c1c28",
    skin=("#e6c8bc", "#f8e2d4", "#fff4ec"),
    coat="#dcd6c6", coat_l="#fffaf0", coat_d="#9a9486", trim="#2c2a3a", trim_l="#4e4c66", trim_d="#14141c", pipe="#e0b848",
    shirt="#2c2a3a", belt="#3c3044", buckle="#f0d070", boot="#1c1c24", sole="#0a0a0e", strap="#4a3c52",
    iris=("#8a5a10", "#f2c048", "#fff0b0"), eye_glow="#ffd35a", tat="#d02a38", tat_l="#ff6a70",
)


# ================================================================================================ the head
def tattoo(f, P, form2):
    """The red tattoo on the character's left cheek (the viewer's right): curled strokes under the eye, a dot; in form 2 it reaches over the eye and down the jaw."""
    c, cl = P["tat"], P["tat_l"]
    strokes = [[(6.15, 5.9), (6.7, 6.4), (6.95, 7.2)], [(5.75, 6.3), (6.3, 6.85), (6.35, 7.6)], [(7.1, 5.5), (7.4, 6.2)]]
    if form2:
        strokes += [[(7.5, 3.9), (6.9, 4.35), (6.2, 4.15), (5.6, 4.5)], [(6.9, 7.3), (6.2, 7.85), (5.2, 7.95)], [(7.3, 6.7), (7.0, 7.5)]]
    for s in strokes:
        f.fill(f.m_polyline(s, 0.34), H.tone(c, 0.55), 0.75)
        f.fill(f.m_polyline(s, 0.24), c, 0.98)
        f.fill(f.m_polyline(s[: max(2, len(s) - 1)], 0.1), cl, 0.8)
    f.fill(f.m_ellipse(6.55, 5.75, 0.26, 0.2), c, 0.98)
    f.fill(f.m_ellipse(6.5, 5.7, 0.1, 0.08), cl, 0.9)
    if form2:                                                   # cheek lines of the elf: thin gold lines that glow, under the eyes
        for p in ([(0.8, 6.1), (1.5, 6.9), (2.2, 7.5)], [(1.4, 5.9), (2.0, 6.5)]):
            K.glow_line(f, p, "#ffd35a", 0.16, 0.8, core="#fff4c0", core_a=0.5)


def third_eye(f, P):
    """The open third eye of form 2 on the brow line: a gold almond with a slit pupil; the lower half shows under the fringe."""
    cx, cy, w, h = 4.0, 3.25, 1.75, 1.0
    al = H.eye(f, cx, cy, w, h, P["iris"], lash="#3a2408", tilt=0.0, lash_w=0.3, flick=0.0, lid_shadow=0.25, hi=0.7, outer=1.0)
    K.eye_glow(f, al, cx, cy, w, h, "#ffb347", 0.95)
    f.glow_fill(f.m_ellipse(cx, cy, w * 0.62, h * 0.9, soft=3.0), "#ff9a30", 0.35)


def head_front(cv, P, form2):
    f = cv.face("head", "front")
    K.face_base(f, P["skin"], side=0.08, chin=0.07)
    pal = K.hair_pal(P["hair"], light_k=1.18, dark_k=0.55)
    pat = K.hair_pal(P["patch"], light_k=1.05, dark_k=0.8)
    # the fringe: a messy mass of locks over the brow, with the patches of the other colour
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.5, 2.0), (6.4, 2.6), (5.3, 2.0), (4.2, 2.7), (3.0, 2.0), (1.8, 2.5), (0.8, 1.9), (-0.3, 2.4)], y_bot=2.4)
    K.strands(f, pal, (0.9, 2.1, 3.3, 4.5, 5.7, 6.9), 0.0, 2.0, mass, a=0.4)
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.6, 1.5, 1.5, 2.0, -0.35)), (0.85, (1.4, 1.5, 1.5, 2.0, 0.35)), (0.9, (4.3, 1.4, 1.6, 2.1, 0.25)), (1.0, (3.0, 1.3, 1.6, 1.5, -0.1)), (1.0, (5.6, 1.2, 1.6, 1.4, -0.2))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    for cx, lean in ((0.4, 0.15), (7.6, -0.15)):
        K.lock(f, pal, cx, 0.9, 0.8, 3.5, lean, 1.0)
    for cx, w, top, ln, lean in ((2.2, 1.0, 0.6, 2.4, 0.2), (5.0, 0.8, 0.6, 2.1, -0.1)):          # the white (or in form 2 black) patches: locks of the other colour
        K.lock(f, pat, cx, w, top, ln, lean, 1.0)
    # thin dark brows, a little slanted
    brow = "#14141c" if not form2 else "#8a8aa0"
    H.brow(f, (3.45, 3.95), (1.0, 3.55), brow, 0.3, 0.12, arch=0.04)
    H.brow(f, (4.55, 3.95), (7.0, 3.55), brow, 0.3, 0.12, arch=0.04)
    # sharp half-lidded eyes
    for cx in (2.35, 5.65):
        K.eye_bag(f, cx, 6.0, 2.0, "#a8909c" if not form2 else "#c0a498", 0.3)
        al = H.eye(f, cx, 5.1, 2.5, 1.6, P["iris"], lash="#08080e" if not form2 else "#3a2a10", tilt=0.3, lash_w=0.5, flick=0.46, lid_shadow=0.45, hi=0.85)
        if P["eye_glow"]:
            K.eye_glow(f, al, cx, 5.1, 2.5, 1.6, P["eye_glow"], 0.9)
    H.nose(f, 4.0, 6.3, "#cc9c90", 0.22)
    # a one-sided smirk, raised on the tattoo side
    f.fill(f.m_polyline([(3.0, 7.2), (4.0, 7.3), (5.0, 7.12), (5.7, 6.75)], 0.26), "#8a4a4e", 0.95)
    f.fill(f.m_polyline([(3.4, 7.55), (4.3, 7.6), (5.2, 7.45)], 0.18), "#d49a96", 0.45)
    f.fill(f.m_polyline([(5.75, 6.7), (5.95, 6.55)], 0.2), "#8a4a4e", 0.8)
    tattoo(f, P, form2)
    if form2:
        third_eye(f, P)


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.2, dark_k=0.58)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.5, 1.0, 2.8, lean, 0.95)


# ================================================================================================ the coat
def coat_front(f, P, ov):
    """The open robe: two lit cream panels, a dark shirt in the opening, piping, lapel flaps, a stand collar; the belt crosses at y 8.7..10.3."""
    cl = P["coat"]
    K.cloth(f, f.m_all(), cl, top=1.08, bottom=0.86, round_=0.12, fold_xs=[1.2, 2.3, 5.7, 6.8], fold_depth=0.09, fold_y0=1.8, fold_y1=8.6)
    for x0, x1 in ((0.0, 3.0), (5.0, 8.0)):
        K.sheen(f, f.m_rect(x0, 1.8, x1, 8.6), [x0 + 0.7 if x0 < 4 else x1 - 0.7], 2.0, 8.4, 0.2, "#ffffff", width=0.3)
    f.fill(f.m_rect(0.0, 1.8, 0.28, 8.6), P["coat_l"], 0.35)
    f.fill(f.m_rect(7.72, 1.8, 8.0, 8.6), P["coat_d"], 0.45)
    # the opening: a dark shirt with a centre seam, a stitched hem of the coat on both edges
    op = f.m_poly([(3.05, 0.0), (4.95, 0.0), (5.25, 9.0), (2.75, 9.0)])
    f.fill(op, H.lin((0, 0), (0, 9), [(0, H.tone(P["shirt"], 1.15)), (1, H.tone(P["shirt"], 0.7))]))
    f.fill(f.m_line((4.0, 1.9), (4.0, 9.0), 0.18, 0.18) * op, H.tone(P["shirt"], 1.6), 0.45)
    for y in (3.0, 4.6, 6.2, 7.8):
        H.stud(f, 4.0, y, 0.18, P["pipe"], spec=0.8)
    for x0, x1, s in ((3.05, 2.75, -1.0), (4.95, 5.25, 1.0)):
        H.trim_line(f, [(x0, 1.0), (x1, 9.0)], P["trim"], width=0.34, spec=0.35, outline=0.6)
        K.seam(f, [(x0 + s * 0.44, 1.4), (x1 + s * 0.5, 9.0)], P["pipe"], 0.06, 0.7)
    # the lapel flaps and the stand collar
    for sgn in (-1.0, 1.0):
        fl = f.m_poly([(4.0 + sgn * 0.95, 0.0), (4.0 + sgn * 2.5, 0.0), (4.0 + sgn * 2.2, 1.7), (4.0 + sgn * 1.0, 2.4)])
        f.fill(fl, H.lin((0, 0), (0, 2.4), [(0, P["coat_l"]), (1, H.tone(cl, 0.9))]))
        f.fill(f.m_polyline([(4.0 + sgn * 1.0, 2.4), (4.0 + sgn * 2.2, 1.7), (4.0 + sgn * 2.5, 0.0)], 0.26), P["trim"], 0.95)
        K.seam(f, [(4.0 + sgn * 1.05, 2.0), (4.0 + sgn * 2.0, 1.5)], P["pipe"], 0.05, 0.6)
    H.shadow_under(f, 2.5, 0.5, 0.2, x0=1.4, x1=2.9)
    H.shadow_under(f, 2.5, 0.5, 0.2, x0=5.1, x1=6.6)


def belts(f, P, front):
    K.belt_band(f, 8.7, 9.45, P["belt"], stitch=P["strap"])
    K.belt_band(f, 9.6, 10.3, P["belt"], stitch=P["strap"])
    if front:
        K.buckle(f, 4.0, 9.5, 2.1, 1.7, P["buckle"], hole=P["shirt"])
    else:
        H.stud(f, 4.0, 9.5, 0.2, P["buckle"], spec=0.7)


def coat_back(f, P, ov):
    cl = P["coat"]
    K.cloth(f, f.m_all(), cl, top=1.08, bottom=0.84, round_=0.12, fold_xs=[1.2, 2.4, 5.6, 6.8], fold_depth=0.10, fold_y0=1.8, fold_y1=8.6)
    f.fill(f.m_rect(0.0, 0.0, 8.0, 1.5), H.lin((0, 0), (0, 1.5), [(0, P["coat_l"]), (1, cl)]))
    f.fill(f.m_rect(0.0, 1.45, 8.0, 1.6), P["trim"], 0.9)
    K.seam(f, [(0.2, 1.1), (7.8, 1.1)], P["pipe"], 0.05, 0.6)
    f.fill(f.m_line((4.0, 1.7), (4.0, 12.0), 0.2, 0.2), P["coat_d"], 0.75)                  # the centre seam of the coat and its vent
    K.seam(f, [(3.6, 1.8), (3.6, 8.6)], P["pipe"], 0.05, 0.4)
    K.sheen(f, f.m_rect(0, 1.8, 8, 8.6), [1.5, 6.4], 2.0, 8.4, 0.16, "#ffffff", width=0.3)


def coat_side(f, P, name):
    K.cloth(f, f.m_all(), P["coat"], top=1.08, bottom=0.84, round_=0.12, fold_xs=[1.2, 2.6], fold_depth=0.09, fold_y0=1.8, fold_y1=8.6)
    f.fill(f.m_rect(0.0, 0.0, f.w, 1.5), P["coat_l"], 0.7)
    f.fill(f.m_rect(0.0, 1.45, f.w, 1.6), P["trim"], 0.9)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 1.8), (ed, 8.6)], P["pipe"], 0.05, 0.5)


def skirt(f, P, name):
    """The coat's hem over the hips: the coat continues on the body to y 12 (an open skirt in front)."""
    m = f.m_band(10.3, 12.0)
    K.cloth(f, m, P["coat"], top=1.04, bottom=0.8, round_=0.1, fold_xs=[f.w * 0.25, f.w * 0.75], fold_depth=0.1, fold_y0=10.4, fold_y1=12.0)
    H.shadow_under(f, 10.32, 0.5, 0.28)
    if name == "front":
        f.fill(f.m_poly([(3.1, 10.3), (4.9, 10.3), (5.2, 12.0), (2.8, 12.0)]), H.lin((0, 10.3), (0, 12), [(0, H.tone(P["shirt"], 0.9)), (1, H.tone(P["shirt"], 0.6))]))
        for x0, x1 in ((3.1, 2.8), (4.9, 5.2)):
            H.trim_line(f, [(x0, 10.3), (x1, 12.0)], P["trim"], width=0.3, spec=0.3, outline=0.5)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                coat_front(f, P, ov)
            elif nm == "back":
                coat_back(f, P, ov)
            else:
                coat_side(f, P, nm)
            belts(f, P, nm == "front")
            skirt(f, P, nm)


# ================================================================================================ arms and legs
def sleeve(f, P, nm, part):
    cl = P["coat"]
    m = f.m_band(0.0, 9.5)
    K.cloth(f, m, cl, top=1.1, bottom=0.84, round_=0.16, fold_xs=[0.7, 1.7], fold_depth=0.12, fold_y0=1.4, fold_y1=9.0)
    K.sheen(f, m, [f.w * 0.3], 1.4, 9.0, 0.2, "#ffffff", width=0.26)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.5), P["coat_l"], 0.7)                                 # the shoulder seam, lit
    K.seam(f, [(0.0, 0.62), (f.w, 0.62)], P["coat_d"], 0.05, 0.6)
    if nm in ("west", "east"):
        K.seam(f, [(f.w * 0.5, 0.8), (f.w * 0.5, 9.0)], P["coat_d"], 0.05, 0.5)
    for y, k in ((5.0, 0.5), (5.55, 0.32)):                                                # the elbow
        f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.26), (f.w, y + 0.05)], 0.14) * m, P["coat_d"], k)
    f.fill(f.m_polyline([(0.0, 4.8), (f.w * 0.5, 5.05), (f.w, 4.85)], 0.2) * m, P["coat_l"], 0.3)
    # the dark cuff with a pipe, a turned edge and two studs
    K.cuff(f, 9.2, 10.4, P["trim"], pipe=P["pipe"], stud_color=P["pipe"], spec=0.3)
    # the bare hand: knuckles, finger gaps
    sk = P["skin"]
    K.limb_skin(f, sk, 10.4, 12.0, tendon=False)
    K.fingers(f, sk, 11.0, 11.95, n=2 if f.w < 4 else 3)
    H.shadow_under(f, 10.45, 0.6, 0.3)


def arm(cv, part, P):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve(f, P, nm, part)
    for ov in (False, True):
        K.cap(cv, part, "top", P["coat"], edge=P["trim"], k_top=1.15, overlay=ov)


def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 7.1)
                K.cloth(f, m, P["coat"], top=1.04, bottom=0.82, round_=0.14, fold_xs=[1.0, 2.9], fold_depth=0.11, fold_y0=0.2, fold_y1=6.5)
                H.shadow_under(f, 0.0, 0.6, 0.25)
                if nm == "front":
                    f.fill(f.m_rect(0.0, 0.0, 0.3, 6.8), P["coat_l"], 0.3)
                f.fill(f.m_band(6.35, 7.1), P["trim"], 1.0)                                  # the dark hem of the robe
                f.fill(f.m_band(6.35, 6.5), P["trim_l"], 0.7)
                K.seam(f, [(0.12, 6.78), (f.w - 0.12, 6.78)], P["pipe"], 0.05, 0.6)
                H.shadow_under(f, 7.15, 0.7, 0.34)
                K.boot(f, P["boot"], 7.15, nm, strap=True, strap_color=P["strap"], gold=P["buckle"], cuff_color=P["trim_l"], cuff_h=0.9, welt=P["trim"], sole=P["sole"])
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", P["sole"], edge=None, k_top=1.0, overlay=ov)


# ================================================================================================ paint
def paint(spec, cid, masks):
    form2 = K.is_form2(cid)
    P = P2 if form2 else P1
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE_2 if form2 else HAIR_STYLE_1, P["hair"], P["hair_rgb"])
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"])
    head_sides(cv, P)
    head_front(cv, P, form2)
    body(cv, P)
    arm(cv, "arm_r", P)
    arm(cv, "arm_l", P)
    legs(cv, P)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim)
