"""Generate distinct, transparent grimoire cover sigils for the wiki-attribute books.

    python tools/gen_wiki_magic_sigils.py

The small ornamental marks sit around the cover's clover medallion, preserving the
cover-tier emblem while making each newly added magic readable at a glance.
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_book")
SCALE = 4
W, H = 64 * SCALE, 80 * SCALE

MAGICS = {
    "air": ((159, 232, 255), "wind"),
    "hair": ((255, 110, 154), "threads"),
    "memory": ((188, 168, 255), "eye"),
    "mineral": ((120, 168, 200), "gem"),
    "modification": ((255, 155, 88), "arrows"),
    "mucus": ((114, 214, 180), "drops"),
    "mud": ((138, 89, 61), "waves"),
    "nail": ((199, 185, 166), "spikes"),
    "permeation": ((101, 215, 232), "portal"),
    "poison_plant": ((159, 203, 59), "leaf"),
    "red_ochre": ((214, 76, 55), "sun"),
    "rock": ((119, 124, 134), "stone"),
    "sandstone": ((213, 167, 108), "dunes"),
    "scale": ((99, 189, 181), "scales"),
    "shakudo": ((110, 73, 70), "gear"),
    "skin": ((225, 169, 149), "shield"),
    "smoke": ((130, 120, 143), "smoke"),
    "snow": ((231, 247, 255), "snow"),
    "song": ((255, 180, 232), "notes"),
    "soul_corpse": ((108, 120, 153), "skull"),
    "soul": ((114, 217, 204), "flame"),
    "sound": ((112, 200, 240), "sound"),
    "spike": ((155, 168, 185), "spikes"),
    "switching": ((255, 210, 96), "arrows"),
    "tongue": ((230, 108, 112), "serpent"),
    "tree": ((71, 125, 57), "tree"),
    "stone": ((143, 140, 134), "stone"),
    "vine": ((109, 183, 67), "leaf"),
    "vortex": ((89, 184, 201), "spiral"),
    "wing": ((230, 226, 244), "wind"),
}


def pt(x, y):
    return round(x * SCALE), round(y * SCALE)


def line(draw, points, color, width=1.0):
    draw.line([pt(x, y) for x, y in points], fill=color, width=max(1, round(width * SCALE)), joint="curve")


def circle(draw, x, y, r, color, width=1.0):
    x0, y0 = pt(x - r, y - r)
    x1, y1 = pt(x + r, y + r)
    draw.ellipse((x0, y0, x1, y1), outline=color, width=max(1, round(width * SCALE)))


def polygon(draw, points, color, fill=None, width=1.0):
    coords = [pt(x, y) for x, y in points]
    draw.polygon(coords, fill=fill)
    if color:
        draw.line(coords + [coords[0]], fill=color, width=max(1, round(width * SCALE)), joint="curve")


def glyph(draw, kind, x, y, color, pale):
    r = 6.2
    circle(draw, x, y, r, color, 0.8)
    if kind == "wind":
        for off in (-2.2, 0, 2.2):
            line(draw, [(x - 4.5, y + off), (x - 1, y + off), (x + 1.5, y + off - 1), (x + 4.5, y + off - 1)], pale, 0.9)
    elif kind in ("threads", "leaf"):
        line(draw, [(x - 3, y + 4), (x - 2, y + 1), (x + 1, y - 1), (x + 2, y - 4)], pale, 0.9)
        for side in (-1, 1):
            line(draw, [(x + side * 1.2, y + side * 0.4), (x + side * 4, y - 2)], color, 0.8)
            line(draw, [(x + side * 0.2, y + 2), (x + side * 3.5, y + 4)], color, 0.8)
    elif kind == "eye":
        polygon(draw, [(x - 4.5, y), (x - 2, y - 2.5), (x + 2, y - 2.5), (x + 4.5, y), (x + 2, y + 2.5), (x - 2, y + 2.5)], pale, width=0.8)
        circle(draw, x, y, 1.5, color, 1)
    elif kind in ("gem", "stone", "scales"):
        polygon(draw, [(x, y - 4.6), (x + 3.8, y - 1.5), (x + 2.5, y + 3.7), (x - 2.5, y + 3.7), (x - 3.8, y - 1.5)], pale, width=0.9)
        line(draw, [(x, y - 4.6), (x, y + 3.7)], color, 0.7)
        if kind == "scales":
            line(draw, [(x - 3, y - 0.4), (x + 3, y - 0.4)], color, 0.8)
            line(draw, [(x - 2, y + 1.5), (x + 2, y + 1.5)], color, 0.8)
    elif kind == "arrows":
        line(draw, [(x - 4, y - 1), (x + 3, y - 1), (x + 1, y - 3)], pale, 1)
        line(draw, [(x + 4, y + 1), (x - 3, y + 1), (x - 1, y + 3)], color, 1)
    elif kind == "drops":
        polygon(draw, [(x, y - 4.5), (x + 3, y), (x + 2.5, y + 3), (x, y + 4), (x - 2.5, y + 3), (x - 3, y)], pale, width=0.8)
        circle(draw, x + 1, y + 1.5, 0.7, color, 0.7)
    elif kind in ("spikes", "sun"):
        for i in range(5 if kind == "spikes" else 8):
            angle = i * 6.28318 / (5 if kind == "spikes" else 8)
            import math
            inner, outer = 2.2, 5.2 if kind == "spikes" else 4.5
            line(draw, [(x + math.cos(angle) * inner, y + math.sin(angle) * inner),
                        (x + math.cos(angle) * outer, y + math.sin(angle) * outer)], pale, 1)
        circle(draw, x, y, 1.6, color, 0.8)
    elif kind == "portal":
        circle(draw, x, y, 3.8, pale, 0.9)
        circle(draw, x, y, 1.8, color, 0.8)
        line(draw, [(x, y - 5), (x, y - 3.8)], color, 1)
        line(draw, [(x, y + 3.8), (x, y + 5)], color, 1)
    elif kind == "dunes":
        for off in (-2, 0, 2):
            line(draw, [(x - 4.5, y + off), (x - 2, y + off - 1.2), (x + 1, y + off - 1.5), (x + 4.5, y + off)], pale, 0.8)
    elif kind == "gear":
        polygon(draw, [(x, y - 4), (x + 1, y - 2), (x + 3.5, y - 2), (x + 2.5, y), (x + 3.5, y + 2), (x + 1, y + 2), (x, y + 4), (x - 1, y + 2), (x - 3.5, y + 2), (x - 2.5, y), (x - 3.5, y - 2), (x - 1, y - 2)], pale, width=0.8)
        circle(draw, x, y, 1.4, color, 0.8)
    elif kind == "shield":
        polygon(draw, [(x - 4, y - 3), (x + 4, y - 3), (x + 3, y + 1), (x, y + 5), (x - 3, y + 1)], pale, width=0.9)
        line(draw, [(x, y - 2), (x, y + 3)], color, 0.8)
    elif kind in ("smoke", "spiral", "serpent"):
        points = []
        import math
        for i in range(18):
            t = i / 17
            a = t * 4.6 + 0.2
            rad = (0.4 + 3.5 * t) if kind == "spiral" else 2.8
            points.append((x + math.cos(a) * rad, y + math.sin(a) * rad))
        line(draw, points, pale, 0.9)
    elif kind == "snow":
        for angle in (0, 1.0472, 2.0944):
            import math
            dx, dy = math.cos(angle) * 4.6, math.sin(angle) * 4.6
            line(draw, [(x - dx, y - dy), (x + dx, y + dy)], pale, 0.8)
    elif kind == "notes":
        circle(draw, x - 1.4, y + 2, 1.1, pale, 0.8)
        line(draw, [(x - 0.3, y + 2), (x - 0.3, y - 4), (x + 3, y - 3)], color, 0.9)
        circle(draw, x + 3, y + 3, 1.1, color, 0.8)
    elif kind == "skull":
        circle(draw, x, y - 0.8, 3.2, pale, 0.9)
        circle(draw, x - 1.1, y - 1, 0.7, color, 0.8)
        circle(draw, x + 1.1, y - 1, 0.7, color, 0.8)
        line(draw, [(x - 1.5, y + 2), (x + 1.5, y + 2)], color, 0.8)
    elif kind == "flame":
        polygon(draw, [(x, y - 5), (x + 3.5, y - 1), (x + 2, y + 4), (x, y + 5), (x - 2.5, y + 3), (x - 3, y)], pale, width=0.9)
        line(draw, [(x, y + 3), (x - 1, y), (x + 1, y - 2)], color, 0.8)
    elif kind == "sound":
        for r in (1.5, 3, 4.5):
            line(draw, [(x - 1, y - r), (x + 1, y), (x - 1, y + r)], pale if r != 3 else color, 0.8)
    elif kind == "tree":
        line(draw, [(x, y + 4.5), (x, y - 2), (x - 3.2, y - 4), (x, y - 2), (x + 3.2, y - 4)], pale, 1)
        for side in (-1, 1):
            line(draw, [(x, y), (x + side * 3.5, y - 1)], color, 0.8)
    image_points = None


def generate(name, color, shape):
    image = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    primary = (*color, 245)
    highlight = (255, 236, 179, 235)
    # Fine corner rails and paired magic seals frame, but do not replace, the clover medallion.
    line(draw, [(5, 7), (5, 73), (12, 73)], primary, 0.75)
    line(draw, [(59, 7), (59, 73), (52, 73)], primary, 0.75)
    line(draw, [(5, 7), (12, 7)], primary, 0.75)
    line(draw, [(59, 7), (52, 7)], primary, 0.75)
    glyph(draw, shape, 32, 16, primary, highlight)
    glyph(draw, shape, 32, 64, primary, highlight)
    glyph(draw, shape, 11, 40, highlight, primary)
    glyph(draw, shape, 53, 40, highlight, primary)
    line(draw, [(12, 16), (18, 22)], primary, 0.7)
    line(draw, [(52, 16), (46, 22)], primary, 0.7)
    line(draw, [(12, 64), (18, 58)], primary, 0.7)
    line(draw, [(52, 64), (46, 58)], primary, 0.7)
    return image.resize((64, 80), Image.Resampling.LANCZOS)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, (color, shape) in MAGICS.items():
        generate(name, color, shape).save(os.path.join(OUT, f"magic_sigil_{name}.png"), optimize=True)
    print(f"wrote {len(MAGICS)} magic-specific cover sigils")


if __name__ == "__main__":
    main()
