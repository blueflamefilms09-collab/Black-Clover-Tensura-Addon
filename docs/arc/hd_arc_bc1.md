# HD arc, group bc1: Asta, Yuno, Noelle Silva, Yami Sukehiro, Mimosa Vermillion

Five looks hand-painted at 256x256 (every model pixel is a 4x4 texel block). The painters are `tools/arc_hd/chars/{asta,yuno,noelle_silva,yami_sukehiro,mimosa_vermillion}.py`
with the shared helpers in `tools/arc_hd/chars/_bc1_kit.py` (the underscore keeps the kit out of the generator and out of the module test; it is this group's own copy of the
sds1 helpers plus `feather_pad`, `bare_arm`, `blob`, `head_rear`, so a change to another group's kit can never change these skins). Output:
`src/main/resources/assets/nusmp/textures/entity/playermodel/<id>.png` and `tools/arc_hd/looks/<id>.json` for each id. None of the five has a `_form2` and none has a glow map
(the look's only glow is on an extra: the grimoire of Yuno and Mimosa, the cigarette ember of Yami). `looks.json` is merged by the lead (`merge_looks.py`), not here.

Previews: `docs/previews/arc_hd/bc1_front_back.png` (front and back of all five) and `docs/previews/arc_hd/bc1_close.png` (bust of all five).

## What is the same for all five

- **64x64 stays** (rule 2): the 64x64 sheets (`tools/arc/char_*.py`) and the baseline bridge are untouched; HD is an added mode. Each painter starts from the 64x64 blueprint
  (`A.blueprint`, `A.bridge`) so the colour blocking and layout of the sheet carry over, then redraws by hand what has to be crisp.
- **Both layers.** The game drops the second layer and the hair beyond 24 blocks (`PlayerLod`), so every detail is on the base layer too; the second layer only holds cloth where
  the sheet has cloth (`K.all_covers` / `K.hold_cover`).
- **Extras are not painted over.** Cape and cape_low (Asta, Yami, Noelle, Mimosa), the grimoire (Yuno, Mimosa), the Demon-Slayer sword (Asta), the katana and the cigarette (Yami)
  and the headband tails (Asta) are separate models of the look, so the skin only carries what is under them. Slim/wide arms, `scale`, `hold_r` and the glow semantics are kept.
- **Hair is the kit's attached 3D hair** (one colour, `hair_color`); the skin paints the scalp, the visible fringe (thick layered locks, never fingers, every lock starts on the forehead
  mass) and, for the long cuts, the back and the sides of the head down to the hairline (`K.head_rear`). The 3D fringe hides the painted brows and headbands on the front, so the eyes,
  the mouth and the colour blocking carry the face. Hair sheets stay 64x64.
- Colours are mixed with `K.lit` / `K.cloth` / `K.hair_palette`, not `H.tone` above 1.0 (that whitens and turned dark cloth grey). Deterministic code (numpy + Pillow only, fixed seeds).

## Sources (rule 10)

No wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org both refused the connection, so neither the English nor the Japanese fallback worked). Every
entry is from memory of the series plus `docs/arc/npca.md` / `docs/arc/npcb.md` (which cite the wikis) and the spec of each sheet. Could not be fetched at all: every blackclover.fandom.com and ja.wikipedia.org page.

## Asta (`asta`): hair `anime_spike_tall`, #d6dae4

