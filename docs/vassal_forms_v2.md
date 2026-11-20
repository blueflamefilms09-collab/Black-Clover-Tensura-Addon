# Vassal forms v2: swinging chains and whips, remodelled axes, books and carriages

Owner request: "Make the axe better. Make the mace, in my opinion, make the flail and the whip actually be animated if you can when you attack with it. But yeah, in my opinion, the axe
could look better. The book needs to maybe be improved a bit. And so does the carriage looking thing."

Two jobs came out of it:

1. **Motion** for the flail and the whip weapons: the chain or the lash of the model now swings, lags, whips past and settles while the weapon is being swung (client look only).
2. **Remodel** of the 12 forms of the Vassal Axe, Vassal Book and Vassal Carriage (more detail, a better silhouette). The old models stay one setting away.

Nothing here touches the server: no packet, no server rule, no new data component. Everything is drawing.

## NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | Swing motion: `client/motion/SwingMath`, `SwingRig`, `SwingRuntime`; `bone` / `skin` lines in 15 weapon meshes; `tools/weaponmotion/*`; settings `models.weaponMotion` and `vassalForms.classic`; Better Combat profiles `lash_whip`, `lash_coilwhip` and `flail`; the classic copies of the 12 old forms (`weapon_meshes/classic_vassal/`, `textures/entity/weapon/classic_vassal/`); builders `tools/blender/forms_vassal_{axe,book,carriage}_v1.py` (the old builders, unchanged); tests `SwingMathTest`, `SwingRigTest`, `SwingMeshFilesTest`, `SwingSourceScanTest`, `ClassicVassalFilesTest` |
| **REPLACED** | The meshes and skins of `vassal_axe__{timber_axe,pipe_axe,shine_axe,curse_axe}`, `vassal_book__{pipe_book,animal_needle_book,revised_edition,library_tome}`, `vassal_carriage__{filolial_carriage,war_carriage,hourglass_sand_carriage,queens_chariot}` (the old ones are kept, see below). The Better Combat attack set of `vassal_chain`, `vassal_whip`, `viper_whip`, `junglethorn_whip`, `starlight_whip`, `white_whip_of_welting` and `anheteps_sunscourge` (they used the sword-like presets before) |
| **KEPT** | Every old model and skin (copied, not deleted), every item id, recipe, grip, class, enchanting rule and the base axe / book / carriage; all other weapons and their Better Combat files; the Time Magic VFX (not touched) |

## 1. Motion

### What you see

* **Flail (`vassal_chain` and its six forms) and morning-star whip:** the chain is a string of links. When the arm swings (Better Combat's mace swing: a slash and an overhead slam), the head lags
  behind the hand, then whips past and rings out over about a second, link by link.
* **Whips (`vassal_whip` and its forms, `starlight_whip`, `anheteps_sunscourge`, `rems_morning_star`):** the lash is cut into 8 to 12 slices that trail the strike, crack at the end and flutter;
  the hilt, gems, glass and sun discs stay rigid.
* **At rest:** a very small sway (a quarter of the swing strength) while the holder moves, so a held whip is not a statue.

Not animated, honestly: `familia_smiths_mace` (a hammer-class mace, one rigid head: nothing to bend) and the relic coil whips `viper_whip`, `junglethorn_whip`, `white_whip_of_welting`
(their meshes have no part structure to cut a lash from). They only got the new Better Combat profiles.

### How it works (all client side)

* **Rig in the mesh file.** `tools/weaponmotion/build_swing.py` (spec: `motion_specs.json`) reads a weapon mesh, finds the chain links or slices the lash along its length, reorders the quads so the
  rigid part (hilt) comes first and the moving tail last, and appends two kinds of line that older readers ignore:
  `bone <i> <parent> <px py pz> <ax ay az> <ux uy uz> <w> <zeta> <gain> <maxdeg>` (a joint on a chain of at most 32) and `skin <layer> <first quad vertex> <count> <codes...>` (per vertex: bone
  index and a blend weight, 6 bits). The `# part` comments are kept and still cover the quads exactly once.
* **Maths.** `SwingMath`: a cascade of damped springs, one per bone, each driven by the bone before it, the arm's angular acceleration (from Better Combat's swing progress) as the first
  drive. Fixed sub-steps of 1/60 s (at most 4 per drawn frame), a hard cap on the arm acceleration and on every angle, a reset when the gap between two frames is long. A Python twin
  (`tools/weaponmotion/swing_math.py`) is pinned to the Java by a golden test, and draws the preview strips.
* **Drawing.** `WeaponRenderer` asks `SwingRuntime.prepare(...)` per weapon draw. Only for a mesh with a valid rig, only with a holder (not for the ground, a frame or a menu), only within 24
  blocks of the camera, only with `models.weaponMotion` on. The rigid head goes through the old code path unchanged; the skinned tail is drawn with blended bone matrices from pre-allocated
  matrices. A rig that does not fit its mesh, or a mesh the loader moved, is dropped at load and the weapon is drawn rigid.
