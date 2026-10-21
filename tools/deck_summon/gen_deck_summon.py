"""gen_deck_summon: the texture atlas of the Deck of Many Things card summon (client effect `DeckSummonLayer`, release 1.110.0).

    python3 -B tools/deck_summon/gen_deck_summon.py        writes src/main/resources/assets/nusmp/textures/particle/deck_summon.png (512 x 256, 8 x 4 cells of 64)

Cells: 0..21 the faces of the 22 cards (colour and motif from the card's alignment and name, never a picture of the card), 22 the card back, 23 the gold rim, 24 a light streak,
25 a shockwave ring, 26 the column of light. The effect cells are white with their alpha as the light (the vertex colour tints them).
"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle", "deck_summon.png")
CARDS = [("Balance", 1), ("Comet", 0), ("The Donjon", 2), ("Euryale", 2), ("The Fates", 0), ("Flames", 2), ("The Fool", 2), ("Gem", 0), ("Idiot", 2), ("Jester", 0), ("Key", 0),
         ("Knight", 0), ("Moon", 0), ("Rogue", 2), ("Ruin", 2), ("Skull", 2), ("Star", 0), ("Sun", 0), ("Talons", 2), ("Throne", 0), ("Vizier", 0), ("The Void", 2)]
BASE = {0: ((18, 52, 34), (255, 216, 112), (120, 240, 150)), 1: ((14, 42, 58), (190, 240, 255), (110, 216, 255)), 2: ((46, 12, 30), (170, 90, 230), (232, 60, 100))}
ROMAN = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX", "XXI", "XXII"]
S = 4          # supersampling
CS = 64


def font(n):
    try:
        return ImageFont.load_default(size=n)
    except Exception:
        return ImageFont.load_default()


def motif(d, k, cx, cy, r, col):
    w = max(2, int(r * 0.12))
    if k == 0:      # star
        pts = [(cx + (r if i % 2 == 0 else r * 0.45) * math.cos(math.pi / 5 * i - math.pi / 2), cy + (r if i % 2 == 0 else r * 0.45) * math.sin(math.pi / 5 * i - math.pi / 2)) for i in range(10)]
        d.polygon(pts, fill=col)
    elif k == 1:    # sun
        d.ellipse([cx - r * 0.5, cy - r * 0.5, cx + r * 0.5, cy + r * 0.5], fill=col)
        for i in range(12):
            a = math.pi / 6 * i
            d.line([cx + r * 0.62 * math.cos(a), cy + r * 0.62 * math.sin(a), cx + r * math.cos(a), cy + r * math.sin(a)], fill=col, width=w)
    elif k == 2:    # moon
        d.ellipse([cx - r * 0.8, cy - r * 0.8, cx + r * 0.8, cy + r * 0.8], fill=col)
        d.ellipse([cx - r * 0.35, cy - r * 0.95, cx + r * 1.25, cy + r * 0.65], fill=(0, 0, 0, 0))
    elif k == 3:    # key
        d.ellipse([cx - r * 0.55, cy - r * 0.9, cx + r * 0.05, cy - r * 0.3], outline=col, width=w)
        d.line([cx - r * 0.25, cy - r * 0.3, cx - r * 0.25, cy + r * 0.9], fill=col, width=w)
        d.line([cx - r * 0.25, cy + r * 0.6, cx + r * 0.2, cy + r * 0.6], fill=col, width=w)
        d.line([cx - r * 0.25, cy + r * 0.9, cx + r * 0.2, cy + r * 0.9], fill=col, width=w)
    elif k == 4:    # skull
        d.ellipse([cx - r * 0.7, cy - r * 0.9, cx + r * 0.7, cy + r * 0.4], fill=col)
        d.rectangle([cx - r * 0.4, cy + r * 0.2, cx + r * 0.4, cy + r * 0.8], fill=col)
        for sx in (-1, 1):
            d.ellipse([cx + sx * r * 0.3 - r * 0.2, cy - r * 0.35, cx + sx * r * 0.3 + r * 0.2, cy + r * 0.05], fill=(0, 0, 0, 255))
    elif k == 5:    # flame
        d.polygon([(cx, cy - r), (cx + r * 0.55, cy - r * 0.1), (cx + r * 0.4, cy + r * 0.8), (cx, cy + r * 0.45), (cx - r * 0.4, cy + r * 0.8), (cx - r * 0.55, cy - r * 0.1)], fill=col)
    elif k == 6:    # scales
        d.line([cx, cy - r * 0.9, cx, cy + r * 0.9], fill=col, width=w)
        d.line([cx - r * 0.9, cy - r * 0.5, cx + r * 0.9, cy - r * 0.5], fill=col, width=w)
        for sx in (-1, 1):
            d.polygon([(cx + sx * r * 0.9, cy - r * 0.5), (cx + sx * r * 0.45, cy + r * 0.2), (cx + sx * r * 1.0, cy + r * 0.2)], fill=col)
    else:           # a ringed void
        d.ellipse([cx - r * 0.9, cy - r * 0.9, cx + r * 0.9, cy + r * 0.9], outline=col, width=w)
        d.ellipse([cx - r * 0.4, cy - r * 0.4, cx + r * 0.4, cy + r * 0.4], fill=col)


def face(i):
    name, mood = CARDS[i]
    dark, rim, glow = BASE[mood]
    im = Image.new("RGBA", (CS * S, CS * S), dark + (255,))
    d = ImageDraw.Draw(im)
    for y in range(CS * S):                                  # a vertical glow of the alignment colour
        t = y / (CS * S)
        c = tuple(int(dark[k] + (glow[k] - dark[k]) * 0.35 * (1 - abs(t - 0.45) * 2)) for k in range(3))
        d.line([0, y, CS * S, y], fill=c + (255,))
    d.rectangle([2 * S, 2 * S, (CS - 3) * S, (CS - 3) * S], outline=rim + (255,), width=3 * S // 2)
    d.rectangle([6 * S, 6 * S, (CS - 7) * S, (CS - 7) * S], outline=glow + (255,), width=S // 2)
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    motif(ImageDraw.Draw(layer), (i * 5 + mood) % 8, CS * S / 2, CS * S * 0.46, CS * S * 0.26, rim + (255,))
    im = Image.alpha_composite(im, layer)
    d = ImageDraw.Draw(im)
    t = ROMAN[i]
    f = font(9 * S)
    w = d.textlength(t, font=f)
    d.text(((CS * S - w) / 2, (CS - 17) * S), t, fill=rim + (255,), font=f)
    return im.resize((CS, CS), Image.LANCZOS)


def back():
    im = Image.new("RGBA", (CS * S, CS * S), (24, 20, 52, 255))
    d = ImageDraw.Draw(im)
    for k in range(-CS, 2 * CS, 8):
        d.line([k * S, 0, (k + CS) * S, CS * S], fill=(110, 90, 190, 255), width=S // 2 + 1)
        d.line([k * S, CS * S, (k + CS) * S, 0], fill=(110, 90, 190, 255), width=S // 2 + 1)
    d.rectangle([2 * S, 2 * S, (CS - 3) * S, (CS - 3) * S], outline=(232, 190, 90, 255), width=3 * S // 2)
    pts = [(CS * S / 2 + (CS * S * (0.2 if k % 2 else 0.09)) * math.cos(math.pi / 4 * k), CS * S / 2 + (CS * S * (0.2 if k % 2 else 0.09)) * math.sin(math.pi / 4 * k)) for k in range(8)]
    d.polygon(pts, fill=(232, 190, 90, 255))
    return im.resize((CS, CS), Image.LANCZOS)


def rim():
    im = Image.new("RGBA", (CS, CS), (232, 190, 80, 255))
    d = ImageDraw.Draw(im)
    d.line([0, CS // 2, CS, CS // 2], fill=(255, 238, 160, 255), width=6)
    return im


def streak():
    im = Image.new("RGBA", (CS, CS), (255, 255, 255, 0))
    px = im.load()
    for y in range(CS):
        for x in range(CS):
            u = abs(x - CS / 2) / (CS / 2)          # across the streak
            v = y / (CS - 1)                         # along it: bright at the root
            a = max(0.0, 1 - u) ** 2.2 * (1 - v) ** 1.2
            px[x, y] = (255, 255, 255, int(255 * a))
    return im


def ring():
    im = Image.new("RGBA", (CS, CS), (255, 255, 255, 0))
    px = im.load()
    for y in range(CS):
        for x in range(CS):
            r = math.hypot(x - CS / 2 + 0.5, y - CS / 2 + 0.5) / (CS / 2)
            a = max(0.0, 1 - abs(r - 0.82) / 0.16) if r < 1.0 else 0.0
            px[x, y] = (255, 255, 255, int(255 * min(1.0, a * a * 1.4)))
    return im


def column():
    im = Image.new("RGBA", (CS, CS), (255, 255, 255, 0))
    px = im.load()
    for y in range(CS):
        for x in range(CS):
            u = abs(x - CS / 2 + 0.5) / (CS / 2)
            v = y / (CS - 1)                         # 0 top .. 1 bottom (the foot)
            scan = 0.78 + 0.22 * (1 if (y // 2) % 2 == 0 else 0)       # hologram scan lines
            a = (1 - u) ** 1.5 * (v ** 0.8) * (1 - v * 0.15) * scan
            px[x, y] = (255, 255, 255, int(255 * min(1.0, a)))
    return im


def main():
    atlas = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    cells = [face(i) for i in range(22)] + [back(), rim(), streak(), ring(), column()]
    for i, c in enumerate(cells):
        atlas.paste(c, ((i % 8) * CS, (i // 8) * CS))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    atlas.save(OUT, optimize=True)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
