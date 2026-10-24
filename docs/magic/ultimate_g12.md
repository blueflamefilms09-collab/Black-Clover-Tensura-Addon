# Ultimate Magic, group g12: the first 48 attributes of the proposal table (rows 1 to 48 of `docs/magic/ultimate_audit.md` section 4)

Owner order: "try them all". Builder g12 turned rows 1 to 48 of the proposal table into data rows, following the style of the eight core rows (`docs/magic/ultimate_magic.md`): `UltimateDefs1` (rows 1-24, Explosion to Anti-Magic) and `UltimateDefs2` (rows 25-48, Steel to Barrier), in table order. Nothing in the framework changed: no new class, no packet, no config key, no client class; each row is one `UltimateDef` record, one lang text, one new-pages entry. Rows 49 to 96 belong to the other builder (`UltimateDefs3`, `UltimateDefs4`).

How a player meets them, the gate, the wind-up, the price, the cancel rules and the settings are exactly those of the flagship eight: see `docs/magic/ultimate_magic.md`. Short form: the Ultimate is the LAST family page of the book, hidden for a fresh player (full mastery bar, Senior 1st Class, 1,000,000 EP; Wizard King waives rank and EP), held page = long wind-up, ALL current mana at the release, three pulses (60 / 25 / 15 percent) on at most 24 foes each, 15 minute cooldown.

## New vs replacement (rule 5)

* NEW: all 48 pages. No Ultimate existed for any of these attributes (the core eight are Fire, Water, Wind, Earth, Lightning, Light, Dark, Spatial; Time has none by owner rule 3).
* REPLACED: nothing. KEPT untouched: every existing page of every one of these 48 books, including the canon Ultimate-level pages `blade_domain` ("Demon-Dweller Sword: Conquering Eon", Sword) and `mimosa_flower_princess_utopia` ("Flower Princess Utopia", Plant), `grand_explosion` of the Explosion book, and all ids, modes, unlock bits and mastery positions (the Ultimate is appended after everything else by `UltimatePages.append`).
* Shared files edited (append / extend only): `tools/lang/merge_ultimate.py` (reads every `tools/lang/ultimate*.json`), `UltimateRegistryTest` (the "exactly eight books" check became "the eight flagship books are still there"), `UltimatePagesTest` (the new-pages file, lang fragment and doc checks read every group file), `tools/vfx_preview/stubs/.../CompassVisualConfig.java` (a stub so the Mercury / Compass / Slash layer compiles in the preview tool).

## The 48 Ultimates

