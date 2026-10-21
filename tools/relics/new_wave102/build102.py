"""Builds the assets of Wave102 (release 1.111.0): the meshes and sprites of the weapons, the sprites and item models of the gear, the worn and held models.

    python3 -B tools/relics/new_wave102/build102.py weapons [id ...]     the 9 weapon meshes, skins, sprites and item models (+ the bow / display / shield post steps)
    python3 -B tools/relics/new_wave102/build102.py gear                 the 20 gear sprites and item models
    python3 -B tools/relics/new_wave102/build102.py all
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))


def weapons(only):
    import weapons102 as W
    n = 0
    for wid, fn in W.BUILDERS.items():
        if only and wid not in only:
            continue
        fn()
        n += 1
    print("built %d weapons" % n)
    if not only:                                                     # the post steps of the weapon pipeline (all idempotent, each touches only what the new weapons need)
        import subprocess
        root = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
        for cmd in (["tools/relics/bow_vanilla.py"], ["tools/relics/new_wave102/presets102.py"], ["tools/gen_shield_models.py"], ["tools/safe_additive/scan_textures.py", "--fix"]):
            subprocess.run([sys.executable, "-B"] + cmd, cwd=root, check=False)


def main(argv):
    what = argv[0] if argv else "all"
    only = set(argv[1:])
    if what in ("weapons", "all"):
        weapons(only)
    if what in ("gear", "all"):
        import gear102
        gear102.main()


if __name__ == "__main__":
    main(sys.argv[1:])
