# Sinbad, King of the Seven Seas (Magi) - arc boss

A new arc boss built on the current arc boss framework (`ArcBoss` / `ArcCharacter` / `Move`), ported from the owner-supplied, never-compiled
"sinbad_boss" zip. Goku (`arc/goku/*`) was the structural template. Lore: `docs/sinbad_boss_lore.md` (the file of the other session, kept as it came).

## NEW / REPLACED / KEPT

NEW (CLAUDE.md rule 5: nothing like it existed in the mod, so it is added next to everything else)
- Boss `sinbad_boss` in `com.newuniverse.nusmp.arc.magi`: `SinbadBoss`, `SinbadMoves`, `SinbadTable` (17 moves, pure), `SinbadEquip` (pure one-djinn rule),
  `SinbadDjinn` (enum), `MagiCharacters` (roster entry, saga ALLIES, tier 3), `SinbadBosses` (registration + spawn gate),
  `SinbadBossConfig`, `SinbadBossEvents`, `SinbadBossCommand`, `SinbadBossSelfTests`.
- Command `/multiverse sinbad_boss spawn [pos] | status | clear` (operators, level 2).
- Server settings file `nusmp-sinbad-boss-server.toml`, section `[sinbad_boss]`: `enabled` (true) and `globalCap` (2, range 1..4).
- Self-tests `sinbad.boss.data`, `sinbad.boss.spawn`, `sinbad.boss.cap` (smoke prefix `sinbad.boss`).
- Pure tests `SinbadTableTest`, source-scan test `SinbadBossSourceScanTest`.
- Lang key `entity.anime_multiverse.sinbad_boss` through the append-only fragment `tools/lang/sinbad_boss.json` and `tools/lang/merge_sinbad_boss.py`.

REPLACED
- Nothing in the mod. Only inside the zip itself: its `Djinn` enum was renamed `SinbadDjinn` (name clash risk), and the details listed under "Changed from the zip" below.

KEPT
- Every shared file was only appended to (one line or one block each): `ArcCharacters` (registration after Goku), `ArcSpawner` (the gate, before the existing-bosses line),
  `ArcSystem` (config, events, self-tests), `MultiverseCommands` (command node), `en_us.json` (one key at the end). No existing boss, move, scaling or enum was changed.
- All the damage goes through the existing law (`ZogratisBoss.strike` -> `ZogratisDamage` -> `BalanceLaw`). No new scaling was added.

## What the fight is

Rotation of the seven djinn equips: Baal (lightning), Valefor (ice), Zepar (sound), Focalor (wind), Vepar (water rain), Crocell (saber), Furfur (darkness).
One djinn at a time lasts 20 s (12 s once the Gates are open). Only the moves of the equipped djinn pass the move filter (`SinbadEquip.allowed`); the sword moves are always there.
A djinn unlocks by the fight's power line: Baal and Focalor 10, Valefor/Zepar/Crocell 25, Vepar 40, Furfur 60. In the last 3 s of an equip the boss may choose a direct switch.

Moves (table order, append-only, the client reads names by index): sword_slash, magoi_thrust, djinn_equip, bararaq, bararaq_saiqa, bararaq_inqerad_saiqa (safe golden circles),
garufor_zairu, garufor_kirestal, stagnation, command_scream (sleep scream), rukh_intrusion, foraz_zora, gale_step, vepar_istella, saber_flurry, rending_darkness, seven_seas_sovereign (finale, Gates only).
Passives: Magoi Manipulation (magic hits deal 75 percent), the "flow of destiny" sidestep (12 percent chance, 10 s cooldown).

## Changed from the zip (ported vs changed)

- The 3 percent heal on a djinn swap is gone: `BossHealGuard` blocks boss heals in combat, so it could never work and would only fight the guard. Nothing heals him.
- `setForm` is not used (the framework form switch is not needed; the default body is kept).
- Time rewind was replaced by a spatial rift look (no time effect is used, rule 3).
- Sound constants that could not be proven were replaced by ones used elsewhere in the repo (`SdsaStrikes.sound`).
- The telegraph is redrawn every pulse (the zip drew it once, so it vanished on the client); redraw goes through `pulse(left, total, every)` because the tell callback only fires every third tick.
- The Vepar rain is fired in volleys of 4 every 3 ticks through `later(...)`, not all at once. Saber flurry hits are 10 ticks apart. The finale is 7 `later` steps.
- The finale's recovery was raised from 80 to 150 ticks so he stays exposed until the last strike has landed (a test guards it).
- The `players()` walk of the zip was replaced by `fighters()` + `getPlayerByUUID`.
- No raw particles: all effects go through `Tells`, `VfxSpawn` and `SdsaStrikes` (the particle/budget path); `FxSourceScanTest` stays green.
- The zip's Charter (skill grant) was dropped. Its skill ids were not verified when the zip was written, and the source scan forbids naming them in the code. The ids that were checked in the
  repo's id reference and are ready for a later charter are: `lightning_domination`, `wind_domination`, `sound_domination`, `thought_acceleration`, `multilayer_barrier`.

