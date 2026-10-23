# Spirit Dive in the style of the Djinn Full Equip

Owner request (round 1.99.x): "I want to use the same sorta style as the Full Djinn equip, cause I hate the style the current Spirit Dives have, never used that style for anything." Every Spirit Dive
(Wind / Sylph, Fire / Salamander, Water / Undine, Earth / Gnome) is rebuilt as a full set of plates on the player's own body, forged on piece by piece the way the Djinn Full Equip is. It is the new default
look; the 3.0.0 remake (wind halo and blades, flame mane, water ribbons, rock crown) and the 1.99 look stay in the game behind a client option.

Nothing here was seen in a running game. Every picture is the repo's headless rasteriser (`tools/render_preview`, the real painter classes against Minecraft stubs). Read section 11 before trusting any
number about looks.

## 1. What defines the style (the study of the Djinn Full Equip)

Read from `client/relics/djinn/` (DjinnArmorLayer, DjinnMeshes, DjinnDraw, DjinnLook, DjinnFirstPerson, DjinnAmbient), `relics/djinn/DjinnEngine` (the sounds), `tools/relics/` (gen_djinn_art, djinn_parts,
mesh_lib, gen_djinn_unique), `docs/djinn_roster_36.md`, `docs/djinn_weapons.md`, the Djinn previews (`docs/previews/djinn36/`) and the Requip armours, which are the same style built on the geo engine
(`tools/erza_armour_kit.py`, `docs/previews/erza/`). What makes it that style:

| Trait | What the Djinn does | What the dive does |
|---|---|---|
| Silhouette | Plates on every limb: helm with crest / horns, chest and belt, both pauldrons, vambraces, gauntlets, greaves, a mantle, wings, a tail, a halo or a piece standing behind the back. The skin is never seen between plates. | The same: helm, crest, chest, backplate, belt, pauldrons, vambraces, gauntlets, cuisses, greaves, sabatons for every tier; the character tier adds gorget, skirt and the signature pieces (wings, plume, mantle, crown). |
| Surface | Painted plates: engraved edges, trim, rivets, filigree, gems, a body colour and a gold or silver trim, worn edges. Quads, no thickness. | The Djinn-quality painter of the Requip armours (height field lit from the top left, edge wear, decals, gems, emblems): leaf, wave, flame, diamond, sun. Boxes thinner than 0.5 to 0.58 px are written as two broad faces only (the Djinn's quads have no thickness either). |
| Glow | A second, additive pass of the engraved glow map, full bright, tinted by the Djinn's glow colour, breathing (`0.85 + 0.15 sin`), brighter on a cast. | The same pass per piece (the piece's glow factor, 0 = no glow pass), tinted by the tier's glow colour, the V2 pulse, plus 40 percent while the wearer casts. |
| Body art | An additive rune texture drawn over the player model: glow veins on the skin between the plates; a fresnel aura through the Zagred shader. | The additive vein texture of the element over the armoured parts (never the head) and a faint haze (`aura/spirit2_<element>_veins`, `spirit_<element>_haze`). The Zagred shader is not used (see section 11). |
| Forge-in | Each piece flies in from its own side while it fades up and grows 85 to 100 percent over 8 ticks, staggered 0 to 12 ticks by weight (the light under-layers first, the plates, the heavy cuirass, then the wings and the back piece); the pieces shatter out over 12 ticks when it ends. | `EquipAnimation`, the Djinn's own arithmetic. The delays and fly-in sides copy `requip/ArmourSets`. A ring of light opens at the feet while it forges (white and the element's colour; a third, larger one for the character tier) and one flare at the chest. |
| Sound | Vanilla sounds from the server at the change of phase: amethyst resonance, a beacon, amethyst breaking. | The same three moments (begin, upgrade, end), `SpiritDiveSound` / `SpiritDiveForge`. |
| Restrained particles | 6 to 13 ambient particles a second per Djinn. | 5 a second (standard), 10 a second (character), through `Fx` and its budgets. |
| Hands | Gauntlet and vambrace in first person (`DjinnFirstPerson`). | `DiveFirstPerson`: the same hand placement, the dive's own gauntlet and vambrace. |
| Cost | 2856 to 4351 quads per unique set (36 sets, counted from the files), a 1024 x 1024 atlas of 5.4 to 7.3 texels per model pixel; skipped beyond 48 blocks, no details beyond 24. | See section 7: never more than the Djinn's own numbers. |

