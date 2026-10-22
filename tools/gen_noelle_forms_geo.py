"""Noelle Silva's armour forms (1.99.0): writes every geo model, texture, glow map and animation of the four forms (docs/magic/noelle_forms.md).

    python3 -B tools/gen_noelle_forms_geo.py              # all forms
    python3 -B tools/gen_noelle_forms_geo.py val mer      # only these (val = Valkyrie / Saint Valkyrie, mer = Mermaid, drg = Sea Dragon God, blk = Black Dragon)
    python3 -B tools/geo_check.py nvl_arm_wings ...

The painter is client/aura/NoelleFormsAura.java (Aura.WATER, styles 1 to 4, tier bit 16: book/NoelleFormRules). Every piece is a GeoLite model in whole-body coordinates (see
tools/noelle_forms_lib.py); the shell / haze textures of the water skin and the ink skin are written by tools/gen_noelle_forms_textures.py.
"""
import importlib
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

FORMS = {"val": "gen_noelle_forms_val", "mer": "gen_noelle_forms_mer", "drg": "gen_noelle_forms_drg", "blk": "gen_noelle_forms_blk"}


def clean_glow(model_id):
    """Zero the colour under every fully transparent texel of the model's glow map (docs/vfx_square_bug.md: nothing the additive passes draw may carry colour under alpha 0)."""
    from geo_builder import ROOT
    path = os.path.join(ROOT, "textures", "entity", model_id + "_glow.png")
    a = np.array(Image.open(path).convert("RGBA"))
    a[a[..., 3] == 0, :3] = 0
    Image.fromarray(a, "RGBA").save(path)


def main(argv):
    want = argv or list(FORMS)
    total = 0
    for f in want:
        if f not in FORMS:
            sys.exit("unknown form %s (one of %s)" % (f, ", ".join(FORMS)))
        mod = importlib.import_module(FORMS[f])
        made = mod.build()
        cubes = sum(m.cube_count() for m in made)
        print("== %s: %d models, %d cubes" % (f, len(made), cubes))
        for m in made:
            clean_glow(m.id)
            print("   %-24s cubes %3d" % (m.id, m.cube_count()))
        total += cubes
    print("total cubes", total)


if __name__ == "__main__":
    main(sys.argv[1:])
