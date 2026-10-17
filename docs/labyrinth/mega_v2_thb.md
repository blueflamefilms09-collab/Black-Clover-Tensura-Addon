# Mega-Dungeon v2, builder THEME-B (`thb`): the wild and old places of the Holy road (1.59.0)

Four complete themes on the Holy road (`RegionTheme.Route.HOLY`): **Feywild Grove**, **Dwarven Forgehold**, **Crystal Geode** and **Sunken Temple**. Each is a whole sector look and not a
block swap: a palette (theme file), six signature rooms (templates, drawn only in sectors of that theme), a population (encounter table, faction mix, relic routing) and its words (lang
fragment). The only change to the mod's own Java code is four appended enum lines; everything else is data, Python generators and tests.

**Nothing in this document has been run in the game.** Every sentence of the form "this works in the game" is a prediction made from the models the tools use (the walk graph, the light and fluid
checks, the survival-rule checker). Section 7 lists exactly what is unverified. The previews are renders of the template files by the repository's own preview tools, not screenshots of the game.

## 0. What ships

| Theme | Enum line | Colour | Danger | Light | Rooms | Encounter table | Signature weight (hall / set / puzzle or trial / treasure) |
|---|---|---|---|---|---|---|---|
| Feywild Grove | `FEYWILD_GROVE(3, 3, Route.HOLY)` | #D98BE8 | 1.05 | +1 | 6 | 14 entries | 60 / 12 / 16 / 16 |
| Dwarven Forgehold | `DWARVEN_FORGEHOLD(3, 3, Route.HOLY)` | #C98A3C | 0.9 | +1 | 6 | 14 entries | 60 / 12 / 16 / 16 |
| Crystal Geode | `CRYSTAL_GEODE(3, 3, Route.HOLY)` | #C9A0FF | 0.95 | +2 | 6 | 13 entries | 60 / 12 / 16 + 16 / 16 |
| Sunken Temple | `SUNKEN_TEMPLE(3, 3, Route.HOLY)` | #3FB8B0 | 0.9 | 0 | 6 | 14 entries | 60 / 12 / 16 / 16 |

Files (all new unless marked): `data/nusmp/mega/themes/<id>.json`, `data/nusmp/mega/encounters/<id>.json`, `data/nusmp/mega/template_themes/<id>.json`,
`data/nusmp/structure/mega/{hall,set,puzzle,trial,treasure}/<id>_*.nbt` (24 templates), `tools/labyrinth/mega_theme_kit.py`, `tools/labyrinth/gen_mega_theme_<id>.py` (four generators),
`src/test/java/.../labyrinth/mega/gen/TemplateThemesDataTest.java`, the lang fragment (kept outside the repo for the lead to merge), this document. Edited: `RegionTheme.java` (four appended
lines), eleven faction files (`themeWeights`), `relic_themes.json` (section 6.3), three data tests (lists of shipped themes), and the two generated files `worldgen/template_pool/mega/side.json`
and `src/test/resources/mega/catalog.json` (regenerated; the lead regenerates once more after the merge).

## 1. Method shared by the four themes

### 1.1 Research, and its honest limits

For every theme I searched for its sources (setting books and adventures, folklore, game design writing) and recorded the URLs below with what I took from them: ideas, shapes, names of
spaces and the rules of thumb, never text. **The pages themselves could not be opened** (the fetch tool had no name resolution in this session, every fetch failed with a host lookup error),
so everything I took is what the search tool's own summary of the result pages said. Several results are fan wikis, shop listings or auto-generated pages, and the search summaries said so;
I used them only for ideas that two or more results agreed on and I name the weak ones. Where the brief names a space I could find nothing for, the room was designed from the brief alone.

