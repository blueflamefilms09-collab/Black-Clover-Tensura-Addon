"""Mars, the Diamond Kingdom's crystal weapon, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for both forms: `mars_form2.py` is the same code with the Gates palette (K.is_form2).

What he wears is the 64x64 sheet's outfit (tools/arc/char_mars.py): a dark indigo uniform with a lavender trim and a stand collar, long sleeves with lavender cuffs, a plain belt with a lavender
buckle, dark trousers, calf boots, dark gloves, and the Nemean Armor of lavender crystal (pauldrons, bracers, greaves); the diamond of his kingdom on the back; three glowing mage stones (forehead,
chest, back of the left forearm). The crystal sword and the orbiting crystal are the `pmx_mars*` extras, and so are the chest stone and the forearm stone (boxes in front of the chest and on the
forearm): the chest of the skin is kept as a quiet dark plate with a lavender frame so the extra stone sits on it, and the back of the left forearm is left plain. Wide arms, the height scale and the
glow semantics (eyes, forehead stone, cracks, trim, the back emblem) are the sheet's.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): narrow cold eyes with glowing irises under flat pale brows, a flat mouth, a cracked face (jagged
lines that glow) and the stone in the forehead; a fringe of pale locks with a lavender streak; the uniform with a lit shoulder line, piping, folds, stand collar, a plain belt with a faceted
buckle; pauldrons, bracers and greaves of faceted crystal (strips of alternating tone, a lit top edge, a jagged lower border, a soft glow); gloves with knuckles; boots with a cuff; the big
faceted diamond on the back with a glowing rim. Form 2 (the Gates, the fire of Fana): the crystal turns warm (cream and amber), the stones and cracks burn orange, the hair is warm white.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia are blocked by the session's proxy); from memory of the series plus docs/arc/mars.md
and the sheet (the search digests of the repo could not confirm his hair, eyes or clothes: everything but the crystal and the stones is the mod's own).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE_1 = "anime_spiky"                                    # pale thick clumps (hair_map)
HAIR_STYLE_2 = "anime_spike_tall"

P1 = dict(
    hair="#dcd8ee", hair_rgb=(220, 216, 238), streak="#a48ce8",
    skin=("#dcc4cc", "#f0dde0", "#fff2f2"),
    cloth="#2c2c4a", cloth_l="#46466e", cloth_d="#15152a", trim="#a48ce8", trim_l="#d8ccff", trim_d="#5a4a9a", pants="#20203a", boot="#2a2a42", glove="#2c2c44", belt="#36365a",
    crystal="#8c74e0", crystal_l="#d4c6ff", crystal_d="#3e2c92", edge="#f2f8ff", glow="#a48ce8",
    iris=("#4a3a9a", "#a48ce8", "#e6dcff"), eye_glow="#c8b8ff", crack="#8a78c8", crack_g="#b8a4ff", stone="#d8ccff", stone_g="#a48ce8",
)
P2 = dict(
    hair="#fff0d8", hair_rgb=(255, 240, 216), streak="#ff9a40",
    skin=("#e6c4b4", "#f6dccc", "#fff0e4"),
    cloth="#34304a", cloth_l="#524a6e", cloth_d="#18142a", trim="#ff9a40", trim_l="#ffd890", trim_d="#a8501a", pants="#241e3a", boot="#2e2a42", glove="#322c44", belt="#3c3454",
    crystal="#f0b45c", crystal_l="#ffeab8", crystal_d="#a05818", edge="#fffaf0", glow="#ff9a40",
    iris=("#a04a10", "#ffb060", "#fff0c0"), eye_glow="#ffd080", crack="#d27a2a", crack_g="#ffb060", stone="#ffe0b0", stone_g="#ff9a40",
)


# ================================================================================================ the head
def head_front(cv, P, form2):
    f = cv.face("head", "front")
    K.face_base(f, P["skin"], side=0.08, chin=0.07)
    pal = K.hair_pal(P["hair"], light_k=1.06, dark_k=0.66)
    st = K.hair_pal(P["streak"], light_k=1.15, dark_k=0.7)
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.2), (7.4, 2.0), (6.3, 2.5), (5.2, 1.9), (4.1, 2.6), (3.0, 1.9), (1.9, 2.4), (0.9, 1.9), (-0.3, 2.3)], y_bot=2.3)
    K.strands(f, pal, (0.9, 2.1, 3.3, 4.5, 5.7, 6.9), 0.0, 2.0, mass, a=0.4)
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.7, 1.4, 1.4, 2.2, -0.3)), (0.85, (1.3, 1.4, 1.4, 2.2, 0.3)), (1.0, (3.2, 1.3, 1.6, 1.5, -0.1)), (1.0, (5.2, 1.3, 1.6, 1.6, 0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, st, 4.2, 0.9, 1.0, 2.3, 0.1, 1.0)                                                # the lavender streak in the middle of the fringe
    for cx, lean in ((0.4, 0.15), (7.6, -0.15)):
        K.lock(f, pal, cx, 0.9, 0.8, 3.4, lean, 1.0)
    # the stone in the forehead (the lower half shows under the fringe) with the cracks that run from it over the face
    H.gem(f, 4.0, 3.0, 0.5, P["stone"], setting=P["trim_d"], facets=6, spec=0.9, glow=P["stone_g"])
    K.cracks_on(f, [[(3.6, 3.55), (3.1, 4.0), (3.35, 4.45)], [(4.4, 3.55), (4.95, 3.95), (4.7, 4.4)], [(7.4, 4.4), (6.95, 5.2), (7.25, 5.9), (6.8, 6.7)], [(0.65, 4.9), (1.0, 5.6), (0.7, 6.4)],
                    [(6.95, 5.2), (6.4, 5.4)]], P["crack"], glow=P["crack_g"], width=0.14, core="#ffffff", core_a=0.45)
    # flat pale brows, cold narrow eyes
    brow = "#a6a2c0" if not form2 else "#d8b890"
    H.brow(f, (3.5, 3.9), (0.95, 3.8), brow, 0.3, 0.12, arch=0.0)
    H.brow(f, (4.5, 3.9), (7.05, 3.8), brow, 0.3, 0.12, arch=0.0)
    for cx in (2.35, 5.65):
        K.eye_bag(f, cx, 6.0, 1.9, "#bca6c6" if not form2 else "#d4aa98", 0.28)
        al = H.eye(f, cx, 5.1, 2.5, 1.35, P["iris"], lash="#2a2446" if not form2 else "#5a3a18", tilt=0.2, lash_w=0.42, flick=0.38, lid_shadow=0.4, hi=0.85, glow=None)
        K.eye_glow(f, al, cx, 5.1, 2.5, 1.35, P["eye_glow"], 0.95)
    H.nose(f, 4.0, 6.25, "#c8a0a4", 0.2)
    f.fill(f.m_polyline([(3.15, 7.2), (4.0, 7.25), (4.85, 7.2)], 0.22), "#8a6670", 0.95)
    f.fill(f.m_polyline([(3.5, 7.5), (4.5, 7.5)], 0.16), "#d4a8ae", 0.4)


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.1, dark_k=0.62)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.4, 1.0, 2.6, lean, 0.95)


# ================================================================================================ the uniform
def collar(f, P, nm):
    m = f.m_band(0.0, 1.7)
    K.cloth(f, m, P["cloth"], top=1.3, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.22, gain=1.15, spec=0.2)
    f.fill(f.m_rect(0.0, 1.55, f.w, 1.72), P["trim"], 1.0)
    f.fill(f.m_rect(0.0, 1.55, f.w, 1.62), P["trim_l"], 0.7)
    H.shadow_under(f, 1.75, 0.5, 0.3)


def uniform_front(f, P, ov):
    cl = P["cloth"]
    K.cloth(f, f.m_all(), cl, top=1.3, bottom=0.8, round_=0.14, fold_xs=[1.2, 6.8], fold_depth=0.1, fold_y0=2.0, fold_y1=8.6)
    for x0, x1 in ((0.0, 2.4), (5.6, 8.0)):
        K.sheen(f, f.m_rect(x0, 1.9, x1, 8.6), [x0 + 0.6 if x0 < 4 else x1 - 0.6], 2.0, 8.4, 0.24, "#aab0e8", width=0.28)
    f.fill(f.m_rect(0.0, 1.9, 0.26, 8.7), P["cloth_l"], 0.4)
    f.fill(f.m_rect(7.74, 1.9, 8.0, 8.7), P["cloth_d"], 0.5)
    # the chest plate under the stone (the stone is an extra box in front of it): a dark plate with a lavender frame and four corner studs
    plate = f.m_rect(2.15, 2.5, 5.85, 6.7, r=0.3)
    f.fill(plate, H.lin((0, 2.5), (0, 6.7), [(0, H.tone(cl, 0.7)), (1, H.tone(cl, 0.5))]))
    f.fill(f.m_ring(4.0, 4.6, 2.0, 0.2) * plate, P["trim_d"], 0.0)
    H.trim_line(f, [(2.15, 2.8), (2.15, 6.4)], P["trim"], width=0.22, spec=0.5, outline=0.5)
    H.trim_line(f, [(5.85, 2.8), (5.85, 6.4)], P["trim"], width=0.22, spec=0.5, outline=0.5)
    H.trim_line(f, [(2.4, 2.5), (5.6, 2.5)], P["trim"], width=0.22, spec=0.5, outline=0.5)
    H.trim_line(f, [(2.4, 6.7), (5.6, 6.7)], P["trim"], width=0.22, spec=0.5, outline=0.5)
    for x, y in ((2.55, 2.9), (5.45, 2.9), (2.55, 6.3), (5.45, 6.3)):
        H.stud(f, x, y, 0.16, P["trim_l"], spec=0.9)
    # a lavender placket line below the plate and diagonal piping from the collar to the plate corners
    H.trim_line(f, [(4.0, 6.7), (4.0, 8.7)], P["trim"], width=0.26, spec=0.4, outline=0.5)
    for x0, x1 in ((1.0, 2.15), (7.0, 5.85)):
        K.seam(f, [(x0, 1.7), (x1, 2.5)], P["trim"], 0.06, 0.8)
    H.shadow_under(f, 6.75, 0.5, 0.25, x0=2.2, x1=5.8)
    collar(f, P, "front")


def uniform_back(f, P, ov):
    cl = P["cloth"]
    K.cloth(f, f.m_all(), cl, top=1.3, bottom=0.8, round_=0.14, fold_xs=[1.4, 6.6], fold_depth=0.1, fold_y0=2.0, fold_y1=8.6)
    f.fill(f.m_rect(0.0, 1.9, 0.26, 8.7), P["cloth_l"], 0.3)
    f.fill(f.m_rect(7.74, 1.9, 8.0, 8.7), P["cloth_d"], 0.5)
    H.trim_line(f, [(0.0, 8.5), (8.0, 8.5)], P["trim"], width=0.2, spec=0.3, outline=0.5)
    # the diamond of the Diamond Kingdom: big, faceted, with a glowing rim
    K.diamond(f, 4.0, 5.1, 2.7, 3.2, P["crystal"], edge=P["edge"], glow=P["glow"], seed=3, hot=P["stone_g"])
    collar(f, P, "back")


def uniform_side(f, P, nm):
    K.cloth(f, f.m_all(), P["cloth"], top=1.3, bottom=0.8, round_=0.14, fold_xs=[1.2, 2.6], fold_depth=0.1, fold_y0=2.0, fold_y1=8.6)
    ed = f.w - 0.7 if nm == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.6)], P["trim"], 0.06, 0.6)
    collar(f, P, nm)


def belt(f, P, front):
    K.belt_band(f, 8.7, 9.9, P["belt"], stitch=P["trim_d"])
    H.trim_line(f, [(0.0, 8.76), (f.w, 8.76)], P["trim"], width=0.18, spec=0.3, outline=0.4)
    if front:
        K.buckle(f, 4.0, 9.3, 2.2, 1.5, P["trim"], hole=P["cloth_d"])
        f.ellipse(4.0, 9.3, 0.3, 0.3, P["stone"], a=0.9, glow=P["stone_g"])
    else:
        H.stud(f, 4.0, 9.3, 0.2, P["trim_l"], spec=0.8)
    m = f.m_band(9.9, 12.0)
    f.fill(m, H.lin((0, 9.9), (0, 12), [(0, K.lit(P["cloth"], 1.05)), (1, H.tone(P["cloth"], 0.7))]))
    H.folds(f, m, [f.w * 0.3, f.w * 0.7], depth=0.12, width=0.25, y0=10.0, y1=12.0)
    H.shadow_under(f, 9.95, 0.5, 0.3)
    f.fill(f.m_rect(0.0, 11.7, f.w, 12.0), P["trim_d"], 0.85)
    f.fill(f.m_rect(0.0, 11.7, f.w, 11.78), P["trim"], 0.8)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                uniform_front(f, P, ov)
            elif nm == "back":
                uniform_back(f, P, ov)
            else:
                uniform_side(f, P, nm)
            belt(f, P, nm == "front")


# ================================================================================================ arms and legs
def arm(cv, part, P):
    sk = P["skin"]
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            m = f.m_band(0.0, 10.0)
            K.cloth(f, m, P["cloth"], top=1.3, bottom=0.8, round_=0.18, fold_xs=[0.9, 2.2], fold_depth=0.12, fold_y0=3.0, fold_y1=9.2)
            K.sheen(f, m, [f.w * 0.3], 3.0, 9.0, 0.2, "#aab0e8", width=0.26)
            for y, k in ((4.6, 0.5), (5.1, 0.32)):                                               # the elbow
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.26), (f.w, y + 0.05)], 0.14) * m, P["cloth_d"], k)
            if nm in ("west", "east"):
                K.seam(f, [(f.w * 0.5, 3.4), (f.w * 0.5, 6.2)], P["trim_d"], 0.05, 0.5)
            # the pauldron: a plate of crystal with a lit top, a jagged lower border and a glow
            crystal_pauldron(f, P)
            # the bracer: a band of crystal below the elbow with the lavender cuff under it
            K.crystal_band(f, 6.4, 9.6, P["crystal"], P["crystal_l"], P["crystal_d"], edge=P["edge"], n=3, jag=0.35, seed=7, glow=P["glow"], glow_a=0.25)
            H.shadow_under(f, 9.95, 0.6, 0.3)
            # the glove
            K.glove(f, P["glove"], 9.9, 12.0, nm, finger_n=3, cuff_color=P["trim"], cuff_h=0.5, knuckle_k=1.35)
            H.shadow_under(f, 6.4, 0.5, 0.2)
    for ov in (False, True):
        K.cap(cv, part, "top", P["crystal"], edge=P["crystal_d"], k_top=1.25, overlay=ov)


def crystal_pauldron(f, P):
    K.crystal_band(f, 0.0, 3.0, P["crystal"], P["crystal_l"], P["crystal_d"], edge=P["edge"], n=3, jag=0.55, seed=2, glow=P["glow"], glow_a=0.35)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), P["edge"], 0.45)


def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, P["pants"], 0.0, 7.0, nm, knee=True, top=1.35, bottom=0.8, seam_color=P["trim_d"])
                H.shadow_under(f, 0.0, 0.6, 0.25)
                # the boot and the greave over it: crystal plates from the knee guard to the ankle
                K.boot(f, P["boot"], 6.9, nm, strap=False, cuff_color=P["trim_d"], cuff_h=0.5, welt=P["trim_d"], sole="#0a0a14", toe=False)
                K.crystal_band(f, 7.4, 10.5, P["crystal"], P["crystal_l"], P["crystal_d"], edge=P["edge"], n=2, jag=0.4, seed=11, glow=P["glow"], glow_a=0.25)
                H.shadow_under(f, 7.4, 0.6, 0.25)
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", "#0a0a14", edge=None, k_top=1.0, overlay=ov)


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
