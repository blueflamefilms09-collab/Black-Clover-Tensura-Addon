"""Diane, the Serpent's Sin of Envy, HD 256x256 (docs/arc/hd_arc_sds1.md): this module paints both looks, `diane` and `diane_form2` (Drole's Dance; `diane_form2.py` only loads it).

What she wears is the 64x64 sheet's outfit (tools/arc/char_diane.py): an orange sleeveless one-piece with a cream-peach neckline and trim, a brown belt with a small gold buckle, very short orange shorts
with a gold hem over high brown boots (knee height, a gold cuff), studded grey gauntlets with gold rings on both forearms, bare shoulders. The hair is the kit's classic twin tails (the skin only paints the bangs
and the side locks). Form 2: the suit goes sunrise orange with pale-gold trim, a glowing rune on the chest, and the crystal plates of the form (scaled gold pauldrons, bracers and greaves with glowing gems),
gold eyes, light lines and a golden hair.

Drawn at 4x detail on both layers: big shaded purple eyes with lashes and a cheerful open smile with blush, layered bangs and side locks; the suit with its seams, a zipper at the back and the belt with
stitching; the studded gauntlets (plates, rivets, rings); the shorts' hem and the boots with their straps, buckles, welt and sole.

Source (rule 10): no wiki page could be fetched here; the look is from memory and docs/arc/diane.md (which cites the wikis): a cheerful young giant with brown twin tails and purple eyes in an orange suit.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

SKIN = ("#e9b79f", "#f8d5c3", "#ffeee2")
BASE = dict(
    hair="#8a5434", hair_rgb=(138, 84, 52), suit="#e8782a", trim="#f8c070", belt="#6a4426", boot="#6a4426", gold="#f8c070", metal="#8a8a98",
    iris=("#3c1670", "#9a58d8", "#e6c8ff"), eye_glow=None, brow="#5a341c", form2=False, glow="#ffd070",
)
FORM2 = dict(
    hair="#c47a3a", hair_rgb=(196, 122, 58), suit="#f09a3a", trim="#ffe0a0", belt="#6b4526", boot="#6b4526", gold="#ffd070", metal="#e8a850",
    iris=("#a8700a", "#ffd070", "#fff4c0"), eye_glow="#fff0b0", brow="#8a521c", form2=True, glow="#ffd070",
)


# ================================================================================================ the head
def head_front(cv, P):
    f = cv.face("head", "front")
    K.clear_glow(f)
    H.skin_face(f, SKIN, hair=None, side=0.08, chin=0.07, shadow=0.0)
    pal = K.hair_palette(P["hair"])
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.7), (7.3, 2.0), (6.4, 2.45), (5.3, 1.7), (4.3, 2.3), (3.7, 1.55), (2.6, 2.3), (1.5, 1.95), (0.6, 2.8), (-0.3, 2.9)]
    mass = f.m_poly(outline)
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - 1.9) / 2.2, 0, 1) * (f.Y > 1.5), 0.87)
    K.fringe_mass(f, pal, outline, 0.0, 2.6)
    for x in (0.8, 2.0, 3.2, 4.6, 5.8, 7.0):
        f.fill(f.m_line((x, 0.0), (x + 0.12, 2.0), 0.10, 0.05) * mass, pal["h"], 0.35)
    # a centre-left parting: bangs sweep to both sides, two short wisps hang between the brows
    for sh, (cx, w, top, ln, lean) in ((0.8, (7.2, 1.7, 1.2, 3.2, -0.45)), (0.8, (0.9, 1.7, 1.2, 3.4, 0.45)), (0.95, (5.7, 1.5, 1.4, 1.7, 0.4)), (0.95, (2.5, 1.5, 1.4, 1.8, -0.3)),
                                       (1.0, (4.5, 1.0, 1.5, 1.2, 0.2)), (1.0, (3.5, 0.9, 1.5, 1.0, -0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.35, 0.9, 1.0, 5.2, 0.15, 1.0)                                       # the long side locks that frame the face
    K.lock(f, pal, 7.65, 0.9, 1.0, 5.2, -0.15, 1.0)
    H.brow(f, (3.45, 3.85), (1.35, 3.45), P["brow"], 0.24, 0.1, arch=0.14)
    H.brow(f, (4.55, 3.85), (6.65, 3.45), P["brow"], 0.24, 0.1, arch=0.14)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.15, 2.7, 2.55, P["iris"], lash="#2a1426", tilt=0.1, lash_w=0.42, flick=0.4, lid_shadow=0.3, glow=P["eye_glow"], iris_r=1.0)
    H.blush(f, 1.15, 6.5, 1.0, 0.55, "#ee7e86", 0.5)
    H.blush(f, 6.85, 6.5, 1.0, 0.55, "#ee7e86", 0.5)
    H.nose(f, 4.0, 6.3, "#d49680", 0.2)
    H.mouth_smile(f, 4.0, 6.85, 3.0, color="#7a2e34", lip="#e08a86", depth=0.34, thick=0.17, teeth="#fffdf6", open_h=0.6)
    if P["form2"]:
        for sgn in (1.0, -1.0):                                                          # light lines of the form: from the outer eye corner across the cheek, and a short pair on the brow
            x0 = 4.0 + sgn * 3.5
            K.glow_line(f, [(x0, 4.6), (x0 - sgn * 0.1, 5.8), (x0 - sgn * 0.45, 6.9)], "#fff0b0", 0.2, 0.85)
            K.glow_line(f, [(4.0 + sgn * 0.9, 2.9), (4.0 + sgn * 1.5, 3.25)], "#fff0b0", 0.16, 0.7)


# ================================================================================================ the suit
def suit_panel(f, P, name):
    s, t = P["suit"], P["trim"]
    H.cloth(f, f.m_all(), s, top=1.12, bottom=0.84, round_=0.1, fold_xs=[1.3, 6.7] if name in ("front", "back") else [1.2, 2.8], fold_depth=0.08, fold_y0=2.0, fold_y1=9.0, weave_k=0.0)
    if name in ("front", "back"):
        H.sheen(f, f.m_rect(0, 1.5, 8, 9), [1.1, 6.9], width=0.22, strength=0.2, tilt=0.04, y0=2.0, y1=8.6, color="#ffe8c0")
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.tone(s, 0.9), 0.6)


def belt(f, P, front):
    a, g = P["belt"], P["gold"]
    f.fill(f.m_rect(0.0, 9.0, f.w, 10.3), H.lin((0, 9.0), (0, 10.3), [(0, H.tone(a, 1.4)), (0.4, a), (1, H.tone(a, 0.68))]))
    f.fill(f.m_rect(0.0, 9.02, f.w, 9.2), "#b08a58", 0.5)
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.34), "#0e0a08", 0.8)
    K.seam(f, [(0.0, 9.3), (f.w, 9.3)], "#d4ac68", 0.055, 0.75, (0.3, 0.18), 0.2)
    K.seam(f, [(0.0, 10.0), (f.w, 10.0)], "#d4ac68", 0.055, 0.6, (0.3, 0.18), 0.2)
    H.shadow_under(f, 10.3, 0.5, 0.3)
    if front:
        f.rect(3.1, 8.85, 4.9, 10.45, "#4a3210", r=0.24)                                # a small gold buckle
        f.rect(3.18, 8.93, 4.82, 10.37, H.metal_paint(8.93, 10.37, g, 1.15), r=0.2)
        H.bevel(f, f.m_rect(3.18, 8.93, 4.82, 10.37, 0.2), depth=0.2, gain=1.3, spec=0.7)
        f.rect(3.55, 9.3, 4.45, 10.0, "#241808", r=0.1)
        f.ellipse(3.4, 9.15, 0.2, 0.12, "#fffbe0", a=0.8)


def body_front(f, ov, P):
    s, t, g = P["suit"], P["trim"], P["gold"]
    suit_panel(f, P, "front")
    # darker side panels (the princess seams of the suit) and a bust seam
    for x0, x1 in ((0.0, 1.55), (6.45, 8.0)):
        f.fill(f.m_rect(x0, 1.6, x1, 9.0), H.tone(s, 0.84), 0.5)
    K.seam(f, [(1.55, 2.3), (1.45, 8.7)], t, 0.065, 0.7)
    K.seam(f, [(6.45, 2.3), (6.55, 8.7)], t, 0.065, 0.7)
    f.fill(f.m_polyline([(2.0, 3.8), (4.0, 4.5), (6.0, 3.8)], 0.12), H.tone(s, 0.62), 0.45)       # the line under the bust
    f.fill(f.m_polyline([(2.0, 3.55), (4.0, 4.25), (6.0, 3.55)], 0.07), H.tone(s, 1.5), 0.3)
    # the neckline: a rounded scoop trimmed in cream-peach with a stitch
    neck = f.m_poly([(0.0, 0.0), (8.0, 0.0), (8.0, 0.35), (6.6, 1.0), (5.2, 1.5), (4.0, 1.65), (2.8, 1.5), (1.4, 1.0), (0.0, 0.35)])
    f.fill(neck, H.lin((0, 0), (0, 1.65), [(0, H.tone(t, 1.12)), (1, H.tone(t, 0.8))]))
    f.fill(f.m_polyline([(0.0, 0.4), (1.4, 1.05), (2.8, 1.55), (4.0, 1.7), (5.2, 1.55), (6.6, 1.05), (8.0, 0.4)], 0.13), H.tone(t, 0.6), 0.8)
    K.seam(f, [(0.2, 0.15), (1.5, 0.75), (2.9, 1.2), (4.0, 1.3), (5.1, 1.2), (6.5, 0.75), (7.8, 0.15)], H.tone(t, 0.55), 0.055, 0.6, (0.3, 0.2), 0.0)
    H.shadow_under(f, 1.7, 0.5, 0.22, mask=1.0 - neck)
    belt(f, P, True)
    f.fill(f.m_rect(0.0, 10.3, f.w, 12.0), H.tone(s, 0.9), 0.4)
    f.fill(f.m_line((4.0, 10.5), (4.0, 12.0), 0.1, 0.1), H.tone(s, 0.55), 0.6)
    if P["form2"]:
        rune(f, ov, P)


def rune(f, ov, P):
    """The glowing rune of Drole's Dance on the chest: a ringed sun with eight rays, a gem in its heart."""
    g = P["gold"]
    cx, cy = 4.0, 5.6
    ring = f.m_ring(cx, cy, 1.45, 0.2)
    f.fill(ring, g, 1.0)
    f.fill(f.m_ring(cx, cy, 1.9, 0.1), H.tone(g, 0.9), 0.9)
    for i in range(8):
        a = i * np.pi / 4
        f.fill(f.m_line((cx + np.cos(a) * 1.55, cy + np.sin(a) * 1.55), (cx + np.cos(a) * 2.35, cy + np.sin(a) * 2.35), 0.2, 0.06), g, 0.95)
    H.gem(f, cx, cy, 0.62, "#ffe9a0", setting=H.tone(g, 0.8), facets=6, glow=False)
    if ov:
        f.glow_fill(f.m_ring(cx, cy, 1.45, 0.45, soft=3.0), g, 0.8)
        f.glow_fill(f.m_ellipse(cx, cy, 0.8, 0.8, soft=3.0), "#fff4c0", 0.95)
        for i in range(8):
            a = i * np.pi / 4
            f.glow_fill(f.m_line((cx + np.cos(a) * 1.55, cy + np.sin(a) * 1.55), (cx + np.cos(a) * 2.35, cy + np.sin(a) * 2.35), 0.3, 0.12), g, 0.7)


