"""The Devil Avatar, the floor boss of the Underworld Gate, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for the four looks: the Avatar of Lucifero (violet) and of Megicula (crimson), each as the
base look and as `_form2` (the Gates open); `devil_avatar_form2.py`, `devil_avatar_megicula.py` and `devil_avatar_megicula_form2.py` are the same code (K.sibling) and the devil and the form are read
from the id.

What it is on the 64x64 sheet (tools/arc/char_devil_avatar.py): a tall gaunt devil, bald with slit glowing eyes, fangs, cracks and veins, bare violet (crimson) skin, a ragged tabard with a wide collar
and a glowing spade on the chest, trim and runes, bone pauldrons and bracers, black gauntlets, a plain belt with a lit buckle, trousers and knee boots with a trim cuff. The horns (a second pair in
the Gates), the claws, the tattered cloak, the spade-tipped tail, the turning halo and the sigil tablets are the `pmx_devil_avatar*` extras: the skin keeps the top of the head plain (the horns stand
on it), the fingers and the claws are left to the model, the back keeps the tabard's panel.
Drawn at 4x detail on both layers (the second layer is dropped beyond 24 blocks): the bare head with a hard brow, narrow slit eyes with a lit core, fangs, veins at the temples and cracks on the cheeks
(the forehead sigil and the cheek lines in the Gates), pointed ears; the tabard with a bare V at the neck, a wide lit collar, the lit spade emblem in a ring, trim piping, runes, folds and a ragged hem;
bone pauldrons with a lit rim and bracers, black gauntlets with a lit knuckle rim, a belt with a glowing gem, trousers with a seam, knee boots with a trim cuff. Form 2 (the Gates): bone thorns on the
shoulders, a gorget of bone plates, more cracks, brighter trim.

Source (rule 10): no wiki page applies (the Devil Avatar is the mod's own Underworld boss, a "high devil of the Spade Kingdom"); the colours are the sheet's (Lucifero violet, Megicula crimson, from memory
of the series); nothing was fetched.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = None                                                                # bald: the devil's body has no hair (the horns are extras)


def hx(a):
    return "#%02x%02x%02x" % tuple(int(round(float(v) * 255)) for v in a[:3])


VIOLET = dict(
    skin=("#3a3050", "#54486e", "#6c6090"), plate="#4a3c70", cloak="#34284e", trim="#a878ff", glow="#b890ff", horn="#1e1830", horn2="#6a4aa8", bone="#cfc6dc",
    iris=("#5a2a9a", "#b890ff", "#f0e4ff"), mark="#c8a8ff",
)
MEGICULA = dict(
    skin=("#4a2830", "#683840", "#884a52"), plate="#6a2a38", cloak="#481e2c", trim="#ff5a5a", glow="#ff6a6a", horn="#26101a", horn2="#a02a3a", bone="#dccaca",
    iris=("#8a1020", "#ff4a4a", "#ffd0d0"), mark="#ff9090",
)


def palette(cid):
    t = MEGICULA if "megicula" in cid else VIOLET
    form2 = K.is_form2(cid)
    P = dict(t)
    P["form2"] = form2
    P["hair"] = P["horn"]
    P["hair_rgb"] = tuple(int(P["horn"][i:i + 2], 16) for i in (1, 3, 5))
    P["plate_l"] = hx(K.lit(P["plate"], 1.45))
    P["plate_d"] = hx(K.deep(P["plate"], 0.55))
    P["trim_l"] = hx(K.lit(P["trim"], 1.2))
    P["trim_d"] = hx(K.deep(P["trim"], 0.5))
    P["bone_l"] = hx(K.lit(P["bone"], 1.12))
    P["bone_d"] = hx(K.deep(P["bone"], 0.62))
    P["horn_l"] = hx(K.lit(P["horn"], 1.9))
    P["rune"] = P["trim"]
    return P


# ================================================================================================ the head
def head_front(cv, P):
    f = cv.face("head", "front")
    sk = P["skin"]
    form2 = P["form2"]
    K.face_base(f, sk, side=0.1, chin=0.09)
    f.fill(f.m_ellipse(4.0, 1.2, 3.4, 1.3), H.tone(sk[2], 1.05), 0.3)
    # the hard brow ridge, narrow angry slit eyes with a lit core
    brow = hx(K.deep(sk[0], 0.55))
    f.fill(f.m_polyline([(0.5, 3.45), (2.2, 3.7), (3.8, 4.05)], 0.4), brow, 0.95)
    f.fill(f.m_polyline([(7.5, 3.45), (5.8, 3.7), (4.2, 4.05)], 0.4), brow, 0.95)
    for cx in (2.3, 5.7):
        K.eye_bag(f, cx, 6.2, 2.2, hx(K.deep(sk[0], 0.8)), 0.5)
        al = H.eye(f, cx, 5.2, 2.7, 1.25, P["iris"], lash="#08040c", tilt=0.34, lash_w=0.6, flick=0.42, lid_shadow=0.55, hi=0.7, pupil="#10040a")
        f.fill(f.m_ellipse(cx, 5.22, 0.13, 0.55) * al, "#06020a", 0.95)
        K.eye_glow(f, al, cx, 5.2, 2.7, 1.25, P["glow"], 0.95)
        f.glow_fill(f.m_ellipse(cx, 5.2, 1.8, 1.0, soft=3.0), P["glow"], 0.28)
    f.fill(f.m_polyline([(4.0, 5.7), (3.88, 6.3), (4.12, 6.45)], 0.15), hx(K.deep(sk[0], 0.7)), 0.8)
    # the fanged mouth
    f.fill(f.m_poly([(2.3, 6.95), (4.0, 7.2), (5.7, 6.95), (5.2, 7.8), (4.0, 8.0), (2.8, 7.8)]), "#12040a", 1.0)
    for x, y0 in ((2.85, 7.0), (5.15, 7.0), (3.8, 7.2), (4.25, 7.2)):
        f.fill(f.m_poly([(x - 0.18, y0 + 0.05), (x + 0.18, y0 + 0.05), (x, y0 + (0.85 if x in (2.85, 5.15) else 0.5))]), "#f2ece4", 1.0)
    f.fill(f.m_polyline([(2.3, 6.95), (4.0, 7.2), (5.7, 6.95)], 0.16), hx(K.deep(sk[0], 0.7)), 0.9)
    # veins at the temples and cracks on the cheeks (lit)
    for pts in ([(0.5, 2.6), (1.2, 3.1), (1.5, 3.8)], [(7.5, 2.6), (6.8, 3.1), (6.5, 3.8)], [(2.0, 1.6), (2.6, 2.3)], [(6.0, 1.6), (5.4, 2.3)]):
        K.taper_line(f, pts, 0.12, hx(K.deep(P["trim"], 0.7)), 0.8, ends=0.4)
    cr = [[(1.8, 6.0), (1.3, 6.7), (1.7, 7.4)], [(6.2, 6.0), (6.7, 6.7), (6.3, 7.4)]]
    if form2:
        cr += [[(0.6, 4.3), (0.9, 5.2), (0.5, 6.0)], [(7.4, 4.3), (7.1, 5.2), (7.5, 6.0)], [(4.0, 4.6), (3.8, 5.0)]]
        # the forehead sigil: a lit diamond with a cross and the cheek lines of the Gates
        d = [(4.0, 0.9), (4.7, 1.8), (4.0, 2.7), (3.3, 1.8)]
        f.fill(f.m_poly(d), "#0a0610", 0.95)
        f.fill(f.m_polyline(d + [d[0]], 0.13), P["trim"], 0.95, glow=P["glow"])
        f.fill(f.m_line((3.0, 1.8), (5.0, 1.8), 0.1, 0.1), P["trim"], 0.9, glow=P["glow"])
        f.fill(f.m_line((4.0, 1.8), (4.0, 3.2), 0.1, 0.05), P["trim"], 0.9, glow=P["glow"])
        for s in (-1.0, 1.0):
            K.taper_line(f, [(4.0 + s * 3.3, 4.4), (4.0 + s * 3.5, 5.6), (4.0 + s * 3.0, 6.8)], 0.17, P["trim"], 0.9, glow=P["glow"], ends=0.3)
    else:
        for s in (-1.0, 1.0):                                                    # the forehead: a thin lit line the horns grow from
            K.taper_line(f, [(4.0 + s * 1.2, 1.3), (4.0 + s * 1.9, 2.2)], 0.11, P["trim"], 0.8, glow=P["glow"], ends=0.4)
    K.cracks_on(f, cr, P["trim"], glow=P["glow"], width=0.12, core=P["trim_l"], core_a=0.5, dark="#08020a")


def head_other(cv, img64, spec, P):
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"], ears=True, ear_y=4.4)
    for nm in ("west", "east"):
        f = cv.face("head", nm, False)
        K.taper_line(f, [(2.4, 2.4), (2.9, 3.2), (2.6, 3.9)], 0.13, hx(K.deep(P["trim"], 0.7)), 0.8, ends=0.4)
        K.taper_line(f, [(f.w * 0.55, 5.8), (f.w * 0.7, 6.8)], 0.15, P["trim"], 0.85, glow=P["glow"], ends=0.3)
    f = cv.face("head", "back", False)
    K.cracks_on(f, [[(4.0, 1.0), (3.6, 2.2), (4.2, 3.4)], [(1.6, 4.0), (2.2, 5.2)], [(6.4, 3.6), (5.8, 5.0)]], P["trim"], glow=P["glow"], width=0.12, core=P["trim_l"], core_a=0.5, dark="#08020a")
    f = cv.face("head", "top", False)
    K.cracks_on(f, [[(f.w * 0.5, 0.0), (f.w * 0.42, f.h * 0.4), (f.w * 0.55, f.h * 0.8)]], P["trim"], glow=P["glow"], width=0.11, core=P["trim_l"], core_a=0.5, dark="#08020a")


# ================================================================================================ the tabard
def runes_on(f, mask, y0, y1, P, seed, a=0.8):
    c = P["trim"]
    for j in range(int((y1 - y0) / 1.3)):
        for i in range(int(f.w / 1.2)):
            if H.hsh(seed, i, j) < 0.45:
                continue
            x, y = 0.6 + i * 1.2 + 0.2 * H.hsh(seed, i, j, 1), y0 + 0.6 + j * 1.3
            g = int(H.hsh(seed, i, j, 2) * 4)
            pts = ([(x - 0.3, y - 0.4), (x + 0.3, y - 0.4), (x, y + 0.4)], [(x - 0.3, y - 0.4), (x - 0.3, y + 0.4), (x + 0.3, y)], [(x, y - 0.45), (x, y + 0.45), (x + 0.3, y + 0.1)], [(x - 0.3, y), (x + 0.3, y), (x, y - 0.45), (x, y + 0.45)])[g]
            f.fill(f.m_polyline(pts, 0.17) * mask, c, a, glow=P["glow"] if H.hsh(seed, i, j, 3) > 0.55 else None)


def collar(f, P, nm):
    """The wide collar: a lit bone-and-plate band, a trim rim; the bare V of the neck on the front."""
    m = f.m_band(0.0, 1.9)
    K.cloth(f, m, P["plate_l"], top=1.2, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.26, gain=1.2, spec=0.3)
    f.fill(f.m_rect(0.0, 1.72, f.w, 1.9), P["trim"], 1.0, glow=P["glow"])
    f.fill(f.m_rect(0.0, 1.72, f.w, 1.78), P["trim_l"], 0.7)
    H.shadow_under(f, 1.9, 0.5, 0.35)
    if nm == "front":
        v = f.m_poly([(2.4, 0.0), (5.6, 0.0), (4.0, 2.6)])
        f.fill(v, H.lin((0, 0), (0, 2.6), [(0, P["skin"][2]), (1, P["skin"][1])]))
        f.fill(f.m_polyline([(2.4, 0.0), (4.0, 2.6), (5.6, 0.0)], 0.2), P["trim"], 1.0, glow=P["glow"])
        f.fill(f.m_polyline([(3.1, 0.7), (3.7, 0.95)], 0.12), hx(K.deep(P["skin"][0], 0.8)), 0.7)
        f.fill(f.m_polyline([(4.9, 0.7), (4.3, 0.95)], 0.12), hx(K.deep(P["skin"][0], 0.8)), 0.7)


def tabard_front(f, P, ov):
    K.cloth(f, f.m_all(), P["plate"], top=1.45, bottom=0.78, round_=0.14, fold_xs=[0.9, 1.9, 6.1, 7.1], fold_depth=0.14, fold_y0=2.0, fold_y1=9.0)
    # the spade emblem in a ring on the chest, lit
    cx, cy = 4.0, 5.0
    f.glow_fill(f.m_ellipse(cx, cy, 3.2, 3.2, soft=3.0), P["glow"], 0.22)
    f.fill(f.m_ring(cx, cy, 2.4, 0.16), P["trim"], 0.85, glow=P["glow"])
    f.fill(f.m_ellipse(cx, cy, 2.1, 2.1), P["plate_d"], 0.55)
    sp = [(cx, cy - 1.6), (cx + 1.35, cy + 0.2), (cx + 0.95, cy + 0.95), (cx + 0.25, cy + 0.65), (cx + 0.45, cy + 1.7), (cx - 0.45, cy + 1.7), (cx - 0.25, cy + 0.65), (cx - 0.95, cy + 0.95), (cx - 1.35, cy + 0.2)]
    f.fill(f.m_poly(sp), H.lin((0, cy - 1.6), (0, cy + 1.7), [(0, P["trim_l"]), (0.5, P["trim"]), (1, P["trim_d"])]), 1.0, glow=P["glow"])
    f.fill(f.m_polyline(sp + [sp[0]], 0.1), P["trim_d"], 0.9)
    f.fill(f.m_line((cx, cy - 1.0), (cx, cy + 0.2), 0.12, 0.08), "#ffffff", 0.6)
    # the runes at the sides, the piping down both edges, lit side edge
    for x0, x1 in ((0.0, 1.5), (6.5, 8.0)):
        runes_on(f, f.m_rect(x0, 2.1, x1, 8.8), 2.2, 8.8, P, 3 if x0 < 4 else 4, 0.7)
    for x in (1.55, 6.45):
        K.seam(f, [(x, 2.0), (x, 8.8)], P["trim"], 0.06, 0.85)
    f.fill(f.m_rect(0.0, 1.9, 0.24, 9.0), P["plate_l"], 0.45)
    f.fill(f.m_rect(7.76, 1.9, 8.0, 9.0), P["plate_d"], 0.55)
    collar(f, P, "front")


def tabard_back(f, P, ov):
    K.cloth(f, f.m_all(), P["plate"], top=1.45, bottom=0.78, round_=0.14, fold_xs=[1.4, 2.8, 5.2, 6.6], fold_depth=0.14, fold_y0=2.0, fold_y1=9.0)
    f.fill(f.m_rect(0.0, 1.9, 0.24, 9.0), P["plate_l"], 0.35)
    f.fill(f.m_rect(7.76, 1.9, 8.0, 9.0), P["plate_d"], 0.55)
    runes_on(f, f.m_rect(0.0, 2.0, 8.0, 9.0), 2.2, 8.8, P, 9, 0.6)
    f.fill(f.m_line((4.0, 2.0), (4.0, 9.0), 0.2, 0.2), P["plate_d"], 0.6)
    f.fill(f.m_ring(4.0, 5.2, 2.2, 0.16), P["trim"], 0.8, glow=P["glow"])
    for pts in ([(4.0, 3.2), (4.0, 7.2)], [(2.4, 4.0), (5.6, 6.4)], [(5.6, 4.0), (2.4, 6.4)]):
        f.fill(f.m_polyline(pts, 0.12), P["trim"], 0.7, glow=P["glow"])
    collar(f, P, "back")


def tabard_side(f, P, nm):
    K.cloth(f, f.m_all(), P["plate"], top=1.45, bottom=0.78, round_=0.14, fold_xs=[1.2, 2.6], fold_depth=0.14, fold_y0=2.0, fold_y1=9.0)
    ed = f.w - 0.7 if nm == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.8)], P["trim"], 0.06, 0.8)
    runes_on(f, f.m_rect(0.0, 2.0, f.w, 9.0), 2.2, 8.8, P, 11 + (1 if nm == "east" else 0), 0.55)
    collar(f, P, nm)


def belt_hem(f, P, nm):
    K.belt_band(f, 8.9, 9.7, P["horn"], stitch=P["trim_d"])
    H.trim_line(f, [(0.0, 8.95), (f.w, 8.95)], P["trim"], width=0.12, spec=0.3, outline=0.4, glow=P["glow"])
    if nm == "front":
        K.buckle(f, 4.0, 9.3, 1.6, 1.0, P["trim_d"], hole=P["horn"])
        f.ellipse(4.0, 9.3, 0.3, 0.3, P["trim_l"], a=1.0, glow=P["glow"])
        f.ellipse(3.9, 9.17, 0.1, 0.08, "#ffffff", a=0.9)
    elif nm == "back":
        f.ellipse(4.0, 9.3, 0.22, 0.22, P["trim_l"], a=1.0, glow=P["glow"])
    m = f.m_band(9.7, 12.0)
    K.cloth(f, m, P["plate"], top=1.25, bottom=0.72, round_=0.12, fold_xs=[f.w * 0.2, f.w * 0.5, f.w * 0.8], fold_depth=0.14, fold_y0=9.8, fold_y1=12.0)
    H.shadow_under(f, 9.72, 0.5, 0.3)
    f.fill(f.m_rect(0.0, 11.2, f.w, 11.36), P["trim"], 1.0, glow=P["glow"])
    f.fill(f.m_rect(0.0, 11.2, f.w, 11.26), P["trim_l"], 0.8)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                tabard_front(f, P, ov)
            elif nm == "back":
                tabard_back(f, P, ov)
            else:
                tabard_side(f, P, nm)
            belt_hem(f, P, nm)


# ================================================================================================ the arms
def gauntlet(f, P, y0):
    """The black gauntlet over the hand: plates, a lit rim at the wrist and at the knuckles, finger gaps (the claws are the model's)."""
    f.fill(f.m_band(y0, 12.0), H.lin((0, y0), (0, 12), [(0, P["horn_l"]), (0.3, P["horn"]), (1, hx(K.deep(P["horn"], 0.5)))]))
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.9), H.lin((0, y0), (0, y0 + 0.9), [(0, P["horn2"]), (1, P["horn"])]), 1.0)
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.14), P["trim"], 1.0, glow=P["glow"])
    H.shadow_under(f, y0 + 0.9, 0.5, 0.35)
    if f.name in ("front", "back"):
        for i in range(1, 4):
            x = f.w * i / 4.0
            f.fill(f.m_line((x, y0 + 1.5), (x, 11.98), 0.2, 0.16), "#06040a", 0.9)
        f.fill(f.m_polyline([(0.2, y0 + 1.4), (f.w * 0.5, y0 + 1.55), (f.w - 0.2, y0 + 1.4)], 0.14), P["trim"], 0.75, glow=P["glow"])
    f.fill(f.m_rect(0.0, y0, 0.24, 12.0), P["horn_l"], 0.4)


