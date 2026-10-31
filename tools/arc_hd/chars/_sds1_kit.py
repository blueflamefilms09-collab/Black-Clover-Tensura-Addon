"""Shared drawing helpers of the hand-painted Seven Deadly Sins characters of group sds1 (Meliodas, King, Diane, Ban and their second forms), docs/arc/hd_arc_sds1.md.

Not a character module (the leading underscore keeps it out of the generator and of the module test): it only holds what four painters would otherwise copy. It is built on arc_hdlib (the bridge
and the HD library of the Tier List bosses). Everything works in MODEL PIXELS of a face (4 texels at 256), deterministic, numpy and Pillow only.
"""
import importlib.util
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import arc_hdlib as A  # noqa: E402

H = A.H
SIDES = ("front", "back", "west", "east")


def sibling(name):
    """Loads tools/arc_hd/chars/<name>.py by path (the form-2 modules reuse the code of their base character)."""
    path = os.path.join(HERE, name + ".py")
    spec = importlib.util.spec_from_file_location("sds1_" + name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


# ================================================================================================ the contract helpers (second layer stays inside the 64x64 blueprint)
def cover_of(cv, part, name):
    """The second-layer coverage of the blueprint (what the 64x64 sheet draws on the second layer)."""
    return (cv.face(part, name, True).m_alpha() > 0.5).astype(np.float32)


def all_covers(cv):
    return {(p, n): cover_of(cv, p, n) for p in ("body", "arm_r", "arm_l", "leg_r", "leg_l") for n in SIDES}


def hold_cover(cv, covers):
    """Clips every second-layer island to its blueprint coverage after the drawing (the overlay may only be where the sheet puts cloth)."""
    for (part, name), m in covers.items():
        f = cv.face(part, name, True)
        f.fill(1.0 - m, "#000000", mode="erase")
        f.glow[..., 3] *= m


def clear_glow(f):
    f.glow[...] = 0.0


def seam(f, pts, color="#c9a867", width=0.07, a=0.8, dash=(0.34, 0.2), shadow=0.28):
    """A stitched line; at 256 texels a line thinner than one texel (0.25 px) breaks into dots, so width and dashes are kept at texel scale."""
    d = None if dash is None else (max(dash[0], 0.5), max(dash[1], 0.36))
    H.stitch(f, pts, color, width=max(width, 0.2), dash=d, shadow=shadow, a=a)


def rim_light(f, mask, x0, x1, base, k=1.55, a=0.22):
    f.fill(mask * f.m_rect(x0, f.Y.min(), x1, f.Y.max() + 1.0), H.tone(base, k), a)


def ragged_hem(f, y, depth=0.9, period=0.9, seed=1, x0=0.0, x1=None):
    """Erases a torn lower edge from y down (use on the second layer only): a row of teeth of varying depth. Returns the mask of what was erased (so the torn edge can be darkened)."""
    x1 = f.w if x1 is None else x1
    n = max(2, int(round((x1 - x0) / period)))
    pts = [(x0, y + depth * 0.3)]
    for i in range(n):
        xa = x0 + (x1 - x0) * (i + 0.15) / n
        xb = x0 + (x1 - x0) * (i + 0.6) / n
        pts += [(xa, y + depth * (0.05 + 0.25 * H.hsh(i, seed, 3))), (xb, y + depth * (0.45 + 0.8 * H.hsh(i, seed, 5)))]
    pts += [(x1, y + depth * 0.3), (x1 + 0.5, f.h + 1.0), (x0 - 0.5, f.h + 1.0)]
    gone = f.m_poly(pts)
    f.clear(gone)
    return gone


# ================================================================================================ hair on the face (the fringe) and locks
def lock(f, pal, cx, w, top, length, lean, shade=1.0, edge=0.07):
    """One tapered lock of hair hanging from y=top: a teardrop that leans, a dark edge on its shadow side, a light streak down its middle. pal = dict(d, m, l, h) dark / mid / light / highlight."""
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
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(pal["m"], pal["d"], 0.25 * (1 - shade))), (0.55, H.mix(pal["l"], pal["m"], 0.2 * (1 - shade))), (1, H.mix(pal["m"], pal["d"], 0.3 + 0.3 * (1 - shade)))]))
    f.fill(f.m_polyline(left[2:], edge) * m, pal["d"], 0.8)
    f.fill(f.m_polyline(right[2:], edge) * m, H.mix(pal["d"], "#000000", 0.25), 0.85)
    mid = [(cx + lean * (i / n) ** 2 * 0.9, top + length * 0.12 + length * 0.7 * (i / n)) for i in range(n)]
    f.fill(f.m_polyline(mid, 0.12) * m, pal["h"], 0.55 * shade)
    return m