Two sources are used by all four themes: the "Jaquaying the Dungeon" series (loops and more than one connection between levels instead of a single bottleneck:
https://thealexandrian.net/wordpress/13113/roleplaying-games/jaquaying-the-dungeon-part-3-the-philosophy-of-jaquaying ,
https://thealexandrian.net/wordpress/13103/roleplaying-games/xandering-the-dungeon-part-2-xandering-techniques , https://bumblingthroughdungeons.com/?p=939 ) and the existing 122 rooms of the
mod, which are the floor of quality. From the first I took one rule that shows in every big room: a multi-level room has at least two ways between its levels (a ramp and a bridge in the hollow
tree, two flights and a bridge in the coral atrium, two curved stairs plus four bridges in the great geode, two gallery flights plus the pit stairs in the chasm forge).

### 1.2 What a theme is made of

1. **Palette** (`themes/<id>.json`): the nine block-family slots the `nusmp:region_theme` processor swaps. Every room is built in the palette constants of `mega_lib` (deepslate family and dark oak
   family), so the same template is re-skinned per sector; a room therefore uses no architecture block outside the palette (`mega_check` warns about it). The character of a theme is the free vanilla
   blocks on top of the palette (glass, froglights, coral, amethyst, copper, lava behind glass ...), which the processor does not touch.
2. **Rooms** (`structure/mega/<kind>/<id>_<room>.nbt`): built by `tools/labyrinth/gen_mega_theme_<id>.py`, claimed for the theme by `template_themes/<id>.json` (a template whose id starts with
   a claimed prefix is only ever drawn in sectors of that theme, `TemplateThemes`).
3. **Population**: `encounters/<id>.json` (what finds a party), `themeWeights` in the faction files (which native factions settle there), `relic_themes.json` (which relics lead there).
4. **Words**: theme name, one flavour line and the encounter messages, in `thb.lang.json`.

Custom blocks are only the mechanical ones of `mega_lib.CUSTOM` (the sigil plates, the riddle stone and the puzzle door of the puzzle rooms, the keystone and glyphs of the trial rooms, markers).
Everything else is vanilla: no new block, item, entity or game Java class was needed.

### 1.3 The check stack (run by `tools/labyrinth/mega_theme_kit.py`, `run()`)

A room with a defect is not written (`--force` writes it anyway, to look at it; the exit status stays 1). Each room must pass:

* `Hall.check()` of `mega_lib`: a walk graph from the first door, 3-wide routes between all doors, light (minimum 5 at every standing cell and at most 4 percent of cells under the gloom limit),
  fluid leaks, floating decoration, marker rules, at most 12 armor stands, and `shared_problems` = `mega_arch_check` (survival rules: lanterns, candles, rails, ladders, waterlogging ...) +
  the attachment audit + the shape-preview light report.
* `gen_mega_rooms2.check_room`, a second and independent model: doors, 3-wide routes, fluid sealing, flammable blocks within 3 cells of lava, attachment, and for the puzzle and trial rooms the
  mechanic checks (a sigil closet that opens, a trial core with a hostile roster).
* `real_state_problems` (kit): every block state is compared with `vanilla_block_states_1_21_1.json`.

Then `mega_check.py` over the whole template tree and `mega_arch_check.py <id>` per theme. Previews (`--preview`): two isometric cutaways, a plan, optional cuts and 3 to 7 first-person views
per room, rendered with the repository's `mega_shape_preview.py` / `mega_walk_preview.py` through the kit and LOOKED AT; they are in the build session's scratch folder
`$S/previews/thb/` (about 180 PNG files, not in the repository). Two limits of the preview tools that I worked around and that the lead should know when looking at them: the walk viewer blends every
quarter-block voxel of glass, so glass looks nearly opaque unless the kit lowers its alpha (it does, `GLASS_ALPHA`); and lava is dull orange in the viewer, brighter in the game.

### 1.4 Commands

```
python3 tools/labyrinth/gen_mega_theme_<id>.py                  # build, check and write every room (a room with a defect is not written)
python3 tools/labyrinth/gen_mega_theme_<id>.py <room> --preview # one room, then render it in its theme into $THB_PREVIEWS
python3 tools/labyrinth/gen_mega_theme_<id>.py --check          # CI style: build in memory, check, compare with the files on disk (exit 1 on any difference)
python3 tools/labyrinth/gen_mega_theme_<id>.py --table          # markdown table of the rooms (size, doors, markers, light) from the files on disk
python3 tools/labyrinth/mega_check.py ; python3 tools/labyrinth/mega_arch_check.py <id>
python3 tools/labyrinth/gen_mega_pools.py ; python3 tools/labyrinth/mega_catalog.py     # regenerate the pools and the catalog after any room or weight change
```

`<id>` is `feywild_grove`, `dwarven_forgehold`, `crystal_geode` or `sunken_temple`. The generators are deterministic (a fixed seed per room), so `--check` is a byte comparison.

`mega_theme_kit.py` exists so that the four generators are short scripts of rooms: the room registry and runner, the preview wrapper, geometry helpers (discs, rings, blobs, ramps with `ramp_levels`
/ `build_ramp`), and the models' quirks written down once (`sea_lantern` counts as a full block, the copper bulbs count as light only in the previews, never in the checks).

## 2. Feywild Grove

### 2.1 Sources and what was taken

* The Wild Beyond the Witchlight, an adventure set in the Feywild domain of Prismeer, ruled by a coven of three hag sisters, whose encounters all have a non-combat way through:
  https://en.wikipedia.org/wiki/The_Wild_Beyond_the_Witchlight , https://arcaneeye.com/product-spotlights/wild-beyond-the-witchlight-review/ ,
  https://www.syfy.com/index%2ephp/syfy-wire/dungeons-dragons-wild-beyond-witchlight-hither-swamp-preview . Took: whimsy first and danger second, and a hag who bargains (the `hag_bargain` encounter).
* The courts of the Feywild (a Summer court of light and growth, a Winter court of dark and decay, the seasons as temperaments, a plane eternally lit by a sun that neither rises nor sets, walking
  south from the spring court through the seasons): https://blackcitadelrpg.com/archfey-5e/ , https://alittlebithuman.com/dungeons-and-dragons-feywild/ ,
  https://www.greyhawkonline.com/greyhawkwiki/Feywild , https://www.enworld.org/threads/fey-in-5e-dnd.641930/ , https://www.worldanvil.com/w/crossroads-chancellor/a/eladrin-species .
  Took: the court of the four seasons as the set piece, and the permanent glow (giant flowers, wisps, froglights) as the light.
* Fairy ring folklore (a ring made by dancing fey, a mortal who joins the dance dances until exhausted, walking straight across is allowed but never round the ring, iron or thyme frees the trapped,
  in some traditions a door to the fairy world): https://magikitos.com/en/article/fairy-ring-folklore-european-tradition , https://www.norfolkwildlifetrust.org.uk/node/3545 ,
  https://mythus.fandom.com/wiki/Fairy_ring , https://www.wildfooduk.com/articles/fairy-rings/ , https://www.gwct.org.uk/wildlife/species-of-the-month/2012/october .
  Took: the puzzle: eight standing stones in a circle, a well of moonlight in the middle, and a "dance" that must be done in the right order. (The search summary itself warned that the "lost time"
  motif is mostly on weaker sites; I did not use it.)

### 2.2 Signature spaces (8) and props (5)

| Space | Built as | Note |
|---|---|---|
| The feast that is never cleared | `feast_hall` | the hall of the invitation nobody refuses |
| The hollow ancient tree | `hollow_tree` | the grove's cathedral, the big multi-storey hall |
| The moonlit pond | `moonlit_glade` | the hall that is only beautiful, so the rest can be dangerous by contrast |
| The fairy ring | `fairy_ring` | the puzzle |
| The court of the four seasons | `court_of_seasons` | the set piece |
| The pixie hollow (trinkets) | `pixie_nook` | the calm room and reward |
| Mirror pools | partly | the moon pond of the glade and the moon-well of the ring; no separate room |
| The hag's cottage / a fey crossing | not built | the hag is an encounter (`hag_bargain`); the mod already has its own gateway |

Props: the giant glowing flower (a stem of lime glass panes, a bloom of coloured glass round a froglight), the giant toadstool (mushroom blocks with shroomlight under the brim), the will-o'-wisp
(a lantern or soul lantern on a chain), the standing stone with a settled wisp, and glow berries with cherry petals. Cave vine heads are written at age 25 (the last stage) so they never grow on
random ticks; every leaf block is `persistent` so the canopy does not decay.

### 2.3 Palette and WHY (`themes/feywild_grove.json`)

Mud bricks for walls, mossy stone bricks for floors, polished diorite for pillars and frames, amethyst for keystones, mossy cobblestone as rough fill, packed mud and moss block as the cracked
variants, rooted dirt as the deep rock and cherry planks for timber. WHY: pastel and alive where the other roads are cold grey; it must not be mistaken for Verdant (grey mossy bricks, jungle wood,
nothing pink): Feywild is the one theme with pink wood, clay walls and purple accents. Polished diorite has no wall, so one explicit swap hands the polished wall to the diorite wall; every other
shape is derived by the processor.

### 2.4 Rooms and WHY

| Template | Modules | Blocks x z | Doors | Spawn | Patrol | Other markers | Light (min) |
|---|---|---|---|---|---|---|---|
| `treasure/feywild_grove_pixie_nook` | 1x2 | 9x18 | n@4 | 1 | 0 | faction beast, room_center | 7 |
| `hall/feywild_grove_feast_hall` | 2x4 | 18x36 | n@13 e@4 w@31 s@4 | 6 | 2 | faction cultist, furniture, seat x2, room_center | 6 |
| `hall/feywild_grove_hollow_tree` | 3x3 | 27x27 | n@13 w@13 e@13 s@13 | 7 | 3 | faction beast | 5 |
| `hall/feywild_grove_moonlit_glade` | 3x3 | 27x27 | n@13 w@13 e@13 s@13 | 7 | 3 | faction beast | 5 |
| `set/feywild_grove_court_of_seasons` | 4x4 | 36x36 | n@13 e@13 w@22 s@22 | 9 | 3 | faction cultist, room_center | 5 |
| `puzzle/feywild_grove_fairy_ring` | 3x2 | 27x18 | n@13 w@13 | 0 | 2 | faction beast | 6 |

* **feast_hall** (long hall): a table down the middle that is never cleared (cakes, melons, pumpkins, potted blossoms, candles on top slabs), a bench each side, wisps over it, a dais with a
  throne grown into a giant flower, garlands of azalea leaves, coloured glass windows lit from behind. The aisles are four and five wide so the fight (the guests turn on whoever sits down) has room;
  the dais is the high ground and the throne is the one place not to sit.
* **hollow_tree** (big, multi-storey): a trunk nineteen blocks across with four arched entrances, hollow to the sky: a pool and a giant flower on the floor, a spiral ramp up the inside of the bark
  to a gallery, a plank bridge across the hollow, a moon (a disc of glass lit from above) in the shaft. Outside the trunk the corners are glades under a canopy of cherry leaves. The party fights on
  the floor, on the ramp one at a time, on the gallery (high ground) and on the bridge (a place to be pushed off).
* **moonlit_glade** (open and bright): a pond that keeps the moon, an arched plank bridge, a ring path three wide round it, gardens in the margins. It is open on purpose: nothing gives cover and
  what comes out of the pond does not announce itself.
* **court_of_seasons** (set piece): a hall cut into four gardens by a square path and a cross, one garden per season (cherry and petals; green leaves and sunflowers; red wool leaves, pumpkins, fallen
  carpets; snow, blue ice and pale lanterns), a wheel of four coloured wedges where the paths meet, a chandelier, and over each garden a disc of coloured glass lit from above. Doors are a pinwheel,
  so a party enters one season and sees all four. It is the room the party remembers.
* **fairy_ring** (puzzle): the Spire's sigil puzzle (four plates, a Riddle Stone, a puzzle door, a closet of two chests) staged as eight standing stones round a moon-well. The order is 1 0 3 2, the
  "wrong way round" of the 1.54.0 halls (2 3 1 0 and 0 2 1 3). The door and the closet are the shared mechanic (`gen_mega_rooms2.sigil_closet`), same blocks, same block entity.
* **pixie_nook** (calm): a dead end behind one door with a pond the width of the room, a chest on a bed of petals under a giant flower, wisps over the pond, glow berries over the chest. Nothing
  waits in it; the beasts of the grove den in it (faction beast), one spawn marker.

### 2.5 Danger and mood numbers (`themes/feywild_grove.json`)

Danger 1.05, the only Holy theme above 1: whimsical but dangerous, the fey play tricks. Light +1 (wisps and glowing flowers). Mood: `entity.allay.ambient_without_item`, volume 0.4, pitch 0.8, once in
7 two-second pulses (a small chime on the wind). Ambient particle `minecraft:cherry_leaves`. Catastrophe weights collapse 1 / flood 2 / spirit 4 (a trickster, never a plain rockfall).
Keywords (12): fairy, faerie, pixie, wisp, trickster, quickling, glimmer, twilight, petal, blossom, satyr, revel.

### 2.6 Encounter design (`encounters/feywild_grove.json`)

Default faction `beast`, budget scale 1.0, roll bonus 0 (lively, not hostile). The table is small, quick, bright things that are a nuisance alone and a danger in a crowd, with beasts and satyrs for
the real fights and a hag, an illusioner and a Wild Hunt for the tricks. Light and dark steer it: wisps only come to a party with no light, the illusioner only to a bright one, the one brute to noise.
Seven entries carry a message (lang keys `message.nusmp.mega.enc.feywild_grove.<id>`). Bands: minor 2, standard 7, hard 3, ambush 2.

| entry | band | weight | creatures | when / where |
|---|---|---|---|---|
| pixie_swarm | minor | 14 | vex x2-3 | threat <= 8; 2 side(s) |
| thicket_kin | minor | 14 | #room faction x1-2 | - |
| briar_wolves | standard | 12 | tensura:direwolf x2-4 | 1 side(s) |
| giant_bees | standard | 10 | tensura:army_wasp x3-5 | threat >= 2; 2 side(s) |
| satyr_revel (message) | standard | 10 | vindicator x2-3, vex x1-2 | threat >= 2 |
| dryad_wardens | standard | 14 | nusmp:living_armor x2-3 | - |
| will_o_wisps (message) | standard | 8 | vex x3-4 | threat >= 2; needs dark; 2 side(s) |
| briar_snare (message) | standard | 8 | cave_spider x3-5, spider x1-2 | 3 side(s); range 10-22; hunt 500 |
| mirror_court | standard | 8 | illusioner x1, vex x2-3 | threat >= 3; needs lit |
| thorn_ogre | hard | 10 | tensura:horned_bear x1 | threat >= 3; needs noisy |
| hag_bargain (message) | hard | 8 | witch x1-2, #beast x2-3 | threat >= 3 |
| wild_hunt (message) | hard | 10 | tensura:blade_tiger x1, tensura:direwolf x2-3 | threat >= 4; 2 side(s) |
| bramble_tide (message) | ambush | 10 | #beast x5-8 | threat >= 3; 3 side(s) |
| midsummer_revel (message) | ambush | 8 | vindicator x2-3, vex x4-6, illusioner x1 | threat >= 5; party >= 2; 2 side(s) |

(`#name` is a faction tag. Satyrs are vindicators, pixies are vexes, dryads' wardens are living armour: there is no fey creature in the mod, so the vanilla and Tensura creatures stand in. Every
id exists; `EncounterDataTest` resolves the first creature of each entry.)

### 2.7 Weights (`template_themes/feywild_grove.json`)

Claims `hall`, `set`, `puzzle`, `treasure` (`nusmp:mega/<folder>/feywild_grove_`), weights 60 / 12 / 16 / 16. WHY these numbers and what they mean: section 6.1.

## 3. Dwarven Forgehold

### 3.1 Sources and what was taken

* Gauntlgrym and Mithral Hall (Forgotten Realms dwarven holds): the cave network that widens into a central chamber closed by a huge carved door, the Vault of Kings with columns of bright iron showing
  the old kings, the Forgeheart (a core of lava that powers the smithies), doors big enough for dragons, a hold that is "more mine than city" with furnaces and mines below the guest chambers:
  https://forgottenrealms.fandom.com/wiki/Gauntlgrym , https://www.worldanvil.com/w/forgotten-realms-phork37/a/gauntlgrym-article ,
  https://www.worldanvil.com/w/forgotten-realms3A-d26d-international-ghostplasma/a/mithral-hall-settlement , https://www.worldanvil.com/w/not-forgotten-realms-palant/a/mithrall-hall-article .
  (The search summary said most of these are user-built wikis, "community material rather than canon", and that no source names a "Great Forge" or "Hall of Kings"; the Vault of Kings and the forge
  districts are the closest.) Took: the Vault of Kings with its gate, the Forgeheart, the mine under the hall.
* Khazad-dum (a single-file bridge over a chasm with no rail, meant to slow an enemy that breached the outer defences; a brightly lit First Hall above): https://tolkiengateway.net/wiki/Bridge_of_Khazad-d%C3%BBm ,
  https://encyclopedia-of-arda.com/b/bridgeofkhazaddum.html , https://v6.elanillounico.com/tolkienpedia/lugares/first-hall/ . Took: the chasm forge: grate bridges over a pit, the edge as the danger.
* Duergar and hammer golems (grey dwarves of the Underdark who war on their kin; hammer golems unique to dwarven civilisation; a forge guarded by a protector weak to magma, from a video game):
  https://www.worldanvil.com/w/the-forgotten-realms---old-version-theaaronw0/a/duergar--ethnicity , https://forgottenrealms.fandom.com/wiki/Hammer_golem , https://bg3.wiki/wiki/The_Adamantine_Forge .
  Took: rebel duergar as an encounter, constructs on their rounds as the evergreen fight.

### 3.2 Signature spaces (8) and props (5)

| Space | Built as | Note |
|---|---|---|
| The great hall of rune pillars and long tables | `hall_of_pillars` | the hall a hold is remembered for |
| The chasm forge with the Forgeheart | `chasm_forge` | the big multi-storey hall: pit, ring floor, galleries |
| The mines under the hold | `mine_works` | the narrow, ambush hall, the opposite of the great hall |
| The Vault of Kings | `vault_of_kings` | the set piece, with the Gate and a lava channel under a grate |
| The forging of the guard | `anvil_trial` | the trial |
| The clan strongroom | `hoard_nook` | the calm room and reward |
| The great door / Gate | inside the vault | a portal in a partition with its portcullis drawn up |
| The guest chambers of the Maze | not built | no room needed beyond the halls |

Props: the rune pillar (a 2x2 shaft with rings of lit copper bulbs; a copper bulb keeps the `lit` state it is placed with, so the runes glow without wiring), the Great Anvil (iron blocks, a waist of
one block and a plate), lava windows (lava one block thick in the wall behind glass), mine timbers with rails and a lantern on each beam, and the smithy workstation (anvil, grindstone, smithing table,
lit blast furnace, water cauldron). Every lava cell has glass, wall or floor on its four sides and below, so it cannot flow, and no wood stands within three cells of lava.

### 3.3 Palette and WHY (`themes/dwarven_forgehold.json`)

Polished andesite walls, stone-brick floors, waxed cut copper for pillars and door frames with waxed chiselled copper keystones, plain andesite fill, cracked stone bricks and gilded blackstone as
the cracked variants (the gold flecks of a struck seam), stone as deep rock, oak timber. WHY: masonry cut by people who take pride in it. It must not be mistaken for Frost (stone bricks everywhere,
spruce, snow) or Flame (black stone): the copper trim and the oak timber are what only the hold has. Waxed copper does not rust. Copper and polished andesite have no walls, so the explicit map hands
them the andesite wall, and the cold soul lights of the templates become the warm lights of a forge (soul lantern to lantern, soul torch to torch, soul campfire to campfire).

### 3.4 Rooms and WHY

| Template | Modules | Blocks x z | Doors | Spawn | Patrol | Other markers | Stands | Light (min) |
|---|---|---|---|---|---|---|---|---|
| `hall/dwarven_forgehold_hall_of_pillars` | 3x5 | 27x45 | n@13 w@22 e@22 s@13 | 7 | 3 | faction construct, furniture x2, room_center | 4 | 5 |
| `hall/dwarven_forgehold_chasm_forge` | 4x4 | 36x36 | w@13 e@13 w@22 e@22 | 9 | 3 | faction elemental, room_center | 0 | 5 |
| `hall/dwarven_forgehold_mine_works` | 3x3 | 27x27 | n@13 w@13 e@13 s@13 | 8 | 3 | faction goblin, room_center | 0 | 6 |
| `set/dwarven_forgehold_vault_of_kings` | 3x5 | 27x45 | n@13 w@22 e@22 | 8 | 3 | faction construct | 6 | 5 |
| `trial/dwarven_forgehold_anvil_trial` | 3x3 | 27x27 | n@13 s@13 | 0 | 3 | faction construct | 1 | 8 |
| `treasure/dwarven_forgehold_hoard_nook` | 1x2 | 9x18 | n@4 | 1 | 0 | faction construct, room_center | 0 | 5 |

* **hall_of_pillars**: 27 x 45 and sixteen high, two rows of rune pillars holding a grid of copper beams, a nave seven wide with two long tables set for a feast that never came, a gallery four
  wide down each side with eight lava windows (96 lava cells in all: they light the hold the colour of its forge and nobody can reach them), and the Great Anvil at the crossing ringed with furnaces,
  grindstones and smithing tables. Galleries and cross aisles are the 3-wide routes; tables and platform are cover, pillars are corners, and the lava is the thing nobody wants to be knocked towards.
* **chasm_forge** (big, multi-storey): the floor is cut away in a square sixteen wide and six deep with the Forgeheart (a lava tower twenty-eight rows high in glass and copper) at its heart and four
  iron-grate bridges reaching it; the pit is the lower forge (blast furnaces in its walls, anvils, cauldrons, ore, a chest) with stairs at two corners; two galleries (a pinwheel) climb nine blocks
  above the ring on a flight each, with mine-cart rails and an arcade under them. Three levels to fight on: pit, ring, gallery. The rim has a wall all round with a gap where bridges and stairs
  begin; whoever is knocked over lands six blocks down among the forges, not in lava (the only lava is the Forgeheart, behind glass).
* **mine_works**: solid rock with tunnels cut in it: a cross of 3-wide tunnels from the four doors (mine timbers, a lantern on every beam, rails with a brake now and then), a round cavern at the
  crossing (a column of ore into the dome, a loop of rails, two minecart entities that are invulnerable), and four pockets in the corners: a stockpile of raw ore with a hopper and chest, a collapse,
  a spring that fills a pool under dripstone, and the foreman's tool store. The room of ambush and narrow fights; the native creatures are goblins (raiders digging from the lower deeps).
* **vault_of_kings** (set piece): from the north door an antechamber of armoured statues in lit niches leads to the Gate; behind it the nave, where the middle five blocks are a grate of iron over a
  channel of lava (285 lava cells), so the way to the kings is walked over fire; eight kings in niches (a block of gold at each head); three steps up to the first king's tomb, a sarcophagus of copper
  and gold with braziers, chests and a heap of raw gold. The danger is the lava underfoot and the kings' guard.
* **anvil_trial** (danger room): the Spire's trial chamber (keystone seals both doors, glyphs spawn a wave scaled by the party, barriers fall when the last one dies) staged as the forging of the
  guard: nested copper rings in the floor, the Great Anvil on a platform with the keystone hanging over it on two chains, a forge fire in a glass cage in each corner (40 lava cells, unbreakable
  into), four lantern pillars as cover, eight glyphs, the reward in an alcove. The wave is living armour (the anvil wakes what it made), two vindicators and two magma cubes.
* **hoard_nook** (calm): a clan strongroom, a dead end with a window onto the forge's lava (9 cells), two chests in heaps of gold, ore and copper, a counting table with a ledger, a hearth with an
  anvil. The lectern is on the far side of the room so no wood is near the lava. Nothing waits in it.

### 3.5 Danger and mood numbers

Danger 0.9 (craftsmen, neutral to good; the trouble is the stone coming down). Light +1 (forges burn in every hall). Mood: `block.blastfurnace.fire_crackle`, volume 0.4, pitch 0.9, once in 5 pulses.
Ambient particle `minecraft:small_flame` (a drift of embers). Catastrophes collapse 4 / flood 1 / spirit 1. Keywords (12): dwarf, dwarven, anvil, mithril, rune, delver, miner, smith, adamant, ingot,
bellows, mattock.

### 3.6 Encounter design (`encounters/dwarven_forgehold.json`)

Default faction `construct`, budget scale 1.0, roll bonus 0. A hold is a place of craftsmen, so its trouble is what a hold has always had: constructs that keep working after their masters left, fire
that was never put out, raiders from the deeps, a rebel faction and the stone itself. Noise draws the golem, the dark draws the bats. Five entries carry a message.

| entry | band | weight | creatures | when / where |
|---|---|---|---|---|
| cave_bats | minor | 14 | tensura:giant_bat x2-3 | threat <= 8 |
| hold_sentries | minor | 14 | #room faction x1-2 | - |
| goblin_raiders | standard | 12 | #goblin x3-5 | - |
| forge_armour | standard | 12 | nusmp:living_armor x2-3 | - |
| ember_spirits | standard | 10 | #elemental x2-3 | threat >= 2 |
| forge_salamanders | standard | 8 | tensura:salamander x1-2, magma_cube x2-3 | threat >= 2 |
| rebel_duergar (message) | standard | 10 | vindicator x2-3, pillager x1 | threat >= 2 |
| war_gnome_band | standard | 8 | tensura:war_gnome x2-3 | threat >= 3 |
| infested_ore (message) | standard | 8 | silverfish x4-6, cave_spider x2-3 | 3 side(s); range 10-22; hunt 500 |
| stone_giant | hard | 10 | iron_golem x1 | threat >= 3; needs noisy |
| smithy_awakens (message) | hard | 8 | nusmp:living_armor x1-2, blaze x2-3 | threat >= 3; 2 side(s); range 10-18; hunt 300 |
| dark_wings | hard | 6 | tensura:giant_bat x4-5, magma_cube x2 | threat >= 4; needs dark |
| deeps_horde (message) | ambush | 10 | #goblin x6-10 | threat >= 3; 3 side(s) |
| hold_in_arms (message) | ambush | 8 | iron_golem x1, nusmp:living_armor x2-3, #elemental x2 | threat >= 5; party >= 2; 2 side(s) |

### 3.7 Weights (`template_themes/dwarven_forgehold.json`)

Claims `hall`, `set`, `trial`, `treasure` (no puzzle room), weights 60 / 12 / 16 / 16. See section 6.1.

## 4. Crystal Geode

### 4.1 Sources and what was taken

* How the game builds a geode (three layers: an outer layer of smooth basalt, a middle layer of calcite, an inner hollow layer of amethyst blocks, some of them budding amethyst, which alone grows
  buds that become clusters; older pages say the outer layer is tuff): https://digminecraft.com/getting_started/geode.php , https://www.exitlag.com/blog/amethyst-minecraft/ .
  The search summary said the sources disagree on the outer layer and on the generation height; I used smooth basalt for floors and calcite for walls and did not depend on any height.
* Crystal caves as Underdark places (saved for the deeper "Middledark", darkness hazards that contrast with luminous crystals, umber hulks that tunnel out of a wall toward a sound, gelatinous cubes
  as a stock Underdark resident, cave encounter tables): https://www.enworld.org/posts/6680897/ , https://www.randroll.com/caves-caverns , https://artemislena.eu/posts/2024/12/dnd-summary-4.html ,
  https://www.tribality.com/?p=618 . I cannot say which of these pages carried which detail; the search summary mixed them. Took: the umber-hulk-like brute that breaks out of the wall at the
  noise (a ravager), the cube as a slime crowd, bats that hunt by echo.
* Light gradient: from the brief (glow lichen / sea lanterns); realised with sea lanterns laid in floors and set into linings, brighter toward the middle of the echo gallery.

### 4.2 Signature spaces (8) and props (5)

| Space | Built as | Note |
|---|---|---|
| The great geode (dome with a budding lining) | `great_geode` | the set piece and the only multi-storey space of the theme: floor, balcony, crown |
| The echoing gallery | `echo_gallery` | long, dim at the ends, glittering in the middle; chimes between amethyst ribs |
| The prism atrium | `prism_atrium` | tall (18) and bright, the light sorted into colour; one floor level |
| The resonance hall | `resonance_hall` | the puzzle: tuned crystals and a bank of note blocks |
| The shattering of the lining | `shatter_trial` | the trial in an octagonal chamber |
| The gem pocket | `gem_vault` | the calm room and reward |
| Crystal bridges | inside `great_geode` | four amethyst-stair bridges from the balcony to the crown |
| Lightless caves | not built | a darkness hazard from the sources; the geode is the bright theme |

Props: budding amethyst with buds and clusters grown out of the lining (every one through one call that needs a full block behind it, so none can float), the amethyst spire (stepped column with
tinted glass and a cluster crown), the chime (an amethyst block on a chain with a cluster growing from its foot), the cut-geode relief in the wall (rings of basalt, calcite and amethyst round a
hollow of crystals), and the prism (an octahedron of stained glass round a sea lantern, on a chain).

### 4.3 Palette and WHY (`themes/crystal_geode.json`)

Calcite walls, smooth basalt floors, amethyst blocks for pillars and frames, purpur pillar keystones, tuff as rough rock and bamboo for the few wooden parts. WHY: built the way the game builds one:
white shell, dark floor, purple trim; the floors are DARK while the walls are white, which separates it from Astral (quartz, prismarine, birch, a dark night floor). Calcite, smooth basalt, amethyst
and purpur have no stairs, slabs or walls the processor can derive, so the explicit map names every shape (quartz shapes for calcite, polished blackstone for basalt, purpur for the trim). The colour is
a pale lavender (#C9A0FF) and not Spatial's violet (#B070E0), so two theme names are not shown in the same colour.

### 4.4 Rooms and WHY

| Template | Modules | Blocks x z | Doors | Spawn | Patrol | Other markers | Light (min) |
|---|---|---|---|---|---|---|---|
| `set/crystal_geode_great_geode` | 4x4 | 36x36 | n@13 e@13 w@22 s@22 | 9 | 3 | faction construct, room_center | 5 |
| `hall/crystal_geode_echo_gallery` | 2x5 | 18x45 | n@13 w@22 e@22 s@4 | 7 | 3 | faction slime | 6 |
| `hall/crystal_geode_prism_atrium` | 3x3 | 27x27 | n@13 w@13 e@13 s@13 | 8 | 3 | faction elemental | 5 |
| `puzzle/crystal_geode_resonance_hall` | 3x2 | 27x18 | n@13 w@13 | 0 | 2 | faction elemental | 7 |
| `trial/crystal_geode_shatter_trial` | 3x3 | 27x27 | w@13 e@13 | 0 | 3 | faction elemental | 5 |
| `treasure/crystal_geode_gem_vault` | 1x2 | 9x18 | n@4 | 1 | 0 | faction slime, room_center | 7 |

* **great_geode** (set piece): a dome 33 across and 26 high; lining amethyst (264 budding blocks, buds and clusters of every size), shell calcite and basalt, a spire of amethyst and sea lanterns at the
  heart. Three levels: the floor, a basalt balcony at the ninth row on eight columns reached by two curved stairs, and a crown platform round the Heart at the thirteenth, to which four railed bridges
  climb. The danger is height and glitter; the balcony is the archers' place, the crown the only place the Heart is seen whole.
* **echo_gallery** (long hall): forty-one blocks of amethyst ribs on flush pilasters with a chime between every pair, a cut geode in every bay, spires in the side aisles, sea lanterns laid in the nave
  floor, brighter toward the middle (the ends are dim, the centre glitters). The nave is six wide and clear: a party is strung out and a fight comes from both ends; the spires are cover.
* **prism_atrium** (tall hall): eighteen high, a stained-glass prism hung in the middle, four towers of coloured glass (a sea lantern up each heart), a glass disc lit from above in the roof, coloured
  slits in the walls, a floor of eight terracotta wedges in the colours of a spectrum. Wide, bright, open; nothing hides in the dark, so the fight is a skirmish with sprites. **Honest deviation from the
  brief:** this hall is tall but has one floor level; the theme's multi-storey space is the set piece above, not a hall.
* **resonance_hall** (puzzle): the sigil puzzle as a hall of tuned crystals: each plate has an amethyst pillar capped in the plate's colour (a tuning fork), the south wall is a bank of note blocks, eight
  chimes hang over the middle, the stone that tells the order stands under the bells. The order is 3 1 0 2 (not 2 3 1 0, 0 2 1 3 or the fairy ring's 1 0 3 2). Nothing spawns in the hall.
* **shatter_trial** (danger room): an octagonal chamber (the corners cut away and lined with amethyst), a star of calcite and amethyst in the floor, the Resonator (a budding block with a crown of
  clusters on a platform) where the keystone hangs, four spires as cover, eight glyphs, the reward in a niche. The wave is the geode's own: living armour, three vexes, four silverfish.
* **gem_vault** (calm): an oval pocket lined with amethyst and calcite, a plinth with the chest among clusters and purple candles, chimes, sea lanterns set into lining and floor so it glitters.

### 4.5 Danger and mood numbers

Danger 0.95, light +2 (the brightest wild theme: crystals give light and the place glitters). Mood: `block.amethyst_cluster.hit`, volume 0.4, pitch 0.5, once in 6 pulses (a struck cluster, low).
Ambient particle `minecraft:witch` (a purple sparkle). Catastrophes collapse 4 / flood 1 / spirit 2. Keywords (14): geode, gem, amethyst, jewel, shard, facet, opal, sapphire, diamond, emerald,
resonan, chime, echo, glitter.

### 4.6 Encounter design (`encounters/crystal_geode.json`)

Default faction `construct`, budget scale **1.1** (fights are rarer and heavier, as in the astral sectors), roll bonus 0. Everything in a geode is made of the same stone as its walls, so the table is
constructs, slimes and elementals, shulker turrets in the facets (placed close, hunt 0: a shulker never walks), bats that hunt by echo and silverfish that eat the crystal. Noise draws the bats and the
brute, light draws the sprites. Seven entries carry a message. Thirteen entries: minor 2, standard 6, hard 3, ambush 2.

| entry | band | weight | creatures | when / where |
|---|---|---|---|---|
| glint_bats | minor | 14 | tensura:giant_bat x2-3 | threat <= 8 |
| shard_crawlers | minor | 14 | silverfish x2-4 | - |
| crystal_golems | standard | 12 | #construct x2-3 | - |
| gelatinous_cubes | standard | 10 | #slime x3-5 | - |
| spark_elementals | standard | 10 | #elemental x2-3 | threat >= 2 |
| crystal_turrets (message) | standard | 8 | shulker x2-3 | threat >= 2; 3 side(s); range 10-22; hunt 0 |
| echo_wings (message) | standard | 8 | tensura:giant_bat x3-4, bat x3-4 | threat >= 2; needs noisy |
| prism_sprites | standard | 8 | vex x3-4 | needs lit; 2 side(s) |
| umber_brute (message) | hard | 10 | ravager x1 | threat >= 3; needs noisy |
| living_statues (message) | hard | 8 | iron_golem x1, nusmp:living_armor x1-2 | threat >= 3; 2 side(s); range 10-18; hunt 300 |
| cube_flood (message) | hard | 8 | #slime x5-7, #elemental x2 | threat >= 4; 2 side(s) |
| shard_storm (message) | ambush | 10 | vex x5-7, silverfish x5-8 | threat >= 3; 3 side(s) |
| geode_collapse (message) | ambush | 8 | ravager x1, #construct x2-3, #slime x3-4 | threat >= 5; party >= 2; 2 side(s) |

### 4.7 Weights (`template_themes/crystal_geode.json`)

Claims `hall`, `set`, `puzzle`, `trial`, `treasure`, weights 60 / 12 / 16 / 16 / 16. See section 6.1.

## 5. Sunken Temple

### 5.1 Sources and what was taken

* Flooded temples in play (a cistern split by a low retaining wall with overflow drains to the level below; a ruined temple whose plaza flooded and whose lower rooms are submerged, with shafts that
  once gave air and light; puzzles in a recently flooded temple such as a reverse pressure plate that must rise as the water pressure drops): https://www.gamebanshee.com/templeofelementalevil/areasoftheweek/cisternchamber.php ,
  https://dysonlogos.blog/2020/04/20/froghemoth-temple/ , https://www.dndbeyond.com/forums/dungeon-masters-only/230808-imprisoning-water-god/next , https://thealexandrian.net/page/299 .
* Water as a mechanism (a water level that is a lock; valves at different heights on a flooded hall's walls controlling drains; underwater pit traps; areas reachable only through water; air pockets
  as safe havens; sluice gates redirecting water): https://www.greyhawkgrognard.com/2008/11/25/water-water-everywhere/ , https://forums.giantitp.com/archive/index.php/t-551715.html ,
  https://trle.net/walk/213.htm , https://tombraiders.net/stella/walks/TR4walk/details/citadel-7.html , https://www.ludo.guide/guide/1384-ex-voto/walkthrough/chapter-3-the-sunken-monastery/access-the-flooded-halls .
  (Several of these are video game walkthroughs; I used them for the mechanics of water only.) Took: the tidal gate (a sluice that is a puzzle), basins with stepped edges, and the rule that nothing
  traps the player: the brief says walkable.

### 5.2 Signature spaces (8) and props (5)

| Space | Built as | Note |
|---|---|---|
| The nave with canals | `coral_nave` | a dry causeway between two canals of knee-deep water |
| The drowned shrines | `drowned_shrines` | a circle of niches with acolytes round a basin split in four |
| The well | `coral_atrium` | the big multi-storey hall: a deep well, a dry ring, a gallery, a bridge |
| The place the god was kept | `heart_of_tides` | the set piece: a pearl of sea lantern on an island in a round basin |
| The sluice | `tidal_gate` | the puzzle |
| The chapel of last offerings | `offering_nook` | the calm room and reward |
| The cistern with its retaining wall | inside the basins | every basin has stepped, sealed edges |
| Air-pocket rooms | not built | nothing is submerged: every room has air, water is at most 3 deep and only in basins |

Props: the pillar with a band of sea lantern (the god's light still burns), the live reef in the water (corals, fans, sea pickles over a coral block), the acolyte (an armor stand in a turtle shell
helmet with a trident, scenery only), the soul lantern on a chain (the blue light a sea lantern does not give), and dead coral fans on the dry walls (the sea went out and left the grey ones) with vines.

### 5.3 Palette and WHY (`themes/sunken_temple.json`)

Prismarine bricks walls, dark prismarine floors, rough prismarine for pillars and frames, **sea lantern** for the chiselled slot (keystones and column caps: the capitals glow), mossy cobblestone as
fill, mossy stone bricks and dead tube coral block as the cracked variants, dark prismarine as deep rock, acacia as the timber that did not rot. WHY: prismarine is the wall here (in Astral it is only
trim), the floors are dark, the capitals glow. Prismarine bricks and dark prismarine have no walls of their own, so the explicit map hands them the prismarine wall. Light is the baseline's because the sea
lanterns already carry the room.

### 5.4 Rooms and WHY

| Template | Modules | Blocks x z | Doors | Spawn | Patrol | Other markers | Stands | Light (min) |
|---|---|---|---|---|---|---|---|---|
| `hall/sunken_temple_coral_nave` | 2x5 | 18x45 | n@13 w@22 e@22 s@4 | 7 | 3 | faction aquatic, room_center | 0 | 5 |
| `hall/sunken_temple_drowned_shrines` | 3x3 | 27x27 | n@13 w@13 e@13 s@13 | 7 | 3 | faction aquatic, room_center | 9 | 5 |
| `hall/sunken_temple_coral_atrium` | 3x3 | 27x27 | n@13 w@13 e@13 s@13 | 9 | 3 | faction aquatic | 0 | 5 |
| `set/sunken_temple_heart_of_tides` | 4x4 | 36x36 | n@13 e@13 w@22 s@22 | 9 | 3 | faction aquatic, room_center | 4 | 5 |
| `puzzle/sunken_temple_tidal_gate` | 3x2 | 27x18 | n@13 w@13 | 0 | 2 | faction aquatic | 0 | 7 |
| `treasure/sunken_temple_offering_nook` | 1x2 | 9x18 | n@4 | 1 | 0 | faction aquatic, room_center | 2 | 7 |

* **coral_nave** (long hall): a dry causeway six wide runs the length of the hall between two rows of prismarine pillars with sea-lantern bands; either side a canal of knee-deep water (four basins,
  132 water cells), a live reef on their floors lighting the water from below; a dry cross aisle where the side doors come in. The causeway is the 3-wide route; the canals are where things come out of
  (the aquatic faction); the wading party is slowed by them while the enemy is not.
* **drowned_shrines**: a basin in the middle split in four by a dry cross with the god's statue on its plinth where the arms meet, eight pillars with sea-lantern bands, eight shrines in the walls each
  with an acolyte, a disc of blue glass in the roof as the sea's surface, a mosaic floor of faded coral. A fight in a circle: the cross is the way across, the pools slow whoever wades them.
* **coral_atrium** (big, multi-storey): a basin eleven across and three deep (267 water cells in the room) fills the middle of a square atrium twenty-two high, with a dry ring round it and a gallery of
  prismarine four wide, eight blocks up, on a colonnade, reached by two flights and crossed by a bridge over the well. A fall from the top lands in water; every edge of the well is a step, so nobody is
  trapped. Soul lanterns hang from a wheel under a glass oculus.
* **heart_of_tides** (set piece): a round basin twenty across and two deep (352 water cells) in a hall thirty-two wide, four prismarine causeways to an island with the pearl (a sphere of sea
  lantern on a pedestal that lights the whole hall) and four acolytes; twelve pillars round the basin, four glowing tide pools in the corners, planters of live coral. The causeway is the one place to
  hold, since the drowned come up out of the water on either side.
* **tidal_gate** (puzzle): the sluice that keeps the tide out of the vault, staged as a flooded hall: dry causeways from the north and west doors crossing by the stone, the rest knee-deep water with
  four plates on stepping stones, six iron bars as the raised portcullises. The order is 0 3 1 2. Nothing spawns in the hall.
* **offering_nook** (calm): a little basin of knee-deep water lies across the chapel (the wader's way is the water, the dry way two narrow margins), then three steps up to a prismarine altar with the
  chest, two candles, a grey coral fan and two acolytes. Nothing waits in it.

Water rules the models enforce (and why): every water cell has a full block on four sides and below, so a basin cannot leak and no doorway is ever wet; waterlogged plants are only in planters (a
waterlogged non-full block beside plain water reads as an open side to `mega_arch_check`), so the basins use lily pads and live coral blocks instead of seagrass.

### 5.5 Danger and mood numbers

Danger 0.9 (a kind place; the tide does the harm). Light 0. Mood: `block.conduit.ambient`, volume 0.35, pitch 0.8, once in 6 pulses. Ambient particle `minecraft:falling_water`. Catastrophes
collapse 1 / flood 4 / spirit 2 (the highest flood weight of any theme). Keywords (14): tide, tidal, coral, trident, brine, ocean, marine, pearl, nautilus, siren, reef, lagoon, current, sluice.

### 5.6 Encounter design (`encounters/sunken_temple.json`)

Default faction `aquatic`, budget scale 1.0, roll bonus 0. The aquatic and construct tags with the drowned as the evergreen body; **every creature in it fights on land** (the amphibious part of the
aquatic tag, the drowned, lizardmen), because a fish cannot stand on a floor. Light draws the tide spirits, the dark draws the cistern creatures, noise wakes the colossus (a golem). Five entries carry a
message.

| entry | band | weight | creatures | when / where |
|---|---|---|---|---|
| drowned_acolytes | minor | 14 | drowned x1-2 | - |
| pool_crawlers | minor | 12 | #aquatic x2-3 | threat <= 8 |
| temple_drowned | standard | 14 | drowned x3-5 | - |
| prismarine_sentinels | standard | 12 | nusmp:living_armor x2-3 | - |
| reef_lizardmen | standard | 10 | tensura:lizardman x2-3 | threat >= 2 |
| tide_spirits | standard | 8 | #elemental x2-3 | threat >= 2; needs lit |
| lost_choir (message) | standard | 8 | #undead x2-3 | threat >= 2 |
| sunken_priests | standard | 8 | evoker x1, vex x2-3 | threat >= 3 |
| brine_slimes | standard | 8 | #slime x3-5 | 2 side(s) |
| colossus_awakes (message) | hard | 10 | iron_golem x1 | threat >= 3; needs noisy |
| guarded_shrine | hard | 8 | nusmp:living_armor x1-2, #aquatic x2-3 | threat >= 3; 2 side(s); range 10-18; hunt 300 |
| deep_hunt (message) | hard | 6 | #aquatic x3-4, drowned x2 | threat >= 4; needs dark |
| flood_tide (message) | ambush | 10 | #aquatic x5-8, drowned x3-5 | threat >= 3; 3 side(s) |
| drowned_choir (message) | ambush | 8 | evoker x1, vex x3-5, drowned x4-6 | threat >= 5; party >= 2; 2 side(s) |

### 5.7 Weights (`template_themes/sunken_temple.json`)

Claims `hall`, `set`, `puzzle`, `treasure`, weights 60 / 12 / 16 / 16. See section 6.1.

## 6. Shared data

### 6.1 `template_themes`, the pool maths, and what the lead must decide

The format is the contract's (`mega_v2.md` 5.1): `templates` are id prefixes, `relative` are the weights `gen_mega_pools.py` puts on them. The brief asks for "a themed sector about half
signature rooms, tuned with the real pool maths". **That cannot be reached with the maths as they are, for any value, and the attempt has a side effect on the old sectors.** Measured by reproducing
`gen_mega_pools.pool_elements` on the union of the files of the four theme builders (the other builders' files as they stood while I worked; a scratch script, not in the repository):

* The current rule gives each FOLDER a fixed share (`mega_pools.json`, scale 20) and splits it by relative weight into integers clamped to 1..150. With 69 halls in the `hall` folder the integer rounding
  saturates: a themed hall of relative weight 14, 60 or 100 all come out at the same weight 1 or 2 (I tried all three). The signature rooms of a theme are then **4 to 9 percent of all draws and
  15 to 28 percent of the room draws** in its sector, whatever the builders write (mine: 4.4 to 5.7 percent of all draws, 15 to 19 percent of the room draws). "Room draws" are the draws of every
  folder except corridors and junctions.
* The side effect: themed rooms take their weight out of the folder's fixed share, so in the old (neutral) sectors the neutral rooms starve. With every theme builder's files merged the neutral weights per
  folder go: hall 63 to 28, puzzle 20 to 2, set 20 to 1, trial 20 to 2, treasure 21 to 3, trap 21 to 3, safe 18 to 6, cavern 21 to 10. A Gravity sector would lose almost all its set pieces, puzzles and
  treasure rooms.
* Builder `tha` wrote a patch (`tha_pools_absolute.patch`, `claimed` weights: a template under a claimed prefix gets its `relative` value as an ABSOLUTE weight and takes nothing from the folder's share).
  With it the neutral weights stay exactly as before and the same simulation gives my themes **33 to 38 percent of all draws, 47 to 52 percent of the room draws** (hall 66 to 74 percent of the hall
  draws, set 38, puzzle / trial / treasure 43 to 44).

So I wrote the weights as absolute weights, in line with the other builders' scale: **hall 60 (each of the 2 or 3 halls), set 12, puzzle 16, trial 16, treasure 16**. WHY each: a theme has two or three
halls against 28 neutral halls of total weight 63, so 60 each puts a themed hall in about three out of four hall draws of its sector; a set piece (the neutral `set` folder weighs 20) at 12 is about a
third of set draws, so it stays an event; the puzzle, trial and treasure rooms (neutral 20 / 20 / 21) at 16 are about 44 percent of their folder in the theme's sector. Under the CURRENT rule these
values are harmless (they saturate to the same small integers as 14 would) but they do not help; **the lead should adopt the `claimed` patch of `tha` (or an equivalent) and regenerate the pools**,
or the old sectors lose their special rooms. If the lead prefers different numbers, the only place to change is the `relative` map of each theme file, then `gen_mega_pools.py` and `mega_catalog.py`.

Tests: `TemplateThemesDataTest` (new, pure, reads the shipped files): every file parses and names a real theme, every prefix is a real kind folder and claims at least one template, every template
named `<theme>_*` is claimed by that theme's file, the weights are 1..150 and written for exactly the claimed prefixes, each theme has two halls, a set piece, a danger room and a calm room (five or
more rooms), the rule built from the files keeps each themed room to its own sectors and frees every other template, and every themed room is in a pool and in the catalog (a room in no pool is an
orphan that would never be drawn). The six tests pass; I broke a prefix and a weight in a copy to see the test fail, and restored the files.

### 6.2 Factions (`factions/*.json`, `themeWeights`)

Eleven faction files gained four numbers each (0 to 5, the same scale as the existing eleven themes). WHY each row: who would settle in that place.

| faction | feywild_grove | dwarven_forgehold | crystal_geode | sunken_temple |
|---|---|---|---|---|
| aquatic | 2 | 0 | 0 | 5 |
| beast | 5 | 1 | 2 | 1 |
| construct | 2 | 5 | 4 | 3 |
| cultist | 3 | 1 | 1 | 2 |
| demon | 0 | 1 | 0 | 0 |
| elemental | 3 | 4 | 4 | 3 |
| goblin | 1 | 3 | 0 | 0 |
| insect | 4 | 0 | 2 | 0 |
| ogre | 2 | 2 | 2 | 0 |
| slime | 2 | 0 | 3 | 2 |
| undead | 0 | 1 | 0 | 2 |

Beasts, insects and cultists (the revellers) in the grove; constructs, elementals and goblins in the hold; constructs, elementals and slimes in the geode; aquatic, constructs and undead (the lost choir)
in the temple. The faction tag files themselves are untouched. These eleven files will conflict textually with the other theme builders' additions (the same last line); the resolution is to keep
all keys.

### 6.3 Relic routing (`relic_themes.json`): the one REPLACEMENT

* `ruleset_names` (NEW, 4 entries): "The Fey Bargain", "The Hold of Iron", "The Singing Geode", "The Drowned Hymn". WHY: `RelicThemesDataTest` requires a name for every shipped theme's ruleset.
* **REPLACEMENT: 20 relics now lead to my themes** instead of the theme they led to (the old value is in git). WHY: a theme is only reachable by the party's choice of relic (a relic taken in a
  vault names the NEXT region's theme), the table wins over keywords, and `RelicThemesDataTest.everyShippedThemeIsTheTargetOfSomeRelic` needs at least one relic per theme; I moved five per theme, by
  lore, so that the relics that fit a place lead to it. Note for the lead: `Route` is not consulted when a relic names the theme (only the random draw in `RegionTheme` filters by road), so an
  Abyssal party that takes one of these relics reaches a Holy-only theme; I did not change that, and I did not read the rest of that path to see whether anything else filters.

| relic | was | now |
|---|---|---|
| dryads_wedding_band, rose_petal, scimitar_of_the_seasons, amulet_of_the_forest | verdant | feywild_grove |
| staff_of_moonlight | astral | feywild_grove |
| ring_of_power, gold_circlet | flame | dwarven_forgehold |
| time_hammer, heros_shield, overprotective_halberd | gravity | dwarven_forgehold |
| skycrystal_armor, the_power_stone, elemental_hand_wraps | storm | crystal_geode |
| mind_stone, the_mind_stone | astral | crystal_geode |
| leviathan_axe | frost | sunken_temple |
| amulet_of_the_sea_of_stars | astral | sunken_temple |
| palladion, deliverance, mantle_of_selflessness | radiant | sunken_temple |

Each theme also has 12 to 14 `keywords` in its theme file (listed above) for relics that are not in the table. `RelicThemesDataTest` checks that no keyword is listed by two themes.
Reverting any row is a one-line edit.

### 6.4 Lang fragment (`$S/arc/frag/thb.lang.json`, 32 keys, flat, ASCII)

`message.nusmp.mega.mgd.theme.<id>` (the name, read by `LabyrinthGuardianEntity`), `message.nusmp.mega.mgd.theme.<id>.flavour` (one line each, the style of the other builders' fragments), and the 24
encounter messages `message.nusmp.mega.enc.<table>.<entry>` named by the `message` fields of the encounter files (feywild 7, dwarven 5, crystal 7, sunken 5). The script that wrote it checks the keys
against the encounter files, so a text without an encounter or an encounter without a text stops it. The lead merges the file into `en_us.json`; none of the 32 keys exists there or in another
fragment.

### 6.5 Tests

| Test | Change | Why |
|---|---|---|
| `ThemeDataTest` | the shipped list gets the four ids; `SIMPLE_PARTICLES` += `cherry_leaves`, `falling_water`; `MOOD_SOUNDS` += the allay, blast furnace, amethyst cluster and conduit sounds | the lists are the test's allow-lists of ids that are known to exist; each addition is a constant of the game's registries |
| `RelicThemesDataTest` | shipped list | same |
| `EncounterDataTest` | theme list; "twelve tables" became "the default plus every shipped theme" | the count was hard-coded |
| `TemplateThemesDataTest` | new, six tests | section 6.1 |

Run with the repository's JUnit shim (`jt.sh`, `assertTrue/False/Equals/NotEquals/Null/NotNull/Same` only): `MegaContractTest`, `FactionDataTest`, `EncounterDataTest`, `MegaCatalogTest`,
`CatalogRoundTripTest`, `SectorDraftTest`, `SectorRequestsTest`, `TemplateThemesDataTest`, `TemplateThemesTest`, `GuardCatalogTest`, `GuardianScalingTest`, `SafeCatalogTest`, `RelicThemesDataTest`,
`RelicThemesTest`, `ThemeDataTest`: 146 passed, 2 failed. The two failures are `GuardianScalingTest.everyThemeHasItsOwnTrait` and `themeColoursAreDistinctRgb`, and they fail for **any** new theme (an unmapped
theme gets the baseline trait and the default colour). That table is shared guard code, so I left it; the lines for the lead are in section 8. After the last colour change `ThemeDataTest` was run
alone again (12 passed). The real JUnit runs in CI.

Other checks, run on the final files: the four generators with `--check` (build in memory, run the whole check stack of section 1.3, compare byte for byte with the files on disk): all four exit 0
and print nothing in quiet mode; `mega_check.py`: 146 templates, 15 pools, 0 errors, 2 warnings, both on existing rooms (`landing_b`, `hearth_hall_a`, polished blackstone outside the palette) and none
on mine; `mega_arch_check.py feywild_grove`: 6 templates, 0 problems (re-run after the last room change; the other three ids ran clean on the same bytes earlier and again inside the generator checks,
which include the same checker); `gen_mega_pools.py --check`: 0 pool files stale; `mega_catalog.py` rewrote an identical catalog after the last room change. The last room change was
only the cave vine age in `pixie_nook` (section 2.2); the other 23 room files did not change byte for byte (checked by hash before and after for the Feywild rooms). The previews were rendered
while the rooms were being built, so a change of block state after a preview (this one is the only one I remember) is not in the pictures.

## 7. What is unverified (honest list)

Nothing here ran in the game and the Gradle build was not run; the Java edit is four enum lines and one new test class, which compiled only in the shim.

* **The research** is search summaries only; no page was opened (section 1.1).
* **The rooms** were proven by the models only: walk graph and 3-wide routes, light (the shape-preview light model, not the game's), fluid sealing (a water or lava cell with a full block on four sides and
  below), survival rules, attachment. Water and lava were never seen to stay put in the game. Lava and water are where I would look first.
* **Blocks and states whose game behaviour I could not check**: copper bulbs as always-lit runes (a bulb keeps the `lit` state it is placed with; I believe it flips only on a redstone pulse); sea pickles
  and live coral planters (they need water; the planter keeps its own waterlog); `sea_lantern` as the chiselled cap of the Sunken Temple (the processor swaps the slot, so it depends on the palette
  swap of `chiseled`; the previews show it); note blocks in the resonance hall (decor, the bank plays notes if powered; nothing powers it); two `minecraft:minecart` entities in the mine works
  (written invulnerable; a template entity is created at placement); the armor stands with turtle shells (12-stand cap respected, they are scenery); trapdoor decks over the chasm arms; `budding_amethyst`
  keeps growing buds and clusters on random ticks (vanilla), so over time the geode lining grows into the room, at worst small obstacles in a 3-wide route; vines spread by random tick (vanilla rule);
  dripstone under the mine spring may grow.
* **Sector filtering** depends on the game reading `template_themes` (GEN2 wires the loader); without it the rooms are free for every theme and the pools still contain them.
* **Pool shares** (section 6.1) are a simulation on other builders' unfinished files.
* **The crystal hall deviation** (section 4.4): no multi-storey hall in the Geode; its multi-storey space is the set piece.
* **Encounters**: only the shapes were tested (the resolvable ids, the bands, the table parse). Whether the fights are fun and fair at each threat is untested, as is `needs noisy / dark / lit` in play, and
  `hunt 0` for the shulkers.
* **Guardian**: the 2 failing JUnit tests above, and the Mana Core tooltip colour of the new themes (the default grey until the lead adds the four colours).
* **Previews** are renders of the template files; real lighting, particles and the processor's swap are not shown, and lava is duller in them than in the game.

## 8. For the lead

* Enum: four appended lines in `RegionTheme` (conflicts with the other builders' lines; keep the order of each list as written, ids lower case). The three data tests that list themes need the union of
  all builders' ids.
* Lang: merge `$S/arc/frag/thb.lang.json` into `en_us.json`.
* Guard: `GuardianScaling.themeColor` needs `case "feywild_grove" -> 0xD98BE8; case "dwarven_forgehold" -> 0xC98A3C; case "crystal_geode" -> 0xC9A0FF; case "sunken_temple" -> 0x3FB8B0;` and
  `traitOf` a mapping per theme; my suggestion: feywild_grove to VERDANT (a root), dwarven_forgehold to FLAME (a trail of fire), crystal_geode to ASTRAL (falling shards), sunken_temple to FROST (a slow,
  the cold water). `GuardianScalingTest.everyThemeHasItsOwnTrait` then needs those cases in its `switch`.
* Pools: adopt the `claimed` rule (section 6.1), then run `gen_mega_pools.py` and `mega_catalog.py` once after the merge.
* No config keys, commands, registrations, client registrations or quest keys were added.
