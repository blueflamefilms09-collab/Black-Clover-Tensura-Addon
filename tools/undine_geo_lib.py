"""Geo model kit of the Undine (Noelle Silva's water Spirit Lord, docs/magic/undine.md). A model whose cubes and CARDS take their faces from a hand-painted atlas, so the detail is texture and
the geometry stays low: a card is a cube that is flat in z (one quad, drawn from both sides by the no-cull render types), which is what the streaming hair ribbons, the strips of the gown and the
waves are. Writes the files the other geo models of the mod use (read by client.geo.GeoModelData):

    assets/nusmp/geo/entity/<id>.geo.json          the model (Bedrock / GeckoLib format 1.12.0)
    assets/nusmp/animations/entity/<id>.animation.json
    assets/nusmp/textures/entity/<sheet>.png        the colour sheet

Frame of the files (client.geo.GeoDraw): units are 1/16 block, y UP, origin at the feet, the model's front on -z, +x is the model's LEFT. Cube coordinates are ABSOLUTE (the bone's pivot only
centres its own rotation). Rotation angles are degrees; the loader turns a point by Rx first, then Ry, then Rz (M = Rz . Ry . Rx as plain right-handed matrices of the file frame), cubes about
their own pivot, bones about theirs. {@link Xf} below mirrors that exactly so the generator can place things by maths (the preview checks it).

This file is the Undine's own copy of the small kit the Salamander uses (tools/firecast_geo_lib.py), extended with cards and the transform maths; the Salamander's file is not touched.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp")

BEDROCK = {"west": "east", "north": "north", "east": "west", "south": "south", "up": "up", "down": "down"}    # Bedrock names its sides from the other end of x
FACE_ORDER = ("up", "down", "west", "north", "east", "south")


def rgb(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)


# ====================================================================================================================== rotation maths (the loader's convention)
def rx_m(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]], dtype=np.float64)


def ry_m(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]], dtype=np.float64)


def rz_m(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]], dtype=np.float64)


def rot_m(r):
    """The file's rotation (degrees x, y, z) as a matrix: a point is turned by Rx first, then Ry, then Rz (JOML's rotationZYX is the product Rz . Ry . Rx)."""
    return rz_m(r[2]) @ ry_m(r[1]) @ rx_m(r[0])


def euler_of(m):
    """The inverse of rot_m: degrees (x, y, z) with m = Rz(z) . Ry(y) . Rx(x)."""
    b = -math.asin(max(-1.0, min(1.0, m[2][0])))
    if abs(math.cos(b)) < 1e-6:
        a = math.atan2(-m[1][2], m[1][1])
        c = 0.0
    else:
        a = math.atan2(m[2][1], m[2][2])
        c = math.atan2(m[1][0], m[0][0])
    return (math.degrees(a), math.degrees(b), math.degrees(c))


def look_card(normal, up_hint=(0, 1, 0), roll=0.0):
    """Euler (x, y, z) that turns a card (a flat quad whose front faces -z, whose u axis runs along +x and v axis along -y) so that its front faces `normal`, with its up as near to up_hint as
    the normal allows; then rolled by `roll` degrees about the normal."""
    n = np.array(normal, dtype=np.float64)
    n /= np.linalg.norm(n)
    f = -n                                   # the card's own -z maps to n, so its +z maps to -n
    u = np.array(up_hint, dtype=np.float64)
    u = u - n * np.dot(u, n)
    if np.linalg.norm(u) < 1e-6:
        u = np.array([1.0, 0.0, 0.0]) - n * n[0]
    u /= np.linalg.norm(u)
    x = np.cross(u, f)                       # right-handed numeric frame: x = y cross z
    x /= np.linalg.norm(x)
    m = np.stack([x, u, f], axis=1)
    if roll:
        m = rot_axis(n, roll) @ m
    return euler_of(m)


def rot_axis(axis, deg):
    a = np.array(axis, dtype=np.float64)
    a /= np.linalg.norm(a)
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    x, y, z = a
    return np.array([[c + x * x * (1 - c), x * y * (1 - c) - z * s, x * z * (1 - c) + y * s],
                     [y * x * (1 - c) + z * s, c + y * y * (1 - c), y * z * (1 - c) - x * s],
                     [z * x * (1 - c) - y * s, z * y * (1 - c) + x * s, c + z * z * (1 - c)]])


# ====================================================================================================================== atlas
def needed_height(sizes, width, pad=0):
    """The height a sheet of this width needs for the regions (name -> (w, h)) packed in shelves, tallest first, rounded up to 16."""
    x = y = row = 0
    for w, h in sorted(sizes.values(), key=lambda s: -s[1]):
        if x + w > width:
            x, y, row = 0, y + row, 0
        x += w
        row = max(row, h)
    return int(math.ceil((y + row) / 16.0) * 16)


class Atlas:
    """The texture sheet: named rectangles packed in shelves, each painted by a function returning an RGBA float array of its size (straight alpha, 0..255)."""

    def __init__(self, size_w, size_h):
        self.w, self.h = size_w, size_h
        self.regions = {}
        self.color = np.zeros((size_h, size_w, 4), dtype=np.float32)
        self._x = self._y = self._row = 0

    def region(self, name, w, h, painter=None):
        if self._x + w > self.w:
            self._x, self._y, self._row = 0, self._y + self._row, 0
        if self._y + h > self.h:
            raise ValueError("atlas full at %s (%d x %d)" % (name, w, h))
        x, y = self._x, self._y
        self._x += w
        self._row = max(self._row, h)
        self.regions[name] = (x, y, w, h)
        if painter is not None:
            self.color[y:y + h, x:x + w] = painter(w, h)
        return self.regions[name]

    def used(self):
        return (self._y + self._row) / float(self.h)

    def image(self, scale=1.0):
        a = np.clip(self.color, 0, 255).astype(np.uint8)
        img = Image.fromarray(a, "RGBA")
        if scale != 1.0:
            img = img.resize((int(self.w * scale), int(self.h * scale)), Image.LANCZOS)
        arr = np.asarray(img).copy()
        arr[arr[..., 3] == 0] = 0                                                  # nothing under a fully transparent texel (the safe additive rule)
        return Image.fromarray(arr, "RGBA")

    def save_color(self, name, scale=1.0):
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        self.image(scale).save(os.path.join(ROOT, "textures", "entity", name + ".png"), optimize=True)


# ====================================================================================================================== model
class Model:
    """Bones and cubes in the file frame. A cube's faces name a region of the atlas; only the faces that are given are drawn (a face that is not given costs nothing)."""

    def __init__(self, ident, atlas, tex_scale=1.0):
        self.id, self.atlas = ident, atlas
        self.tex_scale = tex_scale
        self.bones, self.order = {}, []
        self.bone("root", (0, 0, 0))
        self.count = 0

    def bone(self, name, pivot=(0, 0, 0), parent=None, rotation=None):
        b = {"name": name, "pivot": [float(v) for v in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        elif name != "root":
            b["parent"] = "root"
        if rotation:
            b["rotation"] = [float(v) for v in rotation]
        self.bones[name] = b
        self.order.append(name)
        return b

    def _uv(self, region, flip=False, window=None):
        x, y, w, h = self.atlas.regions[region]
        if window:                                                             # (u0, v0, u1, v1) in 0..1 of the region
            u0, v0, u1, v1 = window
            x, y, w, h = x + u0 * w, y + v0 * h, (u1 - u0) * w, (v1 - v0) * h
        if flip:
            return [x + w, y], [-w, h]
        return [x, y], [w, h]

    def cube(self, bone, origin, size, faces, rot=None, pivot=None):
        """faces: {'up'|'down'|'west'|'north'|'east'|'south'|'*': region name | (region, flip) | (region, flip, window)}; '*' fills the faces not named; None skips a face."""
        o = [float(v) for v in origin]
        s = [float(v) for v in size]
        c = {"origin": [round(v, 4) for v in o], "size": [round(v, 4) for v in s]}
        if rot is not None and any(abs(v) > 1e-9 for v in rot):
            c["rotation"] = [round(float(v), 4) for v in rot]
            c["pivot"] = [round(float(v), 4) for v in (pivot if pivot is not None else self.bones[bone]["pivot"])]
        uv = {}
        self.count += 1
        for f in FACE_ORDER:
            spec = faces.get(f, faces.get("*"))
            if spec is None:
                continue
            if isinstance(spec, str):
                spec = (spec, False, None)
            region = spec[0]
            flip = spec[1] if len(spec) > 1 else False
            window = spec[2] if len(spec) > 2 else None
            xy, wh = self._uv(region, flip, window)
            uv[BEDROCK[f]] = {"uv": [round(v, 3) for v in xy], "uv_size": [round(v, 3) for v in wh]}
        c["uv"] = uv
        self.bones[bone]["cubes"].append(c)
        return c

    def card(self, bone, center, size, euler, region, flip=False, window=None, both=False):
        """A flat quad of size (w, h) centred on `center` (absolute), turned by `euler` about its centre: its u runs along the card's +x, v down its -y. One quad, or two with both=True (front and
        back with their own normals; the back is the mirror image)."""
        w, h = size
        faces = {"north": (region, flip, window)}
        if both:                                                                  # a thin slab, so the two faces are not in one plane (no z-fight)
            faces["south"] = (region, not flip, window)
            return self.cube(bone, (center[0] - w / 2.0, center[1] - h / 2.0, center[2] - 0.03), (w, h, 0.06), faces, rot=euler, pivot=center)
        return self.cube(bone, (center[0] - w / 2.0, center[1] - h / 2.0, center[2]), (w, h, 0.0), faces, rot=euler, pivot=center)

    def quads(self):
        return sum(len(c["uv"]) for b in self.bones.values() for c in b["cubes"])

    def cubes(self):
        return sum(len(b["cubes"]) for b in self.bones.values())

    def save(self):
        bones = [self.bones[n] for n in self.order]
        k = self.tex_scale
        if k != 1.0:                                                         # the same layout on a smaller copy of the sheet
            bones = json.loads(json.dumps(bones))
            for b in bones:
                for c in b["cubes"]:
                    for f in c["uv"].values():
                        f["uv"] = [round(v * k, 3) for v in f["uv"]]
                        f["uv_size"] = [round(v * k, 3) for v in f["uv_size"]]
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.id, "texture_width": int(self.atlas.w * k), "texture_height": int(self.atlas.h * k),
                            "visible_bounds_width": 8, "visible_bounds_height": 8, "visible_bounds_offset": [0, 1, 0]}, "bones": bones}]}
        os.makedirs(os.path.join(ROOT, "geo", "entity"), exist_ok=True)
        with open(os.path.join(ROOT, "geo", "entity", self.id + ".geo.json"), "w", newline="\n") as f:
            json.dump(geo, f, indent=1)
            f.write("\n")