def spike(f, pal, cx, w, top, length, lean, shade=1.0, edge=0.07):
    """A thick pointed clump of hair (a spike): wide at the root, a pointed tip that leans, a dark shadow edge, a light streak. pal = dict(d, m, l, h)."""
    n = 8
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = w / 2.0 * (1.0 - t ** 1.25)
        x = cx + lean * t ** 1.5
        y = top + length * t
        left.append((x - half, y))
        right.append((x + half, y))
    m = f.m_poly(left + right[::-1])
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(pal["l"], pal["m"], 0.3 + 0.4 * (1 - shade))), (0.5, pal["m"]), (1, H.mix(pal["m"], pal["d"], 0.55))]))
    f.fill(f.m_polyline(left[1:], edge) * m, pal["d"], 0.75)
    f.fill(f.m_polyline(right[1:], edge) * m, H.mix(pal["d"], "#000000", 0.2), 0.9)
    mid = [(cx - w * 0.12 + lean * (i / n) ** 1.5 * 0.9, top + length * 0.1 + length * 0.62 * (i / n)) for i in range(n)]
    f.fill(f.m_polyline(mid, 0.11) * m, pal["h"], 0.6 * shade)
    return m


def hair_palette(base):
    """dict(d, m, l, h) for a hair colour: shadow, mid, light, highlight."""
    return {"d": H.tone(base, 0.62), "m": H.col(base), "l": H.tone(base, 1.22), "h": H.tone(base, 1.6)}


def fringe_mass(f, pal, poly, y0=0.0, y1=2.2):
    """The hair mass of the forehead (a polygon of the face, model pixels) with the soft shadow it throws on the skin under it."""
    mass = f.m_poly(poly)
    f.fill(mass, H.lin((0, y0), (0, y1), [(0, H.mix(pal["d"], pal["m"], 0.45)), (0.5, pal["m"]), (1, H.tone(pal["m"], 0.9))]))
    return mass


