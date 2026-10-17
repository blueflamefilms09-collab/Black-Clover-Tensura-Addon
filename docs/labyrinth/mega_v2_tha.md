# The good road, four more themes (builder THEME-A `tha`, 1.59.0)

Part of the Mega-Dungeon second edition (`docs/labyrinth/mega_v2.md` section 5, `docs/labyrinth/mega_theme.md`). This builder owns four complete themes of the Holy road:
**Holy Church** (`holy_church`), **Celestial Court** (`celestial_court`), **Sky Citadel** (`sky_citadel`) and **Mountain Monastery** (`mountain_monastery`). Each is a whole
sector look, not a block swap: a palette file, an encounter table, signature rooms that no other theme can draw, the relics that lead to it and its language entries.

**State of this document.** Written without a game. The rooms are written by Python generators and walked by the static checkers of the repository; the data files are read by
the repository's own data tests (run here with the small JUnit shim of the session, not the real JUnit). **Nothing here has been run in the game**; section 12 lists exactly
what that leaves unverified. Every file and strategy below says WHY it is the way it is.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5)

| Part | Status | Notes |
|---|---|---|
| `RegionTheme` entries `HOLY_CHURCH`, `CELESTIAL_COURT`, `SKY_CITADEL`, `MOUNTAIN_MONASTERY` (all `Route.HOLY`) | NEW | Appended after `ABYSS`; nothing else in the enum touched. They are `exclusive()` (ordinal 11 and later): never drawn on the Abyssal road. |
| `data/nusmp/mega/themes/<id>.json` (four) | NEW | Palette families, explicit swaps, keywords, catastrophes, ambient, light, sound, danger, `_why`. |
| 27 signature rooms (`structure/mega/<folder>/<id>_<room>.nbt`) | NEW | Seven for the Church, seven for the Court, six for the Citadel, seven for the Monastery. No existing room was changed. |
| `data/nusmp/mega/template_themes/<id>.json` (four) | NEW | The rule "this room is drawn only in a sector of its theme" and the pool weights of the rooms. |
| `data/nusmp/mega/encounters/<id>.json` (four) | NEW | 13 entries each, all four bands. |
| `data/nusmp/mega/factions/*.json` (eleven) | EDITED | Four lines each: the weight of every faction in the four new themes (`FactionDataTest` demands a weight for EVERY theme in EVERY faction file). The entity tag files are untouched. |
| `data/nusmp/mega/relic_themes.json` | EDITED | 20 relics moved to the new themes (section 9), four ruleset names added. Nothing removed: every relic is still listed once. |
| Generated pools `worldgen/template_pool/mega/side.json`, `safe.json` and `src/test/resources/mega/catalog.json` | REGENERATED | By `gen_mega_pools.py` and `mega_catalog.py`; the lead regenerates once more after the merge. |
| Tests: `GoodRoadThemesDataTest` | NEW | Seven checks of the glue between rooms, files and pools (section 10). |
| Tests: theme lists of `ThemeDataTest`, `RelicThemesDataTest`, `EncounterDataTest` | EDITED | My four ids appended to their `SHIPPED` / `THEMES` lists; two hard-coded counts replaced by the size of the list. |
| Tools: `gen_mega_theme_<id>.py` (four), `mega_tha_kit.py`, `mega_tha_view.py` | NEW | The generators, the shared kit and the themed preview wrapper (section 11). |
| The 122 rooms of the first edition, the eleven themes, their tables, every other relic | KEPT | |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. Research (what was looked up and what was taken)

The brief asks for the sources of each theme. **What actually happened:** `WebSearch` worked and returned result lists with short summaries; `WebFetch` could not reach the
network from this environment (`getaddrinfo ENOTFOUND` for every host tried), so no page below was read in full and nothing is quoted: what follows are ideas and facts
(names of spaces, proportions, what a place is for), each one something that those results state or that is common knowledge about the subject. No text was copied.

| Theme | Searched for | URLs the searches returned and that fed the choices |
|---|---|---|
| Holy Church | the parts of a cathedral (nave, aisles, transept, crossing, choir, apse, ambulatory, chapels, cloister, chapter house, crypt, bell tower, baptistery, triforium, clerestory); church areas in Ravenloft's Curse of Strahd; cloister gardens, wells, reliquaries and treasuries of abbeys; gargoyles and animated statues in the Monster Manual | https://en.wikipedia.org/wiki/Cathedral_floorplan , https://courses.lumenlearning.com/suny-arthistory1/chapter/church-architecture/ , https://smarthistory.org/?p=1151 , https://sites.pitt.edu/~medart/menuglossary/crypt.htm , https://www.dhi.ac.uk/cistercians/kirkstall/buildings/cloister/cloister2.php , https://brewminate.com/sacred-spaces-architecture-and-the-medieval-abbey-layout/ , https://production.english-heritage.org.uk/siteassets/home/visit/places-to-visit/wenlock-priory/history/wenlock-priory-plan.pdf , https://ravenloft.fandom.com/wiki/Curse_of_Strahd , https://5thsrd.org/gamemaster_rules/monsters/gargoyle/ |
| Celestial Court | Mount Celestia (the seven heavens Lunia, Mercuria, Venya, Solania, Mertion, Jovar, Chryseia), its look and its inhabitants (the archons: lantern, hound, warden, trumpet; devas, planetars, solars) | https://planescape.fandom.com/wiki/Mount_Celestia , https://forgottenrealms.fandom.com/wiki/Lunia , https://forgottenrealms.fandom.com/wiki/Warden_archon , https://greyhawkonline.com/greyhawkwiki/Celestia , https://planewalker.com/encyclopedia/mount-celestia_.html |
| Sky Citadel | cloud giant castles in Storm King's Thunder (Maelstrom, Lyn Armaal and the cloud castles), giant-scale furniture, griffons as mounts, Jack and the Beanstalk's castle, Laputa, Cloud Cuckoo Land | https://forgottenrealms.fandom.com/wiki/Cloud_giant , https://www.dndbeyond.com/sources/skt , https://www.czepeku.com/blog/top-10-battlemaps-for-storm-kings-thunder , https://theotherside.timsbrannan.com/2016/09/castle-of-cloud-giant-queen.html , https://thecrazygm.root.sx/ogl/pf1e/items/cloud-castle-of-the-storm-king/ |
| Mountain Monastery | Shaolin, Tibetan and Zen monasteries (assembly hall, prayer wheels, debate courtyard, chortens, stairs cut into a cliff, bell tower, hot springs, the dry garden, shoji and tatami), the Shaolin wooden men and the 36 chambers, shrine guardians, torii and stone lanterns | https://en.wikipedia.org/wiki/Tibetan_Buddhist_architecture , https://en.wikipedia.org/wiki/Pagoda_Forest_at_Shaolin_Temple , https://en.wikipedia.org/wiki/Shaolin_Wooden_Men , https://en.wikipedia.org/wiki/The_36th_Chamber_of_Shaolin , https://buffaloah.com/a/virtual/tib/sera/sera.html , https://asianart.com/kora/fkora1.html , https://historyrise.com/article/architectural-innovations-in-kamakura-the-development-of-zen-temples-and-castles/ , https://muza-chan.net/japan/index.php/blog/japanese-spiritual-architecture-hakamagoshi-shoro , https://www2.city.kyoto.lg.jp/bunshi/bunkazai/isan-l-e.htm |

