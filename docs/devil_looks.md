# Contract devils on the anime player model

Owner request: remake the contract devils (docs/devil_contract.md, docs/devil_forms.md, docs/devil_worlds.md) in the quality and style of the Black Clover boss looks of 4.14.0 (docs/bc_playermodels.md): painted anime face on a 256x256 skin, layered cube hair, horns, wings, tails, bracers, mantles, glowing gems and eyes in each devil's palette, the head in proportion.

Setting: **`bossLooks.devilPlayerModel`** in `config/nusmp-playermodel-client.toml`, next to `bcPlayerModel`. It is a **CLIENT** config (it only changes how a devil is drawn). Default **true**. **false restores the previous figures exactly**: the Blender meshes (the ritual figure and the form worn on a signer) or, for a devil without a mesh, the generic horns and wings. Ids, hitboxes, saves, contracts, assimilation numbers and fights are untouched; nothing here runs on the server except the self-test.

## What is a software render here, and what was not seen

- The sheets in `docs/devil_looks_previews/` (`overview.png`, one turnaround sheet per devil `dv_<id>.png`: front, three-quarter, side, back; `overview_worn.png` and `dv_<id>_worn.png`: the look as it is worn on a player, hair layers left out) are **software renders** made by `tools/bc_playermodels/preview_dv.py` on the repo's `tools/playermodel` numpy renderer. They are not the game: lighting, mip-mapping, fog, the additive glow level and the sway of the extras differ, the extras are frozen at an idle frame, the worn sheets use a plain stand-in skin.
- **Nothing was run in a game and Gradle was not run** (no Gradle in this session; CI compiles). The Java was written against the kit's classes by reading them; the data (JSON, PNG) was checked with a Python mirror of `DevilLooksAudit` (0 problems for all 26 looks).
- **Models were not made in Blender** (rule 14): Blender is not available here. The parts are cubes and single cards built by the generator (`tools/bc_playermodels/dvlib.py`, `dvchars.py`, on the Black Clover builder `bclib` / `bcparts*`), like the kit's other extras. A real modelling pass would still improve silhouettes.
- **The skins are not hand painted**: they are painted by code (`dvskin.py`: outfit from a spec, then `bcskin.paint_face`), so faces are clean but generic. Expect the owner to want touch-ups.
- The pose of the ritual figure is a slow breath and the roar (arms raised); there is no walk, because the figure never walks. The worn form on a player uses the player's own pose.

## Sources (rule 10)

English wikis first, fetched through the fandom MediaWiki API (`api.php?action=parse&prop=wikitext`; the pages themselves answer 403, the API does not), the Appearance / Description sections, treated as untrusted data:

| Devil(s) | Source |
|---|---|
| Liebe, Lucifero, Beelzebub, Megicula, Lucifugus | Black Clover wiki (English) |
| Astaroth (Black Clover) | the English wiki has no Appearance section (only "highest-ranking devil, Time Magic"): **the look is this mod's design** (golden, time-themed) |
| Fox Devil, Curse Devil, Future Devil, Ghost Devil | Chainsaw Man wiki (English) |
| Sebastian Michaelis | Kuroshitsuji wiki (English) |
| the kin of Iblis, Egyn, Amaimon, Astaroth, Lucifer | Blue Exorcist wiki (English), the Demon Kings' host looks; **the kin are the mod's own designs** in their king's palette |
| the daemons of Noir, Blanc, Jaune, Violet, Rouge, Vert, Bleu | Tensura wiki (English), the Primordials' looks; the roster's devils are "daemons of their line" and wear the line's colours and style |
| God Hand | Berserk wiki (English): God Hand, Femto |
| Etherious | Fairy Tail wiki (English): Etherious, Mard Geer |
| Hall of Hell demon (Dororo) | **not fetched** (no page under the guessed names): drawn from memory of the series (an oni-like demon with horns and a fur collar) |

Not fetched at all: every Japanese wiki (the English ones answered, so the fallback was not needed) and the Dororo wiki.

