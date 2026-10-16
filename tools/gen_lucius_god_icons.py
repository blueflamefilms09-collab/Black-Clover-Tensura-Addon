"""Draws the three 18x18 mob effect icons of the god-tier Lucius (textures/mob_effect/{relic_nullified,severed,time_decay}.png).

Why a script and not hand-made files: each icon is the player's only sign of what was done to them (relics silenced, Djinn and spirits
cut off, healing stopped), so each has to read at 18 pixels at a glance, and a script keeps the palettes in one place, the same as the
other mod icons. relic_nullified: an ice-blue ring with a gem and a slash through it. severed: a violet wisp cut by a bright line.
time_decay: a gold hourglass with a crack. Run from the repository root: python tools/gen_lucius_god_icons.py
"""
from pathlib import Path
from PIL import Image

OUT = Path("src/main/resources/assets/nusmp/textures/mob_effect")
EDGE = (14, 10, 26, 255)

RING = [
    "..................",
    "......xxxxxx......",
    "....xxxxxxxxxx....",
    "...xxx......xxx...",
    "..xxx........xxx..",
    "..xx....gg....xx..",
    ".xxx...gggg...xxx.",
    ".xx....gggg....xx.",
    ".xx.....gg.....xx.",
    ".xxx..........xxx.",
    "..xxx........xxx..",
    "..xxxx......xxxx..",
    "...xxxxxxxxxxxx...",
    "....xxxxxxxxxx....",
    "......xxxxxx......",
    "..................",
    "..................",
    "..................",
]

WISP = [
    "..................",
    ".......xx.........",
    "......xxxx........",
    ".....xxxxx........",
    ".....xxxx.........",
    "......xxx.........",
    ".......xxx........",
    "........xxx.......",
    "........xxxx......",
    ".......xxxxx......",
    ".......xxxxx......",
    "........xxxx......",
    ".........xx.......",
    "..................",
    "..................",
    "..................",
    "..................",
    "..................",
]

GLASS = [
    "..................",
    "...xxxxxxxxxxxx...",
    "...xxxxxxxxxxxx...",
    "....x........x....",
    ".....x......x.....",
    "......x....x......",
    ".......x..x.......",
    "........xx........",
    "........xx........",
    ".......x..x.......",
    "......x.ss.x......",
    ".....x.ssss.x.....",
    "....x.ssssss.x....",
    "...xxxxxxxxxxxx...",
    "...xxxxxxxxxxxx...",
    "..................",
    "..................",
    "..................",
]


def put(grid, palette, shade=None):
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c in palette:
                px[x, y] = palette[c]
    for y in range(18):                      # dark outline
        for x in range(18):
            if px[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 18 and 0 <= ny < 18 and px[nx, ny][3] and px[nx, ny] != EDGE:
                    px[x, y] = EDGE
                    break
    return im


def line(px, pts, colour):
    for x, y in pts:
        if 0 <= x < 18 and 0 <= y < 18:
            px[x, y] = colour


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    ring = put(RING, {"x": (127, 215, 255, 255), "g": (240, 252, 255, 255)})
    line(ring.load(), [(14 - i, 3 + i) for i in range(0, 12)], (255, 90, 74, 255))      # the slash through it
    ring.save(OUT / "relic_nullified.png")

    wisp = put(WISP, {"x": (176, 124, 255, 255)})
    px = wisp.load()
    for y in range(1, 13):                   # the lighter core of the wisp
        for x in range(18):
            if px[x, y] == (176, 124, 255, 255) and (x + y) % 3 == 0:
                px[x, y] = (226, 205, 255, 255)
    line(px, [(3 + i, 12 - i) for i in range(0, 12)], (255, 255, 255, 255))               # the cut
    wisp.save(OUT / "severed.png")

    glass = put(GLASS, {"x": (216, 166, 74, 255), "s": (255, 225, 140, 255)})
    line(glass.load(), [(9, 5), (8, 6), (9, 7), (8, 8)], EDGE)                            # the crack
    glass.save(OUT / "time_decay.png")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
