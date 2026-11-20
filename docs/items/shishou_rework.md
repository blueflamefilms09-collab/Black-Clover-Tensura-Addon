# Shishou rework: pet mode, canon skills, borrowed skills, original abilities

Code: `src/main/java/com/newuniverse/nusmp/shishou/` (new: `ShishouPet`, `ShishouPetGoals`, `ShishouCanon`, `ShishouAbilities`, `ShishouBorrow`, `ShishouBorrowTable`, `ShishouVfx`; changed: see the table). Data: `src/main/resources/data/anime_multiverse/shishou/borrowed_skills.json`.
Tests: `ShishouReworkTest` (pure and source scans), self-tests `relic.shishou_pet`, `relic.shishou_canon`, `relic.shishou_borrow`, `relic.shishou_abilities`. The 3.x description stays in `docs/items/shishou.md`.

**Nothing here has been seen in a running game.** What was checked: the whole `shishou` package, the client HUD and `ShishouUltimateSkill` compile against the real jars, and 206 JUnit tests (all Shishou tests, the source scans of the perf and server-safety packages, config file names, ultimate pages, tooltips, magic enum audit) pass. The self-tests only run in CI on a real server; they were compiled here, not executed.

## NEW / REPLACED / KEPT (rule 5)

| | what |
|---|---|
| NEW | Pet mode of the thrown sword (follow / hunt / guard / retreat / stay brain, owner-target goals, wall avoidance, orders). |
| NEW | Seven skills appended at bits 31..37: Self-Repair, High-Speed Self-Repair, Self-Evolution (passives, canon), Telekinetic Grasp (canon), Akashic Index, Blades of the Record, Akashic Sanctuary (original). Two Ultimate-skill modes appended: Akashic (the four new active skills) and Borrowed. |
| NEW | Borrowed skills: a data table of 35 entries (15 base Tensura, 4 TR Nightmares, 4 Tensura Mysticism, 12 grimoire echoes of this mod), learned by Skill Eater (a kill) or by level, cast with the sword's mana by the thrown sword on his own, or by the wielder from the Borrowed mode. |
| NEW | Commands and keys: `/multiverse shishou order follow\|stay\|attack`; Telekinesis mode + sneak cycles the order while he is thrown; Borrowed mode casts the picked skill, with sneak it picks the next. |
| NEW | Server settings: `pet.mode`, `canon.skills`, `compat.borrowedSkills`, `compat.tensura`, `compat.nightmares`, `compat.mysticism`, `compat.multiverse`, `compat.borrowGapTicks`. |
| NEW | Lang keys (`tools/lang/shishou.json`, `shishou_skill.json`, `shishou_rework.json`, merged append-only into `en_us.json`), a record save form with three new keys (`bmask`, `bsel`, `ord`). |
| REPLACED | Nothing. Every old skill, mode, bit, key and save field stays. |
| FIXED | A Wind Magic cast from the thrown body (no wielder in hand) flung the wielder: `ShishouSpells` now only pushes the wielder when the cast is made by the wielder and not by a body. |
| KEPT | The old follow/hunt AI (runs unchanged when `pet.mode` is false), the 3.x mana numbers (when `canon.skills` is false), all 31 old skills with their costs and cooldowns, the Stomach, the one-per-world rules, Thought Communication, every existing config key. The Time Magic files were not touched. |

One old test pin changed on purpose: the castable cycle at level 21 now ends with Telekinetic Grasp (appended after Stomach) instead of Stomach, and the skill count pin is 38 (`ShishouProgressionTest`, `ShishouUpdate21Test`).

## 1. Pet mode (setting `pet.mode`, default on; off = the old AI)

Only when the sword is a thrown minion (`ShishouEntity.isPet()`).

