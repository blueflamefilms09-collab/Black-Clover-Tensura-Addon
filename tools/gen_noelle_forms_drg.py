"""Noelle's Sea Dragon God Form (1.99.0): Aura.WATER style 3 (standard) / 19 (character). Writes geo/entity/ndg_arm_*: see tools/noelle_dragon_kit.py.

    python3 -B tools/gen_noelle_forms_geo.py drg

Canon: after the contract with the sea god Leviathan, the Valkyrie Armor takes the Dragon Form (manga ch. 359: horned, winged, dragon-scaled; the kanji \u6d77\u7adc\u795e\u5f62\u614b). Deep-sea blue
scales with pale belly plates, pearl horns, silver trim, aqua membranes, a cyan glow in the seams.
"""
from noelle_dragon_kit import build_form

KEY = "ndg"


def build():
    return build_form(KEY, False)
