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
* **Audio is not generated** (superseded: made in section 10.2). The coordinator makes it with the offline Kokoro tool (`tools/audio/offline_voice.py`, profiles `goku` and `sinbad`); put the files at `src/main/resources/assets/anime_multiverse/sounds/voice/<boss>/<id>.ogg` and run `python3 tools/audio/boss_lines_gen.py --register`, which rewrites the registered list in `BossLines.java` and appends the `sounds.json` events. Until then every line is chat text only and `BossLinesTest` checks that "registered" equals "has a file".
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

**Interaction with the player's own armour layer.** `DjinnBossLayer` still draws the player's Djinn Equip pieces over this body. The new bodies already carry crowns, collars and shoulder pieces, so the armour pieces can cover or clip through them. This was NOT seen in a game. Decision for the owner: keep both, or let the references stand alone and draw the armour layer only during the forge-in animation. **Resolved in section 10.1** (the pieces the look wears are forged in and then dissolve into it; a client option restores the old drawing).

**Cost (rule 11), worst case per Sinbad look** (from `stats_gs.json`): about 250 to 320 main quads, 12 fx quads, 70 to 100 hair quads; one look per boss instance, drawn only for nearby viewers by the existing renderer. No new draw route, no new packet, no server work.

**Previews:** `docs/boss_showcase/compare_*.png` (reference left, render right), see `docs/boss_showcase/README.md` section 1b; references are in `docs/boss_showcase/reference/`. The tool is `tools/bc_playermodels/compare_gs.py`. All are software renders, not in-game.

## 10. Armour fit and voice pass (follow-up of the two open points)

Owner: "do what you think is best". Rule 5 check: the armour fit is a **NEW** switch and a **NEW** pure check on top of what exists (nothing is replaced, the old drawing is one setting away); the voice pass is a **REPLACEMENT** of the audio of the same 117 lines (same ids, same texts, same file names), with the first-pass files kept.

| | What |
|---|---|
| **NEW** | `GsArmorFit` (pure box checks + the table of Djinn pieces the Sinbad looks already wear), the client option `bossLooks.bossArmorFit` (default on), `DjinnArmorLayer.draw(..., hide)` and the mask in `DjinnBossLayer`; `GsArmorFitTest`; `tools/bc_playermodels/gs_fit_report.py`; `tools/audio/boss_voice_polish.py` (pronunciation table, per-line chain, measured loudness), `tools/audio/voice_measure.py`, profiles `goku2` and `sinbad2`, `BossVoicePolishTest`, `tools/audio/boss_voice_polish_report.json` (before / after numbers), `tools/lang/boss_armor_fit.json` (+ 2 keys appended to `en_us.json`). |
| **REPLACED** | The audio of all 52 Goku and 65 Sinbad lines (`sounds/voice/<boss>/<id>.ogg`). |
| **KEPT** | The 117 first-pass files, copied to `sounds/voice/goku/classic/` and `sounds/voice/sinbad/classic/` (never played, never registered; `VoiceAssetsTest` skips `/classic/` in its orphan walk); the old profiles `goku` and `sinbad`, `offline_voice.py` unchanged; every geo, texture and look file of the 16 looks (nothing was edited); the player's own Djinn Equip drawing (mask 0). |

### 10.1 Armour overlap

**Where it is drawn.** Goku and Sinbad are `PlayerModelRenderer.Body` instances over the kit's `KitPlayerModel` (vanilla player cubes). On top of the base cubes the renderer draws: the vanilla `HumanoidArmorLayer` (the bosses wear nothing), the look's geo extras (`ExtrasLayer`: `pmx_gs_<id>`, `_fine`, `_fx`, `_hair`, `_hair_fine`), and for Sinbad `DjinnBossLayer` -> `DjinnArmorLayer.draw`, the player's own armour meshes (`djinn/u_<id>/<piece>.txt`, one set per djinn) anchored on the same model parts.

**Measured (pure, `tools/bc_playermodels/gs_fit_report.py`, all 16 looks, 992 cubes):**

