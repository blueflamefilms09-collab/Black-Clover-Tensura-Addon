# Sameness report

```
Weapons measured: 154 (Blender-made, pull stages skipped)
  gold-dominant (gold share > 0.12): 59   navy+gold recipe: 13   mean gold/navy/gem: 0.12 / 0.08 / 0.04
  part class band  present in 118 weapons (77%), mean quad share 0.085
  part class gem   present in  86 weapons (56%), mean quad share 0.068
  part class stud  present in  52 weapons (34%), mean quad share 0.025
  part class grip  present in  44 weapons (29%), mean quad share 0.016
  part class star  present in   9 weapons (6%), mean quad share 0.007
  part class wing  present in  38 weapons (25%), mean quad share 0.037
  part class rune  present in   5 weapons (3%), mean quad share 0.002
  weapons with an evenly spaced identical-island row (gap CV < 0.20): 29
  weapons with a row of >=3 identical islands: 145
  perfectly mirrored (mirror >= 0.90): 13
  glow share > 0.25 of the surface: 80   mean glow 0.27
  family silhouette sameness (mean best IoU / mean palette NN distance):
    legendary_shield       n=11  IoU 0.76  pal-nn 0.23  gold 0.12
    vassal_book            n= 5  IoU 0.64  pal-nn 0.15  gold 0.10
    vassal_mirror          n= 7  IoU 0.62  pal-nn 0.23  gold 0.07
    legendary_spear        n=11  IoU 0.61  pal-nn 0.28  gold 0.14
    legendary_sword        n=11  IoU 0.54  pal-nn 0.27  gold 0.16
    vassal_axe             n= 5  IoU 0.54  pal-nn 0.24  gold 0.08
    vassal_katana          n= 5  IoU 0.49  pal-nn 0.26  gold 0.07
    vassal_staff           n= 5  IoU 0.44  pal-nn 0.31  gold 0.24
    legendary_bow          n=11  IoU 0.42  pal-nn 0.21  gold 0.15
    vassal_instrument      n= 5  IoU 0.33  pal-nn 0.28  gold 0.12
    vassal_carriage        n= 5  IoU 0.33  pal-nn 0.27  gold 0.20
    vassal_chain           n= 7  IoU 0.31  pal-nn 0.25  gold 0.08
    vassal_projectile      n= 5  IoU 0.30  pal-nn 0.34  gold 0.06
    vassal_scythe          n= 5  IoU 0.25  pal-nn 0.32  gold 0.06
    vassal_sewing_set      n= 4  IoU 0.25  pal-nn 0.30  gold 0.15
    vassal_whip            n= 5  IoU 0.25  pal-nn 0.31  gold 0.22
```

## Ranked (most generic first; listing filter --only ^(legendary|vassal)_ --exclude _curse$)

