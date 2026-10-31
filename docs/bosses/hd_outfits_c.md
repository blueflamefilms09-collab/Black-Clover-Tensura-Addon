# HD outfits, group c (Big Mom, Code, Foxy, Ao)

Owner request: "make the outfits HD", plus the hair rework (no spiky-sucks hair, no floating hair). Group c is painted with the HD pipeline of `docs/bosses/hd_outfits.md` (256x256 skin, every
model pixel a 4x4 block) and the hair styles of the hair rework (`docs/bosses/anime_style_v2.md`, hair section). Method: `tools/tier_bosses/hd/<id>.py`; the colour blocking of the 64x64
remake (`docs/bosses/remake_c.md`, `tools/tier_bosses/remake/<id>.py`) stays the blueprint, HD adds detail and does not redesign the characters.

Regenerate: `python3 -B tools/tier_bosses/gen_tier_looks.py big_mom code foxy ao`. The 64x64 versions stay reachable with `--size 64` (the remake modules are untouched).
Previews (software renderer, no game, no real light): `docs/previews/tier_style_v4/c_front_back.png` (front and back of all four), `c_close.png` (three-quarter bust of all four).

## New or replacement (CLAUDE.md rule 5)

| item | status |
|---|---|
| `tools/tier_bosses/hd/big_mom.py`, `code.py`, `foxy.py`, `ao.py` | NEW (HD painters) |
| `big_mom.png`, `code.png`, `foxy.png`, `ao.png` (256x256) | REPLACE the 64x64 PNGs (owner asked for HD); the 64 painters stay in `remake/` |
| `big_mom_glow.png`, `code_glow.png`, `ao_glow.png` | REPLACE (now 256x256, same glow roles; Ao gains the Byakugan eye) |
| `foxy` glow map | none (as before) |
| `looks_tier.json`: the `hair` and `hair_color` of the four ids | CHANGED by the generator (table below) |
| `tools/tier_bosses/remake/{big_mom,code,foxy,ao}.py` and the 64x64 look | KEPT (rule 2) |

## Hair choice per character (checked in the preview with the real new hair: front, back, three-quarter)

| id | before | now | colour | why |
|---|---|---|---|---|
| big_mom | `anime_mane` | `anime_curls` | `#e8458c` | big round pink curls: the biggest silhouette of the four, attached to the cap, frames the face; the tiara is covered by the curls on top and shows at the sides |
| code | `anime_spiky` | `anime_spike_tall` | `#f2f5ff` | thick tall clumps, strong silhouette, nothing floats. `anime_spike_swept` was tried and rejected: from behind it read as a star-burst halo |
| foxy | `anime_afro` | `anime_afro_wide` | `#e8ecf6` | wide silver-white afro that touches the head on all sides; colour raised from `#d4d8e2` because the hair tint renders paint colours flatter and darker than the swatch |
| ao | `anime_short_open` | `anime_bangs_long` | `#1e244c` | dark blue-black clumped bangs; the steel headband plate shows between the bangs. Colour darkened from `#2a2f58` (the tint renders brighter than the swatch) |

Hair colours are tuned on the rendered result, not on the swatch. Nothing floats in any of the four (checked at 0, 45, 90, 180 and 270 degrees).

## Big Mom (One Piece, Charlotte Linlin)

* Face: wide mad violet eyes with long lashes and heavy plum eyeshadow, arched angry brows, a huge crimson-lipped grin with glossy lip, blush. Gold tiara band on both head layers with a ruby (glows).
* Body: hot-pink satin bodice with princess seams, a white lace scalloped collar with pearls, a big red heart brooch on a gold-trimmed tabard with a gold stud, a gold sash with a buckle and ruby,
  a plum skirt with gores, tone-on-tone damask and rows of pink hearts, a gold scalloped hem and a lace petticoat (on both layers, so the skirt stays beyond the 24-block LOD cut).
* Back: a big bow at the waist, the gores continue, hair hides the top.
* Arms: puffed pink shoulders with gold rims, lace cuffs, skin hands. Legs: plum skirt tails, white stockings, wine boots with gold cuffs (re-laid out so the lace does not hide them).
* Glow: tiara jewel, brooch stud, buckle and ruby (about 1 KB glow map).

## Code (generic armoured character, neon cyan)

* Face: sharp cold eyes with a dark-teal iris, cyan ring and white core (glows), flat brows, a thin line mouth, a forehead seal (partly under the fringe).
* Body: slate coat plates with a V neck collar, a tabard with a cyan neon frame and a column of four geometric rune glyphs (the first glyph looked like a letter, so diamond+bar, ladder, chevrons and
  H+mark were used), a hex buckle with a cyan gem, a skirt panel with a neon hem, a glowing ring crest with a rune on the back. Plates have only a low specular so the dark steel does not wash out.
