# HD outfits, group b (Mr. 2 Bon Clay, Malty Melromarc, Rachel, Jugram Haschwalth)

Owner request: "make the outfits HD", plus the hair rework. Group b is painted with the HD pipeline of `docs/bosses/hd_outfits.md` (256x256 skin, every model pixel a 4x4 block) and the hair
styles of the hair rework (`docs/bosses/anime_style_v2.md`, hair section). Method: `tools/tier_bosses/hd/<id>.py`, the colour blocking of the 64x64 remake (`docs/bosses/remake_b.md`) stays,
HD adds detail and does not redesign the characters.

Regenerate: `python3 -B tools/tier_bosses/gen_tier_looks.py malty_melromarc rachel jugram_haschwalth` (Bon Clay is the pilot module, regenerate with `mr_2_bon_clay`). 64x64 versions stay reachable
with `--size 64` (the remake modules are untouched).
Previews (software renderer, no game): `docs/previews/tier_style_v4/b_front_back.png` (front and back), `b_close.png` (three-quarter bust), `b_trio.png` (front, back, bust per character).

## New or replacement (CLAUDE.md rule 5)

| item | status |
|---|---|
| `tools/tier_bosses/hd/malty_melromarc.py`, `rachel.py`, `jugram_haschwalth.py` | NEW |
| `malty_melromarc.png`, `rachel.png`, `jugram_haschwalth.png` (256x256) | REPLACE the 64x64 PNGs (owner asked for HD); the 64 painters stay in `remake/` |
| `malty_melromarc_glow.png`, `jugram_haschwalth_glow.png` | NEW (rubies; Quincy cross and belt gem) |
| `looks_tier.json`: Malty `glow: true`, Jugram `glow: true`, `hair_color` of Jugram | CHANGED by the generator |
| Jugram hair style `anime_long_open` (the hair contract suggested `anime_bangs_long`, rejected, see below) | KEPT style, colour `#fff8e0` -> `#faf0c4` |
| Bon Clay (`mr_2_bon_clay`) | KEPT, checked: `anime_afro_wide` is attached to the head (side puffs touch it), hair colour `#2a2a3e`, the afro does not cover the eyes or the eyeshadow |

## Hair choice per character (checked in the preview with the real new hair, front, back, three-quarter)

