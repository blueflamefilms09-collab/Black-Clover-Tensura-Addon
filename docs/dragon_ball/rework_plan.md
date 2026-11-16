# Dragon Ball rework, plan (task #29), phase 1

Status: phase 1 = this plan, plus the removal of the 15 old Dragon Ball skills (done, section 5), plus a design for "Saiyan as a real race" (section 6, not built).
Nothing here was seen in a game and Gradle was not run; CI is the only compile and test.

## 1. What was read

| Source | What it is | Read |
|---|---|---|
| `5c96048e-...goku_progression_core_codebase.md` | Spec A: Human to Saiyan 4-stage racial tree, 16-form table (no ape, no Kaio-ken), Java for `GokuForm`, `SaiyanRaces`, `SaiyanData`, Zenkai, combat handler, UI auto-dodge | all |
| `b0f54cc2-...codebase2.md` | Spec A, second version: same plus branch switch (`G`) / sub-form cycle (`Shift+G`) / revert (`X`), `FormControlPayload`, `FormTickHandler` (Aura drain), Goku skill table (Kamehameha, Instant Transmission, Spirit Bomb, Dragon Fist, Giant Ki Avatar) | all |
| `e7684215-...anime_accurate_abilities_visual_fx.md` | Spec B: 3-phase cinematic pipeline, `KamehamehaBeamEntity`, `SpiritBombEntity`, Dragon Fist, camera shake, per-form aura particles | all |
| `e7e56476-...ssg_ritual_divine_awakening_perks.md` | Spec C: block True Demon Lord / True Hero for Saiyans, 5-player Super Saiyan God ritual, Divine Saiyan gifts, God Ki scan masking | all |
| `26f03c5a-...blender_3d_models_form_renderer.md` | Spec D: Blender asset list for 16 forms (OBJ/GLB, GeckoLib), `BlenderModelRegistry`, `SaiyanBlenderRenderLayer` | all |
| `b6e82b73-...Saiyan_Race_System...pdf` | Spec P: the original "Warrior's Ascension" prompt, parts 1 to 10 and the opening of 11 (it is cut off in the middle of the VFX list) | all 13 pages |
| `83a681f1-kaioken_wig...glb`, `84bf8f3f-false_super_saiyan_wig...glb` | two hair meshes (not named in the task, found next to the specs) | listed only, not opened |

Wiki sources (rule 10): none were fetched in this phase. Every fact is from the owner's files and from this repo. Nothing could not be fetched because nothing was tried; the canon checks (form order, Time Power / Limit Breaker lore) belong to phase 3 and 4, English wiki first, Japanese wiki as fallback.

Important: the code in specs A to D is written for the mod id `db_tensura` and for Tensura classes that this repo does not use and that do not exist under those names here (`com.github.manasmods.tensura.race.Race`, `TensuraRaces.RACE_REGISTRY_KEY`, `TensuraEPCapability`, `TensuraPlayerCapability.getAura()`, `AwakeningEvent`, `SkillEvolutionEvent`, `TensuraTamableEntity`). This build uses `io.github.manasmods.tensura.*` and `io.github.manasmods.manascore.*` (see `docs/dragonball_skills.md`, `CrownAwakening.java`, `RaceBuffs.java`). The specs are read as design, never pasted as code ("do not invent APIs", PDF part 1).

## 2. What the mod already has (so nothing is built twice)

The Saiyan system is large and shipped (docs: `docs/saiyan.md`, `docs/saiyan_tech3.md`; code: `src/main/java/com/newuniverse/nusmp/saiyan/`, 6,100 lines, 13 test classes, self-tests `saiyan.rules`, `saiyan.player`, `saiyan.stage2`, `saiyan.goku`, `saiyan.tech`, `saiyan.tech3`, `saiyan.look` in the smoke list).

