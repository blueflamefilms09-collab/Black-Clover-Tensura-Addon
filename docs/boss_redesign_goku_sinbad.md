# Goku and Sinbad bosses: redesign (looks, forms, arts, bars, voice)

Owner request: the Goku and Sinbad bosses looked ugly and not in the style of the new player-model rework; Sinbad did not actually switch into his djinn forms (he should show the same Djinn Equip animations a player does, with better magic and effects); show a showcase before anything goes in the game. Later rules from the owner: a bar of their own for each boss, their own voice lines, abilities that do not copy what the mod already has, and abilities that really work.

**Showcase first:** `docs/boss_showcase/README.md` lists every image and says what it shows and what it does not.

Scope note: this change does **not** touch the spawn, despawn, leash, death or dimension code (another worker owns that). It owns looks, forms, animations, effects, bars, voice and the showcase.

## 1. NEW, REPLACED, KEPT (rule 5)

| | What |
|---|---|
| **NEW** | 16 looks in the new player-model style (`looks_gs.json`, `pmx_gs_*` geo/textures/animations, `GsLooks`, `GsBodies`, `GsAuraLayer`). Goku: base, SSJ, SSJ2, SSJ3, God, Blue, UI sign, MUI; the ten ladder steps map onto them (Blue Kaio-ken uses Blue, True UI uses Perfected UI). Sinbad: base and seven djinn bodies. |
| **NEW** | `DjinnBossLayer`: the boss adapter of the player's own Djinn Equip drawing (`DjinnArmorLayer.draw` is now static and shared; `DjinnAmbient` and `DjinnRoster36Fx` accept any `LivingEntity`). |
| **NEW** | Three Goku arts: Zanzoken Cage, Solar Flare, Kienzan Boomerang (`arc/goku/GokuArts`). Two Sinbad arts: Rukh Orrery, Kingdom Sigil (`SinbadMoves`, `SinbadTable` specs appended). Helpers `arc/BossShapes` (pure geometry), `arc/BossFx` (budgeted particle sender), `arc/ArtTestFoe` (test wolf). |
| **NEW** | Two boss bars: `dragon_ball` (Goku) and `magi_djinn` (Sinbad): frames, emblems (`dragon_ball`, `vessel_star`), motifs, style json, resolver mappings, phase and form names (`BossBarSources`). |
| **NEW** | Own voice lines for each boss: `tools/audio/goku_lines.json` (52 lines) and `sinbad_lines.json` (65 lines), `arc/lines/*`, subtitle keys. |
| **NEW** | Self-tests `goku.boss.arts` and `sinbad.boss.arts`, pure tests `BossLinesTest`, `BossLineGateTest`, and the showcase. |
| **REPLACED** | The look of both bosses (their earlier geo-model look). Behind the client setting `bossLooks.bossLookClassic` (default false); set it to true and the earlier look is drawn exactly as before. The old geo, textures and registrations are all still in the jar. |
| **REPLACED** | Sinbad's form switch: the equip state existed on the server only, so the client never changed. Now a synced number (`SinbadBoss.EQUIP_IDX`) drives the client. |
| **REPLACED** | The few hard-coded lines of the two bosses (opening, death, form changes, Kaio-ken, Spirit Bomb, equip call, foresight, Seven Seas, Inqerad). Each now comes from the boss's own set; the old text is kept as the first line of its group or as the fallback when a group has no lines. The one-liner "Everyone... lend me your energy!" in `GokuMoves.bomb` was removed because the same line is now said when the cast starts (it is `goku_spirit_bomb_1`). |
| **KEPT** | Every old move and its numbers; every old bar style, frame and emblem (old PNGs unchanged, emblem cells 36 and 37 appended); the shared `boss_<family>_<event>` voice lines; all enum entries (only appended); the data component ids; the Time Magic VFX (untouched). |

### 1b. The models are looks on the existing player-model kit (owner order)

