# Mega-Dungeon, second edition: THEME-C, the evil road's dead and gothic places (1.59.0)

Four complete themes for the Abyssal road, in the order of the brief: **Cursed Archive** (`CURSED_ARCHIVE`), **Necropolis** (`NECROPOLIS`),
**Vampire Castle** (`VAMPIRE_CASTLE`), **Haunted Manor** (`HAUNTED_MANOR`). Each is a whole sector look: a palette that is recognisable at a glance, its
own signature rooms that only its sectors draw (`docs/labyrinth/mega_v2.md` section 5.1), its own encounter table, faction weights and relic roads, and
lang entries. This page is the WHY of every file that came with them and an honest list of what is not verified. **Nothing here has run in the game**:
the rooms were generated, checked and looked at offline (previews), the data was checked by the data tests with a JUnit shim, and every claim of the
form "it works in play" is an assumption that is listed in section 10.

Contents: 1 summary, 2 research and sources, 3 the four themes (palette, numbers, signature spaces and props, every room), 4 the weights, 5 the encounter
tables, 6 factions and relic roads, 7 files and tools, 8 how the rooms are checked, 9 tests, 10 unverified, 11 for the lead.

## 1. Summary

| Theme | Road | Rooms | Of which | Encounter table | Walls (brick slot) |
|---|---|---|---|---|---|
| Cursed Archive | Abyssal | 8 | 3 halls, 1 set piece, 1 trap, 1 puzzle, 1 treasure, 1 safe | 13 entries | red-brown clay bricks |
| Necropolis | Abyssal | 5 | 2 halls, 1 set piece, 1 trap, 1 treasure | 12 entries | sandstone (bone block pillars) |
| Vampire Castle | Abyssal | 6 | 2 halls, 2 set pieces, 1 trial, 1 treasure | 12 entries | grey andesite (crimson trim) |
| Haunted Manor | Abyssal | 6 | 2 halls, 1 set piece, 1 trial, 1 trap, 1 safe | 12 entries | diorite plaster (spruce timber) |

25 new templates, all in `data/nusmp/structure/mega/<kind>/<theme>_<room>.nbt`; four generators `tools/labyrinth/gen_mega_theme_<theme>.py` that write
them byte for byte (`--check` proves it); one kit `tools/labyrinth/mega_thc_kit.py`; four theme files, four template-theme files, four encounter tables,
44 faction weights (four in each of eleven faction files), 24 explicit relic roads and 4 ruleset names, 37 lang keys, one data test.

New or replacement (owner rule 5): everything here is **new**. No existing room, theme, table or asset was replaced or removed. The only edits to
existing files are appended lines: four enum entries in `RegionTheme`, four `themeWeights` keys in each of the eleven faction files, 24 explicit relic
lines and 4 ruleset names in `relic_themes.json`, and the regenerated pool and catalog files (generated, expected to conflict, the lead regenerates).

## 2. Research and sources

The brief asks for research first. What I could actually do: `WebSearch` worked and returned summaries of the pages; a direct `WebFetch` of one D&D
Beyond page failed (the host was not reachable from the sandbox), so **I read search summaries, not the pages themselves**. Everything below is an idea
or a fact taken from those summaries; no text was copied (owner rule: facts and ideas only). The summaries themselves said several of these sources are
weak (fan reports, product pages, a CAD blog, an unfamiliar PDF host); where that matters it is said.

| Source (URL) | What it gave, and what was decided from it |
|---|---|
| https://www.dndbeyond.com/posts/1607-castle-ravenloft-maps-explore-strahds-castle-with | Castle Ravenloft has seven zones (walls, main floor, court of the count, rooms of weeping, spires, larders, dungeons and catacombs): a castle is a stack of very different places. Decided: a great hall, a chapel, a crypt, a tower, a court (five of the six Vampire Castle rooms). |
| https://ravenloft.fandom.com/wiki/Curse_of_Strahd, https://pages.roll20.net/castle-ravenloft-guide, https://www.czepeku.com/blog/top-10-curse-of-strahd-maps, https://www.worldanvil.com/w/faerun-danhons/a/session-143A-the-dinner-with-strahd-report | Fan analyses of the castle: a feast laid out in a dining hall (wine laced with vampire blood), a hub room whose gargoyles only attack when the party comes back through it, a guests' hall with an empty suit of armour whose lighting changes after the first meeting, a stair from the larders to the Heart of Sorrow, a catacomb with few ways in, a crypt under the chapel. Decided: the Great Hall is the one room lit like a feast; gargoyles are an encounter (`vc_gargoyles`); the crypt is a low hall with four lanes in. The Great Hall / Chapel text itself was not in the summaries, so the rooms are my own design. |
| https://dungeonmister.com/lore/locations/candlekeep/, https://www.syfy.com/syfy-wire/dungeons-dragons-candlekeep-mysteries-adventure-book-anthology | Candlekeep Mysteries: seventeen adventures each starting with a book that is a door; a library kept by scholars; an "Animated Chained Library" in the contents. Decided: books are keys and doors (the Card Catalogue's Riddle Stone), chained tomes are the prop of the Archive, the scholars are a cult (the Archive's default faction is `cultist`). |
| https://www.enworld.org/threads/a-trap-on-a-book.585677/, https://paizo.com/threads/rzs2rb8r, https://www.dndbeyond.com/forums/dungeon-masters-only/2185-non-combat-encounter-ideas/next | Forum ideas for a library: flying books that act like bats, swarms of loose pages, mimic tomes, shelves that fall on a thief, moths that eat memory, a bound archivist with chains of quill and bone, a fire elemental guard. Decided: ink and pages are vexes with silverfish (`ca_ink_wisps`, `ca_torn_page`), a bay of shelving falls (`ca_shelf_fall`), bone-bound constructs (`ca_bone_bound`), and fire is the Archive's own defence (the Burning Stacks). Forum posts, so only starting points. |
| https://www.worldanvil.com/epic/1a623048-675a-4747-acf5-bf3837ff31ff/report/e625d1c4-65cc-4e71-a06d-f442418ca097, https://charles-plemons.blog.wku.edu/?p=1189, https://titus171.itch.io/one-of-us-will-die/devlog/935198/curse-of-strahd-adventure-report-i-the-death-house, https://www.enworld.org/goto/post?id=6843311, https://nyc1.lr.ggtyler.dev/r/dndnext/comments/49bvms/notes_from_running_death_house/d0r0rlg/?context=3 | Play reports of Death House: an attic of plain rooms, a padlocked nursery with child-sized beds, a toy chest and a dolls' house that is a copy of the house, the dolls' house as the clue to a way down to the cellar, a cellar that holds the house's real story. Decided: the Nursery with a dolls' house that is a small copy of the room, an Attic of trunks and a spinning wheel, the cellar as the next room to build (not built, section 3.4). |
| https://caddetailsblog.com/post/what-makes-a-building-seem-haunted, https://www.brainbound.blog/haunted-nurseries, https://www.homestyler.com/article/how-to-design-an-immersive-home-haunted-house | Haunted-house design: a Victorian theme (creepy dolls, dim light, long hallways), a rocking horse and chair that start to move, a music box winding down, humming from a nursery, a cold spot in a sealed room, portraits whose eyes follow, cracked mirrors, dust, an attic or closet as the dread space, and a flow of foyer, hallway, narrow threshold, big room. Decided: the Grand Stair is the foyer, the Nursery the threshold, the Ballroom the big room; props are the rocking horse, portraits, black mirrors, dust sheets; encounters `hm_cold_spot`, `hm_music_box`. Low-authority sources (a CAD blog, a marketing page), used for mood only. |
| https://en.wikipedia.org/wiki/Sedlec_Ossuary, https://strangeremains.com/2015/02/21/the-charnel-delights-of-sedlec-ossuary/, https://www.mentalfloss.com/article/575129/sedlec-ossuary-facts | The Sedlec ossuary: the bones of tens of thousands arranged in 1870, a chandelier of every bone in the body, chains of jawbones, a coat of arms in bone, a memento mori, bleached white. Decided: the Necropolis is bone-white and sand-pale, with skull niches and a bone obelisk. The bone chandelier is the one prop I wanted and did not build (section 3.2). |
| https://www.vagablogging.net/catacombs-and-churches-of-bones.html, https://weburbanist.com/2009/10/30/7-wonders-of-the-undead-world-global-ossuaries/, https://tombtravel.com/ossuaries-around-the-world/ | Travel write-ups of the Paris catacombs (stacked tibiae and skulls as walls), the Capuchin crypt in Rome (hooded skeletons, yellow-brown bones, "what you are now we once were") and Sedlec. Decided: walls of loculi (a skull, a candle or a heap of bones in every second niche) on solid burial blocks; hooded skeleton statues. Travel blogs, so figures were not used. |
| https://necropraxis.com/2012/11/page/2/, https://tenfootpole.org/ironspike/?p=983 | Reviews of Barrowmaze: a dungeon of tombs spread under burial mounds, wide rather than deep, with pits, columns, statues, secret and bricked-up doors, same-level stairs, pools, odd rooms and several entrances. Decided: the Catacombs have six doors, the Grave Pits are a pit field, columns and statues repeat through the street. The summary found nothing useful on Tomb of Horrors or other necropolis modules. |
| https://www.dndbeyond.com/monsters/3654064-vamp-vamp (and the other user-posted vampire pages in the same result), https://sciphijournal.org/index.php/2024/06/21/dracula-in-the-looking-glass | Vampires cast no reflection; a vampire's lair is a castle, fortified manor or abbey with the coffin in an underground crypt guarded by spawn; the surrounding region has more bats, rats and wolves. Decided: black mirrors (the Gallery of Mirrors), an open coffin on a dais, and the court's household of thralls, hounds and bats in the encounter table. User-posted pages that repeat one text, so not authoritative. |

