# Catch audit (follow-up of 4.80.1)

Why: 4.80.1 narrowed the 29 Tensura max-EP reads from `catch (Throwable)` to `RuntimeException | LinkageError` and said the other broad catches and the
empty catches were not touched. This pass takes the most dangerous 60 of them (server side, per tick, events, data loading first). A `catch (Throwable)` also swallows
`OutOfMemoryError` and `StackOverflowError`, so a dying server keeps limping; an empty catch hides a real bug. `RuntimeException | LinkageError` is what a missing or
changed Tensura / Curios class really throws (`NoClassDefFoundError`, `NoSuchMethodError`, `ClassCastException`, a null, a missing config value). Success path and
returned fall-back values are unchanged everywhere. Time Magic files were not touched. No version bump, no changelog, nothing committed.

Counts: 246 `catch (Throwable` before, 196 after (50 narrowed). 60 sites changed in 37 files.

## What changed

### Empty or comment-only catches (16): narrowed, and where a bug would hide, logged once

New helper `core/CatchLog.once(key, throwable)`: logs the first failure of a key with its stack at WARN, drops the rest; at most 64 keys, so it is bounded.

| File | Was | Now |
|---|---|---|
| `labyrinth/mega/enc/WandererGuard.java` (3: target, heal, drops) | `Throwable`, empty | `RuntimeException \| LinkageError` + `CatchLog.once` (the guard still fails open, same as before) |
| `quests/QuestWorldEvents.java` (3: boss death, arena watch, advancement) | `Throwable`, empty / "ignore" | narrow + `CatchLog.once` |
| `avatar/item/GenesisCoreDrops.java` | `Throwable`, empty | narrow + `CatchLog.once` |
| `avatar/party/AvatarPartyManager.java` (party tax) | `Throwable`, empty | narrow, kept empty (no Tensura attribute: the party costs nothing) |
| `entity/TensuraCaster.java` `ensureMana`, `teach` | `Throwable` (`{}` / logged) | narrow, same body |
| `item/DemonSlayerSwordItem.java` `genesis` | `Throwable {}` | narrow, one-line reason |
| `scalecast/ScaleCastKit.java` `weightOf` | `Throwable { }` | narrow, one-line reason |
| `book/MirrorWorks.java` | `Throwable {}` | narrow, one-line reason |
| `labyrinth/mega/gen/ExpansionQueue.java` (inner "tick must never throw") | `Throwable` | narrow (the outer catch logs the first failure and stays) |
| `core/magic/profile/GenericFallbackFx.java` | `Throwable`, empty | narrow (an Error now reaches `SafeRunner`, which records it) |
| `item/CuriosLink.java` `isRendered` | `Throwable`, empty | narrow (draw it) |
| `relics/RelicPowerSelfTests.java` | `Throwable`, empty | narrow |
| `manacore/ManaCoreRuntime.java` `addMagicule` | `Throwable` | narrow |
| `entity/ZagredBossEntity.java` (summon count) | `Throwable {}` | narrow, one-line reason |

### Silent reads of Tensura data (31): narrowed, same fall-back value

`mana/zone/ManaZonePlayer.java` (4: skills, quest count, floor, king), `mana/zone/ManaZoneManager.java` (2), `antimagic/Nullification.java` (3: two skill lists, summon check),
`boss/BossBuffEngine.java` (aura jam), `ultimate/UltimateManager.java` (9: mastery, rank step, king, heritage, cover, seal check, cooldown, mana law, balance law),
`relics/scaling/AuraCache.java` (2), `mana/method/RuneManager.java` (3: anti-magic check keeps its fall-back `true`, damage source `null`, resistance `0.0`),
`shishou/ShishouBorrow.java` (a borrowed skill that throws: false), `labyrinth/mega/safe/RestMana.java` (2: test setter false, share -1.0),
`core/magic/profile/EnumMagicProfile.java` (2: wiki flag false, colour fall-back), `entity/ZagredDefense.java` (summon: false), `book/KotodamaWords.java` (summon: false).

### Data files that fell back silently (10): exception type kept, now logged once

`Exception` is not an `Error`, so the type stays; what was missing is a trace when a bundled JSON is broken: `relics/GradeDamage.java`, `relics/djinn/DjinnExtremeTypes.java`,
`devil/phase2/Phase2Table.java`, `devil/mastery/DevilKit.java`, `devil/dual/DevilRanks.java`, `devil/dual/DarkRitualRules.java`, `relics/Rankings.java`,
`spiritdark/SpiritDarkVoiceCatalog.java`, `arc/kakashi/KakashiLines.java`, `labyrinth/mega/guard/AbyssalLooks.java`. Each now calls `CatchLog.once("<file>", e)` and falls back exactly as before.

### Test pin changed on purpose

`ShishouReworkTest.noOptionalModIsClassLinkedAndTheGlueGatesOnTheModList` asserted the text `catch (Throwable` in `ShishouBorrow`; it now asserts `catch (RuntimeException | LinkageError`.

