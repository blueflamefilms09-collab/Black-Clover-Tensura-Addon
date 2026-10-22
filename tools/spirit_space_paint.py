"""The painters of the geo sheets of the Spirit of Space (docs/magic/spirit_space.md): the cracked-glass crystals of the orb's rift ring and the faces of the wisp's small horned head. Each painter returns
(colour, glow) float arrays (h, w, 4) in 0..255, the contract of spirit_space_lib.Atlas.region. Deterministic (every painter is seeded by its name). The look: deep indigo / black with violet and cyan
star points, thin pale cel linework (a dark outline and a lilac highlight line), glow only in the glow arrays (drawn additive through the safe helpers).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spirit_space_lib import rgb, smoothstep  # noqa: E402
from gen_spirit_space_art import pfbm, seed_of, C  # noqa: E402

SS = 4


def col255(name, a=255):
    c = C[name] * 255.0
    return int(c[0]), int(c[1]), int(c[2]), a


def canvas(w, h):
    return Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))


def finish(img, w, h):
    return np.asarray(img.resize((w, h), Image.LANCZOS), dtype=np.float32)


def over(base, layer):
    a = layer[..., 3:4] / 255.0
    ba = base[..., 3:4] / 255.0
    oa = a + ba * (1 - a)
    out = np.where(oa > 1e-5, (layer[..., :3] * a + base[..., :3] * ba * (1 - a)) / np.maximum(oa, 1e-5), 0.0)
    return np.concatenate([out, oa * 255.0], axis=-1)


def blank(w, h):
    return np.zeros((h, w, 4), np.float32)


def dark_surface(w, h, name, lift=0.0, specks=0.004):
    """Deep indigo with a soft violet haze and a few star specks: the base of every dark face."""
    rng = np.random.default_rng(seed_of(name))
    n = pfbm(rng, w, h, 3, 4)
    n2 = pfbm(rng, w, h, 6, 3)
    base = C["void"] + (C["indigo"] - C["void"]) * smoothstep(n, 0.25, 0.8)[..., None]
    base = base + (C["deep"] - C["indigo"]) * (smoothstep(n2, 0.5, 0.9) * 0.7)[..., None]
    base = base + (C["violet"] * 0.35 - base) * lift
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = np.clip(base, 0, 1) * 255.0
    out[..., 3] = 255
    sp = rng.random((h, w)) < specks
    tint = np.array([C["pale"], C["cyan"], C["violet_l"]])[rng.integers(0, 3, (h, w))] * 255.0
    out[..., :3] = np.where(sp[..., None], tint * rng.uniform(0.5, 1.0, (h, w, 1)), out[..., :3])
    return out


def vgrad(arr, top, bottom):
    """Multiplies the rgb by a vertical light ramp (top lit, bottom shaded): the cel shading of a face."""
    h = arr.shape[0]
    k = np.linspace(top, bottom, h, dtype=np.float32)[:, None, None]
    out = arr.copy()
    out[..., :3] = np.clip(out[..., :3] * k, 0, 255)
    return out


def outline(arr, color=("ink", 255), light=("violet_l", 150), px=1.0, inner=True):
    """The thin cel linework: a dark line round the face and a lilac highlight line just inside its top and front edges."""
    h, w = arr.shape[:2]
    out = arr.copy()
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
    line = np.clip(1.0 - (d - 0.0) / px, 0, 1)[..., None]
    c = np.array(col255(color[0])[:3], np.float32)
    out[..., :3] = out[..., :3] * (1 - line) + c * line
    if inner:
        d2 = np.minimum(xx, yy)
        hl = np.clip(1.0 - np.abs(d2 - (px + 1.2)) / 0.9, 0, 1)[..., None] * (light[1] / 255.0)
        c2 = np.array(col255(light[0])[:3], np.float32)
        out[..., :3] = out[..., :3] * (1 - hl) + c2 * hl
    return out


# ------------------------------------------------------------------------------------------------------------------ the orb's crystals
def crystal(w, h, name, bright=1.0):
    """A face of a cracked-glass shard (drawn ADDITIVE, so the dark is nothing and only the light shows): a violet body, a hot cyan-white rim, pale fracture lines inside, star specks."""
    rng = np.random.default_rng(seed_of(name))
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = xx / max(1, w - 1), yy / max(1, h - 1)
    body = (C["violet"] * 0.30)[None, None, :] * (0.45 + 0.55 * (1 - v))[..., None] * np.ones((h, w, 1), np.float32)
    n = pfbm(rng, w, h, 3, 3)
    body = body * (0.55 + 0.9 * n[..., None])
    d = np.minimum(np.minimum(u, 1 - u) * w, np.minimum(v, 1 - v) * h)
    rim = np.clip(1.0 - d / 1.3, 0, 1)[..., None]
    col = body * (1 - rim) + (C["cyan"] * 0.75 + C["pale"] * 0.15)[None, None, :] * rim
    img = Image.fromarray(np.clip(col * 255, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    big = img.resize((w * 3, h * 3), Image.BICUBIC)
    dr = ImageDraw.Draw(big)
    for k in range(int(2 + w / 32)):                                            # fracture lines
        x0, y0 = rng.uniform(0, w * 3), rng.uniform(0, h * 3)
        ang = rng.uniform(0, math.tau)
        ln = rng.uniform(w, w * 3 * 0.9)
        pts = [(x0, y0)]
        for s in range(3):
            ang += rng.uniform(-0.6, 0.6)
            pts.append((pts[-1][0] + math.cos(ang) * ln / 3, pts[-1][1] + math.sin(ang) * ln / 3))
        dr.line(pts, fill=col255("lilac", 150), width=1)
        dr.line([(p[0] + 1, p[1] + 1) for p in pts], fill=col255("cyan", 110), width=1)
    arr = np.asarray(big.resize((w, h), Image.LANCZOS), dtype=np.float32)
    sp = rng.random((h, w)) < 0.012
    arr[..., :3] = np.where(sp[..., None], 255.0, arr[..., :3])
    arr[..., :3] *= bright
    arr[..., 3] = 255
    return np.clip(arr, 0, 255), blank(w, h)


# ------------------------------------------------------------------------------------------------------------------ the wisp's head
def star_dots(arr, rng, n, region=None):
    h, w = arr.shape[:2]
    for _ in range(n):
        x, y = int(rng.integers(1, w - 1)), int(rng.integers(1, h - 1))
        c = [C["pale"], C["cyan"], C["violet_l"]][int(rng.integers(0, 3))] * 255.0
        arr[y, x, :3] = c
    return arr


def constellation(img, pts, w, h, line=("cyan_d", 150), dot=("pale", 255), width=1):
    """A few stars joined by thin lines: the cosmic marking of the dragon's cheek."""
    dr = ImageDraw.Draw(img)
    p = [(x * w * SS, y * h * SS) for x, y in pts]
    for a, b in zip(p, p[1:]):
        dr.line([a, b], fill=col255(line[0], line[1]), width=max(1, width * SS // 2))
    for x, y in p:
        r = 1.1 * SS
        dr.ellipse([x - r, y - r, x + r, y + r], fill=col255(dot[0], dot[1]))


def eye_shapes(img_c, img_g, cx, cy, ew, eh, w, h, tilt=-12.0):
    """The almond eye: a dark socket, a cyan iris, a white-hot slit; the glow sheet gets the iris only (the additive fx cube draws it)."""
    ang = math.radians(tilt)

    def almond(scale):
        pts = []
        for i in range(32):
            t = i / 32 * math.tau
            x, y = math.cos(t) * ew * scale, math.sin(t) * eh * scale * (1.0 - 0.25 * abs(math.cos(t)))
            pts.append(((cx + x * math.cos(ang) - y * math.sin(ang)) * w * SS, (cy + x * math.sin(ang) + y * math.cos(ang)) * h * SS))
        return pts

    dc, dg = ImageDraw.Draw(img_c), ImageDraw.Draw(img_g)
    dc.polygon(almond(1.35), fill=col255("ink", 255))
    dc.polygon(almond(1.0), fill=col255("cyan_d", 255))
    dc.polygon(almond(0.62), fill=col255("cyan", 255))
    dg.polygon(almond(1.0), fill=col255("cyan", 255))
    slit = [((cx + dx * ew * 0.9) * w * SS, (cy + dy * eh * 0.9) * h * SS) for dx, dy in ((-0.1, -0.55), (0.1, -0.55), (0.1, 0.55), (-0.1, 0.55))]
    ts = [(cx * w * SS + (x - cx * w * SS) * math.cos(ang) - (y - cy * h * SS) * math.sin(ang), cy * h * SS + (x - cx * w * SS) * math.sin(ang) + (y - cy * h * SS) * math.cos(ang)) for x, y in slit]
    dc.polygon(ts, fill=col255("pale", 255))
    dg.polygon(ts, fill=col255("pale", 255))


def head_side(w, h, mirror=False):
    """The side of the skull (16 x 12 units): the brow arch, the eye, a constellation on the cheek, scale arcs toward the back, a ridge where the cheek fin grows. Front is the left of the picture
    (the face is flipped for the other side)."""
    name = "head_side"
    base = vgrad(dark_surface(w, h, name, lift=0.18), 1.25, 0.65)
    glow = blank(w, h)
    img_c, img_g = canvas(w, h), canvas(w, h)
    dc = ImageDraw.Draw(img_c)
    for k in range(5):                                                         # scale arcs fanning to the back
        x0 = (0.52 + 0.09 * k) * w * SS
        dc.arc([x0 - 0.22 * w * SS, 0.30 * h * SS, x0 + 0.22 * w * SS, 1.05 * h * SS], 300, 60, fill=col255("violet_l", 110 - k * 10), width=SS)
    dc.line([(0.06 * w * SS, 0.32 * h * SS), (0.30 * w * SS, 0.20 * h * SS), (0.62 * w * SS, 0.17 * h * SS)], fill=col255("lilac", 200), width=SS)           # the brow
    dc.line([(0.06 * w * SS, 0.36 * h * SS), (0.30 * w * SS, 0.24 * h * SS), (0.62 * w * SS, 0.21 * h * SS)], fill=col255("ink", 230), width=SS)
    eye_shapes(img_c, img_g, 0.30, 0.43, 0.14, 0.10, w, h, tilt=-14.0)
    constellation(img_c, [(0.50, 0.58), (0.62, 0.70), (0.74, 0.62), (0.84, 0.78), (0.90, 0.58)], w, h)
    dc.line([(0.90 * w * SS, 0.10 * h * SS), (0.93 * w * SS, 0.90 * h * SS)], fill=col255("ink", 220), width=2 * SS)
    out = over(base, finish(img_c, w, h))
    glow = over(glow, finish(img_g, w, h))
    out = outline(out)
    if mirror:
        out, glow = out[:, ::-1].copy(), glow[:, ::-1].copy()
    return out, glow


def head_top(w, h):
    """The top of the skull (14 wide x 16 long; the front is the top of the picture): a ridge, the brows, the star specks."""
    base = vgrad(dark_surface(w, h, "head_top", lift=0.28), 1.3, 0.9)
    img = canvas(w, h)
    dc = ImageDraw.Draw(img)
    dc.line([(0.5 * w * SS, 0.02 * h * SS), (0.5 * w * SS, 0.96 * h * SS)], fill=col255("lilac", 190), width=SS)
    for sgn in (-1, 1):
        dc.line([(0.5 * w * SS + sgn * 0.10 * w * SS, 0.10 * h * SS), (0.5 * w * SS + sgn * 0.34 * w * SS, 0.30 * h * SS), (0.5 * w * SS + sgn * 0.40 * w * SS, 0.58 * h * SS)], fill=col255("violet_l", 160), width=SS)
        dc.line([(0.5 * w * SS + sgn * 0.12 * w * SS, 0.14 * h * SS), (0.5 * w * SS + sgn * 0.36 * w * SS, 0.34 * h * SS)], fill=col255("ink", 220), width=SS)
    out = outline(over(base, finish(img, w, h)))
    return out, blank(w, h)


def snout(w, h, side, mirror=False):
    """The muzzle: a nostril, a lilac line along the top edge, the corner of the mouth; front on the left."""
    base = vgrad(dark_surface(w, h, "snout_" + side, lift=0.22), 1.25, 0.6)
    img = canvas(w, h)
    dc = ImageDraw.Draw(img)
    if side == "side":
        dc.ellipse([0.10 * w * SS, 0.20 * h * SS, 0.18 * w * SS, 0.36 * h * SS], fill=col255("ink", 255))
        dc.line([(0.02 * w * SS, 0.80 * h * SS), (0.5 * w * SS, 0.86 * h * SS), (0.98 * w * SS, 0.74 * h * SS)], fill=col255("lilac", 190), width=SS)
        dc.line([(0.60 * w * SS, 0.20 * h * SS), (0.98 * w * SS, 0.30 * h * SS)], fill=col255("violet_l", 140), width=SS)
        for k in range(4):
            x = (0.30 + 0.14 * k) * w * SS
            dc.arc([x - 0.07 * w * SS, 0.3 * h * SS, x + 0.07 * w * SS, 0.8 * h * SS], 270, 90, fill=col255("violet_l", 90), width=SS)
    elif side == "top":
        dc.line([(0.5 * w * SS, 0.0), (0.5 * w * SS, h * SS)], fill=col255("lilac", 170), width=SS)
        for sgn in (-1, 1):
            dc.ellipse([(0.5 + sgn * 0.26) * w * SS - 0.05 * w * SS, 0.06 * h * SS, (0.5 + sgn * 0.26) * w * SS + 0.05 * w * SS, 0.16 * h * SS], fill=col255("ink", 255))
    else:                                                                       # the front of the muzzle: two nostrils, the upper teeth
        for sgn in (-1, 1):
            cx = (0.5 + sgn * 0.22) * w * SS
            dc.ellipse([cx - 0.07 * w * SS, 0.28 * h * SS, cx + 0.07 * w * SS, 0.50 * h * SS], fill=col255("ink", 255))
        for k in range(5):
            x = (0.14 + 0.18 * k) * w * SS
            dc.polygon([(x - 0.05 * w * SS, 0.72 * h * SS), (x + 0.05 * w * SS, 0.72 * h * SS), (x, 0.98 * h * SS)], fill=col255("pale", 255))
    out = outline(over(base, finish(img, w, h)))
    if mirror:
        out = out[:, ::-1].copy()
    return out, blank(w, h)


def jaw(w, h, mirror=False):
    base = vgrad(dark_surface(w, h, "jaw_side", lift=0.15), 1.1, 0.6)
    img = canvas(w, h)
    dc = ImageDraw.Draw(img)
    for k in range(7):                                                          # small fangs along the lip
        x = (0.10 + 0.12 * k) * w * SS
        dc.polygon([(x - 0.02 * w * SS, 0.0), (x + 0.02 * w * SS, 0.0), (x, 0.45 * h * SS)], fill=col255("pale", 255))
    dc.line([(0, 0.92 * h * SS), (w * SS, 0.92 * h * SS)], fill=col255("lilac", 170), width=SS)
    out = outline(over(base, finish(img, w, h)))
    if mirror:
        out = out[:, ::-1].copy()
    return out, blank(w, h)


def jaw_in(w, h):
    """The inside of the mouth (the top of the lower jaw): dark, with a glow in the throat."""
    base = vgrad(dark_surface(w, h, "jaw_in", lift=0.05), 0.9, 1.4)
    g = blank(w, h)
    yy = np.linspace(0, 1, h, dtype=np.float32)[:, None]
    xx = np.linspace(-1, 1, w, dtype=np.float32)[None, :]
    throat = np.exp(-((yy - 0.1) ** 2) / 0.08) * np.exp(-(xx ** 2) / 0.35)
    g[..., :3] = (C["cyan"] * 0.9 + C["violet_l"] * 0.2)[None, None, :] * 255.0 * np.clip(throat, 0, 1)[..., None]
    g[..., 3] = np.clip(throat * 255.0, 0, 255)
    return outline(base, inner=False), g


def horn(w, h):
    """A horn segment: lilac bone with dark groove rings, a pale line along the front (u across, v along; the tip is at v = 1)."""
    base = vgrad(dark_surface(w, h, "horn", lift=0.5), 1.35, 0.55)
    base[..., :3] = np.clip(base[..., :3] * 0.6 + np.array(col255("violet_l")[:3], np.float32) * 0.45, 0, 255)
    img = canvas(w, h)
    dc = ImageDraw.Draw(img)
    for k in range(1, 6):
        y = k / 6 * h * SS
        dc.line([(0, y), (w * SS, y + 0.03 * h * SS)], fill=col255("ink", 200), width=SS)
    dc.line([(0.22 * w * SS, 0), (0.22 * w * SS, h * SS)], fill=col255("pale", 170), width=SS)
    out = outline(over(base, finish(img, w, h)))
    return out, blank(w, h)


def whisker(w, h):
    base = dark_surface(w, h, "whisker", lift=0.4)
    base[..., :3] = np.clip(base[..., :3] * 0.5 + np.array(col255("lilac")[:3], np.float32) * 0.5, 0, 255)
    return outline(base, inner=False), blank(w, h)


def fin_glow(w, h):
    """An additive fin: the whole picture is light (the colour sheet is dark and unused for it; the fx model reads the glow sheet)."""
    rng = np.random.default_rng(seed_of("fin_glow"))
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = xx / max(1, w - 1)
    v = yy / max(1, h - 1)
    root = (1.0 - u) ** 0.9
    spar = np.zeros((h, w), np.float32)
    for k in range(5):
        t = (k + 0.5) / 5
        spar = np.maximum(spar, np.exp(-(((v - t) * h) ** 2) / 0.8) * (1 - u * 0.6))
    n = pfbm(rng, w, h, 3, 3)
    a = np.clip(root * (0.4 + 0.5 * n) + spar * root, 0, 1)
    col = C["violet"][None, None, :] * (1 - u[..., None]) + C["cyan"][None, None, :] * u[..., None]
    col = col * (1 - spar[..., None] * 0.7) + C["pale"] * (spar[..., None] * 0.7)
    g = blank(w, h)
    g[..., :3] = np.clip(col, 0, 1) * 255.0 * 0.7
    g[..., 3] = a * 255.0 * 0.8
    return blank(w, h), g


def glow_solid(w, h, a_name="cyan", b_name="pale", k=1.0):
    """A small glow cube face: a bright core fading to the edge colour (eye caps, horn tips, whisker beads)."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.hypot((xx - w / 2 + 0.5) / (w / 2), (yy - h / 2 + 0.5) / (h / 2))
    kk = np.clip(1.0 - d, 0, 1)[..., None]
    g = blank(w, h)
    g[..., :3] = (C[a_name] * (1 - kk) + C[b_name] * kk) * 255.0 * k
    g[..., 3] = np.clip((1.0 - d) * 4.0, 0, 1) * 255.0                              # an ellipse with a soft edge: the eye and the beads are not squares
    return blank(w, h), g
