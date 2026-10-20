# Worn relic upgrade: how the two slices were merged (release 1.106.0)

Two slices extended the same worn relic library in parallel: **God worn** (`docs/visual_upgrade/god_worn.md`, 24 God relics, own skin per relic, palette tinted) and **Charms / Genesis** (`docs/visual_upgrade/charms_genesis_worn.md`, 27 relics, painted skins, glow map, nebula and halo passes). This page says how they became one model. `UpgradeMergeTest` pins every point below.

## One quad flag table (`WornMesh`)

| bit | flag | slice | meaning |
|---|---|---|---|
| 1 | EMIT | library | lit stone: dark in the solid pass, bright in the additive pass |
| 2 | SHELL | library | rim glow / shimmer shell |
| 4 | HOT | library | pulses |
| 8 | GLOWMAP | Charms / Genesis | redrawn additively with the glow map of the model (white, pulsing) |
| 16 | STAR | Charms / Genesis | starfield / nebula pass (scrolling) |
| 32 | HALO | Charms / Genesis | halo / light shaft / portal pass over `vu_fx.png` |
| 64 | LIT | God worn | also glows where its glow map says so, in the glow slot colour |
| 128 | ADD | God worn | additive pass only (halo, soft sprite), never in the lit solid pass |

The Charms / Genesis values (8, 16, 32) were released in 1.104.0 and keep their numbers. God worn's LIT (was 8) and ADD (was 16) moved to 64 and 128. The God worn generator (`gw_lib.py`, `metrics.py`, `preview.py`) writes the new numbers, `build.py --force` regenerated the `gw_*.txt` models, and the result was checked line by line against the old files: only the flags field of 19428 quad lines changed, nothing else. The flags are stored in a byte; bit 128 is a negative byte and is tested with a mask, which a test covers.

`emitQuads` counts EMIT, LIT and ADD quads (the lit-glow pass); `glowMapQuads`, `starQuads`, `haloQuads` count the Charms / Genesis flags.

## One skin model

`skin <v>` in a model file is either a texture path ending in `.png` (Charms / Genesis: painted skin, untinted, `glowtex` and `startex` name the other textures) or a bare id (God worn: `textures/relic_worn/god/<id>.png`, glow map `<id>_glow.png`, grey albedo tinted by the palette slots like the atlas). `WornMesh.tinted()`, `skinPath()` and `glowPath()` give the answer; `skin`, `glowTex`, `starTex` and `lodName` are the records as written (the older tests read them).

## One draw path (`WornLibDraw`)

Every model with a skin is drawn by `customPasses`: (1) skin (tinted skins use the slot colours, EMIT quads dark), (2) glow map of the GLOWMAP quads, (3) lit glow of the EMIT / LIT / ADD quads of a tinted skin (pulse of the grade plan for a worn relic), (4) nebula, (5) halos. One texture cache (`tex`), cleared by `clearCustom` on the one reload path in `WornLibClient`. The God worn per-skin buffer map was removed.

**One LOD rule** for both slices (`UpgradeCaps.useLod / additive / rich`): the LOD model beyond 12 blocks and at the low quality; additive passes (glow map, lit glow, motes, rune ring, shell) only up to 24 blocks; nebula and halos only up to 24 blocks and not at the low quality. A God worn relic draws its catalog `lod` pieces (`Ensemble.lodPieces`), a Charms / Genesis model draws the lod named in its file (`WornMesh.lod()`). An upgraded ensemble (either slice) counts at most 2600 vertices against the effect budget of its wearer.

Behaviour change for Charms / Genesis: at the low quality the lod models are now used (before: only the nebula and halos were dropped). Costs less, never more.

## One lookup, one cache, one switch

* Order (`UpgradeSource.choose`): the Charms / Genesis override table (`visual_upgrade/overrides.txt`), then the God worn catalog (`visual_upgrade/catalog.txt`), then the older catalogs. Both data sets are kept. No relic is in both (test).
* `WornLibrary` and `HandForms` keep one cache each, rebuilt when `VuModels.generation()` changes (the switch flips or the resources reload). The God worn two-map cache was removed. An upgraded model that cannot be loaded falls back to the 1.101 look.
* The switch is `visual.upgrade` in `config/nusmp-visual-charms-client.toml` (default true). `RelicFxOptions.upgrade()` reads it; the second switch God worn had added to `nusmp-relicfx-client.toml` (never released) was removed.
* Held forms: an override entry first (also sets the hold style and size), else the God worn `held` pieces, else the older hand catalog.
* Caps: each slice keeps its own table (`caps_charms_genesis_worn.txt`; the God worn row of `caps.py` / `UpgradeCaps`); `UpgradeCaps.lookup(slice, grade)` is the one place that answers, and a test ties it to both files.
* Acts: `RelicEngine` calls `UpgradeSignatures.afterAct` (God melee), `UpgradeActFx.after` (God worn, only for its relics) and `VuActVfx.fire` (Charms / Genesis, only for its relics). Self-tests `visual.god_worn` and `visual.charms_genesis_worn` are both registered. `gen_worn_all.py` runs both generator hooks.

## NEW / REPLACED / KEPT

| | what |
|---|---|
| NEW | `UpgradeSource`, `WornMesh.tinted/skinPath/glowPath`, `UpgradeCaps.lookup/useLod/additive/rich`, `UpgradeMergeTest`, this page |
| REPLACED | God worn flag numbers (8 > 64, 16 > 128) and its models' flag fields; its second `visual.upgrade` switch and per-skin draw buffers and two-map caches (by the shared ones) |
| KEPT | every model, skin, table, id, stat and act of both slices; the released Charms / Genesis flag numbers; both data sets and both self-tests |

## Not verified

Nothing was run in a game. The merged `customPasses` (tinted skins in the same pass code as the painted ones) is type-checked and its decisions are unit-tested, but the real draw, the look of the God worn glow under the shared pass order and the frame cost were not seen.
