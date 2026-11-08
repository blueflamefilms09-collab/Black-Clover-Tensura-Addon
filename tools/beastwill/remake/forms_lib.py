#!/usr/bin/env python3
"""Smooth 3D Beast Will forms (3.61.0): the geometry kit.
No stacked boxes. A form is made of CARDS (one picture quad each, drawn from both sides) laid along curves:
  sweep()  a tube / shell lofted along a path, radii per point (limbs, snouts, horns, tails, necks, pauldrons, ellipsoid blobs); the shading is chosen per card from its normal,
  blade()  a curved painted strip (feather, flame tongue, fur tuft, wing finger, membrane), the shape lives in the picture's alpha so the silhouette is round, not square.
All numbers are in the whole-body file frame (1/16 block, y up, feet at 0, front on -z, +x is the model's left), the frame of the other geo pieces (Noelle, Flame ...).
The same Card list feeds the preview renderer (forms_preview.py) and the geo export (forms_export.py), so what is previewed is what the game draws."""
import math, sys
import numpy as np
sys.dont_write_bytecode = True

V = lambda *a: np.array(a, dtype=np.float64)
LIGHT = np.array([0.38, 0.72, -0.58]); LIGHT /= np.linalg.norm(LIGHT)       # from above, in front, a little to the model's left
TONES = 5


def unit(v):
    v = np.asarray(v, dtype=np.float64); n = np.linalg.norm(v)
    return v / n if n > 1e-9 else V(0, 0, 1)


def hx(h): h = h.lstrip('#'); return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float64)
def mixc(a, b, t): return np.asarray(a, float) * (1 - t) + np.asarray(b, float) * t


class Card:
    """One picture quad. region = name in the sheet; win = (u0, v0, u1, v1) window of the region (fractions, v0 at the top); bone = the animated group; glow = drawn additive too."""
    __slots__ = ("c", "r", "u", "w", "h", "region", "win", "part", "bone", "solid", "tex")

    def __init__(s, c, r, u, w, h, region, win=None, part="body", bone="root", solid=None, tex=None):
        s.c, s.r, s.u, s.w, s.h, s.region, s.win, s.part, s.bone, s.solid, s.tex = V(*c), unit(r), unit(u), float(w), float(h), region, win, part, bone, solid, tex


# ====================================================================================================================== materials and the sheet
class Mat:
    """name; base/dark/light colours (0-255); pattern; glow colour+amount (0 = none); tex = size of its tone tiles."""

    def __init__(s, name, base, pattern="skin", dark=None, light=None, glow=None, glow_amt=0.0, seed=1, size=20):
        s.name = name; s.base = hx(base) if isinstance(base, str) else np.asarray(base, float)
        s.dark = hx(dark) if isinstance(dark, str) else (s.base * .38 if dark is None else np.asarray(dark, float))
        s.light = hx(light) if isinstance(light, str) else (mixc(s.base, [255, 255, 255], .55) if light is None else np.asarray(light, float))
        s.pattern, s.seed, s.size = pattern, seed, size
        s.glow = (hx(glow) if isinstance(glow, str) else np.asarray(glow, float)) if glow is not None else s.light
        s.glow_amt = glow_amt


def _noise(rng, w, h, sx, sy):
    """smooth value noise of cell sizes sx, sy (px), tiled in neither direction, 0..1"""
    gw, gh = int(w / sx) + 3, int(h / sy) + 3
    g = rng.random((gh, gw))
    ys = np.arange(h) / sy; xs = np.arange(w) / sx
    y0 = ys.astype(int); x0 = xs.astype(int); fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
    fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0] * (1 - fx) + g[y0][:, x0 + 1] * fx
    b = g[y0 + 1][:, x0] * (1 - fx) + g[y0 + 1][:, x0 + 1] * fx
    return a * (1 - fy) + b * fy


