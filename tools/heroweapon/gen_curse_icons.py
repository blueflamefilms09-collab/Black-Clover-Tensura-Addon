"""1.62.0: the inventory icon and the item model of the Purification Charm (the stand-in for the Bless Series of the hero weapons' Curse Series).

    python3 -B tools/heroweapon/gen_curse_icons.py            writes the icon and the model into src/main/resources
    python3 -B tools/heroweapon/gen_curse_icons.py --preview DIR   also writes a 16x enlarged picture of the icon (on dark and on light) into DIR

Writes
  assets/nusmp/textures/item/purification_charm.png    the 16x16 icon
  assets/nusmp/models/item/purification_charm.json     the flat item model (item/generated)

The charm is a small shield of white pearl in a gold frame, with a pale aqua gem in its heart and a spark of gold light across the gem (the curse breaking),
hanging from a cord. White and gold are the colours of the Shield of Compassion, the Bless Series shield of the series. The picture is drawn
from the character map below, one letter per pixel, so it can be changed by editing the map; the palette is the table under it. No randomness: running it twice
gives the same file.
"""
import json
import os
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
ICON_DIR = os.path.join(ASSETS, "textures", "item")
MODEL_DIR = os.path.join(ASSETS, "models", "item")

ITEM_ID = "purification_charm"

# one letter per pixel, 16 rows of 16 columns; '.' is transparent
CHARM = [
    "......oRRo......",
    ".....oR..Ro.....",
    ".....oR..Ro.....",
    "......oRRo......",
    "...oooggggooo...",
    "..oGYYYYYYYYGo..",
    "..oGYWWWWWwYGo..",
    "..oGYWWBBWwYGo..",
    "..oGWWBCCBWwGo..",
    "..oGWWBCCBWwGo..",
    "...oGWWBBWwGo...",
    "....oGWWWwGo....",
    ".....oGWwGo.....",
    "......oGGo......",
    ".......oo.......",
    "................",
]

# the spark across the gem: gold pixels drawn over the map (the curse breaking into light)
CRACK = [(7, 7), (8, 10)]

PALETTE = {
    "o": (58, 40, 20),      # outline, dark brown
    "R": (150, 100, 52),    # cord
    "g": (156, 108, 22),    # gold, shadow
    "G": (222, 172, 42),    # gold
    "Y": (255, 226, 112),   # gold, highlight
    "W": (246, 240, 224),   # pearl
    "w": (206, 198, 176),   # pearl, shade
    "b": (36, 138, 150),    # aqua, dark
    "B": (88, 206, 198),    # aqua
    "C": (226, 255, 250),   # aqua, core
    "T": (236, 220, 160),   # tassel
}


def build():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(CHARM):
        assert len(row) == 16, "row %d is %d wide" % (y, len(row))
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            r, g, b = PALETTE[ch]
            im.putpixel((x, y), (r, g, b, 255))
    for (x, y) in CRACK:
        im.putpixel((x, y), (255, 214, 72, 255))
    return im


def write_model():
    os.makedirs(MODEL_DIR, exist_ok=True)
    model = {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + ITEM_ID}}
    with open(os.path.join(MODEL_DIR, ITEM_ID + ".json"), "w") as f:
        json.dump(model, f, indent=2)
        f.write("\n")


def preview(im, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    big = im.resize((256, 256), Image.NEAREST)
    sheet = Image.new("RGBA", (256 * 2 + 16, 256), (0, 0, 0, 255))
    dark = Image.new("RGBA", (256, 256), (38, 34, 46, 255))
    light = Image.new("RGBA", (256, 256), (198, 198, 198, 255))
    dark.alpha_composite(big)
    light.alpha_composite(big)
    sheet.paste(dark, (0, 0))
    sheet.paste(light, (272, 0))
    sheet.save(os.path.join(out_dir, ITEM_ID + "_x16.png"))


def main(argv):
    os.makedirs(ICON_DIR, exist_ok=True)
    im = build()
    im.save(os.path.join(ICON_DIR, ITEM_ID + ".png"))
    write_model()
    if "--preview" in argv:
        i = argv.index("--preview")
        preview(im, argv[i + 1])
    print("wrote", ITEM_ID)


if __name__ == "__main__":
    main(sys.argv[1:])
