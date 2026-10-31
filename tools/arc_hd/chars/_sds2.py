"""Shared drawing helpers of the sds2 group (Gowther, Merlin, Escanor): small, deterministic, numpy and Pillow only. (The leading underscore keeps this file out of the hand-module list of
test_arc_hd.py and of gen_arc_hd.py: it is not a character.)

Everything here works in MODEL pixels of a face (see tools/tier_bosses/hd/hd_core.py) and, like the pilot (chars/meliodas.py), keeps lines at texel width: at 256 one texel is a quarter of a model
pixel, so a line thinner than 0.2 breaks into dots.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import arc_hdlib as A  # noqa: E402

H = A.H
SIDES = ("front", "back", "west", "east")
LIMB_PARTS = ("body", "arm_r", "arm_l", "leg_r", "leg_l")


# ================================================================================================ the second layer and the lines
def cover_of(cv, part, name):
    """The second-layer coverage of the blueprint (what the 64x64 sheet draws on the second layer), kept so the redraw stays inside it."""
    return (cv.face(part, name, True).m_alpha() > 0.5).astype(np.float32)


def covers(cv, parts=LIMB_PARTS):
    return {(p, n): cover_of(cv, p, n) for p in parts for n in SIDES}


def hold_cover(cv, cov):
    """Clips every second-layer island to its blueprint coverage after the drawing (the overlay may only be where the sheet puts cloth)."""
    for (part, name), m in cov.items():
        f = cv.face(part, name, True)
        f.fill(1.0 - m, "#000000", mode="erase")
        f.glow[..., 3] *= m


def seam(f, pts, color="#c9a867", width=0.07, a=0.8, dash=(0.34, 0.2), shadow=0.28, mask=None):
    """A stitched line kept at texel scale (width at least 0.2, dashes at least 0.5 / 0.36)."""
    d = None if dash is None else (max(dash[0], 0.5), max(dash[1], 0.36))
    H.stitch(f, pts, color, width=max(width, 0.2), dash=d, shadow=shadow, a=a, mask=mask)


def edge_light(f, mask, x0, x1, color, k=1.6, a=0.3):
    """A thin lit edge on one side of a panel (smooth dark cloth needs it, or the panels read as flat)."""
    f.fill(mask * f.m_rect(x0, -1.0, x1, f.h + 1.0), H.tone(color, k), a)


def pair(cv, part, name):
    """(base island, second-layer island) of a face."""
    return A.lay(cv, part, name)


# ================================================================================================ hair painted on the front of the head
def hair_pal(mid, dark=None, light=None, high=None):
    """The four tones of a hair colour: shadow, body, light, glint."""
    return {"d": dark or H.tone(mid, 0.62), "m": mid, "l": light or H.tone(mid, 1.28), "h": high or H.tone(mid, 1.75)}


def lock(f, cx, w, top, length, lean, pal, shade=1.0, streak=True):
    """One tapered lock of hair hanging from y=top: a teardrop that leans, a dark edge on its shadow side, a light streak down its middle."""
    n = 9
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = w / 2.0 * (1.0 - t ** 1.6)
        x = cx + lean * t * t
        y = top + length * t
        left.append((x - half, y))
        right.append((x + half, y))
    m = f.m_poly(left + right[::-1])
    d, mm, l, h = pal["d"], pal["m"], pal["l"], pal["h"]
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(mm, d, 0.25 * (1 - shade))), (0.55, H.mix(l, mm, 0.2 * (1 - shade))), (1, H.mix(mm, d, 0.3 + 0.3 * (1 - shade)))]))
    f.fill(f.m_polyline(left[2:], 0.16) * m, d, 0.8)
    f.fill(f.m_polyline(right[2:], 0.16) * m, H.mix(d, "#000000", 0.25), 0.85)
    if streak:
        mid = [(cx + lean * (i / n) ** 2 * 0.9, top + length * 0.12 + length * 0.7 * (i / n)) for i in range(n)]
        f.fill(f.m_polyline(mid, 0.2) * m, h, 0.5 * shade)
    return m


def hair_mass(f, pal, poly, shadow_k=0.86, shadow_depth=2.4):
    """The mass of the fringe on the forehead: a gradient fill of a polygon, a soft shadow it throws on the skin below it, a few lighter strand lines."""
    mass = f.m_poly(poly)
    ymax = max(p[1] for p in poly)
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - ymax + 0.3) / shadow_depth, 0, 1) * (f.Y > ymax - 0.5), shadow_k)
    f.fill(mass, H.lin((0, 0), (0, ymax), [(0, H.mix(pal["d"], pal["m"], 0.45)), (0.5, pal["m"]), (1, H.tone(pal["m"], 0.9))]))
    return mass


def strands(f, pal, xs, y0, y1, mask, a=0.35):
    for x in xs:
        f.fill(f.m_line((x, y0), (x + 0.12, y1), 0.2, 0.12) * mask, pal["h"], a)


# ================================================================================================ small cloth and metal pieces
def panel(f, mask, color, top=1.2, bottom=0.8, round_=0.08, folds=None, fold_depth=0.09, y0=0.0, y1=None):
    """A cloth panel: the vertical gradient, the rounded far side and soft creases of H.cloth, without a weave."""
    H.cloth(f, mask, color, top=top, bottom=bottom, round_=round_, fold_xs=folds, fold_depth=fold_depth, fold_y0=y0, fold_y1=y1, weave_k=0.0)


def belt_straps(f, a_color, b_color, y0=9.0, y1=11.0, split=None, stitch_color="#d4ac68"):
    """Two straps: the wide one (y0..split) over the darker one (split..y1), stitched, with a cast shadow under the lower one."""
    split = split if split is not None else y0 + (y1 - y0) * 0.52
    a = f.m_rect(0.0, y0, f.w, split)
    f.fill(a, H.lin((0, y0), (0, split), [(0, H.tone(a_color, 1.4)), (0.4, a_color), (1, H.tone(a_color, 0.7))]))
    f.fill(f.m_rect(0.0, y0 + 0.03, f.w, y0 + 0.2), H.tone(a_color, 1.9), 0.5)
    b = f.m_rect(0.0, split, f.w, y1)
    f.fill(b, H.lin((0, split), (0, y1), [(0, H.tone(b_color, 1.5)), (0.3, b_color), (1, H.tone(b_color, 0.7))]))
    f.fill(f.m_rect(0.0, split - 0.03, f.w, split + 0.09), "#0a0709", 0.8)
    seam(f, [(0.0, y0 + 0.3), (f.w, y0 + 0.3)], stitch_color, 0.055, 0.7, (0.3, 0.18), 0.2)
    H.shadow_under(f, y1, 0.5, 0.3)


def rivet_row(f, xs, y, r, color, spec=0.7):
    for x in xs:
        H.stud(f, x, y, r, color, spec=spec)


def sole(f, y0, y1=12.0, welt="#5a4a5a", sole_dark="#0d0b10", sole_light="#1c1620"):
    """The welt and the sole of a boot at the bottom of a leg face."""
    f.fill(f.m_band(y0, y0 + 0.35), welt, 0.95)
    f.fill(f.m_band(y0 + 0.3, y1), H.lin((0, y0 + 0.3), (0, y1), [(0, sole_light), (1, sole_dark)]))
    f.fill(f.m_band(y0, y0 + 0.12), H.tone(welt, 1.5), 0.6)
