# Guide fixes P2 (and the P3 / P4 items that could be checked here), the class damage hookup and the legacy grimoire costs

Source: the owner's master build guide (Part 0 §0.5, Part 4 §5 P2 #6-#13, P3 #14-#17, P4 #17-#20; follow-ups of `docs/ultimate_classes.md` and `docs/boss_cores.md`).
Nothing here was run in a game and Gradle was not available: only the pure rules were compiled and run (`QuenchedRules`); CI is the first full compile.

## 1. The combined class damage cap is live (follow-up of `docs/ultimate_classes.md`)

`SkillDamage.hit` now asks the skill instance for its class and calls `BalanceLaw.damage(caster, target, scaled, masteryFrac, cls)` (`cls = instance.getSkill() instanceof NUSkill s ? s.ultimateClass() : null`).
A hit without an instance (DbMoves, the old forms) has a null class, so it is the plain law. The old path stays behind the existing SERVER switch `[ultimateClasses] classDamage` (false = every hit plain).
So a Lord hit is at most x24, a King hit x30, a God / Dragon hit x36 (epCap x lateGameShare), whatever the caster's EP and pool. The 37 skill call sites already pass their instance (P1 #2).

## 2. The legacy grimoire skills pay through Tensura (follow-up of P1 #1)

`GrimoirePageSkill` (the Time pages, 7 call sites in `TimePages`) and `GrimoireMagicSkill` (the `grimoire_<magic>` skills, 5 sites) extend Tensura's `Skill`, not `NUSkill`, so they get a small shared helper, `skill/LegacyPay`:
the skill's `getMagiculeCost` answers the price held in a ThreadLocal (0 outside a payment, as before), and `EnergyHelper.isOutOfEnergy(player, instance, mode)` checks and takes it once (creative pays nothing,
Tensura's own "not enough magicule" message). The price is the old flat price scaled by `BalanceLaw.classCost(player, null, flat)` (never below the flat price; the plain 1 % / 1,000 law, the same as an unclassified NUSkill cast).
`GrimoirePageSkill.pay(instance, player, base)` is the new overload; the 7 Time page calls pass their instance (only the cost lines of `TimePages` changed, no Time Magic VFX line). The old `pay(player, base)` stays.
SERVER switch: `[ultimateClasses] flatCosts = true` writes the flat price straight to the pool, as before. The Chrono Stasis Grigora zone pays each second and when dragged, so it is one `isOutOfEnergy` per second per active zone.

## 3. Guide Part 0 §0.5 list: what was done and what was not

