#!/usr/bin/env python3
"""Reads the god table in skill/rules/FaithGodRules.java and writes
  tools/lang/faith_gods.json                      (names, descriptions, mode names, messages)
  src/main/resources/assets/anime_multiverse/textures/skill/<id>.png   (32x32 icon for each of the 16 skills)
Run from the repo root:  python3 tools/faith_gods/gen_lang_icons.py
The English text is also the fallback in the Java code (translatableWithFallback), so the game reads right without the lang file.
"""
import json, math, re, sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SRC = (ROOT / "src/main/java/com/newuniverse/nusmp/skill/rules/FaithGodRules.java").read_text(encoding="utf-8")
ICONS = ROOT / "src/main/resources/assets/anime_multiverse/textures/skill"
UNIQUE_ACTIVES, UNIQUE_PASSIVES = 3, 1
CLASS = {"LORD": (4, 2), "KING": (4, 3), "GOD": (5, 3), "DRAGON": (3, 2)}

gods = []
for seg in SRC.split("new God(")[1:]:
    head = re.match(r'\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', seg)
    cls = re.search(r"UltimateClass\.(\w+),\s*0x([0-9A-Fa-f]+),\s*SIG_(\w+)", seg)
    moves = re.findall(r'\bm\("([^"]+)",\s*"([^"]+)",\s*Verb\.(\w+)', seg)
    pas = re.findall(r'\bp\("([^"]+)",\s*"([^"]+)",\s*Pas\.(\w+)', seg)
    if not head or not cls:
        continue
    gods.append(dict(id=head[1], ult=head[2], name=head[3], ultname=head[4], franchise=head[5], domain=head[6],
                     cls=cls[1], rgb=int(cls[2], 16), sigil=cls[3], moves=moves, pas=pas))
if len(gods) != 8:
    sys.exit("expected 8 gods, parsed %d" % len(gods))


def describe(g, ult):
    uid = g["ult"] if ult else "faith_of_" + g["id"]
    na, npas = (CLASS[g["cls"]] if ult else (UNIQUE_ACTIVES, UNIQUE_PASSIVES))
    b = ("Ultimate (%s class). Evolved from %s. " % (g["cls"].lower(), g["name"])) if ult else ("Unique, evolves into %s once mastered. " % g["ultname"])
    b += "God of %s. Worshippers and offerings feed it. Passives (one ON/OFF switch): " % g["domain"].lower()
    b += ", ".join(p[1] for p in g["pas"][:npas]) + ", actives: " + ", ".join(m[1] for m in g["moves"][:na]) + "."
    return uid, b


lang = {}
for g in gods:
    for ult in (False, True):
        uid, text = describe(g, ult)
        lang["anime_multiverse.skill." + uid] = g["ultname"] if ult else g["name"]
        lang["anime_multiverse.skill." + uid + ".description"] = text
        for m in g["moves"]:
            lang["anime_multiverse.skill.mode.%s.%s" % (uid, m[0])] = m[1]
msgs = {
    "off": "The Faith gods are switched off on this server.",
    "cooldown": "%s is recharging (%ss).",
    "no_faith": "%s needs %s Faith (you have %s). Worshippers' offerings and Pray fill it.",
    "no_foes": "No foe in reach.", "no_target": "Look at a foe.", "no_room": "No room to move.",
    "unknown": "There is no such god. /faith gods lists them.",
    "need_faith": "Only a holder of Faith or Goddess of Faith can become a god.",
    "already": "You already walk the path of %s.",
    "too_many": "You may follow only %s god line(s) at a time.",
    "wait": "The ritual cannot be repeated yet.",
    "shrine": "Stand on a block of gold: it is the shrine.",
    "offering": "The offering is %s x%s (carried).",
    "proofs": "Three proofs are needed (you have %s): an Ultimate skill, %s worshippers, %s Faith offered by them, Faith mastered.",
    "faith_low": "The ritual costs %s Faith.",
    "ascended": "You have become the god %s.", "failed": "The ritual failed.",
    "broadcast": "%s has become %s.",
    "list_title": "The Faith gods (/faith ascend <id>): stand on a gold block, carry the offering, prove yourself.",
}
for k, v in msgs.items():
    lang["anime_multiverse.skill.msg.gods." + k] = v