## NEW / REPLACED / KEPT (rule 5)

- **NEW** (all of it): the 26 looks `dv_<devil id>` (skins, atlases, geo layers, animations, `looks_dv.json`), the `bossLooks.devilPlayerModel` option, `DevilLooks`, `DevilLooksAudit`, `DevilExtras`, the generator and previews, the tests, the self-test `bcmodel.devils`.
- **REPLACED**: nothing. The look is chosen by the option; the old drawing code stays in the files behind the off switch.
- **KEPT**: the devil meshes (`entity_meshes/devil_<id>.txt`), `DevilModels` horns and wings, the Weg markings, the ground ring, `bc_lucifero` and every other Black Clover look. `dv_lucifero` is a separate new build with the same canon as `bc_lucifero` (the boss and the contract devil are different characters in the mod).

## How it is built

| Layer | What |
|---|---|
| Skin 256x256 | The kit's 64x64 layout at 4x4 texels per pixel (`SkinPath.validSkinSize`): body painted from the outfit spec (coat, bare, open, dress, robe, armour, butler), painted anime face (iris, highlight, lashes, brows, mouth, nose, blush) and per-devil marks. A `_glow.png` map for glowing eyes and markings where the devil has them |
| Hair | Cube hair of the extras layers `_hair` and `_hair_fine`: a shell, stacked tiers, spikes, fringe, side locks, ponytails, flame or vine hair. The shell is 8.9 px wide over the 8 px skull = **1.11x**, so the head stays about 1.1x vanilla (a horn or crown adds to it on purpose). The look has no kit hair |
| Parts | Horns (curved, ram, bull, ox, small, four, brow), wings (bat, daemon, feathered), tails, bracers, mantles, collars, pauldrons, skirts, ribcage, skull, circlet or crown, gems |
| Glow | Only the `_fx` model has the glow flag; the others draw no additive pass |
| Layers | `pmx_dv_<id>` (main), `_fine` (single strands, tassels, fur: dropped beyond the detail distance), `_fx` (glowing parts), `_hair`, `_hair_fine`; one atlas per devil |

### Where each look is drawn

- **The ritual figure** (`DevilNpcRenderer`, the devil in front of the caller): the vanilla `PlayerModel` with the devil's skin and glow map, then `DevilExtras.draw` for the cube parts. Same RISE, ROAR, MERGE and DISSOLVE motion as the mesh figure (sink, arms raised, shrink, fade).
- **The form worn on a signer** (`DevilFormLayer`): `DevilExtras.draw` on the player's own model without the hair layers (the player keeps their hair and skin). Each anchor forges in with the same stage and timing as the mesh parts: head from 20 %, arms, wings and tail from 50 %, legs at 100 % (wings and the tail hang on the body anchor, so mantles and skirts of the look come in with them at 50 %). `WegLayer` already skips the generic horns and wings while the form is worn; it does so for the look too (`DevilFormLayer.usable`).
- **Devil forms** (docs/devil_forms.md): the form is the devil's look; each devil keeps its own horns, wings and palette. There is one look per devil, not one per stage: the stages only decide which parts are shown.

## Quad budget per devil (full detail)

Quads = faces of the cubes the geo files hold; the player model's own body (about 72 quads for the six parts plus about 72 for the second layer) is on top. The audit limit is 400 quads and 60 glowing quads per look, atlas at most 512x512 (all are 256x256 or smaller).

