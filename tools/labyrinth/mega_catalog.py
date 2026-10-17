#!/usr/bin/env python3
"""
Exports the REAL template and pool files of the Mega-Dungeon as one JSON catalog (`src/test/resources/mega/catalog.json`) for the pure Java
assembler tests and the simulator: every template's size, jigsaw sockets (position, facing, name, target, pool, priorities, final state) and data
markers, and every pool's weighted elements and fallback.

    python3 tools/labyrinth/mega_catalog.py            # (re)writes the catalog
    python3 tools/labyrinth/mega_catalog.py --check    # exit 1 when the file on disk is stale (CI-safe: no game, no network)

Why a catalog and not "read the NBT in the test": the assembler is pure Java (no Minecraft types) so JUnit can run it thousands of times per
second with no game; it still has to be tested against the TEMPLATES THAT SHIP, not against hand-made shapes, or a test would pass while the
real pools produced a dead sector. The catalog is generated from the .nbt and .json files themselves (never hand-edited), and `--check` keeps it
in step with them.
"""
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from template import load  # noqa: E402

DATA = os.path.join(L.RES, "data", "nusmp")
OUT = os.path.join(L.ROOT, "src", "test", "resources", "mega", "catalog.json")
FOLDER_KIND = {"landing": "LANDING", "safe": "SAFE", "corridor": "CORRIDOR", "junction": "JUNCTION", "hall": "HALL", "set": "SET_PIECE", "cavern": "CAVERN",
               "trial": "TRIAL", "trap": "TRAP", "puzzle": "PUZZLE", "treasure": "TREASURE", "arena": "ARENA", "antechamber": "ANTECHAMBER",
               "vault": "VAULT", "onward": "ONWARD", "link": "LINK", "cap": "CAP", "stairwell": "STAIRWELL"}
THEME_DIR = os.path.join(DATA, "mega", "template_themes")


def theme_rules():
    """[(prefix, theme id)] of every data/nusmp/mega/template_themes/*.json (1.58.0, docs/labyrinth/mega_v2.md section 5.1): a template whose id starts with a prefix may
    only be drawn in sectors of that theme. Prefixes are written with or without the namespace; here they get it, like the pool entries have it. A file that cannot be
    read is skipped here and reported by mega_check.py."""
    out = []
    for path in sorted(glob.glob(os.path.join(THEME_DIR, "*.json"))):
        try:
            doc = json.load(open(path))
        except ValueError:
            continue
        theme = str(doc.get("theme", "")).strip().lower()
        for prefix in doc.get("templates", []):
            if isinstance(prefix, str) and prefix.strip():
                p = prefix.strip()
                out.append((p if ":" in p else "nusmp:" + p, theme))
    return out


def themes_of(template_id, rules):
    """The themes that may draw this template (sorted), or [] when it is free for every theme: the themes of the LONGEST matching prefix, the rule of TemplateThemes.java."""
    best, themes = -1, set()
    for prefix, theme in rules:
        if template_id.startswith(prefix):
            if len(prefix) > best:
                best, themes = len(prefix), {theme}
            elif len(prefix) == best:
                themes.add(theme)
    return sorted(themes)


def build():
    templates = {}
    rules = theme_rules()
    for path in sorted(glob.glob(os.path.join(DATA, "structure", "mega", "**", "*.nbt"), recursive=True)):
        name = os.path.relpath(path, os.path.join(DATA, "structure")).replace(os.sep, "/")[:-4]
        t = load(path, name)
        parts = name.split("/")
        kind = FOLDER_KIND.get(parts[1], "OTHER") if len(parts) > 2 else "OTHER"
        sockets = []
        for (x, y, z), o, d in t.jigsaws():
            sockets.append({"x": x, "y": y, "z": z, "facing": o.split("_")[0], "name": str(d["name"]), "target": str(d["target"]), "pool": str(d["pool"]),
                            "sel": int(d.get("selection_priority", 0)), "place": int(d.get("placement_priority", 0)), "final": str(d["final_state"])})
        markers = []
        for (x, y, z), c in sorted(t.cells.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            if c.block == "minecraft:structure_block" and c.nbt is not None:
                meta = str(c.nbt.get("metadata", ""))
                if meta.startswith("nusmp:"):
                    parts_m = meta.split(";")
                    args = {}
                    for a in parts_m[1:]:
                        if "=" in a:
                            k, v = a.split("=", 1)
                            args[k] = v
                        elif a:
                            args[a] = "true"
                    markers.append({"kind": parts_m[0][6:], "x": x, "y": y, "z": z, "args": args})
        templates["nusmp:" + name] = {"size": list(t.size), "kind": kind, "sockets": sockets, "markers": markers, "themes": themes_of("nusmp:" + name, rules)}
    pools = {}
    for path in sorted(glob.glob(os.path.join(DATA, "worldgen", "template_pool", "mega", "*.json"))):
        pid = "nusmp:mega/" + os.path.basename(path)[:-5]
        p = json.load(open(path))
        els = []
        empty = 0
        for e in p.get("elements", []):
            el = e.get("element", {})
            w = int(e.get("weight", 1))
            if el.get("element_type") == "minecraft:empty_pool_element":
                empty += w
            else:
                els.append({"template": el.get("location"), "weight": w})
        pools[pid] = {"fallback": p.get("fallback", "minecraft:empty"), "elements": els, "emptyWeight": empty}
    return {"module": L.MODULE, "floorRow": L.FLOOR_ROW, "height": L.HEIGHT, "templates": templates, "pools": pools}


def main(argv):
    cat = build()
    text = json.dumps(cat, indent=1, sort_keys=True) + "\n"
    if "--check" in argv:
        cur = open(OUT).read() if os.path.exists(OUT) else ""
        if cur != text:
            print("catalog.json is stale: run python3 tools/labyrinth/mega_catalog.py")
            return 1
        print("catalog.json is current (%d templates, %d pools)" % (len(cat["templates"]), len(cat["pools"])))
        return 0
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, "w").write(text)
    print("wrote %s: %d templates, %d pools" % (os.path.relpath(OUT, L.ROOT), len(cat["templates"]), len(cat["pools"])))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
