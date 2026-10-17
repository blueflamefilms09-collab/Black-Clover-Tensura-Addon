# Mega-Dungeon rooms 2: the grand and mechanical rooms (1.54.0, builder ROOMS-2)

Why this file exists: the Mega-Dungeon (`docs/labyrinth/mega.md`) is assembled from templates that are all built from vanilla blocks by Python, because
nothing here can run in the game and a generator can be checked, regenerated and reviewed. ROOMS-1 builds the furnished rooms. This builder makes the
**grand rooms** (a cathedral rotunda, round halls, colonnades, flooded and burning halls, a mirror hall, three caverns) and the **mechanical rooms** (the
pit, dark, puzzle and trial rooms of the 1.36.0 Spire, redrawn on the 9-grid so they can sit anywhere in a sector). 21 templates, no Java, no config, no
lang keys.

All of them follow the connector contract of `mega.md` (9-block modules, 48 rows, floor row 8, two-thick walls, a 3 x 4 doorway hole with a frame and a
jigsaw on the outer layer at a module centre, `nusmp:mega/side` / `nusmp:mega_door`, markers as data structure blocks). The file name of every template is
its place in the generator's registry; the folder sets the `RoomKind` (`set` SET_PIECE, `hall` HALL, `cavern` CAVERN, `trap` TRAP, `puzzle` PUZZLE,
`trial` TRIAL).

## What is here

| file | what it is |
|---|---|
| `tools/labyrinth/gen_mega_rooms2.py` | the generator: one builder function per template, the checks, the command line |
| `tools/labyrinth/mega_decor_r2.py` | the helper module: geometry, columns, arches, wall dressing, the cavern engine, the analysis model |
| `src/main/resources/data/nusmp/structure/mega/{set,hall,cavern,trap,puzzle,trial}/*.nbt` | the 21 generated templates |
| `docs/labyrinth/mega_rooms2.md` | this file |

Commands (Python 3 and Pillow, no game):

```
python3 tools/labyrinth/gen_mega_rooms2.py                     # build, analyse and write all 21 templates
python3 tools/labyrinth/gen_mega_rooms2.py --check             # CI-safe: writes nothing; exit 1 when an analysis error is found or a file differs
python3 tools/labyrinth/gen_mega_rooms2.py --only cistern_a    # one template (repeatable)
python3 tools/labyrinth/gen_mega_rooms2.py --table --no-write  # the markdown table below
python3 tools/labyrinth/mega_check.py                          # the lead's validator (pool errors are expected until the pools exist)
python3 tools/labyrinth/mega_preview.py mega/set/cathedral_rotunda out.png --y 8:20 --scale 10   # a cutaway; --top for the plan
```

The output is deterministic: every random choice comes from the template's own name (`MT.rng`), nothing depends on Python's hash seed (the two runs
under different `PYTHONHASHSEED` values write the same bytes).

## The templates

