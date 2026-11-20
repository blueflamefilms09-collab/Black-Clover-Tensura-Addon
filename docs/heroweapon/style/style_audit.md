# Style audit: why the hero weapons and bows read as procedurally generated

Owner feedback (their words): "I feel like you could improve the style of some of the weapons and bows to look better and not so AI looking."

Read-only study. Nothing in `src/`, `tools/blender/`, assets, versions or the changelog was touched. New files only:
`docs/heroweapon/style/` (this file, `style_guide.md`, the contact sheets, `sameness_report_output.md`) and `tools/heroweapon/style/sameness_report.py`.
Companion: `style_guide.md` (the rules for the rework and the ranked list of 20 in three waves).

## 0. What was looked at

Images viewed with the image reader (all software-rendered stills from `tools/relics/preview_mesh.py` / the Blender preview step, not a game):
`docs/previews/blender_weapons/legendary_shield_spear_bow.png`, `seven_star_1.png`, `legendary_sword.png`, `vassal_katana_scythe_fan_harpoon_chain.png`;
`docs/previews/legendary_weapons/before_after.png`; `docs/heroweapon/form_previews/legendary_bow_{sagittarius,meteor_bow,hunters_bow,arrow_shower,bind_arrow,bird_hunting,eagle_piercer,frozen_rain,scouts_bow}.png`,
all `legendary_spear_*`, `legendary_sword_*`, `legendary_shield_*` form previews (as contact sheets); `docs/previews/vassal_forms_v2/after_axe.png`;
`docs/previews/flat_parts_v2/after_legendary_bow__sagittarius_pull3.png`; `docs/berserk_weapons_previews/sheet_new_weapons_and_fixed_hands.png`.
Not viewed one by one: the rest of `vassal_forms_v2`, `flat_parts_v2` (the same bows again), `docs/previews/ingame/` (the CI client shots).

Builders read: `tools/blender/{bl_lib,legendary,seven_star,vassal_a,vassal_b,lord_weapons,forms_kit,forms_legendary_bow}.py` and grep passes over every `tools/blender/*.py`.
Lore read: `docs/heroweapon/weapon_{bow,spear,sword,shield,book,katana}.md`, `forms.md`.

Numbers below come from `tools/heroweapon/style/sameness_report.py` (section 5), which reads the shipped meshes and skins of the 154 Blender-made weapons
(`weapon_meshes/<id>.txt` + `textures/entity/weapon/<id>.png`; draw-stage `_pull1..3` meshes skipped). It measures files, not a game.

## 1. Verdict in four lines

1. The weapons are individually competent, but they are all cut from one recipe: **gold + navy/steel + one glowing round gem, gold collars, spikes, wing blades**.
   59 of 154 Blender-made weapons are gold-dominant; 77 percent carry band/ring parts, 56 percent a gem part, 34 percent studs or spikes.
2. The recipe is applied **by helper function with fixed parameters**, so repeated things are literally identical (nine bows call `jewel()` and `hand_binding()` with the same numbers).
3. The shapes have **no hierarchy**: a thin shaft or limb carrying a lot of equally sized small parts. Nothing is big, nothing is a deliberate one-off.
4. The surface is **clean CG metal with a uniform bevel and no painted history**: the base weapons have zero edge wear, dirt or scratches (`wear=` appears 0 times in
   `legendary.py` and `seven_star.py`), while glow is spread over everything.

There is already proof in the repo that the fix works: `docs/previews/vassal_forms_v2/after_axe.png` (timber, shine and curse axe) built with `forms_kit.surface()` (mottle, wear) and
a single dominant shape each, look hand-made; the Berserk set (`docs/berserk_weapons_previews/…`) is dark, single-accent, and reads as designed. The worst offenders are the **older base
hero weapons** (`legendary.py`, `seven_star.py`) and the **bow forms**.

## 2. Findings, with the evidence

### F1. One palette on everything (navy + gold + a glowing gem)
- `legendary_shield_spear_bow.png`: shield = gold rim, navy rune ring, steel, big green gem; spear = steel, gold collars, red gem; bow = steel and gold, yellow gem. Three weapons, one gold.
- `seven_star_1.png`: hammer, axe, staff and claw are all navy blocks with gold trim and a round gem (amber, teal, ice-blue, amber). `seven_star_same_recipe.png` (this folder) puts all seven side by side:
  hammer, axe, staff, claw, gauntlet, whip and projectile each have a gold-ringed round gem; only the gem colour changes.
