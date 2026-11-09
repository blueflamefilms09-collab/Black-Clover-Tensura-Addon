"""Why: Adaptability (3.70.0) gets its own 32x32 skill icon in the same framed style as tools/skills/gen_skill_icons.py (Unique = gold frame): a dark ground, three nested shield arcs that grow
brighter inward (the body learning a blow, layer by layer) and a small spiral-arrow of light. Deterministic.

    python tools/skills/gen_adaptability_icon.py     # writes src/main/resources/assets/nusmp/textures/skill/adaptability.png
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill", "adaptability.png")
S, N = 256, 32
GOLD, GOLD_D, GOLD_L = (222, 176, 62), (120, 84, 22), (255, 232, 140)


def main():
    im = Image.new("RGBA", (S, S), (14, 18, 34, 255))
    d = ImageDraw.Draw(im)
    for y in range(S):  # a slow vertical gradient so the ground is not flat
        d.line([(0, y), (S, y)], fill=(14 + y // 18, 18 + y // 14, 34 + y // 8, 255))
    d.rectangle((0, 0, S - 1, S - 1), outline=GOLD_D + (255,), width=18)
    d.rectangle((9, 9, S - 10, S - 10), outline=GOLD + (255,), width=10)
    d.rectangle((19, 19, S - 20, S - 20), outline=GOLD_L + (255,), width=3)
    cx, cy = S // 2, S // 2 + 6
    for i, (r, col) in enumerate([(88, (60, 96, 170)), (66, (92, 150, 220)), (44, (150, 212, 255))]):  # nested shields, brighter inward
        pts = [(cx - r, cy - r + 6), (cx, cy - r - 10), (cx + r, cy - r + 6), (cx + r - 6, cy + r * 0.2), (cx, cy + r + 8), (cx - r + 6, cy + r * 0.2)]
        d.polygon(pts, outline=col + (255,), width=9)
    pts = []  # a spiral of light from the heart outwards, ending in an arrow head
    for k in range(0, 61):
        a = k * 0.28
        r = 4 + k * 0.62
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    d.line(pts, fill=GOLD_L + (255,), width=9, joint="curve")
    ex, ey = pts[-1]
    pa = math.atan2(pts[-1][1] - pts[-4][1], pts[-1][0] - pts[-4][0])
    d.polygon([(ex + 20 * math.cos(pa), ey + 20 * math.sin(pa)), (ex + 12 * math.cos(pa + 2.3), ey + 12 * math.sin(pa + 2.3)),
               (ex + 12 * math.cos(pa - 2.3), ey + 12 * math.sin(pa - 2.3))], fill=GOLD_L + (255,))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    im.resize((N, N), Image.LANCZOS).convert("RGBA").save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
