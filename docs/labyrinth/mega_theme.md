# Region Themes and the Crucible of Choice (builder THEME, 1.54.0 and 1.55.0)

Part of the Mega-Dungeon (`docs/labyrinth/mega.md`, `docs/labyrinth/mega_floors.md`). This builder owns four things: the **Region Theme processor**
`nusmp:region_theme` with the **eleven theme files** that say what each sector looks, sounds and bites like; the **Crucible of Choice**, the vault at the end of
a sector where exactly three relics float and taking ONE of them forfeits the rest and starts a catastrophe; the **relic roads**, which let the relic that was
taken decide the theme (and the ruleset) of the NEXT sector; and the **six vault templates** the Crucible lives in.

**State of this document.** Written without a game. CI compiles the Java and runs the pure JUnit tests; the templates are written by a script and walked by
three static checkers; the in-game self-tests (section 12) are run by the CI server smoke test. **Nothing here has been run in the game**; section 15 lists
exactly what that leaves unverified. Every class, file and strategy below says WHY it is the way it is.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5)

| Part | Status | Notes |
|---|---|---|
| Structure processor `nusmp:region_theme`, `ThemeSwap` (suffix rules), `ThemeDef` / `ThemeDefs` / `ThemeTables` | NEW | The processor list `nusmp:mega_interior` (ARCH) already names it first. |
| Theme files `data/nusmp/mega/themes/*.json` (eleven) | NEW | The brief said nine; `RegionTheme` gained `RADIANT` and `ABYSS` in 1.55.0, so there are eleven. A test pins "every `RegionTheme` has a file". |
| Theme light, mood sound and danger (`light`, `sound`, `danger` in the theme files) | NEW | Optional fields; a file without them is the baseline's light, silence and danger 1. |
| Ambient particle and mood sound handler (`ThemeAmbient`) | NEW | |
| Crucible of Choice: vault ticker, offer, pick hook, catastrophes (collapse, flood, spirit), Cursed Spirit roster | NEW | Extends the existing Relic Pedestal; relics stay 100 % unobtainable by normal play. |
| `RelicLoot.peekUnissued(...)` (one new public method) | NEW | Rolls candidates WITHOUT stamping the God of Order's ledger. |
| Take hook on `RelicPedestalBlockEntity`, drop guard on `RelicPedestalBlock` | NEW (additive) | Default behaviour unchanged for every pedestal that is not a Crucible pedestal. |
| `RelicLoot.rollUnissuedAll` / `anyUnissued` and `RelicExclusivity.allowsMobDrop(Level)` answer "nothing" inside the Mega-Dungeon | NEW (outside the brief, see 13) | A Mimic or a chest table could otherwise add a second source of relics; the Crucible is the only one. |
| Crucible of "1 to 3 relics of mixed grades" (1.54.0 draft: the grade band rose with the sector) | **REPLACED** | By the owner's 1.55.0 rule: EXACTLY three distinct relics, always of the grades Unique, Legend, Mythical (`Grade.GOD`) or Genesis. The config `offerCount` (1 to 3, default 3) keeps the old shape available; the vault templates for one and two relics are kept (pools `vault_1`, `vault_2`). |
| "Theme relic pool prefers relics whose name has a theme keyword" (1.54.0 draft) | **REPLACED** | By the 1.55.0 rule that the CHOSEN relic decides the NEXT theme: the same keywords now map a relic to the theme it leads to (`RelicThemes`). |
| `relics.gradeBias` in a theme file | RETIRED | A file that still has it loads without a warning; nothing reads it. |
| Vault templates `vault/one_a`, `one_b`, `two_a`, `two_b`, `three_a`, `three_b` | NEW | `three_*` are the ones used (pool `vault_3`); `one_*` and `two_*` stay registered for a different `offerCount` / theme range. |
| `ThemeConfig` (`nusmp-labyrinth-mega-theme-server.toml`, section `[theme]`) | NEW | |
| Spire pedestals, creative-placed pedestals, the old Relic Pedestal behaviour | KEPT | The hook answers "not mine" for any pedestal that is not in a sector's saved pedestal list. |
| `RelicExclusivity` rules of 1.35.0 (Labyrinth and Living Dungeon only) | KEPT | Extended by one branch for the Mega-Dungeon. |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. The whole thing on one page

