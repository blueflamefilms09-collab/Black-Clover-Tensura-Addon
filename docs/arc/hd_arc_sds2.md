# HD arc, group sds2: Gowther, Merlin, Escanor (and their second forms)

Six looks hand-painted at 256x256 (every model pixel is a 4x4 texel block), each with the glow map the look already had (Escanor's base form has none, as before).
The painters are `tools/arc_hd/chars/{gowther,merlin,escanor}.py` plus the thin `*_form2.py` loaders (each `paint()` calls `paint_form()` of `_gowther.py` /
`_merlin.py` / `_escanor.py` with the palette of the form), and the shared helpers in `tools/arc_hd/chars/_sds2.py` (the underscore keeps these files out of the
generator and out of the module test). Output: `src/main/resources/assets/nusmp/textures/entity/playermodel/<id>.png` (+ `_glow.png`) and
`tools/arc_hd/looks/<id>.json` for each id. `looks.json` is merged by the lead (`merge_looks.py`), not here.

Previews: `docs/previews/arc_hd/sds2_front_back.png` (front and back of all six, extras on) and `docs/previews/arc_hd/sds2_close.png` (bust of all six).

## What is the same for all three

- **64x64 stays** (rule 2): the 64x64 sheets (`tools/arc/char_{gowther,merlin,escanor}.py`) and the baseline bridge are untouched; HD is an added mode and `gen_arc_hd.py --size 64`
  still writes the old skin. Each painter starts from the 64x64 blueprint (`A.blueprint`, `A.bridge`), so the colour blocking and the layout carry over, then redraws by hand
  what has to be crisp.
- **Both layers.** The game drops the second layer and the hair beyond 24 blocks (`PlayerLod`), so every detail (face, trim, buckles, seams) is painted on the base layer
  too. The second layer only holds cloth where the sheet has cloth (`S.hold_cover` clips every overlay island to the blueprint coverage), with one deliberate exception
  (Escanor form 2, below).
- **Extras are not painted over.** Gowther's bow and the wings and broken halo of form 2, Merlin's cloak (it covers her whole back) and the hanging sash at her left hip,
  Escanor's Divine Axe in the right hand are separate models (`extras`). So Merlin's back is kept plain, Gowther's form 2 back only has two plain wing-mount rings, Merlin's
  front-left hip is simple and Escanor's right fist is a plain bare hand. Slim/wide arms (`slim`), height `scale` and `hold_r` are untouched.
- **Hair is the kit's attached 3D hair**, one colour (`hair_color`, taken from the module's return value into the fragment). The skin paints the scalp and the visible fringe as
  thick layered clumps (`S.hair_mass` + `S.lock`: every lock starts on the forehead mass, tapered, with a dark edge and a light streak), never single floating strands.
- **Eyes.** Shaded anime eyes (`H.eye`: sclera, iris gradient with a dark limbal ring, pupil, two highlights, thick lashes with a flick). The inner corners are 0.9 model px apart
  (round 3: at 0.4 to 0.5 the tapered lashes joined into a grey line across the nose like glasses). Glowing eyes only light a small part of the iris on the glow map (round 2: lighting the whole
  iris turned the eyes into white-yellow blobs).
- **Ears and earrings** (`S.ear`) on the side faces of the head, both layers.
- **Deterministic**: numpy + Pillow only, fixed seeds, no `hash()`, the same bytes on every run.

## Sources (rule 10)

Neither the English nor the Japanese wiki could be fetched in this environment (the fandom host did not resolve), so every entry below is **from memory of the series plus the
earlier research in `docs/arc/<id>.md`**, which cites the wikis. Nothing was traced. Entries marked "added in the HD pass" are details the 64x64 sheet did not have. Not fetched
at all: all wiki pages (English and Japanese) of the three characters.

## Gowther, the Goat's Sin of Lust (`gowther`, `gowther_form2`)

- **Design.** The doll-like boy: round amber eyes with long lashes, thin plum brows, a pink doll's blush and a tiny mouth. Knee-length indigo coat with pink piping and a high
  split collar, white shirt with pearl buttons and a placket, a pink satin jabot with a pearl, the pink crest of the Goat on the chest (a small shield with two horns, glows),
  white gloves with turned cuffs piped in pink, a double belt with a pink ring buckle and a gem, coat tails with a pink hem, calf boots with turned pink cuffs, a strap with a stud, welt
  and sole. The bow is the `pmx_gowther` extra.
- **Colours.** Hair #b4457f, coat #2f2a66 (light #4b4597), trim #e68ac0, shirt/gloves #f4eef8, belts #1e1b44, boots #262050 with straps #4a3a80, eyes #6e4310 / #e0a030 / #ffeaa8,
  glow #ff8ad0, eye light #ffcf70.
- **Hair: `anime_short`** (was `curtains` in `looks.json`). `curtains` is a boxy two-panel cut; `anime_short` is a shaggy mop that matches the sheet's messy pink cut and the
  fringe painted on the face (parted a little left, long side-swept bang).
