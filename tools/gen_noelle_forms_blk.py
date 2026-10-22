"""Noelle's Black Dragon Form (1.99.0): Aura.WATER style 4 (standard) / 20 (character). Writes geo/entity/nbk_arm_*: see tools/noelle_dragon_kit.py.

    python3 -B tools/gen_noelle_forms_geo.py blk

The dark form of the owner's reference: obsidian scales with a blue flame in the seams, black horns with red tips, torn wings with blue-flame veins, spikes, red eyes (drawn by the painter).
Gated in the game (book.NoelleFormState): Noelle's grimoire only, ninety seconds, exhaustion afterwards, refused during Devil Union.
"""
from noelle_dragon_kit import build_form

KEY = "nbk"


def build():
    return build_form(KEY, True)