| template | size | blocks | doors | spawn / patrol | faction |
|---|---|---|---|---|---|
| `set/cathedral_rotunda` | 45x45 (5x5 modules) | 40043 | n22, e22, s22, w22 | 10 / 3 | cultist |
| `hall/round_hall_a` | 27x27 (3x3) | 9678 | n13, e13, s13, w13 | 8 / 3 | construct |
| `hall/round_hall_b` | 27x27 (3x3) | 10688 | n13, e13, s13, w13 | 8 / 3 | cultist |
| `hall/colonnade_hall_a` | 18x45 (2x5) | 7736 | n4, s4, n13, s13, w22, e22 | 8 / 3 | undead |
| `hall/colonnade_hall_b` | 18x45 (2x5) | 12108 | n13, s4, w13, w31, e22 | 8 / 3 | construct |
| `hall/cistern_a` | 27x27 (3x3) | 10248 | n13, e13, s13, w13 | 8 / 3 | aquatic |
| `hall/cistern_b` | 27x27 (3x3) | 10156 | n13, e13, s13, w13 | 8 / 3 | aquatic |
| `hall/lava_hall_a` | 27x27 (3x3) | 8304 | n13, e13, s13, w13 | 6 / 3 | elemental |
| `hall/lava_hall_b` | 27x27 (3x3) | 8553 | n13, e13, s13, w13 | 8 / 3 | demon |
| `hall/mirror_hall` | 18x27 (2x3) | 4022 | n4, s4, n13, s13 | 6 / 3 | cultist |
| `cavern/fungal_grotto_a` | 36x36 (4x4) | 18703 | n13, e22, s22, w13 | 8 / 3 | insect |
| `cavern/fungal_grotto_b` | 36x36 (4x4) | 19593 | n22, e13, s13, w22 | 8 / 3 | slime |
| `cavern/overgrown_cavern` | 45x36 (5x4) | 27068 | n22, s22, w13, e22 | 10 / 3 | beast |
| `cavern/crystal_cavern` | 27x27 (3x3) | 10531 | n13, s13, e13 | 7 / 3 | insect |
| `trap/pit_hall_a` | 27x27 (3x3) | 8024 | n13, s13, e13 | 8 / 3 (+4 `trap;type=pit`) | goblin |
| `trap/pit_hall_b` | 27x27 (3x3) | 7950 | w13, e13, n13 | 8 / 3 (+4 `trap;type=pit`) | construct |
| `trap/dark_room` | 18x18 (2x2) | 2722 | n4, s13 | 6 / 1 | undead |
| `puzzle/sigil_hall_a` | 27x18 (3x2) | 4260 | w13, n13 | 0 / 2 | cultist |
| `puzzle/sigil_hall_b` | 27x18 (3x2) | 4258 | e4, s13 | 0 / 2 | construct |
| `trial/trial_hall_a` | 27x27 (3x3) | 5348 | n13, s13 | 0 / 3 | undead |
| `trial/trial_hall_b` | 27x27 (3x3) | 5281 | w13, e13 | 0 / 3 | construct |

"blocks" counts the cells the template writes (air is not written). The biggest file is the rotunda at about 118 KB. Every template has the faction marker
once; the rotunda, `cistern_a/b` and `lava_hall_b` also carry one `room_center` marker. The spawn and patrol counts stay inside the contract's 4-10 and
1-3 even for the rotunda; the trial and puzzle halls carry no spawn markers (the archway spawns its own waves, a puzzle is not a fight).

## Reading the rooms (WHY and how each one plays)

### `set/cathedral_rotunda` (SET_PIECE, 45 x 45, four doors)

Why: the owner's reference clip is a round cathedral hall, and a Mega-Dungeon sector needs one room that is not a box and that the party remembers.
It is the only set piece of this builder, so it gets all the detail: a round hall 39 across (a nave 26 wide inside its colonnade) in the 45 x 45 footprint,
with three tiers.

* **Ground floor** (floor row 8): dark floor tile with a chequer of polished tiles in the ambulatory, red carpet runners from the three plain doors to the
  plinth, a ring of red and brown terracotta inlay, and the **candle field**: clusters of 1-4 lit candles in seven colours on the dark floor around the plinth,
  kept off the runners (the doors look straight at the cross). A pointed portico (two pilasters and an arch) frames each plain door.
* **The crimson cross** on a stepped plinth (two steps edged with stairs, a pedestal): a shaft of `nether_wart_block` banded with shroomlight, two arms
  (floor + 22 to 24, eight cells each way, a shroomlight tip on each), a shroomlight capstone and a chain up to the dome. **Fire at its foot**: four caged
  braziers on the plinth (campfires in iron bars), eight campfires round the pedestal and four lava cauldrons.
* **Two colonnaded galleries** at floor + 8 and floor + 16, each with sixteen polished pillars (capitals, bases, a lantern on a corbel on the nave side of
  every pillar of every tier), a balustrade of wall posts and a ring beam. They are reached by **two curved stairs** (A on the ground up to gallery 1, B from
  gallery 1 to gallery 2), eight steps each, three wide in an outer lane with a stringer wall, every rise built from a stair half and a full block so nobody
  has to jump. Both galleries are one ring, so a fight can circle the room above the nave.
