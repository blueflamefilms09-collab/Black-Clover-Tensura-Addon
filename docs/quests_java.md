# Quests (Java side): the `nusmp:event` trigger, fire points, quest installer

Release 1.49.0. Nothing here has been run in the game. The data side (advancement JSON, FTB Quests SNBT pack) is in `docs/quests.md` and
`docs/advancements.md`; the contract between the two is `docs/quests.md` (event key table) and this file.

## What is new, what is replaced
All NEW. Nothing was replaced or removed. Existing code only received one-line, additive `NusmpEvents.fire(...)` calls (list below); behaviour is unchanged.

## Files (package `com.newuniverse.nusmp.quests`, tests under `src/test/java/.../quests`)
| File | Why it exists |
|---|---|
| `NusmpEventTrigger` | The advancement trigger type `nusmp:event` (codec: `key`, optional `count` default 1, optional `arg`, optional vanilla `player`). One trigger for everything vanilla cannot see. |
| `NusmpEvents` | The door the game code calls: `fire`, `fireOnce`, `fireTeam`, `fireAll`, `count`, `futureBoss`, `init(modBus)`. Counters live in the player's persistent data (`nusmp_quest_counts`), copied on `PlayerEvent.Clone`. Server thread only (hops there otherwise), ignores null/fake/client players, swallows and logs its own failures once. |
| `EventCounters` | Pure: which counters one event bumps (`key` always, `key|arg` when an arg is given) and copy-on-respawn. |
| `TriggerRule` | Pure: the matching rule (key equal; criterion arg equal if present; counter >= count). |
| `FutureBossIds` | Pure: `nusmp:future/<arc>_<boss>` id format shared by the generator and the game. |
| `QuestHooks` | Small logic behind a few fire points (cast hook, relic discovery, tier, boss participants) so every call in game code stays one line. |
| `QuestWorldEvents` | Listeners that need no edit inside features: boss death (`boss.defeated`, `dungeon.floor_boss`, `labyrinth.warden_defeated`, peak boss `labyrinth.seal_opened`) and mirroring of the `nusmp:convergence/*` advancements (`convergence.stage`, `convergence.anomaly`). |
| `QuestTeams` | FTB Teams by reflection (like `multiverse.Squads`): the player's team, member ids, online members nearby. Without FTB Teams everything is empty. |
| `QuestsConfig` | `nusmp-quests-server.toml`: `autoInstallOnFirstRun`, `shareSquadCredit`, `shareSquadCreditRadius`. Plain `define`. |
| `QuestsSetup` | One call registering all of it: `QuestsSetup.init(modEventBus, modContainer)`. |
| `pack/PackManifest` | Pure: the marker file `.nusmp_pack` (version, hash, file list) and the content hash. |
| `pack/PackPlan` | Pure: install decision (fresh / over ours / forced backup / refuse), backup name, update plan, uninstall plan, path safety. |
| `pack/PackInstaller` | The file operations on two `Path`s (so tests use a temp dir): install, update, uninstall, status, backups. |
| `pack/QuestsInstall` | Glue: finds the jar's `quest_pack`, the `config/ftbquests/quests` folder, the mod version, asks FTB Quests to reload, the first-run install on `ServerAboutToStartEvent`. |
| `pack/QuestsCommand` | `/nusmp quests status|install [force]|update|uninstall`, operator level 2. |

## Trigger JSON and counters
`{"trigger":"nusmp:event","conditions":{"key":"spell.cast","arg":"thread","count":10}}`
* `key` required; `arg` optional (omit = any argument); `count` optional (default 1).
* One event bumps the counter `key` and, when it has an arg, `key|arg`. A criterion without `arg` reads the plain key counter ("any spell"), one with `arg` reads `key|arg`.
* The criterion completes on the event that makes the counter reach `count`. Counters are not reset by `/advancement revoke` (re-earning is instant if the count is already met) and survive death.
* The plain key is always counted, so there is no separate "also fire the plain key" call.

