# Dagger and katana sizes in the hand (1.110.0)

Owner report: the dagger is tiny in first and third person, and the katanas look thin and small.

## Why (measured)
* How a weapon is drawn: `WeaponRenderer` places the mesh from its `place` line (apparent length = mesh length x the third-person display scale of the model json). `OversizeGuard` and `tools/relics/display_presets.py` only ever SHRINK long weapons in first person, `WeaponNormaliser` only scales up a mesh below 75 percent of its class minimum. Nothing looked at blade width or thickness, and short weapons keep the vanilla first-person scale 0.68.
* Every dagger and katana blade is a sliver. Median blade section (45 to 70 percent of the length): katana 0.03 to 0.10 block wide (0.5 to 1.6 px) and 0.011 to 0.05 thick; grips about 1 px. A vanilla sword blade is about 2.5 px.
* Four daggers (twin fangs, stinger, goblin fang, primordial hearth blade) carried a hand-written first-person scale of 0.383: a 0.27 block weapon. The rest sat at 0.44 blocks.
* Katana lengths were already 1.56 to 1.66 blocks in third person (class range 1.4 to 1.8, the owner's earlier size rule), longer than the 1.15 to 1.3 of a sword suggested for this task, so lengths are kept; the katana problem is width and thickness. First-person katana scale (0.51 to 0.71) is bounded by the camera rule of display_presets.py and is left alone.
* The 36 dark Djinn weapons: valefor (daggers), glasya_labolas (claws) and seere (sabre) are 2.3 to 4 blocks long and 0.1 to 2 blocks wide, not slivers; unchanged. Demon-Slayer is a longsword-class mesh, unchanged.

## What was built
`tools/relics/weapon_sizes.py` writes thickened COPIES (old files stay) into `weapon_meshes_sz/`, `weapon_meshes_sz/up/`, `weapon_meshes_vu_sz/` (with their level-of-detail meshes). The renderer prefers a copy when it exists (`WeaponSizeRule.szPath`, one hook each in `WeaponRenderer.read` and `VuRenderer.load`); deleting a copy restores the old look. Rule numbers: `assets/nusmp/weapon_sizes.json`.
* Cross section scaled about each slice's own centre line (a curved katana stays curved) by a smooth profile handle, guard, blade with a short collar. Blade at least 1/8 block (0.125) wide in first person and 0.11 in third person, spine at least 0.05 (dagger) / 0.06 (katana) apparent, handle at least 0.10 / 0.11, guard at least 1.25 x the blade base. Quads crossing a knot are cut first. Body, void, glow and vein layers only; halos, flares and orbiting groups are untouched.
* Daggers: length scaled about the grip to 0.68 blocks third person (was 0.50 to 0.62), first-person scale 0.775 with a re-anchored translation (first-person length 0.62 blocks, was 0.27 to 0.50).
* 24 ids (the two fairy gauntlets are excluded: wide by design), 28 meshes: old, `up` (Kusanagi, twin fangs), `vu` (Void Origin Katana, primordial hearth blade).

## NEW / REPLACED / KEPT
| | |
|---|---|
| NEW | `weapon_meshes_sz*` copies, `assets/nusmp/weapon_sizes.json`, `visual/sizes/WeaponSizeRule`, self-test `visual.weapon_sizes`, tests, tools, previews |
| REPLACED (look) | the drawn mesh of every listed dagger/katana; first-person display of 12 dagger jsons (marker `nusmp_weapon_sizes`) |
| KEPT | every old mesh, skin, id, stat, engraving, Better Combat file, class table, katana lengths and first-person scales, upgrade effects |

## Measured before and after (blocks; L3/L1 length third/first person, w blade width, d thickness; b before, a after)
```
id                             class      var |   L3b   L3a |   L1b   L1a |    w1b    w1a |    w3b    w3a |   d3b   d3a
black_wing_blade               longsword  old |  1.20  1.20 |  0.76  0.76 |  0.049  0.049 |  0.077  0.077 | 0.043 0.043  (unchanged)
blackrazor                     greatsword old |  2.65  2.65 |  0.98  0.98 |  0.061  0.061 |  0.166  0.166 | 0.035 0.035  (unchanged)
blackrazor                     greatsword vu  |  2.65  2.65 |  0.98  0.98 |  0.103  0.103 |  0.279  0.279 | 0.026 0.026  (unchanged)
bloodbound_katana              katana     old |  1.56  1.56 |  0.98  0.98 |  0.054  0.125 |  0.086  0.199 | 0.045 0.060
bottle_blade                   shortsword old |  1.12  1.12 |  0.90  0.90 |  0.055  0.055 |  0.069  0.069 | 0.032 0.032  (unchanged)
brooch_pin                     dagger     old |  0.55  0.68 |  0.44  0.62 |  0.027  0.125 |  0.034  0.137 | 0.018 0.050
broodqueens_stinger            dagger     old |  0.60  0.68 |  0.27  0.62 |  0.022  0.125 |  0.050  0.137 | 0.014 0.050
buster_sword                   greatsword old |  2.60  2.60 |  0.98  0.98 |  0.104  0.104 |  0.277  0.277 | 0.027 0.027  (unchanged)
caesars_gladius                shortsword old |  1.12  1.12 |  0.90  0.90 |  0.100  0.100 |  0.125  0.125 | 0.020 0.020  (unchanged)
chronarchs_pendulum_sabre      sabre      old |  1.22  1.22 |  0.95  0.95 |  0.052  0.052 |  0.067  0.067 | 0.010 0.010  (unchanged)
chronarchs_pendulum_sabre      sabre      up  |  1.22  1.22 |  0.95  0.95 |  0.052  0.052 |  0.067  0.067 | 0.017 0.017  (unchanged)
contingency_plan               dagger     old |  0.55  0.68 |  0.44  0.62 |  0.031  0.125 |  0.039  0.137 | 0.012 0.050
corpse_eater                   katana     old |  1.56  1.56 |  1.15  1.15 |  0.089  0.125 |  0.120  0.169 | 0.018 0.064
cyclone_rapier                 rapier     old |  1.15  1.15 |  0.92  0.92 |  0.036  0.036 |  0.045  0.045 | 0.023 0.023  (unchanged)
dagger_of_betrayal             dagger     old |  0.55  0.68 |  0.44  0.62 |  0.043  0.125 |  0.054  0.137 | 0.044 0.056
deliverance                    longsword  old |  1.56  1.56 |  1.00  1.00 |  0.060  0.060 |  0.094  0.094 | 0.052 0.052  (unchanged)
delvers_longsword              longsword  old |  1.62  1.62 |  0.98  0.98 |  0.054  0.054 |  0.089  0.089 | 0.017 0.017  (unchanged)
demon_destroyer_sword          greatsword old |  2.98  2.98 |  0.99  0.99 |  0.157  0.157 |  0.472  0.472 | 0.072 0.072  (unchanged)
demon_dweller_sword            greatsword old |  2.47  2.47 |  0.99  0.99 |  0.113  0.113 |  0.282  0.282 | 0.050 0.050  (unchanged)
demon_slasher_katana           katana     old |  1.58  1.58 |  0.88  0.88 |  0.073  0.125 |  0.131  0.224 | 0.040 0.060
demon_slayer_sword             longsword  vu  |  0.99  0.99 |  0.52  0.52 |  0.056  0.056 |  0.107  0.107 | 0.105 0.105  (unchanged)
disintegration_dagger          dagger     old |  0.55  0.68 |  0.44  0.62 |  0.051  0.126 |  0.064  0.138 | 0.015 0.049
dragon_force_twin_fangs        dagger     old |  0.60  0.68 |  0.27  0.62 |  0.106  0.244 |  0.236  0.267 | 0.013 0.050
dragon_force_twin_fangs        dagger     up  |  0.60  0.68 |  0.27  0.62 |  0.112  0.256 |  0.248  0.281 | 0.014 0.052
dragon_slayer                  greatsword old |  2.65  2.65 |  0.98  0.98 |  0.075  0.075 |  0.204  0.204 | 0.039 0.039  (unchanged)
dragon_tooth_blade_red         katana     old |  1.56  1.56 |  1.20  1.20 |  0.034  0.125 |  0.044  0.162 | 0.015 0.060
drakefang_greatsword           greatsword old |  2.65  2.65 |  0.98  0.98 |  0.061  0.061 |  0.167  0.167 | 0.035 0.035  (unchanged)
dream_glass                    greatsword old |  2.52  2.52 |  1.03  1.03 |  0.063  0.063 |  0.153  0.153 | 0.024 0.024  (unchanged)
druidic_blade                  shortsword old |  1.12  1.12 |  0.90  0.90 |  0.091  0.091 |  0.114  0.114 | 0.015 0.015  (unchanged)
duality_blade                  shortsword old |  1.12  1.12 |  0.90  0.90 |  0.063  0.063 |  0.079  0.079 | 0.079 0.079  (unchanged)
elsdocia                       greatsword old |  2.64  2.64 |  0.99  0.99 |  0.132  0.132 |  0.352  0.352 | 0.076 0.076  (unchanged)
elsdocia                       greatsword vu  |  2.64  2.64 |  0.99  0.99 |  0.140  0.140 |  0.374  0.374 | 0.046 0.046  (unchanged)
elsdocia_legacy                greatsword old |  2.30  2.30 |  1.84  1.84 |  0.260  0.260 |  0.326  0.326 | 0.079 0.079  (unchanged)
encircled_blade                shortsword old |  1.12  1.12 |  0.90  0.90 |  0.085  0.085 |  0.106  0.106 | 0.029 0.029  (unchanged)
eschaton                       rapier     old |  1.15  1.15 |  0.92  0.92 |  0.034  0.034 |  0.042  0.042 | 0.021 0.021  (unchanged)
fairy_glitter_gauntlets        dagger     old |  0.60  0.60 |  0.27  0.27 |  0.238  0.238 |  0.527  0.527 | 0.153 0.153  (unchanged)
fairy_glitter_gauntlets        dagger     vu  |  0.55  0.55 |  0.25  0.25 |  0.203  0.203 |  0.450  0.450 | 0.164 0.164  (unchanged)
fairy_heart_gauntlets          dagger     old |  0.60  0.60 |  0.27  0.27 |  0.266  0.266 |  0.590  0.590 | 0.163 0.163  (unchanged)
flame_empress_blade            longsword  old |  1.25  1.25 |  0.79  0.79 |  0.102  0.102 |  0.161  0.161 | 0.048 0.048  (unchanged)
flamecutter                    longsword  old |  1.56  1.56 |  1.03  1.03 |  0.071  0.071 |  0.108  0.108 | 0.017 0.017  (unchanged)
frostbrand                     sabre      old |  1.22  1.22 |  0.93  0.93 |  0.055  0.055 |  0.073  0.073 | 0.009 0.009  (unchanged)
giant_jawblade                 greatsword old |  2.52  2.52 |  0.92  0.92 |  0.152  0.152 |  0.415  0.415 | 0.075 0.075  (unchanged)
goblin_fang_dagger             dagger     old |  0.60  0.68 |  0.27  0.62 |  0.024  0.125 |  0.052  0.138 | 0.015 0.050
heart_of_the_flame             greatsword old |  2.52  2.52 |  0.95  0.95 |  0.094  0.094 |  0.249  0.249 | 0.045 0.045  (unchanged)
hellblaze_the_demon_sin        longsword  old |  1.62  1.62 |  0.98  0.98 |  0.071  0.071 |  0.118  0.118 | 0.026 0.026  (unchanged)
hellblaze_the_demon_sin        longsword  up  |  1.62  1.62 |  0.98  0.98 |  0.085  0.085 |  0.140  0.140 | 0.026 0.026  (unchanged)
holy_avenger                   longsword  old |  1.62  1.62 |  0.98  0.98 |  0.057  0.057 |  0.094  0.094 | 0.016 0.016  (unchanged)
humbling_longsword             longsword  old |  1.56  1.56 |  1.13  1.13 |  0.061  0.061 |  0.084  0.084 | 0.015 0.015  (unchanged)
ilmare_gin_the_starlight_silver katana     old |  1.56  1.56 |  0.99  0.99 |  0.028  0.125 |  0.044  0.197 | 0.018 0.060
jade_xiphos                    shortsword old |  1.12  1.12 |  0.90  0.90 |  0.101  0.101 |  0.126  0.126 | 0.015 0.015  (unchanged)
katana_of_carnage              katana     old |  1.56  1.56 |  1.15  1.15 |  0.029  0.125 |  0.040  0.169 | 0.013 0.060
katana_of_the_crimson_shogun   katana     old |  1.56  1.56 |  0.99  0.99 |  0.028  0.125 |  0.044  0.198 | 0.014 0.060
krakeblade                     greatsword old |  2.52  2.52 |  0.91  0.91 |  0.109  0.109 |  0.300  0.300 | 0.067 0.067  (unchanged)
kusanagi_the_grass_cutter      katana     old |  1.62  1.62 |  1.03  1.03 |  0.035  0.125 |  0.054  0.196 | 0.024 0.060
kusanagi_the_grass_cutter      katana     up  |  1.62  1.62 |  1.03  1.03 |  0.036  0.125 |  0.056  0.196 | 0.024 0.060
last_word                      shortsword old |  1.12  1.12 |  0.90  0.90 |  0.041  0.041 |  0.051  0.051 | 0.029 0.029  (unchanged)
legendary_sword                longsword  old |  1.70  1.70 |  1.03  1.03 |  0.044  0.044 |  0.073  0.073 | 0.017 0.017  (unchanged)
legendary_sword                longsword  old |  1.70  1.70 |  1.03  1.03 |  0.071  0.071 |  0.117  0.117 | 0.017 0.017  (unchanged)
licht_destroyer_sword          greatsword old |  2.98  2.98 |  0.99  0.99 |  0.157  0.157 |  0.472  0.472 | 0.072 0.072  (unchanged)
licht_dweller_sword            greatsword old |  2.47  2.47 |  0.99  0.99 |  0.113  0.113 |  0.282  0.282 | 0.050 0.050  (unchanged)
liquid_sword                   longsword  old |  1.56  1.56 |  1.11  1.11 |  0.114  0.114 |  0.160  0.160 | 0.019 0.019  (unchanged)
luck_blade                     shortsword old |  1.20  1.20 |  0.96  0.96 |  0.118  0.118 |  0.148  0.148 | 0.023 0.023  (unchanged)
magebane_longsword             longsword  old |  1.56  1.56 |  0.99  0.99 |  0.092  0.092 |  0.145  0.145 | 0.063 0.063  (unchanged)
magic_tool_sword               longsword  old |  1.56  1.56 |  0.95  0.95 |  0.107  0.107 |  0.176  0.176 | 0.065 0.065  (unchanged)
marids_valor                   sabre      old |  1.12  1.12 |  0.90  0.90 |  0.072  0.072 |  0.090  0.090 | 0.022 0.022  (unchanged)
marids_valor                   sabre      old |  1.12  1.12 |  0.90  0.90 |  0.068  0.068 |  0.085  0.085 | 0.020 0.020  (unchanged)
miasma_infused_katana          katana     old |  1.58  1.58 |  0.88  0.88 |  0.068  0.125 |  0.121  0.224 | 0.036 0.060
nakagami_edge                  longsword  old |  1.30  1.30 |  0.82  0.82 |  0.068  0.068 |  0.107  0.107 | 0.040 0.040  (unchanged)
null_blade                     greatsword old |  2.52  2.52 |  1.03  1.03 |  0.074  0.074 |  0.180  0.180 | 0.042 0.042  (unchanged)
palladion                      longsword  old |  1.56  1.56 |  1.20  1.20 |  0.058  0.058 |  0.075  0.075 | 0.014 0.014  (unchanged)
permafrost_sabre               sabre      old |  1.12  1.12 |  0.90  0.90 |  0.049  0.049 |  0.061  0.061 | 0.033 0.033  (unchanged)
primal_chaos                   rapier     old |  1.10  1.10 |  0.69  0.69 |  0.012  0.012 |  0.019  0.019 | 0.019 0.019  (unchanged)
primordial_hearth_blade        dagger     old |  0.60  0.68 |  0.27  0.62 |  0.026  0.125 |  0.057  0.137 | 0.012 0.050
primordial_hearth_blade        dagger     vu  |  0.60  0.68 |  0.27  0.62 |  0.034  0.125 |  0.076  0.137 | 0.012 0.050
prism_blade                    longsword  old |  1.56  1.56 |  0.99  0.99 |  0.092  0.092 |  0.145  0.145 | 0.068 0.068  (unchanged)
quicklings_ivy                 rapier     old |  1.13  1.13 |  0.90  0.90 |  0.033  0.033 |  0.041  0.041 | 0.022 0.022  (unchanged)
rainbow_rapier                 rapier     old |  1.12  1.12 |  0.90  0.90 |  0.012  0.012 |  0.015  0.015 | 0.007 0.007  (unchanged)
reflected_kings_mirrorblade    longsword  old |  1.62  1.62 |  0.98  0.98 |  0.054  0.054 |  0.089  0.089 | 0.014 0.014  (unchanged)
reflected_kings_mirrorblade    longsword  up  |  1.62  1.62 |  0.98  0.98 |  0.054  0.054 |  0.089  0.089 | 0.020 0.020  (unchanged)
rimeheart_greatblade           greatsword old |  2.65  2.65 |  0.98  0.98 |  0.087  0.087 |  0.236  0.236 | 0.036 0.036  (unchanged)
rimeheart_greatblade           greatsword up  |  2.65  2.65 |  0.98  0.98 |  0.099  0.099 |  0.267  0.267 | 0.041 0.041  (unchanged)
rimeheart_runeblade            greatsword old |  2.64  2.64 |  1.00  1.00 |  0.162  0.162 |  0.426  0.426 | 0.112 0.112  (unchanged)
scimitar_of_the_seasons        sabre      old |  1.05  1.05 |  0.66  0.66 |  0.048  0.048 |  0.077  0.077 | 0.029 0.029  (unchanged)
severing_greatsword            greatsword old |  3.33  3.33 |  0.99  0.99 |  0.183  0.183 |  0.618  0.618 | 0.117 0.117  (unchanged)
shadow_burn                    greatsword old |  2.52  2.52 |  0.97  0.97 |  0.075  0.075 |  0.196  0.196 | 0.053 0.053  (unchanged)
shadowsteel_katana             katana     old |  1.62  1.62 |  1.03  1.03 |  0.034  0.125 |  0.053  0.196 | 0.012 0.060
sidras_swearing_sword          shortsword old |  1.12  1.12 |  0.90  0.90 |  0.102  0.102 |  0.128  0.128 | 0.018 0.018  (unchanged)
soul_shatter_blade             greatsword old |  2.52  2.52 |  1.20  1.20 |  0.052  0.052 |  0.109  0.109 | 0.020 0.020  (unchanged)
spell_forged_rapier            rapier     old |  1.55  1.55 |  0.92  0.92 |  0.037  0.037 |  0.063  0.063 | 0.031 0.031  (unchanged)
spider_sword                   sabre      old |  1.05  1.05 |  0.66  0.66 |  0.061  0.061 |  0.097  0.097 | 0.015 0.015  (unchanged)
spotlight                      shortsword old |  1.12  1.12 |  0.90  0.90 |  0.063  0.063 |  0.078  0.078 | 0.024 0.024  (unchanged)
stegosaurus_greatsword         greatsword old |  2.52  2.52 |  1.07  1.07 |  0.078  0.078 |  0.184  0.184 | 0.137 0.137  (unchanged)
sunsword                       longsword  old |  1.62  1.62 |  0.98  0.98 |  0.055  0.055 |  0.091  0.091 | 0.019 0.019  (unchanged)
sword_crystal                  shortsword old |  1.12  1.12 |  0.90  0.90 |  0.252  0.252 |  0.315  0.315 | 0.057 0.057  (unchanged)
sword_of_the_first_dawn        longsword  old |  1.62  1.62 |  0.98  0.98 |  0.057  0.057 |  0.095  0.095 | 0.018 0.018  (unchanged)
sword_of_the_first_dawn        longsword  vu  |  1.64  1.64 |  0.99  0.99 |  0.062  0.062 |  0.102  0.102 | 0.016 0.016  (unchanged)
the_dagger_of_many_daggers     dagger     old |  0.50  0.68 |  0.40  0.62 |  0.032  0.125 |  0.040  0.137 | 0.009 0.050
the_stabshroom                 dagger     old |  0.55  0.68 |  0.44  0.62 |  0.067  0.125 |  0.083  0.137 | 0.020 0.050
thundercleave                  greatsword old |  2.52  2.52 |  0.99  0.99 |  0.144  0.144 |  0.367  0.367 | 0.095 0.095  (unchanged)
tripolar_sword                 longsword  old |  1.56  1.56 |  0.91  0.91 |  0.082  0.082 |  0.141  0.141 | 0.015 0.015  (unchanged)
twin_hook_swords               shortsword old |  1.05  1.05 |  0.84  0.84 |  0.020  0.020 |  0.025  0.025 | 0.016 0.016  (unchanged)
twin_hook_swords               shortsword old |  1.05  1.05 |  0.84  0.84 |  0.020  0.020 |  0.025  0.025 | 0.016 0.016  (unchanged)
twin_swords_of_deflection      shortsword old |  1.12  1.12 |  0.90  0.90 |  0.091  0.091 |  0.114  0.114 | 0.033 0.033  (unchanged)
twin_swords_of_deflection      shortsword old |  1.12  1.12 |  0.90  0.90 |  0.091  0.091 |  0.114  0.114 | 0.033 0.033  (unchanged)
unicorn_dagger                 dagger     old |  0.55  0.68 |  0.44  0.62 |  0.024  0.125 |  0.030  0.137 | 0.030 0.050
vassal_claw                    shortsword old |  1.18  1.18 |  0.80  0.80 |  0.190  0.190 |  0.279  0.279 | 0.038 0.038  (unchanged)
vassal_katana                  katana     old |  1.66  1.66 |  0.99  0.99 |  0.027  0.125 |  0.045  0.210 | 0.011 0.060
vassal_projectile              dagger     old |  0.62  0.68 |  0.50  0.62 |  0.057  0.125 |  0.072  0.137 | 0.027 0.050
vassal_sewing_set              rapier     old |  1.30  1.30 |  1.04  1.04 |  0.114  0.114 |  0.143  0.143 | 0.071 0.071  (unchanged)
void_origin_katana             katana     old |  1.62  1.62 |  1.01  1.01 |  0.034  0.125 |  0.055  0.200 | 0.011 0.060
void_origin_katana             katana     vu  |  1.62  1.62 |  1.01  1.01 |  0.034  0.125 |  0.055  0.200 | 0.012 0.060
vorpal_sword                   longsword  old |  1.62  1.62 |  0.98  0.98 |  0.043  0.043 |  0.072  0.072 | 0.017 0.017  (unchanged)
warmages_longsword             longsword  old |  1.56  1.56 |  1.20  1.20 |  0.050  0.050 |  0.065  0.065 | 0.017 0.017  (unchanged)
wavebreaker                    longsword  old |  1.64  1.64 |  1.16  1.16 |  0.025  0.025 |  0.036  0.036 | 0.009 0.009  (unchanged)
weapon_of_resurgence           longsword  old |  1.56  1.56 |  0.99  0.99 |  0.087  0.087 |  0.138  0.138 | 0.083 0.083  (unchanged)
whirling_blades                sabre      old |  1.12  1.12 |  0.90  0.90 |  0.064  0.064 |  0.080  0.080 | 0.009 0.009  (unchanged)
whirling_blades                sabre      old |  1.12  1.12 |  0.90  0.90 |  0.064  0.064 |  0.080  0.080 | 0.009 0.009  (unchanged)
windcutter                     longsword  old |  1.56  1.56 |  0.92  0.92 |  0.058  0.058 |  0.098  0.098 | 0.051 0.051  (unchanged)
wizard_kings_vow               rapier     old |  1.22  1.22 |  0.98  0.98 |  0.020  0.020 |  0.025  0.025 | 0.008 0.008  (unchanged)
wizard_kings_vow               rapier     up  |  1.22  1.22 |  0.98  0.98 |  0.021  0.021 |  0.027  0.027 | 0.009 0.009  (unchanged)
```
Previews: `docs/previews/weapon_sizes/daggers_before_after.png`, `katanas_before_after.png` (left old, right new). Regenerate with `python3 tools/relics/weapon_sizes_sheet.py`.

## Tests
`WeaponSizesTest` (ranges per class over every real mesh file, first-person width at least 1/8 block, spine, grip fits a hand, nearest corner at least 0.16 in front of the camera, never shorter or thinner, negative control on the old Void Origin Katana), `SizedMeshNormaliserTest`; self-test `visual.weapon_sizes`.

## Not verified
Nothing was run in the game. Whether a 0.19 block katana blade looks right (it is broad because the first-person scale is 0.54) and whether the thicker grip sits well in the Better Combat pose is unseen; previews are the offline rasteriser only. The first-person tip of some katanas sits at 1.32 to 1.35 of the view limit (as before for upgraded meshes). Normals of thickened bevels are not recomputed. `display_presets.py --check` and `audit_weapon_scale.py --check` already reported unrelated ids before this change.
