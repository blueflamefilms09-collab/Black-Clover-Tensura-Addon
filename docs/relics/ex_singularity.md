# EX-Singularity: the relic grade above Genesis

The owner played 2.0.0 and saw Shishou's tooltip say `★★★★☆☆☆ Unique Grade` over a yellow `Akashic Relic - Grade: EX-Singularity`, while Genesis (`★★★★★★★ Genesis Grade`) was the rainbow
one. This change makes **EX-Singularity** a real top tier of the relic grade system, above Genesis, with a look that is plainly a step up from Genesis's pale rainbow, and gives it to
Shishou (the only item that carries it).

Everything that is **a number or a rule** is server-safe common code (`relics/Grade`, `relics/ExSingularity`, `relics/GradeDamage`, the roll guards). Everything that is **a look** is
client code in `client/` behind client options (rule 9). Previews are MOCKS (see the end).

## NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | `Grade.AKASHIC` (shown "EX-Singularity", tier 8), appended at the END of the enum; `Grade.rollable()` (false for it, weight 0) |
| **NEW** | `relics/ExSingularity` (pure palette + budgets), `relics/ExSingularitySelfTests` (self-test `relic.ex_singularity`) |
| **NEW** | Tooltip style: nine-star run `✦★★★★★★★★★✦` + "EX-Singularity Grade", per-glyph full-spectrum text with a travelling white glint, spectrum hairlines, circling two-hue border, deep violet glass (`client/ui/tooltip/PrismText`, `UnifiedTooltip`) |
| **NEW** | `UnifiedTooltipRules.Category.AKASHIC_RELIC` ("Akashic Relic"), appended |
| **NEW** | Inventory / hotbar slot frame: a rainbow ring that turns round the item, a counter-turning inner ring, a faint sheen, one twinkling corner (`client/ex/ExSingularityClient`) |
| **NEW** | Sword look: glow walks the whole spectrum on a tint-neutral glow skin; iridescent blade veil with a bright band running up the blade; eight orbiting prismatic shards (`client/ExSingularityFx`); same on the wild entity |
| **NEW** | Prismatic motes (particle theme `relic.grade.AKASHIC`) round the carrier, the sword's body and the sword on the ground |
| **NEW** | Client options `nusmp-ex-singularity-client.toml` (`exSingularity.prismatic`, `.slotFrame`, `.particles`), all default on; off restores the 2.0.0 look |
| **NEW** | Textures `ex_singularity_star.png`, `shishou_prism_glow.png` (+ `tools/gen_ex_singularity_assets.py`); preview tool `tools/relics/preview_ex_singularity.py`; lang fragment `tools/lang/ex_singularity.json` |
| **NEW** | `GradeDamage` range for the grade: 420 to 560 (above Elsdocia's own 400); `Grade.epBonus` 0.20 |
| **REPLACED** | Shishou's tooltip grade presentation: it was Unique (pink, 4 of 7 stars, plain yellow line); now EX-Singularity. The item's own lines (`shishou.grade` and the rest) are kept; the yellow line is drawn as the prismatic line instead |
| **REPLACED** | Shishou's sword glow colour and glow skin in hand and in the world: the one cyan pulse is now the cycling spectrum (the cyan skin is kept for the option-off look) |
| **KEPT** | All seven old grades, their ordinals (saves), weights, stars, colours, EP bonuses, damage ranges; Genesis's tooltip and glow exactly as before; every relic's grade (nothing re-graded); the tier-ascension system (still capped at Genesis: `TierRules.MAX_TIER = 6`); Shishou's stats, skills and numbers (not touched); the Time Magic VFX (not touched) |

## The grade system: what was found and what changed

* `relics/Grade.java`: enum, ordinal = order (saved: `TierComponents.TIER` stores 0..6, nothing stores a Grade ordinal above 6). Only `AKASHIC` is appended.
* Exhaustive switches/arrays over `Grade`: none exist (the switches named `grade` in the tree belong to other enums: `StoneTable.Grade`, `GearGrade`). Places that use `Grade.values()` or
  `tier()` only see relic defs (none has AKASHIC): `RelicLook`, `Tiers` (`TierRules.clampTier` caps at 6 so a stack can never be raised into it), `ForgeScreen` (bounds-checked), `RelicCommands` (the
  suggestion list now offers `ex-singularity`; the "unknown grade" message lists it), `GradeDamage` (table extended to 8, clamped to the top).
* Never rolled: `RelicLoot.candidates` (the one place every vault, chest, mimic and wave pick goes through), `DecennialReward.candidates` and `DecennialRewards.catalog` skip
  `!grade.rollable()`; the loot weight is 0; `Grade.ofCard` never returns it; the relic wave files never name it (`ExSingularityTest` scans for that and for datapack loot entries naming it).
* Particle theme: `ParticleThemesTest` demands a `relic.grade.<NAME>` theme for every Grade constant, so the theme is `relic.grade.AKASHIC` (the brief said `grade.ex_singularity`; this is the name the existing
  rule needs), added to `tools/particles/gen_themes.py` and regenerated into `particle_themes.json` (one appended block).

## What the player sees

**Tooltip** (hovering Shishou; `UnifiedTooltip` + `PrismText`)
* name: every glyph its own hue, rebuilt only when the 50 ms colour step changes;
* grade row: `✦★★★★★★★★★✦  EX-Singularity Grade`, drawn glyph by glyph, full saturation (HSV 0.78), the hue turns once in 5.2 s and spreads over about 18 glyphs, plus a white glint that sweeps the row every 1.6 s
  (Genesis's row is one pale hue, HSB saturation 0.35); a hairline under it of eight spectrum segments;
* `Akashic Relic - Grade: EX-Singularity`: the item's own yellow line, replaced in place by the same prismatic drawing (the item's lang key `shishou.grade` is untouched);
* type row `Type: Akashic Relic` (new category), prismatic hairline above the footer, glass `0xF2160C2E`, border a two-hue gradient (a third of a turn apart) that circles every 7 s.

**Inventory / hotbar slot** (item decorator, only for the Shishou item): outer ring of 16 fills (six 3 px pieces top and bottom, two gradient fills per side) whose hue runs once round the slot and turns every 6 s; inner ring of
4 softer strips turning the other way; a 1.5 % sheen over the sprite; one corner at a time twinkles with a plus-shaped glint. One managed draw (`drawManaged`), 23 fills at most.

**Sword in hand / in the world / on the ground** (`WeaponRenderer`, `ShishouMeshDraw`, so the wild entity is identical):
* the glow pass walks the whole spectrum (one turn in 10 s, slow pulse) using a tint-neutral copy of the glow skin (`shishou_prism_glow.png`, made from `shishou_glow.png`: the cyan map could only ever give dim cyan-ish
  reds and greens);
* the blade veil: the 453 quads of the upper blade drawn once more, additive, with the blade's own skin, one colour per quad (the spectrum laid along the blade, 0.8 of a turn guard to tip), dim with a bright band running up the blade every 3 s: the silver
  shimmers like oil on water;
* eight shards (four-pointed stars) orbit the blade, each its own hue, twinkling;
* beyond 24 blocks none of the extras draw; a dormant sword gets only the cycling glow (no veil, no shards), a Stealth-veiled one none of the extras.

**Particles**: a mote every 4 ticks (5 a second) from each carrier (main or off hand), from Shishou's body and from the sword on the ground, theme `relic.grade.AKASHIC` tinted with the glow's hue, through `Fx` (so the
client particle budget and the effect quality apply).

## Power

The grade's numbers are data, not code paths that existing items use: `GradeDamage` range 420 to 560 (only used for a relic definition that names the grade; none does), `Grade.epBonus` 0.20 (worn relics only),
loot weight 0. Shishou's own attack and level scaling live in `ShishouRules` / `ShishouConfig` and were deliberately not touched (the stats builder owns them); if the owner wants them moved to the grade table that is a one-line
change there.

## Worst-case cost (rule 11)

| Where | Per frame / second | Bound |
|---|---|---|
| Tooltip | one `drawString` per glyph of the three prismatic rows (at most 64 each, a longer row is one draw) plus 16 hairline fills; the name's 7 components rebuilt at most 20 times a second; glyph tables built once per tooltip | only while Shishou is hovered |
| Slot frame | 23 fills in one managed draw per decorated slot | at most 4 slots a frame (counter reset by the frame clock); the decorator is registered for that one item |
| Sword (hand or entity) | one extra buffer pass of 453 veil quads (1 812 vertices) + 8 shards x 4 quads (128 vertices) in a second buffer; the glow pass is the existing one | a Genesis weapon draws its 971 glow quads twice (1 942); this draws them once plus 485 = 1 456: fewer. Skipped beyond 24 blocks. No allocation per frame: scratch vector, ints, floats; the veil table is built once per mesh |
| Particles | 5 a second per carrier; carriers looked at only within 48 blocks, at most 12 players per tick, one entity check per tick | goes through the `Fx` client budget and LOD |
| Packets / server | none; no new server work per tick (grade checks are enum reads) | |

## Exact edits to existing files (all additions unless noted)

* `relics/Grade.java`: constant `AKASHIC` appended; `rollable()`; `stars()` returns `ExSingularity.STARS` for it; two doc lines.
* `relics/GradeDamage.java`: range `{420, 560}` and name `akashic` appended; aliases `ex-singularity`; `range(int)` clamps to the table length instead of 6.
* `relics/RelicLoot.java`, `labyrinth/mega/flow/DecennialReward.java`, `labyrinth/mega/decennial/DecennialRewards.java`: one guard line each (`!grade.rollable()`).
* `relics/RelicCommands.java`: the "unknown grade" message lists `ex-singularity`.
* `relics/RelicSystem.java`: one line registering `ExSingularitySelfTests`.
* `tooltip/UnifiedTooltipRules.java`: `Category.AKASHIC_RELIC` appended, `Style.akashic()`, one line at the top of `classify`.
* `client/ui/tooltip/UnifiedTooltip.java`: parts `PRISM_LINE`, `HEAD_PRISM`; name, grade line, head, border, hairlines for the AKASHIC style (the other styles take the old paths unchanged).
* `client/WeaponRenderer.java`: three one-liners (glow colour, glow texture, the extras call) guarded by `ExSingularityFx.applies(id)`.
* `client/ShishouMeshDraw.java`: the glow colour/texture and one extras call guarded by the option (a Shishou-owned draw bridge; the Shishou builder's files were not otherwise touched).
* `client/relics/RelicClient.java`: one line `ExSingularityClient.init(modBus)`.
* `NUSMP.java`: one line registering `nusmp-ex-singularity-client.toml`.
* `tools/lang/unified_tooltips.json`: one key (`tooltip.nusmp.unified.cat.akashic_relic`): `UnifiedTooltipRulesTest.langFragmentCoversEveryCategory` demands every category key in that fragment.
* `tools/particles/gen_themes.py` + `data/nusmp/particle_themes.json`: theme `relic.grade.AKASHIC`.
* Tests: `GradeTest.theOrderIsTheTensuraOrder` (7 became 8 grades, `g[7] == AKASHIC`): the only existing test that had to change.

New files: `relics/ExSingularity.java`, `relics/ExSingularitySelfTests.java`, `client/ExSingularityFx.java`, `client/ex/ExSingularityClient.java`, `client/ex/ExSingularityOptions.java`,
`client/ui/tooltip/PrismText.java`, `tools/gen_ex_singularity_assets.py`, `tools/relics/preview_ex_singularity.py`, `tools/lang/ex_singularity.json`, the two textures, `src/test/.../relics/ExSingularityTest.java`, this doc, the previews.

## Tests

* `ExSingularityTest` (pure; 16 tests): grade order and saved ordinals, never rolled (including a scan that no relic wave and no datapack loot entry names the grade), star run, damage table, hsv known colours, text palette
  bounded / moving / full spectrum / glint / more saturated than Genesis, glow loop and slowness, veil gradient and travelling band, frame and border bounds and the one-twinkle-at-a-time rule (the draw budget relies on it), shard orbit
  bounds, Shishou resolving to the Akashic style at every rarity, lang fragments, the two textures (premultiplied, nothing under alpha), the particle theme, the hook wiring and option registration, and two source scans
  (the pure class and the draw methods allocate nothing and build no strings; no per-tick world scan).
* Existing suites run and green: `GradeTest`, `GradeDamageTest`, `UnifiedTooltipRulesTest`, `ParticleThemesTest`, `SafeAdditiveSourceScanTest`, `SafeAdditiveTextureScanTest`, `SquareBugAssetsTest`, `PerfSourceScanTest`,
  `ServerSafetyTest`, `ConfigFileNamesTest`, `ShishouAssetsTest`, `ShishouSourceScanTest`, `AttuneGradeRuleTest`, `RelicPowerTest`, `RelicChecksTest`, `RelicUniqueLooksTest`, `RelicExclusivityMegaTest`, `TierRulesTest`.
  (`MagicEnumAuditTest` cannot link in this checkout's stub environment, with or without this change.)
* Self-test (CI server smoke): **`relic.ex_singularity`** (grade order, never rolled, no registered relic carries it, Shishou's registered item resolves to the Akashic style, damage table, palette ranges).

## Previews (`docs/previews/ex_singularity/`) and how they were made

The game cannot be run here, so these are mocks from the same numbers (the Python palette is checked line for line against values printed by the compiled Java class at the top of `tools/relics/preview_ex_singularity.py`):
`tooltip_compare.png` (Shishou in 2.0.0 / Genesis / Shishou now), `tooltip_phases.png` (4 phases), `slot_frame_phases.png` (no frame + 4 phases), `sword_phases.png` (2.0.0 + 4 hue phases, two views, real mesh and skin).
Regenerate with `python3 -B tools/relics/preview_ex_singularity.py`. The tooltip is drawn with Unifont (letter widths differ from the game's font); the sword raster is the repo's headless preview renderer; the shard and veil blends are approximations of the game's.
Iterations: tooltip 2 rounds (hairline made a spectrum), slot frame 3 rounds (blocky 8-colour ring replaced by a smooth gradient ring, inner ring alpha raised), sword 4 rounds (cyan glow could not take hues, so a neutral glow skin; white-out fixed by lowering the glow value;
the halo pass was replaced by the blade veil because the veil reads far better and costs less than Genesis).

## Not verified / honest caveats

* **Nothing was seen in a running game.** Not seen: the real font's glyph widths and whether `✦` renders in the default font (it is in Unifont, which Minecraft falls back to; the stars `★` already worked for Genesis); the exact additive blend of the veil on the real blade
  (it can look stronger or weaker than the mock; the knobs are `ExSingularity.veilRgb` value 0.16 + 0.50 and `VEIL_FROM`); that the item decorator draws above the sprite in every GUI (it uses `RenderType.guiOverlay`, like the durability bar); first-person
  veil/shards at the first-person scale; the ambient motes' look (theme particles `star4 glint gem_sparkle dream_star`).
* Classic-look tooltip (UI theme anime off) keeps the prismatic text but not the fills; not mocked.
* The hotbar's floating item name still uses Shishou's own name colour (`ShishouItem.NAME_RGB`, in the Shishou builder's file; it is common code and cannot reach the client text helper). One line there would fix it if wanted.
* The brief asked for a theme named `grade.ex_singularity`; the existing theme test needs `relic.grade.AKASHIC`, which is what was added.
* Shishou's attack numbers were not moved onto the grade table (see Power).
* `tools/lang/ex_singularity.json` holds the prismatic line's text (`nusmp.ex_singularity.line`, the tooltip reads it, falling back to Shishou's own `shishou.grade`); the category label is in `unified_tooltips.json` because a test requires it there. The generated `en_us.json` was not refreshed (lead's merge step).
* Decision for the owner: whether other items should ever carry EX-Singularity (add their registry path to `ExSingularity.AKASHIC_PATHS`).
