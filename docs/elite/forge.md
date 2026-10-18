# Forge & Custom Crafting System (EliteTensuraAddon inventory, "forge")

The owner's inventory line: "Forge & Custom Crafting System. Station Tiers: Basic, Magisteel, Mythril, Dragonforge. Crafting Minigames: HammerTiming, MagiculeFlow,
RuneAlignment, TemperatureForge. Forge Mechanics: quality rolling (Common to Mythical), engrave mechanics (Engraving Handler), Station Upgrade, specialization options,
skill bonus resolutions (Skill Bonus Resolvers), and progression storage (Crafting Progress Storage)."

**The original addon's source was not available. Every number and rule below was DESIGNED here** (the names are the inventory's). Where a number is a matter of taste it
is in the data files or the config, not in code. Package `com.newuniverse.nusmp.elite.forge` (client half in `client/elite/forge`), one entry point
`ForgeRegistry.init(modBus, container)` wired by one line in `elite/EliteSystem.java`.

## What the mod already had (CLAUDE.md rule 5) and what each part is

| Part of the inventory | Already in the mod | Decision |
|---|---|---|
| Forge station blocks (4 tiers) | `relics/forge` Relic Forge (a menu table: relic grade, sockets, engraving books), `avatar/item/SoulForge*` (the character-creation ritual), Tensura's Smithing Bench (API), vanilla smithing table and anvil | **NEW**. The Relic Forge is a different job (relics) and stays untouched (EXISTS); the stations craft gear from recipe data |
| Recipe system (`data/nusmp/elite/forge/recipes`) | none (vanilla recipe JSON only; the Black Market has price tables) | **NEW** |
| Four minigames | none (a grep for "minigame" finds nothing) | **NEW** |
| Quality roller Common..Mythical | none. Relic "tiers" (`relics/tier`) and Hero "levels" are other ladders for other items | **NEW** (it is a separate ladder on forged items, as data components) |
| Engraving handler | `item/WeaponEngravings` (lore engravings, put once on the mod's own weapons), `relics/RelicEngravings`, the Relic Forge's Engrave tab (an enchanted book onto a mod weapon, 3 per weapon) | **EXISTS and kept**. **NEW**: the forge engraves by specialization pool, capped by quality; it counts its own engravings in a separate component, so it neither undoes nor repeats the lore pass |
| Station upgrade | none | **NEW** (kit item + crafting cost) |
| Specializations | none | **NEW** |
| Skill bonus resolvers | the Hephaestus skill (`skill/HephaestusSkill`, repairs the held item) | **EXISTS and kept**; **NEW** resolver reads it among others (see below) |
| Crafting progress storage | patterns only (HeroBond attachment, SkillTree state) | **NEW** attachment `nusmp:forge_progress` + world ledger |
| `EliteHooks.forgeCraftedCount` / `forgeBestQuality` | declared by the scaffold, default 0 | **OWNED and set** by `ForgeStore.install()` |
| Forged output items | all items exist (vanilla tools and armor, `nusmp:` weapons, Tensura ingots) | **NO new item models**: the output stacks reuse the existing items and their models (owner rule 7: nothing is invented). The only new items are the 4 station block items and 3 kits, with textures made by `tools/elite/` |

**Replacement:** none. Nothing existing was removed or changed except one additive edit (the four station ids added to the pickaxe tag).

## How it plays

1. Craft a **Basic Forge Station** (iron, furnaces, anvil, smooth stone) and place it. Right click opens the **station screen**.
2. Pick a recipe (list on the left, filter chips by Weapon / Armor / Tool / Rune; a recipe is red when the station tier is too low, green when you have the materials).
   The detail shows the materials with what you carry, optional **premium materials** (each adds quality points), the minigame sequence, and the quality a perfect and an
   ordinary play would reach here (the same `QualityRoller` the server uses).
3. **Forge** opens a session. The screen plays the recipe's minigames one after another (each starts with a click), sends only what was played, and shows the verdict:
   the item, its quality, the points it was made of, whether the station held it back, the engravings.
4. A **specialization** (Weaponsmith, Armorsmith, Toolsmith, Runesmith) is chosen once for free and changed later for experience levels.
5. **Engrave held item** puts one more Tensura engraving on a forged item in the main hand for experience levels, while the quality has free slots.
6. **Upgrade** a station by right clicking it with the kit of the next tier (the kit's crafting recipe is the cost).

### Station tiers (`rules/StationTier`)

| Tier | Quality points | Best quality it can make | Upgrade kit (crafted from) |
|---|---|---|---|
| Basic | +0 | Rare | crafted directly: 5 iron ingots, 2 furnaces, anvil, 3 smooth stone |
| Magisteel | +4 | Epic | 4 low magisteel ingots, 4 iron blocks, anvil |
| Mythril | +8 | Legendary | 4 mithril ingots, 2 high magisteel ingots, 2 diamond blocks, netherite ingot |
| Dragonforge | +13 | Mythical | 4 adamantite ingots, 2 hihiirokane ingots, 2 netherite blocks, nether star |

Why: a flat bonus alone makes the Basic station "almost as good"; the **ceiling** is what gives each tier a reason to exist (a Mythical item needs a Dragonforge
station however well you play). A recipe of tier N needs a station of tier N or better (config `enforceStationTier`). Upgrading is a block change in place; the
station's own statistics (items forged, best quality, number of upgrades) are copied to the new block entity.

### Quality roll (`rules/QualityRoller`, tested to the number)

```
points = recipe base (0..30) + 50 x minigame score (0..1) + station bonus + premium materials (cap 8) + skill bonus (cap 16) + specialization (0 or 6) + 4 x luck (-1..1)
quality = highest threshold reached: 30 Uncommon, 45 Rare, 60 Epic, 75 Legendary, 92 Mythical (else Common), then CAPPED by the station's ceiling
```
Playing badly (score under 0.10) can never go above Common, so flat bonuses cannot carry an item you did not work on. A poor forge is never destroying: it gives a Common
item (like the Relic Forge: "a failure never destroys"); only a REFUSED report gives nothing, and then it costs nothing. Luck can be switched off (`luck`).

What a quality does (`craft/QualityApplier`): the data component `nusmp:forge_quality` (1..6) and `nusmp:forge_smith` (who forged it); a bigger maximum durability
(+0%, 10, 20, 35, 55, 85%); an extra `ADD_MULTIPLIED_TOTAL` attribute modifier on the item's attack damage and armour entries (+0%, 4, 8, 13, 20, 30%), in the same slot
as the entry it grows. The item's own modifiers are copied first, so nothing is lost; a bow or hoe gets durability and the label only. The tooltip shows the quality, the
smith and the forge's engravings. `qualityStatScale` scales the strengths.

### Minigames (`rules/*`, pure, mouse driven; the client screens are `client/elite/forge/*View`)

| Minigame | Player does | Input reported (ints) | Server scoring |
|---|---|---|---|
| Hammer Timing | click (or Space) when the sweeping cursor is in the gold zone; `3 + difficulty` blows | blow times in ms | cursor is a triangle wave of the time with a seeded phase; per-blow accuracy 1 / 0.6 / 0 by distance from the zone |
| Magicule Flow | move the mouse above/below the middle of a tube to push a drifting needle back to the middle | 1 mouse sample (0..1000) per 50 ms tick | needle simulation with seeded 3-sine drift; hand push is slew-limited (0.35 per tick) |
| Rune Alignment | drag to turn concentric rings, right click to lock each as its rune meets a gold notch; rings keep turning by themselves | per ring: angle and lock time | angle + drift at the lock time vs the seeded target; within 3 degrees = full, 0 at `44 - 4 x difficulty` degrees |
| Temperature Forge | hold the left button (bellows) to heat, release to cool, keep the gauge in a moving band | 1 button sample (0/1) per tick | heat simulation, band moves on a seeded sine, a button cannot flip faster than every 2 ticks |

A recipe lists 1 to 4 steps with a difficulty 1..5 each (`minigames` in the recipe file).

**Server validation (`rules/SessionCheck`, tested):** the client never sends a score. The server picks one seed per step when the session opens, remembers the time,
and when the report arrives it (1) takes the session out of the book (a report counts once, and only for the player who started it), (2) checks the number of step results
and the SHAPE of each input (refused: wrong lengths, out-of-range numbers, impossible strike times such as a blow before 250 ms or two blows 180 ms apart), (3) re-scores
every step with the same pure code, clamped to 0..1000, (4) requires that the time the inputs CLAIM to have taken (whole sampled durations, the last strike, the lock
times) is not more than the real elapsed time plus 3 s and 5% (a result cannot come faster than it can be played), (5) refuses a session older than the sum of the steps'
limits plus two minutes. Physical limits of a hand (slew, flip rate) are applied inside the simulation rather than by refusing, so an honest twitch is never punished.
**Honest limit:** the server cannot tell a person from a macro that plays the game perfectly in real time (`perfectInput` of each game is exactly such a macro, and the
self-test uses it). What the design guarantees is: no forged, rushed, out-of-range or replayed result. The protection against a perfect bot is the cost: materials are taken
only on an accepted result, and the station ceiling and the recipe tiers bound what perfection can buy.

### Engraving (`rules/EngravingRules` + `craft/EngravingHandler`)

* Tensura's engravings are the enchantments of the `#tensura:engraving` tag. The handler applies ONLY ids that are in that tag, that the installed Tensura has, that the
  item can carry (`isSupportedItem`), that are not already on it and are compatible with its enchantments; anything else is skipped silently.
* **Slots by quality:** Common 0, Uncommon 1, Rare 2, Epic 3, Legendary 4, Mythical 5; +1 slot for a matching Weaponsmith / Armorsmith / Toolsmith on their category; never
  more than 6. The forge fills half (rounded up) when the item is made; the rest the player fills with the Engrave button. The number the forge put on an item is the
  component `nusmp:forge_engraved`, and the category is `nusmp:forge_kind` (so the slot bonus is the same later).
* **Level:** 1, +1 from Epic, +1 more at Mythical, +1 for a Runesmith, capped by each enchantment's own maximum.
* **Pools by specialization** (ids resolved softly): Weaponsmith: severance, barrier_piercing, swift, crushing, sturdy, enervation. Armorsmith: sturdy, magic_protection,
  vitality, restoration, magicule_absorption. Toolsmith: sturdy, swift, restoration, vitality. Runesmith: magic_weapon, magic_interference, magicule_absorption,
  elemental_boost, holy_weapon, energy_steal, soul_eater. No specialization: sturdy, swift, restoration. The order is a seeded shuffle.
* Owner rule 6 (every weapon gets engravings): the forge makes no new weapon item, it engraves what it forges (weapons, armor and rune work carry `engrave: true`); the
  mod's own weapons keep their lore engravings from `WeaponEngravings` as before.

### Specializations (`rules/Specialization`)

Chosen at a station: the first choice is free, a change costs `specializationChangeLevels` experience levels (creative pays nothing). A matching category gets +6 quality
points; the engraving slot / level bonus and the pool are above. `/multiverse elite forge spec set|clear` sets one without the cost.

### Skill Bonus Resolvers (`rules/SkillBonusTable`, `craft/SkillProbe`, `data/nusmp/elite/forge/skill_bonus.json`)

A data table of skill registry ids and the quality points each pays when learned and when mastered, capped at 16 in total. The resolver is pure; the only Minecraft part
asks the player's ManasCore skill storage by `ResourceLocation` and treats anything that goes wrong (unknown id, no storage, an exception from another mod's skill) as "not
owned", so the table works whether or not a skill exists. Shipped rows: `nusmp:hephaestus` (5 / 8, an EXISTING skill), `nusmp:runesmiths_insight` (5 / 9, the elite skill
the skills builder may add), `nusmp:forge_mastery` (4 / 7, reserved), `tensura:creator`, `great_sage`, `sage`, `analytical_appraisal`, `molecular_manipulation`,
`thought_acceleration`. A pack can replace the file.

