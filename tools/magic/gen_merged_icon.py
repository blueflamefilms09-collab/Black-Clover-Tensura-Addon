"""The icon of a merged magic and the fallback icon of any magic whose own icon is missing.

    python tools/magic/gen_merged_icon.py

Writes src/main/resources/assets/nusmp/textures/skill/grimoire/merged.png (32x32, like every grimoire icon): two interlocking rune
circles in warm and cool light on a midnight card with a gold frame. Drawn at 8x with Pillow and reduced; deterministic.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill", "grimoire", "merged.png")
N, SS = 32, 8
S = N * SS


def ring(draw, cx, cy, r, width, color):
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=width)


def main():
    base = Image.new("RGBA", (S, S), (10, 12, 30, 255))
    # soft midnight gradient
    px = base.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - S / 2, y - S / 2) / (S * 0.72)
            k = max(0.0, 1.0 - d)
            px[x, y] = (int(10 + 34 * k), int(12 + 26 * k), int(30 + 64 * k), 255)
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    warm, cool = (255, 170, 70, 255), (90, 200, 255, 255)
    r = S * 0.25
    for (cx, col) in ((S * 0.40, warm), (S * 0.60, cool)):
        ring(g, cx, S * 0.5, r, SS * 2, col)
        ring(g, cx, S * 0.5, r * 0.62, SS, col[:3] + (200,))
    # the lens where the circles meet
    lens = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lens)
    ld.ellipse([S * 0.40 - r, S * 0.5 - r, S * 0.40 + r, S * 0.5 + r], fill=(255, 255, 255, 120))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).ellipse([S * 0.60 - r, S * 0.5 - r, S * 0.60 + r, S * 0.5 + r], fill=255)
    lens.putalpha(Image.composite(lens.getchannel("A"), Image.new("L", (S, S), 0), mask))
    # four small sparks on the axes
    for a in range(4):
        ang = a * math.pi / 2 + math.pi / 4
        x, y = S * 0.5 + math.cos(ang) * S * 0.36, S * 0.5 + math.sin(ang) * S * 0.36
        g.ellipse([x - SS, y - SS, x + SS, y + SS], fill=(255, 255, 255, 230))
    blur = glow.filter(ImageFilter.GaussianBlur(SS * 1.6))
    img = Image.alpha_composite(base, blur)
    img = Image.alpha_composite(img, lens)
    img = Image.alpha_composite(img, glow)
    # gold frame
    f = ImageDraw.Draw(img)
    f.rectangle([0, 0, S - 1, S - 1], outline=(226, 185, 90, 255), width=SS)
    f.rectangle([SS, SS, S - 1 - SS, S - 1 - SS], outline=(120, 90, 40, 255), width=max(1, SS // 4))
    img = img.resize((N, N), Image.LANCZOS)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print("wrote", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
