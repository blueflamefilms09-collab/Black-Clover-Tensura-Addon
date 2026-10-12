"""Inventory icons and item models of the Bolt Thrower and the Crossbow of Swift Death (two crossbows from the reference sheet
"Magic Item Compendium I": docs/attributes/art_reference/weapons/bolt_thrower_crossbow_of_swift_death_...webp).

    python3 -B tools/gen_bolt_weapon_icons.py

Writes assets/nusmp/textures/item/<id>.png (16x16, the stock on the same diagonal as every other weapon icon so the 3D mesh of
tools/gen_weapon_models.py is placed the same way) and the two item models <id>.json (the 3D in-hand model, display transforms copied
from the Magebane Longsword) and <id>_icon.json (the flat inventory icon).
Each icon is mirror-symmetric about the diagonal, so its principal axis is exactly the 45 degree sword axis.
"""
import json
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")
S = 16

OUTLINE = (12, 14, 24)


def mirror(c):
    """The reflection of a cell across the diagonal that runs from the bottom-left (0,15) to the top-right (15,0)."""
    return (S - 1 - c[1], S - 1 - c[0])


class Icon:
    def __init__(self):
        self.cells = {}

    def put(self, x, y, rgb, sym=True):
        for c in ((x, y), mirror((x, y))) if sym else ((x, y),):
            if 0 <= c[0] < S and 0 <= c[1] < S:
                self.cells[c] = rgb

    def line(self, a, b, rgb, sym=True):
        (x0, y0), (x1, y1) = a, b
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            t = i / max(1, n)
            self.put(round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), rgb, sym)

    def save(self, name):
        im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        px = im.load()
        for (x, y) in list(self.cells):
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    c = (x + dx, y + dy)
                    if 0 <= c[0] < S and 0 <= c[1] < S and c not in self.cells:
                        px[c] = OUTLINE + (255,)
        for (x, y), rgb in self.cells.items():
            px[x, y] = tuple(int(v) for v in rgb) + (255,)
        im.save(os.path.join(OUT, name + ".png"))
        return im

    def dump(self):
        rows = []
        for y in range(S):
            rows.append("".join("#" if (x, y) in self.cells else "." for x in range(S)))
        return "\n".join(rows)


def axis(k):
    return (k, S - 1 - k)


def frame(a, b):
    """A cell from the weapon's own frame: a runs along the stock (butt 0 .. muzzle 15), b across it (right of the stock +)."""
    return (int(round(a + b)), int(round(S - 1 - a + b)))


def arc(ic, a0, bmax, bend, rgb, rgb2=None, thick=1, step=0.2):
    """A limb arc across the stock at a0, tips swept back by bend * b^2."""
    n = int(bmax / step)
    for i in range(-n, n + 1):
        b = i * step
        a = a0 - bend * b * b
        for t in range(thick):
            ic.put(*frame(a + t, b), rgb2 if (rgb2 and t) else rgb, sym=False)


def chord(ic, a, bmax, rgb, step=0.25):
    n = int(bmax / step)
    for i in range(-n, n + 1):
        ic.put(*frame(a, i * step), rgb, sym=False)


def stock(ic, k0, k1, c_axis, c_side, c_side2=None):
    """A diagonal stock: the axis cell plus one neighbour that alternates sides (a 2 px staircase, mirror-symmetric overall)."""
    for k in range(k0, k1 + 1):
        x, y = axis(k)
        ic.put(x, y, c_axis(k) if callable(c_axis) else c_axis, sym=False)
        nx, ny = (x + 1, y) if k % 2 == 0 else (x, y - 1)
        ic.put(nx, ny, c_side(k) if callable(c_side) else c_side, sym=False)


