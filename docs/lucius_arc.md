# Lucius, the final arc (1.42.0)

The owner asked for Lucius Zogratis to fight the way he does in the last arc of the story: a tactical god whose danger comes from
lethal spell combinations, control of the field and relentless pressure, **not** from a bloated health bar or passive healing. This
document is the design: what every class is for and why it is built that way, the numbers, the owner's exceptions to the boss
rules, what is new and what replaces something, and what has not been tried in the game.

The whole rework sits behind one switch, `luciusAnimeArc` (server config `nusmp-boss-rules-server.toml`, default **true**). With it off
the boss is exactly the 1.20 to 1.37 Lucius of `docs/zogratis_bosses.md` section 9 (his old precognition charges, the Gates refill,
every move of the table). Nothing of the old code was deleted.

> **1.45.0**: the god tier (three stages with health resets, a hard hit cap, percent-of-max-health true damage, Time Decay, the relic pulse,
> severance, starvation, anti-flight, the answers to massive channels and Ultimates, Time Stasis Traps) is built on top of this document and
> switched by `luciusGodTier`; with it off the fight is exactly what is described below. See `docs/lucius_god.md`. Where this document says the
> acts follow 65% and 30% health, with the god tier on they follow the stage instead.

## New or replacement (rule 5)

| Part | New or replacement | What happens to the old |
|---|---|---|
| Three acts by health (65% and 30%), the act name on the boss bar | **New** | The power curve (20/40/60/80/100%) still runs underneath: it still scales damage, cooldowns, mana, and opens the Gates at 25% |
| Chains (Checkmate, Soul Touch follow-ups, act 1 and act 3 chains) | **New** | The old 25-move table stays; with the arc on, only a few light moves of it are used as filler between chains (below) |
| Soul Touch, Soul Corruption, turning the victim's companions | **New** (the existing Soul Paladins, which turn *hostile mobs* into immortal knights, are a different feature and are untouched) | |
| Precognition (phase out, four ways to beat it) | **Replacement** of the old charge-based precognition, `entity/zogratis/Precognition` | Kept as is and used when the arc is off |
| Active Time Reversal channel (act 2) | **New** (the old *Time Reversal* move of the table, which returned him to an earlier spot, is untouched and unused in the arc) | |
| Clones with their own Time/Gravity/Spatial grimoire (act 3) | **New** use of `ZogratisCloneEntity` (the Mirror Court clones are untouched) | |
| Chest gem as a second hit box (a NeoForge `PartEntity`) | **New** | |
| Levitation in act 1 | **New** | |
| The Gates' 100% refill for Lucius | **Replacement**: not used while the arc is on, see "Decisions" | Still used with the switch off, and for every other boss |
| Julius's four windows (75/55/35/12%) | **Kept** in both modes | They fire inside the arc; see below |

## The owner's exceptions to the boss rules

`docs/boss_rules.md` (1.37.0) says bosses never regenerate and never use Unique or Ultimate skills, and has exactly one exception: the
Gates refill (`refillAtGates`). The arc adds a second one, **asked for by the owner**:

* `luciusTimeReversal` (default true): in act 2, after a burst of damage Lucius channels Time Reversal for three seconds and, if nobody
  stops him, returns to the health he had when the burst began. It is the *only* way he ever heals, and it is a scripted restoration, not
  a regeneration: it runs through `BossHealGuard.allow(...)` and the budget's `bypass` exactly like `PhaseRefill`. `BossHealGuard`
  remains the whitelist; with the switch off, or with `stripRegeneration` untouched, nothing else heals him, and the arc removes the
  Gates refill for him (so net healing is smaller than in the old design).

## Acts

| Act | Health | Name on the bar | How he fights |
|---|---|---|---|
| 1 | above 65% | The Untouchable Messiah | Levitates 3 blocks above the ground (bounded hover). Never melees. Chains: `stasis_volley` (three slow bolts of time that hold what they hit), `tower_spam` (three Grimoire Tower spells), and a teleport away whenever someone comes within 6 blocks (every 6 s). An aura of superiority puts a light Weakness (2.5 s, renewed every 2 s) on everyone within 12 blocks: it does not stun |
| 2 | 65% to 30% | Triad Supremacy and Soul Corruptor | Lands, closes in, aggressive. Chains: `checkmate`, `checkmate_soul`, `soul_touch_followup`, `crush_pressure`, `crush_blink_touch`. After a burst of damage, the Time Reversal channel |
| 3 | under 30% | The Infinite Clones | Back among 3 to 5 copies (3 at 30%, one more for each 5% lost). Chains: `clone_wave`, `checkmate`, `soul_touch_followup`, `crush_pressure`, `stasis_volley`, `tower_spam`. The chest gem shows in windows. Body blows do 15% |