Numbers are the Part 0 §0.5 list (Part 4 §5 numbers them one higher from #7 on; the code comments use the Part 4 numbers).

| # | Item | Result |
|---|---|---|
| 6 | `GrimoireCastEvent` (pre / post, cancellable) | DONE. `book/GrimoireCastEvent` with `Pre` (cancellable, before the cast is paid and after every check) and `Post`. The ~14 direct calls of `castPage`'s tail (quests, relics wave 6 / 16, character flair, franchise, Solomon's cast log, Dream / Mirror, Spirit of Darkness, Rukh flow ...) moved, in the same order, to `book/GrimoireCastListeners`, the built-in `Post` listener registered in `NUSMP`. SERVER switch `[guide_fixes] grimoireCastEvent` (false = `castPage` calls the same list directly, no event). Not converted: `onHeld` / `onRelease` hooks and `getMagiculeCost` hooks (they are inputs to the cast, not follow-ups; an event there would be one per held tick). |
| 7 | `PageContext` (`spend()`, `hurt()`, `size()`) | PARTLY. The duplicated multiplier block of `hurtAs` / `hurtDirect` is one private `pageDamage` (the amount is unchanged, so no switch). Gate of Babylon's 10 spend / fail preambles are already routed through `NUSkill.pay` (P1 #1) and not extracted further; per-book JSON numbers (FireBook ...) not done. |
| 8 | Delete `merged_NN`, `grimoire_*`, old devil union | NOT DONE: needs a login remap of players' skills and the owner's rule 2 (nothing is removed unless asked). The two legacy families now pay through Tensura (item 2) instead. |
| 9 | Persistent-data keys to attachments | NOT DONE: ~1,450 keys across the mod, a save-data migration; not verifiable without a game. |
| 10 | Translate `Component.literal` strings | PARTLY (bounded): the `grimoire_<magic>` mode names (5), the devil-union broadcast, "pages sealed", "recharging", "master first", "summon first", "returns to you" are `translatableWithFallback` / `SkillMsg.t` with the old English as the fallback (so the text is identical without a lang entry). Keys in `tools/lang/guide_p2.json` (12). About 1,290 literals remain. |
| 11 | Rename `Wave0..71` / `Batch1..8`, `XxxBoot.init` | NOT DONE: a rename of ~300 classes breaks the many source-scan tests that name them by path and the owner's keep-the-old rule. |
| 12 | Expose `skill/rules` and page numbers in config | NOT DONE (large; the class table and the new switches are config, `skill/rules` numbers are not). |
| 13 | (P3) Mark dirty only on change | DONE for the two named sites: `GrimoireBook.onHeld` (the charge sync stops once the charge is full) and `AntiMagic.setAmp` (setting the same value, every second at the cap, syncs nothing). SERVER switch `[guide_fixes] markDirtyOnlyOnChange` (false = the old marks). |
| 14 | `canTick=false` for books that need no tick, consolidate the 165 listeners | NOT DONE: every book's once-a-second `second()` (mana sense, spirit-control sync, upkeep) is driven by both ManasCore's tick and the player tick, but whether ManasCore also advances cooldowns through the same tick could not be verified without the API; a wrong `canTick` would freeze cooldowns. |
| 15 | Scope the `LevelGuardMixin` / `LevelChunkGuardMixin` | CHECKED, no change needed: `DungeonGuard.on(level)` is `isClientSide` plus three dimension reference compares (`ZogratisArena.in`, `ArcArena.in`, `Labyrinth.in`) before anything else; `afterSet` returns on the same test. The block is read last. |
| 16 | Immutable `pages()` caches | DONE as safe publication: `GrimoireBook.pages()` and `familyAll()` build the finished list, then publish it through one `volatile` holder record (version, flags, list), so the client and server threads of a single-player game never see a half built or mixed list. The lists themselves are not wrapped read-only (a caller that mutates one would then throw; none was found by grep, but it could not be proven for every `var`). |
| 17 (P4) | Declare GeckoLib, `manascore_race`, `manascore_network`, optional `trnightmare` in `mods.toml` | NOT DONE, on purpose: no jar of these mods is readable here, so their exact mod ids and ranges are unverified, and a wrong id on a `required` entry stops the game from starting. `CompatCatalogTest` also pins the required list of the toml to `CompatCatalog` (the Codex). To do it: read the four ids and versions from the jars in the Tensura instance's `mods` folder, add the `[[dependencies.anime_multiverse]]` blocks (`ordering = "AFTER"`) and the matching `CompatCatalog` entries. |
| 18 (P4) | Reduce the 244 MB of textures | NOT DONE: image downscaling changes assets (rule 2) and was not asked for by name. |
| 19 (P4) | Remove the `*SelfTests` from release builds | NOT DONE: the smoke test needs them in the jar (`SelfTests` is the CI check) and a dev-only source set needs a Gradle change that could not be built here. |
| 20 (P4) | Narrow the blanket `Throwable` catches | NOT DONE: 500+ sites, each needs a decision (`SafeCast` is meant to catch). |

## 4. Quenched (Tensura No Index): Telesma and Malice

