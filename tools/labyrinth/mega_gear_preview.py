#!/usr/bin/env python3
"""Why: the owner cannot play the standard Normal / Special / Rare gear of the Mega-Dungeon (1.55.0, builder GEAR) from a terminal, so this draws what the
gear IS: three pictures with Pillow, no game needed.

  gear_grade_tables.png   what each grade is made of, the base item of every slot, how the magic resistance adds up
  gear_drop_chances.png   the chance per kill of each grade by floor (the roller's real numbers) and how the mix shifts with depth
  gear_tooltips.png       example pieces of each grade drawn like the game's item tooltips (name, enchantments, lore, attributes)

It also is an INDEPENDENT second implementation of the roller and of the factory's arithmetic (GearRoller.chances / make, GearFactory.addPower,
GearLook), written from the Java source, so the pictures can be checked against the Java: `--verify dump.json` replays the exact Java seeds (this file
carries java.util.Random's generator) and fails if a single chance, item, bonus, enchantment or resistance differs. The dump comes from
tools/labyrinth/mega_gear_dump/PreviewDump.java (its comment has the commands). The numbers come from
data/nusmp/mega/gear/tables.json and the config defaults (GearRates.DEFAULT), so a change to the data file changes the pictures.

Usage:  python3 tools/labyrinth/mega_gear_preview.py [--out DIR] [--verify dump.json]      (needs Pillow; run from the project root)
Vanilla numbers (attack damage, durability) are from github.com/misode/mcmeta 1.21.1-summary item_components; armour points are the vanilla armour
material table; enchantment tooltip order is the vanilla tag #minecraft:tooltip_order of 1.21.1.
"""
import argparse
import json
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TABLES = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "gear", "tables.json")

GRADES = ["normal", "special", "rare"]
SLOTS = ["sword", "axe", "bow", "crossbow", "shield", "helmet", "chestplate", "leggings", "boots"]
ARMOUR = ["helmet", "chestplate", "leggings", "boots"]
POWER_SLOTS = ["sword", "axe"] + ARMOUR
ENGRAVE_SLOTS = ["sword", "axe", "bow", "crossbow", "shield"]
DEFAULTS = {"normal": 0.05, "special": 0.015, "rare": 0.004, "floorShift": 0.02, "lootingBonus": 0.01}   # GearRates.DEFAULT
MAX_RESIST = 40                                                                                            # GearConfig default

# ------------------------------------------------------------------------------------------------------------------ vanilla facts
# item -> [attack damage modifier, attack speed modifier, max durability]   (1.21.1 item_components)
VANILLA = {
    "wooden_sword": [3.0, -2.4, 59], "wooden_axe": [6.0, -3.2, 59], "stone_sword": [4.0, -2.4, 131], "stone_axe": [8.0, -3.2, 131],
    "iron_sword": [5.0, -2.4, 250], "iron_axe": [8.0, -3.1, 250], "diamond_sword": [6.0, -2.4, 1561], "diamond_axe": [8.0, -3.0, 1561],
    "bow": [0, 0, 384], "crossbow": [0, 0, 465], "shield": [0, 0, 336],
    "leather_helmet": [0, 0, 55], "leather_chestplate": [0, 0, 80], "leather_leggings": [0, 0, 75], "leather_boots": [0, 0, 65],
    "chainmail_helmet": [0, 0, 165], "chainmail_chestplate": [0, 0, 240], "chainmail_leggings": [0, 0, 225], "chainmail_boots": [0, 0, 195],
    "iron_helmet": [0, 0, 165], "iron_chestplate": [0, 0, 240], "iron_leggings": [0, 0, 225], "iron_boots": [0, 0, 195],
    "diamond_helmet": [0, 0, 363], "diamond_chestplate": [0, 0, 528], "diamond_leggings": [0, 0, 495], "diamond_boots": [0, 0, 429],
}
# armour material -> (helmet, chestplate, leggings, boots) points, toughness
ARMOUR_POINTS = {"leather": ((1, 3, 2, 1), 0), "chainmail": ((2, 5, 4, 1), 0), "iron": ((2, 6, 5, 2), 0), "diamond": ((3, 8, 6, 3), 2)}
NAMES = {"leather_helmet": "Leather Cap", "leather_chestplate": "Leather Tunic", "leather_leggings": "Leather Pants"}
TOOLTIP_ORDER = ["sharpness", "power", "piercing", "sweeping_edge", "knockback", "punch", "protection", "feather_falling", "efficiency", "quick_charge",
                 "respiration", "aqua_affinity", "swift_sneak", "thorns", "unbreaking"]
ROMAN = {1: "I", 2: "II", 3: "III", 4: "IV", 5: "V"}
VANILLA_MAX_LEVEL = {"sharpness": 5, "unbreaking": 3, "knockback": 2, "sweeping_edge": 3, "efficiency": 5, "power": 5, "punch": 2, "quick_charge": 3, "piercing": 4,
                     "protection": 4, "respiration": 3, "aqua_affinity": 1, "thorns": 3, "swift_sneak": 3, "feather_falling": 4}
ENGRAVING_NAMES = {"magic_interference": "Magic Interference", "magicule_absorption": "Magicule Absorption", "barrier_piercing": "Barrier Piercing",
                   "magic_protection": "Magic Protection", "sturdy": "Sturdy", "swift": "Swift", "crushing": "Crushing"}

