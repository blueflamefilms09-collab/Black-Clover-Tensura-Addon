"""dark_item_icon: the 16x16 sprite and the plain generated model of the Dark Rukh Shard (relics.djinn.dark).

A black crystal splinter with a violet core and a thin gold edge, a few wisps of black rukh round it. Pure PIL, deterministic, idempotent:
    python tools/relics/dark_item_icon.py
writes src/main/resources/assets/nusmp/textures/item/dark_rukh_shard.png and models/item/dark_rukh_shard.json.
"""
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp"))

BLACK = (14, 8, 24)
DEEP = (34, 14, 58)
VIOLET = (96, 40, 150)
GLOW = (176, 112, 232)
GOLD = (236, 190, 70)
GOLD_DIM = (168, 126, 40)
WISP = (52, 24, 82)

# the splinter: a tall, slightly leaning crystal; one row per entry, x0..x1 inclusive of the body on that row (y from the top)
BODY = {
    1: (8, 9),
    2: (7, 9),
    3: (7, 10),
    4: (6, 10),
    5: (6, 10),
    6: (5, 10),
    7: (5, 10),
    8: (5, 10),
    9: (5, 9),
    10: (5, 9),
    11: (5, 8),
    12: (6, 8),
    13: (6, 7),
    14: (7, 7),
}


def build():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, (x0, x1) in BODY.items():
        for x in range(x0, x1 + 1):
            t = (x - x0) / max(1, x1 - x0)                      # 0 left .. 1 right: lit from the upper left
            if x == x0:
                c = GOLD if y % 3 != 2 else GOLD_DIM            # the gold edge, broken like a chipped inlay
            elif x == x1:
                c = BLACK
            elif t < 0.5:
                c = VIOLET if 3 <= y <= 9 else DEEP
            else:
                c = DEEP
            px[x, y] = c + (255,)
    for y in range(4, 9):                                       # the living core
        px[8, y] = GLOW + (255,)
    px[8, 6] = (232, 210, 255, 255)
    px[7, 5] = VIOLET + (255,)
    px[9, 8] = VIOLET + (255,)
    px[8, 1] = GOLD + (255,)                                    # the tip catches the light
    # wisps of black rukh curling off the shard
    for (x, y) in ((3, 4), (2, 5), (3, 6), (12, 3), (13, 4), (12, 5), (11, 9), (12, 10), (4, 12), (3, 13), (11, 12)):
        px[x, y] = WISP + (255,)
    for (x, y) in ((3, 5), (12, 4)):
        px[x, y] = (80, 40, 120, 255)
    return im


def main():
    tex = os.path.join(ASSETS, "textures", "item")
    mod = os.path.join(ASSETS, "models", "item")
    os.makedirs(tex, exist_ok=True)
    os.makedirs(mod, exist_ok=True)
    build().save(os.path.join(tex, "dark_rukh_shard.png"))
    with open(os.path.join(mod, "dark_rukh_shard.json"), "w", encoding="utf-8") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/dark_rukh_shard"}}, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    main()
