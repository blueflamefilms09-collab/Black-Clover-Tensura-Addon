# Flat-part audit (every weapon mesh)

Made by `python3 tools/heroweapon/flat_parts_audit.py --md docs/heroweapon/flat_parts_audit.md` (pure Python 3 + numpy). Each named part of each mesh is measured along its thinnest principal axis
(PCA of the body-layer quads, times the `place` scale, so the numbers are blocks). Two rules:

- **must be solid** (the heads of spears and harpoons, the arrow of every bow draw stage): FLAT below 0.004 blocks or below 6 % of the part's second extent, THIN below 2 % of the weapon's length;
- **sheet** (every other part): thinner than 0.004 blocks and wider than 0.02 blocks. Thinner and narrower is a hairline (strings, rivet heads).

## Counts

| What | Count |
| --- | --- |
| meshes measured | 610 |
| named parts measured | 4557 |
| meshes with a must-be-solid part (spear / harpoon head, bow arrow) | 44 |
| of those, flat or paper-thin in the ORIGINAL (`flat_parts_old`) | 44 |
| of those, flat or paper-thin NOW | 0 |
| of those, patched by the Blender-less script (no `# blender-solid` line), not modelled in Blender | 0 |
| of those, rebuilt solid in Blender | 44 |
| other sheet-like parts, thin by design (strings, fullers, rivets, edge lines, ...) | 86 parts in 63 meshes |
| other sheet-like parts, candidates for a later redo | 24 parts in 24 meshes |

## The 44 meshes with a must-be-solid part

