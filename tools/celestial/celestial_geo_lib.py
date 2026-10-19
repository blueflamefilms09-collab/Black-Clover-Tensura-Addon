"""A small cube-model builder on top of tools/geo_builder.py for the Celestial Spirit Magic models: every cube names a material, the cubes are packed into the
texture sheet by shelf packing (box UV, the vanilla layout), every face is painted from its material (grain, shading by face, a pattern), a cube may glow (its
faces also go into the emissive map) and a decal can paint details on one face (eyes, a clock face, a keyhole) in both maps.

    b = B("celestial", "leo")
    b.bone("root"); b.bone("body", "root", (0, 12, 0))
    b.cube("body", (-4, 12, -2), (8, 12, 4), "cloth_black")
    b.cube("head", (-4, 24, -4), (8, 8, 8), "skin", decal=("front", eyes))
    b.save(anim)                # writes geo, animation, texture and glow map; the texture sheet size is chosen to fit

Sizes of cubes are whole pixels (the box UV layout), positions may be fractions. Deterministic. Run python as `python3 -B`.
"""
import math
import zlib
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
from geo_builder import Model, Anim  # noqa: E402

# material: (base rgb, grain, pattern); the pattern is a small function name below
MATERIALS = {
    "skin": ((236, 202, 176), 0.04, None), "skin_tan": ((214, 168, 130), 0.05, None), "skin_pale": ((246, 226, 214), 0.03, None), "skin_dark": ((168, 118, 88), 0.05, None),
    "skin_goat": ((222, 214, 200), 0.06, "fur"), "skin_bull": ((150, 98, 66), 0.07, "fur"), "skin_red": ((210, 96, 78), 0.05, "shell"),
    "hair_blue": ((70, 150, 235), 0.06, "strand"), "hair_orange": ((246, 160, 60), 0.06, "strand"), "hair_pink": ((244, 150, 196), 0.06, "strand"),
    "hair_blonde": ((250, 222, 130), 0.06, "strand"), "hair_brown": ((120, 78, 48), 0.06, "strand"), "hair_white": ((240, 240, 246), 0.05, "strand"),
    "hair_black": ((34, 30, 44), 0.05, "strand"), "hair_purple": ((140, 96, 210), 0.06, "strand"), "hair_red": ((210, 70, 60), 0.06, "strand"),
    "cloth_white": ((240, 240, 246), 0.04, "weave"), "cloth_black": ((36, 34, 48), 0.04, "weave"), "cloth_blue": ((60, 130, 220), 0.05, "weave"),
    "cloth_pink": ((246, 170, 206), 0.05, "weave"), "cloth_lilac": ((172, 160, 236), 0.05, "weave"), "cloth_red": ((200, 60, 70), 0.05, "weave"),
    "cloth_tan": ((206, 170, 116), 0.05, "weave"), "cloth_green": ((70, 150, 110), 0.05, "weave"), "cloth_night": ((24, 28, 70), 0.04, "weave"),
    "cloth_gold": ((236, 200, 110), 0.04, "weave"), "cloth_brown": ((110, 76, 52), 0.05, "weave"),
    "gold": ((232, 184, 64), 0.05, "metal"), "silver": ((204, 210, 222), 0.05, "metal"), "iron": ((110, 116, 130), 0.06, "metal"), "bronze": ((176, 120, 60), 0.06, "metal"),
    "marble": ((238, 236, 242), 0.04, "marble"), "marble_dark": ((70, 68, 96), 0.05, "marble"), "wood": ((126, 84, 52), 0.07, "wood"), "wood_dark": ((70, 44, 34), 0.07, "wood"),
    "scale_teal": ((60, 190, 176), 0.06, "scale"), "scale_blue": ((52, 120, 220), 0.06, "scale"), "scale_orange": ((246, 150, 60), 0.06, "scale"),
    "wool": ((248, 246, 250), 0.07, "wool"), "shell": ((220, 80, 60), 0.06, "shell"), "horn": ((236, 226, 200), 0.05, "horn"), "sand": ((226, 190, 120), 0.07, "wool"),
    "glass": ((170, 220, 250), 0.03, None), "star": ((255, 244, 200), 0.02, None), "void": ((20, 14, 44), 0.03, None), "stone": ((120, 118, 140), 0.07, "marble"),
    "ceramic": ((60, 120, 210), 0.04, "marble"), "dog_white": ((250, 250, 252), 0.03, None), "dog_orange": ((246, 150, 70), 0.04, None), "black": ((18, 16, 24), 0.02, None),
}
SHADE = {"top": 1.16, "front": 1.0, "left": 0.9, "right": 0.9, "back": 0.82, "bottom": 0.66}


