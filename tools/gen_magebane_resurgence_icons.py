"""1.2.0: inventory icons and item models of the Magebane Shield and the Weapon of Resurgence (reference sheets
magebane_longsword_and_shield.jpg and paladin_items_weapon_of_resurgence_hand_of_providence_cloak_deliverance.jpg).

    python3 -B tools/gen_magebane_resurgence_icons.py

Writes
  assets/nusmp/textures/item/<id>.png       the 16x16 inventory icon (the shield stands upright, the sword lies on the usual diagonal)
  assets/nusmp/models/item/<id>_icon.json   the flat inventory icon model
  assets/nusmp/models/item/<id>.json        the 3D in-hand model (builtin/entity, drawn by client/WeaponRenderer from the mesh that
                                            tools/gen_weapon_models.py builds); the sword's display transforms are the Magebane
                                            Longsword's, the shield has its own (held on the forearm, its face turned to the front).
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
ICONS = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")
S = 16

from gen_reference_weapon_icons import sword  # noqa: E402  (writes textures/item/<id>.png)


def rgba(c):
    return (c[0], c[1], c[2], 255)


# ---------------------------------------------------------------- the Weapon of Resurgence: a jade longsword wound with a holy chain
def resurgence():
    sword("weapon_of_resurgence", (8, 44, 40), (24, 104, 90), (52, 160, 132), (150, 235, 200), (255, 238, 160),
          guard=((150, 104, 30), (236, 192, 72), (255, 236, 150)), grip=(22, 66, 58), gem=(255, 246, 200), thick=2, tip=14)
    path = os.path.join(ICONS, "weapon_of_resurgence.png")
    im = Image.open(path).convert("RGBA")
    px = im.load()
    gold, gold_d, gold_l = (236, 192, 72), (160, 112, 34), (255, 236, 150)
    # the holy chain winding up the blade: a bright link, then a shadowed one crossing the blade on the other side
    for k in range(1, 10):
        x, y = 4 + k, 11 - k
        if k % 2 == 1:
            px[x, y] = rgba(gold_l if k % 4 == 1 else gold)
            if 0 <= x + 1 < S and 0 <= y - 1 < S and px[x + 1, y - 1][3] == 0:
                px[x + 1, y - 1] = rgba(gold_d)
        else:
            px[x, y] = rgba(gold_d)
    # a cross-guard: two short quillons beyond the bar, tipped with gold
    for (x, y) in [(0, 9), (6, 15)]:
        if 0 <= x < S and 0 <= y < S:
            px[x, y] = rgba(gold_d)
    im.save(path)


# ---------------------------------------------------------------- the Magebane Shield: a turquoise heater shield, bronze-silver rim, a phoenix
SHIELD = [
    "................",
    "..OOOOOOOOOOOO..",
    ".ORrrrRRRRrrrRO.",
    ".OrFFFFFFFFFFrO.",
    ".ORFFhFFFFhFFRO.",
    ".ORFFFFFFFFFFRO.",
    ".ORFFFFFFFFFFRO.",
    ".ORFFFFFFFFFFRO.",
    "..ORfFFFFFFfRO..",
    "..ORfFFFFFFfRO..",
    "...ORfFFFFfRO...",
    "...ORffFFffRO...",
    "....ORfFFfRO....",
    ".....ORffRO.....",
    "......OROO......",
    ".......OO.......",
]
# the phoenix, left half (x, y); mirrored to the right half across the shield's axis
PHOENIX_L = [(7, 4), (7, 5), (7, 6), (7, 7), (7, 8), (6, 6), (5, 5), (4, 4), (3, 3), (6, 7), (5, 6), (4, 5), (3, 4), (5, 7), (4, 6), (6, 9), (5, 9), (6, 10), (7, 10), (6, 11), (4, 3)]
PALETTE = {
    "O": (14, 40, 44),         # outline
    "R": (150, 132, 104),      # rim, worn silver-bronze
    "r": (214, 196, 156),      # rim highlight
    "F": (52, 158, 156),       # field, turquoise
    "f": (30, 100, 108),       # field, shadowed
    "h": (130, 226, 214),      # field, glint
    "B": (206, 160, 92),       # emblem, bronze
    "e": (190, 255, 244),      # the phoenix's eye, glowing
}


def shield():
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(SHIELD):
        assert len(row) == S, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = rgba(PALETTE[ch])
    for (x, y) in PHOENIX_L:
        px[x, y] = rgba(PALETTE["B"])
        px[S - 1 - x, y] = rgba(PALETTE["B"])
    for (x, y) in [(7, 4), (8, 4)]:
        px[x, y] = rgba(PALETTE["e"])                  # the eyes
    for (x, y) in [(5, 5), (10, 5), (4, 4), (11, 4)]:
        px[x, y] = rgba((236, 196, 128))                 # feather highlights
    im.save(os.path.join(ICONS, "magebane_shield.png"))


# ---------------------------------------------------------------- item models
def write_json(name, data):
    with open(os.path.join(MODELS, name + ".json"), "w", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def icon_model(item_id):
    write_json(item_id + "_icon", {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}})


def tf(rot, tr, sc):
    return {"rotation": list(rot), "translation": list(tr), "scale": [sc, sc, sc]}


def sword_model(item_id):
    """The Magebane Longsword's display transforms (the sprite sits on the usual diagonal)."""
    src = json.load(open(os.path.join(MODELS, "magebane_longsword.json")))
    src["textures"] = {"particle": "nusmp:item/" + item_id}
    write_json(item_id, src)


def shield_model(item_id):
    """The shield is built upright with its face toward +z. Held on the forearm:
    third person (item frame: x = the wearer's right, y = forward, z = up): rotation X -90 then Z 180 turns the face to the front and
    the point down; the shield sits a little in front of and above the hand, outside the arm (a left-hand value is mirrored by the game).
    First person (camera frame, z toward the viewer): the face turns to the viewer, tilted a little toward the centre of the screen;
    the shield rides low in the corner of the view."""
    d = {
        "thirdperson_righthand": tf((-90, 0, 180), (2.5, 4.5, 3.0), 1.05),
        "thirdperson_lefthand": tf((-90, 0, 180), (2.5, 4.5, 3.0), 1.05),
        "firstperson_righthand": tf((0, -25, 8), (2.0, 5.5, 0.5), 0.62),
        "firstperson_lefthand": tf((0, -25, 8), (2.0, 5.5, 0.5), 0.62),
        "ground": tf((0, 0, 0), (0, 2, 0), 0.5),
        "head": tf((0, 180, 0), (0, 13, 7), 1),
        "fixed": tf((0, 180, 0), (0, 0, 0), 1),
        "gui": tf((0, 0, 0), (0, 0, 0), 1),
    }
    write_json(item_id, {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id}, "display": d})


def main():
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    resurgence()
    shield()
    for item_id in ("weapon_of_resurgence", "magebane_shield"):
        icon_model(item_id)
    sword_model("weapon_of_resurgence")
    shield_model("magebane_shield")
    print("wrote the icons and models of magebane_shield and weapon_of_resurgence")


if __name__ == "__main__":
    main()
