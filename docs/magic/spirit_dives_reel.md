# Spirit Dives, REEL look: the V2 design executed like the showcase videos (the default look since 3.3)

The owner liked the design of the V2 Wind dive (crown of crystal blocks with a gold ring halo, hex-membrane wing, gold rings on the limbs) and asked: "try making it like the videos". The videos
(a Black Clover Minecraft mod, Yuno / Wind Spirit Dive and Noelle / Water Valkyrie dress) were read frame by frame (`docs/previews/spirit_dive_reel/references/`: yuno_B03 ribbons, B04 flash, B07 cage of circles,
B09 crown and sword, B10 final form; noelle_A03 mosaic body, A09 halo and bloom, A10 final form). The new client look value `REEL` keeps the V2 pieces as the design language and rebuilds the look, the entry
and the motion the way the videos do. Everything is client drawing: no server code, no packet, no gameplay change, no camera change.

## 1. What was built

| Part | What it is (video reference) | Where |
|---|---|---|
| Entry, 22 ticks | white flash at the chest (B04), 6-10 thin white-gold helical ribbons spiralling UP the body (B03), a sphere of 3-5 crossing circles closing round the body (B07), a ring running out over the ground, the body assembling from the feet to the head, a second burst when the form appears, crown cubes igniting one after the other, wings unfolding with an overshoot, weapon drawn out of its circle. The exit is the same clock backwards over 20 ticks. | `SpiritDiveReelRules` (the one clock), `SpiritDiveReel` |
| Body | the wearer's own skin and clothes stay; a mosaic of glowing block patches in 2-3 shades (5 growing levels per element) switched on feet to head, a bright rim hull and a soft hull (the ANIME shell passes are reused). Noelle's body is a solid blue armour (A03, A10), the other tiers wear lighter patches | `SpiritDiveReel.shell`, `sdr_mosaic_<el>_1..5` |
| Gauntlets | glowing cube forearm with a white inner light, a spiral band, a glow copy and a fist cube past the hand, in the arm's own frame (B09, B10, A10) | `SpiritDiveReelPieces.gauntlet`, `sdr_cube_<el>` |
| Crown | V2 crystal-block crown, now a 12-cube ring with vertical bars and a white light on top of each (B09), the V2 gold ring halo under it; Noelle adds the thin tilted halo ring with a bloom (A09, A10) | `SpiritDiveReelPieces.crown/halo`, `sdr_bar_<el>`, `sdr_halo_ring` |
| Wings | Wind: the V2 hex-membrane panel shaped as the video's leaf, up and back (2 leaves a side); Water: a comb of 9 (Noelle 15) thin overlapping feather blades a side (A10); Fire: flame-feather blades; Earth: crystal shard blades | `SpiritDiveReelPieces.wings`, `sdr_wing_<el>`, `sdr_wing_glow_<el>` |
| Gold bands, orbit rings, ground ring, drifting fragments | gold rings on the upper arms and thighs (V2), 2-3 thin white rings orbiting at hip and shoulder height, a soft ring on the ground, 5-8 sparks | `SpiritDiveReel.extras` |
| Character weapons | Yuno: the huge spirit sword with a turning magic circle at its hilt, hovering at his side (B09, B10), a flat strip, not an item, no keybind. Noelle: a spiky ball of 2 discs x 10 water blades with a bright core, and a crystal water spire. Fuegoleon: a flame blade with a circle; his flame arm is not redrawn (`FuegoleonArmAura.drawsArm`) | `SpiritDiveReelPieces.sword/ball/spire` |
| Stages | the first dive (HALF) keeps the ANIME stage logic: Wind dresses the left half first (left gauntlet, one leaf, half a crown); the other elements dress the arms and add one short fan; UPGRADE keeps what is on and forges the rest | `SpiritDiveReelRules.sides/arms/crowned` |

Four elements, standard tier and the three characters (Yuno, Fuegoleon, Noelle), the same grammar. Hover lift for Noelle: NOT built (see section 8). Camera push-in: NOT built.

