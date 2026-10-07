"""Item models + textures for Painting Magic's palette & brush (0.44). Deterministic; writes:

    textures/item/paint_studio.png        wood (top + edge), lacquered handle, silver ferrule, bristles
    textures/item/paint_pools.png(.mcmeta) the wet paints, animated: 7 colour cells per frame, a gloss and ripple running over them
    models/item/paint_palette[_<paint>].json   a 3D wooden thumb-hole palette (voxel runs of the kidney outline) with 7 raised,
                                               full-bright pools of paint; the selected paint's pool is bigger and wetter
    models/item/paint_brush[_<paint>].json     a tapered brush laid diagonally like a tool sprite, its tip loaded with the paint

The base model is the INK variant; custom model data (paint ordinal + 1) picks the others (PaintStudio keeps it in sync).

    python tools/gen_paint_items.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(ROOT, "textures", "item")
MOD = os.path.join(ROOT, "models", "item")
PAINTS = [("ink", (0x3A, 0x7B, 0xFF)), ("fire", (0xFF, 0x5A, 0x3A)), ("water", (0x4A, 0xA8, 0xFF)), ("ice", (0x8A, 0xE6, 0xFF)),
          ("wind", (0x7C, 0xF0, 0xB0)), ("earth", (0xB0, 0x86, 0x4A)), ("lightning", (0xFF, 0xE6, 0x5A))]
GLOW = {"block_light": 15, "sky_light": 15}


def shade(rgb, k):
    return tuple(int(max(0, min(255, c * k + (255 - c) * max(0, k - 1)))) for c in rgb)


# ------------------------------------------------------------------------------------------------ textures
def studio_texture():
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):                                    # wood top (u 0..16 px): warm maple with grain
        for x in range(16):
            g = math.sin((x * 0.9 + y * 0.25) * 0.8 + math.sin(y * 0.7) * 1.5) * 0.5 + 0.5
            px[x, y] = shade((0xC8, 0x92, 0x5A), 0.88 + 0.18 * g) + (255,)
    for y in range(8):                                     # wood edge (u 16..24, v 0..8): darker end grain
        for x in range(16, 24):
            px[x, y] = shade((0x9A, 0x66, 0x38), 0.9 + 0.1 * ((x + y) % 3 == 0)) + (255,)
    for y in range(8, 16):                                 # lacquered handle (u 16..20) with a highlight line
        for x in range(16, 20):
            px[x, y] = ((0xE8, 0x9A, 0x8A) if x == 17 else (0x7A, 0x1E, 0x24)) + (255,)
    for y in range(8, 16):                                 # silver ferrule (u 20..24)
        for x in range(20, 24):
            px[x, y] = ((0xF4, 0xF4, 0xFA) if x == 21 else (0xA8, 0xAC, 0xBC)) + (255,)
    for y in range(16):                                    # bristles (u 24..32): pale tan with strands
        for x in range(24, 32):
            px[x, y] = shade((0xE6, 0xD2, 0xA8), 0.85 + 0.15 * (x % 2)) + (255,)
    im.save(os.path.join(TEX, "paint_studio.png"))


def pools_texture(frames=8):
    """7 paint cells (8x8 px, 4 per row) per 32x32 frame, frames stacked; a gloss crescent and a ripple ring move over them."""
    strip = Image.new("RGBA", (32, 32 * frames), (0, 0, 0, 0))
    for f in range(frames):
        ph = f / frames
        for k, (_, rgb) in enumerate(PAINTS):
            cx, cy = (k % 4) * 8, f * 32 + (k // 4) * 8
            for y in range(8):
                for x in range(8):
                    u, v = (x + 0.5) / 8 - 0.5, (y + 0.5) / 8 - 0.5
                    r = math.hypot(u, v)
                    ring = max(0.0, 1 - abs(r - (0.05 + 0.4 * ((ph + k * 0.13) % 1))) / 0.07)
                    hl = math.exp(-(((u + 0.15 - 0.08 * math.sin(ph * 6.283)) / 0.12) ** 2 + ((v + 0.18) / 0.08) ** 2))
                    k2 = 0.82 + 0.2 * ring + 0.55 * hl - 0.25 * max(0.0, r - 0.35) * 2
                    strip.putpixel((cx + x, cy + y), shade(rgb, k2) + (255,))
    strip.save(os.path.join(TEX, "paint_pools.png"))
    with open(os.path.join(TEX, "paint_pools.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, fh, indent=2)


# ------------------------------------------------------------------------------------------------ models
def face_all(tex, uv, cull_dir=None, emissive=False):
    f = {d: {"texture": tex, "uv": uv} for d in ("north", "south", "east", "west", "up", "down")}
    return f


def pool_uv(k):
    """UV (in 0..16 units of the 32 px pools texture) of paint k's cell."""
    u, v = (k % 4) * 4, (k // 4) * 4
    return [u + 0.5, v + 0.5, u + 3.5, v + 3.5]


def palette_mask():
    """The kidney board with a thumb hole and finger notch at 16x16 (row 0 = top)."""
    s, ss = 16, 8
    im = Image.new("L", (s * ss, s * ss), 0)
    d = ImageDraw.Draw(im)
    S = s * ss
    d.ellipse((S * 0.0, S * 0.1, S * 1.0, S * 0.92), fill=255)
    d.ellipse((S * -0.12, S * 0.62, S * 0.22, S * 1.06), fill=0)
    d.ellipse((S * 0.2, S * 0.38, S * 0.38, S * 0.56), fill=0)
    return np.asarray(im.resize((s, s), Image.BOX)) > 110


POOL_SLOTS = [(5, 3.0), (8.5, 2.5), (12, 3.5), (13.5, 7), (12, 10.5), (8.5, 11), (5.5, 12.5)]   # (x, y) top-left in 16-grid rows


def palette_model(sel):
    m = palette_mask()
    els = []
    for row in range(16):                                  # one element per horizontal run of wood
        x = 0
        while x < 16:
            if not m[row, x]:
                x += 1
                continue
            x0 = x
            while x < 16 and m[row, x]:
                x += 1
            y0 = 15 - row
            uvf = [x0 / 2, row / 2, x / 2, (row + 1) / 2]
            els.append({"from": [x0, y0, 7.5], "to": [x, y0 + 1, 8.5],
                        "faces": {"north": {"texture": "#wood", "uv": [8 - x / 2, row / 2, 8 - x0 / 2, (row + 1) / 2]},
                                  "south": {"texture": "#wood", "uv": uvf},
                                  "up": {"texture": "#edge", "uv": [8, 0, 12, 0.5]}, "down": {"texture": "#edge", "uv": [8, 0, 12, 0.5]},
                                  "east": {"texture": "#edge", "uv": [8, 0, 8.5, 0.5]}, "west": {"texture": "#edge", "uv": [8, 0, 8.5, 0.5]}}})
    for k, (px, py) in enumerate(POOL_SLOTS):              # pools of paint on the front face, full-bright
        big = k == sel
        w = 2.6 if big else 1.8
        cx, cy = px, 15.5 - py
        depth = 9.4 if big else 8.9
        uv = pool_uv(k)
        els.append({"from": [cx - w / 2, cy - w / 2, 8.5], "to": [cx + w / 2, cy + w / 2, depth], "shade": False,
                    "neoforge_data": GLOW,
                    "faces": {d: {"texture": "#paint", "uv": uv} for d in ("south", "east", "west", "up", "down")}})
    return {
        "gui_light": "front",
        "textures": {"particle": "nusmp:item/paint_studio", "wood": "nusmp:item/paint_studio", "edge": "nusmp:item/paint_studio",
                     "paint": "nusmp:item/paint_pools"},
        "elements": els,
        "display": {
            "thirdperson_righthand": {"rotation": [-25, 0, 0], "translation": [0, 3, 1.5], "scale": [0.62, 0.62, 0.62]},
            "thirdperson_lefthand": {"rotation": [-25, 0, 0], "translation": [0, 3, 1.5], "scale": [0.62, 0.62, 0.62]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7]},
            "fixed": {"rotation": [0, 180, 0]},
        },
    }