# ------------------------------------------------------------------------------------------------------------------ java.util.Random
class JavaRandom:
    """java.util.Random's generator, so a Java seed replays here exactly."""
    MASK = (1 << 48) - 1

    def __init__(self, seed):
        self.seed = (seed ^ 0x5DEECE66D) & self.MASK

    def _next(self, bits):
        self.seed = (self.seed * 0x5DEECE66D + 0xB) & self.MASK
        return self.seed >> (48 - bits)

    def next_double(self):
        return ((self._next(26) << 27) + self._next(27)) * (1.0 / (1 << 53))


class Dice:
    """The mod's Dice: next() in [0,1); nextInt(bound) in [0,bound) and no draw at all when bound <= 1."""
    def __init__(self, rnd):
        self.rnd = rnd

    def next(self):
        return self.rnd.next_double()

    def next_int(self, bound):
        return 0 if bound <= 1 else min(bound - 1, int(self.next() * bound))


def jround(x):
    return int(math.floor(x + 0.5))      # Java's Math.round


# ------------------------------------------------------------------------------------------------------------------ the tables (GearTablesParser, GearRoller)
class Tables:
    def __init__(self, raw):
        self.curve = {g: sorted([(float(a), float(b)) for a, b in raw["curve"][g]]) for g in GRADES}
        caps = raw["caps"]
        self.max_chance = {g: float(caps[g]) for g in GRADES}
        self.total_cap = float(caps["total"])
        self.slot_weight = {s: float(raw["slotWeights"][s]) for s in SLOTS}
        self.grade = {}
        for g in GRADES:
            t = raw["grades"][g]
            self.grade[g] = {
                "items": {s: list(t["items"].get(s, [])) for s in SLOTS},
                "power": tuple(t["power"]), "durability": tuple(t["durability"]),
                "ench_count": tuple(t["enchants"]["count"]), "level_cap": int(t["enchants"]["levelCap"]),
                "ench_pool": {s: [self._pool(p) for p in t["enchants"]["pool"].get(s, [])] for s in SLOTS},
                "eng_count": tuple(t["engravings"]["count"]),
                "eng_pool": {s: [self._pool(p) for p in t["engravings"]["pool"].get(s, [])] for s in SLOTS},
                "resist_armour": tuple(t["resist"]["armour"]), "resist_hand": tuple(t["resist"]["hand"]),
            }

    @staticmethod
    def _pool(p):
        lo = max(1, int(p["min"]))
        return (p["id"], lo, max(lo, int(p["max"])))

    def offers(self, g, s):
        return len(self.grade[g]["items"][s]) > 0

    def curve_at(self, g, x):
        pts = self.curve[g]
        if not pts:
            return 1.0
        if x <= pts[0][0]:
            return pts[0][1]
        for i in range(1, len(pts)):
            if x <= pts[i][0]:
                span = pts[i][0] - pts[i - 1][0]
                if span <= 0:
                    return pts[i][1]
                f = (x - pts[i - 1][0]) / span
                return pts[i - 1][1] + (pts[i][1] - pts[i - 1][1]) * f
        return pts[-1][1]


def threat(floor):
    return max(1, abs(floor))


def progress(floor, shift=DEFAULTS["floorShift"]):
    return min(1.0, max(0.0, shift) * (threat(floor) - 1))


def chances(t, floor, looting=0, rates=DEFAULTS):
    """GearRoller.chances: base * curve at the depth progress, held to the grade's cap; Looting adds to the total, shared in proportion; total cap."""
    x = progress(floor, rates["floorShift"])
    w = {}
    total = 0.0
    for g in GRADES:
        offered = any(t.offers(g, s) for s in SLOTS)
        base = max(0.0, rates[g]) if offered else 0.0
        w[g] = max(0.0, min(base * t.curve_at(g, x), t.max_chance[g]))
        total += w[g]
    if total <= 0.0:
        return w
    boosted = min(total + max(0, looting) * max(0.0, rates["lootingBonus"]), t.total_cap)
    scale = boosted / total
    return {g: min(w[g] * scale, t.max_chance[g]) for g in GRADES}


def _pick_range(d, rng):
    lo, hi = rng
    if lo > hi:
        lo, hi = hi, lo
    return lo + d.next_int(hi - lo + 1)


def _pick_slot(d, t, g):
    offered = [s for s in SLOTS if t.offers(g, s)]
    if not offered:
        return None
    total = sum(max(0.0, t.slot_weight[s]) for s in offered)
    if total <= 0.0:
        return offered[d.next_int(len(offered))]
    u = d.next() * total
    edge = 0.0
    last = None
    for s in offered:
        w = max(0.0, t.slot_weight[s])
        if w <= 0.0:
            continue
        last = s
        edge += w
        if u < edge:
            return s
    return last


def _draw(d, count, pool, cap):
    if not pool or max(count) <= 0:
        return []
    n = min(_pick_range(d, count), len(pool))
    if n <= 0:
        return []
    left = list(pool)
    out = []
    for _ in range(n):
        pid, lo, hi = left.pop(d.next_int(len(left)))
        top = max(lo, min(hi, cap))
        level = lo + d.next_int(top - lo + 1)
        out.append((pid, max(1, min(level, cap))))
    return out