Checked: no Quenched / Tensura No Index jar, source or API is in the repo, the worktree, the gradle caches or the uploads (only `tools/compat/multiverse_x_tensura_no_index/` data and `known_ids.json`, which list items and entities). So no
class of that mod is touched and nothing is hard-wired. The stub is replaced by a data-driven bridge (`labyrinth/mega/cores/QuenchedBridge`, rules in the pure `QuenchedRules`), SERVER file `nusmp-boss-cores-server.toml`, section `bossCores.quenched`:

| Setting | Default | Meaning |
|---|---|---|
| `bridge` | false | true = an Ascendant kill also runs the commands below, only when `tensuranoindex` is loaded |
| `telesmaCommand`, `maliceCommand` | "" | the command text of that mod, with `{player}` and `{amount}` filled in; blank = nothing |
| `telesmaPerEp`, `malicePerEp` | 0.0 | amount = the EP the Ascendant added x this, rounded down |
| `maxPerKill` | 1000 | the most one kill pays of each |

The command runs as the server at permission level 2 with its output suppressed, once per payment, for at most two commands per kill and one payment per player per 10 ticks; the player name must be a vanilla name (1-16 of `A-Za-z0-9_`), the line one line of at most 256 characters without control characters.
**What the owner must provide:** the exact command that adds Telesma and the one that adds Malice in Tensura No Index (or Quenched), for example `<their command> {player} {amount}`, and the per-EP factors; or the jar so the bridge can be written against the real API (then only `QuenchedBridge.onAscendantGain` changes).
Without that it does nothing, as before. State: one `UUID -> tick` map, dropped at logout, death and server stop.

## 5. Armour models for the four boss-core families (Blender): NOT DONE

Blender is not installed here (no `bpy`, no `blender` binary), and the pipeline (`tools/blender/bl_lib.py`, `docs/heroweapon/blender_pipeline.md`) bakes in Blender. The repo has a worn-model library (`client/relics/worn/lib`, `WornMesh`) and `ModeArmor`,
but binding a baked armour mesh to the 28 pieces (4 slots x 7 sets) needs the models first and a game to judge the fit. The armour keeps its layer textures and 16 x 16 icons. To do it: model one helmet / chest / legs / boots per family (Aegis, Striker, Wellspring, Ascendant; the four Wellspring resources recolour one model)
through `bl_lib.py`, then add them to the worn-model library and keep the vanilla layer as the fallback. Reported as "not made in Blender".

## Settings (all SERVER)

| Where | Key | Default | Restores |
|---|---|---|---|
| `[ultimateClasses]` | `classDamage` (existing) | true | false = the plain damage law for every hit |
| `[ultimateClasses]` | `flatCosts` (existing) | false | true = the flat price for the NUSkill family AND the two legacy grimoire families |
| `[guide_fixes]` | `grimoireCastEvent` | true | false = the old direct calls in `castPage`, no event |
| `[guide_fixes]` | `markDirtyOnlyOnChange` | true | false = a sync every held tick / every AMP set |
| `nusmp-boss-cores-server.toml` `[bossCores.quenched]` | `bridge`, `telesmaCommand`, `maliceCommand`, `telesmaPerEp`, `malicePerEp`, `maxPerKill` | off / blank / 0 / 1000 | the old stub (nothing happens) |

## Worst-case cost (rule 11)

- Class damage: one `instanceof` and a null check more per skill hit; the damage math is the same size as before. No packets, no entities.
- Legacy grimoire payment: one ThreadLocal set / remove and one `isOutOfEnergy` per cast (what NUSkill and the grimoire books already do); the Chrono Stasis Grigora zone, one per second while it is open. No packets beyond Tensura's own sync.
- Cast event: one `Post` event per cast (plus one `Pre` when the switch is on), posted on the server thread, with one built-in listener running the same ~14 calls as before; with no extra listeners this is a few object allocations per cast, not per tick. 0 packets.
- Dirty gating: fewer syncs, never more (a held page at a full charge: 0 skill syncs per tick instead of 1 per tick; AMP at the cap: 0 per second instead of 1).
- Page caches: one volatile read per `pages()` call, a rebuild only when the version or a flag changes.
- Quenched bridge: off by default (one config read per Ascendant kill); on, at most 2 server commands per kill, 1 payment per player per 10 ticks, 1 small map entry per player.
- Messages: translatable components cost the same as literals; no per-tick work.

