"""Builds the meshes, skins, icons and item models of the 35 weapons of 1.97.0 (wave 71):

    python3 -B tools/relics/new197_build.py [id ...]          (no ids = all; idempotent, deterministic)
    N197_PREVIEW=<dir> python3 -B tools/relics/new197_build.py vorpal_sword      (also renders the preview sheet of each)

The weapons are in new197_legend.py (13), new197_god.py (12) and new197_genesis.py (10); the table is new197_roster.py.
"""
import importlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from new197_roster import ROSTER                                         # noqa: E402

MODULES = {"LEGEND": ["new197_legend", "new197_legend2"], "GOD": ["new197_god", "new197_god2"], "GENESIS": ["new197_genesis", "new197_genesis2"]}


def builder(e):
    for name in MODULES[e["grade"]]:
        try:
            mod = importlib.import_module(name)
        except ImportError:
            continue
        fn = mod.BUILDERS.get(e["id"])
        if fn is not None:
            return fn
    return None


def main(only):
    n = 0
    for e in ROSTER:
        if only and e["id"] not in only:
            continue
        fn = builder(e)
        if fn is None:
            print("no builder yet for", e["id"])
            continue
        fn()
        n += 1
    print("built", n)


if __name__ == "__main__":
    main(set(sys.argv[1:]))
