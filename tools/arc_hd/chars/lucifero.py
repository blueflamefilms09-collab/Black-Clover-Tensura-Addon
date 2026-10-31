"""Lucifero, the King of Devils, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for both forms: `lucifero_form2.py` is the same code with the full-power palette (K.is_form2).

What he is on the 64x64 sheet (tools/arc/char_lucifero.py): a tall lanky devil with grey skin covered in black patterns, long white hair, a sleeveless dark-grey tank with runes, leggings with a black
stripe, bare feet, a dark sash with a violet buckle, pauldrons and bracers of dark bone with violet trim, a third eye in the forehead. The two pairs of horns, the two pairs of bat wings and the dark
sphere he holds are the `pmx_lucifero*` extras: the wings sit behind the shoulders, so the back of the body keeps one clean panel between them (the patterns run under them), the horns sit on the
head. Wide arms, the height scale (1.44) and the glow semantics (eyes, third eye, trim, the glow lines of the armour) are the sheet's.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): slit pale eyes with a black slit pupil under flat white brows, a sharp nose, a thin cold mouth, the
black tribal patterns on the face, neck, arms and legs (tapered strokes with hooked ends, they follow the limbs), the third eye (a pale almond without iris, glowing; the lower half shows under the
fringe), long white hair with a pale-lavender shade; the tank with rune strokes and a scooped neck over the collarbones, the sash with a glowing buckle; pauldrons and bracers of dark bone plates
with a lit edge and a glowing violet trim line, thin bony hands with long finger gaps, leggings with a black stripe and patterns, bare feet with toes. Form 2 (full power): a wider pattern, spikes on
the shoulders and elbows (dark bone, glowing tips), white glowing eyes, cracks in the face, a brighter third eye.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia are blocked by the session's proxy); from memory of the series plus docs/arc/lucifero.md
(which cites the wiki through search digests): tall, long white hair, grey skin with black patterns, a third eye at full power. The violet glow and the dark clothes are the mod's own.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_long_open"                                   # long white hair that leaves the forehead (the third eye) free; hair_map says anime_long, whose fringe hides it

P1 = dict(
    hair="#ece8f6", hair_rgb=(236, 232, 246),
    skin=("#6e7080", "#8e90a2", "#b0b2c4"),
    tank="#322e42", tank_l="#524c6c", tank_d="#161222", ink="#0e0a16", ink_l="#2a2438", tmark="#7a68b8",
    sash="#1a1624", bone="#241f30", bone_l="#5c5478", bone_d="#0e0a16", trim="#b58cff", trim_l="#e0ccff", glow="#b58cff",
    legs="#302c3e", stripe="#a58ae8",
    iris=("#8a78c8", "#e4dcff", "#ffffff"), eye_glow="#b58cff", eye3="#f0e6ff", eye3_g="#b58cff",
)
P2 = dict(
    hair="#f2eefa", hair_rgb=(242, 238, 250),
    skin=("#686a7e", "#8a8ca2", "#b0b2cc"),
    tank="#2a2638", tank_l="#4a4468", tank_d="#100c1a", ink="#08060e", ink_l="#241e34", tmark="#9a82e0",
    sash="#16121e", bone="#1c1826", bone_l="#564e72", bone_d="#0a0710", trim="#d8c0ff", trim_l="#f4ecff", glow="#d8c0ff",
    legs="#2a2638", stripe="#c8b0ff",
    iris=("#d0c4f0", "#ffffff", "#ffffff"), eye_glow="#ffffff", eye3="#ffffff", eye3_g="#d8c0ff",
)


# ================================================================================================ the head
def head_front(cv, P, form2):
    f = cv.face("head", "front")
    K.face_base(f, P["skin"], side=0.1, chin=0.08)
    pal = K.hair_pal(P["hair"], light_k=1.04, dark_k=0.7)
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.4), (7.6, 2.1), (6.6, 2.9), (5.4, 2.1), (4.0, 2.2), (2.6, 2.1), (1.4, 2.9), (0.6, 2.1), (-0.3, 2.5)], y_bot=2.5)
    K.strands(f, pal, (0.9, 2.1, 3.3, 4.5, 5.7, 6.9), 0.0, 2.2, mass, a=0.35)
    for sh, (cx, w, top, ln, lean) in ((0.9, (6.9, 1.4, 1.6, 2.6, -0.3)), (0.9, (1.1, 1.4, 1.6, 2.6, 0.3)), (1.0, (6.0, 1.1, 1.8, 1.6, -0.1)), (1.0, (2.0, 1.1, 1.8, 1.6, 0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    for cx, lean in ((0.4, 0.15), (7.6, -0.15)):
        K.lock(f, pal, cx, 1.0, 0.8, 3.8, lean, 1.0)
    # the third eye: a pale almond with no iris and no pupil, open and glowing on the brow line
    cx, cy, w, h = 4.0, 3.15, 1.9, 1.15
    al = f.m_poly([(cx - w / 2, cy), (cx - w * 0.2, cy - h * 0.46), (cx + w * 0.2, cy - h * 0.46), (cx + w / 2, cy), (cx + w * 0.2, cy + h * 0.46), (cx - w * 0.2, cy + h * 0.46)])
    f.fill(f.m_polyline([(cx - w / 2 - 0.15, cy), (cx, cy - h * 0.7), (cx + w / 2 + 0.15, cy)], 0.26), P["ink"], 0.9)
    f.fill(al, H.lin((0, cy - h / 2), (0, cy + h / 2), [(0, P["eye3_g"]), (0.4, P["eye3"]), (1, "#ffffff")]))
    f.glow_fill(al, P["eye3_g"], 0.9)
    f.glow_fill(f.m_ellipse(cx, cy, w * 0.7, h * 0.9, soft=3.0), P["eye3_g"], 0.4)
    # the black pattern on the face: strokes from the brow down the cheeks and over the chin
    for pts in ([(0.9, 3.3), (1.1, 4.4), (0.8, 5.6), (1.2, 6.8), (1.6, 7.8)], [(7.1, 3.3), (6.9, 4.4), (7.2, 5.6), (6.8, 6.8), (6.4, 7.8)], [(1.8, 6.4), (2.3, 7.2)], [(6.2, 6.4), (5.7, 7.2)]):
        K.taper_line(f, pts, 0.3, P["ink"], 0.95, glow=None, ends=0.3)
    for pts in ([(2.0, 3.0), (2.4, 3.5)], [(6.0, 3.0), (5.6, 3.5)]):
        K.taper_line(f, pts, 0.2, P["ink"], 0.9, ends=0.4)
    if form2:                                                    # the face cracks of the full power: thin glowing lines from the eyes
        K.cracks_on(f, [[(1.2, 5.6), (0.6, 6.2), (0.9, 6.9)], [(6.8, 5.6), (7.4, 6.2), (7.1, 6.9)], [(3.2, 3.7), (3.0, 4.2)], [(4.8, 3.7), (5.0, 4.2)]], P["trim"], glow=P["glow"], width=0.13, core="#ffffff", core_a=0.5, dark=P["ink"])
    # flat white brows, cold slit eyes with a black slit pupil
    brow = "#e4e0f2"
    H.brow(f, (3.45, 3.95), (0.95, 3.75), brow, 0.3, 0.12, arch=0.0)
    H.brow(f, (4.55, 3.95), (7.05, 3.75), brow, 0.3, 0.12, arch=0.0)
    for cx in (2.35, 5.65):
        al = H.eye(f, cx, 5.1, 2.5, 1.3, P["iris"], lash="#0e0a16", tilt=0.18, lash_w=0.5, flick=0.34, lid_shadow=0.45, hi=0.8, pupil="#0e0a16", iris_r=1.0)
        f.fill(f.m_ellipse(cx, 5.15, 0.16, 0.56) * al, "#0a0612", 0.95)
        K.eye_glow(f, al, cx, 5.1, 2.5, 1.3, P["eye_glow"], 0.95)
        f.glow_fill(f.m_ellipse(cx, 5.1, 1.7, 0.9, soft=3.0), P["eye_glow"], 0.25)
    # a sharp nose, a thin cold mouth
    f.fill(f.m_polyline([(4.0, 5.6), (3.85, 6.3), (4.15, 6.45)], 0.16), H.tone(P["skin"][1], 0.6), 0.8)
    f.fill(f.m_polyline([(3.0, 7.35), (4.0, 7.45), (5.0, 7.35)], 0.2), "#2a2030", 0.95)
    f.fill(f.m_polyline([(3.4, 7.7), (4.6, 7.7)], 0.14), H.tone(P["skin"][1], 0.78), 0.5)


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.04, dark_k=0.66)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.5, 1.0, 3.0, lean, 0.95)
        for pts in ([(2.2, 3.6), (2.6, 4.8), (2.3, 6.0)],):
            K.taper_line(f, pts, 0.24, P["ink"], 0.85, ends=0.3)


# ================================================================================================ the body
def tank_front(f, P, ov):
    m = f.m_band(0.0, 12.0)
    K.cloth(f, m, P["tank"], top=1.2, bottom=0.8, round_=0.14, fold_xs=[1.2, 6.8], fold_depth=0.1, fold_y0=2.0, fold_y1=8.6)
    f.fill(f.m_rect(0.0, 1.9, 0.26, 8.7), P["tank_l"], 0.4)
    f.fill(f.m_rect(7.74, 1.9, 8.0, 8.7), P["tank_d"], 0.5)
    # the bare chest above the scooped neck: collarbones, a notch
    sc = f.m_poly([(0.9, 0.0), (7.1, 0.0), (6.4, 1.1), (5.0, 2.1), (4.0, 2.4), (3.0, 2.1), (1.6, 1.1)])
    f.fill(sc, H.lin((0, 0), (0, 2.4), [(0, P["skin"][2]), (1, P["skin"][1])]))
    f.fill(f.m_polyline([(1.2, 0.9), (2.4, 1.15), (3.6, 0.95)], 0.2), H.tone(P["skin"][0], 0.78), 0.7)
    f.fill(f.m_polyline([(6.8, 0.9), (5.6, 1.15), (4.4, 0.95)], 0.2), H.tone(P["skin"][0], 0.78), 0.7)
    f.fill(f.m_polyline([(1.6, 1.1), (3.0, 2.1), (4.0, 2.4), (5.0, 2.1), (6.4, 1.1)], 0.26), P["tank_d"], 0.95)
    # rune strokes over the tank, a belt of glowing violet line along the hem of the neckline
    K.flow_marks(f, m * (1.0 - sc), 0.3, 7.7, 2.6, 8.8, 5, 11, P["tmark"], width=0.24, amp=0.28, a=0.95, glow=P["glow"])
    K.seam(f, [(1.6, 1.2), (3.0, 2.2), (4.0, 2.5), (5.0, 2.2), (6.4, 1.2)], P["trim"], 0.06, 0.8)
    for x, y in ((2.2, 5.0), (5.8, 6.2)):                                                # a few rune dots that glow
        f.fill(f.m_ellipse(x, y, 0.18, 0.18), P["trim_l"], 0.9, glow=P["glow"])


def tank_back(f, P, ov):
    m = f.m_band(0.0, 12.0)
    K.cloth(f, m, P["tank"], top=1.2, bottom=0.8, round_=0.14, fold_xs=[1.4, 6.6], fold_depth=0.1, fold_y0=2.0, fold_y1=8.6)
    f.fill(f.m_rect(0.0, 1.9, 0.26, 8.7), P["tank_l"], 0.3)
    f.fill(f.m_rect(7.74, 1.9, 8.0, 8.7), P["tank_d"], 0.5)
    f.fill(f.m_rect(0.0, 0.0, 8.0, 1.2), H.lin((0, 0), (0, 1.2), [(0, P["skin"][2]), (1, P["skin"][1])]))
    f.fill(f.m_line((4.0, 0.2), (4.0, 12.0), 0.2, 0.2), P["tank_d"], 0.55)                      # the spine
    K.flow_marks(f, m, 0.3, 7.7, 1.6, 9.0, 6, 12, P["tmark"], width=0.26, amp=0.3, a=0.95)


def tank_side(f, P, nm):
    m = f.m_band(0.0, 12.0)
    K.cloth(f, m, P["tank"], top=1.2, bottom=0.8, round_=0.14, fold_xs=[1.2, 2.6], fold_depth=0.1, fold_y0=2.0, fold_y1=8.6)
    f.fill(f.m_rect(0.0, 0.0, f.w, 1.1), H.lin((0, 0), (0, 1.1), [(0, P["skin"][2]), (1, P["skin"][1])]))
    K.flow_marks(f, m, 0.3, f.w - 0.3, 1.6, 9.0, 3, 13, P["tmark"], width=0.24, amp=0.22, a=0.9)


def sash(f, P, front):
    K.belt_band(f, 9.0, 10.5, P["sash"], stitch=P["bone_l"])
    H.trim_line(f, [(0.0, 9.1), (f.w, 9.1)], P["trim"], width=0.14, spec=0.2, outline=0.4, glow=P["glow"])
    if front:
        K.buckle(f, 4.0, 9.75, 1.9, 1.3, P["bone_l"], hole=P["sash"])
        f.ellipse(4.0, 9.75, 0.28, 0.28, P["trim_l"], a=1.0, glow=P["glow"])
    else:
        f.ellipse(4.0, 9.75, 0.22, 0.22, P["trim_l"], a=1.0, glow=P["glow"])
    # the sash ends hang over the hip
    if front:
        f.fill(f.m_poly([(5.4, 10.5), (6.8, 10.5), (6.5, 12.0), (5.6, 11.6)]), H.lin((0, 10.5), (0, 12), [(0, P["sash"]), (1, H.tone(P["sash"], 0.6))]), 0.95)


def hips(f, P, nm):
    m = f.m_band(10.5, 12.0)
    f.fill(m, H.lin((0, 10.5), (0, 12), [(0, K.lit(P["legs"], 1.05)), (1, H.tone(P["legs"], 0.7))]))
    f.fill(f.m_rect(0.0, 10.5, f.w, 12.0) * 0.0, "#000000", 0.0)
    H.shadow_under(f, 10.5, 0.5, 0.28)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                tank_front(f, P, ov)
            elif nm == "back":
                tank_back(f, P, ov)
            else:
                tank_side(f, P, nm)
            hips(f, P, nm)
            sash(f, P, nm == "front")


# ================================================================================================ the arms
def arm(cv, part, P, form2):
    sk = P["skin"]
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            m = f.m_band(3.0, 12.0)
            K.limb_skin(f, sk, 2.5, 12.0, tendon=True)
            K.flow_marks(f, f.m_band(3.2, 10.0), 0.3, f.w - 0.3, 3.4, 9.6, 2, 21 + (3 if part == "arm_l" else 0), P["ink"], width=0.22, amp=0.2, a=0.95)
            # the thin bony hand: long finger gaps and a knuckle line
            f.fill(f.m_band(10.0, 12.0), H.lin((0, 10), (0, 12), [(0, sk[1]), (1, H.tone(sk[0], 0.9))]))
            if nm in ("front", "back"):
                for i in range(1, 4):
                    x = f.w * i / 4.0
                    f.fill(f.m_line((x, 10.9), (x, 11.95), 0.22, 0.14), H.tone(sk[0], 0.62), 0.85)
                f.fill(f.m_polyline([(0.3, 10.8), (f.w * 0.5, 10.95), (f.w - 0.3, 10.8)], 0.16), H.tone(sk[0], 0.7), 0.6)
            # the pauldron: dark bone plates with a lit edge, glowing violet trim at the lower edge
            K.plate_band(f, 0.0, 3.2, P["bone"], trim=P["trim"], glow=P["glow"], segs=2, light_k=1.55, rivets=[0.7, f.w - 0.7] if f.w >= 4 else None, jag=0.35, seed=4)
            # the bracer
            K.plate_band(f, 6.4, 9.9, P["bone"], trim=P["trim"], glow=P["glow"], segs=2, light_k=1.55, rivets=[0.7, f.w - 0.7] if f.w >= 4 else None, seed=6)
            H.shadow_under(f, 3.5, 0.6, 0.3)
            if form2:                                                           # the spikes of the full power: bone thorns on the shoulder and at the elbow (glowing tips)
                for bx, by, tx, ty, wd in ((f.w * 0.3, 2.9, f.w * 0.2, 0.5, 0.8), (f.w * 0.72, 2.9, f.w * 0.8, 0.7, 0.8)):
                    pass
    for ov in (False, True):
        K.cap(cv, part, "top", P["bone"], edge=P["trim"], k_top=1.4, overlay=ov)


def spikes(cv, part, P):
    """Form 2: bone thorns standing on the shoulder board and at the elbow, drawn on the sides of the arm (the top is the board)."""
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            for bx, by, tx, ty, wd in ((f.w * 0.25, 3.0, f.w * 0.12, 1.2, 0.8), (f.w * 0.72, 3.0, f.w * 0.82, 1.4, 0.8)):
                K.shard(f, bx, by, tx, ty, wd, P["bone_l"], H.tone(P["bone"], 0.7), edge=P["bone_d"], glow=P["glow"], a=0.95)
            K.shard(f, f.w * 0.5, 6.5, f.w * 0.5, 4.9, 0.9, P["bone_l"], H.tone(P["bone"], 0.7), edge=P["bone_d"], glow=P["glow"], a=0.95)


# ================================================================================================ the legs
def legs(cv, P):
    sk = P["skin"]
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 10.4)
                K.cloth(f, m, P["legs"], top=1.3, bottom=0.8, round_=0.14, fold_xs=[1.2, 2.8], fold_depth=0.1, fold_y0=0.3, fold_y1=10.0)
                H.shadow_under(f, 0.0, 0.6, 0.25)
                # the black stripe on the outer side and the patterns
                sx = (f.w - 0.9, f.w - 0.2) if nm == "front" and part == "leg_r" else ((0.2, 0.9) if nm == "front" else (f.w * 0.42, f.w * 0.58))
                f.fill(f.m_rect(sx[0], 0.0, sx[1], 10.4) * m, P["stripe"], 0.98, glow=P["glow"])
                f.fill(f.m_rect(sx[0], 0.0, sx[0] + 0.1, 10.4) * m, P["tank_l"], 0.2)
                K.flow_marks(f, m, 0.3, f.w - 0.3, 1.2, 9.6, 2, 31 + (5 if part == "leg_l" else 0), P["tmark"], width=0.2, amp=0.18, a=0.75, curls=False)
                for y, k in ((3.4, 0.45), (4.0, 0.3)):                                    # the knee
                    f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.26), (f.w - 0.2, y + 0.05)], 0.14) * m, H.tone(P["legs"], 0.5), k)
                # the bare foot: skin, toes, a heel line
                foot = f.m_band(10.4, 12.0)
                f.fill(foot, H.lin((0, 10.4), (0, 12), [(0, sk[1]), (1, H.tone(sk[0], 0.82))]))
                H.shadow_under(f, 10.42, 0.5, 0.3)
                f.fill(f.m_rect(0.0, 10.4, f.w, 10.62), P["legs"], 0.9)
                if nm == "front":
                    for i in range(1, 4):
                        f.fill(f.m_line((f.w * i / 4.0, 11.35), (f.w * i / 4.0, 11.95), 0.2, 0.14), H.tone(sk[0], 0.62), 0.85)
                    f.fill(f.m_band(11.8, 12.0), H.tone(sk[0], 0.6), 0.6)
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", P["skin"][0], edge=None, k_top=1.0, overlay=ov)


# ================================================================================================ paint
def paint(spec, cid, masks):
    form2 = K.is_form2(cid)
    P = P2 if form2 else P1
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, P["hair"], P["hair_rgb"])
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"])
    head_sides(cv, P)
    head_front(cv, P, form2)
    body(cv, P)
    arm(cv, "arm_r", P, form2)
    arm(cv, "arm_l", P, form2)
    if form2:
        spikes(cv, "arm_r", P)
        spikes(cv, "arm_l", P)
    legs(cv, P)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim)