| # | id | sameness | sil IoU (nearest) | pal dist (nearest) | gold/navy/gem | pattern | repeat (CV) | mirror | glow | hf | parts |
|---|----|---------:|------------------|------------------|--------------|--------:|-------------|-------:|-----:|---:|------:|
| 1 | vassal_book | 75.0 | 0.83 (vassal_book__pipe_book) | 0.20 (vassal_hammer) | 0.18/0.05/0.05 | 0.52 | 0.38 (0.01, 5 rows) | 0.48 | 0.37 | 9.0 | 14 |
| 2 | legendary_bow__eagle_piercer | 71.2 | 0.45 (legendary_bow__hunters_bow) | 0.17 (legendary_bow__hunters_bow) | 0.18/0.00/0.00 | 0.53 | 0.42 (0.22, 11 rows) | 0.39 | 0.03 | 14.9 | 16 |
| 3 | legendary_shield | 70.3 | 0.80 (legendary_shield__slime_shield) | 0.23 (legendary_sword) | 0.28/0.10/0.02 | 0.44 | 0.31 (0.41, 3 rows) | 1.00 | 0.24 | 14.6 | 11 |
| 4 | vassal_book__revised_edition | 70.1 | 0.72 (vassal_book) | 0.12 (vassal_book__library_tome) | 0.09/0.00/0.00 | 0.23 | 0.48 (0.30, 11 rows) | 0.46 | 0.31 | 13.8 | 30 |
| 5 | legendary_shield__light_metal_shield | 69.1 | 0.72 (legendary_shield__needle_shield) | 0.18 (vassal_mirror__white_tiger_clone_mirror) | 0.04/0.02/0.02 | 0.38 | 0.31 (0.48, 7 rows) | 0.59 | 0.06 | 16.0 | 13 |
| 6 | legendary_bow__hunters_bow | 66.7 | 0.49 (legendary_bow__frozen_rain) | 0.17 (legendary_bow__eagle_piercer) | 0.15/0.00/0.00 | 0.47 | 0.36 (0.15, 12 rows) | 0.10 | 0.02 | 15.9 | 18 |
| 7 | legendary_bow__arrow_shower | 63.4 | 0.28 (legendary_bow__scouts_bow) | 0.17 (legendary_bow__scouts_bow) | 0.31/0.00/0.00 | 0.31 | 0.39 (0.14, 14 rows) | 0.42 | 0.02 | 17.4 | 19 |
| 8 | vassal_katana__siderite_katana | 63.1 | 0.52 (vassal_katana__sakura_stone_katana) | 0.26 (legendary_bow__bind_arrow) | 0.00/0.09/0.00 | 0.51 | 0.49 (0.44, 14 rows) | 0.12 | 0.05 | 26.0 | 18 |
| 9 | legendary_spear__dragon_minute_hand | 62.1 | 0.64 (legendary_spear_curse) | 0.18 (vassal_carriage__hourglass_sand_carriage) | 0.15/0.00/0.00 | 0.22 | 0.38 (0.41, 10 rows) | 0.55 | 0.01 | 12.7 | 18 |
| 10 | legendary_bow | 61.5 | 0.57 (legendary_bow_curse) | 0.19 (seifort_holy_sword_radiant) | 0.30/0.00/0.03 | 0.56 | 0.00 (-, 0 rows) | 0.80 | 0.23 | 14.7 | 10 |
| 11 | legendary_bow__bird_hunting | 60.9 | 0.39 (legendary_bow__hunters_bow) | 0.22 (legendary_sword__wind_king_katana) | 0.03/0.09/0.00 | 0.41 | 0.32 (0.29, 10 rows) | 0.25 | 0.03 | 21.7 | 16 |
| 12 | vassal_book__pipe_book | 60.8 | 0.83 (vassal_book) | 0.15 (vassal_book__animal_needle_book) | 0.06/0.11/0.00 | 0.09 | 0.54 (0.12, 6 rows) | 0.52 | 0.07 | 11.0 | 29 |
| 13 | vassal_carriage__war_carriage | 59.4 | 0.44 (vassal_carriage__queens_chariot) | 0.14 (vassal_book__library_tome) | 0.00/0.00/0.00 | 0.36 | 0.57 (0.36, 16 rows) | 0.01 | 0.05 | 9.4 | 21 |
| 14 | vassal_staff__rune_staff | 59.1 | 0.45 (vassal_staff__mirror_staff) | 0.28 (vassal_sewing_set__ariadnes_thread) | 0.45/0.00/0.01 | 0.46 | 0.43 (0.10, 7 rows) | 0.02 | 0.34 | 10.3 | 14 |
| 15 | vassal_book__library_tome | 58.6 | 0.81 (vassal_book__pipe_book) | 0.12 (vassal_book__revised_edition) | 0.04/0.01/0.00 | 0.04 | 0.44 (0.42, 16 rows) | 0.18 | 0.21 | 10.4 | 15 |
| 16 | legendary_sword__phoenix_sword | 58.2 | 0.57 (legendary_sword__spirit_tortoise_sword) | 0.22 (legendary_spear__meteor_spear) | 0.32/0.00/0.12 | 0.41 | 0.26 (0.42, 8 rows) | 0.08 | 0.17 | 15.9 | 12 |
| 17 | legendary_bow__sagittarius | 57.3 | 0.30 (legendary_bow__frozen_rain) | 0.22 (vassal_mirror__yata_mirror) | 0.32/0.05/0.00 | 0.65 | 0.39 (0.60, 12 rows) | 0.07 | 0.08 | 20.0 | 16 |
| 18 | vassal_staff__archmage_staff | 57.2 | 0.46 (vassal_staff__focus_staff) | 0.31 (vassal_chain) | 0.11/0.25/0.00 | 0.32 | 0.55 (0.42, 9 rows) | 0.02 | 0.57 | 9.2 | 15 |
| 19 | legendary_sword | 57.0 | 0.51 (legendary_sword__thunder_god_sword) | 0.23 (legendary_shield) | 0.34/0.10/0.06 | 0.42 | 0.08 (0.01, 1 rows) | 1.00 | 0.30 | 11.8 | 10 |
| 20 | vassal_mirror__yata_mirror | 56.7 | 0.58 (vassal_mirror__white_tiger_clone_mirror) | 0.22 (legendary_bow__sagittarius) | 0.24/0.00/0.00 | 0.39 | 0.37 (0.50, 9 rows) | 0.60 | 0.17 | 6.0 | 14 |
| 21 | vassal_projectile__hatchet | 56.1 | 0.28 (vassal_projectile) | 0.23 (vassal_axe__pipe_axe) | 0.00/0.00/0.00 | 0.29 | 0.34 (0.34, 13 rows) | 0.28 | 0.16 | 13.2 | 11 |
| 22 | legendary_shield__leaf_shield | 55.8 | 0.75 (legendary_shield__soul_eater_shield) | 0.18 (vassal_mirror__hetsu_kagami) | 0.06/0.00/0.00 | 0.33 | 0.25 (0.54, 11 rows) | 0.05 | 0.01 | 9.2 | 17 |
| 23 | legendary_spear | 55.3 | 0.71 (legendary_spear_curse) | 0.38 (legendary_spear__triaina) | 0.16/0.00/0.05 | 0.67 | 0.13 (0.12, 1 rows) | 0.87 | 0.06 | 7.3 | 11 |
| 24 | vassal_axe__timber_axe | 55.2 | 0.53 (vassal_axe__curse_axe) | 0.21 (legendary_shield__needle_shield) | 0.04/0.00/0.04 | 0.20 | 0.16 (0.14, 11 rows) | 0.01 | 0.33 | 13.5 | 29 |
| 25 | vassal_katana__white_tiger_katana | 55.0 | 0.51 (vassal_katana__siderite_katana) | 0.13 (vassal_mirror__white_tiger_clone_mirror) | 0.06/0.03/0.00 | 0.18 | 0.23 (0.31, 14 rows) | 0.12 | 0.27 | 25.3 | 18 |
| 26 | vassal_projectile__chakram | 54.8 | 0.30 (vassal_projectile__over_custom_arsenal) | 0.25 (seifort_holy_sword_radiant) | 0.18/0.00/0.04 | 0.61 | 0.35 (0.70, 18 rows) | 0.66 | 0.05 | 14.4 | 18 |
| 27 | legendary_sword__meteor_sword | 54.7 | 0.54 (legendary_sword__wind_king_katana) | 0.24 (vassal_whip__morning_star_whip) | 0.11/0.00/0.02 | 0.31 | 0.24 (0.37, 11 rows) | 0.05 | 0.15 | 20.8 | 15 |
| 28 | legendary_shield__needle_shield | 54.6 | 0.73 (legendary_shield__leaf_shield) | 0.21 (vassal_axe__timber_axe) | 0.13/0.00/0.00 | 0.29 | 0.36 (0.67, 15 rows) | 0.10 | 0.01 | 13.1 | 43 |
| 29 | legendary_bow__frozen_rain | 54.6 | 0.49 (legendary_bow__hunters_bow) | 0.26 (legendary_bow__bird_hunting) | 0.02/0.14/0.06 | 0.25 | 0.40 (0.44, 13 rows) | 0.26 | 0.22 | 18.2 | 16 |
| 30 | vassal_gauntlet | 54.2 | 0.00 (-) | 0.23 (vassal_mirror__yata_mirror) | 0.12/0.12/0.00 | 0.62 | 0.48 (0.13, 7 rows) | 0.01 | 0.05 | 10.9 | 12 |