| Mesh | Part | Original: thickness (blocks) / share of length | Now | Made by |
| --- | --- | --- | --- | --- |
| `legendary_bow__arrow_shower_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__arrow_shower_pull1` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__arrow_shower_pull1` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__arrow_shower_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__arrow_shower_pull2` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__arrow_shower_pull2` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__arrow_shower_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__arrow_shower_pull3` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__arrow_shower_pull3` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__bind_arrow_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0389 / 2.8 % | Blender |
| `legendary_bow__bind_arrow_pull1` | arrow_head | 0.0215 / 1.6 % | 0.0365 / 2.6 % | Blender |
| `legendary_bow__bind_arrow_pull1` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__bind_arrow_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0389 / 2.8 % | Blender |
| `legendary_bow__bind_arrow_pull2` | arrow_head | 0.0215 / 1.6 % | 0.0365 / 2.6 % | Blender |
| `legendary_bow__bind_arrow_pull2` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__bind_arrow_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0389 / 2.8 % | Blender |
| `legendary_bow__bind_arrow_pull3` | arrow_head | 0.0215 / 1.6 % | 0.0365 / 2.6 % | Blender |
| `legendary_bow__bind_arrow_pull3` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__bird_hunting_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 2.9 % | Blender |
| `legendary_bow__bird_hunting_pull1` | arrow_head | 0.0132 / 0.9 % | 0.0479 / 3.3 % | Blender |
| `legendary_bow__bird_hunting_pull1` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 4.7 % | Blender |
| `legendary_bow__bird_hunting_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 2.9 % | Blender |
| `legendary_bow__bird_hunting_pull2` | arrow_head | 0.0132 / 0.9 % | 0.0479 / 3.3 % | Blender |
| `legendary_bow__bird_hunting_pull2` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 4.7 % | Blender |
| `legendary_bow__bird_hunting_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 2.9 % | Blender |
| `legendary_bow__bird_hunting_pull3` | arrow_head | 0.0132 / 0.9 % | 0.0479 / 3.3 % | Blender |
| `legendary_bow__bird_hunting_pull3` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 4.7 % | Blender |
| `legendary_bow__eagle_piercer_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__eagle_piercer_pull1` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__eagle_piercer_pull1` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__eagle_piercer_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__eagle_piercer_pull2` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__eagle_piercer_pull2` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__eagle_piercer_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__eagle_piercer_pull3` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__eagle_piercer_pull3` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__frozen_rain_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__frozen_rain_pull1` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__frozen_rain_pull1` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__frozen_rain_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__frozen_rain_pull2` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__frozen_rain_pull2` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__frozen_rain_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__frozen_rain_pull3` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__frozen_rain_pull3` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__hunters_bow_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__hunters_bow_pull1` | arrow_head | 0.0215 / 1.6 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__hunters_bow_pull1` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__hunters_bow_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__hunters_bow_pull2` | arrow_head | 0.0215 / 1.6 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__hunters_bow_pull2` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__hunters_bow_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__hunters_bow_pull3` | arrow_head | 0.0215 / 1.6 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__hunters_bow_pull3` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__meteor_bow_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__meteor_bow_pull1` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.4 % | Blender |
| `legendary_bow__meteor_bow_pull1` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 4.9 % | Blender |
| `legendary_bow__meteor_bow_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__meteor_bow_pull2` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.4 % | Blender |
| `legendary_bow__meteor_bow_pull2` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 4.9 % | Blender |
| `legendary_bow__meteor_bow_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__meteor_bow_pull3` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.4 % | Blender |
| `legendary_bow__meteor_bow_pull3` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 4.9 % | Blender |
| `legendary_bow__sagittarius_pull1` | arrow_shaft | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__sagittarius_pull1` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__sagittarius_pull1` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__sagittarius_pull2` | arrow_shaft | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__sagittarius_pull2` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__sagittarius_pull2` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__sagittarius_pull3` | arrow_shaft | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__sagittarius_pull3` | arrow_head | 0.0230 / 1.7 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__sagittarius_pull3` | arrow_fletch | 0.0296 / 2.1 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__scouts_bow_pull1` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__scouts_bow_pull1` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__scouts_bow_pull1` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__scouts_bow_pull2` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__scouts_bow_pull2` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__scouts_bow_pull2` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow__scouts_bow_pull3` | arrow_stem | 0.0109 / 0.8 % | 0.0413 / 3.0 % | Blender |
| `legendary_bow__scouts_bow_pull3` | arrow_head | 0.0132 / 1.0 % | 0.0479 / 3.5 % | Blender |
| `legendary_bow__scouts_bow_pull3` | arrow_fletch | 0.0296 / 2.2 % | 0.0684 / 5.0 % | Blender |
| `legendary_bow_curse_pull1` | arrow_shaft | 0.0071 / 0.6 % | 0.0371 / 3.1 % | Blender |
| `legendary_bow_curse_pull1` | arrow_head | 0.0138 / 1.1 % | 0.0292 / 2.4 % | Blender |
| `legendary_bow_curse_pull1` | arrow_fletch | 0.0191 / 1.6 % | 0.0536 / 4.5 % | Blender |
| `legendary_bow_curse_pull2` | arrow_shaft | 0.0071 / 0.6 % | 0.0371 / 3.1 % | Blender |
| `legendary_bow_curse_pull2` | arrow_head | 0.0138 / 1.1 % | 0.0292 / 2.4 % | Blender |
| `legendary_bow_curse_pull2` | arrow_fletch | 0.0191 / 1.6 % | 0.0536 / 4.5 % | Blender |
| `legendary_bow_curse_pull3` | arrow_shaft | 0.0071 / 0.6 % | 0.0371 / 3.1 % | Blender |
| `legendary_bow_curse_pull3` | arrow_head | 0.0138 / 1.1 % | 0.0292 / 2.4 % | Blender |
| `legendary_bow_curse_pull3` | arrow_fletch | 0.0191 / 1.6 % | 0.0536 / 4.5 % | Blender |
| `legendary_bow_pull1` | arrow_shaft | 0.0081 / 0.7 % | 0.0422 / 3.5 % | Blender |
| `legendary_bow_pull1` | arrow_head | 0.0157 / 1.3 % | 0.0332 / 2.8 % | Blender |
| `legendary_bow_pull1` | arrow_fletch | 0.0239 / 2.0 % | 0.0611 / 5.1 % | Blender |
| `legendary_bow_pull2` | arrow_shaft | 0.0081 / 0.7 % | 0.0422 / 3.5 % | Blender |
| `legendary_bow_pull2` | arrow_head | 0.0157 / 1.3 % | 0.0332 / 2.8 % | Blender |
| `legendary_bow_pull2` | arrow_fletch | 0.0239 / 2.0 % | 0.0611 / 5.1 % | Blender |
| `legendary_bow_pull3` | arrow_shaft | 0.0081 / 0.7 % | 0.0422 / 3.5 % | Blender |
| `legendary_bow_pull3` | arrow_head | 0.0157 / 1.3 % | 0.0332 / 2.8 % | Blender |
| `legendary_bow_pull3` | arrow_fletch | 0.0239 / 2.0 % | 0.0611 / 5.1 % | Blender |
| `legendary_spear` | spearhead | 0.0519 / 1.3 % | 0.1015 / 2.6 % | Blender |
| `legendary_spear__chaos_spear` | head_blade | 0.0520 / 1.2 % | 0.0980 / 2.3 % | Blender |
| `legendary_spear__clockwork_spear` | head_blade | 0.0588 / 1.5 % | 0.0980 / 2.4 % | Blender |
| `legendary_spear__dragon_minute_hand` | head_blade | 0.0588 / 1.5 % | 0.0980 / 2.4 % | Blender |
| `legendary_spear__filolial_spear` | head_blade | 0.0464 / 1.2 % | 0.0980 / 2.5 % | Blender |
| `legendary_spear__hunters_spear` | head_blade | 0.0434 / 1.1 % | 0.0980 / 2.5 % | Blender |
| `legendary_spear__meteor_spear` | head_blade | 0.0611 / 1.5 % | 0.0980 / 2.4 % | Blender |
| `legendary_spear__portal_spear` | head_blade | 0.0559 / 1.3 % | 0.0980 / 2.3 % | Blender |
| `legendary_spear__triaina` | head_blade | 0.0618 / 1.5 % | 0.0980 / 2.4 % | Blender |
| `legendary_spear__triaina` | head_prongs | 0.0509 / 1.2 % | 0.0891 / 2.2 % | Blender |
| `legendary_spear_curse` | spearhead | 0.0519 / 1.3 % | 0.1009 / 2.6 % | Blender |
| `vassal_harpoon` | head_blade | 0.0280 / 1.0 % | 0.0901 / 3.2 % | Blender |
| `vassal_harpoon` | head_barbs | 0.0198 / 0.7 % | 0.0654 / 2.3 % | Blender |

