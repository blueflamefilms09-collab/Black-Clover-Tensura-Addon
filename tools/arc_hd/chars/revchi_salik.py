"""Revchi Salik, the Chain Thief of the first chapter, HD 256x256 (docs/arc/hd_arc_bc6.md). One character, one form (the looks table has no `_form2` for him).

What he wears is the 64x64 sheet's outfit (tools/arc/char_revchi_salik.py): a dark thief's coat with a steel trim and a high collar over a pale shirt, a purple scarf, long sleeves with steel cuffs,
fingerless gloves, a double belt with a steel buckle and pouches, dark trousers and knee boots. The ragged purple cape, the steel chains coiled round both forearms and the chain with the kunai
blade hanging from the belt at the back are the `pmx_revchi_salik` extras (the rings sit on the forearms, the tail at the right hip of the back), so the back of the coat is kept plain and the
cuffs and the gloves are drawn clear of the rings. Slim arms (the sheet's), height scale kept.
Glow: only the steel of the chains (as on the sheet): the diagonal chain across the chest (an addition of this paint, a short bandolier of the same links) and the buckle; no glow in the eyes.
Drawn at 4x detail on both layers (the second layer and the hair are dropped beyond 24 blocks): a greedy half-lidded eye with a dark ring under it, a thin sharp brow, the other eye cut by the burn
scar that covers the left side of the face (a raised pink mass with a dark rim and shiny streaks), stubble on the jaw, a lopsided smirk, long droopy side locks (the cut itself is the 3D hair
`anime_bangs_long`); the coat with steel piping along the opening, the lapels and the hem, a torn hem, a pale laced shirt in the opening, the purple scarf in two turns with a hanging end, the
double belt with two pouches and a steel buckle, the sleeves with a steel cuff and studs, fingerless gloves with bare fingertips, trousers and knee boots with a steel turn-down and straps.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); from memory of the series plus docs/arc/revchi_salik.md (which cites the wiki
summaries): a slim man in his thirties with long droopy dark hair, a large burn scar over the left side of his face and a greedy, calculating look. Every colour of the clothes is the sheet's
(the doc marks them unverified).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc6 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_swept"                                        # long droopy hair swept over one side of the brow (hair_map says anime_short; anime_bangs_long is a spiky pom, anime_long_open reads like a bob)

P = dict(
    hair="#2a2622", hair_rgb=(66, 58, 54), tint=(50, 42, 42),
    skin=("#dcbca8", "#eed2bc", "#fcebdc"),
    coat="#2c2a36", coat_l="#4a4860", coat_d="#14131a",
    steel="#7a8498", steel_d="#434a5c", steel_l="#c4cee0",
    shirt="#cfc8bc", shirt_d="#8a8478",
    scarf="#5a4470", scarf_l="#836aa4", scarf_d="#2e2040",
    belt="#3c2c24", belt_d="#1a110c", buckle="#aab4c4", pouch="#4a382c",
    pants="#1e1e26", boot="#2a2220", glove="#2a2a30",
    iris=("#262630", "#60607a", "#b8b8d4"), lash="#14141a",
    burn="#c8786e", burn_l="#ecaa9c", burn_d="#8a4048", burn_glint="#ffd8cc",
    chain="#aab4c4", chain_d="#262a36", chain_glow="#bcd0ff",
)


# ================================================================================================ the head
def burn_scar(f):
    """The big burn scar over the left side of the face (the viewer's right): a raised mass over the cheek, the brow and the eyelid, a dark rim, shiny streaks and a puckered edge."""
    poly = [(4.7, 2.9), (6.4, 2.6), (7.7, 3.0), (8.0, 4.4), (8.0, 6.7), (7.2, 7.9), (6.0, 7.5), (5.2, 6.6), (4.5, 5.6), (4.9, 4.2)]
    m = f.m_poly(poly)
    f.fill(f.m_polyline(poly + [poly[0]], 0.34), P["burn_d"], 0.6)
    f.fill(m, H.lin((0, 2.6), (0, 7.9), [(0, K.lit(P["burn"], 1.1)), (0.55, P["burn"]), (1, H.tone(P["burn"], 0.82))]), 0.94)
    # the mottled mass: darker blotches and pale shiny streaks
    for cx, cy, rx, ry, c, a in ((5.6, 4.0, 0.7, 0.5, P["burn_d"], 0.45), (6.9, 5.2, 0.6, 0.8, P["burn_d"], 0.45), (6.0, 6.5, 0.8, 0.5, P["burn_d"], 0.4), (7.3, 3.5, 0.5, 0.4, P["burn_d"], 0.4)):
        f.fill(f.m_ellipse(cx, cy, rx, ry, soft=2.0) * m, c, a)
    for pts in ([(5.0, 3.6), (5.9, 3.3), (6.8, 3.7)], [(5.4, 5.5), (6.2, 5.9), (7.2, 6.2)], [(7.3, 4.1), (7.7, 5.4)], [(5.6, 7.0), (6.4, 7.3)]):
        f.fill(f.m_polyline(pts, 0.16) * m, P["burn_l"], 0.7)
    for pts in ([(5.2, 4.9), (6.0, 5.0)], [(6.6, 6.5), (7.4, 6.9)]):
        f.fill(f.m_polyline(pts, 0.1) * m, P["burn_glint"], 0.55)
    return m


def head_front(cv):
    f = cv.face("head", "front")
    fo = cv.face("head", "front", True)
    fo.clear()
    K.face_base(f, P["skin"], side=0.08, chin=0.07)
    pal = K.hair_pal(P["hair"], light_k=1.45, dark_k=0.6)
    # long droopy bangs: a heavy fringe that falls over the brow and two long side locks
    rag = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.7), (7.4, 3.1), (6.6, 2.3), (5.6, 3.0), (4.7, 2.2), (3.6, 2.9), (2.6, 2.2), (1.6, 3.0), (0.7, 2.4), (-0.3, 2.9)]
    mass = K.fringe_mass(f, pal, rag, y_top=0.0, y_bot=3.1)
    K.strands(f, pal, (0.8, 1.9, 3.1, 4.3, 5.5, 6.7, 7.5), 0.0, 2.8, mass, a=0.4)
    for cx, lean, ln in ((0.45, 0.14, 6.6), (7.55, -0.14, 6.4)):
        K.lock(f, pal, cx, 1.1, 1.4, ln, lean, 1.0)
    for cx, lean in ((1.35, 0.2), (6.65, -0.2)):
        K.lock(f, pal, cx, 0.8, 2.0, 3.6, lean, 0.9)
    # the burn scar first, the eye cut into it
    burn_scar(f)
    # the greedy eye (the viewer's left): half lidded, sharp, a ring under it
    H.brow(f, (3.55, 4.1), (1.0, 3.55), "#3c342c", 0.34, 0.12, arch=0.03)
    K.eye_bag(f, 2.3, 6.05, 2.3, "#a08490", 0.55)
    H.eye(f, 2.3, 5.2, 2.7, 1.5, P["iris"], lash=P["lash"], tilt=0.3, lash_w=0.55, flick=0.42, lid_shadow=0.55, hi=0.8)
    # the other eye: a slit in the scar, the lid puckered, one scar line across it
    H.eye(f, 5.9, 5.3, 2.3, 1.05, ("#3a2430", "#7a5a6a", "#d8b8c4"), lash=P["lash"], tilt=-0.2, lash_w=0.45, flick=0.3, lid_shadow=0.7, hi=0.4)
    f.fill(f.m_polyline([(4.8, 4.5), (5.9, 4.7), (7.0, 4.4)], 0.2), P["burn_d"], 0.75)
    f.fill(f.m_polyline([(5.2, 6.3), (6.2, 6.1), (7.2, 6.4)], 0.16), P["burn_d"], 0.6)
    f.fill(f.m_polyline([(5.5, 3.9), (6.4, 4.2)], 0.12), P["burn_l"], 0.7)
    H.nose(f, 4.0, 6.3, "#c49e92", 0.2)
    # a lopsided smirk: the character's left corner pulled up
    H.mouth_smile(f, 3.9, 7.35, 2.1, color="#8a5a58", lip="#c48e88", depth=0.1, thick=0.17)
    f.fill(f.m_line((4.9, 7.3), (5.35, 6.95), 0.16, 0.1), "#7a4a4a", 0.8)
    # stubble: a dusting of dark dots on the jaw and above the lip
    rng = np.random.RandomState(7)
    for _ in range(46):
        x, y = rng.uniform(1.0, 7.0), rng.uniform(6.9, 8.0)
        if 6.7 > (y - 6.9) * 9 + 5.2 and (x < 4.6 or y > 7.4):
            f.fill(f.m_ellipse(x, y, 0.07, 0.07), "#6a5648", 0.55)
    f.fill(f.m_band(7.75, 8.0), "#8a6c60", 0.25)
    # the second layer: only the hair and the scar's rim are kept crisp beyond 24 blocks; the cut is the 3D hair
    K.fringe_mass(fo, pal, rag, y_top=0.0, y_bot=3.1)


