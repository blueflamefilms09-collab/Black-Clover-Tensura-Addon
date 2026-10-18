# The Canon Grimoire: the remade 3D books and the new name

The items `charter_grimoire_<id>` (seven bosses and 23 arc characters, 30 books) are called **Canon Grimoire** for the player now, and every one is a new hardcover 3D model held correctly in both views.
Nothing here ran in the game: section 8 says what was previewed (by `tools/canon/preview_canon.py`, a software renderer of the game's pose maths), tested and not verified.

## 1. New and replacement (owner rule 5)

| Part | Status | What |
|---|---|---|
| Registered model `models/item/charter_grimoire_<id>.json` (30) | REPLACEMENT | now a `neoforge:separate_transforms` wrapper: the 3D body everywhere but the GUI, the old flat sprite (`_icon`) in the GUI, as `skill_book.json` does |
| Old model of each book | KEPT | as `charter_grimoire_<id>_v1.json`, pointing at `textures/item/charter_grimoire_<id>_v1.png` (the old 32x32 texture, copied) |
| Body `models/item/charter_grimoire_<id>_3d.json` (30) | NEW | 44 cubes and the full display block |
| Texture `textures/item/charter_grimoire_<id>.png` (30) | REPLACEMENT | a 192 x 240 palette atlas replaces the old 32 x 32 one (kept as `_v1.png`) |
| `_icon.json` / `_icon.png` | untouched | the old sprite, still the inventory icon |
| Name "Charter Grimoire" -> "Canon Grimoire" | REPLACEMENT | section 6; ids, registry names, classes, files, config keys, NBT keys and the command word `charter` are NOT renamed |
| `/multiverse canon ...` | NEW | an alias of `/multiverse charter ...` |
| Tools `tools/canon/*`, test `CanonGrimoireAssetsTest`, this document | NEW | |

## 2. The model (`tools/canon/canon_book.py` is the source, `gen_canon_models.py` writes the json)

The book stands upright in model space: front cover SOUTH (+Z, like the front of an item sprite), spine WEST (-X), fore-edge (pages) EAST. Boards 9.6 wide (x 3.4..13.0), 13 high (y 1.5..14.5), 4 thick (z 6..10), 10.6 wide with the spine,
its middle at (8.2, 8, 8): a real book proportion (the old slab was 11 x 15), and the middle of the model's 16 unit box, which is what scaling and rotating happen about.

| Elements | What and why |
|---|---|
| `front_board` + `front_bevel`, `back_board` + `back_bevel` | each cover is a board (0.4) plus a bevel (0.3) 0.25 inset on every side: the stepped edge reads as a rounded edge from every angle |
| `spine_1..3`, `band_i_k` (4 x 3) | a three-step spine (wide to narrow) is a round back seen from above; four raised bands are the spine's own profile grown by 0.05 (a ridge that follows the curve, not a peg) |
| `pages_1..5` | the page block, five slices that bulge towards the middle of the fore-edge (x 12.5, 12.7, 12.8, 12.7, 12.5), inset 0.2..0.5 from the boards; tops are disjoint (no z-fighting) and the steps between slices get a face only where they show; page lines in the texture |
| `ribbon`, `ribbon_tail_a/b` | the marker ribbon with a notched tail |
| `cap_*` (4) | metal corner caps, 0.1 proud of the boards |
| `frame_*` (4) | a thin raised metal frame round the front cover |
| `medal_wide`, `medal_tall`, `emblem_glow` | the medallion is a plus-shaped octagon (two overlapping boxes) with a gold bezel and a dark disc; the emblem is on a separate quad in front of it |
| `strap_front/edge/back`, `strap_tip`, `buckle_plate`, `buckle_stone` | a leather strap round the fore-edge with a buckle and a stone |

**Glow.** The mod's glow pattern for items (`shadow_core_3d.json`, `gen_paint_items.py`) is `"neoforge_data": {"block_light": 15, "sky_light": 15}` on an element: `emblem_glow` and `buckle_stone` use it, so the emblem of every book is
full-bright in a dark room and in the hand.

**Texture** (`canon_book.REG`, shared by the generator and the previewer): 192 x 240, 8 px per model unit on the covers. Front 80 x 104, back 80 x 104 (a round seal with the sigil large), spine 30 x 104 (tooled lines, a label with the sigil between band 2 and 3),
the board edge strips, the medallion (44 x 44), the emblem (40 x 40, transparent where it is not), gold and bronze plates, ribbon, stone, stitched strap leather, plain leather, and the page lines (fore-edge and head/tail). It is saved as a palette PNG (about 120 colours,
colour 0 transparent), every ramp a handful of steps.

**Identity of each book** is read from its OLD texture (`_v1.png`), not invented: the cover colour (the old `shade()` formula inverted on six plain pixels of the old back cover, within 3 per channel of the Charter table), the metal (gold; the dark trim on Lucius), the trim colour
and the 7x7 sigil (hand-drawn for the seven bosses, the id-derived glyph for the others). The sigil is kept on the nameplate, the spine label and the back seal. The **emblem** on the medallion is new, chosen from 30 motifs (`tools/canon/motifs.py`, `MOTIF_OF`, with the reason for each):
gravity well (Dante), blood drop (Vanica), rift (Zenon), hourglass (Lucius), flame (Devil Avatar), fist (Goliath), whirlpool (Charybdis), twin-headed serpent (Amphisbaena), fox (Ban), anvil (Black Goliath), hammer (Diane), sun (Escanor), eye (Fana), goat (Gowther), snowflake (Heath Grice),
wings (King), cloud (Lotus Whomalt), crown (Lucifero), crystal (Mars), sword (Meliodas), orbit (Merlin), bull (Minotaur), magic circle (Morris), star (Patolli), wisp (Rades Spirito), chain (Revchi Salik), twin circles (Rhya), skull (Udaeus), claws (Vetto), rune (Zagred).
Four Seven Deadly Sins books (Diane, Gowther, King, Merlin) have the Builder's default grey in the roster; they keep it, and get an emblem glow colour of their own (`GLOW_OVERRIDE`) so they are not four copies.

## 3. Display transforms: the maths and the numbers

A `display` entry is `rotation [rx, ry, rz]` (degrees), `translation [tx, ty, tz]` (1/16 block, clamped to +-80), `scale [s, s, s]` (clamped to 4). The game puts a model vertex `v` (0..16 units) at

```
p = Pose * T(t/16) * Rx(rx) Ry(ry) Rz(rz) * S(s) * T(-0.5) * (v/16)
```

so the **translation is in the frame of the surrounding pose, not in the rotated frame of the book**, the book turns about the middle of the 16 unit box, and for a LEFT-hand context the game first negates `tx`, `ry`, `rz`: the same numbers in both hands give the mirrored pose (the cover turned
outward on both sides, the book not reflected). That is why the left entries carry the same numbers as the right ones. `Pose` is what the game wraps around the item (`docs/skilltree/lines_book_art.md` section 3 has the table; `tools/lines/preview_book.py` implements it).

Instead of tuning by hand, `tools/canon/solve_display.py` solves for what we WANT: pick the world direction the cover should face and the direction its top should point, search the Euler angles (5 degree grid, refined, small angles preferred) for the best fit, then solve the translation that puts a
chosen point of the book at a chosen place (`t/16 = P - R S (q/16 - 0.5)`). Then the result was looked at (`preview_canon.py`) and kept.

| Context | Wanted | rotation | translation | scale |
|---|---|---|---|---|
| `firstperson_righthand` (= left) | cover facing the player, leaning back 17 degrees (front `(-0.22, 0.32, 0.92)`, camera frame x right, y up, z toward the eye), top `(-0.10, 0.93, -0.35)`, spine on the left; book centre at `(0.40, -0.22, -0.75)` = lower right | -19, -13, 6 | -2.63, 4.79, -0.49 | 0.38 |
| `thirdperson_righthand` (= left) | in the fist: the lower spine at the palm, cover out and a little forward (front `(0.87, -0.21, -0.45)`, player frame x = right, -z = forward), top up leaning forward 14 degrees, the book extending forward from the hand | 47, 120, 12 | -1.21, 0.96, 1.1 | 0.55 |
| `ground` | lying on its back, cover up, spinning with the item entity | -90, 0, 0 | 0, 0, 0 | 0.55 |
| `fixed` | cover to the viewer (the `ry = 180` of every sprite), filling the frame | 0, 180, 0 | 0, 0, 0 | 1.2 |
| `head` | upright on the head, cover forward | 0, 180, 0 | 0, 12, 0 | 0.8 |
| `gui` | present for completeness; the inventory shows the old sprite | 20, -155, 0 | 0, 0, 0 | 0.62 |

**First person.** Vanilla anchors a held item at `translate(0.56, -0.52, -0.72)`: at 70 degrees field of view that is below the bottom edge of the screen, so a model written for a sprite sits half off screen. The old Charter display (`rotation 0, -90, 25`, scale 0.68 on an 11 x 15 slab)
turned the slab edge-on and made it 0.68 x 15 / 16 = 0.64 block tall at 0.7 block away: 65 percent of the screen height. Now the book is 13 x 0.38 / 16 = 0.31 block tall, about a third of the screen height (the area is a quarter of the old one), centred 0.75 block away at the lower right, the cover facing the player and tilted
back like a tome held open towards the reader; the translation is solved so the centre lands there. The game draws no arm for an ordinary held item, so nothing else is on screen.

**Third person.** `ItemInHandLayer` places the display origin at `Rx(-90) Ry(180) translate(1/16, 0.125, -0.625)` after the arm's own turn: working it out, the origin is the middle of the FRONT-bottom edge of the arm box, and the fist (the centre of the 4 x 4 end of the arm) is 2 px behind it and 1 px up
the arm, i.e. display `(0, -2, +1)/16`. The old display put the middle of a flat slab at the origin, with the model turned sideways: the arm touched one corner and the book floated beside it like a tray. Now the point of the book that is gripped, the lower part of the SPINE (model `(2.9, 5.2, 8)`), is solved onto that point,
and the book extends from it, upright, cover out. The numbers keep the book clear of the torso (the fore-edge points forward, only about 0.5 of its width inward) and its box 7 px tall by 5.8 wide is the size of a small tome in a 4 px fist.

**Left hand.** Same numbers, mirrored by the game; verified in the previews (`tp_left_front`, `tpz_left_45`, `fp_left`).

## 4. Does anything else use the model? (hip, floating grimoire, worn)

Searched: the Summon Grimoire floating at the hand and the hip rendering (`client/grimoire/GrimoireFloatClient`, `GrimoireGeometry`, `models/item/grimoire.json`) draw the Black Clover grimoire's own model; nothing there reads a `charter_grimoire` model. The charter books are plain `Item`s,
so the custom item renderers (`HandItemRenderer`, `ArmorItemRenderer`: armour and weapon items only) do not touch them either. Worn in a Curios charm slot they use the worn library entry `item charter_grimoire_<id> charm_tome` (`relic_worn/catalog.txt`), unrelated to the item json.
So no code change was needed for the new models, and the `_icon.json` files keep working as before: they are now also what the wrapper shows in the GUI.

## 5. Why `neoforge:separate_transforms` and a wrapper, not a loader-less model

The owner's rule 7 wants the 3D model in hand and the old sprite as the inventory icon. A loader-less model has one `display` block, and the GUI context would show the 3D book (the old files did, with a purple slab in every slot). `neoforge:separate_transforms` replaces single contexts: the body keeps its own
`display` for the hands, ground, frames and head, and `perspectives.gui` is the `_icon` model. The registered file is therefore the wrapper, and the elements live in `charter_grimoire_<id>_3d.json` (the pattern of `skill_book.json` / `skill_book_3d.json`). `gui_light: front` because the flat sprite goes dark under the side lighting
a block uses. `textures.particle` is the old icon (the wrapper needs a particle sprite of its own).

## 6. The rename

Player-visible "Charter Grimoire" is "Canon Grimoire" everywhere the player can read it; the words that describe the books follow ("The Canon of the Gravity Devil", "A Written Canon", "The Complete Canon", "canon grimoire", "Boss canons"). Not renamed: item ids, registry names, `has_dropped_charter_grimoire_*` keys, classes
and packages, asset file names, config keys and the config file, NBT keys, advancement ids, and the command word `charter` (the alias `canon` was added in `CharterCommand`). The skill book "Charter of the First Master" (Fairy Tail route) is a different thing and keeps its name.

Edited in place: `CharterGrimoireItem.getName` fallback ("Canon Grimoire of <name>", used for all 23 arc books, which have no lang entry), `CharterCommand` (messages, alias), `CharterEvents` (the first-claim broadcast), `CharterMirror` (the lender name in "<lender> lends you X"), `CharterConfig` (comments), `CharterTable` and `ArcCharterRules` (lore lines), `ArcCommand`, `ArcSelfTests`,
`ArcWorldSelfTests`, `ExistingBosses`, `ArcStory`, `ModWorldDropData` (admin and log text), `data/nusmp/arc/dialogue/marx_francois.json`, `quest_pack/lang/en_us.snbt`, the quest and advancement generators `tools/quests/spec_*.py`, `lang/qst.lang.json`, `gen_docs.py`.
`en_us.json` is NOT edited: its 12 entries are in `tools/canon/lang_canon.json` (flat key -> full new text; regenerate with `tools/canon/gen_lang_canon.py`) for the lead to merge: 7 item names, 4 advancement lines, `arc.dialogue.marx_francois.11`.
`python3 tools/canon/check_names.py` greps for what is left (Java string literals, data, quests, dialogue, en_us outside the fragment) and checks the fragment; it exits 1 on a leftover.

## 7. Files and commands

```
python3 tools/canon/gen_canon_models.py            # writes the 30 books (idempotent; keeps the old art as _v1 once)
python3 tools/canon/gen_canon_models.py --list     # what was read from the old art
python3 tools/canon/preview_canon.py --id ban      # contact sheet of one book from every angle (build/canon_preview)
python3 tools/canon/preview_canon.py --all --view m_front
python3 tools/canon/solve_display.py               # reproduces the display numbers of section 3
python3 tools/canon/motifs.py sheet.png            # the 30 emblems
python3 tools/canon/gen_lang_canon.py && python3 tools/canon/check_names.py
```

`tools/canon/canon_book.py` (geometry, atlas layout, display), `canon_tex.py` (painter), `motifs.py` (emblems), `gen_canon_models.py`, `preview_canon.py` (extends `tools/lines/preview_book.py`: glow elements full-bright, a 16:9 first person, close-up third person `tpz_*`),
`solve_display.py`, `gen_lang_canon.py`, `check_names.py`, `lang_canon.json`; `src/test/java/.../charter/CanonGrimoireAssetsTest.java`; previews `docs/canon/preview_meliodas.png` (one book from every angle) and `preview_all_books.png` (all 30 fronts).
`tools/gen_charter_assets.py` and `tools/gen_arc_grimoire_assets.py` (the old generators) are untouched: running them would overwrite the new models, so run `gen_canon_models.py` after them (it keeps `_v1` from the first run).

## 8. What was checked and what was not

Checked: the model json of all 30 books is produced from the generator and read back by the test (structure, ranges, texture, palette, display bounds, left = right, `_v1` kept); the rename leaves no player-visible "Charter" (`check_names.py`); the Java edits type-check against the API stubs (only the stub
artefacts "abstract; cannot be instantiated"); `CanonGrimoireAssetsTest`, `CharterTableTest`, `ArcCharterRulesTest`, `AdvancementDataTest`, `ArcWorldAssetsTest`, `RelicRenderCoverageTest` pass in the JUnit shim.
**Not verified in the game:** the poses are the game's pose maths run by my own software renderer with a stand-in blocky player (no hand model, nearest-texel, no real lighting): the first-person size and placement and the third-person grip are previewed, not seen in Minecraft; the glow (`neoforge_data` light) and
the `separate_transforms` perspective model are taken from how `shadow_core_3d.json` and `skill_book.json` are written, not from running them; the Gradle build was not run. Expect to adjust `_FP` / `_TP` in `canon_book.py` by a few units after a look in the game (then run `gen_canon_models.py`).
