# Phase E pilot 1 — Asta (hero NPC), "anime cel" style

Status: **preview only.** The model, its clips and its texture exist as resource files and render in `tools/render_preview`; nothing registers, spawns or draws
them in the game yet (the renderer and the engine come after the owner signs off on the style, see `remake_plan.md` section 8).

## New or replacement (rule 5)

| Part | New / replacement | Note |
|---|---|---|
| `anime_asta` geo, animation, texture (+ glow) | **NEW** | next to the old one, nothing removed |
| `arc_asta` (old model, texture, renderer, entity) | **KEPT** | stays registered and drawn until the owner says "replace it" (rule 2) |
| `tools/gen_anime_asta.py`, `scenes.d/anime_asta.json` | **NEW** | the generator and the preview scenes |

## What it is

- 76 cubes, 21 bones, about 300 quads per pass, a 512 x 192 texture (5.7 KB PNG); 2464 vertices in the preview against 3504 for the old model.
- About 4 heads tall: an 8 px head on a 33 px body (about 2.1 blocks with the hair spikes).
- Style: flat colour, one shadow band, a one-texel darker outline, one cel-shaded tone per face; five hue families (skin, ash-blond, white, navy/blue, black with
  gold).
- Hair is flat triangle cards (the alpha cut is the shape): 10 crossed pairs on the crown and 6 hanging bangs. The cape is two swinging pieces with a
  Black Bulls crest; the Demon-Slayer is a back sword and a hand sword that the attack clip swaps at 0.12 s.
- The face is a bare head plus five thin plates (calm / grin / shout / hurt / blink). Clips show one plate at a time by scale (instant, 0.02 s apart).
- Clips: idle (with a blink), walk, run, attack (draw and slash), talk (mouth flaps, one hand gestures), wave, hurt, death.

## Cost (rule 11)

Preview only, so zero game cost today. If it is wired later, the worst case per drawn Asta is about 300 quads per pass and 21 bone transforms per frame, one
texture bind, no per-frame allocation (the clips are baked keyframes). Face plates are scale tricks, not extra draws of a new texture.

## Look sources (rule 10)

- English wiki first: the Black Clover fandom wiki host was **not reachable** from this session (`getaddrinfo ENOTFOUND blackclover.fandom.com`).
- The look (ash-blond spiky hair, green eyes, black headband, short-sleeved outfit, a black sword carried on the back, the Black Bulls crest on the cape) comes from
  web-search summaries of the fan wiki, Wikipedia and figure listings, not from the wiki pages themselves. The Japanese wiki was not tried.
- Not fetched at all: the wiki pages themselves. The colours and the exact outfit (blue vest over a white tunic) are an interpretation, not a trace.

## Not included

- No entity, renderer, spawn, dialogue or AI wiring; no sound.
- No second Asta form (Black Asta, demon-slayer mode) and no sword glow.
- No in-game check: the preview is a software rasteriser, the real game will light it differently.

## Re-making it

`python3 -B tools/gen_anime_asta.py` rewrites the three resource files and the scene file; `python3 -B tools/gen_anime_asta.py --preview` renders the scenes into
`$ZOG_SCRATCH/previews_anime_asta`. The contact sheets are in `docs/mobs/phaseE/asta_*.png`.