def head_sides(cv):
    pal = K.hair_pal(P["hair"], light_k=1.5, dark_k=0.6)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.8, 1.0, 5.6, lean, 0.95)


# ================================================================================================ the coat
def scarf_ring(f, name):
    """The purple scarf in two turns round the neck; the high steel-trimmed coat collar stands up behind it."""
    m = f.m_band(0.0, 1.9)
    K.cloth(f, m, P["coat"], top=1.3, bottom=0.8, round_=0.1)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.18), P["steel"], 0.9)
    # two scarf turns, the lower one wider
    for y0, y1, k in ((0.3, 1.05, 1.0), (1.0, 1.85, 0.9)):
        mm = f.m_rect(0.0, y0, f.w, y1)
        f.fill(mm, H.lin((0, y0), (0, y1), [(0, K.lit(P["scarf"], 1.35 * k)), (0.5, P["scarf"]), (1, H.tone(P["scarf"], 0.65))]))
        f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.1), P["scarf_l"], 0.7)
        f.fill(f.m_rect(0.0, y1 - 0.12, f.w, y1), P["scarf_d"], 0.8)
        H.folds(f, mm, [f.w * 0.25, f.w * 0.55, f.w * 0.82], depth=0.12, width=0.22, y0=y0, y1=y1)
    f.fill(f.m_rect(0.0, 1.78, f.w, 1.92), "#0c0a12", 0.55)
    H.shadow_under(f, 1.92, 0.55, 0.32)
    if name == "front":
        # the hanging end of the scarf over the character's right breast, with a frayed tip
        end = f.m_poly([(1.0, 1.6), (2.7, 1.7), (2.9, 5.6), (2.3, 5.2), (1.9, 5.9), (1.4, 5.3), (0.9, 5.7)])
        f.fill(f.m_polyline([(1.0, 1.6), (2.7, 1.7), (2.9, 5.6), (2.3, 5.2), (1.9, 5.9), (1.4, 5.3), (0.9, 5.7), (1.0, 1.6)], 0.3), P["scarf_d"], 0.8)
        f.fill(end, H.lin((0, 1.6), (0, 5.9), [(0, K.lit(P["scarf"], 1.3)), (1, H.tone(P["scarf"], 0.72))]))
        H.folds(f, end, [1.5, 2.1], depth=0.12, width=0.22, y0=1.8, y1=5.6)
        f.fill(f.m_rect(0.9, 5.1, 2.9, 5.2), P["scarf_l"], 0.6)


