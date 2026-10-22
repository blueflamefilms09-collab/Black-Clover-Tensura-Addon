"""Geo model kit of the Spirit of Light (docs/magic/spirit_light.md): the lion cub, the seraph, their levels of detail and their glow-only models. A model is a set of bones and cubes; every cube
FACE that is drawn gets a region of the model's own texture sheet, sized from the face (texels per model unit = the sheet's density) and painted by a named painter (tools/gen_spirit_light_art.py),
so every face can carry its own cel-shaded painting and thin dark linework: the geometry stays low and the detail is texture. Faces that are not given cost nothing (no quad, no texels).

Writes the files the other geo models of the mod use (client.geo.GeoModelData / GeoDraw):

    assets/nusmp/geo/entity/<id>.geo.json            the model (Bedrock / GeckoLib format 1.12.0)
    assets/nusmp/textures/entity/<id>.png            its sheet (colour sheet; for a glow-only model the ADDITIVE sheet, nothing under a fully transparent pixel)
    assets/nusmp/animations/entity/<id>.animation.json   the clips of the full model (the levels of detail and the glow model are driven by the same file)

Frame of the files: units are 1/16 block, y UP, origin at the feet, the model's front on -z, +x is the model's LEFT. Rotations are degrees. A region may be shared between faces by a `key` (the left
and right wing use one painting, the right one mirrored by the uv).
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


class Sheet:
    """The texture sheet of one model: a fixed width, shelf packed regions, grows downward. Regions are painted when the sheet is rendered."""

    def __init__(self, width, density):
        self.w = int(width)
        self.density = float(density)
        self.jobs = []                                                   # (x, y, w, h, kind, face, opts)
        self.keys = {}                                                   # key -> (x, y, w, h)
        self._x = self._y = self._row = 0

    def alloc(self, w, h):
        w, h = int(max(2, w)), int(max(2, h))
        if w > self.w:
            raise ValueError("a region of %d texels does not fit the sheet of %d" % (w, self.w))
        if self._x + w > self.w:
            self._y += self._row
            self._x, self._row = 0, 0
        x, y = self._x, self._y
        self._x += w
        self._row = max(self._row, h)
        return x, y

    @property
    def height(self):
        used = self._y + self._row
        return int(max(16, math.ceil(used / 16.0) * 16))

    def render(self, painter, bleed=True):
        """Paints every region with painter(kind, w, h, face, opts) -> float RGBA (h, w, 4); returns the sheet as an RGBA float array."""
        out = np.zeros((self.height, self.w, 4), dtype=np.float32)
        for x, y, w, h, kind, face, opts in self.jobs:
            a = painter(kind, w, h, face, opts)
            out[y:y + h, x:x + w] = a
        if bleed:
            out = bleed_colour(out)
        else:
            out[out[..., 3] < 0.5] = 0.0                                  # nothing under a fully transparent texel (the safe additive rule)
        return out

    def save_png(self, ident, arr):
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        img = Image.fromarray(np.clip(arr + 0.5, 0, 255).astype(np.uint8), "RGBA")
        img.save(os.path.join(ROOT, "textures", "entity", ident + ".png"), optimize=True)


def bleed_colour(a, iterations=3):
    """Spreads the colour of opaque texels into the fully transparent ones next to them (so a filtered or mip-mapped edge of a cut-out never shows a dark fringe). Alpha is untouched."""
    a = a.copy()
    solid = a[..., 3] >= 8
    for _ in range(iterations):
        acc = np.zeros(a.shape[:2] + (3,), np.float32)
        cnt = np.zeros(a.shape[:2], np.float32)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            sh = np.roll(np.roll(solid, dy, 0), dx, 1)
            col = np.roll(np.roll(a[..., :3], dy, 0), dx, 1)
            take = sh & ~solid
            acc += np.where(take[..., None], col, 0.0)
            cnt += take
        fill = cnt > 0
        a[..., :3] = np.where(fill[..., None], acc / np.maximum(cnt, 1)[..., None], a[..., :3])
        solid = solid | fill
    return a


class Model:
    """Bones and cubes in the file frame. A cube's faces name a painter (and options); the face's region of the sheet is allocated from the face's size."""

    def __init__(self, ident, sheet):
        self.id, self.sheet = ident, sheet
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

    def cube(self, bone, origin, size, faces, rot=None, pivot=None):
        """faces: {'up'|'down'|'west'|'north'|'east'|'south'|'*': kind | (kind, opts) | None}; '*' fills the faces that are not named; None skips a face. opts may carry 'key' (share the region) and 'flip' (mirror it)."""
        o = [float(v) for v in origin]
        s = [float(v) for v in size]
        c = {"origin": o, "size": s}
        if rot:
            c["rotation"] = [float(v) for v in rot]
            c["pivot"] = [float(v) for v in (pivot if pivot is not None else self.bones[bone]["pivot"])]
        fsize = {"up": (s[0], s[2]), "down": (s[0], s[2]), "west": (s[2], s[1]), "east": (s[2], s[1]), "north": (s[0], s[1]), "south": (s[0], s[1])}
        uv = {}
        self.count += 1
        d = self.sheet.density
        for f in FACE_ORDER:
            spec = faces.get(f, faces.get("*"))
            if spec is None:
                continue
            kind, opts = (spec, {}) if isinstance(spec, str) else (spec[0], dict(spec[1]) if len(spec) > 1 else {})
            opts["_d"] = d                                                  # texels per model unit: the painters scale their line widths with it
            opts["_size"] = (fsize[f][0], fsize[f][1])                      # the face in model units
            fw, fh = fsize[f]
            w, h = max(2, int(round(fw * d))), max(2, int(round(fh * d)))
            key = opts.get("key")
            flip = bool(opts.get("flip", False))
            if key is not None and (key, f if opts.get("per_face") else None) in self.sheet.keys:
                x, y, w, h = self.sheet.keys[(key, f if opts.get("per_face") else None)]
            else:
                x, y = self.sheet.alloc(w, h)
                self.sheet.jobs.append((x, y, w, h, kind, f, opts))
                if key is not None:
                    self.sheet.keys[(key, f if opts.get("per_face") else None)] = (x, y, w, h)
            if flip:                                                       # a negative uv_size mirrors the face (the loader keeps the corners in order)
                uv[BEDROCK[f]] = {"uv": [int(x + w), int(y)], "uv_size": [int(-w), int(h)]}
            else:
                uv[BEDROCK[f]] = {"uv": [int(x), int(y)], "uv_size": [int(w), int(h)]}
        c["uv"] = uv
        self.bones[bone]["cubes"].append(c)
        return c

    def quads(self):
        return sum(len(c["uv"]) for b in self.bones.values() for c in b["cubes"])

    def cubes(self):
        return sum(len(b["cubes"]) for b in self.bones.values())

    def save(self, texture_height=None):
        bones = [self.bones[n] for n in self.order]
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.id, "texture_width": int(self.sheet.w), "texture_height": int(texture_height or self.sheet.height),
                            "visible_bounds_width": 12, "visible_bounds_height": 12, "visible_bounds_offset": [0, 3, 0]}, "bones": bones}]}
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
            bones[bone] = {k: {"%.3f" % t: [round(float(x), 4) for x in v] for t, v in sorted(frames.items())} for k, frames in tracks.items()}
        out[name] = {"loop": c["loop"], "animation_length": c["length"], "bones": bones}
    os.makedirs(os.path.join(ROOT, "animations", "entity"), exist_ok=True)
    with open(os.path.join(ROOT, "animations", "entity", ident + ".animation.json"), "w", newline="\n") as f:
        json.dump({"format_version": "1.8.0", "animations": out}, f, indent=1)
        f.write("\n")


