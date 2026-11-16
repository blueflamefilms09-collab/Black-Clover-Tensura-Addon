# Argonaut rework: Firebolt folded into Argonaut, Bell Cranel's Ultimate renamed

Owner request: "Firebolts should be mixed in with the Argonaut skill ... Argonaut Hero should be called whatever that main character from that anime is. It should give it his name plus something else."
Nothing here was run in a game (no Gradle in the session; CI is the compiler). Section 7 lists what is unverified.

## 1. Research and sources

| Fact used | Source |
|---|---|
| Bell Cranel is DanMachi's main character; Firebolt is his chantless, instant lightning-fire spell | **general knowledge of the series, not fetched** |
| Argonaut is a charge skill: a bell rings while it builds, the charged action hits harder; Bell pairs it with Firebolt and his knife in the big fights | **general knowledge, not fetched** |
| "Little Rookie" is Bell's nickname; Liaris Freese is the growth skill (already in the mod, untouched) | **general knowledge, not fetched** |

English wiki (danmachi.fandom.com), Japanese wiki (dungeon.fandom.com/ja), en.wikipedia.org and ja.wikipedia.org: **all four could not be fetched**: the session's network does not resolve those hosts (`getaddrinfo ENOTFOUND`). Nothing in the design depends on a number from a wiki; the numbers are the mod's own. Fetch the pages once the hosts are allowed and compare the flavour (section 3).

## 2. NEW / REPLACED / KEPT

| Part | Status |
|---|---|
| Firebolt mode of Argonaut (mode 1) and of Hero's Argonaut (mode 2): rapid cast, its own price and cooldown | **NEW** |
| Fire on a charged release (two or more bells): the strike and Hero's Beam burn what they hit, a fire-lightning bolt along the strike, a visual lightning on a full charge | **NEW** |
| Login / respawn migration (Firebolt holder -> Argonaut) and its undo | **NEW** (`ArgonautMerge`) |
| Server setting `danmachi.argonautFireboltMerge` (default on) | **NEW** (`NUConfig`) |
| Pure rules `ArgonautMergeRules`, test `ArgonautMergeRulesTest`, self-tests `argonaut.modes` / `argonaut.rules` | **NEW** |
| Display name of `heros_argonaut`: "Hero's Argonaut" -> **"Bell Cranel, Hero Argonaut"** (registry id unchanged) | **REPLACED** (name only; also `avatar_skills/heros_argonaut.json`, `tools/avatar/forge_tables.py`, README) |
| Hero's Argonaut mode methods (moved up into `ArgonautSkill`, same ids and order) | **REPLACED** (code only, behaviour kept) |
| `firebolt` skill id, class, config keys, icon, VFX, the standalone cast code (now `FireboltSkill.cast`, shared) | **KEPT** (thin skill, still castable) |
| `argonaut` / `heros_argonaut` ids, evolution `ARGONAUT -> HEROS_ARGONAUT`, charge, strike, beam, costs, cooldowns | **KEPT** |
| `ChargeRules.Strike` STRIKE 0, BEAM 1 | **KEPT**; `FIREBOLT` appended as 2 |
| Liaris Freese | **KEPT**, not touched |

## 3. The new Argonaut

