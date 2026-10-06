"""Pixel-art swords (0.33), in the style of the owner's references (the ice-blue runeblade, the @LoFolyCookie arsenal sheet):
chunky 32x32 sprites, a 4-step palette ramp per material lit from the top-left, the outline taken from each material's own
shadow tone, and the mana on its own full-bright layer so it glows.

    python tools/gen_pixel_swords.py   ->  textures/item/<sword>.png (+ <sword>_glow.png when it has mana)
                                           models/item/<sword>.json
                                           build/weapons/pixel_swords.png (preview, not shipped)

Replaces the 0.28 smooth 128 px sprites (tools/gen_weapon_textures.py) and the old rapier / greatsword sprites.
The Rimeheart Runeblade has its own script (tools/gen_runeblade_texture.py) and is only shown in the preview.

Coordinates (same as the runeblade): every pixel has
    d = x - y        along the sword: about -25 at the pommel (bottom-left) .. 27 at the tip (top-right)
    s = x + y - 31   across it: negative = the upper-left side, which faces the light
For one d only every other s exists; that is what keeps diagonal pixel lines clean.
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item")
MODELS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "models", "item")
PREVIEW = os.path.join(ROOT, "build", "weapons")
N = 32

# ------------------------------------------------------------------------------------------- palettes (shadow, base, light, glint)
BLACK_STEEL = ["#15151c", "#272732", "#3e3e4e", "#6a6a82"]
BLACK_EDGE = ["#3a3a48", "#5c5c6e", "#9a9aae", "#dcdce8"]
RUST = ["#3c2018", "#5a3022", "#764030", "#9a5a40"]
IRON = ["#2c3038", "#4a505c", "#767e8e", "#b0b8ca"]
WHITE_STEEL = ["#8f97a8", "#c3cad8", "#e6ebf4", "#ffffff"]
GOLD = ["#6b4a12", "#a87a22", "#dcb04a", "#fff0b0"]
SILVER = ["#5b6474", "#9aa4b5", "#d3dbe8", "#ffffff"]
STEEL = ["#4a5160", "#7a8394", "#aeb7c6", "#e8eef8"]
LEATHER = ["#1e1410", "#352218", "#4e3426", "#6c4a36"]
WRAP_BLACK = ["#0e0e12", "#1c1c24", "#2c2c38", "#44445a"]
WRAP_RED = ["#3a0a0e", "#6a1218", "#9a1e24", "#c83a3a"]
WRAP_WHITE = ["#8a8a90", "#bdbdc4", "#e2e2e8", "#ffffff"]
WRAP_CREAM = ["#9a8f78", "#c8bc9e", "#e6dcc0", "#fffaf0"]
CRIMSON = ["#6a0810", "#b0101c", "#ff3040", "#ffb0b0"]      # Asta's anti-magic edge (emissive)
GOLD_MANA = ["#9a6a10", "#e0a830", "#ffe070", "#fffbe0"]    # Licht's light mana (emissive)
BLUE_MANA = ["#0a6ab0", "#1fa2f0", "#7fd4ff", "#e8f8ff"]    # rapier / greatsword runes (emissive)


def rgb(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


def hsh(*k):
    x = 0x9E3779B1
    for v in k:
        x = ((x ^ (v & 0xFFFFFFFF)) * 0x85EBCA6B) & 0xFFFFFFFF
        x ^= x >> 13
    return x


class Sword:
    def __init__(self):
        self.base, self.glow = {}, {}

    def cells(self):
        for y in range(N):
            for x in range(N):
                yield x, y, x - y, x + y - 31

    def put(self, x, y, h):
        self.base[(x, y)] = h

    def shine(self, x, y, h):
        self.glow[(x, y)] = h

    @staticmethod
    def lit(s, centre=0):
        """Ramp index for a body pixel: light side 2, centre 1, shadow side 0."""
        return 2 if s < centre else 1 if s == centre else 0

    # ------------------------------------------------------------------------------- parts
    def pommel_round(self, dc, r, pal, gem=None):
        for x, y, d, s in self.cells():
            if abs(d - dc) + abs(s) <= r + (1 if abs(d - dc) < r and abs(s) < r else 0):
                self.put(x, y, pal[3] if (d == dc + 1 and s == -1) else pal[self.lit(s) + (1 if abs(d - dc) <= 1 and abs(s) <= 1 else 0)])
                if gem and abs(d - dc) <= 0 and abs(s) <= 1:
                    self.put(x, y, gem[0])
                    self.shine(x, y, gem[2] if s == 0 else gem[1])

    def pommel_ring(self, dc, r, pal):
        """A ring pommel (hollow)."""
        for x, y, d, s in self.cells():
            m = abs(d - dc) + abs(s)
            if r - 1 <= m <= r + 1 and not (abs(d - dc) <= 1 and abs(s) <= 1):
                self.put(x, y, pal[self.lit(s) + (1 if d > dc else 0)])

    def pommel_spike(self, dc, pal):
        for x, y, d, s in self.cells():
            if (d == dc and abs(s) <= 2) or (s == 0 and dc - 2 <= d <= dc):
                self.put(x, y, pal[self.lit(s) + (1 if s == 0 else 0)])

    def grip(self, d0, d1, w, pal, style="wrap", alt=None):
        for x, y, d, s in self.cells():
            if d0 <= d <= d1 and abs(s) <= w:
                if style == "wrap":          # raised band every third row
                    band = d % 3 == 0
                    i = (2 if band else 1) + (1 if s < 0 and band else 0) - (1 if s > 0 else 0)
                    self.put(x, y, pal[max(0, min(3, i))])
                elif style == "spiral":      # bands running round it
                    self.put(x, y, (alt if (d + s) % 4 in (0, 1) else pal)[1 + (1 if s < 0 else 0) - (1 if s > 0 else 0)])
                elif style == "ito":         # katana diamond wrap: cord over a contrasting base
                    cross = (d + s) % 4 == 0 or (d - s) % 4 == 0
                    self.put(x, y, (alt if cross else pal)[1 + (1 if s < 0 else 0)])

    def guard_bar(self, d0, d1, half, pal, tips=0):
        """Straight crossguard; tips > 0 flares the ends toward the blade."""
        for x, y, d, s in self.cells():
            a = abs(s)
            if (d0 <= d <= d1 and a <= half) or (tips and half - tips < a <= half + 1 and d1 < d <= d1 + (a - (half - tips))):
                self.put(x, y, pal[min(3, self.lit(s) + (1 if d == d1 else 0))])

    def guard_round(self, dc, r, pal):
        """Round katana tsuba seen edge-on: a short wide oval."""
        for x, y, d, s in self.cells():
            if abs(d - dc) <= 1 and abs(s) <= r:
                self.put(x, y, pal[min(3, self.lit(s) + (1 if d == dc + 1 else 0))])
            elif abs(d - dc) == 2 and abs(s) <= r - 2:
                self.put(x, y, pal[0])

    def guard_four(self, dc, pal, half=6):
        """Four-sided ornate guard: a diamond with long points across the blade and short ones along it."""
        for x, y, d, s in self.cells():
            a, e = abs(s), abs(d - dc)
            if (e == 0 and a <= half) or (e == 1 and a <= half - 2) or (e == 2 and a <= 1) or (a == 0 and e <= 3):
                self.put(x, y, pal[min(3, self.lit(s) + (1 if e == 0 and a <= 1 else 0))])

    def guard_arrow(self, dc, pal, half=5):
        """Arrow-shaped guard pointing at the blade."""
        for x, y, d, s in self.cells():
            a = abs(s)
            if a <= half and dc - 1 <= d <= dc and d >= dc - 1 + (a >= half - 1):
                self.put(x, y, pal[self.lit(s) + (1 if d == dc else 0)])
            elif a <= half - 2 and d == dc + 1:
                self.put(x, y, pal[self.lit(s)])

    def guard_swept(self, dc, pal):
        """A rapier's swept hilt: a small bar plus a knuckle loop curving back down the grip on the shadow side."""
        for x, y, d, s in self.cells():
            a = abs(s)
            if d == dc and a <= 4:
                self.put(x, y, pal[self.lit(s) + 1])
            elif s in (3, 4) and dc - 9 <= d < dc:                     # the knuckle loop
                self.put(x, y, pal[1 if s == 3 else 0])
            elif d == dc - 9 and 0 < s <= 4:
                self.put(x, y, pal[1])

    def blade(self, d0, d1, width, body, edge, curve=None, fuller=None, edge_side="both", tip_glint=True):
        """width(d) -> half width (0 = single pixel line); curve(d) -> centre offset in s (katanas)."""
        for x, y, d, s in self.cells():
            if not (d0 <= d <= d1):
                continue
            c = int(round(curve(d))) if curve else 0
            w = width(d)
            if w < 0:
                continue
            a = s - c
            if abs(a) > w:
                continue
            on_edge = abs(a) == w and w > 0 and (edge_side == "both" or (edge_side == "outer" and a > 0) or (edge_side == "inner" and a < 0))
            if on_edge:
                self.put(x, y, edge[2 if a < 0 else 1])
            elif fuller and fuller(d) and a == 0:
                self.put(x, y, body[0])
            else:
                self.put(x, y, body[2 if a < 0 else 1 if a == 0 else 0] if w > 0 else body[2])
            if tip_glint and d >= d1 - 1:
                self.put(x, y, edge[3])

    def speckle(self, pal_from, pal_to, rate, seed, region=lambda d, s: True):
        """Rust / wear: recolour some pixels of one ramp into another (keeps the light step)."""
        for (x, y), h in list(self.base.items()):
            d, s = x - y, x + y - 31
            if h in pal_from and region(d, s) and hsh(x, y, seed) % 100 < rate:
                self.base[(x, y)] = pal_to[pal_from.index(h)]

    def chips(self, d0, d1, width, seed, rate=22):
        """Nicks in the edges: drop some edge pixels."""
        for (x, y) in list(self.base):
            d, s = x - y, x + y - 31
            if d0 <= d <= d1 and abs(s) == width(d) and hsh(x, y, seed) % 100 < rate:
                del self.base[(x, y)]

    def image(self):
        b = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        for (x, y), h in self.base.items():
            b.putpixel((x, y), rgb(h))
        g = None
        if self.glow:
            g = Image.new("RGBA", (N, N), (0, 0, 0, 0))
            for (x, y), h in self.glow.items():
                g.putpixel((x, y), rgb(h))
        return b, g