## Multiplayer and server rules (CLAUDE.md 9, 11)

- All state is on the server and per boss. Per-player data is keyed by UUID (`fighters()`); logout, death and dimension change drop the player (`SinbadBossEvents`);
  server stop clears the live set; the locked spots, aim and equip are cleared when a fight ends (`onFightEnd`).
- Caps: `globalCap` (default 2, up to 4) live Sinbads at a time and `enabled`, both checked in `SinbadBosses.gate(sl, at)`, which `ArcSpawner.summon` asks for the command, the egg and the arena.
  Arena clearance 24 blocks. The live set is a concurrent set of bosses, pruned on use.
- Settings that change gameplay are SERVER config (`nusmp-sinbad-boss-server.toml`). There is no client setting.
- No client class is touched from common code (guarded by `SinbadBossSourceScanTest`).

## worst case (per Sinbad, bounded)

- Entities spawned: 0. He spawns nothing (no add, no projectile entity, no lightning entity, no item); every hit is an instant `strike` on a target found from the fight's own player list.
- Live bosses: at most `globalCap` (4 at the highest setting, 2 by default).
- Targets per move: only players in `fighters()` that are in range; each hit is one `strike`. A move marks at most `MAX_SPOTS` = 12 telegraph spots (the rain is 4 per volley, three volleys cover the cap).
- Packets: effects use `VfxSpawn.send` / `Tells`, which go to nearby players only and count against the per-tick effect budget (`PerfBudgets.effectPacket`, 160 per tick, shared by all effects on the server).
  A pulse redraws the spots only every few ticks. One boss at its heaviest (the finale step, 12 spots) is on the order of 12 to 20 effect packets per pulse, a few pulses per second, so well under 100 packets per second per boss,
  and anything over the budget is dropped by the budget, not queued.
- Particles per second: dust rings of a fixed small count per telegraph (Tells), at most 12 spots times the ring size per pulse; no per-tick world scan and no per-tick allocation beyond that.
- Server work per tick: the fight tick reads the player list of the fight, no world scan, no chunk load, no block write, no explosion. The magic circle on him is redrawn every 40 ticks.
- Client draw cost: the default body (`npc_default`) through `ArcBossRenderer`; nothing extra is drawn on the client by this change.

## Settings and smoke

- `nusmp-sinbad-boss-server.toml`: `enabled`, `globalCap`.
- Smoke prefix to add to the CI list: `sinbad.boss` (keys `sinbad.boss.data`, `sinbad.boss.spawn`, `sinbad.boss.cap`).

## Names: wiki-sourced or invented

No wiki was fetched in this change (rule 10 was not exercised here). The names and effects come from the zip and from `docs/sinbad_boss_lore.md` (written by the other session from the English wiki).
- From the lore file: Baal (Bararaq, Bararaq Saiqa, Bararaq Inqerad-Saiqa), Valefor (Stagnation, Garufor Zairu, Garufor Kirestal), Zepar (sound / command scream), Focalor (wind), Vepar (Istella), the Magoi Manipulation idea, the foresight idea.
- Invented for the fight (not canon): the Furfur move (Rending Darkness slash), the Crocell saber moves (Saber Flurry), the Seven Seas Sovereign finale and its seven steps, and all the numbers.

## Not checked

- Nothing was seen in a game. The code was compiled against the real MC/NeoForge classpath with javac and the pure and source-scan tests were run, but there was no Gradle build and no server or client run here; CI is the compiler.
- No model and no texture: there is no Sinbad art. He uses the default body look. The real model and texture are a TODO (rule 14: not made in Blender; nothing was faked as a PNG).
- No Better Combat file and no engravings: he is a boss, not a held weapon (rules 6 and 13 are about weapons/items).
- Balance is a first pass (numbers from the zip, scaled only through the existing balance law); needs a playtest.
- No floors, no themes, no charter (see above), no story credit line beyond the framework's own.
- `Rework3Rows` has no row for `sinbad_boss`; unknown moves are allowed by it, so he runs, but his moves are not in that table.
- Move names in the boss bar come from the table labels; no lang key per move was added (only the entity name key).
- `BossHealGuard` means any heal idea (the zip's swap heal) is not possible; if the owner wants it, it needs a deliberate exception there.
