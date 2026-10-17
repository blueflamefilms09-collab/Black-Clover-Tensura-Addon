# Avatar system, part AV4: the Soul Forge (rebuild of the creation, release 1.58.0)

The owner's brief ("what to use to REBUILD the Avatar creation system") asked for a ritual instead of a button: a craftable altar, a locked player, a circle of light,
a full-screen character creator in the spirit of a life-simulation creator, a free gamble for a rare gift against a heavy permanent tax for choosing it, and a birth
with a beam, a camera shake and a first ability. This part builds all of it on top of the 1.51.0 system (`av1.md` entity and outcomes, `av2.md` AI, `av3.md` creation
screen and networking) without removing any of it.

Nothing was run in the game. What was checked is in "What was checked" below; the first run in the game is the real test.
Namespace note: the real mod id is `nusmp` (the brief says `multiverse_mod`), so ids and datapack paths use `nusmp`.

## NEW / REPLACES / KEPT
| | What |
|---|---|
| NEW | the block `nusmp:soul_forge` (altar, reusable), its item with a 3D in-hand model and the old-style 16x16 inventory sprite, loot table, blockstate, textures |
| NEW | the recipes `soul_forge` and `genesis_core` (until now a Genesis Core could only be given by command or dropped by a boss) |
| NEW | the ritual (`avatar/ritual/`): crash-safe player lock, circle, the screen opening, the cancel paths, the birth spectacle |
| NEW | the full-screen creator `SoulForgeScreen` with five round category buttons and five pages (Identity with a 3D die, Race & Appearance, Traits with a sample line, Skills, Aptitudes with a slot-machine reel) |
| NEW | Magic Aptitudes as data (`data/nusmp/avatar_aptitudes/*.json`, 30 files) with a reload listener; the chosen-skill list (`data/nusmp/avatar_skills/*.json`, 42 files) with its own reload listener |
| NEW | six payloads, the server-side validator and the cost maths, `AvatarSoulForgeConfig` (own file), the Instant Skill Awakening, in-game self-tests, quest keys |
| REPLACES | the creation FLOW: the Genesis Core no longer opens a screen when used alone (`avatar.coreUse = "forge"`), and the Soul Forge screen is the default creation screen (`avatar.creationUi = "soulforge"`) |
| KEPT | the 1.51.0 four-page screen `AvatarBuilderScreen` and its menu, behind `avatar.creationUi = "classic"`; every classic payload and handler (`C2S_RequestStatRollPayload` is shared: the stat roll of page 1 serves both screens); the stat roll, reroll rules, races, personalities, appearance engine, entity, outcomes, naming, chatter |
| KEPT | the Contract classes `Aptitude` and `AptitudeCatalog` (untouched), `AvatarEntity` setters (`setMagicAptitudes`, `setChosenSkills`, `setPermanentTax`) |

## How a creation goes
1. Craft a Genesis Core and a Soul Forge, place the forge. Using the forge with a Genesis Core anywhere in the inventory starts the ritual
   (`SoulForgeBlock.useWithoutItem` -> `SoulForgeRitual.interact`). It is refused, with a plain message, when the Avatar system is off, the player already has the
   maximum number of Avatars, has no core, started a ritual less than `startGapSeconds` ago, or is dead, a spectator, riding or asleep.
2. The player is locked (`RitualLock`: two transient attribute modifiers that multiply walking and jumping by 0, so they are never saved and a crash cannot trap anyone),
   the great circle is drawn (`RitualFx`, `VfxSpawn` shapes), the quest key `avatar.ritual_started` fires.