| devil | look | cubes | quads main+fine | hair+hair_fine | glow (fx) | total | atlas tiles | scale |
|---|---|---|---|---|---|---|---|---|
| liebe | dv_liebe | 62 | 108+0 | 66+8 | 0 | 182 | 29 | 0.9 |
| lucifero | dv_lucifero | 95 | 153+16 | 48+38 | 0 | 255 | 33 | 1.08 |
| beelzebub | dv_beelzebub | 103 | 180+18 | 42+38 | 0 | 278 | 34 | 1.1 |
| astaroth | dv_astaroth | 113 | 186+42 | 42+38 | 22 | 330 | 41 | 1.04 |
| megicula | dv_megicula | 100 | 199+16 | 42+38 | 0 | 295 | 33 | 0.94 |
| lucifugus | dv_lucifugus | 100 | 211+24 | 42+38 | 22 | 337 | 41 | 1.06 |
| fox_devil | dv_fox_devil | 92 | 140+18 | 66+8 | 0 | 232 | 34 | 1.0 |
| curse_devil | dv_curse_devil | 59 | 249+0 | 0+0 | 0 | 249 | 17 | 1.06 |
| future_devil | dv_future_devil | 61 | 173+22 | 0+0 | 2 | 197 | 13 | 1.08 |
| ghost_devil | dv_ghost_devil | 107 | 60+58 | 42+38 | 20 | 218 | 33 | 1.0 |
| sebastian | dv_sebastian | 34 | 45+0 | 42+12 | 0 | 99 | 21 | 1.06 |
| kin_of_iblis | dv_kin_of_iblis | 81 | 164+40 | 24+3 | 22 | 253 | 26 | 0.98 |
| kin_of_egyn | dv_kin_of_egyn | 63 | 91+16 | 36+20 | 0 | 163 | 31 | 0.92 |
| kin_of_amaimon | dv_kin_of_amaimon | 67 | 146+0 | 58+8 | 0 | 212 | 26 | 1.0 |
| kin_of_astaroth | dv_kin_of_astaroth | 71 | 164+0 | 62+8 | 2 | 236 | 38 | 0.96 |
| kin_of_lucifer | dv_kin_of_lucifer | 100 | 156+24 | 62+8 | 32 | 282 | 38 | 1.06 |
| daemon_noir | dv_daemon_noir | 68 | 106+18 | 42+17 | 0 | 183 | 31 | 1.06 |
| daemon_blanc | dv_daemon_blanc | 84 | 96+18 | 42+38 | 0 | 194 | 31 | 1.05 |
| daemon_jaune | dv_daemon_jaune | 62 | 100+18 | 36+17 | 2 | 173 | 26 | 0.98 |
| daemon_violet | dv_daemon_violet | 69 | 97+16 | 36+24 | 0 | 173 | 25 | 0.96 |
| daemon_rouge | dv_daemon_rouge | 108 | 210+18 | 42+38 | 0 | 308 | 34 | 1.06 |
| daemon_vert | dv_daemon_vert | 82 | 100+16 | 42+38 | 2 | 198 | 31 | 1.0 |
| daemon_bleu | dv_daemon_bleu | 60 | 100+16 | 36+17 | 2 | 171 | 25 | 0.94 |
| hall_demon | dv_hall_demon | 97 | 126+18 | 70+8 | 0 | 222 | 41 | 1.1 |
| god_hand | dv_god_hand | 93 | 189+0 | 42+12 | 10 | 253 | 29 | 1.08 |
| etherious | dv_etherious | 87 | 169+0 | 42+45 | 0 | 256 | 38 | 1.04 |

Largest look: `dv_lucifugus`, 337 quads. The same numbers are in `tools/bc_playermodels/stats_dv.json` (written by `gen_dv.py`, checked by `DevilPlayerModelsTest`).

## Worst-case cost (rule 11)

All of it is **client draw cost**. Nothing runs on the server except the self-test (one pass over the resource files at boot); there are **no new packets, no new entities, no particles, no per-tick work, no world scans**. The server never loads a client class (`DevilLooks` and `DevilLooksAudit` are pure; `DevilExtras` lives in `client/`).

