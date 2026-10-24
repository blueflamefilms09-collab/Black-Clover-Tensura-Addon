# Earth VFX remake: three art directions (proposal only, nothing implemented)

Read `earth_research.md` first (canon, current look, costs, failed sources). Pictures: `docs/previews/vfx_remake/earth_baseline.png` (now), `earth_reference_board.png` (owner art with captions). The owner asked to see 3 different ideas before anything is built: pick one (or mix) and say go.

## 0. Rules every direction follows

- Rule 2 (add, keep old): the new look is drawn by a NEW layer `EarthSpellLayerV2` (client only) for the SAME shapes `STONE_SPIKES, EARTH_RISE, EARTH_FISSURE, EARTH_CLAWS, EARTH_RAMPAGE` and, if wanted, the three `EARTHFAM_*` shapes. No enum entry is removed or renamed; old layers stay in the code.
- Switch: CLIENT config `vfx.element.earth = V2 | V1` (default V2 after approval, V1 restores today's look). Look-only, so a client config (rule 9). Server unchanged: no new packets, same `VfxSpawn.send` payload (from, to, power, seed, duration), so a dedicated server and mixed clients work.
- No shaders. Only `VfxVertexBuffer.quad / billboard / ring / beam` with textures drawn by Pillow scripts, in the style of `tools/gen_element_vfx_textures.py` (new script `tools/gen_earth_vfx_v2_textures.py`, textures `earth2_*.png` in `textures/particle/`, no overwrite of `earth_rock` etc.).
- Budget: every effect at or below TODAY's quad cost (EARTH_SPIKES-stone 41, RISE 40, FISSURE 76, CLAWS 80, RAMPAGE 93; cap 100 quads / 400 vertices per effect). Flying debris obeys `ctx.seg(full, far)` (halved for distant effects), effects of far players are skipped by the existing distance cull. Per-frame allocation: reuse a few `Vector3f` fields, no new lists. Worst case per player: one effect alive at a time per page, <= 100 quads, no packets added.
- Do not touch Time Magic VFX (rule 3). Not shared with other elements: new helper class `EarthFx2`, not `ElementFx`.
- Where I could not see the real anime frame, the direction says so; hex colours are taken from the owner's Ideal_Closer / Gravity_Singularity / Red_Shine_Mace stills (measured by eye, not sampled from the episodes).

Canon fix offered in every direction (optional switch, gameplay unchanged): Claws drawn as an earth MOLE mound plus claws, Rampaging Mother Earth drawn as a stone GOLEM silhouette, Divided Mother Earth as 3 small golems (see research 2.3 to 2.5). The effect shape stays; only the drawing changes.

---

## Direction A: "Anime cel" (chunky angular slabs, outline, rim, dust cels)

Look: what the owner's Ideal_Closer still shows. Every rock is a flat-shaded polygon with a 1.5 px dark outline and a bright rim on the lit edge; the dust is a cluster of round cel puffs with a hard grey shadow side, like the smoke in Gravity_Singularity.

Shapes (all from existing `quad`): "plate" = low-poly 5-side prism (2 triangles-as-quads on top, 3 visible sides = 5 quads), 3 flat tones per plate, NO texture gradient.
Textures (Pillow, 64 px, nearest filter, drawn with `ImageDraw.polygon`, no noise):
- `earth2_plate_lit/mid/dark` : flat fill + 2 px outline `#1B1620`.
- `earth2_rim` : 2 px bright edge strip `#E4E8F2`.
- `earth2_puff` : 3 overlapping circles, light `#C9C2B6`, shade `#8F887C`, outline `#3A342E`, hard edge (alpha 0 or 255).
- `earth2_shard` : sharp triangle, outline, 3 tones (Gravity_Singularity shards).
Palette: stone `#8A93A8` lit / `#6C7590` mid / `#4B5468` dark (cool grey-blue, Ideal_Closer); soil variant `#B07A44 / #8A5A30 / #5A3A20` (Gravity_Singularity ground); outline `#1B1620`; dust light `#C9C2B6`, shade `#8F887C`; mana vein none. The colour passed by the spell (page colour) tints the soil/stone choice.

Per effect (quads, <= current):
- Earth Spikes: 6 to 9 tall irregular columns (hexagonal prism, height 1.2 to 2.4 x power, 5 quads each = 45 max, trimmed to 36 with `ctx.seg`), each pops up in 3 stepped frames (cel animation on 2s: height 0, 40%, 100%, overshoot 110% then 100%; steps every 1 tick, not smooth) + 1 puff cel each on emerging (6 puffs).
- Ground Wall: THE WALL becomes visible: 5 plates side by side rise 0 to 2.2 x power over 6 ticks (easeOutBack stepped on 2s), 5 x 5 = 25 quads; outline-only crack strips at the base (2 quads), 6 puffs rolling out, 4 shards thrown. Total 38.
- Divided Mother Earth (fissure): a crack strip with white rim line, 6 plates pushed up in a zigzag (30), 6 puffs, 0 chunks. Total 42. Option "golems": 3 mini golems (see Rampage) replace the plates: 3 x 10 = 30.
- Witch Hunter Claws: dirt mound travelling under the ground (a flat bump of 6 plates, 24 quads) from edge to centre, then 4 clawed hooks of 3 plates each close (4 x 3 x 5 = 60 would be too many, so 4 hooks x 2 plates x 5 = 40). Total <= 70.
- Rampaging Mother Earth: a golem silhouette made of 9 to 10 plates (torso 2, head 1, 2 shoulders, 2 forearms/fists, 2 legs), arranged as the Ideal_Closer pose, 10 x 5 = 50 quads; it rises out of the ground (0 to 100% in 8 ticks, stepped), steps along from to to by moving 2 blocks per 4 ticks (stepped, no smoothing), a dust puff at each step (8) and 6 shards. Total 70 <= 93.
Motion curves: stepped "on twos": `t' = floor(t * 12) / 12` for pose changes; anticipation (hold 3 ticks and sink 5%) then overshoot (110%); impacts get a 2 tick freeze (scale 1.15 white flash quad `#FFFFFF` alpha 0.8 for 1 tick) and the camera shake already used (`VfxShake`).
Impact: radial shards (Iron_God_of_War manga look): 8 thin triangles from the centre outward `#E4E8F2`, 4 ticks.
Particles: 10 to 16 debris shards + puffs per effect, all counted in the quads above.
Risk: flat tones can look "paper" on a textured Minecraft world; golem made of plates may look blocky/odd from behind (mitigate: plates face-culled both sides); the stepped animation may look like lag at 20 tps (mitigate: option "smooth", default stepped only on rises). Replaces: the five EarthSpellLayer look-alikes by V2 drawn with the same shapes. Doesn't need new shapes.

Estimated work after approval: texture script 1 day equivalent, layer + helper 1 day, preview scenes 0.5, tests (budget, texture lists) 0.5.

---

## Direction B: "Volumetric rubble" (layered dust clouds, flying debris, soft shadows)

Look: the physical version. Rock is the existing textured brown stone; the story is told by DUST and DEBRIS: layered billboards in 3 depths, soft contact shadows on the ground, many small stones with parabolic flight and bounce, a ground "shock ring" that kicks the dust. Closer to the dust cloud in the Gravity_Singularity still (soft grey-tan billows) than to its outlines.
Textures (Pillow, fbm noise as in the current script):
- `earth2_cloud_a/b/c` 128 px: three lumpy puffs with alpha falloff, grey-tan `#B9AA90` core, `#6E6350` underside, noise-eroded edge (domain-warped fbm).
- `earth2_shadow` 64 px: round soft black `#000000` alpha 0.45 (ground-decal under rocks).
- `earth2_pebble` 32 px x 4 variants: 7-point polygons with 3-tone lighting, `#7A6A56 / #A18F76 / #4A3F33`.
- `earth2_crack_wide`: 256x64 crack with 2 dark cores and chipped edges.
Palette: soil `#7A6048` to `#B08850` (matches `EarthFxStyle` earth colour), dust `#B9AA90`, deep shadow `#2B2118`, dry highlight `#E1D2B2`.
Motion: ballistic, `y = v t - 0.5 g t^2` with `g = 0.5 blocks / tick^2 / 20` scaled; bounce once at 40% restitution; dust `size = s0 (1 + 2.2 sqrt(t))`, alpha `(1 - t)^1.5`, rises at 0.05 blocks/tick and drifts with the shape's direction; layers: front, mid, back puffs at different speeds (parallax by 1.0 / 0.7 / 0.5).
Per effect (quads):
- Earth Spikes: keep the 3D spike geometry but taller and irregular (6 spikes x 4 quads = 24), crack_wide strip, 6 dust billows (3 depths), 6 pebbles + 6 shadows (shadows only for the near 4). Total 40.
- Ground Wall: 4 tall slabs (4 x 5 = 20, textured stone with strata) rising on easeOutCubic, 10 dust billows rolling sideways along the base in 3 depths, 10 pebbles, 2 cracks. Total 44.
- Divided Mother Earth: crack widening (4 quads), 5 slabs (25), 10 dust, 12 pebbles (+ shadows trimmed). Total 51.
- Witch Hunter Claws: ground buckles in a moving ridge of soil (6 quads), 14 pebbles flying, dust trail behind the ridge (8 billows), then 4 curved earth claws (3 quads each, 12 plus 8 side = 20), dust ring. Total 52.
- Rampaging Mother Earth: golem as a pile: 7 chunky rocks (5 quads each = 35), rising with dust sheet (12 billows), pebbles from joints (14), shock ring (4). Total 65.
Budget: pebbles use the `chunk` billboards with lighting baked into 3 tones (no per-frame shade computation). `ctx.seg` halves dust/pebble counts at distance; the shadow decals are skipped beyond 24 blocks.
Risk: the dust billboards over alpha can overdraw heavily (fill-rate on weak GPUs): mitigate by capping each cloud at 2.5 blocks and not stacking more than 12; "volumetric" is faked, can look blurry compared with the cel anime; tan dust conflicts with the grey cel puff in the anime stills. Replaces nothing but the five Earth look-alikes.

---

## Direction C: "Hybrid" (cracked-ground decals + rock slabs with glowing mana veins)

Look: mixes anime rim light and mana. A big readable ground decal (the crack, the sigil, the ring) does most of the storytelling and costs 1 to 3 quads; the rock slabs are chunky with an anime plate look from A but each plate has a thin glowing vein in the spell colour (the existing page colour `0xFFFFB45A` gives amber; Earth family acid green `0xD2E65A` and the soil gold are the mod's own palette). Mana makes it read as magic (the owner's existing spells all carry glow), and the vein is additive so it costs no extra mesh.
Textures (Pillow):
- `earth2_decal_crack` 256 px: radial crack star with dark core, lit edge, 3 forks (used flat on the ground, 1 quad).
- `earth2_decal_ring` 256 px: rune ring segments (`earth_family` circle recoloured), dark soil `#3A2412`.
- `earth2_slab_vein` 64 px: plate face dark grey `#5A5F70` with 1 to 2 jagged vein polylines `#FFB45A` (additive layer on top, same UV, 1 extra quad per plate only for the 4 nearest).
- `earth2_dust_cel` : 2-tone puff from A, but tinted by soil.
- `earth2_mote` 16 px: glowing dot `#FFE08C`.
Palette: slab `#6E7488 / #50566A / #353A4A`, vein core `#FFE08C`, vein glow `#FFB45A` (page colour), decal dark `#2A1C12`, crack lit edge `#C9A05A`.
Motion: 1) anticipation: the decal draws itself outward in 3 ticks (UV scroll), 2) slabs rise 0 to 100% with overshoot 108% over 5 ticks (easeOutBack) and the veins flash white then amber (fade over 10), 3) dust cels roll out and fade, 4) veins pulse once (`0.6 + 0.4 sin(2 pi t / 10 ticks)`), 5) slabs sink back in the last 8 ticks and the decal fades.
Per effect (quads):
- Earth Spikes: decal line (1) + 7 plate-columns (7 x 4 = 28) + veins on nearest 3 (3) + 4 motes + 4 dust. Total 40.
- Ground Wall: decal ring base (1), crack (2), 5 plates (5 x 5 = 25), veins (5), 6 dust, 6 shards. Total 45.
- Divided Mother Earth: decal fissure (3) + 6 plates (30) + veins (6) + 6 dust + 6 motes. Total 51. Option golems: 3 small veined golems (3 x 10 = 30).
- Witch Hunter Claws: decal star (1) + ridge (6) + 4 claws x 2 plates (40) + veins (4) + 6 dust. Total 57.
- Rampaging Mother Earth: golem of 8 veined plates (40), veins (8), decal wake along the path (3), dust (10), shards (8). Total 69. The golem's chest vein glows brightest (the "core").
Risk: the glow can conflict with canon (anime earth magic has no glow in the sources I could read; unverified) and with the family colours (Gnome plate acid green vs amber); mana veins make Earth look like Fire/Lightning at a glance (mitigate: veins thin, colour cooler `#FFE08C` core, grey slabs). Slightly more overdraw (additive layer). This is the most "magic-looking" but the least canon.

---

## Comparison

| | A cel | B volumetric | C hybrid |
|---|---|---|---|
| Matches owner art (Ideal_Closer etc.) | best | partial (dust only) | good (plates) |
| Readability from far | best (flat silhouettes) | medium | good (decal) |
| Worst-case quads | 70 | 65 | 69 |
| Fill-rate risk | low | highest | medium |
| Canon fit (no glow) | yes | yes | no (glow added) |
| New textures | 6 | 8 | 8 |
| Effort | medium | medium | highest |

## Recommendation

A for Spikes, Wall, Fissure, Claws and the Golem (it follows the owner's own earth stills), with B's soft shadow decal and dust billows under it as an optional layer, and C's ground decals (not the veins) for the zone and burst. If the owner wants magic glow, take C. The one decision needed from the owner: should Claws be a MOLE, Rampaging a GOLEM and Divided Mother Earth three golems (canon) or keep claws / slabs / fissure (current)?

## Not done in phase 1

No code, no textures, no config key, no preview of the new looks. Paths and quad numbers above are plans: counts must be re-measured in `preview.py` when built (mark "measured" then). Nothing was seen in a game.