- **Modes.** Argonaut: 0 Argonaut (hold to charge, release to strike; unchanged), **1 Firebolt**. Hero's Argonaut: 0 Hero's Strike, 1 Hero's Beam (both unchanged), **2 Firebolt**. Cycle the mode with the normal mode key.
- **Firebolt mode.** Press: the bolt leaves at once and strikes what you look at (same code as the old skill: `FireboltSkill.cast`, `FireboltRules`): full damage, 50% splash, ignite, a visual-only lightning strike. Price `fireboltMagiculeCost` (100), cooldown `fireboltCooldown` (20 ticks), damage `fireboltDamage` (8). Cooldowns are per mode, so a Firebolt does not delay the next charged strike and the reverse. Damage x2, range and splash larger when Argonaut is mastered or Hero's.
- **Charged release.** Two or more bells (`ArgonautMergeRules.FIRE_MIN_CHARGE`): everything the cone strike or Hero's Beam damages is set alight for 2 + charge seconds (cap 12). The strike also draws the existing orange fire-lightning bolt along it; on a full charge one visual-only lightning strikes the first creature hit (no damage, no fire, no block change). Meaning: in the series Bell's biggest hits are Argonaut-charged attacks with Firebolt's fire; here the charged strike carries Firebolt's fire (general knowledge, unverified).
- **Mastery.** A Firebolt that hit teaches Argonaut 0.5 (the old skill taught 1), so rapid casting cannot master Argonaut faster than the strike and skip to Hero's Argonaut early.
- **Damage and price** go through the same paths as before (`SkillDamage.hit` -> BalanceLaw / Tensura source; `pay` -> Tensura magicule cost with the Unique's plain cost scaling). No new damage number.

## 4. Firebolt holders (migration)

Choice: **thin skill plus login migration**. `firebolt` stays registered and castable (old saves, `/tensura edit ... grant`), and `ArgonautMerge` moves holders: at login and respawn a player holding `firebolt` gets `argonaut` (if they hold neither Argonaut nor Hero's Argonaut; the Ultimate already has the mode) and `firebolt` is forgotten. The holder instance is stamped `FireboltMigrated`. With the setting off, the next login gives `firebolt` back to a stamped holder. Why not `LegacyMigration`/`LegacySkill`: those pattern inert stubs of *removed* skills; Firebolt is not removed, only folded, so it keeps working. A new Argonaut starts at 0 mastery: the old Firebolt mastery is **not** carried (carrying it could master Argonaut at once and evolve it).

## 5. Name of the Ultimate

**"Bell Cranel, Hero Argonaut"**: his name (the owner asked for it) plus the title that says what the skill is. "Argonaut" is the skill he is named by in the series and "Hero" is the evolved form's own word (the id stays `heros_argonaut`); "Little Rookie" would describe the Unique, not the Ultimate. The in-game text comes from `anime_multiverse.skill.heros_argonaut` in `en_us.json`; `en_us.json` was **not** touched, so until the lang merge applies `tools/lang/argonaut_rework.json` (it carries this override and the new keys) the game still shows the old en_us text "Hero's Argonaut" (the code fallback and the avatar data already say the new name). **Coordinator: apply the overrides of that file (they replace existing keys; the merge scripts do not overwrite).** Evolution is untouched: `SkillEvolution` still maps `ARGONAUT -> HEROS_ARGONAUT`.

## 6. Bounded, server-safe, worst case

- All rules decided on the server; the client only draws the existing `SKILL_FIREBOLT` / `SKILL_ARGONAUT` shapes (no new client class, no new shape, no new packet type).
- State is per player on the skill instance (`Paid`, `Blocked`, `FireboltMigrated`); no static map, nothing to clear on logout (the charge is reset at the next press). The migration runs once per login / respawn: two registry reads, at most one learn and one forget.
- Worst case per player: Firebolt mode = 1 cast per 20 ticks (1 per second at the default; the cooldown setting can lower it, `fireboltCooldown` 0 allows one per tick, as for the old skill): 1 ray cast, <= 24 splash targets (`SkillTargets.MAX_FOES`), 1 visual `LightningBolt` entity (dies in a few ticks), 3 effect packets (beam, shape, `SKILL_FIREBOLT`) through `VfxSpawn`'s existing nearby-players-only budget. Charged release: <= 24 targets ignited, 1 extra `SKILL_FIREBOLT` packet and at most 1 visual lightning per release (cooldown 200 ticks). Hold: unchanged (<= 1 chime packet per second).
- Setting `danmachi.argonautFireboltMerge` (SERVER config, default true). Off: Argonaut has one mode, Hero's two, the charged strike does not burn, nothing is migrated, stamped holders get Firebolt back. The new mode strings are fallback-only and unused.

## 7. Tests and unverified

- `ArgonautMergeRulesTest` (mode table, old indexes, stale mode, cycle, burn curve, mastery, migration answers). `ChargeAndBoltRulesTest` is unchanged and still holds. Self-tests `argonaut.modes`, `argonaut.rules` (prefix `argonaut` added to `SMOKE_EXPECT` before `scaling`).
- **Not seen in a game / not compiled:** the Firebolt mode through the real key (press in mode 1, no hold side effects), the mode cycle UI, the login migration against a real saved player (`Skills.learnSkill` / `forgetSkill` / `getSkill(ResourceLocation)` calls are copied from `SkillEvolution` and `LegacyMigration`), the lang override, the look of the burning strike. The wiki flavour could not be checked (section 1).
- Decision for the owner: whether the old Firebolt mastery should carry over (see section 4), and the final wording of the name.
