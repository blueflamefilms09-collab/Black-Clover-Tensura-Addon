# Skill icon study (stage 1)

Why: the owner said the skill icons "look like a 4-year-old drew them" and pointed at the Tensura mods as the quality bar (Tensura No Index for the animated ones). This page records what was measured in those mods and in ours, so the redesign (docs/skill_icons_v3.md) rests on numbers and not on taste.

The other mods are style references only. No pixel, file name or picture of them is in this repo. The side-by-side sheet made from them (`/tmp/claude-0/scratch_ref/sheets/study_compare.png`) is deliberately not committed, because it holds their pixels.

## Mods looked at

Tensura Reincarnated itself (the base 32 px skill frames), Tensura No Index, Tensura Ascensions, Tensura Mysticism, Tensura More Skills, Elite Tensura, Tensura Isekai Evolved Addon, Minesur Addons, Tensura Hardcore NDC, Devil x Slime. Also opened for layout only: Tensura Origins, Restricted, Blade, Leon Boss, More Overlord, Nightmares, Date a Slime.

## Measured (colours per icon, median)

| set | icons | colours / icon |
|---|---|---|
| Tensura base | 214 | 82 |
| No Index (76 px sheets, read as a 38 grid) | 175 | 204 |
| Ascensions | 57 | 122 |
| Mysticism | 58 | 128 |
| More Skills | 17 | 239 |
| Elite Tensura | 29 | 168 |
| Isekai Evolved | 29 | 10 |
| Minesur Addons | 20 | 110 |
| Hardcore NDC | 39 | 87 |
| Devil x Slime | 37 | 18 |
| **ours before: root** | 146 | **588** |
| **ours before: grimoire** | 107 | **835** |
| **ours before: buff / ultimate** | 77 | **833** |
| ours after: root | 146 | 73 |
| ours after: grimoire | 107 | 58 |
| ours after: buff / ultimate | 236 | 83 |

Reference band: 10 to 240 colours, most between 80 and 200. Ours before were three to eight times above the top of it.

Edge and flat shares (same measure for before and after, from `tools/icon_forge_v3`, threshold on luminance step between neighbouring pixels: strong > 40, soft 6 to 40, flat <= 6). Reference values from the first measuring script used a slightly different cut, so only the before/after columns below are comparable with each other:

| set | strong edge | soft (smooth gradient) | flat |
|---|---|---|---|
| root before | 0.52 | 0.30 | 0.17 |
| root after | 0.62 | 0.20 | 0.18 |
| grimoire before | 0.63 | 0.34 | 0.03 |
| grimoire after | 0.54 | 0.29 | 0.17 |
| buff / ultimate before | 0.68 | 0.30 | 0.02 |
| buff / ultimate after | 0.63 | 0.23 | 0.14 |

Reading: the old icons were almost all "soft" pixels with practically no flat areas (grimoire 3 %, buff 2 %). The reference mods have 25 to 70 % flat pixels and a clear outline.

## What the references do (layout and craft)

- Native pixel art on the 32 px grid (No Index draws at about 2x and keeps hard pixels). Nothing is drawn smooth and shrunk.
- A single strong silhouette, 1 px darker outline, 3 to 4 flat tones per material, light from the top-left, a lit edge on the lit side.
- One glyph per skill, big, centred, readable at 16 px. Small secondary marks (stars, sparks) only as accents.
- Flat or banded backgrounds. No gradients, no film grain.
- Rank is shown by a frame or a small plate, not by tinting the whole picture.
- Animated icons (No Index): the loop is a glint sweeping over the symbol, a twinkle, a pulse of the glow, or rising embers. 8 to 16 frames, frametime 2 to 4 ticks, `interpolate` false so frames stay hard.
- Tensura base frames: 3 px frame, 1 px bright ring on the outside, rounded corners with a transparent corner pixel. Ours already used this and it is kept.

## Why ours looked childish (diagnosis)

1. The pipeline painted smooth at 128 or 256 px with blurs and bevels, then box-downsampled to 32 px. That gives 590 to 840 colours of mush: every edge is blurred, the outline is lost, small parts merge.
2. No flat areas, so no shape language: the eye sees noise.
3. Smooth gradients across the background fought with the glyph for contrast.
4. The animated Veldanava medallions were the 128 px art box-filtered to 32 px: the ring and the emblem blurred into one another.

## What the redesign changed (measured result)

Pixel-art render on the 32 px grid, hard mask at 0.45, 4 flat tones from the top-left, selective 1 px outline, separation lines between parts, rim light, banded backgrounds, star sparkles, thin 3 px frames with a 1 px bright ring, rank pips in the Grade colours. Median colours fall from 588 / 835 / 833 to 73 / 58 / 83, inside the reference band. The strong-edge share is above the references for root icons, which is deliberate (the outline), the soft share is down by a third to a half.

Honest gap that remains: contrast and saturation are still below No Index, and root glyph geometry is reused from the old hand-built symbols (re-rendered, not redrawn).

Sheets: `docs/skill_icons_v3/*_before.png` and `*_after.png`, `animated_frames_*.png`.