## Most alike pairs inside a family (silhouette IoU - palette distance)

- vassal_book__pipe_book <-> vassal_book__library_tome : sil IoU 0.81, palette dist 0.20
- vassal_book <-> vassal_book__pipe_book : sil IoU 0.83, palette dist 0.26
- vassal_book__revised_edition <-> vassal_book__library_tome : sil IoU 0.65, palette dist 0.12
- vassal_book__revised_edition <-> vassal_book__pipe_book : sil IoU 0.67, palette dist 0.18
- legendary_shield__leaf_shield <-> legendary_shield__chimera_viper_shield : sil IoU 0.72, palette dist 0.25
- vassal_book <-> vassal_book__library_tome : sil IoU 0.76, palette dist 0.30
- vassal_book <-> vassal_book__revised_edition : sil IoU 0.72, palette dist 0.27
- legendary_shield__leaf_shield <-> legendary_shield__needle_shield : sil IoU 0.73, palette dist 0.32
- legendary_shield__chimera_viper_shield <-> legendary_shield__soul_eater_shield : sil IoU 0.79, palette dist 0.39
- legendary_shield__zombie_shield <-> legendary_shield__slime_shield : sil IoU 0.80, palette dist 0.40
- vassal_mirror__envious_glass <-> vassal_mirror__green_eyed_mirror : sil IoU 0.58, palette dist 0.25
- legendary_shield__chimera_viper_shield <-> legendary_shield__slime_shield : sil IoU 0.73, palette dist 0.39

