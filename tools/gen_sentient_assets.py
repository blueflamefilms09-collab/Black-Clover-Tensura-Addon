#!/usr/bin/env python3
"""
Assets of the Sentient Dungeon: the twelve rare synthesis materials the dead of the Dungeon leave behind (the owner's design: "a Direwolf
pelt, a Goblin dagger"): 16x16 item sprites and their item models (parent item/generated). Idempotent: running it again rewrites the same
files.

    python3 tools/gen_sentient_assets.py

Style of the repo's other item sprites: a dark outline, three or four tones of shading lit from the top left, a few bright highlights and
the odd sparkle pixel. Most sprites are drawn from small character maps (one character is one pixel, `.` is clear); the round ones are
shaded by a sphere-light function. The outline is added automatically round whatever is drawn.
"""
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(RES, "assets", "nusmp", "textures", "item")
MODELS = os.path.join(RES, "assets", "nusmp", "models", "item")

N = 16


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


class Sprite:
    """A 16x16 canvas of RGBA tuples (None = clear)."""

    def __init__(self):
        self.px = [[None] * N for _ in range(N)]

    def put(self, x, y, c):
        if 0 <= x < N and 0 <= y < N and c is not None:
            self.px[y][x] = c

    def get(self, x, y):
        return self.px[y][x] if 0 <= x < N and 0 <= y < N else None

    def draw_map(self, rows, palette, dx=0, dy=0):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != "." and ch != " ":
                    self.put(x + dx, y + dy, hexrgb(palette[ch]))

    def outline(self, color, diagonal=False):
        """A one pixel outline round everything drawn (4-neighbours; 8 when diagonal)."""
        col = hexrgb(color)
        marks = []
        for y in range(N):
            for x in range(N):
                if self.px[y][x] is not None:
                    continue
                for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)) + (((1, 1), (-1, 1), (1, -1), (-1, -1)) if diagonal else ()):
                    if self.get(x + ox, y + oy) is not None:
                        marks.append((x, y))
                        break
        for x, y in marks:
            self.px[y][x] = col

    def image(self):
        img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        for y in range(N):
            for x in range(N):
                if self.px[y][x] is not None:
                    img.putpixel((x, y), self.px[y][x])
        return img


def mirrored(left_rows, flip=None):
    """A 16 wide map from its left half (8 characters a row): the right half is the mirror image; `flip` re-letters the mirrored side (the light is at the top left)."""
    flip = flip or {}
    out = []
    for r in left_rows:
        assert len(r) == 8, r
        out.append(r + "".join(flip.get(c, c) for c in reversed(r)))
    return out


def inside(x, y, poly):
    """Even-odd point in polygon test."""
    c = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            c = not c
        j = i
    return c


def ramp_pick(ramp, t):
    """t in 0..1 (dark..light) -> a colour of the ramp."""
    i = max(0, min(len(ramp) - 1, int(t * len(ramp))))
    return hexrgb(ramp[i])


def sphere(sp, cx, cy, rx, ry, ramp, light=(-0.55, -0.7), mask=None, rim=None):
    """Shades an ellipse like a lit ball: bright toward the top left, dark toward the bottom right."""
    for y in range(N):
        for x in range(N):
            u, v = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            d = u * u + v * v
            if d > 1.0:
                continue
            if mask is not None and not mask(x, y):
                continue
            z = math.sqrt(max(0.0, 1.0 - d))
            b = -(u * light[0] + v * light[1]) * 0.55 + z * 0.55
            sp.put(x, y, ramp_pick(ramp, b))
            if rim is not None and d > 0.72:
                sp.put(x, y, hexrgb(rim))


def save(name, sp):
    os.makedirs(TEX, exist_ok=True)
    sp.image().save(os.path.join(TEX, name + ".png"))
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + name}}, f, indent=2)
        f.write("\n")


# ---------------------------------------------------------------- the twelve


