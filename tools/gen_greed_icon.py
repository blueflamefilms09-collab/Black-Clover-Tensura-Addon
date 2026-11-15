"""Draws the Lord of Gluttonous Greed skill icon (32x32): a black square with a crimson edge, a glowing red hungry maw with teeth and two slit eyes.
Original art, drawn from scratch with Pillow. Run: python tools/gen_greed_icon.py -> src/main/resources/assets/anime_multiverse/textures/skill/lord_of_gluttonous_greed.png"""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "skill", "lord_of_gluttonous_greed.png")
S = 32


def icon():
    im = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    px = im.load()
    c = (S - 1) / 2.0
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - c, y - c) / 22.0
            v = int(30 - 22 * min(1.0, d))
            px[x, y] = (max(4, v // 2 + 4), 2, 6, 255)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, S - 1, S - 1], outline=(150, 14, 34, 255))
    d.rectangle([1, 1, S - 2, S - 2], outline=(70, 6, 18, 255))
    # slit eyes
    for ex in (9, 21):
        d.polygon([(ex - 4, 9), (ex + 4, 8), (ex + 2, 11), (ex - 3, 11)], fill=(235, 30, 52, 255))
        d.line([(ex - 1, 9), (ex + 2, 9)], fill=(255, 190, 190, 255))
    # the maw: a wide dark mouth ringed in red
    d.ellipse([5, 14, 26, 29], fill=(190, 16, 40, 255))
    d.ellipse([7, 16, 24, 28], fill=(14, 0, 4, 255))
    # teeth, top and bottom
    for x in range(8, 24, 3):
        d.polygon([(x, 16), (x + 3, 16), (x + 1.5, 21)], fill=(235, 225, 215, 255))
    for x in range(9, 23, 3):
        d.polygon([(x, 28), (x + 3, 28), (x + 1.5, 23)], fill=(220, 208, 198, 255))
    # a glint of hunger in the throat
    d.ellipse([13, 22, 18, 25], fill=(120, 6, 20, 255))
    return im


if __name__ == "__main__":
    icon().save(OUT)
    print("wrote", OUT)
