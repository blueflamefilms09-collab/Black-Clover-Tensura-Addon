# Phase E pilot: Goliath in the anime cel style (preview only)

Status: PREVIEW ONLY. New files, nothing registered, no Java or gameplay change, nothing seen in a real game.
The design follows `docs/mobs/remake_plan.md` sections 4 and 5 (cubes and cards only, painted outline, limited palette, glow sheet).

## What the old Goliath looked like

- `goliath_body` (the stone Goliath of the floors): 38 cubes, 189 quads per pass, about 101 px (6.3 blocks) tall. A tall, slim tower of grey cobble
  texture with moss splotches and a horned head. Photographic noise texture, no outline, no strong silhouette; it reads as "a statue of cobblestone".
- `arc_goliath` (the arc kit's Goliath): 33 cubes, 189 quads per pass, 96 px tall. A pale humanoid with moss trims and a face painted with a dithered gradient.
  It is not a golem at all and has no stone read.

Both stay untouched. The new model is added next to them (rule 5: it is a replacement candidate for the visuals, but nothing is swapped in this pilot).

## What was built (all NEW files, prefix `anime_goliath` / `gen_anime_goliath`)

An original towering mossy stone golem:

- heavy shoulders: a twisted three-tier "pagoda" pauldron on each side with a flared outer guard, a front hood plate, two sweeping horn cards and two thorns;
- fists 1.5x the size of the old ones: palm, hand plate, four chunky fingers, thumb; forearm bracers with a rune, cuffs, elbow spike cards;
- a small head (jaw, grille muzzle, brow ridge, crown, cranium, ear plates) with a glowing amber eye slit under the brow and a five-card shard crest;
- a V torso: abdomen, two stacked rib slabs, an offset chest plate with a glowing sigil, yoke and collar, a back plate with a second sigil, a spine ridge with spike cards;
- belt, buckle with a small sigil and a stone loincloth of five hanging plates; legs of thigh, thigh plate, hip plate, kneecap with rune, two greaves, a three-toed foot;
- glowing rune cracks on the chest, arms, bracers, kneecaps and loincloth (glow sheet);
- moss and vines as alpha cards (shoulder tufts, back tufts, two vines per forearm), drips of moss painted on the plate edges;
- a rubble ring of 10 stones around the feet, a 7-stone trail behind, and 4 stones orbiting the shoulders;
- rest pose has a hunch (chest -9, head +8, arms forward) so the silhouette is a hunched bruiser, not a statue. Nothing is a smooth sphere; everything is a cube, a notched slab or a card.

Palette: warm grey stone ("slate"), teal-slate plates ("basalt", chosen to separate plates from body and to read at distance), sand/ivory blocks,
moss and vine greens, amber glow. Outline tones are the hue-shifted dark of the local colour, never pure black. Every face is painted at 4x on a palette-index
image and reduced by the most common colour, so no blended or dithered texels exist. Shading is three cel bands per hue family plus a rim light on the lit edge.
Front, left and back faces are treated as lit, the right face as shadow (the game renders with FlatLit, the preview wrapper `tools/anime_goliath_render.py` mimics that).

## Numbers (measured by `tools/gen_anime_goliath.py`)

| item | value |
|---|---|
| cubes | 165 (cards are 0.5-thick cubes with only their front face) |
| quads per pass | 619 (boss LOD0 budget in the plan: 1500 to 2500) |
| bones | 32 |
| albedo texture | 1024 x 512, palettised PNG8 with a transparency table, 25 colours, 27 KB |
| glow texture | 512 x 256, palettised, 5 colours, 1.7 KB (same UVs, half size) |
| geo json | 133 KB |
| animation json | 1.29 MB (dense Catmull-Rom sampled keys, see limits) |
| total Goliath assets | about 1.45 MB, under the 2.5 MB budget |
| model height | 107 px in the file (6.7 blocks) before the rest hunch lowers it, about 6.5 blocks on screen; the hit box (6 blocks) is unchanged, so about 8 percent taller |

Clips (gen_veldanava format, loop flags set): idle (8 s loop), walk (2 s loop), slam (2.2 s), stomp (3 s, twin stomp), roar (2.4 s), rise (2.4 s, root sinks to y -26 and climbs out),
hurt (0.6 s), death (3 s, pitches forward and collapses), transform (3.4 s, scales up to 1.07), attack (1.2 s). Secondary motion (moss, vines, tassets, crest, ridge, orbit stones) lags the body.

## Worst-case cost (rule 11)

- Client draw: 619 quads in the base pass and the same mesh again in the glow (eyes) pass = 1238 quads per frame for one Goliath, drawn only for the boss that is in view.
  There is one boss per arena, so even 4 Goliaths on screen are about 5000 quads. No per-frame allocation is added: the model is the existing GeoLite path, the animation is a sampled table.
- Animation evaluation: 32 bones, linear interpolation of the sampled keys, as for every other GeoLite mob.
- Server: none. No Java, no packets, no entities, no ticking code. The model is a pure client asset.
- Memory: one 1024 x 512 texture (2 MB in VRAM uncompressed) plus a 512 x 256 glow texture (0.5 MB).
- Not covered by this pilot: LOD1 and LOD2 (the plan wants a far LOD). A far version is a separate step; see limits.

## Regeneration

```
python3 -B tools/gen_anime_goliath.py                      # geo, textures, animations, scenes
python3 -B tools/gen_anime_goliath.py --preview            # render every scene (software rasteriser)
python3 -B tools/gen_anime_goliath.py --preview anime_goliath_slam anime_goliath_front   # some scenes only
python3 -B tools/gen_anime_goliath.py --sheets             # compose docs/mobs/phaseE/goliath_*.png
```

Files: `tools/anime_goliath_kit.py` (palette, cel painters, decals, cards), `tools/gen_anime_goliath.py` (the model and writers),
`tools/gen_anime_goliath_clips.py` (the clips), `tools/gen_anime_goliath_scenes.py` (preview scenes), `tools/gen_anime_goliath_sheets.py` (contact sheets),
`tools/anime_goliath_render.py` (flat-light wrapper of `tools/render_preview/preview.py`), `tools/render_preview/scenes.d/anime_goliath.json` (generated).
The generator reuses `tools/geo_builder.py` and `tools/zogratis_lucius_kit.py` (Pack) read-only. Set `RENDER_PREVIEW_BUILD` to a private folder when several agents render at once.

## Sheets (docs/mobs/phaseE)

- `goliath_views.png`: front, 3/4 left, back, front 3/4, 3/4 right, side.
- `goliath_closeups.png`: head front and 3/4, chest, fist, and the far read.
- `goliath_poses_a.png`: idle, walk, slam, stomp. `goliath_poses_b.png`: roar, rise, death, transform. `goliath_poses_c.png`: hurt, attack.
- `goliath_old_vs_new.png`: OLD goliath_body and OLD arc_goliath beside the NEW one, front and 3/4, far read, and the old and new slam strips.

## Honest limits

- Everything was judged in the repo's software rasteriser (`tools/render_preview`), not in the game. Real lighting, mipmapping, shader glow and the in-game camera may differ.
- Only the base (LOD0) model exists. No LOD1 or LOD2 and no separate FX model, which the plan needs before any in-game use.
- The animation file is large (1.29 MB) because the clips are sampled densely; a keyframe-reducing pass would cut it to a fraction before shipping.
- Nothing is wired: no entity, renderer, registry or lang entry points at `anime_goliath`. The `attack` clip is an extra, the plan did not ask for it.
- Engravings (rule 6) do not apply, this is a mob and not a weapon.
- The quad budget (619) is far below the boss LOD0 range (1500 to 2500), so there is room for more silhouette (see below).
- Known art weaknesses are listed in the report: the silhouette is still boxy and wide, the top faces of plates are very pale, and the torso front is plain around the chest plate.
