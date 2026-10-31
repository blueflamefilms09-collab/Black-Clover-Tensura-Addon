"""Patolli, the elf who leads the Eye of the Midnight Sun, HD 256x256 (docs/arc/hd_arc_bc6.md). One painter for both forms: `patolli_form2.py` is the same code with the Dark Elf palette.

What he wears is the 64x64 sheet's outfit (tools/arc/char_patolli.py): a floor-long white robe edged in black over a dark inner shirt, a stand collar, wide sleeves with black cuffs, a black sash
with a gold buckle, and a gold circlet with a gem on the brow. The black mantle with its gold edge (`cape`) hangs from the shoulders down the back, the four-leaf grimoire is in the left hand and
(form 2) the broken halo floats over the head: those are the `pmx_patolli*` extras, so the back of the robe is kept plain under the mantle and the left hand is a bare hand. Slim, tall.
Form 2 (the Dark Elf): dark violet skin, black eyes ringed with glowing violet fire, violet war paint, a night robe with violet trim and sash, a violet circlet; the glow map is the eyes, the war
paint and the circlet's gem (as on the sheet).
Glow (form 1): the golden eyes and the gem of the circlet.
Drawn at 4x detail on both layers: narrow gold eyes with a red wing of eyeshadow and the two red tattoo lines under each eye, thin white brows, a thin cold smirk, two long face-framing locks of
white hair (the braid itself is the classic 3D hair `braid`, kept from the sheet: no anime cut is a braid); the circlet with its gem; the robe with its black front edges, a second thin black line
inside them, the dark inner shirt in the opening, the stand collar edged in gold, the sash with a gold buckle and a hanging tail, the black cuffs with a gold line, long bare fingers, a robe hem in
black with a gold line.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); from memory of the series plus docs/arc/patolli.md (which cites the wiki
summaries): tall and slim, long white braided hair, gold eyes ringed by red tattoos, a small gold cap with a gem, a long white hooded robe with black lines over dark and light layers; the Dark Elf
form has dark skin, black eyes and violet. Sources disagree on small details; the sheet follows the wiki.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc6 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "braid"                                                    # KEPT classic (hair_map): the long braid is the character; no anime cut is a braid

P1 = dict(
    hair="#f6f3ec", hair_rgb=(246, 243, 236),
    skin=("#e4c0ae", "#f4d8c6", "#fdeade"),
    robe="#f0ebe0", robe_l="#ffffff", robe_d="#b8b0a0",
    black="#26262f", black_l="#4a4a5a",
    trim="#d8ae4a", trim_l="#fff0b0", trim_d="#8a6a20",
    inner="#30303c", sash="#2a2a34",
    paint="#c02a2a", paint_d="#7a1418", paint_glow=None,
    iris=("#a06c14", "#ecc452", "#fff4b8"), sclera="#fffdf4", lash="#2a1c18", eye_glow="#ffe58a",
    gem="#ffd24a", gem_glow="#ffe58a",
    brow="#d4d0c8",
)
P2 = dict(
    hair="#f6f2ff", hair_rgb=(246, 242, 255),
    skin=("#4a3c5c", "#5e4c72", "#7a6890"),
    robe="#32204a", robe_l="#583c82", robe_d="#180e26",
    black="#120a1e", black_l="#3a2658",
    trim="#a24ae4", trim_l="#e0a8ff", trim_d="#5a2a8a",
    inner="#160c22", sash="#140a20",
    paint="#a24ae4", paint_d="#5a2a8a", paint_glow="#c060ff",
    iris=("#05030a", "#150f22", "#3c2060"), sclera="#2a2038", lash="#0a0610", eye_glow="#c060ff",
    gem="#d080ff", gem_glow="#c060ff",
    brow="#cfc4e4",
)


# ================================================================================================ the head
def war_paint(f, P, cx, outer):
    """The red eyeshadow wing round an eye and the two tattoo lines under it."""
    col, dk, gl = P["paint"], P["paint_d"], P["paint_glow"]
    f.fill(f.m_ellipse(cx, 4.6, 1.75, 0.75, soft=2.2), col, 0.5)
    f.fill(f.m_polyline([(cx + outer * 1.0, 4.45), (cx + outer * 2.0, 4.15), (cx + outer * 2.6, 3.7)], 0.3), col, 0.9, glow=gl)
    for dx, y1 in ((0.0, 6.85), (outer * 0.55, 6.5)):
        f.fill(f.m_line((cx + dx, 6.05), (cx + dx + outer * 0.1, y1), 0.26, 0.16), dk, 0.9)
        f.fill(f.m_line((cx + dx, 6.05), (cx + dx + outer * 0.1, y1), 0.14, 0.08), col, 1.0, glow=gl)


def circlet(f, P):
    """The gold circlet on the brow with a gem (drawn on both layers)."""
    band = f.m_band(2.15, 2.95)
    f.fill(f.m_rect(0.0, 2.08, f.w, 3.02), H.tone(P["trim"], 0.4), 0.6)
    f.fill(band, H.metal_paint(2.15, 2.95, P["trim"]))
    H.bevel(f, band, depth=0.25, gain=1.2, spec=0.5)
    f.fill(f.m_rect(0.0, 2.15, f.w, 2.22), P["trim_l"], 0.7)
    f.fill(f.m_poly([(4.0, 1.55), (4.6, 2.55), (4.0, 3.55), (3.4, 2.55)]), H.tone(P["trim"], 0.35), 1.0)
    H.gem(f, 4.0, 2.55, 0.46, P["gem"], setting=P["trim"], facets=4, spec=0.9, glow=P["gem_glow"], ry=0.62)


def head_front(cv, P, form2):
    f = cv.face("head", "front")
    fo = cv.face("head", "front", True)
    fo.clear()
    K.face_base(f, P["skin"], side=0.08, chin=0.07)
    pal = K.hair_pal(P["hair"], light_k=1.1, dark_k=0.7)
    # white hair framing the face: a parted fringe and two long locks
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.5, 1.8), (6.4, 1.2), (5.0, 0.8), (4.0, 1.0), (3.0, 0.8), (1.6, 1.2), (0.6, 1.8), (-0.3, 2.3)], y_bot=2.0)
    K.strands(f, pal, (0.8, 2.0, 3.2, 4.8, 6.0, 7.2), 0.0, 1.6, mass, a=0.3)
    for cx, lean in ((0.5, 0.1), (7.5, -0.1)):
        K.lock(f, pal, cx, 1.2, 0.7, 6.6, lean, 1.0)
    for cx, lean in ((1.25, 0.15), (6.75, -0.15)):
        K.lock(f, pal, cx, 0.9, 1.2, 3.6, lean, 0.92)
    # thin brows, a cold look
    H.brow(f, (3.5, 3.95), (1.1, 3.55), P["brow"], 0.24, 0.12, arch=0.04)
    H.brow(f, (4.5, 3.95), (6.9, 3.55), P["brow"], 0.24, 0.12, arch=0.04)
    for cx, outer in ((2.35, -1.0), (5.65, 1.0)):
        war_paint(f, P, cx, outer)
    for cx in (2.35, 5.65):
        al = H.eye(f, cx, 5.05, 2.5, 1.55, P["iris"], lash=P["lash"], sclera=P["sclera"], tilt=0.3, lash_w=0.5, flick=0.44, lid_shadow=0.4, hi=0.8)
        K.eye_glow(f, al, cx, 5.05, 2.5, 1.55, P["eye_glow"], 0.9)
        if form2:                                                        # black eyes ringed with violet fire: the glow runs round the lid
            f.glow_fill(f.m_ellipse(cx, 5.05, 1.7, 1.0, soft=3.0) * (1.0 - f.m_ellipse(cx, 5.05, 1.1, 0.62, soft=2.0)), "#c060ff", 0.55)
    H.nose(f, 4.0, 6.3, "#d0a894" if not form2 else "#3e3050", 0.2)
    H.mouth_smile(f, 4.0, 7.35, 1.8, color="#a86a68" if not form2 else "#2a1838", lip="#d49a94" if not form2 else "#6a4a8a", depth=0.12, thick=0.15)
    f.fill(f.m_line((4.9, 7.3), (5.2, 7.0), 0.14, 0.1), "#b0807a" if not form2 else "#3a2a50", 0.7)
    for g in (f, fo):
        circlet(g, P)


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.1, dark_k=0.7)
    for nm in ("west", "east"):
        for ov in (False, True):
            g = cv.face("head", nm, ov)
            if ov:
                g.clear()
            f = g
            band = f.m_band(2.15, 2.95)
            f.fill(f.m_rect(0.0, 2.08, f.w, 3.02), H.tone(P["trim"], 0.4), 0.6)
            f.fill(band, H.metal_paint(2.15, 2.95, P["trim"]))
            H.bevel(f, band, depth=0.25, gain=1.2, spec=0.4)
            f.fill(f.m_rect(0.0, 2.15, f.w, 2.22), P["trim_l"], 0.7)
    for nm in ("back",):
        for ov in (False, True):
            g = cv.face("head", nm, ov)
            if ov:
                g.clear()
            band = g.m_band(2.15, 2.95)
            g.fill(g.m_rect(0.0, 2.08, g.w, 3.02), H.tone(P["trim"], 0.4), 0.6)
            g.fill(band, H.metal_paint(2.15, 2.95, P["trim"]))
            H.bevel(g, band, depth=0.25, gain=1.2, spec=0.4)
            g.fill(g.m_rect(0.0, 2.15, g.w, 2.22), P["trim_l"], 0.7)


# ================================================================================================ the robe
def collar(f, name, P, form2):
    m = f.m_band(0.0, 1.9)
    K.cloth(f, m, P["black"], top=1.9 if not form2 else 1.6, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.25, gain=1.15, spec=0.2)
    f.fill(f.m_rect(0.0, 1.74, f.w, 1.88), P["trim"], 1.0)
    f.fill(f.m_rect(0.0, 1.74, f.w, 1.8), P["trim_l"], 0.7)
    f.fill(f.m_rect(0.0, 1.88, f.w, 2.0), "#08080c", 0.55)
    K.seam(f, [(0.15, 0.95), (f.w - 0.15, 0.95)], P["trim_d"], 0.05, 0.6)
    H.shadow_under(f, 1.98, 0.5, 0.3)


def sash(f, P, front, tail=False):
    K.belt_band(f, 8.55, 10.1, P["sash"], stitch=P["trim_d"], edge_k=1.9)
    if front:
        f.fill(f.m_rect(0.0, 8.5, f.w, 8.62), P["trim"], 0.85)
        K.buckle(f, 4.0, 9.32, 2.1, 1.45, P["trim"], hole="#0c0a12")
        H.gem(f, 4.0, 9.32, 0.3, P["gem"], setting=P["trim"], facets=4, spec=0.8, glow=P["gem_glow"] if True else None)
    else:
        H.stud(f, 4.0, 9.32, 0.2, P["trim"], spec=0.7)


def robe_body(f, P, form2, name):
    K.cloth(f, f.m_all(), P["robe"], top=1.04 if not form2 else 1.5, bottom=0.82 if not form2 else 0.75, round_=0.1, fold_xs=[0.9, 1.9, 6.1, 7.1], fold_depth=0.09, fold_y0=2.0, fold_y1=11.6)


def body_front(f, P, form2):
    robe_body(f, P, form2, "front")
    for x0, x1 in ((0.0, 2.4), (5.6, 8.0)):
        K.sheen(f, f.m_rect(x0, 2.0, x1, 11.8), [x0 + 0.7 if x0 < 4 else x1 - 0.7], 2.2, 11.2, 0.28 if not form2 else 0.35, "#ffffff" if not form2 else "#c8a0ff", width=0.26)
    # the dark inner shirt in the opening
    op = f.m_rect(2.4, 1.9, 5.6, 8.6)
    f.fill(op, H.lin((0, 1.9), (0, 8.6), [(0, K.lit(P["inner"], 1.3)), (1, H.tone(P["inner"], 0.7))]))
    f.fill(f.m_rect(2.4, 1.9, 2.8, 8.6), "#050508", 0.5)
    f.fill(f.m_line((4.0, 2.0), (4.0, 8.4), 0.14, 0.14), P["black_l"], 0.5)
    for y in (3.0, 4.4, 5.8, 7.2):
        H.stud(f, 4.0, y, 0.17, P["trim"], spec=0.8)
    # the black front edges of the robe: a thick edge and a thin line inside it
    for x, sg in ((2.4, 1), (5.6, -1)):
        f.fill(f.m_rect(x - 0.3 if sg > 0 else x, 1.9, x if sg > 0 else x + 0.3, 12.0), P["black"], 1.0)
        f.fill(f.m_line((x - 0.5 * sg, 2.0), (x - 0.5 * sg, 12.0), 0.14, 0.14), P["black"], 0.9)
        f.fill(f.m_line((x - 0.1 * sg, 2.0), (x - 0.1 * sg, 12.0), 0.1, 0.1), P["trim"], 0.55)
    # more black lines down the skirt
    for x in (1.0, 7.0):
        f.fill(f.m_line((x, 10.3), (x, 12.0), 0.14, 0.14), P["black"], 0.7)
    collar(f, "front", P, form2)
    sash(f, P, True)
    # the sash tail hanging on the right (the viewer's right) of the buckle
    tail = f.m_poly([(5.4, 10.1), (6.5, 10.1), (6.6, 12.0), (5.3, 12.0)])
    f.fill(tail, H.lin((0, 10.1), (0, 12.0), [(0, K.lit(P["sash"], 1.7)), (1, H.tone(P["sash"], 0.7))]))
    f.fill(f.m_rect(5.3, 11.55, 6.6, 12.0), P["trim"], 0.95)
    K.seam(f, [(0.2, 2.6), (2.1, 2.3)], P["robe_d"], 0.05, 0.6)
    K.seam(f, [(7.8, 2.6), (5.9, 2.3)], P["robe_d"], 0.05, 0.6)


def body_back(f, P, form2):
    robe_body(f, P, form2, "back")
    K.sheen(f, f.m_rect(0.0, 2.0, 8.0, 11.8), [1.5, 6.5], 2.2, 11.2, 0.22, "#ffffff" if not form2 else "#c8a0ff", width=0.28)
    # the hood hangs folded between the shoulders: a lined cowl (the mantle covers most of it)
    hood = f.m_poly([(1.2, 1.0), (6.8, 1.0), (7.3, 3.2), (4.0, 4.4), (0.7, 3.2)])
    f.fill(hood, H.lin((0, 1.0), (0, 4.4), [(0, K.lit(P["robe"], 1.12)), (1, H.tone(P["robe"], 0.7))]))
    f.fill(f.m_polyline([(0.7, 3.2), (4.0, 4.4), (7.3, 3.2)], 0.2), P["black"], 0.8)
    H.folds(f, hood, [2.4, 4.0, 5.6], depth=0.12, width=0.25, y0=1.2, y1=4.2)
    collar(f, "back", P, form2)
    f.fill(f.m_line((4.0, 4.6), (4.0, 12.0), 0.16, 0.16), P["black"], 0.7)
    sash(f, P, False)


def body_side(f, P, form2, name):
    robe_body(f, P, form2, name)
    collar(f, name, P, form2)
    ed = f.w - 0.35 if name == "west" else 0.35                          # the black front edge of the robe, seen from the side
    f.fill(f.m_line((ed, 2.0), (ed, 12.0), 0.5, 0.5), P["black"], 0.95)
    sash(f, P, False)


def body(cv, P, form2):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, P, form2)
            elif nm == "back":
                body_back(f, P, form2)
            else:
                body_side(f, P, form2, nm)


# ================================================================================================ arms and legs
def arm(cv, part, P, form2):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.5)
            K.cloth(f, sleeve, P["robe"], top=1.04 if not form2 else 1.5, bottom=0.84 if not form2 else 0.75, round_=0.14, fold_xs=[0.7, 1.7], fold_depth=0.09, fold_y0=1.8, fold_y1=7.8)
            K.sheen(f, sleeve, [f.w * 0.3], 1.8, 7.6, 0.28 if not form2 else 0.34, "#ffffff" if not form2 else "#c8a0ff", width=0.26)
            f.fill(f.m_rect(0.0, 1.0, f.w, 1.3), P["black"], 0.95)                              # the black shoulder line
            f.fill(f.m_rect(0.0, 1.0, f.w, 1.07), P["trim"], 0.8)
            H.stud(f, f.w * 0.5, 0.45, 0.17, P["trim"], spec=0.8)
            for y, k in ((4.8, 0.45), (5.35, 0.3)):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.28), (f.w, y + 0.05)], 0.15) * sleeve, P["robe_d"], k)
            if nm in ("west", "east"):
                f.fill(f.m_line((f.w * 0.5, 1.4), (f.w * 0.5, 6.6), 0.2, 0.2) * sleeve, P["robe_d"], 0.5)
            # the black cuff of the wide sleeve with a gold line
            K.cuff(f, 6.9, 8.6, P["black"], pipe=P["trim"], stud_color=P["trim_l"], spec=0.2)
            f.fill(f.m_rect(0.0, 6.86, f.w, 6.96), P["trim"], 0.9)
            # the long bare hand
            hand = f.m_band(8.6, 12.0)
            sk = P["skin"]
            f.fill(hand, H.lin((0, 8.6), (0, 12.0), [(0, H.tone(sk[1], 0.92)), (0.5, sk[1]), (1, H.tone(sk[0], 0.95))]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(hand, 1.0 - 0.1 * (1.0 - H.smoothstep(0.0, 0.8, side)), 0.9)
            H.shadow_under(f, 8.65, 0.8, 0.35, mask=hand)
            if nm in ("front", "back"):
                for i in (1, 2):
                    x = f.w * i / 3
                    f.fill(f.m_line((x, 10.7), (x, 11.95), 0.2, 0.16), H.tone(sk[0], 0.72), 0.65)
            f.fill(f.m_band(10.55, 10.7), H.tone(sk[0], 0.74), 0.5)


def arm_tops(cv, P):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", P["robe"], edge=P["black"], k_top=1.04, overlay=ov)


def legs(cv, P, form2):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_all()
                K.cloth(f, m, P["robe"], top=1.04 if not form2 else 1.5, bottom=0.8 if not form2 else 0.7, round_=0.12, fold_xs=[1.0, 2.0, 3.0], fold_depth=0.12, fold_y0=0.2, fold_y1=9.6)
                K.sheen(f, m, [f.w * 0.3], 0.4, 9.4, 0.22 if not form2 else 0.3, "#ffffff" if not form2 else "#c8a0ff")
                # the black hem band with a gold line above it, and a black line along the front edge of the skirt
                f.fill(f.m_rect(0.0, 10.0, f.w, 12.0), H.lin((0, 10.0), (0, 12.0), [(0, K.lit(P["black"], 2.0)), (1, P["black"])]))
                f.fill(f.m_rect(0.0, 9.85, f.w, 10.05), P["trim"], 0.95)
                f.fill(f.m_rect(0.0, 9.85, f.w, 9.92), P["trim_l"], 0.7)
                f.fill(f.m_rect(0.0, 10.05, f.w, 10.18), "#08080c", 0.6)
                H.shadow_under(f, 10.0, 0.6, 0.3)
                if nm in ("front", "back"):
                    xe = 0.2 if part == "leg_l" else f.w - 0.2
                    f.fill(f.m_line((xe, 0.0), (xe, 10.0), 0.24, 0.24), P["black"], 0.6)
                f.fill(f.m_rect(0.0, 11.75, f.w, 12.0), "#050508", 0.85)


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