- Code: the same gold literal `rgb(240, 186, 82)` is defined in `legendary.py`, `seven_star.py`, `vassal_a.py` and twice in `vassal_b.py`; the navy `rgb(40, 50, 96)` / `rgb(46, 62, 110)`
  in `legendary.py` and `seven_star.py`. `seven_star.py` uses `M["gold"]` 41 times; "gold" is mentioned 61 times in `legendary.py`.
- Measured: gold-dominant 59 of 154; gold+navy recipe flagged on 13 (shield, sword, claw, staff, katana, scythe, harpoon, bows ...). Bows average gold 0.15, swords 0.16.

### F2. Every bow carries the same yellow jewel at the same place
- `bows_same_recipe.png` (this folder, built from the nine form previews): in all nine forms and the base bow a bright yellow disc sits at the same riser spot. In `legendary_bow_{hunters,scouts,
  bird_hunting,arrow_shower,frozen_rain,meteor,eagle_piercer}.png` the disc is the brightest thing and the same size.
- Code: `forms_legendary_bow.py` calls `jewel(...)` 9 times (`y=7.6` in six, `6.8` in two, `7.4` in one; `r` 3.4 to 3.9; `claws` 5 to 7) and every palette defines
  `P["gem"] = S("…gem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8 …)`; seven of nine use exactly that colour and emission.
- `hand_binding(...)` is called 9 times; `bands=(-9.3, -4.5, 1.6)` is identical in 6 forms (`hunters_bow` has `-9.2, -4.0, 1.8` style variants of the same idea).
- Lore note (rule 10): the bow's jewel is canon (`weapon_bow.md` section 1, "THE GEM"). The problem is not that it exists, it is that a tier-1 Hunter's Bow and the tier-5 Sagittarius carry the same
  polished 3.8 cm cabochon. Canon justifies it on the **base** bow; the forms should change its size, mount and brightness.

### F3. Evenly spaced gold bands and rings
- `legendary_bow_sagittarius.png`: seven gold collars per limb at `t = 0.1, 0.24, 0.38, 0.52, 0.66, 0.8, 0.92` (a constant 0.14 step), alternating gold and gold2; plus three 4-point stars per limb on both faces.
  21 percent of its quads are band/ring parts, 12 identical-island rows.
- `hunters_bow`, `eagle_piercer`, `arrow_shower`: rows of hoops/lashes with a gap CV of 0.15, 0.22, 0.14 (very even). `meteor_bow` and `eagle_piercer` both use `band("limb_hoop…", … P["gold"], "limb_hoop", width=0.014)`.
- `seven_star_1.png`: hammer head has two gold bands at each end, the haft has four gold fittings; the staff shaft carries three gold rings; `seven_star.py` calls its own `band()` 5 times
  with `lip=0.6`/`0.4` for hammer, axe, staff and whip.
- Whole set: 29 weapons have a row of identical islands with gap CV below 0.20; 145 of 154 have a row of three or more identical islands.

### F4. Every weapon has a gem, and there are spike studs everywhere
- Gem part present in 86 of 154. `gem(...)`/`gem_setting(...)` has 152 call sites in `tools/blender` (forms_vassal_projectile 15, forms_vassal_katana 10, forms_legendary_sword 9, seven_star 8 ...).
- Studs/spikes/thorns/claws/horns in 52 of 154. `legendary.py`: the sword guard is ten mirrored thorns and the pommel a ring of spikes; the bow gets a `crest` of spikes; `seven_star.py`: hammer finial, claw/gauntlet
  `knuckle_studs`, whip `tip_spikes`. `legendary_sword.png` (3.80.0): the guard is a gold starburst of near-identical talons, the guard is two tiers of curved talons.
- `rivets(...)`: 86 call sites; `forms_vassal_axe` alone 11.

