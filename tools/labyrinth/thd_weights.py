#!/usr/bin/env python3
"""
Why: the brief says 'a themed sector should be about half signature rooms; tune with the real pool maths of gen_mega_pools.py', and the pool maths has changed twice while the themes were built, so
this script reproduces all three versions of the arithmetic from the real files (the folders, mega_pools.json, every data/nusmp/mega/template_themes/*.json) and answers the question that matters:
in a sector of theme T, which share of the templates that the `side` pool can draw is a signature room of T?

    absolute (the default): the lead's patch of 1.59.0 (`tha_pools_absolute.patch`). A room that a template_themes file claims is drawn with the weight written in that file, as it stands (1..150),
        and takes nothing from the folder's share; the free rooms of the folder split the share by their own relative weights alone. So the free rooms weigh the same in every sector, however many
        themes put rooms into the folder, and a themed room is compared with the free rooms it competes with.
    --free-only: an earlier proposal of this builder: the claimed rooms stay RELATIVE weights, but the denominator of a folder is the relative mass of its free rooms only.
    --legacy: the generator as it was in 1.58.0: weight = round(share x scale x relative / sum of the relative weights of the whole folder), restricted rooms included in the sum. Every theme that puts
        rooms into a folder then shrinks the weight of every free room in every sector, and the floor of weight 1 flattens the free rooms (the trap this builder found). `--others N` adds N x the weight
        mass of THIS builder's themes to the folder totals, as the other builders' themes will when the branches are merged.

    python3 tools/labyrinth/thd_weights.py                    # the four THEME-D themes, absolute weights, scale 20
    python3 tools/labyrinth/thd_weights.py --scale 100        # one scale
    python3 tools/labyrinth/thd_weights.py --legacy           # the 1.58.0 arithmetic, with the other twelve themes counted like these four three times over (--others 0 for this branch alone)
    python3 tools/labyrinth/thd_weights.py --free-only        # the earlier proposal
"""
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_mega_pools as GP  # noqa: E402

ROOM_FOLDERS = ("hall", "trap", "puzzle", "trial", "treasure", "cavern", "set", "safe")
MINE = ("fungal_underdark", "infernal_foundry", "drowned_crypt", "spider_warrens")


def load_themes():
    """{theme id: (prefixes without the namespace, relative map)} of every template_themes file."""
    out = {}
    for path in sorted(glob.glob(os.path.join(GP.THEME_DIR, "*.json"))):
        doc = json.load(open(path))
        out[doc["theme"]] = ([p.replace("nusmp:", "") for p in doc.get("templates", [])], doc.get("relative", {}))
    return out


def owner(tid, themes):
    """The theme a template is restricted to (longest prefix), or None."""
    best, best_len = None, -1
    for theme, (prefixes, _rel) in themes.items():
        for p in prefixes:
            if tid.startswith(p) and len(p) > best_len:
                best, best_len = theme, len(p)
    return best


def weights(cfg, pool, scale, extra_mass, mode="absolute"):
    """[(tid, weight, folder)] of one pool at `scale`. `mode` is 'absolute', 'free' or 'legacy' (see the header); `extra_mass` {folder: relative mass} is added to the folder totals in the legacy
    mode only (other themes' rooms that are not on disk)."""
    pool_relative = dict(pool.get("relative", {}))
    claimed = GP.theme_weights()
    themes = load_themes()
    out = []
    for folder, share in sorted(pool["folders"].items()):
        items = [(tid, stem) for tid, stem in GP.templates_in(folder) if stem.startswith(pool.get("filter", ""))]
        if not items:
            continue
        if mode == "absolute":
            own = [(tid, stem) for tid, stem in items if owner(tid, themes) is not None]
            for tid, _stem in own:
                out.append((tid, max(1, min(150, GP.relative_weight(claimed, tid))), folder))
            items = [x for x in items if x not in own]
            if not items:
                continue
            rels = [GP.relative_weight(pool_relative, tid) for tid, _ in items]
            total = float(sum(rels))
        else:
            relative = dict(pool_relative)
            relative.update(claimed)
            rels = [GP.relative_weight(relative, tid) for tid, _ in items]
            if mode == "free":
                total = float(sum(r for (tid, _s), r in zip(items, rels) if owner(tid, themes) is None))
            else:
                total = float(sum(rels)) + extra_mass.get(folder, 0.0)
        for (tid, _stem), rel in zip(items, rels):
            out.append((tid, max(1, min(150, int(round(share * scale * rel / total)))), folder))
    return out