# ------------------------------------------------------------------------------------------- the swords
def taper(full, start, end=27, last=0):
    """Half width: `full` up to `start`, then narrowing to `last` at the tip."""
    return lambda d: full if d <= start else max(last, int(round(full - (full - last) * (d - start) / max(1, end - start))))


def demon_slayer():
    """Asta's first sword: huge, dirty black, chipped, pointed tip, the base angled inward to the guard, a narrow fuller."""
    sw = Sword()
    sw.pommel_round(-24, 2, IRON)
    sw.grip(-21, -9, 1, LEATHER)
    sw.guard_bar(-9, -7, 5, IRON)
    width = lambda d: 2 if d <= -5 else 3 if d <= -3 else 4 if d <= 19 else max(0, int(round(4 - 4 * (d - 19) / 8)))
    sw.blade(-6, 27, width, BLACK_STEEL, BLACK_EDGE, fuller=lambda d: d <= 8)
    sw.chips(-2, 20, width, 5)
    sw.speckle(BLACK_STEEL, RUST, 7, 6)
    return sw


def demon_dweller(white=False):
    """Long slender blade, stepped fuller with markings, four-sided ornate guard, spiral grip, sphere pommel."""
    sw = Sword()
    metal, body, edge = (GOLD, WHITE_STEEL, WHITE_STEEL) if white else (IRON, BLACK_STEEL, BLACK_EDGE)
    sw.pommel_round(-24, 2, metal)
    sw.grip(-21, -10, 1, WRAP_CREAM if white else WRAP_BLACK, "spiral", GOLD if white else IRON)
    sw.guard_four(-7, metal, half=7)
    width = taper(2, 21)
    sw.blade(-4, 27, width, body, edge, fuller=lambda d: 0 <= d <= 20)
    for x, y, d, s in sw.cells():                                        # the markings beside the fuller
        if 1 <= d <= 19 and abs(s) == 1 and d % 3 == 0:
            if white:
                sw.put(x, y, GOLD_MANA[0])
                sw.shine(x, y, GOLD_MANA[2])
            else:
                sw.put(x, y, BLACK_EDGE[2])
    if not white:
        sw.speckle(BLACK_STEEL, RUST, 3, 7)
    return sw


