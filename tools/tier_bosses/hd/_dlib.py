"""Shared helpers of the HD group d painters (axe_hand_morgan, bertholdt_hoover, shou_tucker, light_yagami).  The leading underscore keeps this file out of the list of character modules
(test_hdlib.HD_IDS).  Only the shared hd_* files of docs/bosses/hd_outfits.md are imported, nothing in them is changed.

* `lay(cv, part, name)`         the base island and the second-layer island of a face (draw the crisp things on both: PlayerLod drops the second layer and the hair beyond 24 blocks)
* `base_fill(cv, colour)`       fills every base island, so that no hole is left (the test fails on a hole)
* `head_lod(cv, ...)`           the base head under the 3D hair: hair painted on the sides, back and top (what shows beyond 24 blocks), the ear, the nape, the chin shade
* `paint_hair`, `buckle`, `rivets`, `stitch_seam`, `leather`, `ribbed`  small cloth, leather and metal pieces used by the four modules
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402

SIDES = ("front", "back", "west", "east")
ALL6 = ("front", "back", "west", "east", "top", "bottom")


def lay(cv, part, name):
    """The base island and the second-layer island of a face."""
    return (cv.face(part, name, False), cv.face(part, name, True))


def base_fill(cv, colour):
    for f in cv.faces(overlay=(False,)):
        f.fill(f.m_all(), colour)


def mirror(pts, w):
    """The same polygon seen from the other side of a w-wide face."""
    return [(w - x, y) for x, y in pts]


# ================================================================================================ hair painted on the base head
def hair_strands(f, mask, c, strands=2.6, seed=0, flow=0.25, a=1.0):
    """Strands painted through a mask (no colour outside of it)."""
    n = int(f.w * strands)
    for i in range(n):
        x = (i + 0.5) / strands + 0.22 * math.sin(i * 1.9 + seed)
        dark = (i + seed) % 2 == 0
        m = f.m_line((x, 0.0), (x + flow * math.sin(i * 1.3 + seed), f.h), 0.075, 0.04) * mask
        f.fill(m, H.tone(c, 0.62 if dark else 1.4), (0.42 if dark else 0.2) * a)
    return f


def paint_hair(f, mask, c, strands=2.6, seed=0, top=1.28, bottom=0.72, a=1.0, flow=0.25):
    f.fill(mask, H.lin((0, 0), (0, f.h), [(0, H.tone(c, top)), (0.55, c), (1, H.tone(c, bottom))]), a)
    hair_strands(f, mask, c, strands, seed, flow, a)
    return f


def head_lod(cv, skin, hairc, sb_x=7.3, sb_y=3.4, fringe=1.5, ear_x=4.5, nape=6.7, back_nape=6.9, ear=True, strands=2.6, skin_hi=None, hair_top=None, ear_y=4.55, jaw_shade=0.18):
    """The base head round the face: the west and east sides (skin in front, hair behind the ear, a sideburn, an ear), the back (hair down to the nape, the neck below), the top (hair) and the
    bottom (the shaded neck). `skin` = (shadow, base, light). The 3D hair covers all of it near; this is what shows when the hair is dropped (24 blocks)."""
    sk_d, sk, sk_l = skin
    for nm in ("west", "east"):
        f = cv.face("head", nm)
        f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, sk_l), (0.5, sk), (1, H.tone(sk_d, 0.95))]))
        pts = [(-0.2, -0.2), (8.2, -0.2), (8.2, fringe), (sb_x + 0.5, fringe + 0.15), (sb_x, sb_y), (ear_x, sb_y + 0.2), (ear_x - 0.15, nape), (-0.2, nape + 0.1)]
        if nm == "east":
            pts = mirror(pts, 8.0)
        hm = f.m_poly(pts)
        paint_hair(f, hm, hairc, strands=strands, seed=3 if nm == "west" else 7)
        f.fill(f.m_polyline(pts[3:7], 0.12), H.tone(hairc, 0.55), 0.35)
        # the soft shadow the hair throws on the skin
        edge = H.minus(f.m_all(), hm)
        # the ear (the front of the side face is x=8 on the west face, x=0 on the east face)
        if ear:
            ex = (ear_x + 0.55) if nm == "west" else 8.0 - (ear_x + 0.55)
            f.ellipse(ex + (0.07 if nm == "west" else -0.07), ear_y + 0.1, 0.62, 0.98, H.tone(sk_d, 0.8), a=0.45, soft=2.0)
            f.ellipse(ex, ear_y, 0.62, 0.98, H.rad((ex - 0.1, ear_y - 0.3), 1.2, [(0, sk_l), (0.6, sk), (1, H.tone(sk_d, 0.92))]))
            f.ellipse(ex + (0.04 if nm == "west" else -0.04), ear_y + 0.05, 0.28, 0.56, H.tone(sk_d, 0.82), a=0.7, soft=2.0)
        # the cheek rim and the jaw line: darker toward the chin
        f.shade(edge, 1.0 - jaw_shade * H.smoothstep(5.6, 8.0, f.Y))
    b = cv.face("head", "back")
    b.fill(b.m_all(), sk_d)
    hm = b.m_poly([(-0.2, -0.2), (8.2, -0.2), (8.2, back_nape), (6.6, back_nape + 0.35), (4.0, back_nape - 0.1), (1.4, back_nape + 0.35), (-0.2, back_nape)])
    paint_hair(b, hm, hairc, strands=strands, seed=1)
    b.shade(H.minus(b.m_all(), hm), 0.84)
    t = cv.face("head", "top")
    paint_hair(t, t.m_all(), hairc if hair_top is None else hair_top, strands=strands, seed=5, top=1.0, bottom=1.0, flow=0.4)
    for k in range(3):                                                                           # a whorl of strands on the crown
        a0 = k * 2.1
        t.polyline([(4.0 + 0.4 * math.cos(a0 + s * 0.6) * s * 0.35, 4.4 + 0.4 * math.sin(a0 + s * 0.6) * s * 0.35) for s in range(0, 8)], H.tone(hairc, 0.6), 0.1, a=0.5)
    d = cv.face("head", "bottom")
    d.fill(d.m_all(), H.lin((0, 0), (0, 8), [(0, H.tone(sk_d, 0.9)), (1, H.tone(sk_d, 0.7))]))


# ================================================================================================ metal, leather, cloth bits
def rivets(f, pts, r=0.13, color="#c9ced8", glow=None):
    for x, y in pts:
        H.stud(f, x, y, r, color, glow=glow)


def buckle(f, cx, cy, w, h, color="#cfd5df", hole="#1c1a22", prong=True, r=0.12, glow=None):
    """A belt buckle: a bevelled frame with an open middle and a prong."""
    x0, x1, y0, y1 = cx - w / 2.0, cx + w / 2.0, cy - h / 2.0, cy + h / 2.0
    frame = f.m_rect(x0, y0, x1, y1, r)
    f.fill(f.m_rect(x0 - 0.05, y0 - 0.06, x1 + 0.05, y1 + 0.08, r), H.tone(color, 0.35), 0.6)
    f.fill(frame, H.metal_paint(y0, y1, color))
    t = min(w, h) * 0.24
    inner = f.m_rect(x0 + t, y0 + t, x1 - t, y1 - t, r * 0.5)
    f.fill(inner, hole)
    H.bevel(f, frame, depth=min(w, h) * 0.3, gain=1.2, spec=0.55, spec_pow=12)
    H.bevel(f, inner, depth=0.1, gain=-0.6)
    if prong:
        f.line((cx - w * 0.04, y0 + t * 0.5), (cx - w * 0.04, y1 - t * 0.5), H.tone(color, 1.15), 0.09, 0.09)
    if glow:
        f.glow_fill(frame, glow, 0.6)


def stitch_seam(f, pts, color="#d8c9a4", width=0.05, dash=(0.26, 0.17)):
    H.stitch(f, pts, color, width=width, dash=dash, shadow=0.0)


def leather(f, mask, c, y0, y1, grain=0.03, sheen_x=None, sheen_strength=0.26):
    """A leather band or strap: a vertical gradient (light on top), a faint grain and optional sheen streaks."""
    f.fill(mask, H.lin((0, y0), (0, y1), [(0, H.tone(c, 1.22)), (0.45, c), (1, H.tone(c, 0.68))]))
    if grain:
        H.weave(f, mask, strength=grain, period=0.5, diagonal=True)
    if sheen_x:
        H.sheen(f, mask, sheen_x, width=0.2, strength=sheen_strength, tilt=0.04, y0=y0, y1=y1, color="#ffffff")
    return mask


def ribbed(f, mask, xs, c, depth=0.16, width=0.1):
    """Thin raised ribs (steel plate ridges, corduroy): a dark line with a light flank at every x."""
    for x in xs:
        f.fill(f.m_line((x, 0.0), (x, f.h), width, width) * mask, H.tone(c, 0.55), depth * 3.0)
        f.fill(f.m_line((x - 0.1, 0.0), (x - 0.1, f.h), width, width) * mask, H.tone(c, 1.45), depth * 2.2)


def noise_speckle(f, mask, c, n=40, seed=0, a=0.4, r=0.035, area=None):
    """Tiny dots (stubble, dust, grain) placed by a hash: deterministic."""
    x0, y0, x1, y1 = area if area else (0.0, 0.0, f.w, f.h)
    for k in range(n):
        x = x0 + (x1 - x0) * H.hsh(k, seed, 1)
        y = y0 + (y1 - y0) * H.hsh(k, seed, 2)
        f.fill(f.m_ellipse(x, y, r, r) * mask, c, a)


def clear_top_bottom_overlay(cv, part):
    cv.face(part, "top", True).clear()
    cv.face(part, "bottom", True).clear()


def slash_pocket(f, outer_left, c, y0=0.25, y1=2.1, reach=0.55, dark=0.55, light=1.4):
    """A slanted trouser pocket on the front of a leg: the opening runs from the waist near the middle down to the side seam, drawn as a shadow line, a light lip, a stitch and a
    small bar tack.  `outer_left` is True where the side seam is the left edge (the character's right leg, seen from the front)."""
    w = f.w
    ox = 0.0 if outer_left else w
    ix = w * reach if outer_left else w * (1.0 - reach)
    a, b = (ix, y0), (ox, y1)
    f.line(a, b, H.tone(c, dark), 0.09, 0.09, a=0.7)
    f.line((a[0], a[1] + 0.1), (b[0], b[1] + 0.1), H.tone(c, light), 0.06, 0.06, a=0.45)
    H.stitch(f, [(a[0], a[1] + 0.2), (b[0] + (0.1 if outer_left else -0.1), b[1] + 0.2)], H.tone(c, 0.8), width=0.035, dash=(0.2, 0.14), shadow=0.0)
    f.line((ix - 0.1, y0 - 0.05), (ix + 0.1, y0 + 0.15), H.tone(c, dark), 0.05, 0.05, a=0.7)
