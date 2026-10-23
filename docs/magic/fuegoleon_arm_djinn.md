# Fuegoleon's flame arm, Djinn style (3.1): a sculpted limb of flame, forged in piece by piece

Owner request: "The flaming arm must be remade. I want to use the same sorta style as the Full Djinn equip; I hate the style the current Spirit Dives have." Then, with the exact target
(`docs/previews/fuegoleon_arm_djinn/reference_arm.png`, cropped from the owner's art): "Fuegoleon arm model to look as close as this image. ... If the Djinn-equip armour style conflicts with
matching this image, the IMAGE wins for the arm."

So the arm is **not** armour. The reference shows the right arm below the shoulder as a pure, solid limb of flame (bright orange body, a hot yellow core, a dark red outline at the silhouette,
flat cel-shaded bands) with the shape of a shoulder, upper arm, elbow, forearm, wrist and a clenched fist, flame tongues curling off the shoulder, the elbow (one big hooked tongue) and the wrist,
and a few drifting sparks; the rest of the body keeps its normal clothes. What the new look takes from the Djinn Equip is the way the Equip is **built and shown**.

## 1. The style, defined (what was studied and what was kept)

Studied: `docs/djinn_weapons.md`, `client/relics/djinn/` (`DjinnArmorLayer`, `DjinnMeshes`, `DjinnDraw`, `DjinnLook`, `DjinnFirstPerson`), the mesh files `assets/nusmp/djinn/u_*/` and the forge-in
(`client/aura/EquipAnimation`, the Djinn's own arithmetic). The Djinn Equip is:

1. **A set of pieces**, each its own mesh anchored on the model part it was built for (pauldron, vambrace, gauntlet ...), drawn with the part's pose so it follows every swing and crouch.
2. **Forged in piece by piece**: each piece fades up, grows from 85 percent and slides in from its own side over 8 ticks, the pieces staggered by 0 to 12 ticks, and shatters out the same way.
3. **Ornate painted textures with an emissive map** (glow layer, additive, full bright) lighting the veins.
4. **A first person path**: the arm pieces are drawn in the hand (`RenderArmEvent` for an empty hand, `RenderHandEvent` redoing the arm's placement for a held item).
5. **Bounded cost**: a piece is a few dozen quads.

Kept (the image does not conflict): 1 (five pieces), 2 (the very same `EquipAnimation`, here the flame *kindles* from the shoulder down to the fist with a flash), 3 (the glow map lights only the hot
core), 4 (the same hand placement), 5 (budget below). Dropped because the image has none: **armour plates, cracks cut through plates, the salamander-head pauldron or gauntlet crest**. The
Salamander is still the cause (the contract, the spirit out, the Fire dive: the server decides, unchanged) and its motif is the flame itself.

## 2. What it looks like

**The limb** (five models of the key `fuegoleonarm`, `tools/gen_fuegoleon_arm_djinn.py`; frame as the other geo tools: model pixels, y up, front on -z, the player's right arm at x = -6):

| piece | file | cubes | what it is |
|---|---|---|---|
| SHOULDER | `fuegoleonarm_shoulder` | 4 | the shoulder over the sleeve and its rounded cap (the red cloth of the sleeve stays above) |
| UPPER | `fuegoleonarm_upper` | 4 | the upper arm, the bicep the widest part |
| ELBOW | `fuegoleonarm_elbow` | 2 | the joint, narrow (the hollow between the muscles) |
| FORE | `fuegoleonarm_fore` | 4 | the forearm muscle, tapering to the wrist |
| FIST | `fuegoleonarm_fist` | 7 | the back of the hand, four curled fingers (knuckles forward and down), the thumb |

Every slice of the limb is a straight box (4.8 to 6.4 px wide: it covers the player's own arm) plus a smaller box turned 45 degrees, so the section reads as a tube with cut corners, not a square.
Textures are painted by the generator in the reference's flat cel bands (white-yellow, yellow, orange, red, dark red; a one texel dark red outline at every face edge) worked out in absolute
height, so a hot streak runs on from one slice into the next and the limb reads as one body of flame. Each piece has a glow map (hot core only) and a clip `idle` (a few degrees of sway, a different
phase per piece, so the arm breathes). All are drawn at full brightness (the cel bands must keep their colours).

**The tongues** (`FuegoleonArmDjinnFx`, table `FuegoleonArmDjinnRules.TONGUE`): ten flame tongues, best first: the big hooked tongue off the elbow (out, then up and curling back), one up off the
shoulder curling outward, wavy ones off the forearm, the wrist and knuckles, one trailing back off the elbow, one off the upper arm. Each is a strip of three quads that bends along a curl and
sways, turned about its own axis to face the camera (a cylindrical billboard: a flat cel flame from the side and in the hand), textured from `textures/aura/fad_tongue.png` (four frames, a 4 frame
flicker loop, flat bands with a dark red outline, alpha blended: additive would lose the outline). A tongue grows with the forge-in of the piece it belongs to.

**Sparks, haze, flashes**: up to eight small additive coals rise off the arm, one soft additive glow breathes behind it (restrained), and each piece gets a glow flash on its centre while it is forged.
World-space sparks are particles of the theme `fuegoleon_arm.sparks` (`FuegoleonArmSparks`, through `Fx`).

**Forge-in** (`FuegoleonArmDjinn.limb`): `EquipAnimation.assembled(t, left, delay)` with the delays 0, 3, 6, 9, 12 ticks: shoulder, upper arm, elbow, forearm, fist, the last whole at tick 20. It runs
whenever the lease starts (the Salamander bond starts, or the Salamander comes out: the lease style changes) and shatters out the same way if the lease ever runs out unrenewed. The server decides when
(`FuegoleonArm.reasonFor`, the `Aura.FUEGOLEON_ARM` lease, one renewal per 40 ticks, all unchanged); the client only picks the look.

**Strengths** (`FuegoleonArmDjinnRules.State`; the limb itself is solid at every strength, the numbers below move the flames):

| state | when | glow pass | tongues (of 10) | tongue length | sparks | haze |
|---|---|---|---|---|---|---|
| QUIET | the contract is held | 0.55 | 6 | x0.62 | 2 | 0.07 |
| OUT | the Salamander is out | 0.80 | 10 | x0.90 | 5 | 0.12 |
| FLARE | casting, or the Fire Spirit Dive runs | 1.00 | 10 | x1.25 | 8 | 0.20 |

`fuegoleonArm.view` still applies: OFF draws nothing, FLARE draws nothing at QUIET, ALWAYS (default) draws all.

**First person** (`FireArmFirstPerson`, same events and the same hand placement as before and as `DjinnFirstPerson`): the same five pieces and tongues in the hand, forge-in clocked from the lease's start,
the hand's own camera basis for the billboards. The slim (Alex) arm is detected like the third person painter does (`WaterAuraRig.slim`: the arm pivot's y), the limb is scaled to 80 percent of its width
and stays wide enough to cover the arm.

**Settings** (client, `nusmp-spiritdive-client.toml`): `fuegoleonArm.look` = `DJINN` (default) | `V3` | `CLASSIC`, beside `fuegoleonArm.view`. V3 is the 3.0.0 arm (molten box model, flame sheets, a mane of
strands); CLASSIC is the plain molten model of 3.0.0 without sheets or mane (cheapest). A viewer who dislikes the new look changes one word. Nothing in the server config changed.

**For the Spirit Dive builder:** `FuegoleonArmAura.drawsArm(AbstractClientPlayer p)` (public static) is true while this client draws the arm of `p` (lease running with a valid style, visible, the view not OFF),
in every look. The Fire dive's own set must not draw a second arm on a wearer for whom it is true.

## 3. NEW / REPLACED / KEPT

| | what |
|---|---|
| **NEW** | the Djinn-style flame limb: five models (`fuegoleonarm_*`, geo + clip + texture + glow, 21 cubes), the tongue sheet `textures/aura/fad_tongue.png`, `book/FuegoleonArmDjinnRules` (pure look / state / table / budget / cap rules), `client/aura/FuegoleonArmDjinn` (painter), `FuegoleonArmDjinnFx` (tongues, sparks, haze, flashes), `FuegoleonArmLook` (the look flag), `FuegoleonArmSparks` + the theme `fuegoleon_arm.sparks`, `FuegoleonArmAura.drawsArm`, the config key `fuegoleonArm.look`, the self-test `magic.fuegoleon_arm_djinn`, the generator `tools/gen_fuegoleon_arm_djinn.py`, the preview scenes (`scenes.d/fuegoleon_arm_djinn.json`, `examples/FadArm*.java`) and sheets tool (`tools/fad_preview_sheets.py`), two test classes |
| **REPLACED** (the default look only; the old one is behind `fuegoleonArm.look`) | the look of Fuegoleon's arm: the 3.0.0 molten box model with flame sheets and a mane is no longer drawn by default, in third and first person |
| **KEPT** | everything of the 3.0.0 arm, unchanged and reachable with `look = V3` / `CLASSIC`: the model `spiritdive_arm_flame_arm_r` and its clip, `FuegoleonArmFx`, `sdv_flame_sheet`, `sdv_wisp`, the painter code path in `FuegoleonArmAura` and `FireArmFirstPerson`; the server rules (`FuegoleonArm`, `FuegoleonArmRules`, the lease, its cadence and style), `fuegoleonArm.view`, the server switch `spiritDive.fuegoleonArm` |

Existing tests: none had to change (`FuegoleonArmRulesTest`, `SpiritDiveRemakeScanTest`, `SpiritDivePiecesTest` and the sweep below pass as they were; no test pinned the default look).

## 4. Budget (rule 11) and worst cases

* **Client, per arm, full detail (within 24 blocks):** limb 21 cubes x 24 vertices x 2 passes (body, glow) = 1008; effects 10 tongues x 3 segments + 8 sparks + 1 haze + 5 flashes = 44 quads = 176 vertices;
  **total 1184 vertices**, under the Djinn arm assembly the owner called the standard (unique set: pauldron 151 + vambrace 100 + gauntlet 80 quads = 331 quads = 1324 vertices; the test counts it from the
  mesh files). Beyond 24 blocks (HALF, up to 48): no glow pass, five tongues of two segments, no sparks, no haze: 504 + 40 = about 544 vertices. Beyond 48 blocks nothing is drawn (far arms are skipped).
* **Per frame:** at most 4 arms draw at the full level (`FuegoleonArmDjinnRules.Cap`, keyed by game time and partial tick, and by the first painter coming round again while paused); the rest of a crowd
  draw HALF. The wearer's own first person arm is not counted. No allocation per frame (scratch arrays only; the scan test checks the frame methods). Probe sections: `c.fuegoleon.arm` (existing,
  the painter), `c.fuegoleon.arm.sparks` (new).
* **Particles:** arms within 16 blocks, at most 4 arms per tick, one particle every 10 / 5 / 2 ticks (quiet / out / flare) per arm: at most 40 particles a second before the client particle budget, the
  effect quality and the distance bands thin them; nothing while paused; the viewer's `spiritDive.particles` switch applies.
* **Network:** unchanged: one lease renewal per 40 ticks per wearer (0.5 packets a second), sent to the players that track the wearer. The new look adds no packet.
* **Server:** unchanged (one modulo and a flag read per player per tick; one grimoire read per 40 ticks for non-Fuegoleon players). The new server code is only the self-test and a pure class.
* **Cleanup:** nothing new is stored: the lease table (`PlayerAuras`) is dropped at logout and cleared on death, as before.

## 5. Previews (`docs/previews/fuegoleon_arm_djinn/`, preview harness renders, never a game screenshot)

| file | shows |
|---|---|
| `compare_reference.png` | the reference crop, the new arm quiet / Salamander out / flare, the Djinn armour (a crop of `docs/previews/djinn36/bael_sheet.png`) |
| `states_third_person.png` | quiet / out / flare, front 3/4, right side, back 3/4, at normal distance |
| `forge_in.png`, `forge_in_close.png` | ticks 2, 5, 8, 11, 14, 18, 24 after the lease starts: the flame kindles from the shoulder down to the fist |
| `old_looks_and_slim.png` | the kept 3.0.0 look next to the new one, and the new one on the slim (Alex) arm |
| `first_person_mock.png` | **MOCK / APPROXIMATION**: a camera behind the shoulder looking down the swung arm; the real hand pass cannot be rendered by the harness |
| `reference_arm.png`, `djinn_gauntlet.png` | the inputs of the comparisons |

Rebuild: `python3 -B tools/gen_fuegoleon_arm_djinn.py`, then render the scenes and run `python3 -B tools/fad_preview_sheets.py` (commands in its docstring; `fad_flare` needs `SDV_CAST=1`).

**Iteration against the reference (seven rounds, each: change, render every scene, look at the sheets):**

1. First render: the limb was a barrel of noisy red-brown speckle and pale cream tongues like sheets of paper; far too wide (the turned box of equal size makes an eight-pointed star 1.4 times wider).
2. Absolute-height bands so streaks continue between slices, flat cel bands instead of per-texel noise; still too much dark red (stripes like a barber pole).
3. The turned box made 80 percent of the straight one and the straight box sized to just cover the player's arm; the tongue sprite redrawn with a pinched root, a belly and a long hooked tip, thinner.
4. Heat formula changed so faces are mostly orange with yellow blobs and red only toward the edges; the tongue core no longer pale cream but yellow-orange.
5. The fist: four chunky fingers with a crease and a thumb instead of a barcode of stripes; bicep and forearm widened, elbow and wrist narrowed (the reference's silhouette); redder palette (`FF6A14`, `DC300C`).
6. First person mock repositioned, flash glows shrunk (they read as grey discs), the look flag reset in the preview helper (a V3 scene had leaked its look into the next scene of the same run).
7. Tongue bands thinned so thin tongues stay orange-yellow with a red outline instead of dark red; limb heat raised a little.

What still differs from the reference, said plainly: the limb is built of cubes, so its silhouette is blocky where the art is smooth, and the tongues are flat cards with a four frame flicker, not the art's
many spiral curls; the arm hangs along the body (the reference arm is held out).

## 6. Tests

* `book/FuegoleonArmDjinnRulesTest` (17 cases, pure plus the real files): look selection and parsing, which look draws what, the states and the view, every number grows quiet -> out -> flare, the tongue
  table, the piece tables and the forge-in order, the flash, level of detail, the per-frame cap (including a paused clock), the spark cadence and its spread over a crowd, the budget by cube sums over the real model
  files against the Djinn assembly counted from its mesh files, packets unchanged, the assets (models, clips, glow maps with nothing under alpha 0, the tongue sheet: four differing frames, no black under clear texels).
* `book/FuegoleonArmDjinnScanTest` (11 cases, source scans): no client class and no static state in the common classes; no allocation and no built location or string in the frame methods (a negative
  control, a `new float[2]` put into a frame method, makes it fail); blends only through `AuraRender`; every count from the table; the per-frame cap, the distance cut, the forge-in and the probe sections; particles
  through `Fx` and the viewer's switch; the old arm, its assets and its code paths still there (rule 2); the server's rules untouched; the config key, the self-test, the wiring; no hunger.
* Self-test `magic.fuegoleon_arm_djinn` (`book/FuegoleonArmDjinnSelfTests`, run by the CI server smoke test): every file in the jar, the cubes of the real model files within the budget, the look table, and
  the server's decision unchanged (the contract gives the arm lease with style 1, another grimoire none). Registered from `SpiritDiveBoot`.
* The sweep that was run here (offline JUnit, the tests that need no game classes): the two new classes plus `FuegoleonArmRulesTest`, `SpiritDiveRemakeScanTest`, `SpiritDivePiecesTest`, `TextureAlphaTest`,
  `SafeAdditiveTextureScanTest`, `SafeAdditiveSourceScanTest`, `PerfSourceScanTest`, `ScreenBlurGuardTest`, `ParticleThemesTest`, `ConfigFileNamesTest`, `ServerSafetyTest`: all green. `GeoAssetsTest` and
  `EquipAnimationTest` need game classes and could not be compiled here; the models were checked with `tools/geo_check.py` and by the preview harness, which loads them with the real `GeoModelData` parser.

## 7. Sources and what could not be verified

* Design source: the owner's reference art (`reference_arm.png`) and the Djinn Equip code and docs of this repo. Series canon (the official profile: Fuegoleon "has a flaming right arm") as in `docs/magic/spirit_dives.md`
  section 2; no wiki was fetched for this change (no spell or character detail was looked up).
* **Nothing was seen in a running game.** Every picture is the preview harness; translucency, bloom, the real light, the particles and the distance behaviour are unknown.
* **The first person hand** is written from the game's own hand code and the Djinn gauntlet's placement, not run; the mock sheet is an approximation. `FireArmFirstPerson.ENABLED = false` still switches it off.
* **The slim arm**: the 80 percent scale and the `WaterAuraRig.slim` detection are the third person painter's existing logic, applied to the new limb; seen in the harness with a slim skin, not in a game.
* **The per-frame cap** keys a frame by game time and partial tick; in a running game a frame that lands on exactly the same partial tick (a very fast machine) would count as the same frame: the effect is
  only that one of the arms draws HALF.
* The flash glows look grey in the harness (its additive blend is an approximation); the real additive result is likely warmer.
* The cast flare reads `CastAnimClient.oneShotActive` through `DiveCast` (a stub in the harness: `SDV_CAST=1`).
