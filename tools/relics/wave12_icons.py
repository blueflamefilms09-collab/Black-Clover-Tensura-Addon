#!/usr/bin/env python3
"""
Relics of Many Worlds, second set, wave 12: the 16x16 inventory icons of the ten hand-held weapons, drawn to the mod's look (a dark outline, light on the
top-left edges, shade on the bottom-right ones, a few bright details and glints) with a rim pixel in the colour of the Tensura grade, plus the item models:

    python3 tools/relics/wave12_icons.py            # writes the icons and the models
    python3 tools/relics/wave12_icons.py --sheet P  # also writes a contact sheet (scaled 12x) to the path P

Every weapon is drawn on the sprite diagonal (grip at the bottom left, point at the top right): wave12_models.py places its 3D mesh on this diagonal
(mesh_lib.Mesh.place_on_sprite), so the old display transforms fit. Per id it writes
  textures/item/<id>.png            the 16 x 16 sprite (the inventory icon)
  models/item/<id>.json             parent builtin/entity (the 3D mesh is drawn by client/WeaponRenderer), display copied from the Rainbow Rapier
  models/item/<id>_icon.json        parent item/handheld with the sprite (the inventory icon)
Idempotent: running it again rewrites the same files.
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import wave2_icons as w2                                                  # noqa: E402  (the shared Icon class: bevel, outline, rim, glints)
from wave2_icons import Icon, rgb, light, dark, mix                       # noqa: E402

ASSETS = w2.ASSETS
TEX = w2.TEX
MODELS = w2.MODELS


# ---------------------------------------------------------------- the diagonal: i runs from the grip (0) to the point (14), o across it
def at(i, o):
    return (1 + i + o, 14 - i + o)


def put(ic, i, o, c, a=255):
    """One cell of the diagonal: two pixels side by side, so a band steps like a pixel-art sword and its edges are not all corners."""
    x, y = at(i, o)
    ic.px(x, y, c, a)
    ic.px(x + 1, y, c, a)


def span(ic, i0, i1, o0, o1, c, a=255):
    for i in range(i0, i1 + 1):
        for o in range(o0, o1 + 1):
            put(ic, i, o, c, a)


def blade(ic, i0, i1, o0, o1, c, tip=True):
    """A blade: a band that narrows to a point over its last cells."""
    for i in range(i0, i1 + 1):
        left = i1 - i
        a, b = o0, o1
        if tip and left < 2:
            a, b = (o0 + (1 - left), o1 - (1 - left)) if o1 - o0 >= 2 else (o0, o1)
        if tip and left == 0:
            a = b = (o0 + o1) // 2
        for o in range(a, b + 1):
            put(ic, i, o, c)


# ====================================================================== the ten icons
def giant_jawblade():
    ic = Icon()
    bone, iron, grip = rgb(0xE0BE78), rgb(0x4B433E), rgb(0x3A2E2A)
    span(ic, 0, 0, 0, 0, iron)                                                  # round pommel
    span(ic, 1, 3, 0, 0, grip)                                                  # thin dark grip
    span(ic, 3, 5, -1, 0, iron)                                                 # the iron-plated base
    blade(ic, 5, 14, -2, 0, bone)                                               # the slab of jawbone
    span(ic, 8, 13, 1, 1, light(bone, 0.1))
    img = ic.finish("RARE")
    for i in (7, 9, 11):                                                        # tooth spikes along the lower edge
        put(ic, i, 2, rgb(0xF4E6C0))
    put(ic, 4, 0, rgb(0x6FB8E8))                                                # the rune disc
    for i in (7, 10, 12):                                                       # cracks and pits in the bone
        put(ic, i, -1, rgb(0xB8944E))
    put(ic, 9, -2, rgb(0xF6E2A8))
    ic.glints([(14, 0), (15, 3)], rgb(0xFFF0C0), 150)
    return img


def the_lumberjack():
    ic = Icon()
    wood, wrap, steel = rgb(0x8A5A2E), rgb(0x6A6C72), rgb(0xC4CAD0)
    span(ic, 0, 11, 0, 0, wood)
    ic.rect(7, 1, 13, 3, steel)                                                 # the head: a broad plate on the top, a bearded blade hanging at the left
    ic.rect(3, 1, 7, 5, steel)
    ic.rect(4, 4, 6, 7, steel)
    img = ic.finish("RARE")
    for i in (2, 4, 6, 8):                                                      # grey leather strips wound round the haft
        put(ic, i, 0, wrap)
        put(ic, i, 1, wrap) if i % 4 == 0 else None
    ic.px(5, 3, rgb(0x2C2E34)); ic.px(6, 5, rgb(0x2C2E34))                      # the notch and the nicks of the edge
    ic.px(4, 2, rgb(0xFFFFFF)); ic.px(10, 2, rgb(0xE8EEF2))
    ic.px(9, 3, rgb(0x8A9096)); ic.px(12, 3, rgb(0x8A9096))
    ic.px(2, 14, rgb(0x6A6C72))                                                 # the ferrule at the butt
    return img


def white_whip_of_welting():
    ic = Icon()
    towel, shade = rgb(0xE6E6E2), rgb(0xB9BAB8)
    span(ic, 1, 12, -1, 0, towel)                                               # the tightly wound towel
    span(ic, 3, 11, 1, 1, shade)
    span(ic, 0, 0, -1, 0, towel)
    img = ic.finish("RARE")
    for i in (3, 5, 7, 9, 11):                                                  # the rolled folds
        put(ic, i, 0, shade)
    red, blue = rgb(0xD04848), rgb(0x4A68C8)                                    # the embroidered trim at the end
    put(ic, 0, -1, red); put(ic, 0, 0, blue); put(ic, 1, 1, red); put(ic, 1, 0, blue)
    ic.glints([(14, 1), (15, 2)], rgb(0x9CC8FF), 150)                           # a few drops: it is meant to be wetted
    ic.px(3, 8, rgb(0x9CC8FF), 190)
    return img


def whirling_blades():
    ic = Icon()
    dark_grip, cyan, red = rgb(0x3A3A42), rgb(0x66E8D8), rgb(0xE8704A)
    span(ic, 0, 3, -2, -2, dark_grip)
    span(ic, 0, 3, 2, 2, dark_grip)
    blade(ic, 4, 14, -3, -2, cyan)                                              # the cold blade, up-left
    blade(ic, 3, 13, 2, 3, red)                                                 # the hot blade, down-right
    ic.px(5, 12, rgb(0x3A3A42)); ic.px(8, 9, rgb(0x3A3A42)); ic.px(9, 8, rgb(0x3A3A42))   # the sword ends joined by their spiky grips
    img = ic.finish("RARE")
    put(ic, 7, -3, rgb(0xE8FFFB)); put(ic, 6, 3, rgb(0xFFD2B0))
    put(ic, 9, -3, rgb(0xB8FFF4)); put(ic, 8, 3, rgb(0xFFB08A))
    ic.glints([(0, 3), (13, 1), (15, 14)], rgb(0xA8F8F0), 120)
    return img


def warmages_longsword():
    ic = Icon()
    steel, gold, wrapc = rgb(0xCDD2DC), rgb(0xD8AC3C), rgb(0x2F4C94)
    put(ic, 0, 0, rgb(0xB070F0))                                                # the pommel gem
    span(ic, 1, 3, 0, 0, wrapc)
    span(ic, 4, 4, -2, 2, gold)                                                 # the crossguard
    blade(ic, 5, 14, -1, -1, steel)
    img = ic.finish("UNIQUE")
    for i in (6, 8, 10, 12):                                                    # five violet gems down the blade (the fifth is the pommel)
        put(ic, i, -1, rgb(0xB070FF))
    put(ic, 4, -3, gold); put(ic, 4, 3, gold)
    ic.glints([(12, 4), (13, 2), (9, 6), (2, 9)], rgb(0xD0A8FF), 160)            # the arcane mist
    return img


def sword_crystal():
    ic = Icon()
    ice, deep = rgb(0x5CC8F0), rgb(0x2E8FC8)
    span(ic, 0, 1, -1, 0, rgb(0xE0B090))                                        # the stopper
    blade(ic, 2, 14, -3, 0, ice)                                                # the jagged crystal
    span(ic, 6, 9, 1, 2, ice)
    img = ic.finish("RARE")
    for i, o in ((4, -2), (6, -1), (8, -2), (9, 0), (11, -1), (7, 2)):          # facets
        put(ic, i, o, deep)
    for i, o in ((5, 0), (7, -1), (10, -2), (12, 0), (8, 1)):
        put(ic, i, o, rgb(0xCFF6FF))
    ic.glints([(0, 6), (14, 13), (8, 2)], rgb(0xBFF4FF), 150)
    return img


def the_stabshroom():
    ic = Icon()
    capred, tan, grip, glow = rgb(0xC02A24), rgb(0xD8C090), rgb(0x4A2A2A), rgb(0x78F03C)
    span(ic, 0, 1, -1, 0, tan)                                                  # the shelf-mushroom pommel
    span(ic, 2, 4, 0, 0, grip)
    span(ic, 5, 5, -3, 3, capred)                                               # the red cap that is the guard
    span(ic, 6, 6, -2, 2, capred)
    blade(ic, 6, 14, -1, 0, glow)
    img = ic.finish("RARE")
    for o in (-2, 0, 2):                                                        # the white spots
        put(ic, 5, o, rgb(0xF8F0E8))
    put(ic, 6, -1, rgb(0xF8F0E8))
    for i in (6, 7):                                                            # orange where the blade meets the guard
        put(ic, i, 0, rgb(0xE88A2E))
    put(ic, 10, 0, rgb(0xD8FF9C)); put(ic, 12, 0, rgb(0xD8FF9C))
    ic.glints([(14, 3), (12, 2), (2, 7)], rgb(0x9CFF6C), 150)
    return img


def twin_hook_swords():
    ic = Icon()
    steel, teal = rgb(0xC0C8D2), rgb(0x1F7A68)
    for o in (-2, 1):
        span(ic, 0, 3, o, o, teal)                                              # the cord-wrapped grips
        span(ic, 4, 13, o, o, steel)                                            # the straight spear-like blades
    ic.px(1, 12, steel); ic.px(5, 8, steel)
    span(ic, 4, 4, -3, -2, rgb(0xDEE4EC))                                       # the crescent guards
    span(ic, 3, 3, 1, 2, rgb(0xDEE4EC))
    img = ic.finish("NORMAL")
    put(ic, 14, -2, steel); put(ic, 14, 1, steel)                               # the hooks at the points
    put(ic, 13, -3, steel); put(ic, 13, 0, steel)
    put(ic, 2, -2, rgb(0x2FB89A)); put(ic, 2, 1, rgb(0x2FB89A))
    return img


def bottle_blade():
    ic = Icon()
    green, band, water = rgb(0x2E8F6C), rgb(0xB8C0C8), rgb(0x7FEFFF)
    span(ic, 0, 3, -1, 1, green)                                                # the green bottle
    span(ic, 4, 5, 0, 0, green)                                                 # its neck
    span(ic, 2, 2, -1, 1, band)                                                 # the silver band with the sword and leaf
    blade(ic, 6, 14, 0, 0, water)                                               # the stream that shoots out and freezes into a blade
    span(ic, 8, 13, 1, 1, rgb(0xB8F6FF), 190)
    img = ic.finish("RARE")
    put(ic, 3, 0, rgb(0x6FD0A8)); put(ic, 1, -1, rgb(0x6FD0A8))
    put(ic, 2, 0, rgb(0x5A6068))
    for (x, y) in ((12, 5), (13, 6), (10, 8), (14, 10)):                        # droplets
        ic.px(x, y, rgb(0xBFFBFF), 190)
    return img


def disintegration_dagger():
    ic = Icon()
    black, brown, strap = rgb(0x2A2220), rgb(0x5A3A2C), rgb(0x1C1A22)
    span(ic, 0, 0, 0, 0, strap)
    span(ic, 1, 3, 0, 0, brown)                                                 # the grip
    span(ic, 4, 4, -3, 3, black)                                                # the spiked guard
    blade(ic, 5, 13, -1, 0, black)                                              # the layered, scale-ridged blade
    img = ic.finish("RARE")
    for i in (6, 8, 10, 12):                                                    # raised scale ridges
        put(ic, i, 0, brown)
    put(ic, 4, -3, brown); put(ic, 4, 3, brown)
    put(ic, 7, 0, rgb(0x58B4FF)); put(ic, 8, 0, rgb(0xB8E4FF))                  # the glowing blue rune
    put(ic, 2, 1, rgb(0x6A5A4A))                                                # the strap tied round it
    ic.glints([(14, 1), (13, 14), (3, 6)], rgb(0x58B4FF), 130)
    return img


ICONS = {
    "giant_jawblade": giant_jawblade,
    "the_lumberjack": the_lumberjack,
    "white_whip_of_welting": white_whip_of_welting,
    "whirling_blades": whirling_blades,
    "warmages_longsword": warmages_longsword,
    "sword_crystal": sword_crystal,
    "the_stabshroom": the_stabshroom,
    "twin_hook_swords": twin_hook_swords,
    "bottle_blade": bottle_blade,
    "disintegration_dagger": disintegration_dagger,
}


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, "rainbow_rapier.json")) as f:                 # the 3D item model every wave 1 weapon shares
        entity_model = json.load(f)
    made = {}
    for rid, fn in ICONS.items():
        img = fn()
        img.save(os.path.join(TEX, rid + ".png"))
        em = dict(entity_model)
        em["textures"] = {"particle": "nusmp:item/" + rid}
        with open(os.path.join(MODELS, rid + ".json"), "w", newline="\n") as f:
            json.dump(em, f, indent=2)
            f.write("\n")
        with open(os.path.join(MODELS, rid + "_icon.json"), "w", newline="\n") as f:
            json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
            f.write("\n")
        made[rid] = img
    if "--sheet" in sys.argv:
        path = sys.argv[sys.argv.index("--sheet") + 1]
        scale = 12
        sheet = Image.new("RGBA", (5 * 16 * scale + 60, 2 * 16 * scale + 30), (58, 58, 66, 255))
        for i, (rid, img) in enumerate(made.items()):
            big = img.resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.alpha_composite(big, (10 + (i % 5) * (16 * scale + 10), 10 + (i // 5) * (16 * scale + 10)))
        sheet.save(path)
    print("wave 12: wrote %d icons" % len(made))


if __name__ == "__main__":
    main()
