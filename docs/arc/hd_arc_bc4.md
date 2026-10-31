# HD arc, group bc4: Fuegoleon Vermillion, Mereoleona Vermillion, Leopold Vermillion, Julius Novachrono, William Vangeance, Nacht Faust

Six looks hand-painted at 256x256 (every model pixel is a 4x4 texel block). None has a `_form2`. The painters are
`tools/arc_hd/chars/{fuegoleon_vermillion,mereoleona_vermillion,leopold_vermillion,julius_novachrono,william_vangeance,nacht_faust}.py` with the shared helpers in
`tools/arc_hd/chars/_bc4.py` (a copy of `_bc2.py` plus `each`, `cap`, `fur_band`, `scar` and `muscle`; the underscore keeps the kit out of the generator and the module test). Output:
`src/main/resources/assets/nusmp/textures/entity/playermodel/<id>.png` (+ `william_vangeance_glow.png`, which existed, and `nacht_faust_glow.png`, which is new) and
`tools/arc_hd/looks/<id>.json` for each id. `looks.json` is merged by the lead (`merge_looks.py`), not here.

Previews: `docs/previews/arc_hd/bc4_front_back.png` (front and back of all six, extras on) and `docs/previews/arc_hd/bc4_close.png` (bust of all six).

## What is the same for all six

- **64x64 stays** (rule 2): the 64x64 sheets and the baseline bridge are untouched; HD is the added mode and the skins keep their names. Each painter starts from the 64x64 blueprint
  (`A.blueprint`, `A.bridge`) and redraws by hand what has to be crisp.
- **Both layers.** The game drops the second layer and the hair beyond 24 blocks (`PlayerLod`), so faces, trim, buckles and seams are on the base layer too; the second layer only
  holds cloth where the sheet has cloth (`hold_cover` clips it to the blueprint coverage).
- **Extras are not painted over.** The Crimson Lion capes (Fuegoleon, Mereoleona, Leopold), the Golden Dawn cape (William), the Black Bull cloak (Nacht), Julius's tassel and drum and every
  floating grimoire are the `pmx_<id>` extras; the backs are kept plain, the sash knot of Julius sits at the tassel and the chest of Nacht is free of cloth. Slim/wide arms, `scale`, the extras and
  the glow semantics are kept.
- **Hair is the kit's attached 3D hair**; the skin paints the scalp, the fringe and the head sides/back (`K.tidy_head`, `K.fringe_mass`, `K.lock`). The 3D hair covers the forehead down to
  about y 4.1 for `anime_swept`, so the eyes sit at about y 5.3 and the brows at about 4.4 (a first pass with eyes at 4.8 was cut by the fringe). Hair sheets stay 64x64 (owner decision).
- **Pale and black tints.** The hair sheet is grey and the tint multiplies, so white and cream hair starts very light and black is lifted a little.
- Deterministic code (numpy + Pillow only): no random numbers, the same bytes on every run.

## Sources (rule 10)

The English and the Japanese wiki could not be fetched in this environment (curl status 000 on the fandom hosts and on ja.wikipedia). Every entry below is **from memory of the series plus
the repo's sheets (`tools/arc/char_<id>.py`) and `docs/arc/npcb.md`** (which cites the wikis). Nothing was traced and nothing could be fetched at all. Entries marked "added in the HD pass"
are details the 64x64 sheet did not have.

## Fuegoleon Vermillion, Captain of the Crimson Lion Kings (`fuegoleon_vermillion`)

- **Design.** Calm, noble and sharp: slanted amber eyes, thin brows, a closed mouth, red hair swept back with a fringe and two long framing locks. Dark crimson high-collared uniform
  jacket (a darker yoke band with a gold line, a gold-edged stand collar with a cream cravat V, a gold flame crest on the chest, a gold-piped placket with four buttons), bulky dark
  crimson shoulder boards with a gold border and studs, gold cuffs over black gloves, a double belt with a gold buckle, a coat skirt over black trousers, boots with a gold cuff and a strap.
  The cape and the floating grimoire are the `pmx_fuegoleon_vermillion` extra.