def goblin_dagger():
    """A short, crude steel dagger: a broad nicked blade, a bent guard, a hilt wrapped in dirty green cloth and a bone pommel."""
    sp = Sprite()
    W, S, D = hexrgb("#f1f5f8"), hexrgb("#aeb7c2"), hexrgb("#6b7482")
    blade = [(13, 2), (12, 3), (11, 4), (10, 5), (9, 6), (8, 7)]
    sp.put(14, 1, W)
    for i, (x, y) in enumerate(blade):
        sp.put(x - 1, y, W)
        sp.put(x, y, S)
        sp.put(x + 1, y, D if i > 0 else S)
    sp.put(7, 8, S)
    sp.put(8, 8, D)
    sp.put(6, 8, W)
    for x, y in ((4, 7), (5, 8), (6, 9), (7, 10), (3, 6)):          # the bent guard
        sp.put(x, y, hexrgb("#b78a43"))
    for x, y in ((5, 7), (4, 8), (6, 10)):
        sp.put(x, y, hexrgb("#d8b266"))
    sp.put(7, 9, hexrgb("#8a6430"))
    for x, y in ((5, 9), (4, 10), (3, 11), (2, 12)):                 # the wrapped hilt
        sp.put(x, y, hexrgb("#6f9a45"))
        sp.put(x, y + 1, hexrgb("#46682b"))
    sp.put(4, 11, hexrgb("#46682b"))
    sp.put(3, 12, hexrgb("#8fbf5a"))
    sp.put(1, 13, hexrgb("#d8d0b6"))                                 # the bone pommel
    sp.put(2, 13, hexrgb("#d8d0b6"))
    sp.put(1, 14, hexrgb("#a89f86"))
    for x, y in ((11, 4), (9, 6)):                                   # the nicks of a goblin's blade
        sp.px[y][x + 1] = None
    sp.outline("#17121d")
    return sp


def kobold_fang():
    """A curved bone-white fang with a reddish root."""
    sp = Sprite()
    rows = [
        "................",
        "....rrrrr.......",
        "...rRRRRRr......",
        "...RWWWWWRr.....",
        "...RWWwwwWR.....",
        "....RWwwwwR.....",
        "....rWwwwwR.....",
        ".....WwwwwR.....",
        ".....WwwwDr.....",
        ".....WwwwD......",
        "......WwwD......",
        "......WwwD......",
        ".......WwD......",
        ".......Wd.......",
        "........d.......",
        "................",
    ]
    pal = {"r": "#7a2a2e", "R": "#b3434a", "W": "#fbf6e6", "w": "#e2d6b3", "D": "#a99a74", "d": "#8a7b57"}
    sp.draw_map(rows, pal)
    sp.outline("#2a1a1c")
    return sp


def slime_core():
    """The glossy blue-green core of a Tensura slime, with a bright heart and a white glint."""
    sp = Sprite()
    ramp = ["#16405a", "#1f6a86", "#2f9aa8", "#52cdbc", "#93eed0"]
    sphere(sp, 8, 8.4, 6.2, 6.0, ramp)
    for x, y in ((8, 8), (9, 8), (8, 9), (9, 9)):           # the core inside
        sp.put(x, y, hexrgb("#e9fff2"))
    for x, y in ((7, 8), (10, 8), (8, 7), (9, 10)):
        sp.put(x, y, hexrgb("#b8f7da"))
    for x, y in ((5, 4), (6, 4), (5, 5)):                   # the glint
        sp.put(x, y, hexrgb("#ffffff"))
    sp.outline("#0e2233")
    return sp


def aqua_slime_gel():
    """A drop of cyan gel that holds its shape, with a bubble in it."""
    sp = Sprite()
    rows = [
        "................",
        ".......cc.......",
        "......cCCc......",
        ".....cCCCCc.....",
        ".....cCCCCc.....",
        "....cCCCCCCc....",
        "...cCCWWCCCCc...",
        "...cCCWCCCCCc...",
        "..cCCCCCCCCCCc..",
        "..cCCCCCbbCCCc..",
        "..cCCCCCbBCCDc..",
        "..cCCCCCCCCDDc..",
        "...cCCCCCDDDc...",
        "....cDDDDDDc....",
        ".....cccccc.....",
        "................",
    ]
    pal = {"c": "#1d6f8f", "C": "#3fc2dc", "W": "#f4ffff", "b": "#8fe6f2", "B": "#d5faff", "D": "#2a98b6"}
    sp.draw_map(rows, pal)
    sp.outline("#0e3347")
    sp.put(12, 3, hexrgb("#d5faff"))                         # a sparkle
    return sp


def direwolf_pelt():
    """A grey wolf skin laid flat: head with its ears, splayed legs, a dark stripe down the spine and a brush of a tail."""
    sp = Sprite()
    left = [
        "........",
        "....M...",
        "....MMMM",
        "....MLMM",
        "...MMLMD",
        "..MMMMMD",
        "MMMMLMMD",
        ".MMMLMMD",
        "..MMMMMD",
        "..MLMMMD",
        "..MMMMMD",
        ".MMMMLMD",
        ".MMM.MMD",
        ".MM..MMD",
        "........",
        "........",
    ]
    rows = mirrored(left, {"L": "D"})
    pal = {"M": "#8c7e74", "L": "#c4b9a8", "D": "#5a4f48"}
    sp.draw_map(rows, pal)
    for y in (6, 9):                                                     # tufts of pale fur on the flanks
        sp.put(2, y, hexrgb("#c4b9a8"))
    for x, y in ((7, 12), (8, 12), (7, 13), (8, 13), (7, 14), (8, 14)):   # the tail
        sp.put(x, y, hexrgb("#6d6058"))
    sp.put(7, 15, hexrgb("#3e3530"))
    sp.put(8, 15, hexrgb("#3e3530"))
    sp.outline("#1d1714")
    return sp