* **The walls**: pilasters over every pillar, window slits (iron bars with a glowing shroomlight behind them), twelve niches with an armoured statue and a
  lantern on a chain, banners (red and black) between the windows, string courses of upside-down stairs under each gallery, soul lanterns on chains under the arms
  of the cross, four chandeliers in the dome.
* **The dome**: a flattened ellipsoid (radius 19, rising 10 rows from row 34, so the apex glass is at row 44, the lit backing at 45 and the shell at 46-47) of
  cyan stained glass and tinted glass with ribs of waxed oxidised cut copper (teal). Every cell of the skin that touches the room is glass, so no riser of
  backing shows; behind it is a lit backing and a shell, so the room never sees the void.
* **The north entrance is the clip's first frame**: an iron-grate floor (iron trapdoors laid flush) over a sealed lava pit, between two lava shafts behind
  amber (orange) glass. The lava is bounded by brick on every side and no wood, wool or carpet stands within three blocks of it (the runner stops short of it).

How it plays: the party enters at ground level, sees the whole hall and the cross from the door, fights on the ring between the plinth and the pillars
(spawn markers on the candle field and in the ambulatory), and can flee up either stair onto the galleries where the patrol nodes are. The three-wide route
between all four doors is checked; 2250 standing cells are reachable, 1.8% of them at or under the gloom limit.

### Round halls (`hall/round_hall_a`, `round_hall_b`, 27 x 27, four doors)

Why: rooms in a tabletop dungeon are not all boxes (the reference map has several round rooms), and a hall with a ring of pillars is a different place to
fight in than a rectangle: cover all round, a centre that everybody can see from every door. Both variants are a drum 12 rows high under a ribbed brick dome,
twelve pillars at radius 8 joined by a ring beam with a lantern on each, four doors on the compass points between the pillars so each door looks straight at
the centre, niches with statues, candle altars or campfires, window slits with a glowing backing, banners, skull sconces, and a cornice of upside-down stairs.

* **A (construct)**: a stepped dais (radius 5.6, a ring of stairs round it) with a chiselled obelisk, eight armoured statues in a ring and a chandelier from
  the dome: the party's high ground and cover.
* **B (cultist)**: instead of a dais, a sunken fire pit (radius 4, two blocks deep) with a lava cauldron, campfires, soul campfires and candles and a flight of
  stairs down on each side; the dome has an oculus of sea lanterns. A fight happens round the pit, which is a refuge or a trap.

### Colonnade halls (`hall/colonnade_hall_a`, `colonnade_hall_b`, 18 x 45)

Why: a long hall is the dungeon's nave in miniature: the party enters at one end and sees the whole length at once, with the aisles as cover.

* **A (undead)**: two arcades of pointed arches divide a four-wide nave (rising to row 24 under beams and three chandeliers) from two four-wide aisles (lower,
  row 21, ribs and hanging lanterns). A red carpet runs to a one-step dais with an altar, braziers, banners and a red stained-glass window at the north end.
  Arched alcoves with statues, sarcophagi (with candles on a whole block at the foot) and candle altars line both outer walls; six doors (two at each end and
  one in the middle of each long side).
* **B (construct)**: the first hall's opposite. A reflecting pool (two blocks deep, sealed in a basin, a bridge at the side-door row, landing steps at both
  ends) runs down the nave; the pillars carry free-standing pointed arches (no arcade wall); a ribbed barrel vault covers the whole hall; armoured statues
  stand in niches under chained lanterns; the raised end is at the south, with a high seat of stairs between two braziers and banners. Five doors (one north,
  one south, two on the west side, one east). The party fights on the aisles with the water between them.

### Cisterns (`hall/cistern_a`, `cistern_b`, 27 x 27, four doors) - water

Why: a flooded cistern is the dungeon's water level without the dungeon having to flood. The hall is **five rows of water** (rows 3 to 7) under a stone
floor that exists only on the walkways; everything else is open water you can swim in. (The brief said "the 8 rows under the floor level": seven rows are the
most the 48-row module allows over a solid bottom, five keep two thick basin floor rows and let the sea lanterns shine up.)

