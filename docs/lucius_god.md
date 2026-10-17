# Lucius, the god tier (1.45.0)

The owner asked for Lucius Zogratis to be a fight for players who have already broken the rest of the mod: astronomical Existence
Points, regeneration that outpaces any boss, custom 3D weapons that deal a bar of health in one swing, Ultimate skills that end anything,
relics and Djinn on top of all of it. This document is the design of the answer: what every class is for and why it is built that way, the
numbers, what is new and what replaces something, what was kept, the config, and what has not been tried in the game.

It is built **on top of** the 1.42.0 final arc (`docs/lucius_arc.md`; read that first). Nothing of the entity, its registration or its
1.42.0 code was rewritten: the god tier is hooks into `LuciusBoss` and `LuciusArcDirector`, a handful of pure classes in `boss/god`, and a
few new classes in `entity/zogratis`. It sits behind **one master switch**, `luciusGodTier` (config `nusmp-lucius-god-server.toml`,
default **true**, needs `luciusAnimeArc`): with it off the fight is exactly the 1.42.0 one, and every mechanic below has a switch of its own.

Nothing was run in Minecraft. See "Unverified in the game".

## New, replacement, kept (rule 5), item by item

| # | Owner's item | New / replacement | What happens to the old |
|---|---|---|---|
| 1 | Three stages, full health reset, invulnerable transition, unlock the stage's arsenal | **New** (`LuciusStage`, `LuciusGod.fall`). Replaces the 65% / 30% health lines **only while the god tier is on** | The threshold acts (`LuciusArc.update`) are untouched and used when it is off. The PhaseRefill machinery of the Gates is reused, not copied |
| 2 | Theatrical voice lines | **New** (`LuciusLines`, `LuciusGod.speak`) | The old `quip` / `say` lines are kept (they are the 1.42.0 chatter); the table adds the eight moments of the brief |
| 3 | Hard damage gating | **New** (`HitGate`, the first clamp in `LuciusBoss.filterDamage`, the last in `ZogratisBoss.lastCap`) | The per-second budget (6%/s), `PowerCurve.clampDamage`, the nullification cuts and precognition are all kept; the cap sits in front of them |
| 4 | Percent-of-max-HP true damage | **New** (`TrueDamage`, `LuciusGod.strike/landTrue`). For a magic hit on a player it **replaces** the 1.42.0 base-damage law while the god tier is on | The old law (`ZogratisDamage`, `BalanceLaw`) is still what every non-magic hit, every hit on a creature, and the whole fight with the switch off use. The pressure cap is kept and applies on top |
| 5 | Anti-regeneration aura | **New** (`RegenGuard`, handler) | Nothing existed for players. The boss heal guard (`BossHealGuard`) is for bosses and is untouched |
| 6 | Relic nullification EMP | **New** (`RelicNullification`, the effect, the chain step) | Nothing existed. The anti-magic jamming of Tensura skills (`Nullification`) is a different feature and is kept |
| 7 | Spirit / Djinn severance | **New** (`Severance`, the effect, the chain step) | Nothing existed |
| 8 | Magicule starvation | **New** (`StarvationMath`, handler, the pulse chain) | Nothing existed |
| 9 | Anti-flight gravity | **New** (`FlightGuard`, handler) | Nothing existed (Zagred only *notices* flying players; Kotodama's "fall" word is a single-target spell) |
| 10 | Spatial disorientation | **New** (`ReflectWindow`, `LuciusGod.trySwap`) | The old Swap Strike swaps him with a *clone*; it is kept and unrelated |
| 11 | Ultimate skill reversal | **New** (`LuciusCapabilities`, `LuciusGod.reverseUltimate`) | `Nullification.interfere` (the anti-magic jam of Ultimates with a chance) is kept |
| 12 | The Checkmate counter | **New chain** `checkmate_counter` reusing the existing blink, chain runner and fairness rule | The old `checkmate` chain (gravity, space, time) is untouched |
| 13 | Time Stasis Traps | **New** (`TimeStasisTrap`, `TrapLedger`, `DodgeSpamMeter`, `DodgeEvents`) | Four one-line `DodgeEvents.record` calls are added to Rouge, Butoh (two) and Ebb Step; nothing else of them changes |
| 14 | Phase variety | **New chains** `stasis_snare`, `soul_ray`, `blood_tide_pressure`, `starvation_pulse`, `severance_bind`, `relic_emp`, and the two answers | Every 1.42.0 chain is kept; with the switch off they are never drawn |

## The stages, precisely

`LuciusStage` (pure) holds the number (1 to 3), what a fatal blow means, the refill id of each stage and the **virtual bar**.

* A fatal blow in stage 1 or 2 is caught in `LuciusBoss.die`, which is the one door every fatal hit uses (`hurt`, true damage, `setHealth(0)`
  through `baseTick`), before the death event, the loot and the bar end (so `LivingDeathEvent` is never used). `LuciusGod.fall`:
  1. puts him on 1 health at once (a body left at 0 health is removed by the game within a second), through
     `BossHealGuard.allow` and the budget's `bypass`, the same two wrappers `PhaseRefill` and Time Reversal use;
  2. starts the existing invulnerable refill, `refillForMode("luciusStageN", stageRefillTicks)`: the health eases up to the maximum over
     80 ticks, nothing can hurt him meanwhile, the bar flashes, `RefillFx` plays;
  3. advances the stage, drops everything of the old stage (the running chain, the rewind channel and its window, the unpaid gem debt, the
     stagger, the gem's clock, the copies, the precognition history, the chain cooldowns), announces it, shows a title and a subtitle to the
     arena, and speaks the line.
* **The same refill can not fire twice**: each stage has its own id, `PhaseRefill` remembers the ids in the save data, and a refused refill
  (already spent, or another one running) is **not** a second wind: he dies for real instead of staying at 1 health. The kill command and
  the void always kill (so an admin can end the fight). The last stage's fatal blow is a real death: loot, the bar ends.
* **The Gates**: his 1.42.0 decision stands: the Gates' *refill* is off for Lucius while the arc is on (`refillOnGates()` is false). The
  stage change is the **only** refill. The Gates' transformation (60 invulnerable ticks, the divine form) is kept; because the power curve
  now reads the virtual bar they open late in stage 3, not at 25% of stage 1.
* **The virtual bar**: `ZogratisBoss.powerHealth()/powerMaxHealth()` (new, default the real bar) are overridden to lay the three stages end
  to end (stage 1 is the top third). The power steps (20/40/60/80/100), the Gates, `PowerCurve.clampDamage` (no line skipped by a big hit), the
  burn of the gem and Julius's four windows all read it, so they run over the whole fight and never restart with a refilled bar. The end of
  stage 1 and the start of stage 2 are the same virtual point; **while the bar refills the new stage counts as full**, or the dip would
  raise the power early. In practice: stage 1 reaches 40%, stage 2 60% and 80%, the Gates open a quarter of the way into stage 3; Julius
  surfaces at 75% (late stage 1), 55% and 35% (stage 2) and 12% (stage 3).
* **The acts follow the stage** (`LuciusArc.enterStage`): stage 1 is act 1 (Time and Soul: the hover, the stasis volley and the snare, the
  tower spam, the soul lance from range), stage 2 is act 2 (Gravity and Blood: the crush zones, the Blood Tide, Time Reversal, the Soul
  Touch), stage 3 is act 3 (Space: the blink chains, the relic pulse, the copies, the chest gem). The act is announced once, as before.
* **Time Reversal** is kept as the owner's second exception. In the god tier its restore ceiling is the stage's own maximum (a stage starts
  full, the old "top of act 2" line would have capped it at 65%), and the heavy-blow test that breaks the channel reads the blow **before**
  the hit cap, otherwise a 6% blow could never exist. Its interplay with the stage change: a stage change aborts a running channel
  and clears its window, so a rewind can never carry health across a stage; a rewind can never exceed the health the burst began with.
* **Act 3 body factor**: the 1.42.0 arc cut every body blow to 15% because act 3 began at 30% health. A stage now starts full, so in the
  god tier the body takes 50% of a blow (`GOD_BODY_FACTOR`); the hit cap and the gem still rule. The gem's shatter (22% of the bar as a
  debt paid over seconds) is the fastest way through stage 3, as designed.

## Hard damage gating

No single hit may take more than `luciusHitCapPct` (2.0) percent of his maximum health. Applied in `LuciusBoss.filterDamage`, which
`ZogratisBoss.hurt` calls before the mitigation, the power clamp and the budget, so it is "the very start of the damage path"; applied
again, last, in `ZogratisBoss.lastCap` just before the budget (the exposed bonus is applied after the filter and could lift it).
Julius's 40% and the rewind channel's 25% are therefore also under it. When a hit was clamped:

* the attacker sees the cap on the action bar (at most every 2 seconds);
* he reacts, **alternating** an evasive teleport (the 1.42.0 `retreatSpot`) and a counter-attack (the chain `hit_counter`: a 14-tick
  telegraph, then a strike beside the target), at most once every 3 seconds, never during a chain, a cast, a channel, a stagger, a
  transformation, Julius's window or a stage change. A reaction refused for being busy does not use up the alternation.

**Exempt, because none of them is a "hit"**: the gem shatter (a debt paid through `setHealth`, truncated by the per-second budget), Time
Reversal's restore, the stage refill, every heal; and `/kill` and the void (they bypass invulnerability, and an admin must be able to end
the fight). Creative attackers are capped like everyone else (the `/kill` command is the tester's tool).

## Percent-of-maximum-health true damage

`TrueDamage` (pure): five tiers, **3, 4.5, 6.5, 9 and 12 percent** of the victim's maximum health. The tier of an old move comes from the
base number it already had (`tierOfBase`: up to 6.5 chip, 11 light, 16 medium, 22 heavy, above finisher): the chrono bolt (5) and the soul
mote (6.5) are chip, the gravity pin (9) light, the spatial strike (12) and the soul lance (16) medium, the soul touch (26) a finisher.
In `LuciusGod.strike` every **magic** hit of his on a **player** becomes `max health x percent x luciusTrueDamageScale` (a trap's guaranteed
critical x1.5), at most 40% of the maximum in one hit, delivered as indirect magic damage (armour is ignored) and therefore under the
shared 30%-per-second pressure cap of 1.42.0 (`LuciusArcHooks.onIncoming` sees Lucius as the attacker). Physical hits, hits on creatures
and every hit with the switch off use the old law. **EP**: Tensura exposes the magicule pool cheaply (`TensuraStorages.getExistenceFrom`,
`EnergyHelper.getMaxMagicule`); each true hit also drains the **same percent** of the victim's magicule, scaled by
`luciusMagiculeDrainFactor` (the pool the Black Clover mana shares). The Existence Points themselves (the maximum) are never touched: that
would be permanent.

## Time Decay (anti-regeneration aura)

Within `luciusAuraRadius` (20) of him while the fight is on, players and the companions of players cannot heal passively.

* The heal event (`LivingHealEvent`, the 1.21.1 NeoForge name) is cancelled and zeroed, unless a **potion or food is in use** (the use
  window opens at `LivingEntityUseItemEvent.Start` for a drink or a meal, and for four ticks after a thrown potion lands) **and** the heal
  is a burst of 2 points or more (`RegenGuard`): natural regeneration and the Regeneration effect heal 1 or less and can not ride a potion.
* Tensura's regeneration skills may call `setHealth`, which no event sees. Every tick the health of the players in the aura is compared with
  the last tick's (`RegenGuard.Tracker`); a rise that no allowed heal explains is reverted. Pets and summons are covered by the heal event
  only (not by the snapshot): a `setHealth` regeneration on a pet is not caught.
* Julius's window lifts the aura (his blessing is a heal on purpose). The boss's own heals are not touched: this code looks only at players
  and their companions; his Time Reversal and the refill are whitelisted through `BossHealGuard`.
* A ring on the ground (the existing `DIVINE_SIGIL`, radius = the aura, renewed every 50 ticks), an effect icon `nusmp:time_decay` on
  whoever stands in it, and one action-bar notice per entry.

## Relic nullification (the EMP)

The chain `relic_emp` (stage 3, telegraph 24 ticks): a pale ring of 14 blocks closes; at the end every **player** inside it (not holding
anti-magic, the designed counter) gets the effect `nusmp:relic_nullified` for `luciusEmpSeconds` (10). **One check function**,
`relics.RelicNullification.isNullified(entity)`, is asked in the shared places: `Relics.loadout` and `Relics.loadoutFresh` (everything the
relic waves, the sockets and the once-a-second attribute pass read: the pass then removes the modifiers by itself and puts them back when
the effect ends), `dungeon.nexus.relic.Worn.mask` and `Worn.stack` (the Treasury relics of the two `dungeon_relic` Curios slots), and
`RelicEngine.activate` (every act, held or socketed). Items are never unequipped, dropped or damaged. It ends by itself, on death, with milk, when the
player leaves the arena and when the fight ends. **Not covered**: the Tensura engraving enchantments on armour and weapons are read by Tensura
itself; switching them off needs a change in Tensura.

## Severance (Spirit / Djinn)

The chain `severance_bind` (stages 2 and 3, telegraph 28 ticks): a ray and a vortex mark one player; at the end, if he is still in Lucius's
line of sight and holds no anti-magic, he gets `nusmp:severed` for `luciusSeveranceSeconds` (12). `relics.Severance.isSevered` is asked by:
`DjinnEngine.onPlayerTick` (no upkeep, no skills lent, no sync) and `DjinnEngine.onKey` (refused with a message); `DjinnEngine.severed(p, on)`
(new) which on the start lets go a running Rukh or an Extreme Magic charge (the Magoi is refunded), takes the Maso's attribute boosts and its
flight off, and on the end puts them back and moves a Lesser Summon's clock on by the time lost; `SpiritBond.willAnswer` (the one door of the
grimoire's spirit pages and of the Spirit Lord skill); `SpiritLordSkill` (call, tick, spirit body); `SpiritLordEntity` (the companion
stands idle); and the Spirit Dive's upkeep (it waits, nothing is drained and nothing ends). Nothing is destroyed. **Limit**: an already
running Spirit Dive keeps its armour on while severed (it is only not fed).

