"""Builds everything of wave 101 (release 1.111.0, doc docs/relics/genesis_new.md), in the order the pieces need each other:

    python3 -B tools/relics/new_wave101/build_all.py [--no-weapons]

  1. bake_all.py   the twelve upgrade models, skins, fx tables, the override table and the act effects
  2. icons.py      the twelve 16 x 16 sprites and the item models of the non-weapons
  3. weapons.py    the three weapons (mesh, skins, sprite, item models, class rows, display presets)
  4. lang.py       tools/lang/genesis_new.json
  5. the shared generators that read the wave sources: the Curios tags and the Better Combat inventory and attribute files (they add only the ids of this wave)

Idempotent and deterministic. The one hook line that runs it with the shared generators is at the end of tools/relics/gen_worn_all.py (main).
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, HERE)


def run(*args):
    subprocess.run([sys.executable, "-B", *args], check=True, cwd=ROOT)


def main(argv):
    run(os.path.join(HERE, "bake_all.py"))
    run(os.path.join(HERE, "icons.py"))
    if "--no-weapons" not in argv:
        run(os.path.join(HERE, "weapons.py"))
    run(os.path.join(HERE, "lang.py"))
    run(os.path.join(ROOT, "tools", "relics", "gen_curios_tags.py"))
    run(os.path.join(ROOT, "tools", "bettercombat", "inventory.py"))
    run(os.path.join(ROOT, "tools", "bettercombat", "gen_attributes.py"))
    return 0


if __name__ in ("__main__", "wave101_hook"):
    sys.exit(main(sys.argv[1:] if __name__ == "__main__" else []))
