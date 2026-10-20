"""tdlib: the building kit of the Tiny Djinn generator (tools/gen_tiny_djinn.py): palettes, the 64 x 64 texture atlas, swatch cubes, painted boxes, the
painters (head, torso, arms, tails, cape) and the small shape helpers (spikes, arcs, rings) the part recipes in tdparts.py are written with.

Frame (the one GeoDraw reads): pixels (1/16 block), y UP, the model's front on -z, +x is the model's left, origin at the waist. Rotations are degrees in the
file's frame; to know which way a sign turns, read GeoDraw.rotate (rz > 0 tilts the +y axis towards -x, rx > 0 tilts it towards +z, ry > 0 turns the front towards -x).

Texture: 64 x 64. The top 56 rows hold the painted boxes (shelf packed, deterministic), the bottom 8 rows are the SWATCH band: 32 strips of 2 x 8 px, each a
light-to-dark gradient of one material. Small ornaments are cubes whose six faces all map onto one strip (per-face uv), so a model with 120 cubes needs no more
texture than one with 60. The light and the dark texture share the layout, so one geo file serves both: the cubes of the Dark Djinn's thorn crown and eye crest
use two strips that are transparent in the light texture.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
from geo_builder import Model  # noqa: E402

TEX = 64
BAND_Y = 56
SWATCH_NAMES = ["skin", "skin2", "hair", "hair2", "cloth", "cloth2", "trim", "metal", "accent", "glow", "eye", "white", "black", "fire", "water", "ice",
                "leaf", "wood", "bone", "gold", "silver", "crimson", "shadow", "gem", "bolt", "smoke", "wind", "iron", "feather", "coral", "dkthorn", "dkeye"]
SW = {n: i for i, n in enumerate(SWATCH_NAMES)}
assert len(SWATCH_NAMES) == 32

SKIN_TONES = {"pale": "F6DCCB", "fair": "F0C8A8", "tan": "D9A074", "brown": "A66E48", "dark": "6E4630", "blue": "9CC8E8", "green": "A8D0A0", "ash": "B8B4C4",
              "violet": "BBA0D8", "red": "D8806A", "gold": "E8C070", "teal": "86C8C0", "grey": "9EA4AE", "bone": "E6E0CC"}


# ================================================================ colour helpers
def hexrgb(h):
    if isinstance(h, (tuple, list)):
        return tuple(float(v) for v in h[:3])
    h = h.lstrip("#")
    return tuple(float(int(h[i:i + 2], 16)) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def mul(c, k):
    return tuple(max(0.0, min(255.0, v * k)) for v in c[:3])


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def hsh(*k):
    h = 0
    for v in k:
        h = (h * 1000003 + int(v * 97) + 12345) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return (h & 0xFFFF) / 65535.0


# ================================================================ palettes
def light_palette(spec):
    """The palette of the Djinn in its own light: the spec's colours, resolved."""
    p = spec["pal"]
    skin = hexrgb(SKIN_TONES.get(spec.get("skin", "fair"), spec.get("skin", "fair")))
    P = {"skin": skin, "skin2": mul(skin, 0.78), "hair": hexrgb(p["hair"]), "cloth": hexrgb(p["cloth"]), "cloth2": hexrgb(p["cloth2"]), "trim": hexrgb(p["trim"]),
         "metal": hexrgb(p["metal"]), "accent": hexrgb(p["accent"]), "glow": hexrgb(p["glow"]), "eye": hexrgb(p.get("eye", p["glow"])), "dark": False}
    P["hair2"] = mix(P["hair"], (255, 255, 255), 0.28)
    return P


def dark_palette(spec, lp):
    """The Dark Djinn's palette: black metal and cloth, gold trim, ash skin, the eyes and the glow keep the Djinn's own colour (as the full equip's dark look does)."""
    skin = mul(mix(lp["skin"], (150, 140, 168), 0.6), 0.78)
    P = {"skin": skin, "skin2": mul(skin, 0.75), "hair": mix(lp["hair"], (18, 14, 26), 0.72), "cloth": mix(lp["cloth"], (16, 12, 22), 0.84),
         "cloth2": mix(lp["cloth2"], (28, 20, 38), 0.80), "trim": (208, 166, 66), "metal": (30, 28, 38), "accent": lp["glow"], "glow": lp["glow"],
         "eye": lp["glow"], "dark": True}
    P["hair2"] = mix(P["hair"], (120, 90, 150), 0.25)
    return P