`boss/arc/LuciusArc` decides the act: pure, no Minecraft imports. Going down is immediate; going back up (only a Time Reversal can raise
his health) needs 3% above the line, so a restore that lands on a line cannot make the act flap; the restore is also capped at the top of
act 2 (65% minus one health point) so it can never undo an act. One act is entered per call (a hit across two lines still announces both,
in order), and the set of acts already announced is a bit mask: an act is announced **once**, even after a regress. State is two ints in the
save data (`LArc.Phase`, `LArc.Entered`), restored without re-announcing.

## Chains (`boss/arc/LuciusChains`)

A chain is data: a list of steps, each with the ticks of **telegraph** the players get (the warning on the ground or in the air, which lasts
exactly that long), the **gap** after it, and whether it can hurt. The rule that makes the fight readable is checked by a test over every
chain: *a step that can hurt is shown at least 12 ticks ahead*. `Runner` walks one chain a tick at a time and emits BEGIN, FIRE (exactly
`telegraph` ticks after BEGIN), DONE; it is saved (chain id, step, ticks left) and a reload **restarts the current step's warning from
the top**, never shortens it. A chain aborts when its target dies or leaves 64 blocks, when he is staggered, channels, transforms, or
Julius surfaces.

| Chain | Acts | Steps (telegraph ticks) | Cooldown |
|---|---|---|---|
| `checkmate` | 2, 3 | gravity pin (20) -> spatial blink behind the target (12) -> Chrono Stasis bubble (16) | 18 s |
| `checkmate_soul` | 2 | the same, then 64 ticks of waiting for the stasis to end, then the Soul Touch (14) | 38 s |
| `soul_touch_followup` | 2, 3 | blink behind (12) -> Soul Touch (14); three times as likely when the target is within 8 blocks | 12 s |
| `crush_blink_touch` | 2 | gravity crush zone (24) -> blink (12) -> Soul Touch (14) | 21 s |
| `crush_pressure` | 2, 3 | two gravity crush zones one after another (24 each) | 16 s |
| `stasis_volley` | 1, 3 | three Chrono bolts (18 each) | 15 s |
| `tower_spam` | 1, 3 | three Grimoire Tower volleys (20 each) | 12 s |
| `retreat` | 1 | teleport away (8; nothing hurts), started by proximity only | 6 s |
| `clone_wave` | 3 | the clones appear (30), started when copies are missing | 20 s |

What a step does (`entity/zogratis/ArcSteps`, one place for the world effects, shared with the clones):

* **Gravity pin**: a marked well (the ring closes over the telegraph), then everything inside radius 3.4: heavy downward velocity,
  Slowness IV for the balance law's control time (`BalanceLaw.controlTicks`, 2.5 s at the most, halved on repeats), 9 base magic crush,
  held to the floor for one more second. Fall distance is cleared.
* **Spatial blink**: the spot 2.5 blocks behind the target's look vector (flat), from `LuciusFx.standable` (solid floor, nothing inside the
  body, never over a pit); fall-backs at 1.8 and 3.2 blocks, then either side; none free = the step is skipped. The spot is shown for
  the whole telegraph. He blinks with `ZogratisBoss.blinkTo` and turns to face the target.
* **Chrono Stasis bubble**: the existing Time Magic freeze, bounded: the target and up to three others within 3 blocks, **for the balance
  law's control time (3 seconds at most on a player)**, never a creative or spectator player, never a holder of anti-magic (the designed
  counter), never without line of sight, never during Julius's window, and a 6-second grace afterwards (`ChronoMoves.release`). It never
  damages by itself and an 8-health blow breaks it (the existing `TimeStop` rule).
* **Chrono bolt**: a slow bolt (0.55 blocks a tick, weak homing so a sidestep avoids it, 4 seconds of life, stops at walls) that hurts (5
  base) and holds what it touches as above. Drawn with the existing Time Magic stasis sphere every third tick.
* **Tower volley**: `TowerSpells.draw/cast` (the existing library of every magic), with the circle in the magic's colour as the telegraph.
* **Crush zone**: `DanteMoves.gravityField` (radius 7 + half a stage, 80 ticks), the same field Dante and the old Lucifero's Weight use.
* **Soul Touch**: see next section.

