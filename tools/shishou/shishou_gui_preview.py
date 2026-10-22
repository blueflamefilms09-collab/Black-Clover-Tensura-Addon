"""Why: nothing of the game can be run where this mod is built, so this makes a MOCK of Shishou's Akashic GUIs with Pillow: the Stomach screen (3 rows, as the default config) and the HUD with an Appraisal readout,
drawn from the real atlas (textures/gui/shishou/akashic.png) with the same numbers and the same order of drawing as client/shishou/AkashicTheme.java, ShishouStomachScreen.java and ShishouHud.java
(constants repeated from shishou/ShishouGuiLayout.java). It is a MOCK: fonts, item sprites and the exact blending of the game differ.

    python3 tools/shishou/shishou_gui_preview.py        # writes docs/previews/shishou/gui_stomach_mock.png, gui_hud_mock.png, gui_atlas.png
"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
ATLAS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "shishou", "akashic.png")
OUT = os.path.join(ROOT, "docs", "previews", "shishou")
SC = 4

REG = {"star": (0, 0, 64, 64), "emblem": (64, 0, 32, 32), "mote": (96, 0, 8, 8), "corner_tl": (104, 0, 16, 16), "corner_tr": (120, 0, 16, 16), "corner_bl": (136, 0, 16, 16),
       "corner_br": (152, 0, 16, 16), "prism": (0, 64, 128, 2), "slotrow": (0, 96, 162, 18), "gauge": (0, 128, 64, 8)}
CYAN, CYAN_HI, VIOLET, GOLD = (92, 216, 255), (127, 235, 255), (155, 107, 255), (255, 215, 106)
INK_TOP, INK_BOT = (6, 8, 24), (26, 15, 58)
EXP_LOW, EXP_HIGH, MANA_LOW, MANA_HIGH = (0xB8, 0x86, 0x2B), (0xFF, 0xE9, 0xA0), (0x2D, 0x8F, 0xC4), (0xBF, 0xF4, 0xFF)


def font(size):
    for name in ("DejaVuSans.ttf", "DejaVuSansMono.ttf", "LiberationSans-Regular.ttf"):
        for base in ("/usr/share/fonts/truetype/dejavu", "/usr/share/fonts/truetype/liberation", "/usr/share/fonts"):
            p = os.path.join(base, name)
            if os.path.exists(p):
                return ImageFont.truetype(p, size)
    return ImageFont.load_default()


class Canvas:
    """A 1x pixel canvas that is scaled up with nearest neighbour at the end, text is drawn after the scale-up so it stays crisp."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.texts = []
        self.atlas = Image.open(ATLAS).convert("RGBA")

    def region(self, name):
        u, v, w, h = REG[name]
        return self.atlas.crop((u, v, u + w, v + h))

    def blit(self, name, x, y, w=None, h=None, alpha=1.0, add=False):
        r = self.region(name)
        if w is not None and (w != r.width or h != r.height):
            r = r.resize((w, h), Image.NEAREST)
        if alpha < 1.0:
            a = r.getchannel("A").point(lambda v: int(v * alpha))
            r.putalpha(a)
        if add:
            base = self.img.crop((x, y, x + r.width, y + r.height))
            px, rp = base.load(), r.load()
            for yy in range(r.height):
                for xx in range(r.width):
                    cr, cg, cb, ca = px[xx, yy]
                    sr, sg, sb, sa = rp[xx, yy]
                    k = sa / 255.0
                    px[xx, yy] = (min(255, int(cr + sr * k)), min(255, int(cg + sg * k)), min(255, int(cb + sb * k)), max(ca, 255 if ca else int(255 * k)))
            self.img.paste(base, (x, y))
        else:
            self.img.alpha_composite(r, (x, y)) if x >= 0 and y >= 0 else self.img.alpha_composite(r, (max(0, x), max(0, y)), (max(0, -x), max(0, -y)))

    def fill(self, x0, y0, x1, y1, rgb, a=1.0):
        d = Image.new("RGBA", (max(0, x1 - x0), max(0, y1 - y0)), rgb + (int(255 * a),))
        if d.width and d.height:
            self.img.alpha_composite(d, (x0, y0)) if x0 >= 0 and y0 >= 0 else None

    def gradient(self, x0, y0, x1, y1, top, bot, a=1.0):
        for y in range(y0, y1):
            t = (y - y0) / max(1, y1 - y0 - 1)
            col = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
            self.fill(x0, y, x1, y + 1, col, a)

    def outline(self, x, y, w, h, rgb, a):
        self.fill(x, y, x + w, y + 1, rgb, a)
        self.fill(x, y + h - 1, x + w, y + h, rgb, a)
        self.fill(x, y + 1, x + 1, y + h - 1, rgb, a)
        self.fill(x + w - 1, y + 1, x + w, y + h - 1, rgb, a)

    def text(self, s, x, y, rgb, size=8, shadow=True):
        self.texts.append((s, x, y, rgb, size, shadow))

    def finish(self):
        big = self.img.resize((self.w * SC, self.h * SC), Image.NEAREST)
        d = ImageDraw.Draw(big)
        for s, x, y, rgb, size, shadow in self.texts:
            f = font(size * SC)
            if shadow:
                d.text((x * SC + SC, y * SC + SC - 2), s, font=f, fill=(20, 24, 36, 255))
            d.text((x * SC, y * SC - 2), s, font=f, fill=rgb + (255,))
        return big