- **Colours.** Hair #c2321c, coat #7d1c1f / #521214 / #a52a2a, gold #e6b44a, cream #efe9dc, trousers #262630, boots #4a3430, belt #3e2a22, iris #7a3408 / #e2902a / #ffd982.
- **Hair: `anime_swept`** (kept): red hair swept over the brow like the owner's silver reference 155 in red; thick layered clumps.
- **Added in the HD pass:** the flame crest, the shoulder boards, the cravat, the double belt (the sheet had a plain jacket).
- **Source:** from memory + `docs/arc/npcb.md`.

## Mereoleona Vermillion, the Crimson Lion Kings' strongest (`mereoleona_vermillion`)

- **Design.** Fierce and proud: tan skin, bright blue eyes with strong slanted brows, a wide grin with a canine, a wild red mane. Dark red sleeveless tank with a scoop neck and a gold
  lacing, a gold neck chain, a dark belt with a gold buckle, muscular bare arms (shaded biceps, forearm lines), cream wraps that end in fists (a loose end on the wrap), dark trousers,
  boots from the shin with straps. The cape is the `pmx_mereoleona_vermillion` extra.
- **Colours.** Hair #d4421c, skin #be845c / #d8a072 / #ecbc8a, tank #7a1d17 / #4c100e, gold #e6b44a, wraps #e9dcbc, trousers #2c2424, boots #5a3026, belt #3e2a22, iris #1c3a8c / #4a88e4 / #c2e0ff.
- **Hair: `anime_mane`** (kept): the big wild lion's mane is the only cut that is as big as her canon hair.
- **Added in the HD pass:** the lacing and chain, the muscle shading, the loose end of the wrap, the strapped boots.
- **Source:** from memory + `docs/arc/npcb.md`.

## Leopold Vermillion, the youngest Vermillion (`leopold_vermillion`)

- **Design.** Hot-tempered and cheeky: big amber eyes under thick slanted brows, a grin with a canine, a blush; red hair that stands up in flame-like clumps. Red tunic with a gold-piped
  placket and three toggles, a collar, a belt with a gold buckle, short sleeves with gold cuffs, bare forearms with a wrist band and a fingerless hand, a tunic skirt, dark trousers, boots
  with a gold cuff and a strap. The cape is the `pmx_leopold_vermillion` extra.
- **Colours.** Hair #de4a1e, tunic #a02a1e / #6a1612, gold #e8b64c, trousers #2a2a34, boots #4a3228, belt #46301f, iris #7a3408 / #f2a236 / #ffe9a8.
- **Hair: `anime_spike_tall`** (CHANGED from `anime_spiky`): the owner wants spiky hair as thick layered clumps, not fingers; `anime_spiky` is a ring of thin spikes, `anime_spike_tall` stands
  up in thick clumps like flames. The `hair` field of the fragment changes (see NEW / REPLACED).
- **Source:** from memory + `docs/arc/npcb.md`.

## Julius Novachrono, the Wizard King (`julius_novachrono`)

- **Design.** Gentle and old beyond his years: warm amber eyes, a calm smile, pale cream hair that frames the face in curtains. Floor-length off-white robe with gold trim, a clover brooch
  and a vine embroidery on the chest, a wide collar with a skin V, a gold sash with a pale buckle and a knot (the tassel extra hangs from it), wide sleeves with gold cuffs and a lining, a
  robe hem in gold that covers the legs down to the boot toe. The floating drum, the tassel and the cape are the `pmx_julius_novachrono` extras.
- **Colours.** Hair #fff6d2 (round 4: from #f4eac0, it read grey under the tint), robe #f4efe2 / #d3cab4, gold #dcb450 / #8a6a20, trousers #e8e0c8, boots #5a4a30, iris #6a4a0c / #d6a640 / #fff1b8.
- **Hair: `anime_long_open`** (kept): medium-long hair that frames the face with no centre fringe, as the spec says.
- **Source:** from memory + `docs/arc/npcb.md`.

## William Vangeance, Captain of the Golden Dawn (`william_vangeance`)

