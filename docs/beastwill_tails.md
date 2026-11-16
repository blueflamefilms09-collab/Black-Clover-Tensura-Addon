# Beast Will cores and mesh tails

Two things in one change: (a) every Beast Will core opens the acquire UI, (b) the tails and tentacles of the 3D Beast Will forms are
real Blender meshes that sway, instead of stacks of cubes.

## (a) The cores

Why "only a few work": a bare core (a plain `/give`, no data component) returned FAIL from `use()` and did nothing; the creative tab listed only 4 cores
(two of them below their own will's lowest rank, so the ask was refused); and a refusal only showed on the action bar.

* `beastwill/BeastWillCores.java` (NEW, pure): all 27 wills with the will's own beast name and its own lowest rank.
* `BeastWillItems.creativeStacks()`: all 27 cores, each at its will's rank (REPLACES the old 4-entry list; the drops and commands are untouched).
* `BeastWillCoreItem.use()`: a bare core is stamped with the first will the player does not hold, then the ask opens. If `open()` refuses, the reason goes to chat.
* Self-test `beastwill.cores.all` (smoke list: `beastwill.cores`): every creative core opens an ask on a stand-in player.
* Pure test `BeastWillCoresTest`: the table matches the 27 data files (beast, rank).

Not changed: cores already in a world keep their stored rank (an old Dragon S / Elderwood A core still shows the grey rank refusal, with its reason).
The Mana Core "+ Will" variants (`RankCoreItem`) offer their will only on sneak + use (`ManaCoreActives.beastForm`); if those are the ones that "give no UI", that is a separate path.

### Can anything else in the transform path throw?

* `GeoModelData.parse` and `GeoAnim.parse` were not wrapped: a malformed file threw in the render thread. Now caught, logged once (the result is cached), and the model draws nothing / the clip stays at rest (`client/geo/GeoModels.java`).
* `TensuraBridge.maxEp` already returns 0 on any Throwable. The `BeastWillNet.Preview` string limits hold for all 27 wills (longest beast 46 of 64, affinity 10 of 24, id 25 of 40).
* `GeoDraw.paint` itself and the client `onPreview` stay unwrapped; I found no throw there with a well-formed model.

## (b) Mesh tails and tentacles

| Form | Kind | Chains x parts | Quad lines (body + glow) | Far copy | Skin / glow |
|---|---|---|---|---|---|
| dragon | tail | 1 x 8 | 223 | 119 | 55 / 3 KB |
| nidhoggr | tail | 1 x 9 | 437 | 222 | 39 / 3 KB |
| jormungandr | tail | 1 x 9 | 248 | 124 | 44 / 4 KB |
| serpent | tail | 1 x 7 | 207 | 98 | 43 / 4 KB |
| apophis | two snake heads | 2 x 8 | 735 | 297 | 44 / 6 KB |
| typhon | two snake heads | 2 x 7 | 621 | 293 | 52 / 3 KB |
| kitsune | nine fluffy tails | 9 x 6 | 1363 | 632 | 72 / 3 KB |
| kraken | 12 tentacles with suckers | 12 x 4 | 1416 | 694 | 63 / 1 KB |
| scylla_charybdis | 6 waist tentacles | 6 x 4 | 828 | 407 | 60 / 1 KB |

Files per form: `entity_meshes/bwtail_<stem>.txt` + `_lod.txt`, `textures/entity/boss_mesh/bwtail_<stem>.png` + `_glow.png` (512 x 512, palette PNG), previews in
`docs/beastwill_tails/previews/<stem>_side.png` and `_back.png` (the back view is the sway pose).

### How it reads

* A tail is a chain of parts named `sw<chain>_<segment>`; each segment overlaps the previous one with a collar, so a bend leaves no gap.
* `beastwill/BeastTailRules.java` (pure): the table above, part name to role number, and the sway (a wave that runs down the chain, bigger toward the tip, chains out of step).
  `tools/blender/beastwill_tails.py` mirrors the sway for its previews.
* `client/aura/BeastWillTails.java`: the placer. It anchors the mesh on the player's body part (the same chain as the cube piece, so a crouch or a lean carries it),
  turns each segment about its joint on top of the one before it, and draws through `BossMeshDraw.drawParts`. Forged in and shattered out with the form (`EquipAnimation`).
* `BeastWillForms`: for a form with a mesh tail, the full form is built without the cube tail (and for Kraken / Scylla with the body that has no cube tentacles,
  `bwf_<stem>_arm_body_nt`, made by `tools/beastwill/tails/make_body_nt.py`).
* `BossMeshDraw` accepts the `sw*` roles (unknown names fall back to `BeastTailRules.role`); the 11 boss roles are unchanged.

### Settings

* Client `beastwill.meshTails` (default ON, `BeastWillSpiritClientConfig`): OFF = the cube tails of 3.76.0, exactly as before. `classicForms` ON also keeps the old cube forms.
* A form without a mesh tail keeps its cube tail. A missing or broken mesh file answers "not usable" once and the cube tail is drawn.

### Worst-case cost (rule 11)

* Server: nothing. It is client drawing only; no packet, no tick work, no new state.
* Client per frame: at most 1500 extra quads per player (the biggest is the Kraken's 1416); the far copy (about 40-50 percent of the quads) from 40 blocks; the glow pass only within standard level;
  nothing beyond 48 blocks; at most 4 players per frame get a mesh tail (the rest keep the cube tail). Worst frame: 4 x 1416 = about 5.7k extra quads, in place of those players' cube tails (about 100-250 quads each).
* No allocation per frame (static scratch matrices and angle array; pinned by `PerfSourceScanTest.theBeastWillTailPathAllocatesNothingAndIsCapped`). PerfProbe section `c.beastwill.tails`.

## NEW / REPLACED / KEPT

* NEW: `BeastWillCores`, `BeastTailRules`, `BeastWillTails`, 9 mesh sets + skins + previews, the two `_nt` body pieces, the `meshTails` setting, `tools/blender/beastwill_tails.py`, `tools/beastwill/tails/make_body_nt.py`,
  tests (`BeastWillCoresTest`, `BeastTailRulesTest`, a scan test in `PerfSourceScanTest`), self-test `beastwill.cores.all`.
* REPLACED: the 4-entry creative core list (now 27); the bare-core "does nothing" path; the cube tail / cube tentacles on the nine forms (while `meshTails` is ON).
* KEPT: every cube tail piece, every geo file, the cores' drops and commands, the Time Magic VFX (untouched).

## Made in Blender, and what could not be

All nine tail sets were modelled, unwrapped and baked in Blender (`bl_lib` / `bl_entity.finish_entity`, `bpy` 5.3.0, CPU bake at 1024 into a 512 skin).
Nothing here is hand-placed from code boxes. The cube bodies (`bwf_*`) are still the generated cube pieces; only the tails and tentacles are new meshes.

## Not checked (honest caveats)

* Nothing was seen in a running game and the Java was never compiled (no Gradle here; CI is the first compiler). The previews show the tail alone, without the body, using a Python copy of the sway.
* Whether the tails sit at exactly the right spot on the body, and whether the sway direction looks right in game, is unverified; the anchor mirrors `NoelleFormRig.draw` and `DevilFormLayer`.
* The kitsune back preview is seen nearly edge-on, so the nine tails read as one fan; it should be checked in a game from behind.
* Meshes are cached until a resource reload; `GeoModels.clear()` now clears them too.