* The basin is sealed first, then the water: every source block is bounded by a two-thick floor, the walls and the walkway slabs, and `Analysis.fluid_problems`
  re-checks **every** water cell for an open neighbour and for any contact with a doorway hole. Waterlogged stairs are the landing steps (a swimmer can always
  climb out); pillars standing in the water are plain brick up to a chiselled foot at the walkway.
* **A (aquatic)**: a stone cross of three-wide walkways meeting in an octagonal plaza with a statue; four gate arches on pillars in the water, two corner
  islands joined to the cross by bridges (braziers, banners) and two that must be swum to (**chest** each); pier lamps (a brick post in the water, a wall post,
  a lantern) light the arms; sea lanterns glow up through the water; drip chains with soul lanterns and stalactites hang from the ceiling.
* **B (aquatic)**: a wheel instead of a cross: a plaza, four spokes to a ring bridge (radius 8 to 10, three wide) that circles it, four spokes to the doors,
  gate arches where the spokes meet the ring, eight pillars round the plaza carrying a ring of lanterns, and four 3 x 3 islets in the corner pools (two with
  a chest, two with a statue) that can only be swum to.

How they play: the fight is on the walkways with water on both sides (the aquatic faction comes out of the pools); nothing but the walkways can be stood on
(all spawn and patrol markers are on them), swimming for a chest costs time and leaves the party split.

### Lava halls (`hall/lava_hall_a`, `lava_hall_b`, 27 x 27, four doors) - fire

Why: the dungeon's fire level. Lava is carved from a solid slab (rows 5 to 7 under the floor), so it can never flow; iron trapdoors laid flush make the
bridges (the lava glows through the bars); and **no flammable block stands within three blocks of lava** (the generator proves it), because lava sets wood,
wool and carpet alight.

* **A (elemental)**: two three-wide rivers run the length of the hall, crossed by grate bridges at the door axis and twice more, with a railing of polished
  walls along the banks; in the middle band two glass cages hold a column of lava up to the ceiling; hanging iron cages with skulls, magma cracks in the walls.
* **B (demon)**: a round lake (radius 6) with a crossing of grate bridges to a stepped island, four glass-cage lava shafts in the corners, pillars of crying
  obsidian, hanging cages with skulls, a stair of lanterns and braziers on the rim. The ledge round the lake joins the four doors.

### `hall/mirror_hall` (18 x 27, four doors)

Why: the brief's snake portals ("doors that lead nowhere"). Two portals (`mega_lib.snake_portal`: copper scales, lightning-rod fins, a blackstone head with
end-rod eyes, open trapdoor jaws, a purple membrane) on each long wall, a dark mirror (tinted glass in a polished frame) on each end wall, a strip of dark
glass down the floor and in the ceiling over sea lanterns, end rods over the pillar lines, and a perfect left-right symmetry. The four real doors are at the
ends, two on each; a party that follows a portal finds a wall. The membrane's invisible light block of the portal is swapped for a sea lantern, because the
outer wall layer is the template boundary and an invisible block there would let the room look out into the void (the leak check catches it).

### Caverns (`cavern/*`, organic, three sizes)

Why: the reference map has one green cavern among its rooms; the cavern engine of `mega_decor_r2.Cavern` makes caves that are smooth, reproducible and
provably open from three ingredients: a footprint (wobbled blobs plus a corridor to every door), a floor that rolls gently (flat at the doors, steps of at
most one block, one-block-deep ponds with flat banks) and a dome rule so the cave narrows as it rises. Each door gets a straight lane (3 wide at the mouth,
5 wide inside, open to row 14) so a doorway is never buried; a rock mass with a skirt up to the highest neighbouring ceiling keeps the void out of sight.
Everything on top (ground cover, plants, vines, lichen, dripstone, mushrooms, crystals) is seeded and checked; wherever a feature could close the three-wide
route between the doors it is **tried on the real analysis and rolled back** when it would (`keep_if_route`).

