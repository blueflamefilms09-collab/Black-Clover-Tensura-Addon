"""Gauche's hand mirror (0.34): a 16x16 pixel-art relic sprite - an oval silver-framed mirror with a handle, pale blue glass, a glint.

    python tools/gen_relic_mirror.py  ->  textures/item/gauches_hand_mirror.png + models/item/gauches_hand_mirror.json
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item", "gauches_hand_mirror.png")
MODEL = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "models", "item", "gauches_hand_mirror.json")
SILVER = ["#4a5262", "#8a94a6", "#c8d0dc", "#ffffff"]
GLASS = ["#5aa0c8", "#8ccbe8", "#c8ecfa", "#ffffff"]
GRIP = ["#3a2a3e", "#5a4060", "#7a5a80"]


def rgb(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


def main():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy, rx, ry = 9.5, 5.5, 4.6, 5.2                    # the oval head, upper right
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2
            if d <= 0.62:
                g = GLASS[2] if (x + 0.5 - cx) < -0.5 else GLASS[1] if (x + 0.5 - cx) < 1.5 else GLASS[0]
                im.putpixel((x, y), rgb(g))
            elif d <= 1.0:
                im.putpixel((x, y), rgb(SILVER[2] if y + 0.5 < cy else SILVER[1]))
    for (x, y) in ((8, 3), (9, 2), (7, 5)):                 # the glint
        im.putpixel((x, y), rgb(GLASS[3]))
    for k in range(6):                                      # the handle, down-left
        x, y = 6 - k, 10 + k
        if 0 <= x < 16 and 0 <= y < 16:
            im.putpixel((x, y), rgb(GRIP[1 if k % 2 else 2]))
            if x + 1 < 16:
                im.putpixel((x + 1, y), rgb(GRIP[0]))
    im.putpixel((1, 15), rgb(SILVER[2]))
    im.save(TEX)
    with open(MODEL, "w", newline="\n") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/gauches_hand_mirror"}}, f)
        f.write("\n")
    print("wrote gauches_hand_mirror")


if __name__ == "__main__":
    main()
