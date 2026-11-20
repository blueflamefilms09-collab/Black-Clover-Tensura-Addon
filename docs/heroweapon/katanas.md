# Katanas v2: real katana shapes

Owner request: "Katanas should actually be shaped and look like katanas and not so thick looking. That applies to any katanas in my mod."
Status: built in the worktree, not committed, not released, not seen in a game. Version, changelog and smoke list are left to the release step.

## Why they looked thick (root cause)

Most katanas were not thick in their own mesh. The old script meshes were thin ribbons (blade width about 1/19 to 1/47 of the mesh length). The 1.110.0 size rule
(`WeaponSizeRule`, `weapon_sizes.json`, `tools/relics/weapon_sizes.py`) then replaced every katana-class mesh with a thickened copy in `weapon_meshes_sz`,
`weapon_meshes_sz/up` and `weapon_meshes_vu_sz`, and `WeaponRenderer` prefers those copies. The copies drew the blade about 1/7 to 1/11 of its length wide
(the rule's minimum blade width is 0.115 block in hand), i.e. a broadsword. Only the Bloodbound, Demon Slasher and Miasma katanas were broad in their own mesh,
and the four vassal forms, the Wind King and Hellblaze were chunky by design.

Two fixes, both needed: (1) rebuild the blades as real katanas, (2) take them out of the thickening rule (`excluded` in `weapon_sizes.json`), so the thin blade is what is drawn.

## The target shape (what "real" was measured against)

Blade 0.70 to 0.80 of the whole length; width 1/22 to 1/18 of the blade length at the habaki, tapering to the tip; thickness at most a third of the width (flat diamond
section with a shinogi ridge); sori a few percent of the blade; kissaki with a yokote; hamon painted in the skin; small thin tsuba; habaki; tsuka about 0.25 of the length
with diamond ito and menuki; kashira. Variation between katanas comes only from tsuba shape, wrap colour, hamon pattern, blade finish, sori inside the real range and
one lore feature each. Never a fatter blade.

## The katanas and how each is built now

| id | form | builder | status |
|---|---|---|---|
| bloodbound_katana, demon_slasher_katana, miasma_infused_katana, hakurou_school_practice_katana, selection_hopefuls_bokken, katana_of_carnage, katana_of_the_crimson_shogun, shadowsteel_katana, void_origin_katana, corpse_eater, ilmare_gin_the_starlight_silver, kusanagi_the_grass_cutter, dragon_tooth_blade_red, wavebreaker | sprite katanas that were script meshes + thickened copy | new `tools/blender/katanas.py` + `katanas_specs.py` | REPLACED (rebuilt) |
| vassal_katana | the v2 vassal katana | `vassal_a.py` (`THIN_KATANA`, `--broad-katana` restores the old one) | REPLACED |
| vassal_katana__white_tiger / soul_eater / siderite / sakura_stone | vassal hero forms | `forms_vassal_katana.py` (`THIN = True`) | REPLACED, blade thinned, ornaments kept |
| legendary_sword__wind_king_katana | legendary sword | `forms_legendary_sword.py` (`THIN_WIND`, `--broad-katana`) | REPLACED, blade thinned, crown kept |
| hellblaze_the_demon_sin__katana, `_lod`, `__saya` | visual-upgrade (`up/`) | `hellblaze_katana.py` (`THIN`, `--broad`) | REPLACED |

Per-katana lore feature in the new script builds (every id keeps its place line, length class, grip line, glow tint, part-name style):
bloodbound: glowing blood groove, blood patches, red tassel. demon slasher: thorn tsuba, claw-slash marks. miasma: violet miasma patches, choji hamon. hakurou: plain notare hamon, plain school katana.
bokken: wood, round oval section (kasane 0.62 on purpose), grain lines, no hamon / habaki / menuki. carnage: blood stains, charm tassel. crimson shogun: gold sun tsuba, folded-steel grain, mon crest.
shadowsteel: glowing groove, shade patches. void origin: hovering glowing ring, suguha hamon. corpse eater: 7 spine fangs, painted staring eye. ilmare gin: starlight flecks.
kusanagi: leaf marks, jade bead tassel, aoi tsuba. dragon tooth: choji hamon, iron hook tail. wavebreaker: odachi proportions (div 22), wave hamon, foam, tassel.

Hero forms: thinned by parameter, not restructured. Blade width factors: vassal_katana 0.7 of the old (and thickness 0.68), white tiger 0.55, soul eater 0.47, siderite 0.42, sakura 0.5,
wind king 0.8, hellblaze 0.52 thickness / taper; stations, notches, rivets, stripes, ghost blade and habaki scale with it. The ornaments (tiger paws and wing blades, soul-eater ribs and skull, siderite
star guard and orbit ring, sakura petals, wind-king feather crown) keep their size on purpose: they are the lore of those forms.

## Measurements: width and thickness against length

`python3 -B tools/blender/katana_measure.py --table` (pure python, reads the mesh body layer). "old mesh" = the mesh as it was (kept in classic_vassal), "drawn until now" =
its thickened copy that the game actually drew, "now" = the rebuilt mesh, which is what is drawn now. w/L = widest blade width over the whole mesh length (the Java `WeaponSizeRule.measure`
way); t/w = thickness over width.

| katana | old mesh w/L | drawn until now w/L | now w/L | now t/w |
|---|---|---|---|---|
| bloodbound_katana | 1/19 | 1/8 | 1/28 | 0.30 |
| demon_slasher_katana | 1/12 | 1/7 | 1/25 | 0.30 |
| miasma_infused_katana | 1/13 | 1/7 | 1/25 | 0.30 |
| hakurou_school_practice_katana | 1/28 | 1/8 | 1/28 | 0.27 |
| selection_hopefuls_bokken | 1/25 | 1/8 | 1/27 | 0.62 (wooden oval) |
| katana_of_carnage | 1/40 | 1/9 | 1/33 | 0.27 |
| katana_of_the_crimson_shogun | 1/36 | 1/8 | 1/30 | 0.27 |
| shadowsteel_katana | 1/32 | 1/8 | 1/31 | 0.26 |
| void_origin_katana | 1/31 | 1/8 | 1/32 | 0.24 |
| corpse_eater | 1/16 | 1/11 | 1/25 | 0.23 |
| ilmare_gin_the_starlight_silver | 1/36 | 1/8 | 1/33 | 0.25 |
| kusanagi_the_grass_cutter | 1/31 | 1/8 | 1/32 | 0.27 |
| dragon_tooth_blade_red | 1/39 | 1/10 | 1/30 | 0.28 |
| wavebreaker | 1/47 | (no thickened copy) | 1/35 | 0.26 |
| vassal_katana | 1/20 | 1/8 | 1/30 | 0.33 |
| vassal_katana__white_tiger_katana | 1/10 | same as old | 1/12 | 0.32 |
| vassal_katana__soul_eater_katana | 1/7 | same as old | 1/9 | 0.34 |
| vassal_katana__siderite_katana | 1/11 | same as old | 1/23 | 0.40 |
| vassal_katana__sakura_stone_katana | 1/8 | same as old | 1/9 | 0.31 |
| legendary_sword__wind_king_katana | 1/8 | same as old | 1/9 | 0.39 |
| hellblaze_the_demon_sin__katana (up/) | 1/18 | same as old | 1/28 | 0.28 |

Reading it: for the 15 real-katana builds the drawn blade went from about 1/8 to about 1/30 of the length (a 3.5x thinner look), thickness a quarter of the width. `ThinKatanasTest` enforces w/L between 1/60 and 1/17 and t/w <= 0.66 on them.
For the hero forms the whole-mesh number moves little (tiger 1/10 to 1/12, soul eater 1/7 to 1/9, sakura 1/8 to 1/9) because the widest point of the mesh is an ornament (wing blades, ribs, petals, crown), not the blade. Their blade itself is 0.42 to 0.55 of the old width. Not asserted by a test.
The last column the tool prints (w0 / blade length) is meaningless for the hero forms (the guard sits on the habaki), so it is left out here.

Width compromise: a true 1/18 blade is about 0.07 block wide in the hand; the 1.110.0 rule's floor was 0.115 to 0.125 block. The katanas are intentionally under that floor now (that is the point), and the 3D mesh keeps a ridge and a kissaki so they still read at first-person distance. If this proves too fine in a game, raise `div` down to 16 in `katanas_specs.py` for that id.

## What changed per katana (all REPLACED unless stated)

All 14 script katanas: new mesh, new baked skin and glow skin, same id, same place line (hilt at low y, tip bows to -x), grip line, glow tint, class and length class. Quad lines 1920 to 3309.
Hero forms: new blade meshes, ornaments as before; quad lines 4064 to 4614; skins 302 to 343 KB.
Hellblaze katana: quads 1187 (old 1190), lod 230 (223), saya 525 (496); saya narrowed (`SF 0.62`) so it fits the thin blade.

## NEW / REPLACED / KEPT

NEW (added): `tools/blender/katanas.py`, `katanas_specs.py`, `katanas_classic.py`, `katana_measure.py`, `katana_sheet.py`; `ThinKatanasTest`; `thin_katanas` and `thin_katanas_doc` in `weapon_sizes.json`; the classic copies below; previews in `docs/previews/katanas_v2/`.
REPLACED: the 21 meshes and their skins listed above (in `weapon_meshes`, `textures/entity/weapon`, Hellblaze in the `up/` folders); the thickened copies of 14 ids are no longer used (moved, not deleted); 18 ids appended to `excluded` in `weapon_sizes.json`.
KEPT: ids, lang, Better Combat files (`weapon_attributes`), engravings (`WeaponEngravings`), grip lines, glow tints, class map, Time Magic VFX untouched. Old data of every replaced katana:
- `weapon_meshes/classic_vassal/<id>.txt` and `textures/entity/weapon/classic_vassal/<id>(_glow).png` (the old mesh and skins).
- `weapon_meshes_sz/classic_vassal/<id>.txt` (the thickened copy the game drew; `WeaponSizeRule.szPath` maps the classic path to it, so the classic setting shows exactly the old look).
- `weapon_meshes_sz/up/classic_vassal` (Kusanagi up), `weapon_meshes_vu_sz/classic_vassal` (Void Origin vu), plus classic copies of `up/kusanagi` and `vu/void_origin`.
- Hellblaze old files in `weapon_meshes/up/classic_vassal` and `textures/entity/weapon/up/classic_vassal` (on disk only, not wired to the setting).
Wiring: `WeaponRenderer.isRemodelledVassalForm` now also returns true for the 20 ids in a new `THIN_KATANAS` set, so the existing `classicVassalForms` setting restores the old katanas (old mesh + old skin + thickened copy, via the classic path). Default stays the new thin look.
The builders keep a flag to build the old broad blade again (`--broad-katana`, `--broad`, `--broad-katana`); the repo meshes are generated files, the game does not run them.

## Budgets

Mesh under 5000 quad lines: all 21 inside (max 4614, tiger). Skin plus glow about 350 KB: 14 script katanas 312 to 350 KB, hero forms 302 to 343 KB (a few were re-packed with fewer colours:
corpse eater 96, siderite 64, vassal_katana 96, wind king 1536 px resized to 1024 and 48 colours). Hellblaze skins keep their Hellblaze budget (572 + 104 KB, old 633 + 130 KB). Class `katana` apparent length 1.4 to 1.8 blocks: all in class
(wavebreaker and the wind king are `longsword` class by the class map, 1.63 and 1.71). Nothing changes at runtime cost: same file formats, fewer quads than before for most, no new packets, no new per-tick work.

## Tests run (javac + JUnit launcher, no Gradle; Minecraft-dependent tests not run)

Passed: `ThinKatanasTest` (new), `WeaponSizesTest`, `ClassicVassalFilesTest`, `MeshCapTest` (with a stub of `MeshPackTest.RES`), `WeaponAttributesTest`, `FormMeshFilesTest`, `GripMathTest`, `GripTableTest`, `HoldAuditTest`, `SwingSourceScanTest`, `SizedMeshNormaliserTest` (with `WeaponMeshesTest`, `UpgradedMeshNormaliserTest`), `DemonSlayerAssetsTest`, `VuAssetsTest`, `VuCardLimitsTest`, `ShieldClassesTest`.
Changed tests: `WeaponSizesTest` (katana count 0, variants >= 16; the negative control reads the classic copy), `SizedMeshNormaliserTest` (`checked >= 15`, was 28, because 14 thickened copies and 4 variants were retired).
Sync tools run: `register_form_meshes.py` (also regenerated `grip_golden.json`), `bettercombat/inventory.py` (unchanged), `grip_table.py --write`, `hold_audit.py --write` (2 flags, both books/carriages, none on a katana).
Not run: the CI server smoke test, `ServerSafetyTest`, `ConfigFileNamesTest` and the rest of the full sweep (need Gradle / Minecraft on the classpath).

## Flags and caveats

- Nothing was seen in a game. Previews are numpy rasteriser stills (`tools/blender/katana_sheet.py`), not Cycles renders. The models are scripted bpy builds run headless (module bpy, Cycles bake); no Blender window and no Blockbench were used (rule 14, rule 15: not a Blockbench model).
- Hero forms are only thinned, not remodelled: tiger, soul eater, siderite, sakura and the Wind King still have large ornaments and read as fantasy blades. If the owner wants those as true katanas, that is a remodel decision.
- Kusanagi `up/` and Void Origin `vu/` bodies were kept as they are; only their thickened copies were retired. Check them in game with the visual upgrade / Genesis settings.
- Hellblaze before/after is a small change (it was already thin-ish); its builder also rewrites the two preview PNGs in `docs/hellblaze/previews`: those were restored to the old ones, the new ones are `docs/previews/katanas_v2/hellblaze_*_new_blender.png`.
- Bloodbound and shadowsteel baked at 2048 px / 24 samples, the other script katanas at 1536 px / 16 samples (time), all packed to 1024 px skins. Palette packing flattens fine grain slightly (hakurou 32 colours).
- Wavebreaker was already an id of the mod; its new class stays `longsword` per the class map, so it is not covered by the `katana` size rule either way.
- The four vassal hero forms have no thickened copy in the repo today; they were added to `excluded` so `weapon_sizes.py` cannot create one later.
- `tools/relics/weapon_sizes.py --check` still lists some vassal hero forms and four vassal projectiles as "would change": that was already the case before this change for forms without a copy.

## Previews

`docs/previews/katanas_v2/all_katanas.png`: one labelled contact sheet, BEFORE row (the old mesh, classic copy) above AFTER row, same scale, 21 katanas (the thickened copies are not in the BEFORE row; they are shown
by the measurements above). `close_katana_of_the_crimson_shogun_vassal_katana__white_tiger_katana.png`: side view and edge-on close-up of two blades, before and after.