def body_back(f, ov, P):
    s, t, g = P["suit"], P["trim"], P["gold"]
    suit_panel(f, P, "back")
    f.fill(f.m_rect(0.0, 1.6, 1.55, 9.0), H.tone(s, 0.84), 0.5)
    f.fill(f.m_rect(6.45, 1.6, 8.0, 9.0), H.tone(s, 0.84), 0.5)
    f.fill(f.m_polyline([(0.0, 0.4), (2.0, 1.0), (4.0, 1.2), (6.0, 1.0), (8.0, 0.4)], 0.3), H.tone(t, 0.95))                 # the back of the neckline
    K.seam(f, [(0.2, 0.9), (2.0, 1.4), (4.0, 1.6), (6.0, 1.4), (7.8, 0.9)], H.tone(t, 0.55), 0.055, 0.6, (0.3, 0.2), 0.0)
    f.fill(f.m_line((4.0, 1.2), (4.0, 9.0), 0.2, 0.2), H.tone(s, 0.55), 0.7)                                                  # the zipper: a dark channel, gold teeth, a pull tab
    for y in np.arange(1.5, 8.8, 0.5):
        f.fill(f.m_rect(3.82, float(y), 4.18, float(y) + 0.22), H.tone(g, 1.0 if int(y * 2) % 2 else 0.8), 0.9)
    f.poly([(3.75, 1.15), (4.25, 1.15), (4.25, 2.0), (4.0, 2.2), (3.75, 2.0)], H.tone(g, 1.15))
    belt(f, P, False)
    f.fill(f.m_rect(0.0, 10.3, f.w, 12.0), H.tone(s, 0.9), 0.4)