def pattern_field(mat, n):
    """luminance variation (-1..1) and glow mask (0..1) of the material's tile"""
    rng = np.random.default_rng(mat.seed * 7919 + 13)
    yy, xx = np.mgrid[0:n, 0:n].astype(float)
    p = mat.pattern
    if p == "fur":
        f = _noise(rng, n, n, 6, 1.4) * .55 + _noise(rng, n, n, 3, .8) * .45
        f = (f - .5) * 2.1
        streak = (_noise(rng, n, n, 1.1, 7) > .72) * .5
        L = f + streak; G = np.zeros((n, n))
    elif p == "scale":
        sw, sh = n / 4.0, n / 5.0
        row = np.floor(yy / sh); xo = xx + (row % 2) * sw / 2
        cx = (xo % sw) / sw - .5; cy = (yy % sh) / sh
        d = np.sqrt(cx * cx * 1.6 + (cy - .35) ** 2 * 1.1)
        L = np.where(cy > .78, -.9, 0) + (.5 - d) * 1.5 - .25 + (_noise(rng, n, n, 3, 3) - .5) * .5
        L = np.where((cy < .22) & (d < .5), L + .35, L); G = np.zeros((n, n))
    elif p == "plate":
        e = np.minimum(np.minimum(xx, n - 1 - xx), np.minimum(yy, n - 1 - yy))
        L = np.where(e < 1, np.where((xx + yy) < n, .85, -.95), 0.) + (_noise(rng, n, n, 8, 1.2) - .5) * .5
        L = L + ((xx - n / 2) ** 2 + (yy - n / 2) ** 2 < 2) * .5; G = np.zeros((n, n))
    elif p == "bark":
        f = _noise(rng, n, n, 2.0, 9); crack = (f < .30) * -1.0
        L = (f - .5) * 1.6 + crack + (_noise(rng, n, n, 5, 5) - .5) * .6; G = (f < .24) * .6
    elif p == "energy":
        w = np.sin((yy * .55 + _noise(rng, n, n, 5, 5) * 5)) * .5 + .5
        L = (w - .5) * 1.2; G = (w > .62) * 1.0
    elif p == "water":
        w = np.sin(yy * .8 + np.sin(xx * .45) * 2.2 + _noise(rng, n, n, 6, 6) * 4) * .5 + .5
        L = (w - .5) * 1.1; G = (w > .8) * .6
    elif p == "bone":
        f = _noise(rng, n, n, 5, 5); L = (f - .5) * 1.2 - (_noise(rng, n, n, 1.2, 8) > .8) * .9; G = np.zeros((n, n))
    elif p == "stone":
        f = _noise(rng, n, n, 4, 4) * .6 + _noise(rng, n, n, 1.7, 1.7) * .4; L = (f - .5) * 2.2; G = np.zeros((n, n))
    elif p == "cloth":
        L = (np.sin(xx * 1.6 + _noise(rng, n, n, 6, 6) * 2) * .25) + (_noise(rng, n, n, 3, 3) - .5) * .5; G = np.zeros((n, n))
    else:      # skin / chitin: soft mottling
        L = (_noise(rng, n, n, 5, 5) - .5) * 1.0 + (_noise(rng, n, n, 2, 2) - .5) * .4; G = np.zeros((n, n))
    return np.clip(L, -1, 1), np.clip(G, 0, 1)


TONE_K = (-.62, -.30, 0.0, .13, .28)         # dark -> light along dark/base/light


def tone_color(mat, k):
    t = TONE_K[k]
    return mixc(mat.base, mat.dark, -t / .62 * .85) if t < 0 else mixc(mat.base, mat.light, t * 1.6)


def paint_tone(mat, k):
    n = mat.size
    L, G = pattern_field(mat, n)
    col = tone_color(mat, k)[None, None, :] * (1 + L[..., None] * .26)
    out = np.zeros((n, n, 4)); out[..., :3] = np.clip(col, 0, 255); out[..., 3] = 255
    glow = np.zeros((n, n, 4))
    if mat.glow_amt > 0:
        g = np.clip(mat.glow_amt * (.25 + G * .9) * (0.55 + k * .15), 0, 1)
        glow[..., :3] = mat.glow[None, None, :] * g[..., None]; glow[..., 3] = 255 * (g > .02)
    return out, glow


