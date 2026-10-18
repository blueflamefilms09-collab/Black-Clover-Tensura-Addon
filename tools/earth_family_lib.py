"""Shared painter and model helpers of the earth-family rebuild (Sandstone, Rock, Red Ochre, Stone, Earth, Mineral magic).

Used by gen_earth_family_armor.py (the temporary armour pieces), gen_earth_family_constructs.py (golems, spires, cages ...) and
gen_earth_family_textures.py (the VFX sprites). Nothing here is random at import time: every cube is painted from a seed derived from the
model seed and the cube's index, so rerunning a generator rewrites byte-identical files.

A model is a Sheet: cubes get their own box-uv region from a shelf packer and are painted face by face by a MATERIAL (a palette ramp plus a few
surface rules). The glow map carries only what should shine: cracks, runes, crystal cores, pigment lines.

File frame of a model (tools/geo_builder.py): pixels, y up, feet at the origin, front on -z, +x is the model's left. Armour pieces are authored
in the humanoid's proportions (head origin y 24..32, body x +-4, arms at x +-4..8, legs x 0..4).
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim, ROOT  # noqa: E402

# ------------------------------------------------------------------------------------------------ palettes (dark -> light)
PAL = {
    "sandstone": [(0x3e, 0x28, 0x12), (0x7a, 0x52, 0x2a), (0xb0, 0x82, 0x4c), (0xd5, 0xa7, 0x6c), (0xe8, 0xc8, 0x90), (0xf7, 0xe6, 0xba)],
    "sand_dark": [(0x2a, 0x1a, 0x0a), (0x58, 0x3a, 0x1c), (0x84, 0x5c, 0x30), (0xa8, 0x7c, 0x44), (0xc8, 0xa0, 0x62)],
    "rock": [(0x10, 0x12, 0x18), (0x26, 0x2a, 0x34), (0x44, 0x4a, 0x58), (0x68, 0x6e, 0x7c), (0x8c, 0x92, 0xa0), (0xb4, 0xba, 0xc6)],
    "ochre": [(0x26, 0x0a, 0x06), (0x62, 0x1a, 0x10), (0xa2, 0x30, 0x1e), (0xd6, 0x4c, 0x37), (0xe8, 0x82, 0x4c), (0xf6, 0xc0, 0x84)],
    "ochre_pale": [(0x6a, 0x3a, 0x1c), (0xa8, 0x6a, 0x32), (0xd4, 0x98, 0x4c), (0xee, 0xc4, 0x78), (0xfa, 0xe4, 0xb0)],
    "stone": [(0x1c, 0x1c, 0x20), (0x46, 0x44, 0x44), (0x74, 0x72, 0x6e), (0x9a, 0x98, 0x92), (0xbe, 0xbc, 0xb4), (0xe2, 0xe0, 0xd8)],
    "earth": [(0x1a, 0x0e, 0x06), (0x3a, 0x24, 0x12), (0x66, 0x44, 0x22), (0x8a, 0x60, 0x32), (0xae, 0x86, 0x4e), (0xd0, 0xac, 0x70)],
    "grass": [(0x10, 0x2a, 0x0c), (0x24, 0x52, 0x18), (0x44, 0x80, 0x28), (0x74, 0xb0, 0x40), (0xa4, 0xd8, 0x64)],
    "mineral": [(0x14, 0x1c, 0x28), (0x30, 0x40, 0x56), (0x4c, 0x66, 0x80), (0x78, 0xa8, 0xc8), (0xa8, 0xd4, 0xee), (0xe0, 0xf4, 0xff)],
    "amethyst": [(0x20, 0x0a, 0x3a), (0x56, 0x22, 0x8c), (0x86, 0x4a, 0xc4), (0xb4, 0x82, 0xe6), (0xe2, 0xc6, 0xff)],
    "malachite": [(0x06, 0x2c, 0x1c), (0x12, 0x62, 0x3e), (0x2a, 0x9c, 0x64), (0x7a, 0xde, 0xa6)],
    "pyrite": [(0x4a, 0x34, 0x06), (0x94, 0x6c, 0x16), (0xd6, 0xae, 0x2e), (0xff, 0xe8, 0x76)],
    "ember": [(0x30, 0x10, 0x04), (0x80, 0x30, 0x0c), (0xe0, 0x68, 0x18), (0xff, 0xb0, 0x50)],
    "iron_oxide": [(0x1c, 0x08, 0x06), (0x4a, 0x14, 0x0c), (0x7e, 0x24, 0x16)],
}

SHADE = {"top": 1.16, "north": 1.0, "west": 0.88, "east": 0.93, "south": 0.8, "bottom": 0.55}


def ramp(pal, t):
    p = np.array(pal, float)
    t = np.clip(t, 0, 1) * (len(p) - 1)
    i = np.minimum(t.astype(int), len(p) - 2)
    f = (t - i)[..., None]
    return p[i] * (1 - f) + p[i + 1] * f


def vnoise(h, w, rng, cell):
    gh, gw = int(math.ceil(h / cell)) + 2, int(math.ceil(w / cell)) + 2
    g = rng.random((gh, gw)).astype(np.float32)
    im = Image.fromarray(g, mode="F").resize((max(w, int(gw * cell)), max(h, int(gh * cell))), Image.BICUBIC)
    return np.clip(np.asarray(im)[:h, :w], 0, 1)


def face_rects(u, v, w, h, d):
    return {"west": (u, v + d, d, h), "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h), "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d)}


# ------------------------------------------------------------------------------------------------ the face painters
def _common(face, W, H, rng, o):
    yy, xx = np.mgrid[0:H, 0:W]
    fx, fy = (xx + 0.5) / W, (yy + 0.5) / H
    ed = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy)).astype(float)
    shade = SHADE[face] * o.get("lum", 1.0)
    n = vnoise(H, W, rng, 4) * 0.5 + vnoise(H, W, rng, 2) * 0.3 + rng.random((H, W)) * 0.2
    t = 0.42 * shade + (n - 0.5) * o.get("grain", 0.4)
    if face not in ("top", "bottom"):
        t += 0.14 * (0.5 - fy)
    t += np.where(yy == 0, 0.16, 0.0) + np.where(xx == 0, 0.07, 0.0) - np.where(yy == H - 1, 0.18, 0.0) - np.where(xx == W - 1, 0.1, 0.0)
    return yy, xx, fx, fy, ed, n, t


def _cracks(t, glow, W, H, rng, count, glow_rgb, depth=0.30):
    """Dark crack lines (random walks); a lit core in the glow map when glow_rgb is given."""
    if W < 4 or H < 4:
        return
    for _ in range(count):
        x, y = float(rng.integers(0, W)), float(rng.integers(0, H))
        ang = rng.random() * math.tau
        for _k in range(int(rng.integers(max(3, min(W, H) // 2), max(5, (W + H) // 2 + 1)))):
            xi, yi = int(x), int(y)
            if 0 <= xi < W and 0 <= yi < H:
                t[yi, xi] -= depth
                if glow_rgb is not None:
                    glow[yi, xi] = np.maximum(glow[yi, xi], np.array(glow_rgb, float) * 0.85)
            ang += (rng.random() - 0.5) * 0.9
            x += math.cos(ang)
            y += math.sin(ang)


def paint_face(face, W, H, rng, mat, o):
    """One face of one cube: (rgb h x w x 3 floats 0..255, glow h x w x 3 floats)."""
    yy, xx, fx, fy, ed, n, t = _common(face, W, H, rng, o)
    glow = np.zeros((H, W, 3))
    pal = o.get("pal") or PAL.get(mat, PAL["stone"])
    gc = o.get("glow")                                 # glow rgb for the material's own shining parts

    if mat in ("sandstone", "sand_dark"):
        # strata: horizontal bands of alternating tone, wavy; wind-polished edges
        bands = o.get("bands", 3 + H // 5)
        wob = vnoise(H, W, rng, 5) * 0.8
        phase = (fy * bands + wob) * math.tau if face not in ("top", "bottom") else (fx * bands + fy * 2.3 + wob) * math.tau
        t += 0.10 * np.sin(phase) + 0.05 * np.sin(phase * 2.7 + 1.0)
        t += (rng.random((H, W)) < 0.08) * 0.14 - (rng.random((H, W)) < 0.05) * 0.12          # grains
        rgb = ramp(pal, t + o.get("tone", 0.0))
        if gc is not None and o.get("shine"):                                                 # the hourglass light in the seams
            seam = (np.sin(phase) > 0.93).astype(float)
            glow = np.array(gc, float)[None, None, :] * seam[..., None] * 0.7
        return rgb, glow

    if mat == "rock":
        big = vnoise(H, W, rng, max(3, min(W, H) // 2))
        t += (big - 0.5) * 0.45
        t += (rng.random((H, W)) < 0.04) * 0.2                                                # mica flecks
        _cracks(t, glow, W, H, rng, min(4, o.get("cracks", 1 + (W * H) // 60)), None, 0.30)           # dark fissures
        if gc is not None and W * H >= 30:
            _cracks(t, glow, W, H, rng, 1, gc, 0.34)                                                  # one lit fissure per face: the amber seam
            glow *= 0.8
        if o.get("moss"):
            m = (vnoise(H, W, rng, 3) + ed * 0.0 > 0.62) & (fy > 0.5 if face != "top" else True)
            rgb = ramp(pal, t)
            rgb = np.where(m[..., None], ramp(PAL["grass"], 0.15 + 0.4 * n)[..., :], rgb)
            return rgb, glow
        return ramp(pal, t + o.get("tone", 0.0)), glow

    if mat in ("ochre", "ochre_pale"):
        t = np.clip(t * 0.9 + 0.1, 0, 1)
        # smooth fired clay: soft noise only; painted bands and dots in the pale pigment
        rgb = ramp(pal, t + o.get("tone", 0.0))
        if o.get("paint", True) and W >= 4 and H >= 4:
            if face not in ("top", "bottom"):
                for yb in (H // 5, (H * 4) // 5):
                    rgb[max(0, yb - 1):yb + 1, :] = ramp(PAL["ochre_pale"], 0.55 + 0.3 * n[max(0, yb - 1):yb + 1, :])
                if W >= 7:
                    for k in range(int(rng.integers(1, 4))):
                        cx, cy = int(rng.integers(1, W - 1)), int(rng.integers(1, H - 1))
                        rgb[cy, cx] = ramp(PAL["ochre_pale"], 0.8)
                        if o.get("glyph"):
                            glow[cy, cx] = np.array(gc or (255, 120, 60), float)
        if o.get("glyph") and W >= 6 and H >= 6 and face in ("north", "south"):               # an ochre glyph: a handprint-ish cross of dots
            cx, cy = W // 2, H // 2
            for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1), (0, 2), (-2, -1), (2, -1)):
                x, y = cx + dx, cy + dy
                if 0 <= x < W and 0 <= y < H:
                    rgb[y, x] = ramp(PAL["ochre_pale"], 0.9)
                    glow[y, x] = np.array(gc or (255, 130, 70), float)
        return rgb, glow

    if mat == "stone":
        # dressed masonry: mortar courses, chisel marks, clean bevel
        rgb = ramp(pal, t * 0.95 + 0.05 + o.get("tone", 0.0))
        course = o.get("course", 5)
        if W >= 6 and H >= course + 1 and not o.get("smooth"):
            for yb in range(course, H, course):
                rgb[yb, :] = ramp(pal, 0.14 + 0.1 * n[yb, :])
                off = (yb // course) % 2 * (course)
                for xb in range(off + course, W, course * 2):
                    rgb[yb - course + 1:yb, xb] = ramp(pal, 0.14 + 0.1 * n[yb - course + 1:yb, xb])
        if W > 3 and H > 3:
            for _ in range(int(rng.integers(0, 3))):                                         # chisel scratches
                x0, y0 = int(rng.integers(0, W)), int(rng.integers(0, H))
                for k in range(int(rng.integers(2, 5))):
                    x, y = x0 + k, y0 + k // 2
                    if 0 <= x < W and 0 <= y < H:
                        rgb[y, x] = rgb[y, x] * 1.12
        if o.get("rune") and W >= 6 and H >= 6 and face in ("north", "south", "east", "west"):
            gcol = np.array(gc or (150, 230, 255), float)
            cx, cy = W // 2, H // 2
            r = max(2, min(W, H) // 2 - 1)
            for k in range(-r, r + 1):                                                        # a rune: a vertical stem and two branches
                for (x, y) in ((cx, cy + k), (cx + abs(k) // 2 * (1 if k > 0 else -1), cy + k) if abs(k) > 1 else (cx, cy + k)):
                    if 0 <= x < W and 0 <= y < H:
                        rgb[y, x] = gcol * 0.85
                        glow[y, x] = gcol
            for dx in (-2, -1, 1, 2):
                x, y = cx + dx, cy - r + abs(dx)
                if 0 <= x < W and 0 <= y < H:
                    rgb[y, x] = gcol * 0.85
                    glow[y, x] = gcol
        return rgb, glow

    if mat == "earth":
        t += (vnoise(H, W, rng, 3) - 0.5) * 0.3
        rgb = ramp(pal, t + o.get("tone", 0.0))
        pebble = rng.random((H, W)) < 0.07
        rgb = np.where(pebble[..., None], ramp(PAL["stone"], 0.45 + 0.3 * n)[..., :], rgb)
        if W >= 5 and H >= 5:                                                               # roots: dark twisting lines
            for _ in range(int(rng.integers(1, 3))):
                x, y = float(rng.integers(0, W)), 0.0
                for _k in range(H):
                    xi, yi = int(x), int(y)
                    if 0 <= xi < W and 0 <= yi < H:
                        rgb[yi, xi] = ramp(PAL["earth"], 0.1 + 0.1 * n[yi, xi])
                    x += (rng.random() - 0.5) * 1.6
                    y += 1
        g = o.get("grass", 0.0) * (1.0 if face == "top" else 0.0)
        gh = o.get("grass_side", 0.0)
        if g > 0 or gh > 0:
            edge = (fy < 0.34 + 0.2 * n) if face not in ("top", "bottom") else np.ones((H, W), bool)
            mask = (edge & (rng.random((H, W)) < (0.95 if face == "top" else gh))) if (face == "top" and g > 0) or (face not in ("top", "bottom") and gh > 0) else np.zeros((H, W), bool)
            rgb = np.where(mask[..., None], ramp(PAL["grass"], 0.35 + 0.55 * n + (fy < 0.15) * 0.1)[..., :], rgb)
        if gc is not None and o.get("shine"):
            seed = (rng.random((H, W)) < 0.05).astype(float)
            glow = np.array(gc, float)[None, None, :] * seed[..., None]
        return rgb, glow

    if mat in ("mineral", "amethyst", "malachite", "pyrite", "crystal"):
        key = "mineral" if mat == "crystal" else mat
        pal = o.get("pal") or PAL[key]
        # facets: diagonal bands of different brightness, a sharp bright edge, a luminous core
        diag = (fx * 1.0 + fy * 0.7) * (2 + (W + H) // 8) + vnoise(H, W, rng, 4) * 0.8
        facet = np.floor(diag) % 3
        tt = 0.38 + 0.16 * facet + (n - 0.5) * 0.12 + 0.12 * (SHADE[face] - 0.8)
        tt += np.where(yy == 0, 0.18, 0.0) + np.where(xx == 0, 0.1, 0.0) - np.where(yy == H - 1, 0.1, 0.0)
        core = np.exp(-(((fx - 0.5) / 0.38) ** 2 + ((fy - 0.5) / 0.46) ** 2))
        tt += 0.22 * core * o.get("core", 1.0)
        rgb = ramp(pal, tt + o.get("tone", 0.0))
        spark = (rng.random((H, W)) < 0.025)
        rgb = np.where(spark[..., None], np.array(pal[-1], float), rgb)
        g = core * (0.55 + 0.35 * (facet == 2)) * o.get("shine", 1.0)
        glow = ramp(pal, 0.55 + 0.45 * core)[..., :] * g[..., None]
        if gc is not None:
            glow = np.array(gc, float)[None, None, :] * g[..., None]
        return rgb, glow

    if mat == "matrix":                                                                      # mineral host rock with ore flecks
        big = vnoise(H, W, rng, 3)
        t += (big - 0.5) * 0.4
        rgb = ramp(PAL["mineral"], t * 0.7 + 0.04)
        fl = rng.random((H, W)) < 0.06
        pick = rng.random((H, W))
        rgb = np.where((fl & (pick < 0.4))[..., None], ramp(PAL["pyrite"], 0.7), rgb)
        rgb = np.where((fl & (pick >= 0.4) & (pick < 0.7))[..., None], ramp(PAL["amethyst"], 0.7), rgb)
        rgb = np.where((fl & (pick >= 0.7))[..., None], ramp(PAL["malachite"], 0.7), rgb)
        return rgb, glow

    if mat in ("eye", "ember", "core"):                                                      # a glowing lump
        col = np.array(gc or {"eye": (160, 255, 255), "ember": (255, 170, 70), "core": (255, 255, 200)}[mat], float)
        rgb = ramp(pal if o.get("pal") else PAL["rock"], t * 0.6)
        cx, cy = (W - 1) / 2, (H - 1) / 2
        r = np.sqrt(((xx - cx) / max(1.0, W / 2)) ** 2 + ((yy - cy) / max(1.0, H / 2)) ** 2)
        c = np.clip(1.3 - r, 0, 1)
        glow = col[None, None, :] * c[..., None]
        rgb = rgb * (1 - c[..., None]) + col[None, None, :] * c[..., None]
        return rgb, glow

    return ramp(pal, t), glow


# ------------------------------------------------------------------------------------------------ a model with its packed atlas
class Sheet:
    def __init__(self, key, name, tw, th, seed):
        self.m = Model(key, name, tw, th)
        self.tw, self.th, self.seed = tw, th, seed
        self.items = []

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def cube(self, bone, o, s, mat="stone", rot=None, pv=None, infl=0.0, **opt):
        c = self.m.cube(bone, origin=o, size=s, uv=(0, 0), inflate=infl, rotation=rot, pivot=pv)
        self.items.append((c, tuple(s), mat, opt, len(self.items)))
        return c

    def mirror_cube(self, bone, o, s, **kw):
        """The same cube mirrored across x = 0 (origin x -> -(x + sx))."""
        rot, pv = kw.pop("rot", None), kw.pop("pv", None)
        self.cube(bone, (-(o[0] + s[0]), o[1], o[2]), s, rot=None if rot is None else (rot[0], -rot[1], -rot[2]),
                  pv=None if pv is None else (-pv[0], pv[1], pv[2]), **kw)

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
        return place

    def paint(self):
        place = self.pack()
        img = np.zeros((self.th, self.tw, 4))
        glow = np.zeros((self.th, self.tw, 4))
        for c, s, mat, opt, i in self.items:
            u, v = place[i]
            c["uv"] = [u, v]
            w, h, d = [int(math.ceil(x)) for x in s]
            rng = np.random.default_rng(self.seed * 1009 + i * 7919)
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                x0, y0 = int(math.floor(fx)), int(math.floor(fy))
                x1, y1 = int(math.ceil(fx + fw)), int(math.ceil(fy + fh))
                W, H = max(1, x1 - x0), max(1, y1 - y0)
                rgb, g = paint_face(face, W, H, rng, mat, dict(opt))
                img[y0:y0 + H, x0:x0 + W, :3] = np.clip(rgb, 0, 255)
                img[y0:y0 + H, x0:x0 + W, 3] = 255
                glow[y0:y0 + H, x0:x0 + W, :3] = np.clip(g, 0, 255)
                glow[y0:y0 + H, x0:x0 + W, 3] = 255
        return Image.fromarray(img.astype(np.uint8), "RGBA"), Image.fromarray(glow.astype(np.uint8), "RGBA")

    def save(self, anim=None, quiet=False):
        im, gl = self.paint()
        self.m.save(anim)
        self.m.save_texture(im, gl)
        if not quiet:
            print("wrote", self.m.id, "cubes", self.m.cube_count(), "tex", self.tw, "x", self.th, "clips", sorted(anim.clips) if anim else "-")
        return self.m.id


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


def keys(a, clip, bone, kind, table, length=None, loop=False):
    """table: {time: (x, y, z)}."""
    if clip not in a.clips:
        a.clip(clip, length=length or max(table), loop=loop)
    getattr(a, kind)(clip, bone, {t: tuple(v) for t, v in table.items()})


def jag(sh, bone, mat, rng, cx, cy, cz, rx, ry, rz, n, smin=1.6, smax=3.2, **opt):
    """n angular chunks scattered inside the ellipsoid (cx,cy,cz) +- (rx,ry,rz): each a cube with a random tilt; the boulder look."""
    for _ in range(n):
        s = [float(rng.uniform(smin, smax)) for _ in range(3)]
        u = rng.uniform(-1, 1, 3)
        while np.linalg.norm(u) > 1:
            u = rng.uniform(-1, 1, 3)
        px, py, pz = cx + u[0] * rx, cy + u[1] * ry, cz + u[2] * rz
        rot = (float(rng.uniform(-28, 28)), float(rng.uniform(-35, 35)), float(rng.uniform(-28, 28)))
        sh.cube(bone, (px - s[0] / 2, py - s[1] / 2, pz - s[2] / 2), tuple(float(max(1, round(v))) for v in s), mat=mat, rot=rot, pv=(px, py, pz), **opt)


# ------------------------------------------------------------------------------------------------ cube specs (so a right piece can be mirrored into the left one)
def spec(o, s, mat="stone", rot=None, pv=None, infl=0.0, bone="root", **opt):
    return dict(o=tuple(o), s=tuple(s), mat=mat, rot=rot, pv=pv, infl=infl, bone=bone, opt=opt)


def mirrored(specs):
    out = []
    for sp in specs:
        o, s, rot, pv = sp["o"], sp["s"], sp["rot"], sp["pv"]
        out.append(dict(sp, o=(-(o[0] + s[0]), o[1], o[2]), rot=None if rot is None else (rot[0], -rot[1], -rot[2]),
                        pv=None if pv is None else (-pv[0], pv[1], pv[2])))
    return out


def jagspecs(rng, cx, cy, cz, rx, ry, rz, n, smin=1.6, smax=3.2, mat="rock", bone="root", **opt):
    out = []
    for _ in range(n):
        s = [float(max(1, round(rng.uniform(smin, smax)))) for _ in range(3)]
        u = rng.uniform(-1, 1, 3)
        while np.linalg.norm(u) > 1:
            u = rng.uniform(-1, 1, 3)
        px, py, pz = cx + u[0] * rx, cy + u[1] * ry, cz + u[2] * rz
        rot = (float(rng.uniform(-28, 28)), float(rng.uniform(-35, 35)), float(rng.uniform(-28, 28)))
        out.append(spec((px - s[0] / 2, py - s[1] / 2, pz - s[2] / 2), s, mat=mat, rot=rot, pv=(px, py, pz), bone=bone, **opt))
    return out


SIZES = [(64, 32), (64, 64), (128, 64), (128, 128), (128, 256), (256, 256)]


def build(key, name, seed, specs, bones=None, anim=None, quiet=True):
    """Builds, paints and saves one model. bones: list of (name, parent, pivot) beyond 'root' (a bone at the origin). The smallest sheet that fits is used."""
    last = None
    for tw, th in SIZES:
        sh = Sheet(key, name, tw, th, seed)
        sh.bone("root", pivot=(0, 0, 0))
        for b in bones or []:
            sh.bone(b[0], b[1], b[2])
        for sp in specs:
            sh.cube(sp["bone"], sp["o"], sp["s"], sp["mat"], sp["rot"], sp["pv"], sp["infl"], **sp["opt"])
        try:
            sh.pack()
        except SystemExit as e:
            last = e
            continue
        return sh.save(anim, quiet=quiet), sh
    raise last
