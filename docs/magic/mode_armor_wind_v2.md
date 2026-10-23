# Wind Spirit Dive overlay v2 (Yuno), "10x better"

Owner request: "this is the one now, upgrade it to look 10x better" (the Mode Armour overlay `WIND_SPIRIT_DIVE`, the render layer of Spirit of Zephyr from 1.107.0,
now Yuno's version of Spirit Dive). Prefix self-test `magic.mode_wind_v2`, textures `mwv2_*`, previews `docs/previews/mode_armor_wind_v2/`.
Kept identity: a fur-lined high-collared green coat with coat tails and fur cuffs, a crown of star blades, two semi-transparent wind wings.

References read: the owner's mode pack (`Black_clover_modes.zip`: Spirit_Dive.png, Spirit_of_Zephyr.png, the Boreas bow art with the fur collar and the crown of tall arrows),
the earlier A/B/C wind previews, `docs/mode_armor_spec.md` (wind section), the current preview `spirit_zephyr_layer/wind_spirit_dive_mode_armor_zoom.png`.
Wiki research: none needed (no new canon detail); nothing could not be fetched.

## What was built, how it is read, what is not included

**Built (client look only, no new packet):**
1. **Coat**, a real layered mesh instead of six slabs: a 12-sided lofted torso (shoulder, chest, waist, flared hem) with a button line, gold trim and lapels painted in; a **tapered, flaring fur collar**
   (outer wall with alpha-cut tufts, top lip, inner wall; standing 5.4 px behind the head, at the shoulder at the sides, low at the chin); **shoulder mantles** (domes with a fur trim); sleeves with
   **fur cuffs**; a **sash** with a gold buckle, two cords and a tassel; a fur hem; **four coat tails** (two long split ones behind, two short in front) that sway with a CPU wave and flare as you run;
   trousers and **boots with fur tops**. Cloth is cel banded (four bands, a dark outline, a light edge on every band step) with a gold back crest and wind-stitch curls.
2. **Glow thread** (additive pass, same meshes, `mwv2_glow.png`): seams, trim, buttons, crest and stitches light up; a very faint green wash over the whole coat is the aura.
3. **Crown**: ten arrow blades (square prism shaft, pyramid head) of alternating heights on a thin ring with a bright rim, a flat halo, floating, bobbing and turning slowly. **62 to 64 percent** of the
   old crown (ring radius 2.9 px vs 4.6, blades 4.45 / 3.15 px vs 7 / 5; a unit test pins 58 to 68 percent).
4. **Wings**: two big smooth **leaf sheets** (4 x 8 curved quads, outline from a leaf profile: convex leading edge, bright edge line, midrib and curved side veins, soft translucent gradient), a
   **second smaller leaf** each, a **cross ornament** near the tip and a small one at the root, a gentle flap (7 degrees) and a wind sway along the sheet; the big leaves are drawn twice (translucent and
   additive) so they shine. They unfold on the dive clock like the aura's wings did (clock 9 to 20), the crown ignites on clock 11 to 19.
5. **Details**: glowing eyes (additive), a faint hair tint, **wind ribbons** from both cuffs and both coat-tail ends (4 strips), six drifting **leaves** (capped).
6. **Setting** (client): `modeArmor.windLook = V2 | CLASSIC` in its own file `nusmp-modearmor-client.toml` (default V2). CLASSIC keeps the 73-quad overlay.

**Not included / not done:** no new model for the standard tier (see decision); slim-arm skins use the wide-arm sleeve (0.5 px of cloth stands off the arm, same as the old overlay);
the aura's alternative Winds B and C still draw their own crown and wings (the suppression hook is for the default Wind A only); no hair model (a tint only); the drifting leaves are quads in the
body frame, not world particles (so they follow the wearer and cost no particle budget).

## NEW / REPLACED / KEPT (rule 5)

| | |
|---|---|
| **REPLACED** (for the Yuno tier, setting V2) | the look of the overlay `WIND_SPIRIT_DIVE`: slabs, cards, 8 flat star blades, the two fan wings. The old overlay is not removed. |
| **NEW** | `ModeArmor.Mode.WIND_SPIRIT_DIVE_YUNO` (appended; wire only), the meshes (`book/ModeArmorWindV2Mesh`), the rules (`book/ModeArmorWindV2Rules`), the painter (`client/mode/ModeArmorWindV2`), the client option, five textures, the generator, the aura hook, the tests, the self-test, the previews. |
| **KEPT** | the 73-quad overlay (`ModeArmorLayer.wind`, textures `wind_*`) for every other Wind user and for CLASSIC; all other modes; the whole Wind A aura: sword at the hip, Boreas' bow, wrist circle of crosses, circle behind the back, ribbons, sparkles, ground ring, **merge layer and the 22 tick entry**; the B / C / PACK / REEL Winds; the library's 3D pieces for the classic overlay. |

## Which tier gets what (decision)

* **Yuno tier** (the mage whose bound grimoire is Yuno's, `SpiritDiveForm.gradeOf(p, WIND) == YUNO`): overlay v2 is the body (coat, crown, wings). The aura's **leaf wings and 3D crown are skipped**
  (`ModeArmorWindV2.replacesAuraPieces`, one map read per diver per frame) while v2 is on; everything else of Wind A stays.
* **Standard tier** (every other Wind user): **keeps the classic overlay unchanged** (recommended and done). Reason: v2 is Yuno's character look; a standard mage gets the light dive.
* **How the client knows the tier, also for Spirit of Zephyr (no dive aura):** `ModeArmor.start` sends the existing mode packet with the appended ordinal `WIND_SPIRIT_DIVE_YUNO` when the wearer is
  Yuno-tier (one grimoire read per second per diving mage, the same read the dive already does). The server keeps and returns `WIND_SPIRIT_DIVE` everywhere, so every existing check
  (`active == overlay`, `stop(overlay)`) is unchanged; `onStartTracking` / `onLogin` send the same variant (one persisted boolean `nusmp_mode_armor_yuno`). No new packet.
  If the server turns the character tier off (`spiritDive.characterTier`) Yuno gets the classic overlay too.

## Budgets (rule 11)

Vertices of one wearer, counted from the real meshes (`ModeArmorWindV2Rules.vertices`, pinned by `ModeArmorWindV2Test`):

| level (distance) | cap | counted | what |
|---|---|---|---|
| FULL (0 to 24 blocks, and admitted) | 3,200 | **3,168** | coat 864, fur 560, glow 440, wings 352 + 352 additive, crown / crosses 488, halo, ribbons, eyes, hair 112 (leaves 24 included in FX) |
| MID (24 to 48, or not admitted) | 1,200 | **732** | torso, sash, one pair of tails, sleeves, legs, collar, cuffs, torso glow, both leaves, crown with 6 flat blades, halo |
| FAR (48 to 64) | 150 | **60** | one leaf each, crown ring, halo |
| beyond 64 | 0 | 0 | nothing |

* **At most 3 wearers per frame draw FULL** (`SpiritDiveFrame.admitModeV2`, a `PerfBudget` of 3 per frame keyed by the frame counter, the same pattern as `admitCharacter`); the others draw MID. A crowd
  of N Yunos costs 3 x 3,168 + (N - 3) x 732 vertices per frame. The old overlay cost 292.
* **Combined Yuno dive, calm steady state, FULL:** the aura without its wings and crown 1,616 + overlay 3,168 = **4,784** (cap 6,500; the aura alone was 2,624). Measured in the harness with the merge
  layer and ground ring at age 50: 5,068. Entry (ticks 0 to 22) is the loud part and peaks at about 7,300 for a few ticks at tick 14 (the aura's own entry flash was 5,456 before); the owner's
  rule is "loud only in entry / exit / after a cast", the steady state is what the cap holds.
* **Client cost:** no per-frame allocation by this class (static pose slots and scratch fields; the PoseStack's own `pushPose` allocates as in every layer: 9 pushes per wearer); 6 buffer switches per
  wearer (pass by pass, not piece by piece); one sine per swaying vertex (about 900 at FULL); one `HashMap` read for the hook.
* **Server cost:** none beyond one grimoire read per second per diving Wind mage (already done by `SpiritDiveForm.look`) and one persisted boolean; no packet, no tick work, no entity.
  Nothing to clean up on logout / death / dimension change: the client map is cleared by `ModeArmorClient` as before.
* **Server friendly (rule 9):** all rules and meshes are pure (`book/`), the painter is client only, the option is a CLIENT config, `ServerSafetyTest` and the new scan test pass; the mode
  variant is decided on the server from the grimoire.

## Preview results (harness renders, NOT game screenshots)

`docs/previews/mode_armor_wind_v2/`: `before_after_full.png` (5 views), `collar_closeup.png`, `crown_closeup.png`, `wings_closeup.png`, `running_coat_tails.png`, `entry_overlay.png`,
`yuno_dive_combined.png` + `yuno_dive_big.png` (aura Wind A with the sword, BEFORE the old overlay AND the aura's wings / crown, AFTER overlay v2), `yuno_dive_entry.png`, the texture atlases.
Reproduce: `python tools/gen_mwv2_textures.py`, `python tools/mode_armor_wind_v2_scenes.py`, `RENDER_PREVIEW_BUILD=<dir> python tools/render_preview/preview.py <scene names>`,
`RENDER_PREVIEW_BUILD=<dir> python tools/mode_armor_wind_v2_sheets.py`. The harness runs the real `ModeArmorLayer` and the real aura painters (`tools/render_preview/examples/MwV2*.java`). The old
`python tools/mode_preview/preview.py` still works and shows the new mode as one more column.

Iterations: five rounds against the pack (silhouette of the wings crossing the head, wing and fur brightness, crown and cross sizes, eye and hair tint, fur and cloth contrast, winding of every quad
until the harness reported none, the 60 to 65 percent crown).

## Files

`book/ModeArmorWindV2Rules.java`, `book/ModeArmorWindV2Mesh.java`, `book/ModeArmorWindV2SelfTests.java` (`magic.mode_wind_v2`, registered in `SpiritDiveBoot`),
`client/mode/ModeArmorWindV2.java`, `client/mode/ModeArmorWindV2Config.java` (`NUSMP.java` one line), small edits in `blackclover/ModeArmor.java`, `client/mode/ModeArmorLayer.java`,
`client/aura/SpiritDiveFrame.java`, `client/aura/SpiritDiveWind.java`; `tools/gen_mwv2_textures.py`, `tools/mwv2_layout.json`, `tools/mode_armor_wind_v2_*.py`, harness stubs and examples;
tests `ModeArmorWindV2Test`, `ModeArmorWindV2AssetTest`, `ModeArmorWindV2ScanTest`.

## Not seen in a game (honest caveats)

* Everything above is from the software harness. Not seen in a game: the real lighting of the coat (the harness lights approximately), the real blend of the additive wing and glow passes
  (`SafeAdditive` picks the alpha-weighted type), z-fighting between the coat shell and a worn chestplate, the look with slim arms, the exact bob / flap / sway speed, and the frame time of three Yunos at
  once (budgeted and counted, not measured; `/multiverse perf` shows the layer as `c.mode.armor_layer`).
* The CI smoke list in `.github` was not touched: add the prefix `magic.mode_wind_v2`.
* The entry peak (about 7,300 vertices for a few ticks) is above the steady cap by design.

## Blocky arms (option `modeArmor.windArms = BLOCKY | SMOOTH`)

Owner request: "I want the Blocky part for the arms for the classic." Our reading: the arms of the V2 overlay should have the CLASSIC overlay's blocky pieces (square box sleeve, chunky fur cuff, sitting square round the arm).
Everything else of V2 (coat, collar, crown, wings, ribbons, glow, sash, tails) is unchanged. Previews (harness renders, not game screenshots): `docs/previews/mode_armor_wind_v2_blocky_arms/`
(`arms_before_after_classic_full.png`, `arms_closeup.png`, `arms_yuno_dive.png`, `arms_yuno_dive_sword.png`; rows BEFORE = smooth, AFTER = blocky, CLASSIC).
Reproduce: `python tools/gen_mwv2_textures.py`, `python tools/mode_armor_wind_v2_scenes.py`, `RENDER_PREVIEW_BUILD=<dir> python tools/render_preview/preview.py "mwv2_*"`,
`RENDER_PREVIEW_BUILD=<dir> python tools/mode_armor_wind_v2_sheets.py <out> arms_closeup.png ...`.

* **BLOCKY (default)**: the sleeve is the classic box (x -1.3..3.3, y -2.3..6.6, z +-2.3: the same boxes as `ModeArmorLayer.wind`), the fur cuff is the classic box (0.5 px wider all round, y 6.4..8.6) with the
  tuft line of the V2 fur along its top and a fur lip on top, and the shoulder junction is a square mantle slab (y -3.1..-0.9) over a square fur band (the cuff's box again), instead of the dome. The cloth is the
  cel banded V2 cloth: new regions of `mwv2_coat.png` / `mwv2_glow.png` (`SLEEVE_BOX` 4 faces of 20 x 36, `SLEEVE_CAP`, `MANTLE_BOX`, `MANTLE_CAP`; the coat texture rows 128..163 only, every old texel is byte for byte
  the same) with dark chunky corner lines, the gold band above the cuff, two gold buttons on the front face, a back seam, a wind curl on the outer and inner face, and the glow thread of V2. The boxes are 5 faces
  (the bottom is hidden by the cuff or faces the hand), flat normals, nothing sways. The wind ribbon from the cuff stays.
* **SMOOTH**: the first V2 arms (lofted round sleeve, flared cuff, domed mantle with fur trim), exactly as before.
* **Client option**: `modeArmor.windArms = BLOCKY | SMOOTH` (default BLOCKY) in the existing `nusmp-modearmor-client.toml` (`ModeArmorWindV2Config`, mirrored into `ModeArmorWindV2.setArms` on load / reload, so the painter reads a field).
  A CLIENT setting: only changes what this client draws; nothing on the server or the network.
* **`modeArmor.windLook = CLASSIC` is unchanged** and still draws the 73-quad classic overlay (`ModeArmorLayer.wind`, 292 vertices in the harness, the same as before): the change touches neither `ModeArmorLayer` nor the
  classic textures, `ModeArmorWindV2.active` is false for CLASSIC so `windArms` is ignored there; `ModeArmorWindV2Test` pins the wiring and the untouched classic sleeve / cuff boxes.

### Budget (rule 11)

| level | cap | SMOOTH (before) | BLOCKY (after) |
|---|---|---|---|
| FULL | 3,200 | 3,168 | **2,792** (-376: sleeve 128 + glow 128, cuff 128, mantle 128, trim 64 = 576 became 5 faces x 4 x 2 x 5 pieces = 200) |
| MID | 1,200 | 732 | **700** |
| FAR | 150 | 60 | 60 (no arms at this level) |

The vertex count does not grow with the option (`ModeArmorWindV2Test` pins blocky <= smooth at every level, 3,168 and 2,792 at FULL). Combined Yuno dive in the harness (aura Wind A with the sword, merge layer, ring, age 50): 4,924 smooth, 4,548 blocky;
the steady-state cap check (`yunoCombinedSteady`) still uses the smooth numbers (the larger). Texture cost: one extra 80 x 36 + three small regions in textures that are already loaded (no new texture, no new draw call, same passes).
No packet, no server state, no per-frame allocation (the same pass-by-pass painter; the arm pieces are static meshes).

### NEW / REPLACED / KEPT

* **NEW**: `ArmStyle` (BLOCKY, SMOOTH), pieces `SLEEVE_BOX`, `CUFF_BOX`, `MANTLE_BOX`, `MANTLE_TRIM_BOX` (appended to `Piece`), four coat / glow texture regions, the client option `windArms`, tests, previews, a harness scene set (`mwv2_smooth_*`) and a stub of the worn library for the harness.
* **REPLACED (default only)**: what the V2 overlay draws on the arms and the shoulders by default (smooth sleeves, cuffs, domed mantles -> blocky ones). Nothing is removed: the smooth pieces stay registered and are one option away.
* **KEPT**: everything else of V2, the classic overlay and the CLASSIC option, all textures (old regions identical), the aura, all other modes.

Not seen in a game: the real lighting of the box faces, the look with slim arms (the box is the wide-arm size, 0.3 px off the arm, like the classic), z-fighting of the shoulder slab with a worn chestplate.
No new smoke prefix: the self-test `magic.mode_wind_v2` now checks both arm styles, their caps and the option default.
