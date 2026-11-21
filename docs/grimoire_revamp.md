# Grimoire revamp: model and effects

What this is, in plain words. The held and floating grimoire is now a thicker, better made 3D book, and a cast or summon has a
rune ring, a glow behind the pages, drifting page motes and a flare. Nothing about what a grimoire does has changed.

## What you see

**The book (model).** A layered, bevelled cover (board, raised frame ring, plateau), a metal corner cap on all four corners of both
covers, a rounded spine with five metal bands, a ribbon bookmark, a page block with visible page layers, and a raised metal
plate (front) or rosette (back) that carries the per-attribute emblem. The open book keeps the same hinge, so the open halves turn
the same way as before. A tattered book (Asta's five-leaf) uses a worn leather variant of the same atlas and has no frame ring.
Colours still come from the magic: the cover tint, trim tint and emblem tint are unchanged.

**The effects (client only).** While a grimoire floats open beside its owner:
- a soft glow halo behind the pages (brighter while held),
- a slowly turning rune ring (two counter-turning rings) in the magic's colour,
- one page mote about every 8 ticks while fully open,
- 14 motes once when the book finishes opening (summon),
- a flare on every cast: the ring swells and brightens for 16 ticks and 4 (chant), 8 (release) or 12 (signature) motes fly out.
The motes use the per-magic particle theme (`FxThemes.SKILL_GRIMOIRE`) and a per-magic slot (flame sparks, water and earth are soft
and heavy, wind is the accent voice, anti-magic crackles).

## Settings

| Setting | File | Side | Default |
|---|---|---|---|
| `visual.classicGrimoires` | `nusmp-grimoire-client.toml` | client (looks only) | `false` |

`true` restores the old look completely: the old box-built book and the old summon look, with none of the new ring, halo, motes or
flare. It is a client setting, so every player chooses for themselves; it changes nothing in the game rules.

## New, replaced, kept

- **NEW:** the revamp model (`tools/blender/grimoire_revamp.py`, mesh `assets/anime_multiverse/grimoire_revamp/revamp_mesh.txt`,
  atlases `textures/item/grimoire_book/revamp_atlas*.png`), the plan `GrimoireRevampPlan`, the effect layer
  `client/grimoire/GrimoireRevampFx`, the numbers `GrimoireRevampMath`, the halo texture, the setting, the self-test
  `visual.grimoire_revamp`, the test `GrimoireRevampTest`.
- **REPLACED (by default):** the look of the held and floating grimoire book. The item ids, the hold and hover anchors and the
  inventory icons are the same.
- **KEPT:** the classic plan `GrimoireBookPlan` and all its textures (used when `visual.classicGrimoires` is on, and for far owners),
  every per-magic bespoke effect (Time Magic and the rebuilt books are not touched), the charter grimoire JSON models, all item ids
  and data components.

## Cost (worst case)

- **Model:** closed book at most 420 quads (budget `MAX_CLOSED_QUADS`; real worst case about 310). Open book at most 760 drawn
  quads (budget `MAX_OPEN_QUADS`; the open book is drawn double-sided, so about 2 x 272 = 544). The classic closed book was 94 quads.
  Quads are baked once per look and cached, so this is a per-frame draw cost, not a per-frame build cost.
- **Effect layer:** 6 translucent quads per frame per floating book (3 two-sided: halo plus two rings), no allocation per frame beyond
  the existing pose work, profiled in `PerfProbe` section `c.grimoire.revamp`.
- **Particles:** 14 once on summon, at most 12 per cast, one per 8 ticks while idle-open; a flare restarts at most every 6 ticks. All go
  through the `Fx` facade and its existing budgets.
- **Distance:** owners within 24 blocks get everything. From 24 to 64 blocks the owner's book is drawn with the cheap classic model
  and no effects. Beyond 64 blocks it is not drawn. Your own book is always full detail.
- **Network and server:** zero new packets and zero server work. The cast flare is triggered by the body animation that already goes
  to nearby players (`CastAnimPayload`), through one hook in `CastAnimClient`.

## Checks

Pure tests: `GrimoireRevampTest` (13 tests: mesh parses and garbage is refused, every look inside the item cube and under budget,
atlases 512 px and every quad lands on an opaque texel, classic plan untouched, tiers, flare, bursts, source pins for the setting, the
LOD and the facade), plus the existing `GrimoireBookPlanTest`, `PerfSourceScanTest` (the new layer is on its hot-render-file list),
`FxSourceScanTest` (no vanilla `ParticleTypes` in new code), `ServerSafetyTest`, `ConfigFileNamesTest`, `TextureAlphaTest`,
`ParticleThemesTest`. CI server smoke test: self-test `visual.grimoire_revamp` (added to `SMOKE_EXPECT`).

## How it was made

The model was made in Blender (the `bpy` module, Cycles bake): real solids with bevels, an unwrap, a baked 512 px grey atlas that the
cover, trim and emblem tints colour in game, and a worn-leather variant on the same UVs. It is not a script that places code boxes.
It was not Blockbench. The previews in `docs/previews/grimoire_revamp/` are from a small software rasteriser
(`tools/grimoire_revamp/preview.py`) fed by the real plan, so they are an approximation of the game: no bloom, no particles (the cast
flare shows the halo and ring only).

## Not included / not checked

- Not seen in a running game. Nothing was launched; the first look in game is the owner's.
- The charter grimoire JSON models, the Julius drum and the boss-book effects are not changed.
- Per-magic bespoke VFX (Time Magic and the rebuilt books) keep their own effects; they are not replaced.
- Gradle was not run; the changed Java was compiled with plain `javac` and the pure tests run with the JUnit console.
- Weapon engravings and Better Combat files do not apply (a grimoire is not a weapon).
