"""Elsdocia's 16x16 inventory sprite (1.2.0 remake): a broad shattered-crystal blade with a bronze-gold top section and a clover cut-out,
a thin gold cross guard, a dark grip and a gold pommel, on the same diagonal as every other weapon icon (hilt bottom left).
The previous sprites stay as textures/item/elsdocia_v099.png. Run: python3 -B tools/gen_elsdocia_icon.py"""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item", "elsdocia.png")
NAVY, DEEP, MID, LIGHT, ICE, WHITE = (12, 26, 82), (28, 70, 190), (52, 128, 238), (110, 204, 255), (190, 236, 255), (246, 252, 255)
GOLD, GOLD_HI, GOLD_LO, GRIP = (226, 178, 62), (255, 226, 120), (150, 104, 30), (30, 34, 52)

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c + (255,))


# axis coordinates: t runs along the sword (0 pommel ... 29 tip), p across it (0 = centre line)
def xy(t, p):
    return (t + p + 1) // 2 if False else None


for y in range(16):
    for x in range(16):
        t, p = x + (15 - y), x - (15 - y)          # t 0..30 along the diagonal, p across (even/odd lattice: the diagonal pixels have p == 0)
        # pommel and grip
        if t in (0, 1) and abs(p) <= 1:
            put(x, y, GOLD_LO if t == 0 else GOLD)
        elif 2 <= t <= 6 and p == 0:
            put(x, y, GRIP)
        # cross guard
        elif t in (7, 8) and abs(p) <= 6 and (p % 2 == 0 or abs(p) <= 2):
            put(x, y, GOLD_HI if t == 8 and abs(p) <= 2 else GOLD)
        # blade: |p| <= 3 wide from t 9, bronze first, then crystal, taper to a point
        elif t >= 9 and abs(p) <= (4 if t < 26 else 2 if t < 29 else 0):
            edge = abs(p) == (4 if t < 26 else 2 if t < 29 else 0)
            if t < 14:                              # bronze top with the clover cut-out
                c = (GOLD_LO if edge else GOLD)
                if t in (11, 12) and abs(p) <= 1:
                    c = NAVY
                put(x, y, c)
            else:
                if edge:
                    put(x, y, NAVY)
                else:
                    h = (x * 7 + y * 13 + t * 3) % 11
                    c = MID if abs(p) >= 2 else LIGHT
                    if h in (0, 5):
                        c = WHITE
                    elif h == 3:
                        c = ICE
                    elif h in (1, 8) and abs(p) >= 2:
                        c = DEEP
                    put(x, y, c)
img.save(OUT)
print("wrote", OUT)
