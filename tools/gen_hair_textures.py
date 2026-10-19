"""Hair Magic particle textures (1.93.0 rebuild): the sprites of HairLayer and of the armour accents.

    python3 -B tools/gen_hair_textures.py        # writes textures/particle/hair_*.png

Sprites are grey + alpha (the vertex colour tints them). Deterministic. Built from tools/shakudo_vfx_kit.py.
"""
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import shakudo_vfx_kit as K

K_ = "hair"


def main():
    K.save(K.polar_sigil(256, "curl", 11), K_ + "_sigil")
    K.save(K.ring_sprite(256, seed=11), K_ + "_ring")
    K.save(K.flare_sprite(128), K_ + "_flare")
    K.save(K.streak_sprite(64, 256), K_ + "_streak")
    K.save(K.glow_blob(128), K_ + "_glow")
    K.save(K.crack_sprite(256, 11), K_ + "_crack")
    K.save(K.mote_sprite(32), K_ + "_mote")
    K.save(K.lock_sprite(128, 3), K_ + "_lock")
    K.save(K.lock_sprite(128, 5, curl=1.6), K_ + "_lock2")
    K.save(K.lock_sprite(64, 7, curl=0.4), K_ + "_strand")
    K.save(K.flake_sprite(64, 11), K_ + "_tuft")


if __name__ == "__main__":
    main()
