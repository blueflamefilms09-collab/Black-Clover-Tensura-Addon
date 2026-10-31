"""Heath Grice, the ice mage of the Eye of the Midnight Sun, HD 256x256 (docs/arc/hd_arc_bc6.md). One painter for both forms: `heath_grice_form2.py` is the same code with the Purified palette.

What he wears is the 64x64 sheet's outfit (tools/arc/char_heath_grice.py): a black military jumpsuit with a stand collar and a row of buttons, two straps buckled round the neck, long sleeves with
stitched cuffs, black gloves, a double belt with a steel buckle and calf boots. The pocket watch on his belt, the floating ice orb at his shoulder and (form 2) the angel wings and the halo are the
`pmx_heath_grice*` extras: the wings sit behind the shoulders, so the back of the body keeps one clean cloth panel between them, and the front of the belt is kept free of any watch drawing.
Glow (as on the sheet): the icy eyes; in form 2 the golden eyes and the light lines of the face; plus a faint rime on the shoulders and the left forearm (the sheet's frost shards, which the geo
extras do not carry) in form 1.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): sharp half-lidded eyes with dark rings under them and a diagonal scar from the character's right
brow across the nose bridge to the left cheek, thin flat brows, a hard mouth, a combed-back hairline with two sideburns (the cut itself is the 3D hair `anime_short_open`); the jumpsuit with lit chest
panels, steel piping on the placket, shoulder boards with rivets, yoke seams, two buckled neck straps in steel grey (colour blocking against the black), the double belt, black gloves with a steel
knuckle line, trousers with knee creases and boots with a steel cuff and strap.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); from memory of the series plus docs/arc/heath_grice.md and the sheet: pale,
slanted eyes, silver hair combed back, a long scar over the face, black high-collared clothes with straps. The strap and rime colours are the sheet's metal tones, lifted.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc6 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_short_open"                               # silver hair combed to one side with an open forehead (hair_map says the classic `slick`, a flat cap; this reads like the cut)

# palette of both forms
P1 = dict(
    hair="#cdd2e6", hair_rgb=(205, 210, 230),
    skin=("#e0c4c6", "#f2dcdc", "#fff0f0"),
    cloth="#2a2a3c", cloth_l="#4a4a64", cloth_d="#14141f",
    trim="#8e9ab4", trim_d="#4a5470", trim_l="#d8e2f4",
    strap="#8c98b2", strap_d="#4a5068", buckle="#c8d4ea",
    belt="#34343f", glove="#1a1a24", boot="#1d1d29", sole="#08080c",
    iris=("#3a5a9a", "#8aa6e0", "#e0f2ff"), eye_glow="#a6e4ff",
    frost="#cfeeff", frost_g="#8ad8ff",
    scar="#c88888", scar_l="#f0c4c0",
)
P2 = dict(
    hair="#f6f6fc", hair_rgb=(246, 246, 252),
    skin=("#e6cfc6", "#f6e6dc", "#fff6ee"),
    cloth="#e8ecf8", cloth_l="#ffffff", cloth_d="#aeb8d8",
    trim="#cfa646", trim_d="#8a6a20", trim_l="#fff0b0",
    strap="#d0a844", strap_d="#8a6a20", buckle="#fff2b0",
    belt="#c8a040", glove="#cfd4e4", boot="#ccd2e4", sole="#7a7e94",
    iris=("#b8892a", "#f2d070", "#fff6c8"), eye_glow="#ffe58a",
    frost="#fff4c8", frost_g="#ffe58a",
    scar="#d09a8c", scar_l="#fbd6c8",
)


# ================================================================================================ the head
def head_front(cv, P, form2):
    f = cv.face("head", "front")
    K.face_base(f, P["skin"], side=0.08, chin=0.07)
    pal = K.hair_pal(P["hair"], light_k=1.1 if form2 else 1.22, dark_k=0.62)
    # the hairline: combed back, a short widow's peak and a sideburn each side (the cut is the 3D hair)
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 1.9), (7.4, 1.55), (6.3, 1.2), (4.8, 0.9), (3.5, 1.3), (2.2, 1.45), (1.2, 1.8), (-0.3, 2.1)], y_bot=2.0)
    K.strands(f, pal, (1.0, 2.2, 3.4, 4.6, 5.8, 7.0), 0.0, 1.5, mass, a=0.4)
    for cx, lean in ((0.45, 0.15), (7.55, -0.15)):
        K.lock(f, pal, cx, 1.0, 0.6, 3.4, lean, 1.0)
    # thin flat brows, slanted a little: a cold, calculating look
    brow = "#8d92aa" if not form2 else "#c9b890"
    H.brow(f, (3.45, 3.75), (1.0, 3.45), brow, 0.3, 0.12, arch=0.02)
    H.brow(f, (4.55, 3.75), (7.0, 3.45), brow, 0.3, 0.12, arch=0.02)
    # tired sharp eyes: half lidded, a raised outer corner, the iris glows
    K.eye_bag(f, 2.35, 5.95, 2.1, "#9a8aa4" if not form2 else "#d4b8a8", 0.42)
    K.eye_bag(f, 5.65, 5.95, 2.1, "#9a8aa4" if not form2 else "#d4b8a8", 0.42)
    for cx in (2.35, 5.65):
        al = H.eye(f, cx, 5.1, 2.5, 1.45, P["iris"], lash="#14141c" if not form2 else "#6a5a3a", tilt=0.34, lash_w=0.5, flick=0.46, lid_shadow=0.5, hi=0.8)
        K.eye_glow(f, al, cx, 5.1, 2.5, 1.45, P["eye_glow"], 0.85)
    H.nose(f, 4.0, 6.25, "#c8a4a8" if not form2 else "#d8b0a0", 0.2)
    H.mouth_smile(f, 4.0, 7.3, 1.9, color="#8a5a64" if not form2 else "#a8705c", lip="#cc9aa0", depth=-0.04, thick=0.17)
    # the scar: from the character's right brow across the nose bridge to the left cheek (raised, lighter core, darker edge, three stitch marks)
    pts = [(0.9, 0.4), (2.0, 1.7), (3.0, 3.0), (3.85, 4.0), (4.0, 5.4), (4.5, 6.5), (5.6, 7.2), (6.8, 7.8)]
    K.scar(f, pts, P["scar_l"], P["scar"], 0.24)
    for t in (0.2, 0.42, 0.66):
        i = int(t * (len(pts) - 1))
        x, y = pts[i]
        f.fill(f.m_line((x - 0.35, y + 0.28), (x + 0.35, y - 0.28), 0.12, 0.12), P["scar"], 0.7)
    if form2:                                                # the light lines of the Purified form: thin golden veins that glow, on the cheeks and under the eyes
        for p in ([(0.5, 5.4), (1.0, 6.4), (1.6, 7.3)], [(7.5, 5.4), (7.0, 6.4), (6.4, 7.3)], [(3.3, 6.3), (3.0, 7.0)], [(4.7, 6.3), (5.0, 7.0)]):
            K.glow_line(f, p, "#ffe58a", 0.16, 0.85, core="#ffffff", core_a=0.5)


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.2, dark_k=0.62)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.6, 1.0, 3.2, lean, 0.95)


# ================================================================================================ the jumpsuit
def collar(f, name, P, form2):
    """A stand collar with two buckled straps round the neck."""
    m = f.m_band(0.0, 1.8)
    K.cloth(f, m, P["cloth"], top=1.35 if not form2 else 1.04, bottom=0.78 if not form2 else 0.86, round_=0.1)
    H.bevel(f, m, depth=0.25, gain=1.15, spec=0.25)
    for y0, y1, bx in ((0.08, 0.86, 2.9), (0.98, 1.76, 5.1)):
        f.fill(f.m_rect(0.0, y0, f.w, y1), H.lin((0, y0), (0, y1), [(0, K.lit(P["strap"], 1.3)), (0.5, P["strap"]), (1, H.tone(P["strap"], 0.7))]))
        f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.14), K.lit(P["strap"], 1.8), 0.55)
        f.fill(f.m_rect(0.0, y1 - 0.12, f.w, y1), "#0a0a10", 0.5)
        K.seam(f, [(0.1, (y0 + y1) / 2.0), (f.w - 0.1, (y0 + y1) / 2.0)], P["strap_d"], 0.055, 0.55, (0.3, 0.18), 0.15)
        if name == "front":
            K.buckle(f, bx, (y0 + y1) / 2.0, 1.3, 0.78, P["buckle"], hole=P["cloth_d"], r=0.12)
            for k in (1, 2, 3):                                              # the punched holes of the strap tail
                H.stud(f, bx + 0.95 + 0.34 * k, (y0 + y1) / 2.0, 0.07, P["strap_d"], spec=0.2)
        elif name == "back":
            H.stud(f, 4.0, (y0 + y1) / 2.0, 0.17, P["buckle"], spec=0.7)
    f.fill(f.m_rect(0.0, 1.72, f.w, 1.84), P["trim"], 1.0)
    f.fill(f.m_rect(0.0, 1.72, f.w, 1.78), P["trim_l"], 0.7)
    H.shadow_under(f, 1.86, 0.5, 0.3)


def jumpsuit_front(f, P, form2):
    cl = P["cloth"]
    K.cloth(f, f.m_all(), cl, top=1.45 if not form2 else 1.03, bottom=0.78 if not form2 else 0.86, round_=0.14, fold_xs=[1.3, 6.7], fold_depth=0.11, fold_y0=2.0, fold_y1=8.8)
    for x0, x1 in ((0.0, 3.3), (4.7, 8.0)):                                                  # the two lit chest panels
        K.sheen(f, f.m_rect(x0, 1.9, x1, 8.7), [x0 + 0.9 if x0 < 4 else x1 - 0.9], 2.1, 8.4, 0.28 if not form2 else 0.18, "#c4ccf4" if not form2 else "#ffffff", width=0.28)
    f.fill(f.m_rect(0.0, 1.9, 0.28, 8.7), P["cloth_l"], 0.35)
    f.fill(f.m_rect(7.72, 1.9, 8.0, 8.7), "#080810" if not form2 else P["cloth_d"], 0.35)
    # the placket: a darker strip with steel piping on both edges and a row of buttons
    f.fill(f.m_rect(3.4, 1.9, 4.6, 8.8), H.tone(cl, 0.82), 0.6)
    for x in (3.4, 4.6):
        H.trim_line(f, [(x, 1.9), (x, 8.8)], P["trim"], width=0.2, spec=0.4, outline=0.5)
    for y in (2.6, 3.9, 5.2, 6.5, 7.8):
        H.stud(f, 4.0, y, 0.22, P["trim_l"], spec=0.9)
    # yoke seams from the collar to the shoulders
    K.seam(f, [(0.3, 2.4), (2.9, 2.0)], P["trim_d"], 0.05, 0.6)
    K.seam(f, [(7.7, 2.4), (5.1, 2.0)], P["trim_d"], 0.05, 0.6)
    # the watch chain: from the second button over the left breast down to the belt (the watch itself hangs on the extra)
    K.chain_path(f, [(4.65, 3.1), (5.4, 3.7), (6.2, 5.2), (6.4, 7.2), (6.0, 8.7)], step=0.8, color=P["trim_l"], dark=P["cloth_d"], rx=0.5, ry=0.32)
    collar(f, "front", P, form2)
    belts(f, P, form2, True)


def belts(f, P, form2, front):
    """The double belt at the waist with a steel buckle (the pocket watch hangs from it on the extra: no drawing of it here)."""
    K.belt_band(f, 8.75, 9.45, P["belt"], stitch=P["trim_d"])
    K.belt_band(f, 9.62, 10.32, P["belt"], stitch=P["trim_d"])
    if front:
        K.buckle(f, 4.0, 9.55, 2.0, 1.75, P["buckle"], hole=P["cloth_d"])
    else:
        H.stud(f, 4.0, 9.55, 0.2, P["buckle"], spec=0.7)


def hips(f, P, form2, name):
    m = f.m_band(10.32, 12.0)
    f.fill(m, H.lin((0, 10.32), (0, 12.0), [(0, K.lit(P["cloth"], 1.05)), (1, H.tone(P["cloth"], 0.75))]))
    H.folds(f, m, [f.w * 0.3, f.w * 0.7], depth=0.12, width=0.25, y0=10.4, y1=12.0)
    H.shadow_under(f, 10.35, 0.5, 0.3)


def snowflake(f, cx, cy, r, P):
    """The Crystal Ice Shield's snowflake on the upper back (a small steel emblem with a faint glow): six spokes with side branches."""
    import math
    for k in range(6):
        a = math.radians(90 + 60 * k)
        ux, uy = math.cos(a), math.sin(a)
        tip = (cx + ux * r, cy + uy * r)
        f.fill(f.m_line((cx, cy), tip, 0.34, 0.2), P["trim"], 0.95, glow=P["frost_g"])
        f.fill(f.m_line((cx, cy), tip, 0.14, 0.08), P["trim_l"], 0.9)
        for t in (0.55, 0.8):
            bx, by = cx + ux * r * t, cy + uy * r * t
            for sgn in (-1, 1):
                b = a + sgn * math.radians(55)
                f.fill(f.m_line((bx, by), (bx + math.cos(b) * r * 0.26, by + math.sin(b) * r * 0.26), 0.2, 0.1), P["trim_l"], 0.9)
    f.ellipse(cx, cy, 0.3, 0.3, P["trim_l"], a=1.0)