# ---------------------------------------------------------------- silhouettes (alpha lives in the picture)
def _aa(d, soft=1.0): return np.clip(.5 + d / soft, 0, 1)


def paint_shape(kind, mat, w, h, accent=None, seed=1):
    """kind: leaf | fang | flame | feather | tuft | membrane | disc | halo | claw | frond ; v=0 is the tip. Returns color, glow arrays (h, w, 4)."""
    rng = np.random.default_rng(seed * 31 + 5)
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    v = (yy + .5) / h                       # 0 tip .. 1 base
    u = ((xx + .5) / w - .5) * 2            # -1..1
    acc = mat.light if accent is None else accent
    hw = np.zeros_like(v)                   # half width profile 0..1
    if kind == "leaf":
        hw = np.sin(np.pi * np.clip(v * .95 + .02, 0, 1) ** .72) * .95 * (v > .0)
        hw = np.where(v > .9, hw * (1 - (v - .9) * 6), hw)
    elif kind == "fang":
        hw = np.clip(v * 1.15, 0, 1) ** .85 * (1 - np.clip((v - .85) / .15, 0, 1) * .55) * .92
    elif kind == "claw":
        hw = np.clip(v, 0, 1) ** .6 * .9 * (1 - np.clip((v - .8) / .2, 0, 1) * .5)
    elif kind == "flame":
        wob = np.sin(v * 9.0 + seed) * .08 * v + np.sin(v * 5.0 + seed * 2) * .06 * v
        hw = np.sin(np.pi * np.clip(v ** .8, 0, 1)) * (.85 - .15 * v) + 0
        u = u - wob * 2.2
        hw = np.where(v < .04, 0, hw)
    elif kind == "feather":
        hw = np.sin(np.pi * np.clip(v * .97 + .015, 0, 1) ** .62) * .92
        hw = np.where(v > .93, hw * (1 - (v - .93) * 14) , hw)
        hw = np.maximum(hw, 0)
    elif kind == "tuft":
        lobes = 3
        base = np.clip(v * 1.05, 0, 1) ** .7
        saw = (np.abs(((u * lobes * .7 + v * 1.4) % 1) - .5) * 2)       # jagged edge: v-dependent teeth
        hw = base * (.78 + .22 * saw) * (1 - np.clip((v - .9) / .1, 0, 1) * .6)
    elif kind == "frond":
        hw = np.sin(np.pi * np.clip(v ** .85, 0, 1)) ** .8 * .95
        hw = hw * (.82 + .18 * np.sin(v * 38 + seed))
    elif kind == "membrane":
        # a wing panel: straight leading edge at the left, scalloped trailing edge below, three finger bones
        hw = np.ones_like(v)
    elif kind == "disc":
        r = np.sqrt(u * u + ((v - .5) * 2) ** 2)
        hw = np.where(r < 1, 1.0, 0.0)
    elif kind == "halo":
        r = np.sqrt(u * u + ((v - .5) * 2) ** 2)
        hw = np.where((r < 1) & (r > .72), 1.0, 0.0)
    if kind == "membrane":
        # u: -1 (root edge) .. +1 (tip edge); v: 0 top (leading bone) .. 1 bottom (trailing edge)
        tr = .55 + .45 * (1 - (u * .5 + .5))               # the trailing edge rises toward the tip
        scallops = .07 * np.abs(np.sin((u * .5 + .5) * 3 * np.pi))
        a = _aa((tr - scallops - v) * h, 1.2) * _aa((1 - np.abs(u)) * w * .5, 1.0)
        d_bone = np.minimum.reduce([np.abs(v - (u * .5 + .5) * .5 - .02) * h for _ in range(1)])
        fing = np.zeros_like(v)
        for f in (.25, .55, .85):
            line = (v - f * tr * (0.2 + 0.8 * (1 - (u * .5 + .5)) + .15))
        spine_d = np.minimum(np.abs(v - .06) * h, 99)
        col = mat.base[None, None, :] * (.72 + .28 * (1 - v))[..., None]
        col = col * (1 + .0 * v[..., None])
        bones = np.zeros_like(v)
        for f in (.0, .33, .66, 1.0):               # three/four finger bones fanning from the root edge to the trailing edge
            x0, y0 = -1.0, 0.06; x1, y1 = 1.0, 0.06 + f * (tr.mean() * 0.0 + .55)
            # distance to the fan line (root -> (1, y1))
            dx, dy = x1 - x0, (y1 - y0)
            dd = np.abs((u - x0) * dy * (h / 2) - (v - y0) * dx * (w / 2) * 1) / math.hypot(dx * w / 2, dy * h)
            bones = np.maximum(bones, _aa(.9 - dd, .8))
        col = np.where(bones[..., None] > .5, mat.dark[None, None, :] * 1.1, col)
        col = np.clip(col * (1 + .15 * (_noise(rng, w, h, 4, 4)[..., None] - .5)), 0, 255)
        out = np.zeros((h, w, 4)); out[..., :3] = col; out[..., 3] = 255 * a * (.92 - .0 * v)
        glow = np.zeros((h, w, 4))
        g = np.clip(mat.glow_amt * (.15 + bones * .8 + (1 - v) * .0), 0, 1) * a
        glow[..., :3] = mat.glow[None, None, :] * g[..., None]; glow[..., 3] = 255 * (g > .02)
        return out, glow
    edge = hw - np.abs(u)                                        # >0 inside
    a = _aa(edge * w * .5, 1.1) * (v >= 0)
    # shading across the shape: lit on the left edge, dark on the right, bright rib
    side = np.clip(u * .5 + .5, 0, 1)
    along = v
    k = {"flame": 0, "feather": 0, "leaf": 0, "tuft": 0, "fang": 0, "claw": 0, "frond": 0, "disc": 0, "halo": 0}.get(kind, 0)
    base = mixc(mat.base, mat.dark, 0)
    col = np.zeros((h, w, 3))
    if kind == "flame":
        t = np.clip(1 - np.abs(u) / np.maximum(hw, .05), 0, 1) * (1 - v * .55)              # hot in the middle, near the base
        col = mixc(mat.dark, mat.base, np.clip(1 - v * 1.1, 0, 1))[None, None, :] * np.ones((h, w, 1))
        col = col * (1 - t[..., None]) + mixc(mat.base, mat.light, .9)[None, None, :] * t[..., None]
        col = mixc(col, acc, np.clip(t * 1.2 - .35, 0, 1)[..., None])
        a = a * np.clip(1.15 - (1 - v) * .0, 0, 1) * np.clip(.35 + (v) * 1.4, 0, 1) ** .6                  # the tip fades
        a = np.where(v < .06, a * v / .06, a)
        g = np.clip(mat.glow_amt * (.55 + t * .6), 0, 1) * a
    else:
        rib = _aa(.9 - np.abs(u) * w * .5, 1.0) * (hw > 0) if kind in ("feather", "leaf", "frond") else 0 * u
        lum = (.62 + .55 * (1 - side)) * (.88 + .12 * along)
        if kind in ("feather", "tuft", "frond"):
            barbs = (np.sin((v * h * 1.1 + np.abs(u) * w * .9) * 1.5) > .35) * .12
            lum = lum + barbs * (kind == "feather") + (_noise(rng, w, h, 1.2, 4) - .5) * .25
        if kind in ("fang", "claw"):
            lum = (.5 + .7 * (1 - side) ** 1.2) * (.8 + .3 * (1 - along))
        col = mixc(mat.dark, mat.light, np.clip(lum - .35, 0, 1))[..., None, :] if False else None
        t = np.clip(lum - .3, 0, 1.15)[..., None]
        col = mat.dark[None, None, :] * (1 - np.minimum(t, 1)) + mixc(mat.base, mat.light, np.clip(t - 1, 0, 1) * 4 + .0)[None, None, :] * np.minimum(t, 1)
        col = np.where(((kind in ("feather", "leaf", "frond")) and True) & (np.abs(u)[..., None] < .08), mat.light[None, None, :], col)
        col = np.clip(col, 0, 255)
        # the tip catches the accent colour
        tipk = np.clip(1 - v * 4.0, 0, 1)[..., None] * (.9 if kind in ("fang", "claw", "leaf", "tuft") else .5)
        col = col * (1 - tipk) + acc[None, None, :] * tipk
        g = np.clip(mat.glow_amt * (.1 + tipk[..., 0] * .9), 0, 1) * a
    out = np.zeros((h, w, 4)); out[..., :3] = np.clip(col, 0, 255); out[..., 3] = 255 * a
    glow = np.zeros((h, w, 4)); glow[..., :3] = mat.glow[None, None, :] * g[..., None]; glow[..., 3] = 255 * (g > .02)
    return out, glow


