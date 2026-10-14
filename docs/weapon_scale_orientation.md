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
