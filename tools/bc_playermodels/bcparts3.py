"""Third set of parts: bat wings as shaped cards, a crown of gold spikes. Same conventions as bcparts.py (a `Build`, a layer)."""
import math

import numpy as np
from PIL import Image, ImageDraw

import bclib as L
from bclib import rgb, tone, hsh
import bcparts as P
from bcparts import HEAD


def m_wing(color="#241a34", vein="#6a4a8a", edge="#9a7ad0", tw=64, th=48, seed=0, fingers=4, eyes=False):
    """A bat wing as one tile: the root at the lower left, the arm up to the wrist, `fingers` finger bones fanning to the scalloped lower edge, a membrane between them, a lit leading edge. Alpha outside."""
    ss = 4
    W, H = tw * ss, th * ss
    root = (1.5 * ss, 41.0 * ss)
    wrist = (tw * 0.40 * ss, 5.0 * ss)
    tips = []
    for i in range(fingers):
        k = i / max(1.0, fingers - 1.0)
        ang = math.radians(-62 + 148 * k)                 # from up-right round to down-left
        ln = (tw * 0.62) * (1.0 - 0.22 * abs(k - 0.35))
        tips.append((wrist[0] + math.cos(ang) * ln * ss * 0.95 * (0.95 + 0.1 * hsh(i, seed)), wrist[1] - math.sin(ang) * ln * ss * 0.55 * -1.0))
    tips = [(min(W - 2 * ss, max(2 * ss, x)), min(H - 2 * ss, max(4 * ss, y))) for x, y in tips]
    tips.sort(key=lambda p: math.atan2(p[1] - wrist[1], p[0] - wrist[0]))
    body_low = (3.0 * ss, 46.0 * ss)
    pts = [root, wrist]
    prev = wrist
    for i, t in enumerate(tips):
        pts.append(t)
        if i + 1 < len(tips):
            n = tips[i + 1]
            mid = ((t[0] + n[0]) / 2.0, (t[1] + n[1]) / 2.0)
            pull = 0.30
            pts.append((mid[0] + (wrist[0] - mid[0]) * pull, mid[1] + (wrist[1] - mid[1]) * pull))
    last = tips[-1]
    mid = ((last[0] + body_low[0]) / 2.0, (last[1] + body_low[1]) / 2.0)
    pts.append((mid[0] + (wrist[0] - mid[0]) * 0.18, mid[1] + (wrist[1] - mid[1]) * 0.18))
    pts.append(body_low)
    mask = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(mask)
    d.polygon(pts, fill=255)
    m = np.asarray(mask.resize((tw, th), Image.BOX), np.float32) / 255.0
    img = np.zeros((th, tw, 4), np.float32)
    ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
    dist = np.sqrt((xs - wrist[0] / ss) ** 2 + (ys - wrist[1] / ss) ** 2) / (tw * 0.7)
    base = rgb(color)
    ec = rgb(edge)
    k = np.clip(dist, 0, 1)[..., None]
    col = base * (1.0 - 0.25 * k) + ec * 0.30 * k ** 2
    img[..., :3] = col
    img[..., 3] = (m > 0.5).astype(np.float32)
    # bones: the arm and the fingers, drawn as lit lines
    bone = Image.new("L", (W, H), 0)
    bd = ImageDraw.Draw(bone)
    lw = int(1.9 * ss)
    bd.line([root, wrist], fill=255, width=int(lw * 1.35))
    for t in tips:
        bd.line([wrist, t], fill=255, width=lw)
    bm = np.asarray(bone.resize((tw, th), Image.BOX), np.float32) / 255.0
    bm = bm * img[..., 3]
    vc = rgb(vein)
    img[..., :3] = img[..., :3] * (1 - bm[..., None]) + vc[None, None, :] * bm[..., None]
    lit = np.zeros_like(bm)
    lit[:-1] = np.clip(bm[:-1] - bm[1:], 0, 1)
    img[..., :3] = np.clip(img[..., :3] + lit[..., None] * ec * 0.6, 0, 1)
    if eyes:
        for ex, ey in ((tw * 0.45, th * 0.45), (tw * 0.62, th * 0.62), (tw * 0.32, th * 0.7)):
            em = ((xs - ex) / 2.6) ** 2 + ((ys - ey) / 1.6) ** 2 < 1.0
            img[em, :3] = (0.95, 0.85, 0.9)
            pm = ((xs - ex) / 0.9) ** 2 + ((ys - ey) / 1.5) ** 2 < 1.0
            img[pm, :3] = (0.8, 0.1, 0.2)
    return L._edge_outline(img, tone(color, 0.35), 0.7)