def coat_front(f):
    K.cloth(f, f.m_all(), P["coat"], top=1.35, bottom=0.8, round_=0.14, fold_xs=[0.9, 1.7, 6.3, 7.1], fold_depth=0.11, fold_y0=2.0, fold_y1=8.6)
    # the pale shirt in the opening, laced up
    op = f.m_poly([(2.5, 1.9), (5.5, 1.9), (5.4, 8.5), (2.6, 8.5)])
    f.fill(op, H.lin((0, 1.9), (0, 8.5), [(0, K.lit(P["shirt"], 1.1)), (1, H.tone(P["shirt"], 0.72))]))
    f.fill(f.m_rect(2.5, 1.9, 2.9, 8.5), P["shirt_d"], 0.5)
    for y in (2.6, 3.5, 4.4, 5.3, 6.2):
        f.fill(f.m_polyline([(3.3, y), (4.0, y + 0.45), (4.7, y)], 0.1), "#4a4034", 0.8)
        f.fill(f.m_polyline([(3.3, y + 0.45), (4.0, y), (4.7, y + 0.45)], 0.1), "#4a4034", 0.8)
    f.fill(f.m_line((4.0, 2.0), (4.0, 7.2), 0.1, 0.1), "#6a5e4e", 0.6)
    # steel piping along both lapels and the opening
    for pts in ([(2.5, 1.9), (2.6, 8.5)], [(5.5, 1.9), (5.4, 8.5)]):
        H.trim_line(f, pts, P["steel"], width=0.22, spec=0.5, outline=0.5)
    f.fill(f.m_line((0.0, 2.1), (2.5, 2.1), 0.1, 0.1), P["steel_d"], 0.6)
    K.seam(f, [(0.3, 2.5), (2.0, 2.2)], P["steel_d"], 0.05, 0.6)
    K.seam(f, [(7.7, 2.5), (6.0, 2.2)], P["steel_d"], 0.05, 0.6)
    # two steel buttons on each side of the opening
    for y in (3.2, 5.6):
        H.stud(f, 1.4, y, 0.2, P["steel_l"], spec=0.9)
        H.stud(f, 6.6, y, 0.2, P["steel_l"], spec=0.9)
    # the chain across the chest (a short bandolier, a lock plate at the shoulder), it glows a little
    K.chain_path(f, [(0.3, 2.6), (2.0, 3.3), (4.0, 4.7), (6.0, 6.2), (7.7, 7.4)], step=0.78, color=P["chain"], dark=P["chain_d"], glow=P["chain_glow"], rx=0.5, ry=0.3)
    f.rect(0.0, 2.2, 0.8, 3.1, P["steel"], r=0.12)
    H.stud(f, 0.4, 2.65, 0.14, P["steel_l"], spec=0.9)
    scarf_ring(f, "front")
    belts(f, True)