- **Form 2 (the Doll Unbound).** Lighter indigo coat #4a448c, pale-pink trim #ffd0ec, long pale-pink hair `anime_long` with colour #ffacd8 (was #f0b8d8; the hair sheet is grey and the tint
  multiplies, so a pale pink read grey: a more saturated pink stays pink in the shaded steps), cracks of light on the face (three dark seams with glowing cores) and two light lines
  on the cheeks, light lines along the coat and the sleeves, two wing-mount rings on the back, golden eyes with a light #ffd890. The wings and the halo are the `pmx_gowther_form2` extra.
- **Glow map.** Eyes (small inner light), the goat crest, the piping of collar, cuffs, hems and belts, the buckle gem, the cracks and light lines and the mount rings of form 2.
- **Added in the HD pass:** the jabot and its pearl, the crest brooch, the coat tails with the split at the middle, the boot straps and the cracks of form 2.
- **Source:** from memory + `docs/arc/gowther.md`.

## Merlin, the Boar's Sin of Gluttony (`merlin`, `merlin_form2`)

- **Design.** Sharp, calm, half-lidded golden eyes with violet eyeshadow, a liner wing and heavy lashes, thin arched brows, small red lips, a gold earring with a violet gem. Short
  violet dress with a white fur collar (scalloped tufts), a gold choker with a violet gem, a bodice with gold piping and a gold star on the chest (glows), a gold sash with a violet
  inner band and a gem buckle, detached bell sleeves with gold cuffs over long black gloves, a skirt with a gold hem, black thigh-high boots with gold straps and a gold top. The
  cloak and the hanging sash are the `pmx_merlin` extra, so the back is a plain dress and the front-left hip is kept simple.
- **Colours.** Hair #17131f with a violet streak lock (#7a5cff) inside the fringe, dress #4a3ca8, sleeves #33288a, gold #d8b24a, fur #f0eaf8, boots #241a38, gloves #17131f,
  eyes #7a5a08 / #e8c040 / #fff4b0, glow #ffd86b.
- **Hair: `bob`** (unchanged in `looks.json`). A neat chin-length black cut is the canon shape; I compared it with `anime_swept`, `anime_bangs_long`, `hime`, `anime_long_open`,
  `layered`: the anime styles are too spiky or messy for her sleek hair and `hime` too long. The cost is that `bob` is a blocky, flat-topped classic style (see the caveats).
- **Form 2 (Infinity unbound).** Lighter violet dress #6a54d8, pale-gold trim #e6c868, a violet-white streak (#cdbcff), pale eyes with a lighter glow, a gear ring around the star
  and light lines on the sleeves. The gear halo above her head is an extra.
- **Glow map.** Eyes (small inner light), the choker gem, the star, the earring jewel, the gold trim.
- **Added in the HD pass:** the scalloped fur, the earrings, the back buttons (hidden under the cloak), the straps of the boots.
- **Source:** from memory + `docs/arc/merlin.md`.

## Escanor, the Lion's Sin of Pride (`escanor`, `escanor_form2`)

- **Design (night, `escanor`).** The gentle giant: big shy brown eyes with eye bags and sad brows, a faint blush, a short messy brown mop. A short brown coat with a stand collar
  and gold-brown piping over a cream shirt with a placket and brass buttons, a plain belt with a gold buckle, dark trousers, calf boots with folded tops, straps and soles, stitched
  cuffs and bare hands. Colours: hair #6a4a30, coat #5c3e2a (round 2: was grey-brown), trim #9a7440, shirt #cfc4ac, trousers #241c20, boots #34261e, gold #dda331.
  No glow map (as before).
- **Design (noon, `escanor_form2`).** Pride in daylight: tan skin, thick straight angry brows, narrow glowing amber eyes, stubble and a wide toothy grin. A black tank with a gold V
  collar, bare muscled arms, a double belt with a glowing sun buckle, black trousers and boots, and the bulky pauldrons (three metal lames, gold - bronze - gold, with rivets and a dark
  gap under each) and bracers (bronze with gold rims and a raised ridge) of the sheet, drawn as polished metal with cylinder shading, a bright line under each top edge and glowing gold edges.
  Colours: skin #b07c58 / #d8a072 / #efc08e, tank #1c1618, gold #daa030 / #ffd070, bronze #b8782a, hair #ff8c14, glow #ffd070.
- **The one overlay exception.** The day arms' second layer is a little bulkier than the sheet's (pauldrons to row 4, bracers to row 11) and is not clipped to the blueprint coverage:
  bulky armour pieces are the owner's reference style. Between the pauldron and the bracer and on the hand the overlay is erased, so the bare arm shows.
