"""HD skins, part 2: detail helpers (3.38.0). Everything takes a `Face` of hd_core and a mask, in model-pixel coordinates of that face, and is deterministic.

* surfaces: `bevel` (a lit, domed edge with a specular streak: metal plates, trim, buttons, belts), `folds` (soft cloth creases), `weave` (a faint fabric weave), `shadow_under` (a soft shadow
  band under a hem or a belt), `rim` (a thin light along the lit edge of a shape), `sheen` (satin or leather streaks), `scallops` (feather or frill rows).
* trims: `stitch` (dashed stitching along a path), `fringe` (hanging threads), `gem` (faceted stone with a specular dot and an optional glow), `stud` (a round metal button).
* faces: `eye`, `brow`, `blush`, `mouth_smile`, `nose` (anime eyes at 4x: sclera with a lid shadow, iris gradient with a rim, pupil, two highlights, thick upper lash with a flick, thin lower lash).
"""
import math

import numpy as np

from hd_core import (Face, blur, col, cov, hsh, lin, rad, mix, tone, smoothstep, union, minus, sd_poly, sd_segment, lum)  # noqa: F401


# ================================================================================================ surfaces
def height_normals(face, mask, depth=0.45):
    """The slope of a dome built from a mask: (gx, gy) per texel, about 0..1 at the mask's edge, 0 in the flat middle and outside. depth = the width of the bevel in model pixels."""
    f = face.f
    h = blur(np.asarray(mask, np.float32), max(0.8, depth * f * 0.55))
    gy, gx = np.gradient(h)
    k = max(1.0, depth * f) * 1.6
    return gx * k, gy * k


def bevel(face, mask, depth=0.45, gain=1.1, spec=0.0, spec_pow=26.0, spec_color="#fffbe8", a=1.0):
    """Light a mask like a rounded, raised plate: edges that face the light go lighter, the others go darker, an optional specular streak rides the lit edge."""
    mask = np.asarray(mask, np.float32)
    gx, gy = height_normals(face, mask, depth)
    lx, ly = face.light
    n = np.sqrt(gx * gx + gy * gy + 1.0)
    L = np.array([lx * 0.62, ly * 0.62, 0.78], np.float32)
    L /= np.linalg.norm(L)
    nl = (-gx * L[0] + -gy * L[1] + L[2]) / n
    diff = nl - L[2]
    face.shade(mask, 1.0 + diff * gain, a)
    if spec > 0:
        Hv = L + np.array([0, 0, 1], np.float32)
        Hv /= np.linalg.norm(Hv)
        ns = np.clip((-gx * Hv[0] + -gy * Hv[1] + Hv[2]) / n, 0, 1)
        face.fill(mask * (ns ** spec_pow) * spec, spec_color, a, mode="add")
    return face


def rim(face, mask, width=0.28, strength=0.35, a=1.0):
    """A thin light band along the edges of the mask that face the light (a rim light), and a faint dark band on the far edges."""
    mask = np.asarray(mask, np.float32)
    f = face.f
    lx, ly = face.light
    sh = max(1, int(round(width * f)))
    off = shift(mask, int(round(ly * sh)), int(round(lx * sh)))     # the mask moved toward the light: what sticks out of the shifted copy is the lit edge
    lit = np.clip(mask - off, 0, 1) * mask
    away = shift(mask, int(round(-ly * sh)), int(round(-lx * sh)))
    dark = np.clip(mask - away, 0, 1) * mask
    face.shade(lit, 1.0 + strength, a)
    face.shade(dark, 1.0 - strength * 0.7, a)
    return face


def shift(arr, dy, dx):
    from hd_core import shift as _s
    return _s(arr, dy, dx)


def shadow_under(face, y, width=0.9, strength=0.28, mask=None, x0=None, x1=None):
    """A soft shadow band hanging below y (a hem, a belt, a collar): darkest at y, gone after `width` model pixels."""
    t = np.clip((face.Y - y) / max(width, 1e-3), 0, 1)
    band = (1.0 - t) ** 1.7 * (face.Y >= y - 0.02)
    if x0 is not None:
        band = band * ((face.X >= x0) & (face.X <= x1))
    m = band * (np.ones((face.H, face.W), np.float32) if mask is None else np.asarray(mask, np.float32))
    face.shade(m, 1.0 - strength)
    return face


