# Avatar / NPC creation: the core (AV1, release 1.51.0)

Owner spec: "My Avatar's Path to Greatness"-style Avatar creation (sections 1, 2, 6, 7, 8 of the brief). This document covers the core: the entity, the creation service, the outcome engine, luck, stats, naming,
races, goals, the betrayal tracking, the Genesis Core and the admin command. The GUI and network are AV3's, the AI, casting, chatter and Yandere are AV2's (see `docs/avatar/av2.md`, `av3.md`).

Nothing here was run in the game (no compiler or game in the build session). What was done: the pure classes were compiled and **54 unit tests run** (JUnit shim, plus mutation checks); the Minecraft-facing classes were
type-checked against API stubs (the only errors left are whole missing stub packages, see "Unverified"). CI runs the real JUnit.

**Datapack namespace note.** The owner wrote `data/multiverse_mod/avatar_races/`. The mod id is `nusmp`, so the files are `data/nusmp/avatar_races/*.json`.

## NEW vs REPLACEMENT vs KEPT (owner rule 5)

Everything is **NEW**: nothing like an Avatar creation system exists in the mod. Checked against the existing code: `entity/avatar/*` is the Devil Avatar *boss* (a different thing; untouched, its id is `nusmp:devil_avatar`,
ours is `nusmp:player_avatar`); there was no 4d6 roller, no D&D stat enum, no Falna record, no race datapack. Nothing was removed or replaced; the only edit to existing behaviour is none (all shared-file edits are listed for the lead).

## Classes and why each exists

| class | why |
|---|---|
| `AvatarEntity` | the player-made NPC: a `PathfinderMob` (never a Player) implementing `OwnableEntity` (so relics, Rouge's ally list and pet-aware mods treat it as the creator's) and `HasDndStats` (AV2 reads the scores). Saves CreatorUUID, CreatorStoredLuck, AvatarName, IsNamed, personalities, PermanentMagiculeTax, OutcomeTimer, HasUniqueOrUltimateSkill, AppearanceData, RaceId, OutcomeState, Stats, Perk in its own NBT. |
| `AvatarEntities` | the entity type register (`nusmp:player_avatar`, 0.6 x 1.8, tracking 10). Scale comes from the vanilla `SCALE` attribute set to `appearance().scale()`. |
| `AvatarSystem` | the single entry point `init(modBus, modContainer)`; registers everything; holds the rule handlers (damage guard, tax refresh on login, the wake pass). |
| `AvatarConfig` | every number, plain `define`/`defineInRange`, static accessors that fall back to the defaults when the spec is not loaded. |
| `AvatarPlayerData`, `AvatarSession` | the two player attachments (see below). |
| `AvatarManager` | world SavedData: every Avatar's creator, name, state, last dimension and position, tax. Per-player limit, betrayal tracking in unloaded chunks, tax sums. |
| `AvatarCreation` | the server door of the GUI: `rollStats`, `finalizeAvatar`, `nameAvatar`, `spawn`. |
| `TensuraBridge` | all Tensura existence/EP/magicule/aura calls, defensive (one place to fix when Tensura changes). |
| `AvatarCommand` | `/nusmp avatar ...` |
| `AvatarNamingHook` | listens on Tensura's `NAMING_EVENT` so Tensura's own naming UI works on an Avatar too. |
| `dnd/Stat`, `StatRoller`, `AvatarStats` | the six scores, 4d6-drop-lowest / 5d6-keep-three, and the mapping to health, damage, speed, armour, EP, magicule, aura. Pure. |
| `luck/LuckMath`, `LuckPoller` | Total Luck: vanilla `Attributes.LUCK` + Tensura skills whose id contains probability/luck/fortune (+2 each, max +6; found by registry scan, never a hard id) + Vanessa's Rouge (`RougeArmed` tag or an active Rouge entity: +3, once) + Dice Magic's Loaded Dice buff (+2; DiceBook keeps no roll history, only this buff, so it is the only readable dice luck). |
| `outcome/Outcome`, `OutcomeMath`, `AvatarOutcomes`, `AvatarUniqueSkill` | the three-day engine, the weights, the generated Unique Skill table. |
| `naming/NamingMath`, `NameRules`, `TensuraNamingEngine`, `NamingSleepEffect` | naming surge and tax numbers, name validation, the naming consequences, the `nusmp:naming_sleep` debuff. |
| `race/AvatarRace`, `AvatarRaceParser`, `AvatarRaces`, `ConditionEvaluator`, `RaceConnectionChecker` | datapack races, strict parser, reload listener, the tag-to-predicate registry (pure core + the real player context). |
| `goal/*` | `AvatarFollowCreatorGoal`, `AvatarDefendSelfOrCreatorGoal`, `AvatarHuntCreatorGoal`, `AvatarSignatureGoal`, `AvatarSignatures`, `AvatarTargeting` (the one rule for who may be hurt). |
| `betrayal/BetrayalDimensionHandler` | teleport next to a player in any dimension; wake unloaded Avatars with a chunk ticket. |
| `item/GenesisCoreItem`, `AvatarItems`, `GenesisCoreDrops` | the creation key, its registration, and its dungeon-boss drops. |