def bolt_thrower():
    """Dark blued steel, brass trim, a cyan bolt channel down the stock, a cyan orb pommel, limbs swept back, a skull prow."""
    ic = Icon()
    steel_d, steel_m, steel_l = (30, 38, 58), (58, 72, 100), (112, 130, 162)
    brass, brass_l = (196, 154, 58), (252, 222, 124)
    cyan, white = (90, 224, 255), (232, 252, 255)
    stock(ic, 3, 13, lambda k: cyan if k >= 8 else steel_l, lambda k: steel_m if k % 3 else steel_l)
    for c in ((1, 13), (2, 13), (1, 14), (2, 14)):                           # the pommel orb
        ic.put(c[0], c[1], cyan, sym=False)
    ic.put(1, 14, white, sym=False)
    ic.put(*frame(3.5, 0), brass, sym=False)
    arc(ic, 10.0, 4.0, 0.09, steel_d, brass, thick=2)                           # the prod, brass along its leading edge
    chord(ic, 7.4, 3.4, white)                                                # the drawn string
    for k in (12, 13, 14):                                                    # the skull prow and muzzle
        ic.put(*axis(k), steel_l, sym=False)
    ic.put(*axis(14), brass_l, sym=False)
    ic.put(15, 0, cyan, sym=False)
    for c in ((4, 3), (3, 4), (11, 11), (12, 12), (6, 4), (13, 12)):          # lightning off the limbs
        ic.put(c[0], c[1], cyan if (c[0] + c[1]) % 2 else white, sym=False)
    return ic


def crossbow_of_swift_death():
    """Black wood and iron, a long stock with crimson notches, crank handles at the rear, a toothed jaw at the front and a wide dark prod."""
    ic = Icon()
    wood_d, wood_m, wood_l = (46, 30, 30), (84, 58, 48), (128, 96, 72)
    iron_d, iron_m, iron_l = (26, 26, 36), (62, 62, 80), (118, 118, 140)
    red, red_l, bone = (218, 30, 56), (255, 100, 112), (230, 218, 192)
    stock(ic, 2, 13, lambda k: wood_l if k % 2 else wood_m, wood_d)
    for k in (5, 7, 9):                                                        # the notches above the trigger
        ic.put(*axis(k), red, sym=False)
    ic.put(*axis(6), red_l, sym=False)
    for c in ((1, 14), (2, 14), (1, 13)):                                      # the butt plate
        ic.put(c[0], c[1], iron_l, sym=False)
    for b in (-2, -1, 1, 2):                                                   # the crank handles at the rear
        ic.put(*frame(3, b * 1.2), iron_m, sym=False)
    ic.put(*frame(3, -3.6), bone, sym=False)
    ic.put(*frame(3, 3.6), bone, sym=False)
    arc(ic, 10.0, 4.4, 0.07, iron_d, iron_l, thick=2)                         # the wide prod
    ic.put(*frame(8.6, 4.2), red, sym=False)
    ic.put(*frame(8.6, -4.2), red, sym=False)
    chord(ic, 7.2, 3.6, bone)
    for c in ((12, 3), (13, 2), (14, 1)):                                      # the toothed jaw and the bolt head
        ic.put(c[0], c[1], iron_l, sym=False)
    for c in ((14, 3), (12, 1), (13, 0), (15, 2)):
        ic.put(c[0], c[1], bone, sym=False)
    ic.put(14, 1, red_l, sym=False)
    ic.put(15, 0, red, sym=False)
    return ic


def models(item_id):
    tpl = json.load(open(os.path.join(MODELS, "magebane_longsword.json"), encoding="utf-8"))
    tpl["textures"] = {"particle": "nusmp:item/" + item_id}
    with open(os.path.join(MODELS, item_id + ".json"), "w", newline="\n") as f:
        f.write(json.dumps(tpl, indent=2))
    with open(os.path.join(MODELS, item_id + "_icon.json"), "w", newline="\n") as f:
        f.write(json.dumps({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}, indent=2))


def main():
    for item_id, fn in (("bolt_thrower", bolt_thrower), ("crossbow_of_swift_death", crossbow_of_swift_death)):
        ic = fn()
        ic.save(item_id)
        models(item_id)
        print(item_id)
        print(ic.dump())


if __name__ == "__main__":
    main()
