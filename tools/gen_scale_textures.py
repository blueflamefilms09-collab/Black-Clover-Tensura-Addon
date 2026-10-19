"""Scale Magic particle textures (1.93.0 rebuild): the sprites of ScaleLayer and of the armour accents.

    python3 -B tools/gen_scale_textures.py        # writes textures/particle/scale_*.png

Sprites are grey + alpha (the vertex colour tints them). Deterministic. Built from tools/shakudo_vfx_kit.py.
"""
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import shakudo_vfx_kit as K

K_ = "scale"


def main():
    K.save(K.polar_sigil(256, "scallop", 13), K_ + "_sigil")
    K.save(K.ring_sprite(256, seed=13), K_ + "_ring")
    K.save(K.flare_sprite(128), K_ + "_flare")
    K.save(K.streak_sprite(64, 256), K_ + "_streak")
    K.save(K.glow_blob(128), K_ + "_glow")
    K.save(K.crack_sprite(256, 13), K_ + "_crack")
    K.save(K.mote_sprite(32), K_ + "_mote")
    K.save(K.scale_sprite(128, 3), K_ + "_scale")
    K.save(K.ripple_sprite(256, 4), K_ + "_ripple")
    K.save(K.poly_sprite(128, [(0.5, 0.02), (0.78, 0.45), (0.9, 0.98), (0.1, 0.98), (0.22, 0.45)], 5, base=0.7), K_ + "_fin")
    K.save(K.poly_sprite(64, [(0.5, 0.05), (0.85, 0.5), (0.5, 0.95), (0.15, 0.5)], 6), K_ + "_gleam")


if __name__ == "__main__":
    main()
