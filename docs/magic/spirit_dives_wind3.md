# Spirit Dive, Wind remake: three selectable designs with real 3D weapons (A, B, C)

The owner asked: "remake Wind Magic Spirit Dive again, I will allow you to make **actual rendered weapons like the clips**, I also want you to **show me 3 different versions**". This is that remake, both
tiers (the standard Wind user with the Sylph bond and Yuno, the mage whose bound grimoire is Yuno's). The weapons and pieces are now **real 3D models** (blade with ridge, fuller, cross-guard, wrapped grip,
rings as torus-like strips, leaf limbs with thickness, feather blades), authored in the project's mesh pipeline, instead of the flat textured strips of the PACK look.

Client option `spiritDive.windLook` (CLIENT config `nusmp-spiritdive-client.toml`, looks only, nothing the server decides): `PACK`, `REEL` (both unchanged, still choosable), and the three new designs
`A_ANIME`, `B_CLIP`, `C_SAINT` (enum values appended). **Default for now: `B_CLIP`** (see "Recommendation"). Nothing here is server work: no packet, no state, no key bind, no item, no loot.

Previews: `docs/previews/spirit_dive_wind3/`, all renders of the repo's software harness (`tools/render_preview`, `tools/preview_sdw3_models.py`), **not game screenshots** (no bloom, additive passes approximated, the
arms hang because the harness has no animation). `versions_yuno.png` / `versions_std.png` (A | B | C, same camera, front, front 3/4, back 3/4), `weapons_turntable_a.png` / `_b.png` (every real piece from four
angles, solid and with its glow shell), `weapons_in_place.png`, `entry_yuno.png` / `entry_std.png`, `cast_yuno.png`, `half_dives.png`, and each design next to its reference
(`ref_a_anime.png`, `ref_b_clip.png`, `ref_c_saint.png`).

## The three designs (two sentences each)

- **A "Anime-true"**: the PACK look (pale green-white light over the whole body, the thin arrow-bar crown on a ring, the big smooth leaf wing with its cross, the circle of crosses round the wrist, the back
  circle) whose weapons are now real: Zephyr's sword (hexagonal blade with a fuller, up-curved cross-guard, wrapped grip, gem pommel, two crossed rings turning round the hilt) floats at the right hip, Boreas'
  leaf bow (two leaf limbs, cross ornaments, a string) rides on the back and comes into the left hand with a real arrow of light while a page is cast. Standard tier: a small leaf blade, no rings.
- **B "Clip-style"**: what the clips show, in mint / teal / white: a ring of glowing cubes and tall cube bars with white lights as the crown, glowing cube gauntlets, a voxel leaf wing of stacked glowing panels
  (a small one on the other side for Yuno), the real sword with a flat magic circle round its hilt and a second blade (a leaf-headed spear with a cross) standing up behind the shoulder, white spiral ribbons on
  entry and the circle on the ground. Standard tier: crown, one gauntlet, the big wing and a small leaf blade.
- **C "Saint Spirit"** (my own idea, different from A and B): the spirit assimilated, drawn as a saint: wings of long feather blades fanned from the shoulder blades (four per side for Yuno, three standard), a
  tall crown of real 3D arrow bars (the standard tier keeps the pack's bar crown), pauldron plates of light on the shoulders (not a full armour) and a halo of seven small spirit arrows circling the body (four
  standard); no big sword, the arrows are the weapon, and in a cast one big arrow of light flies out of the hand. A faint Sylph silhouette from the geo model was not built (see "Not included").

Standard tier = the lighter version of the same design (fewer pieces, one weapon: a small leaf blade for A and B, the shorter halo for C), Yuno tier = full. The first dive (half stage) draws the left half
(left gauntlet, left wing, the half crown, half the halo), the upgrade forges the right half in, as in the other looks; the entry clock and ribbons are the REEL look's.

## Recommendation

**B "Clip-style" is the default.** The owner's complaint was that the previous result did not feel like the clips and that the weapons were flat strips; B is the design that matches the clips piece by piece
(cube crown, cube gauntlets, a floating sword with a flat circle round the hilt, a second blade behind, stacked-panel wing) and it reads at once at game distance. A is the one to pick if the anime stills matter
more than the clips (calmer, paler, thin crown); C is the most original and the heaviest on screen.

## The real 3D models (rule 7)

Authored by `tools/gen_sdw3_models.py` (deterministic; the loft / box / ring vocabulary of `tools/gen_weapon_models.py`), written to `assets/nusmp/sdw3_meshes/<piece>.txt` (`q <layer> 4 x (x y z u v) normal
r g b`; layers: 0 solid body, 1 soft glow shell drawn additive, 2 rings and ornaments) and the atlas `textures/aura/sdw3_skin.png` (+ the round `sdw3_soft.png` glow sprite). Face shade is baked, the solid
layers are drawn full bright through the emissive type, the glow layer additive, both through `SafeAdditive`. They are **worn aura pieces drawn by the dive painter for the diver** (shared mesh instances read by
`book/SpiritDiveWind3Mesh`, drawn by `client/aura/SpiritDiveWind3Draw`), not items; registering them as creative display items was optional and is not done.