def arm(cv, part, P):
    sk = P["skin"]
    form2 = P["form2"]
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            K.limb_skin(f, sk, 2.5, 12.0, tendon=True)
            for pts in ([(f.w * 0.35, 3.8), (f.w * 0.3, 4.6), (f.w * 0.4, 5.5)],):
                K.taper_line(f, pts, 0.12, hx(K.deep(P["trim"], 0.7)), 0.7, ends=0.4)
            # the bone pauldron (the second layer, rows 0 to 3) and the bracer with a lit rim
            K.plate_band(f, 0.0, 3.4, P["bone_d"], trim=P["trim"], glow=P["glow"], segs=2, light_k=1.9, rivets=[0.7, f.w - 0.7], jag=0.3, seed=5)
            K.plate_band(f, 6.3, 8.9, P["bone_d"], trim=P["trim"], glow=P["glow"], segs=2, light_k=1.75, rivets=[0.7, f.w - 0.7], seed=6)
            H.shadow_under(f, 3.5, 0.6, 0.3)
            gauntlet(f, P, 8.9)
            if form2:                                                                     # the thorns of the Gates stand on the shoulder and the elbow
                for bx, by, tx, ty, wd in ((f.w * 0.28, 3.0, f.w * 0.16, 0.7, 0.85), (f.w * 0.74, 3.0, f.w * 0.84, 0.8, 0.85)):
                    K.shard(f, bx, by, tx, ty, wd, P["bone_l"], hx(K.deep(P["bone"], 0.7)), edge=P["bone_d"], glow=P["glow"], a=1.0)
                K.shard(f, f.w * 0.5, 6.4, f.w * 0.5, 4.9, 0.85, P["bone_l"], hx(K.deep(P["bone"], 0.7)), edge=P["bone_d"], glow=P["glow"], a=0.95)
    for ov in (False, True):
        K.cap(cv, part, "top", P["bone"], edge=P["trim"], k_top=1.3, overlay=ov)