### What was decided from it (signature spaces and props)

* **Holy Church**, spaces: the nave with its pews and arcades, the clerestory and the stained glass of a different colour on every wall, the chancel and altar at the east end, the choir
  loft with the organ, the bell tower you climb to a real belfry, the cloister garden with a well, the confessional booths (here a puzzle), the crypt under the nave with a sunk chamber of
  bones, the reliquary treasury, the baptistery (a font under a stone canopy, the Church's Safe Zone). Props: lancets and rose windows of stained glass lit from behind, pews of stairs, an organ of
  iron-bar pipes tipped with lightning rods, a bronze bell on a beam, sarcophagi with their lids half off, gilded cases of relics, holy-water fonts by the doors of the bell tower and the confessional.
* **Celestial Court**, spaces: terraces climbing to a halo window, the processional hall of the Golden Gate, the throne room, the sundial court, the banquet hall, the tribunal of the
  court, a quiet fountain court. Props: polished marble and gold, cyan glass, a gate of gold and iron bars, statues of archons on plinths, wagon chandeliers of candles, banners.
  The aviaries of the brief are the owls of the encounter table, not a room.
* **Sky Citadel**, spaces: the giant's great hall (a table four blocks high, chairs with a seat two blocks up), the wind gallery open to the sky, the chasm with its rope bridges, the Storm
  Heart under a dome, the giant's pantry with shelves ten blocks tall, the thunder court. Props: giant furniture, lightning rods in rings and on masts, copper pillars with lit bulbs,
  bridges of planks with a plank missing, hay nests, hams on chains. Stairs with two-block risers are expressed as ladders up a table leg or a shelf post (a vanilla stair cannot rise two).
* **Mountain Monastery**, spaces: the dojo, the prayer wheel hall with one great wheel and a gallery, the scroll library, the zen garden with a pond and a red moon bridge, the stair cut into a
  cliff with a torii on every landing, the hot springs, the duel ring. Props: shoji screens of white panes in timber lattices lit warm from behind, giant paper lanterns (a cube of white glass
  round a lantern on a chain), stone lanterns, a torii, a prayer wheel of red lacquer with gold hoops, bamboo, a cherry tree, a bell on a frame.

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. Palettes, light and mood (`data/nusmp/mega/themes/<id>.json`)

The rooms are written in the deepslate palette of `mega_lib` and the Region Theme turns it into the theme's families at generation time (`mega_theme.md` section 3). So a theme file is
nine family slots plus an explicit map. Each file keeps its own `_why`; the numbers:

| | Holy Church | Celestial Court | Sky Citadel | Mountain Monastery |
|---|---|---|---|---|
| brick (walls) | smooth sandstone | polished diorite | white concrete | bamboo mosaic |
| tile (floors, ceilings) | stone bricks | polished andesite | light grey concrete | dark oak planks |
| polished (pillars, trim) | polished diorite | quartz block | smooth quartz | stripped bamboo block |
| chiseled (capitals, keystones) | chiselled sandstone | gold block | light blue concrete | red terracotta |
| cobbled / rock / wood | sandstone / stone / dark oak | andesite / stone / birch | andesite / snow block / oak | cobblestone / stone / spruce |
| light | +1 | +2 | +2 | +1 |
| mood sound (volume, pitch, every s) | bell resonate (0.3, 0.5, 10) | beacon ambient (0.3, 1.0, 9) | lightning thunder (0.2, 1.3, 14) | amethyst chime (0.3, 1.4, 9) |
| ambient particle | white ash | enchant | cloud | smoke |
| danger | 0.9 | 0.9 | 1.0 | 0.95 |
| catastrophes (collapse, flood, spirit) | 2, 1, 4 | 3, 1, 2 | 4, 1, 2 | 3, 1, 3 |

**Why these.** The Holy road already has Radiant (smooth quartz, smooth stone, gold, calcite). The owner's wish for the church was "not Radiant's quartz sameness", so the Church is warm
limestone: cream smooth sandstone ashlar, grey stone-brick floors and vaults, white-grey diorite pillars, dark oak, and the colour comes from what the rooms add (red carpet, gold,
candles, stained glass). The Court is white over grey with gold (polished diorite walls, polished andesite floors, quartz pillars, solid gold capitals) so it is not Radiant (no grey at
all) and not Astral (dark prismarine floor). The Citadel is the one theme with no stone grain: concrete walls and floors, quartz pillars, sky-blue capitals, andesite rubble and a snow
mass, so everything reads as cloud and weather. The Monastery is timber and paper: cobblestone footing (the stone is only at the foot), bamboo-mosaic walls, dark oak boards for floors
and ceilings, spruce for beams and furniture, red terracotta where the other themes have keystones (every doorway gets a torii-red lintel).

Concrete, bamboo mosaic and the plank families have no stairs, slabs or walls of their own, so the explicit maps name every shape the palette needs (the Citadel names the most: eight
swaps). All four turn every soul light of the templates into an ordinary warm one: a church is lit by candle, a court by lantern, giants burn torches the size of trees and paper
lanterns are never cold. Danger is under 1 except at the Citadel: the Holy road is the kinder road, but a fall from a cloud giant's gallery is a real danger and its table is a tenth
bigger (section 7). `ThemeDataTest` (12 checks: keywords at least eight and unique across all themes, light at most +3, danger at least 0.8, the brick differs from every other theme's,
at least four slot differences, at most eight fallbacks, fixed particle and sound allow-lists) passes with the four files.

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The rooms

Every room is generated by `tools/labyrinth/gen_mega_theme_<id>.py`, built on the furnishing kit of the first edition (`mega_decor_r1.Hall`) and `mega_tha_kit`, and passes: the hall's own
gate (`Hall.check`: walk graph, 3-wide routes, light, leaks, floating decor, markers), `mega_arch_check.py <id>` (survival rules of lanterns, chains, ladders, water, gravity), and
`mega_check.py` (the connector contract). Safe rooms also pass `mega_safe_verify`. A room with a defect is never written to the data folder (`--force` writes it to be looked at).
All 27 are 9-block-grid modules with 3x4 doorways at module centres. Size is `modules wide x deep` and, where it is worth saying, the interior height in rows (a module is 9 blocks; the shell adds the floor row and
two ceiling layers), blocks are what the generator writes.

### 4.1 Holy Church (`holy_church_*`)

| Room (folder) | Size | Blocks | What it is, and why |
|---|---|---|---|
| `nave` (hall) | 3x5, 22 high | 14.9k | The big multi-storey hall: a pointed barrel vault over a nave, two arcades, a gallery deck above each aisle, a choir loft with an organ of bar pipes against the south wall, the chancel one step up with an altar, a reredos and a cross, three-seat pews of stairs on both sides of a red runner, a different stained-glass set on every wall and gable windows at both ends, wagon chandeliers. The hall that says "church" at a glance. |
| `cloister` (hall) | 3x3 | 5.8k | The calm hall: a covered walk on four sides round an open garden under a jewel-glass skylight, four trees, a well, lamp posts, a colour of flower to every quarter, benches and banners between high windows. Light from above, quiet. |
| `bell_tower` (set) | 2x2, 36 high | 6.0k | The set piece: a climb of three flights of eight steps round the walls (a landing at each corner, rails on the open side) to a belfry deck with louvres of bars lit from behind, a beam and a real bell, four gargoyles; a chest on the second landing; a saint on a plinth at the foot. |
| `confessional` (puzzle) | 3x2 | 4.5k | The danger room that is a puzzle: six booths with a prie-dieu and a lit grille; four hold a sigil plate (a right click, never a pressure plate), two a skull. A Riddle Stone by the door names the order of the colours; the right order opens the puzzle door to the sacristy (two chests of the trial loot). Pews, candles, a holy-water font. |
| `crypt` (hall) | 3x3 | 6.1k | The low hall: three arches of vault between pier pairs, sarcophagi along both aisles (two with the lid off), skulls in wall niches, webs, bones, the saint's tomb on a dais at the north end, and a stairwell down into a sunk chamber of bones and tombs with a chest (the crypt's own treasure). Six doors, so a party can cross it from many sides. |
| `reliquary` (treasure) | 2x2 | 4.1k | The calm room: a hipped roof ending in a yellow-glass lantern of light, a gold-and-white floor medallion, a gilded glass case round a skull on a gold block, red carpets from both doors, eight niches each with a small case of the relic's metal (two hold chests). |
| `baptistery` (safe) | 2x2 | 3.0k | The second calm room, a real Safe Zone (passes `mega_safe_verify`): a font (a ring of still water round a pedestal with candles, a polished rim) under a stone canopy of four pillars and a hipped roof that ends in a lantern of light, a diamond of gold in the floor, nine seats on three sides (every one a rest anchor), a votive fire in a niche in the south-west corner with a bench, three white pallets with a banner each, stained glass on every wall. The owner's baptistery. |