```
 generation                     first visit                      the pick                                  right after the pick
 ----------                     -----------                      --------                                  --------------------
 sector built with a theme      a survival player enters         right-click a pedestal                    Phase RESOLVED + MegaDungeon.crucibleResolved:
   |                            the vault box (+-14, +-8)          |                                          the next sector is released
   | RegionContext (ThreadLocal)  |                                v CrucibleHook.onTake                      |
   v                              v CrucibleVault (every 10 ticks)  1. Phase CHOSEN written FIRST            v
 RegionThemeProcessor swaps     UNOPENED -> ROLLED                  2. ONE relic issued through the ledger     the generator asks MegaDungeon.themeOverride(sector)
   deepslate / dark oak         3 relics, Unique or above,         3. given to the player                       -> NextTheme: the chosen relic's theme (and ruleset)
   for the theme's families     three DIFFERENT roads where        4. ALL pedestals emptied and sealed         meanwhile the catastrophe runs on its own clock
   and shifts light blocks      the world allows, none issued      5. quest key + the catastrophe starts        (collapse 8 s, flood 30 s, spirit until it dies or is let go)
                                                                   6. RESOLVED + crucibleResolved
```

The relic taken is saved in the Crucible of its sector (`Sector.crucible.chosen`), so everything downstream (the next theme, the ruleset, the status
command) is a pure function of saved data and survives a restart.

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. The processor and the swap rule

`RegionThemeProcessor` (`nusmp:region_theme`, a `StructureProcessor` with a unit codec) reads the sector being built from `RegionContext` (a ThreadLocal the
generator sets for each piece) and rewrites the template's palette blocks. WHY a ThreadLocal: a processor sees one block at a time and knows nothing about the
sector; world generation of the Overworld runs on other threads at the same time and must never see a sector's theme; with no context (a datapack Jigsaw,
`/place`, the old Spire) the processor returns every block untouched. The new state is `target.withPropertiesOf(state)`, so a stair keeps its facing, half
and shape, a wall its sides, a log its axis, a slab its type; the block entity data travels unchanged. It never throws.

`ThemeSwap` derives the target of every palette block from ONE base block per slot, by the id suffix, in this order:

1. the theme's `explicit` map wins when it names the template block and the target exists (how a theme turns `soul_lantern` into `lantern`);
2. the plain block is the family base of its slot;
3. a shape is the base plus the shape's suffix (`_stairs`, `_slab`, `_wall`); when that id does not exist, the singular of a trailing `bricks` / `tiles`
   (`nether_bricks` gives `nether_brick_stairs`), then the base without a trailing `_block` (`purpur_block` gives `purpur_stairs`);
4. wood shapes use the stem of the planks (`crimson_planks` gives `crimson_stairs`; a log is `_log`, else `_stem`, else `_block`) and NEVER fall back to a stone family;
5. a stone shape the family lacks (purpur has no wall, polished diorite has no wall, netherrack has nothing) comes from the first FALLBACK slot that has it
   (brick, then tile, polished, cobbled);
6. when nothing exists the answer is NONE and the template block stays.

WHY suffix rules and not a table: vanilla is not regular (quartz bricks have no stairs at all, purpur no wall) and nine themes times 31 palette blocks would
be hundreds of JSON lines that must all be right; the exceptions are few enough to be spelled out once and tested (`ThemeSwapTest`, `ThemeDataTest`).
`ThemeTables` turns the derived ids into an identity map from `Block` to `Block` once per theme (lazily: the registries are not bound at mod construction;
every id is checked against `BuiltInRegistries.BLOCK`, a bad one is logged ONCE and costs only that block its swap) and drops it on a datapack reload.

**The theme's light.** The same processor moves the invisible `minecraft:light` blocks of the templates (window glow, glowing growths) by the theme's
`light` shift, `ThemeSwap.shiftLight`: never above 15 and never below level 6 (or below their own level when lower), so a theme never takes a template's
designed light under the dungeon's gloom limit. The visible lamps change by the `explicit` map: Radiant, Flame turn the soul lanterns, soul torches and soul
campfires of the templates into warm ones (15 instead of 10); Abyss, Shadow and Frost turn warm ones into soul lights.

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The eleven themes

`RegionTheme` names them (append-only, the ordinal is never saved); `data/nusmp/mega/themes/<id>.json` says what they mean. The lead's follow-up spoke of twelve
themes: the enum has eleven (the original nine and `RADIANT`, `ABYSS`), and every one has a file.