- Follows the wielder to a shoulder spot; starts catching up at 9 blocks (4 while guarding or retreating); when stuck for 40 ticks and 10+ blocks away he blinks to the shoulder if the spot is free.
- Hunts hostiles near the wielder (the existing leash and hunt radius), and takes what the wielder punched (`getLastHurtMob`) and what attacked the wielder (`getLastHurtByMob`), for up to 160 ticks, checked every 5 ticks.
- Never attacks: tamed animals, pets and allies of anyone, players, the wielder, peaceful creatures (`ShishouTargets.protectedKind` / `petMayAttack`). A target the wielder named is still refused if it is protected.
- Low mana: below the retreat threshold he drops the target, stays by the wielder (GUARD/RETREAT, follow distance 4) and says so once; he resumes when the mana is back (`ShishouPet.decide`, pure and tested).
- Walls: one ray per two ticks (max 12 blocks, never past the aim point, only into loaded chunks) turns the charge along the wall instead of into it.
- Orders: FOLLOW (default), STAY (holds place, strikes only what comes within the stay guard), ATTACK (hunts past the usual leash). Kept in the record, restored on throw, saved in the entity NBT.

## 2. Canon abilities (setting `canon.skills`, default on; also each skill has its own switch under `[skills]`)

Compared with the 31-skill table, the canon abilities that were missing are the four below. Nothing existing was changed against canon, because the compared skills matched; the setting only gates the new numbers.

| skill | canon | what it does here | cap |
|---|---|---|---|
| Self-Repair (jisoku shufuku, level 15) | he mends himself | passive: mana regeneration x1.5 | none |
| High-Speed Self-Repair (level 40) | faster mending | passive: mana regeneration x2 in all | none |
| Self-Evolution (level 60) | he keeps growing | passive: +10% experience, and a free mana step every 25 kills (the steps are capped by the rules) | steps capped by the existing rules |
| Telekinetic Grasp (nennou, level 12, 8 mana, 80 ticks) | telekinesis | lifts up to 4 creatures (Levitation 30 ticks, then slowed 60 ticks, small damage), range 22 | 4 targets |

Canon sources: **the English and the Japanese wiki hosts could not be reached from this environment (DNS failure), so these canon facts are from memory of the series and are unverified.** Names and kanji of the new skills in the lang files are likewise unverified. (Rule 10: nothing could be read from either wiki; nothing is cited.)

## 3. Original abilities (not canon, with effects through `PerfParticles`)

| skill | level | mana / cooldown | effect | worst case |
|---|---|---|---|---|
| Akashic Index | 30 | 10 / 200 | names up to 6 hostiles within 24 blocks (Glowing 100 ticks); the wielder's and the thrown sword's blows on them deal +15% for 200 ticks | 6 effect applies; the named-table is capped at 16 entries |
| Blades of the Record | 55 | 35 / 300 | five waves of spectral blades, one every 4 ticks, on up to 6 targets within 6 blocks of the aimed point, each wave 30% of a strike | 5 x 6 = 30 hurts, at most 60 particles per wave |
| Akashic Sanctuary | 70 | 50 / 900 | a field of 5 blocks around the wielder for 100 ticks: pulses every 10 ticks heal the wielder a little and slow and scratch hostiles; one at a time per wielder; ends on logout, death or dimension change | 10 pulses x 12 touches = 120, at most 60 particles per pulse |

## 4. Borrowed skills (settings `compat.*`)

The table `borrowed_skills.json` lists, per entry: source mod, kind (a skill id, or an "echo" of one of this mod's magics), level to have it by level (0 = only by eating), mana, cooldown, keywords for Skill Eater (matched against the dead creature's skills and name), damage share. Loading is bounded (at most 64 entries; a broken entry is skipped, not fatal).

