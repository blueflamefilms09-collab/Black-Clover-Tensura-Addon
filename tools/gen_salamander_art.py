"""The hand-painted sheets of the Salamander (Fuegoleon Vermillion's Spirit Lord, docs/magic/fire_casting.md): scales with ring spots, belly plates, the faces of the head with the yellow slit eyes
and the teeth, spikes, claws and the wing membranes with their spars and veins. Deterministic (every painter is seeded). The colours are read off the four reference stills of the
Black Clover wiki (https://blackclover.fandom.com/wiki/Salamander, English wiki first): the small form is the RED scaled lizard-dragon with orange bat wings (perched on the shoulder), the big form
the GOLDEN-ORANGE spotted dragon with red ring spots, red claws, veined wings and a spiny crest. Used by tools/gen_salamander_geo.py.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from firecast_geo_lib import rgb, smoothstep, fbm  # noqa: E402

SS = 3                                                    # supersampling of the drawn shapes

SMALL = dict(base="#DC4A2C", dark="#8E2512", light="#F57A44", outline="#4A120C", ring="#F6B04C", ring_in="#E5582F", belly="#F4A44B", belly_dark="#C8702E", belly_line="#8A3818",
             membrane="#F7B65E", membrane2="#F28C4A", vein="#C3401E", bone="#B22E1C", bone_light="#E36A3C", eye="#F4D64A", eye_dark="#27090A", tooth="#F6ECD6", claw="#EFDDB4",
             spike="#E4502C", spike_tip="#F7B35A", mouth="#7A1210", glow="#FFD45A", spots=0.20, cell=9, seam=0.55)
BIG = dict(base="#F0AE55", dark="#CC7D32", light="#FAD48A", outline="#7A3A14", ring="#B9561F", ring_in="#EDAA55", belly="#F5CD84", belly_dark="#DA9A4E", belly_line="#B26A2E",
           membrane="#F0B862", membrane2="#E68A40", vein="#B24B1E", bone="#C2622C", bone_light="#EE9A52", eye="#F8E858", eye_dark="#2A0F06", tooth="#FAF0DA", claw="#B8472B",
           spike="#DC722E", spike_tip="#F6C170", mouth="#7E1A12", glow="#FFE070", spots=0.70, cell=11, seam=0.24)


def col(h, a=255):
    c = rgb(h)
    return int(c[0]), int(c[1]), int(c[2]), a


def canvas(w, h):
    return Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))


def done(img, w, h):
    return np.asarray(img.resize((w, h), Image.LANCZOS), dtype=np.float32)


def over(base, layer):
    """Alpha-composites an RGBA float layer over an RGBA float base (straight alpha), returns RGBA float."""
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


# ------------------------------------------------------------------------------------------------------------------ scales
def scales(w, h, pal, seed, cell=None, rings=True):
    """Round scales on a hex lattice (each pixel belongs to its nearest scale centre): lit from the upper left, a dark seam where two scales meet and a dark crescent under each, a tone of its
    own per scale; on top, ring spots (golden rings on the red small form, red rings on the golden big one). pal['seam'] is how dark the seams are."""
    rng = np.random.default_rng(seed)
    cw = float(cell or pal["cell"])
    ch = cw * 0.86
    base, dark, light, outline = rgb(pal["base"]), rgb(pal["dark"]), rgb(pal["light"]), rgb(pal["outline"])
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    row0 = np.floor(yy / ch).astype(int)
    best = np.full((h, w), 1e9, np.float32)
    second = np.full((h, w), 1e9, np.float32)
    bx = np.zeros((h, w), np.float32)
    by = np.zeros((h, w), np.float32)
    bid = np.zeros((h, w), np.int64)
    for dr_ in (-1, 0, 1, 2):
        r = row0 + dr_
        off = (r % 2) * cw * 0.5
        c0 = np.floor((xx - off) / cw).astype(int)
        for dc in (-1, 0, 1, 2):
            cx = (c0 + dc + 0.5) * cw + off
            cy = (r + 0.5) * ch
            dd = np.hypot((xx - cx) / cw, (yy - cy) / ch * 0.92)
            better = dd < best
            second = np.where(better, best, np.minimum(second, dd))
            bx = np.where(better, cx, bx)
            by = np.where(better, cy, by)
            bid = np.where(better, (r * 7919 + (c0 + dc) * 104729) & 0xFFFF, bid)
            best = np.where(better, dd, best)
    tone = (bid % 257).astype(np.float32) / 257.0
    dx = (xx - bx) / (cw * 0.5)
    dy = (yy - by) / (ch * 0.5)
    lit = 0.70 - 0.40 * dy - 0.22 * dx + (tone - 0.5) * 0.24
    t1 = smoothstep(lit, 0.05, 0.60)[..., None]
    t2 = smoothstep(lit, 0.62, 1.05)[..., None]
    c = dark + (base - dark) * t1
    c = c + (light - base) * t2 * 0.85
    gap = second - best
    seam = (1.0 - smoothstep(gap, 0.0, 0.16))[..., None] * pal.get("seam", 0.55)
    under = (smoothstep(dy, 0.25, 1.0) * 0.30)[..., None]
    c = c * (1 - under) + dark * under * 0.8
    c = c * (1 - seam) + outline * seam
    c = c * (0.95 + 0.10 * fbm(rng, w, h, 3, 3)[..., None])
    out = np.concatenate([np.clip(c, 0, 255), np.full((h, w, 1), 255.0, np.float32)], axis=-1)
    if rings:
        out = ring_spots(out, w, h, pal, rng)
    return out


def ring_spots(base_img, w, h, pal, rng):
    """Ring spots over the scales: a coloured ring and a lighter / darker centre, scattered on a jittered grid; pal['spots'] is the share of cells that get one."""
    big = canvas(w, h)
    dr = ImageDraw.Draw(big)
    gap = max(12, int(min(w, h) / 10.0))
    for gy in range(-1, h // gap + 2):
        for gx in range(-1, w // gap + 2):
            if rng.random() > pal["spots"]:
                continue
            px = (gx + 0.5 + (rng.random() - 0.5) * 0.7) * gap + (gy % 2) * gap * 0.5
            py = (gy + 0.5 + (rng.random() - 0.5) * 0.7) * gap
            r = gap * (0.20 + 0.16 * rng.random())
            inner = col(pal["ring_in"], 235)
            ring = col(pal["ring"], 245)
            for ox in (-w, 0, w):                                                # wraps over the left and right edge so the window of a face never shows a cut ring
                x0, y0 = (px + ox) * SS, py * SS
                dr.ellipse([x0 - r * SS, y0 - r * SS, x0 + r * SS, y0 + r * SS], fill=ring)
                dr.ellipse([x0 - r * 0.66 * SS, y0 - r * 0.66 * SS, x0 + r * 0.66 * SS, y0 + r * 0.66 * SS], fill=inner)
                dr.ellipse([x0 - r * 0.28 * SS, y0 - r * 0.28 * SS, x0 + r * 0.28 * SS, y0 + r * 0.28 * SS], fill=ring)
    return over(base_img, done(big, w, h))


# ------------------------------------------------------------------------------------------------------------------ belly, bone, spike, claw
def belly(w, h, pal, seed):
    """Belly plates: wide bands across, each lit at its top edge and dark under it, a faint line along the middle."""
    rng = np.random.default_rng(seed)
    b, dk, ln = rgb(pal["belly"]), rgb(pal["belly_dark"]), rgb(pal["belly_line"])
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    band = max(5.0, h / 9.0)
    t = (yy % band) / band
    c = dk + (b - dk) * smoothstep(1 - t, 0.0, 0.55)[..., None]
    seam = smoothstep(t, 0.86, 1.0)[..., None]
    c = c * (1 - 0.7 * seam) + ln * 0.7 * seam
    c = c * (0.96 + 0.08 * fbm(rng, w, h, 3, 3)[..., None])
    mid = np.exp(-((xx - w / 2) / (w * 0.012 + 0.6)) ** 2)[..., None] * 0.25
    c = c * (1 - mid) + ln * mid
    return np.concatenate([np.clip(c, 0, 255), np.full((h, w, 1), 255.0, np.float32)], axis=-1)


def bone_bar(w, h, pal, seed):
    """The arm bone of a wing: a dark red bar with lighter knuckle rings and a lit top edge."""
    rng = np.random.default_rng(seed)
    b, lt, dk = rgb(pal["bone"]), rgb(pal["bone_light"]), rgb(pal["outline"])
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    along = np.clip(1 - np.abs((yy + 0.5) / h - 0.30) * 2.2, 0, 1)
    c = b + (lt - b) * (along ** 2)[..., None] * 0.9
    ring = (xx % max(10, w // 6)) < 2.0
    c = np.where(ring[..., None], c * 0.72 + dk * 0.28, c)
    c = c * (0.94 + 0.12 * fbm(rng, w, h, 3, 4)[..., None])
    return np.concatenate([np.clip(c, 0, 255), np.full((h, w, 1), 255.0, np.float32)], axis=-1)


def spike(w, h, pal, seed):
    """A flat spike (the face of a thin cube): a tall triangle, red at the foot and golden at the tip, a dark outline and a lit left edge; transparent outside."""
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)
    pts = [(w * 0.04 * SS, h * SS), (w * 0.96 * SS, h * SS), (w * 0.62 * SS, h * 0.45 * SS), (w * 0.34 * SS, 1.0 * SS)]
    dr.polygon(pts, fill=col(pal["outline"]))
    inset = [(w * 0.14 * SS, (h - 2.2) * SS), (w * 0.86 * SS, (h - 2.2) * SS), (w * 0.58 * SS, h * 0.46 * SS), (w * 0.35 * SS, 3.6 * SS)]
    dr.polygon(inset, fill=col(pal["spike"]))
    arr = done(img, w, h)
    yy = np.mgrid[0:h, 0:w][0].astype(np.float32)
    tip = smoothstep(1 - yy / h, 0.30, 0.95)[..., None]
    inside = (arr[..., 3:4] > 200) & (np.abs(arr[..., :3] - rgb(pal["outline"])).sum(-1, keepdims=True) > 40)
    mixed = arr[..., :3] * (1 - tip * 0.75) + rgb(pal["spike_tip"]) * tip * 0.75
    arr[..., :3] = np.where(inside, mixed, arr[..., :3])
    return arr


def claw(w, h, pal, seed):
    """A claw: a curved hook, pale (or red for the big form), dark at the root."""
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)
    pts = [(w * 0.10 * SS, h * SS), (w * 0.90 * SS, h * SS), (w * 0.72 * SS, h * 0.55 * SS), (w * 0.40 * SS, h * 0.14 * SS), (w * 0.30 * SS, 0.0)]
    dr.polygon(pts, fill=col(pal["outline"]))
    inner = [(w * 0.22 * SS, (h - 1.4) * SS), (w * 0.78 * SS, (h - 1.4) * SS), (w * 0.64 * SS, h * 0.56 * SS), (w * 0.40 * SS, h * 0.22 * SS), (w * 0.34 * SS, 2.0 * SS)]
    dr.polygon(inner, fill=col(pal["claw"]))
    return done(img, w, h)


# ------------------------------------------------------------------------------------------------------------------ the head
def _eye(dr, w, h, cx, cy, rx, ry, pal, tilt):
    def pt(a, r, sx=1.0, sy=1.0):
        return (cx + (math.cos(a) * r * rx * sx) * math.cos(tilt) - (math.sin(a) * r * ry * sy) * math.sin(tilt),
                cy + (math.cos(a) * r * rx * sx) * math.sin(tilt) + (math.sin(a) * r * ry * sy) * math.cos(tilt))
    ring = [pt(a, 1.0) for a in np.linspace(0, math.tau, 40, endpoint=False)]
    dr.polygon([(x * SS, y * SS) for x, y in ring], fill=col(pal["outline"]))
    iris = [pt(a, 0.82) for a in np.linspace(0, math.tau, 40, endpoint=False)]
    dr.polygon([(x * SS, y * SS) for x, y in iris], fill=col(pal["eye"]))
    core = [pt(a, 0.5, 0.9, 0.8) for a in np.linspace(0, math.tau, 32, endpoint=False)]
    dr.polygon([(x * SS, y * SS) for x, y in core], fill=col("#FFF4A0"))
    slit = [pt(-math.pi / 2, 0.86, 0.0, 1.0), pt(0, 0.20, 1.0, 0.0), pt(math.pi / 2, 0.86, 0.0, 1.0), pt(math.pi, 0.20, 1.0, 0.0)]
    dr.polygon([(x * SS, y * SS) for x, y in slit], fill=col(pal["eye_dark"]))
    sx, sy = pt(math.radians(-60), 0.46)
    dr.ellipse([(sx - 0.9) * SS, (sy - 0.9) * SS, (sx + 0.9) * SS, (sy + 0.9) * SS], fill=(255, 255, 255, 255))
    lid = [pt(a, 1.04) for a in np.linspace(math.pi * 1.05, math.pi * 1.95, 18)]
    dr.line([(x * SS, y * SS) for x, y in lid], fill=col(pal["outline"]), width=int(2.4 * SS))


def head_side(w, h, pal, seed, mirror=False):
    """The skull's side: scales, a dark brow ridge, the big yellow slit eye toward the front (the left of the picture; mirrored for the other side), a line of spines at the back. Returns colour, glow."""
    img = scales(w, h, pal, seed, cell=w / 10.0, rings=False)
    layer = canvas(w, h)
    dr = ImageDraw.Draw(layer)
    ex, ey, rx, ry = w * 0.30, h * 0.46, w * 0.20, h * 0.15
    brow = [(w * 0.04, h * 0.18), (w * 0.62, h * 0.10), (w * 0.60, h * 0.30), (w * 0.10, h * 0.40)]
    dr.polygon([(x * SS, y * SS) for x, y in brow], fill=col(pal["dark"], 235))
    dr.line([(x * SS, y * SS) for x, y in brow[:2]], fill=col(pal["outline"]), width=int(2.2 * SS))
    _eye(dr, w, h, ex, ey, rx, ry, pal, math.radians(-14))
    cheek = canvas(w, h)
    dc = ImageDraw.Draw(cheek)
    dc.line([(w * 0.22 * SS, h * 0.78 * SS), (w * 0.95 * SS, h * 0.62 * SS)], fill=col(pal["outline"], 200), width=int(1.6 * SS))
    out = over(img, done(layer, w, h))
    out = over(out, done(cheek, w, h))
    g = np.zeros((h, w, 4), dtype=np.float32)
    gl = canvas(w, h)
    dg = ImageDraw.Draw(gl)
    r2 = rx * 0.80
    dg.ellipse([(ex - r2) * SS, (ey - ry * 0.80) * SS, (ex + r2) * SS, (ey + ry * 0.80) * SS], fill=col(pal["glow"], 255))
    ga = done(gl, w, h)
    slit = canvas(w, h)
    ds = ImageDraw.Draw(slit)
    ds.polygon([(ex * SS, (ey - ry * 0.7) * SS), ((ex + 1.1) * SS, ey * SS), (ex * SS, (ey + ry * 0.7) * SS), ((ex - 1.1) * SS, ey * SS)], fill=(0, 0, 0, 255))
    sa = done(slit, w, h)
    g[..., :3] = ga[..., :3]
    g[..., 3] = np.clip(ga[..., 3] * (1.0 - sa[..., 3] / 255.0), 0, 255)
    if mirror:
        out, g = out[:, ::-1].copy(), g[:, ::-1].copy()
    return out, g