def swatch_table(P):
    """name -> (top rgb, bottom rgb, alpha, glow strength 0..1). The elemental strips are fixed colours; the dark set drowns them."""
    dk = P["dark"]

    def el(c):
        return mul(mix(c, (40, 30, 56), 0.55), 0.8) if dk else c

    t = {
        "skin": (mul(P["skin"], 1.08), mul(P["skin"], 0.84), 255, 0.0),
        "skin2": (P["skin2"], mul(P["skin2"], 0.8), 255, 0.0),
        "hair": (mul(P["hair"], 1.18), mul(P["hair"], 0.72), 255, 0.0),
        "hair2": (mul(P["hair2"], 1.1), mul(P["hair2"], 0.8), 255, 0.0),
        "cloth": (mul(P["cloth"], 1.18), mul(P["cloth"], 0.7), 255, 0.0),
        "cloth2": (mul(P["cloth2"], 1.15), mul(P["cloth2"], 0.7), 255, 0.0),
        "trim": (mul(P["trim"], 1.22), mul(P["trim"], 0.74), 255, 0.0),
        "metal": (mul(P["metal"], 1.45), mul(P["metal"], 0.7), 255, 0.0),
        "accent": (mul(P["accent"], 1.2), mul(P["accent"], 0.75), 255, 0.25),
        "glow": (mix(P["glow"], (255, 255, 255), 0.35), P["glow"], 255, 1.0),
        "eye": (mix(P["eye"], (255, 255, 255), 0.4), P["eye"], 255, 1.0),
        "white": (el((250, 250, 252)), el((196, 200, 214)), 255, 0.0),
        "black": ((46, 42, 56), (14, 12, 20), 255, 0.0),
        "fire": (el((255, 236, 120)), el((236, 78, 20)), 255, 0.75),
        "water": (el((190, 244, 255)), el((40, 134, 214)), 235, 0.35),
        "ice": (el((236, 252, 255)), el((120, 190, 232)), 245, 0.3),
        "leaf": (el((148, 212, 84)), el((40, 110, 52)), 255, 0.0),
        "wood": (el((152, 104, 60)), el((74, 46, 26)), 255, 0.0),
        "bone": (el((246, 240, 218)), el((176, 164, 134)), 255, 0.0),
        "gold": ((255, 226, 120), (176, 120, 30), 255, 0.15),
        "silver": (el((240, 244, 252)), el((120, 128, 150)), 255, 0.0),
        "crimson": (el((200, 40, 56)), el((86, 10, 26)), 255, 0.0),
        "shadow": ((54, 46, 70), (16, 12, 26), 255, 0.0),
        "gem": (mix(P["accent"], (255, 255, 255), 0.5), P["accent"], 255, 1.0),
        "bolt": (el((255, 255, 214)), el((140, 190, 255)), 255, 1.0),
        "smoke": (mix(P["cloth2"], (240, 240, 250), 0.55), mix(P["cloth"], (120, 120, 150), 0.4), 215, 0.0),
        "wind": (el((244, 255, 250)), el((156, 224, 206)), 215, 0.2),
        "iron": ((120, 124, 138), (48, 50, 62), 255, 0.0),
        "feather": (mix(P["cloth2"], (255, 255, 255), 0.6), mix(P["cloth2"], (200, 200, 210), 0.4), 255, 0.0),
        "coral": (el((255, 168, 150)), el((214, 80, 92)), 255, 0.0),
        "dkthorn": ((62, 54, 78), (10, 8, 16), 255 if dk else 0, 0.0),
        "dkeye": (mix(P["glow"], (255, 255, 255), 0.5), P["glow"], 255 if dk else 0, 1.0 if dk else 0.0),
    }
    return t


# ================================================================ canvas and atlas
class Canvas:
    def __init__(self):
        self.img = np.zeros((TEX, TEX, 4), np.float32)
        self.glow = np.zeros((TEX, TEX, 4), np.float32)

    def px(self, x, y, rgb, a=255, g=None, gk=1.0):
        if 0 <= x < TEX and 0 <= y < TEX:
            self.img[y, x] = (rgb[0], rgb[1], rgb[2], a)
            if g is not None:
                self.glow[y, x] = (g[0] * gk, g[1] * gk, g[2] * gk, 255)
            else:
                self.glow[y, x] = (0, 0, 0, 0)

    def get(self, x, y):
        return tuple(self.img[y, x][:3])

    def paint_band(self, P):
        t = swatch_table(P)
        for name, i in SW.items():
            top, bot, a, gk = t[name]
            for col in range(2):
                for row in range(8):
                    k = row / 7.0
                    c = mix(top, bot, k)
                    if col == 1:
                        c = mul(c, 0.93)
                    self.px(i * 2 + col, BAND_Y + row, c, a, c if gk > 0 else None, gk)

    def images(self):
        return (Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8), "RGBA"),
                Image.fromarray(np.clip(self.glow, 0, 255).astype(np.uint8), "RGBA"))


