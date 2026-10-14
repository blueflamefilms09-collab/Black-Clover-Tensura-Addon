# Asset pipeline

The 3D asset and animation pipeline of the mod, section by section (each package of the Multiverse overhaul writes its own section between its markers).

<!-- m4 begin -->
## Curio attachment (head, wrist, belt)

How a worn relic's model is authored, attached to the player and animated. The code is `client/relics/worn/*` (the registry `WornRenderers`, the three renderers, the maths `WornMath`) and `client/relics/RelicWornLayer` (which calls the registry). Curios is not a compile dependency here (it is read reflectively through `item/CuriosLink`), so the `ICurioRenderer` of the MAS prompt is this layer plus the registry; the behaviour is the same (bone tracking, rotation interpolation, emissive layers).

### The player model's frame (vanilla `PlayerModel`, layer space)
Model units are pixels (16 to a block). In the layer's pose the origin is the neck line, **y points down**, the front is **-z**, the player's right is **-x** (the right arm's pivot is at x = -5). Feet are 24 px below the origin (y = +24). A GeoLite file (Blockbench / `tools/geo_builder.py`) is authored y **up**, feet at the origin, front on -z, +x the player's left; `GeoDraw.Space.PLAYER` converts (`y_layer = 24 - y_file`), so the file's neck line is y = 24 and the hips are y = 12.

| Part (`PlayerModel`) | Pivot (x, y, z) in layer space | Box relative to the pivot | Notes |
|---|---|---|---|
| `head` | (0, 0, 0) | x -4..4, y -8..0, z -4..4 | helmet armour inflates by 1 (x, z +-5, top -9); hat layer 0.5. Crouch: y = +4.2 |
| `body` | (0, 0, 0) | x -4..4, y 0..12, z -2..2 | chest plate inflates by 1 (x +-5, z +-3); leggings by 0.5. Crouch: y +3.2, xRot 0.5 |
| `rightArm` | (-5, 2, 0) (slim: (-5, 2.5, 0)) | x -3..1 (slim -2..1), y -2..10, z -2..2 | the wrist is at y 7.4..8.8; sleeve layer inflates 0.25. Crouch: y 5.2 |
| `leftArm` | (5, 2, 0) (slim (5, 2.5, 0)) | x -1..3 (slim -1..2), y -2..10, z -2..2 | |

Rules of attachment:
1. **Always attach through the part**: `part.translateAndRotate(pose)`. It applies the part's pivot, its rotation (ZYX) and everything `setupAnim` wrote into it this frame: walk swing, the attack swing's rotation and offset, a raised bow arm, the sneak offsets, a swim lean. A relic that follows `model.head`, `model.rightArm` or `model.body` therefore follows all of them with no extra maths.
2. After `translateAndRotate`, `pose.scale(1/16, 1/16, 1/16)` and draw in pixels; or leave the scale and draw a GeoLite model with `GeoDraw.paint(..., Space.PLAYER, ...)`, which applies its own 1/16 and the +24 px.
3. Armour sits **outside** the base box: keep worn pieces at least 0.3 px outside the sleeve layer, and 0.05 px outside the chest plate (x +-5, z +-3) for a belt, otherwise they z-fight with the armour layer.
4. Slim arms are 1 px narrower on the outside (x -2 instead of -3): read `WornCtx.slim()`.
5. Draw nothing in first person (`WornCtx.firstPerson()`): the hand there is the item renderer's.
6. A renderer fetches **one buffer per render type, writes everything of that type, then asks for the next one** (a second `getBuffer` ends the first builder). The layer draws every relic that has a registered renderer after the generic ones for this reason.
7. Floating parts (the crown's shards) give back part of the head's rotation (`pose.mulPose(Axis.XP.rotation(-head.xRot * k))`, then `Axis.YP.rotation(-head.yRot * k)`) so they hang in the air and lag a quick head turn; k = 0.85 for yaw, 0.6 for pitch. Their orbit angle is `age * deg-per-tick` with `age` = the layer's `ageInTicks` (tick + partial tick), which is the interpolation between ticks.

### Blockbench bone hierarchies
**Head: Dante's Gravity Crown** (procedural in `DanteGravityCrownRenderer`; to author it in Blockbench use this skeleton with the model origin at the head pivot, y up in the file = -y in layer space):
```
head_pivot            pivot (0, 24, 0)            <- the neck line of the file frame; the whole model follows model.head
  crown_band          pivot (0, 24, 0)            4 slabs, outer face 5.5 px out, y 24 + 7.2 .. 24 + 9.0
    spike_0 .. spike_7  pivot at each spike base  tall front spike 5.0 px, corners 4.0, sides 3.4..3.8, tilted 0.7 px outward
    brow_setting        pivot (0, 24 + 8.1, -5.7) the gem (emissive layer)
  floating            pivot (0, 24 + 14, 0)       child of head_pivot, counter-rotated by 85% yaw / 60% pitch at run time
    shard_0 .. shard_5  pivot (9.5, 0, 0) rotated about floating's y at i * 60 degrees + age * 2.4 deg/tick; each a 0.8 x 2.4 x 0.8 px octahedron + a 1.25 x 3.3 x 1.25 glow shell
    lens_ring           pivot (0, -2.4, 0)        flat annulus r 8..9.5 px, tilt 7 degrees, spins at 1.1 deg/tick (additive layer)
```
**Wrist: Zenon's Spatial Ring** (`ZenonSpatialRingRenderer`; origin at the right arm's pivot, y down in layer space):
```
right_arm_pivot       pivot (-5, 2, 0)            <- model.rightArm (swing, attack offset and sneak included)
  wrist_band          pivot (-1, 8.1, 0)          4 slabs 0.4 px thick, 0.25 px outside the sleeve, y 7.4..8.8, dark rims
    rift_core           pivot (-1, 8.1, -2.95)    a 0.9 x 2.3 x 0.6 px slit diamond on the front of the band (emissive)
  gyro                pivot (-1, 8.1, 0)          turns about the arm's axis at 5 deg/tick
    ring_a              annulus r 3.65..4.2 px, tilted +24 degrees
      fragment_0..2     0.7 x 1.8 x 0.7 px diamonds on ring_a at 0, 120, 240 degrees
    ring_b              annulus r 2.55..3.1 px, tilted -38 degrees, turns the other way at 1.35 x
```
**Belt: Artifact Mimic** (GeoLite, `tools/relics/waveboss_mimic_geo.py` writes it; frame = the player's file frame, y 24 = neck line, y 12 = hips):
```
root                  pivot (0, 0, 0)
  straps              pivot (0, 13.4, 0)          front/left/right/back leather straps (outer faces at x +-5.06, z -3.06 / +3.06), six gold studs
  chest               pivot (0, 13.0, -3)         the mimic base on the front: box 7 x 3 x 3 at y 11.6..14.6, z -6..-3; lower teeth and fangs
    lid                 pivot (0, 14.6, -3)       the JAW: hinge on the base's back top edge; lid 7 x 2 x 3, domed top 6 x 1 x 2, the hasp, upper teeth and fangs
      eyes                pivot (0, 17.2, -5.6)   two amber eyes on the brow (blink, widen)
    tongue1             pivot (0, 14.6, -4)       lies on the base
      tongue2             pivot (0, 14.5, -6)     hangs out of the mouth
        tongue3             pivot (0, 13.9, -8)   the tip
```
Pivot conventions: a hinge bone's pivot is **on the hinge line** (the lid's at the base's rear top edge), a chain's child pivot is at the **end** of its parent's cube, and every bone that is animated by position (the chest lunges) has its pivot at its own centre of mass so a scale or rotation added later does not shift it. Rotation values are degrees added to the bone's own; **+x raises the front (-z) edge of the lid**; positions are pixels, +z is back, -z is forward, +y up (file frame).

### Mimic keyframe states
| Clip | Length | Loop | Keyframes (s) |
|---|---|---|---|
| `idle` | 3.0 s (60 ticks) | yes | closed; the lid breathes 0 -> 2.5 degrees; the tongue chain wobbles; one blink at 2.4 s (eyes scale y 1 -> 0.15 -> 1) |
| `yawn` (Hoard) | 1.2 s (24 ticks) | once | 0.0 closed -> 0.3 jaw open 48 degrees -> 0.9 held at 50 -> 1.2 closed; tongue lolls; eyes widen 1.3x |
| `bite` | 1.4 s (28 ticks) | once | **closed** 0.0 -> **jaw open** 0.45: lid 62 degrees, chest coils back 0.8 px, tongue snaps back, eyes widen 1.4x -> hold 0.55: 65 degrees -> **bite** 0.63: lid 0, chest lunges 2.2 px forward, eyes squint to 0.3 -> **recover** 0.7: lid shivers 6, 0, 3, 0 (by 1.0), chest settles by 1.4 |

The server fires the damage 12 ticks (0.6 s) after the cast, which is on the snap (0.55-0.63 s). The lengths are constants of `WornMath` and are checked against this file by `WornMathTest`. The bite stamp reaches the client as `WaveBossNet.Cue` (entity id, relic, kind); `WornCues` keeps (kind, game time) per player and `ArtifactMimicRenderer` plays `now - start + partialTick` ticks of the clip, then returns to `idle`.

### Blender -> `.geo.json` / `.animation.json` (the workflow of `tools/geo_builder.py`)
1. Build the model in Blender with one object (or empty) per bone, named like the table above; keep the **player's frame**: 1 Blender unit = 16 px, +Z up in Blender is the file's y up, the front faces -Y in Blender so it ends on -z in the file (`file.z = Blender.y`, `file.y = Blender.z`, `file.x = -Blender.x`). Origins of the objects are the bone pivots.
2. Use axis-aligned boxes (GeoLite draws cubes, not meshes): each box becomes `m.cube(bone, origin=(x0, y0, z0), size=(sx, sy, sz), uv=(u, v), inflate=...)` (origin is the minimum corner in file px). A turned cube carries `rotation=(rx, ry, rz)` and `pivot=...`.
3. Textures: the vanilla box-UV layout (the unfolded cube, width 2(sx + sz), height sz + sy; `faces()` in `waveboss_mimic_geo.py` shows the rectangles). Cubes of one kind and size may share one rectangle. A second PNG named `<model>_glow.png` is the emissive layer (drawn additive, full bright).
4. Animation: sample the Blender action at the key times and write `a.rot(clip, bone, {time: (rx, ry, rz)})`, `a.pos(...)`, `a.scale(...)` (`Anim` in `geo_builder.py`); angles are degrees about the bone's pivot, the rotation signs are those of the file frame (right-handed, y up): flip a sign in the script if Blender's differs, and look at `tools/relics/waveboss_preview.py`, which draws the rest, jaw-open and bite poses next to the body's boxes.
5. `m.save(anim)` writes `assets/nusmp/geo/entity/<key>_<name>.geo.json` and `animations/entity/<key>_<name>.animation.json`; `m.save_texture(img, glow)` the PNGs; then `python3 -B tools/geo_check.py <key>_<name>` (bones linked, uvs inside the sheet, animation bones exist).
6. In code: `GeoSpec.of(key, name)` names the four files and `GeoDraw.paint(pose, buffers, spec, clip, seconds, Space.PLAYER, Layer.CUTOUT, light, 0xFFFFFFFF, glowArgb)` draws it from the pose of the part it hangs on.
<!-- m4 end -->

<!-- m5 begin -->
## Oversized weapons

Every 3D hand weapon is a baked mesh (`assets/nusmp/weapon_meshes/<id>.txt`, drawn by `client/WeaponRenderer`) under the display transforms of its model json (`assets/nusmp/models/item/<id>.json`, parent `minecraft:builtin/entity`, because only a builtin/entity model is handed to the renderer). The transforms the first weapons were given are vanilla's `handheld` ones: scale 0.68 about the middle of the item's one-block cube. They suit a sprite sword (a diagonal 1.41 blocks long). A 2.5-block polearm at 0.68 is 1.7 blocks long in the hand, and because the hand pose turns the item's +x axis toward the viewer, the blade rises **toward the camera**: the tip passes through the near plane and the weapon is cut off. The prompt's rule is: **large in third person, reduced and re-anchored in first person**.

### Attachment rules for parts that are not curios

These are the rules for everything that is held, not worn (worn relics: "Curio attachment" above).

1. **The weapon's long axis is the mesh's +y**, the grip at the bottom, x across, z through (the same frame `mesh_lib` and `tools/gen_weapon_models.py` use). The `place ox oy angle scale ymin` line puts the butt (`y = ymin`) at `(ox, oy)` in the item's unit cube and turns the axis to `angle` degrees (45 = the old sprite's diagonal, 90 = upright); that is the whole attachment. Never move a weapon by editing vertices to make it fit the hand: change `place` or the display transform.
2. **The hand holds the grip, a quarter of the way up from the butt** (`GRIP_FRACTION = 0.25`). Build the handle (and the guard above it) to sit at that fraction of the total length; a weapon whose real grip is elsewhere (a staff held in the middle, a hammer held near the end) still gets the preset for 0.25, so tune it with a hand-made display block (below).
3. **Parts that move are separate named parts.** A weapon with a string, a limb, an arrow or a bolt names those parts `string*`, `limb*`, `arrow*`, `bolt*` (see "Bows and crossbows"); a weapon that unfolds is a GeoLite model with one bone per hinged part (see the workflow below). Static decoration is never a bone.
4. **Glow is a second texture, not geometry**: `textures/entity/weapon/<id>_glow.png`, drawn additive and full bright by the `glow` line's colour and pulse. A state or variant mesh (`_pull2`, `_genesis`, `_5leaf`, ...) shares the rest mesh's two PNGs, so it must reuse its UVs.
5. **The inventory icon stays the old 16x16 sprite** (`models/item/<id>_icon.json`); only the hand, ground, frame and head contexts use the mesh.

### The numbers

First-person camera space has x right, y up and z toward the viewer (the camera looks down -z). The hand rests at **H = (0.56, -0.52, -0.72)** blocks (`ItemInHandRenderer.applyItemArmTransform`). A point `p` of the item's cube lands at

`c = H + T + R * (s * (p - P))` with `P = (0.5, 0.5, 0.5)`

where `T` is the json translation / 16, `s` its scale and `R = Rx * Ry * Rz` its rotation in degrees (JOML `rotationXYZ`: the z turn is applied first). The vanilla `[0, -90, 25]` sends the model's +x to the viewer, its +y to up, and so makes a 45-degree blade rise and lean in.

| Constant | Value | Meaning |
|---|---|---|
| `LONG_BLOCKS` | 1.4 | A weapon longer than this gets a preset (the sprite sword's diagonal is 1.41) |
| `APPARENT_MAX_BLOCKS` | 1.0 | No weapon is longer than this in first person (`s * L`) |
| `SCALE_FLOOR` | 0.28 | Never smaller than this |
| `GRIP_FRACTION` | 0.25 | Where the hand is on the weapon |
| `Z_MARGIN` | 0.16 | Every corner of the mesh box stays this far in front of the camera (the near plane is 0.05) |
| `Y_VIEW` | 0.92 x tan 35 deg = 0.644 | The tip stays inside this share of the vertical field of view (the hand uses a fixed 70 degree field) |
| `X_VIEW` | 1.32 | The tip's `|x| / depth` may not exceed that of a sprite sword's tip (1.30; the hand sits at the window's edge) |

1. **Length** `L = (ymax - ymin) * scale` (blocks), read from the mesh file (`ymax` = the highest vertex, `scale` and `ymin` from its `place` line).
2. **Anchor.** Where the grip of an ordinary sprite sword sits under the vanilla transform: `A = H + T0 + R * (0.68 * (Gs - P))`, `Gs = (0.25, 0.25, 0.5)`, `T0 = [1.13, 3.2, 1.13] / 16`. Numerically `A = (0.631, -0.546, -0.732)`. **Every long weapon's grip is put on `A`**: `T(s) = A - H - R * (s * (G - P))`, in px `T * 16`. (The old transforms centred the weapon on the cube's middle, which put a long weapon's grip far below the screen.)
3. **Scale.** With the grip fixed, a point `q` of the weapon is at `c = A + s * R * (q - G)`: linear in `s`. Each limit is one inequality, solved in closed form: all eight corners of the mesh box `c.z <= -Z_MARGIN`; the tip `|c.y| <= Y_VIEW * (-c.z)` and `|c.x| <= X_VIEW * (-c.z)`; and `s * L <= APPARENT_MAX_BLOCKS`. `s = max(SCALE_FLOOR, min(0.68, all the bounds))`, rounded **down** to 3 decimals (rounding up could leave the view by a hair).
4. **Left hand.** Vanilla mirrors a left-hand transform (the y and z rotation and the x translation change sign), so the left entry gets the same scale and translation and keeps its own rotation.
5. **Third person is not touched.** `thirdperson_*`, `ground`, `fixed`, `head` and `gui` stay as they are, so the weapon stays large when someone else (or you, in F5) holds it.

Examples (rotation `[0, -90, 25]`; `T.x` is always 1.13): a 1.5-block weapon gets `s = 0.65`, `T = [1.13, 2.8, 0.96]`; a 2.0-block weapon `s = 0.50`, `T = [1.13, 2.5, 0.86]`; a 2.5-block polearm `s = 0.385`, `T = [1.13, 1.76, 0.53]`. The table is every weapon the tool has changed in this build (`tools/relics/display_presets_manifest.json` is the live list; "apparent" is `s * L`):

| Weapon | L | 1st-person scale | apparent | T.y | T.z | 3rd-person scale | 3rd-person length |
|---|---|---|---|---|---|---|---|
| `magic_tool_spear` | 2.47 | 0.385 | 0.95 | 1.76 | 0.53 | 0.85 | 2.10 |
| `otherworld_trident` | 2.23 | 0.447 | 1.00 | 2.90 | 0.93 | 0.85 | 1.90 |
| `staff_of_moonlight` | 2.18 | 0.459 | 1.00 | 3.15 | 0.79 | 0.85 | 1.85 |
| `astralis_the_eye_of_the_universe` | 2.18 | 0.405 | 0.88 | 2.48 | 0.87 | 0.85 | 1.85 |
| `dead_blade` | 2.18 | 0.459 | 1.00 | 3.37 | 0.85 | 0.85 | 1.85 |
| `severing_greatsword` | 2.12 | 0.467 | 0.99 | 2.47 | 0.86 | 1.57 | 3.33 |
| `catstaff` | 2.00 | 0.478 | 0.96 | 2.68 | 0.99 | 0.85 | 1.70 |
| `demon_destroyer_sword` | 2.00 | 0.496 | 0.99 | 2.52 | 0.86 | 1.49 | 2.98 |
| `giant_jawblade` | 2.00 | 0.462 | 0.92 | 2.35 | 1.00 | 0.85 | 1.70 |
| `licht_destroyer_sword` | 2.00 | 0.496 | 0.99 | 2.52 | 0.86 | 1.49 | 2.98 |
| `krakeblade` | 1.88 | 0.485 | 0.91 | 2.93 | 0.83 | 0.85 | 1.60 |
| `thundercleave` | 1.88 | 0.525 | 0.99 | 2.86 | 1.01 | 0.85 | 1.60 |
| `soul_shatter_blade` | 1.82 | 0.548 | 1.00 | 2.85 | 0.27 | 0.85 | 1.55 |
| `buster_sword` | 1.76 | 0.553 | 0.98 | 3.05 | 0.58 | 0.85 | 1.50 |
| `demon_dweller_sword` | 1.76 | 0.560 | 0.99 | 2.60 | 0.91 | 1.40 | 2.47 |
| `dream_glass` | 1.76 | 0.566 | 1.00 | 3.00 | 0.96 | 0.85 | 1.50 |
| `licht_dweller_sword` | 1.76 | 0.560 | 0.99 | 2.60 | 0.91 | 1.40 | 2.47 |
| `null_blade` | 1.76 | 0.566 | 1.00 | 3.17 | 0.93 | 0.85 | 1.50 |
| `club_of_rot` | 1.72 | 0.580 | 1.00 | 2.38 | 0.70 | 0.85 | 1.47 |
| `heart_of_the_flame` | 1.71 | 0.558 | 0.95 | 2.89 | 0.89 | 0.85 | 1.45 |
| `hunger` | 1.71 | 0.586 | 1.00 | 3.54 | 1.04 | 0.85 | 1.45 |
| `mountainguards_greataxe` | 1.71 | 0.586 | 1.00 | 3.51 | 0.52 | 0.85 | 1.45 |
| `wavebreaker` | 1.71 | 0.586 | 1.00 | 3.07 | 0.88 | 0.85 | 1.45 |
| `shadow_burn` | 1.65 | 0.588 | 0.97 | 3.09 | 0.65 | 0.85 | 1.40 |
| `stegosaurus_greatsword` | 1.65 | 0.607 | 1.00 | 3.12 | 0.88 | 0.85 | 1.40 |
| `rune_splitter` | 1.59 | 0.629 | 1.00 | 3.43 | 0.66 | 0.85 | 1.35 |
| `doom_hammer` | 1.55 | 0.394 | 0.61 | 1.86 | 0.87 | 0.85 | 1.31 |
| `astralaria` | 1.53 | 0.627 | 0.96 | 3.49 | 1.00 | 0.85 | 1.30 |
| `flamecutter` | 1.53 | 0.653 | 1.00 | 3.19 | 0.95 | 0.85 | 1.30 |
| `rimeheart_runeblade` | 1.53 | 0.653 | 1.00 | 2.75 | 0.96 | 1.36 | 2.08 |
| `time_bender_dadao` | 1.53 | 0.612 | 0.94 | 2.96 | 0.92 | 0.85 | 1.30 |
| `magebane_longsword` | 1.47 | 0.672 | 0.99 | 3.15 | 1.11 | 0.85 | 1.25 |
| `sorrows_edge` | 1.47 | 0.679 | 1.00 | 3.76 | 1.23 | 0.85 | 1.25 |
| `palladion` | 1.41 | 0.680 | 0.96 | 3.03 | 0.38 | 0.85 | 1.20 |
| `windcutter` | 1.41 | 0.674 | 0.95 | 2.97 | 0.59 | 0.85 | 1.20 |

### Running it

```
python tools/relics/display_presets.py            # dry run: the table above
python tools/relics/display_presets.py --apply    # rewrites the model jsons (only firstperson_* and one marker key) and the manifest
python tools/relics/display_presets.py --check    # exit 1 if --apply would change something (CI)
python tools/relics/test_display_presets.py       # the maths and the file rules
```

It is **idempotent**: the result is a function of the mesh and of the rotation already in the json (which it never changes); it ignores the scale and translation it wrote last time. A changed json carries `"nusmp_display_preset": {"length_blocks": ..., "scale": ..., "rule": ...}` (the game's model loader ignores unknown keys); when a rebuilt mesh is no longer long, the next run puts the vanilla handheld values back and removes the marker. A json with `"nusmp_display_preset": "manual"` is hand-tuned and never touched. **After a new weapon (or a rebuilt mesh) arrives, re-run `--apply`.** A model json that is not `builtin/entity` (a weapon still on its sprite model) is skipped and listed as `skip_not_3d`.

### The render-time guard

`WeaponRenderer` also guards the first-person contexts (`client/relics/item/OversizeGuard`): it reads the display scale out of the pose matrix and, if `L * scale` is more than 1.15 times `APPARENT_MAX` (a json the tool has not seen yet: 0.68 on a long mesh), shrinks the mesh about the grip point by `1.15 / apparent` (never below 0.4). A json the tool processed draws at most 1.0, so the guard does nothing for it. The guard does not solve the frustum; it only keeps an unprocessed weapon out of the camera until the tool is run.

## GeckoLib / GeoLite / Blender workflow

**Which loader for what.** Held weapons are baked meshes (above and `tools/relics/mesh_lib.py`). Everything that has bones and keyframes (bosses, props, worn relics, auras) is **GeoLite**: the repo's own loader in `client/geo` (`GeoModels`, `GeoModelData`, `GeoAnim`, `GeoDraw`, `GeoSpec`) reading **Blockbench / GeckoLib format** files, and `tools/geo_builder.py` writes them. GeckoLib itself is not a dependency; the files are the same, so a model exported from Blockbench's GeckoLib exporter loads (cubes only: no meshes, no locators, no Molang, linear interpolation).

The chain, step by step (the Curio section has the file-by-file conventions of the player frame; this is the weapon side):

1. **Blender.** Model the part with one object (or empty) per bone, origins on the pivots; 1 Blender unit = 16 px; the file frame is y up, feet (or the weapon's grip) at the origin, front on -z (`file.x = -Blender.x`, `file.y = Blender.z`, `file.z = Blender.y`). Box the parts: GeoLite draws cubes. For a weapon, put the **grip at the origin** and the long axis on +y.
2. **`.geo.json` (format 1.12.0).** `Model(key, name, tex_w, tex_h)`, one `m.bone(name, parent=, pivot=)` per hinge and one `m.cube(bone, origin=, size=, uv=, rotation=, pivot=)` per box. A hinge bone's pivot is **on the hinge line**; a chain's child pivot is at the end of its parent's cube.
3. **Texture + glow layer.** `assets/nusmp/textures/entity/<key>_<name>.png` (box-UV like a vanilla model) and optionally `<key>_<name>_glow.png`: drawn additive and full bright, tinted by the colour handed to `GeoDraw.paint`. Pixels that glow go on the glow PNG only (the body PNG keeps the dark material under them). In Blender bake the emission to a second image; in Blockbench paint it on a second texture.
4. **`.animation.json` (format 1.8.0).** Keyframes per bone: `rotation` (degrees, **added** to the bone's own), `position` (px, added), `scale` (multiplied); `Anim.clip(name, length, loop)` then `a.rot / a.pos / a.scale(clip, bone, {time: (x, y, z)})`. Sample the Blender action at the key times. Values are linearly interpolated, clamped before the first and after the last key; a clip that does not loop holds its last frame.
5. **Check.** `m.save(anim)`, `m.save_texture(img, glow)`, `python3 -B tools/geo_check.py <key>_<name>` (bones linked, uvs inside the sheet, animation bones exist).
6. **Draw.** `GeoDraw.paint(pose, buffers, GeoSpec.of(key, name), clip, seconds, Space.PROP, Layer.CUTOUT, light, argb, glowArgb)`.

### Weapon unfolding: the keyframe states

A folding weapon (a fan-blade, a collapsible staff, a tonfa that opens into a halberd) is one GeoLite model; the state the item is in picks the clip and the clock. The example is a fan of four blades `blade_1 .. blade_4` hinged at the grip (`hinge` bone at the pivot, each blade a child at the hinge, rotation about z in the file frame, degrees):

| Clip | Length | Loop | Keyframes (s) |
|---|---|---|---|
| `folded` | 1.0 | yes | all blades at 0 degrees, stacked; the grip bone breathes 0 -> 0.4 px -> 0; a glow pulse is the item's own |
| `unfold` | 0.8 (16 ticks) | once | 0.00 folded -> 0.12 blades anticipate **-6 degrees** back -> 0.45 `blade_1` +20, `blade_2` +60, `blade_3` +100, `blade_4` +140 (overshoot) -> 0.62 +18 / +54 / +90 / +126 (settle) -> 0.80 +20 / +60 / +100 / +140 held |
| `open` | 2.0 | yes | the open pose; a 0.6 degree sway of the whole fan; the glow brightens |
| `fold` | 0.6 (12 ticks) | once | the reverse of `unfold` without the overshoot; ends on the `folded` pose |

The clock is `now - start + partialTick` ticks over 20, started by the event that opened the weapon (the use of the act, the item component, a server cue like `WaveBossNet.Cue`); at the end of a once-clip the renderer switches to the loop clip. **Not yet shipped:** no weapon is a GeoLite model today and `GeoDraw` has `Space.PROP` and `Space.PLAYER` only. To draw one from the item renderer, translate the pose to the grip point of the placement (`place ox oy`) and draw with `Space.PROP` (one px is 1/16 block; scale the pose by the same `scale` factor the mesh would use). The inventory icon, the display transforms, the oversized rule and the engravings are the same as for a baked weapon.

## Bows and crossbows

The item classes (`relics/RelicBowItem extends BowItem`, `relics/RelicCrossbowItem extends CrossbowItem`; a wave def calls `.bow()` or `.crossbow()` on a MAIN_HAND relic, `RelicDef.RangedKind {NONE, BOW, CROSSBOW}`) keep vanilla's draw, load and shoot **unchanged**: the full draw is 20 ticks (`BowItem.getPowerForTime`), a crossbow charges for `CrossbowItem.getChargeDuration` (Quick Charge counts), arrows and bolts are found and used up as vanilla does (Infinity, creative), a loaded crossbow keeps its charged-projectiles component (`CrossbowItem.isCharged`, fireworks), the stack never breaks. What is added: the relic's acts go on **sneak + use** (the first act; the worn-relic key fires any act, as for every relic) because the use button means "draw"; a relic that needs attunement cannot be drawn until it is yours, and sneak + use holds the five-second ritual. A crouching archer whose act is on cooldown still draws.

**Properties.** `client/relics/item/RangedItemProperties` registers vanilla's `pull`, `pulling` (and for crossbows `charged`, `firework`) on every ranged relic with vanilla's formulas, from `ItemFxClient.init`. They drive any `overrides` in a model json, which is how the inventory icon can change (`WeaponRenderer` resolves the `_icon` model's overrides). The 3D hand model is `builtin/entity`, so no override can swap it: `ItemStateVariants` computes the same numbers (`RangedStage`) and `WeaponRenderer` picks the **mesh**.

**Stages** (`RangedStage`, tested): the holder is the local player in first person and the living entity being rendered in third person (`HolderTracker`, between `RenderLivingEvent.Pre` and `Post`); a stack on the ground or in a frame has no holder and draws at rest (a loaded crossbow still draws `_charged`).

| Stage | Mesh id | Bow | Crossbow |
|---|---|---|---|
| rest | `<id>` | not drawn | not loading, not loaded |
| 1 | `<id>_pull1` | drawing, `pull` 0 .. 0.65 | loading, `pull` 0 .. 0.58 |
| 2 | `<id>_pull2` | `pull` 0.65 .. 0.9 | `pull` 0.58 .. 1.0 |
| 3 | `<id>_pull3` | `pull` >= 0.9 | `pull` 1.0 (fully charged, about to load) |
| loaded | `<id>_charged` | (none) | loaded, whoever holds it |
| spent | `<id>_spent` | (none) | only the two self-loading crossbows, while their cooldown runs |

`pull` is ticks used over 20 (bow) or over the charge duration (crossbow), the thresholds are vanilla's own model thresholds. A mesh a weapon does not ship is skipped: pull3 falls back to pull2, pull1, then `<id>`, `_charged` and `_spent` to `<id>`. So a weapon may ship only the stages it has time for.

**Authoring.** Build the rest mesh with `mesh_lib`, giving the moving parts these names: `limb*` (the stave: flexes about the grip), `string*` (becomes a V pulled to the nock), `arrow*` (a bow's arrow, drawn only in the pull states) and `bolt*` (a crossbow's bolt, drawn only in `_charged`). Lay out and paint the atlas once, then:

```python
import ranged_meshes as rm
rig = rm.Rig(tip_a=(-6.3, -55, 0), tip_b=(-6.3, 55, 0),      # the string's anchors at rest, on the limb tips
             draw_dir=(-1, 0, 0), max_draw=48.0, flex=7.0,    # toward the archer; the nock travels 48 cm, each tip 7 cm
             grip=(0, 0, 0), limb_axis=(0, 1, 0), limb_span=55.0)
rm.emit_states(mesh, rig, "bow", "my_bow", atlas=atlas)      # writes my_bow.txt (no arrow), _pull1, _pull2, _pull3
```

`emit_states` moves vertices of the finished rest mesh and keeps every UV and normal, so all states share the rest mesh's skin and glow. The pulls are shown at 0.30 / 0.75 / 1.00 (bow) and 0.30 / 0.80 / 1.00 (crossbow, plus `_charged` = full draw and the bolt). Maths: limb tip travel `d * flex * f * u^2` (`u` = distance from the grip over `limb_span`); a string point at `t` along tip-to-tip moves by the tip travel plus `d * max_draw * f * (1 - |2t - 1|)` (the V); an arrow rides `d * max_draw * f`. `python tools/relics/ranged_meshes.py --selftest <dir>` builds a bow, emits and checks the states and renders a sheet. Previews: `python tools/relics/preview_mesh.py <path.txt>` (copy a state to `<id>.txt` first so the skin is found, `ranged_meshes.preview_states` does it).

**The two crossbows the mod already had.** The Bolt Thrower and the Crossbow of Swift Death are `MagicWeaponItem` kinds that load themselves (a right-click looses a bolt, then the item cooldown runs): there is no draw. Their old meshes (kept, unchanged) already show the limbs drawn, the string at the latch and the bolt on the rail. `python tools/relics/crossbow_states.py` (the worked example on a baked mesh) adds `<id>_charged` (ready: the same mesh with a faster, brighter glow pulse) and `<id>_spent` (just fired: the bolt removed, the limbs relaxed forward by their own back-sweep, the string rebuilt as a straight strip between the relaxed tips). `ItemStateVariants` shows `_charged` while the weapon is ready and `_spent` while `Player.getCooldowns()` runs (other players' cooldowns are not known to a client: they always look ready).

## NBT state layers

Two variants can be laid over any mod weapon that `WeaponRenderer` draws (a read-only decision per draw, cached until the next resource reload; no stack or entity is ever changed, and nothing that is not a mod item reaches the renderer). `client/relics/item/VariantRules` is the pure decision table (tested), `ItemStateVariants` feeds it.

| Variant | When | Mesh | If the weapon ships no such mesh |
|---|---|---|---|
| **Genesis** | the stack's relic grade is `GENESIS` (any context), **or** the holder's body is Genesis (a hand context) | `<id>_genesis` | the same mesh, glow pulse **1.6x faster and 1.6x wider** (clamped 0..1.5), plus an extra `RenderType.entityTranslucentEmissive(glow texture)` pass tinted with the glow colour at alpha 60..160 |
| **Five leaf** | the holder owns a grimoire of **5 leaves or more** and the weapon is a Black Clover one (`MagicWeaponItem`: the Demon-Slayer swords and the rest) | `<id>_5leaf` | the glow tinted **violet-black** (65% toward `0x5A1E9E`, then x0.8) and the body multiplied by `0xFFB8A2D8` |

They are independent and may both apply. With a draw stage the mesh order is, for each id of the stage chain, `<id>_genesis`, `<id>_5leaf`, `<id>`: a drawn bow keeps its `pull2` mesh when only the rest pose has a genesis mesh.

**Where the flags come from.** `TensuraGenesisBody` is a flag in the player's persistent data (`getPersistentData()`, at the top or inside `PlayerPersisted`); persistent data never reaches a client, so `relics/RelicHolderSync` reads it (and the leaves of the player's grimoire: `GrimoirePages.coverOf(...).tier`, 3 common, 4, 5 and above forbidden covers) once a second per player, and sends a packed int (`HolderFlags`) to everyone who can see the player when it changes and to whoever starts tracking them. Nothing is written to the player. A client that has not received a flag treats it as false.

To add a third variant, extend `VariantRules.Decision` and `candidates` (the order is the priority), add its row to `VariantRulesTest`, and add the case in `WeaponRenderer` next to `state.intensify()` / `state.tint()`.
<!-- m5 end -->

<!-- dv begin -->
## Djinn armour: the unique and the Dark Djinn looks

The Djinn's Half Equip and Full Equip are a render layer (`client/relics/djinn/DjinnArmorLayer`), never an item. Each of the twelve Djinn has three mesh sets under `assets/nusmp/djinn/`:

| Set | Folder | Made by | Used |
|---|---|---|---|
| shared rig (the first look, **kept as the fallback**) | `<id>/` | `tools/relics/gen_djinn_art.py` + `djinn_parts.py` | when a Djinn has no set of its own |
| unique look | `u_<id>/` | `tools/relics/gen_djinn_unique.py` | by default |
| Dark Djinn | `d_<id>/` | the same script, variant `d` | while the synced flag bit 3 (value 8) is set |

A Djinn is drawn from **one** set (`DjinnMeshes.setOf`: the first of dark -> unique -> shared whose `chest.txt` exists), so a piece a set leaves out on purpose (Baal has no halo, Sitri no tail) is never filled in from an older set. Skins: `textures/entity/djinn/<id>_u_armor(.png|_glow.png)`, `<id>_d_armor(...)`, body-art `<id>_u_rune.png` / `<id>_d_rune.png` (additive), the dark veins `<id>_d_art.png` (alpha blended over the skin) and the dark bust `<id>_d_bust.png`. The 16x16 Seal icons are unchanged.

**Run it.** `python3 tools/relics/gen_djinn_unique.py` (all twelve, both variants, about half a minute), `--djinn baal --variant d` for one, `--only art|skins`. `--preview OUT_DIR --refs DIR_WITH_59.png_60.png_61.png_62.png` writes one contact sheet per Djinn: reference crop, Half Equip, Full Equip (3/4 and back), the weapon, the dark reference, Dark Full, Dark Half. Everything is deterministic and idempotent; the committed meshes and PNGs are the output.

**Modules.** `djinn_unique.py` (kit, helpers, materials: the unique palette per Djinn and the dark palette: black plate and cloth, gold trim, the Djinn's colour kept in the crystals), `djinn_unique_heads.py` (helm, horns, ears, crest, the dark eye crest), `djinn_unique_back.py` (wings, tails, halos, back pieces, mantles, the dark halo of thorns), `djinn_unique_arms.py` (chest and emblem, skirt, pauldrons, forearm and greave adornments, the twelve weapons), `djinn_dark.py` (black rukh feathers on the wings, the unique and dark body-art patterns, the black veins, the dark bust).

**Pieces.** The old 23 plus four appended: `weapon` (the Half Equip weapon, authored in the held item's own space: the grip at the origin, the blade along +y, **not** flipped into model space), `eye` (the Dark Djinn's brow crest), `backdrop` (a piece standing behind the back: a ring, an arch, a pair of extra arms) and `mantle`. `DjinnMeshes.Piece` only ever gets entries appended.

**Budget.** At most 5,200 quad lines per set in a Full Equip (the tail counts five times, the layer draws it five times); the shipped sets are 2,900 to 4,700. Parsed once into float arrays, no allocation per frame, nothing drawn beyond 48 blocks, the small details dropped beyond 24. `DjinnUniqueAssetsTest` checks the format, the budget, that no two Djinn share a silhouette piece, that the weapon is in item space and that the old rig is still there.

**Per-Djinn motion and particles** are the pure tables in `client/relics/djinn/DjinnLook` (wing beat, tail length, halo and back-piece spin, weapon size; the ambient particles: sparks off Baal, embers from Amon, bubbles from Vinea, feathers from Focalor, petals from Zagan, shards from Zepar, notes from Furfur, a trail of embers from Marchosias, motes from Sitri, violet sparks from Bifron, white feathers from Paimon, ink and drops from Vassago; a Dark Djinn adds black rukh streaming off its wings). Spawned client side by `DjinnAmbient`, once a tick per player within 32 blocks.
<!-- dv end -->
