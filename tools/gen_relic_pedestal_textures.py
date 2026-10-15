"""Textures of the Relic Pedestal (the Labyrinth's Treasure Vault, 1.35.0). Deterministic, Pillow only.

    python3 -B tools/gen_relic_pedestal_textures.py          -> src/main/resources/assets/nusmp/textures/block/relic_pedestal*.png
    python3 -B tools/gen_relic_pedestal_textures.py --check  -> exit 1 if a shipped PNG differs from what this script makes

    relic_pedestal       one vertical slice of the pedestal, read by the block model's UVs row by row:
                         rows 0-1 spare, 2-4 the top plate (gold trim band), 5-13 the column (dark stone, a gold rune line), 14-15 the base
    relic_pedestal_top   the top faces: dark stone with a gold ring; the plate uses its centre 12x12, the base the full 16x16

Why one slice: the model's three elements map their side faces to the rows of the same texture at the same heights (uv [3,5,13,14] for the
column and so on), so the trim lines meet across element edges and the whole pedestal reads as one carved piece from any side.
"""
import os
import sys

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "block")

STONE = [(38, 36, 44), (44, 42, 51), (33, 31, 39), (49, 47, 56)]       # dark stone, four shades
GOLD = (214, 172, 72)
GOLD_DARK = (150, 112, 38)
GOLD_LIGHT = (240, 208, 120)
GLYPH = (96, 196, 220)                                                 # a faint magicule cyan in the rune line


def hashed(x, y, salt=0):
    """A small deterministic hash for the stone grain (no random module, so the file is byte-stable)."""
    h = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    h = (h ^ (h >> 13)) * 0x5BD1E995 & 0xFFFFFFFF
    return (h ^ (h >> 15)) & 0xFFFFFFFF


def stone(x, y, salt=0):
    return STONE[hashed(x, y, salt) % len(STONE)]


def side():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = stone(x, y, 1) + (255,)
    # top plate rows 3-4 (row 2 is the plate's upper edge): a gold band with a darker seam below
    for x in range(16):
        px[x, 2] = GOLD_LIGHT + (255,)
        px[x, 3] = GOLD + (255,)
        px[x, 4] = GOLD_DARK + (255,)
    # column rows 5-13: a gold rune line at row 9 with cyan glyph dots, gold fillets at rows 5 and 13
    for x in range(16):
        px[x, 5] = GOLD_DARK + (255,)
        px[x, 13] = GOLD_DARK + (255,)
        px[x, 9] = GOLD + (255,) if x % 3 != 1 else GLYPH + (255,)
    # base rows 14-15: a gold edge on top of the base
    for x in range(16):
        px[x, 14] = GOLD + (255,)
        px[x, 15] = stone(x, 15, 2) + (255,)
    # rows 0-1 are never sampled by the model: keep them stone so a mipmap edge never bleeds colour
    return im


def top():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = stone(x, y, 3) + (255,)
    # the base's outer gold rim (the plate's 12x12 centre is rows/cols 2..13, so its own rim is the ring at 2 and 13)
    for i in range(16):
        for j in (0, 15):
            px[i, j] = GOLD_DARK + (255,)
            px[j, i] = GOLD_DARK + (255,)
        for j in (2, 13):
            px[i, j] = GOLD + (255,) if 2 <= i <= 13 else px[i, j]
            px[j, i] = GOLD + (255,) if 2 <= i <= 13 else px[j, i]
    # a small gold diamond in the centre where the relic floats, with a cyan heart
    for dx, dy in ((0, -2), (-1, -1), (0, -1), (1, -1), (-2, 0), (-1, 0), (1, 0), (2, 0), (-1, 1), (0, 1), (1, 1), (0, 2)):
        px[7 + dx, 7 + dy] = GOLD + (255,)
        px[8 + dx, 8 + dy] = GOLD + (255,)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = GLYPH + (255,)
    return im


def main(argv):
    check = "--check" in argv
    made = {"relic_pedestal.png": side(), "relic_pedestal_top.png": top()}
    bad = 0
    for name, im in made.items():
        path = os.path.join(OUT, name)
        if check:
            if not os.path.exists(path):
                print("missing: " + path)
                bad += 1
                continue
            have = Image.open(path).convert("RGBA")
            if have.size != im.size or have.tobytes() != im.tobytes():
                print("differs: " + path)
                bad += 1
        else:
            os.makedirs(OUT, exist_ok=True)
            im.save(path, optimize=True)
            print("wrote " + path)
    if check:
        print("ok" if bad == 0 else "%d problem(s)" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
