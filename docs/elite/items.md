# elite/items: weapons, armour, consumables, crates and plushies

Source: the "Items, Equipment & Blocks" part of the owner's EliteTensuraAddon inventory (docs/elite/plan.md). The original addon's source is **not available**, so only the NAMES are the
original's; every number, ability, recipe, drop table, texture and model below is designed here. Package `com.newuniverse.nusmp.elite.items` (client part `client/elite/items`), config
`nusmp-elite-items-server.toml`, lang fragment `tools/elite/items_lang.json` (en_us.json is not edited by this job), art generators in `tools/elite/`.

## Comparison with what the mod already has (CLAUDE.md rule 5)
Searched `src/main`, `tools`, `docs` for: crate, plush, catalyst, health_potion, nightingale, astral, void_edge, fenrir, walpurgis, runic, chronicle.

| Item (ids are `nusmp:<id>`) | Status | What exists / why it is new |
|---|---|---|
| Astral Edge `astral_edge` | **NEW** | no sword by that name. ("astral" is only a dungeon theme and a boss-theme tag.) |
| Void Edge `void_edge` | **NEW** | no sword. The Void Edge SKILL is another builder's (elite/skills); this sword only carries a data-driven technique file keyed by its item id and reads the skill through a late-bound predicate. |
| Fenrir Sword `fenrir_sword` | **NEW** | only the staff spell "Fenrir Rod" (heroweapon/staff) exists; that is a spell of the Vassal Staff, not an item. Different thing, both kept. |
| Runic Sword Renewed / Ancient / Mythical `runic_sword_renewed`, `_ancient`, `_mythical` | **NEW** | the Rimeheart *Runeblade* and the "rune stone" relic are different items (a mana-lattice sword, a mana-method relic). Kept. |
| Nightingale armour `nightingale_hood`, `nightingale_cloak`, `nightingale_leggings`, `nightingale_boots` | **NEW** | the 11 Magic Knight robe sets and the relic armour stay as they are. |
| Potential Catalyst `potential_catalyst` | **NEW** | the Devil's Core / Genesis Seed (relics/tier) are tier catalysts of RELICS and the forge's "catalyst" slot is a forge input. A different item: a consumable that raises the player (Skill Points and a permanent health step), in the style of the SP Books. |
| Health Potions `health_potion_small`, `_medium`, `_large` | **NEW** | the Recovery Salve is a relic (a slow-heal charm), the vanilla potions are effects. These are quick-drink heal items with a regeneration tail and a shared cooldown. |
| Chronicle Book `chronicle_book` | **NEW** | the **Chronicle Sigil** (arc/world, 1.61.0) is a different item and stays. The book only stores a chronicle id (`nusmp:chronicle_id`) for the seasons builder. |
| Walpurgis Orb / Seal `walpurgis_orb`, `walpurgis_seal` | **NEW** | nothing named so. Plain items with `nusmp:elite_ref` (a free string id) for the council builder. |
| Crates `common_crate`, `rare_crate`, `elite_crate` (blocks) + keys `common_crate_key`, `rare_crate_key`, `elite_crate_key` | **NEW** | the mod has chests with loot tables and the Black Market, no crate. |
| 54 Mob Plushies `plush_<mob>` (blocks + items) | **NEW** | none. |
| Skill Books / SP Books | **EXISTS, reused** | the `skill_book` and `sp` crate rewards and the catalyst's Skill Points go through the skill tree's own doors (`LootDrops.routeBook`, `SkillTreeApi.addSp`); nothing is changed there. |

Replacements: **none**. Nothing existing is removed or renamed; every edit to an existing file is an addition (see the list at the end).

## What is in it and why

