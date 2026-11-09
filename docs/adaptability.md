# Adaptability (Unique skill, 3.70.0)

A Unique, toggleable skill that "evolves through punishment". Built from the owner's reference: the Adaptability skill of the uploaded `tensuramoreskills-0.0.5.jar` (read for its behaviour and its default numbers; our code is our own, nothing is copied). No wiki was consulted (rule 10 does not apply: the reference was a file).

## What it does (while the toggle is on)

| Part | Rule |
|---|---|
| Damage learning | Every hit you take adds one **stack** for that kind of damage (its damage-type id, e.g. `minecraft:player_attack`). Each stack takes 0.7143% off the next hit of that kind, but the hit never falls below **75%** (so at most 25% off, reached at 35 stacks). Stacks are kept per kind. |
| Self mastery | Each time a new stack is learned the skill gives itself mastery (1.0; +2.0 once mastered; at most 6.0). |
| Severance | Tensura's Severance builds up on a body. After **25** Severance hits you adapt: the built-up Severance is cleared and Severance damage is blocked (a plain physical blow is not). A message tells you. |
| Resistance learning | The learning progress of Tensura resistance skills is multiplied by **3.45** (**5.85** once mastered). |
| Global mastery | Every 100 ticks (5 s) every learned, not yet mastered skill gets +0.5 mastery (+2.0 once mastered), at most 24 skills per pass, the window turning each pass so a longer list is covered in turns. |

The record (stacks, Severance hits, the immunity flag) lives in the skill instance's own data, so it saves with the player and goes away with the skill. At most **16** kinds are remembered; a new kind forgets the one with the fewest stacks.

## Getting it

`/multiverse adaptability give <player>` (operators), or Tensura's own `/tensura edit <player> ability grant nusmp:adaptability`. Press the skill's key to toggle it. Skill id `nusmp:adaptability`, icon `textures/skill/adaptability.png` (`tools/skills/gen_adaptability_icon.py`).

## Settings

Server file `serverconfig/nusmp-adaptability-server.toml`, table `[adaptability]` (every number clamped again when read): `enabled`, `damage.damageReductionPerHit`, `damage.minimumDamageMultiplier`, `damage.maxStacksNormal`, `damage.maxStacksMastered`, `damage.severanceHitsForImmunity`, `learning.resistanceLearningMultiplier`, `learning.resistanceLearningMultiplierMastered`, `learning.globalMasteryIntervalTicks`, `learning.globalMasteryBonus`, `learning.globalMasteryBonusMastered`, `learning.maxSkillsPerPass`, `learning.adaptiveMasteryBaseBonus`, `learning.adaptiveMasteryMasteredBonus`, `learning.adaptiveMasteryMaxBonus`. `enabled = false` turns every effect off.

## Not in this release

The reference's **Extermination Blade** mode (a summoned sword that spends stored EP for bonus damage, with Severance / Tsukumogami enchants) is not built: it is a weapon, so by rules 6 and 7 it needs a real 3D model, engravings and its own effects, and comes in its own release. The reference's "Divine General Mahoraga" Ultimate (a menu of adaptations with a toggle each) is also not built. The reference's default for the acquisition cost (90000 magicules) is not used: the skill is granted, not bought.

## Where the code is

`adaptability/AdaptabilityRules` (pure maths), `AdaptabilityConfig`, `AdaptabilitySkill` (toggle, global mastery pass), `AdaptabilityEvents` (damage hook, resistance-learning hook), `AdaptabilityTensura` (Tensura's Severance helpers, reached by name), `AdaptabilityCommand`, `AdaptabilityBoot`, `AdaptabilitySelfTests` (`adaptability.rules`). Registered in `NUSkills.ADAPTABILITY` (last, so no older order moves). Tests: `AdaptabilityRulesTest`, `AdaptabilitySourceScanTest`, and `SkillAssetsTest` (icon + lang).

## Worst-case cost (rule 11)

- **Per hit on a player without the skill or with it off:** one skill-map lookup. Nothing else.
- **Per hit with it on:** one copy of the record (at most 16 entries) and a handful of cached reflective calls (three, only for Severance), no world scan, no entity, no packet. At most 16 + 2 NBT writes.
- **Per tick:** nothing except every 100th tick a `markDirty` (one skill sync packet per toggled player per 5 s) and every `globalMasteryIntervalTicks` (default 100) one pass over at most 24 learned skills (one list copy).
- **Memory:** a 16-entry compound per skill holder; a static class map of at most one entry per registered skill class.
- **Network:** at most one skill sync per toggled player per 5 s.
- **Client:** no drawing.

## Honest caveats

- Not seen in a game; the Gradle build was not run locally.
- Three Tensura hooks are reached by name (so a Tensura update cannot break the build): the Severance helpers `TensuraDamageHelper.isSeveranceDamage(source, victim, boolean)` / `isPhysicalAttack`, `TensuraStorages.getEffectFrom(...)` + the effect's Severance setters, and the `TensuraSkillEvents.SKILL_LEARNING` event. The names were read from the reference jar; if any is not found in the installed Tensura the server logs one warning and only that part switches off.