| Piece | Vertices (solid / glow / ornaments) | What it is |
|---|---|---|
| sword | 488 / 72 / 384 = 944 | Spirit of Zephyr: 46 px blade (hexagonal section, bevels, fuller, point), cross-guard with up-curved quillons, wrapped grip, gem pommel, two crossed rings (ornaments) |
| bow | 320 / 96 / 96 = 512 | Spirit of Boreas: two leaf limbs (diamond section, widest in the middle), riser, cross ornaments, string |
| arrow | 48 / 32 / 176 = 256 | arrow of light: shaft, leaf head, barbs, fletching |
| spear | 80 / 32 / 232 = 344 | the second blade of B: shaft, leaf head, cross and ring |
| blade | 64 / 32 / 0 = 96 | small spirit blade: the leaf blade of the standard tier, the arrows of the halo |
| feather | 64 / 48 / 24 = 136 | the feather blade of the wings of C |
| crown | 576 / 0 / 128 = 704 | twelve 3D arrow bars on a ring (C, Yuno) |
| plate | 72 / 16 / 24 = 112 | the pauldron of C (three stacked lames) |
| cube, panel | 24 / 24 / 0 = 48 each | the glowing cube (B crown and gauntlets) and the leaf panel of the voxel wing |

## NEW / REPLACED / KEPT (rule 5)

- **NEW** (nothing like it existed; all added next to the old): the three designs and their painter `SpiritDiveWind3`, the mesh drawer `SpiritDiveWind3Draw`, the pure `SpiritDiveWind3Rules` /
  `SpiritDiveWind3Geometry` / `SpiritDiveWind3Mesh`, the self-test `magic.spirit_dive_wind3`, the models (`tools/gen_sdw3_models.py`, 10 meshes, `sdw3_skin.png`, `sdw3_soft.png`), the turntable and sheet tools, the scenes
  `spirit_dive_wind3.json`, the examples `SdwV3A/B/C`, the probe section `c.spiritdive.wind3`, the option values `A_ANIME`, `B_CLIP`, `C_SAINT`, the tests `SpiritDiveWind3Test`, `SpiritDiveWind3AssetTest`,
  `SpiritDiveWind3ScanTest`.
- **REPLACED (as the Wind a player sees by default; behind the option)**: the PACK look's flat strip sword, bow and arrow are replaced by real 3D models **in design A**; the default of `windLook` moves from
  `PACK` to `B_CLIP`. PACK (with its flat weapons), REEL, and every other look stay registered and choosable (`windLook = PACK` restores exactly the 3.4 Wind).
- **KEPT unchanged**: REEL, PACK, ANIME, DJINN, V2, CLASSIC, the Fire / Water / Earth dives, the server, the style number, tiers and entry clock, the first-person arms, Time Magic VFX, all textures `sdw_*`.
- Small edits to existing files: `SpiritDiveWind` (crown, wrists, leaves, ribbons, sparkles, `extras`, `firstPersonSelf` became package-private so A reuses them; one `real3d` flag swaps the weapons), `SpiritDiveReel`
  (one dispatch line), `SpiritDiveLook`, `DiveClientConfig`, `SpiritDiveWindRules` (enum values appended, `DEFAULT_LOOK`), `SpiritDiveBoot` (the self-test), `tools/render_preview/preview.py` (the meshes' folder on the
  harness class path), and the 3.4 tests that pinned "default PACK" / "values [PACK, REEL]" (now: default `B_CLIP`, values appended).

## Faults of the previous Wind look found against the clips and the pack

1. **Flat strips.** The sword, bow and arrow of the PACK look were single textured quads turned to face the camera: no thickness, no edge, no fuller, no guard, and from the side they vanished (seen in
   `docs/previews/spirit_dive_wind/wind_yuno.png`, side view). The clips show real voxel / 3D blades with thickness and glow. Fixed: real meshes (sword 944, bow 512, arrow 256 vertices).
2. **The crossed circles were one sprite.** Pack and clip have rings round the hilt; the old guard was a flat card. Now real torus-like rings (A, B) and, in B, the flat magic circle as the clip has it.
3. **The crown was a thin sprite fence**, the clip's crown is a ring of glowing cubes with taller bars and white lights (B), the pack's is arrow bars (A keeps it, C makes them real 3D bars).
4. **Leaf wings were two flat sheets.** The clips have a wing of stacked glowing panels; B builds it from real panels, C from real feather blades. A keeps the pack's smooth leaf on purpose (it is the anime-true design).
5. **No gauntlets** in the PACK look; the clips have glowing cubes on both forearms (B).
6. **The second blade** standing up behind him in the clips was missing (B has it). The bow of Boreas was only shown in a cast and flat; A now carries it on the back and in the hand as a real model.
7. The three looks above are chosen at runtime, so the owner can compare in game instead of in stills.

