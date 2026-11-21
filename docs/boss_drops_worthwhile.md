# Worthwhile boss drops (a signature per boss, personal rolls, one-of-ones)

Owner request: "all the bosses in my mod should actually have worthwhile boss drops, like custom drops, one-of-one drops, etc." What the bosses dropped before is in `docs/boss_drops_audit.md`
(28 of 54 dropped nothing worth the fight, 24 were thin, 2 were fine). Nothing here was played in a game: what is proved is proved by unit tests, source scans and the CI server self-tests (`bossdrops2.*`).

## 1. In plain words

* **Every boss of the mod now pays a signature drop of its own**: an item with the boss's name, its own icon, a rarity by boss strength and a boon you get by right-clicking it (a few effects for 4 to 15 minutes).
  On top of it, every fighter rolls their own extras (Magic Stones, diamonds, experience bottles, golden apples, netherite scrap, one boss-themed material, and for some a Tensura gold coin).
* **The 19 strongest or most iconic bosses also pay a one-of-one**: a relic the mod already has, which exists once per world. The first kill makes it and gives it to the player who did the most damage
  (the killer when nobody's damage was seen). While it exists, the next kill pays that top fighter an **Echo** of it instead (a fading copy that cannot be equipped, only absorbed for a strong long boon), and the other fighters
  roll for an echo too (15 to 20 percent). If the one-of-one is lost or destroyed, the God of Order's ledger lets a later kill make it again.
* **A group is paid as a group**: up to five fighters (everybody who dealt at least 2 percent of the boss's health, plus the killer, plus the fighters a Zogratis boss itself recorded). Each rolls their own rows.
* **One setting** switches it all off: `bossDrops.worthwhile` in `nusmp-bossdrops2-server.toml` (default true). False gives exactly the drops of today.

## 2. NEW / REPLACED / KEPT (rule 5)

| | What |
| --- | --- |
| NEW | `bossdrops2/` (13 classes), the 55 data tables `data/anime_multiverse/boss_signature/<entity path>.json` (54 bosses plus `default`), the two items `boss_signature` (one item, the boss lives in the stack) and `boss_echo`, 55 icons and models, 195 lang keys, the command `/multiverse bossdrops list|where|give`, the setting `bossDrops.worthwhile`, the self-tests `bossdrops2.tables|items|pay|unique`. |
| REPLACED | Nothing. No boss, drop, item, texture, model, effect or recipe was removed or changed. |
| KEPT | Every old drop: the Canon Grimoire (`CharterEvents`), the 23 trophies, coins and Lucifero's armour (`bossdrops/`, killer only, its own `nusmp-bossdrops-server.toml`), Goku's / Kakashi's / Light's / the tier bosses' own drops, the Floor Warden's loot table, the dungeon relic and Boss Core rewards. The new rows are paid ON TOP, by a separate hook. No boss entity class was edited (a test pins it), including `GokuBoss` and `ZogratisBoss`. |

New or replacement was checked per boss against the audit: all 54 are additions (nothing in the mod was a "signature drop" or a boss one-of-one before). The 22 bosses that already pay a trophy keep it and get the signature next to it.

## 3. How a kill is paid

1. **Hits**: `LivingDamageEvent.Post`. A hit by a player on a boss is added to a small ledger (boss UUID -> player UUID -> damage). Everything else leaves after two checks (the victim is a boss of this mod, the attacker is a player).
2. **Death**: `LivingDeathEvent` at lowest priority (like the older boss drops; a death another mod cancelled is no kill). The boss entity is claimed once (a second death event of the same entity pays nothing).
   The table is the file named after the entity type path; a boss made by tag (`BossIdentity`) with no table of its own gets `default`.
3. **Fighters** (`SignaturePlan.select`): everybody who dealt at least 2 percent of the boss's maximum health (and at least 1 point), the killer (always, even with no damage seen), and for a Zogratis boss the fighters it recorded itself.
   Creative and spectator players and offline players are not paid. Ordered by damage (the killer wins a tie), at most 5, the killer is never the one cut.
4. **The one-of-one** (`OneOfOne`): if the table has one and its relic is valid, the God of Order's ledger (`UniqueLedger.issue`) is asked once. Made: the top fighter gets it. A live copy exists: the top fighter gets an Echo.
5. **The pieces** (`SignaturePlan.plan`): the one-of-one or echo, then each fighter's signature, then each other fighter's echo roll, then the extras dealt round by round in damage order. At most **12 pieces for the whole kill**,
   in that priority, so a full party never loses the one-of-one or a signature.
6. **Hand-over**: into each player's inventory, the rest drops at their feet (a one-of-one on the ground is made indestructible and permanent by the Unique Guardian the moment it joins the world).
   One chat line per player (vanilla chat): the one-of-one, the echo, or "you fought it and it pays you".

## 4. Seeing where a one-of-one is (bounded, no new tracking)

The one-of-one is an ordinary relic: the **God of Order's relic ledger** (`nusmp_relic_ledger`) is the only authority on whether it exists, and the **Unique Guardian** (`unique/`) already tracks where the valid copy is, keeps a dropped copy from despawning
and puts a destroyed one back into the loot pool. This change adds no second store. To see it:

* `/multiverse bossdrops list` (everybody): each boss's one-of-one, "unclaimed" or "claimed by <player>". It never shows a position.
* `/multiverse bossdrops where <boss>` (operators): the Unique Guardian's last record (held, dropped or in a container, and the dimension and block position).
* `/multiverse unique status relic:<id>` and `/multiverse relic order status` (operators): the same from the existing tools.
* `/multiverse bossdrops give <boss> [count]` (operators): a signature stack, for testing a boon or an icon.

## 5. The setting

`nusmp-bossdrops2-server.toml` (a SERVER config, because it changes gameplay):

```
[bossDrops]
    # Worthwhile boss drops: ...  False: bosses drop what they dropped before this feature.
    worthwhile = true
```

`bossdrops.enabled` (the older trophies) is separate and unchanged.

## 6. The data file

```json
{ "boss": "meliodas", "tier": "apex",
  "signature": { "chance": 1.0, "min": 1, "max": 1, "boon": [ { "effect": "minecraft:strength", "seconds": 900, "amplifier": 1 } ] },
  "extras": [ { "item": "anime_multiverse:magic_stone", "chance": 1.0, "min": 12, "max": 24 } ],
  "one_of_one": { "relic": "hellblaze_the_demon_sin", "echo_chance": 0.2, "echo_boon": [ ... ] } }
```

Checked on load (`SignatureTable`, tested): a boss id of `[a-z0-9_]`, a tier (lesser, mid, high, apex), at most 6 extras, counts 1 to 64, `0 < chance <= 1`, 1 to 4 effects of 10 to 3600 seconds and amplifier 0 to 3,
`echo_chance` in [0, 1]. A bad file is logged and skipped. `/reload` and a data pack work. The tier sets the rarity colour (lesser uncommon, mid rare, high and apex epic, apex also glints).
The tables, icons and lang are written by `python3 -B tools/gen_boss_signature_assets.py` (one place that says what every boss pays; the order is the `custom_model_data` of each icon and is append only).

## 7. What every boss pays

Extras are personal rolls: `item min-max (chance)`. The signature always drops (chance 100 percent, 1 piece). Boons are on the signature; the one-of-one rows have their own stronger, longer echo boon.
Names were invented from each character's canon (no wiki was fetched).

| # | Id | Tier | Signature | Boon (right-click) | Extras | One-of-one (relic) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `meliodas` | apex | Hellblaze Heart | Strength II, Fire Resistance for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), gold_coin 1-2 (50%) | Hellblaze, the Demon Sin (`hellblaze_the_demon_sin`), echo 20% |
| 2 | `escanor` | apex | Noon-Born Sunmark | Strength II, Fire Resistance, Haste for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), gold_coin 1-2 (50%) | Noon Cleaver (`noon_cleaver`), echo 20% |
| 3 | `ban` | high | Snatcher's Fox Fang | Regeneration, Speed II for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), beast_hide 1-2 (60%) | Mammon's Vault Ring (`mammons_vault_ring`), echo 15% |
| 4 | `diane` | high | Gideon Splinter | Resistance II, Strength for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), iron_block 1-2 (50%) | Hammer of Thunderbolts (`hammer_of_thunderbolts`), echo 15% |
| 5 | `king` | high | Chastiefol Seedling | Speed II, Jump Boost II, Slow Falling for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), amethyst_shard 3-6 (60%) | Fairy Glitter Gauntlets (`fairy_glitter_gauntlets`), echo 15% |
| 6 | `gowther` | high | Goat's Lucid Dream | Night Vision, Luck II, Speed for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), ender_pearl 1-2 (60%) | Pandora's Actor Mask (`pandoras_actor_mask`), echo 15% |
| 7 | `merlin` | apex | Sliver of Infinity | Haste II, Absorption II, Regeneration for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), ender_pearl 2-4 (70%) | Astralis - The Eye of the Universe (`astralis_the_eye_of_the_universe`), echo 20% |
| 8 | `fana` | mid | Hatred's Afterglow | Fire Resistance, Strength for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), blaze_powder 3-6 (60%) | - |
| 9 | `rhya` | mid | Dirgeroot Bloom | Regeneration, Resistance for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), bone_meal 4-8 (70%) | - |
| 10 | `rades_spirito` | high | Soulbound Lodestone | Night Vision, Speed, Resistance for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), disciple_sigil 1-1 (40%) | - |
| 11 | `patolli` | high | Loaded Die of Fortune | Luck II, Speed for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), gold_nugget 6-12 (70%) | - |
| 12 | `heath_grice` | mid | Knight's Rime Charm | Resistance, Regeneration for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), blue_ice 2-4 (50%) | - |
| 13 | `lotus_whomalt` | mid | Sideline General's Lotus Seal | Regeneration, Night Vision for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), fermented_spider_eye 1-3 (50%) | - |
| 14 | `mars` | mid | Crystal Weapon Fragment | Strength, Resistance for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), quartz 4-8 (60%) | - |
| 15 | `revchi_salik` | lesser | Chain Thief's Link | Speed II, Haste for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), chain 3-6 (60%) | - |
| 16 | `vetto` | mid | Bound Beast's Clasp | Speed II, Jump Boost for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), beast_hide 1-2 (50%) | - |
| 17 | `minotaur` | high | Labyrinth Bull Crest | Strength, Resistance for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), beast_hide 1-3 (60%) | Minotaur King's Labrys (`minotaur_kings_labrys`), echo 15% |
| 18 | `black_goliath` | high | Wrath Core Sliver | Strength II, Fire Resistance for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), obsidian 2-4 (60%) | - |
| 19 | `amphisbaena` | high | Twin-Head Water Scale | Water Breathing, Dolphin's Grace, Regeneration for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), lizardman_scale 1-3 (60%) | - |
| 20 | `udaeus` | high | Palace Bone Seal | Resistance, Strength for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), bone_block 2-4 (60%) | - |
| 21 | `morris_libardirt` | high | Scholar's Specimen Jar | Haste, Night Vision, Luck for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), writable_book 1-1 (50%) | - |
| 22 | `lucifero` | apex | Sovereign's Gravity Shard | Resistance II, Strength II, Slow Falling for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), crying_obsidian 2-4 (60%) | Godking Crown (`godking_crown`), echo 20% |
| 23 | `dante_zogratis` | apex | Gravity Devil's Weight Stone | Resistance II, Strength, Slow Falling for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), devil_horn 1-2 (50%) | Starsplitter Hammer (`starsplitter_hammer`), echo 20% |
| 24 | `vanica_zogratis` | apex | Curse-Warding Blood Droplet | Regeneration II, Resistance for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), nether_wart 4-8 (60%) | Staff of Blood Magic (`staff_of_blood_magic`), echo 20% |
| 25 | `zenon_zogratis` | apex | Spatial Devil's Door-Key | Speed II, Jump Boost II, Luck for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), ender_pearl 2-4 (70%) | Gate of Babylon Crest (`gate_of_babylon_crest`), echo 20% |
| 26 | `lucius_zogratis` | apex | Time Devil's Stopped Second | Haste II, Speed II, Regeneration for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), clock 1-2 (50%) | Time hammer (`time_hammer`), echo 20% |
| 27 | `devil_avatar` | apex | Walking Devil's Hellfire Coal | Fire Resistance, Strength II, Resistance for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), devil_horn 1-3 (60%) | Incinerator Arm (`incinerator_arm`), echo 20% |
| 28 | `goliath` | high | Pit Giant's Knuckle Stone | Resistance II, Strength for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), juggernaut_heart 1-1 (60%) | - |
| 29 | `charybdis_spawn` | high | Whirlpool's Tidal Pearl | Water Breathing, Dolphin's Grace II, Conduit Power for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), water_spirit_essence 1-3 (60%) | - |
| 30 | `zagred` | apex | Word-Soul Devil's Last Word | Absorption II, Luck, Regeneration for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), heart_of_words 1-1 (50%) | Serie's Ancient Grimoire (`series_ancient_grimoire`), echo 20% |
| 31 | `zagred_legacy` | high | Legacy Word-Soul Ember | Absorption, Luck for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), heart_of_words 1-1 (30%) | - |
| 32 | `goku_boss` | apex | Saiyan's Unbowed Spirit | Strength II, Speed II, Regeneration II for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), golden_carrot 4-8 (70%) | Ruyi Power Pole (`ruyi_power_pole`), echo 20% |
| 33 | `kakashi_hatake` | apex | Copy-Ninja's Sealed Scroll | Speed II, Night Vision, Resistance for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), lightning_rod 1-1 (50%) | Perfect Susanoo Plate (`perfect_susanoo_plate`), echo 20% |
| 34 | `sinbad_boss` | apex | Seven Seas Vessel Shard | Water Breathing, Dolphin's Grace II, Luck II, Strength for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%), nautilus_shell 1-2 (60%) | Leviathan's Crown Trident (`leviathans_crown_trident`), echo 20% |
| 35 | `light_yagami` | lesser | Torn Death Note Page | Luck, Night Vision for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), apple 1-2 (70%) | - |
| 36 | `charlos` | lesser | Celestial Dragon's Gilded Tag | Luck, Speed for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), gold_ingot 2-4 (60%) | - |
| 37 | `zetsu` | lesser | Plant Clone Spore Pod | Regeneration, Night Vision for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), spore_blossom 1-1 (40%) | - |
| 38 | `yammy_llargo` | lesser | Zero Espada's Mask Fragment | Strength, Resistance for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), bone 3-6 (70%) | - |
| 39 | `sogou_nobuyuki` | lesser | Fairy King's Gilded Feather | Speed, Jump Boost, Slow Falling for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), feather 4-8 (70%) | - |
| 40 | `mr_2_bon_clay` | lesser | Okama Way Ballet Ribbon | Speed II, Jump Boost II for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), pink_dye 2-4 (70%) | - |
| 41 | `kurozumi_orochi` | lesser | Shogun's Eightfold Scale | Resistance, Strength for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), scute 1-2 (40%) | - |
| 42 | `malty_melromarc` | mid | Scarlet Princess' Accusation Seal | Luck II, Speed for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), redstone 4-8 (70%) | - |
| 43 | `rachel` | mid | Climber's Lighthouse Light | Night Vision, Slow Falling, Jump Boost II for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), lantern 1-1 (50%) | - |
| 44 | `jugram_haschwalth` | high | Balance Ritter's Scale Weight | Resistance II, Strength, Absorption for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), iron_ingot 4-8 (70%) | - |
| 45 | `big_mom` | high | Emperor's Soul Candy | Health Boost II, Regeneration, Strength for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), sweet_berries 6-12 (80%) | - |
| 46 | `code` | mid | Kara's Karma Shard | Strength, Resistance for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), ender_pearl 1-2 (50%) | - |
| 47 | `bertholdt_hoover` | high | Colossal Titan's Steam Ash | Fire Resistance, Resistance II, Strength for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%), blaze_powder 3-6 (60%) | - |
| 48 | `axe_hand_morgan` | lesser | Axe-Hand's Marine Badge | Strength, Haste for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), iron_nugget 6-12 (70%) | - |
| 49 | `foxy` | lesser | Silver Fox's Return Coin | Speed II, Luck for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%), gold_nugget 6-12 (70%) | - |
| 50 | `shou_tucker` | mid | Sewing-Life Chimera Thread | Regeneration, Resistance for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), string 4-8 (70%) | - |
| 51 | `ao` | mid | Byakugan Seer's Pale Lens | Night Vision, Speed, Luck for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%), amethyst_shard 2-4 (50%) | - |
| 52 | `floor_warden` | lesser | Warden's Sealed Shadow Link | Resistance, Night Vision for 4 minutes. | magic_stone 2-4 (100%), emerald 1-3 (50%) | - |
| 53 | `labyrinth_guardian` | high | Sector Guardian's Core Splinter | Resistance II, Strength, Regeneration for 10 minutes. | magic_stone 8-14 (100%), diamond 1-3 (60%), experience_bottle 6-12 (80%), golden_apple 1-1 (35%) | - |
| 54 | `god_of_order` | apex | Judge's Unsealed Verdict | Resistance II, Absorption II, Luck II, Regeneration for 15 minutes. | magic_stone 12-24 (100%), diamond 2-5 (80%), netherite_scrap 1-2 (50%), enchanted_golden_apple 1-1 (12%) | Administrator's Authority Key (`administrators_authority_key`), echo 20% |
| 55 | `default` | mid | Boss Remnant | Strength, Resistance for 6 minutes. | magic_stone 4-8 (100%), emerald 2-5 (60%), experience_bottle 3-6 (60%) | - |

