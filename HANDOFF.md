> **Superseded in 0.21.0:** the procedural look described below was replaced by the art-based grimoire (see README "0.21"). The classes, textures, legacy models and commands named here no longer exist.

# Handoff: procedural 3D grimoires (nusmp 0.20.0)

Mod: "Multiverse of Anime in Tensura", id `nusmp`, NeoForge 1.21.1 (built against 21.1.234, user runs 21.1.250), Tensura 2.0.1.x, ManasCore 4.0.0.2.
Previous version 0.19.0. Standing rule from the user: **never delete or remove anything; add the new, keep the old** (the old model is `models/item/grimoire_legacy.json`, all 420 old variant models and old textures are untouched).

## What was asked
Replace the flat per-variant grimoires with 3D, procedurally generated ones (103,000 looks from ~30-40 greyscale textures, tint layers 0-3, dynamic geometry with an LRU quad cache, `fromSeed` / `fromGrimoireId`), then make them as **Black Clover canon accurate** as possible.

## State: done, builds, tests green
`gradlew.bat build` passes (22 tests: 10 appearance, 8 geometry, 1 preview, 3 pre-existing VFX). Jar: `build/libs/multiverse-of-anime-0.20.0.jar`.
**Not verified in-engine**: the dev client boots to the main menu with our mod loaded and no errors, and the model bakes, but nobody has looked at the books in an actual world. Display transforms for hand / third person / item frame in `models/item/grimoire.json` are reasoned, not seen.

## Where things are
| Thing | Path (under `src/main/java/com/newuniverse/nusmp/`) |
|---|---|
| Look record, codecs, generator, owner look, canon mapping | `grimoire/GrimoireAppearance.java` |
| Data component `nusmp:appearance` | `grimoire/GrimoireComponents.java` |
| Enums (append-only, ordinals are on the wire): crest, cover, trim, thickness, clasp, affinity | `grimoire/InsigniaType, CoverMaterial, TrimMetal, Thickness, ClaspType, AuraAffinity.java` |
| 33 sprite ids | `grimoire/GrimoireSprite.java` |
| Geometry planner (pure data, unit-tested) | `grimoire/GrimoireModelPlan.java` |
| Cache key (colour is NOT in it; `held` is) | `grimoire/GrimoireRenderKey.java` |
| Magic -> cover/aura colours | `grimoire/MagicPalette.java` |
| Creative-tab showcase looks | `grimoire/GrimoireShowcase.java` |
| Client: loader, baked model + 512 LRU, quad baker, colour handler | `client/grimoire/*` |
| Wiring | `NUSMP.java` (registers component + client init), `blackclover/GrimoireItem.java` (attaches component, migrates old stacks in `inventoryTick`, tooltip), `NUCreativeTab.java`, `blackclover/GrimoireCommand.java` (`look`, `lookseed`), `NUConfig.java` (`[black_clover] uniqueLooks`) |
| Model + textures | `src/main/resources/assets/nusmp/models/item/grimoire.json` (loader `nusmp:grimoire`), `textures/item/grimoire3d/*.png` (33 files) |
| Texture generator | `tools/gen_grimoire_textures.py` (Pillow + numpy; deterministic) |
| Tests | `src/test/java/.../grimoire/` (`GrimoirePreviewTest` is a dev tool, see below) |

## How it works (short)
1. Every grimoire stack carries a `GrimoireAppearance` (10 fields, 16 bytes on the wire).
2. Natural grimoires: `fromCover(cover, magic)` (cached) and, when bound, `forOwner(cover, magic, uuid)` (stable per owner; only border style of common books, thickness and a small colour drift vary). Old stacks get the component on first server tick.
3. Generator: `fromGrimoireId(0..102999)` = id * 31337 mod 107,520 (bijection, no collisions), decoded mixed-radix into crest x cover x metal x thickness x clasp x affinity. `fromSeed(long)` hashes to an id. NOTE: `TrimMetal.NONE` was appended, so combos are now 125,440 and the id -> look mapping changed vs the first build of 0.20.0 (nothing released).
4. Client: `GrimoireBakedModel.Overrides.resolve` -> `GrimoireRenderKey` -> LRU of baked quad lists; one-entry identity memo avoids allocations. `View.applyTransform` returns the **held** variant (adds the glow) in first/third person hands.
5. Tint layers: 0 cover, 1 trim / gilded border, 2 crest, 3 emissive aura.

