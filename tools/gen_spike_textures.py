"""Spike Magic particle textures (1.93.0 rebuild): the sprites of SpikeLayer and of the armour accents.

    python3 -B tools/gen_spike_textures.py        # writes textures/particle/spike_*.png

Sprites are grey + alpha (the vertex colour tints them). Deterministic. Built from tools/shakudo_vfx_kit.py.
"""
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import shakudo_vfx_kit as K

K_ = "spike"


def main():
    K.save(K.polar_sigil(256, "spike", 22), K_ + "_sigil")
    K.save(K.ring_sprite(256, seed=22), K_ + "_ring")
    K.save(K.flare_sprite(128), K_ + "_flare")
    K.save(K.streak_sprite(64, 256), K_ + "_streak")
    K.save(K.glow_blob(128), K_ + "_glow")
    K.save(K.crack_sprite(256, 22), K_ + "_crack")
    K.save(K.mote_sprite(32), K_ + "_mote")
    K.save(K.spike_sprite(128, 3), K_ + "_spike")
    K.save(K.spike_sprite(128, 4, lean=0.12), K_ + "_spike2")
    K.save(K.poly_sprite(64, [(0.5, 0.02), (0.92, 0.6), (0.6, 0.98), (0.12, 0.7)], 8), K_ + "_shard")
    K.save(K.poly_sprite(128, [(0.08, 0.9), (0.28, 0.5), (0.5, 0.04), (0.72, 0.5), (0.92, 0.9), (0.5, 0.78)], 9, base=0.68), K_ + "_burst")


if __name__ == "__main__":
    main()