def coat_back(f):
    K.cloth(f, f.m_all(), P["coat"], top=1.3, bottom=0.8, round_=0.14, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.11, fold_y0=2.0, fold_y1=8.6)
    f.fill(f.m_line((4.0, 1.9), (4.0, 8.6), 0.2, 0.2), P["coat_d"], 0.6)
    K.seam(f, [(3.62, 2.0), (3.62, 8.5)], P["steel_d"], 0.05, 0.5)
    K.seam(f, [(0.3, 2.5), (2.9, 2.1)], P["steel_d"], 0.05, 0.5)
    K.seam(f, [(7.7, 2.5), (5.1, 2.1)], P["steel_d"], 0.05, 0.5)
    scarf_ring(f, "back")
    belts(f, False)


def coat_side(f, name):
    K.cloth(f, f.m_all(), P["coat"], top=1.3, bottom=0.8, round_=0.14, fold_xs=[1.3, 2.7], fold_depth=0.11, fold_y0=2.0, fold_y1=8.6)
    ed = f.w - 0.35 if name == "west" else 0.0
    f.fill(f.m_rect(ed, 1.9, ed + 0.35, 8.6), P["steel"], 0.9)
    K.seam(f, [(f.w * 0.5, 2.0), (f.w * 0.5, 8.5)], P["steel_d"], 0.05, 0.5)
    scarf_ring(f, name)
    belts(f, False)


def belts(f, front):
    """The double belt: two straps with stitching, a steel buckle in front and a pouch each side (the chain tail hangs at the back right, so the back keeps only the straps)."""
    K.belt_band(f, 8.55, 9.2, P["belt"], stitch=P["steel_d"])
    K.belt_band(f, 9.4, 10.05, P["belt"], stitch=P["steel_d"])
    if front:
        K.buckle(f, 4.0, 9.3, 1.9, 1.75, P["buckle"], hole=P["belt_d"])
        for x0 in (0.4, 5.6):                                                                       # two pouches hanging from the lower strap
            p = f.m_rect(x0, 10.0, x0 + 2.0, 11.9, 0.15)
            f.fill(p, H.lin((0, 10.0), (0, 11.9), [(0, K.lit(P["pouch"], 1.5)), (1, H.tone(P["pouch"], 0.7))]))
            f.fill(f.m_rect(x0, 10.0, x0 + 2.0, 10.12), K.lit(P["pouch"], 2.2), 0.6)
            f.fill(f.m_rect(x0 + 0.1, 10.0, x0 + 1.9, 10.9), H.tone(P["pouch"], 0.78), 0.55)
            f.fill(f.m_polyline([(x0 + 0.1, 10.9), (x0 + 1.0, 11.15), (x0 + 1.9, 10.9)], 0.12), "#120a06", 0.8)
            H.stud(f, x0 + 1.0, 11.1, 0.16, P["buckle"], spec=0.8)
            H.shadow_under(f, 11.9, 0.4, 0.3)
    else:
        H.stud(f, 4.0, 9.3, 0.2, P["buckle"], spec=0.7)


def hips(f, name):
    m = f.m_band(10.05, 12.0)
    f.fill(m, H.lin((0, 10.05), (0, 12.0), [(0, K.lit(P["coat"], 1.1)), (1, H.tone(P["coat"], 0.75))]))
    H.folds(f, m, [f.w * 0.3, f.w * 0.7], depth=0.12, width=0.25, y0=10.1, y1=12.0)
    # the torn hem: a steel line with jagged cuts
    f.fill(f.m_rect(0.0, 11.7, f.w, 11.82), P["steel_d"], 0.8)
    for x in (1.0, 2.6, 4.3, 6.0, 7.3):
        f.fill(f.m_poly([(x - 0.3, 11.75), (x + 0.3, 11.75), (x, 12.0)]), "#050408", 0.9)
    H.shadow_under(f, 10.08, 0.5, 0.3)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                coat_front(f)
            elif nm == "back":
                coat_back(f)
            else:
                coat_side(f, nm)
            hips(f, nm)