The 19 one-of-ones are relics the mod already has (grades GOD, GENESIS or LEGEND, most with 3D hand models, engravings and Better Combat files, so no new weapon was made and rules 6, 13 and 14 do not apply to new weapons here).
Four of them were chosen away from the first draft because the Rift Pockets own their relics (`PocketDataTest`: a pocket relic may appear in no other data file), so Merlin pays Astralis, Vanica Staff of Blood Magic,
Lucius the Time hammer and Zagred Serie's Ancient Grimoire.

## 8. Cost, worst case (rule 11)

| What | Worst case |
| --- | --- |
| Server work per tick | None. No tick, no timer, no scan, no entity spawn of its own. |
| Server work per hit on any living thing | Two cheap `instanceof` checks; for a player's hit on a non-player one registry key lookup and one hash lookup (`BossIdentity` only for an entity of this mod without a table). A hit on a boss adds one ledger entry. |
| Memory | At most 64 bosses x 16 players = 1024 damage entries (the least recently hit boss is forgotten first, a boss is forgotten when paid) and 256 remembered "already paid" ids; 55 small immutable tables. Cleared when the server stops. |
| Server work per kill | One table lookup, one plan of at most 12 pieces for at most 5 fighters, one relic ledger lookup (and one saved-data write when the one-of-one is made), one scan request, at most 5 chat lines. At most 12 item entities when inventories are full. |
| Network | 0 packets of its own (vanilla inventory sync and chat only). |
| Client | 55 icon models (16 x 16 sprites, one flat quad each) loaded at resource load; drawn only while held, dropped or in a slot. No renderer, no particles, no per-frame work. |
| Multi-player | State is keyed by player and boss UUID; nothing is "the current player"; only online, non-creative, non-spectator fighters are paid; the one-of-one is decided by the server-wide relic ledger, so two simultaneous kills of two bosses cannot both make the same relic (they are distinct relics) and two kills of the same boss cannot either. |

