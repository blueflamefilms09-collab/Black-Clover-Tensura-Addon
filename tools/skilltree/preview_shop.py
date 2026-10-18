#!/usr/bin/env python3
"""Why: the Black Market screen cannot be run without the game, but it is the part of this job the owner will look at first, so the layout, the colours and the text fit are checked
here with a Pillow mock-up before they are trusted: it draws the shop from the SAME numbers as the Java (client/skilltree/shop/ShopLayout.java, ShopPainter.java, ShopScreen.java) on
top of the repo's own mock of the anime theme (tools/ui_preview/preview_ui.py: panel, well, heading, scrim, magic circle), with the real portrait and cobweb textures, and Minecraft's
proportional text widths. It is NOT the game: the item icons are stand-ins, the light and the font are approximations, and the numbers of the layout are mirrored by hand (the script reads
the constants out of ShopLayout.java and stops when they differ, so it cannot silently drift).

    python3 -B tools/skilltree/preview_shop.py OUT_DIR              # writes shop_*.png into OUT_DIR
    python3 -B tools/skilltree/preview_shop.py OUT_DIR --scale 3    # bigger

The scenes: the common 427x240 GUI with a rich player, hovering the flagship; hovering a good; a poor player refused; the same at 854x480; the tightest 320x180; and the two empty states.
"""
import importlib.util
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA_LAYOUT = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "skilltree", "shop", "ShopLayout.java")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "black_market")
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
SANS_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

spec = importlib.util.spec_from_file_location("preview_ui", os.path.join(ROOT, "tools", "ui_preview", "preview_ui.py"))
pu = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pu)

# ---- the shop's palette (ShopPainter.java)
PLUM, AMBER, PAPER, DIM, FAINT, GOOD, BAD = 0xFFB878E0, 0xFFF0B650, 0xFFEADFCB, 0xFFA09080, 0xFF6A6070, 0xFF9CE8A8, 0xFFE87868
SPEECH = 0xFFEFE2FA
with_alpha, mix = pu.with_alpha, pu.mix

# ---- the layout constants, read from the Java so the mock cannot drift from it
SRC = open(JAVA_LAYOUT, encoding="utf-8").read()


def const(name):
    m = re.search(r"\b%s = (\d+)" % name, SRC)
    if not m:
        raise SystemExit("ShopLayout.java no longer declares %s: update the mock" % name)
    return int(m.group(1))


MAX_W, MAX_H, MIN_W, MIN_H = const("MAX_W"), const("MAX_H"), const("MIN_W"), const("MIN_H")
HEAD_H, FOOT_H, PAD, SCROLL_W, HEADING_H, BULK_H = const("HEAD_H"), const("FOOT_H"), const("PAD"), const("SCROLL_W"), const("HEADING_H"), const("BULK_H")
SHELF_ROW, GOODS_ROW, BUY_W, BUY_H, SELL_W, SELL_H = const("SHELF_ROW"), const("GOODS_ROW"), const("BUY_W"), const("BUY_H"), const("SELL_W"), const("SELL_H")
BULK_PAD, BULK_GAP, BULK_PAD_TIGHT, BULK_GAP_TIGHT = const("BULK_PAD"), const("BULK_GAP"), const("BULK_PAD_TIGHT"), const("BULK_GAP_TIGHT")
assert "0.58f" in SRC and "y + 3, 40, 42" in SRC and "x + w - PAD - 150, y + 7, 150, 34" in SRC, "ShopLayout.of changed: mirror it here"


class R:
    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = x, y, w, h

    right = property(lambda s: s.x + s.w)
    bottom = property(lambda s: s.y + s.h)

    def contains(self, px, py):
        return self.x <= px < self.x + self.w and self.y <= py < self.y + self.h


def jround(v):
    return int(math.floor(v + 0.5))


