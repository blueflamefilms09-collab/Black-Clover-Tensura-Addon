# Visual upgrade, slice `god_melee_b` (release 1.102.0): the God melee weapons of the rift pockets and Paradox

Owner request: "Upgrade God Grade weapons/relics to look 5x better". "5x better looking" cannot be measured, so this page records **measured proxies** (before and after, from the shipped
files) and says plainly what they do and do not show. **Nobody has seen these weapons in a game**: nothing here could be run (no Gradle, no game); the numbers are read from the files
and the pictures in `docs/previews/visual_upgrade/god_melee_b/` come from the project's software rasteriser, not from the game's renderer.

The slice: the eight God weapons of the 1.99.0 rift-pocket bosses (`aurliels_seraph_halberd`, `chronarchs_pendulum_sabre`, `deepwatch_harpoon`, `orials_starfall_staff`,
`reflected_kings_mirrorblade`, `rimeheart_greatblade`, `sanguine_wardens_cleaver`, `valkarins_stormlance`; Wave78) and the Paradox (`paradox`, Wave30). The twelve God weapons of Wave71 belong to
the slice `god_melee_a`, the Genesis weapons to `genesis_weapons`; none of their files were touched.

## 1. NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | for each of the nine weapons: an upgraded mesh `weapon_meshes/up/<id>.txt`, its level-of-detail mesh `up/<id>_lod.txt`, the mesh of its one orbiting piece `up/<id>_orbit.txt`; skins `textures/entity/weapon/up/<id>.png` (1024 px, Paradox 1536 px), `_glow.png` (core glow, additive) and `_glow2.png` (energy veins, a separate emissive map); the effect table `assets/nusmp/visual_upgrade/<id>.json` |
| **NEW** | client code: `client/GodMeleeBRender` (the extra passes), `client/vupgrade/godb/{UpgradeSpec, UpgradeMath, UpgradeSpecs, UpgradeConfig, UpgradeFx, GodBUpgrade}`; common `vupgrade/GodBSelfTests` (self-test `visual.god_melee_b`) |
| **NEW** | client option file `config/nusmp-visualupgrade-god_melee_b-client.toml`: `visual.upgrade` (default **true**; false draws every one of the nine exactly as before), `visual.guiModel` (default false: the sprite stays the inventory icon; true shows the 3D model, tilted, in the slot), `visual.particles` |
| **NEW** | generators `tools/visual_upgrade/god_melee_b/{lib, kit, items_a, items_b, build, preview, measure}.py`, the measurements `docs/visual_upgrade/god_melee_b_baseline.json` and `_after.json`, the before / after sheets in `docs/previews/visual_upgrade/god_melee_b/`, JUnit `UpgradeMathTest`, `UpgradeSpecTest`, `GodMeleeBBudgetTest`, `GodMeleeBRatioTest` |
| **REPLACED** | nothing was removed. In the hand, on the ground, in frames and on mobs the nine weapons are drawn from the NEW meshes instead of the old ones while `visual.upgrade` is on (that is the only replacement, and it is a switch). The idea is the same weapon with more on it: the upgraded mesh is the **original builder run again** (`tools/rift/pocket_weapon_models.py`, `tools/relics/wave30_m_paradox.py`) with extra parts added before the UV layout, so every old part, proportion and the placement in the hand (the `place` line is copied) are the old ones |
| **KEPT** | the old meshes, skins, glow skins, 16 x 16 sprites (still the inventory icons), item models and display presets (`models/item/<id>.json`, `_icon.json`), item ids, stats, acts, passives, engravings (`item/WeaponEngravings`, Wave78 / Wave30 definitions), Better Combat files, `weapon_classes.json`, `weapon_fixes.json`; the VFX shapes of every act; the Time Magic VFX (`TimeMagicLayer`, `time_*` textures, `gen_time_vfx_textures.py`, the Time calls of `TimeBook`) were not read or edited |

Rule 6 (engravings) and rule 7 (3D model, old sprite as icon, glow / effects) hold: no weapon was added or reworked in stats; the models are real 3D, the sprite is the icon, the new glow and effects are the ones listed below. Rule 4 (version bump) was **not** applied: the brief of this release forbids touching the version files.

## 2. Measured proxies (before and after)