## 9. Tests

* `SignatureTableTest` (pure): a good file, 22 bad files refused with a reason, the caps.
* `SignaturePlanTest` (pure): fighter selection (top damager, killer wins a tie, the 2 percent share, the killer kept, the cap of 5), the one-of-one (first kill, second kill pays an echo, the ledger is asked once and only when wanted, a release lets it be made again), personal rolls, counts in range, the 12-piece cap over 3000 random kills.
* `DamageLedgerTest` (pure): sums, the 64-boss and 16-player caps, the paid-once memory, ignored NaN / negative / infinite hits.
* `SignatureAssetsTest` (reads the repo): catalog = index = data files, order = `custom_model_data`, every table valid and equal to the catalog, only items and effects that can exist, an icon and a model per boss, the lang keys.
* `SignatureSourceScanTest`: no boss class names the package, exactly three one-line appends outside it, no client class, no tick or render event, no static player, no second saved store, the one setting, the setting is checked first.
* CI self-tests `bossdrops2.tables` (every catalog boss and every boss of the registry has a table; every extra, effect and relic exists in the real registries), `bossdrops2.items`, `bossdrops2.pay` (three stand-in fighters, the cap, everybody gets a signature),
  `bossdrops2.unique` (the first kill makes the one-of-one through the real ledger, the second pays an echo and no second copy; the relic is released again afterwards).