- **Ritual figure**: at most `npc.max` figures exist on the server (default 8, one per player), culled beyond 64 blocks. Per figure per frame: the player model (about 144 quads) plus at most 337 quads of the look (the fine layers only within the detail distance, 24 blocks by default) and at most two additive passes (skin glow map and the `_fx` model; none beyond the far distance, 64 blocks). Worst case on screen: 8 x (144 + 337) = about **3,850 quads and 16 additive batches** per frame, in practice far below (each look has fewer than 340 and the fine layers drop with distance).
- **Worn form on a player**: the existing crowd budget still decides who is drawn at all (`NoelleFormRules.budgeted`: at most 4 at the full level and 6 at the far level per frame, nothing beyond 48 blocks, and your own body always standard). The look is drawn without hair, so at most 337 - (hair layers) quads per wearer, at most 4 x 337 + 6 x (337 - fine layers) = about **3,400 quads per frame worst case**, and the additive gem pass only for the full level. One more object per frame: none (the opacity array is a field of the rig).
- **Allocation**: `DevilExtras.draw` and `drawLook` build no object (the same iterator over a small map the kit's `ExtrasDraw` makes); looks are table lookups (`DevilLooks.of` is one map get, `PlayerAssets.lookOrNull` one synchronized map get); the textures are the kit's cached locations. `DevilPlayerModelsTest` asserts that the two bodies contain no `new`.
- **Memory**: 26 skins (256x256, about 256 KB each in GPU memory, loaded only when that devil is first drawn), 26 atlases (256x256 or smaller) and 104 small geo files, read once and cached by `ExtraModels`.
- **Perf probe**: the existing sections `c.devil.npc` and `c.devil.form` time the new drawing, because it sits inside the same methods.

## Tests

- `DevilPlayerModelsTest` (JUnit, no game): audit of the files finds nothing; every `WorldDevil` has a look in roster order; the old looks (Black Clover included) are kept and the new table comes last; hair shell at most 9 px and budgets from `stats_dv.json`; the option is a client config, default on, the old figures are one switch away; no server-unsafe class and no per-frame allocation in the new sources; lang fragment; self-test registered under the smoke prefix; doc, generator and previews exist.
- `GeoAssetsTest.sheet()` (appended, one line): `pmx_dv_*` geos (`_fx`, `_fine`, `_hair`, `_hair_fine`) map to the base atlas, the same way `pmx_bc_*_fine/_fx` do, so the existing texture, UV and glow-size checks cover them.
- Self-test `bcmodel.devils` (CI server smoke): runs `DevilLooksAudit` on the jar. It sits under the existing `bcmodel` prefix of the smoke list, so the list did not change.

## Files

Generator and previews: `tools/bc_playermodels/{dvlib,dvskin,dvchars,dvtable,gen_dv,preview_dv}.py`, `stats_dv.json`, `docs/devil_looks_previews/`. Resources: `playermodel/looks_dv.json`, `textures/entity/playermodel/dv_<id>(_glow).png`, `textures/entity/pmx_dv_<id>(_glow).png`, `geo/entity/pmx_dv_<id>{,_fine,_fx,_hair,_hair_fine}.geo.json`, `animations/entity/pmx_dv_*.animation.json`. Java: `client/playermodel/{DevilLooks,DevilLooksAudit,DevilExtras}.java`; appended to `PlayerLookTables.FILES`, `PlayerModelOptions`, `PlayerModelSelfTests`; `BcAudit.geoProblems` made package-private; edited `client/devil/DevilNpcRenderer.java` and `DevilFormLayer.java`. Lang fragment `tools/lang/devil_looks.json`.

## Limits and decisions for the owner

- No hair on a worn form: the signer keeps their own hair and skin, only the devil's parts (horns, wings, tail, bracers, mantle, gems) appear. If the owner wants the devil's face and hair on the signer, that needs a skin swap and is a separate decision.
- One look per devil (forms differ by stage, not by look). A second look per stage would double the assets.
- `dv_lucifero` and `bc_lucifero` are two builds of the same canon; the owner may want them unified later.
- The Hall of Hell demon (Dororo) and Astaroth (Black Clover) are not backed by a fetched wiki page, and the kin devils are the mod's own designs.
- Unverified in a game: pose of the NPC (the arms-raised roar), the stage timing of the worn parts, the scale of the heads on the player model at 4x skin, and every colour.
