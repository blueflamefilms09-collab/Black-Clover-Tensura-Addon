# HD arc, group bc3: Vanessa, Klaus, Secre, Zora, Gauche, Marx, Solid (Black Clover)

Seven looks hand-painted at 256x256 (every model pixel is a 4x4 texel block). None of them has a `_form2`. The painters are `tools/arc_hd/chars/{vanessa_enoteca,klaus_lunettes,secre_swallowtail,zora_ideale,gauche_adlai,marx_francois,solid_silva}.py`
plus the shared helpers in `tools/arc_hd/chars/_bc3_kit.py` (the underscore keeps the kit out of the generator and out of the module test; it is this group's own copy of the bc1 kit with
extra helpers, so no group imports another). Output: `src/main/resources/assets/nusmp/textures/entity/playermodel/<id>.png` (+ `_glow.png` where the look glows: Vanessa, Secre, Gauche) and
`tools/arc_hd/looks/<id>.json`. `looks.json` is merged by the lead (`merge_looks.py`), not here.

Previews: `docs/previews/arc_hd/bc3_front_back.png` (front and back of all seven, extras on) and `docs/previews/arc_hd/bc3_close.png` (bust of all seven).

## What is the same for all seven

- **64x64 stays** (rule 2): the 64x64 sheets (`tools/arc/char_<id>.py`) and the baseline bridge are untouched; HD is an added mode and `gen_arc_hd.py --size 64` still writes the old skin. Each painter
  starts from the 64x64 blueprint (`A.blueprint`, `A.bridge`, skipping only the front face of the head), so the colour blocking and the layout carry over, then redraws by hand what has to be crisp.
- **Both layers.** The game drops the second layer and the hair beyond 24 blocks (`PlayerLod`), so every detail (face, trim, buckles, seams) is painted on the base layer too. The second layer only
  holds cloth where the sheet has cloth (`K.hold_cover` clips every overlay island to the blueprint coverage), with the documented exceptions below.
- **Extras are not painted over.** Every one of the seven has a `pmx_<id>` extra that holds only the cape / cloak / mantle across the back (plus the floating book `prop_float`, Marx's ledger in his left
  hand and Vanessa's thread cube). So the backs of the coats and dresses are kept plain and the effort goes to the front, arms and legs. Slim or wide arms (`slim`; only Marx has wide arms), the height
  `scale` and the glow flag are untouched.
- **Hair is the kit's attached 3D hair** (`hair` + `hair_color` in the fragment), no hair sheet was changed (the owner said no to HD hair sheets). The skin paints the scalp and the visible fringe as
  thick layered clumps (`K.hair_mass` + `K.lock`, tapered with a dark edge and a light streak), never single floating strands. The hair sheet is grey and the tint multiplies, so pale colours read grey
  or mauve: the hair colours were raised in saturation until the lit steps stayed on hue (see each character).
- **Eyes.** Shaded anime eyes (`H.eye`: sclera, iris gradient with a dark limbal ring, pupil, highlights, lashes with a flick), each character with its own shape.
- **The head overlay is hand-drawn.** Every second-layer island of the head is cleared (`K.clear_head_overlay`) and the accessories of the sheet (Klaus's glasses, Gauche's monocle, hoops, Zora's face
  straps, ear cuffs) are redrawn by hand on both layers.
- **Deterministic**: numpy + Pillow only, fixed seeds (`H.hsh`), no `hash()`, the same bytes on every run.

## Sources (rule 10)

Neither the English nor the Japanese wiki could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection), so every entry below is **from memory of the
series plus the earlier research in `docs/arc/npcb.md` and the headers of `tools/arc/char_<id>.py`**, which cite the wikis. Nothing was traced. Where a detail is the sheet's own choice (the
colours of Marx and Solid, whose sheets say no source described their clothes) it is kept. Not fetched at all: all wiki pages (English and Japanese) of the seven characters.

## Vanessa Enoteca (`vanessa_enoteca`)

- **Design.** Lazy, sultry witch: half-lidded violet eyes with violet eyeshadow and a lid crease, arched pink brows, full red lips, a faint blush, gold earrings with a violet-gem drop. Dark purple
  dress with a V neckline (wine piping, bare collarbones), princess seams, a wine satin sash with a gold ring buckle and a gem, a pleated flared skirt with a wine hem and gold dot (on the legs),
  bare arms with a red thread wound round the right forearm in three clear coils with a knot and a loose end, and knee boots with plum turned cuffs, a strap with studs, welt and sole. The very
  short Black Bull cloak and the red cord hanging from the right hand are the `pmx_vanessa_enoteca` extra.