## Magicule starvation

`StarvationMath` (pure): everyone in the arena loses `luciusStarvationPctPerSec` (1.0) percent of the maximum magicule per second (applied
every 10 ticks), and the chain `starvation_pulse` (stages 2 and 3, 20 ticks of a closing violet ring of 18 blocks) takes `luciusStarvationPulsePct`
(5) percent more from everyone inside. **Never health, never below 1% of the pool**: the mana law's black-out is below 0.5% and its shock
below 5% of a *spend* (`ManaLaw.exhaust`, called by the code that spends), so the arena alone can never trigger either; a player's own casts
still can. Creative and spectator players and players without a Tensura record are left alone. Interaction with `ManaLaw`: none beyond
reading the same pool (the Black Clover mana is the Tensura magicule pool), so an Ultimate that costs a share of the pool simply finds less of it.

## Anti-flight gravity

`FlightGuard` (pure) and the handler. While a player is in the arena during the fight: `mayfly` and `flying` are cleared every tick for
survival players (and for creative players when `luciusAffectCreative` is on; default off), `stopFallFlying` for an elytra, and a downward
push (`extraGravity`: nothing below 3 blocks, then 0.03 growing to at most 0.12 a tick) when more than 3 blocks up. Flight in this mod is
granted by many systems (the relic waves, the Djinn Maso, Tensura skills) and some re-assert it every second, so a naive "restore the old
value" would turn a flight that ended during the fight into permanent flight. **The restore is by evidence**: on leaving (or at the end of the
fight, or on logout, which only drops the record) flight is given back if the source was seen to re-assert it in the last 5 seconds, or the stay
was shorter than 10 seconds, or the flight is inherent (creative); otherwise the source gives it back itself the next time it asserts. The
Tensura flight skills (`GravityFlightSkill`, the `FlightMagic` and the `AirFlightArt`) fly by velocity, not by the abilities flag, and have no
event or flag to cancel their ticks; their **activation is vetoed** through ManasCore's activate event while the fight is on in the arena
("Lucifero's gravity holds you to the ground"), and one that was already running when the player entered is only fought by the downward push.