def demon_destroyer(white=False):
    """Broad blade with a bite torn out near the guard; the end flares wide and carries a clover."""
    sw = Sword()
    metal, body, edge = (GOLD, WHITE_STEEL, WHITE_STEEL) if white else (IRON, BLACK_STEEL, BLACK_EDGE)
    sw.pommel_round(-24, 2, metal)
    sw.grip(-21, -10, 1, WRAP_CREAM if white else LEATHER)
    sw.guard_arrow(-8, metal, half=6)
    width = lambda d: 3 if d <= 12 else 4 if d <= 15 else 5 if d <= 21 else max(0, int(round(5 - 5 * (d - 21) / 6)))
    sw.blade(-6, 27, width, body, edge, fuller=lambda d: d <= 13)
    for (x, y) in list(sw.base):                                        # the bite out of the upper edge
        d, s = x - y, x + y - 31
        if 0 <= d <= 6 and s <= -2 and abs(s) >= 3 - (1 if d in (2, 3, 4) else 0) - (1 if d == 3 else 0):
            del sw.base[(x, y)]
    clover = {(17, 0), (19, -2), (19, 2), (19, 0), (21, 0), (16, -1), (16, 1)}   # three leaves round a centre, stem toward the guard
    for x, y, d, s in sw.cells():
        if (d, s) in clover or (d == 18 and abs(s) == 1) or (d == 20 and abs(s) == 1):
            if white:
                sw.put(x, y, GOLD_MANA[0])
                sw.shine(x, y, GOLD_MANA[2])
            else:
                sw.put(x, y, BLACK_STEEL[0] if (d, s) not in clover else "#06060a")
    if not white:
        sw.speckle(BLACK_STEEL, RUST, 5, 8)
    return sw