def body_side(f, ov, P, name):
    s, t = P["suit"], P["trim"]
    suit_panel(f, P, name)
    f.fill(f.m_poly([(0, 0), (f.w, 0), (f.w, 1.2), (0, 1.2)]), H.tone(t, 0.9), 0.0)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.45), H.tone(t, 0.92), 0.9)                          # the edge of the armhole
    K.seam(f, [(0.1, 0.8), (f.w - 0.1, 0.8)], H.tone(t, 0.6), 0.055, 0.55)
    K.seam(f, [(f.w * 0.5, 1.6), (f.w * 0.5, 8.8)], t, 0.06, 0.55)
    belt(f, P, False)
    f.fill(f.m_rect(0.0, 10.3, f.w, 12.0), H.tone(s, 0.9), 0.4)


def body(cv, P):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            if ov:
                K.clear_glow(f)
            if nm == "front":
                body_front(f, ov, P)
            elif nm == "back":
                body_back(f, ov, P)
            else:
                body_side(f, ov, P, nm)


# ================================================================================================ arms: bare shoulders, studded gauntlets (form 2: scaled pauldrons and bracers)
def gauntlet(f, P, ov):
    m, g = P["metal"], P["gold"]
    if P["form2"]:
        plate = f.m_band(6.0, 12.0)
        f.fill(plate, H.lin((0, 6.0), (0, 12.0), [(0, H.tone(m, 1.35)), (0.35, m), (1, H.tone(m, 0.68))]))
        H.scallops(f, plate, 6.4, rows=4, cell=1.5, depth=1.9, base=None, tip=None, edge=H.tone(m, 0.4), edge_a=0.6, shadow=0.2)
        H.bevel(f, plate, depth=0.3, gain=0.8, spec=0.35)
    else:
        plate = f.m_band(6.0, 12.0)
        f.fill(plate, H.lin((0, 6.0), (0, 12.0), [(0, H.tone(m, 1.28)), (0.5, m), (1, H.tone(m, 0.62))]))
        H.sheen(f, plate, [f.w * 0.3], width=0.22, strength=0.3, tilt=0.0, y0=6.6, y1=11.6, color="#e8ecf6")
        H.bevel(f, f.m_band(6.8, 10.0), depth=0.26, gain=0.8, spec=0.3)
        f.fill(f.m_line((0.1, 8.45), (f.w - 0.1, 8.45), 0.1, 0.1), H.tone(m, 0.5), 0.6)                  # the plate seam
    H.trim_band(f, 6.0, 6.8, g, spec=0.5, outline=0.55, glow=("#ffd070" if P["form2"] and ov else None))      # the gold ring at the elbow end
    H.trim_band(f, 9.8, 10.6, g, spec=0.5, outline=0.55, glow=("#ffd070" if P["form2"] and ov else None))     # and the one at the wrist
    H.shadow_under(f, 6.8, 0.5, 0.3)
    H.shadow_under(f, 10.6, 0.45, 0.28)
    for y in (7.8, 9.0, 11.2):                                                           # the rivets of the studs
        for k in range(int(round(f.w))):
            x = (k + 0.5) * f.w / int(round(f.w))
            H.stud(f, x, y, 0.2, "#e8d9a0", spec=0.8, glow=("#ffd070" if P["form2"] and ov and y == 8.4 else None))
    f.fill(f.m_band(11.75, 12.0), H.tone(m, 0.45), 0.8)


