#!/usr/bin/env python3
"""Why: the Shadow Core (nusmp:shadow_core, the System's second reward) needs, by the owner's rule, a real 3D model in hand with its 16x16 sprite kept as the inventory
icon. It is a trinket and not a weapon, so it follows the Genesis Core of 1.51.0 (tools/gen_avatar_assets.py) rather than the weapon mesh pipeline: a vanilla elements
model, switched by "neoforge:separate_transforms" (the sprite in the GUI, the 3D model everywhere else), which needs no renderer class and no registration line in a
shared file. The model is a small dark crystal, stepped like a bipyramid and turned a quarter so its faces catch the light, with a bright violet heart band at the
equator, four shards that orbit it and a pale plate below it like a hovering shadow. The heart, the shards and the plate are full-bright (neoforge_data light 15, no
shading), the crystal itself is lit like any block. Deterministic: running it again writes the same bytes.

Writes (under src/main/resources/assets/nusmp):
  textures/item/shadow_core.png          16x16 inventory icon
  textures/item/shadow_core_model.png    32x32 texture of the model: four 8-pixel bands (dark facet, light edge, glowing heart, pale shard)
  models/item/shadow_core.json           neoforge:separate_transforms: the sprite in the GUI, the 3D model elsewhere
  models/item/shadow_core_icon.json      the flat sprite model
  models/item/shadow_core_3d.json        the elements model
Run: python tools/system/gen_shadow_core_assets.py   (then: python tools/system/preview_item_model.py to look at it)
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "nusmp"))


def p(*a):
    return os.path.join(ROOT, *a)


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def icon():
    """A faceted dark crystal with a violet heart and four sparks; left-top facet catches the light."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy, rx, ry = 7.5, 7.5, 4.6, 7.0
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = abs(dx) / rx + abs(dy) / ry
            if d > 1.0:
                continue
            if dx < 0 and dy < 0:
                c = (74, 58, 138, 255)
            elif dx >= 0 and dy < 0:
                c = (50, 38, 104, 255)
            elif dx < 0:
                c = (40, 30, 86, 255)
            else:
                c = (26, 19, 62, 255)
            if d > 0.80:
                c = lerp(c, (12, 8, 32, 255), (d - 0.80) / 0.20)             # a dark rim
            if abs(dx) < 0.6 and dy < -1:
                c = lerp(c, (150, 130, 235, 255), 0.55)                       # the ridge up the middle
            h = math.hypot(dx * 1.15, dy)
            if h < 3.2:                                                       # the heart
                c = lerp((255, 255, 255, 255), (150, 90, 255, 255), h / 3.2)
            elif h < 4.3:
                c = lerp(c, (130, 70, 230, 255), (4.3 - h) / 1.1 * 0.7)
            im.putpixel((x, y), c)
    for (x, y, c) in [(2, 4, (210, 190, 255, 255)), (13, 11, (190, 160, 255, 235)), (12, 3, (230, 215, 255, 255)), (3, 12, (170, 140, 245, 220)), (1, 8, (150, 120, 235, 160)), (14, 7, (150, 120, 235, 160))]:
        im.putpixel((x, y), c)
    im.save(p("textures", "item", "shadow_core.png"))


def model_texture():
    """32x32: rows 0-7 the dark facet, 8-15 the light edge, 16-23 the glowing heart, 24-31 the pale shard. Each band is used by a whole face, so it is mostly a gradient."""
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            band, ry = divmod(y, 8)
            if band == 0:                                   # dark facet: indigo with fine lighter veins
                base = lerp((30, 22, 66, 255), (62, 46, 118, 255), (x / 31.0 * 0.5 + ry / 7.0 * 0.5))
                if (x * 3 + ry * 5) % 11 == 0:
                    base = lerp(base, (120, 100, 210, 255), 0.45)
                c = base
            elif band == 1:                                 # light edge: a facet glint
                c = lerp((88, 70, 168, 255), (170, 150, 245, 255), 1.0 - abs(x - 15.5) / 15.5)
            elif band == 2:                                 # the heart: white in the middle of the face, violet at its sides
                c = lerp((255, 255, 255, 255), (150, 84, 255, 255), abs(x - 15.5) / 15.5 * 1.1)
            else:                                           # the shard / plate: pale lavender fading to violet
                c = lerp((226, 212, 255, 255), (150, 110, 240, 255), ry / 7.0 * 0.6 + abs(x - 15.5) / 15.5 * 0.4)
            im.putpixel((x, y), c)
    im.save(p("textures", "item", "shadow_core_model.png"))