* **`fungal_grotto_a` (insect)**: a mycelium, podzol and moss floor, fourteen giant red and brown mushrooms with shroomlight spots (each shrunk to the
  air the cave has at its site), small mushrooms, a pond, hanging glow berries and lichen; the light is the mushrooms'.
* **`fungal_grotto_b` (slime)**: the same engine on another seed and another outline (two chambers and a neck), a **warped** forest: fourteen warped stems
  with warped-wart caps and shroomlight spots, nylium and moss floor, warped roots and nether sprouts, a larger pond. Different blocks and silhouette.
* **`overgrown_cavern` (beast)**: the green cavern of the owner's map, the biggest cave of the set (45 x 36, ceilings to row 40): a moss floor with carpets,
  azaleas and flowering azaleas, ferns, a pond with lily pads, glow berries and spore blossoms from the dome, vines (hundreds) down the walls.
* **`crystal_cavern` (insect)**: the geode: walls of budding amethyst, amethyst and calcite veined by noise, clusters on every face (a cluster gives light 5,
  large bud 4, medium 2, small 1), seven amethyst spires from the floor (three to eight high, clusters on every side) and seven hanging crystals, a calcite and
  tuff floor.

### Trap halls (`trap/pit_hall_a`, `pit_hall_b`, `dark_room`)

These are the 1.36.0 Spire traps (`docs/labyrinth/f1_traps.md`), redrawn on the grid and with the same blocks and block states.