def katana(body, edge, wrap, cord, tsuba, glow_edge=None, hamon=None):
    sw = Sword()
    sw.pommel_spike(-24, tsuba)
    sw.grip(-23, -9, 1, wrap, "ito", cord)
    sw.guard_round(-7, 4, tsuba)
    curve = lambda d: -2.2 * max(0.0, (d + 4) / 31.0) ** 2              # sori: the blade bends toward the upper side
    width = lambda d: 1 if d <= 23 else 0
    sw.blade(-4, 27, width, body, edge, curve=curve, edge_side="outer")
    for x, y, d, s in sw.cells():
        c = int(round(curve(d)))
        if -3 <= d <= 25 and s - c == 1:                                 # the cutting edge (outer side)
            if hamon and (d % 4 in (0, 1)):
                sw.put(x, y, hamon)
            if glow_edge:
                sw.put(x, y, glow_edge[0])
                sw.shine(x, y, glow_edge[2] if d % 3 else glow_edge[3])
    return sw


def demon_slasher():
    """Asta's katana: slim curved black blade, crimson anti-magic edge, round black tsuba, red-over-black wrap."""
    return katana(BLACK_STEEL, BLACK_EDGE, WRAP_BLACK, WRAP_RED, IRON, glow_edge=CRIMSON)


def yami_katana():
    """Yami's katana: plain curved steel, wavy temper line, dark tsuba, black-and-white wrap."""
    return katana(STEEL, STEEL, WRAP_BLACK, WRAP_WHITE, IRON, hamon="#f4f8ff")


def spell_forged_rapier():
    """A needle-thin rapier with a swept gold hilt and a line of blue spell runes."""
    sw = Sword()
    sw.pommel_round(-24, 1, GOLD)
    sw.grip(-22, -11, 0, LEATHER)
    sw.guard_swept(-10, GOLD)
    sw.blade(-9, 27, lambda d: 1 if d <= 2 else 0, STEEL, STEEL)
    for x, y, d, s in sw.cells():
        if 0 <= d <= 24 and s == 0 and d % 4 == 1:
            sw.put(x, y, BLUE_MANA[0])
            sw.shine(x, y, BLUE_MANA[2])
    return sw