## Budget (rules 9 and 11): worst case per Wind diver

Vertices per frame of one diver (a vertex is a quad corner), counted by `SpiritDiveWind3Rules.vertices` from the real mesh files and summed by `SpiritDiveWind3Test` for every design, tier, level of detail and
stage. "Entry" is the worst of the 22 ticks of the transformation and of the exit (sprites of the entry added), "steady" the standing dive; every figure is under the caps **4000 (Yuno) / 2500 (standard)**.

| Design | Yuno FULL: entry worst / steady | standard FULL: entry worst / steady | MID (24-48 blocks) | FAR (48-64) | beyond 64 |
|---|---|---|---|---|---|
| A | 3868 (with the bow drawn in a cast, as counted) / 2596 | 1756 / 944 | 740 | 128 | 0 |
| B | 3980 / 2964 | 2172 / 1364 | 920 | 128 | 0 |
| C | 3672 / 2408 | 2140 / 1336 | 888 | 128 | 0 |

At most three character-tier dives per frame (`SpiritDiveFrame.admitCharacter`); every other Wind diver draws the standard tier, so a crowd of eight Wind divers costs at most 3 x Yuno + 5 x standard =
about 3 x 2.9k + 5 x 1.3k vertices in steady state. The character tier is not drawn beyond 24 blocks, the pieces are not drawn at MID except the small ones named in the table, FAR is the body light and the
ground ring only, beyond 64 blocks nothing (culled). Per piece per frame: sword 944, bow 512 (+ arrow 256 in a cast), spear 344, a halo arrow 68, a feather 64, a cube 24 (48 with glow). No allocation per
frame (static scratch matrices and arrays, shared mesh instances, cached textures and render types; pinned by `SpiritDiveWind3ScanTest`), no packet, no server work, no per-player state, no camera access,
timed in the probe section `c.spiritdive.wind3`. A client without the option never reaches these classes (the dispatch is one line in `SpiritDiveReel`).

## Tests (rule 12)

`SpiritDiveWind3Test` (the option and its append-only values, every mesh sane, the weapon budgets, level of detail order, caps at every stage, A is the PACK look away from the weapons, palette without blue,
geometry), `SpiritDiveWind3AssetTest` (the textures, every face of every mesh on an opaque texel, clean under alpha 0, no blue tint), `SpiritDiveWind3ScanTest` (server safety, no allocation, level of detail /
cap / probe, the older Winds still reachable, registered). Self-test `magic.spirit_dive_wind3` (files, meshes, budgets, option, geometry), registered in `SpiritDiveBoot`; **it has to be added to the smoke list in
`.github/workflows/smoke.yml`** (not touched by this builder). The 3.4 Wind tests, the other Spirit Dive tests (Reel, Anime, Djinn, Pieces, Tier, Remake), `ServerSafetyTest`, `ConfigFileNamesTest`,
`PerfSourceScanTest` and `ParticleThemesTest` still pass (223 + the new ones). No new source pattern, so `PerfSourceScanTest` is unchanged.

## Not included, not verified

- A faint **Sylph silhouette** from the geo model for C: not built (a geo model per diver is far over the budget; the feathers and arrows carry the idea).
- **Euros and Notos** still not built (only tiny manga panels in the pack).
- The bow and the big arrow show during **any** cast, not only Spirit of Boreas (the dive cannot know which page is cast without a packet). No cross-fade between the two weapons.
- Rings turn about the sword axis but the meshes do not animate otherwise; the pieces bob a little.
- **Never seen in a game**: software render, no bloom; the soft glow discs look pixelated in the harness and are smoother in the game's blend. Hand and arm positions follow the model in the game; the harness
  has hanging arms, so the gauntlets and the cast pose (bow in the left hand) must be checked with a raised arm. The first-person view hides the crown (as in the other looks).
- Worn pieces are VISUAL only; the engravings of rule 6 apply to weapons the player holds, not to aura pieces, so none are listed in `WeaponEngravings`.

## Decisions for the owner

1. Which design is the default: B (recommended) / A / C (one config line, `spiritDive.windLook`).
2. Should the sword and bow also become real creative-display items (the meshes are ready; then engravings would be assigned: severance, barrier_piercing, holy_weapon for Zephyr; magic_interference for Boreas)?
3. Should one design replace PACK and REEL for Wind for good (rule 2: nothing is removed until you say so)?
