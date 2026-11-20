#!/usr/bin/env python3
"""Draws the layout of the Devil Contract ritual and writes its plain-text how-to, FROM THE MOD'S OWN DATA (original art, PIL only, deterministic).

    python3 tools/gen_devil_ritual_layout.py

Reads (so the picture can not drift from the game): devil/DevilRitualRules.java (circle, offer, cooldown), devil/DevilNpcRules.java (how far in front the devil stands), devil/DevilConfig.java (the default
light threshold and participant radius, the dark dimensions), devil/DevilWorld.java (the eight rituals, their length and what each asks), assets/.../devil_dark_magic.json (the dark magics),
data/.../tags/worldgen/biome/devil_dark_places.json (the dark biomes) and data/.../recipe/devil_contract.json (the craft).

Writes docs/devil_ritual_layout.png (1600 px wide), docs/devil_ritual_layout_discord.png (1200 px wide, drawn again at that size so the text stays sharp) and docs/devil_ritual_howto.md (plain text).
"""
import json
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
J = ROOT / "src/main/java/com/newuniverse/nusmp/devil"
RES = ROOT / "src/main/resources"
DOCS = ROOT / "docs"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
BASE_W, BASE_H = 1600, 1560


# ------------------------------------------------------------------------------------------------ data
def num(src, name, cast=float):
    m = re.search(r"\b%s\s*=\s*([0-9.]+)" % re.escape(name), src)
    assert m, "constant %s not found" % name
    return cast(m.group(1))


def load():
    rules = (J / "DevilRitualRules.java").read_text(encoding="utf-8")
    npc = (J / "DevilNpcRules.java").read_text(encoding="utf-8")
    cfg = (J / "DevilConfig.java").read_text(encoding="utf-8")
    worlds = (J / "DevilWorld.java").read_text(encoding="utf-8")
    d = {
        "circle": num(rules, "CIRCLE"),
        "offer_ticks": num(rules, "OFFER_TICKS", int),
        "cooldown": num(rules, "COOLDOWN", int),
        "front": num(npc, "FRONT"),
        "max_light": int(re.search(r'defineInRange\("maxLight",\s*(\d+)', cfg).group(1)),
        "radius": int(re.search(r'defineInRange\("participantRadius",\s*(\d+)', cfg).group(1)),
        "dims": re.search(r'defineListAllowEmpty\("darkDimensions",\s*java\.util\.List\.of\(([^)]*)\)', cfg).group(1).replace('"', "").replace(" ", "").split(","),
    }
    d["worlds"] = [{"label": m.group(1), "ritual": m.group(2), "ticks": int(m.group(3)), "color": int(m.group(4), 16), "need": m.group(5)}
                   for m in re.finditer(r'^    [A-Z_]+\("([^"]*)", "([^"]*)", (\d+), 0x([0-9A-F]+),\s*\n?\s*"([^"]*)"', worlds, re.M)]
    assert len(d["worlds"]) == 8, d["worlds"]
    dm = json.loads((RES / "assets/anime_multiverse/devil_dark_magic.json").read_text(encoding="utf-8"))
    d["dark_magics"], d["dark_attrs"], d["dark_skills"] = dm["magics"], dm["devilAttributes"], dm["skills"]
    d["biomes"] = json.loads((RES / "data/anime_multiverse/tags/worldgen/biome/devil_dark_places.json").read_text(encoding="utf-8"))["values"]
    rec = json.loads((RES / "data/anime_multiverse/recipe/devil_contract.json").read_text(encoding="utf-8"))
    d["recipe"] = rec
    return d


def first_sentence(s):
    return re.split(r"(?<=[.])\s", s, maxsplit=1)[0]


def seconds(t):
    return t / 20.0


def nice(rid):
    return rid.split(":")[-1].replace("_", " ")


def recipe_lines(rec):
    names = {"D": "Diamond", "O": "Crying Obsidian", "N": "Nether Star", "S": "Wither Skeleton Skull", "B": "Royal Blood, Zane Blood or a Gneiss Crystal (the tag #anime_multiverse:devil_contract_blood)"}
    return ["%s = %s" % (k, names.get(k, nice(v.get("item", v.get("tag", "?"))))) for k, v in rec["key"].items()], rec["pattern"]