### F5. Wing blades and tufts as the default ornament
- `legendary_bow.png` (3.80.0): a fan of gold-and-steel blades per limb (`limb_blades` in `legendary.py:478`, one swept steel section scaled along the limb). `forms_legendary_bow.py` has 11 `tuft(...)`/feather-fan calls; seven of the nine forms have a feather or leaf
  bunch at the riser or limbs (hunters, scouts, bird_hunting, eagle_piercer, meteor, sagittarius; arrow_shower has its quiver). `legendary_shield__light_metal_shield` is a shield plus a ring of near-identical leaf blades (shield sheet).
- `wing/feather` helper calls: 62 across the builders (forms_legendary_bow 11, forms_vassal_carriage 11 plus its v1 copy 11).

### F6. No silhouette hierarchy
- Bows: thin curved stave with 20 to 60 equal small parts (sagittarius 16 parts, 4666 quad lines including the glow layer, 12 repeated rows; eagle piercer 16 parts, 11 rows). Nothing reads as the one big shape. The exceptions
  that already read better (`arrow_shower`'s quiver fan, `bind_arrow`'s web, `frozen_rain`'s ice) are the ones with one large, odd element.
- Seven Star: all seven are "vertical stack of identical-width segments" (hammer, axe, staff, whip), a stack of rings on a stick.
- Silhouette overlap inside families (profile mask IoU, section 5): shield 0.76 mean best match, book 0.64, mirror 0.62, spear 0.61, sword 0.54. Books: `vassal_book` and `pipe_book` overlap 0.83, four
  books share one outline. Shields: slime and zombie shield 0.80.

### F7. Wrapped-grip clones
- `grip_wrapped()/helix_wrap()` is called 56 times (forms_legendary_spear 12, forms_legendary_sword 9, forms_vassal_projectile 7, forms_vassal_axe 6 ...). Every shaft or handle is "core + helical cord + two collars".
  Spear and sword previews: grip = a dark segment between two gold rings. 44 of 154 weapons have a bind/wrap part.
- `hand_binding()` in the bows: 9 of 9.

### F8. Noise instead of painted material breaks
- `noisy(ob, amp, scale, seed)` is called 9 times in the bows with amplitudes 0.08 to 0.5; it is geometric `displace()` noise on the limb, which gives symmetric lumps rather than a carved or worn limb.
- Skin: the bake uses fine bump noise and the same Bevel node width on every part (`mat(... bevel=0.14..0.3)`). Measured high-frequency skin noise: bows 17, swords 21, katanas 25 to 26,
  versus 5 to 8 on the Berserk and trophy sets that look intentional. High HF with no large colour breaks is the "texture noise" look (`vassal_katana__siderite_katana` 26.0, `white_tiger_katana` 25.3).
- Wear: `forms_kit.surface()` supports `wear`, `dirt`, `scratches`, `mottle`, but the 11 base hero weapons were built before it and use `bl_lib.mat()`, which has none
  (0 uses of `wear=` in `legendary.py` and `seven_star.py`, 47 in `forms_legendary_bow.py`, 13 in `forms_vassal_axe.py`). The older weapons are therefore the cleanest, hence the most "rendered".

### F9. Glow everywhere
- Glow-layer share of surface: mean 0.27, above 0.25 on 80 of 154. Bows: gem + inlay + string + wing edges + star plates all glow in the base bow (0.23).
- `emit_strength=0.7, emit_flicker=0.4` on every bow gem; shield has twelve glowing rim gems plus the centre plus the runes; Seven Star: every weapon has a glowing gem and a glowing edge or ring.
  Reserved glow is what makes a glow read (Berserk set: glow only on one slit or one line).

### F10. Perfect symmetry and "one more of everything"
- 13 weapons are perfectly mirrored (>= 0.90 vertex mirror score): `legendary_sword` 1.00, `legendary_shield` 1.00, `vassal_staff` 1.00, `legendary_sword_curse` 1.00, `black_spear_of_vanity` 1.00 ... Combined with F3/F4 this
  gives the "stamped" feel. `asym_mirror()` exists in `forms_kit.py` (jitter 0.08, rot 3 degrees) and is the right tool, but **no builder calls it** (grep: defined once, used nowhere).
- Part counts: bows have 8 to 12 materials each (`shower_palette` 12, `bird/eagle/hunters/scouts` 11), so a bow is eleven materials on a stick.

## 3. The helpers, counted

Counts from the report (`--md` output, definitions excluded) and the mesh-level part scan.

| helper / pattern | where defined | call sites | weapons that carry it (mesh part scan) | note |
|---|---|---|---|---|
| `band()` | `forms_legendary_bow.py:106`, `forms_legendary_shield.py:101`, `seven_star.py:174`, `vassal_a.py:82`, `vassal_b.py:147` (five different functions with one name) | 48 (vassal_b 15, bow 11, shield 10, vassal_a 7, seven_star 5) | band/ring part in 118 of 154 | the single biggest source of evenly spaced rings |
| `jewel()` | `forms_legendary_bow.py:228` | 9 (all bow forms) | gem part in 86 of 154 (all 9 bows + base bow) | identical yellow 244,172,20 emissive |
| `hand_binding()` | `forms_legendary_bow.py:221` | 9 | bind/wrap part in 44 | `bands=(-9.3,-4.5,1.6)` in 6 forms |
| `star_plate()` | `forms_legendary_bow.py:1113` (+ `tools/gen_zogratis_lucius.py` unrelated) | 2 (sagittarius) | star part in 9 | 4-point gold star plates, 3 per limb x 2 faces |
| `noisy()` | `forms_legendary_bow.py:138` | 9 | all bow limbs | geometric noise |
| `gem()/gem_setting()` | `bl_lib.py:346`, `forms_kit.py:892` | 152 | 86 | |
| `spike()` | `legendary.py:84`, `seven_star.py:170` | 15 (+ `spike_row`, `spike_array`) | stud/spike part in 52 | |
| `rivets()` | `forms_kit.py:761` | 86 | | |
| `grip_wrapped()/helix_wrap()` | `forms_kit.py:859,876` | 56 | | cord + collars |
| wing/feather/`tuft()` | `forms_legendary_bow.py:247`, `forms_kit.py:980-991` | 62 | wing part in 38 | |
| `text()/runes()` | `bl_lib.py:363`, `forms_kit.py:935` | 32 | rune part in 5 | rune rings on shield, sword, hammer |

Target of the fix: `band()` (give it spacing and size variation and make "no band" the default), `jewel()` (size, mount, colour and glow per form), `hand_binding()` (per form grip), the bow band loops, and
`gem_setting()`. These four edits hit about 25 weapons at once.

## 4. What already looks right (keep as the reference)

- `vassal_axe__timber_axe`, `shine_axe`, `curse_axe` (`after_axe.png`): one dominant shape each (a leaf blade, two crescent moons, a bat wing), painted mottle and edge wear, small hardware. Built with `forms_kit.surface`.
- `legendary_shield__egg_shield`, `leaf_shield`, `orange_balloon_shield`, `chimera_viper_shield` (the shield contact sheet): silhouettes from the creature absorbed, not from the recipe.
- `legendary_sword__thunder_god_sword` (a lightning-bolt blade) and `spirit_tortoise_sword`: the blade shape is the idea.
- Berserk set (`docs/berserk_weapons_previews/`): dark body, one accent colour, glow on a single feature.
- `legendary_bow__arrow_shower` (the quiver fan), `bind_arrow` (web), `frozen_rain` (ice): one oversized idea each; their remaining flaws are the generic hoops and jewel.

## 5. The measurements (script output)

Script: `python tools/heroweapon/style/sameness_report.py` (also `--md out.md`, `--sheet out.png`, `--selftest`). Full ranked table in `sameness_report_output.md`. Run on the repo as it was when this audit was written.

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
  family silhouette sameness (mean best IoU): shield 0.76, book 0.64, mirror 0.62, spear 0.61, sword 0.54, axe 0.54, katana 0.49, staff 0.44, bow 0.42
```

Group means (same script): sameness score bows 56.6, shields 54.6, spears 50.2, swords 49.9, vassal 44.1, Berserk/seifort 45.2, trophies 30.5; skin noise bows 17.1, swords 21.2, Berserk 7.8, trophies 5.3.

Top 15 of the ranking (listing filter: `legendary_*` and `vassal_*`, curse looks excluded; a higher score is more generic):

| # | id | sameness | best silhouette match | nearest palette | pattern share | notes |
|--:|---|---:|---|---|---:|---|
| 1 | vassal_book | 75.0 | 0.83 pipe_book | 0.20 vassal_hammer | 0.52 | five books, one outline, gold + blue sun medallion |
| 2 | legendary_bow__eagle_piercer | 71.2 | 0.45 hunters_bow | 0.17 hunters_bow | 0.53 | 11 repeated rows, hoop CV 0.22 |
| 3 | legendary_shield | 70.3 | 0.80 slime_shield | 0.23 legendary_sword | 0.44 | perfectly mirrored, 12 rim gems |
| 4 | vassal_book__revised_edition | 70.1 | 0.72 vassal_book | 0.12 library_tome | 0.23 | |
| 5 | legendary_shield__light_metal_shield | 69.1 | 0.72 needle_shield | 0.18 | 0.38 | |
| 6 | legendary_bow__hunters_bow | 66.7 | 0.49 frozen_rain | 0.17 eagle_piercer | 0.47 | hoop CV 0.15 |
| 7 | legendary_bow__arrow_shower | 63.4 | 0.28 | 0.17 scouts_bow | 0.31 | 14 rows, CV 0.14 |
| 8 | vassal_katana__siderite_katana | 63.1 | 0.52 sakura_stone | 0.26 | 0.51 | skin noise 26.0 |
| 9 | legendary_spear__dragon_minute_hand | 62.1 | 0.64 spear_curse | 0.18 | 0.22 | |
| 10 | legendary_bow | 61.5 | 0.57 bow_curse | 0.19 | 0.56 | |
| 11 | legendary_bow__bird_hunting | 60.9 | 0.39 | 0.22 | 0.41 | skin noise 21.7 |
| 12 | vassal_book__pipe_book | 60.8 | 0.83 vassal_book | 0.15 | 0.09 | |
| 13 | vassal_carriage__war_carriage | 59.4 | 0.44 queens_chariot | 0.14 library_tome | 0.36 | 16 rows |
| 14 | vassal_staff__rune_staff | 59.1 | 0.45 | 0.28 | 0.46 | gold 0.45 |
| 15 | vassal_book__library_tome | 58.6 | 0.81 pipe_book | 0.12 | 0.04 | |

Most alike pairs: `vassal_book` / `pipe_book` (IoU 0.83), `shield` / `slime_shield` (0.80), `zombie_shield` / `slime_shield` (0.80), `chimera_viper_shield` / `soul_eater_shield` (0.79), `pipe_book` / `library_tome` (0.81).

### How to read the score and where it is wrong
- It is a ranking aid, not a verdict. Silhouette IoU is a profile view at a fixed canvas, so thin long weapons (spears) overlap easily and shields (round) always overlap; read it inside a family.
- The palette comes from the baked skin at the quad centroids, area weighted, quantised to 4x4x4 RGB; lit navy often reads as grey-blue, so the navy share under-reads (the Seven Star hammer reads 0.02 although it is visibly navy).
  `vassal_hammer`, `vassal_axe` and the other Seven Star weapons therefore rank low (hammer 56th of 108 listed, axe 75th, claw 97th) although the contact sheet shows they are the cleanest example of the recipe. The wave list
  in `style_guide.md` corrects for this by eye.
- Repeat-row detection treats islands of the same part and size class as one row; it also fires on feather tufts and mirrored pairs (145 of 154), so use the CV column and the evenly-spaced count (29) rather than the raw count.
- The other 460 weapons of the mod (`tools/relics/wave*`, `gen_weapon_models.py`, `djinn_weapons.py`, built by older code paths, not Blender) were not audited; the same script runs on them with `--all` but they are outside this question.
- Nothing here was checked in a running game; the stills use the project's software rasteriser.

## 6. Caveats

- Evidence for "AI looking" is by eye and from the files; the owner's taste is the final judge. The sheet `worst10_contact_sheet.png` is the quickest way to confirm the ranking.
- Source-pattern counts are grep counts of call sites; some builders (the `_v1` copies, `devil_group*`, `boss_*`) are not hero weapons and inflate `gem()`/`rivets()` totals. Mesh-level numbers (section 5) are the ones to trust for "how many weapons".
