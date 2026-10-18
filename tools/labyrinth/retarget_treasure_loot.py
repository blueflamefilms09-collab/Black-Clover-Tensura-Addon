#!/usr/bin/env python3
"""Points the chests of the Mega-Dungeon's treasure rooms (data/nusmp/structure/mega/treasure/*.nbt) at the hidden cache loot table (1.75.0).

Why: the owner wants relics to be findable in hidden rooms of ordinary floors, not only in the Crucible. The treasure rooms are those hidden rooms. Their chests rolled
`nusmp:chests/labyrinth_trial`; this swaps that reference for `nusmp:chests/hidden_cache`, which rolls the same trial loot (it includes that table) plus a 30% chance of one relic of
Normal to Rare grade (the grades that are not hard-capped, so the Crucible stays the only source of the capped ones: RelicExclusivity.allowsWildRoll(inMega, maxGrade)).
The old table is kept. Idempotent; run it after regenerating the rooms (the generators still name the old table). `--check` exits 1 when a template would change (RelicCacheTemplatesTest is the CI guard).
"""
import glob
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import nbt  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OLD, NEW = "nusmp:chests/labyrinth_trial", "nusmp:chests/hidden_cache"


def walk(v):
    """Replaces OLD by NEW in every string of the tree; returns how many were changed."""
    n = 0
    if isinstance(v, dict):
        for k in list(v.keys()):
            if isinstance(v[k], str) and v[k] == OLD:
                v[k] = nbt.String(NEW)
                n += 1
            else:
                n += walk(v[k])
    elif isinstance(v, list):
        for i, x in enumerate(v):
            if isinstance(x, str) and x == OLD:
                v[i] = nbt.String(NEW)
                n += 1
            else:
                n += walk(x)
    return n


def main(check):
    changed = 0
    for f in sorted(glob.glob(os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "structure", "mega", "treasure", "*.nbt"))):
        name, data = nbt.load(f)
        n = walk(data)
        if n:
            changed += 1
            print(("would change " if check else "changed ") + os.path.basename(f) + " (%d chests)" % n)
            if not check:
                nbt.dump(f, data, name)
    print("%d templates %s" % (changed, "to change" if check else "changed"))
    return 1 if (check and changed) else 0


if __name__ == "__main__":
    sys.exit(main("--check" in sys.argv))