### Weapons (`weapon/`, data `data/nusmp/elite/items/abilities/`, art `tools/elite/gen_elite_weapons.py`)
One item class, `EliteWeaponItem` (a `SwordItem`), for the six weapons; `RunicSwordItem` extends it for the three with rune slots. What a weapon DOES is data: `abilities/<item>.json` holds a right-click
technique (`primary`), a sneak + right-click technique (`alt`, on its own cooldown kept in the player's data, the pattern of `WeaponArts`), the bonus strike of a full swing (`swing`, attack charge at 90% or
more, recorded on `AttackEntityEvent`), and an optional paired `skill` with a damage `skill_multiplier`. Six technique KINDS are implemented in `AbilityRunner`: `line_slash`, `chain_marks`, `rift_pull`,
`blink`, `nova`, `ward`. Damage goes through `BalanceLaw.damage` (grows with the wielder's EP, capped on players), allies and the wielder are skipped, particles are the mod's own VFX shapes plus Light / Dark
Rukh only. The numbers are designed here (and scaled by config `damageScale` / `cooldownScale`).

| Weapon | Material, attack | Right-click | Sneak + right-click | Full swing | Engravings (`WeaponEngravings`) |
|---|---|---|---|---|---|
| Astral Edge | netherite tier +4, -2.4 | **Starfall Cut**: 16-block crescent, 11 dmg, glowing and slow (Light Rukh) | **Constellation**: marks up to 5 foes within 14 blocks and strikes each in turn (8 dmg, strips a buff) | +4, 1 health back, glowing | holy_weapon 1, barrier_piercing 1, swift 1, elemental_boost 1 |
| Void Edge | netherite +5, -2.6 | **Rift Cut**: a rift 10 blocks ahead pulls and cuts twice (9 dmg), blind and slow (Dark Rukh) | **Void Step**: blink 9 blocks, cutting what is in the way (10 dmg) | +5, strips a buff, darkness | severance 2, magic_interference 1, soul_eater 1, enervation 1 |
| Fenrir Sword | netherite +6, -2.8 | **Fang Rush**: lunge 8 blocks, 11 dmg, the bite festers (wither) | **Fenrir's Howl**: 8-block nova, knock-back, weakness II and slow II on foes, Speed II and Strength I on you and allies | +4, 2 health back | severance 1, swift 2, energy_steal 1, crushing 1 |
| Runic Sword Renewed | diamond tier +3, -2.4 | **Rune Burst**: 3.5-block nova, 8 dmg | **Rune Ward**: Absorption 4, Resistance, shared with allies | +3 | magic_weapon 1, sturdy 1 |
| Runic Sword Ancient | netherite +3, -2.4 | Rune Burst: 4.5 blocks, 12 dmg | Rune Ward: Absorption 8, Resistance, Regeneration | +4 | magic_weapon 1, magic_interference 1, sturdy 2, elemental_boost 1 |
| Runic Sword Mythical | netherite +6, -2.2 | Rune Burst: 6 blocks, 17 dmg, strips a buff | Rune Ward: Absorption 12, Resistance II, Regeneration II | +6, strips a buff | magic_weapon 1, magic_interference 2, elemental_boost 2, barrier_piercing 1, magicule_absorption 1, sturdy 2 |

**Void Edge pairing.** The sword never imports the skill. Its file names `"skill": "nusmp:void_edge"` and a multiplier (1.35); `EliteItemsApi.skillCheck` is a late-bound `BiPredicate<ServerPlayer,String>` that the
skills builder sets once (default: nobody has it, the sword is simply not boosted).

**Rune slots (Runic Swords).** 1 / 2 / 3 slots by tier (`RuneRules`, pure). A rune is an engraving: drag an enchanted book that carries a Tensura engraving (the tag `#tensura:engraving`, or any `nusmp:` enchantment),
or a rune item from `data/nusmp/elite/items/runes/*.json` (e.g. `minecraft:echo_shard` -> `tensura:magic_interference`), onto the sword in the inventory with the right mouse button (the way a bundle takes an item,
`overrideOtherStackedOnMe`). The engraving is put on as a real enchantment and remembered in `nusmp:runes`; the same engraving cannot be slotted twice; a full sword refuses; a rune whose enchantment a grindstone removed
frees its slot (checked once a second). Each active rune adds flat attack damage (1.0 / 1.5 / 2.0 per rune by tier) as an attribute modifier that is rewritten whenever the runes change, and strengthens the techniques
(`rune_scale` in the file).

**3D models (rule 7).** `tools/elite/gen_items_art.py weapons` draws the 16x16 diagonal icons (the old-style sprite the mesh is placed on); `tools/elite/gen_elite_weapons.py` imports `tools/gen_weapon_models.py`
(its Atlas, Blade, plates, boxes, paints and `write()`), builds the six meshes, skins and glow skins, writes the model jsons and the class table; `tools/elite/finish_weapon_assets.py` adds the game's class lines, the
display presets and their manifest rows without touching older entries. `client/elite/items/EliteItemsClient` registers the six items with the mod's own `WeaponRenderer` (so its mesh cache, class table and normaliser
apply) and the `_icon` models. Astral Edge: slim star-metal blade with a constellation whose stars glow, crescent guard, star pommel. Void Edge: black glass with violet cracks, saw-tooth back, the crimson-violet
dimensional void shows through four fractures (layer 1), broken-ring guard. Fenrir Sword: broad ice-silver blade with fang notches and a glowing fuller, two wolf heads as quillons, fur grip, fang pommel. Runic Swords: one
bastard sword at three ages (mended steel and bronze; dark gold inlay; violet crystal with a winged crown guard), lit rune glyphs on the blade and gems in the guard, one per slot. Sizes are inside the class ranges (longsword
1.4-1.8, greatsword 2.2-3.0 apparent blocks). Better Combat profiles come from `tools/bettercombat/inventory.py` + `gen_attributes.py` (longsword / greatsword).

