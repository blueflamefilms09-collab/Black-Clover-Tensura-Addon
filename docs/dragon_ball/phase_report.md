# Dragon Ball rework, phases 2 to 6, Giant Ki Avatar and the Saiyan race stages: report

Status: built on the worktree branch, NOT compiled (no Gradle in the session; only the pure classes and the config were compiled with plain `javac` against stubs, and the pure unit test was run). CI is the first real compile. Nothing was seen in a game.
Version, CHANGELOG and push are left to the coordinator (as asked). Lang is in `tools/lang/db_phase2.json` (not merged into `en_us.json`).

## What was built, per phase of `rework_plan.md`

| Phase | Status | What |
|---|---|---|
| 2 Saiyan as a race | PARTLY (fallback, see "Race API") | stages 1 to 4 (Saiyan, Primal Saiyan, Divine Saiyan, Autonomous Martial God) as a saved number next to the flag, rising by rule, buffs as transient attribute modifiers, `/saiyan stage`, operator `/saiyan stage <player> <n>`, server switch `phase.race.realRace` (OFF) |
| 3 Physical techniques | DONE | Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct |
| 4 Ki techniques and God Ki | DONE (God Ki as a radar mask only) | Charged Ki Sphere, Ki Wave (hold, release, channel), Ki Reinforcement, Afterimage Technique, Gravity Training, Super Saiyan God ritual (`/saiyan ritual`), Divine Saiyan gift (tame animals), God Ki hidden from a Saiyan's radar who has none |
| 5 Forms | PARTLY | Kaio-ken x10 and x20 behind a switch (the three old stages are bit-identical), mastered-form drain rule. NOT built: the seven new form rows (Primal, Berserk x2, ape x2, mastered Super Saiyan, Time Power), their looks, the berserk flicker and the moon gate are only rules (pure, tested), no form uses them yet |
| 6 Polish | NOT built | Ultimate rows for Vegeta/Broly/Gohan/Frieza, radial wheel, cooldown lines in the HUD, sounds, Blender hair/fur/tail/avatar meshes |
| T3 Saiyan techniques III | DONE | God Bind and Dragon Fist already existed (kept, untouched); NEW: Giant Ki Avatar (server rules, `GIANT_AVATAR` shape, `GiantAvatarLayer`), the Tensura damage source helper `SaiyanDamage`, self-tests `saiyan.t3.*` |

## NEW / REPLACED / KEPT (rule 5)

NEW: `SaiyanNet2`, `SaiyanPhaseRules`, `SaiyanRaceRules`, `SaiyanRitualRules` (pure), `SaiyanPhaseConfig` (`nusmp-saiyan-phase2-server.toml`), `SaiyanTechniques4`, `SaiyanDamage`, `SaiyanRace`, `SaiyanRitual`,
`SaiyanPhaseSelfTests` (`db.phase2.rules|race|player`, `saiyan.t3.rules|player`), `vfx/client/layer/GiantAvatarLayer`, `VfxShape.GIANT_AVATAR` (appended after `SKILL_SIGIL`), four client keys, `tools/lang/db_phase2.json`, tests `SaiyanPhaseRulesTest`, `SaiyanPhaseSourceScanTest`, this report.
Smoke list: `db.phase2,saiyan.t3,` inserted right after `saiyan.kivfx,` in `.github/workflows/smoke.yml`.

REPLACED: nothing was removed. Three existing numbers were widened without changing old results: `SaiyanForms.kaioDamageMul/kaioDrainMul/kaioStrainRate` tables now have 5 entries (stages 1 to 3 identical), `SaiyanState.MAX_COOLDOWNS` 16 to 32 (the new techniques need cooldown slots; old saves read the same), the stage clamp 3 to 5.

KEPT: the Saiyan flag and the questionnaire race, every older technique and key, `SaiyanNet.valid` (the old range check is untouched: new asks 22 to 35 go through `SaiyanNet2` and `SaiyanManager.onActionPhase`), the form table, the look table, `DbSkills`, all pinned messages.

Edited existing files (small, additive): `SaiyanState`/`SaiyanTag` (`raceStage`, `gravitySecs`, saved), `SaiyanAttributes` (`applyTech`, `applyRace`, cleared with the rest), `SaiyanManager` (`Run.tech4`, tick, second, cancel, `onActionPhase`, clean-up), `SaiyanNet` (route), `SaiyanCombat.defend` (one hook after the dodge), `SaiyanFormEngine` (Ki Reinforcement factors, Kaio stage cap through the switch, drain rule), `SaiyanForms` (Kaio tables), `SaiyanSenses` (God Ki mask), `SaiyanCommands` (`ritual`, `stage`), `SaiyanBoot`, `SaiyanTechniques3.lockFoe` (private to package), `SaiyanClient` (keys), `VfxManager` (registration), tests `SaiyanSourceScanTest` (lang fragment, new self-test file allowed), `GreedVfxAssetTest` and `SkillAnimeVfxAssetTest` (SKILL_SIGIL no longer the last shape), `PerfSourceScanTest` (hot render list).

## Keys (all rebindable; G stays Ki Flight)