class Atlas:
    """Shelf packer of the painted boxes in the 64 x 56 area above the swatch band."""

    def __init__(self):
        self.x = self.y = self.row = 0

    def alloc(self, w, h):
        if self.x + w > TEX:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > BAND_Y:
            raise RuntimeError("tiny djinn atlas is full (%d rows used)" % self.y)
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v


def box_rects(u, v, w, h, d):
    """The vanilla box layout of a w x h x d cube at u, v: face -> (x, y, fw, fh). right = -x (the model's right), left = +x."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


FACE_LIGHT = {"top": 1.10, "front": 1.0, "right": 0.88, "left": 0.88, "back": 0.80, "bottom": 0.60}


def fill_box(T, rects, fn):
    """fn(face, i, j, fw, fh) -> (rgb, alpha, glow rgb or None, glow strength). Applies the face light."""
    for face, (x0, y0, fw, fh) in rects.items():
        k = FACE_LIGHT[face]
        for j in range(fh):
            for i in range(fw):
                r = fn(face, i, j, fw, fh)
                if r is None:
                    continue
                rgb, a = r[0], r[1]
                g = r[2] if len(r) > 2 else None
                gk = r[3] if len(r) > 3 else 1.0
                T.px(x0 + i, y0 + j, mul(rgb, k), a, g, gk)


def edge_shade(i, j, fw, fh, amount=0.14):
    """A soft darker rim on a face so a small box reads as a box."""
    e = min(i, j, fw - 1 - i, fh - 1 - j)
    return 1.0 - (amount if e == 0 and min(fw, fh) > 3 else 0.0)


# ================================================================ the context a recipe builds in
BONES = [  # name, parent, pivot (file frame)
    ("root", None, (0, 0, 0)), ("body", "root", (0, 0, 0)), ("head", "body", (0, 6, 0)), ("lids", "head", (0, 12, -4.5)), ("hair", "head", (0, 6, 0)),
    ("crown", "head", (0, 15, 0)), ("brow", "head", (0, 12, -4.5)), ("ear_l", "head", (4.5, 11, 0)), ("ear_r", "head", (-4.5, 11, 0)),
    ("horn_l", "head", (3, 14, 0)), ("horn_r", "head", (-3, 14, 0)), ("dark", "head", (0, 6, 0)),
    ("arm_l", "body", (4, 5.5, 0)), ("hand_l", "arm_l", (4, 0.5, 0)), ("arm_r", "body", (-4, 5.5, 0)), ("hand_r", "arm_r", (-4, 0.5, 0)),
    ("weapon", "hand_r", (-4, 0.5, -1)), ("cape", "body", (0, 5.5, 2)), ("scarf", "body", (0, 5.5, 0)), ("back", "body", (0, 4, 3)), ("acc", "body", (0, 3, 0)),
    ("tail1", "body", (0, 0, 0)), ("tail2", "tail1", (0, -4, 0)), ("tail3", "tail2", (0, -8, 0)), ("tail4", "tail3", (0, -12, 0)),
]
BONE_NAMES = [b[0] for b in BONES]


class Ctx:
    def __init__(self, spec):
        self.spec = spec
        self.m = Model("tinydjinn", spec["id"], TEX, TEX)
        for n, par, pv in BONES:
            self.m.bone(n, parent=par, pivot=pv)
        self.atlas = Atlas()
        self.jobs = []                      # (rects, painter(T, P, rects, spec))
        self.rng_i = 0

    # ---- cubes
    def cube(self, bone, origin, size, sw, rot=None, pivot=None, infl=0.0):
        """A swatch cube: every face maps onto the 2 x 8 strip of the material `sw`."""
        i = SW[sw]
        face = {"uv": [i * 2, BAND_Y], "uv_size": [2, 8]}
        uv = {k: {"uv": list(face["uv"]), "uv_size": list(face["uv_size"])} for k in ("north", "south", "east", "west", "up", "down")}
        o = tuple(round(v, 3) for v in origin)
        s = tuple(round(v, 3) for v in size)
        piv = tuple(round(v, 3) for v in pivot) if pivot is not None else None
        return self.m.cube(bone, o, s, uv=uv, inflate=infl, rotation=rot, pivot=piv)

    def pbox(self, bone, origin, size, painter, rot=None, pivot=None, mirror=False, uv_from=None):
        """A painted box (box uv). `uv_from` reuses the layout of an earlier box (the left arm shares the right arm's skin)."""
        w, h, d = (int(round(v)) for v in size)
        if uv_from is None:
            u, v = self.atlas.alloc(2 * (w + d), h + d)
            rects = box_rects(u, v, w, h, d)
            self.jobs.append((rects, painter))
            uv_from = (u, v)
        self.m.cube(bone, tuple(origin), (w, h, d), uv=uv_from, rotation=rot, pivot=pivot, mirror=mirror)
        return uv_from

    # ---- shapes
    def spike(self, bone, base, h, w, rot=(0, 0, 0), sw="trim", wz=None, infl=0.0):
        """A cube standing on `base` (its bottom centre), h tall, w wide, tilted about its base: a horn tooth, a feather, a spire."""
        x, y, z = base
        wz = w if wz is None else wz
        return self.cube(bone, (x - w / 2, y, z - wz / 2), (w, h, wz), sw, rot=rot if any(rot) else None, pivot=(x, y, z) if any(rot) else None, infl=infl)

    def arc(self, bone, start, phi0, turn, n, seglen, w0, w1, sw, plane="xy", wz=None):
        """A curved horn / tusk / tail: n spikes along an arc in the xy plane (or the yz plane), each tilted along it. phi0 is the start direction in degrees from +y
        towards +x (xy plane) or towards -z (yz plane); `turn` the total bend over the arc. Returns the end point."""
        x, y, z = start
        for k in range(n):
            t = k / max(1, n - 1)
            phi = phi0 + turn * t
            w = w0 + (w1 - w0) * t
            a = math.radians(phi)
            if plane == "xy":
                self.spike(bone, (x, y, z), seglen, w, rot=(0, 0, -phi), sw=sw, wz=wz)
                x += math.sin(a) * seglen * 0.92
                y += math.cos(a) * seglen * 0.92
            else:
                self.spike(bone, (x, y, z), seglen, w, rot=(-phi, 0, 0), sw=sw, wz=wz)
                z -= math.sin(a) * seglen * 0.92
                y += math.cos(a) * seglen * 0.92
        return (x, y, z)

    def ring(self, bone, centre, r, n, w, h, sw, tilt=(0, 0, 0), thick=None, phase=0.0):
        """A horizontal ring of n cubes (a halo, a collar); `tilt` tilts every cube about the ring centre (a halo set at an angle)."""
        cx, cy, cz = centre
        thick = w if thick is None else thick
        for k in range(n):
            a = phase + 2 * math.pi * k / n
            x, z = cx + math.cos(a) * r, cz + math.sin(a) * r
            self.cube(bone, (x - w / 2, cy - h / 2, z - thick / 2), (w, h, thick), sw, rot=(0, -math.degrees(a) + 90, 0), pivot=(x, cy, z))

    def disc_v(self, bone, centre, r, n, w, thick, sw, phase=0.0):
        """A ring standing in the xy plane (a vertical wheel, a sun disc behind the back): n cubes along the rim, each turned to follow it."""
        cx, cy, cz = centre
        for k in range(n):
            a = phase + 2 * math.pi * k / n
            x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
            self.cube(bone, (x - w / 2, y - w / 2, cz - thick / 2), (w, w, thick), sw, rot=(0, 0, math.degrees(a)), pivot=(x, y, cz))

    def rays(self, bone, centre, r0, length, n, w, sw, thick=0.8, phase=0.0, skip=None):
        cx, cy, cz = centre
        for k in range(n):
            if skip and k in skip:
                continue
            a = phase + 2 * math.pi * k / n
            x, y = cx + math.cos(a) * r0, cy + math.sin(a) * r0
            phi = math.degrees(a) - 90          # the ray points along a: from +y turn by (a - 90deg) -> tilt rz = -(...)
            self.cube(bone, (x - w / 2, y, cz - thick / 2), (w, length, thick), sw, rot=(0, 0, phi), pivot=(x, y, cz))

    def count(self):
        return self.m.cube_count()


def face_name_sides():
    return ("right", "left")
