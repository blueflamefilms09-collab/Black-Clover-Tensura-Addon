"""The decal recipe of every God worn relic's skin (skin.py): the emblem on its crest, the motif of its panels and surface weave, the border patterns, a seed for its runes."""

# crest / panel / surface: motif names of skin.MOTIFS; border / trim2: border pattern 0..5 (greek key, thorns, scallops, chain links, waves, diamonds)
SPECS = {
    "flammes_founder_cloak": {"crest": "star_staff", "panel": "star_burst", "border": 0, "trim2": 2, "surface": "star_burst", "seed": 101},
    "mantle_of_the_overlord": {"crest": "skull_crown", "panel": "crown_glyph", "border": 1, "trim2": 5, "surface": "skull_crown", "seed": 102},
    "pactbreaker_shroud": {"crest": "broken_seal", "panel": "pact_chain", "border": 3, "trim2": 1, "surface": "broken_seal", "seed": 103},
    "shadow_monarchs_mantle": {"crest": "shadow_crown", "panel": "edge_blades", "border": 1, "trim2": 4, "surface": "shadow_crown", "seed": 104},
    "shadows_slime_cloak": {"crest": "slime_face", "panel": "waves", "border": 2, "trim2": 4, "surface": "slime_face", "seed": 105},
    "silent_winter_mantle": {"crest": "snowflake", "panel": "snowflake", "border": 5, "trim2": 2, "surface": "snowflake", "seed": 106},
    "six_faced_dragons_scale": {"crest": "dragon_face", "panel": "dragon_crest", "border": 1, "trim2": 5, "surface": "dragon_face", "seed": 107},
    "urek_mazinos_chain_mantle": {"crest": "tower_chain", "panel": "pact_chain", "border": 3, "trim2": 0, "surface": "tower_chain", "seed": 108},
    "excaliburs_scabbard_avalon": {"crest": "celtic_knot", "panel": "celtic_knot", "border": 2, "trim2": 0, "surface": "celtic_knot", "seed": 109},
    "silvered_sash_of_reflections": {"crest": "mirror_diamond", "panel": "hex_glass", "border": 5, "trim2": 3, "surface": "mirror_diamond", "seed": 110},
    "berserker_armor_shard": {"crest": "claw_x", "panel": "blood_drops", "border": 1, "trim2": 5, "surface": "claw_x", "seed": 111},
    "orsteds_dragon_god_armor": {"crest": "dragon_crest", "panel": "dragon_face", "border": 0, "trim2": 1, "surface": "dragon_crest", "seed": 112},
    "greaves_of_the_wind_walker": {"crest": "wind_spiral", "panel": "wing", "border": 4, "trim2": 5, "surface": "wind_spiral", "seed": 113},
    "white_whistle": {"crest": "waves", "panel": "waves", "border": 4, "trim2": 2, "surface": "waves", "seed": 114},
    "hand_of_vecna": {"crest": "hand_eye", "panel": "eye", "border": 0, "trim2": 3, "surface": "hand_eye", "seed": 115},
    "aerie_courier_gloves": {"crest": "wing", "panel": "wind_spiral", "border": 4, "trim2": 5, "surface": "wing", "seed": 116},
    "bloodbound_gauntlets": {"crest": "blood_drops", "panel": "claw_x", "border": 1, "trim2": 3, "surface": "blood_drops", "seed": 117},
    "incinerator_arm": {"crest": "gear_vent", "panel": "gear_vent", "border": 0, "trim2": 3, "surface": "gear_vent", "seed": 118},
    "eye_of_vecna": {"crest": "eye", "panel": "hand_eye", "border": 0, "trim2": 5, "surface": "eye", "seed": 119},
    "basilica_halo": {"crest": "cross_rays", "panel": "cross_rays", "border": 5, "trim2": 2, "surface": "cross_rays", "seed": 120},
    "godking_crown": {"crest": "crown_glyph", "panel": "star_burst", "border": 0, "trim2": 5, "surface": "crown_glyph", "seed": 121},
    "limitless_blindfold": {"crest": "six_eyes", "panel": "six_eyes", "border": 4, "trim2": 5, "surface": "six_eyes", "seed": 122},
    "odins_all_seeing_eye": {"crest": "raven", "panel": "eye", "border": 0, "trim2": 4, "surface": "raven", "seed": 123},
    "ras_dawn_crown": {"crest": "sun_ankh", "panel": "cross_rays", "border": 2, "trim2": 5, "surface": "sun_ankh", "seed": 124},
}