## What was kept, and why (all listed in `docs/broad_catch_allow.txt`)

196 `catch (Throwable` remain in 96 files:

* Self-tests and diagnostics (`*SelfTest*`, `SelfTests`, `GeometryCheck`, `CuriosProbe`): a throw is reported as a failed check, so they must see everything.
* Logged firewalls around a scheduled effect, tick or event (boss `*Moves` / `*Fx` / `Decrees` / `Strikes`, `labyrinth/mega/*` directors and generators, `labyrinth/squad`, `multiverse/*`,
  `quests/NusmpEvents`, `book/ext/AttributeEvents`, `skill/ResetGuard`, `GrimoireGuard`, Avatar hooks): the failure is already logged and the effect is stopped; deliberate. They are the next place to
  look if a stricter rule is wanted (an Error there would crash the tick instead of stopping one effect).
* `core/magic/profile/SafeRunner`: the magic safety net, it rethrows `VirtualMachineError` itself.
* Reflection into optional mods (`CuriosLink`, `NightmareSouls`, `compat/create/*`): the try bodies throw checked reflective exceptions; narrowing needs `ReflectiveOperationException | RuntimeException | LinkageError`, left for a pass that can be tested against the real mods.
* `client/` (12 `Throwable`, plus the `Exception` catches): pure render / lookup fallbacks, not server state, skipped on purpose. Empty client ones seen: `client/ui/CodexPages` (2), `WeaponGrip:170`, `WeaponRenderer:755, :823`, `playermodel/BcAudit:90`.
* NEXT PASS (silent, mechanical, left only because of the 60-site cap; marked `NEXT PASS` in the allow-list): `dragonballs/DbConfig` (16 config reads; `RuntimeException` is enough), `avatar/TensuraBridge` (8 of 16), `boss/BossSkills` (4), `boss/BossSkillRules` (3),
  `boss/BossBuffConfig` (3, one is reflection), `boss/LuciusProfile` (3), `dungeon/MealSkills` (3), `core/magic/profile/MagicProfiles` (2), `core/magic/profile/MagicAuditRuntime` (1), and one each in
  `AvatarEntity`, `ForgeSkillResolver`, `CharterMirror`, `DungeonFood`, `LuciusCapabilities`, `ManaMethodPages`, `RuneManager:474`, `GrandZones`, `TrueMagic`, `Wave7Events`, `AllFictionSkill`, `Paladins`, `SpiritControlPages`.
* `catch (Exception` empties left: `book/pages15/Pages15SelfTests`, `ultimate/UltimateSelfTests` (self-tests, a lang file that is unreadable shows up as missing keys). The `valueOf` parsers (`Devil`, `MagicType`, `GrimoireCover`, `ForbiddenMagic`, spirit lord kinds)
  catch `Exception` to mean "bad name": harmless, could be `IllegalArgumentException`. `FranchiseMap/Data`, `CodexNames`, `TensuraNotes` already catch `Exception | LinkageError` (silent fall-back to an empty table; could log).

## The new scan test

`src/test/java/com/newuniverse/nusmp/core/BroadCatchScanTest.java` reads every file of `src/main/java` (comment lines ignored) and fails when:

1. a file has more `catch (Throwable` (also `final`, also inside a multi-catch) than `docs/broad_catch_allow.txt` allows for it (`path:count  # reason`, default 0; a ceiling, so it can only go down);
2. an allow-list line is stale (file gone, or no `catch (Throwable` left in it: remove the line), or has no reason;
3. any file catches `OutOfMemoryError`, `StackOverflowError` or `VirtualMachineError`.

When a NEXT PASS site is narrowed, lower or remove its allow-list line in the same change.

## Cost

No runtime cost: catch clauses only. `CatchLog` does one set lookup on an already failing path, at most 64 keys, one log line per key.

## Risks

* A narrowed catch now lets any non-`RuntimeException`, non-`LinkageError` throwable through. In the event hooks (`WandererGuard`, `QuestWorldEvents`, `GenesisCoreDrops`) and the per-tick Tensura reads (`UltimateManager`, `ManaZonePlayer`, `ShishouBorrow`, `RuneManager`) that means an `AssertionError` or an `OutOfMemoryError` is no longer swallowed and reaches the server; that is intended. A Tensura or Curios call that throws some other custom `Error` (not a `LinkageError`) would also now escape; none is known.
* `ShishouBorrow.skill` runs another mod's skill code: a skill of a third mod that throws an `Error` outside `LinkageError` now reaches the tick (before it was swallowed). Same for `TensuraCaster.ensureMana`.
* `RuneManager.antiMagicUser` keeps its fail-safe `true` only for runtime and linkage failures.
* Not run in a game; verified by compiling the 37 changed files with javac and running the whole test suite (12,629 tests, 0 failures).