| Theme | brick | tile | polished | chiseled | cobbled | cracked_brick | cracked_tile | rock | wood |
|---|---|---|---|---|---|---|---|---|---|
| Gravity | deepslate_bricks | deepslate_tiles | polished_deepslate | chiseled_deepslate | cobbled_deepslate | cracked_deepslate_bricks | cracked_deepslate_tiles | deepslate | dark_oak_planks |
| Blood | nether_bricks | red_nether_bricks | polished_blackstone_bricks | chiseled_nether_bricks | netherrack | cracked_nether_bricks | cracked_polished_blackstone_bricks | netherrack | crimson_planks |
| Spatial | end_stone_bricks | purpur_block | amethyst_block | purpur_pillar | end_stone | end_stone | obsidian | end_stone | warped_planks |
| Flame | blackstone | polished_blackstone_bricks | polished_blackstone | chiseled_polished_blackstone | basalt | gilded_blackstone | cracked_polished_blackstone_bricks | basalt | dark_oak_planks |
| Frost | stone_bricks | polished_diorite | snow_block | chiseled_stone_bricks | cobblestone | cracked_stone_bricks | calcite | stone | spruce_planks |
| Verdant | mossy_stone_bricks | mossy_cobblestone | stone_bricks | chiseled_stone_bricks | cobblestone | cracked_stone_bricks | moss_block | mossy_cobblestone | jungle_planks |
| Storm | tuff_bricks | waxed_oxidized_cut_copper | polished_tuff | waxed_oxidized_chiseled_copper | tuff | waxed_weathered_cut_copper | waxed_exposed_cut_copper | tuff | spruce_planks |
| Shadow | polished_blackstone_bricks | deepslate_tiles | polished_deepslate | chiseled_polished_blackstone | blackstone | cracked_polished_blackstone_bricks | cracked_deepslate_tiles | deepslate | dark_oak_planks |
| Astral | quartz_bricks | polished_diorite | prismarine_bricks | chiseled_quartz_block | calcite | smooth_quartz | dark_prismarine | calcite | birch_planks |
| Radiant | smooth_quartz | smooth_stone | quartz_block | gold_block | diorite | quartz_bricks | calcite | calcite | birch_planks |
| Abyss | polished_deepslate | cobbled_deepslate | deepslate_bricks | chiseled_deepslate | netherrack | sculk | moss_block | deepslate | mangrove_planks |

| Theme | Colour | Catastrophe weights | Ambient particle | Light | Mood sound (volume, pitch, every N pulses) | Danger | Explicit swaps |
|---|---|---|---|---|---|---|---|
| Gravity | #6B6F7A | collapse 4, flood 1, spirit 1 | ash | 0 | ambient.cave (0.5, 0.8, 8) | 1.0 | none (the identity) |
| Blood | #9A1B1B | 1 / 2 / 3 | crimson_spore | -1 | ambient.crimson_forest.mood (0.5, 1.0, 6) | 1.15 | none |
| Spatial | #B070E0 | 1 / 2 / 2 | portal | 0 | block.portal.ambient (0.35, 0.9, 7) | 1.0 | none |
| Flame | #E8741E | 3 / 1 / 2 | flame | +2 | block.lava.ambient (0.6, 0.9, 4) | 1.1 | soul lights to warm lights |
| Frost | #8FD3F4 | 3 / 2 / 1 | snowflake | +1 | block.amethyst_block.chime (0.45, 0.6, 6) | 1.0 | lantern to soul lantern |
| Verdant | #4F9D3A | 1 / 4 / 2 | spore_blossom_air | 0 | ambient.warped_forest.mood (0.5, 1.0, 6) | 0.95 | none |
| Storm | #2FB5A8 | 3 / 1 / 2 | electric_spark | 0 | entity.lightning_bolt.thunder (0.25, 0.9, 9) | 1.1 | none |
| Shadow | #3B3558 | 1 / 2 / 4 | sculk_soul | -2 | ambient.soul_sand_valley.mood (0.55, 1.0, 5) | 1.15 | warm lights to soul lights |
| Astral | #F0E6B8 | 1 / 3 / 2 | end_rod | +2 | block.amethyst_block.resonate (0.4, 1.0, 6) | 0.95 | quartz stairs and slabs, diorite and prismarine walls |
| Radiant | #FFD95A | 3 / 2 / 1 | nusmp:light_rukh | +3 | block.bell.resonate (0.3, 0.7, 8) | 0.8 | soul lights to warm lights, diorite walls |
| Abyss | #1F4F57 | 1 / 3 / 3 | soul_fire_flame | -4 | entity.warden.heartbeat (0.5, 0.8, 4) | 1.3 | warm lights to soul lights |

Reading the table: the Holy road's signature theme Radiant is the brightest, the kindest and its sky is thick with Light Rukh; the Abyssal road's Abyss is the darkest,
the cruellest and grows sculk and moss through dark deepslate. They must not be mistaken for Astral (a cold white observatory with a dark prismarine floor)
or Shadow (black blackstone walls, nothing growing), which is why the data test insists that any two themes differ in at least four of the nine slots and
in their walls. WHY every number is a plain data value: nothing here could be balanced in a game before it shipped, so the owner retunes a file and not the mod.

### The theme JSON schema