- **Hair.** Night: `anime_short` (unchanged), painted fringe as thick clumps. Noon: **`anime_spike_tall`** (was `anime_mane` in `looks.json`) with colour #ff8c14 (was #e0902a). `anime_mane`
  read as a flat crown of thin rays; `anime_spike_tall` gives thick layered flame-like clumps swept up and back like the blazing mane of the character and like owner reference 154.
  `anime_spike_swept`, `anime_spiky`, `anime_swept` and `anime_short` were compared too. The saturated orange keeps the spike tips orange after the multiplicative tint.
- **Glow map (noon only).** Eyes (small inner light), the gold collar of the tank, the sun buckle, the lame edges of the pauldrons and the rims of the bracers.
- **Added in the HD pass:** the tank hem and neckline shading, the rivets, the folded boot tops, the eye bags and the stubble.
- **Source:** from memory + `docs/arc/escanor.md` (that document says the clothes are design choices of the game).

## NEW / REPLACED / KEPT (rule 5)

- **NEW files:** `chars/_escanor.py`, `chars/escanor.py`, `chars/escanor_form2.py` (before this the two Escanor looks were baseline bridges), and `docs/arc/hd_arc_sds2.md`
  with the two previews.
- **REPLACED (reworked in place):** `chars/_sds2.py`, `chars/_gowther.py`, `chars/_merlin.py`, `chars/gowther.py`, `chars/gowther_form2.py`, `chars/merlin.py`, `chars/merlin_form2.py`
  (they existed from the wip commit 530d1b5); the textures and glow maps of all six looks are regenerated (same file names). Hair changes the lead gets from the fragments at merge:
  `gowther` `curtains` -> `anime_short`; `gowther_form2` colour #f0b8d8 -> #ffacd8; `escanor_form2` `anime_mane` #e0902a -> `anime_spike_tall` #ff8c14.
- **KEPT:** the 64x64 sheets and `tools/arc/char_*.py`, all `looks.json` entries until the merge, the `extras`, slim/wide arms, `scale`, `hold_r`, glow on/off per look, the hair sheets
  (stay 64x64), the `_form2` ids, the Time Magic VFX (not touched).

## Cost (rule 11)

Skins are client assets only: no server work, no packets, no ticks, no per-player state (rule 9 unaffected). Worst case: 11 PNGs of 256x256 (6 skins + 5 glow maps), about 253 KB on disk
in total; each loaded look costs about 341 KiB of GPU memory (256x256 RGBA with mips) and one extra for its glow map; only the looks within draw distance are loaded, and the 24-block LOD
drops the second layer and the hair as before. Per-frame draw cost is unchanged (same quads as the 64x64 model); the painters run at tool time, nothing is allocated per frame.

## Review rounds (at least four per character, front / back / close with `tools/arc_hd/preview_arc.py`)

1. Baseline bridge vs. the old 64x64 look: faces and fringes redrawn, hair styles compared (`hairtry` grids per character).
2. Colour and glow: eyes were white-yellow blobs (glow lit the whole iris) -> a small inner light; cloth gradients too bright and gloves grey-lit -> `top 1.2 / bottom 0.8` panels and low sheen;
   Gowther's mouth too pale, strap and boot colours; form 2 hair grey -> saturated pink; Escanor's coat grey-brown -> warm brown, trousers darker.
3. Detail: Escanor's day pauldrons read as wooden barrels -> metal lames with cylinder shading, dark gaps, rivets and glowing edges; day hair `anime_mane` -> `anime_spike_tall`; the grey line across the
   nose between the eyes (inner corners 0.5 apart) -> eyes 0.2 px narrower and their centres 0.1 px further out in all six faces (gap 0.4-0.5 -> 0.9); Merlin's fringe rebuilt as one clean slanted mass with the violet streak inside it.
4. Base-layer and extras check (all six with and without extras, front, back, 3/4, side) and a final pass against owner references 154 to 156 and the old 64x64 look; leftover test code removed.

## Honest caveats

- Software renderer only (`tools/playermodel`, flat shading, no game light): nothing here was seen in the game, so the in-game light, the hair engine and the extras at their real size are unconfirmed.
- Weaker than the references 154 to 156: no truly bulky collars or pauldrons except Escanor's (the overlay geometry is one pixel thick), softer colour blocking (more texture, less flat
  colour than the references), and the sides, top and back of the head keep the baseline bridged scalp texture under the 3D hair.
- Hair-engine matters (not skin): Merlin's `bob` is a blocky, flat-topped cut; Gowther form 2's `anime_long` has pale highlight patches that read grey-white in the flat preview; `anime_short`
  (Gowther, Escanor) has loose thin chips at the top; `anime_spike_tall` (Escanor noon) has no back mane, so the nape shows the scalp, and its tips read pale in the flat preview.
- `test_fragments_match_looks_json` in `tools/arc_hd/test_arc_hd.py` fails until the lead runs `merge_looks.py` (the fragments differ from `looks.json` by the three hair changes above).