# ---------------------------------------------------------------- the sheet
class Sheet:
    """A per-will texture sheet: the tone tiles of its materials and its silhouette pictures, packed in shelves."""

    def __init__(s, W=256, H=256):
        s.W, s.H = W, H
        s.color = np.zeros((H, W, 4)); s.glow = np.zeros((H, W, 4))
        s.reg = {}; s._x = s._y = s._row = 0

    def put(s, name, col, glow):
        if name in s.reg: return s.reg[name]
        h, w = col.shape[:2]
        if s._x + w > s.W: s._x, s._y, s._row = 0, s._y + s._row, 0
        if s._y + h > s.H: raise ValueError("sheet full at " + name)
        x, y = s._x, s._y; s._x += w; s._row = max(s._row, h)
        s.color[y:y + h, x:x + w] = col; s.glow[y:y + h, x:x + w] = glow
        s.reg[name] = (x, y, w, h)
        return s.reg[name]

    def mat(s, m):
        for k in range(TONES):
            n = "t_%s_%d" % (m.name, k)
            if n not in s.reg: s.put(n, *paint_tone(m, k))

    def shape(s, name, kind, m, w, h, accent=None, seed=1):
        n = "s_%s" % name
        if n not in s.reg: s.put(n, *paint_shape(kind, m, w, h, accent, seed))
        return n

    def sample(s, region, u, v, win=None):
        x, y, w, h = s.reg[region]
        if win:
            u0, v0, u1, v1 = win; u = u0 + u * (u1 - u0); v = v0 + v * (v1 - v0)
        px = np.clip((x + u * w).astype(int), 0, s.W - 1); py = np.clip((y + v * h).astype(int), 0, s.H - 1)
        return s.color[py, px], s.glow[py, px]