### Crafting Progress Storage (`rules/ForgeProgress`, `store/*`)

The attachment `nusmp:forge_progress` (saved with the player, copied on death, not written for a player who never forged) holds: items crafted in total and per station
tier and per quality, the best quality, the highest station used, the recipes forged at least once (the station knowledge shown as a star in the list), the specialization
and how many times it was changed. The pure class converts to and from plain maps; `ProgressTag` converts those to NBT and checks every key's tag type, so a missing key, a
key of another type or a junk list falls back to the default (tests, and an in-game self-test). It is synced to the client (login, after every change). The two
`EliteHooks` this subsystem owns read it through a small world-saved **ledger** (UUID -> items crafted, best quality), so they also answer for a player who is offline.
Both answer 0 while the server is not running or the master switch is off.

## Recipes (`data/nusmp/elite/forge/recipes/*.json`, 48 files)

Format (parsed and validated by `rules/ForgeRecipeDef`, tested against every shipped file):
```
{ "tier": 0..3, "category": "weapon|armor|tool|rune", "output": {"item","count"}, "inputs": [1..6 {"item","count"}], "premium": [{"item","count","bonus 1..8"}],
  "minigames": [1..4 {"type","difficulty 1..5"}], "base_quality": 0..30, "engrave": true|false }
```
The id of a recipe is `<namespace>:<file name>`; another pack can add or replace recipes. A recipe naming an item that does not exist is kept but marked unavailable (and logged)
so one typo or one missing Tensura item never breaks the others; the self-test `elite.forge.recipes` fails on it so CI shows it.