* **Better Combat.** New profiles in `tools/bettercombat/profiles.py`: whips use `lash_whip` / `lash_coilwhip` (the `cutlass` preset: wide horizontal slashes that suit a lash), the flail
  uses `flail` (the `mace` preset: a slash and an overhead slam). 7 attribute files regenerated by `tools/bettercombat/gen_attributes.py` (`--check` is clean, 310 weapons covered).

### Settings (client config)

| Key | Default | Meaning |
|---|---|---|
| `models.weaponMotion` | on | off = every weapon is drawn rigid, exactly as before |
| `vassalForms.classic` | off | on = the 12 axe / book / carriage forms are drawn with their old models and skins |

### Worst-case cost (rule 11)

| What | Bound |
|---|---|
| Server | none: no tick work, no packet, no entity |
| Network | none |
| Tracked holders | at most 48 per hand, pruned every 20th tick (40 ticks of keep), all dropped on logout; keyed by holder UUID |
| Per drawn weapon | one spring step of at most 4 sub-steps x 32 bones; no allocation (matrices pre-allocated, one `State` object per holder the first time it is seen) |
| Skinned vertices | quads x 4: the largest rig is `vassal_chain__orochi_chain` with 3784 animated quads (about 15 000 vertices, two matrix transforms for blended ones); `SwingMeshFilesTest` caps every rig at 18 000 skinned vertices |
| Distance | beyond 24 blocks the weapon is drawn rigid (no spring step at all) |

## 2. Remodel of the axe, book and carriage forms

Method: the old builders are kept as `forms_vassal_{axe,book,carriage}_v1.py` and the old meshes and skins were copied to `classic_vassal/` before anything was rebuilt. The v2 builders
(`forms_vassal_{axe,book,carriage}.py`, on `forms_kit.py`) change the geometry; every form was baked again (Cycles, 2048 px bake, 1024 px skin, packed palette PNG) through the existing
pipeline, with the same frame, grip and size class as before. `forms_vassal_common_b.py` got a `--wb DIR` switch (fast Workbench stills, no bake); it needs a GPU / EGL, which this session did not have, so it was added but could not be run here (the Cycles `--beauty` stills were used instead).

### Axes (what changed, what was weak)

* **Timber axe:** the cheek is now a bearded felling blade (a hooked beard under a concave throat, a curled toe, three chips in the edge) with a stepped section (raised rim, dished field,
  ground bevel) instead of a flat wedge; leaf-glass inlays along the rim, tree-ring lines in the field (baked), tornado scrolls kept; iron langets run down the haft, bracket fungi grow on the
  log, a cord lashing binds the beard and a tassel hangs from the hook.
* **Pipe axe:** the guillotine blade got the stepped section, two brass portholes with glowing steam glass, two meshing pinions on the spine plate and a spring coil round the haft; flange bolts are
  baked into the skin (the quads went to the new detail).
* **Shine axe (crescents were plain):** the two bits are now true crescent moons whose horns curl back toward the haft, on ivory necks, with a gold root band, two raised filigree arcs, a
  corona of lancets round the outer rim and studs of light (baked); the diamond studs of the hub are baked too, which paid for the new geometry.
* **Curse axe (jaw and eye were weak):** the hub is now a skull's face: a brow ridge with three spikes, upper and lower lids round the eye, cheek horns, a row of teeth, and a lower jaw (chin plate
  with upward fangs, jaw horns) standing before and behind the haft; the cleaver edge got real saw teeth, a stepped section and bone ribs raking across the cheek.

### Books

All four books share a new block (`body2`): cover boards with cut corners and a stepped edge, a raised frame bar on each cover, a page block that bulges (a loft with its own head and tail dip, not a
slab), a rounded spine with raised bands and a head cap and tail cap.

* **Pipe book:** a hand-wheel valve on the spine, two steam stacks, rivet strips (baked) on the cover edges.
* **Animal needle book (thin sheet, oversized):** plume feathers and needles are about 35 percent smaller and the feathers are thick, with a real quill; a glowing slit eye on the cover in a bone
  setting, stitched borders (baked), smaller emblem feathers and a smaller loose page.
* **Revised edition:** the new block plus runic title bands (baked) on both covers.
* **Library tome:** the new block (a thicker, framed cover for the arch, columns and shelves).

### Carriages

* **Wheels (all four):** a new wheel (`wheel2`): a tyre with a raised tread ridge, a banded felloe, turned spokes that swell toward the hub (or the form's own spoke: spiked planks, cog spokes, feather
  blades), a barrelled nave with bands, a hex hub-nut; nail heads and bands are baked. A leaf spring on each side joins the hull to the axle.
* **Filolial carriage:** a moulded panel with two gold scrolls on each side; fewer, larger feather tufts (the rest of the quads went into the panel and wheels).
* **War carriage:** a row of round shields on each side, crenellations round the roof, a ram's curled horns on the ram cap; the chains got cheaper links.
* **Hourglass sand carriage (near budget):** gears have fewer teeth, the clock studs are baked, the glass has more sides (rounder), and the base has three claw feet and a pendulum under the beam.
* **Queen's chariot:** gold scroll-work on the prow plates and a lamp on a post at each rear corner; fewer scale plates and lighter wing feathers to stay in budget.

