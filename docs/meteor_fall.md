# The shared meteor (`meteor_fall`)

Owner request: "Meteor should summon like an actual sort of meteor model that comes crashing down, and it should apply to anything that has a meteor like ability."

## What it is, in plain words

One real falling rock for every meteor ability. It appears high in the sky, tumbles and glows, drags a tail, comes in at about 30 degrees from the vertical, is slow at first and fast at the end, and when it hits the ground the ability does what it always did (damage, effects, picture). No crater, no fire, no block is ever changed.

Not included: block damage, fires, a new sound pack, a per-ability model (the six styles share one rock and one tail model, only the textures differ), the Ultimate engine and tier-boss engine meteors (see "Left alone").

## API (server only)

```java
MeteorFall.drop(caster, impact, spec)            // true = a meteor is in the air, false = nothing was made
MeteorFall.dropOr(caster, impact, spec, oldCode) // false => oldCode runs now (setting off, caps, bad point)
MeteorWiring.spec("<system>.<ability>")          // look, size, radius and usual fall of a wired ability
MeteorSpec.of(style).radius(..).damage(..,type).size(..).fall(ticks).knockback(..).onImpact((level, at, caster) -> ..)
```

Styles (`MeteorStyle`, append only): fire, ice, dark, ki, holy, star. A landing runs the caller's `onImpact`, and if the spec has damage the generic landing hurts foes in the radius (never the caster, allies, own pets, creative players; nearest 24 only; damage falls off to half at the edge). With no `onImpact` it also plays one explosion sound and one existing impact effect of the style (`FIRE_BURST`, `DEMON_ICE_FX3`, `KI_SHOCKWAVE`, `MAGIC_CIRCLE_EXPLOSION`).

`drop` returns false and makes nothing when: `meteor.realMeteors` is off, the caster is not on a server level, the caster has `maxPerCaster` meteors up, the server has `maxGlobal`, the point is not finite or is further than 160 blocks from the caster, or its chunk is not loaded. Every caller keeps the code it ran before, so a refused meteor never loses the ability.

## Settings

| File | Key | Default | Meaning |
|---|---|---|---|
| `nusmp-meteor-server.toml` | `meteor.realMeteors` | true | false = every ability does exactly what it did before (no entity) |
| | `meteor.maxPerCaster` | 4 (1..8) | meteors in the air per caster |
| | `meteor.maxGlobal` | 24 (1..64) | meteors in the air in the server |
| | `meteor.maxFallTicks` | 60 (8..60) | longest fall, longer ones are cut |
| `nusmp-meteor-client.toml` | `meteor.trailQuality` | HIGH | OFF / LOW (2 particles a tick) / HIGH (5); model and tail are always drawn |

## How it works (and why it is cheap)

- `MeteorEntity` has five synced values (start, impact centre, fall ticks, style, size). Its position every tick is a pure function of those and `tickCount` (`MeteorRules.position`), computed identically on the server and each client. No movement packets: the only traffic is the vanilla spawn packet, sent by the tracker to players in range. `noSave`, `noSummon`, not pickable, not attackable.
- The server lands it on the tick `tickCount >= fallTicks` (once, removed first so a throwing landing cannot leave a ghost). A meteor still alive 10 ticks past its fall (chunk unloaded, server stall) is discarded without landing.
- Cleanup: all meteors of a player are discarded on logout, death and dimension change; everything is dropped on server stop. Books are pruned of removed entities on every query (at most `maxGlobal` entries).
- Per-player state is keyed by UUID. No client class is touched from common code: the renderer is in `client/meteor/` and is registered only on the client in `MeteorBoot`.

### Worst case cost

- Server: 24 meteors in flight (default) = 24 entities doing one small position computation per tick, no world scans, no chunk loads. One landing = one bounded entity query (radius at most 16, at most 24 targets hurt), up to 3 sounds, and the caller's own effect packet(s) through the existing effect budget. At most one impact packet per landing from this system.
- Network: 24 spawn packets per fall window, nothing per tick afterwards.
- Client: a meteor beyond 112 blocks is not drawn, beyond 64 blocks it makes no trail particles. Draw cost per meteor: 14 rock cubes plus 12 tail cubes = 26 boxes (about 156 quads) and one additive glow pass of the rock (14 boxes). The trail goes through `PerfBudgets.clientParticles` (2 or 5 per meteor per tick, then the global budget cuts it). No per-frame allocation in the renderer (scratch rotation objects).

## Look and model

