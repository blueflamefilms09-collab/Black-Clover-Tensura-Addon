# Phase E pilot: the hell hound ("anime cel" four-legged model)

Status: PREVIEW ONLY. Nothing is registered, no Java changed, no gameplay changed, no old file edited or deleted (rule 2).
All files are new and prefixed `anime_hound` / `gen_anime_hound`. The Time Magic VFX files were not touched (rule 3).

## What the old model was

The dungeon pup / direwolf (`arc_dungeon_direwolf.geo.json`, 71 cubes, 324 quads) and the foundry pup (`foundry_bramble_pup.geo.json`,
52 cubes, 215 quads) are both humanoid art-kit rigs: two legs, two arms, a wolf head with ears and a tail. Standing on two legs, they read as
"a person in a wolf costume" (see `docs/mobs/samples/sample_foundry_creatures_are_people.png`). They stay in the mod unchanged.
This is a REPLACEMENT proposal (rule 5): if the owner approves, the hound would replace the humanoid pup/direwolf look; until then it is only previewed.

## What was built (NEW)

A real four-legged hound, about 1.1 block at the shoulder (the shoulder line sits at 17.5 px of 16 px per block, the withers flames above it):
charcoal-slate fur, ember-orange cracks and underglow on legs, torso and tail, a bone-plate collar with spikes, a mane of flame cards on
the neck and spine, a three-segment spiked tail with a flame tip, pointed ear cards with ember insides, glowing eyes, visible fangs, claws.

Face plates (rule: bones toggled by scale): `face_open`, `face_narrow`, `face_closed`, `face_glow`, `face_snarl`. Each holds two eye cards
(one per facet of the skull). Scale 1 = shown, 0.001 = hidden. The clips switch plates with 10 ms steps (idle: open with a closed blink, walk: open, run: narrow, bite: snarl for 0.2-0.7 s,
pounce: glow, howl: closed, hurt: closed then open, death: open then closed, sleep: closed). The extra clips `pose_face_<name>`
exist for previews only.

Clips in `anime_hound.animation.json`: idle (3.2 s loop), walk, run, bite (0.8 s, snap at 0.64 s), pounce (1.2 s), howl (3.0 s),
hurt (0.45 s), death (2.0 s, held), sleep (4.0 s loop).

## Files

| File | What |
|---|---|
| `tools/gen_anime_hound.py` | the generator: skeleton, cubes and cards, packing, painting calls, clips, preview scenes |
| `tools/anime_hound_paint.py` | the cel painter (palette, materials, outlines, glow) used by the generator |
| `tools/anime_hound_sheets.py` | PIL contact sheets into `docs/mobs/phaseE/` |
| `tools/render_preview/scenes.d/anime_hound.json` | preview scenes (generated) |
| `assets/nusmp/geo/entity/anime_hound.geo.json` | LOD0 |
| `assets/nusmp/geo/entity/anime_hound_lod1.geo.json`, `_lod2.geo.json` | LOD1 / LOD2 (same skeleton, same texture) |
| `assets/nusmp/geo/entity/anime_hound_fx.geo.json` | glow-only subset (eyes, flames) for a separate fx pass |
| `assets/nusmp/textures/entity/anime_hound.png`, `anime_hound_glow.png` | base sheet and glow sheet |
| `assets/nusmp/animations/entity/anime_hound.animation.json` | the clips |
| `docs/mobs/phaseE/hound_views.png`, `hound_faces.png`, `hound_clips.png`, `hound_lods.png`, `hound_old_vs_new.png` | the contact sheets |

## Measured numbers

| | cubes + cards | quads |
|---|---|---|
| LOD0 | 176 (65 cubes, 111 cards) | 423 |
| LOD1 | 97 | 344 |
| LOD2 | 46 | 188 |
| fx subset (glow-only) | 31 | 31 |
| OLD arc_dungeon_direwolf | 71 | 324 |
| OLD foundry_bramble_pup | 52 | 215 |

Budget: the brief asked for 600-900 quads at LOD0 and far under 1500 cubes x 6. The hound is 423 quads, well inside. Cards are single quads,
drawn from both sides (no-cull), which is why 111 cards cost so little.