def make(t, d, g, forced=None):
    """GearRoller.make: one piece of the grade; the same random stream gives the same piece as the Java."""
    gt = t.grade[g]
    slot = forced if (forced is not None and t.offers(g, forced)) else (None if forced is not None else _pick_slot(d, t, g))
    if slot is None:
        return None
    items = gt["items"][slot]
    item = items[d.next_int(len(items))]
    power = _pick_range(d, gt["power"]) if slot in POWER_SLOTS else 0
    dur = _pick_range(d, gt["durability"])
    ench = _draw(d, gt["ench_count"], gt["ench_pool"][slot], gt["level_cap"])
    eng = _draw(d, gt["eng_count"], gt["eng_pool"][slot], 1 << 30) if slot in ENGRAVE_SLOTS else []
    resist = _pick_range(d, gt["resist_armour"] if slot in ARMOUR else gt["resist_hand"])
    return {"grade": g, "slot": slot, "item": item, "power": power, "dur": dur, "resist": resist, "ench": ench, "eng": eng}


# ------------------------------------------------------------------------------------------------------------------ the finished stack (GearFactory, GearLook)
def fmt(v):
    """The game's attribute number format (#.##)."""
    s = ("%.2f" % v).rstrip("0").rstrip(".")
    return s if s else "0"


def title(path):
    return NAMES.get(path, path.replace("_", " ").title())


def build_stack(spec):
    """What the factory makes of a spec, as plain data: name, enchantment lines, lore lines, attribute lines, durability."""
    path = spec["item"].split(":", 1)[1]
    slot, grade = spec["slot"], spec["grade"]
    base_name = title(path)
    name = {"normal": base_name, "special": "Refined " + base_name, "rare": "Warded " + base_name}[grade]
    max_dmg = VANILLA[path][2]
    new_max = max_dmg
    if spec["dur"] > 0 and max_dmg > 0:
        new_max = max(max_dmg + 1, jround(max_dmg * (100 + spec["dur"]) / 100.0))
    # the percentage bonus: a share of the item's own value, added as a flat modifier
    base_lines, extra_lines, header = [], [], None
    if slot in ("sword", "axe"):
        dmg, speed, _ = VANILLA[path]
        own = 1.0 + dmg
        header = "When in Main Hand:"
        base_lines = [(" %s Attack Damage" % fmt(own), "dark_green"), (" %s Attack Speed" % fmt(4.0 + speed), "dark_green")]
        if spec["power"] > 0:
            extra_lines = [("+%s Attack Damage" % fmt(jround(own * spec["power"]) / 100.0), "blue")]
    elif slot in ARMOUR:
        material = path.split("_")[0]
        points, tough = ARMOUR_POINTS[material]
        own = points[ARMOUR.index(slot)]
        header = {"helmet": "When on Head:", "chestplate": "When on Body:", "leggings": "When on Legs:", "boots": "When on Feet:"}[slot]
        base_lines = [("+%s Armor" % fmt(own), "blue")]
        if tough:
            base_lines.append(("+%s Armor Toughness" % fmt(tough), "blue"))
        if spec["power"] > 0:
            extra_lines = [("+%s Armor" % fmt(jround(own * spec["power"]) / 100.0), "blue")]
    # enchantments: the vanilla ones in the tag's order first, then the Tensura engravings (the game lists them after)
    van = [(i, l) for i, l in spec["ench"]]
    van.sort(key=lambda e: TOOLTIP_ORDER.index(e[0].split(":", 1)[1]) if e[0].split(":", 1)[1] in TOOLTIP_ORDER else 99)
    ench_lines = []
    for i, lvl in van:
        key = i.split(":", 1)[1]
        mx = VANILLA_MAX_LEVEL.get(key, 5)
        lvl = min(lvl, mx)
        ench_lines.append((title(key) + ("" if (lvl == 1 and mx == 1) else " " + ROMAN.get(lvl, str(lvl))), "gray"))
    for i, lvl in spec["eng"]:
        key = i.split(":", 1)[1]
        ench_lines.append((ENGRAVING_NAMES.get(key, title(key)) + " " + ROMAN.get(lvl, str(lvl)), "gray"))
    colour = {"normal": "gray", "special": "green", "rare": "aqua"}[grade]
    lore = [("Standard Gear - %s Grade" % grade.title(), colour),
            ({"normal": "Everyday make: no magic.", "special": "Refined craftsmanship.", "rare": "High-grade make: it turns magic aside."}[grade], "gray")]
    if spec["power"] > 0 and slot in POWER_SLOTS:
        lore.append(("+%d%% %s" % (spec["power"], "armour" if slot in ARMOUR else "attack damage"), "blue"))
    if spec["dur"] > 0:
        lore.append(("+%d%% durability" % spec["dur"], "blue"))
    if spec["resist"] > 0:
        lore.append(("Magic Resistance: %d%%" % spec["resist"], "dark_aqua"))
    return {"name": name, "colour": colour, "ench": ench_lines, "lore": lore, "header": header, "attr": base_lines + extra_lines,
            "durability": (max_dmg, new_max)}