## Player data: why attachments for the player and NBT for the entity

The owner asked for NeoForge `AttachmentType`s. Two are registered on the **player**: `nusmp:avatar_player_data` (codec-serialised, copied on death: the list of the creator's Avatar ids, the "first core already granted"
pity flag, how many were ever created) and `nusmp:avatar_session` (never serialised: the pending roll). The per-Avatar fields stay in the **entity's own NBT**: an entity's saved data travels with it through chunk
unloads and `changeDimension` (which re-creates the entity from NBT) for free, so an attachment on the entity would only duplicate that.

## Creation (server validated, the client only chooses)

`rollStats(player)`: needs a Genesis Core (creative: not needed); the first roll is free, each reroll costs `rerollCostMagicule` and at most `maxRerolls`; luck = `LuckPoller.total`; five dice when luck is **strictly above**
`luckBonusThreshold` (10); the result, the luck and a random token are stored in the player's `AvatarSession`. A refused roll is `Roll.refused()` (empty stats).

`finalizeAvatar(player, request[, token])` checks, in order: system on, race exists and is selectable, the Avatar limit, a Genesis Core, the race's connection conditions, personalities (primary required, secondary
dropped when equal, both allowed by the race), the session (stats are read from the session, never from the request; a stale token is refused), the optional display name, the magicule cost (`base_magicule_cost`; refunded
if the entity cannot be placed), the appearance (AV3's `DynamicAppearanceEngine.sanitize`). Only after the entity is in the world is the core consumed and the session cleared. Races with `requires_dnd_roll: false` skip the
roll and use scores of 12.

Stats become attributes in `AvatarStats.derive`: health = 20 + 2 per CON above 10, damage = 4 + 0.6 per STR above 10, speed from DEX, armour from CON/DEX, EP = race `base_ep` x (0.6 + total/72), magicule from INT/WIS,
aura from STR/CON/CHA; each score is first multiplied by the race's multiplier for it. The race's `base_attributes` are added as permanent additive modifiers (`nusmp:avatar_race_<attribute>`). EP, magicule and aura are
written through `TensuraBridge` (base max EP, max magicule, max aura, and the current values).

## Outcome engine (owner section 8)

`AvatarEntity.aiStep` -> `AvatarOutcomes.tick`: counts `OutcomeTimer` (72000 ticks by default) down while the state is PENDING; every 100 ticks the AvatarManager row is refreshed; every 200 ticks Tensura skills are polled
for a Unique/Ultimate (only skills in Tensura's own namespace count: this mod's grimoire books are Unique-type skills and must not trigger it).

At zero: `OutcomeMath.decide(named, hasUnique, weights(luck), random)`.

* Weights (pure, tested): shift = clamp(luck x 0.01, 0, 0.30) (never more than the betrayal chance there is); betrayal = 0.30 - shift, soul = 0.25 + shift, ordinary = the rest (0.45 at luck 0). The sum is always 1 even with a tuned config.
* **Named** (Soul-Bond Lock): no roll. The moment of naming sets `SKIPPED_NAMED` and cancels the timer. The Avatar **stays loyal**, forever; it keeps following and defending; a named Avatar that had already betrayed is cured (naming
  turns BETRAYAL into SKIPPED_NAMED). A named Avatar can still be Soul-Awakened or turn Ordinary only by command.
* **Unique/Ultimate skill**: the roll is skipped (`SKIPPED_UNIQUE`), it stays loyal. Naming wins when both apply.
* **Betrayal**: goals are thrown away and rebuilt (`rebuildGoals`): melee + `AvatarHuntCreatorGoal` only; AV2's AI is not installed (a betrayed Avatar never casts at its creator). The creator is warned. The hunt goal follows the creator
  across dimensions with `BetrayalDimensionHandler.teleportNear` (a 1.21.1 `changeDimension(DimensionTransition)`, which re-creates the entity from its NBT; the goal keeps using the returned entity). After every teleport the creator gets
  `betrayalGraceTicks` (200 = 10 s) in which the Avatar holds back and the creator is told "X has found you". Every blow on a player is capped at `betrayalHitCap` (25%) of their max health (damage guard in `AvatarSystem`). Naming cures it.
* **Soul Awakening**: `AvatarUniqueSkill.generate` picks a perk from a ten-entry table: a passive (+15..30% attack, health, speed, armour or regeneration) and a signature move (shockwave, dash strike, soul flame, 6-12 raw damage
  through `BalanceLaw.damage`, 8-12 s cooldown scaled by `cooldownMultiplier()`). No new Tensura skill is registered (impossible at runtime and it would pollute the registry); the perk is entity data and permanent attribute
  modifiers (`nusmp:avatar_perk`). Sets `HasUniqueOrUltimateSkill`, **lifts the permanent tax** (it is no longer the creator's), and makes the Avatar independent: it no longer follows or obeys, it wanders as a neutral named NPC,
  defends only itself (`HurtByTargetGoal`) and never harms its creator.
* **Ordinary Existence**: +15% health and +10% damage (permanent modifiers), keeps `AvatarFollowCreatorGoal` in its *limited* form (only while the creator is within 32 blocks in the same dimension, no teleporting); the tax stays.

## Naming (owner section 6)

`TensuraNamingEngine.name` (our GUI, via `AvatarCreation.nameAvatar`) and `AvatarNamingHook` (Tensura's own naming, which also gets a validity check before Tensura spends anything):
Soul-Bond Lock (above); EP surge `NamingMath.epBoost(luck, INT, random, min, max)` = min + (max - min) x (0.5 random + 0.3 luck/20 + 0.2 (INT-3)/15) (default +50%..+200%); evolution to the race's `evolves_to_race_id` (race id swapped,
scale scaled by the ratio of the race scales and re-sanitised, stats and EP recomputed with the surge as the multiplier) or just the EP scaled when the race has no evolution; the creator pays: the `nusmp:naming_sleep`
effect for `namingSleepSeconds` (doubled when the Avatar evolved) and a permanent magicule tax of `permanentTaxPercent` (10%) as the transient attribute modifier `nusmp:avatar_tax` on the creator's
`tensura:max_magicule` (re-applied on login, by the Avatar's slow tick, on death; summed over living Avatars from the AvatarManager, capped at 90%); the NAMING chat bark; quest key `avatar.named`. Via Tensura's naming the EP
surge of Tensura applies and ours is skipped (no double surge). Name rules: 2 to 24 characters, letters, digits, space, apostrophe, hyphen; unique per creator (case-insensitive).

`NamingSleepEffect` (`nusmp:naming_sleep`, harmful): magicule regeneration x0.1 (`tensura:magicule_regeneration_multiplier`), -15% speed, and 2% of the maximum magicule drained every second through Tensura's storage.
(The owner said "through the existing mana law": the mod's ManaLaw is the grimoire layer; the actual pool is Tensura's existence storage, which `EnergyBridge` and the Rouge mana already use, so this uses the same.)

## Races (owner section 7)

Reload listener (`AddReloadListenerEvent`) over `data/<ns>/avatar_races/*.json`. A dynamic datapack registry was **not** used: it would need a full codec and a client sync of the record, while the GUI only ever needs the list the
server sends in AV3's races payload, and a listener lets one bad file be skipped with a log line instead of failing the datapack load.

Schema (every field optional except none; defaults in brackets):

```json
{
  "display_name": "Demon Host", "description": "...", "default_scale": 1.25,            [id, 1.0, 0.3..4.0]
  "base_attributes": { "minecraft:generic.max_health": 6.0 },                           [{}] added to the score-derived values
  "stat_multipliers": { "str": 1.15, "int": 1.2 },                                      [1.0 each, (0..5]]
  "base_magicule_cost": 1500, "base_ep": 2500,                                          [0, 100]
  "evolves_to_race_id": "devil_vessel",                                                 [""] "nusmp:" prefix allowed
  "requires_dnd_roll": true, "selectable": true,                                        [true, true] selectable=false: reachable only by evolution
  "connection_conditions": ["black_clover:devil_contract", "min_ep:1000", "has_item:nusmp:genesis_core"],   [[]]
  "allowed_personalities": ["LOYAL", "STOIC"],                                          [[] = all]
  "appearance": { "skin_tone": "#B04A4A", "horn_id": 2, "aura_rgb": "#C01030" }        [{}] colours as "#RRGGBB" / "0x..." / numbers
}
```

Fifteen races ship: `demon_host` (exactly the owner's example), `devil_vessel` (its evolution), `human`, `elf` -> `high_elf`, `dwarf`, `ogre` -> `kijin`, `goblin` -> `hobgoblin`, `lizardman` -> `dragonewt`, `beastman`,
`fallen_angel`, `spirit`. The evolved forms are `selectable: false`. (Tensura race ids were not used: the mod's own `multiverse/Race` families are looser and the Avatar's race is its own thing.)

Connection tags (`ConditionEvaluator`, the predicate registry map; `RaceConnectionChecker` supplies the real player): `has_item:<id>`, `has_skill:<id>`, `has_grimoire`, `has_magic:<MagicType>`, `black_clover:devil_contract`
(= `ForbiddenMagic.devilOf(p) != null`), `danmachi:has_falna` (**this mod has no Falna record**: it counts a god's blessing, i.e. holding `nusmp:hestia`, `freya`, `hephaestus`, `loki` or the Faith skill),
`relics:requires_curios_attunement` (`Attune.carried >= 1`), `min_ep:<n>` (max EP), `dimension:<id>`; a leading `!` negates. The key is the longest registered prefix, so `has_item:nusmp:genesis_core` splits correctly. **Unknown tags and
throwing predicates fail closed** with a warning (a typo can never open a race, and negating an unknown tag does not open it either). `ConditionEvaluator.register` lets an add-on add a tag.

## Goals

* `AvatarFollowCreatorGoal`: walks when > 7 blocks away, teleports beyond `followTeleportDistance` (24) when not fighting, follows across a dimension change (same utility as the betrayal), limited form after Ordinary Existence.
* `AvatarDefendSelfOrCreatorGoal`: targets whoever hurt it, whoever hurt its creator, what its creator is attacking, within a 5-second memory; `AvatarTargeting.canHarm` excludes the creator, FTB squadmates (the mod's reflective
  `Squads.partyMembers`; FTB stays a soft dependency), other Avatars of the same creator, the creator's pets, creative/spectator players.
* `AvatarHuntCreatorGoal`: see Betrayal. `AvatarSignatureGoal`: the awakened perk's move.
* AV2 adds its goals through `AvatarAi.install(this)` (called after these; see AVATAR_NOTES). `cooldownMultiplier()` is 1.0, or 0.1 in a frenzy.

## Cross-dimension tracking (`BetrayalDimensionHandler`)

An entity in an unloaded chunk cannot act. Every 100 ticks `wake` reads the AvatarManager: for each Avatar that is not loaded and whose creator is online, and which is either betrayed or loyal-and-left-behind in another
dimension, it puts a vanilla portal chunk ticket (radius 3, expires by itself after 300 ticks, at most once per 300 ticks per Avatar) on its last known position, so it loads, ticks, and its own goal teleports it. Nothing stays loaded for good.
Block changes: none (the Avatar system changes no blocks), so `DungeonGuard` is not involved.

## Genesis Core

`nusmp:genesis_core`, epic, stack 16, foil. Use opens AV3's `AvatarBuilderMenu` and does **not** consume it; creation does. Drops (`GenesisCoreDrops`, on `LivingDeathEvent`): only from the dungeon bosses (entity types `goliath`, `charybdis_spawn`,
`floor_warden`, or the persistent tags `nusmp_floor_boss`, `nusmp_juggernaut`, `nusmp_charybdis`, `nusmp_peak_boss`: the same ids the quest hooks use), to every participant (`QuestHooks.participants`): the first time is guaranteed (pity
flag in `AvatarPlayerData`), afterwards `genesisCoreDropChance` (15%). Put straight in the inventory (or dropped at the feet when full). A loot-table modifier was not used: a death hook gives per-player pity and per-participant
rolls, which a global loot modifier cannot. Hand model: `tools/gen_avatar_assets.py` writes a 3D element model (a glowing core in a translucent shell with four orbit studs and a halo ring, emissive light data) used everywhere except
the inventory, where the 16x16 sprite stays (`neoforge:separate_transforms`). The core is not a weapon, so it needs no engravings.

## Config (`nusmp-avatar-server.toml`, section `avatar`)

`enabled`, `maxAvatarsPerPlayer` (1), `outcomeTicks` (72000), `betrayalBase` (0.30), `soulBase` (0.25), `ordinaryBase` (0.45, informational: ordinary is always the remainder), `luckShiftPerPoint` (0.01), `luckShiftMax` (0.30),
`luckBonusThreshold` (10), `namingMinEpBoost` (0.5), `namingMaxEpBoost` (2.0), `permanentTaxPercent` (10), `namingSleepSeconds` (180), `frenzyHealthBurnPerCast` (2.0, read by AV2), `genesisCoreDropChance` (0.15),
`rerollCostMagicule` (0), `maxRerolls` (5), `betrayalHitCap` (0.25), `betrayalGraceTicks` (200), `followTeleportDistance` (24).

## Commands (op level 2): `/nusmp avatar ...`

`spawn <race> [player]` (no core, cost or limit), `list`, `info`, `kill` (nearest within 64 blocks), `outcome now|reset`, `outcome force betrayal|awaken|ordinary`, `give genesis_core [player]`.

## Quest keys fired

`avatar.created` (arg: race id), `avatar.named`, `avatar.betrayed`, `avatar.awakened` (through `NusmpEvents.fire`).

## Tests (`src/test/java/com/newuniverse/nusmp/avatar/`)

`LuckMathTest`, `StatRollerTest` (seeded, 4d6/5d6, drop-lowest with a fixed dice sequence, derived stats), `OutcomeMathTest` (weights, bands, the skip rules, the generated skills), `NamingTest` (EP boost band, tax, name rules),
`AvatarRaceTest` (parser on bad input, all shipped files parse, evolution chains, the owner's `demon_host`, the condition registry with a fake player: parameterised tags, namespaced keys, negation, unknown tags failing closed).

## Unverified (nothing was run in the game)

* Everything Minecraft-facing was only type-checked against incomplete stubs. The remaining errors are stub gaps (missing packages `goal.target`, `packs.resources`, `portal`, `MinecraftServer`, Architectury, `Entity.getData`...), not wrong calls
  of ours, but the real compile is CI's.
* `TensuraEntityEvents.NAMING_EVENT`: the meaning of the two `Changeable<Double>` parameters is unknown, so the hook only reads the name and the namer. If Tensura fires the event before it commits the naming, our consequences run one tick
  later on the server thread; if the event is cancelled by another listener after ours, the Avatar would be marked named anyway (the hook cannot see that).
* Setting EP on a mob (`EnergyHelper.setBaseMaxEP`, the existence storage) assumes Tensura gives every living entity its attributes and storage; every Tensura call is guarded and logs once if it fails.
* `changeDimension` of a mob re-creating it from NBT (so `rebuildGoals` runs in `readAdditionalSaveData`), the chunk ticket waking, and the `neoforge:separate_transforms` item model loader are standard but not exercised.
* The held model's look (the preview python of the weapon pipeline does not render vanilla element models).
* The Tensura probability skill ids are found by name (probability/luck/fortune); if Tensura has none, that part of the luck is simply 0.