## 2. What was built, in one paragraph

Seven generated armour sets (`tools/gen_spirit_dive_djinn.py`): Wind standard and Yuno, Fire standard and Fuegoleon, Water standard and Noelle, Earth standard (no canon character: always the standard
tier). 138 geo models with textures, glow maps and clips. A pure table (`book/SpiritDiveDjinnPieces`), a painter (`client/aura/SpiritDiveDjinn`), first person gauntlets (`client/aura/DiveFirstPerson`), a
forge sound (`book/SpiritDiveForge`, `SpiritDiveSound`), the look option `look = DJINN | V2 | CLASSIC` (default DJINN) in the client config, a self-test, three test classes, and the preview tools.

## 3. The look option and the tiers

* `nusmp-spiritdive-client.toml`, `spiritDive.look = DJINN` (default), `V2` (the remake of 3.0.0, untouched) or `CLASSIC` (1.99, untouched). The old key `spiritDive.classicLook = true` still wins and means CLASSIC.
* The server entry point is untouched: `GrimoireBook.spiritDive`, `SpiritDiveForm.look`, `gradeOf`, the style number (element + 4 * stage + 16 * grade), the server config `spiritDive.characterTier`. The grade picks the tier exactly as before
  (`SpiritDiveTier.gradeFor`, `CharacterSpells.characterOf`): Yuno's grimoire on Wind, Fuegoleon's on Fire, Noelle's on Water earn the character set; everyone else the standard set; Earth always the standard set.
* The client draws the character set only for the nearest `SpiritDiveTier.CHARACTER_PER_FRAME` = 3 dives of a frame (`SpiritDiveFrame.admitCharacter`), within 48 blocks, and when `spiritDive.characterEffects` is on;
  every other dive draws the standard set.
* Light, Darkness and Space spirits have no Spirit Dive in the mod (checked: their packages have links and abilities only), so there is nothing to remake for them.

## 4. The sets

Pieces are `assets/nusmp/geo/entity/sdj_<element>_<s|c>_arm_<piece>.geo.json` (+ `textures/entity/<id>.png`, `<id>_glow.png`, `animations/entity/<id>.animation.json` for the pieces that move). Common to every
full set: helm, chest, backplate, belt, pauldron r/l, vambrace r/l, gauntlet r/l, cuisse r/l, greave r/l, sabaton r/l. Character sets add gorget and skirt.

