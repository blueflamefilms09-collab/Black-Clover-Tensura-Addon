#!/usr/bin/env python3
"""
Art of the Charter Grimoires of the arc bosses (arc/ArcCharacter.java, charter/CharterTable.addArc).

Why: the seven grimoires of the Zogratis family and the floor bosses have hand-picked sigils (tools/gen_charter_assets.py). The bosses of the arcs add
thirty more, each registered as the item charter_grimoire_<id>, and an item without a model and texture shows the purple-and-black missing cube, so each
needs the same kind of art. This tool makes it from the character's own record: it reads every `ArcCharacter.boss("<id>", ...)` in the Java sources,
keeps the ones that declare a `.charter(...)`, takes their `.colors(main, accent)` (the Builder's grey when absent) and draws the same 3D book as the
older grimoires with a sigil derived from the id (a deterministic mirrored 7x7 glyph, so a character always gets the same one). Idempotent; it never
touches the art of the seven (their ids are skipped).

    python3 tools/gen_arc_grimoire_assets.py                    # writes the assets of every arc boss with a grimoire
    python3 tools/gen_arc_grimoire_assets.py --list             # only lists what it found
    python3 tools/gen_arc_grimoire_assets.py --ids foo:203040:90B0D0 bar    # explicit characters (id[:main[:accent]], hex colours), for a test or a one-off
    python3 tools/gen_arc_grimoire_assets.py --out DIR          # write under DIR instead of src/main/resources/assets/nusmp
    python3 tools/gen_arc_grimoire_assets.py --preview sheet.png   # also a contact sheet of the books (a software render, the game is not needed)
"""
import argparse
import hashlib
import os
import re
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_charter_assets as gca  # noqa: E402  (the older generator: the book model, the textures and the software preview)

SRC = os.path.join(HERE, "..", "src", "main", "java")
DEFAULT_RES = gca.RES
DEFAULT_MAIN, DEFAULT_ACCENT = 0x8A8A9A, 0xC8C8D8      # the Builder's grey in ArcCharacter

BOSS_RE = re.compile(r'ArcCharacter\s*\.\s*boss\(\s*"([a-z][a-z0-9_]*)"')
COLORS_RE = re.compile(r'\.colors\(\s*(0x[0-9A-Fa-f]{6})\s*,\s*(0x[0-9A-Fa-f]{6})\s*\)')


def old_ids():
    return {b[0] for b in gca.parse_table()}


def scan(src_dir):
    """[(id, main, accent)] of every arc boss in the sources that declares a grimoire."""
    found = {}
    for root, _dirs, files in os.walk(src_dir):
        for name in sorted(files):
            if not name.endswith(".java"):
                continue
            text = open(os.path.join(root, name), encoding="utf-8").read()
            for m in BOSS_RE.finditer(text):
                end = text.find(".build()", m.end())
                chain = text[m.start(): end if end >= 0 else m.end() + 4000]
                if ".charter(" not in chain:
                    continue
                c = COLORS_RE.search(chain)
                main = int(c.group(1), 16) if c else DEFAULT_MAIN
                accent = int(c.group(2), 16) if c else DEFAULT_ACCENT
                found[m.group(1)] = (m.group(1), main, accent)
    return sorted(found.values())


def explicit(items):
    out = []
    for it in items:
        parts = it.split(":")
        out.append((parts[0], int(parts[1], 16) if len(parts) > 1 else DEFAULT_MAIN, int(parts[2], 16) if len(parts) > 2 else DEFAULT_ACCENT))
    return out


def sigil_for(ident):
    """A mirrored 7x7 glyph from the id: three free columns per row, a free centre column, mirrored; 16 to 30 cells so it is neither empty nor a block."""
    for salt in range(64):
        h = hashlib.md5(("%s/%d" % (ident, salt)).encode()).digest()
        bits = int.from_bytes(h[:4], "big")
        rows = []
        for y in range(7):
            left = [(bits >> (y * 3 + x)) & 1 for x in range(3)]
            centre = (h[4] >> y) & 1
            cells = left + [centre] + left[::-1]
            rows.append("".join("#" if c else "." for c in cells))
        filled = sum(r.count("#") for r in rows)
        if 16 <= filled <= 30 and rows[0].count("#") and rows[6].count("#"):
            return rows
    return ["...#...", "..###..", ".#####.", "#######", ".#####.", "..###..", "...#..."]


def write(chars, res, preview_path):
    os.makedirs(os.path.join(res, "textures", "item"), exist_ok=True)
    os.makedirs(os.path.join(res, "models", "item"), exist_ok=True)
    sheet = Image.new("RGBA", (240 * max(1, min(len(chars), 6)), (240 + 80) * ((len(chars) + 5) // 6 or 1)), (30, 30, 36, 255)) if preview_path else None
    for i, (ident, main, accent) in enumerate(chars):
        gca.SIGILS[ident] = sigil_for(ident)
        tex, icon = gca.build(ident, main, accent)
        tex.save(os.path.join(res, "textures", "item", "charter_grimoire_%s.png" % ident))
        icon.save(os.path.join(res, "textures", "item", "charter_grimoire_%s_icon.png" % ident))
        mdl = gca.model(ident)
        gca.write_json(os.path.join(res, "models", "item", "charter_grimoire_%s.json" % ident), mdl)
        gca.write_json(os.path.join(res, "models", "item", "charter_grimoire_%s_icon.json" % ident),
                       {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/charter_grimoire_%s_icon" % ident}})
        if sheet is not None:
            x, y = 240 * (i % 6), (240 + 80) * (i // 6)
            sheet.paste(gca.preview(mdl, tex), (x, y))
            sheet.paste(icon.resize((64, 64), Image.NEAREST), (x + 20, y + 244))
    if sheet is not None:
        sheet.save(preview_path)
        print("preview:", preview_path)
    print("wrote %d arc grimoires to %s" % (len(chars), res))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--src", default=SRC)
    ap.add_argument("--out", default=DEFAULT_RES)
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--ids", nargs="*")
    ap.add_argument("--preview")
    args = ap.parse_args()
    chars = explicit(args.ids) if args.ids else scan(args.src)
    clash = sorted({c[0] for c in chars} & old_ids())
    if clash:
        print("skipped (one of the seven older grimoires): " + ", ".join(clash))
        chars = [c for c in chars if c[0] not in clash]
    if args.list:
        for ident, main_c, accent in chars:
            print("%s  main %06X  accent %06X" % (ident, main_c, accent))
        print("%d arc bosses with a grimoire" % len(chars))
        return 0
    if not chars:
        print("no arc boss with a grimoire found under %s (nothing to write)" % args.src)
        return 0
    write(chars, args.out, args.preview)
    return 0


if __name__ == "__main__":
    sys.exit(main())
