# HD arc, group bc2: Nozel Silva, Luck Voltia, Magna Swing, Finral Roulacase, Charmy Pappitson, Gordon Agrippa

Six looks hand-painted at 256x256 (every model pixel is a 4x4 texel block). None has a `_form2`. The painters are
`tools/arc_hd/chars/{nozel_silva,luck_voltia,magna_swing,finral_roulacase,charmy_pappitson,gordon_agrippa}.py` with the shared helpers in
`tools/arc_hd/chars/_bc2.py` (the underscore keeps the kit out of the generator and out of the module test). Output:
`src/main/resources/assets/nusmp/textures/entity/playermodel/<id>.png` (+ `finral_roulacase_glow.png`, the only glow map of the group) and
`tools/arc_hd/looks/<id>.json` for each id. `looks.json` is merged by the lead (`merge_looks.py`), not here.

Previews: `docs/previews/arc_hd/bc2_front_back.png` (front and back of all six, extras on) and `docs/previews/arc_hd/bc2_close.png` (bust of all six).

## What is the same for all six

- **64x64 stays** (rule 2): the 64x64 sheets and the baseline bridge are untouched; HD is an added mode. Each painter starts from the 64x64 blueprint
  (`A.blueprint`, `A.bridge`), so the colour blocking and the layout of the sheet carry over, then redraws by hand what has to be crisp.