def snout_side(w, h, pal, seed, mirror=False):
    """The upper jaw's side: scales, a dark mouth line along the bottom with a row of teeth hanging below it, a nostril near the tip (the left)."""
    img = scales(w, h, pal, seed, cell=w / 9.0, rings=False)
    layer = canvas(w, h)
    dr = ImageDraw.Draw(layer)
    y0 = h * 0.74
    dr.polygon([(0, y0 * SS), (w * SS, (y0 - 1.0) * SS), (w * SS, h * SS), (0, h * SS)], fill=col(pal["mouth"], 255))
    n = max(5, int(w / 8))
    for i in range(n):
        x = (i + 0.5) * w / n
        t = 0.8 + 0.5 * math.sin(i * 2.1)
        dr.polygon([((x - w / n * 0.30) * SS, (y0 - 1) * SS), ((x + w / n * 0.30) * SS, (y0 - 1) * SS), (x * SS, (y0 + (h - y0) * 0.9 * t) * SS)], fill=col(pal["tooth"]))
    dr.line([(0, y0 * SS), (w * SS, (y0 - 1.0) * SS)], fill=col(pal["outline"]), width=int(1.8 * SS))
    dr.ellipse([(w * 0.12) * SS, (h * 0.28) * SS, (w * 0.12 + 5) * SS, (h * 0.28 + 3.4) * SS], fill=col(pal["outline"]))
    out = over(img, done(layer, w, h))
    g = np.zeros((h, w, 4), dtype=np.float32)
    if mirror:
        out, g = out[:, ::-1].copy(), g[:, ::-1].copy()
    return out, g