# bands, as uv rows of the 16-unit face space (the 32 px texture is addressed as 0..16)
DARK, EDGE, HEART, SHARD = (0, 4), (4, 8), (8, 12), (12, 16)


def box(frm, to, band, top_band=None, glow=0, angle=0.0, axis="y", origin=(8, 8, 8)):
    """A cuboid whose faces use one band (its top face may use another). Glowing ones are full-bright and unshaded."""
    faces = {}
    for k in ("north", "east", "south", "west", "down"):
        faces[k] = {"uv": [0, band[0], 16, band[1]], "texture": "#t"}
    tb = top_band or band
    faces["up"] = {"uv": [0, tb[0], 16, tb[1]], "texture": "#t"}
    if glow:
        for f in faces.values():
            f["shade"] = False
    el = {"from": list(frm), "to": list(to), "faces": faces}
    if angle:
        el["rotation"] = {"angle": angle, "axis": axis, "origin": list(origin)}
    if glow:
        el["neoforge_data"] = {"block_light": glow, "sky_light": glow}
    return el


def layer(y0, y1, half, band=DARK, top=EDGE, glow=0):
    return box([8 - half, y0, 8 - half], [8 + half, y1, 8 + half], band, top, glow, 45.0)


def models():
    # the crystal: a stepped bipyramid, 8 layers, a quarter turn about its axis; heart band at the equator
    steps = [(1.0, 2.0, 0.75), (2.0, 3.5, 1.5), (3.5, 5.5, 2.25), (5.5, 7.25, 3.0), (8.75, 10.5, 3.0), (10.5, 12.5, 2.25), (12.5, 14.0, 1.5), (14.0, 15.0, 0.75)]
    elements = [layer(a, b, h) for (a, b, h) in steps]
    elements.append(box([8 - 3.4, 7.25, 8 - 3.4], [8 + 3.4, 8.75, 8 + 3.4], HEART, HEART, 15, 45.0))        # the heart band, a little wider than the layers beside it
    # four shards that orbit it, small diamonds turned a quarter, two high and two low so they read as circling
    for (cx, cz, y0) in [(8 + 5.2, 8, 8.0), (8 - 5.2, 8, 5.0), (8, 8 + 5.2, 5.0), (8, 8 - 5.2, 8.0)]:
        elements.append(box([cx - 0.5, y0, cz - 0.5], [cx + 0.5, y0 + 3.0, cz + 0.5], SHARD, SHARD, 15, 45.0, "y", (cx, 8, cz)))
    # the hovering shadow: a dark disc under it with a violet rim of light showing round its edge
    elements.append(box([8 - 4.8, 0.0, 8 - 4.8], [8 + 4.8, 0.2, 8 + 4.8], HEART, HEART, 15, 45.0))
    elements.append(box([8 - 4.2, 0.2, 8 - 4.2], [8 + 4.2, 0.5, 8 + 4.2], DARK, DARK, 0, 45.0))
    display = {
        "gui": {"rotation": [25, 35, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 12, 0], "scale": [0.8, 0.8, 0.8]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 2], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 2], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [10, -25, 0], "translation": [1.5, 3.5, -2.5], "scale": [0.7, 0.7, 0.7]},
        "firstperson_lefthand": {"rotation": [10, 25, 0], "translation": [-1.5, 3.5, -2.5], "scale": [0.7, 0.7, 0.7]},
    }
    three_d = {"_comment": "Shadow Core held model, generated by tools/system/gen_shadow_core_assets.py (rule 7: real 3D in hand, sprite as the inventory icon).",
               "textures": {"t": "nusmp:item/shadow_core_model", "particle": "nusmp:item/shadow_core"}, "elements": elements, "display": display}
    with open(p("models", "item", "shadow_core_3d.json"), "w") as f:
        json.dump(three_d, f, indent=2)
        f.write("\n")
    with open(p("models", "item", "shadow_core_icon.json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/shadow_core"}}, f, indent=2)
        f.write("\n")
    root = {"loader": "neoforge:separate_transforms", "base": {"parent": "nusmp:item/shadow_core_3d"}, "perspectives": {"gui": {"parent": "nusmp:item/shadow_core_icon"}}}
    with open(p("models", "item", "shadow_core.json"), "w") as f:
        json.dump(root, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    for d in (("textures", "item"), ("models", "item")):
        os.makedirs(p(*d), exist_ok=True)
    icon()
    model_texture()
    models()
    print("wrote the Shadow Core assets")
