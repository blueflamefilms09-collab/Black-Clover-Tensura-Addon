"""Paints the inventory icons (16x16 sprites, the inventory keeps a sprite; the item in hand / on the ground / in a frame is the 3D Blender model) and the vanilla armour layer textures of the Saiyan alloy set.

    python3 -B tools/saiyan_armor/gen_icons.py [--check]

Writes src/main/resources/assets/anime_multiverse/textures/item/saiyan_alloy_*.png, textures/models/armor/saiyan_alloy_layer_1.png / _layer_2.png (the plain layer a mob or an armour stand wears; a
player wears the Blender models instead) and the item model jsons (models/item/<id>.json = builtin/entity for the 3D object form, <id>_icon.json = the sprite, the way
tools/relics/gen_armor_item_models.py does it for the other armour). Idempotent. Colours: the white plate, gold trim, navy lame and steel of the cosmetic battle armour (tools/gen_saiyan_armor_geo.py).
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
TEX_ITEM = os.path.join(ASSETS, "textures", "item")
TEX_ARMOR = os.path.join(ASSETS, "textures", "models", "armor")
MODELS = os.path.join(ASSETS, "models", "item")

PAL = {".": None, "o": (0x1B, 0x20, 0x36), "w": (0xE9, 0xEC, 0xF2), "W": (0xA9, 0xB2, 0xC8), "g": (0xE2, 0xB5, 0x36), "G": (0x9A, 0x6C, 0x10), "n": (0x2A, 0x3F, 0x9A),
       "N": (0x14, 0x20, 0x5E), "s": (0x7C, 0x84, 0x96), "S": (0x45, 0x4C, 0x5E), "l": (0x44, 0xE8, 0x90), "L": (0xB8, 0xFF, 0xD8), "h": (0xFF, 0xFF, 0xFF), "b": (0xBF, 0xEF, 0xFF)}

ICONS = {
    "saiyan_alloy_ingot": [
        "................",
        "................",
        "................",
        "....oooooooo....",
        "...owwwwwwWWo...",
        "..owhhwwwwWWGo..",
        ".oggggggggggGGo.",
        ".oggggggggggGGo.",
        ".oGggggggggGGGo.",
        "..oGGGGGGGGGGo..",
        "...oooooooooo...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "saiyan_alloy_scouter": [
        "................",
        "................",
        ".......s........",
        ".......b........",
        "......sS........",
        "..oooooosSo.....",
        ".osssssssSSo....",
        ".osLLLLLsSSSo...",
        ".oslllllsSSSo...",
        ".oslllllssSSo...",
        ".osLLLLLsSSo....",
        ".osssssssSo.....",
        "..ooooooSo......",
        "...........o....",
        "................",
        "................",
    ],
    "saiyan_alloy_battle_armor": [
        "................",
        ".oooo.gggg.oooo.",
        "owwwwoggggowwwwo",
        "owwgwwowwowwgwwo",
        "oWwwwwowwowwwwWo",
        ".oWwwwwwwwwwwWo.",
        "..owwwbwwbwwwo..",
        "..owwwbwwbwwwo..",
        "..owwwwggwwwwo..",
        "..ogggggggggGo..",
        "..onnnnnnnnnNo..",
        "..onnnnnnnnnNo..",
        "..oNnnnnnnnNNo..",
        "...oooooooooo...",
        "................",
        "................",
    ],
    "saiyan_alloy_waist_guard": [
        "................",
        "................",
        "..oooooooooooo..",
        "..onnnnggnnnNo..",
        "..onnnnggnnnNo..",
        "..oooooooooooo..",
        ".owwooggoogwwwo.",
        ".owwoggggoggwwo.",
        ".owwoggggoggwwo.",
        ".oWWoggggoggWWo.",
        "..oooGggGoGGooo.",
        "...ooowwwwwooo..",
        "................",
        "................",
        "................",
        "................",
    ],
    "saiyan_alloy_greaves": [
        "................",
        "................",
        "..oooo....oooo..",
        "..onnNo..oNnnNo.",
        "..oggGo..oGggGo.",
        "..owwWo..oWwwWo.",
        "..owwWo..oWwwWo.",
        "..ogggo..ooGGgo.",
        "..owwWo..oWwwWo.",
        ".owwwWo..oWwwwWo",
        ".owwwwWo.oWwwwwo",
        ".oooooooooooooo.",
        "................",
        "................",
        "................",
        "................",
    ],
}

DISPLAY = {
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def sprite(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if PAL[c] is not None:
                im.putpixel((x, y), PAL[c] + (255,))
    return im


def fill(im, x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            im.putpixel((xx, yy), PAL[c] + (255,))


def layers():
    """The vanilla 64x32 armour layers. layer_1: helmet, chestplate (body + arms), boots; layer_2: leggings. UV boxes as in vanilla (head 0,0; body 16,16; right arm 40,16; right leg 0,16)."""
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill(l1, 0, 0, 32, 16, "s")                      # the scouter helmet: a steel cap, a lens on the front's left side
    fill(l1, 8, 8, 8, 2, "S")
    fill(l1, 12, 11, 3, 3, "l")
    fill(l1, 0, 8, 32, 1, "g")
    fill(l1, 16, 16, 24, 16, "w")                    # the chestplate: white plate, gold line, navy lame
    fill(l1, 20, 26, 8, 6, "n")
    fill(l1, 20, 25, 8, 1, "g")
    fill(l1, 32, 28, 8, 4, "N")
    fill(l1, 23, 20, 2, 5, "g")
    fill(l1, 40, 16, 16, 16, "w")                    # arms: white pad with a gold rim at the top, a gold band at the wrist
    fill(l1, 40, 16, 16, 2, "g")
    fill(l1, 40, 27, 16, 1, "g")
    fill(l1, 0, 16, 16, 16, "w")                     # boots: white, gold band, navy at the hip end (leggings overlay it in layer_2)
    fill(l1, 0, 16, 16, 4, "n")
    fill(l1, 0, 26, 16, 1, "g")
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill(l2, 0, 16, 16, 16, "n")                     # leggings: navy, gold trim at the top, white shin plate in front
    fill(l2, 0, 16, 16, 1, "g")
    fill(l2, 4, 24, 4, 6, "w")
    fill(l2, 16, 16, 24, 16, "n")                    # the belt / waist guard region of the body
    fill(l2, 16, 17, 24, 1, "g")
    fill(l2, 22, 17, 4, 3, "g")
    return l1, l2


def write_all(check):
    problems = []
    os.makedirs(TEX_ITEM, exist_ok=True)
    os.makedirs(TEX_ARMOR, exist_ok=True)
    for name, rows in ICONS.items():
        sprite(rows).save(os.path.join(TEX_ITEM, name + ".png"))
    l1, l2 = layers()
    l1.save(os.path.join(TEX_ARMOR, "saiyan_alloy_layer_1.png"))
    l2.save(os.path.join(TEX_ARMOR, "saiyan_alloy_layer_2.png"))
    os.makedirs(MODELS, exist_ok=True)
    for name in ICONS:
        icon = {"parent": "minecraft:item/generated", "textures": {"layer0": "anime_multiverse:item/" + name}}
        if name == "saiyan_alloy_ingot":
            json.dump(icon, open(os.path.join(MODELS, name + ".json"), "w"), indent=2)
            continue
        json.dump(icon, open(os.path.join(MODELS, name + "_icon.json"), "w"), indent=2)
        json.dump({"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "anime_multiverse:item/" + name}, "display": DISPLAY},
                  open(os.path.join(MODELS, name + ".json"), "w"), indent=2)
    return problems


if __name__ == "__main__":
    write_all("--check" in sys.argv)
    print("icons, layers and item models written")