| Tier | Recipes (count) |
|---|---|
| Basic (0) | iron sword, axe, pickaxe, shovel, hoe, helmet, chestplate, leggings, boots; golden sword; bow; crossbow; chainmail chestplate; shield; magic tool sword / spear / bow (rune work with Tensura magic stones and crystals) (17) |
| Magisteel (1) | diamond sword, axe, pickaxe, shovel, helmet, chestplate, leggings, boots; permafrost sabre; cyclone rapier; spell-forged rapier (11) |
| Mythril (2) | netherite sword, axe, pickaxe, helmet, chestplate, leggings, boots; trident; magebane longsword; prism blade; severing greatsword; magebane shield (12) |
| Dragonforge (3) | mace; thundercleave; bloodbound katana; rimeheart runeblade; doom hammer; netherite chestplate, helmet and pickaxe made again at the top station for a better chance at Mythical (8) |

(48 in total: 17 + 11 + 12 + 8. The table is generated from `tools/elite/gen_forge_recipes.py`; edit that table and run it.) The `nusmp:` outputs are existing weapons of this
mod; the owner may prefer some of them to stay loot-only, in which case their files are simply deleted. Tensura ids used are the ones other files of this mod already rely on
(`low_magisteel_ingot`, `high_magisteel_ingot`, `mithril_ingot`, `adamantite_ingot`, `hihiirokane_ingot`, `magic_stone`, the three magic crystals, `daemon_essence`); Tensura
tool and armor ids were NOT guessed (they cannot be verified from the API stubs), so no Tensura tool or armor is a recipe output yet.

