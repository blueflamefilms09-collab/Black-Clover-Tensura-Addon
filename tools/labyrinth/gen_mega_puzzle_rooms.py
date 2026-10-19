#!/usr/bin/env python3
"""
PUZZLE-ROOMS of the Mega-Dungeon (1.94.0): two open "galleries" in the `puzzle/` folder, next to the Spire's sigil halls and the themed puzzle rooms that already exist. Nothing is replaced.

    python3 tools/labyrinth/gen_mega_puzzle_rooms.py                  # (re)writes every template
    python3 tools/labyrinth/gen_mega_puzzle_rooms.py gallery_a        # only these
    python3 tools/labyrinth/gen_mega_puzzle_rooms.py --check          # regenerates in memory, compares with the files on disk, runs every template check
    python3 tools/labyrinth/gen_mega_puzzle_rooms.py --list

Why: every puzzle room the Mega-Dungeon had before 1.94.0 is built around its own blocks (four sigil plates, a Riddle Stone, a puzzle door), so a room of that kind can only ever hold THAT puzzle. The
twenty-nine puzzles of `labyrinth/mega/puzzle` lay themselves out in an empty floor at run time (floor tiles, pillars, chests, a lectern), so they need rooms that are empty on purpose: a wide, flat,
well lit hall with three doors, a clear floor, banners and braziers along the walls, and ONE `nusmp:puzzle` marker in the middle that tells the puzzle director where the layout goes. These are those rooms.
The folder is `puzzle/`, so the room kind is PUZZLE and the pools, the factions and the audit treat them like every other puzzle room; the template name starts with `gallery`, which the weights file
(`data/nusmp/mega/puzzles/weights.json`, key `openRooms`) lists as "no apparatus of its own: the director chooses the puzzle by theme and depth, or leaves the room to the library's puzzle mechanics".
Same footprint and doorways as the Soldiers' Quarters (`gen_mega_rooms3.py`): doors north, south and east, so a gallery fits wherever a hall fits. No spawn markers (a puzzle is not a fight).
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import gen_mega_rooms1 as R1  # noqa: E402
from mega_decor_r1 import GROUND, Hall  # noqa: E402

ROOMS = {}


def room(name):
    def deco(fn):
        ROOMS[name] = fn
        return fn
    return deco


def gallery(name, wear=0.08):
    """A 3 x 2 hall with the doorways of the Soldiers' Quarters: doors north (22), south (4) and east (13); only the doorway aprons are routes, the floor is otherwise free."""
    h = Hall("mega/puzzle/" + name, 3, 2, 8, wear=wear)
    h.door("n", 22)
    h.door("s", 4)
    h.door("e", 13)
    return h


def finish(h, faction="construct"):
    R1.finish_common(h)
    h.faction(faction, 13, 7)
    D.auto_light(h)
    return h


def inlay(h, block, x1, z1, x2, z2):
    """A one-block line of a different floor block round a rectangle (the edge of the puzzle floor)."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if x in (x1, x2) or z in (z1, z2):
                h.floor_set(x, z, block)


# ===================================================================================================================
# 1. gallery_a: the Gallery of Trials (a clear tiled floor, a banner on every wall, braziers in the corners)
# ===================================================================================================================
@room("gallery_a")
def gallery_a():
    h = gallery("gallery_a")
    inlay(h, "minecraft:chiseled_stone_bricks", 4, 3, 22, 14)
    for x, z in ((4, 3), (16, 3), (10, 14), (18, 14)):
        D.brazier(h, x, z)
    D.dress_wall(h, "s", [D.it_banner("purple", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5)], lo=7, hi=24, step=(3, 4))
    D.dress_wall(h, "n", [D.it_banner("cyan", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5)], lo=3, hi=18, step=(3, 4))
    D.beams(h, "z", [6, 14, 22])
    h.marker(13, GROUND + 1, 8, "puzzle")
    return finish(h)


# ===================================================================================================================
# 2. gallery_b: the Hall of Mechanisms (a wider inlay of polished stone, wall lamps, a clear centre)
# ===================================================================================================================
@room("gallery_b")
def gallery_b():
    h = gallery("gallery_b", wear=0.12)
    inlay(h, L.POLISHED, 5, 4, 21, 13)
    inlay(h, L.POLISHED, 4, 3, 22, 14)
    for x, z in ((4, 3), (16, 3), (10, 14), (18, 14)):
        D.brazier(h, x, z)
    D.dress_wall(h, "s", [D.it_banner("orange", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5)], lo=7, hi=24, step=(4, 4))
    D.dress_wall(h, "n", [D.it_banner("gray", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5)], lo=3, hi=18, step=(4, 4))
    D.beams(h, "z", [6, 14, 22])
    h.marker(13, GROUND + 1, 8, "puzzle")
    return finish(h, "goblin")


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("names", nargs="*")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--force", action="store_true")
    a = ap.parse_args(argv)
    if a.list:
        print("\n".join(ROOMS))
        return 0
    bad = 0
    for n in a.names or list(ROOMS):
        if n not in ROOMS:
            print("unknown template", n)
            return 2
        h = ROOMS[n]()
        h.finish()
        problems, stats = h.check()
        problems = [p for p in problems if p != "no spawn marker"]                     # a puzzle room carries no spawn markers (a puzzle is not a fight)
        if stats.get("markers", {}).get("spawn"):
            problems.append("a puzzle room must carry no spawn marker")
        if stats.get("markers", {}).get("puzzle") != 1:
            problems.append("a puzzle room carries exactly one puzzle marker")
        if problems:
            bad += 1
        if a.check:
            p = h.mt.t.path(L.RES)
            if not (os.path.exists(p) and R1._same(h, p)):
                print("STALE %s (the file on disk differs from the generator)" % n)
                bad += 1
        elif not problems or a.force:
            h.save()
        else:
            print("NOT WRITTEN %s" % n)
        print("%s %-22s %5d blocks %2d doors %2d stands light min %s median %s dark %s%% markers %s" % ("ok  " if not problems else "FAIL", n, stats.get("blocks", 0), len(h.doors),
              stats.get("armor_stands", 0), stats.get("eye_light_min"), stats.get("eye_light_median"), round(100 * stats.get("dark_fraction", 0), 1), stats.get("markers")))
        for p in problems:
            print("     - " + p)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
