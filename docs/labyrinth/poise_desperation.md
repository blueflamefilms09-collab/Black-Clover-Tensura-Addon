# Poise, super armor, desperation, telegraphed attacks and the passive dungeon (1.107.0, lane "combat")

The owner's spec (section "Lethal Elite Mobs" and "Advanced Squad AI, Passive Inter-Mob Relations") asks for five things. An audit found the pieces they sit on already built (the Mob Foundry's
abilities and brain, the squads and their captains, the eco turf wars), so this lane adds the missing layer on top and keeps everything that was there.

| # | Spec item | Status before | This lane |
|---|-----------|---------------|-----------|
| 1 | Poise / super armor for elites and captains, until the bar breaks | not built | NEW |
| 2 | Desperation phase below 30 percent health (faster attacks, ultimates, cue) | not built | NEW |
| 3 | Telegraphed attack goals on the foundry abilities (cleave, slam ...) | the foundry had a wind-up and a cast clip, no floor warning, no armor, no frozen aim | NEW goal that extends the old one; the old goal stays behind a flag |
| 4 | Unified passive dungeon faction (no mob-vs-mob target, no friendly fire) | eco turf wars ON by default, foundry rival goal ON by default | NEW rule; both old systems KEPT behind their config |
| 5 | The "why" of each decision | - | the section "Why" below |

## NEW / REPLACED / KEPT

