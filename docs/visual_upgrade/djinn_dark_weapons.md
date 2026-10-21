# Visual upgrade, slice `djinn_dark_weapons` (release 1.108.0): the 36 dark Djinn weapons

Owner request: "Upgrade God Grade weapons/relics to look 5x better". "5x better looking" cannot be measured, so this page records **measured proxies** (before and after, read from the
shipped files) and says plainly what they do and do not show. **Nobody has seen these weapons in a game**: nothing here could be run (no Gradle, no game); the numbers come from the files, the
pictures in `docs/previews/visual_upgrade/djinn_dark_weapons/` from the project's software rasteriser, not from the game's renderer.

The slice: the 36 **dark** Djinn weapons `djinn_weapon_<name>_dark` (God grade, `WeaponGrades.djinn(kind, true)`; the light twins are Legend grade and are not touched). They were left out of the
two earlier God slices (`god_melee_a`: the twelve God weapons of Wave 71; `god_melee_b`: the rift pocket weapons and the Paradox) as "cut: the 36 dark Djinn weapons"
(`docs/visual_upgrade/god_melee_b.md`, section 4, note 4). A dark Djinn weapon is the Djinn Equip's loaned main-hand weapon (`DjinnWeaponItem`, loan rules in `docs/djinn_weapons.md`).

## 1. NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | for each of the 36: an upgraded mesh `weapon_meshes/up/<id>.txt`, its level-of-detail mesh `up/<id>_lod.txt`, the mesh of its one orbiting piece `up/<id>_orbit.txt`; skins `textures/entity/weapon/up/<id>.png` (1024 px), `_glow.png` (core glow, additive) and `_glow2.png` (energy veins, a separate emissive map); the effect table `assets/nusmp/visual_upgrade/<id>.json` (36 files) |
| **NEW** | generators `tools/visual_upgrade/djinn_dark_weapons/{frame, dlib, dkit, skin_glyphs, ditems, build, preview, measure, make_doc}.py`; the measurements `docs/visual_upgrade/djinn_dark_weapons_baseline.json` (taken before anything was changed, first commit of the slice) and `_after.json`; the before / after sheets `docs/previews/visual_upgrade/djinn_dark_weapons/<name>.png` |
| **NEW** | `vupgrade/DjinnDarkSelfTests` (self-test `visual.djinn_dark_weapons`), the pure `relics/upgrade/UpgradeParticleNames`, JUnit `DjinnDarkBudgetTest`, `DjinnDarkRatioTest`, `UpgradeParticleNamesTest`, `UpgradeFxPurgeScopeTest`; the lang fragment `tools/lang/djinn_dark_weapons.json` (one tooltip line) |
| **REPLACED** | nothing was removed. In the hand, on the ground, in frames and on mobs a dark Djinn weapon is drawn from the NEW mesh instead of the old one while the client option `visual.upgrade` is on (that is the only replacement, and it is a switch). The upgraded mesh keeps **every old quad** (position, normal, shape: a test checks it) and adds to it, so the silhouette, the size and the placement in the hand (`place` line copied) are the old ones |
| **KEPT** | the old meshes, skins, glow skins and 16 x 16 sprites (still the inventory icons), item models and display presets, item ids, stats, acts, passives, engravings (`DjinnWeaponRules`, `item/WeaponEngravings` rule 6 holds: no weapon was added or reworked in stats), the Djinn Equip, the loan, the Better Combat files; the 36 light weapons; the Time Magic VFX (`TimeMagicLayer`, `time_*` textures, `gen_time_vfx_textures.py`, the Time calls of `TimeBook`) were not read or edited |
| **REPLACED (follow-up)** | `client/relics/fx/UpgradeFxClient` and `relics/upgrade/UpgradeActFx` built vanilla coloured `DustParticleOptions`; they now spawn the mod's own custom particles (section 4) and their two exclusion lines are gone from `docs/rukh/scope.txt` |

