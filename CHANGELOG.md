# Changelog

## [3.30.0] — Veldanava, the anime Creator God (setting `classicMode`)

### NEW
- **God-tier Veldanava** (`VeldanavaStats`): exactly 5,000,000 health, attack 80 (one-shots an unarmoured player), speed 0.55, armor 30 / toughness 20, knockback resistance 1.0, follow range 96. Vanilla's 1024 health cap is lifted for that one attribute (`HealthCap`); if a runtime refuses, a damage divisor keeps the fight the same size.
- **Nullifications:** fall, fire, lava and wither damage are refused; poison, wither, slowness and every other potion or status effect cannot be applied.
- **Ultraspeed Regeneration:** heals 0.1% of his maximum per second while fighting (setting `regenPercentPerSecond`), unless he is hit by the counter-element (damage type tag `nusmp:veldanava_counter`: `conceptual_severance`, `out_of_world`), which breaks it for 6 seconds.
- **Boss bar per form** (`VeldanavaBar`): teal dragon, white-gold Creator God, violet-crimson Dark Avatar; pulsing title and symbols, flash while changing body, yellow while exposed or guarding, red while regeneration is broken, a status line, and "4,999,200 / 5,000,000".
- **Six new moves** with their own effects: Absolute Severance, Black Lightning, Black Lightning Blast, Death Tornado, Disintegration, Hell Flare, and a second ultimate, Megiddo. Seven new `VELD_*` visual shapes with layers and preview scenes.
- Server settings `classicMode` (false), `healthTotal` (5,000,000) and `regenPercentPerSecond` (0.1) in `nusmp-veldanava-server.toml`.
- Tests: `VeldanavaAnimeTest`; the rules, source-scan tests and the `veldanava.*` self-tests cover the new numbers.

### REPLACED
- Outside `classicMode`: the 3.11.0 stats (4000 health, attack 18, speed 0.36, armor 12 / toughness 6, follow 64) and the old resistance list.

### KEPT
- All older moves, forms, models, loot and boons; `bossHealth` and the 3.11.0 behaviour come back with `classicMode = true`.

### Caveats
- Not seen in a running game. 5,000,000 health is practically unkillable at ordinary damage (lower `healthTotal` if needed). The health cap is lifted by reflection.

## [3.29.0] — A signature effect for each of the ten Evolution Ultimates (setting `visual.uniqueUltimates`)

### NEW
- **Ten unique Ultimate effects.** Iron Shadow Dragon Mode, Satan Soul: Hell's Core, All Crush, Water Body: Great Flood, Monarch of Shadows, Chairman's Last Stand, Devouring Shadow, Final Dose, Disaster of Floods and Elf Ring: Thousand Elf each play their own effect (shapes `ULT_*`, ten new layers), instead of a generic circle, pool or burst. Gajeel: iron dragon and rust. Mirajane: pentacle, bat wings and a black core. Gildarts: a cracked-glass disc that lets go. Juvia: a water wall, a water woman and a crashing wave. Ashborn: shadow soldiers, a crown and a violet pillar. Go Gun-hee: a gold guild seal, light lances and thunder. Regis: a shadow maw of fangs closing on a wolf head. Lee Seolhwa: needles that close into a venom flower. Shin Yoosung: a storm tide with monsters on the crests. Lefiya: a vast elven circle with ten small circles firing borrowed spells. Details, costs and previews: `docs/vfx_remake/unique_ultimates.md`.
- Client setting `visual.uniqueUltimates` (config/nusmp-ultimates-client.toml, **default on**): off plays the generic effect each Ultimate used before 3.29.0.
- Self-test `evolution.ultimatevfx` (CI server smoke test) and `UniqueUltimatesTest`: every unique shape is used by exactly one shipped Ultimate, has its layer and scene, and the switch restores the old shapes.

### REPLACED
- The effect of ten Ultimates (their `vfx` entry in the line rows and generated skill trees). The old shapes (SHADOW_POOL, DEVIL_CIRCLE, ASH_FORMATION, WATER_BURST, SPIRIT_AURA, POISON_BREATH, ELF_CIRCLE) stay in the mod and are used again when the setting is off. Older skill books are unchanged.

### KEPT
- Damage, radius, cooldown and every other number of the ten Ultimates; the dormant shapes of 3.28.1 are now filled in (same names).

### Cost (CLAUDE.md rule 11)
- Server: nothing added (same single effect packet, only the shape id differs). Client: at most 400 vertices per activation, worst measured 384, constant loops, no per-frame collections, deterministic from the seed; one activation per Ultimate cast (cooldowns 15000-18000 ticks).

### Not verified / caveats
- Seen only in the headless preview, not in a game: depth against blocks, real lighting, blending and texture filtering are unchecked. Weak spots: the Iron Shadow Dragon head is a spiky camera-facing shape, the Disaster of Floods monsters read as dark blobs, the Great Flood wave is boxy, the Devouring Shadow head reads as a shadow-cat more than a wolf, and the flashes of Chairman's Last Stand and Thousand Elf are heavy at their peak.

## [3.28.1] — Worn relics drawn the 1.41.0 way again (setting `relicfx.classicWorn`)

### FIXED
- **Worn relics did not all show on the player.** Multiverse 1.41.0 (build 213) drew every relic a player had in a Curios or armour slot; later releases put a per-relic plan (`WornPlan`) in front of that draw and not every relic got through it. New client setting `relicfx.classicWorn` (config/nusmp-relicfx-client.toml, **default on**) draws the worn relics exactly the way build 213 did: its own renderer, else the library model (best grade first), else the plain box of its slot (a slot with no box gets the floating charm, never nothing). The Curios eye icon, `relicfx.hideArmourRelics`, `relicfx.showWorn`, the distance cull and invisible/spectator wearers still hide relics as before. Off = the plan-driven layer of 3.28.0, unchanged.

### NEW
- Setting `relicfx.classicWorn` (client, looks only). Dormant groundwork for the unique Ultimate effects: ten new `VfxShape` names (`ULT_IRON_DRAGON` ... `ULT_THOUSAND_ELF`, appended) with empty placeholder layers and the same names in the art catalogue. No skill uses them yet (the ten Ultimates keep their old effects), so nothing changes in play. The Ultimate burst effect now lasts 20 ticks instead of 16.

### REPLACED
- Nothing. The plan-driven draw is kept behind the setting.

### Cost (CLAUDE.md rule 11)
- Client only, no server work, no packets. The classic draw reads the same cached relic list as before (`Relics.locatedList`) and draws through the same budgeted library (`relicfx.quality`: at most about 2400 / 4200 / 6000 vertices per player); wearers beyond 32 blocks are not drawn.

### Not verified / caveats
- Nothing visual could be run here (no game, no Gradle). The classic path is a port of the 1.41.0 loop onto the current relic list; whether every relic now shows needs a look in game. If one still does not, `/multiverse relic debug` says why.
- Jar size (about 417 MB): not a fat jar (no shadow plugin, no `.git`/`run` files); the size is real content: about 300 MB of PNG, of which 14 weapon textures are 2048x2048 (5 to 9 MB each). The PNGs are already optimally compressed; shrinking them means lowering their resolution, which is the owner's decision (see the report).

## [3.28.0] — Third Evolution batch: ten more Unique skills (Fairy Tail, Solo Leveling, Omniscient Reader's Viewpoint, TBATE, DanMachi)

### NEW
- **Ten more Evolution Unique skills** (10 new books, 284 in all), same format and same engine as 3.26.0 and 3.27.0 (no engine change). Fairy Tail gets four: Iron Dragon Slayer (Gajeel; Ultimate "Iron Shadow Dragon Mode"), Take Over: Satan Soul (Mirajane; "Satan Soul: Hell's Core"), Crash Magic (Gildarts; "All Crush") and Water Body (Juvia; "Water Body: Great Flood"). Solo Leveling: Ashborn, the Shadow Monarch ("Monarch of Shadows") and Chairman Go's Fist (Go Gun-hee; "Chairman's Last Stand"; his team buff "Hold the Line" sits on a normal finisher). Omniscient Reader's Viewpoint: Lee Seolhwa, Needle and Blade ("Final Dose") and Shin Yoosung, Monster Gate ("Disaster of Floods"; her team buff on a normal finisher). TBATE: Regis Leywin, the Devouring Shadow ("Devouring Shadow"). DanMachi: Lefiya Viridis, the Thousand Elf ("Elf Ring: Thousand Elf"). Each has 2 stances, 3-4 stats, 8 arts, 7-8 passives (one Debt store, 2-3 evolved), 2 finishers and a locked Ultimate Evolution.
- Evolution requirements: Gajeel 4,000 damage taken, Mirajane 600 arts cast, Gildarts 120,000 damage dealt, Juvia 12 lethal blows survived, Ashborn 12 boss kills, Go Gun-hee 40 elite kills, Regis 900 kills, Lee Seolhwa 700 kills, Shin Yoosung 350 ally buffs, Lefiya 500 arts cast.
- Rarities: 2 mythic (Gildarts, Ashborn), 4 epic (Mirajane, Go Gun-hee, Regis, Lefiya), 4 rare (Gajeel, Juvia, Lee Seolhwa, Shin Yoosung); each book uses the franchise look of 3.26.0. Ten new 32x32 skill icons, lang keys, research notes in `docs/skills/research/*_c.md`, plan in `docs/skills/evolution_ten_c.md`.

### REPLACED
- Nothing. (All ten are new; no line of the mod covered these characters.)

### KEPT
- Everything of 3.27.0 and before: all 274 earlier books and lines, all looks, the engine, all settings and saves. Juvia's art is "Water Body Form" and Laxus's stays "Fairy Law Dome" because the ids `water_body` and `fairy_law` belong to removed old magics (kept banned).

### Cost (CLAUDE.md rule 11)
- Identical to 3.27.0: counters O(1) per event; Debt tracks at most 20 foes per player and one burst touches at most 16 foes, cooldown 12000 to 18000 ticks; no entities, no per-tick scans, no new packet type. Data only: ten route files read once at load.

### Not included / caveats
- Nothing has run in a real game or under Gradle on this machine; CI builds and runs the server smoke test.
- The wikis could not be opened from the build environment (host unreachable). Canon came from search summaries only: English first, the Japanese searches added little. Many art and passive names are invented in the spirit of the character (marked per skill in `docs/skills/research/*_c.md`); all numbers are the mod's own.
- Canon is thin for Go Gun-hee (mostly invented arts), Regis (no confirmed Shadow technique: shadow arts are invented) and Shin Yoosung (the area form of Beast King's Breath is invented).
- Lee Seolhwa turned out to be the Company's doctor and poison specialist, not a swordswoman as first planned; the id `lee_seolhwa_sword` stays (her blade is invented).
- The characters were chosen by me because no names were given; swapping any is a small change in `tools/lines/rows/`.

### Checked
- Line schema, text, asset, rarity, lang and file tests for all 284 lines; source scans, `ServerSafetyTest`, `ConfigFileNamesTest`; CI server smoke test (existing `skillbook` and `evolution` prefixes cover the new lines).

## [3.27.0] — Another Evolution batch: ten more Unique skills (Fairy Tail, DanMachi, Solo Leveling, Tower of God, God of High School)

### NEW
- **Ten more Evolution Unique skills** (10 new books, 274 in all), same format and same engine as 3.26.0 (no engine change): Fire Dragon Slayer (Natsu), Lightning Dragon Slayer (Laxus; Ultimate "Red Lightning Dragon Mode") and Sky Dragon Slayer (Wendy; team buff Ile Armor on a finisher) from Fairy Tail; Ais, the Sword Princess (DanMachi; Ultimate "Tempest Unbound"); Cha Hae-In's Sword and Baran, Monarch of Demons (Solo Leveling); Anak Zahard and Rak Wraithraiser (Tower of God); Ilpyo's Family Blade and Taejin's Renewal Charyeok (The God of High School). Each has 2 stances, 3-4 stats, 6-8 arts, 6-8 passives (one Debt store, 2-3 evolved), 2 finishers and a locked Ultimate Evolution.
- Evolution requirements: Natsu 150 elite kills, Laxus 8 boss kills, Wendy 400 ally buffs, Ais 800 kills, Cha Hae-In 30 elite kills, Baran 80000 damage dealt, Anak 60000 damage dealt, Rak 700 arts cast, Ilpyo 1500 kills, Taejin 10 lethal blows survived.
- Rarities: 1 mythic (Baran), 6 epic, 3 rare (Wendy, Rak, Ilpyo); each book uses the franchise look of 3.26.0. Ten new 32x32 skill icons, lang keys (name and description per skill), research notes in `docs/skills/research/*_b.md`, plan in `docs/skills/evolution_ten_b.md`.

### REPLACED
- Nothing. (All ten are new; no line of the mod covered these characters.)

### KEPT
- Everything of 3.26.0 and before: all 264 earlier books and lines, all looks, the engine, all settings and saves. Laxus's art is called "Fairy Law Dome" because the id `fairy_law` belongs to a removed old magic (kept banned).

### Cost (CLAUDE.md rule 11)
- Identical to 3.26.0: counters O(1) per event; Debt tracks at most 20 foes per player and one burst touches at most 16 foes, cooldown 12000 to 18000 ticks; no entities, no per-tick scans, no new packet type. Data only: ten route files (about the size of the existing ones) are read once at load.

### Not included / caveats
- Nothing has run in a real game or under Gradle on this machine; CI builds and runs the server smoke test.
- The wikis could not be opened from the build environment (host unreachable). Canon came from search summaries only: English wiki digests first; the Japanese wikis could not be fetched either. Many art and passive names are invented in the spirit of the character (listed per skill in `docs/skills/research/*_b.md`), all numbers are the mod's own.
- The characters were chosen by me because no names were given; swapping any of them is a one-line change in `tools/lines/rows/`.
- Wendy's team buff (Ile Armor) sits on a normal finisher because the Ultimate must be a Debt burst.

### Checked
- Line schema, text, asset, rarity, lang and file tests for all 274 lines; source scans, `ServerSafetyTest`, `ConfigFileNamesTest`; CI server smoke test (existing `skillbook` and `evolution` prefixes cover the new lines).

## [3.26.0] — Skill Books: new look per franchise, rarity tiers, franchise blacklist, ten Evolution skills

### NEW
- **New Skill Book look** (client setting `legacyLook`, default false, file `nusmp-skillbook-look-client.toml`): painted 3D books in hand, one look per franchise (11 franchises, Fairy Tail added), five rarities each (common, uncommon, rare, epic, mythic) that add trim and glow; epic and mythic glint, the mythic page edge is animated. 55 body models, 110 inventory icons, item properties `nusmp:rarity`, `nusmp:franchise`, `nusmp:look`. Optional drop shimmer (`dropShimmer`). See `docs/skillbooks_3_26_look.md`.
- **Rarity tiers** (server file `nusmp-skilltree-loot-server.toml`, section `[rarity]`: `rarityTiers`, `guardianBookChance`, `rarityBaseChance`): tier 1 Luck- and Looting-scaled weighted drops, tier 2 themed Guardian drops (27 region themes), tier 3 rift pockets (mythic only). Rarity is also stored on the stack (`nusmp:skill_book_rarity`). See `docs/skillbooks_3_26_rarity.md`.
- **Franchise blacklist** (`hideBlockedFranchises`, `blockedFranchises`, default Tensura, Black Clover and Magi): books of blocked franchises no longer drop and are not sold; the files are kept and `/give` still works.
- **Evolution engine** (server file `nusmp-evolution-server.toml`, `evolutionSkills`): per-player counters (kills, elite kills, boss kills, damage dealt, damage taken by kind, lethal blows survived, arts cast, ally buffs), evolved passive numbers, the Debt mechanic (`debt_store` passive, `debt_burst` Ultimate) and the locked Ultimate Evolution art. See `docs/skills/evolution_engine.md`.
- **Ten Evolution Unique skills** (10 new books, 264 in all): Rose Shower and True Flame (Tower of God), Dragon Monarch's Breath and Tiger's Claws (Solo Leveling), Uchide no Kozuchi and Astraea's Wooden Sword (DanMachi), Ice-Make and Requip: The Knight (Fairy Tail), Haetae to Jade Emperor and Yeo-Po Bong-Seon (The God of High School). 1 mythic, 4 epic, 5 rare. See `docs/skills/evolution_ten.md`.
- Tests: rarity, evolution, look and asset tests; self-tests `skillbook.rarity.drops`, `skillbook.rarity.read`, `skillbook.blocked`, `skillbook.look.files`, `evolution.counters`, `evolution.debt`.

### REPLACED
- Nothing removed. The default look of every Skill Book changes to the new one; the old look returns with `legacyLook`. The old flat rule "every book glints" becomes "epic and mythic glint" (old rule with `legacyLook`).

### KEPT
- Everything of 3.24.0, the old 1.82.0 models and textures (used by `legacyLook`), all existing books and lines, all old saves. Fairy Tail is a franchise again (the removed Fairy Tail line ids stay removed). Time Magic VFX untouched.

### Cost (CLAUDE.md rule 11)
- Server: no entities, no per-tick work. Counters are O(1) per event; Debt tracks at most 20 foes per player (pruned on hold time, death, logout, dimension change), one burst touches at most 16 foes per cast, cooldown 12000 to 18000 ticks. No new packet type; the route/card wire format gained an appended evolution block.
- Client: one model per held or dropped book, no per-frame allocation, shimmer capped by `BookShimmerRules` and distance-culled.

### Not included / caveats
- Nothing has run in a real game or under Gradle on this machine; CI builds and runs the server smoke test. The look was judged only in the Python previewer (hand scale 0.46 first person / 0.55 third person is tunable; the Black Clover silver relief is faint; mythic claws read as a crest; there is no new generic cover for books with no franchise; real lighting, emissive layer, animated edge, item frames and inventory tint are unverified).
- Looting and Luck reads were never exercised with real potions; the copy of evolution counters on death-respawn is unverified in game.
- Client and server must run the same version (the route/card wire format changed).
- Wikis could not be opened (host unreachable, proxy 403); the canon came from search summaries; several canon names are invented in the spirit of the series (see `docs/skills/research/`). All numbers are the mod's own.
- Haruhime's team buff sits on her first finisher, not on her Ultimate (the Ultimate must be a `debt_burst`).

### Checked
- Test sweep: every test class that reads the repo's files, source scans, `ServerSafetyTest`, `ConfigFileNamesTest`, particle, texture and lang tests; new classes pass individually; the remaining failures equal the stub-classpath baseline of this machine. CI build and server smoke test are run by this release.

## [3.24.0] — Solomon: abilities that hurt and 3D models

### NEW
- **Solomon, abilities that should hurt now do** (setting `solomonUpgrades`, default true; every blow goes through the Balance Law and the active-damage cap):
  - **Arcane Convergence** flings what it drew in back: 40 true damage plus 8 for every projectile, cloud or field taken in (at most 240) to the 8 nearest foes within 16 blocks.
  - **Mana Zone Erasure** has a backlash: each caster whose spell was erased and who stutters loses 5% of its maximum health (at most 24 casters).
  - **Realm of Magic** wears its enemies down: each second every enemy inside (at most 24) loses 1% of its maximum health, on top of the silence and the action lock.
  - The 72 Pillars, Grand Spell Copy and Spell Chain already dealt damage (the pillars 70 each) and are unchanged.
- **Solomon 3D models in the effects** (client setting `visual.solomonModels`, default true, file `nusmp-solomon-client.toml`): six real fluted marble pillars with element sigils rise in a ring round each target of the 72 Pillars; a floating open Grimoire with turning pages for Grand Spell Copy and the Realm of Magic; rune-engraved chain links and a sigil head for Spell Chain; Solomon's signet ring with a violet gem over the marked target; rune crystals in the Convergence streams; falling shards of glass for the Erasure. See `docs/vfx_remake/solomon_models.md`.
- Tests: `SolomonModelsTest`, extended `JudgmentRulesTest` and `JudgmentSourceScanTest`; self-test `solomon.upgrades`.

### REPLACED
- Nothing removed. Only while the models setting is on, the particle detail is not minimal and the effect is near: the flat page and book sheets of Copy and the Realm, the flat chain strip of Spell Chain, one rune sprite in five of the Convergence and the falling-pillar light of the 72 Pillars (capped to 3 falling, 3 landing and 4 scars while the ring is drawn) are drawn as models instead; the Mark gets a ring and the Erasure gets shards added.

### KEPT
- Everything of 3.23.0: every old texture and draw call (the fallback when `visual.solomonModels` is off, far away or out of budget), the toggle bits and old saves, the master toggle, the Time Magic VFX (untouched). With `solomonUpgrades` off the abilities behave as in 3.23.0.

### Cost (CLAUDE.md rule 11)
- Server: no new packets, entity spawns or chunk loads. The Convergence burst is one more 16-block box query per press, at most 8 true hits; the Erasure backlash is at most 24 hits on the casters the sweep already holds; the Realm's wear is one `share` call per enemy its pulse already loops over each second (at most 24, no new query).
- Client: at most 100 quads / 400 vertices per effect (the engine cap). Measured worst cases with models on: Copy 41, Convergence 81, Realm 42 (two overlapping 84), Chain 75, Erasure 72, Mark 53, Pillars 88 (18 pillars) / 99 (72 pillars, the buffer stops at 100). Every model asks the budget first and is skipped whole; no allocation per frame.

### Not included
- Not played in a real game, previews are headless only. Known weaknesses: the chain links are rectangular loops rather than round rings; the ring bezel reads flat from some angles; the sigil panels of the pillars are soft at 128 px; the erasure shards are thin slivers edge-on; the 72 Pillars at its full 72 sits at 99 of 100 quads, so a busy view simply leaves the last scars undrawn.

### Checked
- The whole test sweep (every test class that reads the repo's files, the source scans, `ServerSafetyTest`, `ConfigFileNamesTest`, the particle and texture tests): nothing new failed against the baseline (1792 passed; the only differences are the stub-classpath artefacts of `AnimatedIconsTest`); `SolomonModelsTest`, `SolomonShapesTest`, `JudgmentRulesTest` and `JudgmentSourceScanTest` pass. CI build and server smoke test are run by this release.

## [3.23.0] — Jibril: abilities that hurt and 3D models

### NEW
- **Jibril, abilities that should hurt now do** (setting `jibrilUpgrades`, default true; every holy blow goes through the Balance Law and the active-damage cap):
  - **Judgment of Heaven** smites the unholy: the undead, demons and creatures carrying Temporal Debt take half as much again (330 instead of 220 base).
  - **Heavenly Purge** burns the unholy within 14 blocks of you: 120 holy damage to the 12 nearest foes that are undead, demonic or in debt. Peaceful creatures and allies are spared.
  - **Holy Shackles** bite: 40 holy damage plus 25 for every good effect they take from the target (at most 8 count).
  - **Kingdom of God** wears its enemies down: each second every enemy inside (at most 24) loses 1% of its maximum health.
- **Jibril 3D models in the effects** (client setting `visual.jibrilModels`, default true, file `nusmp-jibril-client.toml`): hinged golden cuffs and chain links for the Holy Shackles; a Spear of Heaven that falls through the light and stays in the pillar for Judgment of Heaven; an open tome of law for Word of Law; a crown with six crystal spikes for Absolute Command; a halo with three orbiting law plates for the Kingdom of God and the Wings. See `docs/vfx_remake/jibril_models.md`.
- Tests: `JibrilModelsTest`, extended `JudgmentRulesTest` and `JudgmentSourceScanTest`; self-test `jibril.upgrades`.

### REPLACED
- Nothing removed. Only while the models setting is on, the particle detail is not minimal and the effect is near (64 blocks; 42 for the Kingdom and Wings halo, plates and cuffs): the upper and lower flat chain rings of the Shackles, the flat scroll panel and two nested halos of Word of Law, the inner peach halo of Command, and the two flat halos, orbiting feathers and prism bloom of the Kingdom are drawn as models instead. Some petals, motes, sparks and feathers are thinned while a model is drawn, to stay under the caps.

### KEPT
- Everything of 3.22.0: every old texture and draw call (the fallback when `visual.jibrilModels` is off, far away or out of budget), the toggle bits and old saves, the master toggle, the Time Magic VFX (untouched). With `jibrilUpgrades` off the abilities behave as in 3.22.0.

### Cost (CLAUDE.md rule 11)
- Server: no new packets, entity spawns or chunk loads. Judgment's smite is a multiplier on the blow it already makes; the Purge burn is one more 14-block box query per press, at most 12 creatures; the Shackles' bite is one blow on one creature; the Kingdom's wear is one `share` call per enemy it already touches each second (at most 24).
- Client: at most 100 quads / 400 vertices per effect (the engine cap). Measured worst cases with models on: Command 66 quads, Judgment 76, Shackles 79, Word 53, Kingdom 44 of 50, Wings 47 of 50. Judgment beyond 64 blocks draws the old flat effect. Every model asks the budget first and is skipped whole; no allocation per frame.

### Not included
- Not played in a real game, previews are headless only. Known weaknesses: the cuffs read as golden hexagonal bands, not round manacles; the chain links read as flat picture frames (the weakest model); the spear reads well only while falling, then the bright pillar swallows most of it; the law plates are small at range and the tome is blown out in the first ticks of the flash; the models use a fixed sun, not world lighting; the decorative extras are visibly fewer while a model is drawn.

### Checked
- The whole test sweep (every test class that reads the repo's files, the source scans, `ServerSafetyTest`, `ConfigFileNamesTest`, the particle and texture tests): nothing new failed against the baseline (1783 passed; the only differences are the stub-classpath artefacts of `AnimatedIconsTest`); `JibrilModelsTest`, `JudgmentRulesTest` and `JudgmentSourceScanTest` pass. CI build and server smoke test are run by this release.

## [3.22.0] — Israfil: abilities that hurt and 3D models

### NEW
- **Israfil, abilities that should hurt now do** (setting `israfilUpgrades`, default true; every blow goes through the Balance Law and the active-damage cap):
  - **The Grand Trumpet** is now a blast as well as a silence: 150 true damage close in, falling in a straight line to a quarter of that at the rim (100 blocks), on the 48 nearest hostile creatures.
  - **Coordinate Shift** shears the hostile creature it trades places with for 70 true damage as it lands.
  - **Spatial Collapse**: when its Nihility field runs out by itself it implodes once more, 8% of maximum health to everyone still inside, and pulls them in.
- **Israfil 3D models in the effects** (client setting `visual.israfilModels`, default true, file `nusmp-israfil-client.toml`): a lathe-turned Grand Trumpet (gold band with gems, coiled tube, glowing mouth) that swings up, blares, cracks and breaks into four flying shards; a weight plate that falls wobbling on chains and slams onto the target for Weight of Sins; maze walls and pillars that slide at each re-shuffle of the Labyrinth; faceted reality shards spiralling into an icosahedron core for Spatial Collapse; and a coordinate gate at each end of a Coordinate Shift. See `docs/vfx_remake/israfil_models.md`.
- Tests: `IsrafilModelsTest`, extended `JudgmentRulesTest` and `JudgmentSourceScanTest`; self-test `israfil.upgrades`.

### REPLACED
- Nothing removed. Only while the models setting is on, the particle detail is at least 0.5 and the effect is near (horn 80, plate 52, maze 60, core 64, shards 44, gates 44 blocks): the flat Trumpet sprite and glow, the flat coordinate plate and its ring, the first 8 pull-in billboard shards and 4 orbit shards of the Collapse and the two void billboards are drawn as models instead; the Shift hoop planes shrink to sit inside the gate; at the Trumpet the billboard shards are cut to a third and the motes dropped while the horn is up.

### KEPT
- Everything of 3.21.0: every old texture and draw call (the fallback when `visual.israfilModels` is off, far away or out of budget), the toggle bits and old saves, the master toggle, the Time Magic VFX (untouched). With `israfilUpgrades` off the abilities behave as in 3.21.0.

### Cost (CLAUDE.md rule 11)
- Server: no new packets, entity spawns or chunk loads. The Trumpet's blast reuses the up to 48 creatures it already touches (one blow each); the shear is one blow on one creature; the last implosion is one box query when a Nihility field ends naturally, at most the field's target cap (24 creatures), no packet beyond the existing effect.
- Client: at most 100 quads / 400 vertices per effect (the engine cap). Measured worst cases with models on: Labyrinth 74 quads, Gravity 60, Collapse 50, Shift 54, Trumpet 98 (the tight one), Rift 27 (unchanged). Every model asks the budget first and is skipped whole; no allocation per frame.

### Not included
- Not played in a real game, previews are headless only. Known weaknesses: the Trumpet's bell goes pale and over-exposed in the burst of light and its tube is slender next to it; the weight plate is dark navy and reads somewhat like a thin tray; the coordinate gates are small and plain; the shard textures are small at range; the Trumpet peaks at 98 of 100 quads (a view that needs more than the reserve falls back to the flat horn).

### Checked
- The whole test sweep (every test class that reads the repo's files, the source scans, `ServerSafetyTest`, `ConfigFileNamesTest`, the particle and texture tests): nothing new failed against the baseline; `IsrafilModelsTest` 10/10, `JudgmentRulesTest` and `JudgmentSourceScanTest` 36/36. CI build and server smoke test are run by this release.

## [3.21.0] — Sandalphon: abilities that hurt and 3D models

### NEW
- **Sandalphon, abilities that should hurt now do** (setting `sandalphonUpgrades`, default true; every blow goes through the Balance Law and the active-damage cap):
  - **Judgment Bullet** adds 12 true damage for every good effect it tears off (at most 3 count).
  - **Edict** (the rule that strips boons) now also deals damage: 60 plus 20 per good effect stripped (at most 4), on enemies, players and mobs that target you.
  - **Guillotine Cleave** grows with the Temporal Debt on the target (up to +30%) and **executes** a sigil-marked target below 35% health.
  - **Verdict field** tolls when it runs out naturally: 6% of maximum health to the creatures inside (bounded to the field's target cap), with a bell.
- **Sandalphon 3D models in the effects** (client setting `visual.judgmentModels`, default true, file `nusmp-judgment-client.toml`): an angled guillotine blade with a glowing bevel for the Cleave, scales of judgment (a feather against a burning heart, tilting and settling) for the Field, a law tablet that rises, flashes and shatters into shards for the Edict, a spinning lance head with gold barbs for the Bullet, and a sword of judgment that descends, plunges and stands in the ground for the Cross. See `docs/vfx_remake/judgment_models.md`.
- Tests: `JudgmentModelsTest`, extended `JudgmentRulesTest` and `JudgmentSourceScanTest`; self-test `sandalphon.upgrades`.

### REPLACED
- Nothing removed. Only while the models setting is on, the particle detail is at least 0.5 and the effect is within 64 blocks, the Cleave's old four-stroke blade loop is replaced by the model. The Bullet head flare, the Edict glyph band and motes, and the Field and Cross motes are dimmed or thinned (not removed) beside the new models.

### KEPT
- Everything of 3.20.0: every old texture and draw call (the fallback when `visual.judgmentModels` is off, far away or out of budget), the toggle bits and old saves, the master toggle, the Time Magic VFX (untouched). With `sandalphonUpgrades` off the abilities behave as in 3.20.0.

### Cost (CLAUDE.md rule 11)
- Server: no new packets, entity spawns or chunk loads. The Bullet and Edict reuse the creatures they already touch; the Cleave adds one debt lookup per hit; the toll is one 1-box query when a Verdict field ends naturally, at most the field's target cap. Worst case per press unchanged except one extra blow per creature already queried.
- Client: at most 100 quads / 400 vertices per effect (the engine cap). Measured worst cases with models on: Bullet 49 quads, Cleave 73, Field 90, Edict 71, Cross 76 (Sigil unchanged, 17). Every model asks the budget first and is skipped whole; no allocation per frame; models are drawn only within 64 blocks.

### Not included
- Not played in a real game, previews are headless only. Known weaknesses: the guillotine tang is a bit long and the blade is thin from the side; the lance body vanishes into the bright beam core (its gold barbs carry it); the scales, tablet and sword turn by a fixed yaw and can look like billboards when you walk around them; the rising tablet is not clipped by terrain; the sword looks slim and grey before the boom. The optional sigil model was not built.

## [3.20.0] — Master toggle for the five Ultimates, Hercules 3D models and stronger abilities

### NEW
- **One master toggle for the passives of all five Ultimates** (owner: "the passives should be under the tensura toggle button in the Base Tensura Mod"): Sandalphon, Israfil, Jibril, Solomon and Hercules now use Tensura's own ON/OFF toggle of the skill. ON turns the four passives on together, OFF turns them all off. The four passive entries leave the mode wheel (six modes are left: five actives and the mastery active); their bits stay in the skill and the player mask, so old saves keep working, and a save with any passive on comes up with the toggle ON. Setting `masterToggle` (default true) restores the old ten-mode wheel.
- **Hercules, abilities that should hurt now do** (setting `herculesUpgrades`, default true): Earth-Shattering Tremor ends with an **aftershock** 14 ticks later (half a tremor, 9 blocks, up to 24 creatures, no blocks broken); Titan's Grip **crushes** what it pulls; Battle Roar **thunders** on the nearest sixteen foes within 12 blocks; Invictus gives a **second wind** (10% of maximum health, 20% mastered) when it runs out. All blows go through the Balance Law and the active-damage cap.
- **Hercules 3D models in the effects** (client setting `visual.herculesModels`, default true, file `nusmp-hercules-client.toml`): a bronze Titan hand for Titan's Grip, a clenched fist for World-Breaking Strike, a spinning boulder for a hurled chunk of ground (Devastating Throw sends power 2 for a chunk), a laurel crown and ring for Invictus, rock chunks and glowing fissures for the Tremor, a lighter variant for the aftershock. See `docs/vfx_remake/hercules_models.md`.
- Tests: `HerculesModelsTest`, extended `HerculesRulesTest`, `JudgmentRulesTest`, `JudgmentSourceScanTest`, `HerculesShapesTest`; self-test `hercules.master`; the five skills' self-tests read the wheel through the master toggle.

### REPLACED
- Nothing removed. The Hercules passives are no longer separate wheel entries when the master toggle is on (setting `masterToggle` brings them back); the skill descriptions mention the switch.

### KEPT
- Everything of 3.19.0: the 2D sprites of the Hercules effects (used when `visual.herculesModels` is off, far away, at minimum particles or out of budget), the toggle bits and old saves, the Time Magic VFX (untouched).

## [3.19.0] — Hercules, Lord of Martial Prowess (the fifth and last Veldanava Ultimate skill)

### NEW
- **Hercules, Lord of Martial Prowess** (owner: "Solomon and Hercules as actual skills come next ... yes make them"), ten modes from the owner's spec, taken from the Will of the Creator screen (the card is open now) or `/veldanava skill hercules`:
  - **Passives (toggles)**: Adamantine Body (90% less physical, 50% less magic damage; immune to knockback, fire, fall damage and Temporal Stutter), God of Strength (melee x3, x4.5 mastered; blocks up to obsidian break in one hit; a blow's shockwave hurts the enemies around the one struck), Unstoppable Momentum (more than 3 s of movement gives three stages of attack speed, move speed and armour pierce), Colossal Vigor (out-of-combat regeneration, melee blows heal in combat).
  - **Actives**: Earth-Shattering Tremor (leap, and the landing shatters the ground and unbalances enemies within 14 blocks), Invictus (crouch + press; 10 s with a health floor of 1 and melee that grows as health falls), Titan's Grip (pulls the enemy in sight, a flying boss too, into melee range and locks it), Devastating Throw (crouch + press; hurls an enemy or a chunk of ground along your look), Battle Roar (cleanses you, Fear and Temporal Stutter on enemies within 30 blocks).
  - **Mastery**: World-Breaking Strike (15K MP, 180 s): one punch that shatters Michael's Castle Guard, Uriel's prison, a Seraphim's guard, other players' pockets and the enemy's fields, breaks armour (new effect **Shattered Armour**), lays a heavy Temporal Debt and hits up to eight enemies.
  - Costs and cooldowns by mastery like the other four skills (see `docs/veldanava_skills.md`).
- **Hercules VFX** (`HerculesLayer`, ten new shapes, 17 textures): golden fists and laurels, cracking ground, shock rings, chains and plates of adamantine.
- **Three server settings** in `nusmp-veldanava-skills-server.toml`, each restoring the plain behaviour: `herculesTerrainDamage` (tremor, throw crater and lifted chunk remove blocks), `herculesBlockBreak` (one-hit mining), `herculesShockwave` (a heavy blow hurts the enemies around).
- **Shattered Armour** effect (`nusmp:armor_break`): armour and toughness fall to nothing.
- Tests: `HerculesRulesTest`, `HerculesShapesTest`, the extended `JudgmentRulesTest` and `JudgmentSourceScanTest`, and the smoke prefix `hercules` (`hercules.rules`, `hercules.skill`, `hercules.conditions`).

### REPLACED
- Nothing. (The Hercules card was sealed; it is open now. The old "Hercules is sealed" test lines were changed to match, as all five cards are open.)

### KEPT
- Everything of 3.18.0 and earlier: the other four skills, their numbers, the animated icons (default ANIMATED), and the Time Magic VFX (untouched).

### Cost (CLAUDE.md rule 11)
- Per press at most one box query (landing, roar, strike, throw's blast) and one effect packet; Throw: one field of at most 9 pulses, at most 8 creatures on the way, 12 in the blast, 60 blocks; Tremor at most 24 creatures and 240 blocks; Strike at most 8 creatures. Per second per owner: two attribute checks and one heal, no box query. Memory: three small UUID maps. No entity spawns, no chunk loads. Full list in `docs/veldanava_skills.md`.

### Touched shared files
- `VeldanavaBoss` gains `shatterSpatial()` (drops the Seraphim guard and ends Uriel's prison); `JudgmentEffects` gains Shattered Armour; `JudgmentConfig` gains three keys; `JudgmentRules` lists Hercules; `VfxShape` is appended to.

### Not included
- Not played in a real game. The client does not predict one-hit mining (the server decides). The leap's tremor fires on a landing that raises a fall event (a landing in water wastes it). The Strike shatters only the barriers this mod defines. See the honest limits in `docs/veldanava_skills.md`.

## [3.18.0] — Solomon, Lord of Sorcery (the fourth Veldanava Ultimate skill)

### NEW
- **Solomon, Lord of Sorcery** (owner: "Solomon and Hercules as actual skills come next ... yes make them"), ten modes from the owner's spec, taken from the Will of the Creator screen (the card is open now) or `/veldanava skill solomon`:
  - **Passives (toggles)**: Infinite Grimoire (+100% maximum Magicule, +200% regeneration, half chant, immune to anti-magic and mana drain; mastered: +150% / +300%, 35% chant, every spell half price), Omnipotent Magic (your magic blows ignore resistances, foresight of enemy magic, presence sense), Mana Skin (a lethal blow is paid in Magicule, 1 HP = 10 MP), Elemental Sovereignty (fire, lava, poison and freezing cannot hurt you, elemental projectiles are absorbed into Magicule).
  - **Actives**: Grand Spell Copy (copy the last grimoire spell cast near you, then cast it once, free), Arcane Convergence (draw in projectiles, clouds and fields within 30 blocks and turn them into Magicule), Realm of Magic (held: allies cast for nothing with a one-tick chant, enemies are silenced and locked), Spell Chain (your last three grimoire spells at once, free), Mana Zone Erasure (delete enemy projectiles, clouds, fields and summons within 40 blocks, the casters stutter).
  - **Mastery**: The 72 Pillars (80K MP, 240 s): 72 pillars of the eight elements fall on up to eight marked enemies, true damage that ignores Castle Guard, and break the ground.
  - Costs and cooldowns by mastery like the other four skills (see `docs/veldanava_skills.md`).
- **Solomon VFX** (`SolomonLayer`, nine new shapes, 17 textures): grimoires with turning pages, hexagrams, nested magic circles, linked chains and elemental pillars in violet, gold and emerald.
- **Three server settings** in `nusmp-veldanava-skills-server.toml`, each restoring the plain behaviour: `solomonTerrainDamage` (pillars dig craters), `solomonSpellReplay` (copy and chain), `solomonMagicBypass` (Omnipotent Magic ignores resistances).
- Tests: `SolomonRulesTest`, `SolomonShapesTest`, the extended `JudgmentRulesTest` and `JudgmentSourceScanTest`, and the smoke prefix `solomon` (`solomon.rules`, `solomon.skill`, `solomon.conditions`).

### REPLACED
- Nothing. (The Solomon card was sealed; it is open now.)

### KEPT
- Everything of 3.17.0 and earlier: the other four skills, their numbers, the animated icons (default ANIMATED), and the Time Magic VFX (untouched). Hercules stays sealed until 3.19.0.

### Cost (CLAUDE.md rule 11)
- Per press one or two box queries (at most 96 things touched) and one effect packet; the 72 Pillars: one field, 72 impacts over about 6 seconds, at most 12 creatures and 10 blocks per impact and 600 blocks per cast, loaded chunks only, no entity spawns. Per second per owner: one 24-block box query (8 marked); the Realm one 40-block box query (24 touched). Memory: a 64-line cast log. Full list in `docs/veldanava_skills.md`.

### Touched shared files
- The grimoire's chant and price pass through `SolomonHooks` (a first-line return for everyone else), the grimoire cast logs one line, and seven drain points skip a wearer of Infinite Grimoire.

### Not included
- Not played in a real game. Only grimoire pages are copied or chained. See the honest limits in `docs/veldanava_skills.md`.

## [3.17.0] — The five Ultimate skill icons, remade and animated

### NEW
- **Animated, painted icons for all five Ultimate skills** (owner request: "Remake all the Ultimate skills icons to be animated and look 10x better"). Each is a round medallion drawn from scratch in code (`tools/gen_<id>_icon_anim.py`, shared helper `tools/icon_anim_lib.py`): 32 frames, a 3.2 second seamless loop, five to seven layers moving at different speeds, bevelled gold or bronze rims, bloom and light sweeps.
  - **Sandalphon**: midnight navy and white-gold; a blade of light lowers into a balance with shock rings, black cracks open and heal, four clock rings turn the other way round each other.
  - **Israfil**: indigo and cyan; a lit brass trumpet over five broken maze rings that turn at different speeds so the gaps line up and part, sound-wave arcs pulse outward.
  - **Jibril**: the dawn palette of the 3.16.0 VFX; layered wings, a counter-rotating double halo with tiny orbiting feathers, golden scales that tilt and settle, falling petals, an iridescent rim (pink, gold, aqua, violet).
  - **Solomon** (new art, there was no icon and no spec): violet medallion, turning rune ring, an interlaced gold and emerald hexagram, an open grimoire under a glowing orb, seven orbiting spell lights.
  - **Hercules** (new art, same): a bronze and gold fist in a laurel wreath between two pillars, a power pulse with shock rings, rising embers, heat shimmer, twelve rim studs that light in order.
- **Where they animate**: in every ManasCore skill list (the animation is registered at the skill's own icon path, so the skills and ManasCore are untouched) and in the five cards of the Will of the Creator screen, which now show the icon at 48 px (128 px source, smooth) inside the old ring; sealed cards show it dimmed.
- **New client setting** `icons.ultimateStyle` in `nusmp-icons-client.toml`: `ANIMATED` (default), `STILL` (the new art as one frame, nothing ticks) or `CLASSIC` (the old 32 px sprites; Solomon and Hercules never had one, so they show the still). It only changes looks, so it is a CLIENT config; the server never reads it.
- New files per icon under `textures/skill/veldanava/`: `<id>_anim.png` (32 x 1024 strip), `<id>_anim_hd.png` (128 x 4096), two `.mcmeta` (frametime 2), and `<id>.png` is now the still hero frame.
- Code (client only): `IconStrip` (pure arithmetic), `AnimatedIconTexture` (a dynamic texture that swaps a frame as the game ticks), `AnimatedIcons` (registration, once-a-second check, card screen), `AnimatedIconOptions`. Test `AnimatedIconsTest` (10 checks: strip geometry, frame loop, mcmeta, five icons with matching frame counts, the still is a frame of the strip, no colour under transparent pixels, clean corners, seamless loop, client-only and allocation-free tick); `PerfSourceScanTest` lists the two classes.

### REPLACED
- The art of `sandalphon.png`, `israfil.png` and `jibril.png` (owner request). The old sprites are kept as `<id>_classic.png` and are what `CLASSIC` shows.
- The small plain ring behind each card of the Will of the Creator screen is now a larger ring around the icon.

### KEPT
- Everything else: all skills, modes, numbers, settings, the Jibril 3.16.0 VFX, and the Time Magic VFX (untouched). The generator scripts of the old icons (`gen_sandalphon_icon.py`, `gen_israfil_icon.py`, `gen_jibril_icon.py`) stay.

### Cost (CLAUDE.md rule 11)
- Server: none, no packet, nothing read by the server.
- Client per tick: five integer divisions; every second tick a 32 x 32 copy and upload per icon (4 KB each, 10 uploads a second at most). Nothing is allocated per tick; locations are built once. While the card screen is open five more 128 x 128 uploads (64 KB) every second tick; those textures are released when it closes. A once-a-second check (five map lookups) puts the animation back after a resource reload.
- Memory: about 5 x (4 KB strip x 32) for the small strips and 10 MB of decoded strips while the card screen is open.

### Checked
- `AnimatedIconsTest`, `PerfSourceScanTest`, typecheck of the new client files, the whole sweep, CI build and server smoke test (nothing here is on the server; the server boots the same).

### Not included
- Nothing was seen in a real game: how the animated texture behaves under ManasCore's own blit, and whether a resource reload leaves the animation in place, are checked by reading the stubs only. The 32 px versions lose fine detail (Israfil's maze turns to blue noise; the Jibril wings read as one soft mass); Solomon's and Hercules' designs are my own, with no wiki spec. Hercules' laurel is olive-bronze.
- Solomon and Hercules the skills (3.18.0, 3.19.0).

### Decisions for the owner
- Keep the animation default on (`ANIMATED`)?
- Solomon and Hercules icons: happy with the designs (hexagram and grimoire; fist and laurel), or something else?

## [3.16.0] — Jibril's new look: the "dawn" VFX, about five times richer

### NEW
- **A new look for all eight Jibril effects** (owner request: "make Jibril VFX look slightly different than Sandalphon VFX, also upgrade the VFX by 5x"). Sandalphon is cold white-gold with black cracks, clock rings, a hexagram and a cross; Jibril is now **dawn**: a pearl-white core, rose-gold and amber warmth, azure-to-lavender shadow and an iridescent rim that cycles pink, gold, aqua and violet. Motifs: layered feather wings, nested halos with orbiting feathers, law banners, ribbons and unrolling scrolls of script, an open book under a crown of rays, a braided pillar with falling petals. Nothing of Sandalphon's cracks, clocks, hexagram, steel grey or cross, no scales-and-sword emblem, and none of Israfil's cyan maze.
- **"5x" is meant as richness, not quads** (the engine's cap of 100 quads per effect is the shared performance budget, rule 11): every effect now plays at least five staged layers (anticipation, strike or lock, resolve, afterglow), the big textures (glyph, seal, wing) are 1024 px and the rest 2-4x larger, and the motion is richer (scrolling shimmer, staggered feather and petal fall, easing, flat and camera-facing layers mixed). Quads about doubled: worst frame Command 73, Judgment 69, Purge 60, Shackles 70, Word 59, Reflect 58, Kingdom 41 and Wings 45 (82 and 90 for the 20 ticks a re-send overlaps), all under the cap of 100.
- Six new textures (`jibril_scroll`, `_emblem`, `_petal`, `_ribbon`, `_spiral`, `_prism`; 22 in all) and three new preview scenes (`jibril_command_close`, `_word_close`, `_reflect_close`).

### REPLACED
- The 3.15.0 white-gold look of the eight Jibril effects: the layer code `JibrilLayer` is rewritten and the 16 `jibril_*.png` textures are redrawn under the same names (owner request). Nothing else changes: the eight `JIBRIL_*` shapes, their packets and every number of the skill are the same, so the server and the network are untouched.

### KEPT
- Everything of 3.15.0 (all ten modes, the numbers, the settings, the icon), Sandalphon's and Israfil's effects, and the Time Magic VFX (untouched).

### Cost
- Client only. No new packet, no server work, nothing kept between frames, no per-frame collection allocation; repeated elements thin through `ctx.seg` and decorative extras are distance-culled. The Kingdom and the wings ask for a vertex budget of 200 (50 quads each) so two overlapping re-sends stay at 100. The texture set is about 20 MB of video memory.

### Checked
- `JibrilShapesTest` (updated to the new budgets, six new textures used, nothing borrowed from Sandalphon or the Trial layer), `TrialShapesTest`, `PerfSourceScanTest` and the particle / texture tests; the full sweep against the clean 3.15.0 tree (see the report). Previews, including a Jibril-next-to-Sandalphon sheet, were sent to the owner.

### Not included
- The skill icon (`jibril.png`) still shows scales of law; redraw it in the dawn palette? Nothing was seen in a real game: the preview has no depth against blocks, no lighting and no post-processing, and it cannot show how the wings sit behind a real player or which way the Word of Law glyph faces. Word's middle frames are the brightest moment, the Kingdom curtains read a little faint, and the Command lock flash saturates for about one tick.
- Solomon and Hercules (3.17.0, 3.18.0).

### Decisions for the owner
- Redraw the skill icon in the dawn palette?
- Purge, Word and Reflect use 58-60 quads of the 100 available; push them closer to 90?

## [3.15.0] — Jibril, Lord of Rigor

### NEW
- **Jibril, Lord of Rigor** (ultimate skill, 10 modes). The card on the Will of the Creator screen is now open (Solomon and Hercules stay sealed). Four toggle passives, five actives and one mastery-only held active; the cooldowns fall with mastery like every Veldanava skill (`cooldownScale` scales them).
  - **Divine Rigor** (toggle): no harmful status effect lands on you and the ones you carry are lifted (8 a second). Mastered: allies within 16 blocks get half of it, so debuffs that land on them last half as long.
  - **Heavenly Presence** (toggle): once a second up to 12 demons, undead and creatures carrying Temporal Debt within 16 blocks take holy damage (14 + 0.5 per stack of debt, debt part capped at 20).
  - **Strict Governance** (toggle): the skill and spell cooldowns of enemy players within 30 blocks (the 16 nearest) run at half speed. New server setting `strictGovernance`.
  - **Wings of Revelation** (toggle): creative-style flight at a faster speed, and a 70% chance (85% mastered) to dodge a blow taken in the air. New server setting `wingsFlight`.
  - **Absolute Command** (5.0K / 3.5K MP, 25 / 15 s): the creature in sight (40 blocks) is halted (Action Lock, Event Silence, and for a player the hotbar is held on its slot), for the time the Balance Law allows.
  - **Judgment of Heaven** (crouch + press, 8.5K / 6.0K MP, 30 / 20 s): a pillar of light on the creature in sight (64 blocks): 220 holy damage, its good effects stripped, half the damage to up to 6 creatures within 3 blocks.
  - **Heavenly Purge** (7.0K / 5.0K MP, 40 / 25 s): within 50 blocks lingering clouds (at most 64), other casters' Veldanava fields and pocket mazes are cleared; you and up to 24 allied players are cleansed of harmful effects. It never touches a block.
  - **Holy Shackles** (crouch + press, 4.5K / 3.0K MP, 20 / 12 s): the creature in sight (40 blocks) is bound (Movement Interference) and up to 8 of its good effects are taken from it and given to you (at most 5 minutes each).
  - **Word of Law** (6.0K / 4.0K MP, 18 / 10 s): a parry window of 0.6 s (1 s mastered): the first blow from a living attacker is negated and comes back at 300%.
  - **Kingdom of God** (mastery only, held, 4.0K / 2.5K MP per second, 90 / 60 s): a sanctuary of 50 blocks round you for up to one minute: you and your allies cannot be reduced below 1 HP, and once a second up to 24 enemies lose 4 good effects and are crushed (Gravity Crush, 1.5 s).
- **Jibril VFX** (our own, drawn from scratch: white and warm gold, pale sky-blue shadow; halos, law glyphs, chains of light, feathers, wings, a pillar from the sky): eight new shapes `JIBRIL_*` appended at the end of `VfxShape`, one new layer `JibrilLayer`, 16 textures made by `tools/gen_jibril_vfx_textures.py`. Previews were sent to the owner. The Time Magic VFX is untouched.
- Tests: new `JibrilRulesTest`; `JudgmentSourceScanTest` extended (Jibril's caps, one tick hook, clean-up on death / logout / login / stop, settings, assets, lang, smoke prefix); `JibrilShapesTest`; smoke prefix `jibril` (`jibril.rules`, `jibril.skill`, `jibril.conditions`).

### REPLACED
- Nothing. The Jibril card was sealed in 3.14.0; it is open now. The tests that used Jibril as the example of a sealed card now use Solomon.

### KEPT
- Sandalphon and Israfil, the pocket labyrinth, the card screen, the boons, the loot, the orb, every setting (the file only gains `strictGovernance` and `wingsFlight`). The Time Magic VFX is untouched.

### Cost
- Per press: Absolute Command 1 line query; Judgment of Heaven 1 line query + 1 box query, 7 hits; Heavenly Purge 2 box queries (at most 64 clouds removed, 24 players cleansed) and a field sweep; Holy Shackles 1 line query, 8 effects moved; Word of Law no query. Every active is on a cooldown of 10 s or more.
- Per second per owner: one 30-block box query for Presence and Governance together (12 creatures burned, 16 players' cooldowns walked: 24 skills x 16 modes at most), 8 effects cleansed at most, one ability refresh for the wings. The Kingdom: one field, one box query a second, 24 enemies touched at most.
- Per tick: the existing field manager and one hotbar hold that returns on its first line when nobody is commanded (64 players at most, one small packet to that player only, and only when they moved the slot).
- Network: one effect packet per press; Kingdom and wings one every 80 ticks (nearby players only, through the effect budget).
- Client draw: at most 44 quads per effect (Command 35, Judgment 44, Purge 29, Shackles 35, Word 17, Reflect 23, Kingdom 24, Wings 19; engine cap 100), through the existing distance cull and LOD.

### Checked
- Full sweep of every test class that reads the repo's files against the clean 3.14.0 tree, and typecheck of the new and edited files against the game stubs (see the report). The sweep ran against type stubs, not the real game classes, so the real run is the CI build and the `jibril` smoke test.

### Not included
- Solomon and Hercules (3.16.0, 3.17.0). Nothing was played in a real game: the flight, the dodge chance, the hotbar hold, the parry window, the sanctuary floor and every visual are checked only by tests and the preview harness. ManasCore's cooldown units are assumed to be seconds counted down once a second (taken from the Israfil code); if a game shows otherwise, Strict Governance stretches the wrong amount.

### Decisions for the owner
- Divine Rigor blocks **every** harmful effect, including the Veldanava ones (Temporal Debt and the Judgment effects too). Too strong against another Veldanava holder?
- The ally share of a mastered Divine Rigor is half-length debuffs, not a chance. Fine?
- Strict Governance only slows **players** (not mobs), and +1 s every other second. Wings dodge only direct, explosion and projectile hits, never true damage.
- The Kingdom's 1 HP floor does not stop `/kill` or the void.

## [3.14.0] — Israfil, Lord of Trials; cooldowns by mastery; the shared skill base

### NEW
- **Israfil, Lord of Trials** (ultimate skill, 10 modes): four toggle passives (Spatial Domination, Trial of Endurance, Labyrinthian Mind, Weight of Consequence), five actives (Dimensional Labyrinth 6.5K/4.8K MP, Weight of Sins 4.0K/2.9K, Spatial Collapse 9.5K/7.2K, Coordinate Shift 3.0K/1.5K, Rift Walk 1.2K/0.8K per second held) and the mastery-only **The Grand Trumpet** (60K MP, 180 s). The card is now open on the Will of the Creator screen. Full numbers in `docs/veldanava_skills.md`.
- **Dimensional Labyrinth is a pocket dimension** (owner request: "make it into an ever-shifting pocket dimension"). The creature in your sight (a boss excepted) is pulled out of the world into the new dimension `nusmp:trial`: a sealed maze of black stone, 9 x 9 cells (11 x 11 mastered), dimly lit from the floor. **The walls never stand still**: every 3 s (1.8 s mastered) one closed wall opens and one wall on the new loop shuts, after a 0.8 s flash between the two, and the maze always stays one connected maze with a single way between two places, so nobody is ever sealed in. A crying-obsidian exit waits in it and moves every third shift; reaching it lets the creature out at once. A player stays 20 s (30 s mastered), a mob 30 s (45 s), takes 1% of its maximum health a second and +5 Temporal Debt; mastered, up to two creatures next to the target are pulled in too. Everyone is put back exactly where they stood when it ends or when the caster dies or leaves; the Grand Trumpet breaks the labyrinth of other casters. New server setting `pocketDimension` (default on).
- **Two conditions** shown as potion icons: **Gravity Crush** (falls fast, cannot fly) and **Nihility Collapse** (slower, no healing). *The "Nihility Collapse item" is built as the damage field and condition of Spatial Collapse; the owner's file names no separate item.*
- **Trial VFX** (our own, drawn from scratch; six new shapes `TRIAL_*` appended at the end of `VfxShape`, one new layer, 18 textures): labyrinth, gravity plate, collapse, coordinate shift, trumpet and rift. Previews were sent to the owner.
- **Cooldowns for every active, falling with mastery** (owner request). Sandalphon (normal / mastered): Bullet 6/3 s, Condemnation 20/12, Edict 30/20, Cleave 8/4, Verdict 40/25; Final Judgment stays 120 s. New server setting `cooldownScale` (default 1.0, 0 = none) scales every Veldanava cooldown.
- **`VeldanavaSkillBase`**: the shared shape of all five skills (mode table, toggles, crouch rule, cost and refund, cooldown, mastery gate). The player mask now carries the skill's code, so one skill's toggles never switch another skill's passives on. `JudgmentFields` takes a pluggable behaviour per field.
- Tests: `JudgmentRulesTest` and `JudgmentSourceScanTest` extended, new `PocketMazeTest` (a maze stays perfect through 2,000 shifts, walls that may not close never close, the blocks match the walls) and `PocketSourceScanTest`, smoke prefix `israfil` (`israfil.rules`, `.skill`, `.conditions`, `.pocket`: a mastered maze is painted in the real dimension, shifted 30 times block by block and taken down).

### REPLACED
- **Dimensional Labyrinth** (first design in this release: a virtual cage of radius 4.5 round the target, nothing built) is replaced by the ever-shifting pocket dimension. The cage was only ever in the unreleased builds of 3.14.0.
- Sandalphon's five actives had no cooldown; they now have one that falls with mastery (owner request).
- `executePlayers` now defaults to **true** (Final Judgment may execute players; `executeBosses` stays false). Only fresh `nusmp-veldanava-skills-server.toml` files get the new default; an existing file keeps its value.

### KEPT
- The old cage (`IsrafilSkill.Cage`): it still cages a boss (a boss is never pulled into the pocket), and it is what the skill does with `pocketDimension = false` or when a data pack removes the `nusmp:trial` dimension.
- Everything else of 3.13.0: Sandalphon's numbers, conditions and VFX, the card screen, the boons, the loot, the orb. The Time Magic VFX is untouched.

### Cost
- Per press: at most one box query and 16-48 creatures (collapse 16, trumpet 48), every active on a cooldown of 5 s or more; Rift Walk one session per walker (4 s, 8 s mastered), a Magicule charge per walker per second.
- Pocket maze: at most 8 mazes on a server, one per caster, 3 creatures each. Opening paints at most 45 x 45 x 6 blocks once (about 12,000 block reads, 6,500 writes without neighbour updates) and forces at most 9 chunks; closing does the reverse and unforces them. While it stands: one wall moves every 1.8-3 s (24 block writes, 2 effect packets), members are looked at 5 times a second, hurt once a second, and the caster gets one action-bar line a second. No pocket open = one empty-map check per tick.
- Per tick: the field manager (24 fields at most, now with Israfil's cage and Nihility field) and the Rift Walk manager, both returning on their first line when idle.
- Network: one effect packet per press (nearby players only, through the effect budget); Rift Walk one small packet a second per walker.
- Client draw: at most 62 quads per effect (labyrinth 43, gravity 31, collapse 35, shift 35, trumpet 62, rift 27; engine cap 100), through the existing distance cull and LOD.

### Checked
- Full sweep of every test class that reads the repo's files, against the clean 3.13.0 tree: the same stub-only failures on both sides and nothing new (one extra, `MechanicsDataTest`, is a class-loading artefact of the sweep and passes alone). New and extended: `PocketMazeTest`, `PocketSourceScanTest`, `JudgmentRulesTest`, `JudgmentSourceScanTest`, `TrialShapesTest` (Veldanava + pocket: 44 pass). Typecheck of the skills package against the game stubs. The sweep ran against type stubs, not the real game classes, so the real run is the CI build and the `israfil` smoke test.

### Not included
- Jibril, Solomon, Hercules (3.15.0, 3.16.0, 3.17.0). A separate Nihility Collapse item (none in the owner's file). Breaking real barrier blocks with the Trumpet (it ends other Veldanava fields and other casters' labyrinths). A hard lock on the pocket: a trapped creature with a teleport skill that does not go through the game's teleport event can still leave it by its own means. A real-game walk through the maze, the visuals and Rift Walk with two clients (previews and the CI server test only).

### Decisions for the owner
- The pocket's numbers: a player is held 20 s (30 s mastered), a wall moves every 3 s (1.8 s), the maze is 9 x 9 (11 x 11). Longer, shorter, faster?
- Is the Nihility Collapse "item" the field and condition built here, or did you mean an item you can hold?
- The Grand Trumpet's "reset enemy cooldowns to maximum" is capped at 60 s so nobody is locked for long. Raise or lower?

## [3.13.0] — Sandalphon, Lord of Judgment: the first Veldanava ultimate skill

### NEW
- **Sandalphon, Lord of Judgment** (ultimate skill, 10 modes): four toggle passives (Executioner's Authority, Absolute Severance, Inevitable Demise, Judgment Gaze), five actives (Judgment Bullet, Condemnation, Supreme Edict, Guillotine Cleave, Verdict of Ruin) and the mastery-only held active **Final Judgment** (45K MP, 120 s cooldown, detonates the Temporal Debt x5). Costs are 2.5K/1.8K, 4.2K/3.1K, 5.5K/4.0K, 3.8K/2.5K and 8.0K/5.5K MP (normal/mastered). Full numbers in `docs/veldanava_skills.md`.
- **Six conditions** shown as potion icons: Temporal Stutter, Temporal Debt (stacks, +1% magic damage taken per stack up to +50%), Event Silence, Future Rejection, Movement Interference, Action Lock.
- **The reward card screen:** the Will of the Creator now offers five ultimate skills on a card screen (pick one, once in a player's life, validated on the server). Sandalphon is open; Israfil, Jibril, Solomon and Hercules are shown sealed and open one per release. Chat/command fallback: `/veldanava skill <id>`.
- **Judgment VFX** (our own, drawn from scratch; six new shapes `JUDGMENT_*` appended at the end of `VfxShape`, one new layer, 14 textures): bullet, sigil, edict, cleave, field and the Final Judgment cross. Worst case 17-78 quads per effect (engine cap 100). Previews were sent to the owner.
- **Server settings** in `nusmp-veldanava-skills-server.toml`: `enabled`, `rewardScreen`, `costScale`, `debtDamage`, `finalJudgmentCap`, `activeDamageCap`, `executePlayers` (off), `executeBosses` (off), `debtMax`, `fieldsPerPlayer`, `fieldsTotal`.
- Tests: `JudgmentRulesTest`, `JudgmentSourceScanTest`, smoke prefix `sandalphon` (`sandalphon.rules`, `.skill`, `.conditions`), and the `veldanava.orb` test rewritten for skills.

### REPLACED
- What the Will of the Creator offers: the three attribute boons in chat become the five-card skill screen (behind `rewardScreen`; set it to `false` for the 3.11 offer).

### KEPT
- The three attribute boons (code, tags, command, tests) - no longer offered by default, still working. The loot, the orb, the boss. The Time Magic VFX is untouched.

### Cost
- Per press: one box query, at most 8 / 32 / 12 creatures (bullet / edict / cleave), at most 2.5 presses a second per player, one effect packet each (nearby players only, through the effect budget).
- Client draw: at most 78 quads (73 edict, 55 cross, 40 field, 27 bullet, 17 sigil) per effect, through the existing distance cull and LOD.
- Per tick: the field manager only, which does nothing when no field exists; at most 24 fields on a server, 12 box queries and 288 hits a second worst case. Two passive scans per player per second (12 and 16 targets).

### Checked
- Unit and source-scan tests (new: 25; Veldanava: 28; sweep of 137 repo-reading test classes against a clean HEAD: no new failure). The sweep ran against type stubs, not the real game classes, so the real run is the CI build and the `sandalphon` smoke test.

### Also fixed (found by CI)
- `GeoAssetsTest` failed on the unwired Phase E preview assets of 3.12.x work: the Hell Hound's `lod1`, `lod2` and `fx` models had no texture of their own (copies of `anime_hound.png` and its glow were added) and the Goliath glow map was half the size of its texture (scaled up 2x, nearest). Nothing is wired to them yet; the mob remake is still waiting for the owner's style notes.

### Not included
- The other four skills (one per release), the Nihility Collapse item (with Israfil), aim assist for the bullet, Mana Zone cancelling by the Edict, a lock on hotbar swapping, a real-game check of the visuals (previews only).

### Decisions for the owner
- `executePlayers` and `executeBosses` default to off (a player or a boss takes a limited hit instead of dying outright). Say if Condemnation should kill them.
- Players who already took a 3.11 boon can still pick one skill.
- Only Final Judgment has a cooldown; the other actives have an 8-tick gap and cost Magicules.

## [3.12.1] — The head/feet self-test is counted by the CI smoke check

### FIXED
- The smoke self-test of 3.12.0 was registered as the bare prefix `relic.headfeet`; the CI check counts keys that start with `relic.headfeet.`, so it reported "registry is not wired" for it (the test itself ran and passed its data check). It is now `relic.headfeet.slots`. The same pin is in `HeadFeetWiringTest` (the key must have a part after the prefix and the prefix must be in the smoke list).
- Nothing else changes: no gameplay, no setting, no asset.

### KEPT
- Everything in 3.12.0, unchanged.

### Cost
- None.

### Not included
- The other 118 entries of the smoke list with a bare name still report the same wiring FAIL (they were not renamed; this release only touches the head/feet test). The smoke step also pipes through `tee` without `pipefail`, so a FAIL row does not turn the job red; that is left as it is until the owner decides.
- This commit also asks for the CI client run (`[clientshot]`), which casts the five numbered undead on the fixed code of 3.10.2 and logs what the real client sees.

## [3.12.0] — Head and feet relics move to the Curios slots

### NEW
- **Head and feet relics are worn in the Curios slots** (owner request: "move them", because the armour-slot ones never showed on the body). All 115 head and 49 feet relics (the Mask of Mind Melting, the Ruby Boots of Planestriding, ...) now go to **Head Relic** (`relic_head`) and **Feet Relic** (`relic_feet`), which 3.10.0 added, and are drawn by the same worn layer as neck and back relics (Curios eye icon: off = not drawn).
  - **Right-click** puts the relic in the free Curios place (sound and an action-bar line). Sneak + right-click is still the attunement ritual. If the place is taken, nothing is put on (the action bar says so; it is not worn as armour behind your back). Without Curios the old behaviour runs.
  - **A relic found in the HEAD / FEET armour slot** (a click, a shift-click, a dispenser, an old save) is moved to the free Curios place within a second. If the place is taken or Curios is missing it stays and keeps working; the try is repeated every 10 seconds.
  - **Nothing is lost by moving:** a Curios-worn head or feet relic adds the armour points and toughness its armour form had, as ordinary attribute modifiers.
  - The client no longer predicts the vanilla armour swap for these relics (the server decides: Curios slot, or armour as the fallback), so its armour slot cannot show a piece the server never gave.
  - The tooltip's slot line now says "Slot: Head (Curios: Head Relic slot)".
- **Server setting** `relics.headFeetInCurios` in `nusmp-relicpower-server.toml` (on by default). Off restores the 3.11 behaviour exactly.
- Tests: `HeadFeetMoveRulesTest`, `HeadFeetWiringTest`, and the smoke self-test `relic.headfeet` (right-click equip, the move out of the armour slot, a taken place, the kept armour points, for a real head relic and a real feet relic; the smoke list now expects the prefix). Doc: `docs/relic_head_feet_slots.md`.

### REPLACED
- The right-click of a head or feet relic: it used to put the relic on as armour; with the setting on it goes to the Curios slot instead (the armour path stays as the fallback and as the setting-off behaviour).

### KEPT
- Every relic item, armour material, painted layer, model and texture; the Curios slots, tags and the worn layer of 3.10.0; chest relics as armour; named mobs wearing relics in their armour slots; the Time Magic VFX; every other item, model, page and setting.

### Cost
- Once a second per player, inside the relic pass that already runs: two armour-slot reads and an instance check; the Curios inventory is read only when a head / feet relic is actually in an armour slot, and a try that found no place backs off for 200 ticks. At most 4 attribute modifiers per player (cap 8). No scan of entities, chunks or blocks, no packet of its own. Full table in `docs/relic_head_feet_slots.md`.

### Not included
- Not seen in a running game. The draw of a Curios-worn head or feet relic is the 3.10.0 worn-layer path; it is covered by tests and the CI server smoke test, not by eye.
- The armour-slot display bug itself (the CI client run found the slot empty on the client a few seconds after the server set it) was not diagnosed; this release moves the relics out of that slot instead.
- Named mobs still wear relics in their armour slots (they have no Curios inventory). Chest relics stay armour.

## [3.11.0] — Veldanava, the Creator God

### NEW
- **Veldanava, the Creator God** (boss, one entity with three bodies; owner request from the three reference images and the loot guide). Spawn with `/multiverse boss veldanava` (operators) or the **Veldanava Spawn Egg**; one per world.
  - **Star King Dragon** (7.5 x 9 hitbox, drawn big): Stardust Sweep (a beam that turns one full circle), Gravity Pull + crush, Starfall (stars on every player), Dragon Roar, Bite. It cannot be killed: a lethal blow starts an 80-tick transformation into the Creator God with a second full health bar.
  - **Creator God** (slim swordsman): God Speed, Genesis Slash, Justice King Michael (guard, projectiles sent back), Covenant King Uriel (prison), Law Manipulation (Weight / Fragility / Decay). At 10% health the Limiter is released.
  - **Dark Avatar of God**: the same five moves, faster and harder, plus **Genesis: Creation of the World** (100-tick charge, one huge wave, leaves him exposed).
  - **Soul Seizing** (server config, on by default): in the two human bodies a blow that would kill a player puts them in spectator mode until the fight ends instead; restored on boss death, removal, reset or login.
  - Three GeoLite models with idle / walk / run / attack / cast / cast_big / hurt / transform / death clips and glow layers (`tools/gen_veldanava.py`).
- **Will of the Creator**: an orb appears when he falls and gives every fighter (up to 8) three clickable boon lines: Magic King Solomon (+24 max health, +3 toughness), Martial King Hercules (+8 attack, +10% speed) or Wisdom King Raphael (+6 armor, +0.5 attack speed, +2 luck). One boon per player ever (`hasReceivedVeldanavaBoon`); `/multiverse veldanava boon <0..3>` is what the lines run (`0` = loot only).
- **Seven drops** with icons and card text: Star King Dragon Core, Genesis Fragment (x3), Creator's Essence (full heal / clear / regeneration), Justice King Michael Fragment, Covenant King Uriel Fragment, Nihility Fragment (15% one of the three), Veldanava's Divine Core (1%).
- Server config `nusmp-veldanava-server.toml`: `enabled`, `bossHealth`, `damageScale`, `soulSeizing`, `willOfTheCreatorSeconds`.
- Tests: `VeldanavaRulesTest`, `VeldanavaBoonsTest`, `VeldanavaSourceScanTest`, and the smoke self-tests `veldanava.rules`, `veldanava.assets`, `veldanava.boss`, `veldanava.orb` (the smoke list now expects the `veldanava` prefix).

### REPLACED
- Nothing that was released. (The True Dragon model went through two drafts while building; only the second is in the jar.)

### KEPT
- Everything else is untouched: the Zogratis bosses, the dungeon, the relics, the Time Magic VFX, and every existing item, model, page and setting.

### Cost
- One boss per world, one orb afterwards. Per tick at most 24 targets hurt (the player list only, no entity or chunk scan), 12 effect packets (also under the global effect-packet budget, nearby players only), 24 blocks changed (only with `griefBlocks`, all put back), 12 falling stars per cast. Client: one pass plus a glow pass, culled beyond 220 blocks, nothing allocated per frame. Full table in `docs/veldanava.md`.

### Not included
- Not seen in a running game: the fight's feel, the renderer, Soul Seizing and the orb are covered by tests and the CI server smoke test only.
- The guide's **Aura of the Creator** and **Nodens' sword**, the Dragon Core socketing, the Genesis anvil upgrade and the Divine Core's off-hand negation are not built (their materials drop and their cards describe them). The boon choice is chat lines, not a screen.
- The head / feet armour relics (Mask of Mind Melting, Ruby Boots of Planestriding) are still open (see 3.10.2); the numbered undead fix of 3.10.2 has not been seen in a game either.

## [3.10.2] — The numbered undead can be created again

### FIXED
- **The numbered undead (Jimmy, David, Alfredo, Carl, Michael Caesal) never appeared when their spells were cast.** Found by the CI client run: creating one threw `NullPointerException: Cannot invoke NamedCorpses.ranged() because this.kind is null`. The Mob constructor calls `registerGoals()` before the entity's own `kind` field is set, so the entity could not be built and nothing was spawned. The goals are now added after `kind` is set (`NamedCorpseEntity.addGoals`). Behaviour, stats and goals are unchanged.
- The `/multiverse` worn-relic debug report no longer fails on its "layer reached" line (an enum was passed as a translatable argument).
- Dev: the CI client screenshot step no longer crashes on its crop box.

### Cost
- None: the same goals, added once per entity at construction.

### Not included
- Not seen in a game. The CI client run showed the player's head and feet armour relics arriving on the server but empty on the client a few seconds later; that cause is still open, so the armour relics are not fixed by this release.

## [3.10.1] — Grimoire Tower and Library Ruins no longer generate

### REMOVED (owner request)
- **The Grimoire Tower and the Library Ruins** no longer generate in the overworld: their two biome modifiers (`neoforge/biome_modifier/grimoire_tower.json`, `library_ruins.json`) are deleted. Nothing else is touched: the feature code, the configured/placed features, their loot tables, the tower wizard and every grimoire path stay registered, so old worlds load; only new chunks stop getting these structures. Structures already built in an existing world stay where they are (a world needs a fresh area, or the blocks removed by hand). To get them back, restore the two modifier files.

### Cost
- Less worldgen work per new chunk; nothing added.

### Not included
- Not seen in a game by the build. The Mega-Dungeon gateway and the sanctuary are different structures and are unchanged.

## [3.10.0] — Head and feet relic slots

### NEW
- **Two Curios slots for head and feet relics** (owner request: the armour-slot head and feet relics were not drawn on the body): `relic_head` ("Head Relic") and `relic_feet` ("Feet Relic"), one place each, given to the player. Every HEAD relic (115) and FEET relic (49) is in the item tag of its slot, so it can be put on in the Curios screen. A relic worn there is found by the relic scan like a neck or back relic and drawn by the same worn layer, with its Curios eye icon (off = not drawn). The armour slots work as before (nothing removed); a relic worn in both counts once. The Mask of Mind Melting and the Ruby Boots of Planestriding are among them.
- Dev: `tools/relics/gen_curios_tags.py` writes the two new tags; `-Pclientshot` / `.github/workflows/clientshot.yml` is a CI-only client run that takes screenshots of worn armour relics (inert without the property).

### Cost
- No new per-tick work: the Curios read is the one that already existed; at most one extra stack per slot; no new packets (WornSync already sends the Curios list).

### Not included
- Not seen in a game by the build; the chest relics have no slot of their own; right-click equip still puts head/feet relics on as armour.

## [3.9.1] — Wings on feet relics stay as they were

### FIXED
- **Winged feet relics** (owner request): the wings of the winged sandals (`sandals_winged`) and the Wind Walker greaves (`greaves_windwalker`) are no longer made blocky; their wing quads are copied unchanged from the classic models (`tools/relics/gen_worn_x3.py`, `wing_groups`). Only the boot or greave itself is blocky. Quads of the feet models: 15,370 -> 14,708. Cost bounds unchanged. Preview: `docs/previews/relic_x3/feet_wings_kept.png`.

## [3.9.0] — Blocky feet relics and a contact fit for worn relics

### NEW
- **Blocky feet relics** (owner request, narrowed to the feet): the boots, greaves and sandals relics (49 worn models) get a blocky, more detailed look like the Saint Valkyrie Dress: raised trim frames, panels and glow lines, about 2.3x the quads. Every relic keeps its own colours. Head, chest, capes, belts, amulets, gloves and charms are not changed. New client setting `relicfx.modelDetail` (`x3` default, `classic` = the 3.8 look); drawn only within 12 blocks and for the nearest four wearers of a frame (`UpgradeCaps.X3_*`, `X3Gate`).
- **Contact fit** (owner report: armour relics badly placed): worn models now shrink about their own middle instead of the joint of the body part, so boots stay on the feet and hems on the hips; setting `relicfx.contactFit` (default on, off = the 3.8 placement).
- **Hip charms** (owner report: Curios relics badly placed): flasks, tomes and pouches hung inside the arm; they are lowered beside the thigh (part of the contact fit).
- **Armour relic log line**: the first time an armour relic is drawn, one INFO line says how many pieces and vertices were drawn, so an invisible one can be traced from `latest.log`.
- Self-tests `relic.display39` and `relic.x3`; `tools/relics/gen_worn_x3.py`; `docs/relics/relic_x3.md`; preview `docs/previews/relic_x3/feet_before_after.png`.

### REPLACED
- The body fit of worn relics (pivot at the joint -> pivot at the middle of the model) and the place of the hip charms; both switchable back with `relicfx.contactFit = false`.
- The look of feet relics at close range (x3 models); the classic models are kept as the far / low-quality form.

### KEPT
- Every old worn model (`relic_worn`), the visual-upgrade models, capes and all Curios relics' looks, the plan, the failsafe, ids and save data.

### Cost (rule 11)
- At most 6,400 vertices per x3 model, 14,000 per wearer, four wearers per frame: 56,000 vertices per frame worst case for all x3 relics together. No packets, no server work.

### Not done / caveats
- The real cause of "armour relic invisible" is not found (no failure in the owner's log); the contact fit fixes the floating and hidden-in-arm placements that were found, and the new log line names the rest. Nothing was seen in a game; the blocky look was checked on the offline rasteriser only. The head, chest, and the rest of the Curios relics keep their 3.8 models.
- The 110 visual-upgrade models with their own skin, the armour sets of the mod and the capes were left as they were. Owner decisions: more gold frames or less; whether head and chest relics or the armour sets (robes) should get the style too.

## [3.8.0] — Runes as a passive, own-element runes, Star magic for every Star mage, Rades's undead drawn properly

### NEW
- **Mana Method access modes** (owner request): server setting `manaMethod.access` = `PASSIVE` (new default: the rune pages open on their own once the grimoire's mastery reaches `manaMethod.passiveMastery`, default 0.7, nothing to turn on), `TIMED` (a new last page, **Rune State**, switches the runes on for `manaMethod.timedSeconds` (default 480 = 8 min), then `manaMethod.timedCooldownSeconds` (default 600 = 10 min) must pass; stamps are kept in the player's data so a relog cannot reset the cooldown; it also ends on logout, death, dimension change and server stop) or `MANA_ZONE_GATE` (exactly the 3.7.0 gate). One tiny player-only packet per state change.
- **Own-element runes** (owner request, like the anime): with `manaMethod.ownElementOnly` (default on) a Fire grimoire lists and casts only the Flame Rune, Water only Water, Wind only Wind, Earth only Earth; a fused grimoire counts for both parents; other magics get no elemental rune. Air Step, Dash, Body, Brush, True and the new Rune State are for everyone. The server re-checks the cast.
- **Star Magic for every Star mage** (owner request): the cause was the server-side heir gate on six of the seven new Star pages (Quartile Scutum, Quartile Hasta, the Flagellum whip, the three Quintile pages), not the drawing. New server setting `star.vfxForAll` (default on) opens them to every Star grimoire by the ordinary mastery order. Black Neverland and the heir perks stay heir-only. New self-test `magic.star_nodes_all`.
- **Rades's numbered undead** (owner report: only circles): the dungeon player-model kit registered its old Jimmy/Alfredo/Carl renderer after the new looks and won, so the new looks are now registered inside the kit's listener, after it. If a model or texture fails to load a tinted zombie stand-in is drawn (`corpse.fallbackLook`, client) and the client log names the file; refused numbered casts and bodies that vanish within five seconds now give a reason to the player and the server log (`corpse.diagnose`); a self-test casts all five numbered pages through the real grimoire cast and checks the body exists and lives on; sun burning of the numbered undead and Soul Zombies is asserted off, and old-path raised undead are put out each sweep (`corpse.noSunBurn`).

### REPLACED
- The default access of the nine Mana Method pages: from the late-game Mana Zone gate to PASSIVE at 0.7 mastery (`access = MANA_ZONE_GATE` restores it).
- The gate of the six Star pages: from "heir" to "heir unless `star.vfxForAll`"; the Hasta bonus now asks the real heir mark. The text of five Star pages (no longer "Needs the Grinberryall heir mark").
- The Mana Method page descriptions (late-game sentence replaced, an element sentence added).

### KEPT
- Every old page, id and order (the new page is appended last), the Mana Zone / Omni Volley / Pressure Space gate, Black Neverland and the heir perks, all Rades models, textures and pages (`corpse.classicLooks` still restores the old looks), the 3.7.0 Body Rune reduction, the GUI default theme `anime`, Time Magic and its VFX.

### Cost (rule 11)
- Mana Method: idle = one `isEmpty()` check; TIMED costs one pass per second over the players in the state only; at most one 5-byte packet per state change to that player. Star: per cast unchanged (at most 10 effect packets, nearby players only, through the 160 per tick budget; stars 128, panes 8 on the server). Rades: no new entity, scan or packet; at most one log line per player per second; the zombie stand-in is under 40 quads and only appears when a model failed.

### Not done / caveats
- Nothing was seen in a game. The Rades cause (renderer order) is certain from the code, but another cause for a missing body is possible; the new log lines will name it. The new self-tests `magic.mana_method.access`, `.element`, `magic.star_nodes_all` and the numbered-cast section run for the first time in CI. Whether the client's mastery value is fresh enough when the grimoire list opens was not seen in a game. The old-path sun fire-clearing is best effort and can flicker.
- Owner decisions: `passiveMastery` 0.7 (about stage 3 of 9); should sub-attribute magics (Ice, Magma, Storm) get their parent's rune; TIMED cooldown counted from the end of the state (chosen) or the cast; a minimum mastery for TIMED; Black Neverland's dome and the 8th star for non-heirs; the Quintile pages (unverified canon) open to everyone.

## [3.7.0] — Body Rune turns away a little damage on a full hold

### NEW
- **Body Rune damage reduction** (owner decision): a FULL-hold Body Rune also turns away 15 % of each hit its wearer takes while the runes last (15 s by default; hard cap 20 %). A tap, a half hold and a cast without a measured hold give none. Void and kill damage are never touched. Server setting `manaMethod.bodyDamageReduction` (default on). The Body Rune page text says so.

### REPLACED
- The 3.6.0 Body Rune rule "gifts only, no damage reduction" (`bodyDamageReduction` off restores it).

### KEPT
- Everything else of 3.6.0: the 40 % tap price and the one-shot Brush Rune trap (the owner confirmed both), Anti-Magic cancelling True Magic, the GUI default theme `anime`, Time Magic and its VFX.

### Cost (rule 11)
- One `isEmpty()` check per damage event while no body carries runes; otherwise one map read per hit on a player. No packet, no entity, nothing allocated.

### Not done / caveats
- Not seen in a game; the extended `magic.mana_method.body` self-test is the first real run. Tensura's own damage handling may change the amount again after this event; the order was not checked in a game. The 15 % is the author's number.

## [3.6.0] — Mana Method: cost follows the charge, lightning grows with the charge, Body Rune (Gadjah) and Brush Rune (Rill)

### NEW
- **The price follows the charge**: a tap pays 40 % of the page's cost, a half hold (24 ticks) 76.8 %, a full hold 100 % (settings `manaMethod.costScalesWithCharge`, `minCostFraction` 0.40). The cooldown is not scaled, a cancelled hold costs nothing, Air Step is unscaled.
- **Lightning grows with the charge**: the True Lightning strike is 1 to 3 visual bolts, 8 to 22 blocks tall and 0.9 to 1.8 wide, with a hit and splash radius of x1.0 to x1.35 (inside the existing caps); the held lightning rune draws 1, 2 or 3 arcs. The real vanilla bolt stays off (`trueMagic.lightningRealBolt`); the new bolts reuse the existing lightning spear effect. Setting `trueMagic.lightningChargeScale`.
- **Body Rune** (page `mana_method_body`, Gadjah: the Heart Kingdom's Spirit Guardian writes runes on his own body): 10 % of the pool (at least 200), cooldown 600. Runes written on your body give temporary speed, attack, jump, step height (from the second ring tier), safe fall and knockback resistance (full-hold caps: +25 % speed, +25 % attack, +0.15 jump, +0.4 step, +5 fall, +0.4 knockback) and a full hold adds 60 ticks of slow falling. No flight and no damage shield. One session per player, 64 on the server; it ends by time (`bodySeconds`, 15), logout, death, dimension change or server stop. A chest rune (and a feet rune from the second tier) follows the body.
- **Brush Rune** (page `mana_method_brush`, Rill Boismortier: the Painting Magic user learned the Mana Method in the Heart Kingdom): 9 % (at least 180), cooldown 240. Paints an array on the face you look at within 5 blocks (floor, wall or ceiling); armed after 14 ticks; a foe stepping in fires ONE bolt and the array is spent; it never triggers on or hurts you or an ally. Radius 1.3 to 2.4 blocks and life 0.5x to 1x `brushSeconds` (30) by charge; 3 per caster, 24 on the server. Only on loaded chunks, where you may build, never in the Labyrinth or a Dungeon Guard world. A small record, not an entity and not a block.
- Both pages are appended last (fire, water, wind, earth, true, air step, dash, body, brush). Nine new server settings in `nusmp-mana-method-server.toml`, all default on: `costScalesWithCharge`, `minCostFraction`, `lightningChargeScale`, `bodyRunes`, `bodySeconds`, `brushRunes`, `brushSeconds`, `brushPerCaster`, `brushGlobal`.

### REPLACED
- The flat price (`costScalesWithCharge` off restores it) and the one-bolt fixed-radius lightning strike (`lightningChargeScale` off restores it).
- Without a setting: a refused cast no longer costs mana (the grimoire used to take the price before the page ran), and the Air Step and hold sweeps now use the game clock their stamps were written with.

### KEPT
- The seven older Mana Method pages, their ids and order; the 28-byte rune packet and the hold packet; all older config keys; the balance law; Anti-Magic still cancels True Magic by default; the Air Step and Dash fall grace; the GUI; Time Magic and its VFX.

### Cost (rule 11)
- Price: one multiplication and one map read. Lightning: at most 3 strikes a second on the server, 9 small effect packets a second through the effect budget, 72 vertices per bolt (216 for three).
- Body Rune: one session per player (64 at most), six modifiers, a sweep every 5 ticks only while a session exists, 2 packets per cast.
- Brush Rune: 3 arrays per caster (hard cap 5) and 24 on the server (hard cap 64), one small entity query per array every 4 ticks and at most 8 queries per tick, no chunk load, no block access, 2 packets per array; the fired bolt counts inside the existing rune caps. Client: at most 8 painted runes drawn at once, none beyond 56 blocks, 680 vertices for a fully written one, nothing allocated per frame.

### Not done / caveats
- Nothing in this release has been seen in a game; the four new self-tests (`magic.mana_method.cost`, `.lightning_charge`, `.body`, `.brush`) have never run before CI. The previews are software renders with a capsule for the player or foe and no world or lighting.
- A tap now costs 40 %, so four taps cost 1.6 full holds; only the unscaled cooldown limits the rate. A flying foe more than 2 blocks above a floor array does not trigger it. A wall array seen from behind can hit the wall at once. Other mods that read safe fall, step height or jump strength see the Body Rune bonus while it lasts.
- Canon came from search summaries of the English wiki pages (Gadjah, Rill Boismortier, Potrof, Mana Method, Painting Magic, chapter 226, episode 155); the wiki host could not be fetched and the Japanese wiki was not fetched. Every number is the author's. Gadjah hovering without magic is a separate ability and only a short slow fall was built.

## [3.5.0] — Mana Method: hold to charge, command filter, True Magic as physical damage, True Lightning and Plant cage, Air Step and Dash runes

### NEW
- **Hold to charge**: the Mana Method pages now follow how long you hold the cast. A tap fires a BASIC rune (power x0.5, one ring); a hold under 40 ticks is CHARGED (x0.75 to x1.6, two rings); a full hold of 40 ticks or more is FULL (x1.6, every ring, sneaking writes both runes). The rings are drawn while you hold and the early-release rune is drawn smaller. The server measures the hold per player; a small `nusmp:mana_method_hold` packet (HOLD, RELEASE, CANCEL, at most 3 per cast) tells nearby players. Settings `manaMethod.holdToCharge`, `minHoldTicks` (8), `maxHoldTicks` (40).
- **Command filter**: the rune injects its commands into the bolt as it leaves: **Pursue** (homing) and **Grow** (hit radius and damage grow with distance), scaled by the charge, with hard caps (hit radius 3.6, splash 4.8, damage x3.0). Setting `manaMethod.commandFilter`. The rune stays a small server record, not an entity (a vanilla entity would be tracked, ticked, saved and synced for every cast on a public server).
- **True Magic as physical damage**: True Magic now hits with a vanilla physical or environmental damage type credited to the caster (fire, impact, falling block, lightning; player attack for water and plant), so Tensura's magic-blocking skills do not stop it. The balance-law caps still hold. Setting `trueMagic.physicalDamage`. **Anti-Magic still cancels True Magic by default** (`trueMagic.antiMagicStillCancels`, default on); switch it off and True Magic slips past Anti-Magic, the Black Form, Physique and shields that read the damage type.
- **True Plant cage**: a cage of logs and leaves (17 cells) around the target, taken back after `blockSeconds`; the moss carpet stays as the fallback (`trueMagic.plantCage`).
- **True Lightning**: a lightning strike at the impact (`trueMagic.lightningBolt`); the real vanilla lightning bolt entity is off by default (`trueMagic.lightningRealBolt`) because it sets fires and changes mobs.
- **Two mobility pages**, appended after the five Mana Method pages, same gate: **Air Step Rune** (temporary invisible barrier blocks under your feet for `airStepSeconds` = 6, sneak to drop; 8 % cost, 240 tick cooldown) and **Dash Rune** (a velocity push along your look, also in the air, scaled by the charge; 6 % cost, 60 tick cooldown, 3 blocks of fall grace). Settings `manaMethod.mobility`, `airStepSeconds`, `dashStrength` (0.9).
- Canon check in `docs/magic/mana_method.md` (English wiki search summaries and Pixiv Encyclopedia for the Japanese side; the runes are instructions such as pursue and grow, True Magic is the real element at Third Stage or higher, Keraunos puts mana arrays under the feet and Gadjah flies by formula).

### REPLACED
- The fixed 18-tick charge (the hold replaces it; `holdToCharge` off restores it), the bolt numbers read straight from the spec (now the injected bolt state; `commandFilter` off restores them), the magic-damage True Magic hit (`physicalDamage` off restores it), the moss carpet as True Plant's block (`plantCage` off restores it).
- True blocks of a player are now removed at once on logout, death or dimension change, where before they waited for their timer (no setting).

### KEPT
- The five older Mana Method pages, their ids and order, the 28-byte rune packet, the config file and keys, the older True Magic page, the Rune Stone, the Heart kingdom perk, the Time Magic and its VFX.

### Cost (rule 11)
- At most 3 hold packets per cast and none per tick, to players within 64 blocks through the effect budget. No entity is spawned (the lightning entity only if `lightningRealBolt` is on: 0 or 1 per strike, at most 3 a second on the server).
- Blocks: cage 17 per cast, 36 per caster, 240 in the world, at most 30 s; Air Step 4 per caster, 16 sessions, 64 in the world. Idle server: two `isEmpty()` checks; the hold sweep runs every 5 ticks over holders only.
- Client: at most 6 runes, each at most 300 quads (1,200 vertices), at most 7,200 vertices a frame for six; the held rune draws fewer until fully written; nothing allocated in the layer.

### Not done / caveats
- Nothing in this release has been seen in a game; the six new `magic.mana_method` self-tests have never run before CI. The previews are software renders and cannot draw the grimoire or real terrain.
- The charge numbers (x0.5, x0.75 to x1.6, x1.6; 8 and 40 ticks) are first drafts. The damage order tap < half < full is not strictly guaranteed because the balance-law caps run after the charge scaling.
- Not verified whether Tensura's `MAGIC_RESISTANCE` applies only to magic damage types; if it applies to all types, the physical route is reduced by it too.
- The canon gives no numbers and names only Pursue and Grow; no further runes were invented. Full fandom pages could not be fetched. Runes applied to the body (Gadjah) and brush-painted runes (Rill) are not built.
- A 3.4.0 server with a 3.5.0 client sends no hold packet, so the client draws the old fixed rune. Air Step barrier blocks are visible to map or claim mods for a few seconds.

## [3.4.0] — Great Spirit GUI (skins B and C, alert crest), companion screens in the new style, Star Magic phase 2 (stars break to Anti-Magic, Black Neverland as in the manga)

### NEW
- **Great Spirit GUI** (client option `ui.theme = great_spirit`; the default stays `anime` until you switch): two skins, **B Parchment and Ink** for normal users and **C Slime Crest** for devil users (`ui.greatSpirit.skin` AUTO | B | C; AUTO = C for a forbidden or devil-bound grimoire, Devil Union active or used, or a Devil or Daemon race). Filigree corners, clover frames (three leaves = Extra, four = Unique, five = Ultimate / Anti-Magic in the Slime Crest styling), the ink-to-matrix compile animation on new text, the slime ripple on hover, and `ui.greatSpirit.quality` HIGH | LOW. There is no mana ring or gauge, by the owner's decision.
- **Alert crest**: a small crest in a corner that shows notices as `[Notice... Great Sage has...]`; the player picks where it sits on a move screen and can change it later.
- **Companion screens in the Great Spirit style** (client option `companions.screenStyle` AUTO | GREAT_SPIRIT | CLASSIC): Talk, Interact, Bond and Pouch tabs with a portrait from the companion's own skin face (breathing, blink, five moods), typed dialogue with ink-to-matrix compile, rapport meter with leaves at 3 / 10 / 25, a gift slot (click or drag), remembered likes and dislikes, stance icons, a bond-story log, a 9-slot pouch with a heal ring, and restyled speech bubbles; crest notices for rapport, tiers, gifts, joining and leaving.
- **Star Magic: stars break to Anti-Magic.** The mod's one dispel (`Nullification.shatterBarriers`: every Anti-Magic slash, the Nihility zone and about 40 other dispel arts) now rolls each star and Quartile Scutum pane of other mages inside the sphere (`star.dispelChance`, default 0.35; a pane and its end-stars about 1.7 times that; a Grinberryall heir's stars half). A survivor is left alone for 40 ticks. The caster's own and allied stars are never hit. At most 16 stars considered and 8 broken per dispel. (3.3.0's notes listed this by mistake; it was not in that jar.)

### REPLACED
- **Black Neverland** now follows the manga (ch. 381 to 383): it needs the heir mark, the Sylph bond (flag `sylph_bond`) and an anti-magic source (a Demon-Slayer, Dweller, Destroyer or Slasher sword in hand, or being the Anti-Magic Lord), unless the server setting `star.neverlandCanonGate` is false (then the heir mark alone, and the plain dome of 3.3.0). Six rim stars; the dome silences foes, jams their skills and shatters barriers and stars for 80 of its 100 ticks, then the anti-magic fades; it costs 8% of max Aura (at least 30). Duration, size and cost are the mod's own (no source gives them).

### KEPT
- Every older page, theme, screen and VFX (the `anime` and `classic` themes, the classic companion screen and bubble, the 3.3.0 dome behaviour behind the gate setting), the Quintile pages, Time Magic and its VFX. `star.nodes` off still restores the old Star Magic.

### Cost (rule 11)
- GUI: no packets and no server work. The Great Spirit look is drawn only while a menu or the crest is open; about 200 fills and sprites a frame on the companion screen, at most 36 item icons (gift picker) or 9 + 9 (pouch), nothing allocated per frame; `quality = LOW` drops ripples and glow pulses.
- Companion screens: no new packet; inventory scan of 36 slots every 5 ticks only on the Bond and Pouch tabs; gift memory at most 8 companions x 24 items, bond story 8 x 6 events, all cleared on logout; bubbles at most 24 x 6 draws, 2 lines beyond 12 blocks.
- Star dispel: one pass over the in-memory registries (at most 512 stars and 8 panes), zero work when no star exists, at most 8 effect packets per dispel, a grace table capped at 640 entries. Black Neverland: 5 pulses, each one foe query, one ally query and at most 6 bolts; at most 4 `shatterBarriers` calls, 8 shatter packets and 8 foes affected per cast; one dome packet per cast.

### Not done / caveats
- Nothing in this release has been seen in a game; all previews are software renders. The Great Spirit text and companion labels truncate more in the previews than in the game.
- Great Spirit is not the default theme. There is no ink-script font. Clover frames appear in the companion portrait and the Four Kingdoms header, not in Tensura's own skill windows. The heal ring cannot see the server's 30-second timer. Gift memory and the bond story are lost on logout.
- Star dispel and the Black Neverland numbers (chance 0.35, grace 40 ticks, duration, size, cost) are first drafts. Only the mod's own dispel breaks stars; Tensura's dispel skills do not. The canon facts for Black Neverland come from search summaries of the English wiki and fan summaries of the chapters (the wiki host could not be fetched, a Japanese search found nothing); Quintile is still unverified canon.

## [3.3.0] — Valkyrie Armor + drill lance, Star Magic nodes, Ultimate Magic for 104 attributes, Mana Method runes, Mana Zone upgrade, companions

### NEW
- **Ultimate Magic**: one Ultimate for every attribute that has a grimoire (8 flagship + 48 rows 1-48 + 48 rows 49-96 = 104; Time Magic has none), the last page of each family book. Gate: full mastery of the book, Senior 1st Class rank and 1M EP (the Wizard King skips rank and EP). Wind-up of 110-220 ticks, 99% mana to start, all mana taken at the release, three damage pulses (60/25/15%), at most 24 foes per pulse, 2 casts at once on the server, 15 minute cooldown (server settings in `nusmp-ultimate-server.toml`). The existing canon ultimates (Excelitius Leonum, Lightning Battle Fiend, Apocalypra Astrauza) are untouched. All 96 new Ultimates are original designs (no wiki page could be fetched), numbers are first drafts.
- **Kingdom charge animations**: 12 new player clips `ult_<clover|diamond|spade|heart>_<start|loop|release>` (Clover roars, Diamond decrees, Spade crosses its arms, Heart embraces); the Dark Triad follow Spade. Setting `ultimate.kingdomAnimations` (server, default on).
- **Character grimoires show their character's own Ultimate** (setting `ultimate.showInCharacterGrimoires`, server, default on; off = the old rule).
- **Mana Method**: five new pages (Flame, Water, Wind, Earth, True Magic Rune) behind the Mana Zone gate with Homing and Growth runes, True Magic placing real blocks, caps of 2 runes per caster and 24 on the server (server setting `nusmp-mana-method-server.toml`); the rune is drawn as actual rune symbols written in the air in dense rings with an open mod grimoire.
- **Mana Zone upgrade**: evasion of arrows and projectiles, +15% attack and +0.5 knockback resistance while the zone is open, and omnidirectional zone-casting (the page is cast from behind the target / the zone edge / above). All server settings in the `[upgrade]` section of `nusmp-mana-zone-server.toml`, each switchable back to the 1.99.0 zone.
- **Valkyrie Armor v2** (a real armour render layer, client setting `valkyrieLook` V2 | CLASSIC, default V2): wide marbled veil wings with a stepped notch, scarf-blade, fin boots, first-person gauntlets, and the Saint and Mermaid looks (`valkyrieForm`); its own 1200-quad pool (720 medium, 360 low quality), nearest wearers first.
- **Valkyrie Drill Lance (A + B merged)** as a real weapon item `valkyrie_drill_lance` (creative tab, trident numbers, no throw): a rigid grip and a turning drill, 3D in hand, engravings barrier_piercing 3, elemental_boost 2, crushing 1, magic_weapon 1; on the armour it is `valkyrieLance` OFF | ATTACHED | FLOATING (drawn at near and mid distance only).
- **Star Magic nodes** (server `nusmp-star-server.toml`, `star.nodes` default on): placed stars (6 per player, 8 for the Grinberryall heir, 128 on the server, 40 s), Conjunction teleports to the star you look at and brings up to 3 allies, Quartile Scutum (pane), Hasta (spear), Flagellum (whip, heir), Black Neverland (dome), and Quintile Atrum Scutum / Hasta / Flagellum (unverified canon); the Quartile family needs the heir mark (Spade Star grimoire at full mastery + the Spade Kingdom Raid cleared); stars sometimes break to Anti-Magic (`star.dispelChance`, default 0.35); `star.dualMode` FUSION | SECOND_GRIMOIRE (default FUSION). Look: cel base with a glow mix, `vfx.element.star` V2 | V1 and a reduced-flashes option. Halation is not built.
- **Companion system (own version)**: a talk screen (Talk / Interact / Bond / Pouch tabs, all state on the server), speech bubbles, gifts with favourites, rapport tiers (3 / 10 / 25), stances (guard / aggressive / passive), a 9-slot healing pouch, friend-or-foe rules, followers that cross dimensions, and a voice-line hook. First companion: Charmy Pappitson (`nusmp:arc_companion`, spawn egg in the creative tab). Settings `nusmp-companions-server.toml` / `-client.toml`; `companions.enabled=false` restores the old arc chat talk.
- Docs and previews: `docs/companions/*`, `docs/magic/ultimate_*.md`, `mana_method.md`, `mana_zone_upgrade.md`, audits, `docs/previews/ultimate/*`, `docs/previews/mana_method/*`, GUI redesign directions A/B/C (`docs/gui_redesign/`, not built yet).

### REPLACED
- The Mana Method rune look: the written runes replace the staged dot-ring look (only ever in unreleased builds, nothing shipped is removed).
- Character-grimoire rule "the Ultimate page is hidden unless the set lists it" (setting off restores it); the kingdom clips replace `cast_mana_zone` + `cast_signature` as the Ultimate charge pose (the old clips stay as the fallback for a caster with no kingdom).

### KEPT
- Every older page, VFX layer, texture and shape, the Time Magic and its VFX, `CastAnimClient`, the 28 old clips, the older Mana Zone and all earlier ultimates.

### Cost (rule 11)
- Ultimate: at most 12 effect packets per cast (limit 16), 4 animation packets plus 1 stop per cast, 24 foes per pulse, 2 casts at once, no entities, blocks or chunks created.
- Mana Method: at most 300 quads (1,200 vertices) per rune, 1,800 quads for six; 1 rune packet of 28 bytes per cast; 7 blocks per cast, 160 in the world, taken back after 30 s; no entities.
- Valkyrie: at most 1200 quads / 4800 vertices a frame for all wearers (one near wearer with lance 1004, mid 523, far 174), first person at most 69 quads, no packets. Star: 128 star entities and 8 panes at most, 8 pane queries per tick, at most 10 effect packets per cast; a placed star is 7 quads near, 2 far, 0 beyond 64 blocks, a crowded view (20 mages, 6 stars each, 8 panes) 528 quads a frame.
- Companions: nothing per tick but a gated crossing check; sweeps once a second over at most one record per player; at most 5 inbound packets per second per player, 8 bubble packets per tick server-wide, 24 bubbles drawn within 24 blocks, followers per player 2 (hard cap 6), no forced chunk loads.
- Mana Zone upgrade: no entities, no chunk loads, at most one aim packet per 3 ticks (dropped faster than 2) per zone owner, 4 projectile looks per scan, at most 4 dodges per server tick.

### Not done / caveats
- Nothing in this release has been seen in a game: previews are software renders, the self-tests (`magic.ultimate`, `magic.mana_method`, `magic.zone_up`) only ran offline as pure tests; the CI server smoke test is their first real run.
- Ultimate numbers are not balance-tested; some proposal mechanics are not built (Anti-Magic buff-stripping, Curse Warding cleanse, Dice variance, Copy replay, Blood life-steal, Switching swap, Scale weight, Food ally heal).
- The floating grimoire may clip into the arms in the Diamond and Spade poses; clips only move arms and legs.
- Companions: never run; Charmy's tastes were picked for play, not from a wiki; no recorded voice lines; no high-five pose; followers also follow into arenas and the Mega-Dungeon (no dimension deny-list yet).
- Valkyrie and Star Magic were never seen in a game (software renders; the Star palette is a placeholder, no canon stills; Quintile is unverified canon; the pane stops projectiles and the mod's bolts only). The old Water Spirit Dive plates still draw under the new Valkyrie armour (only wings, helm and halo are skipped).
- Not in this release (still being built): Black Neverland to the manga's rules, the Great Spirit GUI (skins B and C, crest) and the companion screen restyle, Earth.

## [3.2.0] — Element effect remakes: Wind (pixel-anime), Water (3D toon + glass), Fire (cel + glow); Shishou flurry

### NEW
- **Wind pixel-anime effects** for Crescent Kamaitachi (Slash), Slicing Wind Emperor, Gale (the gust lane `FX_WIND_GUST` is now actually drawn) and the Wind Ring: 32 px pixel sprites with a dark green outline, 5 colours, hard 4-step alpha, a 2-frame flip-book on twos with smear ghosts. Client option `vfx.element.wind = MC_C | V1` (file `nusmp-wind-mc-client.toml`, default MC_C). Layer `WindMcLayer`, textures `wmc_*`, self-test `magic.wind_mc`.
- **Water hybrid look ("BC")**: real 3D toon-shaded cores (dragon head with jaw, spear, sphere, dome, crown) wrapped in volumetric glass layers, for Roar, Lance (Aqua Javelin look), Cradle, Nest, Burst, Splash and Ring. Client option `vfx.element.water = BC | V1` (file `nusmp-water-bc-client.toml`, default BC). New mesh draw path with a per-frame ceiling of 1200 mesh quads + 500 sprite quads for all water in view, nearest first, then lighter meshes, then flat sprites. Layer `WaterBcLayer`, meshes `wbc_meshes`, textures `wbc_*`, self-test `magic.water_bc`. Splash and Ring stay tintable for the blood / paint pages.
- **Fire V2 (anime cel mixed with volumetric glow)** for all 11 fire shapes: cel lion head, Leo Rugiens / spear / pillar / burst / zone / blast in flat cel tones, glow for spiral, cast and trail; Ignis Columna is the pillar plus a defensive vortex; Wild Bursting Flame redone to the anime still (ground circle of flame with fireballs thrown out in waves); the zone keeps its sun-lion sigil. Client options `vfx.element.fire = V2 | V1` (default V2) and `vfx.element.fireReducedFlashes` (default false: no hard flashes, rays or pulsing light, slower flip-book). Layer `FireV2Layer`, textures `fnw_*`, self-test `magic.fire_v2`.
- **Shishou flurry**: after a landed strike Shishou bounces off the target and dashes in again (server setting `flurryBounce`, default true; false = 3.1 behaviour). Entity attributes: flying speed 1.5, movement speed 0.8, follow range 64, attack damage 14 (strike damage is still level based). The 3.0 hunter AI is kept.
- Docs and previews: Wind pixel styles A / B / C, Fire / Water / Earth research, proposals and 3-direction previews, Water BC and Valkyrie Armor previews (`docs/vfx_remake/`, `docs/previews/vfx_remake/`).

### REPLACED (look only, each behind its V1 switch)
- Wind: Slash, Emperor, Gale, Ring. Water: Roar, Lance, Cradle, Nest, Burst, Splash, Ring (the new mesh path). Fire: the 11 shapes of FireSpellLayer / FlameFxLayer / FlameMagicLayer.

### KEPT (not touched)
- **Wind Tornado and Spirit of Zephyr** (owner request), the swallow sprites, all old layers and textures of Wind / Water / Fire, the old Water Valkyrie armour (the new Valkyrie Armor is not built yet), `DemonFireLayer`, the Salamander armour, Time Magic, Earth.

### Cost
- Worst case per activation (quads, old peak in brackets): Wind Slash 8 (72), Emperor 18 (80), Gale 16 (68), Ring 13 (100). Fire V2: lion 22 (66), spear 17 (49), pillar + vortex 33 (55), burst 29 (61), spiral 49 (67), wild 69 (93), cast 26 (45), zone 31 (39), fx burst 26 (54), trail 24 (41), blast 29 (73). Water BC (mesh + sprite quads, LOD0): Roar 335 + 54, Lance 110 + 39, Cradle 176 + 23, Nest 120 + 26, Burst 208 + 30, Splash 144 + 17, Ring 132 + 7 (above the old cap, kept in check by the per-frame ceiling and LOD / sprite fall-back). No new packets, no server work except the Shishou bounce (one velocity set per strike).

### Not done / caveats
- None of the new looks has been seen in a game (software renders without bloom; the nearest-neighbour filter for Wind, the translucent blend order and depth interplay of the Water cores, and the real GPU cost are unverified). The Water glass is not depth-sorted against the translucent hulls. Water shapes still fired by BasicArts / TrueMagic / CharacterArts / the old Water book keep the old look. Very dark tints lose the outline in Wind and Fire. Wind Ring is a flat decal.
- Fire's Salamander Claw / Breath, Shining Flame Cross, Flame Spike and buckshot spells are not built.
- Earth, the Valkyrie Armor, Star Magic, the mana systems (Mana Method, Mana Zone upgrade, Ultimate Magic), the companion system and the GUI redesign are in progress and not in this release.

## [3.1.2] — Spirit Dive no longer ends after one second for a creative diver; relic robe sets show all their pieces again

### FIXED
- **The Spirit Dive ended after about a second ("The spirit slips away.") on every cast** when the dive upkeep (2 per cent of max magicule each second) could not be paid from the pool: a creative player's pool is unused and can be empty, and the cast itself is free there. A creative diver now holds the dive (survival still pays and still ends at an empty pool); every end through the upkeep logs one INFO line `nusmp spirit dive ended: <reason> (<name>, creative=<bool>)` so the cause can be read from latest.log. This is the best reading of the report, not proven in a game.
- **Relic robe sets showed only the chest piece (the cloak).** The 1.48.0 "Clover robes in the anime style" update made the anime leggings and boots entries `none` for all 11 Magic Knight robe sets (and the hoods of four of them), which silenced the older models too, so helmet, leggings and boots vanished. Those pieces draw their older models again; the anime cloaks, Asta's headband and the devil horns stay. New client option `relicfx.robeAnimeBare` (default false; true restores the 1.48.0 look with only the cloak); `relicfx.robeAnime` is unchanged.

### KEPT
- Everything else of 3.1.1.

### Not done / caveats
- Not run in a real game. If the dive still ends, send latest.log (the new line names the reason). If a non-robe armour relic is still missing, name it: that would be another cause.

## [3.1.1] — Yuno's Wind overlay gets the classic's blocky arms

### NEW
- **Yuno's Wind Spirit Dive overlay (V2) now has the classic's blocky arms by default**: the box sleeve, the chunky fur cuff and a square shoulder slab, in the V2 cloth (cel bands, dark corner lines, gold band and buttons). Client option `modeArmor.windArms = BLOCKY | SMOOTH` (SMOOTH = the 3.1.0 arms); `modeArmor.windLook = CLASSIC` still draws the unchanged classic overlay.

### REPLACED
- The default arm and shoulder pieces of the V2 overlay (they stay one option away). The overlay is now 2,792 vertices at close range, down from 3,168.

### KEPT
- Everything else of 3.1.0.

### Not done / caveats
- Not run in a real game (software renders only); slim-arm skins use the wide-arm box, and the shoulder slab over a worn chestplate is unverified.

## [3.1.0] — Every grimoire has 15 spells, the Wind Spirit Dive rebuilt (Yuno's 1.107.0 overlay upgraded), spirit merge, Zephyr sword and Boreas bow, Soul Corpse numbered summons back

### NEW
- **Every grimoire shows 15 spells (basic grimoire of all 106 books, and the grimoire of all 43 characters).** 728 new pages appended after the old ones (no mode number moves), written in 8 batches: 7 per cent to a quarter of them are canon (marked `canon: <source>` in `docs/magic/pages15_batch1..8.md`), the rest are original spells in each magic's style (marked `original`). Page slots now grow to 15 at full mastery (19 with a contract), and a second unlock mask lets a book hold up to 64 family pages (Fire has 33). Server setting `magic.pagesFifteen` (default on) restores the old lists.
- **Wind Spirit Dive rebuilt.** Basic Wind users get the new Wind A look (thin 3D crown of arrow bars, smooth leaf wings, circle of crosses on the wrist, calm steady state); Yuno gets the Spirit of Zephyr overlay from 1.107.0 (green fur coat, star-blade crown, wind wings) rebuilt as real 3D (layered coat and collar, leaf wings with veins, ribbons) plus his sword at the hip, Boreas bow, merge and entry. Client options `spiritDive.windLook` (A_ANIME default, B_CLIP, C_SAINT, PACK, REEL), `spiritDive.calm`, `modeArmor.windLook = V2 | CLASSIC`.
- **Every Spirit Dive: the user merges with the spirit** (Sylph, Salamander, Undine, Gnome): the spirit appears, spirals into the chest with a flash, leaves a faint inner silhouette and separates on exit; slim smooth spirit models, a calmer entry flash while it flows in, and a first-person version. Options `spiritDive.merge`, `mergeModels`, `calmEntry`, `mergeFirstPerson`.
- **Smaller, more distinct Spirit Dive crowns** (Wind arrow bars, Fire flame tongues, Water wave crests, Earth crystals).
- **Spirit of Zephyr (sword) and Spirit of Boreas (bow)**: creative-tab items with real 3D models, wind glow, engravings (Zephyr: severance, barrier piercing, holy weapon; Boreas: magic interference); no loot or recipe.
- Other looks added and kept selectable: `spiritDive.look = REEL (default) | ANIME | DJINN | V2 | CLASSIC`; Fuegoleon's flame-limb arm from 3.0.x.

### FIXED
- **Soul Corpse numbered summons** (No.4 Jimmy, Alfredo, Carl, No.3 David, No.0 Michael Caesal, True Soul Corpse Magic) were hidden in every Soul Corpse grimoire but Rades's; they are listed again and, with the new server setting `corpse.numberedForAll` (default true), open for every holder by mastery (false restores the Rades-only lock).

### REPLACED
- The default Spirit Dive look (DJINN -> REEL), the default Wind dive (REEL Wind -> A_ANIME), the Yuno overlay look (CLASSIC -> V2), the boxy merge spirits and the loud entry during the merge window. Every older look stays one option away.

### KEPT
- All page ids, orders and casts of the existing books, every earlier Spirit Dive look, the Time Magic VFX, Shishou, relics and everything from 3.0.0.

### Not done / caveats
- Not run in a real game: all pictures are software renders (no bloom). The 728 new pages are unplayed (balance and feel untested; mostly original spells because the fandom wiki could not be fetched), the first-person merge direction and the Yuno overlay over worn armour are unverified.
- Wind B and C still draw their own crown and wings (they would double up with Yuno's overlay).
- Euros and Notos forms are not built (the pack had only tiny manga panels).

## [3.0.0] — Magic overhaul: anime-accurate character spell sets, spirits without keybinds + combat mode, remade Spirit Dives and Noelle's forms, Fuegoleon's flaming arm, basic grimoires, Soul Corpse fixed

### NEW
- **Every character's grimoire now holds the spells they actually used in the anime**, unlocked in the order the anime gave them (e.g. Yuno's Spirit of Zephyr, Spirit Storm and Hushed Dance only after Spirit Dive). 43 characters (Yuno, Noelle, Fuegoleon, Asta, Yami, the Black Bulls, Golden Dawn, Crimson Lion, Silver Eagle, the other captains, the Spade, Heart and Diamond casters ...) with about 100 new pages and per-character signature effect layers (`docs/magic/character_spells*.md`; sources and what could not be confirmed are listed there; server switch `magic.characterSpellSets`, off restores the old behaviour).
- **Basic grimoires are anime-based and different from the characters' spells:** 23 characters' signature spells are no longer in the basic books (they stay registered and open in their owners' grimoires), 19 new generic anime-based pages for Fire, Water, Wind, Dark, Ice and Beast (`docs/magic/basic_grimoires.md`; `basicBookHidesSignatures`).
- **Spirits are summoned and released from the grimoire or with `/multiverse spirit ...` (alias `/spirit`), no keybinds:** the seven spirit key sets (Salamander, Sylph, Undine, Gnome, Light, Darkness, Space) are removed. New **Spirit Combat Mode** (grimoire page, command or the Spirit Lord skill's third mode): the spirit casts at your target using BOTH its own mana and a share of yours (`docs/magic/spirit_control.md`).
- **Spirit Dives remade for Wind, Water, Fire and Earth** with a standard look and a more layered character look for Yuno, Fuegoleon and Noelle grimoires (halo, wings, mane, ribbons, entry shockwave, particle trail); **Fuegoleon's flaming right arm** while his Salamander is bonded, in third and first person (`docs/magic/spirit_dives.md`; `nusmp-spiritdive-client.toml` `classicLook` restores the old dive).
- **Noelle's forms remade:** Saint Valkyrie Dress, new Mermaid Form (open to every Water mage), Sea Dragon God Form and Black Dragon Form (Noelle only; limits and exhaustion; refused during Devil Union), standard and character tiers (`docs/magic/noelle_forms.md`).
- **Soul Corpse summons work:** the pages could never be unlocked (mastery 67 to 100 percent, rank gates, slot limits) and the zombie summons failed silently; the generic summons now open for any Soul Corpse user (starters at once, others at 30 to 80 percent mastery without rank or slot), only Rades's numbered undead stay Rades-only (`docs/magic/soul_corpse_fix.md`).

### REPLACED
- The seven spirit keybinds (and their key packets), the whole-book grimoire of each character with a set, the old Spirit Dive look and Valkyrie Dress look (client options restore them), the basic books' copies of character signature spells, the page-unlock rules of the Soul Corpse summons, and three CanonBook magic assignments (Charlotte Briar, Lucius Soul, Mars Mineral, Kirsch Cherry Blossom).

### KEPT
- Every page id, order, cast and unlock bit, all old lock lines, the Time Magic VFX, relics, Shishou and everything else from 2.1.0.

### Not done / caveats
- Not run in a real game before this release; all previews are mock-ups. Spell acquisition order is the mod's design where chapter or episode numbers could not be found; some effects are inferred from a name alone. The wiki hosts could not be fetched (search summaries only). Floga, Lolopechka and Zagred have no set (no named spells found). Fire book is at its 24-page limit.

## [2.1.0] — Shishou update: new top grade EX-Singularity (prismatic), faster hunter, Tensura Ultimate Skill instead of keybinds, stronger growth, feeding from the inventory, Akashic GUI

### NEW
- **A new relic grade above Genesis: "EX-Singularity"** (Akashic Relic). Own tooltip (prismatic per-letter rainbow name, grade row and border, 11-glyph star run), animated rainbow ring around the item in inventory and hotbar slots, the sword's glow cycles the whole spectrum in hand and in the world with an iridescent blade veil, orbiting prism shards and motes (lighter than a Genesis weapon's glow). It is never rolled in loot or relic waves; only Shishou carries it. Client options in `nusmp-ex-singularity-client.toml`, off restores the old look (`docs/relics/ex_singularity.md`).
- **Shishou hunts fast:** target scan every 6 ticks over a 40-block cube (was 24 horizontally and only 4 up or down), supersonic charge dash, immediate rescan after each kill, patrols toward the nearest player when idle, dormant only after about 80 kills (was 25); the thrown minion hunts aggressively around you.
- **No new keybinds: Shishou is now an Ultimate Skill of the Tensura skill system** ("Shishou, the Teacher", 7 modes: Telekinesis, Sensory, Defence, Offence, Storage, Magic, Counsel). Claiming the sword grants the skill, losing it revokes it; chat fallback `/multiverse shishou cast <ability>` and `throw`. The H, J, K keys are removed.
- **Stronger and growing like the anime:** starts at level 10 with low-magisteel-sword damage (assumed 8.5) and climbs steeply (L30 22, L50 38, L70 57, L100 78 held damage), bigger mana pool and faster regeneration, faster EXP; saves from 2.0.0 are raised safely.
- **Feed him from the inventory:** click a stack from the cursor onto the sword (left = whole stack up to 64, right = one): magic stones and monster drops become EXP and can teach skills (blaze rod -> Fire, ender pearl -> Enderman, ...); owner only, rate limited (`relic.shishouFeed`).
- **Akashic GUI:** new Stomach (storage) screen and Appraisal HUD (star-field, cyan frame, prismatic line-work, gauge bars), movable like the other GUIs.

### REPLACED
- Shishou's Unique-grade banner and cyan glow (now EX-Singularity, old look kept behind the option), the H/J/K keys, the 2.0.0 hunting goals and old level/mana/damage numbers (kept as legacy values for migration), the old Stomach screen and HUD look.

### KEPT
- All old grades and ordinals, every relic's grade, all 31 skills and their cooldown/mana logic, Shift+Right click Stomach, the one-per-world rules, saves, the Time Magic VFX.

### Caveats
- Not run in a real game before this release; previews are mock-ups. The low-magisteel damage (8.5) is an assumption (no repo value, wiki unreachable). The Tensura skill wiring copies the Requip pattern and was only type-checked; the hotbar floating name still shows the old blue.

## [2.0.0] — Shishou, the Akashic Relic sword (new): a talking, flying, levelling sword with 31 skills; new version numbering from 2.0.0

### NEW
- **Shishou (Teacher), "Reincarnated as a Sword", as an Akashic Relic (Grade: EX-Singularity):** one-of-a-kind sentient sword (hard cap of ONE per world, recoverable, never duplicable). A flying entity hunts hostile mobs, eats their drops into EXP, and goes dormant standing in the ground; right-click it to claim it (soulbound to your UUID). Non-owners get Slowness X and Wither and "Let go of me, you aren't my wielder." and lose it after 10 seconds. Throw / recall key H, cast key J, next skill K, a movable Appraisal HUD, `/multiverse shishou ...` admin commands, rare natural appearance (config).
- **31 skills with Tensura names** (all from the sword's own mana, with cooldowns, caps and a server config switch each): Telekinesis, Thought Communication, Ultra-Speed Regeneration, Stat Concealment, Skill Eater, Skill Sharing, Wielder Registration, Appraisal, Multilayer Barrier, Stomach (27-slot storage that survives death), Elemental Synthesis, Sword Arts Mastery, Magic Sense, Black Flame (new effect), Stealth, Vibration Magic, Spatial Motion, Demon Lord's Haki, Divine Sword Release, Black Lightning, Spatial Blade, Doppelganger, and absorbed Fire, Wind, Earth, Water-Ice, Darkness, Healing, Purification, Poison Coat. Tensura engravings, a new franchise "Reincarnated as a Sword" with 2 engravings.
- **3D model built from your reference image** (about 4,965 quads, level of detail, four animation states idle / aggro / attack / dormant), glow skin, inventory icon, trail particle theme (`docs/items/shishou.md`).
- **Voice pipeline for Shishou (no audio yet):** 41 lines manifest, generate and convert scripts, speech gate (owner and players within 32 blocks, one line per listener per 4 s); until the voice is made he speaks through action-bar text.
- **New version numbering:** from 2.0.0 on the mod uses MAJOR.MINOR.PATCH (feature release = minor, fixes = patch); CLAUDE.md rule 4 updated.

### REPLACED
- Nothing.

### KEPT
- Everything from 1.113.0.

### Not done / caveats
- Not run in a real game before this release (CI build and server smoke are the first runs); balance numbers are first guesses (every cap is a config value).
- GeckoLib replaced by the mod's own animation system; no real Tensura API calls; Skill Sharing is a small speed bonus (no mana sync); no wall-placing spells; Rukh particles replaced by a custom theme (repo rules forbid Rukh outside the Djinn and dungeons).
- Wiki: the English and Japanese wiki hosts could not be fetched from the build environment; skill facts come from the owner's documents and Japanese web-search snippets. "Sword Lord's Haki" and "Elemental Synthesis" are owner design, not canon-checked.

## [1.113.0] — Spirit Lords remade (Salamander, Sylph, Undine, Gnome) and new Spirits of Darkness, Light and Space; Vermillion/Magna casting styles and Scale Magic; movable GUIs; dungeon rooms rebuilt; Engravement Codex; smaller jar

### NEW
- **Spirit Lords with poses, faces and voices** (all server-decided, bounded, with LOD and frame budgets, each behind its own `magic.<spirit>` server config, off = old behaviour): **Salamander** (closer to the references, moods, 25 synthesised creature sounds), **Sylph** (small, wings rebuilt like the references with a rainbow sheen, 14 new poses, 14 faces), **Undine** (tall water woman, 10 poses, 7 faces), **Gnome** (stone golem companion and 6 m giant, 9 poses, 6 faces, Feremena's spirit), and new **Spirits of Darkness** (shadow-wisp and aristocratic demon, life steal, mark, Shadow Step), **Light** (golden cub and seraph, piercing beam and Glowing pulse) and **Space** (rift orb and wisp-dragon, portal rings, waypoints). Docs and previews: `docs/magic/*.md`, `docs/previews/*`.
- **Casting styles:** Magna needs his Soul Bat (bat swing launches the spell), Mereoleona casts through fist combos, Fuegoleon through his Salamander, Leopold through a charging spiral; **Scale Magic** weighs the opponent (Scale readout, suppression aura) (`docs/magic/fire_casting.md`, `scale_casting.md`).
- **Engravement Codex** book listing what every engraving does; `docs/items/engraving_codex.md`.
- **Relics are wearable and always visible** on players unless toggled off (`/nusmp relic wear`).
- **Movable GUIs (F9 or `/nusmp gui move`)**: drag, snap, resize, hide and reset the Djinn panel and every other custom HUD element and screen panel; client config `nusmp-gui-layout-client.toml` (`docs/ui/gui_move.md`).
- **Dungeon rooms and hallways rebuilt at the same size:** 460 new room and hallway variants (new roofs, floors, pillars, galleries, lighting), server config `gen.roomsRebuilt`; weapon racks are no longer generated; **armour stands are a rare prop** (about 6 percent of eligible rooms, at most 2 per floor; `props.armourStandRarity`) (`docs/labyrinth/rooms_rebuilt.md`).
- **Elsdocia / Demon Dweller no longer drain the target's magicule** (charge still builds; `magic.weaponsDrainTargetMagicule`, default off).
- **Smaller jar:** 739 large textures reduced to 256-colour palettes with a quality gate (PSNR, alpha, size unchanged), about 150 MB less (`tools/shrink_pngs.py`).
- **Mob remake plan and audit** (`docs/mobs/remake_plan.md`, no mob changed yet; decisions pending).
- **CLAUDE.md rule 12** (building and shipping standard).

### REPLACED
- The look and sound of Sylph's wings, the Water, Earth and Fire Spirit Lords (old looks kept behind each spirit's switch); armour stands as set decoration (now rare); the System window position store (old keys still read as the fallback).

### KEPT
- All old pages, ids, models, textures, saves and the Time Magic VFX; weapon rack block, item and loot tables stay registered.

### Not done / caveats
- Spoken lines are only partly recorded (ElevenLabs credits ran out): Undine 13/40, Light 13/35, Darkness 24/47, Gnome 15/21, Sylph 2/56; missing situations use synthesised sounds. Voice licences are unchecked.
- Nothing was run in a real game before this release; CI (build + server smoke) is the first real run.

## [1.112.0] — Soul Corpse zombies and Rades's five undead, 3D Compass, anime Recombination golem, Storm staff (Fairy Tail), Light and Bone remake, flat white shapes gone, lag audit

### NEW
- **Soul Corpse summons real zombies** (they never burn in daylight, obey the caster, capped 6 per player / 60 per server; server config `corpse.zombiesInsteadOfPuppets`, off = old puppets). **Rades Spirito's five numbered undead:** No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl, No.0 Michael Caesal, each a custom-looking undead mob with its own model, skin, animations and signature move (`docs/exclusive/rades_undead.md`).
- **Grimoire Unique Skill shows the character's name** ("Rades Spirito's Soul Corpse Magic") for every canon character.
- **Compass Magic is one floating 3D compass** (bronze rim, green dial, 8-point rose, needle that points at the target) and every Compass spell comes out of it as 3D models; level of detail and frame budget keep it cheap (`docs/magic/compass_3d.md`, client config `visual.compass3d`).
- **Recombination Magic looks like the anime:** the Raging Black Bull is a house-golem (stacked houses, tower-spire horns, glowing eyes, brick fists) that rises, walks, punches and stomps with dust, bricks and an impact flash; rooms, ring and drill are house/brick pieces (`docs/magic/recomb_anime.md`, client config `visual.recombAnime`).
- **Storm Magic is a Fairy Tail magic tied to the new Stormcaller Staff** (3D weapon): eight storm spells (Gathering, Downpour, Hailstorm, Thunderhead, Gale Front, Storm Surge, Twister, Eye of the Storm) with a cloud deck, rain, hail, forked bolts and funnels; the old grimoire stays registered but is not rolled (`storm.staffOnly`) (`docs/magic/storm_staff.md`).
- **Light and Bone Magic remade after the wiki:** Light has the nine listed spells (Light Sword of Judgment ... Absolute Monarch of Divine Brilliance), Bone gets Eternal Fangs, Bone Bullets, Demon Sword Dainsleif and Spatial Rupture, all with 3D props (`docs/magic/light_bone_ja.md`, server config `magic.lightBoneJa`).
- **The ugly flat white shapes are gone** (weapon constructs, Mirror doubles, Soul Corpse hand/grave/shard/skull billboards): solid shaded models instead, old look behind `visual.flatProps`.
- **Lag audit (CLAUDE.md rule 11):** caps, throttles, level of detail and frame budgets for the Compass, franchise engravings, new relics, armour failsafe; per-effect worst-case costs in every doc (`docs/perf/lag_audit_1.md`).
- **CLAUDE.md rules 10 and 11:** English wiki first with the Japanese wiki as fallback; no lag on a public server.

### KEPT
- Every old page id, model, texture and behaviour (all replaced looks stay one setting away).

## [1.111.0] — Relic pyramid rebalanced by ADDING relics (Millennium Items, 287 new Normal), franchise engravings, armour relics never invisible, weapons held by the hilt

### NEW
- **Relic count balance, nothing removed or regraded:** 356 new relics (Normal 9 -> 296, Special 96 -> 141, God 51 -> 60, Genesis 20 -> 35; Rare, Unique and Legend unchanged). Docs: `docs/relics/special_normal_a.md`, `normal_b.md`, `normal_c.md`, `god_special_new.md`, `genesis_new.md`.
- **15 new Genesis relics:** the seven Millennium Items (Puzzle, Eye, Ring, Rod, Key, Scale, Necklace; wearing 3 to 7 together gives a set bonus) plus Ea, the Requiem Arrow, the Orbs of the Sage, the Hogyoku, the Seven Dragon Balls, Joy Boy's Straw Hat, the Law of Cycles Gown and the Greed Island Binder.
- **9 new God and 20 new Special relics** (Zangetsu, Ruyi Bong, Susanoo plate, Stella bow, Hylian shield, Black Bulls robe ...); and 25 more Special relics in the first Normal wave..
- **287 new Normal relics** (everyday gear and trinkets of 35 worlds, 18 simple weapons, each with its own 3D worn model, sprite, passive or act).
- **Franchise engravings:** 69 custom engravings for 32 franchises (Black Clover, Magi, Fairy Tail, Shield Hero, Yu-Gi-Oh, DanMachi, Solo Leveling, Fate, FMA ...), applied once to every item of that world (915 items mapped), with a "Franchise" tooltip line, server config `nusmp-franchise-server.toml`, caps on every effect (`docs/items/franchise_engravings.md`).
- **Weapons are held by the hilt:** every 3D weapon is now placed so its grip point sits on the hand (third and first person, both hands, any size); the grip is read from the mesh handle, a per-weapon table or the class (`docs/weapon_grip.md`, self-test `item.weapon_grip`). The old placement stays as a fallback (`-Dnusmp.weaponGrip=off`).
- **Armour relics can no longer be invisible:** failsafe boxes when nothing of a worn head, chest or feet relic was drawn, every armour texture painted on its own and re-checked after reloads, truthful `/multiverse relic debug` per armour relic, self-test `relic.armor_visible` (`docs/relics/armor_visible.md`).

### KEPT
- Every existing relic, grade, model and engraving; nothing was replaced.

### NOTES
- Wave104 and Wave102/105 had clashing ids; the Wave105 and Wave102 versions were kept and 4 duplicate Normal relics were dropped from Wave104 (116 instead of 120).

## [1.110.0] — Voices, white squares fixed for good, custom particles with lag limits, bigger daggers and katanas, vanilla-style shields, duel-disk decks, relic armour, one tooltip for every item

### NEW
- **Spoken voices (ElevenLabs library voices):** 120 lines in 4 voices (David for the System announcer, Final Boss for boss taunts, Justin for the hub NPCs, Obsidian for the 36 Djinn): System floor, lockdown, ambush, rest, boss and rift lines; intro, phase 2 and death lines for 10 boss families; merchant and quest lines; a summon and an attack call for every Djinn. The server decides and sends the line to nearby players only (rate-limited per player); the client plays it in the Voice sound category. Client `voice.enabled` and `voice.volume`, server `voice.maxPerSecond`. The text of every message stays. About 4 MB of Ogg. `docs/audio/spoken_voices.md`. **Check each library voice's licence for commercial use before a public release.**
- **One tooltip for every item:** every item of the mod gets the relic-style tooltip (grade-coloured name, grade banner with stars, type row, existing lines in the relic palette, footer, dark frame); relics keep their own. `docs/ui/unified_tooltips.md`.
- **Duel-disk deck relics:** the Deck of Many Things and the Pact of the Deck are worn as a Yu-Gi-Oh duel disk on the left forearm (arm plate, blade fan with monster zones, spell zone, life-point strip, deck holder with a card stack) and held as a larger disk; the Deck keeps its floating golden cards. **Draw One Card** now plays a 2-second 3D card summon from the disk (card slides out, light streaks, slams onto a zone with a shockwave, flips, a column of light in the card's alignment colour, dissolves into sparks); client option `deck.summonEffect`. `docs/visual_upgrade/duel_disk.md`.
- **Armour-slot relics always show:** the painted armour layer is now drawn whenever the 3D worn model is not, so a crown or boots can no longer be invisible; `/nusmp debug worn` lists every worn relic and how it is drawn. Relic armour layers are repainted (colour ramp, bevels, trim, chest emblem, brow gem, glow). `docs/visual_upgrade/armor_slot_relics.md`.
- **Shields behave like the vanilla shield:** all 7 held shields use vanilla's shield display and blocking pose; right-click raises the Hero's Shield, the Calamity Wave Aegis and unattuned relic shields (their first act moved to sneak + use); the Legendary Shield works from the off hand; an axe hit disables the shield used. `docs/visual_upgrade/shield_fix.md`.
- **Daggers and katanas are no longer slivers:** thickened copies of 24 dagger and katana meshes (blade at least 1/8 block wide in first person, real spine, grip and guard) and daggers about 0.68 blocks in third person and 0.62 in first person (some were 0.27). `docs/visual_upgrade/weapon_sizes.md`.
- **Permanent fix for the white squares:** every glow, halo and flare pass of the weapons, relics and effects goes through one safe alpha-weighted blend (`SafeAdditive`); the shared halo texture and 326 others were cleaned of the hidden colour that made the boxes; tests fail the build if any class draws a glow type itself or any additive texture carries the defect. Elsdocia included. `docs/visual_upgrade/safe_additive.md`.
- **Only the mod's own particles, and far fewer:** every particle of the upgraded weapons and relics is now a custom particle (577 vanilla tokens in 86 tables moved); default rates are halved (`particles.upgradeDensity` 0.5), with caps per item and per player and a live cap, thinned by frame rate and distance, none for others' items beyond 16 blocks; particles shrink near the camera so a flame can no longer fill the view. `docs/visual_upgrade/upgrade_particles.md`.

### REMOVED (the owner asked)
- The custom Backpack (block, items, upgrades, screen, recipes) and its drop roll. The player-placed-block exemption, Luck loot and Luck coins stay. A world that held an old backpack loses it.
- The flat floating card picture of the Deck of Many Things (a test keeps it from coming back).

### REPLACED
- How the deck relics, the 7 shields, the daggers and katanas, and relic armour are drawn (old files stay on disk where possible; `visual.upgrade=false` brings the old look back for the visual changes).

### KEPT
- Every id, stat, act, passive, engraving, Better Combat file, saved data (except the removed backpack), the player-placed-block exemption, Time Magic.

### Honest notes
- Nothing has been run in the game and no voice has been listened to. Many numbers (particle caps, summon timing, shield display values written from memory of 1.21.1, ground-drop position, katana width) are first guesses.
- The duel disk does not retract, and the card summon does not follow an arm swing or a sneak.

## [1.109.0] — The 36 dark Djinn weapons upgraded

### NEW
- **Dark Djinn weapons (visual upgrade), all 36:** each keeps every part of its old model and gains the horns, wings, tail and halo of its Djinn, an affinity pack and a "dark rig" (grip bands, pommel thorns, an eye gem, a name plate with the Djinn's name engraved and lit, rune rings, a chain or thorn collar); a new 1024 px skin with a second lit glow map, six orbiting pieces of its own, a swing trail, seven ambient particle layers, and a reduced-detail model beyond 12 blocks. `docs/visual_upgrade/djinn_dark_weapons.md`.
- The last two effect files that used coloured vanilla dust (`UpgradeFxClient`, `UpgradeActFx`) now use the mod's own particles.

### REPLACED
- How the 36 dark Djinn weapons are drawn in hand, on the ground and in frames (old meshes and skins stay on disk; `visual.upgrade=false` brings them back).

### KEPT
- Every id, stat, act, engraving, Better Combat file, the 36 light weapons, the inventory icons, Time Magic.

### Honest notes
- Nothing has been run in the game. Measured proxies: 5.0x to 10.2x the quads (5.0x for every weapon at least), 9.4x to 29.5x the painted texels; the combined score is 6.7 to 10.7 and is not a measure of how it looks. Orbit sizes and the placement of the added parts (placed from a measured frame, not by hand) are untuned.
- The jar grows by about 75 MB of textures for this slice.

## [1.108.0] — Genesis Demon-Slayer upgraded, smaller textures, a config file clash fixed

### NEW
- **Genesis Demon-Slayer (visual upgrade):** a 4,296-quad blade and hilt with chipped slab, framed panel, fanged split, void holes with charred glowing rims, engraved lore text, a clover pommel, nine orbiting black-glass shards, hanging blade chips and five circling clover leaves; 2048 px skin with glow and vein maps; 16 ambient layers, 15 idle animations, a draw animation, a swing trail and a signature effect on all three acts. Black Meteorite still breaks the blade into four shards, Resonance still swells the clover, and the 1.6x growth of Black Divider is kept. Reduced-detail model beyond 12 blocks. `docs/visual_upgrade/demon_slayer.md`.
- **(this build)** Server-friendly rule and guard: CLAUDE.md rule 9 and `ServerSafetyTest` (no client-only classes in common code, no `Minecraft.getInstance()` outside `client/`); `ConfigFileNamesTest` (no two config files may share a name).

### FIXED
- **Two settings files shared one name:** the Genesis weapons' and the God weapons' visual-upgrade options were both registered as `nusmp-visual-upgrade-client.toml` since 1.102.0. NeoForge keeps one config per file name, so this was a config conflict. The God weapons' options now use `nusmp-visual-upgrade-god-client.toml` (their old values reset to the defaults once). This may be behind the "Cannot get config value before config is loaded" crash reported against Tensura No Index, but that is not confirmed.

### CHANGED
- **Smaller jar:** every texture was recompressed losslessly (10,587 of 10,738 PNGs, 323.6 MB to 275.5 MB, every pixel checked identical, including the colour under transparent pixels). `tools/optimize_pngs.py` repeats it.

### REPLACED
- How the Demon-Slayer is drawn in hand (the old model, skins, icon and world effect stay on disk; `visual.upgrade=false` brings them back).

### KEPT
- Every id, stat, act, passive, engraving, Better Combat file, saved data; Time Magic.

### Honest notes
- Nothing has been run in the game. The Demon-Slayer's combined proxy score is dominated by the texture term (a 64 px skin against 2048 px); with every ratio capped at 20 it is 16.1, so it does not honestly reach "20x".
- The dark Djinn weapons upgrade is still being built.

## [1.107.0] — Dungeon spec gaps: break what you place, Gorgon ban, poise and desperation, 7x7 clearance, themed music, weapon racks, Luck loot and backpacks, room lockdowns and ambushes

### NEW
- **Player-placed blocks can be broken (dungeon protection exemption):** torches, sleeping bags, crafting stations and backpacks a player sets down in the dungeon can be broken and picked up again; every generated block stays unbreakable. The positions are kept per dimension (capped, oldest dropped first) and synced to clients so the crack animation works. `docs/labyrinth/guard_exemption.md`.
- **Gorgon ban:** `iceandfire:gorgon` can never spawn in any dungeon dimension (natural, spawner, encounter director, saved chunks; operators' summons too unless allowed). Config list `spawn.blacklist` and tag `nusmp:dungeon/banned`. A floor-theme mob palette filter keeps factions to the themes they belong to.
- **Poise and desperation:** elites and squad captains have a poise bar and super armor during telegraphed attacks until it breaks (stagger); under 30% HP they enter a desperation phase (faster, ultimate unlocked, cue effects). Attacks now show their hit area on the ground and freeze their aim late in the wind-up. `docs/labyrinth/poise_desperation.md`.
- **One dungeon faction:** dungeon monsters no longer target or hurt each other (turf wars and faction wars are off by default; `foundry.poise.dungeonPassive=false` brings them back).
- **7x7 clearance:** every doorway, corridor, bend, bridge, seal and stair is cut to a 7x7 free passage for the Tensura giant races, after the piece is placed, behind `gen.clearance7` (default on; `/multiverse mega clearance`). Stairs are 7 high but not 7 wide (they need new stairwell templates). `docs/labyrinth/clearance7.md`.
- **Themed music engine:** the client crossfades between 25 original synthesised loops (10 theme families x explore/battle, plus boss, safe, hub, rift) by the floor's theme and what is happening; vanilla music is silenced while one plays; the music volume slider applies. Replace them with real tracks by shipping the same file names in a resource pack. `docs/audio/themed_music.md`.
- **Weapon racks:** a real block with a block-entity renderer (up to five items, no entity, no tick) replaces the dungeon's armour stands (flag `props.rackInsteadOfArmourStand`), with themed loot variants.
- **Rift shards and parallax:** floating, drifting, disintegrating reality shards around the space-time fractures and a two-layer masked parallax on the ordinary rift (not a true stencil).
- **Luck-scaled loot and backpacks:** loot counts follow a bell curve that rises with Luck and depth, Tensura coins also scale with Luck, chests and lucky kills hand out rolled armour, and there is a placeable, upgradable Backpack (27 to 63 slots, five tiers) that keeps its contents when picked up.
- **Room lockdowns and ambushes:** key challenge rooms seal with unbreakable barriers until their wave is dead (the escape rules still apply); a player who bypasses a room (leaving with live monsters, running through corridors, teleporting, escaping a lockdown) gets an ambush squad ahead. `/multiverse mega lockdown ...`. `docs/labyrinth/lockdown_ambush.md`.

### REPLACED
- The dungeon's armour stands (rack on by default, flag restores them); the old door, corridor and stair sizes (flag `gen.clearance7` restores them); turf wars and faction wars default to off; the Gorgon's entries in two dungeon mob tags (moved to the exclusion tag).

### KEPT
- Every id, stat, saved data, template, floor height, the saturating floor scaling, Time Magic.

### Honest notes
- Nothing has been run in the game; every new system has only pure-logic tests, type checks and CI self-tests. The numbers (poise, Luck curve, ambush heat, lockdown rooms) are first guesses and live in config or data files.
- Not done: exponential floor scaling (the saturating curve is deliberate), the nested skill checkboxes (removed at the owner's request), a true stencil rift, 7-wide stairs, real OST music.

## [1.106.0] — Worn God relics upgraded, the Rukh kept for Djinn and dungeons only, CI fixes for 1.105.0

### NEW
- **Worn God relics (visual upgrade), 24 items:** cloaks, crowns, armour and ornaments each have their own 3D worn model (about 5.5x the quads of the old ones), a painted skin with its own glow map, palette tint, ambient effect layers, idle animation groups, act visuals and a reduced-detail model. They share one library with the 1.104.0 charms and Genesis relics (one flag table, one draw path, one switch `visual.upgrade`, one lookup order). `docs/visual_upgrade/god_worn.md`, `docs/visual_upgrade/worn_merge.md`.
- **The particle conversion now covers the relics too:** the relic waves, the client relic effects and the rift pockets use the custom particles; the Rukh (magoi) stays only in the Djinn and dungeon code (`docs/rukh/exclusive_enforced` makes the test strict).

### FIXED
- 1.105.0 did not build: two source-scan tests found the old `END_ROD` particle left in the relic effect files. Converted.

### REPLACED
- How the 24 God worn relics are drawn (old models stay on disk; `visual.upgrade=false` gives the old look).

### KEPT
- Every id, stat, act, passive, engraving, saved data; Time Magic.

### Honest notes
- Nothing has been run in the game; the "5x" figures are measured proxies (quads, texels, layers, animation groups: ratios between 5.2x and 14.6x), not how it looks. Two helper files still use coloured dust (`UpgradeFxClient`, `UpgradeActFx`): follow-up.

## [1.105.0] — Boss damage fix, custom particles, Genesis weapons upgraded (carries the 1.104.0 content, whose build failed on a test)

### NEW
- **Fix: a hit no longer takes a slice of the boss's maximum health.** The balance law added about 40% of the target's max health on top of every hit (a 400 tooltip emptied a million-HP boss). The extra is now a tiny sliver (at most 2% of max health, none on bosses and none on hits above 20). Elsdocia and every other weapon now deal what the tooltip says plus that sliver. Pure class `DamageSliver` with tests.
- **Custom particles instead of the Rukh/vanilla ones:** 70 particle types in 21 groups (fire, ice, stars, motes, shards, runes ...), a theme table of 284 themes (all magic types, relic grades, elements and waves, skills, bosses, dungeon areas) and the `Fx` facade through the perf budgets. `/multiverse particles list|spawn|theme|status`. Client/server switch `particles.custom`. `docs/custom_particles.md`.
- **The Rukh (magoi) is now exclusive to Djinn users and the dungeons.** Everything else uses the new particles (`docs/rukh/exclusive.md`).
- **Genesis hand weapons upgraded (visual upgrade), 13 items:** Astralis, Blackrazor, Cardinal Halo Chakram, Fairy Glitter Gauntlets, Gae Bolg, Magic Emperor's Black Halberd, Primordial Hearth Blade, Starforged Aeon Bow, Starsplitter Hammer, Sword of the First Dawn, Void Origin Katana, Nekrathul's Pact Scythe and Elsdocia: 2500+ quad meshes with floating parts, 2048 px skins with glow and vein maps, a halo layer, 8 to 10 ambient effect layers, idle and draw animations, swing trails, a signature effect on every act, a reduced-detail mesh from 12 blocks, a looped sound id. `docs/visual_upgrade/genesis_weapons.md`.
- Everything from 1.104.0: the Lesser Summon as a real tiny Djinn, the second slice of God weapons and 27 charms and Genesis relics (see the 1.104.0 entry below).

### REPLACED
- How the 13 Genesis weapons are held (new meshes and skins while client `visual.upgrade` is on; the old files stay on disk). The old Rukh calls outside Djinn/dungeons now use the custom particles (`particles.custom=false` brings back the old ones).

### KEPT
- Every id, stat, act, passive, engraving, Better Combat file, saved data; the Genesis Demon-Slayer; Time Magic.

### Honest notes
- Nothing has been run in the game. For the Genesis weapons the measured proxy score is 4.0x to 18.6x (not 20x: the vertex cap bounds the mesh ratio); the visual impression is unverified. Not done yet: the Genesis Demon-Slayer, worn God relics (cloaks, crowns, armour), the dark Djinn weapons. The jar is larger because of the 2048 px skins.
- The soundboard sounds still wait for the domain `www.101soundboards.com` to be allowed or for the clips to be uploaded.

## [1.104.0] — Tiny Djinn for the Lesser Summon, God weapons (second slice), charms and Genesis relics upgraded

### NEW
- **The Lesser Summon is a real tiny Djinn.** A 0.45 block chibi figure of the bearer's own Djinn (all 36 Djinn, each with a light and a Dark texture, wisp tail instead of legs, its crown, mane or ornaments in miniature) hovers at the right shoulder (low and small in first person), with a poof entrance, idle float with blinks, a cast pose that points at the foe, a dash attack, a hop after a kill, a spiral exit into the owner, a name tag and a timer bar. The Djinn panel's circle shows its head. A small visual-only packet (`DjinnNet.Cue`) tells clients 16 ticks before the Lesser Summon's bolt lands who it strikes. Client option `djinn.tinyModel=false` brings the old bust back. `docs/tiny_djinn.md`.
- **God weapons, second slice (visual upgrade):** Staff of the Magi, Wizard King's Vow, Wand of the Wisdom King, Hammer of Thunderbolts, Gungnir, Kusanagi, Leviathan's Crown Trident, Calamity Wave Aegis, Hellblaze, Minotaur King's Labrys, Dragon Force Twin Fangs and Dawnpiercer Crossbow: new 1024 px skins (5.3x to 11x the painted detail) with engravings, lore parts (rings, shards, ravens, rune stones, magatama, kelp, flame tongues, twelve spectral axe heads...), orbiting parts, flowing energy veins, a halo shell, six ambient particle layers, swing trails, a signature effect on each act and a reduced-detail mesh from 12 blocks. `docs/visual_upgrade/god_melee_a.md`.
- **God charms and Genesis relics (visual upgrade), 27 items:** the Infinity-style stones, Deck, Warren's Key, Rose Petal, Administrator's Key, Archivist's Codex, Behelit Egg, Kamish's Fang, Moon-Tower Sigil, Orrery, Pocket Watch of Return, Serie's Grimoire, Vaultmaker's Pocketwatch (God) and the Holy Grail fragment, Philosopher's Stone, Prison Cube, Seal of the Sealed God, Sukuna's Fingers, the Infinity Gauntlet, Absolute Being's and Zahard's crowns and the Gate of Babylon crest (Genesis): each with its own hand model and worn model (God 512 px skins, Genesis 1024 px), glow map, star/nebula pass, 9 to 17 ambient layers following its lore, act effects, a draw animation and a reduced-detail model. `docs/visual_upgrade/charms_genesis_worn.md`.
- Visual upgrades share one switch: client `visual.upgrade` (default on); off gives the 1.101 look.

### REPLACED
- The translucent Djinn bust above the shoulder (visual only; kept behind `djinn.tinyModel=false`), and how the 12 Wave71 God weapons and 27 charms and Genesis relics are held and worn (old models kept on disk).

### KEPT
- Every id, stat, act, passive, engraving, Better Combat file, saved data; the Lesser Summon's bolt, price, duration and cooldown; Time Magic.

### Honest notes
- Nothing has been run in the game. "5x" and "20x" are measured proxies, not how it looks: for the 12 Wave71 God weapons the combined score rose 1.8x to 4.1x (the meshes were already large, so mesh detail grew 1.1x to 3.3x), and for the 27 charms and relics the score is dominated by the texture term (without it about 3 to 5x). The Genesis weapons, the worn God relics (cloaks, crowns, armour) and the dark Djinn weapons are still to come.

## [1.103.0] — Custom boss bars for the dungeon bosses

### NEW
- **36 custom boss bar styles** for the dungeon bosses, drawn by the mod (a generated frame atlas per style with emissive glow, a scrolling fill effect, a name plate with the boss's name and title, and an emblem): Holy, Evil, Undead, Fire, Ice, Storm, Void, Forest, Water, Blood, Clockwork, Mirror, Underworld, Qliphoth gate, Devil, Supreme Devil, Dragon, Angel, Archon, Lich, Fallen King, Plague, the pocket tomb mega-bosses, the arc bosses (Zogratis, Midnight Sun, Seven Sins, Spade Kingdom, Magic Knights, DanMachi, Lucius) and Goliath, Charybdis, the Warden, the Spirit King, Zagred and the God of Order. The 200 Decennial bosses resolve onto 22 of them by archetype, hall theme and creature.
- **What the bar shows:** a damage-lag ghost chip, phase notches with the phase number and name, a shield band, an invulnerable state (ice veil and chains, the title becomes Invulnerable), an enrage glow, the next lair action flashing with a countdown ring in its last 5 seconds, adds left, a healing wash, how many fighters, the boss's wind-up as a thin bar under the main bar, a low-HP pulse, an optional damage-per-second ticker, an entrance animation per style and a shatter or dissolve when the boss dies, plus a live portrait of the boss. Health is shown as a percentage (`bossbar.showHp` adds the numbers). Three or more bars at once are drawn compact.
- **Config and commands:** client `bossbar.custom` (off = the old animated bar), `scale`, `showPhaseMarkers`, `showHp`, `portrait`, `showDps`; `/multiverse bossbar styles|preview <style> [hp%] [phase]` (OP) and the client twin `/nusmp bossbar ...` to see any style without a boss. `docs/boss_bars.md`.

### REPLACED
- Nothing. The 1.25.0 animated bar stays as the fallback and as the look of other mods' and vanilla bars.

### KEPT
- All boss logic; vanilla and other mods' boss bars keep their look.

### Honest notes
- Nothing has been run in the game: the art was only seen in a software mock-up with a different font, so some ornaments may touch the title line, and the live portrait and the entrance/death animations have never run. Julius's bar and the Lucius channel bar are not tracked yet.

## [1.102.0] — White squares under magic circles fixed, Dungeon Map screen fixed, first God weapon visual upgrade

### NEW
- **The white / yellow SQUARE under magic circles and on spells is fixed.** Cause: the aura and prop effects (the Erza equip ring, the glow sprites on armour, 253 call sites through `AuraRender.additive`) were drawn with vanilla's `eyes` render type, which adds a texture's colour and ignores its alpha; it is made for textures that are black outside the shape. 251 of the 346 sprites the mod draws that way are painted the other way round (colour everywhere, alpha shapes the sprite), so every transparent texel lit up and the quad became a square. They are now drawn with an alpha-weighted additive type (`NURenderTypes.additiveAlpha`, chosen per texture by an audit; textures that are black under alpha keep the old type), every VFX batch sets its whole render state, and a missing or opaque ring/circle/glow texture is replaced by a generated soft disc. Config `vfx.safeBlend` (client) forces the safe path. Time Magic is untouched. `docs/vfx_square_bug.md`. The big pale polygons in the Dimension Slash screenshot look like vanilla clouds seen from below (not proven; try clouds off).
- **The Dungeon Map screen is crisp.** It drew the map first and then let the game blur the whole frame over it. It now draws the background once, then the map under a clip, with a title bar, a Close button, a wrapping legend and keys: C centres on you, F fits the floor, + and - zoom, Esc closes; pan and zoom are limited and keep their state when the window is resized. Every other screen of the mod was scanned for the same mistake: none had it. `docs/dungeon_map_gui_fix.md`.
- **God weapons, first slice (visual upgrade):** the eight pocket boss weapons (Aurliel's Seraph Halberd, Chronarch's Pendulum Sabre, Deepwatch Harpoon, Oriel's Starfall Staff, Reflected King's Mirrorblade, Rimeheart Greatblade, Sanguine Warden's Cleaver, Valkarin's Stormlance) and Paradox get new 1024 px skins with engravings and a separate vein glow, extra parts (filigree, crystals, rings, cogs, chains, ribbons), orbiting pieces, a breathing halo, swing ghosts, seven ambient particle layers, idle animations and a reduced-detail mesh for distance. Client option `visual.upgrade` (default on) brings the old look back. Measured against the baseline taken first (`docs/visual_upgrade/god_melee_b*.json`): painted texels 5.8x to 11.4x, glow passes 2 to 5, ambient layers 1 to 9, animations 1 to 7. The mesh quad count is only 1.3x to 2.9x because the old meshes were already big and the brief's own vertex cap would not allow more. `docs/visual_upgrade/god_melee_b.md`.

### REPLACED
- `AuraRender.additive` (was `RenderType.eyes`), the per-frame render state of `VfxVertexBuffer`, the draw order of the Dungeon Map screen.

### KEPT
- Every texture, effect and Time Magic as it was; the old weapon meshes and skins on disk; the Dungeon Map data and the `dungeon_map` GUI scale id.

### Honest notes
- Nothing has been run in the game. The square fix is a diagnosis from the screenshots, the code and a bit-for-bit comparison of the old and new vertex buffers; the performance pass is ruled out as the cause. The dark Djinn weapons (God grade) are not upgraded yet. The remaining God and Genesis items follow in later releases.

## [1.101.0] — Hidden dungeons fixed (one room, no switch), Djinn binding with Better Combat, relic damage locked behind attunement

### NEW
- **Hidden dungeons (rift pockets) work.** Cause found: the Dungeon Guard refused every block the pocket code turned into air, so doorways were never opened (every realm was ONE room: the arrival room), the bars and the treasury seal never lifted (NO SWITCH), and an expired rift stayed in the world. All pocket block writes now run inside the guard's internal span (a test keeps it so). The old realm has 5 rooms (a new **lever chamber** with a visible lever between two sea lanterns, a glowing sign and a sign over the bars), the 13 exclusive tombs have 14 to 21 rooms each with a seal **lever** in the key room (never behind the seal), a Sigil chest, signs at the seal, and a second exit rift in the treasury. The lever works at once when the master is dead, else after 120 s; the door opens by itself after 420 s so nobody is ever stuck. You are held at the tear with a progress percentage until the whole tomb is built; a watchdog repairs missing doorways. `/multiverse mega rift rooms|switch [open|tp|repair]` for testing. `docs/labyrinth/pocket_fix.md`.
- **Djinn binding works with Better Combat.** Cause found in Better Combat's source: it makes the OFF HAND look empty whenever a two-handed weapon is held (23 of the 46 vessel weapons are two-handed), so the Seal could not see the vessel. The Seal, the Dark Rukh Shard, Holy Water, the Unbinding Seal and the admin grant now read the raw inventory; new ways to bind: click the Seal onto a relic in the inventory (either way round), `/djinn bind [main|off|slot <n>] [djinn]`, and the Djinn key (J). Refusals now say why. 30 missing Better Combat files for 15 Djinn weapons added. `docs/djinn_bc_fix.md`.
- **Relic damage is locked behind attunement**: a relic that needs attunement deals about 20% of its power (melee and shots from relic bows and crossbows) to anyone it is not attuned to (`relics.unattunedPower`, `relics.attuneLock`); relics that need no attunement (Elsdocia), Djinn, Requip, hero weapons and loot gear are not affected; a tooltip line explains it and `/multiverse relic power [player]` shows the factor. `docs/relic_attune_lock.md`.

### REPLACED
- Unattuned relic swords no longer hit for full damage; an unattuned relic bow now draws at 20% instead of refusing.

### KEPT
- All pocket ids, saved pocket data, commands and loot tables; every Djinn rule, price and weapon file; the attunement ritual and the limit of three.

### Honest notes
- Nothing has been run in the game: the pocket, Djinn and relic fixes come from reading the code (and Better Combat 2.4.0's source) plus tests and the server smoke test. The off-hand Seal behind a two-handed main weapon stays impossible by Better Combat's design: use the other ways.

## [1.100.0] — Requip Magic abilities and the performance pass

### NEW
- **Requip Magic itself** (new modes 15 to 27 of Requip: The Knight, appended after Dismiss, so every saved mode keeps its index): a **Requip Armoury** (pocket dimension, 54 cells, its own window and key, capacity grows with mastery and EP), **six Loadouts** swapped in one press (the old set goes into the Armoury, never to the ground), **Requip: Blade**, **Barrage** (8 to 24 spectral swords, damage from the weapon's grade, boss-capped, allies never hit), **Strike** (throw and recall), **Partial Armour** (wear only chosen piece groups of an Erza armour), **seven finishers** (Dimension Cut, Circle Sword, Blumenblatt, Titania's Rampage, Dance of the Swords, Sea of Swords, Moonlight Cross) and the passives Instant-Change, Pocket Capacity, Weapon Affinity and Requip Reflex. `docs/requip_magic.md`.
- **Performance pass**: distance LOD, culling and a frame vertex budget for VFX; per-quad and per-cube allocations removed in the effect, weapon and geo renderers; cached locations and models in the aura, relic, Foundry and Abyssal renderers; effect-packet and particle budgets per tick; scan intervals as config; a probe (`/multiverse perf on|off|report`, client `/nusmp perf`) that shows which handler is slow. Defaults give the same result near the camera. Config: `nusmp-performance-common.toml` and `nusmp-performance-client.toml`. `docs/performance_1_99.md`.

### REPLACED
- The aura payload style field is now a VarInt (same single byte below 128) so the Partial Armour mask fits. Per-frame allocation chains in the renderers listed above (same output, checked bit for bit by tests).

### KEPT
- The 14 armours, their attacks, Dismiss, the six weapons, every saved mode index, the Time Magic VFX.

### Honest notes
- Nothing has been run in the game or profiled: every performance figure is a static estimate, and the LOD distances are reasoned defaults. The Requip Barrage swords are the mod's blade shape plus particles, not a 3D copy of the held weapon. The Spirit and Demon-Slayer renderers, the squad and rift code were not part of the performance pass (listed in the doc as a second pass).

## [1.99.0] — Dungeon overhaul: bosses and arenas, squads and loot, rift pockets, hunger ban, mob sounds, Mana Zone, armour policy, Erza remake, Great Spirits, player-model humanoids

### NEW
- **200 Decennial bosses** (100 Holy at levels 1..100, 100 Evil at -1..-100; one per 10 floors) with 2-4 phases and lair actions, and **36 thematic boss halls** (graveyard with breakable tombstones that spawn adds, ruined throne room with destructible pillars, cathedral, forge, frozen keep, ...). The Evil side follows the Tree of Qliphoth: seven gate halls, devils stationed across the bands, three Supreme Devils at levels -98 to -100. `docs/labyrinth/decennial_bosses.md`.
- **Squads of 5 to 8** in the Dungeon Master (tank, melee, flanker, ranged, support, leader) with `InterceptAttackGoal`, `FlankTargetGoal`, `SupportRetreatGoal`, formation and morale; `squads.enabled=false` brings the old spawning back. **Functional anime loot** by a global loot modifier: 30 buff foods and elixirs, 24 monster parts with recipes and anvil uses, 8 weapons, 8 armour pieces and 8 trinkets with pre-rolled stats, sold to the Hub merchant for Tensura coins. `docs/labyrinth/squads_and_loot.md`.
- **Mega-rare rifts and 13 exclusive pocket dungeons** (own biome look, 4-5 exclusive mobs, a mega-boss, inverted gravity rooms, mana-sealed treasury, collapse when the relic is taken), 26 relics and 13 boss weapons that drop only there, a new space-tear renderer, rarity tiers with a pity counter. `docs/labyrinth/rift_pockets.md`.
- **Admin `/dungeon` tree** (OP): `tp <floor>`, `skip`, `boss spawn <level>` / `boss kill`, `rift spawn <pocket|shortcut>`, `skill unlock`, `skill give_sp` (skill points were removed in 1.96.0: it adds mastery points and says so), `reset`, `debug wfc` / `debug info`. Merged into the player's existing `/dungeon`.
- **Mob sound engine**: 25 mob families x 8 cues = 200 sound events, 525 generated ogg files, `sounds.json` entries, subtitles; dungeon mobs no longer use vanilla zombie or skeleton voices (`tools/gen_mob_sounds.py`, `docs/audio/mob_audio.md`).
- **Mana Zone is late game** (2,000,000 EP, Senior 1st Class, mastery, 15 boss kills, dungeon floor 40 and an attunement inside a Grand Magic Zone). Mana Zone follows the canon: attacks from any point of the zone, sharper senses, pressurised protective spaces, footholds in mid air. **Grand Magic Zones are places**: five named regions (Seabed Temple, Witches' Forest, Ultime-Volcano, Gravito Rock Zone, Elysia) with chaotic gravity, currents, magma heat, storms. Optional True Magic page. `docs/mana/mana_zone_redesign.md`.
- **Erza Requip armours remade at Djinn quality**: all 14 armours as full sets of 19-22 pieces with glow maps (`docs/erza_armour_remake.md`). **Great Spirits**: Sylph is a tiny winged fairy, Undine a giant mermaid, Salamander gets a flame layer; **Spirit Dive** looks: Wind half and full dive, Saint Valkyrie Dress, Fire draconic arms, Earth (`docs/rebuild/great_spirits.md`).
- **Dungeon humanoids on the player model** (Foundry people, Living Armor, Warden, Guardian incl. D&D and Decennial bosses, Goliath, the named corpses) with 385 HD skins and appendages; `client.playermodel.dungeon=false` restores the old renderers. `docs/playermodel_phase2.md`.
- **Relic rendering diagnostics**: `/multiverse relic debug` names why a relic does not draw; relic packs of NPCs and Avatars now reach clients (`docs/relic_visuals/worn_render_2.md`).

### REPLACED
- **Casting no longer equips armour.** Armour belongs to explicit forms: of 105 magics only 11 have one (Spirit Dive for Flame, Water, Wind, Earth, Devil Union for Anti-Magic, Shadow; toggles for Lightning, Dark, Mercury, Corundum, Beast); the rest are visual effects only. `docs/rebuild/armour_policy.md`.
- **Hunger ban**: every hunger or exhaustion drain of this mod was removed or turned into a magicule cost (rests, gloom, traps, curses, relics, Djinn backlash); a test fails if one comes back. `docs/labyrinth/hunger_ban.md`.
- Mana Zone's early toggle buff, the "Grand Magic Zone = dense biome" reading, the Erza armour pieces (same ids), the Sylph and Undine looks in the renderer (old models stay on disk).

### KEPT
- Every page id, mode, armour id, entity id, save data and the Time Magic VFX; all old renderers behind their config flags.

### Honest notes
- Nothing has been run in the game; models and arenas were checked by software previews, tests and the server smoke test only. The wiki pages could not be reached: Mana Zone, Grand Magic Zone, Spirit Dive and Qliphoth follow the canon text the owner supplied. The Tensura ability-menu injection and skill points from the spec were not rebuilt (skill books teach Unique skills directly since 1.96.0). Rests recover HP and mana but not ability cooldowns yet. Requip Magic abilities and the performance pass follow in 1.100.0.

## [1.98.0] — 35 new Legend, God and Genesis weapons

### NEW
- **13 Legend, 12 God and 10 Genesis grade weapons** (`relics/wave/Wave71`), each a full relic with its own 3D mesh, skin, glow skin, 16x16 sprite, Better Combat file (melee), 2 acts (Legend) or 3 acts (God, Genesis), a passive and Tensura engravings; base damage from the grade table of 1.97.0, by weapon type.
  - Legend: Vorpal Sword, Holy Avenger, Luck Blade, Frostbrand, Dragon Slayer, Sunsword, Crimson Lion's Flame Lance, Silver Eagle's Galewing Bow, Starlight Whip, Fairy Heart Gauntlets, Tidebreaker Chakram, Noon Cleaver, Calamity Scythe.
  - God: Staff of the Magi, Hammer of Thunderbolts, Kusanagi the Grass-Cutter, Gungnir the Unerring, Leviathan's Crown Trident, Wizard King's Vow, Wand of the Wisdom King, Dragon Force Twin Fangs, Calamity Wave Aegis, Hellblaze the Demon Sin, Minotaur King's Labrys, Dawnpiercer Crossbow.
  - Genesis: Blackrazor, Gae Bolg Heartseeker, Starforged Aeon Bow, Primordial Hearth Blade, Void Origin Katana, Fairy Glitter Gauntlets, Cardinal Halo Chakram, Magic Emperor's Black Halberd, Starsplitter Hammer, Sword of the First Dawn.
  They drop by grade from the existing relic pools (vault tables, labyrinth pedestal, Crucible). `docs/new_weapons_197.md`.

### REPLACED / KEPT
- Nothing replaced; every older weapon is untouched.

### Honest notes
- Nothing has been run in the game; the models were previewed only in the software rasteriser and the 16x16 sprites are auto-rendered from the meshes. The dungeon humanoids on the player model are still not in a release.

## [1.97.0] — Elsdocia the Genesis sword, grade damage ranges, Celestial Spirit Magic and the Spirit King

### NEW
- **Elsdocia is a Genesis grade weapon with no attunement**: base damage 400, +25 for every new Grand Magic rank owner it is passed to, and it can never be passed back to its last owner. The engraving "Legacy of the Wizard Kings" now gives it one new magic ability per Black Clover magic type among its owners (shift-use cycles, use casts; the tooltip counts "N of 105 magics"). `/multiverse relic elsdocia reset|owner|lineage`. `docs/elsdocia_legacy.md`.
- **Celestial Spirit Magic** (`celestial`, left out of the 1.96.0 notes though it was in that jar): a Unique skill with 31 modes; 12 Gold Zodiac keys, 4 silver keys and the Spirit King's gate key as real 3D items (inventory, off hand or a new Curios slot); contract ritual; the 16 spirits; Star Dress forms; with all 12 gold keys the Celestial Gate lets you challenge the Spirit King in his own dimension, earn his recognition or summon him. `/multiverse celestial give|bind|reset|king|gate|info`. `docs/celestial_keys.md`.

### REPLACED
- **Weapon damage follows the grade table**, scaled by Tensura weapon type: normal 20-30, special 35-55, rare 60-85, unique 90-115, legend 130-175, god 190-240, Genesis 260-350 (`relics/GradeDamage`). Every relic, hero, Requip, Djinn and Black Clover weapon was moved into its grade's range; tier ascension and the aura bonus stay inside the effective grade's range. Grades assigned by the builder: Holy weapons Legend, Seven Star Unique, Vassal Rare, Requip Unique (Nakagami Legend), Djinn light Legend and dark God. `docs/grade_damage.md`.

### KEPT
- Hero form bonuses and Djinn bonds still stack on top of the new base; skill and boss damage are unchanged.

### Honest notes
- Nothing has been run in the game. The new legend, god and Genesis weapons and the dungeon-humanoid player models are not in this release.

## [1.96.0] — Erza's Requip: The Knight; skill books now teach Unique skills; the Skill Tree is removed

### NEW
- **Requip: The Knight** (Erza Scarlet, `requip`, `skill/ErzaRequipSkill`): a standalone Ultimate skill with 15 modes, 14 armours (Heart Kreuz, Heaven's Wheel, Flame, Lightning and Sea Empress, Black Wing, Clear Heart Clothing, Nakagami, Heaven's Wheel Grand Chariot, Adamantine, Purgatory, Flight Armor, Wind God, Fairy Armor) and Dismiss. Every swap and dismiss uses the Djinn full-equip animation (`EquipAnimation`): the old armour shatters, the new one forges in, with a flash and themed particles. Attribute buffs, resistances, flight on the winged armours, per-second upkeep, six 3D weapons with Tensura engravings. Learned from the book "Titania's Requip" (100,000 max EP, or 25,000 plus a mastered Unique skill); `/requip book|teach|set|dismiss`. `docs/erza_requip.md`.
- `/multiverse skillbook list|learn|forget|give|gold|shop`.

### REPLACED
- **Skill books** now teach a real Tensura UNIQUE skill directly (254 skills, `nusmp:line_<id>`); the book is consumed, refused if you already own the skill. The skill's modes are the line's arts, passives run while it is switched on, and mastery replaces SP. Dungeon loot, rift chests, the Black Market and the creative tab hand out these books. Old saves migrate at login (owned lines become unique skills; SP is dropped; Black Market gold kept).
- **The Skill Tree system is removed**: tree GUI, Arts bar, Passives panel, the nested Tensura panel, SP and its HUD, route nodes, `/multiverse tree`, keybinds, tree docs and tests. The 29 Fairy Tail books of the old system are removed (the new Fairy Tail skills of 1.95.0 and Requip stay). `docs/skillbooks_unique.md`, `docs/skilltree/REMOVED.md`.

### KEPT
- Old item and component ids (SP book etc.) and hidden legacy skill ids stay registered so old worlds load.

### Honest notes
- Nothing has been run in the game. Mastery progression and every balance number are first-pass. The celestial keys and Spirit King, Elsdocia, the grade damage ranges, new legend/god/Genesis weapons and the dungeon-humanoid player models are not in this release.

## [1.95.0] — Fairy Tail skills, GUI size options, worn relics on the player model

### NEW
- **Eight Fairy Tail skills** (`fairytail`): Fire Dragon Slayer (Unique, evolves to the Ultimate Fire Dragon King with Dragon Force, Lightning Flame Dragon Mode and Fire Dragon King's Mode), Ice-Make, Sky Dragon Slayer, Lightning Dragon Slayer, Iron Dragon Slayer, Crash and Water Magic. Spells are modes opened by mastery; ten forms wear a render-layer armour that forges in with the Djinn equip animation. One skill book teaches a skill directly (`/multiverse fairytail give|learn|list`; 12 percent pool in the dungeon relic vault). `docs/fairy_tail_skills.md`.
- **GUI size options** (`client/ui/GuiScale`): global and per-GUI scale for 19 screens and 10 HUD elements, a GUI Size screen (button in the status screen, key F8, Mods > Config), fit-to-window. Config `nusmp-gui-client.toml`. `docs/gui_scale.md`.
- **Worn relics on the player model**: the player-model kit now draws relics on its humanoids; relic and slot pairs without a catalog entry fall back to the slot's default look; mod items worn in Curios with no model draw a floating charm. `docs/relic_worn_fix.md`.

### REPLACED
- Nothing.

### KEPT
- Everything else. The skill tree removal, Erza's Requip, celestial keys, Elsdocia, the grade damage ranges and the dungeon-humanoid player models are not in this release.

### Honest notes
- Nothing has been run in the game; balance numbers are first-pass guesses. The Curios slots necklace, back, belt, hands and charm must come from Curios itself.

## [1.94.0] — Dungeon overhaul: 29 new puzzles, encounters fixed, rifts fixed, the System names the floor, no more lock-ins, Create traps, true-to-D&D bosses, player-model humanoids with new hair, Anti-Magic on Aura

Owner brief: "remake all models to use the player model, fix the hair, fix direct encounters, way more types of puzzles, fix the abyssal bosses like the lich to look like a D&D lich, fix the hidden dungeon rifts, Create compatibility for the traps, the System should tell the floor's theme and mechanic, remove the random being locked in a room or add a way out, anti-magic should use aura". Per-part docs: `docs/labyrinth/puzzles_v2.md`, `encounters_fix.md`, `rifts_fix.md`, `system_floor_info.md`, `abyssal_bosses.md`, `docs/compat/create.md`, `docs/playermodel.md`, `docs/antimagic_aura.md`.

### NEW
- **29 new puzzle types** (`labyrinth/mega/puzzle`): plate sequences, mirror beams, Simon levers, memory tiles, lights-out, sliding tiles, riddles with clickable answers, water levels, pipes, a number lock, constellations, mimic chests, pendulums, sentries, escorts and more. Each is solvable by one player, deterministic from a seed, restores its room block for block, never seals anyone in (a lectern gives up, and every puzzle winds down by itself) and pays through the trial loot table. Chosen by theme, depth and road from `data/nusmp/mega/puzzles/weights.json`; two new gallery rooms. Admin: `/multiverse mega puzzle ...`.
- **The System names the floor** (`mega/system/FloorInfo`, `FloorCard`): on arrival it shows the floor number and direction, the theme (name, description, road, danger), the floor mechanics with how to beat them, the depth effect on monsters and the boss floor. Re-show with the period key, `/dungeon floor` or `/multiverse mega system floor`.
- **Ways out of every lock** (`mega/lock`): sealing mechanics and trial chambers show a release rune after a timeout and open on their own; boss arenas always show an exit rune; `/dungeon unstuck` takes a player or party to the last safe room; a flood-fill check in the generator guarantees every room reaches the stairs.
- **Create compatibility** (`compat/create`): with Create installed the dungeon gets six trap families (saw run, press, drill wall, treadmill, pour, sequence) with levers to disarm; no compile dependency, a wrong id falls back to a vanilla stand-in, Create contraptions are refused inside the dungeon. Config `nusmp-labyrinth-create-compat-server.toml`.
- **22 true-to-D&D boss looks** for the Labyrinth Guardian (lich, Acererak, Vecna, Strahd, Zariel, Asmodeus, Orcus, Lich King, Solar, Demogorgon, Beholder, Elder Brain, Kraken, Tiamat, Bahamut, Dracolich, Tarrasque, Lolth, quasit, vrock, hezrou, balor), three phases each, humanoids as player-model skins with extras.
- **Player-model kit** (`client/playermodel`): story, arc, Zogratis, avatar and NPC humanoids now use the vanilla player model (slim/wide arms, armour layers, extras for horns, wings, crowns, props). **New hair system**: 23 styles, layered with a front fringe, swaying and bouncing with movement, hidden under helmets.
- Admin tools: `/multiverse mega dm why|force` (why no encounter fired), `/multiverse mega rift ...`.

### REPLACED
- Encounters: single-ring spawn search, one shared placement budget and straight-line hunting are replaced by a place ladder, per-monster budgets and room-by-room hunting; a pity timer makes quiet dark parties meet monsters.
- Hidden-dungeon rifts: 12 bugs fixed (exit loop, unguarded tick, barred vault, shortcut aiming at unbuilt floors and more).
- Anti-Magic abilities now cost Tensura Aura instead of the custom Anti-Magic Power bar; AMP stays as a harmless stat so saves keep working.
- Humanoid models: the old custom humanoid models and avatar hair layers are replaced by the player-model kit (old assets stay in the repo).

### KEPT
- Every existing puzzle, trial, seal and mechanic, the Floor Warden look for ordinary guardians, creature models, all older tests.

### Honest notes
- Nothing has been run in the game. Every Create block id is from memory of Create 6.x. The dungeon humanoids (living armor, floor warden, Goliath, the Foundry mobs) are converted in the next release.

## [1.93.0] — The magic rebuild (41 magics), the custom (merged) magic removed, 24 new Djinn, an animated Djinn panel, the vessel-overlay fix

Owner brief: "remove the custom magic, rebuild the magics (list), based on the Black Clover anime: custom summon models, models, render layer temp armor (the same equip animation the Djinn full equip has), vfx and mechanics; buff Djinn, add 24 custom Djinn, make the active Djinn GUI look different per Djinn and animated; fix the weapon-as-vessel bug". Per-magic detail, with its own NEW / REPLACED / KEPT table: `docs/attributes/<key>_rebuild.md`; shared brief: `docs/rebuild/magic_rebuild_brief.md`.

### NEW
- **Shared equip animation** (`client/aura/EquipAnimation`): the Djinn full-equip assembly (pieces anchored on their model part, staggered 0 to 12 ticks, flying in from their own side, fading and growing 85 to 100 percent over 8 ticks, shattering over the last 18) as one helper. Every rebuilt armour below uses it.
- **41 rebuilt magics**, each with: a real set of temporary-armour pieces (geo models with an emissive glow pass, slim-arm fit, first-person head hiding, level of detail), custom summon / construct models with animation (drawn through the prop system, no new entity types), three new VFX effects (`<NAME>_FX1..3`, or the family's own shapes) with generated sprites, canon-style mechanics through the balance law and the control gate, appended pages and self-tests:
  - Earth family: Sandstone, Rock, Red Ochre, Stone, Earth, Mineral (one armour kit, nine constructs, e.g. the Variable Hardness Prison cage).
  - Metal / body: Shakudo, Scale, Spike, Nail, Hair (golem, serpent, spike pillar, nail bolt, hair cocoon ...).
  - Nature: Tree, Vine, Plant, Poison Plant, Mud, Mucus (13 constructs).
  - Sound / soul: Song, Sound, Soul, Soul Corpse, Memory.
  - Air / weather: Smoke, Snow, Wing, Vortex, Air, Ash.
  - Space / force: Spatial, Gravity, Permeation, Mirror (the Real Doubles wear the glass garb too), Slash.
  - Elements: Water (Valkyrie Dress, Sea Dragon), Dark, Lightning (Raging Lightning, thunderbird), Flame (Crimson Lion, phoenix).
  - Transformation: Transmutation, Recombination (the Black Bull), Modification, Mercury (Silver Eagle).
- **24 new Djinn** (36 in all, appended after Vassago): Bael, Agares, Valefor, Barbatos, Gusion, Vepar, Shax, Buer, Andrealphus, Kimaris, Amdusias, Belial, Decarabia, Seere, Dantalion, Andromalius, Glasya-Labolas, Haagenti, Crocell, Furcas, Balam, Alloces, Camio, Murmur. Each has its own Half Equip special, Extreme Magic (five new shapes: rain, cone, quake, cross, implode), full-equip pieces (light and dark), seal, two weapons (light and dark twin, 48 new acts) with Tensura engravings, particles. `docs/djinn_roster_36.md`, `docs/djinn_roster_36_art.md`.
- **Animated active-Djinn panel** (`client/relics/djinn/gui`): themed per affinity and per Djinn (frame, palette, medallion built from the Djinn's look, phase reveal, full-equip state, summon intro), data-driven so every Djinn gets one. Config `nusmp-djinn-gui-client.toml` (`style`, `anchor`). `docs/djinn_gui.md`.

### REPLACED
- The armour, effects and spell code of the 41 listed magics (the owner asked for the remake). Page ids, order and mastery bits are kept; new pages are appended after the family pages, which moves the universal pages (Devil Union, Summon Grimoire ...) up. The generic page texts in `en_us.json` were replaced by the fragments' texts.
- The merge generator ("custom magic": two attributes with no magic of their own used to be fused into one generated grimoire) is removed: two attributes now stay two separate grimoire skills. The 64 skill ids `nusmp:merged_00..63` stay registered as inert placeholders (old worlds load) and are forgotten at login.
- Djinn: the 12 existing Djinn are buffed (Half Equip share and cap up, effects about 1.5 times longer, Extreme Magic +1 to 2 reach and +12 to 15 percent damage, weapon acts stronger); the old bar of the active panel stays as the `CLASSIC` style.

### FIXED
- Djinn weapon over a weapon vessel: the Djinn weapon displaced the vessel to another slot, the Equip lost its vessel and ended. The weapon now overlays the vessel in place; the vessel is kept whole (enchantments, components) and comes back exactly on end, death, logout, dimension change and creative (`VesselOverlay`, `WeaponLoan`).

### KEPT
- Time Magic and its VFX, every other magic, all older Djinn art, the classic Djinn bar.

### Honest notes
- Nothing has been run in the game. The Black Clover wiki was unreachable for every builder, so canon comes from the repo's wiki copies and memory; only some spells are 1:1 canon and each doc says which. All visuals are checked only in the software previews, and the heavy armours (6000 to 12000 vertices with glow passes) may need trimming.

## [1.92.0] — Unique Skill Lines in Tensura's own Ability Selection menu

Owner brief (the two Gemini specs): the Skill Tree must live inside the base Tensura mod's "Ability Selection" menu, not only in a screen of its own. Details and the WHY of every choice: `docs/skilltree/tensura_menu.md`.

### NEW
- **Stand-in skills** (`skilltree/menu`): one inert Tensura UNIQUE skill per line (`nusmp:line_<id>`, 283 = 275 generated lines + 8 classic routes), registered at start-up (the ManasCore skill registry freezes then). A line the player owns is taught silently as its stand-in, and forgotten when the line is not owned (the server half of the fog of war), so the line appears in Tensura's own left-hand Unique list with its name and icon. The stand-in cannot activate, toggle, tick or be slotted, so it can never double-fire an art.
- **Nested panel** (`client/skilltree/Tensura*`): when a line's entry is selected in Tensura's Ability Selection screen a panel shows the line's arts, passives and stat bonuses nested under it, with an **Equip** button (the existing single-active-line rule), a Skill Tree button, and Tensura-style **checkboxes** for passives (ticked = "ON", empty = "OFF" tooltip) that send the existing toggle packet. Operators and creative players get an **ADMIN INSPECT** switch that lists all 283 lines; everybody else only sees lines they own.
- Placement is read at run time (Tensura's own frame and scroll-bar getters), never guessed pixels, with an escape hatch config: `nusmp-tensura-menu-common.toml` (`enabled`, `panelPlacement`, `panelWidth`, `checkboxSkin`, `adminButton`). Self-test `tree.tensura_menu`, 41 new unit tests, 283 line icons.

### REPLACED
- Nothing. The own Skill Tree, Passives and Arts screens, the Codex button and inspect mode all stay.

### KEPT
- The route registry, the Hub economy, the cyclic dungeon, 3D skill books, rifts and loot modifiers from the same specs already existed and are untouched.

### Honest notes
- Nothing has been run in the game, and Tensura's bytecode was not available, only its API signatures. Everything visual is unverified: where the panel docks, how the checkbox looks in Tensura's art, that Tensura lists a learned stand-in Unique skill, that its lists are writable (if not, the integration disables itself and logs once), and what Tensura does with a Unique skill it did not register (limits, toasts, evolution lists). 283 extra registry entries mean server and client must run the same jar.

## [1.91.0] — Better drops the deeper (or higher) you go, bows held the way vanilla's are, the depth rule reaches Tensura's monsters

### NEW
- **Depth drops** (`labyrinth/mega/enc/DepthDrops`, `DepthDropRules`, `docs/labyrinth/depth.md`): the deeper or higher the floor (the same for both directions), the more a dungeon mob leaves. Four saturating curves of the floor's distance from the Hub: up to **+3 extra rolls** of the mob's own loot table (raw, with extra luck, so a deep monster leaves its whole drop list again, not only bigger stacks), up to **+100 % on every stack**, up to **+3 Luck** on those rolls and up to **x4 experience** (floor 10: 0.12 extra rolls and x1.09 xp; floor 200: 1.5 rolls, +33 % stacks, x2.2 xp; floor 1000: 2.5 rolls, +71 %, x3.3). Coins and equipment already scale with depth and are not changed. Config keys `drops`, `dropRollsMax`, `dropCountMax`, `dropLuckMax`, `xpMax`, `dropScale`; self-test `enc.depth_drops`; 9 unit tests.

### REPLACED
- **Bow placement** (24 bow meshes: the five relic and hero bows and their draw stages): the owner's screenshots showed the custom bows still turned the wrong way in first person. I measured the vanilla `bow.png` itself: its wood arc runs from the top right tip round the upper LEFT to the bottom left tip, its string lies on the lower right, the arrow flies up and to the left. 1.63.0 (angle 45) had the limbs right but the bow shooting backwards, and 1.84.0 (angle 135) turned the limbs a quarter turn off. The place angle is now **225** (and every bow mesh carries `orient manual`, because a downward place angle would otherwise be "corrected" by the runtime weapon normaliser, which turns a tip-down mesh over), which puts the limbs on the same diagonal as the sprite and the belly and arrow toward the upper left (`tools/relics/bow_vanilla.py`, `BowDisplayGuardTest`). The display transforms are the vanilla bow's, which is what the owner's new spec lists.

### FIXED
- The depth rule of 1.89.0 skipped most of the dungeon: it only touched mobs that are `Enemy`, but Tensura's monsters are tamable animals that the Dungeon Master marks. It now uses the economy's own test (`GearDrops.hostile`, `isBoss`, `isServant`, made public), so Tensura's monsters get the strength and the mind too.

### KEPT
- Everything else. The rest of the two new specs (Hub economy, cyclic dungeon, 3D skill books, passive toggles, rifts, loot modifiers) already exists; the Tensura Ability Selection menu integration is a separate piece (next release).

### Honest notes
- Nothing has been run in the game. The bow angle comes from the vanilla texture's pixels and the geometry of the mesh (limbs along y, belly and arrow along +x, checked on all six bow meshes and their draw stages), not from a screenshot of the result.

## [1.90.0] — FTB Chunks claims are refused in the dungeons

Owner brief: "players should not be able to claim chunks in the dungeons with FTB." Details: `docs/compat/maps.md` section 7.

### NEW
- **Claim block** (`compat/maps/FtbClaimBlock`, `ClaimBlockRules`): when FTB Chunks is installed, claiming and force-loading chunks is refused (in the map screen and in `/ftbchunks claim`, with the message "Claiming is not allowed in the dungeons") in every dimension of this mod: the Mega-Dungeon, the Hub, the Labyrinth, the arenas, the pocket realms. It uses FTB Chunks' own cancellable claim events, hooked by reflection (no compile-time dependency). Config `nusmp-compat-maps-server.toml`: `blockFtbClaims` (true), `ftbClaimBlockedDimensions` (`nusmp:*`). `/multiverse compat maps` shows whether it is active.

### REPLACED
- Nothing. The operators' warning from 1.87.1/1.88.0 is kept and now shows only when the block could not be registered or is switched off.

### KEPT
- Unclaiming is never blocked (claims made before this update can still be removed). Everything else.

### Honest notes
- Nothing has been run with FTB Chunks installed. The event and method names were checked against the FTB Chunks and Architectury sources for 1.21.1, but the hook itself is only unit-tested for its decision; if it fails to register, the log says so and the old warning returns.

## [1.89.0] — The deeper (or higher) you go, the stronger and smarter the mobs get

Owner brief: "make it so the higher/lower you go in a dungeon the stronger and smarter the mobs will get." Full description: `docs/labyrinth/depth.md`.

### NEW
- **Depth rule** (`labyrinth/mega/enc/Depth*.java`): every hostile mob born or loaded in the Mega-Dungeon gets bonuses from the depth of its floor (the distance from the Hub, the same for Holy floors above and Abyssal floors below). **Stronger:** health up to +400 %, damage up to +250 %, speed up to +22 %, armour up to +12, knockback resistance up to +0.6, follow range up to +24 blocks, on a saturating curve (floor 10: +12 % health, floor 100: +99 %, floor 1000: +308 %). **Smarter:** a mind level 0 to 6 (depth 6, 16, 40, 100, 250, 500), each level teaching one behaviour: it keeps hunting where it last saw you, calls the monsters near it, flanks with its pack, falls back and recovers when badly hurt, closes down a target that kites it, and goes for the weakest player; a deeper mind also thinks more often.
- Config `nusmp-labyrinth-mega-depth-server.toml` (`enabled`, `strength`, `smarts`, `strengthScale`, `shareForScaledMobs`, `mindFloors`), command `/multiverse mega dm depth [floor]`, self-test `enc.depth`, 13 unit tests.

### REPLACED
- Nothing.

### KEPT
- The Dungeon Master's threat scaling, the Mob Foundry's tier scaling and every boss's own scaling are untouched. Bosses, owned pets and summons, non-hostile animals and the Hub are not affected by the depth rule; mobs that already scale with the floor take half of the strength bonus (config) and the full mind.

### Honest notes
- Nothing has been run in the game. The numbers and the thresholds are my design; flanking and the close-down burst share the mob's navigation with its own attack goal and may look jittery on some mobs, and Tensura mobs that think with a brain may ignore a goal's steering for a pass.

## [1.88.0] — EliteTensuraAddon systems removed (at the owner's request)

The owner asked for everything built from the "EliteTensuraAddon" specification to be taken out. 1.85.0 and 1.87.1 never produced a release (their builds failed on tests), and 1.86.0 is the only published build that contained part of it.

### REMOVED
- **Races** (Lesser / Medium / High / Divine Saiyan, Primordial race, transformations), **records, titles and synergies**, **Nations and War**, the **Elite Forge** (stations, minigames, kits), the **21 elite skills and the evolution engine**, the **calamity and magicule world** (Gravebound Colossus, Lich Boss, Gaia Spike, Void Bolt, crystal blocks, ore conversion), and the **elite items** (Astral / Void / Fenrir / Runic weapons, Nightingale armour, potions, Potential Catalyst, Chronicle Book, Walpurgis Orb and Seal, crates and keys, 54 plushies).
- With them: their commands, key bindings, screens, config files (`nusmp-elite-<area>-server.toml`), lang keys, recipes, loot tables, models, textures, sounds, tests, generators and docs; the rows they added to the engraving tables, weapon class tables, sounds, worn-armour catalog, the global loot modifier list and the pickaxe tag; and the self-test prefixes from the CI smoke test. `EliteHooks` keeps only the coin wallet.
- Worlds that already contain the removed blocks, entities or items will lose them (unknown ids are dropped on load); player data written by the removed systems is ignored.

### KEPT
- The **Hub economy** (Tensura coins, Hub NPCs, quests, shop, Dungeon Map) and the **cyclic dungeon generation**: they came from the other specification, not the EliteTensuraAddon list.
- The **Canon Grimoire** remake and the **map-mod compatibility** (Xaero's waypoint sharing, FTB Chunks claim check, map colours and their guard test).
- Everything from 1.84.0 and earlier. The git history of 1.85.0 to 1.87.1 is the way back.

## [1.87.1] — Calamity bosses and the magicule world, elite items, crates and plushies, map-mod compatibility

Everything merged since 1.86.0. As with the rest of the EliteTensuraAddon inventory, the original source was not available: names are the inventory's, every number and rule is designed here and lives in config / data (docs: `docs/elite/world.md`, `docs/elite/items.md`, `docs/compat/maps.md`).

### NEW
- **Calamities, bosses and magicule** (`elite/world`): a per-chunk magicule density, five crystal blocks (small / medium / large bud, crystal cluster, Mother Rock) that grow with it, ore conversion, stronger "touched" mobs, and a calamity director (omen, rise, three phases, Calamity Core drop and ritual). Bosses: **Gravebound Colossus** and **Lich Boss** (with breakable phylacteries), plus Gaia Spike and Void Bolt entities. Public events `CalamityEvents`, `CalamityDamageEvent`, `CalamityKillEvent`. Commands `/multiverse elite magicule` and `/multiverse elite calamity`; config `nusmp-elite-world-server.toml`.
- **Elite items** (`elite/items`): six weapons (Astral Edge, Void Edge, Fenrir Sword, Runic Swords Renewed / Ancient / Mythical, with techniques and engravings), the **Nightingale** armour set with its set bonus, Potential Catalyst, small / medium / large Health Potions, Chronicle Book, Walpurgis Orb and Seal, **Common / Rare / Elite crates** with keys (loot-only) and data-driven reward tables, and **54 plushies**. Config `nusmp-elite-items-server.toml`.
- **Map-mod compatibility** (`compat/maps`): map colours checked for every block, a guard test for new blocks, waypoint sharing in Xaero's format (`/multiverse waypoint share|here|gate|return`), a check that warns ops when FTB Chunks can still claim the mod's dimensions, a minimap-avoidance helper for HUD layers, and `/multiverse compat maps`. Nothing imports or reflects a map mod.

### REPLACED
- Nothing. Two blocks that had no map colour (`arena_return_sigil`, `relic_forge`) and the two Nations blocks now have one; a vanilla map no longer looks through them.

### KEPT
- Everything in 1.86.0 and earlier. The Foundry lich mobs and the Lich Wand stay untouched; the new Lich Boss is a separate mob.

### Honest notes
- Nothing has been run in the game: boss balance, the crate opening animation, rune drag-and-drop, the waypoint chat line against a real Xaero's, the FTB Chunks config key names (from memory) and the sounds (synthesised, never listened to) are all unverified. The in-game self-tests `elite.world.*`, `elite.items.*` and `compat.maps` run in the CI smoke test.
- The four forge station blocks and the world / plush / crate blocks have computed ids, so the map-colour guard test cannot list them; their colours are set in code.

## [1.86.0] — Nations and wars, the Elite Forge, 21 elite skills with evolution, Canon Grimoire remake (also carries everything in 1.85.0, whose build was never released)

1.85.0 failed its build on one stale test (a records test asserted that `en_us.json` held no records text, which the lead had just merged), so it was never published. This release contains all of 1.85.0 below plus the next three EliteTensuraAddon pieces and the Canon Grimoire remake. As before, the original addon source was not available: names are the inventory's, every number and rule is invented and lives in config / data.

### NEW
- **Nations and War** (`elite/nations`, `docs/elite/nations.md`): found and level a nation, custom roles, treasury ledger (Tensura coins), perks, 58 contracts and bounties, reputation tiers, alliances and wars with phases, score and spoils, `/nation`, key N.
- **Elite Forge** (`elite/forge`, `docs/elite/forge.md`): four stations (Basic, Magisteel, Mythril, Dragonforge), four minigames (Hammer Timing, Magicule Flow, Rune Alignment, Temperature Forge), quality rolls Common to Mythical, engraving, specialisations, skill bonuses and per-player progress. Config `nusmp-elite-forge-server.toml`.
- **Elite skills and evolution** (`elite/skills`, `docs/elite/skills.md`): 21 skills (Gravity Well, Kamehameha, Static Chain, Ancient Soul, Iron Resolve, Meditation, Runesmith's Insight, Scavenger, Kaioken, Mechanical Eye/Hands, Crocodile Skin, Hecate, Void Edge, Basilisk Skin, Beelzebuth, Gaia with Pillars and Vein Bloom, Hephaestus, Ouroboros, Rapheal, Void Sovereign) and an evolution engine with 12 triggers (EP gate, kills, ability mastered, forge quality and count, held titles, records, item consume, resets and more); `/nusmp eliteskills` for admins.
- **Canon Grimoire models** (`tools/canon`, `docs/canon/canon_grimoire.md`): all 30 boss and arc-character books are rebuilt as 44-cube 3D books (rounded covers and spine, page block, ribbon, corner caps, glowing medallion with a motif per book), with new first-person, third-person, ground and frame poses. The inventory icon stays the old sprite.

### REPLACED
- The registered `charter_grimoire_<id>` model and texture (30 books) are replaced by the 3D version; the visible name "Charter Grimoire" becomes **Canon Grimoire** (item names, messages, lore, quests, advancements). Command `/multiverse charter` still works, `canon` is added as an alias.

### KEPT
- The old book art stays as `charter_grimoire_<id>_v1.json` / `_v1.png`; all ids, class names, config keys and NBT keys keep the word "charter", so old worlds load. Everything in 1.85.0 below.

### Tests
- Three tests that said the shared lang file must not hold these keys now check that the merged text is present (records, skills, forge).

## [1.85.0] — Tensura-coin economy and Hub NPCs, no more dead-end mazes, Saiyan and Primordial races, records and titles

The first four pieces of the EliteTensuraAddon inventory and the Hub economy brief (plan: `docs/elite/plan.md`). The original addon source was not available: the names are the inventory's, every number and rule is designed here and listed in the docs.

### NEW
- **Hub economy** (`elite/hub`, `docs/elite/hub.md`): Tensura coins (bronze, silver, gold, stellar gold) are the dungeon's currency. Mobs, boss Guardians, chests and rift chests drop coins scaled by tier and floor; the Hub never pays. **Quest Giver and Merchant NPCs** stand in the Sanctuary (villager model, own skins); the **shop** (45 offers: gear, food, utility) charges coins only, stock per player per day; **73 quests** (daily, per road, hunts, deliveries; kill, clear floors, deliver, reach floor) with a HUD tracker ("12/50 Tier 1 Abyssal Kills") and a dialogue screen; the **Dungeon Map** item (3D rolled map, also in the shop) shows the rooms of the floor you stand on with fog of war.
- **Cyclic dungeon generation** (`labyrinth/mega/cyclic`, `docs/labyrinth/cyclic_wfc.md`): new sectors are grown with a loop-closing pass (open doorways of neighbouring rooms are joined, short hallways link near ones, leaves are trimmed) and a placement lookahead that prefers rooms whose doorways line up. Measured on the shipped templates: dead-end rooms **32.6% -> 6.1%**, rooms on a loop **34% -> 80%**, boss chain and stairs untouched. Config `nusmp-labyrinth-mega-gen-server.toml` `[generation] cyclic` (on for new sectors only; sectors already generated keep the old generator forever).
- **Races** (`elite/races`): real Tensura races **Lesser, Medium, High and Divine Saiyan** (an evolution line with EP, kill, mastery, item rules) and the **Primordial** race (entered by a rite); transformation states (boosted, ascended, great ape at full moon, divine) as a state machine with a hotkey (`.`); `/multiverse elite race ...`.
- **Records, achievements and titles** (`elite/records`): **210 records** (20 condition types), **81 titles** with attribute/effect bonuses, **26 title synergies**, a mouse-driven Records screen (key `I` inside the Codex menus, button injected), toasts, `/records`. Other systems report to it through `RecordEvents`.

### REPLACED
- What the Mega-Dungeon pays: coins instead of Magic Stones (config `economy.currency = tensura_coins`, default). Magic Stone items, ids, saves and the old drop code are KEPT (`magic_stones` mode, `keepStoneDrops`).

### KEPT
- All old generation for existing sectors, the Black Market, the multiverse race buffs, the Tensura titles.

### Honest notes
- Nothing has been run in game. The shop, quest and map screens, the NPC spawn spots, the Saiyan evolution against Tensura's real race code, the hotkeys and the generated hallways are only type-checked and unit-tested; the in-game self-tests (`elite.hub`, `elite.races`, `elite.records`, `gen.cyclic`) run in the CI smoke test.
- Coin ratios bronze = silver/100 and stellar gold = 100 gold are my assumptions (only gold = 100 silver was in the mod). Tensura coin pouches are not counted.
- The 400 foundry mobs and the Guardian bypass the normal loot modifier, so coins for them come from death listeners; the skill-book loot modifier does not fire for them either (to be looked at).
- Counters for forge, war, Walpurgis and calamities in the records stay at 0 until those systems (not built yet) report to `RecordEvents`.

## [1.84.0] — Relics below Legend need no attunement, the Unbinding Seal, bows held the vanilla way

### NEW
- **Unbinding Seal** (`nusmp:unbinding_seal`, craftable: 2 amethyst shards, an ender pearl, a gold ingot and paper): hold it in one hand and an attuned relic in the other and use it; the relic is freed from its attunement (everything else on it, tier, sockets, engravings, charges, is untouched), the seal is used up (not in creative). 3D tablet model with its own display transforms.

### CHANGED
- **Normal, Special, Rare and Unique relics no longer need attunement** (your request: "normal-unique relics don't require attunements"). Only Legend, God and Genesis relics still do (`AttuneRules.REQUIRED_FROM`). The relic definitions are untouched; tooltips and the "needs attunement" prompts follow automatically, and the three-attuned-relics limit now counts only Legend and above.
- **Bows face the way a vanilla bow faces** (your report: "bows are still facing the wrong way"): the six mesh bows (Divine Mercy, The Dragon's Spirit, The Reaver, Embrasure, Legendary Bow and its curse form, all draw stages) were placed with their limbs along the sprite's main diagonal, so the belly pointed down-right and the bow lay across the hand. A vanilla bow shoots along the main diagonal like a sword points, its limbs lie on the other diagonal and its belly faces up-right; the meshes are now placed at 135 degrees (was 45) about the same middle. `tools/relics/bow_vanilla.py` and `BowDisplayGuardTest` carry the new angle.

### FIXED
- 1.83.0 (the System points and mouse window) never got its own release: its build failed on `ScreenBlurGuardTest` because the two new System screens called `renderBackground` inside `render` (the vanilla blur trap of 1.15.0). They now draw a plain dim themselves. Everything of 1.83.0 is in this release.

### KEPT
- All attunement data on existing relics, the five-second ritual for Legend and above, the sprite bows (they already used vanilla's model), all display blocks and sizes of 1.72.0.

### Honest notes
- The bow fix is derived from vanilla's bow geometry and the renderer's pose maths, not seen in game: if a bow still looks wrong, tell me which view (first or third person) and which way it faces.

## [1.83.0] — The System: you choose where each stat point goes; move and resize the window with the mouse

Your requests: "allow player to pick where each skill point for the dungeons solo leveling system go, allow them to move it around with mouse not key they should also be able to change the size of it as well" and "remove the key moving for the system only keep the toggle to turn on or off to move it".

### NEW
- **Free stat points.** A cleared floor quest now pays into a pool of free points (server config `system.pointMode = choose`, the default) and YOU decide where each point goes. A server-validated allocate request moves points from the pool into STR / AGI / VIT / INT / SENSE (cap per stat as before), the bonuses apply at once. `/multiverse mega system free give|take`.
- **Stat screen** (key `I`, or the MOVE WINDOW button): five rows with points, a bar, and what one more point really does (from the stat table); buttons +1 (shift-click 5), +5, MAX per stat, mouse wheel over a row to add or take back a point, RESET (config `system.allowRespec`, default on), tooltips. A "+N PTS" badge appears on the System window while points are waiting.
- **Mouse move and resize** (edit toggle key `U`): drag the window anywhere with the left mouse button (it snaps to the nearest of nine anchors and keeps its place across resolution and GUI-scale changes), drag a corner handle or use the wheel to resize (0.5x to 2.5x), RESET, DONE.

### REPLACED
- Moving the System window with the keyboard is **removed** (the arrow, Home and End keys, the key-edit mode and the one-time message that explained them). Only the on/off toggle for the mouse edit screen (`U`) and the window toggle (`Y`) remain.

### KEPT
- `pointMode = random` is the old behaviour (a point goes to a random stat). All old saves load (the free pool starts at 0); config keys `anchor`, `offsetX`, `offsetY`, `scale` keep their meaning (scale now goes to 2.5).

### Honest notes
- Nothing has been run in game: the two screens, the drag and wheel handling and the new payloads are only type-checked and unit-tested; the in-game self-test `system.points` runs in CI.
- Clicking the HUD window itself is not possible (the cursor is captured while it is drawn): use the stat screen key.

## [1.82.0] — 275 Unique Skill Lines, 3D skill books, passive toggles, tree backgrounds, fog of war

The owner's brief ("275+ unique skill lines, 3D books, Tensura-style passive toggles, thematic tree backgrounds, visibility lock, one active line") is an extension of the 1.62.0 Skill Tree engine; the dungeon half of the same brief (rifts, waypoint, WFC rooms, tier mobs, the D&D boss, the Mana Core) already existed (see `docs/skilltree/lines.md`).

### NEW
- **275 new skill lines in 11 franchises, 25 each** (The Exiled Heavy Knight, Tensura, Black Clover, Magi, Solo Leveling, DanMachi, Fairy Tail, TBATE, Tower of God, The God of High School, Omniscient Reader's Viewpoint), each of 19 to 24 nodes in a branching tree: 2 stances, 3 to 4 stat nodes, 6 to 8 active arts, 6 to 8 passives and 2 finishers. With the 8 hand-built routes that is **283 lines**. Written from `tools/lines/rows/<franchise>.py` by `tools/lines/gen_lines.py` (validated: kinds, ranges, icons are real 1.21.1 items, art-order variety).
- **Effect runtime** (`skilltree/line/`): one executor for each of 26 active kinds, 25 passive kinds, the stance and attribute nodes (`tools/lines/catalog.py`); a line is data, no per-node Java. Arts are cast from an **Arts bar** (keys `[` `]` `\`), cooldown and magicule cost per node; the single-equipped-line rule applies.
- **Tensura-style Passive Toggle panel** (key `;`, a button next to the Skill Tree button in the Black Clover menu): every bought passive or attribute node adds a switch (default ON), scrollable, stored in the player's skill-tree data; passives run only if the line is equipped AND the switch is on.
- **Visibility lock**: a player sees only the lines they own; operators (`hasPermissions(2)`) and creative players get an **ADMIN INSPECT MODE** toggle that lists every line (read-only for unowned ones, re-checked on the server).
- **283 thematic tree backgrounds** (15 themes: magma, void, stellar, forest, ocean, storm, ruin, blood, holy, frost, tower, city, dream, sand, iron; `tools/lines/gen_backgrounds.py`).
- **3D Skill Book** (`models/item/skill_book_3d.json`: boards, spine, pages, ribbon, corner caps, clasp; full display transforms), one cover per franchise (11), tinted by the line's colour. Lines the client does not know show their name and colour from `assets/nusmp/skill_books.json`.
- **Book loot**: books of any line from Abyssal mobs (existing modifier), Decennial Guardians (existing) and the new secret rift vault chests (`nusmp:rift_books`), cached weight table (no list per drop), pity rule (the next book is a line you do not own yet).
- Schema fields `franchise`, `gui_background`, `book_texture`, `background_theme`, node `type`/`kind`/`params`; routes also load from `data/<ns>/unique_skills/`.

### REPLACED
- The registered `skill_book.json` model is now a `separate_transforms` wrapper (3D in hand and on the ground); the old 2D model is kept as `skill_book_2d.json` and is still the inventory icon.
- Server route limit: 1024 routes / 64 MB (was 64); the client receives only the lines it owns plus name cards.

### KEPT
- Every old route (Fire/Lightning/Sky Dragon Slayer, Ice-Make, the four Heavy Knight routes), the old book item and sprite, the Black Market, SP, and all saved unlocks.

### Honest notes
- Nothing has been run in game. The Arts bar, the toggle panel, the 3D book transforms and the `separate_transforms` loader are only type-checked and unit-tested.
- The 275 lines share about 50 effect kinds; what differs is names, numbers, enums, stances and combinations, not hand-coded mechanics. There is no summon, burn or freeze kind yet.
- The Exiled Heavy Knight lines are game-system archetypes invented in the spirit of the story, not canon skill names; some TBATE, Tower of God and God of High School lines are invented.
- All book and tree art is generated; the display angles of the book are my own reading of the pose maths.

## [1.81.0] — Smoldering Fang of Insanity is its own skill route (not the Fire Dragon Slayer's)

### NEW
- **Route `nusmp:smoldering_fang_of_insanity`**, the fourth route of The Exiled Heavy Knight (next to Oath of the Heavy Armor, Basic Swordsmanship and Lady Luck), with its own book (*Smoldering Fang of Insanity*), class and six nodes: **Ember Bite**, **Rabid Fangs** (passive: harder blows at low health), **Ash Skin** (passive: foes that strike you wither), **Fit of Insanity** (Strength and Speed, but you take more damage), **Howl of Ruin** (weaken, slow and shove back a crowd) and the Secret Art **Fang of Insanity** (leap and a huge withering bite that heals you). Four new skills (`hk_ember_bite`, `hk_insanity`, `hk_howl_of_ruin`, `hk_fang_of_insanity`), two passives, lang, tests (`HkRouteFilesTest` now checks that the Fang is not the Fire Dragon Slayer's), docs `docs/skilltree/hk_smoldering_fang.md`.
- Like the other Heavy Knight routes it works only while it is the equipped route.

### REPLACED
- The Fire Dragon Slayer's book is now called *Heart of the Fire Dragon* (it carried the Fang's name before). The Black Market's flagship line (`fang_of_smoldering_insanity`, the dearest book) now sells the Fang route's book.

### KEPT
- The Fire Dragon Slayer route (all seven techniques), the other routes, and every saved unlock.

### Honest notes
- The names are from my own knowledge of the work and the numbers are a first guess; not run in game.

## [1.80.0] — The Mob Foundry: 400 custom dungeon mobs, 200 per road, all on player-rig models

### NEW (nothing existed before: the 400 custom mobs had not been built)
- **400 mobs: 200 Holy (ascension) and 200 Abyssal (descent), 40 per tier on each road** (`tools/foundry/rows/*.txt`, one line per mob: name, faction, build, palette, source, look, archetype, 2 to 3 abilities). Every one is an art-kit character on the player rig (the Old Witch standard), with creature parts (horns, wings, tails, halos) for non-humans, its own palette and glow texture.
- **One entity type `nusmp:foundry_mob`** whose identity is a synced string; 10 archetypes with six style goals (melee, hit-and-run, ranged, support, charge, air); 53 abilities over 25 kinds as data (only Light/Dark Rukh particles); scaling by tier and floor; summoner minions that drop nothing; faction extras on drops (Magic Stones and gear through the standard listeners); a faction turf-war goal.
- **Dungeon integration:** each monster of an encounter becomes, at the configured weight (default 60 %), a foundry mob of the floor's tier and the sector's road; the room's requested faction is 4x as likely. The old tables still supply the other 40 %.
- **Variety guard** (`tools/foundry/guard.py`, `test_guard.py`): fails on any duplicate name, id, look signature or palette, any two mobs that differ in fewer than 3 of 16 structure groups (closest pair of 79,800 differs in 3), a tier that is not exactly 40 per road, or a source above 8 %.
- **Sources:** 194 different (45 anime and game series, largest Berserk at 4 %, plus 149 D&D monsters and classes counted each as its own source; read as one D&D family it is 178 mobs, 44 %, so say if you want that balance changed).
- Config `nusmp-labyrinth-mega-foundry-server.toml`; command `/multiverse mega foundry list|spawn|count|verify`; five self-tests (`foundry.*`, now required by the CI smoke test); lang fragment merged; docs `docs/labyrinth/foundry.md` and `foundry_report.txt`.
- Size: 4.6 MB deflated in the jar (17.4 MB uncompressed); clips are shared by 107 animation classes.

### KEPT
- Every existing mob, table and renderer. The mechanics library (1.79.0) can now use foundry variants in `mechanic_groups.json`.

### Honest notes
- Not run in game: AI and ability feel (cast timing, charge, hover), the real 60 % spawn mix and the live renderer. Tiers use the repo's `DndRules.tierOf` (five tiers of 200 floors), not "20 levels".
- A plain `/summon nusmp:foundry_mob` becomes a random roster mob on its first tick.

## [1.79.0] — 1000 dungeon mechanics from 48 anime and D&D sources

### NEW (everything; nothing replaced)
- **1000 data-defined dungeon mechanics** (`data/nusmp/mega/mechanics/*.json`, generated by `tools/mechanics/gen_mechanics.py` from 159 hand-written base designs, each with 5 to 6 variants changing element, trigger or counter): 125 each of floor, puzzle, hazard, combat, risk, boss, travel and party mechanics.
- **48 sources, none above 8 %:** 40 anime (Black Clover, Fairy Tail, Tensura, Tower of God, Hunter x Hunter, Naruto, Solo Leveling, Made in Abyss, SAO, Frieren, Fate, Overlord, Re:Zero, Berserk, Attack on Titan, Magi, Dungeon Meshi, JJK, KonoSuba, Dragon Ball, DanMachi and more) and 8 D&D books (DMG, Monster Manual, PHB, Tomb of Horrors, Xanathar's, Ravenloft, Planescape, Out of the Abyss).
- **Engine:** 80 effect primitives, 18 triggers, 19 counters, a seeded Director (threat budget `8 + 4*tier`, 4 per floor, 1 per room, conflict table, theme and road weights), never in safe rooms, the Hub or rift pockets. All block changes are journalled and restored (also after a crash). Reward chests only open for the party that beat the counter. Only Light/Dark Rukh particles.
- **Config** `nusmp-labyrinth-mega-mechanics-server.toml` (`enabled`, `perFloor`, `perRoom`, `difficulty`, eight category switches, `blacklist`). **Command** `/multiverse mega mechanic list|info|apply|clear|reset|count|verify`. 3055 lang keys. Docs `docs/labyrinth/mechanics.md`.
- Tests: 28 JUnit tests plus 7 in-game self-tests (`mech.*`), which the CI smoke test now requires.

### KEPT
- Traps, riddle rooms, room kinds, Dungeon Master, safe zones, rifts and Guardians untouched. With `[mechanics] enabled=false` the dungeon is as before.

### Honest notes
- The engine has never run in game; primitive timings, platform riders and water numbers are balance guesses. The first real run is the CI smoke test.
- The foundry mob names in `mechanic_groups.json` are placeholders until the 400-mob foundry lands.

## [1.78.0] — The mobs are remodelled on the player rig (27 mob ids, 33 characters, 42 models)

### REPLACED (new look only; entity ids, stats, AI and hitboxes unchanged)
- Zogratis four (Dante, Vanica, Zenon, Lucius, each with a second form; Lucius's divine form has three pairs of wings; the clone wears Lucius), dark disciples (four devil skins), Devil Avatar, God of Order, Goliath, Charybdis Spawn, Floor Warden and Labyrinth Guardian, Living Armor, Tower Wizard, Zagred's legacy (phase 4 with the trident), Grimoire Daemons (4 tiers), Painted constructs (hound, knight, giant), Cotton sheep/cook/cloud, Rouge (sitting pose; the head-cat is the kit Rouge), Blood Beast, Dungeon Direwolf, Basilisk, Dungeon Slime, the Spirit Lords (salamander, undine, sylph, gnome, black devil).
- Creatures became anthropomorphic characters on the Old Witch quality rig (wolf-man, serpent-folk, slime child, dragonfolk, sea titan, cloud-face on a wool heap).
- New renderers in `client/remodel/` (`RemodelTable`, `KitMobRenderer`, `KitBossRenderer`, `KitCloudRenderer`, `KitZagredRenderer` keeps the aura shader); one line in `NUSMP.java` registers them last so they win. Docs: `docs/arc/remodel.md`.

### NEW
- The Spirit Lord companion is now a full body (was an orb), the wind spirit lord has a body (its renderer drew nothing), and a black devil spirit.

### KEPT
- All old models, textures, generators and renderer classes stay on disk (delete a row from `RemodelTable.ENTRIES` to get a mob's old look back). `mirror_double` and `player_avatar` stay as they are (they should look like the player).

### Honest notes
- Not run in game: the scale against each hitbox, light on dark models, and the "last registered renderer wins" assumption. Dropped for now: Goliath's per-move clips, Undine/Sylph overlays, equipment on Living Armor, painted-beast genome heads. The cotton cloud is bigger than its hitbox, so riders sink in a little.
- About 13 MB uncompressed (1.9 MB deflated) of new models and animations.

## [1.77.0] — The skills are rebuilt: 38 skills, real mechanics, distinct Ultimates, unique icons, chanted spells

### REPLACED (old versions of the same skills; ids, saves and config keys kept)
- **Every custom skill class** (25 Uniques/Extras, 10 Ultimate evolutions, 3 standalone Ultimates) rewritten on shared bases (`NUSkill`, `SkillTargets`, `SkillDamage`, `SkillZones`, `Summons`, `SkillEvents`) plus 14 Minecraft-free rule classes in `skill/rules/`.
- **Ultimates are now different skills, not flagged copies:** Lord of Summons (double cap, stronger cards, Legion and Wraith modes), Goddess of Faith (Blessing, Miracle), Hero's Argonaut (ten bells, Hero's Beam), Hell Conqueror (life steal, hellfire aura, berserk), Immortal Will (rises at full health, shockwave, Rally), Adolla Burst (Light Speed plus explosion mode), Gold Experience Requiem (Life Burst), Black Wings (full reflection, no fall damage, glide); All Fiction and Limitless rebuilt with their mode sets.
- **The five DanMachi spells are chanted** (hold the key, a line every 14 ticks, 6 when mastered, release early = broken chant, free). Blizzard, flame pillars and gravity linger through a zone scheduler. This is a play change from the old instant cast.
- **Targeting and balance:** area effects hit only foes (not pets, villagers or other players' summons); heals and buffs reach players and the caster's creatures; damage, hard control and healing go through `BalanceLaw` (EP scaling, PvP hit cap).
- **Summons:** capped, tracked by UUID, timed, steered, never auto-target players, removed when the owner dies or leaves.
- The Visionary's meteor no longer uses `level.explode` (no vanilla particles or fire); damage and sound stay.

### NEW
- **38 unique 32x32 skill icons** (`textures/skill/<id>.png`, `tools/skills/gen_skill_icons.py`), 197 lang keys (98 new, 36 descriptions changed), `docs/skills/rebuild.md`, tests (`skill/rules/*Test`, `SkillAssetsTest`).

### KEPT
- All registry ids, the Unique to Ultimate evolution table, saved data (`Faith`, Infinity switch, `H0..H4`, `Paid`, summon tags), config keys, Time Magic and Grimoire files untouched.

### Honest notes
- Written against API stubs, not run in game: summon steering, Vex summons, Black Wings deflection, zone effects and the chosen VFX shapes need an in-game look. A few icons (Hero's Argonaut, Book Maker's screw) are simple at 32 px.
- The summon ledger is in memory: after a restart it refills from entities near the owner.

## [1.76.0] — 102 more relics: the 300 are done (KonoSuba, The Eminence in Shadow, Tower of God, Fullmetal Alchemist and Berserk, Sword Art Online and Log Horizon)

### NEW
- **102 relics in five more series** (waves 42 to 46): KonoSuba (19), The Eminence in Shadow (19), Tower of God (18), Fullmetal Alchemist and Berserk (19), Sword Art Online and Log Horizon (27). Each has its own sprite, palette and worn ornament.
- **Total of the relic request: 298 new relics** in 16 waves (Re:Zero, Made in Abyss, Tomb Raider King, Jujutsu Kaisen, Fate, the rifts, Overlord, Frieren, Delicious in Dungeon, Solo Leveling, Mushoku Tensei and these), all different in sprite, palette and worn look (`RelicUniqueLooksTest`), all passing through the God of Order's ledger from Unique up, and findable in the hidden treasure rooms up to Rare (1.75.0).
- The names are inspired by the works from my own knowledge of them (I could not look the items up); the rules text is the mod's own. No weapons or consumables yet (they need meshes and hand models).

### KEPT
- Everything older.

## [1.75.0] — Relics can be found in the hidden rooms of ordinary floors

Your request: "relics can also be found in hidden rooms in regular floors, not just dungeons". I read it as: the Mega-Dungeon's ordinary (non-boss) floors, whose hidden rooms are the treasure rooms. If you meant something else (new secret-door rooms), say so.

### NEW
- The 15 treasure rooms of the Mega-Dungeon (three closets and twelve themed nooks and vaults) now roll the new loot table `nusmp:chests/hidden_cache`: the old trial loot plus a **30% chance per chest of one relic of Normal to Rare grade** (any of the 196+ generated relics and the older ones of those grades, through the God of Order's ledger so none is duplicated).
- Rule kept: relics of **Unique and above stay the Crucible's alone** inside the Mega-Dungeon (one per region); a cache never rolls above Rare (`RelicExclusivity.allowsWildRoll(inMega, maxGrade)`), and Mimics still drop no relic there.
- Guards: `RelicCacheTemplatesTest` (every treasure chest rolls the cache; the table's grades stay at Rare or lower), `RelicExclusivityMegaTest` (the grade rule), `tools/labyrinth/retarget_treasure_loot.py --check`. Docs: `docs/labyrinth/hidden_caches.md`.

### KEPT
- The old loot table, all room templates and the Crucible unchanged.

## [1.74.0] — Crash fix, and 98 more relics (Overlord, Frieren, Delicious in Dungeon, Solo Leveling, Mushoku Tensei)

### FIXED
- **Server crash (your crash report: `ConcurrentModificationException` in `ExpansionTrigger.onServerTick`).** Cause: the once-a-second sweep that starts a waiting expansion looped over the live list of sectors while `requestPending` queued the next sector, which adds to that same list. Fix: it loops over a copy. I checked the other loops over the sector list: they only read.

### NEW
- **98 relics in five series**, each with its own sprite, palette and worn ornament (guarded by `RelicUniqueLooksTest`, now covering waves 31 to 80): Overlord (20, wave 37), Frieren (19, wave 38), Delicious in Dungeon (19, wave 39), Solo Leveling (20, wave 40), Mushoku Tensei (20, wave 41). With waves 31 to 36 that is 196 generated relics so far; about 100 more to reach the ~300 you asked for. Names are inspired by the works from my own knowledge of them; the rules text is the mod's own. The relic wave loader now reads waves up to 80.
- No weapons or consumables in this batch (those need their own meshes and hand models).

### KEPT
- Everything older. Not done yet from the relic request: hidden relic rooms in ordinary floors.

## [1.73.0] — Dimensional rifts, hidden pocket realms and the last-location waypoint

Your new brief (Rift Type A and B, the Return Portal and the Last Location Waypoint). **All of it is NEW: nothing like it existed, so nothing was replaced.** I cannot run the game here: the rules are unit-tested and the real-game paths have self-tests that the CI smoke test runs, but the look of the rift in particular has not been seen by me, so tell me what you see.

### NEW
- **The rift** (`nusmp:dimensional_rift`, no item): a vertical tear drawn by a block entity renderer: a window onto another place (End star field / the mod's crimson void / End gateway field), a bright rim and jagged electrical arcs that re-roll every 3 ticks, a soft glow, Light/Dark Rukh aura particles; opens, holds, gutters, closes.
- **Type B, shortcut rifts:** jump 3 to 5 floors on, never past a boss floor's seal; a rift whose destination is not built yet is dim and has the floors built for it.
- **Type A, pocket rifts:** an instanced hidden realm (a cell of a far field of the Mega-Dungeon's dimension; a second dimension per rift cannot be made at run time) with either a secret boss or a puzzle vault, a vault of three chests with the dungeon's loot tables (the rare loot), a collapse timer and a clean-up. Whoever enters the same rift shares it.
- **The waypoint** (your `PlayerDungeonWaypoint`; dimension, level, floor, position, rotation) kept in the player's persistent data (NeoForge 1.21 has no capability for this), sampled every 2 s, honoured only on a floor of the player's own party.
- **Relics (wave 36):** Return Crystal, Gate Key (also opens a Return Portal on Floor 0), Rift Compass, Pocket Key, Waypoint Anchor, each with its own sprite, palette and worn ornament.
- Config `nusmp-labyrinth-mega-rift-server.toml`, commands `/multiverse mega rift ...`, self-tests `rift.*`, docs `docs/labyrinth/rifts.md`.

### KEPT
- Everything else. The Labyrinth Guardian gained one method (`configureForFloor`) for the pocket's secret boss; its other behaviour is unchanged.

### Not done
- Rift Type B jumps are held at boss floors on purpose; tell me if you want them to cross cleared boss floors.
- The Tier / "Metal Vessel" / 15-mob / WFC systems of the same brief already exist from 1.54.0 to 1.58.0 and were not rebuilt.

## [1.72.0] — Every bow is the size and pose of Minecraft's bow

From your screenshots: the brown bow (vanilla's) looked right and the golden Legendary Bow did not. I could not look at the game here, so this is found by measuring the files.

### FIXED
- **Cause:** the mesh bows were 1.76 to 2.58 blocks long tip to tip (the Legendary Bow 1.76; the Reaver, Divine Mercy, the Dragon's Spirit and Embrasure 2.58), while vanilla's bow is about 1.15. So in first person they filled the right third of the screen and ran off its edges, and in third person they stood taller than the player. The display transforms were already vanilla's (1.63.0); the size was not.
- **Fix:** all bows (the Legendary Bow and its Curse form, the Reaver, Divine Mercy, the Dragon's Spirit, Embrasure, and every draw stage `_pull1..3` of each) are scaled to 1.2 blocks, centred where vanilla's sprite is held, so the same draw animation, hand position and pose now show a bow of vanilla's apparent size. The class table's bow range is now 0.9 to 1.4 blocks (was 1.3 to 1.6), because the runtime would otherwise have scaled them back up.
- Guard: `BowDisplayGuardTest.everyBowIsTheSizeOfTheVanillaBow` fails if any bow mesh is outside 0.9 to 1.4 blocks; `tools/relics/bow_vanilla.py` does the sizing (idempotent, `--check`).
- The white "X" in the middle of your screenshot is a fired arrow seen from behind (vanilla's arrow is two crossed planes); that is not changed.

### KEPT
- Every bow, its meshes, glow, forms, engravings and abilities; only the scale and the position on the diagonal changed. If a bow now looks too small or too big to you, say which and by how much: the size is one number (`BOW_LENGTH`).

## [1.71.0] — Relics no longer hide under the skin's second layer; the Vassal Claw and Gauntlet are worn on the hand

I cannot run the game here, so both fixes are built from the geometry and unit-tested, not looked at. If either needs a nudge, tell me what you see.

### FIXED
- **Relics showed under the player's skin layers (jacket, sleeves, pants, hat).** Cause: every on-body relic model is drawn at 85% size (or 80% for shoulder and bracer pieces, moved toward the body) about its body part, which sank their surfaces into the skin's outer layer, so the layer covered them. Fix: `WornMesh.clearOf` moves only the vertices that would land inside that layer (0.25 px on the body, arms and legs, 0.5 px on the head) out through its nearest face, accounting for the shrink and the mirrored left arm; a model that already stands clear is not changed. Animated parts (orbiting motes) are left alone. Test: `WornMeshClearanceTest`.
- **Vassal Claw and Vassal Gauntlet were held like swords, floating in front of the fist.** Now, in third person, the bracer sits round the forearm and the hand plate round the fist (`HandWrap`: the mesh is turned so its forearm axis runs along the arm, scaled to the arm's 4 x 4 pixel section, mouth 2-3 pixels below the shoulder; the left hand wears the same glove turned about the arm). Their model json has identity third-person display entries and `nusmp_display_preset: manual`. In first person vanilla draws only the item (no arm), so that view is unchanged. Test: `HandWrapTest`.

### KEPT
- All meshes, forms, skills and models of both weapons; only where they are drawn in third person changed. The other Vassal weapons are still held (a claw and a gauntlet are the ones that are worn).

### NOT DONE YET
- The skills list you sent (Summoner, Faith, Firebolt ... and the Ultimate evolutions): "remove/rebuild these" needs a decision from you before I touch them (see my reply).

## [1.70.0] — 95 new relics from the anime and webtoon worlds, and every relic looks different

First batch of the "about 300 more relics" request: **95 relics in five series**, each with its own sprite, its own palette and its own worn ornament. Names and places come from the works; the rules text is the mod's own, and the item lists come from my own knowledge of the series (the web searches I ran returned no usable item lists), so a few names are "inspired by" rather than exact.

### NEW
- **Re:Zero (20, wave 31)**: Echidna's Tea Set, Gospel of the Mansion, Puck's Crystal Pendant, Gloves of the Unseen Hand, Oni Maid's Horn Circlet, Sword Saint's Mantle, Shard of the Witch Factor, Pocket Watch of Return, Roswaal's Jester Cloak, Forbidden Library Key, ...
- **Made in Abyss (21, wave 32)**: the Red / Blue / Moon / Black / White Whistles, Star Compass, Cradle of Desire, Incinerator Arm, Zoaholic Ring, Curse-Repelling Vessel, Veil of the Princess of the Gate, ...
- **Tomb Raider King (18, wave 33)**: the gods' tomb relics (Odin's All-Seeing Eye, Gungnir's Shadow, Mammon's Vault Ring, Ra's Dawn Crown, Anubis, Thoth, Osiris, Set, Horus) and Hunter gear.
- **Jujutsu Kaisen (17, wave 34)**: Sukuna's Finger and Sukuna's Twenty Fingers, Limitless Blindfold, Prison Realm Cube, Black Flash Gloves, Binding Vow Seal, Ten Shadows Seal, ...
- **Fate (19, wave 35)**: Holy Grail Fragment, Command Seal Tattoo, Rule Breaker, Avalon, Gae Bolg, Gate of Babylon Crest, Unlimited Blade Works Brooch, Servant catalysts, ...
- The unique and higher ones go through the God of Order's one-per-world ledger like the older ones (and the `nusmpGodOfOrder` gamerule and `/relic order unique` list apply to them).
- Generated from tables (`tools/relics/relic_rows_*.py` -> `gen_relic_waves.py`), so the next batches are cheap to add; no weapons or consumables in this batch (those need their own meshes and hand models, they come later).

### NEW: unique looks
- Every one of the 95 wears its slot's look **plus an ornament of its own** (`tools/relics/gen_worn_unique.py`: two or three motifs from spikes, gem clusters, rings, circling motes, feather fans, hanging chains, crescents, rune bars, horns and flame tongues, picked and sized by a seed made from the id), a **different sprite** (the icon generator varies each one by its id and bumps until it is new), and a **different palette**.
- `RelicUniqueLooksTest` fails if two generated relics ever share a sprite, an ornament or a palette.

### KEPT
- Every older relic, look and model is unchanged (nothing replaced). Not done yet from the same request: hidden relic rooms in ordinary floors, and the other ~200 relics.

## [1.69.0] — Bosses no longer take random damage outside the dungeons

The owner reported that bosses take random damage when they are spawned outside the dungeons. **I could not reproduce it (nothing here runs a game), so this fixes the likely cause and says so.**

### FIXED
- **Cause (most likely):** the dungeon halls are built so that no world damage can happen there; outside, a big boss (the Labyrinth Guardian is 5 blocks tall and more) stands half inside a tree or a cliff and suffocates, is crammed, falls from its spawn height, wades into water, touches a cactus, a berry bush, a stalagmite or magma, freezes in powder snow, is struck by a storm's lightning or (the undead) burns in daylight. None of it has an attacker, so it looks random.
- **Fix:** a boss of the mod (`BossIdentity`: the Zogratis family, Zagred, the Floor Warden and Guardian, the dungeon floor bosses, the arc bosses tagged `nusmp:boss`) takes none of that world damage, only when nothing attacked it. Real fights are untouched (weapons, magic, lava, the void, `/kill`, and fire or drowning that a player caused). New file `boss/rules/BossEnvironment`, one switch `bossRules.environmentImmune` (default on; false = the old behaviour).
- If a boss still loses health unexplained outside the dungeons, tell me which boss, how it was spawned and what was around it.

## [1.68.0] — The God of Order's rulebook: a gamerule and a one-world command list

The owner asked for a gamerule that disables the God of Order entity and for a command list on the God of Order that sets what is and is not one world only. **Nothing has been run in a game client.**

### NEW (nothing replaced)
- **Gamerule `nusmpGodOfOrder`** (default true). Off: the God never appears, a visit in progress ends, the waiting list is dropped, `/multiverse relic order summon` refuses, and nothing is taken.
- **`/multiverse relic order unique ...`** (permission 2): `list`, `show <id>`, `set <id> one_world|many`, `reset <id>`, `reset_all`, `grade <grade> one_world|many`, `all one_world|many`. The list is saved with the world (`nusmp_relic_unique_rules`, only what an operator changed) and plugged into `RelicDef.isUnique`, so the ledger, the scanner, the loot roll, the commands and the vaults all obey it. A relic set to `many` can be rolled from loot as often as it comes up; a relic set to `one_world` after copies exist is not purged (the first copy found becomes the valid one).
- By default nothing changes: every relic but a consumable is one world only, as before.

## [1.67.0] — D&D's famous bosses in the dungeons

The owner asked for famous D&D bosses in the dungeons. **Nothing has been run in a game client.**

### NEW (nothing replaced)
- **Seventeen famous bosses** as a second pool of Decennial profiles (`data/nusmp/mega/dnd_bosses/famous/`): Beholder, Elder Brain, Strahd, Lolth, Zariel, Kraken, Demogorgon, Asmodeus, Orcus, Acererak, Tiamat, Vecna, the Tarrasque, Bahamut, the Solar, a Dracolich and a Lich King, each with its signature legendary actions, lair actions, breath weapon, anti-magic aura or Word of Ending, and an anime echo.
- **How they appear:** a sector boss is one of them with the server's chance (`[guard] famousPercent`, default 50; 0 = only the twenty ordinary bosses), chosen from those that fit the floor's tier and road, fixed by the floor and the sector so it never changes after a reload.
- **Five new legendary actions:** dragon breath (fire, frost, lightning, poison or acid, telegraphed), eye rays, mind blast, death touch (a marked circle to step out of) and vampiric bite; and the Tarrasque's Reflective Carapace.
- Tests: `DndRulesTest` (the files, tier coverage of both roads, the stable pick) and the in-game `guard.dnd`. A Guardian with `dnd = false` is unchanged.

## [1.66.0] — More of the D&D boss kit

The owner said "add more" after the D&D Decennial boss system. **Nothing has been run in a game client.**

### NEW (nothing replaced)
- **Four legendary actions:** counterspell, life drain (half becomes a shield, never health), haki pulse (a save against DC 15) and gravity crush.
- **Two lair actions:** falling stars (five telegraphed strikes) and necrotic mist (a damage zone for 7 s).
- The twenty profiles in `data/nusmp/mega/dnd_bosses/` carry them by side and tier (Holy: counterspell and stars; Evil: life drain and mist; dragons: haki pulse; tiers 4 and 5: gravity crush and both zones). The generator, the id lists, the lang and `docs/labyrinth/dnd_boss.md` follow. A Guardian with `[guard] dnd = false` is unchanged.

## [1.65.0] — The D&D Decennial boss system

The owner asked for the multi-phase Decennial boss system of the Mega-Dungeon spec: D&D lethality (legendary actions, lair actions, legendary resistances, mythic phase-shifting, Power Word Kill, anti-magic fields) fused with anime spectacle (Acnologia-style magic absorption, True Dragon pressure, the Anti-Magic Devil). **Nothing has been run in a game client**; the numbers are first guesses. Docs: `docs/labyrinth/dnd_boss.md`.

### NEW — a layer on the Labyrinth Guardian (`labyrinth/mega/guard/Dnd*.java`)
- **Not a second boss:** the Guardian already carries scaling, the theme trait, the leash, the damage budget and the Mana Core / Gatekeeper chain, so the layer is one `DndBoss` object per Guardian that the entity calls from its tick, hurt, death and save.
- **Rules:** Legendary Resistance (2 to 5 charges, effects turned away through `MobEffectEvent.Applicable`), Legendary Actions (3 to 5 points per 6 s round, spent in reaction to a player's blow), Lair Actions (tremor, searing light, anti-magic field, each telegraphed), three phases with a **Mythic Trait** (the first death is cancelled once; `/kill` and the void still end it), an anti-magic aura, magic absorption into a shield, frightful presence (a d20 save against DC 15) and the **Word of Ending** (only in the mythic form of tier 3+ bosses: names a player under 35 % health, marks them for 3 s, kills them unless they heal, break sight or interrupt the boss).
- **Data:** twenty profiles (`data/nusmp/mega/dnd_bosses/`, ten Holy and ten Evil, five tiers of two) that name the anime and the D&D creature each boss fuses, loaded by a reload listener so a datapack can retune or add one. The floor picks the profile; the boss bar shows its name and form.
- **Config:** `[guard] dnd` (false = the 1.64 Guardian) and `execution`.
- **Tests:** `DndRulesTest` (rules, the twenty files) and the in-game self-test `guard.dnd`.
- **Kept:** the Guardian's traits, scaling, leash, budget, altar, Mana Core and Gatekeeper are unchanged; a Guardian made with `dnd = false` behaves as in 1.64.

## [1.64.0] — Seven new functional rooms for the Mega-Dungeon (Dungeon Keeper style)

The owner sent the Mega-Dungeon spec again with two room-idea references (the Dungeon Keeper room list and a dungeon-room-ideas article; the sites could not be opened from the build session, so the rooms follow the Dungeon Keeper catalogue as remembered) and chose "new functional room types". The dungeon already had the barracks, cells, torture chamber, crypts, libraries, alchemy labs, forges, kitchen and shrines; what was missing is added next to them, **nothing replaced or removed**.

### NEW — `structure/mega/hall/*` (generator `tools/labyrinth/gen_mega_rooms3.py`, same 3 x 2 module footprint and doorways as the Soldiers' Quarters)
- **hatchery_a** (fenced livestock pens with hay, eggs and water, feed troughs; beast faction), **training_hall_a** (a sand ring, straw dummies on posts, archery targets, weapon racks; goblin faction), **beast_den_a** (the Lair: iron-bar cages, hay, bones, blood; beast faction), **temple_a** (an idol, a sacrificial slab, pews, braziers; undead faction), **graveyard_hall_a** (the Boneyard: graves with headstones, a dead tree; undead faction), **guard_post_a** (a watch desk, bunks, an armour stand, weapon racks; goblin faction) and **casino_hall_a** (felt tables, a bar counter with a merchant marker).
- They are in the `hall` folder, so the pools pick them up with no Java (the folder's share of the pool is fixed, so they add variety without changing the mix); `gen_mega_pools.py` and `mega_catalog.py` were re-run. Every room passes the template checks (walk graph, routes, light, leaks, markers); the 54 catalog and assembler tests pass.
- **Not run in a game client.** The boss spec of the second message (Decennial bosses with legendary actions, Mana Cores, Metal Vessels) is already covered by the Labyrinth Guardian traits, Mana Core and Decennial rewards of 1.55 to 1.58; it is audited, not rebuilt, here.

## [1.63.0] — Bows held like the vanilla Bow (and guarded), and the Exiled Heavy Knight skill routes

The owner reported (screenshot 81) that the 1.62.0 Legendary Bow stood up in front of the player like a vertical plank with a mis-posed arm, asked for the fix on every bow of the mod and for a guard against it happening again ("now make all bow base items use the same as base minecraft bow"), and asked for the skill trees of *The Exiled Heavy Knight Knows How to Game the System* ("like the Lady Luck"). **Nothing has been run in a game client**: the bow poses and every route number are first guesses that need a player.

### Bows (REPLACEMENT of the old bow display; nothing removed)
- **Cause:** the Legendary Bow's model json carried a hand-solved third-person display (`rotation [75, 90, 0]`) that tipped the upright mesh over, while the relic bows used other values; no check compared them.
- **Fix:** every bow of class `bow` (Legendary Bow and its Curse look, The Reaver, Embrasure, Divine Mercy, The Dragon's Spirit, with all their draw-stage meshes) now takes **vanilla's own Bow display** in all four hand views, and its meshes are placed on the sprite diagonal (place angle 45, the middle of the bow at the sprite's grip), which is what the vanilla bow sprite does, so the pose, the arm and the size follow the vanilla bow. `tools/relics/bow_vanilla.py` writes both (idempotent, `--check`); `tools/heroweapon/art_hart1.py` now emits the same display. The old meshes' limb shapes are untouched; the crossbows keep their displays.
- **Arm:** a drawn bow of the mod is held in vanilla's bow-and-arrow arm pose whatever its use animation is (`WeaponRenderer.getArmPose`).
- **Guard:** `BowDisplayGuardTest` fails the build if a bow of class `bow` has a display that is not vanilla's, a mesh off the sprite diagonal, a draw stage that places differently from its rest mesh, or a grip above 0.5; and if a crossbow is tilted about x by more than 30 degrees.

### Skill routes (NEW) — franchise `exiled_heavy_knight`
- Three standalone routes, each its own book, class and file (`data/nusmp/skill_trees/`), grouped under "The Exiled Heavy Knight" in the Skill Tree screen; the existing Fire Dragon Slayer route already carries the book *Fang of Smoldering Insanity* and is kept as it is. Only the equipped route works (the engine's single-active-class rule and Equip button); a new franchise needs only a JSON file and a lang line.
- **Oath of the Heavy Armor** (tank): Provoke (taunt), Shield Wall, Shield Bash; passives Iron Hide (armor and knockback resistance), Bulwark (cuts blows while a shield is raised), Unyielding (a last stand under a third of health).
- **Basic Swordsmanship**: Power Strike, Swift Step, Crescent Cut; passives Sword Mastery (damage while a sword is held) and Riposte.
- **Lady Luck** (utility): Fortune's Favor (a Luck attribute while equipped), **Cat's Paw** (a chance, tilted by Luck, to take no damage from a blow), Lucky Strike (crits), Treasure Nose (a doubled drop), Gambler's Coin and the Secret Art Jackpot.
- **Fix of the engine:** the Sky Dragon Slayer and Ice-Make passives checked only "owns the route", so they worked while the route was not equipped; they now use the same gate as the casts (`SkillTreeApi.canCast`).
- Tests: `HkRouteFilesTest` (files, ids, fallbacks) and the in-game self-test `tree.hk.heavy_knight` (data wiring, and that the Luck modifier and the Riposte vanish when another route is equipped).
- **Not built (not asked):** the TBATE "Elemental Mana Core" tree of the spec; it needs only a JSON file plus its skills.

## [1.62.0] — The weapons of The Rising of the Shield Hero, and a Skill Book & Skill Tree engine for Fairy Tail magic

The owner asked for two things in one message. First, "the weapons of The Rising of the Shield Hero": the **four Holy (Cardinal) Weapons** (Legendary Shield, Sword, Spear and Bow) and the **seventeen Seven Star / Vassal Weapons** (Scythe, Fan, Carriage, Axe, Hammer, Gauntlet, Claw, Whip, Staff, Projectile, Katana, Mirror, Musical Instrument, Harpoon, Sewing Set, Chain and Book), with the series' mechanics: absorbing materials into a central gem to unlock forms, stats and skills; the Curse Series, unlocked by despair and tied to the seven deadly sins at a heavy cost; and the restrictions (Holy Weapons reject other weapons, are bound to the body, and give no experience to heroes who stand too close to one another). Second, a **Skill Book & Skill Tree system** inspired by *The Exiled Heavy Knight Knows How to Game the System* (rare skill books sold by an old witch in a hidden Black Market for millions of gold), as the foundation for anime magic systems, starting with Fairy Tail (Dragon Slayer and Maker magic): reading a book unlocks a whole route of a data-driven tree, Skill Points are spent on its nodes, and the tree sits in the existing menu. Docs: `docs/heroweapon/design.md` (the contract; then `core.md`, `forms.md`, `curse.md`, `gem.md`, one `weapon_<name>.md` and `art_*.md` per weapon) and `docs/skilltree/design.md` (then `core.md`, `items.md`, `gui.md`, `shop.md`, `ft_*.md`). Every class of both jobs says in its Javadoc why it is built that way (the owner's strict "WHY" rule), and each doc has a WHY section per file group and a list of the alternatives that were rejected.

**State:** written without a whole-project compiler by twenty builders working in parallel from two written contracts; each part was type-checked against the API stubs and the whole tree once more after the merge (only the known stub artefacts remain), the pure logic and every data file are covered by JUnit tests (run here on a small shim, and by the real JUnit in CI), and the CI smoke test boots a real server and runs the self-tests `hero.*` and `tree.*`. The 3D models and the sprites are software renders and the screens are layout mock-ups, not game screenshots. **Nothing has been run in a game client**: every number, effect, feel and the carriage's vehicle physics are first guesses that need a player.

### NEW — the hero weapons (`heroweapon/**`, `client/heroweapon/**`)
- **Twenty-one weapons, 225 techniques, 100 passives, 142 forms** (23 of the forms are curse forms). Each weapon is a real 3D model in the hand (the mesh method of the owner's tools, a painted skin, a glow skin with the weapon's gem, a 16x16 sprite kept as the inventory icon), carries **Tensura engravings** that fit its lore, and has its own page in `docs/heroweapon/` with the canon (from fan-wiki search summaries, each fact marked as summary, memory or design), every technique with its numbers, tell, answer, cost and cooldown, and an honest list of what is unverified.
- **The four Holy Weapons.** **Legendary Shield** (Naofumi; Wrath): a guard with a readable parry window and a Counter meter that soaked damage fills, party cover (Second Shield, Protect), control (Prison, Chain, Hook, Undead Control) and counter offence (Change Shield (Attack), Iron Maiden, Needle Burst), the Soul Eater Shield as a form, 15 + 4 Wrath techniques. **Legendary Sword** (Ren; Greed and Gluttony): a Tempo combo that builds speed and a Flow strike, ghost strikes after dashes, 14 + 5 curse techniques. **Legendary Spear** (Motoyasu; Lust): the longest reach, vaults and hooks, 15 + 4 Lust techniques, and a **bounded Time Reversal** (the ultimate of its top form: it rewinds the wielder alone to the oldest safe moment of the last 3 to 8 seconds and heals at most a quarter of their health; it never touches the world or anyone else; cooldown 2400 ticks). **Legendary Bow** (Itsuki; Pride): arrows made of mana, the vanilla draw curve, six "arming" techniques that change the next shot (sniper, net, mark, burst, seeker, second arrow), rains and a meteor, 13 + 4 Pride techniques. Each Holy Weapon has 10 ordinary forms (tiers 0 to 5, the series' names where it gives them) and 3 or 4 curse forms.
- **The seventeen Seven Star and Vassal Weapons**, each with 4 to 7 forms and 7 to 10 techniques of its own character: **Hammer** (poise and break, Weapon Form Synthesis), **Claw** (wounds that bleed, Skill Mastery), **Axe** (Brawn stacks, bounded execution), **Whip** (tethers, the bounded Command), **Staff** (a casting rhythm that shortens the next spell), **Gauntlet** (a point pool spent on fist techniques), **Projectile** (marks cashed in by Recall Blades), **Carriage** (Fitoaria; a rideable vehicle with a driver and one or two passengers, see below), **Fan** (Glass; magic residue and a glide), **Scythe** (L'Arc Berg; souls gathered by kills and a bounded reap), **Katana** (tempo chain, a parry that returns damage), **Book** (two chapters read page by page), **Mirror** (panes of glass, a stored "gleam", Prism Ray), **Musical Instrument** (a violin, a rhythm that adds harmony), **Harpoon** (a line tied to one creature), **Sewing Set** (stitch marks on foes, beacons on allies) and **Chain** (reach lash, pull and grapple).
- **The Vassal Carriage** is also a vehicle: an entity `nusmp:vassal_carriage_wagon` (one per wielder, never saved, it never touches a block) that the wielder drives and allies ride (driver and one passenger, two with the Roomy Cabin form), drawn by a renderer with the same mesh as the hand-held model.
- **The bond** (`nusmp:hero_bond`, a data attachment): one weapon per player (`maxBonds`), one holder per Holy Weapon, and the weapon is **bound to the body**: the item in the hand is a projection of the bond (the Djinn weapons' loan machinery), summoned and put away with **B**, and it cannot be lost, dropped or stolen. Techniques are on the use key, sneak + use and **Z X C**; **N** cycles the form; a HUD shows the weapon, its level (0 to 100), the cooldowns and what is repelling. Every technique goes through one dispatcher (cooldowns, the 40-tick global lock, the Magoi-style cost, the wind-up) and every blow through the balance law, so no weapon can skip a rule.
- **Forms and absorption** (`data/nusmp/hero/forms/<weapon>.json`, one file per weapon, reload-able): a weapon **absorbs the materials its next forms ask for** (the off-hand, or the Weapon Book) into the gem, learns forms whose parents it knows, trains each form with mastery (hits, techniques, blocks) and earns a permanent bonus when a form is mastered. A strict validator (the same code for JUnit and the in-game test `hero.forms`) rejects a file with a wrong tier, an unknown item or an unreachable skill. **Weapon Book** (key **M**, `/multiverse hero form book`): the tree, the requirements, and what in your inventory feeds the gem.
- **The Curse Series** (`heroweapon/curse/**`): despair gathers from the deaths of pets, allies and avatars, a betrayal, near-death and defeats; past a threshold the weapon **offers** its series and you accept it deliberately (hold Sneak + **O** for three seconds, or the Embrace button of the Weapon Book); a curse form then adds corruption per second and per technique and takes its toll: **Wrath** drains health and cuts healing, **Pride** shuts out help, **Lust** pulls you toward foes, **Envy** takes a technique at random, **Greed** weighs you down, **Sloth** delays you, **Gluttony** starves you; too much corruption becomes an **Overrun** (10 to 20 seconds of forced swings, then weakness). The price is bounded (the maximum health tax never takes you under four hearts) and it is cleared by a rest in a Safe Zone, by time away from the curse form, and by the new **Purification Charm** (`nusmp:purification_charm`, amethyst + gold + lapis + ghast tear). Seven sins are covered: Wrath (Shield, Claw), Greed (Sword, Axe), Gluttony (Sword), Lust (Spear), Pride (Bow), Envy (Mirror), Sloth (Chain).
- **The restrictions** (all server rules, each with a switch in `nusmp-heroweapon-server.toml`): **rejection** (a Holy wielder who fights with another weapon is shocked, never lethally, with the line "The Legendary Shield rejects the weapon"; Seven Star and Vassal wielders are never shocked; `#nusmp:hero_allowed` lists what is always welcome), **bound to the body** (above) and **interference** (no experience, from orbs, kills or the weapon itself, while another Holy wielder stands within 48 blocks; Seven Star and Vassal wielders neither block nor are blocked).
- **Acquisition** (`heroweapon/gem/**`; this is my default, see the notes): the **Calling Gem** (nether star + four netherite ingots + emerald, lapis, redstone and gold blocks) makes one free Legendary Weapon answer when you hold right click for five seconds, standing still on firm ground with open sky; a **Vassal Seal** (one of seventeen, each from a Gem Fragment and one signature material) calls its named weapon. 19 recipes, a circle of light, a title when the weapon chooses you; `/multiverse hero gem give <player> holy|seal <weapon>|fragment`.
- **Commands** `/multiverse hero ...`: `status`, `list`, `bind`, `release`, `summon`, `dismiss`, `level`, `set`, `registry list|clear`, `gem give|holders`, `form list|book|report|learn|set|absorb|reset|mastery`, `curse show|embrace|decline|despair|corruption|unlock|offer|clear|purify`. **Config** files: `nusmp-heroweapon-server.toml`, `-client.toml`, `-gem-server.toml`, `-forms-server.toml`, `-curse-server.toml`, `-curse-client.toml`. **Quest hooks:** `hero.bound`, `hero.summoned`, `hero.level`, `hero.skill`, `hero.repelled`, `hero.rewind`, `hero.absorb`, `hero.form`, `hero.mastery`, `hero.curse_offer`, `hero.curse`, `hero.overrun`. **Self-tests** (CI smoke): `hero.catalog`, `hero.bond`, `hero.rules`, `hero.use`, `hero.forms`, `hero.absorb`, `hero.curse`, `hero.gem`, `hero.gem.ritual` and one `hero.weapon.<name>` per weapon.

### NEW — the Skill Book & Skill Tree engine (`skilltree/**`, `client/skilltree/**`)
- **A route is data** (`data/nusmp/skill_trees/<name>.json`, loaded by a reload listener and sent to the clients): the route id, the book that unlocks it, its name and colour, the class id, and the nodes (id, parents, Skill Point cost per level, maximum level, the ability a node grants, the per-level numbers the ability reads). A strict schema rejects a bad file with a sentence naming it; adding a magic system later is adding a file.
- **The player's tree** (`nusmp:skill_tree`, a data attachment that survives death): unlocked routes, Skill Points (SP), node levels and the active class, synced to the client; every purchase is checked on the server (route unlocked, parents owned, SP enough, rate limit), the client only asks. One door, `SkillTreeApi`, for every change.
- **Skill Books** (`nusmp:skill_book`, one item whose component names the route): reading one **permanently unlocks the whole route** with an unlock show (two-stage effects, a "ROUTE UNLOCKED" banner in the route's colour, a chime); a second copy is **converted to SP** (or refused, `duplicateBookGivesSp`). **SP Books** (`nusmp:sp_book`): Beginner Archery +2, Elemental Primer +3, Magic Booster +5, Jack-in-the-Box (a gamble from 1 to 9 SP with a rare jackpot of 20) and the loot-only Greater Primer +10.
- **The menu**: a **Skill Tree** button in the existing Codex menu (and in the older Four Kingdoms and Multiverse Status screens), **no separate keybind**. The screen lists the routes by family, locked routes greyed with a padlock and "Requires: <book>", unlocked routes as a node canvas you can pan and zoom, a detail card with the per-level numbers and the price, a Buy button (a double-click or Enter also buys) that sends one packet, and **Set active** for the class.
- **Abilities** are real Tensura skills (so they sit in the skill menu and use the skill keys you already have) wrapped in `TreeSkill`: a technique casts **only if its route is unlocked, its node is bought and its class is active**, otherwise it refuses with a sentence.
- **Four Fairy Tail routes**, 37 nodes, 30 techniques and 7 passives, all numbers read from the route file: **Fire Dragon Slayer** (book *Fang of Smoldering Insanity*; Iron Fist, Roar, Claw, Wing Attack, Sword Horn, Brilliant Flame, secret art Crimson Lotus: Exploding Flame Blade, and the passives that eat fire), **Lightning Dragon Slayer** (*Scale of Crackling Sovereignty*; Roaring Thunder), **Sky Dragon Slayer** (*Wing of Tender Tempests*; support, healing and wind, Sky Drill) and **Ice-Make** (*Chisel of Silent Winters*; constructs: Lance, Floor, Shield, Cannon, Saucer, Eagle, Hammer, Cold Excalibur). Secret arts need two branches.
- **Isolation from the Black Clover Grimoire** by construction: no class of the tree names a grimoire class and no grimoire class names the tree; a source-scanning JUnit test fails the build if that changes. They share no data, currency, packet or screen logic.
- **Where the books come from.** (1) **Loot:** a Global Loot Modifier (`nusmp:skill_books`) rolls route books and SP Books from the monsters of the **Abyssal floors** of the Mega-Dungeon (about 4 route books and 40 SP Books per thousand kills), and the guardian of a **boss floor** (every tenth floor, the "decennial" boss) drops a route book with 30% and one to three SP Books (the guardian rolls no loot table, so a small drop listener serves it; Holy floors drop nothing unless `holyBossDrops` is on). (2) **The Black Market:** the **Old Witch** (a new NPC of the saga Allies; she comes to the Hub after Revchi Salik falls; sneak + right-click her) sells route books for **2.67 to 3 million gold** and SP Books, and buys monster parts, stones, meat and metals. **Gold** is a number on the player (not an item), shown in her shop; the shelf is the same for every player and restocks every three days. `/multiverse tree gold <player> add|set <n>` gives gold; she has her own advancement.
- **Commands** `/multiverse tree show|sp|route|node|class|reset|respec|reload|routes|gold|shop status|stock|restock`. **Config:** `nusmp-skilltree-server.toml` (enabled, requireActiveClass, classSwitchSeconds, respec, duplicateBookGivesSp, maxSp, purchaseRateLimit), `-loot-server.toml`, `-shop-server.toml`. **Quest hooks:** `tree.route`, `tree.node`, `tree.node.level`, `tree.sp`, `tree.class`, `tree.shop.buy`. **Self-tests:** `tree.core.routes`, `tree.core.rules`, `tree.core.sync`, `tree.items.use`, `tree.items.loot`, `tree.shop.stock`, `tree.shop.buy`, `tree.ft.fire_dragon_slayer`, `tree.ft.lightning_dragon_slayer`, `tree.ft.sky_dragon_slayer`, `tree.ft.ice_make`. A creative player can take every book from the creative tab; `/give @s nusmp:skill_book[nusmp:skill_book_id="nusmp:fire_dragon_slayer_book"]` gives one.

### Wiring (small additive edits to existing files)
- `NUSMP` (two init lines), `MultiverseCommands` (the `hero` and `tree` nodes), `NUCreativeTab` (the weapons, gems, seals, charm and books), `WeaponRenderer` (the 21 weapons draw as meshes; a hero weapon in its curse look draws `<id>_curse`), `WeaponClasses` and `tools/relics/weapon_classes.py` (`curse` joins the state suffixes, `weapon_classes.json` gets the 21 ids), `ItemStateVariants` (the Legendary Bow draws like a relic bow: pull meshes), `ArcNpcEntity.mobInteract` (three lines: sneak + click opens the Black Market for the Old Witch only), `ArcCharacters.bootstrap` (the witch), `global_loot_modifiers.json` (one entry added, the two old ones kept), `WeaponEngravings` (21 entries), the smoke test workflow (`hero` and `tree` prefixes), the lang file, one advancement (`met_black_market_witch`) and the Better Combat attribute files of the new melee weapons.

### KEPT
- Everything that existed: no class, asset, config, effect or feature was removed or replaced. The Black Clover Grimoire, Tensura's skill system, the Djinn weapons (their loan machinery is reused, not changed), the Charter, the System, the Mega-Dungeon, the Time Magic effects. The only existing data files edited gained entries, nothing lost.

### Notes for the owner
- **Acquisition is my default** (Calling Gem, Vassal Seals, admin commands). If you would rather have the Waves of Calamity, boss drops or an altar, only `gem/**` changes.
- **Choices I made where the text left room:** item ids `legendary_*` and `vassal_*`; one weapon per player, one holder per Holy Weapon (several players may hold the same Vassal Weapon: `vassalUnique`); the anti-party rule is read as "no experience while another Holy wielder is within 48 blocks" because you wrote "stand too close to one another" (the series ties it to fighting together); the curse needs a deliberate acceptance; the Soul Eater Shield is an ordinary tier-4 form of the Shield; **Envy and Sloth** are given to the Vassal Mirror and the Vassal Chain by design (canon ties no Holy Weapon to them), the Claw's curse form is Wrath (design) and the Axe's is Greed (one summary); the Hammer and Whip have no curse form; the Bless Series, the Boat, the Waves of Calamity as an event and weapon NPCs are not built.
- **Balance, as written:** damage goes through the balance law, heals through the healing law, hard control shares one 12-second lock, no technique changes a block or spawns an entity (the Carriage is the one vehicle), and a technique shows a tell above 8 raw damage. The dispatcher shortens cooldowns by the weapon's level (x0.75 at level 100, never below 20 ticks) and keeps a 40-tick lock between techniques, so the cooldown floors of the budget hold for the base rows only. Every number is a desk number. Players have a poise gauge of 12, so one heavy Hammer blow breaks a player's guard (bounded).
- **Keys:** Z X C (techniques), B (summon), N (cycle form), M (Weapon Book), O (Embrace, with Sneak). None is used by this mod before; Tensura's own default keys could not be looked up, so check Controls for a clash.
- **The System's** quest prizes (stat points, shadow cores) are not blocked by the experience rule because that system grants no experience; one line in `SystemRewards.grant` would do it if you read it the other way.
- **Prices:** 200,000 gold per diamond makes a route book about 10 to 15 diamonds. Knobs: `priceScale` and `sellScale` in the shop config, and the two files in `data/nusmp/black_market/`.
- **The Fairy Tail names** of some passives are mine (Eating Flame, Blazing Scales, Lightning Body, Eating Lightning); Dragon Force, Dragon Mode, Iron Hammer (it changes blocks) and Raging Bolt are not built, with the reasons in `docs/skilltree/ft_*.md`.
- **Looks:** the shapes of the Seven Star weapons are design (the series gives them no picture the builders could read), the Holy Weapons' base shapes are from memory of the anime; the colours of the gems (Shield green, Sword blue, Spear red, Bow yellow) are from the series' wiki summaries.
- Canon was read from search summaries only: the fan wikis could not be opened from the build machine.

## [1.61.0] — The characters of the arcs: the bosses of Black Clover, the Seven Deadly Sins and DanMachi, a remade Zagred and the friends in the Hub

The owner asked to resume "the bosses and NPCs of the arcs": the bosses and allies of Black Clover (Saga 1 and 2), the Seven Deadly Sins, DanMachi, a remake of Zagred that keeps his magic, and the Magic Knights as friendly characters. The systems under them came first: in 1.58.0 the roster, the shared NPC entity, the dialogue, the story chain, the Charter Grimoire rows of the bosses, the spawn egg and `/multiverse arc ...` (`docs/arc/core.md`), and in 1.59.0 the art kit that generates every character model and its clips (`docs/arc/art_kit.md`). This release carries the characters themselves. Each character has its own doc in `docs/arc/`.

**State:** written without a whole-project compiler; the pure logic and the data are covered by JUnit tests, the models are generated and were looked at in the kit's software preview (not in the game), and the CI smoke test boots a server with the characters registered. Nothing has been run in a game client.

### NEW
- **Five Saga 1 bosses, in the order of the anime** (`docs/arc/early.md` and one doc per boss with the canon, the move tables and what is unverified): **Revchi Salik** (Magic Binding chains), **Heath Grice** (a timetable of ice cages), **Lotus Whomalt** (a smoke dose you must keep down), **Mars** (three mage stones and Nemean Armor) and **Vetto** (beast auras, and the Mana Release). Each has 9 to 10 telegraphed moves, an ultimate in the second half of the fight and two or three mechanics with a counter a player can learn (cut a bind, break his line of sight, crack a stone, step behind a wall), runs on the Zogratis boss framework (the damage cap, the control law, the arena rules) and has a generated 3D model (Mars and Vetto a second form). Their Charter Grimoires lend real Tensura skills (8, 8, 8, 8 and 9 each) and each boss unlocks the next step of the story. They appear as floor bosses of the dungeon and in the story arenas (below); `/multiverse arc summon <id>` (or the spawn egg) calls any of them at once.
- **The Eye of the Midnight Sun bosses** (`docs/arc/midnight_sun.md` and one doc each): **Rades Spirito** (a ward of zombies, Soul Abductor), **Fana** (a fireball you can turn back, the Phoenix Robe, the Evil Heart countdown), **Rhya** (learns the grimoire magic that hits him, and a magic he knows hurts him less) and **Patolli** (Overreach, the Light Shaft, Heaven's Verdict), plus the **Third Eye bond** with Vetto: when one of the trio falls the others rage (+10 percent power a step). Each has a second form at the Gates and a Charter Grimoire (config `nusmp-midnight-server.toml`).
- **Zagred, remade** (`docs/arc/zagred.md`): the same entity id `nusmp:zagred` and all of his Kotodama magic, but a new raid fight in five acts where every Word is shown on screen as a title with its counter underneath (a ward with an unwritten window, a closed and open heart, verdicts that name three players, Overwrite held by rune stones), a new model with a human and a devil form, and a Charter Grimoire. The old Zagred stays as `nusmp:zagred_legacy` (no egg, no floor), so old saves load. `/multiverse boss zagred` summons the remade one (still gated by `zagredBossEnabled`).
- **The Spade Kingdom Raid** (`docs/arc/morris_libardirt.md`, `lucifero.md`): **Morris Libardirt** (Structural Tuning, Spell Rewrite, the Specimen thread, the Dissection Theatre cage) and **Lucifero**, King of Devils (the Bow and Kneel Before the King, the Gravity Shroud, Flattened Air, Event Horizon); tier 4, two forms each, three new mob effects, config `nusmp-spade-server.toml`.
- **The Seven Deadly Sins, part 1** (`docs/arc/sdsa.md`): **Meliodas** (Full Counter throws spells back at the caster, Assault Mode, the Demon King's Wrath), **Escanor** (Sunshine: a fight clock sets his power, size and moves; Cruel Sun) and **Ban** (the Fountain of Youth: three lives with a window in which nothing hurts him, Snatch); tier 3, two forms each, Charter Grimoires, config `nusmp-arc-sdsa-server.toml`.
- **The Seven Deadly Sins, part 2** (`docs/arc/merlin.md`, `gowther.md`, `king.md`, `diane.md`): **Merlin** (Absolute Cancel erases the next spell, the Perfect Cube, Infinity fields that last until she is burst), **Gowther** (he reads the kind of blow you keep dealing; real rings are gold and steady, illusions violet and flickering; no mind control), **King** (hovers out of reach, a Guardian shell that ice or anti-magic breaks, the Pollen Garden and Disaster field with a safe garden always in reach) and **Diane** (human size to giant with a visible quake, a burst cuts her down to size, Heavy Metal, the Diamond Shield, Drole's Dance); tier 3, two forms each, Charter Grimoires.
- **DanMachi** (`docs/arc/danmachi.md`): four monsters of the Dungeon, **the Minotaur** (the hunt marks the weakest fighter; a charge that hits a wall stuns it), **the Black Goliath** (a black hide that only heavy blows split), **the Amphisbaena** (a red head you must cut to stop the mist) and **Udaeus** (a rib cage that faces your last blow), with the **Dungeon Level** mechanic (a boss's Level comes from the Tensura EP; a strong party at the Gates makes it level up, which unlocks the leveled form and a move), plus six friends of the Guild (**Eina Tulle**, **Bell Cranel**, **Hestia**, **Welf Crozzo**, **Liliruca Arde** and **Ais Wallenstein**) who ask you to hunt them. Config `nusmp-danmachi-server.toml`.
- **Advancements for every character**: an `arc/` branch of 73 advancements (one per character, plus the saga ones), regenerated from `tools/quests/arc_roster.txt`, which a new tool (`tools/quests/arc_roster_from_source.py`) can now fill from the Java records without a running game. The boss bar of an arc boss shows the character's name and strength instead of a devil's name.
- **Fourteen Black Bulls and friends** (`docs/arc/npca.md`) for the Hub: Yami Sukehiro, Asta, Noelle Silva, Finral Roulacase, Magna Swing, Luck Voltia, Charmy Pappitson, Vanessa Enoteca, Gordon Agrippa, Zora Ideale, Gauche Adlai, Grey, Secre Swallowtail and Nacht Faust, each with 20 to 30 dialogue lines (443 in all) and a generated model; they arrive in waves as the early bosses fall.
- **Where the characters appear** (`docs/arc/world.md`): **the seven older bosses** (Goliath, Charybdis, the Devil Avatar, Dante, Vanica, Zenon and Lucius) are roster characters now, with the same story credit; **floor bosses**: the Guardian Altar of a boss floor may host an arc boss instead of the plain Guardian (a deterministic choice from the seed, sector and floor, favouring the bosses of the floor's theme, honouring the story gate and the chance `arc.floorBossChance`, with the plain Guardian as the fallback), which keeps the whole Guardian contract (floor scaling, the Mana Core, the altar told of its death), and arc bosses can also wander (`data/nusmp/mega/wanderers_arc.json`); **six story arenas** in a new dimension `nusmp:arc_arena` (Midnight Sun ruins, Spade throne, Wizard court, Sunlit plain, Orario pit, stone ring), entered with the **Chronicle Sigil** (a 3D item) and a lectern with the story pages and the boss choice; **the Hub** gets a Promenade ring and a Chronicle rotunda outside the protected plaza with 47 NPC posts (an NPC with a prerequisite appears once any online player has beaten it); **rewards**: a saga-clear reward (items and a commemorative book) once per player per saga, and an `arc/` advancement branch. Config `nusmp-arc-world-server.toml` (`arenas`, `arenaReplay`, `arenaCooldownMinutes`, `sagaRewards`, `chronicle`, `hubNpcs`, `hubNpcsAlwaysThere`, `hubGuide`, `floorBossScalePerFloor`); commands `/multiverse arc dump | chronicle | arena | hub`; quest events `arc.defeated`, `arc.met`, `arc.saga_clear`, `arc.arena`.
- **Sixteen friendly Magic Knights and court members** (`docs/arc/npcb.md`), all NPCs of the shared `nusmp:arc_npc` entity: the court (**Julius Novachrono**, **Marx Francois**), the Golden Dawn (**Yuno**, **William Vangeance**, **Mimosa Vermillion**, **Klaus Lunettes**), the Crimson Lion (**Fuegoleon**, **Mereoleona**, **Leopold Vermillion**), the Silver Eagle (**Nozel Silva**, **Solid Silva**), three more captains (**Charlotte Roselei** of the Blue Rose, **Jack the Ripper** of the Green Mantis, **Dorothy Unsworth** of the Coral Peacock) and the Seabed Temple siblings **Kahono** and **Kiato** (the mod's exclusive Song and Butoh casters).
- Each has a record (saga Allies), a dialogue file with 20 to 30 lines (6 greetings and 3 farewells, the rest hints that point at the next arc and boss and react to the player's story stage; no quest or shop line, because no quest system can finish one yet), and a generated model with its clips (idle, walk, run, attack, cast, hurt, death). 354 dialogue lines in all.
- Ten of them wait in the Hub for one of the five Saga-1 opener champions (Revchi Salik, Heath Grice, Lotus Whomalt, Mars, Vetto) to fall; the court and the juniors stand there from the first day (the Hub posts are below). `/multiverse arc spawn_npc <id>` (or the spawn egg) places any of them anywhere.

### KEPT
- Everything that existed: the grimoires of the same people (`CanonBook`), the Song and Butoh books of Kahono and Kiato, the fallback villager model, the two reference characters of the art kit.

### Notes for the owner
- The squads follow the series, not the first brief: Jack the Ripper is captain of the Green Mantis, Dorothy of the Coral Peacock, Kahono and Kiato are the Seabed Temple siblings, Nozel and Solid are Silver Eagle. The captain of the Purple Orca (Kaiser Granvorka) has a grimoire in the mod but no NPC yet.
- The looks are partly from memory and partly invented (the colours of several characters, the casual clothes); each model's source file says which parts.

## [1.60.0] — Parties: the Mega-Dungeon is instanced per FTB team (or per player)

The owner asked for the new dungeon to be "multiplayer friendly (should also work with FTB Teams)". Until now every player shared one chain of floors per road, so the first player to clear a floor changed it for everyone and a group could not keep its own progress. Doc: `docs/labyrinth/mega_v2_party.md` (the lane table, the FTB Teams notes, the decisions and the honest list of what is unverified).

**State:** written without a whole-project compiler; the pure parts (party keys, the party book, lanes, policy, the console lines) are covered by JUnit tests, the smoke test grows a second party from the console and checks the lanes. Nothing has been run in a game client, FTB Teams was never installed where this was written (its API is called by reflection and was checked against its source only).

### NEW
- **A party is the FTB team of a player, or the player alone** (the personal team FTB gives everyone counts as no team). Each party gets an index from a saved book (`data/nusmp_mega_parties.dat`), and the index is a **lane**: party 0 keeps the old lanes (Holy at z = 0, Abyssal at z = 300 000), so an existing world is unchanged; party k gets Holy at z = -k x 4096 and Abyssal at z = 300 000 + k x 4096. Every sector stores its party; generation, expansion, stairs, the Avatars' delving, the themes and the Crucible all follow the sector's party, with the seed mixed per party.
- The party is decided when a player enters through the gate (`PartyService.enter`); the Hub stays one shared Sanctuary. A guard sends a player off the floors of a party they do not belong to (operators bypass). The party name and head count are told in chat on arrival. A new FTB team inherits the lane of the member who played alone (the owner first); forming a team later is how friends end up in one dungeon.
- **The System for a party:** kills are credited to the killer and the members of their FTB team who are on the floor (`SystemParty.setProvider`, the seam of 1.58.0).
- Admin commands `/multiverse mega party list | of <player> | tp <player> | set <player> <n>` (`set <player> 0` merges a player into the old chain) and an optional `party <n>` on `mega status` and `mega expand`. Quest hook `mega.party` (the number of the party).
- Config `nusmp-labyrinth-mega-party-server.toml`, section `[party]`: `mode` ("party", or "shared" for the old shared dungeon), `maxParties` (64, up to 256), `useFtbTeams` (true), `guard` (true), `opsBypass` (true), `teamInheritsLane` (true). Self-tests `party.lanes`, `party.book`, `party.resolve`, `party.guard`, `party.chains`.

### CHANGED
- The System's default kill-credit group (everyone standing in the same sector) is replaced by the party provider; with no FTB Teams every player is a party of one, so the credit goes to the killer alone. `mega.maxSectors` now counts per party, the Dungeon Master's party-size grouping never joins two parties, and a Hub-to-dungeon entry decides the party.

### KEPT
- Every old signature and saved format (a sector without a party reads as party 0), the whole floor model, the stairs, the themes, the Crucible, the Guardian, the Hub and the Directional Lock. `party.mode = "shared"` (or the old ten-floor model, `floors.perSector` above 1) gives the 1.59.0 behaviour exactly, and switching back deletes nothing.

### Notes for the owner
- After the update the **first party that enters adopts the old chain** (party 0). If you would rather have one dungeon for everybody, set `mode = "shared"`; to merge a player into the old chain use `/multiverse mega party set <player> 0`.
- Friends who want one dungeon must be in one FTB party team before they enter (the personal team does not count). Without FTB Teams installed, or with `useFtbTeams = false`, everyone is alone in their own dungeon; use `mode = "shared"` on such a server.
- Far lanes (up to z = -258 048 and 558 048 with 64 parties) rely on chunk loading and the world border behaving; the server logs a warning at start if the border would cut a lane.
- The gate prompt in the Hub was not changed (the party is told after arrival instead).

## [1.59.0] — Sixteen new themes for the two roads: the Cursed Archive, the Holy Church and fourteen more

The owner asked for more themes for the dungeon, among them "a massive book archive called the Cursed Archive (evil route) and a Holy Church (good route)", and "many others" researched from D&D, fantasy and horror. Four builders each made four themes; this release carries all sixteen. Docs: `docs/labyrinth/mega_v2_tha.md`, `mega_v2_thb.md`, `mega_v2_thc.md`, `mega_v2_thd.md` (sources with URLs, every room and why, the palette, the numbers and the honest list of what is unverified).

**State:** written without a whole-project compiler; every room was built by a Python generator, passed the template checks (`mega_check.py`, `mega_arch_check.py`, the safe-zone check) and was rendered and looked at; CI compiles and tests it and a headless server smoke test places pieces of the changed pools with the real Jigsaw. Nothing has been run in a game client.

### NEW
- **The good road (Holy)** gets eight themes. **Holy Church**: a cathedral with a three-storey nave and organ loft, a cloister, a crypt, a bell tower, a confessional puzzle, a reliquary and a baptistery that is a real Safe Zone. **Celestial Court**: marble terraces, a colonnade, a sundial hall, a banquet hall, a throne room, a tribunal trial and a fountain court Safe Zone. **Sky Citadel**: a cloud giant's castle with giant furniture, a wind gallery, a cloud chasm trap, a storm spire, a pantry and a thunder court. **Mountain Monastery**: a dojo, a prayer-wheel hall, a scroll library, a zen garden, cliff stairs, hot springs (a Safe Zone) and a duel ring. **Feywild Grove**: a feast hall, a hollow tree, a moonlit glade, a court of the seasons, a fairy-ring puzzle. **Dwarven Forgehold**: a hall of pillars, a chasm forge, mine works with carts, a vault of kings, an anvil trial. **Crystal Geode**: an echo gallery, a prism atrium, a three-level great geode, a resonance puzzle, a shatter trial, a gem vault. **Sunken Temple**: a coral nave, drowned shrines, a heart of the tides, a tidal-gate puzzle, an offering nook.
- **The evil road (Abyssal)** gets eight themes. **Cursed Archive**: endless stacks, a reading hall, a scriptorium, a rotunda, burning stacks (a trap), a card-catalogue puzzle, a restricted section and the librarian's study (a Safe Zone). **Necropolis**: catacombs, a mausoleum street, bone bridges, grave pits and a burial vault. **Vampire Castle**: a great hall, a coffin crypt, a chapel, a clock tower, a blood court trial and a mirror gallery. **Haunted Manor**: a grand stair, a kitchen, a nursery, a ballroom trial, an attic trap and the master bedroom (a Safe Zone). **Fungal Underdark**: a two-storey drow enclave, a spore forest cavern, a fungus farm, a myconid circle, chasm ledges (a trap), a dark lake and a myconid garden (a Safe Zone). **Infernal Foundry**: a two-storey grand furnace, a penal forge, a contract hall puzzle, a cage arena trial, a soul wheel, a smelter's rest (a Safe Zone) and a counting house. **Drowned Crypt**: a flooded nave of two storeys, a sarcophagus vault, a tide-gate puzzle, a drowned font trial, a drowned chapel, an air bell (a Safe Zone) and a pearl reliquary. **Spider Warrens**: a web great hall of two storeys, a cocoon larder, the queen's lair, a brood hatch trial, a silk nook (a Safe Zone) and an egg cache.
- Each theme is a palette (the dungeon restyles every ordinary room into it), 5 to 8 signature rooms that **only its own sectors draw**, an encounter table (12 to 13 entries), faction weights in all eleven factions, an ambient particle and mood sound, a Guardian trait and a Mana Core colour, and a ruleset name. 103 new room templates in all.
- **Road-bound themes**: a theme belongs to one road (`RegionTheme.Route`) and is never drawn on the other. Every theme is also reachable by relic: a relic taken from a vault can lead to the theme it fits (over eighty relic entries were re-homed or added, so each theme is the road of at least five relics and at least one of them is Unique or higher, which the Crucible requires).
- New tools: `tools/labyrinth/mega_theme_kit.py`, `mega_tha_kit.py`, `mega_thc_kit.py` (shared building kits), one generator per theme, and new data tests (`TemplateThemesDataTest`, `TemplateThemesFilesTest`, `TemplateThemeDataTest`, `GoodRoadThemesDataTest`, and a check that every theme has a Unique-or-higher relic).

### CHANGED
- **How rooms are weighted**: a theme's own rooms now carry an absolute weight (a themed hall folder totals 60, the other kinds 20), so they take nothing from the old rooms in other sectors (`gen_mega_pools.py`, the `claimed` rule). Without this the new rooms would have starved the old ones everywhere.
- The Guardian's trait table and the Mana Core colour table know the new themes, and the shared theme tests list them.

### KEPT
- The eleven themes of 1.54.0 and 1.55.0, all their rooms, tables and pools; the old pool weights are unchanged in every old sector.

### Notes for the owner
- The planner now has 235 room templates to choose from; in a test of 200 random sectors one needed its simple fallback layout. If you see a floor that looks plain or thin in play, tell me its number.
- Lava, water and the many props (copper bulbs, minecarts, sea pickles, budding amethyst) are held in place by the room models only; if one behaves badly in play, tell me which room.

## [1.58.0] — The Mega-Dungeon as floors with stairs, the old dungeon removed, the System for everyone, the Soul Forge

The owner asked for a stack of changes at once: remove the old dungeon "completely, but keep the blocks the new one uses"; make every floor its own level that you leave by a stair, as in D&D; make the System work for every player in the dungeon with a window you can move; and rebuild the Avatar from the brief that was sent. This release is the first part. The party layer (FTB Teams), the new themes and the characters of the arcs follow in 1.59.0 and 1.60.0. Every number, file and decision is explained in `docs/labyrinth/mega_v2.md` (the contract) and one doc per part.

**State:** written without a whole-project compiler by parallel builders and merged by the lead; CI is the first real compile, the tests run in CI, and a headless server smoke test (`[smoke] [strict]`) boots the mod and runs the in-game self-tests. Nothing has been run in a real game client. Each doc ends with the list of what could not be verified.

### NEW
- **Floors are levels** (`docs/labyrinth/mega_v2_gen2.md`). A sector is one floor (`floors.perSector`, default 1). You arrive on the stairwell of the floor, explore about 28 rooms (`mega.floorRooms` 18 to 40; a boss floor eight more), and have to find the forward stairs, which hang from the door of the room farthest from the arrival by walking distance. Standing on a stair pad moves only that player to the neighbouring floor (title "Floor +4"). The back stairs of the first floor lead out to Floor 0, the Hub. The Holy road climbs, the Abyssal road descends. Every tenth floor is a boss floor: the whole boss chain at the far end and the grand stairwell behind the rubble seal. A floor stands in a fixed slot of its road's lane, so it is planned from its own saved data and can be regenerated or audited alone. The next floor is queued when a player enters a floor, so the stairs are normally ready when found. A theme belongs to a decade of floors. New: ten stairwell templates with their pools, the floor recipe `data/nusmp/mega/recipe/floor.json`, `template_themes/*.json` (which rooms a theme may use), the stair commands under `/multiverse mega` and the self-tests `stairs.*`.
- **The System for every player in the dungeon** (`docs/labyrinth/mega_v2_sys2.md`). It covers both roads; the Hub shows a status window (stat points, no quest). Kills are credited to the killer and to the party members within `system.shareRadius`. The stat bonus works **only inside the dungeon dimensions** (transient modifiers, `system.buffOnlyInDungeon`, on by default; off restores the old always-on behaviour) and an old world's permanent modifiers are purged on login. **The window can be moved**: `U` toggles edit mode, the arrow keys nudge it (Shift = 10 pixels), `Home` resets it, `End` cycles the corner, and with chat, the inventory or the pause menu open it can be dragged with the mouse. Eight key mappings under Controls, category "Multiverse: System"; client config `anchor`, `offsetX`, `offsetY`.
- **The Soul Forge, the new way to make an Avatar** (`docs/avatar/av4_ui.md`). A craftable altar block (`nusmp:soul_forge`, needs a Genesis Core to craft). Using it locks you in place (transient modifiers, a crash cannot trap anyone), draws a circle of light, and after four seconds opens a full-screen creator: five round category buttons (Identity with a 3D die over the stat roll, Race and Appearance, Traits with a sample line, Skills with a Unique and an Ultimate slot, Aptitudes) and your Avatar in the middle reacting live. Aptitudes are either hand-picked at a **permanent magicule tax** or rolled for free by the server with a slot-machine reel. Finalize sends only choices; the server re-reads your reserves, validates them and pays only at the birth beat, so every cancel path (closing the screen, taking a hit, dying, logging out, walking away) takes nothing. The birth is a beam, a camera shake and a first line. 30 aptitudes and 42 skills are data files (two reload listeners), plus an **Instant Skill Awakening** (a d100 below 5 plus your luck, or certain when paid). Config `nusmp-avatar-soulforge-server.toml`.
- **The Genesis Core is craftable** (it could only be given or dropped before): netherite, a diamond block, a nether star, a totem, a heart of the sea, an echo shard, an enchanted golden apple and a dark rukh shard. Every Avatar eats one, so this is the price of one Avatar. Both recipes ship unconditionally; a datapack can remove them.
- **The Avatar entity rebuilt** (`docs/avatar/av4_ai.md`): a native Tensura subordinate (a tamable base, same entity id and NBT keys, so old Avatars load), a 32-slot inventory (hands, armour, eight trinket slots, pockets; sneak and click with an empty hand), a weapon AI that picks melee, bow, crossbow, mage or shield by what it holds, a looting AI, a skill loadout it presses like a player, evolution, an autonomous naming and NPC party, independent delving, `/avatar follow|stay|delve|recall|share|party|release`.
- **The framework of the arc characters** (`docs/arc/core.md`, no characters yet): the roster, one friendly NPC entity, a dialogue and story chain, Charter Grimoire rows for bosses, a spawn egg and `/multiverse arc ...`. The bosses, the Seven Deadly Sins, the DanMachi characters and the ally NPCs come in 1.60.0.

### REPLACED (the owner asked for these)
- **The old dungeon is gone** (`docs/labyrinth/mega_v2_prune.md`): the Spire layout of 1.35.0, the whole Living Dungeon (its cave dimension content, tiers, ecology, Golden City, Variable Core, sentient rooms, immune system, Maw, food web, the two floor arenas) and the legacy gates and Maws. Config removed: `nusmp-dungeon-server.toml`, `labyrinth.engine`, `mega.layout`, `mega.legacyGates`; `/multiverse dungeon ...` is gone except `/multiverse dungeon relic ...`. The blocks, items and creatures the Mega-Dungeon, the Hub, the economy, the relics or a test still use stay registered, as do the dimensions `nusmp:labyrinth` and `nusmp:living_dungeon` (inert; a player found in the Living Dungeon is carried out), so old worlds still decode. Removed blocks become air and removed items vanish when an old chunk or inventory loads. Goliath and Charybdis keep their ids and are summoned by `/multiverse boss goliath|charybdis` until the arcs give them a home in 1.60.0.
- **The System** was Holy-road-only with permanent modifiers; it is now everywhere in the dungeon with transient ones (see above). The old `abyssalToo` key stays in the file and is ignored.
- **The creation of an Avatar** is the Soul Forge. The Genesis Core used alone only shows a hint (`avatar.coreUse = "item"` lets the core start a creation on its own again) and `avatar.creationUi = "classic"` brings back the four-page screen, which is kept.
- **Naming** now adds its tax to the tax the Soul Forge already charged (capped at 90 percent) instead of overwriting it, and a Soul Awakening no longer refunds the forge's tax of a forged Avatar.

### KEPT
- The Hub, both roads, the Directional Lock, the Rukh flow, the standard gear, the Guardian, the Crucible, the economy, the encounters, every room and pool of 1.54.0 and 1.55.0, the old banded floors when `floors.perSector` is more than 1, the classic Avatar screen and every classic payload, the stat roll, races, personalities, appearance, outcomes and the Yandere handling.

### Notes for the owner
- A world that already opened its roads keeps its sectors as they were (no world is renumbered); the floor model applies to a road that is opened now, so a new world or a road not yet opened uses it.
- Chosen Unique and Ultimate skills of the Soul Forge make an Avatar skip the three-day betrayal roll (`avatar.chosenSkillsSecure`, on by default). Tensura 2.0.1.3 registers no Ultimate skill of its own, so the Ultimate slot holds this mod's Ultimates and the sins; the in-game self-test `avatar.forge_skills` names any id that does not resolve.
- Party lanes and FTB Teams, the 16 new themes (Cursed Archive, Holy Church and more) and the arc bosses and NPCs are still being built and arrive in the next releases.

## [1.57.0] — Fix: the Mega-Dungeon is now really the default dungeon

In 1.55.0 and 1.56.0 the config default of `mega.layout` was still `"spire"` (a leftover from the build-up): the Gates, Maws and `/multiverse labyrinth` led to the old Spire, and the new dungeon was reachable only with the `/multiverse mega ...` commands or after setting `layout = "mega"`. The default is now `"mega"`: the Overworld Gateway leads to Floor 0 (the Sanctuary Hub) and its gates to the Holy and Abyssal roads (`hub.enabled`). The Spire is kept: set `mega.layout = "spire"` to route back to it.

**If you already ran 1.55.0 or 1.56.0** the config file `serverconfig/nusmp-labyrinth-mega-server.toml` of your world was written with `layout = "spire"` and keeps it: change it to `layout = "mega"` (and restart or `/reload` is not enough for server configs: restart the world), or delete the file so it is rewritten with the new default.

## [1.56.0] — Lucius: the chest gem reveal clip and a clean stage change

The owner sent the Lucius Zogratis god-tier brief again. Compared with the mod (rule 5), nearly all of it already exists since 1.45.0 and is kept as it is (`docs/lucius_god.md`, which maps every item): the three stages with a full health reset and the invulnerable change, the theatrical lines, the hit cap, percent true damage, the anti-regeneration aura, the relic EMP, Djinn severance, magicule starvation, anti-flight gravity, spatial disorientation, ultimate reversal, the Checkmate counter and the Time Stasis Traps. Two things of the brief were missing and are new.

### NEW
- **`chest_gem_reveal`**, a one-shot Lucius animation (1.8 s) on both bodies, the human and the divine form: a crouch, then the chest thrown out and the arms spread wide while the glow swells (the divine form opens its wings too). It plays once, when the chest gem opens in the last stage (the rising edge of the synced gem flag), and settles back to the idle pose. A model without the clip simply skips it. Built in `tools/gen_zogratis_lucius.py` (previews: `lucius_base_chest_gem_reveal`, `lucius_divine_chest_gem_reveal`), played by `ZogratisPose` (new `Frame.gemOut`, tested).
- **A stage change cleanses Lucius**: every harmful effect on him ends when the next stage begins (the brief's "clear debuffs"). Before, only the players were cleansed.

### KEPT (every number the brief names stays a config value the owner already tuned)
- The hit cap is `luciusHitCapPct` (2.0 here; the brief says 2.5) and the aura radius `luciusAuraRadius` (14 here, eased in 1.46.0; the brief says 30). Both are in `nusmp-lucius-god-server.toml`; they were not changed because the owner eased them on purpose in 1.46.0.
- The Java 17 / mixin wording of the brief does not apply: the mod runs on Java 21 and every mechanic is an event hook or a gated call in the existing classes.
- The brief file ends in the middle of its section 5 ("The Checkmate ..."): the Checkmate counter exists since 1.45.0; if more was meant after that, it did not arrive.

State: the animation was checked in the headless render preview only; nothing was run in the game.

## [1.55.0] — Floor 0 and the two roads: the Sanctuary Hub, Holy and Abyssal floors, the System, the Rukh flow, standard gear

Ships together with 1.54.0 below (the Mega-Dungeon itself); 1.54.0 was never released on its own. **State:** written without a whole-project compiler; CI is the first real compile. Pure logic is covered by JUnit tests that ran locally in a small shim, the whole tree was type-checked against API stubs, and a CI smoke test boots a headless server with `[smoke] [strict]`. Nothing has been run in the game. Docs: `docs/labyrinth/mega_floors.md` (the brief against what existed, decisions and their WHY) and one `docs/labyrinth/mega_<part>.md` per part.

### NEW
- **Floor 0, the Sanctuary Hub** (dimension `nusmp:sanctuary`, biome `nusmp:sanctuary_plains`): a safe, buildable plains world. Only passive animals spawn, no structure of any other mod generates, a protected plaza (arrival point, notice pavilion, fountain, the Gate of Return home) and two polarity gates. Built by its own `ChunkGenerator` (`nusmp:sanctuary`): 3D simplex noise for the plains, the **Holy Spire** (a tiered tower, trial chambers by a 3D Wave Function Collapse around a guaranteed spiral stair) and the **Abyssal Chasm** (a pit with caverns by a polar 3D WFC around a guaranteed ramp); both roads are proven walkable by a search in the tests.
- **Two roads** from one dungeon dimension: Holy floors +1, +2 ... and Abyssal floors -1, -2 ... (two chains of sectors 300 000 blocks apart). A sector is ten floors; the Landing is its first floor and the Guardian's hall the last (every tenth floor is a boss floor). Floors are numbered by an even split of rooms by distance (`floors.banding = "even"`; `"arena"` is the brief's literal doorway-depth rule, which left about 3.6 of 10 floors empty in measurement).
- **Directional Lock** (`PlayerDungeonAlignment`, a data attachment): clearing floor +1 or -1 locks the player to that road; the other road's stairs, gates and chain travel refuse them, and a hard guard casts out a player who gets across anyway. Operators bypass it. Config `nusmp-labyrinth-mega-floors-server.toml`.
- **The System** (Solo Leveling): a holographic client overlay (`nusmp:system_overlay`, key Y), per-floor quests, permanent stat rewards or a **Shadow Core** item that extracts the shadow of a defeated Holy enemy as a minion (new item `nusmp:shadow_core`).
- **The Rukh flow** (Magi): on Holy floors a Light cast does x2.5 damage for 1.5 s, a Dark cast is rejected (8% of max health, never lethal); the 105 magic families are classified in a data file (3 Light, 16 Dark, 86 neutral). The Abyssal road has no flow by default.
- **Standard gear**: Normal, Special and Rare pieces (ordinary vanilla items stamped with `nusmp:gear_grade`, Tensura engravings on Special and Rare weapons, magic resistance on Rare) drop without limit from the dungeon's monsters and appear in its chests (global loot modifier `nusmp:gear_chests`). They are not relics and not in the Order's ledger.
- **Gateway ruin guard**: survival and adventure players cannot mine or blow up the Overworld Gateway (a creative operator can; switch `labyrinth.gatewayUnbreakable`).
- Ten new advancements (Floor 0, the two roads, ten floors, the System, Arise, standard gear); event keys `mega.hub`, `mega.aligned`, `mega.floor`, `mega.gear`, `mega.system_quest`, `mega.shadow`, `mega.vessel`.
- Shared template tools: block states are checked against the game's own table (`tools/labyrinth/vanilla_block_states_1_21_1.json`), a static survival check (`mega_arch_check.py`), cutaway and first-person previews (`mega_shape_preview.py`, `mega_walk_preview.py`), a sector composer.

### REPLACED
- The destination of the Overworld Gateway: it now leads to Floor 0 first (`hub.enabled = false` restores the 1.54.0 way straight into sector 0). Leaving a road of the Mega-Dungeon returns to Floor 0.
- The Crucible of Choice offers **exactly three distinct Unique-or-better relics** (Unique, Legend, Mythical, Genesis) instead of 1 to 3 of mixed grades (`theme.offerCount` can restore the old shape); the relic taken decides the look and ruleset of the next ten floors.
- Threat follows the floor (monsters, loot, rest ambushes; the sector Guardian takes the threat of its last floor) instead of one number per sector.
- Next sectors wait for the Crucible (`mega.expandAfterVault`, default on): the pick releases them.

### KEPT (nothing removed)
- The 1.54.0 Mega-Dungeon, the Spire and the Living Dungeon (`mega.layout`, `labyrinth.engine`), the old Dungeon Gates and Maws, the vanilla-particle purge and `nusmp:light_rukh` (1.36.0), the Magic Stone and meat economy and the Vital Strike (1.36.0), all relics and the Order's ledger (the mod's existing low-grade named relics are still one-per-world).
- The guaranteed decennial **Metal Vessel** is built but OFF by default (`flow.vesselEnabled = false`): the newest brief says one top-tier relic per ten floors (the Crucible's).
- Normal gear carries no engraving ("Normal is plain"); making it light is a data-only change in `data/nusmp/mega/gear/tables.json`.

### Config and commands
- New server config files: `nusmp-labyrinth-mega-{floors,hub,macro,system,flow,gear}-server.toml` (plus `nusmp-labyrinth-mega-system-client.toml`); `labyrinth.gatewayUnbreakable`. Commands under `/multiverse mega`: `floors`, `hub`, `macro`, `system`, `flow`, `gear`, `crucible`, `expand [holy|abyssal]`.

## [1.54.0] — The singular, expanding Mega-Dungeon

**State:** as 1.55.0 (never released on its own). Docs: `docs/labyrinth/mega.md` and `mega_core.md`, `mega_arch.md`, `mega_rooms1.md`, `mega_rooms2.md`, `mega_safe.md`, `mega_enc.md`, `mega_eco.md`, `mega_guard.md`, `mega_theme.md`, `mega_ci.md`.

### REPLACED
- **The default dungeon.** Dungeon Gates and Maws now lead into ONE persistent dimension, `nusmp:mega_dungeon`, built from vanilla blocks as a tightly packed tabletop-style layout of sectors (122 room templates on a 9-block grid, corridors, junctions, halls, caverns, safe zones, trap/puzzle/trial rooms, a boss chain and a vault per sector). The Spire (1.35.0) stays registered and is reachable with `mega.layout = "spire"`.

### NEW
- **Live expansion:** the generator assembles a sector from the template pools (deterministic, audited, saved, placed over several ticks) and grows the next one off the previous sector's dead end when its **Labyrinth Guardian** is cleared and the sector's Gatekeeper door (`kind=sector`) is opened with the Guardian's **Mana Core**; `/multiverse mega expand|status|regen|audit|plan|unseal`.
- **Dungeon Master:** D&D encounter tables rolled by threat, party size, noise and light, wandering bosses (`mega.enc`); **factions** and turf wars, faction drops (`mega.eco`).
- **Safe zones and rests:** Landings and safe rooms with seats, hearths and beds; short and long rests only there, no easy passive regeneration; a data-driven mod-compat processor swaps in Farmer's Delight and MrCrayfish furniture when installed (`mega.safe`).
- **Region themes:** the `nusmp:region_theme` processor restyles every room by swapping vanilla block families (11 themes) and the **Crucible of Choice** vault with its catastrophe (`mega.theme`).
- The Overworld **Gateway** structure (`nusmp:mega_gateway`) and an in-game self-test system read by a CI smoke test that boots a headless server (`tools/ci/server_smoke.py`, `.github/workflows/smoke.yml`).

### KEPT
- The Spire, the Living Dungeon, the old gates, the Sealed Archway trials, trap blocks, puzzle doors, relic pedestals and exit gates (reused as rooms), the Magic Stone and meat economy, the Vital Strike, and every relic rule.

## [1.53.0] — Fix: the blurry Avatar menu, and a guard so no screen can do it again

### FIXED
- The Avatar action menu was blurry because it called `renderBackground` by hand and then `Screen.render` called it again over the finished panel (since 1.21 that call blurs everything drawn before it). The panel is now drawn inside a `renderBackground` override, after the background. Same bug class as the questionnaire in 1.15.0.

### NEW (guard)
- `ScreenBlurGuardTest`: the build fails if any screen under `client/` calls `renderBackground` or `renderBlurredBackground` inside its own `render(...)`.

## [1.52.0] — Fix: the Avatar action menu text

### FIXED
- The menu that opens when you right-click your Avatar listed the name, race, personality and luck twice, and the extra lines ran over the buttons. The duplicated lines are gone, the panel is taller, and the list stops above the buttons.

## [1.51.0] — The Avatar creation system

**State:** written without a compiler; CI is the first compile. Nothing run in game. Docs: `docs/avatar/av1.md`, `av2.md`, `av3.md`.

### NEW (nothing replaced; the Devil Avatar boss is untouched)
- **Genesis Core** item (drops only from DanMachi dungeon boss rooms: first kill guaranteed, then a chance) opens the **creation GUI**: roll D&D stats on the server (4d6 drop lowest, or 5d6 drop two lowest when your total luck is above 10), pick a race from `data/nusmp/avatar_races/*.json` (15 shipped, connection conditions checked on the server), one or two of 14 personalities, design the look (hair, horns, wings, outfit, skin, aura, eyes), name optional.
- **The Avatar** (`nusmp:player_avatar`): follows and defends the creator. After 3 in-game days an unnamed Avatar without a unique skill rolls its outcome: Betrayal (30% minus luck), Soul Awakening (25% plus luck), Ordinary Existence. Naming it (Tensura-style: magicule exhaustion tax, permanent tax, +50 to +200% EP surge, evolution, Soul-Bond Lock) cancels betrayal. Betrayed Avatars hunt the creator across dimensions (hits capped, grace after each teleport).
- **Personalities and chatter:** 1386 hand-written lines, hybrid mixing, Yandere frenzy (burns its own health, cooldown x0.1, no magicule cost, jealousy and intercept goals, FTB squadmates exempt), and a fake-player caster so grimoire and Tensura magic can be cast by the Avatar.
- **Look:** layered renderer with 16 hairs, 8 horns, 6 wings, 12 outfits, tinted skin, aura ring, glowing eyes (red in a frenzy).
- Config: `nusmp-avatar-server.toml`, `nusmp-avatar-ai-server.toml`, `nusmp-avatar-net-server.toml`. Command `/nusmp avatar ...`. Quest keys `avatar.created`, `avatar.named`, `avatar.betrayed`, `avatar.awakened`.

## [1.50.0] — Named mobs can carry and attune to relics

**State:** written without a compiler; CI is the first compile. Nothing run in game. Details: `docs/mob_relics.md`.

### NEW
- Sneak + use a relic on a named or owned mob (a goblin you named, a tamed monster): it takes the relic (weapons in hand, armour on its slots, the rest in a small pack), a relic that needs attunement attunes to the mob (3 at most), and the mob wears the relic's attribute passives, effects and resistances. Sneak + empty hand takes it back. The pack drops on death. Config `nusmp-mobrelics-server.toml` (`enabled`, `namedOnly`, `maxCarried`).
- Not for mobs: relic acts, grade existence bonus and skill leases (they are written for players).

## [1.49.0] — Advancements and the FTB Quests questline

**State:** written without a compiler; CI is the first compile. Nothing run in game, and FTB Quests has never loaded the pack (written to its source format and checked with a validator). Docs: `docs/quests.md`, `docs/advancements.md`, `docs/quests_java.md`.

### NEW
- **369 advancements** in a "Multiverse of Anime" tab (start, 105 magic families, spells, bosses, relics, weapons, dungeon and Labyrinth, spirits, squads, world, challenges, 13 arcs). The 25 hidden Convergence ones are untouched. Custom trigger `nusmp:event` with about 55 hooks into the game (spell casts, relics, forge, sockets, Djinn, squads, bosses, arena, and more).
- **FTB Quests questline:** 22 chapters, 339 quests in three groups: The Journey, The Anime Arcs (13 main arcs in order) and Bosses and Challenges. Squads chapter on FTB Teams; boss kills credit the killer's whole team. Arcs whose bosses are not made yet are complete chapters with locked "coming soon" boss quests waiting on hidden `nusmp:future/*` advancements; a new boss unlocks its quest with `NusmpEvents.futureBoss(player, arc, boss)`.
- **Installer** `/nusmp quests status|install [force]|update|uninstall` (op): copies the bundled pack into `config/ftbquests/quests`, refuses to overwrite a foreign quest book unless forced (then backs it up), never touches other chapters on update. Config `nusmp-quests-server.toml`.

## [1.48.0] — Every relic in hand as a 3D model, and the Clover robes in the anime style

**State:** written without a compiler; CI is the first compile. Nothing run in game. Details: `docs/relic_hand_models.md`, `docs/robe_anime.md`.

### NEW
- **3D relics in hand, on the ground and in item frames** for every relic that is not a weapon (240 relics): 83 signature models (the Infinity Stones and Gauntlet, Hand and Eye of Vecna, the Deck, Warren's Key, the Soup Spoon, the Cooking Pot, the lantern, both lyres, Horn of Leroy, collars, medallions, orbs, ten rings, bags, coins and more), 21 consumable models (jars, vials, flasks, a genie bottle, scrolls), garment bodies under the ornaments of chest relics, and data-driven hold styles (`assets/nusmp/relic_hand/transforms.txt`). The inventory keeps the 16x16 sprite. The 14 weapons you listed as flat already had meshes (they are older items kept as relics).
- **The Magic Knight robes look like the anime:** the chest piece is only the cloak (shoulder drape, long swaying back panel, squad emblem, clasp, collar); the helmet piece is a single accessory (Asta's headband for Black Bull, horns for the devil coat, a hood laid back for some, nothing for others); legs and boots draw nothing so your own legs show. 11 sets. Designs are from memory of the series; `docs/robe_anime.md` says which parts are canon and which invented.

### REPLACEMENT (old behind a switch)
- Client options `relicfx.handModels` (default on; off = the old flat form) and `relicfx.robeAnime` (default on; off = the 1.34.0 lofted robes). Old models and catalog lines are kept.
- The 1.34.0 object form of relic armour is now drawn by the new hand renderer.

## [1.47.0] — docs: the relic format

### NEW
- `docs/relic_format.md`: the card template and the definition format every relic is built from. No code changed.

## [1.46.0] — Fixes: crashes, Curios relics, Better Combat, Butoh, Rouge, Soul Corpse, and Lucius's arena

**State:** written without a compiler; CI is the first compile. Nothing run in game. Details: `docs/better_combat.md` (1.46.0), `docs/relic_fix_1_46.md`, `docs/exclusive/fx3.md`, `docs/lucius_arena.md`.

### FIXED
- **Crash "Not building!" in the player aura layer** (two crash reports): a painter held two vertex buffers and the second ended the first. All aura painting now goes through `LazyBuffers`, which re-acquires its builder at every vertex.
- **Relics did nothing in Curios slots:** `CuriosLink` shared one initialisation between two lookups, left one null, and then marked Curios broken for the whole session. Fixed, and a failure now only pauses Curios reads for 10 s. Also: Ring of the Mimic was missing from the charm slot tag, and 14 charms had no worn model (they are drawn now).
- **30 magics had no rune texture** (Soul, Soul Corpse, Song and 27 more attributes): generated.
- **Better Combat:** every weapon now uses only Better Combat's own presets and animations (no custom animations referenced; the old files stay on disk unreferenced). Root causes in `docs/better_combat.md`.
- **Soul Corpse spawned nothing:** the reanimation page was sealed behind mastery (now opened for every holder), an empty ledger did nothing (new bodies are conjured), puppets could spawn inside blocks and undead burned in daylight.

### REPLACEMENT (old behind a switch)
- Butoh dance is a passive for anyone with the magic, with a mana upkeep (`butoh.passive`); the page now toggles it off and on.
- Rouge rests on your head again (`thread.rougeOnHead`) and can be named with a name tag (or by using one while looking up).
- Lucius god-tier balance: true-damage tiers halved, caps and cooldowns eased, fewer clones, new `luciusDifficulty` (default 0.8) and scaling by player count.
- The Named corpse pages (Jimmy, Alfredo, Carl) are no longer locked to Rades.

### NEW
- **Lucius's arena** (`nusmp:zogratis_arena`) and the **Hourglass of the Final Hour**: hold right click 3 s to be carried there with allies within 8 blocks, and again to go home. Lucius can only exist in the arena (`luciusArenaOnly`, default on); the spawn egg, summon command and portals are refused elsewhere. Return Sigil block, `/zogratis_arena`.

## [1.45.0] — Lucius Zogratis, god-tier counter-mechanics

**State:** written without a compiler; CI is the first compile. Nothing run in game. Full design in `docs/lucius_god.md`.

### NEW (all behind `luciusGodTier`, default on; off = the 1.42.0 fight)
- Three stages, each a full health bar: the fatal blow is intercepted, health refilled (a third owner exception to the no-healing rule, next to Time Reversal and the Gates refill), the stage advances with theatrical voice lines to everyone in the arena. Stage 1 Time and Soul, stage 2 Gravity and Blood, stage 3 Spatial, clone swarm and the chest gem.
- Hit cap of 2% of his max health per blow (evasive teleport or counter when it triggers); his spells hit for a percent of the target's max health as true damage (never more than 40% in one blow, still under the 30%/s pressure cap), and drain the same percent of the target's magicule.
- Time Decay aura (20 blocks): no passive regeneration for players and pets; Relic Nullification EMP (10 s, nothing is unequipped); Spirit and Djinn Severance (12 s, reversible); Magicule Starvation field (never touches health); Anti-Flight gravity in the arena (creative off by default); Spatial swap when you launch a devastating attack; Ultimate Skill Reversal (cancels the activation, mirrors 6% of your max health); Checkmate Counter against channelled spells; Time Stasis Traps for players who spam dodges.
- Effects `nusmp:relic_nullified`, `nusmp:severed`, `nusmp:time_decay`; config `nusmp-lucius-god-server.toml` with a switch per mechanic.

### Kept
- Every 1.42.0 chain and the old Swap Strike; the Gates refill stays off for Lucius (the stage change is the only refill).

## [1.44.0] — The exclusive casters: Vanessa, Spatial, Lucius, Rades, Kahono, Kiato, Nacht

**State:** written without a compiler; CI is the first compile. Nothing run in game. Per-caster details, WHY and unverified points: `docs/exclusive/{vn,sp,lz,rs,kh,kt,nf}.md`.

### NEW
- **Vanessa (Thread):** Rouge as a real unkillable thread-cat entity that runs to endangered allies and taps them; a fatal or 35%+ hit on the caster, pets, squad or chosen allies is cancelled and the target nudged aside, at a massive mana cost (Rouge unravels if it cannot be paid). New pages: Arresting Dark Weave, Dancing Doll recall, Thread Trap, Reinforcement / Mana Skin (toggle).
- **Lucius (Soul):** Paladin Conversion (3 s purification channel, buffed, loyal, halo and wings) and Paladin Recall.
- **Rades (Soul Corpse):** Reanimate, the named corpses Jimmy, Alfredo and Carl (roles invented), decaying stitched overlay, True Soul Corpse pact (cancels a death inside a window; a player already on the respawn screen cannot be revived in Minecraft).
- **Kahono (Song):** four song modes plus Crescendo, driven by Simple Voice Chat microphone loudness (optional dependency; hold V as a fallback without it).
- **Kiato (Butoh):** Dance state with unpredictable looping moves, evasion, shrunken hitbox, and the deflect dash.
- **Nacht (Shadow):** Shadow Travel (light below 4), Unite Equus and Felis, a Gallus call, armour overlays and `.geo.json` models for all four Devil modes.

### REPLACEMENT (the old is kept behind a switch)
- Rouge: the head-cat with a 12% dodge and a one-time death undo is replaced by the entity when `thread.rougeEntity` is on (default). Off restores the old behaviour.
- Spatial portal: the flat portal is replaced by a 3D animated one with a refracting shader, momentum through gates, and projectiles and items passing, when `spatial.portal3d` is on (default). Off restores the flat portal.

### Notes
- Magic merge generator (blend VFX, layers and models of parent magics) already exists since 1.27.0; not rebuilt.
- Butoh pages added at the end of its page list shift the universal pages after them by two mode numbers; ids are unchanged.

## [1.43.0] — Fix: part 4 relics could not be put into Curios slots

**State:** data fix, CI is the first load. Nothing run in game.

### FIXED
- 52 worn relics from the 1.41.0 batch (neck 6, hands 13, belt 2, charm 31) were missing from the Curios item tags (`data/curios/tags/item/{necklace,hands,belt,charm}.json`), so Curios refused them in every slot. The tags were last regenerated for 1.25.0; `tools/relics/gen_curios_tags.py` was not rerun after waves 20 to 30. It has now been, and **it must be rerun after every relic wave** (`python3 tools/relics/gen_curios_tags.py --check`). Entries are `required: false`, nothing existing was removed.

## [1.42.0] — Lucius Zogratis, final-arc overhaul

**State:** written without a compiler; CI is the first compile. Nothing run in game. Design and reasons in `docs/lucius_arc.md`.

### NEW
- Three acts (above 65%, 30-65%, below 30%) with the act name on the boss bar; switch `luciusAnimeArc` (default on; off = the 1.37.0 Lucius).
- Combo chains (Checkmate: gravity pin, spatial blink behind, Chrono Stasis bubble; every harmful step telegraphed 12+ ticks), Soul Touch, the Soul Corruption effect (-10/-20/-30% max health, floor 4 hearts, 60 s), reversible turning of pets and summons for 20 s (max 6).
- Precognition bypass rules (multiple attackers, rapid hits, anti-magic weapons via tag , two high-tier relics), act 1 levitation and teleport-away, act 2 Time Reversal channel (interruptible; switch ), act 3 clones (3-5) and the chest gem hitbox (crit or anti-magic hit shatters it).

### REPLACEMENT
- Old charge-based Precognition is replaced in arc mode (kept when the arc is off). Lucius skips the Gates refill in arc mode; Time Reversal is a second, owner-requested exception to the no-healing boss rule.

## [1.41.0] — Relics of the World, part 4: 124 new relics

**State:** written without a compiler; CI is the first compile. Nothing run in game. Every card's translation is in `docs/relics_of_many_worlds_part4.md`.

### NEW
- **124 relics in 11 waves** (Wave20 to Wave30), from the 114 part 4 card images plus your Hand and Eye of Vecna and Deck of Many Things; one more image per card read for art, rules and rarity. 46 of them are 3D hand-held weapons (melee, staves and wands, four bows, a crossbow-lute, four shields) with Tensura engravings, size-class meshes and Better Combat profiles; the rest are rings, charms, masks and helms, gloves and gauntlets, collars and medallions, potions and the Deck.
- **Hand of Vecna and Eye of Vecna** (GOD, bonded pair: they graft and cannot be taken off, `/vecna release <player>` frees them; Eye: truesight, X-ray glow, five spell acts with a 5% "Vecna's Hunger" that never takes control; Hand: strength, cold blows, Touch of Death / Rimed Grasp / Skeleton to Jelly; set bonus when both are worn: poison and disease immunity, regeneration, safe X-ray, a Wish every 30 days).
- **Deck of Many Things** (GOD): a 22-card shuffled draw, at most 3 draws a game day, every card a bounded effect (no relic or ledger item is taken, nothing kills); the deck crumbles after the 22nd card.
- **The Infinity Gauntlet and six Infinity Stones**, Paradox, Warren's Key.
- Duplicates skipped: Quickling's Ivy (already in the mod).

### Also done in this release
- Weapon classes were added for the new weapons (`tools/relics/weapon_classes.json`), the size/orientation/display-preset tools were run over them, and four orientation false positives (Efreeti's Kusarigama, Rift Breaker, The Reaver, the Librarian's Maul) were reviewed and settled.

## [1.40.0] — The Relic Forge: upgrade, socket and engrave at a table

**State:** written without a compiler; CI is the first compile. Nothing run in game. See `docs/relic_forge.md`.

### NEW
- **Relic Forge** (`nusmp:relic_forge`, craftable): right-click it to open a menu that does what the off-hand sneak flows did, which Better Combat's off-hand handling had made unreachable: **Upgrade** a relic's tier with catalysts, **Socket** a Spirit or Devil Core, **Draw out** a core with the Empty Element Core (Cycle chooses the socket), and an **Engrave** tab that adds a Tensura engraving from an enchanted book to a mod weapon (up to 3 engravings added at the forge, never replacing one). It runs the existing rules (attunement, costs, cooldowns, the roll, a failure never destroys a relic) and edits the one relic stack in place, so the ledger sees the same relic. Items left on the table come back on close. A Rules entry for it appears in the Codex.
- The sneak-use flows are kept and still work.

## [1.39.0] — Better Combat: the swing animations now load

**State:** nothing run in game; the format was checked against Better Combat's own repository files. See `docs/better_combat.md`.

### FIXED
- **Weapons showed Better Combat's pose but played no swing animation.** The custom animations of 1.38.0 were written to `assets/nusmp/attack_animations/`, a folder nothing loads; Better Combat resolves every `animation` id through Player Animator's registry, which reads `assets/<namespace>/player_animations/`. They are now generated there (`tools/bettercombat/gen_animations.py`), in the same form Better Combat's own animation files use (one part and one axis per move entry, `isLoop` as the string "false", `returnTick` 2, `description`). The profiles in `data/nusmp/weapon_attributes` are unchanged (every parent and animation id was checked to exist). The old `attack_animations` folder is left in place, unused.

## [1.38.0] — Better Combat support for every melee weapon, with custom swing animations

**State:** written without a compiler or Better Combat to load it; CI is the first compile. Nothing run in game. See `docs/better_combat.md`.

### NEW
- **A Better Combat profile for all 116 melee weapons** (`data/nusmp/weapon_attributes/<id>.json`, generated by `tools/bettercombat/gen_attributes.py`): parent, reach matched to the weapon's size class, two-handed where it is, and our own combos for greatswords (sweep, back sweep, slam), katanas (draw, rise, thrust), rapiers (three lunges), daggers, axes, hammers, dadao, whips and the twin pairs (both halves). Hand-written profiles for the Genesis Demon-Slayer, Elsdocia and the 22 Djinn weapons. Weapons of the other classes keep Better Combat's own combo for their parent.
- **10 custom attack animations** (`assets/nusmp/attack_animations/`): greatsword slam / sweep / back sweep, katana draw / rise / thrust, rapier lunge, axe chop, twin cross, whip lash.
- A build test fails when a melee weapon has no profile, so a new weapon cannot be added without one (run `python tools/bettercombat/gen_attributes.py`).
- Audit: weapon on-hit effects run from the normal damage path, not from click events, so a Better Combat swing still triggers them.

### CHANGED
- The 12 older profiles keep their `parent` and `range_bonus`; some gained attributes. Without Better Combat nothing changes: the files are ignored.

## [1.37.0] — The Codex stays ours, updates itself and learns your spells; bosses lose regeneration and Unique/Ultimate skills

**State:** written without a compiler; CI is the first compile. Nothing run in game. See `docs/codex.md` and `docs/boss_rules.md`.

### REPLACEMENT
- **The Great-Sage "Analysis complete" overlay on Tensura's own Skills windows is off** (new client config `ui.tensuraOverlay`, default false; the old code is kept). Analysis text now appears only in the mod's own menus (Codex, Multiverse Status, Four Kingdoms, Questionnaire, Grimoire Slot, Compatibility Codex, the page-unlock toast).
- **Bosses no longer regenerate and no longer use Unique or Ultimate skills** (`nusmp-boss-rules-server.toml`: `stripRegeneration`, `banUniqueUltimate`, `forbiddenWords`): regeneration/healing skills and effects are refused, removed on join and every 100 ticks, healing in combat is cancelled, and a cancellable skill-event veto stops forbidden skills. The old skill-lending code stays behind the filters. Exception on purpose: the Gates refill of the Zogratis bosses (your earlier request) stays, switch `refillAtGates` (default true).
- **Lucius's spell chooser** is replaced by a cooldown-managed combo planner (`luciusComboAi`, default true): four casts of distinct roles (spatial, offensive, defensive or control) and one heavy magic cast; no repeats inside cooldowns. The old chooser is the fallback.

### NEW
- **The Codex builds itself from data**: every folder `assets/<ns>/codex/<section>/` becomes a tab and every JSON in it an entry (title, text, discovery keys, icon, order, remove), other packs can add or override, F3+T reloads; generated tabs for Blocks, Entities, Magic attributes and Effects read the registries each time the book opens; a Rules tab (13 stock rules with live values) and a **Learned Spells tab** (the pages you have opened, grouped by magic, with cost, cooldown and what each does; sealed pages show as ???). The tab column scrolls.

## [1.36.0] — D&D layer of the Labyrinth and the Rukh particles

**State:** written without a compiler; CI is the first compile. Nothing run in game. See `docs/labyrinth.md` (D&D section) and `docs/rukh/r1_core.md`.

### NEW
- **Traps and puzzles** (Spire side rooms, `docs/labyrinth/f1_traps.md`): the Pitfall Tile (a floor tile that gives way to a spiked, dark cell), the Snuffer Glyph (Darkness, snuffs torches and refuses new lights near it), Sigil Plates + Riddle Stone + a third Gatekeeper door kind `puzzle` (open it by pressing the plates in the order the stone shows). Three new rooms: `side/pit_room`, `side/dark_room`, `side/puzzle_room`.
- **Monster economy** (`docs/labyrinth/f2_economy.md`): hostile monsters of the Labyrinth drop physical Magic Stones/crystals and monster meat; no vanilla XP orbs.
- **Dungeon hunger and gloom**: standing in the dark without a light darkens and weakens you; hunger drains 1.5x; low food slows you.
- **HP buffer and Vital Strike** (`docs/labyrinth/f3_vital.md`): Labyrinth monsters have 2.5x health; a heavy hit (18% of their max health) or four hits with a relic of Rare grade or higher is a Vital Strike that removes at least 35% of their health. Bosses excluded.
- Config `nusmp-labyrinth-dnd-server.toml` (every switch above).
- **Dark Rukh and Light Rukh particles** (`nusmp:dark_rukh`, `nusmp:light_rukh`): bird-flock sprites (ink-black flock and glowing cream flock), 15-25 tick life, translucent, budgeted (`RukhFx`, at most 96 per tick per level). The Djinn Equip now gathers Rukh toward you.

### REPLACEMENT
- **Vanilla particles removed from relics, equipment and active abilities**: all 731 files of the scope in `docs/rukh/scope.txt` (relics, relic waves, Djinn, weapons and items, spells (`MagicType` and the magics), skills, charter grimoires, Nexus relics) now emit Rukh particles only, through `RukhFx`/`RukhClientFx`; large spell bursts are capped (12 per call). A build test fails on any vanilla particle in that scope. Not touched, on purpose: bosses, the dungeon ecosystems and ambience, animals/entities, world blocks, and the Time Magic VFX (`book/TimeBook.java` and its layer).
- The asset path is `assets/nusmp/...`, not `tensura_black_clover` (the mod id is `nusmp`).

## [1.35.0] — The Labyrinth: Jigsaw dungeon architecture (gate, spire, trials, seals, vault)

**State:** written without a compiler; CI is the first compile. Nothing run in game. Master document `docs/labyrinth.md`; every file's WHY in `docs/labyrinth/*.md`.

### REPLACEMENT (owner's request: the cave dungeon generation is replaced; the old one stays registered, rule 2)
- **Dungeon generation**: the Living Dungeon's noise caves are no longer where the Maws lead. `labyrinth.engine` (`nusmp-labyrinth-server.toml`, default `labyrinth`) routes Maws and `/multiverse dungeon enter` to the new Labyrinth; `legacy` restores the old dimension. The old dimension, blocks, creatures, ecosystem, floor arenas and Nexus stay registered and loadable.
- **Relics of Many Worlds are exclusive to the Labyrinth** (`labyrinth.relicsExclusive`, default true): no vanilla chest, no mimic outside a dungeon dimension; vault chests, Relic Pedestals and boss drops only. A build test keeps relics out of every recipe and other loot table.

### NEW
- **Dimension `nusmp:labyrinth`**: a void sky; **Labyrinth Spires** (structure `nusmp:labyrinth_spire`, set `nusmp:labyrinth_spires`) built by Jigsaw from the chained pools base → ascent 1-4 → Warden hall → Boss Peak → Treasure Vault → crown; side chambers (trials, treasuries, corridors, balconies, a wildcard adapter for other mods' rooms), caps so no path dead-ends.
- **Dungeon Gate** (structure `nusmp:dungeon_gate`, set `nusmp:dungeon_gates`): a ruined Black Clover arch or a floating Solo-Leveling ring on the Overworld surface (`WORLD_SURFACE_WG` heightmap, `beard_thin`), in vanilla surface biomes and optional `c:` convention biome tags (Biomes O' Plenty, Terralith). Walk into the gate plane to enter; the Exit Gate on the arrival island brings you back; a fall into the void returns you to the island.
- **Trial chambers**: the Sealed Archway seals every doorway with Magic Barriers when a player enters, spawns the chamber's mobs (data in the template), and lifts the seal when they are dead; abandoned trials reset.
- **Boss seals**: Gatekeeper Doors (Warden kind: a Shadow Key dropped by the Floor Warden mini-boss; Boss kind: opened when the Peak Altar's floor boss dies). The Peak Altar spawns a boss from its roster (Dante, Vanica, Zenon, the Floor Warden) when the room is entered.
- **Treasure Vault**: `nusmp:chests/dungeon_relic_vault`, six Relic Pedestals (`nusmp:chests/labyrinth_pedestal`, one relic each, taken by hand), the `nusmp:relic` loot entry type (grade ranges, ledger-safe, pairs give both halves).
- **Processors**: `nusmp:biome_palette` (masonry adapts to the surrounding biome through the NeoForge data map `nusmp:labyrinth_palette`; inside the Labyrinth it mirrors the Overworld biome above the gate) and `nusmp:magicule_veins` (glowing veins, denser toward the Boss Peak and Vault).
- Blocks: Labyrinth Bricks/Tiles/Pillar/Glyph/Lantern, Magicule Vein, Labyrinth Gate, Labyrinth Exit Gate, Sealed Archway, Magic Barrier, Ward Rune, Spawn Glyph, Gatekeeper Door, Peak Altar, Relic Pedestal. Items: Shadow Key. Entity: Floor Warden.
- Commands: `/multiverse labyrinth locate|enter|leave|here|palette`.
- Tools: `tools/labyrinth/` (NBT writer, template DSL, generators for the gate, the spire and the textures). Opt-in datapacks in `docs/labyrinth/`: `vanilla_dungeons_off` and `labyrinth_wildcard_example`.

## [1.34.0] — Wands, grips, tooltips, Djinn weapons, armour build, boss effects and second health bar

**State:** written without a compiler; CI is the first compile. Nothing run in game.

### NEW
- **Djinn Half/Full Equip weapons**: 24 real temporary weapon items (light + dark) with two acts each, a safe loan/restore of the displaced hotbar item. `docs/djinn_weapons.md`.
- **Armour build**: all 11 Magic Knight robe sets, 16 more armour relics, the 5 transformation modes and the dungeon relics / charter grimoires drawn as relic-style 3D models, a generic fallback for future armour, armour engravings. `docs/armor_build.md`.
- **Boss second health bar** (`PhaseRefill`): every Zogratis boss refills to full at the 100% mode (Gates), shielded while it fills; bar tint glows. Hook for other bosses.
- **Boss effects**: telegraph and strike effects for every move of Dante, Vanica, Zenon, Lucius; new layers for the Devil Avatar, Goliath, Charybdis, God of Order, Zagred, Commander and Juggernaut. Eight new late-phase moves; health buffs on the four. `docs/boss_vfx_buff_b1.md`.
- **Compatibility Codex** is now a grimoire-style item and screen (the written book stays).
- Tooltips shown on relics in Curios slots, worn relics all drawn (toggle respected), `relicfx.hideArmourRelics`.

### FIXED / CHANGED
- **Weapon grips**: Windcutter, Tripolar Sword, Flamecutter, Encircled Blade were held by the blade; flipped.
- **Sizes**: class table made larger; Elsdocia bigger. **Wands** 0.55-0.85 -> 0.8-1.2 blocks; the Wand of Bubbles 0.67 -> 1.0.

### REPLACEMENTS
- Robe sets render with the library model for players (GeckoLib kept for mobs/stands/option off); look-only relic armour models upgraded to signature models. Nothing removed.

## [1.33.0] — Tensura No Index compatibility pack and the Compatibility Codex

**State:** written without a compiler; CI is the first compile. Nothing run in game. See `docs/compat/README.md`.

### NEW
- **Compatibility data pack** `nusmp_tni_compat` for Tensura No Index (0.71.0): built into the jar and switched on only when `tensuranoindex` is loaded, and also as a standalone zip `docs/compat/multiverse_x_tensura_no_index.zip` for a world's `datapacks` folder. It adds low-chance Tensura No Index loot (phase dust, name slips, Canalis relic, Philosopher's Stone) to our Library Ruins and Grimoire Tower chests and to our eight bosses, and puts our eight bosses and their seven into the `tensura:no_max_ep_plunder` tag (all optional ids).
- **Compatibility Codex**: a written book of what has compatibility with a live Loaded / Not installed status per mod (creative tab, `/multiverse compat book`, `/multiverse compat status`).

### Finding about the other mod's own bridge pack
Its bundled pack does not take effect: it has no `global_loot_modifiers.json`, targets loot tables of ours that no longer exist (`riven_remake`, `marquis_remake`), names a removed item in its loot table, and puts its tag in a misspelled folder. Ours repairs the tag and the chest loot; nothing is rolled twice. We do not edit their jar.

### Not covered
Their 15 Toaru codex skills (we have no data hook for a skill codex), their relics other than the Philosopher's Stone (not registered items), grimoires as loot.


## [1.32.0] — Ability Selection window: the magic circle goes behind it

- The anime overlay on Tensura's Ability Selection window drew its magic circle on top of the window, so its lines ran across the skill names, the slots and the panel. The circle is now drawn before Tensura's screen (behind it); Tensura's panel covers it and only the part outside the window shows. The corner brackets and the Great Sage readout are unchanged. Applies to the ability, mode and category windows.
- If the corner brackets do not appear, Tensura's panel bounds could not be read; the circle is then centred on the screen. Tell me and I will read the bounds another way.


## [1.31.0] — Hotfix: second first-launch config crash

- **Fixed the same crash as 1.29.0 in the new `nusmp-relicfx-client.toml` (1.30.0).** `relicfx.quality` used `defineInList(List.of(...))`; with no file yet NeoForge checks the missing value (null) against that list, `List.of(..).contains(null)` throws and the whole config load aborts, so other mods crashed reading their unloaded configs (SkyFracture). It is now a string with a validator accepting `low`, `medium` or `high`. Every other config of the mod was checked: none uses a list check any more.


## [1.30.0] — Weapon size and orientation, true pairs, worn relic models, tiers, sockets, boss DPS cap, Elsdocia

**State:** written without a compiler; CI is the first compile. Nothing run in game. Design notes (each with a 'Why' per component): `docs/weapon_scale_orientation.md`, `docs/relic_pairs.md`, `docs/relic_visuals.md`, `docs/relic_tiers_and_scaling.md`, `docs/relic_sockets_and_evolution.md`, `docs/boss_dps_budget.md`, `docs/elsdocia_rebuild.md`.

### REPLACEMENT (old kept or reversible)
- **Weapon orientation and size** (38 weapons): Sorrow's Edge, Astralaria, Axe of the Exile, Leviathan Axe, Random Axe of Charity, Hunger and Doom Hammer faced the wrong way (head toward the wielder) and are mirrored; 14 axe, hammer and cleaver heads are enlarged (hand axes ~0.34 blocks across, battle axes ~0.5); 18 short weapons raised to their class length. Every edit is recorded on the mesh (`# nusmp_fix`) and reversible with `tools/relics/apply_weapon_fixes.py`.
- **Per-game safety net**: `WeaponRenderer` now normalises each mesh as it loads (turns a tip-down mesh over, mirrors a head that points at the wielder, scales up anything under 60% of its class minimum) and logs one line per correction. Add a weapon and it is right even if the mesh is wrong. `orient manual` opts out.
- **Twin Hook Swords, Whirling Blades, Twin Swords of Deflection** are true pairs: a right half (the old id) and a `_left` half, each its own 3D blade, obtained together, one pair per world, with an off-hand second blow (60%, PvP-capped). Old worlds get the missing left half once on login. Per-blade damage lowered so a pair lands near the old single item.
- **Worn relics** get real 3D models (about 85 models, ~65 signature relics such as the Tiny Duck Crown, Circlet of the Phoenix, the dragon circlets, unicorn crowns) with grade effects (rim glow, shimmer, orbiting motes, rune ring, halo, particles) and a quality option `relicfx.quality`. Armour relic art got a wear coat and a glow channel.
- **Elsdocia** rebuilt from the reference (fractured crystal blade, club inlay, bronze guard) with a pulsing glow, held glitter, and a model json that now actually uses the 3D mesh. The old version is kept as `elsdocia_legacy`.
- **First-person, ground and item-frame scales** of long weapons re-run.
- Closed grimoire hip pose (already in 1.27.0) kept.

### NEW
- **Tier ascension**: relics rise through the seven Tensura grades with a sneak-use catalyst ritual (data component `nusmp:tier`, no new item ids): Devil's Core and Genesis Seed are new catalysts, Tensura ingots are used for the lower tiers. Stats, act power, costs and cooldowns scale with caps; failure never destroys the relic.
- **Aura scaling**: relic weapon damage grows with the player's max magicules and with awakening (capped lower against bosses, through the existing damage caps).
- **Sockets**: 15 Cores (`spirit_core_*` for six elements, `devil_core_*` for nine devils) bound by sneak-use give a passive buff and an active ability; socketed relics glow in the element colour with ambient particles; tier LEGEND and above uses the Genesis look. `/multiverse relic socket|unsocket|tier`.
- **Boss damage-per-second cap** for every mod boss (6% of max health per second by default, `nusmp-boss-budget-server.toml`): damage and direct health changes are truncated, so true damage and `/kill` cannot one-shot a boss.

### Verify in game
Everything visual (worn models, enlarged heads, mirrored weapons, the Elsdocia mesh, left-hand pair meshes), the pair login grant on an old world, the ritual and socket flows, boss fights with the DPS cap, and the particle budgets.


## [1.29.0] — Hotfix: first launch crash

- **Fixed a crash on the first launch after installing 1.27.0 or 1.28.0.** The new `nusmp-ui-client.toml` declared the menu theme with `defineInList(List.of(...))`; when the file does not exist yet NeoForge checks the missing value (null) against that list, `List.of(..).contains(null)` throws, and the exception aborted the loading of every config. Other mods then crashed reading their own unloaded configs (seen as Tensura No Index `TreeSeal` and `SkyFracture.tick`). The theme is now a plain string with a validator that accepts only `anime` or `classic`. If you hit this crash, update to this build; deleting the `config` folder entries is not needed.


## [1.28.0] — Review fixes for 1.27.0

- A wiki magic inside a merged magic keeps its own colour, so it no longer draws as Air.
- The 64 merged skills are registered last in `NUSkills`, so no existing skill's registry order moves.
- A failed profile build can no longer change which magics count as restricted; merged skills are not forgotten if the world data fails to load; the merged lookup is a map; the magic audit test cannot fail the build over a missing resource.
- A merged book reports its dominant parent magic instead of Flame in the grimoire page code.
- Discovery: relics placed straight into a Curios slot count as found; the first sync no longer flashes an empty cache; a bad packet length cannot throw; the skill read cannot throw out of the tick handler.
- GUI: the page-unlock toast no longer flashes opaque at the start and end of its fade; the Codex caches its per-frame strings and honours `ui.motion=false`.


## [1.27.0] — Merged magic, a safe magic framework, the anime GUI, discovery, the cast-hold fix

**State:** written without a compiler; CI is the first compile. Nothing run in game. Design notes: `docs/magic_merge.md`, `docs/magic_framework.md`, `docs/gui_theme.md`, `docs/discovery.md`, `docs/wiki_magic_vfx.md`.

### NEW
- **Magic merge generator**: `/multiverse magic merge <a> <b> [<c> [<d>]] [name]` (plus list, info, remove, grant, revoke, audit). Two to four existing magics become a brand-new magic with a blended spell list, blended VFX (every parent's own effect plus a composite layer), a blended temporary armour layer and a baked 3D emblem. Definitions are saved in the world and synced; the parents are never changed. Up to 64 per world.
- **Magic framework** (`core/magic/profile`): a `MagicProfile` for every magic with safe defaults, a registry that never returns null, an audit (test, server start, `/multiverse magic audit`) and a runtime `SafeCast` guard that contains a failing cast, logs it once and falls back to a generic effect, so a new magic type no longer needs edge-case fixes.
- **Discovery**: items, relics, spells, pages and creatures stay hidden (`???` or a silhouette) until a player finds or unlocks them. Existing owners keep everything (migration on first login). Config `discovery.showAll`.
- **Anime / Tensura GUI** (`ui.theme` = anime or classic): translucent glass panels, animated rune magic circles, Great Sage style analysis text; restyled Codex, Multiverse Status, Four Kingdoms, grimoire slot, questionnaire, relic tooltips, a page-unlock panel and a Great Sage readout on Tensura's ability selection window.
- **Wiki magic armour auras** for the 30 wiki magics (Guard page).

### REPLACEMENT (old kept where stated)
- **Wiki magic VFX** (`WikiMagicLayer`): a documented table maps the 30 magics to seven groups with their own texture, blend and shape (Sandstone sits in the shards group), null-safe with a GLOW fallback.
- **Held cast key**: the spell circle no longer strobes. The cause was a new circle every 4 ticks plus a body-animation restart on every press and fizzle. It is now one continuous circle that refreshes while held and fades out (`CastHold`); a tap behaves as before.
- **Closed grimoire** sits closer to the body (hip pose right 0.27 instead of 0.42, slightly lower).
- `MagicType.restricted()` now uses names instead of hard-coded ordinals (same eight values); `WikiBooks.color()` no longer throws for a wiki magic without a colour.
- The classic GUI drawing is kept behind `ui.theme = classic`.

### Verify in game
Everything visual (circle, armour, GUI mock-ups were rendered with a PIL re-implementation, not the game), the merged cast end to end, the hold-cast key behaviour with ManasCore, the page-unlock overlay, discovery sync on a server.


## [1.26.0] — Djinn review fixes

- A chained Half Equip hit (Baal) on a player is capped at 6 like a direct hit, also for a Dark Djinn.
- Trust and assimilation modifiers are re-applied on respawn and login, so a Djinn owner who died keeps them.
- Docs: a vessel destroyed for good leaves its Djinn bound; the owner frees it with `/multiverse djinn release`. The sheath orientation fix (1.25.0) reverts in `DjinnArmorLayer.sheath()` by scaling `(1/16, 1/16, 1/16)`.

## [1.25.0] — Boss overhaul, Charter Grimoires, relics part 2, unique and Dark Djinn

**State:** written without a compiler; CI is the first compile. Nothing run in game. Built from the owner's MAS prompt (`neoforge_mas_prompt.md`), the 143 card images of "Relics of the world" parts 2 and 3, the Magi picture references and the owner's fixes list. Design notes: `docs/boss_overhaul.md`, `docs/relics_of_many_worlds_part2.md`.

### NEW
- **Mod Boss Global Buff Engine** (`boss/`): permanent Tensura barriers, regeneration and theme skills on this mod's seven bosses only (namespace-checked), `bosses.globalBuffs` / `bosses.buffMultiplier` config. Lucius gets maxed pools, passives and three new telegraphed moves (Megiddo, Dimension Cutter, True Darkness); real nullification skills are replaced by capped damage cuts so he stays killable.
- **Charter Grimoires** (`charter/`): seven books (Dante, Vanica, Zenon, Lucius, Devil Avatar, Goliath, Charybdis Spawn). A boss drops its book once per world (`ModWorldDropData`, `has_dropped_charter_grimoire_<boss>`), first claim wins and is saved at once; holding or wearing a book lends its skill set temporarily (never `usurper`, never removes anything). `/multiverse charter status|give|reset`.
- **Animated boss bar** for the mod bosses (phase colours, flames, shine, shake on heavy damage, shatter on phase change); every other bar is drawn like vanilla and falls back to vanilla if anything fails. Client option `bossbar.animations`.
- **Worn renderers** for Dante's Gravity Crown (head), Zenon's Spatial Ring (arm) and the Artifact Mimic (belt, GeoLite model with bite), with their relics and rare drops from Dante and Zenon.
- **Relic bows and crossbows** (`RelicBowItem`, `RelicCrossbowItem`, `.bow()`/`.crossbow()`): vanilla draw timing, pull/charged/spent mesh states, acts on sneak+use and the worn-relic key. Genesis and five-leaf item layers with a holder-flag sync.
- **Boots slot** (`RelicSlot.FEET`, looks Boots/Sandals/Greaves) with a painted worn layer.
- **158 relics in 12 waves** (8 to 19): 59 weapons (all with 3D hand models and Tensura engravings, three real crossbows), 17 head and boots, 17 back and hand items, 17 neck and charm relics, 17 pouches and charms, 17 bags, 14 scrolls and consumables. Reviewed waves are listed in `docs/relics_of_many_worlds_part2.md`.
- **Dark Djinn**: the Dark Rukh Shard (dropped by the Blood-Red Commander, Devil Avatar and Lucius at 15%, vault chests 2%) darkens a bound Djinn; Holy Water or `/multiverse djinn dark off` purifies it. Stronger Maso and Extreme Magic, a heavier price (upkeep, assimilation, trust cap 60). Black-and-gold look with thorn halo, brow eye crest, rukh feathers, veins, dark bust, first-person arm and HUD tint.

### REPLACEMENT (old kept as fallback where stated)
- **Djinn Half and Full Equip looks**: each of the 12 Djinn now has its own helm, wings, weapon, back piece, tail, halo and particles; the old shared rig stays in the repo as the fallback.
- **First-person display** of 35 long weapons (`tools/relics/display_presets.py`): they no longer fill the view. Third person is untouched. A hand-tuned model can opt out with `"nusmp_display_preset": "manual"`.
- **Boss bars** of the mod bosses (the vanilla bars of every other entity look the same).
- **Lucius**: his profile and move list are extended; his old 20 moves keep their indices.

### FIXES
- **Dying with a Djinn vessel no longer takes the Djinn off.** The owner keeps the bond and the vessel's binding; only the running phase ends.
- Relic summons are discarded when the server stops and when they load back from disk (they were saved untracked and stayed forever).
- Review fixes across the new waves: damage caps against players, mayfly/game-mode restores, duplication loopholes (Satchel, Bianca refund, pouch nesting), unique relics refused inside pouches and bags (so the Order can count them), state-mesh skins fall back to the weapon's skin, stack-state changes no longer replay the equip animation.

### Verify in game
Everything. In particular the boss-bar reflection into `BossHealthOverlay`, worn renderer placement, bow/crossbow draw, the Djinn assets (about 20 MB of new textures), the numbers of the buff engine, Dark Djinn balance, and `gen_curios_tags.py` slot tags.

## [1.24.0] — Relics of Many Worlds and the Djinn

**State:** written without a compiler; CI is the first compile. Nothing run in game. Built from the owner's 78 card images (87 distinct cards), the Magi Djinn guide and the Tensura equipment rarity guide.

### Replacements (owner request: "remake the 16 in my mod to work with this new system")
- **The 16 existing items** (Quickling's Ivy, Magebane Longsword, Magebane Shield, Bolt Thrower, Brooch Pin, Club of Rot, Crossbow of Swift Death, Doom Hammer, Weapon of Resurgence, Hand of Providence, Infernal/Devil Contract, Prism Blade, Permafrost Sabre, Cyclone Rapier, Bloodbound Katana, Thundercleave) now carry a Tensura grade, attunement, the abilities of their cards and the uniqueness rule. Item ids, sprites, engravings and old techniques stay registered. Bloodbound Katana: undead and constructs no longer feed the blade. Devil Contract is named **Infernal Contract**, gains four contracts and the Damned curse, and its hand model is the card's scroll (the old book mesh is kept as `devil_contract_book`). Boreal Blades = the Permafrost Sabre + Cyclone Rapier set ability (Fusion).
- 15 of the 16 are now **unique**: a second copy is retrieved by the God of Order (a 10-minute census runs first in existing worlds).

### New
- **Seven Tensura grades** (Normal, Special, Rare, Unique, Legend, God, Genesis) with grade colours, glow and particles, EP lift, attunement (3 at a time), passives, actives with magicule/aura, EP, cooldown and per-day costs, a Use Worn Relic key (V), and a tooltip that wraps, shrinks and pages so it never leaves the screen.
- **70 new relics** in seven waves (weapons and staves with 3D models; charms and cloaks; headwear, gloves, belt and drinks; amulets and pendants; armors and robes). Card abilities became real mechanics; what base Minecraft cannot do uses lent Tensura skills (checked against the Tensura id list), Tensura stats, or an invented mechanic. See the docs for each card.
- **The Order**: one copy of each relic per world; a second copy summons the **God of Order**, who retrieves it (not farmable, no loot); works with chests, shulkers, ender chests, Lootr per-player chests, mobs and item entities. Commands `/multiverse relic order ...`.
- **Loot and Mimics**: unissued relics appear in dungeon-type chests (Lootr-safe through the ledger) and drop from Mimics (any mimic entity).
- **The Djinn**: 12 Djinn Seals, a one-owner-per-Djinn ledger, a binding ritual into a Legend-grade vessel, household sharing, four phases (Lesser Summon, Half Equip, Full Equip/Maso, Extreme Magic), per-Djinn armor drawn as render layers, HUD and keys (J/K/H). A player holds only one of Black Clover Spirit, Devil Contract or Djinn; Astralis is the multi-bond exception at a price. Magoi = Tensura magicules.
- Commands `/multiverse relic ...`, `/multiverse djinn ...`. Mesh loader accepts smoothed per-vertex normals (`qn`).

### Honest list
Everything is unverified in game: relic abilities, all 3D looks (previews only), the God of Order fight, the Djinn render layers and first-person arms, networking, balance numbers. Heavy meshes (Catstaff about 7,000 quads, each Djinn armor 3,500 to 4,900) may cost frame time. Many Tensura skill, effect and damage ids are looked up at run time and skipped silently if absent. Admin grants bypass the one-bond rule.

## [1.23.0] — The floor arenas: Goliath's Pit (Upper Catacombs) and the Charybdis Basin (Deep Waterways)

**State:** written without a compiler; CI is the first compile. Nothing run in game: no arena built, no boss fought. Charybdis' model is rough. **All new** (nothing replaced).

- **Arena cells.** About 8 percent of the non-core cells of the Upper Catacombs and the Deep Waterways build a 64-block arena instead of nothing (the Variable Core's four zones are unchanged; two zones appended to the Nexus framework: `GOLIATH_PIT`, `CHARYBDIS_BASIN`). Admin: `/multiverse dungeon nexus zone goliath|charybdis`.
- **Goliath's Pit** (Floor A): a pale stone-brick colosseum sealed in bedrock, three gates, a Treasury behind a mana gate. **The Goliath** (1500 health) rises from the seal; Ground Slam, Twin Stomp, Roaring Shockwave, Bedrock Fist, Seismic Line, Rubble Fall, Bedrock Quake at 60 percent power, a cracked Gates form at 25 percent. Its fall starts the Treasury Collapse.
- **The Charybdis Basin** (Floor B): a drowned, flooded circular basin with kelp-wrapped pillars, prismarine and sea lanterns, a dry Treasury behind a mana gate. **Charybdis Spawn** (1100 health) surfaces when a player nears the dais: Magicule Scale Volley (parry with a shield or the Magebane parry window, or strike a scale to send it back for about 4 percent of its health), Tentacle Sweep, Whirlpool, Beak Lunge, Summon Megalodons (real Tensura megalodons, at most 4), Tidal Crush, Maelstrom of Scales at 60 percent. Water conducts lightning as before.
- Both pools hold the four relics of the Sentient design; floor bosses leave Magic Stones through the Sentient death rules.
- Commands: `/multiverse boss goliath [rise]`, `/multiverse boss charybdis`; spawn eggs. 
- **Honest list:** Charybdis collapse leaves its water standing, which pours into the caves; the flooded plan, the deflect rules, the Goliath rise and every fight are unverified in game; the Charybdis art is rough (thin fins, undefined head). With `dungeon.sentient=false` a Goliath kill still leaves one carcass block.

## [1.22.0] — The Sentient Dungeon: wall births, the immune system, ash and Magic Stones, two relic slots, the Blood-Red Commander, the Devil Avatar, Collapse v2

**State:** written without a compiler; CI is the first compile. Nothing run in game: no wall birth seen, no Juggernaut woken, no boss fought, no relic worn; every number and look is a first estimate. Built from the owner's "Technical Design Document: The Sentient Dungeon".

### Replacements (the owner's design replaces these; the old code stays in the repo and runs again with `dungeon.sentient=false`)
- **Ecosystem spawning -> wall births.** With `dungeon.sentient` on (default) nothing in the Dungeon breeds, hunts or sleeps by itself any more: the food web (`population`, `WildDriver`) is switched off. Every room a player enters is rolled once and, if populated, its walls crack and birth a capped group (Catacombs: goblins, slimes, hound dogs [the Kobold stand-in]; Waterways: lizardmen, aqua slimes, water spirits; Underworld: lesser devils and Dark Disciples; never more than 8 a room; refill after 30 minutes).
- **Carcasses -> ash and Magic Stones.** Dead creatures turn to ash and leave Tensura Magic Stones/crystals by tier; 12 new rare materials drop (4 percent, plus Looting).
- **Wild Magic Surge** of the Gambler's Page now aggroes the immune system (the 1.21.0 collapse trigger stays for `sentient=false`); its mana toll now persists through relog and death.
- **Relic slots.** All Relics move from ring/charm/back/belt to two dedicated Curios slots `dungeon_relic` (never more than two work). Relics worn in the old slots return to the inventory at the next login.
- **Treasuries and vaults** hold the four relics of the design (Band, Sheath, Page, Amulet); Chalice and Mantle stay in the game but are no longer placed. Minor pedestals never carry a twin of the central relic.
- **Collapse wording and guards.** "[Server] The Dungeon wails in agony! The floor is collapsing!"; the guard waves become a frenzied horde born from the walls (caps 24 alive, 6 per exit, 56 total), six waves.
- **The Monarch -> the Blood-Red Commander.** Two phases (50 percent and 25 percent), forced summoning of minions, three new telegraphed arts (Lunge, Cross, Executioner's Arc), red look; rule update in place (not gated by `dungeon.sentient`).

### New
- **The immune system.** Threat from hammering unbreakable walls (4), blasting them (1 a block) and casting massive spells (cost c percent adds c*c/10), decaying 1.5 a second; the walls tremble at half the limit, at 100 a **Juggernaut** is born (health x4 up to 1024, damage x2.2, boss bar), cooldown per player, at most 3 alive; boss rooms and Nexus volumes are sealed from passive threat.
- **Amulet of the Juggernaut's Blindness** (masks mana signature; curse Muffled Mana halves natural mana regeneration). Use Relic key R casts the Gambler's Page from the relic slot. Attunement (10 s sneak) now resets if you move more than a quarter block or are hurt.
- **The Devil Avatar** (Avatar of Lucifero / of Megicula): Underworld Gate floor boss with gravity (Ceiling Pull, Floor Crush, Inversion Field) and fast curses (Hex Volley, Dread Brand, Curse Nova, Shadow Step); killing it starts the Treasury Collapse. Halls built before this version get one after `/multiverse dungeon nexus reset`.
- **Collapse v2:** Spatial Surge (a 2 s tear, then a throw of 8-24 blocks toward safe ground, never lethal; off with `dungeon.hazards=false`).
- New commands: `/multiverse dungeon sentient status|birth|threat|juggernaut|rooms forget`, `/multiverse boss avatar [lucifero|megicula]`. New server config: sentient, roomPopulation, roomRebirthMinutes, maxBornPerPlayer, juggernaut, juggernautThreat, juggernautCooldownMinutes, stoneDrops. New tags `nusmp:dungeon/born_*` and `juggernaut_*`.

### Honest list
Unverified in game: wall-crack visuals and births, how Tensura's brain mobs react to the driver, Juggernaut behaviour, every boss fight, the Curios slots with the real mod, the Amulet's effect on Tensura's regeneration attribute, the 3D look of the Avatar and Amulet (previews only). The 12 materials are plain sprites without 3D hand models. The Avatar and the Commander rework are not gated by `dungeon.sentient`. With 4 treasury relics the fifth pedestal is left empty.

## [1.21.0] — The Variable Core: the Procedural Dimensional Nexus, hidden vaults, delving gear, five Relics and the Treasury Collapse

**State:** written without a compiler; CI is the first compile. Nothing run in game: no zone was ever built, no boss woken, no relic attuned; every number and every look is a first estimate (see the end for the honest list). Built from the owner's design document (`living_dungeon_complete_tdd.md`), translated from Forge 1.20.1 to NeoForge 1.21.1. **Replacement (owner request):** the static Golden City is no longer built by itself: the deepest floor's core cells now pull one of four zones from the multiverse. The 1.19.0 city code, its Altar and Aurelian Core stay in the repo and are the heart of the Golden City zone (a city built by an older version is adopted, never built over). Everything else below is **new**.

### The Variable Core (design section 5)
- **A Nexus builder** (`dungeon/nexus`): when a player in the Dungeon comes within 56 blocks of a core cell (the deepest tier, 128 x 128 blocks), the cell builds itself as one of four zones chosen by a stable hash of the cell (the server and the clients agree on it): lazy, resumable after a restart, budgeted per tick, loaded chunks only, written through the Dungeon guard. Admin: `/multiverse dungeon nexus where | build | zone <monarch|underworld|mirage|golden> | reset`. The first player into a core cell breaks its seal: "[Server] An immense Mana presence is felt as a Spatial Nexus seal is breached. <name> walks the <zone>." The deepest tier is now called **The Variable Core**; its fog, particles and ambient sounds follow the zone.
- **The Monarch's Vault** (Solo Leveling): a colossal hollow obsidian hall piled with gold, artifacts and bones (nothing lootable, nothing breeds), a Treasury behind a mana gate, and one towering boss asleep on the dais: the real Tensura **arch daemon**, grown to 4800 health. It wakes when someone comes within 18 blocks, hits it, enters the Treasury or takes the central relic. Five telegraphed moves (Domain Slam, Shadow Rain, Summon Shadow Soldiers (real lesser and greater daemons), Soul Rend, the Monarch's Authority), three phases at 66% and 33%. When it falls the Nexus collapses.
- **The Underworld Gate** (Spade Kingdom): a dark gravity-warped ruin round a vast Gate with the Treasury behind a mana gate; gravity zones that press down, let go and flip on a slow visible cycle (telegraphed four seconds ahead, never lethal); a Gatekeeper daemon, lesser daemons and **Dark Disciples** channelling Lucifero, Megicula and Beelzebub at 10 to 40 percent of a Devil's power. (`DarkDiscipleEntity.spawnFree`: new, for disciples that serve a place, not a boss.)
- **The Tempest Mirage**: a twisted overgrown illusion of a monster village (plaza with a banner, town hall, 18 houses, fake houses that dissolve into ambushes) full of mutated Tensura orcs, lizardmen, slimes, goblins and hound dogs under a **hive-mind tactical AI** (scouts that raise the alarm, tanks, flankers, skirmishers, slimes that ring and slow, wounded members that fall back; it learns where you shoot from and splits a clumped party). Every change of the hive's state is announced; damage per blow and per second is capped.
- **The Golden City** (Delicious in Dungeon): a pristine sealed cavern biosphere (golden and azalea great trees, glow berries, waterfalls, moss meadows, house-size mushrooms) round the 1.19.0 city, with apex and legendary predators over herds of grazers, and a **Biosphere cycle** that shifts and regrows the garden near you (flowers bloom and wilt, vines grow and retract, moss spreads, groves sprout). New block Golden Leaves.
- Every zone has a **Treasury behind a mana gate** with minor relic pedestals and exactly one **central** relic: taking it (or killing the Monarch) collapses the Nexus. Because the Dungeon is unbreakable, every zone opens **ways in** from the caves (an open iron door keeps the flooded caves out) and retries while players are near.

### Hidden vaults, Descent Gates and spatial distortion (design sections 3 and 4)
- **Illusion Stone and Illusion Deepslate**: they look exactly like the rock, have no collision, block light and cannot be washed away or seeped through by water. Tag `nusmp:illusion_stones`.
- **Hidden vaults** are carved lazily next to the caves behind an illusion wall (resumable, bounded, written through the Dungeon guard): the **Puzzle Vault** (nine plates, a hinted order, iron bars over a minor relic or a food cache), the **Nutrient Garden** (moss and Nutrient Blooms) and the **Guardian Vault** (a buffed tier-3 Tensura mob named Vault Guardian over a minor relic). **Descent Shrines** hold the Descent Gates (extra scope: the design gave nothing that places gates, and the lantern needs gates in the world).
- **Descent Gate**: step into it (or use it) and you arrive on the next floor at the same x/z through a matching gate; the way back is a rising gate. Deep to Core only above a core cell whose Nexus is built. A failed attempt costs nothing.
- **Spatial distortion**: Warp Traps lie in distorted pockets (a barely visible rune; throws you 8 to 24 blocks away, dizzy), gravity pulses warn with a spiral and then lift or slam (never damage), and blocks you place in a pocket are refused ("Space here rejects what you build."). The Band of the Spatial Anchor ignores all of it; `dungeon.hazards = false` turns it off.
- **Nutrient Bloom** and **Nutrient Nectar**: a flower that ripens slowly and gives a rich, safe food.
- Admin: `/multiverse dungeon vault where | build [puzzle|nutrient|miniboss|gate]`, `/multiverse dungeon gate where`.

### Delving gear and organ extraction (design section 4)
- **Ki-Resonance Lantern** (3D in hand, four flame states): off-hand lantern fuelled with Magic Stones, shards, crystals and slime (tag `nusmp:lantern_fuel`). Its flame is amber, turns **blue** facing an illusion wall within 14 blocks and **red** facing the nearest descent gate within 96 blocks. Recipe: iron, glass and a slime ball.
- **Anti-Bird Compass** (3D in hand, 32-frame icon): a rare drop (4% + 1.5% per Looting) of avian creatures killed by a player in the Upper Catacombs. In the Dungeon the needle points at the nearest cluster of life (densest chunks by biomass), spins wildly inside a pocket of warped space.
- **Organ extraction**: with the Butcher Cleaver the height of the click on a carcass picks the cut (upper flank: organs, a Venom Gland from basilisks and spiders, a Magic Stone from anything above tier 1; middle: the prime meat as before; lower: bone meal or leather). New item **Venom Gland**; spider carcasses are a new carcass kind.

### The Treasury and its five Relics (design section 5)
- **Relic Pedestal** and **Mana Gate** blocks. The gate opens for 5 seconds near a Mana Zone or Magic Sense and never shuts on someone in the doorway. Taking a **central** relic collapses the Nexus.
- Five relics, each working only worn (a Curios slot, or the off-hand without Curios) and **attuned** (hold in the main hand and sneak for 10 uninterrupted seconds; at most 3 at once; release by sneak-use, dropping it or `/multiverse dungeon relic release`; death breaks every attunement):
  - **The Glutton's Chalice** (charm): raw monster meat becomes a safe full stew and for minutes shuts out suffocation, hot floors and magicule radiation. Curse *Symbiotic Hunger*: drains hunger and magicules after three unfed days.
  - **Mantle of the Scavenger** (back): apex predators ignore you. Curse *Prey's Weakness*: physical blows do nothing to tier 2 and 3, and taking it off brands you with a 30 s Pheromone Mark.
  - **Band of the Spatial Anchor** (ring): immune to the Dungeon's spatial traps. No curse.
  - **Devil-Bound Weapon Sheath** (belt): charges from parried or blocked magic; at 100 the next melee strike releases a cross-dimensional Anti-Magic slash. Curse *Mana Siphon*.
  - **The Gambler's Grimoire Page** (off-hand): a random overpowered spell of five, 60 s rest. Curse *Wild Magic Surge*: a hidden d20, a 1 backfires.
- Curios: tag edits for charm, back and belt and a new ring tag. Commands: `/multiverse dungeon relic give | attune | release | status`.
- **Visuals**: the lantern, compass, chalice, sheath and page are real 3D models in hand (previews only); the mantle and the band are sprites.

### Dungeon Maw portals (design section 2)
- A Dungeon Maw now **manifests** in the Overworld as a plain stone **tower** (5 x 5, 8 to 11 high, a root-plugged doorway, a ladder inside, a battlemented deck), **ruins** (a half-sunken chamber with broken walls, fallen columns and rubble, roots over the breach) or an **isolated gate** (two pillars, a lintel, roots across the frame). A stable hash of the place and the biome picks it; moss and cracks follow the biome. If the picked shape does not fit the ground, the other two are tried, and the 1.17.0 plain mouth is kept as the last fallback.
- The root mechanic is unchanged (the mouth cannot be entered until roots are cut; at least 8 roots always stay). When a Maw closes only its roots and the Maw vanish: the stone stays as ruins.
- Built safely, since the Overworld has no Dungeon guard: loaded flat-enough ground only, nothing but grass, leaves and natural ground in the way, never in water or round a creature, footings under low columns, a hard cap on writes and a rollback of the whole patch if anything fails.
- **New broadcast wordings** (owner's text, still behind the `broadcast` config): "[Server] A spatial distortion has materialized. A new Dungeon Tower (Ruin, Gate) has breached the <Biome> biome near (x, z)..." and "[Server] A localized famine in the <tier> has caused a mass predator die-off." The closing line is unchanged.
- Admin: `/multiverse dungeon maw open [tower|ruin|gate|hole]`.

### The Treasury Collapse (design section 6)
- Taking a Nexus's **central relic**, killing the Monarch, or a Wild Magic Surge backfire inside the Dungeon starts a **three minute collapse** (`DungeonCollapseEvent` answered by `CollapseManager`); one per 96 blocks; saved and resumed after a restart. The cell is marked COLLAPSED at the start: it is never rebuilt, breeds nothing, and Descent Gates refuse it.
- "[Server] The Mana Zone has destabilized! The Nexus is collapsing!" Players inside get a title, the clock and the way to the nearest exit over the hotbar. Biomass and oxygen of the footprint are held at 0 (thin air).
- 3:00 to 2:00 cracks and dust only; then the stone comes down as falling blocks, bottom-up, from the far side towards the exits (bounded per tick, at most 240 in the air, vanishing where they land so rubble never walls a corridor). The 7 blocks round every exit stand until the end and get only single telegraphed blocks (at most 3 damage, none in the last 5 seconds). Anyone under the front is warned 4 seconds ahead.
- A **Root Gate** opens at each way out. **Five waves of mutated guards** (real roster mobs, tier 2 then some tier 3, named and outlined, announced by portal light) come out of the exits: at most 10 alive, 3 per exit, 24 in all; they never go for creative or spectating players.
- At 0:00 the Nexus is wiped in bounded passes and whoever is still inside is **expelled** with 75% of their maximum health in damage (never lethal: half a heart is always left; `LETHAL` switch in `CollapseExpel`) and Magicule Poisoning III for two minutes.
- A collapse outside any standing Nexus (a Wild Magic Surge in the catacombs) is a **pocket**: the same clock, guards and expulsion, rubble rains but no terrain is removed. Admin: `/multiverse dungeon collapse start | stop [all] | status`.
- The Dungeon guard now lets falling blocks spawned by the mod's own code (inside `DungeonGuard.internal`) join the world; every other gravity block and TNT stays frozen.

### Replacements and what is new (CLAUDE.md rules 2 and 5)
- **Replacements (owner request):** the static Golden City is no longer built by itself, the Variable Core zones do it (the old city code, Altar and Aurelian Core stay and are the Golden City zone's centre; an old city is adopted); the Maw's opening and famine broadcast wordings (new text); the deepest tier's label ("Golden City" to "The Variable Core"; the enum name is kept for saves). The 1.17.0 hole stays as the Maw's last fallback.
- **New:** everything else in this release. Nothing was removed.

### Not verified (honest list)
- Nothing was run in game and this is the first compile (CI). No zone has ever been built, no boss woken, no relic attuned, no collapse run; every number is a first estimate.
- The Tensura mobs are brain-driven (SmartBrainLib): the Monarch's shadow soldiers, the Mirage hive, the Underworld servants, the Vault Guardians and the collapse guards are driven from outside (target, navigation, strikes); a brain may undo a formation order between two passes. Damage per blow and per second is capped regardless.
- The ways in to each zone are cut from nearby caves; a cell with no cave near it may stay closed until a player brings one close. The vault and relic density is generous and meant to be tuned.
- `FallingBlockEntity.disableDrop()` and a few API calls were written from the dumps and memory; CI is the check.
- The look of the three Maw manifestations, the zone interiors and the five relics' 3D models were only checked in previews.
- Variable Core volume above y=-40 (roof gardens, skywalks, domes) now counts as the core tier (`DungeonTier.CORE_TOP = -26`).

## [1.20.0] — The Zogratis bosses: Dante, Vanica, Zenon and Lucius

**State:** written without a compiler; CI is the first compile. Nothing run in game: no boss was ever spawned, so every number (health, damage, cooldowns, telegraph timing) is a first estimate. All **new**; nothing replaced. The request was phrased for Bedrock JSON packs; this mod is a NeoForge Java mod, so the bosses are Java classes plus GeoLite models in this repo (see `docs/zogratis_bosses.md` for the file map).

### How to fight them
- Spawn with `/multiverse boss dante | vanica | zenon | lucius | triad` (ops) or the spawn eggs in the creative tab. `triad` brings the three Dark Triad rulers of the Spade Kingdom together; when one falls the others rage (+10% power).
- **The Percentage Power System** (`entity/zogratis/PowerCurve`, `ZogratisBoss`): each boss draws out its devil's power as its health falls: 20% at the start, 40%, 60%, 80%; at 25% health the **Gates of the Tree of Qliphoth** open: 100% power and a transformation (the Triad become partial devil hybrids with new models; Lucius takes his divine form). From 60% power the boss's mana is unbound. The boss bar shows name, devil and percentage.
- **Fair by design:** every move has a telegraph (at least 8 ticks), ultimates are spaced out and leave the boss exposed for 3 seconds, a single hit cannot skip a power step, damage scales with the victim's EP but is capped by the pvp hit cap. World damage (craters, rot, bone, domains) only happens with the `griefBlocks` rule on, is budgeted per tick, and every changed block is put back when the fight ends (also inside the Living Dungeon).
- **Dark Disciples:** elite followers infused with up to 40% of the boss's devil power (Lucifero: gravity slam; Megicula: weakening curse; Beelzebub: blink strike; Astaroth: slow-then-dash), capped, gone when their master dies, nothing dropped but XP.

### The bosses
- **Dante Zogratis (Lucifero, 1200 health):** Body Magic passive (a rate-limited mortal-wound regeneration pulse: cut by anti-magic or a burst of damage), Gravity Magic fields that slow, pin and crush, compressed rock sword volleys, 12 moves, and the Gravity Singularity ultimate (black spheres over telegraphed rings; bites terrain only with griefBlocks, restored afterwards).
- **Vanica Zogratis (Megicula):** Blood Magic lances, scythe and whip with blood drain (healing capped), summoned **Blood Beasts** (new minions), Curse-Warding **Curse of Decay** (new effect *Megicula's Decay*: weakness, cut healing, a growing bite; broken by leaving the sigil, anti-magic or a cleanse) and a spreading Rotting Field, 13 moves, ultimate Crimson Crown.
- **Zenon Zogratis (Beelzebub):** Bone Magic spears, blades and bone gardens, Spatial Magic portals and blinks, 11 moves, and **Spatial Mana Domination** (Absolute Space): a telegraphed cubic domain that severs mana: no grimoire spells (a check at the top of `GrimoireBook.castPage`), no Tensura skills (ManasCore skill events), spell damage cut to a fifth; weapons, items and movement still work; it can be left, and shatters if Zenon takes enough damage.
- **Lucius Zogratis (Astaroth, 3600 health, the endgame boss):** Soul Magic (soul drain, **Soul Purge** that strips Devil Union and attunement and cuts the Triad off from their devils, memory manipulation, resurrection of his clones), **Soul Paladins** (powerful mobs made loyal and immortal until their soul is cut with anti-magic/holy/soul damage or Lucius falls; new effects *Soul Paladin* and *Purified*), **Two Souls** (at 75/55/35/12% Julius Novachrono's will surfaces for 8 s: Lucius stumbles, takes more damage, the party is blessed), Time Magic (precognition dodges, light-speed steps, Chrono Stasis, Time Stop up to 3 s, Time Reversal, acceleration; the existing Time Magic effects are called, not changed), absorbed Gravity and assimilated Body/Blood/Bone through the siblings' own moves, **Grimoire Tower Access** (casts a real page of any magic, 'The towers answer'), realistic clones, 20 moves, four ultimates, divine form at the Gates.

### Look
- New GeoLite models, textures, glow layers and animations (idle, walk, run, attack, cast, cast_big, hurt, transform, death) from your reference images (`tools/gen_zogratis_triad.py`, `tools/gen_zogratis_lucius.py`): base and devil-hybrid forms of the Triad, Lucius human and divine forms (six wings, gear halo, antlers, floating golden grimoire), the Dark Disciple (four devil tints), the Blood Beast and the Soul Paladin. The faces and clothes are original pixel-art interpretations.

### Not done / unverified
- No loot drops, no boss music or custom sounds, no advancement; Lucius's Time Reversal rewinds only himself.
- Unverified in game: all balance numbers, the fight loops, the renderers, the ManasCore skill-event veto of the domain (a refused release of a channelled skill), and the look of every model.

## [1.19.0] — The Living Dungeon, polish: real Tensura mobs, Ice and Fire compatibility, unbreakable blocks, the Golden City, its own sky and sounds, meals that lend skills, a Maw reward

**State:** written without a compiler; CI is the first compile. Nothing run in game. Every part below is **new** except where it says **replacement**.

### Replacements (owner request: "not custom ones")
- **The Dungeon's creatures are now the real mobs of base Tensura** (and of Ice and Fire Community Edition when it is installed), not the custom ones. **Replacement:** the ecosystem no longer spawns the custom Dungeon Slime, Living Armor, Direwolf and Basilisk of 1.18.0; those classes, renderers and assets stay registered (old worlds may hold them, and they count as roster members and are the fallback if no roster mob exists).
- **Roster by entity-type tags** (`data/nusmp/tags/entity_type/dungeon/`: `tier1`, `tier2`, `tier3`, `legendary`, `grazers`, `aquatic`, `excluded`): every entry is optional, so a mod that is not installed is skipped and a modpack can edit the lists. Tier 1 holds slimes and small things, tier 2 the Armorsaurus (it takes the old Living Armor's place), Lizardmen, Orcs, hound dogs, ants and the like, tier 3 the Direwolf, Basilisk, Black and Knight Spiders, Blade Tiger, Giant Bear and more; Ice and Fire's Pixie to Hydra, Cyclops, Gorgon, Sea Serpent etc. sit in the same tiers (their ids are from memory: a wrong one is skipped, the log shows the real counts). Fire, Ice and Lightning dragons are **legendary**: one at a time, only where the chunk is rich in life, the fire dragon in the Vents, the ice dragon in the waterways, the lightning dragon in the Golden City. Bosses and story mobs are excluded.
- **Driven from outside** (`dungeon/wild/WildDriver`): Tensura's mobs think with brains, not goals, so a server sweep gives them the food web: hunger (with starvation), hungry tier 2+ creatures hunting a weaker tier within 16 blocks, tier 1 and grazers eating carcasses and mycelium, a full apex predator resting (and waking on hunger, damage or a player coming near), the same breeding gates as 1.18.0 (slimes where magicules are dense, tier 2 where there are slimes, apex where there is something to hunt). Births now stand on the floor nearest the players, not the highest one in the column. Their own AI keeps deciding about players; nothing new is aggressive to players.
- **Carcasses by kind**: Tensura slimes leave the slime carcass, the Armorsaurus the armor one, the Basilisk and Direwolf theirs, everything else the generic one. The Butcher Cleaver's Anatomical Cut marks any roster creature for an extra portion.
- `/multiverse dungeon spawn slime|armor|direwolf|basilisk` now spawn the Tensura equivalents; new `spawn mob <entity_type>` and `spawn custom slime|armor|direwolf|basilisk` (the old ones); `census` counts by tier.

### Unbreakable (owner request: "unbreakable by any means")
- **Every block of the Dungeon is unbreakable** (`dungeon/DungeonGuard`, config `unbreakable`, default true): mining (creative too; the crack never starts), explosions, mob griefing, pistons, axe/shovel/hoe changes, farmland trampling, and a mixin on `Level.destroyBlock/removeBlock/setBlock` and `LevelChunk.setBlockState` that catches mods, spells and skills that break blocks directly. Fire cannot take a protected block either. Carcasses, Dungeon Mycelium, fluids and fire are exempt on purpose (the ecosystem eats them). The mod's own builders (city, floor cutting, the Root Gate, spell blocks being taken back) run inside `DungeonGuard.internal(...)`.
- Side effects, on purpose: falling blocks and lit TNT do nothing in the Dungeon (they would copy blocks), placed sand hovers, `/setblock`, `/fill` and `/clone` are refused there (turn the switch off to edit), and a player who walls themselves in can use `/multiverse dungeon exit`.
- Not covered, honestly: writes below the chunk API (world generation, WorldEdit-style section editors), a protected block swapped for another solid block (spreading sculk, a spell turning stone to ice), and mods that drop items and then fail to remove the block. Mobs' griefing is off for every entity in the Dungeon, which also stops loot pickup and villager farming there.

### The Golden City
- **A real place** (`dungeon/city/*`): when a player in the Dungeon comes within 56 blocks of a city cell (`DungeonTier.isCityCell`, an addition), the city builds itself over a few seconds of ticks, resumable after a restart (`CitySave`): a sealed vault with a carved cavern, a broken outer wall with two sealed golden gates and up to three open iron-door ways in where a cave comes close (the Dungeon is unbreakable, so a city with no way in could not be entered), four avenues, a plaza with a glowing moat and a dais, 8 to 14 buildings (towers with stairs, halls, shrines, obelisks, arches, some ruined), lamp posts and chandeliers. The layout is deterministic per cell (`CityPlan`, pure and tested).
- **The Aurelian Altar** (new block): each player may claim one **Aurelian Core** from each city; using the Core in the air spends it and grants a 20 minute **Golden Ward** (no magicule radiation). 2 to 4 guards (Armorsaurus-class mobs of base Tensura) stand on the plaza. Nothing in the city is a chest.
- Admin: `/multiverse dungeon city where|build|reset`.

### Magic Sense, meals that lend skills, the Maw reward
- **Magic Sense blunts the city's radiation** (it builds only every other second, with a one-time hint); a Mana Zone or a Golden Ward shuts it out completely and lets it drain (`dungeon/RadiationShield.java`).
- **Cooked meals lend a Tensura skill for a few minutes** (`MealSkills`): Candied Slime Jelly = Magic Sense, Grilled Armor Flank = Physical Attack Resistance, Roast Basilisk Fillet = Poison Resistance, Roast Direwolf Haunch = Danger Sense. Taken back when the time is up; relogging, dying or restarting cannot make it permanent; a skill you already own is never touched. Every skill id is checked against the registry at run time.
- **Meals attune you to a second magic** (`MealAttunement`): Gel, Iron, Poison or Blood. While attuned your spell hits also carry that magic's rider, at most once a second per target. Tooltips and the action bar say what and for how long.
- **Maw reward** (`MawReward`): everyone who chopped a Maw's roots and stands within 32 blocks when it closes gets experience and 1 to 3 **Maw Fruit** (the faster the cut, the more). Maw Fruit feeds, gives a short Regeneration and +15% spell power for 3 minutes.

### The Dungeon's own sky, fog and sounds
- **Sky and fog** (`client/dungeon/DungeonSky`, effects id `nusmp:living_dungeon` in `dimension_type/living_dungeon.json`: **replaces** the Nether look it used): each tier has its own mood, blended smoothly: cold grey-blue dust in the Upper Catacombs, deep teal in the Deep Waterways, red-black smoke and embers in the Volcanic Vents, warm gold haze and gold motes in the Golden City. Lava, water, powder snow, Blindness and Darkness fog are left to the game.
- **Ambient sounds** (`dungeon/DungeonAmbience`): tier-specific vanilla sounds near each player, and a far growl when an awake apex predator is within 40 blocks.
- A custom biome `nusmp:dungeon_depths` (new chunks only; existing chunks keep their stored biomes) joins the two vanilla cave biomes.

### Not done
- Predator den structures and pack behaviour for the real mobs (Tensura's Direwolf spawns as a pack by itself); the custom Living Armor model was dropped on request (the real mobs are used instead).
- Everything above is unverified in game. Honest list: the biome file, the fog hook, the Level/LevelChunk mixins, whether Tensura's brain-driven mobs obey the driver (hunting, resting), Ice and Fire's ids and dragons, and the city's look.

## [1.18.0] — The Living Dungeon, stages 2 and 3: the food web, carcasses, the Butcher Cleaver and cooking

**State:** written without a compiler; CI is the first compile. Nothing run in game. The creatures' AI, the births and the harvesting have never been seen working; the Direwolf and Basilisk models were checked with the repo's headless preview only. All **new** (nothing replaced). The weapon is the Butcher Cleaver (3D model, engravings, Curios belt slot).

### The food web (stage 2)
- **Four creatures** (`dungeon/DungeonCreature.java` and subclasses, spawn rules in `DungeonEcology.population`): **Dungeon Slime** (tier 1, three sizes by growth: grazes the chunk's magicules, the mycelium and carcasses), **Living Armor** (tier 2, a mollusk colony wearing found armour: hunts slimes), **Direwolf** and **Basilisk** (tier 3, apex: the Basilisk's bite poisons). Each has a **hunger** (0-100) that falls with time: hungry predators hunt (`HuntForSustenance`: the nearest prey within 16 blocks, kill = hunger +50 and the chunk's magicules +20), a **full predator sleeps** and ignores everyone (`Sleep`), one that starves takes damage, and players are prey only to a hungry creature or to one defending its den (within 6 blocks of where it lives). Slimes flee when hurt.
- **Closed-loop breeding** (every five seconds, per simulated chunk): slimes are born where magicules are dense (each birth costs 40) up to what the air can feed; Living Armors appear where there are at least 4 slimes and the air is fit to breathe; predators come where there are at least 2 Living Armors. A chunk that held six or more and is now empty has **collapsed**: a server-wide broadcast. Biomass is recounted from the living each pass.
- **Nothing drops loot in the Dungeon** (config `noLoot`, default true; players excepted): what dies leaves a **Carcass** block (`CarcassBlock`, no block entity: kind, meat portions and decay stage are block states). Scavengers eat a portion at a time; left alone a carcass rots through three stages (about 3 minutes) and returns to the soil: its nutrition goes back to the chunk's magicules and a Dungeon Mycelium veil grows there, which slimes graze. Creatures from other mods that die there leave a generic carcass too.
- Config `life` turns breeding off. Commands (admin): `/multiverse dungeon spawn slime|armor|direwolf|basilisk`, `census`.
- **Models:** the slime and the Living Armor use the game's own slime and humanoid models with textures of their own (`tools/gen_dungeon_assets.py`); the Direwolf and the Basilisk are GeoLite models with walk, idle, attack and sleep clips (`tools/gen_dungeon_mobs.py`, `client/dungeon/*Renderer`).

### The table (stage 3, "Delicious in Dungeon")
- **Butcher Cleaver** (`item/ButcherArts.java`, Kind appended at the end): a cook's cleaver in a real 3D model; right-click a living thing in front of you for the **Anatomical Cut** (armour does not stop it, it bleeds, and a Dungeon creature cut this way leaves an extra portion); right-click a **carcass** to cut the meat out: the Cleaver finds the prime cuts (sometimes two); any other blade (sword, axe: the `nusmp:butchering_blades` tag) takes one cut; anything blunt, a pickaxe or a bare hand, **ruins the meat**. Tensura engravings: severance, sturdy, enervation.
- **Raw monster meat** (slime jelly, armor flank, basilisk fillet, direwolf haunch, monster meat) is **toxic** (Magicule Sickness) unless the eater has Predator or Gluttony; holy water or honey purifies it (shapeless recipe, `#nusmp:purifiers`) into plain safe food. **Cooked** in a furnace, smoker or campfire it becomes a meal that feeds well and **bends the wizard's magic for a few minutes** (`dungeon/MealBuffs.java`, read by `ManaLaw` beside the Signature): Candied Slime Jelly (spells cost 15% less, 5 min), Grilled Armor Flank (+5% power, 4 min), Roast Basilisk Fillet (+20% power, 3 min), Roast Direwolf Haunch (+10% power, speed, 3 min), Seared Monster Meat (plain). Temporary skills and secondary attributes from meals are not done.

## [1.17.0] — The Living Dungeon, stage 1: the dimension, its ecosystem, its hazards and the Dungeon Maw

**State:** written without a compiler; CI is the first compile. Nothing run in game. The dimension reuses vanilla's `minecraft:caves` noise settings (a stone cave world with a bedrock roof and floor and water at sea level) and was never loaded: if the game refuses the dimension file the Maw will say "the Dungeon does not answer". The design is `docs/living_dungeon.md` (the original Forge 1.20.1 design translated to NeoForge 1.21.1: data attachments instead of capabilities, a dimension instead of a biome modifier). All of this is **new**; nothing replaces anything. The creatures, carcasses and cooking are stages 2 and 3 (1.18.0, 1.19.0).

### New
- **The dimension `nusmp:living_dungeon`** (`data/nusmp/dimension/living_dungeon.json`, `dimension_type/living_dungeon.json`): an enclosed cave world, y -64 to 128, no sky. Nothing spawns in it by itself: natural, chunk-generation, structure, patrol and reinforcement spawns are cancelled (`DungeonEcology.onFinalizeSpawn`).
- **The ecosystem of every chunk** (`dungeon/EcosystemData.java`, a NeoForge **data attachment** on the `LevelChunk`, `LivingDungeon.ECOSYSTEM`): magicule (0-1000), biomass (0-400), oxygen (0-100). Chunks within `simRadius` (3) of a player tick every five seconds: the air comes back toward its ceiling (60 in the Volcanic Vents), the roots feed magicule and life eats it. Spells cast in the Dungeon spend the chunk's magicules and **flame magic burns its oxygen**.
- **The four tiers** (`DungeonTier`): Upper Catacombs (y >= 24), Deep Waterways (the flooded floors), Volcanic Vents (patches of 112 blocks: burn damage, thin air) and the Golden City (below y -40, in rare 128-block cells). Hazards (config `hazards`): **heat** burns anyone not fire-resistant or wet in the Vents; **thin air** (oxygen at 35 or less) gives *Suffocation*; **magicule radiation** in the Golden City builds *Magicule Poisoning* (maximum health -10% a level, five levels) unless you keep a **Mana Zone** up; **water conducts**: an electric hit on someone in water jumps at half strength to every swimmer within 6 blocks and slows them; fire and explosion damage burn the chunk's oxygen. The first player into the Golden City breaks its seal for the whole server (a broadcast, once).
- **Three mob effects** (`DungeonEffects`, with icons): Suffocation, Magicule Poisoning, Magicule Sickness (raw monster meat, stage 3).
- **Mined terrain grows back** (`DungeonSave`, saved with the world): stone, deepslate, ores and dripstone broken in the Dungeon return after `regrowDays` (default 2 in-game days), never while a player is within 6 blocks; a **Dungeon Mycelium** veil grows in the hole meanwhile.
- **The Dungeon Maw** (`dungeon/DungeonMaw.java`): every grimoire spell adds `cost%^2/100` to the ambient magicule of its region (4x4 chunks; it decays 30 a minute); when a region crosses `mawThreshold` (default 600) the ground near the caster tears open into a **Dungeon Maw** wrapped in **Maw Roots** (cannot be broken itself; cut the roots with an axe to reach it, or to close it). Step in and you land on a safe floor of the Dungeon at the same x and z, beside a **Root Gate** that takes you back where you stood. A Maw closes when its roots are all gone or after `mawMinutes` (20). Server-wide broadcasts for a Maw opening (rounded to 100 blocks) and closing, and for the Golden City's seal (config `broadcast`). A region that opened a Maw falls to a quarter of the threshold; `mawCooldownMinutes` (30) keeps two apart.
- **Blocks:** Dungeon Maw, Root Gate, Maw Root, Dungeon Mycelium (models, textures from `tools/gen_dungeon_assets.py`, lang, axe tag; in the creative tab).
- **Commands:** `/multiverse dungeon status | exit` (everyone), `enter | maw open | maw close | ecosystem magicule|biomass|oxygen <v>` (admins).
- **Config:** `nusmp-dungeon-server.toml` (`enabled`, `hazards`, `broadcast`, `mawThreshold`, `mawMinutes`, `mawCooldownMinutes`, `regrowDays`, `simRadius`).
- Tests: `DungeonTierTest`.

## [1.16.0] — Every magic at the tower, fused grimoires, magic that is born, and a signature for every wizard

**State:** written without a compiler; CI is the first compile. Nothing run in game. The effects (`BornMagicLayer`) and the fused cover geometry have never been looked at on a screen; the blend table and the trait numbers are first guesses.

### Replaced
- **The tower no longer gives only Fire, Water, Wind and Earth.** The Tower Wizard's nature questions (six of them, drawn from a pool of twenty) now add points to sixteen *flavors* (Hot, Cold, Flow, Gust, Stone, Metal, Gem, Green, Bright, Shade, Soul, Space, Time, Body, Mind, Maker), never to a magic by name, and every attribute carries one or two flavors (`wizard/MagicFlavors.java`), so **every attribute that can be rolled** (all but Anti-Magic, Kotodama, Curse and the devil-only Demon magics) can be the answer; a two-flavor attribute (Glass: Gem and Hot) needs both. A mild tilt makes the rarest attributes rarer. Switch: `wizard.allMagics` (default true; false = the ceremony's `starterMagics` as before). The questionnaire is now 12 questions (6 nature, 2 hearth, 2 blood, 1 resolve, 1 class).
- **Two attributes no longer mean two grimoires.** When two attributes from different flavors are nearly tied (chance `wizard.dualAttributeChance`), they **fuse into one grimoire**. If the pair has a well-known blend the grimoire is that attribute's book (Fire + Water = **Glass**, Fire + Earth = Magma, Fire + Wind = Explosion, Water + Earth = Mud, Wind + Earth = Sand, Plant + Maker = **Cotton** as for Charmy, Fire + Maker = Food, ... 45 blends, `MagicFlavors.blendOf`), with the pair written on it. If the pair has no blend in the game, **a new magic is born** of them, named from the two ("Pyro" + "tide" = Pyrotide Magic). (`GrimoireAcceptance.grantExtra`, the old second grimoire, stays in the code but the tower no longer calls it.)

### New
- **A custom-looking grimoire for a fused mage.** `BookLook` takes the pair: the leather is the two parents' colours blended, the metal is the second parent's, the glow both; the geometry grows a generated filigree (`grimoire/FusionDesign.java`, built into the plan): a ring of gems round the medallion alternating the two metals, corner brackets, gems down the spine, different for every pair, on the closed book and the open V. The item's tooltip says "Fused: Fire + Water" or "Pyrotide Magic, born of Fire and Water".
- **No two wizards' magic is the same** (`mana/Signature.java`): every grimoire grant also gives a *signature*: a name unique on the server ("Ashen Flame", "Sunless Gale of Blackmere", a registry saved with the world keeps them apart), two *traits* (small trades of spell power, mana cost and recharge: Overcharged, Quickened, Thrifty, Heavy, Focused, Wild, Steady, Radiant) that apply to every spell of the grimoire, and a *shape*. It shows in the Multiverse status panel and on the Signature page.
- **Custom spells** (`book/SignatureSpell.java`), appended after the Mana Zone so no mode number moves: the **Signature Spell** (every grimoire; named after the wizard's signature; its shape, a lance, nova, wave, orb, rain or spiral, is the wizard's own), and, only on a fused grimoire, **two more** (Fusion Spell, Born Spell) in the next shapes, heavier, built from both parents: both parents' on-hit effects land and both palettes are drawn.
- **A procedural effect generator for them** (`vfx/client/layer/BornMagicLayer.java`; shapes `BORN_BEAM`, `BORN_NOVA`, `BORN_WAVE`, `BORN_RAIN`, appended): the server sends only the first parent's colour, the second's (the seed's high half) and a style; the client builds the whole effect (two strands of motes winding round a core, a circle and a counter-turning ring, rings racing out, a falling bolt), so a magic born of any pair has effects in its own two colours without any asset being made for it.
- Tests: `FusionDesignTest`.

## [1.15.0] — The Magebane Shield is a real shield, the weapons fit Curios slots, and the questionnaire is no longer blurred

**State:** written without a compiler; CI is the first compile. Nothing run in game. The Curios integration reads Curios by reflection and has never met the real mod.

### Fixed
- **The Tower Wizard's questionnaire was blurred** (sheet, title and question): the screen drew its parchment and text and then called `Screen.render`, whose background pass blurs everything already on screen, so only the buttons stayed sharp. The sheet is now part of the background layer and the text is drawn after it.

### Replaced (the shield's use)
- **Magebane Shield: hold right-click to block, like a vanilla shield.** It reports `SHIELD_BLOCK`, `UseAnim.BLOCK` and a 72000-tick use, so blocking, the lowered arm pose, durability loss on blocked hits and an axe knocking it down are the game's own. The old one-press, 6 s-cooldown Spellbreak Parry no longer locks the shield: raising the shield opens the Parry whenever it has recharged (its own timer, 18 s by default), lasting 2 s (3 s with the Magebane Longsword in a hand) only while the shield stays up; otherwise the shield is simply a shield. Sneak + raise fires Absorb Surge when it is ready and a caster is near, else you just block. A client item model override (`blocking`) raises the shield in front of the first-person view (`models/item/magebane_shield_blocking.json`; the transform is a guess, not seen in game). Nothing removed: the passive magic cut, Aegis Slam, the bash and the paired Dispel Surge parry are unchanged (the latter no longer needs the shield up).

### New
- **Curios support.** Every weapon is listed in `data/curios/tags/item/` so Curios can socket it: blades on the **belt**, greatswords, hammers, crossbows, the trident, tools and the Magebane Shield on the **back**, the Hand of Providence in the **hands** slot, the Brooch Pin as a **charm**, and all of them in the generic **curio** slot. `neoforge.mods.toml` lists Curios as an optional dependency. With Curios installed, a weapon worn in a slot (and not also in a hand) grants a small sheathed bonus shown in its tooltip (e.g. katanas +4% speed, Elsdocia +4 health, the Magebane Shield keeps its magic cut and +1 armour); `item/CuriosLink.java`. The numbers are first guesses to tune. Curios draws nothing on the body for them (no worn renderers yet).

## [1.14.0] — The Convergence system is removed (on the owner's request)

**State:** written without a compiler; CI is the first compile. Nothing run in game.

### Removed
- **The Convergence** (`multiverse/Convergence.java`): the 5% anomaly roll, the origin and stages (SIGNS to SPADE), the rumours, the anonymous announcements, and the rule that only anomalies could be chosen by a grimoire. **Secret Quests** (`SecretQuests.java`) and the **FTB Quests "Convergence" chapter** (`FtbQuestsChapter.java`) that only served it, the `/multiverse convergence ...` and `/multiverse quests ...` commands, the status panel's Origin and Secret Quests blocks, `docs/convergence.md`, and every `convergence.*` config entry (the grimoire guard moved to its own `[guard]` section: `guard.grimoireGuard`, still on by default).
- Because of this the Grimoire Towers generate from the start (they used to wait for the Clover stage), `/multiverse locate tower` works for everyone, and any player without a grimoire is eligible for the old ceremony as well as the Tower Wizard. A grimoire granted by the old ceremony is announced like any other (3-leaf quietly, 4-leaf and up to the server).
- Old worlds keep a harmless saved-data file for the removed system; nothing reads it.

## [1.13.0] — The Tower Wizard calls every mage, whatever the Convergence says

### Fixed
- **Almost nobody could get a summons.** The Convergence (on by default) made only its 5% "anomaly" players eligible for a grimoire, so with the Tower Wizard on (`wizard.enabled`) the summons, the library and the questionnaire were closed to everyone else. In wizard mode eligibility is now just "no grimoire yet" plus `everyoneEligible` (or an admin's eligible flag); the Convergence's anomaly rule applies only to the older ceremony (`wizard.enabled=false`) and to the Convergence's own staged grant for anomalies. A player who receives a grimoire from the wizard counts as revealed to the Black Clover side (the Convergence already treats every grimoire holder so). Not run in game.

## [1.12.0] — The Tower Wizard has his own model

**State:** the model was checked with the repo's headless preview only (geo check OK, 0 errors); the Java renderer is new and uncompiled until CI; nothing run in game.

### Replaced
- **The Tower Wizard is no longer the vanilla witch.** From the owner's design (`docs/attributes/art_reference/tower_wizard_design.png`): a 60-cube model about 1.9 blocks tall with the hat (`tools/gen_tower_wizard.py` -> `geo/entity/tower_wizard.geo.json`, `animations/entity/tower_wizard.animation.json`, 256x128 `tower_wizard.png` and `_glow.png`): a wide-brimmed, tipped-back brown hat with a floppy cone and a band, a larger head with heavy lids, bushy brows, a nose and moustache, a long grey beard of stacked blocks, a flared layered robe with a belt and mantle, wide sleeves, and a glowing green orb at chest height with orbiting motes and a halo. Idle (breathing, beard and hat sway, orb bob) and talk (a nod and a hand gesture, played while a player stands within four blocks). `client/wizard/TowerWizardRenderer.java` draws it through the GeoLite pipeline with the glow pass. The vanilla-witch renderer is no longer registered for him (the entity class still extends the witch for its behaviour).
- Preview scenes: `python3 -B tools/render_preview/preview.py tower_wizard_idle` (front, three-quarter, full body, side).

## [1.11.0] — The wizard's library dimension, next-step hints, one menu, social class buffs and debuffs

**State:** written without a compiler; CI is the first compile. Nothing run in game. The Tower Wizard's custom model comes in a later version.

### New
- **The Tower Wizard's library** (`wizard/WizardLibrary.java`, dimension `nusmp:grimoire_library`): right-click the Grimoire Summons and it carries you to a round reading room in the void (shelves, lanterns, a rug, a lectern, the wizard at the centre) with 26 books flying around whoever stands in it (they exist only while someone is inside). Nothing there can be broken. Sneak + right-click the wizard, or finish the questionnaire, and you are sent back to where you stood.
- **Next-step hints:** when you reach the age / level the summons arrives with a chat message and a title ("You are summoned: right-click the Grimoire Summons"); a reminder every two minutes until you go; "click an answer" and "sneak to leave" in the library; after the grimoire arrives, what to do next. The summons item is used up.
- **Social class has buffs and debuffs** (it was flavour only): Royalty (spells 5% cheaper, +1 luck / -2 max health, 10% fewer gold stars), Noble (spells hit 4% harder and cost 3% less, +5% skill mastery gain / -1 armor), Commoner (+5% skill learning), Peasant (+4 max health, +3% speed, 10% more gold stars / spells cost 5% more and hit 4% weaker). The questionnaire has a childhood question that sets it; `/multiverse profile class` still works. Shown in the status panel.

### Replaced
- **One menu:** the Four Kingdoms slot in Tensura's magic screen (and its label) now opens the Codex, which holds every page (overview, status, grimoire, weapons, relics, bestiary, spirit lords), and the small "Multiverse" button in the corner of Tensura's screens is gone. The older Four Kingdoms screen is kept in the code but no longer opened from the menu.

## [1.10.0] — Costs through Tensura's consume helpers; resistances as flat modifiers

**State:** written without a compiler, from the owner's reading of the Tensura 2.0.1.3 bytecode; CI is the first compile. Nothing run in game.

### Replaced
- **Mana Zone and Negative Mana now pay through Tensura.** `EnergyHelper.isOutOfMagiculeConsuming(entity, cost, min)` deducts the cost itself and returns the unpaid shortfall (0 = paid): the Mana Zone's per-second upkeep uses it with a 5% floor and the zone collapses (and the mage faints) on a shortfall; opening a Mana Zone costs 5% the same way. Attuning to a Magic Stone now costs 20% of the magicule through `EnergyHelper.isOutOfEnergy` (true = cannot afford, nothing deducted; the stone is then not used up). No more direct writes of the magicule value for these.
- **Kingdom and blood resistances are flat modifiers.** Tensura's resistances have a base of 0, where a percentage does nothing: they are now flat additions (+/-0.05 to 0.1). Attributes with a base of 1 or more (element power, regeneration, gain, dodge strength, learning and mastery gain) stay percentages of the base.

## [1.9.0] — Kingdom and blood buffs and debuffs use Tensura's own attributes

**State:** written without a compiler, against the Tensura 2.0.1.3 signatures the owner supplied; CI is the first compile. Nothing run in game.

### New
- **Each kingdom and each mixed-in blood now also has a buff and a debuff on a Tensura attribute** (`TensuraAttributes`): magicule / aura regeneration and gain, skill learning and mastery gain, dodge strength, magic and elemental resistance, elemental power (e.g. Spade +10% darkness power, -10% magic resistance; Elf +10% wind power, -10% earth resistance; Undead +10% darkness resistance, -10% light resistance). They are percentages of the attribute's base (so they scale with Tensura whatever its units), added next to the earlier health / attack / speed modifiers, and shown in the status panel and the result of the questionnaire. **Unverified:** the real base values of these attributes in Tensura; a percentage of a base of zero does nothing.
- The Wizard King's mana regeneration now goes through Tensura's `EnergyHelper.gainMagicule` instead of writing the magicule value, so it respects Tensura's own gain rules.

### Not changed
- Mana Zone and the Negative-Mana cost still write the magicule value directly: the signature of Tensura's consume helper (`isOutOfMagiculeConsuming`) does not say what its double means, and `drainEnergy` is for taking energy from another entity, not for paying a cost.

## [1.8.0] — The Wizard King's awakening runs through Tensura's own numbers

**State:** written without a compiler in the session, against the Tensura 2.0.1.3 API signatures the owner supplied; CI is the first compile. Nothing run in game.

### Replaced
- **The crown's awakening now uses Tensura's awakening mechanics** (`multiverse/CrownAwakening.java`) instead of only this mod's attribute buffs: it multiplies the base of Max Aura and Max Magicule (`TensuraAttributes`) by the bigger of the hero and demon-lord EP multipliers from Tensura's race config, refills both, and evolves the Tensura race to its awakening evolution (once), exactly as `RaceHelper.awakening` does for a hero or demon lord, **without** setting the true-hero or true-demon-lord flag. Tensura's `AWAKENING_EVENT` is refused for the Wizard King, so the crown holder cannot also awaken as a hero or a demon lord, and a mage who already is one cannot take the crown. Losing the crown divides the multiplier back out (the evolved race stays). Since Tensura's EP follows those attributes, every EP-scaled number of this mod (spell damage, balance law) rises with the crown. The earlier attribute buffs, Saint Stage casting and the Wizard King skill stay.

## [1.7.0] — The Wizard King: one holder, an awakening of its own, its own skill

**State:** written without a compiler in the session; CI is the first compile. Nothing run in game.

### New
- **The Wizard King is a title only one player on the server can hold** (`multiverse/WizardKing.java`, saved with the world). Take the crown with `/multiverse wizardking claim` (a Grand Magic Knight with at least `wizard.wizardKingMinStars` net stars, default 100, and more net stars than the holder), or an admin `appoint` / `dethrone`s. Setting the rank to Wizard King any other way goes through the crown, so there can never be two. Losing it (also while offline) takes everything back.
- **It counts as an awakening, a third path beside a true hero and a demon lord (you cannot choose those: the crown is its own).** The holder gets the same *kind* of power an awakening gives: +20 max health, +4 armor, +2 toughness, +15% attack, +10% speed, +0.3 knockback resistance, +2 luck; Saint Stage casting (spells 20% cheaper and 25% stronger on top); mana that refills itself (1.5% a second) and life that mends; and no black-out from spent mana. Tensura's own EP and its hero / demon lord awakening are not touched (no safe API for it): the crown's power is this mod's own, on top of the Tensura stats.
- **The Wizard King's skill** (Unique; only the holder has it; mode key cycles): *Sovereign Decree* (16 blocks: foes slowed, weakened and lit up, allies resistant and regenerating), *Crown of Mana* (a free Mana Zone for 60 s), *Sovereign Strike* (a 40-block piercing bolt), *Royal Guard* (8 s of immunity to spells, plus absorption), *Return to the Capital* (home at once, 5 minutes). Passive: all damage -10%.
- `/multiverse wizardking info|claim|appoint|dethrone`.

## [1.6.0] — The Tower Wizard: a summons, a questionnaire, a grimoire that floats to you; kingdom and blood buffs and debuffs

**State:** written without a compiler in the session; CI is the first compile. Nothing run in game. Details: `docs/mana_system.md`.

### New
- **Grimoire Summons** (item) given when you are old and grown enough (play time and/or experience levels, configurable), **Tower Wizard** (a witch-model NPC at the tower), a randomly drawn **questionnaire** (a screen) that decides your magic attribute(s) (a second grimoire for a dual mage), your grimoire type, the **kingdom** you hail from (buff and debuff) and the **blood of a Tensura race** mixed into yours (buff and debuff). The grimoire then floats from the wizard to you.
- **Heritage** (`mana/Heritage.java`): the kingdom and blood modifiers and Mage Stage buffs, as transient attribute modifiers on top of the player's Tensura stats. Shown in the status panel.
- `/multiverse wizard summon|spawn`.

### Replaced
- At a Grimoire Tower or Altar an eligible mage is now sent to the Tower Wizard instead of being chosen at once (`wizard.enabled`, default on). The 1.5.0 choose-your-magic list and the old roll remain for `wizard.enabled=false` and for admin grants.

### Not done
- The wizard has no custom model or skin (he is the vanilla witch); the questionnaire screen is plain parchment with buttons; towers that were never visited are not found by the summons.

## [1.5.0] — The mana system of the video, with no bad draws

**State:** written without a compiler in the session; CI is the first compile. Nothing run in game. Full table (video vs. mod): `docs/mana_system.md`.

### New
- **Mana as life force** (shock at 5%, black-out at 0.5%; lethal only with `mana.lethalExhaustion`), **Mage Stages 9-0 / Arcane / Saint** from rank and class (power from training, not from the draw), **Affinity** (loved / ordinary / hated: a balanced trade you can change any time), **Mana Zone** (page in every grimoire; thick mana in the deep sea, the Nether, mountains, the deep dark and the End makes it stronger: Grand Magic Zones), **Magic Stone** and negative mana with corruption, `/multiverse mana ...`, a Mana block in the status panel.
- **Mana Strike**: a universal combat page in every grimoire, so a healing, sealing or trap magic is never a dud.

### Replaced
- **The ceremony no longer rolls your magic: you choose it** (`mana.chooseMagic`, default on; chat buttons or `/multiverse choose <magic>`). The cover (rarity), Black Magic, God-Tier and Convergence paths are unchanged. Set `chooseMagic=false` for the old roll.
- The old fixed exhaustion at 5% mana is now `ManaLaw.exhaust` (same shock, plus the black-out).

### Not done
- Dungeons made of mana; mana inherited from parents; Mana Method runes do not scale with stage yet.

## [1.4.0] — Brooch Pin and Hand of Providence get their own abilities

**State:** not yet compiled when written; CI is the first compile. Nothing run in game.

### Replaced
- The stopgap `PinProvidenceArts` (fold / unfold, Mage Hand) is no longer wired (the file is kept). **Brooch Pin**: Piercing Pin, a lunge of up to 7 blocks that runs through every foe on the line (armour does not stop it) and slows them, striking the first from behind; Ruby Ward absorbs damage (`BroochPinArts`). **The Hand of Providence**: Laying On of Hands, a big mend, Regeneration and every curse and poison cleansed on the ally you look at within 6 blocks or on yourself (`HandOfProvidenceArts`). Engravings updated.

## [1.3.0] — Riven Remake removed, eight reference weapons, high-resolution weapon skins, real-life sizes

**State:** every commit of this line compiled in CI up to the Riven removal and the size/skin pass; the Brooch Pin / Hand of Providence wiring is new in this release. Nothing here was run in game.

### Removed (on the owner's request)
- **Riven Remake / Marquis Remake** completely: the boss and its story constructs, renderer, boss bar, config (`nusmp-riven.toml`), `/nusmp riven` commands, combat code, models, textures, animations, loot tables, the Black Bull Bard Relic, Story Page, Crest Cape token, Cracked Crown and spawn egg, the Riven canon grimoire, its boss VFX shape (`RIVEN_VOID_GATE`) and all lang entries. The anime-skill codex (`skill/codex`, `data/nusmp/skills/codex`) existed only for Riven's research tools and went with him. Sylph's Gale Feather stays.

### Fixed
- **Codex weapons page ran over the frame:** a normal (not turning) page was drawn without a clip, so long weapon lists spilled over the book edges and the page title. Pages are now clipped to the paper; the per-weapon line cap is gone; indented tooltip lines stay indented and wrap under themselves.

### New (from the reference sheets)
- **Magebane Shield, Weapon of Resurgence, Bolt Thrower, Crossbow of Swift Death, Club of Rot, Doom Hammer** (3D models, engravings, techniques, full-swing strikes, VFX, icons), plus **Brooch Pin** (folds into a brooch / unfolds into a dagger; Hidden in Plain Sight, Pinprick Lunge) and **The Hand of Providence** (Mage Hand lay on hands / holy strike, Lay on Hands on yourself). The Brooch Pin and Hand meshes are first versions.

### Replaced
- **Weapon skins raised to 512x512, Elsdocia to 1024x1024** (`tools/hires_weapon_textures.py`, called by `gen_weapon_models.py`; the 128 originals are kept in `tools/weapon_tex_128/`). Elsdocia's blade is repainted as faceted blue crystal with white-hot cores and bright fractures, the bronze section keeps its clover cut-out. The other skins are smoothed and sharpened with surface grain, not repainted: they are higher resolution, not new artwork.
- **Real-life sizes:** katanas about 1 m, longsword 1.25 m, greatsword 1.8 m, trident 1.9 m, spear 2.1 m, Elsdocia 2.3 m (`REAL` table in `gen_weapon_models.py`; 1 block = 1 m).

### Not done
- The older weapon meshes are still 40-170 flat quads; they are not sculpted high-poly models like the tensuranoindex jar's. The Brooch Pin does not animate its fold.

## [1.2.0] — Compiled: the 1.1.0 line builds, Elsdocia grand, boss spawn grace, tooltip wrapping

**State:** the merge of the three lines (the owner's 1.0.0 Marquis / Riven line, the 0.99 Elsdocia + Key restoration line, and the 0.58 line with every new grimoire, VFX, sounds and summons) compiles in CI and passes its tests. Nothing here was run in game.

### Fixed
- **Build:** the Codex UI imported `net.minecraft.client.renderer.Lighting`; in 1.21.1 it is `com.mojang.blaze3d.platform.Lighting` (this broke the 1.1.0 build). The Riven loot test now expects the 5-pool table the Story Page drop made.
- **Boss dies on spawn (Marquis / Riven Remake):** on top of the 0.100 damage guard, for the first 100 ticks `setHealth(<= 0)`, `kill()` and every damage source that is not a creative player, `/kill` or the void are ignored (and logged with the real source: look for `Marquis/Riven: ignored` in `latest.log`). Unverified in game.
- **Elsdocia description ran off the screen:** every weapon tooltip is now wrapped at word breaks (44 characters, continuation lines indented); Elsdocia gained its wiki lore line.
- **Release notes:** the CI release body is now the newest section of this file.

### Replaced
- **Elsdocia remade again (owner reference image + the Black Clover Movie scale):** the blade top is worked bronze-gold with the clover cut out of it, the rest is shattered-glass crystal in deep azure with bright cracks and white-hot cells down to a long pointed tip; a thin, wide cross guard with swept tips; a slender dark grip; gold pommel. **Twice the size in hand** (a greatsword). New 16x16 inventory sprite (`tools/gen_elsdocia_icon.py`); the earlier sprites stay as `elsdocia_v099.png`.

### Version gap check (1.0.0 -> 1.1.0 -> 1.2.0): see `docs/version_gap_1.2.md`

## [1.1.0] — Resumed from v1.0.0 (compiled in 1.2.0)

The tree is v1.0.0 (the Marquis / Riven line) with the 0.100-0.102 work ported onto it; the older line's own Riven implementation (`RivenPassives`, `riven_codex/`, story weapons) is superseded by v1.0.0's and is not restored. Details are in README.md under "1.1.0". In short: seven reference-sheet weapons, Elsdocia remade, the usable Bard Relic, Story Page and Gale Feather, the Codex UI, and the four Black Clover Spirit Lords with true bodies (Tensura spirits stay orbs).

## [0.102.0] — The Black Clover Spirit Lords get their bodies (NOT yet compiled)

**New (from "Spirit Lord model Ideas"):** `ElementalSpiritLordEntity` with four hand-built models: **Salamander** (winged fire dragon), **Undine** (woman of water with foam hair, wave dress, fin ears and orbiting drops), **Sylph** (green fairy with dragonfly wings and bubbles) and **Gnome** (hunched stone giant). Each has idle / move / cast / hit animations, a glow layer, a technique and an aura for its master (see `docs/spirit_lords.md`). Exclusive as in Black Clover: one mage per element (`SpiritSlots`), only that mage can call it, one body per element. Models by `tools/gen_spirit_lords.py` (+ `tools/spirit_geo.py`), previews in `tools/render_preview/scenes.d/spirit_lords.json`.
**Tensura-style spirits stay small floating orbs** that follow the player (`SpiritLordEntity`, unchanged); without a Black Clover bond the Spirit Charm now calls one (use: pick the element, sneak-use: call). Admin: `spirit_body <player> <kind>` calls a body.
**Also:** a Spirit Lords page in the Codex.
**Replacement:** the Spirit Charm no longer turns a bonded mage's lord into an orb; it calls the real body (the orb entity is untouched).
**Build note:** still not compiled (maven.neoforged.net blocked in the session); build with `gradlew.bat build` and send me any errors.

## [0.101.0] — Reference weapons, boss drops, the Codex (NOT yet compiled)

**New:**
- 7 weapons from the "Custom weapon i want added" sheets, each with a 3D in-hand model, glow layer, inventory icon, Tensura engravings, a right-click technique, a sneak-click second technique and a full-swing strike (logic in `item/ReferenceWeapons.java`): **Quickling's Ivy** (rapier: rooting poison ivy), **Magebane Longsword** (strips buffs, jams casters), **Prism Blade** (blinding flash, seven-colour beam), **Thundercleave** (thunder arc, thunderstorm, hits arc to a second foe), **Bloodbound Katana** (Blood Charges, Bloodflare), **Permafrost Sabre** and **Cyclone Rapier** (the Boreal Blades; hold both for Boreal Fusion).
- Boss drops: **Story Page** (Riven Remake; Rewrite: cleanse + heal 40%), **Gale Feather** (Sylph; Gale Leap), loot tables `entities/riven_remake.json` and `entities/wind_spirit_lord.json`.
- **The Codex** (`client/ui`): an open book with page-turn animation, mouse-wheel scrolling, tabs, and on the first spread your own figure in a random pose (standing, sitting, bowing, casting, floating, meditating, victory, salute, on guard) next to the magic this mod has given you. Pages are registered in `CodexPages`, so the book grows without touching the screen.

**Replacement:** the Multiverse Status screen is replaced by the Codex everywhere it opened (hotkey, Tensura-menu button, panel click). Its status blocks are the Codex's Status page; the old class stays only for the compact Tensura-menu panel. The **Black Bull Bard Relic** (an inert trophy) is now usable (sneak-use: Bardic Inspiration); same item id.

**Build note:** this session could not reach maven.neoforged.net, so none of 0.100/0.101 was compiled. Build with `gradlew.bat build` and send me any errors.

## [0.100.0] — Elsdocia remake + boss spawn guard (NOT yet compiled)

**Replacement:** Elsdocia's 3D model skin, glow layer and inventory sprite (the wiki/reference look: long straight crystal blade, deep azure with a cyan core and light motes, gold clover emblem above a gold guard, dark grip). The old sprite is kept as `textures/item/elsdocia_v099.png`.
**Fix (unverified in-game):** Riven Remake spawn guard. Damage and deaths in his first 60 ticks are blocked and logged (`[nusmp] Riven: ignored ...` in latest.log, which names the real killer), Tensura passive grants are wrapped in try/catch, and he is given a magicule pool like Zagred so Tensura skills cannot starve and hurt him.
**Build note:** this session could not reach maven.neoforged.net, so 0.100.0 was not compiled. Build it locally with `gradlew.bat build`.

## [0.99.0] — Elsdocia + Key Magic Restoration

**Release Date:** 2026-10-10

### Added

#### Elsdocia: the first Wizard King's legacy sword
- New God-tier weapon: **Elsdocia** (`nusmp:elsdocia`) — broad crystal-like blade with a clover emblem near the golden hilt (per the Black Clover wiki)
- Custom engraving: **Legacy of the Wizard Kings** (Tensura enchantment, level 1) — additive damage in main hand
- Standard Tensura engravings: Magic Interference 2, Magicule Absorption 2, Barrier Piercing 1
- 3D in-hand mesh (broad crystal blade, clover-crest plate on both faces, ornate gold hilt), 16×16 sprite stays the inventory icon
- Mechanics:
  - **Absorbs/stores/releases magic:** hitting a foe drains 2% of their magicule pool into the sword's charge (`ElsdociaCharge`, 0-1000)
  - **Legacy Release** (right-click): releases stored charge as a broad spatial rift; heals the wielder proportional to spent charge; boosted by Key Magic grimoire
  - **Legacy Gate** (sneak+right-click): requires a floating Key Magic grimoire + ≥100 stored charge; opens a 32-block spatial rift that drains 4% of each hit foe's magicules
  - **Key Magic synergy:** with a Key Magic grimoire floating, Legacy Release reaches 28 blocks (vs 22), hits for ×1.35 damage, and Legacy Gate becomes usable
  - Country-destroying lore scale is lore-only (tooltip); no terrain destruction
- Lore tooltips (per wiki): broad crystal-like blade / clover emblem; crafted by Lemiel Silvamillion Clover; used by Asta to defeat Conrad; contains a piece of the past Wizard Kings' souls
- EPIC rarity, Netherite tier, unbreakable legacy blade

#### Key Magic restoration
- Restored the physical **Magic Key** item (`nusmp:magic_key`) — uses the mini-key model's 64x64 texture as its item icon, dropped by Janus Baptism, consumed by Janus Abigail for extra gates
- Restored `KeyArts.janusBaptism` to drop a Magic Key on cast (was a tag-charge-only system)
- Restored `KeyArts.janusAbigail` to consume held Magic Keys (keys fill the door slots after the legacy charges, with four extra doors total) to extend to 10 doors
- Improved `KeyAura` to use the textured `great_key` model at 0.08 scale (was `mini_key` at 0.3) — the later approved improvement, not an exact recreation of `v0.82.0-b126`

### Fixed
- `WeaponEngravings` now parses fully-qualified enchantment ids (e.g. `nusmp:legacy_of_the_wizard_kings`); short ids still resolve to the `tensura` namespace
- Build fix: `BronzeAura.piece` used nonexistent `PartPose.x()/y()/z()` accessors — now uses the public `x/y/z` fields
- Build fix: `BronzeProps` used nonexistent `MagicPropEntity.hasLineOfSight` — now uses a block raycast between the prop and its target (same behavior)
- These Bronze build errors were pre-existing on the default branch (`760f10a`) and prevented any build before 0.99.0

## [0.98.0] — Marquis Remake Raid Boss

**Release Date:** 2026-10-09

### Added

#### Marquis Remake Boss (0.1.0-ALPHA)
- New raid boss: **Marquis Remake** — Minecraft-skin-body Black Bulls bard with god-summon mechanics and New Order reality rewrites
- Player-scale humanoid model (1.8 blocks, Steve hitbox) with Minecraft skin + geo coat layers (4 phases)
- Four-phase fight progression:
  - Phase 1 (Bard): Fa Jin charge + New Order + random grimoire
  - Phase 2 (Hack): 75% HP, coat fade-in, Gearshift + Compress + Evil Eye
  - Phase 3 (Rift): 60s timer, dimension pull to `nusmp:story_rift`, arena-scale mechanics
  - Phase 4 (Final Form): 25% HP, page-wings + cracked crown, The Creator god summon
- **300,000,000 EP** via Tensura API (non-plunderable, shown on appraisal)
- **New Order**: Reality-rewrite gaze mechanic (6-block range, 1.0s tell). Legal orders: +50% cast warmup, weapon speed halved, ground becomes solid, speed boost. Barks the rule. Anti-magic fizzles.
- **Fa Jin**: Melee/dash kinetic charge (white rings on crest). 5 stacks → 4-block cone punch with knockback. 10s timeout. Cannot hold through stagger.
- **Gearshift**: Speed rank (40% Low / 200% Top) on touch. Red-to-blue motion trail. Minimum Low. 4s duration. Ranks shown on boss bar.
- **Compress**: Marble-compresses blocks, constructs, or failed-tell players. Purple sphere in offhand for 3s. Player: removed from fight 3s, no damage, then released. Block: arena-only, restores at phase end.
- **Random Grimoire**: Draws one registered `nusmp.skill.grimoire_*` book each combat roll, cast via SpellRuntime. Named on boss bar. Anti-magic burns page.
- **Evil Eye**: God-scale, arena-scale. Both eyes, violet, 0.8s tell. Marks target with Existence Reduction: regen (ultra/totem/absorption/natural/mod/avatar) does nothing for 6s. Current HP does not auto-drop. Hits taken while marked cannot heal after mark ends. Works on players, high-EP Tensura bodies, and his own avatars. Phase 4 mark lasts 8s. Boss bar shows eye + subtitle "This page ends."
- **God Avatars** (10 total, one at a time, 12s life):
  - Zeus: One punch + Gearshift Top. 4-tick afterimage tell + 4-block hit. Survives one Island Fall.
  - Beerus: Hakai sphere (3-block radius, arena blocks + constructs only, blocks restore). Players take signature damage only.
  - Truth: Equivalent Exchange. Avatar damage mirrored to last player who hit Marquis (capped at phase signature).
  - Ultimate Madoka: One arrow. Removes one negative on him or one positive on marked player.
  - Arceus: Judgment. Next grimoire draw forced to element target is weakest to (via ThreatScan).
  - Grand Zeno: Erase. Deletes one construct or marble-compresses Evil Eye marked player (releases at anchor with mark intact).
  - Anti-Spiral: Three galaxy-shuriken projectiles (wide, slow tell, dimension-slash shader).
  - Lord of Nightmares: Sea of Chaos pool under gold ring (4s, pauses regen even without Evil Eye).
  - Kami Tenchi: Passive. Arena gravity stabilizes, no elytra, his i-frame window blocked. Dim gold outline, no attack.
  - The Creator: Phase 4 only, once. Law: "healing does not exist here" for 8s, whole rift. Stacks with Evil Eye. Then avatar closes.
- **Combat Type Rolls** (every 12s phase 2, every 8s phase 3): Caster, Hexblade, Legion, Rift, Song, Hack. Named on boss bar.
- **Damage Phases** (signature damage, not multiplier):
  - Phase 1: 22 base
  - Phase 2: 42 base
  - Phase 3: 62 base
  - Phase 4: 85 base
- **Null Types** (1.5s i-frame, phase change only):
  - Phase 2: Physical null 4s, then magic null 4s (alternating). Other damage type still hurts. Repeating nulled type = 0 damage.
  - Phase 3: Spatial null 4s on rift entry.
  - Phase 4: Rotating null 4s.
  - Anti-magic cancels i-frame window if not started.
  - The Creator adds 1.5s i-frame on entrance.
- **Health Scaling**: Base 1200 HP, +400 per extra player (cap 4).
- **Model & Animations**:
  - Phase 1: Minecraft skin only, grimoire closed on hip, eyes dim
  - Phase 2: Black coat + bull emblem fade on over sweatshirt, eyes go violet, grimoire opens and orbits
  - Phase 3: Lightning on coat, page shards, gold rift ring
  - Phase 4: Page-wings and cracked bull crown (same rig, no entity swap)
  - Animations: idle, walk, cast, gearshift dash, compress, evil-eye, summon, hit, phase change, death (0.4–1.4s, interrupt into hit)
- **VFX**:
  - New Order: white text rune at target's feet
  - Fa Jin: white rings on crest, cone flash on release
  - Gearshift: red-to-blue motion trail
  - Compress: purple marble with player's face shrunk on it
  - Evil Eye: full violet eyes, cone (anti_magic_slash tinted violet)
  - Avatars: gold page-body, feature as weapon (not second player model)
  - Fallback to arc_portal if shader fails
- **Loot Table** (`data/nusmp/loot_table/entities/marquis_remake.json`):
  - Guaranteed: Black Bull bard relic, story page trophy, skin-cape cosmetic (sweatshirt crest)
  - One of: Page Edge engraving, Bull Brand engraving, single-use rift key
  - 8% drop: Final Form crown (vanity)
  - No Tensura unique, no New Order item, no Evil Eye item
- **Boss Bar**:
  - Name: Marquis Remake
  - Subtitle swaps by phase
  - Combat type roll displays
  - Gearshift rank display
  - Eye indicator on Evil Eye mark
  - Hides outside arena leash

#### Arena & Visuals
- Story rift dimension (`nusmp:story_rift`, phase 3 mechanic)
- Character sheet visual lock (black high-collar coat, silver filigree, Black Bull skull, violet-blue eyes, floating grimoire)
- Minecraft skin body (brown hair, blue sweatshirt with white crest, gray pants, white shoes, player proportions)
- Coat and grimoire as geo layers that grow in, not replacement entity
- Phase progression shows visual escalation

#### Compatibility
- Tensura race/barrier/EP detection via ManasCore
- Black Clover grimoire book registry (`nusmp.skill.grimoire_*`) integration
- Anti-magic and NihilityZone: stagger Marquis, burn drawn page, force redraw
- ThreatScan integration for New Order order selection and Arceus judgment
- No Predator copy, no Usurper take-over, no Tensura unique skills

#### Configuration
- `config/nusmp-marquis.toml`
- Base health: 1200
- Per-player health: 400
- Avatar duration: 12s
- Evil Eye mark duration: 6s (phase 4: 8s)
- New Order duration: 8s
- Gearshift duration: 4s
- Compress marble duration: 3s

### Technical

- Package root: `com.newuniverse.nusmp`
- Entity: `MarquisRemakeBossEntity`
- Brain: `MarquisBrain` (threat-scan + god summon picker)
- Attacks: `MarquisAttacks` (New Order, Fa Jin, Gearshift, Compress, grimoire cast, Evil Eye, summon)
- Payload: `MarquisStatePayload` (phase, HP, cast ID, combat type, gearshift rank, evil eye mark)
- Renderer: `MarquisRenderer` (geo layers, coat fade, phase-specific textures)
- Boss bar: `MarquisBossBar` (animated overlay, phase subtitle, combat roll, eye indicator)
- Avatars: `GodsAvatarEntity` (10 variants, AI-driven, 12s life, breaks into pages)
- Asset structure:
  - `assets/nusmp/geo/entity/marquis_remake.geo.json` (4 phase variants on one rig)
  - `assets/nusmp/animations/entity/marquis_remake.animation.json` (idle, walk, cast, gearshift, compress, evil-eye, summon, hit, phase, death)
  - `assets/nusmp/textures/entity/marquis_remake_skin.png` (player skin)
  - `assets/nusmp/textures/entity/marquis_remake_coat.png` (coat layer, phases 2–4)
  - `assets/nusmp/textures/entity/marquis_remake_wings.png` (page-wings, phase 4)
  - `assets/nusmp/textures/entity/marquis_remake_crown.png` (cracked crown, phase 4)
  - `assets/nusmp/models/entity/gods/` (10 avatar models, gold page-body variants)
  - `data/nusmp/boss/marquis_remake.json` (phases, HP, drops, rift anchor)
  - `data/nusmp/loot_table/entities/marquis_remake.json` (drops)
  - `data/nusmp/dimension/story_rift_marquis.json` (arena dimension, phase 3)
- Lang keys in `assets/nusmp/lang/en_us.json`:
  - `entity.nusmp.marquis_remake` = Marquis Remake
  - `entity.nusmp.marquis_remake_spawn_egg` = Marquis Remake Spawn Egg
  - `boss.nusmp.marquis_order.*` = Order barks
  - `avatar.nusmp.gods.*` = Avatar names

### Dependencies

- NeoForge: [21.1,)
- Minecraft: 1.21.1
- Hard:
  - tensura [2.0.1.0,)
  - manascore_skill [4.0.0.2,)
  - manascore_storage [4.0.0.2,)
- Optional: bettercombat

### Testing & Quality Assurance

- New Order only uses legal orders (no delete/erase world/inventory commands)
- Fa Jin charge clears on stagger, cannot exceed 5 stacks
- Gearshift minimum is 40% speed (never 0), no negative speed
- Compress marble is visible, has 3s timer, no permanent locks
- Evil Eye stops only regen types (not direct damage), works on avatars
- Avatar summons one at a time, 12s life hard limit, breaks into pages
- The Creator fires once per phase 4, cannot be resummoned
- 300,000,000 EP shows correctly on appraisal, is not plunderable
- Dimension pull has tell + anchor release
- Anti-magic stagger correctly triggers page burn and redraw
- Zagred and dream dimension unchanged
- Dedicated server + client sync via `MarquisStatePayload`
- Boss bar UUID stable across all phases

### Known Issues & Limitations

- v1: Story rift dimension exists only during phase 3; players outside leash cannot enter
- god avatars do not load chunks outside rift (containment by design)
- The Creator law "healing does not exist" is rift-wide, not global (by design)
- Marquis + Zagred can share arena but do not have coupled AI (v2 feature)

### Future Enhancements

- Coupled Marquis + Zagred dual-boss mode
- Extended god summon pool (more anime franchises)
- Boss dialogue system (phase barks, order taunts, avatar summon callouts)
- Configurable arena themes for story rift (forest / castle / void)
- Kill replay system for New Order / Compress analysis

---

## [0.61.0] — Previous Release (Riven Remake)

See [RIVEN_REMAKE_RELEASE_NOTES.md](RIVEN_REMAKE_RELEASE_NOTES.md) for full details on Riven Remake (0.1.0-ALPHA).

---

## Versioning

This mod uses **semantic versioning**: `MAJOR.MINOR.PATCH`

- `0.98.0`: Marquis Remake raid boss added to nusmp 0.97.0 baseline
- Target `1.0.0` when both Riven and Marquis are production-ready

## Contributors

- **MarcusBlu**: Design, spec, and implementation
- **blueflamefilms09-collab**: Repository maintainer

---

**Last Updated:** 2026-10-09