```json
{
  "_why": "why this theme is built this way (mandatory in this project; a key starting with _ is ignored by the parser)",
  "id": "blood",
  "name": "Blood",
  "color": "#9A1B1B",
  "families": {"brick": "minecraft:nether_bricks", "tile": "...", "polished": "...", "chiseled": "...", "cobbled": "...",
               "cracked_brick": "...", "cracked_tile": "...", "rock": "...", "wood": "minecraft:crimson_planks"},
  "explicit": {"minecraft:soul_lantern": "minecraft:lantern"},
  "relics": {"keywords": ["blood", "crimson", "vampir"]},
  "catastrophes": {"collapse": 1, "flood": 2, "spirit": 3},
  "ambient": "minecraft:crimson_spore",
  "light": -1,
  "sound": {"id": "minecraft:ambient.crimson_forest.mood", "volume": 0.5, "pitch": 1.0, "every": 6},
  "danger": 1.15
}
```

`families` takes the nine slots (a missing slot borrows from its fallback slots); `explicit` takes any vanilla block to any vanilla block; `keywords` are the relic words
of section 6; `catastrophes` are weights 0 to 100 (a theme without any gets 4 / 1 / 1); `ambient` must be a particle without options; `light` is -8 to 8;
`sound.volume` 0.05 to 1, `pitch` 0.5 to 2, `every` 1 to 60 (once in that many two-second pulses per player, five to nine blocks away); `danger` 0.5 to 2.
`ThemeDef.parse` turns every wrong field into one warning line and a safe default, never an exception: a typo in a datapack must not stop a sector from
generating. A theme without a file looks like the baseline; the baseline `gravity` is also baked into the code.

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. The Crucible of Choice

State is `Sector.crucible` (`phase`, `offered` in slot order, `chosen`, `chooser`, `catastrophe`, `since`); the pedestals are `Sector.pedestals`, in the order of the
`pedestal;slot=i` markers of the vault template. The rules are pure (`CrucibleRules`, `CatastropheRules`) and tested; the glue reads the saved state, asks one
of them and applies the answer.

**Offer** (`CrucibleOffer`). The first survival player inside the vault box (centre `Sector.vaultCenter`, +-14 blocks horizontally, +-8 vertically) of an UNOPENED vault
rolls it: `offerCount` (default 3) distinct relics of the grades from `minGrade` (default Unique) up to Genesis, weighted by each grade's own loot weight
(Unique 8, Legend 4, Mythical 1.5, Genesis 0.5, so Genesis stays very rare), never a pair relic, never one that is alive, never one already on offer in another
vault. They come from `RelicLoot.peekUnissued`, which does NOT touch the ledger: the unchosen relics must stay unissued or taking one would burn the others. A dozen
are drawn and the first three that lead to different roads are kept (`CrucibleRules.distinctRoads`): the relic taken decides the next ten floors, so a choice
between three relics of one road would be no choice. Fewer than three left in the world: the vault offers what exists; none: it is RESOLVED empty with a
message. The stacks on the pedestals are plain, unissued stacks; the saved ids are the truth, so a re-entry (every two seconds while ROLLED) repairs a
pedestal that lost its stack. A creative player does not wake the vault.

**Reading the choice** (`showRoads`, default true). When the offer appears every player in the vault is told, in chat, the three relics with their grade and the road
each leads to ("Frostfang (Legend): leads to Frost: The Long Winter"), and a player who looks at one of the floating relics (within 15 degrees, seven blocks) reads
the same line on the action bar: a right-click on a pedestal IS the pick, so the name has to come from looking.

**The pick** (`CrucibleHook`, installed in front of `RelicPedestalBlockEntity.take`). For a pedestal in a sector's pedestal list it does, in this order, and the order is the guarantee:

1. Phase CHOSEN, the chooser, the chosen id, the drawn catastrophe are written and the data marked dirty BEFORE anything else can fail or anybody else can click: a second player in the same
   tick, a double click, a second pedestal all find CHOSEN and get "too late" (`CrucibleRules.take`);
2. ONLY the chosen relic is issued through the God of Order's ledger (`UniqueLedger.issue`); if it went live meanwhile, ONE replacement is rolled and tried once; if that
   fails too the player is told and the vault ends like an empty one (the chosen id is cleared, so the next theme does not follow a relic nobody holds);
3. the stack goes to the player's inventory, else to the floor at their feet;
4. in a `finally`, so no exception can leave a relic on a pedestal, EVERY pedestal of the vault is emptied and marked taken (the others crumble: poof, ash, dust, a crash);
5. the quest key `mega.crucible_choice` is fired with the relic id, the announcement says which road the relic chose, and the catastrophe starts;
6. the vault is marked RESOLVED and `MegaDungeon.crucibleResolved(level, sectorId)` is called, which releases the next sector (the generator then asks `MegaDungeon.themeOverride`, section 6): the choice is made, the
   catastrophe is its aftermath and runs on its own clock, so the dungeon grows while the party deals with it. Phase CHOSEN is only the lock of step 1; it outlives a pick only if something failed in the middle,
   and the vault ticker sweeps such a vault up (RESOLVED and the same event) so the dungeon is never held back by it.

