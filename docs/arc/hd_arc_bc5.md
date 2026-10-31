# HD arc characters, group bc5: the Zogratis family and Fana (hand-painted 256x256)

Group bc5 is ten looks: Dante, Vanica, Zenon and Lucius Zogratis, and Fana of the Eye of the Midnight Sun, each with the second form. The painters are `tools/arc_hd/chars/<look>.py`
(`paint(spec, cid, masks) -> (skin 256, glow 256 or None, hair style id, hair rgb, slim)`), the form-2 modules `<id>_form2.py` load the painter of the base look and swap the palette
(`K.sibling`). The private kit is `tools/arc_hd/chars/_bc5.py` (a copy of the bc4 kit plus a thin trim band, a sibling loader, bandage wraps, stubble, jagged cracks, a bevelled plate, a
spade, a glow eraser; the underscore keeps it out of the generator and the module test). The pipeline is the one of `docs/arc/hd_arc.md`: the 64x64 sheet is the blueprint (layout, colour
blocking and the second-layer coverage stay), the front of the head and every body island are redrawn at 4x detail, the other faces of the head are repainted clean as skin and scalp.
The hair sheets stay 64x64 (owner: no HD hair sheets). The old 64x64 skins are REPLACED under the same file names (`src/main/resources/assets/nusmp/textures/entity/playermodel/<look>.png`
and `_glow.png`), the generator keeps its 64 mode (`--size 64`).

Preview: `docs/previews/arc_hd/bc5_front_back.png` (front and back of all ten), `docs/previews/arc_hd/bc5_close.png` (the bust of all ten).

## Source (rule 10)

English wiki first, Japanese wiki as the fallback: neither could be fetched in this session (`blackclover.fandom.com` and the Japanese fandom give ENOTFOUND, the network allow-list of the
session blocks them; the owner should allow `blackclover.fandom.com` if a wiki check is wanted). So every look is **from memory of the series** plus what the repo already records
(`docs/arc/remodel.md`, `docs/arc/fana.md` with its wiki URLs, `tools/arc/char_*.py` specs): the cut and the colours are the sheet's choice, the canon points used are listed per
character. Not fetched at all: all ten wiki pages.

## What is new and what is replaced (rule 5)

REPLACED (same file names): the ten 64x64 skins and the four `_glow` maps that exist, now 256x256; the look fragments `tools/arc_hd/looks/<look>.json` (size, hair, hair colour; the extras
are kept as they were). NEW: the ten painter modules and the kit `_bc5.py`, this document, the two previews. KEPT: every extra (wings, horns, halos, weapons, cloaks), the slim and wide arms,
the height scale, the glow semantics, the hair sheets, `looks.json` (the lead merges the fragments).

## Dante Zogratis (and form 2)

Design: a broad, hard, tanned face with thick slanted brows, sharp green eyes, a wide nose, a wolfish grin with one fang, a moustache and a small goatee, a stubble shadow, a scar down the
left cheek and a diagonal scar over the right eye; a gold crown band round the brow with three points and a red cabochon (drawn on both layers, round the whole head); a tan tunic `#93603a`
with lit panels, stitched patches, a lace placket and a gold collar and a geometric gold hem; brown sleeves `#7a4a2a` with gold cuffs, muscled bare forearms with a scar and a vein,
cream bandage wraps on the hands; a double belt with a square gold buckle; dark trousers and calf boots with gold cuffs. The red scarf is an extra and is left free.
Hair: `pompadour` (the classic style kept by hair_map), colour `#181010` (the 3D tint renders brown-black): it is the only style that leaves the crown's row visible; every `anime_*` cut
covers the crown at the front (tested: anime_short, spiky, swept, spike_tall, mane).
Form 2: violet skin, a tunic `#6c4a60` with lilac trim `#c8a0ff`, lilac slit glowing eyes, fangs, glowing cracks on the face and the arms; hair `#241a34`. Glow: only form 2 (eyes, cracks);
form 1 has none, as before.

## Vanica Zogratis (and form 2)

Design: slim; a pale heart-shaped face with a long-lashed red eye, an arched brow, red lips in an open grin with a fang, a blush; the black eyepatch over the eye on the left of the picture
(gold rim, a gold spade, a strap up to the crown and one round to the ear) on both layers; a gold crown with five points and a ruby; a wine-red dress `#8a2236` with princess seams, a darker
quilted lower bodice under a gold sweetheart seam, a gold spade on the chest, a ring buckle on a studded belt and a scalloped gold hem; long black sleeves with gold shoulder and cuff
bands, gloves; long black boots with a gold cuff and straps.
Hair: `bob` (classic, kept): the bob frames the face and the eyepatch. **Hair problem:** the crown's row is hidden by the bob (and by every other style tested: anime_short,
anime_short_open, anime_bangs_long, hime, layered, curtains, anime_swept, swept, ponytail, short), so the crown shows only from the sides and the back.
Form 2: red skin, a darker dress `#5e1022` with coral trim `#ff6a5a`, a slit glowing eye, fangs, a glowing sigil on the brow, veins and cracks, a glowing chest spade (the only glow).
The horns, thorn halo, blood orb and claws are extras.

