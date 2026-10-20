"""Skin painters of the God melee (a) upgrade: hammered / worn metal, bevel and ambient occlusion, engraved lore glyphs, energy veins, rune plates.

All of them work on a UV group through the painter context of tools/relics/new197_lib.py (`PC`: pc.img / pc.glow float32 RGBA views of the skin and of the glow map, pc.X / pc.Y the
model coordinates in centimetres of every texel, pc.face True on a flat face group). Deterministic: every random draw comes from a seeded generator.
"""
import math
import zlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import ukit as U

L = U.L
w = U.w
ml = U.ml

METALS = ("steel", "iron", "silver", "gold", "bronze", "obsidian", "stone")


def noise_a(shape, cx, cy, seed):
    """Smooth value noise in 0..1 with a different feature size across (cx px) and down (cy px)."""
    rng = np.random.default_rng(seed)
    h, wd = shape
    g = rng.random((max(2, int(h / cy) + 2), max(2, int(wd / cx) + 2)), dtype=np.float32)
    return np.clip(np.asarray(Image.fromarray(g).resize((wd, h), Image.BICUBIC)), 0, 1)


def border_dist(h, wd):
    yy, xx = np.mgrid[0:h, 0:wd]
    return np.minimum(np.minimum(xx, wd - 1 - xx), np.minimum(yy, h - 1 - yy)).astype(np.float32)


def seed_of(part, group_key=0):
    return (zlib.crc32(part.name.encode()) + 31 * int(group_key if isinstance(group_key, int) else 0)) & 0x7FFFFFFF


# ================================================================ generic detail: hammered metal, wear, bevel / AO, scratches
def detail_part(atlas, part, strength=1.0, scratches=True, bevel=True):
    """Fine detail over everything the base materials give: dents, pits, ambient occlusion at the borders of the plates, a light bevel line inside the border, scratches."""
    mat = part.material
    if part.layer == 1:
        return
    sd = seed_of(part)

    def fn(pc):
        img = pc.img
        h, wd = img.shape[:2]
        if h < 5 or wd < 5:
            return
        kk = pc.c.group.key
        s = (sd + (int(kk[0]) if isinstance(kk, tuple) else (kk & 0xFFFF)) * 17 + (h * 7 + wd)) & 0x7FFFFFFF
        if mat in METALS:
            dent = noise_a((h, wd), 5.0, 5.0, s) * 2 - 1
            fine = noise_a((h, wd), 1.4, 1.4, s + 1) * 2 - 1
            img[..., :3] *= (1.0 + strength * (0.075 * dent + 0.035 * fine))[..., None]
            pit = noise_a((h, wd), 1.8, 1.8, s + 2)
            img[..., :3] *= (1.0 - strength * 0.2 * (pit > 0.93))[..., None]
        elif mat == "wood":
            gr = noise_a((h, wd), 1.5, 14.0, s) * 2 - 1
            img[..., :3] *= (1.0 + strength * 0.12 * gr)[..., None]
        elif mat in ("leather", "cloth", "bone"):
            gr = noise_a((h, wd), 1.6, 1.6, s) * 2 - 1
            img[..., :3] *= (1.0 + strength * 0.05 * gr)[..., None]
        elif mat == "crystal":
            fa = noise_a((h, wd), 6.0, 14.0, s) * 2 - 1
            img[..., :3] *= (1.0 + strength * 0.10 * fa)[..., None]
        if bevel and min(h, wd) >= 9:
            bd = border_dist(h, wd)
            ao = np.clip((3.0 - bd) / 3.0, 0, 1)
            img[..., :3] *= (1.0 - strength * 0.30 * ao)[..., None]
            if mat in METALS:
                hi = np.exp(-((bd - 4.0) / 1.1) ** 2)
                img[..., :3] += (strength * 20.0 * hi)[..., None]
        if scratches and mat in METALS and h * wd > 1500:
            rng = np.random.default_rng(s + 5)
            mk = Image.new("L", (wd, h), 0)
            dr = ImageDraw.Draw(mk)
            for _ in range(int(min(40, h * wd / 5000))):
                x0, y0 = rng.uniform(0, wd), rng.uniform(0, h)
                ang = rng.normal(math.pi / 2 if h >= wd else 0.0, 0.25)
                ln = rng.uniform(5, 28)
                dr.line([(x0, y0), (x0 + ln * math.cos(ang), y0 + ln * math.sin(ang))], fill=int(rng.uniform(70, 170)), width=1)
            sc = np.asarray(mk, np.float32) / 255.0
            img[..., :3] += (strength * 55.0 * sc)[..., None] * 0.6
            img[..., :3] *= (1.0 - 0.08 * strength * sc)[..., None]
        np.clip(img[..., :3], 0, 255, out=img[..., :3])

    L.paint(atlas, part, fn)