* Race: a **flag on the player** (`/saiyan race <p> on`), not a registered race (the `Race` enum, questionnaire and race pool are untouched, on purpose, see `docs/saiyan.md`). Ki is the Tensura magicule.
* Battle Power with six ranks, anti-farm rules; Ki Awakening; Ki Blast, Rapid Barrage; Ki Flight (5 tiers, server speed check); Saiyan Senses (16/32/64/128) and Ki Suppression; Warrior's Reflex, Battle Adaptation, Zenkai (with daily cap and near-death rules); four training meters.
* `SaiyanUltimates` registry (race-restricted rows), one row: Son Goku, King of Martial Arts, with awakening needs and the Trial of the Martial King.
* Goku techniques: Kamehameha (hold/release, ray), Rock Scissors 'N' Paper, Ki Barrier, Instant Transmission, Spirit Bomb (particle bomb, friends make it stronger), Meteor Combination, Dragon Fist, God Bind, Ki Deflection, Energy Transfer.
* Form engine (`SaiyanForms.ALL`, 16 forms plus Base, every number a server setting): Kaio-ken, False SS, SS, SS Full Power, SS2, SS3, SS4, Super Full Power SS4, SS4 Limit Breaker, SSG, SSB, Perfected SSB, SSB Kaio-ken, UI Sign, Perfected UI, True UI. No ape forms (owner's earlier decision).
* Look: armour pieces, fur and tail, aura, eyes, charge-up, animated power-up clip (`SaiyanLook`, `client/aura/SaiyanAura.java`, `tools/gen_saiyan_armor_geo.py`). **No hair** (parked in `tools/saiyan_look_parked/`).
* 15 old Dragon Ball character skills (`DbSkills`/`DbRules`): now removed from play, section 5. The older single `KiSkill` ("Ki", Dragon Ball Ultimate, 3.47.0) is a different thing and is untouched.

## 3. Feature table: NEW / REPLACED / KEPT

Rule 5 check: every spec feature was compared with the list above. "KEPT" = the repo already has it and the spec adds nothing that must replace it; the spec's numbers are NOT adopted (see section 4, they use Tensura EP and Aura, the repo uses Battle Power, the balance law and magicule).

| Spec feature | Spec | Verdict | What happens |
|---|---|---|---|
| Saiyan race, starting bonuses, Ki, Battle Power ranks | P part 2, 4 | KEPT | built in 3.63.0, all values configurable |
| Warrior's Reflex, Battle Adaptation, Zenkai, Saiyan Senses, Ki Awakening | P part 2 | KEPT | 3.65.0 |
| Universal physical techniques: Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct | P part 3 | NEW | phase 3 (not in the repo; "Meteor Combination" is a Goku move, a different thing) |
| Ki techniques: Blast, Barrage, Barrier, Deflection, Suppression, Flight, Energy Transfer | P part 3 | KEPT | Charged Ki Sphere, Ki Wave as a separate technique and Ki Reinforcement are NEW (phase 3) |
| Human to Saiyan evolution tree, 4 racial stages (Awakened, Primal, Divine, Autonomous) | A | NEW | needs the race to be a real race first, phase 2 (design in section 6) |
| Battle Power as the progression number | P | KEPT | spec A/B use EP as the number; the repo's decision (Battle Power, balance law) stays |
| 16-form table with EP multipliers (`GokuForm`) | A | KEPT (data), REPLACED never | the repo already has a 16-form roster with its own numbers. The spec's `Required EP` / `Power Multiplier` / `Aura drain` columns are not adopted (they would bypass the balance law, section 7). Forms missing from the repo are added as rows (next lines) |
| Mastered Super Saiyan (Grade 4) | A | NEW | one row; "zero drain at 100% mastery" is already how mastery works (mastered form drains 60%), so the row only adds the zero-drain rule |
| SS Grade 2, Grade 3 | A (md), P | NEW | rows with the Grade 3 speed/attack-speed penalty (the SS3 row already shows negative speed bonuses, the pattern exists) |
| SS2 Berserk, Blue Berserk | P | NEW | rows, "reduced control" = a capped random Ki-flicker, no input theft |
| Time Power SS4, Time Power Blue | P | NEW, **blocked on rule 3** | needs its own time-looking aura; the Time Magic VFX may not be touched or copied (section 7) |
| Kaio-ken x10, x20, SSB Kaio-ken x10/x20 | P | NEW (extends KEPT Kaio-ken, SSB Kaio-ken) | rows or new strain steps; spec A/D say "no Kaio-ken", spec P says yes, the repo already has it (section 4) |
| Oozaru, Golden Great Ape | P | NOT BUILT | owner said "no ape forms" in 3.65.0 and spec A/D repeat it; P still lists it. Stays out unless the owner reverses it |
| Branch switch `G` + `Shift+G`, revert `X`, `FormControlPayload` | A2 | REPLACED (only if the owner asks) | the repo already has form on/off, next/previous keys and `/saiyan form`; `G` is already Ki Flight (section 4). Proposal: a radial form wheel in phase 5, additive, old keys stay |
| God Ki as its own system (masks EP from scans) | A2, C, P | NEW | phase 4: a flag on the God branch plus a hook in Tensura's appraisal; the hook API must be found first |
| SSG 5-player ritual, Divine Saiyan | C | NEW | phase 4: server-only, bounded, UUID-keyed; the repo has the old Super Saiyan God form but a plain unlock, no ritual |
| Block True Demon Lord / True Hero for Saiyans | C | NEW, risky | needs a cancellable Tensura awakening event; unknown in this build (section 7), phase 4 only after the event is found |
| Divine gifts: evolve subordinates, spiritual lifeform, 2x/2.5x pools | C | NEW | phase 4, numbers need an owner decision (it multiplies magicule/aura bases, which the owner's balance law caps) |
| Ultimate Skill registry, Goku path | P part 5, 6 | KEPT | `SaiyanUltimates`; Vegeta, Broly, Gohan, Frieza rows come later, one row each |
| Instant Transmission | A2, P | KEPT | present (ray to a creature, blink) |
| Spirit Bomb | B, P | KEPT | present as a server-light particle bomb; spec B's `SpiritBombEntity` (48-block scan every tick, `level.explode` TNT-level) is NOT adopted (section 7) |
| Kamehameha | B, P | KEPT, plus visuals NEW | present (charge, ray, ranges). NEW in phase 5: per-form beam colour (blue, crimson 10x in SS4, God red/blue, silver), client-drawn beam, ring pulses |
| Dragon Fist | B, P | KEPT, visual NEW | present (lock, 6-step winding, blow). Client painter for the golden dragon is NEW in phase 5 (the doc says it is missing) |
| God Bind | P | KEPT | present |
| Giant Ki Avatar (MUI/TUI) | A2, B, D | NEW | phase 5, client layer only, one translucent mesh, plus a small server rule (reach, limited time) |
| Afterimage Technique | P | NEW | phase 5, client-only visual (the doc says it is not built) |
| Gravity training, Divine Training | P part 10 | NEW | phase 3/4; the training meters exist, the gravity room does not |
| Zenkai (healing from <15% HP, +2% EP) | A | KEPT | repo version is stricter (daily cap, diminishing); spec's `LivingHealEvent` gain would be an infinite farm and is NOT adopted |
| Per-form hair / fur / tail / aura shell meshes made in Blender | D | NEW (hair) and REPLACED (fur/tail/armour geometry) | hair is NEW (parked, plus the two uploaded GLB). Fur/tail/armour exist as script-made geometry (`tools/gen_saiyan_armor_geo.py`); rule 14 wants Blender, so the Blender versions are added next to them and the old ones stay (rule 2) until the owner approves the swap |
| Camera shake on release | B | NEW, small | client setting, bounded, default low |
| Terrain craters from the beam, TNT-level Spirit Bomb | B | NOT BUILT | breaks claims and perf (section 7); the repo's rule is "no block touched" |
| The 15 Dragon Ball character skills | (owner's order) | REMOVED from play | section 5, ids kept |

## 4. Conflicts between the specs, and between the specs and what exists

1. **Kaio-ken and ape forms:** A and D say "no Ape, no Kaio-ken". P lists Oozaru, Golden Great Ape and Kaio-ken x2..x20. The repo (owner's decision, 3.65.0) has Kaio-ken and no ape. Plan: keep Kaio-ken (extend it), keep ape out. The owner should confirm.
2. **Number system:** A/B/C/D drive everything from uncapped Tensura EP times a form multiplier (up to 500,000,000x) into bonus damage. The repo's rule is the balance law (PvP hit cap, boss caps, mob multiplier) and Battle Power as a label. The spec's multipliers are not adopted.
3. **Aura vs Ki:** A2 drains Tensura "Aura" each tick. The repo's decision (owner) is "Ki is the magicule, no second pool". The spec's drain per tick is converted to the existing per-second Ki drain of each form row.
4. **Keys:** A2 puts the branch switch on `G`; `G` is Ki Flight in the repo (11 Saiyan keys already, all rebindable).
5. **Race source:** A/D want real races (`awakened_saiyan` ...); the repo uses a flag. Resolved in section 6.
6. **Hair:** D wants hair meshes in Blender and GeckoLib; the repo parked hair for lack of a texture/mesh, and has no GeckoLib player layer for it.

## 5. Phase 1 deliverable: the old 15 Dragon Ball skills are removed from play

The owner's order: remove all old Dragon Ball skills. CLAUDE.md rule 2 says old data is kept for save compatibility, so "remove" is built as removal from play, not deletion of ids.

| Where | Change |
|---|---|
| `NUConfig.DB_LEGACY_SKILLS` (`dragonBallLegacySkills`, SERVER config, default **false** = removed) | the one switch; `true` restores the 3.56.0 behaviour (`dragonBallEnabled` still applies on top) |
| `DbSkills.node()` | the `/multiverse dragonball` node is hidden (`requires` reads the setting lazily, the server config is not loaded when commands are built); `give` also refuses, belt and braces |
| `DbSkills.evolve` (called from `SkillEvolution`, line ~68) | returns at once while removed, so nothing evolves |
| `DbSkill` (`onPressed`, `onTick`) | read `DbSkills.active()` = enabled and not removed: a copy that a player still holds (for example given by `/tensura edit`) refuses every mode and its passives do nothing |
| `DbSkills.onLogin` (registered in `NUSMP` next to the Meal skill listeners) | at login, once, a player who owns any of the 30 ids has them forgotten through the normal `forgetSkill` (this runs `onForgetSkill`, which clears the passive modifiers) and gets one grey message. Cost: 30 map lookups per login, no tick work, no packets unless something was taken |
| Kept, untouched | `DbSkills.registerAll` (`NUSkills.DRAGON_BALL`), `DbSkill`, `DbRules.SPECS` (15), `DbMoves`, `DbPassives`, the 30 icons, `tools/lang/dragonball.json` and the lang keys, `docs/dragonball_skills.md`, the `skill.db` self-test and its smoke prefix, `BeastSkillRules` (re-uses `DbRules` and `DbMoves`) |
| Not hidden | the registry entries themselves: Tensura's `/tensura edit ... ability grant` list still suggests the ids, because hiding a registry entry means unregistering it, which breaks old saves. A grant there is inert (see `DbSkill` row) and is taken at the next login |

Tests: `DbRulesTest`, `SkillAssetsTest`, `BeastSkillRulesTest` are unchanged and stay valid (SPECS is still 15, the ids are still registered, the icons are still there). New: `DbLegacyRemovalTest` (source scan: default false, server config, every entry point reads the switch, ids/classes/icons kept). The `skill.db` self-test still checks the 30 registry entries and now also prints the new setting. I did not add a stand-in-player self-test: the existing ones (`ResetGuardSelfTests`) note that a stand-in often cannot hold a mod skill in CI, so such a test would prove nothing.

Worst case (rule 11): login 30 lookups per player once; every 5 seconds the evolution call returns on its first line; no new packet, no entity, no tick work.

## 6. Saiyan as a real race: design (not built in phase 1)

Step 3 of the task said: add the race only if the repo already uses a Tensura race registration API; otherwise write the design. **The repo reads Tensura races (`RaceAPI.getRaceFrom`, `TensuraRace`, `RaceHelper`) but registers none**, so the design goes here.

What is known about the real API (from the bytecode of an uploaded third-party addon, `tensuramoreskills-0.0.5.jar`, not copied, used as evidence of names only):
* races are registered in Architectury's `Registrar` from `io.github.manasmods.manascore.race.impl.RaceRegistry.RACES`, each a `RegistrySupplier<ManasRace>`;
* a race class extends `io.github.manasmods.tensura.race.template.DefaultRace` and overrides `getDefaultConfig()` (`RaceConfig.Default`), `getBaseAuraRange()`, `getBaseMagiculeRange()`, `getAlignment()`, `getNextEvolutions`, `getPreviousEvolutions`, `getEvolutionRequirements` (a map of `EvolutionRequirement` to a number), `getIntrinsicSkills`, `getDefaultEvolution`, `getHarvestFestivalEvolution`;
* its stats come from a config class (so the race is tunable).
None of these signatures were compiled against here, so phase 2 starts by reading them with a compile check, not by writing them blind.

Design:
1. Four races, appended to the registry only: `saiyan` (stage 1, evolves from Human), `primal_saiyan`, `divine_saiyan`, `autonomous_god` (names as in spec A; the spec's "Awakened Saiyan" becomes `saiyan`, because a Saiyan is born one). The Human to Saiyan step is the existing flag turning on (`/saiyan race on`, or evolution), so the questionnaire and the `Race` enum are untouched.
2. The existing `SaiyanState.saiyan` flag stays the source of truth for every current rule. The race is added next to it: `SaiyanManager` sets the Tensura race when the flag turns on and the race is Human (and clears nothing when it turns off). A server setting `saiyanRealRace` (default **false**) gates all of it, so today's behaviour is the default and a failure of the race path cannot break the flag path.
3. The evolution requirements map to existing numbers, not to the spec's EP: stage 2 = Battle Power and SS3 mastery, stage 3 = the SSG ritual (phase 4), stage 4 = near-death plus Perfected UI. All are server settings.
4. Branch unlocks (Primal, God Ki, Autonomous) read the race stage as one more gate in `SaiyanForms` (`branch needs stage n`), default off until the setting is on.
5. Risks to settle before building: how Tensura saves a race for a player when the mod is removed; whether a registered race appears in Tensura's race-selection screens (it must not appear for new players); whether Harvest Festival / hero awakening can be cancelled for this race (needed by spec C, section 7).

## 7. Things in the specs that conflict with the repo rules

| Rule | Where the specs conflict | Handling |
|---|---|---|
| 3: Time Magic VFX untouchable | P/B: Time Power SS4 and Time Power Blue "time-themed energy effects" | build them with a new aura (own texture and particle set); never reuse `TimeMagicLayer`, `time_*.png` or `gen_time_vfx_textures.py` |
| 14: models in Blender | A2/D: GeckoLib and OBJ exported from Blender: fine. But the repo's current armour/fur/tail are script-built (`gen_saiyan_armor_geo.py`) and the spec's hair would be hand-placed | all new meshes (hair, fur, tail, aura shell, avatar) go through `tools/blender/bl_lib.py`; the two uploaded GLB wigs are imported into Blender, unwrapped and baked there, not loaded raw. If Blender is not available in a session the item is reported as "could not be made in Blender" |
| 9: server safety | B: the bomb and beam entities run `getEntitiesOfClass` and `level.explode` each tick on the server; D/B `Minecraft.getInstance()` and client classes live in event handlers; A: `LivingHealEvent` gain; A2: `player.attack(attacker)` and teleport inside a damage event | all state stays on the server, keyed by UUID; client code only in `client/`; the bomb stays the repo's bounded version; no teleport or attack inside a damage event (it can re-enter the event) |
| 9: validated packets | A2 `FormControlPayload` (`action` int, no range check, no rate limit) | any new action goes through the existing `saiyan_action` packet (range-checked, rate-limited, sender must be a Saiyan) |
| 11: no lag | B: 2 to 3 particles per 1.5 blocks over a 72-block beam every tick, rings every 6 blocks, 12 explosion emitters; `gatherGenkiFromSurroundings` over 48 blocks every tick; ritual loop; `evolveAllSubordinates` over a 256-block box | beams/spheres are drawn by the client from one small packet to nearby players; server work is one ray and one query per release; particle counts through `Fx`/`PerfParticles` budgets; subordinate evolution capped (at most N per ritual, owner-listed only) |
| 9/11: protected claims | B: `level.setBlockAndUpdate(... AIR)` craters, `level.explode(... TNT)`; PDF part 7: "must not destroy protected claims" | no block changes (repo rule); if craters are ever wanted, a setting default off, claim checks, and a per-event block cap |
| `NoHungerDrainTest` | P part 8: Kaio-ken "drains stamina", "exhaustion"; part 10: gravity penalties | "stamina" is the mod's own meter (`SaiyanState.stamina`), never the food bar; no `addExhaustion`, no `setFoodLevel` decrease, no Hunger effect. "Exhaustion" in Kaio-ken means the existing strain meter and a max-stamina reduction. The test scans `src/main/java` and will fail the build if a phase uses food APIs |
| 12/rule 6: engravings, 13: Better Combat, 7: weapon models | the specs add no weapons, but Goku's power pole (Nyoibo) and Z-sword are common asks | out of scope; if added later they follow rules 6, 7, 13 |
| 2: keep old / add new | A/D rename races and keys, B rewrites skills as entities | additive only; old keys, ids, forms stay |
| 4/12: version, changelog, release | this phase must not bump the version or edit CHANGELOG (the order of this task) | the first releasing phase does both |
| 10: wiki research | spec form order and lore checks | done per phase, source named in each doc; none in phase 1 |
| "do not invent APIs" (P part 1) | all spec code uses `com.yourname.dbtensura` and Tensura classes that do not exist here | every Tensura/ManasCore call is checked against the repo's own imports or the real jars before use |

## 8. Phased build order

Each phase is one release, additive, behind a setting that restores the old behaviour, with pure tests, a self-test and the cost note in its doc (rule 12).

* **Phase 1 (this one):** plan, removal of the 15 old skills, race design.
* **Phase 2: Saiyan as a real race.** Section 6, setting `saiyanRealRace` default off. Self-test `saiyan.race`. Cost: one race lookup per Saiyan per second at most.
* **Phase 3: missing combat content.** Physical techniques (Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct), Charged Ki Sphere, Ki Wave, Ki Reinforcement, gravity training. Each is an op on the existing `saiyan_action` packet. Wiki check first (rule 10).
* **Phase 4: forms and divinity.** Missing form rows (Grade 2/3, Mastered, SS2 Berserk, Blue Berserk, Kaio-ken x10/x20); God Ki scan masking (find the Tensura appraisal hook); SSG 5-player ritual and Divine Saiyan gifts (bounded: one ritual per server tick budget, subordinate cap); blocker for hero/demon-lord awakening only if a cancellable event is found. Time Power forms last, with their own aura (rule 3).
* **Phase 5: visuals in Blender.** Hair (from the two GLB wigs and `tools/saiyan_look_parked/`), SS4 fur/tail, aura shell, Giant Ki Avatar (client layer, one mesh, 12-block scale, budgeted quads), Afterimage, per-form beam colours and a client Dragon Fist painter, camera shake. Report which models were made in Blender.
* **Phase 6: Ultimate path and polish.** Vegeta/Broly/Gohan/Frieza rows in `SaiyanUltimates` (one at a time), sounds, HUD cooldown lines, radial form wheel, wiki-checked names and descriptions, balance pass with an owner play-test.

## 9. Worst-case cost of the whole rework (target, rule 11)

| Part | Server per tick | Network | Client per frame |
|---|---|---|---|
| Today (3.71.0, see `docs/saiyan.md`) | bounded per Saiyan, no spawned entity | about 4 small packets a second per Saiyan, nearby only | under 190 quads per Saiyan in full detail, none beyond 64 blocks |
| Phase 2 race | 1 lookup per Saiyan per second | none | none |
| Phase 3 techniques | one ray and one query (at most 64 candidates) per use, cooldown-limited | the existing action packet, 1 byte | none |
| Phase 4 forms and ritual | 16 rule checks per 5 s per Saiyan (as now); ritual: one active ritual per caster, at most 6 players read per tick, a cap on how many rituals run at once | 1 chat line per player, no new packet | none |
| Phase 5 visuals | none (the server sends the form code it already sends) | none new | hair, tail, shell: a few hundred quads per Saiyan, LOD at 32 blocks, none beyond 64; the avatar one mesh with a quad budget, only for the local player and players within 48 blocks; no per-frame allocation (pooled vectors) |

A part that cannot be shown to stay within these numbers does not ship.

## 10. Decisions the owner still has to make

1. Kaio-ken and ape forms: keep Kaio-ken and no ape (the repo today), or follow spec P and add Oozaru and Golden Great Ape?
2. Number system: confirm that the spec's EP multiplier table is not adopted and the repo's Battle Power and balance law stay.
3. Whether the Saiyan should become a real Tensura race (phase 2) and whether it may be chosen at the start or only reached by `/saiyan race on`.
4. Whether the key `G` stays Ki Flight (the repo) or becomes the form branch key (spec A2).
5. Hero and demon-lord block for Saiyans (spec C): wanted even though it needs a Tensura event this repo has never hooked?