def folds(face, mask, xs, depth=0.10, width=0.26, sway=0.22, y0=0.0, y1=None, light_side=1.0, fade_top=1.2):
    """Soft vertical cloth creases at the given x positions: a dark core, a light flank on the lit side, a slight wave, strongest toward the bottom."""
    mask = np.asarray(mask, np.float32)
    y1 = face.h if y1 is None else y1
    X, Y = face.X, face.Y
    rel = np.clip((Y - y0) / max(1e-3, y1 - y0), 0, 1)
    amp = smoothstep(0.0, fade_top / max(1e-3, y1 - y0) + 0.05, rel) * (0.45 + 0.55 * rel) * ((Y >= y0) & (Y <= y1))
    for i, x in enumerate(xs):
        wob = x + sway * np.sin(Y * 1.4 + i * 1.9 + x)
        d = X - wob
        core = np.exp(-(d / width) ** 2)
        flank = np.exp(-((d + light_side * width * 1.5) / (width * 1.2)) ** 2)
        k = 1.0 - depth * core * amp + depth * 0.65 * flank * amp
        face.shade(mask, k)
    return face


def weave(face, mask, strength=0.03, period=0.75, diagonal=True):
    """A faint twill: a diagonal pattern of `period` model pixels (about 3 texels at 256), strength = brightness swing."""
    p = (face.X + face.Y) if diagonal else face.X
    k = 1.0 + strength * np.sin(p * (2 * math.pi / period)) * 0.5
    face.shade(mask, k)
    return face


def sheen(face, mask, xs, width=0.22, strength=0.35, tilt=0.0, color="#ffffff", y0=0.0, y1=None):
    """Satin / leather streaks: bright narrow bands (a specular lobe) at the given x positions, slightly slanted, fading at both ends."""
    y1 = face.h if y1 is None else y1
    rel = np.clip((face.Y - y0) / max(1e-3, y1 - y0), 0, 1)
    ends = np.clip(np.sin(rel * math.pi), 0, None) ** 0.8 * ((face.Y >= y0) & (face.Y <= y1))
    for x in xs:
        d = face.X - (x + tilt * (face.Y - (y0 + y1) / 2))
        face.fill(np.asarray(mask, np.float32) * np.exp(-(d / width) ** 2) * ends * strength, color, mode="add")
    return face


def scallops(face, mask, y0, rows=1, cell=1.0, depth=1.0, base=None, tip=None, edge=None, offset=0.0, edge_a=0.8, shadow=0.24, a=1.0):
    """Rows of overlapping rounded tabs (feathers, frills, scales) starting at y0, each `depth` high and `cell` wide, every other row shifted by half a cell. Like roof shingles the upper
    row lies over the lower one: the rows are painted from the bottom up, each throws a soft shadow on the row under it. The tab body is `base` (or the existing colour, shaded, when None),
    lightening toward the rounded tip (`tip`); the rim of every tab is `edge` (a darker line)."""
    mask = np.asarray(mask, np.float32)
    f = face.f
    X, Y = face.X, face.Y
    for r in reversed(range(rows)):
        ry = y0 + r * depth * 0.62
        xo = offset + (0.5 * cell if r % 2 else 0.0)
        u = ((X - xo) / cell) % 1.0 - 0.5                       # -0.5..0.5 across a tab
        v = (Y - ry) / depth                                    # 0 at the tab's top, 1 at its tip
        tip_y = 1.0 - 4.0 * u * u * 0.55                        # the rounded lower outline of the tab (u = 0 lowest)
        sd = (tip_y - v) * depth
        tab = np.clip(0.5 + sd * f, 0, 1) * (v >= -0.02)
        m = mask * tab
        t = np.clip(v / np.maximum(tip_y, 1e-3), 0, 1)
        if base is not None:
            colr = mix(base, tip if tip is not None else base, smoothstep(0.35, 1.0, t))
            face.fill(m, np.asarray(colr, np.float32), a)
        else:
            face.shade(m, 1.0 + 0.10 * smoothstep(0.4, 1.0, t) - 0.08 * (1 - t), a)
        if edge is not None:
            line = np.clip(1.0 - np.abs(sd) * f / 1.6, 0, 1) * tab * (u * u < 0.25)
            face.fill(mask * line, edge, a * edge_a)
        if shadow > 0:
            under = np.clip(1.0 - (-sd) * 1.0 / 0.55, 0, 1) * (sd < 0) * (v >= 0)
            face.shade(mask * under, 1.0 - shadow, a)
    return face


