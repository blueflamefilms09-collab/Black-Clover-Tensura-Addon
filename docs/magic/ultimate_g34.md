# Ultimate Magic, groups 3 and 4: the last 48 attributes of the proposal table

Builder `ultimate g34`. Owner: "try them all": build an Ultimate for EVERY attribute of the proposal table in `docs/magic/ultimate_audit.md` section 4 as data rows in the style of the eight core rows. This file covers rows 49 to 96 (`UltimateDefs3` = rows 49 to 72, `UltimateDefs4` = rows 73 to 96, in table order). Rows 1 to 48 belong to another builder (`UltimateDefs1`, `UltimateDefs2`, `docs/magic/ultimate_g12.md`). How the framework works (wind-up, price, cancel rules, settings, gate, cost): `docs/magic/ultimate_magic.md`; nothing of it changed.

## What is NEW and what is a REPLACEMENT (rule 5)

* **NEW: 48 Ultimates**, one per attribute, each the last family page of its grimoire (`UltimatePages.append`). No old page, id, mode, unlock bit or mastery step moves (rule 2). Nothing was replaced, removed or renamed.
* **Skipped rows: none.** Every one of the 48 attributes has a registered family book (18 are registered in `NUSkills`, the 30 generic wiki attributes Air to Wing by `WikiBooks.newAttributes()` through `NUSkills.WIKI_ATTRIBUTE_BOOKS`, all named `book_<magic>`), none of them has an Ultimate yet (the core eight are Fire, Water, Wind, Earth, Lightning, Light, Dark, Spatial), none is Time. The books stay under the 64 family pages the unlock mask holds (the self-test `magic.ultimate.rules` checks it in the game).
* **KEPT: everything else.** The old pages stay: the character pages (Excelitius Leonum, Lightning Battle Fiend, Apocalypra Astrauza) and every other page named "Ultimate Magic" are not touched.

## Canon or original (rule 10: English wiki first, Japanese as the fallback)

**All 48 are ORIGINAL (canon: 0, original: 48).** Black Clover's own named "Ultimate Magic" belongs to a handful of mages (Mereoleona's Excelitius Leonum, Luck's Lightning Battle Fiend, Gadjah's Apocalypra Astrauza; Fire and Lightning, already character pages of the mod and part of the core eight's audit), and none of the 48 attributes here has a named ultimate-level spell on the pages that could be reached. Each name is therefore a proposal of the audit in the show's naming style, "Ultimate <Attribute> Magic: <Name>", and is marked `original` in the table, in the registry (`origin`) and in the lang text.

Sources tried (WebSearch summaries only; the Fandom host itself could not be fetched from the build machine, as the audit already noted):

