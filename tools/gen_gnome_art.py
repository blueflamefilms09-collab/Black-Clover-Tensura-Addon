"""The hand-painted sheet of the Gnome (the Earth Spirit of Black Clover, docs/magic/gnome.md), in the CEL look of the series: FLAT tones (no gradients), a dark thin ink line round every facet
and every crack, a lit and a shaded side on each stone, flat olive moss with an ink edge, amber crystal veins and a glowing magic circle. Deterministic (every painter is seeded), so the
sheet can be redone. Used by tools/gen_gnome_geo.py.

Every painter returns (colour RGBA, glow RGBA) float arrays of its size: the colour goes to the model, the glow to the glow-only model (drawn additive, so nothing may carry colour under a
transparent pixel). Palette of the earth: warm brown stone, amber crystal and glow, olive moss, near-black brown ink; a white-blue "diamond" crystal is the nod to the Diamond Kingdom (its
host, Feremena, is one of the Eight Shining Generals).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gnome_geo_lib import rgb, smoothstep, fbm  # noqa: E402

SS = 3

PAL = dict(ink="#1F130C", ink2="#34200F",
           shade="#4A3D35", dark="#66543F", base="#8F7C66", light="#BBA58A", hi="#E0CDA8",
           warm="#B27A3E",                                   # a few stones are warmer (ochre)
           moss_d="#34421A", moss="#566A28", moss_l="#7C9036",
           amber_d="#9A5A16", amber="#F0A52A", amber_l="#FFD35C", amber_w="#FFF2B8",
           glow="#FFB43C", glow_core="#FFE9A0",
           dia_d="#5E8E9E", dia="#A6D6E2", dia_l="#E4F7FB", dia_w="#FFFFFF")


def col(h, a=255):
    c = rgb(h)
    return int(c[0]), int(c[1]), int(c[2]), a


def canvas(w, h):
    return Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))


def done(img, w, h):
    return np.asarray(img.resize((w, h), Image.LANCZOS), dtype=np.float32)


def over(base, layer):
    a = layer[..., 3:4] / 255.0
    ba = base[..., 3:4] / 255.0
    oa = a + ba * (1 - a)
    rgbo = np.where(oa > 1e-5, (layer[..., :3] * a + base[..., :3] * ba * (1 - a)) / np.maximum(oa, 1e-5), 0.0)
    return np.concatenate([rgbo, oa * 255.0], axis=-1)


def solid(w, h, c):
    out = np.zeros((h, w, 4), dtype=np.float32)
    out[..., :3] = rgb(c)
    out[..., 3] = 255
    return out


def clear(w, h):
    return np.zeros((h, w, 4), dtype=np.float32)


def flat(mask, c):
    """A flat layer of colour c where mask > 0.5 (cel: no soft edge except the one texel of the resize)."""
    out = np.zeros(mask.shape + (4,), dtype=np.float32)
    out[..., :3] = rgb(c)
    out[..., 3] = np.clip(mask, 0, 1) * 255.0
    return out


# ------------------------------------------------------------------------------------------------------------------ facets
def voronoi(w, h, cell, seed, jitter=0.82, stretch=1.0):
    """Jittered-grid Voronoi: returns d1, d2 (distance to the nearest and second nearest feature point, in texels), the nearest point (cx, cy) and a hash of it in 0..1. Wraps in x and y."""
    rng = np.random.default_rng(seed)
    nx, ny = max(2, int(round(w / cell))), max(2, int(round(h / (cell * stretch))))
    px = (rng.random((ny, nx)) - 0.5) * jitter
    py = (rng.random((ny, nx)) - 0.5) * jitter
    hs = rng.random((ny, nx))
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    gx = xx / (w / nx)
    gy = yy / (h / ny)
    ix, iy = np.floor(gx).astype(int), np.floor(gy).astype(int)
    d1 = np.full((h, w), 1e9, np.float32)
    d2 = np.full((h, w), 1e9, np.float32)
    cx = np.zeros((h, w), np.float32)
    cy = np.zeros((h, w), np.float32)
    hh = np.zeros((h, w), np.float32)
    for dj in (-1, 0, 1):
        for di in (-1, 0, 1):
            jx, jy = (ix + di), (iy + dj)
            kx, ky = jx % nx, jy % ny
            fx = (jx + 0.5 + px[ky, kx]) * (w / nx)
            fy = (jy + 0.5 + py[ky, kx]) * (h / ny)
            dd = np.hypot(xx - fx, yy - fy)
            better = dd < d1
            d2 = np.where(better, d1, np.minimum(d2, dd))
            cx = np.where(better, fx, cx)
            cy = np.where(better, fy, cy)
            hh = np.where(better, hs[ky, kx], hh)
            d1 = np.where(better, dd, d1)
    return d1, d2, cx, cy, hh


def stone(w, h, seed, cell, tone=0.0, warm=0.0, line=2.0, shade_side=0.26, moss=0.0, moss_top=True, cracks=0, vein=0, light=(-0.55, -0.80), flat_tone=None):
    """A cel-shaded rock face: Voronoi facets, each a FLAT tone (shade / dark / base / light / highlight) from where it sits in the light and a hash of its own; a shaded side and a lit edge on
    every facet; an ink line along every facet border; optional flat olive moss, ink cracks and amber veins. tone shifts the whole face lighter (+) or darker (-). Returns colour, glow."""
    rng = np.random.default_rng(seed * 7 + 1)
    d1, d2, cx, cy, hh = voronoi(w, h, cell, seed, stretch=0.9)
    # where the facet sits: light from the upper left, a little lighter near the top
    lit = 0.57 + tone + (0.5 - cy / h) * 0.22 + (0.5 - cx / w) * 0.10 + (hh - 0.5) * 0.55
    if flat_tone is not None:
        lit = np.full_like(lit, flat_tone) + (hh - 0.5) * 0.35
    # inside each facet: a shaded lower-right side and a lit upper-left rim (the cel step)
    lx, ly = light
    loc = ((np.mgrid[0:h, 0:w][1] - cx) * lx + (np.mgrid[0:h, 0:w][0] - cy) * ly) / (cell * 0.5)        # -1 toward the light, +1 away
    lit = lit - np.where(loc > -0.20 + (1.0 - shade_side * 2.6), 0.30, 0.0) + np.where(loc < -0.62, 0.20, 0.0)
    cols = [rgb(PAL["shade"]), rgb(PAL["dark"]), rgb(PAL["base"]), rgb(PAL["light"]), rgb(PAL["hi"])]
    th = [0.18, 0.40, 0.66, 0.90]
    idx = np.zeros((h, w), int)
    for t in th:
        idx += (lit > t).astype(int)
    c = np.zeros((h, w, 3), np.float32)
    for k in range(5):
        c = np.where((idx == k)[..., None], cols[k], c)
    if warm > 0:                                                         # some facets are ochre: a flat swap, never a blend
        wm = (hh * 7.31 % 1.0) < warm
        wc = rgb(PAL["warm"])
        c = np.where((wm & (idx >= 2))[..., None], np.where((idx == 3)[..., None], wc * 1.16, wc), c)
    # manga hatching: parallel ink strokes on the shaded facets (the pen-and-ink shading of the series)
    gy, gx = np.mgrid[0:h, 0:w]
    stroke = ((gx + gy) % 6) < 1.25
    hatch = stroke & ((idx == 0) | ((idx == 1) & (loc > 0.05)))
    c = np.where(hatch[..., None], c * 0.52 + rgb(PAL["ink"]) * 0.48, c)
    # small flat chips: a darker speck here and there, as hand-inked texture
    n = fbm(rng, w, h, 3, max(3, int(w / 22)))
    chip = (n > 0.70) & (idx >= 2)
    c = np.where(chip[..., None], c * 0.82, c)
    out = np.concatenate([c, np.full((h, w, 1), 255.0, np.float32)], axis=-1)
    glow = clear(w, h)
    if moss > 0:
        mn = fbm(rng, w, h, 4, max(3, int(w / 40)))
        top = (1.0 - np.mgrid[0:h, 0:w][0] / h) ** 0.7 if moss_top else np.full((h, w), 0.6)
        m = (mn * 0.75 + top * 0.35 * moss * 1.4 + (hh - 0.5) * 0.2) > (1.02 - 0.55 * moss)
        m = m & (d2 - d1 > line * 1.6)
        edge = m & ~(np.roll(m, 2, 0) & np.roll(m, -2, 0) & np.roll(m, 2, 1) & np.roll(m, -2, 1))
        lit_m = (loc < 0.0)
        mc = np.where(lit_m[..., None], rgb(PAL["moss_l"]), rgb(PAL["moss"]))
        mc = np.where(((d1 / (cell * 0.5)) > 0.75)[..., None] & ~lit_m[..., None], rgb(PAL["moss_d"]), mc)
        out[..., :3] = np.where(m[..., None], mc, out[..., :3])
        out[..., :3] = np.where(edge[..., None], rgb(PAL["moss_d"]), out[..., :3])
    # the ink line of the facet borders
    seam = ((d2 - d1) * 0.5) < (line * 0.5)
    out[..., :3] = np.where(seam[..., None], rgb(PAL["ink"]), out[..., :3])
    # cracks and amber veins
    if cracks or vein:
        lay, glw = crack_layers(w, h, rng, cracks, vein)
        out = over(out, lay)
        glow = over(glow, glw)
    return out, glow


def crack_layers(w, h, rng, n_ink, n_vein):
    """Random-walk polylines: ink cracks (colour only) and amber veins (a dark amber line in the colour, the glow in the glow sheet: a wide soft amber line and a thin white-hot core)."""
    ink, veinc, glw1, glw2 = canvas(w, h), canvas(w, h), canvas(w, h), canvas(w, h)
    di, dv, dg1, dg2 = ImageDraw.Draw(ink), ImageDraw.Draw(veinc), ImageDraw.Draw(glw1), ImageDraw.Draw(glw2)

    def walk(n):
        x, y = rng.random() * w, rng.random() * h
        a = rng.random() * math.tau
        pts = [(x, y)]
        for _ in range(n):
            a += (rng.random() - 0.5) * 1.5
            x += math.cos(a) * (h * 0.05 + 2.0)
            y += math.sin(a) * (h * 0.05 + 2.0)
            pts.append((x, y))
        return pts

    for _ in range(n_ink):
        p = walk(int(5 + rng.random() * 6))
        di.line([(x * SS, y * SS) for x, y in p], fill=col(PAL["ink"]), width=int(1.5 * SS), joint="curve")
    for _ in range(n_vein):
        p = walk(int(7 + rng.random() * 8))
        pp = [(x * SS, y * SS) for x, y in p]
        dv.line(pp, fill=col(PAL["ink"]), width=int(3.0 * SS), joint="curve")
        dv.line(pp, fill=col(PAL["amber_d"]), width=int(1.6 * SS), joint="curve")
        dg1.line(pp, fill=col(PAL["glow"], 255), width=int(3.4 * SS), joint="curve")
        dg2.line(pp, fill=col(PAL["glow_core"], 255), width=int(1.2 * SS), joint="curve")
    lay = over(done(ink, w, h), done(veinc, w, h))
    glw = over(done(glw1, w, h), done(glw2, w, h))
    return lay, glw


# ------------------------------------------------------------------------------------------------------------------ crystals
def crystal(w, h, seed, pal="amber"):
    """A crystal shard's side: vertical facet stripes, each a FLAT tone (dark, base, light, white-hot ridge), an ink outline on the left and right edge and between the facets, a pale tip.
    Colour is opaque everywhere (the cube is the shard). The glow sheet gets a flat amber fill (the crystal glows from within) with a bright white-hot ridge."""
    rng = np.random.default_rng(seed)
    d, b, l, wt = (PAL["amber_d"], PAL["amber"], PAL["amber_l"], PAL["amber_w"]) if pal == "amber" else (PAL["dia_d"], PAL["dia"], PAL["dia_l"], PAL["dia_w"])
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    k = 4
    edges = np.sort(np.concatenate([[0.0], rng.uniform(0.12, 0.88, k - 1), [1.0]]))
    fid = np.zeros((h, w), int)
    fx = xx / w
    for e in edges[1:-1]:
        fid += (fx > e).astype(int)
    tones = [rgb(d), rgb(b), rgb(l), rgb(b), rgb(d)]
    order = [1, 2, 3, 0]
    c = np.zeros((h, w, 3), np.float32)
    for i in range(k):
        t = tones[order[i % 4] if i < 4 else 1]
        c = np.where((fid == i)[..., None], t, c)
    # lower part of each shard darker (rooted in the rock), top lighter: two flat steps
    v = yy / h
    c = np.where((v > 0.82)[..., None], c * 0.72, c)
    c = np.where((v < 0.14)[..., None], np.where((c.sum(-1) > 1)[..., None], rgb(wt) * 0.55 + c * 0.45, c), c)
    seam = np.zeros((h, w), bool)
    for e in edges[1:-1]:
        seam |= np.abs(fx - e) < (0.9 / w)
    for k0 in (0.30, 0.62):                                           # the diagonal cuts of a gem across the stripes
        seam |= np.abs((yy / h) - k0 - 0.28 * (fx - 0.5)) < (0.8 / h)
    seam |= (xx < 1.2) | (xx > w - 2.2)
    seam |= (yy > h - 1.6)
    c = np.where(seam[..., None], rgb(PAL["ink"]), c)
    out = np.concatenate([c, np.full((h, w, 1), 255.0, np.float32)], axis=-1)
    g = clear(w, h)
    inner = (~seam) & (v < 0.9)
    ridge = (fid == order.index(2)) if False else (fid == 1)
    gcol = rgb(PAL["glow"]) if pal == "amber" else rgb("#BEEAF5")
    g[..., :3] = np.where(inner[..., None], gcol, 0.0)
    g[..., 3] = np.where(inner, 120.0 + 60.0 * (1 - v), 0.0)
    g[..., :3] = np.where((inner & ridge)[..., None], rgb(PAL["glow_core"]), g[..., :3])
    g[..., 3] = np.where(inner & ridge, 230.0, g[..., 3])
    return out, g


# ------------------------------------------------------------------------------------------------------------------ the face
def face_front(w, h, seed):
    """The Gnome's face: a stone mask carved with a heavy brow ridge, two narrow slanted eyes that glow amber (a white-hot core), a nose ridge, a mouth seam with square stone teeth and cheek
    cracks. Ink linework everywhere. The glow sheet has the eyes (and the thin amber lines in the cracks)."""
    base, _ = stone(w, h, seed, cell=w / 5.0, tone=-0.06, line=1.5, cracks=2)
    lay = canvas(w, h)
    d = ImageDraw.Draw(lay)

    def P(x, y):
        return (x * w * SS, y * h * SS)

    # brow ridge: a heavy dark slab over both eyes, lit on top
    d.polygon([P(0.02, 0.20), P(0.98, 0.20), P(0.98, 0.40), P(0.60, 0.46), P(0.50, 0.40), P(0.40, 0.46), P(0.02, 0.40)], fill=col(PAL["ink2"]))
    d.polygon([P(0.04, 0.22), P(0.96, 0.22), P(0.96, 0.36), P(0.60, 0.42), P(0.50, 0.36), P(0.40, 0.42), P(0.04, 0.36)], fill=col(PAL["dark"]))
    d.polygon([P(0.04, 0.22), P(0.96, 0.22), P(0.96, 0.27), P(0.04, 0.27)], fill=col(PAL["light"]))
    # eye sockets (ink) and the eyes: narrow, slanted down toward the nose, the "angry stone" look
    for s in (-1, 1):
        cx = 0.5 + s * 0.23
        sock = [P(cx - 0.17, 0.44 - s * 0.0), P(cx + 0.17, 0.44), P(cx + s * 0.02 + 0.13 * -s, 0.60), P(cx - 0.15 * -s, 0.56)]
        d.polygon([P(cx - 0.20, 0.42 - 0.04 * s * -1 + 0.0), P(cx + 0.20, 0.42), P(cx + 0.16, 0.62), P(cx - 0.16, 0.62)], fill=col(PAL["ink"]))
        tilt = 0.06 * s
        eye = [P(cx - 0.17, 0.50 - tilt), P(cx + 0.17, 0.50 + tilt), P(cx + 0.14, 0.58 + tilt), P(cx - 0.14, 0.57 - tilt)]
        d.polygon(eye, fill=col(PAL["amber"]))
        core = [P(cx - 0.12, 0.52 - tilt * 0.8), P(cx + 0.12, 0.52 + tilt * 0.8), P(cx + 0.10, 0.56 + tilt * 0.8), P(cx - 0.10, 0.55 - tilt * 0.8)]
        d.polygon(core, fill=col(PAL["amber_w"]))
    # nose ridge, mouth seam, square teeth
    d.polygon([P(0.46, 0.46), P(0.54, 0.46), P(0.58, 0.72), P(0.42, 0.72)], fill=col(PAL["dark"]))
    d.line([P(0.46, 0.46), P(0.42, 0.72)], fill=col(PAL["ink"]), width=int(1.5 * SS))
    d.line([P(0.54, 0.46), P(0.58, 0.72)], fill=col(PAL["ink"]), width=int(1.5 * SS))
    d.rectangle([P(0.12, 0.80), P(0.88, 0.90)], fill=col(PAL["ink"]))
    for i in range(7):
        x0 = 0.15 + i * 0.10
        d.rectangle([P(x0, 0.80), P(x0 + 0.07, 0.86 + 0.02 * (i % 2))], fill=col(PAL["hi"]))
    d.line([P(0.04, 0.20), P(0.96, 0.20)], fill=col(PAL["ink"]), width=int(2.0 * SS))
    gem = [P(0.5, 0.025), P(0.57, 0.10), P(0.5, 0.175), P(0.43, 0.10)]                    # a small gem on the forehead: the mage stone of the host
    d.polygon(gem, fill=col(PAL["ink"]))
    d.polygon([P(0.5, 0.05), P(0.545, 0.10), P(0.5, 0.15), P(0.455, 0.10)], fill=col(PAL["amber"]))
    d.polygon([P(0.5, 0.075), P(0.525, 0.10), P(0.5, 0.125), P(0.475, 0.10)], fill=col(PAL["amber_w"]))
    out = over(base, done(lay, w, h))
    g = clear(w, h)
    gl = canvas(w, h)
    dg = ImageDraw.Draw(gl)
    gc = canvas(w, h)
    dc = ImageDraw.Draw(gc)
    for s in (-1, 1):
        cx = 0.5 + s * 0.23
        tilt = 0.06 * s
        eye = [P(cx - 0.17, 0.50 - tilt), P(cx + 0.17, 0.50 + tilt), P(cx + 0.14, 0.58 + tilt), P(cx - 0.14, 0.57 - tilt)]
        dg.polygon(eye, fill=col(PAL["glow"]))
        core = [P(cx - 0.12, 0.52 - tilt * 0.8), P(cx + 0.12, 0.52 + tilt * 0.8), P(cx + 0.10, 0.56 + tilt * 0.8), P(cx - 0.10, 0.55 - tilt * 0.8)]
        dc.polygon(core, fill=col(PAL["glow_core"]))
    dg.polygon([P(0.5, 0.05), P(0.545, 0.10), P(0.5, 0.15), P(0.455, 0.10)], fill=col(PAL["glow"]))
    dc.polygon([P(0.5, 0.075), P(0.525, 0.10), P(0.5, 0.125), P(0.475, 0.10)], fill=col(PAL["glow_core"]))
    g = over(over(g, done(gl, w, h)), done(gc, w, h))
    return out, g


def face_side(w, h, seed):
    """The side of the head: facets, a stone ear slab, an ink brow line."""
    out, g = stone(w, h, seed, cell=w / 4.0, tone=-0.03, line=1.5, moss=0.3, cracks=1)
    lay = canvas(w, h)
    d = ImageDraw.Draw(lay)
    d.polygon([(w * 0.05 * SS, h * 0.20 * SS), (w * 0.95 * SS, h * 0.20 * SS), (w * 0.95 * SS, h * 0.36 * SS), (w * 0.05 * SS, h * 0.42 * SS)], fill=col(PAL["dark"]))
    d.line([(w * 0.05 * SS, h * 0.20 * SS), (w * 0.95 * SS, h * 0.20 * SS)], fill=col(PAL["ink"]), width=int(2 * SS))
    d.line([(w * 0.05 * SS, h * 0.42 * SS), (w * 0.95 * SS, h * 0.36 * SS)], fill=col(PAL["ink"]), width=int(1.6 * SS))
    return over(out, done(lay, w, h)), g


# ------------------------------------------------------------------------------------------------------------------ the magic circle (the chest) and the bands
def magic_circle(w, h, seed):
    """The carved magic circle of the chest: concentric rings, a ring of rune ticks, a diamond (the Diamond Kingdom's suit) inside a four-pointed star, a small clover of four dots. Carved in
    dark ink-brown lines with a lit lower edge on a rock plate; the glow sheet has the same lines in amber, the centre brightest."""
    base, _ = stone(w, h, seed, cell=w / 3.0, tone=0.0, line=1.7, moss=0.0)
    ink, rim, glw, core = canvas(w, h), canvas(w, h), canvas(w, h), canvas(w, h)
    di, dr, dg, dc = ImageDraw.Draw(ink), ImageDraw.Draw(rim), ImageDraw.Draw(glw), ImageDraw.Draw(core)
    cx, cy = w / 2.0, h / 2.0
    R = min(w, h) * 0.46

    def stroke(fn, wd):
        fn(di, col(PAL["ink"]), wd)
        fn(dr, col(PAL["light"]), wd * 0.7)
        fn(dg, col(PAL["glow"]), wd * 1.15)
        fn(dc, col(PAL["glow_core"]), wd * 0.45)

    def circ(r, wd):
        def f(d, c, wd2):
            d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], outline=c, width=int(wd2 * SS))
        stroke(f, wd)

    def poly(pts, wd, closed=True):
        pp = [(x * SS, y * SS) for x, y in pts]
        if closed:
            pp = pp + [pp[0]]

        def f(d, c, wd2):
            d.line(pp, fill=c, width=int(wd2 * SS), joint="curve")
        stroke(f, wd)

    circ(R, 3.4)
    circ(R * 0.88, 1.8)
    circ(R * 0.62, 2.6)
    for i in range(24):                                                    # rune ticks between the two outer rings
        a = i / 24.0 * math.tau
        r0, r1 = R * 0.89, R * (0.97 if i % 2 == 0 else 0.94)
        poly([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r1, cy + math.sin(a) * r1)], 1.8, False)
    star = []
    for i in range(8):                                                     # a four-pointed star
        a = i / 8.0 * math.tau - math.pi / 2
        r = R * (0.60 if i % 2 == 0 else 0.30)
        star.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    poly(star, 2.4)
    dia = [(cx, cy - R * 0.30), (cx + R * 0.20, cy), (cx, cy + R * 0.30), (cx - R * 0.20, cy)]            # the diamond
    poly(dia, 2.8)
    for k in range(4):                                                     # clover: four dots round the diamond
        a = k / 4.0 * math.tau + math.pi / 4
        x, y = cx + math.cos(a) * R * 0.46, cy + math.sin(a) * R * 0.46
        for d, c in ((di, col(PAL["ink"])), (dg, col(PAL["glow"])), (dc, col(PAL["glow_core"]))):
            rr = 3.2 if d is not dc else 1.7
            d.ellipse([(x - rr) * SS, (y - rr) * SS, (x + rr) * SS, (y + rr) * SS], fill=c)
    for d, c in ((dg, col(PAL["glow"])), (dc, col(PAL["glow_core"]))):
        rr = R * 0.07 if d is dg else R * 0.035
        d.ellipse([(cx - rr) * SS, (cy - rr) * SS, (cx + rr) * SS, (cy + rr) * SS], fill=c)
    # the lit lower edge of every carved line: the ink shifted down-right by 1 texel, drawn first under the ink
    rimd = done(rim, w, h)
    rimd = np.roll(np.roll(rimd, 1, 0), 1, 1)
    out = over(base, rimd)
    out = over(out, done(ink, w, h))
    g = over(done(glw, w, h), done(core, w, h))
    return out, g


def bracer(w, h, seed):
    """A band of rune glyphs (cuffs of the forearms, the belt): a darker stone strip, ink edges, square-cut glyphs that glow amber."""
    base, _ = stone(w, h, seed, cell=h * 0.9, tone=-0.1, line=1.6)
    lay, gl, gc = canvas(w, h), canvas(w, h), canvas(w, h)
    d, dg, dc = ImageDraw.Draw(lay), ImageDraw.Draw(gl), ImageDraw.Draw(gc)
    d.rectangle([0, 0, w * SS, 2.2 * SS], fill=col(PAL["ink"]))
    d.rectangle([0, (h - 2.2) * SS, w * SS, h * SS], fill=col(PAL["ink"]))
    d.rectangle([0, 2.2 * SS, w * SS, 4.0 * SS], fill=col(PAL["light"]))
    rng = np.random.default_rng(seed)
    n = max(4, int(w / (h * 0.9)))
    gw = w / n
    for i in range(n):
        x0 = i * gw + gw * 0.2
        x1 = x0 + gw * 0.6
        y0, y1 = h * 0.26, h * 0.74
        segs = [[(x0, y0), (x1, y0)], [(x1, y0), (x1, y1)], [(x1, y1), (x0, y1)], [(x0, y1), (x0, y0)], [(x0, (y0 + y1) / 2), (x1, (y0 + y1) / 2)],
                [((x0 + x1) / 2, y0), ((x0 + x1) / 2, y1)], [(x0, y0), (x1, y1)], [(x1, y0), (x0, y1)]]
        pick = [s for s in segs if rng.random() < 0.55] or segs[:2]
        for s in pick:
            pts = [(x * SS, y * SS) for x, y in s]
            d.line(pts, fill=col(PAL["ink"]), width=int(2.6 * SS))
            dg.line(pts, fill=col(PAL["glow"]), width=int(2.6 * SS))
            dc.line(pts, fill=col(PAL["glow_core"]), width=int(1.0 * SS))
    out = over(base, done(lay, w, h))
    g = over(done(gl, w, h), done(gc, w, h))
    return out, g


def knuckle(w, h, seed):
    """The front of a fist: four fingers' blocks side by side, each a lit top and a dark seam, cracks, a worn bright scuff."""
    base, g = stone(w, h, seed, cell=w / 4.2, tone=0.0, line=1.8, cracks=3, vein=1, shade_side=0.3)
    lay = canvas(w, h)
    d = ImageDraw.Draw(lay)
    for i in range(1, 4):
        x = w * i / 4.0
        d.line([(x * SS, 3 * SS), (x * SS, h * 0.92 * SS)], fill=col(PAL["ink"]), width=int(2.2 * SS))
    d.line([(0, h * 0.30 * SS), (w * SS, h * 0.30 * SS)], fill=col(PAL["ink"]), width=int(1.8 * SS))
    return over(base, done(lay, w, h)), g


def veins_plate(w, h, seed):
    """Glow-only plate of crystal veins that branch like lightning across rock; the colour sheet is empty here (the plate is drawn only by the glow-only model)."""
    rng = np.random.default_rng(seed)
    gl, gc = canvas(w, h), canvas(w, h)
    dg, dc = ImageDraw.Draw(gl), ImageDraw.Draw(gc)

    def branch(x, y, a, n, wd):
        for _ in range(n):
            a2 = a + (rng.random() - 0.5) * 1.1
            nx, ny = x + math.cos(a2) * (h * 0.07 + 2), y + math.sin(a2) * (h * 0.07 + 2)
            dg.line([(x * SS, y * SS), (nx * SS, ny * SS)], fill=col(PAL["glow"]), width=int(max(1.2, wd * 2.4) * SS))
            dc.line([(x * SS, y * SS), (nx * SS, ny * SS)], fill=col(PAL["glow_core"]), width=int(max(0.8, wd) * SS))
            if rng.random() < 0.22 and wd > 0.9:
                branch(nx, ny, a2 + (rng.random() - 0.5) * 2.2, int(n * 0.55), wd * 0.7)
            x, y, a = nx, ny, a2
    for i in range(3):
        branch(w * (0.2 + 0.3 * i), h * 0.02, math.pi / 2 + (rng.random() - 0.5) * 0.7, 12, 1.5)
    for i in range(2):
        branch(w * 0.05, h * (0.25 + 0.4 * i), 0.1 + rng.random() * 0.4, 9, 1.3)
    return clear(w, h), over(done(gl, w, h), done(gc, w, h))


def moss_strand(w, h, seed):
    """A hanging tuft of moss / roots: a few flat olive leaf-blades with ink edges, transparent outside (a cutout plate)."""
    rng = np.random.default_rng(seed)
    lay = canvas(w, h)
    d = ImageDraw.Draw(lay)
    n = 5
    for i in range(n):
        x = (i + 0.5) / n * w + (rng.random() - 0.5) * w * 0.12
        ln = h * (0.55 + 0.4 * rng.random())
        bw = w / n * 0.62
        pts = [(x - bw / 2, 0), (x + bw / 2, 0), (x + bw * 0.18, ln * 0.7), (x, ln), (x - bw * 0.18, ln * 0.7)]
        d.polygon([(a * SS, b * SS) for a, b in pts], fill=col(PAL["ink"]))
        inner = [(x - bw / 2 + 1.5, 0), (x + bw / 2 - 1.5, 0), (x + bw * 0.1, ln * 0.68), (x, ln - 2.2), (x - bw * 0.1, ln * 0.68)]
        d.polygon([(a * SS, b * SS) for a, b in inner], fill=col(PAL["moss"] if i % 2 else PAL["moss_l"]))
        d.line([(x * SS, 1 * SS), (x * SS, ln * 0.8 * SS)], fill=col(PAL["moss_d"]), width=int(1.2 * SS))
    return done(lay, w, h), clear(w, h)


def rubble(w, h, seed):
    """Loose dirt and gravel for the base the Gnome rises from: flat brown clods with ink edges."""
    out, g = stone(w, h, seed, cell=w / 6.0, tone=-0.12, line=1.5, warm=0.4)
    return out, g
