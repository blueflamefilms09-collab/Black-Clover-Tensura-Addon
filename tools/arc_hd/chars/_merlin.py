"""Merlin, the Boar's Sin of Gluttony, HD 256x256: the painter of both forms (chars/merlin.py and chars/merlin_form2.py are thin wrappers).

Outfit of the 64x64 sheet (tools/arc/char_merlin.py, docs/arc/merlin.md): a short violet dress with a big white fur collar and a gold-trimmed hem, detached bell sleeves with gold cuffs over long black
gloves, a gold sash with a violet inner band, a gold choker with a violet gem, black thigh-high boots; gold earrings; a cloak with a crest hangs behind her and the sash of her belt hangs in front (both are
separate models, pmx_merlin: the cloak covers the whole back of the body, so the back of the dress is kept plain, and the sash hangs over the left hip, so that part of the front is kept simple).
The glow is the gem of the choker, the star on her chest, the eyes and the trim. The second form (Infinity unbound) is the same dress in a lighter violet with pale-gold trim, a violet-white streak in the
hair, light lines on the sleeves and a ring around the star (the gear halo above her head is a separate model).

Drawn at 4x detail on BOTH layers: the sharp golden eyes with violet eyeshadow, wing liner and heavy lashes, thin arched brows, red lips; the fur collar of scalloped tufts; the choker; the bodice with its
gold piping and the star; the gold sash; the bell sleeves with their gold cuffs; the long black gloves; the skirt with its gold hem; the thigh-high boots with straps and a gold top. Original art from memory
of the character's look (docs/arc/merlin.md lists the sources; the English and Japanese wikis could not be fetched in this session), nothing traced.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds2 as S  # noqa: E402

A, H = S.A, S.H

SKIN = ("#e0bcb9", "#f6dad2", "#ffeee6")
F1 = {
    "hair": "#17131f", "hair_rgb": (23, 19, 31), "hair_pal": S.hair_pal("#17131f", dark="#07050c", light="#2e2745", high="#6a56c8"),
    "style": "bob",
    "dress": "#4a3ca8", "dress_l": "#6c5ed4", "dress_d": "#2a2260", "sleeve": "#33288a", "sleeve_l": "#4c40b0", "gold": "#d8b24a", "gold_l": "#fff0b0", "violet": "#7a5cff",
    "fur": "#f0eaf8", "fur_d": "#b8aedc", "boot": "#241a38", "glove": "#17131f", "cape": "#2a1a5a",
    "iris": ("#7a5a08", "#e8c040", "#fff4b0"), "eye_glow": "#ffd86b", "shadow": "#8a6ad8", "lip": "#a8203c", "glow": "#ffd86b", "form2": False,
}
F2 = dict(F1)
F2.update({
    "hair_pal": S.hair_pal("#17131f", dark="#07050c", light="#2e2745", high="#cdbcff"),
    "dress": "#6a54d8", "dress_l": "#8a7af0", "dress_d": "#3c2e9a", "sleeve": "#4a3cb8", "sleeve_l": "#6a5cd8", "gold": "#e6c868", "gold_l": "#fffbe0", "violet": "#cdbcff",
    "cape": "#3a2a80", "iris": ("#8a6a10", "#fff0b0", "#ffffff"), "eye_glow": "#fff6c8", "shadow": "#a898f0", "glow": "#ffe9a0", "form2": True,
})


# ================================================================================================ the head
def head_front(cv, P):
    f = cv.face("head", "front")
    pal = P["hair_pal"]
    streak = S.hair_pal("#4a38b0", dark="#2a1e78", light="#7a66e0", high=P["violet"])
    H.skin_face(f, SKIN, hair=None, side=0.08, chin=0.06, shadow=0.0)
    # eyeshadow first (the eye is drawn over it): a thin violet crescent above the lid that sweeps out to the temple
    for cx, sgn in ((2.45, -1.0), (5.55, 1.0)):
        sh = f.m_ellipse(cx + sgn * 0.2, 4.5, 1.55, 0.5, rot=sgn * -16)
        f.fill(sh, P["shadow"], 0.4)
        f.fill(sh * np.clip((f.Y - 4.0) / 0.45, 0, 1), H.tone(P["shadow"], 1.3), 0.2)
    # the fringe: a long straight cut parted right of the middle, a little lower on the left
    mass = S.hair_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (6.2, 2.5), (4.0, 2.75), (1.8, 3.0), (-0.3, 3.2)])
    S.strands(f, pal, (0.8, 1.9, 3.1, 4.3, 5.6, 6.9), 0.0, 2.8, mass, 0.3)
    S.lock(f, 1.7, 1.9, 0.1, 3.0, 0.4, pal, 0.9)
    S.lock(f, 3.6, 1.8, 0.2, 2.7, 0.3, pal, 1.0)
    S.lock(f, 5.4, 1.7, 0.2, 2.5, 0.1, pal, 0.95)
    S.lock(f, 7.0, 1.4, 0.2, 2.3, -0.2, pal, 1.0)
    S.lock(f, 2.55, 0.55, 0.0, 2.9, 0.15, streak, 1.0)                                     # the violet streak inside the fringe
    S.lock(f, 0.5, 1.1, 1.0, 5.6, 0.1, pal, 1.0)                                           # the long locks that frame the face
    S.lock(f, 7.5, 1.1, 1.0, 5.6, -0.1, pal, 1.0)
    S.lock(f, 0.62, 0.45, 1.6, 4.4, 0.05, streak, 1.0)
    # the brows: thin, arched and sharp
    H.brow(f, (3.5, 3.8), (1.0, 3.3), "#241a30", 0.26, 0.10, arch=0.14)
    H.brow(f, (4.5, 3.8), (7.0, 3.3), "#241a30", 0.26, 0.10, arch=0.14)
    # the eyes: golden, half lidded, the outer corner raised with a liner wing
    for cx in (2.3, 5.7):
        H.eye(f, cx, 5.2, 2.5, 1.75, P["iris"], lash="#120c1a", tilt=0.32, lash_w=0.5, flick=0.58, iris_r=0.98, lid_shadow=0.34)
        f.glow_fill(f.m_ellipse(cx, 5.3, 0.4, 0.5, soft=2.0), P["eye_glow"], 0.5)
    H.blush(f, 1.35, 6.4, 0.9, 0.42, "#f08a9a", 0.2)
    H.blush(f, 6.65, 6.4, 0.9, 0.42, "#f08a9a", 0.2)
    H.nose(f, 4.0, 6.15, "#cf9a96", 0.22)
    # red lips: small, a faint smile, a lit lower lip
    pts = [(4.0 + u * 0.85, 6.95 + 0.1 * (1.0 - u * u)) for u in (-1.0, -0.8, -0.6, -0.4, -0.2, 0.0, 0.2, 0.4, 0.6, 0.8, 1.0)]
    f.fill(f.m_poly(pts + [(4.0 + u * 0.8, 7.12 + 0.34 * (1.0 - u * u)) for u in (1.0, 0.6, 0.2, -0.2, -0.6, -1.0)]), "#c63c58")
    f.fill(f.m_poly(pts + [(4.0 + u * 0.85, 7.05) for u in (1.0, 0.0, -1.0)]), "#98223e", 0.95)
    f.fill(f.m_polyline(pts, 0.14), "#5a0e22", 0.95)
    f.fill(f.m_ellipse(3.8, 7.22, 0.38, 0.09), "#ffb0bc", 0.75)
    if P["form2"]:
        light_line(f, [(2.4, 6.55), (1.5, 6.9), (0.7, 6.85)], P)
        light_line(f, [(5.6, 6.55), (6.5, 6.9), (7.3, 6.85)], P)


def light_line(f, pts, P, a=0.6):
    f.fill(f.m_polyline(pts, 0.12), "#fffbe8", a, glow=P["glow"])


def head_sides(cv, P):
    for nm in ("west", "east"):
        for ov in (False, True):
            S.ear(cv.face("head", nm, ov), nm, SKIN, 4.7, earring=P["gold"], jewel=(P["violet"], P["glow"]))


# ================================================================================================ the body
def fur_collar(f, P, back=False):
    """A big white fur collar of scalloped tufts across the shoulders, open on a V at the front where the choker sits."""
    fur, fd = P["fur"], P["fur_d"]
    depth = 2.35 if not back else 2.2
    m = f.m_rect(0.0, 0.0, f.w, depth)
    f.fill(m, H.lin((0, 0), (0, depth), [(0, H.tone(fur, 1.04)), (0.55, fur), (1, H.tone(fur, 0.82))]))
    f.fill(f.m_band(0.0, 0.25), H.tone(fur, 0.8), 0.5)
    # tufts hanging from the collar's lower edge
    n = int(round(f.w / 1.3))
    for i in range(n):
        x = (i + 0.5) * f.w / n
        ln = 0.7 + 0.45 * H.hsh(i, 3 if not back else 5)
        tuft = f.m_poly([(x - f.w / n / 2.0 - 0.05, depth - 0.7), (x + f.w / n / 2.0 + 0.05, depth - 0.7), (x + 0.1, depth + ln), (x - 0.1, depth + ln * 0.9)])
        f.fill(tuft, H.lin((0, depth - 0.7), (0, depth + ln), [(0, fur), (1, H.tone(fur, 0.84))]))
        f.fill(f.m_polyline([(x - f.w / n / 2.0, depth - 0.2), (x - 0.05, depth + ln * 0.9)], 0.1), fd, 0.55)
    f.fill(f.m_band(depth - 1.0, depth + 0.1) * (f.Y > depth - 1.0), fd, 0.0)
    for i in range(int(f.w / 0.9)):                                                                  # a fluff texture: short strokes
        x = (i + 0.5) * 0.9
        y = 0.5 + 1.6 * H.hsh(i, 9)
        f.fill(f.m_line((x, y), (x + 0.18, y + 0.5), 0.1, 0.06) * m, fd, 0.35)
        f.fill(f.m_line((x + 0.3, y - 0.2), (x + 0.4, y + 0.3), 0.1, 0.06) * m, "#ffffff", 0.45)
    H.shadow_under(f, depth + 0.45, 0.6, 0.3)


def star(f, cx, cy, r, color, glow=None):
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.42
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a) * 1.0))
    m = f.m_poly(pts)
    f.fill(f.m_poly([(x + 0.07, y + 0.1) for x, y in pts]), "#241a50", 0.7)
    f.fill(m, H.lin((cx, cy - r), (cx, cy + r), [(0, H.tone(color, 1.35)), (0.5, color), (1, H.tone(color, 0.7))]))
    H.bevel(f, m, depth=0.2, gain=1.0, spec=0.5)
    if glow:
        f.glow_fill(f.m_poly(pts, soft=3.0), glow, 0.9)
    return m


def choker(f, P):
    band = f.m_rect(2.55, 0.0, 5.45, 0.95)
    f.fill(band, H.metal_paint(0.0, 0.95, P["gold"], 1.0))
    H.bevel(f, band, depth=0.2, gain=1.1, spec=0.5)
    f.fill(f.m_rect(2.55, 0.9, 5.45, 1.0), H.tone(P["gold"], 0.45), 0.8)
    H.gem(f, 4.0, 0.95, 0.5, P["violet"], setting=P["gold"], facets=5, spec=0.9, glow=P["glow"], ry=0.55)


def body_front(f, P):
    d, dl, dd, g, gl = P["dress"], P["dress_l"], P["dress_d"], P["gold"], P["gold_l"]
    S.panel(f, f.m_all(), d, 1.22, 0.8, 0.08, folds=[1.0, 7.0], fold_depth=0.09, y0=2.0, y1=9.0)
    # the bodice: a lighter centre panel between two gold piping lines, darker side panels
    cen = f.m_poly([(2.1, 1.8), (5.9, 1.8), (6.1, 9.0), (1.9, 9.0)])
    f.fill(cen, H.lin((0, 1.8), (0, 9.0), [(0, H.tone(dl, 1.05)), (1, H.tone(d, 0.95))]), 0.8)
    H.sheen(f, cen, [3.0, 5.2], width=0.22, strength=0.2, tilt=0.02, y0=2.5, y1=8.6, color="#d0c8ff")
    for x0, x1 in ((0.0, 1.9), (6.1, 8.0)):
        f.shade(f.m_rect(x0, 1.8, x1, 9.0), 0.86, 0.6)
    for x0, x1 in ((2.15, 1.95), (5.85, 6.05)):
        H.trim_line(f, [(x0, 2.0), (x1, 9.0)], g, width=0.3, spec=0.5, outline=0.6)
        f.glow_fill(f.m_polyline([(x0, 2.0), (x1, 9.0)], 0.2), P["glow"], 0.3)
    S.seam(f, [(2.6, 2.3), (2.5, 8.8)], gl, 0.06, 0.45)
    S.seam(f, [(5.4, 2.3), (5.5, 8.8)], gl, 0.06, 0.45)
    star(f, 4.0, 4.6, 1.35, g, glow=P["glow"])
    if P["form2"]:
        f.fill(f.m_ring(4.0, 4.7, 2.1, 0.2), gl, 0.8, glow=P["glow"])                                # the gear ring of Infinity round the star
        for i in range(12):
            a = i * math.pi / 6
            f.fill(f.m_line((4.0 + 2.05 * math.cos(a), 4.7 + 2.05 * math.sin(a)), (4.0 + 2.5 * math.cos(a), 4.7 + 2.5 * math.sin(a)), 0.22, 0.22), gl, 0.8, glow=P["glow"])
    fur_collar(f, P)
    choker(f, P)
    belts(f, P, True)
    # the skirt (rows 11..12): violet with a gold hem line, pleats
    sk = f.m_rect(0.0, 11.0, f.w, 12.0)
    f.fill(sk, H.lin((0, 11.0), (0, 12.0), [(0, H.tone(d, 0.78)), (1, H.tone(d, 0.62))]))
    for x in (1.0, 3.0, 5.0, 7.0):
        f.fill(f.m_line((x, 11.1), (x + 0.1, 12.0), 0.16, 0.16), H.tone(dd, 0.8), 0.5)
    H.shadow_under(f, 11.0, 0.5, 0.25)


def belts(f, P, front=False):
    """A gold sash band with a violet inner stripe (y 9..11); the long sash itself hangs in front of the left hip as a separate model."""
    g = P["gold"]
    a = f.m_rect(0.0, 9.0, f.w, 10.3)
    f.fill(a, H.metal_paint(9.0, 10.3, g, 0.9))
    H.bevel(f, a, depth=0.3, gain=1.0, spec=0.4)
    f.fill(f.m_rect(0.0, 9.0, f.w, 9.1), H.tone(g, 0.5), 0.8)
    f.fill(f.m_rect(0.0, 10.15, f.w, 10.35), H.tone(g, 0.45), 0.8)
    b = f.m_rect(0.0, 10.35, f.w, 11.0)
    f.fill(b, H.lin((0, 10.35), (0, 11.0), [(0, H.tone(P["violet"], 0.9)), (1, H.tone(P["violet"], 0.6))]))
    S.seam(f, [(0.0, 9.65), (f.w, 9.65)], P["gold_l"], 0.055, 0.5, (0.3, 0.18), 0.2)
    H.shadow_under(f, 11.0, 0.5, 0.3)
    if front:
        f.rect(2.55, 8.75, 5.45, 10.6, "#3a2c10", r=0.3)                                            # the buckle: a gold frame, a light jewel
        f.rect(2.62, 8.82, 5.38, 10.53, H.metal_paint(8.82, 10.53, g, 1.15), r=0.26)
        H.bevel(f, f.m_rect(2.62, 8.82, 5.38, 10.53, 0.26), depth=0.25, gain=1.2, spec=0.7)
        H.gem(f, 4.0, 9.68, 0.62, P["gold_l"], setting=g, facets=6, spec=0.95, glow=P["glow"])


def body_back(f, P):
    d, g = P["dress"], P["gold"]
    S.panel(f, f.m_all(), d, 1.2, 0.78, 0.08, folds=[1.5, 3.0, 5.0, 6.5], fold_depth=0.09, y0=2.0, y1=9.0)
    S.seam(f, [(4.0, 2.4), (4.0, 8.9)], P["gold_l"], 0.07, 0.5)                                    # the lacing of the back (the cloak hides it)
    for y in (3.0, 4.4, 5.8, 7.2):
        f.fill(f.m_line((3.6, y), (4.4, y + 0.5), 0.14, 0.14), g, 0.8)
        f.fill(f.m_line((4.4, y), (3.6, y + 0.5), 0.14, 0.14), g, 0.8)
    fur_collar(f, P, True)
    belts(f, P)
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(d, 0.78)), (1, H.tone(d, 0.62))]))
    H.shadow_under(f, 11.0, 0.5, 0.25)


def body_side(f, P, name):
    d, g = P["dress"], P["gold"]
    S.panel(f, f.m_all(), d, 1.2, 0.8, 0.08, folds=[1.3, 2.7], fold_depth=0.08, y0=2.0, y1=9.0)
    edge = f.w - 0.7 if name == "west" else 0.7
    H.trim_line(f, [(edge, 2.2), (edge, 9.0)], g, width=0.26, spec=0.4, outline=0.55)
    fur_collar(f, P, True)
    belts(f, P)
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(d, 0.78)), (1, H.tone(d, 0.62))]))
    H.shadow_under(f, 11.0, 0.5, 0.25)


def body(cv, P):
    for nm in S.SIDES:
        for f in S.pair(cv, "body", nm):
            if nm == "front":
                body_front(f, P)
            elif nm == "back":
                body_back(f, P)
            else:
                body_side(f, P, nm)


# ================================================================================================ the arms
def arm(cv, part, P):
    sl, sll, g, gl = P["sleeve"], P["sleeve_l"], P["gold"], P["gold_l"]
    for nm in S.SIDES:
        for f in S.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 7.9)
            S.panel(f, sleeve, sl, 1.35, 0.78, 0.12, folds=[f.w * 0.3, f.w * 0.72], fold_depth=0.11, y0=0.6, y1=7.6)
            H.sheen(f, sleeve, [f.w * 0.3], width=0.22, strength=0.22, tilt=0.03, y0=0.8, y1=7.4, color="#c8c0ff")
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.45), H.tone(sll, 1.15))                                   # the lit shoulder
            f.fill(f.m_rect(0.0, 0.42, f.w, 0.55), H.tone(sl, 0.55), 0.8)
            S.seam(f, [(0.0, 0.95), (f.w, 0.95)], gl, 0.055, 0.45, (0.3, 0.2), 0.15)
            # the glove: long and black, from the wrist down
            gv = f.m_band(7.9, 12.0)
            f.fill(gv, H.lin((0, 7.9), (0, 12.0), [(0, H.tone(P["glove"], 1.35)), (0.4, H.tone(P["glove"], 1.15)), (1, H.tone(P["glove"], 0.7))]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(gv, 1.0 - 0.18 * (1.0 - H.smoothstep(0.0, 0.8, side)), 0.9)
            H.sheen(f, gv, [f.w * 0.35], width=0.26, strength=0.17, tilt=0.0, y0=8.6, y1=11.3, color="#8a7cc8")
            if nm in ("front", "back"):
                for x in (f.w * 0.3, f.w * 0.5, f.w * 0.7):
                    f.fill(f.m_line((x, 10.9), (x, 11.9), 0.18, 0.12) * gv, "#000000", 0.7)
                f.fill(f.m_band(10.75, 10.95) * gv, "#000000", 0.5)
            H.trim_band(f, 7.35, 8.35, g, spec=0.5, outline=0.6)                                          # the gold cuff of the bell sleeve
            S.seam(f, [(0.12, 7.85), (f.w - 0.12, 7.85)], gl, 0.06, 0.55, (0.3, 0.18), 0.2)
            H.shadow_under(f, 8.35, 0.6, 0.35)
            f.glow_fill(f.m_rect(0.0, 7.45, f.w, 7.6), P["glow"], 0.3)
            if P["form2"] and nm in ("front", "west", "east"):
                light_line(f, [(f.w * 0.5, 1.7), (f.w * 0.5, 4.2), (f.w * 0.55, 6.8)], P, 0.45)


def arms(cv, P):
    arm(cv, "arm_r", P)
    arm(cv, "arm_l", P)


# ================================================================================================ the legs
def leg(f, P, name):
    d, dd, g, gl = P["dress"], P["dress_d"], P["gold"], P["gold_l"]
    sk = f.m_band(0.0, 3.4)
    S.panel(f, sk, d, 1.2, 0.8, 0.12, folds=[1.2, 2.8], fold_depth=0.09, y0=0.2, y1=3.2)
    H.sheen(f, sk, [f.w * 0.3], width=0.2, strength=0.15, tilt=0.03, y0=0.4, y1=3.0, color="#c8c0ff")
    f.fill(f.m_rect(0.0, 3.2, f.w, 3.75), H.tone(dd, 1.0))                                              # a dark under-hem
    H.trim_band(f, 3.55, 4.55, g, spec=0.5, outline=0.6)                                                 # the gold hem
    S.seam(f, [(0.12, 4.05), (f.w - 0.12, 4.05)], gl, 0.06, 0.55, (0.3, 0.18), 0.2)
    f.glow_fill(f.m_rect(0.0, 3.62, f.w, 3.78), P["glow"], 0.3)
    H.shadow_under(f, 4.55, 0.9, 0.34)
    boot = f.m_band(4.55, 11.2)
    f.fill(boot, H.lin((0, 4.55), (0, 11.2), [(0, H.tone(P["boot"], 1.5)), (0.4, P["boot"]), (1, H.tone(P["boot"], 0.7))]))
    H.sheen(f, f.m_band(5.0, 10.8), [f.w * 0.3], width=0.2, strength=0.38, tilt=0.03, y0=5.2, y1=10.4, color="#b4a6e8")
    H.trim_band(f, 4.6, 5.3, H.tone(g, 0.8), spec=0.4, outline=0.5)                                       # the top of the boot, edged in gold
    f.fill(f.m_polyline([(0.0, 9.5), (f.w * 0.5, 9.75), (f.w, 9.55)], 0.11), H.tone(P["boot"], 0.5), 0.7)
    if name == "front":
        for y in (6.6, 8.6):                                                                              # two thin gold straps with studs
            f.fill(f.m_rect(0.0, y, f.w, y + 0.34), H.tone(g, 0.75), 0.9)
            f.fill(f.m_rect(0.0, y, f.w, y + 0.1), gl, 0.45)
            H.stud(f, f.w * 0.5, y + 0.17, 0.2, g)
        f.ellipse(f.w * 0.5, 10.4, 1.2, 0.6, H.tone(P["boot"], 2.0), a=0.35, soft=2.0)
    S.sole(f, 11.0, 12.0, welt="#5a4a7a")
    if name in ("west", "east"):
        S.seam(f, [(f.w * 0.5, 5.6), (f.w * 0.5, 10.8)], gl, 0.055, 0.28)


def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in S.SIDES:
            for f in S.pair(cv, part, nm):
                leg(f, P, nm)


# ================================================================================================ paint
def paint_form(spec, cid, masks, P):
    spec["hair"]["color"] = P["hair"]
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=P["style"])
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, P["hair_rgb"], skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    cov = S.covers(cv)
    head_front(cv, P)
    head_sides(cv, P)
    body(cv, P)
    arms(cv, P)
    legs(cv, P)
    S.hold_cover(cv, cov)
    skin, glow = cv.finish()
    return skin, glow, hair, P["hair_rgb"], slim