def jumpsuit_back(f, P, form2):
    cl = P["cloth"]
    K.cloth(f, f.m_all(), cl, top=1.4 if not form2 else 1.03, bottom=0.78 if not form2 else 0.86, round_=0.14, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.11, fold_y0=2.0, fold_y1=8.8)
    collar(f, "back", P, form2)
    K.seam(f, [(0.3, 2.4), (2.9, 2.0)], P["trim_d"], 0.05, 0.5)
    K.seam(f, [(7.7, 2.4), (5.1, 2.0)], P["trim_d"], 0.05, 0.5)
    f.fill(f.m_line((4.0, 1.9), (4.0, 8.7), 0.2, 0.2), "#08080e" if not form2 else P["cloth_d"], 0.6)
    K.seam(f, [(3.62, 2.0), (3.62, 8.6)], P["trim_d"], 0.05, 0.45)
    if not form2:
        snowflake(f, 4.0, 4.6, 1.9, P)
    belts(f, P, form2, False)


def jumpsuit_side(f, P, form2, name):
    K.cloth(f, f.m_all(), P["cloth"], top=1.4 if not form2 else 1.03, bottom=0.78 if not form2 else 0.86, round_=0.14, fold_xs=[1.3, 2.7], fold_depth=0.11, fold_y0=2.0, fold_y1=8.8)
    collar(f, name, P, form2)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.5)], P["trim_d"], 0.05, 0.5)
    belts(f, P, form2, False)


