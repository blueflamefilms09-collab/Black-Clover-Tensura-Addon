"""Contact sheets of the worn armour models on a Steve-like figure: every robe set as a whole (hood, robe, leggings, boots together) with its palette, the five transformation
overlays, and the armour relics that got a signature model.

    python3 tools/relics/armor_contact_sheet.py [--out docs/armor_build] [--only sets|modes|relics|curios|anime]

Uses the software rasteriser of tools/relics/preview_worn.py and the catalog of gen_worn_all.py (run that first). The figure is plain boxes: judge the silhouette, the parts, the
colours and the glow, not the exact fit against the skin."""
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_worn_all as A  # noqa: E402
import gen_worn_armor as AR  # noqa: E402
import gen_worn_modes as MO  # noqa: E402
import preview_worn as P  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))


def stack(tiles, cols):
    w, h = tiles[0].width, tiles[0].height
    sh = Image.new("RGB", (w * cols, h * ((len(tiles) + cols - 1) // cols)), (30, 32, 40))
    for i, t in enumerate(tiles):
        sh.paste(t, ((i % cols) * w, (i // cols) * h))
    return sh


def sets_sheet():
    cat = A.catalog()
    items = dict(cat.items)
    pals = dict(cat.ipal)
    tiles = []
    for (sid, *_rest) in AR.SETS:
        pieces = []
        for suffix in ("_hood", "", "_leggings", "_boots"):
            pieces += list(items[sid + suffix])
        tiles.append(P.sheet_for(P.parse_pieces(pieces), pals[sid], ["front", "three", "back"], "wide", panel=(250, 400), label=sid))
    return stack(tiles, 2)


def anime_pieces(items, sid):
    """The pieces of the four `anime:` entries of one set (the `none` pieces draw nothing)."""
    out = []
    for suffix in ("_hood", "", "_leggings", "_boots"):
        out += [p for p in items["anime:" + sid + suffix] if p != "none"]
    return out


def anime_sheets():
    """The 1.48.0 anime cloaks, front / three-quarter / side / back of every set, in three sheets (docs/armor_build/anime_sets_a..c.png)."""
    cat = A.catalog()
    items = dict(cat.items)
    pals = dict(cat.ipal)
    tiles = []
    for (sid, *_rest) in AR.ANIME_SETS:
        tiles.append(P.sheet_for(P.parse_pieces(anime_pieces(items, sid)), pals["anime:" + sid], ["front", "three", "side", "back"], "wide", panel=(240, 400), label=sid))
    return [stack(tiles[0:4], 1), stack(tiles[4:8], 1), stack(tiles[8:11], 1)]


def modes_sheet():
    tiles = [P.sheet_for(P.parse_pieces(list(ps)), pal, ["front", "three", "back"], "wide", panel=(250, 330), label=key) for key, ps, pal, _t, _th in MO.MODES]
    return stack(tiles, 2)


def curios_sheet():
    tiles = []
    for key, slot, pieces, pal, _t in AR.CURIOS:
        zoom = {"BACK": "wide", "HANDS": "arm", "BELT": "body", "CHARM": "body", "NECK": "body"}[slot]
        tiles.append(P.sheet_for(P.parse_pieces(list(pieces)), pal, ["front", "three"], zoom, panel=(250, 300), label=key))
    return stack(tiles, 2)


def relics_sheet():
    consts = {}
    files = glob.glob(os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "**", "*.java"), recursive=True)
    for f in files:
        for m in re.finditer(r'\b(ID_\w+)\s*=\s*"([a-z0-9_]+)"', open(f, encoding="utf8").read()):
            consts[m.group(1)] = m.group(2)
    pat = re.compile(r'RelicDef\.of\(\s*(?:"([a-z0-9_]+)"|[\w.]*?(ID_\w+))\s*,\s*"([^"]*)"\s*,\s*Grade\.(\w+)\s*,\s*RelicSlot\.(HEAD|CHEST|FEET)\s*\)')
    defs = {}
    for f in files:
        s = open(f, encoding="utf8").read()
        for m in pat.finditer(s):
            i = m.group(1) or consts[m.group(2)]
            tail = s[m.end(): m.end() + 1500]
            tail = tail[: tail.find(";")]
            pal = re.search(r"\.palette\(\s*(0x[0-9A-Fa-f]+)\s*,\s*(0x[0-9A-Fa-f]+)\s*,\s*(0x[0-9A-Fa-f]+)", tail)
            defs[i] = (m.group(5), tuple(int(x, 16) for x in pal.groups()) if pal else (0x8C8C9C, 0xD8C070, 0xFFE9A0), m.group(3))
    cat = A.catalog()
    tiles = []
    for rid, ps in cat.relics:
        if rid not in defs or not any(p.split("@")[0].rstrip("~") in AR.MODELS or p.split("@")[0].rstrip("~") in __import__("gen_worn_armor_relics").MODELS for p in ps):
            continue
        slot, pal, name = defs[rid]
        zoom = {"CHEST": "all", "FEET": "leg", "HEAD": "head"}[slot]
        tiles.append(P.sheet_for(P.parse_pieces(list(ps)), pal, ["front", "three", "back"] if slot == "CHEST" else ["front", "three", "side"], zoom, panel=(250, 330), label=name))
    return stack(tiles, 2)


def main(argv):
    out = os.path.join(ROOT, "docs", "armor_build")
    only = ""
    if "--out" in argv:
        out = argv[argv.index("--out") + 1]
    if "--only" in argv:
        only = argv[argv.index("--only") + 1]
    os.makedirs(out, exist_ok=True)
    if not only or only == "anime":
        for letter, img in zip("abc", anime_sheets()):
            path = os.path.join(out, "anime_sets_%s.png" % letter)
            img.save(path)
            print("wrote", path)
    for name, fn in (("sets", sets_sheet), ("modes", modes_sheet), ("relics", relics_sheet), ("curios", curios_sheet)):
        if only and only != name:
            continue
        path = os.path.join(out, "armor_%s.png" % name)
        fn().save(path)
        print("wrote", path)


if __name__ == "__main__":
    main(sys.argv[1:])
