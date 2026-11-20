# Flat parts redone in Blender (the 44 hero weapon meshes)

Owner backlog item: "redo the 44 flat-part meshes in Blender". The 44 meshes are the ones `docs/heroweapon/flat_parts.md` made solid with the Blender-less script
(`tools/heroweapon/solidify_flat_parts.py`): the arrow of every Legendary Bow draw stage, the head of the Legendary Spear and of the Vassal Harpoon. CLAUDE.md rule 14 was
not met for them. They are now modelled in Blender by the same builders that made the weapons, baked and exported through the existing pipeline (`tools/blender/bl_lib.py`,
`forms_kit.py`).

## What was built (bpy 5.3 as a Python module, no window; scripts drive it, like every other model of this family)

| Builder | Change | Meshes |
| --- | --- | --- |
| `tools/blender/legendary.py` (`spear`, `spear_curse`, `bow`, `bow_curse`) | spear head lozenge half thickness x1.95 (0.052 -> 0.10 blocks); the bow arrow is a new solid model: octagonal shaft with a nock collar and neck, flattened hexagonal broadhead, three crossed 1.1 unit thick vanes | `legendary_spear`, `legendary_spear_curse`, `legendary_bow(_curse)` + `_pull1..3` |
| `tools/blender/forms_legendary_bow.py` (`arrow_solid`) | the same solid arrow for the 9 bow forms (`broad` / `bodkin` / `barb` heads keep their kind); a lean variant (hexagonal shaft, no socket) when the scene already uses nearly all 5000 quad lines (`bind_arrow`) | 9 forms x (rest + 3 draw stages) |
| `tools/blender/forms_legendary_spear.py` (`solidify_heads`, `PRONG_R`) | the head blades are stretched along their thinnest axis in Blender to 0.098 blocks (factor 1.6 to 2.3, capped at 2.6), the studs / claws / sparks that sit on the blade are carried outward by the same share; the Triaina's gold prong tubes are 1.75x thicker | 8 forms (Bruenack was already thick and is untouched) |
| `tools/blender/vassal_a.py` (`harpoon`) | head half thickness x3.2, barbs x3.5 (0.028 -> 0.09 blocks, 0.02 -> 0.065) | `vassal_harpoon` |

Every builder keeps its old geometry behind `--classic-flat` (builds the old thin head / hairline arrow). Place line, length, `grip` lines and part names are unchanged.
Seeds are unchanged: the rest of each weapon bakes to the same geometry (verified for `legendary_spear`: the non-quad lines of the rebuilt mesh equal the shipped one).

`tools/heroweapon/install_solid_meshes.py DIR...` installs a builder's `--trial` output: keeps the old skin, writes the meshes with a `# blender-solid` header line and the
shipped `gripxyz`, packs the skins, checks the 5000 quad-line and 350 KB budgets. `--pin-grips` re-pins a grip point the finder moved (3 meteor-bow draw stages).

## Result (the audit, `docs/heroweapon/flat_parts_audit.md`)

`python3 tools/heroweapon/flat_parts_audit.py --md docs/heroweapon/flat_parts_audit.md` measures every part of all 610 meshes (4557 parts):

- 44 meshes have a must-be-solid part. All 44 were flat or paper-thin in the original; all 44 are solid now and all 44 are Blender builds (0 script-made).
- Spear head thinnest extent: 1.1-1.5 % of the weapon before, 2.3-2.6 % now, harpoon 3.2 % (the audit asks 2 %). Arrow shaft 0.8 % -> 3.1 %, head 1.6-1.7 % -> 2.6-2.8 %, fletching 2.2 % -> 5.1 %.
- 24 other parts in 24 meshes are sheet-like (under 0.004 blocks thick, wider than 0.02): bow limb panels, fins, streamers, one pinwheel. Listed as candidates, not redone.
  86 parts in 63 meshes are thin by design (bow strings, fuller grooves, rivet heads) and are only counted.

## NEW / REPLACED / KEPT (rule 5)

NEW
- `docs/heroweapon/flat_parts_audit.md`, `flat_parts_audit.py --md`, `install_solid_meshes.py`, `flat_parts_v2_previews.py`, previews in `docs/previews/flat_parts_v2/` (before_*, after_*).
- The solid-arrow model `arrow_solid` (forms bows) and the solid branches of the four builders.
- `textures/entity/weapon/flat_parts_old/`: the 21 skins (+ glow skins) the old meshes were laid out for. A Blender re-bake packs the skin again, so the old UVs no longer fit the new skin.
- Java: `WeaponRenderer.readFlatOld` draws an old flat-part mesh with its own skin (no new setting; it is the existing `visual.heroSolidParts = false`).
- `HeroSolidTest.theSolidMeshesAreBlenderBuildsAndTheOldSkinsAreKept`.

REPLACED (the owner asked for the redo; rule 2 exception)
- The 44 script-made meshes are now Blender meshes: 33 bow draw stages (+ the 11 rest meshes, whose skin was baked again), 2 + 8 spears, the harpoon. The script versions are
  not kept (they are in git history, 4.54.0); the script `solidify_flat_parts.py` stays in the repo.
- The 21 skins and glow skins of those weapons (new bake).

KEPT
- The original hairline / paper-thin meshes in `weapon_meshes/flat_parts_old/` (44 files) behind `visual.heroSolidParts = false`, now with their own skins.
- All ids, `place`, `grip`/`gripxyz` lines, part names, Better Combat files (`inventory.py`: unchanged), hold table (`hold_audit.py`: only the 2 known NONE notes), `classic/` models.
- `legendary_spear__bruenack`, the scythe blades, the thin-by-design parts.

## Cost (rule 11) and server safety (rule 9)

Client drawing only, no server, network, tick or config change. Worst case per weapon in hand: the mesh's quad lines, at most 5000 as before (largest now: `bind_arrow` pull
stages 4991, `dragon_minute_hand` 4962, `bird_hunting` pull 4970); the runtime mesh cap still applies. The solid arrow is 70-125 quads (before: 230 in the forms, 138 in the
base bow). Skins 99-338 KB per weapon (skin + glow, limit 350). `flat_parts_old/` skins add about 6 MB of resources.

## Tool honesty (rule 14)

All models are built by Python scripts that drive Blender (bpy as a module: bmesh, modifiers-free lofts / lathes / strips, Cycles bake, Smart UV Project). No Blender window was
used and no model was sculpted by hand; "made in Blender" here means the same scripted bpy pipeline as every other weapon of the mod. Blockbench was not used (these are baked
meshes, not cube models). The spear head stretch is a vertex transform done in bpy after the scripted modelling (not a hand-modelled blade).

## Not seen / caveats

- Nothing was seen in a game and no Gradle build ran. The previews are the repo's software rasteriser (low resolution, glow off), not in-engine and not Cycles stills.
- The thicker spear heads are fat diamonds (the audit's 2 % rule, as in the script version); the engraved blue core of the base spear head is baked detail and sits on the
  new surface. The Triaina's studs are carried outward by the stretch, not re-modelled.
- Skin packing: the chaos spear is boxed to 768 px so skin + glow fit 350 KB (as the repack rule of the builder does); the spear skins use 32-80 colour palettes (PSNR 25-31 dB), the same
  packer the 4.27.0 forms used.
- `bind_arrow`'s arrow is the lean variant (hexagonal shaft, no socket collar): the rest bow alone is 4916 of the 5000 quad lines.
- The draw-stage meshes of every bow stay on the shared skin, as before.