### 2.1 What was chosen: signature spaces and props

"Built" means a template exists; "not built" is an honest gap. The brief asks for six to ten spaces and three to five props per theme.

* **Cursed Archive** spaces (10): the Stacks, the Reading Hall, the Scriptorium, the Restricted Section, the Librarian's Study, the Rotunda of Chained
  Tomes, the Card Catalogue, the Burning Stacks (all built); a floating-book chamber and an ink-black pool (not built). Props: the chained tome (a
  chiselled bookshelf hung on chains), the lectern with an open book and an ink pot (a wither rose in a flower pot), the free-standing shelf tower with
  tunnels, the drawer bank of the catalogue (barrels and chiselled bookshelves), lamps of pale violet let into the shelving.
* **Necropolis** spaces (8): the Terraced Catacombs, the Mausoleum Street, the Bone Bridges, the Grave Pits, the Burial Vault (built); an ossuary with a
  bone chandelier, a funeral procession hall and a grave field under a cavern sky (not built). Props: the skull niche (loculus) in a burial block, the
  bone obelisk crowned by a skull, the mausoleum house with a barred gate, the sarcophagus with its lid shoved aside, the soul brazier with a ghost-green lamp.
* **Vampire Castle** spaces (9): the Great Hall, the Desecrated Chapel, the Coffin Crypt, the Clock Tower (with its belfry and bell), the Blood Court, the
  Gallery of Mirrors (built); a torture dungeon, a separate bat-filled belfry and a balcony over fog (not built). Props: the coffin (open lid, red carpet, a
  skull), the mirror that shows nothing (tinted glass in a gilded frame), the pointed arch rib, the round window of red glass, the caged skull on a chain.
* **Haunted Manor** spaces (10): the Grand Stair, the Ballroom, the Nursery, the Kitchen, the Master Bedroom, the Attic (built); a cellar, a parlour, a
  library nook and a long corridor (not built). Props: the rocking horse, the grandfather clock stopped at the hour of the death, the portrait with its
  candle, the dolls' house, the crib with a bell mobile, the dust sheet.

## 3. The four themes

Every theme file is `data/nusmp/mega/themes/<id>.json` in the format of the existing ones (`families`, `explicit`, `color`, `relics.keywords`,
`catastrophes`, `ambient`, `light`, `sound`, `danger`, `_why`). Every block id is vanilla and in `tools/labyrinth/vanilla_blocks_1_21_1.txt`
(`ThemeDataTest.everyBlockIdIsAVanillaBlock`). The `_why` of each file says the same as the "palette" paragraph here.

The numbers in one table (light is the shift of the template's light in levels, danger the multiplier of the sector's danger; the mood sound is played once
in `every` two-second pulses per player, at `volume` and `pitch`, five to nine blocks from the player, so `every` 5 is about once in ten seconds):

| Theme | Colour | Light | Danger | Ambient particle | Mood sound (volume, pitch, every) | Catastrophes (collapse / flood / spirit) | Encounter roll bonus, budget scale |
|---|---|---|---|---|---|---|---|
| cursed_archive | #5B3A7A | -1 | 1.10 | enchant | item.book.page_turn (0.4, 0.6, 5) | 2 / 2 / 4 | 0, 1.0 |
| necropolis | #C9B27A | -2 | 1.20 | white_ash | ambient.basalt_deltas.mood (0.45, 0.7, 6) | 2 / 1 / 4 | 1, 1.1 |
| vampire_castle | #7A1230 | -1 | 1.25 | smoke | entity.bat.ambient (0.3, 0.6, 7) | 1 / 2 / 4 | 1, 1.2 |
| haunted_manor | #6E8A82 | -3 | 1.05 | soul | block.wooden_door.open (0.3, 0.6, 7) | 3 / 1 / 4 | 0, 0.9 |

