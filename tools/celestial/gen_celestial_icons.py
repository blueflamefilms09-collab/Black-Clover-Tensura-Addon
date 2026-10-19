"""Celestial Spirit Magic: the 16 x 16 inventory icons and the item model json of the keys, the book and the sceptre.

    python3 -B tools/celestial/gen_celestial_icons.py

Writes assets/nusmp/textures/item/<id>.png (the inventory icon; the 3D model of tools/celestial/gen_celestial_keys.py is placed on its diagonal), and
assets/nusmp/models/item/<id>.json (builtin/entity: the item is drawn by client/WeaponRenderer, "nusmp_display_preset": "manual") and <id>_icon.json (the flat icon).
A key is a diagonal key: the bit at the lower left, a straight shaft, a round bow at the upper right with the colour of its spirit as a gem and a few pixels that make the
sign of the spirit (horns for Taurus, drops for Aquarius, a mane for Leo, claws for Cancer ...). Gold keys are gold, silver keys silver, the gate key pale gold and white.
Deterministic.
"""
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from celestial_data import KEYS, out_path, rgb, mix  # noqa: E402

OUTLINE = (24, 18, 34)
RAMPS = {
    "gold": [(255, 244, 170), (240, 196, 64), (190, 138, 22), (110, 72, 14)],
    "silver": [(246, 249, 255), (200, 208, 224), (142, 150, 170), (76, 82, 100)],
    "gate": [(255, 255, 255), (252, 238, 176), (214, 192, 120), (128, 108, 62)],
}
# the pixels that make the sign: offsets in the 7 x 7 box round the bow (x right, y down); the ring itself is the 5 x 5 inside it
EXTRAS = {
    "aquarius": [(2, 0), (4, 0), (3, 1)], "taurus": [(1, 0), (5, 0), (0, 0), (6, 0)], "leo": [(3, 0), (0, 3), (6, 3), (3, 6), (1, 1), (5, 1)],
    "virgo": [(2, 0), (3, 0), (4, 0)], "sagittarius": [(6, 0), (5, 0), (6, 1), (4, 1)], "gemini": [(2, 0), (4, 0), (2, 6), (4, 6)],
    "cancer": [(0, 2), (0, 4), (6, 2), (6, 4), (0, 1), (6, 1)], "scorpio": [(6, 2), (6, 1), (5, 0), (4, 0)], "capricorn": [(0, 0), (0, 1), (6, 0), (6, 1)],
    "pisces": [(0, 2), (0, 4), (6, 2), (6, 4)], "aries": [(0, 1), (0, 2), (6, 1), (6, 2), (0, 3), (6, 3)], "libra": [(3, 0), (0, 3), (6, 3), (1, 1), (5, 1)],
    "horologium": [(1, 0), (5, 0), (1, 6), (5, 6)], "crux": [(3, 0), (3, 6), (0, 3), (6, 3)], "lyra": [(0, 2), (0, 3), (0, 4), (1, 1), (1, 5)],
    "nikora": [(1, 0), (5, 0), (1, 1), (5, 1)], "spirit_king_gate": [(3, 0), (2, 1), (4, 1), (1, 1), (5, 1)],
}
BOW_CENTRE = (10, 5)         # the middle of the 7 x 7 box on the 16 x 16 icon


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = c

    def outline(self):
        out = {}
        for (x, y) in list(self.px):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                p = (x + dx, y + dy)
                if p not in self.px and 0 <= p[0] < 16 and 0 <= p[1] < 16:
                    out[p] = OUTLINE
        self.px.update(out)

    def image(self):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), c + (255,))
        return im