The visual score is `V = sum(weight x after / before)` over five components, so `V_before = 1` by construction. The weights are mine (the brief leaves them open) and are held in
`tools/visual_upgrade/god_melee_b/measure.py`: **mesh 0.20, texture 0.20, passes 0.15, ambient layers 0.25, idle animations 0.20**. The mesh gets the low weight because the performance
guard caps it (section 4); the layers that cost a few vertices or a few particles carry more. The brief's own example weights (mesh .35, texture .2, glow .15, ambient .2, animation .1) give the
last column, which is under 5 for five of the nine. Both are reported; neither says how it looks.

Components, as measured: **mesh** = quads at full detail (body + void layer, plus every instance of the orbiting piece); **texture** = painted texels (alpha > 0) of the skin and of every emissive map;
**passes** = draw passes (before: body, core glow; after: body, core glow, vein wave, halo shell, orbiting pieces); **ambient layers** = effect layers running while the weapon is held (before: the grade
shimmer of `RelicGradeFx` only; after: that, the seven particle layers and the orbiting pieces); **idle animations** = channels that move on their own (before: the glow pulse only; after: glow pulse,
core flicker, vein wave flow, halo breath, orbit revolve, orbit piece spin, orbit bob).

| item | quads before -> after (full detail, orbit pieces included) | welded vertices after | LOD quads | painted texels before -> after, all maps | skin px | passes | ambient layers | idle animations | V (our weights) | V (brief's example weights) |
|---|---|---|---|---|---|---|---|---|---|---|
| aurliels_seraph_halberd | 1512 -> 2626 | 2287 | 321 | 87886 -> 601586 (x6.8) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 5.74 | 4.85 |
| chronarchs_pendulum_sabre | 1351 -> 2533 | 2018 | 316 | 136036 -> 1102288 (x8.1) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 6.02 | 5.15 |
| deepwatch_harpoon | 1492 -> 2412 | 2113 | 306 | 60047 -> 485708 (x8.1) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 5.97 | 5.06 |
| orials_starfall_staff | 858 -> 2232 | 1690 | 267 | 56448 -> 643586 (x11.4) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 6.83 | 6.07 |
| reflected_kings_mirrorblade | 774 -> 2266 | 1868 | 283 | 106379 -> 823640 (x7.7) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 6.16 | 5.45 |
| rimeheart_greatblade | 904 -> 2564 | 2079 | 331 | 192431 -> 1287535 (x6.7) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 5.93 | 5.21 |
| sanguine_wardens_cleaver | 791 -> 2183 | 2017 | 287 | 215014 -> 1244620 (x5.8) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 5.74 | 5.00 |
| valkarins_stormlance | 1616 -> 2522 | 2375 | 334 | 36296 -> 240150 (x6.6) | 512 -> 1024 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 5.66 | 4.75 |
| paradox | 2028 -> 2590 | 2210 | 330 | 232707 -> 1526148 (x6.6) | 512 -> 1536 | 2 -> 5 | 1 -> 9 | 1 -> 7 | 5.59 | 4.63 |

`quads before -> after` is **not** 5x for any item and cannot be (section 4). Texture is at least 4x everywhere (the skins are 4x the area; the new parts add islands). The brief's "ambient layers >= 3,
an idle animation, a separate emissive glow map, a swing trail" targets for God are all met by construction (section 3), the mesh floor of 600 quads too.

## 3. What was built, per item

All nine share the same effect set (data in `assets/nusmp/visual_upgrade/<id>.json`, drawn by `GodMeleeBRender`, ticked by `UpgradeFx`); the colours, the speeds, the particle types and the pieces are each weapon's own.

**Shared effect set**
* **Core glow** (the old pass): now breathes with a second slow flicker (`core_flicker`).
* **Vein pass** (new, `RenderType.entityTranslucentEmissive` of `<id>_glow2.png`): the second emissive map holds the engraved lines, ribs, rings and gems; the per-vertex brightness is a wave running up the weapon (`UpgradeMath.wave`, one table of 128 floats per frame).
* **Halo shell** (new): the level-of-detail glow quads scaled 5 to 9% about the axis, translucent, breathing in alpha and scale.
* **Orbiting pieces** (new): 4 to 7 copies of one small piece on a circle round the weapon axis, revolving, spinning and bobbing; drawn body first, glow second (one render type at a time).
* **Swing trail** (new): while the holder swings, 3 to 4 ghost copies of the glow outline lag behind the blade (turned about the grip by a growing angle), fading.
* **Seven particle layers** (new, `UpgradeFx`): `motes` (idle drift off the blade), `halo_ring` (a ring of light turning round the head), `edge_line` (sparks running along the blade: the energy line), `ground_cast` (a ring of light at the feet), `wrist_flare` (the hand), `light_shaft` (rising off the tip), `accent_flare` (rare burst).
* **Level of detail** (new): the full mesh up to 12 blocks, `<id>_lod` (at most 14% of the quads, vertex clustering of the full mesh) from 12 blocks, no additive pass of the upgrade from 24 blocks (the core glow pass is skipped too), no particles beyond 32 blocks, half rate beyond 12.
* **Skin detail** (new, for every part old and new): fine grain, an ambient-occlusion band and a bevel rim along every UV island, hairline scratches (`lib.detail_pass`), plus each weapon's own engravings (etched into the skin and lit on the maps).

**Per weapon: the geometry and engravings added to the old model**

| Weapon | New parts (all named parts of the mesh) | Orbiting piece, palette of the effects |
|---|---|---|
| Rimeheart Greatblade | 18 raised frost chevrons (two faces), 18 + 14 frost shards and icicles along spine and edge, a heart halo ring and a cluster of six gems round the heart crystal, 12 guard gems, three frost bands on the grip, six pommel gems, spine runes; winding frost-lace etched and lit down both faces | ice shard with a companion; snowflakes, pale cyan |
| Deepwatch Harpoon | barb teeth along the head's edge, a coral crown at the socket, two kelp ribbons, three scale bands, four rising bubble pearls, anchor flukes; wave-light bands on the shaft, lit vines on the head | bubble; bubbles and nautilus motes, teal |
| Sanguine Warden's Cleaver | a chain hanging from the skull pommel, 14 spine rivets, 10 tally marks, three branching blood vessels, 15 edge serrations, bone spurs on the grip, a raised border ridge round both faces; the hole's rim and the edge lit | blood droplet; crimson dust and spores, red |
| Valkarin's Stormlance | a counter-wound lightning coil, two wind pennants, three tail fins, three rings on the head, a halo ring round the storm gem, ten storm runes; forked lightning scratched on the head | small bolt; electric sparks, storm blue |
| Oriel's Starfall Staff | nine star studs and a constellation thread winding up the shaft, two armillary rings round the falling star, nine thorns on the cradle, four inlay bands, a comet tail of three ribbons, a star-shaped butt | four-pointed star; end-rod motes and enchant glyphs, violet and gold |
| Chronarch's Pendulum Sabre | twelve hour numerals on the clock face, three cogs on the blade, a spring round the pendulum rod, fins on the weight, ten spine ticks, escapement arms; engraved scale ticks and a pendulum arc | cog; gold motes, brass |
| Reflected King's Mirrorblade | eight raised mirror panels, a filigree vine down both faces, frame curls and corner gems, a seven-point guard crown, two echo rings round the blade, twelve grip gems, five pommel shards, two guard panes | mirror pane; white motes and electric sparks, silver |
| Auriel's Seraph Halberd | 30 barbs on the wing feathers, a ring of twelve beads on the halo, ten sun rays round the sun gem, three cloth streamers, three filigree bands, a crown ring above the point | white-gold feather; end-rod motes, a totem flare, gold |
| Paradox | a horizontal time ring with eight ticks round the crown, eight sand shards, a thorn vine round the lower shaft; violet light running up the shaft (no Time Magic file touched) | sand glass; enchant glyphs and portal wisps, violet and green |

## 4. Decisions the brief did not settle (honest notes)

1. **The brief's 5x quads cannot meet its own 2500-vertex God cap.** The old meshes already have 774 to 2028 quads (up to 2704 in the old Paradox), which is 3000 to 11000 emitted vertices; "5x the quads"
   would be 4000 to 14000 quads and 16 000 to 56 000 vertices per pass of one hand item. I read the cap as **welded vertices of the full-detail mesh including its orbiting pieces** (the figure a modeller sees
   and the one the brief's own floor of 600 quads matches), kept every weapon at or under 2500 (`GodMeleeBBudgetTest`), and let the mesh component reach 1.3x to 2.9x. The quad ratio is reported, not claimed.
2. **Painted texels are measured as `alpha > 0` over all maps.** The old skins were 512 px, 12% to 89% painted; the new ones are 1024 px (Paradox 1536 px, because its old skin was 89% painted and 4x needs more
   than a 1024 px skin can give). Video memory: a 1024 px RGBA skin is 4 MB, the three maps of one weapon 12 MB, Paradox 28 MB; a skin is uploaded only when a weapon is first drawn.
3. **The hand weapons only.** No armour, no worn relic, no Genesis item was touched.
4. **Cut: the 36 dark Djinn weapons** (`djinn_weapon_<name>_dark`, grade God in `WeaponGrades.djinn(kind, true)`, main hand, loaned by the Djinn equip, meshes `djinn_weapon_*_dark`) are God-grade main-hand
   weapons that nobody in this release upgrades; they were left out for time and go to a follow-up (their meshes come from `tools/relics/djinn_weapons.py` / `djinn36_*.py`). No other God-grade main-hand weapon exists
   outside the Wave71 slice (`god_melee_a`) and this one (checked in every `RelicDef.of(..., Grade.GOD, RelicSlot.MAIN_HAND)` and in `WeaponGrades`).
5. **A held weapon's own particles are placed from the holder's body yaw** (hand = a fixed offset from the body, the blade = forward and up), not from the real hand bone: close, never exact.

## 5. Where the code touches existing files (all additions, listed so a merge stays trivial)

* `client/WeaponRenderer.java` (nine one-line hooks, no existing behaviour changed with `visual.upgrade` off): `meshId` ends with `return GodMeleeBRender.meshFor(id)`; at the top of `renderByItem` the inventory hook
  `if (ctx == GUI && GodMeleeBRender.gui(...)) return`; after the mesh is resolved `upTier = GodMeleeBRender.tier(...)` and the level-of-detail swap; the glow condition gets `&& upTier < TIER_FAR` and the pulse is
  multiplied by `GodMeleeBRender.flicker(...)` (1 for everything that is not upgraded); before the final `popPose` the call `GodMeleeBRender.extras(...)`; `emit` became package-private; the skin regex of `skin()`
  gained `|lod|orbit`.
* `NUSMP.java`: `GodBUpgrade.init(modEventBus, modContainer)` (client section, next to `PerfClient.init`) and `GodBSelfTests.register()` (next to `AntiMagicSelfTests`).
* `tools/rift/build_pocket_weapons.py`: one line at the end of `main` that runs `tools/visual_upgrade/god_melee_b/build.py` for the same ids (Paradox: `python3 -B tools/visual_upgrade/god_melee_b/build.py paradox`).
* Self-test prefix: `visual.god_melee_b`. Lang fragment: none (no player-visible text; the config comments are English only, like the other option files).

## 6. How to rebuild and check

```
python3 -B tools/visual_upgrade/god_melee_b/build.py [id ...]       the models, skins, tables (deterministic: a second run changes no byte)
python3 -B tools/visual_upgrade/god_melee_b/preview.py [id ...]     the before / after sheets
python3 -B tools/visual_upgrade/god_melee_b/measure.py after        docs/visual_upgrade/god_melee_b_after.json (and `table` for the markdown above)
```
JUnit: `UpgradeMathTest`, `UpgradeSpecTest`, `GodMeleeBBudgetTest`, `GodMeleeBRatioTest` (and the existing `WeaponMeshesTest`, `WeaponClassesTest`, `WeaponAttributesTest`, `BowDisplayGuardTest`,
`RelicRenderCoverageTest`, `RelicHandCoverageTest`, `RelicUniqueLooksTest`, `GeoAssetsTest` still pass). `WeaponMeshesTest` skips the new meshes through `orient manual`; the new files sit in sub-folders
(`weapon_meshes/up/`, `textures/entity/weapon/up/`) so no existing listing sees them.

## 7. What could NOT be verified

* Nothing was run in the game. The Java compiles against the project's API stubs (no error in the new or edited files; a deliberate wrong call was flagged by the same check), the pure logic is tested.
* How the vein wave, the halo shell, the orbiting pieces and the swing ghosts look and how fast they move is untuned. The direction the swing ghosts lag in follows the item's own axes and may be the wrong way round for some swings.
* The additive and translucent passes of the new look depend on the render types behaving as they do for the existing Elsdocia halo and the socket pass (the same calls are used); not seen.
* The particle positions (section 4, note 5) and their density; the level-of-detail meshes (vertex clustering) were only looked at in the software preview.
* `visual.guiModel` (the tilted model in the inventory slot) was written from the item-space maths of the hand renderer and never drawn.
* Frame-time: the cost model is by construction (one extra pass over at most 700 glow quads for the veins, 330 level-of-detail quads for the halo and the ghosts, at most 12 small pieces), not measured; `/nusmp perf` was not run.