def body(cv, P, form2):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                jumpsuit_front(f, P, form2)
            elif nm == "back":
                jumpsuit_back(f, P, form2)
            else:
                jumpsuit_side(f, P, form2, nm)
            hips(f, P, form2, nm)


# ================================================================================================ arms and legs
def frost(f, P, nm, forearm=True):
    """The sheet's frost shards (the geo extras do not carry them): a row of icicles hanging from the shoulder board and a fan of three on the forearm, lit and dark halves, a faint glow."""
    sh = [(f.w * 0.18, 1.3, f.w * 0.1, 3.5, 0.55), (f.w * 0.5, 1.3, f.w * 0.5, 4.1, 0.6), (f.w * 0.84, 1.3, f.w * 0.9, 3.2, 0.5)]
    for bx, by, tx, ty, w in sh:
        K.shard(f, bx, by, tx, ty, w, P["frost"], H.tone(P["frost"], 0.72), edge=H.tone(P["frost_g"], 0.5), glow=P["frost_g"], a=0.95)
    if forearm and nm in ("front", "west", "east"):
        for bx, by, tx, ty, w in ((f.w * 0.3, 6.1, f.w * 0.14, 7.5, 0.5), (f.w * 0.55, 6.0, f.w * 0.55, 7.6, 0.55), (f.w * 0.8, 6.1, f.w * 0.9, 7.4, 0.45)):
            K.shard(f, bx, by, tx, ty, w, P["frost"], H.tone(P["frost"], 0.72), edge=H.tone(P["frost_g"], 0.5), glow=P["frost_g"], a=0.95)


