"""Why: the appendages the player model needs that the art kit's model does not give it as separate bones. The kit builds wings, tails, horns, halos, capes and hats as their own bones (the extras
extractor of the player-model kit carries those over), but a goblin's long ears, an elf's pointed ears, a fish-folk's fins, an orc's tusks, a dwarf's beard, a kobold's snout and a lich's crown of
spikes are drawn on the kit's HEAD box, which the player model replaces. They are rebuilt here as small GeoLite parts anchored on the head (`x_head`, absolute humanoid coordinates: y up, 24 = the neck
line, the face at -z, +x the entity's left), each with a small grey texture of its own (`pmx_dg_<part>.png`), so a look tints the part with the skin, bone or hair colour it needs
(`PlayerLook.Extra.tint`). The geometry is plain boxes with per-face texture rectangles, so any size works. Run through tools/gen_dungeon_skins.py (step `parts`). Python as `python3 -B`.
"""
import math
import os

import numpy as np
from PIL import Image

import common as CM
from geo_builder import Model

PARTS = {}                       # name -> builder(model, atlas)


class Atlas:
    """A 64x64 sheet filled rectangle by rectangle (a shelf packer); every cube face asks for one."""

    def __init__(self, w=64, h=64, seed=1):
        self.w, self.h, self.seed = w, h, seed
        self.img = np.zeros((h, w, 4), np.uint8)
        self.x = self.y = self.row = 0

    def rect(self, rw, rh):
        rw, rh = max(1, int(math.ceil(rw))), max(1, int(math.ceil(rh)))
        if self.x + rw > self.w:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + rh > self.h:
            raise ValueError("the atlas is full")
        at = (self.x, self.y)
        self.x += rw
        self.row = max(self.row, rh)
        return at, rw, rh

    def fill(self, at, rw, rh, c, noise=0.07, inner=None):
        for yy in range(rh):
            for xx in range(rw):
                k = 1.0 + (CM_hash(at[0] + xx, at[1] + yy, self.seed) - 0.5) * 2 * noise
                col = c
                if inner and 0 < xx < rw - 1 and 0 < yy < rh - 1:
                    col = inner
                if xx in (0, rw - 1) or yy in (0, rh - 1):
                    k *= 0.86
                self.img[at[1] + yy, at[0] + xx] = (min(255, int(col[0] * k)), min(255, int(col[1] * k)), min(255, int(col[2] * k)), 255)


