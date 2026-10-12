"""1.3.0: inventory icons and item models of the Brooch Pin and the Hand of Providence (reference sheets
bolt_thrower_crossbow_of_swift_death_club_of_rot_doom_hammer_brooch_pin.webp and
paladin_items_weapon_of_resurgence_hand_of_providence_cloak_deliverance.jpg).

    python3 -B tools/gen_pin_providence_icons.py

Writes
  assets/nusmp/textures/item/<id>.png      the 16x16 inventory icon, on the usual bottom-left to top-right diagonal so the 3D mesh of
                                           tools/gen_weapon_models.py (built by tools/gen_pin_providence_models.py) is placed the same way
  assets/nusmp/models/item/<id>_icon.json  the flat inventory icon model
  assets/nusmp/models/item/<id>.json       the 3D in-hand model (builtin/entity, drawn by client/WeaponRenderer from the mesh); the display
                                           transforms are the Magebane Longsword's
The Brooch Pin is a ruby brooch in gold filigree with a long steel pin (bottom left, pin to the top right, as on the sheet); the Hand of
Providence is a golden gauntlet closed in a fist with a blue cross on the back of the hand (cuff bottom left, fist top right).
"""
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
ICONS = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")
S = 16
R2 = math.sqrt(2.0)

OUTLINE = (44, 24, 14)


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, rgb):
        if 0 <= x < S and 0 <= y < S:
            self.px[(x, y)] = rgb

    def outline(self, rgb=OUTLINE):
        cells = set(self.px)
        for (x, y) in cells:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    q = (x + dx, y + dy)
                    if q not in cells and 0 <= q[0] < S and 0 <= q[1] < S:
                        self.px[q] = rgb

    def save(self, item_id):
        im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), (int(c[0]), int(c[1]), int(c[2]), 255))
        im.save(os.path.join(ICONS, item_id + ".png"))
        return im

    def dump(self, legend):
        for y in range(S):
            print("".join(legend.get(self.px.get((x, y)), "#") if (x, y) in self.px else "." for x in range(S)))


def paint_rows(rows, palette):
    cv = Canvas()
    for y, row in enumerate(rows):
        assert len(row) == S, (y, len(row), row)
        for x, ch in enumerate(row):
            if ch != ".":
                cv.put(x, y, palette[ch])
    return cv


# ---------------------------------------------------------------- the Brooch Pin
GOLD = (206, 152, 52)
GOLD_L = (255, 224, 124)
GOLD_D = (132, 86, 28)
RUBY = (214, 28, 54)
RUBY_L = (255, 150, 164)
RUBY_D = (116, 8, 34)
TOOTH = (252, 242, 218)
STEEL = (178, 190, 208)
STEEL_L = (244, 248, 255)
STEEL_D = (96, 108, 132)

# the brooch: row -> (x0, x1) of its silhouette, a rounded square of about 9 x 10 in the bottom-left corner
BROOCH_ROWS = {5: (3, 7), 6: (2, 8), 7: (1, 9), 8: (0, 9), 9: (0, 9), 10: (0, 9), 11: (0, 9), 12: (1, 9), 13: (2, 8), 14: (3, 7)}


def brooch_pin():
    cv = Canvas()
    # the pin first (the brooch is drawn over its foot): a long steel needle on the diagonal x + y = 15 .. 16, tip in the top right corner
    for y in range(0, 10):
        for x in range(16):
            d = x + y
            if d == 14 and y > 0:
                cv.put(x, y, OUTLINE)
            elif d == 15:
                cv.put(x, y, STEEL_L if y > 1 else STEEL)
            elif d == 16:
                cv.put(x, y, STEEL_D)
            elif d == 17 and y >= 2:
                cv.put(x, y, OUTLINE)
    cv.put(14, 0, OUTLINE)
    cv.put(15, 0, STEEL_L)
    mask = set()
    for y, (x0, x1) in BROOCH_ROWS.items():
        for x in range(x0, x1 + 1):
            mask.add((x, y))

    def depth(x, y):                                        # how many steps inside the silhouette (Chebyshev)
        d = 0
        while all((x + dx, y + dy) in mask for dx in range(-d - 1, d + 2) for dy in range(-d - 1, d + 2)):
            d += 1
            if d > 6:
                break
        return d

    for (x, y) in mask:
        d = depth(x, y)
        if d == 0:
            cv.put(x, y, OUTLINE)
        elif d == 1:                                        # the gold frame, light from the upper left
            cv.put(x, y, GOLD_L if x + y < 15 and (x + y) % 2 == 0 else GOLD if x + y < 19 else GOLD_D)
        else:                                               # the ruby, a teardrop shading from the glint to the deep corner
            c = RUBY
            if x + y > 19:
                c = RUBY_D
            elif x + y < 14:
                c = RUBY_L
            cv.put(x, y, c)
    for (x, y) in ((5, 6), (1, 9), (9, 9), (5, 13)):        # the teeth of the collar
        if (x, y) in mask:
            cv.put(x, y, TOOTH)
    cv.put(3, 8, RUBY_L)
    cv.put(4, 8, RUBY_L)
    cv.put(3, 9, RUBY_L)
    cv.save("brooch_pin")
    return cv


# ---------------------------------------------------------------- the Hand of Providence
# An upright gauntlet like the sheet's: a flared cuff at the bottom, a dark wrist band, the back of the hand with its blue cross, the
# four knuckle plates of a closed fist on top and the thumb on the right. O outline, g/G/d gold light/mid/dark, e curled-finger shadow,
# k wrist leather, B blue cross, b its glint.
HAND_PAL = {
    "O": OUTLINE,
    "g": (255, 226, 132), "G": (226, 170, 62), "d": (150, 98, 32), "e": (112, 70, 24),
    "k": (84, 44, 34),
    "B": (50, 142, 224), "b": (176, 228, 255),
}
HAND = [
    "................",
    "..OOOOOOOOOOO...",
    ".OgGOgGOgGOgGO..",
    ".OGdOGdOGdOGdO..",
    ".OdeOdeOdeOdeO..",
    ".OOOOOOOOOOOOO..",
    ".OggGGGbGGGGdGdO",
    ".OgGGGBBBGGGdGdO",
    ".OgGGGGBGGGGdGdO",
    ".OGGGGGBGGGGdddO",
    "..OkkkkkkkkkOOO.",
    "..OgGGGGGGGdO...",
    "..OgGGGGGGGdO...",
    "..OgGGdGGGGdO...",
    ".OgggGGGGGGGdO..",
    ".OOOOOOOOOOOOO..",
]


def hand_of_providence():
    cv = Canvas()
    for y, row in enumerate(HAND):
        assert len(row) == S, (y, len(row), row)
        for x, ch in enumerate(row):
            if ch != ".":
                cv.put(x, y, HAND_PAL[ch])
    cv.save("hand_of_providence")
    return cv


# ---------------------------------------------------------------- item models
def write_json(name, data):
    with open(os.path.join(MODELS, name + ".json"), "w", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def icon_model(item_id):
    write_json(item_id + "_icon", {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}})


def weapon_model(item_id):
    """The Magebane Longsword's display transforms (the sprite sits on the usual diagonal)."""
    src = json.load(open(os.path.join(MODELS, "magebane_longsword.json")))
    src["textures"] = {"particle": "nusmp:item/" + item_id}
    write_json(item_id, src)


def main():
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    brooch_pin()
    hand_of_providence()
    for item_id in ("brooch_pin", "hand_of_providence"):
        icon_model(item_id)
        weapon_model(item_id)
    print("wrote the icons and models of brooch_pin and hand_of_providence")


if __name__ == "__main__":
    main()
