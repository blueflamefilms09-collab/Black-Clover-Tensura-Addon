#!/usr/bin/env python3
"""Why: the three small textures of the Nations system's two blocks (the nation banner and the contract board) are drawn by code so they are reproducible and no binary has to be hand-made.
Run from the repo root: python3 tools/elite/gen_nations_assets.py   (needs Pillow). Writes assets/nusmp/textures/block/{nation_banner_cloth,nation_banner_pole,contract_board_side}.png.
These are first-pass art for blocks that carry no nation colour (the screen and the name carry it); replace the PNGs freely, nothing reads their content."""
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "nusmp", "textures", "block")


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def cloth():
    img = Image.new("RGBA", (16, 16), (122, 20, 28, 255))
    for y in range(16):
        for x in range(16):
            shade = ((x * 7 + y * 13) % 5) - 2
            r, g, b, _ = img.getpixel((x, y))
            img.putpixel((x, y), (max(0, min(255, r + shade * 3)), max(0, g + shade), max(0, b + shade), 255))
    gold = (222, 178, 70, 255)
    dark = (84, 12, 18, 255)
    for i in range(16):
        px(img, i, 0, gold)
        px(img, i, 15, gold)
        px(img, 0, i, gold)
        px(img, 15, i, gold)
        px(img, i, 1, dark)
        px(img, 1, i, dark)
    # a small crown / star emblem in the middle
    emblem = [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (6, 7), (7, 7), (8, 7), (9, 7),
              (7, 8), (8, 8), (6, 9), (9, 9), (5, 10), (10, 10)]
    for x, y in emblem:
        px(img, x, y, gold)
    for x in range(5, 11):
        px(img, x, 12, gold)
    return img


def pole():
    img = Image.new("RGBA", (16, 16), (92, 62, 34, 255))
    for y in range(16):
        for x in range(16):
            n = ((x * 5 + y * 3) % 4) - 1
            r, g, b, _ = img.getpixel((x, y))
            img.putpixel((x, y), (max(0, r + n * 6), max(0, g + n * 4), max(0, b + n * 3), 255))
    for x in range(16):
        px(img, x, 0, (60, 40, 22, 255))
    return img


def board():
    img = Image.new("RGBA", (16, 16), (150, 112, 62, 255))
    for y in range(16):
        for x in range(16):
            n = ((x * 3 + y * 7) % 5) - 2
            r, g, b, _ = img.getpixel((x, y))
            img.putpixel((x, y), (max(0, r + n * 4), max(0, g + n * 3), max(0, b + n * 2), 255))
    paper = (226, 214, 176, 255)
    ink = (70, 50, 30, 255)
    for (x0, y0, w, h) in [(1, 2, 6, 6), (9, 1, 6, 7), (2, 9, 5, 6), (9, 10, 6, 5)]:
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                px(img, x, y, paper)
        for y in range(y0 + 1, y0 + h - 1, 2):
            for x in range(x0 + 1, x0 + w - 1):
                px(img, x, y, ink)
        px(img, x0 + w // 2, y0, (180, 40, 40, 255))
    for i in range(16):
        px(img, i, 0, (96, 66, 34, 255))
        px(img, i, 15, (96, 66, 34, 255))
        px(img, 0, i, (96, 66, 34, 255))
        px(img, 15, i, (96, 66, 34, 255))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    cloth().save(os.path.join(OUT, "nation_banner_cloth.png"))
    pole().save(os.path.join(OUT, "nation_banner_pole.png"))
    board().save(os.path.join(OUT, "contract_board_side.png"))
    print("wrote 3 textures to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
