# Unique Skill Lines: the book, the backgrounds and the loot (parts E, F, H of `docs/skilltree/lines.md`)

Builder `lines-book` (1.82.0). Everything here is an ADDITION next to the 1.62.0 items system (`docs/skilltree/items.md`), except the items marked REPLACEMENT in section 1.
Nothing here ran in the game: see section 10 for what was previewed, type-checked and unit tested, and what only CI or a player will tell.

## 1. New and replacement (owner rule 5)

| Part | Status | What |
|---|---|---|
| 3D model `skill_book_3d.json` (22 cube elements, full display block) and its per-franchise bodies `skill_book_3d_<franchise>.json` | NEW | nothing like it existed (the book was a flat sprite) |
| 12 cover textures `textures/item/skill_book_3d/<franchise>.png` (+ `generic.png`), the atlas `base.png` | NEW | |
| Models `skill_book_st_<franchise>.json` (separate_transforms per franchise) | NEW | |
| Registered model `models/item/skill_book.json` | REPLACEMENT | the owner's brief says the registered item's model may switch to the 3D one. The old text (`item/generated`, layer0 `skill_book`, layer1 `skill_book_cover`) is KEPT as `models/item/skill_book_2d.json` and is still the inventory icon; `skill_book.png` and `skill_book_cover.png` are untouched. |
| `assets/nusmp/skill_books.json` (client table: book id, line, colour, franchise, names) | NEW | |
| Item property `nusmp:franchise`, the cover colour fall-back, the tooltip for a line the client does not know | NEW | `BookClient`, `BookLook` |
| 8 hand-built route backgrounds + the generator for all the others (`gen_backgrounds.py`) | NEW | no background existed; the `gui_background` field is new in lines.md part A |
| `LineAssetsTest`, `BookArtAssetsTest`, `BookLookTest` | NEW | `ItemsAssetsTest.theSkillBookModelNamesBothLayersInOrder` now reads `skill_book_2d.json` (same assertions) |
| Loot of any line from the pocket-realm vault chests (`nusmp:rift_books`) | NEW | the first two doors (Abyssal monsters, decennial Guardians) already existed since 1.62.0 |
| `LootDrops.routeBook`: the per-drop list and weight array | REPLACEMENT | replaced by the cached cumulative table `RouteTable` + `WeightedTable` (same proportions, section 8); the old `RoutePick` class is KEPT (tests and the self-test still use it) |
| Pity (never an owned line twice in a row) | NEW | the old code had no such notion |

## 2. The 3D book model (`tools/lines/book_model.py` is the source, `gen_book_textures.py` writes the json)

The book stands upright in model space: front cover looks SOUTH (+Z, like the front of every item sprite, so the `fixed` and `head` rotations of vanilla sprites apply unchanged), spine on the WEST (-X), pages on the
EAST. 9 wide, 13 high, 4 thick (x 3.5..12.5, y 1.5..14.5, z 6..10 of the 16-unit model space), centred on (8, 8, 8) so scaling and rotating happen about the middle of the book.

| Elements | What and WHY |
|---|---|
| `front_board`, `back_board` (0.8 thick) | the thick cover; tinted leather. The back board repeats the front art so the book is a book from every side. |
| `spine_1..3`, `spine_band_1..3` | a three-step spine (wide to narrow: z 6.2..9.8, 6.6..9.4, 7.2..8.8) reads as a rounded back from above; three raised gold bands across it are the "bound tome" silhouette cue |
| `pages_1..3` | three boxes, narrow-wide-narrow in thickness (z 7.7..8.3 sticks out furthest at the fore-edge), so the page block bulges like a real book and is INSET from the boards (x <= 12.3 vs boards 12.5, y 2.1..13.9 vs 1.5..14.5). The fore-edge texture has one sheet per pixel column and a darker gap every fourth, i.e. page lines. |
| `ribbon`, `ribbon_tail` | the marker hanging out of the foot of the pages; grey cloth tinted with the line colour, with a notched tail |
| `cap_top_west`, `cap_bottom_west`, `cap_top_east`, `cap_bottom_east` | metal corner caps wrapping the full thickness (z 5.7..10.3) and 0.2 proud on every side |
| `clasp_strap`, `clasp_tongue`, `clasp_back_plate`, `clasp_catch`, `clasp_stone` | a strap round the fore-edge, a tongue on the front cover with the pale-cyan stone, a catch on the strap (bronze) |