## Zenon Zogratis (and form 2)

Design: a lean, pale, scarred young man: cold pale-grey eyes under flat dark brows with dark rings under them, a thin flat mouth, a vertical scar with cross bars through the right
eye and a short scar on the cheek; an open maroon vest `#8a2a38` with lapels, dark piping and stitched patches over a dark tank `#322a38`; two belts with two pouches and a steel buckle;
bare arms with veins and scars and grey-white bandage wraps on the hands; dark trousers and calf boots with a leather cuff.
Hair: `anime_short` in `#1a1618` (a neutral near-black; the first try `#1c1824` rendered purple). A short messy cut leaves the face and the scar free.
Form 2: grey skin, hollow sockets with burning indigo points, fangs, glowing cracks, a bone ribcage and sternum with indigo light over the chest and a spine on the back, bone
pauldrons and bracer rings with indigo glow; hair `mohawk` (classic, kept) in bone `#d8d0b4`. Glow only in form 2. The horns and insect wings are extras.

## Lucius Zogratis (and form 2)

Design: a pale face with narrow glowing red eyes under sharp brows, a faint smirk and a small diamond sigil on the left of the forehead (red, glowing); a white fur mantle over the
shoulders and chest (tufted lower edge), a dark collar and choker, a brown-maroon robe `#7a4444` with two dark bands and crossed leather straps with a gold ring clasp, a belt with a gold
buckle, wide sleeves with dark cuffs and gloves, a floor-length robe with folds and a dark hem.
Hair: `anime_spike_tall` in `#1a1618`: a crown of tall black clumps, the strongest silhouette of the group.
Form 2: a divine robe `#cde0f8` with a white tabard, gold side borders and a glowing gold sun medallion on the chest, gold-edged cuffs, white gloves, wide golden glowing eyes, gold light
lines on the cheeks, a white under-skirt and a gold fringed hem; hair white `#e8e4f0`. Glow in both forms (form 1: eyes and sigil; form 2: eyes, cheek lines, sigil, medallion). The wings,
antlers and the black cloak are extras.

## Fana (and form 2)

Design: slim, a small angry girl. Large sea-green eyes `#3fb894` (shaded iris, a lash flick, tilted rose brows), a blush, a clenched frown; the purple mage stone `#9a4ae0` on the forehead
(faceted, in a dark setting, glowing), the red tattoo that frames it (two hooked lines and a tick), and the third eye under it (a dark-rimmed almond, a red iris, a pupil, a highlight, glowing);
a high-collared furry maroon dress `#7a1a2c` (a fleece of short light and dark strokes, princess seams, lit panels) with a tufted cream fur collar, a pale almond **eye emblem** of the
Eye of the Midnight Sun on the chest (a red iris, soft glow), long sleeves with cream fur cuffs, a brown belt with a gold buckle and studs, a dark hem with a rose zigzag, brown knee boots with
a dusty-rose turned cuff, a strap and a stitched seam. The light hooded robe, the grimoire, the satchel and the spark of Salamander are extras and are left free.
Hair: **`anime_long_open`** in `#e0709c` (a deeper rose than the sheet's `#e88fb4`: the 3D tint renders paler than its hex). hair_map says `anime_long`, whose centre fringe hides the
forehead down to about row 3.6, so the stone, the tattoo and the third eye would be invisible; `anime_long_open` keeps the long wavy pink silhouette and leaves the forehead free (the stone is
still partly hidden by the one lock that falls over the middle of the brow).
Form 2 (the Evil Heart): a brighter dress `#b02030` with burning orange trim `#ffb347` and an orange glowing zigzag hem, burning orange eyes with slit pupils, a mad grin with fangs, glowing
veins on the cheeks, a gold third eye, a hot pink stone, an orange glowing emblem; hair **`anime_spike_swept`** `#ff7a9e` (hair_map has `anime_mane`, which closes the forehead; the swept crest
reads as hair that lifts and keeps the burning third eye visible). The crown of flames is an extra. Glow: stone, third eye and emblem (form 1); plus eyes, tattoo, veins and trim (form 2).

## Rounds, honest limits

At least four look rounds per character were made with `preview_arc.py` (front, back, bust) against the owner references 154, 155, 156 and the old 64x64 look. Weaker than the references: the faces are 8 model pixels wide, so the eye and mouth work stops at about 4 texels per detail;
Fana's frown and the boots of every character are the least detailed parts; the fur and fleece are strokes, not real fur; nothing was seen in a running game, only in the software renderer
(flat shading, no game light).

## Cost (rule 11)

Client only, nothing on the server: each skin is a 256x256 PNG (256 KiB raw, about 40 to 90 KiB packed) and, where there is a glow map, one more of the same size; ten looks are at most 20
textures. No new packets, entities, ticks or config. The textures are generated offline by deterministic code (numpy and Pillow, `H.hsh` hashes, no random numbers); a repaint of all
ten takes about 22 s. Draw cost in the game is the player model's, unchanged (same boxes, a bigger texture).