def lizardman_scale():
    """A single hard green scale, shield-shaped, ridged, with a pale lit edge."""
    sp = Sprite()
    left = [
        "........",
        "...LLLLL",
        "..LMMMMM",
        ".LMMMmMM",
        ".LMMmMMm",
        ".LMMMmMM",
        ".LMmMMmM",
        ".LMMMmMM",
        "..MMmMMm",
        "..MMMmMM",
        "...MmMMM",
        "...MMmMM",
        "....MMMM",
        ".....MMM",
        "......MM",
        ".......M",
    ]
    rows = mirrored(left, {"L": "D", "M": "M"})
    pal = {"L": "#a7e07f", "M": "#5fa24a", "m": "#3f7a3a", "D": "#2f5e2e"}
    sp.draw_map(rows, pal)
    for y in range(2, 8):                                                # the shadow side of the right half
        for x in (13, 14):
            if sp.get(x, y) is not None and y > 3:
                sp.put(x, y, hexrgb("#478a3a"))
    sp.outline("#101c12")
    return sp


def water_spirit_essence():
    """A drop of water spirit: a pale blue orb with a slow swirl inside and motes of light round it."""
    sp = Sprite()
    ramp = ["#173f8c", "#2a67c8", "#4a9be8", "#86cdf5", "#d3f2ff"]
    sphere(sp, 8, 8.6, 5.4, 5.6, ramp)
    swirl = ((6, 9), (6, 8), (7, 7), (8, 7), (9, 8), (9, 9), (8, 10), (7, 10))
    for x, y in swirl:
        sp.put(x, y, hexrgb("#e9fbff"))
    sp.put(7, 9, hexrgb("#ffffff"))
    sp.outline("#0b1f4a")
    for x, y in ((2, 3), (13, 2), (14, 9), (1, 11), (11, 14)):    # motes
        sp.put(x, y, hexrgb("#bfeaff"))
    sp.put(3, 3, hexrgb("#7bc6f0"))
    return sp


def megalodon_tooth():
    """A great triangular tooth, serrated, white at the point and yellowed at the root."""
    sp = Sprite()
    rows = [
        "................",
        "......WWWW......",
        "......WwwwD.....",
        ".....WWwwwD.....",
        ".....WwwwwDd....",
        "....WWwwwwwDd...",
        "....WwwwwwwDd...",
        "...WwWwwwwWwDd..",
        "...WwWwwwwWwDd..",
        "..WwWwwwwwWwwDd.",
        "..WWwwwwwwwwwDd.",
        "..YYWwwwwwwwDYd.",
        "..yYYYYYYYYYYyd.",
        "...yyyyyyyyyyd..",
        "................",
        "................",
    ]
    pal = {"W": "#fffdf2", "w": "#efe7cf", "D": "#bfb393", "d": "#8e8466", "Y": "#d6bf80", "y": "#a98f54"}
    sp.draw_map(rows, pal)
    sp.outline("#2b2519")
    return sp


def devil_horn():
    """A black-red curved horn, ridged, with an ember glowing at its tip."""
    sp = Sprite()
    rows = [
        "............oO..",
        "...........oOEO.",
        "..........hrOEO.",
        "..........hRrO..",
        ".........hRRrD..",
        "........hRRrD...",
        ".......hRRrDd...",
        "......hRRRrD....",
        ".....hRrRrDd....",
        "....hRRRrrD.....",
        "...hRrRrDDd.....",
        "..hRRRrDDd......",
        "..hRrrDDd.......",
        "..hhrDDd........",
        "...ddd..........",
        "................",
    ]
    pal = {"o": "#ff9a3a", "O": "#ff5a1f", "E": "#ffe08a", "h": "#7a4a5e", "R": "#a02a3c", "r": "#6d1a2c", "D": "#3b0f1c", "d": "#240913"}
    sp.draw_map(rows, pal)
    sp.outline("#12060c")
    return sp