**Only visible faces are modelled** (a face on a surface that is covered by another element is omitted): fewer quads, no z-fighting, and no cost for anything the player cannot see.

**Texture layout** (`book_model.py`, shared by the generator and the previewer so they cannot disagree): the atlas `base.png` is 64x64, 4 px per uv unit (pages vertical and horizontal, paper, ribbon cloth, the stone, gold plate, bronze plate: the gold plate is stretched
whole over every metal face, so its bevel and rivets frame each plate). The franchise cover is 128x128, 8 px per uv unit: front 72x104 (9x13 units), spine 32x104, the cover's top/bottom edge strip and fore-edge strip.

## 3. Display transforms: the Blockbench-style maths

A `display` entry is `rotation [rx, ry, rz]` in degrees, `translation [tx, ty, tz]` in 1/16 block (the game clamps to +-80), `scale [sx, sy, sz]` (clamped to 4). The game (`ItemTransform.apply`, then `ItemRenderer.render`) places a model vertex `v` (in 0..16 model units) at

```
p = Pose * T(tx/16, ty/16, tz/16) * Rx(rx) * Ry(ry) * Rz(rz) * S(sx, sy, sz) * T(-0.5, -0.5, -0.5) * (v / 16)
```

read right to left: the model is first moved so the middle of its 16-unit cube is the origin, scaled, rotated (`Quaternionf.rotationXYZ` = Rx*Ry*Rz as a matrix, so on a vector Z is applied first, then Y, then X), and only then translated, so the
**translation is in the frame of the surrounding pose, not in the rotated frame of the book**. For a LEFT-hand context `ItemTransform.apply` first negates `tx`, `ry` and `rz`: the same numbers in both hands give the mirrored pose.
That is why `thirdperson_lefthand` and `firstperson_lefthand` carry the SAME numbers as the right-hand entries (vanilla's own handheld model writes the left hand pre-mirrored, which the game then un-mirrors; that gives the same pose in both hands, wrong for a book you want turned
outward). A rotation is mirrored, the book is not reflected, so the book keeps its handedness: the observer sees the spine on the left of the front cover in both hands.

`Pose` is what the game wraps around the item, and it fixes which way the display axes point:

| Context | Pose (what the game does before the display transform) | Display axes in the world |
|---|---|---|
| `firstperson_*hand` | `translate(+-0.56, -0.52, -0.72)` in the camera frame (x right, y up, z back toward the eye); no rotation | x right, y up, z toward the eye |
| `thirdperson_*hand` | arm pivot `(+-5, 2, 0)/16`, arm `xRot = -18 deg` (holding pose), then `Rx(-90) Ry(180) translate(+-1/16, 0.125, -0.625)` (`ItemInHandLayer`) | display x = the player's right, display y = FORWARD, display z = up the arm toward the shoulder |
| `ground` | `translate(0, bob + 0.25 * scale.y, 0)`, the entity's spin about Y | x, y, z as the world |
| `fixed` (frames) | frame turned to the wall, `translate(0, 0, 0.4375)`, `scale(0.5)`; the viewer is on the -z side of the item | which is why every sprite uses `ry = 180` |
| `head` | `translate(0, -0.25, 0) Ry(180) scale(0.625, -0.625, -0.625)` on the head | the same `ry = 180` convention |
| `gui` | (not used: the GUI shows the old sprite, section 4) | |

**How the numbers were chosen** (`tools/lines/preview_book.py` implements exactly this pipeline, so the numbers were solved for directions, then looked at):