- **Colours.** Hair #f5559f, dress #34214f (light #5d3f8c), wine #a22f58, plum cuff #6a3070, gold #e6b858, thread #e0344e (shade #7a1228), eyes #3a1c6a / #8658d0 / #dcc0ff, boots #3a2a46.
- **Hair: `anime_long_open`** (was `anime_long` in the fragment). `anime_long` draws a striped lilac-and-pink slab over the forehead that hides the fringe and reads like a hat; `anime_long_open`
  keeps the long wavy hair and the cheek locks but leaves the painted side-parted fringe visible. #ec78b0 turned mauve through the tint, #f5559f stays pink.
- **Glow map.** The red thread (three coils, knot, loose end), on the base layer; the second-layer cord is a cutout and has no separate glow.
- **Forms.** None.
- **Source:** from memory + `docs/arc/npcb.md`.

## Klaus Lunettes (`klaus_lunettes`)

- **Design.** Proud strategist: sharp fuchsia anime eyes behind thin dark rectangular glasses (frames on the second layer so they stand off the face, glare streaks on the lens), thin pale-blue
  brows, a level mouth, gold hoop earrings. Navy double-breasted uniform with a high gold-bordered collar, a white jabot with a pin, gold borders down both edges, two columns of gold buttons,
  belt with a gold buckle, coat tails with a gold hem, the blue feathered shoulder collar of the Golden Dawn mantle, long sleeves with beveled steel bracers (rim, ridge, rivets, gold bands) over white
  gloves, dark trousers and knee boots with gold-trimmed turned cuffs. The gold mantle with the sun and the floating book are the `pmx_klaus_lunettes` extra.
- **Colours.** Hair #6fa4ee, coat #2b3659, gold #d9b24e, steel #6f89a8, shirt #ece8e2, belt #40291f, boots #4a3528, eyes #62144a / #cc4496 / #ffb4de, glasses #1a1d2c.
- **Hair: `anime_bangs_long`** (was `anime_swept`). `anime_swept` puts a block fringe over the glasses and the brows; `anime_bangs_long` sweeps long parted bangs out over the forehead and leaves
  the glasses and eyes clear. #8cb6e6 read grey on the side faces, #6fa4ee stays blue.
- **Glow map.** None (the glow is on the grimoire).
- **Forms.** None.
- **Source:** from memory + `docs/arc/npcb.md`.

## Secre Swallowtail (`secre_swallowtail`)

- **Design.** Small, shy and gloomy: flat half-closed dark eyes with faint bags, flat brows, a tiny neutral mouth, a blunt black bob fringe with two long side locks. Black sleeveless dress with a
  sweetheart neckline and thin straps, grey cord lacing down the bodice, a grey cord round the neck with a knot and a small gem that glows cyan, three tiers of ruffles ending in a ragged hem
  (rows 0 to 4 of the legs on the second layer), bare shaded arms and legs (kneecaps, calves) and low black slippers with an ankle strap and a silver buckle. The Black Bull cloak is the extra.
- **Colours.** Hair #26263a, dress #1b1b24, cord grey, gem #5ad8ff, skin #f4dcd6, eyes #0c0c14 / #32324a / #7a7a9c, slippers #17171e.
- **Hair: `bob`** (unchanged). The classic bob is the right cut (a straight chin-length bob with a blunt fringe). It is a boxy slab with a flat top, and its fringe corners show a thin dark sliver
  (the geometry of the style). A pure black #1c1c28 read as a flat black block; #26263a keeps the lit steps visible.
- **Glow map.** The gem in the neck cord (cyan), small.
- **Forms.** None.
- **Source:** from memory + `docs/arc/npcb.md`.

## Zora Ideale (`zora_ideale`)

- **Design.** The rude prankster: sharp blue eyes with a smirk and a flash of teeth, thin angled brows, a stud above the right brow, steel ear cuffs. The sheet builds the leather straps wound round his
  lower face as an extra in code, but the packed model holds only the cloak, so the straps are painted: two leather bands round the jaw (under the nose and under the mouth) joined by a strap on each
  cheek, with rivets and a buckle, on both layers of the head. Dark ribbed sleeveless tank with worn patches, a rip and a frayed hem, two buckled belts round the neck, bare arms with muscle shading,
  black leather wrist wraps with buckled straps, a double belt with a steel buckle, trousers with a thigh strap and bevelled knee patches, boots with straps, steel toe caps, welt and sole.