## Soul Touch, Soul Corruption, and the turned companions

The ring shown for 14 ticks around the target's feet (3 blocks across) is the reach: whoever is still in it when the touch comes is
touched (he steps in, up to 6 blocks, with a safe blink); whoever left it is not, and the chat says "Lucius's hand closes on nothing".
A touch that lands does 26 base magic damage through `LuciusBoss.strike` (the balance law's cap of 40% of a player's health per hit applies
and the arc's pressure cap below), then:

1. **Soul Corruption** (`entity/zogratis/LuciusEffects`, own `DeferredRegister<MobEffect>`, id `nusmp:soul_corruption`): level 1 on the first
   touch, one more level for each new touch (three at most): maximum health -10%, -20%, -30% for **60 seconds**, never below **four
   hearts** (`boss/arc/SoulCorruptionMath.reduction`), recomputed every two seconds against the current base maximum, removed with the
   effect by milk, a Totem of Undying, death or expiry. It is not damage and cannot kill: the lowest maximum is 8 health, and health above
   the new maximum is clamped by the game when the attribute changes. Why one transient `ADD_VALUE` modifier computed by hand and not the stock
   attribute template: the template cannot honour the floor. Icon: `textures/mob_effect/soul_corruption.png` (`tools/gen_lucius_arc_icons.py`);
   lang `effect.nusmp.soul_corruption` and `.description`.
2. **Turned companions** (`entity/zogratis/PaladinTurns`, bookkeeping in pure `boss/arc/PaladinTurn`): for 20 seconds, at most 6 at once,
   the victim's tamed animals and other `OwnableEntity` mobs, and the creatures called by relics (`Fx.summon`, the persistent tag
   `nusmp_relic_owner`) fight for Lucius against their original owner. **Djinn are skipped: in this mod they are armour-only effect data
   (`relics/djinn`), there is no Djinn entity to turn.** Bosses, the Zogratis family and players are never turned.
   * It is a **loan**: no type, health or attribute is changed. A compound `nusmp_paladin_turn` in the creature's persistent data holds
     Lucius, the owner and the end tick. A `TamableAnimal` refuses to attack its owner, so for the loan its owner id is cleared and written
     back on release.
   * Targets: the creature's own goals are untouched (protected fields): `LivingChangeTargetEvent` redirects any target that is not a player
     in survival or a not-turned companion to its prey (the original owner first), and a drive every half second sets the target, paths to it
     and strikes in reach, the way `relics/RelicSummons` already drives Tensura's brain-driven mobs. `RelicSummons.drive` has one added
     line that skips a turned creature (otherwise it would re-aim it at the owner's enemies).
   * Given back: on expiry, when Lucius dies, when the fight ends or resets, when he is removed for any reason (including a chunk unload),
     when the creature joins or leaves a level carrying the tag (so a saved-in-the-middle turn can never survive a restart), and for every
     level at `ServerStoppingEvent`.
   * Nothing hurts the creature on our account: **the owner's own blows on it are cancelled** (with a hint), and an **anti-magic blow frees
     it**. A turned creature's blow on a player is capped at 12% of the player's maximum health and counts against the pressure cap.

## Precognition (`boss/arc/PrecogMath` + the damage filter)

While active, every incoming blow with an attacker is **phased out**: no damage, a rewind dial and a soul vortex where he stood, a chime,
and a word on the attacker's action bar (at most every 2 seconds). It does **not** work when:

| Rule | Condition |
|---|---|
| Multi-attacker | two different attackers' blows within 6 ticks of each other (a player's pets and relic summons count as the player) |
| Rapid | one attacker lands 4 blows within 20 ticks; from the fourth on, every blow lands while the pace holds |
| Anti-magic | `LuciusFx.antiMagicHit` (demon swords, the Anti-Magic Lord, the Anti-Magic grimoire) **or** the main hand is in the item tag `nusmp:anti_magic_weapons` (Demon-Slayer, Demon-Dweller, Demon-Destroyer, the Licht swords and Elsdocia; every entry optional) |
| Synchronized relics | the attacker has **at least 2 working relics of grade Unique or better** at once (`Relics.loadout(p).active()`) |
| No attacker | poison, fire, a field nobody stands in: nothing to read, it lands (as before) |
| Off | he channels Time Reversal, is staggered, the gem is out, he is transforming or exposed after an ultimate, or Julius holds the body |

