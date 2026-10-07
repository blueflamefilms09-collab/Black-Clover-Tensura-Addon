#!/usr/bin/env python3
"""Briar Magic VFX sprites -> src/main/resources/assets/nusmp/textures/particle/briar_*.png

Look (after the owner's anime still): pale flat ribbon-straps with fine parallel grooves, a dark ink outline and small triangular
thorns along the edges, coiling into tangles; small blue roses. The sprites are grey-scale + alpha so the vertex colour tints them
(pale yellow for the straps, blue for the roses); the dark outline stays dark after tinting, like the cel linework of the still.

Writes: briar_strap (vertical strap, tiles along V), briar_band (the same, horizontal, for ring bands), briar_helix (a strap wound
round a column), briar_tangle (a mass of looping straps), briar_rose, briar_petal, briar_thorn, briar_sigil (rose / thorn mana array),
briar_burst (radial thorn star), briar_halo (soft ring), briar_pollen (glint).
Run: python3 -B tools/gen_briar_textures.py   (deterministic)
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4
OUTLINE = 52


class Canvas:
    """Value (V) and alpha (A) layers, drawn together at SS x resolution; V starts at the outline value so fringes read as ink."""

    def __init__(self, w, h, bg=OUTLINE):
        self.w, self.h = w, h
        self.v = Image.new("L", (w * SS, h * SS), bg)
        self.a = Image.new("L", (w * SS, h * SS), 0)
        self.dv, self.da = ImageDraw.Draw(self.v), ImageDraw.Draw(self.a)

    def poly(self, pts, v, a=255):
        pts = [(x * SS, y * SS) for x, y in pts]
        self.dv.polygon(pts, fill=int(max(0, min(255, v))))
        self.da.polygon(pts, fill=a)

    def line(self, pts, v, width, a=0):
        pts = [(x * SS, y * SS) for x, y in pts]
        self.dv.line(pts, fill=int(max(0, min(255, v))), width=max(1, int(width * SS)))
        if a:
            self.da.line(pts, fill=a, width=max(1, int(width * SS)))

    def ellipse(self, cx, cy, rx, ry, v, a=255):
        box = [(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS]
        self.dv.ellipse(box, fill=int(max(0, min(255, v))))
        self.da.ellipse(box, fill=a)

    def save(self, name, fade_edge=0.0):
        v = self.v.resize((self.w, self.h), Image.LANCZOS)
        a = self.a.resize((self.w, self.h), Image.LANCZOS)
        if fade_edge > 0:
            yy, xx = np.mgrid[0:self.h, 0:self.w]
            d = np.minimum(np.minimum(xx, self.w - 1 - xx) / self.w, np.minimum(yy, self.h - 1 - yy) / self.h)
            k = np.clip(d / fade_edge, 0, 1)
            k = k * k * (3 - 2 * k)
            a = Image.fromarray((np.asarray(a, dtype=np.float32) * k).astype(np.uint8))
        rgba = np.dstack([np.asarray(v)] * 3 + [np.asarray(a)]).astype(np.uint8)
        Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, name + ".png"))
        print("wrote", name, self.w, self.h)


def normals(pts):
    n = len(pts)
    out = []
    for i in range(n):
        p0 = pts[max(0, i - 1)]
        p1 = pts[min(n - 1, i + 1)]
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        L = math.hypot(dx, dy) or 1.0
        out.append((-dy / L, dx / L, dx / L, dy / L))     # nx, ny, tx, ty
    return out


def ribbon(cv, pts, width, grooves=5, thorn_every=0, thorn_h=0.0, shade=None, seg_ok=None, phase=0.0, twist=0.0, base=222, ol=1.4):
    """A flat strap along pts. width: number or list per point. shade: per-point multiplier. seg_ok(i): draw only some segments."""
    n = len(pts)
    ws = width if isinstance(width, (list, tuple)) else [width] * n
    sh = shade if shade is not None else [1.0] * n
    nr = normals(pts)

    def edge(i, f, extra=0.0):
        w = ws[i] * 0.5 * f
        return (pts[i][0] + nr[i][0] * (w + extra), pts[i][1] + nr[i][1] * (w + extra))

    segs = [i for i in range(n - 1) if seg_ok is None or seg_ok(i)]
    for i in segs:      # ink outline
        cv.poly([edge(i, 1, ol), edge(i + 1, 1, ol), edge(i + 1, -1, ol), edge(i, -1, ol)], OUTLINE)
    # thorns sit on the edge, under the body
    if thorn_every:
        k = 0
        for i in range(2, n - 2, thorn_every):
            if i not in segs:
                continue
            side = 1 if k % 2 == 0 else -1
            k += 1
            b0, b1 = edge(i - 1, side), edge(i + 2, side)
            tip = (pts[i][0] + nr[i][0] * side * (ws[i] * 0.5 + thorn_h) - nr[i][2] * thorn_h * 0.55,
                   pts[i][1] + nr[i][1] * side * (ws[i] * 0.5 + thorn_h) - nr[i][3] * thorn_h * 0.55)
            mid = (pts[i][0], pts[i][1])
            cv.poly([b0, tip, b1, mid], OUTLINE)
            ins = 0.8
            t2 = (tip[0] + (mid[0] - tip[0]) * 0.12, tip[1] + (mid[1] - tip[1]) * 0.12)
            cv.poly([(b0[0] + (mid[0] - b0[0]) * 0.2, b0[1] + (mid[1] - b0[1]) * 0.2), t2,
                     (b1[0] + (mid[0] - b1[0]) * 0.2, b1[1] + (mid[1] - b1[1]) * 0.2), mid], 205 * sh[i])
    for i in segs:      # body, brighter on one side to read as a flat strap twisting
        tw = 0.88 + 0.12 * math.cos(phase + twist * i)
        v = base * tw * (sh[i] + sh[i + 1]) * 0.5
        cv.poly([edge(i, 1), edge(i + 1, 1), edge(i + 1, -1), edge(i, -1)], v)
        # light edge band on the +side
        cv.poly([edge(i, 1), edge(i + 1, 1), edge(i + 1, 0.74), edge(i, 0.74)], min(255, v + 22))
    for g in range(1, grooves + 1):     # fine parallel grooves
        f = -1 + 2.0 * g / (grooves + 1)
        for i in segs:
            cv.line([edge(i, f), edge(i + 1, f)], 150 * (sh[i] + sh[i + 1]) * 0.5, 0.55)


def path_curve(fn, n):
    return [fn(i / (n - 1)) for i in range(n)]


def catmull(points, per=14):
    out = []
    P = [points[0]] + list(points) + [points[-1]]
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for s in range(per):
            t = s / per
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * ((2 * p1[k]) + (-p0[k] + p2[k]) * t + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * t2
                                   + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * t3) for k in (0, 1)))
    out.append(points[-1])
    return out


# ------------------------------------------------------------------------------------------------------------- sprites
def strap_img():
    """64 x 256 vertical strap, tiles along V (thorn spacing divides 256)."""
    cv = Canvas(64, 256)
    pts = [(32.0, y) for y in range(-8, 266, 2)]
    ribbon(cv, pts, 30, grooves=5, thorn_every=8, thorn_h=9.0)
    # wrap: duplicate the wrapped part so the tile is seamless
    return cv


def make_strap():
    cv = Canvas(64, 256)
    # draw 3 stacked copies shifted by 256 and keep the middle copy -> seamless
    big = Canvas(64, 256 * 3)
    pts = [(32.0, y) for y in range(-8, 256 * 3 + 8, 2)]
    ribbon(big, pts, 30, grooves=5, thorn_every=16, thorn_h=9.5)
    cv.v = big.v.crop((0, 256 * SS, 64 * SS, 512 * SS))
    cv.a = big.a.crop((0, 256 * SS, 64 * SS, 512 * SS))
    cv.dv, cv.da = ImageDraw.Draw(cv.v), ImageDraw.Draw(cv.a)
    return cv


def to_horizontal(cv, name):
    v = cv.v.resize((cv.w, cv.h), Image.LANCZOS).rotate(90, expand=True)
    a = cv.a.resize((cv.w, cv.h), Image.LANCZOS).rotate(90, expand=True)
    rgba = np.dstack([np.asarray(v)] * 3 + [np.asarray(a)]).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, name + ".png"))
    print("wrote", name)


def make_helix():
    """128 x 256: two straps wound round a column, 2 turns per tile (back halves dark, drawn first)."""
    cv = Canvas(128, 256)
    big = Canvas(128, 256 * 3)
    R, cx = 38.0, 64.0
    pitch = 128.0
    strands = [0.0, math.pi]
    paths = []
    for off in strands:
        pts, dep = [], []
        for k in range(0, 256 * 3 + 8, 2):
            ph = 2 * math.pi * (k - 8) / pitch + off
            pts.append((cx + R * math.sin(ph), float(k - 8)))
            dep.append(math.cos(ph))
        paths.append((pts, dep))
    for front in (False, True):
        for pts, dep in paths:
            sh = [1.0 if front else 0.55 for _ in pts]
            ok = (lambda i, d=dep, f=front: (d[i] >= 0) == f)
            ribbon(big, pts, 24, grooves=4, thorn_every=10, thorn_h=8.0, shade=sh, seg_ok=ok, twist=0.1)
    cv.v = big.v.crop((0, 256 * SS, 128 * SS, 512 * SS))
    cv.a = big.a.crop((0, 256 * SS, 128 * SS, 512 * SS))
    cv.dv, cv.da = ImageDraw.Draw(cv.v), ImageDraw.Draw(cv.a)
    return cv


def make_tangle():
    rnd = random.Random(417)
    cv = Canvas(256, 256)
    loops = []
    for k in range(15):
        cx, cy = rnd.uniform(60, 196), rnd.uniform(60, 196)
        rx, ry = rnd.uniform(34, 92), rnd.uniform(24, 76)
        turns = rnd.uniform(1.0, 1.9)
        a0 = rnd.uniform(0, 6.28)
        tilt = rnd.uniform(0, 3.14)
        drift = rnd.uniform(-30, 30)
        wid = rnd.uniform(15, 25)
        pts = []
        for i in range(70):
            t = i / 69.0
            th = a0 + t * turns * 2 * math.pi
            x, y = rx * math.cos(th) * (1 + 0.18 * t), ry * math.sin(th)
            xr = x * math.cos(tilt) - y * math.sin(tilt)
            yr = x * math.sin(tilt) + y * math.cos(tilt)
            pts.append((cx + xr + drift * (t - 0.5), cy + yr))
        loops.append((pts, wid))
    # a couple of long sweeping straps across
    for k in range(4):
        P = [(rnd.uniform(-10, 40), rnd.uniform(20, 236))] + [(rnd.uniform(40, 216), rnd.uniform(20, 236)) for _ in range(3)] + [(rnd.uniform(216, 266), rnd.uniform(20, 236))]
        loops.append((catmull(P, 16), rnd.uniform(18, 26)))
    rnd.shuffle(loops)
    for pts, wid in loops:
        w = [wid * (0.75 + 0.25 * math.sin(math.pi * i / (len(pts) - 1)) + 0.0) for i in range(len(pts))]
        sh = [0.72 + 0.28 * rnd.random() for _ in range(1)] * len(pts)
        ribbon(cv, pts, w, grooves=4, thorn_every=rnd.choice([9, 11, 13]), thorn_h=7.0, shade=sh, twist=rnd.uniform(0.05, 0.25), phase=rnd.uniform(0, 6))
    return cv


def petal_shape(cv, cx, cy, ang, length, width, v_in, v_out):
    """A rounded petal pointing along ang, drawn as nested outlines for a soft gradient."""
    def outline(k):
        pts = []
        for i in range(28):
            t = i / 27.0
            th = t * 2 * math.pi
            r_l = 0.5 * length * (1 + math.cos(th) * 0.0)
            x = math.cos(th) * 0.5 * length * k
            y = math.sin(th) * 0.5 * width * k * (1.0 - 0.28 * math.cos(th))
            xr = x + 0.5 * length
            pts.append((cx + xr * math.cos(ang) - y * math.sin(ang), cy + xr * math.sin(ang) + y * math.cos(ang)))
        return pts
    cv.poly(outline(1.07), OUTLINE)
    for j in range(6):
        k = 1.0 - j * 0.12
        cv.poly(outline(k), v_out + (v_in - v_out) * (j / 5.0) * -1 if False else v_out - (v_out - v_in) * (j / 5.0))
    # centre vein
    cv.line([(cx + 0.1 * length * math.cos(ang), cy + 0.1 * length * math.sin(ang)),
             (cx + 0.82 * length * math.cos(ang), cy + 0.82 * length * math.sin(ang))], v_in * 0.8, 0.7)


def make_rose():
    cv = Canvas(128, 128, bg=OUTLINE)
    c = 64.0
    ang0 = 0.3
    layers = [(5, 54, 40, 30, 205), (5, 42, 34, 24, 220), (5, 30, 27, 19, 232), (4, 19, 20, 14, 242)]
    for li, (n, dist, ln, wd, vout) in enumerate(layers):
        for k in range(n):
            ang = ang0 + li * 0.62 + k * 2 * math.pi / n
            cx, cy = c + 0.0 * math.cos(ang), c + 0.0 * math.sin(ang)
            petal_shape(cv, cx, cy, ang, dist * 1.05, wd * 1.5 + 4 * (4 - li), vout * 0.72, vout)
    # curled heart
    for k, r in enumerate([11, 8, 5, 2.5]):
        cv.ellipse(c + 1.5, c, r + 1.2, r + 1.2, OUTLINE)
        cv.ellipse(c + 1.5, c, r, r, 150 + 28 * k)
    cv.line([(c - 4, c + 2), (c + 2, c - 3), (c + 6, c + 3)], 110, 0.9)
    return cv


def make_petal():
    cv = Canvas(64, 64)
    petal_shape(cv, 14, 32, 0.0, 40, 22, 175, 238)
    return cv


def make_thorn():
    """64 x 128: one curved triangular thorn, base at the bottom, tip at the top (strut a = base, b = tip)."""
    cv = Canvas(64, 128)
    L = [(10, 126), (54, 126), (44, 80), (36, 40), (34, 4)]
    R = [(10, 126), (12, 90), (24, 50), (32, 4)]
    cv.poly([(8, 128), (56, 128), (46, 80), (37, 38), (34, 1), (30, 38), (20, 80), (6, 128)], OUTLINE)
    cv.poly([(12, 126), (52, 126), (42, 82), (35, 42), (33, 8), (29, 42), (21, 84), (14, 126)], 215)
    cv.poly([(34, 12), (35, 42), (42, 82), (52, 126), (44, 126), (38, 84), (33, 44)], 160)
    for k in range(5):
        y = 118 - k * 20
        cv.line([(30 - (y - 20) * 0.1, y), (38 + (y - 20) * 0.1 * 0.0, y)], 150, 0.6)
    return cv


def make_sigil():
    """256: the briar mana array. Outer thorn-vine ring, inner ring with roses, hexagram of straps, centre rose."""
    cv = Canvas(256, 256, bg=OUTLINE)
    c = 128.0
    def circle(r, n=140):
        return [(c + r * math.cos(2 * math.pi * i / (n - 1)), c + r * math.sin(2 * math.pi * i / (n - 1))) for i in range(n)]
    ribbon(cv, circle(118), 13, grooves=3, thorn_every=3, thorn_h=8.5)
    ribbon(cv, circle(90), 9, grooves=2, thorn_every=0)
    ribbon(cv, circle(70), 7, grooves=1)
    # hexagram of two triangles
    for rot in (0.0, math.pi / 3):
        P = [(c + 82 * math.cos(rot + math.pi / 2 + k * 2 * math.pi / 3), c + 82 * math.sin(rot + math.pi / 2 + k * 2 * math.pi / 3)) for k in range(4)]
        pts = []
        for k in range(3):
            for s in range(24):
                t = s / 24.0
                pts.append((P[k][0] + (P[k + 1][0] - P[k][0]) * t, P[k][1] + (P[k + 1][1] - P[k][1]) * t))
        pts.append(P[3])
        ribbon(cv, pts, 6, grooves=1, thorn_every=6, thorn_h=4.5)
    # small roses at the six points + centre
    def mini_rose(x, y, r):
        for li, (n, f, vv) in enumerate([(5, 1.0, 205), (5, 0.7, 225), (4, 0.42, 240)]):
            for k in range(n):
                a = li * 0.6 + k * 2 * math.pi / n
                petal_shape(cv, x, y, a, r * f * 1.05, r * f * 0.95, vv * 0.72, vv)
        cv.ellipse(x, y, r * 0.15, r * 0.15, 150)
    for k in range(6):
        a = math.pi / 2 + k * math.pi / 3
        mini_rose(c + 104 * math.cos(a) - 0, c + 104 * math.sin(a), 15)
    mini_rose(c, c, 30)
    return cv


def make_burst():
    """256 radial thorn star with soft heart; for flashes (ADD)."""
    img = np.zeros((256, 256), np.float32)
    yy, xx = np.mgrid[0:256, 0:256].astype(np.float32)
    dx, dy = xx - 127.5, yy - 127.5
    r = np.hypot(dx, dy) + 1e-3
    th = np.arctan2(dy, dx)
    spikes = np.zeros_like(r)
    rnd = random.Random(9)
    for k in range(14):
        a = k * 2 * math.pi / 14 + rnd.uniform(-0.08, 0.08)
        ln = 118 if k % 2 == 0 else 74
        d = np.abs(((th - a + math.pi) % (2 * math.pi)) - math.pi)
        w = 0.20 * np.clip(1 - r / ln, 0, 1) ** 1.3
        spikes = np.maximum(spikes, np.clip(1 - d / (w + 1e-4), 0, 1) ** 1.2 * np.clip(1 - r / ln, 0, 1) ** 0.5)
    core = np.exp(-(r / 26.0) ** 2)
    halo = np.exp(-(r / 62.0) ** 2) * 0.35
    img = np.clip(np.maximum(spikes, core) + halo, 0, 1)
    img *= np.clip((126 - r) / 20, 0, 1)
    v = (80 + 175 * img)
    out = np.dstack([v, v, v, img * 255]).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(os.path.join(OUT, "briar_burst.png"))
    print("wrote briar_burst")


def make_halo():
    yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
    r = np.hypot(xx - 63.5, yy - 63.5) / 63.5
    ring = np.exp(-((r - 0.82) / 0.07) ** 2) + 0.35 * np.exp(-((r - 0.62) / 0.03) ** 2) + 0.18 * np.exp(-((r - 0.9) / 0.2) ** 2)
    ring *= np.clip((1 - r) / 0.1, 0, 1)
    ring = np.clip(ring, 0, 1)
    out = np.dstack([255 * np.ones_like(ring)] * 3 + [ring * 255]).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(os.path.join(OUT, "briar_halo.png"))
    print("wrote briar_halo")


def make_pollen():
    yy, xx = np.mgrid[0:64, 0:64].astype(np.float32)
    dx, dy = xx - 31.5, yy - 31.5
    r = np.hypot(dx, dy)
    core = np.exp(-(r / 4.0) ** 2)
    cross = (np.exp(-(dx / 1.6) ** 2) * np.exp(-(np.abs(dy) / 15.0)) + np.exp(-(dy / 1.6) ** 2) * np.exp(-(np.abs(dx) / 15.0))) * 0.8
    soft = np.exp(-(r / 12.0) ** 2) * 0.5
    a = np.clip(core + cross + soft, 0, 1) * np.clip((31 - r) / 8, 0, 1)
    out = np.dstack([255 * np.ones_like(a)] * 3 + [a * 255]).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(os.path.join(OUT, "briar_pollen.png"))
    print("wrote briar_pollen")


def main():
    os.makedirs(OUT, exist_ok=True)
    s = make_strap()
    s.save("briar_strap")
    to_horizontal(s, "briar_band")
    make_helix().save("briar_helix")
    make_tangle().save("briar_tangle", fade_edge=0.06)
    make_rose().save("briar_rose")
    make_petal().save("briar_petal")
    make_thorn().save("briar_thorn")
    make_sigil().save("briar_sigil", fade_edge=0.01)
    make_burst()
    make_halo()
    make_pollen()


if __name__ == "__main__":
    main()
