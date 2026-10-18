#!/usr/bin/env python3
"""
Sprites and item models of the Skill Tree's two books (docs/skilltree/items.md).

WHY a script and not hand-drawn files: the Skill Book is ONE item for every route, so its look is a pair of 16x16 layers instead of a texture per route:
layer0 is the book (pages, binding, gold fittings, gem) and layer1 is the cover in neutral greys. The client multiplies layer1 by the route's own colour
(client/skilltree/ItemsClient, an item colour handler on tint index 1), so a new magic is a JSON file and needs no new picture. The five SP Books have a
sprite each (a model override reads the item property nusmp:sp_kind). A script makes the pair exactly complementary (no cover pixel under a fitting, no
gap between the layers) and makes the whole set reproducible: running it twice writes byte-identical files (no clock, no randomness).

    python3 tools/skilltree/gen_item_icons.py                # writes textures and models under src/main/resources/assets/nusmp
    python3 tools/skilltree/gen_item_icons.py --preview DIR  # also writes enlarged previews (the books, and the Skill Book tinted in four route colours) to DIR

Textures: item/skill_book.png, item/skill_book_cover.png, item/sp_book.png (a stack with no known kind), item/sp_book_<kind>.png.
Models:   models/item/skill_book.json (two layers), models/item/sp_book.json (the override list), models/item/sp_book_<kind>.json.
The order of KINDS is the order of SpKind.java (the override thresholds are index / 10 - 0.05): the asset test checks it against the Java.
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")

# the kinds, in the order of SpKind.java (index = position + 1; the model value is index / 10)
KINDS = ["beginner_archery", "elemental_primer", "magic_booster", "jack_in_the_box", "greater_primer"]

CLEAR = (0, 0, 0, 0)


def rgb(h):
    return (h >> 16 & 255, h >> 8 & 255, h & 255, 255)


# ---- one shared palette for the fixed colours (layer0 of the Skill Book and the whole of the SP Books)
PAL = {
    "K": rgb(0x1C1022),   # outline
    "P": rgb(0xF4E8C4),   # page
    "p": rgb(0xD0BE96),   # page shade
    "L": rgb(0xAA9670),   # page line
    "G": rgb(0xF0C448),   # gold
    "g": rgb(0xB07C26),   # gold shade
    "W": rgb(0xFFF0A0),   # gold light
    "J": rgb(0x58E0FF),   # gem
    "j": rgb(0x1C78C4),   # gem shade
    "h": rgb(0xF0FFFF),   # gem light
    "R": rgb(0xD83A3A),   # red
    "r": rgb(0x8C1E2A),   # dark red
    "B": rgb(0x3C78E0),   # blue
    "E": rgb(0x44B05A),   # green
    "Y": rgb(0xFFE23C),   # yellow
    "y": rgb(0xC09A18),   # dark yellow
    "T": rgb(0xC8A878),   # tan
    "N": rgb(0x7A8494),   # steel
    "n": rgb(0x4A5260),   # dark steel
    "F": rgb(0xFFD0A8),   # skin
    "Z": rgb(0xFFFFFF),   # white
}

# ---- the cover layer: greys that the client multiplies by the route colour
COVER = {
    "H": (255, 255, 255, 255),   # highlight
    "C": (226, 226, 226, 255),   # cover
    "b": (190, 190, 190, 255),   # hinge groove highlight
    "c": (150, 150, 150, 255),   # shadow side
    "d": (104, 104, 104, 255),   # spine
    "e": (186, 186, 186, 255),   # engraved rune
}


class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (16, 16), CLEAR)

    def px(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.img.putpixel((x, y), c)

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def frame(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            self.px(x, y0, c)
            self.px(x, y1, c)
        for y in range(y0, y1 + 1):
            self.px(x0, y, c)
            self.px(x1, y, c)

    def stamp(self, x, y, art, legend=None):
        """Draws rows of characters with the top-left corner at (x, y); '.' is left alone; letters come from legend, else PAL."""
        legend = legend or PAL
        for dy, row in enumerate(art):
            for dx, ch in enumerate(row):
                if ch != "." and ch in legend:
                    self.px(x + dx, y + dy, legend[ch])

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path, optimize=False)


def gem_pixels(cx, cy, radius):
    """The pixels of a faceted gem in a gold bezel, as (x, y, colour): a diamond of |dx| + |dy| <= radius, the outermost ring is the bezel, the rest the gem.
    The light comes from the top left: the upper-left facets and bezel are bright, the lower-right ones dark, the centre and one specular dot are white."""
    out = []
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            d = abs(dx) + abs(dy)
            if d > radius:
                continue
            side = dx + dy                       # negative: towards the light
            if d == radius:                      # the bezel
                colour = PAL["W"] if side <= -2 else PAL["g"] if side >= 2 else PAL["G"]
            elif dx == 0 and dy == 0:
                colour = PAL["h"]
            elif (dx, dy) == (-1, -1) and radius >= 3:
                colour = PAL["h"]                # the specular dot
            elif side < 0:
                colour = PAL["J"]
            elif side > 0:
                colour = PAL["j"]
            else:
                colour = PAL["J"]
            out.append((cx + dx, cy + dy, colour))
    return out


# =====================================================================================================  the Skill Book
def skill_book():
    """Returns (layer0, layer1): the book and its tintable cover; the two never share a pixel."""
    base, cover = Canvas(), Canvas()
    # the pages behind the cover: only the right edge and the bottom edge show
    base.rect(13, 3, 13, 13, PAL["P"])
    for y in range(4, 14, 2):
        base.px(13, y, PAL["L"])
    base.rect(13, 2, 14, 2, PAL["K"])
    base.rect(14, 3, 14, 15, PAL["K"])
    base.rect(4, 14, 13, 14, PAL["P"])
    for x in range(5, 14, 2):
        base.px(x, 14, PAL["L"])
    base.rect(3, 15, 14, 15, PAL["K"])
    base.px(3, 14, PAL["K"])
    base.px(13, 14, PAL["p"])
    # the cover's outline
    base.frame(1, 0, 12, 13, PAL["K"])
    # the cover leather (layer1): highlight on top, a dark spine on the left, a groove, the face, a shadow on the right and at the bottom
    cover.rect(2, 1, 11, 12, COVER["C"])
    cover.rect(2, 1, 11, 1, COVER["H"])
    cover.rect(2, 1, 2, 12, COVER["d"])
    cover.rect(3, 1, 3, 12, COVER["b"])
    cover.rect(11, 2, 11, 12, COVER["c"])
    cover.rect(4, 12, 10, 12, COVER["c"])
    for x in (5, 7, 9):                                    # engraved runes above and below the gem
        cover.px(x, 2, COVER["e"])
        cover.px(x, 10, COVER["e"])
    # gold corners (they replace cover pixels, so the layers stay disjoint)
    corners = {
        (2, 1): "W", (3, 1): "G", (2, 2): "G",
        (10, 1): "G", (11, 1): "G", (11, 2): "g",
        (2, 12): "G", (3, 12): "g", (2, 11): "G",
        (11, 12): "g", (10, 12): "g", (11, 11): "g",
    }
    for (x, y), k in corners.items():
        base.px(x, y, PAL[k])
        cover.px(x, y, CLEAR)
    # the gem in a gold diamond setting (7x7), drawn on the base layer; the cover is cleared under it so the two layers stay disjoint
    for x, y, colour in gem_pixels(7, 6, 3):
        base.px(x, y, colour)
        cover.px(x, y, CLEAR)
    return base, cover


# =====================================================================================================  the SP Books
def primer(cover_light, cover_mid, cover_dark, trim, emblem, ribbon=True):
    """A small book in plain colours: the same silhouette as the Skill Book, thinner, with an emblem and (optionally) a ribbon."""
    c = Canvas()
    K = PAL["K"]
    # pages
    c.rect(13, 3, 13, 13, PAL["P"])
    for y in range(4, 14, 2):
        c.px(13, y, PAL["L"])
    c.rect(13, 2, 14, 2, K)
    c.rect(14, 3, 14, 15, K)
    c.rect(4, 14, 13, 14, PAL["P"])
    for x in range(5, 14, 2):
        c.px(x, 14, PAL["L"])
    c.rect(3, 15, 14, 15, K)
    c.px(3, 14, K)
    # cover
    c.frame(1, 0, 12, 13, K)
    c.rect(2, 1, 11, 12, cover_mid)
    c.rect(2, 1, 11, 1, cover_light)
    c.rect(2, 2, 2, 12, cover_dark)
    c.rect(3, 2, 3, 12, cover_light)
    c.rect(11, 2, 11, 12, cover_dark)
    c.rect(4, 12, 10, 12, cover_dark)
    # trim: small corners
    for (x, y) in ((2, 1), (11, 1), (2, 12), (11, 12)):
        c.px(x, y, trim)
    c.stamp(4, 3, emblem[0], emblem[1])
    if ribbon:
        c.stamp(9, 13, ["R", "r"], PAL)
        c.px(9, 14, PAL["R"])
        c.px(9, 15, PAL["r"])
    return c


def archery():
    # a target (red, white, red, yellow bullseye) with an arrow stuck in it, on a forest-green cover
    target = [
        "..RRR..",
        ".RZZZR.",
        "RZRRRZR",
        "RZRYRZR",
        "RZRRRZR",
        ".RZZZR.",
        "..RRR..",
    ]
    c = primer(rgb(0x62B060), rgb(0x3E8A48), rgb(0x255A30), PAL["G"], ([], PAL))
    c.stamp(4, 5, target)
    # the arrow comes in from the upper right: a tan shaft, a steel tip in the bullseye, white and red fletching
    for (x, y) in ((8, 6), (9, 5), (10, 4)):
        c.px(x, y, PAL["T"])
    c.px(7, 7, PAL["N"])
    c.px(10, 3, PAL["Z"])
    c.px(11, 4, PAL["Z"])
    c.px(11, 3, PAL["R"])
    c.px(9, 4, PAL["R"])
    c.px(10, 5, PAL["R"])
    return c


def elemental():
    # a diamond of four coloured triangles meeting in a white centre: fire above, water to the right, earth below, wind to the left
    c = primer(rgb(0x7C8EA8), rgb(0x56667E), rgb(0x364254), PAL["G"], ([], PAL))
    for dy in range(-3, 4):
        for dx in range(-3, 4):
            if abs(dx) + abs(dy) > 3:
                continue
            if dx == 0 and dy == 0:
                colour = PAL["Z"]
            elif abs(dy) >= abs(dx):
                colour = PAL["R"] if dy < 0 else PAL["E"]
            else:
                colour = PAL["B"] if dx > 0 else PAL["Y"]
            c.px(7 + dx, 6 + dy, colour)
    # a thin dark outline round the diamond so the colours do not bleed into the cover
    for dy in range(-4, 5):
        for dx in range(-4, 5):
            if abs(dx) + abs(dy) == 4:
                c.px(7 + dx, 6 + dy, PAL["K"])
    return c


def booster():
    # a lightning bolt with two sparks, on a violet cover
    art = [
        "....YY..",
        "...YY..Z",
        "..YY....",
        ".YYYYY..",
        "...YY...",
        "..YY....",
        "..Y..Z..",
    ]
    return primer(rgb(0xA46CF0), rgb(0x7A44C8), rgb(0x4C2A8C), PAL["Y"], (art, PAL))


def greater():
    # a thick crimson tome with a big gem in a gold setting (no emblem stamp, the gem is drawn by hand)
    c = primer(rgb(0xC83C4C), rgb(0x9C2236), rgb(0x641424), PAL["G"], ([], PAL), ribbon=False)
    for x, y, colour in gem_pixels(7, 6, 2):
        c.px(x, y, colour)
    for (x, y) in ((4, 2), (9, 2), (4, 10), (9, 10)):
        c.px(x, y, PAL["G"])
    # gold bands on the spine
    for y in (3, 9):
        c.px(2, y, PAL["G"])
        c.px(3, y, PAL["W"])
    c.stamp(9, 13, ["Y", "y"], PAL)
    c.px(9, 14, PAL["Y"])
    c.px(9, 15, PAL["y"])
    return c


def jack_in_the_box():
    """A toy box with a spring and a grinning head bursting out of it: not a book, so it reads as the gamble."""
    c = Canvas()
    art = [
        "....KKKK........",
        "...KFFFFK.......",
        "..KFFKFKFK......",
        "..KFFFFFFK......",
        "..KFRRRRFK......",
        "...KFFFFK.......",
        "....KKKK........",
        "....KNNK........",
        "....KnnK........",
        "....KNNK........",
        "..KKKKKKKK......",
        ".KRRYYRRYYRK....",
        ".KRRYYRRYYRK....",
        ".KrrYYrrYYrK....",
        ".KrrYYrrYYrK....",
        "..KKKKKKKKK.....",
    ]
    legend = dict(PAL)
    c.stamp(0, 0, art, legend)
    # shift the picture right by 2 so it sits in the middle of the frame
    shifted = Canvas()
    shifted.img.paste(c.img, (2, 0))
    c = shifted
    # a hat on the head, a bright button nose, and a spring
    c.px(6, 0, PAL["B"])
    c.px(7, 0, PAL["B"])
    c.px(8, 0, PAL["B"])
    c.px(9, 0, PAL["B"])
    c.px(7, 1, PAL["Y"])
    c.px(8, 1, PAL["Y"])
    c.px(7, 3, PAL["h"])
    c.px(9, 3, PAL["h"])
    c.px(8, 4, PAL["R"])
    return c


def sp_default():
    """The picture of a stack with no known kind: a plain cream-and-brown primer with a gold gem."""
    c = primer(rgb(0xC8A878), rgb(0xA8845A), rgb(0x6C5034), PAL["G"], ([], PAL), ribbon=True)
    gem = [
        ".J.",
        "JhJ",
        ".j.",
    ]
    c.stamp(6, 5, gem)
    return c


# =====================================================================================================  models
def models():
    out = {}
    out["skill_book"] = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "nusmp:item/skill_book", "layer1": "nusmp:item/skill_book_cover"},
    }
    overrides = []
    for i, kind in enumerate(KINDS):
        threshold = round((i + 1) / 10.0 - 0.05, 2)
        overrides.append({"predicate": {"nusmp:sp_kind": threshold}, "model": "nusmp:item/sp_book_" + kind})
    out["sp_book"] = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "nusmp:item/sp_book"},
        "overrides": overrides,
    }
    for kind in KINDS:
        out["sp_book_" + kind] = {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/sp_book_" + kind}}
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


# =====================================================================================================  previews
def tinted(layer0, cover, colour):
    """What the client draws: layer0 as it is, the cover multiplied by the route colour."""
    r, g, b = colour
    out = layer0.img.copy()
    for y in range(16):
        for x in range(16):
            px = cover.img.getpixel((x, y))
            if px[3] == 0:
                continue
            out.putpixel((x, y), (px[0] * r // 255, px[1] * g // 255, px[2] * b // 255, 255))
    return out


def sheet(images, scale, path, cols):
    """Images on a dark checkered background, enlarged with nearest-neighbour."""
    pad = 2
    rows = (len(images) + cols - 1) // cols
    w = cols * (16 + pad) + pad
    h = rows * (16 + pad) + pad
    bg = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    for y in range(h):
        for x in range(w):
            bg.putpixel((x, y), (46, 46, 56, 255) if ((x // 4) + (y // 4)) % 2 == 0 else (58, 58, 70, 255))
    for i, im in enumerate(images):
        bg.alpha_composite(im, (pad + (i % cols) * (16 + pad), pad + (i // cols) * (16 + pad)))
    bg = bg.resize((w * scale, h * scale), Image.NEAREST)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    bg.save(path)


def main():
    textures = os.path.join(RES, "textures", "item")
    base, cover = skill_book()
    base.save(os.path.join(textures, "skill_book.png"))
    cover.save(os.path.join(textures, "skill_book_cover.png"))
    sprites = {
        "sp_book": sp_default(),
        "sp_book_beginner_archery": archery(),
        "sp_book_elemental_primer": elemental(),
        "sp_book_magic_booster": booster(),
        "sp_book_jack_in_the_box": jack_in_the_box(),
        "sp_book_greater_primer": greater(),
    }
    for name, canvas in sprites.items():
        canvas.save(os.path.join(textures, name + ".png"))
    for name, obj in models().items():
        write_json(os.path.join(RES, "models", "item", name + ".json"), obj)
    if "--preview" in sys.argv:
        folder = sys.argv[sys.argv.index("--preview") + 1]
        # the four shipped routes (fire, lightning, ice, sky) and the grey of a route the client does not know
        colours = [(0xE8, 0x54, 0x1C), (0xF5, 0xD6, 0x3B), (0xBD, 0xEF, 0xFF), (0x6E, 0xC3, 0xF7), (0x8A, 0x8A, 0x9A)]
        row = [tinted(base, cover, c) for c in colours] + [base.img, cover.img]
        sheet(row, 24, os.path.join(folder, "skill_book_tints.png"), 7)
        sheet([sprites[n].img for n in sprites], 24, os.path.join(folder, "sp_books.png"), 6)
    print("wrote", len(sprites) + 2, "textures and", len(models()), "models")


if __name__ == "__main__":
    main()