## Canon rules implemented (from research, see sources)
- Kingdom insignia centred on the front cover; binding colour = owner's magic.
- Three-leaf: common, plain leather, simple border, gold clover. Four-leaf (Clover only): gold clover + gilded ornate border (`trim_border.png` overlay), gold trim (Yuno: gold and green). Corrupted four-leaf = five-leaf: darker cover (x0.55), dark ornaments, black clover. Anti-Magic five-leaf = Asta's: tattered, filthy leather, black clover.
- No straps / chains / locks / corner caps / spine bands on natural grimoires. Glow only while **held** (canon: slight glow while floating in use). Boundless crest = Julius' coverless book.
- Clasps, chains, padlock, dragon hide, runed trim and the full aura (circle + sparks) exist only for generator ids and the creative showcase.

### Not canon / unverified (be honest about these)
- Spade / heart / diamond kingdom books: only "suit symbol on the front cover" is confirmed. Suit colours (silver spade, pink heart, blue diamond) are invented. Double/triple spade, two-heart, tiered heart, cracked covers, five-sided diamond are the mod's own inventions (kept, per the no-removal rule), as are the Forbidden Runes crest.
- Gold colour of the **three-leaf** clover, grimoire thickness, page colour, cover ornament per owner (e.g. Charmy's crossed knife and fork) are not modelled / not confirmed.
- Research was thin: the main wiki (blackclover.fandom.com) returned HTTP 402 to fetches, so details came from search summaries. Sources: [Asta (Black Clover), Wikipedia](https://en.wikipedia.org/wiki/Asta_(Black_Clover)), [Grimoire System explained, CBR](https://www.cbr.com/grimoires-black-clover-explained/), [What exactly are grimoires, GameRant](https://gamerant.com/black-clover-what-exactly-are-grimoires/), [Grimoire, Black Clover wiki](https://blackclover.fandom.com/wiki/Grimoire) (via search snippets), [Four-Leaf Grimoire, FandomWire](https://fandomwire.com/the-four-leaf-grimoires-twisted-nature-in-black-clover-was-subtly-hidden-by-yuki-tabata-in-its-good-luck/).

## How to build / test / preview
```
gradlew.bat build                                  # compile + all tests + jar
python tools/gen_grimoire_textures.py              # regenerate the 33 textures
GRIMOIRE_PREVIEW_DIR=build/preview gradlew.bat test --tests "*GrimoirePreviewTest"   # contact sheets (software renderer, same quads/textures/tints/gui transform)
```
Dev client: `gradlew.bat runClient` fails with "Mod tensura requires terrablender 4.1.0.0" because this project's `build.gradle` has no TerraBlender. A local copy of the user's `TerraBlender-neoforge-1.21.1-4.1.0.8.jar` was dropped in `run/client/mods/` (untracked dev output) and then the client boots. Adding TerraBlender as a dev dependency to `build.gradle` was not done (not asked).

## Open issue the user reported
User said "no items in JEI, none of the commands, nothing from 0.20.0". Investigation: the only recently run profile is Modrinth `TESTING` (NeoForge 21.1.250, Tensura 2.0.1.3, JEI 19.57) and **its `mods` folder does not contain the nusmp jar**; its `latest.log` has no `nusmp` line at all. The only copy of the jar on the machine was `build/libs/`. The mod loads cleanly in the dev client. Most likely the jar was never placed in the profile. Not yet resolved: ask the user to put the jar in that profile's `mods` folder and, if it still fails, send `logs/latest.log` / newest crash report. Do not guess further without that log.

## Suggested next steps
1. Get the user's in-game result (or log) for the jar in the right profile; verify items, `/nusmp ...` commands, creative tab, JEI.
2. Eyeball hand / third-person / item-frame / ground transforms in `grimoire.json` and adjust (these are the likeliest thing to look off).
3. If the user wants more canon: per-magic cover motifs, confirm spade/heart/diamond designs from the series art, decide whether to keep the invented cover variants, optionally make Time-magic's Julius-style book coverless.
4. Optional: add TerraBlender to `build.gradle` runtime so `runClient` works out of the box.

## Leftovers on disk (generated by testing, safe to delete by the user, not by an assistant under the standing rule)
`nusmp/run/` (dev client output + the copied TerraBlender jar), `nusmp/build/`, `nusmp/.gradle/`.