Nothing custom was built for the bodies. The 16 looks are made by `tools/bc_playermodels/gen_gs.py` from `gsbosses.py` with the kit's own libraries (`bclib`, `bcparts`, `bcparts3/4`, `bcskin`, `dvlib`, `dvskin`, `dvtable`, `hd_detail`: painted anime face, layered cube hair, extras and glow parts), registered as `PlayerLook`s through `PlayerModelRegistry` and `PlayerLookTables`, drawn by `PlayerModelRenderer` with a `PlayerBody` (`GsBodies`, like `DevilLooks`/`DungeonBodies`). Output is named `pmx_gs_<id>` (main, `_fine`, `_fx`) like `pmx_bc_<id>`. There is no separate mesh route and no Blender body. The only extra draws are the player's own Djinn Equip armour pieces (the existing `DjinnArmorLayer` meshes) and Goku's form aura (`GsAuraLayer`, the mod's existing fresnel aura render type applied to the same posed model). The older geo-model look is kept only behind the client switch `bossLooks.bossLookClassic`. The showcase renders come from `tools/bc_playermodels/preview_gs.py`, which imports the kit's `preview_bc.py` renderer, so they are drawn exactly like `docs/bc_playermodels_previews/`. Honest quality note: the Black Clover bosses carry crowns, halos and capes in their canon; Goku and Sinbad carry hair, face, outfit, markings, glow and (Sinbad) the player's armour, so they read simpler, and the djinn bodies in the look sheet are shown WITHOUT their armour (the armour is in the separate `sinbad_equip_*` renders).

## 2. Sinbad did not switch into djinn forms: cause and fix

* **Cause:** `SinbadBoss.equip` was a plain server field. The client never learned which djinn he wore, and the player-style Djinn Equip drawing only runs from client state, so Sinbad kept one look in every phase.
* **Fix, server:** a synced int `EQUIP_IDX` (`0` = none, `1..7` = Baal, Valefor, Zepar, Focalor, Vepar, Crocell, Furfur), written by `syncEquip()` whenever the equip changes and reset to 0 when the fight ends.
* **Fix, client:** `DjinnBossLayer` reads that number and plays the same timeline a player's Djinn Equip plays (phases 1 to 4: sigil, armour forging in piece by piece, full armour, and the reverse on removal) through the same `DjinnArmorLayer.draw`, `DjinnAmbient` and `DjinnRoster36Fx`. So he looks like a player doing Djinn Equip, and the body under it (hair, eyes, robe colours) is the matching `gs_sinbad_<djinn>` look.
* **Better than the player:** the bar names the djinn he wears ("Djinn Equip: Baal"), his two new arts are built from the seven djinn colours, and each djinn move got a rider (see section 3).

## 3. The arts (original shapes; none repeats a move the mod already has)

Each has a telegraph, a counterplay rule, a particle theme of its own (Goku: warm ki gold, flash white, Kienzan blue; Sinbad: the seven djinn colours and Rukh white-gold) and a bounded cost. Numbers are in the Java constants and are read by `tools/bc_playermodels/preview_arts.py` for the stills.

| Art | How it reads | Counterplay |
|---|---|---|
| **Zanzoken Cage** (Goku, 40 tick wind-up) | Four afterimage pillars grow round the target on a ring of radius 5.5; he streaks through all four (hit radius 2.8, 7 ticks apart) and then slams the centre (radius 4.2). The centre is locked at the start. | Leave the cage before the first blink. |
| **Solar Flare** (Goku, 30 ticks) | Three warning rings (4, 8, 12 blocks), then a flash out to 28 blocks. Only foes who face him with a clear line are blinded. | Look away or break the line of sight. |
| **Kienzan Boomerang** (Goku, 24 ticks) | A spinning disc flies out 22 blocks in 14 ticks (fast, slowing at the far end) and returns along the same line; hit radius 1.9, one hit per pass. | Step off the line; it never homes. |
| **Rukh Orrery** (Sinbad, 44 ticks) | Seven djinn lights orbit above him; seven spots are locked round the target (spread 3.2) and the lights land one by one, 4 ticks apart, each with its djinn's colour and rider effect. | Keep moving; the spots are fixed when the cast starts. |
| **Kingdom Sigil** (Sinbad, 56 ticks) | A circle of radius 11 with six sectors; every second one is lit. The lit ones erupt three times; the dark ones are safe. The pattern and the turn are locked at the start. | Read the pattern, stand in a dark sector. |

