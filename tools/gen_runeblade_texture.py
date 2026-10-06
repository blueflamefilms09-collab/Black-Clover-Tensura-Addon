"""Rimeheart Runeblade (0.32): pixel-art sprite after the owner's ice-blue runeblade reference.

    python tools/gen_runeblade_texture.py  ->  textures/item/rimeheart_runeblade.png          (base layer, 32x32)
                                                textures/item/rimeheart_runeblade_glow.png     (emissive mana layer, 32x32)
                                                build/weapons/runeblade_preview.png            (not shipped)

Drawn on the vanilla diagonal (pommel bottom-left, tip top-right) in two coordinates per pixel:
    d = x - y        along the weapon (-25 at the pommel .. 27 at the tip)
    s = x + y - 31   across it (0 on the centre line; only every other value exists for a given d, which is what makes a clean
                     diagonal pixel line)
Base layer = steel, silver and grip with a 4-step palette ramp per material (shadow / base / light / glint), lit from the top-left.
Glow layer = only the cyan mana pixels (runes, gem cores, the edge glow near the tip); the item model renders it full-bright so it
reads as emissive, and shader packs pick it up as a separate layer.
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item")
PREVIEW = os.path.join(ROOT, "build", "weapons")
N = 32

# palette ramps: shadow, base, light, glint (hex in docs/runeblade_spec.md)
STEEL = ["#0d1430", "#1c2a5c", "#34488f", "#6d86d6"]      # the blade: deep navy steel
EDGE = ["#3a4f8c", "#5a6fae", "#9fb6e8", "#e6eeff"]       # honed edge
SILVER = ["#5b6474", "#9aa4b5", "#d3dbe8", "#ffffff"]     # guard and pommel
GRIP = ["#141824", "#252c40", "#3b4560", "#56617f"]       # wrapped grip
MANA = ["#0a8fb0", "#1fd2f0", "#7ff4ff", "#e8ffff"]       # the emissive cyan mana


def rgba(h, a=255):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), a)


def cells():
    for y in range(N):
        for x in range(N):
            yield x, y, x - y, x + y - 31


def light(s, d):
    """Ramp index from the top-left light: the s < 0 side faces the light."""
    return 2 if s < 0 else 1 if s == 0 else 0


def paint():
    base = {}
    glow = {}

    def put(x, y, col):
        base[(x, y)] = col

    def shine(x, y, col):
        glow[(x, y)] = col

    for x, y, d, s in cells():
        a = abs(s)
        # ---------------- pommel: a spiked silver crown round a mana gem (d -25 .. -20)
        if -25 <= d <= -20:
            c = -23
            r = abs(d - c)
            if a + r <= 3:
                put(x, y, SILVER[light(s, d) + (1 if r == 0 else 0)])
                if a <= 1 and r <= 1:
                    shine(x, y, MANA[2 if a == 0 and r == 0 else 1])
            elif (d == c and a in (4, 5)) or (a == 0 and d in (c - 4, c + 4)) or (a == 2 and r == 2 and d < c):
                put(x, y, SILVER[2 if s < 0 else 1])                   # the crown's spikes
        # ---------------- grip (d -19 .. -5): wrapped, every third row a raised band
        elif -19 <= d <= -7 and a <= 1:
            band = (d % 3 == 0)
            put(x, y, GRIP[(2 if band else 1) + (1 if s < 0 and band else 0) - (1 if s > 0 else 0)])
        # ---------------- crossguard (d -4 .. 0): swept, spiked wings of silver with a mana core
        elif -6 <= d <= 2:
            wing = (-3 <= d <= -2 and a <= 11)                         # the main bar
            sweep = (9 <= a <= 14 and d == -3 + (a - 9))               # wing tips sweep toward the blade
            sweep2 = (10 <= a <= 13 and d == -2 + (a - 10))
            fin = (a in (5, 6) and -1 <= d <= 1 and d <= -1 + (a - 4))  # inner fins
            spur = (a == 7 and d == -4)                                # small spurs back toward the grip
            back = (10 <= a <= 12 and d == -4 - (a - 10))  # wing tips also hook back
            core = (a <= 2 and -4 <= d <= 0)
            if a <= 1 and d <= -5:
                put(x, y, GRIP[1 - (1 if s > 0 else 0)])                # the grip runs on under the hooked wings
            elif core:
                put(x, y, SILVER[3 if (a == 0 and d == -2) else light(s, d) + 1])
                if a <= 1 and -3 <= d <= -1:
                    shine(x, y, MANA[3 if (a == 0 and d == -2) else 2])
            elif wing or sweep or sweep2 or fin or spur or back:
                put(x, y, SILVER[min(3, 1 + (1 if s < 0 else 0) + (1 if sweep or sweep2 else 0))])
                if (sweep and a in (12, 13)) or (wing and a == 6):
                    shine(x, y, MANA[1])                               # mana nodes in the wings
        # ---------------- blade (d 1 .. 27): navy steel, light edge on one side, mana runes and a barbed fin near the tip
        elif 1 <= d <= 27:
            w = 2 if d <= 20 else max(0, int((27 - d) * 0.35 + 0.5))
            if a <= w:
                if a == w and w > 0:
                    put(x, y, EDGE[2 if s < 0 else 1])                 # the honed edges
                elif a == 0:
                    put(x, y, STEEL[0])                                # the dark fuller
                else:
                    put(x, y, STEEL[2 if s < 0 else 1])
                if d >= 25:
                    put(x, y, EDGE[3])
                # runes down the fuller: one every third pixel, brighter toward the tip
                if a == 0 and 3 <= d <= 18 and d % 3 == 0:
                    shine(x, y, MANA[1 if d < 12 else 2])
                # the tip runs with mana along the light edge
                if d >= 19 and s < 0:
                    shine(x, y, MANA[2 if d >= 23 else 1])
            elif (d in (17, 18) and s in (-3, -4)) or (d == 16 and s == -4):
                put(x, y, EDGE[2])                                     # the barbed fin
                shine(x, y, MANA[0])
    return base, glow


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    base, glow = paint()
    b = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    g = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    for (x, y), c in base.items():
        b.putpixel((x, y), rgba(c))
    for (x, y), c in glow.items():
        g.putpixel((x, y), rgba(c))
        if (x, y) not in base:
            b.putpixel((x, y), rgba(MANA[0]))                          # never leave a hole under a glow pixel
    b.save(os.path.join(TEX, "rimeheart_runeblade.png"))
    g.save(os.path.join(TEX, "rimeheart_runeblade_glow.png"))
    # preview: base, glow, and the two composited, zoomed 10x on a dark backdrop
    Z = 10
    sheet = Image.new("RGBA", (3 * N * Z + 40, N * Z + 20), (14, 16, 26, 255))
    comp = b.copy()
    comp.alpha_composite(g)
    for i, im in enumerate((b, g, comp)):
        sheet.alpha_composite(im.resize((N * Z, N * Z), Image.NEAREST), (10 + i * (N * Z + 10), 10))
    sheet.save(os.path.join(PREVIEW, "runeblade_preview.png"))
    print("wrote rimeheart_runeblade (+ glow)")


if __name__ == "__main__":
    main()