def pauldron(f, P, ov):
    """Form 2: three overlapping gold scale plates over the shoulder with a gem on the top edge (the second layer only has them at rows 0..3.6, the sheet's coverage)."""
    m, g = P["metal"], P["gold"]
    plate = f.m_band(0.0, 3.7)
    f.fill(plate, H.lin((0, 0), (0, 3.7), [(0, H.tone(m, 1.3)), (0.5, m), (1, H.tone(m, 0.7))]))
    H.scallops(f, plate, 0.4, rows=3, cell=1.5, depth=1.7, base=None, tip=None, edge=H.tone(m, 0.4), edge_a=0.65, shadow=0.24)
    H.bevel(f, plate, depth=0.28, gain=0.9, spec=0.4)
    H.trim_band(f, 0.0, 0.55, g, spec=0.5, outline=0.5)
    H.trim_band(f, 3.15, 3.75, g, spec=0.5, outline=0.55, glow=("#ffd070" if ov else None))
    if f.name in ("front", "back", "west", "east"):
        H.gem(f, f.w * 0.5, 1.7, 0.38, "#fff0b0", setting=H.tone(g, 0.8), facets=6, glow=ov)


def arms(cv, P):
    for part in ("arm_r", "arm_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                if ov:
                    K.clear_glow(f)
                K.limb_skin(f, SKIN, 0.0, 6.0, tendon=False)
                H.shadow_under(f, 0.0, 0.6, 0.0)
                f.fill(f.m_ellipse(f.w * 0.5, 0.7, f.w * 0.55, 0.8, soft=3.0), H.tone(SKIN[2], 1.1), 0.25)
                gauntlet(f, P, ov)
                if P["form2"]:
                    pauldron(f, P, ov)
                elif nm in ("front", "back", "west", "east"):
                    pass


# ================================================================================================ legs: short shorts with a gold hem, high brown boots (form 2: golden greaves)
def legs(cv, P):
    s, t, b, g = P["suit"], P["trim"], P["boot"], P["gold"]
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                H.cloth(f, f.m_band(0.0, 4.2), s, top=1.1, bottom=0.86, round_=0.12, fold_xs=[1.2, 2.8], fold_depth=0.08, fold_y0=0.3, fold_y1=3.9, weave_k=0.0)
                H.shadow_under(f, 0.0, 0.5, 0.0)
                H.trim_band(f, 4.0, 5.0, t if not P["form2"] else g, spec=0.4, outline=0.55)                  # the hem of the shorts, gold-peach
                K.seam(f, [(0.12, 4.5), (f.w - 0.12, 4.5)], H.tone(t, 0.6), 0.055, 0.7, (0.28, 0.18), 0.2)
                H.shadow_under(f, 5.0, 0.7, 0.3)
                if P["form2"]:
                    greave(f, P, nm, ov)
                else:
                    brown_boot(f, P, nm)


def brown_boot(f, P, name):
    b, g = P["boot"], P["gold"]
    f.fill(f.m_band(5.0, 11.25), H.lin((0, 5.0), (0, 11.25), [(0, H.tone(b, 1.28)), (0.4, b), (1, H.tone(b, 0.66))]))
    f.fill(f.m_band(5.0, 5.5), H.tone(b, 1.45), 0.5)
    H.sheen(f, f.m_band(5.3, 11.0), [f.w * 0.28], width=0.2, strength=0.28, tilt=0.03, y0=5.6, y1=10.6, color="#d8b090")
    for y in (6.9, 8.7):                                                                 # two buckled straps
        f.rect(0.0, y, f.w, y + 0.7, H.tone(b, 0.8))
        f.fill(f.m_rect(0.0, y, f.w, y + 0.16), H.tone(b, 1.6), 0.8)
        f.fill(f.m_rect(0.0, y + 0.62, f.w, y + 0.72), "#0e0a08", 0.6)
        if name == "front":
            H.stud(f, f.w * 0.5, y + 0.35, 0.3, g)
    f.fill(f.m_polyline([(0.0, 10.0), (f.w * 0.5, 10.25), (f.w, 10.05)], 0.1), H.tone(b, 0.45), 0.7)
    if name == "front":
        f.ellipse(f.w * 0.5, 10.5, 1.1, 0.55, H.tone(b, 1.6), a=0.38, soft=2.0)
        f.fill(f.m_line((f.w * 0.5, 5.6), (f.w * 0.5, 6.8), 0.1, 0.1), H.tone(b, 0.55), 0.6)
    f.fill(f.m_band(11.0, 11.35), "#8a5e3c", 0.95)
    f.fill(f.m_band(11.3, 12.0), H.lin((0, 11.3), (0, 12.0), [(0, "#1c1512"), (1, "#0d0b0a")]))
    f.fill(f.m_band(11.0, 11.12), "#b88c60", 0.6)


def greave(f, P, name, ov):
    m, g, b = P["metal"], P["gold"], P["boot"]
    f.fill(f.m_band(5.0, 6.0), H.lin((0, 5.0), (0, 6.0), [(0, H.tone(b, 1.1)), (1, H.tone(b, 0.7))]))
    plate = f.m_band(6.0, 11.25)
    f.fill(plate, H.lin((0, 6.0), (0, 11.25), [(0, H.tone(m, 1.35)), (0.4, m), (1, H.tone(m, 0.65))]))
    H.scallops(f, plate, 6.4, rows=3, cell=1.35, depth=2.0, base=None, tip=None, edge=H.tone(m, 0.4), edge_a=0.6, shadow=0.2)
    H.bevel(f, plate, depth=0.3, gain=0.8, spec=0.4)
    H.trim_band(f, 5.8, 6.5, g, spec=0.5, outline=0.55, glow=("#ffd070" if ov else None))
    if name == "front":
        H.gem(f, f.w * 0.5, 7.6, 0.4, "#fff0b0", setting=H.tone(g, 0.8), facets=6, glow=ov)
    f.fill(f.m_band(11.0, 11.35), H.tone(m, 0.6), 0.95)
    f.fill(f.m_band(11.3, 12.0), H.lin((0, 11.3), (0, 12.0), [(0, "#2a1c10"), (1, "#0d0b0a")]))


# ================================================================================================ paint
def paint(spec, cid, masks):
    P = FORM2 if cid.endswith("_form2") else BASE
    spec["hair"]["color"] = P["hair"]
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair="twin_tails")
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, P["hair_rgb"], skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    covers = K.all_covers(cv)
    head_front(cv, P)
    body(cv, P)
    arms(cv, P)
    legs(cv, P)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, P["hair_rgb"], slim
