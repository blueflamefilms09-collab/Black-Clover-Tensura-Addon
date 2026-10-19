"""Shared kit of the 1.93.0 magic rebuild generators (Transmutation, Recombination, Modification, Mercury): a GeoLite model with a shelf-packed box-uv
atlas, a face painter with materials (alchemy brass, timber, structure bone, liquid chrome ...), engraved glyphs / planks / roof tiles / windows / ripples
that also write the emissive glow map, and keyframe helpers. Written so every texture can be redone by running the generator again.

    from transmutation_geo_kit import Sheet, Anim, wave, keys, S, MATS, skin_texture, ROOT
    sh = Sheet("transmutation", "helm", 64, 64, seed=3)
    sh.bone("root", pivot=(0, 24, 0))
    sh.cube("root", (-4.4, 24.2, -4.4), (8.8, 3, 8.8), mat="alch_brass", deco=("seam", "glyph"))
    sh.save(Anim())

File frame (tools/geo_builder.py): pixels, y up, feet at the origin, front on -z, +x = the model's left. A humanoid armour piece is authored in the
static pose of the player (head y 24..32, shoulders y 24, arms x -8..-4 and 4..8, legs x -4..4); the aura painter moves it with the limb it is
anchored on. In this renderer a positive x rotation swings a hanging limb FORWARD, a positive z rotation swings it toward +x.
Run python as `python3 -B`. Everything is written under src/main/resources/assets/nusmp/ (geo_builder.ROOT).
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim, ROOT  # noqa: E402,F401

SHADE = {"top": 1.14, "north": 1.0, "west": 0.88, "east": 0.93, "south": 0.8, "bottom": 0.55}


# ------------------------------------------------------------------------------------------------ colour helpers
def ramp(pal, t):
    p = np.array(pal, float)
    t = np.clip(t, 0, 1) * (len(p) - 1)
    i = np.minimum(np.asarray(t).astype(int), len(p) - 2)
    f = (t - i)[..., None]
    return p[i] * (1 - f) + p[i + 1] * f


def vnoise(h, w, rng, cell):
    """Smooth value noise 0..1 of h x w texels with features of about 'cell' texels."""
    gh, gw = int(math.ceil(h / cell)) + 2, int(math.ceil(w / cell)) + 2
    g = rng.random((gh, gw)).astype(np.float32)
    im = Image.fromarray(g, mode="F").resize((max(w, int(gw * cell)), max(h, int(gh * cell))), Image.BICUBIC)
    return np.clip(np.asarray(im)[:h, :w], 0, 1)


def face_rects(u, v, w, h, d):
    """The six faces of a box-uv region (vanilla layout): name -> (x, y, width, height) in texels."""
    return {"west": (u, v + d, d, h), "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h), "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d)}


# ------------------------------------------------------------------------------------------------ materials
# pal: dark -> light ramp; grain: noise strength; polish: the lit blob of a cast surface; glow: the colour that glyphs / windows / cores emit;
# deco: decorations drawn on every face unless the cube says otherwise.
MATS = {
    # Transmutation: an alchemist's brass and dark iron, engraved circles glowing teal-green
    "alch_brass": dict(pal=[(0x2a, 0x1c, 0x0a), (0x5a, 0x3e, 0x14), (0x94, 0x6c, 0x24), (0xc9, 0x9a, 0x3c), (0xee, 0xca, 0x6c), (0xff, 0xf0, 0xb0)], grain=0.34, polish=0.7, glow=(0x7a, 0xf0, 0xd8), deco=("seam",)),
    "alch_iron": dict(pal=[(0x0c, 0x10, 0x14), (0x20, 0x28, 0x30), (0x3a, 0x46, 0x52), (0x5c, 0x6c, 0x7a), (0x86, 0x98, 0xa6), (0xb8, 0xc8, 0xd2)], grain=0.38, polish=0.5, glow=(0x7a, 0xf0, 0xd8), deco=("seam",)),
    "alch_glass": dict(pal=[(0x10, 0x4a, 0x44), (0x20, 0x84, 0x74), (0x4c, 0xc8, 0xaa), (0x92, 0xf0, 0xd8), (0xd0, 0xff, 0xf0)], grain=0.2, polish=0.9, glow=(0x7a, 0xf0, 0xd8), deco=("core",)),
    "alch_gold": dict(pal=[(0x5a, 0x3a, 0x08), (0xa0, 0x70, 0x14), (0xe0, 0xb0, 0x30), (0xff, 0xdc, 0x70), (0xff, 0xf4, 0xb8)], grain=0.2, polish=1.0, glow=(0xff, 0xdc, 0x70), deco=()),
    # Recombination: the Black Bulls' magic house: dark oak, iron brackets, red-brown roof tiles, warm windows
    "oak": dict(pal=[(0x1c, 0x12, 0x0a), (0x3a, 0x26, 0x14), (0x5c, 0x3e, 0x20), (0x82, 0x5a, 0x30), (0xa8, 0x7a, 0x44), (0xc8, 0x9c, 0x62)], grain=0.3, polish=0.15, glow=(0xff, 0xa0, 0x40), deco=("plank",)),
    "oak_dark": dict(pal=[(0x0e, 0x08, 0x05), (0x20, 0x14, 0x0a), (0x36, 0x24, 0x14), (0x50, 0x36, 0x1e), (0x6c, 0x4a, 0x2a)], grain=0.3, polish=0.1, glow=(0xff, 0xa0, 0x40), deco=("plank",)),
    "bull_iron": dict(pal=[(0x08, 0x08, 0x0a), (0x1a, 0x1a, 0x20), (0x30, 0x30, 0x3a), (0x4c, 0x4c, 0x58), (0x70, 0x70, 0x80), (0xa0, 0xa0, 0xb0)], grain=0.38, polish=0.45, glow=(0xff, 0xa0, 0x40), deco=("seam", "rivet")),
    "tile": dict(pal=[(0x24, 0x0e, 0x0a), (0x4a, 0x1c, 0x14), (0x78, 0x2e, 0x20), (0xa0, 0x44, 0x2c), (0xc4, 0x62, 0x3c)], grain=0.3, polish=0.2, glow=(0xff, 0xa0, 0x40), deco=("tile",)),
    "stone": dict(pal=[(0x14, 0x14, 0x16), (0x2c, 0x2c, 0x30), (0x48, 0x48, 0x4e), (0x68, 0x68, 0x70), (0x8c, 0x8c, 0x94), (0xb0, 0xb0, 0xb8)], grain=0.5, polish=0.1, glow=(0xff, 0xa0, 0x40), deco=()),
    "lamp": dict(pal=[(0x80, 0x30, 0x08), (0xd0, 0x70, 0x18), (0xff, 0xa8, 0x38), (0xff, 0xd8, 0x80), (0xff, 0xf4, 0xc8)], grain=0.15, polish=0.9, glow=(0xff, 0xa0, 0x40), deco=("core",)),
    # Modification: bone-white structure, dark steel, orange rune bands
    "bone": dict(pal=[(0x30, 0x28, 0x24), (0x6a, 0x5c, 0x50), (0xa4, 0x94, 0x80), (0xd2, 0xc4, 0xac), (0xee, 0xe2, 0xc8), (0xff, 0xf6, 0xe0)], grain=0.3, polish=0.5, glow=(0xff, 0x9b, 0x58), deco=("seam",)),
    "frame": dict(pal=[(0x0a, 0x08, 0x0a), (0x1c, 0x18, 0x1e), (0x34, 0x2c, 0x34), (0x52, 0x46, 0x50), (0x78, 0x68, 0x74), (0xa8, 0x96, 0xa2)], grain=0.36, polish=0.55, glow=(0xff, 0x9b, 0x58), deco=("seam", "bands")),
    "flesh_edit": dict(pal=[(0x1c, 0x16, 0x1c), (0x44, 0x38, 0x42), (0x76, 0x66, 0x70), (0xa8, 0x98, 0xa0), (0xd0, 0xc4, 0xc8)], grain=0.35, polish=0.3, glow=(0xff, 0x9b, 0x58), deco=("veins",)),
    "rune_orange": dict(pal=[(0x7a, 0x2a, 0x08), (0xd0, 0x62, 0x18), (0xff, 0x9b, 0x58), (0xff, 0xcc, 0x90), (0xff, 0xf0, 0xd8)], grain=0.15, polish=0.9, glow=(0xff, 0x9b, 0x58), deco=("core",)),
    # Mercury: liquid silver, chrome with bluish reflections
    "chrome": dict(pal=[(0x20, 0x26, 0x34), (0x4a, 0x58, 0x70), (0x86, 0x98, 0xb0), (0xc4, 0xd2, 0xe2), (0xee, 0xf4, 0xfa), (0xff, 0xff, 0xff)], grain=0.14, polish=1.0, glow=(0xc8, 0xe8, 0xff), deco=("ripple",)),
    "chrome_dark": dict(pal=[(0x0c, 0x10, 0x18), (0x24, 0x2e, 0x40), (0x48, 0x5c, 0x78), (0x7a, 0x92, 0xb0), (0xb0, 0xc8, 0xde), (0xe0, 0xee, 0xfa)], grain=0.16, polish=0.8, glow=(0xc8, 0xe8, 0xff), deco=("ripple",)),
    "quick_core": dict(pal=[(0x50, 0x70, 0x90), (0x90, 0xc0, 0xe0), (0xd0, 0xf0, 0xff), (0xff, 0xff, 0xff)], grain=0.08, polish=1.0, glow=(0xc8, 0xe8, 0xff), deco=("core",)),
}


# ------------------------------------------------------------------------------------------------ decoration drawing
def _glyph_mask(W, H, rng, density=0.16):
    """Engraved rune strokes: short axis-aligned polylines on the texel grid (not touching the border, so seams stay clean)."""
    m = np.zeros((H, W), bool)
    if W < 4 or H < 4:
        return m
    strokes = max(1, int(W * H * density / 4))
    for _ in range(strokes):
        x, y = int(rng.integers(1, W - 1)), int(rng.integers(1, H - 1))
        d = int(rng.integers(0, 2))
        for _ in range(int(rng.integers(2, 4))):
            ln = int(rng.integers(2, 5))
            for _k in range(ln):
                if 1 <= x < W - 1 and 1 <= y < H - 1:
                    m[y, x] = True
                if d == 0:
                    x += 1
                else:
                    y += 1
            d = 1 - d
            if rng.random() < 0.3:
                d = int(rng.integers(0, 2))
    return m


def circle_mask(W, H, rings=(0.9, 0.62), spokes=6, ticks=24):
    """A transmutation circle on a W x H face: concentric rings, a spoked star inside, ticks between the two outer rings."""
    yy, xx = np.mgrid[0:H, 0:W]
    cx, cy = (W - 1) / 2, (H - 1) / 2
    R = min(W, H) / 2 - 0.5
    dx, dy = (xx - cx) / max(R, 1), (yy - cy) / max(R, 1)
    r = np.sqrt(dx * dx + dy * dy)
    ang = np.arctan2(dy, dx)
    m = np.zeros((H, W), bool)
    th = 0.55 / max(R, 1)
    for rr in rings:
        m |= np.abs(r - rr) < th
    if ticks and len(rings) >= 2 and R >= 6:
        a = (ang + math.pi) / (2 * math.pi) * ticks
        m |= (r > rings[1] + th) & (r < rings[0] - th) & (np.abs(a - np.round(a)) < 0.14)
    if spokes and R >= 5:
        inner = rings[-1] - th * 2
        a = (ang + math.pi) / (2 * math.pi) * spokes * 2
        for k in range(2):                         # two triangles (a hexagram when spokes = 3 each)
            tri = (ang + math.pi + k * math.pi / spokes) / (2 * math.pi) * spokes
            edge = np.abs(np.cos(math.pi * tri) * r - inner * 0.5) < th * 1.1
            m |= edge & (r < inner)
    m |= r < 0.1
    return m


def _decorate(face, W, H, rng, rgb, glow, mat, deco, opt):
    yy, xx = np.mgrid[0:H, 0:W]
    gl = np.array(mat["glow"], float)
    if "seam" in deco and W >= 5 and H >= 5:                       # panel seams: a dark line a third / two thirds along the longer side
        dark = rgb * 0.55
        if W >= H:
            for k in (W // 3, 2 * W // 3):
                rgb[:, k] = dark[:, k]
        else:
            for k in (H // 3, 2 * H // 3):
                rgb[k, :] = dark[k, :]
    if "rivet" in deco and W >= 5 and H >= 5:
        for (x, y) in [(1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2)]:
            rgb[y, x] = rgb[y, x] * 0.4 + 255 * 0.6
    if "plank" in deco and face not in ("top", "bottom") and H >= 4:
        step = 3 if H < 14 else 4
        for k in range(step, H, step):                            # the gap between two boards, and a lit edge under it
            rgb[k - 1, :] = rgb[k - 1, :] * 0.5
            if k < H:
                rgb[k, :] = np.minimum(255, rgb[k, :] * 1.12)
        g = vnoise(H, W, rng, 6)
        rgb = rgb * (0.9 + 0.2 * g[..., None])                     # long grain
        if W >= 6 and H >= 6 and rng.random() < 0.7:               # two nail heads
            for (x, y) in [(1, 1), (W - 2, 1)]:
                rgb[y, x] = np.array((0x30, 0x2c, 0x2c))
    if "tile" in deco and face not in ("top", "bottom") and H >= 4:
        for row in range(0, H, 3):
            rgb[row, :] = rgb[row, :] * 0.5
            off = 2 if (row // 3) % 2 else 0
            for x in range(off, W, 4):
                rgb[row:row + 3, x] = rgb[row:row + 3, x] * 0.7
    if "bands" in deco and H >= 4:                                  # an orange rune band round the piece
        yb = H // 2
        glowm = np.zeros((H, W), bool)
        glowm[yb, :] = True
        rgb[glowm] = gl * 0.9
        glow[glowm] = gl
    if "veins" in deco:
        v = vnoise(H, W, rng, 3)
        line = (np.abs(v - 0.52) < 0.02)
        rgb[line] = gl * 0.8
        glow[line] = gl
    if "ripple" in deco:
        r = np.sin((yy * 0.9 + vnoise(H, W, rng, 5) * 6) * 1.4 + (0 if face in ("north", "south") else 1.7))
        band = (r > 0.82)
        rgb[band] = np.minimum(255, rgb[band] * 1.18 + 14)
        rgb[(r < -0.88)] *= 0.82
    if "glyph" in opt.get("show", ()) or "glyph" in deco:
        m = _glyph_mask(W, H, rng, opt.get("glyph_density", 0.16))
        rgb[m] = gl * 0.85
        glow[m] = gl
    cf = opt.get("circle_face", "north")
    if "circle" in deco and (face == cf or cf == "both" or (cf == "sides" and face in ("north", "south"))):
        m = circle_mask(W, H, opt.get("rings", (0.9, 0.62)), opt.get("spokes", 6), opt.get("ticks", 24))
        rgb[m] = gl * 0.9
        glow[m] = gl
        if opt.get("hollow"):                                      # only the engraved rings are there: a dilated mask is the alpha of the face
            d = m.copy()
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                d |= np.roll(np.roll(m, dy, 0), dx, 1)
            opt["_alpha"] = d
    if "window" in deco and face in opt.get("window_faces", ("north",)) and W >= 5 and H >= 5:
        x0, y0, x1, y1 = 1, 1, W - 2, H - 2
        rgb[y0:y1 + 1, x0:x1 + 1] = gl * 0.95
        glow[y0:y1 + 1, x0:x1 + 1] = gl * 0.8
        mx, my = (x0 + x1) // 2, (y0 + y1) // 2                  # the muntins
        rgb[y0:y1 + 1, mx] = (0x28, 0x1a, 0x10)
        rgb[my, x0:x1 + 1] = (0x28, 0x1a, 0x10)
        glow[y0:y1 + 1, mx] = 0
        glow[my, x0:x1 + 1] = 0
    if "core" in deco:
        r = np.sqrt(((xx - (W - 1) / 2) / max(1.0, W / 2)) ** 2 + ((yy - (H - 1) / 2) / max(1.0, H / 2)) ** 2)
        c = np.clip(1.4 - r, 0, 1)
        glow[...] = np.maximum(glow, gl[None, None, :] * c[..., None])
    if "edge_glow" in deco:
        ed = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy))
        m = ed == 0
        rgb[m] = gl * 0.85
        glow[m] = gl * 0.9
    return rgb, glow


def paint_face(face, W, H, rng, matname, opt):
    mat = MATS[matname]
    yy, xx = np.mgrid[0:H, 0:W]
    fx, fy = (xx + 0.5) / W, (yy + 0.5) / H
    shade = SHADE[face] * opt.get("lum", 1.0)
    n = vnoise(H, W, rng, 4) * 0.5 + vnoise(H, W, rng, 2) * 0.3 + rng.random((H, W)) * 0.2
    t = 0.40 * shade + (n - 0.5) * opt.get("grain", mat["grain"])
    if face not in ("top", "bottom"):
        t += 0.17 * (0.5 - fy)
    t += np.where(yy == 0, 0.20, 0.0) + np.where(xx == 0, 0.09, 0.0) - np.where(yy == H - 1, 0.22, 0.0) - np.where(xx == W - 1, 0.12, 0.0)
    pol = opt.get("polish", mat["polish"])
    t += pol * 0.36 * np.exp(-(((fx - 0.30) / 0.30) ** 2 + ((fy - 0.26) / 0.32) ** 2))
    rgb = ramp(mat["pal"], t + opt.get("tone", 0.0))
    glow = np.zeros((H, W, 3))
    deco = opt.get("deco", mat["deco"])
    rgb, glow = _decorate(face, W, H, rng, rgb, glow, mat, deco, opt)
    return rgb, glow


# ------------------------------------------------------------------------------------------------ a model with its packed atlas
class Sheet:
    def __init__(self, key, name, tw, th, seed=1):
        self.m = Model(key, name, tw, th)
        self.tw, self.th, self.seed = tw, th, seed
        self.items = []

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def cube(self, bone, o, s, mat="alch_brass", rot=None, pv=None, infl=0.0, **opt):
        c = self.m.cube(bone, origin=o, size=s, uv=(0, 0), inflate=infl, rotation=rot, pivot=pv)
        self.items.append((c, tuple(s), mat, opt, len(self.items)))
        return c

    def pair(self, right, left, specs):
        """specs are dicts of cube arguments for the right side (-x); the left side gets the mirrored cube (rot y, z negated)."""
        for sp in specs:
            sp = dict(sp)
            o, s = sp.pop("o"), sp.pop("s")
            rot, pv = sp.pop("rot", None), sp.pop("pv", None)
            self.cube(right, o, s, rot=rot, pv=pv, **sp)
            self.cube(left, (-(o[0] + s[0]), o[1], o[2]), s, rot=None if rot is None else (rot[0], -rot[1], -rot[2]),
                      pv=None if pv is None else (-pv[0], pv[1], pv[2]), **sp)

    def pack(self):
        rects = []
        for c, s, mat, opt, i in self.items:
            w, h, d = [int(math.ceil(x)) for x in s]
            rects.append((2 * (w + d), d + h, i))
        order = sorted(rects, key=lambda r: (-r[1], -r[0]))
        x = y = rowh = 0
        place = {}
        for (rw, rh, i) in order:
            if x + rw > self.tw:
                x, y, rowh = 0, y + rowh + 1, 0
            if y + rh > self.th:
                raise SystemExit("sheet %s is too small (%d x %d): needs more than %d rows" % (self.m.id, self.tw, self.th, y + rh))
            place[i] = (x, y)
            x += rw + 1
            rowh = max(rowh, rh)
        return place, y + rowh

    def paint(self):
        place, _ = self.pack()
        img = np.zeros((self.th, self.tw, 4))
        glow = np.zeros((self.th, self.tw, 4))
        for c, s, mat, opt, i in self.items:
            u, v = place[i]
            c["uv"] = [u, v]
            w, h, d = [float(math.ceil(x)) for x in s]
            rng = np.random.default_rng(self.seed * 1009 + i * 7919)
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                x0, y0 = int(fx), int(fy)
                W, H = max(1, int(fw)), max(1, int(fh))
                fopt = dict(opt)
                rgb, g = paint_face(face, W, H, rng, mat, fopt)
                img[y0:y0 + H, x0:x0 + W, :3] = np.clip(rgb, 0, 255)
                al = fopt.get("_alpha")
                if al is None and fopt.get("hollow") and face in ("north", "south", "top", "bottom", "west", "east"):
                    al = np.zeros((H, W), bool) if (W > 2 and H > 2) else None
                img[y0:y0 + H, x0:x0 + W, 3] = 255 if al is None else np.where(al, 255, 0)
                glow[y0:y0 + H, x0:x0 + W, :3] = np.clip(g, 0, 255)
                glow[y0:y0 + H, x0:x0 + W, 3] = 255
        return Image.fromarray(img.astype(np.uint8), "RGBA"), Image.fromarray(glow.astype(np.uint8), "RGBA")

    def save(self, anim=None):
        im, gl = self.paint()
        self.m.save(anim)
        self.m.save_texture(im, gl)
        print("wrote", self.m.id, "cubes", self.m.cube_count(), "tex", self.tw, "x", self.th, "clips", sorted(anim.clips) if anim and anim.clips else "-")


# ------------------------------------------------------------------------------------------------ animation helpers
def wave(a, clip, bone, length, steps=12, x=None, y=None, z=None, kind="rot"):
    """Keyframes every length/steps seconds; x, y, z are functions of the phase p in [0, 1) (None = 0). The last key equals the first."""
    if clip not in a.clips:
        a.clip(clip, length=length, loop=True)
    frames = {}
    for k in range(steps + 1):
        p = (k % steps) / steps
        frames[round(length * k / steps, 4)] = tuple(float(f(p)) if f else (1.0 if kind == "scale" else 0.0) for f in (x, y, z))
    getattr(a, kind)(clip, bone, frames)


def S(amp, phase=0.0, cycles=1, off=0.0):
    return lambda p: off + amp * math.sin(2 * math.pi * (p * cycles + phase))


def keys(a, clip, bone, kind, table, loop=True):
    """table: {time: (x, y, z)}."""
    if clip not in a.clips:
        a.clip(clip, length=max(table), loop=loop)
    getattr(a, kind)(clip, bone, {t: tuple(v) for t, v in table.items()})


# ------------------------------------------------------------------------------------------------ 64 x 64 player skin layout textures (aura shells)
PARTS = {
    "head": ((0, 0, 8, 8, 8), (32, 0, 8, 8, 8)),
    "body": ((16, 16, 8, 12, 4), (16, 32, 8, 12, 4)),
    "rarm": ((40, 16, 4, 12, 4), (40, 32, 4, 12, 4)),
    "larm": ((32, 48, 4, 12, 4), (48, 48, 4, 12, 4)),
    "rleg": ((0, 16, 4, 12, 4), (0, 32, 4, 12, 4)),
    "lleg": ((16, 48, 4, 12, 4), (0, 48, 4, 12, 4)),
}


def skin_texture(path, painter, seed=1):
    """Writes a 64x64 texture laid out like a player skin. painter(part, layer, face, W, H, rng) -> (rgb HxWx3, alpha HxW, glow HxWx3 or None).
    'path' is relative to assets/nusmp/textures/ without extension; <path>_glow.png is written when any painter returns a glow."""
    rng = np.random.default_rng(seed)
    img = np.zeros((64, 64, 4))
    glow = np.zeros((64, 64, 4))
    any_glow = False
    for part, (base, over) in PARTS.items():
        for layer, (u, v, w, h, d) in (("base", base), ("over", over)):
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                W, H = int(fw), int(fh)
                rgb, alpha, g = painter(part, layer, face, W, H, rng)
                x0, y0 = int(fx), int(fy)
                img[y0:y0 + H, x0:x0 + W, :3] = np.clip(rgb, 0, 255)
                img[y0:y0 + H, x0:x0 + W, 3] = alpha
                if g is not None:
                    any_glow = True
                    glow[y0:y0 + H, x0:x0 + W, :3] = np.clip(g, 0, 255)
                    glow[y0:y0 + H, x0:x0 + W, 3] = 255
    out = os.path.join(ROOT, "textures", path + ".png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    Image.fromarray(img.astype(np.uint8), "RGBA").save(out)
    if any_glow:
        Image.fromarray(glow.astype(np.uint8), "RGBA").save(os.path.join(ROOT, "textures", path + "_glow.png"))
    print("wrote textures/%s.png" % path)