def sine(length, amp, axis=0, phase=0.0, steps=8, offset=(0, 0, 0), bias=0.0):
    """A closed sine track: {time: (x, y, z)} for `steps` keys and the closing key (the amplitude goes on `axis`)."""
    fr = {}
    for i in range(steps + 1):
        t = i / steps
        a = bias + amp * math.sin((t + phase) * math.tau)
        v = list(offset)
        v[axis] += a
        fr[t * length] = tuple(v)
    return fr


def spin(length, turns, axis=1, steps=8, offset=(0, 0, 0)):
    """A steady spin of `turns` whole turns (negative = the other way) about one axis over the clip: a linear ramp, so the loop closes without a jump."""
    fr = {}
    for i in range(steps + 1):
        t = i / steps
        v = list(offset)
        v[axis] += 360.0 * turns * t
        fr[t * length] = tuple(v)
    return fr


def const(length, vec, steps=1):
    return {i / steps * length: tuple(vec) for i in range(steps + 1)}


def sample(track, t):
    """The value of a {time: (x, y, z)} track at t, linearly interpolated between its keys (held before the first and after the last)."""
    ks = sorted(track)
    if t <= ks[0]:
        return track[ks[0]]
    if t >= ks[-1]:
        return track[ks[-1]]
    for a, b in zip(ks, ks[1:]):
        if a <= t <= b:
            k = 0.0 if b == a else (t - a) / (b - a)
            va, vb = track[a], track[b]
            return tuple(va[i] + (vb[i] - va[i]) * k for i in range(3))
    return track[ks[-1]]


def merge(*tracks):
    """Adds several {time: (x, y, z)} tracks (sums the vectors at the union of their key times, each track interpolated where it has no key)."""
    times = sorted({t for tr in tracks for t in tr})
    out = {}
    for t in times:
        s = (0.0, 0.0, 0.0)
        for tr in tracks:
            v = sample(tr, t)
            s = (s[0] + v[0], s[1] + v[1], s[2] + v[2])
        out[t] = s
    return out


def keys(times, values):
    return {float(t): tuple(v) for t, v in zip(times, values)}
