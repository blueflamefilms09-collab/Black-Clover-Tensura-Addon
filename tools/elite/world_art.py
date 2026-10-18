"""tools/elite/world_art.py: the small painting kit of the elite world assets (shared by gen_world_models.py and gen_world_blocks.py).

Why: the Gravebound Colossus, the Lich, the phylactery, the Gaia spike and the void bolt are GeoLite models (client/geo, built with tools/geo_builder.py), not arc-kit characters:
the arc kit paints named humanoid characters (faces, hair, outfits), and these are a golem of grave soil, a robed skeleton and pieces of earth. What they need is what the kit
does for its own bodies: a texture atlas whose every cube gets a material (a hard-edged ramp of colours with value noise, darker edges, a few cracks) and a glow layer for the parts
that burn (eyes, runes, cores). This module is that: an atlas packer (box UV like a vanilla model), the material ramps, the face painter and a glow painter. Pure python with Pillow; the
output is deterministic (hashed noise, no random module), so regenerating a model never changes a pixel unless the model changed.
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))

from geo_builder import Model, Anim  # noqa: E402,F401
from PIL import Image  # noqa: E402

RAMPS = {
    "soil": [(48, 36, 26), (70, 54, 38), (96, 76, 54), (124, 100, 72)],
    "stone": [(44, 46, 52), (68, 70, 78), (96, 98, 108), (130, 132, 142)],
    "bone": [(104, 94, 76), (160, 148, 122), (204, 192, 164), (238, 230, 206)],
    "cloth": [(22, 12, 36), (38, 22, 60), (58, 36, 90), (84, 56, 122)],
    "trim": [(70, 52, 20), (120, 90, 34), (176, 140, 58), (226, 190, 96)],
    "crystal": [(36, 14, 76), (72, 34, 138), (122, 70, 198), (196, 148, 255)],
    "dark": [(10, 8, 14), (20, 16, 26), (32, 26, 42), (46, 40, 60)],
    "rune": [(16, 8, 8), (26, 12, 10), (38, 20, 16), (54, 30, 24)],
    "moss": [(30, 44, 28), (48, 70, 40), (70, 98, 54), (100, 130, 74)],
}


def noise(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFF) / 255.0


def ramp_color(name, t):
    r = RAMPS[name]
    t = max(0.0, min(0.9999, t)) * (len(r) - 1)
    i = int(t)
    f = t - i
    a, b = r[i], r[i + 1]
    # hard edged: snap the blend to quarters so the look stays chunky
    f = round(f * 3) / 3.0
    return tuple(int(a[k] + (b[k] - a[k]) * f) for k in range(3))


class Atlas:
    """Packs cubes (box UV) into a texture and remembers what to paint on each."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.x = self.y = self.rowh = 0
        self.items = []

    def alloc(self, size, mat, glow=None, seed=0, light=None):
        sx, sy, sz = size
        w, h = 2 * (sx + sz), sz + sy
        if self.x + w > self.w:
            self.x, self.y, self.rowh = 0, self.y + self.rowh, 0
        if self.y + h > self.h or w > self.w:
            raise ValueError("atlas %dx%d is full at cube %s" % (self.w, self.h, size))
        uv = (self.x, self.y)
        self.x += w
        self.rowh = max(self.rowh, h)
        self.items.append({"uv": uv, "size": size, "mat": mat, "glow": glow, "seed": seed + len(self.items) * 7, "light": light})
        return uv

    def faces(self, it):
        """The six faces of a box UV cube as (name, x, y, w, h)."""
        u, v = it["uv"]
        sx, sy, sz = it["size"]
        return [("up", u + sz, v, sx, sz), ("down", u + sz + sx, v, sx, sz), ("west", u, v + sz, sz, sy), ("front", u + sz, v + sz, sx, sy),
                ("east", u + sz + sx, v + sz, sz, sy), ("back", u + 2 * sz + sx, v + sz, sx, sy)]

    def paint(self, cracks=True):
        img = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        glow = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        px, gx = img.load(), glow.load()
        for it in self.items:
            mat, seed = it["mat"], it["seed"]
            light = {"up": 1.0, "down": 0.25, "west": 0.55, "east": 0.55, "front": 0.75, "back": 0.45}
            if it["light"]:
                light.update(it["light"])
            for name, x0, y0, w, h in self.faces(it):
                for y in range(h):
                    for x in range(w):
                        n = noise(x0 + x, y0 + y, seed)
                        t = light[name] * 0.78 + (n - 0.5) * 0.5 + 0.08
                        edge = x == 0 or y == 0 or x == w - 1 or y == h - 1
                        if edge and w > 3 and h > 3:
                            t -= 0.16
                        if cracks and mat in ("soil", "stone", "bone") and noise((x0 + x) // 2, (y0 + y) // 3, seed + 99) > 0.93:
                            t -= 0.34
                        c = ramp_color(mat, t)
                        px[x0 + x, y0 + y] = c + (255,)
                if it["glow"]:
                    gc = it["glow"]
                    for y in range(h):
                        for x in range(w):
                            k = 0.78 + 0.22 * noise(x0 + x, y0 + y, seed + 5)
                            edge = (x == 0 or y == 0 or x == w - 1 or y == h - 1) and w > 3 and h > 3
                            kk = k * (0.7 if edge else 1.0)
                            gx[x0 + x, y0 + y] = (int(gc[0] * kk), int(gc[1] * kk), int(gc[2] * kk), 255)
        return img, glow


def box(model, atlas, bone, origin, size, mat, glow=None, rotation=None, pivot=None, inflate=0.0, seed=0, light=None):
    """Adds a cube with a fresh atlas rectangle. size must be integers (box UV)."""
    size = tuple(int(s) for s in size)
    uv = atlas.alloc(size, mat, glow, seed, light)
    return model.cube(bone, origin, size, uv=uv, inflate=inflate, rotation=rotation, pivot=pivot)


def draw_on_face(atlas, img, item_index, face, painter):
    """Calls painter(px, x0, y0, w, h) for one face of the item_index-th cube (to paint eyes, runes, teeth)."""
    it = atlas.items[item_index]
    for name, x0, y0, w, h in atlas.faces(it):
        if name == face:
            painter(img.load(), x0, y0, w, h)
            return
    raise KeyError(face)
