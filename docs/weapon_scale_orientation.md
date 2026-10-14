# Weapon size and orientation (1.29.0)

The owner reported that many weapons, relics and items were smaller than they should be, that most axes were far too small with heads like little plates,
and that some weapons faced the wrong way (Sorrow's Edge: the crescent bulged toward the player). This page is the one rule for how a baked weapon mesh
is sized and oriented, the tools that audit and fix every mesh, and the runtime safety net that catches any weapon added later.

Everything here is about the 3D meshes in `assets/nusmp/weapon_meshes/<id>.txt` (the base weapons of `WeaponRenderer.BASE_WEAPONS` and every `model3d`
relic, state meshes included). The inventory icon (the old 16 x 16 sprite) is untouched.

## 1. The orientation convention

A mesh lives in **mesh space**: `y` runs along the weapon from the butt (grip end, low y) to the tip or head (high y), `x` across it in the item sprite's
plane, `z` through it (thickness). `WeaponRenderer` lays mesh y on the item sprite's up-right diagonal (the `place` line's angle, about 45 degrees), so:

* the **tip or head points up-forward**, away from the hand, never down and never back at the wielder;
* **mesh -x is forward** (the sprite's up-left side, where the edge of a vanilla axe is): the cutting edge, the convex side of a crescent, the blade of an
  axe, the hook of a hook sword face -x; the flat, the back, the poll and the spine face +x;
* a **curved blade** bends like its class: a katana's tip toward its edge (-x), a sabre's, scimitar's and dadao's toward its spine (+x);
* a **symmetric weapon** (a straight sword, a staff, a double-bladed axe) has no handedness: only "tip up" applies.

Where -x ends up on screen (derived from `ItemInHandRenderer`, `ItemInHandLayer` and the model json's display transforms, drawn by
`tools/relics/weapon_sheet.py`):

| view | what -x (forward) is |
|---|---|
| first person (display rotation `[0, -90, 25]`) | away from the camera: the edge faces forward, the back faces you; a head on +x comes straight at the camera (Sorrow's Edge, screenshot 64) |
| third person right hand (`[0, -90, 55]`) | down: the weapon points forward, an axe head hangs below its haft with the edge down; a head on +x stands up above the arm (screenshot 63) |
| ground, item frame, head | the same mesh, only scaled (section 6) |

So a correct axe looks like a woodcutter's: haft forward, blade hanging, edge down in third person and edge away from you in first person.

## 2. The size rule: classes and apparent length

Sizes are **apparent blocks**: the mesh length along its axis, `(ymax - ymin) * place scale`, times the model json's `thirdperson_righthand` scale
(0.85 for most; some relics carry 1.3 to 1.6). 1 block = 1 metre, Steve is 1.8. The head size of an axe or hammer is the widest slice of the mesh across,
also times that scale.

The table (`tools/relics/weapon_classes.json`, the game's copy `assets/nusmp/weapon_classes.json`, the built-in fallback in `WeaponClasses.java`; one test
keeps the three equal):

| class | length (blocks) | head min | grip |
|---|---|---|---|
| dagger | 0.35 - 0.55 | - | 0.20 |
| shortsword | 0.80 - 1.00 | - | 0.20 |
| longsword, rapier, katana | 1.10 - 1.30 | - | 0.20 |
| sabre | 1.00 - 1.25 | - | 0.20 |
| dadao | 1.20 - 1.45 | - | 0.20 |
| greatsword | 1.50 - 1.90 | - | 0.22 |
| handaxe | 0.70 - 0.90 | 0.30 | 0.25 |
| battleaxe | 1.10 - 1.30 | 0.45 | 0.25 |
| greataxe | 1.60 - 2.00 | 0.70 | 0.28 |
| crescent (crescent-headed axe) | 1.20 - 1.50 | 0.50 | 0.25 |
| scythe | 1.50 - 1.90 | 0.50 | 0.30 |
| cleaver | 0.55 - 0.80 | 0.25 | 0.25 |
| spear, glaive, trident | 1.80 - 2.40 | - | 0.33 |
| staff | 1.60 - 2.00 | - | 0.40 |
| whip (extended) / coilwhip (coiled or short stock) | 1.50 - 2.00 / 0.90 - 1.40 | - | 0.15 |
| crossbow | 0.70 - 0.95 | - | 0.50 |
| hammer | 1.10 - 1.50 | 0.40 | 0.25 |
| mace / club | 0.80 - 1.10 / 0.90 - 1.40 | - | 0.25 |
| wand | 0.35 - 0.55 | - | 0.20 |
| shield | 0.70 - 1.10 | - | 0.50 |
| item (held trinket, book, charm) | no length range, largest dimension at least 0.40 | - | 0.50 |

`grip` is the share of the length (from the butt) that stays where it is when a weapon grows: the hand does not move, the weapon grows away from it.

Every mesh has a **reviewed class** in `tools/relics/weapon_classes.json` (`weapons`: id to class; a state mesh such as `needler_pull2` or
`marids_valor_liquid` uses its base id). The first guess came from the id, the name and the card text; every class was then checked against the picture
of the mesh. An id nobody has classified falls back to a keyword guess (`weapon_classes.guess_class`, `WeaponClasses.guess`: `axe`, `sword`, `dagger`,
`spear`, `staff`, `hammer`, `whip`, ...; first hit wins; `item` when nothing matches).

A weapon over `1.5 x max` is reported as **large** and never shrunk (the Demon-Destroyer, Licht's pair, the Severing Greatsword and the Rimeheart
Runeblade are deliberately huge); a crossbow over its range is left as it is.

## 3. The tools

| tool | what it does |
|---|---|
| `python tools/relics/audit_weapon_scale.py` | the table of every mesh: class, mesh length, display scale, apparent length against the range, head, status (`small`, `head_small`, `large`) and flags. `--problems`, `--json`, `--check` (CI: every mesh has a class, the game copy is in sync), `--write` (copy the table to the game) |
| `... --sheet out.png [--only problems] [ids]` | contact sheets, 8 weapons a page: the weapon in the sprite plane (-x is left) with a 1-block ruler and the class range as a green bar, and the **held view** next to Steve (1.8 m; the real third-person chain: arm pose, `ItemInHandLayer`, display transform) |
| `... --overview out.png` | every mesh as one small front view with its id, class, apparent length and flags (the quick scan for an upside-down mesh) |
| `... --before-after DIR` | before (what the generators wrote, with every recorded fix undone) and after sheets of every fixed weapon, plus before / after overviews of all meshes; the shipped set is in `docs/preview/weapon_scale_orientation/` |
| `python tools/relics/apply_weapon_fixes.py [--apply] [--check] [--suggest [--write]]` | applies `tools/relics/weapon_fixes.json` to the baked meshes (section 4) |
| `python tools/relics/display_presets.py [--apply] [--check] [--include-excluded]` | the first-person oversize rule (unchanged) plus, new, the ground and item-frame scales of long weapons (section 6) |
| `python tools/relics/test_weapon_scale.py`, `python tools/relics/test_display_presets.py` | the Python tests |

The detector (`tools/relics/weapon_orientation.py`, mirrored exactly by `client/relics/item/WeaponOrientation.java`, the two agree to four decimals on the
real meshes) measures the body layer: every quad is clipped into 32 slices along y; per slice the area, the area-weighted x and the x / z extents. From that:

* `imbalance`: area-weighted mean x offset from the haft over the upper 70 % (negative = mass toward -x). A `handed` class (axe, crescent, hammer, mace, cleaver,
  glaive, hook) with `imbalance > 0.012` blocks has its head toward the player: flag `head_toward_player`.
* `centroid`: a head-heavy class (axe, spear, staff, club, ...) with its area centroid below 0.30 of its length has its head at the butt: `tip_down`.
  A `place` angle that points down the sprite is `tip_down` for any weapon class.
* `widest_at`: a pointed class with its widest part above 0.72 of its length gets `tip_suspect` (informational: an ornament near the tip, or a blade held by its point).
* `curve`: sideways displacement of the tip; a katana bending toward +x or a sabre / dadao bending toward -x by more than 0.05 blocks gets `mirrored` (informational).

`head_toward_player` and `tip_down` are **actionable** (fixed offline, fixed at runtime). `mirrored` and `tip_suspect` are informational: the edge side of a
blade cannot be told from its geometry alone, so a curved blade is never flipped automatically (the Demon-Slasher and Yami's katanas, the Permafrost Sabre
are listed; the original sprite curve is what the owner approved). The audit of the shipped set finds no actionable flag left.

## 4. weapon_fixes.json

`tools/relics/weapon_fixes.json` maps an item id (a state mesh uses its base id's entry) to a correction:

| key | meaning |
|---|---|
| `apparent` | target length in blocks as seen in the third-person hand |
| `extent` | the same for a held item: its largest dimension |
| `scale` | a plain multiplier of the place scale (instead of `apparent` / `extent`) |
| `grip` | share of the length that stays put when the weapon grows (default: the class's `grip`) |
| `flip_x` | mirror across the haft axis relative to what the generator wrote (edge / head to the other side) |
| `rotate_z` | degrees about z through the middle of the length (180 turns a tip-down mesh tip-up) |
| `head` | `{"k": 1.8, "ky": .., "kz": .., "cut": .., "band": 0.06}`: everything above `cut` (default: where the head starts) is grown across by `k`, along by `ky` (default `1 + 0.6 (k-1)`), through by `kz` (default `1 + 0.4 (k-1)`), easing in over `band` of the length |
| `orient_manual` | writes `orient manual`: hand-tuned, the runtime normaliser never touches the mesh |
| `note` | why |

The tool edits the baked text **reversibly**: the rewritten file ends with `# nusmp_fix {...}` recording the request and the original `place` line, so a
changed request first undoes the old one (flip is an involution, the head operation is exactly invertible, the place line is restored) and a repeated
request changes nothing. `mesh_lib.Mesh.write_txt` and `gen_weapon_models.write` call `apply_weapon_fixes.apply_file` after writing, so **regenerating
a mesh with its generator gives it its fixes again**; no generator needs editing for a size or head correction. (A generator that wants to opt out of the runtime net sets
`mesh.orient_cfg = "manual"`, which `write_txt` writes as `orient manual`.)

What was fixed in 1.29.0 (38 ids, 43 mesh files with the state meshes; 7 flips, 14 heads, 31 lengths):

* **flipped** (head or crescent was toward the player): Sorrow's Edge, Astralaria, Axe of the Exile, Leviathan Axe, Random Axe of Charity, Hunger, Doom Hammer;
* **heads made big and bold** (and the weapon brought into its class): Returning Hand Axe, Thornblight, Axe of the Exile, Leviathan Axe, Random Axe of Charity,
  Axe of the Crimson Saint, Astralaria, Frostfang, Rune Splitter, The Lumberjack, Sorrow's Edge, Hunger, Mountainguard's Greataxe, Butcher Cleaver;
* **brought up to the class length** (18): Caesar's Gladius, Jade Xiphos, Sidra's swearing sword, Druidic Blade, Bottle Blade, Liquid Sword, Magic Tool Sword, Sword Crystal,
  Bloodbound Katana, Marid's Valor (both stages), Heart of the Flame, Shadow Burn, Stegosaurus Greatsword, Wavebreaker, the Needler (all five stages) and the three whips
  (the Viper Whip and the Junglethorn Whip as coiled whips, the towel as an extended one);
* **held items raised to 0.43 blocks** (5): Devil Contract, Fortune Die, Recovery Salve, Spirit Charm, Written Consent.

The exact numbers are in the json.

Not touched (owned by other agents in the same release): Elsdocia and its legacy mesh, the dual-wield pair meshes (Twin Hook Swords, Twin Swords of Deflection,
Whirling Blades and their `_left` meshes). No other pair exists among the meshed weapons (the other "pairs" in the card texts, the bangles and pouches, are not
meshes). `weapon_classes.json` lists them under `excluded`; the audit, the fix tool and `display_presets.py` skip them (the lead's final run uses
`display_presets.py --include-excluded`).

## 5. Adding a new weapon so it is right the first time

1. Build the mesh along **+y** (butt at the bottom), the **edge / head / blade of an asymmetric weapon on -x**, the back and poll on +x.
2. Choose the class in `tools/relics/weapon_classes.json` (`weapons`: your id to a class), run `python tools/relics/audit_weapon_scale.py --write`.
3. Give the generator a real length from the class table: `real_m` (or the `REAL` table of `gen_weapon_models.py`) is the apparent length in metres, so use a value inside
   the class range, towards its lower third for a plain weapon and its upper half for a boss weapon. For an axe make the head at least the class `head_min`
   across (hand axe 0.30, battle axe 0.45 to 0.6, greataxe 0.70 or more): big and bold, not a plate.
4. Write the mesh, then run the audit (`python tools/relics/audit_weapon_scale.py <id>`): it must report no status and no flag, and the sheet
   (`--sheet out.png <id>`) must show the head hanging below the haft in the held view.
5. If the audit says `small` / `head_small` / `head_toward_player`, add an entry to `weapon_fixes.json` (or run `apply_weapon_fixes.py --suggest` to get one) and
   `--apply`; or fix the generator and regenerate.
6. Run `python tools/relics/display_presets.py --apply` so the first-person scale and the ground / frame scales follow the length.
7. Tests: `python tools/relics/test_weapon_scale.py` (fails when a shipped mesh is small or faces the wrong way) and the Java tests under
   `src/test/java/.../client/relics/item/`.

If you forget, the runtime safety net (section 7) still catches the worst cases.

## 6. The other views of a corrected weapon

* **Inventory / GUI**: the old icon (`nusmp:item/<id>_icon`), unchanged.
* **First person**: `display_presets.py` limits the apparent length to 1.0 block so the tip never goes through the camera, with the grip kept where a sprite
  sword's hilt is; `client/relics/item/OversizeGuard` is the render-time guard for a json the tool has not processed. The enlarged weapons are processed again
  (the manifest `tools/relics/display_presets_manifest.json` is rewritten). A head turned to -x now points away from the camera, so it no longer fills the screen.
* **Ground and item frame** (new, `display_presets.py`): a weapon longer than 1.6 blocks gets a smaller `ground` scale (a lying weapon at most 0.8 blocks) and `fixed`
  scale (a framed weapon at most 1.5, the frame already halves it), floors 0.3 and 0.55; only the scale follows, the entry's own rotation and translation stay; a
  weapon under the limit gets the base values back (0.5 and 1).
* **Head** (worn) and third person: unchanged (third person is what the sizes are measured in).

## 7. The per-game safety net (runtime)

`WeaponRenderer.mesh(id)` runs `WeaponNormaliser` once on every freshly loaded mesh (re-run after a resource reload):

1. it reads the id's class from `assets/nusmp/weapon_classes.json` (else the keyword guess), the third-person display scale and the `"nusmp_display_preset": "manual"`
   marker from the item's model json (a state mesh reads its base id's);
2. a mesh with `orient manual` or a manual model json is **never touched**; `orient <rotZ degrees> <flipX 0|1>` applies an explicit turn / mirror at load and the net still runs;
3. **tip down** (the `place` angle points down, or a head-heavy class with its weight at the butt): turned over;
4. **head toward the player** (a handed class with mass on +x): mirrored across its haft axis, quad winding reversed so the glow layer is not culled;
5. **too small**: apparent length below 60 % of the class minimum (a held item: below 60 % of 0.40): scaled up to the minimum about the grip, at most 4 times;
6. one log line per corrected mesh: `[nusmp] weapon mesh <id> (<class>) normalised: <what>`.

It changes nothing on the shipped meshes (a test runs it over all of them). Fed the 1.28 meshes it would have mirrored Sorrow's Edge, Astralaria, the Exile
axe, the Leviathan Axe, the Charity axe, Hunger and the Doom Hammer and raised the whips; it would not have enlarged the heads (it never invents a bigger head: the
offline `head` fix does that), which is why both exist. Curved blades are never flipped at runtime.

## 8. Why (the reasons for each piece)

* **One convention (mesh -x forward).** The meshes come from many generators and cards written months apart; each author put the edge where the reference art had it.
  Without one rule the only check is looking at every weapon in game. -x is the side the vanilla sprites put the edge on, so every old sprite already agrees with it,
  and it is the side that points away from the camera in first person and down in third person, which is how an axe is carried.
* **Apparent length (mesh length x display scale), not mesh length.** The display scale differs per model json (0.55 to 1.57), so the same mesh is a different size in the hand.
  Judging the mesh alone would flag the weapons that were already compensated and miss the ones that were not.
* **A class table with ranges and a head minimum.** "Too small" has no meaning without a target; real-life sizes by class give one the owner can read and change, and the
  head minimum is the owner's complaint about axes made measurable. Ranges, not points: weapons of one class are allowed to differ; only the ones below the range are raised
  (to a third into it, so they are clearly in range and not at its edge), and the ones far above it are reported, not shrunk (a huge sword is a design choice).
* **Measures on clipped slices.** A blade is a few long quads; binning quads by their centre would put all of a blade's area in one slice. Clipping each quad into the slices it crosses gives
  the real profile, so the centroid, the imbalance and the width are right for both a plate blade and a lofted haft.
* **Offline fixes in a data file, applied reversibly.** A per-id json is reviewable (the owner can read "Sorrow's Edge: flip, head x2.7, 1.3 blocks"), survives regeneration (the
  generators call the same function after writing) and needs no edits in a dozen generators; the `# nusmp_fix` marker makes it undoable, so tuning a number is a re-run and not a hand-repair of a
  baked file. The head operation grows only what is above the head's start so the haft is not fattened.
* **A runtime net on top.** A weapon added later (by a new relic wave, a mod pack, a rebuilt mesh) should not need an audit run to be playable: the net corrects the clear faults at load,
  logs them so the author sees them, and respects `manual` so a hand-tuned weapon is never overridden. It does not flip curved blades and does not enlarge heads because it cannot know what the author meant.
* **Pure Java classes with a Python twin.** The rule is the same in the tool (which fixes the shipped files) and the game (which guards the rest); keeping both pure and equal to four decimals
  (and testing the class tables for equality) means a change to the rule is made in two places and caught by tests in both, without launching the game.
* **Ground and frame scales.** Enlarging weapons made the longest ones overhang an item frame and tower over a dropped item; the view scales are derived from the same length the first-person rule uses,
  so one number (the mesh length) drives every view.

## 9. Not verified in game

Nothing here was run in the game. The held-view sheets are a software reproduction of the vanilla hand chain (checked against screenshots 63 and 64), the Java classes are pure and were run
through the Python cross-check, the unit tests and a pass over all shipped meshes, but `WeaponRenderer` itself (the model json read, the log line, the `orient` directive) was only
type-checked against stubs. Open points: the crossbows keep their limbs in the sprite plane (limbs up and down in third person; vanilla's crossbow uses its own display rotation); the katana /
sabre curves are reported only; the first-person look of the enlarged heads should be checked once in game.

---

# 10. 1.34.0: the grip end and the bigger size rule

The owner's two complaints after 1.30.0: (screenshot 68) "some weapons are flipped so the player is holding the blade and not the hilt", and "the weapons are still too small:
unless the weapon should be small, for example a dagger, make it bigger; a great sword should be massive; Elsdocia must be bigger". Sections 1 to 9 stay as written; where
they disagree with this section (the class table of section 2, the runtime thresholds of section 7, the first-person cap of section 6) this section wins.

## 10.1 The weapon in screenshot 68 is the Windcutter

A silver blade, a starburst of silver feathers (its winged guard) and a lilac ribbon round the blade: `windcutter`. Its generator (`wave11_ma.py`) measures the card with the
point at y = 0 ("y runs up from the point here; the pommel is at the top as in the card"), so the mesh had its hilt at the high-y end. The hand anchor is at the low-y end
(`WeaponRenderer` lays mesh y from the sprite's butt up the diagonal; the hand is at the grip, `GRIP_FRACTION` 0.25 of the length for the first-person guard, the class `grip`
for growing), so the player held the blade and the pommel pointed away. The 1.30.0 audit only checked the direction of a head or crescent and "tip up", never which end is the grip.

The audit now finds exactly four meshes built point first: **Windcutter, Tripolar Sword, Flamecutter, Encircled Blade** (the other 120 base meshes were looked at in the overview
sheets as well: every one has its hilt at the butt). `docs/preview/weapon_size_1_34/grip_before_01.png` / `grip_after_01.png` show the four before and after.

## 10.2 The grip-end detector

`weapon_orientation.py` / `WeaponOrientation.java` (same maths, tested to agree): a new actionable flag `grip_far_end`.

1. **Directive first.** A mesh may state where its grip is: `grip <fraction>` (the share of the length, from the low-y end, where the middle of the grip lies). Above 0.5 the mesh is
   reversed; at or below 0.5 the grip is trusted and the geometry is not consulted.
2. **Part names.** `mesh_lib.Mesh.write_txt` now writes `# part <name> <first quad line> <count>` comment lines (one per contiguous run of a part's quads) and derives the
   directive from the part names (`Mesh.grip_fraction`: the quads of parts named pommel / grip / handle / hilt / wrap / haft / butt against the quads of parts named blade / tip /
   point / head / edge / spike / fuller; undecided when either is missing or both sit at the same height). A regenerated mesh therefore states its own grip. Nothing requires it.
3. **Geometry fallback** (a pointed class: dagger, shortsword, longsword, greatsword, rapier, katana, sabre, dadao): the widest part (the guard or an ornate guard) above 0.60 of the
   length **and** the area centroid above 0.54 **and** the top end narrower than 0.10 blocks across (a pommel or a point, not a blunt jaw or a wide blade end). The four reversed
   meshes satisfy all three; Giant Jawblade (blunt top), Krakeblade, Corpse Eater and every correct sword do not. The rule is deliberately conservative: a plain reversed sword with a
   small guard is not caught by geometry alone, which is why the directive exists.

`apply_weapon_fixes.py` has a new key `flip_y`: a half turn about the x axis through the middle of the length (`y -> -y`, `z -> -z`, normals' y and z negated, winding kept). No x
moves, so a blade keeps its edge on the same side and a curve its direction; the model's front and back faces swap, which only matters for a one-sided texture. It is an involution
(pivot recorded in the `# nusmp_fix` marker, so undo is exact), it is applied first (before `flip_x`, `rotate_z`, `head`, the size), and it writes `grip 0.120` into the mesh (the
generator's own `grip` line, if it wrote one, is replaced and restored on undo). The four ids carry `flip_y` in `weapon_fixes.json`; their generators have a comment saying so (do not
flip in both places).

**Runtime** (`WeaponNormaliser`): a mesh with `grip > 0.5`, or (no directive) the geometry rule above, is turned over along its length at load with one log line
`[nusmp] weapon mesh <id> (<class>) normalised: grip at the far end: turned over along its length`. A mesh with a `grip` line at or below 0.5 is never judged by the geometry.
`orient manual` still switches the whole net off. The log line, the `grip` directive parse and the new `normalise(..., gripDirective)` overload were type-checked against stubs only.

## 10.3 The new size table (apparent blocks: mesh length x the third-person scale)

| class | length | head min (across) | grip |
|---|---|---|---|
| dagger / knife | 0.45 - 0.70 | - | 0.20 |
| shortsword, rapier, sabre | 1.00 - 1.30 | - | 0.20 |
| longsword, katana | 1.40 - 1.80 | - | 0.20 |
| dadao (broad chopping sabre; the Time-Bender's card says greatsword class) | 1.80 - 2.40 | - | 0.20 |
| greatsword / claymore / zweihander (massive) | 2.20 - 3.00 | - | 0.22 |
| handaxe (an axe held in one hand), cleaver | 1.10 - 1.40, 0.90 - 1.20 | 0.50, 0.45 | 0.25 |
| battleaxe, crescent | 1.60 - 2.00 | 0.75 | 0.25 |
| greataxe | 2.40 - 3.00 | 1.10 | 0.28 |
| scythe | 2.00 - 2.60 | 0.60 | 0.30 |
| hammer, mace, club | 1.30 - 1.80 | 0.50, 0.50, - | 0.25 |
| greathammer (new class) | 2.20 - 2.80 | 0.90 | 0.28 |
| spear, glaive, halberd, trident | 2.80 - 3.60 | - | 0.33 |
| staff | 2.00 - 2.60 | - | 0.40 |
| whip (extended), coilwhip (coiled or short stock) | 1.60 - 2.40, 1.10 - 1.70 | - | 0.15 |
| crossbow, bow (new class) | 1.00 - 1.20, 1.30 - 1.60 | - | 0.50 |
| wand | 0.55 - 0.85 | - | 0.20 |
| shield | 0.90 - 1.20 | - | 0.50 |
| handitem (new class; the old `item` class stays in the table for old data) | largest dimension 0.50 - 0.90 | - | 0.50 |

The table lives in the same three places as before (`weapon_classes.py`, `weapon_classes.json` + its game copy, the Java fallback in `WeaponClasses.java`; one test checks all three).
`ITEM_MIN_EXTENT` is 0.50 (was 0.40). `STATE_SUFFIX` now also strips `_left`, so the left half of a pair follows its right half's class and fix.

**Review.** Every id was classified again by its name and card text. Changes: `wavebreaker` longsword (a quick nodachi), `sword_crystal` shortsword ("crystal short sword"),
`rimeheart_runeblade` greatsword, `doom_hammer` hammer, `sorrows_edge` crescent (1.6 - 2.0, a heavy axe), the 24 held items `handitem`, and the former "excluded" ids (Elsdocia, the three
pairs) are in the table now (`excluded` is empty): Elsdocia and its legacy mesh greatsword, Twin Hook Swords and the Twin Swords of Deflection shortsword, Whirling Blades sabre.

**Retargeting** (`tools/relics/retarget_weapon_sizes.py`, `--write` rebuilds `weapon_fixes.json`): the mesh as the generator wrote it plus the orientation keys already in the table is
measured; a weapon below its class minimum (3 % margin) is brought to `min + 0.40 (max - min)` (a boss weapon to 0.50 - 0.55: Elsdocia, the Rimeheart Runeblade, Hunger, ...), scaled about
the grip; a weapon already in range or above it is untouched (nothing is shrunk: the Severing Greatsword at 3.33 blocks and the Demon-Destroyer at 2.98 stay); a head below its class
minimum is grown to 1.12 x the minimum by the existing `head` fix; a held item below 0.5 blocks is raised to 0.6 (an `extent` entry). An id with several mesh files (the crossbow draw stages,
a pair's left half) gets a `scale` entry instead of `apparent`, so every state grows by one factor and the draw stages do not jump in size. 88 ids have an entry (103 mesh files carry a fix, with the
state meshes and the pair halves). Examples (apparent blocks before the fix -> after): Buster Sword 1.50 -> 2.60, Dream Glass 1.50 -> 2.52, Magic Tool Spear 2.10 -> 3.12, Dead Blade 1.85 -> 3.12,
Hunger 1.45 -> 2.73 (head 0.80 -> 1.23), Sorrow's Edge 1.25 -> 1.76 (head 0.38 -> 0.83), Staff of Moonlight 1.85 -> 2.24, a long sword 1.05 - 1.20 -> 1.56, a short sword 0.62 - 0.9 -> 1.12,
daggers 0.37 - 0.42 -> 0.55, the Wand of Bubbles 0.52 -> 0.67, the needler 0.61 -> 1.08.

**Elsdocia** is a greatsword-class crystal blade now: 2.64 blocks apparent (was 1.25; the head, i.e. the guard, is 0.83 across). `elsdocia_rebuild.py` keeps measuring the reference as a 1.25 m
longsword (every proportion, facet and texel density is the reference's) and places it with `APPARENT_M = 2.64`, the same number as its entry in `weapon_fixes.json`, so
a rebuild and a fix agree. The three-view check is `docs/preview/weapon_size_1_34/size_after_01.png` (top left).

**The sprite-only bow.** `magic_tool_bow` is a vanilla-model sprite item, not a mesh; its json now carries the vanilla bow display with the third-person scale 1.4 (was 0.9, about 1.4 blocks
apparent) and the first-person scale 0.95. The Genesis Demon-Slayer (`DemonSlayerRenderer`) was already at 1.615.

## 10.4 Runtime thresholds and first person

* `WeaponClasses.RUNTIME_SMALL` is 0.75 (was 0.60): a mesh below 75 % of its class minimum is scaled up to the minimum about the grip (at most 4 times; an item to `ITEM_MIN_EXTENT`).
  It still never enlarges a head (the offline `head` fix does that) and never shrinks.
* **First person.** `display_presets.APPARENT_MAX_BLOCKS` 1.0 -> 1.5 (`OversizeGuard.APPARENT_MAX` likewise; the guard steps in above 1.5 x 1.15). The camera bounds of the tool (every
  corner of the mesh box in front of the near plane, the tip inside the field of view at a ratio of 1.32) still decide for the longest weapons: greatswords come out at s = 0.31 - 0.40
  (about 1.0 block apparent, as the view allows), battle axes and heavy blades at 0.47 - 0.67 (1.0 - 1.4 blocks), hand axes at the vanilla 0.68. `SCALE_FLOOR` 0.28 -> 0.20 so the 3.7-block spear and
  trident (0.26 and 0.28) get their tip into view instead of being listed out of view (none is now). Ground scale floor 0.3 -> 0.2 and item frame floor 0.55 -> 0.35 so a 3-block weapon
  does not overhang the frame or tower over a dropped item. `python tools/relics/display_presets.py --apply --include-excluded` was run at the end (52 model jsons rewritten, the manifest is
  `tools/relics/display_presets_manifest.json`).

## 10.5 Tools and tests

* `audit_weapon_scale.py` prints `grip_at` and has `grip_far_end` in the flags; it skips ids starting `djinn_weapon_` (the Djinn temporary weapons are another release's), as do
  `apply_weapon_fixes.py`, `retarget_weapon_sizes.py` and `display_presets.py` (which has `--include-djinn` to process them).
* `python tools/relics/test_weapon_scale.py` (35 tests: the new table equals the owner's ranges, nothing below its class minimum, every greatsword at least 2.2, daggers stay small,
  Elsdocia at least 2.2, every state of one weapon has one scale, every mesh is held by its hilt, the four reversed swords are found in the generators' output, the flip is an involution that keeps
  every x, the grip line is written and restored), `python tools/relics/test_display_presets.py` (19), and the Java tests `WeaponClassesTest`, `WeaponNormaliserTest`, `WeaponOrientationTest`,
  `WeaponMeshesTest` (the runtime normaliser has nothing to do on any shipped mesh), `OversizeGuardTest` (all compiled and run with a JUnit console here: 33 tests).
* Adding a new weapon: build the hilt at low y, give the generator real part names (pommel, grip, guard, blade, tip ...) so `write_txt` states the grip, class it in `weapon_classes.json`, run
  `retarget_weapon_sizes.py` (dry run first), then `apply_weapon_fixes.py --apply`, `display_presets.py --apply`.

Not verified in game: nothing here was run in Minecraft. The held views are the software reproduction of the vanilla hand chain (section 9); the grip-end flip of the four swords, the
sizes in the hand and the first-person fit of the longest weapons should be looked at once in game. The z flip of the four swords swaps the two broad faces of the mesh (the texture is painted
per part, so the difference is small but look at the Windcutter's ribbon and the Flamecutter's wings).
