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
            if not isinstance(spec, str) and len(spec) > 3 and spec[3] is not None:       # a named window of the region: (u0, v0, u1, v1) as fractions of its size, v0 at the top (a continuous picture cut over several boxes)
                rx_, ry_, rw_, rh_ = self.atlas.regions[region]
                u0, v0, u1, v1 = spec[3]
                xy, wh = [rx_ + u0 * rw_, ry_ + v0 * rh_], [(u1 - u0) * rw_, (v1 - v0) * rh_]
            else:
                xy, wh = self.uv_of(region, fw, fh, density, self.count)
            if flip:                                                           # a negative uv_size mirrors the face (the loader keeps the corners in order)
                xy = [xy[0] + wh[0], xy[1]]
                wh = [-wh[0], wh[1]]
            uv[BEDROCK[f]] = {"uv": [int(v) for v in xy], "uv_size": [int(v) for v in wh]}
        c["uv"] = uv
        self.bones[bone]["cubes"].append(c)
        return c


    def box(self, bone, center, size, R, faces, d=3.0):
        """A box of `size` (local x, y, z) centred on `center`, turned by the rotation matrix R about its own centre. faces as in cube()."""
        c = np.asarray(center, dtype=np.float64)
        s = np.asarray(size, dtype=np.float64)
        o = c - s / 2.0
        rx, ry, rz = euler_of(R)
        if abs(rx) + abs(ry) + abs(rz) < 1e-6:
            return self.cube(bone, o, s, faces, None, None, d)
        return self.cube(bone, o, s, faces, (rx, ry, rz), c, d)

    def limb(self, bone, p0, p1, w, h, faces, up=(0, 1, 0), pad=0.0, roll=0.0, d=3.0):
        """A box laid from p0 to p1 (local z along the segment, `pad` longer in total), w wide and h high; overlapping neighbours hide the joints."""
        p0, p1 = np.asarray(p0, dtype=np.float64), np.asarray(p1, dtype=np.float64)
        n = float(np.linalg.norm(p1 - p0))
        return self.box(bone, (p0 + p1) / 2.0, (w, h, n + pad), frame(p1 - p0, up, roll), faces, d)

    def card(self, bone, center, right, up, w, h, region, face="north", d=0, thick=0.0):
        """A flat picture: one quad (nothing is culled, so both sides are seen), the region of the sheet mapped over w x h, the picture's right and up given in the file frame. thick > 0 makes a slab (then the two big faces)."""
        R = card_frame(right, up)
        faces = {"north": region} if thick <= 0 else {"north": region, "south": region}
        if thick <= 0:
            faces = {"north": region}
        for f in list(faces):
            pass
        return self.box(bone, center, (w, h, thick), R, faces, d)

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


# ====================================================================================================================== frames, limbs and cards (the silhouette helpers)
def vec(*a):
    return np.array(a, dtype=np.float64)


def unit(v):
    v = np.asarray(v, dtype=np.float64)
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else vec(0, 0, 1)


def rot_matrix(rx, ry, rz):
    """The file's cube rotation as a matrix: the point is turned about x first, then y, then z (degrees); this is how GeoDraw turns a bone or a cube."""
    ax, ay, az = math.radians(rx), math.radians(ry), math.radians(rz)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    RX = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    RY = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    RZ = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return RZ @ RY @ RX


def euler_of(R):
    """The (rx, ry, rz) degrees whose rot_matrix is R (a proper rotation)."""
    sy = -R[2, 0]
    sy = max(-1.0, min(1.0, sy))
    ry = math.asin(sy)
    if abs(sy) < 0.99999:
        rx = math.atan2(R[2, 1], R[2, 2])
        rz = math.atan2(R[1, 0], R[0, 0])
    else:                                                                  # gimbal lock: all the turn goes to z
        rx = 0.0
        rz = math.atan2(-R[0, 1], R[1, 1])
    return tuple(math.degrees(v) for v in (rx, ry, rz))


def frame(z_axis, up=(0, 1, 0), roll=0.0):
    """The rotation matrix whose local z is `z_axis` and whose local y is as near `up` as possible (then turned by `roll` degrees about z): columns are the local x, y, z in the file frame."""
    z = unit(z_axis)
    u = vec(*up)
    if abs(float(np.dot(unit(u), z))) > 0.98:
        u = vec(1, 0, 0)
    y = unit(u - z * float(np.dot(u, z)))
    x = np.cross(y, z)
    if roll:
        c, s = math.cos(math.radians(roll)), math.sin(math.radians(roll))
        x, y = x * c + y * s, y * c - x * s
    return np.stack([x, y, z], axis=1)


def card_frame(right, up):
    """The frame of a flat card: local x = the picture's right, local y = the picture's up; the picture reads correctly for a viewer on the local -z side."""
    x = unit(right)
    u = vec(*up)
    y = unit(u - x * float(np.dot(u, x)))
    z = np.cross(x, y)
    return np.stack([x, y, z], axis=1)


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