- Soft compat: the optional mods are never class-linked. A skill is looked up by id string in the Tensura skill registry, only if `ModList.isLoaded(namespace)`; if either is missing the entry is just unavailable. Switches: `compat.borrowedSkills` (all), `compat.tensura`, `compat.nightmares`, `compat.mysticism`, `compat.multiverse`.
- Learning: by sword level, or by Skill Eater when a creature he killed matches an entry (message "I have eaten ...").
- Casting: the thrown sword casts one borrowed skill per `compat.borrowGapTicks` (default 40, minimum 10) against his target, only within 24 blocks and never within 5 blocks of the wielder for skill-kind entries; per-entry cooldown and the sword's own mana. The wielder can cast the picked one from the Borrowed mode (only while he is thrown; held, he lends nothing and says so). Tensura skills are driven through `TensuraCaster.cast` with the body as the caster; a throw is caught and costs nothing but the attempt.
- Echoes are the sword's own effect (a particle and a hit of the matching magic type on up to the spell-target cap of hostiles within 3 blocks), no external call.

## 5. Optional rogue-boss encounter

Not built. It was optional and not cheap: it would need a boss entity, an arena or spawn rule and rewards, none of which is bounded by what the sword code already caps.

## 6. Fixes and self-tests

- Fixed: the Wind Magic bug above.
- Self-tests (all under the existing smoke prefix `relic.shishou`, so `.github/workflows/smoke.yml` needed no change):
  - `relic.shishou_pet`: valid and invalid targets (hostile, cow, tamed wolf, player), owner-target goals, orders and their cycle, retreat and resume, stay range.
  - `relic.shishou_canon`: regeneration factors, Self-Evolution experience and the 25th-kill step, the setting off restoring the old numbers.
  - `relic.shishou_borrow`: table loaded, steal table, per-entry cast with cooldown and no double cast, the gate between casts, a held sword lends nothing.
  - `relic.shishou_abilities`: each new skill fired on a group of zombies within its caps, then every castable skill through the dispatcher without a throw, and that a body's Wind spares its wielder.

## 7. Worst-case cost (rule 11)

There is one Shishou per world, so every "per sword" figure is also the per-world figure.

- Server per tick: the pet brain adds one wall ray per 2 ticks (10 rays per second, max 12 blocks, loaded chunks only); target scanning reuses the existing single box query (radius up to 48, once per ~6 ticks); the owner-target goal reads two fields every 5 ticks. No new world scans, no chunk loads, no entity spawns.
- Borrowed cast: at most one per `borrowGapTicks` (default 40 ticks = 0.5 per second; the setting's floor of 10 ticks = 2 per second), a loop over at most 64 table entries with the cheap checks first.
- Abilities: Grasp 4 targets; Index 6 targets and 16 table entries; Blades 30 hurts per cast; Sanctuary 120 touches per cast, one cast alive per wielder; all scheduled on `SpellRuntime` and cleared on logout, death and dimension change (`ShishouLive.forget/clearAll`).
- Network: no new packets and no new payloads. Visuals go through `PerfParticles` to nearby players only (at most 60 particles per step, 20 for an echo).
- Client draw: unchanged HUD; no new renderers or models.
- Save: three small longs added to the record's flat map; old saves load (tested), bad values are clamped.

## 8. Not verified / not done

- Nothing was run in a game. The look of the effects, the pet's flight feel, the follow distances and the numbers of the abilities are unplayed.
- The Nightmares and Mysticism skill ids and the mod namespaces (`trnightmare`, `mysticism`) are from memory. A wrong id only makes that entry unavailable; fix it in `borrowed_skills.json`.
- Whether a Tensura skill cast by a Mob (the body) behaves the same and spares the wielder is unknown; the 5-block safe distance and the catch are the mitigation.
- The HUD does not name the picked borrowed skill; the pick shows in the action bar when changed.
- Canon: no wiki could be reached (English and Japanese both failed); everything canon is from memory. Kanji of the new skills in the lang files are unverified.
- The Better Combat file, engravings and Blender models: not needed, because no new weapon or item model was added (the sword itself is unchanged).
- Not done on purpose: no version bump, no changelog, no commit, no push (asked by the coordinator).