## Fire points (every edit in existing code; all one line, additive)
| Key (arg) | File:line | Notes |
|---|---|---|
| `grimoire.received` (magic) | `blackclover/GrimoireAcceptance.java:176` | in `grantExact`, the sink of rolls, ceremony, wizard, admin give (an admin give counts too) |
| `grimoire.opened` (magic), once | `book/GrimoireSummon.java:96` | first Summon Grimoire, `fireOnce` |
| `grimoire.kingdom` (clover/spade/heart/diamond), once per arg | `blackclover/GrimoireAcceptance.java:177` | the kingdom of the granted cover; plain counter = number of different kingdoms |
| `spell.distinct` (magic), once per arg | `book/GrimoireBook.java:334` -> `QuestHooks.onCast` | first cast of each magic; the plain counter = number of different magics cast |
| `arena.victory` (`norelic`), team-shared | `arena/ArenaTravel.java:57` (`QuestHooks.arenaEntered`) and `arena/ArenaDirector.java:181` (`QuestHooks.arenaVictory`), plus a 1 s watch in `QuestWorldEvents` | flag set at entry from what the player carries; the watch sets it if a relic is worn or held in a hand during the fight (Relics.equipped = held, armour, Curios; a relic only in the backpack does not count); victory fires `norelic` if the flag is still clear |
| `ceremony.completed` | `multiverse/Ceremony.java:206` | in `Ceremony.finish`, used by altar, ceremony and Tower Wizard |
| `wizard.questionnaire` | `wizard/WizardSystem.java:144` | answers accepted by the wizard |
| `spell.cast` (magic), `spell.cast.signature` (magic), `time.stop` (page) | `book/GrimoireBook.java:334` -> `QuestHooks.onCast` | after a successful cast; signature = page cost >= 35 percent (the mod's signature tier); `time.stop` = Time pages `chrono_stasis`, `grigora` |
| `page.unlocked` (page id) | `multiverse/MasteryPages.java:124` | a page opened by mastery (not the admin command) |
| `mastery.max` (magic), once | `multiverse/MasteryPages.java:107` | the grimoire's mastery bar is full; checked every 2 s |
| `relic.obtained` (relic id), `relic.grade` (grade), `relic.unique_ledger` (relic id) | `core/discovery/DiscoveryTracker.java:94` -> `QuestHooks.onDiscovered` | the first time the player finds a relic item (pickup, craft, any slot scan; also the one-off grandfathering pass on first login after this update, which is intended) |
| `relic.equipped` (relic id) | `relics/Attune.java:46` | "equipped" = attuned (bound) to the player; the mod has no other single equip moment |
| `relic.tier` (grade), `relic.grade` (grade) | `relics/tier/TierRitual.java:172` | tier ritual success; arg = the grade reached |
| `relic.forged` | `relics/forge/RelicForgeMenu.java:223` | a successful Relic Forge action (upgrade, socket, engrave; not draw-out) |
| `weapon.engraved` | `relics/forge/RelicForgeMenu.java:224` | engraving applied at the forge (engravings applied at creation by `WeaponEngravings` are not a player action: not hooked) |
| `relic.socketed` (core key) | `relics/socket/SocketService.java:56` | |
| `relic.djinn_bound` (djinn) | `relics/djinn/DjinnBinding.java:71` | |
| `labyrinth.entered` | `labyrinth/gate/LabyrinthTravel.java:64` | |
| `labyrinth.seal_opened` (`warden`) | `labyrinth/seal/GatekeeperDoorBlock.java:114` | Shadow Key opens a warden door |
| `labyrinth.seal_opened` (`boss`) | `QuestWorldEvents` | the peak boss (tag `nusmp_peak_boss`) dies |
| `labyrinth.warden_defeated` | `QuestWorldEvents` | entity `floor_warden` dies |
| `labyrinth.vault_opened` | `labyrinth/vault/RelicPedestalBlockEntity.java:171` | a relic taken from a pedestal |
| `dungeon.entered` | `dungeon/LivingDungeon.java:79` | the legacy Living Dungeon only (with `labyrinth.engine = labyrinth` the Labyrinth fires `labyrinth.entered` instead) |
| `dungeon.maw` | `dungeon/DungeonMaw.java:62` | the player's magic tore a Maw open |
| `dungeon.floor_boss` (`goliath`/`charybdis`) | `QuestWorldEvents` | entity `goliath` / `charybdis_spawn` dies |
| `arena.entered` | `arena/ArenaTravel.java:56` | each player carried in |
| `arena.hourglass_used` (`enter`/`leave`) | `arena/ZogratisHourglassItem.java:130` | channel finished |
| `arena.victory` (`lucius`), team-shared | `arena/ArenaDirector.java:180` | for each player inside when Lucius falls in the arena (uses `fireTeam`) |
| `boss.defeated` (entity path, e.g. `lucius_zogratis`) | `QuestWorldEvents` | `BossIdentity.isBoss` (all mod bosses, the Floor Warden, tagged dungeon bosses, Zagred ...) or peak boss; credited to killer, recorded fighters (Zogratis bosses) and players within 64 blocks, plus their FTB team |
| `boss.phase` (`lucius:2`, `lucius:3`, `zagred:2..4`) | `entity/zogratis/LuciusGod.java:257`, `entity/ZagredBossEntity.java:621` | the stage/phase that STARTS |
| `spirit.contract` (`spirit`) | `book/SpiritBond.java:119` | a spirit bonds (also reached by the Spirit Lord skill) |
| `spirit.contract` (`devil`) | `book/ForbiddenMagic.java:133` | a devil pact |
| `devil.union` | `book/GrimoireBook.java:548` | Devil Union page cast |
| `squad.created` + `squad.captain` (`founder`) | `multiverse/Squads.java:245-246` | |
| `squad.captain` (`handover`) | `multiverse/Squads.java:275` | only when FTB accepted the transfer |
| `squad.vice` | `multiverse/Squads.java:287` | |
| `squad.joined`, once | `multiverse/Squads.java:342` | seen by the 5 s squad tick for a non-owner member of a flagged squad (so up to 5 s after joining; FTB's own join event is not hooked) |
| `convergence.stage` (stage name), `convergence.anomaly` (name) | `QuestWorldEvents` | mirrors the earning of `nusmp:convergence/stage_*` (arg = text after `stage_`) and every other `nusmp:convergence/*` (arg = its name). The Convergence itself is not in this mod; its data pack grants those advancements. |
| `magic.merged` (merged id) | `core/magic/merge/MergeAccess.java:78` | |
| `weapon.genesis` | `item/DemonSlayerSwordItem.java:149` | every Conceptual Severance of the Genesis Demon-Slayer, by a player |
| `butoh.dance` | `butoh/ButohDance.java:101` | a dance begins (not the quiet resume) |
| `song.sung`, once | `song/SongTicker.java:76` | first pulse |
| `corpse.raised` | `corpse/Corpses.java:214` | |
| `paladin.converted` | `soul/Paladins.java:173` | |
| `rouge.saved` (`caster`/`saved`) | `thread/RougeFateEvents.java:129,131` | Rouge rewrote a fatal or heavy blow; `caster` = the owner, `saved` = another player who was saved |
| `thread.trap` | `thread/ThreadTrapEntity.java:136` | a foe tripped a thread trap |
| `shadow.travel` | `shadow/ShadowTravel.java:71` | |
| `unite.mode` (`canis`/`equus`/`felis`/`gallus`) | `shadow/UniteState.java:61` | |

Not hooked on purpose: `boss.phase` for Dante, Vanica, Zenon, Devil Avatar (they have no stage list that is one clear place; add
`QuestHooks.bossPhase(this, "<id>", n)` where their phase changes). `time.stop` is read from the cast hook instead of editing `book/TimeBook.java` (the owner's rule about the Time Magic files).
No key from qst.md is unhooked.

## Future bosses
`NusmpEvents.futureBoss(player, "<arc>", "<boss>")` awards every remaining criterion of the advancement `nusmp:future/<arc>_<boss>`
(`FutureBossIds` builds the id). A new boss calls it from its death hook for the players who took part, for example:
```java
for (ServerPlayer p : QuestHooks.participants(boss, source)) NusmpEvents.futureBoss(p, "spade_raid", "dante");
```
Existing bosses are NOT given a call: they already report `boss.defeated`, and QST keeps no placeholder for a boss that exists.

## Squad credit
`fireTeam` / `fireAll` also credit the online members of the player's FTB team within `shareSquadCreditRadius` blocks (same dimension) when
`shareSquadCredit` is true. Used for `boss.defeated`, `boss.phase` and `arena.victory`. Everything else is per player. Without FTB Teams only the player is credited.
FTB Quests progress itself is per FTB team already; this makes the advancements (which are per player) agree.

## The installer
`/nusmp quests <sub>` (operator level 2):
* `status`: FTB Quests loaded or not, quest folder state, bundled pack (files, hash, mod version), installed marker (hash, version, file count, up to date or not).
* `install`: copies `quest_pack` from the jar into `<config>/ftbquests/quests`. Folder missing/empty: install. Folder has our marker: reinstall (copy backup first). Folder holds foreign files: REFUSED with a message; `install force` moves the old folder to `config/ftbquests/quests_backup_<UTC timestamp>` first. Writes `.nusmp_pack`.
* `update`: only with our marker. Backup copy first, writes the pack, deletes only files our old marker listed that the new pack no longer has, never touches files the owner added. Says "Already up to date" when the hash and all files match.
* `uninstall`: only with our marker. Backup copy first, deletes exactly the files in the marker plus the marker, removes empty directories.
* Everything except `status` refuses when FTB Quests is not loaded. After a change it runs `ftbquests reload quests` through `performPrefixedCommand` (FTB's command, editor permission) and tells the operator to run `/ftbquests reload` themselves if nothing changes.
* `quests.autoInstallOnFirstRun` (default false): on `ServerAboutToStartEvent` (HIGH priority, before FTB Quests loads) installs when FTB Quests is loaded and the folder does not exist at all.
* A `README*` file in the top of `quest_pack` is documentation and is not copied. If the pack has a `quests/` subfolder with `data.snbt` (and no top-level `data.snbt`) that subfolder is the content root.
* Update overwrites our files, so edits an owner made inside OUR chapters (or `chapter_groups.snbt`, `data.snbt`, `lang/en_us.snbt`) are replaced; the backup keeps them.

## Config
`nusmp-quests-server.toml` (SERVER): `quests.autoInstallOnFirstRun` = false, `quests.shareSquadCredit` = true, `quests.shareSquadCreditRadius` = 128.

## Tests (pure JUnit)
`EventCountersTest` (increment, key|arg, cap, copy), `TriggerRuleTest` (every key/arg/count combination, end to end with counters), `FutureBossIdsTest`,
`pack/PackPlanTest` (install decisions, backup name, update/uninstall plans, unsafe paths, marker round trip, hash), `pack/PackInstallerTest` (temp directory:
fresh install, refuse, force with backup, update keeps owner chapters and retires old ones, uninstall removes only ours, nested folder, missing pack).
Run with the JUnit shim: 28 tests passed. Not run: real JUnit/Gradle.

## Unverified (no compiler for the advancement API here)
The Minecraft advancement classes are not in the API dumps used for type checking, so these signatures are written from knowledge of 1.21.1 and
must be confirmed by the first real build (the CI compile): `SimpleCriterionTrigger<T>.codec()` and protected `trigger(ServerPlayer, Predicate<T>)`,
`SimpleCriterionTrigger.SimpleInstance.player()`, `EntityPredicate.ADVANCEMENT_CODEC`, `Registries.TRIGGER_TYPE`,
`PlayerAdvancements.getOrStartProgress(AdvancementHolder)` / `award(AdvancementHolder, String)`, `AdvancementProgress.getRemainingCriteria()`,
`MinecraftServer.getAdvancements().get(ResourceLocation)`, `AdvancementEvent.AdvancementEarnEvent.getAdvancement().id()`, `FakePlayer`'s package, `IModFile.findResource`
(used the same way by `compat/CompatPacks`). To get them into the dumps add `^net/minecraft/advancements/.*` to `.github/api-request.txt`.
Also unverified: that `ServerAboutToStartEvent` at HIGH priority runs before FTB Quests reads its folder and after NeoForge has loaded server configs
(if not, the auto-install config reads its default false and does nothing); that `performPrefixedCommand` with the operator's source passes FTB's editor check;
that persistent data of the player is kept on respawn by NeoForge (the clone handler copies it either way); that `ServerPlayer.teleportTo` is
what moves players in the entry points hooked (they are hooked at the entry method, not on a dimension event, so this does not matter).