def wing(b, color="#1a1228", vein="#6a4a9a", edge="#9a7ad0", span=26.0, layer="main", y=22.0, side_offset=2.2, z=2.8, sweep=38.0, rise=14.0, fingers=4, pair=1, seed=13, sway=4.0, layers_=2, eyes=False, aspect=0.75):
    """A pair of bat wings: per side `layers_` cards (the inner one a little smaller and more swept back) of the bat-wing tile, hung from the shoulder blade and flapping gently (a bone per side)."""
    geo = b.g(layer)
    for s in (-1, 1):
        nm = "wing%d_%s" % (pair, "l" if s > 0 else "r")
        bone = b.sway(layer, "body", nm, (s * side_offset, y, z), ax=1.0, az=sway, walk=3.0, run=5.0, cast=3.0, phase=0.0 if s > 0 else math.pi)
        for k in range(layers_):
            w = span * (1.0 - 0.22 * k)
            h = w * aspect
            tile = b.atlas.tile(("wing", L.hexs(rgb(color)), L.hexs(rgb(vein)), L.hexs(rgb(edge)), fingers, seed, eyes), 64, 48, lambda: m_wing(color, vein, edge, 64, 48, seed, fingers, eyes))
            x0 = s * side_offset if s > 0 else s * side_offset - w
            y0 = y - h * 0.86 + 1.0 + k * 0.3
            piv = (s * side_offset, y, z + 0.2 * k)
            geo.cube(bone, (x0, y0, z + 0.2 * k), (w, h, 0.0), tile, faces=("north",), rot=(0.0, s * (sweep + 14.0 * k), s * (rise - 3.0 * k)), pivot=piv, mirror_uv=(s < 0))


def crown(b, gold="#e8b830", gem="#d81c40", spikes=5, y=32.45, tall=3.6, width=9.7, layer="main", seed=8, gems=True, band_h=1.1, point_w=2.0):
    """A gold crown: a band round the head and `spikes` pointed gold spikes (crossed cards) standing on its front half, with a gem at the foot of each (the gems in the fx layer shine)."""
    geo = b.g(layer)
    metal = b.t_metal(gold, 16, 16, seed=seed)
    w = width
    d = width
    geo.cube(HEAD, (-w / 2, y, -d / 2), (w, band_h, 1.0), metal)
    geo.cube(HEAD, (-w / 2, y, d / 2 - 1.0), (w, band_h, 1.0), metal)
    for s in (-1, 1):
        geo.cube(HEAD, (s * (w / 2 - 0.5) - 0.5, y, -d / 2 + 1.0), (1.0, band_h, d - 2.0), metal)
    sp = b.t_spike(gold, tw=12, th=20, seed=seed + 3, tip="#fff2b0")
    xs = [(-0.5 + i / (spikes - 1.0)) * (w - 2.4) for i in range(spikes)]
    for i, x in enumerate(xs):
        hh = tall * (1.0 - abs(x) / (w / 2) * 0.32)
        z = -d / 2 + 0.5
        piv = (x, y + band_h - 0.2, z)
        geo.cube(HEAD, (x - point_w / 2, y + band_h - 0.2, z), (point_w, hh, 0.0), sp, faces=("north",), pivot=piv)
        geo.cube(HEAD, (x, y + band_h - 0.2, z - point_w / 2), (0.0, hh, point_w), sp, faces=("east",), pivot=piv)
        if gems:
            b.g("fx").cube(HEAD, (x - 0.55, y + 0.1, -d / 2 - 0.35), (1.1, 0.9, 0.5), b.t_gem(gem), faces=("north", "up"))
    for s in (-1, 1):
        for j in range(2):
            x = s * (1.4 + j * 2.2)
            z = d / 2 - 0.5
            piv = (x, y + band_h - 0.2, z)
            geo.cube(HEAD, (x - point_w / 2, y + band_h - 0.2, z), (point_w, tall * 0.8, 0.0), sp, faces=("north",), pivot=piv)
            geo.cube(HEAD, (x, y + band_h - 0.2, z - point_w / 2), (0.0, tall * 0.8, point_w), sp, faces=("east",), pivot=piv)
