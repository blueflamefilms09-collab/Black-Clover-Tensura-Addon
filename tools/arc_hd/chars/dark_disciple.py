"""The Dark Disciple, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for all five skins: the base (grey) and the four devil tints; `dark_disciple_<devil>.py` is the same code (K.sibling) and the devil
is read from the id (the tint is the cloth, the trim, the runes and the glow, as in tools/arc/char_dark_disciple.py: Lucifero violet, Megicula crimson, Beelzebub indigo, Astaroth gold, the base grey).

What he is on the 64x64 sheet: a hooded acolyte of a devil-bearer, an ashen face under a bone skull mask (the lower face, the second layer) with two glowing eyes, a ragged long robe written with the
devil's runes with a high collar and a trim at the hem, a skull emblem on the back, wide sleeves with a trim cuff, dark trousers and ankle boots, bone-coloured wraps on the hands, a dark sash with a
trim buckle. The hood (anime_hood), the hooded cloak, the ritual dagger and the floating orb are the `pmx_dark_disciple` extras.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): the mask is a cracked bone plate with a brow ridge, two almond eye holes with a lit eye each, a nose slit,
a tooth row, a glowing rune line on each cheek and dark straps; the robe has deep folds, a lit rune pattern, a dark inner panel with trim piping, a high collar with a lit rim, a ragged hem; the back
wears a big lit skull emblem; the cuffs carry the rune band; the hands are bound with bone wraps; the boots have a trim cuff and a strap.

Source (rule 10): no wiki page applies (the Dark Disciple is the mod's own mob, "the game's own look, not canon", tools/arc/char_dark_disciple.py); the devils' colours follow the devils (Lucifero violet,
Megicula crimson, Beelzebub indigo, Astaroth gold) from memory of the series. Nothing was fetched.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_hood"                                                        # the cultist's hood (hair_map)

TINTS = {
    "": dict(cloth="#5a566a", trim="#d8cfb4", rune=(190, 184, 214), glow="#c8c4e8"),
    "lucifero": dict(cloth="#4a3a80", trim="#d8c4ff", rune=(190, 150, 255), glow="#9a5cff"),
    "megicula": dict(cloth="#7a2c3c", trim="#ffc8be", rune=(255, 120, 120), glow="#ff3a4a"),
    "beelzebub": dict(cloth="#34408a", trim="#c0caff", rune=(130, 150, 255), glow="#5a70ff"),
    "astaroth": dict(cloth="#806a30", trim="#fff0b0", rune=(255, 220, 110), glow="#ffc83a"),
}


def hx(a):
    return "#%02x%02x%02x" % tuple(int(round(float(v) * 255)) for v in a[:3])


def palette(cid):
    key = cid[len("dark_disciple"):].lstrip("_")
    t = TINTS.get(key, TINTS[""])
    cloth = t["cloth"]
    rgb = tuple(int(cloth[i:i + 2], 16) for i in (1, 3, 5))
    return dict(
        hair=cloth, hair_rgb=rgb, cloth=cloth, cloth_l=hx(K.lit(cloth, 1.45)), cloth_d=hx(K.deep(cloth, 0.6)), inner=hx(K.deep(cloth, 0.38)), inner_l=hx(K.deep(cloth, 0.55)),
        trim=t["trim"], trim_l=hx(K.lit(t["trim"], 1.12)), trim_d=hx(K.deep(t["trim"], 0.62)), rune=t["rune"], rune_hex="#%02x%02x%02x" % t["rune"], glow=t["glow"],
        skin=("#5e5868", "#7e7888", "#9c96a8"), bone="#ece4cc", bone_l="#faf4e2", bone_d="#b8ae94", ink="#2a2430",
        trou="#2c2a36", trou_l="#46445a", wrap="#c9c0a8", wrap_d="#8a8068", sash="#241f2c",
    )


# ================================================================================================ the head
def mask_face(f, P):
    """The face: ash skin with a rune on the brow (under the hood), then the bone skull mask over the lower face (y 2 to 8)."""
    sk = P["skin"]
    K.face_base(f, sk, side=0.1, chin=0.07)
    d = [(4.0, 0.35), (4.45, 0.95), (4.0, 1.55), (3.55, 0.95)]
    f.fill(f.m_polyline(d + [d[0]], 0.12), P["glow"], 0.9, glow=P["glow"])
    f.fill(f.m_line((3.2, 0.95), (4.8, 0.95), 0.08, 0.08), P["glow"], 0.8, glow=P["glow"])
    # the mask plate
    plate = f.m_rect(0.0, 2.0, 8.0, 8.0)
    f.fill(plate, H.lin((0, 2), (0, 8), [(0, P["bone_l"]), (0.35, P["bone"]), (1, P["bone_d"])]))
    f.fill(f.m_rect(0.0, 2.0, 0.55, 8.0), P["bone_d"], 0.7)
    f.fill(f.m_rect(7.45, 2.0, 8.0, 8.0), K.deep(P["bone_d"], 0.8), 0.8)
    f.fill(f.m_band(2.0, 2.28), P["ink"], 0.9)                                                         # the dark upper rim of the mask
    H.shadow_under(f, 2.28, 0.45, 0.4, mask=plate)
    for i in range(14):
        f.fill(f.m_ellipse(8 * H.hsh(151, i, 0), 2.4 + 5.4 * H.hsh(151, i, 1), 0.09, 0.07) * plate, P["bone_d"], 0.5)
    # the brow ridge, the eye holes and the lit eyes
    for s in (-1.0, 1.0):
        cx = 4.0 + s * 1.65
        f.fill(f.m_polyline([(cx - s * 1.25, 3.5), (cx, 3.35), (cx + s * 1.35, 3.0)], 0.3), P["bone_d"], 0.9)
        al = f.m_poly([(cx - s * 1.25, 4.35), (cx - s * 0.5, 3.75), (cx + s * 0.8, 3.7), (cx + s * 1.3, 4.1), (cx + s * 0.6, 4.65), (cx - s * 0.5, 4.7)])
        f.fill(al, H.lin((0, 3.7), (0, 4.7), [(0, "#000000"), (1, P["ink"])]), 1.0)
        f.fill(f.m_polyline([(cx - s * 1.25, 4.35), (cx - s * 0.5, 3.75), (cx + s * 0.8, 3.7), (cx + s * 1.3, 4.1)], 0.13), K.deep(P["bone_d"], 0.5), 0.9)
        f.fill(f.m_ellipse(cx + s * 0.05, 4.2, 0.68, 0.3) * al, P["glow"], 1.0, glow=P["glow"])
        f.fill(f.m_ellipse(cx + s * 0.05, 4.2, 0.34, 0.18) * al, "#ffffff", 0.9)
        f.glow_fill(f.m_ellipse(cx, 4.2, 1.5, 0.8, soft=3.0), P["glow"], 0.3)
    # the nose slit, a cheek rune line on each side, cracks in the plate
    f.fill(f.m_poly([(3.75, 5.0), (4.25, 5.0), (4.45, 6.2), (3.55, 6.2)]), P["ink"], 0.95)
    for s in (-1.0, 1.0):
        K.taper_line(f, [(4.0 + s * 2.9, 4.9), (4.0 + s * 3.2, 5.7), (4.0 + s * 2.8, 6.5)], 0.22, P["glow"], 0.95, glow=P["glow"], ends=0.3)
        K.taper_line(f, [(4.0 + s * 2.2, 5.5), (4.0 + s * 2.4, 6.2)], 0.14, P["glow"], 0.8, ends=0.4)
    for pts in ([(1.3, 2.4), (1.7, 3.0), (1.5, 3.3)], [(6.6, 2.4), (6.3, 2.9), (6.5, 3.3)], [(4.2, 2.35), (3.9, 2.9)]):
        f.fill(f.m_polyline(pts, 0.1), K.deep(P["bone_d"], 0.5), 0.8)
    # the jaw: a dark mouth slit and the tooth row
    f.fill(f.m_rect(1.4, 6.95, 6.6, 8.0), P["ink"], 0.97)
    for y0, y1 in ((7.0, 7.45), (7.55, 7.98)):
        for k in range(7):
            x0 = 1.45 + k * 0.72
            f.fill(f.m_rect(x0 + 0.04, y0, x0 + 0.66, y1), H.lin((0, y0), (0, y1), [(0, P["bone_l"]), (1, P["bone_d"])]))
    f.fill(f.m_rect(1.4, 7.46, 6.6, 7.54), K.deep(P["bone_d"], 0.5), 0.8)
    f.fill(f.m_rect(0.0, 7.7, 1.2, 8.0), P["ink"], 0.6)
    f.fill(f.m_rect(6.8, 7.7, 8.0, 8.0), P["ink"], 0.6)


def head_sides(cv, P, img64, spec):
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"], ears=False)
    for nm in ("west", "east"):
        f = cv.face("head", nm, False)
        # the dark mask strap across the temple and the rune on it
        f.fill(f.m_rect(0.0, 3.3, f.w, 4.3), P["ink"], 0.97)
        f.fill(f.m_rect(0.0, 3.3, f.w, 3.4), K.lit(P["ink"], 2.0), 0.4)
        f.fill(f.m_ellipse(f.w * 0.5, 3.8, 0.28, 0.2), P["trim"], 1.0, glow=P["glow"])
        f.fill(f.m_rect(0.0, 4.3, 0.7 if nm == "west" else f.w, 8.0) * 0.0, P["ink"], 0.0)


# ================================================================================================ the robe
def runes_on(f, mask, y0, y1, P, seed, a=0.85):
    c = P["rune_hex"]
    for j in range(int((y1 - y0) / 1.3)):
        for i in range(int(f.w / 1.2)):
            if H.hsh(seed, i, j) < 0.4:
                continue
            x, y = 0.6 + i * 1.2 + 0.2 * H.hsh(seed, i, j, 1), y0 + 0.6 + j * 1.3
            g = int(H.hsh(seed, i, j, 2) * 4)
            pts = ([(x - 0.3, y - 0.4), (x + 0.3, y - 0.4), (x, y + 0.4)], [(x - 0.3, y - 0.4), (x - 0.3, y + 0.4), (x + 0.3, y)], [(x, y - 0.45), (x, y + 0.45), (x + 0.3, y + 0.1)], [(x - 0.3, y), (x + 0.3, y), (x, y - 0.45), (x, y + 0.45)])[g]
            f.fill(f.m_polyline(pts, 0.17) * mask, c, a, glow=P["glow"] if H.hsh(seed, i, j, 3) > 0.5 else None)


def collar(f, P):
    m = f.m_band(0.0, 1.8)
    K.cloth(f, m, P["cloth_l"], top=1.2, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.26, gain=1.15, spec=0.2)
    f.fill(f.m_rect(0.0, 1.62, f.w, 1.8), P["trim"], 1.0, glow=P["glow"])
    f.fill(f.m_rect(0.0, 1.62, f.w, 1.68), P["trim_l"], 0.8)
    H.shadow_under(f, 1.8, 0.5, 0.35)


def robe_front(f, P):
    K.cloth(f, f.m_all(), P["cloth"], top=1.45, bottom=0.78, round_=0.14, fold_xs=[0.9, 1.8, 6.2, 7.1], fold_depth=0.14, fold_y0=1.9, fold_y1=9.0)
    # the dark inner panel down the middle with trim piping on both edges, lit runes on the two sides
    op = f.m_poly([(2.7, 1.8), (5.3, 1.8), (5.6, 9.0), (2.4, 9.0)])
    f.fill(op, H.lin((0, 1.8), (0, 9), [(0, P["inner_l"]), (1, P["inner"])]))
    for x0, x1, s in ((2.7, 2.4, -1.0), (5.3, 5.6, 1.0)):
        H.trim_line(f, [(x0, 1.8), (x1, 9.0)], P["trim"], width=0.26, spec=0.4, outline=0.5, glow=P["glow"])
    f.fill(f.m_line((4.0, 2.0), (4.0, 8.8), 0.16, 0.16) * op, P["inner"], 0.9)
    for x0, x1 in ((0.0, 2.2), (5.8, 8.0)):
        pm = f.m_rect(x0, 1.9, x1, 9.0)
        runes_on(f, pm, 2.0, 8.8, P, 3 if x0 < 4 else 4)
        K.sheen(f, pm, [x0 + 0.5 if x0 < 4 else x1 - 0.5], 2.0, 8.7, 0.25, P["cloth_l"], width=0.26)
    f.fill(f.m_rect(0.0, 1.9, 0.24, 9.0), P["cloth_l"], 0.45)
    f.fill(f.m_rect(7.76, 1.9, 8.0, 9.0), P["cloth_d"], 0.55)
    collar(f, P)


def robe_back(f, P):
    K.cloth(f, f.m_all(), P["cloth"], top=1.45, bottom=0.78, round_=0.14, fold_xs=[1.4, 2.8, 5.2, 6.6], fold_depth=0.14, fold_y0=1.9, fold_y1=9.0)
    f.fill(f.m_rect(0.0, 1.9, 0.24, 9.0), P["cloth_l"], 0.35)
    f.fill(f.m_rect(7.76, 1.9, 8.0, 9.0), P["cloth_d"], 0.55)
    runes_on(f, f.m_rect(0.0, 1.9, 8.0, 9.0), 2.0, 8.8, P, 9, 0.45)
    # the skull emblem on the back, big and lit
    cx, cy = 4.0, 4.9
    f.glow_fill(f.m_ellipse(cx, cy, 3.4, 3.4, soft=3.0), P["glow"], 0.22)
    f.fill(f.m_ellipse(cx, cy - 0.4, 2.1, 1.9), H.lin((0, cy - 2.3), (0, cy + 1.5), [(0, P["trim_l"]), (1, P["trim_d"])]), 1.0, glow=P["glow"])
    f.fill(f.m_rect(cx - 1.15, cy + 0.7, cx + 1.15, cy + 2.2), P["trim"], 1.0, glow=P["glow"])
    for s in (-1.0, 1.0):
        f.fill(f.m_ellipse(cx + s * 0.85, cy - 0.5, 0.58, 0.5), P["inner"], 1.0)
    f.fill(f.m_poly([(cx - 0.3, cy + 0.2), (cx + 0.3, cy + 0.2), (cx, cy + 0.85)]), P["inner"], 1.0)
    for k in range(5):
        f.fill(f.m_line((cx - 0.9 + k * 0.45, cy + 1.45), (cx - 0.9 + k * 0.45, cy + 2.1), 0.1, 0.1), P["inner"], 0.9)
    f.fill(f.m_ring(cx, cy, 2.55, 0.14), P["trim"], 0.8, glow=P["glow"])
    collar(f, P)


def robe_side(f, P, nm):
    K.cloth(f, f.m_all(), P["cloth"], top=1.45, bottom=0.78, round_=0.14, fold_xs=[1.2, 2.6], fold_depth=0.14, fold_y0=1.9, fold_y1=9.0)
    ed = f.w - 0.7 if nm == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.8)], P["trim_l"], 0.05, 0.5)
    runes_on(f, f.m_rect(0.0, 1.9, f.w, 9.0), 2.0, 8.8, P, 11 + (1 if nm == "east" else 0), 0.4)
    collar(f, P)


def sash(f, P, front):
    K.belt_band(f, 8.95, 9.7, P["sash"], stitch=P["trim_d"])
    H.trim_line(f, [(0.0, 9.0), (f.w, 9.0)], P["trim"], width=0.12, spec=0.3, outline=0.4, glow=P["glow"])
    if front:
        K.buckle(f, 4.0, 9.35, 1.6, 1.0, P["trim_d"], hole=P["sash"])
        f.ellipse(4.0, 9.35, 0.28, 0.28, P["trim_l"], a=1.0, glow=P["glow"])
    else:
        H.stud(f, 4.0, 9.35, 0.2, P["trim_l"], spec=0.8)


def skirt(f, P, nm):
    """The robe skirt (rows 9.7 to 12): ragged panels with a trim hem and a jagged lower edge."""
    m = f.m_band(9.7, 12.0)
    K.cloth(f, m, P["cloth"], top=1.25, bottom=0.72, round_=0.12, fold_xs=[f.w * 0.2, f.w * 0.5, f.w * 0.8], fold_depth=0.16, fold_y0=9.8, fold_y1=12.0)
    H.shadow_under(f, 9.72, 0.5, 0.3)
    f.fill(f.m_rect(0.0, 11.1, f.w, 11.28), P["trim"], 1.0, glow=P["glow"])
    f.fill(f.m_rect(0.0, 11.1, f.w, 11.16), P["trim_l"], 0.8)
    if nm in ("front", "back"):
        runes_on(f, f.m_rect(0.0, 9.8, f.w, 11.0), 9.8, 11.0, P, 17, 0.6)
    for x in (0.6, 1.9, 3.3, 4.7, 6.1, 7.3):                                                         # the torn hem
        f.fill(f.m_poly([(x - 0.35, 11.8), (x + 0.35, 11.8), (x + 0.05, 12.0)]), P["cloth_d"], 0.0)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                robe_front(f, P)
            elif nm == "back":
                robe_back(f, P)
            else:
                robe_side(f, P, nm)
            skirt(f, P, nm)
            sash(f, P, nm == "front")


# ================================================================================================ arms and legs
def arm(cv, part, P):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            K.cloth(f, f.m_band(0.0, 9.4), P["cloth"], top=1.4, bottom=0.78, round_=0.14, fold_xs=[f.w * 0.3, f.w * 0.62], fold_depth=0.14, fold_y0=0.3, fold_y1=9.0)
            f.fill(f.m_rect(0.0, 0.0, 0.24, 9.2), P["cloth_l"], 0.4)
            f.fill(f.m_rect(f.w - 0.24, 0.0, f.w, 9.2), P["cloth_d"], 0.5)
            runes_on(f, f.m_band(1.0, 8.0), 1.0, 8.0, P, 21 + SIDES.index(nm) + (6 if part == "arm_l" else 0), 0.5)
            # the wide cuff: a trim band with a lit rune rim
            K.cuff(f, 8.4, 9.6, P["cloth_d"], pipe=P["trim"], stud_color=P["trim_l"], studs=(f.w >= 4))
            f.fill(f.m_rect(0.0, 8.35, f.w, 8.5), P["trim"], 1.0, glow=P["glow"])
            H.shadow_under(f, 9.6, 0.5, 0.3)
            # the bone wraps on the hand
            w = f.m_band(9.6, 12.0)
            f.fill(w, H.lin((0, 9.6), (0, 12), [(0, P["wrap"]), (1, P["wrap_d"])]))
            K.wrap_lines(f, w, 9.7, 12.0, P["wrap_d"], P["wrap"], step=0.5, slant=0.3, a=0.9, width=0.2)
            if nm in ("front", "back"):
                f.fill(f.m_rect(f.w * 0.5 - 0.06, 11.2, f.w * 0.5 + 0.06, 12.0), P["wrap_d"], 0.85)
    for ov in (False, True):
        K.cap(cv, part, "top", P["cloth_l"], edge=P["cloth_d"], k_top=1.15, overlay=ov)


def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 9.0)
                K.cloth(f, m, P["trou"], top=1.3, bottom=0.8, round_=0.14, fold_xs=[0.9, 2.0, 3.0], fold_depth=0.12, fold_y0=0.3, fold_y1=8.8)
                f.fill(f.m_rect(0.0, 0.0, 0.26, 9.0), P["trou_l"], 0.4)
                H.shadow_under(f, 0.0, 0.6, 0.3)
                # the ankle boot with a trim cuff, a strap and a stud
                bm = f.m_band(8.6, 12.0)
                f.fill(bm, H.lin((0, 8.6), (0, 12), [(0, K.lit(P["trou"], 1.3)), (0.4, P["trou"]), (1, K.deep(P["trou"], 0.4))]))
                f.fill(f.m_rect(0.0, 8.6, f.w, 9.4), P["cloth_d"], 1.0)
                f.fill(f.m_rect(0.0, 8.6, f.w, 8.74), P["trim"], 1.0, glow=P["glow"])
                f.fill(f.m_rect(0.0, 9.4, f.w, 9.5), K.deep(P["cloth_d"], 0.5), 0.9)
                H.shadow_under(f, 9.5, 0.5, 0.3)
                f.fill(f.m_rect(0.0, 10.2, f.w, 10.5), K.deep(P["trou"], 0.55), 0.8)
                if nm == "front":
                    H.stud(f, f.w * 0.5, 10.35, 0.2, P["trim"], spec=0.8)
                    f.fill(f.m_ellipse(f.w * 0.5, 11.55, f.w * 0.4, 0.35), K.lit(P["trou"], 1.35), 0.45)
                f.fill(f.m_band(11.7, 12.0), "#0a0810", 1.0)
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            K.cap(cv, part, "bottom", "#0a0810", edge=None, k_top=1.0, overlay=ov)


# ================================================================================================ paint
def paint(spec, cid, masks):
    P = palette(cid)
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, P["hair"], P["hair_rgb"])
    head_cov = {("head", "front"): K.cover_of(cv, "head", "front")}
    head_sides(cv, P, img64, spec)
    for f in K.pair(cv, "head", "front"):
        mask_face(f, P)
    body(cv, P)
    arm(cv, "arm_r", P)
    arm(cv, "arm_l", P)
    legs(cv, P)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim, extra_covers=head_cov)
