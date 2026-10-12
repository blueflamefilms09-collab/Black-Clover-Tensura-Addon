"""1.6.0: the Grimoire Summons' 16x16 icon: a cream letter with a gold wax seal."""
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_summons.png")
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = im.load()
INK = (60, 40, 24, 255)
for y in range(3, 14):
    for x in range(2, 14):
        edge = x in (2, 13) or y in (3, 13)
        px[x, y] = INK if edge else (236 + (x + y) % 3 * 3, 222 + (x * 2 + y) % 3 * 3, 186, 255)
for i in range(1, 6):                                                    # the flap folds from the corners to the middle
    px[2 + i, 3 + i] = (170, 140, 96, 255)
    px[13 - i, 3 + i] = (170, 140, 96, 255)
for (x, y) in ((7, 8), (8, 8), (7, 9), (8, 9), (6, 8), (9, 8), (6, 9), (9, 9), (7, 7), (8, 7), (7, 10), (8, 10)):
    px[x, y] = (214, 168, 52, 255)
px[7, 8] = (255, 232, 140, 255)
px[8, 9] = (150, 108, 24, 255)
im.save(OUT)
print("wrote", OUT)