### Nightingale armour (`armor/`)
Four `ArmorItem`s of one material (defence 3 / 8 / 6 / 3, toughness 2, enchantability 24, repaired with phantom membrane). Lore: a night singer: midnight blue, moon silver, a russet feather. **Set bonus "Nightsong"**
(`NightingaleRules`, `NightingaleBonus`; config `nightingale.setBonus`): Speed I, Night Vision in the dark (light level at most 7; 13 s so it never flickers), and while sneaking the Lullaby heals the wearer and allied
players within 6 blocks by 1 every two seconds (Light Rukh only). Engravings in `ArmorEngravings` (the cloak carries the list, the leggings two, hood and boots one). Worn model: the mod's worn-model library draws any
`nusmp` armour item on a player; the four pieces are listed in the library's catalog (`tools/relics/gen_worn_armor.py` `catalog_elite`, regenerated `relic_worn/catalog.txt`: hood cowl, robe with wide sleeves and a bird
emblem, wrapped legs, tall boots, midnight blue and silver). The painted vanilla layers (`nightingale_layer_1/2.png`) are for everything the library does not draw (a zombie, an armour stand, the option `relicfx.models` off);
on a player the library draws, `getArmorTexture` answers a fully transparent layer so nothing is drawn twice.

### Consumables and plain items (`consumable/`)
* **Potential Catalyst**: right-click, used up. +5 Skill Points through `SkillTreeApi.addSp` (skipped while the skill tree is off) and +1 heart of maximum health for good, at most 10 per player (the count is in the player
  attachment `nusmp:elite_items`, copied on death; the health bonus is an exact function of the count, rebuilt on login and respawn, never stacking). Config `potentialCatalyst.*`.
* **Health Potions** (`HealthPotionTier`, pure numbers): small 6 health at once + Regeneration I 5 s (8 in all), medium 12 + Regeneration I 10 s (16), large 20 + Regeneration II 10 s + 4 hearts of Absorption 30 s (28 and
  absorption). Drink times 1.0 / 1.2 / 1.6 s, stacks 16 / 16 / 8, one cooldown shared by the three sizes (default 3 s), an empty glass bottle back (not in creative). Recipes: bottle + glistering melon + redstone; two
  small + golden carrot; two medium + golden apple.
* **Chronicle Book**: opens nothing yet; stores `nusmp:chronicle_id` (`EliteComponents` / `EliteItemsApi`). A real 3D book in hand (`models/item/chronicle_book_3d.json`, the Skill Book's separate-transforms method: 3D in
  hand and on the ground, the flat icon in the inventory), a teal cover with a gold frame and an hourglass of light.