# ------------------------------------------------------------------------------------------------------------------ drawing
COLOURS = {"gray": (170, 170, 170), "green": (85, 255, 85), "aqua": (85, 255, 255), "blue": (85, 85, 255), "dark_aqua": (0, 170, 170),
           "dark_green": (0, 170, 0), "white": (255, 255, 255), "yellow": (255, 255, 85), "dark_gray": (85, 85, 85)}
BG, PANEL, GRID = (26, 26, 25), (36, 36, 34), (58, 58, 55)
INK, INK2, MUTED = (236, 236, 232), (178, 178, 172), (128, 128, 122)
GRADE_RGB = {"normal": COLOURS["gray"], "special": COLOURS["green"], "rare": COLOURS["aqua"]}

FONT_DIRS = ["/mnt/skills/examples/canvas-design/canvas-fonts", "/usr/share/fonts/truetype/liberation", "/usr/share/fonts/truetype/dejavu"]


def font(names, size):
    for d in FONT_DIRS:
        for n in names:
            p = os.path.join(d, n)
            if os.path.exists(p):
                return ImageFont.truetype(p, size)
    return ImageFont.load_default(size)


def sans(size, bold=False):
    return font(["InstrumentSans-Bold.ttf", "LiberationSans-Bold.ttf"] if bold else ["InstrumentSans-Regular.ttf", "LiberationSans-Regular.ttf"], size)


def pixel(size):
    """The tooltip font. A pixel font (Pixelify Sans) was tried first and rejected: its 2, 3, 5 and 8 look alike, and a picture of numbers must be readable."""
    return font(["JetBrainsMono-Regular.ttf", "LiberationMono-Regular.ttf"], size)


def shade(rgb, k=0.25):
    return tuple(int(c * k) for c in rgb)


def tooltip_image(stack, scale=3):
    """The game's tooltip: dark box with cut corners and a violet frame, a 2-pixel gap under the name, text with a quarter-brightness shadow."""
    f = pixel(7 * scale + 1)
    line_h, pad = 10 * scale, 4 * scale
    lines = [(stack["name"], stack["colour"], True)]
    for t, c in stack["ench"]:
        lines.append((t, c, False))
    for t, c in stack["lore"]:
        lines.append((t, c, False))
    if stack["attr"]:
        lines.append(("", "gray", False))
        lines.append((stack["header"], "gray", False))
        for t, c in stack["attr"]:
            lines.append((t, c, False))
    probe = ImageDraw.Draw(Image.new("RGB", (4, 4)))
    width = max(probe.textlength(t, font=f) for t, _, _ in lines) + 2 * pad
    height = len(lines) * line_h + 2 * pad + 2 * scale
    img = Image.new("RGBA", (int(width), int(height)), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w, h, s = int(width), int(height), scale
    bg = (16, 0, 16, 240)
    d.rectangle([s, 0, w - s - 1, h - 1], fill=bg)
    d.rectangle([0, s, w - 1, h - s - 1], fill=bg)
    for y in range(s, h - s):                                    # the frame: violet at the top fading to dark at the bottom
        k = (y - s) / max(1, h - 2 * s)
        col = (int(80 + (40 - 80) * k), 0, int(255 + (127 - 255) * k), 255)
        d.rectangle([s, y, 2 * s - 1, y], fill=col)
        d.rectangle([w - 2 * s, y, w - s - 1, y], fill=col)
    d.rectangle([2 * s, s, w - 2 * s - 1, 2 * s - 1], fill=(80, 0, 255, 255))
    d.rectangle([2 * s, h - 2 * s, w - 2 * s - 1, h - s - 1], fill=(40, 0, 127, 255))
    y = pad
    for i, (t, c, is_title) in enumerate(lines):
        if t:
            rgb = COLOURS[c]
            d.text((pad + s, y + 1 * s), t, font=f, fill=shade(rgb) + (255,))
            d.text((pad, y), t, font=f, fill=rgb + (255,))
        y += line_h + (2 * s if is_title else 0)
    return img


def text_wrap(draw, text, fnt, width):
    out, line = [], ""
    for word in text.split():
        trial = (line + " " + word).strip()
        if draw.textlength(trial, font=fnt) <= width or not line:
            line = trial
        else:
            out.append(line)
            line = word
    if line:
        out.append(line)
    return out


def rounded_top_bar(d, x0, y0, x1, y1, fill, r=4):
    if y1 - y0 <= 0:
        return
    if y1 - y0 > r * 2:
        d.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=fill, corners=(True, True, False, False))
    else:
        d.rectangle([x0, y0, x1, y1], fill=fill)