Bones: 39. Texture: 256x256 PNG8 palette, 8.2 KB; glow sheet 3.9 KB. Geo (minified): 40 KB / 27 KB / 15 KB / 8 KB. Animations: 332 KB
(keys at 30 per second for the gait clips). All hound assets together: 435 KB (limit 1.5 MB). The sheets in `docs/mobs/phaseE` are about
2.7 MB in total, but they are docs, not shipped assets.

## Worst-case cost (rule 11)

- Per hound per frame: LOD0 423 quads, plus the glow pass, which redraws the whole geometry (423) additively, so about 850 quads. With the
  fx-only model for the glow pass instead (not the default here), the second pass would be 31 quads. Per-frame allocations: none added by this
  model; the animation is a fixed set of keys.
- LOD1 (344 + glow) and LOD2 (188 + glow) are meant for mid and far distance; the engine picks them by distance, as for the other GeoLite entities.
- Worst case with 20 hounds in view at LOD0: 20 x 850 = 17,000 quads per frame. At the LOD split (5 at LOD0, 15 at LOD2) about 7,100 quads.
  Effects of far hounds should be skipped by the existing distance culling.
- Server cost: none. No entity, no packets, no ticks. The model is client art only; it is not wired in, and any later wiring must keep the
  server decide the state (clip and plate come from the entity state, never from client logic).
- Memory: one 256x256 texture and one 256x256 glow texture (about 0.5 MB in VRAM together), shared by all hounds and all LODs.

Open point for wiring: the hit box stays about 1.0 x 1.2. The model is longer than that (nose to rump about 41 px, about 2.6 blocks), so it overhangs
the footprint at the front and back. Ear tips and the tail flame reach about 29-31 px (about 1.9 blocks), above the 1.2-block box. A shorter model would
fix the length but lose the dog proportions; the owner decides.

## How to regenerate

```
python3 -B tools/gen_anime_hound.py                    # geo (3 LODs + fx), sheets, animations, preview scenes
python3 -B tools/gen_anime_hound.py --preview          # render all scenes with the software rasteriser (about 1-2 min)
python3 -B tools/anime_hound_sheets.py                 # labelled contact sheets into docs/mobs/phaseE/
```
`HOUND_SCRATCH` sets where the renders go (default: a scratch folder). The generator uses its own `RENDER_PREVIEW_BUILD` folder,
so it does not touch the shared preview build.

## Art-direction iterations (honest log)

1. First pass: shirtless-humanoid read gone, but the body was three separate boxes with seams, the collar sat inside the chest, the neck was invisible,
   ears were thin slivers. Fixed: a chamfered torso (barrel plus chest and hips sections), a longer neck with the head shifted forward, wider ear cards.
2. Second pass: fur read purple and flat, glow turned the flames white, forehead dots looked like crosses. Fixed: a lighter slate palette plus a highlight tone,
   flame glow mapped one band darker, a forehead zigzag crack instead of dots, double crossed flame cards for a fuller mane.
3. Third pass: muzzle read as a pale slab, hind thigh was a huge square, jaw/muzzle proportions. Fixed: muzzle side faces use the fur tone with only a pale bridge
   stripe on top, thinner thigh, a bigger muzzle and jaw. The animation clips were all rendered and checked (bite, pounce, howl, death, sleep read correctly).

## Honest limits and weaknesses

- Everything was judged in the repo's software rasteriser (`tools/render_preview`), which is not the game: no lighting shader, no real glow bloom,
  no real entity renderer. Nothing was seen in a game. The additive glow look in the game may differ (flames may be brighter or whiter).
- The silhouette is still boxy. It reads as a hound, but it is a blocky one: legs are straight slabs, the torso is a chamfered box, and the neck
  is short. It is closer to a "cel-painted voxel hound" than to a flowing anime beast.
- The head is a hexagonal block set on the neck, and from the front the snout reads as a dark box (the muzzle front, nose and the open mouth interior merge).
  A tapered snout (rotated or scaled cards) would help.
- The shoulder and collar area is still busy: bone plates, spikes, tufts and a ruff overlap.
- The cel look works best on the legs, ears, mane and eyes; the big charcoal torso is low-contrast and flat.
- Pounce and run use a fixed pose set; the legs fold with simple limb rotations, so there is no real muscle motion.
- The old pup and direwolf are not replaced anywhere; wiring them (renderer, entity, spawn tables, hit box) is out of scope here.