def detail_all(m, atlas, skip=(), strength=1.0, scratches=True):
    for p in m.parts:
        if p.layer == 1 or any(k in p.name for k in skip):
            continue
        detail_part(atlas, p, strength, scratches)


# ================================================================ engraved lore glyphs
def glyph(ch):
    """The strokes of one letter of the weapon's alphabet: [((x0, y0), (x1, y1))] in a unit box (y down). Deterministic: the same letter always gives the same sign."""
    rng = np.random.default_rng(ord(ch) * 131 + 7)
    segs = []
    kind = int(rng.integers(0, 4))
    if kind != 3:
        segs.append(((0.5, 0.0), (0.5, 1.0)))
    for _ in range(int(rng.integers(1, 4))):
        y0 = float(rng.choice([0.0, 0.2, 0.4, 0.6]))
        y1 = y0 + float(rng.choice([0.25, 0.45]))
        side = float(rng.choice([-1, 1]))
        reach = float(rng.choice([0.5, 1.0]))
        segs.append(((0.5, y0), (0.5 + side * 0.42 * reach, min(1.0, y1))))
    if rng.random() < 0.35:
        yy = float(rng.choice([0.3, 0.55, 0.8]))
        segs.append(((0.12, yy), (0.88, yy)))
    if kind == 3:
        segs.append(((0.15, 0.1), (0.85, 0.9)))
        segs.append(((0.85, 0.1), (0.15, 0.9)))
    return segs


class Engraver:
    """Draws marks in model coordinates (cm) onto one UV group, then burns them into the skin (dark groove with a light rim) and, optionally, into the glow map."""

    def __init__(self, pc):
        self.pc = pc
        self.h, self.wd = pc.img.shape[:2]
        self.mask = Image.new("L", (self.wd, self.h), 0)
        self.dr = ImageDraw.Draw(self.mask)
        self.ppu = pc.c.group.ppu
        X, Y = pc.X, pc.Y
        self.x0, self.dx = float(X[0, 0]), float(X[0, 1] - X[0, 0]) if self.wd > 1 else 1.0
        self.y0, self.dy = float(Y[0, 0]), float(Y[1, 0] - Y[0, 0]) if self.h > 1 else -1.0
        if abs(self.dx) < 1e-9:
            self.dx = 1.0 / max(self.ppu, 1e-6)
        if abs(self.dy) < 1e-9:
            self.dy = -1.0 / max(self.ppu, 1e-6)

    def tx(self, x, y):
        return ((x - self.x0) / self.dx, (y - self.y0) / self.dy)

    def line(self, p0, p1, width_cm, level=255):
        a, b = self.tx(*p0), self.tx(*p1)
        self.dr.line([a, b], fill=level, width=max(1, int(round(width_cm * self.ppu))))

    def poly(self, pts, level=255):
        self.dr.polygon([self.tx(*p) for p in pts], fill=level)

    def circle(self, c, r, width_cm, level=255, a0=0, a1=360):
        a = self.tx(*c)
        rr = r * self.ppu
        self.dr.arc([a[0] - rr, a[1] - rr, a[0] + rr, a[1] + rr], a0, a1, fill=level, width=max(1, int(round(width_cm * self.ppu))))

    def dot(self, c, r, level=255):
        a = self.tx(*c)
        rr = max(0.8, r * self.ppu)
        self.dr.ellipse([a[0] - rr, a[1] - rr, a[0] + rr, a[1] + rr], fill=level)

    def legible(self, cw, ch, stroke):
        """A letter is never smaller than 7 x 9 texels with a stroke of at least 1.3 texels: on a coarse skin (a long staff) the same text is simply drawn larger."""
        return max(cw, 7.0 / self.ppu), max(ch, 9.0 / self.ppu), max(stroke, 1.3 / self.ppu)

    def text_col(self, text, x, y_top, cw, ch, stroke=0.12, gap=0.5, level=255):
        """A column of letters going down from y_top at x (centre), each cw x ch cm."""
        cw, ch, stroke = self.legible(cw, ch, stroke)
        y = y_top
        for c in text:
            if c != " ":
                for (a, b) in glyph(c):
                    self.line((x + (a[0] - 0.5) * cw, y - a[1] * ch), (x + (b[0] - 0.5) * cw, y - b[1] * ch), stroke, level)
            y -= ch * (1.0 + gap)
        return y

    def text_row(self, text, x_left, y, cw, ch, stroke=0.1, gap=0.35, level=255):
        cw, ch, stroke = self.legible(cw, ch, stroke)
        x = x_left
        for c in text:
            if c != " ":
                for (a, b) in glyph(c):
                    self.line((x + a[0] * cw, y - a[1] * ch), (x + b[0] * cw, y - b[1] * ch), stroke, level)
            x += cw * (1.0 + gap)
        return x

    def burn(self, dark=0.55, rim=0.22, glow_rgb=None, glow_strength=0.9, core=0.5, inside=None):
        m = np.asarray(self.mask.filter(ImageFilter.GaussianBlur(0.55)), np.float32) / 255.0
        if inside is not None:
            m = m * np.clip(inside, 0, 1)
        if m.max() <= 0.01:
            return 0.0
        pc = self.pc
        pc.img[..., :3] *= (1.0 - dark * m)[..., None]
        ring = np.asarray(self.mask.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(0.6)), np.float32) / 255.0
        if inside is not None:
            ring = ring * np.clip(inside, 0, 1)
        pc.img[..., :3] += (255.0 * rim * np.clip(ring - m, 0, 1))[..., None] * 0.55
        if glow_rgb is not None:
            L.put_glow(pc, m, glow_rgb, glow_strength, core)
        return float(m.sum())