Markers: spawn, patrol, a faction where the room is not a Safe Zone (nave and confessional `cultist`, cloister, bell tower and reliquary `construct`, crypt `undead`), a room centre where the room has one.

### 4.2 Celestial Court (`celestial_court_*`)

| Room (folder) | Size | Blocks | What it is, and why |
|---|---|---|---|
| `terrace` (hall) | 3x4, 22 high | 9.2k | The big multi-storey hall: three marble terraces (on the floor, two rows up, four rows up) with stairs between them and solid stone under them, climbing north to a halo window; a long pool on the lowest terrace and a fountain with a gold spout on each of the others (water sealed by its rim); colonnaded side walks with an architrave, banners and windows between the columns; a lattice of white and cyan glass in the roof, lit from above; braziers of white fire at the doors. |
| `colonnade` (hall) | 2x5 | 7.3k | The processional hall: a white runner with gold borders over a chequered white and slate field under a long skylight, the Golden Gate (two pylons, a pointed arch and a portcullis of iron bars in a gold frame) across the nave a third of the way in, side walks with tall windows, banners on the columns, statues of archons between them, chandeliers. |
| `throne_room` (set) | 3x4, 24 high | 17.7k | The set piece: a hipped vault with a ridge of white glass lit from above, a dais three rows high at the north end with a flight of three steps, a throne with armrests and a high back of polished stone crowned with gold and a rod of light, a long cyan carpet with gold borders, two rows of columns, tall windows, chandeliers hung from the ridge. The largest room of the four themes. |
| `sundial` (hall) | 3x3, 16 high, round | 7.5k | A drum with a great sundial in the floor (a polished disc, a gold hour ring, twelve hour marks, a slate hub, the shadow of the gnomon laid in dark concrete), an oculus of white and cyan glass in a ring of gold overhead, twelve columns round the dial with benches between them, windows cut into the wall mass between the doors. |
| `banquet` (hall) | 3x3 | 5.4k | Two long laid tables (white cloth, golden candle clusters, melons, cakes) with a row of chairs each side, a high table on a one-step dais at the north end with a gilded seat in the middle and a halo window behind it, a cyan runner between polished lines, basins in the south corners, chandeliers over the tables. |
| `tribunal` (trial) | 3x3 | 5.4k | The danger room: a court of tiers and a judges' bench with lecterns and the High Judge's seat under a rose window, a jury on both sides, a ring of eight glyphs. Walking in seals both doors (a Sealed Archway keystone hangs over the middle, watching 13 blocks) and the court sends three living armours and two vexes. A chest on the bench is the reward. |
| `fountain_court` (safe) | 2x3 | 3.5k | The calm room, a real Safe Zone (passes `mega_safe_verify`): a fountain with a gold medallion, nine seats (every one a rest anchor), a hearth, three beds, lamp posts, light of at least 8 everywhere. |

