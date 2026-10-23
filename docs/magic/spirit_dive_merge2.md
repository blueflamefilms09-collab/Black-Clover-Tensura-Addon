# Spirit Dive MERGE 2: slim spirits, a calmer entry, the merge in first person, smaller and more distinct crowns

> The owner answered yes to four improvements of the merge layer of `docs/magic/spirit_dive_merge.md` and added "the Spirit Dive Crown needs to be smaller and more distinct".
> Four client options, all default **on**, each restores the previous behaviour when it is switched off: `spiritDive.mergeModels` (SLIM | BOXY), `spiritDive.calmEntry`, `spiritDive.mergeFirstPerson`,
> `spiritDive.crownDistinct`. `spiritDive.merge` keeps working (false = no merge at all, so none of the first three shows).

Nothing here was seen in a game. Every picture in `docs/previews/spirit_dive_merge2/` is the project's software harness (`tools/render_preview`, the real painter classes against Minecraft stubs): no bloom, approximate additive
blending. Section 8 lists what is not verified.

## 1. NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| **NEW** | the slim spirit meshes (`assets/nusmp/sdm2_meshes/{wind,water,fire}_{char,std,core}.txt`, `earth_{std,core}.txt`, 11 files, `tools/gen_sdm2_models.py`); the small element crowns (`sdm2_meshes/crown_*.txt`, 9 files, `tools/gen_sdm2_crowns.py`); the first person merge (`SpiritDiveMergeFirstPerson` draws, `DiveMergeFirstPerson` is the event hook); the pure rules `SpiritDiveMerge2Rules`; the client option holder and crown painter `SpiritDiveMerge2`; the options `mergeModels`, `calmEntry`, `mergeFirstPerson`, `crownDistinct`; the self-test `magic.spirit_dive_merge2`; 3 tests; the preview scenes, sheets and harness helpers |
| **REPLACED** (the new thing is the default, the old one is one option away) | the boxy merge spirits (now slim; BOXY brings them back); the loud look entry during the merge window (calm; `calmEntry=false` brings it back); the Wind A 3D crown, the REEL cube crown (every element of the REEL look) (the new element crowns; `crownDistinct=false` brings them back); the size of the B / C / PACK / ANIME crowns (about 62 percent; same option) |
| **KEPT** | the old merge meshes `sdm_meshes/*` and their loader, the old crown meshes `sdw4_meshes/*` and `SpiritDiveWind4Rules`, the cube crown code of the REEL look, the sprite crowns, every look, the first merge's rules, budgets and self-test. Shared files only **appended to or hooked in one place**: `SpiritDiveMerge` (mesh choice and a shared draw method), `SpiritDiveReel` (entry and ribbons), `SpiritDiveReelPieces` / `SpiritDiveWind4` (crown hook at the top), `SpiritDiveWind` / `SpiritDiveWind3` (one `fit(pose)` line each), `SpiritDiveAnime` (the crown size), `DiveClientConfig` (4 options), `DiveClient` (one listener), `SpiritDiveBoot` (one line), `tools/render_preview/preview.py` (an optional scene key `bg` = dark / bright) |

## 2. Slim spirit models (`tools/gen_sdm2_models.py`, `assets/nusmp/sdm2_meshes`)