* **`pit_hall_a` (goblin)**: a 5-wide ledge round a 13 x 13 field of `nusmp:pitfall_tile` (169 tiles: 50 safe, 119 pits, all looking the same) with a treasure
  island in the middle (a chest, candles, a skeleton). One thin zig-zag of safe tiles (a self-avoiding walk, at least 16 long) leads to it, and a lantern hangs over
  every turn of it (look up). A wrong step drops the party three blocks (the Spire's fall) into a dark chamber of pointed dripstone on a lattice (26 spikes)
  with a **ladder** out, so the trap costs health and time but never traps anyone. Four `trap;type=pit` markers sit on the route.
* **`pit_hall_b` (construct)**: a disc of radius 7 (177 tiles: 42 safe, 135 pits) with a different clue: the route is traced overhead as a trail of sea
  lanterns in the ceiling over every second safe tile; eight pillars on the rim, caged skulls over the corners, the ladder at the east rim.
* **`dark_room` (undead, 18 x 18)**: a crypt with two `nusmp:snuffer_glyph` (they put out torches, lanterns and candles near them, forbid placing new ones and
  darken the screen), pillars, sarcophagi, sculk, skulls and webs; **not one light source** in the room (the generator proves it) and a chest in an arched alcove
  behind a cobweb curtain, within six blocks of a glyph. The doors are on opposite corners, so crossing the room means crossing the glyphs' reach.

### Puzzle halls (`puzzle/sigil_hall_a`, `sigil_hall_b`, 27 x 18, two doors)

The Spire's puzzle room (four sigil plates, a Riddle Stone, a puzzle door) as a two-door hall that can be walked through. Four `nusmp:sigil_plate` of the four
colours, one `nusmp:riddle_stone` with the order to step on them, and a `nusmp:gatekeeper_door[kind=puzzle]` (2 wide, 3 tall) in a partition that closes a
4 x 6 closet with **two chests**. Everything the stone scans for (plates and door within 12 cells) is in its reach, and the closet cannot be entered while the
door is shut (both are checked, with the door shut and open).

* **A (cultist)**: doors west and north; the plates round a chiselled medallion; the stone on a plinth by the south wall; bookshelves and banners. Order 2, 3, 1, 0.
* **B (construct)**: doors east and south; the closet at the west end; the plates in a row across the middle; the stone on the north wall; a ring of eight
  lanterns hangs from the ceiling like an orrery; lecterns with books line the south wall. Order 0, 2, 1, 3 (different from A, and not "left to right").

### Trial halls (`trial/trial_hall_a`, `trial_hall_b`, 27 x 27, two doors)

The Spire's trial chamber (`docs/labyrinth/e1_trial.md`): walk through a doorway and the `nusmp:sealed_archway` seals both doors, the glyphs spawn the wave
(scaled by the party), the barriers fall when the last one dies. The archway keystone hangs in the ceiling centre (`radius 13`, `height 13`, `reset 60`), so the
box it watches is the whole hall and the doorway runes on the edge are inside it; `nusmp:ward_rune` fills the sill of each doorway and `nusmp:spawn_glyph` the
floor.

* **A (undead)**: a round dais with a chiselled sigil in the middle (the party's anchor), eight glyphs on a ring, twelve pillars with lanterns for cover, a
  hanging eye over the dais and a chest in an arched alcove opposite the door. Wave: 3 zombies, 2 skeletons (scaled).
* **B (construct)**: a hall of armour: a throne alcove at the north with the chest on a three-step dais, two rows of pillars with lanterns, a black carpet from
  the west door to the east door, eight armoured statues in niches that only watch (the wave is the keystone's roster: 2 living armour, 1 dire wolf), six
  glyphs flanking the carpet.

## Custom blocks and where their usage was copied from

Only the mechanics are custom (the Vanilla Block Primacy Rule); architecture, decoration and every trap that can be built from vanilla blocks is vanilla.

| custom block | used in | copied from |
|---|---|---|
| `nusmp:pitfall_tile` (`safe`, `open=false`) | both pit halls | `gen_spire.py` `build_pit_room()`: the whole floor of the field is the tile, `safe=true` on the route and island, `safe=false` elsewhere; below it a dark chamber of dripstone and a ladder. `docs/labyrinth/f1_traps.md` |
| `nusmp:snuffer_glyph` (`active=false`) | `dark_room` | `gen_spire.py` `build_dark_room()`: the glyph in the middle of a ring of polished blackstone, the chest within its darkness |
| `nusmp:sigil_plate` (`color`, `lit=false`), `nusmp:riddle_stone` (block entity `{id, sequence: [int]}`), `nusmp:gatekeeper_door` (`kind=puzzle`, `half`, `open=false`) | both sigil halls | `gen_spire.py` `build_puzzle_room()` and `gatekeeper_door()`; the door is two columns of `half=lower` with `half=upper` above (3 tall) |
| `nusmp:sealed_archway` (block entity `{id, radius, height, mobs: [{id, count}], reset}`), `nusmp:ward_rune`, `nusmp:spawn_glyph` | both trial halls | `gen_spire.py` `build_trial()` and `archway_nbt()`; the mobs and `reset 60` are the Spire's trial_a / trial_b |

Deliberate differences from the Spire: the puzzle closets hold two chests and no relic pedestal (the brief asks for a chest; relics are earned elsewhere in the
Mega-Dungeon); the trial halls place no `nusmp:magic_barrier` (the archway writes the barriers into the doorways at run time, exactly as in the Spire).

## The analysis (why the generator can say "this room works" without the game)

`mega_decor_r2.Analysis` reads a finished template back as a small model of the game rules and `check_room` asks it the questions below for every template;
`gen_mega_rooms2.py` exits 1 on any error and `--check` makes that a CI test. Every check has a negative control (the room is mutated to break the rule and the
check is seen to fail).

* **Doorways**: every socket has a standing cell inside it; every door reaches every other; a **three-wide route** joins all doors (every node on it has a
  standable cell in its whole 3 x 3 neighbourhood); all `spawn`, `patrol` and `room_center` markers stand on floor the party can reach.
* **No void**: a see-through flood from the inside finds every boundary cell that can be seen from the room; none may exist (the shell never leaks).
* **Fluids**: every water and lava cell (and every waterlogged block) has solid blocks on all sides it could flow to, none touches a doorway hole.
* **Fire**: no flammable block within three cells of lava.
* **Light**: block light is propagated with vanilla's rule (one level per cell, from the table of emitters) and the share of reachable floor cells whose
  eye cell is at or under the gloom limit (block light 4, `GloomClock`; the case of a player with no light in hand) is reported; more than 12% is a warning,
  the dark room is exempt. (Worst cases now: `overgrown_cavern` 11.6%, `colonnade_hall_a` 10.0%, `round_hall_a` 7.6%; every other room is under 6%.)
* **Attachment** (new, `attachment_problem`): the `canSurvive` rules of every attached vanilla block the generator uses, because a template is placed with
  "known shape" (no neighbour updates) and a floating lantern looks fine until a block next to it is broken: ladders and wall banners and wall torches need a
  solid block behind them, hanging lanterns something to hang from (a chain counts, a bottom slab does not), standing lanterns and candles a block that can
  carry their centre (not a bottom slab, not a stair), carpets a block under them, dripstone rock to grow from, amethyst a block behind, vines, lichen and sculk
  veins a block behind **every** face that is set, cave vines and hanging roots a block above, plants the right soil. It found 43 real defects in the first
  version (floating niche lanterns, a ladder that faced the wrong way, torches on lightning rods, candles on slabs, sculk veins on doorway cells) and they are fixed.
* **Mechanics** (kind-specific): pit halls (a safe route from the ledge to the chest, most of the field is pits, every pit has a sealed chamber below it, all of
  the chamber's free cells reach the ladder, spikes exist, the ladder has a wall behind it); puzzle halls (one plate of each colour, a valid sequence, every
  door `kind=puzzle`, the chests unreachable with the door shut and reachable with it open, everything the stone scans for inside ±12 cells); trial halls (the
  keystone box covers the glyphs and runes, the doorway runes are on the box edge); dark room (no emitter, a chest within six cells of a glyph).
* **Counts**: at most 16 armor stands per room; exactly one faction marker; trial and puzzle halls carry no spawn markers.

The light model is an approximation: vanilla's light also depends on opacity of each block and on the sky, neither of which exists here.

## Design decisions worth knowing

* **Palette**: architecture uses only the `mega_lib` palette constants (deepslate and its families), so a Region Theme restyles it; glass, copper, candles,
  lanterns, lava, water, chains, bars, sea lanterns, shroomlight, terracotta (the rotunda's red inlay) and plants are the neutral accents. `mega_check` finds
  no architecture outside the palette in these templates.
* **No invisible blocks where they could leak**: the window slits and portal membranes sit in the boundary layers, so their backing is a visible glowing
  block (shroomlight or sea lantern), never a `light` block next to the template edge. `light` blocks are used only inside open air (caves, lava halls).
* **Rooms that can be shrunk to fit**: the mushrooms, crystals and dripstone of the caves are tried on the real analysis and rolled back when they would close
  the route between the doors; the mushrooms shrink to the air the cave has at their site.
* **Sockets**: every doorway is `MT.socket` at a module centre; unused sockets become their niche and are capped by the lead's `cap` pieces (the contract).

## Recommended pool weights (relative to a plain furnished hall at 10)

The assembler decides how many of each kind a sector may have (the rotunda is a SET_PIECE: at most one per sector); these are relative weights inside a pool
for that kind.

| pool / kind | templates and weight |
|---|---|
| SET_PIECE (`set`) | `cathedral_rotunda` 1 (rare: it is the room people remember; use it once per two or three sectors at most) |
| HALL (`hall`) | `round_hall_a` 6, `round_hall_b` 6, `colonnade_hall_a` 6, `colonnade_hall_b` 5, `cistern_a` 4, `cistern_b` 4, `lava_hall_a` 4, `lava_hall_b` 4, `mirror_hall` 4 |
| CAVERN (`cavern`) | `fungal_grotto_a` 5, `fungal_grotto_b` 5, `overgrown_cavern` 3, `crystal_cavern` 4 (the green cavern is one per sector at most; weight it by the theme: grottos and the overgrown cavern in green regions, the crystal cavern in dark ones) |
| TRAP (`trap`) | `pit_hall_a` 3, `pit_hall_b` 3, `dark_room` 3 (they drain health and light: about one per sector, like the Spire) |
| PUZZLE (`puzzle`) | `sigil_hall_a` 3, `sigil_hall_b` 3 |
| TRIAL (`trial`) | `trial_hall_a` 3, `trial_hall_b` 3 |

## For the lead

* **Block-state schema bug in `mega_lib.py` (not mine to edit)**: `_SCHEMA` lists `(r".*_door", {...})` before `(r"nusmp:.*", None)`, and the path of
  `nusmp:gatekeeper_door` matches `.*_door`, so `mega_check` rejects the puzzle door's `kind` property (12 errors in the two sigil halls). Fix: move the line
  `(r"nusmp:.*", None),` to the first position of `_SCHEMA`, or start `_check_cached` with `if block.startswith("nusmp:"): return None`. `mega_decor_r2.py`
  applies the first fix to its own copy at import time so the generator can run today; the file on disk is identical either way.
* **Pools**: the pool builder must pick up the folders `set`, `hall`, `cavern`, `trap`, `puzzle`, `trial` (the weights above). `mega_check` reports 74
  "pool nusmp:mega/side does not exist" errors for these templates until that pool exists; nothing else is wrong.
* **Catalog**: run `python3 tools/labyrinth/mega_catalog.py` after the merge (this builder did not write `src/test/resources/mega/catalog.json`; it exports
  these templates without error: sizes, 2 to 6 sockets each, the markers above).
* **The attachment rules are reusable**: they found 43 defects in this builder's first version (floating lanterns, a backwards ladder, torches on lightning
  rods, candles on slabs), and every builder places the same kind of blocks. To scan any template of the Mega-Dungeon for blocks that nothing holds:

  ```
  python3 - <<'EOF'
  import sys; sys.path.insert(0, "tools/labyrinth")
  import glob, mega_decor_r2 as D
  from template import load
  for p in sorted(glob.glob("src/main/resources/data/nusmp/structure/mega/**/*.nbt", recursive=True)):
      t = load(p, p)
      for pos, c in sorted(t.cells.items()):
          r = D.attachment_problem(t.cells, pos, c)
          if r: print(p, pos, c.block, r)
  EOF
  ```

  (Importing `mega_decor_r2` also applies the schema fix above to the running process, which is harmless.)
* No Java, no config key, no lang key, no quest key, no registration: the templates are plain data.

## What is not verified (nothing ran in the game)

* Nothing was loaded in Minecraft. Placement of the templates by the jigsaw assembler (rotation of the chests, banners, ladders and armor stands), the
  `block_ignore` and theme processors on these templates, and the real light, fluid and collision code are **assumed** to behave like the models here.
* The light model is vanilla's rule applied to a table of emitters; whether a particular spot is bright enough at night, with the gloom clock's real
  thresholds and with players carrying their own light, is not tested. The 12% limit is a heuristic.
* The attachment rules are written from the vanilla `canSurvive` code as remembered for 1.21.1; a block that survives in the model might still pop in the
  game (and a model failure might be a false alarm). The custom blocks' collision shapes were read in their Java classes (`PitfallTileBlock` is a full block
  when closed; the riddle stone has the default shape).
* The walking model (steps of one block up, up to three down, two blocks of head room, a one-block jump) is a simplification of player movement; stair and
  slab steps, ladders and the curved stairs of the rotunda are walkable in the model but were not walked.
* The pit fall (three blocks into a spiked chamber), the snuffer glyphs' real radius, the Riddle Stone's scan box and the sealed archway's box are checked
  against the numbers read from the Java classes and the Spire docs; the behaviour itself was not run. Both trial rooms use the Spire's mob rosters
  (`nusmp:living_armor` and `nusmp:dungeon_direwolf` are registered in `DungeonEntities`; the chest loot table `nusmp:chests/labyrinth_trial` exists).
* The rendered pictures are made by a scratch viewer and the lead's `mega_preview.py`; both draw colours from a keyword table, so a block's real texture and
  shading were never seen.
* Structure size: every template is below 120 KB compressed; the placement cost of the biggest ones (the rotunda at 40 thousand cells, the cisterns with five
  thousand water cells) in a world was not measured.