| id | hair style | colour | why |
|---|---|---|---|
| mr_2_bon_clay | `anime_afro_wide` | `#2a2a3e` | big attached afro, strong silhouette (pilot's choice, fits) |
| malty_melromarc | `anime_long_open` | `#c8141f` | long hair framing the face, parted fringe: the gold tiara and ruby show through the part |
| rachel | `anime_long_open` | `#7a4528` | long chestnut hair; the white ribbon band shows between the fringe locks |
| jugram_haschwalth | `anime_long_open` | `#faf0c4` | long pale-blond hair with a parted fringe; the hair contract suggested `anime_bangs_long`, which I tried and rejected: in a pale colour it reads as a short messy mop (tousled spikes), not Haschwalth's long sleek hair. `anime_long` and `anime_long_open` both work, `_open` shows the forehead |

Nothing floats in any of the three: the long styles hang from the cap. Report to the hair agent: `anime_bangs_long` in a very pale colour loses its clump structure and looks like short spiky hair.

## Malty S. Melromarc (The Rising of the Shield Hero)

* Face: sharp red eyes with a raised outer corner, long lashes with a flick, rose eyeshadow, thin high arched brows, a red-lipped smirk (bowed upper lip, glossy lower lip, a smirk corner).
* Hat layer: gold tiara band round the head, a peak, two side points, a ruby, filigree studs (glows).
* Dress: crimson satin bodice with princess seams, a skin V neckline with a gold gorget and a ruby brooch, a cream brocade tabard with gold piping and stitching, a gold belt with a ruby buckle and a gold
  cord and tassel, a pleated short skirt with tone-on-tone damask and a gold scalloped hem (on both layers, so it stays beyond the 24-block LOD cut).
* Mantle (back and sides): garnet velvet with deep folds and damask, an ermine collar with dark tails, a gold diamond crest with a ruby, a gold cinch belt, a gold scalloped hem. The long hair hides most of
  the back in the preview.
* Arms (slim): crimson puffed shoulders with gold rims, cream sleeves with gold buttons, a lace ruffle, gold cuffs with a ruby, bare hands with crimson nails.
* Legs: wine thigh-high stockings with a sheen, gold garters with a stud, maroon boots with gold lacing and gold folded cuffs, dark soles.
* Glow: tiara ruby, collar ruby, belt ruby, crest ruby (about 2 KB glow map).

## Rachel (Tower of God)

* Face: round amber-brown eyes with a heavy lid shadow (the cold look), flat brows, a thin straight mouth, no blush; a white satin hair ribbon band with a bow on the right side of the head (hat layer).
* Dress: pale cyan fitted satin with princess seams and a faint tone-on-tone damask, a white stand-up collar, a cascading cyan bow, a white satin tabard with bright-cyan piping, stitching and a chain of
  diamonds, a white pointed lace capelet with a scalloped border and pearls (front and a round back one with a big bow at the waist), a charcoal leather belt with a silver clasp and a cyan stone,
  a flared pleated skirt ending in a white scalloped petticoat (both layers).
* Arms (slim): cyan sleeves, white shoulder tabs, big double white ruffle cuffs with a cyan trim line.
* Legs: charcoal leggings with a bright-cyan side line, slate boots with two straps and silver buckles, white folded cuffs, dark soles.
* Glow: none.

## Jugram Haschwalth (Bleach)

* Face: calm sharp pale-blue eyes with thin lashes and a cool lid shade, pale ash brows, a cold thin mouth.
* Body: black vest with a glowing blue Quincy cross (a long bar, a short bar, pointed flared tips, a ring and a white core), white open coat with blue piping, topstitching, folds, silver buttons and a chain of blue
  diamonds, an upright white collar with blue trim, a steel belt with rivets and a blue gem buckle, the coat back with a vent and a faint embroidered cross. The coat edges, back and sides are on the base
  layer too, so the figure stays white-coated when the second layer is dropped.
* Arms (wide): layered round-bottomed steel pauldrons (rib, blue enamel diamond, rivets) over a dark mail joint, white sleeves, a blue cuff band, bulky riveted steel gauntlets with plate seams and a knuckle plate.
* Legs: charcoal trousers, white coat tails with a blue hem (the inner column of each front face open), flared steel boots with a lip, plates, rivets, a toe cap, a blue line, dark soles.
* Glow: the Quincy cross and the belt gem (about 1.5 KB glow map).

## Sources (rule 10)

English wiki first, Japanese as the fallback. `shieldhero.fandom.com`, `bleach.fandom.com` and `towerofgod.fandom.com` could not be fetched in this session (the hosts do not resolve: they are outside the
session's allowed network domains); per the tool's advice no further sites (Japanese fandom, ja.wikipedia) were tried. **All looks are from memory** of the canon designs plus the colours of the sheets in
`tier_specs.py` and the earlier remake notes. Rachel is still the least certain (the series is an assumption from the sheet). Nothing was traced; all art is original.
The wikis that need to be allowed to check the designs: shieldhero.fandom.com, bleach.fandom.com, towerofgod.fandom.com (and the ja versions).

## Cost and bounds (rules 9 and 11)

* No runtime code changed, no packet, no tick work: these are client resource-pack PNGs, like every skin.
* Texture memory per boss when it is loaded: 256 KB skin, plus 256 KB for the glow map of Malty and Jugram (Rachel has none). Worst case 0.5 MB each, loaded on first sight only.
* Draw cost: identical geometry (same quads and draw calls), only the texture is sampled finer. Hair and overlay are dropped beyond 24 blocks (`PlayerLod`); the base layer carries the skirt, the coat and the
  main colours so the characters still read from far away (checked with a base-layer-only render).
* Jar size: Malty 37 KB + 2 KB, Rachel 34 KB, Jugram 33 KB + 1.5 KB.
* Tool cost: about 1.3 s per character (numpy, Pillow), deterministic, nothing random.

## What was not seen in a game

* Only the software renderer (`tools/playermodel/pmrender.py`) was used: flat lighting, no shaders, a different anti-aliasing. How the GPU filters a 256 skin (NEAREST, no mipmaps) is reasoned, not observed.
* Pale hair (Jugram) renders greyer in that renderer than it will with real lighting; the colour was nudged warmer, not verified in game.
* Weaker than the owner references: the references use big contrasty hair masses and bulky, strongly outlined armour; here the silhouette is limited by the vanilla model (overlay layers are only 0.25 px
  thick), so the bulk comes from colour, bevels and shadows, not geometry. The long-hair styles hide the backs of Malty's mantle and Rachel's capelet.