Rule 6 (engravings) and rule 7 (3D model, old sprite as the icon, glow / effects) hold. Rule 4 (version bump) was **not** applied: the brief of this release forbids touching the version files.

## 2. Measured proxies (before and after)

The visual score is `V = sum(weight x after / max(1, before))` over five components, so `V_before = 1` by construction. The weights are those of `god_melee_b` (the numbers of the two slices
are comparable): **mesh 0.20, texture 0.20, passes 0.15, ambient layers 0.25, idle animations 0.20**; the brief's own example weights (mesh .35, texture .2, glow .15, ambient .2, animation .1)
are reported next to them. Components, as measured: **mesh** = quads at full detail (body + void layer, plus every instance of the orbiting piece); **texture** = painted texels (alpha > 0) of
the skin and of every emissive map; **passes** = draw passes (before: body, core glow; after: body, core glow, vein wave, halo shell, orbiting pieces); **ambient layers** = effect layers that run
while the weapon is held; **idle animations** = channels that move on their own.

**Baseline conventions, said plainly.** A dark Djinn weapon is a `DjinnWeaponItem`, not a relic definition, so it never had the grade shimmer of `client/relics/RelicGradeFx` (that looks at
relic definitions only): its baseline ambient layer count is **0** and the ratio divides by max(1, 0), so that component is the plain count of 8 layers. Its one idle animation was the glow pulse
(`VariantRules.pulse`). **Two of the five components (ambient 8.0, animation 7.0, together 3.4 of the V) are therefore cheap counts that start from 0 or 1, and V is not a measure of how the
weapon looks.** The two ratios that mean something are the mesh and the texture ones.