- **Colours.** Hair #d02e1a, skin #f2d2c4, tank #34343f, trousers #3b3b47, leather #221d22, steel #b6b8c8, belt #2e241f, boots #2c2c36, eyes #0e2a66 / #3f78d0 / #a8d4ff.
- **Hair: `anime_spiky`** (was `anime_spike_swept`). `anime_spike_swept` renders as thin radiating spikes that read as fingers (the owner's complaint) and a glassy halo. `anime_spiky` is a
  crest of thick layered clumps rooted in the crown. #e2432e read coral-pink on the lit faces, #d02e1a stays red.
- **Glow map.** None (the bull on the cloak glows, not the skin).
- **Forms.** None.
- **Source:** from memory + `docs/arc/npcb.md` + the header of `tools/arc/char_zora_ideale.py` (which cites the wikis: face straps, studs, ear cuffs, sleeveless shirt).

## Gauche Adlai (`gauche_adlai`)

- **Design.** The cold, clever brother: a tired dark right eye with bags and flat heavy brows, the left eye as a polished silver mirror (silver almond, sky-and-ground reflection with a dark horizon,
  violet rim, glint) behind a raised silver monocle ring on the second layer with a short chain, a thin cold mouth, a messy dark fringe. Near-black high-collared uniform with lavender piping and a
  clasp, a front opening with lavender plackets and silver-lavender buttons, lavender shoulder lines, a dark belt with a lavender buckle, sleeves with lavender cuff bands over black gloves,
  trousers with a lavender side stripe and calf boots with buckled straps. The cloak and the orbiting glass orb are the `pmx_gauche_adlai` extra.
- **Colours.** Hair #2a2a40, coat #2a2933, lavender #9a94c8, violet #a89cff, trousers #22212b, boots #2c2a36, mirror #6a6a8a / #c8c8e4 / #ffffff, eye #14121c / #3a3550 / #7a7298.
- **Hair: `anime_short_open`** (was `anime_short`). `anime_short` puts two black block pieces over the forehead and hides the painted fringe, and a black #1c1b26 turned the whole head into a
  flat black mass. `anime_short_open` leaves the messy fringe visible; #2a2a40 is a blue-black that keeps the clumps readable.
- **Glow map.** The mirror eye, the ring of the monocle and the clasp stud, in the violet of his Mirror Magic (the sheet says the monocle shines violet).
- **Forms.** None.
- **Source:** from memory + `docs/arc/npcb.md` + the header of `tools/arc/char_gauche_adlai.py`.

## Marx Francois (`marx_francois`)

- **Design.** The Wizard King's exact, long-suffering aide: sharp blue-violet eyes with faint bags, flat thin brows, a stern line at the nose, a level mouth, a blunt bowl-cut fringe. Deep-blue
  aide's coat with a high collar piped in silver, a white cravat knot with a sapphire pin, the coat open in a V over a slate waistcoat with silver buttons, wide lapels piped in silver, a silver
  aiguillette cord on the right chest, two silver clasps where the cape is pinned, a belt with a silver buckle, coat tails with a silver hem, wide (4 px) arms with navy-and-silver shoulder
  boards, turned silver cuffs with two buttons over white gloves, creased trousers with knee folds and calf boots with silver-trimmed cuffs. The short cape with the silver clover and the ledger in
  the left hand are the `pmx_marx_francois` extra.
- **Colours.** Hair #5a4232, coat #2a3a6a, waistcoat #1f2a52, silver #c8d4e8, shirt #eef0f6, belt #1e2438, trousers #242c48, boots #2c3350, eyes #27306a / #5a6aa8 / #a4b6ea.
- **Hair: `bob`** (unchanged). A neat bowl cut is exactly a bob. `anime_short_open` and `anime_flattop` were tried: the first is messy, the second looks like a cap.
- **Glow map.** None (the sheet's glow is the faint trim of the ledger, which is a separate model).
- **Forms.** None.
- **Source:** from memory + `docs/arc/npcb.md` section 10 (mushroom-cut hair and formal robes are from a source; the colours are the sheet's choice and are kept).

## Solid Silva (`solid_silva`)

- **Design.** The sneering Silver Eagle knight: sharp half-lidded blue eyes with a cold highlight, thin pale brows with the right one arched, a crooked smirk with a dimple, a swept silver-blue
  fringe of angular blades. Dark-blue noble's uniform with a high silver-piped collar, a white cravat with a sapphire pin, a closed front with two narrow silver plackets and a column of silver
  buttons, chevron stitching, the Silver Eagle crest (spread wings of three silver feathers each round a sapphire) on the chest, a belt with a silver buckle, coat tails with a silver hem, slim
  (3 px) sleeves under layered silver feather pauldrons with a silver stripe, turned silver cuffs over white gloves, creased trousers and knee boots with silver turned cuffs and knee plates. The
  silver mantle with its wings and the water-blue floating grimoire are the `pmx_solid_silva` extra.
- **Colours.** Hair #8fb4f4, coat #3a4668, silver #c8d4ec, shirt #eef0f6, belt #3a3e4a, trousers #2a3048, boots #303a58, eyes #1c3a7a / #4a7ac8 / #a8d0ff.
- **Hair: `anime_swept`** (unchanged). The neat side-parted sweep suits a noble. It still covers the brows with a block fringe, which hides the arched brow of the sneer; the smirk carries it. A
  pale #c8d4ec (the sheet's colour) read grey, #8fb4f4 keeps the blue.
- **Glow map.** None (the water-blue glow is on the grimoire).
- **Forms.** None.
- **Source:** from memory + `docs/arc/npcb.md` section 10 (the sheet says no source described his hair or uniform, so the colours are a choice and are kept).

## Deliberate overlay exceptions (rule 5 / hd_arc.md, like sds2's Escanor form 2)

| Character | Where the second layer is larger than the 64x64 sheet | Why |
|---|---|---|
| Vanessa | both legs, rows 4.0 to 5.9 (the turned boot cuff) | the cuff stands off the leg as a bulky piece |
| Vanessa | right forearm, rows 7.2 to 10.2 | the red thread stands off the arm (cutout cord, erased outside those rows) |
| Zora | body, rows 0 to 1.8 | the two neck belts stand off the neck |
| all seven | head, all islands cleared and redrawn by hand | glasses, monocle, hoops, ear cuffs and straps are hand-drawn, only where the sheet has them |

Everything else is held to the blueprint coverage by `K.hold_cover`.

## NEW / REPLACED / KEPT (rule 5)

- **REPLACED:** the 64x64 skins of the seven (same file names, now 256x256 hand-painted), their hair choice where it changed (Vanessa `anime_long_open`, Klaus `anime_bangs_long`, Zora `anime_spiky`,
  Gauche `anime_short_open`; Secre, Marx and Solid keep theirs) and the hair colours of all seven (re-saturated for the tint). The owner asked for the HD replacement.
- **NEW:** the seven painters, `_bc3_kit.py`, the glow maps of Vanessa (thread), Secre (neck gem) and Gauche (mirror eye, monocle, clasp) (the 64x64 skins had none), Zora's painted face
  straps and Vanessa's painted thread (the packed models do not hold them), the previews and this document.
- **KEPT:** the 64x64 mode (`gen_arc_hd.py --size 64` still reproduces the old skins, the sheets in `tools/arc/` are untouched), the extras (`pmx_<id>` models and textures), slim or wide arms,
  height scale, hold poses, the hair sheets (64x64, as the owner decided), the glow semantics of the extras, old data and save compatibility.

## Cost and server impact (rules 9 and 11)

Skins are client assets: no server code, no packet, no tick work, nothing changed in common code. Worst case per distinct look: one 256x256 RGBA texture (256 KiB) plus a glow map for Vanessa,
Secre and Gauche (another 256 KiB each) = about 340 KiB GPU per look including mipmaps; the seven looks together are about 2.4 MiB (about 3.2 MiB with the three glow maps). The model, quad count and
hair geometry are unchanged (the hair styles are existing kit styles of at most 180 quads, within the 200 limit). The PNGs on disk are about 25 to 36 KiB each (deterministic, optimised).

## Checks

- `PlayerModelDataTest`: 12 passed. `ArcHdSkinsTest`: 4 passed, 1 failed, and the one failure is the lead's pending step, not this group: `looks.json differs from its fragment` (it names `asta`, whose
  fragment was changed by another group; `merge_looks.py` has to run once all groups are done). `tools/arc_hd/test_arc_hd.py`: 7 of 8 passed, the same `looks.json` merge check failed.
- Every image of the seven was checked in front, three-quarter, back and bust views, and as texture sheets at 2x, in four rounds each: the first draft (outfit, colours, hair), the hair style and
  colour (a style-by-style comparison for each character), the details (thread, crest, glasses, eyes, bare skin, boots), and a final texture and silhouette audit against the old 64x64 look.

## Weaker than the references (honest caveats, nothing was seen in a game)

- The arms of the bare-armed characters (Vanessa, Secre, Zora) are plain skin with soft muscle shading; they are flatter than the owner references' cloth and metal.
- Klaus's bracers read as pale blue-white sleeves from a distance; the gold bands and rivets only show close up.
- The hair sheets are 64x64 and grey; the pale colours (Klaus, Solid) still show some grey-blue steps on the side faces.
- Solid's block fringe hides his brows; Gauche's `anime_short_open` has one dark block piece at the left temple (the geometry of the style).
- The `bob` style (Secre, Marx) is a boxy slab with a flat top and thin dark slivers at the fringe corners.
- Vanessa's legs show no bare skin (the dress and the boots meet as in the sheet); Zora's face straps are heavy by design (the sheet puts two leather rings round the jaw).
- No wiki page could be fetched, so every detail is from memory plus the repo's own research notes.