def report(scale, others, mode):
    cfg = json.load(open(GP.CONFIG))
    themes = load_themes()
    pool = cfg["pools"]["side"]
    # the mass the other twelve themes would add per folder, if each were built like these four (others x the mass of the four), counted per folder (legacy mode)
    own_mass = {}
    relative = dict(pool.get("relative", {}))
    relative.update(GP.theme_weights())
    for folder in pool["folders"]:
        for tid, _stem in GP.templates_in(folder):
            if owner(tid, themes) in MINE:
                own_mass[folder] = own_mass.get(folder, 0.0) + GP.relative_weight(relative, tid)
    extra = {f: m * others for f, m in own_mass.items()}
    els = weights(cfg, pool, scale, extra, mode)
    label = {"absolute": "absolute weights of the claimed rooms (the lead's patch); neutral sectors unchanged by any number of themes",
             "free": "free-only denominator (an earlier proposal); neutral sectors unchanged",
             "legacy": "1.58.0 arithmetic, other themes counted as %.1f x these four" % others}[mode]
    print("scale %d, %s" % (scale, label))
    print("  %-10s %5s %7s" % ("folder", "free", "w_free"))
    free = {}
    for tid, w, folder in els:
        if owner(tid, themes) is None:
            free.setdefault(folder, []).append(w)
    for folder in ROOM_FOLDERS + ("corridor", "junction"):
        ws = free.get(folder, [])
        print("  %-10s %5d %7d" % (folder, len(ws), sum(ws)))
    # the price the neutral sectors pay in the legacy arithmetic: the folder's total is shared with every themed room, so a free room's weight shrinks by this factor
    if mode == "legacy":
        for folder in ROOM_FOLDERS:
            free_rel = sum(GP.relative_weight(relative, tid) for tid, _s in GP.templates_in(folder) if owner(tid, themes) is None)
            themed_rel = own_mass.get(folder, 0.0) * (1.0 + others)
            print("  neutral sector: %-9s keeps %3.0f%% of its nominal weight (free relative %d, themed relative %d)" % (folder, 100.0 * free_rel / max(1.0, free_rel + themed_rel), free_rel, themed_rel))
    print("  per theme: share of all draws of the `side` pool / of its room draws | signature weight in folder / free weight in folder")
    for theme in MINE:
        sig = [(tid, w, f) for tid, w, f in els if owner(tid, themes) == theme]
        free_els = [(tid, w, f) for tid, w, f in els if owner(tid, themes) is None]
        empty = pool.get("empty", 0)
        allowed_all = sum(w for _t, w, _f in free_els) + sum(w for _t, w, _f in sig) + empty
        sig_all = sum(w for _t, w, _f in sig)
        allowed_rooms = sum(w for _t, w, f in free_els if f in ROOM_FOLDERS) + sum(w for _t, w, f in sig if f in ROOM_FOLDERS)
        sig_rooms = sum(w for _t, w, f in sig if f in ROOM_FOLDERS)
        by_folder = {}
        for _t, w, f in sig:
            by_folder[f] = by_folder.get(f, 0) + w
        line = ", ".join("%s %d/%d" % (f, by_folder.get(f, 0), sum(free.get(f, []))) for f in ROOM_FOLDERS if by_folder.get(f))
        print("  %-17s %2.0f%% / %2.0f%% | %s" % (theme, 100.0 * sig_all / allowed_all, 100.0 * sig_rooms / max(1, allowed_rooms), line))


def main(argv):
    others = 3.0
    mode = "absolute"
    if "--legacy" in argv:
        mode = "legacy"
    if "--free-only" in argv:
        mode = "free"
    configured = json.load(open(GP.CONFIG)).get("scale", 20)
    scales = [20, 100, 200] if mode == "legacy" else [configured]
    if "--others" in argv:
        others = float(argv[argv.index("--others") + 1])
    if "--scale" in argv:
        scales = [int(argv[argv.index("--scale") + 1])]
    for s in scales:
        report(s, others, mode)
        print()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
