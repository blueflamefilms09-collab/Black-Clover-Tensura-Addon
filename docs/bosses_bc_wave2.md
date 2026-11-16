# Black Clover bosses, wave 2: redesigned player-model looks

Wave 1 (4.14.0, `docs/bc_playermodels.md`) gave 14 Black Clover boss looks a painted anime face, layered cube hair, props, glow and a wiki palette. Wave 2 does the same for the remaining
Black Clover bosses in the mod. Built on the same generator (`tools/bc_playermodels/gen_bc.py`, extended, not rewritten), the same audit (`BcAudit`), the same look file (`looks_bc.json`).
Every wave 2 look is a software render only: nobody has seen them in a game yet.

## What is built (21 looks)

| Boss | Looks (ids are `bc_<old id>`) | Source |
|---|---|---|
| Dark Disciples | `dark_disciple`, `_lucifero`, `_megicula`, `_beelzebub`, `_astaroth` | English Black Clover wiki (Megicula, Beelzebub, Astaroth, Devil, Dark Triad). The disciples have no page of their own (only "loyal subordinates of the triad"), so the look is the mod's own hooded acolyte, coloured by its devil. |
| Devil Avatar | `devil_avatar`, `_form2`, `_megicula`, `_megicula_form2` | Mod character; takes the canon devil look of Lucifero / Megicula (horns, wings, tail, long light hair). No wiki page of its own. |
| God of Order | `god_of_order`, `_form2` | Mod character (`docs/bosses/rework_wave3.md`): hood, layered white robe with gold trim, gold stole, closed serene face, halo, staff and ledger. |
| Heath Grice | `heath_grice`, `_form2` (purified) | English wiki, Appearance section: grey hair combed back, scar, blue shirt and short hooded cape; purified form has wings, halo, light clothes. |
| Lotus Whomalt | `lotus_whomalt` | English wiki, Appearance. |
| Mars | `mars`, `_form2` | English wiki, Appearance (spiky hair, ruff collar, sword, crystal). |
| Revchi Salik | `revchi_salik` | English wiki, Appearance. |
| Vetto | `vetto`, `_form2` (demonic) | English wiki, Appearance. |
| Morris Libardirt | `morris_libardirt`, `_form2` (purified) | English wiki, Appearance (the "Moris" spelling has no separate page). |

Source rule 10: the English wiki was reachable through `api.php`; the Japanese wiki was not needed. Not fetchable as their own page: Dark Disciples, Devil Avatar, God of Order (all mod-made).

## Part counts and quad budget

From `tools/bc_playermodels/stats.json` (main layer / fine layer dropped beyond 24 blocks / glow layer). Limits (`BcAudit`): 700 quads in all, 120 glowing quads, atlas 512.

| Look | main | fine | fx | total |
|---|---|---|---|---|
| dark_disciple (all five) | 94 | 0 | 2 | 96 |
| devil_avatar | 253 | 38 | 2 | 293 |
| devil_avatar_form2 | 317 | 52 | 4 | 373 |
| devil_avatar_megicula | 301 | 38 | 2 | 341 |
| devil_avatar_megicula_form2 | 413 | 52 | 4 | 469 (largest) |
| god_of_order | 138 | 24 | 22 | 184 |
| god_of_order_form2 | 138 | 24 | 34 | 196 |
| heath_grice | 106 | 6 | 0 | 112 |
| heath_grice_form2 | 102 | 6 | 4 | 112 (+ kept props) |
| lotus_whomalt | 111 | 27 | 0 | 138 |
| mars / mars_form2 | 94 | 21 | 33 | 148 |
| revchi_salik | 88 | 42 | 0 | 130 |
| vetto | 80 | 72 | 0 | 152 |
| vetto_form2 | 194 | 72 | 0 | 266 |
| morris_libardirt | 72 | 8 | 0 | 80 |
| morris_libardirt_form2 | 182 | 8 | 2 | 192 (+ kept props) |

Heath form 2 and Morris form 2 also draw a small `_keep` model (the held watch / floating orb, or the right arm prop) taken from the old model; its quads are small and counted by the audit.

## Worst-case cost (rule 11)

- Server: nothing new. No new entity, packet, tick work or config on the server; the looks are client-only data.
- Network: none.
- Client draw: at most 469 quads for one boss at full detail (largest look); beyond 24 blocks the fine layer is dropped (at most 52 quads) and far bosses fall back as before. Glow layer at most 34 quads (limit 120). Atlases are 256-class textures, one per look.
- Shimmer: `BcShimmer` unchanged, 5 particles per second per boss, full detail only, through the existing perf budgets.
- Animation: reuses the generator's bone clips (idle / walk / run / cast); no per-frame allocation added.

