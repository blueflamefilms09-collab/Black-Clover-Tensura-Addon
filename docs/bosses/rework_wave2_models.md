# Boss rework, wave 2: models made in Blender

The owner asked to "rework the bosses: models, abilities, and VFX". Wave 1 (3.96.0, [`rework_wave1.md`](rework_wave1.md)) did the abilities and the VFX and noted that the mod
had no way to draw a Blender mesh on an entity. This wave adds that path and the first two boss bodies made in Blender (rule 14): the **Goliath** and the **Minotaur**.

Settings (CLIENT, `nusmp-playermodel-client.toml`, section `playermodel`; each restores the look before this wave, rule 12; read every frame, no restart):

| Setting | Default | Off means |
|---|---|---|
| `goliathBlenderModel` | `true` | the 3.96.0 stone geo model (`goliath_body`, its moss, cracks and clips), exactly as before. Only applies while `goliathStoneModel` (wave 1) is on. |
| `minotaurBlenderModel` | `true` | the art kit's geo model (`arc_minotaur`), exactly as before. Asterius (the second form) keeps his geo model either way. |

No new config file (the two keys are appended to the existing `PlayerModelOptions` spec, so `ConfigFileNamesTest` is unaffected). No server change, no packet, no self-test (nothing new runs on the server).

## Previews (rendered in Blender, Cycles, from the baked skins)

* Goliath: `docs/bosses/previews/goliath_blender.png` (three-quarter), `goliath_blender_side.png`, `goliath_blender_slam.png` (the slam pose, turned with the same part turns the game uses).
* Minotaur: `docs/bosses/previews/minotaur_blender.png`, `minotaur_blender_side.png`, `minotaur_blender_slam.png`.

## What was made in Blender

| Boss | Script | Parts | Quads (body + glow) | LOD copy | Skin |
|---|---|---|---|---|---|
| Goliath | `tools/blender/boss_goliath.py` | torso, head, arm_l/r, forearm_l/r, leg_l/r | 2416 + 330 = 2746 | 1152 | 1024 px + glow |
| Minotaur | `tools/blender/boss_minotaur.py` | the same eight (the great axe is part of forearm_r) | 2574 + 61 = 2635 | 1169 | 1024 px + glow |

Both are modelled with Blender's own tools (bmesh lofts of irregular rock / muscle sections, the Subdivision Surface, Displace (Clouds texture), Solidify, Bevel
and Decimate modifiers), unwrapped with Smart UV Project into one skin, and baked with Cycles (the lit surface with its procedural bump and colour into the skin,
the emission into the glow skin) through `tools/blender/bl_lib.py` (`finish`), exactly as the hero weapons are. Nothing is hand-placed from code boxes.

* **Goliath** (DanMachi, the floor boss of the 17th floor): a hunched dark-grey stone giant, 6.65 blocks tall (about 7 m): short thick legs with knee boulders and toes,
  a massive chest with pectoral slabs and a belly stone, shoulder boulders, rock spikes on the back, long arms with huge knuckled fists that hang to the knees, a small
  head under a heavy brow, **red glowing eyes**, and molten veins (Voronoi edges masked by a slow noise) on parts of the chest, arms and thighs that feed the glow pass.