Fixes to existing abilities: Sinbad's Djinn Equip (the move that picks a djinn) now actually changes his visible form, which was the broken part. No other existing move was changed. The two `*.boss.arts` self-tests fire each new art for real on the server against a tamed-wolf test foe and assert hits (a lit sector hits, a dark one does not; seven motes land; the cage forgets its centre).

## 4. Boss bars

* Goku: `dragon_ball`. A calm, tight palette: dark brown frame, orange and gold trim, one red accent (the stars); blue only for the cast bar and the shield. Seven Dragon Balls (one to seven stars) evenly spaced along the lower edge, the four-star ball as the one emblem, a smooth orange fill with the kit's subtle shimmer scroll. His ten forms are shown as seven phases with six notches at the health lines of the form ladder (the bar system allows at most nine phases and six notches); the phase name is the form name.
* Sinbad: `magi_djinn`. Navy frame with gold trim and a teal fill (navy, gold, teal; nothing else), seven vessel gems in the djinn colours on a thin gold chain along the lower edge, the seven-point vessel star as the one emblem, the same subtle shimmer. While he wears a djinn the phase name reads "Djinn Equip: <djinn>". A first draft had a flame crest, a crown and a flock of birds; the previews looked cluttered, so they were taken out and the two calm motifs `dragon_balls_clean` and `magi_chain_clean` were added (the first versions stay in the generator, unused).
* Client look only (rule 9). Appended ids only (`BarStyleIds.ALL`, `BarStyle.EMBLEMS`), old bars untouched. The fill colour is the style's, not per form.

## 5. Voice lines

* Each boss has **its own** set, not the shared boss lines. Groups: fight intro, every transformation (Goku) or djinn equip call (Sinbad), each signature attack call-out, taking a heavy hit, low health, dodge (Goku), foresight and unequip (Sinbad), defeat, victory (players lost), a player falls, idle taunts.
* Files: `tools/audio/goku_lines.json` and `tools/audio/sinbad_lines.json`. Each line has `id`, `group`, `text`, `delivery` (a note for the voice tool) and `source`. Source is one of: `catchphrase` (a technique name or phrase the character is known for: Kamehameha, Solar Flare, Kaio-ken, Bararaq, Foraz...), `in the character's manner` (written for the fight in his way of speaking), or `kept from the old boss` (the text he said before).
* **Sources, honestly:** the lead asked for English wiki first, Japanese wiki as fallback. `dragonball.fandom.com` and `magi.fandom.com` could not be fetched in this session (the network refused the hosts: ENOTFOUND, outside the allowed domains), and the Japanese fallback was not attempted after that. So **no line is claimed as verbatim from an episode or chapter**. The technique and spell names come from the repo's own lore (`docs/sinbad_boss_lore.md`, written earlier from the English Magi wiki) and from well-known catchphrases; every other line is written in the character's manner. Episode and chapter numbers could not be added.
* **Wiring (server only):** `BossSpeech` sends the chat text of the boss (the subtitle of a line with no recording) and, only when the line has a voice file (`BossLines.isRegistered`), also the sound to players within 48 blocks (`Voice.sayNear`), which shows the same words as its subtitle. `BossLineGate` keeps a rest per group, a gap of 40 ticks between two non-priority lines of one boss, and remembers at most 16 bosses.
* **Audio is not generated.** The coordinator makes it with the offline Kokoro tool (`tools/audio/offline_voice.py`, profiles `goku` and `sinbad`); put the files at `src/main/resources/assets/anime_multiverse/sounds/voice/<boss>/<id>.ogg` and run `python3 tools/audio/boss_lines_gen.py --register`, which rewrites the registered list in `BossLines.java` and appends the `sounds.json` events. Until then every line is chat text only and `BossLinesTest` checks that "registered" equals "has a file".
* Subtitle keys `subtitles.anime_multiverse.voice.<id>` are already appended to `en_us.json` (fragment: `tools/lang/boss_lines_gs.json`).
* ElevenLabs is not used.

## 6. Cost (rule 11), worst case