## Helper call counts in tools/blender/*.py (definitions excluded)

- `spike(` total 15 : beastwill_tails.py 5, legendary.py 5, seven_star.py 4, black_sword_greed.py 1
- `gem(/gem_setting(` total 152 : forms_vassal_projectile.py 15, devil_group2.py 12, forms_vassal_katana.py 10, devil_group3.py 9, forms_legendary_sword.py 9, seven_star.py 8, forms_vassal_instrument.py 7, forms_legendary_shield.py 6, forms_vassal_mirror.py 6, vassal_a.py 6, boss_kakashi.py 5, forms_vassal_book.py 5, forms_vassal_carriage.py 5, forms_vassal_carriage_v1.py 5, legendary.py 5, boss_lucifero_v2.py 4, forms_vassal_axe_v1.py 4, forms_vassal_book_v1.py 4, devil_group1.py 3, forms_vassal_axe.py 3, forms_vassal_chain.py 3, boss_god_of_order.py 2, boss_lucifero.py 2, devil_group4.py 2, forms_legendary_bow.py 2, belial_bone.py 1, boss_goliath.py 1, boss_minotaur.py 1, forms_exemplars.py 1, forms_kit.py 1, forms_legendary_spear.py 1, forms_redo.py 1, forms_vassal_lib.py 1, hellblaze_katana.py 1, vassal_b.py 1
- `rivets(` total 86 : forms_vassal_axe.py 11, forms_legendary_sword.py 7, forms_vassal_axe_v1.py 7, forms_vassal_carriage.py 7, forms_exemplars.py 6, forms_legendary_shield.py 6, forms_vassal_instrument.py 5, belial_bone.py 4, forms_redo.py 4, forms_vassal_carriage_v1.py 4, forms_vassal_projectile.py 4, forms_legendary_bow.py 3, forms_vassal_book.py 3, forms_vassal_chain.py 3, forms_vassal_katana.py 3, forms_vassal_mirror.py 3, forms_vassal_scythe.py 3, forms_vassal_book_v1.py 2, forms_legendary_spear.py 1
- `wing/feather fn(` total 62 : forms_legendary_bow.py 11, forms_vassal_carriage.py 11, forms_vassal_carriage_v1.py 11, devil_group3.py 5, forms_exemplars.py 5, forms_legendary_spear.py 4, forms_kit.py 2, forms_legendary_shield.py 2, forms_vassal_book.py 2, forms_vassal_book_v1.py 2, lord_weapons.py 2, seven_star.py 2, belial_bone.py 1, boss_lucifero_v2.py 1, forms_redo.py 1
- `text(/runes(` total 32 : black_sword_greed.py 7, vassal_b.py 6, legendary.py 3, forms_vassal_staff.py 2, seven_star.py 2, vassal_a.py 2, belial_bone_preview.py 1, belial_scythe.py 1, forms_exemplars.py 1, forms_kit.py 1, forms_legendary_shield.py 1, forms_v_common.py 1, forms_vassal_book.py 1, forms_vassal_common_b.py 1, forms_vassal_run.py 1, lord_weapons.py 1
- `grip_wrapped(/helix_wrap(` total 56 : forms_legendary_spear.py 12, forms_legendary_sword.py 9, forms_vassal_projectile.py 7, forms_vassal_axe.py 6, forms_vassal_axe_v1.py 5, forms_vassal_scythe.py 4, belial_scythe.py 2, forms_exemplars.py 2, forms_kit.py 1, forms_legendary_bow.py 1, forms_redo.py 1, forms_vassal_chain.py 1, forms_vassal_katana.py 1, forms_vassal_lib.py 1, forms_vassal_mirror.py 1, forms_vassal_staff.py 1, forms_vassal_whip.py 1
- `band(` total 48 : vassal_b.py 15, forms_legendary_bow.py 11, forms_legendary_shield.py 10, vassal_a.py 7, seven_star.py 5
- `jewel(` total 9 : forms_legendary_bow.py 9
- `hand_binding(` total 9 : forms_legendary_bow.py 9
- `star_plate(` total 2 : forms_legendary_bow.py 2
- `noisy(` total 9 : forms_legendary_bow.py 9
