"""Draws the 32x32 skill icon of the Sharingan (a red eye with three tomoe): textures/skill/sharingan.png. Run: python3 tools/gen_sharingan_icon.py"""
import math
from PIL import Image, ImageDraw
S = 8                                   # draw at 8x, then shrink for clean edges
img = Image.new("RGBA", (32 * S, 32 * S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
c = 16 * S
def circle(r, fill, outline=None, w=0):
    d.ellipse([c - r, c - r, c + r, c + r], fill=fill, outline=outline, width=w)
circle(15 * S, (20, 6, 8, 255))
circle(14 * S, (196, 24, 28, 255), (90, 8, 12, 255), S)
circle(10 * S, (150, 10, 16, 255), (50, 0, 4, 255), S // 2)   # ring of the tomoe
circle(3 * S, (8, 0, 2, 255))                                  # pupil
for k in range(3):
    a = math.radians(90 + k * 120)
    cx, cy = c + 7 * S * math.cos(a), c - 7 * S * math.sin(a)
    r = 2.2 * S
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(8, 0, 2, 255))
    ta = a - math.radians(70)                                    # the comma tail curls behind the head
    tx, ty = cx + 3.2 * S * math.cos(ta), cy - 3.2 * S * math.sin(ta)
    d.line([cx, cy, tx, ty], fill=(8, 0, 2, 255), width=int(1.4 * S))
img.resize((32, 32), Image.LANCZOS).save("src/main/resources/assets/anime_multiverse/textures/skill/sharingan.png")
print("ok")