## Spatial disorientation, ultimate reversal, the Checkmate counter

What counts as a **massive channel / launched devastating attack** is one definition (`CounterRules`): a grimoire page that costs 25% of the
mana or more (the signature pages), held for at least 75% of its cast time and 10 ticks; switching the Mana Zone on; a Tensura skill with a
hold time of 30 ticks or more, held for 30 ticks. The grimoire reports it with one call in `onHeld`, `onRelease` and `onPressed` (Mana Zone);
the skills through ManasCore's events.

* **Checkmate counter** (`checkmate_counter`: blink 12 ticks, strike 14 ticks; once per target every 8 seconds): he drops a running chain
  or a wind-up (never a Time Reversal channel, a stagger, a transformation, Julius's window or a stage change), appears behind them
  (the existing `behindSpot`), and **at that moment the channel is broken** (the grimoire refuses casts for 40 ticks, the Mana Zone switches
  off, ManasCore skills are vetoed for 40 ticks); then, after the shown ring (2.8 blocks) has been on the ground 14 ticks, the strike lands on
  whoever is still in it: true damage (finisher tier), shield lowered first, unblockable. A target that left the ring is missed and told so.
* **Spatial disorientation** (once per player every 12 seconds): on the release of a massive attack he swaps places with the caster (both
  spots must be standable floor: never the void, lava or a wall). What the game lets us do reliably is not to steer a spell in flight but to
  remember **who** was swapped: for two seconds a blow of that player on him is absorbed and **half of it returns to the player** (at most 40% of
  their maximum health), so the attack "lands on its owner at 50%". The ultimates have their own answer (below) and never reach this.
* **Ultimate skill reversal** (once per 10 seconds): the activation of a skill whose Tensura type is Ultimate by a player in the arena is
  cancelled (ManasCore's activate and toggle-on events return "interrupt, false"; the release of it is cancelled too), a 12-tick reflection shell
  (a sigil, a rewind dial, a shatter, a bell) shows, and the caster takes `luciusMirrorPct` (6) percent of their own maximum health as true
  damage. **Honest note**: the damage the skill would have done is unknowable (the activation is cancelled before it is worked out), so the
  "50% mirrored" of the brief is a share of the caster's health, once per activation, under the pressure cap. Outside the cooldown the next
  Ultimate goes through: that is the players' window.

## Time Stasis Traps

`DodgeSpamMeter` (pure): 3 dodge events in 6 seconds. `DodgeEvents.record(player)` is the tiny public call: Rouge's saves, Butoh's evasion and
dash, the Ebb Step page, a skill whose id names a dodge, an evasion, a blink or a foresight, and a hit of Lucius's side that some other
system cancelled (a cancelled `LivingIncomingDamageEvent`, so a Precognition-like cancel is caught without knowing it; his own pressure-cap
cancel is excluded). One dodge counts once per tick. **Not hooked**: vanilla `hurtResistantTime` abuse (his overlapping hits would be
counted against innocent players). A trap (`TimeStasisTrap`, `TrapLedger`) is **not an entity**: a record in the boss's ledger plus rings (no
registration, no renderer, no save format for something that lives twelve seconds). At most 6, 12 seconds each (`luciusTrapSeconds`), laid under
the dodger's feet, armed 20 ticks later (the ring is the warning, the existing Time Magic clock shows once it is armed). Whoever steps in is
held by the existing freeze under the balance law (**3 seconds at most**, less on a repeat, never creative or spectator, never a holder of
anti-magic), once per player per 12 seconds, and the **next hit of Lucius on them within 12 seconds is a guaranteed critical** (x1.5, under
the cap). The traps go with the stage change, the fight and the boss.

## The classes (the WHY)

Pure, in `boss/god` (no Minecraft types, each with a test): `LuciusStage` (the stage, the fatal-blow decision, the refill ids, the virtual
bar), `LuciusLines` (the voice table), `HitGate` (the cap and the alternating reaction), `TrueDamage` (the tiers, the 40% cap, the pool drain),
`StarvationMath` (the drain and the floor), `RegenGuard` (which heals pass, the snapshot tracker), `DodgeSpamMeter`, `CounterRules` (the
definitions of a massive channel, the cooldown book, the mirror numbers), `FlightGuard` (the evidence-based restore and the gravity curve),
`TrapLedger` (six, twelve seconds, armed after a warning), `ReflectWindow`. Glue: `LuciusGodConfig` (every switch and number, plain
`define`), `DodgeEvents` (the public call, one listener, one gate).

In `entity/zogratis`: `LuciusGod` (one per boss: the stages, the cap, the voice, the answers; a registry of live fights), `GodSteps` (what the six
new chain steps do in the world, like `ArcSteps`), `TimeStasisTrap`, `LuciusGodEffects` (the three marker effects in their own
`DeferredRegister`), `LuciusBossEventHandler` (the one subscriber for everything that is about players), `LuciusCapabilities` (the pool, the
sabotage, breaking a channel, ManasCore's skill events). In `relics`: `RelicNullification`, `Severance` (the two one-line questions).

Edits to existing files (all additive and gated): `ZogratisBoss` (three protected hooks `powerHealth`, `powerMaxHealth`, `lastCap`, and
`interruptCast`), `LuciusBoss` (the god state, `die`, `baseTick`, `strike`, `filterDamage` split into the cap and `filterBody`, the lines, the bar title,
save and load), `LuciusArcDirector` (stage-driven act, the new steps, `startGodChain`, `onStageChange`), `LuciusArcHooks` (`ownCancel`, `luciusSide`),
`ArcSteps.hold` (line of sight optional), `LuciusChains` and `LuciusArc` (new kinds, chains and `enterStage`), `Relics`, `RelicEngine.activate`,
`Worn`, `DjinnEngine`, `SpiritBond.willAnswer`, `SpiritLordSkill`, `SpiritLordEntity`, `GrimoireBook` (three calls and the dive upkeep), `ButohDance`,
`ButohDash`, `ButohArts.ebbStep`, `RougeFateEvents` (one `DodgeEvents.record` each).

## Config (`nusmp-lucius-god-server.toml`, section `luciusGod`)

`luciusGodTier` (true, master), `luciusThreeStages`, `luciusVoiceLines`, `luciusHitCap`, `luciusHitCapPct` (2.0), `luciusTrueDamage`,
`luciusTrueDamageScale` (1.0), `luciusMagiculeDrainFactor` (1.0), `luciusTimeDecay`, `luciusAuraRadius` (20), `luciusRelicEmp`, `luciusEmpSeconds` (10),
`luciusSeverance`, `luciusSeveranceSeconds` (12), `luciusStarvation`, `luciusStarvationPctPerSec` (1.0), `luciusStarvationPulsePct` (5),
`luciusAntiFlight`, `luciusAffectCreative` (false), `luciusSpatialSwap`, `luciusUltimateReversal`, `luciusMirrorPct` (6), `luciusCheckmateCounter`,
`luciusStasisTraps`, `luciusTrapSeconds` (12), `luciusBlockEscapeTeleports` (ender pearls and chorus fruit are refused in the arena), `luciusStageRefillTicks` (80).

## The fairness rules that stay green

Every harmful step has a telegraph of 12 ticks or more (`LuciusChains.valid`, a test over every chain including the new ones: the relic pulse
24, the severance 28, the soul lance 18, the blood tide 22, the counter strike 14). The shared pressure cap (30% of a player's maximum health
a second) holds over every true hit, and one hit never takes more than 40% of a full-health player's maximum. Control uses the balance law
(`controlTicks`: 3 seconds at most on a player). Environmental drain never touches health. No new thing changes a block.

## Unverified in the game

Everything was type-checked against API stubs and its pure part unit tested (and the tests mutation-checked); **nothing was run in Minecraft.**
Worth watching first:

1. `LuciusBoss.die` as the door of the stage change: that a fatal `hurt` reaches `die` with the body at 0 health and that setting 1 health, the
   refill and returning from `die` leaves a standing, invulnerable boss (the death sound plays once); that `PhaseRefill` is happy to begin from 1 health.
2. The virtual power bar: the power steps and the Gates over three stages (they are tested as pure arithmetic, not in the game).
3. The cancelled `ACTIVATE_SKILL` of an Ultimate really stops the skill (it does for the boss rules and for `SpellSeal`), and the veto of the release.
4. Heal sources: that potions and food heals arrive inside the use window (the heal of a drunk potion happens in `finishUsingItem`, after `Start`);
   that no other mod heals through a path that is neither the heal event nor a `setHealth` the tracker sees (the tracker catches any rise).
5. `p.getAbilities().mayfly` clearing against the relic waves' flight (they re-assert once a second: the player will flicker to the ground each second,
   which is the intent) and the restore evidence rules.
6. The Djinn suspension (`DjinnEngine.severed`): the Maso's attribute boosts come back correctly, `Magoi` refunds, the lent skills lapse and return.
7. The grimoire hooks: `castReady` is evaluated a second time per held tick while a fight is on; the swap at release.
8. Balance: 2%, the tiers, 6%, the starvation numbers, 80 ticks of invulnerable stage change, the body factor, are a first pass.
9. The three effect icons (`tools/gen_lucius_god_icons.py`) and the lang entries (`frag/lu2.lang.json`).

## 1.56.0: the brief arrived a second time

The same brief (three stages, the 2.5% cap, the 30-block aura, the EMP, the severance, starvation, the gravity domain, the swap, the reversal, traps, the Checkmate) was sent again. Every item is the 1.45.0 work above. What was added: the **`chest_gem_reveal`** clip (both models, played by `ZogratisPose` on the rising edge of the gem flag, in `tools/gen_zogratis_lucius.py`) and the **debuff cleanse** of the boss at a stage change (`LuciusGod.fall`). The numbers differ from the brief on purpose: `luciusHitCapPct` stays 2.0 and `luciusAuraRadius` 14 (eased in 1.46.0); set 2.5 and 30 in the config for the brief's values. Not verified in the game.