Markers: every room but the fountain court names `construct` (the court's guardians); the fountain court is a Safe Zone and names none.

### 4.3 Sky Citadel (`sky_citadel_*`)

| Room (folder) | Size | Blocks | What it is, and why |
|---|---|---|---|
| `great_hall` (hall) | 3x5, 30 high | 13.6k | The giant's hall, huge scale: a table whose top is four blocks up with legs three wide that a party can walk under, two ladders up the legs and loaves, cheese, barrels and two chests on top, chairs with a seat two up and a back seven high along both walls, a three-fire hearth in a chimney breast, windows five wide and sixteen tall, two wagon chandeliers. |
| `wind_gallery` (hall) | 2x5 | 5.8k | A long gallery open to the sky on one side: arches of bars in front of a glare, a fire in every recess of the other wall, banners between them, a ring chandelier of lightning rods, a floor drifted with pale stone as if cloud had blown across it. |
| `cloud_chasm` (trap) | 3x3, 18 high | 6.6k | The trap and the long drop: a round chasm (an ellipse 17 x 15) with a floor of lit cloud seven blocks down (white wool and snow with sea lanterns, so the chasm glows), an island of straw with four storm masts and a chest in the middle on a cone of stone, four rope bridges to it (planks, rails, a lamp post at each head), three of them with planks missing (one, two, one) and the fourth whole, two ladders out for those who fall, a ring ledge round it all (three wide: nobody has to cross). A fall costs about four health, never traps. |
| `storm_spire` (set) | 3x3, 34 high | 15.6k | The Storm Heart: a drum under a dome with an oculus of glass, a floor like a storm rose (copper spokes, two sky-blue rings), a three-by-three copper pillar with a band of lit bulbs every fifth row, a heart of sea lanterns in a glass cage on top with five lightning rods, two plank gantries round the pillar with ladders up the faces (south face to the first, north face to the second) and a chest on the upper one, up to sixteen slits of coloured glass in the drum, eight pillars between the doors. |
| `pantry` (treasure) | 2x3 | 5.1k | The giant's pantry: two blocks of shelving (three boards, posts floor to ceiling) stocked with barrels, jars, sacks of grain and flour, wheels of cheese and bolts of cloth, ladders up them, five chests (two on the floor, three on boards), hams and sausages on chains from beams, a light-blue runner. |
| `thunder_court` (trial) | 3x3 | 5.8k | The danger room: eight copper masts on a ring, each with a lit bulb and a lightning rod, a rim of bars between them with lanterns hanging from it, a bolt of light zigzagging across the roof, the storm altar (three steps and a chest) in the west wall. Walking in seals both doors and a storm of two breezes and three vexes comes. |

Markers: great hall and pantry `ogre`, wind gallery, storm spire and thunder court `elemental`, cloud chasm `beast`.

### 4.4 Mountain Monastery (`mountain_monastery_*`)

| Room (folder) | Size | Blocks | What it is, and why |
|---|---|---|---|
| `dojo` (hall) | 3x3 | 5.3k | The training hall: a mat of woven bamboo ringed in red lacquer with a red circle, four dummies on it (a log post, a hay head, fence arms), racks of staves, a shrine stage in the north wall with a lectern, candles and a bell on a frame, four giant paper lanterns, shoji on every wall. |
| `prayer_wheel_hall` (hall) | 3x3, 26 high | 13.1k | The big multi-storey hall: one great prayer wheel (a drum of red lacquer with three gold hoops, a lid and a finial) on a stepped plinth, a ring gallery three wide at the height of its middle with a rail and a flight up from the south-west corner, racks of small wheels (barrels two high) along the walls, a hipped timber roof closing to a lantern of warm light, four giant paper lanterns. |
| `scroll_library` (hall) | 2x3 | 4.9k | A long library on two levels: bookshelves along both long walls on both floors, a U of galleries joined by a bridge over the south doors, a flight up the north wall, two long low desks with lecterns and cushions, giant paper lanterns over them. |
| `zen_garden` (hall) | 3x3 | 5.5k | The calm hall: raked white gravel in stripes and in rings round the rocks, a pond with a stone rim, lily pads and a red moon bridge, a cherry tree on a moss mound with petals, a corner of bamboo, stone lanterns along the walls, a veranda with a roof along the north wall, a lid of glass over the sky. The walks round the pond are three wide. |
| `cliff_stairs` (set) | 3x3, 34 high | 12.0k | The set piece: a mountain of three terraces in the middle of the hall, each narrower than the one below, with a stone stair hugging its faces in three flights (south face, east face, north face of the top terrace), a landing at every turn, red torii at the foot of the stair, at the turn to the second flight and on the summit bridge, a small shrine with an offering chest on top, stone lanterns on the ledges, four giant paper lanterns on long chains. |
| `hot_springs` (safe) | 3x2 | 5.5k | The calm room, a real Safe Zone: two hot pools with rims of rock and benches facing them (seven seats), a bamboo screen, a stove with a bench in the north-west corner, three futons in the north-east, a hipped roof ending in a ridge of warm light, giant paper lanterns over the pools. |
| `duel_ring` (trial) | 3x3 | 5.6k | The danger room: a low platform of woven bamboo ringed in red with a rope on posts and a gap on each side, a red torii before each door, benches for the watchers, racks of staves, a bell by the door, a chest in a recess of the west wall. Walking in seals both doors and an iron golem and two living armours come. |

Markers: dojo and scroll library `cultist` (the rogue monks), prayer wheel hall and duel ring `construct`, zen garden and cliff stairs `beast`; the hot springs are a Safe Zone and name none.

**Strategy shared by the four generators.** A room is a function in a Python file that returns a finished `Hall`; the same call builds, checks, renders and saves it, so the file on disk
is always the output of the script (`--check` proves it, and exits 1 on any difference). The script has no randomness except the seeded RNG of the template name, so a rebuild is
byte-identical. WHY not hand-placed NBT: a hundred thousand blocks are not editable by hand, and the owner can rebuild a room after a change of the palette or the kit.

**Why the kit** (`mega_tha_kit.py`). The four themes all needed the same things the first edition's furnishing kit does not have: windows of several coloured panes that glow (a lancet, a
rose, a bar arch), a pointed arcade between piers, decks with stairs and rails, a hip roof and a dome, a spiral and straight flights, wagon chandeliers on chains, ring-shaped lining under
the floor (a chasm, a pool), the Sealed Archway trial core, the puzzle closet, an `auto_light` that hangs lanterns from whatever solid block lies above a dark cell (a gallery roof, a
loft). It also teaches the first edition's checks the handful of vanilla blocks these themes bring in (grass, bamboo mosaic, copper bulbs, doors of paper): they are real full blocks
the old tables did not list, so without the patch a garden floor would count as "see-through". The patch is applied in memory, never to a shared file.

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. Safe rooms, trials and puzzles (what the rules asked and how it was met)