def brush_model(sel):
    rot = {"origin": [8, 8, 8], "axis": "z", "angle": -45, "rescale": True}

    def box(fr, to, tex, uv, glow=False):
        e = {"from": fr, "to": to, "rotation": rot, "faces": {d: {"texture": tex, "uv": uv} for d in ("north", "south", "east", "west", "up", "down")}}
        if glow:
            e["shade"] = False
            e["neoforge_data"] = GLOW
        return e

    els = [
        box([7.5, 0, 7.5], [8.5, 9.5, 8.5], "#wood", [8, 4, 10, 8]),                    # lacquered handle
        box([7.6, -0.4, 7.6], [8.4, 0.2, 8.4], "#wood", [10, 4, 12, 5]),                # silver end cap
        box([7.2, 9.5, 7.2], [8.8, 11.5, 8.8], "#wood", [10, 4, 12, 8]),                # silver ferrule
        box([7.1, 11.5, 7.1], [8.9, 13, 8.9], "#wood", [12, 0, 16, 4]),                 # bristles
        box([7.25, 13, 7.25], [8.75, 14.6, 8.75], "#paint", pool_uv(sel), glow=True),   # loaded with paint
        box([7.6, 14.6, 7.6], [8.4, 15.6, 8.4], "#paint", pool_uv(sel), glow=True),     # the wet point
    ]
    return {
        "gui_light": "front",
        "textures": {"particle": "nusmp:item/paint_studio", "wood": "nusmp:item/paint_studio", "paint": "nusmp:item/paint_pools"},
        "elements": els,
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0]},
        },
    }


def write(name, model):
    with open(os.path.join(MOD, name + ".json"), "w") as fh:
        json.dump(model, fh, indent=2)


def main():
    os.makedirs(TEX, exist_ok=True)
    studio_texture()
    pools_texture()
    for base, fn in (("paint_palette", palette_model), ("paint_brush", brush_model)):
        root = fn(0)
        root["overrides"] = [{"predicate": {"custom_model_data": k + 1}, "model": f"nusmp:item/{base}_{PAINTS[k][0]}"} for k in range(1, len(PAINTS))]
        write(base, root)
        for k in range(1, len(PAINTS)):
            write(f"{base}_{PAINTS[k][0]}", fn(k))
    print("wrote palette & brush models")


if __name__ == "__main__":
    main()
