#!/usr/bin/env python3
"""
Textures of the Labyrinth's seals (docs/labyrinth/e2_seal.md): the Gatekeeper Door (closed warden/boss faces, the open frame), the Peak
Altar, the Shadow Key item and the Floor Warden's skin (a violet re-tint of the Living Armor sheet). Pillow only, deterministic (a seeded
PRNG for the stone grain), idempotent: running it again rewrites the same bytes.

    python3 tools/gen_labyrinth_seal_assets.py            # write every texture
    python3 tools/gen_labyrinth_seal_assets.py --check    # exit 1 unless every texture exists with its expected size
"""
import argparse
import json
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures")

VIOLET = (138, 77, 224)
CRIMSON = (196, 22, 28)
IRON = (54, 50, 62)
IRON_DARK = (34, 30, 40)
IRON_LIGHT = (84, 80, 94)
GOLD = (214, 170, 60)
GOLD_DARK = (150, 110, 30)
STONE = (46, 42, 54)
STONE_DARK = (30, 27, 36)
STONE_LIGHT = (66, 62, 76)

# published file -> (width, height); the two animated door faces hold 4 frames stacked
OUTPUTS = {
    "block/gatekeeper_door_warden_bottom.png": (16, 64),
    "block/gatekeeper_door_warden_top.png": (16, 16),
    "block/gatekeeper_door_boss_bottom.png": (16, 64),
    "block/gatekeeper_door_boss_top.png": (16, 16),
    "block/gatekeeper_door_end.png": (16, 16),
    "block/gatekeeper_door_frame.png": (16, 16),
    "block/peak_altar_top.png": (16, 16),
    "block/peak_altar_side.png": (16, 16),
    "item/shadow_key.png": (16, 16),
    "entity/floor_warden.png": (64, 64),
}
ANIMATED = {"block/gatekeeper_door_warden_bottom.png": 6, "block/gatekeeper_door_boss_bottom.png": 6}


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def grain(rng, base, amount=6):
    return tuple(max(0, min(255, c + rng.randint(-amount, amount))) for c in base)


def put(img, x, y, rgb, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), (rgb[0], rgb[1], rgb[2], a))


def iron_plate(rng, w=16, h=16):
    """A dark riveted iron plate with a lighter inner border: the body of every door face."""
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            put(img, x, y, grain(rng, IRON, 5))
    for i in range(w):
        put(img, i, 0, IRON_LIGHT); put(img, i, h - 1, IRON_DARK)
    for i in range(h):
        put(img, 0, i, IRON_LIGHT); put(img, w - 1, i, IRON_DARK)
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        put(img, x, y, GOLD); put(img, x + 1, y, GOLD_DARK); put(img, x, y + 1, GOLD_DARK)
    return img


def seal_frame(colour, phase):
    """One 16x16 frame of the sealed lower face: the iron plate with a glowing circle-and-glyph seal pulsing between phases."""
    rng = random.Random(11)
    img = iron_plate(rng)
    glow = 0.55 + 0.45 * math.sin(phase * math.pi * 2)
    bright = mix(colour, (255, 255, 255), 0.35 * glow)
    dim = mix(IRON, colour, 0.45 + 0.3 * glow)
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 4.6 <= d <= 5.6:
                put(img, x, y, mix(colour, bright, glow))              # the ring
            elif d < 4.6 and (abs(x - 7.5) < 0.8 or abs(y - 7.5) < 0.8):
                put(img, x, y, mix(colour, bright, 0.6 * glow))        # the cross glyph
            elif d < 4.6:
                put(img, x, y, dim)                                     # the lit disc
    put(img, 7, 7, bright); put(img, 8, 7, bright); put(img, 7, 8, bright); put(img, 8, 8, bright)
    return img


def door_bottom(colour):
    sheet = Image.new("RGBA", (16, 64))
    for f in range(4):
        sheet.paste(seal_frame(colour, f / 4.0), (0, f * 16))
    return sheet


def door_top(colour):
    """The upper face: the plate with a pointed arch of gold studs and a hanging chain in the seal's colour."""
    rng = random.Random(23)
    img = iron_plate(rng)
    arch = [(7, 3), (8, 3), (6, 4), (9, 4), (5, 5), (10, 5), (4, 6), (11, 6), (3, 7), (12, 7), (3, 8), (12, 8), (3, 9), (12, 9)]
    for (x, y) in arch:
        put(img, x, y, GOLD)
    for y in range(10, 16):
        put(img, 7, y, mix(colour, IRON_LIGHT, 0.4)); put(img, 8, y, mix(colour, IRON_DARK, 0.4))
    put(img, 7, 9, mix(colour, (255, 255, 255), 0.3)); put(img, 8, 9, mix(colour, (255, 255, 255), 0.3))
    return img