def _noise(rng, h, w, cell):
    g = rng.rand(max(2, h // cell + 2), max(2, w // cell + 2))
    im = Image.fromarray((g * 255).astype(np.uint8)).resize((max(1, w), max(1, h)), Image.BICUBIC)
    return np.asarray(im, np.float32) / 255.0


def paint_face(mat, w, h, seed, face):
    base, grain, pattern = MATERIALS[mat]
    rng = np.random.RandomState(seed)
    lo = _noise(rng, h, w, 3)
    t = 1.0 + (lo - 0.5) * 0.35 + (rng.rand(h, w) - 0.5) * grain * 2
    yy = np.linspace(1.06, 0.94, h, dtype=np.float32)[:, None]
    t = t * yy
    if pattern == "weave":
        t = t * (1.0 + 0.07 * (((np.arange(w)[None, :] + np.arange(h)[:, None]) % 2) - 0.5))
    elif pattern == "metal":
        t = t * (1.0 + 0.16 * np.sin(np.arange(h)[:, None] * 0.9 + lo * 4)) + 0.10 * (np.arange(w)[None, :] % 5 == 0)
    elif pattern == "marble":
        veins = np.abs(np.sin((np.arange(w)[None, :] * 0.35 + np.arange(h)[:, None] * 0.2 + lo * 7)))
        t = t * (0.9 + 0.12 * veins)
    elif pattern == "wood":
        t = t * (0.88 + 0.2 * np.abs(np.sin(np.arange(w)[None, :] * 1.3 + lo * 3)))
    elif pattern == "scale":
        sc = ((np.arange(w)[None, :] + (np.arange(h)[:, None] // 2) * 2) % 4 < 2).astype(np.float32)
        t = t * (0.86 + 0.24 * sc)
    elif pattern == "wool":
        t = t * (0.85 + 0.3 * _noise(rng, h, w, 2))
    elif pattern == "fur" or pattern == "strand":
        t = t * (0.88 + 0.2 * np.abs(np.sin(np.arange(w)[None, :] * 1.7 + lo * 4)))
    elif pattern == "shell":
        t = t * (0.9 + 0.22 * _noise(rng, h, w, 3))
    elif pattern == "horn":
        t = t * (0.9 + 0.12 * np.sin(np.arange(h)[:, None] * 1.1))
    sh = SHADE.get(face, 1.0)
    rgb = np.clip(np.array(base, np.float32)[None, None, :] * (t * sh)[..., None], 0, 255)
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = rgb
    out[..., 3] = 255
    return out


class B:
    """The builder. `cube()` records the cube and adds it to the geo model; `save()` packs, paints and writes everything."""

    def __init__(self, key, name, tw=None, th=None):
        self.key, self.name = key, name
        self.m = Model(key, name, 64, 64)
        self.items = []

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent, pivot, rotation)

    def cube(self, bone, origin, size, mat, glow=None, inflate=0.0, rotation=None, pivot=None, mirror=False, decal=None, seed=None):
        """glow: None, an rgb tuple (the whole cube glows in it) or (rgb, strength). decal: (face, fn) with fn(draw, gdraw, rect) painting in texel space."""
        size = tuple(int(round(s)) for s in size)
        c = self.m.cube(bone, origin, size, (0, 0), inflate, rotation, pivot, mirror)
        self.items.append({"cube": c, "size": size, "mat": mat, "glow": glow, "decal": decal, "seed": len(self.items) * 7 + (seed or 0), "bone": bone})
        return c

    def lift(self, dy):
        """Moves the whole model up by dy pixels (a figure whose tail hangs below its feet): every pivot, origin and cube pivot."""
        for bone in self.m.bones.values():
            bone["pivot"][1] += dy
            for c in bone["cubes"]:
                c["origin"][1] += dy
                if "pivot" in c:
                    c["pivot"][1] += dy

    def lay_out(self):
        foot = [(2 * (it["size"][0] + it["size"][2]), it["size"][1] + it["size"][2]) for it in self.items]
        area = sum(w * h for w, h in foot)
        side = 64
        while True:
            u = v = row = 0
            ok = True
            pos = []
            order = sorted(range(len(foot)), key=lambda i: (-foot[i][1], -foot[i][0]))
            res = {}
            for i in order:
                w, h = foot[i]
                if w > side:
                    ok = False
                    break
                if u + w > side:
                    u, v, row = 0, v + row, 0
                if v + h > side:
                    ok = False
                    break
                res[i] = (u, v)
                u += w
                row = max(row, h)
            if ok:
                break
            side *= 2
        self.side = side
        for i, it in enumerate(self.items):
            it["cube"]["uv"] = list(res[i])
        self.m.tex_w = self.m.tex_h = side

    @staticmethod
    def faces(u, v, sx, sy, sz):
        return {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "right": (u, v + sz, sz, sy), "front": (u + sz, v + sz, sx, sy),
                "left": (u + sz + sx, v + sz, sz, sy), "back": (u + 2 * sz + sx, v + sz, sx, sy)}

    def paint(self):
        side = self.side
        img = np.zeros((side, side, 4), np.float32)
        glow = np.zeros((side, side, 4), np.float32)
        for it in self.items:
            u, v = it["cube"]["uv"]
            sx, sy, sz = it["size"]
            for fname, (x, y, w, h) in self.faces(u, v, sx, sy, sz).items():
                if w <= 0 or h <= 0:
                    continue
                img[y:y + h, x:x + w] = paint_face(it["mat"], w, h, it["seed"] * 31 + zlib.crc32(fname.encode()) % 97, fname)
                g = it["glow"]
                if g is not None:
                    col, st = (g, 1.0) if isinstance(g[0], int) else g
                    glow[y:y + h, x:x + w, :3] = np.array(col, np.float32)
                    glow[y:y + h, x:x + w, 3] = 255 * st
        base = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
        gl = Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA")
        d, gd = ImageDraw.Draw(base), ImageDraw.Draw(gl)
        for it in self.items:
            if it["decal"] is None:
                continue
            face, fn = it["decal"]
            u, v = it["cube"]["uv"]
            sx, sy, sz = it["size"]
            fn(d, gd, self.faces(u, v, sx, sy, sz)[face])
        return base, gl

    def save(self, anim=None):
        self.lay_out()
        base, gl = self.paint()
        self.m.save(anim)
        self.m.save_texture(base, gl)
        return self.m.id


# ------------------------------------------------------------------ small decals
def eyes(color=(30, 24, 40), glow=None, gap=2, y=3, size=1):
    """Two eyes on a face (x, y in texels from the face's top-left): a decal for the head's front."""
    def fn(d, gd, rect):
        x, y0, w, h = rect
        cx = x + w // 2
        for sgn in (-1, 1):
            ex = cx + sgn * (gap // 2 + 1) - (1 if sgn < 0 else 0) - (size - 1 if sgn < 0 else 0)
            d.rectangle([ex, y0 + y, ex + size - 1, y0 + y + max(0, size - 1) + (1 if size == 1 else 0)], fill=color + (255,))
            if glow:
                gd.rectangle([ex, y0 + y, ex + size - 1, y0 + y + max(0, size - 1) + (1 if size == 1 else 0)], fill=glow + (255,))
    return fn


def face(eye_color=(30, 24, 40), mouth=(150, 70, 80), glow=None, glasses=False, blush=False, y=3):
    def fn(d, gd, rect):
        x, y0, w, h = rect
        cx = x + w // 2
        for ex in (cx - 3, cx + 1):
            d.rectangle([ex, y0 + y, ex + 1, y0 + y + 1], fill=eye_color + (255,))
            if glow:
                gd.rectangle([ex, y0 + y, ex + 1, y0 + y + 1], fill=glow + (255,))
        d.rectangle([cx - 1, y0 + y + 3, cx, y0 + y + 3], fill=mouth + (255,))
        if blush:
            d.rectangle([cx - 4, y0 + y + 2, cx - 4, y0 + y + 2], fill=(246, 150, 150, 255))
            d.rectangle([cx + 3, y0 + y + 2, cx + 3, y0 + y + 2], fill=(246, 150, 150, 255))
        if glasses:
            d.rectangle([cx - 4, y0 + y - 1, cx - 1, y0 + y + 2], outline=(20, 20, 30, 255))
            d.rectangle([cx, y0 + y - 1, cx + 3, y0 + y + 2], outline=(20, 20, 30, 255))
            d.point([cx - 1, y0 + y], fill=(20, 20, 30, 255))
    return fn


def disc_decal(rgb, glow_rgb=None, ring=(30, 30, 40), hands=True):
    """A round clock face (or a seal) on a face."""
    def fn(d, gd, rect):
        x, y0, w, h = rect
        cx, cy, r = x + w / 2 - 0.5, y0 + h / 2 - 0.5, min(w, h) / 2 - 0.5
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=rgb + (255,), outline=ring + (255,))
        if hands:
            d.line([cx, cy, cx, cy - r * 0.7], fill=ring + (255,))
            d.line([cx, cy, cx + r * 0.5, cy], fill=ring + (255,))
            if glow_rgb:
                gd.line([cx, cy, cx, cy - r * 0.7], fill=glow_rgb + (255,))
                gd.line([cx, cy, cx + r * 0.5, cy], fill=glow_rgb + (255,))
        if glow_rgb:
            gd.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=glow_rgb + (255,))
    return fn


def keyhole(glow_rgb):
    def fn(d, gd, rect):
        x, y0, w, h = rect
        cx = x + w // 2
        d.rectangle([cx - 1, y0 + 1, cx, y0 + 2], fill=(20, 14, 30, 255))
        d.rectangle([cx - 1, y0 + 3, cx, y0 + h - 2], fill=(20, 14, 30, 255))
        gd.rectangle([cx - 1, y0 + 1, cx, y0 + 2], fill=glow_rgb + (255,))
        gd.rectangle([cx - 1, y0 + 3, cx, y0 + h - 2], fill=glow_rgb + (255,))
    return fn


# ------------------------------------------------------------------ animation helpers
def clips(a, bones, extras=None, big=1.0):
    """The four standard clips of a spirit on the standard skeleton (only the bones that exist are animated): idle (2 s loop), summon (0.7 s), cast (1.2 s), leave (0.7 s).
    extras(a) may add tracks for the spirit's own bones. Rotations in degrees about the bone's pivot; +x raises an arm to the front."""
    has = set(bones)
    a.clip("idle", length=2.0, loop=True)
    a.clip("summon", length=0.7, loop=False)
    a.clip("cast", length=1.2, loop=False)
    a.clip("leave", length=0.7, loop=False)

    def rot(clip, bone, frames):
        if bone in has:
            a.rot(clip, bone, frames)

    def pos(clip, bone, frames):
        if bone in has:
            a.pos(clip, bone, frames)

    pos("idle", "root", {0.0: (0, 0, 0), 1.0: (0, 1.2, 0), 2.0: (0, 0, 0)})
    rot("idle", "head", {0.0: (0, 0, 0), 1.0: (2.5, 3, 0), 2.0: (0, 0, 0)})
    rot("idle", "arm_r", {0.0: (0, 0, -2), 1.0: (6, 0, -4), 2.0: (0, 0, -2)})
    rot("idle", "arm_l", {0.0: (0, 0, 2), 1.0: (-6, 0, 4), 2.0: (0, 0, 2)})
    rot("idle", "body", {0.0: (0, 0, 0), 1.0: (1.5, 0, 0), 2.0: (0, 0, 0)})
    # summon: rises out of the circle, arms lifted then lowered, head up
    pos("summon", "root", {0.0: (0, -12, 0), 0.4: (0, -2, 0), 0.7: (0, 0, 0)})
    rot("summon", "arm_r", {0.0: (80, 0, -20), 0.5: (50, 0, -10), 0.7: (0, 0, -2)})
    rot("summon", "arm_l", {0.0: (80, 0, 20), 0.5: (50, 0, 10), 0.7: (0, 0, 2)})
    rot("summon", "head", {0.0: (-25, 0, 0), 0.5: (-8, 0, 0), 0.7: (0, 0, 0)})
    # cast: a thrust forward with the right arm, the left arm following
    rot("cast", "arm_r", {0.0: (0, 0, -2), 0.25: (-30, 0, -10), 0.5: (100, 0, 0), 0.9: (95, 0, 0), 1.2: (0, 0, -2)})
    rot("cast", "arm_l", {0.0: (0, 0, 2), 0.25: (-20, 0, 10), 0.5: (70, 0, 0), 0.9: (60, 0, 0), 1.2: (0, 0, 2)})
    rot("cast", "body", {0.0: (0, 0, 0), 0.25: (-8, 0, 0), 0.5: (10, 0, 0), 1.2: (0, 0, 0)})
    rot("cast", "head", {0.0: (0, 0, 0), 0.5: (-8, 0, 0), 1.2: (0, 0, 0)})
    pos("cast", "root", {0.0: (0, 0, 0), 0.3: (0, 0, 1.5), 0.6: (0, 1.5, -1), 1.2: (0, 0, 0)})
    # leave: arms cross, sinks
    pos("leave", "root", {0.0: (0, 0, 0), 0.7: (0, -12, 0)})
    rot("leave", "arm_r", {0.0: (0, 0, -2), 0.7: (30, 0, 30)})
    rot("leave", "arm_l", {0.0: (0, 0, 2), 0.7: (30, 0, -30)})
    rot("leave", "head", {0.0: (0, 0, 0), 0.7: (20, 0, 0)})
    if extras:
        extras(a)
    return a