- Model: `geo/entity/meteor_rock.geo.json` (14 rotated cubes, bone `rock`, 64x64) and `geo/entity/meteor_tail.geo.json` (6 tapering segments of 2 crossed cubes, 128x128). Textures `textures/entity/meteor_rock_<style>.png`, `_glow.png`, `meteor_tail_<style>.png` for the six styles (the fire set also under the unsuffixed base names).
- **Tool really used: neither Blender nor Blockbench could run in the session.** The geo json and a Blockbench-importable `tools/blockbench/meteor.bbmodel` are written by the script `tools/blockbench/gen_meteor.py` (a code fallback, per rule 15, not made in an app); textures are painted by the same script (numpy value noise, Voronoi cracks). `python3 tools/geo_check.py meteor_rock` / `meteor_tail` pass.
- Preview (a small software rasteriser, NOT the game): `docs/previews/meteor/meteor_styles.png` (six styles) and `meteor_fall_scene.png` (fall along the path). Regenerate with `python3 tools/blockbench/gen_meteor.py --preview`.
- The rock spins about a fixed axis (faster when smaller), the tail is additive along the opposite of the velocity, full-bright.

## What is wired

| Id | Style | Radius | Size | Fall (ticks) | Source |
|---|---|---|---|---|---|
| `sword.meteor` | star | 1.8 | 0.8 | 14 (its own delay) | heroweapon/weapon/sword/SwordOps.java |
| `gambler.meteor_of_bones` | dark | 5.5 | 1.5 | 30 | dungeon/nexus/relic/GamblerPage.java |
| `star.meteor_swarm` | star | 2.6 | 1.0 | 18 | book/pages15/Batch1.java |
| `visionary.meteor` | star | 6.0 | 2.4 | 36 | skill/TheVisionarySkill.java |
| `merlin.starfall` | star | 3.0 | 1.2 | 20 | arc/sds/MerlinMoves.java (boss) |
| `demon_ice.black_comet` | ice | 5.0 | 1.8 | 20 | book/pages15/Batch2.java |

Each keeps its old damage and effects: they now run in the meteor's `onImpact`, at the moment it lands. A meteor beyond the per-caster cap (a swarm, Merlin's star count) lands the old instant way via `dropOr`. The old sky streak effects are skipped when a real meteor flies and kept in the fallback.

## Left alone, and why

The table is `MeteorWiring.LEFT_ALONE` (22 entries, each with a reason; a test fails if one has none). Summary:

- Not a fall: Saiyan Meteor Combination / Meteor Strike and Goku's meteor combo (melee rushes), Black Meteorite (a dash and slash), Greed's Piercing Meteor (a flat beam), Mirror's Meteor Dragon (a ray), Rising Meteor (rises from the ground), Comet / Starfall element book (a bolt and a ground field), Skyfall Cleave (a slam), Bifron's Skyfall, Lucius's Judgment of Light (pillars), Dream Falling Stars (an illusion), meteor names that are only words.
- Owner decision, could become meteors: Mirror's Meteorite (a 6 to 14 blade shard rain), Meteor Spear (a lance thrown along a line).
- Shared engines, follow-up: Ultimate "Stellar Rain of Ruin" and "Iron Meteor Anvil" (one engine for 40 ultimates), the tier boss "Falling Star" (shared marked-strike engine).
- Not mine: the Meteor Bow and the meteor/starfall arrow acts (`heroweapon/weapon/bow`), shield "Shooting Star" (shield code).

### Bow seam (for the coordinator)

Do not copy the bow's own meteor. In the bow's meteor act call, from the server thread:

```java
MeteorFall.dropOr(shooter, impactPoint, MeteorWiring.spec("bow.meteor_bow") /* add the entry at the END of WIRED */
        .onImpact((level, at, who) -> <the bow's existing landing>), () -> <the bow's existing landing code>);
```

Add `bow.meteor_bow` to `MeteorWiring.WIRED` (style, radius, size, fall, file) and delete its row from `LEFT_ALONE`; `MeteorWiringTest` then checks the file contains `MeteorWiring.spec("bow.meteor_bow")`. `MeteorWiringTest.theBowCodeIsNotTouched` fails once the bow uses the shared meteor: remove that test method in the same change.

## Tests

- `MeteorRulesTest` (pure: bounds, monotone fall that lands exactly, caps, clamps, NaN).
- `MeteorWiringTest` (reads the repo: callers, reasons, textures and models exist, server-safe and cheap source scan, bow untouched).
- Self-test `meteor.fall` (CI server smoke test, id for the coordinator to add to `smoke.yml`): entity spawn and start height, closer every tick, one landing on the last tick, clean books, cap, cleanup, generic landing hurts only the foe inside the radius, setting-off fallback.
- Also run: `ConfigFileNamesTest`, `ServerSafetyTest` (allow-list `meteor/MeteorBoot.java`), `PerfSourceScanTest`, texture and geo tests.

- 4.73.0: the Meteor Bow technique now drops the shared meteor (`bow.meteor_bow`, BowRain.meteor; the old streak and delayed hit run when the setting is off).