# ================================================================================================ trims
def stitch(face, pts, color="#f4e9c8", width=0.085, dash=(0.40, 0.24), shadow=0.30, a=0.95, mask=None):
    """Dashed stitching along a path (model pixels), with a thin shadow offset down and right. dash = (stitch, gap) in model pixels; dash=None for a solid line."""
    d, arc = face.polyline_sd(pts)
    f = face.f
    line = np.clip(0.5 - (d - width / 2) * f, 0, 1)
    if dash:
        per = dash[0] + dash[1]
        ph = (arc % per)
        line = line * np.clip((dash[0] - ph) * f + 0.5, 0, 1) * np.clip(ph * f + 0.5, 0, 1)
    if mask is not None:
        line = line * np.asarray(mask, np.float32)
    if shadow > 0:
        sh = shift(line, max(1, f // 3), max(1, f // 4))
        face.shade(sh * (1.0 - line) * (np.ones((face.H, face.W), np.float32) if mask is None else np.asarray(mask, np.float32)), 1.0 - shadow * 0.6)
    face.fill(line, color, a)
    return face


def fringe(face, x0, x1, y0, length, n=7, color="#e8b830", dark="#a86a14", width=0.09, var=0.35, seed=0, mask=None):
    """Hanging threads (an epaulette's fringe): n thin vertical lines from y0, lengths varying by `var`, a light thread with a darker side line."""
    for i in range(n):
        x = x0 + (x1 - x0) * (i + 0.5) / n
        ln = length * (1.0 - var * hsh(i, seed, 7))
        sway = (hsh(i, seed, 3) - 0.5) * 0.10
        m = face.m_line((x, y0), (x + sway, y0 + ln), width, width * 0.7)
        if mask is not None:
            m = m * mask
        face.fill(face.m_line((x + 0.07, y0), (x + sway + 0.07, y0 + ln), width * 0.8, width * 0.5) * (mask if mask is not None else 1.0), dark, 0.6)
        face.fill(m, lin((x, y0), (x, y0 + ln), [(0, color), (0.6, tone(color, 1.1)), (1, tone(color, 0.8))]))
    return face


def stud(face, cx, cy, r, color="#e8b830", rim_color=None, spec=0.8, glow=None):
    """A round metal button: a dark rim ring, a domed face lit from the upper left, a specular dot."""
    rim_color = rim_color if rim_color is not None else tone(color, 0.5)
    face.ellipse(cx + r * 0.10, cy + r * 0.14, r * 1.08, r * 1.08, rim_color, a=0.5, soft=2.0)
    face.ellipse(cx, cy, r, r, rim_color)
    face.ellipse(cx - r * 0.06, cy - r * 0.08, r * 0.82, r * 0.82, rad((cx - r * 0.3, cy - r * 0.35), r * 1.3, [(0, tone(color, 1.45)), (0.5, color), (1, tone(color, 0.62))]))
    if spec > 0:
        face.ellipse(cx - r * 0.34, cy - r * 0.38, r * 0.24, r * 0.17, "#ffffff", a=spec, rot=-35, soft=1.4)
    if glow is not None:
        face.glow_fill(face.m_ellipse(cx, cy, r * 0.85, r * 0.85), glow if glow is not True else color, 0.9)
    return face


def gem(face, cx, cy, r, color="#d81c40", setting="#e8b830", facets=6, spec=0.95, glow=True, ry=None):
    """A faceted stone: a metal bezel, a radial body (light core, dark rim), thin facet lines, a specular glint; glow=True also lights the glow map."""
    ry = r if ry is None else ry
    if setting is not None:
        face.ellipse(cx, cy, r * 1.28, ry * 1.28, rad((cx - r * 0.3, cy - r * 0.4), r * 1.8, [(0, tone(setting, 1.4)), (0.55, setting), (1, tone(setting, 0.55))]))
    body = face.m_ellipse(cx, cy, r, ry)
    face.fill(body, rad((cx - r * 0.25, cy - ry * 0.3), max(r, ry) * 1.35, [(0, tone(color, 1.55)), (0.45, color), (1, tone(color, 0.45))]))
    for i in range(facets):
        ang = math.pi * 2 * i / facets + 0.3
        p1 = (cx + math.cos(ang) * r * 0.95, cy + math.sin(ang) * ry * 0.95)
        lit = (math.cos(ang) * face.light[0] + math.sin(ang) * face.light[1]) > 0
        face.fill(face.m_line((cx, cy), p1, r * 0.07, r * 0.03) * body, "#ffffff" if lit else tone(color, 0.35), 0.28)
    face.fill(face.m_ring(cx, cy, r * 0.62, r * 0.06) * body, tone(color, 1.6), 0.35)
    if spec > 0:
        face.ellipse(cx - r * 0.38, cy - ry * 0.42, r * 0.26, ry * 0.18, "#ffffff", a=spec, rot=-35, soft=1.4)
        face.ellipse(cx + r * 0.32, cy + ry * 0.38, r * 0.10, ry * 0.07, "#ffffff", a=spec * 0.55, soft=1.4)
    if glow:
        face.glow_fill(face.m_ellipse(cx, cy, r * 1.05, ry * 1.05, soft=3.0), glow if glow is not True else tone(color, 1.2), 0.95)
    return face


# ================================================================================================ the face
def _lid(u, h, upper=True, bulge=1.0):
    """Eyelid curve height at u in -1..1."""
    if upper:
        return -h * 0.5 * (1.0 - np.abs(u) ** 2.3) ** 0.85 * bulge
    return h * 0.5 * (1.0 - np.abs(u) ** 1.9) ** 0.95 * 0.85


def eye(face, cx, cy, w, h, iris=("#2a1a50", "#7a4ad0", "#d8bcff"), lash="#1c1226", sclera="#fffefb", outer=None, tilt=0.10, pupil="#0c0814", iris_r=1.0,
        glow=None, hi=0.95, lid_shadow=0.30, look=(0.0, 0.0), lash_w=0.36, flick=0.34, a=1.0):
    """An anime eye of w x h model pixels centred on (cx, cy) of a face (normally the front of the head). The outer corner is on the side away from the face's centre unless `outer` (-1 or +1).
    iris = (dark top, middle, light bottom); `glow` lights the iris on the glow map (a colour or True)."""
    if outer is None:
        outer = -1.0 if cx < face.w / 2.0 else 1.0
    f = face.f
    us = np.linspace(-1, 1, 25)
    # in the eye's own frame u runs from the inner corner (-1) to the outer corner (+1); the world x of u is cx + outer * u * w / 2
    def wx(u):
        return cx + outer * u * w / 2.0
    def wy_up(u):
        return cy + _lid(u, h, True) - tilt * u * (h / 2.0)
    def wy_lo(u):
        return cy + _lid(u, h, False) - tilt * u * (h / 2.0)
    up = [(float(wx(u)), float(wy_up(u))) for u in us]
    lo = [(float(wx(u)), float(wy_lo(u))) for u in us[::-1]]
    almond = face.m_poly(up + lo)
    # sclera: bright, a darker band under the upper lid
    scl = lin((cx, cy - h * 0.5), (cx, cy + h * 0.5), [(0, tone(sclera, 0.78)), (0.35, tone(sclera, 0.93)), (1, sclera)])
    face.fill(almond, scl, a)
    # iris: an ellipse, vertical gradient, a dark rim, shifted by `look` (in eye sizes)
    ix, iy = cx + look[0] * w * 0.25, cy + h * 0.04 + look[1] * h * 0.25
    irx, iry = w * 0.34 * iris_r, h * 0.50 * iris_r
    irism = face.m_ellipse(ix, iy, irx, iry) * almond
    face.fill(irism, lin((ix, iy - iry), (ix, iy + iry), [(0, iris[0]), (0.45, iris[1]), (1, iris[2])]), a)
    face.fill(irism * (1.0 - face.m_ellipse(ix, iy, irx * 0.80, iry * 0.80, soft=2.5)), iris[0], 0.55 * a)          # the darker limbal ring
    face.fill(face.m_ellipse(ix, iy + iry * 0.05, irx * 0.40, iry * 0.46) * almond, pupil, a)
    # the lid casts a soft shadow on the iris and sclera
    face.shade(almond * np.clip(1.0 - (face.Y - (cy - h * 0.5)) / (h * 0.55), 0, 1) ** 1.6, 1.0 - lid_shadow, a)
    if glow is not None:
        face.glow_fill(irism, iris[2] if glow is True else glow, 0.95)
    # highlights
    if hi > 0:
        face.ellipse(ix - irx * 0.36, iy - iry * 0.34, irx * 0.34, iry * 0.28, "#ffffff", a=hi * a, soft=1.5)
        face.ellipse(ix + irx * 0.42, iy + iry * 0.40, irx * 0.14, iry * 0.12, "#ffffff", a=hi * 0.75 * a, soft=1.5)
    # lashes: a thick upper lid line tapering to the inner corner, a flick at the outer corner, a thin lower line
    pts = [(wx(u), wy_up(u) - 0.02) for u in np.linspace(-1.0, 1.0, 14)]
    for i in range(len(pts) - 1):
        u0, u1 = -1.0 + 2.0 * i / (len(pts) - 1), -1.0 + 2.0 * (i + 1) / (len(pts) - 1)
        th0 = lash_w * (0.35 + 0.65 * (0.5 + 0.5 * u0)) * (1.0 - 0.0 * abs(u0))
        th1 = lash_w * (0.35 + 0.65 * (0.5 + 0.5 * u1))
        face.line(pts[i], pts[i + 1], lash, th0, th1, a=a)
    tip = (wx(1.0) + outer * flick, wy_up(1.0) - flick * 0.62)
    face.line(pts[-1], tip, lash, lash_w * 0.75, lash_w * 0.1, a=a)
    face.line(pts[-2], (tip[0] - outer * 0.05, tip[1] + 0.03), lash, lash_w * 0.5, lash_w * 0.2, a=a)
    low = [(wx(u), wy_lo(u)) for u in np.linspace(-0.9, 0.95, 10)]
    face.polyline(low, tone(lash, 1.6), 0.07, a=0.5 * a)
    return almond


def brow(face, p0, p1, color, w0=0.26, w1=0.12, arch=0.12, a=1.0):
    """An eyebrow from p0 (the inner end, thick) to p1 (the outer end, thin) bowed by `arch` (model pixels, positive = up)."""
    n = 10
    mid = ((p0[0] + p1[0]) / 2.0, (p0[1] + p1[1]) / 2.0 - arch)
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * mid[0] + t * t * p1[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * mid[1] + t * t * p1[1]
        pts.append((x, y))
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        face.line(pts[i], pts[i + 1], color, w0 + (w1 - w0) * t0, w0 + (w1 - w0) * t1, a=a)
    return face


def blush(face, cx, cy, rx, ry, color="#f08890", a=0.5):
    """A soft cheek blush: a radial fade from the colour at the centre to nothing."""
    m = face.m_ellipse(cx, cy, rx, ry, soft=1.0)
    d = np.sqrt(((face.X - cx) / rx) ** 2 + ((face.Y - cy) / ry) ** 2)
    face.fill(m * np.clip(1.0 - d, 0, 1) ** 0.8, color, a)
    return face


def mouth_smile(face, cx, cy, w, color="#a84a4a", lip="#d8847a", depth=0.28, thick=0.16, teeth=None, open_h=0.0, a=1.0):
    """A mouth: a curved smile line of width w (model pixels) bowing down by `depth`; open_h > 0 opens it (a dark mouth with optional teeth and a lip line)."""
    n = 12
    pts = []
    for i in range(n + 1):
        t = i / n
        u = t * 2 - 1
        pts.append((cx + u * w / 2.0, cy + depth * (1.0 - u * u)))
    if open_h > 0:
        lower = [(x, y + open_h * (1.0 - ((x - cx) / (w / 2.0)) ** 2) ** 0.8) for x, y in pts]
        shape = face.m_poly(pts + lower[::-1])
        face.fill(shape, tone(color, 0.42), a)
        if teeth:
            face.fill(face.m_poly(pts + [(x, y + open_h * 0.45 * (1.0 - ((x - cx) / (w / 2.0)) ** 2) ** 0.8) for x, y in pts[::-1]]) * shape, teeth, a)
        face.polyline(lower, lip, thick * 0.9, a=a * 0.9)
    face.polyline(pts, color, thick, a=a)
    face.polyline([(x, y + thick * 0.8) for x, y in pts[2:-2]], lip, thick * 0.55, a=0.55 * a)
    return face


def nose(face, cx, cy, color="#c8847a", size=0.26, a=0.8):
    """A tiny anime nose: a short shadow stroke and a light dot."""
    face.line((cx - 0.02, cy - size * 0.9), (cx + 0.03, cy), color, size * 0.22, size * 0.34, a=a * 0.6)
    face.ellipse(cx + 0.03, cy + 0.04, size * 0.38, size * 0.24, color, a=a)
    return face


# ================================================================================================ metal, trims and bands
def metal_paint(y0, y1, color="#e8b830", contrast=1.0, x=None):
    """A vertical 'reflection' gradient for polished metal / gold between two y values of a face: a bright top, a mid body, a dark horizon line, a warm bounce light at the bottom."""
    k = contrast
    return lin((x or 0.0, y0), (x or 0.0, y1), [(0.0, tone(color, 1.0 + 0.45 * k)), (0.42, color), (0.58, tone(color, 1.0 - 0.38 * k)), (0.85, tone(color, 1.0 - 0.12 * k)), (1.0, tone(color, 1.0 + 0.18 * k))])


def trim_line(face, pts, color="#e8b830", width=0.3, depth=None, spec=0.5, glow=None, mask=None, a=1.0, outline=0.5):
    """A bevelled metal or piping ribbon along a path: a dark outline, the metal gradient, a domed bevel with a specular streak. Returns its mask."""
    m = face.m_polyline(pts, width)
    if mask is not None:
        m = m * np.asarray(mask, np.float32)
    ys = [p[1] for p in pts]
    if outline > 0:
        face.fill(face.m_polyline(pts, width + 0.16) * (1.0 if mask is None else np.asarray(mask, np.float32)), tone(color, 0.42), outline * a)
    face.fill(m, metal_paint(min(ys) - width * 0.6, max(ys) + width * 0.6, color) if max(ys) - min(ys) > width else color, a, glow=glow)
    bevel(face, m, depth=depth if depth is not None else width * 0.5, gain=1.2, spec=spec, spec_pow=12, a=a)
    return m


def trim_band(face, y0, y1, color="#e8b830", x0=0.0, x1=None, r=0.0, spec=0.5, glow=None, mask=None, a=1.0, outline=0.55):
    """A horizontal bevelled band (a belt, a cuff, a hem trim) y0..y1 across x0..x1 (default the whole width). Returns its mask."""
    x1 = face.w if x1 is None else x1
    m = face.m_rect(x0, y0, x1, y1, r)
    if mask is not None:
        m = m * np.asarray(mask, np.float32)
    if outline > 0:
        face.fill(face.m_rect(x0 - 0.02, y0 - 0.08, x1 + 0.02, y1 + 0.08, r) * (1.0 if mask is None else np.asarray(mask, np.float32)), tone(color, 0.40), outline * a)
    face.fill(m, metal_paint(y0, y1, color), a, glow=glow)
    bevel(face, m, depth=max(0.12, (y1 - y0) * 0.45), gain=1.2, spec=spec, spec_pow=12, a=a)
    return m


def skin_face(face, skin, hair=None, hairline=2.0, side=0.07, chin=0.05, shadow=0.20, wave=0.35):
    """The base of a face on the front of the head: a soft skin gradient, a darker rim at both sides (the head's roundness), a chin shade, the hair roots at the top with a wavy hairline and the
    soft shadow the fringe throws on the forehead. skin = (shadow, base, light) tones or one colour; hair = a colour or None."""
    if isinstance(skin, (list, tuple)) and len(skin) == 3 and not isinstance(skin[0], (int, float)):
        sk_d, sk, sk_l = skin
    else:
        sk, sk_d, sk_l = skin, tone(skin, 0.82), tone(skin, 1.08)
    W, Hh = face.w, face.h
    face.fill(face.m_all(), lin((0, 0), (0, Hh), [(0, sk_l), (0.45, sk), (1, sk_d)]))
    xe = np.minimum(face.X, W - face.X)
    face.shade(face.m_all(), 1.0 - side * (1.0 - smoothstep(0.0, 1.1, xe)))
    face.shade(face.m_all(), 1.0 - chin * smoothstep(Hh - 1.4, Hh, face.Y))
    if hair is not None:
        wl = [(0.0, 0.0)]
        n = 8
        pts = [(W * i / n, hairline + wave * (0.5 - hsh(i, 5)) + (0.18 if i % 2 else -0.05)) for i in range(n + 1)]
        top = [(-0.2, -0.2), (W + 0.2, -0.2)] + pts[::-1]
        hm = face.m_poly(top, soft=1.0)
        face.fill(hm, lin((0, 0), (0, hairline + 0.4), [(0, tone(hair, 0.8)), (0.7, hair), (1, tone(hair, 1.12))]))
        face.fill(face.m_polyline(pts, 0.10) * hm, tone(hair, 0.6), 0.6)
        under = np.clip((face.Y - np.interp(face.X, [p[0] for p in pts], [p[1] for p in pts])) / 0.9, 0, 1)
        face.shade(face.m_all() * (1.0 - hm) * (1.0 - under) ** 1.6, 1.0 - shadow)
    return face


def cloth(face, mask, color, top=1.10, bottom=0.84, round_=0.10, fold_xs=None, fold_depth=0.07, fold_y0=0.0, fold_y1=None, weave_k=0.02, tint=None, a=1.0):
    """A clean HD cloth fill over a mask: a vertical light-to-shade gradient from `top` to `bottom` (tone factors of `color`), the side away from the light a little darker (`round_`),
    soft creases at `fold_xs` (model pixels) and a faint twill. This is what a big flat garment area should be painted with instead of keeping the blueprint's pixel mottling."""
    mask = np.asarray(mask, np.float32)
    ys = np.where(mask > 0.5)[0]
    y0 = face.Y[ys.min(), 0] - 0.5 / face.f if len(ys) else 0.0
    y1 = face.Y[ys.max(), 0] + 0.5 / face.f if len(ys) else face.h
    face.fill(mask, lin((0, y0), (0, y1), [(0, tone(color, top)), (0.55, color), (1, tone(color, bottom))]), a)
    away = face.X if face.light[0] > 0 else face.w - face.X                  # distance from the lit side
    face.shade(mask, 1.0 - round_ * smoothstep(0.45 * face.w, face.w, away), a)
    if fold_xs:
        folds(face, mask, fold_xs, depth=fold_depth, width=0.25, y0=fold_y0 if fold_y0 else y0, y1=fold_y1 if fold_y1 else y1)
    if weave_k:
        weave(face, mask, strength=weave_k)
    if tint is not None:
        face.fill(mask, tint, 0.12 * a, mode="over")
    return face


def heart_pts(cx, cy, w, n=48):
    """The outline of a heart (model pixels) of width w centred on (cx, cy)."""
    pts = []
    for i in range(n):
        t = math.tau * i / n
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((cx + x / 32.0 * w, cy + (y + 2.5) / 32.0 * w))
    return pts


def sparkle(face, cx, cy, r, color="#ffffff", a=0.9):
    """A four-point glint: two thin crossed diamonds with a soft core."""
    face.poly([(cx, cy - r), (cx + r * 0.14, cy), (cx, cy + r), (cx - r * 0.14, cy)], color, a)
    face.poly([(cx - r, cy), (cx, cy - r * 0.14), (cx + r, cy), (cx, cy + r * 0.14)], color, a)
    face.ellipse(cx, cy, r * 0.28, r * 0.28, color, a=a * 0.8, soft=2.0)
    return face