| Context | rotation | translation | scale | Result (front cover / top of the book / spine, in the world: x right of the player or the camera, y up, z BACK) |
|---|---|---|---|---|
| `thirdperson_righthand` (= left) | 60, 150, 0 | 0, 1, 3 | 0.5 | right hand: front (0.50, -0.18, -0.85) = forward and outward, top (0.00, 0.98, -0.21) = up, spine (0.87, 0.10, 0.49) outward-back. The left hand is the mirror: front (-0.50, -0.18, -0.85). A grid search over the rotations minimised the angle to "front forward-outward, top up" (error 0.29 rad); the translation (0, 1, 3) lifts the book 3/16 up the arm onto the palm and 1/16 forward. |
| `firstperson_righthand` (= left) | -15, -15, 15 | -2.9, 4.3, -1.3 | 0.42 | front (-0.26, 0.25, 0.93) = toward the eye and a little to the centre, top (-0.25, 0.92, -0.31), spine on the left; book centre at (0.379, -0.255, -0.797) in the camera frame (the left hand mirrors it). The translation moves the vanilla anchor (0.56, -0.52, -0.72) up and in so that the book is fully on screen at 70 degrees field of view; scale 0.42 keeps it about 0.34 x 0.23 x 0.1 block. |
| `ground` | -90, 0, 0 | 0, 0, 0 | 0.55 | `Rx(-90)` takes the front (+Z) to +Y: the book lies on its back, cover up, spine -x, top toward -z; it spins about Y with the entity. About 0.35 wide x 0.54 long and 0.17 thick (in blocks): a lying tome, not a floating card. The entity's own hover keeps its underside above the grass. |
| `fixed` | 0, 180, 0 | 0, 0, 0 | 1.2 | `Ry(180)` turns the front to the viewer (as for every sprite); 1.2 so the 13-unit book fills the frame like a map would |
| `head` | 0, 180, 0 | 0, 13, 0 | 1 | upright on the head facing forward; 13 is vanilla's value for "rests on top of the head" (13/16 * 0.625 = half a block above the head centre) |
| `gui` | 20, -155, 0 | 0, 0, 0 | 0.62 | present for completeness (any context that uses the base model: an addon that renders the item in a `gui` context outside the inventory); the inventory shows the sprite |

## 4. Why `neoforge:separate_transforms`, and the per-franchise cover

The owner wants the 3D book in hand, on the ground and in frames, and the OLD sprite as the inventory icon. `neoforge:separate_transforms` is exactly that loader: a `base` model for every perspective and a `perspectives` map that replaces single contexts with another model, the model's
OWN `display` applying per perspective. The registered `skill_book.json` is `{loader, gui_light: front, textures.particle, base: skill_book_3d, perspectives: {gui: skill_book_2d}, overrides: [...]}`: `gui_light: front` because the flat sprite goes dark under the side lighting a block uses,
`textures.particle` because the wrapper needs its own particle sprite. *Rejected:* one item per franchise (the book is ONE item whose component says which book, `items.md` section 3); `custom_model_data` (a stack made by `/give` with only the book id would show the wrong cover: the property below follows the component the stack
really has); `neoforge:composite` (cannot switch a single context to a flat sprite).