## Commands, config, hooks

* `/multiverse elite forge give station <tier> [player]`, `give kit <magisteel|mythril|dragonforge> [player]`, `spec set <player> <spec>`, `spec clear <player>`,
  `recipes` (how many are loaded and which are unavailable) are operator commands (permission 2); `info [player]` shows progress, the skill bonus and, when looking at a
  station, its numbers (anyone may read their own). The branch is merged with the `/multiverse elite` branch the other elite systems register (Brigadier merges nodes).
* `nusmp-elite-forge-server.toml`: `enabled` (master switch), `enforceStationTier`, `luck`, `announceMythical`, `specializationChangeLevels` (20), `engraveLevels` (5),
  `stationRange` (8), `qualityStatScale` (1.0).
* Hooks owned: `EliteHooks.forgeCraftedCount` (items forged), `EliteHooks.forgeBestQuality` (0 none .. 6 Mythical).

## Files

| File | Why |
|---|---|
| `ForgeRegistry` | the one entry point: registers, config, packets, reload listener, events, creative tab (by the tab-contents event), commands, self-tests, hooks |
| `ForgeConfig`, `ForgeComponents`, `ForgeCommand`, `ForgeSelfTests` | config, the four data components, the commands, the in-game self-tests |
| `rules/*` | all pure rules: `Quality`, `StationTier`, `Category`, `Specialization`, `QualityRoller`, `SkillBonusTable`, `EngravingRules`, `ForgeProgress`, `ForgeRecipeDef`, `ForgeRng`, `Minigame` + the four games, `Minigames`, `SessionCheck` (no Minecraft classes, JUnit-tested) |
| `block/*`, `item/UpgradeKitItem`, `station/StationUpgrade` | the station block (one class, 4 tiers, `lit` while forging), its block entity (statistics), the kit, the upgrade rule |
| `data/ForgeRecipes` | the data reload listener (recipes + skill table) with the registry check |
| `craft/*` | `QualityApplier`, `EngravingHandler`, `SkillProbe`, `Inventories` (all-or-nothing payment) |
| `store/*` | attachment, NBT adapter, world ledger, hook installation |
| `session/*` | the book of open sessions, the finisher (the one place an item is made) |
| `net/*` | nine packets (4 server to client, 5 client to server), their server logic, and the late-bound client bridge |
| `client/elite/forge/*` | station screen, minigame host screen, four views, drawing helpers, client wiring and tooltip |
| `tools/elite/*` | `gen_forge_recipes.py`, `gen_forge_station_textures.py`, `gen_forge_station_assets.py`, `forge_lang.json` |

## Tests

* JUnit (pure, run here): `rules/MinigamesTest`, `SessionCheckTest`, `QualityRollerTest`, `EngravingRulesTest`, `ForgeProgressTest`, `ForgeDataTest` (all recipe files and the
  skill table), and `ForgeLangTest`, `ForgeAssetsTest` (every key and every asset reference).
* In-game self-tests (key prefix `elite.forge.`, add `elite.forge` to `SMOKE_EXPECT`): `registry`, `recipes`, `quality`, `upgrade`, `flow` (a whole session from the begin to
  the item in the inventory, with the clock replaced, a long sequence refused when reported too early at no cost, a tier-too-low refusal), `engrave`, `storage`, `skills`, `net`.
  These have been TYPE-CHECKED only (the Gradle build and the game cannot run in the authoring environment); they have not been executed.

## Merge notes for the lead

* `tools/elite/forge_lang.json` is a fragment; its keys use the namespace `nusmp.eforge.*` (the Relic Forge owns `nusmp.forge.*`), and `ForgeLangTest` checks that none exists in
  en_us.json yet and that every key the code uses is in the fragment.
* One additive edit to an existing shared data file: the four station ids appended to `data/minecraft/tags/block/mineable/pickaxe.json`.