## 2. Faults of the ANIME look against the videos (what was changed)

1. ANIME bathed the body in a flowing shell and plume sheets: no blocks. The videos are blocky: a crown of cubes, cube gauntlets, a block mosaic. REEL draws those.
2. ANIME had no ribbon tornado, no flash that floods, no cage of circles: its entry was a pillar and a swirl. REEL's entry is the video's: flash, helical ribbons, circles, ring pulse, second burst.
3. ANIME wings were plume sheets of one big shape; the video's wing is a leaf (Yuno) or a comb of parallel feathers (Noelle). REEL draws leaf and comb.
4. ANIME gave the characters no weapon; the video's final frames are about the sword with the circle (Yuno) and the spiky ball (Noelle). REEL adds them.
5. ANIME changed the hair and eyes; the video keeps the clothes visible. REEL leaves skin, clothes and hair alone.
6. Found while tuning against the frames: the first comb was a spray of needles (blades narrower than the gap between them), the leaves and the blades were about twice the length of the video's wing, the crown bars were three times the cube height (B09: about 1.5x), and the flash bloom cut a hard line where it met the ground. Blade width now covers the gap (neighbours overlap), lengths 17-24 px, bars 2.2-3.4 px, the bloom is centred higher and smaller.

## 3. The look option and its default

`spiritDive.look` (CLIENT config `nusmp-spiritdive-client.toml`): `DJINN | V2 | CLASSIC | ANIME | REEL`, `REEL` appended last and the default (`SpiritDiveDjinnPieces.DEFAULT_LOOK`, `DiveClientConfig`, `SpiritDiveLook`). Every other look is untouched and still chosen by the option. `classicLook = true` still wins.
The camera option `spiritDive.cameraPush` was NOT added (see section 8).

## 4. Files

- common: `book/SpiritDiveReelRules.java` (clock, timeline, specs, vertex counting, palette, texture paths, option parse), `book/SpiritDiveReelGeometry.java` (crown, leaf, comb, helix, orbit, cage, weapons, sparks as float-array arithmetic), `book/SpiritDiveReelSelfTests.java` (self-test `magic.spirit_dive_reel`, registered in `SpiritDiveBoot`).
- client: `client/aura/SpiritDiveReel.java` (entry point, shell, ribbons, extras, cage), `client/aura/SpiritDiveReelPieces.java` (gauntlets, crown, halo, wings, sword, ball, spire), dispatch in `SpiritDiveAura`, flag `SpiritDiveLook.reel()`, first person arms `DiveAnimeFirstPerson` (also for REEL).
- appended to shared files: enum `Look.REEL`, ribbon `Shape.BLADE` (`SpiritDiveAnimeGeometry`), ANIME self-test updated (the default moved on).
- assets: 52 textures `assets/nusmp/textures/aura/sdr_*.png` written by `tools/gen_spirit_dive_reel_textures.py`; the ANIME `sda_rim` and `sda_soft` hulls are shared.
- tools: `tools/spirit_dive_reel_scenes.py` (43 preview scenes), `tools/spirit_dive_reel_previews.py` (the sheets), `tools/render_preview/examples/SdrReel.java`.

## 5. NEW / REPLACED / KEPT (rules 2 and 5)

- NEW: the whole REEL look (it covers things ANIME and V2 also have: the Spirit Dive look, so it is a new look value next to them, not a replacement), its textures, tests, tools, doc, previews.
- REPLACED: only the DEFAULT of the look option (ANIME to REEL). Nothing was removed.
- KEPT: DJINN, V2, CLASSIC and ANIME looks, their painters, textures, the server entry, tiers, style bits, FuegoleonArmAura, all packets. ANIME tests that asserted "ANIME is the default" now assert REEL is (the old look stays choosable and is still asserted).

## 6. How it is read in a game

No shader and no new asset format: opaque or translucent textured quads with full-bright light, plus additive glow copies (`SafeAdditive`). Strips (ribbons, leaves, blades) turn their face to the viewer and share one scratch spine. Nothing is drawn for the local player's head in first person.