**The franchise cover.** A client item property `nusmp:franchise` (registered in `BookClient`) answers `(index + 1) / 20` for the franchise at `index` of the table's `franchises` list (0 = unknown, the generic cover). The registered model has one override per franchise, thresholds half a step below the value
(`(i + 1) / 20 - 0.025`; the game compares with `>=` and the last match wins, so no float rounding can miss a cover), pointing to `skill_book_st_<franchise>.json` (a separate_transforms of that franchise's body and the same 2D icon). *Rejected:* a texture per line (283 files for no gain over a tint);
overrides on the texture variable (overrides pick MODELS, not textures); one model per franchise written by hand (the generator writes them, so a franchise is one line in `lines_lib.FRANCHISES`). The property reads the table first (`BookLook`) and the known route's `magic` (= franchise key) second, so a datapack line of an existing franchise
takes the franchise's cover. At most 19 franchises fit (`(i + 1) / 20 <= 1`: the game clamps a property to 0..1); the generator asserts it.

**The tint.** Cover faces (and the ribbon) carry `tintindex: 1`, the same index the old sprite's cover layer has, so the ONE existing item colour handler (`ItemsClient.registerColors`, tint 1 -> line colour) tints both. The cover textures hold only greys, none darker than 104, with an exactly white embossed highlight, so the tint is clean:
the brightest tinted pixel is exactly the line's colour (`BookArtAssetsTest` pins this, the same rule `ItemsAssetsTest` pins for the sprite). The metal, pages and stone are untinted: the clasp stays gold on a red book and a blue book. The colour comes from the registry's route if the client has it, else from the shipped table (section 5), else a neutral grey.

**The covers** (`gen_book_textures.py`): leather grain, a vignette, an embossed double frame with corner ornaments by franchise (plain, round, leaf, star), a title plate with three diamonds, and the franchise emblem pressed in as a relief (light edge top-left, shade bottom-right): the Exiled Heavy Knight a heater shield with a sword, Tensura a slime, Black Clover a four-leaf clover,
Magi an eight-pointed magoi star in a ring, Solo Leveling a gate with a dagger, DanMachi a flame in a ring, Fairy Tail a ghost-tail swoosh with a star, TBATE a crowned hourglass, Tower of God a tower, The God of High School a yin-yang, Omniscient Reader's Viewpoint an eye over an open book, and a star in a ring for `generic`.

## 5. The client table `assets/nusmp/skill_books.json` (`tools/lines/gen_book_index.py`)

The server syncs a client only the lines the player owns, so a book found, dropped or bought for a line the player has not unlocked has NO route in the client's registry; the old code showed a grey cover and "The pages are blank". The table (about 100 bytes a line: book id, line name, book name, colour,
franchise; about 30 KB for 283) is generated from the SAME two sources as the routes (`route_sources.py`: the route json files under `data/nusmp/skill_trees/`, plus `gen_lines.load_rows()`, the rows winning only where no file exists, tolerating an empty or half-filled `rows/`), so it cannot drift; `LineAssetsTest` fails when a shipped route is missing from it
or disagrees (book id, colour, name, franchise). The table is keyed by line id but searched by BOOK id too (`BookLook.byBook`), because a book id is not always `<line>_book` (`nusmp:heavy_armor_book` opens `oath_of_the_heavy_armor`). It is read on every resource reload (F3+T) by `BookClient` into immutable maps behind one volatile reference; on a dedicated server nothing loads it and it
stays empty, which is right (the server has the routes). `SkillBookItem.getName` and the tooltip use it when the route is unknown, and show "From: <franchise>" (new keys in `tools/lines/lang_book.json`). The component `nusmp:skill_book_id` stores the BOOK id (what the route's `book` field says), which the registry or the table maps to the line; that was checked and is unchanged.

## 6. The tree backgrounds (`tools/lines/gen_backgrounds.py`)

One 256x256 palette PNG (32 colours, about 6 to 35 KB, 17 KB on average: about 5 MB for 283) per line at `textures/gui/skill_trees/<id>.png`, for every route found in the json files and the rows. Theme: the line's `background_theme`, for the hand-built routes the table of the brief (fire dragon slayer and the Fang -> magma, lightning -> storm, sky -> stellar, ice make -> frost,
heavy armor -> iron, swordsmanship -> ruin, lady luck -> city), for a row with none the franchise's usual place. The 15 themes are different MATERIALS, not 15 colours: magma (Worley cell plates with glowing cracks), void (fbm nebula, a ring, stars), stellar (star field, dust band, glints, a constellation), forest (overlapping leaves, light shafts), ocean (depth gradient, caustics, bubbles),
storm (cloud fbm, rain, a forked bolt), ruin (stone blocks, cracks, moss), blood (branching veins, drops, pulse rings), holy (rays from above, halo, glints), frost (facets with bright edges, snowflakes, speckle), tower (floors that grow with depth, lit windows, pillars, rings), city (skyline, windows, a street grid, neon), dream (pastel gradient, bokeh, sparkles),
sand (warped dune ridges with a lit side), iron (riveted brushed plates, scratches, rust).

