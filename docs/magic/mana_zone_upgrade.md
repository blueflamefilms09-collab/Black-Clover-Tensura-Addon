# Mana Zone upgrade: evasion, buffs and omnidirectional casting

Builder `zoneup`. Self-test prefix for the smoke list: **`magic.zone_up`** (three tests: `magic.zone_up.rules`, `magic.zone_up.buffs_evasion`, `magic.zone_up.cast`).
Audit, bullet by bullet: `docs/magic/zoneup_audit.md`. The base technique and its gate: `docs/mana/mana_zone_redesign.md`.

## What was built, how it is read, what is not included

The mod already had a Mana Zone (sphere that grows to 14..40 blocks, speed bonus, foreign projectiles destroyed from within, foreseen blows cut, pressurised spaces, Omni Volley, footholds).
The owner's spec asked for three things on top, and this change adds only what was missing:

1. **Evasion.** The zone owner reads the flight of a projectile that is about to hit and steps out of its way; the server rolls the dodge (chance, cooldown, a little mana).
2. **Attack and knockback-resistance buffs** next to the existing speed bonus.
3. **Omnidirectional casting.** While the zone is open, the grimoire page you cast is cast FROM THE ZONE, AT the point or foe under your crosshair, born behind the target (or at the zone's edge, or above it), not at your hand. It applies by itself: no keybind, no new page; it works for every existing page that does not act on the caster.

How it is read in play: open a Mana Zone as before. Aim at something inside the zone (a foe, or the ground), and cast any attack page as usual. The spell appears behind/above/around the target and flies at it.
Evasion needs nothing from the player: an arrow that a boss shot at you, which the zone only slows, may now be sidestepped; you get a small shove sideways and a quick afterimage ring.

Not included (said plainly):
* effects a page schedules for LATER ticks (`SpellRuntime.later`) read the caster's real pose again, so the second half of a delayed multi-stage page still comes from the hand;
* pages whose id says they act on the caster (dash, blink, teleport, flight, armour, summon, store/draw, modes, spirit pages, devil union, the zone's own pages) are never zone-cast; any other page that moves the caster is undone after it ran;
* no new visual: the origin gets the existing circle burst, the dodge the existing afterimage burst (so there are no previews for this change);
* nothing could be run in a game here: see "Not verified in a game".

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `ZoneEvasion` (predicted dodge + impact backstop), `mana_zone_attack` and `mana_zone_knockback` modifiers, `ZoneCast` (server), `ZoneCastNet.AimPayload` (client to server), `client/manazone/ZoneCastClient`, `ZoneUpRules`/`ZoneUpMath` (pure), `ZoneUpSelfTests`, the `upgrade` section of `nusmp-mana-zone-server.toml` |
| REPLACED | nothing |
| KEPT | everything of the 1.99.0 zone: radius, speed bonus, projectile destruction/slowing, foresight, ally pressure, bubbles, Omni Volley, Pressure Space, footholds, upkeep, gate, True Magic, Grand Magic Zones. With the switches below off the zone is exactly the 1.99.0 zone |
| Shared files touched (hooks only) | `book/GrimoireBook.java` (castPage wraps the handler; `aim`, `target`, `along`, `vfx` consult the override: 5 small additions), `ManaZoneManager` (buffs on/off, per-owner cleanup, a capped call from the scan), `ManaZoneBoot` (3 registrations), `ManaZoneConfig` (new section), `client/manazone/ManaZoneClient` (one read accessor) |

## Settings (server config `serverconfig/nusmp-mana-zone-server.toml`, section `[upgrade]`)

All gameplay settings are SERVER settings (there is no client setting; the client only looks and asks). Default on.

| Key | Default | Meaning |
|---|---|---|
| `evasion` | true | the dodge; false = the 1.99.0 zone |
| `evasionChance` | 0.6 | base chance; +25% at full Reinforcement mastery; halved against a boss's projectile; max 0.95 |
| `evasionCooldownTicks` | 30 | between two dodges of one owner |
| `evasionCostPercent` | 0.3 | percent of max mana per dodge (free zones and creative pay nothing) |
| `buffs` | true | attack and knockback-resistance modifiers |
| `attackBonus` | 0.15 | +15% attack damage |
| `knockbackResistance` | 0.5 | added knockback resistance |
| `zoneCast` | true | omnidirectional casting; false = pages are cast from the hand |
| `zoneCastOrigin` | BEHIND | BEHIND (past the target as seen from you), EDGE (zone boundary, flying inward), ABOVE |
| `zoneCastRelocate` | true | pose the caster at the origin while the page runs (false: only the pages' aim/target/beam/effect helpers are redirected) |
| `zoneCastGapTicks` | 6 | between two zone-casts of one caster |

Not in the file but fixed and sanitised in `ZoneUpRules` (a config cannot leave these ranges): horizon 6 ticks, 4 projectile looks per scan, hit radius 0.9, sidestep impulse 0.6, aim packet gap 2 ticks, aim lifetime 30 ticks, origin reach 3 blocks, minimum run 2 blocks, zone tolerance 1.5 blocks.

## How it works (mapping of the spec to this mod's real architecture)

* **Spec "RayTrace + packet + server verification".** Client: `ZoneCastClient` (client-only class, started from `ManaZoneBoot` behind `Dist.CLIENT`). While the local player's own zone is open (`ManaZoneClient.ownRadius`, from the synced zone state) and one of Tensura's existing skill slot keys (`TensuraKeybinds.ACTIVATE_SLOT_1..3`, read with `isDown()`) is held, it casts a ray from the camera through the crosshair as far as the zone reaches (a block stops it, a living thing on the way is the target) and sends `AimPayload(x, y, z, entityId)`. No `KeyMapping` is registered.
* **Server.** `ZoneCastNet` registers the packet (`playToServer`, optional) and calls `ZoneCast.onAim` on the server thread with the real sender. `onAim` checks, cheapest first and all against the SERVER's own data (`ZoneUpMath.checkAim`): the switch, the sender owns an open zone, same dimension, finite coordinates (and under 3e7), the per-UUID rate (one packet per 2 ticks), and that the target is inside the zone sphere (the zone's own radius at this tick; centre = the owner's server position; 1.5 blocks of tolerance because the owner moves between checks). A foe id is kept only when that entity is alive, a living thing, inside the zone and within 4 blocks of the claimed point; otherwise only the point is kept. One pending aim per zone owner (a reused object).
* **Which page is active.** The client does not say. The server knows the page being cast at the moment of the cast (`castPage` passes its id); `ZoneUpMath.pageEligible` decides, and the aim is single-use and expires after 30 ticks, so an aim cannot be replayed on another cast later.
* **The cast.** `GrimoireBook.castPage` runs the page handler inside `ZoneCast.around`. With a fresh aim, an eligible page, the zone still open in the same dimension, the target still inside it and the per-caster cooldown (6 ticks) respected, the handler runs with an ORIGIN OVERRIDE: one ray from the target outwards finds the origin (the configured mode, then straight above), stopping 0.6 block short of a wall and needing at least 2 blocks of run; the caster is then posed at that origin looking at the target (position, yaw, pitch, head yaw) and `GrimoireBook.aim / target / along / vfx` answer with the target, the aimed foe and the origin. Everything is restored in a `finally` right after the handler (position, look, velocity; a page that moved the caster is undone) and the client is never told about the pose (no packet), so the existing pages become omnidirectional without being rewritten. If anything fails, the page is simply cast from the hand.
* **Evasion.** `ZoneEvasion.look` is called by the zone's existing scan for the projectiles it already found with its one AABB query and did not destroy (a boss's, or past the scan's budget of 6). The owner examines at most 4 of them per scan. The straight-line flight is projected 6 ticks ahead (`ZoneUpMath.timeToHit`); one that will pass within 0.9 block of the body is rolled ONCE (the last four decided projectile ids are remembered). On success: a sideways impulse (perpendicular to the flight, towards the side the owner already stands on) and `hurtMarked`, an afterimage ring and a sound. `ZoneEvasion.onImpact` (`ProjectileImpactEvent`) is the backstop for what arrives between two scans: the same decision; a dodged impact is cancelled so the projectile flies on through (it stays cancelled while it overlaps the body).
* **Buffs.** Transient attribute modifiers added in `ManaZoneManager.start`, removed in `end` (also when the config is switched off later).

## Worst-case cost (rule 11)

| What | Worst case |
|---|---|
| Entities spawned | 0 |
| Chunk loads / world scans | 0 (`hasChunkAt` is a lookup; the evasion adds no query: it reads the scan's own list; the cast does one entity lookup by id) |
| Client to server packets | a zone owner holding a skill key: at most one per 3 ticks (6.7 per second) plus one at key release; the server drops anything faster than one per 2 ticks (10 per second). 27 bytes of payload. A player without a zone: dropped after one comparison. 100 zone owners all holding a key: about 700 packets per second of 27 bytes, each O(1) |
| Server to client packets | per zone-cast: 1 effect packet (through `VfxSpawn`'s effect-packet budget); per dodge: 1 effect packet, 1 motion packet, 1 sound; at most one dodge per owner per 30 ticks and 4 per server tick (a `PerfBudget`) |
| Server CPU per zone-cast | 1 to 2 rays (origin search), 2 `setPos` (pose and restore), a few vector operations |
| Server CPU for evasion | at most 4 projectile looks per owner per scan (every 4 ticks): a few multiplications each; the impact hook returns at once when no zone is open |
| Memory | per zone owner: one pending aim, one `long[2]`, one evasion state (cooldown + four ids); dropped when the zone ends (death, logout, dimension change, expiry, collapse, server stop; an owner who vanished is cleared by the tick) |
| Client | idle: one short list walk per tick and an early return. Key held: one ray and one entity query along it every 3 ticks (a few allocations, not per frame) |
| Draw cost | no new drawing |

## Server friendliness (rule 9)

All decisions are on the server; the client only draws and asks. No client class is touched from common code (`ZoneUpSourceScanTest` pins it, and `docs/server_safe_allow.txt` did not grow). State is per player UUID with cleanup. The packet validates its sender, the switch, the dimension, the numbers and the target. Gameplay settings are SERVER configs. Effects go through the effect-packet budget and only to players near the effect (`VfxSpawn`).

## Tests

* `ZoneUpMathTest` (15): finite/range checks, entity id range, rate limit incl. clock going back, freshness, each verdict of the aim check in its order and the tolerance boundary, page eligibility, the three origins (far side, edge capped at 16, above), every origin inside the zone for 12,000 random targets, the look against Minecraft's own view-vector formula, projectile prediction (head-on, away, wide, inside, horizon), dodge chance, sidestep direction/length.
* `ZoneUpRulesTest` (3): defaults sane and unchanged by the sanitiser, OFF = old zone, wild and NaN configs stay in range.
* `ZoneUpSourceScanTest` (8): no client class in common code, no keybind in the client half, the packet validated before anything is kept (and by sender UUID), the pose restored in a `finally`, no world scan and a ray budget, cleanup on every end of a zone, shared files only gained hooks, settings in the SERVER config.
* Self-tests (CI server smoke test, `magic.zone_up`): `rules`; `buffs_evasion` (modifiers on and off with the zone; an arrow flying away or passing wide is ignored; one at the owner is dodged by a sideways impulse of exactly the configured size, the cooldown holds, it dodges again after it; state dropped with the zone); `cast` (aim from a player without a zone, before the zone has a radius, NaN, far, absurd: dropped; too fast: first kept; a fresh aim: the page sees the target, the posed caster looks at it from inside the zone, pose/look/velocity restored exactly; no aim: untouched; cast cooldown; ineligible page; stale aim; a page that moves the caster is undone; a foe id believed only when the foe is at the point; everything dropped when the zone ends).
* Unit tests run with `msf_jt.sh`; the type check with `tc_wt.sh` (the remaining errors in the touched files are the stub artefacts that were there before: `MobEffectInstance is abstract`, `RenderSystem.setShader`).

## Not verified in a game

Nothing was run in Minecraft. Unverified: that `TensuraKeybinds.ACTIVATE_SLOT_*` are the keys the grimoire is cast with in every setup (if a player casts through another path the aim packet is simply not sent and the page is cast from the hand); how the 700 page handlers behave when posed at a far origin (the helper redirection and the pose are tested only through the self-test's stand-in handler, not through real pages); that `ProjectileImpactEvent` cancellation makes every projectile type fly on through; the feel of the sidestep. The wiki (English and Japanese) could not be fetched (`ENOTFOUND`); the lore is the owner's spec text.

## Decisions for the owner

1. Default origin: BEHIND the target. EDGE (spells fly inward from the zone's boundary, as in the spec's "from the edges of the zone") is one setting away; it can be made the default.
2. The pose approach (`zoneCastRelocate`) is what makes all 700 pages omnidirectional at once; the alternative (false) is the safer but weaker helper-only mode. Default on; flip it if a page misbehaves in play.
3. Dodge chance 60% and the 30-tick cooldown are guesses.