Why these numbers: the test `ThemeDataTest` fixes the frame (the baseline is 0 light and 1.0 danger, `radiant` is the brightest and kindest, `abyss` at
-4 light and 1.3 danger is the darkest and cruellest, so nothing may go beyond them; every theme needs a mood sound under 0.7 volume and not more often
than once in three pulses). Inside that frame: the Manor is the dimmest of the four (-3, a house with the lamps burnt out) but the gentlest (1.05, small
rooms and sudden fright, not a hunt); the Necropolis is dark and old (-2, 1.2: patient and many); the Castle is the hungriest (1.25: a court that is old
and hungry) and keeps the most light of the dark ones because a feast hall is lit; the Archive is the middle (1.1: knowledge bites but does not hunt).
Spirit is the heaviest catastrophe in all four (4: a haunting is the evil road's disaster); collapse is 3 in the Manor (an old house gives way), flood 2
in the Archive and the Castle (a cellar, a moat) and 1 in the Necropolis and the Manor. These are judgement calls, not measurements.

Mood sounds: all four are vanilla sound events that exist in 1.21.1 as far as I know (`ThemeAmbient` skips an id the game does not know, so a wrong id would
be silence, not a crash); only `ambient.basalt_deltas.mood` was on the test's list of "checked" sounds, so the lead adds the other three (section 11).

### 3.1 Cursed Archive: a massive library of forbidden knowledge

**Palette and why.** Walls `minecraft:bricks` (red-brown baked clay: the warm colour of old bindings and of a scholars' hall; no other theme uses plain
bricks as its wall), worn walls `mud_bricks` (the older, browner masonry that shows through), floors `polished_blackstone_bricks`, pillars and trim
`polished_blackstone`, every keystone, capital and lintel `crying_obsidian` (a purple glow that weeps in the dark, the cursed ink of the place, the
first thing the eye finds in a corridor), rubble `cobbled_deepslate`, rock `deepslate`, wood `dark_oak_planks` (shelves and desks). Every warm light
becomes a soul light (`explicit`: lantern, torch, wall torch, campfire), because candles are the only warm thing left and the signature rooms add their
own. It must not be mistaken for Shadow (black walls, no brown, no purple), Blood (the dark purple-red of nether bricks) or the Infernal Foundry (red nether
bricks): the walls are the orange-brown of baked clay and the black and purple are only trim. Keywords: tome, book, scroll, librar, archiv, notebook,
spellbook, grimoire, scribe, codex, lectern, quill, arcan.

*Changed during the work:* the first version used `mud_bricks` for the walls, which two other builders also chose (the Feywild Grove and the Underdark),
and `ThemeDataTest` refuses two themes with the same walls. Mine moved to `bricks`, the only brown masonry left with a full set of stairs, slabs and
walls (the swap rule derives `brick_stairs`, `brick_slab` and `brick_wall` from `bricks` by taking the singular).

**Lights.** Soul lanterns light only to 10 and a thirty-block hall cannot be lit from its roof, so the rooms carry lamps of pale violet (froglights)
let into the shelving in the wall, lanterns on the posts of every gantry, lanterns hung under decks, and candles on tables. Lights are written as soul
lights, because the theme turns every warm light of a template into one and the checks must see what the player sees.

| Room (`cursed_archive_...`) | Kind, size | What it is and why | Numbers (check output) |
|---|---|---|---|
| `stacks` | hall, 3 x 4 modules | The big multi-storey hall. A shelf canyon thirty blocks high: two grand flights (hollow, with reading bays under them) climb the long walls, three hanging gantries cross the void at three heights (one through a tower of books), the floor is a purple-carpeted lane round the tower, the air a canopy of soul lanterns. Why: "vertical grandeur" is the brief's first line for the Archive; the flights are hollow (ribs and a soffit) because a solid wedge would be a brown mountain. | 18718 blocks, 2 doors, light min 5 median 10, 6 spawns |
| `reading_hall` | hall, 2 x 4 | The long, low-feeling hall of different feel: a nave of candle-lit tables with a purple runner, covered aisles along the walls, shelves to the windows, a wheel of candles over every third table. Why: the place where the scholars still sit; eight `seat` and `furniture` markers make the cult read as at work. | 6084 blocks, 4 doors, min 6 median 9 |
| `scriptorium` | hall, 3 x 2 | Six scribes' stations either side of an aisle (lecterns, stools, ink pots), two skeleton scribes who never left (armour stands), a bookbinder's bench with a press, vats of ink under black candles. Why: the brief's lecterns, ink and quills. | 4303 blocks, 2 doors, 2 stands, min 5 median 9 |
| `restricted_section` | treasure, 2 x 2 | A reading nook with a skeleton librarian at the gate and, behind iron bars, the forbidden books chained to lecterns; three chests on plinths among black candles; a gate left ajar. Why: the calm-but-watched treasure room. | 2950 blocks, 1 door, 1 stand |
| `librarians_study` | safe, 2 x 2 | The one quiet room: a fire in a cobbled hearth, armchairs, a desk with a lamp, a bed, a purple rug. Nothing spawns; the rest anchors are the seats, the hearth and the bed. Why: the brief's librarian's study as the safe zone. | 3100 blocks, 2 doors, min 6 median 9, markers hearth, seat x3, bed |
| `burning_stacks` | trap, 3 x 3 | Shelf rows either side of a three-wide spine; three tripwires cross the spine, each guarded by a pair of hidden dispensers of fire charges in the pillars at its sides; the shelves are the fuel; soot and scorched stone say what the hall is for. The spine is clear of anything flammable and the walls are masonry. Why: the brief's burning-shelf trap, with a vanilla redstone trap instead of a custom block. | 6275 blocks, 2 doors, 3 trap markers |
| `card_catalogue` | puzzle, 3 x 2 | Banks of drawers (barrels and chiselled bookshelves) make lanes; four dead-end lanes hold the four sigil plates, the Riddle Stone stands at the south wall and the puzzle door in the east wall keeps two chests. The same blocks and block entity as the Spire's puzzle room. Why: the brief's card-catalogue maze, without a new mechanism. | 4727 blocks, 2 doors, min 5 median 6 |
| `rotunda` | set piece, 4 x 4 | The Rotunda of Chained Tomes: a round hall twenty-eight rows high; in the middle a drum of chiselled bookshelves wrapped in chains and crowned with weeping obsidian; a spiral stair winds round it to two galleries (a bridge leads to each); at the foot of the drum, facing the east door, a lectern of black fire. A cylinder inside the square the grid demands. Why: the place the party remembers; "a lectern of black fire" is in the contract text for this theme. | 25730 blocks, 4 doors, 8 spawns |

### 3.2 Necropolis: a city of the dead

**Palette and why.** Walls `sandstone` (pale, dry, dusty; no other theme uses plain sandstone as its wall), floors `smooth_sandstone`, pillars and
trim `bone_block` (every lintel and door frame is bone), capitals `chiseled_sandstone`, rubble `dripstone_block`, worn walls `terracotta`, worn floors
`coarse_dirt` (grave soil showing through the paving: a city slowly taken back by its own cemetery), rock `sandstone`, wood `acacia_planks` (the
red-brown of coffin planks). Soul lights again. Vanilla gives bone blocks and dripstone no stairs, slabs or walls, so the six shapes of deepslate that
the templates use are written out in `explicit` and take the sandstone shapes on purpose, not by fallback. Not to be mistaken for Verdant (green),
Flame (black) or Radiant (white quartz): nothing here is shiny, green or black. Keywords: bone, skelet, crypt, mummy, lich, wight, undead, ossuar, dead,
death, necro, ghoul, fallen, horseman.

**Lights.** Soul light plus ghost-green froglights let into the burial blocks (a "ghost lamp" every fourth cell of a face) and chandelier wheels over the
crossings, because the stone is pale and the green reads as the dead's own light.

| Room (`necropolis_...`) | Kind, size | What it is and why | Numbers |
|---|---|---|---|
| `catacombs` | hall, 3 x 4 | The Terraced Catacombs, the big multi-storey hall: two crossing corridors of burial blocks with skull niches in every face, a flight up each of two opposite corners to terraces on top of the blocks, four small mausoleums on the terraces and six bridges of dark stone carrying the dead across the corridors at the height of the vault. Why: the brief's catacomb corridors with wall niches of skulls, with vertical grandeur and six doors (several entrances, as the Barrowmaze reviews stress). | 13426 blocks, 6 doors, 4 stands, min 8 median 11 |
| `mausoleum_street` | hall, 3 x 3 | A hall of different feel, open and street-like: two streets of the dead cross in a flagged plaza with a bone obelisk; four tomb houses (a carved front, a raised gate, a step up, two lamp posts; a sarcophagus, candles and in two a chest inside). Why: the brief's mausoleums with carved doors; you can walk in. | 6468 blocks, 4 doors, 7 spawns |
| `bone_bridges` | set piece, 4 x 4 | A round of ledges around an abyss, an island in the middle with a bone obelisk crowned by a skull, four bridges of bone block on piers. Whoever falls lands six blocks down among soul fire, bones and a chest, with a ladder out at each side: the abyss costs health and time, never the way back. Loculi of skulls in the walls above the ledge, stalactites in the ceiling. Why: the brief's bone bridges over an abyss. | 20437 blocks, 4 doors, min 5 median 7 |
| `grave_pits` | trap, 3 x 3 | A churchyard that looks like every other: a ledge of cracked flags, headstones and soul braziers round a square of fresh graves, which are the mod's pitfall tiles; one thin zig-zag of them is safe and leads to an open vault with a chest, and a ghost light hangs on a chain over every turn of the safe way. A wrong step drops three blocks into a spiked chamber (the Spire's own fall) with a ladder out. Same blocks, chamber and checks as the two pit halls of ROOMS-2; the dressing and the hint are new. Why: the brief's grave fields, as a danger room. | 7778 blocks, 3 doors, 4 trap markers |
| `burial_vault` | treasure, 2 x 2 | A lord's tomb sealed with his wealth: a stepped dais with a great sarcophagus (lid shoved aside), two skull-faced knights at its head, chests in niches on both walls, gold lamps in the base of the dais. The calm room: the dead are quiet until the loot is touched. Why: the brief's burial vault. | 3066 blocks, 1 door, 2 stands |