**Colour and seed.** `mid = mix(theme mid, 0.55 * line colour, 0.42)`, `accent = mix(theme accent, line colour, 0.55)`, `base = mix(theme base, 0.14 * line colour, 0.5)`: the line's colour shifts the picture without turning a forest into a fire (a very dark line colour is lifted first or it would paint nothing). Every random choice comes from `numpy.default_rng(sha256(id + theme))`,
so two lines of the same theme are two different pictures and the output never depends on the clock or the machine. **Dark and low contrast** is enforced for every theme by `normalize()`: the 99th percentile of the brightness is pulled to 0.27, the mean may not exceed 0.15, plus a soft vignette; the node icons and lines are drawn over this and the picture must lose to them.
`--selftest` checks the promises (deterministic, seeded, the 15 themes pairwise different, dark, tinted by the colour, under 45 KB). `--check` compares decoded pixels (mean error <= 1.5/255) instead of bytes, so another zlib does not make CI flap. The generator never deletes a file. `LineAssetsTest` guards: a declared `gui_background` exists, is a real PNG of 256 or 512 x 256 and at most 60 KB;
every route has a picture whether or not it declares one; no two lines share a byte-identical file; declared themes and `book_texture`s exist.

## 7. Loot: three doors, one pick

| Door | Where | Chance | Class |
|---|---|---|---|
| (a) Abyssal-road monsters of the Mega-Dungeon | global loot modifier `nusmp:skill_books` (existing) | 0.4% a route book per kill, 4% an SP Book (config `skill_books.abyssalBookChance` / `abyssalSpChance`) | `SkillBookLootModifier` |
| (b) the pocket realms' vault chests | NEW global loot modifier `nusmp:rift_books` | per table in `data/nusmp/loot_modifiers/rift_books.json`: relic-vault chest 20%, trial chests 8% each (the vault has 1 + 2), x config `lines.riftBookScale` (1.0) | `RiftBookLootModifier` |
| (c) the Decennial bosses (the Guardian of every 10th floor) | the Guardian's death listener (existing: the Guardian rolls no table, so a loot modifier never sees it) | 30% a route book, always 1 to 3 SP Books (config `bossBookChance`) | `BossBookDrops` |