# ================================================================================================ arms and legs
def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.7)
            K.cloth(f, sleeve, P["coat"], top=1.35, bottom=0.8, round_=0.18, fold_xs=[0.7, 1.7], fold_depth=0.12, fold_y0=1.6, fold_y1=7.8)
            K.sheen(f, sleeve, [f.w * 0.3], 1.6, 7.6, 0.24, "#bcb8e0", width=0.26)
            # a steel shoulder plate with rivets and a piping, and a steel seam on the outer side
            sb = f.m_band(0.0, 1.45)
            f.fill(sb, H.lin((0, 0), (0, 1.45), [(0, K.lit(P["steel"], 1.25)), (1, H.tone(P["steel"], 0.75))]))
            H.bevel(f, sb, depth=0.2, gain=1.2, spec=0.4)
            f.fill(f.m_rect(0.0, 1.38, f.w, 1.5), "#0a0a10", 0.7)
            for x in (0.55, f.w - 0.55):
                H.stud(f, x, 0.7, 0.14, P["steel_l"], spec=0.9)
            if nm in ("west", "east"):
                f.fill(f.m_line((f.w * 0.5, 1.2), (f.w * 0.5, 7.2), 0.16, 0.16) * sleeve, P["steel_d"], 0.6)
            for y, k in ((4.8, 0.55), (5.3, 0.35)):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.28), (f.w, y + 0.05)], 0.15) * sleeve, P["coat_d"], k)
            f.fill(f.m_polyline([(0.0, 4.6), (f.w * 0.5, 4.85), (f.w, 4.65)], 0.2) * sleeve, K.lit(P["coat"], 1.7), 0.3)
            # a torn patch on the forearm
            if nm == "front":
                f.fill(f.m_poly([(f.w * 0.2, 5.7), (f.w * 0.75, 5.9), (f.w * 0.6, 6.5), (f.w * 0.15, 6.3)]), H.tone(P["coat"], 0.62), 0.75)
                f.fill(f.m_polyline([(f.w * 0.2, 5.7), (f.w * 0.75, 5.9), (f.w * 0.6, 6.5), (f.w * 0.15, 6.3), (f.w * 0.2, 5.7)], 0.1), P["steel_d"], 0.6)
            # the steel cuff with a piping and two studs
            K.cuff(f, 7.55, 8.7, P["steel"], pipe=P["steel_l"], stud_color=P["steel_l"], spec=0.45)
            # the fingerless glove: dark leather to the knuckles, bare fingertips with gaps
            K.glove(f, P["glove"], 8.7, 11.0, nm, finger_n=2, cuff_color=None, knuckle_k=1.4, thumb=False)
            f.fill(f.m_band(8.7, 11.0), K.lit(P["glove"], 1.0), 0.0)
            f.fill(f.m_band(11.0, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(P["skin"][1], 0.95)), (1, H.tone(P["skin"][0], 0.95))]))
            f.fill(f.m_band(10.92, 11.06), H.tone(P["glove"], 0.5), 0.9)
            K.fingers(f, P["skin"], 11.15, 12.0, n=2)
            H.shadow_under(f, 8.75, 0.5, 0.3)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", P["coat"], edge=P["steel_d"], k_top=1.3, overlay=ov)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, P["pants"], 0.0, 6.6, nm, knee=False, top=1.5, bottom=0.8, seam_color=P["steel_d"])
                H.shadow_under(f, 0.0, 0.6, 0.25)
                K.boot(f, P["boot"], 6.4, nm, strap=True, strap_color=P["belt"], gold=P["buckle"], cuff_color=P["steel"], cuff_h=1.0, welt=P["steel_d"], sole="#0a0808")
                if nm in ("front", "west", "east"):                                                  # a second strap lower on the shaft
                    f.fill(f.m_rect(0.0, 9.2, f.w, 9.75), H.tone(P["belt"], 1.0), 0.95)
                    f.fill(f.m_rect(0.0, 9.2, f.w, 9.28), K.lit(P["belt"], 1.8), 0.6)
                    f.fill(f.m_rect(0.0, 9.65, f.w, 9.75), "#0e0a08", 0.7)
                    if nm == "front":
                        H.stud(f, f.w * 0.5, 9.48, 0.2, P["buckle"], spec=0.8)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, P["hair"], P["hair_rgb"])
    K.tidy_head(cv, img64, spec, P["hair"], P["hair_rgb"], P["skin"])
    head_sides(cv)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    arm_tops(cv)
    legs(cv)
    return K.finish(cv, covers, hair, P["tint"], slim)
