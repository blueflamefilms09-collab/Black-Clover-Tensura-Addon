"""Draws the 18x18 mob effect icon of Soul Corruption (textures/mob_effect/soul_corruption.png).

Why a script and not a hand-made file: the icon is the player's only sign of the debuff, so it has to read at 18 pixels at a glance
(a cracked violet heart with a pale soul wisp leaving it, the cut of maximum health shown as a notch at the top), and a script keeps
the palette in one place, the same as the other mod icons. Run from the repository root: python tools/gen_lucius_arc_icons.py
"""
from pathlib import Path
from PIL import Image

OUT = Path("src/main/resources/assets/nusmp/textures/mob_effect/soul_corruption.png")

HEART = [
    "..................",
    "..................",
    "....xxxx..xxxx....",
    "...xxxxxxxxxxxx...",
    "..xxxxxxxxxxxxxx..",
    "..xxxxxxxxxxxxxx..",
    "..xxxxxxxxxxxxxx..",
    "..xxxxxxxxxxxxxx..",
    "...xxxxxxxxxxxx...",
    "....xxxxxxxxxx....",
    ".....xxxxxxxx.....",
    "......xxxxxx......",
    ".......xxxx.......",
    "........xx........",
    "..................",
    "..................",
    "..................",
    "..................",
]

DEEP, MID, LIGHT, EDGE = (59, 15, 107, 255), (107, 63, 160, 255), (155, 91, 255, 255), (22, 6, 40, 255)
WISP, GOLD = (240, 232, 255, 255), (232, 185, 35, 255)


def main() -> None:
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(HEART):
        for x, c in enumerate(row):
            if c == "x":
                px[x, y] = MID if (x + y) % 5 else LIGHT
    for y in range(18):                      # shade the lower right, light the upper left
        for x in range(18):
            if px[x, y][3] and (x + y > 20):
                px[x, y] = DEEP
    for y in range(18):                      # dark outline
        for x in range(18):
            if px[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 18 and 0 <= ny < 18 and px[nx, ny][3] and px[nx, ny] != EDGE:
                    px[x, y] = EDGE
                    break
    crack = [(9, 3), (8, 4), (9, 5), (8, 6), (9, 7), (8, 8), (7, 9)]
    for x, y in crack:
        px[x, y] = EDGE
    for i, (x, y) in enumerate([(12, 6), (13, 5), (13, 4), (14, 3), (14, 2)]):   # the soul leaving
        px[x, y] = GOLD if i == 4 else WISP
    OUT.parent.mkdir(parents=True, exist_ok=True)
    im.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
