#!/usr/bin/env python3
"""
Writes every template pool of the Mega-Dungeon (data/nusmp/worldgen/template_pool/mega/*.json) from the template folders and the weights of
tools/labyrinth/mega_pools.json (1.54.0, docs/labyrinth/mega.md section 4, docs/labyrinth/mega_arch.md).

    python3 tools/labyrinth/gen_mega_pools.py            # (re)writes the pools, prints one line per pool
    python3 tools/labyrinth/gen_mega_pools.py --check    # exit 1 when a pool file on disk differs from what would be written
    python3 tools/labyrinth/gen_mega_pools.py --strict   # exit 1 when a pool has no elements (the game accepts it, the checker does not)

Why a generator: the pools are derived data. Which template belongs to which pool is decided by its FOLDER (structure/mega/<folder>/<name>.nbt) and,
for the vault and the Way Onward, by its file name prefix (one_*, two_*, three_*, straight_*, left_*, right_*). Every builder of a room drops its
templates into the right folder and runs this script, instead of hand-editing JSON that nine people would conflict on. The output is sorted and
deterministic, so a diff shows exactly the templates that came or went.

Why weights are per FOLDER share: a pool such as `side` is drawn per doorway, template by template. If every template weighed the same, a folder
with twenty templates would drown one with three. Here a folder's share of the pool is fixed in mega_pools.json and the folder's templates split it
(by relative weights), so adding templates to a folder changes the variety, not the mix.

A pool with no templates is still written (an empty `elements` list is valid for the game: a doorway that draws it simply falls back to the next
pool); the script says so, because `mega_check.py` calls an empty pool an error: it means a builder's folder is still empty.
"""
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402

STRUCT = os.path.join(L.RES, "data", "nusmp", "structure")
POOL_DIR = os.path.join(L.RES, "data", "nusmp", "worldgen", "template_pool", "mega")
CONFIG = os.path.join(HERE, "mega_pools.json")


def templates_in(folder):
    """[(template id like 'mega/hall/crypt_a', file stem)] of structure/mega/<folder>/ (sorted)."""
    out = []
    for p in sorted(glob.glob(os.path.join(STRUCT, "mega", folder, "*.nbt"))):
        stem = os.path.basename(p)[:-4]
        out.append(("mega/%s/%s" % (folder, stem), stem))
    return out


def relative_weight(relative, tid):
    best, best_len = 1, -1
    for prefix, w in relative.items():
        if tid.startswith(prefix) and len(prefix) > best_len:
            best, best_len = w, len(prefix)
    return best


def pool_elements(cfg, pool):
    """[(template id, weight)] for one pool, in sorted order."""
    scale = cfg.get("scale", 20)
    relative = pool.get("relative", {})
    absolute = pool.get("absolute", False)
    out = []
    for folder, share in sorted(pool["folders"].items()):
        items = [(tid, stem) for tid, stem in templates_in(folder) if stem.startswith(pool.get("filter", ""))]
        if not items:
            continue
        rels = [relative_weight(relative, tid) for tid, _ in items]
        total = float(sum(rels))
        for (tid, _stem), rel in zip(items, rels):
            w = rel if absolute else int(round(share * scale * rel / total))
            out.append((tid, max(1, min(150, w))))
    return out


def element_json(tid, weight, processors):
    return {"weight": weight, "element": {"element_type": "minecraft:single_pool_element", "location": "nusmp:" + tid, "processors": processors, "projection": "rigid"}}


THEME_DIR = os.path.join(L.RES, "data", "nusmp", "mega", "template_themes")


def theme_weights():
    """1.58.0: {template id prefix: relative weight} gathered from every data/nusmp/mega/template_themes/*.json (the `relative` map of each theme file, same
    style of key as mega_pools.json: 'mega/hall/holy_church_'). A theme's builder tunes how often its own rooms are drawn in its own file instead of editing
    the shared mega_pools.json; the rooms are only ever drawn in sectors of that theme (the `templates` prefixes of the same file, read by the game)."""
    out = {}
    for path in sorted(glob.glob(os.path.join(THEME_DIR, "*.json"))):
        try:
            doc = json.load(open(path))
        except ValueError:
            continue
        for prefix, w in (doc.get("relative") or {}).items():
            out[prefix] = int(w)
    return out


def build():
    """{pool name: json text} for every pool of the config."""
    cfg = json.load(open(CONFIG))
    extra = theme_weights()
    if extra:
        for pool in cfg["pools"].values():
            pool["relative"] = dict(pool.get("relative", {}), **extra)
    out = {}
    report = []
    for name, pool in sorted(cfg["pools"].items()):
        els = sorted(pool_elements(cfg, pool))
        elements = [element_json(tid, w, cfg["processors"]) for tid, w in els]
        if pool.get("empty", 0) > 0:
            elements.append({"weight": pool["empty"], "element": {"element_type": "minecraft:empty_pool_element"}})
        doc = {"fallback": pool.get("fallback", "minecraft:empty"), "elements": elements}
        out[name] = json.dumps(doc, indent=2) + "\n"
        report.append((name, len(els), sum(w for _, w in els) + pool.get("empty", 0)))
    return out, report


def main(argv):
    texts, report = build()
    stale = 0
    empty = []
    for name, text in texts.items():
        path = os.path.join(POOL_DIR, name + ".json")
        have = open(path).read() if os.path.exists(path) else None
        if "--check" in argv:
            if have != text:
                stale += 1
                print("STALE %s" % os.path.relpath(path, L.ROOT))
            continue
        os.makedirs(POOL_DIR, exist_ok=True)
        if have != text:
            open(path, "w").write(text)
    for name, n, total in report:
        flag = ""
        if n == 0:
            empty.append(name)
            flag = "   <-- EMPTY (no template in its folder yet)"
        print("pool %-16s %3d templates, total weight %4d%s" % (name, n, total, flag))
    if "--check" in argv:
        print("%d pool file(s) stale" % stale)
        return 1 if stale else 0
    if empty:
        print("empty pools: %s" % ", ".join(empty))
    return 1 if (empty and "--strict" in argv) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