* Outer-layer cubes against the vanilla body cube of their bone (head, body, arms, legs; slim and wide): **0 buried in the body, 0 sharing a face plane with it (within 0.02 px).** 16 sandal soles only touch the leg from outside (not a fight). 40 hair-shell cubes of the 8 Sinbad looks sit inside the head's second-layer envelope (+0.5): by the kit's design (`dvlib.HG`, a 0.05 px gap off the skull), the second skin layer (hat, jacket, sleeves, pants) is **empty in all 16 skins** (checked per pixel in the test), so nothing hides them; Goku's hair stands 0.3 px off the head. So the bodies themselves do not clip, and nothing in the geo or the textures was changed.
* **The overlap is the player's Djinn Equip pieces over the Sinbad looks.** Each Sinbad body already carries a crown, a wide collar with chains, a belt with a buckle, cuffs, shoulder plates (not Zepar, Furfur) and sandals (`gsbosses.LOOKS_S`); the player's set has a helm, brow crest, chest, belt, vambraces, pauldrons and greaves of the same names, drawn 0.5 to 2 px outside the same cubes. Share of the quads of each piece that lie within 0.8 px of a cube of the look on the same bone (from the report): chest 0.63 to 0.89, vambraces 0.41 to 0.89, belt 0.30 to 0.57, greaves 0.36 to 0.46, pauldrons 0.33 to 0.84 (0.00 for Zepar and Furfur, who have no shoulders), helm 0.14 to 0.67, brow crest 0.27 to 0.73. Wings, tail, halo, gauntlets, ears, mantle, the piece behind the back and the weapon have no counterpart on the looks.

