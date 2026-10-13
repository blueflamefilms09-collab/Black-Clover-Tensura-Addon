"""Book of Mispells (card: Mr James Gifford, "Book of Mispells", rare): a thick blue-teal spellbook with gold corner caps and a gold clasp, the title
"Book of Spell" in wobbly letters, dark tattered spiky scorch / ink damage on the cover, and ribbon bookmarks in purple, green and tan. The 3D model,
built with the owner's method (stood upright, spine on the left, face +z).

Reference: the card, 750 x 708; the book sits tilted in the middle. Measured on its cover: 167 px tall by 104 wide, pages and clasp 36 px thick (card px).
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
H_PX, W_PX, T_PX = 167.0, 104.0, 36.0                # the book on the card, in card px (cover height, width, thickness)
H_CM = 28.0                                          # a large spellbook, 28 cm tall
K = w.px_to_cm(H_CM, H_PX)                           # cm per card px
W = W_PX * K                                         # 17.4 cm
T = 6.5                                              # thick, about 6.5 cm (the clasp side shows 36 px on the card, seen at an angle)
COVER_T = 0.6                                        # a cover board
OVERHANG = 0.6                                       # the boards stand this far beyond the pages
BORDER_INSET, BORDER_W, BORDER_H = 2.0, 0.32, 0.12   # the raised inner border on the front cover
CORNER = 3.2                                         # a corner cap's leg length
LOCK_R = 2.2                                         # the round lock of the clasp (card diameter 40 px of the 104 wide cover is about 17 cm: scaled)
STRAPS = (0.28, 0.72)                                # where the two clasp straps cross the fore-edge (share of the height)
FRONT = T / 2


def build():
    m = ml.Mesh("book_of_mispells")
    x0, x1 = -W / 2, W / 2
    # ---- 1 front cover, 2 back cover: boards with chamfered edges
    fc = w.add(m, "front_cover", "leather", base=(40, 110, 128), smooth=30, wear=0.6)
    ml.box(fc, (x0 - OVERHANG * 0.2, 0.0, FRONT - COVER_T), (x1 + OVERHANG, H_CM, FRONT), chamfer=0.25)
    bc = w.add(m, "back_cover", "leather", base=(36, 98, 116), smooth=30, wear=0.6)
    ml.box(bc, (x0 - OVERHANG * 0.2, 0.0, -FRONT), (x1 + OVERHANG, H_CM, -FRONT + COVER_T), chamfer=0.25)
    # ---- 3 the page block, with 4 the spine
    pg = w.add(m, "pages", "bone", base=(232, 220, 190), smooth=20, wear=0.3)
    ml.box(pg, (x0 + 0.7, OVERHANG, -FRONT + COVER_T), (x1 - 0.2, H_CM - OVERHANG, FRONT - COVER_T), chamfer=0.12)
    sp = w.add(m, "spine", "leather", base=(34, 94, 112), smooth=45, uv_mode="loft")
    ml.loft(sp, "y", (x0 - 0.05, 0, 0), [0.0, H_CM], ml.rounded_rect(1.4, T, 0.65, 3))
    # ---- 5 spine bands: three gold ridges across it
    sb = w.add(m, "spine_bands", "gold", base=(214, 178, 84))
    for f in (0.12, 0.5, 0.88):
        ml.collar(sb, H_CM * f - 0.3, 1.0, 0.7, lip=0.15, sides=12, origin=(x0 - 0.05, 0, 0), axis="y", oval=T / 2.0)
    # ---- 6-9 the four gold corner caps on the front: triangular plates with a gem each
    caps = w.add(m, "corner_caps", "gold", base=(212, 170, 78), smooth=35, wear=0.7)
    tri = lambda sx, sy: [(0.0, 0.0), (sx * CORNER, 0.0), (0.0, sy * CORNER)]
    for (cx, cy, sx, sy) in ((x0 - 0.1, H_CM, 1, -1), (x1 + OVERHANG, H_CM, -1, -1), (x0 - 0.1, 0.0, 1, 1), (x1 + OVERHANG, 0.0, -1, 1)):
        pts = tri(sx, sy) if sx * sy < 0 else tri(sx, sy)[::-1]
        ml.loft(caps, "z", (cx, cy, FRONT - 0.02), [0.0, 0.32], pts)
    gems = w.add(m, "corner_gems", "crystal", base=(90, 170, 240), glow=True, smooth=0)
    for (cx, cy, sx, sy) in ((x0, H_CM, 1, -1), (x1 + OVERHANG, H_CM, -1, -1), (x0, 0.0, 1, 1), (x1 + OVERHANG, 0.0, -1, 1)):
        ml.gem(gems, (cx + sx * 0.95, cy + sy * 0.95, FRONT + 0.42), 0.34, long=1.0, sides=6, axis="z")
    # ---- 10 the raised border frame on the front cover (four bars)
    fr = w.add(m, "border_frame", "gold", base=(200, 168, 96), wear=0.7)
    a, b = BORDER_INSET, BORDER_INSET
    pts = [(x0 + a + 0.4, a, FRONT), (x1 - b + 0.2, a, FRONT), (x1 - b + 0.2, H_CM - a, FRONT), (x0 + a + 0.4, H_CM - a, FRONT)]
    for i in range(4):
        ml.bar(fr, pts[i], pts[(i + 1) % 4], BORDER_W, BORDER_H, up=(0, 0, 1), taper=0.0, section="rect")
    # ---- 11 the clasp: two gold straps over the fore-edge, 12 the lock wheel, 13 its spokes
    st = w.add(m, "clasp_straps", "gold", base=(206, 170, 80), smooth=25, wear=0.6)
    for f in STRAPS:
        yy = H_CM * f
        ml.box(st, (x1 - 3.4, yy - 1.0, FRONT), (x1 + OVERHANG + 0.25, yy + 1.0, FRONT + 0.3), chamfer=0.1)
        ml.box(st, (x1 + OVERHANG, yy - 1.0, -FRONT - 0.3), (x1 + OVERHANG + 0.25, yy + 1.0, FRONT + 0.3), chamfer=0.05)
        ml.box(st, (x1 - 3.4, yy - 1.0, -FRONT - 0.3), (x1 + OVERHANG + 0.25, yy + 1.0, -FRONT), chamfer=0.1)
    lk = w.add(m, "lock_wheel", "gold", base=(222, 184, 92), smooth=50)
    lc = (x1 - 0.7, H_CM * 0.5, FRONT + 0.2)
    ml.lathe(lk, "z", lc, [(0.0, 1.5), (0.25, LOCK_R), (0.55, LOCK_R * 0.96), (0.8, LOCK_R * 0.7), (1.0, 0.0)], sides=20)
    sk = w.add(m, "lock_spokes", "bronze", base=(150, 110, 60), wear=0.5)
    for i in range(8):
        a_ = math.radians(i * 45)
        ml.bar(sk, (lc[0], lc[1], lc[2] + 0.8), (lc[0] + 1.5 * math.cos(a_), lc[1] + 1.5 * math.sin(a_), lc[2] + 0.8), 0.18, 0.08, up=(0, 0, 1))
    lg = w.add(m, "lock_gem", "crystal", base=(255, 210, 120), glow=True, smooth=0)
    ml.gem(lg, (lc[0], lc[1], lc[2] + 1.0), 0.38, long=1.0, sides=6, axis="z")
    # ---- 14 the scorch spikes: dark tattered cones standing out of the cover's right-centre
    sc = w.add(m, "scorch_spikes", "obsidian", base=(24, 22, 36), wear=0.2, smooth=20)
    rng = np.random.default_rng(11)
    for i in range(9):
        px = rng.uniform(-1.0, 6.0)
        py = rng.uniform(5.0, 17.0)
        h = rng.uniform(1.4, 3.8)
        lean = rng.uniform(-0.5, 0.5)
        ml.loft(sc, (lean, 0.25, 1.0), (px, py, FRONT), [(0.0, 1.0, 1.0), (h, 0.0, 0.0)], ml.circle(rng.uniform(0.28, 0.5), 5))
    # ---- 15-17 the ribbon bookmarks hanging off the bottom
    for nm, col, xx, ln in (("ribbon_purple", (120, 78, 170), -4.0, 9.0), ("ribbon_green", (96, 140, 90), -0.5, 7.0), ("ribbon_tan", (190, 168, 124), 3.2, 5.6)):
        rb = w.add(m, nm, "cloth", base=col, smooth=30, uv_mode="loft", wear=0.2)
        pts = [(xx + 0.6 * math.sin(t * 2.3), -ln * t + 0.4, 0.2 + 0.6 * t) for t in np.linspace(0, 1, 9)]
        ml.sweep(rb, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.7 * (1.0 - 0.15 * t) if t < 0.96 else 0.0, 0.12 if t < 0.96 else 0.0))

    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("3873c6c0917bcf1049094f451ecb5396 (1).jpg", (230, 70, 520, 330), "book_of_mispells")
    w.finish("book_of_mispells", m, atlas, real_m=0.30, fit=True, glow=("FFFFFF", 0.16, 0.5, 1.0), ref=ref)


def _paint(m, atlas):
    def cover(c):                                                      # the front cover: teal at the lower left, purple toward the upper right, a rune border, the title, the scorch
        if not (c.kind == "dom" and c.g.key[0] == 2 and c.g.key[1] > 0):
            return
        h, wd = c.img.shape[:2]
        yy, xx = np.mgrid[0:h, 0:wd].astype(np.float32)
        u, v = xx / wd, yy / h
        mix = np.clip(1.4 * (u * 0.6 + (1 - v) * 0.5) - 0.45, 0, 1)[..., None]
        teal, purple = np.array([42, 112, 130], np.float32), np.array([84, 72, 138], np.float32)
        lum = c.img[..., :3].mean(axis=2, keepdims=True) / 110.0
        c.img[..., :3] = (teal * (1 - mix) + purple * mix) * np.clip(lum, 0.6, 1.3)
        im = Image.new("RGBA", (wd, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        gim = Image.new("RGBA", (wd, h), (0, 0, 0, 0))
        gd = ImageDraw.Draw(gim)
        pad = int(2.4 * c.g.ppu)
        rng = np.random.default_rng(3)
        for i in range(60):                                            # tiny rune ticks along the inner border
            side = i % 4
            t = rng.uniform(0.05, 0.95)
            if side == 0:
                p = (pad + t * (wd - 2 * pad), pad * 0.55)
            elif side == 1:
                p = (wd - pad * 0.55, pad + t * (h - 2 * pad))
            elif side == 2:
                p = (pad + t * (wd - 2 * pad), h - pad * 0.55)
            else:
                p = (pad * 0.55, pad + t * (h - 2 * pad))
            L = max(2, int(0.5 * c.g.ppu))
            q = (p[0] + (L if side % 2 == 0 else 0), p[1] + (L if side % 2 == 1 else 0))
            d.line([p, q], fill=(210, 230, 240, 255), width=1)
            gd.line([p, q], fill=(160, 210, 255, 200), width=1)
        try:
            font = ImageFont.load_default(size=max(9, int(2.6 * c.g.ppu)))
        except TypeError:
            font = ImageFont.load_default()
        for k, (txt, fy, dx) in enumerate((("BOoKe", 0.15, -1), ("of", 0.25, 2), ("sPeL", 0.34, -3))):   # the wobbly title
            d.text((wd * 0.30 + dx * c.g.ppu * 0.2, h * fy), txt, fill=(240, 226, 170, 255), font=font)
            gd.text((wd * 0.30 + dx * c.g.ppu * 0.2, h * fy), txt, fill=(255, 230, 150, 120), font=font)
        poly_pts = []
        cx0, cy0 = wd * 0.56, h * 0.66
        for i in range(24):                                            # the scorch: a jagged dark blotch
            a = 2 * math.pi * i / 24
            r = (0.07 + (0.07 if i % 3 == 0 else 0.0) + 0.05 * rng.random()) * wd
            poly_pts.append((cx0 + r * math.cos(a) * 0.9, cy0 + r * math.sin(a) * 1.5))
        d.polygon(poly_pts, fill=(22, 20, 34, 255))
        ov = np.asarray(im, np.float32)
        al = ov[..., 3:4] / 255.0
        c.img[..., :3] = c.img[..., :3] * (1 - al) + ov[..., :3] * al
        go = np.asarray(gim, np.float32)
        c.glow[...] = np.maximum(c.glow, go)
    w.paint_part(atlas, m.part("front_cover"), cover)

    def pages(c):                                                      # page edges: fine horizontal lines
        n = np.sin(c.B * 9.0 + c.A * 0.2)[..., None] * 10
        c.img[..., :3] = np.clip(c.img[..., :3] - 14 + n, 0, 255)
    w.paint_part(atlas, m.part("pages"), pages)
    w.paint_part(atlas, m.part("corner_gems"), lambda c: w.set_glow(c, (110, 190, 255), 1.0, core=0.4))
    w.paint_part(atlas, m.part("lock_gem"), lambda c: w.set_glow(c, (255, 210, 120), 1.0, core=0.4))