* **Minotaur** (DanMachi, the Level 2 bull): a dark-brown hide over heavy muscle, a hump of dark fur, a bull's head with a long muzzle, the gold nose ring and great curved
  horns (docs/arc/minotaur.md: the ring, the wrist wraps and the glowing eyes are the mod's additions), a leather loincloth and belt, cloven hooves with shin fur, and in the
  right fist a **double-bitted great axe** of dungeon stone on a thick wrapped haft (the owner's "great axe/club"; the series gives it a natural weapon).

## The render path (client only)

* `mesh/EntityMeshData.java` (pure, no Minecraft class): reads `assets/anime_multiverse/entity_meshes/<id>.txt`. The quads are the weapon meshes' `qn` lines (same packer
  and tools); the header adds `entity <id> <scale>`, `glow <rgb> <speed> <min> <max>`, `pivot <part> <parent|-> x y z` (a parent before its children) and `part <name>`
  before each part's quads. More than 3000 quads, a part without a pivot or a child before its parent is refused (the caller then draws its old model).
* `client/bossmesh/BossMeshDraw.java`: one per entity renderer (a resource reload makes the renderers again, so the mesh is read again then, once, on its first frame).
  Each frame it works out one matrix per part (the parent's, turned about the part's pivot: X, then Y, then Z, the order Blender's previews use) into matrices made at
  load, draws all body quads in one cutout batch (translucent while the body fades out in death) and the glow quads in one additive batch (`SafeAdditive`).
* `client/bossmesh/MeshPose.java` (pure, unit tested): the procedural animation. Breathing at rest; a walk paced by the ground covered (legs and arms swing against
  each other, the torso rolls and bobs; the amount follows the smoothed speed, so starting and stopping never jumps); and one curve per kind of blow, played over the
  progress of the clip the boss's renderer already picks (the wind-up ends at 0.8 where the blow lands, the rest is the settling): `attack` = SWING (a backhand),
  `slam` / `cast` / `cast_big` = SLAM (both fists high, then down), `stomp` = STOMP, `roar` / `transform` = ROAR (arms wide, head back), `fist` / `line` = PUNCH,
  `rubble` = HURL, the Goliath's rise = RISE (fists first through the floor), `hurt` = FLINCH, death = DEATH (sags and tips forward). Every curve starts and ends at rest.
* `GoliathRenderer` (wave 1's stone renderer) and `ArcBossRenderer` (the Minotaur) keep all their state (rise sink, facing, size, glow level, death fade); only the paint
  call is replaced by the Blender body while the setting is on and the mesh loaded. The Gates' cracked form of the Goliath turns the glow amber and bright instead of
  swapping the skin; the transformation's flash brightens the glow.

## Cost per entity (rule 11)

* Client, per Blender boss in view per frame: the Goliath 2416 body quads + 330 glow quads within 48 blocks; beyond 40 blocks the Decimate copy (1152 quads, no glow pass).
  The Minotaur 2574 + 61 near, 1169 far.
  Hard cap 3000 quads per mesh (`EntityMeshData.MAX_QUADS`, refused at load). Two draw batches (cutout, additive). 8 part matrices per frame, no allocation per frame or
  per vertex (`PerfSourceScanTest.theBossMeshDrawAllocatesNothingPerFrame`). The 3.96.0 geo Goliath was 1512 vertices (378 quads) plus its glow; the art-kit Minotaur a
  few hundred cubes: the Blender bodies cost more per frame near the camera and about the same beyond 40 blocks. Bosses are few at a time (one floor boss per floor).
* Memory: the mesh is read once per resource reload per renderer (the Goliath: 2746 + 1152 quads, about 125 KB of floats; the Minotaur about the same). The Minotaur's
  renderer is the shared `ArcBossRenderer`, so one copy per arc-boss renderer that has the Minotaur's look (one).
* Jar: `entity_meshes` is added to the packed mesh folders (`build.gradle` `meshFolders`, `MeshPackTest.PACKED_FOLDERS`): the four files go from 1.8 MB of text to
  about 190 KB packed (round trip checked); the skins are palette PNGs (`tools/shrink_pngs.py`), about 0.85 MB together.
* Server and network: none (no packet, no tick, no entity, no state).

## NEW / REPLACED / KEPT

* NEW: the entity mesh path (`EntityMeshData`, `BossMeshDraw`, `MeshPose`), the Blender entity pipeline (`tools/blender/bl_entity.py`: part-tagged export, LOD by Decimate,
  previews rendered in Blender with the game's pose maths), the Goliath and Minotaur bodies (`entity_meshes/goliath*.txt`, `minotaur*.txt`, `textures/entity/boss_mesh/`),
  the two client settings, the previews, `BossMeshWave2Test`, one `PerfSourceScanTest` pin.
* REPLACED (the default look, behind a setting): the Goliath's 3.96.0 stone geo model and the Minotaur's art-kit model are no longer drawn by default.
* KEPT: every old model, skin, clip and renderer (the geo files still pace the Blender bodies' blows: their clip lengths), the 1.99.0 Goliath routing behind
  `goliathStoneModel`, Asterius's model, every move, number, effect and the server side. Time Magic VFX untouched.

## How to add the next boss body

1. Write `tools/blender/boss_<id>.py` like `boss_goliath.py`: `be.seed(n)`, materials with `bl.mat` (or your own node trees), the pieces with `be.body_loft`, `be.limb`,
   `be.boulder`, `be.shard`, `bl.tube`, `bl.gem`, ... each with `part=` one of the role names of `MeshPose.ROLE_NAMES` (a part with another name is drawn but never moves),
   a `RIG = {part: (parent or None, pivot)}` in model units (1/16 block, y up, face +z, its left +x, feet on y = 0), and `be.finish_entity(ID, RIG, ...)`.
2. Look first: `bvenv/bin/python -B tools/blender/boss_<id>.py --trial DIR --quick` (mesh, skins and the three previews in DIR), then without `--trial` to write the assets.
   Keep body + glow under 2800 quads (the script stops over the budget).
3. `python3 -B tools/shrink_pngs.py src/main/resources/assets/anime_multiverse/textures/entity/boss_mesh`.
4. In the boss's renderer: a `BossMeshDraw` field, and in `render`, behind a CLIENT setting, `pose.solve(...)` from its clip and `draw(...)` in place of the old paint call
   (see `GoliathRenderer` / `ArcBossRenderer`). Add the asset to `BossMeshWave2Test` (or a new test like it).

## Caveats (not seen in a game)

* Nothing of this wave was seen in a running game (no client here): the bodies are checked by the Blender renders, the parser test and the source scans only. Things to
  look at first in a game: the scale and the feet on the floor (the models are at their true size: the Goliath 6.65 blocks, the hitbox 6.2), the facing (+z), the walk
  pacing, the lighting of the baked skins under the game's light (the bake has its own studio light, as the weapons), and the glow strength.
* The procedural animation is simple by design (no imported skeletal clips): the legs do not bend at the knee (a leg is one part) and the hands do not open.
* The Gates' cracked Goliath is an amber glow, not a second skin; Asterius keeps his art-kit model.
* Previews: rendered in Blender with a plain sun and grey world, not with the game's lighting.
* Wiki research (rule 10): no wiki was fetched in this wave; the looks follow `docs/arc/minotaur.md` (its sources could not be opened either) and the owner's brief.