**Fix (client only, no geometry removed).** `GsArmorFit.hiddenPieces(equipIdx)` lists, per djinn, the piece names the look already wears (crown: helm and brow crest; collar: chest; belt; cuffs: both vambraces; sandals: both greaves; shoulders unless Zepar or Furfur; Baal's antlers stand in for his horns). `DjinnBossLayer` turns that into a bit mask; `DjinnArmorLayer.part` forges those pieces in exactly as before (they fly in and fade up) but fades them out again as they arrive (alpha `4a(1-a)`, gone at full assembly) and does not draw them in the shatter phase, so the look's own gear stands. Everything else (wings, tail, halo, gauntlets, ears, mantle, back piece, weapon, aura, burst, the Lesser and Half Equip) is the player's drawing unchanged. A player's own call uses mask 0: **nothing changes for players**. Setting: client config `bossLooks.bossArmorFit` (file `nusmp-playermodel-client.toml`, default `true`); `false` = every piece stays, the pre-fix drawing. Fewer quads are drawn while it is on (the hidden pieces are 1130 to 2160 of the 3250 to 4350 quads of one djinn set, a third to a half).

**Not done / not claimed.** Not seen in a game: the dissolve timing and whether the gauntlets (kept) still touch the feather cuffs at the wrist are judged from numbers only. A vanilla helmet or armour put on a boss (none is) would still wrap the hair shell (outer armour +1.0).

**Cost (rule 11).** Server: none (client only, no packet, no config on the server). Client: one int mask lookup per boss per frame (the mask is built once per equip number), at most the player's piece set that is already drawn for one player in Djinn Equip, fewer quads than before while the fit is on; no allocation per frame, same 48 and 24 block culls.

**Tests.** `GsArmorFitTest` (6): the checker is proved on made-up cubes (buried, jacket-buried, face-plane fight, touch from outside, a wrap, a plate); every cube of every `pmx_gs_*` geo file is clear of its bone's body cube; the second skin layer is empty in the 16 skins; every hidden name is a real `DjinnMeshes.Piece`, the set keeps its wings, tail, halo, gauntlets, ears, mantle, back piece and weapon, Zepar and Furfur keep their pauldrons, and a look only replaces a piece when it has cubes on that bone; the option, the mask and the "player call = mask 0" are in the source. Ran with `javac` + the JUnit launcher (no Gradle): all pass.

### 10.2 Kokoro voice pass

**Offline only.** Kokoro-82M (Apache-2.0, in `~/.cache/nusmp_voice`), numpy and ffmpeg; no ElevenLabs, ForgeGUI or other paid or web service was called (the polish tool has no network code; `BossVoicePolishTest` checks that).

**What was wrong, measured on the first-pass files** (`tools/audio/voice_measure.py`, same tool before and after):

* Sinbad: spectral centroid **515 Hz** and **43 %** of the energy below 150 Hz (a boomy, muffled voice: +2.5 dB at 200 Hz, a 0.9 s reverb and a chorus on top, all pitch-shifted together); the end of a clip had **0.004 s** of tail left (the reverb was cut off, not faded).
* Goku: 6.0 % of the energy above 5 kHz (hiss and sibilance), median pitch 142 Hz.
* Both: loudness only roughly level (spread 0.25 LU in both sets, single clips from -17.0 to -14.8 LUFS) because ffmpeg `loudnorm` on a 1 to 3 s clip falls back to a dynamic mode and misses; and names Kokoro cannot read: `Kamehameha` came out as "kay-m-hay-m-ha", `Saiyan` as "say-yan", `Kaio-ken` as "kye-oh-ken", `Nngh`, `HAAAAAA` as odd vowels.

**What changed** (`tools/audio/boss_voice_polish.py`, profiles `goku2` and `sinbad2` in `offline_voice_profiles.json`; the old profiles stay):

1. **Pronunciation.** 23 IPA respellings (`PRON`; names of the techniques and djinn, the grunts, and the held shouts as long vowels; British adaptation for Sinbad's voices) plus one whole-line respelling for the `Ka... me... ha...` line. Only lines with such a word go through Kokoro's phoneme path; the other lines are spoken from their text exactly as before. `python3 tools/audio/boss_voice_polish.py probe` prints the phonemes of every line (34 of the 117 lines use a respelling).
2. **Voices (chosen by measurement of median pitch and spectral balance on 8 lines each, 3 to 4 candidates per hero).** Goku: blend `am_eric 0.5, am_liam 0.3, am_santa 0.2`, speed 1.08, pitch -0.5 semitone (median pitch 162 Hz, was 142). Sinbad: `bm_george 0.5, bm_lewis 0.3, bm_fable 0.2`, speed 0.95, pitch -0.7 with shifted formants (median pitch 132 Hz, was 131, kept in his baritone). Per-group speed on top (charge-ups and the Spirit Bomb slower, quick calls faster).
3. **Chain.** Pitch, high-pass (80 / 85 Hz), EQ (Goku: presence +3.5 dB at 3.3 kHz; Sinbad: -2.5 dB at 110 Hz and -1.5 dB at 250 Hz against the boom, +2 dB at 3.2 kHz), de-esser, gentle compressor, light chorus (Sinbad), THEN the room (Goku: 0.35 s, 5 % wet, Sinbad: 0.7 s, 11 % wet, 18 ms pre-delay), so the reverb is no longer pitch-shifted; the tail is faded (60 ms), not cut.
4. **Per-line cleanup.** DC removed, Kokoro's lead and tail babble below -45 dBFS cut by a 10 ms envelope, 20 ms lead, 5 ms fade-in, 60 ms fade-out.
5. **Loudness.** A measured gain to -16 LUFS, limited at -2.3 dBFS, measured again (up to three rounds); Ogg Vorbis q4, 32 kHz, mono (was q3).

**Before / after, all 117 lines** (first pass = `classic/`; the full per-clip numbers are in `tools/audio/boss_voice_polish_report.json`):

| | Goku first pass | Goku now | Sinbad first pass | Sinbad now |
|---|---|---|---|---|
| lines | 52 | 52 | 65 | 65 |
| total seconds | 86.0 | 90.4 | 176.3 | 162.8 |
| mean clip length (s) | 1.65 | 1.74 | 2.71 | 2.5 |
| loudness mean (LUFS) | -16.21 | -16.09 | -16.01 | -16.13 |
| loudness spread (std, LU) | 0.25 | 0.05 | 0.25 | 0.07 |
| loudest / quietest clip (LUFS) | -16.0 / -17.0 | -16.0 / -16.2 | -14.8 / -16.8 | -15.9 / -16.3 |
| true peak, highest (dBTP) | -1.2 | -1.5 | -1.4 | -1.4 |
| median pitch (Hz) | 142.0 | 162.0 | 131.0 | 132.0 |
| spectral centroid (Hz) | 1212.0 | 1094.0 | 515.0 | 909.0 |
| energy above 5 kHz (%) | 5.95 | 3.18 | 1.47 | 4.19 |
| energy below 150 Hz (%) | 8.74 | 11.64 | 43.42 | 34.17 |
| lead silence (s) | 0.031 | 0.027 | 0.026 | 0.026 |
| tail left at the end (s) | 0.086 | 0.057 | 0.004 | 0.036 |
| size of the files | 744 KB | 764 KB | 1387 KB | 1288 KB |

**Read honestly.** Better by the numbers: loudness spread 0.25 to 0.05 / 0.07 LU, every clip within 0.3 LU of -16 LUFS, true peak at most -1.4 dBTP, Sinbad's boom (energy below 150 Hz 43 to 34 %, centroid 515 to 909 Hz), Goku's sibilance (above 5 kHz 6.0 to 3.2 %), the faded tails, no clip longer or shorter than 0.75 to 1.3 times its first-pass length (no cut-off or runaway take), the respellings. Different on purpose, not "better": Goku's median pitch is 20 Hz higher (a younger, more energetic hero; his centroid fell from 1212 to 1094 Hz and his low end rose from 8.7 to 11.6 % because the new blend is a little fuller), Sinbad's high end rose (above 5 kHz 1.5 to 4.2 %) so his consonants are clearer, which also makes him less "hall-like"; Sinbad is still bass-heavy (34 %), only less so. These are my choices, not facts about taste.

**Registration.** `python3 tools/audio/boss_lines_gen.py --register` was run; the registered list, `sounds.json` and the subtitles did not change (same ids). `tools/audio/goku_voice_report.json` and `sinbad_voice_report.json` were rewritten with the new durations and sizes.

**Tests.** `BossLinesTest` (8), `BossVoicePolishTest` (2: 117 mono 32 kHz Ogg files of 0.5 to 12 s, every first-pass file still on disk and different from the new one, new profiles, tools present, no web call), `VoiceAssetsTest` (5, now skipping `/classic/`), `SoundsJsonTest`, `SalamanderSoundsTest`, `SylphSoundsTest`, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest` and the 13 `client/playermodel` test classes (107 tests in that run, with `GsArmorFitTest`): all pass under `javac` + the JUnit launcher. The one run that failed (`DungeonLooksTest`) failed only because my ad-hoc classpath had no resources; it passes with them.

**Honest caveats (voice).** I can measure sound; I cannot judge it. Loudness, peak, spectrum and pitch are numbers; whether Goku now sounds like a cheerful fighter and Sinbad like a warm king, whether the respellings are said right, and whether the slower or faster lines fit the moment, **has not been heard by anyone**. The phoneme strings were checked against Kokoro's tokenizer and by phonology, not by ear. The files are 117 short clips (2.1 MB first pass, 2.0 MB now, plus 2.1 MB kept under `classic/` in the jar).

**Owner decisions.** (1) Listen to five lines from each hero (`goku_intro_1`, `goku_form_ssj_1`, `goku_kamehameha_1`, `goku_kamehameha_2`, `goku_low_hp_1`, `sinbad_intro_1`, `sinbad_equip_baal_1`, `sinbad_inqerad_1`, `sinbad_garufor_2`, `sinbad_defeat_2`) next to the `classic/` take; if a hero is worse, copy that `classic/` file back over the new one (the ids are identical) or change the profile and re-run `synth` and `convert` for that boss. (2) Keep or drop the `classic/` copies from the jar (they add the size above; the rule says never delete, so they stay). (3) Whether Sinbad's gauntlets should dissolve too (they are kept; the look has cuffs just above them). (4) Whether `bossArmorFit` should stay on by default.
