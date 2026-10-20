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
    # the two post steps of the pipeline: the bows lie on vanilla's diagonal (bow_vanilla.py), the long weapons get their first-person display (new197_presets.py)
    import subprocess
    subprocess.run([sys.executable, "-B", os.path.join(HERE, "bow_vanilla.py")], check=False)
    import new197_presets
    new197_presets.main(False)
    # 1.102.0 (visual upgrade, slice god_melee_a): the upgraded meshes / skins / specs of the twelve God melee items are built ON TOP of the shipped ones (they read weapon_meshes/<id>.txt for their placement)
    subprocess.run([sys.executable, "-B", os.path.join(HERE, "..", "visual_upgrade", "god_melee_a", "build.py")] + sorted(only), check=False)


if __name__ == "__main__":
    main(set(sys.argv[1:]))
