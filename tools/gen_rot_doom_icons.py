"""Inventory icons and item models of the Club of Rot and the Doom Hammer (two weapons of the reference sheet "Magic Item Compendium I":
docs/attributes/art_reference/weapons/bolt_thrower_crossbow_of_swift_death_club_of_rot_doom_hammer_brooch_pin.webp).

    python3 -B tools/gen_rot_doom_icons.py

Writes assets/nusmp/textures/item/<id>.png (16x16, the weapon on the same bottom-left to top-right diagonal as every other weapon icon,
so the 3D mesh of tools/gen_weapon_models.py is placed the same way) and the two item models <id>.json (the 3D in-hand model, display
transforms copied from the Magebane Longsword) and <id>_icon.json (the flat inventory icon). The 3D models come from gen_weapon_models.py.
Both icons are drawn in (along, across) coordinates of the diagonal, then shaded with the light coming from the upper left.
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")
S = 16
R2 = math.sqrt(2.0)


def frame(x, y):
    """Pixel (x, y) (y down) -> (along, across): along runs bottom-left to top-right, across is positive towards the lower right."""
    cx, cy = x + 0.5, S - y - 0.5
    return (cx + cy) / R2, (cx - cy) / R2


def cell(along, across):
    """The inverse: pixel coordinates of a point in the frame."""
    cx = (along + across) / R2
    cy = (along - across) / R2
    return int(math.floor(cx)), int(math.floor(S - cy))


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, rgb):
        if 0 <= x < S and 0 <= y < S:
            self.px[(x, y)] = rgb

    def get(self, x, y):
        return self.px.get((x, y))

    def fill(self, inside, paint):
        """inside(along, across) -> bool; paint(x, y, along, across, edge) -> rgb. 'edge' is true on the outermost pixels of the shape."""
        mask = {}
        for y in range(S):
            for x in range(S):
                a, c = frame(x, y)
                if inside(a, c):
                    mask[(x, y)] = (a, c)
        for (x, y), (a, c) in mask.items():
            edge = any((x + dx, y + dy) not in mask for dx in (-1, 0, 1) for dy in (-1, 0, 1) if dx or dy)
            self.put(x, y, paint(x, y, a, c, edge))

    def outline(self, rgb):
        """A one pixel outline round everything drawn so far."""
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
        im.save(os.path.join(OUT, item_id + ".png"))
        return im

    def dump(self):
        rows = []
        for y in range(S):
            rows.append("".join("#" if (x, y) in self.px else "." for x in range(S)))
        return "\n".join(rows)


def tone(rgb, k):
    return tuple(max(0, min(255, int(v * k))) for v in rgb)


def mixc(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


# ---------------------------------------------------------------- the Club of Rot
def club_of_rot():
    """A gnarled bone club: a wrapped grip below, a knobbed bulb of bone above streaked with rot, green growths and a dripping ooze."""
    cv = Canvas()
    bone_l, bone_m, bone_d = (246, 238, 212), (216, 204, 170), (160, 142, 108)
    linen_l, linen_d = (200, 188, 150), (140, 124, 92)
    rot_g, rot_l, rot_d = (84, 150, 52), (170, 232, 92), (52, 96, 40)
    blood = (176, 56, 44)

    def radius(a):
        if a < 6.2:
            return 1.0
        k = min(1.0, (a - 6.2) / 8.0)
        return 0.95 + 1.45 * (k ** 0.8) - (max(0.0, a - 16.6) ** 2) * 0.3          # the bulb swells, then rounds off

    def club(a, c):
        return 0.2 <= a <= 19.2 and abs(c) <= radius(a) + 0.15 * math.sin(a * 1.7)

    def paint(x, y, a, c, edge):
        if a < 6.4:                                                                      # the grip: grave linen, wound diagonally
            base = linen_l if int(a * 1.3 + c * 2) % 3 else linen_d
            return tone(base, 1.0 - 0.18 * max(0.0, c) + 0.1 * min(0.0, c))
        shade = 1.0 - 0.34 * (c / max(0.8, radius(a))) * 0.5 - (0.14 if c > 0.4 else 0.0)
        base = bone_l if c < -0.7 else bone_m
        if c > 0.7:
            base = bone_d
        return tone(base, shade + 0.04)

    cv.fill(club, paint)
    # the knobbed end of the femur: two condyles
    for (a, c, r) in ((19.2, -1.6, 1.25), (19.4, 1.5, 1.25), (20.4, 0.0, 1.0)):
        for y in range(S):
            for x in range(S):
                aa, cc = frame(x, y)
                if math.hypot(aa - a, cc - c) <= r:
                    cv.put(x, y, tone(bone_l if cc < 0 else bone_m, 1.0 if cc < 0.4 else 0.86))
    # the linen binding: a darker band between the grip and the bulb, a loose tail hanging
    for y in range(S):
        for x in range(S):
            a, c = frame(x, y)
            if 5.2 <= a <= 6.6 and abs(c) <= 1.5 and (x, y) in cv.px:
                cv.put(x, y, tone(linen_d, 0.9))
    # rot: dark patches, green growths and a bleeding tip
    for (a, c, col) in ((9.6, 0.8, rot_d), (11.2, -0.6, rot_d), (13.0, 1.0, rot_d), (15.8, -1.1, rot_d), (17.2, 0.6, rot_d)):
        x, y = cell(a, c)
        cv.put(x, y, col)
    for (a, c, big) in ((8.4, -1.4, False), (12.0, 2.2, True), (14.6, -2.2, True), (16.8, 1.9, True), (18.6, -1.2, False)):
        x, y = cell(a, c)
        cv.put(x, y, rot_l)
        cv.put(x + 1, y, rot_g)
        if big:
            cv.put(x, y + 1, rot_g)
    for (a, c) in ((20.2, -0.5), (19.6, 0.9)):
        x, y = cell(a, c)
        cv.put(x, y, blood)
    cv.outline((38, 30, 22))
    # ooze dripping off the bulb (outside the outline so it reads against any background)
    x, y = cell(12.4, 3.4)
    cv.put(x, y, rot_g)
    cv.put(x, y + 1, rot_l)
    x, y = cell(16.2, -3.6)
    cv.put(x, y, rot_g)
    cv.put(x - 1, y + 1, rot_d)
    return cv


# ---------------------------------------------------------------- the Doom Hammer
def doom_hammer():
    """A black-iron war hammer: a long haft with an iron band, a broad hammer face on one side, a hooked pick on the other, a spike on top and an ember-lit doom rune."""
    cv = Canvas()
    iron_d, iron_m, iron_l = (30, 30, 42), (66, 68, 88), (128, 132, 156)
    haft_d, haft_l = (44, 36, 40), (92, 78, 78)
    ember, ember_l = (236, 88, 28), (255, 196, 96)

    def haft(a, c):
        return 0.2 <= a <= 16.0 and abs(c) <= 0.7

    def haft_paint(x, y, a, c, edge):
        base = haft_l if c < -0.2 else haft_d
        if int(a) % 5 == 2:
            base = iron_m                                                                # an iron band round the haft
        return base

    cv.fill(haft, haft_paint)

    def face(a, c):                                                                      # the hammer face: a blunt block
        return 12.6 <= a <= 17.4 and 0.4 <= c <= 4.2

    def socket(a, c):                                                                    # the collar round the haft
        return 12.6 <= a <= 17.4 and -0.8 <= c < 0.4

    def pick(a, c):                                                                      # the pick: tapers and hooks down
        if not (-7.0 <= c < -0.8):
            return False
        k = (-c - 0.8) / 6.2
        mid = 15.2 - 3.2 * k * k
        return abs(a - mid) <= 2.3 * (1 - k) ** 0.9 + 0.2

    def spike(a, c):
        return 17.4 < a <= 20.2 and abs(c + 0.2) <= 0.9 * (20.2 - a) / 2.8 + 0.1

    def head(a, c):
        return face(a, c) or socket(a, c) or pick(a, c) or spike(a, c)

    def head_paint(x, y, a, c, edge):
        base = iron_m
        if face(a, c):
            base = iron_d if a < 17.0 and c > 1.0 else iron_m
            if a >= 16.7 or c < 1.1:
                base = iron_l                                                            # the lit rim of the face
        elif pick(a, c) or spike(a, c):
            base = iron_l if c < -2.0 or a > 18.0 else iron_m
        return base

    cv.fill(head, head_paint)
    # the doom rune cut into the face, lit from inside
    for (a, c) in ((14.4, 2.2), (15.4, 2.2), (14.4, 3.4), (15.4, 3.4), (16.0, 2.8)):
        x, y = cell(a, c)
        cv.put(x, y, ember)
    x, y = cell(14.9, 2.8)
    cv.put(x, y, ember_l)
    x, y = cell(0.9, 0.0)
    cv.put(x, y, iron_l)
    cv.outline((10, 10, 16))
    x, y = cell(18.8, 3.0)
    cv.put(x, y, ember)                                                                  # an ember lifting off the head
    return cv


def models(item_id):
    tpl = json.load(open(os.path.join(MODELS, "magebane_longsword.json"), encoding="utf-8"))
    tpl["textures"] = {"particle": "nusmp:item/" + item_id}
    with open(os.path.join(MODELS, item_id + ".json"), "w", newline="\n") as f:
        f.write(json.dumps(tpl, indent=2))
    with open(os.path.join(MODELS, item_id + "_icon.json"), "w", newline="\n") as f:
        f.write(json.dumps({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}, indent=2))


def main():
    for item_id, fn in (("club_of_rot", club_of_rot), ("doom_hammer", doom_hammer)):
        cv = fn()
        cv.save(item_id)
        models(item_id)
        print(item_id)
        print(cv.dump())


if __name__ == "__main__":
    main()