* The whole unit sweep (12,685 tests) passed except one test that found the four pocket relics in the first draft; fixed and re-run (59 of 59 in the touched packages).

## 10. NOT DONE, not seen, and decisions for the owner

* **Nothing was seen in a game.** The unit tests and the whole sweep ran; the server self-tests run only in CI. The boons, the chat lines and the icons have not been looked at in a client.
* **The signature items are 2D sprites**, drawn by a Python script (`tools/gen_boss_signature_assets.py`, Pillow), 16 x 16, six shapes recoloured per boss. They are NOT Blender or Blockbench models (rule 14 / 15): no 3D hand model was made for them.
  The 19 one-of-ones keep the 3D models they already have. A set of per-boss 3D signature models is not built.
* **No new weapon was made**, so nothing needed new engravings or Better Combat files. The one-of-ones already have theirs.
* **Echo has one icon for every boss** (one neutral item; its name carries the relic).
* **Only five fighters share a kill, only the first sixteen players who hit a boss are counted, and a full party's extras are squeezed by the 12-piece cap** (the one-of-one, echoes and signatures always come first). A bigger raid would need a decision on a higher cap.
* **Offline fighters are not paid** (nothing can be put into an offline inventory).
* **A clone or decoy of a boss that has the same entity type and dies pays too** (the older trophy hook has the same property); the `once per entity` guard only stops a second death event of the same entity.
* **Relics can still be found elsewhere first**: if a one-of-one relic was already rolled from a chest or a vault, the boss pays the echo, never a second copy (by design, but the owner may want the 19 relics out of the chest pools).
* **Balance is a first guess**: the boons (4 to 15 minutes), the extras and the echo chances (15 to 20 percent) are one table in the generator; tune them by editing the JSON or the generator.
* **The `default` table** pays any boss made by tag (and any addon boss in the tag) a mid-tier signature and extras but no one-of-one. A boss made by a tag on a Tensura body is the only non-mod entity it can ever pay for.
* **God of Order and the Labyrinth Guardian**: both now pay (the Order's authority key is its one-of-one). The Guardian pays on every sector Guardian and Decennial boss, on top of the Decennial relic and Boss Cores.
* **Wiki research**: none was fetched (rule 10); the names are invented from canon. Nothing could not be fetched because nothing was tried.
* **Hand-over notes for the release step** (not done here by instruction): bump the version, write the changelog and README jar name, and add `bossdrops2` to the smoke list (`SMOKE_EXPECT`); the three one-line appends are in `NUSMP.java` (boot), `NUCreativeTab.java` (creative stacks) and `MultiverseCommands.java` (the command).
