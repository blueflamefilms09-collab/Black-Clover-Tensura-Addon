#!/usr/bin/env python3
"""Textured software renderer of the Beast Will cards on a Minecraft player (orthographic, painter's order + depth test, glow bloom).
Reads the same Card list the geo export writes, so what you see is what the game draws."""
import math, sys
import numpy as np
from PIL import Image, ImageFilter, ImageDraw
sys.dont_write_bytecode = True
from forms_lib import Card, V, unit

SKIN = (221, 168, 128); SHIRT = (52, 156, 168); PANTS = (62, 58, 140); HAIR = (60, 38, 24); SHOE = (70, 70, 74)


def player_cards(skin=SKIN, shirt=SHIRT, pants=PANTS, hair=HAIR, head=True, lean=0.0):
    """the standard player (head 8, body 8x12x4, arms/legs 4x12x4) as solid cards in the file frame"""
    out = []

    def box(lo, hi, col, face_cols=None, part="player"):
        x0, y0, z0 = lo; x1, y1, z1 = hi
        cx, cy, cz = (x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2; sx, sy, sz = x1 - x0, y1 - y0, z1 - z0
        faces = [((cx, cy, z0), (1, 0, 0), (0, 1, 0), sx, sy, 0), ((cx, cy, z1), (1, 0, 0), (0, 1, 0), sx, sy, 1),
                 ((x0, cy, cz), (0, 0, 1), (0, 1, 0), sz, sy, 2), ((x1, cy, cz), (0, 0, 1), (0, 1, 0), sz, sy, 3),
                 ((cx, y1, cz), (1, 0, 0), (0, 0, 1), sx, sz, 4), ((cx, y0, cz), (1, 0, 0), (0, 0, 1), sx, sz, 5)]
        shade = (1.0, .62, .8, .8, 1.12, .5)
        for c, r, u, w, h, k in faces:
            cc = np.clip(np.array(col if face_cols is None or k not in face_cols else face_cols[k], float) * shade[k], 0, 255)
            out.append(Card(c, r, u, w, h, "", None, part, "root", solid=cc))
    box((-4, 12, -2), (4, 24, 2), shirt)
    box((-4, 0, -2), (0, 12, 2), pants); box((0, 0, -2), (4, 12, 2), pants)
    box((-4, 0, -2), (0, 1.6, 2), SHOE); box((0, 0, -2), (4, 1.6, 2), SHOE)
    box((4, 12, -2), (8, 24, 2), shirt, {1: skin, 0: skin, 2: skin, 3: skin, 5: skin}); box((-8, 12, -2), (-4, 24, 2), shirt, {1: skin, 0: skin, 2: skin, 3: skin, 5: skin})
    for sx in (4, -8):
        box((sx, 12, -2), (sx + 4, 17, 2), skin)
        box((sx, 18.5, -2), (sx + 4, 24, 2), shirt)
    if head:
        box((-4, 24, -4), (4, 32, 4), skin, {4: hair, 0: skin})
        # eyes + mouth on the face (front, -z)
        for ex in (-2.4, 1.4):
            out.append(Card((ex + .8, 28.1, -4.02), (1, 0, 0), (0, 1, 0), 1.6, 1.0, "", None, "player", "root", solid=np.array([40, 60, 120.])))
        out.append(Card((0, 25.8, -4.02), (1, 0, 0), (0, 1, 0), 2.4, .6, "", None, "player", "root", solid=np.array([140, 80, 70.])))
        box((-4.2, 29.6, -4.2), (4.2, 32.2, 4.2), hair)
    return out


def _rot(yaw, pitch):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return Rx @ Ry


def render(cards, sheet, yaw=0, pitch=8, W=300, H=420, scale=9.2, target=(0, 16, 0), SS=2, bloom=1.0, bg=(26, 24, 34), glow_k=1.0):
    R = _rot(yaw, pitch)
    Ws, Hs = W * SS, H * SS; s = scale * SS
    col = np.zeros((Hs, Ws, 3)); col[:] = np.array(bg, float)
    # a soft floor shadow + gradient for depth
    gy = np.linspace(0, 1, Hs)[:, None, None]
    col = col * (0.82 + 0.38 * (1 - gy))
    zbuf = np.full((Hs, Ws), 1e9); gl = np.zeros((Hs, Ws, 3))
    tgt = V(*target)
    items = []
    for c in cards:
        cc = R @ (c.c - tgt); rr = R @ c.r * c.w / 2; uu = R @ c.u * c.h / 2
        items.append((cc[2], cc, rr, uu, c))
    items.sort(key=lambda t: -t[0])          # far (large z) first; viewer looks along +z so nearer = smaller z
    for z, cc, rr, uu, c in items:
        P0 = np.array([cc[0] * s + Ws / 2, Hs / 2 - cc[1] * s])
        sR = np.array([rr[0] * s, -rr[1] * s]); sU = np.array([uu[0] * s, -uu[1] * s])
        det = sR[0] * sU[1] - sR[1] * sU[0]
        if abs(det) < 1e-3: continue
        xs = [P0[0] + a * sR[0] + b * sU[0] for a in (-1, 1) for b in (-1, 1)]
        ys = [P0[1] + a * sR[1] + b * sU[1] for a in (-1, 1) for b in (-1, 1)]
        x0, x1 = max(int(min(xs)) - 1, 0), min(int(max(xs)) + 2, Ws); y0, y1 = max(int(min(ys)) - 1, 0), min(int(max(ys)) + 2, Hs)
        if x1 <= x0 or y1 <= y0: continue
        gy_, gx_ = np.mgrid[y0:y1, x0:x1]
        dx = gx_ + .5 - P0[0]; dy = gy_ + .5 - P0[1]
        a = (dx * sU[1] - dy * sU[0]) / det; b = (sR[0] * dy - sR[1] * dx) / det
        inside = (np.abs(a) <= 1) & (np.abs(b) <= 1)
        if not inside.any(): continue
        uu_ = (a + 1) / 2; vv_ = (1 - b) / 2
        zz = cc[2] + a * rr[2] + b * uu[2]
        if c.solid is not None:
            rgb = np.broadcast_to(c.solid, a.shape + (3,)); al = np.ones(a.shape); g = np.zeros(a.shape + (3,))
        else:
            tex, gtex = sheet.sample(c.region, np.clip(uu_, 0, .9999), np.clip(vv_, 0, .9999), c.win)
            rgb = tex[..., :3]; al = tex[..., 3] / 255.0; g = gtex[..., :3] * (gtex[..., 3:4] / 255.0) * glow_k
        ok = inside & (zz < zbuf[y0:y1, x0:x1] + 0.02) & (al > .02)
        if not ok.any(): continue
        sub = col[y0:y1, x0:x1]; aa = (al * ok)[..., None]
        sub[:] = sub * (1 - aa) + rgb * aa
        gsub = gl[y0:y1, x0:x1]; gsub[:] = gsub * (1 - aa) + g * aa + g * aa * .0
        solid = ok & (al > .5)
        zsub = zbuf[y0:y1, x0:x1]; zsub[solid] = zz[solid]
    img = np.clip(col + gl * .55, 0, 255)
    im = Image.fromarray(img.astype(np.uint8), "RGB")
    if bloom > 0:
        gim = Image.fromarray(np.clip(gl * 1.4, 0, 255).astype(np.uint8), "RGB")
        b1 = np.asarray(gim.filter(ImageFilter.GaussianBlur(4 * SS)), float); b2 = np.asarray(gim.filter(ImageFilter.GaussianBlur(11 * SS)), float)
        im = Image.fromarray(np.clip(np.asarray(im, float) + (b1 * .55 + b2 * .55) * bloom, 0, 255).astype(np.uint8), "RGB")
    return im.resize((W, H), Image.LANCZOS)


def sheet_of(rows, labels, path, cols=4, cw=300, ch=420):
    n = len(rows); rws = (n + cols - 1) // cols
    out = Image.new("RGB", (cols * cw, rws * ch), (20, 18, 26)); d = ImageDraw.Draw(out)
    for i, (im, lb) in enumerate(zip(rows, labels)):
        x, y = (i % cols) * cw, (i // cols) * ch
        out.paste(im, (x, y)); d.text((x + 6, y + 4), lb, fill=(235, 235, 245))
    out.save(path)