### 3.3 Vampire Castle: Ravenloft

**Palette and why.** Walls plain grey `andesite` (the cold curtain stone of a keep), floors `polished_andesite`, pillars and trim `polished_blackstone`,
every keystone and capital `red_nether_bricks` (a thin crimson line at every arch: the blood in the mortar), worn walls `cracked_stone_bricks`, worn
floors `polished_blackstone_bricks`, rubble `cobbled_deepslate`, rock `deepslate`, wood `dark_oak_planks` (doors, pews, tables and coffins).
**Unlike the other dark themes the lights stay warm**: the soul lights of the templates are turned back into lanterns and torches (`explicit` maps
soul to warm), because a vampire's court is lit by hundreds of candles and gold light on grey stone is the gothic picture; the cold of the place is the stone,
not the flame. It must not be mistaken for Frost (white stone bricks), Storm (grey-green tuff and copper) or Shadow (black walls). Vanilla gives polished
andesite no wall, so that one shape falls back to the brick family's wall. Keywords: bats, fang, baron, castle, coffin, gothic, dracul, thrall, lord,
noble, nosferatu. (`regal` and `throne` were dropped when the Celestial Court of another builder claimed them: a keyword may belong to one theme only.)

**Lights.** Lanterns on the posts of the galleries, chandeliers on long chains hung low, braziers, candelabra, and windows of red or purple glass lit
from behind by a block of shroomlight (a window is also a lamp for the light check).

| Room (`vampire_castle_...`) | Kind, size | What it is and why | Numbers |
|---|---|---|---|
| `great_hall` | hall, 3 x 5 | The big multi-storey hall, three storeys: a nave nine wide between two arcades of columns carrying pointed ribs thirty blocks over the floor; behind each arcade a gallery over the aisle (a flight of eight steps up, a parapet, lanterns), tall windows of red glass in every bay, two long tables of dark oak with candles and cake, chairs, a dais with the high table and a throne under red drapes at the north end, braziers, chandeliers, banners. The one place lit like a feast. Why: the brief's grand dining hall with a long table; the feast hall of the castle in the sources. | 13105 blocks, 4 doors, min 6 median 11, 8 spawns |
| `coffin_crypt` | hall, 3 x 3 | The opposite of the Great Hall by design: low, dark, close, under oak beams. The lord's coffin lies open on a dais ringed by four braziers; four lanes lead to it from the four doors; the four quarters between the lanes are rows of the court's coffins, lids shut or pushed aside, a red candle at every head. Why: the brief's coffin crypts; the lair rule that the coffin sits in a crypt among spawn. | 5799 blocks, 4 doors, min 5 median 8 |
| `chapel` | set piece, 3 x 4 | The Desecrated Chapel: a nave twenty-three wide, pews in two blocks with a red runner, broken and overturned; shattered windows with a lamp showing behind them; a round window of red and black glass over an altar on a dais; a cross hung upside down; red carpet "bleeding" down the steps; caged skulls hanging on chains; a choir loft with an organ of pipes over the door. Why: the brief's chapel desecrated. | 9657 blocks, 3 doors, 2 stands |
| `clock_tower` | set piece, 3 x 3 | A square tower thirty-seven rows high with a well in the middle; two galleries round the walls, three flights (one in each of three corners), the third leading to the belfry: a floor round a shaft with a great bell on a beam and, in each wall, the back of a clock face of white and black glass lit from behind; a chain as long as the tower carries an iron pendulum over a clock face of chiselled stone in the floor; wooden gear wheels on the walls. Why: the brief's clock tower, and the bell for the belfry. | 12160 blocks, 4 doors, min 5 median 11 |
| `blood_court` | trial, 3 x 3 | The throne room and its trial: walk in through either door and the Sealed Archway shuts both; a ring of red sigils lets in the court's thralls and hounds (three zombies and two dire wolves per wave); the barriers fall when the last is dead. A throne on a dais under a great window of red glass, twelve pillars with lanterns, a red runner from door to door, the court's tribute in a chest at the throne's foot. Same blocks, barriers and checks as the trial halls of ROOMS-2; the room and the roster are new. Why: the danger room. | 5448 blocks, 2 doors, no spawn markers (the trial summons) |
| `mirror_gallery` | treasure, 2 x 3 | The long, quiet room where the court once dressed: six tall gilded mirrors that show nothing (panes of black glass), candelabra, a red runner, four guests in golden armour waiting for a dance, and on a dais under the biggest mirror the chests of the house. Nothing spawns and nothing is warm. Why: the brief's mirrors that reflect nothing; it is the calm room. (The brief also asks for a mirror *puzzle*: not built, section 10.) | 3784 blocks, 2 doors, 4 stands |

### 3.4 Haunted Manor: the house that remembers