## Tests and self-tests

- Pure: `labyrinth/mega/cores/QuenchedRulesTest` (amount, name, command text, pacing; also compiled and run with plain `javac` here).
- Source scans: `skill/GuideP2SourceScanTest` (class damage call, LegacyPay path and switch, the event and its listener and order, one damage block, volatile caches, dirty gates, the bridge, the lang keys, the smoke prefix). Pinned neighbours fixed: `UltimateClassSourceScanTest` (LegacyPay is the other flat fallback),
  `LateGameScalingSourceScanTest` (the grimoire buff is applied once, in `pageDamage`), and the four tests that read the cast tail from `GrimoireBook.java` now also read `GrimoireCastListeners.java`
  (`SpiritDarkSourceScanTest`, `CharacterSetBullsTest`, `CharacterSetSourceScanTest`, `CharacterSetRestSourceScanTest`).
- Self-tests `guide.p2.pay`, `guide.p2.cast_event`, `guide.p2.damage_class`, `guide.p2.quenched` (`skill/GuideP2SelfTests`; a stand-in player, nothing paid, damaged or run); `guide.p2` is on the smoke list right after `ultclass`.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `skill/LegacyPay`, `GrimoirePageSkill.pay(instance, player, base)` + `getMagiculeCost`, `GrimoireMagicSkill.getMagiculeCost`, `book/GrimoireCastEvent` + `GrimoireCastListeners`, `labyrinth/mega/cores/QuenchedRules`, settings `guide_fixes.grimoireCastEvent` / `markDirtyOnlyOnChange` and `bossCores.quenched.*`, `GuideP2SelfTests`, three tests, `tools/lang/guide_p2.json`, this doc |
| REPLACED | `SkillDamage.hit`'s 4-argument `BalanceLaw.damage` by the class-aware one (old path: `classDamage = false`); the legacy grimoire flat writes (old path: `flatCosts = true`); `castPage`'s direct feature calls by the event (old path: `grimoireCastEvent = false`); the QuenchedBridge stub by the data-driven bridge (old behaviour = the defaults); the duplicated damage block by `pageDamage` (same amount); the loose page caches by volatile holders; the grimoire literals by translatable ones (same English) |
| KEPT | `SkillUtil.spendMagicules`, `GrimoirePageSkill.pay(player, base)`, every skill id, mode and cost number, all Time Magic VFX, every other `Component.literal`, `merged_NN` and `grimoire_*` skills, the persistent-data keys, the package names, `canTick`, the mods.toml, the textures |

## Caveats

- Not seen in a game. `Skill.getMagiculeCost` on the page skills is overridden on the assumption it is overridable (NUSkill already does it with the same signature).
- Creative players now pay nothing for the legacy grimoire skills and the Time pages (Tensura's rule), and their price grows with max magicule late game (the flat price stays the floor).
- A `Post` listener that throws now surfaces from the event bus instead of from `castPage` (the same exception either way, since the old calls were not inside `SafeCast` either).
- `onHeld` marks dirty only when the charge or mode changed; other tag writes a held page's helpers make (fire casting, rune hold) mark their own dirt as before, but this was not checked for every style.
- API names not verifiable here (CI is the check): `NeoForge.EVENT_BUS.post(event)` returning the event, `ICancellableEvent.isCanceled/setCanceled`, `CommandSourceStack.withSuppressedOutput().withPermission(2)`, `MinecraftServer.getCommands().performPrefixedCommand(source, String)`, `Component.translatableWithFallback(key, fallback, args...)`.