def arm(cv, part, P, form2):
    left = part == "arm_l"
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, part, nm), (False, True)):
            cl = P["cloth"]
            sleeve = f.m_band(0.0, 8.5)
            K.cloth(f, sleeve, cl, top=1.45 if not form2 else 1.03, bottom=0.8 if not form2 else 0.88, round_=0.18, fold_xs=[0.7, 1.7], fold_depth=0.12, fold_y0=1.8, fold_y1=8.0)
            K.sheen(f, sleeve, [f.w * 0.3], 1.8, 7.8, 0.26 if not form2 else 0.16, "#c4ccf4" if not form2 else "#ffffff", width=0.26)
            # the shoulder board: a steel plate over the shoulder with two rivets and a piping
            sb = f.m_band(0.0, 1.35)
            f.fill(sb, H.lin((0, 0), (0, 1.35), [(0, K.lit(P["trim"], 1.25)), (1, H.tone(P["trim"], 0.78))]))
            H.bevel(f, sb, depth=0.2, gain=1.2, spec=0.35)
            f.fill(f.m_rect(0.0, 1.28, f.w, 1.4), "#0a0a10" if not form2 else P["trim_d"], 0.7)
            for x in (0.6, f.w - 0.6):
                H.stud(f, x, 0.65, 0.15, P["trim_l"], spec=0.9)
            # the outer seam of the sleeve
            if nm in ("west", "east"):
                f.fill(f.m_line((f.w * 0.5, 1.5), (f.w * 0.5, 7.4), 0.2, 0.2) * sleeve, P["trim_d"], 0.55)
            # elbow creases
            for y, k in ((4.9, 0.55), (5.4, 0.35)):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.28), (f.w, y + 0.05)], 0.15) * sleeve, P["cloth_d"], k)
            f.fill(f.m_polyline([(0.0, 4.7), (f.w * 0.5, 4.95), (f.w, 4.75)], 0.2) * sleeve, K.lit(cl, 1.7 if not form2 else 1.05), 0.28)
            # the stitched cuff: a steel band with piping and two studs
            K.cuff(f, 7.5, 8.6, P["trim"], pipe=P["trim_l"], stud_color=P["trim_l"], spec=0.4)
            glove(f, P, nm, form2)
            if not form2 and nm in ("front", "west", "east", "back"):
                frost(f, P, nm, forearm=left)
            if form2 and nm in ("front", "back", "west", "east"):                                      # a gold piping line down the outer seam that glows a little
                K.glow_line(f, [(f.w * 0.5, 1.6), (f.w * 0.5, 7.2)], "#ffe58a", 0.14, 0.7, core="#fff8dc", core_a=0.4)


def glove(f, P, nm, form2):
    K.glove(f, P["glove"], 8.6, 12.0, nm, finger_n=3, cuff_color=None, knuckle_k=1.4 if not form2 else 1.05)
    if nm in ("front", "back"):
        f.fill(f.m_polyline([(f.w * 0.12, 9.5), (f.w * 0.5, 9.58), (f.w * 0.88, 9.5)], 0.14), P["trim"], 0.8)
    H.shadow_under(f, 8.65, 0.5, 0.3)


def arm_tops(cv, P):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", P["trim"], edge=P["trim_d"], k_top=1.3, overlay=ov)


def legs(cv, P, form2):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, P["cloth"], 0.0, 7.4, nm, knee=True, top=1.5 if not form2 else 1.03, bottom=0.8 if not form2 else 0.88, seam_color=P["trim_d"])
                H.shadow_under(f, 0.0, 0.6, 0.25)
                K.boot(f, P["boot"], 7.3, nm, strap=True, strap_color=P["trim_d"], gold=P["trim_l"], cuff_color=P["trim"], cuff_h=1.0, welt=P["trim_d"], sole=P["sole"])


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
    arm_tops(cv, P)
    legs(cv, P, form2)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim)