class Layout:
    """ShopLayout.of(screenW, screenH)."""

    def __init__(self, sw, sh):
        w = max(MIN_W, min(MAX_W, sw - 8))
        h = max(MIN_H, min(MAX_H, sh - 8))
        if sw - 8 < MIN_W:
            w = max(200, sw - 4)
        if sh - 8 < MIN_H:
            h = max(150, sh - 4)
        shelf_w = jround((w - 3 * PAD) * 0.58)
        x, y = (sw - w) // 2, (sh - h) // 2
        self.panel = R(x, y, w, h)
        self.portrait = R(x + PAD, y + 3, 40, 42)
        self.purse = R(x + w - PAD - 150, y + 7, 150, 34)
        body_top, body_bottom = y + HEAD_H + 2, y + h - FOOT_H - 2
        body_h = body_bottom - body_top
        self.shelf_box = R(x + PAD, body_top, shelf_w, body_h)
        self.goods_box = R(self.shelf_box.right + PAD, body_top, w - 3 * PAD - shelf_w, body_h)
        sb, gb = self.shelf_box, self.goods_box
        self.shelf_list = R(sb.x + 2, sb.y + HEADING_H, sb.w - 4 - SCROLL_W - 1, sb.h - HEADING_H - 2)
        self.shelf_bar = R(self.shelf_list.right + 1, self.shelf_list.y, SCROLL_W, self.shelf_list.h)
        self.bulk_row = R(gb.x + 2, gb.y + HEADING_H, gb.w - 4, BULK_H)
        gl_y = self.bulk_row.bottom + 2
        self.goods_list = R(gb.x + 2, gl_y, gb.w - 4 - SCROLL_W - 1, gb.bottom - 2 - gl_y)
        self.goods_bar = R(self.goods_list.right + 1, self.goods_list.y, SCROLL_W, self.goods_list.h)
        self.close = R(x + w - PAD - 54, y + h - FOOT_H + 7, 54, 16)
        self.speech = R(x + PAD, y + h - FOOT_H + 2, self.close.x - PAD - (x + PAD), FOOT_H - 4)
        self.shelf_visible = max(1, self.shelf_list.h // SHELF_ROW)
        self.goods_visible = max(1, self.goods_list.h // GOODS_ROW)

    def shelf_row(self, i, scroll):
        return R(self.shelf_list.x, self.shelf_list.y + (i - scroll) * SHELF_ROW, self.shelf_list.w, SHELF_ROW - 1)

    def goods_row(self, i, scroll):
        return R(self.goods_list.x, self.goods_list.y + (i - scroll) * GOODS_ROW, self.goods_list.w, GOODS_ROW - 1)


def buy_button(row):
    return R(row.right - 3 - BUY_W, row.y + (row.h - BUY_H) // 2, BUY_W, BUY_H)


def sell_button(row):
    return R(row.right - 2 - SELL_W, row.y + (row.h - SELL_H) // 2, SELL_W, SELL_H)


def icon_rect(row):
    return R(row.x + 3, row.y + (row.h - 16) // 2, 16, 16)


# ---- Minecraft's default font: advance widths (glyph + 1 px spacing)
NARROW = {"i": 2, "!": 2, ".": 2, ",": 2, ":": 2, ";": 2, "'": 2, "|": 2, "l": 3, "I": 4, "t": 4, "f": 5, "k": 5, " ": 4, "(": 4, ")": 4, "[": 4, "]": 4, "*": 4, '"': 4, "<": 5, ">": 5, "{": 4, "}": 4,
          "@": 7, "~": 7}


def adv(ch, bold=False):
    return NARROW.get(ch, 6) + (1 if bold else 0)


def tw(s, bold=False):
    return sum(adv(c, bold) for c in s)


def fit(s, max_w):
    if max_w <= 0:
        return ""
    if tw(s) <= max_w:
        return s
    out = ""
    for c in s:
        if tw(out + c) > max_w - tw("..."):
            break
        out += c
    return out + "..."


def lift(rgb):
    """ShopColors.lift: a route's colour made readable on dark glass, hue kept (scale the strongest channel up to 0xE0, then mix white in until the perceived brightness is 150)."""
    r, g, b = (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255
    peak = max(r, g, b)
    if peak < 8:
        return 0xB0B0B0
    if peak < 0xE0:
        f = 0xE0 / peak
        r, g, b = min(255, jround(r * f)), min(255, jround(g * f)), min(255, jround(b * f))
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    if lum < 150:
        t = min(0.65, (150 - lum) / (255 - lum))
        r, g, b = jround(r + (255 - r) * t), jround(g + (255 - g) * t), jround(b + (255 - b) * t)
    return (r << 16) | (g << 8) | b


def wrap(text, max_w, max_lines):
    """ShopLayout.wrap: greedy on spaces, a word wider than a line broken by letters, the last line cut and ended with "..." when the text needs more lines than there are."""
    if not text.strip() or max_w <= 0 or max_lines <= 0:
        return []
    allv, cur = [], ""
    for word in text.split():
        joined = word if not cur else cur + " " + word
        if tw(joined) <= max_w:
            cur = joined
            continue
        if cur:
            allv.append(cur)
            cur = ""
        rest = word
        while len(rest) > 1 and tw(rest) > max_w:
            n = len(rest) - 1
            while n > 1 and tw(rest[:n]) > max_w:
                n -= 1
            allv.append(rest[:n])
            rest = rest[n:]
        cur = rest
    if cur:
        allv.append(cur)
    if len(allv) <= max_lines:
        return allv
    out = allv[:max_lines]
    last = out[-1] + "..."
    while len(last) > 3 and tw(last) > max_w:
        last = last[:-4] + "..."
    out[-1] = last
    return out


def speech_lines(r):
    """ShopLayout.speechLines."""
    return max(1, (r.h - 5) // 9)


def plan_bulk(strip, cats, label_for):
    """ShopScreen.planBulk: 'All stones' when it fits, else the bare name, else the bare name with tight padding (and whatever still does not fit is left out)."""
    for attempt in range(3):
        tight = attempt == 2
        labels = [label_for(c, attempt == 0) for c in cats]
        pad, gap = (BULK_PAD_TIGHT, BULK_GAP_TIGHT) if tight else (BULK_PAD, BULK_GAP)
        rects, x = [], strip.x
        for lab in labels:
            w = tw(lab) + pad
            if x + w > strip.right:
                break
            rects.append(R(x, strip.y, w, strip.h))
            x += w + gap
        if len(rects) == len(labels) or tight:
            return labels[:len(rects)], rects
    return [], []


class Mock(pu.Gui):
    """The theme mock plus proportional text, images, item stand-ins and the shop's own painters."""

    def __init__(self, sw, sh, scale):
        pu.W, pu.H = sw, sh
        super().__init__(scale)
        self.sw, self.sh = sw, sh
        self.f = ImageFont.truetype(SANS, int(round(8.3 * scale)))
        self.fb = ImageFont.truetype(SANS_B, int(round(8.3 * scale)))

    # ---- text with Minecraft's advances
    def mtext(self, s, x, y, argb, bold=False, shadow=False):
        sc = self.s
        w = tw(s, bold) + 2
        layer = Image.new("RGBA", (int(w * sc) + 4, 12 * sc), (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        f = self.fb if bold else self.f
        cx = 0.0
        for ch in s:
            if shadow:
                d.text((cx * sc + sc, sc * 0.5 + sc), ch, font=f, fill=(0, 0, 0, (argb >> 24) & 255))
            d.text((cx * sc, sc * 0.5), ch, font=f, fill=pu.rgba(argb))
            cx += adv(ch, bold)
        self._blend(x, y - 1, x + layer.width / sc, y - 1 + layer.height / sc, layer)

    def mcenter(self, s, cx, y, argb, bold=False):
        self.mtext(s, cx - tw(s, bold) / 2, y, argb, bold)

    def image(self, im, x, y, w=None, h=None):
        w, h = w or im.width, h or im.height
        sc = self.s
        layer = im.resize((int(w * sc), int(h * sc)), Image.NEAREST)
        self._blend(x, y, x + w, y + h, layer)

    # ---- the stand-ins for item sprites (16x16)
    def item(self, kind, x, y, color=0xFFB878E0):
        c = lambda v: v
        if kind == "book":
            base = color
            self.fill(x + 3, y + 1, x + 14, y + 15, mix(base, 0xFF000000, 0.45))
            self.fill(x + 4, y + 2, x + 13, y + 14, base)
            self.fill(x + 4, y + 2, x + 6, y + 14, mix(base, 0xFF000000, 0.35))
            self.fill(x + 7, y + 5, x + 12, y + 7, mix(base, 0xFFFFFFFF, 0.5))
            self.fill(x + 8, y + 8, x + 11, y + 11, 0xFFE8D060)
            self.fill(x + 4, y + 14, x + 13, y + 15, 0xFFE8DCC8)
        elif kind == "gem":
            self.fill(x + 5, y + 3, x + 11, y + 5, mix(color, 0xFFFFFFFF, 0.5))
            self.fill(x + 3, y + 5, x + 13, y + 9, color)
            self.fill(x + 5, y + 9, x + 11, y + 12, mix(color, 0xFF000000, 0.3))
            self.fill(x + 7, y + 12, x + 9, y + 13, mix(color, 0xFF000000, 0.5))
        elif kind == "meat":
            self.fill(x + 3, y + 4, x + 13, y + 12, 0xFFC86060)
            self.fill(x + 4, y + 5, x + 9, y + 8, 0xFFE89090)
            self.fill(x + 11, y + 10, x + 14, y + 13, 0xFFEADFCB)
        elif kind == "stone":
            self.fill(x + 4, y + 4, x + 12, y + 12, 0xFF8A90A0)
            self.fill(x + 5, y + 5, x + 9, y + 8, 0xFFC8CCD8)
            self.fill(x + 8, y + 9, x + 12, y + 12, 0xFF60667A)
        elif kind == "tooth":
            self.fill(x + 6, y + 2, x + 10, y + 11, 0xFFE8E4D8)
            self.fill(x + 7, y + 11, x + 9, y + 14, 0xFFC8C4B8)
            self.fill(x + 7, y + 3, x + 8, y + 9, 0xFFFFFFFF)
        elif kind == "hide":
            self.fill(x + 3, y + 3, x + 13, y + 13, 0xFF8A5A3A)
            self.fill(x + 4, y + 4, x + 12, y + 7, 0xFFA87850)

    # ---- the painters of ShopPainter.java
    def coin(self, x, y, lit):
        body, edge, shine = (0xFFE8B040, 0xFFA06820, 0xFFFFF0A0) if lit else (0xFF8A7A60, 0xFF5A5040, 0xFFB0A488)
        self.fill(x + 2, y, x + 5, y + 1, edge)
        self.fill(x + 1, y + 1, x + 6, y + 2, body)
        self.fill(x, y + 2, x + 7, y + 5, body)
        self.fill(x + 1, y + 5, x + 6, y + 6, body)
        self.fill(x + 2, y + 6, x + 5, y + 7, edge)
        self.fill(x, y + 2, x + 1, y + 5, edge)
        self.fill(x + 6, y + 2, x + 7, y + 5, edge)
        self.fill(x + 2, y + 2, x + 4, y + 3, shine)
        self.fill(x + 3, y + 3, x + 4, y + 5, edge)

    def sbutton(self, r, label, enabled, hovered, accent):
        a = accent if enabled else 0xFF5C5668
        self.fill(r.x, r.y, r.right, r.bottom, with_alpha(0x0A0710, 0.88))
        self.gradient(r.x + 1, r.y + 1, r.right - 1, r.bottom - 1, with_alpha(a, 0.38 if enabled and hovered else 0.17), with_alpha(a, 0.05))
        self.outline(r.x, r.y, r.w, r.h, with_alpha(a, (1.0 if hovered else 0.75) if enabled else 0.35))
        color = (0xFFFFFFFF if hovered else PAPER) if enabled else 0xFF7A7080
        self.mcenter(label, r.x + r.w / 2, r.y + (r.h - 8) // 2, color)

    def row(self, r, hovered, flagship, now):
        self.fill(r.x, r.y, r.right, r.bottom, with_alpha(0x1A1222, 0.92 if hovered else 0.70))
        self.fill(r.x, r.bottom - 1, r.right, r.bottom, with_alpha(0x6A4A2A, 0.55))
        if hovered:
            self.fill(r.x, r.y, r.x + 2, r.bottom, with_alpha(AMBER, 0.9))
            self.fill(r.x + 2, r.y, r.right, r.bottom - 1, with_alpha(AMBER, 0.07))
        if flagship:
            glow = 0.55 + 0.25 * math.sin(now * 0.08)
            self.outline(r.x, r.y, r.w, r.h - 1, with_alpha(AMBER, glow))

    def scrollbar(self, track, total, visible, scroll):
        self.fill(track.x, track.y, track.right, track.bottom, with_alpha(0x000000, 0.35))
        if total > visible and track.h >= 8:
            th = max(8, jround(track.h * (visible / total)))
            travel = track.h - th
            mx = max(0, total - visible)
            top = 0 if mx == 0 else jround(travel * (min(scroll, mx) / mx))
            self.fill(track.x, track.y + top, track.right, track.y + top + th, with_alpha(AMBER, 0.65))

    def bubble(self, r):
        self.fill(r.x, r.y, r.right, r.bottom, with_alpha(0x120A1A, 0.82))
        self.outline(r.x, r.y, r.w, r.h, with_alpha(PLUM, 0.55))
        for i, w in enumerate((1, 1, 1)):
            self.fill(r.x + 6 + i, r.y - 3 + i, r.x + 7 + i, r.y, with_alpha(PLUM, 0.55))

    def cobwebs(self, r, webs):
        s = 48
        self.image(webs.crop((0, 0, 48, 48)), r.x, r.y)
        self.image(webs.crop((48, 0, 96, 48)), r.right - s, r.y)
        self.image(webs.crop((0, 48, 48, 96)), r.x, r.bottom - s)
        self.image(webs.crop((48, 48, 96, 96)), r.right - s, r.bottom - s)

    def dust(self, r, now, count):
        for i in range(count):
            fx = (i * 0.6180339 + 0.13) % 1.0
            fy = (i * 0.7548776 + 0.41) % 1.0
            speed = 0.012 + 0.02 * ((i * 0.3247) % 1.0)
            x = r.x + 2 + int(fx * (r.w - 4) + math.sin(now * 0.02 + i * 1.7) * 5.0)
            y = r.y + 2 + int(((fy - now * speed * 0.01) % 1.0) * (r.h - 4))
            if x < r.x + 1 or x >= r.right - 1:
                continue
            tw_ = 0.5 + 0.5 * math.sin(now * 0.07 + i * 2.3)
            self.fill(x, y, x + 1, y + 1, with_alpha(0xF4E6C0, 0.10 + 0.28 * tw_))


# ---- the sample shop
def gfull(v):
    return f"{v:,}"


def gcompact(v):
    if v < 1000:
        return str(v)
    for unit, suf in ((10 ** 12, "T"), (10 ** 9, "B"), (10 ** 6, "M"), (10 ** 3, "K")):
        if v >= unit:
            whole, rest = divmod(v, unit)
            dec = 0 if whole >= 100 else 1 if whole >= 10 else 2
            s = str(whole)
            if dec:
                frac = rest * (10 ** dec) // unit
                f = str(frac).rjust(dec, "0").rstrip("0")
                if frac > 0 and f:
                    s += "." + f
            return s + suf
    return str(v)


OFFERS = [  # name, kind, color/icon, price, left, total, flagship, blurb key
    ("Fang of Smoldering Insanity", "book", 0xFFE8541C, 2_874_000, 1, 1, True, "flagship"),
    ("Tome of the Thunder Wyrm", "book", 0xFFE8D040, 2_391_000, 1, 1, False, "route"),
    ("Tome of the Sky Wyrm", "book", 0xFF70C8E8, 2_652_000, 1, 1, False, "route"),
    ("Ice-Make Grimoire", "book", 0xFF80B8F0, 2_130_000, 0, 1, False, "route"),
    ("Beginner Archery Primer", "book", 0xFF58B87A, 148_000, 4, 5, False, "sp"),
    ("Elemental Primer", "book", 0xFF9A7CFF, 312_000, 3, 3, False, "sp"),
    ("Magic Booster", "book", 0xFFF0A040, 704_000, 5, 5, False, "sp"),
    ("Jack-in-the-Box", "book", 0xFFE06A9A, 389_000, 3, 4, False, "sp"),
]
GOODS = [  # name, icon kind, color, count, unit, category
    ("Magic Stone", "stone", 0xFF8A90A0, 13, 50_000, "stones"),
    ("Medium Quality Magic Crystal", "gem", 0xFF60A8F0, 2, 100_000, "stones"),
    ("Raw Monster Meat", "meat", 0, 24, 4_000, "meat"),
    ("Seared Monster Meat", "meat", 0, 8, 8_000, "meat"),
    ("Spider Fang", "tooth", 0, 6, 15_000, "materials"),
    ("Monster Leather (C)", "hide", 0, 3, 60_000, "materials"),
    ("Diamond", "gem", 0xFF50E8E0, 5, 200_000, "gems"),
    ("Emerald", "gem", 0xFF40D070, 9, 60_000, "gems"),
    ("Nether Star", "gem", 0xFFF0F0C0, 1, 1_500_000, "treasure"),
]
BULK = ["stones", "meat", "materials"]
CAT = {"stones": "stones", "meat": "meat", "materials": "materials"}
BLURB = {"flagship": "The fang. Every fire-eater's dream, and every fire-eater's ruin.",
         "route": "A whole road in one book. Read it once and it is yours for good.",
         "sp": "Small purse, small book. Points are points, dearie.",
         "goods": "{} gold each. Take it or leave it.",
         "sell": "{} gold for one. Shift or right click and the whole stack goes."}
TITLE, SUB = "THE BLACK MARKET", "Behind the cobwebs, nothing is cheap."


def render(path, sw, sh, scale, gold=3_250_000, hover=None, voice="Show me the coin first, dearie. Then the shelves. Those are the rules.",
           offers=OFFERS, goods=GOODS, now=40.0, shelf_scroll=0, goods_scroll=0, limit=-1, clock="2d 4h", reply=None):
    g = Mock(sw, sh, scale)
    L = Layout(sw, sh)
    webs = Image.open(os.path.join(TEX, "cobwebs.png")).convert("RGBA")
    portrait = Image.open(os.path.join(TEX, "witch.png")).convert("RGBA")
    # background of the screen: the theme's scrim and the plum magic circle
    g.scrim(0.85)
    g.magic_circle(sw / 2, sh / 2, min(sw, sh) * 0.62, now, PLUM, 0.16, pu.CALM)
    p = L.panel
    g.panel(p.x, p.y, p.w, p.h, AMBER, 0.55, 1.0)
    g.gradient(p.x + 1, p.y + 1, p.right - 1, p.bottom - 1, with_alpha(0x2A1030, 0.30), with_alpha(0x0A0408, 0.40))
    g.cobwebs(p, webs)
    g.dust(p, now, 16)

    # header
    pr = L.portrait
    g.fill(pr.x - 1, pr.y - 1, pr.right + 1, pr.bottom + 1, with_alpha(AMBER, 0.55))
    g.fill(pr.x, pr.y, pr.right, pr.bottom, 0xFF0E0814)
    g.image(portrait, pr.x, pr.y)
    tx = pr.right + 8
    g.mtext(TITLE, tx, p.y + 8, AMBER, bold=True, shadow=True)
    pu_ = L.purse
    gtxt = gfull(gold)
    gw = tw(gtxt)
    ctext = f"Restock in {clock}"
    ltext = f"Buys left today: {limit}" if limit >= 0 else ""
    purse_w = max(gw + 11, tw(ctext), tw(ltext))
    if tw(SUB) <= pu_.right - purse_w - 8 - tx:                  # the sub-title only appears when the whole of it fits beside the purse
        g.mtext(SUB, tx, p.y + 21, DIM)
    g.coin(pu_.right - gw - 11, pu_.y + 1, True)
    g.mtext(gtxt, pu_.right - gw, pu_.y + 1, AMBER, shadow=True)
    g.mtext(ctext, pu_.right - tw(ctext), pu_.y + 13, DIM)
    if ltext:
        g.mtext(ltext, pu_.right - tw(ltext), pu_.y + 23, DIM if limit > 0 else BAD)
    g.fill(p.x + PAD, p.y + HEAD_H - 1, p.right - PAD, p.y + HEAD_H, with_alpha(AMBER, 0.25))

    # shelf
    sb = L.shelf_box
    g.well(sb.x, sb.y, sb.w, sb.h, AMBER, 0.9)
    g.heading("THE SHELVES", sb.x + 4, sb.y + 2, sb.w - 8, AMBER)
    hover_offer = hover_good = -1
    mx, my = hover if hover else (-1, -1)
    if not offers:
        for k, ln in enumerate(wrap("The shelves are bare. Come back after the dust settles.", sb.w - 12, 99)):
            g.mtext(ln, sb.x + 6, sb.y + HEADING_H + 6 + 10 * k, DIM)
    else:
        lst = L.shelf_list
        for i in range(len(offers)):
            row = L.shelf_row(i, shelf_scroll)
            if row.contains(mx, my) and lst.contains(mx, my):
                hover_offer = i
        g.clip = (int(lst.x * g.s), int(lst.y * g.s), int(lst.right * g.s), int(lst.bottom * g.s))
        for i in range(shelf_scroll, min(len(offers), shelf_scroll + L.shelf_visible + 1)):
            name, kind, col, price, left, total, flag, blurb = offers[i]
            row = L.shelf_row(i, shelf_scroll)
            over = i == hover_offer
            sold = left <= 0
            afford = (not sold) and gold >= price
            g.row(row, over, flag, now)
            ic = icon_rect(row)
            g.item(kind, ic.x, ic.y, col)
            buy = buy_button(row)
            text_x = ic.right + 5
            is_route = blurb in ("flagship", "route")
            ncol = (0xFF000000 | lift(col & 0xFFFFFF)) if is_route and col else (AMBER if flag else PAPER)     # ShopScreen.nameColor: route books wear their route's colour
            g.mtext(fit(name, buy.x - text_x - 4), text_x, row.y + 4, ncol)
            ly = row.y + 13
            g.coin(text_x, ly, afford)
            g.mtext(gfull(price), text_x + 10, ly, FAINT if sold else GOOD if afford else BAD)
            sx = text_x + 10 + tw(gfull(price)) + 6
            stock = "SOLD OUT" if sold else f"{left} left"
            if sx + tw(stock) <= buy.x - 2:
                g.mtext(stock, sx, ly, BAD if sold else DIM)
            g.sbutton(buy, "Buy", afford, buy.contains(mx, my), AMBER)
        g.clip = None
        g.scrollbar(L.shelf_bar, len(offers), L.shelf_visible, shelf_scroll)

    # goods
    gb = L.goods_box
    g.well(gb.x, gb.y, gb.w, gb.h, PLUM, 0.9)
    g.heading("YOUR GOODS", gb.x + 4, gb.y + 2, gb.w - 8, PLUM)
    present = []
    for gd in goods:
        if gd[5] in BULK and gd[5] not in present:
            present.append(gd[5])
    labels, rects = plan_bulk(L.bulk_row, present, lambda c, long_form: f"All {CAT[c]}" if long_form else CAT[c].capitalize())
    for label, rr in zip(labels, rects):
        g.sbutton(rr, label, True, rr.contains(mx, my), PLUM)
    gl = L.goods_list
    if not goods:
        for k, ln in enumerate(wrap("Nothing in your pockets interests me, dearie. Go and kill something.", gl.w - 4, 99)):
            g.mtext(ln, gl.x + 3, gl.y + 2 + 10 * k, DIM)
    else:
        for i in range(len(goods)):
            row = L.goods_row(i, goods_scroll)
            if row.contains(mx, my) and gl.contains(mx, my):
                hover_good = i
        g.clip = (int(gl.x * g.s), int(gl.y * g.s), int(gl.right * g.s), int(gl.bottom * g.s))
        for i in range(goods_scroll, min(len(goods), goods_scroll + L.goods_visible + 1)):
            name, kind, col, count, unit, cat = goods[i]
            row = L.goods_row(i, goods_scroll)
            g.row(row, i == hover_good, False, 0)
            ic = icon_rect(row)
            g.item(kind, ic.x, ic.y, col or 0xFF888888)                  # no count badge on the icon: the line under the name already says "x13"
            sell = sell_button(row)
            text_x = ic.right + 4
            g.mtext(fit(name, sell.x - text_x - 3), text_x, row.y + 3, PAPER)
            g.mtext(fit(f"x{count}  {gcompact(unit)} ea", sell.x - text_x - 3), text_x, row.y + 12, AMBER)
            g.sbutton(sell, "Sell", True, sell.contains(mx, my), PLUM)
        g.clip = None
        g.scrollbar(L.goods_bar, len(goods), L.goods_visible, goods_scroll)

    # footer
    sp = L.speech
    g.bubble(sp)
    text = voice
    if reply:
        text = reply
    elif hover_offer >= 0:
        text = BLURB[offers[hover_offer][7]]
    elif hover_good >= 0:
        g_ = goods[hover_good]
        over_sell = sell_button(L.goods_row(hover_good, goods_scroll)).contains(mx, my)
        text = BLURB["sell"].format(gfull(g_[4])) if over_sell else BLURB["goods"].format(gfull(g_[4]))
    said = wrap(text, sp.w - 10, speech_lines(sp))
    top = max(2, (sp.h - 9 * len(said)) // 2)                      # one line sits in the middle of the bubble, two fill it
    for k, ln in enumerate(said):
        g.mtext(ln, sp.x + 5, sp.y + top + 9 * k, SPEECH)
    g.sbutton(L.close, "Close", True, L.close.contains(mx, my), AMBER)

    # a tooltip stand-in at the mouse (vanilla puts it 12 right and 12 up of the cursor)
    if hover_offer >= 0 and mx < buy_button(L.shelf_row(hover_offer, shelf_scroll)).x:
        o = offers[hover_offer]
        lines = [o[0], "Skill Book" if o[7] != "sp" else "SP Book", "(the item's own tooltip)"]
        tip(g, lines, mx, my)
    elif hover_good >= 0 and mx < sell_button(L.goods_row(hover_good, goods_scroll)).x:
        g_ = goods[hover_good]
        tip(g, [g_[0], "(the item's own tooltip)"], mx, my)
    g.save(path)
    return L


def tip(g, lines, mx, my):
    wmax = max(tw(s) for s in lines) + 8
    tx_, ty_ = mx + 12, my - 12
    if tx_ + wmax + 4 > g.sw:                                    # vanilla moves a tooltip that would leave the screen to the left of the cursor
        tx_ = max(4, mx - 12 - wmax)
    g.fill(tx_, ty_, tx_ + wmax, ty_ + 10 * len(lines) + 6, with_alpha(0x100010, 0.94))
    g.outline(tx_, ty_, wmax, 10 * len(lines) + 6, with_alpha(0x5000FF, 0.7))
    for k, s in enumerate(lines):
        g.mtext(s, tx_ + 4, ty_ + 4 + 10 * k, 0xFFFFFFFF if k == 0 else 0xFFA0A0A0)


def main(argv):
    out = argv[0] if argv else os.path.join(ROOT, "build", "shop_preview")
    scale = 2
    if "--scale" in argv:
        scale = int(argv[argv.index("--scale") + 1])
    os.makedirs(out, exist_ok=True)
    L = Layout(427, 240)
    print("427x240: panel %dx%d at %d,%d; shelf rows %d, goods rows %d" % (L.panel.w, L.panel.h, L.panel.x, L.panel.y, L.shelf_visible, L.goods_visible))
    first = L.shelf_row(0, 0)
    good = L.goods_row(1, 0)
    sell = sell_button(L.goods_row(2, 0))
    render(os.path.join(out, "shop_main.png"), 427, 240, scale)
    render(os.path.join(out, "shop_hover_shelf.png"), 427, 240, scale, hover=(first.x + 40, first.y + 8))
    render(os.path.join(out, "shop_hover_goods.png"), 427, 240, scale, hover=(good.x + 40, good.y + 8))
    render(os.path.join(out, "shop_hover_sell.png"), 427, 240, scale, hover=(sell.x + 8, sell.y + 6))
    render(os.path.join(out, "shop_poor.png"), 427, 240, scale, gold=95_000, reply="Not enough gold, dearie. The fang does not accept promises, only coin.", limit=2)
    render(os.path.join(out, "shop_854.png"), 854, 480, scale)
    render(os.path.join(out, "shop_320.png"), 320, 180, scale, reply="Hush, hush. Keep your voice down, dearie. Nobody sold you anything and you were never here.")
    render(os.path.join(out, "shop_empty.png"), 427, 240, scale, offers=[], goods=[], gold=0)
    print("wrote", out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