## Other sheet-like parts: candidates

These are parts of other meshes (not the arrow / spear head families) that are thinner than a quarter of a pixel and wider than a card: bow limb panels, fins and
streamers, a shield ring. Not redone in this pass; the Blender builders of those bows would need a thicker section.

| Mesh | Part | Quads | Thickness (blocks) | Width (blocks) |
| --- | --- | --- | --- | --- |
| `divine_mercy` | limb_coverts | 184 | 0.0019 | 0.138 |
| `divine_mercy_pull1` | limb_coverts | 184 | 0.0019 | 0.144 |
| `divine_mercy_pull2` | limb_coverts | 184 | 0.0019 | 0.153 |
| `divine_mercy_pull3` | limb_coverts | 184 | 0.0019 | 0.158 |
| `silver_eagles_galewing_bow` | limb_streamers | 120 | 0.0016 | 0.069 |
| `silver_eagles_galewing_bow_pull1` | limb_streamers | 120 | 0.0016 | 0.069 |
| `silver_eagles_galewing_bow_pull2` | limb_streamers | 120 | 0.0016 | 0.069 |
| `silver_eagles_galewing_bow_pull3` | limb_streamers | 120 | 0.0016 | 0.069 |
| `starforged_aeon_bow` | limb_fins | 168 | 0.0025 | 0.185 |
| `starforged_aeon_bow_pull1` | limb_fins | 168 | 0.0025 | 0.199 |
| `starforged_aeon_bow_pull2` | limb_fins | 168 | 0.0025 | 0.220 |
| `starforged_aeon_bow_pull3` | limb_fins | 168 | 0.0025 | 0.231 |
| `the_dragons_spirit` | limb_panels | 576 | 0.0025 | 0.124 |
| `the_dragons_spirit_pull1` | limb_panels | 576 | 0.0025 | 0.138 |
| `the_dragons_spirit_pull2` | limb_panels | 576 | 0.0025 | 0.159 |
| `the_dragons_spirit_pull3` | limb_panels | 576 | 0.0025 | 0.171 |
| `the_reaver` | limb_cracks | 384 | 0.0024 | 0.126 |
| `the_reaver_pull1` | limb_cracks | 384 | 0.0024 | 0.137 |
| `the_reaver_pull2` | limb_cracks | 384 | 0.0024 | 0.155 |
| `the_reaver_pull3` | limb_cracks | 384 | 0.0024 | 0.164 |
| `thornmothers_briar_bow` | limb_leaves | 72 | 0.0030 | 0.147 |
| `thornmothers_briar_bow_pull1` | limb_leaves | 72 | 0.0030 | 0.155 |
| `thornmothers_briar_bow_pull2` | limb_leaves | 72 | 0.0030 | 0.167 |
| `thornmothers_briar_bow_pull3` | limb_leaves | 72 | 0.0030 | 0.174 |

## Other sheet-like parts: thin by design

Strings, fuller grooves, rivet heads, edge lines and the like. A string that is a pixel thick is correct; listed so the count is honest.

| Part kind | Parts | Meshes |
| --- | --- | --- |
| string | 37 | 37 |
| riser_rivets | 16 | 8 |
| fuller_f | 5 | 5 |
| fuller_b | 5 | 5 |
| limb_blades | 4 | 4 |
| vane_ | 4 | 1 |
| head_fuller_f | 2 | 2 |
| head_fuller_b | 2 | 2 |
| blade_ribs | 2 | 2 |
| strings | 2 | 2 |
| blade_edge | 1 | 1 |
| portal | 1 | 1 |
| paper | 1 | 1 |
| pg_studs | 1 | 1 |
| wisp_a_wings | 1 | 1 |
| wisp_b_wings | 1 | 1 |
| pinwheel | 1 | 1 |