| Element | Colours | Standard set (every mage) | Character set |
|---|---|---|---|
| **Wind** (Sylph) | green plates, gold trim, white-green glow veins | feathered-wing pauldrons as plates, a circlet helm with a gold-and-green LAUREL of leaf plates round each temple (`crest_l`, `crest_r`) | **Yuno**: plus the crown-like crest MOUNTED on the helm (`crest_c`: a gold ring with a fan of nine leaf prongs and an arc of three behind, a gem in the brow: Yuno's crown halo as a helm crest, not a floating halo), finned vambraces, gorget, skirt, ear-wings, and WING PLATES (`wing_r/l`: two rows of feather blades on a gold spar, idle flap, faster in the air) |
| **Fire** (Salamander) | crimson plates, gold trim, ember glow veins | spiked pauldrons and greaves, claw gauntlets, a visor helm, a three-tongue FLAME CREST of gold-edged plates | **Fuegoleon**: a seven-tongue flame crest and two gold horns, the PLUME (`plume`: three chains of flame-shaped plates hinged on bones, streaming back over the nape, swaying), the MANTLE (`mantle`: five hanging columns of overlapping lames), wing plates of crimson feather blades. His right arm is his flaming arm (`FuegoleonArmAura`): no plate of the right arm is listed for his grade. |
| **Water** (Undine) | pearl and sapphire plates, silver trim, wave engravings | fin pauldrons, a circlet helm with a FIN CREST (a dorsal fin of three pearl plates and a swept fin at each temple), plated skirt panels (tassets) | **Noelle**: a five-plate dorsal fin and two fins a side, a long plated SKIRT of flowing lames (swaying clip), finned vambraces, gorget, fin WING PLATES (translucent membrane ribs) |
| **Earth** (Gnome) | stone and moss-gold plates, amber crystal glow veins | the heaviest silhouette: heavy chest, tiered pauldrons, ring vambraces, plated skirt; a CRYSTAL CROWN (stone ring, nine amber prisms, crystal clusters at the temples), a stone slab on the back with a cluster of amber crystals (`backcrystals`) | (none: the earth spirit is never named) |

Stages (the Djinn's Half Equip and Full Equip): HALF = the crest and the arms (Wind: the LEFT half of the body: left crest, pauldron, vambrace, gauntlet, cuisse, greave, sabaton, left wing plate for Yuno, as in the
series); FULL = the whole set; UPGRADE = a half dive that becomes full: the half's pieces stay on, only the missing ones forge in (`alreadyOn`).

## 5. NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| **NEW** | the Djinn look (the default) and everything it needs: `book/SpiritDiveDjinnPieces`, `book/SpiritDiveForge`, `book/SpiritDiveSound`, `client/aura/SpiritDiveDjinn`, `client/aura/DiveFirstPerson`; 138 geo models, textures, glow maps and clips (`sdj_*`); the generator kit `tools/gen_spirit_dive_djinn.py`, `sdj_sets.py`, `sdj_parts.py`, `sdj_fit.json`; the preview tools `tools/spirit_dive_djinn_scenes.py`, `tools/spirit_dive_djinn_previews.py`, the scene file `tools/render_preview/scenes.d/spirit_dive_djinn.json`, `examples/SdjDjinn.java`; the server switch `spiritDive.forgeSound`; the self-test `magic.spirit_dive_djinn`; the tests `SpiritDiveDjinnTest`, `SpiritDiveDjinnAssetTest`, `SpiritDiveDjinnScanTest`; this doc and `docs/previews/spirit_dive_djinn/` |
| **REPLACED** | the DEFAULT look of the four dives: the V2 look (3.0.0) is no longer drawn unless chosen (`look = V2`); the particle trail and the first person hand of the dive now follow the Djinn look when it is on (V2 and CLASSIC keep their own trail numbers) |
| **KEPT** | everything else, unchanged: the V2 look (`SpiritDiveAura.remade`, `SpiritDiveFx`, `SpiritDivePieces` V2 lists, its geo pieces, sprites and veins), the classic 1.99 look (its pieces, passes and models), the server entry point (`GrimoireBook.spiritDive`, `SpiritDiveForm` start / look / gradeOf / stages), the style numbers, `SpiritDiveTier` (grades, LOD cuts, `CHARACTER_PER_FRAME`), Fuegoleon's flaming arm (not edited: `FuegoleonArm*`, `FireArmFirstPerson`), the existing configs and tests, the Time Magic VFX (not touched), all Djinn code (not edited) |

Edits to existing files (additions only): `SpiritDiveLook` (the `Look` option; the old boolean form kept), `DiveClientConfig` (`look`), `DiveClient` (two listeners), `DiveParticles` (the Djinn trail numbers),
`SpiritDiveAura` (one line choosing the Djinn painter first), `SpiritDiveForm` (three one-line calls to the sound), `SpiritDiveConfig` (`forgeSound`), `SpiritDiveSelfTests` (the new test), the two preview scene files
(`book/SpiritDiveDjinnPieces.java` added to their `realFiles`: the painter now names it).

## 6. How it is drawn

* `SpiritDiveAura.paint` picks the look; for DJINN `SpiritDiveDjinn.paint`: distance band (`SpiritDiveTier.lod`: FULL up to 24 blocks, MID to 48, FAR to 64, culled beyond), the character admission, then
  (1) the veins pass (FULL only), (2) the pieces, (3) the extras.
* Pieces: each is anchored on the model part it was built for (so it follows crouch, swing and swim), forged in and shattered out by `EquipAnimation` (`assembled`, `begin`: the Djinn's arithmetic). Translucent while
  forging, cutout when on; the glow map additive and full bright. Slim arms: arm pieces narrowed to 80 percent about the arm pivot (`WaterAuraRig.slim`). The helm, crests and plume are skipped for the local
  player in first person. `detail` pieces (belt, gorget, gauntlets, sabatons, cuisses, skirt, plume, mantle) are skipped beyond 24 blocks; beyond 48 only the `silhouette` pieces (helm, crests, pauldrons,
  chest, backplate, mantle, wings, crystals) are drawn, body pass only.
* Extras (restrained): the forge rings and the chest flare while forging, one slow ring on the ground, 6 / 12 motes (none beyond 48 blocks for the standard set, 4 for the character set).
* First person (`DiveFirstPerson`): the gauntlet and the vambrace of the arm in the hand's pose (`RenderArmEvent` for an empty hand, `RenderHandEvent` for a held item: the placement the Djinn uses), only for this
  client's own dive, only with the Djinn look. The right arm of Fuegoleon's dive and any right arm while `Aura.FUEGOLEON_ARM` runs on the local player get no plate: that arm is `FireArmFirstPerson`'s.
* Sound: begin (half: amethyst resonance; full: beacon activate + resonance; character: + a beacon power note), upgrade (chime + resonance), end (amethyst breaking): vanilla sounds from the server, at most three
  a cue, one cue per player per second (the last cue's tick lives in the player's own data: nothing to clean up at logout), pitch by element (wind 1.3, water 1.0, fire 0.8, earth 0.6).

## 7. Budgets (rule 11): the worst case, with numbers

The reference is the Djinn Full Equip, counted from its mesh files (`assets/nusmp/djinn/u_*`, 36 unique sets, body plus glow quads): the largest set has **4351 quads = 17,404 vertices** (`u_vepar`), the
smallest **2856 quads = 11,424 vertices** (`u_marchosias`); the shared rigs go up to 4856 quads. A dive may submit at most that many vertices.

| | Standard set | Character set |
|---|---|---|
| Cap for the pieces (Djinn's smallest / largest set minus the veins and the extras) | 10,796 vertices | 16,744 vertices |
| The sets as built (faces x 4 vertices x passes; a piece with no glow costs one pass) | Wind 10,720, Fire 10,704, Water 10,168, Earth 10,600 | Wind 16,536, Fire 16,172, Water 15,168 |
| Vein layer (twelve model boxes, 24 vertices, two passes) | 576 | 576 |
| Extras (flat ring 2 quads, flare 1, motes 1 each) | 13 quads = 52 | 21 quads = 84 |
| **One dive at full detail, worst case** | **11,348 (Djinn's smallest: 11,424)** | **17,196 (Djinn's largest: 17,404)** |
| Faces per set | 1,499 to 1,573 | 2,129 to 2,293 |
| Painted texels per set (the Djinn atlas: 1,048,576) | 0.29 to 0.46 million | 1.08 to 1.29 million |

* The sets are fitted to the caps by `python3 -B tools/gen_spirit_dive_djinn.py --fit` (the least panel thinning, then the least sliver cut, that stays under the cap; the answer is `tools/sdj_fit.json`, so a normal
  build reproduces the files). `SpiritDiveDjinnAssetTest.everySetStaysUnderTheDjinnFullEquipsVertexBudget` and the self-test recount from the real model files.
* A frame: at most 3 character dives and the rest standard: 20 divers in the full band cost at most 3 x 17,196 + 17 x 11,348 = **244,504 vertices** (20 Djinn bearers: up to 348,080). At 24 to 48 blocks the detail
  pieces and the veins go, beyond 48 only the silhouette pieces in one pass, beyond 64 blocks nothing is drawn. No allocation per frame (statics for the scratch quaternion, the specs, the textures; the scan test
  pins it), `PerfProbe` section `c.spiritdive.djinn`.
* Particles: one particle every fourth tick (standard) or second tick (character), at most 6 dives within 24 blocks: **at most 60 a second** before the client particle budget and the effect quality thin them
  (the remake: up to 240); an entry burst of 6 / 16.
* Network: **no new packet**. The dive sends the same one lease a second with the style number. Sounds are vanilla sound packets to the players near: at most 3 per cue, one cue a second per player.
* Server: one `long` written to the player's data per cue; nothing scanned, nothing spawned, no chunk loaded.

## 8. Settings

| File | Key | Default | Effect |
|---|---|---|---|
| `nusmp-spiritdive-client.toml` (CLIENT) | `spiritDive.look` | `DJINN` | `DJINN`, `V2` or `CLASSIC` |
| | `spiritDive.classicLook` | false | kept from 3.0.0: true is CLASSIC and wins over `look` |
| | `spiritDive.characterEffects` | true | false: every dive is drawn as the standard set |
| | `spiritDive.particles` | true | the particle trail |
| `nusmp-spiritdive-server.toml` (SERVER) | `spiritDive.forgeSound` | true | false: the dive is silent |
| | `spiritDive.characterTier`, `spiritDive.fuegoleonArm` | true | unchanged |

## 9. What each part is tested by

* `SpiritDiveDjinnTest` (pure): the default look and the old boolean form; the tier mapping (Earth never character, a stray grade never earns another's set); every named piece has a definition; the half set is part of the full set
  and the full set has plates on every limb; the character set adds the ornate and signature pieces; the first Wind dive is left-only; no right-arm plate for Fuegoleon; an upgrade forges only the missing pieces; the forge
  follows the weight of a piece, inside 12 ticks, the last piece on by tick 20; the anchors; the levels of detail; the budget arithmetic (the caps are the Djinn's numbers, a crowd's worst case); the glow passes; the flash; the
  restrained trail; the forge cues are few, known and rate limited.
* `SpiritDiveDjinnAssetTest` (reads the repo): every piece has model, texture, glow map and clip; the clip is in the animation; the models are valid (identifier, faces inside the texture, texture size equals the declared size,
  at most 1024); the glow maps are clean (SafeAdditive: no colour under alpha 0: the test found stray pixels in more than ten of the first files, the generator now cleans them) and not empty where the table gives a glow pass; no stale model
  file; the Djinn's own quad counts are what the constants say; every set is under its cap; the standard set is the cheaper one; the character sets paint more texels than the Djinn's atlas.
* `SpiritDiveDjinnScanTest` (reads the source): no client class in the common classes; the pure classes touch no Minecraft class; no static table of players; no allocation or built location / string in the painters' frame
  methods; no hand-made render type; the distance bands, the character cap, the probe, `EquipAnimation`, `alreadyOn` and slim arms are in the painter; the first person hand skips Fuegoleon's flame arm; V2 and classic are still in
  the painter and their models still exist; the option has the right default and keeps the old key; the self-test and the three sound calls are in place; no hunger; no packet.
* Self-test (CI server smoke test): **`magic.spirit_dive_djinn`**: every piece of every set exists (model, texture, glow map, clip), the lists are non-empty, left-only for the first Wind dive, no right-arm plate for Fuegoleon, every set
  under its vertex cap counted from the real files, the forge cues are within bounds, and with a stand-in player the lease still carries the tier of the bound grimoire.
* The existing dive tests (`SpiritDiveTierTest`, `SpiritDivePiecesTest`, `SpiritDiveRemakeScanTest`) pass unchanged: **no existing test pinned the old default look**, so none was edited. `ServerSafetyTest`, `ConfigFileNamesTest`,
  `ParticleThemesTest`, `PerfSourceScanTest`, `SafeAdditiveSourceScanTest` pass; `tools/safe_additive/scan_textures.py` reports no offending texture; `tools/geo_check.py` is clean on all 138 models.

## 10. Previews (`docs/previews/spirit_dive_djinn/`)

Rebuilt by `python3 -B tools/spirit_dive_djinn_scenes.py`, `RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py <scenes>`, `python3 -B tools/spirit_dive_djinn_previews.py`. Each element sheet shows a
Djinn Full Equip (cut from `docs/previews/djinn36`, another rasteriser: judge the level of detail and the silhouette, not the shading) next to the standard tier (front, back) and the character tier (front, back, side).

| File | What it shows |
|---|---|
| `wind.png`, `fire.png`, `water.png`, `earth.png` | the full dive of each element next to a Djinn |
| `wind_close.png`, `fire_close.png`, `water_close.png`, `earth_close.png` | head and chest, legs, back, close up (character tier; Earth standard) |
| `wind_forge.png`, `fire_forge.png`, `water_forge.png`, `earth_forge.png` | the forge-in at ticks 2, 5, 8, 12, 16, 24: the pieces fly in, the rings open |
| `half_dives.png` | the first dives of every element and tier (Wind: left half only) |
| `upgrade_wind_yuno.png` | a half dive becoming full: the left half stays on, the right half forges in |
| `shatter_wind_yuno.png` | the last 18 ticks: the pieces drift out and fade |

Iteration log (each round: a change, a render of every scene, a look at the sheets):

1. **First render.** The Requip piece builders with the dive palettes and specs worked at once (green and gold plates with glow veins, the forge-in and the rings as the Djinn does them). Problems: no crest, no wings, the
   standard tiers left the thighs bare (the blue legs of the stand-in skin), and 30,000 vertices where the Djinn's largest set is 17,400.
2. **The spirit pieces and the budget.** Crowns, laurel, flame crest, fin crest, plume, mantle, wing plates and crystals added (`tools/sdj_parts.py`); thin boxes written as two faces; plate panels thinned; cuisses for every
   tier. The crown prongs stood parallel like a comb (fanned out), the Water crest was six slats like a keyboard (three big plates), the Earth crystals were stacks of boxes like bottles (real prisms with a pale point).
3. **The fit and the glow.** `--fit` finds the least thinning under each cap and writes `sdj_fit.json` (every character set is now 5 to 13 percent under the Djinn's largest set and every standard set within 6 percent under its smallest, instead of 55 percent over). The asset test caught colour under
   alpha 0 in the glow maps (the SafeAdditive rule): the generator cleans them. The Water plates were washed out to near white: the blue deepened to a pearl-sapphire.
4. **Earth.** The amber crystals read as orange tubes: deeper amber body, pale point, narrower, shorter, the back cluster a third smaller.
5. **The sequences.** Forge-in, half dives, the upgrade and the shatter looked at frame by frame for every tier: the pieces arrive in the Djinn's order, the left half of Yuno's half dive stays on while the right half forges in,
   the shatter leaves the player's own skin at the last tick. Found nothing broken in the harness; what the harness cannot show is in section 11.

## 11. What could not be verified (honest list)

* **Nothing was seen in a running game.** All pictures are the harness (a software rasteriser): the translucency of forging pieces, bloom, the real light, the particle themes, the crouch and swim poses, slim (Alex) arms and the
  distance behaviour are unknown. The stand-in player in the harness is the Steve skin with a red headband.
* **The Zagred fresnel aura** that the Djinn draws round the whole body (`NURenderTypes.zagredAura`) is NOT used: the dive's rim glow is the additive vein pass and the haze, which is weaker. Using the shader would need a
  silhouette texture and the shader's state; it can be added to `SpiritDiveDjinn.veins` later.
* **First person** (`DiveFirstPerson`): the placement is copied from the Djinn's and Fuegoleon's hands and not run; the forging fade of a piece in the hand has no fly-in. `DiveFirstPerson.ENABLED = false` switches it off in one line.
* **Fuegoleon**: the molten arm of the arm builder and the dive's left-arm plates were rendered together only in the harness; the right shoulder has no plate, which leaves the flame's shoulder flare uncovered by design.
* **The per frame cap of character dives** counts frames with a `RenderLevelStageEvent`; if no frame event arrives every dive is admitted (distance still applies).
* **`GeoAssetsTest` and `EquipAnimationTest` could not run** in the shim (they need Minecraft classes); their checks were repeated by `SpiritDiveDjinnAssetTest` and `tools/geo_check.py`. The type-check is against the API stubs, and Gradle was not available.
* The sabaton of the borrowed Requip builder has a flat sole slab under each boot that reads as a platform in close ups; the piece builders of the Requip armours were reused unchanged (they are another builder's files), so the
  silhouette language of the Requip armours (shingled lames, gold edging) is shared with the dive's; what makes the dives their own are the palettes, the engravings (leaf, wave, flame, diamond), the crests, plume, mantle, wings
  and crystals.
* **Canon research (rule 10):** no wiki page was fetched for this change. The element looks follow the owner's brief and the earlier remake's notes (`docs/magic/spirit_dives.md`, section 2: Yuno's half dive is left-only with a crown-like
  halo and wing-like blades, Fuegoleon's flame mane and flaming right arm, Noelle's water dress), whose own sources were search summaries and the owner's screenshots. Nothing could be newly checked against the English or the Japanese wiki.
* The smoke list of CI (`.github/workflows/smoke.yml`, `SMOKE_EXPECT`) was not edited (not this builder's file): the prefix **`magic.spirit_dive_djinn`** has to be appended to it.