def disciple_sigil():
    """The mark of a Dark Disciple: a ring of violet light round a five-pointed star, on black."""
    sp = Sprite()
    ramp = ["#12091f", "#241039", "#3b1c63", "#5a2f94"]
    sphere(sp, 8, 8, 7.0, 7.0, ramp)
    for y in range(N):                                                   # the ring
        for x in range(N):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 6.0 <= d <= 6.8:
                sp.put(x, y, hexrgb("#a76bff"))
            elif 6.8 < d <= 7.3:
                sp.put(x, y, hexrgb("#6b3cc4"))
    pts = []
    for i in range(10):                                                  # the star
        a = -math.pi / 2 + i * math.pi / 5
        r = 5.1 if i % 2 == 0 else 2.3
        pts.append((8 + r * math.cos(a), 8.5 + r * math.sin(a)))
    for y in range(N):
        for x in range(N):
            if inside(x + 0.5, y + 0.5, pts):
                sp.put(x, y, hexrgb("#f1e6ff") if x + y < 16 else hexrgb("#bb98f7"))
    sp.put(7, 7, hexrgb("#ffffff"))
    sp.outline("#0a0412")
    sp.put(1, 2, hexrgb("#c9a6ff"))
    sp.put(14, 13, hexrgb("#c9a6ff"))
    return sp


def beast_hide():
    """A flat brown hide with a neck, four short legs, a stitched seam and a darker back."""
    sp = Sprite()
    left = [
        "........",
        "....MMMM",
        "....MLMM",
        ".MM.MMMM",
        ".MMMMMMM",
        "..MMLMMM",
        "..MMMMMs",
        "..MMMMMM",
        "..MMLMMs",
        ".MMMMMMM",
        ".MMMMMMM",
        ".MM.MMMM",
        ".MM.MMMM",
        "........",
        "........",
        "........",
    ]
    rows = mirrored(left, {"L": "D"})
    pal = {"M": "#9a6a3c", "L": "#c89a62", "D": "#7a4f2a", "s": "#e2d2a6"}
    sp.draw_map(rows, pal)
    for y in range(4, 12):                                               # the stitched seam down the middle, and the darker back
        sp.put(7, y, hexrgb("#7a4f2a"))
        sp.put(8, y, hexrgb("#e2d2a6") if y % 2 == 0 else hexrgb("#6e4624"))
    for x, y in ((3, 9), (12, 9)):
        sp.put(x, y, hexrgb("#7a4f2a"))
    sp.outline("#1c1008")
    return sp


def juggernaut_heart():
    """The Dungeon's own heart as a Juggernaut carries it: dark red, veined with stone, a crack of white-hot light through it."""
    sp = Sprite()
    rows = [
        "................",
        "...hhh....hhh...",
        "..hRRRh..hRRRh..",
        ".hRLLRRhhRRRRRh.",
        ".hRLLRRRRRRRRRh.",
        ".hRRRRRRRRRRRDh.",
        ".hRRRRRRRRRRRDh.",
        "..hRRRRRRRRRDh..",
        "..hRRRRRRRRDDh..",
        "...hRRRRRRDDh...",
        "....hRRRRDDh....",
        ".....hRRDDh.....",
        "......hRDh......",
        ".......hh.......",
        "................",
        "................",
    ]
    pal = {"h": "#4a0b14", "R": "#a3142a", "L": "#e8505e", "D": "#6a0c1c"}
    sp.draw_map(rows, pal)
    crack = [(8, 4), (7, 5), (8, 6), (9, 7), (8, 8), (7, 9), (8, 10)]   # the crack of light
    for x, y in crack:
        sp.put(x, y, hexrgb("#ffd36a"))
    for x, y in ((8, 5), (9, 6), (7, 8), (8, 9)):
        sp.put(x, y, hexrgb("#ff8a2a"))
    for x, y in ((4, 6), (5, 8), (11, 5), (10, 9)):                        # veins of grey stone
        sp.put(x, y, hexrgb("#5e5560"))
    sp.outline("#14040a")
    sp.put(2, 1, hexrgb("#ff8a2a"))
    sp.put(14, 12, hexrgb("#ff8a2a"))
    return sp


MATERIALS = [
    ("goblin_dagger", goblin_dagger),
    ("kobold_fang", kobold_fang),
    ("slime_core", slime_core),
    ("aqua_slime_gel", aqua_slime_gel),
    ("direwolf_pelt", direwolf_pelt),
    ("lizardman_scale", lizardman_scale),
    ("water_spirit_essence", water_spirit_essence),
    ("megalodon_tooth", megalodon_tooth),
    ("devil_horn", devil_horn),
    ("disciple_sigil", disciple_sigil),
    ("beast_hide", beast_hide),
    ("juggernaut_heart", juggernaut_heart),
]


def main():
    for name, make in MATERIALS:
        save(name, make())
    print("wrote %d sprites and models" % len(MATERIALS))


if __name__ == "__main__":
    main()