# ====================================================================================================================== geometry
def _frames(path, ref=None):
    """parallel-transported (T, N, B) frames along a path"""
    P = [V(*p) for p in path]; n = len(P)
    T = []
    for i in range(n):
        a = P[max(i - 1, 0)]; b = P[min(i + 1, n - 1)]
        T.append(unit(b - a))
    r = V(0, 1, 0) if ref is None else unit(ref)
    if abs(np.dot(r, T[0])) > .95: r = V(1, 0, 0)
    N0 = unit(r - T[0] * np.dot(r, T[0]))
    Ns = [N0]
    for i in range(1, n):
        prev = Ns[-1]
        Nn = prev - T[i] * np.dot(prev, T[i])
        Ns.append(unit(Nn) if np.linalg.norm(Nn) > 1e-6 else prev)
    Bs = [np.cross(T[i], Ns[i]) for i in range(n)]
    return P, T, Ns, Bs


def _tone_of(normal, view_flip=False):
    d = float(np.dot(unit(normal), LIGHT))
    k = int(np.clip(round((d + .55) / 1.55 * (TONES - 1)), 0, TONES - 1))
    return k


def sweep(cards, sheet, part, path, rx, rz=None, sides=8, mat=None, arc=(0.0, 360.0), ref=None, bone="root", cap=True, overlap=1.12, jitter=1):
    """A tube/shell: `path` points, radii rx (and rz) per point (a number or list). arc in degrees starting at the N axis of the frame (ref = which way N points)."""
    sheet.mat(mat)
    P, T, N, B = _frames(path, ref)
    n = len(P)
    rx = [rx] * n if np.isscalar(rx) else list(rx)
    rz = rx if rz is None else ([rz] * n if np.isscalar(rz) else list(rz))
    a0, a1 = arc; closed = abs((a1 - a0) - 360) < 1e-6
    angs = [math.radians(a0 + (a1 - a0) * j / sides) for j in range(sides + 1)]

    def ring(i):
        return [P[i] + N[i] * math.cos(a) * rx[i] + B[i] * math.sin(a) * rz[i] for a in angs]
    rings = [ring(i) for i in range(n)]
    cnt = 0
    for i in range(n - 1):
        for j in range(sides):
            A, Bq, C, D = rings[i][j], rings[i][j + 1], rings[i + 1][j + 1], rings[i + 1][j]
            w1, w2 = np.linalg.norm(Bq - A), np.linalg.norm(C - D)
            if w1 + w2 < 1e-3 or np.linalg.norm(D - A) + np.linalg.norm(C - Bq) < 1e-3: continue
            cen = (A + Bq + C + D) / 4
            right = (Bq - A + C - D)
            up = (D - A + C - Bq)
            h = (np.linalg.norm(D - A) + np.linalg.norm(C - Bq)) / 2
            w = (w1 + w2) / 2
            am = (angs[j] + angs[j + 1]) / 2
            nrm = (N[i] + N[i + 1]) / 2 * math.cos(am) * 1.0 + (B[i] + B[i + 1]) / 2 * math.sin(am) * 1.0
            # the normal of an elliptic section: scale by the inverse radii
            k = _tone_of(nrm)
            cnt += 1
            win = None
            if jitter:
                ox = ((cnt * 37 + i * 11) % 7) / 7 * .3; oy = ((cnt * 53 + j * 5) % 7) / 7 * .3
                win = (ox, oy, ox + .7, oy + .7)
            cards.append(Card(cen, right, up, max(w * overlap, .35), max(h * overlap, .35), "t_%s_%d" % (mat.name, k), win, part, bone))
    if cap and closed:
        pass
    return cards