| measure | min | mean | max |
|---|---|---|---|
| mesh ratio (quads at full detail, orbiting pieces included) | 5.00 | 5.44 | 10.18 |
| painted-texel ratio (all maps) | 9.4 | 23.0 | 29.5 |
| V (our weights) | 6.68 | 9.45 | 10.74 |
| V (brief's example weights) | 6.36 | 9.17 | 11.06 |
| welded vertices at full detail (body + 6 orbiting pieces; cap 2500) | 851 | | 2228 |
| quads before | 168 | | 469 |
| quads after (full detail) | 963 | | 2425 |
| level-of-detail quads | 125 | | 321 |

Items whose mesh ratio is under 5.0: none.

| item | kind | quads before -> after (orbit pieces included) | welded after | LOD quads | painted texels before -> after, all maps | passes | ambient | idle anim | V (ours) | V (brief) |
|---|---|---|---|---|---|---|---|---|---|---|
| baal | lance | 239 -> 1211 | 965 | 149 | 33673 -> 785651 (x23.3) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.46 | 9.12 |
| amon | greatsword | 308 -> 1628 | 1419 | 214 | 65098 -> 1190667 (x18.3) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 8.49 | 8.18 |
| vinea | trident | 388 -> 2072 | 1721 | 256 | 34995 -> 1032175 (x29.5) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.74 | 10.44 |
| focalor | glaive | 307 -> 1537 | 1284 | 195 | 31585 -> 891188 (x28.2) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.42 | 10.07 |
| zagan | staff | 381 -> 1907 | 1676 | 250 | 41856 -> 985879 (x23.6) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.49 | 9.14 |
| zepar | axe | 231 -> 1255 | 1029 | 157 | 68035 -> 1093356 (x16.1) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 8.08 | 7.79 |
| furfur | horn | 168 -> 1710 | 1135 | 170 | 34543 -> 833591 (x24.1) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.64 | 11.06 |
| marchosias | claws | 185 -> 963 | 809 | 125 | 49862 -> 1219277 (x24.5) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.71 | 9.39 |
| sitri | bow | 257 -> 1383 | 1058 | 170 | 31867 -> 730957 (x22.9) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.44 | 9.15 |
| bifron | glaive | 213 -> 1201 | 972 | 147 | 31299 -> 837471 (x26.8) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.25 | 10.00 |
| paimon | glaive | 212 -> 1208 | 989 | 151 | 37363 -> 1075315 (x28.8) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.67 | 10.43 |
| vassago | scythe | 247 -> 1449 | 1118 | 166 | 36958 -> 938297 (x25.4) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.03 | 9.81 |
| bael | claymore | 334 -> 1673 | 1402 | 213 | 60288 -> 978504 (x16.2) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 8.02 | 7.67 |
| agares | harpoon | 372 -> 1866 | 1661 | 248 | 37956 -> 1076441 (x28.4) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.45 | 10.10 |
| valefor | daggers | 326 -> 1632 | 1360 | 207 | 47775 -> 984519 (x20.6) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 8.90 | 8.55 |
| barbatos | bow | 341 -> 1712 | 1369 | 221 | 36705 -> 909802 (x24.8) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.74 | 9.39 |
| gusion | rapier | 331 -> 1656 | 1411 | 218 | 36986 -> 1091832 (x29.5) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.68 | 10.33 |
| vepar | anchor | 285 -> 1573 | 1220 | 184 | 44912 -> 959061 (x21.4) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.15 | 8.88 |
| shax | fans | 469 -> 2425 | 2120 | 321 | 80728 -> 756657 (x9.4) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 6.68 | 6.36 |
| buer | staff | 330 -> 1652 | 1364 | 213 | 40970 -> 1026345 (x25.1) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.79 | 9.44 |
| andrealphus | lance | 437 -> 2207 | 1886 | 287 | 39944 -> 995080 (x24.9) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.77 | 9.43 |
| kimaris | lance | 254 -> 1524 | 1041 | 158 | 33936 -> 908143 (x26.8) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.33 | 10.13 |
| amdusias | rapier | 325 -> 1813 | 1261 | 186 | 39655 -> 905649 (x22.8) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.46 | 9.20 |
| belial | greatsword | 262 -> 1322 | 1161 | 175 | 82110 -> 1191249 (x14.5) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 7.69 | 7.34 |
| decarabia | pincers | 282 -> 1494 | 1270 | 195 | 35292 -> 989535 (x28.0) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.44 | 10.14 |
| seere | sabre | 257 -> 1288 | 1085 | 162 | 29929 -> 866624 (x29.0) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.57 | 10.22 |
| dantalion | staff | 247 -> 1285 | 1009 | 159 | 56270 -> 1085763 (x19.3) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 8.68 | 8.36 |
| andromalius | spear | 248 -> 1286 | 1035 | 159 | 42626 -> 976656 (x22.9) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.39 | 9.07 |
| glasya_labolas | claws | 239 -> 1279 | 1084 | 162 | 45298 -> 1045255 (x23.1) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.46 | 9.16 |
| haagenti | cleaver | 268 -> 1508 | 1252 | 187 | 65213 -> 1044688 (x16.0) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 8.10 | 7.85 |
| crocell | staff | 217 -> 1463 | 1221 | 191 | 57896 -> 1111976 (x19.2) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 8.96 | 8.88 |
| furcas | halberd | 307 -> 1546 | 1312 | 203 | 45242 -> 1164309 (x25.7) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.93 | 9.59 |
| balam | trident | 344 -> 1737 | 1534 | 232 | 42882 -> 1146772 (x26.7) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 10.13 | 9.79 |
| alloces | longsword | 288 -> 1586 | 1378 | 213 | 50643 -> 1132615 (x22.4) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.35 | 9.07 |
| camio | staff | 303 -> 1561 | 1313 | 198 | 35683 -> 863805 (x24.2) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 9.65 | 9.32 |
| murmur | mace | 385 -> 1948 | 1364 | 205 | 70436 -> 988577 (x14.0) | 2 -> 5 | 0 -> 8 | 1 -> 7 | 7.59 | 7.25 |


(`welded after` in the table is the welded vertex count of the body mesh alone; `DjinnDarkBudgetTest` applies the cap of 2500 to the body plus its six orbiting pieces, the figure in the summary table.)

## 3. What was built

**The pipeline** (`tools/visual_upgrade/djinn_dark_weapons/`). The old weapons are baked meshes (the Djinn armour set's weapon piece, converted by `tools/relics/djinn_weapons.py`), not part-structured
builders, so the upgrade is built on the baked file:

1. every old quad is kept (corners, normals); glow-only parts of the old weapon (energy arcs, flame licks) stay glow-only quads;
2. the old skin islands (clusters of quads whose UV boxes touch) are cut out, enlarged (2x where it fits, else 1.5x or 1x; colour bicubic, alpha nearest) and packed with the UV groups of the new
   parts into a **new 1024 px skin** by a MaxRects packer; the old quads get the new UVs, so old and new surfaces have a similar texel density;
3. new parts are built with the owner's mesh_lib method (real sections, lofts, sweeps, lathes; `tools/relics/mesh_lib.py`, `new197_lib.py`) on the **frame of the old weapon** (`frame.py`: the axis,
   the thickness along y, where the handle ends and the head begins), so a ring, a collar or a ribbon sits on the real weapon whatever its shape; a long tube is cut into parts of at most 44 units so
   no UV strip is longer than the skin allows; the detail factor scales segment, side and repeat counts so a build is sized to **5 times the old quads** (never above the cap);
4. painting: the mesh_lib materials, then on every surface, old and new, grain, an ambient-occlusion band and a bevel rim along every island edge and hairline scratches (`gb.detail_pass` of
   god_melee_b for the new parts, `dlib.detail_old` for the enlarged old islands), a slight cast of the Djinn's colour over the old islands, a core glow map, and a **second emissive map**
   (energy veins on the old islands, lit rune script on bands and rings, the Djinn's name engraved and lit on a plate);
5. writers: mesh, level-of-detail mesh (vertex clustering, at most 14% of the quads), the orbiting piece, the three skins and the data table.

**The effect set** is the one of `god_melee_b`, read from the table by the code that already exists (`client/GodMeleeBRender`, `client/vupgrade/godb/*`, no second engine): the core glow (now breathing
with a slow flicker), the **vein pass** (the second emissive map, brightness a wave running up the weapon), the **halo shell** (the level-of-detail glow quads scaled about the axis), six **orbiting
pieces** (revolving, spinning, bobbing; each Djinn its own piece), a **swing trail** of 3 to 4 ghost copies in the Djinn's glow colour, **seven particle layers** (idle motes, a ring turning round the
head, sparks along the blade, a pool of light at the feet, a flare at the hand, a light shaft, a rare flare), the **level of detail** (full mesh to 12 blocks, the level-of-detail mesh beyond, no additive pass
from 24 blocks, no particles beyond 32, half rate beyond 12). The particle layers name the token `fx`: the mod's own particles, themed and dyed by the layer's colour
(`particle.fx.FxRelic.dust`), never a vanilla particle. The acts keep the effect shapes they had (`DjinnFx`): they are not touched.

**What each Djinn got** is decided by its own row of `docs/djinn_roster_36.json`, so the weapon wears the heraldry of its armour: the *horns* style becomes a crown of spikes, crescent horns,
antlers, curled ram horns or three big spikes; the *wings* style blade wings, feather fans, flame wings, veils or membranes; the *tail* a whip, flames, tendrils or a blade hanging from the pommel;
the *halo* a ring, a spoked ring or a ring of runes behind the head; the *affinity* a pack of its own (lightning coil and forked bolts; heat embers and bands; water tide ribbons and pearls; wind
rings and gusts; life vine and leaves; strength heavy studded bands and spurs; sound resonance rings and bells; light rays and star studs). Every dark weapon also has the dark rig: four grip bands,
a crown of thorns at the pommel, an eye gem, a name plate with the Djinn's name engraved and lit, two tilted rings of lit rune script round the head (one if it has a halo), and (by a hash of its name)
a collar of thorns, a ring of gems and a hanging chain.

| Djinn | weapon kind | affinity | horns / wings / tail / halo | orbiting piece |
|---|---|---|---|---|
| Baal | lance | Lightning | crescent / blade / whip / spokes | bolt (x6) |
| Amon | greatsword | Heat | ram / fire / flame / ring | flame (x6) |
| Vinea | trident | Water | crescent / veil / tendril / runes | bubble (x6) |
| Focalor | glaive | Wind | spike / feather / whip / ring | feather (x6) |
| Zagan | staff | Life | stag / feather / tendril / runes | leaf (x6) |
| Zepar | axe | Strength | ram / membrane / blade / none | claw (x6) |
| Furfur | horn | Sound | crown / veil / tendril / runes | note (x6) |
| Marchosias | claws | Heat | spike / membrane / flame / none | flame (x6) |
| Sitri | bow | Light | crown / feather / none / ring | star (x6) |
| Bifron | glaive | Lightning | stag / blade / blade / spokes | bolt (x6) |
| Paimon | glaive | Wind | crescent / feather / whip / runes | feather (x6) |
| Vassago | scythe | Water | ram / membrane / tendril / none | bubble (x6) |
| Bael | claymore | Strength | crown / blade / none / spokes | rune (x6) |
| Agares | harpoon | Water | ram / veil / blade / none | shard (x6) |
| Valefor | daggers | Lightning | spike / blade / whip / ring | bolt (x6) |
| Barbatos | bow | Life | stag / veil / whip / none | leaf (x6) |
| Gusion | rapier | Water | crescent / veil / none / runes | shard (x6) |
| Vepar | anchor | Water | none / veil / tendril / ring | bubble (x6) |
| Shax | fans | Sound | none / feather / whip / ring | feather (x6) |
| Buer | staff | Life | stag / feather / none / ring | leaf (x6) |
| Andrealphus | lance | Light | crown / feather / whip / spokes | star (x6) |
| Kimaris | lance | Strength | spike / membrane / whip / none | orb (x6) |
| Amdusias | rapier | Sound | spike / feather / tendril / runes | note (x6) |
| Belial | greatsword | Heat | crown / fire / flame / none | flame (x6) |
| Decarabia | pincers | Light | none / membrane / blade / spokes | shard (x6) |
| Seere | sabre | Wind | none / feather / whip / spokes | feather (x6) |
| Dantalion | staff | Light | crown / veil / none / runes | rune (x6) |
| Andromalius | spear | Lightning | stag / blade / tendril / runes | rune (x6) |
| Glasya-Labolas | claws | Strength | ram / membrane / whip / none | claw (x6) |
| Haagenti | cleaver | Strength | crescent / feather / whip / none | rune (x6) |
| Crocell | staff | Water | crown / veil / none / ring | shard (x6) |
| Furcas | halberd | Wind | stag / blade / none / ring | shard (x6) |
| Balam | trident | Heat | ram / fire / whip / spokes | flame (x6) |
| Alloces | longsword | Heat | crown / fire / whip / ring | flame (x6) |
| Camio | staff | Sound | none / feather / blade / runes | feather (x6) |
| Murmur | mace | Sound | crescent / membrane / tendril / spokes | note (x6) |

## 4. The follow-up closed: the God worn upgrade effects onto the custom particles

`client/relics/fx/UpgradeFxClient` (the ambient layers of the upgraded God relics) and `relics/upgrade/UpgradeActFx` (the act bursts) built vanilla coloured `DustParticleOptions`; that is why `docs/rukh/scope.txt` excluded
the two files from the purge scan since 1.106.0. Both now go through the custom particle facade: `"dust"` is `FxRelic.dust(color, size)` (a custom mote dyed with the layer's colour); a vanilla name in a table
(`minecraft:end_rod`, `flame`, ...) becomes the replacement of that particle in the theme of the layer's colour (`Fx.options(FxRelic.theme(color), FxOld.X)`); the decision which name gives which `FxOld` constant is the pure
class `relics/upgrade/UpgradeParticleNames` (an alias table for the names `FxOld` has no constant for: `note` is an enchant glyph, `nautilus` a bubble, `cherry_leaves` a heart, ...; an unknown vanilla name becomes the
end-rod slot). The tick builds a local `ParticleOptions` before `level.addParticle`, which is what the scan's call pattern wants. The two exclusion lines were removed from `docs/rukh/scope.txt`;
`RukhPurgeScopeTest` and `FxSourceScanTest` pass, and `UpgradeFxPurgeScopeTest` pins that the two files are in the scope and that the scan patterns find nothing in them; `UpgradeParticleNamesTest` checks that
every particle name in every shipped table resolves to a constant of the generated `FxOld`. The tables themselves (`assets/nusmp/visual_upgrade/*.json`) were not changed: they keep their vanilla names, now only
names for the facade. Not run in a game: how the custom particles of a theme look instead of the dust is unseen.

## 5. Decisions the brief did not settle (honest notes)

1. **The cap read as welded vertices.** As in `god_melee_b`, the brief's "2500 vertices of one item in hand" is read as the welded vertices of the full-detail mesh including its six orbiting pieces. Every weapon is
   at or under 2500 (`DjinnDarkBudgetTest`). The brief's "mesh >= 5x the baseline quads" holds for all 36 only because the old meshes of this slice are small (168 to 469 quads, against 774 to 3170 in the
   earlier God slices, where it could not be met); the build sizes each mesh to 5.0x (the smallest detail that reaches it) and the 2500 cap would stop it above that: the biggest, Shax (469 quads), reaches 5.17x at
   at 2228 welded vertices (the largest of the 36). Part of the ratio is the six orbiting pieces, counted as the brief's "multi-part orbiting pieces" at full detail (they are drawn six times); the body mesh alone grows 3.9x to 7.5x.
   Emitted vertices per pass are 4 x quads, as the renderer writes them.
2. **The old islands are enlarged, not repainted.** The old skins are the armour set's skin cut down to the weapon (28 000 to 80 000 painted texels in small scattered islands), so there is no source of a
   better painting of the old surfaces: they are enlarged and given grain / occlusion / bevel / the Djinn's colour. The new parts are painted fresh. The old surfaces will look softer than the new ones up close.
3. **The painted-texel target (4x)** is met by the new parts' skin area and the glow maps; texels are counted as `alpha > 0`, as in the other slices. The ratios are large (about 9x to 30x) because the old
   skins were the armour skin cut down to a few small islands (28 000 to 82 000 painted texels of 1 048 576) and the new skin is packed full: the texture ratio saturates against the skin's capacity and says
   little beyond "more surface is painted, at a higher texel density". Video memory: a 1024 px RGBA map is 4 MB, the three maps of one
   weapon 12 MB, uploaded when a weapon is first drawn.
4. **The baseline ambient layers are 0** (section 2), which makes the ambient component of V a plain count. Reported, not hidden.
5. **A held weapon's own particles are placed from the holder's body yaw** (as in `god_melee_b`): close, never exact.
6. **The light weapons** (Legend grade) are not part of this slice and were not touched; the 72 weapons share one renderer, only the 36 dark ones have a table.
7. **Sizes.** The 36 weapons add about 79 MB of skins and meshes to the jar (png, optimised); the old files stay.

## 6. Where the code touches existing files (all additions, listed so a merge stays trivial)

* `NUSMP.java`: one line, `com.newuniverse.nusmp.vupgrade.DjinnDarkSelfTests.register()` (next to the other visual self-tests). Self-test prefix: `visual.djinn_dark_weapons`.
* `client/vupgrade/godb/UpgradeSpecs.java`: the particle token `fx` (the mod's own particle by the layer's colour) before the existing `dust` branch; `UpgradeConfig` (the comment of `visual.upgrade` names the dark Djinn weapons).
  `WeaponRenderer` and `GodMeleeBRender` are **not** edited: the renderer finds the tables by item id (`assets/nusmp/visual_upgrade/<item>.json`) through the hooks of `god_melee_b`.
* `relics/djinn/weapon/DjinnWeaponItem.java`: one tooltip line for the dark weapons (`translatableWithFallback`, key `tooltip.nusmp.djinn_weapon.dark_look`, the English text is the fallback and the lang fragment).
* `client/relics/fx/UpgradeFxClient.java`, `relics/upgrade/UpgradeActFx.java`: the follow-up of section 4; `relics/upgrade/UpgradeParticleNames.java` new.
* `docs/rukh/scope.txt`: the two exclusion lines removed.
* `tools/relics/djinn_weapons.py`: one line at the end of `main` that runs `tools/visual_upgrade/djinn_dark_weapons/build.py` after the old files are written (not in `--check`).
* Lang fragment: `tools/lang/djinn_dark_weapons.json`.

## 7. How to rebuild and check

```
python3 -B tools/visual_upgrade/djinn_dark_weapons/build.py [djinn id ...]      the models, skins, tables (deterministic: a second run changes no byte)
python3 -B tools/visual_upgrade/djinn_dark_weapons/preview.py [djinn id ...]    the before / after sheets
python3 -B tools/visual_upgrade/djinn_dark_weapons/measure.py after             docs/visual_upgrade/djinn_dark_weapons_after.json (and `table` for the markdown above)
python3 -B tools/visual_upgrade/djinn_dark_weapons/make_doc.py                  this page
```
JUnit: `DjinnDarkBudgetTest`, `DjinnDarkRatioTest`, `UpgradeParticleNamesTest`, `UpgradeFxPurgeScopeTest` (and the existing `RukhPurgeScopeTest`, `FxSourceScanTest`, `WeaponMeshesTest`, `WeaponClassesTest`,
`DjinnWeaponAssetsTest`, `DjinnWeaponRulesTest`, `GodMeleeBBudgetTest`, `GodMeleeBRatioTest` still pass). The new files sit in sub-folders (`weapon_meshes/up/`, `textures/entity/weapon/up/`) so no
existing listing sees them.

## 8. What could NOT be verified

* Nothing was run in the game. The Java compiles against the project's API stubs (no error in the new or edited files; a deliberate wrong call was flagged by the same check), the pure logic and the files are tested.
* How the vein wave, the halo shell, the orbiting pieces and the swing ghosts look and how fast they move is untuned; the direction the swing ghosts lag in follows the item's own axes and may be the wrong way round
  for some swings. The orbit radius is the widest extent of the weapon plus 5.5 units: for the wide weapons (fans, daggers) the pieces may look far off.
* The motifs are placed from a measured frame of each old mesh (`frame.py`), not by hand: the preview sheets show them sitting plausibly on all 36, but a ring may cut through a wing of the old mesh or a ribbon
  hang oddly for a weapon whose silhouette the frame measures badly (bows, claws, fans). The frame takes the low-y end as the butt and the high-y end as the head, so the two bows (Sitri, Barbatos) carry their
  pommel crown, chain or tail on the lower limb tip and the head motifs on the upper limb: asymmetric, not mirrored; no per-weapon anchor override (`ditems.ANCHORS`, empty) was needed to keep a motif off the
  weapon in the previews, but none was tuned either. The sheets are the software rasteriser's, with one light and no additive blending.
* The enlarged old islands next to the fresh new parts: the difference in sharpness is only seen in the game. The core glow of the old weapon is kept; the veins on the old islands are procedural.
* `visual.guiModel` (the tilted model in the inventory slot) was written for `god_melee_b` and never drawn for these weapons.
* Frame time: the cost model is by construction (one extra pass over at most 700 glow quads for the veins, at most 330 level-of-detail quads for the halo and the ghosts, 6 small pieces), not measured; `/nusmp perf` was not run.