3. After `ritualSeconds` (default 4) the server opens `SoulForgeMenu` and sends the race list (`S2C_RacesPayload`, unchanged) and the state (`S2C_SoulForgeStatePayload`:
   rules, the creator's reserves, free rolls left, both catalogs). The screen may stay open `maxOpenMinutes` (10).
4. Page 1 rolls the stats exactly as before. The Aptitude page's "Randomize Aptitude" asks the server (`C2S_AptitudeRollPayload` carrying only the hand picks); the
   server rolls (class by `Rarity.weight()` shifted by luck, then one aptitude of that class), keeps the result and a token, answers `S2C_AptitudeRollPayload`. The screen
   plays a reel that slows exactly onto the answer; only then does the aptitude drop into its slot.
5. Finalize sends only CHOICES (`C2S_FinalizeSoulForgePayload`: race, personalities, look, name, the stat token, the two skill ids, the hand-picked aptitude ids, the roll
   TOKEN, the paid-awakening flag). The server cleans every string, re-reads the creator's reserves from the game (`ForgeReserves`), runs the pure `ForgeValidator`
   (ids exist, slots and kinds right, the roll is the one the server made, nothing twice, the creator can pay) and answers a refusal in plain words, or accepts, closes the
   screen and starts the spectacle. Nothing has been taken yet.
6. The birth (`RitualClock`, default 50 ticks): the circle charges and a thin beam tells, the beam strikes (camera shake packet), and at the SPAWN beat the creation is checked
   AGAIN (`AvatarCreation.finalizeForge`) and only then paid: the race's magicule and the up-front magicule are spent, the Genesis Core(s) consumed, the permanent tax applied
   (`AvatarEntity.setPermanentTax` + `TensuraNamingEngine.refreshTax`), aptitudes and chosen skills written, the Instant Skill Awakening rolled, the Avatar spawned at the
   forge. Then it shouts its first line and shoves nearby hostile creatures back (`BirthSpectacle`; no damage, the birth is never a weapon), and the lock is lifted.
7. Cancel: closing the screen, being hurt, dying, changing dimension, logging out, straying beyond `reach`, the forge being broken, or the timeout ends the ritual and gives
   the player their movement back. Because the commit is at the spawn beat, a creator who dies during the first second of the spectacle loses nothing.

## Recipes and economics
* `genesis_core` (shaped `IBI / HNT / EAR`): 2 netherite ingots, diamond block, heart of the sea, nether star, totem of undying, echo shard, enchanted golden apple,
  `nusmp:dark_rukh_shard`. Every Avatar eats one, so this is the price of one Avatar.
* `soul_forge` (shaped `CNC / IGI / CAC`): 4 crying obsidian, nether star, 2 netherite ingots, one Genesis Core, enchanted golden apple. The forge is never used up.
* Both ship unconditionally (no `neoforge:conditions`): a server that does not want them removes the recipe with a datapack, and `avatar.enabled = false` already stops every
  use. No recipe-book advancement is shipped (the recipes work in a crafting table whether or not they are unlocked in the book).
* Quest keys `avatar.forge_crafted` and `avatar.core_crafted` fire when the player crafts them.

Prices (all scale-free: a share of the CREATOR's own maximum magicule, so they mean the same on any server's numbers):
* permanent tax = sum over HAND-PICKED aptitudes of `tax_percent / 100 * aptitudeTaxScale` + sum over chosen skills of `ep_tax_percent / 100 * skillTaxScale`
  + `awakeningTaxPercent / 100` when the creator pays for the guaranteed awakening; never more than 0.9; the creator's existing tax plus this must not pass `maxTotalTaxPercent` (60).
* magicule paid at once = tax x `upfrontFactor` x maximum magicule, plus the race's own base cost.
* a skill also needs the creator's MAXIMUM EP to reach its `min_ep` (not in creative).
* Genesis Cores: 1, plus `awakeningExtraCores` (1) for the guaranteed awakening.
* a ROLLED aptitude costs no tax and no magicule (that is the point of the gamble) but takes one of the `maxAptitudes` slots.
* tax ladder of the shipped aptitudes: common 0, uncommon 3 to 4, rare 7 to 10, epic 12 to 15, legendary 20 to 25 percent; Time (legendary) 20, Anti-Magic (legendary) 25.
* example: a creator with 9,800 maximum magicule who hand-picks Spatial (rare, 10) and binds the Unique skill Chef (2) pays a permanent 12 percent (1,176 magicule of their
  pool, for as long as the Avatar lives) and 1,176 magicule now plus the race's base cost, and needs 200 EP.
* Instant Skill Awakening: at birth a d100; strictly below `awakeningBase + total luck` (capped by `awakeningMaxChance`, 75) awakens a Unique/Ultimate perk
  (`AvatarUniqueSkill`) at once; paying makes it certain. An awakened or skill-bound Avatar skips the three-day betrayal roll when `chosenSkillsSecure` is true.

## Config (`AvatarSoulForgeConfig`, SERVER, file `nusmp-avatar-soulforge-server.toml`, section `avatar`)
`creationUi` "soulforge" | "classic"; `coreUse` "forge" | "item"; `maxAptitudes` 2 (0..8); `ritualSeconds` 4; `maxOpenMinutes` 10; `reach` 6.0; `startGapSeconds` 3;
`birthTicks` 50; `aptitudeTaxScale` 1.0; `skillTaxScale` 1.0; `maxTotalTaxPercent` 60; `upfrontFactor` 1.0; `aptitudeRolls` 3; `aptitudeLuckStrength` 0.06;
`awakeningBase` 5; `awakeningMaxChance` 75; `awakeningTaxPercent` 5; `awakeningExtraCores` 1; `chosenSkillsSecure` true; `effects` true; `sigilRadius` 4.5;
`birthShake` 1.0; `rollGapMillis` 1200; `actionGapMillis` 250; `finalizeGapMillis` 1500. Every value is a plain `define` / `defineInRange` read through a `get(...)` helper
that falls back to the default while the spec is not loaded (copied from `DndConfig`); the two strings are cleaned at read time, never `defineInList`.

## Payloads (`AvatarNet.register`, `optional()`, all handlers on the main thread)
| id | direction | content | guards |
|---|---|---|---|
| `nusmp:forge_finalize` | C2S | race id, primary, secondary, look NBT, name, stat token, unique id, ultimate id, <= 8 aptitude ids, roll token, paid flag | menu + ritual token match, `finalizeGapMillis`, every string cleaned, then the validator on fresh reserves |
| `nusmp:forge_roll` | C2S | the hand-picked ids only | menu + ritual, `rollGapMillis`, `aptitudeRolls` per ritual, slots not full |
| `nusmp:forge_bark` | C2S | primary and secondary personality names | menu + ritual, `actionGapMillis` |
| `nusmp:forge_state` | S2C | rules, reserves, rolls left, aptitude rows, skill rows | sent by the server when the screen opens |
| `nusmp:forge_roll_result` | S2C | aptitude id, token, rolls left, reel seed | the result is the server's; Finalize refers to the TOKEN |
| `nusmp:forge_birth` | S2C | position, power, ticks of the camera shake | players within 48 blocks |
The classic payloads are unchanged. `AvatarNetServer` now lets the stat roll serve both screens, and the classic Finalize requires the classic menu.
The sample line travels as an ordinary `S2C_CreationResultPayload` of kind `bark` with the message `EVENT|words`.

## Data
* `data/nusmp/avatar_aptitudes/<id>.json` (30): `name`, `rarity`, `tax_percent`, `magic` (a real `MagicType` name or empty), `skills` (real Tensura skill ids), `tint`, `icon`, `description`.
  Generated from `tools/avatar/forge_tables.py` by `tools/avatar/gen_forge_data.py` (which checks every magic name against `MagicType.java` and every skill id against the Tensura id
  reference `src/test/resources/com/newuniverse/nusmp/charter/tensura_skill_ids.txt`, and refuses to write otherwise; `--check` verifies the files on disk).
* `data/nusmp/avatar_skills/<id>.json` (42): `id`, `kind` (a HINT), `name`, `ep_tax_percent`, `min_ep`, `tint`, `icon`, `description`.
* Finding: Tensura 2.0.1.3 registers no Ultimate skill of its own (only the `SkillType.ULTIMATE` category), so the Ultimate slot is filled from the Tensura sins that are
  Ultimate in the series (the server reads the real category at runtime and the registry wins over the file's hint) and from this mod's own Ultimates (`NUSkills`).
  `ForgeSkillResolver` leaves out any id that is not registered, logging it once, and puts every skill in the slot its real category says.
* icons: `tools/avatar/gen_aptitude_icons.py` (deterministic, 32x32 per aptitude), `tools/avatar/gen_soul_forge_assets.py` (block textures, item sprite, GUI ring, disc, slot, five
  category glyphs).

## Classes and why they exist
| Class | Why |
|---|---|
| `avatar/aptitude/ForgeRules`, `ForgeContext`, `ForgeChoices`, `ForgeCosts`, `ForgePlan`, `ForgeValidator` | the pure anti-cheat core: one formula for the bill, one judge, used by the server, the screen and the tests alike |
| `AptitudeRoll` | the pure weighted roll (rarity classes by `Rarity.weight()` times `exp(strength * luck * rank)`) |
| `AptitudeParser`, `AptitudeLoader`, `AptitudeLook`, `ForgeSkill`, `ForgeSkillParser`, `ForgeSkills`, `ForgeSkillLoader`, `ForgeJson` | data formats and the two reload listeners (a bad file is skipped and logged, never breaks a reload) |
| `avatar/ritual/RitualClock` | the pure timeline (charge, open, timeout, the birth beats exactly once) |
| `RitualLock`, `RitualFx`, `BirthSpectacle`, `ForgeReserves`, `ForgeSkillResolver` | the lock, the effects, the first ability, the fresh reserves, the registry check |
| `SoulForgeRitual`, `SoulForgeSystem`, `SoulForgeSelfTests` | the per-player director, the one-line registration and its game-bus rules, the CI self-tests |
| `avatar/item/SoulForgeBlocks`, `SoulForgeBlock`, `SoulForgeItem` | the altar, its item, its tooltip; `GenesisCoreItem` gained `countCores`, `consume(p, n)`, the forge-only use |
| `avatar/menu/SoulForgeMenu`, `AvatarMenus` | the slotless menu that is the server's proof the screen is open (and the cancel path through `removed`) |
| `avatar/menu/ForgeState`, `ForgeLayout`, `ForgeMotion`, `SlotReel`, `DiceSpin` | the screen's pure brains: choices and problems, layout and zoom, eased slides and hover, the reel, the icosahedron |
| `avatar/net/*` (6 payloads, `ForgeAptitudeRow`, `ForgeSkillRow`, `SoulForgeNetServer`) | the packets and their server handlers |
| `AvatarCreation`, `AvatarSoulForgeConfig` | the Forge variants of the creation (`precheckForge`, `finalizeForge`) and the config |
| `client/avatar/SoulForgeScreen`, `ForgeHost`, `ForgePage` + `IdentityPage`, `AppearancePage`, `TraitsPage`, `SkillsPage`, `AptitudesPage`, `ForgeControls`, `ForgeUi`, `ForgeIcons`, `ForgeDice3D`, `ForgeText`, `SoulForgeClientData` | the screen, one class per page, shared controls and drawing, texture lookup with a letter fallback, the die, the text helper, the packet data |

## The screen
Five round buttons at the top (Identity, Appearance, Traits, Skills, Aptitudes); a red dot says the page still needs something, a green one that it is done. The module of the open page
slides out and the next slides in with an eased curve (`ForgeMotion.ModuleSlide`: never two pages at once, reversible in flight). The Avatar in the middle turns slowly (drag to turn
it), reacts live to every change (its look updates at once, a ring of the choice's colour opens, the aura leans toward the hovered gift, chosen gifts circle its feet) and is drawn by the
repo's GeoLite `AvatarPreview` (deviation: GeckoLib is not a dependency of this mod). The price panel on the right (below the Avatar in a narrow window) shows the permanent tax, the
magicule now, the cores and the awakening chance, built by the same `ForgeCosts` the server uses; what stands between the creator and Finalize is listed in words. A shortage of magicule is
only a warning (the number can change while the screen is open; the server answers); everything else blocks the button.
The scene is composed for a canvas of about 430x243 to 640x360 pixels and a bigger window shows it at 2, 3 or 4 times the size (`ForgeLayout.uiScale`), so a 1080p window at GUI scale 2 looks like the default.
Every text is `ForgeText.t(key, english)`: the screen reads fine before the language file is merged, and `tools/avatar/gen_soulforge_lang.py` writes the fragment from the code.

## What was checked
* Pure logic, with the JUnit shim (`jt.sh`): `AptitudeDataTest`, `ForgeSkillDataTest`, `AptitudeRollTest`, `ForgeCostsTest`, `ForgeValidatorTest` (the hacked-client cases), `RitualClockTest`,
  `ForgeRecipesTest`, `ForgeLayoutTest` (hundreds of window sizes), `ForgeMotionTest`, `SlotReelTest`, `DiceSpinTest`, `ForgeStateTest`.
* Type-checking against the API stubs, with a negative control (a deliberate wrong `GuiGraphics` call is reported): every main file of this part, client included, has no error; the errors the stubs
  produce elsewhere (abstract `PlayerModel`, `MobEffectInstance`, `INBTSerializable`) are the known artifacts and are not in this part's files.
* The screen was RUN against a shim (`tools/avatar/ui_harness/run_ui_harness.py <out-dir>`): the real `SoulForgeScreen` and its pages, unchanged, drawn with Java2D at three window sizes
  while a scripted visit rolls the stats, types a name, picks a race, personalities, a skill and an aptitude, watches the reel stop and presses Finalize; 36 checks, the request it sends is
  printed.
* Pure geometry previews: `python3 -B tools/avatar/preview_forge_ui.py <out-dir>` (layouts, the die, the reel, the slide); the block model: `tools/avatar/preview_block_model.py`.

## Quest keys (`NusmpEvents.fire`)
`avatar.forge_crafted` (the player crafted a Soul Forge), `avatar.core_crafted` (a Genesis Core), `avatar.ritual_started`, `avatar.created`, `avatar.aptitude` (argument: the aptitude id,
once per aptitude the Avatar was given), `avatar.skill_bound` (argument: the skill id), `avatar.awakened_instant` (argument `paid` or `luck`). The lead wires the advancements.

## Integration (the lead's lines; this part edited no shared file)
* `AvatarSystem.init`: add `SoulForgeSystem.init(modEventBus);` after `AvatarMenus.init(modEventBus)` and before `NUItems.ITEMS.register(modEventBus)` (the block item is put on that register when
  `SoulForgeBlocks` loads).
* `NUSMP`: `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.avatar.AvatarSoulForgeConfig.SPEC, "nusmp-avatar-soulforge-server.toml");`
* `NUCreativeTab`: `output.accept(com.newuniverse.nusmp.avatar.item.SoulForgeBlocks.SOUL_FORGE_ITEM.get());`
* optional: `"nusmp:soul_forge"` in `data/minecraft/tags/block/mineable/pickaxe.json` (not edited here; the block drops without a tool requirement).
* the language fragment: `python3 -B tools/avatar/gen_soulforge_lang.py --out <fragment.json>` (keys already in `en_us.json` are never written).
* CI smoke: add `avatar` to `SMOKE_EXPECT` (the self-tests `avatar.forge_registry`, `forge_data`, `forge_recipes`, `forge_skills`, `forge_validator`, `forge_lock`, `forge_ritual`).

## Unverified (read these first when it is first run)
* The whole screen in the real game: fonts, the 3D Avatar in the middle (`AvatarPreview` is AV3's and was not changed), depth order of the overlays at z 200, the blend of the halos,
  scissor behaviour under the slide and the zoom (assumed to ignore the pose, as in 1.21.1), the tooltip position, the EditBox inside the module.
* The payload codecs (written by hand with `StreamCodec.of`) were type-checked but never encoded and decoded; the per-string limits are the same on both sides.
* The skill icons: Tensura's textures are read at run time from the skill registry (`ManasSkill.getSkillIcon`); the harness had no Tensura jar, so only the letter-in-a-circle fallback was seen.
* Whether every skill id of `avatar_skills` resolves in Tensura 2.0.1.3 and in which slot (the self-test `avatar.forge_skills` names the ones that do not).
* The VFX shapes of the ritual (`DIVINE_SIGIL`, `DIVINE_RAY`, `MAGIC_CIRCLE_EXPLOSION`, `SPIRIT_AURA` ...) were chosen from reading `VfxSpawn`; none was seen.
* The in-game self-tests (key prefix `avatar`) were never run; they use a `FakePlayer` and never open a screen or create an Avatar.
* `TensuraNamingEngine.apply` (the naming code of the AI builder) SETS the permanent tax, which would erase the forge's tax the moment the Avatar is named, and `AvatarOutcomes` sets it to 0
  for the Soul Awakening outcome; naming should ADD to the existing tax (capped at 0.9).
* `AvatarNet.registerEvents(gameBus)` is never called by `AvatarSystem`; `SoulForgeSystem` clears the rate limiters on logout itself.
* The lock only multiplies walking and jumping by zero: a creator can still turn, open the inventory or be pushed (the ritual ends if they stray beyond `reach` or are hurt).

## Sources
The owner's brief and the 1.51.0 documents `av1.md`, `av2.md`, `av3.md`; the repository's own classes (`VfxSpawn`, `Tells`, `MagicCircle`, `UiPanel`, `NusmpEvents`, `SelfTests`, `DndConfig`,
`BossSkillRules`); the Tensura 2.0.1.3 id reference in `src/test/resources/com/newuniverse/nusmp/charter/`. No web source was used: the icosahedron is the textbook golden-ratio solid.
