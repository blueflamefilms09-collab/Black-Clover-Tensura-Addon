# Mega-Dungeon v2, THEME-D: the evil road's deep and infernal places

Four complete themes for the Abyssal road, each a whole sector look and not a block swap: `FUNGAL_UNDERDARK`, `INFERNAL_FOUNDRY`, `DROWNED_CRYPT`, `SPIDER_WARRENS`.
This document is the record of the work: the sources, the spaces and props the themes are built from, what every file and every room is and why, the palettes, the
numbers, the encounter design, the pool weights (with the one real problem that the 1.58.0 pool maths had, and the lead's patch that removes it), the tests, and an honest list of what has not been
verified because nothing here has run in the game. Comments in the code say `1.59.0`; the lead sets the real number when the branches are merged.

Contents: 1 what is new and what replaces something / 2 research / 3 the four themes / 4 shared code / 5 weights and the pool maths / 6 data and tests changed outside the themes /
7 what was checked / 8 not verified / 9 notes for the lead / 10 the collisions with the other builders.

## 1. New and replacement (rule 5 of the owner)

| | |
|---|---|
| NEW | the four enum entries; four `themes/*.json`; 27 room templates (7 + 7 + 7 + 6) and their four generators; four `template_themes/*.json`; four encounter tables of 12 entries; the weights of the four themes in all 11 faction files; the lang fragment; the kit `thd_kit.py`, the viewer `thd_preview.py` and the weight calculator `thd_weights.py`; the test `TemplateThemeDataTest`; this document |
| REPLACEMENT | 20 relic assignments of `relic_themes.json` (five per theme) that used to point at `blood`, `flame`, `verdant`, `shadow` or `abyss` now point at the new themes (section 6); nothing else replaces anything |
| KEPT | every existing room, theme, table, faction mix, tool and test. The shared generated files (`template_pool/mega/side.json`, `safe.json`, `src/test/resources/mega/catalog.json`) were regenerated from the folders: they gained exactly the new templates, **and** with the current pool maths the weights of 39 of the 76 existing templates of `side` went down (the dilution of 5.1; the lead's patch of 5.2 removes it) |

Nothing was deleted. In the three edited tests lists were extended or made dynamic; no assertion was dropped.

## 2. Research

### 2.1 How it was done, and its limit

`WebSearch` was used for about twenty-five queries (D&D 5e adventure modules and setting books, Forgotten Realms, the Nine Hells, Ravenloft, Tolkien, game-design write-ups of
flooded temples, spider dens and forges). `WebFetch` failed on every call with a name-resolution error, so **no page was opened**: what the themes took are the ideas in the
text that the search returned for these results (which is a summary of each page, not the page). Ideas only were taken, never wording: every room, name and plan here is
the mod's own. The links are the results that carried the ideas; they are listed so that the owner can check them, not as a claim that each was read in full.

### 2.2 Fungal Underdark

Sources (search results): <https://en.wikipedia.org/wiki/Underdark>, <https://en.wikipedia.org/wiki/Menzoberranzan>, <https://dnd4.fandom.com/wiki/Underdark>,
<https://greyhawkonline.com/greyhawkwiki/Vault_of_the_Drow>, <https://en.wikipedia.org/wiki/Descent_into_the_Depths_of_the_Earth>,
<https://adventureaweek.com/shop/dnd-5e/dnd-one-shot-5e/mini-dungeon-228-spore-song/>, <https://www.dndbeyond.com/search?q=myconids>, <https://www.dndbeyond.com/search?q=ochre>,
<https://rpgmuseum.fandom.com/wiki/Green_slime_(Dungeons_%26_Dragons)>.

What they gave: the Underdark as a world under the world of caverns, lakes and fungus forests (Zurkhwood-style giant fungi, the Darklake); the drow city of towers cut from
stalagmites and outlined in violet faerie fire, ruled by the spider goddess; the myconid colonies that cultivate mushrooms and meld their minds in circles with spores; duergar and
goblin fungus farms; the classic ecology (gricks, ochre jellies, oozes, spiders, floating-eye tyrants); the old drow vault modules for the black-and-purple look.

Seven **spaces** became rooms: the drow house's gate hall (`drow_enclave`), the fungus forest (`spore_forest`), the fungus farm (`fungus_farm`), the myconid circle
(`myconid_circle`), narrow ledges over a chasm (`chasm_ledges`), the Darklake (`darklake`), and a myconid refuge garden (`myconid_garden`). The stalagmite towers of the drow city are
the spires inside the enclave. Five **props** repeat through the rooms: violet faerie-fire globes (stained glass round a hidden light, no vanilla particle), stalagmite spires with a
glowing tip, giant red/brown/warped mushrooms with shroomlight spots, the spider goddess with a web inlaid in crying obsidian, glow lichen and glow-berry vines.

### 2.3 Infernal Foundry

Sources: <https://nwn2.fandom.com/wiki/Nine_Hells_of_Baator>, <https://greyhawkonline.com/greyhawkwiki/Dispater>, <https://www.dndbeyond.com/sources/mtof>,
<https://www.dndbeyond.com/posts/180-the-infernal-options-in-d-ds-mordenkainens-tome-of>, <https://www.tvtropes.org/pmwiki/pmwiki.php/TabletopGame/BaldursGateDescentIntoAvernus>,
<https://slyflourish.notion.site/28-August-2020-DiA-7506ce82d9404a3485e5b3b7a5e5b7cb>, <https://www.dndbeyond.com/forums/d-d-beyond-general/general-discussion/59905-infernal-contract>.

What they gave: Baator as an industrial order, not a pit of chaos: Dis, the iron city of Dispater with its towers and forges; Avernus as a war zone of infernal war machines
and soul coins; devils that bind with signed contracts and clauses; imps, barbed and chain devils, hell hounds and pit fiends as the legion; the Hells as a ledger (hell is paid in
gold and souls).

Seven **spaces** became rooms: the grand furnace (`grand_furnace`), the penal forge (`penal_forge`), the contract hall (`contract_hall`), the pit of cages as a trial
(`cage_arena`), the soul wheel, the thing the factory is for (`soul_wheel`), the smelters' break room (`smelters_rest`) and the counting house (`counting_house`). Five **props**:
chains with hanging iron cages and skulls, lava ladles (a lava cauldron on a chain), anvils with grindstones and lit blast furnaces, gilded stone (the keystones, the vault plinths)
and lecterns with ledgers.

### 2.4 Drowned Crypt

Sources: <https://monumenta.wiki.gg/wiki/Sunken_Temple>, <https://wiki.guildwars2.com:443/wiki/Flooded_Temple>, <https://yohanebid.wiki.gg/wiki/Deepblue_Temple> (game levels of
flooded temples), plus the search summaries for flooded-crypt design (tide gates and water-level puzzles, air pockets, sarcophagi grown with weed) and for Ravenloft/Tomb of
Annihilation style drowned dead.

What they gave: the flooded temple as a level (a processional way with the water either side, galleries above it, air pockets as the places to breathe), the tide gate as a
puzzle about reading the water, kelp and coral growing in coffins, barnacle-crusted walls, a bell, a baptistery, a pearl as the treasure.

Seven **spaces** became rooms: the flooded nave (`flooded_nave`), the sarcophagus vault (`sarcophagus_vault`), the tide gate (`tide_gate`), the drowned font as a trial
(`drowned_font`), the drowned chapel (`drowned_chapel`), the air bell (`air_bell`) and the pearl reliquary (`pearl_reliquary`). Five **props**: kelp-grown sarcophagi, barnacle
walls (dead coral blocks in dark prismarine), lancet windows of blue glass lit by a sea lantern behind, the church bell on a chain, the benches of the congregation under the water.

### 2.5 Spider Warrens

Sources: <https://en.wikipedia.org/wiki/Queen_of_the_Spiders>, <https://en.wikipedia.org/wiki/Queen_of_the_Demonweb_Pits>, <https://dungeonsdragons.fandom.com/wiki/Lolth>,
<https://www.dndbeyond.com/monsters/4829053-avatar-of-lolth>, <https://en.wikipedia.org/wiki/Shelob>, <https://tolkiengateway.net/wiki/Shelobs_Lair>,
<https://tolkiengateway.net/wiki/Torech_Ungol>, <https://dwarffortresswiki.org/index.php/v0.31:Silk_farming>, <https://www.uesp.net/wiki/Oblivion_Mod:Order_of_the_Dragon/Webbed_Cave>,
<https://www.realmeye.com/compare-revisions-of-wiki-page/spider-den-guide/8>.

What they gave: the lair of one huge spider that every tunnel leads to (Shelob's lair, the Demonweb), webs as thick curtains, victims wrapped in silk and stored (the larder),
the brood and the egg sacs, drow who ride and handle spiders, silk as a building material (bridges, hammocks, hangings).

Six **spaces** became rooms: the web-filled great hall with silk bridges (`web_great_hall`), the cocoon larder (`cocoon_larder`), the queen's lair (`queens_lair`), the brood hatch
as a trial (`brood_hatch`), a spinner's burrow as the safe room (`silk_nook`) and the egg cache (`egg_cache`). Five **props**: egg sacs (white wool with a slime block bulging; never
turtle or sniffer eggs, which hatch), hanging cocoons, cocooned adventurers (armour stands behind a pane of glass), silk bridges with chains up to the roof, webs in corners and over
the heads (never on a cell a player walks through, see 3.5).

## 3. The four themes

### 3.1 What each file is and why it exists

| file | what | why |
|---|---|---|
| `labyrinth/mega/RegionTheme.java` | four lines appended after `ABYSS`: `FUNGAL_UNDERDARK(3, 3, Route.ABYSSAL)` and the other three | the contract of 1.58.0: a theme is an enum entry; entries 12 and later are exclusive to their road, so these are never drawn on the Holy road |
| `data/nusmp/mega/themes/<id>.json` | the nine block families, the explicit swaps, mood, ambient, light shift, danger, relic keywords, catastrophe weights, a `_why` | the look the processor applies to every template of the sector (including the free rooms) |
| `structure/mega/<folder>/<id>_<room>.nbt` | 27 rooms | what makes the sector a new place and not a recolour |
| `tools/labyrinth/gen_mega_theme_<id>.py` | the room generators | deterministic (every random choice is seeded by the template's name), each room checked before it is written, `--check` proves the files on disk are what the script writes |
| `data/nusmp/mega/template_themes/<id>.json` | the prefixes the rooms are restricted to, and their `relative` weights | the assembler may draw a themed room only in a sector of its theme; the weights decide how often |
| `data/nusmp/mega/encounters/<id>.json` | 12 entries per theme | what lives in the place, in the four bands |
| `data/nusmp/mega/factions/*.json` | the four theme weights appended to `themeWeights` of all 11 factions | `FactionDataTest` requires every faction to weigh every theme; a missing weight is 1 in the game, which would make the new themes' native factions arbitrary |
| `data/nusmp/mega/relic_themes.json` | 20 re-pointed relics, four ruleset names, a sentence in `_why` | `RelicThemesTest` requires that a relic of the minimum grade leads to every theme |
| `template_pool/mega/side.json`, `safe.json`, `src/test/resources/mega/catalog.json` | regenerated by `gen_mega_pools.py` and `mega_catalog.py` | generated files; the lead regenerates once more after the merge |
| tests (section 6) | | |

### 3.2 Palettes (the nine slots of `ThemeSwap`) and why

The templates are written in the deepslate palette; the processor swaps the families per sector. Each theme had to be recognisable from the existing eleven (`shadow`, `blood`,
`flame`, `abyss` ... are the near neighbours), so each file says in its `_why` what it must not be mistaken for.

| slot | fungal_underdark | infernal_foundry | drowned_crypt | spider_warrens |
|---|---|---|---|---|
| brick (walls) | packed mud | red nether bricks | dark prismarine | polished blackstone |
| tile (floors, ceilings) | polished blackstone | deepslate tiles | mossy stone bricks | cobbled deepslate |
| polished (pillars, trim) | obsidian | polished blackstone bricks | deepslate bricks | calcite |
| chiseled (keystones) | crying obsidian | gilded blackstone | prismarine bricks | chiseled polished blackstone |
| cobbled (rubble) | blackstone | nether bricks | cobbled deepslate | tuff |
| cracked brick | mud bricks | cracked nether bricks | cracked stone bricks | cracked polished blackstone bricks |
| cracked tile (worn floor) | mycelium | cracked deepslate tiles | dead tube coral block | white wool |
| rock (fill) | smooth basalt | blackstone | dripstone block | smooth basalt |
| wood | warped planks | dark oak planks | spruce planks | dark oak planks |
| lights | cold (soul lanterns, soul torches) | unchanged (warm and soul together) | cold | unchanged |
| light shift / danger | -2 / 1.2 | +1 / 1.25 | -2 / 1.15 | -2 / 1.25 |
| ambient particle | `warped_spore` | `smoke` | `dripping_water` | `item_cobweb` |
| mood sound | warped forest whispers (0.5, 0.7, every 5) | blast furnace crackle (0.45, 0.7, every 3) | conduit hum (0.4, 0.6, every 5) | spider ambient (0.3, 0.5, every 8) |
| catastrophes (collapse / flood / spirit) | 1 / 2 / 3 | 2 / 1 / 3 | 1 / 4 / 3 | 2 / 1 / 3 |

Why these: the Underdark is brown packed mud (with patches of old mud brick showing through where the wall is worn, and mud brick stairs, slabs and wall posts) and black stone with
living mycelium where the floor is worn (the fungus is the wear); it first had mud bricks as walls, which the Feywild Grove of the Holy road also has, so the walls became packed mud; the Foundry is the red of hot iron on dark iron plates
with gold on every keystone (hell is paid for); the Crypt is wet green-grey stone with a barnacle crust where the floor is worn; the Warrens are black chitin with white silk, the
only theme with white pillars against black walls and with white wool as its wear. Obsidian, calcite and packed mud have no stairs, slabs or walls of their own, and dark prismarine has no
wall, so the `explicit` maps give their shapes polished blackstone, prismarine or mud brick ones. Light: the light shift never takes a light block under level 6 (the gloom clock punishes block
light of 4 or less), and every room is checked on the **swapped** template, so the soul lanterns of the cold themes are what the checks see. The relic keywords (spore, fungi, drown,
kelp, devil, contract, spider, silk ...) are words that real relics of the mod contain or evoke. They were checked against all 27 theme files of the four builders: none is listed twice
(`anvil` of the Dwarven Forgehold and `brine` and `siren` of the Sunken Temple were dropped from the Foundry and the Crypt), and no keyword of mine is the beginning of one of another theme
except `krake` (the Crypt's, for the Krakeblade) in front of the Abyss's `kraken`. `drow` and `lichen` are not keywords: a keyword matches the beginning of a word, so `drow` would catch
`drown`, `drowning` and `drowsy`, and `lichen` the `lich` of the Necropolis.

### 3.3 Fungal Underdark: the seven rooms

| room | kind, footprint | what it is | why |
|---|---|---|---|
| `drow_enclave` | hall, 27 x 36, two storeys | the gate hall of a drow house: portico, a nave with a spider web inlaid in crying obsidian, four stalagmite spires, a U-shaped gallery on arches reached by two ramps, violet globes, the spider goddess on a dais between soul braziers | the big multi-storey hall of the theme and the place the party remembers the black-and-purple look from |
| `spore_forest` | cavern, 45 x 36 | colossal red, brown and warped caps (up to 8 across, 18 rows high, the biggest flat umbrellas) closing a canopy over a mycelium floor; glowing glass spore clouds, a pond, roots, glow berries | the "forest of huge mushrooms" of the brief; lit from above |
| `fungus_farm` | hall, 27 x 27 | four terraced beds (red on mycelium, brown on podzol, warped on nylium, a processing corner), two overseers' lofts, grow-lamps of shroomlight on chains | the calm-looking hall: low and raised props, nothing tall, colours of crops against black stone; strongly different from the enclave |
| `myconid_circle` | set piece, 45 x 45 | a domed round clearing: twelve elder mushrooms in a ring whose caps touch, a lawn, a pit with stairs on the four axes and toadstool stools, a great glowing spore cloud with six coloured companions | the set piece: the meld of the mushroom folk |
| `chasm_ledges` | trap, 27 x 27 | a round chasm 15 across with a four-wide walk round it (the safe way), an island with the chest reached by a one-block ledge; a fall is four blocks into a dark lake with a stepped shore | the danger room that is not a fight: costs time and a soaking, never a life, never leaves anyone stuck; three `trap;type=pit` markers for the Dungeon Master |
| `darklake` | cavern, 36 x 36 | a lake 17 across and 4 deep, a dike three wide across it, a spur to an island with an obelisk, chest and candles; clouds over the water | the Darklake; a swimmer can always climb out |
| `myconid_garden` | safe, 18 x 18 | a refuge under a flat umbrella cap held by four stems: a soul fire in a ring with four seats, three cots, glow crops, shroomlight | the one warm room of a cold theme; four seats, one hearth, three beds, no spawn, no faction |

### 3.4 Infernal Foundry: the seven rooms

| room | kind, footprint | what it is | why |
|---|---|---|---|
| `grand_furnace` | hall, 27 x 36, two storeys | a colossal furnace stack of red brick whose base and shaft hold lava behind orange glass; an iron-grate gantry round the shaft hung by four chains, bridges to two side platforms, long stairs along the walls, forge bays below (lit blast furnaces, anvils, grindstones), ladles and cages on chains | the big multi-storey hall and the whole theme in one picture |
| `penal_forge` | hall, 27 x 27 | a lava trough down the middle (carved from the slab, so it cannot flow) crossed by two grate bridges; cages and ladles on chains over it; branding bays on the west, the overseers' armouries on the east | the brief's torture/penal forge as a working line, not a third torture chamber |
| `contract_hall` | puzzle, 27 x 18 | a long table with the great contract (a writable book on a lectern), four sigil plates (red, blue, gold, green), a Riddle Stone by the door, clause lecterns along the wall, the vault of the bargain behind a puzzle door | "lecterns with books: a puzzle that is a bargain"; the order is blue, green, red, gold |
| `cage_arena` | trial, 27 x 27 | a round floor with a gilded seal, eight iron holding pens at radius nine each with a spawn glyph, hanging cages, soul braziers, the party's chest in a vault alcove | the "pit of cages" as the fair fight (Sealed Archway, wave from the glyphs) |
| `soul_wheel` | set piece, 36 x 36 | a wheel 23 across on its edge against the north wall, half sunk in a river of lava, twelve spokes of bars, a gilded hub with a glowstone eye, soul lanterns in the rim; a six-wide grate bridge to an island with the chests; a rune circle, soul stills and cages in the south half | the place the party remembers: the thing the factory is for |
| `smelters_rest` | safe, 18 x 18 | the break room: a campfire in a ring of plates with four benches, a kettle on a chain, three bunks, the cold banked forge, workbenches, ale barrels | the one room with no lava, cage or skull; the rest anchors |
| `counting_house` | treasure, 18 x 18 | a counting hall (counter, ledgers, a great iron balance) and, through a barred arch, the vault: three chests on gilded plinths, gold stacks, two golden stands | in the Hells the hoard is a bank; guards in the vault |

Lava is only ever put in cells carved from solid slab, every side is a full block, and nothing flammable stands within three blocks of it (the generator proves both; the
walkways are iron and the brick is nether brick).

### 3.5 Drowned Crypt: the seven rooms, and the rules of the water

| room | kind, footprint | what it is | why |
|---|---|---|---|
| `flooded_nave` | hall, 27 x 36, two storeys | a basilica the sea took back: a five-wide processional way with the flood either side at the level of its floor, benches under the water, choir galleries on two decks reached by flights of eight steps, eleven lancet windows of blue glass, a rose window, six chandeliers on long chains | the big multi-storey hall; a swimmer climbs out anywhere |
| `sarcophagus_vault` | hall, 27 x 27 | the crypt under it: a cross of five-wide lanes, eight sarcophagi (four open on a trough of kelp), twelve pillars, burial niches, stalactites dripping over the open coffins, barnacles | the low, dripping hall: nothing like the nave (tall, open, lit from above) |
| `tide_gate` | puzzle, 27 x 18 | four tide pools of depth 1, 2, 3 and 4 over beds of gold, red, green and blue, a sigil plate of each colour, a Riddle Stone, a well, and the Tide Gate (the mod's puzzle door) with a treasure closet | "tide gates that flood and drain": nothing in a template ticks, so the tide is made a thing to read: press the plates from the shallow pool to the deep one (gold, red, green, blue) |
| `drowned_font` | trial, 27 x 27 | the baptistery: a round font (7 across, 3 deep, a sea lantern on its bed) with a curb, eight glyphs on a ring from which the drowned climb out, pillars for cover, an alcove with the party's chest | the fair fight; nobody falls into the font by accident |
| `drowned_chapel` | set piece, 45 x 45 | a rotunda 41 across under a ribbed dome, flooded to the floor level; a cross of causeways to a round dais with a font under the church's bell on a long chain; the north arm climbs to the altar, the saints, the chest and a rose window; twelve pillars, twelve lancets, the benches on the bed | the place the party remembers |
| `air_bell` | safe, 18 x 18 | the air pocket: dry, with a glass wall along its north side and behind it a lit tank of flood with a living reef; a campfire with four benches, three bunks, a chart table, a diver's suit on a stand | the safe room in a crypt is the place to breathe; the glass is whole blocks, so it holds the water |
| `pearl_reliquary` | treasure, 18 x 18 | a dead end whose floor is a pool four deep, a causeway to a round island with a giant clam of white calcite, two chests either side of a pearl (a sea lantern on a plinth of bone) | the thing the sea keeps best is a pearl |

The rules of the water, which hold in every room: a water source flows into any open neighbour, so a flooded area is a basin carved out of a solid slab (rows 1 to 3 are rock, the
water fills rows 4 to the floor row); every side and the bed of every water cell is a full block or more water; stairs, bars and chains that stand in water would have to be
waterlogged and the lead's arch check refuses a waterlogged block next to water, so the flood holds only full blocks (benches are plank blocks, bed growth is moss, dead coral, bone
and sea lanterns); no plant stands next to plain water (kelp grows only in sealed troughs and in the sealed tanks); no water touches a doorway; and **every walking surface is dry**
because the lead's walking model counts a water cell as a wall: the party walks on causeways, galleries and dais, the flood is what lies between them, and the surface is level with
the causeway so a swimmer can always climb out. The brief said "shallow water in corridors": the free corridors are not mine to change, and a 1x3 corridor with gutters was
considered and left out (see 9).

### 3.6 Spider Warrens: the six rooms, and the rules of the silk

| room | kind, footprint | what it is | why |
|---|---|---|---|
| `web_great_hall` | hall, 27 x 36, two storeys | six calcite pillars wrapped in black bands, a nine-wide central lane and two cross lanes; flights of nine steps up to two nest platforms, a five-wide silk bridge between them over the north door and a spine running south to a round nest with the hoard; webs in the roof, egg sacs on the walls, four cocooned adventurers | the big white-and-open multi-storey hall with the silk bridges of the brief |
| `cocoon_larder` | hall, 27 x 27 | a cross of five-wide lanes and in each quarter a block of four 3 x 3 x 4 wool cocoons (six with a victim inside behind a glass window, one with a gap and a chest), bundles hanging from the roof, valances of web over the quarters | the low dense pantry: nothing like the great hall |
| `queens_lair` | set piece, 45 x 45 | a round chamber under a low ribbed dome; the brood, a silk ball 7 across with eggs showing, hangs from the roof and eight great black legs with white joints arch out and down to the rim: the room stands inside the cage of a spider that is not there (the Dungeon Master puts the queen at the centre); a mound of three terraces with a bone throne and two chests | the place the party remembers |
| `brood_hatch` | trial, 27 x 27 | a mat of silk in rings of stone, eight brood pods of wool at radius nine with a slime-block egg on each and a spawn glyph in the floor, calcite pillars for cover, the party's chest in an alcove | the fair fight: the brood comes out of the walls |
| `silk_nook` | safe, 18 x 18 | a spinner's burrow: a campfire in a ring of bone with four benches, three white hammocks, a loom and a lectern, silk hangings, glow-berry vines | the only room where the silk is a blanket and not a trap |
| `egg_cache` | treasure, 18 x 18 | a hill of wool crowded with slime-block eggs and a few verdant froglights, a lane to the hoard (two chests between white candles and bones) | what a spider keeps is eggs, and what it keeps them round is whatever the last party carried |

The rules of the silk: a cobweb slows whoever stands in it and the lead's walking model counts it as free air, so a web in the feet cell or the head cell of any cell a player can
reach is invisible to every other check. The kit's `web_problems` refuses any such web (it found a real one in the fungal chasm room, which was moved up). Webs are scenery in
corners, over the heads, between pillars and inside sealed hollows of cocoons, never across a way. Egg sacs are wool and slime: turtle and sniffer eggs hatch and are not safe decor.

### 3.7 Encounter tables

Each table has 12 entries (the test needs 8 to 14), all four bands (3 minor, 4 standard, 3 hard, 2 ambush), and ids unique across all tables (prefixes `fu_`, `if_`, `dc_`, `sw_`).
Budget tuning: `rollBonus` 1 / 1 / 2 / 2 (the Abyssal road is already tough, the Crypt and the Warrens are crowded and slow), `budgetScale` 1.2 / 1.3 / 1.1 / 1.3, `defaultFaction`
slime / demon / aquatic / insect. Entity ids are vanilla, the `tensura:*` ids already in the existing tables, `nusmp:living_armor`, or a faction.

* **Fungal Underdark**: spore drifters (slime), gricks (silverfish), cap crawlers (cave spiders); a drow patrol (cultists with spiders), a myconid meld, a web drop from the caps
  (black spider and cave spiders, hunting), a floating eye (a shulker with vexes, only in the dark); an ooze flood, a drow house (cultists and insects), a beholder court (shulkers,
  an evoker and vexes, only in the dark); a spore storm and a spider court (knight spiders, spiders and cultists, in the dark, threat 5).
* **Infernal Foundry**: an imp swarm (vex), slag crawl (magma cubes), a chain gang (wither skeletons); yard hounds (`tensura:barghest`), overseer guard (living armour with a piglin
  brute), a shift crew (demon faction), cinder imps (blaze and vex); contract enforcers, a brute crew (only when noisy), an iron legion (hunting); a forge alarm and a blood war
  (demon against undead, threat 5).
* **Drowned Crypt**: the drip dead (drowned), tide scouts (aquatic), bone shallows (skeletons); a drowned pack, a lizard patrol, crypt walkers, a siren call (a witch with the drowned,
  only in the dark); a flood call, grave knights (living armour with skeletons, hunting), a bell toll (undead faction with vexes, only when noisy); a black tide and a wreck crew.
* **Spider Warrens**: hatchlings (cave spiders), web scouts, wasp drones (`tensura:army_wasp`); a weaver pack, a ceiling drop (black spider, hunting), silk sentries (knight
  spiders), a drow handler (cultists with spiders, in the dark); a brood wave, the queen's guard, a wasp nest (giant ants and wasps, only when noisy); a web trap and the queen's
  court (knight spiders, black spiders and cultists, in the dark, threat 5).

Cues use sounds and particles from the lead's known-id list; the 36 messages are in the lang fragment (9 per table, key `message.nusmp.mega.enc.<table>.<id>`).

Faction mixes (`themeWeights`, the weight of a faction in a sector of the theme; 0 = never native there):

| faction | fungal | infernal | drowned | spider |
|---|---|---|---|---|
| aquatic | 2 | 0 | 5 | 0 |
| beast | 2 | 1 | 1 | 2 |
| construct | 1 | 3 | 2 | 0 |
| cultist | 4 | 3 | 2 | 3 |
| demon | 1 | 5 | 0 | 1 |
| elemental | 1 | 3 | 1 | 0 |
| goblin | 2 | 1 | 0 | 1 |
| insect | 4 | 1 | 1 | 5 |
| ogre | 1 | 3 | 0 | 1 |
| slime | 5 | 2 | 2 | 1 |
| undead | 1 | 2 | 5 | 2 |

## 4. Shared code

* `tools/labyrinth/thd_kit.py`: the one place the four generators share. Safe placement (`put`, `box`, `clear` never overwrite architecture or a doorway), giant mushrooms with the
  right face states, spires and stalactites, glow spheres (stained glass round an invisible light block: a spore cloud or a faerie-fire globe without a vanilla particle), web
  scenery, markers placed by farthest-point sampling on reachable wide floor, invisible-light filling of dark pockets judged on the swapped template, the water helpers (`under_floor`,
  `flood`, `kelp`, `lancet`), `web_problems`, and **one checking entry point** `check_all` that runs every offline check of the lead's tools (the rooms2 analysis, the arch survival rules
  on the raw and the swapped template, the multi-block pairing, the shared light report) before a room is written. It also has the command line the four generators end with
  (`--check`, `--no-write`, `--force`, `--list`, names).
* `tools/labyrinth/thd_preview.py`: draws a finished template in the colours of its theme (the same block swap the processor applies) with the lead's shape and walk viewers.
* `tools/labyrinth/thd_weights.py`: reproduces the pool arithmetic of `gen_mega_pools.py` from the real files (section 5).

## 5. Weights and the pool maths

### 5.1 How the pool weight is computed, and the trap in the 1.58.0 generator

`gen_mega_pools.py` of 1.58.0 gave a template the weight `round(share x scale x relative / sum of the relative weights of its folder)`, clamped to 1..150, with `scale` 20 and the
`side` pool's folder shares corridor 5, junction 3, hall 3, trap 1, puzzle 1, trial 1, treasure 1, cavern 1, set 1, safe 1 plus an empty element of weight 2; every theme's `relative` map
was merged into every pool. The sum counted the relative weight of **all** templates of the folder, restricted ones included, so every theme that put rooms into a folder shrank the
weight of every free room of that folder **in every sector**, including the sectors that can never draw the themed rooms. On this branch, with the generator as it was, the regenerated
`side` pool showed it in whole numbers (total weight of the free templates of each folder, before the four themes and after):

| folder | hall | cavern | trap | puzzle | trial | treasure | set | safe | corridor | junction |
|---|---|---|---|---|---|---|---|---|---|---|
| before | 63 | 21 | 21 | 20 | 20 | 21 | 20 | 18 | 97 | 61 |
| after (1.58.0 arithmetic, these four themes only) | 28 | 10 | 9 | 6 | 4 | 5 | 4 | 6 | 97 | 61 |

39 of the 76 existing templates lost weight (all 28 free halls weighed exactly 1 afterwards, they had weighed 1 to 4), and with the twelve other themes of the plan added the same way a
neutral sector kept 6% of the relative mass of its halls (`thd_weights.py --legacy`). The lead's patch of 1.59.0 (`tha_pools_absolute.patch`, written by the builder of the Holy road) removes
the dilution, and the weights of the four `template_themes` files are written for it.

### 5.2 The arithmetic with the lead's patch, and the values chosen

With the patch a room that a `template_themes` file claims is drawn with **the weight written in that file, as it stands** (clamped to 1..150) and takes nothing from the folder's share;
the free rooms of the folder split the share by their own relative weights alone. So the free rooms weigh the same in every sector, whatever the number of themes, and a themed room is
compared with the free rooms it competes with. The values follow the other builders: **60 per themed hall, 12 for a set piece or a safe room, 16 for a trap, puzzle, trial, treasure or
cavern room.** `thd_weights.py` (default mode) reproduces the patched generator exactly: it was checked against a scratch copy of the generator with `tha_pools_absolute.patch` applied, 0
differences over the 103 templates of `side` and the 10 of `safe`. Signature weight against the free weight of the same folder, scale 20 (the configured scale):

| theme | hall (two) | trap | cavern (two for fungal) | puzzle | trial | treasure | set | safe | share of all `side` draws / of its room draws |
|---|---|---|---|---|---|---|---|---|---|
| fungal_underdark | 120 / 63 | 16 / 21 | 32 / 21 | | | | 12 / 20 | 12 / 18 | 35% / 48% |
| infernal_foundry | 120 / 63 | | | 16 / 20 | 16 / 20 | 16 / 21 | 12 / 20 | 12 / 18 | 35% / 48% |
| drowned_crypt | 120 / 63 | | | 16 / 20 | 16 / 20 | 16 / 21 | 12 / 20 | 12 / 18 | 35% / 48% |
| spider_warrens | 120 / 63 | | | | 16 / 20 | 16 / 21 | 12 / 20 | 12 / 18 | 33% / 46% |

So a themed sector draws about half of its rooms from the theme's own set (46 to 48%), the target of the brief. The halls are 66% signature, a single room of another kind is 37 to 44% of its folder (the two caverns of the Underdark 60%), and
the folders in which a theme has no room are filled by the free rooms.

## 6. Data and tests changed outside the themes

* `relic_themes.json`: five relics per theme, so that a relic of the minimum grade leads to every theme. **Every one is Unique or above except one Rare charm**, because the Crucible only
  offers Unique-or-above relics (minGrade UNIQUE): a theme whose relics were all lower could never be reached from a vault. Infernal Foundry: `aegis_of_the_abyss` (Legend, was abyss),
  `null_blade` (Legend, abyss: it eats maximum health for a ward, a bargain), `hellfire_gauntlet` (Legend, flame), `the_reaver` (Unique, blood: a bow forged by a demon lord), `wallet_of_the_slayer`
  (Legend, blood: the Hells pay in coin). Fungal Underdark: `helm_of_whispers` (Legend, shadow), `the_forgotten_ring` (Legend, shadow), `darkened_cloak_of_elvenkind` (Unique, shadow),
  `nightstalkers_mantle` (Unique, shadow: echolocation and a shriek), `slime_armour` (Unique, abyss: no grip or web holds you). Drowned Crypt: `jellyfish_cap`, `krakeblade`, `wavebreaker` (Unique, all
  three abyss), `shadow_robes` (Unique, shadow: a chill and a necrotic cloud) and `the_soul_stone` (God, shadow: it keeps the dead). Spider Warrens: `cloak_mimic_tame`, `junglethorn_whip` (verdant),
  `the_dagger_of_many_daggers` (shadow; Unique), `needler` (Legend, abyss) and `weaversong` (Rare, in no table before: the weaver's song, a charm that walks three weaverlings at your side).
  Four ruleset names: "The Spore Dark", "The Iron Contract", "The Drowned Hours", "The Silk Road". The first version also took relics that the lead has since given to the Abyssal themes of the
  Archive builder (`devil_contract`, `the_amulet_of_lost_souls`, `pendant_of_echoed_souls`, `urn_of_black_pudding`, `orb_of_plagues`) and `dryads_wedding_band`, which the Feywild Grove claims: all six are
  back at their old entry and replaced. Compared with the relic tables of the other builders and with main's table (each diffed against the base commit): none of my 20 is claimed by anyone else. A
  simulated merge of main's current table with mine leaves every one of the 27 themes at five or more explicit relics and at two or more of Unique or above (mine: 5 / 5, 5 / 5, 5 / 5 and 5 / 4;
  `abyss` 6, `shadow` 7, `blood` 5, `flame` 6, `verdant` 8).
* `ThemeDataTest`: `SHIPPED` gets the four ids, the size assertion (it said 11) uses `SHIPPED.size()`, and the whitelists get the particles `dripping_water`, `item_cobweb` (both in the lead's
  known-id list) and the sounds `ambient.warped_forest.additions`, `block.blastfurnace.fire_crackle`, `block.conduit.ambient`, `entity.spider.ambient`.
* `RelicThemesDataTest`: `SHIPPED` gets the four ids.
* `EncounterDataTest`: the list of themes was written out and every theme builder had to edit it; it is now read from `RegionTheme.values()`, so a theme appended to the enum without
  a table fails here. The method that asserted "twelve tables" asserts `1 + RegionTheme.values().length`.
* `TemplateThemeDataTest` (new): every `template_themes/*.json` names a theme of the enum and says so in its own name; every prefix is a kind folder, starts with the theme id and an
  underscore and matches at least one template on disk; every template whose file name starts with a theme id is claimed by that theme's file (an unclaimed signature room would be
  free for every theme); every `relative` key belongs to a claimed prefix, every claimed prefix has a weight, 1..1000; a theme with rooms has at least five and all are in the
  generated catalog (so the catalog is not stale); no prefix is claimed by two themes.
* `FactionDataTest` needed no edit: it failed until the four weights were added to all 11 faction files, which is the point of the test.

## 7. What was checked

Offline, in this worktree, nothing in the game:

* every generator: `--check` (builds in memory, runs `check_all`, compares with the file on disk): 27 of 27 ok and byte-stable;
* `check_all` per room, on the theme-swapped template: doors reachable, 3-wide routes between all doors, markers on reachable standing cells, one faction marker, at most 16
  armour stands, light, no leaks into the void, fluids, flammables near lava, attachment rules, cobweb on the route, the multi-block pairing. Light, in numbers: the lead's shared light
  report finds the darkest standing cell at block light 5 or more in every room (6 or 7 in the four safe rooms) and 0.0% of standing cells at 4 or less; the independent rooms2 model
  finds at most 4.1% in `chasm_ledges`, 3.5% in `drowned_chapel`, 2.8% in `drowned_font`, 1.1% in `tide_gate`, 1.0% in `sarcophagus_vault` and nothing worth naming elsewhere, all
  under its 12% limit;
* `mega_check.py` (the contract of every template and pool): 149 templates, 15 pools, 0 errors, 2 warnings (both on the lead's own `landing_b` and `hearth_hall_a`: polished
  blackstone outside the palette); `mega_arch_check.py <id>` for each of the four ids (survival rules: support, floating blocks, open water, armour stands): 7, 7, 7 and 6 templates,
  0 problems;
* `gen_mega_pools.py --check` (0 pool files stale) and `mega_catalog.py --check` (current: 149 templates, 15 pools);
* the unit tests that run without the game, with the JUnit shim, in two runs, one process at a time: `ThemeDataTest`, `RelicThemesTest`, `RelicThemesDataTest`, `EncounterDataTest`,
  `FactionDataTest` (80 passed, 0 failed) and `MegaContractTest`, `MegaCatalogTest` (200 seeds assembled and audited, 0 fallback layouts), `SectorRequestsTest`, `TemplateThemesTest`,
  `TemplateThemeDataTest` (32 passed, 0 failed). `EncounterDataTest` skips the translation check of the four new tables until the lang fragment is merged into `en_us.json`, as designed;
* the same data tests again after the collision fixes of section 10 (80 and 32 passed, 0 failed), and the rules of the cross-builder tests replayed by script over all 27 theme files of the
  four builders (the base eleven, mine, and the trees of the Feywild/Crystal/Dwarven/Sunken, Church/Court/Citadel/Monastery and Archive/Manor/Necropolis/Castle builders, read only): every pair
  of themes differs in at least four of the nine slots and in its walls (closest to mine: fungal 7 of 9 against the Cursed Archive, infernal 6 against Gravity and Shadow, drowned 7 against Frost
  and the Vampire Castle, spider 6 against Shadow); no relic keyword is listed twice; the keyword-leads-to-its-theme rule holds for every theme; no relic is claimed by two builders where one
  of the claimants is mine; no encounter entry id and no lang key occurs twice across the 28 tables and the four fragments (and none is already in `en_us.json` with another text);
* a mutation check of the new `TemplateThemeDataTest`, on a scratch copy of the data (nothing in the tree was changed): a typo in a prefix, a prefix without a weight and a theme that
  stops claiming its halls each make the matching tests fail with a message that names the file, and the unmutated copy passes 6 of 6; `pyflakes` is clean on the Python tools;
* every room was rendered (isometric south-east and north-west cutaways, plans, first-person views) with `thd_preview.py` and looked at, and fixed where it showed a defect
  (blocked routes, floating blocks, rooms too dark to see the build). The pictures are in the scratchpad folder `previews/thd/`, prefixes `fu_`, `if_`, `dc_`, `sw_`.

Defects the checks found and that were fixed in the rooms: a soul-wheel route blocked by stills; a wheel built two cells off the wall; waterlogged pews and sea pickles refused by the
arch check; kelp next to plain water; signs standing in doorway holes; font corner pools and braziers blocking a 3-wide route; a chapel door behind the altar; a cocoon in a
doorway; a throne floating over a terrace gap; and a real cobweb on a walkable cell.

## 8. Not verified

Nothing here has run in the game, and the in-game self tests (`ThemeSelfTests`, `EncSelfTests`, `GuardSelfTests`) need Minecraft classes and could not run. Specifically:

* **Water and lava in a placed template.** The checks prove every fluid cell is closed by full blocks or more fluid; they do not prove how the placement order, the processors and the
  fluid ticks behave in a real chunk. A flood that leaks would be visible at once in the Drowned Crypt rooms and in the lava of the Foundry.
* **Light.** The numbers come from the lead's offline light model (a port). Sea lanterns under water lose one level per water cell; froglights, shroomlight, crying obsidian and the
  hidden light blocks are modelled by their vanilla levels. If the game disagrees, a room may be darker than the report says.
* **The puzzles.** The contract hall and the tide gate use the mod's sigil plates, Riddle Stone and puzzle door with the colour order given in the room table; the Riddle Stone's
  `sequence` semantics and the stone's scan box were taken from the lead's puzzle check and the existing rooms, not seen working.
* **Trial rooms.** The spawn glyphs are in the floor of the pens, pods and the font ring, and the wave depends on the Sealed Archway's roster; whether drowned and skeletons come out
  of the font as intended, and how the party's anchor works in a round room, is untested.
* **Armour stands behind glass** (the cocoon victims) and the bell block hung on a chain: the arch check accepts them; the look in the game is unknown.
* **The assembler's behaviour with these weights** (section 5): the draw frequencies are arithmetic, not observed; `TemplateThemes` restricts the rooms by prefix, and a theme with
  no valid room for a socket falls back as the other themes do.
* **The mob ids.** All ids exist in the lead's known-id list, but how a `tensura:barghest`, a knight spider or a lizardman behaves in water, around webs or beside lava was not
  tried. The beholder-like "floating eye" is a shulker with vexes (and an evoker for the court): no better floating mob was found among the known ids.
* **Web research** is second-hand (section 2.1).
* The `.flavour` lang keys are not read by any Java class yet; they are there because the brief asked for a flavour line per theme.

## 9. Notes for the lead

* The brief says a themed corridor or junction "is welcome too". None was built: it is optional, and the lead asked for the work to be wrapped up. A design is ready if wanted: one 1 x 3
  straight per theme with the lead's sockets (n and s at offset 4 with selection priority 1, e and w at 13), interior x 2..6, height 6, features only in the two edge columns and over
  head height so the 3-wide route stays clear (toadstools, bracket fungi, glow lichen and violet windows for the Underdark; furnaces, chains and lava ladles for the Foundry; a dry causeway
  with a flush gutter of water each side, bridged at the side doors, for the Crypt; wool bundles and webs overhead for the Warrens). A draft of the Underdark one was written with the kit
  and passed `check_all` (3-wide routes between its four doors, light 6 or more); it was not looked at, so it was removed again rather than shipped unseen. Each takes about half an hour
  with the kit.
* `tha_pools_absolute.patch` is what the weights of the four `template_themes` files are written for (5.2); with the unpatched 1.58.0 generator the same numbers would be relative ones and the
  free rooms would be starved (5.1). `tools/labyrinth/thd_weights.py` shows both.
* **Two existing tests fail on this branch until the Guardian knows the four themes**: `GuardianScalingTest.everyThemeHasItsOwnTrait` (a theme that is not in its switch must carry its own
  name as the trait) and `themeColoursAreDistinctRgb` (every theme needs its own colour in `GuardianScaling.themeColor`). The patch `thd_guard.patch` (next to the lang fragments) gives
  `fungal_underdark` SHADOW (the dark of the Underdark blinds), `infernal_foundry` FLAME, `drowned_crypt` GRAVITY (the undertow pulls; also the default) and `spider_warrens` VERDANT (webs
  root), with the colours of the theme files; applied to a scratch copy it makes `GuardianScalingTest` pass 10 of 10. That class and its test are the lead's, so nothing was changed in the tree.
* The edits of this branch to shared files (the enum, the eleven faction files, the relic table, the three theme tests and the Guardian) are the lines every theme builder touches, so a textual merge
  conflicts on all of them. They are also kept as one patch against main (`thd_shared_edits.patch`, `git apply --check` clean on main at 1.60.0 WIP c15bda0) and as an idempotent script
  (`thd_merge_into.py <tree>`, which puts the same edits on whatever the target looks like and copies the files that belong to the four themes alone); both sit with the lang fragments in the
  scratchpad folder `arc/frag/`. Tried on a scratch export of main with the branch merged that way: the theme, relic, encounter, faction, guardian, contract, catalog, sector and template theme tests
  pass (138 and 42 tests, 0 failed, the lead's new rule that every theme owns a relic of Unique or above among them) and `mega_check.py` reports 235 templates, 20 pools, 0 errors.
* Re-run `gen_mega_pools.py` and `mega_catalog.py` after the merge (they are generated).

## 10. The collisions with the other three builders, and what changed for them

After the first delivery the lead merged the Feywild/Crystal/Dwarven/Sunken themes and pointed at the other builders' trees. Compared against all three, the following changed in this branch (and
nothing else collided):

1. **Walls.** The Underdark and the Feywild Grove both had mud bricks as walls, which `ThemeDataTest.everyThemeIsClearlyDifferentFromEveryOther` refuses. The Underdark's walls are now packed
   mud, with mud bricks as the worn patches and as every stair, slab and wall post (the `explicit` map); the room checks and the previews were run again on the new walls.
2. **Relic keywords.** `anvil` (Dwarven Forgehold) left the Foundry; `brine` and `siren` (Sunken Temple) left the Crypt. In the Underdark `drow` (it also begins `drown`, `drowning`, `drowsy`)
   and `lichen` (it begins with the Necropolis's `lich`) were replaced by `fungi`.
3. **Explicit relics.** `dryads_wedding_band` (claimed by the Feywild Grove) went back to `verdant`, and the five relics that the lead has given to the Abyssal themes of the Archive builder went
   back to their old entries; the replacements are listed in section 6. Every theme of mine has at least two relics of Unique or above (the Crucible offers nothing lower).
4. **Pool weights.** The `relative` numbers of the four `template_themes` files are now the absolute weights of the lead's patch: 60 per hall, 12 per set piece and safe room, 16 for every other kind.
5. **Entry ids and lang keys** were already unique over all four builders (prefixes `fu_`, `if_`, `dc_`, `sw_`).
6. The Guardian patch of section 9, the theme colours and the traits are listed there and in the report.