Why the hook is `LuciusBoss.filterDamage` and not a `LivingIncomingDamageEvent` listener: `ZogratisBoss.hurt` already calls that hook before
any health is touched and before the budget, the clamp and the mitigation, with the attacker in hand, so it is the same place with no extra
registration and no ordering question against the other listeners (the old precognition used it too). The phased blow returns 0 and `hurt`
returns false. Why a tracker with a short history and not charges: the owner wants the counters to be *things the players do together*.
Every blow counts for the history, phased or not, so the fourth fast blow is recognised.

## Active Time Reversal (`boss/arc/TimeReversalMath`, act 2)

* **Trigger**: he lost 18% of his maximum health within 100 ticks (`Window`, health sampled every 2 ticks), he is not already
  staggered, not casting, Julius is asleep, and the 25-second cooldown (from the end of the last channel) is over.
* **Channel**: 60 ticks. He is **rooted** (speed -100%), his chains stop, **takes 25% more damage** (and his nullifications are lifted:
  `LuciusProfile.open`), precognition is off. Shown by: a gold clock on the ground (`TIME_CLOCK`, radius 4.5) and a closing holy sigil, the
  rewind dial over him (`TIME_REWIND`, renewed every second) and a second boss bar for everyone within 128 blocks, "TIME REVERSAL - break
  it...", that fills over the 3 seconds and is styled through `BossBarSync` (the flash). Only the existing Time Magic effects are called.
* **Interrupts**: an anti-magic blow; a **heavy** blow (one hit of 6% of his maximum health, 3% from a wearer of a Legend-grade relic;
  measured on the incoming amount, before the damage budget); a lock on him (`TimeStop` frozen, or an effect named stasis / time stop /
  spatial lock / restrain); Julius surfacing. Then: **6 seconds of stagger** (Slowness -60%, no casting, mitigation lifted, precognition off),
  a loud message, the burst damage stays, cooldown 25 s.
* **Completion**: his health goes to the health he had at the start of the burst window, never above it, never above the top of act 2, never
  below where he is. Applied through `BossHealGuard.allow` + `dpsBudget().bypass`.

## The clones (`boss/arc/CloneScheduler`, `ZogratisCloneEntity`)

* Count: `CloneScheduler.cloneCount`: 3 under 30%, 4 under 25%, 5 under 20%. `clone_wave` fills missing slots on a ring 6.5 to 8.5 blocks
  round him (on floor, inside the arena radius), one wave per 12 s at most (and 15 s after a shatter).
* Each copy has a **slot** (0 to 4) and one spell by slot: Time, Gravity, Spatial, Time, Gravity. A copy casts on its own clock (every 8 s,
  first casts staggered by slot). The scheduler lets **at most one copy begin a cast per tick** and **never two of the same spell within 40
  ticks**; a test runs 4000 ticks to check it.
  * Time: the same Chrono bolt as his own (telegraph 20 ticks: a line to the target).
  * Gravity: a crush zone of radius 6 for 60 ticks (telegraph 24, a well on the ground).
  * Spatial: a blink to the target's blind spot (telegraph 18, the spot shown) and, 16 ticks after landing, a marked strike (radius 2.6, 12 base).
* They look the same as him (his body and grimoire); health **24**, no loot, no experience, they never heal, they never fade by themselves
  (24000 ticks), and the director pulls any copy that wandered beyond the arena radius back next to him.