def write_animations(ident, clips):
    """clips: {name: {"loop": bool|'hold', "length": s, "bones": {bone: {"rotation"|"position"|"scale": {time: (x, y, z)}}}}} -> animations/entity/<id>.animation.json"""
    out = {}
    for name, c in clips.items():
        bones = {}
        for bone, tracks in c["bones"].items():
            bones[bone] = {k: {"%.3f" % t: [round(float(x), 3) for x in v] for t, v in sorted(frames.items())} for k, frames in tracks.items()}
        loop = c["loop"]
        out[name] = {"loop": "hold_on_last_frame" if loop == "hold" else bool(loop), "animation_length": c["length"], "bones": bones}
    os.makedirs(os.path.join(ROOT, "animations", "entity"), exist_ok=True)
    with open(os.path.join(ROOT, "animations", "entity", ident + ".animation.json"), "w", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": out}, f, indent=1)
        f.write("\n")


def sine(length, amp, axis=0, phase=0.0, steps=8, offset=(0, 0, 0), bias=0.0):
    """A closed sine track of one bone's rotation: {time: (x, y, z)} for `steps` keys and the closing key."""
    fr = {}
    for i in range(steps + 1):
        t = i / steps
        a = bias + amp * math.sin((t + phase) * math.tau)
        v = list(offset)
        v[axis] += a
        fr[t * length] = tuple(v)
    return fr


def const(length, v, steps=8):
    return {i / steps * length: tuple(v) for i in range(steps + 1)}


def merge(*tracks):
    """Adds several {time: (x, y, z)} tracks that share their times (sums the vectors)."""
    out = {}
    for tr in tracks:
        for t, v in tr.items():
            o = out.get(t, (0, 0, 0))
            out[t] = (o[0] + v[0], o[1] + v[1], o[2] + v[2])
    return out


# ====================================================================================================================== the loader's transform, in python (to place and to check cards)
class Xf:
    """Evaluates the file-frame position of a point of a cube after the bone chain and the cube's own turn, with the bones' rest rotations plus optional per-bone offsets (an animation frame)."""

    def __init__(self, model):
        self.m = model

    def chain(self, bone):
        out = []
        b = bone
        while b is not None:
            out.append(b)
            p = self.m.bones[b].get("parent")
            b = p if p in self.m.bones and p != b else None
        return out[::-1]

    def apply(self, bone, point, cube=None, offsets=None):
        p = np.array(point, dtype=np.float64)
        if cube is not None and "rotation" in cube:
            c = np.array(cube["pivot"], dtype=np.float64)
            p = c + rot_m(cube["rotation"]) @ (p - c)
        for name in reversed(self.chain(bone)):
            b = self.m.bones[name]
            piv = np.array(b["pivot"], dtype=np.float64)
            r = list(b.get("rotation", (0, 0, 0)))
            if offsets and name in offsets:
                r = [r[i] + offsets[name][i] for i in range(3)]
            p = piv + rot_m(r) @ (p - piv)
        return p

    def cube_corners(self, bone, cube, offsets=None):
        o, s = cube["origin"], cube["size"]
        pts = []
        for i in range(8):
            pts.append(self.apply(bone, (o[0] + (s[0] if i & 1 else 0), o[1] + (s[1] if i & 2 else 0), o[2] + (s[2] if i & 4 else 0)), cube, offsets))
        return pts