Hand authored low-poly SMOOTH meshes (a tube along a spline with a transported frame for limbs, bodies and tails; ellipsoids for heads; leaf-shaped lofted sheets for wings, fluke and hair ribbons), not cubes.
The same quad line format as the other meshes (`SpiritDiveWind3Mesh`, loaded with `getIn("sdm2_meshes", name)`), two layers: **0 the solid body**, **1 the soft glow shell** (wing margins, the fluke, the flame, the crystals),
both drawn additive (the solid at 92 percent, the shell at 42 percent of the ghost's alpha). Mesh space and normalisation are those of the old meshes (32 px on the longest side), so every size, path and tuning of the first merge still applies.

| Spirit | Character tier (canon pose) | Standard tier | Inner silhouette |
|---|---|---|---|
| **Sylph** | slender fairy, spread arms, two big leaf / dragonfly wings (upper and lower pair, spread), four flowing hair ribbons: 888 vertices | the same figure, wings folded up behind the back, two hair ribbons: 488 | head, torso, skirt, folded wings: 144 |
| **Salamander** | coiled on the ground with the tail curled in, the neck raised in an S, head looking ahead, small wings, flame on the tail tip: 672 | four legged, long curved neck and tail, small wings, horns, flame: 484 | 112 |
| **Undine** | the mermaid: S curved tail ending in a scalloped fluke, one arm raised, hair ribbons, ear and hip fins, ribbons from the wrists: 864 | the same with fewer ribbons: 492 | 224 |
| **Gnome** | none (Earth has no character tier) | round body, rock cap with a crystal, beard, rock shoulder pads, three crystals on the back, a gem on the chest: 480 | 96 |

Budgets: **at most 900 vertices a ghost at the character tier, 500 at the standard tier, 300 for the inner silhouette** (checked by the generator, `SpiritDiveMerge2Rules`, the tests and the self-test). The old boxy
ghosts were 528 to 696 vertices, so a slim character ghost costs up to 200 more vertices (888 against 528 for the Sylph) and a standard one about the same. Still drawn **only in the 22 tick entry / exit and only
within 32 blocks**, the silhouette only at full level of detail. The worst merge of one diver is now: ghost 900 + strips 120 + sprites 12 + cover 144 + silhouette 224 at most = **1,400** (the first merge: 1,260);
per frame at most 3 merges as before (worst 3 x 1,400 = 4,200 vertices). The steady state is the silhouette alone (at most 224), full detail only.

Previews: `turntables.png` (eight views of every spirit and tier, a software rasteriser), `merge_<element>.png` (the merge alone, slim above boxy, per tier).

## 3. Calmer entry (`calmEntry`)

The entry of the REEL look and of every Wind design (A, B, C, PACK, REEL share `SpiritDiveReel.entry` and `SpiritDiveReel.ribbons`) put a white flash sphere (up to a radius of 2.3 blocks at clock 13.5, "the second burst
when the final form appears"), spiral ribbons and a cage of crossing circles over the whole merge window. With the option on (`SpiritDiveMerge2Rules`):

| Clock | Flash alpha / size | Ribbons and cage |
|---|---|---|
| 0..4 | as before (full) | as before (full, up to clock 6) |
| 4..8 | falls to 30 percent alpha, 35 percent size | falls to 30 percent |
| **8..17 (the merge window)** | **at most 30 percent alpha, 35 percent size** (the old 13.5 bell scaled to 30 percent) | **30 percent** (they fade out by themselves from 13) |
| 17..19.8 | a short **final burst** after the contact (peak 90 percent alpha, 90 percent size at 18.4) | still 30 percent, fading |

The ribbons are only dimmed (they are drawn around the body, the ghost travels on a circle of 27 to 34 px round it); the cage of circles is dimmed with them (not asked, but it covered the ghost the same way).
**Vertices:** the same or fewer (a sprite below alpha 0.01 is not drawn; nothing is added): flash sprites 3 x 4, ribbons unchanged, cage unchanged. The numbers are pure rules with a test
(`SpiritDiveMerge2Test.theEntryIsAThirdInTheMergeWindowAndFullAtTheStart`, `aShortBurstFollowsTheContact`).
Other looks (ANIME, DJINN, V2, CLASSIC) have their own entries and are not changed. Previews: `entry_<element>.png` (merge 2 above, everything switched back below).

## 4. First person merge (`mergeFirstPerson`)

In first person the player model is not drawn, so the merge of the first version showed nothing to the wearer. Now `DiveMergeFirstPerson` (a level render stage listener, `AFTER_PARTICLES`, registered once in
`DiveClient`) draws the same ghost through `SpiritDiveMergeFirstPerson.draw` in the VIEW's own space (the pose is the camera rotation; the frame is built like the Paladin overlay's):

* the spirit appears at the **upper side** of the view (a little ahead of the eyes, side by the UUID hash as in third person), fades in, then **flows down and in along a curve, shrinking**, and leaves the screen over
  its **bottom edge** on the way into the chest; it faces its way;
* a **soft flash** (one sprite, at most 55 percent alpha, only its top edge reaches the screen) at the contact (clock 14.5);
* **never more than 35 percent of the screen**: every size goes through `SpiritDiveMerge2Rules.coverage` / `maxExtent` with the real field of view and window aspect, tested at fov 30 / 70 / 110 and aspect 1 / 16:9 / 32:9
  for every clock; the spirit is also at most 60 percent as bright as the third person ghost and fades out as it comes within half a block of the eye (it can never block the view);
* **no blur** (no post shader), and **the faint inner silhouette is not drawn** in first person (the scan test fails if the first person classes mention it).

Only the local player, only while his own dive runs and the camera is first person, only while the merge flows (clock 0..18.6), only with `spiritDive.merge` and `spiritDive.mergeFirstPerson`.
**Cost:** one ghost (at most 900 vertices) and 4 vertices of flash, for one player, once per frame, for 22 ticks per dive; timed as `c.spiritdive.mergefp`; no packet, no server work, no per-player state.
Previews: `fp_<element>.png` is a first person camera in the harness (the camera sits at the eyes, 0.3 block ahead of the face so the head is out of the view; fov 70, tile 320 x 200): the geometry, the path and the
clamp are the real code, the picture is the harness (no bloom, no real hand or HUD in front of it).

## 5. Smaller, more distinct crowns (`crownDistinct`)

Which crowns existed: the **Wind A 3D crown** (`SpiritDiveWind4`, 14 / 8 arrow bars on a ring of radius 5.3 px up to 12.8 px, a lower second ring); the **REEL cube crown** (`SpiritDiveReelPieces.crown`, every element of the REEL look:
a ring of cubes with bars, white lights and a gold ring halo of 7.3 px); **Wind B** (cube crown), **Wind C** (a 3D mesh crown, `sdw3_meshes/crown`), the **PACK** sprite crown (mid distance of Wind A, and the PACK look);
the **ANIME** sprite crown (Wind, Water, Fire, Earth sprites). Now:

| Look / element | The new crown |
|---|---|
| **Wind A** (full detail), **REEL Wind** | new mesh `crown_wind_<char|std>[_half]`: a tight thin ring (radius 3.3 px, 62 percent), **7 (character) / 5 (standard) arrow bars** of alternating height with **one tall bar at the front** (9.4 px, 73 percent of 12.8), the head floats at -10.4 px (the old ring at -12.6: closer, still floating); a bright rim line over a darker teal band (the gap that keeps it from blurring into the wings behind); one haze plane a bar; **no second ring**; no spin, so the tall bar stays in front (the old crown turned slowly); the first dive of Wind keeps the left half |
| **REEL Fire** | `crown_fire_*`: curved flame tongues with hot white-yellow tips, alternating heights |
| **REEL Water** | `crown_water_*`: wave crests curling outwards, a drop above every second crest |
| **REEL Earth** | `crown_earth_std`: five crystal spikes (five sided prisms with pointed tops, leaning out) on a rock ring |
| Wind B, Wind C, the PACK sprite crown, the ANIME sprite crown | **scaled only** (62 percent about the head's top: smaller and closer); they are cubes, a mesh, sprites of their own style, not remodelled |

Vertices (crown only): Wind 320 (standard) / 400 (character), the half crowns 180 / 220, against 400 / 688 and 232 / 344 for the old Wind A crown (**cheaper by 80 to 288**); Fire 320 / 400; Water 476 / 612 (the drops);
Earth 316. Budget in the rules and tests: 480 standard, 620 character. Measured in the harness (the whole steady dive at age 50, front view, vertices before -> after): Wind standard 1,332 -> 1,252, Wind Yuno 2,776 -> 2,488, Fire standard 1,372 -> 1,272, Fuegoleon 2,720 -> 2,484, Water Noelle 2,136 -> 2,112 (the drops cost nearly what the cubes did), Earth 1,356 -> 1,252: every crown is cheaper or equal. The same for the entry (tick 14, standard Wind): 2,840 -> 2,720 vertices.
Previews: `crowns_<element>_<tier>.png`: front, three quarters and side, BEFORE (left) and AFTER (right), on a dark (top) and a bright (bottom) background; `crowns_meshes.png` the bare meshes.

## 6. Files

| Area | File |
|---|---|
| pure rules (common, no Minecraft class) | `book/SpiritDiveMerge2Rules.java` (options, mesh names, budgets, calm numbers, first person path and coverage clamp, crown numbers) |
| client | `client/aura/SpiritDiveMerge2.java` (options + the new crown painter + `fit`), `SpiritDiveMergeFirstPerson.java` (drawing), `DiveMergeFirstPerson.java` (event), `SpiritDiveMerge.java` (mesh choice, shared `draw`) |
| assets | `assets/nusmp/sdm2_meshes/*.txt` (20 files), generators `tools/gen_sdm2_models.py`, `tools/gen_sdm2_crowns.py` |
| self-test | `book/SpiritDiveMerge2SelfTests.java` (`magic.spirit_dive_merge2`, registered in `SpiritDiveBoot`) |
| tests | `SpiritDiveMerge2Test` (rules), `SpiritDiveMerge2AssetTest` (files), `SpiritDiveMerge2ScanTest` (source) |
| previews | `tools/spirit_dive_merge2_scenes.py` (63 scenes), `tools/spirit_dive_merge2_sheets.py`, helpers `tools/render_preview/examples/Sdm2*.java` |

## 7. Rules 9 and 11 (server friendly, no lag), worst cases

* **Server / multiplayer:** no packet, no server work, no config the server reads, no per-player state (the side comes from the UUID hash each frame); the first person hook acts for `Minecraft.player` only;
  the common classes name no client class (scan test, `ServerSafetyTest`). The self-test is pure and runs on the dedicated server.
* **Per diver and frame (third person):** worst 1,400 vertices during the 22 tick entry / exit within 32 blocks, 224 steady at full detail; at most 3 merges a frame; no allocation (static scratch, meshes resolved once).
* **First person:** one ghost (at most 900) + 4 vertices, one player.
* **Crowns:** fewer vertices than before in every look; `fit(pose)` is a translate and a scale.
* **Entry:** not more than before.

## 8. What could not be verified (honest list)

* **Nothing was seen in a running game**: no bloom, real lighting, real depth, other mods, or the real first person camera (the hook and the camera rotation are written against the NeoForge API and type-checked
  against the API stubs, but never run). The first person previews are a harness camera at the eye, not a game screenshot. Check in the game: that the camera rotation maps the view space the way the code assumes
  (a spirit that appears at the bottom or flies the wrong way would mean the flipped sign at `px = -PATH[0]`), the size of the flash sliver at the bottom edge, and that nothing is hidden behind the hand.
* The slim models are low poly and read as spirits in the harness at this size; the Salamander coil is the least convincing (a flat ring with a raised neck) and the Undine is slim; they are not the anime paintings.
* The additive culling of the real render type was not seen; the open sheets (wings, fluke, hair) are emitted on both faces to be safe, the closed bodies are wound outward.
* The calm entry keeps the old flash at the start and a burst at 17..20; whether a third is the right level in the game, and whether the burst reads as the end of the merge, has to be judged there.
  `CALM` (0.30) and `BURST_AT` (18.4) are the numbers.
* Wind B / C, PACK and ANIME crowns are only scaled; if the owner wants element shapes there too, those looks need their own meshes.
* Earth has no character tier, so its crown exists once.
* The Japanese and English wiki pages were not needed (no new canon facts; the spirit forms come from `docs/magic/spirit_dives_anime.md`).

## 9. Rule 12 checklist

New work checked for new versus replacement (section 1); kept behind four options that restore the old behaviour; shared files only appended to or hooked at one place; pure tests plus a self-test; the coordinator has to
add `magic.spirit_dive_merge2` to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`, bump the version, write the changelog and keep the two lines of `DiveClientConfig` / `SpiritDiveBoot` when merging with other builders.
Not done here (the coordinator's): version, changelog, README / LIBRARIES, `en_us.json`, `.github`.