`Num 6` next technique (shows the name), `Num 5` use it, `Num 7` Ki Wave (hold, release), the branch key is unbound. The cycle covers Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct, Charged Ki Sphere, Ki Reinforcement, Giant Ki Avatar, Afterimage, Gravity Training.

## OWNER DECISIONS (all are server switches, all default to the safest answer)

1. Kaio-ken x10 and x20: `phase.owner_decisions.kaiokenExtraStages` = false (x2 to x4 as before). On: x10 from 85% mastery, x20 at full mastery, strain 9 and 18 a second.
2. Ape forms: `apeForms` = false. No ape form row exists yet either; the moon rule (full moon at night, or an artificial moon) is ready and tested. The owner said no ape forms in 3.65.0.
3. Existence Points numbers for the Super Saiyan God ritual: `ritualUsesExistencePoints` = false (Battle Power 1,000,000 is used). On: 100,000,000 EP (`ritual.existencePoints`). EP is read with `EnergyHelper.getMaxEP` like the balance law.
4. Branch key: `branchKey` = false (the spec's G is Ki Flight here). On: the key is an alias of "next form" (the branch idea is not modelled).
5. Awakening hook (keep Saiyans from the True Demon Lord / True Hero awakening): `blockAwakening` = false and NOT built: no Tensura awakening event could be found or read in this build, so the switch is inert. A Tensura "evolve subordinates" call was not found either.
6. New: `race.realRace` = false (the stages are inactive until the owner decides), and `senses.godKiMask` = true (God Ki is hidden from a Saiyan's radar who has none: tell the owner, it changes what the radar shows only for God forms).

## Race API (what could and could not be verified)

Seen: the readable jar `tensuramoreskills-0.0.5.jar` names `RaceRegistry.RACES` (a Registrar), `DefaultRace`, `RaceConfig$Default`, `Alignment`, `EvolutionRequirement` (javap only). Not readable in the session: the Tensura Reincarnated jar, ManasCore and Architectury sources (they resolve from CurseForge only in CI), so the exact constructor, registration timing, evolution API and the datapack format of a race could not be verified, and the repo's `multiverse/Race` enum has exhaustive switches. A race registered blind could crash the server at load, so it was NOT done.
Built instead (documented fallback): the Saiyan flag stays the race; a saved stage 1 to 4 on top of it; buffs by transient attribute modifiers (attack, speed, health, armour, knockback, small, scaled by `race.buffScale`); `/saiyan stage`; operator set. Not done: an entry in the multiverse `RaceBuffs` and the race-select screen. To finish later: register `DefaultRace` ids `saiyan`, `primal_saiyan`, `divine_saiyan`, `autonomous_god` once the API can be compiled against, and read `SaiyanState.raceStage` for the stage.

## Wiki research (rule 10)

Not possible: the English and Japanese wiki fetches were blocked in this session (HTTP 403 through the proxy). Technique and form names, numbers and ordering come from `rework_plan.md`, the owner's spec notes in the repo and the existing Saiyan docs. Nothing was checked against a wiki page.

## Server and cost (rules 9 and 11), worst case

All asks arrive on the existing one-byte `saiyan_action` packet (rate limited to one per 4 ticks per player, release never dropped); no new packet. Every rule is on the server; the client only draws. State is per player UUID (`Run`), dropped on death, logout and dimension change; the ritual set holds UUIDs only.
Per Saiyan: Meteor Strike 1 lookup and 1 blow; Vanishing Step 1 raycast and at most 16 collision checks per use; Aerial Rush 1 entity query and at most 3 blows; Ground Breaker 1 query and at most 8 blows, no block touched; Charged Ki Sphere at most 60 ticks, 1 block read a tick and 1 entity query every second tick, 1 blast of at most 8 targets, never loads a chunk; Ki Wave at most 100 ticks, 1 ray every 4th tick, at most 5 effect packets; Giant Ki Avatar at most 900 ticks, one blast of at most 6 foes a second, ONE effect packet (the avatar is a drawn effect, never an entity); toggles cost a few comparisons a second; gravity training is capped by the existing daily training caps. Whole server: at most `ritual.maxAtOnce` (3) rituals, each one player query of at most 8 helpers every 10 ticks. Client: at most 32 quads (128 vertices) per avatar, distance culled by the effect budget, nothing allocated per frame beyond the vectors of the existing layers. No entity is spawned and no block changed by any of it.
Damage: Ki techniques use Tensura `MAGIC_GENERIC` through the balance law (switch `phase.tensuraDamageSource`); physical ones are the Saiyan's own blow (`hurtBlow`).

## Caveats (honest)

Nothing was compiled with the real dependencies or run in a game. Items most likely to need a fix on first CI: sound constants and `Level.hasChunkAt`/`Vec3.multiply` signatures (checked against how the same classes are used elsewhere in the repo), the `SaiyanPhaseSelfTests` stand-in code (copied from `SaiyanTech3SelfTests`), `GiantAvatarLayer` (draws with the same buffer calls as `KiLayer`, never seen), the unbound branch key.
Not done: Blender models (no Blender here; the avatar is built from ribbons and glows with KiLayer's existing textures), previews of the new visuals, new form rows and looks, wiki check, HUD cooldown lines, the radial wheel, Ultimate rows, multiverse race entry, awakening hook, Time Power forms (they need their own aura; the Time Magic VFX is off limits).