Columns: wind-up in seconds (ticks / 20), radius in blocks, raw damage of the full pulse (before the 60 / 25 / 15 share, the falloff to 55 percent at the edge and the grimoire's balance law), the three pictures (charge ground picture / release / lingering pulses, each `VfxShape power`; the power of a ground sigil is its radius), the potion effects (halved on bosses), and the feel: fire, freeze, push (negative = pulled in), lift.

| # | Book | Page id | Name | Origin | Wind-up | Radius | Raw | Shapes (charge / release / echo) | Debuffs | Feel |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `book_explosion` | `ultimate_explosion` | Ultimate Explosion Magic: Dawn Detonation | original | 7.5 s | 17 | 42 | FIRE_WILD 14 / FLAME_EXPLOSION 8 / FIRE_BURST 6 | nausea 12s, slow II 3s, weak 7s, blind 2s | fire 4s, push 2.3, lift 0.60 |
| 2 | `book_magma` | `ultimate_magma` | Ultimate Magma Magic: Planet's Core Eruption | original | 9.5 s | 15 | 40 | EARTHFAM_ZONE 15 / FIRE_PILLAR 7 / FLAME_EXPLOSION 5 | slow III 9s, weak II 8s, fatigue II 6s | fire 12s, push 0.3, lift 0.90 |
| 3 | `book_ice` | `ultimate_ice` | Ultimate Ice Magic: Absolute Zero Coffin | original | 8.5 s | 16 | 30 | ICE_FX2 16 / ICE_FX3 7 / ICE_FX3 4 | slow IV 10s, weak II 10s, fatigue II 8s | freeze 15s |
| 4 | `book_mercury` | `ultimate_mercury` | Ultimate Mercury Magic: Silver Tide Requiem | original | 8.0 s | 18 | 32 | MERCURY_FX2 17 / MERCURY_FX3 12 / MERCURY_DOME 6 | slow IV 11s, weak II 13s, fatigue 5s | pull 1.0, lift 0.10 |
| 5 | `book_mist` | `ultimate_mist` | Ultimate Mist Magic: Veil of the Thousand Fogs | original | 7.0 s | 20 | 26 | SOUL_VORTEX 18 / MIST_MAGIC 10 / MIST_MAGIC 6 | blind 8s, nausea 9s, weak 7s, slow 4s | push 0.2, lift 0.10 |
| 6 | `book_star` | `ultimate_star` | Ultimate Star Magic: Stellar Rain of Ruin | original | 8.5 s | 19 | 40 | BORN_NOVA 17 / STAR_MAGIC 12 / LIGHT_FLARE 3 | glow 20s, slow II 4s, weak II 10s, blind 2s | push 0.6, lift 0.30 |
| 7 | `book_storm` | `ultimate_storm` | Ultimate Storm Magic: Heaven's Wrath Typhoon | original | 6.5 s | 19 | 36 | WATER_NEST 17 / LIGHTNING_GOD 5 / WIND_RING 3 | levit. 2s, slow II 5s, weak II 9s, nausea 5s | push 1.2, lift 0.70 |
| 8 | `book_plant` | `ultimate_plant` | Ultimate Plant Magic: Forest of the World | original | 9.5 s | 16 | 30 | PLANT_FX2 16 / PLANT_FX3 7 / PLANT_FX3 4 | poison II 10s, slow IV 12s, fatigue II 8s | pull 0.4 |
| 9 | `book_sand` | `ultimate_sand` | Ultimate Sand Magic: Desert Sovereign's Grave | original | 9.0 s | 17 | 34 | ASH_FORMATION 16 / SAND_MAGIC 12 / SAND_MAGIC 8 | blind 8s, slow III 8s, fatigue II 12s | pull 0.8 |
| 10 | `book_sword` | `ultimate_sword` | Ultimate Sword Magic: Thousand Blades Heaven | original | 7.5 s | 15 | 38 | WEAPON_CONSTRUCTS 4 / KOTO_SWORDS 14 / KOTO_SWORDS 8 | wither 6s, slow II 4s, weak II 8s | push 0.3, lift 0.20 |
| 11 | `book_shadow` | `ultimate_shadow` | Ultimate Shadow Magic: Endless Night Banquet | original | 8.5 s | 16 | 34 | SHADOW_POOL 16 / SHADOW_UNITE 10 / SHADOW_POOL 8 | dark 10s, slow III 7s, wither 5s, weak 6s | pull 0.6 |
| 12 | `book_poison` | `ultimate_poison` | Ultimate Poison Magic: Ten Thousand Venoms | original | 8.0 s | 17 | 28 | KOTO_SLUDGE 4 / POISON_BREATH 12 / POISON_CURTAIN 6 | poison III 15s, hunger II 15s, weak II 12s, nausea 5s | push 0.1 |
| 13 | `book_mirror` | `ultimate_mirror` | Ultimate Mirror Magic: Infinite Reflection Hall | original | 7.5 s | 15 | 34 | MIRROR_FX2 15 / MIRROR_FX3 7 / MIRROR_SHATTER 6 | blind 6s, nausea 10s, weak II 8s, glow 10s | lift 0.20 |
| 14 | `book_gravity` | `ultimate_gravity` | Ultimate Gravity Magic: Collapse of the Star | original | 10.0 s | 18 | 44 | GRAVITY_WELL 18 / GRAVITY_SINGULARITY 12 / GRAVITY_IMPACT 14 | slow IV 11s, weak III 10s, fatigue III 12s | pull 2.0 |
| 15 | `book_sealing` | `ultimate_sealing` | Ultimate Sealing Magic: Grand Seal of Ages | original | 10.5 s | 14 | 14 | SEALING_FX2 14 / SEALING_FX3 6 / SEAL_ORBIT 5 | slow IV 12s, weak III 15s, fatigue III 15s | none (control) |
| 16 | `book_reinforcement` | `ultimate_reinforcement` | Ultimate Reinforcement Magic: Titan's Final Fist | original | 7.0 s | 12 | 48 | SPIRIT_AURA 5 / FX_SPIRIT_NOVA 2 / FX_SPIRIT_NOVA 1.5 | weak III 15s, slow II 4s, fatigue II 10s | push 2.5, lift 0.35 |
| 17 | `book_beast` | `ultimate_beast` | Ultimate Beast Magic: King of the Hunt | original | 7.5 s | 16 | 38 | BEAST_FX2 16 / BEAST_FX3 7 / BEAST_FX3 5 | wither 7s, slow III 6s, weak II 8s | pull 0.5, lift 0.20 |
| 18 | `book_bone` | `ultimate_bone` | Ultimate Bone Magic: Ossuary Cathedral | original | 9.0 s | 15 | 36 | BONE_FX2 15 / BONE_FX3 7 / BONE_FX3 5 | slow III 8s, weak II 7s, fatigue III 10s | lift 0.50 |
| 19 | `book_blood` | `ultimate_blood` | Ultimate Blood Magic: Crimson Sea Reaping | original | 8.5 s | 17 | 36 | BLOOD_SIGIL 17 / BLOOD_DOWNPOUR 12 / BLOOD_SIGIL 9 | wither II 10s, hunger II 15s, weak II 10s, slow II 5s | pull 0.5 |
| 20 | `book_creation` | `ultimate_creation` | Ultimate Creation Magic: Genesis Armoury | original | 8.0 s | 16 | 40 | ALCHEMY_CIRCLE 14 / WEAPON_CONSTRUCTS 8 / DREAM_MANIFEST 8 | slow II 5s, weak II 9s, glow 12s, fatigue II 6s | push 0.4 |
| 21 | `book_copy` | `ultimate_copy` | Ultimate Copy Magic: Mirror of Every Art | original | 7.5 s | 15 | 34 | SEAL_BASIC 14 / MIRROR_SHATTER 7 / MIRROR_FRAME 3 | weak III 12s, fatigue II 10s, glow 15s, slow II 4s | none (control) |
| 22 | `book_illusion` | `ultimate_illusion` | Ultimate Illusion Magic: World of False Dreams | original | 8.5 s | 18 | 20 | DREAM_TRANSITION 18 / DREAM_MANIFEST 12 / MIRROR_DOUBLE 4 | blind 7s, nausea 12s, weak 7s, dark 5s | lift 0.15 |
| 23 | `book_dream` | `ultimate_dream` | Ultimate Dream Magic: Eternal Nightmare | original | 10.0 s | 18 | 22 | DREAM_DOME 18 / DREAM_SHATTER 12 / DREAM_MANIFEST 6 | dark 12s, weak III 12s, slow III 8s, blind 3s | none (control) |
| 24 | `book_anti_magic` | `ultimate_antimagic` | Ultimate Anti-Magic: Null Zone | original | 9.0 s | 15 | 14 | NIHILITY_ZONE 15 / ANTI_MAGIC_SLASH 7 / NIHILITY_ZONE 8 | weak IV 20s, fatigue III 15s, slow II 5s | none (control) |
| 25 | `book_steel` | `ultimate_steel` | Ultimate Steel Magic: Iron Fortress Fall | original | 8.5 s | 14 | 44 | SEAL_CUBE 14 / EARTH_CLAWS 11 / WEAPON_CONSTRUCTS 5 | slow III 8s, weak II 7s, fatigue III 11s | push 1.2 |
| 26 | `book_thread` | `ultimate_thread` | Ultimate Thread Magic: Crimson Loom of Fate | original | 8.0 s | 17 | 30 | THREAD_WEB 17 / THREAD_COCOON 6 / ROUGE_CAT 2 | slow III 11s, weak II 11s, fatigue II 6s | pull 0.4 |
| 27 | `book_transmutation` | `ultimate_transmutation` | Ultimate Transmutation Magic: Alchemical Reversal | original | 9.0 s | 16 | 26 | TRANSMUTATION_FX2 16 / TRANSMUTATION_FX3 10 / ALCHEMY_CIRCLE 8 | weak IV 20s, slow II 6s, fatigue II 10s, nausea 5s | none (control) |
| 28 | `book_ash` | `ultimate_ash` | Ultimate Ash Magic: Cinder Apocalypse | original | 9.5 s | 19 | 34 | ASH_FX2 19 / ASH_FX3 10 / ASH_FX3 7 | blind 7s, wither II 6s, weak 7s | fire 5s, push 0.5, lift 0.40 |
| 29 | `book_cotton` | `ultimate_cotton` | Ultimate Cotton Magic: Cloud Sheep Dreamland | original | 7.5 s | 16 | 16 | ELF_CIRCLE 10 / COTTON_SHEEP 10 / DREAM_TRANSITION 8 | slow IV 12s, weak III 13s, fatigue II 10s | pull 0.2, lift 0.30 |
| 30 | `book_recombination` | `ultimate_recombination` | Ultimate Recombination Magic: Molecular Rewrite | original | 8.5 s | 14 | 32 | RECOMBINATION_FX2 14 / RECOMBINATION_FX3 6 / RECOMBINE_CONSTRUCT 6 | weak III 15s, fatigue III 12s, slow II 5s | push 0.8, lift 0.30 |
| 31 | `book_painting` | `ultimate_painting` | Ultimate Painting Magic: The Masterpiece Canvas | original | 9.0 s | 17 | 34 | PAINT_SPLAT 16 / PAINT_BEAST 6 / PAINT_PALETTE 5 | slow II 6s, blind 4s, nausea 6s, weak II 7s | none (control) |
| 32 | `book_world_tree` | `ultimate_worldtree` | Ultimate World Tree Magic: Yggdrasil Awakening | original | 10.5 s | 19 | 36 | TREE_ROOTS 17 / TREE_CANOPY 12 / TREE_ROOTS 10 | slow III 10s, poison II 8s, weak II 10s | pull 0.3, lift 0.50 |
| 33 | `book_dice` | `ultimate_dice` | Ultimate Dice Magic: All-or-Nothing Jackpot | original | 6.5 s | 15 | 36 | ELF_CIRCLE 9 / DICE_D20 12 / DICE_D6 5 | unluck II 20s, weak II 10s, slow II 5s, nausea 5s | push 0.5, lift 0.20 |
| 34 | `book_slash` | `ultimate_slash` | Ultimate Slash Magic: Dimension-Cleaving Heaven | original | 7.0 s | 17 | 42 | SLASH_FX2 17 / SLASH_FX3 8 / SLASH_SCYTHE 12 | slow II 5s, weak II 8s, wither 5s | push 0.5 |
| 35 | `book_compass` | `ultimate_compass` | Ultimate Compass Magic: Lodestar Rift | original | 8.0 s | 18 | 30 | SEAL_BASIC 18 / SPACE_RINGGATE 10 / SEAL_ORBIT 6 | slow III 9s, weak II 7s, glow 12s, nausea 5s | pull 2.2 |
| 36 | `book_kotodama` | `ultimate_kotodama` | Ultimate Kotodama: Word That Ends the World | original | 11.0 s | 16 | 34 | KOTO_AURA 2.5 / KOTO_SHATTER 8 / KOTO_AURA 2.5 | slow IV 12s, fatigue IV 12s, weak II 6s | none (control) |
| 37 | `book_game` | `ultimate_game` | Ultimate Game Magic: Final Boss Raid | original | 7.5 s | 17 | 38 | GAME_BOARD 17 / LIGHT_FLARE 6 / GAME_BOARD 8 | slow II 5s, weak II 8s, unluck II 15s, glow 15s | none (control) |
| 38 | `book_demon_beast` | `ultimate_demonbeast` | Ultimate Demon Beast Magic: Hellhound Stampede | original | 8.5 s | 17 | 40 | DEMON_BEAST_FX2 17 / DEMON_BEAST_FX3 8 / DEMON_BEAST_FX3 5 | wither II 8s, slow III 7s, weak II 8s | fire 3s, push 0.6, lift 0.20 |
| 39 | `book_body` | `ultimate_body` | Ultimate Body Magic: Overdrive Apex | original | 6.0 s | 12 | 46 | BODY_FX2 12 / BODY_FX3 7 / BODY_FX3 4 | weak III 12s, slow II 3s, fatigue II 8s | push 2.5, lift 0.50 |
| 40 | `book_eye` | `ultimate_eye` | Ultimate Eye Magic: Gaze of the Thousand Eyes | original | 8.0 s | 20 | 30 | EYE_FX2 20 / EYE_FX3 8 / EYE_FX3 5 | glow 20s, blind 5s, weak II 9s, slow 3s | none (control) |
| 41 | `book_eyeball` | `ultimate_eyeball` | Ultimate Eyeball Magic: Argus Vigil | original | 9.0 s | 18 | 34 | EYEBALL_FX2 18 / EYEBALL_FX3 7 / EYEBALL_FX3 4 | glow 15s, weak II 8s, nausea 5s, blind 3s | none (control) |
| 42 | `book_curse` | `ultimate_curse` | Ultimate Curse Magic: Doom of the Seven Generations | original | 10.0 s | 17 | 32 | CURSE_FX2 17 / CURSE_FX3 8 / CURSE_FX3 5 | wither III 12s, weak III 15s, unluck III 20s, slow II 4s | none (control) |
| 43 | `book_curse_warding` | `ultimate_cursewarding` | Ultimate Curse Warding: Great Purification | original | 9.5 s | 16 | 20 | CURSE_WARDING_FX2 16 / CURSE_WARDING_FX3 10 / CURSE_WARDING_FX3 6 | weak II 12s, slow II 6s, glow 12s | lift 0.20 |
| 44 | `book_demon_fire` | `ultimate_demonfire` | Ultimate Demon Fire: Hellfire Throne | original | 8.5 s | 17 | 42 | DEMON_FIRE_FX2 17 / DEMON_FIRE_FX3 8 / DEMON_FIRE_FX3 5 | wither II 7s, weak II 8s, slow II 4s | fire 10s, push 0.3, lift 0.40 |
| 45 | `book_demon_ice` | `ultimate_demonice` | Ultimate Demon Ice: Cocytus Descent | original | 9.0 s | 16 | 34 | DEMON_ICE_FX2 16 / DEMON_ICE_FX3 8 / DEMON_ICE_FX3 5 | wither II 6s, slow IV 9s, weak II 7s | freeze 14s |
| 46 | `book_demon_light` | `ultimate_demonlight` | Ultimate Demon Light: Fallen Dawn | original | 9.0 s | 19 | 36 | DEMON_LIGHT_FX2 19 / DEMON_LIGHT_FX3 8 / DEMON_LIGHT_FX3 5 | blind 7s, wither II 7s, weak II 10s, glow 10s | pull 0.4, lift 0.30 |
| 47 | `book_demon_water` | `ultimate_demonwater` | Ultimate Demon Water: Styx Flood | original | 9.0 s | 18 | 36 | DEMON_WATER_FX2 18 / DEMON_WATER_FX3 8 / DEMON_WATER_FX3 5 | nausea 10s, wither II 6s, slow III 8s, weak II 6s | push 1.2 |
| 48 | `book_barrier` | `ultimate_barrier` | Ultimate Barrier Magic: Absolute Cage | original | 9.5 s | 15 | 24 | BARRIER_FX2 15 / BARRIER_FX3 7 / BARRIER_FX3 4 | slow III 11s, weak III 10s, fatigue II 7s | pull 1.5 |

Origin source notes:

* Origin is `original` for all 48 (see "Canon check"). Rows whose canon situation needs a word:
* Row 7 `STORM`: original (Storm is the Stormcaller Staff magic of Fairy Tail in this mod; no canon Ultimate).
* Row 8 `PLANT`: original; canon counterpart "Flower Princess Utopia" (Mimosa, Plant Ultimate Magic, wiki category Ultimate Magic Spells) already exists as the page mimosa_flower_princess_utopia and is kept.
* Row 10 `SWORD`: original; canon counterpart "Demon-Dweller Sword: Conquering Eon" (Licht, Sword Ultimate Magic) already exists as the page blade_domain and is kept.
* Row 24 `ANTI_MAGIC`: original; Anti-Magic has no canon Ultimate, design decision taken: heavy weakness, no buff stripping.
* Row 33 `DICE`: original; the fate roll of the proposal is not built, the framework is deterministic.
* Every other row: no named Ultimate Magic of that attribute exists in the canon sources that could be searched, so the name is in the show's style, "Ultimate <Attribute> Magic: <Name>".

## Skipped rows

None. All 48 attributes have a registered grimoire (`book("book_<x>", ...)` in `NUSkills`), none already had an Ultimate, and none is Time. Rows built with a documented compromise (the framework has no mechanic for it, so the proposal's idea is reduced to heavy debuffs, damage and pictures):

* `ANTI_MAGIC` (row 24): stripping buffs / silencing magic is not part of the framework; the zone is heavy weakness IV (20 s), fatigue III and slowness with almost no damage (raw 14). Anti-Magic is not rollable, but its grimoire exists, so the page exists; the owner may want a real "strip buffs" rule later.
* `CURSE_WARDING` (row 43): the cleansing of allies is not part of the framework; it is a low-damage ward that weakens, slows and marks foes.
* `DICE` (row 33): the fate roll with variance is not built (the framework is deterministic); the roll always lands on the same heavy bad-luck result.
* `COPY` (row 21): replaying the last three spells needs the copy memory; the replay is only a picture, nothing is read from the caster's spell memory.
* `BLOOD` (row 19): no life-steal for the caster (as in the proposal). `SEALING` (15), `ILLUSION` (22), `DREAM` (23), `COTTON` (29), `TRANSMUTATION` (27, no blocks are changed): control spells with low raw damage (14 to 26) and heavy debuffs.

## Canon check (rule 10: English wiki first, Japanese as the fallback)

* English: the Black Clover Fandom wiki page itself could not be fetched (the host is not reachable from the build machine, `getaddrinfo ENOTFOUND`); WebSearch with the wiki domain returned the list of "Category:Ultimate Magic Spells": Exercitus Leonum (Fire, Mereoleona), Lightning Battle Fiend and Black Lightning Battle Fiend (Lightning, Luck), Apocalypra Astrauza (Lightning, Gadjah), Demon-Dweller Sword: Conquering Eon (Sword, Licht), United Forces (compound, Lucius), Flower Princess Utopia (Plant, Mimosa) and Chrono Anastasis (Time). Of my 48 attributes only Sword and Plant appear, and both already exist as pages of this mod (`blade_domain`, `mimosa_flower_princess_utopia`), so by the rule of the flagship eight (a canon name would replace a working page) the Sword and Plant Ultimates here are ORIGINAL, with the canon neighbour kept. Conquering Eon (WebSearch summary: Licht gathers the allies' magic through the Demon-Dweller Sword and releases it in one slash, chapter 207) and Flower Princess Utopia (Mimosa's healing garden) are not copied.
* Japanese fallback: WebSearch for 究極魔法 per attribute (氷 / 水銀 / 重力 / 夢 / 血) found only the game "Quartet Knights" (main / sub / extra / ultimate / link magic per character) and a strength ranking naming Lucius' 超極大究極魔法; no per-attribute list. Not fetchable as pages.
* Not searched: Fairy Tail wiki (Storm Magic is the Stormcaller Staff of this mod; no canon Ultimate was looked up for it), Tensura wiki.
* Result: canon 0, original 48 (canon counterparts that exist as other pages: 2).

## Pictures (all existing, no new effect, no Time Magic shape)

Every shape is an existing `VfxShape`; the families with a `_FX1/2/3` layer use FX2 (zone, power = radius) as the charge picture and FX3 (burst) for the release, the others use their attribute's own shapes (Gravity and Blood use the Zogratis layers, Reinforcement the Spirit effects, Anti-Magic the Nihility zone, ...). A software-render preview of a sample of eight (the real layer classes, no world, lighting or depth): `docs/previews/ultimate/g12_<attribute>.png` (rows: charge, release, echo) and `g12_overview.png` (the release of all eight): Explosion, Ice, Mercury, Gravity, Blood, Curse, Demon Fire, Barrier. The scenes are in `tools/vfx_preview/scenes.d/ultimate_g12.json` (vertex budget of each: 20 to 388 of the 400 per effect). During the work a contact sheet of all rows showed that the stub layers (`IMITATION_FX*`, `DICE_FX*`, `COMPASS_FX*`, `KOTODAMA_FX*`, `WORLD_TREE_FX*`, `PAINTING_FX*`) only draw a glow dot, so the rows of Copy, Dice, Compass, Kotodama, World Tree and Painting use the attribute's real shapes instead. Not previewable in the tool (layer needs GL / game classes): `RuneCircleLayer` (ELF_CIRCLE: Cotton, Dice), `RecombinationLayer`, `DemonSlayerLayer` (NIHILITY_ZONE, ANTI_MAGIC_SLASH: Anti-Magic), the spec-driven `FX_SPIRIT_NOVA` (Reinforcement); the charge picture of Storm is a `WATER_NEST` dome and its release `LIGHTNING_GOD` (the `STORM_*` shapes need a seed the manager does not send). The pictures take the book's colour (`colorNow()`); the preview uses the real book colour for the eight and white for the rest.

## Worst-case cost (rule 11)

No new code path: the cost of one cast is that of the flagship eight (`docs/magic/ultimate_magic.md`), because every row is read by the same `UltimateManager` and clamped by the same `UltimateRules`: at most `maxSimultaneous` (2, hard max 8) casts on the whole server, at most 16 effect packets per cast (every row's wind-up is 120 to 220 ticks, so at most 13), at most 3 area queries per cast, 96 candidate entities looked at and 24 hit per pulse (72 per cast), no entity spawned, no block changed, no chunk loaded, no packet type, no client class. Data bounds of this group: wind-up 120 to 220 ticks (after `windupScale` always 100 to 400), radius 12 to 20 (max 24 after `radiusScale`), raw 14 to 48 (hard cap 80), at most 4 potion effects each, ticks at most 400, amplifier at most III, fire at most 12 s, freeze at most 300 ticks, push at most 2.5. Client draw cost is that of the reused shapes (preview: 20 to 388 of 400 vertices per effect).

## Tests

* `UltimateG12Test` (new, pure): 24 + 24 rows in table order (read from the audit), ids unique and derived from the magic, book = `book_<magic>`, every book registered in `NUSkills`, every magic a real `MagicType`, every shape a real `VfxShape` and never a Time Magic shape, no row on a core book, no Time row, all within the framework clamps at the largest scale (wind-up, radius, pulse raw, packets), only vanilla potion effects, no row a copy of another (debuff mix, wind-up, radius, push, lift, three pictures), variety (at least 40 debuff mixes, 8 push strengths, 10 wind-ups, pulls / pushes / lifts / fire / freeze all present), lang fragment (text per row, none in `ultimate.json`, merge script reads every fragment), armour-policy new-pages file (one id per magic), this doc has every row, no id in the 1.98.0 page snapshot.
* Extended additively: `UltimateRegistryTest`, `UltimatePagesTest` (new-pages files, lang fragments and docs of every group). The existing source-scan, rules, gate and pages tests run over the whole registry and so over these rows.
* In game (CI server smoke, key prefix `magic.ultimate`): `magic.ultimate.rules` already loops over `UltimateRegistry.all()`: each of these books must have the Ultimate as its last family page with handler, lang text (needs `merge_ultimate.py` run by the coordinator), real shapes and potion effects, and be closed for a fresh player in 6 ways. `magic.ultimate.cast` uses the Fire row and the framework; it is unchanged.

## Files

New: `tools/lang/ultimate_g12.json` (48 page texts), `src/test/resources/armour_policy/ultimate_g12_new_pages.json`, `src/test/java/.../ultimate/UltimateG12Test.java`, `tools/vfx_preview/scenes.d/ultimate_g12.json`, `docs/previews/ultimate/g12_*.png`, this doc. Changed: `ultimate/UltimateDefs1.java`, `ultimate/UltimateDefs2.java`, and the shared files listed under "New vs replacement". Not touched (by order): `UltimateRegistry`, `UltimateDefs3/4`, `ultimate.json`, `en_us.json`, version, CHANGELOG, README, LIBRARIES, `.github`.

## Decisions left to the owner

* The 48 lang texts live in `tools/lang/ultimate_g12.json`; the coordinator must run `python3 -B tools/lang/merge_ultimate.py` (it now reads every `ultimate*.json`) before the in-game self-test can pass.
* Whether the two canon counterparts (Conquering Eon, Flower Princess Utopia) should be folded into the Sword and Plant Ultimates later, and whether Anti-Magic should get a real "strip buffs" rule.
* Numbers (wind-up, damage, debuff mix) are first drafts tuned against the framework's clamps and each other, not playtested.

## Not verified

Nothing was played: no cast in a real client, no check of how the grimoire shows the 48 new pages (the book list is the page list plus one), how the key flow feels, how each picture looks inside the game (only the software-render previews of the real layers for a sample of eight, no world, lighting or depth; the preview tint of non-sample rows is white), no multiplayer run, no dedicated-server run. The in-game self-test and the server smoke test were not run here (CI does).
