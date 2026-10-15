# Boss rules (1.37.0): no regeneration, no Unique/Ultimate skills, Lucius's combo AI

Written without a compiler for the whole project; the pure classes were compiled and unit tested with a JUnit shim, the Minecraft-facing
classes were type-checked against API stubs, nothing ran in game. This is a REPLACEMENT of the older "the engine lends bosses Tensura
skills, including Instant Regeneration" design (the old code stays, behind switches and filters) and of Lucius's move chooser (the old
chooser stays as the fallback). Everything else is new.

## Config: `nusmp-boss-rules-server.toml`, section `[bossRules]`

| Key | Default | What it does |
|---|---|---|
| `stripRegeneration` | true | No regeneration skill or effect on a boss; no heal while the boss is in combat. |
| `banUniqueUltimate` | true | A boss never owns or uses a Unique or Ultimate skill. |
| `forbiddenWords` | `regenerat,heal,recover,revive,resurrect` | Comma separated words that add to the built-in regeneration family (a plain string, parsed by the pure policy; the built-in words can never be removed from here). |
| `luciusComboAi` | true | Lucius uses the combo planner; false = the old weighted chooser. |
| `refillAtGates` | true | The Gates (100% mode) refill a Zogratis boss's bar once. See "The one exception". |

## What each class is for (the WHY)

* **`boss/rules/BossSkillPolicy`** (pure). The single source of truth: `isForbidden(skillId, category[, settings])`. Regeneration is a
  word match (Tensura names the family `self_regeneration`, `ultraspeed_regeneration`, `infinite_regeneration`, `full_recovery`,
  `healing`, `healer`, `healthcare`, the effect `instant_regeneration`; a later Tensura or another mod adds more, and a word catches
  those too). Unique/Ultimate is a category match, because the category is what the skill *is* in Tensura's own model. An id whose
  category could not be read fails *closed*: a Tensura id in the table of known Unique/Ultimate skills is forbidden. Other mods' ids
  are never guessed at. `isForbiddenEffect` covers the engine's permanent Instant Regeneration and Minecraft's own healing effects.
* **`boss/rules/BossHealPolicy`** (pure). "A boss in combat gets 0 from a heal": three booleans and a clock (a target, or hurt in the last
  200 ticks), testable without a world.
* **`boss/rules/BossRulesConfig`**. The switches above in their own spec file, so the owner can tune the rules without touching the
  other boss configs. Reads fall back to the defaults before the config loads.
* **`boss/rules/BossSkillRules`** (glue). Reads the category from the real object: Tensura's `Skill.getType()` (UNIQUE, ULTIMATE), then
  the class package (`...skill.unique.`, `...skill.ultimate.`), then "unknown" for a Tensura class of no known family. Magic and
  Battlewill are not `Skill`s, so spellcasting is never caught by the ban. Provides `refuses` (a grant), `vetoes` (an activation) and
  `sweep` (take forbidden skills off a boss). Never touches a player.
* **`boss/rules/BossIdentity`** (glue). One answer to "is this a boss of this mod": the budgeted bosses (`BudgetedBoss`: the Zogratis
  family, Zagred), registry-listed bosses (`BossEntities`), the Floor Warden, the Blood-Red Commander (`MonarchBoss.is`), tagged
  dungeon bosses (`nusmp_floor_boss`, `nusmp_juggernaut`, `nusmp_charybdis`) and the entity type tag `nusmp:boss` (new file
  `data/nusmp/tags/entity_type/boss.json`, every entry `required: false`, so an addon can opt a type in and a missing id is harmless).
  Tensura's and vanilla bosses are never touched, the same isolation rule as the buff engine.