## Settings

- `bossLooks.bcPlayerModel` (client, existing): turns on every wave 2 look except the God of Order. Old looks stay registered and are used when it is off.
- `bossLooks.godOfOrderPlayerModel` (client, NEW, default off): the God of Order keeps its Blender body by default; turn this on for the player-model look. While on, `PlayerModelOptions.blender("god_of_order*")` returns false so the two do not draw together.
- Lang keys are in `tools/lang/bosses_bc_wave2.json` (to be merged into `en_us.json` by the lead).

## NEW / REPLACED / KEPT

- NEW: all 21 `bc_` looks (skins, atlases, geo, animations, `looks_bc.json` entries), `BcWave2`, `bcchars2.py`, `bcparts4.py`, `bcskin2.py`, the `godOfOrderPlayerModel` setting, `BossesBcWave2Test`, the `--wave2` preview mode.
- REPLACED (only when the switch is on): the visible look of the Dark Disciples, Devil Avatar, Heath, Lotus, Mars, Revchi, Vetto and Morris. Their old looks and old extras models stay registered. Dark Disciple bodies now ask `PlayerAssets.bcId(...)` instead of the fixed old id.
- KEPT: every old look and model; the God of Order Blender body (default); held props, capes and floating orbs of the old models (whole old extras kept for the disciples, God of Order, Mars, Revchi, Lotus, Heath base and Morris base; for Heath form 2 and Morris form 2 only the props are kept as `_keep` models, because the old crude wings, horns and halos are replaced by new parts).
- Shared code touched: `BcLooks` (falls through to `BcWave2`, adds `allOldIds()`; `oldIds()` still returns the 14 of wave 1), `BcAudit` (iterates `allOldIds()`, `_keep` models are allowed to have a glow flag), `PlayerAssets.bcId`, `PlayerBodies` (Disciple), `PlayerModelOptions`, `PlayerModelSelfTests` (message count), `BcPlayerModelsTest`, `bcskin.py` (upscales the 64x64 God of Order skin), `gen_bc.py`, `preview_bc.py`.

## Bosses skipped and why

- Seven Deadly Sins bosses (Meliodas, Escanor, Ban, Diane, Gowther, King, Merlin): another franchise, not Black Clover.
- Goliath, Charybdis, Minotaur: not Black Clover. Udaeus: DanMachi. Tier List and Abyssal bosses: other franchises.
- Rhya, Fana, Patolli, Rades (Midnight Sun elves) and the Black Clover NPCs: another worker's share.

## Tests

- `BossesBcWave2Test` (new, pure): the table (21 ids, wave 1 still 14, an SDS boss is not in it), every `bc_` look exists with the old one kept and the old scale, no kit hair, `BcAudit.run` finds nothing, quad budget from the geo files themselves, the `_keep` models (real bones, parents present, no wings / horns / halo, atlas exists), the God of Order switch (default off, Blender body wins unless on), server safety of `BcWave2`, existence of this doc, previews, generator files and lang fragment.
- `BcPlayerModelsTest` and the CI self-test `bcmodel.looks` now also cover wave 2 through `BcLooks.allOldIds()`, so no new smoke prefix is needed.
- Not run: the full Gradle build and the whole test sweep (no Gradle in this session); CI compiles them.

## Previews

`docs/bosses_bc_wave2_previews/overview.png` and one sheet per look (`bc_<id>.png`), software renders of the skin and geometry in an idle pose, made with `python tools/bc_playermodels/preview_bc.py --wave2 --old`.

## Honest caveats

- Not seen in a game. The previews are a numpy software renderer, not Minecraft: lighting, glow and animation can differ.
- Not modelled in Blender (rule 14): the parts are generated cube geometry through the existing generator pipeline (`bclib` / `bcparts*`). The God of Order's Blender model is kept as the default.
- The skins are generated and retouched by the face painter (`bcskin`, `bcskin2`), not hand-painted. Some faces (Lotus, Vetto, Morris) are simpler than the canon art.
- The Dark Disciples, the Devil Avatar and the God of Order have no canon page; those looks are the mod's own design.
- Heath, Morris and the Avatars' animation reuses the generator clips; the wings and tails sway but are not canon animations.
- `looks_bc.json`, `BcLooks`, `BcPlayerModelsTest` and `PlayerModelOptions` are shared with the other worker; if both land, re-run `python tools/bc_playermodels/gen_bc.py` and merge the small Java edits by hand.
- Decisions for the owner: should the God of Order default to the new look? The Seven Deadly Sins bosses could get the same treatment with their own wiki pass.