def CM_hash(x, y, s):
    h = 2166136261
    for v in (x, y, s):
        h = ((h ^ (int(v) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
        h ^= h >> 15
    return (h & 0xFFFF) / 65535.0


def box(m, atlas, bone, x0, y0, z0, x1, y1, z1, tone=(150, 150, 150), inner=None, rotation=None, pivot=None):
    """A cube from (x0, y0, z0) to (x1, y1, z1) in the file frame, its six faces painted a grey tone (top lighter, bottom darker); `inner` paints the middle of the front face."""
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    faces = {}
    shade = {"up": 1.18, "down": 0.66, "north": 1.0, "south": 0.84, "east": 0.9, "west": 0.9}
    dims = {"up": (w, d), "down": (w, d), "north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h)}
    for f in ("up", "down", "north", "south", "east", "west"):
        at, rw, rh = atlas.rect(*dims[f])
        c = tuple(min(255, int(v * shade[f])) for v in tone)
        atlas.fill(at, rw, rh, c, inner=(tuple(int(v * 0.72) for v in c) if (inner and f == "north") else None))
        faces[f] = {"uv": [at[0], at[1]], "uv_size": [rw, rh]}
    m.cube(bone, (x0, y0, z0), (w, h, d), uv=faces, rotation=rotation, pivot=pivot)


def mirrored(fn):
    """Calls fn(sign) for the entity's left (+1) and right (-1) side."""
    for sg in (1, -1):
        fn(sg)


def _xs(sg, a, b):
    return (a, b) if sg > 0 else (-b, -a)


def ears(m, a, segs, tone=(158, 158, 158), inner=True):
    """Ears as contiguous boxes going out from the head's side: segs = [(x0, x1, y0, y1, z0, z1)] for the entity's left ear, mirrored for the right."""
    m.bone("x_head", None, (0, 24, 0))
    for sg in (1, -1):
        for i, (x0, x1, y0, y1, z0, z1) in enumerate(segs):
            xa, xb = _xs(sg, x0, x1)
            box(m, a, "x_head", xa, y0, z0, xb, y1, z1, tone=tone, inner=inner and i < len(segs) - 1)


def build_ears_goblin(m, a):
    ears(m, a, [(3.8, 5.8, 26.2, 29.8, -0.4, 0.7), (5.8, 7.6, 26.8, 30.4, -0.1, 0.8), (7.6, 9.2, 27.6, 30.8, 0.2, 1.0), (9.2, 10.4, 28.4, 31.0, 0.5, 1.2), (10.4, 11.3, 29.2, 31.2, 0.8, 1.4)])


def build_ears_elf(m, a):
    ears(m, a, [(3.8, 5.2, 26.8, 29.4, -0.2, 0.7), (5.2, 6.6, 27.4, 29.8, 0.0, 0.8), (6.6, 7.8, 28.0, 30.0, 0.2, 0.8), (7.8, 8.7, 28.6, 30.2, 0.4, 0.8), (8.7, 9.4, 29.2, 30.3, 0.5, 0.8)])


def build_ears_pointed(m, a):
    ears(m, a, [(3.8, 5.0, 26.6, 29.6, -0.2, 0.8), (5.0, 6.0, 27.2, 29.8, 0.0, 0.8), (6.0, 6.9, 28.0, 30.0, 0.2, 0.8)])


def build_ears_fin(m, a):
    m.bone("x_head", None, (0, 24, 0))
    for sg in (1, -1):
        xa, xb = _xs(sg, 3.8, 4.9)
        box(m, a, "x_head", xa, 25.4, -1.6, xb, 30.6, 1.6, tone=(160, 160, 160), inner=True)
        for k, (zc, top) in enumerate(((-1.0, 32.0), (0.0, 32.8), (1.0, 32.0))):
            xa, xb = _xs(sg, 4.2, 4.9)
            box(m, a, "x_head", xa, 30.6, zc - 0.3, xb, top, zc + 0.3, tone=(190, 190, 190))
        xa, xb = _xs(sg, 4.9, 5.7)
        box(m, a, "x_head", xa, 26.4, -1.2, xb, 29.4, 1.2, tone=(130, 130, 130))


def tusks(m, a, length, thick, spread):
    m.bone("x_head", None, (0, 24, 0))
    for sg in (1, -1):
        xa, xb = _xs(sg, spread - thick / 2, spread + thick / 2)
        box(m, a, "x_head", xa, 23.9, -4.9, xb, 25.0, -4.0, tone=(206, 206, 206))
        xa, xb = _xs(sg, spread - thick / 2 + 0.1, spread + thick / 2 + 0.1)
        box(m, a, "x_head", xa, 25.0, -5.1, xb, 25.0 + length * 0.55, -4.3, tone=(222, 222, 222))
        xa, xb = _xs(sg, spread - thick / 2 + 0.25, spread + thick / 2 + 0.05)
        box(m, a, "x_head", xa, 25.0 + length * 0.55, -5.3, xb, 25.0 + length, -4.7, tone=(236, 236, 236))


def build_tusks_orc(m, a):
    tusks(m, a, 3.2, 0.9, 1.9)


def build_tusks_boar(m, a):
    tusks(m, a, 2.4, 1.2, 2.2)


def build_tusks_small(m, a):
    tusks(m, a, 1.6, 0.8, 1.7)


def build_beard_dwarf(m, a):
    m.bone("x_head", None, (0, 24, 0))
    box(m, a, "x_head", -3.1, 25.0, -4.6, 3.1, 26.0, -4.0, tone=(150, 150, 150))                  # the moustache over the mouth
    box(m, a, "x_head", -3.4, 21.0, -4.7, 3.4, 25.0, -3.6, tone=(160, 160, 160))                  # the beard proper
    box(m, a, "x_head", -3.0, 17.6, -4.8, 3.0, 21.0, -3.9, tone=(146, 146, 146))
    box(m, a, "x_head", -2.0, 15.0, -4.9, 2.0, 17.6, -4.1, tone=(134, 134, 134))
    box(m, a, "x_head", -3.9, 24.0, -4.4, -3.2, 26.4, 0.6, tone=(152, 152, 152))                 # the whiskers up the cheeks
    box(m, a, "x_head", 3.2, 24.0, -4.4, 3.9, 26.4, 0.6, tone=(152, 152, 152))


def build_beard_short(m, a):
    m.bone("x_head", None, (0, 24, 0))
    box(m, a, "x_head", -3.2, 24.0, -4.5, 3.2, 25.0, -3.9, tone=(150, 150, 150))
    box(m, a, "x_head", -2.8, 22.4, -4.6, 2.8, 24.0, -4.0, tone=(142, 142, 142))
    box(m, a, "x_head", -3.9, 24.0, -4.3, -3.3, 26.0, -0.4, tone=(150, 150, 150))
    box(m, a, "x_head", 3.3, 24.0, -4.3, 3.9, 26.0, -0.4, tone=(150, 150, 150))


def build_snout_reptile(m, a):
    m.bone("x_head", None, (0, 24, 0))
    box(m, a, "x_head", -1.7, 24.6, -7.0, 1.7, 26.8, -4.0, tone=(150, 150, 150))                  # the upper snout
    box(m, a, "x_head", -1.5, 24.0, -6.4, 1.5, 24.7, -4.0, tone=(126, 126, 126))                  # the jaw
    box(m, a, "x_head", -1.0, 26.8, -6.6, -0.3, 27.4, -5.8, tone=(110, 110, 110))                 # nostrils
    box(m, a, "x_head", 0.3, 26.8, -6.6, 1.0, 27.4, -5.8, tone=(110, 110, 110))
    box(m, a, "x_head", -0.5, 27.4, -4.2, 0.5, 28.6, -3.4, tone=(140, 140, 140))                   # a brow ridge
    for k in range(4):                                                                              # the teeth
        x = -1.2 + k * 0.8
        box(m, a, "x_head", x, 24.6, -6.6 + k * 0.0, x + 0.35, 25.2, -6.2, tone=(226, 226, 226))


def build_crown_spikes(m, a):
    m.bone("x_head", None, (0, 24, 0))
    box(m, a, "x_head", -4.3, 32.0, -4.3, 4.3, 32.9, 4.3, tone=(150, 150, 150))
    for k in range(8):
        ang = k * math.pi / 4
        x, z = 3.7 * math.cos(ang), 3.7 * math.sin(ang)
        h = 2.6 if k % 2 == 0 else 1.7
        box(m, a, "x_head", x - 0.45, 32.9, z - 0.45, x + 0.45, 32.9 + h, z + 0.45, tone=(190, 190, 190))
    box(m, a, "x_head", -0.6, 32.9, -4.5, 0.6, 35.2, -3.7, tone=(214, 214, 214))              # the tall front spike
    box(m, a, "x_head", -0.6, 32.9, -4.5, 0.6, 33.7, -3.7, tone=(240, 80, 80))                # its gem


def build_horns_helm(m, a):
    m.bone("x_head", None, (0, 24, 0))
    for sg in (1, -1):
        xa, xb = _xs(sg, 3.9, 5.0)
        box(m, a, "x_head", xa, 30.0, -1.0, xb, 31.4, 1.0, tone=(150, 150, 150))
        xa, xb = _xs(sg, 4.6, 5.8)
        box(m, a, "x_head", xa, 31.2, -0.8, xb, 33.4, 0.8, tone=(190, 190, 190))
        xa, xb = _xs(sg, 5.2, 6.2)
        box(m, a, "x_head", xa, 33.2, -0.6, xb, 35.0, 0.6, tone=(225, 225, 225))


BUILDERS = {"ears_goblin": build_ears_goblin, "ears_elf": build_ears_elf, "ears_pointed": build_ears_pointed, "ears_fin": build_ears_fin, "tusks_orc": build_tusks_orc,
            "tusks_boar": build_tusks_boar, "tusks_small": build_tusks_small, "beard_dwarf": build_beard_dwarf, "beard_short": build_beard_short,
            "snout_reptile": build_snout_reptile, "crown_spikes": build_crown_spikes, "horns_helm": build_horns_helm}


def model_name(part):
    return "pmx_dg_" + part


def write_all():
    """Writes pmx_dg_<part>.geo.json and pmx_dg_<part>.png for every part. Returns the part names."""
    names = []
    for name, fn in BUILDERS.items():
        m = Model("pmx", "dg_" + name, tex_w=64, tex_h=64)
        a = Atlas(seed=CM.seed_of(name))
        fn(m, a)
        os.makedirs(CM.GEO, exist_ok=True)
        import json
        with open(os.path.join(CM.GEO, m.id + ".geo.json"), "w") as f:
            json.dump(m.geo(), f, separators=(",", ":"))
        Image.fromarray(a.img, "RGBA").save(os.path.join(CM.TEX, m.id + ".png"), optimize=True)
        names.append(name)
    return names


# which part a kit ears token / an archetype token asks for, and the colour it is tinted with ('skin' = the skin's mid tone, 'bone', 'hair')
def parts_for(tokens, faction=""):
    """[(part name, tint source)] for the head appendages a row's tokens ask for."""
    out = []
    ears = (tokens.get("ears") or "").split("/")[0]
    if "bigears" in tokens or (faction == "goblin" and ears in ("pointed", "elf")):
        out.append(("ears_goblin", "skin"))
    elif ears == "elf":
        out.append(("ears_elf", "skin"))
    elif ears == "pointed":
        out.append(("ears_pointed", "skin"))
    elif ears == "fin":
        out.append(("ears_fin", "skin"))
    t = (tokens.get("tusks") or "").split("/")[0]
    if t:
        out.append(({"orc": "tusks_orc", "boar": "tusks_boar", "small": "tusks_small"}.get(t, "tusks_small"), "bone"))
    b = (tokens.get("beard") or "").split("/")[0]
    if b:
        out.append(({"dwarf": "beard_dwarf", "short": "beard_short"}.get(b, "beard_short"), "hair"))
    if "snout" in tokens:
        out.append(("snout_reptile", "skin"))
    if "crown3d" in tokens:
        out.append(("crown_spikes", "gold"))
    return out