* **`boss/rules/BossHealGuard`** (glue). Decides "drop this heal now". `allow(Runnable)` is the whitelist (a ThreadLocal depth so
  it cannot leak to another entity's heal and always unwinds).
* **`boss/rules/BossRulesHooks`** (glue). `register()` wires: the NeoForge `LivingHealEvent` (cancel and zero for a boss in combat), the
  ManasCore `SkillEvents.ACTIVATE_SKILL`, `RELEASE_SKILL`, `TOGGLE_SKILL` (cancel for a boss and a forbidden skill, the pattern
  SpellSeal already uses for players), and a sweep: on join, then every 100 ticks over the tracked bosses only (nothing scans the
  world). The sweep also strips the engine's old permanent Instant Regeneration effect from bosses saved under the old design.
* **`boss/LuciusCombo`** (pure). Lucius's combo planner, see below.
* **`data/nusmp/tags/entity_type/boss.json`**. The opt-in tag described above.

## Where the rules are enforced (all additive; each is a no-op with its switch off)

1. **Grant path**: `BossSkills.grant` refuses a forbidden skill, which covers the buff engine, Lucius's Absolute Severance and the
   Usurper lease. `LuciusProfile.seen` no longer offers a forbidden skill to the Usurper at all (it would only be refused after the
   cooldown was spent). `TensuraCaster.learn` keeps healing pages out of any boss kit. `BossBuffEngine.ruled` removes forbidden
   effects and skills from the plan: the Instant Regeneration row and its 50%/60% hysteresis are left in `BossBuffTable` untouched,
   they simply never reach a boss; `BossBuffTable.surplus` takes the effect off a boss that already had it.
2. **Cleanup**: `BossSkillRules.sweep` on join and every 100 ticks, plus once per engine refresh.
3. **Runtime veto**: the three ManasCore skill events above. Note `TensuraCaster.cast` drives a mob's magic by calling
   `onPressed`/`onRelease` directly (no event), but it can only hold Magic-class skills, filtered at `learn`.
4. **Healing loops**: `LivingHealEvent` (the framework's passive regeneration `tuning.regenFractionPerSecond`, Zagred's Ultra-Speed
   Regeneration pulse, Vanica's blood drain, Lucius's Soul Drain and Soul Purge, his Body Magic mending, any mod's potion) and the
   direct upward `setHealth` in `BossBudget.setHealth` (Time Reversal's rewind heal, Dante's Body Magic pulses). A reload (`bypass`) and
   the scripted refill (`dpsBudget.bypass`) pass. The reset of an abandoned fight (`resetFight`, healing to full when nobody is left)
   is wrapped in `BossHealGuard.allow`, because that is not a second wind. Messages that would lie ("the purge returns N health")
   are guarded by `BossHealGuard.mayHeal`. Dante's mortal-wound pulse (`bodyAnswers`) is skipped as a whole while `stripRegeneration`.
   Lucius's `spiritual_health_regeneration` +400 modifier is removed (spirit damage now lasts) while the switch is on.
   Limit: the Blood-Red Commander and Juggernaut are driven mobs, so only `heal()` calls reach the guard, not a direct `setHealth`; their
   own refill writes health directly and is the scripted one.

## Skills a boss keeps and loses

* Keeps: the barrier effects (magic, physical, haki coat), `tensura:mortal_fear`, the Severance Blade effect, the attack-profile
  engravings, the anti-magic aura, spellcasting through the Towers (every non-restricted magic), Tensura Magic-class kits.
* Loses: `instant_regeneration` (Devil Avatar, Lucius), `starved` and `mathematician` (Unique, the engine's theme skills; Dante, Vanica,
  Zenon, Goliath, Charybdis Spawn keep only what is standard), `absolute_severance` (Lucius's Unique; the Severance Blade effect
  stays), any Unique copy the Usurper would have lent, the spiritual regeneration bonus, every heal in combat.

## Lucius's combo AI

The scheduler still decides what is *allowed* (power gate at 20/40/60/80%, range band, mana, ultimate pacing), so the HP phases widen
the roster exactly as before. `LuciusCombo` decides which allowed move comes next. Each of his 25 moves has a role:

| Role | Moves |
|---|---|
| SPATIAL | Beyond Light, Beyond Light: Barrage, Swap Strike, Dimension Cutter |
| OFFENSIVE | Soul Motes, Soul Drain, Bone Arsenal, Blood Tide, Megiddo, Judgment of Light, Soul Purge, Sibling Assimilation |
| DEFENSIVE | Mirror Court, Chrono Acceleration, Soul Magic: Paladins, Time Reversal, Resurrection (of his Paladins) |
| CONTROL | Lucifero's Weight, Memory Manipulation, Chrono Stasis, True Darkness, Chrono Rings, Time Stop |
| MAGIC | The Towers Answer, Grimoire Tower Storm |

A combo is four casts: SPATIAL, OFFENSIVE, DEFENSIVE or CONTROL (alternating combo by combo), then one MAGIC cast. The extra heavy
cast is the Towers: the existing `TowerSpells` deck draws a page from every non-restricted `MagicType` (the 36+ magics) before any
repeats, with the magic's own colour and rider, so no new cast API was needed; healing pages are filtered out of its library. A move
is never chosen inside its cooldown or among the last min(3, pool - 1) casts; when the pool is thin the rules relax in a fixed order
(role, then last-cast, then cooldown) and a non-empty pool never gives nothing. If no move is allowed (target out of every band) the
answer is still -1 and he closes in, as before. Seeded, so a fight is reproducible. The hook is `ZogratisBoss.chooseMove` (default =
the old scheduler draw) overridden by `LuciusBoss`; any planner error falls back to `super`.

## The one exception to "no second wind", for the owner to decide

The owner's earlier explicit request (the 100% / Gates mode refills the bar once, `PhaseRefill`) is kept. It writes health directly
inside the budget bypass and never goes through a heal call, so the guards do not touch it. `ZogratisBoss.refillOnGates()` now returns
`refillAtGates` (default true). Set it to false to remove the last restoration; the Juggernaut and Zagred refills (their own
`refillForMode`/`refillTick`) are separate and not switched by this key. **Decision needed:** keep the refill at the Gates (true), or
turn it off for a pure no-restoration fight?

## The unified curve

No new stat system. Every source of damage, whichever mod it comes from, arrives at a boss through `hurt()` and meets the same three
gates; the only thing that differs between Tensura, tensuranoindex, trnightmares and our relics/Curios/5e weapons is how much they
put in front of the gates.

| Stage | Knob (where) | Scales with | Why it is there now that the regen is gone |
|---|---|---|---|
| Damage in | `BalanceLaw.epScale` (addon spells, weapon techniques, relic acts): x1 at 1k EP, x3.5 at 10k, x6 at 100k, x8.5 at 1M, x11 at 10M, max x12; Tensura skills and the other mods scale by their own EP rules | the attacker's max EP | The curve the other mods already share; nothing added. |
| Mitigation | `LuciusMitigation` (Lucius only): magic 45%, physical 30%, spatial 60%, never more than 75% off, never under a quarter of the hit; times `bosses.buffMultiplier` (0..3); lifted while he is exposed or Julius holds the body | the global multiplier, not the attacker | Flat on purpose: a high-EP build is not punished twice, the budget handles it. Replaces the immunities that would have made the fight unwinnable. |
| Barriers | `BossBuffTable` levels of magic barrier, physical barrier, haki coat (0..4 times the multiplier), suspended while exposed | boss tier (1 floor .. 4 Lucius) | Thin shell on floor bosses, thicker on the Triad, Avatar and Lucius. |
| Step guard | `PowerCurve.clampDamage` | the boss's next gate line | No phase can be skipped by one big hit, so every phase's spells and combo are seen. |
| Time floor | `BossBudget` / `BudgetPolicy.capPerSecond`: 6% of max health per second (min 20 hp/s, x1.5 in the final phase, half of it the phase before) | the boss's max health | With regeneration gone this is the whole "how long does it take" rule: a max-EP build cannot kill any boss faster than about 17 s (11 s in the final phase), a weaker build takes health / its own dps. Healing used to be able to undo the cap; now it cannot. |
| Threat out | `DamageMath.victimFactor`: x1 up to 1,000 EP, +0.5 per decade, x3 at 10M; then `BalanceLaw` adds a sliver of max health and caps one hit at 40% of a player's max health (`pvpHitCapPercent`) | the victim's EP | A high-EP build feels up to 3 times the base hit, nobody is one-shot, whatever the mod their power comes from. |
| Pressure | the boss's `PowerCurve` tuning (damage, cooldown, wind-up, speed) per 20/40/60/80/100% step, `ultimateGapTicks` | the boss's own health | Replaces regeneration as the thing that makes the second half of the fight harder. |
| Anti-ultimate aura | `BossBuffEngine.aura`: a cooldown (2 to 3 s) on a player's Ultimate skill, never a removal; players with Anti-Magic exempt | tier | Unchanged. The bosses now refuse Ultimate skills themselves, the aura is the matching limit on the players' side. |

Checked for a gap and left every number as it was: the one asymmetry is that the attacker scale reaches x11 while the threat out
stops at x3 (so above 1M EP a build out-scales the boss's hit roughly 3 to 4 times over), but the flat budget floor and the 40% hit cap
are exactly the parts that do not depend on EP, so a high-EP build still meets a 17 s floor and a 40% hit. Raising `dpsCapFraction`
or `dpsCapFinalPhaseScale` (existing keys of `nusmp-boss-budget-server.toml`) is the lever if the owner wants longer fights; changing
the factor shape would touch every Zogratis fight, so it is listed as a decision, not changed.

## Uncertain and not checked in game

* The class and enum used (`io.github.manasmods.tensura.ability.skill.Skill.getType()`, `SkillType.UNIQUE/ULTIMATE`) and the
  ManasCore `SkillEvents` signatures were read from the API dump; the fail-closed id table contains Ultimate ids that were guessed
  from the ids used in the UsurperPolicy tests.
* The skill-event veto cancels an activation by a boss; whether a mob's skill ever goes through that event (rather than the direct
  calls) was not run.
* Zagred's, the Juggernaut's and the Commander's behaviour without their old heals (an easier or harder fight) was not played.
* "heal" as a substring also matches ids such as `health_...`; the owner can read the exact list in `BossSkillPolicy.words`.

Owner decisions: the Gates refill (above); whether Lucius's four `ultimate` moves (Soul Purge, Sibling Assimilation, Time Stop, Grimoire
Tower Storm) should be renamed or removed too. They are this mod's own move-framework pacing label, not Tensura Unique/Ultimate skills,
so they stay.

## 1.42.0: the second exception (Lucius's Time Reversal) and the arc

The owner asked for Lucius to fight like in the final arc (`docs/lucius_arc.md`). One part of it is a deliberate exception to "no second wind",
like the Gates refill: in act 2, after a burst of damage (18% of his maximum health in 100 ticks) he channels **Time Reversal** for three
seconds and, if the party does not interrupt it (anti-magic, a heavy blow, a lock), returns to the health he had when the burst began, never
above it and never above the top of act 2. Why it still fits the rule: it is a scripted restoration, not a regeneration. It runs through
`BossHealGuard.allow(...)` and the budget's bypass exactly like `PhaseRefill`, so `BossHealGuard` stays the single whitelist; with the new
switch `luciusTimeReversal` (`BossRulesConfig`, default true) off he never rewinds anything. `luciusAnimeArc` (default true) switches the
whole final-arc Lucius; with it on, the Gates' 100% refill is **not** used for Lucius (his arc has its own comeback tool and its own end),
and `refillAtGates` keeps its meaning for every other boss and for Lucius with the arc off.

