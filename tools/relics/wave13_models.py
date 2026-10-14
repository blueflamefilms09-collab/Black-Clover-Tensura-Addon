"""Relics of Many Worlds, wave 13: the nine blades as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave13_models.py [id ...]          (no ids = all nine; idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/wave13_models.py palladion      (also renders the preview beside the card)

Run tools/relics/wave13_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon). Per id it writes
assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png (512 px) and <id>_glow.png.
The helpers (wave1_models: add, paint_part, finish ...) are shared with wave 1; the reference cards are the second card set's images.
Each weapon stays under 14 named parts and 2800 quads (the first wave's 5000 to 7000 quad meshes were flagged as heavy).
"""
import importlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault("RELICS_ZIP", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics2")

MODULES = {"dream_glass": "wave13_m_dream_glass", "axe_of_the_crimson_saint": "wave13_m_saint_axe", "palladion": "wave13_m_palladion",
           "dagger_of_betrayal": "wave13_m_betrayal", "humbling_longsword": "wave13_m_humbling", "mountainguards_greataxe": "wave13_m_greataxe",
           "eschaton": "wave13_m_eschaton", "contingency_plan": "wave13_m_contingency", "time_bender_dadao": "wave13_m_dadao"}


def main(only=None):
    for item_id, mod in MODULES.items():
        if only and item_id not in only:
            continue
        if not os.path.exists(os.path.join(HERE, mod + ".py")):
            print("skip (not written yet):", item_id)
            continue
        importlib.import_module(mod).build()


if __name__ == "__main__":
    main(sys.argv[1:])