A failure inside the hook is logged and answered "nothing given"; it never falls through to the old pick, which would hand a PREVIEW (a relic that was never issued) to a player.
The drop guard on `RelicPedestalBlock` does the same for removal: a Crucible pedestal never drops what it shows (it is a preview); every other pedestal drops as before.

**Catastrophes** (`CatastropheRunner`, server tick driven, state in memory plus the `catastrophe` string; a restart in the middle simply ends it, so nobody is trapped by a countdown that no longer exists). The kind is a weighted draw with the theme's weights, deterministic from the
sector seed and the chooser. None changes a block (so nothing needs the Dungeon Guard's permission and nothing has to be restored); each is survivable by running out of the vault.

* **Collapse.** Two seconds of warning (rumbling, a rain of dust), then every 1.5 seconds a stone is aimed at each person in the vault: a ring of dust and a column of falling dust mark the spot for
  0.7 seconds, then it lands (broken masonry, smoke, a crash) and hurts whoever is still within 1.8 blocks, while harmless stones crash down all round. A person who keeps moving takes almost nothing.
  A stone never takes more than 35 % of a person's maximum health and the whole collapse never more than 60 % of it (a per-person budget), so it cannot kill by itself.
* **Flood.** Toxic magicules: for `floodSeconds` (30) everyone in the vault box gets the mod's magicule sickness (level `round((threat - 1) * floodAmplifierPerThreat)`, at most 4) and slowness II,
  renewed every second for three seconds only (so they lapse on their own), violet particles rise from the floor, the action bar counts down, the exits stay open, and it ends early when nobody is inside.
* **Spirit.** A Cursed Spirit (data: `cursed_spirits.json`) is summoned at the vault centre inside a three second summoning circle (held still and unhurtable so a party can run or brace), named, glowing,
  tagged `nusmp_cursed_spirit`, scaled by threat; boss rules apply (no healing in combat, no forbidden skills, nothing dropped, no experience); it ends when it dies, when it is gone from loaded
  ground, or `spiritMinutes` (10) after the last player left the sector. A leftover spirit that is loaded from disk and that no running catastrophe tracks is refused entry.

The theme's `danger` scales the part of the sector's threat that the catastrophes count (`CrucibleRules.dangerThreat`: threat 1 is never changed), so the same pick is crueller in an Abyss sector and kinder in a Radiant one.
`MegaDungeon.crucibleResolved(level, sectorId)` is called ONCE per vault: at the pick (step 6), or, when no pick happens, when the vault is resolved empty (no relic of those grades left) or the ledger refused, or when a
pick that failed half way is swept up. A catastrophe ending resolves nothing.

**Why the grade band is fixed.** The Crucible is the only place a relic of Unique or above can be found in the Mega-Dungeon (the relic roll, a Mimic and the vault loot tables give none there), so a vault is never a
lesser prize than the one before it; the depth is paid for by the catastrophe and the sector's threat, not by the relic.

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. The relic roads: the relic taken decides the next ten floors

`RelicThemes` (pure) maps a relic to a **road**: `Road(theme, ruleset)`. The theme comes from, in order: (a) the explicit table `data/nusmp/mega/relic_themes.json` (the famous relics, placed by their lore;
about 137 entries), (b) the relic words: the non-baseline theme with the most DISTINCT `relics.keywords` that begin a word of the relic's id or card name (word-prefix, so `ice` does not hide in
"Voice"; a tie goes to the theme that comes first in the enum), (c) a stable hash of the relic id (FNV-1a and splitmix64, pinned by a test) over every theme but the baseline. The baseline Gravity
is reachable only from the table, which is also what stops a generic word such as "stone" from sending every Infinity Stone to the heavy road. Unknown relics therefore still lead somewhere, always to the same place.