| Query | Source | Result |
|---|---|---|
| English wiki: "Ultimate Magic" with the attributes Chain, Key, Sealing, Thread, Mud, Soul | blackclover.fandom.com (attribute pages Light, Star, Catalyst, Wind, Compound Magic came back) | no "Ultimate Magic" page and no list of users; Chain, Key, Mud, Soul appear only in the alphabetical index of about 120 attributes without text |
| English: "Ultimate Magic" with Mereoleona, Luck, Gadjah, Kiato, Rades, Soul Magic | fandom rankings (otakusnotes, epicdope, fandomwire) | only Luck's "Ultimate Magic Eyes" (a Light/Lightning matter), Rill's Twilight of Valhalla (Painting, not in this share) and opinion rankings; nothing for the 48 |
| Japanese: ブラッククローバー 究極魔法 一覧 属性 | ciatr.jp, gamer.ne.jp (Quartet Knights), at-s.com | the game "Quartet Knights" (each hero has an ultimate; Luck's 雷霆の刹鬼, Lightning) and a ranking that calls Lucius's finisher "超極大究極魔法" (Time/Soul, a boss move the mod has already); no list by attribute |
| Japanese: 魔法 属性 一覧 魂魔法 鎖魔法 鍵魔法 (ja.wikipedia) | search result pages only | no per-attribute page reachable |

Could not be fetched at all: any blackclover.fandom.com page body, ja.wikipedia 魔法 pages, the per-attribute Ultimate Magic pages (if they exist). If the owner knows a canon name for one of these, it is one text and one `name` string to change (a replacement, rule 5).

## The 48 Ultimates

Every row follows the core rows: the page is the last family page of its book, opens only at the end of the mastery bar for a Senior 1st Class Magic Knight with 1,000,000 EP (the gate of `UltimateGate`), starts at full magicule, takes ALL of it at the release, hits up to 24 foes in three pulses (60 / 25 / 15 percent) inside the radius, applies the debuffs on the first pulse, and has a 15 minute cooldown (all in `UltimateManager`, unchanged). "Raw" is the damage of the first pulse's full share before the three pulse shares (60 / 25 / 15 percent) and the edge falloff (1.0 at the centre, 0.55 at the edge), clamped by `UltimateRules`, then handed to the grimoire's balance law. Push: positive throws the foes outward, negative pulls them in (blocks of knockback, at most 2.5 either way); lift: upward kick (0 to 1); fire and freeze in seconds and ticks. The debuff column shows the effect, the level (amplifier + 1) and the seconds on a normal foe (halved on a boss).

| # | Book | Page id | Name | Origin | Wind-up (ticks) | Radius | Raw | Push / lift / fire / freeze | Pictures: charge / release / echo | Debuffs |
|---|---|---|---|---|---|---|---|---|---|---|
| 49 | `book_key` | `ultimate_key` | Ultimate Key Magic: Gate of the Last Lock | original | 130 (6.5s) | 14 | 26 | push -0.3 | `KEY_FX2` / `KEY_FX3` / `KEY_FX2` | slowness 4 (5.0s), mining fatigue 4 (5.0s), weakness 2 (10.0s), unluck 2 (15.0s) |
| 50 | `book_chain` | `ultimate_chain` | Ultimate Chain Magic: Prison of a Thousand Links | original | 150 (7.5s) | 16 | 34 | push -0.8 | `CHAIN_FX2` / `CHAIN_FX3` / `CHAIN_FX3` | slowness 3 (7.0s), weakness 3 (13.0s), mining fatigue 2 (10.0s) |
| 51 | `book_butoh` | `ultimate_butoh` | Ultimate Butoh Magic: Dance of the Final Curtain | original | 120 (6.0s) | 15 | 36 | push +1.8, lift 0.30 | `BUTOH_FX2` / `BUTOH_FX3` / `BUTOH_FX2` | nausea 1 (8.0s), slowness 2 (4.0s), weakness 2 (8.0s) |
| 52 | `book_briar` | `ultimate_briar` | Ultimate Briar Magic: Thorn Empress Garden | original | 170 (8.5s) | 17 | 30 | none | `BRIAR_FX2` / `BRIAR_FX3` / `BRIAR_FX2` | poison 2 (10.0s), slowness 3 (7.0s), weakness 2 (8.0s) |
| 53 | `book_cherry_blossom` | `ultimate_cherryblossom` | Ultimate Cherry Blossom Magic: Ten Thousand Petals | original | 140 (7.0s) | 18 | 40 | push +0.6, lift 0.55 | `CHERRY_BLOSSOM_FX2` / `CHERRY_BLOSSOM_FX3` / `CHERRY_BLOSSOM_FX3` | blindness 1 (6.0s), weakness 2 (8.0s), nausea 1 (5.0s) |
| 54 | `book_fungus` | `ultimate_fungus` | Ultimate Fungus Magic: Spore Apocalypse | original | 180 (9.0s) | 19 | 26 | push +0.2, lift 0.10 | `FUNGUS_FX2` / `FUNGUS_FX3` / `FUNGUS_FX2` | poison 2 (12.0s), nausea 1 (10.0s), weakness 2 (12.0s) |
| 55 | `book_food` | `ultimate_food` | Ultimate Food Magic: Grand Banquet of Ruin | original | 190 (9.5s) | 16 | 24 | push -0.5 | `FOOD_FX2` / `FOOD_MAW` / `FOOD_FX3` | hunger 4 (15.0s), nausea 1 (8.0s), weakness 2 (10.0s) |
| 56 | `book_crystal` | `ultimate_crystal` | Ultimate Crystal Magic: Prism Cathedral | original | 150 (7.5s) | 15 | 42 | push +1.0, lift 0.35 | `CRYSTAL_FX2` / `CRYSTAL_FX3` / `CRYSTAL_FX3` | glowing 1 (15.0s), blindness 1 (3.0s), slowness 2 (4.0s), weakness 2 (8.0s) |
| 57 | `book_corundum` | `ultimate_corundum` | Ultimate Corundum Magic: Ruby Sun Cutter | original | 130 (6.5s) | 13 | 46 | push +0.7, lift 0.20, fire 6s | `CORUNDUM_FX2` / `CORUNDUM_FX3` / `CORUNDUM_FX3` | weakness 3 (12.0s), slowness 2 (5.0s), glowing 1 (10.0s) |
| 58 | `book_bronze` | `ultimate_bronze` | Ultimate Bronze Magic: Colossus of the Old Age | original | 190 (9.5s) | 12 | 44 | push +2.2, lift 0.25 | `BRONZE_FX2` / `BRONZE_FX3` / `BRONZE_FX3` | slowness 3 (8.0s), weakness 2 (10.0s), mining fatigue 3 (10.0s) |
| 59 | `book_copper` | `ultimate_copper` | Ultimate Copper Magic: Conduit Overload | original | 120 (6.0s) | 17 | 36 | push +0.5, lift 0.10, fire 3s | `COPPER_FX2` / `COPPER_FX3` / `COPPER_FX3` | slowness 3 (5.0s), weakness 2 (12.0s), blindness 1 (2.0s) |
| 60 | `book_iron` | `ultimate_iron` | Ultimate Iron Magic: Meteor Anvil | original | 170 (8.5s) | 11 | 48 | push +1.7 | `IRON_FX2` / `IRON_FX3` / `IRON_FX3` | slowness 3 (6.0s), weakness 2 (9.0s), mining fatigue 2 (8.0s) |
| 61 | `book_black_oil` | `ultimate_blackoil` | Ultimate Black Oil Magic: Tar Ocean Ignition | original | 170 (8.5s) | 17 | 38 | fire 14s | `BLACK_OIL_FX2` / `BLACK_OIL_FX3` / `BLACK_OIL_FX2` | slowness 4 (10.0s), weakness 2 (8.0s), blindness 1 (4.0s) |
| 62 | `book_gel` | `ultimate_gel` | Ultimate Gel Magic: Great Slime Engulf | original | 150 (7.5s) | 14 | 22 | push -0.6 | `GEL_FX2` / `GEL_FX3` / `GEL_FX2` | slowness 4 (12.0s), mining fatigue 3 (12.0s), weakness 2 (8.0s) |
| 63 | `book_glass` | `ultimate_glass` | Ultimate Glass Magic: Shattered Sky | original | 140 (7.0s) | 18 | 44 | push +0.9, lift 0.30 | `GLASS_FX2` / `GLASS_FX3` / `GLASS_FX3` | blindness 1 (5.0s), slowness 2 (3.0s), weakness 2 (8.0s) |
| 64 | `book_bubble` | `ultimate_bubble` | Ultimate Bubble Magic: Million Bubble Prison | original | 160 (8.0s) | 15 | 20 | push -0.4, lift 0.90 | `BUBBLE_FX2` / `BUBBLE_FX3` / `BUBBLE_FX2` | levitation 1 (3.0s), slowness 3 (7.0s), weakness 2 (8.0s) |
| 65 | `book_ice_wedge` | `ultimate_icewedge` | Ultimate Ice Wedge Magic: Glacier Spear Barrage | original | 150 (7.5s) | 16 | 42 | push +0.9, freeze 260 | `ICE_WEDGE_FX2` / `ICE_WEDGE_FX3` / `ICE_WEDGE_FX3` | slowness 3 (6.0s), weakness 2 (8.0s), mining fatigue 2 (6.0s) |
| 66 | `book_legion` | `ultimate_legion` | Ultimate Legion Magic: Army of the Chessboard | original | 200 (10.0s) | 20 | 36 | push +1.1 | `LEGION_FX2` / `LEGION_FX3` / `LEGION_FX3` | slowness 3 (5.0s), weakness 3 (10.0s), glowing 1 (15.0s) |
| 67 | `book_air` | `ultimate_air` | Ultimate Air Magic: Pressure Collapse | original | 130 (6.5s) | 18 | 36 | push -1.4 | `AIR_FX2` / `AIR_FX3` / `AIR_FX3` | weakness 3 (12.0s), slowness 2 (4.0s), nausea 1 (5.0s) |
| 68 | `book_hair` | `ultimate_hair` | Ultimate Hair Magic: Thousand Strand Binding | original | 150 (7.5s) | 14 | 28 | push -0.7 | `HAIR_FX2` / `HAIR_FX3` / `HAIR_FX2` | slowness 3 (9.0s), weakness 2 (8.0s), mining fatigue 2 (8.0s) |
| 69 | `book_memory` | `ultimate_memory` | Ultimate Memory Magic: Total Recall Erasure | original | 190 (9.5s) | 20 | 14 | none | `MEMORY_FX2` / `MEMORY_FX3` / `MEMORY_FX3` | mining fatigue 4 (15.0s), weakness 3 (15.0s), nausea 1 (8.0s) |
| 70 | `book_mineral` | `ultimate_mineral` | Ultimate Mineral Magic: Vein Eruption | original | 170 (8.5s) | 15 | 42 | push +0.9, lift 0.55 | `EARTHFAM_ZONE` / `EARTHFAM_BURST` / `EARTHFAM_BURST` | slowness 3 (6.0s), weakness 2 (8.0s), mining fatigue 3 (10.0s) |
| 71 | `book_modification` | `ultimate_modification` | Ultimate Modification Magic: Absolute Adjustment | original | 200 (10.0s) | 17 | 18 | none | `MODIFICATION_FX2` / `MODIFICATION_FX3` / `MODIFICATION_FX3` | slowness 4 (10.0s), weakness 4 (15.0s), mining fatigue 3 (10.0s) |
| 72 | `book_mucus` | `ultimate_mucus` | Ultimate Mucus Magic: Slime Tide | original | 160 (8.0s) | 17 | 24 | push +0.9 | `MUCUS_FX2` / `MUCUS_FX3` / `MUCUS_FX2` | slowness 4 (12.0s), weakness 2 (8.0s), nausea 1 (6.0s) |
| 73 | `book_mud` | `ultimate_mud` | Ultimate Mud Magic: Swamp of Despair | original | 180 (9.0s) | 19 | 26 | push -0.5 | `MUD_FX2` / `MUD_FX3` / `MUD_FX2` | slowness 4 (13.0s), mining fatigue 3 (13.0s), weakness 2 (7.0s) |
| 74 | `book_nail` | `ultimate_nail` | Ultimate Nail Magic: Iron Maiden Rain | original | 130 (6.5s) | 13 | 46 | push +0.2 | `NAIL_FX2` / `NAIL_FX3` / `NAIL_FX3` | wither 1 (5.0s), slowness 2 (5.0s), weakness 2 (8.0s) |
| 75 | `book_permeation` | `ultimate_permeation` | Ultimate Permeation Magic: Phase Through All | original | 140 (7.0s) | 12 | 54 | none | `PERMEATION_FX2` / `PERMEATION_FX3` / `PERMEATION_FX3` | weakness 3 (12.0s), darkness 1 (6.0s), slowness 2 (4.0s) |
| 76 | `book_poison_plant` | `ultimate_poisonplant` | Ultimate Poison Plant Magic: Venom Bloom | original | 170 (8.5s) | 16 | 32 | none | `POISON_PLANT_FX2` / `POISON_PLANT_FX3` / `POISON_PLANT_FX2` | poison 3 (13.0s), wither 2 (6.0s), slowness 2 (5.0s) |
| 77 | `book_red_ochre` | `ultimate_redochre` | Ultimate Red Ochre Magic: Crimson Earth Rite | original | 180 (9.0s) | 15 | 34 | push +0.8, lift 0.40 | `EARTHFAM_ZONE` / `EARTHFAM_BURST` / `EARTHFAM_BURST` | weakness 3 (15.0s), slowness 3 (7.0s), glowing 1 (15.0s) |
| 78 | `book_rock` | `ultimate_rock` | Ultimate Rock Magic: Mountain Fall | original | 200 (10.0s) | 10 | 52 | push +2.4 | `EARTHFAM_ZONE` / `EARTHFAM_BURST` / `EARTHFAM_BURST` | slowness 3 (5.0s), mining fatigue 3 (8.0s), weakness 2 (6.0s) |
| 79 | `book_sandstone` | `ultimate_sandstone` | Ultimate Sandstone Magic: Pharaoh's Tomb Collapse | original | 180 (9.0s) | 16 | 36 | push -0.6 | `EARTHFAM_ZONE` / `EARTHFAM_BURST` / `EARTHFAM_ZONE` | slowness 3 (8.0s), blindness 1 (5.0s), mining fatigue 3 (10.0s) |
| 80 | `book_scale` | `ultimate_scale` | Ultimate Scale Magic: Judgement Scale | original | 160 (8.0s) | 14 | 40 | push +0.6 | `SCALE_FX2` / `SCALE_FX3` / `SCALE_FX3` | weakness 3 (15.0s), slowness 2 (5.0s), glowing 1 (10.0s) |
| 81 | `book_shakudo` | `ultimate_shakudo` | Ultimate Shakudo Magic: Black Gold Flame | original | 150 (7.5s) | 14 | 44 | push +0.6, fire 10s | `SHAKUDO_FX2` / `SHAKUDO_FX3` / `SHAKUDO_FX3` | wither 2 (5.0s), weakness 2 (8.0s), blindness 1 (2.0s) |
| 82 | `book_skin` | `ultimate_skin` | Ultimate Skin Magic: Second Skin Rend | original | 140 (7.0s) | 13 | 38 | push +0.3 | `WIKI_MAGIC_FIELD` / `WIKI_MAGIC_BURST` / `WIKI_MAGIC_BURST` | weakness 3 (13.0s), wither 2 (7.0s), slowness 2 (5.0s) |
| 83 | `book_smoke` | `ultimate_smoke` | Ultimate Smoke Magic: Great Burning Veil | original | 140 (7.0s) | 19 | 28 | push +0.4, lift 0.20, fire 5s | `SMOKE_FX2` / `SMOKE_FX3` / `SMOKE_FX2` | blindness 1 (8.0s), slowness 2 (6.0s), weakness 2 (6.0s) |
| 84 | `book_snow` | `ultimate_snow` | Ultimate Snow Magic: Whiteout Blizzard | original | 170 (8.5s) | 21 | 30 | push +0.5, freeze 280 | `SNOW_FX2` / `SNOW_FX3` / `SNOW_FX2` | slowness 3 (8.0s), blindness 1 (6.0s), weakness 2 (6.0s) |
| 85 | `book_song` | `ultimate_song` | Ultimate Song Magic: Requiem of the World | original | 200 (10.0s) | 22 | 32 | none | `SONG_FX2` / `SONG_FX3` / `SONG_RING` | slowness 3 (6.0s), weakness 3 (10.0s), nausea 1 (5.0s) |
| 86 | `book_soul_corpse` | `ultimate_soulcorpse` | Ultimate Soul Corpse Magic: Legion of the Dead | original | 190 (9.5s) | 17 | 34 | push -0.7 | `SOUL_CORPSE_FX2` / `SOUL_CORPSE_FX3` / `CORPSE_RAISE` | wither 2 (9.0s), slowness 3 (7.0s), weakness 2 (8.0s), darkness 1 (5.0s) |
| 87 | `book_soul` | `ultimate_soul` | Ultimate Soul Magic: Soul Harvest | original | 180 (9.0s) | 18 | 36 | push -1.0 | `SOUL_FX2` / `SOUL_FX3` / `SOUL_VORTEX` | wither 2 (10.0s), weakness 3 (12.0s), slowness 2 (3.0s) |
| 88 | `book_sound` | `ultimate_sound` | Ultimate Sound Magic: Resonance Collapse | original | 110 (5.5s) | 21 | 38 | push +2.1, lift 0.20 | `SOUND_FX2` / `SOUND_FX3` / `SOUND_FX3` | nausea 1 (10.0s), weakness 2 (6.0s), slowness 2 (4.0s) |
| 89 | `book_spike` | `ultimate_spike` | Ultimate Spike Magic: Forest of Spears | original | 140 (7.0s) | 13 | 46 | push +0.3, lift 0.65 | `SPIKE_FX2` / `SPIKE_FX3` / `SPIKE_FX2` | slowness 3 (5.0s), weakness 2 (5.0s), mining fatigue 2 (7.0s) |
| 90 | `book_switching` | `ultimate_switching` | Ultimate Switching Magic: Total Swap | original | 160 (8.0s) | 16 | 24 | push -1.2, lift 0.45 | `WIKI_MAGIC_FIELD` / `SPACE_RINGGATE` / `SPACE_CRACK` | nausea 1 (12.0s), blindness 1 (3.0s), slowness 2 (4.0s), weakness 2 (8.0s) |
| 91 | `book_tongue` | `ultimate_tongue` | Ultimate Tongue Magic: Great Toad Lash | original | 110 (5.5s) | 15 | 40 | push -2.2 | `WIKI_MAGIC_FIELD` / `WIKI_MAGIC_BURST` / `WIKI_MAGIC_CAST` | slowness 2 (4.0s), weakness 2 (10.0s), hunger 2 (8.0s) |
| 92 | `book_tree` | `ultimate_tree` | Ultimate Tree Magic: Ancient Grove Wrath | original | 200 (10.0s) | 19 | 40 | push +1.0, lift 0.60 | `TREE_FX2` / `TREE_FX3` / `TREE_FX2` | slowness 3 (8.0s), weakness 2 (9.0s), mining fatigue 2 (8.0s) |
| 93 | `book_stone` | `ultimate_stone` | Ultimate Stone Magic: Monolith Rain | original | 170 (8.5s) | 14 | 48 | push +1.5 | `EARTHFAM_ZONE` / `BOSS_STONE_COLUMN` / `EARTHFAM_BURST` | slowness 3 (6.0s), mining fatigue 3 (6.0s), weakness 2 (5.0s) |
| 94 | `book_vine` | `ultimate_vine` | Ultimate Vine Magic: Jungle Strangle | original | 170 (8.5s) | 16 | 24 | push -0.8 | `VINE_FX2` / `VINE_FX3` / `VINE_FX2` | slowness 4 (13.0s), poison 2 (8.0s), weakness 2 (7.0s) |
| 95 | `book_vortex` | `ultimate_vortex` | Ultimate Vortex Magic: Maelstrom Abyss | original | 150 (7.5s) | 20 | 34 | push -2.4, lift 0.40 | `VORTEX_FX2` / `VORTEX_FX3` / `VORTEX_FX2` | nausea 1 (10.0s), slowness 2 (3.0s), weakness 2 (6.0s) |
| 96 | `book_wing` | `ultimate_wing` | Ultimate Wing Magic: Skyfall Feathers | original | 130 (6.5s) | 18 | 38 | push +0.9, lift 0.85 | `WING_FX2` / `WING_FX3` / `WING_FX3` | levitation 1 (2.5s), blindness 1 (3.0s), weakness 2 (8.0s) |

(The first column is the number of the row in the proposal table. The table above is parsed by `UltimateG34Test`; the first four columns after the number have the same shape as `docs/magic/ultimate_magic.md`.)

### Skipped rows

| Row | MagicType | Reason |
|---|---|---|
| none | | every attribute of rows 49 to 96 has a registered family book, no Ultimate yet, and is not Time |

## How each one feels different

The rows share one engine, so the difference is data: the **pulse picture** (every attribute's own `<NAME>_FX2` ground picture while it winds up, its own `_FX3` impact at the release and a lingering pulse; the earth family uses the shared `EARTHFAM_*` layer whose look is chosen by the book's colour, Switching and the three generic attributes read their style from `WIKI_MAGIC_*` the same way, Song ends in `SONG_RING`, Soul in `SOUL_VORTEX`, Soul Corpse in `CORPSE_RAISE`, Food in `FOOD_MAW`, Stone in `BOSS_STONE_COLUMN`, Switching in `SPACE_RINGGATE` and `SPACE_CRACK`), the **movement** (pull-ins: Chain, Gel, Hair, Air, Vortex, Tongue, Soul, Food, Soul Corpse, Vine, Mud, Sandstone, Key, Bubble, Switching; throws: Bronze, Rock, Sound, Butoh, Iron, Stone, Tree, Legion; lifts: Bubble, Wing, Spike, Tree, Cherry Blossom, Mineral, Switching, Red Ochre, Vortex), the **debuff mix** (hunger for Food and Tongue, bad luck for Key, wither for Nail, Shakudo, Soul, Soul Corpse, Skin and Poison Plant, darkness for Permeation and Soul Corpse, poison for Briar, Fungus, Poison Plant and Vine, glowing marks for Crystal, Corundum, Red Ochre, Legion, Scale), fire (Black Oil 14 s, Shakudo 10 s, Corundum 6 s, Smoke 5 s, Copper 3 s), freezing (Snow, Ice Wedge), and the **size**: radius 10 (Rock) to 22 (Song) blocks, wind-up 110 ticks (Sound, Tongue; 5.5 s) to 200 ticks (Modification, Legion, Song, Tree, Rock; 10 s), raw damage 14 (Memory, which is a control Ultimate) to 54 (Permeation).

Design notes of single rows (the proposal's "idea" column is honoured except where said):

* **Key** locks (slowness IV and mining fatigue IV for 5 s, weakness, bad luck): the shortest hard hold, low damage.
* **Memory, Modification, Gel, Mucus, Mud, Vine** are control Ultimates (damage 14 to 26) built on heavy slowness, fatigue and weakness.
* **Permeation** has the highest raw damage (54) and no knockback or lift ("phase strikes through armour": the damage still goes through the grimoire's balance law, so it does NOT ignore armour in this version).
* **Scale**: "damage by their weight" is not built (the engine has no per-target weight rule); it applies a flat weakness and glowing marks.
* **Switching**: "scrambles positions" is not built: moving foes' positions is a teleport and would need its own bounded rule. The first version throws the foes about, dizzies and blinds them. The lang text says so.
* **Food**: "sates allies" is not built (no heal in v1). **Soul Corpse**: the undead are pictures only. **Legion**: the board is the picture, the pieces do not fight. **Cherry Blossom, Wing**: the petals and feathers are particles of the picture.
* **Black Oil** burns 14 s for the "tar ocean ignition"; **Snow** and **Ice Wedge** freeze (280 and 260 ticks).

## Worst-case cost (rule 11)

Nothing new runs: the rows only add data to the table that `UltimateManager` already bounds. For every Ultimate, as for the core eight:

* Idle: no cost (the handlers return when no cast exists).
* One cast: at most 11 effect packets in the worst row of these 48 (limit 16, `UltimateRules.MAX_EFFECT_PACKETS`; windups of 110 to 200 ticks), each through `VfxSpawn`'s nearby-players rule and per-tick budget; at most 3 area queries, 96 candidates looked at, 24 foes hit per pulse (72 per cast); no entity, block or chunk is created or loaded; 2 budgeted particle packets every 10 ticks.
* Server totals: at most `maxSimultaneous` casts (default 2, hard cap 8) across ALL Ultimates of all groups, one per caster, 15 minute cooldown each; per-player state is the existing persisted tag and the `Map<UUID, Session>`.
* Client draw cost: the reused layers' own caps (the previews below report the worst row against the 400-vertex budget; see "Previews").
* Memory: 48 immutable records.

`UltimateG34Test` pins the bounds of every row (wind-up 100 to 400, radius 6 to 24, raw 10 to 80, 3 to 4 debuffs, push at most 2.5, lift at most 1, packets at most 16 even at the longest wind-up), that no row is a copy of another, and that nothing is a Time shape.

## Previews

`docs/previews/ultimate/g34_*.png`: software render of the real effect layers for a sample (Key, Black Oil, Cherry Blossom, Song, Soul, Stone, Vortex, Crystal), each sheet the charge picture, the release and the lingering pulse of that Ultimate, plus `g34_overview.png`. Scenes: `tools/vfx_preview/scenes.d/ultimate_g34.json`. The previewer has no world, lighting or depth. 

## Files

New: `UltimateDefs3.java`, `UltimateDefs4.java` (filled in; they were empty), `tools/lang/ultimate_g34.json` (48 page texts, same key format as `ultimate.json`), `src/test/resources/armour_policy/ultimate_g34_new_pages.json`, `docs/magic/ultimate_g34.md`, `src/test/java/.../ultimate/UltimateG34Test.java`, `tools/vfx_preview/scenes.d/ultimate_g34.json`, `docs/previews/ultimate/g34_*.png`.
Shared files touched (minimal, so that the merge with the other group builder is trivial): `merge_ultimate.py` (the glob reads `ultimate*.json`), `UltimatePagesTest` (reads every `ultimate*_new_pages.json`, every `tools/lang/ultimate*.json` and the `docs/magic/ultimate_g*.md` rows), `UltimateRegistryTest` (the flagship books are a subset, the shape check reads the enum body so several constants on one line count, the wiki attributes count as registered books), `tools/vfx_preview/stubs/net/minecraft/util/Mth.java` (`DEG_TO_RAD`, which `LegionLayer` needs). Not touched: `UltimateRegistry`, `UltimateDefs1/2`, `ultimate.json`, `en_us.json` (the coordinator runs `python3 -B tools/lang/merge_ultimate.py`), version, changelog, README, CI.

## Tests

`UltimateG34Test` (new, pure), the extended `UltimateRegistryTest` / `UltimatePagesTest` / `UltimateRulesTest` / `UltimateGateTest` / `UltimateSourceScanTest`, and the existing sweep (`ArmourPolicyTest`, `PageCountsTest`, Pages15 tests, character set tests, `ServerSafetyTest`, `ConfigFileNamesTest`): results in the report of the builder. In the game the existing CI self-tests `magic.ultimate.rules` and `magic.ultimate.cast` already walk every row of the registry (the last family page of its real book, handler, lang text in `en_us.json`, real shapes and potion effects, closed for a fresh player in six ways, worst-case packets), so the 48 rows are covered without a new key prefix; the lang check needs the fragment merged into `en_us.json` first.

## Not verified

Nothing was played. No cast in a real client or on a server, no look at how the 48 pages show in the grimoire UI, no multiplayer run, no look at the effects in the world (only the software previews, and none for Skin, Tongue and Switching's `WIKI_MAGIC_*` charge pictures, which the previewer cannot build), no balance test of the numbers against the real mastery and EP curve, and no check that each of the 48 books stays under its page limit with the page appended other than by the existing in-game self-test.