* **Walpurgis Orb / Seal**: plain items; `nusmp:elite_ref` is a free string id (`/give @s nusmp:walpurgis_orb[nusmp:elite_ref="x"]` works); the council builder gives them use.

### Crates and keys (`crate/`)
`CrateBlock` (one class, state `facing` + `open`, no block entity) for Common / Rare / Elite; `CrateKeyItem` per tier. Using the crate with the matching key in either hand opens it: the key is taken (creative keeps it), the
table of `data/nusmp/elite/items/crates/<tier>.json` is rolled (`CrateRoller`, pure, tested) and the rewards are GIVEN at once, then the show (lid up for 36 ticks, lock sound, a magic circle in the tier colour, Light Rukh
and, for Elite, Dark Rukh, a flare and a shimmer, one chat line per reward). Giving before the show means a logout or crash mid-animation loses nothing. A reward that cannot be given (economy off, skill tree off) is re-rolled
with its type excluded (up to 4 tries), so a switched-off system never makes a crate pay less. `CrateOpenedEvent` is posted for the records builder. Without a key the crate only says which key it needs.

Tables (rolls: common 1-2, rare 2-3, elite 3-4; weights are data): common pays iron, gold, redstone, lapis, beef, XP bottles, small/medium Health Potions, 50-250 coins, XP, 1-2 SP, and rarely the next key; rare pays diamonds,
emeralds, netherite scrap, golden apples, pearls, medium/large potions, Magic Stones, 300-1200 coins, 2-5 SP, a Skill Book, a Potential Catalyst, a few plushies, and rarely a Rare/Elite key; elite pays diamond blocks,
netherite, an enchanted golden apple, a totem, a nether star, large potions, catalysts, a Walpurgis Orb, a Chronicle Book, Magic Stones, 1500-6000 coins, 5-12 SP, Skill Books and the big plushies (Ender Dragon, Warden,
Wither). **Reward serializers** (`RewardSerializers`, a registry, `EliteItemsApi.registerRewardType` adds one): `item`, `coins` (copper units through `EliteHooks.wallet`; skipped when the balance does not move),
`skill_book` (`LootDrops.routeBook`, the skill tree's weighted pick with pity; skipped when it has no route), `sp`, `xp`. **Command-free**: `CrateTableParser` refuses any key it does not know (`command`, `function`, `nbt`
...), so a table can give things but cannot run anything. A bad entry is dropped and logged, a table with nothing usable falls back to a small built-in one.

**Keys in dungeon loot.** A Global Loot Modifier `nusmp:elite_crate_keys` (`data/nusmp/loot_modifiers/elite_crate_keys.json`, listed in `global_loot_modifiers.json`) adds keys to chests that are listed with a chance per
tier (the mod's six chest tables and 9 vanilla dungeon tables: simple dungeon, mineshaft, stronghold, bastion, ancient city, end city, mansion, trial chamber reward). Pure rule `KeyRules` (chance x scale, clamped, each tier
rolled alone). Config: master switch `crates.keyInjection`, scale `crates.keyChanceScale`. Keys have no recipe (loot only); the crates do (planks + iron; Common Crate + diamonds + iron; Rare Crate + gold blocks +
netherite). Crate models: a banded chest with a lock plate (rare / elite a gem), open state has the lid tilted 45 degrees and a glowing light (`neoforge_data` light 15); `tools/elite/gen_crates.py`.