def stone(seed, base=STONE, dark=STONE_DARK, light=STONE_LIGHT, bricks=True):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            put(img, x, y, grain(rng, base, 5))
    if bricks:
        for y in (0, 8):
            for x in range(16):
                put(img, x, y, dark)
        for y in range(16):
            put(img, (0 if y < 8 else 8), y, dark)
        for y in (1, 9):
            for x in range(16):
                if img.getpixel((x, y))[:3] != dark:
                    put(img, x, y, light)
    return img


def door_end():
    img = stone(31)
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        put(img, x, y, GOLD)
    return img


def door_frame():
    return stone(37, bricks=False)


def altar_top():
    """Obsidian-dark top with a violet rune ring and a bright centre: where the boss stands."""
    img = stone(41, base=(28, 22, 40), dark=(16, 12, 24), light=(44, 36, 60), bricks=False)
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 5.2 <= d <= 6.2 or (2.4 <= d <= 3.2):
                put(img, x, y, VIOLET)
            elif d < 1.6:
                put(img, x, y, mix(VIOLET, (255, 255, 255), 0.6))
    for (x, y) in [(7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)]:
        put(img, x, y, GOLD)
    return img


def altar_side():
    img = stone(43, base=(36, 30, 50), dark=(20, 16, 28), light=(56, 48, 72))
    for x in range(16):
        put(img, x, 4, GOLD_DARK); put(img, x, 5, GOLD)
    for x in range(2, 14, 3):
        put(img, x, 11, VIOLET)
    return img


def shadow_key():
    """A black-violet key: a round bow holding a glowing eye, a shaft, two teeth. Drawn on a transparent 16x16."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body = (28, 18, 44); edge = (70, 44, 110); hi = (120, 80, 190)
    cx, cy = 5.5, 5.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 2.6 <= d <= 4.3:
                put(img, x, y, edge if d > 3.7 else body)
    for (x, y) in [(5, 5), (6, 5), (5, 6), (6, 6)]:
        put(img, x, y, VIOLET)
    put(img, 5, 5, (240, 220, 255)); put(img, 6, 6, (80, 230, 255))
    for i in range(8):                                                  # the shaft, diagonal to the bottom right
        x, y = 8 + i, 8 + i
        put(img, x, y, body); put(img, x + 1, y, edge); put(img, x, y + 1, hi if i % 2 else body)
    for (x, y) in [(13, 9), (14, 9), (14, 8), (12, 11), (13, 11)]:   # teeth
        put(img, x, y, edge)
    return img


def warden_skin():
    """The Living Armor sheet re-tinted toward dark violet, alpha kept: the Warden is the same creature grown on magicules."""
    src = Image.open(os.path.join(TEX, "entity", "living_armor.png")).convert("RGBA")
    out = Image.new("RGBA", src.size)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0
            tint = (int(18 + 120 * lum), int(10 + 70 * lum), int(40 + 190 * lum))
            out.putpixel((x, y), (min(255, tint[0]), min(255, tint[1]), min(255, tint[2]), a))
    return out


def build():
    return {
        "block/gatekeeper_door_warden_bottom.png": door_bottom(VIOLET),
        "block/gatekeeper_door_warden_top.png": door_top(VIOLET),
        "block/gatekeeper_door_boss_bottom.png": door_bottom(CRIMSON),
        "block/gatekeeper_door_boss_top.png": door_top(CRIMSON),
        "block/gatekeeper_door_end.png": door_end(),
        "block/gatekeeper_door_frame.png": door_frame(),
        "block/peak_altar_top.png": altar_top(),
        "block/peak_altar_side.png": altar_side(),
        "item/shadow_key.png": shadow_key(),
        "entity/floor_warden.png": warden_skin(),
    }


def write():
    for rel, img in build().items():
        path = os.path.join(TEX, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        assert img.size == OUTPUTS[rel], (rel, img.size)
        img.save(path, optimize=True)
        if rel in ANIMATED:
            with open(path + ".mcmeta", "w") as f:
                json.dump({"animation": {"frametime": ANIMATED[rel]}}, f, indent=2)
                f.write("\n")
    print("wrote", len(OUTPUTS), "textures under", os.path.normpath(TEX))


def check():
    bad = []
    for rel, size in OUTPUTS.items():
        path = os.path.join(TEX, rel)
        if not os.path.isfile(path):
            bad.append(rel + ": missing"); continue
        got = Image.open(path).size
        if got != size:
            bad.append("%s: size %s, expected %s" % (rel, got, size))
        if rel in ANIMATED and not os.path.isfile(path + ".mcmeta"):
            bad.append(rel + ".mcmeta: missing")
    for line in bad:
        print(line)
    print("check:", "ok" if not bad else "%d problem(s)" % len(bad))
    return 0 if not bad else 1


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    ap.add_argument("--check", action="store_true", help="verify the textures exist with their expected sizes; write nothing")
    args = ap.parse_args()
    sys.exit(check() if args.check else (write() or 0))
