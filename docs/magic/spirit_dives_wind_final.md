# Spirit Dive, Wind: design A is the Wind (real 3D crown layer, calm after the transformation)

The owner's decisions on the three Wind designs: "use Version A for both" (Yuno and basic users), "I do want the crown to be a 3D Render Layer floating", "not so heavy on the vfx after the transformation".
This builder ("windfinal") does exactly those three things; the two other decisions of the same message (the Zephyr sword and the Boreas bow as creative-display items, and the wearer merging with the spirit in
every Spirit Dive) are other builders' work and are not in here.

Previews (all renders of the repo's software harness `tools/render_preview`, **not game screenshots**: no bloom, additive passes approximated, no animation so the arms hang): `docs/previews/spirit_dive_wind_final/`.

## Read this first: what is built, how it reads, what is not included

**Built**
1. **A_ANIME is the default Wind**, both tiers. `spiritDive.windLook` now defaults to `A_ANIME` (`SpiritDiveWindRules.DEFAULT_LOOK`). PACK, REEL, B_CLIP and C_SAINT stay selectable (rule 2). A client that already has
   `windLook = "B_CLIP"` written in its `nusmp-spiritdive-client.toml` keeps B until the line is changed or deleted (a config default only applies to a fresh file).
2. **The crown is a real 3D render layer.** Four meshes (`assets/nusmp/sdw4_meshes/crown_yuno|crown_std|crown_yuno_half|crown_std_half.txt`, made by `tools/gen_sdw4_models.py`, same quad-line format and same loader as
   `sdw3_meshes`, same atlas `aura/sdw3_skin`): a thin ring with tall arrow-tip bars of uneven height (Yuno: **14 bars** and a **second, lower ring**; standard: **8 bars**, one ring), each bar a triangular shaft
   with an arrow head wider than the shaft, a glow shell (a haze band on the ring and two crossed haze planes a bar). It floats **12.6 px (about 4 px, a hand's width, above the hat layer)** above the head, **turns
   slowly** (0.021 rad a tick, one revolution in about 15 s), bobs 0.45 px, **follows the head's yaw fully and its pitch by half** (so it floats instead of wobbling with every nod; no per-player state needed),
   rises and brightens as it ignites in the entry. The first dive of Wind (left half) draws the half meshes and does not turn them; the upgrade lights the whole crown. Hidden in first person as before
   (the existing rule). Not hidden by a helmet: the crown floats above the head and the helmet does not reach it (the old sprite crown did not hide either). At 24-48 blocks (mid detail) the cheap sprite crown of the
   pack look is still drawn, beyond 48 blocks nothing.
3. **Calm steady state** (`SpiritDiveWind4Rules`, client option `spiritDive.calm`, default true, false = the 3.5 density). After the 22-tick entry the loud effects fade out over 12 ticks (a smooth fade, no pop) and
   a quiet set stays. They come back in full for the entry, the exit (as the exit clock runs back) and for 2 s after a cast (40 ticks held, 20 ticks of fade), then fade again.

| | loud (entry, exit, cast, 2 s after) | quiet steady state |
|---|---|---|
| body light (shell), leaves with the cross, 3D crown, sword at the hip (blade, guard, grip, glow), bow on the back | yes | **kept** |
| wrist circle | full alpha, plane ring + facing sprite | **kept** as the plane ring only, alpha 0.5 +- 0.1 breathing (a low pulse) |
| steady ribbons of wind (3 for Yuno, 2 standard) | yes | **removed** (-100 percent) |
| four-point sparkles | 8 (Yuno), 4 | **2 / 1** (-75 percent) |
| circle of crosses behind the back (Yuno) | yes, turning | **removed** |
| the sword's two crossed rings (384 vertices) | yes | **removed** (come back for 2 s after a cast) |
| ground ring | alpha 0.8 | alpha 0.32 |
| crown glow | full | 78 percent (a little less) |

**Not included**: the sword and bow as creative-display items (builder 3), merging with the spirit (builder 2), any change to the sword, bow or arrow meshes (their look is unchanged; only the sword's rings
fade in the quiet state), any change to B, C, PACK, REEL, to the entry / exit effects, to the Time Magic VFX, version / changelog / `.github` (the smoke list entry `magic.spirit_dive_windfinal` still has to be added
to `.github/workflows/smoke.yml` by whoever owns it).

## NEW / REPLACED / KEPT (rule 5)

- **NEW**: `SpiritDiveWind4Rules` (pure), `SpiritDiveWind4SelfTests` (`magic.spirit_dive_windfinal`), the painter `client/aura/SpiritDiveWind4`, the four crown meshes + `tools/gen_sdw4_models.py`, the client option
  `spiritDive.calm`, `SpiritDiveLook.calm()`, `SpiritDiveWind3Mesh.getIn` (a second mesh folder through the same loader), scenes / sheets / examples (`spirit_dive_wind_final*.py`, `scenes.d/spirit_dive_wind_final.json`,
  `SdwV4A`, `SdwV4Loud`), the tests `SpiritDiveWind4Test`, `SpiritDiveWind4ScanTest`.
- **REPLACED** (for design A at full detail only, behind the option): the **sprite crown** of the pack look (a flat fence of bar sprites on a ring band) by the 3D crown; the **default** of `windLook` (B_CLIP -> A_ANIME);
  the always-on loud effects of A by the calm set (`calm = false` restores them). The sprite crown code stays (PACK, REEL-less mid distance, `calm`-independent).
- **KEPT**: PACK, REEL, B_CLIP, C_SAINT (selectable, unchanged), the weapons' meshes, the entry / exit, tiers, the server, the other elements' dives, Time Magic VFX.
- Small edits to existing files: `SpiritDiveWind` (energy, crown dispatch, the `e` factor on wrists / back circle / ribbons / sparkles / ground ring), `SpiritDiveWind3` (`weaponsA` takes the energy, the sword rings
  fade), `SpiritDiveWind3Rules` (A's vertex count swaps the crown, `cap(Ver, tier)`, `CAP_CHARACTER_A`, `swordVertsCalm`), `SpiritDiveWind3Mesh` (folder parameter), `SpiritDiveWindRules.DEFAULT_LOOK`,
  `DiveClientConfig`, `SpiritDiveLook`, `SpiritDiveBoot` (self-test), and the tests that pinned "default B_CLIP" (`SpiritDiveWindTest`, `SpiritDiveWindScanTest` text, `SpiritDiveWindSelfTests`).

## Budget (rules 9 and 11), worst case per Wind A diver

Vertices of one diver per frame (a vertex is a quad corner), counted by `SpiritDiveWind3Rules.vertices` / `SpiritDiveWind4Rules.steadyVertices` from the real mesh files and checked against the harness
(`age 50`: 2632 / 1188 measured, formula 2624 / 1184).

| | Yuno FULL | standard FULL | MID (24-48 blocks) | FAR (48-64) | beyond 64 |
|---|---|---|---|---|---|
| entry / exit / cast worst (3.5 -> now) | 3868 -> **4468** (cap 4600 for A, 4000 for B and C) | 1756 -> **2108** (cap 2500) | 740 (unchanged) | 128 | 0 |
| steady state, **before** (3.5, measured) | **2596** | **944** | | | |
| steady state, loud (`calm = false`, 3D crown) | 3192 | 1296 | | | |
| steady state, **quiet (default)** | **2624** | **1184** | 740 | 128 | 0 |

Why the quiet Yuno number equals the old one although the crown grew from 88 to 688 vertices: the quiet set removes 568 vertices of loud effects (the sword's rings 384, ribbons 144, sparkles 24, circle behind the
back 8, wrist sprites 8). The standard tier gains 240 vertices (crown 400 instead of 48, ribbons 96, sparkles 12 and the wrist sprite removed); it has the smallest budget anyway. Vertices are not the whole story:
what the owner sees as "heavy" is the long translucent ribbons, the rings and the sparkles, and those are gone in the steady state (see `calm_vs_loud_*.png`).

The 3D crown costs: Yuno 688 (solid 432, glow 160, lower ring 96), standard 400 (solid 288, glow 112), left halves 344 / 232. At most three character-tier dives per frame (`SpiritDiveFrame.admitCharacter`),
so eight Wind A divers in steady state cost at most 3 x 2.6k + 5 x 1.2k = about 14k vertices (before: 3 x 2.6k + 5 x 0.9k = 12.3k), the entry spike lasts 22 ticks per diver. The 3D crown is drawn at full
detail only (under 24 blocks); first person self: not drawn. No allocation per frame (static scratch quaternions and arrays, meshes resolved once in static fields, a 16-slot array cast clock, pinned by
`SpiritDiveWind4ScanTest`), no packet, no server work, no config read per frame (a volatile flag), timed in the existing probe section `c.spiritdive.wind`. Server side: nothing changed.

## Tests (rule 12)

`SpiritDiveWind4Test` (default A_ANIME and the older looks parse, every crown mesh loads / finite / on opaque texels / green-white, 14 + 8 bars and the rings, halves are left halves, energy in the entry, steady
state, after a cast, exit, always 0..1 and smooth, the quiet set cuts the loud effects by at least 70 percent, budgets under the caps at every tier, stage and detail, quiet <= loud <= entry worst, the 3.5 numbers
reproduced, the cast clock), `SpiritDiveWind4ScanTest` (server safety, no allocation per frame, meshes resolved once, the wiring is design A only, the option is a client option), self-test
`magic.spirit_dive_windfinal` registered in `SpiritDiveBoot` (**to be added to the smoke list in `.github/workflows/smoke.yml`**). Updated: the 3.5 tests that pinned B_CLIP as the default. The sweep (all
`SpiritDive*` and `FuegoleonArm*` tests, `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest`, `ParticleThemesTest`) passes.

## Honest caveats

- Never seen in a game: software render, no bloom, no arm animation (the wrist circle on a raised arm and the cast pose were not seen), the glow discs are coarser than the game's blend.
- The pitch follow of the crown (half the head's pitch) and the spin speed are numbers chosen by eye from stills; they are constants in `SpiritDiveWind4Rules` (`PITCH_FOLLOW`, `SPIN`, `BOB`).
- "Hidden under helmets": the crown is not hidden by a helmet (it floats 4 px above the head and does not intersect it); first person hides it. Say if a helmet should hide it.
- The cast detection is the existing one-shot clip flag (`DiveCast`): any page's cast, not only a Wind page, makes the diver loud for 2 s; a cast of a player not in view is not seen.
- The 3.5 vertex numbers for Yuno / standard come from the harness at t = 50; the formula reproduces them within 4 vertices.
- Players with `windLook = "B_CLIP"` already in their client toml keep B (see above).

## Decisions for the owner

1. Is 2 s of loud effects after a cast the right length (`CAST_HOLD` 40 ticks + 20 fade), and should the sword's rings really be loud-only (they are the biggest cut)?
2. Should B_CLIP and C_SAINT eventually leave the option (rule 2: nothing is removed until you say so)?