**Palette and why.** Walls plain `diorite` (white and grey speckle, the plaster of an old house; no other theme uses diorite for its wall), floors
`deepslate_tiles` (the dark tiles of a hall and a cellar), pillars and trim `polished_andesite`, capitals `chiseled_stone_bricks`, rubble `cobblestone`,
worn walls `andesite` (the grey stone showing where the plaster has come away), worn floors `cracked_deepslate_tiles`, rock `stone`, wood `spruce_planks`
(the dark brown of old furniture and banisters). Nothing in the palette is flammable on purpose: planks as walls would burn a whole sector the first time a
lava hall of the shared pool stood next to them, so timber is left to furniture and beams, which the generator keeps away from lava (`lava_fire_problems`
checks every room for flammables within three blocks of lava). The lights are left as the templates draw them: the cold teal of soul lanterns next to
the warm gold of candles, because a house where two kinds of light disagree is a house where something is wrong; windows are lit by pale green froglights
(the moon on the glass). Keywords: ghost, haunt, spirit, doll, locket, specter, manor, lullaby, memor, viola, puppet. (`wisp` was dropped: the Feywild
Grove claims it.)

*Changed during the work:* the first version used `polished_diorite` for the walls, which the Celestial Court of another builder also uses; it moved to
`diorite`.

| Room (`haunted_manor_...`) | Kind, size | What it is and why | Numbers |
|---|---|---|---|
| `grand_stair` | hall, 3 x 3 | The big hall, two storeys: a broad flight from the front door to a half landing, two return flights to a gallery on three sides, a chandelier of soul lanterns over the well, dark portraits and black mirrors, a grandfather clock by the door stopped at the hour of the death. The first room the party sees of the house. Why: the brief's grand stair; the foyer of the haunted-house flow. | 8743 blocks, 4 doors, min 5 median 9 |
| `kitchen` | hall, 2 x 2 | The small, low hall of different feel: furnaces and smokers under a brick hood, a scrubbed worktable with a chopping block and brewing stands nobody should own, herbs and hams on the beams, a larder of barrels, and in the west a cauldron on a stool with a soul fire beside it, bones and a skull on a barrel. The hag cooked here. Why: the brief's kitchen; small rooms between the sudden big ones. | 2471 blocks, 2 doors, min 4 median 7, 0.6% of the floor darker than 5 |
| `nursery` | set piece, 2 x 2 | Striped wallpaper, three cribs with a bell hung over each on a chain, two rocking horses (still rocking), a dolls' house that is a small copy of the room, a tea table laid for four with four dolls at it, a toy chest, alphabet blocks. Nothing moves. Why: the brief's creepy set piece; the sources' dolls' house and rocking horse. | 2581 blocks, 2 doors, min 5 median 6 |
| `ballroom` | trial, 3 x 3 | A parquet floor of spruce and pale stone with a star in the middle, a stage with a grand piano and four chairs, tall windows of moon-green glass, black mirrors, six pairs of dancers in a waltz that stopped. Walk in at either door and the Sealed Archway shuts both; sigils let in whoever the house still has (three skeletons, two zombies and the hag as a witch per wave); the doors open when the last is down. Same mechanism and checks as ROOMS-2's trial halls; the reward chest stands on the stage. Why: the brief's ballroom of dancing ghosts as the danger room. | 5796 blocks, 2 doors, 12 stands (the dancers), no spawn markers |
| `master_bedroom` | safe, 2 x 2 | The one room where nothing is wrong: a four-poster under a canopy, a fire with two armchairs, a wardrobe, a dressing table with a mirror, a rug, candles, a footlocker, a window with the moon in it. Rest anchors are the bed, the armchairs and the hearth. Why: the calm room, and the safest looking. | 2474 blocks, 2 doors, min 5 median 10 |
| `attic` | trap, 3 x 3 | A long room under a pitched roof with tie beams and a skylight of moon-green glass, trunks along the walls, a dressmaker's dummy, a spinning wheel, a crib with a doll in it, dust sheets. The floorboards are the trap: a field of boards that look alike (the mod's pitfall tiles, the same spiked chamber and ladder and checks as ROOMS-2's pit halls), one thin zig-zag of them sound and leading to a trunk on an island in the middle, a lantern on a chain over every turn of the safe way. Why: the brief's attic of sheets and trunks, with the floor as the danger. | 9676 blocks, 3 doors, 4 trap markers |

## 4. The weights (`data/nusmp/mega/template_themes/<theme>.json`)

**Format.** `templates` are the prefixes `nusmp:mega/<kind>/<theme>_` of every kind folder the theme uses (a template with such a prefix may only be drawn
in sectors of that theme: the rule is `TemplateThemes.allows`, and the assembler's use of it is GEN2's work, which I have not seen); `relative` are the
weights `tools/labyrinth/gen_mega_pools.py` puts on them. They are shipped as:

| Theme | Weights (room: relative) |
|---|---|
| cursed_archive | stacks 110, reading_hall 100, scriptorium 100, rotunda 12, restricted_section 10, librarians_study 10, burning_stacks 8, card_catalogue 6 |
| necropolis | catacombs 150, mausoleum_street 150, bone_bridges 12, burial_vault 10, grave_pits 8 |
| vampire_castle | great_hall 150, coffin_crypt 150, chapel 12, clock_tower 12, blood_court 10, mirror_gallery 10 |
| haunted_manor | grand_stair 150, kitchen 150, nursery 12, ballroom 10, master_bedroom 10, attic 8 |

**The real maths, and a problem the lead must decide.** The target of the brief is "a themed sector is about half signature rooms". What the numbers
give depends on how `gen_mega_pools.py` reads `relative`, and it is read in two ways:

1. *The tool as it stands* (`weight = max(1, min(150, round(share * 20 * relative / sum of the relative weights in that folder)))`, the folder shares
   of the `side` pool being fixed: hall 3, set / trap / puzzle / trial / treasure / safe / cavern 1 each, corridor 5, junction 3). Every themed hall of
   all sixteen themes shares the same 60 weight units of the hall folder (3 x 20), so with 69 halls the weights round to 1 to 3: in the merged
   tree my halls get 2 or 3 each (15 to 18 percent of the hall draws inside their own sector), my sets 2, traps 3, trials 2, puzzle 2, safe 2, treasure 1,
   and the signature rooms are 5 to 7 percent of everything a themed sector draws from the `side` pool (Archive 6.9, Manor 6.1, Castle 5.3, Necropolis 4.9;
   a throw-away simulation over the four worktrees, not in the repo). No value of `relative` fixes that, because the sum in the denominator includes the
   other fifteen themes: if one theme multiplies its numbers, the others fall to the floor of 1. It is a zero-sum split of a fixed budget.
