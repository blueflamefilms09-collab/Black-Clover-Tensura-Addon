"""Geo kit of the Sylph (docs/magic/sylph.md): hand-painted atlas + cubes with explicit uv windows, so a face, a hair strand card, a ruffle or a wing membrane
is ONE painted picture and the geometry stays low. Writes the files client.geo (GeoModels / GeoDraw) reads:

    assets/nusmp/geo/entity/<id>.geo.json         the model (Bedrock / GeckoLib format 1.12.0)
    assets/nusmp/textures/entity/<sheet>.png      the colour sheet (RGBA, painted at 4x and reduced, so the line work is clean)

Frame of the files (client.geo.GeoDraw): units are 1/16 block, y UP, origin at the feet, the model's front on -z. Rotations are degrees (Z, then Y, then X on the point).
A face of a cube is seen from OUTSIDE the cube, upright and not mirrored; on the up and down faces the bottom of the picture is the front (-z).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import firecast_geo_lib as L  # noqa: E402  (write_animations, sine, merge are shared with the Salamander kit)

ROOT = L.ROOT
sine = L.sine


def merge(*tracks):
    """Adds several {time: (x, y, z)} tracks. Each track is read at EVERY time any of them has a key (linear between its own keys, held at its ends), so a constant pose {0: v, T: v}
    and a sine with eight keys a cycle add up to the pose plus the sine all the way through, not only at the times they happen to share."""
    times = sorted({t for tr in tracks for t in tr})
    out = {}
    for t in times:
        acc = [0.0, 0.0, 0.0]
        for tr in tracks:
            ks = sorted(tr)
            if t <= ks[0]:
                v = tr[ks[0]]
            elif t >= ks[-1]:
                v = tr[ks[-1]]
            else:
                hi = next(k for k in ks if k >= t)
                lo = ks[ks.index(hi) - 1] if hi > t else hi
                f = 0.0 if hi == lo else (t - lo) / (hi - lo)
                v = tuple(tr[lo][i] + (tr[hi][i] - tr[lo][i]) * f for i in range(3))
            for i in range(3):
                acc[i] += v[i]
        out[t] = tuple(acc)
    return out


def write_animations(ident, clips):
    """clips: {name: {"loop": bool, "length": s, "bones": {bone: {"rotation"|"position"|"scale": {time: (x, y, z)}}}}} -> animations/entity/<id>.animation.json (keys at 1 ms)."""
    out = {}
    for name, c in clips.items():
        bones = {}
        for bone, tracks in c["bones"].items():
            bones[bone] = {k: {("%.3f" % t).rstrip("0").rstrip(".") if t else "0.0": [round(float(x), 3) for x in v] for t, v in sorted(frames.items())} for k, frames in tracks.items()}
        out[name] = {"loop": c["loop"], "animation_length": c["length"], "bones": bones}
    os.makedirs(os.path.join(ROOT, "animations", "entity"), exist_ok=True)
    with open(os.path.join(ROOT, "animations", "entity", ident + ".animation.json"), "w", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": out}, f, indent=1)
        f.write("\n")
BEDROCK = {"west": "east", "north": "north", "east": "west", "south": "south", "up": "up", "down": "down"}     # the loader reads Bedrock's sides from the other end of x
PAD = 2                                                     # transparent texels between two pictures (a reduced copy of the sheet must not bleed one into the next)
FACES = ("up", "down", "west", "north", "east", "south")


class Atlas:
    """Named PIL pictures packed in shelves of one sheet. region(name, image) -> (x, y, w, h)."""

    def __init__(self, width, max_height=1024):
        self.w, self.max_h = width, max_height
        self.items = []                       # (name, image)
        self.rects = {}

    def add(self, name, img):
        if name in dict(self.items):
            raise ValueError("duplicate region " + name)
        self.items.append((name, img.convert("RGBA")))

    def pack(self):
        x = y = row = 0
        for name, img in sorted(self.items, key=lambda it: -it[1].height):
            w, h = img.size
            if x + w > self.w:
                x, y, row = 0, y + row + PAD, 0
            self.rects[name] = (x, y, w, h)
            x += w + PAD
            row = max(row, h)
        self.h = y + row
        if self.h > self.max_h:
            raise ValueError("atlas too tall: %d" % self.h)
        self.h = int(math.ceil(self.h / 8.0) * 8)
        self.image = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        for name, img in self.items:
            x, y, w, h = self.rects[name]
            self.image.paste(img, (x, y))
        return self

    def rect(self, name, fx0=0.0, fy0=0.0, fx1=1.0, fy1=1.0):
        """The pixel window (x, y, w, h) of a fraction of a region."""
        x, y, w, h = self.rects[name]
        return (x + w * fx0, y + h * fy0, w * (fx1 - fx0), h * (fy1 - fy0))

    def save(self, name, scale=1.0, clean_alpha=True):
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        img = self.image
        if scale != 1.0:
            img = img.resize((max(1, int(self.w * scale)), max(1, int(self.h * scale))), Image.LANCZOS)
        a = np.asarray(img).copy()
        if clean_alpha:
            a[a[..., 3] == 0] = 0                           # nothing under a fully transparent texel (the safe additive rule; also keeps the png small)
        Image.fromarray(a, "RGBA").save(os.path.join(ROOT, "textures", "entity", name + ".png"), optimize=True)


class Model:
    """Bones and cubes. cube(bone, origin, size, faces) where faces maps a face name (or '*') to a pixel window (x, y, w, h) of the atlas, optionally (window, flip)."""

    def __init__(self, ident, atlas, tex_scale=1.0):
        self.id, self.atlas, self.tex_scale = ident, atlas, tex_scale
        self.bones, self.order = {}, []
        self.bone("root", (0, 0, 0))

    def bone(self, name, pivot=(0, 0, 0), parent=None, rotation=None):
        b = {"name": name, "pivot": [round(float(v), 3) for v in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        elif name != "root":
            b["parent"] = "root"
        if rotation and any(abs(v) > 1e-9 for v in rotation):
            b["rotation"] = [round(float(v), 3) for v in rotation]
        self.bones[name] = b
        self.order.append(name)
        return b

    def cube(self, bone, origin, size, faces, rot=None, pivot=None):
        c = {"origin": [round(float(v), 3) for v in origin], "size": [round(float(v), 3) for v in size]}
        if rot and any(abs(v) > 1e-9 for v in rot):
            c["rotation"] = [round(float(v), 3) for v in rot]
            c["pivot"] = [round(float(v), 3) for v in (pivot if pivot is not None else self.bones[bone]["pivot"])]
        uv = {}
        for f in FACES:
            spec = faces.get(f, faces.get("*"))
            if spec is None:
                continue
            flip = False
            if isinstance(spec, tuple) and len(spec) == 2 and isinstance(spec[1], bool):
                spec, flip = spec
            x, y, w, h = spec
            if flip:
                x, w = x + w, -w
            uv[BEDROCK[f]] = {"uv": [round(float(x), 2), round(float(y), 2)], "uv_size": [round(float(w), 2), round(float(h), 2)]}
        c["uv"] = uv
        self.bones[bone]["cubes"].append(c)
        return c

    def quads(self):
        return sum(len(c["uv"]) for b in self.bones.values() for c in b["cubes"])

    def cubes(self):
        return sum(len(b["cubes"]) for b in self.bones.values())

    def save(self):
        k = self.tex_scale
        bones = [self.bones[n] for n in self.order]
        if k != 1.0:
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
