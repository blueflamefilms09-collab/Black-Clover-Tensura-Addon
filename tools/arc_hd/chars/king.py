"""King, the Grizzly's Sin of Sloth, HD 256x256 (docs/arc/hd_arc_sds1.md): this module paints both looks, `king` and `king_form2` (the module `king_form2.py` only loads it).

What he wears is the 64x64 sheet's outfit (tools/arc/char_king.py): a thigh-length cream tunic with a green stand collar, gold piping, the Sacred Tree on the chest (it glows leaf green), a green sash with a gold
buckle, short cream sleeves with green cuffs, bare legs and small brown shoes, a gold circlet and a pink flower on the head, and the pale-green mantle on the shoulders (the cape itself and the wings are 3D
extras of the look and are not painted here: the back only carries the mantle's yoke). The form 2 ("King, the Fairy King Harlequin") is the awake one: a white-gold tunic with gold trim, gold glowing eyes, light lines.
Drawn at 4x detail on both layers: the half-lidded amber eyes with the bags under them, the parted brown fringe in long side locks, the tunic with its placket, piping and stitching, the tree emblem with leaves
and roots, the sash with its knot and buckle, the cuffs, the skin of the legs and the shoes.

Source (rule 10): no wiki page could be fetched in this environment; the look is from memory plus docs/arc/king.md (which cites the wikis): a boyish fairy, brown hair, amber eyes, a sleepy face. The colours
of the clothes are the sheet's (the owner reference 156 shows navy, teal and gold instead; that is a decision for the owner, see the doc).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

SKIN = ("#e6b696", "#f5d2bb", "#ffe9d9")
BASE = dict(
    hair="#c4702e", hair_rgb=(196, 112, 46), tunic="#eae2aa", trim="#4fa04a", trim_d="#244226", gold="#e8d070", sash="#4fa04a", glow="#b8ff80",
    mantle="#9ad07a", lining="#f4f0c0", iris=("#6e3408", "#dc8a28", "#ffd98a"), eye_glow=None, brow="#7a3a14", form2=False,
)
FORM2 = dict(
    hair="#f0a040", hair_rgb=(240, 160, 64), tunic="#fff4c0", trim="#e8d070", trim_d="#a8842a", gold="#f6dc7c", sash="#4fa04a", glow="#d8ffa0",
    mantle="#d8f4a8", lining="#ffffff", iris=("#a86a08", "#ffcf5a", "#fff4b0"), eye_glow="#fff0a0", brow="#a8601a", form2=True,
)


# ================================================================================================ the head
def head_front(cv, P):
    f = cv.face("head", "front")
    K.clear_glow(f)
    H.skin_face(f, SKIN, hair=None, side=0.09, chin=0.07, shadow=0.0)
    pal = K.hair_palette(P["hair"])
    # the fringe: a mop over the brow, parted at x 3.3; two long bangs sweep to the sides and frame the face (owner reference 156)
    mass = f.m_poly([(-0.3, -0.3), (8.3, -0.3), (8.3, 2.0), (7.4, 1.6), (6.3, 2.1), (5.1, 1.5), (4.0, 2.0), (3.3, 1.35), (2.5, 2.0), (1.3, 1.7), (-0.3, 2.1)])
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - 1.8) / 2.2, 0, 1) * (f.Y > 1.5), 0.86)
    K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.0), (7.4, 1.6), (6.3, 2.1), (5.1, 1.5), (4.0, 2.0), (3.3, 1.35), (2.5, 2.0), (1.3, 1.7), (-0.3, 2.1)])
    for x in (0.9, 2.2, 3.7, 5.2, 6.6):
        f.fill(f.m_line((x, 0.0), (x + 0.15, 1.7), 0.10, 0.05) * mass, pal["h"], 0.35)
    for sh, (cx, w, top, ln, lean) in ((0.8, (6.8, 1.6, 1.0, 3.0, -0.5)), (0.8, (1.2, 1.6, 1.0, 3.1, 0.5)), (0.95, (5.2, 1.3, 1.3, 1.5, 0.35)), (0.95, (2.8, 1.3, 1.3, 1.5, -0.3)),
                                       (1.0, (6.0, 1.3, 1.2, 2.2, -0.35)), (1.0, (2.0, 1.3, 1.2, 2.3, 0.3)), (1.0, (3.9, 1.1, 1.3, 1.0, 0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.38, 1.0, 0.9, 4.4, 0.2, 1.0)                                         # the sideburn locks
    K.lock(f, pal, 7.62, 1.0, 0.9, 4.4, -0.2, 1.0)
    # the sleepy look: light brows set a little low, heavy half-open amber eyes, the bags under them
    H.brow(f, (3.5, 4.0), (1.2, 3.85), P["brow"], 0.36, 0.14, arch=0.04)
    H.brow(f, (4.5, 4.0), (6.8, 3.85), P["brow"], 0.36, 0.14, arch=0.04)
    tilt = 0.1 if P["form2"] else -0.05
    h = 2.05 if P["form2"] else 1.65
    for cx in (2.35, 5.65):
        H.eye(f, cx, 5.2, 2.5, h, P["iris"], lash="#3a1c0a", tilt=tilt, lash_w=0.46, flick=0.3, lid_shadow=0.42, glow=P["eye_glow"], iris_r=1.0)
        if not P["form2"]:
            f.fill(f.m_polyline([(cx - 1.05, 6.2), (cx, 6.42), (cx + 1.05, 6.2)], 0.13), H.tone(SKIN[0], 0.78), 0.55)   # the bags
    H.nose(f, 4.0, 6.15, "#cf9680", 0.22)
    if P["form2"]:
        H.mouth_smile(f, 4.0, 7.0, 1.6, color="#8a4640", lip="#e0958a", depth=-0.02, thick=0.16)
        for sgn in (1.0, -1.0):                                                          # the light lines of the awake form: from the outer eye corner down the cheek, and a short one on the brow
            x0 = 4.0 + sgn * 3.55
            K.glow_line(f, [(x0, 5.0), (x0 - sgn * 0.15, 6.2), (x0 - sgn * 0.5, 7.2)], "#fff0a0", 0.2, 0.85)
            K.glow_line(f, [(4.0 + sgn * 1.0, 2.8), (4.0 + sgn * 1.55, 3.2)], "#fff0a0", 0.16, 0.7)
    else:
        H.mouth_smile(f, 4.0, 7.05, 1.4, color="#9a5a50", lip="#d99890", depth=0.05, thick=0.15)


def head_overlay(cv, P):
    """The circlet (row 1 of the second layer, all four sides) and the flower (top right of the front). The 64x64 sheet has exactly these texels, so the redraw stays inside them."""
    for nm in SIDES:
        f = cv.face("head", nm, True)
        f.clear()
        band = f.m_rect(0.0, 1.0, f.w, 2.0)
        f.fill(band, H.lin((0, 1.0), (0, 2.0), [(0, H.tone(P["gold"], 1.5)), (0.3, H.tone(P["gold"], 1.15)), (0.7, P["gold"]), (1, H.tone(P["gold"], 0.62))]))
        f.fill(f.m_rect(0.0, 1.0, f.w, 1.2), "#fffbe0", 0.6)
        for x in np.arange(0.5, f.w, 1.6):                                                # a row of tiny beads along the circlet
            f.ellipse(float(x), 1.55, 0.2, 0.2, H.tone(P["gold"], 1.35), a=0.8)
        if nm == "front":
            for x in (2.2, 6.0):                                                         # the little leaf-shaped gems of the circlet
                f.poly([(x, 1.0), (x + 0.5, 1.5), (x, 2.0), (x - 0.5, 1.5)], P["trim"], 0.95)
                f.poly([(x, 1.1), (x + 0.2, 1.4), (x, 1.5)], "#d8ffb0", 0.7)
            f.ellipse(7.0, 0.5, 0.9, 0.5, "#ff9ab8")                                      # the flower: pink petals around a yellow heart
            f.ellipse(6.55, 0.4, 0.45, 0.4, "#ffb8d0")
            f.ellipse(7.45, 0.4, 0.45, 0.4, "#ffb8d0")
            f.ellipse(7.05, 0.5, 0.35, 0.3, "#ffe678")


# ================================================================================================ the tunic
def tree(f, cx, cy, s, P, glow=True):
    """The Sacred Tree: a canopy of three leaf masses with light on their upper left, a trunk that flares into roots; the canopy glows leaf green."""
    leaf, leaf_d, leaf_l = "#4fa04a", "#2c6a2e", "#9cdc7a"
    if P["form2"]:
        leaf, leaf_d, leaf_l = "#5db657", "#2f7a35", "#c4f4a0"
    f.poly([(cx - 0.28 * s, cy + 0.1 * s), (cx + 0.28 * s, cy + 0.1 * s), (cx + 0.5 * s, cy + 0.95 * s), (cx + 0.15 * s, cy + 0.8 * s), (cx, cy + 1.0 * s), (cx - 0.15 * s, cy + 0.8 * s), (cx - 0.5 * s, cy + 0.95 * s)],
           H.lin((cx, cy), (cx, cy + s), [(0, "#8a5a2c"), (1, "#4a2c14")]))
    can = None
    for (ox, oy, rx, ry) in ((0.0, -0.65, 0.95, 0.72), (-0.62, -0.1, 0.72, 0.58), (0.62, -0.1, 0.72, 0.58), (0.0, -0.15, 0.62, 0.5)):
        m = f.m_ellipse(cx + ox * s, cy + oy * s, rx * s, ry * s)
        f.fill(m, H.rad((cx + ox * s - rx * s * 0.35, cy + oy * s - ry * s * 0.4), max(rx, ry) * s * 1.5, [(0, leaf_l), (0.5, leaf), (1, leaf_d)]))
        f.fill(f.m_ring(cx + ox * s, cy + oy * s, rx * s * 0.92, 0.07) * m, leaf_d, 0.55)
        can = m if can is None else np.maximum(can, m)
    for i in range(7):                                                                   # leaf glints
        a = H.hsh(i, 3, 1) * 6.28
        r = 0.55 * s * H.hsh(i, 4, 2)
        f.ellipse(cx + np.cos(a) * r * 1.2, cy - 0.35 * s + np.sin(a) * r * 0.75, 0.12 * s, 0.07 * s, "#e6ffc8", a=0.55, rot=30)
    if glow:
        f.glow_fill(can, P["glow"], 0.65)
        f.glow_fill(f.m_ellipse(cx, cy - 0.35 * s, 0.75 * s, 0.55 * s, soft=3.0), "#ffffff", 0.35)


def tunic_front(f, ov, P):
    t, g = P["tunic"], P["gold"]
    H.cloth(f, f.m_all(), t, top=1.12, bottom=0.88, round_=0.08, fold_xs=[1.2, 6.8], fold_depth=0.08, fold_y0=2.0, fold_y1=10.0, weave_k=0.0)
    H.sheen(f, f.m_rect(0, 1.5, 8, 9), [1.0, 7.0], width=0.2, strength=0.22, tilt=0.04, y0=2.0, y1=8.8, color="#ffffff")
    f.fill(f.m_line((4.0, 6.7), (4.0, 9.0), 0.17, 0.17), H.tone(t, 0.62), 0.55)       # the placket under the emblem
    f.fill(f.m_line((4.17, 6.7), (4.17, 9.0), 0.12, 0.12), H.tone(t, 1.25), 0.5)
    for x0, x1, s_ in ((0.0, 1.0, 1.0), (8.0, 7.0, -1.0)):                              # the green borders of the tabard down both front edges, a gold piping and stitch beside each
        bm = f.m_rect(min(x0, x1), 1.45, max(x0, x1), 11.0)
        f.fill(bm, H.lin((0, 1.45), (0, 11.0), [(0, H.tone(P["trim"], 1.15)), (1, H.tone(P["trim"], 0.78))]))
        f.fill(f.m_line((x1, 1.5), (x1, 11.0), 0.16, 0.16), H.tone(g, 0.85), 0.95)
        f.fill(f.m_line((x1 - s_ * 0.16, 1.5), (x1 - s_ * 0.16, 11.0), 0.07, 0.07), "#fffbe0", 0.45)
        K.seam(f, [(x1 + s_ * 0.45, 2.0), (x1 + s_ * 0.45, 8.6)], g, 0.055, 0.6)
    tree(f, 4.0, 5.55, 2.7, P, glow=ov)
    # the stand collar: a green band with gold piping and a V of cream cloth at the throat
    H.trim_band(f, 0.0, 1.45, P["trim"], spec=0.35, outline=0.5)
    f.fill(f.m_poly([(3.1, 0.0), (4.9, 0.0), (4.0, 1.25)]), H.tone(t, 0.9))
    f.fill(f.m_polyline([(3.1, 0.0), (4.0, 1.25), (4.9, 0.0)], 0.1), H.tone(P["trim"], 0.5), 0.9)
    K.seam(f, [(0.2, 1.6), (3.0, 1.6)], g, 0.065, 0.85)
    K.seam(f, [(8.0 - 0.2, 1.6), (5.0, 1.6)], g, 0.065, 0.85)
    H.shadow_under(f, 1.45, 0.45, 0.24)
    # the sash with its knot and buckle at the hip, two gold ends hanging
    H.trim_band(f, 9.0, 10.55, P["sash"], spec=0.4, outline=0.55)
    K.seam(f, [(0.15, 9.35), (7.85, 9.35)], g, 0.055, 0.75, (0.3, 0.18), 0.2)
    K.seam(f, [(0.15, 10.2), (7.85, 10.2)], g, 0.055, 0.6, (0.3, 0.18), 0.2)
    f.rect(2.9, 8.75, 5.1, 10.8, "#4a3a12", r=0.28)                                       # the buckle: a gold frame, a dark opening, a prong
    f.rect(2.98, 8.83, 5.02, 10.72, H.metal_paint(8.83, 10.72, g, 1.15), r=0.24)
    H.bevel(f, f.m_rect(2.98, 8.83, 5.02, 10.72, 0.24), depth=0.22, gain=1.3, spec=0.7)
    f.rect(3.4, 9.3, 4.6, 10.2, "#2a2008", r=0.1)
    f.line((3.2, 9.75), (4.1, 9.75), H.metal_paint(9.5, 10.0, g, 1.1), 0.18, 0.14)
    f.ellipse(3.3, 9.15, 0.22, 0.12, "#fffbe0", a=0.8)
    H.shadow_under(f, 10.55, 0.5, 0.3)
    # the tunic skirt: a gold hem line and the shadow of the sash
    f.fill(f.m_rect(0.0, 11.35, 8.0, 12.0), H.tone(t, 0.86), 0.7)
    return f


def tunic_back(f, ov, P):
    t, g = P["tunic"], P["gold"]
    H.cloth(f, f.m_all(), t, top=1.1, bottom=0.86, round_=0.08, fold_xs=[1.6, 3.0, 5.0, 6.4], fold_depth=0.07, fold_y0=4.0, fold_y1=10.0, weave_k=0.0)
    f.fill(f.m_line((4.0, 4.8), (4.0, 9.0), 0.16, 0.16), H.tone(t, 0.6), 0.5)
    # the mantle's yoke over the shoulders: pale green, gold edge, the lining peeking at the lower edge, the tree on it
    yoke = f.m_poly([(0, 0), (8, 0), (8, 3.2), (6.0, 4.5), (4.0, 4.9), (2.0, 4.5), (0, 3.2)])
    f.fill(yoke, H.lin((0, 0), (0, 4.9), [(0, H.tone(P["mantle"], 1.12)), (0.6, P["mantle"]), (1, H.tone(P["mantle"], 0.78))]))
    f.fill(f.m_polyline([(0, 3.4), (2.0, 4.7), (4.0, 5.1), (6.0, 4.7), (8, 3.4)], 0.22) * yoke, P["lining"], 0.9)
    f.fill(f.m_polyline([(0, 3.2), (2.0, 4.5), (4.0, 4.9), (6.0, 4.5), (8, 3.2)], 0.14), H.tone(g, 0.8), 0.95)
    H.bevel(f, f.m_polyline([(0, 3.2), (2.0, 4.5), (4.0, 4.9), (6.0, 4.5), (8, 3.2)], 0.16), depth=0.1, gain=1.0, spec=0.4)
    H.shadow_under(f, 4.5, 0.6, 0.22)
    tree(f, 4.0, 1.4, 2.4, P, glow=False)
    K.seam(f, [(0.2, 3.0), (0.2, 0.2)], g, 0.06, 0.7)
    K.seam(f, [(7.8, 3.0), (7.8, 0.2)], g, 0.06, 0.7)
    # the sash at the back and the hem
    H.trim_band(f, 9.0, 10.55, P["sash"], spec=0.4, outline=0.55)
    K.seam(f, [(0.15, 9.35), (7.85, 9.35)], g, 0.055, 0.75, (0.3, 0.18), 0.2)
    K.seam(f, [(0.15, 10.2), (7.85, 10.2)], g, 0.055, 0.6, (0.3, 0.18), 0.2)
    H.shadow_under(f, 10.55, 0.5, 0.3)
    f.fill(f.m_rect(0.0, 11.35, 8.0, 12.0), H.tone(t, 0.86), 0.7)


def tunic_side(f, ov, P, name):
    t, g = P["tunic"], P["gold"]
    H.cloth(f, f.m_all(), H.tone(t, 0.94), top=1.1, bottom=0.86, round_=0.1, fold_xs=[1.2, 2.6], fold_depth=0.07, fold_y0=3.0, fold_y1=10.0, weave_k=0.0)
    yoke = f.m_poly([(0, 0), (f.w, 0), (f.w, 3.4), (0, 3.4)])
    f.fill(f.m_rect(0, 0, f.w, 3.3), H.lin((0, 0), (0, 3.3), [(0, H.tone(P["mantle"], 1.1)), (1, H.tone(P["mantle"], 0.78))]), 0.0)
    H.trim_band(f, 9.0, 10.55, P["sash"], spec=0.4, outline=0.55)
    K.seam(f, [(0.1, 9.35), (f.w - 0.1, 9.35)], g, 0.055, 0.75, (0.3, 0.18), 0.2)
    H.shadow_under(f, 10.55, 0.5, 0.3)
    f.fill(f.m_rect(0.0, 11.35, f.w, 12.0), H.tone(t, 0.86), 0.7)
    edge = 0.45 if name == "east" else f.w - 0.45
    K.seam(f, [(edge, 2.2), (edge, 8.8)], g, 0.055, 0.5)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            if ov:
                K.clear_glow(f)
            if nm == "front":
                tunic_front(f, ov, P)
            elif nm == "back":
                tunic_back(f, ov, P)
            else:
                tunic_side(f, ov, P, nm)


# ================================================================================================ the arms and legs
def arm(cv, part, P):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            K.limb_skin(f, SKIN, 3.6, 12.0, tendon=nm in ("front", "east", "west"))
            K.fingers(f, SKIN, n=2)
            t, g = P["tunic"], P["gold"]
            H.cloth(f, f.m_band(0.0, 3.2), t, top=1.12, bottom=0.88, round_=0.12, fold_xs=[1.0, 2.2], fold_depth=0.07, fold_y0=0.9, fold_y1=2.9, weave_k=0.0)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), H.tone(t, 0.76), 0.7)                   # the shoulder seam
            K.seam(f, [(0.0, 0.7), (f.w, 0.7)], "#a8a070", 0.055, 0.6, (0.3, 0.2), 0.15)
            H.shadow_under(f, 3.0, 0.5, 0.3)
            H.trim_band(f, 3.0, 4.0, P["trim"], spec=0.35, outline=0.55)                   # the cuff, gold piping on it
            K.seam(f, [(0.1, 3.5), (f.w - 0.1, 3.5)], g, 0.055, 0.8, (0.3, 0.18), 0.2)
            H.shadow_under(f, 4.0, 0.5, 0.18)


def legs(cv, P):
    t, g = P["tunic"], P["gold"]
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                K.limb_skin(f, SKIN, 4.8, 10.2, tendon=False)
                for y, k in ((7.0, 0.5),):                                               # the knee
                    f.fill(f.m_polyline([(0.3, y), (f.w * 0.5, y + 0.25), (f.w - 0.3, y)], 0.12) * f.m_band(5.0, 10.0), H.tone(SKIN[0], 0.78), k)
                # the tunic skirt over the thigh: cream, a gold hem and a dark green pinstripe
                H.cloth(f, f.m_band(0.0, 3.1), t, top=1.12, bottom=0.88, round_=0.1, fold_xs=[1.3, 2.7], fold_depth=0.07, fold_y0=0.2, fold_y1=2.9, weave_k=0.0)
                H.trim_band(f, 3.0, 4.0, g, spec=0.35, outline=0.55)
                f.fill(f.m_band(4.0, 5.0), H.lin((0, 4.0), (0, 5.0), [(0, P["trim_d"]), (1, H.tone(P["trim_d"], 0.8))]))
                H.shadow_under(f, 5.0, 0.7, 0.3)
                f.fill(f.m_band(4.05, 4.25), H.tone(P["trim_d"], 1.9), 0.5)
                # the small shoes: brown leather, a gold clasp, a dark sole
                f.fill(f.m_band(9.55, 10.2), H.lin((0, 9.55), (0, 10.2), [(0, H.tone(g, 0.9)), (1, H.tone(g, 0.62))]))      # the cuff of the shoe
                f.fill(f.m_band(10.0, 11.25), H.lin((0, 10.0), (0, 11.25), [(0, "#8a6238"), (0.45, "#6b4a2a"), (1, "#45301c")]))
                H.sheen(f, f.m_band(10.0, 11.25), [f.w * 0.3], width=0.2, strength=0.26, tilt=0.0, y0=10.1, y1=11.1, color="#d8b890")
                if nm == "front":
                    f.ellipse(f.w * 0.5, 10.6, 0.65, 0.3, g, a=0.9)
                    f.ellipse(f.w * 0.5 - 0.15, 10.5, 0.2, 0.1, "#fffbe0", a=0.8)
                f.fill(f.m_band(11.2, 12.0), H.lin((0, 11.2), (0, 12.0), [(0, "#1c1512"), (1, "#0d0b0a")]))
                f.fill(f.m_band(11.2, 11.32), "#8a6a4a", 0.7)


# ================================================================================================ paint
def paint(spec, cid, masks):
    P = FORM2 if cid.endswith("_form2") else BASE
    spec["hair"]["color"] = P["hair"]
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair="anime_short")
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, P["hair_rgb"], skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    covers = K.all_covers(cv)
    head_cover = {("head", nm): K.cover_of(cv, "head", nm) for nm in SIDES}
    head_front(cv, P)
    head_overlay(cv, P)
    body(cv, P)
    arm(cv, "arm_r", P)
    arm(cv, "arm_l", P)
    legs(cv, P)
    K.hold_cover(cv, covers)
    K.hold_cover(cv, head_cover)
    skin, glow = cv.finish()
    return skin, glow, hair, P["hair_rgb"], slim