Short ash-white upswept hair as thick back-swept clumps (four cuts compared; `anime_spiky` read as fingers, `anime_spike_swept` as thin floating blades). Big emerald eyes (#0f5a2a / #3aae52),
thick slanted ash brows, a wide open grin with teeth and tongue. Black sleeveless top (#22222b) with a ribbed neck, armholes and hem, a broad lit chest plane; bare muscled arms; dark trousers
(#343848) with creases and calf boots (#4a3626) with a folded cuff and strap; brown belt (#5a3d28) with a gold buckle. The black Black Bull headband (#18181e) is painted on the front (under the
fringe) and wraps the back and both sides, where the ribbon extra ties on. Added in the HD pass: the wrapped headband on the sides and back (the sheet only had the front).

## Yuno (`yuno`): hair `anime_bangs_long`, #1a1a24

Black parted fringe (two blades sweeping to the temples), calm sharp teal eyes (#103c34 / #2f8a72), thin brows, a neutral mouth. Dark blue-black shirt (#202034) with a sheen, a gold stand collar
only at the neck, a placket with gold piping and three buttons, a belt with a gold buckle, a shirt tail with a gold hem, sleeves with elbow creases and beveled gold cuffs over bare hands,
dark trousers and brown calf boots. Added in the HD pass: the blue feathered shoulder collar of the Golden Dawn mantle (the sheet's `shoulder_fur` accessory #5f7fb0 was in the spec but not
visible in the 64x64 look or in any extra).

## Noelle Silva (`noelle_silva`): hair `twin_tails`, #e2e7f2

Long silver hair in two low twin tails (the classic `twin_tails`; no anime cut has twin tails), painted silver on the back and sides of the head (the sheet left the nape bare). Big shaded blue-violet
eyes (#17246e / #5a74e0), arched silver brows, a strong blush, a small tight pout. White stand collar with a blue ribbon cravat and tails; navy dress (#2c3e80) with princess seams, white piping and
gold buttons; brown belt with a gold buckle; pleated skirt hem with white trim; white sleeves with lilac cuffs over bare hands; knee boots with a folded cuff and a strap. Added in the HD pass:
the bare thigh between the skirt hem and the knee boots (the sheet's spec says `legs: bare`, the 64x64 look had navy down to the boots).

## Yami Sukehiro (`yami_sukehiro`): hair `anime_spike_tall`, #15151b (was `anime_spiky`)

Wild black swept-up hair as thick clumps (`anime_spike_swept` gave long thin spikes at the sides; `anime_swept` was too small for his volume). Tan broad face (#da9565), narrow sharp dark eyes with
heavy lids, thick slanted black brows, dense stubble on the jaw, a moustache and a small goatee, a lopsided smirk with the viewer's left corner left flat and clear for the cigarette extra. White
tank (#ecebe6) with a deep scooped neck, a black throat strap with a steel ring, folds and a hem; bare broad muscled arms with wrapped forearms; dark belt (#3d2b22) with a big gold buckle and
three small hanging books; black trousers (#26262e) under beige leather chaps (#c4b08a: stitched edges, rivets, a thigh strap with a buckle, knee wrinkles; the outer half of the front, a strip of
the back and the whole outer side); black calf boots with a steel strap buckle. Not verified against a wiki: the forearm wraps (the owner's brief asked for bandages; they are in the sheet's spec) and
the books on the belt (the spec says `books: True`). The right-hand grip area is not painted over (the katana extra).

## Mimosa Vermillion (`mimosa_vermillion`): hair `anime_long`, #f2d878

Long wavy golden hair painted to the nape and behind the ears, a parted golden fringe with two long cheek locks. Big shaded green eyes (#1d6a34 / #58c46a) with lashes and big highlights, worried
golden brows, a strong blush with strokes, a small shy smile. Cream tunic (#f3e9cd) with a tan stand collar, a brown corset (#7a4c33) with lacing and gold eyelets over a brown belt with a gold
buckle, a scalloped tan tunic hem, puffy sleeves with tan cuffs over bare hands, puffy white shorts with a gathered hem band, bare thighs, brown knee boots with a cream-tan folded cuff and lacing.
Added in the HD pass: the blue feathered shoulder collar (spec `shoulder_fur` #5f7fb0), the corset lacing and the bare thighs. The pink hair bow of the spec is NOT drawn: the 3D hair cap covers every
place on the head where it could sit and the look has no ribbon extra.

## NEW / REPLACED / KEPT (rule 5)

- **NEW files:** `chars/mimosa_vermillion.py`, `chars/yami_sukehiro.py` (before this both were baseline bridges), `chars/_bc1_kit.py`, this doc and the two previews.
  Fragments for `yami_sukehiro` and `mimosa_vermillion` now say source "hand".
- **REPLACED (reworked in place):** `chars/asta.py`, `chars/yuno.py`, `chars/noelle_silva.py` (they existed from the wip commit) and the five PNGs
  (`asta`, `yuno`, `noelle_silva`, `yami_sukehiro`, `mimosa_vermillion`). The merge will change `looks.json` for: Asta hair `anime_spiky` -> `anime_spike_tall`, `hair_color` #cfd2d8 -> #d6dae4;
  Yami hair `anime_spiky` -> `anime_spike_tall`; Yuno `hair_color` #16161c -> #1a1a24; Noelle `hair_color` #dfe4ef -> #e2e7f2; Mimosa unchanged.
- **KEPT:** the 64x64 sheets and `tools/arc/char_*.py`, all `looks.json` entries until the merge, the extras, slim/wide arms, `scale`, `hold_r`, glow on/off per look (none), hair sheets (stay 64x64),
  the other groups' files, and the Time Magic VFX (not touched).

## Cost (rule 11)

Skins are client assets only: no server work, no packets, no ticks, no per-player state, nothing to bound per tick. Worst case: 5 PNGs of 256x256 (about 160 KB on disk in total); each loaded look
costs about 341 KiB of GPU memory (256x256 RGBA with mips); only the looks within draw distance are loaded and the 24-block LOD drops the second layer and the hair as before. Per-frame draw
cost is unchanged (same quads as the 64x64 model); the painters run at tool time (about 1.5 s per look) and allocate nothing at game time.

## Review rounds (four per character, front / back / close with `tools/arc_hd/preview_arc.py`)

1. Baseline bridge vs. the 64x64 look: faces and hair silhouettes redrawn, outfit blocks painted; hair cuts compared (Asta four, Yami four).
2. Asta: chest read as two round swells, replaced by a broad lit plane; boot cuff read as a metal stripe, hand-drawn leather cuff. Yuno: flat black and gold, the blue feather collar added.
   Noelle: bare nape painted silver, the pout redrawn (it read as a frown). Yami: stubble was blotchy and the mouth a muddle, moustache and smirk redrawn; hair `anime_spike_swept` rejected. Mimosa: lacing noise, feather collar enlarged.
3. Colour blocking: Yuno's shirt a livelier blue-black with a stronger sheen, Asta's trousers and boots a visible colour, Noelle's navy lifted, Yami's tan warmer.
4. Final front / back / close pass of all five against the owner references 154 to 156 and the old 64x64 look, then the repo run of the generator.

## Honest caveats

- Software renderer only (flat shading, hair not tinted): nothing here was seen in the game, so the in-game light, the hair tint and the extras at their real size are unconfirmed.
- Weaker than the references 154 to 156: no truly bulky pauldrons or collars (the overlay geometry is one pixel thick), softer colour blocking, and rounds 3 and 4 were colour and detail tuning
  rather than redesigns.
- Hair-engine matters (not skin): Yuno's `anime_bangs_long` has a crown of thin curling locks that reads as an afro cluster from above; Yami's and Asta's `anime_spike_tall` is a flat star-shaped
  crown from behind; Noelle's `twin_tails` cap is a flat helmet in front of the bangs; Mimosa's `anime_long` back chain hides her mantle in the back view.
- Not verified against any wiki (blocked): every canon detail above; the unverified additions are listed per character.
