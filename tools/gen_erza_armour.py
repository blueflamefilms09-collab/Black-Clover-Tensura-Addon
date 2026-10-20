"""Requip: The Knight (Erza Scarlet), the Djinn-quality remake (1.99.0): the fourteen armours, piece by piece. The pieces of each armour, their anchors, delays and fly-in sides are the table
requip/ArmourSets.java; this script defines exactly those pieces (the unit tests compare the files with the table).

    python3 -B tools/gen_erza_armour.py                          # every armour
    python3 -B tools/gen_erza_armour.py heart_kreuz              # one armour
    python3 -B tools/gen_erza_armour.py heart_kreuz --only helm,chest      # some pieces of it
    python3 -B tools/erza_armour_audit.py --erza                 # the numbers (faces, texture, density) next to the Djinn's
    python3 -B tools/geo_check.py erza_heart_kreuz_arm_helm ...  # the structural check of the files

Files: assets/nusmp/geo/entity/erza_<armour>_arm_<piece>.geo.json (+ animations/entity/*.animation.json for the animated pieces, textures/entity/*.png and *_glow.png).
Layers: tools/erza_armour_paint.py (materials), tools/erza_armour_kit.py (geometry, atlas), tools/gen_erza_armour_core.py (lofts, spikes), tools/gen_erza_armour_parts.py (the standard pieces),
tools/gen_erza_armour_extras.py (wings, halos, tails, capes, backdrops), tools/gen_erza_armour_sets.py (palettes and styles).
"""
import os
import sys
import time

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_erza_armour_sets as SETS  # noqa: E402
import gen_erza_armour_parts as PARTS  # noqa: E402
import gen_erza_armour_limbs as LIMBS  # noqa: E402
from gen_erza_armour_core import Ctx  # noqa: E402

try:
    import gen_erza_armour_extras as EXTRAS  # noqa: E402
except ImportError:
    EXTRAS = None

STANDARD = ("helm", "gorget", "chest", "backplate", "belt", "skirt", "pauldron", "vambrace", "gauntlet", "cuisse", "greave", "sabaton")


def build(aid, only=None):
    c = Ctx(aid, SETS.PALETTES[aid], SETS.SPECS[aid])
    t0 = time.time()
    for name in STANDARD:
        fn = getattr(PARTS, name, None) or getattr(LIMBS, name, None)
        if fn is None or (only and name not in only):
            continue
        r = fn(c)
        for x in (r if isinstance(r, list) else [r]):
            pass
    if EXTRAS is not None:
        for ex in c.spec.get("extras", ()):
            kind, _, style = ex.partition(":")
            if only and kind not in only:
                continue
            getattr(EXTRAS, kind)(c, style)
    cubes = sum(r["cubes"] for r in c.made)
    faces = sum(r["faces"] for r in c.made)
    print("%-20s pieces %2d cubes %5d faces %5d  (%.1f s)" % (aid, len(c.made), cubes, faces, time.time() - t0))
    for r in c.made:
        print("   %-34s cubes %4d faces %4d tex %s" % (r["id"], r["cubes"], r["faces"], "%dx%d" % r["tex"]))
    return c.made


def main(argv):
    only = None
    if "--only" in argv:
        i = argv.index("--only")
        only = set(argv[i + 1].split(","))
        argv = argv[:i] + argv[i + 2:]
    ids = [a for a in argv if not a.startswith("--")] or list(SETS.SPECS)
    for aid in ids:
        build(aid, only)


if __name__ == "__main__":
    main(sys.argv[1:])