## 7. Budgets and worst case (rules 9 and 11)

Vertices per diver at full detail (counted by `SpiritDiveReelRules.vertices`, equal to the preview harness count at the entry peak, 2932 for Noelle at tick 14):

| | worst full dive | cap | ANIME worst | V2 worst |
|---|---|---|---|---|
| standard tier | 2168 (Water) | 2400 | - | 7700 |
| character tier | 2932 (Water, Noelle) | 3200 | 5384 | 7700 |

MID (24-48 blocks): at most 1064 (no cage, no weapon, 4 ribbons, short fan); FAR (48-64): at most 128 (the plain shell and the ground ring); beyond 64: 0. At most `SpiritDiveTier.CHARACTER_PER_FRAME` = 3 character dives a frame (`SpiritDiveFrame.admitCharacter`), the rest standard: worst frame 3 x 2932 + the standard ones. Entry sprites (flash 3 sprites, ground pulse, cage 5 rings, ribbons 10 x 12 segments x 2 passes) exist only for 22 ticks and only at FULL detail; the exit plays them again for 20.
Per frame: no allocation (statics for scratch arrays, cached textures and render types), no string, no ResourceLocation. Timed in `/multiverse perf` as `c.spiritdive.reel` (`PerfProbe`).
Network: none (no new packet, the style number is the one the server already sends). Server work: none. Per-player state: none (everything is drawn from the aura context of one player). Client config only (looks), no gameplay setting.

## 8. What could not be verified / not included

- Nothing was seen in a running game. The previews are a software rasteriser (translucent and additive passes approximated, no bloom, no post effects, the default Steve skin): colours and glow strength in the game will differ.
- The video's grey hair and violet eyes of Noelle are skin changes; REEL keeps the wearer's own skin by design.
- Noelle's hover lift (valkyrie rises) is NOT built: it would shift the drawn model, and the shell, the crown and the first-person arms all assume the real position; it can be added as a visual-only offset when the owner asks.
- Camera push-in on the face (the videos' first second) is NOT built: no camera code was added, `spiritDive.cameraPush` does not exist; `SpiritDiveReelRules.CAMERA_PUSH` is false and a test pins it.
- Fuegoleon's and Earth's look has no video. They follow the same grammar with flame feathers and crystal shards; the owner's Fuegoleon art was used for the colours only.
- The self-test `magic.spirit_dive_reel` is added to the smoke list by the coordinator.

## 9. Tests

`SpiritDiveReelTest` (look option, tiers, clock, entry effects, budget, geometry, palette), `SpiritDiveReelAssetTest` (files, sizes, clean under alpha 0, mosaic levels, wings, generator), `SpiritDiveReelScanTest` (server safety, allocation, entry point uses LOD / cap / probe / spec, old looks, registration); the ANIME and Djinn tests updated for the new default. Also run: PerfSourceScanTest, ServerSafetyTest, ConfigFileNamesTest, SafeAdditiveSourceScanTest, TextureAlphaTest, SafeAdditiveTextureScanTest. Self-test `magic.spirit_dive_reel` (files, tables, budgets, style round trip, option values).

## 10. Previews (`docs/previews/spirit_dive_reel/`)

`wind.png`, `water.png`, `fire.png`, `earth.png` (video crops, then standard and character tiers: front, 3/4, back, side), `close_*.png` (head and chest), `entry_<element>_<tier>.png` (tick 0-24 and the exit, next to the video's ribbons, cage, flash), `half_dives.png` (first dives and an upgrade). Regenerate: `python3 -B tools/spirit_dive_reel_scenes.py; RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py <scene names>; RENDER_PREVIEW_BUILD=<dir> python3 -B tools/spirit_dive_reel_previews.py`.

## 11. Sources

Frames of the two videos given by the owner (`owner_v2_look.jpg`, `owner_tiktok_yuno.jpg` and the frame sheets); no wiki lookup was needed for this look.
