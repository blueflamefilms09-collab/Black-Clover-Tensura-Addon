#!/usr/bin/env python3
"""Generates the small emblems the Double Magic skill icons are composed from at run time (docs/devil_double_magic.md). Deterministic, idempotent, PIL only.

    python3 tools/gen_devil_emblems.py

Writes to src/main/resources/assets/anime_multiverse/textures/skill/:
  emblem/magic/<key>.png   16x16 white alpha masks, one for EVERY grimoire MagicType (the keys are read from blackclover/MagicType.java, lower case)
  emblem/devil/<key>.png   16x16 white alpha masks, one for EVERY devil attribute except NONE (read from devil/DevilAttribute.java), with two small horns on top
  devil_magic.png, devil_bond.png   32x32 still icons of the two skills (the game swaps them for the animated, composed ones while a contract is held)
and the preview sheet docs/previews/devil/emblems_sheet.png (not part of the mod).

An emblem is the motif of the magic's SOUL family (flame teardrop, water drop, wind swirl, earth crystal, light star, dark crescent, space orbit, time clock, battle blades, fantasy sparkle, empty
slashed circle) plus a few satellite dots and ring arcs that come from a hash of the key, so no two emblems are the same picture. The client tints a mask with the magic's or the attribute's colour and
composes the animated icon from them (skill/icons/EmblemComposer.java): nothing here is animated, and nothing is composed at build time except the still icons.
"""
import hashlib
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/anime_multiverse/textures/skill"
PREV = ROOT / "docs/previews/devil"
SS = 8                                   # supersampling: draw at 128, reduce to 16
N = 16 * SS

SOUL_OF_DEVIL = {"GRAVITY": "SPACE", "SPATIAL": "SPACE", "CURSE": "DARKNESS", "ANTI_MAGIC": "EMPTY", "FLAME": "FLAME", "WORD": "FANTASY", "TIME": "TIME", "BEAST": "BATTLE",
                 "FORESIGHT": "FANTASY", "GHOST": "DARKNESS", "SOUL": "DARKNESS", "ROT": "EARTH", "EARTH": "EARTH", "WATER": "WATER", "LIGHT": "LIGHT", "ETHER": "WIND", "CAUSALITY": "TIME"}


def magic_keys():
    src = (ROOT / "src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java").read_text(encoding="utf-8")
    return [(m.group(1).lower(), m.group(4)) for m in re.finditer(r'^    ([A-Z_]+)\("([^"]*)", "([^"]*)", "([A-Z_]+)"', src, re.M)]


def devil_keys():
    src = (ROOT / "src/main/java/com/newuniverse/nusmp/devil/DevilAttribute.java").read_text(encoding="utf-8")
    body = src.split("public enum DevilAttribute", 1)[1].split(";", 1)[0]
    names = re.findall(r"\b([A-Z][A-Z_]+)\b", re.sub(r"/\*.*?\*/", "", body, flags=re.S))
    return [(n.lower(), SOUL_OF_DEVIL[n]) for n in names if n != "NONE"]


def h(key):
    return int.from_bytes(hashlib.sha256(key.encode()).digest()[:8], "big")


def P(x, y):
    """Coordinates in 16 px units, centre (8, 8), to the supersampled canvas."""
    return (x * SS, y * SS)


