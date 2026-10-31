"""Zagred (legacy), the first Zagred boss, HD 256x256 (docs/arc/hd_arc_bc7.md). One painter for both forms: `zagred_legacy_form2.py` is the same code with the phase-4 palette (K.is_form2).

What he is on the 64x64 sheet (tools/arc/char_zagred_legacy.py): a lanky black-violet devil, bare chested with a cracked ribcage lit by red cracks and runes, a red rune on the brow,
slit red eyes and fangs, a red glowing necklace, a dark sash with a red buckle, black trousers with a red stripe, bone spikes on the shoulders, bone bracers, bare clawed feet.
The curved horns, the bat wings with lit veins, the whip tail and (phase 4) the trident are the `pmx_zagred_legacy*` extras: the back keeps the spine and the shoulder blades only (no big panel).
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): the ribcage is a lit bone cage over a dark cavity with the cracks glowing between the ribs,
the brow rune, the cheek cracks, the veins; a bone shoulder plate with two thorns, a bone bracer with a red edge, long black fingers with pale claws; the legs have the red stripe with
a lit core and bare feet with claws. Form 2 (phase 4): redder and hotter (more cracks, white-hot cores, a brighter stripe, glowing knuckles).

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia are blocked by the session's proxy); from memory of the series (a tall black
devil with red eyes, horns, bat wings and a long tail) plus docs/arc/zagred.md and the 64x64 sheet source above. The colours (black-violet body, red rune light) are the mod's own.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "spiky_long"                                                        # KEPT classic: the long spiky hair (hair_map)

P1 = dict(
    hair="#14101c", hair_rgb=(20, 16, 28),
    skin=("#2a2236", "#403454", "#584a70"),
    bone="#8a7a9c", bone_l="#cfc4dc", bone_d="#4a3e5c", cavity="#120c1a",
    red="#ff3040", red_l="#ff8a96", red_d="#7a0a1c", red_g="#ff3040",
    trou="#241c2c", trou_l="#3c3050", sash="#2a1a2c", sash_l="#4a3250", eye_glow="#ff4a58",
    iris=("#5a0a18", "#ff3040", "#ffb0b8"), claw="#d8d0e0", hot=False,
)
P2 = dict(
    hair="#100c18", hair_rgb=(16, 12, 24),
    skin=("#241c30", "#3a2e4c", "#52446a"),
    bone="#8a7a9c", bone_l="#e4d8ee", bone_d="#44385a", cavity="#0e0814",
    red="#ff4a58", red_l="#ffc0c8", red_d="#8a0a24", red_g="#ff4a58",
    trou="#1e1626", trou_l="#382a48", sash="#261626", sash_l="#4a2a48", eye_glow="#ff6a76",
    iris=("#6a0a1c", "#ff4a58", "#ffd0d8"), claw="#f0e8f0", hot=True,
)


# ================================================================================================ the head
def head_front(cv, P, form2):
    f = cv.face("head", "front")
    sk = P["skin"]
    K.face_base(f, sk, side=0.1, chin=0.09)
    pal = K.hair_pal(P["hair"], light_k=1.9, dark_k=0.5)
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 1.6), (7.2, 1.3), (6.2, 1.9), (5.0, 1.3), (4.0, 1.7), (2.8, 1.3), (1.6, 1.9), (0.6, 1.3), (-0.3, 1.7)], y_bot=1.9)
    K.strands(f, pal, (0.9, 2.1, 3.3, 4.5, 5.7, 6.9), 0.0, 1.6, mass, a=0.45)
    for cx, lean in ((0.4, 0.15), (7.6, -0.15)):
        K.lock(f, pal, cx, 0.9, 0.6, 3.6, lean, 1.0)
    # the red rune on the brow: a ringed diamond with a cross line, lit
    s, g = P["red"], P["red_g"]
    d = [(4.0, 2.0), (4.65, 2.8), (4.0, 3.6), (3.35, 2.8)]
    f.fill(f.m_poly(d), "#12060c", 0.95)
    f.fill(f.m_polyline(d + [d[0]], 0.13), s, 0.95, glow=g)
    f.fill(f.m_line((3.05, 2.8), (4.95, 2.8), 0.1, 0.1), s, 0.9, glow=g)
    f.fill(f.m_line((4.0, 2.8), (4.0, 3.75), 0.1, 0.05), s, 0.9, glow=g)
    f.glow_fill(f.m_ellipse(4.0, 2.8, 1.6, 1.6, soft=3.0), g, 0.22)
    # hard black brows, slit red eyes with a black slit pupil
    brow = "#0e0a14"
    H.brow(f, (3.45, 4.05), (0.8, 3.3), brow, 0.55, 0.17, arch=0.0)
    H.brow(f, (4.55, 4.05), (7.2, 3.3), brow, 0.55, 0.17, arch=0.0)
    for cx in (2.3, 5.7):
        K.eye_bag(f, cx, 6.05, 2.1, "#2a1a30", 0.5)
        al = H.eye(f, cx, 5.15, 2.6, 1.25, P["iris"], lash="#08040a", tilt=0.34, lash_w=0.58, flick=0.42, lid_shadow=0.5, hi=0.7, pupil="#16040a")
        f.fill(f.m_ellipse(cx, 5.17, 0.13, 0.58) * al, "#08020a", 0.95)
        K.eye_glow(f, al, cx, 5.15, 2.6, 1.25, P["eye_glow"], 0.95)
        f.glow_fill(f.m_ellipse(cx, 5.15, 1.8, 1.0, soft=3.0), P["eye_glow"], 0.25)
    f.fill(f.m_polyline([(4.0, 5.6), (3.88, 6.2), (4.12, 6.35)], 0.15), H.tone(sk[0], 0.6), 0.85)
    # a fanged grin
    f.fill(f.m_poly([(2.4, 6.85), (4.0, 7.15), (5.6, 6.85), (5.1, 7.7), (4.0, 7.9), (2.9, 7.7)]), "#14040a", 1.0)
    for x, y0 in ((2.95, 6.9), (5.05, 6.9), (3.85, 7.1), (4.2, 7.1)):
        f.fill(f.m_poly([(x - 0.17, y0 + 0.05), (x + 0.17, y0 + 0.05), (x, y0 + (0.8 if x in (2.95, 5.05) else 0.45))]), "#f4eee4", 1.0)
    f.fill(f.m_polyline([(2.4, 6.85), (4.0, 7.15), (5.6, 6.85)], 0.16), "#5a1a24", 0.9)
    # the cracks and veins on the cheeks and temples
    for pts in ([(0.55, 4.4), (0.9, 5.4), (0.7, 6.5), (1.3, 7.5)], [(7.45, 4.4), (7.1, 5.4), (7.3, 6.5), (6.7, 7.5)]):
        K.taper_line(f, pts, 0.2, s, 0.95, glow=g, ends=0.3)
    for pts in ([(1.7, 3.0), (1.3, 3.7), (1.6, 4.3)], [(6.3, 3.0), (6.7, 3.7), (6.4, 4.3)], [(3.0, 6.4), (2.5, 6.9)], [(5.0, 6.4), (5.5, 6.9)]):
        K.taper_line(f, pts, 0.13, H.tone(s, 0.7), 0.8, ends=0.4)
    cr = [[(2.0, 5.9), (1.5, 6.5), (1.8, 7.3)], [(6.0, 5.9), (6.5, 6.5), (6.2, 7.3)]]
    if form2:
        cr += [[(4.0, 3.7), (3.8, 4.5)], [(1.0, 3.6), (0.5, 4.2)], [(7.0, 3.6), (7.5, 4.2)]]
    K.cracks_on(f, cr, s, glow=g, width=0.12, core="#ffd0d8" if form2 else "#ffb0b8", core_a=0.6 if form2 else 0.4, dark="#08020a")


def head_sides(cv, P):
    pal = K.hair_pal(P["hair"], light_k=1.9, dark_k=0.5)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.4, 0.8, 3.2, lean, 0.95)
        K.taper_line(f, [(3.2, 3.5), (3.5, 4.4), (3.1, 5.4), (3.4, 6.4)], 0.16, P["red"], 0.8, glow=P["red_g"], ends=0.3)


# ================================================================================================ the body
def neck_chain(f, P, nm):
    pts = [(0.2, 0.15), (1.6, 0.75), (3.0, 1.25), (4.0, 1.4), (5.0, 1.25), (6.4, 0.75), (7.8, 0.15)] if nm == "front" else [(0.2, 0.15), (4.0, 0.5), (7.8, 0.15)]
    K.chain_path(f, pts, step=0.62, color=P["red"], dark=P["red_d"], glow=P["red_g"], rx=0.34, ry=0.24)


def rib(f, P, s, y, drop, ln):
    """One rib: bone arc from the sternum round the side, a dark underside, a lit top edge. s = -1 left of the breastbone, +1 right."""
    pts = [(4.0 + s * 0.45, y - 0.05), (4.0 + s * 1.5, y - 0.28), (4.0 + s * 2.6, y - 0.12), (4.0 + s * ln, y + drop)]
    f.fill(f.m_polyline([(x, yy + 0.2) for x, yy in pts], 0.5), P["cavity"], 0.85)
    f.fill(f.m_polyline(pts, 0.42), P["bone_d"], 1.0)
    f.fill(f.m_polyline([(x, yy - 0.06) for x, yy in pts], 0.3), P["bone"], 1.0)
    f.fill(f.m_polyline([(x, yy - 0.15) for x, yy in pts], 0.1), P["bone_l"], 0.9)


def chest_front(f, P, ov):
    sk = P["skin"]
    K.limb_skin(f, sk, 0.0, 9.0, tendon=False)
    f.fill(f.m_ellipse(2.4, 1.6, 2.3, 1.1), H.tone(sk[2], 1.05), 0.35)
    f.fill(f.m_ellipse(5.6, 1.6, 2.3, 1.1), H.tone(sk[2], 1.05), 0.35)
    # the ribcage: a dark cavity, bone ribs, a breastbone, the cracks that glow between them
    cav = f.m_ellipse(4.0, 4.5, 3.6, 3.0)
    f.fill(cav, H.lin((0, 1.6), (0, 7.6), [(0, P["cavity"]), (1, H.tone(sk[0], 0.7))]), 0.95)
    f.fill(f.m_ring(4.0, 4.5, 3.5, 0.2), P["red_d"], 0.6)
    for k, (y, drop, ln) in enumerate(((2.4, 0.5, 3.6), (3.45, 0.5, 3.6), (4.5, 0.5, 3.4), (5.55, 0.5, 3.1), (6.55, 0.4, 2.6))):
        for s in (-1.0, 1.0):
            rib(f, P, s, y, drop, ln)
    f.fill(f.m_rect(3.72, 1.6, 4.28, 7.2), H.lin((3.72, 0), (4.28, 0), [(0, P["bone_d"]), (0.5, P["bone"]), (1, P["bone_d"])]))
    f.fill(f.m_rect(3.88, 1.6, 4.05, 7.2), P["bone_l"], 0.7)
    for y in (2.4, 3.45, 4.5, 5.55):                                                 # the rib joints on the breastbone
        f.fill(f.m_ellipse(4.0, y, 0.38, 0.22), P["bone_l"], 0.85)
    f.fill(f.m_poly([(3.3, 1.5), (4.7, 1.5), (4.0, 2.1)]), P["bone"], 1.0)
    # the glowing cracks between the ribs and the rune lines on the belly
    K.cracks_on(f, [[(2.2, 2.9), (1.7, 3.5), (2.1, 4.0)], [(6.0, 3.9), (6.4, 4.6), (5.9, 5.2)], [(2.0, 5.0), (1.6, 5.9)], [(5.6, 6.2), (6.2, 7.0)]], P["red"], glow=P["red_g"], width=0.13, core=P["red_l"], core_a=0.55, dark="#08020a", mask=cav)
    K.cracks_on(f, [[(1.1, 7.7), (1.9, 8.2), (2.6, 7.9)], [(6.9, 7.6), (6.1, 8.2), (5.4, 7.9)], [(4.0, 7.4), (4.2, 8.5)]], P["red"], glow=P["red_g"], width=0.13, core=P["red_l"], core_a=0.5, dark="#08020a")
    for y in (7.7, 8.5):                                                              # the belly muscle
        f.fill(f.m_polyline([(2.0, y), (3.4, y + 0.1), (4.6, y + 0.1), (6.0, y)], 0.14), H.tone(sk[0], 0.7), 0.55)
    neck_chain(f, P, "front")


def spine_back(f, P, ov):
    sk = P["skin"]
    K.limb_skin(f, sk, 0.0, 9.0, tendon=False)
    f.fill(f.m_ellipse(2.2, 2.4, 1.9, 2.0), H.tone(sk[2], 1.05), 0.3)                  # the shoulder blades
    f.fill(f.m_ellipse(5.8, 2.4, 1.9, 2.0), H.tone(sk[2], 1.05), 0.3)
    for sgn in (-1.0, 1.0):
        f.fill(f.m_polyline([(4.0 + sgn * 0.7, 1.2), (4.0 + sgn * 2.4, 2.0), (4.0 + sgn * 3.0, 3.4)], 0.2), H.tone(sk[0], 0.7), 0.55)
    for k in range(9):                                                                  # the spine: bumps from the neck to the sash
        y = 0.7 + k * 0.95
        f.fill(f.m_ellipse(4.0, y, 0.62 - 0.02 * k, 0.34), P["bone"], 1.0)
        f.fill(f.m_ellipse(3.9, y - 0.08, 0.3, 0.15), P["bone_l"], 0.8)
        f.fill(f.m_line((3.2 - 0.0 * k, y + 0.28), (4.8, y + 0.28), 0.12, 0.12), P["bone_d"], 0.7)
    for pts in ([(2.4, 2.4), (1.6, 3.4), (1.9, 4.6)], [(5.6, 2.4), (6.4, 3.4), (6.1, 4.6)], [(2.2, 5.8), (1.7, 6.9)], [(5.8, 5.8), (6.3, 6.9)]):
        K.cracks_on(f, [pts], P["red"], glow=P["red_g"], width=0.13, core=P["red_l"], core_a=0.5, dark="#08020a")
    neck_chain(f, P, "back")


def side_body(f, P, nm):
    sk = P["skin"]
    K.limb_skin(f, sk, 0.0, 9.0, tendon=False)
    edge = f.w - 0.7 if nm == "west" else 0.7
    for y in (2.4, 3.45, 4.5, 5.55):                                                     # the ribs seen from the side
        f.fill(f.m_polyline([(f.w * 0.15, y), (f.w * 0.5, y + 0.15), (f.w * 0.85, y)], 0.3), P["bone_d"], 0.85)
        f.fill(f.m_polyline([(f.w * 0.15, y - 0.08), (f.w * 0.5, y + 0.07), (f.w * 0.85, y - 0.08)], 0.14), P["bone"], 0.95)
    K.taper_line(f, [(edge, 2.0), (edge, 6.5)], 0.12, P["red"], 0.7, glow=P["red_g"], ends=0.3)


def sash(f, P, front):
    K.belt_band(f, 9.0, 10.5, P["sash"], stitch=P["red_d"])
    H.trim_line(f, [(0.0, 9.1), (f.w, 9.1)], P["red"], width=0.14, spec=0.3, outline=0.4, glow=P["red_g"])
    if front:
        K.buckle(f, 4.0, 9.75, 1.9, 1.3, P["bone_d"], hole=P["sash"])
        f.ellipse(4.0, 9.75, 0.34, 0.34, P["red_l"], a=1.0, glow=P["red_g"])
        f.fill(f.m_poly([(5.4, 10.5), (6.9, 10.5), (6.6, 12.0), (5.6, 11.7)]), H.lin((0, 10.5), (0, 12), [(0, P["sash"]), (1, H.tone(P["sash"], 0.6))]), 0.95)
    else:
        f.ellipse(4.0, 9.75, 0.24, 0.24, P["red_l"], a=1.0, glow=P["red_g"])


def hips(f, P, nm):
    m = f.m_band(10.5, 12.0)
    K.cloth(f, m, P["trou"], top=1.25, bottom=0.8, round_=0.12, fold_xs=[f.w * 0.3, f.w * 0.7], fold_depth=0.1, fold_y0=10.6, fold_y1=12.0)
    H.shadow_under(f, 10.5, 0.5, 0.28)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                chest_front(f, P, ov)
            elif nm == "back":
                spine_back(f, P, ov)
            else:
                side_body(f, P, nm)
            hips(f, P, nm)
            sash(f, P, nm == "front")


# ================================================================================================ the arms
def claws(f, P, y0):
    """The black hand with pale claws: finger gaps, a lit knuckle line, a claw at the tip of every finger."""
    sk = P["skin"]
    f.fill(f.m_band(y0, 12.0), H.lin((0, y0), (0, 12), [(0, sk[1]), (1, H.tone(sk[0], 0.8))]))
    if f.name in ("front", "back"):
        for i in range(1, 4):
            x = f.w * i / 4.0
            f.fill(f.m_line((x, y0 + 0.8), (x, 11.5), 0.22, 0.14), H.tone(sk[0], 0.55), 0.9)
        for i in range(4):
            x = f.w * (i + 0.5) / 4.0
            f.fill(f.m_poly([(x - 0.3, 11.35), (x + 0.3, 11.35), (x, 12.0)]), P["claw"], 1.0)
            f.fill(f.m_line((x - 0.12, 11.4), (x, 11.9), 0.08, 0.04), "#ffffff", 0.6)
        f.fill(f.m_polyline([(0.2, y0 + 0.6), (f.w * 0.5, y0 + 0.75), (f.w - 0.2, y0 + 0.6)], 0.16), H.tone(sk[0], 0.62), 0.7)
        if P["hot"]:
            for i in range(4):
                f.ellipse(f.w * (i + 0.5) / 4.0, y0 + 0.65, 0.12, 0.1, P["red_l"], a=0.9, glow=P["red_g"])
    else:
        f.fill(f.m_poly([(0.3, 11.4), (f.w - 0.3, 11.4), (f.w * 0.5, 12.0)]), P["claw"], 1.0)


def arm(cv, part, P, form2):
    sk = P["skin"]
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            K.limb_skin(f, sk, 2.5, 12.0, tendon=True)
            for pts in ([(f.w * 0.3, 3.4), (f.w * 0.25, 4.6), (f.w * 0.4, 5.6)], [(f.w * 0.65, 9.9), (f.w * 0.6, 10.7)]):
                K.taper_line(f, pts, 0.12, P["red"], 0.7, glow=P["red_g"], ends=0.4)
            # the bone shoulder plate with its thorns and the bone bracer with a red edge
            K.plate_band(f, 0.0, 3.2, P["bone_d"], trim=P["red"], glow=P["red_g"], segs=2, light_k=1.9, rivets=[0.7, f.w - 0.7], jag=0.35, seed=4)
            K.plate_band(f, 6.4, 9.9, P["bone_d"], trim=P["red"], glow=P["red_g"], segs=2, light_k=1.9, rivets=[0.7, f.w - 0.7], seed=6)
            H.shadow_under(f, 3.3, 0.6, 0.3)
            for bx, by, tx, ty, wd in ((f.w * 0.28, 2.9, f.w * 0.16, 0.6, 0.85), (f.w * 0.74, 2.9, f.w * 0.84, 0.7, 0.85)):
                K.shard(f, bx, by, tx, ty, wd, P["bone_l"], H.tone(P["bone"], 0.8), edge=P["bone_d"], glow=None, a=1.0)
            claws(f, P, 10.0)
            # the bracer is drawn over the hand's start: restore the claw line crisp
    for ov in (False, True):
        K.cap(cv, part, "top", P["bone"], edge=P["bone_d"], k_top=1.3, overlay=ov)


# ================================================================================================ the legs
def legs(cv, P, form2):
    sk = P["skin"]
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 10.4)
                K.cloth(f, m, P["trou"], top=1.3, bottom=0.8, round_=0.14, fold_xs=[0.9, 2.9], fold_depth=0.1, fold_y0=0.3, fold_y1=10.0)
                H.shadow_under(f, 0.0, 0.6, 0.25)
                # the red stripe with a lit core down the outer side (front) or the middle (back, sides)
                sx = (f.w - 1.1, f.w - 0.3) if nm == "front" and part == "leg_r" else ((0.3, 1.1) if nm == "front" else (f.w * 0.4, f.w * 0.6))
                f.fill(f.m_rect(sx[0], 0.0, sx[1], 10.4) * m, P["red_d"], 1.0)
                f.fill(f.m_rect(sx[0] + 0.2, 0.0, sx[1] - 0.2, 10.4) * m, P["red"], 1.0, glow=P["red_g"])
                f.fill(f.m_rect((sx[0] + sx[1]) / 2 - 0.05, 0.0, (sx[0] + sx[1]) / 2 + 0.05, 10.4) * m, P["red_l"], 0.7)
                f.fill(f.m_rect(0.0, 0.0, 0.26, 10.2), P["trou_l"], 0.35)
                for y, k in ((3.6, 0.45), (4.2, 0.3)):                                      # the knee
                    f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.26), (f.w - 0.2, y + 0.05)], 0.14) * m, H.tone(P["trou"], 0.5), k)
                # the bare black foot with claws
                foot = f.m_band(10.4, 12.0)
                f.fill(foot, H.lin((0, 10.4), (0, 12), [(0, sk[1]), (1, H.tone(sk[0], 0.82))]))
                H.shadow_under(f, 10.42, 0.5, 0.3)
                f.fill(f.m_rect(0.0, 10.4, f.w, 10.62), P["trou"], 0.95)
                if nm == "front":
                    for i in range(1, 4):
                        f.fill(f.m_line((f.w * i / 4.0, 11.2), (f.w * i / 4.0, 11.9), 0.2, 0.14), H.tone(sk[0], 0.55), 0.9)
                    for i in range(4):
                        x = f.w * (i + 0.5) / 4.0
                        f.fill(f.m_poly([(x - 0.28, 11.6), (x + 0.28, 11.6), (x, 12.0)]), P["claw"], 1.0)
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
    legs(cv, P, form2)
    return K.finish(cv, covers, hair, P["hair_rgb"], slim)