def key_icon(kid, rank, colour):
    r = RAMPS[rank]
    c = Canvas()
    cx, cy = BOW_CENTRE
    ring = [(1, 0), (2, 0), (3, 0), (0, 1), (4, 1), (0, 2), (4, 2), (0, 3), (4, 3), (1, 4), (2, 4), (3, 4)]
    for (dx, dy) in ring:                                                      # the 5 x 5 ring, lit from the upper left
        shade = r[0] if dx + dy <= 1 else r[1] if dx + dy <= 3 else r[2] if dx + dy <= 5 else r[3]
        c.put(cx - 3 + 1 + dx, cy - 3 + 1 + dy, shade)
    for (dx, dy) in EXTRAS[kid]:
        c.put(cx - 3 + dx, cy - 3 + dy, r[1] if dx + dy <= 6 else r[2])
    gem = rgb(colour)
    c.put(cx - 1, cy - 1, mix(gem, (255, 255, 255), 0.55))                      # the gem in the middle of the ring
    c.put(cx, cy - 1, gem)
    c.put(cx - 1, cy, gem)
    c.put(cx, cy, mix(gem, (0, 0, 0), 0.35))
    for i in range(7):                                                         # the shaft, a diagonal two pixels wide, from the ring down to the bit
        x, y = cx - 2 - i, cy + 2 + i
        c.put(x, y, r[1]); c.put(x + 1, y, r[2] if i > 2 else r[1])
        if i == 0:
            c.put(x + 1, y - 1, r[2])
    for (x, y, k) in ((4, 12, 1), (3, 13, 2), (5, 11, 1), (2, 14, 2)):          # the bit: two teeth to one side
        c.put(x - 1 + (1 if k == 1 else 0), y, r[2])
    c.put(3, 11, r[1]); c.put(4, 13, r[1])
    c.put(6, 10, r[0])                                                         # a collar highlight
    c.outline()
    return c.image()


def book_icon():
    c = Canvas()
    cover = [(62, 40, 130), (86, 60, 170), (118, 90, 210), (160, 130, 240)]
    gold = RAMPS["gold"]
    for y in range(3, 14):
        for x in range(3, 13):
            sh = cover[0] if x >= 11 else cover[1] if x >= 8 else cover[2] if x >= 5 else cover[3] if y <= 5 else cover[2]
            c.put(x, y, sh)
    for y in range(3, 14):                                                     # the spine and the page edge
        c.put(3, y, cover[0]); c.put(12, y, (240, 236, 220))
    for x in range(3, 13):
        c.put(x, 13, (222, 216, 196))
    for (x, y) in ((4, 4), (11, 4), (4, 12), (11, 12)):                         # the gold corners
        c.put(x, y, gold[1])
    for (x, y) in ((7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8), (6, 9), (9, 9)):   # the star of the cover
        c.put(x, y, gold[0] if y <= 7 else gold[1])
    c.put(8, 7, (255, 255, 255))
    for y in range(9, 15):                                                     # the ribbon
        c.put(10, y, (214, 70, 90))
    c.outline()
    return c.image()


def sceptre_icon():
    c = Canvas()
    gold = RAMPS["gold"]
    for i in range(10):                                                        # the staff, a diagonal from the lower left to the orb
        x, y = 2 + i, 13 - i
        c.put(x, y, gold[1]); c.put(x + 1, y, gold[2])
    for (x, y) in ((10, 4), (11, 4), (10, 3), (11, 3), (12, 3), (11, 2), (12, 2), (10, 2), (11, 5), (9, 4), (12, 4)):   # the cage and orb
        c.put(x, y, gold[1])
    for (x, y) in ((11, 3), (12, 2), (11, 2), (10, 3)):
        c.put(x, y, (255, 250, 220))
    c.put(11, 3, (255, 255, 255))
    for (x, y) in ((9, 3), (13, 3), (11, 1), (12, 5)):                         # rays of light
        c.put(x, y, (255, 236, 160))
    c.put(7, 8, gold[0]); c.put(5, 10, gold[0])
    c.outline()
    return c.image()


DISPLAY_DIAGONAL = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
DISPLAY_BOOK = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def write_models(item_id, display):
    with open(out_path("models", "item", item_id + ".json"), "w", newline="\n") as f:
        json.dump({"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id}, "display": display,
                   "nusmp_display_preset": "manual"}, f, indent=2)
        f.write("\n")
    with open(out_path("models", "item", item_id + "_icon.json"), "w", newline="\n") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}, f, indent=2)
        f.write("\n")


def main():
    ids = []
    for kid, _name, rank, colour in KEYS:
        item_id = kid + "_key"
        key_icon(kid, rank, colour).save(out_path("textures", "item", item_id + ".png"))
        write_models(item_id, DISPLAY_DIAGONAL)
        ids.append(item_id)
    book_icon().save(out_path("textures", "item", "celestial_spirit_magic_book.png"))
    write_models("celestial_spirit_magic_book", DISPLAY_BOOK)
    sceptre_icon().save(out_path("textures", "item", "spirit_king_sceptre.png"))
    write_models("spirit_king_sceptre", DISPLAY_DIAGONAL)
    ids += ["celestial_spirit_magic_book", "spirit_king_sceptre"]
    print("icons and item models written for", len(ids), "items")
    return ids


if __name__ == "__main__":
    main()
