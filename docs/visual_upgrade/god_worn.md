# God grade worn relics: the visual upgrade (slice `god_worn`, release 1.102.0)

Owner request: "Upgrade God Grade weapons/relics to look 5x better." This slice is the 24 God grade relics that are WORN (back, belt, chest, feet, neck, hands, head). "5x better looking" cannot be measured
literally, so it is made measurable with proxies counted from the files the game reads, recorded before (`docs/visual_upgrade/god_worn_baseline.json`, committed first) and after
(`docs/visual_upgrade/god_worn_after.json`), and enforced by `GodWornUpgradeTest`. **The ratios below are numbers of quads, texels, layers and animation groups. They are not a claim about how it looks: nothing was run in a game.**

## How it is measured

`tools/visual_upgrade/god_worn/metrics.py` counts the same things before and after (the pieces the relic wears, from the catalog and the model files):

| proxy | what is counted |
|---|---|
| mesh | quads of every piece the relic wears at full detail (a mirrored copy on the other arm or leg is drawn again, so it counts again) |
| texture | painted texels: the opaque texels of the atlas cells the quads sample (old: 32x32 cells of the shared 128x256 atlas; new: 64x64 cells of the relic's own 512x512 skin), plus the 64x32 painted armour layer a chest relic keeps. Glow-map texels (non-black) are listed separately |
| glow passes | the draw passes beyond the lit solid pass that the relic's own models take part in: emissive model pass, glow-map pass (new), rim and shimmer (when it has shell quads) |
| ambient layers | effect layers the relic brings while merely worn: emissive model groups that move on their own (orbit, spin, pulse, bob), a catalog particle theme, and (new) every layer of its effect table |
| animation groups | animation groups of its pieces (swing, sway, wave, bob, spin, orbit, pulse) |
| act visuals | acts that have an effect of their own in the effect table (before: none) |

The grade plan of 1.29.0 (five motes, a rune ring, a halo band, the shimmer, the generic ambient particles, the head particle halo) is identical for every God relic before and after, so it is listed in the json (`shared_grade_plan`) and **not scored for either state**.

Score `V = 0.35 mesh + 0.20 texture + 0.15 glow + 0.20 ambient + 0.10 animation`, each term the count over a fixed reference: mesh 900 quads (the God floor for a worn piece set), texture 20480 texels (four times the median old painted texel count), glow 3 passes, ambient 3 layers, animation 3 groups. The weights and references were fixed before the models were built; the ratio `V_after / V_before` is what is reported.

## Result

| relic | quads before > after | painted texels | ambient layers | animation groups | glow passes | act visuals | LOD (quads, % of full) | held (vertices) | V before > after (ratio) |
|---|---|---|---|---|---|---|---|---|---|
| Flamme's Founder Cloak (back) | 392 > 2102 (x5.4) | 6144 > 49152 (x8.0) | 0 > 14 | 1 > 9 | 3 > 4 | 0 > 2 of 2 | 192 (9.1%) | 1952 | 0.40 > 2.73 (**x6.9**) |
| Mantle of the Overlord (back) | 404 > 2202 (x5.5) | 6144 > 57344 (x9.3) | 0 > 11 | 1 > 10 | 3 > 4 | 0 > 2 of 2 | 194 (8.8%) | 1968 | 0.40 > 2.68 (**x6.7**) |
| Pactbreaker Shroud (back) | 376 > 2104 (x5.6) | 6144 > 45056 (x7.3) | 0 > 14 | 1 > 20 | 3 > 4 | 0 > 2 of 2 | 196 (9.3%) | 2052 | 0.39 > 3.06 (**x7.8**) |
| Shadow Monarch's Mantle (back) | 385 > 2154 (x5.6) | 3072 > 36864 (x12.0) | 0 > 14 | 1 > 11 | 3 > 4 | 0 > 2 of 2 | 196 (9.1%) | 1904 | 0.36 > 2.70 (**x7.4**) |
| Shadow's Slime Cloak (back) | 340 > 1954 (x5.8) | 5120 > 36864 (x7.2) | 0 > 19 | 1 > 14 | 3 > 4 | 0 > 2 of 2 | 196 (10.0%) | 2096 | 0.37 > 3.05 (**x8.3**) |
| Silent Winter Mantle (back) | 561 > 2972 (x5.3) | 7168 > 40960 (x5.7) | 0 > 13 | 1 > 9 | 3 > 4 | 0 > 2 of 2 | 268 (9.0%) | 2104 | 0.47 > 2.92 (**x6.2**) |
| Six-Faced Dragon's Scale (back) | 485 > 2768 (x5.7) | 6144 > 40960 (x6.7) | 0 > 13 | 1 > 9 | 3 > 4 | 0 > 2 of 2 | 324 (11.7%) | 2392 | 0.43 > 2.84 (**x6.6**) |
| Urek Mazino's Chain Mantle (back) | 501 > 2728 (x5.5) | 6144 > 40960 (x6.7) | 0 > 11 | 1 > 15 | 3 > 4 | 0 > 2 of 2 | 236 (8.6%) | 2152 | 0.44 > 2.89 (**x6.6**) |
| Excalibur's Scabbard Avalon (belt) | 470 > 2704 (x5.8) | 7168 > 86016 (x12.0) | 5 > 31 | 6 > 32 | 3 > 4 | 0 > 2 of 2 | 252 (9.3%) | 2224 | 0.94 > 5.22 (**x5.6**) |
| Silvered Sash of Reflections (belt) | 394 > 2186 (x5.5) | 6144 > 45056 (x7.3) | 3 > 25 | 4 > 23 | 3 > 4 | 0 > 2 of 2 | 204 (9.3%) | 1912 | 0.70 > 3.92 (**x5.6**) |
| Berserker Armor Shard (chest) | 391 > 2110 (x5.4) | 4672 > 37440 (x8.0) | 1 > 11 | 1 > 6 | 3 > 4 | 0 > 2 of 2 | 226 (10.7%) | 2104 | 0.45 > 2.32 (**x5.2**) |
| Orsted's Dragon-God Armor (chest) | 418 > 2266 (x5.4) | 5696 > 45632 (x8.0) | 0 > 12 | 0 > 7 | 3 > 4 | 0 > 2 of 2 | 214 (9.4%) | 2472 | 0.37 > 2.56 (**x7.0**) |
| Greaves of the Wind Walker (feet) | 648 > 3660 (x5.7) | 5120 > 36864 (x7.2) | 1 > 18 | 2 > 16 | 3 > 4 | 0 > 8 of 8 | 354 (9.7%) | 2072 | 0.59 > 3.72 (**x6.3**) |
| White Whistle (neck) | 301 > 1646 (x5.5) | 4096 > 28672 (x7.0) | 0 > 22 | 0 > 13 | 1 > 4 | 0 > 2 of 2 | 145 (8.8%) | 2068 | 0.21 > 3.02 (**x14.6**) |
| Hand of Vecna (hands) | 328 > 1740 (x5.3) | 3072 > 49152 (x16.0) | 0 > 12 | 0 > 3 | 3 > 4 | 0 > 5 of 5 | 135 (7.8%) | 2012 | 0.31 > 2.26 (**x7.3**) |
| Aerie Courier Gloves (hands) | 336 > 1832 (x5.5) | 5120 > 32768 (x6.4) | 0 > 13 | 0 > 10 | 3 > 4 | 0 > 2 of 2 | 214 (11.7%) | 2144 | 0.33 > 2.43 (**x7.4**) |
| Bloodbound Gauntlets (hands) | 610 > 3408 (x5.6) | 4096 > 45056 (x11.0) | 0 > 11 | 0 > 10 | 3 > 4 | 0 > 2 of 2 | 234 (6.9%) | 2192 | 0.43 > 3.03 (**x7.1**) |
| Incinerator Arm (hands) | 496 > 2642 (x5.3) | 5120 > 36864 (x7.2) | 0 > 12 | 0 > 3 | 3 > 4 | 0 > 2 of 2 | 136 (5.1%) | 2196 | 0.39 > 2.49 (**x6.3**) |
| Eye of Vecna (head) | 307 > 1640 (x5.3) | 7168 > 40960 (x5.7) | 0 > 16 | 1 > 10 | 3 > 4 | 0 > 5 of 5 | 138 (8.4%) | 2008 | 0.37 > 2.64 (**x7.1**) |
| Basilica Halo (head) | 304 > 1748 (x5.8) | 4096 > 36864 (x9.0) | 0 > 18 | 0 > 11 | 1 > 4 | 0 > 2 of 2 | 176 (10.1%) | 2008 | 0.21 > 2.81 (**x13.5**) |
| Godking Crown (head) | 624 > 3468 (x5.6) | 6144 > 36864 (x6.0) | 2 > 14 | 2 > 6 | 1 > 4 | 0 > 2 of 2 | 336 (9.7%) | 2008 | 0.55 > 3.04 (**x5.5**) |
| Limitless Blindfold (head) | 555 > 3068 (x5.5) | 4096 > 36864 (x9.0) | 0 > 28 | 1 > 22 | 3 > 4 | 0 > 2 of 2 | 312 (10.2%) | 2000 | 0.44 > 4.35 (**x9.9**) |
| Odin's All-Seeing Eye (head) | 388 > 2150 (x5.5) | 5120 > 49152 (x9.6) | 0 > 12 | 0 > 6 | 1 > 4 | 0 > 2 of 2 | 184 (8.6%) | 1920 | 0.25 > 2.52 (**x10.0**) |
| Ra's Dawn Crown (head) | 382 > 2133 (x5.6) | 5120 > 40960 (x8.0) | 0 > 21 | 0 > 15 | 1 > 4 | 0 > 2 of 2 | 206 (9.7%) | 2020 | 0.25 > 3.33 (**x13.4**) |

Totals: quads 10396 > 57385, painted texels 128128 > 1033344, ambient layers 12 > 385, animation groups 25 > 290, V 10.0 > 72.5. Lowest V ratio: x5.2, highest x14.6. The checks of the God brief (quads >= max(900, 5x), texels >= 4x, a separate glow map, >= 3 ambient layers, an idle animation, an effect for every act, LOD <= 15%, held form <= 2500 vertices, V >= 5x) all pass for all 24 relics (`god_worn_after.json`, `checks`).

## What was built, relic by relic

* **Flamme's Founder Cloak** (`flammes_founder_cloak`, back): A white and gold founder's cloak: a damask drape cut into four swinging bands with a scalloped hem, a standing star-pointed collar, starburst epaulettes in gilt filigree, the staff-and-star crest on the back, rune tablets and a ring of starlight orbiting the wearer.
* **Mantle of the Overlord** (`mantle_of_the_overlord`, back): A black velvet mantle 'with more folds than cloth': a long train, two shorter capelets over it, a fan collar with crimson spikes, a skull clasp, spiked pauldrons and red veins that glow in the folds.
* **Pactbreaker Shroud** (`pactbreaker_shroud`, back): A torn violet-black shroud: ragged saw-tooth hem, a bunched hood, fluttering shroud tails, snapped chains at the shoulders, a cracked seal on the back and pages of the broken contract orbiting.
* **Shadow Monarch's Mantle** (`shadow_monarchs_mantle`, back): A jagged shadow mantle: crystal blades fanned up from both shoulders, a crown of shadow flames behind the head, a lit crown sigil, violet veins and dark shards circling.
* **Shadow's Slime Cloak** (`shadows_slime_cloak`, back): A living violet slime: glossy blob-hemmed drape, dripping beads, tendrils over the shoulders ending in blobs, a glowing nucleus, a translucent slime sword at the hip and bobbing blob companions.
* **Silent Winter Mantle** (`silent_winter_mantle`, back): Woven hoarfrost: frost-fur ruff, a ring of ice crystals round the neck, icicle-hung cowls, frost lace, a snowflake crest, snow shards orbiting.
* **Six-Faced Dragon's Scale** (`six_faced_dragons_scale`, back): One scale of a dragon-god: scale-covered shoulders and back, six small horned dragon heads, a golden spine ridge, ember glow and orbiting embers.
* **Urek Mazino's Chain Mantle** (`urek_mazinos_chain_mantle`, back): Heavy iron and chains: iron lamed pauldrons, chain swags across chest, back and shoulders, great shackles, a curtain of hanging chains that each swing, a tower crest.
* **Excalibur's Scabbard Avalon** (`excaliburs_scabbard_avalon`, belt): A green-and-gold belt with plates and a knot buckle, a long lofted scabbard at the hip with enamel panel, gold bands, filigree vine and chape, leaf charms, fairy lights and rings of Avalon light.
* **Silvered Sash of Reflections** (`silvered_sash_of_reflections`, belt): A bandolier of hexagonal mirror plates strung on chain across chest and back, a mirrored waist band, tails of glass links ending in shards, glints that wander over the glass.
* **Berserker Armor Shard** (`berserker_armor_shard`, chest): A cracked rust-black cuirass with a spiked gorget, a fan of blood-lit shards across the chest, spine spikes, tassets, hanging chains and large horned pauldrons.
* **Orsted's Dragon-God Armor** (`orsteds_dragon_god_armor`, chest): Layered dragon-god plate: gorget with horns, a dragon relief on the breastplate, scale lames, tassets, a short cape, wing flanges behind the shoulders, curse seals orbiting.
* **Greaves of the Wind Walker** (`greaves_of_the_wind_walker`, feet): Plated greaves with a gilt knee cop, a wind sigil, a cursed cracked band, three wings of feathers (ankle, calf, heel), spinning wind rings and streamers.
* **White Whistle** (`white_whistle`, neck): A bone-white abyss whistle on a braided cord with beads, gold bands, engraved mark and filigree, sound rings that swell outward and drifting notes.
* **Hand of Vecna** (`hand_of_vecna`, hands): Right arm: an ornate gold armguard with a ruby eye plate and filigree. Left arm: the withered grey hand in a gold cuff with runes up the forearm, cold mist and a turning red circle below it.
* **Aerie Courier Gloves** (`aerie_courier_gloves`, hands): Long flared riding gloves with stitched fingers, a gem on the back of the hand, a courier badge, wing fans at the wrist, wind streamers and a spinning wind ring.
* **Bloodbound Gauntlets** (`bloodbound_gauntlets`, hands): Riveted iron gauntlets: plated fists with knuckle spikes, an elbow spike, blood vials, lit veins, dripping tassels, blood mist.
* **Incinerator Arm** (`incinerator_arm`, hands): Right arm: a black mechanical cannon arm with vents lit from inside, exhaust pipes, coils, a furnace core and a long muzzle with a turning ring. Left arm: a servo bracer with gauges and a lit coil.
* **Eye of Vecna** (`eye_of_vecna`, head): A red cord round the brow with a brass spiked plate and the pink-irised eye, lids, optic nerves streaming back, five small eyes circling the head.
* **Basilica Halo** (`basilica_halo`, head): A floating double halo with a rune band, sixteen rays, eight glass-paned pillars to a circlet, a cross emblem, temple wings and shafts of light.
* **Godking Crown** (`godking_crown`, head): A band with nine spires, arches and a crowning orb, wings of flight at the temples, emerald veins, a rune ring and skill orbs circling.
* **Limitless Blindfold** (`limitless_blindfold`, head): A black blindfold with six lit eyes and long tails, three tilted infinity rings, six eyes circling and the blue and red orbs of Hollow Purple.
* **Odin's All-Seeing Eye** (`odins_all_seeing_eye`, head): A gold rune band with the pale eye in its lidded socket, two ravens perched at the temples, raven wings, a floating spear and rune tablets.
* **Ra's Dawn Crown** (`ras_dawn_crown`, head): A gold ray crown with a sun-and-ankh plate, a cobra at the brow, a sun disc behind the head with rotating rays, wings, dawn shafts and sun orbs.

Every relic has: its own full model (2 to 4 files), a LOD model, a held model, a 512x512 skin with its own decals (crest, panel, border, trim, two rune bands, filigree, surface weave) and an emissive glow map, an effect table of 9 to 28 ambient layers and a visual for each of its acts (an effect shape of the VFX system plus a particle burst in its colours).

## How it works

* **Generators** (`tools/visual_upgrade/god_worn/`, python3, deterministic): `kit.py` (geometry vocabulary: cloth bands that swing, chain links, scale fields, feather fans, filigree, shards, wisps), `b_back.py`, `b_back2.py`, `b_body.py`, `b_hands.py`, `b_head.py` (one function per relic, written once for three detail levels), `skin.py` (the 512x512 skin and glow map), `fx.py` (effect tables), `gw_lib.py` (the writer), `build.py` (fits the detail scale per relic so the quad counts land on their targets, writes everything, caches the scales in `scales.json`), `metrics.py`, `baseline.py`, `after.py`, `preview.py`, `doc.py`, `caps.py` (the cap table).
* **Model files** `assets/nusmp/relic_worn/gw_<id>_*.txt`: the same quad format as before plus a `skin <id>` record and two quad flags: `LIT` (64: the quad also glows where its glow map says so, in the glow colour) and `ADD` (128: additive pass only, never in the lit pass). Old files do not use them. Bits 8, 16 and 32 are the Charms / Genesis flags `GLOWMAP`, `STAR` and `HALO` (1.106.0 merge: every flag has its own bit).
* **Skins** `assets/nusmp/textures/relic_worn/god/<id>.png` and `<id>_glow.png`: 8x8 cells of 64 texels; the material library is height-field lit (bevels, ambient occlusion, wear), tileable on a 48 texel period with a gutter; the albedo is grey and tinted by the relic's palette slots like the old atlas, the glow map is drawn additive in the glow colour.
* **Catalog** `assets/nusmp/visual_upgrade/catalog.txt`: `up <id> <pieces>` (worn), `lod <id> <pieces>`, `held <id> <pieces>`. The old `relic_worn/catalog.txt` is untouched, so the flag fallback is just not reading the new catalog.
* **Effect tables** `assets/nusmp/visual_upgrade/<id>.json`: ambient layers (kinds motes, ring, orbit, line, trail, ground) and act visuals. Read by `relics/upgrade/UpgradeFxData` (pure, tested), played by `client/relics/fx/UpgradeFxClient` (ambient) and `relics/upgrade/UpgradeActFx` (acts).
* **Flag** `visual.upgrade` in `config/nusmp-visual-charms-client.toml` (default true; since the 1.106.0 merge the one switch of both upgrade slices, read through `RelicFxOptions.upgrade()`). Off: every relic returns to the 1.101 models, nothing new is drawn or spawned. Takes effect after a resource reload (F3+T) or relog.

## Performance guard (the 1.99.0 pass is not undone)

Cap table (`caps.py`, `relics/upgrade/UpgradeCaps.java`, append-only; the old 2600 / 2600 caps of `gen_worn_all.py` are unchanged and still apply to every other relic):

| grade | model file | everything one relic wears | held form | LOD | LOD distance | additive passes stop |
|---|---|---|---|---|---|---|
| (other relics) | 2600 | 2600 | n/a | n/a | n/a | n/a |
| GOD (this slice) | 4800 vertices | 16000 vertices | 2500 vertices | <= 15% of the full vertices | 12 blocks | 24 blocks |

* Beyond 12 blocks and at `relicfx.quality = low` an upgraded relic is drawn as its LOD model (`WornLibDraw`); beyond 24 blocks its glow pass, glow map and shell pass are skipped.
* An upgraded relic counts at most 2600 vertices against the effect budget of its wearer (`WornBudget`), so its big model does not starve the grade effects of the wearer's other relics; the models themselves are always drawn, as before.
* Ambient layers go through the client particle budget (`PerfBudgets.clientParticles`), fall with distance (full to 12 blocks, none from 24) and with `PerfSettings.vfxQuality()` (30% / 60% / 100%), at most eight wearers a tick, only those in front of the camera, nothing for spectators, invisible wearers, hidden Curios slots or hidden armour relics. Act effects go through `VfxSpawn` (the effect packet budget) and `PerfParticles`.
* No per-frame allocation was added: skins are cached per name, the layer options are resolved once, the tick writes into two scratch arrays.

## NEW / REPLACED / KEPT

| | what |
|---|---|
| NEW | the `gw_*` worn, LOD and held models, the 24 skins and glow maps, the effect tables and catalog, the `skin` record and the `LIT` / `ADD` flags, `UpgradeCatalog`, `UpgradeFxData`, `UpgradeFxMath`, `UpgradeFxClient`, `UpgradeActFx`, `UpgradeCaps`, the flag `visual.upgrade`, the self-test `visual.god_worn`, the tests, the generators and previews |
| REPLACED | the worn look and the held look of the 24 relics (while the flag is on): they were the `uq_<id>` ornament on a shared look model, or a plain look; the hand held form of Hand of Vecna and Eye of Vecna (were signature hand models) |
| KEPT | every old model file (`uq_*`, `cloak_long`, `crown_godking`, `greaves_windwalker`, `hand_of_vecna`, `eye_of_vecna_hand` ...), the shared atlas, `relic_worn/catalog.txt` and `relic_hand/catalog.txt` (not edited), the item ids, stats, acts, passives, the sprites as inventory icons, the painted armour layer of the two chest relics, the Time Magic effects (not touched) |

## Edits to existing files (all additions or one-liners)

* `client/relics/worn/lib/WornMesh.java`: flags `LIT`, `ADD`, the `skin` field and record, `emitQuads` counts the new flags; `WornModels.java`: `upgrade()` loader; `WornLibrary.java`: `Ensemble` gets `upgraded`, `lodPieces`, `lodVerts`, the cache key knows the flag; `WornLibDraw.java`: per-skin buffers, LOD choice, the additive cut-off, the new flags in the solid and glow passes, budget base; `client/relics/hand/HandForms.java`: held pieces of an upgraded relic; `client/relics/fx/RelicFxOptions.java`: `visual.upgrade`.
* one-liners: `relics/RelicEngine.java` (after an act ran: `UpgradeActFx.after`), `relics/RelicSystem.java` (registers `visual.god_worn`), `client/relics/worn/lib/WornLibClient.java` (starts `UpgradeFxClient`).
* `tools/relics/gen_worn_all.py`: the hook at the end of `main()` (4 lines: runs `tools/visual_upgrade/god_worn/build.py`, which skips itself when up to date).

## Tests and self-test

* `GodWornUpgradeTest` (catalog, caps and LOD and held budgets read from the real model files with the game's parser, skins and glow maps, effect tables against the acts in the wave sources, the ratio file against the targets, the old files and the fallback, the new flags), `UpgradeFxMathTest` (parsing, rates, points, orientation). Existing asset tests keep passing: `WornMeshTest`, `HandCatalogTest`, `WornCoverageTest`, `RelicRenderCoverageTest`, `RelicUniqueLooksTest`, `RelicHandCoverageTest`, `ArmorLooksTest`, `WornMeshClearanceTest` (run in the JUnit shim).
* CI server smoke self-test `visual.god_worn` (`UpgradeSelfTests`): every relic exists as a God relic, its models parse and keep the caps, its skin, glow map and effect table are in the jar, and the 1.101 look still resolves with the flag off.

## Previews (`docs/previews/visual_upgrade/god_worn/`)

`<id>_up.png`: BEFORE (old models, shared atlas) above AFTER (own skin) in three views, and the glow pass alone. `<id>_detail.png`: a close-up of the detail, the LOD form, the held form, the side view. Made by `python3 -B tools/visual_upgrade/god_worn/preview.py`, a software rasteriser with the game's rules for these files (orthographic, one light, the 0.85 body scale, emissive additive, animation groups at rest, no clearance push-out). Honest reading: shapes, palettes and the glow layout read as intended; the real lighting, the animation, the Curios placement and the particles are not shown.

## What could not be verified

* Nothing was run in a game: no draw order or winding check in the real renderer (the winding and the y flip follow `gen_worn_lib.write_model` exactly), no look of the skins under real light, no feel of the swing bands and wave groups, no particle density, no act effect timing, no frame cost. The vertex numbers are counted; the frame time is not measured. At full detail near the camera a God relic can cost up to 16000 vertices (x2 with the glow pass for the lit parts) against 2600 before; the LOD, the low quality rule and the budget base are the guards, and `/nusmp perf report` shows the section `c.relic.worn_layer`.
* The Curios eye icon, the armour-relic hide option and the first-person rules are the 1.99.0 ones and were not changed; they were not re-run.
* The act effect shapes are existing `VfxShape` values chosen by name; how each looks with these colours has not been seen.
* The type check ran against the API stubs (known stub artefacts only); the JUnit tests ran in the shim, which cannot load Minecraft classes, so `UpgradeSelfTests`, `UpgradeActFx`, `UpgradeFxClient` and the draw changes are type-checked, not executed.

## Not done / cut

* The inventory icon stays the old 16x16 sprite (owner rule 7); a 3D tilted model in the GUI slot was **not** built: the relic items are `builtin/entity` renderers that draw the sprite in the GUI by design (`HandItemRenderer`), and changing that would replace the icon the owner wants kept.
* `ArmorPainter` (the painted 64x32 armour layer of the two chest relics) was not changed; the 3D body pieces carry the new detail. Head and feet relics already leave their armour box clear.
* Swing trails do not apply to worn relics. A looped ambient sound id is a Genesis item of the brief, not a God one. No sound was added.

## Regenerating

```
python3 -B tools/visual_upgrade/god_worn/baseline.py     # only on the old state; the committed baseline is the record
python3 -B tools/visual_upgrade/god_worn/build.py --force  # models, skins, effect tables, catalog (or python3 tools/relics/gen_worn_all.py, which calls it)
python3 -B tools/visual_upgrade/god_worn/after.py        # god_worn_after.json and the ratio table
python3 -B tools/visual_upgrade/god_worn/preview.py      # docs/previews/visual_upgrade/god_worn
python3 -B tools/visual_upgrade/god_worn/doc.py          # this file
```