def jaw_side(w, h, pal, seed, mirror=False):
    """The lower jaw's side: belly-coloured scales, teeth standing up from the top edge."""
    img = belly(w, h, pal, seed)
    layer = canvas(w, h)
    dr = ImageDraw.Draw(layer)
    n = max(4, int(w / 9))
    for i in range(n):
        x = (i + 0.5) * w / n
        t = 0.8 + 0.5 * math.sin(i * 1.7 + 1)
        dr.polygon([((x - w / n * 0.28) * SS, h * 0.30 * SS), ((x + w / n * 0.28) * SS, h * 0.30 * SS), (x * SS, 0.0)], fill=col(pal["tooth"]))
    dr.line([(0, h * 0.30 * SS), (w * SS, h * 0.30 * SS)], fill=col(pal["outline"]), width=int(1.8 * SS))
    out = over(img, done(layer, w, h))
    g = np.zeros((h, w, 4), dtype=np.float32)
    if mirror:
        out, g = out[:, ::-1].copy(), g[:, ::-1].copy()
    return out, g


def jaw_in(w, h, pal, seed):
    """The inside of the mouth seen on the lower jaw's top: dark red, a pink tongue down the middle, teeth along both edges. Glows (a furnace in the throat)."""
    c = solid(w, h, pal["mouth"])
    layer = canvas(w, h)
    dr = ImageDraw.Draw(layer)
    dr.polygon([(w * 0.34 * SS, 0), (w * 0.66 * SS, 0), (w * 0.60 * SS, h * SS), (w * 0.40 * SS, h * SS)], fill=col("#D8503A"))
    n = max(4, int(h / 8))
    for i in range(n):
        y = (i + 0.5) * h / n
        for side in (0, 1):
            x0 = 0 if side == 0 else w
            sgn = 1 if side == 0 else -1
            dr.polygon([(x0 * SS, (y - h / n * 0.28) * SS), (x0 * SS, (y + h / n * 0.28) * SS), ((x0 + sgn * w * 0.16) * SS, y * SS)], fill=col(pal["tooth"]))
    out = over(c, done(layer, w, h))
    g = np.zeros((h, w, 4), dtype=np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    inner = np.clip(1.0 - np.abs(xx / w - 0.5) * 3.0, 0, 1) * 0.55
    g[..., :3] = rgb("#FF8A30")
    g[..., 3] = inner * 200
    return out, g


def head_top(w, h, pal, seed):
    return scales(w, h, pal, seed, cell=w / 8.0, rings=False), np.zeros((h, w, 4), dtype=np.float32)


def snout_front(w, h, pal, seed):
    """The tip of the snout seen from the front: scales, two nostrils."""
    img = scales(w, h, pal, seed, cell=w / 9.0, rings=False)
    layer = canvas(w, h)
    dr = ImageDraw.Draw(layer)
    for fx in (0.30, 0.70):
        dr.ellipse([(w * fx - 2.2) * SS, (h * 0.40 - 1.6) * SS, (w * fx + 2.2) * SS, (h * 0.40 + 1.6) * SS], fill=col(pal["outline"]))
    return over(img, done(layer, w, h)), np.zeros((h, w, 4), dtype=np.float32)


# ------------------------------------------------------------------------------------------------------------------ a lick of flame (glow sheet only)
def flame_lick(w, h, pal, seed):
    """A tongue of flame standing on its foot (the bottom of the picture): a red-orange body, a yellow heart, a white-hot core, a curling tip; transparent outside. Painted into the GLOW sheet only
    (the glow-only model draws it additive), so the colour sheet stays empty there."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    v = 1.0 - yy / h
    sway = 0.10 * np.sin(v * 5.0 + 1.0) * (v ** 1.2)
    half = (0.40 * (1.0 - v ** 1.5) + 0.04) * (0.85 + 0.15 * np.sin(v * 17.0))
    d = np.abs((xx / w - 0.5) - sway)
    body = np.clip(1.0 - d / np.maximum(half, 1e-3), 0.0, 1.0)
    n = fbm(rng, w, h, 3, 4)
    body = np.clip(body * (0.85 + 0.3 * n), 0.0, 1.0)
    a = smoothstep(body, 0.05, 0.22)
    t = np.clip(body * 1.3 - 0.1, 0.0, 1.0)[..., None]
    red, org, yel, wht = rgb("#E8341A"), rgb("#FF8A20"), rgb("#FFD860"), rgb("#FFF6D0")
    c = red + (org - red) * np.clip(t * 2.0, 0, 1) + (yel - org) * np.clip(t * 2.0 - 1.0, 0, 1) * 0.9 + (wht - yel) * np.clip(t * 2.4 - 1.6, 0, 1)
    g = np.zeros((h, w, 4), dtype=np.float32)
    g[..., :3] = np.clip(c, 0, 255) * a[..., None]
    g[..., 3] = a * 255.0
    return np.zeros((h, w, 4), dtype=np.float32), g


# ------------------------------------------------------------------------------------------------------------------ the wing
def wing_sheet(w, h, pal, seed, split):
    """The whole wing membrane as ONE picture (leading edge along the top, the body at the left, the wrist at x = split * w), cut into the inner and the outer panel by the caller. Four finger spars
    fan from the wrist to the tips, the membrane hangs between them in scalloped arcs, thin veins branch off each spar, the edge is outlined. Transparent outside."""
    rng = np.random.default_rng(seed)
    wx = w * split
    tips = [(w * 0.985, h * 0.10), (w * 0.88, h * 0.44), (w * 0.69, h * 0.80), (w * 0.52, h * 0.985)]
    hip = (w * 0.03, h * 0.86)
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)

    def arc(p, q, depth):
        mx, my = (p[0] + q[0]) / 2, (p[1] + q[1]) / 2
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = math.hypot(dx, dy) or 1.0
        cx, cy = mx + (-dy / n) * -depth * n, my + (dx / n) * -depth * n
        cx, cy = mx + ((wx - mx) * 0.0 + (wx * 0.0)), my                      # control on the chord, then pulled toward the wrist below
        cx = mx + (wx - mx) * 0.22
        cy = my + (6 - my) * 0.22
        pts = []
        for t in np.linspace(0, 1, 18):
            pts.append(((1 - t) ** 2 * p[0] + 2 * (1 - t) * t * cx + t * t * q[0], (1 - t) ** 2 * p[1] + 2 * (1 - t) * t * cy + t * t * q[1]))
        return pts

    poly = [(0.0, 3.0), (wx, 3.0), tips[0]]
    for a, b in zip(tips[:-1], tips[1:]):
        poly += arc(a, b, 0.0)[1:]
    poly += arc(tips[-1], hip, 0.0)[1:]
    poly += [(0.0, h * 0.86)]
    mem = canvas(w, h)
    dm = ImageDraw.Draw(mem)
    dm.polygon([(x * SS, y * SS) for x, y in poly], fill=col(pal["membrane"]))
    marr = done(mem, w, h)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    rr = np.hypot(xx - wx, yy - 3.0) / (w * 0.7)
    mc = rgb(pal["membrane"]) + (rgb(pal["membrane2"]) - rgb(pal["membrane"])) * smoothstep(rr, 0.15, 1.0)[..., None]
    mc = mc * (0.95 + 0.10 * fbm(rng, w, h, 3, 4)[..., None])
    out = np.concatenate([mc, marr[..., 3:4]], axis=-1)
    # veins and spars
    vein = canvas(w, h)
    dv = ImageDraw.Draw(vein)
    spar_w = max(2.2, w * 0.012)
    for k, t in enumerate(tips):
        for j in range(5):                                                      # thin side veins off the spar
            f = 0.22 + 0.15 * j
            bx, by = wx + (t[0] - wx) * f, 3 + (t[1] - 3) * f
            nx, ny = (tips[min(k + 1, 3)][0] - t[0]), (tips[min(k + 1, 3)][1] - t[1])
            ln = math.hypot(nx, ny) or 1.0
            L = (0.22 + 0.10 * rng.random()) * ln
            dv.line([(bx * SS, by * SS), ((bx + nx / ln * L + (wx - bx) * 0.04) * SS, (by + ny / ln * L) * SS)], fill=col(pal["vein"], 170), width=int(1.0 * SS))
    for t in tips:
        dv.line([(wx * SS, 3 * SS), (t[0] * SS, t[1] * SS)], fill=col(pal["outline"]), width=int((spar_w + 1.6) * SS))
        dv.line([(wx * SS, 3 * SS), (t[0] * SS, t[1] * SS)], fill=col(pal["bone"]), width=int(spar_w * SS))
    dv.line([(0, 3 * SS), (wx * SS, 3 * SS)], fill=col(pal["outline"]), width=int((spar_w + 2.4) * SS))
    dv.line([(0, 3 * SS), (wx * SS, 3 * SS)], fill=col(pal["bone"]), width=int((spar_w + 0.8) * SS))
    edge = canvas(w, h)
    de = ImageDraw.Draw(edge)
    de.line([(x * SS, y * SS) for x, y in poly[2:]], fill=col(pal["outline"], 230), width=int(1.7 * SS))
    out = over(out, done(vein, w, h))
    out = over(out, done(edge, w, h))
    out[..., 3] = np.minimum(out[..., 3], np.maximum(marr[..., 3], done(vein, w, h)[..., 3]))      # nothing paints outside the wing
    out[..., :3] = np.where(out[..., 3:4] > 0, out[..., :3], 0.0)
    return out, np.zeros((h, w, 4), dtype=np.float32)