# ================================================================================================ the legs
def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 7.0)
                K.cloth(f, m, P["plate"], top=1.3, bottom=0.8, round_=0.14, fold_xs=[0.9, 2.0, 3.0], fold_depth=0.12, fold_y0=0.3, fold_y1=6.8)
                runes_on(f, m, 0.8, 6.0, P, 41 + SIDES.index(nm), 0.5)
                f.fill(f.m_rect(0.0, 0.0, 0.26, 7.0), P["plate_l"], 0.4)
                H.shadow_under(f, 0.0, 0.6, 0.3)
                # the knee boot: black leather with a lit trim cuff, a strap with a stud, a sole
                bm = f.m_band(6.6, 12.0)
                f.fill(bm, H.lin((0, 6.6), (0, 12), [(0, P["horn_l"]), (0.35, P["horn"]), (1, hx(K.deep(P["horn"], 0.45)))]))
                f.fill(f.m_rect(0.0, 6.6, f.w, 7.6), P["horn2"], 1.0)
                f.fill(f.m_rect(0.0, 6.6, f.w, 6.78), P["trim"], 1.0, glow=P["glow"])
                f.fill(f.m_rect(0.0, 6.78, f.w, 6.86), P["trim_l"], 0.7)
                H.shadow_under(f, 7.6, 0.5, 0.35)
                f.fill(f.m_rect(0.0, 9.4, f.w, 9.8), hx(K.deep(P["horn"], 0.6)), 0.9)
                if nm == "front":
                    H.stud(f, f.w * 0.5, 9.6, 0.22, P["trim"], spec=0.8)
                    f.fill(f.m_ellipse(f.w * 0.5, 11.3, f.w * 0.42, 0.4), P["horn_l"], 0.5)
                f.fill(f.m_rect(0.0, 0.0, 0.2, 12.0) * bm, P["horn_l"], 0.35)
                f.fill(f.m_band(11.6, 12.0), "#06040a", 1.0)
                f.fill(f.m_band(11.6, 11.7), P["horn2"], 0.5)
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", "#06040a", edge=None, k_top=1.0, overlay=ov)


# ================================================================================================ paint
def paint(spec, cid, masks):
    P = palette(cid)
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, P["hair"], P["hair_rgb"])
    head_other(cv, img64, spec, P)
    head_front(cv, P)
    body(cv, P)
    arm(cv, "arm_r", P)
    arm(cv, "arm_l", P)
    legs(cv, P)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim)