* **Safe rooms** (`fountain_court`, `hot_springs`): `h.safe = True`, the volume in front of every doorway is declared four high (`keep_doors_clear`), no spawn and no faction marker, nine and
  seven seats as rest anchors, a hearth marker over a fire, three bed markers over bed feet, two patrol markers, light of at least 8 on the floor; `mega_safe_verify.verify(min_light=6)` is part of the gate.
* **Trials** (`tribunal`, `thunder_court`, `duel_ring`): the Sealed Archway of 1.36.0 written the way `gen_mega_rooms2.py` writes it: a ward rune in the floor of each doorway, eight spawn
  glyphs on the floor, the keystone hung from the roof on a chain with `radius 13`, `reset 60` and the roster of the wave. They carry no `spawn` marker (the keystone sends the wave) and are not
  ambushes. Rosters: tribunal 3 living armour + 2 vex; thunder court 2 breeze + 3 vex; duel ring 1 iron golem + 2 living armour.
* **Puzzle** (`confessional`): `nusmp:sigil_plate` x4 (one colour each), `nusmp:riddle_stone` with the sequence blue, red, green, gold, `nusmp:gatekeeper_door[kind=puzzle]` and a closet whose
  chests use `nusmp:chests/labyrinth_trial`. The order is the same in every copy of the room (the Spire's puzzle rooms do the same).
* **Trap** (`cloud_chasm`): no custom block. The hazard is the drop, the rickety bridges and the island that is a lure, and it never traps (ladders, a ring ledge).

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. How often the rooms appear (`data/nusmp/mega/template_themes/<id>.json`)

Format of `mega_v2.md` 5.1: `templates` (prefixes `nusmp:mega/<folder>/<id>_`) and `relative` (weights per prefix, read by `gen_mega_pools.py`). `TemplateThemes` makes a room drawable only
in a sector of its theme. Weights as written: hall 60, set 10, trap 8, treasure 8, trial 6, puzzle 6, safe 10 (only the folders a theme uses).

**What the pool maths gives (computed with the real `pool_elements` of `gen_mega_pools.py`).** A template's weight is `share * 20 * relative / (sum of the relative weights of every template of that
folder)`, rounded, at least 1, at most 150. With these four themes alone (13 new halls among 28 old ones), a sector of each theme gets: Church 13.9 percent of all doorway draws and 31.2 percent of
the draws of the room folders are its signature rooms (hall weights 4 each; hall 30 percent, puzzle 50, safe 50, set 83, treasure 54); Court 11.4 / 26.7 (hall 36, safe 50, set 83, trial 56); Citadel 12.1 / 27.9
(hall 22, set 83, trap 43, treasure 54, trial 56); Monastery 11.4 / 26.7 (hall 36, safe 50, set 83, trial 56). That is **not** "about half", and it cannot be, for two reasons the generator forces:
the old halls are many (28) and every one weighs at least 1, and the folder's weights are divided by the sum over ALL themes' rooms, so each theme added to the project lowers the others' numbers.
Re-run with sixteen themes of this shape (a stress test, not committed): every hall weighs 1, the signature rooms are 5 to 8 percent of all draws and 13 to 20 percent of room draws.

**What the regenerated pools do to the sectors of the OTHER themes (found at the end, read this).** `TemplateThemes` throws a theme's rooms away in every other theme's sector, but the folder
shares of `gen_mega_pools.py` were divided with them first, so the free (old) templates lose weight everywhere. In the committed files: `side.json` has 27 new elements and 35 of the 76 old ones
lighter (18 halls, mostly 3 or 4 down to 1; `cathedral_rotunda`, the only old set room, 20 down to 1; the two trial halls 10 to 2; the three closets 5 to 8 down to 2; the two sigil halls 10 to 5; the three traps
7 to 4; the six safe rooms 3 to 1), `safe.json` has 3 new elements and its six old ones 3 down to 1. In a sector of one of the eleven old themes the side pool (total 364 before, 248 after, my rooms
rejected) now draws, before to after: halls 17.3 to 11.3 percent, set pieces 5.5 to 0.4, trials 5.5 to 1.6, treasure rooms 5.8 to 2.4, safe rooms 4.9 to 2.4, puzzles 5.5 to 4.0, traps 5.8 to 4.8, while
corridors (26.6 to 39.1), junctions (16.8 to 24.6) and caverns (5.8 to 8.5) gain share because their weights did not move. No weight I could write avoids it entirely under the current maths (even 1 per room moves the old set room from 20 to 4, and rooms of
weight 1 would be so rare that a themed sector would not feel themed), and every builder who adds claimed rooms adds to it. It is a property of the maths of section 5.1, not of the rooms, and the patch below removes it: with it,
all 76 old elements of the side pool and all 6 of the safe pool keep exactly their committed weights (checked by running the patched `build()` against the committed files: identical, totals 364 and 18).

**Recommendation to the lead (not applied: `gen_mega_pools.py` is not mine).** Treat a `template_themes` weight as an ABSOLUTE weight (1 to 150) for a claimed template and leave the folder share to
the free templates. The change is small (in `pool_elements`, split `items` into claimed and free; the free ones keep the old maths; the claimed ones use `relative_weight` directly) and the result no
longer depends on how many themes exist: with weights hall 15 to 30 (so the halls of a theme add up to about 60, the weight of the old halls), set, trap, trial, treasure, puzzle and safe 20, a
sector of each of my themes draws 25 to 28 percent signature rooms of all doorway draws and 37 to 41 percent of the room draws (about half inside each folder), whether four themes exist or sixteen. The exact patch is in
`$S/arc/frag/tha_pools_absolute.patch` and in section 12 of this file; the numbers to write into the four files then are: Church hall 20, Court hall 15, Citadel hall 30, Monastery hall 15, and 20 for
every other folder (the safe rooms included).

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. Encounter tables (`data/nusmp/mega/encounters/<id>.json`)

Format and rules of `mega_enc.md` and `EncounterDataTest` (8 to 14 entries, all four bands, a first monster the band can pay, only ids that exist, ascii only, a `Why:` comment, entry ids unique
over all files, message keys `message.nusmp.mega.enc.<theme>.<entry>`). Each table has 13 entries (3 minor, 5 standard, 3 hard, 2 ambush), an evergreen entry in the minor and standard bands (no `needs`,
no upper threat; the standard one names explicit ids, so a thin tag on the first floors still gives a fight, the minor one is the room's own people as in the older tables, except at the Citadel where it is one or two phantoms), the first sector offers only minor and standard fights, and every id is a vanilla entity, `nusmp:living_armor`,
a Tensura entity that other tables already use, or a faction tag. Entry ids carry the theme's word (`church_`, `court_`, `sky_`, `monastery_`) because four builders write tables at the same time.

| Theme | Roll bonus, budget | Default faction | Design (and why) |
|---|---|---|---|
| Holy Church | -1, 1.0 | cultist | Lit softer than the tower of light, so only -1. Everything here was once devout: penitents (the room's own, minor), votive spirits (vexes for a lit party), belfry bats (a noisy party), stone saints (living armour, evergreen), a choir that lost its god (cultist + construct), gargoyles that drop from the buttresses (construct, close, short hunt, message), the dead of the crypt for a party that lets its candles go out (undead, needs dark: the rare dark entry), confessors (evoker + vexes), the bell ringer (iron golem for a noisy party), fallen templars (armour + a priest), a heretic conclave (cultist + demon, allies by the tag rules), the tolling (vex wave) and the last procession (armour + undead, two players or more). |
| Celestial Court | -2, 1.0 | construct | Open sky and glass, bright like the tower of light, so -2. The court itself: ushers (construct), halo motes (vexes), owls from the aviaries (needs light), the archons' gilded armour, a choir of winds (elemental, needs light), gate sentinels (iron golem + armour at a gate), magistrates (evoker + illusioner + witnesses), the condemned (undead for the unlit), the executioner (iron golem for a noisy party), the judge's verdict (a full bench for a bright party), statues of the colonnade (armour + evoker, close), trumpets of the host and the heavenly host. |
| Sky Citadel | -1, 1.1 | ogre | Bright and open, but built for something bigger and the fall is the real danger, so a tenth more budget. Harpies (phantoms, evergreen minor), storm wisps, owls, the castle's armour (evergreen standard), a flock (phantoms + vexes), the beasts of the aeries (beast tag + owls), a storm choir (elemental), a wall of wind (breezes in the windows, close), a giant's kin (ogre tag + armour for a noisy party; if the ogre tag has nobody in the toughness window the armour alone answers), thunderhead (evoker + breezes), wing knights (armour + harpies, far), the storm front and the giants' feast call. |
| Mountain Monastery | 0, 1.0 | construct | Quiet halls of paper and timber, so no bonus. Novices (the room's own people, evergreen minor), chime spirits (vexes for a noisy party), eave swifts (bats for a party with no light), iron disciples, rogue brothers (vindicators + an evoker), the mountain pack (beast tag), gnawers in the paper walls (silverfish, close), the silent master (one vindicator for a dark party), the gate guardian (iron golem for a noisy party), the duel (a ring of monks, close), the temple warband, the gong of the mountain and the mountain itself. |

Factions are used sparingly and the entity tags are not edited (they are shared). What `FactionDataTest` does demand is a weight of every faction in every theme, so the eleven faction files gained four lines each
(`$S/arc/frag/tha_faction_weights.py` writes them, idempotently): Church construct 5, cultist 4, undead 3, elemental 2 and one each of aquatic, beast, demon; Court construct 5, elemental 4 and one each of cultist,
undead, beast, aquatic; Citadel ogre 5, elemental 5, beast 3, construct 2, goblin 1, insect 1; Monastery beast 4, construct 4, cultist 2 and one each of elemental, insect, slime, aquatic. Zero keeps a faction out of a
theme's rooms that name none (the rooms of this builder all name theirs). Each theme lets at least six factions in, and no faction takes more than 30 percent of 600 rooms (the test allows 45).

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. Language (`$S/arc/frag/tha.lang.json`)

31 keys: the four theme names (`message.nusmp.mega.mgd.theme.<id>`, read by the Guardian), one flavour line each (`...<id>.flavour`: nothing reads it yet, it is the line a loading
screen or the status command could show), and the 23 encounter messages. The lead merges the fragment into `en_us.json`; `EncounterDataTest.messagesAreTranslatedOnceTheFragmentIsMerged` fails a half-merged table.

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Relic roads (`data/nusmp/mega/relic_themes.json`)

A relic leads to the theme that the explicit table names, else the theme whose keywords it carries, else a hash. All 137 relics of Unique grade and above are already in the explicit table, so a
new theme can only be reached by moving entries (and `RelicThemesDataTest` demands at least five relics for every shipped theme and at most a quarter of all for one). Twenty relics moved, by
lore, none removed:

| To | Relic (from) |
|---|---|
| holy_church | `divine_mercy`, `deliverance`, `hand_of_providence`, `holy_handgrenade`, `mantle_of_selflessness` (all from radiant: the cathedral takes the priest's relics) |
| celestial_court | `godking_crown`, `armor_of_the_lionheart`, `unicorn_crown`, `celestial_support`, `sunheart_medallion` (all from radiant: the crowns and the heavenly armour) |
| sky_citadel | `greaves_of_the_wind_walker`, `cloak_of_levitation`, `windcutter` (storm), `boots_of_the_high_ground` (gravity), `the_bifrost` (radiant) |
| mountain_monastery | `elemental_hand_wraps` (storm), `staff_of_moonlight`, `crest_of_foresight`, `old_masters_spellbook` (astral), `katana_of_the_crimson_shogun` (blood) |

Radiant keeps six, storm five, gravity seven, astral thirteen, blood thirteen. `ruleset_names` gained: The Great Cathedral, The Court of Light, The Cloud Castle, The Mountain Retreat. The theme
keywords (15 to 16 each, unique over all files) still lead any relic named for them that is not in the table (the RARE and SPECIAL ones). If another builder wants one of these relics, take it
from this table (a conflict on that line is the lead's to decide; the alternatives for each theme are any other relic of the donor themes).

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. Tests

* `GoodRoadThemesDataTest` (new, 7 checks, 0.1 s): the enum entry, palette, table and template_themes file exist; the file names its theme and every prefix is `nusmp:mega/<folder>/<id>_`; the
  folders of the prefixes are exactly the folders that hold the theme's templates (a prefix with no room is a typo, a room with no prefix is free for every theme); `TemplateThemes` built from
  the same files allows each room in its own theme and in no other of the four, nor in gravity, radiant or abyss; `relative` weighs exactly the prefixes with whole weights; at least five rooms,
  two halls, a set piece, a danger room and a calm room; every room is in the generated `side` pool (and `safe` for safe rooms). Mutation check: a one-letter typo in one prefix fails four of the seven.
* Existing data tests with the four ids added: `ThemeDataTest` (12 of 12), `RelicThemesDataTest` (7 of 7), `EncounterDataTest` (18 of 18), `FactionDataTest` (20 of 20, with the weights above), `NativeFactionsTest`, `FactionGraphTest`.
* The whole pure-test tree (`alltests`, the shim, one class per process) was run with these changes; the result and the failures that are not mine are in section 12.

---------------------------------------------------------------------------------------------------------------------------------------------

## 11. Commands

```
python3 tools/labyrinth/gen_mega_theme_holy_church.py             # also: celestial_court, sky_citadel, mountain_monastery; -h for the list, a name to build one room
python3 tools/labyrinth/gen_mega_theme_<id>.py --check            # writes nothing; STALE when the file on disk differs from the generator
python3 tools/labyrinth/mega_arch_check.py <id>                   # survival rules, per theme
python3 tools/labyrinth/mega_check.py                             # the connector contract, whole repository
python3 tools/labyrinth/gen_mega_pools.py && python3 tools/labyrinth/mega_catalog.py
python3 tools/labyrinth/mega_tha_view.py mega/hall/holy_church_nave holy_church out --iso 0:26,8:34,0:38 --cam 13.5,10.6,40 --look 13.5,14,5     # a themed iso cutaway and a first-person view
```

`mega_tha_view.py` applies the Python port of `ThemeSwap` (the one of `mega_theme_preview.py`) to a copy of the template before drawing it with the two preview renderers of the first edition, and teaches
their colour tables the handful of blocks these themes bring in. The colours are a keyword table, not the game's textures: a judge of shape, light and silhouette. It never writes into the repository.

---------------------------------------------------------------------------------------------------------------------------------------------

## 12. What is NOT verified, and what the lead must know

Nothing here ran in the game. In particular:

* **Placement and swap.** The previews use a Python port of `ThemeSwap`; the Java processor may differ in a corner (a stair of a family that has none). `ThemeDataTest` proves the palette files are legal
  (every id a vanilla block, at most eight fallbacks); it does not prove how a stair of a swapped family looks.
* **Light.** The static checks use two light models (the first edition's and the preview renderer's); the real light engine is the judge. Candles, lanterns and the lit bulbs of the Citadel's pillar are the
  sources; lit copper bulbs placed by a template might flip state on a neighbour update (the Storm Spire and Thunder Court rely on them for part of their light, but the room is lit without them).
* **Water.** Fountains, ponds and pools are shut in by blocks (`mega_arch_check` proves no source is open to a side or below). Nobody has seen them in the game.
* **Entities.** Armor stands (statues) are template entities, as in the first edition; a Sealed Archway keystone, ward runes and spawn glyphs are written like the Spire's trial rooms, which were
  not run either. The wave rosters (`breeze`, `vex`, `iron_golem`, `nusmp:living_armor`) are existing ids; whether three breezes are too hard for a first trial is a balance judgement nobody has played.
* **Ladders and bridges.** Ladders end at the row of the block they climb onto (the pit chamber of the first edition does the same); whether a player steps off in the game is unplayed. The walk
  graph of the static check does not model a jump, so the island of the chasm is reached only over the whole bridge in the model.
* **Pools.** The numbers of section 6 are arithmetic over the generator's own functions; the in-game draw is the assembler's (GEN2) and was not run.
* **Not done.** Themed corridors and junctions (welcome in the brief, optional: the pool maths above would also hide them); spirit foxes (no suitable vanilla mob, so none); a Safe Zone for the Citadel (it has a treasure room as its calm room).

**For the lead.**

1. **Tests that name the themes (all are lists, none is logic):** `ThemeDataTest.SHIPPED`, `RelicThemesDataTest.SHIPPED`, `EncounterDataTest.THEMES` (the count of tables is now `1 + THEMES.size()`; the file
   check "the folder holds exactly the tables" is the reason it fails until the list holds every theme). Done in this tree for my four ids.
2. **`GuardianScalingTest.everyThemeHasItsOwnTrait` fails for every theme after the original eleven** (it asserts that a theme's trait is named like the theme, with `radiant` and `abyss` as the only
   exceptions); this is the GUARD builder's file. Either give the new themes a trait in `GuardianScaling.traitOf` (a suggestion: `holy_church` and `celestial_court` ASTRAL, `sky_citadel` STORM, `mountain_monastery` FROST)
   and `themeColor` (`holy_church` 0xE3D3A8, `celestial_court` 0x7FE0E6, `sky_citadel` 0xBFD8F2, `mountain_monastery` 0xB8844C, the `color` of each theme file) and adapt the test's default branch, or let the default
   (GRAVITY, 0x9AA0B4) stand and change the test to accept it.
3. **Faction `themeWeights`** are in this tree (eleven files, four lines each); they are the same kind of edit as the test lists, one per theme in every faction file, so with sixteen new themes they WILL conflict:
   `$S/arc/frag/tha_faction_weights.py <worktree>` re-applies mine to the merged files in one call.
4. **`mega_arch_check.py` calls sandstone a gravity block** (`"sand" in name`). Harmless here (the check runs on the unthemed templates, which never contain sandstone, and sandstone does not fall), but the
   Holy Church's palette is sandstone, so a check of a THEMED copy would report every floor. Change the test to the exact names (`sand`, `red_sand`, `gravel`, ...) if that ever matters.
5. **Pool maths** (section 6): under the current maths the regenerated pools lower 35 old side-pool weights and all 6 old safe weights (in a sector of an old theme the set pieces fall from 5.5 to 0.4
   percent of the draws, the trials from 5.5 to 1.6), and my rooms are only 11 to 14 percent of the draws in their own sectors. Ship the patch below (every old weight stays exactly as committed, my themes get
   25 to 28 percent) and write the absolute numbers of section 6 into the four `template_themes` files; the `relative` numbers I committed are for the current maths only.
6. **Entry ids and relics** may collide with other builders' (the ids are prefixed to make it unlikely; the relic table lines of section 9 are the likely conflicts).

The patch of section 6 (`tools/labyrinth/gen_mega_pools.py`, against the version of this tree; tested by running the patched functions over the real folders, not applied):

```diff
--- a/tools/labyrinth/gen_mega_pools.py
+++ b/tools/labyrinth/gen_mega_pools.py
@@ -55,11 +55,18 @@
     scale = cfg.get("scale", 20)
     relative = pool.get("relative", {})
     absolute = pool.get("absolute", False)
+    claimed = pool.get("claimed", {})          # template_themes prefix -> weight (the `relative` of the theme files), see build()
     out = []
     for folder, share in sorted(pool["folders"].items()):
         items = [(tid, stem) for tid, stem in templates_in(folder) if stem.startswith(pool.get("filter", ""))]
         if not items:
             continue
+        own = [(tid, stem) for tid, stem in items if any(tid.startswith(p) for p in claimed)]
+        items = [x for x in items if x not in own]
+        for tid, _stem in own:
+            out.append((tid, max(1, min(150, relative_weight(claimed, tid)))))
+        if not items:
+            continue
         rels = [relative_weight(relative, tid) for tid, _ in items]
         total = float(sum(rels))
         for (tid, _stem), rel in zip(items, rels):
@@ -96,7 +103,7 @@
     extra = theme_weights()
     if extra:
         for pool in cfg["pools"].values():
-            pool["relative"] = dict(pool.get("relative", {}), **extra)
+            pool["claimed"] = dict(extra)
     out = {}
     report = []
     for name, pool in sorted(cfg["pools"].items()):
```