def blade(cards, sheet, part, shape, base, tip, side, width, bend=(0, 0, 0), segs=3, bone="root", twist=0.0, flip=False):
    """A curved painted strip from `base` to `tip`; `side` is the direction in which the picture's width runs (at the base); `bend` pushes the middle control point."""
    base, tip = V(*base), V(*tip); bend = V(*bend)
    mid = (base + tip) / 2 + bend
    pts = []
    for i in range(segs + 1):
        t = i / segs
        pts.append((1 - t) ** 2 * base + 2 * t * (1 - t) * mid + t * t * tip)
    sd = unit(side)
    for i in range(segs):
        a, b = pts[i], pts[i + 1]
        up = unit(b - a)
        right = sd - up * np.dot(sd, up)
        if twist:
            c, s = math.cos(math.radians(twist * (i + .5) / segs)), math.sin(math.radians(twist * (i + .5) / segs))
            nrm = np.cross(right, up)
            right = right * c + nrm * s
        if np.linalg.norm(right) < 1e-6: right = V(1, 0, 0)
        v0 = 1 - (i + 1) / segs; v1 = 1 - i / segs
        cards.append(Card((a + b) / 2, right, up, width, np.linalg.norm(b - a) * 1.04, "s_" + shape, (0, v0, 1, v1), part, bone))
    return pts


def membrane_card(cards, sheet, part, shape, p_root, p_tip, p_low, bone="root", segs=1):
    """A wing panel as one or two slanted cards between a leading edge (p_root->p_tip) and a lower corner p_low."""
    p_root, p_tip, p_low = V(*p_root), V(*p_tip), V(*p_low)
    cen = (p_root + p_tip + p_low) / 3 * .5 + (p_tip + p_low) / 2 * .5
    right = p_tip - p_root
    up = (p_low - (p_root + p_tip) / 2)
    cards.append(Card((p_root + p_tip) / 2 * .5 + (p_low + (p_root + p_tip) / 2) / 2 * .5 - (p_root + p_tip) / 2 * .0, right, -up, np.linalg.norm(right), np.linalg.norm(up) * 1.0, "s_" + shape, None, part, bone))
