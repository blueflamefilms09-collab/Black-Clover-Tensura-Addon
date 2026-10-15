"""Relics of Many Worlds, part 4, wave 30: the 3D model of the one hand-held weapon of the wave, Paradox, built with the owner's model method
(tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave30_models.py [id ...]          (no ids = all; idempotent, deterministic)
    RELICS_ZIP=<dir of the card images> W1_PREVIEW=<dir> python3 -B tools/relics/wave30_models.py paradox      (also renders the preview beside the card)

Run tools/relics/wave30_icons.py first: the mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon). It writes
assets/nusmp/weapon_meshes/paradox.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/paradox.png (512 px) and paradox_glow.png.
The helpers (wave1_models: add, paint_part, finish ...) are shared with wave 1. Under 14 named parts and 2800 quads.
"""
import importlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

MODULES = {"paradox": "wave30_m_paradox"}


def main(only=None):
    for item_id, mod in MODULES.items():
        if only and item_id not in only:
            continue
        importlib.import_module(mod).build()


if __name__ == "__main__":
    main(sys.argv[1:])