def motif(d, soul):
    W = 255
    c = 8
    if soul == "FLAME":
        d.polygon([P(8, 2), P(11.5, 6.5), P(11.8, 10), P(10, 12.8), P(8, 13.6), P(6, 12.8), P(4.2, 10), P(4.6, 7), P(6.4, 8.4), P(6.6, 5.5)], fill=W)
        d.polygon([P(8, 8.6), P(9.4, 10.6), P(8, 12), P(6.6, 10.6)], fill=0)
    elif soul == "WATER":
        d.polygon([P(8, 2.2), P(11.6, 8.2), P(4.4, 8.2)], fill=W)
        d.ellipse([P(4.4, 6.0), P(11.6, 13.4)], fill=W)
        d.arc([P(5.6, 8.4), P(10.4, 12.4)], 20, 160, fill=0, width=SS)
    elif soul == "WIND":
        for r, a0 in ((4.6, 200), (3.2, 40), (1.8, 220)):
            d.arc([P(8 - r, 8 - r), P(8 + r, 8 + r)], a0, a0 + 250, fill=W, width=int(SS * 1.15))
        d.ellipse([P(7.4, 7.4), P(8.6, 8.6)], fill=W)
    elif soul == "EARTH":
        d.polygon([P(8, 2), P(12.4, 6.4), P(10.6, 13.6), P(5.4, 13.6), P(3.6, 6.4)], fill=W)
        d.line([P(8, 2), P(7, 13.6)], fill=0, width=SS // 2)
        d.line([P(3.6, 6.4), P(12.4, 6.4)], fill=0, width=SS // 2)
    elif soul == "LIGHT":
        for k in range(8):
            a = math.pi / 4 * k
            r1, r2 = 2.4, 6.4 if k % 2 == 0 else 4.6
            d.line([P(c + r1 * math.cos(a), c + r1 * math.sin(a)), P(c + r2 * math.cos(a), c + r2 * math.sin(a))], fill=W, width=int(SS * 1.2))
        d.ellipse([P(5.6, 5.6), P(10.4, 10.4)], fill=W)
    elif soul == "DARKNESS":
        d.ellipse([P(3.4, 3.2), P(12.6, 12.4)], fill=W)
        d.ellipse([P(6.2, 2.2), P(14.2, 11.2)], fill=0)
        d.ellipse([P(7.6, 6.6), P(8.8, 7.8)], fill=W)
    elif soul == "SPACE":
        d.ellipse([P(2.8, 5.4), P(13.2, 10.6)], outline=W, width=int(SS * 1.1))
        d.ellipse([P(5.6, 5.6), P(10.4, 10.4)], fill=W)
        d.ellipse([P(11.2, 6.2), P(13.2, 8.2)], fill=W)
        d.ellipse([P(7.0, 7.0), P(9.0, 9.0)], fill=0)
    elif soul == "TIME":
        d.ellipse([P(3.0, 3.0), P(13.0, 13.0)], outline=W, width=int(SS * 1.2))
        d.line([P(8, 8), P(8, 4.4)], fill=W, width=int(SS * 1.1))
        d.line([P(8, 8), P(10.8, 9.6)], fill=W, width=int(SS * 1.1))
        d.ellipse([P(7.2, 7.2), P(8.8, 8.8)], fill=W)
    elif soul == "BATTLE":
        d.line([P(3.4, 3.4), P(12.6, 12.6)], fill=W, width=int(SS * 1.5))
        d.line([P(12.6, 3.4), P(3.4, 12.6)], fill=W, width=int(SS * 1.5))
        d.line([P(5.4, 8.6), P(8.6, 5.4)], fill=W, width=int(SS * 1.4))
        d.line([P(7.4, 10.6), P(10.6, 7.4)], fill=W, width=int(SS * 1.4))
    elif soul == "FANTASY":
        d.polygon([P(8, 1.8), P(9.4, 6.6), P(14.2, 8), P(9.4, 9.4), P(8, 14.2), P(6.6, 9.4), P(1.8, 8), P(6.6, 6.6)], fill=W)
    else:                                                         # EMPTY: a circle with a slash
        d.ellipse([P(3.0, 3.0), P(13.0, 13.0)], outline=W, width=int(SS * 1.3))
        d.line([P(4.6, 11.4), P(11.4, 4.6)], fill=W, width=int(SS * 1.3))


def emblem(key, soul, devil):
    img = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(img)
    motif(d, soul)
    hv = h(("devil:" if devil else "magic:") + key)
    for i in range(3):                                            # satellites: three dots on the rim, positions from the hash
        a = ((hv >> (i * 9)) % 360) * math.pi / 180.0
        r = 6.9
        cx, cy = 8 + r * math.cos(a), 8 + r * math.sin(a)
        d.ellipse([P(cx - 0.55, cy - 0.55), P(cx + 0.55, cy + 0.55)], fill=255)
    gap = (hv >> 30) % 4                                          # one rim arc, its place from the hash
    d.arc([P(1.4, 1.4), P(14.6, 14.6)], gap * 90 + 10, gap * 90 + 70, fill=255, width=int(SS * 0.8))
    if devil:                                                     # two small horns on top
        d.polygon([P(3.8, 4.0), P(3.2, 0.6), P(5.6, 3.0)], fill=255)
        d.polygon([P(12.2, 4.0), P(12.8, 0.6), P(10.4, 3.0)], fill=255)
    small = img.resize((16, 16), Image.LANCZOS)
    out = Image.new("RGBA", (16, 16), (255, 255, 255, 0))
    out.putdata([(255, 255, 255, min(255, int(v * 1.15))) for v in small.get_flattened_data()])
    return out


def still(rgb, mark, bond):
    """The 32x32 still icon (the static skill icon): a dark disc, a ring in the colour, the mark."""
    S = 32 * 8
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([8, 8, S - 8, S - 8], fill=(14, 8, 20, 255), outline=rgb + (255,), width=14)
    d.ellipse([40, 40, S - 40, S - 40], outline=tuple(int(c * 0.55) for c in rgb) + (255,), width=5)
    cx = S // 2
    if bond:                                                      # two linked rings
        d.ellipse([cx - 70, cx - 52, cx + 18, cx + 36], outline=rgb + (255,), width=12)
        d.ellipse([cx - 18, cx - 36, cx + 70, cx + 52], outline=(255, 235, 235, 255), width=12)
    else:                                                         # a horned head
        d.polygon([(cx - 60, cx - 10), (cx - 78, cx - 74), (cx - 24, cx - 36)], fill=rgb + (255,))
        d.polygon([(cx + 60, cx - 10), (cx + 78, cx - 74), (cx + 24, cx - 36)], fill=rgb + (255,))
        d.ellipse([cx - 52, cx - 40, cx + 52, cx + 64], fill=(255, 235, 225, 255))
        d.ellipse([cx - 30, cx - 6, cx - 12, cx + 12], fill=rgb + (255,))
        d.ellipse([cx + 12, cx - 6, cx + 30, cx + 12], fill=rgb + (255,))
        d.polygon([(cx - 14, cx + 34), (cx + 14, cx + 34), (cx, cx + 50)], fill=(60, 10, 20, 255))
    return im.resize((32, 32), Image.LANCZOS)


def main():
    (RES / "emblem/magic").mkdir(parents=True, exist_ok=True)
    (RES / "emblem/devil").mkdir(parents=True, exist_ok=True)
    PREV.mkdir(parents=True, exist_ok=True)
    mk, dk = magic_keys(), devil_keys()
    assert len(mk) >= 100 and len(set(k for k, _ in mk)) == len(mk), len(mk)
    assert len(dk) == 17, dk
    seen = {}
    made = []
    for devil, keys in ((False, mk), (True, dk)):
        for key, soul in keys:
            im = emblem(key, soul, devil)
            sig = bytes(im.tobytes())
            assert sig not in seen, "emblem %s is the same picture as %s" % (key, seen.get(sig))
            seen[sig] = ("devil/" if devil else "magic/") + key
            im.save(RES / ("emblem/%s/%s.png" % ("devil" if devil else "magic", key)))
            made.append((devil, key, im))
    still((0xC8, 0x1E, 0x2D), True, False).save(RES / "devil_magic.png")
    still((0x7A, 0x3C, 0xC8), True, True).save(RES / "devil_bond.png")
    cols = 16
    rows = (len(made) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * 56 + 8, rows * 56 + 8), (0x24, 0x26, 0x30))
    for i, (devil, key, im) in enumerate(made):
        tint = Image.new("RGBA", im.size, (255, 120, 90, 255) if devil else (150, 200, 255, 255))
        tint.putalpha(im.getchannel("A"))
        big = tint.resize((48, 48), Image.NEAREST)
        sheet.paste(big, (8 + (i % cols) * 56, 8 + (i // cols) * 56), big)
    sheet.save(PREV / "emblems_sheet.png")
    print("emblems: %d magic + %d devil, all different; still icons devil_magic.png and devil_bond.png" % (len(mk), len(dk)))


if __name__ == "__main__":
    main()