def severing_greatsword():
    """A wide silver greatsword with a broad gold crossguard and a blue gem."""
    sw = Sword()
    sw.pommel_round(-24, 2, GOLD, gem=BLUE_MANA)
    sw.grip(-21, -10, 1, LEATHER)
    sw.guard_bar(-9, -7, 7, GOLD, tips=2)
    for x, y, d, s in sw.cells():
        if -9 <= d <= -7 and abs(s) <= 1:
            sw.put(x, y, BLUE_MANA[0])
            sw.shine(x, y, BLUE_MANA[2] if s == 0 and d == -8 else BLUE_MANA[1])
    width = lambda d: 4 if d <= 18 else max(0, int(round(4 - 4 * (d - 18) / 9)))
    sw.blade(-6, 27, width, SILVER, SILVER, fuller=lambda d: d <= 16)
    return sw


SWORDS = {
    # item id: (painter, third-person scale, first-person scale)
    "demon_slayer_sword": (demon_slayer, 1.9, 1.25),
    "demon_dweller_sword": (lambda: demon_dweller(False), 1.65, 1.1),
    "demon_destroyer_sword": (lambda: demon_destroyer(False), 1.75, 1.15),
    "demon_slasher_katana": (demon_slasher, 1.5, 1.05),
    "miasma_infused_katana": (yami_katana, 1.5, 1.05),
    "licht_dweller_sword": (lambda: demon_dweller(True), 1.65, 1.1),
    "licht_destroyer_sword": (lambda: demon_destroyer(True), 1.75, 1.15),
    "spell_forged_rapier": (spell_forged_rapier, 1.35, 1.0),
    "severing_greatsword": (severing_greatsword, 1.85, 1.2),
}


def model(name, tp, fp, glow):
    lift = 4 + (tp - 1) * 3.2
    m = {}
    if glow:
        m["loader"] = "neoforge:item_layers"
    m["parent"] = "minecraft:item/handheld"
    m["textures"] = {"layer0": f"nusmp:item/{name}"}
    if glow:
        m["textures"]["layer1"] = f"nusmp:item/{name}_glow"
        m["neoforge_data"] = {"layers": {"1": {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}}}
    r = lambda v: round(v, 3)
    m["display"] = {
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, r(lift), 0.5], "scale": [r(tp * 0.85)] * 3},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, r(lift), 0.5], "scale": [r(tp * 0.85)] * 3},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, r(3.2 + (fp - 1) * 2), 1.13], "scale": [r(fp * 0.68)] * 3},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, r(3.2 + (fp - 1) * 2), 1.13], "scale": [r(fp * 0.68)] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    }
    return m


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    tiles = []
    for name, (fn, tp, fp) in SWORDS.items():
        b, g = fn().image()
        if g is not None:                                                # never a hole under a glow pixel
            for y in range(N):
                for x in range(N):
                    if g.getpixel((x, y))[3] and not b.getpixel((x, y))[3]:
                        b.putpixel((x, y), g.getpixel((x, y)))
        b.save(os.path.join(TEX, name + ".png"))
        glow_path = os.path.join(TEX, name + "_glow.png")
        if g is not None:
            g.save(glow_path)
        elif os.path.exists(glow_path):
            os.remove(glow_path)
        with open(os.path.join(MODELS, name + ".json"), "w", newline="\n") as f:
            json.dump(model(name, tp, fp, g is not None), f, indent=2)
            f.write("\n")
        comp = b.copy()
        if g is not None:
            comp.alpha_composite(g)
        tiles.append(comp)
    rb = os.path.join(TEX, "rimeheart_runeblade.png")
    if os.path.exists(rb):
        comp = Image.open(rb).convert("RGBA")
        comp.alpha_composite(Image.open(os.path.join(TEX, "rimeheart_runeblade_glow.png")).convert("RGBA"))
        tiles.append(comp)
    Z, cols = 8, 5
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * (N * Z + 8) + 8, rows * (N * Z + 8) + 8), (18, 22, 30, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t.resize((N * Z, N * Z), Image.NEAREST), (8 + (i % cols) * (N * Z + 8), 8 + (i // cols) * (N * Z + 8)))
    sheet.save(os.path.join(PREVIEW, "pixel_swords.png"))
    print("wrote", len(SWORDS), "swords")


if __name__ == "__main__":
    main()
