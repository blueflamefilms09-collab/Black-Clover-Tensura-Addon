"""Shakudo Magic particle textures (1.93.0 rebuild): the sprites of ShakudoLayer and of the armour accents.

    python3 -B tools/gen_shakudo_textures.py        # writes textures/particle/shakudo_*.png

Sprites are grey + alpha (the vertex colour tints them). Deterministic. Built from tools/shakudo_vfx_kit.py.
"""
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import shakudo_vfx_kit as K

K_ = "shakudo"


def main():
    K.save(K.polar_sigil(256, "scroll", 14), K_ + "_sigil")
    K.save(K.ring_sprite(256, seed=14), K_ + "_ring")
    K.save(K.flare_sprite(128), K_ + "_flare")
    K.save(K.streak_sprite(64, 256), K_ + "_streak")
    K.save(K.glow_blob(128), K_ + "_glow")
    K.save(K.crack_sprite(256, 14), K_ + "_crack")
    K.save(K.mote_sprite(32), K_ + "_mote")
    K.save(K.plate_sprite(128, 3), K_ + "_plate")
    K.save(K.flake_sprite(64, 5), K_ + "_flake")
    K.save(K.poly_sprite(128, [(0.12, 0.7), (0.3, 0.2), (0.62, 0.08), (0.9, 0.34), (0.82, 0.86), (0.4, 0.94)], 7), K_ + "_slab")
    K.save(K.poly_sprite(64, [(0.1, 0.5), (0.4, 0.1), (0.9, 0.3), (0.8, 0.8), (0.3, 0.9)], 9), K_ + "_chip")


if __name__ == "__main__":
    main()