### Plushies (`plush/`, `tools/elite/gen_plushies.py`, `plush_lib.py`, `plush_mobs.py`)
54 plushies (the owner's list, in alphabetical order, appended to only): `PlushKinds` (pure) is the single list; the registry makes one `PlushBlock` and one `BlockItem` per entry; ONE block class, no block entity,
state = facing. Right-click squeaks (the subsystem's own synthesised sound, pitch by size class: tiny 1.75 down to huge 0.8, +-8% random); placing squeaks softly. Each model is 6 to 16 cubes (a big head, a small body,
stubby limbs and the mob's features: ears, horns, wings, antennae, tails, beaks, spikes, tendrils, hats ...) on one 64x64 palette texture of about 2 KB (the whole set 220 KB), built and previewed by the generator
(`--sheet` renders every plush from the front and the back). Translucent slime shell. Soft hook: **`PlushCollectedEvent`** (server player, plushie kind id, distinct count, total) is posted on `NeoForge.EVENT_BUS` the
first time a player holds (an inventory scan every 2 s), places or squeezes a kind; the set is in the attachment `nusmp:elite_items` (kept on death). Config `plushies.collectionCounter`, `squeakVolume`.

### Creative tab and self-tests
Own tab `itemGroup.nusmp.elite_items` ("Elite Items and Plushies"; `NUCreativeTab` is not edited). Self-tests (`/multiverse mega selftest elite.items.<key>`): `registry`, `crates`, `plush`, `abilities`, `engravings`,
`runic`, `catalyst`, `potion`, `nightingale`.

## Config `nusmp-elite-items-server.toml` (section `[items]`)
`enabled`; `crates.enabled`, `crates.keyInjection` (MASTER SWITCH of the loot injection), `crates.keyChanceScale`, `crates.animationTicks`; `healthPotions.cooldownTicks`, `healthPotions.sharedCooldown`;
`potentialCatalyst.maxUses`, `.skillPoints`, `.healthHalfHearts`; `nightingale.setBonus`; `weapons.abilities`, `.damageScale`, `.cooldownScale`; `plushies.collectionCounter`, `.squeakVolume`.

## Public door for the other builders (`elite/items/api/`)
`EliteItemsApi`: `eliteRef` / `setEliteRef`, `chronicleId` / `setChronicleId`, `plushCount` / `plushTotal` / `hasPlush`, `registerRewardType`, `abilitiesOf`, `skillCheck` (the Void Edge's late-bound predicate).
Events on `NeoForge.EVENT_BUS`: `PlushCollectedEvent`, `CrateOpenedEvent`.

## Edits to existing files (all additions)
* `elite/EliteSystem.java`: the one line `com.newuniverse.nusmp.elite.items.EliteItems.init(modBus, container);` in the items section.
* `item/WeaponEngravings.java`: six rows (the six weapons). `item/ArmorEngravings.java`: four rows (the Nightingale pieces).
* `assets/nusmp/weapon_classes.json` and `tools/relics/weapon_classes.json`: six weapon lines; `tools/relics/display_presets_manifest.json`: six rows; `tools/bettercombat/manifest.json`: six rows (generated).
* `assets/nusmp/sounds.json`: four entries appended at the end. `data/neoforge/loot_modifiers/global_loot_modifiers.json`: `"nusmp:elite_crate_keys"` appended.
* `assets/nusmp/relic_worn/catalog.txt` + `tools/relics/gen_worn_armor.py` / `gen_worn_all.py`: the Nightingale pieces (generator entry `catalog_elite`, regenerated lines).

## Tests
Pure JUnit (`src/test/java/.../elite/items`): `CrateRollerTest` (roller, parser, shipped tables), `KeyRulesTest`, `ElitePureRulesTest` (rune rules, catalyst, Nightingale thresholds, ids, ability parser),
`PlushAssetsTest` (54 plushies: model, texture, every face on an opaque patch, blockstate, item model, loot table, lang), `ItemAssetsTest` (icons, 3D weapon files, engravings, classes, Better Combat files, technique
files, armour layers, the book, crates, recipes, names, sounds, potions). In-game: the nine `elite.items.*` self-tests.

## Honest status
Type-checked against the API stubs (`tc_wt.sh`; the only complaints are the stub artefact "MobEffectInstance is abstract"; a negative control was caught) and unit-tested with the pure JUnit runner. NOT run in the game
(the Gradle build cannot run here): the abilities, the crate opening and animation, the rune drag and drop, the key loot modifier, the sounds (synthesised, level-checked, never heard), the `neoforge_data` light on the
open crate, the `getArmorTexture` swap, the creative tab and the 3D weapons in the real renderer are untested. The weapon, plushie and crate pictures are the headless previews (`tools/item_preview/preview_weapons.py`,
`gen_plushies.py --sheet`, `gen_crates.py --sheet`), not game screenshots.
