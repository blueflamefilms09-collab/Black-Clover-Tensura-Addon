"""Contact sheets of every worn relic on a Steve-like figure, with its real palette and the models the catalog gives it.

    python3 tools/relics/worn_contact_sheet.py [--out docs/relic_visuals] [--slot HEAD,NECK,...] [--ids a,b,c] [--views front,three,side]

Reads the relic defs from the Java sources (id, grade, slot, look, palette), resolves the models with the same rule the game uses (a signature entry by id, else the models
of the slot and look) and renders them with tools/relics/preview_worn.py (the software rasteriser). One PNG per slot; the figure is plain boxes, so what you judge is the
silhouette, the parts, the colours and the glow. Run tools/relics/gen_worn_all.py first.
"""
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_worn_all as A  # noqa: E402
import gen_worn_lib as L  # noqa: E402
import preview_worn as P  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
DEFAULT_LOOK = {"HEAD": "CIRCLET", "CHEST": "PLATE", "NECK": "AMULET", "BACK": "CLOAK", "BELT": "BELT", "HANDS": "GAUNTLETS", "CHARM": "CHARM", "FEET": "BOOTS"}
ZOOM = {"HEAD": "head", "NECK": "body", "CHEST": "body", "BELT": "body", "CHARM": "body", "BACK": "wide", "HANDS": "arm", "FEET": "leg"}
GRADE = {"NORMAL": 1, "SPECIAL": 2, "RARE": 3, "UNIQUE": 4, "LEGEND": 5, "GOD": 6, "GENESIS": 7}


def relic_defs():
    """(id, name, grade, slot, look or None, (main, trim, glow) or None) of every RelicDef.of(...) with a body slot, from the Java sources."""
    rows = {}
    for f in glob.glob(os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "**", "*.java"), recursive=True):
        s = open(f, encoding="utf8").read()
        for m in re.finditer(r'RelicDef\.of\(\s*"([a-z0-9_]+)"\s*,\s*"([^"]*)"\s*,\s*Grade\.(\w+)\s*,\s*RelicSlot\.(\w+)\s*\)', s):
            end = s.find("RelicDef.of(", m.end())
            chunk = s[m.end(): end if end > 0 else m.end() + 3000]
            chunk = chunk[: chunk.find(");") + 2] if ");" in chunk else chunk[:2000]
            lk = re.search(r"\.look\(Look\.(\w+)\)", chunk)
            pal = re.search(r"\.palette\(\s*(0x[0-9A-Fa-f]+)\s*,\s*(0x[0-9A-Fa-f]+)\s*,\s*(0x[0-9A-Fa-f]+)", chunk)
            if m.group(4) in ZOOM:
                rows[m.group(1)] = (m.group(1), m.group(2), m.group(3), m.group(4), lk.group(1) if lk else None, tuple(int(x, 16) for x in pal.groups()) if pal else None)
    return list(rows.values())


def pieces_for(cat, rid, slot, look):
    for r, ps in cat.relics:
        if r == rid:
            return list(ps)
    for s, l, ps in cat.looks:
        if s == slot and l == (look or DEFAULT_LOOK.get(slot)):
            return list(ps)
    return []


def render_relic(cat, row, views, panel):
    rid, name, grade, slot, look, pal = row
    ps = pieces_for(cat, rid, slot, look)
    if not ps:
        return None
    pieces = P.parse_pieces(ps)
    im = P.sheet_for(pieces, pal or (0x8C8C9C, 0xD8C070, 0xFFE9A0), views, ZOOM[slot], panel=panel, label="%s  [%s %s %s]" % (name, grade, slot, look or "-"))
    return im


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "docs", "relic_visuals"))
    ap.add_argument("--slot", default="")
    ap.add_argument("--ids", default="")
    ap.add_argument("--views", default="front,three,side")
    ap.add_argument("--cols", type=int, default=1)
    a = ap.parse_args(argv)
    cat = A.catalog()
    views = a.views.split(",")
    panel = (230, 270)
    slots = [s for s in a.slot.split(",") if s] or ["HEAD", "BACK", "NECK", "CHEST", "HANDS", "FEET", "BELT", "CHARM"]
    ids = set(i for i in a.ids.split(",") if i)
    os.makedirs(a.out, exist_ok=True)
    for slot in slots:
        rows = [r for r in relic_defs() if r[3] == slot and (not ids or r[0] in ids)]
        rows.sort(key=lambda r: (-GRADE[r[2]], r[0]))
        tiles = [t for t in (render_relic(cat, r, views, panel) for r in rows) if t is not None]
        if not tiles:
            continue
        cols = a.cols
        w, h = tiles[0].width, tiles[0].height
        sheet = Image.new("RGB", (w * cols, h * ((len(tiles) + cols - 1) // cols)), (30, 32, 40))
        for i, t in enumerate(tiles):
            sheet.paste(t, ((i % cols) * w, (i // cols) * h))
        path = os.path.join(a.out, "worn_%s.png" % slot.lower())
        sheet.save(path)
        print("wrote", path, len(tiles), "relics")


if __name__ == "__main__":
    main(sys.argv[1:])