# ================================================================================================ skin
def limb_skin(f, skin, y0, y1, tendon=True):
    """Bare skin of an arm or a leg from y0 to y1: a soft gradient, darker at both sides (the roundness), a sheen, the shadow of whatever hangs above, a tendon line."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(skin[1], 0.86)), (0.2, skin[1]), (0.7, H.tone(skin[1], 0.98)), (1, H.tone(skin[0], 0.92))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.12 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
    H.sheen(f, m, [f.w * 0.42], width=0.4, strength=0.12, tilt=0.0, y0=y0 + 0.6, y1=y1 - 0.4, color="#fff0e0")
    H.shadow_under(f, y0 + 0.05, 0.9, 0.25, mask=m)
    if tendon:
        f.fill(f.m_line((f.w * 0.64, y0 + 1.6), (f.w * 0.58, y0 + 3.4), 0.16, 0.08) * m, H.tone(skin[0], 0.8), 0.4)
    f.fill(f.m_band(y1 - 0.7, y1), H.tone(skin[0], 0.9), 0.25)
    return m


def fingers(f, skin, y0=10.9, y1=11.9, n=3):
    """The bare hand: finger gaps on the front and the back."""
    if f.name in ("front", "back"):
        for i in range(1, n + 1):
            x = f.w * i / (n + 1)
            f.fill(f.m_line((x, y0), (x, y1), 0.17, 0.12), H.tone(skin[0], 0.7), 0.6)
        f.fill(f.m_band(y0 - 0.1, y0 + 0.05), H.tone(skin[0], 0.72), 0.5)


# ================================================================================================ boots and trousers
def trousers(f, color, y0, y1, name, knee=True, fold_xs=(1.2, 2.8), seam_color="#8a7e9a"):
    H.cloth(f, f.m_band(y0, y1), color, top=1.3, bottom=0.8, round_=0.12, fold_xs=list(fold_xs), fold_depth=0.10, fold_y0=y0 + 0.4, fold_y1=y1 - 0.4, weave_k=0.0)
    if knee:
        for y, k in ((2.6, 0.6), (3.7, 0.45)):
            f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.28), (f.w - 0.2, y + 0.05)], 0.1) * f.m_band(y0, y1), H.tone(color, 0.5), k)
            f.fill(f.m_polyline([(0.2, y - 0.15), (f.w * 0.5, y + 0.13), (f.w - 0.2, y - 0.10)], 0.06) * f.m_band(y0, y1), H.tone(color, 1.8), k * 0.5)
    if name in ("west", "east"):
        seam(f, [(f.w * 0.5, y0 + 0.2), (f.w * 0.5, y1 - 0.4)], seam_color, 0.06, 0.55)


def boot(f, color, y0, y1, name, strap=None, gold="#e0b43e", cuff=None, toe=True, sole="#0d0b0a"):
    """A boot from y0 to the foot line at 11.25 with welt and sole to 12: a gradient, a sheen, ankle wrinkles, an optional strap with a buckle across the shin and a polished toe cap."""
    boot_m = f.m_band(y0, 11.25)
    f.fill(boot_m, H.lin((0, y0), (0, 11.25), [(0, H.tone(color, 1.25)), (0.5, color), (1, H.tone(color, 0.72))]))
    H.shadow_under(f, y0 + 0.05, 0.7, 0.34)
    H.sheen(f, boot_m, [f.w * 0.3], width=0.2, strength=0.26, tilt=0.04, y0=y0 + 0.35, y1=10.6, color="#c8a898")
    f.fill(f.m_polyline([(0.0, 9.45), (f.w * 0.5, 9.7), (f.w, 9.5)], 0.11), H.tone(color, 0.45), 0.7)
    f.fill(f.m_polyline([(0.0, 9.78), (f.w * 0.5, 10.0), (f.w, 9.8)], 0.07), H.tone(color, 1.8), 0.32)
    if strap and name == "front":
        f.rect(0.0, 8.1, f.w, 8.9, H.tone(strap, 1.05), r=0.0)
        f.fill(f.m_rect(0.0, 8.1, f.w, 8.28), H.tone(strap, 1.5), 0.8)
        f.fill(f.m_rect(0.0, 8.82, f.w, 8.92), "#0e0a08", 0.7)
        H.stud(f, f.w * 0.5, 8.5, 0.34, gold)
    if toe and name == "front":
        f.ellipse(f.w * 0.5, 10.25, 1.2, 0.65, H.tone(color, 1.6), a=0.38, soft=2.0)
    f.fill(f.m_band(11.0, 11.35), H.tone(color, 1.5), 0.9)
    f.fill(f.m_band(11.3, 12.0), H.lin((0, 11.3), (0, 12.0), [(0, "#1c1512"), (1, sole)]))
    f.fill(f.m_band(11.0, 11.12), H.tone(color, 2.0), 0.6)


# ================================================================================================ marks and light
def glow_line(f, pts, color, width=0.22, a=0.95, glow=True, core="#ffffff", core_a=0.5):
    """A line of light on the skin: a coloured stroke with a bright core and a glow-map twin."""
    m = f.m_polyline(pts, width)
    f.fill(m, color, a, glow=glow)
    f.fill(f.m_polyline(pts, width * 0.38) * m, core, core_a)
    return m


def glow_soft(f, mask, color, a=0.7):
    f.glow_fill(mask, color, a)
