"""Nail Magic particle textures (1.93.0 rebuild): the sprites of NailLayer and of the armour accents.

    python3 -B tools/gen_nail_textures.py        # writes textures/particle/nail_*.png

Sprites are grey + alpha (the vertex colour tints them). Deterministic. Built from tools/shakudo_vfx_kit.py.
"""
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import shakudo_vfx_kit as K

K_ = "nail"


def main():
    K.save(K.polar_sigil(256, "nail", 17), K_ + "_sigil")
    K.save(K.ring_sprite(256, seed=17), K_ + "_ring")
    K.save(K.flare_sprite(128), K_ + "_flare")
    K.save(K.streak_sprite(64, 256), K_ + "_streak")
    K.save(K.glow_blob(128), K_ + "_glow")
    K.save(K.crack_sprite(256, 17), K_ + "_crack")
    K.save(K.mote_sprite(32), K_ + "_mote")
    K.save(K.nail_sprite(128, 3), K_ + "_nail")
    K.save(K.nail_sprite(64, 4), K_ + "_nail_small")
    K.save(K.poly_sprite(128, [(0.1, 0.35), (0.62, 0.12), (0.9, 0.2), (0.9, 0.5), (0.62, 0.55), (0.56, 0.95), (0.4, 0.95), (0.36, 0.55), (0.1, 0.5)], 7), K_ + "_hammer")
    K.save(K.poly_sprite(64, [(0.5, 0.02), (0.8, 0.5), (0.5, 0.98), (0.2, 0.5)], 6), K_ + "_spark")


if __name__ == "__main__":
    main()