* Arms: riveted pauldrons, sleeves, gauntlets. Legs: greaves, boots with cyan lines and soles.
* Glow: eyes, neon trims, buckle gem, back crest (about 5 KB glow map).

## Foxy (One Piece, Foxy the Silver Fox)

* Face: tan skin, narrow brown eyes, arched brows, a wide toothy grin, a silver moustache and a goatee (the moustache sits above the mouth so it does not cover the grin).
* Coat: blue-grey satin (gradient and sheen, no weave: the first weave read like denim) with panels, lining and orange scroll embroidery, white fluffy fur collar with notched tufts, an orange sash with a
  gold buckle, an opening cleared on the overlay so the shirt shows, coat tails with an orange hem.
* Arms: steel pauldron with a bevel, orange cuff, bare hand. Legs: coat tails, dark trousers, boots with orange cuffs.
* Back: a fox-head crest in orange and gold under the afro.
* Glow: none.

## Ao (Naruto, Ao of the Hidden Mist)

* Face: a Byakugan eye (lavender iris, faint veins, glow `#7a6ad8`, kept darker because additive glow on a light sclera went pure white), a bevelled steel-grey eyepatch with stitching and a strap over
  the other eye, a calm flat mouth. Steel headband on all four head faces (both layers) with a wave mark on the front and a knot on the back.
* Body: sky-blue coat with white piping and a stand collar, navy vest with a zipper, hip pockets, a navy obi, a wave ring crest on the back.
* Arms: steel pauldron plates, a navy undersleeve gap, a bracer. Legs: navy trousers, white bandage wraps on the shins, a steel ankle guard, dark boots with a white cuff.
* Glow: Byakugan eye, headband wave mark (about 2.4 KB glow map).

## What is still weaker than the references (owner pictures 154 to 156)

* Foxy's coat is still plainer than the reference coats; the satin and the scrolls help but it has no layered lining like Malty's mantle.
* Code's forehead seal and Big Mom's tiara are largely hidden by the fringe and curls (the hair is bigger now, as asked). Code's `anime_spike_tall` looks like a radial crown from behind.
* Ao's top tufts of `anime_bangs_long` look slightly frizzy in the software renderer.
* Nothing here was seen in a game: the software renderer has no real light, shaders or glow bloom. The hair tints may need a second pass in game.

## Sources (rule 10)

English wiki first, Japanese as the fallback. The wikis (onepiece.fandom.com, naruto.fandom.com, ja versions) could not be fetched in this session (no route through the proxy, `curl` returns no
answer). **All looks are from memory** of the canon designs plus the colours of the sheets in `tier_specs.py` and the remake notes. Foxy's and Ao's series are taken from the sheet; Code has no
clear canon source and follows the sheet (slate robe, cyan trim, cold sharp eyes, forehead sigil). Nothing was traced; all art is original.
Not fetched at all: the pages of Charlotte Linlin, Foxy, Ao (Naruto) and Code on the English and Japanese wikis.

## Problems found in shared tools (not touched, reported)

* `hd_detail.mouth_smile` with an open mouth (`open_h > 0`) raises `TypeError: ufunc 'hypot' not supported` for some widths (3.4, 3.8, 3.9 at centres 4.0 and 4.15): float rounding makes
  `1 - ((x - cx) / (w / 2)) ** 2` slightly negative and `** 0.8` turns it complex. Foxy uses width 3.7, which works. Fix in the shared helper: clamp that term with `np.maximum(..., 0)`.
* `gen_tier_looks.py` reads and writes the whole `looks_tier.json`. Several agents running it in parallel can overwrite each other. After the last run the four entries above were checked and
  the other characters' entries were still present.

## Cost and bounds (rules 9 and 11)

* No runtime code changed, no packet, no tick work: these are client resource-pack PNGs, like every skin.
* Texture memory per boss when it is loaded: 256 KB skin, plus 256 KB glow map for Big Mom, Code and Ao (Foxy has none). Worst case 0.5 MB each, loaded on first sight only.
* Draw cost: identical geometry (same quads and draw calls), only the texture is sampled finer. Hair and overlay are dropped beyond 24 blocks (`PlayerLod`); the base layer carries the skirt, the
  coat and the main colours so the characters still read from far away (checked with a base-layer-only render).
* Jar size: Big Mom 30 KB + 1 KB, Code 25 KB + 5 KB, Foxy 39 KB, Ao 37 KB + 2.4 KB.
* Tool cost: about 1.5 s per character (numpy, Pillow), deterministic, nothing random.
* Tests run: `tools/tier_bosses/hd/test_hdlib.py` (16 OK), `TierBossesTest` (12 passed, 0 failed), `PlayerModelDataTest` (12 passed, 0 failed).