| Part | Server | Network | Client |
|---|---|---|---|
| Zanzoken Cage | tell: at most 36 points per 3 ticks over 40 ticks; 4 blinks and 1 slam, each a bounded foe query | the budgeted particle packets to players nearby only | none beyond particles |
| Solar Flare | 1 foe query, 3 rings of 32 points | same | none |
| Kienzan Boomerang | 28 steps, at most 12 points each, 14 foe queries over the flight | same | none |
| Rukh Orrery | 7 foe queries and 7 scheduled closures (cleared with the boss) | same | none |
| Kingdom Sigil | 1 disc query, 3 eruptions of 24 points and a column | same | none |
| Every point list | capped at 32 points by `BossShapes.MAX_POINTS`; sent through `BossFx` and `PerfBudgets.particles` | | |
| Cage centre memory | at most 8 entries (per boss UUID), removed when it fires | | |
| Voice | one gate lookup per call; at most one non-priority line per 40 ticks per boss | one packet per player within 48 blocks per registered line (about 1 per 2 s per boss at the very most); chat text otherwise | subtitle only |
| Djinn sync | one int in the entity data, sent only on change | a few bytes | none |
| Looks | none | none | 16 models built once; `DjinnBossLayer` reuses the player's armour drawing (the same quad budget as one player in Djinn Equip), hair and fx meshes are the layered cubes already used by the player-model kit |

Nothing spawns entities or scans the world per tick. All state is per boss UUID and dropped on removal and fight end.

## 7. Tests

* New, pure (run locally with the JUnit console jar): `BossLinesTest` (manifest equals the Java data, every moment has lines, every call-out names a real group and move, every line has a subtitle, registered equals the files, both bosses call `BossSpeech`, no client class), `BossLineGateTest` (rest, gap, rotation, bounded memory).
* Extended: `SinbadTableTest` (19 specs, order), `BcNpcPlayerModelsTest` (3 fewer, the looks now come from the new table).
* Run locally and green: `BossLinesTest`, `BossLineGateTest`, `SinbadBossSourceScanTest`, `GokuBossSourceScanTest`, `GokuLadderTest` (43 tests).
* Could not run locally (they need the Minecraft classes, so Gradle in CI): `SinbadTableTest`, `BarStyleResolverTest`, `BarStylesAssetsTest`, `BossBarLangTest`, `SoundsJsonTest`, `VoiceLogicTest`, `ServerSafetyTest`, `GokuBossMathTest`, and the two self-tests `goku.boss.arts` and `sinbad.boss.arts`.

## 8. Honest caveats (nothing here was seen in a game)

* No in-game screenshot or video exists. Looks, djinn armours and bars were rendered by the repo's software renderer and mock-up tools; the art stills are recipe plans, not particle renders.
* **Blender is not installed here (rule 14).** None of the 16 looks was modelled in Blender; they come from the existing `tools/bc_playermodels` generator (layered cubes and painted textures), the same pipeline as the player-model kit. They should be reviewed and, if the owner wants Blender-grade models, redone in Blender.
* Sinbad's hair is the canon **purple** (from the Magi lore file), not the "golden-brown" mentioned in the request. This is a decision for the owner.
* The telegraph colours of the djinn (`SinbadDjinn.argb`) are not the armour colours of the player's `Djinn` table; they were not unified to keep the existing telegraphs unchanged.
* `SinbadBoss.java` was edited (synced equip, voice hooks) and `GokuBoss.java` got small voice hooks; the worker who owns the spawn and death code may conflict with them, so merge by hand if so.
* The client code (`GsLooks`, `GsBodies`, `GsAuraLayer`, `DjinnBossLayer`, the bar code) was reviewed but not compiled here; CI compiles it.
* The two arts self-tests assert hits on a wolf and could not be run here.
* The bar fill colour is not per form. The new bars are not seen in game.
* The wikis could not be fetched (section 5): no line is verbatim and no source gives an episode or chapter.
* `playermodel` animation of the forms (ki charge, transformation poses) reuses the existing player timelines; no new bespoke transformation cutscene was built.

## 9. Reference pass (the owner's images 12 to 16)

