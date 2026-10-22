"""Geo model kit of the fire casting styles (Fuegoleon's Salamander and its dragon form, docs/magic/fire_casting.md): a model whose cubes take their faces from a HAND-PAINTED
atlas (one shared sheet per form: scales with ring spots, belly plates, head faces with the slit eyes, spikes, claws, wing membranes with veins), so the detail is texture and
the geometry stays low. Writes the same files the other geo models of the mod use:

    assets/nusmp/geo/entity/<id>.geo.json        the model (Bedrock / GeckoLib format 1.12.0, read by client.geo.GeoModelData)
    assets/nusmp/textures/entity/<sheet>.png     the colour sheet
    assets/nusmp/textures/entity/<sheet>_glow.png the glow sheet (additive), same layout, painted for the eyes, the mouth and the spots

Frame of the files (client.geo.GeoDraw): units are 1/16 block, y UP, origin at the feet, the model's front on -z, +x is the model's LEFT. Rotations are degrees.
Several models (the LODs and the glow-only model) share ONE sheet: the same regions, the same uv, so they are drawn with the same textures.
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


def smoothstep(x, a, b):
    t = np.clip((x - a) / max(1e-6, b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def fbm(rng, w, h, octaves=3, base=3):
    out = np.zeros((h, w), dtype=np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = base * (2 ** o)
        g = rng.random((n + 2, n + 2)).astype(np.float32)
        ys = np.linspace(0, n, h, endpoint=False)
        xs = np.linspace(0, n, w, endpoint=False)
        y0 = ys.astype(int)[:, None]
        x0 = xs.astype(int)[None, :]
        fy = (ys - y0[:, 0])[:, None]
        fx = (xs - x0[0, :])[None, :]
        fy = fy * fy * (3 - 2 * fy)
        fx = fx * fx * (3 - 2 * fx)
        a = g[y0, x0] * (1 - fx) + g[y0, x0 + 1] * fx
        b = g[y0 + 1, x0] * (1 - fx) + g[y0 + 1, x0 + 1] * fx
        out += (a * (1 - fy) + b * fy) * amp
        tot += amp
        amp *= 0.5
    return out / tot


class Atlas:
    """The texture sheet: named rectangles packed in shelves, each painted by a function returning (colour RGBA, glow RGBA) float arrays of its size."""

    def __init__(self, size_w, size_h):
        self.w, self.h = size_w, size_h
        self.regions = {}
        self.color = np.zeros((size_h, size_w, 4), dtype=np.float32)
        self.glow = np.zeros((size_h, size_w, 4), dtype=np.float32)
        self._x = self._y = self._row = 0

    def region(self, name, w, h, painter=None):
        if self._x + w > self.w:
            self._x, self._y, self._row = 0, self._y + self._row, 0
        if self._y + h > self.h:
            raise ValueError("atlas full at " + name)
        x, y = self._x, self._y
        self._x += w
        self._row = max(self._row, h)
        self.regions[name] = (x, y, w, h)
        if painter is not None:
            c, g = painter(w, h)
            self.color[y:y + h, x:x + w] = c
            self.glow[y:y + h, x:x + w] = g
        return self.regions[name]

    def save_color(self, name, scale=1.0):
        """The colour sheet as textures/entity/<name>.png, at `scale` times its size (a lower level of detail reads a smaller copy of the same layout)."""
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        img = Image.fromarray(np.clip(self.color, 0, 255).astype(np.uint8), "RGBA")
        if scale != 1.0:
            img = img.resize((int(self.w * scale), int(self.h * scale)), Image.LANCZOS)
        img.save(os.path.join(ROOT, "textures", "entity", name + ".png"), optimize=True)

    def save_glow(self, name, scale=0.5):
        """The glow sheet as textures/entity/<name>.png (the glow-only model's own texture, drawn additive), at `scale` times the sheet's size."""
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        img = Image.fromarray(np.clip(self.glow, 0, 255).astype(np.uint8), "RGBA")
        img = img.resize((int(self.w * scale), int(self.h * scale)), Image.LANCZOS)
        a = np.asarray(img).copy()
        a[a[..., 3] == 0] = 0                                                  # nothing under a fully transparent texel (the safe additive rule)
        img = Image.fromarray(a, "RGBA")
        img.save(os.path.join(ROOT, "textures", "entity", name + ".png"), optimize=True)


class Model:
    """Bones and cubes in the file frame. A cube's faces name a region of the atlas (and which part of it); only the faces that are given are drawn (a face that is not given costs nothing)."""

    def __init__(self, ident, atlas, seed=1, tex_scale=1.0):
        self.id, self.atlas, self.rng = ident, atlas, np.random.default_rng(seed)
        self.tex_scale = tex_scale                                           # the model reads a copy of the sheet at this scale (a LOD, the glow-only model)
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

    def uv_of(self, region, face_w, face_h, d, k):
        """The uv rectangle of one face: a window of the named region at `d` texels per model unit (smaller than the face's own size when the region is smaller), at a place that
        depends on `k` so that neighbouring cubes do not repeat the same scales. d <= 0 means the whole region."""
        x, y, w, h = self.atlas.regions[region]
        if d <= 0:
            return [x, y], [w, h]
        fw, fh = max(1, int(round(face_w * d))), max(1, int(round(face_h * d)))
        fw, fh = min(fw, w), min(fh, h)
        ox = (k * 37) % max(1, w - fw + 1)
        oy = (k * 53) % max(1, h - fh + 1)
        return [x + ox, y + oy], [fw, fh]

    def cube(self, bone, origin, size, faces, rot=None, pivot=None, d=3.0):
        """faces: {'up'|'down'|'west'|'north'|'east'|'south'|'*': region name or (region, density) or (region, density, flip)}; '*' fills the faces not named; None skips a face."""
        o = [float(v) for v in origin]
        s = [float(v) for v in size]
        c = {"origin": o, "size": s}
        if rot:
            c["rotation"] = [float(v) for v in rot]
            c["pivot"] = [float(v) for v in (pivot if pivot is not None else self.bones[bone]["pivot"])]
        fsize = {"up": (s[0], s[2]), "down": (s[0], s[2]), "west": (s[2], s[1]), "east": (s[2], s[1]), "north": (s[0], s[1]), "south": (s[0], s[1])}
        uv = {}
        self.count += 1
        for f in FACE_ORDER:
            spec = faces.get(f, faces.get("*"))
            if spec is None:
                continue
            region, density, flip = (spec, d, False) if isinstance(spec, str) else (spec[0], spec[1] if len(spec) > 1 else d, spec[2] if len(spec) > 2 else False)
            fw, fh = fsize[f]
            xy, wh = self.uv_of(region, fw, fh, density, self.count)
            if flip:                                                           # a negative uv_size mirrors the face (the loader keeps the corners in order)
                xy = [xy[0] + wh[0], xy[1]]
                wh = [-wh[0], wh[1]]
            uv[BEDROCK[f]] = {"uv": [int(v) for v in xy], "uv_size": [int(v) for v in wh]}
        c["uv"] = uv
        self.bones[bone]["cubes"].append(c)
        return c

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
                        f["uv"] = [v * k for v in f["uv"]]
                        f["uv_size"] = [v * k for v in f["uv_size"]]
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.id, "texture_width": int(self.atlas.w * k), "texture_height": int(self.atlas.h * k),
                            "visible_bounds_width": 8, "visible_bounds_height": 8, "visible_bounds_offset": [0, 1, 0]}, "bones": bones}]}
        os.makedirs(os.path.join(ROOT, "geo", "entity"), exist_ok=True)
        with open(os.path.join(ROOT, "geo", "entity", self.id + ".geo.json"), "w", newline="\n") as f:
            json.dump(geo, f, indent=1)
            f.write("\n")


def write_animations(ident, clips):
    """clips: {name: {"loop": bool, "length": s, "bones": {bone: {"rotation"|"position"|"scale": {time: (x, y, z)}}}}} -> animations/entity/<id>.animation.json"""
    out = {}
    for name, c in clips.items():
        bones = {}
        for bone, tracks in c["bones"].items():
            bones[bone] = {k: {"%.2f" % t: [float(x) for x in v] for t, v in sorted(frames.items())} for k, frames in tracks.items()}
        out[name] = {"loop": c["loop"], "animation_length": c["length"], "bones": bones}
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


def merge(*tracks):
    """Adds several {time: (x, y, z)} tracks that share their times (sums the vectors)."""
    out = {}
    for tr in tracks:
        for t, v in tr.items():
            o = out.get(t, (0, 0, 0))
            out[t] = (o[0] + v[0], o[1] + v[1], o[2] + v[2])
    return out