- **Both layers.** The game drops the second layer and the hair beyond 24 blocks (`PlayerLod`), so the face, trim, buckles and seams are on the base layer too;
  the second layer only holds cloth where the sheet has cloth (`all_covers` / `hold_cover`, the overlay is clipped to the blueprint coverage; Magna's strap and
  Finral's earring are added to the cover on purpose).
- **Extras are not painted over.** Nozel's floating grimoire, Magna's broom (it lies across the back) and the cloak of every look are the `pmx_<id>` extras, so the
  backs are kept plain (a centre seam, a yoke, a collar) and nothing is painted where a prop attaches. Slim/wide arms, `scale`, the extras and the glow semantics are kept.
- **Hair is the kit's attached 3D hair** (single colour `hair_color`); the skin paints the scalp and the visible fringe as thick layered clumps (`K.lock`, `K.fringe_mass`),
  and the sides, back and top of the head as clean skin and hair regions (`K.tidy_head`). The 3D hair covers the forehead down to y 4.1 for `anime_swept` at close range,
  so the painted brows and fringe matter for the far LOD, where the hair is dropped. Hair sheets stay 64x64 (owner decision).
- **Pale and black tints.** The hair sheet is grey and the tint multiplies, so silver must start very light (#f2f4ff) and black must be lifted a little (#1b1b26) or it
  reads flat grey or dead black; saturated yellows and browns are used so they do not read khaki.
- Deterministic code (numpy + Pillow only): no random numbers, the same bytes on every run.

## Sources (rule 10)

The English and the Japanese wiki could not be fetched in this environment (curl status 000 on the fandom hosts and on ja.wikipedia), so every entry below is
**from memory of the series plus the repo's sheets and `docs/arc/npcb.md`** (which cites the wikis). Nothing is traced. Entries marked "added in the HD pass" are
details the 64x64 sheet did not have. Nothing could be fetched at all.

## Nozel Silva, Captain of the Silver Eagles (`nozel_silva`)

- **Design.** Tall, slim and cold: narrow half-lidded blue-violet eyes with long lashes, flat pale brows, a closed mouth, a long side fringe over one eye. Knee-length
  silver-white coat with piped steel-blue front edges (0.5 model px thick), a high steel-blue stand collar with a navy V at the throat, an open front that shows a navy
  shirt with silver buttons, the silver eagle brooch on the left breast, a navy belt with a silver buckle, long sleeves with tall steel-blue cuffs over silver-white gloves
  with finger gaps, a coat skirt that ends in a steel-blue hem band, knee-high dark boots with silver cuffs. The cape and the floating grimoire are the `pmx_nozel_silva` extra (the Silver Eagles cloak).
- **Colours.** Hair #f2f4ff, coat #d7dbe9, trim #6f79a6, navy #262b4a, silver #c9cddb, belt #2c3044, gloves #dde0ec, boots #1f2230, iris #1e2a78 / #5a76d2 / #b4c8ff.
- **Hair: `anime_swept`** (kept from the baseline): the big swept silver mass with a long fringe over one eye is the silver owner reference 155. `anime_long_open`, `anime_long`
  and `anime_bangs_long` were tried on the same skin and read as plain helmets or as pigtails, so the swept cut stays.
- **Added in the HD pass:** the eagle brooch, the piped edges and the stitched collar; the cuffs are 1.7 model px tall (round 2: they read thin).
- **Source:** from memory + `docs/arc/npcb.md`.

## Luck Voltia, the smiling lightning mage of the Black Bulls (`luck_voltia`)

- **Design.** Small, eager and grinning: wide bright blue eyes, a wide open grin with two fangs, pushed-up cheeks with a blush, light blond hair. Cyan tunic with a laced V
  neck and gold piping, a dark belt with a gold buckle with a bolt cut in it, gold fingerless gloves with a bolt mark and bare fingertips, the tunic skirt over teal
  trousers with rolled hems, small dark shoes with a gold clasp. The Black Bull cloak (its bull glows lightning yellow) is the `pmx_luck_voltia` extra.
- **Colours.** Hair #f8d63c, tunic #4ab8c8, trim #2a8a96, trousers #25808a, gold #e8c040, belt #3e2a20, shoes #2a2a34, iris #1a64a8 / #5cc4f2 / #d0f4ff.
- **Hair: `anime_short`** (kept): a layered mop with a fringe over the brow like owner reference 156 (a short boy's cut). `anime_spiky` was tried and made him look like Ban.
  Round 2 lifted the yellow (#f1cd3a to #f8d63c) so the fringe no longer reads khaki.
- **Source:** from memory + `docs/arc/npcb.md`.

## Magna Swing, the fire mage of the Black Bulls (`magna_swing`)

- **Design.** Lean ex-delinquent with a hot-headed grin: thick slanted black brows, tinted goggles (black frame with a lit rim, blue lenses with a glare, a bridge, a strap
  into the head sides), a grin with two fangs, a black head strap on all four sides of the head on both layers with a metal slide at the front, a dark stubble above it and
  on the shaved sides and back. Short navy leather jacket with lit edges, piping and stitching and a lighter stand collar over a white shirt (studs, collar flaps), a dark belt
  with two pouches and a gold buckle, black fingerless gloves with studs, creased black trousers and calf boots with a strap. The broom and the Black Bull cloak are the `pmx_magna_swing` extra.
- **Colours.** Hair #1a1a22, jacket #2d3a62, collar #4a5a90, shirt #f0efec, trousers #25252e, boots #1b1b20, gloves #202026, belt #3e2c22, gold #c8a040, lenses #3a78d8.
- **Hair: `mohawk`** (KEPT, the kit's classic): no anime cut is a mohawk (owner decision). In the game it is a tall flat black slab, see the caveats.
- **Source:** from memory + `docs/arc/npcb.md`.

## Finral Roulacase, the Black Bulls' spatial mage (`finral_roulacase`)

- **Design.** Soft, worried and polite: big droopy green eyes (the outer corners hang), sad slanted brows, a small mouth, a light blush; honey-brown hair. Cream shirt with a wide
  open V collar and flaps, a placket with buttons, buttoned cuffs, bare hands with finger gaps, a dark belt with a gold buckle, brown trousers with turned cuffs and strapped boots.
  The earrings (a gold stud and a drop with a green gem) are on both sides of the head, base and second layer.
- **Colours.** Hair #b98c52, shirt #ece6d2 / #b9ad88, trousers #5f4a33, cuffs #80603f, boots #4c3626, belt #3a281b, gold #d0a844, gem #58c878, iris #1e5a34 / #5cbc72 / #c4f4c8.
- **Hair: `anime_short`** (kept): a layered mop with a fringe, the same family as King (owner reference 156).
- **Glow map (NEW for this look).** The old look had no glow map; the gem of the earrings glows `#58e898` (the spec says his earrings glow), so `finral_roulacase.json` now has
  `"glow": true` and `finral_roulacase_glow.png` is written. Only the gems are on it.
- **Source:** from memory + `docs/arc/npcb.md`.

## Charmy Pappitson, the Black Bulls' hungry cook (`charmy_pappitson`)

- **Design.** A small child: huge round green eyes with a big highlight, a big round blush, an open happy mouth with a fang, short blunt black bangs. Cream tunic with a tan trim down
  both front edges, a round neck with a V and a lace, tan hem band, a satchel strap across the chest to a bag with a stitched flap and a gold stud at the left hip (belt below it),
  short sleeves with tan cuffs and bare arms, brown shorts with a turned hem, bare legs, brown shoes with a stud. The Black Bull cloak (its bull glows warm orange) is the `pmx_charmy_pappitson` extra.
- **Colours.** Hair #2a2634, tunic #f2e6c8, trim #c89a58 / #8a6430, shorts #6d4c32, shoes #5e3e2a, belt #4a3020, strap #a8743c, gold #d4aa44, iris #1c6a38 / #4cc472 / #c8f6cc.
- **Hair: `anime_short`** (kept). Canon has a short cut with small tufts or a bun; the kit has no bun, so the short cut with the blunt bangs stays (see the caveats).
- **Added in the HD pass:** the satchel and its strap (the sheet had a plain tunic).
- **Source:** from memory + `docs/arc/npcb.md`.

## Gordon Agrippa, the poison and curse mage of the Black Bulls (`gordon_agrippa`)

- **Design.** Tall, thin and deathly pale: WIDE (4 px) arms; tired half-lidded eyes with dark bags and a crease under them, no eyebrows, a big nose with wide nostrils, a flat frown with
  worry lines, hollow cheeks. Long black hair that sweeps over one eye. Knee-length dark violet-grey coat with a high lighter stand collar piped in pale poison green, a closed front with a
  lapped placket and a row of bone buttons, two stitched lines, a belt with a muted poison-green buckle, long sleeves with bulky grey-violet cuffs and bone buttons, pale long-fingered hands
  with thin veins, a coat skirt with a trim hem over dark trousers and boots with two straps. The hooded cloak (its bull glows poison green) is the `pmx_gordon_agrippa` extra.
- **Colours.** Hair #1b1b26, skin #d7bab6 / #ead3cc / #f6e8e2, coat #3a3950 (round 3 from #2f2e3d: it read as one dark block), collar #6e6a8c, trim #7a7698, collar piping #b6ca96,
  buttons #ddd5bc, belt #2c2430, buckle #98aa78, trousers #1f1e29, boots #18181f.
- **Hair: `anime_swept`** (kept): the long hair falling over one eye of the spec.
- **No skin glow** (the glow is the bull on the cloak, an extra). **Source:** from memory + `docs/arc/npcb.md`.

## NEW / REPLACED / KEPT (rule 5)

- **NEW files:** `chars/_bc2.py`, `chars/gordon_agrippa.py`, the six hand-painted modules (before this the six skins were baseline bridges of the 64x64 sheets: every look fragment now
  says source "hand"), `finral_roulacase_glow.png` (a new glow map), and the satchel of Charmy, the earring glow of Finral, the brooch of Nozel (details the 64x64 sheet did not have).
- **REPLACED (reworked in place):** the six `playermodel/<id>.png` skins (256x256) and `tools/arc_hd/looks/<id>.json`; `hair_color` changes to the painted value (Nozel #e0e4ee to #f2f4ff,
  Luck #e8c84a to #f8d63c, Magna #17171b to #1a1a22, Finral #9a7650 to #b98c52, Charmy #201e26 to #2a2634, Gordon #0f0f14 to #1b1b26: the merge writes these into `looks.json`).
  Finral's look gets `"glow": true`.
- **KEPT:** the 64x64 sheets and `tools/arc/char_*.py`, all `looks.json` entries until the merge, the hair styles (all six unchanged), `extras`, slim/wide arms, `scale`, hair sheets (stay
  64x64), and the Time Magic VFX (not touched).

## Cost (rule 11)

Client assets only: no server work, no packets, no ticks, no per-player state. Worst case: 7 PNGs of 256x256 (6 skins + 1 glow map). Each loaded look costs about 341 KiB of GPU memory
(256x256 RGBA with mips), 682 KiB for Finral (skin and glow map); only the looks within draw distance are loaded, and the 24-block LOD drops the second layer and the hair as before.
Per-frame draw cost is unchanged (the same quads as the 64x64 model); the painters run at tool time and allocate nothing in the game.

## Review rounds (at least four per character, front / back / close / bust with `tools/arc_hd/preview_arc.py`)

1. Baseline bridge vs. the 64x64 look: faces, hair silhouettes and the outfit redrawn. Magna's mouth failed with a width that has no exact half (the smile needs 4.0, 3.0 or 2.0).
2. Colour blocking: Nozel's hair was grey (-> #f2f4ff), Luck's fringe khaki (-> #f8d63c), Nozel's front piping and cuffs too thin (0.34 -> 0.5, cuff 1.3 -> 1.7 rows); Finral's earring cover mismatch
   (overlay clipped to the 1 px column); Charmy's bag sat under the belt (the belt is drawn first now); Gordon's nose and bags smeared the face and hid the frown (re-laid on the face rows).
3. Back and side views: Magna's shaved sides and nape were bare skin (a dark stubble over the head sides and back); Gordon's coat read as one block (lighter coat, collar and trim, green collar piping).
4. Final front / back / close pass against owner references 154 to 156 and the old 64x64 look, base-layer-only check (what is seen beyond 24 blocks).

## Honest caveats

- Software renderer only (`tools/playermodel`, flat shading): nothing was seen in the game, so the in-game light, the hair engine and the extras at their real size are unconfirmed.
- Weaker than the references 154 to 156: no truly bulky pauldrons or collars (the overlay is one pixel thick), softer colour blocking on the dark outfits (Gordon, Magna's trousers),
  and the head sides, top and back are clean regions, not hand-painted locks.
- Hair-engine matters (not skin): `mohawk` on Magna is a tall flat black slab and shows a rectangular dark strip down the back of the head; `anime_swept` on Gordon and Nozel covers the brows and the
  upper fringe at close range (the painted fringe only shows in the far LOD); `anime_short` has no bun, so Charmy's canon tufts are not matched; very dark hair (Gordon, Charmy) reads blue-grey
  at the highlights of the hair sheet.
- Wiki research failed (nothing fetched at all); every canon detail is from memory.
- `test_fragments_match_looks_json` in `tools/arc_hd/test_arc_hd.py` and the fragment-vs-looks.json check in `ArcHdSkinsTest` fail until the lead runs `merge_looks.py` (the fragments differ from
  `looks.json` by the `hair_color` values above and Finral's glow flag).