def hatch(img, box, kind, ink):
    """Secondary encoding for the chart segments (the grade colours are the game's, not a colour-blind-safe set): stripes for Special, dots for Rare.
    Drawn through a mask the size of the segment, so nothing bleeds into the neighbouring segment or the gap."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0 + 1, y1 - y0 + 1
    if w <= 0 or h <= 0 or kind not in ("special", "rare"):
        return
    layer = Image.new("L", (w, h), 0)
    ld = ImageDraw.Draw(layer)
    if kind == "special":
        for k in range(-h, w, 8):
            ld.line([(k, h), (k + h, 0)], fill=255, width=2)
    else:
        for yy in range(3, h, 8):
            for xx in range(3 + ((yy // 8) % 2) * 4, w, 8):
                ld.ellipse([xx - 1, yy - 1, xx + 1, yy + 1], fill=255)
    img.paste(ink, (x0, y0, x0 + w, y0 + h), layer)


def table(d, x, y, widths, header, rows, fnt, fnt_b, pad=10, line=None, colour_col0=True):
    """A plain grid: header row, then rows of cells (a str, or a list of (text, colour) lines). Returns the y under the table."""
    line = line or int(fnt.size * 1.35)
    total_w = sum(widths)
    head_lines = [text_wrap(d, text, fnt_b, wcol - 2 * pad) for wcol, text in zip(widths, header)]
    hh = max(len(h) for h in head_lines) * line + 2 * pad
    d.rectangle([x, y, x + total_w, y + hh], fill=PANEL)
    cx = x
    for wcol, lines in zip(widths, head_lines):
        for l_i, ln in enumerate(lines):
            d.text((cx + pad, y + pad + l_i * line), ln, font=fnt_b, fill=INK)
        cx += wcol
    y += hh
    for r_i, row in enumerate(rows):
        cells = []
        for c_i, cell in enumerate(row):
            if isinstance(cell, str):
                lines = [(ln, None) for ln in text_wrap(d, cell, fnt, widths[c_i] - 2 * pad)]
            else:
                lines = []
                for t, c in cell:
                    lines += [(ln, c) for ln in text_wrap(d, t, fnt, widths[c_i] - 2 * pad)]
            cells.append(lines)
        rh = max(len(c) for c in cells) * line + 2 * pad
        d.line([(x, y), (x + total_w, y)], fill=GRID, width=1)
        cx = x
        for c_i, lines in enumerate(cells):
            for l_i, (t, c) in enumerate(lines):
                col = INK2
                if c is not None:
                    col = c
                elif c_i == 0 and colour_col0:
                    col = INK
                d.text((cx + pad, y + pad + l_i * line), t, font=(fnt_b if (c_i == 0 and colour_col0) else fnt), fill=col)
            cx += widths[c_i]
        y += rh
    d.line([(x, y), (x + total_w, y)], fill=GRID, width=1)
    return y


# ------------------------------------------------------------------------------------------------------------------ picture 1: the grade tables
def pretty_items(t, g, s):
    ids = t.grade[g]["items"][s]
    mats = []
    for i in ids:
        p = i.split(":", 1)[1]
        m = p.split("_")[0] if p not in ("bow", "crossbow", "shield") else p
        if m not in mats:
            mats.append(m)
    return " / ".join(mats)


def rng(r, suffix=""):
    lo, hi = r
    return "none" if hi == 0 else ("%d%s" % (lo, suffix) if lo == hi else "%d to %d%s" % (lo, hi, suffix))


def picture_tables(t, out):
    W = 2200
    img = Image.new("RGB", (W, 2200), BG)
    d = ImageDraw.Draw(img)
    f_t, f_h, f_b, f_s = sans(40, True), sans(26, True), sans(22), sans(19)
    fb = sans(22, True)
    x0 = 60
    d.text((x0, 40), "Standard gear of the Mega-Dungeon: Normal, Special, Rare", font=f_t, fill=INK)
    d.text((x0, 94), "Ordinary vanilla items stamped with the data component nusmp:gear_grade. Not relics: no serial, no Order's ledger, no world limit.", font=f_b, fill=INK2)
    d.text((x0, 128), "A kill by a player in the Mega-Dungeon rolls once over the three grades' chances (at most one piece). A sector Guardian drops one guaranteed Rare piece. "
           "Stones and meat of the economy are untouched.", font=f_b, fill=INK2)

    y = 190
    d.text((x0, y), "What each grade is made of (from tables.json)", font=f_h, fill=INK)
    y += 44
    scale_text = {
        "normal": "Everyday gear from standard metals; no magic, no resistances.",
        "special": "Superior make; better physical performance, sometimes a minor magical effect.",
        "rare": "Premium alloys or monster parts; natural magic resistance.",
    }
    rows = []
    for g in GRADES:
        gt = t.grade[g]
        eng = sorted({p[0].split(":", 1)[1] for s in ENGRAVE_SLOTS for p in gt["eng_pool"][s]})
        rows.append([
            [(g.upper() + " GRADE", GRADE_RGB[g])],
            scale_text[g],
            "sword/axe: " + pretty_items(t, g, "sword") + "; armour: " + pretty_items(t, g, "helmet") + "; bow, crossbow, shield: plain",
            "none" if gt["power"][1] == 0 else "+%s %% attack damage or armour" % rng(gt["power"]).replace(" to ", "-"),
            "none" if gt["durability"][1] == 0 else "+%s %% durability" % rng(gt["durability"]).replace(" to ", "-"),
            "none" if gt["ench_count"][1] == 0 else "%s, up to level %d" % (rng(gt["ench_count"]).replace(" to ", "-"), gt["level_cap"]),
            "none" if gt["eng_count"][1] == 0 else "%s of: %s" % (rng(gt["eng_count"]).replace(" to ", "-"), ", ".join(ENGRAVING_NAMES.get(e, e) for e in eng)),
            "none" if gt["resist_armour"][1] == 0 else "armour %s %%, weapon/shield %s %%" % (rng(gt["resist_armour"]).replace(" to ", "-"), rng(gt["resist_hand"]).replace(" to ", "-")),
        ])
    widths = [190, 300, 310, 230, 190, 190, 440, 270]
    y = table(d, x0, y, widths, ["Grade", "Tensura scale", "Material", "Bonus", "Durability", "Enchantments", "Tensura engravings (weapons, shield)", "Magic resistance"],
              rows, f_b, fb, pad=12) + 50
    d.text((x0, y), "The base item of every slot (vanilla items; a bow, crossbow or shield has no material, so its grade differs by bonus, enchantments and engravings)", font=f_h, fill=INK)
    y += 44
    rows = []
    for g in GRADES:
        rows.append([[(g.upper(), GRADE_RGB[g])]] + [pretty_items(t, g, s) for s in SLOTS])
    widths = [150] + [220] * 9
    y = table(d, x0, y, widths, ["Grade"] + [s.title() for s in SLOTS], rows, f_b, fb, pad=12) + 20
    d.text((x0, y), "Slot weights when a drop is decided: " + ", ".join("%s %d" % (s, t.slot_weight[s]) for s in SLOTS) + "  (armour is half of all drops, weapons about four in ten).", font=f_s, fill=MUTED)
    y += 60

    d.text((x0, y), "Tensura engravings by slot (owner rule 6: weapons and the shield carry them; Normal is plain, armour has the resistance component instead)", font=f_h, fill=INK)
    y += 44
    rows = []
    for s in ENGRAVE_SLOTS:
        cells = [s.title()]
        for g in ("special", "rare"):
            gt = t.grade[g]
            pool = ["%s%s" % (ENGRAVING_NAMES.get(p[0].split(":", 1)[1], p[0]), "" if p[1] == p[2] else " (up to %s)" % ROMAN[p[2]]) for p in gt["eng_pool"][s]]
            cells.append([("%s of: %s" % (rng(gt["eng_count"]).replace(" to ", "-"), ", ".join(pool)), GRADE_RGB[g])])
        rows.append(cells)
    widths = [200, 900, 1030]
    y = table(d, x0, y, widths, ["Slot", "Special", "Rare"], rows, f_b, fb, pad=12) + 20
    d.text((x0, y), "Each engraving is an enchantment of the #tensura:engraving tag, resolved when the piece is made; one the installed Tensura lacks is skipped. Curses (enervation, lethargy, ruination, sealing, stagnation) are never offered.", font=f_s, fill=MUTED)
    y += 60

    d.text((x0, y), "Magic resistance: how Rare pieces add up (armour worn + weapon and shield held, held to the ceiling of %d %%)" % MAX_RESIST, font=f_h, fill=INK)
    y += 44
    ra, rh = t.grade["rare"]["resist_armour"], t.grade["rare"]["resist_hand"]
    avg_a, avg_h = sum(ra) / 2.0, sum(rh) / 2.0
    scen = [("Nothing worn", 0, 0),
            ("One Rare chestplate", avg_a, 0),
            ("Full Rare armour set, no weapon", 4 * avg_a, 0),
            ("Full Rare set + Rare sword + Rare shield", 4 * avg_a + 2 * avg_h, 0),
            ("The best possible roll of everything", 4 * ra[1] + 2 * rh[1], 0)]
    rows = []
    for name, raw, _ in scen:
        capped = min(raw, MAX_RESIST)
        rows.append([name, "%.1f" % raw if raw % 1 else "%d" % raw, ("%.1f" % capped if capped % 1 else "%d" % capped) + (" (ceiling)" if raw > MAX_RESIST else ""),
                     "%.1f" % (100 - capped) if (100 - capped) % 1 else "%d" % (100 - capped)])
    widths = [560, 330, 330, 520]
    y = table(d, x0, y, widths, ["What the player wears and holds", "Sum of the pieces (average rolls)", "After the ceiling", "A spell hit of 100 becomes"], rows, f_b, fb, pad=12) + 20
    d.text((x0, y), "Only magic damage is cut: vanilla magic and indirect magic, Tensura's elemental and spell types. A fall, a blade, an arrow, poison or fire are not (docs/labyrinth/mega_gear.md section 4).", font=f_s, fill=MUTED)
    y += 50
    img = img.crop((0, 0, W, y + 20))
    img.save(os.path.join(out, "gear_grade_tables.png"))
    return img.size


# ------------------------------------------------------------------------------------------------------------------ picture 2: the drop chances
def picture_chances(t, out):
    floors = [1, 5, 10, 20, 26, 30, 40, 51]
    labels = ["1", "5", "10", "20", "26", "30", "40", "51+"]
    data = [chances(t, f) for f in floors]
    data3 = [chances(t, f, 3) for f in floors]
    W, H = 2200, 1660
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    f_t, f_h, f_b, f_s, fb = sans(40, True), sans(26, True), sans(22), sans(19), sans(22, True)
    x0 = 60
    d.text((x0, 40), "How often a kill drops standard gear, by floor", font=f_t, fill=INK)
    d.text((x0, 94), "The roller's own numbers with the default config (normal 5 %, special 1.5 %, rare 0.4 % at floor 1; floorShift 0.02) and the shipped curves. "
           "Floors count as distance from the Hub: -10 is as deep as +10.", font=f_b, fill=INK2)
    d.text((x0, 128), "Going deeper MOVES weight from Normal to Special and Rare; the total stays near 7 % (one piece in about 14 kills).", font=f_b, fill=INK2)

    surface = BG
    panels = [("Chance per kill (percent)", False, 0, 8.0), ("Share of the pieces that drop (percent)", True, 0, 100.0)]
    px = [x0, 1130]
    pw, ph, top = 980, 560, 250
    for (title_s, share, lo, hi), ox in zip(panels, px):
        d.text((ox, top - 52), title_s, font=f_h, fill=INK)
        left, right, bottom = ox + 70, ox + pw, top + ph
        for k in range(5):                                   # recessive grid
            v = lo + (hi - lo) * k / 4.0
            yy = bottom - int(ph * k / 4.0)
            d.line([(left, yy), (right, yy)], fill=GRID, width=1)
            d.text((left - 12, yy - 11), ("%g" % round(v, 1)), font=f_s, fill=MUTED, anchor="ra")
        slot_w = (right - left) / len(floors)
        for i, c in enumerate(data):
            tot = sum(c.values())
            vals = {g: (c[g] / tot * 100.0 if share else c[g] * 100.0) for g in GRADES}
            bx0 = int(left + slot_w * i + slot_w * 0.22)
            bx1 = int(left + slot_w * (i + 1) - slot_w * 0.22)
            ytop = bottom
            for g in GRADES:
                seg = int(ph * vals[g] / (hi - lo))
                y1, y0 = ytop - 2, ytop - seg                  # a 2 px surface gap between the segments
                if g == "normal":
                    y1 = ytop
                if y1 - y0 >= 1:
                    rounded_top_bar(d, bx0, y0, bx1, y1, GRADE_RGB[g], 4 if g == "rare" else 0)
                    hatch(img, (bx0, y0, bx1, y1), g, shade(GRADE_RGB[g], 0.55))
                    if seg >= 26:                                   # the value on a small dark plate, so the stripes never touch the digits
                        txt = "%.1f" % vals[g] if share else "%.2f" % vals[g]
                        cx, cy = (bx0 + bx1) // 2, (y0 + y1) // 2
                        tw = d.textlength(txt, font=f_s)
                        d.rounded_rectangle([cx - tw / 2 - 6, cy - 12, cx + tw / 2 + 6, cy + 12], radius=5, fill=(20, 20, 19))
                        d.text((cx, cy), txt, font=f_s, fill=INK, anchor="mm")
                ytop -= seg
            d.text(((bx0 + bx1) // 2, bottom + 14), labels[i], font=f_b, fill=INK2, anchor="ma")
        d.text(((left + right) // 2, bottom + 52), "floor (distance from the Hub, Holy and Abyssal alike)", font=f_s, fill=MUTED, anchor="ma")
    # legend, with the secondary encodings
    lx, ly = x0 + 70, top + ph + 100
    for g in GRADES:
        d.rectangle([lx, ly, lx + 34, ly + 22], fill=GRADE_RGB[g])
        hatch(img, (lx, ly, lx + 34, ly + 22), g, shade(GRADE_RGB[g], 0.55))
        d.text((lx + 48, ly - 1), g.title() + {"normal": " (solid)", "special": " (stripes)", "rare": " (dots)"}[g], font=f_b, fill=INK2)
        lx += 280
    d.text((lx + 40, ly - 1), "Segment labels in the bars are percent; the table below has every number.", font=f_s, fill=MUTED)

    y = top + ph + 160
    d.text((x0, y), "Every number", font=f_h, fill=INK)
    y += 42
    rows = []
    for g in GRADES:
        rows.append([[(g.title(), GRADE_RGB[g])]] + ["%.2f %%" % (c[g] * 100) for c in data])
    rows.append(["Total per kill"] + ["%.2f %%" % (sum(c.values()) * 100) for c in data])
    rows.append(["One piece per (kills)"] + ["%.1f" % (1 / sum(c.values())) for c in data])
    for g in GRADES:
        rows.append([[(g.title() + " share", GRADE_RGB[g])]] + ["%.0f %%" % (c[g] / sum(c.values()) * 100) for c in data])
    rows.append(["Total with Looting III"] + ["%.2f %%" % (sum(c.values()) * 100) for c in data3])
    widths = [330] + [200] * len(floors)
    y = table(d, x0, y, widths, ["Floor (+- the same)"] + labels, rows, f_b, fb, pad=9) + 24
    d.text((x0, y), "Looting adds 1 % to the TOTAL per level and shares it in proportion: more pieces, same mix. Caps: 30 % normal, 20 % special, 15 % rare, 50 % in all (they only matter for a mistyped config).",
           font=f_s, fill=MUTED)
    y += 40
    d.text((x0, y), "First-pass estimates: nobody has played them. Tune with  /multiverse mega gear status  and  gear roll 100000.  The colours are the game's grade colours; the stripes and dots are the second encoding.",
           font=f_s, fill=MUTED)
    img = img.crop((0, 0, W, y + 50))
    img.save(os.path.join(out, "gear_drop_chances.png"))
    return img.size


# ------------------------------------------------------------------------------------------------------------------ picture 3: tooltips
def pick_example(t, grade, slot, tries=24):
    """The richest of several seeded rolls (most lines), so a picture shows what a grade CAN carry, not the poorest roll."""
    best, best_n = None, -1
    for seed in range(1, tries + 1):
        spec = make(t, Dice(JavaRandom(7000 * (GRADES.index(grade) + 1) + 31 * seed + SLOTS.index(slot))), grade, slot)
        n = len(spec["ench"]) + len(spec["eng"]) + (1 if spec["power"] else 0) + (1 if spec["resist"] else 0)
        if n > best_n:
            best, best_n = spec, n
    return best


def picture_tooltips(t, out):
    rows = ["sword", "bow", "shield", "chestplate"]
    scale = 3
    cells = {}
    for g in GRADES:
        for s in rows:
            cells[(g, s)] = tooltip_image(build_stack(pick_example(t, g, s)), scale)
    colw = max(c.width for c in cells.values()) + 60
    rowh = {s: max(cells[(g, s)].height for g in GRADES) + 50 for s in rows}
    head = 170
    W, H = 60 + colw * 3 + 20, head + sum(rowh.values()) + 130
    img = Image.new("RGB", (W, H), (44, 44, 46))
    d = ImageDraw.Draw(img)
    f_t, f_h, f_b, f_s = sans(38, True), sans(28, True), sans(22), sans(19)
    d.text((60, 36), "Standard gear as the game shows it", font=f_t, fill=INK)
    d.text((60, 88), "Example pieces, drawn like item tooltips (richest of 24 seeded rolls per cell; every number is from the roller and the factory's own arithmetic).", font=f_b, fill=INK2)
    for gi, g in enumerate(GRADES):
        d.text((60 + colw * gi + 10, 130), g.upper() + " GRADE", font=f_h, fill=GRADE_RGB[g])
    y = head
    for s in rows:
        for gi, g in enumerate(GRADES):
            c = cells[(g, s)]
            img.paste(c, (60 + colw * gi + 10, y), c)
        d.line([(60, y + rowh[s] - 26), (W - 60, y + rowh[s] - 26)], fill=(70, 70, 72), width=1)
        y += rowh[s]
    notes = ["Enchantments come first, then the lore block the factory writes, then the item's attribute lines; the blue line under the base stat is the grade's bonus (a flat modifier with the fixed id nusmp:mega_gear/<slot>).",
             "Tensura engravings are shown with the level they were rolled at; the game caps each at the engraving's own maximum. Normal pieces carry nothing: they are exactly the vanilla item with a grade on it."]
    for n in notes:
        for ln in text_wrap(d, n, f_s, W - 120):
            d.text((60, y), ln, font=f_s, fill=INK2)
            y += 28
        y += 6
    img = img.crop((0, 0, W, y + 20))
    img.save(os.path.join(out, "gear_tooltips.png"))
    return img.size


# ------------------------------------------------------------------------------------------------------------------ verify against the Java
def verify(t, path):
    """Replays the Java dump (chances and seeded example specs) with this implementation; returns the list of differences."""
    dump = json.load(open(path))
    bad = []
    for c in dump["chances"]:
        mine0, mine3 = chances(t, c["floor"]), chances(t, c["floor"], 3)
        for k, g in (("n", "normal"), ("s", "special"), ("r", "rare")):
            if abs(mine0[g] - c[k]) > 1e-6:
                bad.append("floor %d %s: java %.6f python %.6f" % (c["floor"], g, c[k], mine0[g]))
            if abs(mine3[g] - c[k + "3"]) > 1e-6:
                bad.append("floor %d %s with Looting III: java %.6f python %.6f" % (c["floor"], g, c[k + "3"], mine3[g]))
        if abs(progress(c["floor"]) - c["x"]) > 1e-4:
            bad.append("floor %d progress: java %.4f python %.4f" % (c["floor"], c["x"], progress(c["floor"])))
    for e in dump["examples"]:
        gi, si = GRADES.index(e["grade"]), SLOTS.index(e["slot"])
        spec = make(t, Dice(JavaRandom(1000 * gi + 37 * e["seed"] + si)), e["grade"], e["slot"])
        mine = {"item": spec["item"], "power": spec["power"], "dur": spec["dur"], "resist": spec["resist"],
                "ench": [list(x) for x in spec["ench"]], "eng": [list(x) for x in spec["eng"]]}
        theirs = {k: e[k] for k in mine}
        if mine != theirs:
            bad.append("%s %s seed %d: java %s python %s" % (e["grade"], e["slot"], e["seed"], theirs, mine))
    return bad, len(dump["chances"]), len(dump["examples"])


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "gear_preview"), help="directory for the PNGs (created)")
    ap.add_argument("--verify", help="JSON dump written by the Java roller (see the class comment); replay it and report differences")
    args = ap.parse_args()
    t = Tables(json.load(open(TABLES, encoding="utf-8")))
    if args.verify:
        bad, nc, ne = verify(t, args.verify)
        print("verified %d chance rows and %d seeded pieces against the Java: %d difference(s)" % (nc, ne, len(bad)))
        for b in bad[:20]:
            print("  " + b)
        if bad:
            sys.exit(1)
    os.makedirs(args.out, exist_ok=True)
    for fn in (picture_tables, picture_chances, picture_tooltips):
        size = fn(t, args.out)
        print("%s: %dx%d" % (fn.__name__, size[0], size[1]))
    print("written to", args.out)


if __name__ == "__main__":
    main()