* They vanish when the real one dies, leaves, or when the gem shatters (`fadeAway`: not struck down, so Resurrection cannot raise them).
* **Shared pressure cap** (`CloneScheduler.PressureBudget`, applied in `LuciusArcHooks.onIncoming`): everything that Lucius, his copies and
  the turned companions do to one player is held to **30% of that player's maximum health per second in total** (before armour). Nothing
  in the spell code had to change: the cap sits where the damage arrives, so the bullet hell of act 3 cannot out-damage it, and the old mode
  (switch off) is not affected. This is the arc's "DPS cap on players": the boss budget of `docs/boss_dps_budget.md` caps damage *to the
  boss*, not to players, so it is not the right tool for the owner's requirement, and the owner's wording ("all your damage goes through
  the existing damage helpers so the cap holds") is met by routing every spell through `LuciusBoss.strike` (the balance law's per-hit cap)
  *and* this per-second cap.

## The core gem (`LuciusCoreGem`, `boss/arc/CoreGemMath`, `LuciusCoreGemLayer`)

* A NeoForge **`PartEntity<LuciusBoss>`**, 0.6 x 0.6, built in his constructor (client and server), positioned on his chest, 0.35 blocks in
  front of it every `aiStep`, `isMultipartEntity()` true, `getParts()` returns it, `setId` gives it `id + 1` (the way the ender dragon's
  parts keep ids in step with the client). `isPickable` is true only while the synced flag says the gem is out; `hurt` is routed to
  `LuciusBoss.hurtGem`. It is never saved or spawned. In front of the chest means the crosshair reaches it before his body, from behind
  the body is hit: you have to face him.
* **Window**: act 3 only; closed for the first 6 seconds of the act (the copies come first), then 8 seconds open out of every 14. Also open
  while he casts the `clone_wave` (he casts Unlimited Cloning with his core open). A warning ring and chime show 1 second before it opens.
  The VFX `LUCIUS_CORE_GEM` (appended at the end of `VfxShape`, drawn by `LuciusCoreGemLayer`, previewable with
  `python tools/vfx_preview/preview.py lucius_core_gem`) follows his body for the length of the window: a pulsing four-pointed star with a
  turning ring and rising motes, 44 vertices.
* **Hits**: a critical blow or an anti-magic blow on the gem **shatters his resonance**; any other blow on the gem is an ordinary body blow.
  Critical = a crit arrow, or the vanilla melee conditions worked out by hand (swing strength over 0.9, falling, off the ground, not
  climbing, not in water, not blind, not riding, not sprinting): vanilla does not crit against a part (it is not a living entity), so the
  swing strength is recorded when the attack starts (`AttackEntityEvent`) and the rest is read when the blow arrives.
* **Body**: in act 3, every blow on his body does **15%** (`filter`); precognition is off while the gem is out.
* **Shatter**: every copy is gone at once; the core takes **22% of his maximum health as true damage** (never leaving him under 1 health);
  he is staggered 10 seconds (slowed, no casting, mitigation lifted, precognition off); the gem stays closed for the next 20 seconds and the
  copies return 15 seconds after the stagger.
  * Decision on the budget: the per-second damage budget (`BossBudget`) **does** apply to damage taken by him (6% of his maximum per second,
    1.25 to 1.5 times that in the last stages), and it is the rule that "nothing one-shots a boss". So the 22% is **not** applied in one
    write: it is a debt (`CoreGemMath.Burn`) paid through `setHealth`, which the budget truncates each second, so it takes 2.5 to 3 seconds
    of the stagger to land, and it respects the power gates (`PowerCurve.clampDamage`, nothing skips the Gates; it waits while he
    transforms). It is still "massive": more than a fifth of the bar inside the stagger, a true damage that armour and nullifications do not
    touch.

## Levitation (`boss/arc/HoverMath`)

Act 1: gravity off, a ground probe straight down (12 blocks), a proportional vertical speed to 3 blocks above the ground clamped to +0.14 /
-0.20 a tick, a gentle horizontal drift to keep 9 to 15 blocks from his target. **No ground within the probe (a pit, the void) = the hover
switches itself off** and he falls like any mob; entering act 2, the end of the fight, the boss leaving the world, and a switch turned off in
the middle all release it; a boss saved in the air and loaded after the act changed is released on its first tick.

## Decisions on the owner's open questions

* **Gates and the refill**: with the arc on, `refillOnGates()` is false for Lucius. His arc already has its own comeback tool (the Time
  Reversal) and its own end (the gem), and a full bar at 25% would erase act 3 (which starts at 30%). The Gates' transformation itself (60
  invulnerable ticks, the divine form, power 100) is kept: it happens at 25% inside act 3. `refillAtGates` keeps its meaning for every
  other boss and for Lucius with the arc off.
* **Julius's windows** are kept in both modes. In the arc they fire at 75/55/35/12% like before: the chain stops, a channel is broken, the copies
  fade, precognition is off, damage x1.4, and afterwards the copies come back by the normal rules. It is the old lore ("two souls") and a
  designed breather; if the owner wants the arc without them, that is a switch away (`JuliusWill`).
* **Old moves**: with the arc on, the move table is used only as filler when no chain is ready (act 1: Towers Answer, Soul Motes; act 2: those
  plus Beyond Light and Blood Tide; act 3: Towers Answer, Soul Motes, Bone Arsenal), under their own cooldowns and mana, so he is never
  idle. The ultimates (Soul Purge, Sibling Assimilation, Time Stop, Tower Storm) and the rest are not used in the arc (they stay in the
  table for the old mode and for the move list indices the client reads).