(ROOT / "tools/lang/faith_gods.json").write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# ---------------------------------------------------------------- icons: a radial glow in the god's colour and a different glyph each
def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


GLYPH = {
    "femto": "wings", "lucifero": "horns", "luminus": "moon", "mavis": "star4",
    "ill_ilah": "eye", "super_saiyan_god": "flame", "ra": "sun", "aqua": "drop",
}


def glyph(d, kind, cx, cy, col):
    if kind == "sun":
        for a in range(0, 360, 30):
            x, y = cx + 11 * math.cos(math.radians(a)), cy + 11 * math.sin(math.radians(a))
            d.line([(cx, cy), (x, y)], fill=col, width=2)
        d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], fill=col)
    elif kind == "moon":
        d.ellipse([cx - 9, cy - 9, cx + 9, cy + 9], fill=col)
        d.ellipse([cx - 4, cy - 11, cx + 12, cy + 7], fill=(10, 10, 30, 255))
    elif kind == "star4":
        d.polygon([(cx, cy - 12), (cx + 3, cy - 3), (cx + 12, cy), (cx + 3, cy + 3), (cx, cy + 12), (cx - 3, cy + 3), (cx - 12, cy), (cx - 3, cy - 3)], fill=col)
    elif kind == "eye":
        d.polygon([(cx - 12, cy), (cx, cy - 7), (cx + 12, cy), (cx, cy + 7)], fill=col)
        d.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=(10, 10, 30, 255))
    elif kind == "flame":
        d.polygon([(cx, cy - 13), (cx + 8, cy - 1), (cx + 6, cy + 10), (cx, cy + 13), (cx - 6, cy + 10), (cx - 8, cy - 1), (cx - 3, cy - 4)], fill=col)
    elif kind == "drop":
        d.polygon([(cx, cy - 13), (cx + 8, cy + 3), (cx, cy + 12), (cx - 8, cy + 3)], fill=col)
    elif kind == "horns":
        d.polygon([(cx - 12, cy - 12), (cx - 4, cy - 2), (cx - 8, cy + 10), (cx - 1, cy + 2), (cx + 1, cy + 2), (cx + 8, cy + 10), (cx + 4, cy - 2), (cx + 12, cy - 12), (cx, cy - 6)], fill=col)
    else:  # wings
        d.polygon([(cx, cy + 10), (cx - 13, cy - 10), (cx - 5, cy - 2), (cx, cy - 12), (cx + 5, cy - 2), (cx + 13, cy - 10)], fill=col)


def icon(g, ult):
    base = ((g["rgb"] >> 16) & 255, (g["rgb"] >> 8) & 255, g["rgb"] & 255)
    im = Image.new("RGBA", (32, 32), (8, 8, 24, 255))
    px = im.load()
    for y in range(32):
        for x in range(32):
            t = min(1.0, math.hypot(x - 15.5, y - 15.5) / 22.0)
            f = (0.55 if ult else 0.4) * (1.0 - t) + 0.1
            px[x, y] = shade(base, f) + (255,)
    d = ImageDraw.Draw(im)
    glyph(d, GLYPH[g["id"]], 16, 16, shade(base, 1.0 if ult else 0.85) + (255,))
    border = (255, 215, 96, 255) if ult else shade(base, 0.7) + (255,)
    d.rectangle([0, 0, 31, 31], outline=border)
    if ult:
        d.rectangle([2, 2, 29, 29], outline=(255, 245, 200, 255))
        for cx, cy in ((3, 3), (28, 3), (3, 28), (28, 28)):
            d.point((cx, cy), fill=(255, 255, 255, 255))
    return im


ICONS.mkdir(parents=True, exist_ok=True)
for g in gods:
    icon(g, False).save(ICONS / ("faith_of_%s.png" % g["id"]))
    icon(g, True).save(ICONS / ("%s.png" % g["ult"]))
print("wrote", len(lang), "lang keys and", 2 * len(gods), "icons")