# ------------------------------------------------------------------------------------------------ the picture
def draw(d, width, path):
    k = width / BASE_W
    H = int(BASE_H * k)
    img = Image.new("RGB", (width, H), (0x12, 0x0E, 0x18))
    g = ImageDraw.Draw(img)

    def f(sz, bold=False):
        return ImageFont.truetype(FONT_B if bold else FONT, max(8, int(sz * k)))

    def S(v):
        return int(v * k)

    def text(x, y, s, sz=22, fill=(0xE8, 0xE2, 0xF0), bold=False, anchor="la"):
        g.text((S(x), S(y)), s, font=f(sz, bold), fill=fill, anchor=anchor)

    def wrap(x, y, s, sz, maxw, fill=(0xC8, 0xC0, 0xD4), gap=6):
        words, line, yy = s.split(), "", y
        font = f(sz)
        for w in words:
            t = (line + " " + w).strip()
            if g.textlength(t, font=font) > S(maxw) and line:
                text(x, yy, line, sz, fill)
                yy += sz + gap
                line = w
            else:
                line = t
        if line:
            text(x, yy, line, sz, fill)
            yy += sz + gap
        return yy

    text(40, 26, "Devil Contract ritual: how to set it up", 40, bold=True)
    text(40, 78, "Drawn from the mod's own data (tools/gen_devil_ritual_layout.py). Distances are to scale: one grid square is one block.", 20, (0xA8, 0x9C, 0xBC))

    # ---- left: the top-down diagram
    ppb = 46                                                  # px per block
    cx, cy = 520, 640
    R_part = d["radius"] * ppb
    g.rectangle([S(40), S(120), S(1000), S(1250)], fill=(0x1A, 0x14, 0x22), outline=(0x3A, 0x30, 0x4C), width=max(1, S(2)))
    for gx in range(-10, 11):
        x = cx + gx * ppb
        if 40 < x < 1000:
            g.line([S(x), S(121), S(x), S(1249)], fill=(0x24, 0x1C, 0x30), width=1)
    for gy in range(-11, 14):
        y = cy + gy * ppb
        if 121 < y < 1249:
            g.line([S(40 + 1), S(y), S(999), S(y)], fill=(0x24, 0x1C, 0x30), width=1)
    # the participant ring (dashed)
    for a in range(0, 360, 6):
        if (a // 6) % 2 == 0:
            g.arc([S(cx - R_part), S(cy - R_part), S(cx + R_part), S(cy + R_part)], a, a + 4, fill=(0x7A, 0x5A, 0xC8), width=max(1, S(3)))
    text(cx, cy - R_part - 28, "%d blocks: a dark magic here lets the ritual begin" % d["radius"], 19, (0xB8, 0x9C, 0xF0), anchor="ma")
    # the circle
    rc = d["circle"] * ppb
    g.ellipse([S(cx - rc), S(cy - rc), S(cx + rc), S(cy + rc)], outline=(0xC8, 0x1E, 0x2D), width=max(1, S(5)))
    g.ellipse([S(cx - rc + 14), S(cy - rc + 14), S(cx + rc - 14), S(cy + rc - 14)], outline=(0x70, 0x14, 0x20), width=max(1, S(2)))
    for i in range(6):                                          # the sigil: a hexagram, drawn in the circle (an original shape)
        a0 = math.radians(60 * i - 90)
        a1 = math.radians(60 * (i + 2) - 90)
        g.line([S(cx + (rc - 18) * math.cos(a0)), S(cy + (rc - 18) * math.sin(a0)), S(cx + (rc - 18) * math.cos(a1)), S(cy + (rc - 18) * math.sin(a1))], fill=(0x9A, 0x1A, 0x28), width=max(1, S(2)))
    text(cx, cy - rc - 62, "the circle: %.1f blocks. Step out and the ritual breaks." % d["circle"], 19, (0xFF, 0x9A, 0xA4), anchor="ma")
    # the caller
    g.ellipse([S(cx - 16), S(cy - 16), S(cx + 16), S(cy + 16)], fill=(0xE8, 0xE2, 0xF0))
    g.polygon([(S(cx), S(cy - 30)), (S(cx - 10), S(cy - 14)), (S(cx + 10), S(cy - 14))], fill=(0xE8, 0xE2, 0xF0))
    text(cx, cy + 24, "you", 20, bold=True, anchor="ma")
    # the devil, `front` blocks ahead (up)
    fy = cy - d["front"] * ppb
    g.ellipse([S(cx - 26), S(fy - 26), S(cx + 26), S(fy + 26)], fill=(0x30, 0x08, 0x10), outline=(0xFF, 0x4A, 0x52), width=max(1, S(3)))
    g.polygon([(S(cx - 20), S(fy - 14)), (S(cx - 30), S(fy - 40)), (S(cx - 6), S(fy - 22))], fill=(0xFF, 0x4A, 0x52))
    g.polygon([(S(cx + 20), S(fy - 14)), (S(cx + 30), S(fy - 40)), (S(cx + 6), S(fy - 22))], fill=(0xFF, 0x4A, 0x52))
    g.ellipse([S(cx - 11), S(fy - 4), S(cx - 3), S(fy + 4)], fill=(0xFF, 0xC2, 0x3A))
    g.ellipse([S(cx + 3), S(fy - 4), S(cx + 11), S(fy + 4)], fill=(0xFF, 0xC2, 0x3A))
    text(cx + 60, fy - 44, "the devil rises %.0f blocks in front of you," % d["front"], 19, (0xFF, 0x8A, 0x92))
    text(cx + 60, fy - 20, "facing you, and makes its offer", 19, (0xFF, 0x8A, 0x92))
    # two participants inside the ring
    for (px, py, lab) in ((cx - 5.6 * ppb, cy + 3.0 * ppb, "dark mage"), (cx + 4.8 * ppb, cy + 4.6 * ppb, "dark mage")):
        g.ellipse([S(px - 13), S(py - 13), S(px + 13), S(py + 13)], fill=(0x5A, 0x3A, 0xA0), outline=(0xB8, 0x9C, 0xF0), width=max(1, S(2)))
        text(px, py + 18, lab, 17, (0xB8, 0x9C, 0xF0), anchor="ma")
    # a scale bar
    g.line([S(60), S(1220), S(60 + 5 * ppb), S(1220)], fill=(0xE8, 0xE2, 0xF0), width=max(1, S(3)))
    text(60, 1190, "5 blocks", 18)

    # ---- right: the conditions
    x0 = 1030
    y = 120
    text(x0, y, "1. Where to stand: one ground is enough", 24, bold=True)
    y += 44
    rows = [("A", "a dark magic at the circle", "Anyone within %d blocks who has one of: %s ... and %d more, or a dark devil attribute (%s), or a listed skill (%d are listed)." %
             (d["radius"], ", ".join(nice(m).title() for m in d["dark_magics"][:6]), len(d["dark_magics"]) - 6, ", ".join(nice(a).title() for a in d["dark_attrs"]), len(d["dark_skills"]))),
            ("B", "a dark dimension", "In: %s." % ", ".join(d["dims"])),
            ("C", "a dark biome", "In: %s." % ", ".join(nice(b) for b in d["biomes"])),
            ("D", "a dark place", "Light level %d or lower at your feet (block and sky together), such as a night in the open or a cave." % d["max_light"])]
    for tag, head, body in rows:
        g.ellipse([S(x0), S(y), S(x0 + 34), S(y + 34)], fill=(0x5A, 0x14, 0x20), outline=(0xFF, 0x4A, 0x52), width=max(1, S(2)))
        text(x0 + 17, y + 17, tag, 20, bold=True, anchor="mm")
        text(x0 + 50, y + 2, head, 21, bold=True)
        y = wrap(x0 + 50, y + 30, body, 17, 480)
        y += 8
    # the light bar
    text(x0, y, "Light level (0 dark ... 15 bright):", 18, (0xA8, 0x9C, 0xBC))
    y += 30
    bw = 540 / 16.0
    for lv in range(16):
        col = (0x30, 0x6A, 0x3A) if lv <= d["max_light"] else (0x3A, 0x34, 0x44)
        g.rectangle([S(x0 + lv * bw), S(y), S(x0 + (lv + 1) * bw - 3), S(y + 26)], fill=col)
        text(x0 + lv * bw + bw / 2 - 1, y + 13, str(lv), 14, anchor="mm")
    y += 38
    y = wrap(x0, y, "Green: dark enough. The game checks once when you begin, and once a second while the ritual runs for the light level only.", 16, 530, (0xA8, 0x9C, 0xBC))
    y += 24
    text(x0, y, "2. What each world's ritual asks", 24, bold=True)
    y += 42
    for w in d["worlds"]:
        g.rectangle([S(x0), S(y + 4), S(x0 + 10), S(y + 52)], fill=(w["color"] >> 16 & 255, w["color"] >> 8 & 255, w["color"] & 255))
        text(x0 + 22, y, "%s: %s (%.0f s)" % (w["label"] if len(w["label"]) < 28 else "Tensura", w["ritual"], seconds(w["ticks"])), 16, bold=True)
        y = wrap(x0 + 22, y + 24, first_sentence(w["need"]), 15, 500, gap=3)
        y += 10
    # ---- bottom: the timeline and the craft
    yb = 1320
    g.line([S(40), S(yb - 10), S(width / k - 40), S(yb - 10)], fill=(0x3A, 0x30, 0x4C), width=max(1, S(2)))
    text(40, yb, "3. Then", 26, bold=True)
    steps = ["The ritual runs (80 to 200 ticks, 4 to 10 s, by world) while you stay in the circle.",
             "The devil makes its offer and waits %.0f seconds: sign (use the contract / click the term) or refuse." % seconds(d["offer_ticks"]),
             "A devil keeps one holder: if somebody holds it already, it is not called again (One of One).",
             "After any ritual, wait %.0f seconds before the next (a failed or refused one too)." % seconds(d["cooldown"])]
    yy = yb + 44
    for s in steps:
        yy = wrap(40, yy, "- " + s, 18, 940)
    keys, pat = recipe_lines(d["recipe"])
    text(1030, yb, "The Devil Contract can be crafted", 22, bold=True)
    for i, row in enumerate(pat):
        for j, ch in enumerate(row):
            bx, by = 1030 + j * 58, yb + 40 + i * 58
            g.rectangle([S(bx), S(by), S(bx + 52), S(by + 52)], fill=(0x24, 0x1C, 0x30), outline=(0x7A, 0x5A, 0xC8), width=max(1, S(2)))
            text(bx + 26, by + 26, ch, 24, bold=True, anchor="mm")
    ky = yb + 44
    for line in keys:
        ky = wrap(1030 + 190, ky, line, 14, 330, gap=2)
    img.save(path)
    return img


# ------------------------------------------------------------------------------------------------ the how-to
def howto(d, path):
    keys, pat = recipe_lines(d["recipe"])
    out = []
    a = out.append
    a("DEVIL CONTRACT RITUAL: HOW TO SET IT UP")
    a("(written from the mod's own data by tools/gen_devil_ritual_layout.py; see docs/devil_ritual_layout.png for the picture)")
    a("")
    a("1. GET THE CONTRACT")
    a("   Find an Infernal Contract (relic loot) or craft one (server setting devil.contractCraftable, on by default):")
    for row in pat:
        a("      " + " ".join(row))
    for line in keys:
        a("      " + line)
    a("")
    a("2. CHOOSE YOUR GROUND (one is enough)")
    a("   A. A dark magic at the circle: anyone within %d blocks with a grimoire magic from %s," % (d["radius"], ", ".join(m.title().replace("_", " ") for m in d["dark_magics"])))
    a("      or a dark devil attribute (%s), or one of the %d listed skills (assets/anime_multiverse/devil_dark_magic.json, add your own)." % (", ".join(x.title().replace("_", " ") for x in d["dark_attrs"]), len(d["dark_skills"])))
    a("   B. A dark dimension: %s." % ", ".join(d["dims"]))
    a("   C. A dark biome: %s." % ", ".join(nice(b) for b in d["biomes"]))
    a("   D. A dark place: light level %d or lower at your feet (block and sky together)." % d["max_light"])
    a("   The game checks once when you begin, and once a second while the ritual runs for ground D only.")
    a("")
    a("3. STAND IN THE CIRCLE")
    a("   The circle is %.1f blocks around the place you began. Leave it and the ritual breaks. The devil rises %.0f blocks in front of you, facing you." % (d["circle"], d["front"]))
    a("")
    a("4. WHAT EACH WORLD'S RITUAL ASKS")
    for w in d["worlds"]:
        a("   %s - %s (%.0f s): %s" % (w["label"], w["ritual"], seconds(w["ticks"]), w["need"]))
    a("")
    a("5. THEN")
    a("   The devil makes its offer and waits %.0f seconds. Sign or refuse." % seconds(d["offer_ticks"]))
    a("   A devil keeps one holder (One of One): if another player holds it, it is not called again until the bond lapses or a bound duel is won.")
    a("   After any ritual wait %.0f seconds before the next one (a failed or refused one too)." % seconds(d["cooldown"]))
    a("")
    Path(path).write_text("\n".join(out), encoding="utf-8")


def main():
    d = load()
    DOCS.mkdir(exist_ok=True)
    draw(d, 1600, DOCS / "devil_ritual_layout.png")
    draw(d, 1200, DOCS / "devil_ritual_layout_discord.png")
    howto(d, DOCS / "devil_ritual_howto.md")
    print("wrote docs/devil_ritual_layout.png (1600 px), docs/devil_ritual_layout_discord.png (1200 px), docs/devil_ritual_howto.md")


if __name__ == "__main__":
    main()
