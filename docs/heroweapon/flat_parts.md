# Flat parts of the hero weapons made solid

Owner request: "fix / make sure the part of the weapon that should be 3d are like the tip of the spear or the arrows from the bow".

Read as: in the hero / legendary / vassal weapons, a part that must be a solid object (a spear head, a harpoon head, the arrow nocked on the bow, the arrow of light that flies, a thrown spear tip) must not be a flat card or a hairline. It is a real shaded 3D part now.

Not included: the scythe blades and the other thin-by-design blades (a blade edge is meant to be thin), the harpoon throw (`LIGHTNING_SPEAR`, a lightning bolt, not a head), any gameplay change, any new packet.

## What was found

`python tools/heroweapon/flat_parts_audit.py` measures the thickness of the parts that must be solid (PCA of each part's quads; flat = under 0.004 blocks or under 6 % of its length).

| Family | Parts audited | Flat or paper-thin before | After |
| --- | --- | --- | --- |
| spear head (`spearhead`, `head_blade`, `head_barbs`, `head_prongs`) in `legendary_spear*`, `vassal_harpoon` | 95 meshes in total with the bows | 10 meshes (base, curse, 8 forms; bruenack was already thick) + the harpoon barbs | 0 |
| bow arrow (`arrow_shaft`, `arrow_stem`, `arrow_head`, `arrow_fletch`) in the 33 `legendary_bow*_pull1/2/3` | 33 meshes | all 33 (a hairline shaft, a card head, flat vanes) | 0 |
| flying arrow of light (server effect `STAR_MAGIC`) | 3 bow classes, 11 call sites | a glow streak, no arrow | solid arrow model |
| flying spear tip (spear effects, hero THRUST) | 3 spear classes, 5 call sites + the hero thrust | a glow streak | solid spear tip |

Audit total: 85 findings before, 0 after (`--check` exits 0).

## What changed

NEW
- `HeroSolid` (pure): switch, caps, aim maths, flight curve.
- `HeroProjectileLayer` with the new shapes `HERO_ARROW` and `HERO_SPEAR` (appended at the end of `VfxShape`): a small solid model (`SolidModels.arrow`, `SolidModels.spearTip`, with a barbed variant for the harpoon) flies point first on a short glow trail.
- The hero THRUST motif (`SkillAnime2Hero`, variants 0 spear, 1 harpoon, 2 arrow) draws the same solid point at its head.
- `SolidParts.beginAimed`, `SolidModels.arrow`, `SolidModels.spearTip`.
- Client setting `visual.heroSolidParts` (key `heroSolidParts` in `nusmp-flatprops-client.toml`, default true).
- `tools/heroweapon/flat_parts_audit.py`, `flat_parts_preview.py`, `solidify_flat_parts.py`.
- Self-test `visual.hero_solid` (on the smoke list), `HeroSolidTest`.
- Previews in `docs/heroweapon/flat_parts_previews/` (before and after).

REPLACED (rule 5: same feature, better version; the old one stays behind the setting)
- 44 meshes in `weapon_meshes/`: 33 `legendary_bow*_pull[123]`, `legendary_spear`, `legendary_spear_curse`, 8 `legendary_spear__*` forms, `vassal_harpoon`. Thicker heads and a rebuilt solid arrow (octagonal shaft, nock, diamond head, crossed vanes).
- The server effect shape of the arrow of light and the spear tip: `STAR_MAGIC` became `HERO_ARROW` / `HERO_SPEAR` in `BowShots`, `BowDirect`, `BowRain`, `SpearGuard`, `SpearMotion`, `SpearStrikes`. One packet per effect, as before; only the shape id differs.

KEPT
- The 44 original meshes, untouched, in `weapon_meshes/flat_parts_old/`. With `heroSolidParts = false`, `WeaponRenderer` reads them again for the classic legendary weapons.
- The old glow beam: `HeroProjectileLayer.legacy()` draws exactly what `STAR_MAGIC` drew when the setting is off. `STAR_MAGIC` itself and its other users are not touched.
- All textures, every other shape, all gameplay values. The harpoon dash in `BowDirect` (line 118) keeps its old effect.

Grip: the thicker arrow moved the automatic grip point of 30 bow pull meshes (z from about 0 to 1.2 to 2.7 mesh units). Those meshes now carry a `gripxyz` line with the old point, so the hand position is identical; `tools/weapon_grip/grip_golden.json` was refreshed and only the `source` of those rows changed (`class` to `gripxyz`). `tools/bettercombat/inventory.py` was not needed (no grip value changed).

## Cost (rule 11) and server safety (rule 9)

- Server: no new work. No new packet, no new entity, no new config, no gameplay change. The same single effect packet per arrow or thrust carries a different shape id. The server never reads `heroSolidParts` and loads no client class (checked by `HeroSolidTest`).
- Client, per effect: one model of at most 48 quads, a 2 quad trail, at most 4 glints (8 quads), one glow quad: under 320 vertices, inside the 400 vertex activation cap.
- Client, per frame: at most 16 solid models (`HeroSolid.MAX_PER_FRAME`); the rest keep the glow trail. Nothing beyond 40 blocks is built (`SolidParts.MAX_DISTANCE`). The manager's caps on running effects and frame vertices still apply.
- Hero THRUST: the solid point adds up to 48 quads to an effect that was about 60 quads; the 400 vertex activation cap clips the excess (the late sonic rings go first), so the worst case does not grow.
- In hand: the meshes keep their quad counts within a few quads (spear 68 quads before and after; bow pull3 102 before, 138 after); the runtime mesh cap (`MeshCap`) still applies.

## Tests

- `HeroSolidTest` (pure and source scan): aim round trip, frame budget, flight curve, switch default, pure file, appended shapes, registered layer, client switch, server calls only send a shape, old meshes kept and differ, smoke list.
- `visual.hero_solid` self-test inside the booted server.
- `flat_parts_audit.py --check`, `grip_table.py --check`.
- `SaiyanPhaseSourceScanTest` pin changed from `"GIANT_AVATAR;"` to `indexOf("GIANT_AVATAR")` because the enum list now continues after it.

## Honest caveats

- Blender was not available in this session (`which blender` found nothing, and installing `bpy` was refused). The thickened heads and rebuilt arrows are made by `tools/heroweapon/solidify_flat_parts.py` (Blender-less), not modelled in Blender: rule 14 is not met for these 44 meshes. Redo them in Blender when it is available.
- Nothing was seen in a game. The previews are the repo's own software renderer; the flying arrow / tip layer, the aim direction and the look of the glow on top were not seen in-engine, and no Gradle compile was run (CI compiles). The pure tests were run with javac and the JUnit launcher.
- Found but not changed: the scythe blades (thin by design), the harpoon throw (`LIGHTNING_SPEAR`).
- `flat_parts_old/` adds about 42 MB of resources to the jar. Delete it only if the owner drops the old look (rule 2).