**Why a rift chest needs two tests.** The pocket vault uses the dungeon's own tables (`chests/dungeon_relic_vault`, `chests/labyrinth_trial`), which fill ordinary chests of the main dungeon too. So a chest pays only when its table is LISTED in the JSON and its origin stands inside a pocket's cell (`RiftRules.slotAt`, the dungeon's own test, a field of 64 cells at x = 4,000,000). The modifier carries the standard
`conditions` (the dimension check, like the gear chests') and checks `MegaDungeon.in` in code as well. A book is ADDED beside what the table gave (add the new, keep the old). Numbers: a pocket vault is worth 0.20 + 2 * 0.08 = **0.36 books** (P(at least one) = 1 - 0.8 * 0.92^2 = 32%): rarer than a decennial boss (0.30 per boss, one every ten floors) because a pocket is a short puzzle or
fight you can open several times an hour; `riftBookScale` and the JSON retune it without code. Anything not listed in `tables` (a datapack can add a table or another number) never pays.

**Which line (all three doors): `LootDrops.routeBook(dice, player)`.** *Uniformly weighted by `loot_weight`, without building a list per drop:* the registry already publishes its lines as one immutable `RouteIndex` snapshot that is replaced as a whole, so the snapshot's identity is the registry version. `RouteTable` keeps the routes and their CUMULATIVE weights for the
snapshot in force in a `VersionedCache` (rebuilt only when the snapshot object changes: a datapack reload, a client sync, or a clear; no invalidation call exists to forget), and `WeightedTable.pick` is one dice value, one multiplication and a binary search, O(log n) and allocation free. With the 8 shipped routes (weights 3, 3, 2, 2, 2, 2, 2, 1) and 275 generated lines at the default weight 1 the total is 292:
a weight-3 line is 1.03% of the book drops, a weight-1 line 0.34%. The table answers exactly what `RoutePick` (kept) answers for the same dice; a unit test cross-checks them on 36,000 random weight sets and dice values.

**Pity.** The old code had none, so the rule is simple and in `BookPity`: remember per player whether the LAST route book handed to them was of a line they already owned; if it was, the next route book is redrawn (the same weighted draw, up to `lines.pityRerolls` = 12 times, then a scan from a random start for any line they do not own) until it is a line they do not own. So an owned line can drop, but never twice in a row, and a player who owns
every line gets the plain draw (a duplicate is still worth Skill Points). The memory is one boolean per ONLINE player (dropped on logout and on server stop; a restart forgets it, which only costs one more chance of a duplicate). The player is the killer (door a, c) or the chest's opener (door b); a drop with no player has no memory. `lines.pity` switches it off.

**Config** `nusmp-skilltree-loot-server.toml`, new section `[lines]`: `riftBooks` (true), `riftBookScale` (1.0, 0 to 10), `pity` (true), `pityRerolls` (12, 1 to 200). The `[skill_books]` section is unchanged.

**Self-test** `tree.lines.loot` (registered in `LinesLoot.init`, run by the CI server smoke test): the type is registered; the weight table matches the registry and a thousand draws neither rebuild it nor pick a weight-0 line; the rift entry point adds one book for a listed table in a pocket cell and nothing for an unlisted table, a position outside the pockets, the Overworld, a scale of 0; a made-up chest table carrying the relic-vault id is rolled through the whole loot pipeline
(so the codec, the JSON and the global list are exercised): books inside a pocket cell, none outside; and a stand-in player that owns one line gets 400 draws with never two owned in a row.

## 8. Files

| File | Role |
|---|---|
| `tools/lines/book_model.py` | geometry, uv layout, display block (shared) |
| `tools/lines/gen_book_textures.py` | draws `base.png` and the covers, writes all item models (`--check`) |
| `tools/lines/preview_book.py` | renders the model json from every display angle with the game's maths (`python3 tools/lines/preview_book.py --franchise tensura --color 4FC3F7`) |
| `tools/lines/gen_book_index.py`, `route_sources.py` | the client table; where the lines come from |
| `tools/lines/gen_backgrounds.py` | the 15 themes (`--check`, `--selftest`, `--demo DIR`) |
| `tools/lines/lang_book.json` | the new strings (flat key -> text), NOT merged into `en_us.json` |
| `skilltree/item/BookLook.java` | the pure table: parsing, franchise property, colour fall-back |
| `client/skilltree/BookClient.java` | resource reload of the table, the property, `coverColor` |
| `skilltree/loot/WeightedTable`, `VersionedCache`, `RouteTable`, `BookPity` | the cached weighted pick and the pity rule (pure except `RouteTable`) |
| `skilltree/loot/RiftBookRules`, `RiftBookLootModifier`, `LinesLoot`, `LinesLootSelfTests` | the rift modifier, its pure rules, the entry point, the self-test |
| `data/nusmp/loot_modifiers/rift_books.json`, `data/neoforge/loot_modifiers/global_loot_modifiers.json` (+1 entry) | the modifier's data and switch |
| Tests: `BookLookTest`, `BookArtAssetsTest`, `LineAssetsTest`, `WeightedTableTest`, `VersionedCacheTest`, `BookPityTest`, `RiftBookRulesTest`, `RouteTableTest` | section 10 |

**Existing files edited** (kept to the minimum): `skilltree/item/SkillBookItem` (name and tooltip fall back to the table when the route is unknown; a "From:" line), `client/skilltree/ItemsClient` (one `BookClient.init(modBus)` line; `coverColor` delegates to `BookClient.coverColor`), `skilltree/item/ItemsSystem` (one `LinesLoot.init(modBus)` line),
`skilltree/loot/LootDrops` (`routeBook` and `materialize` take the player, use the cached table), `skilltree/loot/SkillBookLootModifier` and `BossBookDrops` (pass the killer to `materialize`), `skilltree/loot/LootConfig` (the `[lines]` section), `models/item/skill_book.json` (the 3D wrapper, section 1), `ItemsAssetsTest` (one path),
`data/neoforge/loot_modifiers/global_loot_modifiers.json` (+1 entry).

## 9. Commands

```
python3 tools/lines/gen_book_textures.py        # textures + models   (add --check in CI)
python3 tools/lines/gen_book_index.py           # assets/nusmp/skill_books.json   (run after gen_lines.py: the generated routes are read from the json files)
python3 tools/lines/gen_backgrounds.py          # one PNG per line     (add --check in CI; --selftest; --demo DIR for the 15 themes)
python3 tools/lines/preview_book.py             # PNG contact sheet of the book from every display angle (build/book_preview)
```

After the content rows land: `gen_lines.py`, then `gen_book_index.py` and `gen_backgrounds.py`, commit the output. The hand-built routes' json files do not declare `gui_background` / `book_texture` yet (they predate those fields); their pictures exist, and the engine can fall back to `nusmp:textures/gui/skill_trees/<route id>.png`.

## 10. What was checked and what was not

* **Previewed**: the model json was rendered with the game's own transform maths (`preview_book.py`: first person right and left, third person front, side and back, ground, frame, head, GUI) and looked at for four franchises and colours; the covers of all 12 franchises tinted in 12 colours; the 15 backgrounds and the 8 hand-built ones.
  The previewer is MY implementation of the game's pipeline (item pose, arm frame, lighting, culling); a mistake in my reading of `ItemInHandLayer` or `ItemInHandRenderer` would show as a wrong pose in the game that my picture does not show.
* **Unit tested** (JUnit, run with the shim runner against the real classes, 50 new tests in 8 new classes, plus the old ones that touch these files): `BookLookTest` 8, `BookArtAssetsTest` 8, `LineAssetsTest` 6 (also run against a temporary data set of 12 routes with the new fields, and shown to FAIL when a declared background is deleted), `WeightedTableTest` 8 (including a cross-check against `RoutePick`),
  `VersionedCacheTest` 4, `BookPityTest` 6, `RiftBookRulesTest` 7, `RouteTableTest` 3; `ItemsAssetsTest`, `RoutePickTest`, `LootRulesTest` still pass. Mutation check: breaking `pickAvoiding` is caught by 4 tests, breaking the cache's version test by 4.
* **Type-checked only** (against the API stubs, 0 errors in these files except the known stub artefact `ItemEntity is abstract`; a deliberate wrong call in `LootDrops` was reported, so the check sees errors there): `BookLook`, `BookClient`, `ItemsClient`, `SkillBookItem`, `WeightedTable`, `VersionedCache`, `BookPity`, `RouteTable`, `RiftBookRules`, `RiftBookLootModifier`, `LinesLoot`, `LinesLootSelfTests`, `LootDrops`, `LootConfig`, `ItemsSystem`.
* **NOT verified** (needs the game or CI): that `neoforge:separate_transforms` accepts a nested `parent` that is a `minecraft:item/generated` model for `gui`, and applies each perspective model's OWN `display` (both from my reading of NeoForge's documentation and source, never loaded here); that `gui_light` and `textures.particle` at the top level are read as I expect; that the item colour handler tints the 3D quads with tint index 1 (it does for any baked quad with a tint index, by the rules of `ItemColors`);
  the visual quality of the book in the actual lighting (the previewer's two-light shade is the game's formula, not the game); that `RegisterClientReloadListenersEvent` loads the table before the first tooltip; the codec of `rift_books.json` (`Codec.unboundedMap(ResourceLocation.CODEC, ...)`); that the global modifier runs for the pocket's chests, which CI's `tree.lines.loot` is written to prove; the pity memory with real players (only a stand-in).
* **For the lead**: `RouteWire.MAX_ROUTES = 64` and `MAX_TOTAL_BYTES = 900_000` in `skilltree/data/RouteWire.java` make `RouteIndex.build` REFUSE every route after the 64th on the server: with 283 lines most would not load, and these loot tables, the book table and the backgrounds would then describe lines that do not exist. That is the engine builder's file; it must be raised (and the client sync, which sends only owned lines, must not be bound by it).