def engrave_face(atlas, part, fn, only_faces=True):
    """Runs fn(Engraver, pc) on every flat face group (or every group) of a part."""
    def run(pc):
        if only_faces and not pc.face:
            return
        if pc.img.shape[0] < 6 or pc.img.shape[1] < 6:
            return
        fn(Engraver(pc), pc)
    L.paint(atlas, part, run)


# ================================================================ energy veins
def paint_veins(atlas, part, rgb, seed=3, density=0.9, cell=(4.0, 1.4), strength=0.85, core=0.3, mask_fn=None):
    """Thin branching veins (the ridges of a noise field, stretched along the part) into the CURRENT glow map (call it inside Ctx.on_veins to paint the second map).
    cell = (along, across) feature size in CENTIMETRES, so the look does not depend on the skin's resolution."""
    sd = (seed_of(part) + seed) & 0x7FFFFFFF

    def fn(pc):
        h, wd = pc.img.shape[:2]
        if h < 6 or wd < 6:
            return
        ppu = max(pc.c.group.ppu, 0.5)
        cy, cx = max(1.5, cell[0] * ppu), max(1.5, cell[1] * ppu)
        n = noise_a((h, wd), cx, cy, sd + h)
        n2 = noise_a((h, wd), cx * 0.5, cy * 0.5, sd + 99 + wd)
        ridge = 1.0 - np.abs(2 * (0.7 * n + 0.3 * n2) - 1.0)
        thr = 1.0 - 0.07 * density
        mk = np.clip((ridge - thr) / (1.0 - thr), 0, 1) ** 0.8
        if mask_fn is not None:
            mk = mk * mask_fn(pc)
        L.put_glow(pc, mk, rgb, strength, core)

    L.paint(atlas, part, fn)


def add_faint_core(atlas, ctx):
    """A veins texel only draws when its quad is in the glow layer, which the mesh writer decides from the CORE map: lay a nearly invisible core (alpha 3) under every vein."""
    va = ctx.veins[..., 3] > 4
    low = va & (atlas.glow[..., 3] < 3)
    atlas.glow[..., 3] = np.where(low, 3.0, atlas.glow[..., 3])
    atlas.glow[..., :3] = np.where(low[..., None], ctx.veins[..., :3], atlas.glow[..., :3])


class on_veins:
    """`with on_veins(atlas, ctx):` makes the glow painters write into the second emissive map instead of the core map."""

    def __init__(self, atlas, ctx):
        self.atlas, self.ctx = atlas, ctx

    def __enter__(self):
        self.atlas.glow, self.ctx.veins = self.ctx.veins, self.atlas.glow

    def __exit__(self, *a):
        self.atlas.glow, self.ctx.veins = self.ctx.veins, self.atlas.glow


CORE_K = 0.55           # the hot core of a glow stays this share of what the call asks for, so a palette keeps its colour instead of washing out to white


def glow_parts(atlas, m, names, rgb, strength=0.9, core=0.5):
    for nm in names:
        for p in m.parts:
            if p.name == nm:
                L.paint_glow(atlas, p, rgb, strength, core * CORE_K)


def glow_prefix(atlas, m, prefix, rgb, strength=0.9, core=0.5):
    for p in m.parts:
        if p.name.startswith(prefix):
            L.paint_glow(atlas, p, rgb, strength, core * CORE_K)


def tint_part(atlas, part, rgb, k=0.5):
    L.paint(atlas, part, lambda pc: w.tint(pc.img, rgb, k))
