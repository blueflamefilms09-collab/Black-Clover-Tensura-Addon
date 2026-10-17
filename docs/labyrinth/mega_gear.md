# Mega-Dungeon: standard Normal / Special / Rare equipment, dropped without limit (`mega.gear`, 1.55.0, builder GEAR)

Package `com.newuniverse.nusmp.labyrinth.mega.gear`, data `data/nusmp/mega/gear/tables.json`. **Nothing here was run in game.** The pure classes are
unit tested (JUnit, 86 tests, mutation checked), the game classes were type-checked against the project's API stubs (rebuilt from the API dump, so they
know the 1.21 data-component classes), the shipped table is read and checked by `GearDataTest`, and five IN-GAME self-tests (`gear.*`, section 10) are
registered with the shared `SelfTests` registry so the CI server smoke test exercises the real thing. CI is the first real compile. Three pictures of the
grades, the chances (kills AND chests) and example tooltips are drawn by `tools/labyrinth/mega_gear_preview.py` (section 13). The chest side (the owner's
fourth brief: "from mobs AND standard chests", "Global Loot Modifier JSONs") is section 14.

The brief (system 5, "Abyssal Descents"): *"Standard Loot (Normal, Special, Rare): the `LivingDropsEvent` logic where monsters drop physical Magic
Stones and Monster Meat, alongside lower-tier equipment. Based on the Tensura grading scale, 'Normal' (standard everyday items without magic), 'Special'
(refined craftsmanship), and 'Rare' (high-grade items providing magic resistance) equipment can drop infinitely across the floors. They are not
restricted by world-limits."* And system 8: *"While lower-tier items drop globally, Unique, Legend, Mythical and Genesis tier Relics are strictly
restricted"* (to the Crucible, another builder).

## 1. New, replacement, kept (owner rule 5)

| Part | Status | Notes |
|---|---|---|
| Standard gear: three grades of ordinary vanilla items stamped with the data component `nusmp:gear_grade` | NEW | Nothing like it existed. Not a relic, not a new item class. |
| `GearDrops`: a third `LivingDropsEvent` listener of the Mega-Dungeon | NEW | Added NEXT TO `EconomyDrops` (1.36.0) and `FactionDrops` (1.54.0), which are untouched and keep paying stones and meat. |
| `GearEvents`: magic resistance on `LivingIncomingDamageEvent` | NEW | Works anywhere, for anything that wears or holds the gear. |
| Drop tables as data (`tables.json`) and the reload listener | NEW | Same pattern as the faction files. |
| Admin command `gear give / roll / status` | NEW | The lead attaches it under `/multiverse mega`. |
| Quest key `mega.gear` | NEW | Contract key of `mega_floors.md` section 4. |
| Magic Stones and monster meat | KEPT | Not touched, not duplicated. |
| The 236 low-grade relics, the Order's ledger (`UniqueLedger`), the Crucible's Unique-and-above relics | KEPT | Unchanged and **not used**: a test (`GearIsNotARelicTest`) fails the build if the gear package starts importing them. |
| The relic `Grade` enum (names, colours, labels) | KEPT, REUSED | `GearLook` reads colour and label from it so the three tiers look exactly like the relic tiers. |
| `item/WeaponEngravings`, `item/ArmorEngravings` | KEPT, unedited | They engrave `nusmp:` items only (their first lines return for any other namespace), so they cannot list a vanilla base. The engravings of the standard gear are listed in `tables.json` (owner rule 6, see section 6.3). |
| 1.55.0, fourth brief: `ChestLootModifier`, a NeoForge global loot modifier `nusmp:gear_chests` with its JSON and the classes `ChestRules`, `ChestGear`, `ChestSelfTests` (section 14) | NEW | Gear also drops from standard chests of the Mega-Dungeon. |
| `data/neoforge/loot_modifiers/global_loot_modifiers.json` (the list of global loot modifiers, which already named `nusmp:relic_chests`) | KEPT, one entry ADDED | The relic modifier stays listed; `nusmp:gear_chests` is appended. |
| `caps` of `tables.json` (0.30 / 0.20 / 0.15 / 0.50 became 0.40 / 0.30 / 0.25 / 0.60) | CHANGED by me, my own file | Safety ceilings only; raised because the chest odds reached the old ones (section 14.5). No kill rate changes. |
| Nothing else was replaced and nothing was removed. | | |

## 2. The central decision: ordinary items, not relics

The 236 low-grade relics of the mod are one-of-a-kind items under the Order's ledger (`relics/order/UniqueLedger`, `LedgerRules`: *every relic that is
not a consumable may exist once in the world; a stack carries a serial, the ledger remembers the one valid copy*; the advancement text says it to the
player: *"each one exists only once in the whole world"*). A relic can therefore never "drop infinitely", and the brief says the standard gear must. So the
standard gear is **not made of relics at all**: it is a vanilla item (an iron sword, a diamond chestplate) stamped with a data component that no relic
code ever scans. It has no serial, so the ledger never sees it and cannot refuse, adopt or destroy it; it has no relic definition, so none of the relic
engines (attunement, acts, passives, null-fields) treats it as a relic. It has **no world limit by construction**, not by a switch that could be turned
off. The relic `Grade` colours and names are reused only for the look.

## 3. What happens on a kill

1. `LivingDropsEvent` (game bus, LOW priority, so after the economy's and the faction layer's listeners): the dying thing must be a `Mob` in the
   Mega-Dungeon, `gear.enabled` must be on, and a player must have killed it (the damage source's player, else the kill credit, exactly as the economy
   decides it). A death no player caused (a turf war, a trap, the void) pays no gear.