The **ruleset id** is the name other systems may key their own data on when a theme alone is too coarse (an encounter table, a faction mix, a trap set for one relic's road). It defaults to the theme's
own id, so a system that knows only themes keeps working unchanged; a relic may name a variant in the optional `rulesets` object of `relic_themes.json` (`"frostfang": "the_long_winter"`), and `ruleset_names`
gives the name a player reads (the eleven themes have one each: Heavy Halls, The Blood Tithe, Folded Rooms, The Forge Heat, The Long Winter, The Overgrowth, Static Air, Lightless Halls, The Starlit Way,
Hallowed Ground, The Deep Rot). No relic names a variant yet.

`NextTheme` answers "what comes after this sector?": null until the vault is CHOSEN (or RESOLVED), then the road of the saved `crucible.chosen`; a vault resolved empty gets a stand-in theme from the hash of the
sector id and seed. It is registered as the Mega-Dungeon's `ThemeOverride` by `ThemeRegistry.init` (`MegaDungeon.setThemeOverride(NextTheme.HOOK)`), so the generator asks the contract and never imports this
package. `NextTheme.rulesetOf(MegaData, Sector)` derives the ruleset of any sector from saved data alone (the road of the Crucible of the previous sector of its chain, only when the sector really wears the theme
that road names), so **no field was added to `Sector`**; section 14 says what the contract could add if the lead wants a stored copy.

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. Data files

| File | WHY it is data | Read by |
|---|---|---|
| `data/nusmp/mega/themes/<id>.json` (11) | what a theme looks, sounds and bites like is the owner's to retune without a mod update | `ThemeDefs` (resource-manager reload listener: one broken file is skipped with a log line, the rest still load) |
| `data/nusmp/mega/relic_themes.json` | the hand-made half of the relic-to-theme map and the ruleset names | `RelicThemeMap` (reload listener; missing or broken: every relic is placed by words or hash) |
| `data/nusmp/mega/cursed_spirits.json` | the best candidates for a "massive Cursed Spirit" are other mods' monsters (the Tensura daemons, hounds, moths, weavers, the phantaspore) whose ids and strength the owner will tune | `CursedSpirits` (an id no loaded mod registers is skipped; with nothing left the catastrophe becomes a collapse) |

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. The vault templates (`structure/mega/vault/*.nbt`, written by `tools/labyrinth/gen_mega_vault.py`)

Six deterministic templates in the shared palette (the deepslate and dark-oak families, so the processor restyles the architecture) plus neutral accents the theme leaves alone (gold blocks, red carpet,
stained glass, soul lanterns, candles, iron bars, chains, skulls, lightning rods, banners). The only custom block is the Relic Pedestal, placed WITHOUT block-entity data (the Crucible fills it). Each has ONE socket,
the entry on the south face at offset 13 (`nusmp:mega_vault`, an open hole: the vault is attached TO the antechamber), one `vault_center` marker, one `pedestal;slot=i` marker in the air cell directly above each
pedestal, no spawn marker and no other door: the way out is the way in. The red carpet runs from the door to the relics and the strip is declared clear, so nothing solid is ever put on the route out.

| Template | Size | Pedestals | Look |
|---|---|---|---|
| `three_a` (used, pool `vault_3`) | 27 x 48 x 27 | 3 | THE ROTUNDA: a round hall under a ribbed dome; carpet from the door across a gold-spoked medallion; a horseshoe of six pillars with lightning-rod spires; three raised daises in an arc; four shrines with armoured statues, wall skulls and cobwebs in the corners; cages and a wheel of soul lanterns on chains; a field of lit candles; remains of earlier parties |
| `three_b` (used, pool `vault_3`) | 27 x 48 x 36 | 3 | THE NAVE: a long vaulted hall with a double row of pillars, transverse arches, stained windows with hidden light blocks, a red carpet to a stepped chancel where the relics stand in a row on gold bases before niches with banners, skulls and hanging lanterns; cages and chandeliers hang from the vault |
| `one_a`, `one_b`, `two_a`, `two_b` | as above (`one_b`, `two_b` 27 x 27) | 1, 2 | the same two layouts with one or two daises (the other places hold armoured statues) |

WHY a trial of nerve: the catastrophes happen here, so the room is built for running: a three-wide carpeted route from the door that nothing blocks, a round or long hall with room to turn, cracked blocks, chains
and stalactites that make it look as if it could come down. Pedestals stand on solid floor with free air above (the relic floats), at least four blocks apart. WHY two layouts: a sector's vault should not be the
same room twice in a row.

Checked on every template by `gen_mega_vault.py --check` (`mega_vault_kit.analyze`: sealed room, walkable route from the door to every pedestal, three-wide route, pedestals on solid ground with
free air, one marker per pedestal above it, one vault centre, no spawn marker, nothing floating, floor light), by `mega_check.py` (the connector contract), by `mega_arch_check.py` (the game's survival rules: no
candle on a slab, no lantern hanging from a top slab or a horizontal chain, no chain ending in the air) and by `mega_decor_r2.attachment_problem` (ROOMS-2's second audit): all zero problems.

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Config (`nusmp-labyrinth-mega-theme-server.toml`, section `[theme]`)

| Key | Default | Meaning |
|---|---|---|
| `crucibleEnabled` | true | false: the vault ticker does nothing and the pedestals keep their old behaviour (empty in a new vault) |
| `offerCount` | 3 (1..3) | relics per vault (never more than the vault's pedestals) |
| `minGrade` | "UNIQUE" | the lowest relic grade offered (Normal ... Legend, God or Mythical, Genesis; anything else is read as Unique) |
| `showRoads` | true | tell the party which road each relic leads to, and again when one is taken |
| `floodSeconds` | 30 (5..300) | length of the toxic magicule flood |
| `floodAmplifierPerThreat` | 0.34 | flood level = round((threat - 1) * this), at most 4 |
| `collapseSeconds` | 8 (3..60) | length of the collapse (two seconds of warning, then a stone every 1.5 s) |
| `collapseDamage` | 2.0 | damage of the first collapse stone that lands on a person (capped at 35 % of their maximum health, 60 % for the whole collapse) |
| `collapseDamagePerThreat` | 0.6 | extra damage per threat level above 1 |
| `spiritMinutes` | 10 (1..120) | the spirit is let go this long after the last player left the sector |
| `spiritHealthPerThreat` | 0.25 | spirit maximum health x (1 + this * (threat - 1)) |
| `spiritDamagePerThreat` | 0.08 | spirit attack damage x (1 + this * (threat - 1)), at most 20 |
| `ambientEnabled` | true | the theme's particles and mood sound around the players |

All plain `define` / `defineInRange` (never `defineInList`); every reader answers the default while the spec is not loaded.

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. Command: `/multiverse mega crucible ...` (permission 2; the lead attaches `CrucibleCommand.command()` under `mega`)

* `status [sector]`: the phase, the pedestals, the offered ids in slot order, the choice and chooser, the catastrophe and whether it is running, the grades offered, the road the choice leads to (theme, ruleset, name), the roster size;
* `roll [sector]`: forces the offer now (or, for a vault that is already ROLLED, re-shows it on its pedestals) without a player in the vault;
* `reset <sector>`: puts a vault back to UNOPENED (stopping its catastrophe, emptying its pedestals) and rolls a new offer, so the whole choice can be tried again;
* `catastrophe <collapse|flood|spirit>`: runs one around YOU as a test; it never resolves the Crucible and never touches a vault's saved state.

Quest key: `mega.crucible_choice` (argument: the relic id), fired on a successful pick. Lang keys: `message.nusmp.mega.crucible.*` (fragment `mth.lang.json`).

---------------------------------------------------------------------------------------------------------------------------------------------

## 11. WHY of every class and file

| File | WHY |
|---|---|
| `theme/ThemeRegistry` | the one registration entry `MegaSystem` calls: the processor type (must exist before the datapack is read), the Crucible's hooks, the theme override, the reload listeners, the self-tests |
| `theme/RegionThemeProcessor` | the brief's palette processor; harmless outside the sector generator because the sector comes from a ThreadLocal |
| `theme/ThemeSwap` | derives 31 palette targets per theme from nine family bases by suffix rules; pure so every rule is tested; also the light rule |
| `theme/ThemeTables` | one hash lookup per block; built lazily once per theme; logs each wrong id once |
| `theme/ThemeDef`, `ThemeDefs`, `JsonTrees` | a theme as data; parsed from a plain tree so the parser and the shipped files are tested without a JSON library; a reload listener so one broken file costs one theme |
| `theme/ThemeAmbient` | a sector should be felt before a block is read: particles and a mood sound, every two seconds, capped |
| `theme/ThemeConfig` | every number a player feels is a key, in a spec of its own so no builder edits another's file |
| `theme/RelicThemes`, `RelicThemeMap` | the relic taken decides the next theme: a pure rule plus the data that feeds it |
| `theme/ThemeSelfTests` | the processor sits in the middle of the generator; only the real game can show it is registered, listed and property-safe |
| `crucible/CrucibleRules`, `CatastropheRules` | "exactly one relic per region" and "every catastrophe is survivable" are pure rules with a test for every transition and clock |
| `crucible/CrucibleHook` | the line the whole Crucible stands on: CHOSEN is written first, one relic issued, all pedestals emptied |
| `crucible/CrucibleOffer`, `CruciblePedestals` | the draw without issuing, the pedestal order, the vault box |
| `crucible/CrucibleVault` | wakes the vault when a person walks in, repairs it, resolves what a restart left CHOSEN |
| `crucible/CatastropheRunner` | the three catastrophes as one small tick-driven state machine; no block changes |
| `crucible/CursedSpirits`, `SpiritRoster`, `SpiritRules` | "a massive Cursed Spirit" as a borrowed body with a new name and the boss rules |
| `crucible/NextTheme` | the answer to "what comes after this sector?", also the ruleset |
| `crucible/RelicKeywords` | word-prefix matching of theme keywords (so `ice` does not match "Voice") |
| `crucible/CrucibleCommand`, `CrucibleRegistry`, `CrucibleSelfTests` | the admin's handle; the door between the two packages; the real-game proof of the one-pick rule |
| `tools/labyrinth/gen_mega_vault.py`, `mega_vault_kit.py`, `mega_vault_view.py` | the six templates, the kit (and the checks that stand in for walking in), and a first-person viewer |
| `tools/labyrinth/mega_theme_preview.py` | the processor changes every block of a sector and nobody can see it without a game: this applies the same rules (a Python port of `ThemeSwap`, checked line by line against the Java printout for all eleven themes) to a template and renders it in every theme |

---------------------------------------------------------------------------------------------------------------------------------------------

## 12. Previews and tests

`python3 tools/labyrinth/mega_theme_preview.py mega/vault/three_b out_prefix` renders a template in each of the eleven themes (one PNG each and a sheet); `--table <theme>` / `--table-all` print the swap
table (palette block, target, how) for comparison with the Java rules. `mega_shape_preview.py` (cutaway) and `mega_walk_preview.py` / `mega_vault_view.py` (first person) render the templates themselves.

Tests:

Pure JUnit (CI runs the real JUnit; locally `jt.sh`): `ThemeSwapTest` (every suffix and fallback rule, the light rule, the palette equals `mega_lib.PALETTE`), `ThemeDefTest`, `ThemeDataTest`
(the eleven shipped files: every `RegionTheme` has one, every id is a vanilla block, every palette block has a target, themes are clearly different, light, sound and danger), `RelicThemesTest`,
`RelicThemesDataTest` (the shipped table: every key is a relic that exists, every theme is somebody's road), `CrucibleRulesTest` (every phase transition, the double click, exhaustion, the catastrophe weights,
the distinct-roads choice, the danger scale), `CatastropheRulesTest` (the clocks: warning, spacing, budget), `RelicKeywordsTest`, `SpiritRosterTest`, `RelicExclusivityMegaTest`.
In-game self-tests (`/multiverse mega selftest`): `theme.processor`, `theme.relic_count`, `theme.relic_themes`, `theme.crucible_flow`, `theme.single_relic`, `theme.catastrophe_undo`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 13. Edits to existing files (all additive; the old behaviour is the default)

* `labyrinth/vault/RelicPedestalBlockEntity.java`: `TakeHook` (a `BiFunction<ServerPlayer, RelicPedestalBlockEntity, Boolean>`), `setTakeHook`, one consultation at the top of `take`, `setTaken(boolean)`.
* `labyrinth/vault/RelicPedestalBlock.java`: `DropGuard`, `setDropGuard`, one condition in `onRemove`.
* `relics/RelicLoot.java`: `peekUnissued(...)` (new); `rollUnissuedAll` and `anyUnissued` return nothing in the Mega-Dungeon (`RelicExclusivity.allowsWildRoll`).
* `labyrinth/vault/RelicExclusivity.java`: `allowsWildRoll(...)`, a three-argument `allowsMobDrop`, `inMega` (the two-argument rules are unchanged).

The last two go beyond the brief ("RelicLoot: one new public method; RelicExclusivity untouched"): in the Mega-Dungeon a Mimic or a chest table could otherwise roll a second relic, and the owner's rule
is one per region. Everything outside the Mega-Dungeon behaves byte for byte as before.

---------------------------------------------------------------------------------------------------------------------------------------------

## 14. What the contract could add (optional)

The ruleset of a sector is derived (`NextTheme.rulesetOf`), so nothing is required. If the lead wants it stored on the sector and set by the generator:

```java
// Sector.java, next to `public RegionTheme theme`:
/** 1.55.0: the id of the ruleset the relic taken in the previous Crucible chose for this sector ("" = the theme's own rules). */
public String ruleset = "";
//   save(): t.putString("rs", ruleset);        load(): s.ruleset = t.getString("rs");
// ThemeOverride.java:   default String rulesetAfter(Sector previous) { return null; }
// MegaDungeon.java:     public static String rulesetOverride(Sector previous) { ... same as themeOverride ... }
```

`NextTheme.HOOK` already has a public `rulesetAfter(Sector)` method, which becomes the implementation of that default method without any change here.

---------------------------------------------------------------------------------------------------------------------------------------------

## 15. Not verified (honest list)

Nothing ran in the game. In particular: the Relic Pedestal's right-click flow with the hook (checked by reading `RelicPedestalBlock`); the exact rendering of the floating relic and the grade glow; every vanilla particle and
sound id (checked against the API dump, not played); the Tensura entity ids of the spirit roster (read from the owner's id list, not spawned) and the strength of those bodies (the base health was not readable, so
`healthScale` values are first guesses); the balance of the catastrophes (damage, flood level, spirit health); whether the sculk, moss, snow and copper blocks of the themes look good together; the light of the
vault templates (a model of vanilla's rule, not the game's); that the generator places the vault with the pedestal markers where the sector's `pedestals` list expects them; the previews are scratch renders from
a keyword colour table, not the game's textures.