- **Design.** Slim and noble: a gold eye-mask (pointed temples, a notch for the nose, a dark rim, an engraved brow line, two slanted dark slits with a violet light, temple rivets and a bridge
  stud) over a calm face with a thin mouth; long white hair swept over one eye. Cream coat with a gold collar, a gold-piped placket with buttons, a chain clasp, a double belt with a dark
  buckle, a warm-shaded coat skirt, fur shoulders in blue (the Golden Dawn fur) that hang down the upper arms, gold cuffs over cream gloves, knee boots with a gold cuff. The golden cape
  (back view) and the grimoire are the `pmx_william_vangeance` extra.
- **Colours.** Hair #f4f5fb, coat #ebe2c6 / #c9bc98, gold #d4a846 / #f0cc6a / #7a5a1a, fur #6a90c8 / #bcd2f0 / #38558a, trousers #2e2e3c, boots #4a362a, belt #3e2a22, visor light #b58cff, iris #3a1a68 / #9a6ae0 / #e8d8ff.
- **Hair: `anime_swept`** (kept): the long white hair swept over one eye of the spec.
- **Glow:** the old look already had a glow map (the visor); it is redrawn here for the two slits only (violet #b58cff, core #f0e6ff).
- **Added in the HD pass:** the fur, the chain clasp and the mask engraving (the sheet had a plain band).
- **Source:** from memory + `docs/arc/npcb.md`.

## Nacht Faust, the devil host of the Black Bulls (`nacht_faust`)

- **Design.** Cold, calm and tired: pale skin, pale blue half-lidded eyes with dark rings, thin straight brows, a flat mouth, black hair parted with a fringe over the brow and two long side locks;
  the temples and the back of the head are covered with hair down to the nape (the sheet left them bare skin under the ponytail hat). Dark blue-black high-collared coat with lilac-grey trim: a
  choker at the neck, a stand collar with a trim line, an asymmetric lilac front edge with four buttons, a belt with a lilac buckle, long slim sleeves with lilac cuffs, bare pale hands, a
  coat skirt with a lilac hem over dark trousers with knee creases, boots with lilac cuffs. The Black Bull cloak (its bull glows violet) is the `pmx_nacht_faust` extra.
- **The relic gems (decision for the lead).** The sheet says four violet relic gems hang on a cord at his chest, but the extra's geo only has the cloak, so they were not drawn at all. They are
  painted on the coat in this pass (a dark cord with four faceted violet stones, the two inner stones larger) and light the glow map (`#c8a0ff`), so `nacht_faust.json` gets `"glow": true` and
  `nacht_faust_glow.png` is NEW. One flag switches it off: `GEMS_ON_SKIN = False` in `nacht_faust.py` (then the coat is plain, there is no glow map and the look keeps its old fields). If the
  gems are ever exported in the geo, set the flag to False so they do not appear twice.
- **Colours.** Hair #1c1c28 (lifted from #12121a: pure black reads dead under the tint), skin #e2beba / #f7dcd4 / #ffeee6, coat #30303f / #4c4c64 / #1c1c28, trim #9a9ac2 / #5a5a80, trousers #262634,
  boots #20202c, belt #2a2630, gems #8a4ee0 (glow #c8a0ff), iris #3a5a86 / #92c0e6 / #e6f6ff.
- **Hair: `high_ponytail`** (KEPT, the kit's classic): no anime cut is a ponytail; the painted side locks and the back of the head make the classic cap read as hair.
- **Removed from the first pass:** the rank bar on the breast (it fought the gems).
- **Source:** from memory + `docs/arc/npcb.md`.

## NEW / REPLACED / KEPT (rule 5)

- **NEW files:** `chars/_bc4.py` (appended helpers: `each`, `cap`, `fur_band`, `scar`, `muscle`), the six hand-painted modules (before this the six skins were baseline bridges: every look fragment
  now says source "hand"), `nacht_faust_glow.png` (a new glow map) and these details the 64x64 sheet did not have: Fuegoleon's crest and shoulder boards, Mereoleona's lacing and muscle shading,
  William's fur and mask engraving, Nacht's relic gems and side/back hair, the docs and the two previews.
- **REPLACED (reworked in place, same names):** the six `playermodel/<id>.png` skins (256x256), `william_vangeance_glow.png` (redrawn) and `tools/arc_hd/looks/<id>.json`. `hair_color` changes to the
  painted value (Fuegoleon #b8301c to #c2321c, Mereoleona #d0421e to #d4421c, Leopold #d8431d to #de4a1e, Julius #e0cf9a to #fff6d2, William #eceef6 to #f4f5fb, Nacht #12121a to #1c1c28), Leopold's
  `hair` changes from `anime_spiky` to `anime_spike_tall`, and Nacht's look gets `"glow": true` (the lead's merge writes these into `looks.json`).
- **KEPT:** the 64x64 sheets and `tools/arc/char_*.py`, all `looks.json` entries until the merge, the other five hair styles, `extras`, slim/wide arms, `scale`, hair sheets (stay 64x64), old
  64x64 mode (`gen_arc_hd.py --size 64` still reproduces it), and the Time Magic VFX (not touched).

## Cost (rule 11)

Client assets only: no server work, no packets, no ticks, no per-player state, nothing new for the server smoke test. Worst case: 8 PNGs of 256x256 (6 skins + 2 glow maps, William's and Nacht's).
Each loaded look costs about 341 KiB of GPU memory (256x256 RGBA with mips), 682 KiB for William and Nacht (skin plus glow map); only the looks within draw distance are loaded, and the 24-block
LOD drops the second layer and the hair as before. Per-frame draw cost is unchanged (the same quads as the 64x64 model); the painters run at tool time and allocate nothing in the game.

## Review rounds (at least four per character, front / back / side / close / texture sheet with `tools/arc_hd/preview_arc.py` and a private review script)

1. Baseline bridge vs. the 64x64 look: faces, hair silhouettes, outfit redrawn. Failures: Fuegoleon's skirt was painted over the whole leg (fixed with a y range and a hem), junk lines in
   Mereoleona's body side and a stray `K.cheeks = None` in Leopold's module (removed).
2. Colour blocking and faces: eyes and brows were cut by the 3D fringe (eyes to y 5.3, brows to 4.4); Fuegoleon got the yoke, collar, crest and shoulder boards; William's old overlay frame left a
   gold spike above the visor (the overlay rows 2.7 to 6.9 are erased before the visor is redrawn), the cool grey skirt on cream became a warm shaded one and the fur was made taller (0 to 2.35);
   Leopold's hair changed to thick clumps; Nacht got the cord and gems and lost the rank bar.
3. Back, side and texture sheets: Nacht's temples and nape were bare skin under the ponytail hat (side locks and a covered nape added); the extras' attachment areas (cape, tassel, drum, grimoire)
   were checked to be clear.
4. Final front / back / close pass against owner references 154 to 156 and the old 64x64 look; Julius's hair lifted to #fff6d2; base-layer-only check (what is seen beyond 24 blocks).

## Honest caveats

- Software renderer only (`tools/playermodel`, flat shading, no hair tint pass): nothing was seen in the game, so the in-game light, the hair engine, the tint and the extras at their real size are
  unconfirmed. The pale hairs (Julius, William) read grey in the preview only because the tint is not applied; if Julius still reads khaki in the game, raise `HAIR` towards #fffbe6.
- Weaker than the references 154 to 156: no truly bulky pauldrons (the overlay is one pixel thick; Fuegoleon's shoulder boards are painted on the arm tops), softer colour blocking on the dark outfit
  (Nacht), and the head sides and top of the kit hairs are clean regions, not hand-painted locks.
- Hair problems (engine, not skin): `anime_swept` on Fuegoleon and William covers the brows and the upper fringe at close range (the painted fringe only shows in the far LOD); `high_ponytail` on
  Nacht is a flat black cap with a column down the back and a thin horizontal line at the hair edge in the software renderer; `anime_mane` on Mereoleona and `anime_spike_tall` on Leopold are very wide
  at the top; the ear on side faces is drawn as a ring by the renderer.
- Wiki research failed (nothing fetched at all); every canon detail is from memory.
- Tests: `PlayerModelDataTest` 12 passed; `ArcHdSkinsTest` 4 passed and 1 failed: `looksJsonEqualsTheMergeOfTheArcHdFragments` fails until the lead runs `merge_looks.py` (the fragments of this group
  differ from `looks.json` by the values above; it names `asta` first, which is another group's fragment).