| Part | Kind | Where |
|------|------|-------|
| Poise bar: capacity, drain, regeneration, stagger, immune window | NEW | `mega/poise/PoiseMath`, `PoiseState`, `PoiseTuning` (pure) |
| Who carries a bar (elite / captain), never a boss, servant or minion | NEW | `PoiseBearers` (pure), `PoiseSystem.classify` |
| The attached data `nusmp:mega_poise` (saved with the mob) | NEW | `PoiseData`, `PoiseRegistry` |
| Super armor: blows drain the bar and lose 35 percent, no knockback; the breaking blow staggers | NEW | `PoiseSystem.onIncomingDamage`, `onKnockBack` |
| Desperation rule (enter under 30 percent, leave over 45) | NEW | `Desperation` (pure), `PoiseSystem.step` |
| Desperation cue: dark burst, ring, roar, embers, action-bar line | NEW | `PoiseCue` (only the `Fx` facade) |
| Archetype ultimates (10, one per archetype) | NEW | `data/nusmp/mega/poise/telegraphs.json` ("abilities" rows, the format of `_abilities.json`) |
| Shockwave for desperate captains that are not foundry mobs | NEW | `DesperationShockwaveGoal` |
| Telegraph table (kind / ability -> shape, armor, minimum wind-up) | NEW | `telegraphs.json`, `TelegraphLibrary`, `TelegraphDef`, `TelegraphData` (reload listener) |
| Telegraph geometry, one arithmetic for the outline and the hit test | NEW | `TelegraphGeometry` (pure) |
| `TelegraphedAttackGoal` | NEW | `mega/foundry/TelegraphedAttackGoal` (extends the old `AbilityGoal`) |
| Passive dungeon | NEW | `Passivity`, `PassivityRule` |
| Foundry ability goal | KEPT | `FoundryGoals.AbilityGoal` is used as before when `foundry.poise.telegraphedAttacks` is false (it lost `final`, nothing else) |
| Foundry abilities, archetypes, brain, runner | KEPT | extended only: `FoundryBrain` (pace, inherit), `FoundryAbilityRunner` (frozen aim), `FoundryMobEntity` (`PoiseAware`) |
| Eco turf wars, `TurfWarGoal`, the foundry `RivalGoal` | KEPT | still there; they run only with `foundry.poise.dungeonPassive = false` and their own switch on |
| Default of `eco.turfWarsEnabled` | REPLACED (true -> false, the owner's spec) | `EcoConfig` |
| Default of `foundry.factionWars` | REPLACED (true -> false, the owner's spec) | `FoundryConfig` |

Nothing was deleted. No percentage-of-max-health damage exists anywhere in this lane (the poise capacity is derived from health, but every blow, shockwave and ultimate is a multiple of the
attacker's attack damage). No hunger or exhaustion is touched. No skill tree, no nested skill checkbox, no change to the floor scaling curve.

## How it plays

* A mob is a **bearer** if it is a squad captain (the leader of its squad), a foundry mob of the elite tier (default 3, floors 401 and deeper) or anything carrying the tag `nusmp_elite`. Never a
  boss, never a servant of a player, never a summoner's minion.
* **Capacity** = 0.40 x max health + 8 x tier (a captain x1.3), clamped to 20..600.
* **Super armor** is up (1) during the wind-up and the swing of a telegraphed foundry attack (rows with `armor: true`), (2) while the bearer swings or draws a weapon, (3) during the shockwave of
  a desperate non-foundry captain. While it is up a blow drains the bar by its damage (arrows by half), loses 35 percent of its damage and does not push. The blow that empties the bar does the
  full damage, pushes, **breaks the poise**: the cast in progress is lost (its whole cooldown starts), the mob is slowed and helpless for 50 ticks (2.5 s), the bar is back at half when the stagger
  ends and cannot be broken again for 100 ticks (no stun-lock). Outside the armor, blows are the old game.
* The bar refills 12 percent of its capacity per second, starting 80 ticks after the last blow.
* **Desperation**: under 30 percent health (ends over 45, so a healer cannot flicker it) the bearer attacks 1.4 times as fast (cooldowns and ability rolls divided by 1.4, wind-ups x0.8 but never
  under 8 ticks), moves 25 percent faster, shows a dark burst + ring + roar the moment it starts and embers while it lasts, and the nearby players get an action-bar line. A foundry mob adds its
  **archetype's ultimate** to its abilities (brute `last_fury`, skirmisher `blade_storm`, archer `final_volley`, caster `ruin_nova`, assassin `killing_lunge`, tank `earthbreaker`, support
  `last_prayer`, summoner `legion_call`, flier `death_dive`, charger `rampage`); every other bearer gets the `DesperationShockwaveGoal` (26 ticks of wind-up with armor, a 5 block circle, 1.5 x attack
  damage on players, every 13 seconds).
* **Telegraphs**: for every ability the table knows (cleave, whirlwind, slam, thunder clap, bash, leap, dash, dive, aura, curse, pull, arrow rain and the ten ultimates) the wind-up is at least the
  table's minimum (a 4-tick lunge becomes 10, a cleave 8 becomes 14), the outline of the exact hit ground is drawn with `Fx` particles every four ticks with a smaller copy growing inside it, the
  aim follows the target until the last third of the wind-up and then freezes, and the attack lands where it froze (`FoundryAbilityRunner` reads the frozen aim for dash, dive, leap, cleave, curse and
  arrow rain). The outline is drawn 0.5 block wider than the stated radius because the runner counts a target within sqrt(r^2 + 4).
* **Passive dungeon**: a monster of the Labyrinth dimensions (an `Enemy`, a monster by category, a foundry mob, or tagged by the Dungeon Master) cannot target another one
  (`LivingChangeTargetEvent` cancelled) and cannot hurt one (`LivingIncomingDamageEvent` cancelled: arrows, blasts, abilities). Servants and pets of players are not "dungeon monsters": the dungeon
  still hunts them and they still fight it. The player is never part of the rule.

## Config (`nusmp-labyrinth-mega-foundry-server.toml`, section `[foundry.poise]`; no new file to register)

| Key | Default | Meaning |
|-----|---------|---------|
| `enabled` | true | poise bars and super armor |
| `telegraphedAttacks` | true | the telegraphing ability goal (false = the old `AbilityGoal`; read when a mob is created) |
| `desperation` | true | the desperation phase |
| `desperateBelow` | 0.30 | entry share (exit is 0.15 higher) |
| `eliteTier` | 3 | foundry tier from which a mob is an elite (6 = none by tier) |
| `armorReduction` | 0.35 | share of a blow super armor takes off |
| `staggerTicks` | 50 | helpless time after a break |
| `dungeonPassive` | true | the unified faction; **false brings the old quarrels back** |

`eco.turfWarsEnabled` and `foundry.factionWars` now default to **false**. They only matter with `dungeonPassive = false`: the master switch wins on purpose, because a server config file written by an
older version still says `true` for both and nobody chose that value. To get the old turf wars back: `dungeonPassive = false` and `turfWarsEnabled = true`.

## Why (the spec's "why" for each decision)

* **A data attachment instead of a vanilla capability class.** NeoForge 1.21 has attachments, not capabilities, for per-entity state: `nusmp:mega_poise` is attached only to bearers (a plain zombie
  stores nothing), saved with the mob, and not synced (the client does not need the bar).
* **Poise as a state machine with no clock.** `PoiseState` takes the tick as an argument, so the whole cycle (armor, absorb, break, stagger, recover, immune window) is one unit-tested object, and
  the self-test drives time by hand.
* **Capacity from health, damage from attack.** A bar that scales with health makes a tier-5 captain harder to break than a tier-3 elite; the owner's rule that no hit is a percentage of health
  is kept because the bar is a capacity, never a damage.
* **Super armor reduces, it does not make immune.** A party must always be able to hurt a winding-up elite, otherwise "break its poise" is the only play; the 35 percent is the price of standing in
  front of it, and the bar is the way out.
* **The immune window after a stagger.** Without it two strong players chain a captain into a stun-lock for the rest of the fight.
* **Telegraph and hit test share one arithmetic.** If the outline and the damage disagree, either a dodge fails or an unfair hit lands; `TelegraphGeometry` is the single source and the cone test is the one
  the cleave uses.
* **The aim freezes in the last third.** A telegraph that tracks the player to the last tick cannot be dodged; one that freezes cannot be cheated. The frozen point is also what the runner uses, so
  the warning is the truth.
* **The table is data over the existing abilities.** The abilities (damage, cooldown, radius) stay in `_abilities.json`; a designer adds a warning to the fifty-first ability by writing a line.
  The ultimates are rows in the same format, parsed by the same parser.
* **Desperation has hysteresis and a minimum wind-up.** A healer ticking a captain across 30 percent must not flicker the phase, and a faster boss must stay readable (never under 8 ticks).
* **The passive rule is two events, not removed goals.** The spec says to overwrite `NearestAttackableTargetGoal`; the dungeon also holds modded mobs whose goal lists this mod cannot rewrite,
  so the rule cancels the target change and the damage at the event, which works for every mob, keeps the old goals alive behind the config (the owner's "add the new, keep the old"), and costs
  nothing when no two monsters meet.
* **The master switch beats the old switches.** See the config note above.
* **Every effect through `Fx`.** The mod's own particles come from the facade (budgeted, quality-scaled, one switch back to vanilla); the Rukh stays where it already was (the foundry's cast
  gather), because dungeons are one of its two owners.

## Self-tests (`/multiverse mega selftest dungeon.poise`)

`dungeon.poise.data` (table loaded and audited, defaults), `dungeon.poise.armor` (elite foundry mob and tagged zombie: open, absorbed, no push, break, stagger, recover, immune, swing armor; plain
zombie, boss and servant carry nothing; save round-trip), `dungeon.poise.desperation` (20 percent on, 40 still on, 70 off, ultimate in and out, speed buff, shockwave goal on a zombie),
`dungeon.poise.passive` (monster vs monster and foundry vs foundry blocked on target and damage, player and servant untouched), `dungeon.poise.telegraph` (the goal is there exactly when the config says,
every cleave/slam/leap/dash/dive/bash the roster uses has a warning), `dungeon.poise.cast` (one whole cast per kind: wind-up at least the table's, cooldown starts, armor up, a broken bar cuts the cast short
and starts the cooldown).

JUnit: `PoiseRulesTest`, `TelegraphTest`, `BrainPaceTest` (pure rules, the shipped telegraph file audited against the foundry library, the geometry, the pace of the brain).

## Edits to existing files (all additions)

`MegaSystem` (2 lines), `FoundryConfig` (the `poise` section, `factionWars` default), `EcoConfig` (`turfWarsEnabled` default), `FoundryBrain` (pace, inherit), `FoundryGoals` (`AbilityGoal` not final,
stagger checks, desperate cooldown, RivalGoal gate), `FoundryMobEntity` (`PoiseAware`, brain refresh, the goal choice, `consider` after `setFoundry`), `FoundryAbilityRunner` (frozen aim),
`TurfWarGoal` / `TurfWars` / `EcoSelfTests` (the passive gate, three one-line conditions), `GoalInstaller` / `SquadHook` (`addGoal`, `hasGoal`, the call that gives a fresh squad's captain its bar).

## What could not be verified (nothing here can be run in a game)

* No game, no Gradle: everything was type-checked against API stubs (the only errors left are the known stub artefacts) and the pure logic was run in JUnit; the self-tests above are written but have not run.
* The look of the telegraph outlines (density, the 0.5 block padding, the colour of `dungeon.squad`) and the cue are unseen; the numbers (24 + 10 points every 4 ticks per winding-up mob) were chosen
  against the particle budget, not measured.
* Whether `Mob.setTarget` posts `LivingChangeTargetEvent` for every mob type (it does for the vanilla and the foundry mobs this was read for); a mod that sets its mob's target by writing the
  field or a brain memory directly (a vanilla Brain-based mob such as a piglin) bypasses the target part of the rule; the damage part still holds.
* Vanilla melee mobs have no attack-speed attribute, so "faster attack speed" is a 25 percent speed buff for them (the foundry mobs really attack 1.4 times as fast).
* The frozen aim keeps the body turned with `LookControl`; if another goal re-aims the head in the last third the attack may land a few degrees off the outline (the damage uses the frozen point, the cone
  uses it too, but the mob's visible turn is not forced).
* The stagger of a mob that is not a foundry mob is a heavy slowness effect, not a forced stop: a monster already in reach can still land a blow while staggered.
* A server whose `eco` / `foundry` config files hold explicit values keeps them; the master switch makes that harmless, but `dungeonPassive = false` plus a stale `turfWarsEnabled = true` revives the
  old wars.
* Performance under load (hundreds of bearers) was not measured; the bearer loop is a weak set stepped once per tick, the real work runs every fifth tick.