* **Particles**: the new effects are all `ZogratisVfx` layers (well, pillar, sigil, vortex, ray, gate, shatter), the existing Time Magic
  effects (called, never edited) and the new gem layer. The old helpers that draw dust (`Tells`, `LuciusFx.dust`) are not used by the new code.

## Files

New classes (each has a `Why:` Javadoc): `boss/arc/{LuciusArc, LuciusChains, PrecogMath, TimeReversalMath, CloneScheduler, PaladinTurn,
CoreGemMath, SoulCorruptionMath, HoverMath}` (pure), `entity/zogratis/{LuciusArcDirector, ArcSteps, PaladinTurns, LuciusArcHooks,
LuciusCoreGem, LuciusEffects}`, `vfx/client/layer/LuciusCoreGemLayer`. Tests: `src/test/java/.../boss/arc/*Test` (LuciusArc: lines,
hysteresis, once-only events, save/restore; LuciusChains: fairness, Checkmate order, picks, runner, abort, resume; PrecogMath: each bypass
rule; TimeReversalMath: window, channel, interrupt, restore bounds, cooldown; CloneScheduler: counts, staggering, pressure cap; PaladinTurn:
cap, loans, release, restore; CoreGemMath: window, crit, classification, shatter, burn; SoulCorruptionMath; HoverMath).

Other files and what each is: `data/nusmp/tags/item/anti_magic_weapons.json` (the registered anti-magic weapons, every entry optional, so a
build without one of them still loads; an addon adds its own), `textures/mob_effect/soul_corruption.png` (18x18 icon: a cracked violet heart
with a soul leaving it, drawn by `tools/gen_lucius_arc_icons.py` so the palette lives in one place), `tools/vfx_preview/scenes.d/lucius_arc.json`
(the gem preview), the lang entries (`frag_lucius/lu1.lang.json`).

Edits to existing files (all additions, behind the switch): `LuciusBoss` (hooks to the director, the part, a synced int), `ZogratisCloneEntity`
(arc slot, arc cast, `spawnArc`), `LuciusFx.isFoe` (a turned creature is no foe of Lucius), `LuciusProfile.open` (staggered, channelling or
gem out lifts the nullifications), `BossRulesConfig` (two keys), `VfxShape` (one shape appended last), `VfxManager` (one layer),
`relics/RelicSummons.drive` (one line), `tools/vfx_preview/preview.py` (the gem layer shares `ZogratisFx`).

## Saved state

The entity's save data has a compound `LArc`: act and entered mask, the running chain (id, step, ticks left) and its target, every chain
cooldown, the Time Reversal channel (state, end, cooldown, stagger end, start health), the act-3 start tick, stagger end, gem closed until,
clone respawn tick, the unpaid shatter damage, and the loans (creature, owner, end). All times are absolute game ticks. Nothing is static
except the idempotent flag of `LuciusArcHooks.register`.

## Unverified in the game

Everything here was compiled against API stubs and its pure part unit tested; **nothing was run in Minecraft.** The ones most worth watching:

1. `PartEntity`: that the gem is picked by the crosshair (client) and by the attack packet (server) with the id `id + 1`, that `isMultipartEntity`
   and `getParts` are the NeoForge 21.1 signatures (the stubs lack them), and that an arrow's `isCritArrow` and the recorded swing strength
   reach `hurtGem` as expected.
2. The hover: that a noGravity mob driven by `setDeltaMovement` each tick holds 3 blocks over uneven ground and does not jitter; the drift speed.
3. Companion turning on real Tensura tamed mobs (brain driven): that `LivingChangeTargetEvent` plus the drive is enough, that the
   `TamableAnimal` owner clearing is not undone by the mob's own tick, and that relic summons stay turned (the `RelicSummons.drive` skip).
4. `SoulCorruptionEffect.addAttributeModifiers/removeAttributeModifiers` are the 1.21.1 overridable signatures (read from the API dump: yes,
   with an `AttributeMap`); that max health falling below current health clamps the health; milk and the totem cure it (the default cures).
5. The balance of the numbers (18% burst, 3-second channel, 30% pressure cap, 26 base touch) is a first pass.
6. A chain started in act 3 while clones are casting: the order is right on paper (one cast per tick) but the sheer count of telegraphs on
   screen has not been looked at.
7. The boss bar's second bar (the channel) uses `BossBarSync.update` with the id `lucius_zogratis`: the client style for a second bar of the
   same boss was not looked at.