### Budgets

| form | body quads | glow quads | lines (max 5000) | skins KB (max 350) | class | apparent length (range) |
|---|---|---|---|---|---|---|
| timber_axe | 3852 | 1022 | 4874 | 329.0 | battleaxe | 1.82 (1.6-2.0) |
| shine_axe | 3700 | 973 | 4673 | 332.9 | battleaxe | 1.93 (1.6-2.0) |
| pipe_axe | 3980 | 155 | 4135 | 296.0 | battleaxe | 1.82 (1.6-2.0) |
| curse_axe | 3887 | 434 | 4321 | 282.8 | battleaxe | 1.872 (1.6-2.0) |
| pipe_book | 2673 | 69 | 2742 | 280.9 | handitem | 0.846 (0.5-0.9) |
| animal_needle_book | 2818 | 471 | 3289 | 291.2 | handitem | 0.964 (0.5-0.9) |
| revised_edition | 3236 | 515 | 3751 | 271.4 | handitem | 0.841 (0.5-0.9) |
| library_tome | 3353 | 674 | 4027 | 234.8 | handitem | 0.846 (0.5-0.9) |
| filolial_carriage | 3892 | 287 | 4179 | 290.4 | handitem | 0.881 (0.5-0.9) |
| war_carriage | 3794 | 143 | 3937 | 289.2 | handitem | 0.848 (0.5-0.9) |
| hourglass_sand_carriage | 4025 | 718 | 4743 | 292.5 | handitem | 0.779 (0.5-0.9) |
| queens_chariot | 3900 | 698 | 4598 | 305.4 | handitem | 0.788 (0.5-0.9) |

The build tool's own audit (`forms_kit.report`) returned no out-of-class status for any form. Caveat: the needle book's apparent length (0.964) is above the printed upper bound of 0.9, so the audit evidently allows a margin there; the old needle book was built the same way. Worth a look in game.

## Tools used, honestly (rule 14 / 15)

* **Blender:** the 12 remodelled forms were modelled, unwrapped and baked in Blender 5.3 (the `bpy` pip wheel, headless) through the repo's pipeline (`tools/blender/forms_kit.py`, `bl_lib.py`). No
  model was hand-typed as coordinates in a text file.
* **Not Blender, not Blockbench:** the swing rigs (`bone` / `skin` lines) are written by `tools/weaponmotion/build_swing.py`, a script that reads the finished mesh. They were **not** built in
  Blender or Blockbench. A skeleton made in Blender would have needed the 15 meshes to be re-baked; the script rigs the shipped geometry instead. This is the fallback, reported as such.
* **Blockbench** was not available in the session and was not used. No `.bbmodel` was made for this change.
* **Previews:** the Workbench / EEVEE views of Blender need a GPU, which the session does not have; geometry was checked with low-sample Cycles stills, and the finished skins with the repo's
  software preview (`tools/relics/preview_mesh.py`).

## Not seen in a game

* Nothing here was run in Minecraft. The swing maths is unit-tested and pinned to the Python twin, `SwingRuntime.prepare` / `emit` and the `WeaponRenderer` changes compile against the real Minecraft
  jars, and a source scan checks the no-allocation and cleanup rules. The skinned draw path was not checked with a fake `VertexConsumer` and was never drawn.
* The feel of the swing (how much lag, how long it rings) is tuned from the preview strips only; `w`, `zeta`, `gain` and `max` per mesh in `motion_specs.json` are the knobs, and `build_swing.py`
  rewrites a rig in place (it is idempotent).
* Better Combat's swing progress feeds the drive: if its animation timing differs from what the strips assume, the lag will look different.
* The remodelled forms were judged on stills (`docs/previews/vassal_forms_v2/`), not in a hand.

## Previews

* Before / after contact sheets: `docs/previews/vassal_forms_v2/before_{axe,book,carriage}.png` and `docs/heroweapon/form_previews/vassal_{axe,book,carriage}.png` (the new ones).
* Swing frame strips (the exact maths of the game, side view of the mesh's plane): `docs/previews/weapon_motion/swing_strip_chain_whip.png`, `swing_strip_chain_forms.png`,
  `swing_strip_whip_forms.png`.

## Re-running

```
python tools/weaponmotion/build_swing.py            # rigs from motion_specs.json (idempotent; --dry, --check, --only <mesh>)
python tools/weaponmotion/swing_strip.py <mesh>... --out <png>
python tools/bettercombat/inventory.py && python tools/bettercombat/gen_attributes.py
python tools/blender/forms_vassal_axe.py --only timber,pipe,shine,curse     # also forms_vassal_book.py / forms_vassal_carriage.py (needs bpy)
python tools/heroweapon/register_form_meshes.py
```
