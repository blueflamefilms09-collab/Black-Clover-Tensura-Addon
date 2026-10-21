# Upgrade particles (release 1.110.0): the mod's own particles, a size cap near the camera, a hard budget

Owner report on 1.109.0: "I said make and use CUSTOM particles, not the Minecraft ones, and even so there are so many it will probably cause lag." The four screenshots show white square
puffs (the white-square render type is another builder's lane and is not touched here), a huge pixelated flame sprite filling the first-person view, and cyan vanilla dust.
**Nobody has seen the result in a game**: nothing here could be run (no Gradle, no game); every number below is read from the files or computed by the pure classes.

## 1. Findings (audit of every particle the upgraded items spawn)

Five client emitters read five kinds of table; the tables hold **829 `particle` tokens in 122 files** (`assets/nusmp/visual_upgrade/*.json` and `fx/*.json`). Before this release:

| token | count | where it went |
|---|---|---|
| `dust` | 168 | **vanilla `DustParticleOptions`** in 4 of the 5 loaders (God melee a, Genesis weapons, worn charms, God melee b when the layer said `dust`) |
| `fx` | 252 | already the mod's own mote (`FxRelic.dust`), God melee b / dark Djinn only |
| `minecraft:end_rod` 69, `minecraft:glow` 50, `end_rod` 37, `minecraft:flame` 32, `minecraft:cloud` 24, `minecraft:witch` 23, `minecraft:enchant` 15, `electric_spark` 13, `minecraft:electric_spark` 12, `minecraft:soul_fire_flame` 11, `smoke`, `snowflake`, `happy_villager`, `crit`, `lava`, ... (409 vanilla names, dozens of distinct ones) | 409 | **looked up in the vanilla particle registry** (`BuiltInRegistries.PARTICLE_TYPE.get(minecraft:flame)`) by `UpgradeFx` (God melee a), `VuFx` (Genesis), `VuFxClient` (worn charms), `UpgradeSpecs.options` (God melee b). Only the worn relic tables (`UpgradeFxClient`) and the act bursts (`UpgradeActFx`) had been moved in 1.108.0, and those two went through `Fx.options(.., FxOld)`, which hands the vanilla particle back whenever `particles.custom` is off |

Hard-coded vanilla in the Java (not in tables): `DemonSlayerVu` (two crimson `DustParticleOptions`), `TinyDjinnFx` / `TinyDjinnLayer` (the Djinn's light as `DustParticleOptions`),
`ElsdociaGlitterClient` (two `DustParticleOptions`), `VuFx` (swing-trail dust, act-burst dust). Audited and already custom (through the `Fx` facade, left as they are): `client/relics/worn/*`
(`ArmorFx`, `WornCues`, ...), `client/relics/fx/RelicFxClient`, the `Fx.add(.., FxOld.POOF ..)` calls of the tiny Djinn.

**Why the flame filled the screen.** The vanilla `flame` sprite is a 16 x 16 pixel picture drawn 0.1 to 0.2 blocks wide in its half-size. The Genesis and God emitters place blade particles
0.2 to 1.6 blocks along the look direction from the hand, i.e. 0.3 to 1 block from the eye in first person, and nothing limited the size of a particle that close: at 0.4 blocks a 0.3 blocks
sprite covers about half the view width, and a few of them overlap. The custom `FxParticle` had the same hole (no near-camera rule), only with smaller sprites.

**Why "so many".** Table rates before, particles per second per held item (computed from the tables, `scratchpad/rates.py`): God melee b and dark Djinn (45 items) **mean 122, max 137**; God melee a
(12) mean 61, max 66; worn relics (24) mean 63, max 93; Genesis (14) mean 35, max 60; worn charms (27) mean 12, max 27. The only limit was the shared per-tick client allowance (400 a tick).

## 2. What changed

* **Every token resolves to a custom particle.** New pure `particle/fx/UpgradeParticleAlias`: `fx`, `dust`, empty -> the hue-themed custom mote dyed with the layer colour (`FxRelic.dust`); a catalog id
  (`flame_tongue`, `nusmp:ember`) -> itself; a vanilla name (all 107 vanilla particle names of 1.21.1 are in the table, with and without `minecraft:`) -> the nearest custom particle
  (`end_rod` -> `mote`, `glow` -> `mote_soft`, `flame` -> `flame_tongue`, `soul_fire_flame` -> `flame_soul`, `cloud`/`smoke` -> `smoke_wisp`, `witch` -> `mana_mote`, `enchant` -> `rune_sol`,
  `electric_spark` -> `electric_orb`, `crit` -> `spark`, `lava` -> `lava_blob`, `portal` -> `void_speck`, ...); anything unknown -> `mote`. It never answers a vanilla particle and never reads the
  vanilla registry. New `particle/fx/UpgradeParticleOptions` builds the `TintedParticleOptions` (dyed with the layer colour when the particle is tintable; the vanilla colour of the particle - a witch
  is purple - only while the layer is white; scale clamped 0.4 to 1.0, it was up to 2).
* **Loaders moved onto it:** `client/vupgrade/godb/UpgradeSpecs.options`, `client/upgrade/UpgradeFx` (God melee a), `client/relics/item/vu/VuFx` and `DemonSlayerVu` (Genesis), `client/relics/vu/VuFxClient`
  (worn charms), `client/relics/fx/UpgradeFxClient` (worn relics), `relics/upgrade/UpgradeActFx` (act bursts, server: also half the count, at most 48 instead of 120), `client/relics/djinn/tiny/TinyDjinnFx`,
  `client/ElsdociaGlitterClient`. `UpgradeParticleNames` (1.108.0) stays, unused by the two classes now.
* **The tables say it too.** `tools/visual_upgrade/custom_particles.py` (alias table `tools/visual_upgrade/particle_alias.json`, kept identical to the Java table by a test) rewrote the 577 vanilla
  and `dust` tokens of 86 tables in place (`dust` -> `fx`, `minecraft:flame` -> `nusmp:flame_tongue`; only the token strings change, the layout is untouched). Seven generators call it right before
  they write a table (`god_melee_a/specs.py`, `god_melee_b/build.py`, `genesis_weapons/build_item.py`, `demon_slayer/ds_build.py`, `djinn_dark_weapons/build.py`, `god_worn/fx.py`,
  `charms_genesis_worn/build.py`), so a re-run keeps the fix. `python3 -B tools/visual_upgrade/custom_particles.py --check` fails while a table holds a vanilla token.
* **Size near the camera.** New pure `particle/fx/ParticleNearCap`, used by `FxParticle.getQuadSize` for every ambient-sized custom particle (profile width <= 0.7; the rings, bursts and flashes of the
  library are exempt): the half-size shrinks smoothly from full at 1.5 blocks to zero at 0.35 blocks from the camera and is never more than 0.14 x its distance (about a fifth of the view height at
  most) nor 0.45 blocks. The upgrade emitters also skip any spawn point closer than 1.2 blocks to the camera (`UpgradeGate.add`), which is what removes the first-person blade spray.
* **Budget.** New pure `perf/UpgradeBudget` + client `client/upgrade/UpgradeGate`, in front of every emitter:

  | rule | value |
  |---|---|
  | `particles.upgradeDensity` (client file `nusmp-particles-client.toml`, `FxConfig.upgradeDensity`) | 0.0 to 1.0, **default 0.5**, a straight multiplier on every table rate (so the defaults are halved) |
  | per held item | 16 / 10 / 4 particles a second at HIGH / MEDIUM / LOW (token bucket, half a second of burst) |
  | per player (all items held or worn) | 28 / 18 / 8 a second |
  | alive at once, all holders | 240 / 140 / 60 (x client allowance / 400, clamped 0.25 to 1.5) over a 30-tick life window |
  | quality | x `VfxQuality.detail` (1 / 0.75 / 0.5) |
  | frame rate | measured from the frames the level renders; full from 45 fps, linear down to 0 at 12 fps, **nothing below 12**; unmeasured counts as full |
  | pause / no level / camera not ready | nothing |
  | somebody else's item | full to 8 blocks, linear to 0 at **16 blocks** (own item: no distance cut); nothing behind the camera (dot < -0.1, full above 0.3) unless within 3 blocks |
  | act bursts / tiny Djinn poofs | `wanted x density x quality x fps`, at most 30 / 20 / 8, under the live cap |
  | still applied afterwards | `PerfBudgets.clientParticles` (the per-tick client allowance) |

  Rates after, per item and second (density 0.5, cap 16): God melee b / dark Djinn mean **16** (was 122), God melee a 16 (61), worn relics 16 (63), Genesis 15 (35), worn charms 6 (12). `VuFx`
  also emits one particle per due layer instead of up to two and three trail steps instead of four. `/nusmp perf report` prints a line (`upgrade particles: live n/cap, granted x of y asked`, fps).
* **Tests changed on purpose:** `UpgradeCoreTest.theParticleBudgetIsTakenBeforeAnythingIsSpawned` and `UpgradeParticleNamesTest` (two tests) asserted the 1.108.0 route (`PerfBudgets` before `level.addParticle`,
  `FxRelic.dust` + `Fx.options`); they now assert the new one (`UpgradeGate.take` before `UpgradeGate.add`, the gate calls `PerfBudgets.clientParticles`; shipped tokens are `fx` or `nusmp:<catalog id>`).

## 3. NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | `particle/fx/UpgradeParticleAlias`, `UpgradeParticleOptions`, `ParticleNearCap`; `perf/UpgradeBudget`; `client/upgrade/UpgradeGate`; `FxConfig.upgradeDensity` (`particles.upgradeDensity`); `vupgrade/UpgradeParticlesSelfTests` (`visual.upgrade_particles`); `tools/visual_upgrade/custom_particles.py` + `particle_alias.json`; JUnit `UpgradeParticleAliasTest`, `UpgradeBudgetTest`, `ParticleNearCapTest`; `tools/lang/upgrade_particles.json`; this page |
| **REPLACED** | the vanilla particle of every upgrade effect (dust, registry lookups, `FxOld` fallbacks that returned vanilla) by the mod's own; the 577 vanilla / `dust` tokens in 86 tables; the table rates' effective values (halved by the density default, capped per item / player / total); the 1.108.0 route of `UpgradeFxClient` / `UpgradeActFx` options |
| **KEPT** | the tables' layer lists, kinds, colours, rates (data unchanged apart from the token strings), every mesh, model, skin, act, engraving; `UpgradeParticleNames` and the `FxOld` enum; the `particles.custom` switch of the facade (it still brings the old look back for everything else; the upgrade effects no longer depend on it); the swing key `particles` (descriptive names such as `soul_wisp`, not read by the game); the white-square render types and the weapon mesh sizes (other builders); the Time Magic VFX |

## 4. Checks run

Type-check against the API stubs (`tc_wt.sh`): only the known stub artefacts, plus one deliberate wrong call as a negative control (caught). JUnit (shim runner): the three new test classes, `UpgradeCoreTest`,
`FxSourceScanTest`, `RukhPurgeScopeTest`, `RukhExclusiveScopeTest`, `UpgradeFxPurgeScopeTest`, `PerfSourceScanTest`, `ServerSafetyTest`, `UpgradeParticleNamesTest` and the table tests of the God / Genesis / Djinn /
charms slices all pass.

## 5. Not verified (honest list)

* Nothing was run in a game or on a dedicated server: the on-screen look (sizes, colours of the aliased particles: a custom `smoke_wisp` for a vanilla cloud, `flame_tongue` for a flame), the feel of 0.5 density,
  and the frame-rate measurement (`RenderLevelStageEvent.AFTER_PARTICLES` counted per second) are untested.
* The alias choices are judgement calls (nearest custom particle by look); some may want a different target. They are one table (`UpgradeParticleAlias`, mirrored in `particle_alias.json`).
* The "white squares" in the screenshots come from render types / textures, which this release does not touch; whether the custom particles that now replace the vanilla puffs look right depends on that fix.
* In first person the held blade's own particles are mostly skipped inside 1.2 blocks of the eye, by design; how much of the effect remains visible there is not judged.
* The generators were hooked (`custom_particles.convert`) and syntax-checked but not re-run end to end (they need the full painter toolchain); the shipped tables were converted by the same module.
* `FxParticle` is shared with the white-square builder; if both edit it, the two edits (`nearCapped`, `getQuadSize`) are small and independent.
* The measured fps counts rendered frames per second of this client only; with a frame limiter below 45 the effects thin out (by design: below 45 fps they start to fade).