Rule 5 check: this is a **REPLACEMENT** of the look of the 16 looks (same ids, same kit, same files `pmx_gs_*`; nothing new is registered). The first Sinbad pass is kept in `gsbosses.py` (`sinbad_base`, `_djinn_body`, `SINBAD_V1`, not in `SPECS`), and the old Goku mark function `goku_marks` is kept next to `goku_marks2`.

**Goku (image 16, a Blockbench tutorial frame: anime Goku and a pixel Goku skin).** Read as "Goku should look like this". Base look and the form colours of the other seven now follow the skin: three stacked tiers of cube hair with a swept fringe, a short orange gi sleeve over a short blue undershirt sleeve, bare forearms, blue wristbands, a blue V-neck with crossed orange lapels, orange pants with darker chunky fold patches, and yellow boots with a red stripe and blue bits. The Ultra Instinct gi keeps its navy and silver (the boots are navy with a silver stripe). Hair colour, eyes and glow of each form are unchanged. Not matched: the reference skin is pixel-dithered in hard colour blocks; the kit's cloth painter is smoother, so the render reads cleaner and less blocky than the tutorial skin.

**Sinbad (images 12 to 15), matched by the repo lore and the Djinn table colours.**

| Image | Used for | Why |
|---|---|---|
| 12 | `sinbad_base` | white turban, long purple hair, white robe with a navy hem: the king without a djinn (lore file) |
| 13 | `sinbad_baal` | blue hair, feather and scale armour, gold antler diadem with a red gem; Baal is the blue djinn (`#3c8cff`) |
| 14 | `sinbad_furfur` | dark purple hair with orange tips, a huge feather fan, an orange ring of light; Furfur's table colour is purple (`#b77bff`). Alternative if the owner means another djinn: Focalor (swap the entries in `LOOKS_S`) |
| 15 | `sinbad_zepar` | red hair, red and gold, a gold sun buckle; Zepar is the red djinn (`#d43a3a`) |
| none | Valefor, Focalor, Vepar, Crocell | built in the same design language from the table colours: violet feathers and a fan, green scale shoulders, teal scales and fins, pale blue crystal shards and silver |

What was followed, all through the kit (`gsbosses.py`, helpers `_s_*`): palette, hair length (loose, to the hips) and colour (purple stays canon for the base), the turban or crown (gold antlers for Baal, a gold head band and gem for Zepar and Furfur, a fan of gold points or crystal shards for the others), the wide collar and the gold chains on the bare chest, the shoulder armour (feather rows, scale plates, crystal shards, or none for Zepar and Furfur), feather cuffs, the belt with a round buckle (a sun disc for Zepar), the long skirt with a jagged front and back sweep and torn feather ends (Zepar, Furfur), wing sandals, painted tribal lines (Zepar) and black thorn cracks (Furfur), and a ring of light behind him (two crossing rings in the fx layer, additive, in the djinn's colour) as the aura.

Not matched, honestly: Baal's sword is not modelled; the ref 14 and 15 swirl is a ribbon, here it is two rings; the long feather fan of Furfur reads as a spiky crown from the front; the hair is cube layers, not flowing locks; the chest muscles are painted shading only; the white drape over the shoulder of image 12 is missing.

**Interaction with the player's own armour layer.** `DjinnBossLayer` still draws the player's Djinn Equip pieces over this body. The new bodies already carry crowns, collars and shoulder pieces, so the armour pieces can cover or clip through them. This was NOT seen in a game. Decision for the owner: keep both, or let the references stand alone and draw the armour layer only during the forge-in animation.

**Cost (rule 11), worst case per Sinbad look** (from `stats_gs.json`): about 250 to 320 main quads, 12 fx quads, 70 to 100 hair quads; one look per boss instance, drawn only for nearby viewers by the existing renderer. No new draw route, no new packet, no server work.

**Previews:** `docs/boss_showcase/compare_*.png` (reference left, render right), see `docs/boss_showcase/README.md` section 1b; references are in `docs/boss_showcase/reference/`. The tool is `tools/bc_playermodels/compare_gs.py`. All are software renders, not in-game.