2. *The patch proposed by builder A* (`tha_pools_absolute.patch`: a template that a theme file claims takes `relative` as its plain weight, clamped to
   1..150, and is left out of the folder split). Then the numbers above are weights as they stand, and the share of signature rooms among all draws
   of a themed sector, against the 364 units of the generic templates of the `side` pool, is: Archive 49.4 percent, Castle 48.6, Manor 48.3, Necropolis
   47.6 (the other builders' themes come out at 30 to 42 percent with their numbers; the same simulation applied the patch to a copy of the tool). The
   `safe` pool: 36 percent of its draws for the Archive's study and the Manor's bedroom.

**There is a second, larger effect of reading 1 that has nothing to do with my sectors**: the themed rooms stay in the folder split, so they take weight
away from the generic rooms that every OTHER sector (the eleven older themes, and any theme without its own room of that kind) draws from. Measured on this
worktree alone, with only my 25 rooms added, the generic weights of the `side` pool fall from 364 to 241: halls 63 to 28, set pieces 20 to 1, traps 21 to 6,
treasure 21 to 5, trials 20 to 4, puzzles 20 to 10, safe rooms 18 to 6 (corridors, junctions and caverns do not move), 35 generic templates lose weight, and
with sixteen themes the halls and set pieces of an old sector would be a thin sliver beside the corridors. Reading 2 leaves the generic weights exactly as
they were (364), because the claimed rooms are taken out of the split. That is the stronger reason to ship the patch (or an equivalent), and it is needed
whatever share the signature rooms are meant to have.

I tuned for reading 2 (it is the only one that reaches the brief's target) and left the shipped files compatible with reading 1 (the numbers are still
positive whole numbers, the tool still produces valid pools; they simply come out as 1 to 3). **The lead decides which tool ships; if it is reading 1, the
signature rooms are rare and the fix is in the tool or in `mega_pools.json` (raise the hall share), not in my files.** A note on variety: with
only two or three signature halls per theme and weights of 100 to 150 each, a themed sector draws mostly the same two or three halls (about 80 percent
of its hall draws under reading 2); the remaining signature rooms (set pieces, traps, trials) are weighted 6 to 12 so they stay the rarer, remembered
rooms. More halls per theme would be the better cure (section 10).

Why each number: halls 100 to 150 because they are the rooms that define a sector; set pieces 12 because the party should remember them, not meet
them every few rooms; trap, puzzle, trial 6 to 10 (they cost the party time and health); safe and treasure 10 (the study, the bedroom and the vaults are
wanted but a safe room every other room would empty the danger of the road). The Archive's three halls are 110, 100, 100 (not 150 each) so that its share
under reading 2 (49 percent) is the same as the others' despite having one hall more.

## 5. Encounter tables (`data/nusmp/mega/encounters/<theme>.json`)

The format and the rules are those of `radiant.json` and `EncounterDataTest` (8 to 14 entries, every band present, ids unique across all files, message
keys `message.nusmp.mega.enc.<theme>.<entry id>` and ASCII, only `minecraft:`, `tensura:` and `nusmp:` entity ids, cue sounds and particles from
`src/test/resources/mega/enc/known_ids.txt`, the first monster of every entry affordable at the entry's threat). Entry ids are prefixed (`ca_`,
`nc_`, `vc_`, `hm_`) so they cannot clash with another builder's; the check of the four worktrees found no duplicate id and no duplicate lang key.
Faction entries are used sparingly (a faction tag is shared by all themes), and each table's default faction is the one of its theme.

| Table | Default faction, roll bonus, budget | Entries | Design |
|---|---|---|---|
| cursed_archive | cultist, +0, 1.0 | 13: minor `ca_bookworm_drift` (silverfish), `ca_ink_wisps` (vex, dark), `ca_reading_cell` (faction); standard `ca_scholar_cell`, `ca_hush` (evoker + faction, needs noise), `ca_bone_bound` (constructs), `ca_dark_reader` (witch, dark); hard `ca_censors`, `ca_overdue` (living armor + vex), `ca_illusionist_reads`, `ca_book_wyrm` (constructs + cave spiders, noise); ambush `ca_shelf_fall`, `ca_torn_page` (vex + silverfish, dark and noise) | The library's own population. Most of it is cultists and constructs; the noise gate is the place where a library bites (shouting in the stacks is the one thing the librarians never forgave); the ink only moves when the torch is down. |
| necropolis | undead, +1, 1.1 | 12: minor `nc_grave_shamblers`, `nc_bone_rattle` (skeletons), `nc_grave_chill` (strays, dark); standard `nc_gnawing_ghouls` (husks), `nc_procession`, `nc_wight` (wither skeleton, dark), `nc_soul_lights` (vex); hard `nc_death_knight` (living armor + wither skeletons), `nc_bone_golem` (`tensura:bone_golem`, noise), `nc_hungry_dead`; ambush `nc_ossuary_wakes`, `nc_buried_alive` | The dead in the order a visitor meets them: shamblers at the threshold, ghouls in the loculi, a procession, a death knight in the mausoleum, the ossuary that stands up when the party is loud. The roll bonus of 1 makes the whole road hunt a little. |
| vampire_castle | undead, +1, 1.2 | 12: minor `vc_bat_swarm` (`tensura:giant_bat`), `vc_thralls`; standard `vc_hounds` (`tensura:hound_dog`), `vc_dinner_guests`, `vc_gargoyles` (vex), `vc_mirror_guest` (illusioner, dark); hard `vc_crypt_wake`, `vc_blood_hunt` (`tensura:direwolf` + bats, noise), `vc_castellan` (living armor), `vc_noble` (illusioner + vex); ambush `vc_coffin_lids`, `vc_belfry` (bats, noise) | A household: thralls on the stair, hounds in the yard, bats in the belfry, gargoyles on the roof, and above them the nobles, who are few and hard. The hunt and the belfry answer noise. |
| haunted_manor | undead, +0, 0.9 | 12: minor `hm_cold_spot`, `hm_rattling_walls`, `hm_music_box` (quiet only); standard `hm_servants_hall`, `hm_possessed_armour` (living armor), `hm_poltergeist` (vex, noise), `hm_attic_spiders` (cave spiders, dark); hard `hm_the_hag` (witch + faction), `hm_waltz_of_bones`, `hm_mistress` (evoker + vex); ambush `hm_house_wakes`, `hm_slamming_doors` (vex, dark and noise) | Lighter than the other evil roads (budget 0.9, no roll bonus): small rooms and sudden fright, not a hunt. Ghosts are vexes, the vanilla body that flies through walls; the noise gate is the slamming door. |

The two trial rooms have their own rosters inside the template (the Blood Court: zombies and dire wolves; the Ballroom: skeletons, zombies and a
witch); they are not drawn from these tables.

## 6. Faction weights and relic roads

**Faction weights** (`data/nusmp/mega/factions/*.json`, key `themeWeights`; four keys added after `"shadow"` in each of the eleven files, because
`FactionDataTest` wants every file to cover every theme, at least six factions liking each theme, and a 600-room pick with at least five factions where
none exceeds 45 percent). Order of the columns: Archive, Necropolis, Castle, Manor.

| Faction | CA | NC | VC | HM | Why |
|---|---|---|---|---|---|
| aquatic | 0 | 0 | 0 | 0 | nothing here is wet |
| beast | 0 | 1 | 4 | 1 | the castle's wolves and bats; carrion dogs in the graves |
| construct | 5 | 2 | 3 | 4 | bound books and living armour; possessed objects; bone golems; the castellans |
| cultist | 6 | 3 | 3 | 3 | the scholars of the Archive; gravediggers' cults; the court's servants; the hag's circle |
| demon | 2 | 2 | 3 | 1 | what the forbidden books call up; the blood court |
| elemental | 1 | 0 | 1 | 0 | rare |
| goblin | 0 | 0 | 0 | 1 | a scavenger in the manor |
| insect | 2 | 2 | 1 | 2 | bookworms; carrion beetles; attic spiders |
| ogre | 0 | 0 | 0 | 0 | too big for these rooms |
| slime | 1 | 1 | 0 | 1 | spilled ink; grave ooze; damp |
| undead | 3 | 7 | 7 | 7 | the dead are the point of three of the four |

Each theme is liked by seven or eight factions.

**Relic roads** (`data/nusmp/mega/relic_themes.json`, key `explicit`; relics whose name or lore leads to the theme; the six of each theme are relics
that were not in the table before and that no other builder claims):

* Cursed Archive: `the_librarians_maul`, `spellbook_of_concentration`, `book_of_mispells`, `scroll_of_unreadable_spells`,
  `scroll_of_facsimile_copy_bargain_bin_scrolls_6`, `arcane_glass_of_seeing`. Ruleset name "The Forbidden Stacks".
* Necropolis: `corpse_eater`, `dead_blade`, `lich_wand`, `demon_shovel`, `soul_armor`, `soul_shatter_blade`. "The Long Procession".
* Vampire Castle: `blood_chocolates`, `the_devils_due`, `prides_crown`, `dagger_of_betrayal`, `ravens_wing`, `the_mad_kings_crown`. "The Crimson Court".
* Haunted Manor: `lovelorn_locket`, `viola_of_moonlit_spirits`, `phantoms_bracelet`, `phantom_steps`, `wand_o_wisp`, `ring_of_memories`. "The House Remembers".

Why explicit lines and not only keywords: a relic's road is decided by the explicit table first, then by keyword counts, then by a hash; the test wants at
least five explicit relics per theme, and explicit lines are what makes a themed relic lead where its lore says. The keywords are the second net.

## 7. Files and tools

| File | Why it exists |
|---|---|
| `labyrinth/mega/RegionTheme.java` (four enum lines at the end) | the themes must exist in the enum; appended, never reordered (saved data and JSON use the id, not the ordinal), each `(3, 3, Route.ABYSSAL)`: the Crucible of the sector offers exactly three relics, like every other theme, and the theme belongs to the evil road, so (being entry 12 or later) it is `exclusive()` there and never drawn on the Holy road |
| `data/nusmp/mega/themes/{cursed_archive,necropolis,vampire_castle,haunted_manor}.json` | palette, light, mood, danger, keywords (section 3) |
| `data/nusmp/mega/template_themes/<theme>.json` | which rooms belong to the theme and how often (section 4) |
| `data/nusmp/mega/encounters/<theme>.json` | what lives in the sector (section 5) |
| `data/nusmp/mega/factions/*.json`, `relic_themes.json` | who lives there and which relics lead there (section 6) |
| `data/nusmp/structure/mega/<kind>/<theme>_<room>.nbt` (25) | the signature rooms, written by the generators |
| `data/nusmp/worldgen/template_pool/mega/side.json`, `safe.json`, `src/test/resources/mega/catalog.json` | generated by `gen_mega_pools.py` and `mega_catalog.py`; they will conflict with the other builders' versions, the lead regenerates once after the merge |
| `tools/labyrinth/gen_mega_theme_<theme>.py` (4) | one generator per theme; `python3 tools/labyrinth/gen_mega_theme_<theme>.py` writes, `--check` compares with the files on disk and runs every check, `--list` lists, `--force` writes a room that fails a check so it can be looked at |
| `tools/labyrinth/mega_thc_kit.py` | the common command line of the four generators and the props and checks that only these themes need (below) |
| `src/test/java/.../labyrinth/mega/gen/TemplateThemesDataTest.java` | the data test of the template-theme files (section 9) |
| `$S/arc/frag/thc.lang.json` (outside the repo; `$S` is the scratch folder of the job) | 37 lang keys for the lead to merge into `en_us.json` |

**The kit** (`mega_thc_kit.py`), why each part: `Registry`/`main` (the same contract as `gen_mega_rooms1.py`, so four files hold only rooms);
`burns`/`lava_fire_problems` (nothing flammable within three blocks of lava, the rule `gen_mega_rooms2` proves for its lava halls, applied to every
room); `hollow_flight` (a stair on ribs and a soffit instead of a solid wedge); `hung_lantern_under` and `auto_light` (lamps that never land in the
head room of a stair and, with `prune`, are taken out again when they did not help); `gothic_window`, `pointed_profile`, `arch_rib` (windows lit from
behind and pointed ribs); `coffin`, `mirror`, `pew`; `trial_core` and `check_trial` (the same blocks as the Spire's trial rooms, written with the
Blood Court's and the Ballroom's own stage); strict block-state rules for the redstone and mechanical blocks the furnishing kit did not know
(dispenser, tripwire, bell, the mod's gatekeeper door, sigil plate, pitfall tile ...) and the two trial floor blocks, ward rune and spawn glyph, which
the kit is told are plain full cubes you walk on (they are, see their block classes).

## 8. How the rooms are checked

Every room goes through, before it is written: the furnishing kit's own check (doors on module centres and three wide, a 3 x 3 route between every pair of
doors, nothing in doorways or declared routes, attachment of every torch, lantern, ladder, vine and bell, at most twelve armour stands, markers on
reachable floor, the eye-light check: at least 96 percent of the floor at light 5 or more, measured with a model of vanilla light propagation that counts
soul lanterns as 10 and froglights as 15), the room-specific check (trial: keystone box, three ward runes per door, six glyphs, a reachable chest; pit:
exactly one chest, a safe route, a sealed chamber with a way out; puzzle: one plate of each colour, a stone with a valid order, chests unreachable with the
gate shut), and the lava-and-fire rule. Then `tools/labyrinth/mega_check.py` (147 templates in the worktree, 15 pools, 0 errors; its two warnings are
about the older rooms `landing_b` and `hearth_hall_a`) and `mega_arch_check.py <theme>` (survival rules; 0 problems for all four themes). The four
generators were run with `--check` after the last edit and every room reproduces byte for byte. Each room was rendered with `mega_shape_preview.py` and
`mega_walk_preview.py` (kept in the session's scratch folder, not in the repo) and **looked at**; the bugs found that way are why the kit has the rules it
has (a lamp forest under a deck, a door in a pocket with no three-wide route, ribs colliding with door frames, a crib on a door apron). All 25 were
rendered once more from the final files, in their themes' palettes, and nine of them looked at again (the Stacks, the Rotunda, the Reading Hall, the
Catacombs, the Mausoleum Street, the Great Hall, the Clock Tower, the Grand Stair and the Nursery). The previews apply a Python port of the swap rules (`mega_theme_preview.py`, the same
suffix rules as `ThemeSwap.java`) and colour the blocks from a keyword table, so they judge shape, light and the mix of blocks, not textures; the Java swap
itself was not run on a template.

## 9. Tests

What ran (with the shim `jt.sh`, one class at a time at the end, because the lead asked the builders to stop sweeping):

* In one sweep that was stopped when the lead asked (98 of 133 pure test classes under `labyrinth` had finished): all passed except
  `GuardianScalingTest` (two tests, section 11); among them `FactionDataTest` 20/20, `EncounterDataTest` 18/18 (with its hard-coded list of themes
  widened to my four, restored afterwards), `MegaCatalogTest` 2/2, `MegaContractTest` 12/12, `SectorRequestsTest` 8/8, `TemplateThemesTest` 4/4,
  `PaletteSwapTest`, my `TemplateThemesDataTest` 7/7.
* After the palette and keyword changes of section 3: `ThemeDataTest` 12/12 and `RelicThemesDataTest` 7/7 (with their lists widened, the file count 11
  changed to 15 and the three mood sounds added), `RelicThemesTest` 23/23, `SafeCatalogTest` 6/6. With only the shipped list
  widened `ThemeDataTest` failed twice, which is the lead's item 2 in section 11 (the hard-coded count of eleven and the three sounds). The three test files that had to be widened
  were restored from git after every run (`git status` shows only the regenerated catalog and my new test).
* After the last changes to the weights and the encounter text: my `TemplateThemesDataTest` 7/7, `MegaCatalogTest` 2/2 (200 random sectors from the 147
  real templates, all 200 pass the audit) and `EncounterDataTest` 18/18 again (lists widened, restored afterwards).
* **Mutation check** (one test, as asked): in `template_themes/necropolis.json` the prefix `nusmp:mega/set/necropolis_` was changed to
  `nusmp:mega/set/necropolis_x_`; `TemplateThemesDataTest` went from 7 passes to 3 passes and 4 failures (the prefix must end with the theme's name; it
  matches no template; the room `necropolis_bone_bridges` is no longer claimed; the theme has only four signature rooms), then the file was restored and
  checked by its checksum.
* **Not run:** 31 classes of the stopped sweep that had not started (macro, system, seal, trap, trial, vault, safe math, quest parsing, theme parsing:
  they test logic, not my data), the tests that need game classes (CI), the whole project build, anything in the game.

`TemplateThemesDataTest` (my own, pure, Gson and `java.nio` only, reads the source tree) holds the template-theme files to section 5.1 of the contract:
the `theme` field is the file name and a theme file of `themes/` exists; every prefix starts with `nusmp:mega/` and ends with `/<theme>_`; every prefix
matches real templates and only rooms of its own theme; every template that carries a theme's name is claimed by that theme's file and none by two; at
least five signature rooms in at least three kinds per theme; weights are whole positive numbers that address real rooms of the same theme; every
signature room is listed by a pool (so it can be drawn at all); the files are ASCII with at least two prefixes.

## 10. Unverified, and gaps

* **Nothing ran in the game.** Rooms, palettes, the swap of blocks by the Region Theme processor, the light, the sounds, the encounters, the trial and pit
  mechanisms and the redstone trap are all unseen in play. The checks are offline models; the light model in particular is an approximation of vanilla's.
* The palettes are derived by `ThemeSwap` rules that the tests exercise (every palette block has a target; fallbacks are rare and never wood), but I have
  seen the swapped rooms only through the Python port of those rules in the preview tool, in keyword colours, never textured and never in the game.
* The Burning Stacks' trap (a tripwire hook pair, a dispenser of fire charges either side) follows the usual vanilla design; I did not see it fire. That the fire
  stays in the hall (masonry walls, a spine clear of anything flammable) is a design assumption, not a measured fact.
* The trial rooms (Blood Court, Ballroom) and the pit rooms (Grave Pits, Attic) reuse the ROOMS-2 blocks, block entity data and checks unchanged; the
  rosters and what happens when the party dies inside are theirs, untested here.
* The bell block's attachment (ceiling) and facing are checked against the block-state table, not placed in a world.
* Encounter numbers (weights, thresholds, budgets), faction weights and the danger values are my judgement. `FactionDataTest` passed on my worktree
  alone; the 600-room pick must be rerun after all sixteen themes are merged.
* The research was done from search summaries (section 2); no module text was read, so the rooms are inspired by the places, not copies of them.
* Mood sounds: three of them are not on `ThemeDataTest`'s list of checked sounds (section 11).
* The weights depend on the final merged tree and on which pool tool ships (section 4).
* **Not built, and in the brief:** Necropolis: the ossuary with a bone chandelier, the funeral procession hall, the grave field under a cavern sky (so
  the Necropolis has exactly the minimum of five rooms). Vampire Castle: the torture dungeon, the mirror *puzzle*, the separate bat-filled belfry and the
  balcony over fog. Haunted Manor: the cellar, the parlour, the library nook and the long corridor. Cursed Archive: the floating-book chamber and the
  ink-black pool. I stopped here because the lead asked the builders to wrap up; with two or three more halls per theme the variety problem of section 4
  would also be smaller.
* A corridor or junction with a theme look (welcome in the brief) is not built.

## 11. For the lead

1. **`RegionTheme`**: the four lines after `ABYSS` (appended, in this order): `CURSED_ARCHIVE(3, 3, Route.ABYSSAL)`, `NECROPOLIS(3, 3, Route.ABYSSAL)`,
   `VAMPIRE_CASTLE(3, 3, Route.ABYSSAL)`, `HAUNTED_MANOR(3, 3, Route.ABYSSAL)`, each with a one-line Javadoc. Merge textually with the other builders.
2. **Tests that hard-code eleven themes** (they fail as soon as the enum grows, for every builder): `ThemeDataTest` (`SHIPPED`, `assertEquals(11, files.size())`
   which must become the final count, and `MOOD_SOUNDS` which needs `minecraft:item.book.page_turn`, `minecraft:entity.bat.ambient`,
   `minecraft:block.wooden_door.open` from me), `RelicThemesDataTest` (`SHIPPED`), `EncounterDataTest` (`THEMES` and the count of tables).
3. **`GuardianScalingTest` fails with any new theme** (two tests: `everyThemeHasItsOwnTrait` expects the theme's name as its trait in the default branch,
   and `themeColoursAreDistinctRgb` fails because `GuardianScaling.themeColor` returns one default colour for every unknown theme). Not my files. The colours
   of my themes, taken from their theme files and distinct from the eleven in the table and from the other builders' themes: `case "cursed_archive" -> 0x5B3A7A;
   case "necropolis" -> 0xC9B27A; case "vampire_castle" -> 0x7A1230; case "haunted_manor" -> 0x6E8A82;`. For the trait, `traitOf` gives the baseline GRAVITY to a theme
   it does not know, which is allowed; if you prefer a trait that fits, I would pick VERDANT (chains that root) for the Archive, FROST (the grave chill) for the
   Necropolis, BLOOD for the Castle and SHADOW (the lights go out) for the Manor.
4. **Collisions between other builders** that I found while checking mine (the Feywild Grove and the Underdark both use `mud_bricks` for their walls, which
   `ThemeDataTest` refuses; the keywords `anvil`, `brine` and `siren` are each listed by two themes, which `RelicThemesDataTest` refuses). Mine are clean against
   every theme of the four worktrees as they stood.
5. **Lang**: merge `$S/arc/frag/thc.lang.json` (37 keys: the four theme names `message.nusmp.mega.mgd.theme.<id>`, their flavour lines `...<id>.flavour`, and the 29
   encounter messages `message.nusmp.mega.enc.<theme>.<entry id>`). Until it is merged `EncounterDataTest.messagesAreTranslatedOnceTheFragmentIsMerged` prints a
   note and skips the translation check for the four tables; after the merge it checks all 29 keys.
6. **After the merge**: `python3 tools/labyrinth/gen_mega_pools.py` and `python3 tools/labyrinth/mega_catalog.py`; decide the weight reading (section 4: without
   the absolute patch the themed rooms take weight away from the generic rooms of every other sector, 364 to 241 with my four themes alone); rerun
   `FactionDataTest` and the relic tests with all sixteen themes; the relic table's counts per theme (a theme may not hold more than a quarter of all).
7. No config keys, no commands, no Java besides the four enum lines and `TemplateThemesDataTest`; the shared files named in the brief are untouched.
