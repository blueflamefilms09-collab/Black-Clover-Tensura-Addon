#!/usr/bin/env python3
"""
Generates the Landing and Safe Zone templates of the Mega-Dungeon (1.54.0, docs/labyrinth/mega_safe.md):

    structure/mega/landing/landing_a, landing_b          3 x 3 modules (27 x 27): the safe hall at the start of every sector
    structure/mega/safe/hearth_hall_a, hearth_hall_b     3 x 3: the two-storey vault hall and the inn common room with its central fireplace
    structure/mega/safe/workshop_a, workshop_b           2 x 3 (18 x 27): the smithy and the artisans' hall
    structure/mega/safe/refectory                        2 x 2 (18 x 18): the long table and the kitchen
    structure/mega/safe/chapel                           2 x 2: pews, an altar, glowing stained glass, pilgrims' cots

    python3 tools/labyrinth/gen_mega_safe.py                    # build, verify and write every template
    python3 tools/labyrinth/gen_mega_safe.py landing_a chapel   # only the named ones
    python3 tools/labyrinth/gen_mega_safe.py --check            # build and verify, write nothing
    python3 tools/labyrinth/gen_mega_safe.py --list             # build and verify, print the numbers, write nothing
    python3 tools/labyrinth/gen_mega_safe.py --render out/      # also draw each room (four isometric views, a plan, a light map) into out/ (needs Pillow)

After a change, run `python3 tools/labyrinth/mega_check.py` (the contract: only "pool nusmp:mega/side does not exist" and "not referenced by any pool"
may remain for these rooms) and `python3 tools/labyrinth/mega_catalog.py` (the catalog the JUnit tests read).

Why a generator and not hand-built NBT: the rooms are deterministic code (same script, same bytes, reviewable in a diff, regenerated with one command
when the palette changes), and every template is VERIFIED before it is written (`mega_safe_verify.verify`): a template that fails (a closed shell, a
reachable door and anchor, light on every floor cell, nothing hanging from nothing, markers where Java expects them) is not saved.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_safe_halls as HALLS  # noqa: E402
import mega_safe_landing as LAND  # noqa: E402
import mega_safe_shops as SHOPS  # noqa: E402
import mega_safe_verify as V  # noqa: E402

BUILDERS = [
    ("landing_a", LAND.landing_a),
    ("landing_b", LAND.landing_b),
    ("hearth_hall_a", HALLS.hearth_hall_a),
    ("hearth_hall_b", HALLS.hearth_hall_b),
    ("workshop_a", SHOPS.workshop_a),
    ("workshop_b", SHOPS.workshop_b),
    ("refectory", SHOPS.refectory),
    ("chapel", SHOPS.chapel),
]


def build_all(names=None):
    out = []
    for name, fn in BUILDERS:
        if names and name not in names:
            continue
        sr = fn()
        rep = sr.finish_and_check()
        out.append((name, sr, rep))
    return out


def render(name, sr, rep, directory):
    """Four isometric cutaways (one from each corner), a plan of the floor level with the markers, and a block-light map, as PNG files in `directory`."""
    os.makedirs(directory, exist_ok=True)
    for k in range(4):
        sx, sz = (sr.W, sr.D) if k % 2 == 0 else (sr.D, sr.W)
        V.render(sr, os.path.join(directory, "%s_iso%d.png" % (name, k)), xr=(0, sx - 3), yr=(L.FLOOR_ROW, sr.top), zr=(0, sz - 3), scale=14, turn=k, markers=False)
    V.render(sr, os.path.join(directory, "%s_plan.png" % name), top=True, scale=18, yr=(L.FLOOR_ROW, L.FLOOR_ROW + 4), markers=True)
    V.render(sr, os.path.join(directory, "%s_light.png" % name), top=True, scale=14, light=rep.light)


def main(argv):
    check_only = "--check" in argv
    listing = "--list" in argv
    render_dir = None
    names = []
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "--render":
            if i + 1 >= len(argv):
                print("--render needs a directory")
                return 2
            render_dir = argv[i + 1]
            i += 1
        elif not a.startswith("--"):
            names.append(a)
        i += 1
    known = {n for n, _ in BUILDERS}
    unknown = [n for n in names if n not in known]
    if unknown:
        print("unknown template(s): %s (known: %s)" % (", ".join(unknown), ", ".join(sorted(known))))
        return 2
    results = build_all(names or None)
    bad = 0
    for name, sr, rep in results:
        print(rep.text())
        if not rep.ok:
            bad += 1
        if render_dir:
            render(name, sr, rep, render_dir)
        if rep.ok and not check_only and not listing:
            p = sr.save()
            print("   wrote %s (%d bytes)" % (os.path.relpath(p, L.ROOT), os.path.getsize(p)))
    print("%d template(s), %d failed verification" % (len(results), bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