2. **A sector Guardian** (scoreboard tag `nusmp_sector_guardian`) gets no roll but a guarantee: one Rare piece, when `gear.bossGuaranteed` is on. **Any
   other boss** (the economy's own tests: the DPS-budget interface, the Floor Warden class, the boss and wanderer tags, `c:bosses`, the Warden id) and any
   **servant** (a tame or owned creature, a Paladin, a shadow soldier, a relic summon: the faction layer's `TurfMarks` list) drops no standard gear.
3. Any other **hostile `Enemy` or Dungeon Master creature** (scoreboard tag `nusmp_dm`, `MegaDungeon.isDungeonMonster`: Tensura's monsters are tamable
   animals and not `Enemy`, so the economy, the HP buffer and the faction drops already ask the tag too): one roll. The floor is `MegaDungeon.floorAt` where it died (Floor 0 and the gaps between sectors count as floor 1;
   the Abyssal road is as deep as the Holy one: only the distance from the Hub counts, `FloorMap.threat`). The three grades' chances are laid end to end
   on one random number, so **at most one piece per kill**. A hit picks the slot by weight, a base item from the grade's list, the bonuses, the
   enchantments and engravings, the resistance: a `GearSpec`.
4. `GearFactory` turns the spec into an `ItemStack` and it is added to the drops as an item entity. The first time a player **picks up** a piece of each
   grade the quest key `mega.gear` fires with the grade id.

## 4. The WHY of the decisions

* **Why a data component and not a new item class.** The owner wants standard gear to be "ordinary equipment": usable in any recipe, enchantable at any
  table, repairable at any anvil, displayed by any inventory mod. A new item class would be a new registry entry, a model, a texture and a recipe for
  nothing, and it could not be an iron sword. A component is saved and synced by the game itself, survives copying, anvils and grindstones, and is what
  marks a stack as "standard gear" for the resistance event. It is also a one-line test for the relic guard: the component is the only thing the package
  registers.
* **Why the component also carries the slot.** The brief gave `GearInfo(grade, magicResistPercent)`; the slot was added (`grade, slot, magicResistPercent`)
  because the resistance must count armour only when it is WORN in its own position and a weapon or shield only when HELD. Without it a chestplate carried in
  the hand would resist magic, and a sword stuck on the head with a command would count twice. The other two fields and their meaning are unchanged.
* **Why the percentage bonus starts from the stack's existing attribute modifiers.** In 1.21 an item's attribute modifiers are a data component, and
  *setting* it REPLACES the item's own. A sword keeps its damage in a default component (setting yours would strip its base damage); armour is worse,
  because it keeps its numbers in the item class and the stack's component is empty, so setting just a bonus would remove ALL its armour. `GearFactory`
  therefore takes the current list (the component, or when it is empty the item's default list, which is exactly the fallback the game itself uses) and
  ADDS one modifier with `withModifierAdded`. The self-test `gear.roll` proves it on 300 real stacks: the finished piece still has the base item's own
  attack damage or armour AND the bonus.
* **Why the bonus is flat and relative to the item's own value.** "+15 % attack damage" is 15 % of what the tooltip shows (the item's damage plus the
  player's base 1), added as a flat amount, so it does not depend on what else the wearer has (Strength, other armour) and is easy to read: an iron sword
  (6) at +15 % is 6.9. Bows, crossbows and shields get no percentage: an attack-damage modifier on a bow changes nothing and a shield has no attribute;
  their strength is durability and enchantments.
* **Why the modifier id is fixed per slot (`nusmp:mega_gear/<slot>`).** The brief asked for a fixed id. In 1.21 two equipment pieces that apply the same id
  to the same attribute replace each other (the later removes the earlier), so four armour pieces sharing one id would keep only one bonus. One fixed id per
  slot keeps each piece's bonus and still makes re-stamping idempotent (`withModifierAdded` replaces an entry with the same id).
* **Why the magic resistance is an event and not an attribute.** (1) Vanilla armour does not reduce magic damage at all, so there is no vanilla attribute to
  lean on. (2) Tensura has its own magic-resistance attribute, but its scale and formula live in Tensura's code and cannot be verified here; a number put
  there could be a hundred times too strong or do nothing. (3) A ceiling over the SUM of several pieces (`maxResist`, 40) cannot be written as per-item
  modifiers, while a plain function over the stacks is exact, testable, the same for a player and a monster, and switched off with one number (0).
  The price: it only reduces damage that goes through `LivingIncomingDamageEvent` (everything that calls `hurt`); damage a skill applies by setting health
  directly is not touched.
* **Why "magic" is decided by the damage-type id.** The mod has three slightly different "is this magic" tests and no damage-type tag of its own, so the
  gear uses one pure, tested rule (`MagicDamage`): vanilla `magic`, `indirect_magic` and `dragon_breath`; and a Tensura type when a word of its path is an
  element or a spell word (`elemental`, `magic`, `holy`, `curse`, `megiddo`, `breath`, `ray`, `gravity`, `flame`, `lightning`) or it is `water_blade` or
  `heat_wave`; plus Tensura's own tag of magic fire. It is deliberately NARROW: `magicule_poison`, `fear`, `insanity`, `soul_scatter` (status, mental and
  spiritual damage, which Tensura answers with its own engravings) and the physical skills (`bullet`, `kunai`, `aura_slash`, `severer_blade`) are not
  magic here, and neither is `thorns`, which this mod uses as a neutral source for its own scripted hits (the Anti-Magic test counts it because it reuses
  vanilla's witch-resistance tag). An unknown id is not magic: a missing entry costs a little protection, a wrong one would shield the player from things a
  spell never was. The owner can add types with `gear.extraMagicTypes`.
* **Why the grade mix shifts with depth.** The brief wants lower-tier equipment dropping "across the floors" and better equipment meaning something. A
  fixed mix would make floor 40 drop exactly what floor 1 does. The three base chances are multiplied by a curve over the depth progress
  `x = floorShift * (threat - 1)` (0 at the Hub, 1 at floor 51): Normal falls to 0.40, Special peaks at x = 0.5 (about floor 26) at 1.95 and eases to
  1.40, Rare keeps rising to 6. With the default bases the three together stay near 6.9 % (between 6.5 and 7.1 %), so going deeper MOVES weight from Normal
  to Special and Rare instead of adding drops: the total stays at about one piece per fourteen or fifteen kills, only what drops changes.
* **Why Looting adds to the total and not to a grade.** Looting is "more drops", not "better drops": the bonus (`lootingBonus`, 0.01 per level) is added
  to the total chance and shared out in proportion, so Looting III turns 6.9 % into 9.9 % with the same mix. Otherwise a flat +1 % on Rare would make
  Looting III triple the Rare rate.
* **Why a Dungeon Master creature counts as hostile.** The Dungeon Master (`mega.enc`) tags every monster it spawns `nusmp_dm`, and most of them are
  Tensura monsters, which are tamable animals and not the vanilla `Enemy` interface. The 1.36.0 economy, the HP buffer and the 1.54.0 faction drops therefore ask
  `MegaDungeon.isDungeonMonster` as well as `Enemy`; the first version of this listener asked only `Enemy` and would have dropped gear for zombies and skeletons
  but for almost none of the dungeon's own population. It now asks both (`GearDrops.hostile`), and `gear.drop` proves it with a cow that is no `Enemy`: tagged
  `nusmp_dm` it pays at the table's rate, untagged it never does.
* **Why one piece per kill.** The economy already pays stones and meat on every kill; equipment on top of that must stay an event. One roll over the three
  chances end to end gives at most one piece, is trivially unbiased (the tests check 100000 rolls against the table), and a farm of weak monsters cannot
  shower the player with gear.
* **Why a sector Guardian guarantees a Rare piece.** The decennial boss is the milestone of the dungeon. A boss is not an ordinary kill and the economy
  excludes bosses on purpose, so it gets no random roll; but a guaranteed, bounded reward (one Rare piece, never a relic) makes the fight pay. Other bosses
  keep their own loot tables. The switch is `bossGuaranteed`.
* **Why the base chances are in the config and the shapes in the data file.** The three chances, the shift and the Looting bonus are the knobs of one
  server; the curves, caps, items, enchantments and slot weights are design data a datapack may replace. Keeping the numbers in one place each means the
  config can never be silently overridden by a stale JSON default (the defaults live in `GearRates.DEFAULT`, read by the config and the tests alike).
* **Why Normal armour is leather and Special is chainmail or iron.** The brief: Special has "better base material (iron/chainmail)", Rare "high-grade
  material (iron/diamond)". Normal is therefore the material below: wood, stone and leather. Bows, crossbows and shields have no material tiers, so a grade
  is made of the same base item and differs in bonus, durability, enchantments and engravings.
* **Why no engravings on armour.** Owner rule 6 is about weapons. Armour already carries the magic resistance component, and Tensura's own protection
  engravings would double-count it. Shields count as weapons for engraving (the Magebane Shield carries `magic_protection` too).
* **Why `magic_protection` is only in the Rare shield pool.** It "decreases damage input of magic attacks for each piece of gear equipped", which is the
  same job as the component; confining it to one slot keeps the stacking small. Magic Interference, Magicule Absorption, Sturdy and Barrier Piercing are
  the engravings "that fit magic resistance" the brief asked for.
* **Why the quest fires on pickup.** "Receives" means in the player's hands, not on the floor of a lava room. The command `gear give` fires it directly.
  `NusmpEvents.fireOnce` remembers the grade in the player's persistent data (copied on death) and is a no-op for fake players.
* **Why the table falls back to the copy inside the jar.** A death event must never meet an empty table by accident. If a pack's file is unreadable or
  gives nothing usable the shipped one is used, and if even that fails the table is empty and the log says so once: nothing drops, nothing crashes.

## 5. Classes (the WHY of each)

| Class | Pure? | Why it exists |
|---|---|---|
| `GearGrade` | pure | The three grades of the brief and no more. The four higher grades are relics; an enum that stops at three cannot leak one into the infinite stream. Saved by id. |
| `GearSlot` | pure | The nine kinds of piece and every fact that hangs on them (power stat, engravings, worn or held, armour position). One table for the roller, the factory, the resistance and the tests. |
| `GearInfo` | pure | The value of the component: grade, slot, resistance. Never fails to build (unknown ids become safe defaults), so a stack from a future version still loads. |
| `GearSpec` | pure | A drop as a decision: grade, slot, base item id, bonuses, enchantments, engravings, resistance. Seedable, countable, printable; the factory is the only thing that touches the game. |
| `GearRates` | pure | The five config numbers as one record, with the defaults; the roller takes it so tests need no config. |
| `GearTables` | pure | The immutable tables read from the JSON: curves, caps, slot weights, per-grade items, ranges, pools. A lookup of something absent answers with a harmless default. |
| `GearTablesParser` | pure | Turns the parsed JSON tree into `GearTables`, clamping numbers and reporting every complaint in plain English; never throws. |
| `GearRoller` | pure | The chance of each grade at a floor and the whole roll of a piece. Everything draws from a `Dice`, so a seed replays it. |
| `ResistMath` | pure | Which pieces count where, the sum held to the ceiling (hard cap 90), the percentage as a smaller hit. |
| `MagicDamage` | pure | The rule for "is this damage a spell" over the damage-type id, plus the owner's extra list. |
| `GearComponents` | | The one data component `nusmp:gear_grade` with its saved and network codecs. |
| `GearConfig` | | The server config, section `[gear]`. |
| `GearTablesReload` | | The `ResourceManagerReloadListener` that reads the JSON, with the jar fallback and a record of where the tables came from. |
| `GearFactory` | | A `GearSpec` as an `ItemStack`: component, durability, attribute bonus (starting from the existing modifiers), enchantments, engravings, look. |
| `GearLook` | | The coloured custom name and the lore, from the relic `Grade`'s colours and labels, all translatable with English fallbacks. |
| `GearDrops` | | The `LivingDropsEvent` listener and the decision `process(event, dice)`. |
| `GearEvents` | | The resistance on `LivingIncomingDamageEvent` and the quest hook on pickup. |
| `GearCommand` | | `gear give / roll / status`. |
| `GearSelfTests` | | Four of the in-game self-tests (`gear.tables`, `gear.roll`, `gear.drop`, `gear.resist`) and the shared `carriesRelicMark`. |
| `ChestRules` | pure | Which loot tables are chests that may hold a piece, which are treasure chests, which are never touched (the two relic tables, fixed in code). |
| `ChestGear` | pure | The chest decision: the chest base chances through the kill roller's own curves and caps, the treasure factor, Luck; at most one piece. |
| `ChestLootModifier` | | The NeoForge global loot modifier `nusmp:gear_chests` (a `LootModifier`): codec, registry entry, and the live decision that adds the stack. |
| `ChestSelfTests` | | The in-game self-test `gear.chest`. |
| `GearRegistry` | | `init` (the component, the self-tests) and `register` (the listeners); `command()`. |

## 6. The data file `data/nusmp/mega/gear/tables.json`

A data pack's file of the same path replaces it; `/reload` applies it. Every object carries a `why` string (JSON has no comments, and the owner's rule is a
WHY for every file); a `why` key is ignored everywhere. Unknown keys, unknown slots and grades, ids that are not `namespace:path`, numbers out of range and
reversed ranges are reported in the log at reload (`[nusmp] Gear tables file ...`) and fixed or skipped, never thrown.

```json
{ "curve":  { "normal": [[0.0, 1.0], [0.5, 0.64], [1.0, 0.40]], "special": [...], "rare": [...] },
  "caps":   { "normal": 0.40, "special": 0.30, "rare": 0.25, "total": 0.60 },
  "slotWeights": { "sword": 18, "axe": 8, "bow": 8, "crossbow": 5, "shield": 9, "helmet": 13, "chestplate": 13, "leggings": 13, "boots": 13 },
  "grades": { "rare": { "items": { "sword": ["minecraft:iron_sword", "minecraft:diamond_sword"], ... },
                        "power": [20, 35], "durability": [25, 50],
                        "enchants": { "count": [1, 2], "levelCap": 2, "pool": { "sword": [ {"id": "minecraft:sharpness", "min": 1, "max": 2}, ... ] } },
                        "engravings": { "count": [1, 2], "pool": { "sword": [ {"id": "tensura:magic_interference", "min": 1, "max": 1}, ... ] } },
                        "resist": { "armour": [5, 12], "hand": [4, 6] } } } }
```

* `curve.<grade>`: points `[x, multiplier]`, `x` in 0..1 (sorted for you), piecewise linear, flat before the first and after the last. The grade's chance at a
  floor is `base * curve(x)` with `x = floorShift * (threat - 1)` clamped to 0..1.
* `caps.<grade>`: the most one grade's chance per kill or chest can be; `caps.total`: the most the three together can be after the shift and Looting (the three are scaled down together).
* `slotWeights`: relative weight of each slot (they add up to 100 so each reads as a percent). A slot a grade has no base item for is never rolled for it.
* `grades.<grade>.items.<slot>`: the base item ids, uniform (an id listed twice is twice as likely); vanilla ids only, resolved when a piece is made, an absent one makes no drop.
* `power`: the percentage bonus to attack damage (sword, axe) or armour (the four armour pieces); `durability`: the bonus to the maximum durability.
* `enchants.count` / `levelCap` / `pool`: how many distinct entries are drawn from the slot's pool and the highest level any may take; an entry takes a level in its own `min..max` held to the cap.
* `engravings`: the same for Tensura engravings (enchantments of the `#tensura:engraving` tag); only weapons and shields use them; each is resolved at runtime and capped at the engraving's own maximum level; an absent one is skipped.
* `resist.armour` / `resist.hand`: the magic resistance of one piece, in percent.

### 6.1 The base items (all vanilla)

| Slot | Normal | Special | Rare |
|---|---|---|---|
| sword | wooden, stone | iron | iron, diamond |
| axe | wooden, stone | iron | iron, diamond |
| bow, crossbow, shield | the plain item | the plain item | the plain item |
| helmet, chestplate, leggings, boots | leather | chainmail, iron | iron, diamond |

### 6.2 What each grade adds (the brief's numbers, then mine)

| | Normal | Special | Rare |
|---|---|---|---|
| Attack damage / armour bonus | none | +10 to 20 % (brief) | +20 to 35 % (brief) |
| Durability bonus | none | +10 to 25 % (mine) | +25 to 50 % (mine) |
| Vanilla enchantments | none | 0 or 1, level 1 (brief: "at most one minor") | 1 or 2, level up to 2 (brief) |
| Tensura engravings, weapon or shield | none | 0 or 1, light (brief) | 1 or 2 that fit magic resistance (brief) |
| Magic resistance per piece | none | none | armour 5 to 12 %, weapon or shield 4 to 6 % (brief) |

The vanilla enchantment pools (same for Special and Rare, held to level 1 or 2 by the grade): sword sharpness, unbreaking, knockback, sweeping edge; axe
sharpness, efficiency, unbreaking; bow power, punch, unbreaking; crossbow quick charge, piercing, unbreaking; shield unbreaking; helmet protection,
respiration, aqua affinity, unbreaking; chestplate protection, thorns, unbreaking; leggings protection, swift sneak, unbreaking; boots protection, feather
falling, unbreaking. No pool holds two enchantments that exclude each other in vanilla (Protection and Fire Protection, Sharpness and Smite), so no piece is
"illegal", and a test enforces it.

### 6.3 The Tensura engravings (owner rule 6)

Ids from the owner's Tensura reference (34 enchantments), every one checked against it by `GearDataTest`:

| | Special (0 or 1) | Rare (1 or 2) |
|---|---|---|
| sword | sturdy, swift | magic_interference, magicule_absorption, sturdy (1-2), barrier_piercing |
| axe | sturdy, crushing | magic_interference, sturdy (1-2), crushing, barrier_piercing |
| bow | sturdy | magic_interference, magicule_absorption, sturdy |
| crossbow | sturdy | magic_interference, barrier_piercing, sturdy |
| shield | sturdy | magic_interference, magicule_absorption (1-2), magic_protection, sturdy (1-2) |

No curse (`enervation`, `lethargy`, `ruination`, `sealing`, `stagnation`) is ever offered; a test insists. `item/WeaponEngravings` cannot hold these (it only
engraves `nusmp:` items), so this table is where owner rule 6 is met for the vanilla bases. The engravings are applied once, when the piece is made.

## 7. The numbers (first-pass: nobody has played them)

**Chance per kill by floor** (default config, no Looting; from `GearRoller` with the shipped file; "+-" because the Abyssal road is as deep as the Holy one):

| Floor | Progress x | Normal | Special | Rare | Total | One piece per | Normal share | Special share | Rare share | Total with Looting III |
|---|---|---|---|---|---|---|---|---|---|---|
| +-1 | 0.00 | 5.00% | 1.50% | 0.40% | 6.90% | 14.5 kills | 72% | 22% | 6% | 9.90% |
| +-5 | 0.08 | 4.71% | 1.73% | 0.49% | 6.93% | 14.4 kills | 68% | 25% | 7% | 9.93% |
| +-10 | 0.18 | 4.35% | 2.01% | 0.60% | 6.97% | 14.4 kills | 62% | 29% | 9% | 9.97% |
| +-11 | 0.20 | 4.28% | 2.07% | 0.62% | 6.97% | 14.3 kills | 61% | 30% | 9% | 9.97% |
| +-20 | 0.38 | 3.63% | 2.58% | 0.83% | 7.04% | 14.2 kills | 52% | 37% | 12% | 10.04% |
| +-26 | 0.50 | 3.20% | 2.93% | 0.96% | 7.09% | 14.1 kills | 45% | 41% | 14% | 10.08% |
| +-30 | 0.58 | 3.01% | 2.79% | 1.19% | 6.99% | 14.3 kills | 43% | 40% | 17% | 9.99% |
| +-40 | 0.78 | 2.53% | 2.46% | 1.77% | 6.76% | 14.8 kills | 37% | 36% | 26% | 9.76% |
| +-51 and deeper | 1.00 | 2.00% | 2.10% | 2.40% | 6.50% | 15.4 kills | 31% | 32% | 37% | 9.50% |

**Why these numbers.** The bases 0.05 / 0.015 / 0.004, the shift 0.02 and the Looting 0.01 are the brief's. The curve endpoints are mine, chosen so the total
stays within 6 % of the floor-1 total (a test enforces 10 %), Normal ends at 40 %, Special peaks near x = 0.5 and Rare reaches six times its base: at the
bottom of the dungeon a third of the drops are Rare, which is the point of going there. The caps (0.40 / 0.30 / 0.25 and 0.60 in all) are above anything the
defaults produce, chests included (a test, `noCeilingBitesUnderTheShippedDefaults`, pins that); they exist so that a mistyped config cannot turn every kill or chest into a
shower of gear. (They were 0.30 / 0.20 / 0.15 / 0.50 until the chest odds were drawn: a deep treasure chest hit the old Special and Rare ceilings and the curve went flat.) The slot weights are 13 per armour piece (so armour is half
of the drops, the thing a player needs four of), 18 sword, 8 axe, 8 bow, 5 crossbow, 9 shield.

**Magic resistance.** Armour 5 to 12 % per piece (average 8.5), weapon and shield 4 to 6 % (average 5). The pieces worn and held are summed and held to
`maxResist` (40 by default, 90 at most). A full set of four Rare armour pieces plus a Rare weapon and shield averages 44 and at best reaches 60, so the
ceiling of 40 is reached by a full average set and not exceeded by anyone: a hundred points of spell damage always do at least sixty.

| Worn and held | Typical sum | Spell damage 100 becomes |
|---|---|---|
| nothing | 0 | 100 |
| one Rare chestplate | 8 | 92 |
| a full Rare set, no weapon | 34 | 66 |
| a full Rare set, sword and shield | 44, held to 40 | 60 |

## 8. Config (`nusmp-labyrinth-mega-gear-server.toml`, section `[gear]`)

| Key | Default | Range | Meaning |
|---|---|---|---|
| `enabled` | true | | The gear drops from monsters AND chests (and the quest hook). The resistance of gear already held is switched off with `maxResist = 0`. |
| `normalChance` | 0.05 | 0..1 | Chance per kill of a Normal piece at floor 1, before Looting. |
| `specialChance` | 0.015 | 0..1 | The same for Special. |
| `rareChance` | 0.004 | 0..1 | The same for Rare. |
| `floorShift` | 0.02 | 0..0.2 | Progress along the data curves per floor above the first; 0.02 reaches the end at floor 51; 0 = the same mix on every floor. |
| `lootingBonus` | 0.01 | 0..0.25 | Absolute chance added to the total per Looting level, shared in proportion. |
| `bossGuaranteed` | true | | The sector Guardian drops one guaranteed Rare piece. |
| `maxResist` | 40 | 0..90 | Ceiling of the summed magic resistance, in percent. 0 = no resistance. |
| `engravings` | true | | Special and Rare weapons and shields carry Tensura engravings. |
| `extraMagicTypes` | "" | | Extra damage types that count as magic: ids or `namespace:*`, separated by commas. |
| `chestNormal` | 0.22 | 0..1 | Chance that a standard chest of the Mega-Dungeon holds ONE Normal piece at floor 1. |
| `chestSpecial` | 0.06 | 0..1 | The same for Special. |
| `chestRare` | 0.02 | 0..1 | The same for Rare. 0 for all three = no gear in chests. |
| `chestTreasureFactor` | 2.0 | 1..10 | A treasure chest multiplies its Special and Rare chances by this (Normal unchanged). |

`extraMagicTypes` is an addition to the brief's list (so is the explanation that `enabled` and `maxResist` are the two separate off switches): the rule for
"magic" is a guess at Tensura's ids that nobody can check without the game, and this is the owner's escape hatch. All values are plain `define` /
`defineInRange` and are read through `GearConfig.get`, which answers the default while the spec is not loaded.

## 9. Commands (the lead attaches `GearRegistry.command()` under `/multiverse mega`, permission 2)

* `gear give <player> <normal|special|rare> [slot]`: makes a piece with the real factory (a random slot unless one is given) and gives it; the quest key fires.
* `gear roll [count]`: runs the real roller `count` times (default 1000, at most 200000) at YOUR floor with the config's rates and prints the pieces, the
  histogram by grade and by slot and the average resistance. This is how the table is tuned. The floor is 1 for the console or outside a sector.
* `gear roll chest [count]` and `gear roll treasure [count]` (1.55.0): the same histogram for `count` standard or treasure chests at YOUR floor, with the chest
  chances of the config, through the modifier's own decision (`ChestGear.decide`), no Luck.
* `gear status`: the switches, the depth progress at your floor, and for each grade its configured base, its chance here, its chance with Looting III and its cap;
  and (1.55.0) a line with the standard and the treasure chest chances here.

Text goes through `message.nusmp.mega.mgr.cmd.*` with English fallbacks in the code (the fragment `mgr.lang.json` carries the same texts).

## 10. Tests

### 10.1 JUnit (`src/test/java/com/newuniverse/nusmp/labyrinth/mega/gear`)
`GearRollerTest` (20), `GearTablesParserTest` (15), `GearDataTest` (10), `ResistMathTest` (4), `MagicDamageTest` (5), `GearInfoTest` (6),
`ChestRulesTest` (6), `ChestGearTest` (12), `GearChestDataTest` (4, the shipped JSON files against the Java sources),
`GearIsNotARelicTest` (4), with helpers `GearFx` and `MiniJson` (a copy of the faction tests' tiny reader: no JSON library is on the test class path) and
resources `mega/gear/vanilla_items.txt` (the 1333 constants of the API dump's `Items` class), `vanilla_enchantments.txt` (the 42 enchantments of the 1.21.1
data pack; **from memory, the dump has no enchantment class**; the in-game `gear.tables` proves them), `tensura_enchantments.txt` (the owner's 34) and
`tensura_damage_types.txt` (the constants of the API dump's `TensuraDamageTypes`, 71). They check, among others: the chances at floor 1 are the config
defaults; Normal never rises and Rare never falls from floor 1 to 80; Special peaks between floors 15 and 40; the total drifts less than 10 %; Looting adds
exactly its bonus and keeps the mix; the caps hold under an absurd config; the grade counts of 100000 rolls at floors 1, 26 and 60 are within five standard
deviations of the table (the random stream is a fixed seed, so a pass is a pass for ever); a seed replays the same pieces; every piece is made only of what
the table offers; the slot frequencies follow the weights; every base item is a real vanilla item of the kind of its slot with the material the brief gives
its grade; every vanilla enchantment is real, fits its slot and never excludes another in its pool; every engraving is a real Tensura one, never a curse,
never on armour, and Rare weapons can all roll Magic Interference; every one of Tensura's 71 damage types is classified as spell or not ON PURPOSE (a type
added by an update fails the test until someone decides); the gear package imports nothing of the relic system except the grade colours.

**Mutation checks** (each applied, the tests run, the file restored): the curve ignored, Looting dropped, Special unpickable, the level cap removed, the slot
weights ignored, `reduce` using the percent instead of its complement, armour counting everywhere, the 90 hard cap removed, `thorns` counted as magic, the
word `elemental` lost, substring matching instead of whole words, the wildcard matching every namespace, the range clamp lost, an id of any shape accepted, the
curve unsorted, a missing grade reported silently, a typo'd base item, iron in the Normal grade, a protection-family enchantment on a chestplate, resistance
on Special, Normal rising with depth, the factory importing the ledger, a relic serial stamped on a drop, a second component registered. Every one made at
least one test fail. The relaunched builder repeated fourteen of them on the code as it stands (curve ignored, Looting dropped, the total cap lost, `reduce`
inverted, armour counted in any worn slot, the 90 hard cap lost, the word `elemental` lost, `thorns` counted as magic, an id of any shape accepted, the range swap
lost, the slot pick always the last, the resistance clamp of `GearInfo` lost, the level cap lost, engravings allowed on armour) and TWO SURVIVED: the final level
clamp in `draw` (a pool entry whose own minimum is above the cap) and the armour rule when a pack lists an armour engraving pool (the shipped file has none). Two
tests were added for them (`anEntryWhoseMinimumIsAboveTheCapIsStillHeldToTheCap`, `armourNeverCarriesEngravingsEvenIfAPackListsAPool`) and both mutants now die.

The chest classes (section 14) are pinned the same way: `ChestRulesTest` (a table with a `chests/` segment is a chest, a treasure word makes it treasure, the two relic tables are never touched even
with an empty data file or in another case, mobs', blocks' and unknown tables are nothing, the data file's additions are normalised), `ChestGearTest` (the floor-1 chances are the config, treasure
multiplies only Special and Rare, the depth shift, Luck adds to the total and keeps the mix, the ceilings hold under an absurd config and do NOT bite under the defaults, a chest is more generous than a
kill, one decision and one piece, a hundred thousand seeded chests per kind and floor within five standard deviations, a seed replays) and `GearChestDataTest` (the shipped list file keeps
`nusmp:relic_chests` and names `nusmp:gear_chests` once with `replace` false; the modifier JSON's type equals the id the Java registers and its condition names the dimension that
`Labyrinth.MEGA_DIMENSION` is built from, not the Sanctuary; every chest table of this mod is classified on purpose). Ten mutants of the chest code (treasure multiplying Normal too,
`decide` and `chances` ignoring a non-chest table, the Luck ceiling and the Luck clamp lost, the relic tables not fixed in code, the `chests/` rule loosened to `chest`, the path's case
mattering, the treasure test running before the exclusion, any string accepted as an excluded id) were applied: every one failed at least one test. The ceilings test was mutated by putting
the old ceilings back: it fails at the deep treasure chest, as intended.

### 10.2 In-game self-tests (`GearSelfTests`, run by `/multiverse mega selftest`)

| Key | What it drives and asserts |
|---|---|
| `gear.tables` | The tables in force were read by a data reload (not the jar fallback: this catches an unregistered listener), parse with no complaint, every base item and every vanilla enchantment resolves in this game, and at least some Tensura engravings exist (the detail lists the absent ones). |
| `gear.roll` | 300 pieces of all three grades through the real factory. Each has the component with the right grade, a name in the colour of the relic grade, a lore line `Standard Gear`, NO relic component (`relic_serial`, `relic_attuned`, `relic_uses`, `relic_state`) and no relic definition; Rare armour 5 to 12 % with its resistance line, only Rare resists; a durability bonus raises the maximum; the sword, axe or armour keeps its OWN attack damage or armour next to the new bonus, with exactly one modifier of the slot's fixed id; every enchantment and every existing engraving is on the stack. Then 30000 rolls at floor 40 must produce every grade. |
| `gear.drop` | 8000 zombie kills by a fake player in sector 0 (or anywhere in the Mega level, counted as floor 1, when no sector has been generated yet: the test needs none) posted to `GearDrops.process` as real `LivingDropsEvent`s: never more than one piece per kill, always a graded stack, the count and each grade within six standard deviations of the table; 300 deaths with no player drop nothing; a zombie tagged as a boss, a wanderer or a servant drops nothing; tagged `nusmp_sector_guardian` it drops exactly one Rare piece every time; then 6000 kills of a cow (no `Enemy`) tagged `nusmp_dm` pay at the table's rate and 400 of the untagged cow pay nothing. |
| `gear.chest` | See section 14.6: made-up chest tables rolled through the whole loot pipeline in the Mega-Dungeon (count and grades within six standard deviations, one piece at most, the table's own bread kept, never a relic), nothing in the Overworld or the Hub, and the modifier called alone for the code's own gate. |
| `gear.resist` | A fake player wears and holds Rare pieces: the sum is armour worn plus weapon and shield held (a chestplate carried in the hand and boots worn on the head do not count), held to the ceiling; magic and indirect magic are cut by exactly the share, fall and generic damage are not; four Tensura spells (fire elemental, light elemental, holy, magic generic) are cut and four other Tensura types (bullet, suffocate, magicule poison, severer blade) are not, by their real registry keys. The equipment is restored. |

## 11. Hooks for the lead

* **Nothing to register** for the component, the listeners, the reload listener and the self-tests: `MegaSystem` already calls `GearRegistry.init` and `register`.
* **Config**: `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.mega.gear.GearConfig.SPEC, "nusmp-labyrinth-mega-gear-server.toml");`
* **Command**: attach `com.newuniverse.nusmp.labyrinth.mega.gear.GearRegistry.command()` under `/multiverse mega`.
* **Lang**: merge `mgr.lang.json` (28 keys, `message.nusmp.mega.mgr.*`) into `en_us.json`; the three keys added for the chest output are `cmd.roll.chest.head`, `cmd.roll.chest.grade`, `cmd.status.chest`.
* **Loot modifier list (1.55.0, chests)**: `data/neoforge/loot_modifiers/global_loot_modifiers.json` already existed (entry `nusmp:relic_chests`); this module appends `nusmp:gear_chests` to
  its `entries` (the exact diff is in section 14.4). The type is registered by `GearRegistry.init`, nothing else to wire.
* **GUARD**: the Labyrinth Guardian must carry the scoreboard tag `nusmp_sector_guardian` (`entity.addTag(...)`); the flow module reads the same tag for the Metal Vessel.
* **No creative-tab entry**: there is no new item. `gear give` is the way to get a piece.

## 12. What is NOT verified (honest list)

1. **Nothing ran in game.** Every rate, curve and range is a first estimate; use `gear roll` and `gear status` to look at them.
2. **The Tensura damage-type ids are the constant names of `TensuraDamageTypes` in lower case.** One (`tensura:holy_damage`) is confirmed by existing code; the
   rest are not. `gear.resist` checks four spells and four non-spells by their real keys and will FAIL, naming the type, if the rule is wrong; the fix is
   `gear.extraMagicTypes` or a word in `MagicDamage`.
3. **The Tensura engravings on vanilla bases.** Their ids are from the owner's reference (the 34 ids of `tensura-ids-3-attributes-effects-enchantments.md`, diffed
   against `tensura_enchantments.txt`: identical); their maximum levels are not known, but every level the table asks for (Sturdy 2, Magicule Absorption 2, the
   rest 1) is at or below a level `item/WeaponEngravings` / `ArmorEngravings` already request for the same id (Sturdy 3, Magicule Absorption 2, Magic Interference 2,
   Magic Protection 3, Crushing 3, Barrier Piercing 3, Swift 2), and the factory caps each at the engraving's own maximum anyway; whether each takes effect on a vanilla sword or armour piece as it does on the mod's own weapons is untried. `tensura:magic_protection`
   on a Rare shield may stack with the component's own resistance.
4. **`vanilla_enchantments.txt` is from memory of the 1.21.1 data pack**; the API dump has no enchantment class. The sixteen vanilla enchantments the table
   USES (sharpness, unbreaking, knockback, sweeping_edge, efficiency, power, punch, quick_charge, piercing, protection, respiration, aqua_affinity, thorns,
   swift_sneak, feather_falling, plus looting for the bonus) were checked online against `github.com/misode/mcmeta` branch `1.21.1-data`
   (`data/minecraft/enchantment/<id>.json`: every one exists, none of the pools holds two that exclude each other, the supported-item tags fit the slots; the
   tooltip order of the preview is the tag `tags/enchantment/tooltip_order.json` of the same branch). `gear.tables` resolves every pool entry in the real registry.
5. **The attribute-modifier handling** (`ItemAttributeModifiers.withModifierAdded`, the fallback to the item's default list) is checked against the rebuilt stubs'
   signatures and by `gear.roll` in game, not by hand.
6. **`GearEvents.onIncomingDamage` is a five-line wrapper around `reduced`** and is not itself driven by a test (building a `LivingIncomingDamageEvent` needs
   `DamageContainer`, which neither the dump nor the stubs have). `reduced` is tested.
7. **The type check flags `new ItemEntity(...)`** as "abstract": a stub artifact, the same line is in `FactionDrops` and `EconomyDrops`.
8. **Balance.** A Rare diamond sword at +35 % deals 9.45, more than a vanilla netherite sword (8): the brief's ranges, applied to the item's own damage. A full Rare
   set with sword and shield reaches the 40 % ceiling. Whether that is too generous for floors with 2.5 times the health is for the owner to say after play.
9. **Pickup quest.** `ItemEntityPickupEvent.Post.getOriginalStack()` is relied on to keep the components; the dump lists the method.
10. **The `nusmp_sector_guardian` tag does not exist in THIS worktree** (the guard module is another builder's), but the GUARD builder's worktree sets it
    (`LabyrinthGuardianEntity.SECTOR_TAG = "nusmp_sector_guardian"`, `addTag(SECTOR_TAG)` in the constructor, re-applied on load; its doc
    `docs/labyrinth/mega_guard.md` names it as the key the Metal Vessel and the gear drops hang on), so the match is by reading their code, not by running it. Without
    it the Guardian is just a boss and drops no gear (it is excluded by the boss tests). `gear.drop` tags a zombie itself to prove the branch.
11. Monsters that pick up and wear a Rare piece get its resistance too (the event reads any creature's equipment). That is intended, and untried.
12. **Normal weapons carry no Tensura engraving.** The brief says "Normal is plain" and the Tensura scale describes Normal as gear without any magical property,
    while the owner's rule 6 says every weapon gets engravings (written for new or reworked weapons of the mod). This module follows the brief: Normal is exactly
    the vanilla item with a grade. If the owner wants a light engraving even there it is a data-only change: in `tables.json`, `grades.normal.engravings`, set
    `count` to `[0, 1]` and add a `pool` per weapon slot (for example `tensura:sturdy`).
13. **Rare armour carries no `tensura:magic_protection`.** The mod's own robes do (`item/ArmorEngravings`), but Tensura's engraving "decreases damage input of magic
    attacks for each piece of gear equipped" by a formula nobody can check here, and it would stack on top of the component's own, capped, resistance. The code
    forbids engravings on armour on purpose (a test pins it), so this needs a code change in `GearSlot.engraves`, not a data change, if the owner wants it.
14. **The picture's tooltips are a drawing**, not a screenshot: the font is not the game's, and the maximum level of each Tensura engraving is shown as rolled
    (the game caps it at the engraving's own maximum, which is not known here).
15. **The Rukh flow's rejection drain is magic damage too.** The FLOW module hurts a player who casts opposing magic with `damageSources().magic()`
    (`flow/Rejection.java` in the FLOW builder's worktree), and `magic` is exactly what Rare gear resists, so a player in a full Rare set takes up to 40 % less of
    that drain. It is consistent ("magic resistance") but it softens the punishment the owner asked for; if the drain must stay absolute, FLOW should hurt with a
    damage type outside the rule (`generic`, or `thorns`, which `MagicDamage` deliberately does not count), or `gear.maxResist` can be lowered. Nothing in this module
    needs to change.
16. **The chest side never ran in game** (section 14): the codec, the registry entry, the JSON condition, the list entry and the loot-table wiring are checked by pure tests
    against the Java sources, by the type check, and by NeoForge's own source and docs read online, but only `gear.chest` in a booted game proves the chain.
17. **The `location_check` condition's JSON shape** (`{"condition": "minecraft:location_check", "predicate": {"dimension": "nusmp:mega_dungeon"}}`) is the vanilla
    condition as the NeoForge 1.21.1 loot-condition docs show it (found through a web search; the docs host itself could not be fetched here) and the vanilla
    `predicate.location.dimension` key is confirmed in the 1.21.1 data pack (`github.com/misode/mcmeta` `1.21.1-data`, an advancement). A wrong condition would make the whole modifier fail to
    load (a logged error), which `gear.chest` reports as "nothing at all".
18. **Lootr and other containers** are expected to work (they roll the table through `LootTable`, which is where NeoForge applies the global modifiers) but were not tried.
19. **No treasure table exists in the Mega-Dungeon yet.** Every chest its templates place uses `nusmp:chests/labyrinth_trial` (a standard chest); the two relic tables are excluded; the
    treasure class is for tables a later builder or another mod adds (a boss room, a vault that is not a relic vault).
20. **The owner's fourth brief calls the standard items "Farmable Standard Relics"** ("Relics of Many Worlds", "not bound by world limits"). This module keeps them ordinary vanilla items
    with a grade (they were never under a limit), which meets "drop infinitely". If the owner means that the mod's own 236 low-grade RELICS (Normal, Special and Rare, under the Order's
    ledger) should also lose their one-per-world limit, that is a separate change in the relic system and is not made here.

## 13. Pictures and the preview tool

`python3 tools/labyrinth/mega_gear_preview.py [--out DIR] [--verify dump.json]` (Pillow) draws three PNGs from `tables.json` and the config defaults:
`gear_grade_tables.png` (what each grade is made of, the base item of every slot, the engravings by slot, how the magic resistance adds up),
`gear_drop_chances.png` (chance per kill and share of the drops by floor, with every number in a table underneath; the colours are the game's grade colours, so
stripes and dots are the second encoding) and `gear_tooltips.png` (example Normal, Special and Rare pieces drawn like the game's tooltips: enchantments, the lore
block, the attribute lines with the grade's flat bonus under the base stat). The script is also an INDEPENDENT second implementation of the roller and of the
factory's arithmetic (written from the Java; it carries `java.util.Random`'s generator so it replays the Java seeds): `--verify` takes a JSON dump written by the
Java roller (chances at eleven floors and 162 seeded pieces, one per grade, slot and six seeds; written by `tools/labyrinth/mega_gear_dump/PreviewDump.java`, whose
comment holds the three commands) and fails on any difference. It was run: 11 chance rows and
162 pieces, 0 differences, which is a second check that the chances in section 7 and the pieces the roller makes are what the code says. Vanilla numbers in the
script (attack damage, durability) are from `github.com/misode/mcmeta` `1.21.1-summary/item_components`.

## 14. Chests: the global loot modifier `nusmp:gear_chests` (1.55.0, owner's fourth brief)

The fourth brief says the standard Normal, Special and Rare items "can drop infinitely from mobs and standard chests" and asks for "the `LivingDropsEvent` and Global Loot Modifier JSONs for
the dungeon". Mobs are `GearDrops` (section 3). This is the chest side, everything NEW; nothing of the kill side was changed except that the safety ceilings of `tables.json` were raised (14.5).

### 14.1 Why a global loot modifier
A NeoForge global loot modifier works on loot that has ALREADY been rolled, whoever rolled it: a plain chest, a Lootr container (one roll per opening player), a chest minecart, a mod's
container, a table another builder or a pack adds later. No template, no chest block and no loot-table JSON is edited, and nothing the table rolled is replaced: the modifier ADDS one stack beside it
(add the new, keep the old). The alternative, adding a pool to every chest table, would have to touch each table and miss every table that does not exist yet. NeoForge applies the modifiers in
`LootTable.getRandomItems(LootContext)` after the table's own pools, once per top-level roll (a table pulled in by another table goes through the raw method and is not modified twice), and records
the id of the table being rolled in the context (`getQueriedLootTableId`; `neoforge:unknown_loot_table` when it does not know), which is how the modifier knows what a chest is.

### 14.2 Which tables (`ChestRules`, pure, tested)
* A table counts only if its path has a `chests/` segment (vanilla's and this mod's convention): a mob's, a block's, a fishing or an archaeology table never does; the unknown id never does.
* **TREASURE**: a chest table whose path holds one of `treasureWords` (shipped: treasure, vault, boss, guardian, crucible, antechamber, reward). It rolls the better grades more often (14.3).
* **Never touched**: `nusmp:chests/dungeon_relic_vault` and `nusmp:chests/labyrinth_pedestal`, the two tables the relic system and the Crucible's offer are built on. They are fixed IN THE CODE
  (`ChestRules.ALWAYS_EXCLUDED`): a data pack can add exclusions through the `chests` section of `tables.json` but can never remove these two. WHY: the pedestal holds exactly the relic it was
  made for (a second stack in a one-item pedestal would break it), and the owner's rule is that the Crucible is the only source of the Unique-and-higher relics. The modifier never adds a relic
  of any kind: the stack carries only `nusmp:gear_grade` and `gear.chest` asserts it.
* Only inside the Mega-Dungeon dimension (`MegaDungeon.in`): the Overworld, the Spire and the Sanctuary Hub (a separate dimension, deliberately not a Labyrinth) get nothing. TWO gates: the JSON's
  `location_check` condition on the dimension id, and the same check in the code, so a data pack that edits the conditions still cannot make gear appear elsewhere.

### 14.3 The numbers (first-pass: nobody has played them)
One roll per chest over the three chances laid end to end, so AT MOST ONE piece per chest, through the SAME roller as the kills (`GearRoller.roll`), so the depth curves, the slot weights, the items,
the enchantments, the engravings and the resistance are the kills' and nothing is duplicated.

| | Normal | Special | Rare | Total (floor 1) | One piece per |
|---|---|---|---|---|---|
| a kill (reference) | 5 % | 1.5 % | 0.4 % | 6.9 % | 14.5 kills |
| a standard chest, floor 1 | 22 % | 6 % | 2 % | 30 % | 3.3 chests |
| a treasure chest, floor 1 | 22 % | 12 % | 4 % | 38 % | 2.6 chests |
| a standard chest, floor 51 and deeper | 8.8 % | 8.4 % | 12 % | 29.2 % | 3.4 chests |
| a treasure chest, floor 51 and deeper | 8.8 % | 16.8 % | 24 % | 49.6 % | 2.0 chests |

(The picture `gear_drop_chances.png` has every floor.) WHY these numbers:
* **22 / 6 / 2, about four to five times a kill.** A chest is a deliberate reward found a handful of times a floor, a kill is one of a hundred; three pieces in ten chests make the chests the
  dependable source of survival gear (a Rare one in fifty chests at floor 1, rising to one in eight at depth) while the kills stay the volume. The three are in the SAME proportion as the kills'
  (73 / 20 / 7 percent of the pieces), because the depth curves of `tables.json` were shaped for that mix: with it the chance of a piece stays within 3 % of 30 % at every depth, so going
  deeper changes WHAT a chest holds and not how often. The first try, 20 / 8 / 2, drifted by 11 % mid-way (a test caught it).
* **Treasure multiplies Special and Rare only (default 2).** A treasure chest should hold something BETTER, not simply more of everything; Normal stays 22 %. At depth it reaches one piece in two
  chests, a quarter of them Rare: generous on purpose, it is the reward for the rooms that name a vault or a boss. `chestTreasureFactor` 1 turns the difference off.
* **The floor shift and the Looting bonus are the kills' keys** (`floorShift`, `lootingBonus`): one knob each, so a server that flattens the depth curve does it for both. The bonus counts per
  point of the opener's LUCK (the chest's `getLuck()`, from the Luck attribute and the potion, rounded, at most 10): a lucky player finds more pieces in the same mix, as Looting does for a kill.
* **The floor is the chest's own** (`ORIGIN` of the loot context, `MegaDungeon.floorAt`); 0 (a gap between sectors, no origin) counts as floor 1.
* **Quest.** When the opener is a player the key `mega.gear` fires for the grade (first time per grade only; fake players are ignored by the quest code), so a piece received from a chest counts
  even though taking it from a container fires no pickup event.

### 14.4 The data files and the diff
* `data/nusmp/loot_modifiers/gear_chests.json`, WHY each line: `type` is the id `ChestLootModifier` registers (a test holds the two together); `conditions` is required by NeoForge's codec (an empty
  list would be legal) and carries one vanilla `minecraft:location_check` whose predicate names the dimension `nusmp:mega_dungeon` (a test reads `Labyrinth.MEGA_DIMENSION` and compares); no
  `neoforge:conditions` because nothing here depends on another mod.
* `data/nusmp/mega/gear/tables.json`, new section `chests` (`excluded`, `treasureWords`, each documented by a `why`), parsed by `GearTablesParser`; a file without it gets the defaults, so an old data
  pack behaves the same.
* `data/neoforge/loot_modifiers/global_loot_modifiers.json` ALREADY EXISTED (the relic modifier's entry), so it was merged into, not replaced:

```diff
 {
   "replace": false,
   "entries": [
-    "nusmp:relic_chests"
+    "nusmp:relic_chests",
+    "nusmp:gear_chests"
   ]
 }
```

### 14.5 The ceilings were raised
`caps` of `tables.json` (the most a grade, and all three together, can turn up in one kill or chest) were 0.30 / 0.20 / 0.15 / 0.50 and are now 0.40 / 0.30 / 0.25 / 0.60. They are a safety net for a
mistyped config and no kill rate ever came near them, but the deepest treasure chest (Special 0.234, Rare 0.24) hit the old ones and the picture showed a flat-topped curve. A test,
`noCeilingBitesUnderTheShippedDefaults`, now pins that under the shipped numbers (kills and chests, standard and treasure, floors 1 to 80) no ceiling changes any chance. Nothing a player could
have noticed changes: every kill chance is the same.

### 14.6 The self-test `gear.chest`
It builds a MADE-UP chest table (one bread per roll, an id of its own: `nusmp:chests/gear_selftest`, and `..._treasure`) and rolls it through the game's WHOLE loot pipeline, so the type is found by
its JSON, the list entry and the dimension condition are evaluated for real. In the Mega-Dungeon the number of pieces and of each grade must match the chest chances within six standard deviations,
never more than one per chest, the bread is never replaced, never a relic; the treasure table must match the treasure chances; the same rolls in the Overworld and (where it exists) the Hub must give
nothing. Then the modifier is called ALONE with no JSON conditions: nothing in the Overworld, nothing for the two relic tables, a mob's table, a block's table, a non-chest table or the unknown id in the
Mega-Dungeon, and with Luck 5 the Luck chances. Last, a chest opened by a fake player must not fail. A real vault table is deliberately never rolled through the whole pipeline: other loot modifiers
act on it and may touch the Order's ledger, and a self-test must not burn relics. A failure with "nothing at all" names the likely causes: the type is not registered, the entry is not in the list file,
or the condition is not met.

### 14.7 Verified online (read-only, nothing from the downloads is run)
* NeoForge `1.21.1` sources on `raw.githubusercontent.com/neoforged/NeoForge`: `LootModifier` (the codec's required `conditions` field through `codecStart`, `apply` final with `doApply`),
  `CommonHooks.modifyLoot` (it calls every modifier and first records the queried table id), the patches to `LootContext` (`getQueriedLootTableId`, `Builder.withQueriedLootTableId`) and `LootTable`
  (`setLootTableId`, the modifiers applied in the private `getRandomItems(LootContext)` after the raw roll). The rebuilt API stubs of the type check carry the same methods.
* The vanilla `location.dimension` key and the `location_check` shape: see honest-list item 17.
* The repo's own precedent: the compat resource pack's `neoforge:add_table` modifiers use the same `conditions` array, and `relics/RelicLootModifier` is the other global modifier of the mod
  (a different base, `IGlobalLootModifier`, because it has no conditions).