def panel(c, x, y, w, h, now, prism_y):
    c.gradient(x, y, x + w, y + h, INK_TOP, INK_BOT)
    ox, oy = int(now * 0.08) % 64, int(now * 0.03) % 64
    star = c.region("star")
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for ty in range(-oy, h, 64):
        for tx in range(-ox, w, 64):
            layer.alpha_composite(star, (tx, ty), (0, 0)) if tx >= 0 and ty >= 0 else layer.alpha_composite(star, (max(0, tx), max(0, ty)), (max(0, -tx), max(0, -ty)))
    layer.putalpha(layer.getchannel("A").point(lambda v: int(v * 0.5)))
    c.img.alpha_composite(layer, (x, y))
    breath = 0.5 + 0.5 * math.sin(now / 120.0 * math.tau)
    for k, (rgb, a) in enumerate(((CYAN, 0.05 + 0.07 * breath), (CYAN, 0.18 + 0.14 * breath))):
        c.outline(x - 2 + k, y - 2 + k, w + 4 - 2 * k, h + 4 - 2 * k, rgb, a) if x >= 2 and y >= 2 else None
    c.outline(x, y, w, h, CYAN_HI, 0.75)
    c.outline(x + 1, y + 1, w - 2, h - 2, VIOLET, 0.28)
    c.blit("corner_tl", x, y)
    c.blit("corner_tr", x + w - 16, y)
    c.blit("corner_bl", x, y + h - 16)
    c.blit("corner_br", x + w - 16, y + h - 16)
    for cy in (y, y + h - 1):
        c.fill(x + w // 2 - 2, cy, x + w // 2 + 3, cy + 1, GOLD)
        c.fill(x + w // 2 - 1, cy - 1, x + w // 2 + 2, cy, GOLD)
        c.fill(x + w // 2 - 1, cy + 1, x + w // 2 + 2, cy + 2, GOLD)
    if prism_y > 0:
        prism(c, x + 6, y + prism_y, w - 12, now)


def prism(c, x, y, w, now):
    c.blit("prism", x, y, w, 2, 0.9)
    sx = x + int((now * 0.9) % (w + 40)) - 20
    s0, s1 = max(x, sx), min(x + w, sx + 16)
    if s1 > s0:
        c.fill(s0, y, s1, y + 1, (255, 255, 255), 0.55)


def motes(c, x, y, w, h, now, n):
    for i in range(n):
        a = 0.375 + 0.225 * math.sin(now * (0.03 + 0.007 * (i % 4)) + i * 1.7)
        mx = x + int(((i * 47 + 13) % w + now * (0.05 + 0.02 * (i % 5))) % w)
        my = y + int(((i * 29 + 7) % h - now * (0.04 + 0.0125 * (i % 5))) % h)
        if mx > x + w - 8 or my > y + h - 8:
            continue
        c.blit("mote", mx, my, alpha=a, add=True)


def gauge(c, x, y, w, h, frac, low, high, now):
    c.fill(x - 1, y - 1, x + w + 1, y + h + 1, CYAN, 0.28)
    c.fill(x, y, x + w, y + h, (3, 5, 15))
    fw = round(max(0, min(1, frac)) * w)
    if fw > 0:
        c.gradient(x, y, x + fw, y + h, high, low)
        sx = x + int((now * 1.1) % (fw + 30)) - 14
        s0, s1 = max(x, sx), min(x + fw, sx + 10)
        if s1 > s0:
            c.fill(s0, y, s1, y + h, (255, 255, 255), 0.30)
        c.fill(x + fw - 1, y - 1, x + fw, y + h + 1, (255, 255, 255), 0.85)
        c.fill(x + fw, y, x + fw + 1, y + h, high, 0.45)
        c.fill(x, y, x + fw, y + 1, (255, 255, 255), 0.28)
    for k in range(1, 10):
        tx = x + w * k // 10
        c.fill(tx, y, tx + 1, y + h, (0, 0, 0) if tx <= x + fw else (255, 255, 255), 0.25 if tx <= x + fw else 0.08)


ITEM_COLORS = [(210, 60, 60), (90, 200, 120), (230, 200, 70), (150, 150, 170), (70, 110, 230), (200, 200, 200)]


def stomach(now=40.0, rows=3, level=57, exp=0.62, mana=0.71):
    W, H = 176, 114 + 18 * rows
    pad = 8
    c = Canvas(W + 2 * pad, H + 2 * pad)
    x, y = pad, pad
    panel(c, x, y, W, H, now, 16)
    motes(c, x + 2, y + 2, W - 4, H - 4, now, 14)
    # the slot rows (menu positions: chest rows at y 18, inventory below, hotbar last)
    slot_rows = [18 + r * 18 for r in range(rows)] + [rows * 18 + 31 + r * 18 for r in range(3)] + [rows * 18 + 89]
    for sy in slot_rows:
        c.blit("slotrow", x + 7, y + sy - 1 + 0)
        for col in range(9):
            if (sy + col) % 3 == 0 and sy < 18 + rows * 18 + 1:
                c.fill(x + 8 + col * 18 + 2, y + sy + 2, x + 8 + col * 18 + 14, y + sy + 14, ITEM_COLORS[(col + sy) % len(ITEM_COLORS)], 0.9)
    c.blit("emblem", x + 5, y + 1, 16, 16)
    gx = x + 118
    gauge(c, gx, y + 4, 50, 4, exp, EXP_LOW, EXP_HIGH, now)
    gauge(c, gx, y + 10, 50, 4, mana, MANA_LOW, MANA_HIGH, now)
    c.text("Stomach", x + 24, y + 6, CYAN_HI, 7)
    c.text("Lv %d" % level, x + 70, y + 6, GOLD, 7)
    c.text("Inventory", x + 8, y + H - 94, (143, 163, 191), 7, False)
    return c.finish()


def hud(now=40.0):
    W = 156
    PAD, ROW, BAR = 5, 11, 5
    lines = ["Ogre", "Health 30 / 30", "Level ~38   Mana ~29", "Weak to: Holy", "Skill: Black Flame"]
    h = PAD + (ROW + 3) + (BAR + 2) + (BAR + 4) + (ROW - 2) + (ROW - 2) + ROW + ROW + (4 + len(lines) * ROW) + PAD
    pad = 8
    c = Canvas(W + 2 * pad, h + 2 * pad)
    x, y0 = pad, pad
    panel(c, x, y0, W, h, now, PAD + ROW - 1)
    motes(c, x + 2, y0 + 2, W - 4, h - 4, now, 6)
    y = y0 + PAD
    c.blit("emblem", x + PAD, y - 3, 12, 12)
    c.text("Shishou", x + PAD + 15, y, CYAN_HI, 7)
    c.text("Lv 57", x + W - PAD - 26, y, GOLD, 7)
    y += ROW + 3
    gauge(c, x + PAD, y, W - 2 * PAD, BAR, 0.62, EXP_LOW, EXP_HIGH, now)
    y += BAR + 2
    gauge(c, x + PAD, y, W - 2 * PAD, BAR, 0.71, MANA_LOW, MANA_HIGH, now)
    y += BAR + 4
    c.text("Mana 1190 / 1670", x + PAD + 1, y - 1, (143, 163, 191), 6, False)
    y += ROW - 2
    c.text("Offence", x + PAD + 1, y - 1, VIOLET, 6, False)
    y += ROW - 2
    c.text("Black Lightning (Kurokaminari)", x + PAD + 1, y, (230, 242, 255), 6, False)
    y += ROW
    c.text("Barrier  Sense", x + PAD + 1, y, (124, 240, 176), 6, False)
    y += ROW
    prism(c, x + PAD, y + 1, W - 2 * PAD, now)
    y += 4
    for i, s in enumerate(lines):
        c.text(s, x + PAD + 1, y, GOLD if i == 0 else (230, 242, 255), 6, i == 0)
        y += ROW
    return c.finish()


def main():
    os.makedirs(OUT, exist_ok=True)
    stomach().save(os.path.join(OUT, "gui_stomach_mock.png"))
    hud().save(os.path.join(OUT, "gui_hud_mock.png"))
    atlas = Image.open(ATLAS).convert("RGBA")
    bg = Image.new("RGBA", atlas.size, (40, 40, 48, 255))
    bg.alpha_composite(atlas)
    bg.resize((atlas.width * 3, atlas.height * 3), Image.NEAREST).save(os.path.join(OUT, "gui_atlas.png"))
    print("wrote the mock previews (MOCK: not a game screenshot) to", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
